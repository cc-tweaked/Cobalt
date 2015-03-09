package org.luaj.vm2.compiler;

import junit.framework.TestCase;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.Print;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.*;

abstract public class AbstractUnitTests extends TestCase {
	public final String dir;

	public AbstractUnitTests(String dir) {
		this.dir = "/" + dir + "/";
	}
	protected void setUp() throws Exception {
		super.setUp();
		JsePlatform.standardGlobals();
	}

	protected void doTest(String file) {
		try {
			// load source from jar
			byte[] lua = bytesFromJar(file);

			// compile in memory
			InputStream is = new ByteArrayInputStream(lua);
			Prototype p = LuaC.compile(is, "@" + file);
			String actual = protoToString(p);

			// load expected value from jar
			byte[] luac = bytesFromJar(file.substring(0, file.length() - 4) + ".lc");
			Prototype e = loadFromBytes(luac, file);
			String expected = protoToString(e);

			// compare results
			assertEquals(expected, actual);

			// dump into memory
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DumpState.dump(p, baos, false);
			byte[] dumped = baos.toByteArray();

			// re-undump
			Prototype p2 = loadFromBytes(dumped, file);
			String actual2 = protoToString(p2);

			// compare again
			assertEquals(actual, actual2);

		} catch (IOException e) {
			fail(e.toString());
		}
	}

	protected byte[] bytesFromJar(String path) throws IOException {
		InputStream is = getClass().getResourceAsStream(dir + path);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[2048];
		int n;
		while ((n = is.read(buffer)) >= 0)
			baos.write(buffer, 0, n);
		is.close();
		return baos.toByteArray();
	}

	protected Prototype loadFromBytes(byte[] bytes, String script)
		throws IOException {
		InputStream is = new ByteArrayInputStream(bytes);
		return LoadState.loadBinaryChunk(is.read(), is, script);
	}

	protected String protoToString(Prototype p) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Print.ps = new PrintStream(baos);
		Print.printFunction(p, true);
		return baos.toString();
	}

}
