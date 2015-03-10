/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package org.luaj.vm2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Compatibility tests for the LuaJ VM
 * <p>
 * Results are compared for exact match with
 * the installed C-based lua environment.
 */
public class CompatibilityTest {

	/**
	 * A base class for compatibility tests. This is awful design but meh.
	 */
	private static class CompatibilityTestHelpers {
		private LuaValue savedStringMetatable;

		protected String name;
		protected ScriptDrivenHelpers helpers;

		public CompatibilityTestHelpers(String name, ScriptDrivenHelpers.PlatformType platform) {
			helpers = new ScriptDrivenHelpers(platform, "/");
			this.name = name;
		}

		public static Collection<Object[]> getTests() {
			Object[][] tests = {
				{"baselib"},
				{"coroutinelib"},
				{"debuglib"},
				{"iolib"},
				{"manyupvals"},
				{"mathlib"},
				{"metatags"},
				{"oslib"},
				{"stringlib"},
				{"tablelib"},
				{"tailcalls"},
				{"upvalues"},
				{"vm"},

			};

			return Arrays.asList(tests);
		}

		@Before
		public void setup() {
			helpers.setup();
			savedStringMetatable = LuaString.s_metatable;
		}

		@After
		public void tearDown() {
			LuaNil.s_metatable = null;
			LuaBoolean.s_metatable = null;
			LuaNumber.s_metatable = null;
			LuaFunction.s_metatable = null;
			LuaThread.s_metatable = null;
			LuaString.s_metatable = savedStringMetatable;
		}
	}

	@RunWith(Parameterized.class)
	public static class JseCompatibilityTest extends CompatibilityTestHelpers {
		public JseCompatibilityTest(String name) {
			super(name, ScriptDrivenHelpers.PlatformType.JSE);
		}

		@Test
		public void testOutput() throws Exception {
			helpers.runTest(name);
		}

		@Parameterized.Parameters(name = "{0}")
		public static Collection<Object[]> getTests() {
			return CompatibilityTestHelpers.getTests();
		}
	}

	@RunWith(Parameterized.class)
	public static class LuaJCCompatibilityTest extends CompatibilityTestHelpers {
		public LuaJCCompatibilityTest(String name) {
			super(name, ScriptDrivenHelpers.PlatformType.JSE);
		}

		@Test
		public void testOutput() throws Exception {
			helpers.runTest(name);
		}

		@Parameterized.Parameters(name = "{0}")
		public static Collection<Object[]> getTests() {
			return CompatibilityTestHelpers.getTests().stream()
				.filter(s -> (
					!s[0].equals("debuglib") &&
						!s[0].equals("manyupvals") // Issues with setfenv
					// !s[0].equals("stringlib")
				)).collect(Collectors.toList());
		}
	}
}
