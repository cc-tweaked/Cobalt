package org.squiddev.cobalt.persist;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LocalVariable;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.Upvalue;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.persist.Persist.*;

public class ValueWriter implements Closeable {
	private static final int NO_WRITE = -1;
	static final int MAX_DEPTH = 16;

	private final DataOutput output;

	private final HashMap<Object, Integer> ids = new HashMap<>();
	private final LuaState state;
	private int nextId = 0;
	private int depth = 0;

	private final Queue<Update> queue = new ArrayDeque<>();

	public ValueWriter(LuaState state, DataOutput output) {
		this.output = output;
		this.state = state;
	}

	private int checkWritten(Object object, int type) throws IOException {
		Integer index = ids.get(object);
		if (index == null) {
			int id = nextId++;
			ids.put(object, id);
			writeByte(type);
			return id;
		} else {
			writeByte(TAG_REFERENCE);
			writeVarInt(index);
			return NO_WRITE;
		}
	}

	private int checkWrittenPartial(Object object, int type) throws IOException {
		boolean skip = depth > MAX_DEPTH;
		int id = checkWritten(object, (skip ? FLAG_PARTIAL : 0) | type);
		if (skip && id != NO_WRITE) {
			queue.add(new Update(id, object, type));
			return NO_WRITE;
		}

		return id;
	}

