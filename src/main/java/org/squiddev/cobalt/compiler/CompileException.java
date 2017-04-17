package org.squiddev.cobalt.compiler;

/**
 * Represents a failure in compiling a binary chunk or string
 */
public class CompileException extends Exception {
	private static final long serialVersionUID = 5563020350887073386L;

	public CompileException() {
	}

	public CompileException(String message) {
		super(message);
	}

	public CompileException(String message, Throwable cause) {
		super(message, cause);
	}

	public CompileException(Throwable cause) {
		super(cause);
	}
}
