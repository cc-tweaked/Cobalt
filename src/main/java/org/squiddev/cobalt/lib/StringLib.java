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
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.*;
import org.squiddev.cobalt.lib.StringFormat.FormatState;
import org.squiddev.cobalt.lib.StringMatch.GSubState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.*;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code string}
 * library.
 * <p>
 * This is a direct port of the corresponding library in C.
 *
 * @see LibFunction
 * @see CoreLibraries
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.4">http://www.lua.org/manual/5.1/manual.html#5.4</a>
 */
public final class StringLib {
	static final int L_ESC = '%';
	private static final int MAX_LEN = Integer.MAX_VALUE;

	private StringLib() {
	}

	public static void add(LuaState state, LuaTable env) throws LuaError {
		LuaTable t = RegisteredFunction.bind(new RegisteredFunction[]{
			RegisteredFunction.of("len", StringLib::len),
			RegisteredFunction.of("lower", StringLib::lower),
			RegisteredFunction.of("reverse", StringLib::reverse),
			RegisteredFunction.of("upper", StringLib::upper),
			RegisteredFunction.of("packsize", StringLib::packsize),
			RegisteredFunction.of("dump", StringLib::dump),
			RegisteredFunction.ofV("byte", StringLib::byte$),
			RegisteredFunction.ofV("char", StringLib::char$),
			RegisteredFunction.ofV("find", StringMatch::find),
			RegisteredFunction.ofV("gmatch", StringMatch::gmatch),
			RegisteredFunction.ofV("match", StringMatch::match),
			RegisteredFunction.ofV("rep", StringLib::rep),
			RegisteredFunction.ofV("sub", StringLib::sub),
			RegisteredFunction.ofV("pack", (s, args) -> StringPacker.pack(args)),
			RegisteredFunction.ofV("unpack", (s, args) -> StringPacker.unpack(args)),
			RegisteredFunction.ofFactory("gsub", GSub::new),
			RegisteredFunction.ofFactory("format", Format::new),
		});

		t.rawset("gfind", t.rawget("gmatch"));

		LibFunction.setGlobalLibrary(state, env, "string", t);
		state.stringMetatable = tableOf(INDEX, t);
	}

	private static LuaValue len(LuaState state, LuaValue arg) throws LuaError {
		return valueOf(arg.checkLuaString().length());
	}

	private static LuaValue lower(LuaState state, LuaValue arg) throws LuaError {
		LuaString string = arg.checkLuaString();

		// Find the first capital letter,
		int i = 0, len = string.length();
		for (; i < len; i++) {
			byte b = string.byteAt(i);
			if (b >= 'A' && b <= 'Z') break;
		}
		// If there are none, just return the string unchanged.
		if (i == len) return string;

		byte[] value = new byte[len];
		string.copyTo(value, 0);
		for (; i < value.length; i++) {
			byte c = value[i];
			if (c >= 'A' && c <= 'Z') value[i] = (byte) (c | 0x20);
		}
		return valueOf(value);
	}

	private static LuaValue reverse(LuaState state, LuaValue arg) throws LuaError {
		LuaString s = arg.checkLuaString();
		int n = s.length();
		byte[] b = new byte[n];
		for (int i = 0, j = n - 1; i < n; i++, j--) b[j] = s.byteAt(i);
		return LuaString.valueOf(b);
	}

	private static LuaValue upper(LuaState state, LuaValue arg) throws LuaError {
		LuaString string = arg.checkLuaString();

		// Find the first lower-case letter
		int i = 0, len = string.length();
		for (; i < len; i++) {
			byte b = string.byteAt(i);
			if (b >= 'a' && b <= 'z') break;
		}
		// If there are none, just return the string unchanged.
		if (i == len) return string;

		byte[] value = new byte[string.length()];
		string.copyTo(value, 0);
		for (i = 0; i < value.length; i++) {
			byte c = value[i];
			if (c >= 'a' && c <= 'z') value[i] = (byte) (c & ~0x20);
		}
		return valueOf(value);
	}

	private static LuaValue packsize(LuaState state, LuaValue arg) throws LuaError {
		return LuaInteger.valueOf(StringPacker.packsize(arg.checkLuaString()));
	}

	private static final class GSub extends ResumableVarArgFunction<GSubState> {
		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaString src = args.arg(1).checkLuaString();
			LuaString p = args.arg(2).checkLuaString();
			LuaValue replace = args.arg(3);
			int maxS = args.arg(4).optInteger(src.length() + 1);

			GSubState gsub = new GSubState(state, src, p, replace, maxS);
			di.state = gsub;
			return StringMatch.gsubRun(state, gsub, null);
		}

