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

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LocalVariable;
import org.squiddev.cobalt.unwind.AutoUnwind;

import static org.squiddev.cobalt.Constants.*;

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

	/**
	 * for header of binary files -- this is Lua 5.1
	 */
	public static final int LUAC_VERSION = 0x51;

	/**
	 * for header of binary files -- this is the official format
	 */
	public static final int LUAC_FORMAT = 0;

	/**
	 * size of header of binary files
	 */
	public static final int LUAC_HEADERSIZE = 12;

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
	private static final LuaString[] NOSTRVALUES = {};
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
		if (n == 0) {
			return NOINTS;
		}

		// read all data at once
		int m = n << 2;
		if (buf.length < m) {
			buf = new byte[m];
		}
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
		int size = this.luacSizeofSizeT == 8 ? (int) loadInt64() : loadInt();
		if (size == 0) {
			return null;
		}
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
		if ((bits & ((1L << 63) - 1)) == 0L) {
			return Constants.ZERO;
		}

		int e = (int) ((bits >> 52) & 0x7ffL) - 1023;

		if (e >= 0 && e < 31) {
			long f = bits & 0xFFFFFFFFFFFFFL;
			int shift = 52 - e;
			long intPrecMask = (1L << shift) - 1;
			if ((f & intPrecMask) == 0) {
				int intValue = (int) (f >> shift) | (1 << e);
				return LuaInteger.valueOf(((bits >> 63) != 0) ? -intValue : intValue);
			}
		}

		return ValueFactory.valueOf(Double.longBitsToDouble(bits));
	}

	/**
	 * Load a number from a binary chunk
	 *
	 * @return the {@link LuaValue} loaded
	 * @throws CompileException, LuaError, UnwindThrowable if an i/o exception occurs
	 */
	private LuaValue loadNumber() throws CompileException, LuaError, UnwindThrowable {
		if (luacNumberFormat == NUMBER_FORMAT_INTS_ONLY) {
			return LuaInteger.valueOf(loadInt());
		} else {
			return longBitsToLuaNumber(loadInt64());
		}
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
				case TBOOLEAN -> (0 != readUnsignedByte() ? Constants.TRUE : Constants.FALSE);
				case TINT -> LuaInteger.valueOf(loadInt());
				case TNUMBER -> loadNumber();
				case TSTRING -> loadString();
				default -> throw new IllegalStateException("bad constant");
			};
		}
		return values;
	}

	private Prototype[] loadChildren(LuaString source) throws CompileException, LuaError, UnwindThrowable {
		int n = loadInt();
		Prototype[] protos = n > 0 ? new Prototype[n] : NOPROTOS;
		for (int i = 0; i < n; i++) {
			protos[i] = loadFunction(source);
		}
		return protos;
	}

	private LocalVariable[] loadLocals() throws CompileException, LuaError, UnwindThrowable {
		int n = loadInt();
		LocalVariable[] locals = n > 0 ? new LocalVariable[n] : NOLOCVARS;
		for (int i = 0; i < n; i++) {
			LuaString varname = loadString();
			int startpc = loadInt();
			int endpc = loadInt();
			locals[i] = new LocalVariable(varname, startpc, endpc);
		}
		return locals;
	}

	private LuaString[] loadUpvalueNames() throws CompileException, LuaError, UnwindThrowable {
		int n = loadInt();
		LuaString[] upvalueNames = n > 0 ? new LuaString[n] : NOSTRVALUES;
		for (int i = 0; i < n; i++) upvalueNames[i] = loadString();
		return upvalueNames;
	}

	/**
	 * Load a function prototype from the input stream
	 *
	 * @param p name of the source
	 * @return {@link Prototype} instance that was loaded
	 * @throws CompileException, LuaError, UnwindThrowable On stream read errors
	 */
	public Prototype loadFunction(LuaString p) throws CompileException, LuaError, UnwindThrowable {
		LuaString source = loadString();
		if (source == null) source = p;

		int lineDefined = loadInt();
		int lastLineDefined = loadInt();
		int nups = readUnsignedByte();
		int numparams = readUnsignedByte();
		int is_vararg = readUnsignedByte();
		int maxstacksize = readUnsignedByte();
		int[] code = loadIntArray();
		LuaValue[] constants = loadConstants();
		Prototype[] children = loadChildren(source);
		int[] lineInfo = loadIntArray();
		LocalVariable[] locals = loadLocals();
		LuaString[] upvalueNames = loadUpvalueNames();

		return new Prototype(
			source,
			constants, code, children, numparams, is_vararg, maxstacksize, nups,
			lineDefined, lastLineDefined, lineInfo, null, locals, upvalueNames
		);
	}

	/**
	 * Load the lua chunk header values.
	 *
	 * @throws CompileException, LuaError, UnwindThrowable      if an i/o exception occurs.
	 * @throws CompileException  If the bytecode is invalid.
	 */
	public void loadHeader() throws CompileException, LuaError, UnwindThrowable {
		int luacVersion = readByte();
		if (luacVersion != LUAC_VERSION) throw new CompileException("unsupported luac version");

		int luacFormat = readByte();
		luacLittleEndian = (0 != readByte());
		int luacSizeofInt = readByte();
		luacSizeofSizeT = readByte();
		int luacSizeofInstruction = readByte();
		int luacSizeofLuaNumber = readByte();
		luacNumberFormat = readByte();

		// check format
		switch (luacNumberFormat) {
			case NUMBER_FORMAT_FLOATS_OR_DOUBLES:
			case NUMBER_FORMAT_INTS_ONLY:
			case NUMBER_FORMAT_NUM_PATCH_INT32:
				break;
			default:
				throw new CompileException("unsupported int size");
		}
	}
}
