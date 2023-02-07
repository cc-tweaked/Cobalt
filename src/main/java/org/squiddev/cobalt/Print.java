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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

import static org.squiddev.cobalt.Lua.*;

/**
 * Debug helper class to pretty-print lua bytecodes.
 *
 * @see Prototype
 * @see LuaClosure
 */
public class Print {
	public static final String[] OPNAMES = {
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


	static void printString(PrintWriter ps, final LuaString s) {

		ps.print('"');
		for (int i = 0, n = s.length; i < n; i++) {
			int c = s.bytes[s.offset + i];
			if (c >= ' ' && c <= '~' && c != '\"' && c != '\\') {
				ps.print((char) c);
			} else {
				switch (c) {
					case '"':
						ps.print("\\\"");
						break;
					case '\\':
						ps.print("\\\\");
						break;
					case 0x0007: /* bell */
						ps.print("\\a");
						break;
					case '\b': /* backspace */
						ps.print("\\b");
						break;
					case '\f':  /* form feed */
						ps.print("\\f");
						break;
					case '\t':  /* tab */
						ps.print("\\t");
						break;
					case '\r': /* carriage return */
						ps.print("\\r");
						break;
					case '\n': /* newline */
						ps.print("\\n");
						break;
					case 0x000B: /* vertical tab */
						ps.print("\\v");
						break;
					default:
						ps.print('\\');
						ps.print(Integer.toString(1000 + 0xff & c).substring(1));
						break;
				}
			}
		}
		ps.print('"');
	}

	private static void printValue(PrintWriter ps, LuaValue v) {
		switch (v.type()) {
			case Constants.TSTRING:
				printString(ps, (LuaString) v);
				break;
			default:
				ps.print(v.toString());

		}
	}

	private static void printConstant(PrintWriter ps, Prototype f, int i) {
		printValue(ps, f.constants[i]);
	}

	/**
	 * Print the code in a prototype
	 *
	 * @param f the {@link Prototype}
	 *          @param extended Included extended/non-standard information.
	 */
	public static void printCode(PrintWriter ps, Prototype f, boolean extended) {
		int[] code = f.code;
		int pc, n = code.length;
		for (pc = 0; pc < n; pc++) {
			printOpcode(ps, f, pc, extended);
			ps.println();
		}
	}

	/**
	 * Print an opcode in a prototype
	 *
	 * @param ps the {@link PrintWriter} to print to
	 * @param f  the {@link Prototype}
	 * @param pc the program counter to look up and print
	 * @param extended Included extended/non-standard information.
	 */
	public static void printOpcode(PrintWriter ps, Prototype f, int pc, boolean extended) {
		int[] code = f.code;
		int i = code[pc];
		int o = GET_OPCODE(i);
		int a = GETARG_A(i);
		int b = GETARG_B(i);
		int c = GETARG_C(i);
		int bx = GETARG_Bx(i);
		int sbx = GETARG_sBx(i);
		int line = f.lineInfo != null && pc < f.lineInfo.length ? f.lineInfo[pc] : -1;
		int column = f.columnInfo != null && pc < f.columnInfo.length ? f.columnInfo[pc] : -1;
		ps.print("  " + (pc + 1) + "  ");
		if (extended && line > 0 && column > 0) {
			ps.print("[" + line + "/" + column + "]  ");
		} else if (line > 0) {
			ps.print("[" + line + "]  ");
		} else {
			ps.print("[-]  ");
		}
		ps.print(OPNAMES[o] + "  ");
		switch (getOpMode(o)) {
			case iABC:
				ps.print(a);
				if (getBMode(o) != OpArgN) {
					ps.print(" " + (ISK(b) ? (-1 - INDEXK(b)) : b));
				}
				if (getCMode(o) != OpArgN) {
					ps.print(" " + (ISK(c) ? (-1 - INDEXK(c)) : c));
				}
				break;
			case iABx:
				if (getBMode(o) == OpArgK) {
					ps.print(a + " " + (-1 - bx));
				} else {
					ps.print(a + " " + (bx));
				}
				break;
			case iAsBx:
				if (o == OP_JMP) {
					ps.print(sbx);
				} else {
					ps.print(a + " " + sbx);
				}
				break;
		}
		switch (o) {
			case OP_LOADK:
				ps.print("  ; ");
				printConstant(ps, f, bx);
				break;
			case OP_GETUPVAL:
			case OP_SETUPVAL:
				ps.print("  ; ");
				if (f.upvalueNames.length > b) {
					printValue(ps, f.upvalueNames[b]);
				} else {
					ps.print("-");
				}
				break;
			case OP_GETGLOBAL:
			case OP_SETGLOBAL:
				ps.print("  ; ");
				printConstant(ps, f, bx);
				break;
			case OP_GETTABLE:
			case OP_SELF:
				if (ISK(c)) {
					ps.print("  ; ");
					printConstant(ps, f, INDEXK(c));
				}
				break;
			case OP_SETTABLE:
			case OP_ADD:
			case OP_SUB:
			case OP_MUL:
			case OP_DIV:
			case OP_POW:
			case OP_EQ:
			case OP_LT:
			case OP_LE:
				if (ISK(b) || ISK(c)) {
					ps.print("  ; ");
					if (ISK(b)) {
						printConstant(ps, f, INDEXK(b));
					} else {
						ps.print("-");
					}
					ps.print(" ");
					if (ISK(c)) {
						printConstant(ps, f, INDEXK(c));
					} else {
						ps.print("-");
					}
				}
				break;
			case OP_JMP:
			case OP_FORLOOP:
			case OP_FORPREP:
				ps.print("  ; to " + (sbx + pc + 2));
				break;
			case OP_CLOSURE:
				ps.print("  ; " + f.children[bx].getClass().getName());
				break;
			case OP_SETLIST:
				if (c == 0) {
					ps.print("  ; " + code[++pc]);
				} else {
					ps.print("  ; " + c);
				}
				break;
			case OP_VARARG:
				ps.print("  ; is_vararg=" + f.isVarArg);
				break;
			default:
				break;
		}
	}

	private static void printHeader(PrintWriter ps, Prototype f) {
		String s = String.valueOf(f.source);
		if (s.startsWith("@") || s.startsWith("=")) {
			s = s.substring(1);
		} else if ("\033Lua".equals(s)) {
			s = "(bstring)";
		} else {
			s = "(string)";
		}
		String a = (f.lineDefined == 0) ? "main" : "function";
		ps.print("\n%" + a + " <" + s + ":" + f.lineDefined + ","
			+ f.lastLineDefined + "> (" + f.code.length + " instructions, "
			+ f.code.length * 4 + " bytes at " + id(f) + ")\n");
		ps.print(f.parameters + " param, " + f.maxStackSize + " slot, "
			+ f.upvalueNames.length + " upvalue, ");
		ps.print(f.locals.length + " local, " + f.constants.length
			+ " constant, " + f.children.length + " function\n");
	}

	private static void printConstants(PrintWriter ps, Prototype f) {
		int i, n = f.constants.length;
		ps.print("constants (" + n + ") for " + id(f) + ":\n");
		for (i = 0; i < n; i++) {
			ps.print("  " + (i + 1) + "  ");
			printValue(ps, f.constants[i]);
			ps.print("\n");
		}
	}

	private static void printLocals(PrintWriter ps, Prototype f) {
		int i, n = f.locals.length;
		ps.print("locals (" + n + ") for " + id(f) + ":\n");
		for (i = 0; i < n; i++) {
			ps.println("  " + i + "  " + f.locals[i].name + " " + (f.locals[i].startpc + 1) + " " + (f.locals[i].endpc + 1));
		}
	}

	private static void printUpValues(PrintWriter ps, Prototype f) {
		int i, n = f.upvalueNames.length;
		ps.print("upvalues (" + n + ") for " + id(f) + ":\n");
		for (i = 0; i < n; i++) {
			ps.print("  " + i + "  " + f.upvalueNames[i] + "\n");
		}
	}

	public static void printFunction(PrintWriter ps, Prototype f, boolean full, boolean extended) {
		int i, n = f.children.length;
		printHeader(ps, f);
		printCode(ps, f, extended);
		if (full) {
			printConstants(ps, f);
			printLocals(ps, f);
			printUpValues(ps, f);
		}
		for (i = 0; i < n; i++) {
			printFunction(ps, f.children[i], full, extended);
		}
	}

	private static void format(PrintWriter ps, String s, int maxcols) {
		int n = s.length();
		if (n > maxcols) {
			ps.print(s.substring(0, maxcols));
		} else {
			ps.print(s);
			for (int i = maxcols - n; --i >= 0; ) {
				ps.print(' ');
			}
		}
	}

	private static String id(Prototype f) {
		return f.sourceShort() + ":" + f.lineDefined;
	}

	/**
	 * Print the state of a {@link LuaClosure} that is being executed
	 *
	 * @param cl      the {@link LuaClosure}
	 * @param pc      the program counter
	 * @param stack   the stack of {@link LuaValue}
	 * @param top     the top of the stack
	 * @param varargs any {@link Varargs} value that may apply
	 */
	public static void printState(PrintWriter ps, LuaClosure cl, int pc, LuaValue[] stack, int top, Varargs varargs) {
		// print opcode into buffer
		format(ps, showWith(p -> printOpcode(p, cl.getPrototype(), pc, true)), 50);

		// print stack
		ps.print('[');
		for (int i = 0; i < stack.length; i++) {
			LuaValue v = stack[i];
			if (v == null) {
				ps.print("null");
			} else {
				switch (v.type()) {
					case Constants.TSTRING:
						LuaString s = (LuaString) v;
						ps.print(s.length() < 48 ?
							s.toString() :
							s.substring(0, 32).toString() + "...+" + (s.length() - 32) + "b");
						break;
					case Constants.TFUNCTION:
						ps.print((v instanceof LuaClosure) ?
							((LuaClosure) v).getPrototype().toString() : v.toString());
						break;
					case Constants.TUSERDATA:
						Object o = v.toUserdata();
						if (o != null) {
							String n = o.getClass().getName();
							n = n.substring(n.lastIndexOf('.') + 1);
							ps.print(n + ": " + Integer.toHexString(o.hashCode()));
						} else {
							ps.print(v.toString());
						}
						break;
					default:
						ps.print(v.toString());
				}
			}
			if (i + 1 == top) {
				ps.print(']');
			}
			ps.print(" | ");
		}
		ps.print(varargs);
		ps.println();
	}

	private static String showWith(Consumer<PrintWriter> f) {
		StringWriter output = new StringWriter();
		try (PrintWriter ps = new PrintWriter(output)) {
			f.accept(ps);
		}
		return output.toString();
	}
}
