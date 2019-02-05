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

import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.CoroutineLib;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.lang.ref.WeakReference;

import static org.squiddev.cobalt.debug.DebugFrame.FLAG_ERROR;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_YPCALL;

/**
 * Subclass of {@link LuaValue} that implements a lua coroutine thread.
 *
 * A LuaThread is typically created in response to a scripted call to
 * {@code coroutine.create()}
 *
 * The utility class {@link JsePlatform}
 * sees to it that this initialization is done properly.
 * For this reason it is highly recommended to use one of these classes
 * when initializing globals.
 *
 * The behavior of coroutine threads matches closely the behavior
 * of C coroutine library.  However, because of the use of Java threads
 * to manage call state, it is possible to yield from anywhere in luaj.
 *
 * @see LuaValue
 * @see JsePlatform
 * @see CoroutineLib
 */
public class LuaThread extends LuaValue {
	/**
	 * A coroutine which has been run at all
	 */
	private static final int STATUS_INITIAL = 0;

	/**
	 * A coroutine which has yielded
	 */
	private static final int STATUS_SUSPENDED = 1;

	/**
	 * A coroutine which is currently running
	 */
	private static final int STATUS_RUNNING = 2;

	/**
	 * A coroutine which has resumed another coroutine
	 */
	private static final int STATUS_NORMAL = 3;

	/**
	 * A coroutine which has finished executing
	 */
	private static final int STATUS_DEAD = 4;

	private static final String[] STATUS_NAMES = {
		"suspended",
		"suspended",
		"running",
		"normal",
		"dead",
	};

	private LuaTable env;

	/**
	 * The function called when handling errors
	 */
	public LuaValue errFunc;

	/**
	 * The state that this thread lives in
	 */
	public final LuaState luaState;

	/**
	 * The main function for this thread
	 */
	private final LuaFunction function;

	/**
	 * The current status of this thread
	 */
	private int status;

	/**
	 * Used by DebugLib to store debugging state.
	 */
	public DebugState debugState;

	/**
	 * The thread which resumed this one, and so should be resumed back into.
	 */
	private LuaThread previousThread;

	/**
	 * The depth of the Java blocks. Yielding/resuming is only allowed when this is 0.
	 */
	private int javaCount = 0;

	/**
	 * A weak reference to this thread
	 */
	private WeakReference<LuaThread> reference;

	/**
	 * Constructor for main thread only
	 *
	 * @param state The current lua state
	 * @param env   The thread's environment
	 */
	public LuaThread(LuaState state, LuaTable env) {
		super(Constants.TTHREAD);

		this.luaState = state;
		this.env = env;
		this.function = null;
		this.status = STATUS_RUNNING;
	}

	/**
	 * Create a LuaThread around a function and environment
	 *
	 * @param luaState The current lua state
	 * @param func     The function to execute
	 * @param env      The environment to apply to the thread
	 */
	public LuaThread(LuaState luaState, LuaFunction func, LuaTable env) {
		super(Constants.TTHREAD);
		if (func == null) throw new IllegalArgumentException("function cannot be null");

		this.luaState = luaState;
		this.env = env;
		this.function = func;
		this.status = STATUS_INITIAL;
	}

	@Override
	public LuaThread optThread(LuaThread defval) {
		return this;
	}

	@Override
	public LuaThread checkThread() {
		return this;
	}

	@Override
	public LuaTable getMetatable(LuaState state) {
		return state.threadMetatable;
	}

	@Override
	public LuaTable getfenv() {
		return env;
	}

	@Override
	public void setfenv(LuaTable env) {
		this.env = env;
	}

	public String getStatus() {
		return STATUS_NAMES[status];
	}

	/**
	 * Test if this is the main thread
	 *
	 * @return true if this is the main thread
	 */
	public boolean isMainThread() {
		return luaState.mainThread == this;
	}

	public boolean isAlive() {
		return status != STATUS_DEAD;
	}

	/**
	 * Prevent code from yielding
	 */
	public void disableYield() {
		javaCount++;
	}

	/**
	 * Allow code to yield again
	 */
	public void enableYield() {
		javaCount--;
	}

	/**
	 * Get a weak reference to this thread.
	 *
	 * This may be useful if you want to run a computation on a separate thread and need to
	 * determine if it is still alive.
	 *
	 * @return A weak reference to this thread.
	 */
	public WeakReference<LuaThread> getReference() {
		WeakReference<LuaThread> reference = this.reference;
		return reference == null ? (this.reference = new WeakReference<>(this)) : reference;
	}

	/**
	 * Get the function called as a specific location on the stack.
	 *
	 * @param state The current lua state
	 * @param level 1 for the function calling this one, 2 for the next one.
	 * @return LuaFunction on the call stack, or null if outside of range of active stack
	 */
	public static LuaFunction getCallstackFunction(LuaState state, int level) {
		DebugFrame info = DebugHandler.getDebugState(state.currentThread).getFrame(level);
		return info == null ? null : info.func;
	}

