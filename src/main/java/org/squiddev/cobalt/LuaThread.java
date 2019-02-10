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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

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
	 * Interval in nanoseconds at which to check for lua threads that are no longer referenced.
	 * This can be changed by Java startup code if desired.
	 */
	public static long orphanCheckInterval = TimeUnit.MILLISECONDS.toNanos(30000);

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
	public final DebugState debugState;

	/**
	 * The thread which resumed this one, and so should be resumed back into.
	 */
	private LuaThread previousThread;

	/**
	 * The depth of the Java blocks. Yielding/resuming is only allowed when this is 0.
	 */
	private int javaCount = 0;

	/**
	 * The lock to wait on while this coroutine is suspended as a thread
	 */
	private Condition resumeLock;

	/**
	 * Whether we've yielded in a threaded manner.
	 */
	private boolean needsThreadedResume;

	/**
	 * Constructor for main thread only
	 *
	 * @param state The current lua state
	 * @param env   The thread's environment
	 */
	public LuaThread(LuaState state, LuaTable env) {
		super(Constants.TTHREAD);

		this.luaState = state;
		this.debugState = new DebugState(state);
		this.env = env;
		this.function = null;
		this.status = STATUS_RUNNING;
	}

	/**
	 * Create a LuaThread around a function and environment
	 *
	 * @param state The current lua state
	 * @param func  The function to execute
	 * @param env   The environment to apply to the thread
	 */
	public LuaThread(LuaState state, LuaFunction func, LuaTable env) {
		super(Constants.TTHREAD);
		if (func == null) throw new IllegalArgumentException("function cannot be null");

		this.luaState = state;
		this.env = env;
		this.debugState = new DebugState(state);
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
		return luaState.getMainThread() == this;
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
	 * Get the function called as a specific location on the stack.
	 *
	 * @param state The current lua state
	 * @param level 1 for the function calling this one, 2 for the next one.
	 * @return LuaFunction on the call stack, or null if outside of range of active stack
	 */
	public static LuaFunction getCallstackFunction(LuaState state, int level) {
		DebugFrame info = DebugHandler.getDebugState(state).getFrame(level);
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
	 * @return An exception which can unwind this trace. This should be thrown immediately.
	 * @throws LuaError If attempting to yield the main thread.
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
	 * Yield the current thread and wait for a response
	 *
	 * @param state The current lua state
	 * @param args  The arguments to send as return values to {@link #resume(LuaState, LuaThread, Varargs)}
	 * @return The values this coroutine was resumed with
	 * @throws LuaError             If this thread cannot be yielded.
	 * @throws InterruptedException If this thread was terminated when yielding.
	 */
	public static Varargs yieldBlocking(LuaState state, Varargs args) throws LuaError, InterruptedException {
		Objects.requireNonNull(args, "args cannot be null");

		YieldThreader threader = state.threader;
		if (threader == null) throw new IllegalStateException("Not running with a YieldThreader");

		LuaThread thread = state.currentThread;
		if (thread.status != STATUS_RUNNING) {
			throw new LuaError("cannot yield a " + STATUS_NAMES[thread.status] + " thread");
		}
		if (thread.isMainThread()) throw new LuaError("cannot yield main thread");

		// Mark the parent coroutine as "active", and yield.
		state.currentThread = thread.previousThread;
		thread.status = STATUS_SUSPENDED;
		thread.previousThread = null;

		// Construct a lock to wait on.
		if (thread.resumeLock == null) thread.resumeLock = threader.lock.newCondition();

		threader.lock.lockInterruptibly();
		try {
			// Give the runner a signal, and start it off.
			thread.needsThreadedResume = true;
			threader.set(args);
			threader.loop.signal();

			// Wait for us to be resumed.
			// TODO: Should we switch back to LuaThreads, so we can do WeakReferences too?
			while (thread.resumeLock.awaitNanos(orphanCheckInterval) <= 0) {
				if (state.abandoned) throw new InterruptedException("Abandoned thread");
			}

			return threader.unpack();
		} finally {
			threader.lock.unlock();
			thread.needsThreadedResume = false;
		}
	}

	/**
	 * Resume a thread with arguments.
	 *
	 * @param state  The current lua state
	 * @param thread The thread to resume
	 * @param args   The arguments to resume with
	 * @return An exception to resume another coroutine. This should be thrown immediately.
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

		if (current.javaCount != 0) throw new LuaError("cannot resume within call boundary");

		return UnwindThrowable.resume(thread, args);
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
		YieldThreader threader = state.threader;
		if (threader == null) {
			try {
				return loop(state, thread, function, args);
			} catch (TransferredControlThrowable e) {
				throw new IllegalStateException("Should never have thrown TransferredControlThrowable", e);
			}
		}


		threader.lock.lock();
		try {
			// First, set up the initial state
			state.currentThread = thread;
			threader.set(args);
			threader.running = true;

			Runnable task = new Runnable() {
				LuaFunction func = function;

				@Override
				public void run() {
					try {
						threader.lock.lockInterruptibly();

						try {
							// Clear the function after the first run
							LuaFunction function = func;
							func = null;

							Varargs res = loop(state, state.currentThread, function, threader.unpack());

							// Loop returned a value, which means the top-level coroutine yielded or terminated.
							threader.set(res);
							threader.running = false;
							threader.loop.signal();
						} catch (TransferredControlThrowable ignored) {
							// Just die here: someone else is running now, but the coroutines are still being executed.
						} catch (Throwable e) {
							// Loop threw a LuaError (the top-level coroutine errored) or threw an unknown exception
							// (terminate everything).
							threader.set(e);
							threader.running = false;
							threader.loop.signal();
						} finally {
							threader.lock.unlock();
						}
					} catch (InterruptedException ignored) {
						Thread.currentThread().interrupt();
					}
				}
			};

			while (threader.running && state.currentThread != null) {
				threader.execute(task);
				threader.loop.await();
			}

			return threader.unpack();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while waiting for coroutine", e);
		} finally {
			threader.lock.unlock();
		}
	}

	private static Varargs loop(final LuaState state, LuaThread thread, LuaFunction function, Varargs args) throws LuaError, TransferredControlThrowable {
		YieldThreader threader = state.threader;

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
				} else if (thread.needsThreadedResume) {
					// We only ever resume coroutines which have yielded, never those which have
					// resumed other coroutines. Consequently, we know we will never have an error here.
					if (le != null) {
						throw new IllegalStateException("Cannot resume a threaded coroutine with an error.");
					}

					// Store the arguments in threader, and resume this thread.
					threader.set(args);
					thread.status = STATUS_RUNNING;
					thread.resumeLock.signal();

					throw TransferredControlThrowable.INSTANCE;
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
					return null;
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

	/**
	 * Used inside {@link #loop(LuaState, LuaThread, LuaFunction, Varargs)} when
	 * this particular thread has transferred control elsewhere.
	 */
	private static class TransferredControlThrowable extends Throwable {
		private static final long serialVersionUID = 6854182520592525282L;
		static final TransferredControlThrowable INSTANCE = new TransferredControlThrowable();

		private TransferredControlThrowable() {
			super(null, null, true, false);
		}
	}
}
