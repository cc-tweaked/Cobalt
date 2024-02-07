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
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.debug.Upvalue;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.Lua.*;
import static org.squiddev.cobalt.LuaDouble.valueOf;
import static org.squiddev.cobalt.debug.DebugFrame.*;

/**
 * The main interpreter for {@link LuaInterpretedFunction}s.
 */
final class LuaInterpreter {
	private LuaInterpreter() {
	}

	private static LuaValue[] createStack(Prototype prototype) {
		LuaValue[] stack = new LuaValue[prototype.maxStackSize];
		System.arraycopy(NILS, 0, stack, 0, prototype.maxStackSize);
		return stack;
	}

	public static void setupCall(DebugState ds, DebugFrame frame, LuaInterpretedFunction function, int flags) throws UnwindThrowable, LuaError {
		Prototype p = function.p;
		LuaValue[] stack = createStack(p);
		setupFrame(ds, frame, function, NONE, stack, flags);
	}

	public static void setupCall(DebugState ds, DebugFrame frame, LuaInterpretedFunction function, LuaValue arg, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		LuaValue[] stack = createStack(p);

		switch (p.parameters) {
			case 0 -> setupFrame(ds, frame, function, arg, stack, flags);
			default -> {
				stack[0] = arg;
				setupFrame(ds, frame, function, NONE, stack, flags);
			}
		}
	}

	public static void setupCall(DebugState ds, DebugFrame frame, LuaInterpretedFunction function, LuaValue arg1, LuaValue arg2, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		LuaValue[] stack = createStack(p);

		switch (p.parameters) {
			case 0 -> {
				setupFrame(ds, frame, function, p.isVarArg ? ValueFactory.varargsOf(arg1, arg2) : NONE, stack, flags);
			}
			case 1 -> {
				stack[0] = arg1;
				setupFrame(ds, frame, function, arg2, stack, flags);
			}
			default -> {
				stack[0] = arg1;
				stack[1] = arg2;
				setupFrame(ds, frame, function, NONE, stack, flags);
			}
		}
	}

	public static void setupCall(DebugState ds, DebugFrame frame, LuaInterpretedFunction function, LuaValue arg1, LuaValue arg2, LuaValue arg3, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		LuaValue[] stack = createStack(p);

		switch (p.parameters) {
			case 0 -> {
				setupFrame(ds, frame, function, p.isVarArg ? ValueFactory.varargsOf(arg1, arg2, arg3) : NONE, stack, flags);
			}
			case 1 -> {
				stack[0] = arg1;
				setupFrame(ds, frame, function, p.isVarArg ? ValueFactory.varargsOf(arg2, arg3) : NONE, stack, flags);
			}
			case 2 -> {
				stack[0] = arg1;
				stack[1] = arg2;
				setupFrame(ds, frame, function, arg3, stack, flags);
			}
			default -> {
				stack[0] = arg1;
				stack[1] = arg2;
				stack[2] = arg3;
				setupFrame(ds, frame, function, NONE, stack, flags);
			}
		}
	}

	private static Varargs setupStack(Prototype prototype, LuaValue[] stack, LuaValue[] args, int argsStart, int argsSize) {
		System.arraycopy(args, argsStart, stack, 0, Math.min(argsSize, prototype.parameters));
		return prototype.isVarArg && argsSize > prototype.parameters
			? ValueFactory.varargsOfCopy(args, argsStart + prototype.parameters, argsSize - prototype.parameters)
			: NONE;
	}

	private static Varargs setupStack(Prototype prototype, LuaValue[] stack, Varargs varargs) {
		for (int i = 0; i < prototype.parameters; i++) stack[i] = varargs.arg(i + 1);
		return prototype.isVarArg ? varargs.subargs(prototype.parameters + 1) : NONE;
	}

	static void setupCall(DebugState ds, DebugFrame frame, LuaInterpretedFunction function, Varargs varargs, int flags) throws LuaError, UnwindThrowable {
		Prototype p = function.p;
		LuaValue[] stack = createStack(p);
		Varargs args = setupStack(p, stack, varargs);
		setupFrame(ds, frame, function, args, stack, flags);
	}

