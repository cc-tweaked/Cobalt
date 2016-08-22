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
package org.squiddev.cobalt;

/**
 * Subclass of {@link Varargs} that represents a lua tail call
 * in a Java library function execution environment.
 *
 * Since Java doesn't have direct support for tail calls,
 * any lua function whose {@link Prototype} contains the
 * {@link Lua#OP_TAILCALL} bytecode needs a mechanism
 * for tail calls when converting lua-bytecode to java-bytecode.
 *
 * The tail call holds the next function and arguments,
 * and the client a call to {@link Varargs#eval(LuaState)} executes the function
 * repeatedly until the tail calls are completed.
 *
 * Normally, users of luaj need not concern themselves with the
 * details of this mechanism, as it is built into the core
 * execution framework.
 *
 * @see Prototype
 */
public class TailcallVarargs extends Varargs {

	private LuaValue func;
	private Varargs args;
	private Varargs result;

	public TailcallVarargs(LuaValue f, Varargs args) {
		this.func = f;
		this.args = args;
	}

	@Override
	public boolean isTailcall() {
		return true;
	}

	@Override
	public Varargs eval(LuaState state) {
		while (result == null) {
			Varargs r = OperationHelper.onInvoke(state, func, args);
			if (r.isTailcall()) {
				TailcallVarargs t = (TailcallVarargs) r;
				func = t.func;
				args = t.args;
			} else {
				result = r;
				func = null;
				args = null;
			}
		}
		return result;
	}

	@Override
	public LuaValue arg(int i) {
		if (result == null) {
			throw new IllegalStateException("Hasn't been evaluated");
		}
		return result.arg(i);
	}

	@Override
	public LuaValue first() {
		if (result == null) {
			throw new IllegalStateException("Hasn't been evaluated");
		}
		return result.first();
	}

	@Override
	public int count() {
		if (result == null) {
			throw new IllegalStateException("Hasn't been evaluated");
		}
		return result.count();
	}
}
