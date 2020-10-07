package org.squiddev.cobalt.persist;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LocalVariable;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaInterpretedFunction;
import org.squiddev.cobalt.function.Upvalue;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.IntFunction;

import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.persist.Persist.*;

public class ValueReader implements Closeable {
	private final LuaState state;
	private final DataInput input;
	private Object[] output = new Object[128];
	int id = 0;
	int partial = 0;

	public ValueReader(LuaState state, DataInput input) {
		this.state = state;
		this.input = input;
	}

	public LuaState getState() {
		return state;
	}

	public Object read() throws IOException {
		int tag = readByte();
		int type = tag & MASK_TAG;
		boolean partial = (tag & FLAG_PARTIAL) != 0;

		// If we expect a full object, we can't have a "to populate" instruction.
		if ((tag & FLAG_POPULATE) != 0) throw new IllegalStateException("Unexpected FLAG_POPULATE");
		if (partial) this.partial++;

		switch (type) {
			case TAG_NIL: return NIL;
			case TAG_TRUE: return Constants.TRUE;
			case TAG_FALSE: return Constants.FALSE;
			case TAG_SHORT_INT: return LuaInteger.valueOf(readShort());
			case TAG_INT: return LuaInteger.valueOf(readInt());
			case TAG_FLOAT: return LuaDouble.valueOf(readDouble());

			case TAG_SHORT_STRING: {
				int length = readByte() & 0xFF;
				return LuaString.valueOf(read(length));
			}

			case TAG_STRING: {
				int id = this.id++;
				int length = readVarInt() & 0xFF;
				return set(id, LuaString.valueOf(read(length)));
			}

			case TAG_TABLE: {
				int id = this.id++;

				LuaTable table = new LuaTable();
				set(id, table);
				if (!partial) readTableBody(table);
				return table;
			}

			case TAG_CLOSURE: {
				int id = this.id++;
				Prototype prototype = (Prototype) read();
				int nups = readVarInt();
				Upvalue[] upvalues = nups > 0 ? new Upvalue[nups] : LuaInterpretedFunction.NO_UPVALUES;
				LuaClosure closure = new LuaInterpretedFunction(prototype, upvalues);
				set(id, closure);

				for (int i = 0; i < upvalues.length; i++) upvalues[i] = (Upvalue) read();

				closure.setfenv((LuaTable) read());
				return closure;
			}
			case TAG_PROTOTYPE: {
				int id = this.id++;
				Prototype prototype = new Prototype();
				set(id, prototype);
				if (!partial) readPrototypeBody(prototype);
				return prototype;
			}

			case TAG_UPVALUE: {
				int id = this.id++;
				LuaValue[] stack = (LuaValue[]) read();
				int index = readVarInt();
				return set(id, new Upvalue(stack, index));
			}

			case TAG_STACK: {
				int id = this.id++;
				int length = readVarInt();

				LuaValue[] stack = new LuaValue[length];
				set(id, stack);
				if (!partial) {
					for (int i = 0; i < length; i++) stack[i] = (LuaValue) read();
				}
				return stack;
			}

			case TAG_THREAD: {
				int id = this.id++;

				LuaThread coroutine = new LuaThread(state, null);
				set(id, coroutine);
				if (!partial) readThreadBody(coroutine);
				return coroutine;
			}

			case TAG_SERIALIZER: {
				int id = this.id++;
				String name = readString();

				Serializer<?> serializer = state.getSerializer(name);
				if (serializer == null) throw new IOException("No such serializer " + name);
				return set(id, serializer);
			}

			case TAG_SERIALIZED: {
				int id = this.id++;

				@SuppressWarnings("rawtypes")
				Serializer<?> serializer = (Serializer) read();
				return set(id, serializer.load(this));
			}

			case TAG_REFERENCE:
				return this.output[readVarInt()];

			default:
				throw new IOException("Malformed input: unknown tag " + tag);
		}
	}

	private void readTableBody(LuaTable table) throws IOException {
		while (true) {
			LuaValue key = (LuaValue) read();
			if (key.isNil()) break;

			LuaValue value = (LuaValue) read();
			table.rawset(key, value);
		}

		LuaValue metatable = (LuaValue) read();
		if (!metatable.isNil()) table.setMetatable((LuaTable) metatable);
	}

	private void readThreadBody(LuaThread coroutine) throws IOException {
		coroutine.readInternalState(this);
		coroutine.setfenv((LuaTable) read());

		DebugState debug = coroutine.getDebugState();

		int flags = readByte();
		debug.inhook = (flags & IN_HOOK) != 0;

		if ((flags & HAS_ERRORFUNC) != 0) coroutine.setErrorFunc((LuaValue) read());
		debug.readInternalState(this);
	}