		@Override
		public Varargs resume(LuaState state, GSubState subState, Varargs value) throws LuaError, UnwindThrowable {
			return StringMatch.gsubRun(state, subState, value.first());
		}
	}

	private static final class Format extends ResumableVarArgFunction<FormatState> {
		@Override
		public Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaString src = args.arg(1).checkLuaString();
			FormatState format = new FormatState(src, new Buffer(src.length()), args);
			di.state = format;
			return StringFormat.format(state, format);
		}

		@Override
		public Varargs resume(LuaState state, FormatState formatState, Varargs value) throws LuaError, UnwindThrowable {
			LuaString s = OperationHelper.checkToString(value.first());
			formatState.current.format(formatState.buffer, s);
			return StringFormat.format(state, formatState);
		}
	}

	/**
	 * string.byte (s [, i [, j]])
	 * <p>
	 * Returns the internal numerical codes of the
	 * characters s[i], s[i+1], ..., s[j]. The default value for i is 1; the
	 * default value for j is i.
	 * <p>
	 * Note that numerical codes are not necessarily portable across platforms.
	 *
	 * @param args the calling args
	 */
	private static Varargs byte$(LuaState state, Varargs args) throws LuaError {
		LuaString s = args.arg(1).checkLuaString();
		int l = s.length();
		int posi = posRelative(args.arg(2).optInteger(1), l);
		int pose = posRelative(args.arg(3).optInteger(posi), l);
		if (posi <= 0) posi = 1;
		if (pose > l) pose = l;
		if (posi == pose) return valueOf(s.charAt(posi - 1)); // Do the common case first.

		if (posi > pose) return NONE; // empty interval; return no values
		int n = pose - posi + 1;
		if (posi + n <= pose)  /* overflow? */ {
			throw new LuaError("string slice too long");
		}
		LuaValue[] v = new LuaValue[n];
		for (int i = 0; i < n; i++) {
			v[i] = valueOf(s.charAt(posi + i - 1));
		}
		return varargsOf(v);
	}

	/**
	 * string.char (...)
	 * <p>
	 * Receives zero or more integers. Returns a string with length equal
	 * to the number of arguments, in which each character has the internal
	 * numerical code equal to its corresponding argument.
	 * <p>
	 * Note that numerical codes are not necessarily portable across platforms.
	 *
	 * @param args the calling VM
	 * @return The characters for this string
	 * @throws LuaError If the argument is not a number or is out of bounds.
	 */
	private static LuaValue char$(LuaState state, Varargs args) throws LuaError {
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
	 * <p>
	 * Returns a string containing a binary representation of the given function,
	 * so that a later loadstring on this string returns a copy of the function.
	 * function must be a Lua function without upvalues.
	 *
	 * @throws LuaError If the function cannot be dumped.
	 */
	static LuaValue dump(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		LuaFunction f = arg1.checkFunction();
		boolean strip = arg2.optBoolean(false);
		var bytecode = state.getBytecodeFormat();

		if (!(f instanceof LuaClosure closure) || bytecode == null) throw new LuaError("unable to dump given function");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			bytecode.writeFunction(baos, closure.getPrototype(), strip);
		} catch (IOException e) {
			throw new LuaError(e.getMessage());
		}

		return LuaString.valueOf(baos.toByteArray());
	}

	/**
	 * string.rep (s, n[, sep])
	 * <p>
	 * Returns a string that is the concatenation of n copies of the string s.
	 */
	private static Varargs rep(LuaState state, Varargs args) throws LuaError {
		LuaString s = args.arg(1).checkLuaString();
		int len = s.length();
		int n = args.arg(2).checkInteger();
		LuaString sep = args.arg(3).optLuaString(EMPTYSTRING);

		if (n <= 0) return Constants.EMPTYSTRING;
		if (n == 1) return s;

		long newLen = (long) len * n + (long) sep.length() * (n - 1);
		if (newLen > MAX_LEN) throw new LuaError("resulting string too large");

		final byte[] bytes = new byte[(int) newLen];
		// n >= 2, so copy in the initial string and separator.
		s.copyTo(bytes, 0);
		sep.copyTo(bytes, len);

		// Now, copy the first 0..i characters to i+1..i*2, where i doubles each time.
		int sliceLength = len + sep.length();
		long endIndex = newLen - len;
		for (long i = sliceLength; i != 0 && i < endIndex; i <<= 1) {
			System.arraycopy(bytes, 0, bytes, (int) i, (int) (Math.min(i << 1, endIndex) - i));
		}
		// The above code writes a repeated sequence of "s .. sep". Finish off with one final "s".
		s.copyTo(bytes, (int) endIndex);

		return LuaString.valueOf(bytes);
	}

	/**
	 * string.sub (s, i [, j])
	 * <p>
	 * Returns the substring of s that starts at i and continues until j;
	 * i and j may be negative. If j is absent, then it is assumed to be equal to -1
	 * (which is the same as the string length). In particular, the call
	 * string.sub(s,1,j)
	 * returns a prefix of s with length j, and
	 * string.sub(s, -i)
	 * returns a suffix of s with length i.
	 */
	private static Varargs sub(LuaState state, Varargs args) throws LuaError {
		final LuaString s = args.arg(1).checkLuaString();
		final int l = s.length();

		int start = posRelative(args.arg(2).checkInteger(), l);
		int defval = -1;
		int end = posRelative(args.arg(3).optInteger(defval), l);

		if (start < 1) start = 1;
		if (end > l) end = l;
		if (start <= end) {
			return s.substringOfEnd(start - 1, end);
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
