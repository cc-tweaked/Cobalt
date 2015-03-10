package org.luaj.vm2.compiler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.Arrays;
import java.util.Collection;

/**
 * Framework to add regression tests as problem areas are found.
 */
@RunWith(Parameterized.class)
public class RegressionTests {
	public String fileName;

	public RegressionTests(String file) {
		fileName = file;
	}

	@Before
	public void setup() throws Exception {
		JsePlatform.standardGlobals();
	}

	@Test
	public void compareBytecode() throws Exception {
		CompileTestHelper.compareResults("/regressions/", fileName);
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		Object[][] tests = {
			{"modulo"},
			{"construct"},
			{"bigattr"},
			{"controlchars"},
			{"comparators"},
			{"mathrandomseed"},
			{"varargs"},
		};

		return Arrays.asList(tests);
	}
}
