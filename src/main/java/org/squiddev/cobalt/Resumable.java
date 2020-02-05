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
package org.squiddev.cobalt;

/**
 * A value which can be "resumed" with a specified state.
 */
public interface Resumable<T> {
	/**
	 * Resume this resumable with a value
	 *
	 * @param state  The current Lua state
	 * @param object The state for this object
	 * @param value  The value returned from the function above this in the stack
	 * @return The result of this function
	 * @throws LuaError        When a runtime error occurs.
	 * @throws UnwindThrowable If this {@link Resumable} transfers control to another coroutine.
	 */
	Varargs resume(LuaState state, T object, Varargs value) throws LuaError, UnwindThrowable;

	/**
	 * Resume this resumable with an error
	 *
	 * @param state  The current Lua state
	 * @param object The state for this object
	 * @param error  The error which was thrown
	 * @return The result of this function
	 * @throws LuaError        When a runtime error occurs.
	 * @throws UnwindThrowable If this {@link Resumable} transfers control to another coroutine.
	 */
	default Varargs resumeError(LuaState state, T object, LuaError error) throws LuaError, UnwindThrowable {
		throw error;
	}
}