	public Varargs readVarargs() throws IOException {
		int count = readVarInt();
		switch (count) {
			case 0: return NONE;
			case 1: return (LuaValue) read();
			default: {
				LuaValue[] values = new LuaValue[count];
				for (int i = 0; i < count; i++) values[i] = (LuaValue) read();
				return ValueFactory.varargsOf(values);
			}
		}
	}

	private void readPrototypeBody(Prototype prototype) throws IOException {
		prototype.source = (LuaString) read();
		prototype.linedefined = readVarInt();
		prototype.lastlinedefined = readVarInt();
		prototype.nups = readVarInt();

		int flags = readVarInt();
		prototype.is_vararg = flags & 3;
		prototype.numparams = flags >> 2;
		prototype.maxstacksize = readVarInt();

		prototype.k = readArrayUnsafe(LuaValue[]::new);
		prototype.code = readIntArray();
		prototype.p = readArrayUnsafe(Prototype[]::new);
		prototype.lineinfo = readIntArray();
		prototype.locvars = readArray(LocalVariable[]::new, this::readLocalVariable);
		prototype.upvalues = readArrayUnsafe(LuaString[]::new);
	}

	private LocalVariable readLocalVariable() throws IOException {
		LuaString name = (LuaString) read();
		int startPc = readVarInt();
		int endPc = readVarInt();
		return new LocalVariable(name, startPc, endPc);
	}

	public void close() throws IOException {
		while (partial-- > 0) populateOne();
	}

	private void populateOne() throws IOException {
		int tag = readByte();
		int type = tag & MASK_TAG;

		if ((tag & FLAG_PARTIAL) != 0) throw new IllegalStateException("Unexpected FLAG_PARTIAL");
		if ((tag & FLAG_POPULATE) == 0) throw new IllegalStateException("Expected FLAG_PARTIAL");

		int id = readVarInt();
		Object object = output[id];
		switch (type) {
			case TAG_TABLE:
				readTableBody((LuaTable) object);
				break;
			case TAG_THREAD:
				readThreadBody((LuaThread) object);
				break;
			case TAG_PROTOTYPE:
				readPrototypeBody((Prototype) object);
				break;
			case TAG_STACK: {
				LuaValue[] stack = (LuaValue[]) object;
				for (int i = 0; i < stack.length; i++) stack[i] = (LuaValue) read();
				break;
			}
			default:
				throw new IllegalStateException("Do not know how to reconstruct " + object + " (tag=" + type + ")");
		}
	}

	private <T> T set(int id, T value) {
		if (id >= output.length) output = Arrays.copyOf(output, Math.max(id, output.length * 2));
		if (output[id] != null) throw new IllegalStateException("Duplicate keys for " + id);
		output[id] = value;
		return value;
	}

	// region Primitives
	public final int readVarInt() throws IOException {
		int result = 0;
		for (int j = 0; j < 5; j++) {
			byte b = input.readByte();
			result |= (b & 0x7F) << j * 7;
			if ((b & 0x80) != 128) return result;
		}

		throw new IOException("readVarInt read more than 5 bytes");
	}

	public final void read(byte[] b) throws IOException {
		input.readFully(b);
	}

	public void read(byte[] b, int off, int len) throws IOException {
		input.readFully(b, off, len);
	}

	public final byte[] read(int length) throws IOException {
		byte[] bytes = new byte[length];
		input.readFully(bytes, 0, length);
		return bytes;
	}

	public final int readByte() throws IOException {
		return input.readUnsignedByte();
	}

	public final short readShort() throws IOException {
		return input.readShort();
	}

	public final int readInt() throws IOException {
		return input.readInt();
	}

	public final double readDouble() throws IOException {
		return input.readDouble();
	}

	public <T> T[] readArray(IntFunction<T[]> make, Reader<T> child) throws IOException {
		T[] values = make.apply(readVarInt());
		for (int i = 0; i < values.length; i++) values[i] = child.read();
		return values;
	}

	public int[] readIntArray() throws IOException {
		int[] values = new int[readVarInt()];
		for (int i = 0; i < values.length; i++) values[i] = readVarInt();
		return values;
	}

	@SuppressWarnings("unchecked")
	public <T> T[] readArrayUnsafe(IntFunction<T[]> make) throws IOException {
		return readArray(make, () -> (T) read());
	}

	public String readString() throws IOException {
		int length = readVarInt();
		return LuaString.decode(read(length), 0, length);
	}

	public interface Reader<T> {
		T read() throws IOException;
	}
	// endregion
}
