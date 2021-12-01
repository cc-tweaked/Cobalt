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
package org.squiddev.cobalt.lib;


import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.DumpState;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.*;
import org.squiddev.cobalt.lib.StringFormat.FormatState;
import org.squiddev.cobalt.lib.StringMatch.GSubState;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.*;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code string}
 * library.
 *
 * This is a direct port of the corresponding library in C.
 *
 * @see LibFunction
 * @see JsePlatform
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.4">http://www.lua.org/manual/5.1/manual.html#5.4</a>
 */
public class StringLib implements LuaLibrary {
	static final int L_ESC = '%';

	@Override
	public LuaValue add(LuaState state, LuaTable env) {
		LuaTable t = new LuaTable();
		LibFunction.bind(t, StringLib1::new, new String[]{
			"len", "lower", "reverse", "upper", "packsize"
		});
		LibFunction.bind(t, StringLibV::new, new String[]{
			"dump", "byte", "char", "find", "gmatch", "match", "rep", "sub", "pack", "unpack"
		});
		LibFunction.bind(t, StringLibR::new, new String[]{"gsub", "format"});

		t.rawset("gfind", t.rawget("gmatch"));
		env.rawset("string", t);

		state.stringMetatable = tableOf(INDEX, t);
		state.loadedPackages.rawset("string", t);
		return t;
	}