	public void write(LuaValue value) throws IOException {
		switch (value.type()) {
			case TNIL:
				writeByte(TAG_NIL);
				break;
			case TBOOLEAN:
				writeByte(((LuaBoolean) value).v ? TAG_TRUE : TAG_FALSE);
				break;

			case TNUMBER: {
				if (value instanceof LuaInteger) {
					int v = ((LuaInteger) value).v;
					if (v >= Short.MIN_VALUE && v <= Short.MIN_VALUE) {
						writeByte(TAG_SHORT_INT);
						writeShort(v);
					} else {
						writeByte(TAG_INT);
						writeInt(v);
					}
				} else {
					writeByte(TAG_FLOAT);
					writeDouble(value.toDouble());
				}
				break;
			}

			case TSTRING: {
				LuaString str = ((LuaBaseString) value).strvalue();
				if (str.length <= 24) {
					writeByte(TAG_SHORT_STRING);
					writeByte(str.length);
					write(str.bytes, str.offset, str.length);
				} else {
					if (checkWritten(str, TAG_STRING) == NO_WRITE) return;
					writeVarInt(str.length);
					write(str.bytes, str.offset, str.length);
				}
				break;
			}

			case TTABLE:
				if (checkWrittenPartial(value, TAG_TABLE) == NO_WRITE) return;
				writeTableBody((LuaTable) value);
				break;

			case TTHREAD:
				if (checkWrittenPartial(value, TAG_THREAD) == NO_WRITE) return;
				writeThreadBody((LuaThread) value);
				break;


			case TFUNCTION:
				if (value instanceof LuaClosure) {
					LuaClosure closure = (LuaClosure) value;
					if (checkWritten(closure, TAG_CLOSURE) == NO_WRITE) return;

					write(closure.getPrototype());

					int nups = closure.getPrototype().nups;
					writeVarInt(nups);

					write(closure.getfenv());
					for (int i = 0; i < nups; i++) write(closure.getUpvalue(i));

					break;
				}

				// fallthrough
			default:
				serialize(value);
				break;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void serialize(Object value) throws IOException {
		if (checkWritten(value, TAG_SERIALIZED) == NO_WRITE) return;

		Serializer serializer = Serializable.getSerializer(value);
		if (serializer == null) {
			throw new IllegalStateException(String.format("Cannot serialize %s (of type %s)", value, value.getClass().getName()));
		}

		if (checkWritten(serializer, TAG_SERIALIZER) != NO_WRITE) write(serializer.getName());

		depth++;
		serializer.save(this, (Serializable) value);
		depth--;
	}

	public void write(Varargs value) throws IOException {
		int count = value == null ? 0 : value.count();
		writeVarInt(count);
		for (int i = 0; i < count; i++) write(value.arg(i + 1));
	}

	private void writeTableBody(LuaTable table) throws IOException {
		depth++;
		{
			LuaValue key = NIL;
			try {
				while (true) {
					Varargs pair = table.next(key);
					key = pair.arg(1);
					if (key.isNil()) break;

					write(key);
					write(pair.arg(2));
				}
			} catch (LuaError e) {
				throw new IOException(e);
			}

			writeByte(TAG_NIL);
			LuaTable metatable = table.getMetatable(state);
			if (metatable == null) {
				writeByte(TAG_NIL);
			} else {
				write(metatable);
			}
		}
		depth--;
	}

	private void writeThreadBody(LuaThread coroutine) throws IOException {
		depth++;
		{
			// Thread state
			coroutine.writeInternalState(this);
			write(coroutine.getfenv());

			LuaValue errorFunc = coroutine.getErrorFunc();

			// Debug hooks
			DebugState debug = coroutine.getDebugState();
			writeByte(
				(debug.inhook ? IN_HOOK : 0)
					| (errorFunc != null ? HAS_ERRORFUNC : 0)
			);
			if (errorFunc != null) write(errorFunc);
			debug.writeInternalState(this);
		}
		depth--;
	}

	public void write(Prototype prototype) throws IOException {
		if (checkWrittenPartial(prototype, TAG_PROTOTYPE) == NO_WRITE) return;
		writePrototypeBody(prototype);
	}

	private void writePrototypeBody(Prototype prototype) throws IOException {
		depth++;
		{
			write(prototype.source);
			writeVarInt(prototype.linedefined);
			writeVarInt(prototype.lastlinedefined);
			writeVarInt(prototype.nups);
			writeVarInt((prototype.numparams << 2) | prototype.is_vararg);
			writeVarInt(prototype.maxstacksize);

			writeArray(prototype.k, this::write);
			writeIntArray(prototype.code);
			writeArray(prototype.p, this::write);
			writeIntArray(prototype.lineinfo);
			writeArray(prototype.locvars, this::write);
			writeArray(prototype.upvalues, this::write);
		}
		depth--;
	}

	public void write(Upvalue upvalue) throws IOException {
		if (checkWritten(upvalue, TAG_UPVALUE) == NO_WRITE) return;
		write(upvalue.getArray());
		writeVarInt(upvalue.getIndex());
	}

	public void write(LuaValue[] stack) throws IOException {
		boolean skip = depth > MAX_DEPTH;
		int id = checkWritten(stack, (skip ? FLAG_PARTIAL : 0) | TAG_STACK);
		if (id == NO_WRITE) return;

		writeVarInt(stack.length);
		if (skip) {
			queue.add(new Update(id, stack, TAG_STACK));
			return;
		}

		for (LuaValue value : stack) write(value);
	}

	public void write(LocalVariable var) throws IOException {
		write(var.name);
		writeVarInt(var.startpc);
		writeVarInt(var.endpc);
	}

	public void close() throws IOException {
		Update update;
		while ((update = queue.poll()) != null) {
			writeByte(FLAG_POPULATE | update.type);
			writeVarInt(update.id);
			switch (update.type) {
				case TAG_TABLE:
					writeTableBody((LuaTable) update.object);
					break;
				case TAG_PROTOTYPE:
					writePrototypeBody((Prototype) update.object);
					break;
				case TAG_STACK: {
					for (LuaValue value : (LuaValue[]) update.object) write(value);
					break;
				}
				default:
					throw new IllegalStateException("Cannot resume for " + update.object + " (tag=" + update.type + ")");
			}
		}
	}

	private static final class Update {
		final Object object;
		final int id;
		final int type;

		Update(int id, Object object, int type) {
			this.object = object;
			this.id = id;
			this.type = type;
		}
	}

	// region Primitives
	public final void writeVarInt(int input) throws IOException {
		while ((input & 0xFFFFFF80) != 0) {
			output.writeByte(input & 0x7F | 0x80);
			input >>>= 7;
		}
		output.writeByte(input);
	}

	public final void write(byte[] b) throws IOException {
		output.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		output.write(b, off, len);
	}

	public void writeIntArray(int[] values) throws IOException {
		if (values == null) {
			writeByte(0);
			return;
		}

		writeVarInt(values.length);
		for (int x : values) writeVarInt(x);
	}

	public <T> boolean writeArrayHeader(T[] values) throws IOException {
		if (values == null) {
			writeByte(0);
			return false;
		}

		writeVarInt(values.length);
		return true;
	}

	public <T> void writeArray(T[] values, Writer<T> child) throws IOException {
		if (!writeArrayHeader(values)) return;
		for (T value : values) child.write(value);
	}

	public final void writeByte(int v) throws IOException {
		output.writeByte(v);
	}

	public final void writeShort(int v) throws IOException {
		output.writeShort(v);
	}

	public final void writeInt(int v) throws IOException {
		output.writeInt(v);
	}

	public final void writeDouble(double v) throws IOException {
		output.writeDouble(v);
	}

	public final void write(String str) throws IOException {
		int length = str.length();
		writeVarInt(length);
		for (int i = 0; i < length; i++) writeByte(str.charAt(i) & 0xFF);
	}

	public interface Writer<T> {
		void write(T value) throws IOException;
	}
	// endregion
}