	/**
	 * Replace the error function of this thread.
	 *
	 * @param errFunc the new error function to use.
	 * @return the previous error function.
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
	 * @throws LuaError        If attempting to yield the main thread.
	 * @throws UnwindThrowable To signal the yield.
	 */
	public static UnwindThrowable yield(LuaState state, Varargs args) throws LuaError {
		LuaThread current = state.currentThread;

		if (current.status != STATUS_RUNNING) {
			throw new LuaError("cannot yield a " + STATUS_NAMES[current.status] + " thread");
		}
		if (current.isMainThread()) throw new LuaError("cannot yield main thread");
		if (current.javaCount != 0) throw new LuaError("cannot yield across call boundary");

		return UnwindThrowable.yield(args);
	}

	/**
	 * Resume a thread with arguments.
	 *
	 * @param state  The current lua state
	 * @param thread The thread to resume
	 * @param args   The arguments to resume with
	 * @throws LuaError If this coroutine cannot resume another.
	 */
	public static UnwindThrowable resume(LuaState state, LuaThread thread, Varargs args) throws LuaError {
		LuaThread current = state.currentThread;
		if (current.status != STATUS_RUNNING) {
			throw new LuaError("cannot resume from a " + STATUS_NAMES[current.status] + " thread");
		}

		if (thread.status > STATUS_SUSPENDED) {
			throw new LuaError("cannot resume " + STATUS_NAMES[thread.status] + " coroutine");
		}

		// TODO: Remove this limitation
		if (current.javaCount != 0) throw new LuaError("cannot resume within call boundary");

		return UnwindThrowable.resume(thread, args);
	}

	public static Varargs runMain(LuaState state, LuaFunction function) throws LuaError {
		return run(state, state.mainThread, function, Constants.NONE);
	}

	public static Varargs runMain(LuaState state, LuaFunction function, Varargs args) throws LuaError {
		return run(state, state.mainThread, function, args);
	}

	/**
	 * Start or resume this thread
	 *
	 * @param state The lua state we are executing in
	 * @param args  The arguments to send as return values to {@link #yield(LuaState, Varargs)}
	 * @return {@link Varargs} provided as arguments to {@link #yield(LuaState, Varargs)}
	 * @throws LuaError If the current function threw an exception.
	 */
	public static Varargs run(LuaState state, Varargs args) throws LuaError {
		return run(state, state.currentThread, null, args);
	}

	public static Varargs run(LuaThread thread, Varargs args) throws LuaError {
		return run(thread.luaState, thread, null, args);
	}

	private static Varargs run(final LuaState state, LuaThread thread, LuaFunction function, Varargs args) throws LuaError {
		LuaError le = null;
		do {
			final DebugState ds = DebugHandler.getDebugState(thread);
			try {
				state.currentThread = thread;
				if (thread.status == STATUS_INITIAL || function != null) {
					thread.status = STATUS_RUNNING;

					LuaFunction toExecute = thread.function;
					if (toExecute == null) {
						// We only want to execute the function the first time
						toExecute = function;
						function = null;
					}

					try {
						args = toExecute.invoke(state, args);
					} catch (Exception e) {
						args = null;
						le = e instanceof LuaError ? (LuaError) e : new LuaError(e);
					}
				} else {
					thread.status = STATUS_RUNNING;

					outer:
					while (true) {
						try {
							if (le != null) {
								// If we've an error, walk up the stack until we find an error handler and resume.
								DebugFrame frame = findErrorHandler(ds);
								if (frame == null) break;

								// We need to set the error to null first so we don't count propagate errors across
								// yields
								LuaError err = le;
								le = null;
								args = frame.resumeError(state, err);
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

								args = frame.resume(state, args);
								if (args == null) System.out.println("Got null from " + frame);
							}
						} catch (Exception e) {
							args = null;
							le = e instanceof LuaError ? (LuaError) e : new LuaError(e);
						}
					}
				}

				// We've died, yield into the parent coroutine
				thread.status = STATUS_DEAD;
				LuaThread previous = thread.previousThread;
				thread.previousThread = null;
				thread = previous;

				if (le != null) {
					// If we've an error, fill in the debug info and clean up the stack.
					le.fillTracebackNoHandler(state);

					DebugFrame di;
					for (int i = 0; (di = ds.getFrame(i)) != null; i++) di.cleanup();
				}
			} catch (UnwindThrowable e) {
				if (e.isSuspend()) {
					thread.status = STATUS_SUSPENDED;
					return Constants.NONE;
				} else if (e.isYield()) {
					// Yield into the parent coroutine
					thread.status = STATUS_SUSPENDED;
					LuaThread previous = thread.previousThread;
					thread.previousThread = null;
					thread = previous;
					args = e.getArgs();
				} else {
					// Resume into the next coroutine
					thread.status = STATUS_NORMAL;
					LuaThread next = e.getThread();
					next.previousThread = thread;
					thread = next;
					args = e.getArgs();
					if (args == null) System.out.println("Got null from resume");
				}
			}
		} while (thread != null);

		if (le != null) throw le;
		return args;
	}

	private static DebugFrame findErrorHandler(DebugState ds) {
		for (int i = 0; ; i++) {
			DebugFrame frame = ds.getFrame(i);
			if (frame == null) return null;
			if ((frame.flags & FLAG_YPCALL) != 0) return frame;
		}
	}
}
