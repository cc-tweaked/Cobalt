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
import org.squiddev.cobalt.debug.DebugHelpers;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.LocalVariable;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;
import static org.squiddev.cobalt.function.LibFunction.bindV;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code debug}
 * library.
 *
 * The debug library in luaj tries to emulate the behavior of the corresponding C-based lua library.
 * To do this, it must maintain a stack of calls to {@link LuaClosure} and {@link LibFunction}
 * instances.
 *
 * @see LibFunction
 * @see JsePlatform
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.9">http://www.lua.org/manual/5.1/manual.html#5.9</a>
 */
public class DebugLib implements LuaLibrary {
	private static final LuaString MAIN = valueOf("main");
	private static final LuaString LUA = valueOf("Lua");
	private static final LuaString C = valueOf("C");
	private static final LuaString C_SOURCE = valueOf("[C]");
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
	private static final LuaString ACTIVELINES = valueOf("activelines");
	private static final LuaString NPARAMS = valueOf("nparams");
	private static final LuaString ISVARARG = valueOf("isvararg");

	private LuaValue getinfo;

	@Override
	public LuaTable add(LuaState state, LuaTable env) {
		LuaTable t = new LuaTable();

		bindV(t, "debug", (s, args) -> NONE);
		bindV(t, "getfenv", (s, args) -> getfenv(args));
		bindV(t, "gethook", DebugLib::gethook);
		bindV(t, "getinfo", (s, args) -> getinfo(s, args, getinfo));
		bindV(t, "getlocal", DebugLib::getlocal);
		bindV(t, "getmetatable", DebugLib::getmetatable);
		bindV(t, "getregistry", (s, args) -> getregistry(args));
		bindV(t, "getupvalue", (s, args) -> getupvalue(args));
		bindV(t, "setfenv", (s, args) -> setfenv(args));
		bindV(t, "sethook", DebugLib::sethook);
		bindV(t, "setlocal", DebugLib::setlocal);
		bindV(t, "setmetatable", DebugLib::setmetatable);
		bindV(t, "setupvalue", (s, args) -> setupvalue(args));
		bindV(t, "traceback", DebugLib::traceback);

		// FIXME: This is a terrible bodge. Is there a better way?
		getinfo = t.rawget("getinfo");

		env.rawset("debug", t);
		state.loadedPackages.rawset("debug", t);
		return t;
	}

	private static Varargs gethook(LuaState state, Varargs args) throws LuaError {
		int a = 1;
		LuaThread thread = args.arg(a).isThread() ? args.arg(a++).checkThread() : state.getCurrentThread();
		DebugState ds = thread.getDebugState();

		LuaValue hook;
		if (ds.hookfunc == null) {
			hook = NIL;
		} else if (ds.hookfunc instanceof LuaValue) {
			hook = (LuaValue) ds.hookfunc;
		} else {
			hook = EXTERNAL_HOOK;
		}
		return varargsOf(
			hook,
			valueOf((ds.hookcall ? "c" : "") + (ds.hookrtrn ? "r" : "") + (ds.hookline ? "l" : "")),
			valueOf(ds.hookcount));
	}

	private static Varargs sethook(LuaState state, Varargs args) throws LuaError {
		int a = 1;
		LuaThread thread = args.arg(a).isThread() ? args.arg(a++).checkThread() : state.getCurrentThread();
		int i1 = a++;
		LuaFunction func = args.arg(i1).optFunction(null);
		int i3 = a++;
		String str = args.arg(i3).optString("");
		int i2 = a++;
		int count = args.arg(i2).optInteger(0);
		boolean call = false, line = false, rtrn = false;
		if (func != null) {
			for (int i = 0; i < str.length(); i++) {
				switch (str.charAt(i)) {
					case 'c':
						call = true;
						break;
					case 'l':
						line = true;
						break;
					case 'r':
						rtrn = true;
						break;
				}
			}
		}
		thread.getDebugState().setHook(func, call, line, rtrn, count);
		return NONE;
	}

