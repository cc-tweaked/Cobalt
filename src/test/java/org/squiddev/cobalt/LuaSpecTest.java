package org.squiddev.cobalt;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.debug.DebugHelpers;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.lib.Bit32Lib;
import org.squiddev.cobalt.lib.CoreLibraries;
import org.squiddev.cobalt.lib.system.SystemBaseLib;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

public class LuaSpecTest {
	private static final Logger LOGGER = Logger.getLogger(LuaSpecTest.class.getName());
	private static final Path ROOT = Path.of("src", "test", "resources", "spec");
	private static final Pattern TAG = Pattern.compile(":([^ ]+)");
	private final LuaState state;
	private final LuaTable env;

	private List<DynamicNode> nodes;

	private static String stripTags(String name) {
		return TAG.matcher(name).replaceAll("");
	}

	public LuaSpecTest() throws IOException, LuaError, CompileException {
		state = LuaState.builder().errorReporter((error, message) -> LOGGER.log(Level.WARNING, message.get(), error)).build();
		env = state.globals();
		CoreLibraries.debugGlobals(state);
		new SystemBaseLib(x -> null, System.in, System.out).add(env);
		Bit32Lib.add(state, env);
		TestLib.add(env);

		// Set the "arg" global as we've have some tests which need it.
		env.rawset("arg", new LuaTable());

		try (InputStream is = new BufferedInputStream(Files.newInputStream(ROOT.resolve("_prelude.lua")))) {
			var expect = LoadState.load(state, is, "@_prelude.lua", env);
			LuaThread.runMain(state, expect);
		}

		env.rawset("describe", RegisteredFunction.of("describe", (state, arg1, function) -> {
			String name = arg1.checkString();
			if (nodes == null) throw new LuaError("Cannot register tests at this stage");

			List<DynamicNode> oldNodes = nodes;
			List<DynamicNode> newNodes = nodes = new ArrayList<>();
			try {
				LuaThread.runMain(state, function.checkFunction());
			} finally {
				nodes = oldNodes;
			}

			nodes.add(filterNode(name, DynamicContainer.dynamicContainer(stripTags(name), newNodes)));

			return Constants.NIL;
		}).create());

		env.rawset("it", RegisteredFunction.of("it", (state, arg1, arg2) -> {
			String name = arg1.checkString();
			LuaFunction function = arg2.checkFunction();
			if (nodes == null) throw new LuaError("Cannot register tests at this stage");

			nodes.add(filterNode(name, DynamicTest.dynamicTest(stripTags(name), () -> {
				// Run each test in a clean coroutine.
				LuaThread thread = new LuaThread(state, function);
				Varargs result = LuaThread.run(thread, Constants.NONE);
				if (thread.isAlive()) throw new AssertionError("Thread unexpected yielded with " + result);
			})));

			return Constants.NIL;
		}).create());

		env.rawset("pending", RegisteredFunction.of("pending", (state, arg1, arg2) -> {
			String name = arg1.checkString();
			arg2.checkFunction();
			if (nodes == null) throw new LuaError("Cannot register tests at this stage");

			nodes.add(DynamicTest.dynamicTest(stripTags(name), () -> Assumptions.assumeFalse(false, "Test is 'pending'.")));

			return Constants.NIL;
		}).create());

		env.rawset("fail", RegisteredFunction.of("fail", (state, arg) -> {
			var frame = state.getCurrentThread().getDebugState().getFrame(2);
			if (frame != null && frame.stack != null) {
				var values = frame.stack;
				for (int i = 0; i < values.length; i++) {
					var value = values[i];
					var local = frame.closure.getPrototype().getLocalName(i + 1, frame.pc);
					if (!value.isNil() || local != null) System.out.printf("% 2d => %s [%s]\n", i, values[i], local);
				}
			}
			throw assertionFailure()
				.message(arg.checkString())
				.reason(DebugHelpers.traceback(state.getCurrentThread(), 0))
				.build();
		}).create());
	}

	private static boolean checkTags(String name) {
		var matcher = TAG.matcher(name);
		while (matcher.find()) {
			var tag = matcher.group(1);
			if (tag.equals("cobalt") || tag.startsWith("lua")) continue;
			if (tag.equals("!cobalt")) return false;
			throw new IllegalStateException("Unknown tag " + tag);
		}
		return true;
	}

	private static DynamicNode filterNode(String name, DynamicNode node) {
		return checkTags(name) ? node : makePending(node);
	}

	private static DynamicNode makePending(DynamicNode node) {
		if (node instanceof DynamicContainer container) {
			return DynamicContainer.dynamicContainer(
				container.getDisplayName(),
				container.getTestSourceUri().orElse(null),
				container.getChildren().map(LuaSpecTest::makePending)
			);
		} else if (node instanceof DynamicTest test) {
			return DynamicTest.dynamicTest(
				test.getDisplayName(),
				test.getTestSourceUri().orElse(null),
				PendingTest.of(test.getExecutable())
			);
		} else {
			throw new IllegalStateException("Unknown node " + node);
		}
	}

	/**
	 * A test which is expected to fail.
	 *
	 * @param test The original test to run.
	 */
	private record PendingTest(Executable test) implements Executable {
		public static PendingTest of(Executable test) {
			return test instanceof PendingTest t ? t : new PendingTest(test);
		}

		@Override
		public void execute() throws Throwable {
			try {
				test.execute();
			} catch (Throwable actualException) {
				// If we've got an expected error, then ignore it and mark this test as expected to fail.
				if (actualException instanceof AssertionError || actualException instanceof LuaError) {
					Assumptions.abort("Test is not run on Cobalt");
					return;
				}

				// Otherwise there's some more critical bug, rethrow it!
				throw actualException;
			}

			// This test should have failed but didn't!
			throw new AssertionFailedError("Test is marked :!cobalt, but passes on Cobalt.");
		}
	}

	@TestFactory
	public Stream<DynamicNode> getTests() throws IOException {
		return Files.walk(ROOT)
			.filter(x -> x.getFileName().toString().endsWith("_spec.lua"))
			.flatMap(path -> {
				LuaFunction function;
				try (InputStream stream = Files.newInputStream(path)) {
					function = LoadState.load(state, stream, "@" + path.getFileName(), env);
				} catch (IOException | CompileException | LuaError e) {
					throw new RuntimeException("Failed to load " + path, e);
				}

				List<DynamicNode> nodes = this.nodes = new ArrayList<>();
				try {
					LuaThread.runMain(state, function);
				} catch (LuaError e) {
					throw new RuntimeException(e);
				} finally {
					this.nodes = null;
				}

				return nodes.stream();
			});
	}
}
