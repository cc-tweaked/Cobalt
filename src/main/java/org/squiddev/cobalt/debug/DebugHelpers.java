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
package org.squiddev.cobalt.debug;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.lib.DebugLib;

import java.util.Objects;

import static org.squiddev.cobalt.Lua.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_ANY_HOOK;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_TAIL;

/**
 * Helper methods for the debug library
 */
public final class DebugHelpers {
	private static final LuaString GLOBAL = valueOf("global");
	private static final LuaString LOCAL = valueOf("local");
	private static final LuaString METHOD = valueOf("method");
	private static final LuaString UPVALUE = valueOf("upvalue");
	private static final LuaString FIELD = valueOf("field");
	private static final LuaString QUESTION = valueOf("?");
	private static final LuaString HOOK = valueOf("hook");
	private static final LuaString METAMETHOD = valueOf("metamethod");

	private static final LuaString FUNCTION = valueOf("function");
	private static final LuaString C = valueOf("[C]");

	/**
	 * Size of the first part of the stack
	 */
	private static final int LEVELS1 = 10;

	/**
	 * Size of the second part of the stack
	 */
	private static final int LEVELS2 = 11;

	private DebugHelpers() {
	}

	/**
	 * Get a traceback for a particular thread.
	 *
	 * @param thread LuaThread to provide stack trace for
	 * @param level  0-based level to start reporting on
	 * @return String containing the stack trace.
	 */
	public static String traceback(LuaThread thread, int level) {
		return traceback(new Buffer(), thread, level).toString();
	}

	/**
	 * Get a traceback for a particular thread.
	 *
	 * @param sb     The builder to append to
	 * @param thread LuaThread to provide stack trace for
	 * @param level  0-based level to start reporting on
	 */
	public static Buffer traceback(Buffer sb, LuaThread thread, int level) {
		sb.append("stack traceback:");

		DebugState state = thread.getDebugState();
		int n1 = state.top - level > LEVELS1 + LEVELS2 ? LEVELS1 : -1;
		for (DebugFrame di; (di = state.getFrame(level++)) != null; ) {
			if (n1-- == 0) {
				sb.append("\n\t...");
				level = state.top - LEVELS2 + 1;
				continue;
			}

			sb.append("\n\t");
			sb.append(di.closure == null ? C : di.closure.getPrototype().shortSource());
			sb.append(':');
			if (di.currentLine() > 0) sb.append(Integer.toString(di.currentLine())).append(":");
			sb.append(" in ");

			ObjectName kind = di.getFuncKind();

			if (kind != null) {
				// Strictly speaking we should search the global table for this term - see Lua 5.3's pushglobalfuncname/
				// pushfuncname. However, I'm somewhat reluctant to do that, so we just check it's a global.
				sb.append(kind.what() == GLOBAL ? FUNCTION : kind.what()).append(" '").append(kind.name()).append('\'');
			} else if (di.func instanceof LuaClosure closure && closure.getPrototype().lineDefined == 0) {
				sb.append("main chunk");
			} else if (di.func instanceof LuaClosure) {
				sb.append("function <").append(di.func.debugName()).append(">");
			} else {
				sb.append('?');
			}

			if ((di.flags & FLAG_TAIL) != 0) sb.append("\n\t(...tail calls...)");
		}

		return sb;
	}

	/**
	 * Get file and line for the nearest calling closure.
	 *
	 * @param thread The current thread
	 * @return String identifying the file and line of the nearest lua closure,
	 * or the function name of the Java call if no closure is being called.
	 */
	public static String fileLine(LuaThread thread) {
		DebugState ds = thread.getDebugState();
		DebugFrame di;
		for (int i = 0, n = ds.top; i <= n; i++) {
			di = ds.getFrame(i);
			if (di != null && di.closure != null) {
				return di.sourceLine();
			}
		}
		return fileLine(thread, 0);
	}

	public static @Nullable String fileLine(LuaThread thread, int level) {
		DebugState ds = thread.getDebugState();
		DebugFrame di = ds.getFrame(level);
		return di != null ? di.sourceLine() : null;
	}

	private static ObjectName fromMetamethod(String name) {
		return new ObjectName(valueOf("__" + name), METAMETHOD);
	}

