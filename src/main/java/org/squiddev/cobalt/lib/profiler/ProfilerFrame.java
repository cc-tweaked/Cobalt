package org.squiddev.cobalt.lib.profiler;

import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.lib.DebugLib;

import java.io.DataOutputStream;
import java.io.IOException;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Stores information about the profiler
 */
public class ProfilerFrame {
	public long functionLocalTimeMarker;
	public long functionTotalTimeMarker;
	public long functionLocalTime;
	public long functionTotalTime;

	public final short level;
	private LuaString functionName;
	private LuaString sourceCode;
	private int lineDefined;
	private int lastDefined;

	public ProfilerFrame(short level) {
		this.level = level;
	}

	public void setup(DebugFrame frame) {
		LuaString[] string = frame.getFuncKind();
		functionName = string == null ? DebugLib.QMARK : string[0];


		if (frame.closure == null) {
			sourceCode = valueOf(frame.func.debugName());
			lineDefined = -1;
			lastDefined = -1;
		} else {
			Prototype proto = frame.closure.getPrototype();
			sourceCode = proto.sourceShort();
			lineDefined = proto.linedefined;
			lastDefined = proto.lastlinedefined;
		}
	}

	public void computeLocalTime(long time) {
		functionLocalTime += (time - functionLocalTimeMarker);
	}

	public void computeTotalTime(long time) {
		functionTotalTime += (time - functionTotalTimeMarker);
	}

	public void write(DataOutputStream stream) throws IOException {
		stream.writeShort(level);
		stream.writeInt(sourceCode.length);
		stream.write(sourceCode.bytes, sourceCode.offset, sourceCode.length);
		stream.writeInt(functionName.length);
		stream.write(functionName.bytes, functionName.offset, functionName.length);
		stream.writeInt(lineDefined);
		stream.writeInt(lastDefined);
		stream.writeLong(functionLocalTime);
		stream.writeLong(functionTotalTime);
	}
}