	private static void setupFrame(DebugState ds, DebugFrame di, LuaClosure function, Varargs varargs, LuaValue[] stack, int flags) throws UnwindThrowable, LuaError {
		di.func = function;
		di.closure = function;
		di.varargs = varargs;
		di.stack = stack;
		di.flags |= flags;
		di.extras = NONE;
		di.top = di.pc = 0;
		ds.onCall(di);
	}

	/*
	 ** converts back a "floating point byte" to an integer.
	 ** The floating point byte  is represented as
	 ** (eeeeexxx), where the real value is (1xxx) * 2^(eeeee - 1) if
	 ** eeeee != 0 and (xxx) otherwise.
	 */
	private static int luaO_fb2int(int x) {
		int e = (x >> 3) & 31;
		if (e == 0) return x;
		else return ((x & 7) + 8) << (e - 1);
	}

	static Varargs execute(final LuaState state, DebugFrame di, LuaInterpretedFunction function) throws LuaError, UnwindThrowable {
		final DebugState ds = DebugState.get(state);

		newFrame:
		while (true) {
			// Fetch all info from the function
			final Prototype p = function.p;
			final Upvalue[] upvalues = function.upvalues;
			final int[] code = p.code;
			final LuaValue[] k = p.constants;

			// And from the debug info
			final LuaValue[] stack = di.stack;
			final Varargs varargs = di.varargs;

			int pc = di.pc;

			// process instructions
			while (true) {
				di.pc = pc;
				if (state.isInterrupted()) state.handleInterrupt();
				ds.onInstruction(di, pc);

				// pull out instruction
				int i = code[pc++];
				int a = GETARG_A(i);

				// process the instruction
				switch (GET_OPCODE(i)) {
					case OP_MOVE: // A B: R(A):= R(B)
						stack[a] = stack[GETARG_B(i)];
						break;

					case OP_LOADK: // A Bx: R(A):= Kst(Bx)
						stack[a] = k[GETARG_Bx(i)];
						break;

					case OP_LOADKX: { // A: R(A) := Kst(extra arg)
						assert GET_OPCODE(code[pc]) == OP_EXTRAARG;
						int rb = GETARG_Ax(code[pc++]);
						stack[a] = k[rb];
						break;
					}

					case OP_LOADBOOL: { // A B C: R(A):= (Bool)B: if (C) pc++
						stack[a] = GETARG_B(i) != 0 ? TRUE : FALSE;
						if (GETARG_C(i) != 0) pc++; // skip next instruction (if C)
						break;
					}

					case OP_LOADNIL: { // A B     R(A), R(A+1), ..., R(A+B) := nil
						int b = GETARG_B(i);
						do {
							stack[a++] = NIL;
						} while (b-- > 0);
						break;
					}

					case OP_GETUPVAL: // A B: R(A):= UpValue[B]
						stack[a] = upvalues[GETARG_B(i)].getValue();
						break;

					case OP_GETTABUP: {// A B C: R(A) := UpValue[B][RK(C)]
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						stack[a] = OperationHelper.getTable(state, upvalues[b].getValue(), getRK(stack, k, c), -b - 1);
						break;
					}

					case OP_GETTABLE: { // A B C: R(A):= R(B)[RK(C)]
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						stack[a] = OperationHelper.getTable(state, stack[b], getRK(stack, k, c), b);
						break;
					}

					case OP_SETTABUP: {// A B C: UpValue[A][RK(B)] := RK(C)
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						OperationHelper.setTable(state, upvalues[a].getValue(), getRK(stack, k, b), getRK(stack, k, c), -b - 1);
						break;
					}

					case OP_SETUPVAL: // A B: UpValue[B]:= R(A)
						upvalues[GETARG_B(i)].setValue(stack[a]);
						break;

					case OP_SETTABLE: { // A B C: R(A)[RK(B)]:= RK(C)
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						OperationHelper.setTable(state, stack[a], getRK(stack, k, b), getRK(stack, k, c), a);
						break;
					}

					case OP_NEWTABLE: // A B C: R(A):= {} (size = B,C)
						stack[a] = new LuaTable(luaO_fb2int(GETARG_B(i)), luaO_fb2int(GETARG_C(i)));
						break;

					case OP_SELF: { // A B C: R(A+1):= R(B): R(A):= R(B)[RK(C)]
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						LuaValue o = stack[a + 1] = stack[b];
						stack[a] = OperationHelper.getTable(state, o, getRK(stack, k, c), b);
						break;
					}

					case OP_ADD: { // A B C: R(A):= RK(B) + RK(C)
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						stack[a] = OperationHelper.add(state, getRK(stack, k, b), getRK(stack, k, c));
						break;
					}

					case OP_SUB: { // A B C: R(A):= RK(B) - RK(C)
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						stack[a] = OperationHelper.sub(state, getRK(stack, k, b), getRK(stack, k, c));
						break;
					}

					case OP_MUL: { // A B C: R(A):= RK(B) * RK(C)
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						stack[a] = OperationHelper.mul(state, getRK(stack, k, b), getRK(stack, k, c));
						break;
					}

					case OP_DIV: { // A B C: R(A):= RK(B) / RK(C)
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						stack[a] = OperationHelper.div(state, getRK(stack, k, b), getRK(stack, k, c));
						break;
					}

					case OP_MOD: { // A B C: R(A):= RK(B) % RK(C)
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						stack[a] = OperationHelper.mod(state, getRK(stack, k, b), getRK(stack, k, c));
						break;
					}

					case OP_POW: { // A B C: R(A):= RK(B) ^ RK(C)
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						stack[a] = OperationHelper.pow(state, getRK(stack, k, b), getRK(stack, k, c));
						break;
					}

					case OP_UNM: { // A B: R(A):= -R(B)
						int b = GETARG_B(i);
						stack[a] = OperationHelper.neg(state, getRK(stack, k, b));
						break;
					}

					case OP_NOT:// A B: R(A):= not R(B)
						stack[a] = stack[GETARG_B(i)].toBoolean() ? FALSE : TRUE;
						break;

					case OP_LEN: { // A B: R(A):= length of R(B)
						int b = GETARG_B(i);
						stack[a] = OperationHelper.length(state, stack[b]);
						break;
					}

					case OP_CONCAT: { // A B C: R(A):= R(B).. ... ..R(C)
						int b = GETARG_B(i);
						int c = GETARG_C(i);

						di.top = c + 1;
						concat(state, di, stack, di.top, c - b + 1);
						stack[a] = stack[b];
						di.top = b;
						break;
					}

					case OP_JMP: // sBx: pc+=sBx
						pc += doJump(di, i, 0);
						break;

					case OP_EQ: { // A B C: if ((RK(B) == RK(C)) ~= A) then pc++
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						if (OperationHelper.eq(state, getRK(stack, k, b), getRK(stack, k, c)) == (a != 0)) {
							// We assume the next instruction is a jump and read the branch from there.
							pc += doJump(di, code[pc], 1);
						} else {
							pc++;
						}
						break;
					}

					case OP_LT: { // A B C: if ((RK(B) <  RK(C)) ~= A) then pc++
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						if (OperationHelper.lt(state, getRK(stack, k, b), getRK(stack, k, c)) == (a != 0)) {
							pc += doJump(di, code[pc], 1);
						} else {
							pc++;
						}
						break;
					}

					case OP_LE: { // A B C: if ((RK(B) <= RK(C)) ~= A) then pc++
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						if (OperationHelper.le(state, getRK(stack, k, b), getRK(stack, k, c)) == (a != 0)) {
							pc += doJump(di, code[pc], 1);
						} else {
							pc++;
						}
						break;
					}

					case OP_TEST: { // A C: if not (R(A) <=> C) then pc++
						if (stack[a].toBoolean() == ((GETARG_C(i)) != 0)) {
							pc += doJump(di, code[pc], 1);
						} else {
							pc++;
						}
						break;
					}

					case OP_TESTSET: { // A B C: if (R(B) <=> C) then R(A):= R(B) else pc++
						/* note: doc appears to be reversed */
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						LuaValue val = stack[b];
						if (val.toBoolean() == (c != 0)) {
							stack[a] = val;
							pc += doJump(di, code[pc], 1);
						} else {
							pc++;
						}
						break;
					}

					case OP_CALL: { // A B C: R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1)) */
						int b = GETARG_B(i);
						int c = GETARG_C(i);

						LuaValue val = stack[a];
						if (val instanceof LuaInterpretedFunction) {
							function = (LuaInterpretedFunction) val;

							Prototype newPrototype = function.p;
							LuaValue[] newStack = createStack(newPrototype);
							DebugFrame newFrame = ds.pushInfo();
							Varargs args = b > 0
								? setupStack(newPrototype, newStack, stack, a + 1, b - 1) // Exact args count
								: setupStack(newPrototype, newStack, ValueFactory.varargsOfCopy(stack, a + 1, di.top - di.extras.count() - (a + 1), di.extras)); // From previous top
							setupFrame(ds, newFrame, function, args, newStack, 0);
							di = newFrame;

							continue newFrame;
						} else {
							nativeCall(state, di, stack, val, i, a, b, c);
						}
						break;
					}

					case OP_TAILCALL: { // A B C: return R(A)(R(A+1), ... ,R(A+B-1))
						int b = GETARG_B(i);

						LuaValue val = stack[a];
						Varargs args;
						switch (b) {
							case 1 -> args = NONE;
							case 2 -> args = stack[a + 1];
							default -> {
								Varargs v = di.extras;
								args = b > 0 ?
									ValueFactory.varargsOfCopy(stack, a + 1, b - 1) : // exact arg count
									ValueFactory.varargsOfCopy(stack, a + 1, di.top - v.count() - (a + 1), v); // from prev top
							}
						}

						LuaFunction functionVal;
						if (val instanceof LuaFunction func) {
							functionVal = func;
						} else {
							functionVal = Dispatch.getCallMetamethod(state, val, a);
							args = ValueFactory.varargsOf(val, args);
						}

						if (functionVal instanceof LuaInterpretedFunction) {
							int flags = di.flags;
							di.cleanup();
							ds.popInfo();

							// FIXME: Return hook???!?

							// Replace the current frame with a new one.
							function = (LuaInterpretedFunction) functionVal;
							di = (flags & FLAG_FRESH) != 0 ? ds.pushJavaInfo() : ds.pushInfo();
							setupCall(ds, di, function, args, (flags & FLAG_FRESH) | FLAG_TAIL);
							continue newFrame;
						} else {
							Varargs v = Dispatch.invoke(state, functionVal, args);
							di.top = a + v.count();
							di.extras = v;
							break;
						}
					}

					case OP_RETURN: { // A B: return R(A), ... ,R(A+B-2) (see note)
						int b = GETARG_B(i);

						int flags = di.flags, top = di.top;
						Varargs v = di.extras;
						di.cleanup();

						Varargs ret = b > 0
							? ValueFactory.varargsOfCopy(stack, a, b - 1)
							: ValueFactory.varargsOfCopy(stack, a, top - v.count() - a, v);

						if ((flags & FLAG_FRESH) != 0) {
							// If we're a fresh invocation then return to the parent.
							return ret;
						} else {
							ds.onReturn(di, ret);
							di = ds.getStackUnsafe();
							function = (LuaInterpretedFunction) di.func;
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
							pc += GETARG_sBx(i);
						}
						break;
					}

					case OP_FORPREP: { // A sBx: R(A)-=R(A+2): pc+=sBx
						LuaNumber init = stack[a].checkNumber("'for' initial value must be a number");
						LuaNumber limit = stack[a + 1].checkNumber("'for' limit must be a number");
						LuaNumber step = stack[a + 2].checkNumber("'for' step must be a number");
						stack[a] = valueOf(init.toDouble() - step.toDouble());
						stack[a + 1] = limit;
						stack[a + 2] = step;
						pc += GETARG_sBx(i);
						break;
					}

					case OP_TFORCALL: {
						Varargs result = Dispatch.invoke(state, stack[a], ValueFactory.varargsOf(stack[a + 1], stack[a + 2]), a);
						for (int c = GETARG_C(i); c >= 1; --c) stack[a + 2 + c] = result.arg(c);

						i = code[pc++];
						a = GETARG_A(i);
						assert GET_OPCODE(i) == OP_TFORLOOP;
					}
					// fallthrough to OP_TFORLOOP, avoiding an extra interpreter loop.

					case OP_TFORLOOP: {
						var value = stack[a + 1];
						if (!value.isNil()) {
							stack[a] = value;
							pc += GETARG_sBx(i);
						}
						break;
					}

					case OP_SETLIST: { // A B C: R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
						int b = GETARG_B(i);
						int c = GETARG_C(i);
						if (c == 0) c = GETARG_Ax(code[pc++]);

						int offset = (c - 1) * LFIELDS_PER_FLUSH;
						LuaTable tbl = stack[a].checkTable();
						if (b == 0) {
							b = di.top - a - 1;
							int m = b - di.extras.count();
							tbl.presize(offset + b);

							int j = 1;
							for (; j <= m; j++) tbl.rawset(offset + j, stack[a + j]);
							for (; j <= b; j++) tbl.rawset(offset + j, di.extras.arg(j - m));
						} else {
							tbl.presize(offset + b);
							for (int j = 1; j <= b; j++) tbl.rawset(offset + j, stack[a + j]);
						}
						break;
					}

					case OP_CLOSURE: { // A Bx: R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n))
						Prototype newp = p.children[GETARG_Bx(i)];
						LuaInterpretedFunction newcl = new LuaInterpretedFunction(newp);
						for (int j = 0, nup = newp.upvalues(); j < nup; ++j) {
							var up = newp.getUpvalue(j);
							newcl.upvalues[j] = up.fromLocal() ? di.getUpvalue(up.index()) : upvalues[up.index()];
						}
						stack[a] = newcl;
						break;
					}

					case OP_VARARG: { // A B: R(A), R(A+1), ..., R(A+B-1) = vararg
						int b = GETARG_B(i);
						if (b == 0) {
							di.top = a + varargs.count();
							di.extras = varargs;
						} else {
							for (int j = 1; j < b; ++j) {
								stack[a + j - 1] = varargs.arg(j);
							}
						}
						break;
					}

					default: {
						assert false : "Unknown opcode";
						throw new IllegalStateException("Unknown opcode");
					}
				}
			}
		}
	}

