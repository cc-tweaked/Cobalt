/**
 * ****************************************************************************
 * Copyright (c) 2007-2012 LuaJ. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
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

import org.squiddev.cobalt.lib.CoroutineLib;
import org.squiddev.cobalt.lib.DebugLib;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.lang.ref.WeakReference;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Subclass of {@link LuaValue} that implements
 * a lua coroutine thread using Java Threads.
 * <p>
 * A LuaThread is typically created in response to a scripted call to
 * {@code coroutine.create()}
 * <p>
 * The utility class {@link JsePlatform}
 * sees to it that this initialization is done properly.
 * For this reason it is highly recommended to use one of these classes
 * when initializing globals.
 * <p>
 * The behavior of coroutine threads matches closely the behavior
 * of C coroutine library.  However, because of the use of Java threads
 * to manage call state, it is possible to yield from anywhere in luaj.
 * <p>
 * Each Java thread wakes up at regular intervals and checks a weak reference
 * to determine if it can ever be resumed.  If not, it throws
 * {@link OrphanedThread} which is an {@link java.lang.Error}.
 * Applications should not catch {@link OrphanedThread}, because it can break
 * the thread safety of luaj.
 *
 * @see LuaValue
 * @see JsePlatform
 * @see CoroutineLib
 */
public class LuaThread extends LuaValue {
	private static int coroutineCount = 0;

	/**
	 * Interval at which to check for lua threads that are no longer referenced.
	 * This can be changed by Java startup code if desired.
	 */
	public static long threadOrphanCheckInterval = 30000;

	private static final int STATUS_INITIAL = 0;
	private static final int STATUS_SUSPENDED = 1;
	private static final int STATUS_RUNNING = 2;
	private static final int STATUS_NORMAL = 3;
	private static final int STATUS_DEAD = 4;
	private static final String[] STATUS_NAMES = {
		"suspended",
		"suspended",
		"running",
		"normal",
		"dead",
	};

	private LuaValue env;
	private final State state;

	/**
	 * Field to hold state of error condition during debug hook function calls.
	 */
	public LuaValue err;

	private final CallStack callstack;

	public static final int MAX_CALLSTACK = 256;

	public final LuaState luaState;

	/**
	 * Thread-local used by DebugLib to store debugging state.
	 */
	public DebugLib.DebugState debugState;

	/**
	 * Constructor for main thread only
	 *
	 * @param luaState The current lua state
	 * @param env      The thread's environment
	 */
	public LuaThread(LuaState luaState, LuaValue env) {
		super(Constants.TTHREAD);

		luaState.threads.add(this);
		this.luaState = luaState;
		this.env = env;

		state = new State(this, null);
		state.status = STATUS_RUNNING;
		callstack = new CallStack(luaState);
	}

