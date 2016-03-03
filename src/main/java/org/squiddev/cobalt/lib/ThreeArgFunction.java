/**
 * ****************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
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
package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;

import static org.squiddev.cobalt.Constants.NIL;

/**
 * Abstract base class for Java function implementations that take two arguments and
 * return one value.
 * <p>
 * Subclasses need only implement {@link LuaValue#call(LuaState, LuaValue, LuaValue, LuaValue)} to complete this class,
 * simplifying development.
 * All other uses of {@link LuaValue#call(LuaState)}, {@link LuaValue#invoke(LuaState, Varargs)},etc,
 * are routed through this method by this class,
 * dropping or extending arguments with {@code nil} values as required.
 * <p>
 * If more or less than three arguments are required,
 * or variable argument or variable return values,
 * then use one of the related function
 * {@link ZeroArgFunction}, {@link OneArgFunction}, {@link TwoArgFunction}, or {@link VarArgFunction}.
 * <p>
 * See {@link LibFunction} for more information on implementation libraries and library functions.
 *
 * @see LuaValue#call(LuaState, LuaValue, LuaValue, LuaValue)
 * @see LibFunction
 * @see ZeroArgFunction
 * @see OneArgFunction
 * @see TwoArgFunction
 * @see VarArgFunction
 */
public abstract class ThreeArgFunction extends LibFunction {

	@Override
	public abstract LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3);

	/**
	 * Default constructor
	 */
	public ThreeArgFunction() {
	}

	/**
	 * Constructor with specific environment
	 *
	 * @param env The environment to apply during constructon.
	 */
	public ThreeArgFunction(LuaValue env) {
		this.env = env;
	}

	@Override
	public final LuaValue call(LuaState state) {
		return call(state, NIL, NIL, NIL);
	}

	@Override
	public final LuaValue call(LuaState state, LuaValue arg) {
		return call(state, arg, NIL, NIL);
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) {
		return call(state, arg1, arg2, NIL);
	}

	@Override
	public Varargs invoke(LuaState state, Varargs varargs) {
		return call(state, varargs.first(), varargs.arg(2), varargs.arg(3));
	}

}
