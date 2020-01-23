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

/**
 * Wrapper class for Java function implementations that take any number of arguments and return any number of values.
 *
 * All usages of {@link LuaFunction#call(LuaState)}, etc, are routed through the
 * {@link AnyArgs#call(LuaState, Varargs)}.
 */
final class VarArgFunction extends LibFunction {
	private final AnyArgs delegate;

	VarArgFunction(LuaTable env, String name, AnyArgs delegate) {
		super(env, name);
		this.delegate = delegate;
	}

	@Override
	public LuaValue call(LuaState state) throws LuaError {
		return delegate.call(state, Constants.NONE).first();
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
		return delegate.call(state, arg).first();
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		return delegate.call(state, ValueFactory.varargsOf(arg1, arg2)).first();
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError {
		return delegate.call(state, ValueFactory.varargsOf(arg1, arg2, arg3)).first();
	}

	@Override
	public Varargs invoke(LuaState state, Varargs args) throws LuaError {
		return delegate.call(state, args);
	}

}
