/**
 * ****************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
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
package org.luaj.vm2.lib;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.InputStream;

import static org.luaj.vm2.Constants.*;
import static org.luaj.vm2.Factory.valueOf;
import static org.luaj.vm2.Factory.varargsOf;

/**
 * Subclass of {@link LibFunction} which implements the lua basic library functions.
 * <p>
 * This contains all library functions listed as "basic functions" in the lua documentation for JME.
 * The functions dofile and loadfile use the
 * {@link LuaState#resourceManipulator} instance to find resource files.
 * The default loader chain in {@link PackageLib} will use these as well.
 * <p>
 * This is a direct port of the corresponding library in C.
 *
 * @see org.luaj.vm2.lib.platform.ResourceManipulator
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
		return env;
	}

	private static final class BaseLib2 extends TwoArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) {
			switch (opcode) {
				case 0: // "collectgarbage", // ( opt [,arg] ) -> value
					String s = arg1.checkjstring();
					if ("collect".equals(s)) {
						System.gc();
						return ZERO;
					} else if ("count".equals(s)) {
						Runtime rt = Runtime.getRuntime();
						long used = rt.totalMemory() - rt.freeMemory();
						return valueOf(used / 1024.);
					} else if ("step".equals(s)) {
						System.gc();
						return TRUE;
					} else {
						argError(1, "gc op");
					}
					return NIL;
				case 1: // "error", // ( message [,level] ) -> ERR
					throw new LuaError(arg1.isnil() ? null : arg1.tojstring(), arg2.optint(1));
				case 2: { // "setfenv", // (f, table) -> void
					LuaTable t = arg2.checktable();
					LuaValue f = getfenvobj(arg1);
					if (!f.isthread() && !f.isclosure()) {
						throw new LuaError("'setfenv' cannot change environment of given object");
					}
					f.setfenv(t);
					return f.isthread() ? NONE : f;
				}
			}
			return NIL;
		}
	}

	private static LuaValue getfenvobj(LuaValue arg) {
		if (arg.isfunction()) {
			return arg;
		}
		int level = arg.optint(1);
		arg.argcheck(level >= 0, 1, "level must be non-negative");
		if (level == 0) {
			return LuaThread.getRunning();
		}
		LuaValue f = LuaThread.getCallstackFunction(level);
		arg.argcheck(f != null, 1, "invalid level");
		return f;
	}

	private static final class BaseLibV extends VarArgFunction {
		public BaseLib baselib;

		@Override
		public Varargs invoke(LuaState state, Varargs args) {
			switch (opcode) {
				case 0: // "assert", // ( v [,message] ) -> v, message | ERR
					if (!args.arg1().toboolean()) {
						throw new LuaError(args.narg() > 1 ? args.optjstring(2, "assertion failed!") : "assertion failed!");
					}
					return args;
				case 1: // "dofile", // ( filename ) -> result1, ...
				{
					Varargs v = args.isnil(1) ?
						BaseLib.loadStream(state.STDIN, "=stdin") :
						BaseLib.loadFile(state, args.checkjstring(1));
					if (v.isnil(1)) {
						throw new LuaError(v.tojstring(2));
					} else {
						return v.arg1().invoke(state, NONE);
					}
				}
				case 2: // "getfenv", // ( [f] ) -> env
				{
					LuaValue f = getfenvobj(args.arg1());
					LuaValue e = f.getfenv();
					return e != null ? e : NIL;
				}
				case 3: // "getmetatable", // ( object ) -> table
				{
					LuaValue mt = args.checkvalue(1).getMetatable(state);
					return mt != null ? mt.rawget(state, METATABLE).optvalue(mt) : NIL;
				}
				case 4: // "load", // ( func [,chunkname] ) -> chunk | nil, msg
				{
					LuaValue func = args.checkfunction(1);
					String chunkname = args.optjstring(2, "function");
					return BaseLib.loadStream(new StringInputStream(state, func), chunkname);
				}
				case 5: // "loadfile", // ( [filename] ) -> chunk | nil, msg
				{
					return args.isnil(1) ?
						BaseLib.loadStream(state.STDIN, "stdin") :
						BaseLib.loadFile(state, args.checkjstring(1));
				}
				case 6: // "loadstring", // ( string [,chunkname] ) -> chunk | nil, msg
				{
					LuaString script = args.checkstring(1);
					String chunkname = args.optjstring(2, "string");
					return BaseLib.loadStream(script.toInputStream(), chunkname);
				}
				case 7: // "pcall", // (f, arg1, ...) -> status, result1, ...
				{
					LuaValue func = args.checkvalue(1);
					LuaThread.CallStack cs = LuaThread.onCall(this);
					try {
						return pcall(state, func, args.subargs(2), null);
					} finally {
						cs.onReturn();
					}
				}
				case 8: // "xpcall", // (f, err) -> result1, ...
				{
					LuaThread.CallStack cs = LuaThread.onCall(this);
					try {
						return pcall(state, args.arg1(), NONE, args.checkvalue(2));
					} finally {
						cs.onReturn();
					}
				}
				case 9: // "print", // (...) -> void
				{
					LuaValue tostring = LuaThread.getGlobals().get(state, "tostring");
					for (int i = 1, n = args.narg(); i <= n; i++) {
						if (i > 1) state.STDOUT.write('\t');
						LuaString s = tostring.call(state, args.arg(i)).strvalue();
						int z = s.indexOf((byte) 0, 0);
						state.STDOUT.write(s.m_bytes, s.m_offset, z >= 0 ? z : s.m_length);
					}
					state.STDOUT.println();
					return NONE;
				}
				case 10: // "select", // (f, ...) -> value1, ...
				{
					int n = args.narg() - 1;
					if (args.arg1().equals(valueOf("#"))) {
						return valueOf(n);
					}
					int i = args.checkint(1);
					if (i == 0 || i < -n) {
						argError(1, "index out of range");
					}
					return args.subargs(i < 0 ? n + i + 2 : i + 1);
				}
				case 11: // "unpack", // (list [,i [,j]]) -> result1, ...
				{
					int na = args.narg();
					LuaTable t = args.checktable(1);
					int n = t.length(state);
					int i = na >= 2 ? args.checkint(2) : 1;
					int j = na >= 3 ? args.checkint(3) : n;
					n = j - i + 1;
					if (n < 0) return NONE;
					if (n == 1) return t.get(state, i);
					if (n == 2) return varargsOf(t.get(state, i), t.get(state, j));
					LuaValue[] v = new LuaValue[n];
					for (int k = 0; k < n; k++) {
						v[k] = t.get(state, i + k);
					}
					return varargsOf(v);
				}
				case 12: // "type",  // (v) -> value
					return valueOf(args.checkvalue(1).typeName());
				case 13: // "rawequal", // (v1, v2) -> boolean
					return valueOf(args.checkvalue(1) == args.checkvalue(2));
				case 14: // "rawget", // (table, index) -> value
					return args.checktable(1).rawget(state, args.checkvalue(2));
				case 15: { // "rawset", // (table, index, value) -> table
					LuaTable t = args.checktable(1);
					t.rawset(state, args.checknotnil(2), args.checkvalue(3));
					return t;
				}
				case 16: { // "setmetatable", // (table, metatable) -> table
					final LuaValue t = args.arg1();
					final LuaValue mt0 = t.getMetatable(state);
					if (mt0 != null && !mt0.rawget(state, METATABLE).isnil()) {
						throw new LuaError("cannot change a protected metatable");
					}
					final LuaValue mt = args.checkvalue(2);
					return t.setMetatable(state, mt.isnil() ? null : mt.checktable());
				}
				case 17: { // "tostring", // (e) -> value
					LuaValue arg = args.checkvalue(1);
					LuaValue h = arg.metatag(state, TOSTRING);
					if (!h.isnil()) {
						return h.call(state, arg);
					}
					LuaValue v = arg.tostring();
					if (!v.isnil()) {
						return v;
					}
					return valueOf(arg.tojstring());
				}
				case 18: { // "tonumber", // (e [,base]) -> value
					LuaValue arg1 = args.checkvalue(1);
					final int base = args.optint(2, 10);
					if (base == 10) {  /* standard conversion */
						return arg1.tonumber();
					} else {
						if (base < 2 || base > 36) {
							argError(2, "base out of range");
						}
						return arg1.checkstring().tonumber(base);
					}
				}
				case 19: // "pairs" (t) -> iter-func, t, nil
					return varargsOf(baselib.next, args.checktable(1), NIL);
				case 20: // "ipairs", // (t) -> iter-func, t, 0
					return varargsOf(baselib.inext, args.checktable(1), ZERO);
				case 21: // "next"  ( table, [index] ) -> next-index, next-value
					return args.checktable(1).next(args.arg(2));
				case 22: // "inext" ( table, [int-index] ) -> next-index, next-value
					return args.checktable(1).inext(args.arg(2));
			}
			return NONE;
		}
	}

	public static Varargs pcall(LuaState state, LuaValue func, Varargs args, LuaValue errfunc) {
		LuaValue olderr = LuaThread.setErrorFunc(errfunc);
		try {
			Varargs result = varargsOf(TRUE, func.invoke(state, args));
			LuaThread.setErrorFunc(olderr);
			return result;
		} catch (LuaError le) {
			LuaThread.setErrorFunc(olderr);
			String m = le.getMessage();
			return varargsOf(FALSE, m != null ? valueOf(m) : NIL);
		} catch (Exception e) {
			LuaThread.setErrorFunc(olderr);
			String m = e.getMessage();
			return varargsOf(FALSE, valueOf(m != null ? m : e.toString()));
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
			return varargsOf(NIL, valueOf("cannot open " + filename + ": No such file or directory"));
		}
		try {
			return loadStream(is, "@" + filename);
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static Varargs loadStream(InputStream is, String chunkname) {
		try {
			if (is == null) {
				return varargsOf(NIL, valueOf("not found: " + chunkname));
			}
			return LoadState.load(is, chunkname, LuaThread.getGlobals());
		} catch (Exception e) {
			return varargsOf(NIL, valueOf(e.getMessage()));
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
				if (s.isnil()) {
					return -1;
				}
				LuaString ls = s.strvalue();
				bytes = ls.m_bytes;
				offset = ls.m_offset;
				remaining = ls.m_length;
				if (remaining <= 0) {
					return -1;
				}
			}
			--remaining;
			return bytes[offset++];
		}
	}
}
