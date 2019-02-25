package org.squiddev.cobalt;

/**
 * An unchecked {@link InterruptedException}. This will be propagated to the parent call.
 *
 * If this is thrown, the VM should attempt to recover, and should be discarded.
 */
public class InterruptedError extends Error {
	private static final long serialVersionUID = -7286062944629979963L;

	public InterruptedError(InterruptedException cause) {
		super(cause);
	}

	@Override
	public InterruptedException getCause() {
		return (InterruptedException) super.getCause();
	}
}
