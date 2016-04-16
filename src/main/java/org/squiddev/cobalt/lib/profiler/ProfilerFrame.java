package org.squiddev.cobalt.lib.profiler;

import org.squiddev.cobalt.debug.DebugFrame;

import java.io.IOException;

/**
 * Stores information about the profiler
 */
public class ProfilerFrame {
	public long functionLocalTimeMarker;
	public long functionTotalTimeMarker;
	public long functionLocalTime;
	public long functionTotalTime;

	public final short level;
	private DebugFrame frame;

	public ProfilerFrame(short level) {
		this.level = level;
	}

	public void setup(DebugFrame frame) {
		functionTotalTime = 0;
		functionLocalTime = 0;
		this.frame = frame;
	}

	public void computeLocalTime(long time) {
		functionLocalTime += (time - functionLocalTimeMarker);
	}

	public void computeTotalTime(long time) {
		functionTotalTime += (time - functionTotalTimeMarker);
	}

	public void write(ProfilerStream stream) throws IOException {
		if (frame.closure == null) {
			stream.writeReturn(level, frame.func.debugName(), functionLocalTime, functionTotalTime);
		} else {
			short id = stream.getPrototype(frame.closure.getPrototype());
			stream.writeProtoReturn(level, id, functionLocalTime, functionTotalTime);
		}
	}
}
