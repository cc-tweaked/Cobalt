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
import org.squiddev.cobalt.debug.DebugState;

/**
 * Abstract base class for Java function implementations that takes varaiable arguments and
 * returns multiple return values.
 *
 * Subclasses need only implement {@link LuaValue#invoke(LuaState, Varargs)} to complete this class,
 * simplifying development.
 * All other uses of {@link LuaValue#call(LuaState, LuaValue)}, {@link LuaValue#invoke(LuaState, Varargs)},etc,
 * are routed through this method by this class,
 * converting arguments to {@link Varargs} and
 * dropping or extending return values with {@code nil} values as required.
 *
 * If between one and three arguments are required, and only one return value is returned,
 * {@link ZeroArgFunction}, {@link OneArgFunction}, {@link TwoArgFunction}, or {@link ThreeArgFunction}.
 *
 * See {@link LibFunction} for more information on implementation libraries and library functions.
 *
 * @see LuaValue#invoke(LuaState, Varargs)
 * @see LibFunction
 * @see ZeroArgFunction
 * @see OneArgFunction
 * @see TwoArgFunction
 * @see ThreeArgFunction
 */
public abstract class VarArgFunction extends LibFunction {
	public VarArgFunction() {
	}

	public VarArgFunction(LuaValue env) {
		this.env = env;
	}

	@Override
	public LuaValue call(LuaState state) {
		return invoke(state, Constants.NONE).first();
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg) {
		return invoke(state, arg).first();
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) {
		return invoke(state, ValueFactory.varargsOf(arg1, arg2)).first();
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return invoke(state, ValueFactory.varargsOf(arg1, arg2, arg3)).first();
	}

	/**
	 * Override and implement for the best performance.
	 * May not have expected behavior for tail calls.
	 * Should not be used if either:
	 * - function needs to be used as a module
	 * - function has a possibility of returning a TailcallVarargs
	 *
	 * @param state The current lua state
	 * @param args  the arguments to the function call.
	 */
	@Override
	public Varargs invoke(LuaState state, Varargs args) {
		DebugState ds = state.debug.getDebugState();
		ds.onCall(this);
		try {
			return this.onInvoke(state, args).eval(state);
		} finally {
			ds.onReturn();
		}
	}

	/**
	 * Override to provide a call implementation that runs in an environment
	 * that can participate in setfenv, and behaves as expected
	 * when returning TailcallVarargs.
	 *
	 * @param state The current lua state
	 * @param args  the arguments to the function call.
	 */
	@Override
	public Varargs onInvoke(LuaState state, Varargs args) {
		return invoke(state, args);
	}

}
