package cc.tweaked.cobalt.internal.unwind;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.unwind.SuspendedFunction;

/**
 * A functional interface which starts a task.
 *
 * @param <T> The result of this task.
 */
@FunctionalInterface
public interface SuspendedAction<T> {
	/**
	 * Run the provided action. If it yields, store the resulting suspended task into {@link DebugFrame#state}.
	 * <p>
	 * This method should only be used with {@link AutoUnwind} instrumented functions. It will be replaced at compile
	 * time with a direct dispatch.
	 *
	 * @param frame  The current call frame, into which to store our continuation.
	 * @param action The suspendable action. This should be a constant lambda.
	 * @return The result of evaluating this function.
	 * @throws LuaError        If the function threw a runtime error.
	 * @throws UnwindThrowable If the function yielded.
	 */
	static Varargs run(DebugFrame frame, SuspendedAction<Varargs> action) throws LuaError, UnwindThrowable {
		throw new AssertionError("Calls to this method should not appear in transformed code.");
	}

	/**
	 * Run the provided {@link SuspendedAction}, asserting that it will never yield.
	 * <p>
	 * This method should only be used with {@link AutoUnwind} instrumented functions. It will be replaced at compile
	 * time with a direct dispatch.
	 *
	 * @param action The function to run.
	 * @param <T>    The result type of this function (and resulting task).
	 * @return The result of evaluating this function.
	 * @throws LuaError If the function threw a runtime error.
	 */
	static <T> T noYield(SuspendedAction<T> action) throws LuaError {
		throw new AssertionError("Calls to this method should not appear in transformed code.");
	}

	/**
	 * Convert the provided {@link SuspendedAction} to a {@link SuspendedFunction}.
	 * <p>
	 * This method should only be used with {@link AutoUnwind} instrumented functions. It will be replaced at compile
	 * time with a direct dispatch.
	 *
	 * @param action The function to run.
	 * @param <T>    The result type of this function (and resulting task).
	 * @return A {@link SuspendedFunction}, which can be later invoked.
	 * @see org.squiddev.cobalt.function.SuspendedVarArgFunction
	 */
	static <T> SuspendedFunction<T> toFunction(SuspendedAction<T> action) {
		throw new AssertionError("Calls to this method should not appear in transformed code.");
	}

	T run() throws LuaError, UnwindThrowable;
}
