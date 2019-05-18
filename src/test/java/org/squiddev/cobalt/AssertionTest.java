package org.squiddev.cobalt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.VarArgFunction;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.TimeZone;

/**
 * Lua driven assertion tests
 */
@RunWith(Parameterized.class)
public class AssertionTest {
	private final String name;
	private ScriptDrivenHelpers helpers;

	public AssertionTest(String name) {
		this.name = name;
		this.helpers = new ScriptDrivenHelpers("/assert/");
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"baseIssues"},
			{"stringIssues"},
			{"debug"},
			{"debug-coroutine-hook"},
			{"gc"},
			{"immutable"},
			{"invalid-tailcall"},
			{"load-error"},
			{"no-unwind"},
			{"time"}
		};

		return Arrays.asList(tests);
	}

	@Before
	public void setup() {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));

		helpers.setup();
		helpers.globals.rawset("id_", new VarArgFunction() {
			@Override
			public Varargs invoke(LuaState state, Varargs args) {
				return args;
			}
		});
	}

	@Test
	public void run() throws IOException, CompileException, LuaError, InterruptedException {
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}
}
