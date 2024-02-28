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

import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.CoroutineLib;

import java.util.Objects;

import static org.squiddev.cobalt.debug.DebugFrame.FLAG_ERROR;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_YPCALL;

/**
 * Subclass of {@link LuaValue} that implements a Lua coroutine thread.
 * <p>
 * A LuaThread is typically created in response to a scripted call to {@code coroutine.create()}.
 *
 * @see LuaValue
 * @see CoroutineLib
 */
public final class LuaThread extends LuaValue {
	public enum Status {
		/**
		 * A coroutine which has been run at all.
		 */
		INITIAL("suspended"),

		/**
		 * A coroutine which has yielded.
		 */
		SUSPENDED("suspended"),

		/**
		 * A coroutine which is currently running.
		 */
		RUNNING("running"),

		/**
		 * A coroutine which has resumed another coroutine.
		 */
		NORMAL("normal"),

		/**
		 * A coroutine which has finished executing.
		 */
		DEAD("dead");

		private final String name;
		private final LuaValue nameValue;

		Status(String name) {
			this.name = name;
			nameValue = ValueFactory.valueOf(name);
		}

		public String getDisplayName() {
			return name;
		}

		public LuaValue getDisplayNameValue() {
			return nameValue;
		}
	}

	/**
	 * The state that this thread lives in
	 */
	private final LuaState luaState;

	/**
	 * The current status of this thread
	 */
	private Status status;

	/**
	 * The function called when handling errors
	 */
	private LuaValue errFunc;

	/**
	 * Used by DebugLib to store debugging state.
	 */
	private final DebugState debugState;

	/**
	 * The main function for this thread
	 */
	private final LuaFunction function;

	/**
	 * The thread which resumed this one, and so should be resumed back into.
	 */
	private LuaThread previousThread;

	/**
	 * Constructor for main thread only
	 *
	 * @param state The current lua state
	 */
	public LuaThread(LuaState state) {
		super(Constants.TTHREAD);
		Objects.requireNonNull(state, "state cannot be null");

		status = Status.RUNNING;
		luaState = state;
		debugState = new DebugState(state);
		function = null;
	}

	/**
	 * Create a LuaThread around a function and environment
	 *
	 * @param state The current lua state
	 * @param func  The function to execute
	 */
	public LuaThread(LuaState state, LuaFunction func) {
		super(Constants.TTHREAD);
		Objects.requireNonNull(state, "state cannot be null");
		Objects.requireNonNull(func, "func cannot be null");

		status = Status.INITIAL;
		luaState = state;
		debugState = new DebugState(state);
		function = func;

		LuaThread current = state.getCurrentThread();
		if (current != null && current.debugState.getHook() != null && current.debugState.getHook().inheritHook()) {
			debugState.setHook(
				current.debugState.getHook(),
				current.debugState.hasCallHook(),
				current.debugState.hasLineHook(),
				current.debugState.hasReturnHook(),
				current.debugState.hookCount
			);
		}
	}

	@Override
	public LuaThread checkThread() {
		return this;
	}

	@Override
	public LuaTable getMetatable(LuaState state) {
		return state.threadMetatable;
	}

	public Status getStatus() {
		return status;
	}

	/**
	 * Test if this is the main thread
	 *
	 * @return true if this is the main thread
	 */
	public boolean isMainThread() {
		return luaState.getMainThread() == this;
	}

	public boolean isAlive() {
		return status != Status.DEAD;
	}

	/**
	 * Get the function called as a specific location on the stack.
	 *
	 * @param state The current lua state
	 * @param level 1 for the function calling this one, 2 for the next one.
	 * @return LuaFunction on the call stack, or null if outside of range of active stack
	 */
	public static LuaFunction getCallstackFunction(LuaState state, int level) {
		DebugFrame info = DebugState.get(state).getFrame(level);
		return info == null ? null : info.func;
	}

	/**
	 * Get the debug state for this thread
	 *
	 * @return This thread's debug state
	 * @see DebugState#get(LuaState)
	 */
	public DebugState getDebugState() {
		return debugState;
	}

	/**
	 * Get this thread's error handling function
	 *
	 * @return The error handling function, or {@code null} if not defined.
	 * @see #setErrorFunc(LuaValue)
	 * @see LuaError
	 */
	public LuaValue getErrorFunc() {
		return errFunc;
	}

	/**
	 * Replace the error function of this thread.
	 *
	 * @param errFunc the new error function to use.
	 * @return the previous error function.
	 * @see #getErrorFunc()
	 */
	public LuaValue setErrorFunc(LuaValue errFunc) {
		LuaValue prev = this.errFunc;
		this.errFunc = errFunc;
		return prev;
	}

	/**
	 * Yield the current thread with arguments
	 *
	 * @param state The current lua state
	 * @param args  The arguments to send as return values to {@link #resume(LuaState, LuaThread, Varargs)}
	 * @return Will never return.
	 * @throws LuaError        If attempting to yield the main thread.
	 * @throws UnwindThrowable If we can yield this stack with an exception.
	 */
	public static <T> T yield(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
		Objects.requireNonNull(args, "args cannot be null");

		LuaThread thread = state.currentThread;
		if (thread.status != Status.RUNNING) {
			throw new LuaError("cannot yield a " + thread.status.getDisplayName() + " thread");
		}
		if (thread.isMainThread()) throw new LuaError("cannot yield main thread");

		throw UnwindThrowable.yield(args);
	}

