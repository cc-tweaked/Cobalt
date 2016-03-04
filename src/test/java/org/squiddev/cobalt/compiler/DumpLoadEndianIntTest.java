package org.squiddev.cobalt.compiler;

import org.junit.Before;
import org.junit.Test;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.LuaInterpreter;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class DumpLoadEndianIntTest {
	private static final String SAVECHUNKS = "SAVECHUNKS";

	private static final boolean SHOULDPASS = true;
	private static final boolean SHOULDFAIL = false;
	private static final String mixedscript = "return tostring(1234)..'-#!-'..tostring(23.75)";
	private static final String intscript = "return tostring(1234)..'-#!-'..tostring(23)";
	private static final String withdoubles = "1234-#!-23.75";
	private static final String withints = "1234-#!-23";

	private LuaTable _G;
	private LuaState state;

	@Before
	public void setup() throws Exception {
		state = new LuaState(new FileResourceManipulator());
		_G = JsePlatform.standardGlobals(state);
		DumpState.ALLOW_INTEGER_CASTING = false;
	}

	@Test
	public void testBigDoubleCompile() {
		doTest(false, DumpState.NUMBER_FORMAT_FLOATS_OR_DOUBLES, false, mixedscript, withdoubles, withdoubles, SHOULDPASS);
		doTest(false, DumpState.NUMBER_FORMAT_FLOATS_OR_DOUBLES, true, mixedscript, withdoubles, withdoubles, SHOULDPASS);
	}

	@Test
	public void testLittleDoubleCompile() {
		doTest(true, DumpState.NUMBER_FORMAT_FLOATS_OR_DOUBLES, false, mixedscript, withdoubles, withdoubles, SHOULDPASS);
		doTest(true, DumpState.NUMBER_FORMAT_FLOATS_OR_DOUBLES, true, mixedscript, withdoubles, withdoubles, SHOULDPASS);
	}

	@Test
	public void testBigIntCompile() {
		DumpState.ALLOW_INTEGER_CASTING = true;
		doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, false, mixedscript, withdoubles, withints, SHOULDPASS);
		doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, true, mixedscript, withdoubles, withints, SHOULDPASS);
		DumpState.ALLOW_INTEGER_CASTING = false;
		doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, false, mixedscript, withdoubles, withints, SHOULDFAIL);
		doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, true, mixedscript, withdoubles, withints, SHOULDFAIL);
		doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, false, intscript, withints, withints, SHOULDPASS);
		doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, true, intscript, withints, withints, SHOULDPASS);
	}

	@Test
	public void testLittleIntCompile() {
		DumpState.ALLOW_INTEGER_CASTING = true;
		doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, false, mixedscript, withdoubles, withints, SHOULDPASS);
		doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, true, mixedscript, withdoubles, withints, SHOULDPASS);
		DumpState.ALLOW_INTEGER_CASTING = false;
		doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, false, mixedscript, withdoubles, withints, SHOULDFAIL);
		doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, true, mixedscript, withdoubles, withints, SHOULDFAIL);
		doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, false, intscript, withints, withints, SHOULDPASS);
		doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, true, intscript, withints, withints, SHOULDPASS);
	}

	@Test
	public void testBigNumpatchCompile() {
		doTest(false, DumpState.NUMBER_FORMAT_NUM_PATCH_INT32, false, mixedscript, withdoubles, withdoubles, SHOULDPASS);
		doTest(false, DumpState.NUMBER_FORMAT_NUM_PATCH_INT32, true, mixedscript, withdoubles, withdoubles, SHOULDPASS);
	}

	@Test
	public void testLittleNumpatchCompile() {
		doTest(true, DumpState.NUMBER_FORMAT_NUM_PATCH_INT32, false, mixedscript, withdoubles, withdoubles, SHOULDPASS);
		doTest(true, DumpState.NUMBER_FORMAT_NUM_PATCH_INT32, true, mixedscript, withdoubles, withdoubles, SHOULDPASS);
	}

	public void doTest(boolean littleEndian, int numberFormat, boolean stripDebug,
	                   String script, String expectedPriorDump, String expectedPostDump, boolean shouldPass) {
		try {

			// compile into prototype
			InputStream is = new ByteArrayInputStream(script.getBytes());
			Prototype p = LuaC.compile(is, "script");

			// double check script result before dumping
			LuaFunction f = new LuaInterpreter(p, _G);
			LuaValue r = f.call(state);
			String actual = r.toString();
			assertEquals(expectedPriorDump, actual);

			// dump into bytes
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				DumpState.dump(p, baos, stripDebug, numberFormat, littleEndian);
				if (!shouldPass) {
					fail("dump should not have succeeded");
				}
			} catch (Exception e) {
				if (shouldPass) {
					fail("dump threw " + e);
				} else {
					return;
				}
			}
			byte[] dumped = baos.toByteArray();

			// load again using compiler
			is = new ByteArrayInputStream(dumped);
			f = LoadState.load(state, is, "dumped", _G);
			r = f.call(state);
			actual = r.toString();
			assertEquals(expectedPostDump, actual);

			// write test chunk
			if (System.getProperty(SAVECHUNKS) != null && script.equals(mixedscript)) {
				new File("build").mkdirs();
				String filename = "build/test-"
					+ (littleEndian ? "little-" : "big-")
					+ (numberFormat == DumpState.NUMBER_FORMAT_FLOATS_OR_DOUBLES ? "double-" :
					numberFormat == DumpState.NUMBER_FORMAT_INTS_ONLY ? "int-" :
						numberFormat == DumpState.NUMBER_FORMAT_NUM_PATCH_INT32 ? "numpatch4-" : "???-")
					+ (stripDebug ? "nodebug-" : "debug-")
					+ "bin.lua";
				FileOutputStream fos = new FileOutputStream(filename);
				fos.write(dumped);
				fos.close();
			}

		} catch (IOException e) {
			fail(e.toString());
		}
	}
}
