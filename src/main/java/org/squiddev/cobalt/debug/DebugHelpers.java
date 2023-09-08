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
import org.squiddev.cobalt.compiler.LuaC;
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
		for (int i = 0, n = ds.top; i < n; i++) {
			di = ds.getFrame(i);
			if (di != null && di.closure != null) {
				return di.sourceLine();
			}
		}
		return fileLine(thread, 0);
	}

	public static String fileLine(LuaThread thread, int level) {
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
		return switch (Lua.GET_OPCODE(i)) {
			case OP_CALL, OP_TAILCALL -> getObjectName(di, Lua.GETARG_A(i));
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
		int i; // Instruction i;
		LuaString name = p.getLocalName(stackpos + 1, pc);

		// is a local?
		if (name != null) return new ObjectName(name, LOCAL);

		i = symbexec(p, pc, stackpos); /* try symbolic execution */
		lua_assert(pc != -1);
		switch (Lua.GET_OPCODE(i)) {
			case OP_GETGLOBAL -> {
				int g = Lua.GETARG_Bx(i); /* global index */
				// lua_assert(p.k[g].isString());
				LuaValue value = p.constants[g];
				LuaString string = OperationHelper.toStringDirect(value);
				return new ObjectName(string, GLOBAL);
			}
			case OP_MOVE -> {
				int a = Lua.GETARG_A(i);
				int b = Lua.GETARG_B(i); /* move from `b' to `a' */
				if (b < a) return getObjectName(di, b); /* get name for `b' */
			}
			case OP_GETTABLE -> {
				int k = Lua.GETARG_C(i); /* key index */
				return new ObjectName(constantName(p, k), FIELD);
			}
			case OP_GETUPVAL -> {
				int u = Lua.GETARG_B(i); /* upvalue index */
				return new ObjectName(Objects.requireNonNullElse(p.getUpvalueName(u), DebugLib.QMARK), UPVALUE);
			}
			case OP_SELF -> {
				int k = Lua.GETARG_C(i); /* key index */
				return new ObjectName(constantName(p, k), METHOD);
			}
		}

		return null; // no useful name found
	}

	// return last instruction, or 0 if error
	private static int symbexec(Prototype pt, int lastpc, int reg) {
		int pc;
		int last; /* stores position of last instruction that changed `reg' */
		last = pt.code.length - 1; /*
		 * points to final return (a `neutral'
		 * instruction)
		 */
		if (!(precheck(pt))) return 0;
		for (pc = 0; pc < lastpc; pc++) {
			int i = pt.code[pc];
			int op = Lua.GET_OPCODE(i);
			int a = Lua.GETARG_A(i);
			int b = 0;
			int c = 0;
			if (!(op < Lua.NUM_OPCODES)) return 0;
			if (!checkRegister(pt, a)) return 0;
			switch (Lua.getOpMode(op)) {
				case Lua.iABC -> {
					b = Lua.GETARG_B(i);
					c = Lua.GETARG_C(i);
					if (!(checkArgMode(pt, b, Lua.getBMode(op)))) return 0;
					if (!(checkArgMode(pt, c, Lua.getCMode(op)))) return 0;
				}
				case Lua.iABx -> {
					b = Lua.GETARG_Bx(i);
					if (Lua.getBMode(op) == Lua.OpArgK) {
						if (!(b < pt.constants.length)) return 0;
					}
				}
				case Lua.iAsBx -> {
					b = Lua.GETARG_sBx(i);
					if (Lua.getBMode(op) == Lua.OpArgR) {
						int dest = pc + 1 + b;
						if (!(0 <= dest && dest < pt.code.length)) return 0;
						if (dest > 0) {
							/* cannot jump to a setlist count */
							int d = pt.code[dest - 1];
							if ((Lua.GET_OPCODE(d) == Lua.OP_SETLIST && Lua.GETARG_C(d) == 0)) return 0;
						}
					}
				}
			}
			if (Lua.testAMode(op)) {
				if (a == reg) {
					last = pc; /* change register `a' */
				}
			}
			if (Lua.testTMode(op)) {
				if (!(pc + 2 < pt.code.length)) return 0; /* check skip */
				if (!(Lua.GET_OPCODE(pt.code[pc + 1]) == Lua.OP_JMP)) return 0;
			}
			switch (op) {
				case Lua.OP_LOADBOOL: {
					if (!(c == 0 || pc + 2 < pt.code.length)) return 0; /* check its jump */
					break;
				}
				case Lua.OP_LOADNIL: {
					if (a <= reg && reg <= b) {
						last = pc; /* set registers from `a' to `b' */
					}
					break;
				}
				case OP_GETUPVAL:
				case Lua.OP_SETUPVAL: {
					if (!(b < pt.upvalues)) return 0;
					break;
				}
				case OP_GETGLOBAL:
				case Lua.OP_SETGLOBAL: {
					if (!(pt.constants[b].isString())) return 0;
					break;
				}
				case OP_SELF: {
					if (!checkRegister(pt, a + 1)) return 0;
					if (reg == a + 1) {
						last = pc;
					}
					break;
				}
				case Lua.OP_CONCAT: {
					if (!(b < c)) return 0; /* at least two operands */
					break;
				}
				case Lua.OP_TFORLOOP: {
					if (!(c >= 1)) return 0; /* at least one result (control variable) */
					if (!checkRegister(pt, a + 2 + c)) return 0; /* space for results */
					if (reg >= a + 2) {
						last = pc; /* affect all regs above its base */
					}
					break;
				}
				case Lua.OP_FORLOOP:
				case Lua.OP_FORPREP:
					if (!checkRegister(pt, a + 3)) return 0;
					// fallthrough
				case Lua.OP_JMP: {
					int dest = pc + 1 + b;
					/* not full check and jump is forward and do not skip `lastpc'? */
					if (reg != Lua.NO_REG && pc < dest && dest <= lastpc) {
						pc += b; /* do the jump */
					}
					break;
				}
				case OP_CALL:
				case OP_TAILCALL: {
					if (b != 0) {
						if (!checkRegister(pt, a + b - 1)) return 0;
					}
					c--; /* c = num. returns */
					if (c == Lua.LUA_MULTRET) {
						if (!(checkOpenUp(pt, pc))) return 0;
					} else if (c != 0) {
						if (!checkRegister(pt, a + c - 1)) return 0;
					}
					if (reg >= a) {
						last = pc; /* affect all registers above base */
					}
					break;
				}
				case Lua.OP_RETURN: {
					b--; /* b = num. returns */
					if (b > 0) {
						if (!checkRegister(pt, a + b - 1)) return 0;
					}
					break;
				}
				case Lua.OP_SETLIST: {
					if (b > 0) {
						if (!checkRegister(pt, a + b)) return 0;
					}
					if (c == 0) {
						pc++;
					}
					break;
				}
				case Lua.OP_CLOSURE: {
					int nup, j;
					if (!(b < pt.children.length)) return 0;
					nup = pt.children[b].upvalues;
					if (!(pc + nup < pt.code.length)) return 0;
					for (j = 1; j <= nup; j++) {
						int op1 = Lua.GET_OPCODE(pt.code[pc + j]);
						if (!(op1 == OP_GETUPVAL || op1 == OP_MOVE)) return 0;
					}
					if (reg != Lua.NO_REG) /* tracing? */ {
						pc += nup; /* do not 'execute' these pseudo-instructions */
					}
					break;
				}
				case Lua.OP_VARARG: {
					if (!((pt.isVarArg & Lua.VARARG_ISVARARG) != 0
						&& (pt.isVarArg & Lua.VARARG_NEEDSARG) == 0)) {
						return 0;
					}
					b--;
					if (b == Lua.LUA_MULTRET) {
						if (!(checkOpenUp(pt, pc))) return 0;
					}
					if (!checkRegister(pt, a + b - 1)) return 0;
					break;
				}
				default:
					break;
			}
		}
		return pt.code[last];
	}

	private static boolean precheck(Prototype pt) {
		if (!(pt.maxStackSize <= LuaC.MAXSTACK)) return false;
		lua_assert(pt.parameters + (pt.isVarArg & Lua.VARARG_HASARG) <= pt.maxStackSize);
		lua_assert((pt.isVarArg & Lua.VARARG_NEEDSARG) == 0
			|| (pt.isVarArg & Lua.VARARG_HASARG) != 0);
		return pt.upvalueNames.length <= pt.upvalues && (pt.lineInfo.length == pt.code.length || pt.lineInfo.length == 0) && Lua.GET_OPCODE(pt.code[pt.code.length - 1]) == Lua.OP_RETURN;
	}

	private static boolean checkArgMode(Prototype pt, int val, int mode) {
		switch (mode) {
			case Lua.OpArgN:
				if (!(val == 0)) return false;
				break;
			case Lua.OpArgU:
				break;
			case Lua.OpArgR:
				checkRegister(pt, val);
				break;
			case Lua.OpArgK:
				if (!(Lua.ISK(val) ? Lua.INDEXK(val) < pt.constants.length : val < pt.maxStackSize)) return false;
				break;
		}
		return true;
	}

	private static boolean checkRegister(Prototype proto, int reg) {
		return (reg < proto.maxStackSize);
	}

	private static boolean checkOpenUp(Prototype proto, int pc) {
		int i = proto.code[(pc) + 1];
		return switch (Lua.GET_OPCODE(i)) {
			case OP_CALL, OP_TAILCALL, Lua.OP_RETURN, Lua.OP_SETLIST -> Lua.GETARG_B(i) == 0;
			default -> false; /* invalid instruction after an open call */
		};
	}

	private static LuaString constantName(Prototype proto, int index) {
		if (Lua.ISK(index) && proto.constants[Lua.INDEXK(index)].isString()) {
			return (LuaString) proto.constants[Lua.INDEXK(index)].toLuaString();
		} else {
			return DebugLib.QMARK;
		}
	}

	private static void lua_assert(boolean x) {
		if (!x) throw new RuntimeException("lua_assert failed");
	}
}