	/**
	 * Resume a thread with arguments.
	 *
	 * @param state  The current lua state
	 * @param thread The thread to resume
	 * @param args   The arguments to resume with
	 * @return Will never return.
	 * @throws LuaError        If this coroutine cannot resume another.
	 * @throws UnwindThrowable If we can yield this stack with an exception
	 */
	public static <T> T resume(LuaState state, LuaThread thread, Varargs args) throws LuaError, UnwindThrowable {
		LuaThread current = state.currentThread;
		if (current.status != Status.RUNNING) {
			throw new LuaError("cannot resume from a " + current.status.getDisplayName() + " thread");
		}

		if (thread.status.ordinal() > Status.SUSPENDED.ordinal()) {
			throw new LuaError("cannot resume " + thread.status.getDisplayName() + " coroutine");
		}

		throw UnwindThrowable.resume(thread, args);
	}

	public static Varargs runMain(LuaState state, LuaFunction function) throws LuaError {
		return run(state, state.getMainThread(), function, Constants.NONE);
	}

	public static Varargs runMain(LuaState state, LuaFunction function, Varargs args) throws LuaError {
		return run(state, state.getMainThread(), function, args);
	}

	/**
	 * Start or resume this thread
	 *
	 * @param thread The thread to resume
	 * @param args   The arguments to send as return values to {@link #yield(LuaState, Varargs)}
	 * @return {@link Varargs} provided as arguments to {@link #yield(LuaState, Varargs)}
	 * @throws LuaError If the current function threw an exception.
	 */
	public static Varargs run(LuaThread thread, Varargs args) throws LuaError {
		return run(thread.luaState, thread, null, args);
	}

	private static Varargs run(final LuaState state, LuaThread thread, LuaFunction function, Varargs args) throws LuaError {
		return loop(state, thread, function, args);
	}

	private static Varargs loop(final LuaState state, LuaThread thread, LuaFunction function, Varargs args) throws LuaError {
		LuaError le = null;
		do {
			final DebugState ds = thread.debugState;
			state.currentThread = thread;
			if (thread.status == Status.INITIAL && function == null) function = thread.function;

			try {
				if (function != null) {
					thread.status = Status.RUNNING;

					// We only want to execute the function the first time, so null it out
					LuaFunction toExecute = function;
					function = null;

					try {
						args = Dispatch.invoke(state, toExecute, args);
					} catch (Exception | VirtualMachineError e) {
						args = null;
						le = LuaError.wrap(e);
					}
				} else {
					thread.status = Status.RUNNING;

					outer:
					while (true) {
						try {
							if (le != null) {
								// If we've an error, walk up the stack until we find an error handler and resume.
								DebugFrame frame = findErrorHandler(ds);
								if (frame == null) break;

								// We need to set the error to null first so we don't continue to propogate this error.
								LuaError err = le;
								le = null;
								args = ds.resumeError(frame, err);
							}

							while (true) {
								DebugFrame frame = ds.getStack();
								if (frame == null) break outer;

								// If this frame errored, then we've just exited an error handler: look up the frame
								// for what to return into.
								if ((frame.flags & FLAG_ERROR) != 0) {
									frame = findErrorHandler(ds);
									if (frame == null) break;
								}

								args = ds.resume(frame, args);
							}
						} catch (Exception | VirtualMachineError e) {
							args = null;
							le = LuaError.wrap(e);
						}
					}
				}

				// We've died, yield into the parent coroutine
				thread.status = Status.DEAD;
				LuaThread previous = thread.previousThread;
				thread.previousThread = null;
				thread = previous;

				if (le != null) {
					// If we've an error, fill in the debug info and clean up the stack.
					le.fillTraceback(state);

					DebugFrame di;
					for (int i = 0; (di = ds.getFrame(i)) != null; i++) di.cleanup();
				}
			} catch (UnwindThrowable e) {
				if (e.isSuspend()) {
					thread.status = Status.SUSPENDED;
					return null;
				} else if (e.isYield()) {
					// Yield into the parent coroutine
					thread.status = Status.SUSPENDED;
					LuaThread previous = thread.previousThread;
					thread.previousThread = null;
					thread = previous;
					args = e.getArgs();
				} else {
					// Resume into the next coroutine
					thread.status = Status.NORMAL;
					LuaThread next = e.getThread();
					next.previousThread = state.currentThread;
					thread = next;
					args = e.getArgs();
				}
			}
		} while (thread != null);

		if (le != null) throw le;
		return args;
	}

	private static DebugFrame findErrorHandler(DebugState ds) {
		for (int i = 0; ; i++) {
			DebugFrame frame = ds.getFrame(i);
			if (frame == null || (frame.flags & FLAG_YPCALL) != 0) return frame;
		}
	}
}
