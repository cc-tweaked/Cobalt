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

import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.lib.system.ResourceLoader;
import org.squiddev.cobalt.lib.system.SystemLibraries;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ScriptHelper {
	private final String subdir;
	public LuaState state;
	public LuaTable globals;

	private final DelegatingOutputStream stdout = new DelegatingOutputStream(System.out);
	private final PrintStream stdoutStream = new PrintStream(stdout);

	public ScriptHelper(String subdir) {
		this.subdir = subdir;
	}

	public void setup() throws LuaError {
		setup(x -> {
		});
	}

	public void setup(Consumer<LuaState.Builder> extend) throws LuaError {
		LuaState.Builder builder = LuaState.builder();
		extend.accept(builder);
		setupCommon(builder.build());
	}

	public void setupQuiet() throws LuaError {
		setupCommon(new LuaState());
		stdout.setOut(new VoidOutputStream());
	}

	private void setupCommon(LuaState state) throws LuaError {
		this.state = state;
		globals = SystemLibraries.debugGlobals(state, this::load, new VoidInputStream(), stdoutStream);
		globals.rawset("id_", LibFunction.createV((state$, args) -> args));
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	private InputStream load(String filename) {
		{
			InputStream stream = getClass().getResourceAsStream(subdir + filename);
			if (stream != null) return stream;
		}
		{
			InputStream stream = getClass().getResourceAsStream("/" + filename);
			if (stream != null) return stream;
		}

		return ResourceLoader.FILES.load(filename);
	}

	/**
	 * Runs a test and compares the output
	 *
	 * @param testName The name of the test file to run
	 */
	public void runComparisonTest(String testName) throws Exception {
		// Redirect our stdout!
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		final OutputStream oldOutput = stdout.getOut();
		stdout.setOut(output);

		// Run the script
		try {
			LuaThread.runMain(state, loadScript(testName));

			stdoutStream.flush();
			String actualOutput = output.toString();
			String expectedOutput = getExpectedOutput(testName);
			actualOutput = actualOutput.replaceAll("\r\n", "\n");
			expectedOutput = expectedOutput.replaceAll("\r\n", "\n");

			if (Boolean.getBoolean("cobalt.update") && !expectedOutput.equals(actualOutput)) {
				Files.writeString(Path.of("src/test/resources", subdir, testName + ".out"), actualOutput);
			}

			assertEquals(expectedOutput, actualOutput);
		} catch (LuaError e) {
			System.out.println(output);
			throw e;
		} finally {
			stdout.setOut(oldOutput);
		}
	}

	/**
	 * Loads a script into the global table
	 *
	 * @param name The name of the file
	 * @return The loaded LuaFunction
	 */
	public LuaClosure loadScript(String name) throws IOException, CompileException, LuaError {
		try (InputStream script = load(name + ".lua")) {
			if (script == null) fail("Could not load script for test case: " + name);
			return LoadState.load(state, script, "@" + name + ".lua", globals);
		}
	}

	public void runWithDump(String script) throws LuaError, IOException, CompileException {
		runWithDump(loadScript(script));
	}

	public void runWithDump(LuaClosure function) throws LuaError {
		try {
			LuaThread.runMain(state, function);
		} catch (LuaError e) {
			DebugState debug = state.getCurrentThread().getDebugState();
			int level = 0;
			while (true) {
				DebugFrame frame = debug.getFrame(level++);
				if (frame == null) break;

				System.out.printf("%d %s\n", level, frame.func.debugName());

				if (frame.closure == null || frame.stack == null) continue;
				Prototype proto = frame.closure.getPrototype();
				for (int local = 0; local < proto.maxStackSize; local++) {
					LuaString name = frame.getLocalName(local + 1);
					if (name != null) System.out.printf("  %02x | %10s = %s\n", local, name, frame.stack[local]);
				}
			}
			throw e;
		}
	}

	private String getExpectedOutput(final String name) throws IOException {
		InputStream output = load(name + ".out");
		if (output == null) fail("Failed to get comparison output for " + name);
		try {
			return readString(output);
		} finally {
			output.close();
		}
	}

	private String readString(InputStream is) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int r;
		while ((r = is.read(buf)) >= 0) {
			outputStream.write(buf, 0, r);
		}
		return outputStream.toString();
	}

	private static class VoidOutputStream extends OutputStream {
		@Override
		public void write(int b) {
		}

		@Override
		public void write(byte[] bytes, int off, int len) {
		}
	}

	private static class VoidInputStream extends InputStream {
		@Override
		public int read() {
			return -1;
		}
	}

	private static class DelegatingOutputStream extends FilterOutputStream {
		public DelegatingOutputStream(OutputStream output) {
			super(output);
		}

		public OutputStream getOut() {
			return out;
		}

		public void setOut(OutputStream out) {
			this.out = out;
		}
	}
}
