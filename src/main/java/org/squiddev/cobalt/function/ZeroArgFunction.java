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
 * Wrapper class for Java function implementations that take no arguments and return one value.
 *
 * All usages of {@link LuaFunction#call(LuaState, LuaValue)}, {@link LuaFunction#invoke(LuaState, Varargs)},etc,
 * are routed through the {@link ZeroArgs#call(LuaState)}.
 */
final class ZeroArgFunction extends LibFunction {
	private final ZeroArgs delegate;

	ZeroArgFunction(LuaTable env, String name, ZeroArgs delegate) {
		super(env, name);
		this.delegate = delegate;
	}

	@Override
	public LuaValue call(LuaState state) throws LuaError {
		return delegate.call(state);
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
		return delegate.call(state);
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		return delegate.call(state);
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError {
		return delegate.call(state);
	}

	@Override
	public Varargs invoke(LuaState state, Varargs varargs) throws LuaError {
		return delegate.call(state);
	}

}
