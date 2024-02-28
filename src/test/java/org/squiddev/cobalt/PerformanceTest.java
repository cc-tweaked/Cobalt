/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.squiddev.cobalt.compiler.CompileException;

import java.io.IOException;

@Disabled("Doesn't really test anything useful")
public class PerformanceTest {
	private static final int TOTAL = Integer.parseInt(System.getProperty("cobalt.perfTotal", "1"));
	private static final int DISCARD = Integer.parseInt(System.getProperty("cobalt.perfDiscard", "0"));

	private ScriptHelper helpers;

	@BeforeEach
	public void setup() throws LuaError {
		helpers = new ScriptHelper("/perf/");
		helpers.setupQuiet();
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {"binarytrees", "fannkuch", "nbody", "nsieve", "primes"})
	public void run(String name) throws IOException, CompileException, LuaError, InterruptedException {
		System.out.println("[" + name + "]");

		for (int i = 0; i < TOTAL; i++) {
			long start = System.nanoTime();
			LuaThread.runMain(helpers.state, helpers.loadScript(name));
			long finish = System.nanoTime();

			if (i >= DISCARD) System.out.println("  Took " + (finish - start) / 1.0e9);
		}
	}
}
