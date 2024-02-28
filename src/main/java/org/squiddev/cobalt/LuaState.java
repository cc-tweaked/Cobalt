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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.compiler.BytecodeFormat;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.interrupt.InterruptAction;
import org.squiddev.cobalt.interrupt.InterruptHandler;

import java.util.Objects;
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

	private final @Nullable BytecodeFormat bytecodeFormat;

	private volatile boolean interrupted;
	private final InterruptHandler interruptHandler;

	/**
	 * The currently executing thread
	 */
	LuaThread currentThread;

	/**
	 * The currently executing main thread
	 */
	private final LuaThread mainThread;

	private final LuaTable globals = new LuaTable();

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
		interruptHandler = builder.interruptHandler;
		reportError = builder.reportError;
		bytecodeFormat = builder.bytecodeFormat;

		mainThread = currentThread = new LuaThread(this);
	}

	/**
	 * Get the global environment.
	 *
	 * @return The global environment.
	 */
	public LuaTable globals() {
		return globals;
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
	 * Get the bytecode parser/printer for this Lua state.
	 *
	 * @return The current bytecode format.
	 */
	public @Nullable BytecodeFormat getBytecodeFormat() {
		return bytecodeFormat;
	}

	/**
	 * Interrupt the execution of the current runtime.
	 * <p>
	 * This method is expected to be called from another thread. When the Lua runtime is next able to do so, it will
	 * call the current {@link InterruptHandler}'s {@link InterruptHandler#interrupted()} method.
	 *
	 * @see InterruptHandler
	 * @see #isInterrupted()
	 */
	public void interrupt() {
		if (interruptHandler == null) throw new IllegalStateException("LuaState has no interrupt handler");
		interrupted = true;
	}

	/**
	 * Check if the Lua runtime was interrupted;
	 *
	 * @return If the VM is currently interrupted.
	 * @see #handleInterrupt()
	 * @see #handleInterruptWithoutYield()
	 */
	public boolean isInterrupted() {
		return interrupted;
	}

	/**
	 * Handle the current runtime interrupt. Calls to this method should be guarded with a check of
	 * {@link #isInterrupted()}.
	 *
	 * @throws LuaError        If the {@linkplain InterruptHandler#interrupted() handler} threw an error.
	 * @throws UnwindThrowable If the handler requested the runtime be {@linkplain InterruptAction#SUSPEND suspended}.
	 * @see #interrupt()
	 */
	public void handleInterrupt() throws UnwindThrowable, LuaError {
		interrupted = false;
		switch (interruptHandler.interrupted()) {
			case CONTINUE -> {
			}
			case SUSPEND -> {
				if (currentThread.getStatus() != LuaThread.Status.RUNNING) {
					throw new IllegalStateException("Called checkInterrupt from a " + currentThread.getStatus().getDisplayName() + " thread");
				}

				DebugFrame top = currentThread.getDebugState().getStackUnsafe();
				top.flags |= DebugFrame.FLAG_INTERRUPTED;

				throw UnwindThrowable.suspend();
			}
		}
	}

	/**
	 * Handle an interrupt. Unlike {@link #handleInterrupt()}, this will continue execution if the
	 * handler attempts to {@linkplain InterruptAction#SUSPEND suspend} the Lua machine.
	 *
	 * @throws LuaError If the {@linkplain InterruptHandler#interrupted() handler} threw an error.
	 * @see #interrupt()
	 */
	public void handleInterruptWithoutYield() throws LuaError {
		interrupted = false;
		switch (interruptHandler.interrupted()) {
			case CONTINUE -> {
			}
			// We can't suspend here, so just set the interrupted flag again so we check later.
			case SUSPEND -> interrupted = true;
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
		private LoadState.FunctionFactory compiler = LoadState::interpretedFunction;
		private @Nullable InterruptHandler interruptHandler;
		private @Nullable ErrorReporter reportError;
		private @Nullable BytecodeFormat bytecodeFormat;

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
			Objects.requireNonNull(compiler, "compiler cannot be null");
			this.compiler = compiler;
			return this;
		}

		/**
		 * Set the interrupt handler for this Lua state.
		 *
		 * @param handler The new interrupt handler.
		 * @return This builder
		 * @see LuaState#interrupt()
		 */
		public Builder interruptHandler(InterruptHandler handler) {
			Objects.requireNonNull(handler, "handler cannot be null");
			interruptHandler = handler;
			return this;
		}

		/**
		 * Set the error callback used for this Lua state.
		 *
		 * @param reporter The new error builder.
		 * @return This builder
		 * @see LuaState#reportInternalError(Throwable, Supplier)
		 */
		public Builder errorReporter(ErrorReporter reporter) {
			Objects.requireNonNull(reporter, "reporter cannot be null");
			reportError = reporter;
			return this;
		}

		/**
		 * Set the bytecode parser/printer for this Lua state.
		 *
		 * @param bytecodeFormat The new bytecode format.
		 * @return This builder
		 */
		public Builder bytecodeFormat(BytecodeFormat bytecodeFormat) {
			Objects.requireNonNull(bytecodeFormat, "bytecodeFormat cannot be null");
			this.bytecodeFormat = bytecodeFormat;
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
