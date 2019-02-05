package org.squiddev.cobalt;

/**
 * Used then the current function cannot be resumed
 */
public class NonResumableException extends RuntimeException {
	private static final long serialVersionUID = 199865548299276933L;

	public NonResumableException() {
	}

	public NonResumableException(String message) {
		super(message);
	}

	public NonResumableException(String message, Throwable cause) {
		super(message, cause);
	}

	public NonResumableException(Throwable cause) {
		super(cause);
	}
}
