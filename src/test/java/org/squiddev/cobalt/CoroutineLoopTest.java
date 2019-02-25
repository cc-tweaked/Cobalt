package org.squiddev.cobalt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.VarArgFunction;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.squiddev.cobalt.LuaString.valueOf;

@RunWith(Parameterized.class)
public class CoroutineLoopTest {
	private final String name;

	public CoroutineLoopTest(String name) {
		this.name = name;
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"nested"},
			{"top"},
		};

		return Arrays.asList(tests);
	}

	@Test
	public void run() throws IOException, CompileException, LuaError, InterruptedException {
		ScriptDrivenHelpers helpers = new ScriptDrivenHelpers("/coroutine/loop-");
		helpers.setup();
		helpers.globals.rawset("yieldBlocking", new VarArgFunction() {
			@Override
			public Varargs invoke(LuaState state, Varargs args) throws LuaError {
				try {
					return LuaThread.yieldBlocking(state, args);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});

		LuaThread thread = new LuaThread(helpers.state, helpers.loadScript(name), helpers.globals);

		int i = 0;
		while (!thread.getStatus().equals("dead")) {
			LuaThread.run(thread, valueOf("Resume " + i++));
		}
	}
}
