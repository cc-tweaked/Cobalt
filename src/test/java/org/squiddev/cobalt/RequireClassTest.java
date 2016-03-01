package org.squiddev.cobalt;

import org.junit.Before;
import org.junit.Test;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;
import org.squiddev.cobalt.require.RequireSampleClassCastExcep;
import org.squiddev.cobalt.require.RequireSampleLoadLuaError;
import org.squiddev.cobalt.require.RequireSampleLoadRuntimeExcep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.squiddev.cobalt.Factory.valueOf;

public class RequireClassTest {

	private LuaValue require;
	private LuaState state;

	@Before
	public void setup() {
		state = new LuaState(new FileResourceManipulator());
		LuaTable globals = JsePlatform.standardGlobals(state);
		require = globals.get(state, "require");
	}

	@Test
	public void testRequireClassSuccess() {
		LuaValue result = require.call(state, Factory.valueOf("org.squiddev.cobalt.require.RequireSampleSuccess"));
		assertEquals("require-sample-success", result.tojstring());
		result = require.call(state, Factory.valueOf("org.squiddev.cobalt.require.RequireSampleSuccess"));
		assertEquals("require-sample-success", result.tojstring());
	}

	@Test
	public void testRequireClassLoadLuaError() {
		try {
			LuaValue result = require.call(state, Factory.valueOf(RequireSampleLoadLuaError.class.getName()));
			fail("incorrectly loaded class that threw lua error");
		} catch (LuaError le) {
			assertEquals(
				"sample-load-lua-error",
				le.getMessage());
		}
		try {
			LuaValue result = require.call(state, Factory.valueOf(RequireSampleLoadLuaError.class.getName()));
			fail("incorrectly loaded class that threw lua error");
		} catch (LuaError le) {
			assertEquals(
				"loop or previous error loading module '" + RequireSampleLoadLuaError.class.getName() + "'",
				le.getMessage());
		}
	}

	@Test
	public void testRequireClassLoadRuntimeException() {
		try {
			LuaValue result = require.call(state, Factory.valueOf(RequireSampleLoadRuntimeExcep.class.getName()));
			fail("incorrectly loaded class that threw runtime exception");
		} catch (RuntimeException le) {
			assertEquals(
				"sample-load-runtime-exception",
				le.getMessage());
		}
		try {
			LuaValue result = require.call(state, Factory.valueOf(RequireSampleLoadRuntimeExcep.class.getName()));
			fail("incorrectly loaded class that threw runtime exception");
		} catch (LuaError le) {
			assertEquals(
				"loop or previous error loading module '" + RequireSampleLoadRuntimeExcep.class.getName() + "'",
				le.getMessage());
		}
	}

	@Test
	public void testRequireClassClassCastException() {
		try {
			LuaValue result = require.call(state, Factory.valueOf(RequireSampleClassCastExcep.class.getName()));
			fail("incorrectly loaded class that threw class cast exception");
		} catch (LuaError le) {
			String msg = le.getMessage();
			if (!msg.contains("not found")) {
				fail("expected 'not found' message but got " + msg);
			}
		}
		try {
			LuaValue result = require.call(state, Factory.valueOf(RequireSampleClassCastExcep.class.getName()));
			fail("incorrectly loaded class that threw class cast exception");
		} catch (LuaError le) {
			String msg = le.getMessage();
			if (!msg.contains("not found")) {
				fail("expected 'not found' message but got " + msg);
			}
		}
	}
}
