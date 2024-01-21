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


import cc.tweaked.cobalt.internal.LegacyEnv;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.*;
import org.squiddev.cobalt.function.*;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code debug}
 * library.
 * <p>
 * The debug library in luaj tries to emulate the behavior of the corresponding C-based lua library.
 * To do this, it must maintain a stack of calls to {@link LuaClosure} and {@link LibFunction}
 * instances.
 *
 * @see LibFunction
 * @see CoreLibraries
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.9">http://www.lua.org/manual/5.1/manual.html#5.9</a>
 */
public final class DebugLib {
	private static final LuaString MAIN = valueOf("main");
	private static final LuaString LUA = valueOf("Lua");
	private static final LuaString C = valueOf("C");
	private static final LuaString C_SHORT_SOURCE = valueOf("[C]");
	private static final LuaString C_SOURCE = valueOf("=[C]");
	public static final LuaString QMARK = valueOf("?");
	private static final LuaString EXTERNAL_HOOK = valueOf("external hook");

	private static final LuaString FUNC = valueOf("func");
	private static final LuaString NUPS = valueOf("nups");
	private static final LuaString NAME = valueOf("name");
	private static final LuaString NAMEWHAT = valueOf("namewhat");
	private static final LuaString WHAT = valueOf("what");
	private static final LuaString SOURCE = valueOf("source");
	private static final LuaString SHORT_SRC = valueOf("short_src");
	private static final LuaString LINEDEFINED = valueOf("linedefined");
	private static final LuaString LASTLINEDEFINED = valueOf("lastlinedefined");
	private static final LuaString CURRENTLINE = valueOf("currentline");
	private static final LuaString CURRENTCOLUMN = valueOf("currentcolumn");
	private static final LuaString ACTIVELINES = valueOf("activelines");
	private static final LuaString NPARAMS = valueOf("nparams");
	private static final LuaString ISVARARG = valueOf("isvararg");
	private static final LuaString ISTAILCALL = valueOf("istailcall");

	private DebugLib() {
	}

	public static void add(LuaState state, LuaTable env) throws LuaError {
		LibFunction.setGlobalLibrary(state, env, "debug", RegisteredFunction.bind(new RegisteredFunction[]{
			RegisteredFunction.ofV("debug", DebugLib::debug),
			RegisteredFunction.ofV("getfenv", DebugLib::getfenv),
			RegisteredFunction.ofV("gethook", DebugLib::gethook),
			RegisteredFunction.ofV("getinfo", DebugLib::getinfo),
			RegisteredFunction.ofV("getlocal", DebugLib::getlocal),
			RegisteredFunction.ofV("getmetatable", DebugLib::getmetatable),
			RegisteredFunction.ofV("getregistry", DebugLib::getregistry),
			RegisteredFunction.ofV("getupvalue", DebugLib::getupvalue),
			RegisteredFunction.ofV("setfenv", DebugLib::setfenv),
			RegisteredFunction.ofV("sethook", DebugLib::sethook),
			RegisteredFunction.ofV("setlocal", DebugLib::setlocal),
			RegisteredFunction.ofV("setmetatable", DebugLib::setmetatable),
			RegisteredFunction.ofV("setupvalue", DebugLib::varargs),
			RegisteredFunction.ofV("traceback", DebugLib::traceback),
			RegisteredFunction.ofV("upvalueid", DebugLib::upvalueId),
			RegisteredFunction.ofV("upvaluejoin", DebugLib::upvalueJoin),
		}));
	}

	// ------------------- library function implementations -----------------

	private static Varargs debug(LuaState state, Varargs args) {
		return NONE;
	}

	private static Varargs gethook(LuaState state, Varargs args) throws LuaError {
		int a = 1;
		LuaThread thread = args.arg(a).isThread() ? args.arg(a++).checkThread() : state.getCurrentThread();
		DebugState ds = thread.getDebugState();

		LuaValue hook;
		if (ds.getHook() == null) {
			hook = NIL;
		} else if (ds.getHook() instanceof FunctionDebugHook) {
			hook = ((FunctionDebugHook) ds.getHook()).function();
		} else {
			hook = EXTERNAL_HOOK;
		}
		return varargsOf(
			hook,
			valueOf((ds.hasCallHook() ? "c" : "") + (ds.hasReturnHook() ? "r" : "") + (ds.hasLineHook() ? "l" : "")),
			valueOf(ds.hookCount));
	}

	private static Varargs sethook(LuaState state, Varargs args) throws LuaError {
		int a = 1;
		LuaThread thread = args.arg(a).isThread() ? args.arg(a++).checkThread() : state.getCurrentThread();
		int i1 = a++;
		LuaFunction func = args.arg(i1).optFunction(null);
		int i3 = a++;
		LuaString str = args.arg(i3).optLuaString(EMPTYSTRING);
		int i2 = a++;
		int count = args.arg(i2).optInteger(0);
		boolean call = false, line = false, rtrn = false;
		if (func != null) {
			for (int i = 0; i < str.length(); i++) {
				switch (str.charAt(i)) {
					case 'c' -> call = true;
					case 'l' -> line = true;
					case 'r' -> rtrn = true;
				}
			}
		} else {
			count = 0;
		}
		thread.getDebugState().setHook(func == null ? null : new FunctionDebugHook(func), call, line, rtrn, count);

		return NONE;
	}