	private static Varargs getfenv(Varargs args) throws LuaError {
		LuaValue object = args.first();
		LuaValue env = object.getfenv();
		return env != null ? env : NIL;
	}

	private static Varargs setfenv(Varargs args) throws LuaError {
		LuaValue object = args.first();
		LuaTable table = args.arg(2).checkTable();
		object.setfenv(table);
		return object;
	}

	protected static Varargs getinfo(LuaState state, Varargs args, LuaValue level0func) throws LuaError {
		int arg = 1;
		LuaThread thread = args.arg(arg).isThread() ? args.arg(arg++).checkThread() : state.getCurrentThread();
		LuaValue func = args.arg(arg);
		String what = args.arg(arg + 1).optString("nSluf");

		// find the stack info
		DebugState ds = thread.getDebugState();
		DebugFrame di;
		if (func.isNumber()) {
			int level = func.checkInteger();

			// So if we're getting info on the current thread then we fake a debug.getinfo function
			if (thread == state.getCurrentThread()) {
				di = level > 0 ? ds.getFrame(level - 1) : new DebugFrame(level0func.checkFunction());
			} else {
				di = ds.getFrame(level);
			}
		} else {
			di = ds.findDebugInfo(func.checkFunction());
		}
		if (di == null) return NIL;

		// start a table
		LuaTable info = new LuaTable();
		LuaClosure c = di.closure;
		for (int i = 0, j = what.length(); i < j; i++) {
			switch (what.charAt(i)) {
				case 'S': {
					if (c != null) {
						Prototype p = c.getPrototype();
						info.rawset(WHAT, p.linedefined == 0 ? MAIN : LUA);
						info.rawset(SOURCE, p.source);
						info.rawset(SHORT_SRC, p.sourceShort());
						info.rawset(LINEDEFINED, valueOf(p.linedefined));
						info.rawset(LASTLINEDEFINED, valueOf(p.lastlinedefined));
					} else {
						String shortName = di.func == null ? "nil" : di.func.debugName();
						LuaString name = valueOf("[C] " + shortName);
						info.rawset(WHAT, C);
						info.rawset(SOURCE, name);
						info.rawset(SHORT_SRC, C_SOURCE);
						info.rawset(LINEDEFINED, MINUSONE);
						info.rawset(LASTLINEDEFINED, MINUSONE);
					}
					break;
				}
				case 'l': {
					int line = di.currentLine();
					info.rawset(CURRENTLINE, valueOf(line));
					break;
				}
				case 'u': {
					info.rawset(NUPS, valueOf(c != null ? c.getPrototype().nups : 0));
					info.rawset(NPARAMS, valueOf(c != null ? c.getPrototype().numparams : 0));
					info.rawset(ISVARARG, valueOf(c == null || c.getPrototype().is_vararg > 0));
					break;
				}
				case 'n': {
					LuaString[] kind = di.getFuncKind();
					info.rawset(NAME, kind != null ? kind[0] : NIL);
					info.rawset(NAMEWHAT, kind != null ? kind[1] : EMPTYSTRING);
					break;
				}
				case 'f': {
					info.rawset(FUNC, di.func == null ? NIL : di.func);
					break;
				}
				case 'L': {
					LuaTable lines = new LuaTable();
					info.rawset(ACTIVELINES, lines);
//					if ( di.luainfo != null ) {
//						int line = di.luainfo.currentline();
//						if ( line >= 0 )
//							lines.set(1, IntValue.valueOf(line));
//					}
					break;
				}
			}
		}
		return info;
	}

