package org.squiddev.cobalt.lib.profiler;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.ZeroArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * A Java implementation of LuaProfiler
 * <a href="https://github.com/luaforge/luaprofiler">Original implementation</a>
 */
public class ProfilerLib implements LuaLibrary {
	@Override
	public LuaValue add(LuaState state, LuaValue environment) {
		LuaTable profiler = new LuaTable();
		state.loadedPackages.rawset("profiler", profiler);
		environment.rawset("profile", profiler);

		LibFunction.bind(state, environment, Profiler0.class, new String[]{"milliTime", "nanoTime", "start", "stop"});

		return profiler;
	}

	private static class Profiler0 extends ZeroArgFunction {
		@Override
		public LuaValue call(LuaState state) {
			switch (opcode) {
				case 0: // milliTime
					return valueOf(System.currentTimeMillis());
				case 1: // nanoTime
					return valueOf(System.nanoTime());
				case 2: // start
					return NONE;
				case 3: // stop
					return NONE;
				default:
					return NONE;
			}
		}
	}
}