	public static @Nullable ObjectName getFuncName(DebugFrame di, int stackpos) {
		if (di.closure == null) return null;
		if ((di.flags & FLAG_ANY_HOOK) != 0) return new ObjectName(QUESTION, HOOK);

		Prototype p = di.closure.getPrototype();
		int pc = di.pc; // currentpc(L, ci);
		int i = p.code[pc];
		return switch (GET_OPCODE(i)) {
			case OP_CALL, OP_TAILCALL -> getObjectName(di, GETARG_A(i));
			case OP_SELF, OP_GETTABLE -> fromMetamethod("index");
			case OP_SETTABLE -> fromMetamethod("newindex");
			case OP_ADD -> fromMetamethod("add");
			case OP_SUB -> fromMetamethod("sub");
			case OP_MUL -> fromMetamethod("mul");
			case OP_DIV -> fromMetamethod("div");
			case OP_POW -> fromMetamethod("pow");
			case OP_MOD -> fromMetamethod("mod");
			case OP_UNM -> fromMetamethod("unm");
			case OP_EQ -> fromMetamethod("eq");
			case OP_LE -> fromMetamethod("le");
			case OP_LT -> fromMetamethod("lt");
			case OP_LEN -> fromMetamethod("len");
			case OP_CONCAT -> fromMetamethod("concat");
			default -> null;
		};
	}

	// return StrValue[] { name, namewhat } if found, null if not
	public static @Nullable ObjectName getObjectName(DebugFrame di, int stackpos) {
		if (di.closure == null) return null;
		if ((di.flags & FLAG_ANY_HOOK) != 0) return new ObjectName(QUESTION, HOOK);

		Prototype p = di.closure.getPrototype();
		int pc = di.pc; // currentpc(L, ci);
		LuaString name = p.getLocalName(stackpos + 1, pc);

		// is a local?
		if (name != null) return new ObjectName(name, LOCAL);

		pc = findSetReg(p, pc, stackpos); /* try symbolic execution */
		if (pc == -1) return null;

		int i = p.code[pc];
		switch (GET_OPCODE(i)) {
			case OP_MOVE -> {
				int a = GETARG_A(i);
				int b = GETARG_B(i); /* move from `b' to `a' */
				if (b < a) return getObjectName(di, b); /* get name for `b' */
			}
			case OP_GETTABUP, OP_GETTABLE -> {
				int t = GETARG_B(i);
				LuaString table = GET_OPCODE(i) == OP_GETTABUP ? p.getUpvalueName(t) : p.getLocalName(t + 1, pc);
				int c = GETARG_C(i); /* key index */
				return new ObjectName(constantName(p, c), Objects.equals(table, Constants.ENV) ? GLOBAL : FIELD);
			}
			case OP_GETUPVAL -> {
				int u = GETARG_B(i); /* upvalue index */
				return new ObjectName(Objects.requireNonNullElse(p.getUpvalueName(u), DebugLib.QMARK), UPVALUE);
			}
			case OP_SELF -> {
				int k = GETARG_C(i); /* key index */
				return new ObjectName(constantName(p, k), METHOD);
			}
		}

		return null; // no useful name found
	}

	private static int filterPc(int pc, int jumpTarget) {
		return pc < jumpTarget ? -1 : pc;
	}

	private static int findSetReg(Prototype pt, int lastpc, int reg) {
		int setreg = -1; // Last instruction that changed "reg";
		int jumpTarget = 0; // Any code before this address is conditional

		for (int pc = 0; pc < lastpc; pc++) {
			int i = pt.code[pc];
			int op = GET_OPCODE(i);
			int a = GETARG_A(i);
			switch (op) {
				case OP_LOADNIL -> {
					int b = GETARG_B(i);
					if (a <= reg && reg <= a + b) setreg = filterPc(pc, jumpTarget);
				}
				case OP_TFORCALL -> {
					if (a >= a + 2) setreg = filterPc(pc, jumpTarget);
				}
				case OP_CALL, OP_TAILCALL -> {
					if (reg >= a) setreg = filterPc(pc, jumpTarget);
				}
				case OP_JMP -> {
					int dest = pc + 1 + GETARG_sBx(i);
					// If jump is forward and doesn't skip lastPc, update jump target
					if (pc < dest && dest <= lastpc && dest > jumpTarget) jumpTarget = dest;
				}
				default -> {
					if (testAMode(op) && reg == a) setreg = filterPc(pc, jumpTarget);
				}
			}
		}
		return setreg;
	}

	private static LuaString constantName(Prototype proto, int index) {
		if (ISK(index) && proto.constants[INDEXK(index)].isString()) {
			return (LuaString) proto.constants[INDEXK(index)].toLuaString();
		} else {
			return DebugLib.QMARK;
		}
	}
}
