package org.squiddev.cobalt;

import java.util.Objects;

/**
 * An interface which is thrown when you want the VM to yield.
 */
public final class UnwindThrowable extends Throwable {
	private static final long serialVersionUID = -197039276914556877L;

	private static final UnwindThrowable emptyCache = new UnwindThrowable(Constants.NONE, null, false, true);
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
	 * Create a yield with no arguments
	 *
	 * @return The throwable to yield with
	 */
	public static UnwindThrowable empty() {
		return emptyCache;
	}

	/**
	 * Create a yield with the specified arguments
	 *
	 * @param args The arguments to yield with
	 * @return The throwable to yield with
	 */
	public static UnwindThrowable yield(Varargs args) {
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
	public static UnwindThrowable resume(LuaThread thread, Varargs args) {
		return new UnwindThrowable(args, thread, false, false);
	}

	/**
	 * Create a yield which should suspend this thread and all parent threads.
	 *
	 * @return The throwable to yield with
	 */
	public static UnwindThrowable suspend() {
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
