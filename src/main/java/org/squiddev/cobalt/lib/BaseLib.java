/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.OneArgFunction;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.lib.platform.ResourceManipulator;

import java.io.IOException;
import java.io.InputStream;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Subclass of {@link LibFunction} which implements the lua basic library functions.
 *
 * This contains all library functions listed as "basic functions" in the lua documentation for JME.
 * The functions dofile and loadfile use the
 * {@link LuaState#resourceManipulator} instance to find resource files.
 * The default loader chain in {@link PackageLib} will use these as well.
 *
 * This is a direct port of the corresponding library in C.
 *
 * @see ResourceManipulator
 * @see LibFunction
 * @see JsePlatform
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.1">http://www.lua.org/manual/5.1/manual.html#5.1</a>
 */
public class BaseLib extends OneArgFunction {
	private LuaValue next;
	private LuaValue inext;

	private static final String[] LIB2_KEYS = {
		"collectgarbage", // ( opt [,arg] ) -> value
		"error", // ( message [,level] ) -> ERR
		"setfenv", // (f, table) -> void
	};
	private static final String[] LIBV_KEYS = {
		"assert", // ( v [,message] ) -> v, message | ERR
		"dofile", // ( filename ) -> result1, ...
		"getfenv", // ( [f] ) -> env
		"getmetatable", // ( object ) -> table
		"load", // ( func [,chunkname] ) -> chunk | nil, msg
		"loadfile", // ( [filename] ) -> chunk | nil, msg
		"loadstring", // ( string [,chunkname] ) -> chunk | nil, msg
		"pcall", // (f, arg1, ...) -> status, result1, ...
		"xpcall", // (f, err) -> result1, ...
		"print", // (...) -> void
		"select", // (f, ...) -> value1, ...
		"unpack", // (list [,i [,j]]) -> result1, ...
		"type",  // (v) -> value
		"rawequal", // (v1, v2) -> boolean
		"rawget", // (table, index) -> value
		"rawset", // (table, index, value) -> table
		"setmetatable", // (table, metatable) -> table
		"tostring", // (e) -> value
		"tonumber", // (e [,base]) -> value
		"pairs", // "pairs" (t) -> iter-func, t, nil
		"ipairs", // "ipairs", // (t) -> iter-func, t, 0
		"next", // "next"  ( table, [index] ) -> next-index, next-value
		"__inext", // "inext" ( table, [int-index] ) -> next-index, next-value
	};

	@Override
	public LuaValue call(LuaState state, LuaValue arg) {
		env.set(state, "_G", env);
		env.set(state, "_VERSION", valueOf(Lua._VERSION));
		bind(state, env, BaseLib2.class, LIB2_KEYS);
		bind(state, env, BaseLibV.class, LIBV_KEYS);

		// remember next, and inext for use in pairs and ipairs
		next = env.get(state, "next");
		inext = env.get(state, "__inext");

		// inject base lib int vararg instances
		for (String LIBV_KEY : LIBV_KEYS) {
			((BaseLibV) env.get(state, LIBV_KEY)).baselib = this;
		}

		env.rawset("_VERSION", valueOf("Lua 5.1"));

		return env;
	}

	private static final class BaseLib2 extends TwoArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) {
			switch (opcode) {
				case 0: // "collectgarbage", // ( opt [,arg] ) -> value
					String s = arg1.optString("collect");
					if ("collect".equals(s)) {
						System.gc();
						return Constants.ZERO;
					} else if ("count".equals(s)) {
						Runtime rt = Runtime.getRuntime();
						long used = rt.totalMemory() - rt.freeMemory();
						return valueOf(used / 1024.);
					} else if ("step".equals(s)) {
						System.gc();
						return Constants.TRUE;
					} else {
						throw ErrorFactory.argError(1, "gc op");
					}
				case 1: // "error", // ( message [,level] ) -> ERR
					throw new LuaError(arg1.isNil() ? Constants.NIL : arg1, arg2.optInteger(1));
				case 2: { // "setfenv", // (f, table) -> void
					LuaTable t = arg2.checkTable();
					LuaValue f = getfenvobj(state, arg1);
					if (!f.isThread() && !f.isClosure()) {
						throw new LuaError("'setfenv' cannot change environment of given object");
					}
					f.setfenv(t);
					return f.isThread() ? Constants.NONE : f;
				}
			}
			return Constants.NIL;
		}
	}

	private static LuaValue getfenvobj(LuaState state, LuaValue arg) {
		if (arg.isFunction()) {
			return arg;
		}
		int level = arg.optInteger(1);
		Varargs.argCheck(level >= 0, 1, "level must be non-negative");
		if (level == 0) {
			return state.getCurrentThread();
		}
		LuaValue f = LuaThread.getCallstackFunction(state, level - 1);
		Varargs.argCheck(f != null, 1, "invalid level");
		return f;
	}

	private static final class BaseLibV extends VarArgFunction {
		public BaseLib baselib;

		@Override
		public Varargs invoke(LuaState state, Varargs args) {
			switch (opcode) {
				case 0: // "assert", // ( v [,message] ) -> v, message | ERR
					if (!args.first().toBoolean()) {
						throw new LuaError(args.count() > 1 ? args.arg(2).optString("assertion failed!") : "assertion failed!");
					}
					return args;
				case 1: // "dofile", // ( filename ) -> result1, ...
				{
					Varargs v = args.isNil(1) ?
						BaseLib.loadStream(state, state.stdin, "=stdin") :
						BaseLib.loadFile(state, args.arg(1).checkString());
					if (v.isNil(1)) {
						throw new LuaError(v.arg(2).toString());
					} else {
						return v.first().invoke(state, Constants.NONE);
					}
				}
				case 2: // "getfenv", // ( [f] ) -> env
				{
					LuaValue f = getfenvobj(state, args.first());
					LuaValue e = f.getfenv();
					return e != null ? e : Constants.NIL;
				}
				case 3: // "getmetatable", // ( object ) -> table
				{
					LuaValue mt = args.checkValue(1).getMetatable(state);
					return mt != null ? mt.rawget(Constants.METATABLE).optValue(mt) : Constants.NIL;
				}
				case 4: // "load", // ( func [,chunkname] ) -> chunk | nil, msg
				{
					LuaValue func = args.arg(1).checkFunction();
					String chunkname = args.arg(2).optString("function");
					return BaseLib.loadStream(state, new StringInputStream(state, func), chunkname);
				}
				case 5: // "loadfile", // ( [filename] ) -> chunk | nil, msg
				{
					return args.isNil(1) ?
						BaseLib.loadStream(state, state.stdin, "stdin") :
						BaseLib.loadFile(state, args.arg(1).checkString());
				}
				case 6: // "loadstring", // ( string [,chunkname] ) -> chunk | nil, msg
				{
					LuaString script = args.arg(1).checkLuaString();
					String chunkname = args.arg(2).optString("string");
					return BaseLib.loadStream(state, script.toInputStream(), chunkname);
				}
				case 7: // "pcall", // (f, arg1, ...) -> status, result1, ...
				{
					LuaValue func = args.checkValue(1);
					DebugState ds = state.debug.getDebugState();
					ds.onCall(this);
					try {
						return pcall(state, func, args.subargs(2), null);
					} finally {
						ds.onReturn();
					}
				}
				case 8: // "xpcall", // (f, err) -> result1, ...
				{
					DebugState ds = state.debug.getDebugState();
					ds.onCall(this);
					try {
						return pcall(state, args.first(), Constants.NONE, args.checkValue(2));
					} finally {
						ds.onReturn();
					}
				}
				case 9: // "print", // (...) -> void
				{
					LuaValue tostring = state.getCurrentThread().getfenv().get(state, "tostring");
					for (int i = 1, n = args.count(); i <= n; i++) {
						if (i > 1) state.stdout.write('\t');
						LuaString s = tostring.call(state, args.arg(i)).strvalue();
						int z = s.indexOf((byte) 0, 0);
						state.stdout.write(s.bytes, s.offset, z >= 0 ? z : s.length);
					}
					state.stdout.println();
					return Constants.NONE;
				}
				case 10: // "select", // (f, ...) -> value1, ...
				{
					int n = args.count() - 1;
					if (args.first().equals(valueOf("#"))) {
						return valueOf(n);
					}
					int i = args.arg(1).checkInteger();
					if (i == 0 || i < -n) {
						throw ErrorFactory.argError(1, "index out of range");
					}
					return args.subargs(i < 0 ? n + i + 2 : i + 1);
				}
				case 11: // "unpack", // (list [,i [,j]]) -> result1, ...
				{
					int na = args.count();
					LuaTable t = args.arg(1).checkTable();
					int n = t.length(state);
					int i = na >= 2 ? args.arg(2).checkInteger() : 1;
					int j = na >= 3 ? args.arg(3).checkInteger() : n;
					n = j - i + 1;
					if (n < 0) return Constants.NONE;
					if (n == 1) return t.get(state, i);
					if (n == 2) return varargsOf(t.get(state, i), t.get(state, j));
					LuaValue[] v = new LuaValue[n];
					for (int k = 0; k < n; k++) {
						v[k] = t.get(state, i + k);
					}
					return varargsOf(v);
				}
				case 12: // "type",  // (v) -> value
					return valueOf(args.checkValue(1).typeName());
				case 13: // "rawequal", // (v1, v2) -> boolean
					return valueOf(args.checkValue(1) == args.checkValue(2));
				case 14: // "rawget", // (table, index) -> value
					return args.arg(1).checkTable().rawget(args.checkValue(2));
				case 15: { // "rawset", // (table, index, value) -> table
					LuaTable t = args.arg(1).checkTable();
					t.rawset(args.checkNotNil(2), args.checkValue(3));
					return t;
				}
				case 16: { // "setmetatable", // (table, metatable) -> table
					final LuaValue t = args.first();
					final LuaValue mt0 = t.getMetatable(state);
					if (mt0 != null && !mt0.rawget(Constants.METATABLE).isNil()) {
						throw new LuaError("cannot change a protected metatable");
					}
					final LuaValue mt = args.checkValue(2);
					return t.setMetatable(state, mt.isNil() ? null : mt.checkTable());
				}
				case 17: { // "tostring", // (e) -> value
					LuaValue arg = args.checkValue(1);
					LuaValue h = arg.metatag(state, Constants.TOSTRING);
					if (!h.isNil()) {
						return h.call(state, arg);
					}
					LuaValue v = arg.toLuaString();
					if (!v.isNil()) {
						return v;
					}
					return valueOf(arg.toString());
				}
				case 18: { // "tonumber", // (e [,base]) -> value
					LuaValue arg1 = args.checkValue(1);
					final int base = args.arg(2).optInteger(10);
					if (base == 10) {  /* standard conversion */
						return arg1.toNumber();
					} else {
						if (base < 2 || base > 36) {
							throw ErrorFactory.argError(2, "base out of range");
						}
						return arg1.checkLuaString().tonumber(base);
					}
				}
				case 19: // "pairs" (t) -> iter-func, t, nil
					return varargsOf(baselib.next, args.arg(1).checkTable(), Constants.NIL);
				case 20: // "ipairs", // (t) -> iter-func, t, 0
					return varargsOf(baselib.inext, args.arg(1).checkTable(), Constants.ZERO);
				case 21: // "next"  ( table, [index] ) -> next-index, next-value
					return args.arg(1).checkTable().next(args.arg(2));
				case 22: // "inext" ( table, [int-index] ) -> next-index, next-value
					return args.arg(1).checkTable().inext(args.arg(2));
			}
			return Constants.NONE;
		}
	}

	public static Varargs pcall(LuaState state, LuaValue func, Varargs args, LuaValue errfunc) {
		LuaValue olderr = LuaThread.setErrorFunc(state, errfunc);
		try {
			Varargs result = varargsOf(Constants.TRUE, func.invoke(state, args));
			LuaThread.setErrorFunc(state, olderr);
			return result;
		} catch (LuaError le) {
			le.fillTraceback(state);

			LuaThread.setErrorFunc(state, olderr);
			return varargsOf(Constants.FALSE, le.value);
		} catch (Exception e) {
			LuaThread.setErrorFunc(state, olderr);
			String m = e.getMessage();
			return varargsOf(Constants.FALSE, valueOf(m != null ? m : e.toString()));
		}
	}

	/**
	 * Load from a named file, returning the chunk or nil,error of can't load
	 *
	 * @param state    The current lua state
	 * @param filename Name of the file
	 * @return Varargs containing chunk, or NIL,error-text on error
	 */
	public static Varargs loadFile(LuaState state, String filename) {
		InputStream is = state.resourceManipulator.findResource(filename);
		if (is == null) {
			return varargsOf(Constants.NIL, valueOf("cannot open " + filename + ": No such file or directory"));
		}
		try {
			return loadStream(state, is, "@" + filename);
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static Varargs loadStream(LuaState state, InputStream is, String chunkname) {
		try {
			if (is == null) {
				return varargsOf(Constants.NIL, valueOf("not found: " + chunkname));
			}
			return LoadState.load(state, is, chunkname, state.getCurrentThread().getfenv());
		} catch (Exception e) {
			return varargsOf(Constants.NIL, valueOf(e.getMessage()));
		}
	}


	private static class StringInputStream extends InputStream {
		private final LuaState state;
		final LuaValue func;
		byte[] bytes;
		int offset, remaining = 0;

		StringInputStream(LuaState state, LuaValue func) {
			this.state = state;
			this.func = func;
		}

		@Override
		public int read() throws IOException {
			if (remaining <= 0) {
				LuaValue s = func.call(state);
				if (s.isNil()) {
					return -1;
				}
				LuaString ls = s.strvalue();
				bytes = ls.bytes;
				offset = ls.offset;
				remaining = ls.length;
				if (remaining <= 0) {
					return -1;
				}
			}
			--remaining;
			return bytes[offset++];
		}
	}
}