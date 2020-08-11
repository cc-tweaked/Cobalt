package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.*;

import java.nio.ByteOrder;

import static org.squiddev.cobalt.ValueFactory.varargsOf;

public class StringPacker {
	private static int packint(long num, int size, byte[] output, int offset, int alignment, ByteOrder endianness, boolean signed) {
		int total_size = 0;
		if (offset % Math.min(size, alignment) != 0 && alignment > 1) {
			for (int i = 0; offset % Math.min(size, alignment) != 0 && i < alignment; i++) {
				output[offset++] = 0;
				total_size++;
			}
		}
		if (endianness == ByteOrder.BIG_ENDIAN) {
			int added_padding = 0;
			if (size > 8) for (int i = 0; i < size - 8; i++) {
				output[offset + i] = signed && (num & (1 << (size * 8 - 1))) != 0 ? (byte)0xFF : 0;
				added_padding++;
				total_size++;
			}
			for (int i = added_padding; i < size; i++) {
				output[offset + i] = (byte) ((num >> ((size - i - 1) * 8)) & 0xFF);
				total_size++;
			}
		} else {
			for (int i = 0; i < Math.min(size, 8); i++) {
				output[offset + i] = (byte) ((num >> (i * 8)) & 0xFF);
				total_size++;
			}
			for (int i = 8; i < size; i++) {
				output[offset + i] = signed && (num & (1 << (size * 8 - 1))) != 0 ? (byte)0xFF : 0;
				total_size++;
			}
		}
		return total_size;
	}

	private static int packoptsize(byte opt, int alignment) {
		int retval = 0;
		switch (opt) {
			case 'b':
			case 'B':
			case 'x':
				retval = 1;
				break;
			case 'h':
			case 'H':
				retval = 2;
				break;
			case 'f':
			case 'j': // lua_Integer is 32-bit because of bit32 support
			case 'J':
				retval = 4;
				break;
			case 'l':
			case 'L':
			case 'T':
			case 'd':
			case 'n':
				retval = 8;
				break;
		}
		if (alignment > 1 && retval % alignment != 0) retval += (alignment - (retval % alignment));
		return retval;
	}

