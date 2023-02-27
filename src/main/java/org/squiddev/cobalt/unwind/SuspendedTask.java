package org.squiddev.cobalt.unwind;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.debug.DebugFrame;

/**
 * Represents the state of a function call which yielded part way through. This function may be {@linkplain
 * #resume(Varargs) resumed} to continue execution.
 *
 * @param <T> The result type of this task.
 */
public interface SuspendedTask<T> {
	/**
	 * Resume this task.
	 *
	 * @param args The values to resume with.
	 * @return The result of evaluating this task.
	 * @throws LuaError        If this task errored.
	 * @throws UnwindThrowable If we yielded again after resuming this task. In this case, this object should be reused
	 *                         again to continue execution when this coroutine resumes.
	 */
	T resume(Varargs args) throws LuaError, UnwindThrowable;

	/**
	 * Run the provided action. If it yields, store the resulting suspended task into {@link DebugFrame#state}.
	 * <p>
	 * This method should only be used with {@link AutoUnwind} instrumented functions. It will be replaced at compile
	 * time with a direct dispatch.
	 *
	 * @param frame  The current call frame, into which to store our continuation.
	 * @param action The suspendable action. This should be a constant lambda.
	 * @param <T>    The result type of this function (and resulting task).
	 * @return The result of evaluating this function.
	 * @throws LuaError        If the function threw a runtime error.
	 * @throws UnwindThrowable If the function yielded.
	 */
	static <T> T run(DebugFrame frame, Action<T> action) throws LuaError, UnwindThrowable {
		throw new AssertionError("Calls to this method should not appear in transformed code.");
	}

	/**
	 * Equivalent to {@link #run(DebugFrame, Action)}, but with an assertion that the task will never yield.
	 *
	 * @param action The function to run.
	 * @param <T>    The result type of this function (and resulting task).
	 * @return The result of evaluating this function.
	 * @throws LuaError If the function threw a runtime error.
	 */
	static <T> T noYield(Action<T> action) throws LuaError {
		throw new AssertionError("Calls to this method should not appear in transformed code.");
	}

	/**
	 * Equivalent to {@link #run(DebugFrame, Action)}, but packaging both as a {@link SuspendedFunction}, so it can be
	 * more easily composed with other primitives.
	 *
	 * @param action The function to run.
	 * @param <T>    The result type of this function (and resulting task).
	 * @return A {@link SuspendedFunction}, which can be later invoked.
	 */
	static <T> SuspendedFunction<T> toFunction(Action<T> action) {
		throw new AssertionError("Calls to this method should not appear in transformed code.");
	}

	/**
	 * A functional interface which starts a task.
	 *
	 * @param <T> The result of this task.
	 */
	@FunctionalInterface
	interface Action<T> {
		T run() throws LuaError, UnwindThrowable;
	}
}
