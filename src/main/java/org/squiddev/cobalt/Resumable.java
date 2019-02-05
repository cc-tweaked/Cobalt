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
