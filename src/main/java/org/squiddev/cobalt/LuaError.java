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

import org.squiddev.cobalt.debug.DebugHelpers;

import java.io.Serial;

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
public final class LuaError extends Exception {
	@Serial
	private static final long serialVersionUID = 3065540200206862088L;

	private LuaValue value;

	/**
	 * The traceback for this error message
	 */
	private String traceback;

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
	 * <p>
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
		value = message == null ? Constants.NIL : ValueFactory.valueOf(message);
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
		value = message == null ? Constants.NIL : ValueFactory.valueOf(message);
	}

	/**
	 * Construct a LuaError with a message.
	 *
	 * @param message message to supply
	 */
	public LuaError(LuaValue message) {
		super(rawToString(message));
		this.level = 1;
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
	 * Convert an {@link Throwable} in a {@link LuaError}. This will return the original error if it is some Lua error,
	 * or a wrapped version otherwise.
	 *
	 * @param error The error to convert
	 * @return The converted error
	 */
	public static LuaError wrap(Throwable error) {
		if (error instanceof LuaError) return (LuaError) error;
		return new LuaError(error);
	}

	@Override
	public String getMessage() {
		return traceback != null ? traceback : rawToString(value);
	}

	/**
	 * The value for this error
	 */
	public LuaValue getValue() {
		return value;
	}

	public void fillTraceback(LuaState state) {
		// TODO: Split this into two methods: one which adds the context, and one which computes the traceback.

		if (traceback != null) return;
		if (getCause() != null) state.reportInternalError(getCause(), () -> "Uncaught Java exception");

		LuaThread thread = state.getCurrentThread();
		if (level > 0 && value.type() == Constants.TSTRING) {
			String fileLine;
			if (calculateLevel) {
				fileLine = DebugHelpers.fileLine(thread);
			} else {
				fileLine = DebugHelpers.fileLine(thread, level);
			}
			if (fileLine != null) value = ValueFactory.valueOf(fileLine + ": " + value.toString());
		}

		traceback = getMessage() + "\n" + DebugHelpers.traceback(thread, level);
	}

	private static String rawToString(LuaValue value) {
		return switch (value.type()) {
			case Constants.TTABLE, Constants.TUSERDATA, Constants.TLIGHTUSERDATA ->
				value.typeName() + ": " + Integer.toHexString(value.hashCode());
			default -> value.toString();
		};
	}
}