	private static LuaValue getRK(LuaValue[] stack, LuaValue[] k, int slot) {
		return ISK(slot) ? k[INDEXK(slot)] : stack[slot];
	}

	private static int doJump(DebugFrame frame, int i, int e) {
		int a = GETARG_A(i);
		if (a > 0) frame.closeUpvalues(a - 1);
		return GETARG_sBx(i) + e;
	}

	private static void nativeCall(LuaState state, DebugFrame di, LuaValue[] stack, LuaValue val, int i, int a, int b, int c) throws UnwindThrowable, LuaError {
		switch (i & (MASK_B | MASK_C)) {
			case (1 << POS_B) | (0 << POS_C) -> {
				Varargs v = di.extras = Dispatch.invoke(state, val, NONE, a);
				di.top = a + v.count();
			}
			case (2 << POS_B) | (0 << POS_C) -> {
				Varargs v = di.extras = Dispatch.invoke(state, val, stack[a + 1], a);
				di.top = a + v.count();
			}
			case (1 << POS_B) | (1 << POS_C) -> Dispatch.call(state, val, a);
			case (2 << POS_B) | (1 << POS_C) -> Dispatch.call(state, val, stack[a + 1], a);
			case (3 << POS_B) | (1 << POS_C) -> Dispatch.call(state, val, stack[a + 1], stack[a + 2], a);
			case (4 << POS_B) | (1 << POS_C) -> Dispatch.call(state, val, stack[a + 1], stack[a + 2], stack[a + 3], a);
			case (1 << POS_B) | (2 << POS_C) -> stack[a] = Dispatch.call(state, val, a);
			case (2 << POS_B) | (2 << POS_C) -> stack[a] = Dispatch.call(state, val, stack[a + 1], a);
			case (3 << POS_B) | (2 << POS_C) -> stack[a] = Dispatch.call(state, val, stack[a + 1], stack[a + 2], a);
			case (4 << POS_B) | (2 << POS_C) ->
				stack[a] = Dispatch.call(state, val, stack[a + 1], stack[a + 2], stack[a + 3], a);
			default -> {
				Varargs args = b > 0 ?
					ValueFactory.varargsOfCopy(stack, a + 1, b - 1) : // exact arg count
					ValueFactory.varargsOfCopy(stack, a + 1, di.top - di.extras.count() - (a + 1), di.extras); // from prev top
				Varargs v = Dispatch.invoke(state, val, args, a);
				if (c > 0) {
					while (--c > 0) stack[a + c - 1] = v.arg(c);
				} else {
					di.top = a + v.count();
					di.extras = v;
				}
			}
		}
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
				} else if ((rString = right.checkLuaString()).length() == 0) {
					stack[top - 2] = left.checkLuaString();
				} else if ((lString = left.checkLuaString()).length() == 0) {
					stack[top - 2] = rString;
				} else {
					int length = rString.length() + lString.length();
					stack[top - 2] = lString;
					stack[top - 1] = rString;

					for (; n < total; n++) {
						LuaValue value = stack[top - n - 1];
						if (!value.isString()) break;

						LuaString string = value.checkLuaString();

						// Ensure we don't get a string which is too long
						int strLen = string.length();
						if (strLen > Integer.MAX_VALUE - length) throw new LuaError("string length overflow");

						// Otherwise increment the length and store this converted string
						stack[top - n - 1] = string;
						length += strLen;
					}

					stack[top - n] = LuaString.valueOfStrings(stack, top - n, n, length);
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

	public static void resume(LuaState state, DebugFrame di, LuaInterpretedFunction function, Varargs varargs) throws LuaError, UnwindThrowable {
		int pc = di.pc++;
		Prototype p = function.p;
		int i = p.code[pc];

		switch (GET_OPCODE(i)) {
			case OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_MOD, OP_POW, OP_UNM, OP_LEN, OP_GETTABLE, OP_GETTABUP, OP_SELF -> {
				di.stack[GETARG_A(i)] = varargs.first();
			}

			case OP_LE, OP_LT, OP_EQ -> {
				boolean res = varargs.first().toBoolean();

				// If we should negate this result (due to using lt rather than le)
				if ((di.flags & FLAG_LEQ) != 0) {
					res = !res;
					di.flags ^= FLAG_LEQ;
				}

				if (res == (GETARG_A(i) != 0)) {
					// We assume the next instruction is a jump and read the branch from there.
					di.pc += GETARG_sBx(p.code[di.pc]);
				}
				di.pc++;
			}

			case OP_CALL, OP_TAILCALL -> {
				int a = GETARG_A(i);
				int c = GETARG_C(i);
				if (c > 0) {
					LuaValue[] stack = di.stack;
					while (--c > 0) stack[a + c - 1] = varargs.arg(c);
					di.extras = NONE;
				} else {
					di.extras = varargs;
					di.top = a + varargs.count();
				}
			}

			case OP_SETTABLE, OP_SETTABUP -> {
				// Nothing to be done here
			}

			case OP_TFORCALL -> {
				LuaValue[] stack = di.stack;
				int a = GETARG_A(i);
				for (int c = GETARG_C(i); c >= 1; --c) stack[a + 2 + c] = varargs.arg(c);
			}

			case OP_CONCAT -> {
				int a = GETARG_A(i);
				int b = GETARG_B(i);

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
			}

			default -> throw reportIllegalResume(state, function.p, pc);
		}
	}

	private static LuaError reportIllegalResume(LuaState state, Prototype prototype, int pc) {
		LuaError err = new LuaError("cannot resume this opcode");
		state.reportInternalError(err, () -> {
			StringBuilder output = new StringBuilder();
			output.append(String.format("Resuming function at invalid opcode. file=\"%s\", pc=%d\n", prototype.shortSource(), pc + 1));
			Print.printCode(output, prototype, true);
			return output.toString();
		});
		return err;
	}
}
