package org.squiddev.cobalt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Tests protection
 */
@RunWith(Parameterized.class)
public class ProtectionTest {
	private final String name;
	private ScriptDrivenHelpers helpers;

	public ProtectionTest(String name) {
		this.name = name;
		this.helpers = new ScriptDrivenHelpers("/protection/");
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"string"},
			{"loop"},
		};

		return Arrays.asList(tests);
	}

	@Before
	public void setup() {
		helpers.setup();

		helpers.state.debug = new DebugHandler(helpers.state) {
			private long time = System.currentTimeMillis();

			@Override
			public void poll() throws LuaError {
				if (System.currentTimeMillis() > time + 500) {
					time = System.currentTimeMillis();
					throw new LuaError("Timed out");
				}
			}

			@Override
			public void onInstruction(DebugState ds, DebugFrame di, int pc, Varargs extras, int top) throws LuaError {
				poll();
				super.onInstruction(ds, di, pc, extras, top);
			}
		};
	}

	@Test(timeout = 3000)
	public void run() throws IOException, CompileException, LuaError {
		helpers.loadScript(name).call(helpers.state);
	}
}
