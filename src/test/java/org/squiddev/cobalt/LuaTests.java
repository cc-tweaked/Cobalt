package org.squiddev.cobalt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.OneArgFunction;
import org.squiddev.cobalt.function.ZeroArgFunction;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Lua driven assertion tests
 */
@RunWith(Parameterized.class)
public class LuaTests {
	private final String name;
	private ScriptDrivenHelpers helpers;

	public LuaTests(String name) {
		this.name = name;
		this.helpers = new ScriptDrivenHelpers("/lua5.1/");
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			// {"all"},
			// {"api"}, // Used to test C API.
			{"attrib"},
			// {"big"},
			{"calls"},
			{"checktable"},
			{"closure"},
			{"code"},
			{"constructs"},
			{"db"},
			{"errors"},
			{"events"},
			{"files"},
			{"gc"},
			{"literals"},
			{"locals"},
			// {"main"}, // Tests Lua flags
			{"math"},
			{"nextvar"},
			{"pm"},
			{"sort"},
			{"strings"},
			{"vararg"},
			{"verybig"},
		};

		return Arrays.asList(tests);
	}

	@Before
	public void setup() throws LuaError {
		helpers.setup();
		helpers.globals.rawset("mkdir", new OneArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
				return valueOf(new File(arg.checkString()).mkdirs());
			}
		});
		helpers.globals.rawget("debug").set(helpers.state, "debug", new ZeroArgFunction() {
			@Override
			public LuaValue call(LuaState state) {
				// Insert breakpoint here
				return Constants.NONE;
			}
		});
	}

	@Test(timeout = 10000)
	public void run() throws IOException, CompileException, LuaError {
		helpers.loadScript(name).call(helpers.state);
	}
}
