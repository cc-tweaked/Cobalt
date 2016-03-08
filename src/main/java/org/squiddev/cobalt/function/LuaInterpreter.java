/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
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
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugInfo;
import org.squiddev.cobalt.debug.DebugState;

import static org.squiddev.cobalt.Constants.FALSE;
import static org.squiddev.cobalt.Constants.TRUE;

/**
 * Extension of {@link LuaFunction} which executes lua bytecode.
 *
 * A {@link LuaInterpreter} is a combination of a {@link Prototype}
 * and a {@link LuaValue} to use as an environment for execution.
 *
 * There are three main ways {@link LuaInterpreter} instances are created:
 * <ul>
 * <li>Construct an instance using {@link #LuaInterpreter(Prototype, LuaValue)}</li>
 * <li>Construct it indirectly by loading a chunk via {@link LoadState.LuaCompiler#load(java.io.InputStream, String, LuaValue)}
 * <li>Execute the lua bytecode {@link Lua#OP_CLOSURE} as part of bytecode processing
 * </ul>
 *
 * To construct it directly, the {@link Prototype} is typically created via a compiler such as {@link LuaC}:
 * <pre> {@code
 * InputStream is = new ByteArrayInputStream("print('hello,world').getBytes());
 * Prototype p = LuaC.instance.compile(is, "script");
 * LuaValue _G = JsePlatform.standardGlobals()
 * LuaClosure f = new LuaClosure(p, _G);
 * }</pre>
 *
 * To construct it indirectly, the {@link LuaC} compiler may be used,
 * which implements the {@link LoadState.LuaCompiler} interface:
 * <pre> {@code
 * LuaFunction f = LuaC.instance.load(is, "script", _G);
 * }</pre>
 *
 * Typically, a closure that has just been loaded needs to be initialized by executing it,
 * and its return value can be saved if needed:
 * <pre> {@code
 * LuaValue r = f.call();
 * _G.set( "mypkg", r )
 * }</pre>
 *
 * In the preceding, the loaded value is typed as {@link LuaFunction}
 * to allow for the possibility of other compilers such as LuaJC
 * producing {@link LuaFunction} directly without
 * creating a {@link Prototype} or {@link LuaInterpreter}.
 *
 * Since a {@link LuaInterpreter} is a {@link LuaFunction} which is a {@link LuaValue},
 * all the value operations can be used directly such as:
 * <ul>
 * <li>{@link LuaValue#setfenv(LuaValue)}</li>
 * <li>{@link LuaValue#call(LuaState)}</li>
 * <li>{@link LuaValue#call(LuaState, LuaValue)}</li>
 * <li>{@link LuaValue#invoke(LuaState, Varargs)}</li>
 * <li>{@link LuaValue#invoke(LuaState, Varargs)}</li>
 * <li> ...</li>
 * </ul>
 *
 * @see LuaValue
 * @see LuaFunction
 * @see LuaValue#isClosure()
 * @see LuaValue#checkClosure()
 * @see LuaValue#optClosure(LuaClosure)
 * @see LoadState
 */
public class LuaInterpreter extends LuaClosure {
	private static final Upvalue[] NOUPVALUEs = new Upvalue[0];

	public final Prototype p;
	public final Upvalue[] upvalues;

	public LuaInterpreter() {
		p = null;
		upvalues = null;
	}

	/**
	 * Supply the initial environment
	 *
	 * @param p   The prototype to run
	 * @param env The environement to run in
	 */
	public LuaInterpreter(Prototype p, LuaValue env) {
		super(env);
		this.p = p;
		this.upvalues = p.nups > 0 ? new Upvalue[p.nups] : NOUPVALUEs;
	}

	protected LuaInterpreter(int nupvalues, LuaValue env) {
		super(env);
		this.p = null;
		this.upvalues = nupvalues > 0 ? new Upvalue[nupvalues] : NOUPVALUEs;
	}

	@Override
	public final LuaValue call(LuaState state) {
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(Constants.NILS, 0, stack, 0, p.maxstacksize);
		return execute(state, stack, Constants.NONE).eval(state).first();
	}

	@Override
	public final LuaValue call(LuaState state, LuaValue arg) {
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(Constants.NILS, 0, stack, 0, p.maxstacksize);
		switch (p.numparams) {
			default:
				stack[0] = arg;
				return execute(state, stack, Constants.NONE).eval(state).first();
			case 0:
				return execute(state, stack, arg).eval(state).first();
		}
	}

