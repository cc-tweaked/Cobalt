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
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.InputReader;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.function.ResumableVarArgFunction;
import org.squiddev.cobalt.lib.system.ResourceLoader;
import org.squiddev.cobalt.unwind.SuspendedTask;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * The basic global libraries in the Lua runtime.
 *
 * @see ResourceLoader
 * @see LibFunction
 * @see CoreLibraries
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.1">http://www.lua.org/manual/5.1/manual.html#5.1</a>
 */
public class BaseLib {
	private static final LuaString FUNCTION_STR = valueOf("function");
	private static final LuaString LOAD_MODE = valueOf("bt");
	private static final LuaString ASSERTION_FAILED = valueOf("assertion failed!");

	private LuaValue next;
	private LuaValue inext;

	public void add(LuaTable env) {
		env.rawset("_G", env);
		env.rawset("_VERSION", valueOf("Lua 5.1"));
		RegisteredFunction.bind(env, new RegisteredFunction[]{
			RegisteredFunction.of("error", BaseLib::error),
			RegisteredFunction.ofV("setfenv", BaseLib::setfenv),
			RegisteredFunction.ofV("assert", BaseLib::assert_),
			RegisteredFunction.ofV("getfenv", BaseLib::getfenv),
			RegisteredFunction.ofV("getmetatable", BaseLib::getmetatable),
			RegisteredFunction.ofV("loadstring", BaseLib::loadstring),
			RegisteredFunction.ofV("select", BaseLib::select),
			RegisteredFunction.ofV("type", BaseLib::type),
			RegisteredFunction.ofV("rawequal", BaseLib::rawequal),
			RegisteredFunction.ofV("rawget", BaseLib::rawget),
			RegisteredFunction.ofV("rawset", BaseLib::rawset),
			RegisteredFunction.ofV("setmetatable", BaseLib::setmetatable),
			RegisteredFunction.ofV("tostring", BaseLib::tostring),
			RegisteredFunction.ofV("tonumber", BaseLib::tonumber),
			RegisteredFunction.ofV("pairs", this::pairs),
			RegisteredFunction.ofV("ipairs", this::ipairs),
			RegisteredFunction.ofV("rawlen", BaseLib::rawlen),
			RegisteredFunction.ofV("next", BaseLib::next),
			RegisteredFunction.ofFactory("pcall", PCall::new),
			RegisteredFunction.ofFactory("xpcall", XpCall::new),
			RegisteredFunction.ofFactory("load", Load::new),
		});

		// remember next, and inext for use in pairs and ipairs
		next = env.rawget("next");
		inext = RegisteredFunction.ofS("inext", BaseLib::inext).create();
	}

	private static LuaValue error(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		// error( message [,level] ) -> ERR
		throw new LuaError(arg1.isNil() ? Constants.NIL : arg1, arg2.optInteger(1));
	}

	private static Varargs setfenv(LuaState state, Varargs args) throws LuaError {
		// setfenv(f, table) -> void
		LuaTable t = args.arg(2).checkTable();
		LuaValue f = getfenvobj(state, args.arg(1), false);
		if (f.isThread()) {
			f.setfenv(t);
			return Constants.NONE;
		}

		if (f instanceof LibFunction || !f.setfenv(t)) {
			throw new LuaError("'setfenv' cannot change environment of given object");
		}

		return f;
	}

	private static LuaValue getfenvobj(LuaState state, LuaValue arg, boolean optional) throws LuaError {
		if (arg.isFunction()) return arg;

		int level = optional ? arg.optInteger(1) : arg.checkInteger();
		Varargs.argCheck(level >= 0, 1, "level must be non-negative");
		if (level == 0) return state.getCurrentThread();
		LuaValue f = LuaThread.getCallstackFunction(state, level - 1);
		Varargs.argCheck(f != null, 1, "invalid level");
		return f;
	}

	private static Varargs assert_(LuaState state, Varargs args) throws LuaError {
		// assert( v [,message] ) -> v, message | ERR
		if (args.first().toBoolean()) return args;
		args.checkValue(1);
		throw new LuaError(args.count() > 1 ? args.arg(2) : ASSERTION_FAILED);
	}


	private static Varargs getfenv(LuaState state, Varargs args) throws LuaError {
		// getfenv( [f] ) -> env
		LuaValue f = getfenvobj(state, args.first(), true);
		if (f instanceof LibFunction) {
			return state.getCurrentThread().getfenv();
		} else {
			return f.getfenv();
		}
	}

	private static Varargs getmetatable(LuaState state, Varargs args) throws LuaError {
		// getmetatable( object ) -> table
		LuaTable mt = args.checkValue(1).getMetatable(state);
		return mt != null ? mt.rawget(Constants.METATABLE).optValue(mt) : Constants.NIL;
	}

