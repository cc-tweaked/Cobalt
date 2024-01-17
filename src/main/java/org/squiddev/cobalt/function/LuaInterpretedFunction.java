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
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.debug.Upvalue;

import java.io.InputStream;

import static org.squiddev.cobalt.debug.DebugFrame.*;
import static org.squiddev.cobalt.function.LuaInterpreter.execute;

/**
 * Extension of {@link LuaFunction} which executes lua bytecode.
 * <p>
 * A {@link LuaInterpretedFunction} is a combination of a {@link Prototype}
 * and a {@link LuaValue} to use as an environment for execution.
 * <p>
 * There are three main ways {@link LuaInterpretedFunction} instances are created:
 * <ul>
 * <li>Construct an instance using {@link #LuaInterpretedFunction(Prototype, LuaTable)}</li>
 * <li>Construct it indirectly by loading a chunk via {@link LoadState#load(LuaState, InputStream, String, LuaTable)}
 * <li>Execute the lua bytecode {@link Lua#OP_CLOSURE} as part of bytecode processing
 * </ul>
 * <p>
 * To construct it directly, the {@link Prototype} is typically created via {@linkplain LoadState the compiler}:
 * <pre> {@code
 * InputStream is = new ByteArrayInputStream("print('hello,world').getBytes());
 * Prototype p = LuaC.INSTANCE.compile(is, "script");
 * LuaValue _G = JsePlatform.standardGlobals()
 * LuaClosure f = new LuaClosure(p, _G);
 * }</pre>
 * <p>
 * To construct it indirectly, the {@link LuaC} compiler may be used,
 * which implements the {@link LoadState.FunctionFactory} interface:
 * <pre> {@code
 * LuaFunction f = LuaC.INSTANCE.load(is, "script", _G);
 * }</pre>
 * <p>
 * Typically, a closure that has just been loaded needs to be initialized by executing it,
 * and its return value can be saved if needed:
 * <pre> {@code
 * LuaValue r = f.call();
 * _G.set( "mypkg", r )
 * }</pre>
 * <p>
 * In the preceding, the loaded value is typed as {@link LuaFunction}
 * to allow for the possibility of other compilers such as LuaJC
 * producing {@link LuaFunction} directly without
 * creating a {@link Prototype} or {@link LuaInterpretedFunction}.
 * <p>
 * Since a {@link LuaInterpretedFunction} is a {@link LuaFunction} which is a {@link LuaValue},
 * all the value operations can be used directly such as:
 * <ul>
 * <li>{@link LuaFunction#call(LuaState)}</li>
 * <li>{@link LuaFunction#call(LuaState, LuaValue)}</li>
 * <li>{@link LuaFunction#invoke(LuaState, Varargs)}</li>
 * <li>{@link LuaFunction#invoke(LuaState, Varargs)}</li>
 * <li> ...</li>
 * </ul>
 *
 * @see LuaValue
 * @see LuaFunction
 * @see LoadState
 */
public final class LuaInterpretedFunction extends LuaClosure implements Resumable<Object> {
	private static final Upvalue[] NO_UPVALUES = new Upvalue[0];

	final Prototype p;
	public final Upvalue[] upvalues;

	/**
	 * Supply the initial environment
	 *
	 * @param p The prototype to run
	 */
	public LuaInterpretedFunction(Prototype p) {
		this.p = p;
		this.upvalues = p.upvalues() > 0 ? new Upvalue[p.upvalues()] : NO_UPVALUES;
	}

	public void nilUpvalues() {
		int nups = p.upvalues();
		if (nups > 0) {
			Upvalue[] upvalues = this.upvalues;
			for (int i = 0; i < nups; i++) {
				upvalues[i] = new Upvalue(Constants.NIL);
			}
		}
	}

	@Override
	public Upvalue getUpvalue(int i) {
		return upvalues[i];
	}

	@Override
	public void setUpvalue(int i, Upvalue upvalue) {
		upvalues[i] = upvalue;
	}

	@Override
	public Prototype getPrototype() {
		return p;
	}

	@Override
	public Varargs resume(LuaState state, Object object, Varargs value) throws LuaError, UnwindThrowable {
		DebugState ds = DebugState.get(state);
		var frame = ds.getStackUnsafe();

		if ((frame.flags & (FLAG_ANY_HOOK | FLAG_INTERRUPTED)) != 0) {
			// We're resuming in from a hook
			assert ds.inhook || (frame.flags & FLAG_INTERRUPTED) != 0;

			if ((frame.flags & FLAG_INTERRUPTED) != 0) {
				// This occurs when the insn/line debug hook yield, we continue execution, and then we suspend instead.
				// In this case, we just clear the flag and then resume execution - like with the insn/line hook,
				// everything else should be handled by the interpreter.
				frame.flags &= ~FLAG_INTERRUPTED;
			} else if ((frame.flags & (FLAG_INSN_HOOK | FLAG_LINE_HOOK)) != 0) {
				// Yielded within instruction hook, do nothing - we'll handle this inside the interpreter.
			} else {
				throw new AssertionError("Incorrect debug flag set");
			}
		} else {
			LuaInterpreter.resume(state, frame, this, value);
		}

		return execute(state, frame, this);
	}
}
