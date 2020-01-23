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
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.ResumableVarArgFunction;
import org.squiddev.cobalt.function.ZeroArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.lib.platform.ResourceManipulator;

import java.io.InputStream;

import static org.squiddev.cobalt.OperationHelper.noUnwind;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_ERROR;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_YPCALL;
import static org.squiddev.cobalt.function.LibFunction.*;

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
public class BaseLib implements LuaLibrary {
	private static final LuaString STDIN_STR = valueOf("=stdin");
	private static final LuaString FUNCTION_STR = valueOf("function");

	private LuaValue next;
	private LuaValue inext;

	private static final String[] LIBR_KEYS = {
		"pcall", // (f, arg1, ...) -> status, result1, ...
		"xpcall", // (f, err) -> result1, ...
		"load", // ( func [,chunkname] ) -> chunk | nil, msg
	};

	@Override
	public LuaValue add(LuaState state, LuaTable env) {
		env.rawset("_G", env);
		env.rawset("_VERSION", valueOf(Lua._VERSION));

		bindV(env, "assert", BaseLib::assert_);
		bindV(env, "dofile", BaseLib::dofile);
		bindV(env, "getfenv", BaseLib::getfenv);
		bindV(env, "getmetatable", BaseLib::getmetatable);
		bindV(env, "loadfile", BaseLib::loadfile);
		bindV(env, "loadstring", BaseLib::loadstring);
		bindV(env, "print", BaseLib::print);
		bindV(env, "select", BaseLib::select);
		bindV(env, "unpack", BaseLib::unpack);
		bindV(env, "type", BaseLib::type);
		bindV(env, "rawequal", BaseLib::rawequal);
		bindV(env, "rawget", BaseLib::rawget);
		bindV(env, "rawset", BaseLib::rawset);
		bindV(env, "setmetatable", BaseLib::setmetatable);
		bindV(env, "tostring", BaseLib::tostring);
		bindV(env, "tonumber", BaseLib::tonumber);
		bindV(env, "pairs", this::pairs);
		bindV(env, "ipairs", this::ipairs);
		bindV(env, "next", BaseLib::next);
		bindV(env, "__inext", BaseLib::inext);
		bind1(env, "collectgarbage", BaseLib::collectGarbage);
		bind2(env, "error", BaseLib::error);
		bindV(env, "setfenv", BaseLib::setfenv);

		LibFunction.bind(env, BaseLibR::new, LIBR_KEYS);

		// remember next, and inext for use in pairs and ipairs
		next = env.rawget("next");
		inext = env.rawget("__inext");

		env.rawset("_VERSION", valueOf("Lua 5.1"));

		return env;
	}

	private static LuaValue collectGarbage(LuaState state, LuaValue mode) throws LuaError {
		String modeStr = mode.optString("collect");
		switch (modeStr) {
			case "collect":
				System.gc();
				return Constants.ZERO;
			case "count":
				Runtime rt = Runtime.getRuntime();
				long used = rt.totalMemory() - rt.freeMemory();
				return valueOf(used / 1024.);
			case "step":
				System.gc();
				return Constants.TRUE;
			default:
				throw ErrorFactory.argError(1, "invalid option");
		}
	}

	private static Varargs setfenv(LuaState state, Varargs args) throws LuaError {
		LuaTable t = args.arg(2).checkTable();
		LuaValue f = getfenvobj(state, args.arg(1));
		if (!f.isThread() && !f.isClosure()) {
			throw new LuaError("'setfenv' cannot change environment of given object");
		}
		f.setfenv(t);
		return f.isThread() ? Constants.NONE : f;
	}

	private static LuaValue error(LuaState state, LuaValue message, LuaValue level) throws LuaError {
		throw new LuaError(message, level.optInteger(1));
	}