	private static Varargs getfenv(LuaState state, Varargs args) {
		LuaValue object = args.first();
		LuaValue env = LegacyEnv.getEnv(object);
		return env != null ? env : NIL;
	}

	private static Varargs setfenv(LuaState state, Varargs args) throws LuaError {
		LuaValue object = args.first();
		LuaTable env = args.arg(2).checkTable();
		if (!LegacyEnv.setEnv(object, env)) throw new LuaError("'setfenv' cannot change environment of given object");
		return object;
	}

	private static Varargs getinfo(LuaState state, Varargs args) throws LuaError {
		int arg = 1;
		LuaThread thread = args.arg(arg).isThread() ? args.arg(arg++).checkThread() : state.getCurrentThread();
		LuaValue funcArg = args.arg(arg);
		String what = args.arg(arg + 1).optString("flnStu");

		// Find the stack info
		DebugState ds = thread.getDebugState();
		DebugFrame callInfo;
		LuaFunction function;
		if (funcArg.isNumber()) {
			callInfo = ds.getFrame(funcArg.checkInteger());
			if (callInfo == null) return NIL;
			function = callInfo.func;
		} else {
			callInfo = null;
			function = funcArg.checkFunction();
		}

		// start a table
		LuaTable info = new LuaTable();
		LuaClosure closure = function instanceof LuaClosure c ? c : null;
		for (int i = 0, j = what.length(); i < j; i++) {
			switch (what.charAt(i)) {
				case 'S' -> {
					if (closure != null) {
						Prototype p = closure.getPrototype();
						info.rawset(WHAT, p.lineDefined == 0 ? MAIN : LUA);
						info.rawset(SOURCE, p.source);
						info.rawset(SHORT_SRC, p.shortSource());
						info.rawset(LINEDEFINED, valueOf(p.lineDefined));
						info.rawset(LASTLINEDEFINED, valueOf(p.lastLineDefined));
					} else {
						info.rawset(WHAT, C);
						info.rawset(SOURCE, C_SOURCE);
						info.rawset(SHORT_SRC, C_SHORT_SOURCE);
						info.rawset(LINEDEFINED, MINUSONE);
						info.rawset(LASTLINEDEFINED, MINUSONE);
					}
				}
				case 'l' -> {
					if (callInfo == null || closure == null) {
						info.rawset(CURRENTLINE, valueOf(-1));
						continue;
					}

					Prototype p = closure.getPrototype();
					int line = p.lineAt(callInfo.pc);
					int column = p.columnAt(callInfo.pc);
					info.rawset(CURRENTLINE, valueOf(line));
					if (column > 0) info.rawset(CURRENTCOLUMN, valueOf(column));
				}
				case 'u' -> {
					info.rawset(NUPS, valueOf(closure != null ? closure.getPrototype().upvalues() : 0));
					info.rawset(NPARAMS, valueOf(closure != null ? closure.getPrototype().parameters : 0));
					info.rawset(ISVARARG, valueOf(closure == null || closure.getPrototype().isVarArg));
				}
				case 'n' -> {
					ObjectName kind = callInfo != null ? callInfo.getFuncKind() : null;
					info.rawset(NAME, kind != null ? kind.name() : NIL);
					info.rawset(NAMEWHAT, kind != null ? kind.what() : EMPTYSTRING);
				}
				case 'f' -> {
					info.rawset(FUNC, function == null ? NIL : function);
				}
				case 'L' -> {
					if (closure != null) {
						LuaTable lines = new LuaTable();
						info.rawset(ACTIVELINES, lines);
						int[] lineInfo = closure.getPrototype().lineInfo;
						if (lineInfo != null) {
							for (int line : lineInfo) lines.rawset(line, TRUE);
						}
					}
				}
				case 't' -> {
					info.rawset(ISTAILCALL, valueOf(callInfo != null && (callInfo.flags & DebugFrame.FLAG_TAIL) != 0));
				}
				default -> throw ErrorFactory.argError(arg + 1, "invalid option");
			}
		}
		return info;
	}

	private static Varargs getlocal(LuaState state, Varargs args) throws LuaError {
		int arg = 1;
		LuaThread thread = args.arg(arg).isThread() ? args.arg(arg++).checkThread() : state.getCurrentThread();

		int local = args.arg(arg + 1).checkInteger();
		if (args.arg(arg) instanceof LuaFunction function) {
			if (!(function instanceof LuaClosure closure)) return NIL;

			Prototype proto = closure.getPrototype();
			LocalVariable[] variables = proto.locals;
			return variables != null && local > 0 && local <= variables.length && local <= proto.parameters
				? variables[local - 1].name : NIL;
		} else {
			int level = args.arg(arg).checkInteger();
			DebugFrame di = thread.getDebugState().getFrame(level);
			if (di == null) throw new LuaError("bad argument #" + arg + " (level out of range)");

			LuaString name = di.getLocalName(local);
			if (name == null || di.stack == null) return NIL;
			LuaValue value = di.stack[local - 1];
			return varargsOf(name, value);
		}
	}

