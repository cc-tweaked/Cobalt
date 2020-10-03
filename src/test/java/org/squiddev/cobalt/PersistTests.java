package org.squiddev.cobalt;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.ResumableVarArgFunction;
import org.squiddev.cobalt.lib.*;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.persist.Persist;
import org.squiddev.cobalt.support.PairedStream;
import org.squiddev.cobalt.support.PrettyValue;

import java.io.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.ValueFactory.valueOf;

public class PersistTests {
	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@MethodSource("basicValues")
	public void values(PrettyValue value) throws Exception {
		LuaState state = new LuaState();

		LuaValue read = PairedStream.run(
			output -> Persist.persist(state, new DataOutputStream(output), value.getValue()),
			input -> Persist.unpersist(state, new DataInputStream(input))
		);

		assertEquals(value, new PrettyValue(read));
	}

	public static Stream<PrettyValue> basicValues() {
		return Stream.of(
			Constants.TRUE, Constants.FALSE, Constants.NIL,
			valueOf(0), valueOf(1), valueOf(-5),
			valueOf(0.1), valueOf(Double.POSITIVE_INFINITY),
			valueOf(""), valueOf("hello"),
			valueOf("a long string which will not be stored inline"),
			get(() -> {
				LuaTable table = new LuaTable();
				for (int i = 1; i < 5; i++) table.rawset(i, valueOf(Integer.toString(i)));
				table.rawset("a", table);
				return table;
			}),
			get(() -> {
				LuaTable first = new LuaTable();
				LuaTable last = first;
				for (int i = 0; i < 100; i++) {
					LuaTable next = new LuaTable();
					last.rawset(i, next);
					last = next;
				}
				return first;
			})
		).map(PrettyValue::new);
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"add",
		"coroutines",
		"pcall",
	})
	public void program(String filename) throws IOException, InterruptedException, CompileException, LuaError {
		byte[] contents;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			LuaState state = new LuaState();
			LuaTable env = setupGlobals(state);

			LuaFunction function;
			try (InputStream stream = getClass().getResourceAsStream("/persist/" + filename + ".lua")) {
				if (stream == null) fail("Could not load script for test case: " + filename);
				function = LoadState.load(state, stream, "@" + filename + ".lua", env);
			}

			LuaThread thread = new LuaThread(state, function, env);
			LuaThread.run(thread, NONE);

			assertEquals("suspended", state.getCurrentThread().getStatus());
			Persist.persist(state, new DataOutputStream(out), state.getCurrentThread());
			contents = out.toByteArray();
		}

		LuaState state2 = new LuaState();
		setupGlobals(state2);
		LuaThread thread;
		try (ByteArrayInputStream out = new ByteArrayInputStream(contents)) {
			thread = (LuaThread) Persist.unpersist(state2, new DataInputStream(out));
		}

		Varargs result = LuaThread.run(thread, NONE);
		assertEquals(valueOf("OK"), result);
	}

	private static <T> T get(Supplier<T> supplier) {
		return supplier.get();
	}

	/**
	 * Like {@link JsePlatform#debugGlobals(LuaState)}, but without os, io and package. Namely, the ones
	 * ComputerCraft uses (as that's all we need persistence for right now).
	 *
	 * @param state The state to setup globals in.
	 */
	public LuaTable setupGlobals(LuaState state) {
		LuaTable _G = new LuaTable();
		state.setupThread(_G);
		_G.load(state, new BaseLib());
		_G.load(state, new TableLib());
		_G.load(state, new StringLib());
		_G.load(state, new CoroutineLib());
		_G.load(state, new MathLib());
		_G.load(state, new Utf8Lib());
		_G.load(state, new DebugLib());
		_G.load(state, new Functions());
		return _G;
	}

	private static class Functions extends ResumableVarArgFunction<Object> implements LuaLibrary {
		@Override
		public LuaValue add(LuaState state, LuaTable environment) {
			bind(state, "$test", environment, Functions::new, new String[]{"suspend"});
			return environment;
		}

		@Override
		public Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaThread.suspend(state);
			return NONE;
		}

		@Override
		protected Varargs resumeThis(LuaState state, Object object, Varargs value) {
			return NONE;
		}
	}
}
