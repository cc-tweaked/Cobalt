package org.squiddev.cobalt.table;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.ScriptDrivenHelpers;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.compiler.CompileException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Tests protection
 */
@RunWith(Parameterized.class)
public class TableLuaTest {
	private final String name;
	private ScriptDrivenHelpers helpers;

	public TableLuaTest(String name) {
		this.name = name;
		this.helpers = new ScriptDrivenHelpers("/table/");
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"table-hash-01"},
			{"table-hash-02"},
			{"table-hash-03"},
		};

		return Arrays.asList(tests);
	}

	@Before
	public void setup() {
		helpers.setup();
	}

	@Test
	public void run() throws IOException, CompileException, LuaError, UnwindThrowable {
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}
}