	private static Varargs setlocal(LuaState state, Varargs args) throws LuaError {
		int arg = 1;
		LuaThread thread = args.arg(arg).isThread() ? args.arg(arg++).checkThread() : state.getCurrentThread();
		int level = args.arg(arg).checkInteger();
		int local = args.arg(arg + 1).checkInteger();
		LuaValue value = args.arg(arg + 2);

		DebugFrame di = thread.getDebugState().getFrame(level);
		if (di == null) throw new LuaError("bad argument #" + arg + " (level out of range)");

		LuaString name = di.getLocalName(local);
		if (name == null || di.stack == null) return NIL;

		di.stack[local - 1] = value;
		return name;
	}

	private static Varargs getmetatable(LuaState state, Varargs args) {
		LuaValue object = args.arg(1);
		LuaValue mt = object.getMetatable(state);
		return mt != null ? mt : NIL;
	}

	private static Varargs setmetatable(LuaState state, Varargs args) {
		LuaValue object = args.arg(1);
		try {
			LuaTable mt = args.arg(2).optTable(null);
			switch (object.type()) {
				case TNIL -> state.nilMetatable = mt;
				case TNUMBER -> state.numberMetatable = mt;
				case TBOOLEAN -> state.booleanMetatable = mt;
				case TSTRING -> state.stringMetatable = mt;
				case TFUNCTION -> state.functionMetatable = mt;
				case TTHREAD -> state.threadMetatable = mt;
				default -> object.setMetatable(state, mt);
			}
			return TRUE;
		} catch (LuaError e) {
			return varargsOf(FALSE, valueOf(e.toString()));
		}
	}

	private static Varargs getregistry(LuaState state, Varargs args) {
		return state.registry().get();
	}

	private static LuaString findupvalue(LuaClosure c, int up) {
		Prototype p = c.getPrototype();
		return p.getUpvalueName(up - 1);
	}

	private static Varargs getupvalue(LuaState state, Varargs args) throws LuaError {
		LuaValue func = args.arg(1).checkFunction();
		int up = args.arg(2).checkInteger();
		if (func instanceof LuaClosure c) {
			LuaString name = findupvalue(c, up);
			if (name != null) {
				return varargsOf(name, c.getUpvalue(up - 1).getValue());
			}
		}
		return NIL;
	}

	private static Varargs varargs(LuaState state, Varargs args) throws LuaError {
		LuaValue func = args.arg(1).checkFunction();
		int up = args.arg(2).checkInteger();
		LuaValue value = args.arg(3);
		if (func instanceof LuaClosure c) {
			LuaString name = findupvalue(c, up);
			if (name != null) {
				c.getUpvalue(up - 1).setValue(value);
				return name;
			}
		}
		return NIL;
	}

	private static Varargs traceback(LuaState state, Varargs args) throws LuaError {
		int a = 1;
		LuaThread thread = args.arg(a).isThread() ? args.arg(a++).checkThread() : state.getCurrentThread();
		LuaValue messageValue = args.arg(a++);
		if (messageValue != NIL && !messageValue.isString()) return messageValue;
		LuaString message = messageValue.optLuaString(null);

		int level = args.arg(a).optInteger(thread == state.getCurrentThread() ? 1 : 0);

		Buffer sb = new Buffer();
		if (message != null) sb.append(message).append('\n');
		return valueOf(DebugHelpers.traceback(sb, thread, level).toString());
	}

	private static LuaClosure getClosureForUpvalue(Varargs args, int offset, int upvalue) throws LuaError {
		LuaFunction function = args.arg(offset).checkFunction();
		if (function instanceof LuaClosure closure) {
			if (upvalue >= 0 && upvalue < closure.getPrototype().upvalues()) return closure;
		}

		throw ErrorFactory.argError(offset, "invalid upvalue index");
	}

	private static Varargs upvalueId(LuaState state, Varargs args) throws LuaError {
		int upvalue = args.arg(2).checkInteger() - 1;
		LuaClosure closure = getClosureForUpvalue(args, 1, upvalue);
		return new LuaUserdata(closure.getUpvalue(upvalue));
	}

	private static Varargs upvalueJoin(LuaState state, Varargs args) throws LuaError {
		int upvalue1 = args.arg(2).checkInteger() - 1;
		LuaClosure closure1 = getClosureForUpvalue(args, 1, upvalue1);

		int upvalue2 = args.arg(4).checkInteger() - 1;
		LuaClosure closure2 = getClosureForUpvalue(args, 3, upvalue2);

		closure1.setUpvalue(upvalue1, closure2.getUpvalue(upvalue2));
		return NONE;
	}
}