	/**
	 * string.pack (fmt, v1, v2, ...)
	 *
	 * Returns a binary string containing the values v1, v2, etc.
	 * serialized in binary form (packed) according to the format string fmt.
	 */
	static Varargs pack(Varargs args) throws LuaError {
		LuaString fmt = args.arg(1).checkLuaString();
		ByteOrder endianness = ByteOrder.nativeOrder();
		int alignment = 1;
		int currentSize = 64, pos = 0;
		int argnum = 2;
		byte[] output = new byte[currentSize];
		for (int i = fmt.offset; i < fmt.length; i++) {
			switch (fmt.bytes[i]) {
				case '<': {
					endianness = ByteOrder.LITTLE_ENDIAN;
					break;
				}
				case '>': {
					endianness = ByteOrder.BIG_ENDIAN;
					break;
				}
				case '=': {
					endianness = ByteOrder.LITTLE_ENDIAN;
					break;
				}
				case '!': {
					int size = -1;
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'pack' (invalid format)");
						size = (Math.max(size, 0) * 10) + (fmt.bytes[++i] - '0');
					}
					if (size > 16 || size == 0)
						throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
					else if (size == -1) alignment = 4;
					else alignment = size;
					break;
				}
				case 'b':
				case 'B':
				case 'h':
				case 'H':
				case 'l':
				case 'L':
				case 'j':
				case 'J':
				case 'T': {
					long num = args.arg(argnum++).checkLong();
					if (num >= Math.pow(2, (packoptsize(fmt.bytes[i], 0) * 8 - (Character.isLowerCase(fmt.bytes[i]) ? 1 : 0))) ||
						num < (Character.isLowerCase(fmt.bytes[i]) ? -Math.pow(2, (packoptsize(fmt.bytes[i], 0) * 8 - 1)) : 0))
						throw new LuaError(String.format("bad argument #%d to 'pack' (integer overflow)", argnum - 1));
					if (pos + packoptsize(fmt.bytes[i], alignment) >= currentSize) {
						byte[] newoutput = new byte[currentSize + 64];
						System.arraycopy(output, 0, newoutput, 0, currentSize);
						output = newoutput;
						currentSize += 64;
					}
					pos += packint(num, packoptsize(fmt.bytes[i], 0), output, pos, alignment, endianness, false);
					break;
				}
				case 'i':
				case 'I': {
					boolean signed = fmt.bytes[i] == 'i';
					int size = -1;
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'pack' (invalid format)");
						size = (Math.max(size, 0) * 10) + (fmt.bytes[++i] - '0');
					}
					if (size > 16 || size == 0)
						throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
					else if (alignment > 1 && (size != 1 && size != 2 && size != 4 && size != 8 && size != 16))
						throw new LuaError("bad argument #1 to 'pack' (format asks for alignment not power of 2)");
					else if (size == -1) size = 4;
					long num = args.arg(argnum++).checkLong();
					if (num >= Math.pow(2, size * 8 - (signed ? 1 : 0)) ||
						num < (signed ? -Math.pow(2, (size * 8 - 1)) : 0))
						throw new LuaError(String.format("bad argument #%d to 'pack' (integer overflow)", argnum - 1));
					if (pos + (size % alignment != 0 ? size + (alignment - (size % alignment)) : size) >= currentSize) {
						byte[] newoutput = new byte[currentSize + 64];
						System.arraycopy(output, 0, newoutput, 0, currentSize);
						output = newoutput;
						currentSize += 64;
					}
					pos += packint(num, size, output, pos, alignment, endianness, signed);
					break;
				}
				case 'f': {
					float f = (float)args.arg(argnum++).checkDouble();
					if (pos + 4 >= currentSize) {
						byte[] newoutput = new byte[currentSize + 64];
						System.arraycopy(output, 0, newoutput, 0, currentSize);
						output = newoutput;
						currentSize += 64;
					}
					int l = Float.floatToRawIntBits(f);
					if (pos % Math.min(4, alignment) != 0 && alignment > 1) for (int j = 0; pos % Math.min(4, alignment) != 0 && j < alignment; j++) output[pos++] = 0;
					for (int j = 0; j < 4; j++) output[pos + (endianness == ByteOrder.BIG_ENDIAN ? 3 - j : j)] = (byte)((l >> (j * 8)) & 0xFF);
					pos += 4;
					break;
				}
				case 'd':
				case 'n': {
					double f = args.arg(argnum++).checkDouble();
					if (pos + 8 >= currentSize) {
						byte[] newoutput = new byte[currentSize + 64];
						System.arraycopy(output, 0, newoutput, 0, currentSize);
						output = newoutput;
						currentSize += 64;
					}
					long l = Double.doubleToRawLongBits(f);
					if (pos % Math.min(8, alignment) != 0 && alignment > 1) for (int j = 0; pos % Math.min(8, alignment) != 0 && j < alignment; j++) output[pos++] = 0;
					for (int j = 0; j < 8; j++) output[pos + (endianness == ByteOrder.BIG_ENDIAN ? 7 - j : j)] = (byte)((l >> (j * 8)) & 0xFF);
					pos += 8;
					break;
				}
				case 'c': {
					int size = 0;
					if (i + 1 == fmt.length || fmt.bytes[i + 1] < '0' || fmt.bytes[i + 1] > '9')
						throw new LuaError("missing size for format option 'c'");
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'pack' (invalid format)");
						size = (size * 10) + (fmt.bytes[++i] - '0');
					}
					if (pos + size < pos || pos + size > Integer.MAX_VALUE) throw new LuaError("bad argument #1 to 'pack' (format result too large)");
					LuaString str = args.arg(argnum++).checkLuaString();
					if (str.length > size) throw new LuaError(String.format("bad argument #%d to 'pack' (string longer than given size)", argnum - 1));
					if (size > 0) {
						if (pos + size >= currentSize) {
							int bytestoadd = size % 64 == 0 ? size : size + (64 - (size % 64));
							byte[] newoutput = new byte[currentSize + bytestoadd];
							System.arraycopy(output, 0, newoutput, 0, currentSize);
							output = newoutput;
							currentSize += bytestoadd;
						}
						System.arraycopy(str.bytes, 0, output, pos, Math.min(str.length, size));
						for (int j = str.length; j < size; j++) output[pos + j] = 0;
						pos += size;
					}
					break;
				}
				case 'z': {
					LuaString str = args.arg(argnum++).checkLuaString();
					for (byte b : str.bytes) if (b == 0) throw new LuaError(String.format("bad argument #%d to 'pack' (string contains zeros)", argnum - 1));
					if (pos + str.length + 1 >= currentSize) {
						int bytestoadd = (str.length + 1) % 64 == 0 ? (str.length + 1) : (str.length + 1) + (64 - ((str.length + 1) % 64));
						byte[] newoutput = new byte[currentSize + bytestoadd];
						System.arraycopy(output, 0, newoutput, 0, currentSize);
						output = newoutput;
						currentSize += bytestoadd;
					}
					System.arraycopy(str.bytes, 0, output, pos, str.length);
					output[pos + str.length] = 0;
					pos += str.length + 1;
					break;
				}
				case 's': {
					int size = 0;
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'pack' (invalid format)");
						size = (size * 10) + (fmt.bytes[++i] - '0');
					}
					if (size > 16)
						throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
					else if (size == 0) size = 8;
					LuaString str = args.arg(argnum++).checkLuaString();
					if (str.length >= Math.pow(2, (size * 8)))
						throw new LuaError(String.format("bad argument #%d to 'pack' (string length does not fit in given size)", argnum - 1));
					if (pos + str.length + size >= currentSize) {
						int bytestoadd = (str.length + size) % 64 == 0 ? (str.length + size) : (str.length + size) + (64 - ((str.length + size) % 64));
						byte[] newoutput = new byte[currentSize + bytestoadd];
						System.arraycopy(output, 0, newoutput, 0, currentSize);
						output = newoutput;
						currentSize += bytestoadd;
					}
					packint(str.length, size, output, pos, 1, endianness, false);
					System.arraycopy(str.bytes, 0, output, pos + size, str.length);
					pos += str.length + size;
					break;
				}
				case 'x': {
					if (pos + 1 >= currentSize) {
						byte[] newoutput = new byte[currentSize + 64];
						System.arraycopy(output, 0, newoutput, 0, currentSize);
						output = newoutput;
						currentSize += 64;
					}
					output[pos++] = 0;
					break;
				}
				case 'X': {
					if (i + 1 >= fmt.length) throw new LuaError("invalid next option for option 'X'");
					int size = 0;
					if (fmt.bytes[++i] == 'i' || fmt.bytes[i] == 'I') {
						while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
							if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'pack' (invalid format)");
							size = (size * 10) + (fmt.bytes[++i] - '0');
						}
						if (size > 16 || size == 0)
							throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
					} else size = packoptsize(fmt.bytes[i], 0);
					if (size < 1) throw new LuaError("invalid next option for option 'X'");
					if (pos % Math.min(size, alignment) != 0 && alignment > 1)
						for (int j = 0; pos % Math.min(size, alignment) != 0 && j < alignment; j++)
							output[pos++] = 0;
					break;
				}
				case ' ': break;
				default: throw new LuaError(String.format("invalid format option '%c'", fmt.bytes[i]));
			}
		}
		return LuaString.valueOf(output, 0, pos);
	}

	/**
	 * string.packsize (fmt)
	 *
	 * Returns the size of a string resulting from string.pack with the given format.
	 * The format string cannot have the variable-length options 's' or 'z'.
	 */
	static long packsize(LuaString fmt) throws LuaError {
		long pos = 0;
		int alignment = 1;
		for (int i = fmt.offset; i < fmt.length; i++) {
			switch (fmt.bytes[i]) {
				case '!': {
					int size = 0;
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'packsize' (invalid format)");
						size = (size * 10) + (fmt.bytes[++i] - '0');
					}
					if (size > 16)
						throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
					else if (size == 0) { // This is a bit hacky; it will probably need to be rewritten
						String arch = System.getProperty("os.arch");
						if (arch.equals("x86_64") || arch.equals("amd64") || arch.equals("x64"))
							alignment = 8; // assume 8 bytes on 64-bit architectures
						else alignment = 4; // assume 4 bytes on 32-bit architectures
					} else alignment = size;
					break;
				}
				case 'b':
				case 'B':
				case 'h':
				case 'H':
				case 'l':
				case 'L':
				case 'j':
				case 'J':
				case 'T': {
					int size = packoptsize(fmt.bytes[i], 0);
					if (pos % Math.min(size, alignment) != 0 && alignment > 1) for (int j = 0; pos % Math.min(size, alignment) != 0 && j < alignment; j++) pos++;
					pos += size;
					break;
				}
				case 'i':
				case 'I': {
					int size = 0;
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'packsize' (invalid format)");
						size = (size * 10) + (fmt.bytes[++i] - '0');
					}
					if (size > 16)
						throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
					else if (alignment > 1 && (size != 1 && size != 2 && size != 4 && size != 8 && size != 16))
						throw new LuaError("bad argument #1 to 'pack' (format asks for alignment not power of 2)");
					else if (size == 0) size = 4;
					if (pos % Math.min(size, alignment) != 0 && alignment > 1) for (int j = 0; pos % Math.min(size, alignment) != 0 && j < alignment; j++) pos++;
					pos += size;
					break;
				}
				case 'f': {
					if (pos % Math.min(4, alignment) != 0 && alignment > 1) for (int j = 0; pos % Math.min(4, alignment) != 0 && j < alignment; j++) pos++;
					pos += 4;
					break;
				}
				case 'd':
				case 'n': {
					if (pos % Math.min(8, alignment) != 0 && alignment > 1) for (int j = 0; pos % Math.min(8, alignment) != 0 && j < alignment; j++) pos++;
					pos += 8;
					break;
				}
				case 'c': {
					int size = 0;
					if (i + 1 == fmt.length || fmt.bytes[i + 1] < '0' || fmt.bytes[i + 1] > '9')
						throw new LuaError("missing size for format option 'c'");
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'packsize' (invalid format)");
						size = (size * 10) + (fmt.bytes[++i] - '0');
					}
					if (pos + size < pos || pos + size > Integer.MAX_VALUE) throw new LuaError("bad argument #1 to 'packsize' (format result too large)");
					pos += size;
					break;
				}
				case 'x': {
					pos++;
					break;
				}
				case 'X': {
					if (i + 1 >= fmt.length) throw new LuaError("invalid next option for option 'X'");
					int size = 0;
					if (fmt.bytes[++i] == 'i' || fmt.bytes[i] == 'I') {
						while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
							if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'packsize' (invalid format)");
							size = (size * 10) + (fmt.bytes[++i] - '0');
						}
						if (size > 16 || size == 0)
							throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
					} else size = packoptsize(fmt.bytes[i], 0);
					if (size < 1) throw new LuaError("invalid next option for option 'X'");
					if (pos % Math.min(size, alignment) != 0 && alignment > 1) for (int j = 0; pos % Math.min(size, alignment) != 0 && j < alignment; j++) pos++;
					break;
				}
				case ' ': case '<': case '>': case '=': break;
				case 's': case 'z': throw new LuaError("bad argument #1 to 'packsize' (variable-length format)");
				default: throw new LuaError(String.format("invalid format option '%c'", fmt.bytes[i]));
			}
		}
		return pos;
	}

	private static class UnpackIntState {
		long result = 0;
		int size = 0;
	}

	private static UnpackIntState unpackint(byte[] str, int offset, int size, ByteOrder endianness, int alignment, boolean signed) {
		UnpackIntState retval = new UnpackIntState();
		if (offset % Math.min(size, alignment) != 0 && alignment > 1) for (int i = 0; offset % Math.min(size, alignment) != 0 && i < alignment; i++) {offset++; retval.size++;}
		for (int i = 0; i < size; i++) {
			retval.result |= (((long) str[offset + i] & 0xFF) << ((endianness == ByteOrder.BIG_ENDIAN ? size - i - 1 : i) * 8));
			retval.size++;
		}
		if (signed && (retval.result & (1 << (size * 8 - 1))) != 0)
			for (int i = size; i < 8; i++) retval.result |= ((long)0xFF & 0xFF) << (i * 8);
		return retval;
	}

	/**
	 * string.unpack (fmt, s [, pos])
	 *
	 * Returns the values packed in string s (see string.pack) according to the format string fmt.
	 * An optional pos marks where to start reading in s (default is 1).
	 * After the read values, this function also returns the index of the first unread byte in s.
	 */
	static Varargs unpack(Varargs args) throws LuaError {
		LuaString fmt = args.arg(1).checkLuaString();
		LuaString str = args.arg(2).checkLuaString();
		int pos = args.arg(3).optInteger(1);
		if (pos < 0) pos = str.length + pos;
		else if (pos == 0) throw new LuaError("bad argument #3 to 'unpack' (initial position out of string)");
		else pos--;
		if (pos > str.length || pos < 0) throw new LuaError("bad argument #3 to 'unpack' (initial position out of string)");
		ByteOrder endianness = ByteOrder.nativeOrder();
		int alignment = 1;
		LuaTable retval = new LuaTable();
		for (int i = fmt.offset; i < fmt.length; i++) {
			switch (fmt.bytes[i]) {
				case '<': {
					endianness = ByteOrder.LITTLE_ENDIAN;
					break;
				}
				case '>': {
					endianness = ByteOrder.BIG_ENDIAN;
					break;
				}
				case '=': {
					endianness = ByteOrder.nativeOrder();
					break;
				}
				case '!': {
					int size = 0;
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'unpack' (invalid format)");
						size = (size * 10) + (fmt.bytes[++i] - '0');
					}
					if (size > 16)
						throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
					else if (size == 0) { // This is a bit hacky; it will probably need to be rewritten
						String arch = System.getProperty("os.arch");
						if (arch.equals("x86_64") || arch.equals("amd64") || arch.equals("x64"))
							alignment = 8; // assume 8 bytes on 64-bit architectures
						else alignment = 4; // assume 4 bytes on 32-bit architectures
					} else alignment = size;
					break;
				}
				case 'b':
				case 'B':
				case 'h':
				case 'H':
				case 'l':
				case 'L':
				case 'j':
				case 'J':
				case 'T': {
					if (pos + packoptsize(fmt.bytes[i], 0) > str.length) throw new LuaError("data string too short");
					UnpackIntState res = unpackint(str.bytes, pos, packoptsize(fmt.bytes[i], 0), endianness, alignment, Character.isLowerCase(fmt.bytes[i]));
					retval.insert(retval.length() + 1, LuaInteger.valueOf(res.result));
					pos += res.size;
					break;
				}
				case 'i':
				case 'I': {
					boolean signed = fmt.bytes[i] == 'i';
					int size = 0;
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'unpack' (invalid format)");
						size = (size * 10) + (fmt.bytes[++i] - '0');
					}
					if (size > 16)
						throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
					else if (size > 8)
						throw new LuaError(String.format("%d-byte integer does not fit into Lua Integer", size));
					else if (size == 0) size = 4;
					if (pos + size > str.length) throw new LuaError("data string too short");
					UnpackIntState res = unpackint(str.bytes, pos, size, endianness, alignment, signed);
					retval.insert(retval.length() + 1, LuaInteger.valueOf(res.result));
					pos += res.size;
					break;
				}
				case 'f': {
					if (pos % Math.min(4, alignment) != 0 && alignment > 1) for (int j = 0; pos % Math.min(4, alignment) != 0 && j < alignment; j++) pos++;
					if (pos + 4 > str.length) throw new LuaError("data string too short");
					int l = 0;
					for (int j = 0; j < 4; j++)
						l |= (((int)str.bytes[pos + j] & 0xFF) << ((endianness == ByteOrder.BIG_ENDIAN ? 3 - j : j) * 8));
					retval.insert(retval.length() + 1, LuaDouble.valueOf(Float.intBitsToFloat(l)));
					pos += 4;
					break;
				}
				case 'd':
				case 'n': {
					if (pos % Math.min(8, alignment) != 0 && alignment > 1) for (int j = 0; pos % Math.min(8, alignment) != 0 && j < alignment; j++) pos++;
					if (pos + 8 > str.length) throw new LuaError("data string too short");
					long l = 0;
					for (int j = 0; j < 8; j++) l |= (((long)str.bytes[pos + j] & 0xFF) << ((endianness == ByteOrder.BIG_ENDIAN ? 7 - j : j) * 8));
					retval.insert(retval.length() + 1, LuaDouble.valueOf(Double.longBitsToDouble(l)));
					pos += 8;
					break;
				}
				case 'c': {
					int size = 0;
					if (i + 1 == fmt.length || fmt.bytes[i + 1] < '0' || fmt.bytes[i + 1] > '9')
						throw new LuaError("missing size for format option 'c'");
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'unpack' (invalid format)");
						size = (size * 10) + (fmt.bytes[++i] - '0');
					}
					if (pos + size > str.length) throw new LuaError("data string too short");
					retval.insert(retval.length() + 1, LuaString.valueOf(str.bytes, pos, size));
					pos += size;
					break;
				}
				case 'z': {
					int size = 0;
					while (str.bytes[pos + size] != 0) if (++size >= str.length) throw new LuaError("unfinished string for format 'z'");
					retval.insert(retval.length() + 1, LuaString.valueOf(str.bytes, pos, size));
					pos += size + 1;
					break;
				}
				case 's': {
					int size = 0;
					while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
						if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'unpack' (invalid format)");
						size = (size * 10) + (fmt.bytes[++i] - '0');
					}
					if (size > 16)
						throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
					else if (size == 0) size = 8;
					if (pos + 8 > str.length) throw new LuaError("data string too short");
					UnpackIntState num = unpackint(str.bytes, pos, size, endianness, alignment, false);
					pos += num.size;
					if (pos + num.result > str.length) throw new LuaError("data string too short");
					retval.insert(retval.length() + 1, LuaString.valueOf(str.bytes, pos, (int)num.result));
					pos += num.result;
					break;
				}
				case 'x': {
					pos++;
					break;
				}
				case 'X': {
					if (i + 1 >= fmt.length) throw new LuaError("invalid next option for option 'X'");
					int size = -1;
					if (fmt.bytes[++i] == 'i' || fmt.bytes[i] == 'I') {
						while (i + 1 < fmt.length && fmt.bytes[i + 1] >= '0' && fmt.bytes[i + 1] <= '9') {
							if (size >= Integer.MAX_VALUE / 10) throw new LuaError("bad argument #1 to 'unpack' (invalid format)");
							size = (Math.max(size, 0) * 10) + (fmt.bytes[++i] - '0');
						}
						if (size > 16 || size == 0)
							throw new LuaError(String.format("integral size (%d) out of limits [1,16]", size));
						else if (size == -1) size = 4;
					} else size = packoptsize(fmt.bytes[i], 0);
					if (size < 1) throw new LuaError("invalid next option for option 'X'");
					if (pos % Math.min(size, alignment) != 0 && alignment > 1) for (int j = 0; pos % Math.min(size, alignment) != 0 && j < alignment; j++) pos++;
					break;
				}
				case ' ': break;
				default: throw new LuaError(String.format("invalid format option '%c'", fmt.bytes[i]));
			}
		}
		LuaValue[] retvar = new LuaValue[retval.length() + 1];
		for (int i = 0; i < retval.length(); i++) retvar[i] = retval.rawget(i+1);
		retvar[retval.length()] = LuaInteger.valueOf(pos + 1);
		return varargsOf(retvar);
	}
}