	static final class StringLib1 extends OneArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
			switch (opcode) {
				case 0: // len (function)
					return valueOf(arg.checkLuaString().length());

				case 1: { // lower (function)
					LuaString string = arg.checkLuaString();
					if (string.length == 0) return EMPTYSTRING;

					byte[] value = new byte[string.length];
					System.arraycopy(string.bytes, string.offset, value, 0, value.length);
					for (int i = 0; i < value.length; i++) {
						byte c = value[i];
						if (c >= 'A' && c <= 'Z') value[i] = (byte) (c | 0x20);
					}
					return valueOf(value);
				}

				case 2: { // reverse (function)
					LuaString s = arg.checkLuaString();
					int n = s.length();
					byte[] b = new byte[n];
					for (int i = 0, j = n - 1; i < n; i++, j--) {
						b[j] = (byte) s.luaByte(i);
					}
					return LuaString.valueOf(b);
				}
				case 3: { // upper (function)
					LuaString string = arg.checkLuaString();
					if (string.length == 0) return EMPTYSTRING;

					byte[] value = new byte[string.length];
					System.arraycopy(string.bytes, string.offset, value, 0, value.length);
					for (int i = 0; i < value.length; i++) {
						byte c = value[i];
						if (c >= 'a' && c <= 'z') value[i] = (byte) (c & ~0x20);
					}
					return valueOf(value);
				}
				case 4: { // packsize
					return LuaInteger.valueOf(StringPacker.packsize(arg.checkLuaString()));
				}
			}
			return NIL;
		}
	}

	static final class StringLibV extends VarArgFunction {
		@Override
		public Varargs invoke(LuaState state, Varargs args) throws LuaError {
			switch (opcode) {
				case 0:
					return dump(args.arg(1).checkFunction(), args.arg(2).optBoolean(false));
				case 1:
					return byte_(args);
				case 2:
					return StringLib.char_(args);
				case 3:
					return StringMatch.find(state, args);
				case 4:
					return StringMatch.gmatch(state, args);
				case 5:
					return StringMatch.match(state, args);
				case 6:
					return StringLib.rep(args);
				case 7:
					return StringLib.sub(args);
				case 8:
					return StringPacker.pack(args);
				case 9:
					return StringPacker.unpack(args);
			}
			return NONE;
		}
	}

	static final class StringLibR extends ResumableVarArgFunction<Object> {
		@Override
		public Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0: { // gsub
					LuaString src = args.arg(1).checkLuaString();
					LuaString p = args.arg(2).checkLuaString();
					LuaValue replace = args.arg(3);
					int maxS = args.arg(4).optInteger(src.length() + 1);

					GSubState gsub = new GSubState(state, src, p, replace, maxS);
					di.state = gsub;
					return StringMatch.gsubRun(state, gsub, null);
				}
				case 1: { // format
					LuaString src = args.arg(1).checkLuaString();
					FormatState format = new FormatState(src, new Buffer(src.length), args);
					di.state = format;
					return StringFormat.format(state, format);
				}
				default:
					return NONE;
			}
		}

		@Override
		public Varargs resumeThis(LuaState state, Object object, Varargs value) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0: // gsub
					return StringMatch.gsubRun(state, (GSubState) object, value.first());
				case 1: { // format
					FormatState format = (FormatState) object;
					StringFormat.addString(format.buffer, format.current, OperationHelper.checkToString(value.first()));
					return StringFormat.format(state, format);
				}
				default:
					throw new NonResumableException("Cannot resume " + debugName());
			}
		}
	}


	/**
	 * string.byte (s [, i [, j]])
	 *
	 * Returns the internal numerical codes of the
	 * characters s[i], s[i+1], ..., s[j]. The default value for i is 1; the
	 * default value for j is i.
	 *
	 * Note that numerical codes are not necessarily portable across platforms.
	 *
	 * @param args the calling args
	 */
	static Varargs byte_(Varargs args) throws LuaError {
		LuaString s = args.arg(1).checkLuaString();
		int l = s.length;
		int posi = posRelative(args.arg(2).optInteger(1), l);
		int pose = posRelative(args.arg(3).optInteger(posi), l);
		int n, i;
		if (posi <= 0) posi = 1;
		if (pose > l) pose = l;
		if (posi > pose) return NONE;  /* empty interval; return no values */
		n = pose - posi + 1;
		if (posi + n <= pose)  /* overflow? */ {
			throw new LuaError("string slice too long");
		}
		LuaValue[] v = new LuaValue[n];
		for (i = 0; i < n; i++) {
			v[i] = valueOf(s.luaByte(posi + i - 1));
		}
		return varargsOf(v);
	}

	/**
	 * string.char (...)
	 *
	 * Receives zero or more integers. Returns a string with length equal
	 * to the number of arguments, in which each character has the internal
	 * numerical code equal to its corresponding argument.
	 *
	 * Note that numerical codes are not necessarily portable across platforms.
	 *
	 * @param args the calling VM
	 * @return The characters for this string
	 * @throws LuaError If the argument is not a number or is out of bounds.
	 */
	public static Varargs char_(Varargs args) throws LuaError {
		int n = args.count();
		byte[] bytes = new byte[n];
		for (int i = 0, a = 1; i < n; i++, a++) {
			int c = args.arg(a).checkInteger();
			if (c < 0 || c >= 256) {
				throw ErrorFactory.argError(a, "invalid value");
			}
			bytes[i] = (byte) c;
		}
		return LuaString.valueOf(bytes);
	}

	/**
	 * string.dump (function)
	 *
	 * Returns a string containing a binary representation of the given function,
	 * so that a later loadstring on this string returns a copy of the function.
	 * function must be a Lua function without upvalues.
	 *
	 * @throws LuaError If the function cannot be dumped.
	 */
	static LuaValue dump(LuaFunction f, boolean strip) throws LuaError {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			if (f instanceof LuaClosure) {
				DumpState.dump(((LuaClosure) f).getPrototype(), baos, strip);
				return LuaString.valueOf(baos.toByteArray());
			}

			throw new LuaError("Unable to dump given function");
		} catch (IOException e) {
			throw new LuaError(e.getMessage());
		}
	}


	/**
	 * string.rep (s, n [, sep])
	 *
	 * Returns a string that is the concatenation of n copies of the string s separated
	 * by the string sep. The default value for sep is the empty string (that is, no separator).
	 */
	static Varargs rep(Varargs args) throws LuaError {
		LuaString s = args.arg(1).checkLuaString();
		int n = args.arg(2).checkInteger();
		LuaString sep = args.arg(3).optLuaString(EMPTYSTRING);
		int len = s.length();
		int seplen = sep.length();

		if (n <= 0 || (len == 0 && seplen == 0)) {
			return Constants.EMPTYSTRING;
		} else if (n == 1) {
			return s;
		} else {
			final byte[] bytes = new byte[len * n + seplen * (n - 1)];
			for (int offset = 0; offset < bytes.length; offset += len + seplen) {
				s.copyTo(0, bytes, offset, len);
				if (offset + len + seplen < bytes.length)
					sep.copyTo(0, bytes, offset + len, seplen);
			}
			return LuaString.valueOf(bytes);
		}
	}

	/**
	 * string.sub (s, i [, j])
	 *
	 * Returns the substring of s that starts at i and continues until j;
	 * i and j may be negative. If j is absent, then it is assumed to be equal to -1
	 * (which is the same as the string length). In particular, the call
	 * string.sub(s,1,j)
	 * returns a prefix of s with length j, and
	 * string.sub(s, -i)
	 * returns a suffix of s with length i.
	 */
	static Varargs sub(Varargs args) throws LuaError {
		final LuaString s = args.arg(1).checkLuaString();
		final int l = s.length();

		int start = posRelative(args.arg(2).checkInteger(), l);
		int defval = -1;
		int end = posRelative(args.arg(3).optInteger(defval), l);

		if (start < 1) start = 1;
		if (end > l) end = l;
		if (start <= end) {
			return s.substring(start - 1, end);
		} else {
			return EMPTYSTRING;
		}
	}

	static int posRelative(int pos, int len) {
		if (pos >= 0) return pos;
		if (-pos > len) return 0;
		return len + pos + 1;
	}

	public static boolean isWhitespace(byte b) {
		return StringMatch.isWhitespace(b);
	}
}
