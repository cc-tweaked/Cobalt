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
package org.squiddev.cobalt;

import org.squiddev.cobalt.function.LuaClosure;

import static org.squiddev.cobalt.Lua.*;

/**
 * Debug helper class for pretty-printing Lua bytecode
 *
 * @see Prototype
 * @see LuaClosure
 */
public final class Print {
	private static final String[] OPNAMES = {
		"MOVE",
		"LOADK",
		"LOADBOOL",
		"LOADNIL",
		"GETUPVAL",
		"GETGLOBAL",
		"GETTABLE",
		"SETGLOBAL",
		"SETUPVAL",
		"SETTABLE",
		"NEWTABLE",
		"SELF",
		"ADD",
		"SUB",
		"MUL",
		"DIV",
		"MOD",
		"POW",
		"UNM",
		"NOT",
		"LEN",
		"CONCAT",
		"JMP",
		"EQ",
		"LT",
		"LE",
		"TEST",
		"TESTSET",
		"CALL",
		"TAILCALL",
		"RETURN",
		"FORLOOP",
		"FORPREP",
		"TFORLOOP",
		"SETLIST",
		"CLOSE",
		"CLOSURE",
		"VARARG",
		null,
	};

	private static void printString(StringBuilder out, final LuaString s) {
		out.append('"');
		for (int i = 0, n = s.length(); i < n; i++) {
			int c = s.charAt(i);
			if (c >= ' ' && c <= '~' && c != '\"' && c != '\\') {
				out.append((char) c);
			} else {
				switch (c) {
					case '"' -> out.append("\\\"");
					case '\\' -> out.append("\\\\");
					case 0x0007 -> out.append("\\a");
					case '\b' -> out.append("\\b");
					case '\f' -> out.append("\\f");
					case '\t' -> out.append("\\t");
					case '\r' -> out.append("\\r");
					case '\n' -> out.append("\\n");
					case 0x000B -> out.append("\\v");
					default -> {
						out.append('\\');
						out.append(Integer.toString(1000 + c).substring(1));
					}
				}
			}
		}
		out.append('"');
	}

	private static void printValue(StringBuilder out, LuaValue v) {
		switch (v.type()) {
			case Constants.TSTRING -> printString(out, (LuaString) v);
			default -> out.append(v);
		}
	}

	private static void printConstant(StringBuilder out, Prototype f, int i) {
		printValue(out, f.constants[i]);
	}

	/**
	 * Print the code in a prototype
	 *
	 * @param out      The buffer to write to.
	 * @param f        the {@link Prototype}
	 * @param extended Included extended/non-standard information.
	 */
	public static void printCode(StringBuilder out, Prototype f, boolean extended) {
		int[] code = f.code;
		int pc, n = code.length;
		for (pc = 0; pc < n; pc++) {
			printOpcode(out, f, pc, extended);
			out.append("\n");
		}
	}

	/**
	 * Print an opcode in a prototype
	 *
	 * @param out      The {@link StringBuilder} to write to.
	 * @param f        The {@link Prototype}
	 * @param pc       The program counter to look up and print.
	 * @param extended Included extended/non-standard information.
	 */
	public static void printOpcode(StringBuilder out, Prototype f, int pc, boolean extended) {
		int[] code = f.code;
		int i = code[pc];
		int o = GET_OPCODE(i);
		int a = GETARG_A(i);
		int b = GETARG_B(i);
		int c = GETARG_C(i);
		int bx = GETARG_Bx(i);
		int sbx = GETARG_sBx(i);

		out.append("  ").append(pc + 1).append("  ");

		int line = f.lineAt(pc);
		int column = f.columnAt(pc);
		if (extended && line > 0 && column > 0) {
			out.append("[").append(line).append("/").append(column).append("]  ");
		} else if (line > 0) {
			out.append("[").append(line).append("]  ");
		} else {
			out.append("[-]  ");
		}

		out.append(OPNAMES[o]).append("  ");
		switch (getOpMode(o)) {
			case iABC -> {
				out.append(a);
				if (getBMode(o) != OpArgN) out.append(" ").append(ISK(b) ? (-1 - INDEXK(b)) : b);
				if (getCMode(o) != OpArgN) out.append(" ").append(ISK(c) ? (-1 - INDEXK(c)) : c);
			}
			case iABx -> out.append(a).append(" ").append(getBMode(o) == OpArgK ? -1 - bx : bx);
			case iAsBx -> {
				if (o == OP_JMP) {
					out.append(sbx);
				} else {
					out.append(a).append(" ").append(sbx);
				}
			}
		}
		switch (o) {
			case OP_LOADK -> {
				out.append("  ; ");
				printConstant(out, f, bx);
			}
			case OP_GETUPVAL, OP_SETUPVAL -> {
				out.append("  ; ");
				if (f.upvalueNames.length > b) {
					printValue(out, f.upvalueNames[b]);
				} else {
					out.append("-");
				}
			}
			case OP_GETGLOBAL, OP_SETGLOBAL -> {
				out.append("  ; ");
				printConstant(out, f, bx);
			}
			case OP_GETTABLE, OP_SELF -> {
				if (ISK(c)) {
					out.append("  ; ");
					printConstant(out, f, INDEXK(c));
				}
			}
			case OP_SETTABLE, OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_POW, OP_EQ, OP_LT, OP_LE -> {
				if (ISK(b) || ISK(c)) {
					out.append("  ; ");
					if (ISK(b)) {
						printConstant(out, f, INDEXK(b));
					} else {
						out.append("-");
					}
					out.append(" ");
					if (ISK(c)) {
						printConstant(out, f, INDEXK(c));
					} else {
						out.append("-");
					}
				}
			}
			case OP_JMP, OP_FORLOOP, OP_FORPREP -> out.append("  ; to ").append(sbx + pc + 2);
			case OP_CLOSURE -> out.append("  ; ").append(f.children[bx].getClass().getName());
			case OP_SETLIST -> {
				if (c == 0) {
					out.append("  ; ").append(code[++pc]);
				} else {
					out.append("  ; ").append(c);
				}
			}
			case OP_VARARG -> out.append("  ; is_vararg=").append(f.isVarArg);
			default -> {
			}
		}
	}

