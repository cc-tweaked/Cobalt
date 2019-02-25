/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2017 SquidDev
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

import org.squiddev.cobalt.debug.DebugHelpers;
import org.squiddev.cobalt.lib.UncheckedLuaError;

import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.LuaString.valueOf;

/**
 * RuntimeException that is thrown and caught in response to a lua error.
 *
 * {@link LuaError} is used wherever a lua call to {@code error()}
 * would be used within a script.
 *
 * Since it is an unchecked exception inheriting from {@link RuntimeException},
 * Java method signatures do notdeclare this exception, althoug it can
 * be thrown on almost any luaj Java operation.
 * This is analagous to the fact that any lua script can throw a lua error at any time.
 */
public final class LuaError extends Exception {
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
	 * Whether we should guess the error level
	 */
	private final boolean calculateLevel;

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
		calculateLevel = true;
		value = ValueFactory.valueOf("vm error: " + cause.toString());
	}

	/**
	 * Construct a LuaError with a specific message.
	 *
	 * @param message message to supply
	 */
	public LuaError(String message) {
		super(message);
		level = 1;
		calculateLevel = true;
		value = ValueFactory.valueOf(message);
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
		calculateLevel = false;
		value = ValueFactory.valueOf(message);
	}

	/**
	 * Construct a LuaError with a specific message.
	 *
	 * @param message message to supply
	 */
	public LuaError(LuaValue message) {
		super(rawToString(message));
		level = 1;
		calculateLevel = true;
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
		calculateLevel = false;
		value = message;
	}

	/**
	 * Convert an {@link Exception} in a {@link LuaError}. This will return the original error if it is some Lua error,
	 * or a wrapped version otherwise.
	 *
	 * @param error The error to convert
	 * @return The converted error
	 */
	public static LuaError wrap(Exception error) {
		if (error instanceof LuaError) return (LuaError) error;
		if (error instanceof UncheckedLuaError) return ((UncheckedLuaError) error).getCause();
		return new LuaError(error);
	}

	/**
	 * Convert an {@link Exception} in a {@link LuaError}. This will return the original error if it is some Lua error,
	 * or an error with the same message as the orginal.
	 *
	 * @param error The error to convert
	 * @return The converted error
	 * @see #getMessage()
	 */
	public static LuaError wrapMessage(Exception error) {
		if (error instanceof LuaError) return ((LuaError) error);
		if (error instanceof UncheckedLuaError) return ((UncheckedLuaError) error).getCause();

		String message = error.getMessage();
		return new LuaError(message == null ? NIL : valueOf(message));
	}

	/**
	 * Extract the message from an {@link Exception} or {@link LuaError}.
	 *
	 * This is equivalent to using {@link #wrapMessage(Exception)}} and then {@link #value}.
	 *
	 * @param error The error to convert
	 * @return The extracted message.
	 * @see #wrapMessage(Exception)
	 */
	public static LuaValue getMessage(Exception error) {
		if (error instanceof LuaError) return ((LuaError) error).value;
		if (error instanceof UncheckedLuaError) return ((UncheckedLuaError) error).getCause().value;

		String message = error.getMessage();
		return message == null ? NIL : valueOf(message);
	}

	@Override
	public String getMessage() {
		return traceback != null ? traceback : rawToString(value);
	}

	public void fillTracebackNoHandler(LuaState state) {
		if (traceback != null) return;
		fillTracebackImpl(state);

		LuaThread thread = state.getCurrentThread();
		if (thread.errFunc != null) throw new IllegalStateException("Thread has no error function");
	}

	public void fillTraceback(LuaState state) throws UnwindThrowable {
		if (traceback != null) return;
		fillTracebackImpl(state);

		LuaThread thread = state.getCurrentThread();
		if (thread.errFunc != null) {
			LuaValue errFunc = thread.setErrorFunc(null);
			try {
				value = OperationHelper.call(state, errFunc, value);
			} catch (Exception t) {
				value = ValueFactory.valueOf("error in error handling");
				thread.errFunc = errFunc;
			}
		}
	}

	private void fillTracebackImpl(LuaState state) {
		LuaThread thread = state.getCurrentThread();
		if (level > 0 && value.isString()) {
			String fileLine;
			if (calculateLevel) {
				fileLine = DebugHelpers.fileLine(thread);
			} else {
				fileLine = DebugHelpers.fileLine(thread, level - 1);
			}
			if (fileLine != null) value = ValueFactory.valueOf(fileLine + ": " + value.toString());
		}

		traceback = getMessage() + "\n" + DebugHelpers.traceback(thread, level - 1);
	}

	private static String rawToString(LuaValue value) {
		switch (value.type()) {
			case Constants.TTABLE:
			case Constants.TUSERDATA:
			case Constants.TLIGHTUSERDATA:
				return value.typeName() + ": " + Integer.toHexString(value.hashCode());
			default:
				return value.toString();
		}
	}
}
