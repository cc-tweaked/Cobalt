package org.luaj.vm2.compiler;

import org.luaj.vm2.LoadState;
import org.luaj.vm2.Print;
import org.luaj.vm2.Prototype;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class CompileTestHelper {
	/**
	 * Compiled and compares the bytecode
	 *
	 * @param file The path of the file to use
	 * @throws IOException
	 */
	public static void compareResults(String dir, String file) throws IOException {
		// Compile in memory
		String sourceBytecode = protoToString(LuaC.compile(new ByteArrayInputStream(bytesFromJar(dir + file + ".lua")), "@" + file + ".lua"));

		// Load expected value from jar
		Prototype expectedPrototype = loadFromBytes(bytesFromJar(dir + file + ".lc"), file + ".lua");
		String expectedBytecode = protoToString(expectedPrototype);

		// compare results
		assertEquals(expectedBytecode, sourceBytecode);

		// Redo bytecode
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		DumpState.dump(expectedPrototype, outputStream, false);
		String redumpBytecode = protoToString(loadFromBytes(outputStream.toByteArray(), file + ".lua"));

		// compare again
		assertEquals(sourceBytecode, redumpBytecode);
	}

	/**
	 * Read bytes from a resource
	 *
	 * @param path The path to the resource
	 * @return A byte array containing the file
	 */
	private static byte[] bytesFromJar(String path) throws IOException {
		InputStream is = CompileTestHelper.class.getResourceAsStream(path);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[2048];

		int n;
		while ((n = is.read(buffer)) >= 0) outputStream.write(buffer, 0, n);
		is.close();

		return outputStream.toByteArray();
	}

	/**
	 * Load a file from the bytes
	 *
	 * @param bytes  The bytes to load from
	 * @param script The name of the file to use
	 * @return A Prototype from the compiled bytecode
	 */
	private static Prototype loadFromBytes(byte[] bytes, String script) throws IOException {
		InputStream is = new ByteArrayInputStream(bytes);
		return LoadState.loadBinaryChunk(is.read(), is, script);
	}

	/**
	 * Pretty print a prototype as a String
	 *
	 * @param p The prototype to use
	 * @return String containing a pretty version of the prototype
	 */
	private static String protoToString(Prototype p) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Print.ps = new PrintStream(outputStream);
		Print.printFunction(p, true);
		return outputStream.toString();
	}
}
