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
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;
import org.squiddev.cobalt.lib.platform.ResourceManipulator;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.TimeZone;
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
	 * The active input stream
	 */
	public InputStream stdin;

	/**
	 * The active output stream
	 */
	public PrintStream stdout;

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
	 * Lookup of loaded packages
	 */
	public final LuaTable loadedPackages = new LuaTable();

	/**
	 * The active resource manipulator
	 */
	public final ResourceManipulator resourceManipulator;

	/**
	 * The compiler for this threstate
	 */
	public final LoadState.LuaCompiler compiler;

	/**
	 * The handler for the debugger. Override this for custom debug actions.
	 */
	public final DebugHandler debug;

	/**
	 * The timezone for this state, as used by {@code os}.
	 */
	public final TimeZone timezone;

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

	public LuaState() {
		this(new LuaState.Builder());
	}

	private LuaState(Builder builder) {
		stdin = builder.stdin;
		stdout = builder.stdout;
		stringMetatable = builder.stringMetatable;
		booleanMetatable = builder.booleanMetatable;
		numberMetatable = builder.numberMetatable;
		nilMetatable = builder.nilMetatable;
		functionMetatable = builder.functionMetatable;
		threadMetatable = builder.threadMetatable;
		resourceManipulator = builder.resourceManipulator;
		compiler = builder.compiler;
		debug = builder.debug;
		timezone = builder.timezone;
		threader = new YieldThreader(builder.coroutineExecutor);
		reportError = builder.reportError;

		mainThread = currentThread = new LuaThread(this, new LuaTable());
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

		private InputStream stdin = System.in;
		private PrintStream stdout = System.out;
		private LuaTable stringMetatable;
		private LuaTable booleanMetatable;
		private LuaTable numberMetatable;
		private LuaTable nilMetatable;
		private LuaTable functionMetatable;
		private LuaTable threadMetatable;
		private ResourceManipulator resourceManipulator = new FileResourceManipulator();
		private LoadState.LuaCompiler compiler = LuaC.INSTANCE;
		private DebugHandler debug = DebugHandler.INSTANCE;
		private TimeZone timezone = TimeZone.getDefault();
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
		 * Set the initial standard input for this Lua state. This defaults to {@link System#in}.
		 *
		 * @param stdin The new standard input
		 * @return This builder
		 * @see LuaState#stdin
		 */
		public Builder stdin(InputStream stdin) {
			if (stdin == null) throw new NullPointerException("stdin cannot be null");
			this.stdin = stdin;
			return this;
		}

		/**
		 * Set the initial standard output for this Lua state. This defaults to {@link System#out}.
		 *
		 * @param stdout The new standard output
		 * @return This builder
		 * @see LuaState#stdout
		 */
		public Builder stdout(PrintStream stdout) {
			if (stdout == null) throw new NullPointerException("stdout cannot be null");
			this.stdout = stdout;
			return this;
		}

		/**
		 * Set the initial metatable for string values within this Lua State. This defaults to {@code null}.
		 *
		 * @param metatable The initial metatable
		 * @return This builder
		 */
		public Builder stringMetatable(LuaTable metatable) {
			stringMetatable = metatable;
			return this;
		}

		/**
		 * Set the initial metatable for boolean values within this Lua State. This defaults to {@code null}.
		 *
		 * @param metatable The initial metatable
		 * @return This builder
		 */
		public Builder booleanMetatable(LuaTable metatable) {
			booleanMetatable = metatable;
			return this;
		}

		/**
		 * Set the initial metatable for numeric values within this Lua State. This defaults to {@code null}.
		 *
		 * @param metatable The initial metatable
		 * @return This builder
		 */
		public Builder numberMetatable(LuaTable metatable) {
			numberMetatable = metatable;
			return this;
		}

		/**
		 * Set the initial metatable for nil values within this Lua State. This defaults to {@code null}.
		 *
		 * @param metatable The initial metatable
		 * @return This builder
		 */
		public Builder nilMetatable(LuaTable metatable) {
			nilMetatable = metatable;
			return this;
		}

		/**
		 * Set the initial metatable for functions within this Lua State. This defaults to {@code null}.
		 *
		 * @param metatable The initial metatable
		 * @return This builder
		 */
		public Builder functionMetatable(LuaTable metatable) {
			functionMetatable = metatable;
			return this;
		}

		/**
		 * Set the initial metatable for threads within this Lua State. This defaults to {@code null}.
		 *
		 * @param metatable The initial metatable
		 * @return This builder
		 */
		public Builder threadMetatable(LuaTable metatable) {
			threadMetatable = metatable;
			return this;
		}

		/**
		 * Set the resource manipulator that the {@code os} and {@code io} libraries will use. This defaults to a
		 * {@link FileResourceManipulator}, which uses the default file system.
		 *
		 * @param resourceManipulator The new resource manipulator
		 * @return This builder
		 */
		public Builder resourceManipulator(ResourceManipulator resourceManipulator) {
			if (this.resourceManipulator == null) throw new NullPointerException("resourceManipulator cannot be null");
			this.resourceManipulator = resourceManipulator;
			return this;
		}

		/**
		 * Set the compiler for this Lua state. This defaults to using the {@link LuaC} compiler.
		 *
		 * @param compiler The new compiler to use
		 * @return This builder
		 */
		public Builder compiler(LoadState.LuaCompiler compiler) {
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
		 * Set the timezone for this Lua state.
		 *
		 * @param zone The new timezone
		 * @return This builder
		 */
		public Builder timezone(TimeZone zone) {
			if (zone == null) throw new NullPointerException("zone cannot be null");
			timezone = zone;
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
