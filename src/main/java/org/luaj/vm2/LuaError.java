/**
 * ****************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
package org.luaj.vm2;

import org.luaj.vm2.lib.DebugLib;

import static org.luaj.vm2.Constants.*;
import static org.luaj.vm2.Factory.valueOf;

/**
 * RuntimeException that is thrown and caught in response to a lua error.
 * <p>
 * {@link LuaError} is used wherever a lua call to {@code error()}
 * would be used within a script.
 * <p>
 * Since it is an unchecked exception inheriting from {@link RuntimeException},
 * Java method signatures do notdeclare this exception, althoug it can
 * be thrown on almost any luaj Java operation.
 * This is analagous to the fact that any lua script can throw a lua error at any time.
 */
public final class LuaError extends RuntimeException {
	private static final long serialVersionUID = 3065540200206862088L;

	/**
	 * The value for this error
	 */
	public LuaValue value;

	/**
	 * The traceback for this error message
	 */
	public String traceback;

	/**
	 * The error to raise at
	 */
	private final int level;

	/**
	 * Construct LuaError when a program exception occurs.
	 *
	 * All errors generated from lua code should throw LuaError(String) instead.
	 *
	 * @param cause the Throwable that caused the error, if known.
	 */
	public LuaError(Throwable cause) {
		super(cause);
		level = 1;
		value = valueOf("vm error: " + cause.toString());
	}

	/**
	 * Construct a LuaError with a specific message.
	 *
	 * @param message message to supply
	 */
	public LuaError(String message) {
		super(message);
		level = 1;
		value = valueOf(message);
	}

	/**
	 * Construct a LuaError with a message, and level to draw line number information from.
	 *
	 * @param message message to supply
	 * @param level   where to supply line info from in call stack
	 */
	public LuaError(String message, int level) {
		super(message);
		this.level = level;
		value = valueOf(message);
	}

	/**
	 * Construct a LuaError with a specific message.
	 *
	 * @param message message to supply
	 */
	public LuaError(LuaValue message) {
		super(rawToString(message));
		level = 1;
		value = message;
	}

	/**
	 * Construct a LuaError with a message, and level to draw line number information from.
	 *
	 * @param message message to supply
	 * @param level   where to supply line info from in call stack
	 */
	public LuaError(LuaValue message, int level) {
		super(rawToString(message));
		this.level = level;
		value = message;
	}

	@Override
	public String getMessage() {
		return traceback != null ? traceback : rawToString(value);
	}

	public LuaError fillTraceback(LuaState state) {
		if (traceback != null) return this;

		LuaThread thread = state.currentThread;
		if (value.isstring()) {
			value = valueOf(DebugLib.fileline(thread, level) + ": " + value.toString());
		}

		traceback = getMessage() + "\n" + DebugLib.traceback(thread, level);

		if (thread.err != null) {
			LuaValue errfunc = thread.err;
			thread.err = null;
			try {
				value = errfunc.call(state, value);
			} catch (Throwable t) {
				value = valueOf("error in error handling");
			} finally {
				thread.err = errfunc;
			}
		}

		return this;
	}

	private static String rawToString(LuaValue value) {
		switch (value.type()) {
			case TTABLE:
			case TUSERDATA:
			case TLIGHTUSERDATA:
				return value.typeName() + ": " + Integer.toHexString(value.hashCode());
			default:
				return value.toString();
		}
	}
}
