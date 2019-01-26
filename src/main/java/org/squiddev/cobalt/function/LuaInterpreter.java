/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2017 SquidDev
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

package org.squiddev.cobalt.function;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;

import static org.squiddev.cobalt.Constants.FALSE;
import static org.squiddev.cobalt.Constants.TRUE;
import static org.squiddev.cobalt.Lua.*;

/**
 * The main interpreter for {@link LuaInterpretedFunction}s.
 */
public final class LuaInterpreter {
	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function) throws LuaError {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(Constants.NILS, 0, stack, 0, p.maxstacksize);

		return setupCall(state, function, Constants.NONE, stack);
	}

	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, LuaValue arg) throws LuaError {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(Constants.NILS, 0, stack, 0, p.maxstacksize);

		switch (p.numparams) {
			case 0:
				return setupCall(state, function, arg, stack);

			default:
				stack[0] = arg;
				return setupCall(state, function, Constants.NONE, stack);
		}
	}

	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, LuaValue arg1, LuaValue arg2) throws LuaError {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(Constants.NILS, 0, stack, 0, p.maxstacksize);

		switch (p.numparams) {
			case 0:
				return setupCall(state, function, p.is_vararg != 0 ? ValueFactory.varargsOf(arg1, arg2) : Constants.NONE, stack);

			case 1:
				stack[0] = arg1;
				return setupCall(state, function, arg2, stack);

			default:
				stack[0] = arg1;
				stack[1] = arg2;
				return setupCall(state, function, Constants.NONE, stack);
		}
	}

	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(Constants.NILS, 0, stack, 0, p.maxstacksize);

		switch (p.numparams) {
			case 0:
				return setupCall(state, function, p.is_vararg != 0 ? ValueFactory.varargsOf(arg1, arg2, arg3) : Constants.NONE, stack);

			case 1:
				stack[0] = arg1;
				return setupCall(state, function, p.is_vararg != 0 ? ValueFactory.varargsOf(arg2, arg3) : Constants.NONE, stack);

			case 2:
				stack[0] = arg1;
				stack[1] = arg2;
				return setupCall(state, function, arg3, stack);

			default:
				stack[0] = arg1;
				stack[1] = arg2;
				stack[2] = arg3;
				return setupCall(state, function, Constants.NONE, stack);
		}
	}

	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, Varargs varargs) throws LuaError {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(Constants.NILS, 0, stack, 0, p.maxstacksize);
		for (int i = 0; i < p.numparams; i++) stack[i] = varargs.arg(i + 1);

		return setupCall(state, function, p.is_vararg != 0 ? varargs.subargs(p.numparams + 1) : Constants.NONE, stack);
	}

	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, Varargs varargs, LuaValue[] stack) throws LuaError {
		Prototype p = function.p;
		Upvalue[] upvalues = p.p.length > 0 ? new Upvalue[stack.length] : null;
		if (p.is_vararg >= VARARG_NEEDSARG) stack[p.numparams] = new LuaTable(varargs);

		DebugFrame frame = state.debug.onCall(DebugHandler.getDebugState(state), function, varargs, stack, upvalues);
		frame.top = 0;
		frame.extras = Constants.NONE;
		return frame;
	}

	static Varargs execute(LuaState state, LuaInterpretedFunction function, DebugFrame di) throws LuaError {
		// debug wants args to this function
		DebugState ds = DebugHandler.getDebugState(state);
		DebugHandler handler = state.debug;

		// Fetch all info from the function
		Prototype p = function.p;
		Upvalue[] upvalues = function.upvalues;
		int[] code = p.code;
		LuaValue[] k = p.k;

		// And from the debug info
		int pc = di.pc, top = di.top;
		Varargs v = di.extras;
		LuaValue[] stack = di.stack;
		Upvalue[] openups = di.stackUpvalues;
		Varargs varargs = di.varargs;

		// If we're an initial call, then ensure the PC is actually at 0.
		if (pc == -1) pc = 0;

		// process instructions
		try {
			while (true) {
				handler.onInstruction(ds, di, pc, v, top);

				// pull out instruction
				int i = code[pc++];
				int a = ((i >> POS_A) & MAXARG_A);

				// process the op code
				switch (((i >> POS_OP) & MASK_OP)) {
					case OP_MOVE: // A B: R(A):= R(B)
						stack[a] = stack[(i >>> POS_B) & MAXARG_B];
						break;

					case OP_LOADK: // A Bx: R(A):= Kst(Bx)
						stack[a] = k[(i >>> POS_Bx) & MAXARG_Bx];
						break;

					case OP_LOADBOOL: // A B C: R(A):= (Bool)B: if (C) pc++
						stack[a] = ((i >>> POS_B) & MAXARG_B) != 0 ? Constants.TRUE : Constants.FALSE;
						if (((i >>> POS_C) & MAXARG_C) != 0) pc++; // skip next instruction (if C)
						break;

					case OP_LOADNIL: { // A B: R(A):= ...:= R(B):= nil
						int b = ((i >>> POS_B) & MAXARG_B);
						do {
							stack[b--] = Constants.NIL;
						} while (b >= a);
						break;
					}

					case OP_GETUPVAL: // A B: R(A):= UpValue[B]
						stack[a] = upvalues[((i >>> POS_B) & MAXARG_B)].getValue();
						break;

					case OP_GETGLOBAL: // A Bx	R(A):= Gbl[Kst(Bx)]
						stack[a] = function.env.get(state, k[(i >>> POS_Bx) & MAXARG_Bx]);
						break;

					case OP_GETTABLE: { // A B C: R(A):= R(B)[RK(C)]
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >>> POS_C) & MAXARG_C;
						stack[a] = OperationHelper.getTable(state, stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b);
						break;
					}

					case OP_SETGLOBAL: // A Bx: Gbl[Kst(Bx)]:= R(A)
						function.env.set(state, k[(i >>> POS_Bx) & MAXARG_Bx], stack[a]);
						break;

					case OP_SETUPVAL: // A B: UpValue[B]:= R(A)
						upvalues[(i >>> POS_B) & MAXARG_B].setValue(stack[a]);
						break;

					case OP_SETTABLE: { // A B C: R(A)[RK(B)]:= RK(C)
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >>> POS_C) & MAXARG_C;
						OperationHelper.setTable(state, stack[a], b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], a);
						break;
					}

					case OP_NEWTABLE: // A B C: R(A):= {} (size = B,C)
						stack[a] = new LuaTable((i >>> POS_B) & MAXARG_B, (i >>> POS_C) & MAXARG_C);
						break;

					case OP_SELF: { // A B C: R(A+1):= R(B): R(A):= R(B)[RK(C)]
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						LuaValue o = stack[a + 1] = stack[b];
						stack[a] = OperationHelper.getTable(state, o, c > 0xff ? k[c & 0x0ff] : stack[c], b);
						break;
					}

					case OP_ADD: { // A B C: R(A):= RK(B) + RK(C)
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						stack[a] = OperationHelper.add(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
						break;
					}

					case OP_SUB: { // A B C: R(A):= RK(B) - RK(C)
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						stack[a] = OperationHelper.sub(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
						break;
					}

					case OP_MUL: { // A B C: R(A):= RK(B) * RK(C)
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						stack[a] = OperationHelper.mul(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
						break;
					}

					case OP_DIV: { // A B C: R(A):= RK(B) / RK(C)
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						stack[a] = OperationHelper.div(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
						break;
					}

					case OP_MOD: { // A B C: R(A):= RK(B) % RK(C)
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						stack[a] = OperationHelper.mod(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
						break;
					}

					case OP_POW: { // A B C: R(A):= RK(B) ^ RK(C)
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						stack[a] = OperationHelper.pow(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
						break;
					}

					case OP_UNM: { // A B: R(A):= -R(B)
						int b = (i >>> POS_B) & MAXARG_B;
						stack[a] = OperationHelper.neg(state, stack[b], b);
						break;
					}

					case OP_NOT: // A B: R(A):= not R(B)
						stack[a] = stack[(i >>> POS_B) & MAXARG_B].toBoolean() ? FALSE : TRUE;
						break;

					case OP_LEN: { // A B: R(A):= length of R(B)
						int b = (i >>> POS_B) & MAXARG_B;
						stack[a] = OperationHelper.length(state, stack[b], b);
						break;
					}

					case OP_CONCAT: { // A B C: R(A):= R(B).. ... ..R(C)
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;

						top = c + 1;
						concat(state, stack, top, c - b + 1);
						stack[a] = stack[b];
						top = b;
						break;
					}

					case OP_JMP: // sBx: pc+=sBx
						pc += ((i >>> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
						break;

					case OP_EQ: { // A B C: if ((RK(B) == RK(C)) ~= A) then pc++
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						if (OperationHelper.eq(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c]) == (a != 0)) {
							// We assume the next instruction is a jump and read the branch from there.
							pc += ((code[pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
						}
						pc++;
						break;
					}

					case OP_LT: { // A B C: if ((RK(B) <  RK(C)) ~= A) then pc++
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						if (OperationHelper.lt(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c]) == (a != 0)) {
							pc += ((code[pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
						}
						pc++;
						break;
					}

					case OP_LE: { // A B C: if ((RK(B) <= RK(C)) ~= A) then pc++
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						if (OperationHelper.le(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c]) == (a != 0)) {
							pc += ((code[pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
						}
						pc++;
						break;
					}

					case OP_TEST: // A C: if not (R(A) <=> C) then pc++
						if (stack[a].toBoolean() == (((i >> POS_C) & MAXARG_C) != 0)) {
							pc += ((code[pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
						}
						pc++;
						break;

					case OP_TESTSET: { // A B C: if (R(B) <=> C) then R(A):= R(B) else pc++
						/* note: doc appears to be reversed */
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						LuaValue val = stack[b];
						if (val.toBoolean() == (c != 0)) {
							stack[a] = val;
							pc += ((code[pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
						}
						pc++;
						break;
					}

					case OP_CALL: // A B C: R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1)) */
						switch (i & (MASK_B | MASK_C)) {
							case (1 << POS_B) | (0 << POS_C):
								v = OperationHelper.invoke(state, stack[a], Constants.NONE, a);
								top = a + v.count();
								break;
							case (2 << POS_B) | (0 << POS_C):
								v = OperationHelper.invoke(state, stack[a], stack[a + 1], a);
								top = a + v.count();
								break;
							case (1 << POS_B) | (1 << POS_C):
								OperationHelper.call(state, stack[a], a);
								break;
							case (2 << POS_B) | (1 << POS_C):
								OperationHelper.call(state, stack[a], stack[a + 1], a);
								break;
							case (3 << POS_B) | (1 << POS_C):
								OperationHelper.call(state, stack[a], stack[a + 1], stack[a + 2], a);
								break;
							case (4 << POS_B) | (1 << POS_C):
								OperationHelper.call(state, stack[a], stack[a + 1], stack[a + 2], stack[a + 3], a);
								break;
							case (1 << POS_B) | (2 << POS_C):
								stack[a] = OperationHelper.call(state, stack[a], a);
								break;
							case (2 << POS_B) | (2 << POS_C):
								stack[a] = OperationHelper.call(state, stack[a], stack[a + 1], a);
								break;
							case (3 << POS_B) | (2 << POS_C):
								stack[a] = OperationHelper.call(state, stack[a], stack[a + 1], stack[a + 2], a);
								break;
							case (4 << POS_B) | (2 << POS_C):
								stack[a] = OperationHelper.call(state, stack[a], stack[a + 1], stack[a + 2], stack[a + 3], a);
								break;
							default: {
								int b = (i >>> POS_B) & MAXARG_B;
								int c = (i >> POS_C) & MAXARG_C;
								v = b > 0 ?
									ValueFactory.varargsOf(stack, a + 1, b - 1) : // exact arg count
									ValueFactory.varargsOf(stack, a + 1, top - v.count() - (a + 1), v); // from prev top
								v = OperationHelper.invoke(state, stack[a], v, a);
								if (c > 0) {
									while (--c > 0) {
										stack[a + c - 1] = v.arg(c);
									}
									v = Constants.NONE; // TODO: necessary?
								} else {
									top = a + v.count();
								}
								break;
							}
						}
						break;

					case OP_TAILCALL: // A B C: return R(A)(R(A+1), ... ,R(A+B-1))
						switch (i & MASK_B) {
							case (1 << POS_B):
								return new TailcallVarargs(stack[a], Constants.NONE);
							case (2 << POS_B):
								return new TailcallVarargs(stack[a], stack[a + 1]);
							case (3 << POS_B):
								return new TailcallVarargs(stack[a], ValueFactory.varargsOf(stack[a + 1], stack[a + 2]));
							case (4 << POS_B):
								return new TailcallVarargs(stack[a], ValueFactory.varargsOf(stack[a + 1], stack[a + 2], stack[a + 3]));
							default: {
								int b = (i >>> POS_B) & MAXARG_B;
								v = b > 0 ?
									ValueFactory.varargsOf(stack, a + 1, b - 1) : // exact arg count
									ValueFactory.varargsOf(stack, a + 1, top - v.count() - (a + 1), v); // from prev top
								return new TailcallVarargs(stack[a], v);
							}
						}

					case OP_RETURN: { // A B: return R(A), ... ,R(A+B-2) (see note)
						int b = (i >>> POS_B) & MAXARG_B;
						switch (b) {
							case 0:
								return ValueFactory.varargsOf(stack, a, top - v.count() - a, v);
							case 1:
								return Constants.NONE;
							case 2:
								return stack[a];
							default:
								return ValueFactory.varargsOf(stack, a, b - 1);
						}
					}

					case OP_FORLOOP: { // A sBx: R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }
						LuaValue limit = stack[a + 1];
						LuaValue step = stack[a + 2];
						LuaValue idx = OperationHelper.add(state, step, stack[a]);
						if (OperationHelper.lt(state, Constants.ZERO, step) ? OperationHelper.le(state, idx, limit) : OperationHelper.le(state, limit, idx)) {
							stack[a] = idx;
							stack[a + 3] = idx;
							pc += ((i >>> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
						}
					}
					break;

					case OP_FORPREP: { // A sBx: R(A)-=R(A+2): pc+=sBx
						LuaValue init = stack[a].checkNumber("'for' initial value must be a number");
						LuaValue limit = stack[a + 1].checkNumber("'for' limit must be a number");
						LuaValue step = stack[a + 2].checkNumber("'for' step must be a number");
						stack[a] = OperationHelper.sub(state, init, step);
						stack[a + 1] = limit;
						stack[a + 2] = step;
						pc += ((i >>> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
					}
					break;

					case OP_TFORLOOP: {
						/*
							A C R(A+3), ... ,R(A+2+C):= R(A)(R(A+1),
							R(A+2)): if R(A+3) ~= nil then R(A+2)=R(A+3)
							else pc++
						*/
						// TODO: stack call on for loop body, such as:   stack[a].call(ci);
						v = OperationHelper.invoke(state, stack[a], ValueFactory.varargsOf(stack[a + 1], stack[a + 2]), a);
						LuaValue val = v.first();
						if (val.isNil()) {
							pc++;
						} else {
							stack[a + 2] = stack[a + 3] = val;
							for (int c = (i >> POS_C) & MAXARG_C; c > 1; --c) {
								stack[a + 2 + c] = v.arg(c);
							}
							v = Constants.NONE; // todo: necessary?
						}
						break;
					}

					case OP_SETLIST: { // A B C: R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >> POS_C) & MAXARG_C;
						if (c == 0) c = code[pc++];

						int offset = (c - 1) * LFIELDS_PER_FLUSH;
						LuaTable tbl = stack[a].checkTable();
						if (b == 0) {
							b = top - a - 1;
							int m = b - v.count();
							int j = 1;
							for (; j <= m; j++) {
								tbl.rawset(offset + j, stack[a + j]);
							}
							for (; j <= b; j++) {
								tbl.rawset(offset + j, v.arg(j - m));
							}
						} else {
							tbl.presize(offset + b);
							for (int j = 1; j <= b; j++) {
								tbl.rawset(offset + j, stack[a + j]);
							}
						}
						break;
					}

					case OP_CLOSE: { // A : close all variables in the stack up to (>=) R(A)
						for (int x = openups.length; --x >= a; ) {
							Upvalue upvalue = openups[x];
							if (upvalue != null) {
								upvalue.close();
								openups[x] = null;
							}
						}
						break;
					}

					case OP_CLOSURE: { // A Bx: R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n))
						Prototype newp = p.p[(i >>> POS_Bx) & MAXARG_Bx];
						LuaInterpretedFunction newcl = new LuaInterpretedFunction(newp, function.env);
						for (int j = 0, nup = newp.nups; j < nup; ++j) {
							i = code[pc++];
							int b = (i >>> POS_B) & MAXARG_B;
							newcl.upvalues[j] = (i & 4) != 0
								? upvalues[b] // OP_GETUPVAL
								: openups[b] != null ? openups[b] : (openups[b] = new Upvalue(stack, b)); // OP_MOVE
						}
						stack[a] = newcl;
						break;
					}

					case OP_VARARG: { // A B: R(A), R(A+1), ..., R(A+B-1) = vararg
						int b = (i >>> POS_B) & MAXARG_B;
						if (b == 0) {
							top = a + varargs.count();
							v = varargs;
						} else {
							for (int j = 1; j < b; ++j) {
								stack[a + j - 1] = varargs.arg(j);
							}
						}
					}
				}
			}
		} catch (LuaError le) {
			throw le.fillTraceback(state);
		} catch (Exception e) {
			throw new LuaError(e).fillTraceback(state);
		} finally {
			handler.onReturn(ds);
			if (openups != null) {
				for (int u = openups.length; --u >= 0; ) {
					if (openups[u] != null) {
						openups[u].close();
					}
				}
			}
		}
	}

	private static void concat(LuaState state, LuaValue[] stack, int top, int total) throws LuaError {
		do {
			LuaValue left = stack[top - 2];
			LuaValue right = stack[top - 1];
			LuaString lString, rString;
			int n = 2;
			if (!left.isString() || !right.isString()) {
				// If one of these isn't convertible to a string then use the metamethod
				stack[top - 2] = OperationHelper.concat(state, left, right, top - 2, top - 1);
			} else if ((rString = right.checkLuaString()).length == 0) {
				stack[top - 2] = left.checkLuaString();
			} else if ((lString = left.checkLuaString()).length == 0) {
				stack[top - 2] = rString;
			} else {
				int length = rString.length + lString.length;
				stack[top - 2] = lString;
				stack[top - 1] = rString;

				for (; n < total; n++) {
					LuaValue value = stack[top - n - 1];
					if (!value.isString()) break;

					LuaString string = value.checkLuaString();

					// Ensure we don't get a string which is too long
					if (string.length > Integer.MAX_VALUE - length) throw new LuaError("string length overflow");

					// Otherwise increment the length and store this converted string
					stack[top - n - 1] = string;
					length += string.length;
				}

				byte[] buffer = new byte[length];
				length = 0;
				for (int j = n; j > 0; j--) {
					LuaString string = (LuaString) stack[top - j];
					System.arraycopy(string.bytes, string.offset, buffer, length, string.length);
					length += string.length;
				}

				stack[top - n] = LuaString.valueOf(buffer);
			}

			// Got "n" strings and created one new one
			total -= n - 1;
			top -= n - 1;
		} while (total > 1);
	}
}