	private static Varargs loadstring(LuaState state, Varargs args) throws LuaError {
		// loadstring( string [,chunkname] ) -> chunk | nil, msg
		LuaString script = args.arg(1).checkLuaString();
		return BaseLib.loadStream(state, script.toInputStream(), args.arg(2).optLuaString(script));
	}

	private static Varargs select(LuaState state, Varargs args) throws LuaError {
		// select(f, ...) -> value1, ...
		int n = args.count() - 1;
		if (args.first().equals(valueOf("#"))) return valueOf(n);
		int i = args.arg(1).checkInteger();
		if (i == 0 || i < -n) throw ErrorFactory.argError(1, "index out of range");
		return args.subargs(i < 0 ? n + i + 2 : i + 1);
	}

	private static Varargs type(LuaState state, Varargs args) throws LuaError {
		// type(v) -> value
		return args.checkValue(1).luaTypeName();
	}

	private static Varargs rawequal(LuaState state, Varargs args) throws LuaError {
		// rawequal(v1, v2) -> boolean
		return valueOf(args.checkValue(1).equals(args.checkValue(2)));
	}

	private static Varargs rawget(LuaState state, Varargs args) throws LuaError {
		// rawget(table, index) -> value
		return args.arg(1).checkTable().rawget(args.checkValue(2));
	}

	private static Varargs rawset(LuaState state, Varargs args) throws LuaError {
		// rawset(table, index, value) -> table
		LuaTable t = args.arg(1).checkTable();
		LuaValue k = args.checkValue(2);
		LuaValue v = args.checkValue(3);
		if (k.isNil()) throw new LuaError("table index is nil");
		t.rawset(k.checkValidKey(), v);
		return t;
	}

	private static Varargs setmetatable(LuaState state, Varargs args) throws LuaError {
		// setmetatable(table, metatable) -> table
		final LuaValue t = args.first();
		final LuaTable mt0 = t.getMetatable(state);
		if (mt0 != null && !mt0.rawget(Constants.METATABLE).isNil()) {
			throw new LuaError("cannot change a protected metatable");
		}
		final LuaValue mt = args.checkValue(2);
		t.setMetatable(state, mt.isNil() ? null : mt.checkTable());
		return t;
	}

	private static Varargs tostring(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
		// tostring(e) -> value
		return OperationHelper.toString(state, args.checkValue(1));

	}

	private static Varargs tonumber(LuaState state, Varargs args) throws LuaError {
		// tonumber"(e [,base]) -> value
		LuaValue arg1 = args.checkValue(1);
		final int base = args.arg(2).optInteger(10);
		if (base == 10) {  /* standard conversion */
			return arg1.toNumber();
		} else {
			if (base < 2 || base > 36) {
				throw ErrorFactory.argError(2, "base out of range");
			}
			return arg1.checkLuaString().toNumber(base);
		}
	}

	private Varargs pairs(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
		// pairs(t) -> iter-func, t, nil
		LuaValue value = args.checkValue(1);
		LuaValue pairs = value.metatag(state, Constants.PAIRS);
		if (pairs.isNil()) {
			return varargsOf(next, value, Constants.NIL);
		} else {
			return OperationHelper.invoke(state, pairs, value);
		}
	}

	private Varargs ipairs(LuaState state, Varargs args) throws LuaError {
		// ipairst) -> iter-func, t, 0
		return varargsOf(inext, args.checkValue(1), Constants.ZERO);
	}

	private static Varargs rawlen(LuaState state, Varargs args) throws LuaError {
		// rawlen( table | string ) -> int
		LuaValue v = args.arg(1);
		return switch (v.type()) {
			case Constants.TTABLE -> ValueFactory.valueOf(v.checkTable().length());
			case Constants.TSTRING -> ValueFactory.valueOf(v.checkLuaString().length());
			default -> throw ErrorFactory.argError(1, "table or string expected");
		};
	}

	private static Varargs next(LuaState state, Varargs args) throws LuaError {
		// next( table, [index] ) -> next-index, next-value
		return args.arg(1).checkTable().next(args.arg(2));
	}

	private static Varargs inext(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
		// inext( table, [int-index] ) -> next-index, next-value
		LuaValue table = args.arg(1);
		int key = args.arg(2).checkInteger() + 1;

		if (table instanceof LuaTable tbl && tbl.getMetatable(state) == null) {
			// Fast path for simple tables.
			LuaValue v = tbl.rawget(key);
			return v.isNil() ? NIL : varargsOf(valueOf(key), v);
		}

		return SuspendedTask.run(di, () -> {
			LuaValue v = OperationHelper.getTable(state, table, key);
			return v.isNil() ? NIL : varargsOf(valueOf(key), v);
		});
	}

	// pcall(f, arg1, ...) -> status, result1, ...
	private static class PCall extends ResumableVarArgFunction<ProtectedCall> {
		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaValue func = args.checkValue(1);

