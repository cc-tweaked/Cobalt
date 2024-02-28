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
package org.squiddev.cobalt.compiler;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Print;
import org.squiddev.cobalt.Prototype;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Compiles Lua's test files to bytecode and asserts that it is equal to a golden file produced by luac.
 */
public class CompilerUnitTests {
	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"all", "api", "attrib", "big", "calls", "checktable", "closure", "code", "constructs", "db", "errors",
		"events", "files", "gc", "literals", "locals", "main", "math", "nextvar", "pm", "sort", "strings", "vararg",
	})
	public void lua51(String filename) throws IOException, CompileException, LuaError {
		compareResults("/bytecode-compiler/lua5.1/", filename);
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"modulo", "construct", "bigattr", "controlchars", "comparators", "mathrandomseed", "varargs",
		"multi-assign",
	})
	public void regression(String filename) throws IOException, CompileException, LuaError {
		compareResults("/bytecode-compiler/regressions/", filename);
	}

	private static void compareResults(String dir, String file) throws IOException, CompileException, LuaError {
		var state = LuaState.builder().bytecodeFormat(LuaBytecodeFormat.instance()).build();

		// Compile source file
		Prototype sourcePrototype = LuaC.compile(state, CompilerUnitTests.class.getResourceAsStream(dir + file + ".lua"), "@" + file + ".lua");
		String sourceBytecode = dumpState(sourcePrototype);

		// Load expected value from jar
		Prototype expectedPrototype = LuaC.compile(state, CompilerUnitTests.class.getResourceAsStream(dir + file + ".lc"), file + ".lua");
		String expectedBytecode = dumpState(expectedPrototype);

		if (Boolean.getBoolean("cobalt.update") && !expectedBytecode.equals(sourceBytecode)) {
			try (OutputStream output = Files.newOutputStream(Paths.get("src/test/resources" + dir + file + ".lc"))) {
				BytecodeDumper.dump(sourcePrototype, output, false);
			}
		}

		assertEquals(expectedBytecode, sourceBytecode);

		// Round-trip the bytecode
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		BytecodeDumper.dump(expectedPrototype, outputStream, false);
		String redumpBytecode = dumpState(LuaC.compile(state, new ByteArrayInputStream(outputStream.toByteArray()), file + ".lua"));

		// compare again
		assertEquals(sourceBytecode, redumpBytecode);
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"goto-close-1",
		"goto-close-2",
		"goto-if-1",
		"goto-if-2",
		"goto-nil",
	})
	public void listings(String file) throws IOException, CompileException, LuaError {
		String dir = "/bytecode-compiler/listing/";
		var state = LuaState.builder().build();

		// Compile source file
		Prototype sourcePrototype = LuaC.compile(state, CompilerUnitTests.class.getResourceAsStream(dir + file + ".lua"), "@" + file + ".lua");
		String sourceBytecode = dumpState(sourcePrototype);

		// Load expected value from jar
		var expectedFile = Paths.get("src/test/resources" + dir + file + ".lc");
		String expectedBytecode = Files.readString(expectedFile);

		if (Boolean.getBoolean("cobalt.update") && !expectedBytecode.equals(sourceBytecode)) {
			Files.writeString(expectedFile, sourceBytecode);
		}
		assertEquals(expectedBytecode, sourceBytecode);
	}

	private static String dumpState(Prototype p) {
		StringBuilder output = new StringBuilder();
		Print.printFunction(output, p, true, false);
		return output.toString();
	}
}