	@Override
	public final LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) {
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(Constants.NILS, 0, stack, 0, p.maxstacksize);
		switch (p.numparams) {
			default:
				stack[0] = arg1;
				stack[1] = arg2;
				return execute(state, stack, Constants.NONE).eval(state).first();
			case 1:
				stack[0] = arg1;
				return execute(state, stack, arg2).eval(state).first();
			case 0:
				return execute(state, stack, p.is_vararg != 0 ? ValueFactory.varargsOf(arg1, arg2) : Constants.NONE).eval(state).first();
		}
	}

	@Override
	public final LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(Constants.NILS, 0, stack, 0, p.maxstacksize);
		switch (p.numparams) {
			default:
				stack[0] = arg1;
				stack[1] = arg2;
				stack[2] = arg3;
				return execute(state, stack, Constants.NONE).eval(state).first();
			case 2:
				stack[0] = arg1;
				stack[1] = arg2;
				return execute(state, stack, arg3).eval(state).first();
			case 1:
				stack[0] = arg1;
				return execute(state, stack, p.is_vararg != 0 ? ValueFactory.varargsOf(arg2, arg3) : Constants.NONE).eval(state).first();
			case 0:
				return execute(state, stack, p.is_vararg != 0 ? ValueFactory.varargsOf(arg1, arg2, arg3) : Constants.NONE).eval(state).first();
		}
	}

	@Override
	public final Varargs invoke(LuaState state, Varargs varargs) {
		return onInvoke(state, varargs).eval(state);
	}

	@Override
	public Varargs onInvoke(LuaState state, Varargs varargs) {
		LuaValue[] stack = new LuaValue[p.maxstacksize];
		System.arraycopy(Constants.NILS, 0, stack, 0, p.maxstacksize);
		for (int i = 0; i < p.numparams; i++) {
			stack[i] = varargs.arg(i + 1);
		}
		return execute(state, stack, p.is_vararg != 0 ? varargs.subargs(p.numparams + 1) : Constants.NONE);
	}


	protected Varargs execute(LuaState state, LuaValue[] stack, Varargs varargs) {
		// loop through instructions
		int i, a, b, c, pc = 0, top = 0;
		LuaValue o;
		Varargs v = Constants.NONE;
		int[] code = p.code;
		LuaValue[] k = p.k;

		// upvalues are only possible when closures create closures
		Upvalue[] openups = p.p.length > 0 ? new Upvalue[stack.length] : null;

		// create varargs "arg" table
		if (p.is_vararg >= Lua.VARARG_NEEDSARG) {
			stack[p.numparams] = new LuaTable(varargs);
		}

		// debug wants args to this function
		DebugHandler handler = state.debug;
		DebugState ds = handler.getDebugState();
		DebugInfo di = handler.onCall(ds, this, varargs, stack);

		// process instructions
		try {
			while (true) {
				if (di != null) handler.onInstruction(ds, di, pc, v, top);

				// pull out instruction
				i = code[pc++];
				a = ((i >> 6) & 0xff);

				// process the op code
				switch (i & 0x3f) {

					case Lua.OP_MOVE:/*	A B	R(A):= R(B)					*/
						stack[a] = stack[i >>> 23];
						continue;

					case Lua.OP_LOADK:/*	A Bx	R(A):= Kst(Bx)					*/
						stack[a] = k[i >>> 14];
						continue;

					case Lua.OP_LOADBOOL:/*	A B C	R(A):= (Bool)B: if (C) pc++			*/
						stack[a] = (i >>> 23 != 0) ? Constants.TRUE : Constants.FALSE;
						if ((i & (0x1ff << 14)) != 0) {
							pc++; /* skip next instruction (if C) */
						}
						continue;

					case Lua.OP_LOADNIL: /*	A B	R(A):= ...:= R(B):= nil			*/
						for (b = i >>> 23; a <= b; ) {
							stack[a++] = Constants.NIL;
						}
						continue;

					case Lua.OP_GETUPVAL: /*	A B	R(A):= UpValue[B]				*/
						stack[a] = upvalues[i >>> 23].getValue();
						continue;

					case Lua.OP_GETGLOBAL: /*	A Bx	R(A):= Gbl[Kst(Bx)]				*/
						stack[a] = env.get(state, k[i >>> 14]);
						continue;

					case Lua.OP_GETTABLE: /*	A B C	R(A):= R(B)[RK(C)]				*/
						stack[a] = stack[i >>> 23].get(state, (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
						continue;

					case Lua.OP_SETGLOBAL: /*	A Bx	Gbl[Kst(Bx)]:= R(A)				*/
						env.set(state, k[i >>> 14], stack[a]);
						continue;

					case Lua.OP_SETUPVAL: /*	A B	UpValue[B]:= R(A)				*/
						upvalues[i >>> 23].setValue(stack[a]);
						continue;

					case Lua.OP_SETTABLE: /*	A B C	R(A)[RK(B)]:= RK(C)				*/
						stack[a].set(state, ((b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b]), (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
						continue;

					case Lua.OP_NEWTABLE: /*	A B C	R(A):= {} (size = B,C)				*/
						stack[a] = new LuaTable(i >>> 23, (i >> 14) & 0x1ff);
						continue;

					case Lua.OP_SELF: /*	A B C	R(A+1):= R(B): R(A):= R(B)[RK(C)]		*/
						stack[a + 1] = (o = stack[i >>> 23]);
						stack[a] = o.get(state, (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
						continue;

					case Lua.OP_ADD: /*	A B C	R(A):= RK(B) + RK(C)				*/
						stack[a] = OperationHelper.add(state, (b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b], (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
						continue;

					case Lua.OP_SUB: /*	A B C	R(A):= RK(B) - RK(C)				*/
						stack[a] = OperationHelper.sub(state, (b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b], (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
						continue;

					case Lua.OP_MUL: /*	A B C	R(A):= RK(B) * RK(C)				*/
						stack[a] = OperationHelper.mul(state, (b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b], (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
						continue;

					case Lua.OP_DIV: /*	A B C	R(A):= RK(B) / RK(C)				*/
						stack[a] = OperationHelper.div(state, (b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b], (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
						continue;

					case Lua.OP_MOD: /*	A B C	R(A):= RK(B) % RK(C)				*/
						stack[a] = OperationHelper.mod(state, (b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b], (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
						continue;

					case Lua.OP_POW: /*	A B C	R(A):= RK(B) ^ RK(C)				*/
						stack[a] = OperationHelper.pow(state, (b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b], (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]);
						continue;

					case Lua.OP_UNM: /*	A B	R(A):= -R(B)					*/
						stack[a] = stack[i >>> 23].neg(state);
						continue;

					case Lua.OP_NOT: /*	A B	R(A):= not R(B)				*/
						stack[a] = stack[i >>> 23].toBoolean() ? FALSE : TRUE;
						continue;

					case Lua.OP_LEN: /*	A B	R(A):= length of R(B)				*/
						stack[a] = stack[i >>> 23].len(state);
						continue;

					case Lua.OP_CONCAT: /*	A B C	R(A):= R(B).. ... ..R(C)			*/
						b = i >>> 23;
						c = (i >> 14) & 0x1ff;
					{
						int count = c - b + 1;

						if (count > 1) {
							LuaValue buffer = stack[c];
							while (--c >= b) {
								buffer = OperationHelper.concat(state, stack[c], buffer);
							}
							stack[a] = buffer;
						} else {
							stack[a] = OperationHelper.concat(state, stack[c - 1], stack[c]);
						}
					}
					continue;

					case Lua.OP_JMP: /*	sBx	pc+=sBx					*/
						pc += (i >>> 14) - 0x1ffff;
						continue;

					case Lua.OP_EQ: /*	A B C	if ((RK(B) == RK(C)) ~= A) then pc++		*/
						if (OperationHelper.eq(state, (b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b], (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]) == (a == 0)) {
							++pc;
						}
						continue;

					case Lua.OP_LT: /*	A B C	if ((RK(B) <  RK(C)) ~= A) then pc++  		*/
						if (OperationHelper.lt(state, (b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b], (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]) == (a == 0)) {
							++pc;
						}
						continue;

					case Lua.OP_LE: /*	A B C	if ((RK(B) <= RK(C)) ~= A) then pc++  		*/
						if (OperationHelper.le(state, (b = i >>> 23) > 0xff ? k[b & 0x0ff] : stack[b], (c = (i >> 14) & 0x1ff) > 0xff ? k[c & 0x0ff] : stack[c]) == (a == 0)) {
							++pc;
						}
						continue;

					case Lua.OP_TEST: /*	A C	if not (R(A) <=> C) then pc++			*/
						if (stack[a].toBoolean() == ((i & (0x1ff << 14)) == 0)) {
							++pc;
						}
						continue;

					case Lua.OP_TESTSET: /*	A B C	if (R(B) <=> C) then R(A):= R(B) else pc++	*/
					/* note: doc appears to be reversed */
						if ((o = stack[i >>> 23]).toBoolean() == ((i & (0x1ff << 14)) == 0)) {
							++pc;
						} else {
							stack[a] = o; // TODO: should be sBx?
						}
						continue;

					case Lua.OP_CALL: /*	A B C	R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1)) */
						switch (i & (Lua.MASK_B | Lua.MASK_C)) {
							case (1 << Lua.POS_B) | (0 << Lua.POS_C):
								v = stack[a].invoke(state, Constants.NONE);
								top = a + v.count();
								continue;
							case (2 << Lua.POS_B) | (0 << Lua.POS_C):
								v = stack[a].invoke(state, stack[a + 1]);
								top = a + v.count();
								continue;
							case (1 << Lua.POS_B) | (1 << Lua.POS_C):
								stack[a].call(state);
								continue;
							case (2 << Lua.POS_B) | (1 << Lua.POS_C):
								stack[a].call(state, stack[a + 1]);
								continue;
							case (3 << Lua.POS_B) | (1 << Lua.POS_C):
								stack[a].call(state, stack[a + 1], stack[a + 2]);
								continue;
							case (4 << Lua.POS_B) | (1 << Lua.POS_C):
								stack[a].call(state, stack[a + 1], stack[a + 2], stack[a + 3]);
								continue;
							case (1 << Lua.POS_B) | (2 << Lua.POS_C):
								stack[a] = stack[a].call(state);
								continue;
							case (2 << Lua.POS_B) | (2 << Lua.POS_C):
								stack[a] = stack[a].call(state, stack[a + 1]);
								continue;
							case (3 << Lua.POS_B) | (2 << Lua.POS_C):
								stack[a] = stack[a].call(state, stack[a + 1], stack[a + 2]);
								continue;
							case (4 << Lua.POS_B) | (2 << Lua.POS_C):
								stack[a] = stack[a].call(state, stack[a + 1], stack[a + 2], stack[a + 3]);
								continue;
							default:
								b = i >>> 23;
								c = (i >> 14) & 0x1ff;
								v = b > 0 ?
									ValueFactory.varargsOf(stack, a + 1, b - 1) : // exact arg count
									ValueFactory.varargsOf(stack, a + 1, top - v.count() - (a + 1), v); // from prev top
								v = stack[a].invoke(state, v);
								if (c > 0) {
									while (--c > 0) {
										stack[a + c - 1] = v.arg(c);
									}
									v = Constants.NONE; // TODO: necessary?
								} else {
									top = a + v.count();
								}
								continue;
						}

					case Lua.OP_TAILCALL: /*	A B C	return R(A)(R(A+1), ... ,R(A+B-1))		*/
						switch (i & Lua.MASK_B) {
							case (1 << Lua.POS_B):
								return new TailcallVarargs(stack[a], Constants.NONE);
							case (2 << Lua.POS_B):
								return new TailcallVarargs(stack[a], stack[a + 1]);
							case (3 << Lua.POS_B):
								return new TailcallVarargs(stack[a], ValueFactory.varargsOf(stack[a + 1], stack[a + 2]));
							case (4 << Lua.POS_B):
								return new TailcallVarargs(stack[a], ValueFactory.varargsOf(stack[a + 1], stack[a + 2], stack[a + 3]));
							default:
								b = i >>> 23;
								v = b > 0 ?
									ValueFactory.varargsOf(stack, a + 1, b - 1) : // exact arg count
									ValueFactory.varargsOf(stack, a + 1, top - v.count() - (a + 1), v); // from prev top
								return new TailcallVarargs(stack[a], v);
						}

					case Lua.OP_RETURN: /*	A B	return R(A), ... ,R(A+B-2)	(see note)	*/
						b = i >>> 23;
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

					case Lua.OP_FORLOOP: /*	A sBx	R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }*/ {
						LuaValue limit = stack[a + 1];
						LuaValue step = stack[a + 2];
						LuaValue idx = OperationHelper.add(state, step, stack[a]);
						if (OperationHelper.lt(state, Constants.ZERO, step) ? OperationHelper.le(state, idx, limit) : OperationHelper.le(state, limit, idx)) {
							stack[a] = idx;
							stack[a + 3] = idx;
							pc += (i >>> 14) - 0x1ffff;
						}
					}
					continue;

					case Lua.OP_FORPREP: /*	A sBx	R(A)-=R(A+2): pc+=sBx				*/ {
						LuaValue init = stack[a].checkNumber("'for' initial value must be a number");
						LuaValue limit = stack[a + 1].checkNumber("'for' limit must be a number");
						LuaValue step = stack[a + 2].checkNumber("'for' step must be a number");
						stack[a] = OperationHelper.sub(state, init, step);
						stack[a + 1] = limit;
						stack[a + 2] = step;
						pc += (i >>> 14) - 0x1ffff;
					}
					continue;

					case Lua.OP_TFORLOOP: /*
									 * A C R(A+3), ... ,R(A+2+C):= R(A)(R(A+1),
									 * R(A+2)): if R(A+3) ~= nil then R(A+2)=R(A+3)
									 * else pc++
									 */
						// TODO: stack call on for loop body, such as:   stack[a].call(ci);
						v = stack[a].invoke(state, ValueFactory.varargsOf(stack[a + 1], stack[a + 2]));
						if ((o = v.first()).isNil()) {
							++pc;
						} else {
							stack[a + 2] = stack[a + 3] = o;
							for (c = (i >> 14) & 0x1ff; c > 1; --c) {
								stack[a + 2 + c] = v.arg(c);
							}
							v = Constants.NONE; // todo: necessary?
						}
						continue;

					case Lua.OP_SETLIST: /*	A B C	R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B	*/ {
						if ((c = (i >> 14) & 0x1ff) == 0) {
							c = code[pc++];
						}
						int offset = (c - 1) * Lua.LFIELDS_PER_FLUSH;
						o = stack[a];
						if ((b = i >>> 23) == 0) {
							b = top - a - 1;
							int m = b - v.count();
							int j = 1;
							for (; j <= m; j++) {
								o.set(state, offset + j, stack[a + j]);
							}
							for (; j <= b; j++) {
								o.set(state, offset + j, v.arg(j - m));
							}
						} else {
							o.presize(offset + b);
							for (int j = 1; j <= b; j++) {
								o.set(state, offset + j, stack[a + j]);
							}
						}
					}
					continue;

					case Lua.OP_CLOSE: /*	A 	close all variables in the stack up to (>=) R(A)*/
						for (b = openups.length; --b >= a; ) {
							if (openups[b] != null) {
								openups[b].close();
								openups[b] = null;
							}
						}
						continue;

					case Lua.OP_CLOSURE: /*	A Bx	R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n))	*/ {
						Prototype newp = p.p[i >>> 14];
						LuaInterpreter newcl = new LuaInterpreter(newp, env);
						for (int j = 0, nup = newp.nups; j < nup; ++j) {
							i = code[pc++];
							//b = B(i);
							b = i >>> 23;
							newcl.upvalues[j] = (i & 4) != 0 ?
								upvalues[b] :
								openups[b] != null ? openups[b] : (openups[b] = new Upvalue(stack, b));
						}
						stack[a] = newcl;
					}
					continue;

					case Lua.OP_VARARG: /*	A B	R(A), R(A+1), ..., R(A+B-1) = vararg		*/
						b = i >>> 23;
						if (b == 0) {
							top = a + (b = varargs.count());
							v = varargs;
						} else {
							for (int j = 1; j < b; ++j) {
								stack[a + j - 1] = varargs.arg(j);
							}
						}
				}
			}
		} catch (LuaError le) {
			throw le.fillTraceback(state);
		} catch (Exception e) {
			throw new LuaError(e).fillTraceback(state);
		} finally {
			ds.onReturn();
			if (openups != null) {
				for (int u = openups.length; --u >= 0; ) {
					if (openups[u] != null) {
						openups[u].close();
					}
				}
			}
		}
	}

	@Override
	public LuaValue getUpvalue(int i) {
		return upvalues[i].getValue();
	}

	@Override
	public void setUpvalue(int i, LuaValue v) {
		upvalues[i].setValue(v);
	}

	@Override
	public Prototype getPrototype() {
		return p;
	}
}