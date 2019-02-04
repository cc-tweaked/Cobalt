package org.squiddev.cobalt;

import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.lib.profiler.ProfilerLib;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.fail;

/**
 * Runs the profiler. Not a unit test.
 */
public class ProfilerTest {
	public static void main(String[] args) throws IOException, CompileException, LuaError {
		LuaState state = new LuaState();
		LuaTable env = JsePlatform.debugGlobals(state);
		env.load(state, new ProfilerLib(new ProfilerLib.FileOutputProvider()));

		String name = "profiler.lua";
		InputStream script = ProfilerTest.class.getResourceAsStream("/" + name);
		if (script == null) fail("Could not load script for test case: " + name);
		try {
			LuaFunction function = LoadState.load(state, script, "@" + name, env);
			LuaThread.runMain(state, function);
		} finally {
			script.close();
		}
	}
}