	private static LuaValue getfenvobj(LuaState state, LuaValue arg) throws LuaError {
		if (arg.isFunction()) return arg;

		int level = arg.optInteger(1);
		Varargs.argCheck(level >= 0, 1, "level must be non-negative");
		if (level == 0) return state.getCurrentThread();
		LuaValue f = LuaThread.getCallstackFunction(state, level - 1);
		Varargs.argCheck(f != null, 1, "invalid level");
		return f;
	}

	static Varargs assert_(LuaState state, Varargs args) throws LuaError {
		// ( v [,message] ) -> v, message | ERR
		if (!args.first().toBoolean()) {
			throw new LuaError(args.count() > 1 ? args.arg(2).optString("assertion failed!") : "assertion failed!");
		}
		return args;
	}

	static Varargs dofile(LuaState state, Varargs args) throws LuaError {
		// ( filename ) -> result1, ...
		Varargs v = args.isNil(1) ?
			BaseLib.loadStream(state, state.stdin, STDIN_STR) :
			BaseLib.loadFile(state, args.arg(1).checkString());
		if (v.isNil(1)) {
			throw new LuaError(v.arg(2).toString());
		} else {
			// TODO:
			return OperationHelper.noUnwind(state, () -> OperationHelper.invoke(state, v.first(), Constants.NONE));
		}
	}

	static Varargs getfenv(LuaState state, Varargs args) throws LuaError {
		// ( [f] ) -> env
		LuaValue f = getfenvobj(state, args.first());
		LuaValue e = f.isClosure() || f.isThread() ? f.getfenv() : state.getMainThread().getfenv();
		return e != null ? e : Constants.NIL;
	}

	static Varargs getmetatable(LuaState state, Varargs args) throws LuaError {
		// ( object ) -> table
		LuaTable mt = args.checkValue(1).getMetatable(state);
		return mt != null ? mt.rawget(Constants.METATABLE).optValue(mt) : Constants.NIL;
	}

	static Varargs loadfile(LuaState state, Varargs args) throws LuaError {
		// ( [filename] ) -> chunk | nil, msg
		return args.isNil(1) ?
			BaseLib.loadStream(state, state.stdin, STDIN_STR) :
			BaseLib.loadFile(state, args.arg(1).checkString());
	}

	static Varargs loadstring(LuaState state, Varargs args) throws LuaError {
		// ( string [,chunkname] ) -> chunk | nil, msg
		LuaString script = args.arg(1).checkLuaString();
		return BaseLib.loadStream(state, script.toInputStream(), args.arg(2).optLuaString(script));
	}

	static Varargs print(LuaState state, Varargs args) throws LuaError {
		// (...) -> void
		return noUnwind(state, () -> {
			LuaValue tostring = OperationHelper.getTable(state, state.getCurrentThread().getfenv(), valueOf("tostring"));
			for (int i = 1, n = args.count(); i <= n; i++) {
				if (i > 1) state.stdout.write('\t');
				LuaString s = OperationHelper.call(state, tostring, args.arg(i)).strvalue();
				int z = s.indexOf((byte) 0, 0);
				state.stdout.write(s.bytes, s.offset, z >= 0 ? z : s.length);
			}
			state.stdout.println();
			return Constants.NONE;
		});
	}

