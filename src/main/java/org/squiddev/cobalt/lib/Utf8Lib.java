package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class Utf8Lib implements LuaLibrary {
	@Override
	public LuaValue add(LuaState state, LuaTable environment) {
		LuaTable t = new LuaTable(0, 6);
		t.rawset("charpattern", ValueFactory.valueOf("[\\0-\\x7F\\xC2-\\xF4][\\x80-\\xBF]*"));
		LibFunction.bind(t, Utf8Char::new, new String[]{ "char", "codes", "codepoint", "len", "offset" });
		environment.rawset("utf8", t);
		state.loadedPackages.rawset("utf8", t);
		return t;
	}

	private static class Utf8Char extends VarArgFunction {
		@Override
		public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
			switch(opcode) {
				case 0: {
					Buffer sb = new Buffer();
					for (int i = 1, n = args.count(); i <= n; i++) {
						int codePoint = args.arg(i).checkInteger();
						if (codePoint < 0x80) {
							sb.append((byte) codePoint);
						} else {
							sb.append(new String(new int[]{codePoint}, 0, 1).getBytes(StandardCharsets.UTF_8));
						}
					}
					return sb.value();
				}
				case 1: {
					LuaString s = args.arg(1).checkLuaString();
					byte[] c = new byte[s.length];
					s.copyTo(c, 0);
					return ValueFactory.varargsOf(new Utf8CodesIter(c), Constants.NIL, ValueFactory.valueOf(0));
				}
				case 2: {
					LuaString s = args.arg(1).checkLuaString();
					int i = (args.arg(2).isNil()) ? 1 : args.arg(2).checkInteger();
					int j = (args.arg(2).isNil()) ? i : args.arg(3).checkInteger();
					int[] off = new int[1]; long c; int n = 0;
					LuaNumber[] l = new LuaNumber[j - i + 1];

					do {
						c = decodeUtf8(s.bytes, i - 1, off);
						if (c < 0) {
							throw new LuaError("invalid UTF-8 byte sequence starting at index" + i);
						}
						l[n++] = (LuaInteger.valueOf(c));
					} while((i += off[0]) <= j);

					return ValueFactory.varargsOf(l, 0, n);
				}
				case 3: {
					LuaString s = args.arg(1).checkLuaString();
					int i = (args.arg(2).isNil()) ? 1 : args.arg(2).checkInteger();
					int j = (args.arg(2).isNil()) ? i : args.arg(3).checkInteger();
					int[] off = new int[1]; int n = 0;

					do {
						long c = decodeUtf8(s.bytes, i - 1, off);
						if (c < 0) {
							return ValueFactory.varargsOf(Constants.FALSE, LuaInteger.valueOf(i));
						}
						n++;
					} while((i += off[0]) <= j);

					return ValueFactory.varargsOf(LuaInteger.valueOf(n));
				}
				case 4: {
					LuaString s = args.arg(1).checkLuaString();
					int n = args.arg(2).checkInteger();
					int position = (n >= 0) ? 1 : s.length + 1;
					position = posRelative(args.arg(3).isNil() ? 1 : args.arg(3).checkInteger(), s.length);
					int[] off = new int[1];

					if (n == 0) {
						while(position > 0 && isCont(s, position)) position--;
						return ValueFactory.valueOf(position);
					}

					if (isCont(s, position)) {
						throw new LuaError("starting position for utf8.offset was in the middle of a code point (a continuation byte)");
					}

					if (n < 0) {
						while (n < 0 && position > 0) {
							do {
								position--;
							} while (position > 0 && isCont(s, position));
							n++;
						}
					} else {
						n--;
						while (n > 0 && position < s.length) {
							do {
								position++;
							} while(isCont(s, position));
							n--;
						}
					}
					if (n == 0)
						return ValueFactory.varargsOf(LuaInteger.valueOf(position + 1));
					else
						return ValueFactory.varargsOf();
				}
				default: throw new RuntimeException();
			}
		}
	}

	private static long decodeUtf8(byte[] bytes, int n, int[] offset) {
		int f = bytes[n] & 0xFF;
		if (f < 0x80) {
			offset[0] = 1;
			return bytes[n];
		} else if ((f & 0xe0) == 0xc0) {
			offset[0] = 2;
			return ((long)(bytes[n] & 0x1f) <<  6) |
					((long) (bytes[n + 1] & 0x3f));
		} else if ((bytes[0] & 0xf0) == 0xe0) {
			offset[0] = 3;
			return ((long)(bytes[n] & 0x0f) << 12) |
					((long)(bytes[n + 1] & 0x3f) <<  6) |
					((long) (bytes[n + 2] & 0x3f));
		} else if ((f & 0xf8) == 0xf0 && (f <= 0xf4)) {
			offset[0] = 4;
			return ((long)(bytes[n] & 0x07) << 18) |
					((long)(bytes[n + 1] & 0x3f) << 12) |
					((long)(bytes[n + 2] & 0x3f) <<  6) |
					((long) (bytes[n + 3] & 0x3f));
		} else {
			return -1;
		}
	}

	private static int posRelative(int pos, int len) {
		return pos >= 0 ? pos : len + pos + 1;
	}


	private static boolean isCont(byte[] s, int idx) {
		return idx < s.length && (s[idx] & 0xC0) == 0x80;
	}

	private static boolean isCont(LuaString s, int idx) {
		return isCont(s.bytes, idx);
	}

	/* An iterator for use in implementing utf8.codes. We store the bytes in the closure instead of in the iterator's
	*  invariant state in the hopes that this is the tiniest bit faster.
	*/
	private static class Utf8CodesIter extends VarArgFunction {
		byte[] s;

		protected Utf8CodesIter(byte[] bytes) {
			s = bytes;
		}

		@Override
		public Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
			// Arg 1: invariant state (nil)
			// Arg 2: byte offset + 1
			// Returns: byte offset + 1, code point
			int n = args.arg(2).checkInteger() - 1;
			if (n < 0) {
				n = 0;
			} else if (n < s.length) {
				n++;
				while (isCont(s, n)) n++;
			}
			if (n >= s.length) {
				return ValueFactory.varargsOf();
			} else {
				long c = decodeUtf8(s, n, new int[0]);
				return ValueFactory.varargsOf(LuaInteger.valueOf(n + 1), LuaInteger.valueOf(c));
			}
		}
	}
}
