/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt.compiler;

import cc.tweaked.cobalt.internal.unwind.AutoUnwind;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LocalVariable;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.compiler.LuaBytecodeFormat.*;

/**
 * Parser for bytecode
 */
@AutoUnwind
final class BytecodeLoader {
	private static final String EOF_ERROR = "unexpected end of file";

	/**
	 * format corresponding to non-number-patched lua, all numbers are floats or doubles
	 */
	public static final int NUMBER_FORMAT_FLOATS_OR_DOUBLES = 0;

	/**
	 * format corresponding to non-number-patched lua, all numbers are ints
	 */
	public static final int NUMBER_FORMAT_INTS_ONLY = 1;

	/**
	 * format corresponding to number-patched lua, all numbers are 32-bit (4 byte) ints
	 */
	public static final int NUMBER_FORMAT_NUM_PATCH_INT32 = 4;

	// values read from the header
	private boolean luacLittleEndian;
	private int luacSizeofSizeT;
	private int luacNumberFormat;

	/**
	 * input stream from which we are loading
	 */
	public final InputReader is;

	/**
	 * Private constructor for create a load state
	 *
	 * @param stream The stream to read from
	 */
	BytecodeLoader(InputReader stream) {
		is = stream;
	}

	private static final LuaValue[] NOVALUES = {};
	private static final Prototype[] NOPROTOS = {};
	private static final LocalVariable[] NOLOCVARS = {};
	private static final Prototype.UpvalueInfo[] NOUPVALUES = {};
	private static final int[] NOINTS = {};

	/**
	 * Read buffer
	 */
	private byte[] buf = new byte[512];

	private byte readByte() throws CompileException, LuaError, UnwindThrowable {
		int c = is.read();
		if (c < 0) {
			throw new CompileException(EOF_ERROR);
		}
		return (byte) c;
	}

	private int readUnsignedByte() throws CompileException, LuaError, UnwindThrowable {
		int c = is.read();
		if (c < 0) {
			throw new CompileException(EOF_ERROR);
		}
		return c;
	}

	private void readFully(byte[] buffer, int start, int size) throws CompileException, LuaError, UnwindThrowable {
		for (int i = 0; i < size; i++) {
			byte b = readByte();
			buffer[start + i] = b;
		}
	}

	/**
	 * Load a 4-byte int value from the input stream
	 *
	 * @return the int value laoded.
	 */
	private int loadInt() throws CompileException, LuaError, UnwindThrowable {
		readFully(buf, 0, 4);
		return luacLittleEndian ?
			(buf[3] << 24) | ((0xff & buf[2]) << 16) | ((0xff & buf[1]) << 8) | (0xff & buf[0]) :
			(buf[0] << 24) | ((0xff & buf[1]) << 16) | ((0xff & buf[2]) << 8) | (0xff & buf[3]);
	}

	/**
	 * Load an array of int values from the input stream
	 *
	 * @return the array of int values laoded.
	 */
	private int[] loadIntArray() throws CompileException, LuaError, UnwindThrowable {
		int n = loadInt();
		if (n == 0) return NOINTS;

		// read all data at once
		int m = n << 2;
		if (buf.length < m) buf = new byte[m];
		readFully(buf, 0, m);
		int[] array = new int[n];
		for (int i = 0, j = 0; i < n; ++i, j += 4) {
			array[i] = luacLittleEndian ?
				(buf[j + 3] << 24) | ((0xff & buf[j + 2]) << 16) | ((0xff & buf[j + 1]) << 8) | (0xff & buf[j + 0]) :
				(buf[j + 0] << 24) | ((0xff & buf[j + 1]) << 16) | ((0xff & buf[j + 2]) << 8) | (0xff & buf[j + 3]);
		}

		return array;
	}

	/**
	 * Load a long  value from the input stream
	 *
	 * @return the long value laoded.
	 */
	private long loadInt64() throws CompileException, LuaError, UnwindThrowable {
		int a, b;
		if (this.luacLittleEndian) {
			a = loadInt();
			b = loadInt();
		} else {
			b = loadInt();
			a = loadInt();
		}
		return (((long) b) << 32) | (((long) a) & 0xffffffffL);
	}

	/**
	 * Load a lua strin gvalue from the input stream
	 *
	 * @return the {@link LuaString} value laoded.
	 */
	private LuaString loadString() throws CompileException, LuaError, UnwindThrowable {
		int size = luacSizeofSizeT == 8 ? (int) loadInt64() : loadInt();
		if (size == 0) return null;

		byte[] bytes = new byte[size];
		readFully(bytes, 0, size);
		return LuaString.valueOf(bytes, 0, bytes.length - 1);
	}

	/**
	 * Convert bits in a long value to a {@link LuaValue}.
	 *
	 * @param bits long value containing the bits
	 * @return {@link LuaInteger} or {@link LuaDouble} whose value corresponds to the bits provided.
	 */
	public static LuaValue longBitsToLuaNumber(long bits) {
		return ValueFactory.valueOf(Double.longBitsToDouble(bits));
	}

	/**
	 * Load a number from a binary chunk
	 *
	 * @return the {@link LuaValue} loaded
	 * @throws CompileException, LuaError, UnwindThrowable if an i/o exception occurs
	 */
	private LuaValue loadNumber() throws CompileException, LuaError, UnwindThrowable {
		return longBitsToLuaNumber(loadInt64());
	}

