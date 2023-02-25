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

import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugHelpers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

/**
 * Global lua state
 */
public final class LuaState {
	/**
	 * The metatable for all strings
	 */
	public LuaTable stringMetatable;

	/**
	 * The metatable for all booleans
	 */
	public LuaTable booleanMetatable;

	/**
	 * The metatable for all numbers
	 */
	public LuaTable numberMetatable;

	/**
	 * The metatable for all nil values
	 */
	public LuaTable nilMetatable;

	/**
	 * The metatable for all functions
	 */
	public LuaTable functionMetatable;

	/**
	 * The metatable for all threads
	 */
	public LuaTable threadMetatable;

	/**
	 * The compiler for this threstate
	 */
	public final LoadState.FunctionFactory compiler;

	/**
	 * The handler for the debugger. Override this for custom debug actions.
	 */
	public final DebugHandler debug;

	/**
	 * The currently executing thread
	 */
	LuaThread currentThread;

	/**
	 * The currently executing main thread
	 */
	private final LuaThread mainThread;

	/**
	 * The currently active {@link YieldThreader}.
	 */
	final YieldThreader threader;

	/**
	 * If this state has been abandoned, and threads should be cleaned up.
	 *
	 * @see LuaThread#orphanCheckInterval
	 */
	boolean abandoned;

	/**
	 * Report an internal VM error.
	 */
	private final ErrorReporter reportError;

	private final GlobalRegistry registry = new GlobalRegistry();

	public LuaState() {
		this(new LuaState.Builder());
	}

	private LuaState(Builder builder) {
		compiler = builder.compiler;
		debug = builder.debug;
		threader = new YieldThreader(builder.coroutineExecutor);
		reportError = builder.reportError;

		mainThread = currentThread = new LuaThread(this, new LuaTable());
	}

	/**
	 * Get the global registry, a Lua table used to store Lua values.
	 *
	 * @return The global debug registry.
	 */
	public GlobalRegistry registry() {
		return registry;
	}


	/**
	 * Abandon this state, instructing any pending thread to terminate.
	 */
	public void abandon() {
		abandoned = true;
	}

	/**
	 * Get the main thread
	 *
	 * @return The main thread
	 * @see #getCurrentThread()
	 */
	public LuaThread getMainThread() {
		return mainThread;
	}

	/**
	 * The active thread
	 *
	 * @return The active thread
	 * @see #getMainThread()
	 */
	public LuaThread getCurrentThread() {
		return currentThread;
	}

	/**
	 * Print some information about the internal execution state.
	 * <p>
	 * This includes the current Lua coroutine trace and the native Java stacktrace for all threads currently associated
	 * with the VM.
	 * <p>
	 * This function is purely intended for debugging, its output should not be relied on in any way.
	 *
	 * @param out The buffer to write to.
	 */
	public void printExecutionState(StringBuilder out) {
		LuaThread currentThread = this.currentThread;
		if (currentThread != null) {
			out.append("Current coroutine: ").append(currentThread.state).append("\n");
			try {
				DebugHelpers.traceback(out, currentThread, 0);
			} catch (RuntimeException e) {
				// This function will be called from a separate thread, so the stack could unwind from under us. Catch
				// any possible out-of-bounds/NPEs.
				out.append("Failed printing current coroutine (").append(e).append(")");
			}

			out.append("\n");
		}

		for (Thread thread : threader.threads) {
			out.append("Thread ").append(thread.getName()).append(" is currently ").append(thread.getState()).append('\n');

			Object blocking = LockSupport.getBlocker(thread);
			if (blocking != null) out.append("  on ").append(blocking).append('\n');

			for (StackTraceElement element : thread.getStackTrace()) {
				out.append("  at ").append(element).append('\n');
			}
		}
	}

	@Deprecated
	public void reportInternalError(Throwable error) {
		if (reportError != null) reportError.report(error, () -> "Uncaught Java exception");
	}

	public void reportInternalError(Throwable error, Supplier<String> message) {
		if (reportError != null) reportError.report(error, message);
	}

	public static LuaState.Builder builder() {
		return new LuaState.Builder();
	}

	/**
	 * A mutable builder for {@link LuaState}s.
	 */
	public static class Builder {
		private static final AtomicInteger coroutineCount = new AtomicInteger();
		private static final Executor defaultCoroutineExecutor = Executors.newCachedThreadPool(command -> {
			Thread thread = new Thread(command, "Coroutine-" + coroutineCount.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		});

		private LoadState.FunctionFactory compiler = LoadState::interpretedFunction;
		private DebugHandler debug = DebugHandler.INSTANCE;
		private Executor coroutineExecutor = defaultCoroutineExecutor;
		private ErrorReporter reportError;

		/**
		 * Build a Lua state from this builder
		 *
		 * @return The constructed Lua state.
		 */
		public LuaState build() {
			return new LuaState(this);
		}

		/**
		 * Set the compiler for this Lua state. This defaults to using the {@link LuaC} compiler.
		 *
		 * @param compiler The new compiler to use
		 * @return This builder
		 */
		public Builder compiler(LoadState.FunctionFactory compiler) {
			if (compiler == null) throw new NullPointerException("compiler cannot be null");
			this.compiler = compiler;
			return this;
		}

		/**
		 * Set the debug handler for this Lua state.
		 *
		 * @param debug The new debug handler
		 * @return This builder
		 */
		public Builder debug(DebugHandler debug) {
			if (debug == null) throw new NullPointerException("debug cannot be null");
			this.debug = debug;
			return this;
		}

		/**
		 * Set the coroutine executor for this state.
		 *
		 * @param coroutineExecutor The new executor
		 * @return This builder
		 */
		public Builder coroutineExecutor(Executor coroutineExecutor) {
			if (coroutineExecutor == null) throw new NullPointerException("coroutineExecutor cannot be null");
			this.coroutineExecutor = coroutineExecutor;
			return this;
		}

		public Builder errorReporter(ErrorReporter reporter) {
			if (reporter == null) throw new NullPointerException("report cannot be null");
			reportError = reporter;
			return this;
		}
	}

	/**
	 * A reporter for internal VM errors. This may be implemented by the host to log errors without displaying the
	 * full details inside the Lua runtime.
	 *
	 * @see #reportInternalError(Throwable, Supplier)
	 */
	public interface ErrorReporter {
		/**
		 * Report an internal VM error.
		 *
		 * @param error   The error that occurred.
		 * @param message Additional details about this error.
		 */
		void report(Throwable error, Supplier<String> message);
	}
}
