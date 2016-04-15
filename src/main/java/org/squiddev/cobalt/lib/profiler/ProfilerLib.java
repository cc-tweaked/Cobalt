package org.squiddev.cobalt.lib.profiler;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHook;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.ZeroArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.util.ArrayList;
import java.util.List;

import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * A Java implementation of LuaProfiler
 * <a href="https://github.com/luaforge/luaprofiler">Original implementation</a>
 */
public class ProfilerLib implements LuaLibrary {
	private ProfilerHook hook;

	@Override
	public LuaValue add(LuaState state, LuaValue environment) {
		LuaTable profiler = new LuaTable();
		state.loadedPackages.rawset("profiler", profiler);
		environment.rawset("profiler", profiler);

		LibFunction.bind(
			state, profiler, Profiler0.class,
			new String[]{"milliTime", "nanoTime", "start", "stop", "pause", "resume"},
			ProfilerLib.class, this
		);

		return profiler;
	}

	private static class ProfilerHook implements DebugHook {
		private final ProfilerStack stack;
		private final List<ProfilerFrame> frames = new ArrayList<ProfilerFrame>();
		private boolean running = true;

		private ProfilerHook(ProfilerStack stack) {
			this.stack = stack;
		}

		@Override
		public void onCall(LuaState state, DebugState ds, DebugFrame frame) {
			stack.enter(frame);
		}

		@Override
		public void onReturn(LuaState state, DebugState ds, DebugFrame dFrame) {
			ProfilerFrame frame = stack.leave(true);
			if (frame != null) frames.add(frame);
		}

		@Override
		public void onCount(LuaState state, DebugState ds, DebugFrame frame) {
		}

		@Override
		public void onLine(LuaState state, DebugState ds, DebugFrame frame, int oldLine, int newLine) {
		}

		public void setHook(DebugState state) {
			state.setHook(this, true, false, true, -1);
		}

		public void clearHook(DebugState state) {
			state.setHook(null, false, false, false, -1);
		}

		public boolean running() {
			return running;
		}

		public void resume() {
			if (running) throw new LuaError("Already profiling");
			stack.resume();
			running = true;
		}

		public void pause() {
			if (!running) throw new LuaError("Already paused");
			stack.pause();
			running = false;
		}

		public LuaTable toTable() {
			LuaTable table = new LuaTable();
			int i = 1;
			for (ProfilerFrame frame : frames) {
				table.rawset(i++, frame.toTable());
			}
			return table;
		}
	}

	private static class Profiler0 extends ZeroArgFunction {
		private final ProfilerLib lib;

		public Profiler0(ProfilerLib lib) {
			this.lib = lib;
		}

		@Override
		public LuaValue call(LuaState state) {
			switch (opcode) {
				case 0: // milliTime
					return valueOf(System.currentTimeMillis());
				case 1: // nanoTime
					return valueOf(System.nanoTime());
				case 2: // start
				{
					if (lib.hook != null) throw new LuaError("Already profiling");
					ProfilerHook hook = lib.hook = new ProfilerHook(new ProfilerStack());
					hook.setHook(state.debug.getDebugState());
					return NONE;
				}
				case 3: // stop
				{
					ProfilerHook hook = lib.hook;
					if (hook == null) throw new LuaError("Not profiling");
					hook.clearHook(state.debug.getDebugState());
					if (hook.running()) hook.pause();
					lib.hook = null;
					return hook.toTable();
				}
				case 4: // pause
				{
					ProfilerHook hook = lib.hook;
					if (hook == null) throw new LuaError("Not profiling");
					hook.pause();
					hook.clearHook(state.debug.getDebugState());
					return NONE;
				}
				case 5: // resume
				{
					ProfilerHook hook = lib.hook;
					if (hook == null) throw new LuaError("Not profiling");
					hook.setHook(state.debug.getDebugState());
					hook.resume();
					return NONE;
				}
				default:
					return NONE;
			}
		}
	}
}
