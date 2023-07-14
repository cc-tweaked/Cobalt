package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

class StringPacker {
	/**
	 * The size of {@code size_t}. Cobalt runs on a hypothetical 32-bit little endian machine, so we set this to 4.
	 */
	private static final int SIZEOF_SIZE_T = 4;

	private static final int SIZE_LONG = 8;

	private final static class Buffer {
		byte[] output;
		int offset;

		void ensure(int bytes) {
			if (output == null) {
				output = new byte[Math.max(32, bytes)];
			} else if (offset + bytes > output.length) {
				output = Arrays.copyOf(output, Math.max(output.length * 2, offset + bytes));
			}
		}

		void putUnsafe(byte value) {
			output[offset++] = value;
		}
	}

	private enum Mode {
		INT,
		UINT,
		FLOAT,
		DOUBLE,
		STRING,
		CHAR,
		PADDING,
		ZSTR,
		PADD_ALIGN,
		NONE,
	}

	private static class Info {
		final LuaString string;
		int position;
		final int end;

		// Current state
		boolean isLittle = true;
		int maxAlign = 1;

		// Current option state
		int size;
		int alignTo;

		public Info(LuaString string) {
			this.string = string;
			position = 0;
			end = string.length();
		}

		Mode setup(int size, Mode mode) {
			this.size = size;
			return mode;
		}
	}

	private static boolean digit(int c) {
		return c >= '0' && c <= '9';
	}

	public static int getNum(Info info, int def) {
		if (info.position >= info.end) return def;

		byte c = info.string.byteAt(info.position);
		if (!digit(c)) return def;

		int result = 0;
		do {
			result = result * 10 + (c - '0');
			info.position++;
		} while (info.position < info.end && digit(c = info.string.byteAt(info.position)) && result <= (Integer.MAX_VALUE - 9) / 10);
		return result;
	}

	public static int getNumLimit(Info info, int def) throws LuaError {
		int size = getNum(info, def);
		if (size <= 0 || size > 16) {
			throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
		}

		return size;
	}

	public static Mode getOption(Info info) throws LuaError {
		byte c = info.string.byteAt(info.position++);
		return switch (c) {
			case 'b' -> info.setup(1, Mode.INT);
			case 'B' -> info.setup(1, Mode.UINT);
			case 'h' -> info.setup(2, Mode.INT);
			case 'H' -> info.setup(2, Mode.UINT);
			case 'l' -> info.setup(8, Mode.INT);
			case 'L' -> info.setup(8, Mode.UINT);
			case 'j' -> info.setup(8, Mode.INT);
			case 'J' -> info.setup(8, Mode.UINT);
			case 'T' -> info.setup(SIZEOF_SIZE_T, Mode.UINT);
			case 'f' -> info.setup(4, Mode.FLOAT);
			case 'd' -> info.setup(8, Mode.DOUBLE);
			case 'n' -> info.setup(8, Mode.DOUBLE);
			case 'i' -> info.setup(getNumLimit(info, 4), Mode.INT);
			case 'I' -> info.setup(getNumLimit(info, 4), Mode.UINT);
			case 's' -> info.setup(getNumLimit(info, SIZEOF_SIZE_T), Mode.STRING);
			case 'c' -> {
				int size = getNum(info, -1);
				if (size < 0) throw new LuaError("missing size for format option 'c'");
				yield info.setup(size, Mode.CHAR);
			}
			case 'z' -> info.setup(0, Mode.ZSTR);
			case 'x' -> info.setup(1, Mode.PADDING);
			case 'X' -> info.setup(0, Mode.PADD_ALIGN);
			case ' ' -> info.setup(0, Mode.NONE);
			case '<', '=' -> {
				info.isLittle = true;
				yield info.setup(0, Mode.NONE);
			}
			case '>' -> {
				info.isLittle = false;
				yield info.setup(0, Mode.NONE);
			}
			case '!' -> {
				info.maxAlign = getNumLimit(info, 8);
				yield info.setup(0, Mode.NONE);
			}
			default -> throw new LuaError("invalid format option '" + (char) c + "'");
		};
	}

