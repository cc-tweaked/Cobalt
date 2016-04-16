package org.squiddev.cobalt.lib.profiler;

import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.Prototype;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Stream that handles writing prototypes and profile data
 *
 * The output file is composed of blocks.
 * <pre>
 * Each block starts with a single byte designating which kind it is:
 *  0x00: This defines a prototype.
 *      Id         2 bytes  The id of this prototype
 *      NameLength 4 bytes  The length of the prototype's name
 *      Name       variable The prototype's name
 *      FirstLine  4 bytes  First line this prototype is defined
 *      LastLine   4 bytes  Last line this prototype is defined
 *  0x01: A prototype call
 *      Level      2 bytes  The call stack level
 *      Id         2 bytes  The prototype's id
 *      LocalTime  8 bytes  Time taken within this function excluding other calls
 *      TotalTime  8 bytes  Time taken within this function including other calls
 *  0x02: A Java function call
 *      Level       2 bytes  The call stack level
 *      NameLength 4 bytes  The length of the prototype's name
 *      Name       variable The prototype's name
 *      LocalTime  8 bytes  Time taken within this function excluding other calls
 *      TotalTime  8 bytes  Time taken within this function including other calls
 * </pre>
 */
public class ProfilerStream {
	private final byte PROTOTYPE = 0x00;
	private final byte CALL_PROTO = 0x01;
	private final byte CALL_FUNC = 0x02;

	private final HashMap<Prototype, Short> prototypes = new HashMap<Prototype, Short>();
	private short id = -1;
	private final DataOutputStream stream;

	public ProfilerStream(DataOutputStream stream) {
		this.stream = stream;
	}

	public short getPrototype(Prototype prototype) throws IOException {
		Short cacheId = prototypes.get(prototype);
		if (cacheId != null) return cacheId;

		short newId = ++id;
		prototypes.put(prototype, newId);
		writePrototype(prototype, newId);
		return newId;
	}

	public void writeReturn(short level, String name, long local, long total) throws IOException {
		DataOutputStream stream = this.stream;
		stream.write(CALL_FUNC);
		stream.writeShort(level);
		stream.writeInt(name.length());
		stream.writeBytes(name);
		stream.writeLong(local);
		stream.writeLong(total);
	}

	public void writeProtoReturn(short level, short id, long local, long total) throws IOException {
		DataOutputStream stream = this.stream;
		stream.write(CALL_PROTO);
		stream.writeShort(level);
		stream.writeShort(id);
		stream.writeLong(local);
		stream.writeLong(total);
	}

	private void writePrototype(Prototype prototype, short id) throws IOException {
		DataOutputStream stream = this.stream;
		stream.write(PROTOTYPE);
		stream.writeShort(id);

		LuaString sourceCode = prototype.sourceShort();

		stream.writeInt(sourceCode.length);
		stream.write(sourceCode.bytes, sourceCode.offset, sourceCode.length);
		stream.writeInt(prototype.linedefined);
		stream.writeInt(prototype.lastlinedefined);
	}

	public void close() throws IOException {
		stream.close();
	}
}