	private static Varargs getlocal(LuaState state, Varargs args) throws LuaError {
		int arg = 1;
		LuaThread thread = args.arg(arg).isThread() ? args.arg(arg++).checkThread() : state.getCurrentThread();

		int local = args.arg(arg + 1).checkInteger();
		if (args.arg(arg).isFunction()) {
			LuaFunction function = args.arg(arg).checkFunction();
			if (!function.isClosure()) return NIL;

			Prototype proto = function.checkClosure().getPrototype();
			LocalVariable[] variables = proto.locvars;
			return variables != null && local > 0 && local <= variables.length && local <= proto.numparams
				? variables[local - 1].name : NIL;
		} else {
			int level = args.arg(arg).checkInteger();
			if (thread == state.getCurrentThread()) level--;

			DebugState ds = thread.getDebugState();
			DebugFrame di = ds.getFrame(level);
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

		DebugState ds = thread.getDebugState();
		if (thread == state.getCurrentThread()) level--;
		DebugFrame di = ds.getFrame(level);
		if (di == null) throw new LuaError("bad argument #" + arg + " (level out of range)");

		LuaString name = di.getLocalName(local);
		if (name == null || di.stack == null) return NIL;

		di.stack[local - 1] = value;
		return name;
	}

	private static LuaValue getmetatable(LuaState state, Varargs args) {
		LuaValue object = args.arg(1);
		LuaValue mt = object.getMetatable(state);
		return mt != null ? mt : NIL;
	}

	private static Varargs setmetatable(LuaState state, Varargs args) {
		LuaValue object = args.arg(1);
		try {
			LuaTable mt = args.arg(2).optTable(null);
			switch (object.type()) {
				case TNIL:
					state.nilMetatable = mt;
					break;
				case TNUMBER:
					state.numberMetatable = mt;
					break;
				case TBOOLEAN:
					state.booleanMetatable = mt;
					break;
				case TSTRING:
					state.stringMetatable = mt;
					break;
				case TFUNCTION:
					state.functionMetatable = mt;
					break;
				case TTHREAD:
					state.threadMetatable = mt;
					break;
				default:
					object.setMetatable(state, mt);
			}
			return TRUE;
		} catch (LuaError e) {
			return varargsOf(FALSE, valueOf(e.toString()));
		}
	}

	private static Varargs getregistry(Varargs args) {
		return new LuaTable();
	}

	private static LuaString findupvalue(LuaClosure c, int up) {
		Prototype p = c.getPrototype();
		if (up > 0 && p.upvalues != null && up <= p.upvalues.length) {
			return p.upvalues[up - 1];
		} else {
			return null;
		}
	}

	private static Varargs getupvalue(Varargs args) throws LuaError {
		LuaValue func = args.arg(1).checkFunction();
		int up = args.arg(2).checkInteger();
		if (func instanceof LuaClosure) {
			LuaClosure c = (LuaClosure) func;
			LuaString name = findupvalue(c, up);
			if (name != null) {
				return varargsOf(name, c.getUpvalue(up - 1));
			}
		}
		return NIL;
	}

	private static LuaValue setupvalue(Varargs args) throws LuaError {
		LuaValue func = args.arg(1).checkFunction();
		int up = args.arg(2).checkInteger();
		LuaValue value = args.arg(3);
		if (func instanceof LuaClosure) {
			LuaClosure c = (LuaClosure) func;
			LuaString name = findupvalue(c, up);
			if (name != null) {
				c.setUpvalue(up - 1, value);
				return name;
			}
		}
		return NIL;
	}

	private static LuaValue traceback(LuaState state, Varargs args) throws LuaError {
		int a = 1;
		LuaThread thread = args.arg(a).isThread() ? args.arg(a++).checkThread() : state.getCurrentThread();
		LuaValue messageValue = args.arg(a++);
		if (messageValue != NIL && !messageValue.isString()) return messageValue;
		String message = messageValue.optString(null);
		int level = args.arg(a).optInteger(1) - 1;
		// if (thread == state.getCurrentThread()) level--;
		String tb = DebugHelpers.traceback(thread, level);
		return valueOf(message != null ? message + "\n" + tb : tb);
	}
}
