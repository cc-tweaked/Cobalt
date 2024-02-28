package cc.tweaked.cobalt.internal.unwind;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.UnwindThrowable;

import java.io.Serial;

/**
 * Wraps a {@link UnwindThrowable}, allowing functions to accumulate state as they unwind.
 *
 * @see AutoUnwind
 * @deprecated This should not be referenced from user code.
 */
@Deprecated
public class Pause extends Throwable {
	@Serial
	private static final long serialVersionUID = -398434492393581558L;

	public final UnwindState resumeAt;
	public Object state;

	public Pause(UnwindThrowable cause, UnwindState resumeAt) {
		super(null, cause, true, false);
		state = this.resumeAt = resumeAt;
	}

	@Override
	public synchronized @Nullable UnwindThrowable getCause() {
		return (UnwindThrowable) super.getCause();
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

	public void pushState(UnwindState newState) {
		newState.child = state;
		state = newState;
	}
}