	public static Mode getDetails(Info info, int outPosition) throws LuaError {
		Mode mode = getOption(info);
		int align = info.size;
		if (mode == Mode.PADD_ALIGN) {
			if (info.position >= info.end || getOption(info) == Mode.CHAR || info.size == 0) {
				throw new LuaError("invalid next option for option 'X'");
			}

			align = info.size;
			info.size = 0;
		}

		if (align <= 1 || mode == Mode.CHAR) {
			info.alignTo = 0;
		} else {
			align = Math.min(align, info.maxAlign);
			if ((align & (align - 1)) != 0) {
				throw new LuaError("bad argument #1 to 'pack' (format asks for alignment not power of 2)");
			}

			info.alignTo = (align - (outPosition & (align - 1))) & (align - 1);
		}

		return mode;
	}

	private static void packInt(Buffer buffer, long num, boolean littleEndian, int size, boolean neg) {
		buffer.ensure(size);
		byte[] output = buffer.output;
		int offset = buffer.offset;
		buffer.offset += size;

		for (int i = 0; i < size; i++) {
			output[offset + (littleEndian ? i : size - 1 - i)] = (byte) (num & 0xFF);
			num >>>= 8;
		}

		// Sign-extend for negative numbers
		if (neg && size > SIZE_LONG) {
			for (int i = SIZE_LONG; i < size; i++) output[offset + (littleEndian ? i : size - 1 - i)] = (byte) 0xFF;
		}
	}

	/**
	 * string.pack (fmt, v1, v2, ...)
	 * <p>
	 * Returns a binary string containing the values v1, v2, etc.
	 * serialized in binary form (packed) according to the format string fmt.
	 */
	static LuaValue pack(Varargs args) throws LuaError {
		LuaString fmt = args.arg(1).checkLuaString();

		Info info = new Info(fmt);
		Buffer buffer = new Buffer();
		int i = 2;
		while (info.position < info.end) {
			Mode mode = getDetails(info, buffer.offset);
			buffer.ensure(info.alignTo);
			while (info.alignTo-- > 0) buffer.putUnsafe((byte) 0);

			switch (mode) {
				case PADD_ALIGN:
				case NONE:
					break;
				case INT: {
					long num = args.arg(i++).checkLong();
					if (info.size < SIZE_LONG) {
						long limit = 1L << (info.size * 8 - 1);
						if (-limit > num || num >= limit) throw ErrorFactory.argError(i - 1, "integer overflow");
					}

					packInt(buffer, num, info.isLittle, info.size, num < 0);
					break;
				}
				case UINT: {
					long num = args.arg(i++).checkLong();
					if (info.size < SIZE_LONG) {
						long limit = 1L << (info.size * 8);
						if (num < 0 || num >= limit) throw ErrorFactory.argError(i - 1, "integer overflow");
					}

					packInt(buffer, num, info.isLittle, info.size, false);
					break;
				}

				case FLOAT: {
					float f = (float) args.arg(i++).checkDouble();
					packInt(buffer, Float.floatToIntBits(f), info.isLittle, info.size, false);
					break;
				}
				case DOUBLE: {
					double f = args.arg(i++).checkDouble();
					packInt(buffer, Double.doubleToLongBits(f), info.isLittle, info.size, false);
					break;
				}

				case CHAR: {
					LuaString string = args.arg(i++).checkLuaString();
					if (string.length() > info.size)
						throw ErrorFactory.argError(i - 1, "string longer than given size");

					buffer.ensure(info.size);
					string.copyTo(buffer.output, buffer.offset);
					buffer.offset += info.size;
					break;
				}

				case ZSTR: {
					LuaString string = args.arg(i++).checkLuaString();

					int end = string.length();
					for (int j = 0; j < end; j++) {
						if (string.byteAt(j) == 0) throw ErrorFactory.argError(i - 1, "string contains zeros");
					}

					buffer.ensure(string.length() + 1);
					string.copyTo(buffer.output, buffer.offset);
					buffer.offset += string.length() + 1;
					break;
				}

				case STRING: {
					LuaString string = args.arg(i++).checkLuaString();
					if (info.size < SIZEOF_SIZE_T && string.length() > (1 << (info.size * 8))) {
						throw ErrorFactory.argError(i - 1, "string length does not fit in given size");
					}

					packInt(buffer, string.length(), info.isLittle, info.size, false);
					buffer.ensure(string.length());
					string.copyTo(buffer.output, buffer.offset);
					buffer.offset += string.length();
					break;
				}
				case PADDING:
					buffer.ensure(1);
					buffer.putUnsafe((byte) 0);
					break;
			}
		}

		return buffer.offset == 0 ? Constants.EMPTYSTRING : LuaString.valueOf(buffer.output, 0, buffer.offset);
	}