	static Varargs select(LuaState state, Varargs args) throws LuaError {
		// (f, ...) -> value1, ...
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

	static Varargs unpack(LuaState state, Varargs args) throws LuaError {
		// (list [,i [,j]]) -> result1, ...

		int na = args.count();
		LuaTable t = args.arg(1).checkTable();
		int n = t.length();
		int i = na >= 2 ? args.arg(2).optInteger(1) : 1;
		int j = na >= 3 ? args.arg(3).optInteger(n) : n;
		n = j - i + 1;
		if (n < 0) return Constants.NONE;
		if (n == 1) return t.rawget(i);
		if (n == 2) return varargsOf(t.rawget(i), t.rawget(j));
		LuaValue[] v = new LuaValue[n];
		for (int k = 0; k < n; k++) {
			v[k] = t.rawget(i + k);
		}
		return varargsOf(v);
	}

	static Varargs type(LuaState state, Varargs args) throws LuaError {
		// (v) -> value
		return valueOf(args.checkValue(1).typeName());
	}

	static Varargs rawequal(LuaState state, Varargs args) throws LuaError {
		// (v1, v2) -> boolean
		return valueOf(args.checkValue(1) == args.checkValue(2));
	}

	static Varargs rawget(LuaState state, Varargs args) throws LuaError {
		// (table, index) -> value
		return args.arg(1).checkTable().rawget(args.checkValue(2));
	}

	static Varargs rawset(LuaState state, Varargs args) throws LuaError {
		// (table, index, value) -> table
		LuaTable t = args.arg(1).checkTable();
		LuaValue k = args.checkValue(2);
		LuaValue v = args.checkValue(3);
		if (k.isNil()) throw new LuaError("table index is nil");
		t.rawset(k.checkValidKey(), v);
		return t;
	}

	static Varargs setmetatable(LuaState state, Varargs args) throws LuaError {
		// (table, metatable) -> table
		final LuaValue t = args.first();
		final LuaTable mt0 = t.getMetatable(state);
		if (mt0 != null && !mt0.rawget(Constants.METATABLE).isNil()) {
			throw new LuaError("cannot change a protected metatable");
		}
		final LuaValue mt = args.checkValue(2);
		t.setMetatable(state, mt.isNil() ? null : mt.checkTable());
		return t;
	}

	static Varargs tostring(LuaState state, Varargs args) throws LuaError {
		// (e) -> value
		LuaValue arg = args.checkValue(1);
		LuaValue h = arg.metatag(state, Constants.TOSTRING);
		if (!h.isNil()) {
			// TODO:
			return OperationHelper.noUnwind(state, () -> OperationHelper.call(state, h, arg));
		}
		LuaValue v = arg.toLuaString();
		if (!v.isNil()) {
			return v;
		}
		return valueOf(arg.toString());
	}

	static Varargs tonumber(LuaState state, Varargs args) throws LuaError {
		// (e [,base]) -> value
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

	Varargs pairs(LuaState state, Varargs args) throws LuaError {
		//(t) -> iter-func, t, nil
		return varargsOf(next, args.arg(1).checkTable(), Constants.NIL);
	}

	Varargs ipairs(LuaState state, Varargs args) throws LuaError {
		// (t) -> iter-func, t, 0
		return varargsOf(inext, args.arg(1).checkTable(), Constants.ZERO);
	}

	static Varargs next(LuaState state, Varargs args) throws LuaError {
		// ( table, [index] ) -> next-index, next-value
		return args.arg(1).checkTable().next(args.arg(2));
	}

	static Varargs inext(LuaState state, Varargs args) throws LuaError {
		// ( table, [int-index] ) -> next-index, next-value
		return args.arg(1).checkTable().inext(args.arg(2));
	}

	private static class BaseLibR extends ResumableVarArgFunction<PCallState> {
		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0: // "pcall", // (f, arg1, ...) -> status, result1, ...
					return pcall(state, di, args.checkValue(1), args.subargs(2), null);
				case 1: // "xpcall", // (f, err) -> result1, ...
					return pcall(state, di, args.checkValue(1), Constants.NONE, args.checkValue(2));

				case 2: // "load", // ( func [,chunkname] ) -> chunk | nil, msg
				{
					LuaValue function = args.arg(1).checkFunction();
					LuaString chunkName = args.arg(2).optLuaString(FUNCTION_STR);

					Varargs result = pcall(state, di, new ZeroArgFunction(lState -> {
						try {
							InputStream stream = new StringInputStream(lState, function);
							return LoadState.load(lState, stream, chunkName, lState.getCurrentThread().getfenv());
						} catch (Exception e) {
							throw LuaError.wrapMessage(e);
						}
					}), Constants.NONE, state.getCurrentThread().getErrorFunc());

					if (result.first().toBoolean()) {
						return result.arg(2);
					} else {
						return varargsOf(Constants.NIL, result.arg(2));
					}
				}
				default:
					return Constants.NONE;
			}
		}

		@Override
		protected Varargs resumeThis(LuaState state, PCallState pState, Varargs value) {
			state.getCurrentThread().setErrorFunc(pState.oldErrorFunc);

			if (pState.errored) closeUntil(state, pState.frame);
			return finish(pState, value);
		}

		@Override
		public Varargs resumeErrorThis(LuaState state, PCallState pState, LuaError error) throws UnwindThrowable {
			LuaValue value;
			if (pState.errored) {
				value = valueOf("error in error handling");
			} else {
				// Mark this frame as errored, meaning it will not be resumed.
				DebugHandler.getDebugState(state).getStackUnsafe().flags |= FLAG_ERROR;
				// And mark us as being in the error handler.
				pState.errored = true;
				error.fillTraceback(state);
				value = error.value;
			}

			state.getCurrentThread().setErrorFunc(pState.oldErrorFunc);
			closeUntil(state, pState.frame);
			return finish(pState, value);
		}

		private Varargs finish(PCallState pState, Varargs value) {
			switch (opcode) {
				case 0:
				case 1:
					return pState.errored ? varargsOf(Constants.FALSE, value) : varargsOf(Constants.TRUE, value);
				case 2:
					return pState.errored ? varargsOf(Constants.NIL, value) : value;
				default:
					throw new NonResumableException("Cannot resume " + debugName());
			}
		}
	}