	private static void printHeader(StringBuilder out, Prototype f) {
		String s = String.valueOf(f.source);
		if (s.startsWith("@") || s.startsWith("=")) {
			s = s.substring(1);
		} else if ("\033Lua".equals(s)) {
			s = "(bstring)";
		} else {
			s = "(string)";
		}
		String a = (f.lineDefined == 0) ? "main" : "function";
		out.append("\n%").append(a)
			.append(" <").append(s).append(":").append(f.lineDefined).append(",").append(f.lastLineDefined).append("> (")
			.append(f.code.length).append(" instructions, ").append(f.code.length * 4).append(" bytes at ").append(id(f)).append(")\n");
		out.append(f.parameters).append(" param, ").append(f.maxStackSize).append(" slot, ").append(f.upvalueNames.length).append(" upvalue, ");
		out.append(f.locals.length).append(" local, ").append(f.constants.length).append(" constant, ").append(f.children.length).append(" function\n");
	}

	private static void printConstants(StringBuilder out, Prototype f) {
		int i, n = f.constants.length;
		out.append("constants (").append(n).append(") for ").append(id(f)).append(":\n");
		for (i = 0; i < n; i++) {
			out.append("  ").append(i + 1).append("  ");
			printValue(out, f.constants[i]);
			out.append("\n");
		}
	}

	private static void printLocals(StringBuilder out, Prototype f) {
		int i, n = f.locals.length;
		out.append("locals (").append(n).append(") for ").append(id(f)).append(":\n");
		for (i = 0; i < n; i++) {
			out.append("  ").append(i).append("  ").append(f.locals[i].name).append(" ").append(f.locals[i].startpc + 1).append(" ").append(f.locals[i].endpc + 1).append("\n");
		}
	}

	private static void printUpValues(StringBuilder out, Prototype f) {
		int i, n = f.upvalueNames.length;
		out.append("upvalues (").append(n).append(") for ").append(id(f)).append(":\n");
		for (i = 0; i < n; i++) {
			out.append("  ").append(i).append("  ").append(f.upvalueNames[i]).append("\n");
		}
	}

	public static void printFunction(StringBuilder out, Prototype f, boolean full, boolean extended) {
		int i, n = f.children.length;
		printHeader(out, f);
		printCode(out, f, extended);
		if (full) {
			printConstants(out, f);
			printLocals(out, f);
			printUpValues(out, f);
		}
		for (i = 0; i < n; i++) {
			printFunction(out, f.children[i], full, extended);
		}
	}

	private static String id(Prototype f) {
		return f.shortSource() + ":" + f.lineDefined;
	}

	/**
	 * Print the state of a {@link LuaClosure} that is being executed
	 *
	 * @param out     The string builder to write to.
	 * @param cl      The {@link LuaClosure}
	 * @param pc      The program counter
	 * @param stack   The stack of {@link LuaValue}
	 * @param top     The top of the stack
	 * @param varargs any {@link Varargs} value that may apply
	 */
	public static void printState(StringBuilder out, LuaClosure cl, int pc, LuaValue[] stack, int top) {
		// print opcode into buffer
		int len = out.length();
		printOpcode(out, cl.getPrototype(), pc, true);
		while (out.length() < len + 50) out.append(' ');

		// print stack
		out.append('[');
		for (int i = 0; i < stack.length; i++) {
			LuaValue v = stack[i];
			if (v == null) {
				out.append("null");
			} else {
				switch (v.type()) {
					case Constants.TSTRING -> {
						LuaString s = (LuaString) v;
						out.append(s.length() < 48 ?
							s.toString() :
							s.substringOfEnd(0, 32) + "...+" + (s.length() - 32) + "b");
					}
					case Constants.TFUNCTION -> out.append((v instanceof LuaClosure) ?
						((LuaClosure) v).getPrototype().toString() : v.toString());
					case Constants.TUSERDATA -> {
						out.append(v);
					}
					default -> out.append(v);
				}
			}
			if (i + 1 == top) {
				out.append(']');
			}
			out.append(" | ");
		}
	}
}
