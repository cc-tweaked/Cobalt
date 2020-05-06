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
package org.squiddev.cobalt.function;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;

import java.util.function.Function;

import static org.squiddev.cobalt.Constants.FALSE;
import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.Constants.NILS;
import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.Constants.TRUE;
import static org.squiddev.cobalt.Lua.*;
import static org.squiddev.cobalt.LuaDouble.valueOf;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_FRESH;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_LEQ;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_TAIL;

/**
 * The main interpreter for {@link LuaInterpretedFunction}s.
 */
public final class LuaInterpreter {
	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(NILS, 0, stack, 0, p.maxstacksize);

		return setupCall(state, function, NONE, stack, flags);
	}

	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, LuaValue arg, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(NILS, 0, stack, 0, p.maxstacksize);

		switch (p.numparams) {
			case 0:
				return setupCall(state, function, arg, stack, flags);

			default:
				stack[0] = arg;
				return setupCall(state, function, NONE, stack, flags);
		}
	}

	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, LuaValue arg1, LuaValue arg2, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(NILS, 0, stack, 0, p.maxstacksize);

		switch (p.numparams) {
			case 0:
				return setupCall(state, function, p.is_vararg != 0 ? ValueFactory.varargsOf(arg1, arg2) : NONE, stack, flags);

			case 1:
				stack[0] = arg1;
				return setupCall(state, function, arg2, stack, flags);

			default:
				stack[0] = arg1;
				stack[1] = arg2;
				return setupCall(state, function, NONE, stack, flags);
		}
	}

	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, LuaValue arg1, LuaValue arg2, LuaValue arg3, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(NILS, 0, stack, 0, p.maxstacksize);

		switch (p.numparams) {
			case 0:
				return setupCall(state, function, p.is_vararg != 0 ? ValueFactory.varargsOf(arg1, arg2, arg3) : NONE, stack, flags);

			case 1:
				stack[0] = arg1;
				return setupCall(state, function, p.is_vararg != 0 ? ValueFactory.varargsOf(arg2, arg3) : NONE, stack, flags);

			case 2:
				stack[0] = arg1;
				stack[1] = arg2;
				return setupCall(state, function, arg3, stack, flags);

			default:
				stack[0] = arg1;
				stack[1] = arg2;
				stack[2] = arg3;
				return setupCall(state, function, NONE, stack, flags);
		}
	}

	static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, Varargs varargs, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(NILS, 0, stack, 0, p.maxstacksize);
		for (int i = 0; i < p.numparams; i++) stack[i] = varargs.arg(i + 1);

		return setupCall(state, function, p.is_vararg != 0 ? varargs.subargs(p.numparams + 1) : NONE, stack, flags);
	}

	private static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, LuaValue[] args, int argStart, int argSize, Varargs varargs, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(NILS, 0, stack, 0, p.maxstacksize);

		varargs = ValueFactory.varargsOf(args, argStart, argSize, varargs);
		for (int i = 0; i < p.numparams; i++) stack[i] = varargs.arg(i + 1);

		return setupCall(state, function, p.is_vararg != 0 ? varargs.subargs(p.numparams + 1) : NONE, stack, flags);
	}

	private static DebugFrame setupCall(LuaState state, LuaInterpretedFunction function, Varargs varargs, LuaValue[] stack, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		Upvalue[] upvalues = p.p.length > 0 ? new Upvalue[stack.length] : null;
		if (p.is_vararg >= VARARG_NEEDSARG) stack[p.numparams] = new LuaTable(varargs);

		DebugState ds = DebugHandler.getDebugState(state);
		DebugFrame di = (flags & FLAG_FRESH) != 0 ? ds.pushJavaInfo() : ds.pushInfo();
		di.setFunction(function, varargs.asImmutable(), stack, upvalues);
		di.flags |= flags;
		di.extras = NONE;
		di.pc = 0;

		if (!ds.inhook && ds.hookcall) {
			// Pretend we are at the first instruction for the hook.
			ds.hookCall(di);
		}

		di.top = 0;
		return di;
	}

	static Varargs execute(LuaState state, DebugFrame di, LuaInterpretedFunction function) throws LuaError, UnwindThrowable {
		if (true)
			return partialEval(state, di, function);
		else
			return interpret(state, di, function);
	}

	static Varargs interpret(final LuaState state, DebugFrame di, LuaInterpretedFunction function) throws LuaError, UnwindThrowable {
		final DebugState ds = DebugHandler.getDebugState(state);
		final DebugHandler handler = state.debug;

		newFrame:
		while (true) {
			// Fetch all info from the function
			final Prototype p = function.p;
			final Upvalue[] upvalues = function.upvalues;
			final int[] code = p.code;
			final LuaValue[] k = p.k;

			// And from the debug info
			final LuaValue[] stack = di.stack;
			final Upvalue[] openups = di.stackUpvalues;
			final Varargs varargs = di.varargs;

			int pc = di.pc;

			// process instructions
			while (true) {
				handler.onInstruction(ds, di, pc);

				// pull out instruction
				int i = code[pc++];
				int a = ((i >> POS_A) & MAXARG_A);

				// process the instruction
				switch (((i >> POS_OP) & MAX_OP)) {
					case OP_MOVE: // A B: R(A):= R(B)
						stack[a] = stack[(i >>> POS_B) & MAXARG_B];
						break;

					case OP_LOADK: // A Bx: R(A):= Kst(Bx)
						stack[a] = k[(i >>> POS_Bx) & MAXARG_Bx];
						break;

					case OP_LOADBOOL: // A B C: R(A):= (Bool)B: if (C) pc++
						stack[a] = ((i >>> POS_B) & MAXARG_B) != 0 ? TRUE : FALSE;
						if (((i >>> POS_C) & MAXARG_C) != 0) pc++; // skip next instruction (if C)
						break;

					case OP_LOADNIL: { // A B: R(A):= ...:= R(B):= nil
						int b = ((i >>> POS_B) & MAXARG_B);
						do {
							stack[b--] = NIL;
						} while (b >= a);
						break;
					}

					case OP_GETUPVAL: // A B: R(A):= UpValue[B]
						stack[a] = upvalues[((i >>> POS_B) & MAXARG_B)].getValue();
						break;

					case OP_GETGLOBAL: // A Bx	R(A):= Gbl[Kst(Bx)]
						stack[a] = OperationHelper.getTable(state, function.env, k[(i >>> POS_Bx) & MAXARG_Bx]);
						break;

					case OP_GETTABLE: { // A B C: R(A):= R(B)[RK(C)]
						int b = (i >>> POS_B) & MAXARG_B;
						int c = (i >>> POS_C) & MAXARG_C;
						stack[a] = OperationHelper.getTable(state, stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b);
						break;
					}

					case OP_SETGLOBAL: // A Bx: Gbl[Kst(Bx)]:= R(A)
						OperationHelper.setTable(state, function.env, k[(i >>> POS_Bx) & MAXARG_Bx], stack[a]);
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
						stack[a] = OperationHelper.neg(state, b > 0xff ? k[b & 0x0ff] : stack[b], b);
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

						di.top = c + 1;
						concat(state, di, stack, di.top, c - b + 1);
						stack[a] = stack[b];
						di.top = b;
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

					case OP_CALL: { // A B C: R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1)) */
						int b = (i >>> POS_B) & MAXARG_B;
						int c = ((i >> POS_C) & MAXARG_C);

						LuaValue val = stack[a];
						if (val instanceof LuaInterpretedFunction) {
							function = (LuaInterpretedFunction) val;
							switch (b) {
								case 1:
									di = setupCall(state, function, 0);
									break;
								case 2:
									di = setupCall(state, function, stack[a + 1], 0);
									break;
								case 3:
									di = setupCall(state, function, stack[a + 1], stack[a + 2], 0);
									break;
								case 4:
									di = setupCall(state, function, stack[a + 1], stack[a + 2], stack[a + 3], 0);
									break;
								default:
									di = b > 0
										? setupCall(state, function, stack, a + 1, b - 1, NONE, 0) // exact arg count
										: setupCall(state, function, stack, a + 1, di.top - di.extras.count() - (a + 1), di.extras, 0); // from prev top
							}

							continue newFrame;
						}

						switch (i & (MASK_B | MASK_C)) {
							case (1 << POS_B) | (0 << POS_C): {
								Varargs v = di.extras = OperationHelper.invoke(state, val, NONE, a);
								di.top = a + v.count();
								break;
							}
							case (2 << POS_B) | (0 << POS_C): {
								Varargs v = di.extras = OperationHelper.invoke(state, val, stack[a + 1], a);
								di.top = a + v.count();
								break;
							}
							case (1 << POS_B) | (1 << POS_C):
								OperationHelper.call(state, val, a);
								break;
							case (2 << POS_B) | (1 << POS_C):
								OperationHelper.call(state, val, stack[a + 1], a);
								break;
							case (3 << POS_B) | (1 << POS_C):
								OperationHelper.call(state, val, stack[a + 1], stack[a + 2], a);
								break;
							case (4 << POS_B) | (1 << POS_C):
								OperationHelper.call(state, val, stack[a + 1], stack[a + 2], stack[a + 3], a);
								break;
							case (1 << POS_B) | (2 << POS_C):
								stack[a] = OperationHelper.call(state, val, a);
								break;
							case (2 << POS_B) | (2 << POS_C):
								stack[a] = OperationHelper.call(state, val, stack[a + 1], a);
								break;
							case (3 << POS_B) | (2 << POS_C):
								stack[a] = OperationHelper.call(state, val, stack[a + 1], stack[a + 2], a);
								break;
							case (4 << POS_B) | (2 << POS_C):
								stack[a] = OperationHelper.call(state, val, stack[a + 1], stack[a + 2], stack[a + 3], a);
								break;
							default: {
								Varargs args = b > 0 ?
									ValueFactory.varargsOf(stack, a + 1, b - 1) : // exact arg count
									ValueFactory.varargsOf(stack, a + 1, di.top - di.extras.count() - (a + 1), di.extras); // from prev top
								Varargs v = OperationHelper.invoke(state, val, args.asImmutable(), a);
								if (c > 0) {
									while (--c > 0) stack[a + c - 1] = v.arg(c);
									v = NONE;
								} else {
									di.top = a + v.count();
									di.extras = v;
								}
								break;
							}
						}
						break;
					}

					case OP_TAILCALL: { // A B C: return R(A)(R(A+1), ... ,R(A+B-1))
						int b = (i >>> POS_B) & MAXARG_B;

						LuaValue val = stack[a];
						Varargs args;
						switch (b) {
							case 1:
								args = NONE;
								break;
							case 2:
								args = stack[a + 1];
								break;
							default: {
								Varargs v = di.extras;
								args = b > 0 ?
									ValueFactory.varargsOf(stack, a + 1, b - 1) : // exact arg count
									ValueFactory.varargsOf(stack, a + 1, di.top - v.count() - (a + 1), v); // from prev top
							}
						}

						LuaFunction functionVal;
						if (val.isFunction()) {
							functionVal = (LuaFunction) val;
						} else {
							LuaValue meta = val.metatag(state, Constants.CALL);
							if (!meta.isFunction()) throw ErrorFactory.operandError(state, val, "call", a);

							functionVal = (LuaFunction) meta;
							args = ValueFactory.varargsOf(val, args);
						}

						if (functionVal instanceof LuaInterpretedFunction) {
							int flags = di.flags;
							closeAll(openups);
							ds.popInfo();

							// Replace the current frame with a new one.
							function = (LuaInterpretedFunction) functionVal;
							di = setupCall(state, function, args, (flags & FLAG_FRESH) | FLAG_TAIL);

							continue newFrame;
						} else {
							Varargs v = functionVal.invoke(state, args.asImmutable());
							di.top = a + v.count();
							di.extras = v;
							break;
						}
					}

					case OP_RETURN: { // A B: return R(A), ... ,R(A+B-2) (see note)
						int b = (i >>> POS_B) & MAXARG_B;

						int flags = di.flags, top = di.top;
						Varargs v = di.extras;
						closeAll(openups);
						handler.onReturn(ds, di);

						Varargs ret;
						switch (b) {
							case 0:
								ret = ValueFactory.varargsOf(stack, a, top - v.count() - a, v).asImmutable();
								break;
							case 1:
								ret = NONE;
								break;
							case 2:
								ret = stack[a];
								break;
							default:
								ret = ValueFactory.varargsOf(stack, a, b - 1).asImmutable();
								break;
						}

						if ((flags & FLAG_FRESH) != 0) {
							// If we're a fresh invocation then return to the parent.
							return ret;
						} else {
							di = ds.getStackUnsafe();
							function = (LuaInterpretedFunction) di.closure;
							resume(state, di, function, ret);
							continue newFrame;
						}
					}

					case OP_FORLOOP: { // A sBx: R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }
						double limit = stack[a + 1].checkDouble();
						double step = stack[a + 2].checkDouble();
						double value = stack[a].checkDouble();
						double idx = step + value;
						if (0 < step ? idx <= limit : limit <= idx) {
							stack[a + 3] = stack[a] = valueOf(idx);
							pc += ((i >>> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
						}
					}
					break;

					case OP_FORPREP: { // A sBx: R(A)-=R(A+2): pc+=sBx
						LuaNumber init = stack[a].checkNumber("'for' initial value must be a number");
						LuaNumber limit = stack[a + 1].checkNumber("'for' limit must be a number");
						LuaNumber step = stack[a + 2].checkNumber("'for' step must be a number");
						stack[a] = valueOf(init.toDouble() - step.toDouble());
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
						Varargs v = di.extras = OperationHelper.invoke(state, stack[a], ValueFactory.varargsOf(stack[a + 1], stack[a + 2]), a);
						LuaValue val = v.first();
						if (val.isNil()) {
							pc++;
						} else {
							stack[a + 2] = stack[a + 3] = val;
							for (int c = (i >> POS_C) & MAXARG_C; c > 1; --c) {
								stack[a + 2 + c] = v.arg(c);
							}
							di.extras = NONE;
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
							b = di.top - a - 1;
							int m = b - di.extras.count();
							int j = 1;
							for (; j <= m; j++) {
								tbl.rawset(offset + j, stack[a + j]);
							}
							for (; j <= b; j++) {
								tbl.rawset(offset + j, di.extras.arg(j - m));
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
							di.top = a + varargs.count();
							di.extras = varargs;
						} else {
							for (int j = 1; j < b; ++j) {
								stack[a + j - 1] = varargs.arg(j);
							}
						}
					}
				}
			}
		}
	}

	private static Function<UnwindableRunnable, UnwindableCallable<Varargs>> continuation(LuaState state, DebugFrame di, LuaInterpretedFunction function) {
		return f -> {
			final UnwindableCallable<Varargs> callable = () -> {
				f.run();
				//noinspection ReturnOfNull
				return null;
			};

			function.p.partiallyEvaluated.set(di.pc - 1);
			function.p.compiledInstrs[di.pc - 1] = callable;

			return callable;
		};
	}

	private static Function<UnwindableCallable<Varargs>, UnwindableCallable<Varargs>> rawCont(LuaState state, DebugFrame di, LuaInterpretedFunction function) {
		return f -> {
			function.p.partiallyEvaluated.set(di.pc - 1);
			function.p.compiledInstrs[di.pc - 1] = f;

			return f;
		};
	}

	static Varargs partialEval(final LuaState state, final DebugFrame di, final LuaInterpretedFunction function) throws LuaError, UnwindThrowable {
		Varargs v;
		//noinspection StatementWithEmptyBody
		while ((v = partialEvalStep(state, di, function).call()) == null);
		return v;
	}

	static UnwindableCallable<Varargs> partialEvalStep(final LuaState state, final DebugFrame di, final LuaInterpretedFunction function) throws LuaError, UnwindThrowable {
		final DebugState ds = DebugHandler.getDebugState(state);
		final DebugHandler handler = state.debug;

		// Fetch all info from the function
		final Prototype p = function.p;
		final Upvalue[] upvalues = function.upvalues;
		final int[] code = p.code;
		final LuaValue[] k = p.k;

		// And from the debug info
		final LuaValue[] stack = di.stack;
		final Upvalue[] openups = di.stackUpvalues;
		final Varargs varargs = di.varargs;

		handler.onInstruction(ds, di, di.pc);
		final int pc = di.pc++;
//		System.out.println("pc=" + pc + "; di.pc=" + di.pc);

		if (p.partiallyEvaluated.get(pc)) {
//			System.out.println("  cache hit");
			return p.compiledInstrs[pc];
		}

		// pull out instruction
		final int i = code[pc];
		final int a = (i >> POS_A) & MAXARG_A;

		final Function<UnwindableRunnable, UnwindableCallable<Varargs>> cont = continuation(state, di, function);
		final Function<UnwindableCallable<Varargs>, UnwindableCallable<Varargs>> raw = rawCont(state, di, function);

		// process the instruction
		switch (((i >> POS_OP) & MAX_OP)) {
			case OP_MOVE: { // A B: R(A):= R(B)
				final int b = (i >>> POS_B) & MAXARG_B;
				return cont.apply(() -> stack[a] = stack[b]);
			}

			case OP_LOADK: { // A Bx: R(A):= Kst(Bx)
				final LuaValue konst = k[(i >>> POS_Bx) & MAXARG_Bx];
				return cont.apply(() -> stack[a] = konst);
			}

			case OP_LOADBOOL: { // A B C: R(A):= (Bool)B: if (C) pc++
				LuaBoolean value = ((i >>> POS_B) & MAXARG_B) != 0 ? TRUE : FALSE;
				// skip next instruction (if C)
				boolean skip = ((i >>> POS_C) & MAXARG_C) != 0;

				return cont.apply(() -> {
					stack[a] = value;
					if (skip) di.pc++;
				});
			}

			case OP_LOADNIL: { // A B: R(A):= ...:= R(B):= nil
				final int init = ((i >>> POS_B) & MAXARG_B);
				return cont.apply(() -> {
					int b = init;
					do {
						stack[b--] = NIL;
					} while (b >= a);
				});
			}

			case OP_GETUPVAL: // A B: R(A):= UpValue[B]
				return cont.apply(() -> stack[a] = upvalues[((i >>> POS_B) & MAXARG_B)].getValue());

			case OP_GETGLOBAL: // A Bx	R(A):= Gbl[Kst(Bx)]
				return cont.apply(() -> stack[a] = OperationHelper.getTable(state, function.env, k[(i >>> POS_Bx) & MAXARG_Bx]));

			case OP_GETTABLE: { // A B C: R(A):= R(B)[RK(C)]
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >>> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					// TODO lift conditional out of λ
					stack[a] = OperationHelper.getTable(state, stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b);
				});
			}

			case OP_SETGLOBAL: { // A Bx: Gbl[Kst(Bx)]:= R(A)
				final LuaValue konst = k[(i >>> POS_Bx) & MAXARG_Bx];
				return cont.apply(() -> OperationHelper.setTable(state, function.env, konst, stack[a]));
			}

			case OP_SETUPVAL: // A B: UpValue[B]:= R(A)
				// TODO? can we prefetch the upvalue right away?
				return cont.apply(() -> upvalues[(i >>> POS_B) & MAXARG_B].setValue(stack[a]));

			case OP_SETTABLE: { // A B C: R(A)[RK(B)]:= RK(C)
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >>> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					// TODO lift conditionals out of λ
					OperationHelper.setTable(state, stack[a], b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], a);
				});
			}

			case OP_NEWTABLE: // A B C: R(A):= {} (size = B,C)
				return cont.apply(() -> stack[a] = new LuaTable((i >>> POS_B) & MAXARG_B, (i >>> POS_C) & MAXARG_C));

			case OP_SELF: { // A B C: R(A+1):= R(B): R(A):= R(B)[RK(C)]
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					LuaValue o = stack[a + 1] = stack[b];
					// TODO lift conditional out of λ
					stack[a] = OperationHelper.getTable(state, o, c > 0xff ? k[c & 0x0ff] : stack[c], b);
				});
			}

			case OP_ADD: { // A B C: R(A):= RK(B) + RK(C)
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					// TODO lift conditionals out of λ
					stack[a] = OperationHelper.add(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
				});
			}

			case OP_SUB: { // A B C: R(A):= RK(B) - RK(C)
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					// TODO lift conditionals out of λ
					stack[a] = OperationHelper.sub(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
				});
			}

			case OP_MUL: { // A B C: R(A):= RK(B) * RK(C)
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					// TODO lift conditionals out of λ
					stack[a] = OperationHelper.mul(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
				});
			}

			case OP_DIV: { // A B C: R(A):= RK(B) / RK(C)
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					// TODO lift conditionals out of λ
					stack[a] = OperationHelper.div(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
				});
			}

			case OP_MOD: { // A B C: R(A):= RK(B) % RK(C)
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					// TODO lift conditionals out of λ
					stack[a] = OperationHelper.mod(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
				});
			}

			case OP_POW: { // A B C: R(A):= RK(B) ^ RK(C)
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					// TODO lift conditionals out of λ
					stack[a] = OperationHelper.pow(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c], b, c);
				});
			}

			case OP_UNM: { // A B: R(A):= -R(B)
				final int b = (i >>> POS_B) & MAXARG_B;
				return cont.apply(() -> {
					// TODO lift conditional out of λ
					stack[a] = OperationHelper.neg(state, b > 0xff ? k[b & 0x0ff] : stack[b], b);
				});
			}

			case OP_NOT: { // A B: R(A):= not R(B)
				final int b = (i >>> POS_B) & MAXARG_B;
				return cont.apply(() -> stack[a] = stack[b].toBoolean() ? FALSE : TRUE);
			}

			case OP_LEN: { // A B: R(A):= length of R(B)
				final int b = (i >>> POS_B) & MAXARG_B;
				return cont.apply(() -> stack[a] = OperationHelper.length(state, stack[b], b));
			}

			case OP_CONCAT: { // A B C: R(A):= R(B).. ... ..R(C)
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;
				final int total = c - b + 1;

				return cont.apply(() -> {
					di.top = c + 1;
					concat(state, di, stack, di.top, total);
					stack[a] = stack[b];
					di.top = b;
				});
			}

			case OP_JMP: { // sBx: pc+=sBx
				final int offset = ((i >>> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
				return cont.apply(() -> di.pc += offset);
			}

			case OP_EQ: { // A B C: if ((RK(B) == RK(C)) ~= A) then pc++
				int b = (i >>> POS_B) & MAXARG_B;
				int c = (i >> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					// TODO lift conditionals out of λ
					// TODO precompute for constants
					if (OperationHelper.eq(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c]) == (a != 0)) {
						// We assume the next instruction is a jump and read the branch from there.
						di.pc += ((code[di.pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
					}
					di.pc++;
				});
			}

			case OP_LT: { // A B C: if ((RK(B) <  RK(C)) ~= A) then pc++
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;

				return cont.apply(() -> {
					// TODO lift conditionals out of λ
					if (OperationHelper.lt(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c]) == (a != 0)) {
						di.pc += ((code[di.pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
					}
					di.pc++;
				});
			}

			case OP_LE: { // A B C: if ((RK(B) <= RK(C)) ~= A) then pc++
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;

				return cont.apply(() -> {
					// TODO lift conditionals out of λ
					if (OperationHelper.le(state, b > 0xff ? k[b & 0x0ff] : stack[b], c > 0xff ? k[c & 0x0ff] : stack[c]) == (a != 0)) {
						di.pc += ((code[di.pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
					}
					di.pc++;
				});
			}

			case OP_TEST: { // A C: if not (R(A) <=> C) then pc++
				final boolean c = (i >> POS_C & MAXARG_C) != 0;
				return cont.apply(() -> {
					if (stack[a].toBoolean() == c) {
						di.pc += ((code[di.pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
					}
					di.pc++;
				});
			}

			case OP_TESTSET: { // A B C: if (R(B) <=> C) then R(A):= R(B) else pc++
				/* note: doc appears to be reversed */
				int b = (i >>> POS_B) & MAXARG_B;
				int c = (i >> POS_C) & MAXARG_C;
				boolean nonZeroC = c != 0;

				return cont.apply(() -> {
					LuaValue val = stack[b];
					if (val.toBoolean() == nonZeroC) {
						stack[a] = val;
						di.pc += ((code[di.pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
					}
					di.pc++;
				});
			}

			case OP_CALL: { // A B C: R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1)) */
				final int b = (i >>> POS_B) & MAXARG_B;
				final int c = (i >> POS_C) & MAXARG_C;

				return raw.apply(() -> {
					DebugFrame debugFrame = di;
					LuaInterpretedFunction fn;
					LuaValue val = stack[a];

					if (val instanceof LuaInterpretedFunction) {
						fn = (LuaInterpretedFunction) val;
						switch (b) {
							case 1:
								debugFrame = setupCall(state, fn, 0);
								break;
							case 2:
								debugFrame = setupCall(state, fn, stack[a + 1], 0);
								break;
							case 3:
								debugFrame = setupCall(state, fn, stack[a + 1], stack[a + 2], 0);
								break;
							case 4:
								debugFrame = setupCall(state, fn, stack[a + 1], stack[a + 2], stack[a + 3], 0);
								break;
							default:
								debugFrame = b > 0
										? setupCall(state, fn, stack, a + 1, b - 1, NONE, 0) // exact arg count
										: setupCall(state, fn, stack, a + 1, debugFrame.top - debugFrame.extras.count() - (a + 1), debugFrame.extras, 0); // from prev top
						}

						return partialEval(state, debugFrame, fn);
					}

					switch (i & (MASK_B | MASK_C)) {
						case (1 << POS_B) | (0 << POS_C): {
							Varargs v = debugFrame.extras = OperationHelper.invoke(state, val, NONE, a);
							debugFrame.top = a + v.count();
							break;
						}
						case (2 << POS_B) | (0 << POS_C): {
							Varargs v = debugFrame.extras = OperationHelper.invoke(state, val, stack[a + 1], a);
							debugFrame.top = a + v.count();
							break;
						}
						case (1 << POS_B) | (1 << POS_C):
							OperationHelper.call(state, val, a);
							break;
						case (2 << POS_B) | (1 << POS_C):
							OperationHelper.call(state, val, stack[a + 1], a);
							break;
						case (3 << POS_B) | (1 << POS_C):
							OperationHelper.call(state, val, stack[a + 1], stack[a + 2], a);
							break;
						case (4 << POS_B) | (1 << POS_C):
							OperationHelper.call(state, val, stack[a + 1], stack[a + 2], stack[a + 3], a);
							break;
						case (1 << POS_B) | (2 << POS_C):
							stack[a] = OperationHelper.call(state, val, a);
							break;
						case (2 << POS_B) | (2 << POS_C):
							stack[a] = OperationHelper.call(state, val, stack[a + 1], a);
							break;
						case (3 << POS_B) | (2 << POS_C):
							stack[a] = OperationHelper.call(state, val, stack[a + 1], stack[a + 2], a);
							break;
						case (4 << POS_B) | (2 << POS_C):
							stack[a] = OperationHelper.call(state, val, stack[a + 1], stack[a + 2], stack[a + 3], a);
							break;
						default: {
							Varargs args = b > 0 ?
									ValueFactory.varargsOf(stack, a + 1, b - 1) : // exact arg count
									ValueFactory.varargsOf(stack, a + 1, debugFrame.top - debugFrame.extras.count() - (a + 1), debugFrame.extras); // from prev top
							Varargs v = OperationHelper.invoke(state, val, args.asImmutable(), a);
							if (c > 0) {
								int cnt = c;
								while (--cnt > 0) stack[a + cnt - 1] = v.arg(cnt);
								v = NONE;
							} else {
								debugFrame.top = a + v.count();
								debugFrame.extras = v;
							}
							break;
						}
					}

					// a native call is over, continue executing this function
					return NONE; // FIXME why doesn't this work with null? (see partialEval)
				});
			}

			// TODO needs special handling, JVM doesn't have proper tail calls
			case OP_TAILCALL: { // A B C: return R(A)(R(A+1), ... ,R(A+B-1))
				final int b = (i >>> POS_B) & MAXARG_B;

				if (true) {
					throw new UnsupportedOperationException("tail calls are a to-do, soz");
				}

				return raw.apply(() -> {
					DebugFrame debugFrame = di;
					LuaInterpretedFunction fn = function;

					LuaValue val = stack[a];
					Varargs args;
					switch (b) {
						case 1:
							args = NONE;
							break;
						case 2:
							args = stack[a + 1];
							break;
						default: {
							Varargs v = debugFrame.extras;
							args = b > 0 ?
									ValueFactory.varargsOf(stack, a + 1, b - 1) : // exact arg count
									ValueFactory.varargsOf(stack, a + 1, debugFrame.top - v.count() - (a + 1), v); // from prev top
						}
					}

					LuaFunction functionVal;
					if (val.isFunction()) {
						functionVal = (LuaFunction) val;
					} else {
						LuaValue meta = val.metatag(state, Constants.CALL);
						if (!meta.isFunction()) throw ErrorFactory.operandError(state, val, "call", a);

						functionVal = (LuaFunction) meta;
						args = ValueFactory.varargsOf(val, args);
					}

					if (functionVal instanceof LuaInterpretedFunction) {
						int flags = debugFrame.flags;
						closeAll(openups);
						ds.popInfo();

						// Replace the current frame with a new one.
						fn = (LuaInterpretedFunction) functionVal;
						debugFrame = setupCall(state, fn, args, (flags & FLAG_FRESH) | FLAG_TAIL);

						partialEval(state, debugFrame, fn);
						// TODO
					} else {
						Varargs v = functionVal.invoke(state, args.asImmutable());
						debugFrame.top = a + v.count();
						debugFrame.extras = v;
						// TODO
					}

					return NONE;
				});
			}

			case OP_RETURN: { // A B: return R(A), ... ,R(A+B-2) (see note)
				final int b = (i >>> POS_B) & MAXARG_B;
				final int flags = di.flags, top = di.top;
				final boolean fresh = (flags & FLAG_FRESH) == 0;
				final Varargs v = di.extras;

				return raw.apply(() -> {
					closeAll(openups);
					handler.onReturn(ds, di);

					Varargs ret;
					// TODO lift conditional out of λ
					switch (b) {
						case 0:
							ret = ValueFactory.varargsOf(stack, a, top - v.count() - a, v).asImmutable();
							break;
						case 1:
							ret = NONE;
							break;
						case 2:
							ret = stack[a];
							break;
						default:
							ret = ValueFactory.varargsOf(stack, a, b - 1).asImmutable();
							break;
					}

					// TODO lift conditional out of λ
					if (!fresh) {
						// If we're a fresh invocation then return to the parent.
						return ret;
					} else {
						DebugFrame debugFrame = ds.getStackUnsafe();
						LuaInterpretedFunction fn = (LuaInterpretedFunction) debugFrame.closure;
						debugFrame.pc--; // FIXME this shouldn't be here but otherwise we have an off-by-one error in resume
						resume(state, debugFrame, fn, ret);
						return partialEval(state, debugFrame, fn);
					}
				});
			}

			case OP_FORLOOP: { // A sBx: R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }
				final int offset = ((i >>> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
				return cont.apply(() -> {
					double limit = stack[a + 1].checkDouble();
					double step = stack[a + 2].checkDouble();
					double value = stack[a].checkDouble();
					double idx = step + value;
					if (0 < step ? idx <= limit : limit <= idx) {
						stack[a + 3] = stack[a] = valueOf(idx);
						di.pc += offset;
					}
				});
			}

			case OP_FORPREP: { // A sBx: R(A)-=R(A+2): pc+=sBx
				final int offset = ((i >>> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
				return cont.apply(() -> {
					LuaNumber init = stack[a].checkNumber("'for' initial value must be a number");
					LuaNumber limit = stack[a + 1].checkNumber("'for' limit must be a number");
					LuaNumber step = stack[a + 2].checkNumber("'for' step must be a number");
					stack[a] = valueOf(init.toDouble() - step.toDouble());
					stack[a + 1] = limit;
					stack[a + 2] = step;
					di.pc += offset;
				});
			}

			case OP_TFORLOOP: {
							/*
								A C R(A+3), ... ,R(A+2+C):= R(A)(R(A+1),
								R(A+2)): if R(A+3) ~= nil then R(A+2)=R(A+3)
								else pc++
							*/
				final int initC = (i >> POS_C) & MAXARG_C;
				return cont.apply(() -> {
					Varargs v = di.extras = OperationHelper.invoke(state, stack[a], ValueFactory.varargsOf(stack[a + 1], stack[a + 2]), a);
					LuaValue val = v.first();
					if (val.isNil()) {
						di.pc++;
					} else {
						stack[a + 2] = stack[a + 3] = val;
						for (int c = initC; c > 1; --c) {
							stack[a + 2 + c] = v.arg(c);
						}
						di.extras = NONE;
					}
				});
			}

			case OP_SETLIST: { // A B C: R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
				final int _b = (i >>> POS_B) & MAXARG_B;
				final int _c = (i >> POS_C) & MAXARG_C;
				final int c;
				if (_c == 0) c = code[di.pc + 1]; else c = _c;
				final int offset = (c - 1) * LFIELDS_PER_FLUSH;

				return cont.apply(() -> {
					// TODO lift conditional out of λ
					if (_c == 0) di.pc++;
					LuaTable tbl = stack[a].checkTable();
					// TODO lift conditional out of λ
					if (_b == 0) {
						int b = di.top - a - 1;
						int m = b - di.extras.count();
						int j = 1;
						for (; j <= m; j++) {
							tbl.rawset(offset + j, stack[a + j]);
						}
						for (; j <= b; j++) {
							tbl.rawset(offset + j, di.extras.arg(j - m));
						}
					} else {
						tbl.presize(offset + _b);
						for (int j = 1; j <= _b; j++) {
							tbl.rawset(offset + j, stack[a + j]);
						}
					}
				});
			}

			case OP_CLOSE: // A : close all variables in the stack up to (>=) R(A)
				return cont.apply(() -> {
					for (int x = openups.length; --x >= a; ) {
						Upvalue upvalue = openups[x];
						if (upvalue != null) {
							upvalue.close();
							openups[x] = null;
						}
					}
				});

			case OP_CLOSURE: { // A Bx: R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n))
				Prototype newp = p.p[(i >>> POS_Bx) & MAXARG_Bx];
				return cont.apply(() -> {
					LuaInterpretedFunction newcl = new LuaInterpretedFunction(newp, function.env);
					for (int j = 0, nup = newp.nups; j < nup; ++j) {
						int instr = code[di.pc++];
						int b = (instr >>> POS_B) & MAXARG_B;
						newcl.upvalues[j] = (instr & 4) != 0
								? upvalues[b] // OP_GETUPVAL
								: openups[b] != null ? openups[b] : (openups[b] = new Upvalue(stack, b)); // OP_MOVE
					}
					stack[a] = newcl;
				});
			}

			case OP_VARARG: { // A B: R(A), R(A+1), ..., R(A+B-1) = vararg
				final int b = (i >>> POS_B) & MAXARG_B;
				if (b == 0) {
					return cont.apply(() -> {
						di.top = a + varargs.count();
						di.extras = varargs;
					});
				} else {
					return cont.apply(() -> {
						for (int j = 1; j < b; ++j) {
							stack[a + j - 1] = varargs.arg(j);
						}
					});
				}
			}
		}

		throw new IllegalStateException();
	}

	private static void concat(LuaState state, DebugFrame frame, LuaValue[] stack, int top, int total) throws LuaError, UnwindThrowable {
		try {
			do {
				LuaValue left = stack[top - 2];
				LuaValue right = stack[top - 1];

				LuaString lString, rString;

				int n = 2;

				if (!left.isString() || !right.isString()) {
					// If one of these isn't convertible to a string then use the metamethod
					stack[top - 2] = OperationHelper.concatNonStrings(state, left, right, top - 2, top - 1);
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
		} catch (UnwindThrowable e) {
			frame.top = top;
			throw e;
		}
	}

	public static void closeAll(Upvalue[] upvalues) {
		if (upvalues == null) return;
		for (Upvalue upvalue : upvalues) if (upvalue != null) upvalue.close();
	}

	public static void resume(LuaState state, DebugFrame di, LuaInterpretedFunction function, Varargs varargs) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		final int i = p.code[di.pc++];
		final int opcode = (i >> POS_OP) & MAX_OP;

		switch (opcode) {
			case OP_ADD: case OP_SUB: case OP_MUL: case OP_DIV: case OP_MOD: case OP_POW: case OP_UNM:
			case OP_GETTABLE: case OP_GETGLOBAL: case OP_SELF: {
				di.stack[(i >> POS_A) & MAXARG_A] = varargs.first();
				break;
			}

			case OP_LE: case OP_LT: case OP_EQ: {
				boolean res = varargs.first().toBoolean();

				// If we should negate this result (due to using lt rather than le)
				if ((di.flags & FLAG_LEQ) != 0) {
					res = !res;
					di.flags ^= FLAG_LEQ;
				}

				if (res == (((i >> POS_A) & MAXARG_A) != 0)) {
					// We assume the next instruction is a jump and read the branch from there.
					di.pc += ((p.code[di.pc] >> POS_Bx) & MAXARG_Bx) - MAXARG_sBx;
				}
				di.pc++;
				break;
			}

			case OP_CALL: case OP_TAILCALL: {
				int a = (i >>> POS_A) & MAXARG_A;
				int c = (i >>> POS_C) & MAXARG_C;
				if (c > 0) {
					LuaValue[] stack = di.stack;
					while (--c > 0) stack[a + c - 1] = varargs.arg(c);
					di.extras = NONE;
				} else {
					di.extras = varargs;
					di.top = a + varargs.count();
				}
				break;
			}

			case OP_SETTABLE: case OP_SETGLOBAL:
				// Nothing to be done here
				break;

			case OP_TFORLOOP: {
				LuaValue o = varargs.first();
				if (o.isNil()) {
					di.pc++;
				} else {
					int a = (i >>> POS_A) & MAXARG_A;
					LuaValue[] stack = di.stack;
					stack[a + 2] = stack[a + 3] = o;
					for (int c = (i >>> POS_C) & MAXARG_C; c > 1; --c) {
						stack[a + 2 + c] = varargs.arg(c);
					}
					di.extras = Constants.NONE;
				}
				break;
			}
			case OP_CONCAT: {
				int a = (i >>> POS_A) & MAXARG_A;
				int b = (i >>> POS_B) & MAXARG_B;

				LuaValue[] stack = di.stack;
				int top = di.top - 1;

				stack[top - 1] = varargs.first();
				int total = top - b;
				if (total > 1) {
					// Rewind time, we may end up executing this instruction multiple times.
					di.pc--;
					concat(state, di, stack, top, total);
					di.pc++;
				}
				stack[a] = stack[b];
				di.top = top;

				break;
			}

			default:
				throw new IllegalStateException("Resuming from unknown instruction (" + Print.OPNAMES[opcode] + ")");
		}
	}

	public static Varargs resumeReturn(LuaState state, DebugState ds, DebugFrame di, LuaInterpretedFunction function) throws LuaError, UnwindThrowable {
		int i = function.p.code[di.pc];
		DebugHandler handler = state.debug;

		switch (((i >> POS_OP) & MAX_OP)) {
			case OP_RETURN: {
				int a = (i >>> POS_B) & MAXARG_B;
				int b = (i >>> POS_B) & MAXARG_B;

				Varargs ret;
				switch (b) {
					case 0:
						ret = ValueFactory.varargsOf(di.stack, a, di.top - di.extras.count() - a, di.extras).asImmutable();
						break;
					case 1:
						ret = NONE;
						break;
					case 2:
						ret = di.stack[a];
						break;
					default:
						ret = ValueFactory.varargsOf(di.stack, a, b - 1).asImmutable();
						break;
				}

				int flags = di.flags;
				handler.onReturnError(ds);

				if ((flags & FLAG_FRESH) != 0) {
					// If we're a fresh invocation then return to the parent.
					return ret;
				} else {
					di = ds.getStackUnsafe();
					function = (LuaInterpretedFunction) di.closure;
					resume(state, di, function, ret);
					return execute(state, di, function);
				}
			}

			default:
				Print.printCode(function.p);
				throw new IllegalStateException("Cannot resume on this opcode (pc=" + di.pc + ")");
		}
	}
}
