package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.RegisteredFunction;

import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

public final class Utf8Lib {
	private static final int[] LIMITS = {0xFF, 0x7F, 0x7FF, 0xFFFF};
	public static final long MAX_UNICODE = 0x10FFFFL;

	/**
	 * [\0-\x7F\xC2-\xF4][\x80-\xBF]*
	 */
	private static final LuaString PATTERN = valueOf(new byte[]{
		'[', 0x00, '-', 0x7f, (byte) 0xc2, '-', (byte) 0xf4, ']', '[', (byte) 0x80, '-', (byte) 0xbf, ']', '*',
	});

	/**
	 * Singleton for the utf8.codes() iterator. In lua 5.3 it always returns the same function reference,
	 * so here we always return the same object reference.
	 */
	private LibFunction codesIter;

	private Utf8Lib() {
	}

	public static void add(LuaState state, LuaTable env) throws LuaError {
		var self = new Utf8Lib();
		self.codesIter = RegisteredFunction.ofV("utf8.codesIter", Utf8Lib::codesIter).create();

		LuaTable t = new LuaTable(0, 6);
		RegisteredFunction.bind(t, new RegisteredFunction[]{
			RegisteredFunction.ofV("char", Utf8Lib::char$),
			RegisteredFunction.ofV("codes", self::codes),
			RegisteredFunction.ofV("codepoint", Utf8Lib::codepoint),
			RegisteredFunction.ofV("len", Utf8Lib::len),
			RegisteredFunction.of("offset", Utf8Lib::offset),
		});
		t.rawset("charpattern", PATTERN);

		LibFunction.setGlobalLibrary(state, env, "utf8", t);
	}

	public static int buildCharacter(byte[] buffer, long codepoint) {
		int mfb = 0x3f;
		int j = 1;
		do {
			buffer[8 - j++] = ((byte) (0x80 | (codepoint & 0x3f)));
			codepoint >>= 6;
			mfb >>= 1;
		} while (codepoint > mfb);
		buffer[8 - j] = (byte) ((~mfb << 1) | codepoint);
		return j;
	}

	private static LuaValue char$(LuaState state, Varargs args) throws LuaError {
		Buffer sb = new Buffer(args.count());
		byte[] buffer = null;
		for (int i = 1, n = args.count(); i <= n; i++) {
			int codepoint = args.arg(i).checkInteger();
			if (codepoint < 0 || codepoint > MAX_UNICODE) {
				throw ErrorFactory.argError(i, "value out of range");
			}

			if (codepoint < 0x80) {
				sb.append((byte) codepoint);
			} else {
				if (buffer == null) buffer = new byte[8];
				int j = buildCharacter(buffer, codepoint);
				sb.append(buffer, 8 - j, j);
			}
		}

		return sb.toLuaString();
	}

	private Varargs codes(LuaState state, Varargs args) throws LuaError {
		return varargsOf(codesIter, args.arg(1).checkLuaString(), valueOf(0));
	}

	private static Varargs codepoint(LuaState state, Varargs args) throws LuaError {
		LuaString s = args.arg(1).checkLuaString();
		int length = s.length();
		int i = posRelative(args.arg(2).optInteger(1), length);
		int j = posRelative(args.arg(3).optInteger(i), length);

		if (i < 1) throw ErrorFactory.argError(2, "out of bounds");
		if (j > length) throw ErrorFactory.argError(3, "out of bounds");
		if (i > j) return NONE;

		IntBuffer off = new IntBuffer();
		int n = 0;
		LuaNumber[] codepoints = new LuaNumber[j - i + 1];

		do {
			long codepoint = decodeUtf8(s, i - 1, off);
			if (codepoint < 0) throw new LuaError("invalid UTF-8 code");
			codepoints[n++] = LuaInteger.valueOf(codepoint);
		} while ((i += off.value) <= j);

		return ValueFactory.varargsOfCopy(codepoints, 0, n);
	}

	private static Varargs len(LuaState state, Varargs args) throws LuaError {
		LuaString s = args.arg(1).checkLuaString();
		int len = s.length();
		int i = posRelative(args.arg(2).optInteger(1), len) - 1;
		int j = posRelative(args.arg(3).optInteger(-1), len) - 1;

		if (i < 0 || i > len) throw ErrorFactory.argError(2, "initial position out of string");
		if (j >= len) throw ErrorFactory.argError(3, "final position out of string");

		int n = 0;
		IntBuffer offset = new IntBuffer();
		while (i <= j) {
			long codepoint = decodeUtf8(s, i, offset);
			if (codepoint < 0) return varargsOf(NIL, valueOf(i + 1));

			n++;
			i += offset.value;
		}

		return valueOf(n);
	}

	private static LuaValue offset(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError {
		LuaString s = arg1.checkLuaString();
		int n = arg2.checkInteger();

		int length = s.length();
		int position = (n >= 0) ? 1 : length + 1;
		position = posRelative(arg3.optInteger(position), length) - 1;
		if (position < 0 || position > length) throw ErrorFactory.argError(3, "position out of bounds");

		if (n == 0) {
			while (position > 0 && isCont(s, position)) position--;
		} else {
			if (isCont(s, position)) throw new LuaError("initial position is a continuation byte");

			if (n < 0) {
				while (n < 0 && position > 0) {
					do {
						position--;
					} while (position > 0 && isCont(s, position));
					n++;
				}
			} else {
				n--;
				while (n > 0 && position < length) {
					do {
						position++;
					} while (isCont(s, position));
					n--;
				}
			}
		}

		return n == 0 ? valueOf(position + 1) : NIL;
	}

	private static long decodeUtf8(LuaString str, int index, IntBuffer offset) {
		int first = str.charAt(index);
		if (first < 0x80) {
			offset.value = 1;
			return first;
		}

		int count = 0;
		long result = 0;
		int length = str.length();
		while ((first & 0x40) != 0) {
			// If we're expecting more bytes, abort.
			index++;
			if (index >= length) return -1;

			// Ensure we're a continuation byte
			int cc = str.charAt(index);
			if ((cc & 0xC0) != 0x80) return -1;

			// Add lower 6 bites from continuation
			count++;
			result = (result << 6) | (cc & 0x3F);
			first <<= 1;
		}

		result |= ((first & 0x7F)) << (count * 5);
		if (count > 3 || result > MAX_UNICODE | result <= LIMITS[count]) return -1;
		offset.value = count + 1;
		return result;
	}

	private static int posRelative(int pos, int len) {
		return pos >= 0 ? pos : len + pos + 1;
	}

	private static boolean isCont(LuaString s, int idx) {
		return idx < s.length() && (s.charAt(idx) & 0xC0) == 0x80;
	}

	/*
	 * An iterator for use in implementing utf8.codes.
	 */
	private static Varargs codesIter(LuaState state, Varargs args) throws LuaError {
		// Arg 1: invariant state (the string)
		// Arg 2: byte offset + 1
		// Returns: byte offset + 1, code point
		LuaString s = args.arg(1).checkLuaString();
		int idx = args.arg(2).checkInteger() - 1;
		IntBuffer off = new IntBuffer();
		if (idx < 0) {
			idx = 0;
		} else if (idx < s.length()) {
			idx++;
			while (isCont(s, idx)) idx++;
		}
		if (idx >= s.length()) {
			return varargsOf();
		} else {
			long codepoint = decodeUtf8(s, idx, off);
			if (codepoint == -1 || isCont(s, idx + off.value)) throw new LuaError("invalid UTF-8 code");
			return varargsOf(valueOf(idx + 1), LuaInteger.valueOf(codepoint));
		}
	}

	private static class IntBuffer {
		int value;
	}
}