	private static final class PCallState {
		DebugFrame frame;
		LuaValue oldErrorFunc;
		boolean errored = false;
	}

	private static Varargs pcall(LuaState state, DebugFrame di, LuaValue func, Varargs args, LuaValue errFunc) throws UnwindThrowable {
		// Mark this frame as being an error handler
		PCallState pState = new PCallState();
		di.state = pState;
		di.flags |= FLAG_YPCALL;

		// Store this frame in the current state.
		pState.frame = di;

		LuaValue oldErr = pState.oldErrorFunc = state.getCurrentThread().setErrorFunc(errFunc);
		try {
			Varargs result = varargsOf(Constants.TRUE, OperationHelper.invoke(state, func, args));

			state.getCurrentThread().setErrorFunc(oldErr);
			return result;
		} catch (Exception e) {
			// Mark this frame as errored, meaning it will not be resumed.
			DebugHandler.getDebugState(state).getStackUnsafe().flags |= FLAG_ERROR;
			// And mark us as being in the error handler.
			pState.errored = true;

			LuaError le = LuaError.wrap(e);
			le.fillTraceback(state);

			state.getCurrentThread().setErrorFunc(oldErr);
			closeUntil(state, di);
			return varargsOf(Constants.FALSE, le.value);
		}
	}

	private static void closeUntil(LuaState state, DebugFrame top) {
		DebugState ds = DebugHandler.getDebugState(state);
		DebugHandler handler = state.debug;

		DebugFrame current;
		while ((current = ds.getStackUnsafe()) != top) {
			current.cleanup();
			handler.onReturnError(ds);
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
			return loadStream(state, is, valueOf("@" + filename));
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static Varargs loadStream(LuaState state, InputStream is, LuaString chunkname) {
		try {
			if (is == null) {
				return varargsOf(Constants.NIL, valueOf("not found: " + chunkname));
			}
			return LoadState.load(state, is, chunkname, state.getCurrentThread().getfenv());
		} catch (Exception e) {
			return varargsOf(Constants.NIL, LuaError.getMessage(e));
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
		public int read() {
			if (remaining <= 0) {
				LuaValue s;
				try {
					s = OperationHelper.noUnwind(state, () -> OperationHelper.call(state, func));
				} catch (LuaError e) {
					throw new UncheckedLuaError(e);
				}

				if (s.isNil()) {
					return -1;
				}
				LuaString ls;
				try {
					ls = s.strvalue();
				} catch (LuaError e) {
					throw new UncheckedLuaError(e);
				}
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
