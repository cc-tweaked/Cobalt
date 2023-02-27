package org.squiddev.cobalt.unwind;

import org.squiddev.cobalt.UnwindThrowable;

/**
 * Wraps a {@link UnwindThrowable}, allowing functions to accumulate state as they unwind.
 *
 * @see AutoUnwind
 * @deprecated This should not be referenced from user code.
 */
@Deprecated
public class Pause extends Throwable {
	public final UnwindState resumeAt;
	public Object state;

	public Pause(UnwindThrowable cause, UnwindState resumeAt) {
		super(null, cause, true, false);
		this.resumeAt = resumeAt;
	}

	@Override
	public synchronized UnwindThrowable getCause() {
		return (UnwindThrowable) super.getCause();
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