	/**
	 * string.packsize (fmt)
	 * <p>
	 * Returns the size of a string resulting from string.pack with the given format.
	 * The format string cannot have the variable-length options 's' or 'z'.
	 */
	static long packsize(LuaString fmt) throws LuaError {
		int size = 0;
		Info info = new Info(fmt);
		while (info.position < info.end) {
			Mode mode = getDetails(info, size);

			int thisSize = info.alignTo + info.size;
			if (size > Integer.MAX_VALUE - thisSize) throw ErrorFactory.argError(1, "format result too large");
			size += thisSize;

			if (mode == Mode.STRING || mode == Mode.ZSTR) {
				throw ErrorFactory.argError(1, "variable-length format");
			}
		}

		return size;
	}

	private static long unpackInt(LuaString str, int offset, boolean isLittle, int size, boolean signed) throws LuaError {
		long res = 0;
		int limit = Math.min(size, SIZE_LONG);
		for (int i = limit - 1; i >= 0; i--) {
			res <<= 8;
			res |= str.charAt(offset + (isLittle ? i : size - 1 - i));
		}

		if (size < SIZE_LONG) {
			if (signed) { // If smaller than a long, perform sign extension.
				long mask = 1L << (size * 8 - 1);
				res = (res ^ mask) - mask;
			}
		} else if (size > SIZE_LONG) {
			int mask = (!signed || res >= 0) ? 0 : 0xFF;
			for (int i = limit; i < size; i++) {
				if (str.charAt(offset + (isLittle ? i : size - 1 - i)) != mask) {
					throw new LuaError(size + "-byte integer does not fit into Lua Integer");
				}
			}
		}

		return res;
	}

	/**
	 * string.unpack (fmt, s [, pos])
	 * <p>
	 * Returns the values packed in string s (see string.pack) according to the format string fmt.
	 * An optional pos marks where to start reading in s (default is 1).
	 * After the read values, this function also returns the index of the first unread byte in s.
	 */
	static Varargs unpack(Varargs args) throws LuaError {
		LuaString fmt = args.arg(1).checkLuaString();
		LuaString str = args.arg(2).checkLuaString();
		int pos = StringLib.posRelative(args.arg(3).optInteger(1), str.length()) - 1;
		if (pos > str.length() || pos < 0) throw ErrorFactory.argError(3, "initial position out of string");

		List<LuaValue> out = new ArrayList<>();
		Info info = new Info(fmt);
		while (info.position < info.end) {
			Mode mode = getDetails(info, pos);

			if (info.alignTo + info.size + pos > str.length()) {
				throw ErrorFactory.argError(2, "data string too short");
			}
			pos += info.alignTo;

			switch (mode) {
				case PADD_ALIGN:
				case PADDING:
				case NONE:
					break;

				case INT:
				case UINT: {
					long value = unpackInt(str, pos, info.isLittle, info.size, mode == Mode.INT);
					out.add(valueOf(value));
					break;
				}
				case FLOAT: {
					long bits = unpackInt(str, pos, info.isLittle, info.size, false);
					float value = Float.intBitsToFloat((int) bits);
					out.add(valueOf(value));
					break;
				}
				case DOUBLE: {
					long bits = unpackInt(str, pos, info.isLittle, info.size, false);
					double value = Double.longBitsToDouble(bits);
					out.add(valueOf(value));
					break;
				}
				case CHAR:
					out.add(str.substringOfLen(pos, info.size));
					break;
				case STRING: {
					long len = unpackInt(str, pos, info.isLittle, info.size, false);
					if (info.size + len + pos > str.length()) throw ErrorFactory.argError(2, "data string too short");
					out.add(str.substringOfLen(pos + info.size, (int) len));
					pos += len;
					break;
				}
				case ZSTR: {
					int len = 0;
					for (int i = pos, end = str.length(); i < end; i++, len++) {
						if (str.charAt(i) == 0) break;
					}

					out.add(str.substringOfLen(pos + info.size, len));
					pos += len + 1;
					break;
				}
			}

			pos += info.size;
		}

		out.add(valueOf(pos + 1));
		return varargsOf(out);
	}
}
