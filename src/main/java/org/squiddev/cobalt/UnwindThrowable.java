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

import java.io.Serial;
import java.util.Objects;

/**
 * An interface which is thrown when you want the VM to yield.
 */
public final class UnwindThrowable extends Throwable {
	@Serial
	private static final long serialVersionUID = -197039276914556877L;

	private static final UnwindThrowable suspendCache = new UnwindThrowable(Constants.NONE, null, true, true);

	private final Varargs args;
	private final LuaThread thread;
	private final boolean suspend;
	private final boolean yield;

	private UnwindThrowable(Varargs args, LuaThread thread, boolean suspend, boolean yield) {
		this.args = args;
		this.thread = thread;
		this.suspend = suspend;
		this.yield = yield;
	}

	/**
	 * Create a yield with the specified arguments
	 *
	 * @param args The arguments to yield with
	 * @return The throwable to yield with
	 */
	static UnwindThrowable yield(Varargs args) {
		Objects.requireNonNull(args, "args cannot be null");
		return new UnwindThrowable(args, null, false, true);
	}

	/**
	 * Suspend this thread and enter a different thread
	 *
	 * @param thread The thread to resume
	 * @param args   The arguments to resume with
	 * @return The throwable to resume with
	 */
	static UnwindThrowable resume(LuaThread thread, Varargs args) {
		return new UnwindThrowable(args, thread, false, false);
	}

	/**
	 * Create a yield which should suspend this thread and all parent threads.
	 *
	 * @return The throwable to yield with
	 */
	static UnwindThrowable suspend() {
		return suspendCache;
	}

	/**
	 * Whether this throwable should suspend all threads, rather than just the current one
	 *
	 * @return Whether all threads should be suspended
	 */
	public boolean isSuspend() {
		return suspend;
	}

	/**
	 * Whether this throwable is yielding, rather than passing control to a new thread
	 *
	 * @return Whether all threads should be suspended
	 */
	public boolean isYield() {
		return yield;
	}

	/**
	 * Get the arguments for this unwind. This is only possible
	 * if this is not suspending.
	 *
	 * @return The arguments to yield with.
	 * @throws IllegalStateException If {@link #isSuspend()} is {@code true}.
	 */
	public Varargs getArgs() {
		if (suspend) throw new IllegalStateException("Cannot get args for suspending");
		return args;
	}

	/**
	 * Get the thread to resume for this unwind. This is only possible
	 * if this is not a yield.
	 *
	 * @return The arguments to yield with.
	 * @throws IllegalStateException If {@link #isYield()} is {@code true}.
	 */
	public LuaThread getThread() {
		if (yield) throw new IllegalStateException("Cannot get thread for yielding");
		return thread;
	}

	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
}