	/**
	 * Create a LuaThread around a function and environment
	 *
	 * @param luaState The current lua state
	 * @param func     The function to execute
	 * @param env      The environment to apply to the thread
	 */
	public LuaThread(LuaState luaState, LuaValue func, LuaValue env) {
		super(Constants.TTHREAD);
		if (func == null) throw new LuaError("function cannot be null");

		luaState.threads.add(this);
		this.luaState = luaState;
		this.env = env;

		state = new State(this, func);
		callstack = new CallStack(luaState);
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
	public LuaValue getMetatable(LuaState state) {
		return state.threadMetatable;
	}

	@Override
	public LuaValue getfenv() {
		return env;
	}

	@Override
	public void setfenv(LuaValue env) {
		this.env = env;
	}

	public String getStatus() {
		return STATUS_NAMES[state.status];
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
		return state.status != STATUS_DEAD;
	}

	/**
	 * Callback used at the beginning of a call to prepare for possible getfenv/setfenv calls
	 *
	 * @param state    The current lua state
	 * @param function Function being called
	 * @return CallStack which is used to signal the return or a tail-call recursion
	 * @see DebugLib
	 */
	public static CallStack onCall(LuaState state, LuaFunction function) {
		CallStack cs = state.currentThread.callstack;
		cs.onCall(function);
		return cs;
	}

	/**
	 * Get the function called as a specific location on the stack.
	 *
	 * @param state The current lua state
	 * @param level 1 for the function calling this one, 2 for the next one.
	 * @return LuaFunction on the call stack, or null if outside of range of active stack
	 */
	public static LuaFunction getCallstackFunction(LuaState state, int level) {
		return state.currentThread.callstack.getFunction(level);
	}

	/**
	 * Replace the error function of the currently running thread.
	 *
	 * @param state   The current lua state
	 * @param errfunc the new error function to use.
	 * @return the previous error function.
	 */
	public static LuaValue setErrorFunc(LuaState state, LuaValue errfunc) {
		LuaValue prev = state.currentThread.err;
		state.currentThread.err = errfunc;
		return prev;
	}

	/**
	 * Yield the current thread with arguments
	 *
	 * @param state The current lua state
	 * @param args  The arguments to send as return values to {@link #resume(Varargs)}
	 * @return {@link Varargs} provided as arguments to {@link #resume(Varargs)}
	 */
	public static Varargs yield(LuaState state, Varargs args) {
		State s = state.currentThread.state;
		if (s.function == null) {
			throw new LuaError("cannot yield main thread");
		}
		return s.yield(args);
	}

	public void abandon() {
		if (state.status == STATUS_DEAD) {
			luaState.threads.remove(this);
		} else if (isMainThread()) {
			// Can't do anything if the main thread
			return;
		} else if (state.status > STATUS_SUSPENDED) {
			throw new IllegalStateException("Cannot abandon " + getStatus() + " coroutine");
		} else {
			state.abandon();

			if (luaState.threads.contains(this)) {
				throw new IllegalStateException("Abandonment failed");
			}
		}
	}

	/**
	 * Start or resume this thread
	 *
	 * @param args The arguments to send as return values to {@link #yield(LuaState, Varargs)}
	 * @return {@link Varargs} provided as arguments to {@link #yield(LuaState, Varargs)}
	 */
	public Varargs resume(Varargs args) {
		if (state.status > STATUS_SUSPENDED) {
			return varargsOf(Constants.FALSE, valueOf("cannot resume " + LuaThread.STATUS_NAMES[this.state.status] + " coroutine"));
		}
		return state.resume(this, args);
	}

	private static class State implements Runnable {
		private final LuaState state;
		private final WeakReference<LuaThread> thread;
		private final LuaValue function;
		private Varargs args = Constants.NONE;
		private Varargs result = Constants.NONE;
		private String error = null;
		protected int status = LuaThread.STATUS_INITIAL;
		private boolean abandoned = false;

		private State(LuaThread thread, LuaValue function) {
			this.state = thread.luaState;
			this.thread = new WeakReference<LuaThread>(thread);
			this.function = function;
		}

		@Override
		public synchronized void run() {
			try {
				Varargs a = args;
				args = Constants.NONE;
				result = function.invoke(state, a);
			} catch (Throwable t) {
				error = t.getMessage();
			} finally {
				markDead();
				notify();
			}
		}

		protected synchronized Varargs resume(LuaThread newThread, Varargs args) {
			LuaThread previous = state.currentThread;
			try {
				state.currentThread = newThread;
				this.args = args;
				if (status == STATUS_INITIAL) {
					status = STATUS_RUNNING;
					new Thread(this, "Coroutine-" + (++coroutineCount)).start();
				} else {
					notify();
				}
				previous.state.status = STATUS_NORMAL;
				status = STATUS_RUNNING;
				wait();
				return (error != null ?
					varargsOf(Constants.FALSE, valueOf(error)) :
					varargsOf(Constants.TRUE, result));

			} catch (InterruptedException ie) {
				throw new OrphanedThread();
			} finally {
				state.currentThread = previous;
				state.currentThread.state.status = STATUS_RUNNING;

				this.args = Constants.NONE;
				result = Constants.NONE;
				error = null;
			}
		}

		protected synchronized Varargs yield(Varargs args) {
			try {
				result = args;
				status = STATUS_SUSPENDED;
				notify();
				do {
					wait(threadOrphanCheckInterval);
					if (abandoned || thread.get() == null) {
						markDead();
						throw new OrphanedThread();
					}
				} while (status == STATUS_SUSPENDED);
				return this.args;
			} catch (InterruptedException ie) {
				markDead();
				throw new OrphanedThread();
			} finally {
				this.args = Constants.NONE;
				result = Constants.NONE;
			}
		}

		protected synchronized void abandon() {
			LuaThread currentThread = state.currentThread;
			try {
				currentThread.state.status = STATUS_NORMAL;
				abandoned = true;
				if (status == STATUS_INITIAL) {
					markDead();
				} else {
					notify();
					wait();
				}
			} catch (InterruptedException e) {
				markDead();
			} finally {
				currentThread.state.status = STATUS_RUNNING;
				args = Constants.NONE;
				result = Constants.NONE;
				error = null;
			}
		}

		private void markDead() {
			status = STATUS_DEAD;
			LuaThread current = thread.get();
			if(current != null) state.threads.remove(current);
		}
	}

	public static class CallStack {
		private final LuaState state;
		private final LuaFunction[] functions = new LuaFunction[MAX_CALLSTACK];
		private int calls = 0;

		public CallStack(LuaState state) {
			this.state = state;
		}

		/**
		 * Method to indicate the start of a call
		 *
		 * @see DebugLib
		 */
		final void onCall(LuaFunction function) {
			functions[calls++] = function;
			DebugLib.debugOnCall(state.currentThread, calls, function);
		}

		/**
		 * Method to signal the end of a call
		 *
		 * @see DebugLib
		 */
		public final void onReturn() {
			functions[--calls] = null;
			DebugLib.debugOnReturn(state.currentThread, calls);
		}

		/**
		 * Get the function at a particular level of the stack.
		 *
		 * @param level # of levels back from the top of the stack.
		 * @return LuaFunction, or null if beyond the stack limits.
		 */
		LuaFunction getFunction(int level) {
			return level > 0 && level <= calls ? functions[calls - level] : null;
		}
	}
}
