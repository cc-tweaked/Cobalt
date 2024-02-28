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
 * Debug helper class for pretty-printing Lua bytecode.
 * <p>
 * This follows the implementation in {@code luac.c}.
 *
 * @see Prototype
 * @see LuaClosure
 */
public final class Print {
	private Print() {
	}

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
					default -> out.append(String.format("\\%03d", c));
				}
			}
		}
		out.append('"');
	}

	private static void printConstant(StringBuilder out, Prototype f, int i) {
		var value = f.constants[i];
		switch (value.type()) {
			case Constants.TSTRING -> printString(out, (LuaString) value);
			default -> out.append(value);
		}
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
		for (int pc = 0; pc < code.length; pc++) {
			printOpcode(out, f, pc, extended);
			out.append("\n");
		}
	}

	private static int MYK(int x) {
		return -1 - x;
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
		int ax = GETARG_Ax(i);
		int bx = GETARG_Bx(i);
		int sbx = GETARG_sBx(i);

		out.append("\t").append(pc + 1).append("\t");

		int line = f.lineAt(pc);
		int column = f.columnAt(pc);
		if (extended && line > 0 && column > 0) {
			out.append("[").append(line).append("/").append(column).append("]");
		} else if (line > 0) {
			out.append("[").append(line).append("]");
		} else {
			out.append("[-]");
		}

		out.append("\t");

		var name = Lua.getOpName(o);
		out.append(name);
		for (int j = name.length(); j < 9; j++) out.append(' ');

		out.append("\t");

		switch (getOpMode(o)) {
			case iABC -> {
				out.append(a);
				if (getBMode(o) != OpArgN) out.append(" ").append(ISK(b) ? MYK(INDEXK(b)) : b);
				if (getCMode(o) != OpArgN) out.append(" ").append(ISK(c) ? MYK(INDEXK(c)) : c);
			}
			case iABx -> {
				out.append(a);
				if (getBMode(o) == OpArgK) out.append(" ").append(MYK(bx));
				if (getBMode(o) == OpArgU) out.append(" ").append(bx);
			}
			case iAsBx -> out.append(a).append(" ").append(sbx);
			case iAx -> out.append(a).append(" ").append(MYK(ax));
		}

		switch (o) {
			case OP_LOADK -> {
				out.append("\t; ");
				printConstant(out, f, bx);
			}
			case OP_GETUPVAL, OP_SETUPVAL -> {
				out.append("\t; ");
				printUpvalueName(out, f, b);
			}
			case OP_GETTABUP -> {
				out.append("\t; ");
				printUpvalueName(out, f, b);
				if (ISK(c)) {
					out.append(" ");
					printConstant(out, f, INDEXK(c));
				}
			}
			case OP_SETTABUP -> {
				out.append("\t; ");
				printUpvalueName(out, f, a);
				if (ISK(b)) {
					out.append(" ");
					printConstant(out, f, INDEXK(b));
				}
				if (ISK(c)) {
					out.append(" ");
					printConstant(out, f, INDEXK(c));
				}
			}
			case OP_GETTABLE, OP_SELF -> {
				if (ISK(c)) {
					out.append("\t; ");
					printConstant(out, f, INDEXK(c));
				}
			}
			case OP_SETTABLE, OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_POW, OP_EQ, OP_LT, OP_LE -> {
				if (ISK(b) || ISK(c)) {
					out.append("\t; ");
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
			case OP_JMP, OP_FORLOOP, OP_FORPREP, OP_TFORLOOP -> out.append("\t; to ").append(sbx + pc + 2);
			case OP_CLOSURE -> out.append("\t; ").append(id(f.children[bx]));
			case OP_SETLIST -> {
				if (c == 0) {
					out.append("\t; ").append(code[++pc]);
				} else {
					out.append("\t; ").append(c);
				}
			}
			case OP_EXTRAARG -> {
				out.append("\t; ");
				printConstant(out, f, ax);
			}
			default -> {
			}
		}
	}

	private static void printUpvalueName(StringBuilder out, Prototype f, int upvalue) {
		var upvalueName = f.getUpvalueName(upvalue);
		if (upvalueName != null) {
			out.append(upvalueName);
		} else {
			out.append("-");
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
		out.append("\n%").append(f.lineDefined == 0 ? "main" : "function")
			.append(" <").append(s).append(":").append(f.lineDefined).append(",").append(f.lastLineDefined).append("> (")
			.append(f.code.length).append(" instructions, ").append(f.code.length * 4).append(" bytes at ").append(id(f)).append(")\n");
		out.append(f.parameters).append(" param, ").append(f.maxStackSize).append(" slots, ").append(f.upvalues()).append(" upvalues, ");
		out.append(f.locals.length).append(" locals, ").append(f.constants.length).append(" constants, ").append(f.children.length).append(" functions\n");
	}

	private static void printConstants(StringBuilder out, Prototype f) {
		int i, n = f.constants.length;
		out.append("constants (").append(n).append(") for ").append(id(f)).append(":\n");
		for (i = 0; i < n; i++) {
			out.append("\t").append(i + 1).append("\t");
			printConstant(out, f, i);
			out.append("\n");
		}
	}

	private static void printLocals(StringBuilder out, Prototype f) {
		int i, n = f.locals.length;
		out.append("locals (").append(n).append(") for ").append(id(f)).append(":\n");
		for (i = 0; i < n; i++) {
			out
				.append("\t").append(i)
				.append("\t").append(f.locals[i].name)
				.append("\t").append(f.locals[i].startpc + 1)
				.append("\t").append(f.locals[i].endpc + 1)
				.append("\n");
		}
	}

	private static void printUpvalues(StringBuilder out, Prototype f) {
		out.append("upvalues (").append(f.upvalues()).append(") for ").append(id(f)).append(":\n");
		for (int i = 0, n = f.upvalues(); i < n; i++) {
			var upvalue = f.getUpvalue(i);
			out
				.append("\t").append(i)
				.append("\t").append(upvalue.name())
				.append("\t").append(upvalue.fromLocal() ? '1' : '0')
				.append("\t").append(upvalue.index())
				.append("\n");
		}
	}

	public static void printFunction(StringBuilder out, Prototype f, boolean full, boolean extended) {
		int i, n = f.children.length;
		printHeader(out, f);
		printCode(out, f, extended);
		if (full) {
			printConstants(out, f);
			printLocals(out, f);
			printUpvalues(out, f);
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
	 * @param out   The string builder to write to.
	 * @param cl    The {@link LuaClosure}
	 * @param pc    The program counter
	 * @param stack The stack of {@link LuaValue}
	 * @param top   The top of the stack
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
					case Constants.TFUNCTION ->
						out.append(v instanceof LuaClosure c ? c.getPrototype().toString() : v.toString());
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
