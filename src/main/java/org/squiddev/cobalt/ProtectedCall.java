package org.squiddev.cobalt;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.unwind.SuspendedFunction;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_ERROR;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_YPCALL;

/**
 * Perform a protected call, installing a given error handler.
 */
public class ProtectedCall {
	private static final LuaValue ERROR_IN_HANDLER = ValueFactory.valueOf("error in error handling");

	private final LuaValue errorFunction;
	private final DebugFrame currentFrame;

	private SuspendedFunction<Varargs> currentTask;
	private LuaValue previousErrorFunction;
	private boolean isError;

	public ProtectedCall(DebugFrame currentFrame, @Nullable LuaValue errorFunction) {
		this.errorFunction = errorFunction;
		this.currentFrame = currentFrame;
	}

	/**
	 * Call the provided function, catching any errors.
	 *
	 * @param state The current Lua VM.
	 * @param func  The function to call.
	 * @param args  Arguments to this function.
	 * @return The success/failure of calling this function.
	 * @throws UnwindThrowable If the underlying function yields.
	 */
	public Result apply(LuaState state, LuaValue func, Varargs args) throws UnwindThrowable {
		return apply(state, new CallSuspended(func, args));
	}

	/**
	 * Call the provided function, catching any errors.
	 * <p>
	 * This is a slightly more complex version of {@link #apply(LuaState, LuaValue, Varargs)} which accepts an arbitrary
	 * Java function, meaning a function is not pushed to the Java stack.
	 *
	 * @param state The current Lua VM.
	 * @param task  The yieldable function to call.
	 * @return The success/failure of calling this function.
	 * @throws UnwindThrowable If the underlying function yields.
	 */
	public Result apply(LuaState state, SuspendedFunction<Varargs> task) throws UnwindThrowable {
		if ((currentFrame.flags & FLAG_YPCALL) != 0) throw new IllegalStateException("Cannot have nested pcalls");
		if (currentTask != null) throw new IllegalStateException("Already have a task present");

		// We shouldn't reuse this, but let's reset it just in case.
		isError = false;

		currentTask = task;
		currentFrame.flags |= FLAG_YPCALL;
		previousErrorFunction = state.getCurrentThread().getErrorFunc();
		state.getCurrentThread().setErrorFunc(errorFunction);

		try {
			return finishSuccess(state, task.call(state));
		} catch (Exception | VirtualMachineError e) {
			return callErrorHandler(state, e);
		}
	}

	/**
	 * Resume this protected call after a yield.
	 *
	 * @param state The current Lua VM.
	 * @param args  The arguments returned from the yielding function or error handler.
	 * @return The success/failure of calling this function.
	 * @see Resumable#resume(LuaState, Object, Varargs)
	 */
	public Result resume(LuaState state, Varargs args) throws UnwindThrowable {
		if (currentTask != null) {
			// If we're still inside our task, then resume it.
			try {
				return finishSuccess(state, currentTask.resume(args));
			} catch (Exception | VirtualMachineError e) {
				return callErrorHandler(state, e);
			}
		} else {
			// Otherwise run our error handler.
			return isError ? finishError(state, args.first()) : finishSuccess(state, args);
		}
	}

	private void finish(LuaState state) {
		assert DebugState.get(state).getStackUnsafe() == currentFrame;
		assert (currentFrame.flags & FLAG_YPCALL) != 0;

		// Clear the pcall mask and restore the error function.
		currentFrame.flags &= ~FLAG_YPCALL;
		state.getCurrentThread().setErrorFunc(previousErrorFunction);
	}

	private Result finishSuccess(LuaState state, Varargs result) {
		currentTask = null;
		finish(state);
		return new Result(true, result);
	}

	/**
	 * Resume after an error occurred in the call or error handler.
	 *
	 * @param state The current Lua runtime.
	 * @param error The error that occurred.
	 * @return The success/failure of calling this function.
	 * @throws UnwindThrowable If the error handler yields.
	 * @see Resumable#resumeError(LuaState, Object, LuaError)
	 */
	public Result resumeError(LuaState state, LuaError error) throws UnwindThrowable {
		// If we've already had an error, then this must be a problem in the error handler and so fail. Otherwise the
		// original function errored, so call the handler.
		return isError ? finishError(state, ERROR_IN_HANDLER) : callErrorHandler(state, error);
	}

	private Result callErrorHandler(LuaState state, Throwable error) throws UnwindThrowable {
		// Mark the top frame as errored, meaning it will not be resumed.
		var debug = DebugState.get(state);
		debug.getStackUnsafe().flags |= FLAG_ERROR;
		if (currentTask != null) currentTask = null;

		// And mark us as being in the error handler.
		isError = true;

		LuaError luaError = LuaError.wrap(error);
		luaError.fillTraceback(state);

		LuaValue value;
		if (errorFunction == null) {
			value = luaError.getValue();
		} else {
			try {
				debug.growStackIfError();
				value = Dispatch.call(state, errorFunction, luaError.getValue());
			} catch (Exception | VirtualMachineError t) {
				value = ERROR_IN_HANDLER;
			}
		}

		return finishError(state, value);
	}

	private Result finishError(LuaState state, LuaValue result) {
		// Unwind to this frame and then bits of state.
		closeUntil(state, currentFrame);
		DebugState.get(state).shrinkStackIfError();
		finish(state);
		return new Result(false, result);
	}

	private static void closeUntil(LuaState state, DebugFrame top) {
		DebugState ds = DebugState.get(state);

		DebugFrame current;
		while ((current = ds.getStackUnsafe()) != top) {
			current.cleanup();
			ds.onReturnNoHook();
		}
	}

	public static final class Result {
		private final boolean isSuccess;
		private final Varargs result;

		Result(boolean isSuccess, Varargs result) {
			this.isSuccess = isSuccess;
			this.result = result;
		}

		public boolean isSuccess() {
			return isSuccess;
		}

		public Varargs result() {
			return result;
		}

		/**
		 * Convert this result into a true/false value and then the remaining values.
		 *
		 * @return The resulting varargs.
		 */
		public Varargs asBoolAndResult() {
			return varargsOf(valueOf(isSuccess), result);
		}

		/**
		 * Convert this result into a the returned value, or {@code nil} followed by the error message.
		 *
		 * @return The resulting varargs.
		 */
		public Varargs asResultOrFailure() {
			return isSuccess ? result : ValueFactory.varargsOf(Constants.NIL, result);
		}
	}

	private static class CallSuspended implements SuspendedFunction<Varargs> {
		private final LuaValue func;
		private final Varargs args;

		CallSuspended(LuaValue func, Varargs args) {
			this.func = func;
			this.args = args;
		}

		@Override
		public Varargs call(LuaState state) throws LuaError, UnwindThrowable {
			return Dispatch.invoke(state, func, args);
		}

		@Override
		public Varargs resume(Varargs args) {
			return args;
		}
	}
}