			ProtectedCall call = new ProtectedCall(di, null);
			di.state = call;
			return call.apply(state, func, args.subargs(2)).asBoolAndResult();
		}

		@Override
		protected Varargs resumeThis(LuaState state, ProtectedCall call, Varargs value) throws UnwindThrowable {
			return call.resume(state, value).asBoolAndResult();
		}

		@Override
		protected Varargs resumeErrorThis(LuaState state, ProtectedCall call, LuaError error) throws UnwindThrowable {
			return call.resumeError(state, error).asBoolAndResult();
		}
	}

	// xpcall(f, err) -> result1, ...
	private static class XpCall extends ResumableVarArgFunction<ProtectedCall> {
		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaValue func = args.checkValue(1);
			LuaValue errFunc = args.checkValue(2);

			ProtectedCall call = new ProtectedCall(di, errFunc);
			di.state = call;
			return call.apply(state, func, Constants.NONE).asBoolAndResult();
		}

		@Override
		protected Varargs resumeThis(LuaState state, ProtectedCall call, Varargs value) throws UnwindThrowable {
			return call.resume(state, value).asBoolAndResult();
		}

		@Override
		protected Varargs resumeErrorThis(LuaState state, ProtectedCall call, LuaError error) throws UnwindThrowable {
			return call.resumeError(state, error).asBoolAndResult();
		}
	}

	// load( func|str [,chunkname[, mode[, env]]] ) -> chunk | nil, msg
	static class Load extends ResumableVarArgFunction<ProtectedCall> {
		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			LuaValue scriptGen = args.arg(1);
			LuaString chunkName = args.arg(2).optLuaString(null);
			LuaString mode = args.arg(3).optLuaString(LOAD_MODE);
			LuaTable funcEnv = args.arg(4).optTable(state.getCurrentThread().getfenv());

			// If we're a string, load as normal
			if (scriptGen.isString()) {
				LuaString contents = scriptGen.checkLuaString();
				return BaseLib.loadStream(state, contents.toInputStream(), chunkName == null ? contents : chunkName, mode, funcEnv);
			}

			LuaFunction function = scriptGen.checkFunction();
			ProtectedCall call = new ProtectedCall(di, state.getCurrentThread().getErrorFunc());
			di.state = call;
			return call.apply(state, SuspendedTask.toFunction(() -> {
				try {
					InputReader stream = new FunctionInputReader(state, function);
					return state.compiler.load(LuaC.compile(stream, chunkName == null ? FUNCTION_STR : chunkName, mode), funcEnv);
				} catch (CompileException e) {
					return varargsOf(Constants.NIL, valueOf(e.getMessage()));
				} catch (UncheckedLuaError e) {
					throw e.getCause();
				}
			})).asResultOrFailure();
		}

		@Override
		protected Varargs resumeThis(LuaState state, ProtectedCall call, Varargs value) throws UnwindThrowable {
			return call.resume(state, value).asResultOrFailure();
		}

		@Override
		public Varargs resumeErrorThis(LuaState state, ProtectedCall call, LuaError error) throws UnwindThrowable {
			return call.resumeError(state, error).asResultOrFailure();
		}
	}

	public static Varargs loadStream(LuaState state, InputStream is, LuaString chunkName, LuaString mode, LuaTable env) {
		try {
			return LoadState.load(state, is, chunkName, mode, env);
		} catch (CompileException e) {
			return varargsOf(Constants.NIL, valueOf(e.getMessage()));
		}
	}

	public static Varargs loadStream(LuaState state, InputStream is, LuaString chunkName) {
		return loadStream(state, is, chunkName, null, state.getCurrentThread().getfenv());
	}

	private static class FunctionInputReader implements InputReader {
		private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

		private final LuaState state;
		private final LuaValue func;
		private ByteBuffer bytes = EMPTY;

		FunctionInputReader(LuaState state, LuaValue func) {
			this.state = state;
			this.func = func;
		}

		@Override
		public int read() throws UnwindThrowable {
			if (!bytes.hasRemaining()) {
				LuaValue value;
				try {
					value = OperationHelper.call(state, func);
				} catch (LuaError e) {
					throw new UncheckedLuaError(e);
				}

				if (!fillBuffer(value)) return -1;
			}

			return Byte.toUnsignedInt(bytes.get());
		}

		@Override
		public int resume(Varargs varargs) throws CompileException, UnwindThrowable {
			if (!fillBuffer(varargs.first())) return -1;
			return read();
		}

		private boolean fillBuffer(LuaValue value) {
			if (value.isNil()) return false;
			if (!value.isString()) throw new UncheckedLuaError(new LuaError("reader function must return a string"));

			LuaString ls = OperationHelper.toStringDirect(value);
			bytes = ls.toBuffer();
			return bytes.hasRemaining();
		}
	}
}
