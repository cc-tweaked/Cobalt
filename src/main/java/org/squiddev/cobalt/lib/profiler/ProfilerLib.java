package org.squiddev.cobalt.lib.profiler;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHook;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.OneArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * A Java implementation of LuaProfiler
 * <a href="https://github.com/luaforge/luaprofiler">Original implementation</a>
 */
public class ProfilerLib implements LuaLibrary {
	private ProfilerHook hook;
	private final OutputProvider provider;

	public ProfilerLib(OutputProvider provider) {
		this.provider = provider;
	}

	public ProfilerLib() {
		this.provider = new FileOutputProvider();
	}

	public interface OutputProvider {
		DataOutputStream createWriter(String name) throws IOException;
	}

	public static class FileOutputProvider implements OutputProvider {
		@Override
		public DataOutputStream createWriter(String name) throws IOException {
			return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name)));
		}
	}

	@Override
	public LuaValue add(LuaState state, LuaTable environment) {
		LuaTable profiler = new LuaTable();
		state.loadedPackages.rawset("profiler", profiler);
		environment.rawset("profiler", profiler);

		LibFunction.bind(
			state, profiler, Profiler1.class,
			new String[]{"milliTime", "nanoTime", "start", "stop", "pause", "resume"},
			ProfilerLib.class, this
		);

		return profiler;
	}

	private static class ProfilerHook implements DebugHook {
		private final ProfilerStack stack;
		private final ProfilerStream writer;
		private long count = 0;

		private ProfilerHook(ProfilerStack stack, DataOutputStream writer) {
			this.stack = stack;
			this.writer = new ProfilerStream(writer);
		}

		@Override
		public void onCall(LuaState state, DebugState ds, DebugFrame frame) {
			stack.enter(frame);
		}

		@Override
		public void onReturn(LuaState state, DebugState ds, DebugFrame dFrame) {
			ProfilerFrame frame = stack.leave(false);
			if (frame != null) {
				count++;
				try {
					frame.write(writer);
				} catch (IOException e) {
					throw new LuaError(e);
				}
			}
			stack.resume();
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

		public long close() {
			try {
				writer.close();
			} catch (IOException e) {
				throw new LuaError(e);
			}

			return count;
		}
	}

	private static class Profiler1 extends OneArgFunction {
		private final ProfilerLib lib;

		public Profiler1(ProfilerLib lib) {
			this.lib = lib;
		}

		@Override
		public LuaValue call(LuaState state, LuaValue arg) {
			switch (opcode) {
				case 0: // milliTime
					return valueOf(System.currentTimeMillis());
				case 1: // nanoTime
					return valueOf(System.nanoTime());
				case 2: // start
				{
					if (lib.hook != null) throw new LuaError("Already profiling");
					DataOutputStream writer;
					try {
						writer = lib.provider.createWriter(arg.checkString());
					} catch (IOException e) {
						throw new LuaError(e);
					}
					ProfilerHook hook = lib.hook = new ProfilerHook(new ProfilerStack(), writer);
					hook.setHook(state.debug.getDebugState());
					return NONE;
				}
				case 3: // stop
				{
					ProfilerHook hook = lib.hook;
					if (hook == null) throw new LuaError("Not profiling");
					long count = hook.close();
					hook.clearHook(state.debug.getDebugState());
					lib.hook = null;
					return valueOf(count);
				}
				default:
					return NONE;
			}
		}
	}
}
