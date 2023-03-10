package org.squiddev.cobalt;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.lib.system.SystemLibraries;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LuaSpecTest {
	private static final Path ROOT = Path.of("src", "test", "resources", "spec");
	private final LuaState state;
	private final LuaTable env;

	private List<DynamicNode> nodes;

	public LuaSpecTest() throws IOException, LuaError, CompileException {
		state = new LuaState();
		env = state.getMainThread().getfenv();
		SystemLibraries.debugGlobals(state);

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

			nodes.add(DynamicContainer.dynamicContainer(name, newNodes));

			return Constants.NIL;
		}).create());

		env.rawset("it", RegisteredFunction.of("it", (state, arg1, arg2) -> {
			String name = arg1.checkString();
			LuaFunction function = arg2.checkFunction();
			if (nodes == null) throw new LuaError("Cannot register tests at this stage");

			nodes.add(DynamicTest.dynamicTest(name, () -> {
				// Run each test in a clean coroutine.
				LuaThread thread = new LuaThread(state, function, env);
				Varargs result = LuaThread.run(thread, Constants.NONE);
				if (thread.isAlive()) throw new AssertionError("Thread unexpected yielded with " + result);
			}));

			return Constants.NIL;
		}).create());

		env.rawset("fail", RegisteredFunction.of("fail", (state, arg) -> {
			throw new AssertionError(arg.checkString());
		}).create());
	}

	@TestFactory
	public Stream<DynamicNode> getTests() throws IOException {
		return Files.walk(ROOT)
			.filter(x -> x.getFileName().toString().endsWith("_spec.lua"))
			.map(path -> {
				LuaFunction function;
				try (InputStream stream = Files.newInputStream(path)) {
					function = LoadState.load(state, stream, "@" + path.getFileName(), env);
				} catch (IOException | CompileException e) {
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

				return DynamicContainer.dynamicContainer(path.getFileName().toString(), nodes);
			});
	}
}
