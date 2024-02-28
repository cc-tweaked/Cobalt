package org.squiddev.cobalt.unwind;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.Varargs;

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
}
