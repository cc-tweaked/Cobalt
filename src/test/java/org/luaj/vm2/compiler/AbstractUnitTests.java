package org.luaj.vm2.compiler;

import junit.framework.TestCase;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.Print;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

abstract public class AbstractUnitTests extends TestCase {

	private final String dir;
	private final String jar;
	private LuaTable _G;

	public AbstractUnitTests(String zipdir, String zipfile, String dir) {
		URL zip = null;
		zip = getClass().getResource("/" + zipfile);
		if (zip == null) {
			File file = new File(zipdir + "/" + zipfile);
			try {
				if (file.exists())
					zip = file.toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		if (zip == null)
			throw new RuntimeException("not found: " + zipfile);
		this.jar = "jar:" + zip.toExternalForm() + "!/";
		this.dir = dir;
	}

	protected void setUp() throws Exception {
		super.setUp();
		_G = JsePlatform.standardGlobals();
	}

	protected String pathOfFile(String file) {
		return jar + dir + "/" + file;
	}

	protected InputStream inputStreamOfPath(String path) throws IOException {
		URL url = new URL(path);
		return url.openStream();
	}

	protected InputStream inputStreamOfFile(String file) throws IOException {
		return inputStreamOfPath(pathOfFile(file));
	}

	protected void doTest(String file) {
		try {
			// load source from jar
			String path = pathOfFile(file);
			byte[] lua = bytesFromJar(path);

			// compile in memory
			InputStream is = new ByteArrayInputStream(lua);
			Prototype p = LuaC.compile(is, "@" + file);
			String actual = protoToString(p);

			// load expected value from jar
			byte[] luac = bytesFromJar(path.substring(0, path.length() - 4) + ".lc");
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
		InputStream is = inputStreamOfPath(path);
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