	/**
	 * Load a list of constants from a binary chunk
	 *
	 * @throws CompileException, LuaError, UnwindThrowable if an i/o exception occurs
	 */
	private LuaValue[] loadConstants() throws CompileException, LuaError, UnwindThrowable {
		int n = loadInt();
		LuaValue[] values = n > 0 ? new LuaValue[n] : NOVALUES;
		for (int i = 0; i < n; i++) {
			values[i] = switch (readByte()) {
				case TNIL -> Constants.NIL;
				case TBOOLEAN -> readByte() != 0 ? Constants.TRUE : Constants.FALSE;
				case TNUMBER -> loadNumber();
				case TSTRING -> loadString();
				default -> throw new IllegalStateException("bad constant");
			};
		}
		return values;
	}

	private Prototype[] loadChildren() throws CompileException, LuaError, UnwindThrowable {
		int n = loadInt();
		Prototype[] protos = n > 0 ? new Prototype[n] : NOPROTOS;
		for (int i = 0; i < n; i++) {
			protos[i] = loadFunction();
		}
		return protos;
	}

	private LocalVariable[] loadLocals() throws CompileException, LuaError, UnwindThrowable {
		int n = loadInt();
		LocalVariable[] locals = n > 0 ? new LocalVariable[n] : NOLOCVARS;
		for (int i = 0; i < n; i++) {
			LuaString varName = loadString();
			int startpc = loadInt();
			int endpc = loadInt();
			locals[i] = new LocalVariable(varName, startpc, endpc);
		}
		return locals;
	}

	private void loadUpvaluesNames(Prototype.UpvalueInfo[] upvalues) throws CompileException, LuaError, UnwindThrowable {
		int n = loadInt();
		for (int i = 0; i < n; i++) {
			var upvalue = upvalues[i];
			var name = loadString();
			upvalues[i] = new Prototype.UpvalueInfo(name, upvalue.fromLocal(), upvalue.byteIndex());
		}
	}

	private Prototype.UpvalueInfo[] loadUpvalues() throws CompileException, LuaError, UnwindThrowable {
		int n = loadInt();
		var upvalues = n > 0 ? new Prototype.UpvalueInfo[n] : NOUPVALUES;
		for (int i = 0; i < n; i++) {
			var inStack = readByte() != 0;
			var slot = readByte();
			upvalues[i] = new Prototype.UpvalueInfo(null, inStack, slot);
		}
		return upvalues;
	}

	/**
	 * Load a function prototype from the input stream
	 *
	 * @return {@link Prototype} instance that was loaded
	 * @throws CompileException, LuaError, UnwindThrowable On stream read errors
	 */
	public Prototype loadFunction() throws CompileException, LuaError, UnwindThrowable {
		int lineDefined = loadInt();
		int lastLineDefined = loadInt();
		int numParams = readUnsignedByte();
		boolean isVarArg = readByte() != 0;
		int maxStackSize = readUnsignedByte();

		int[] code = loadIntArray();
		LuaValue[] constants = loadConstants();
		Prototype[] children = loadChildren();
		Prototype.UpvalueInfo[] upvalues = loadUpvalues();

		// See LoadDebug
		var source = loadString();
		if (source == null) source = LuaString.valueOf("=?");

		int[] lineInfo = loadIntArray();
		LocalVariable[] locals = loadLocals();
		loadUpvaluesNames(upvalues);

		return new Prototype(
			source, LoadState.getShortName(source),
			constants, code, children, numParams, isVarArg, maxStackSize, upvalues,
			lineDefined, lastLineDefined, lineInfo, NOINTS, locals
		);
	}

	public void checkSignature() throws CompileException, LuaError, UnwindThrowable {
		// Check rest of signature
		if (is.read() != LUA_SIGNATURE[1] || is.read() != LUA_SIGNATURE[2] || is.read() != LUA_SIGNATURE[3]) {
			throw new IllegalArgumentException("bad signature");
		}
	}

	/**
	 * Load the lua chunk header values.
	 *
	 * @throws CompileException, LuaError, UnwindThrowable      if an i/o exception occurs.
	 * @throws CompileException  If the bytecode is invalid.
	 */
	public void loadHeader() throws CompileException, LuaError, UnwindThrowable {
		int luacVersion = readByte();
		if (luacVersion != LUAC_VERSION) throw new CompileException("version mismatch");

		int luacFormat = readByte();
		if (luacFormat != LUAC_FORMAT) throw new CompileException("incompatible");

		luacLittleEndian = readByte() != 0;
		int luacSizeofInt = readByte();
		luacSizeofSizeT = readByte();
		int luacSizeofInstruction = readByte();
		int luacSizeofLuaNumber = readByte();
		int luacNumberFormat = readByte();

		if (luacSizeofInt != 4 || luacSizeofInstruction != 4 || luacSizeofLuaNumber != 8 || luacNumberFormat != 0) {
			throw new CompileException("incompatible");
		}

		for (byte b : LUAC_TAIL) {
			if (readByte() != b) throw new CompileException("incompatible");
		}
	}
}
