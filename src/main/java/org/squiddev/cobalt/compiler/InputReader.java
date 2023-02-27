package org.squiddev.cobalt.compiler;

import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.Varargs;

public interface InputReader {
	int read() throws CompileException, UnwindThrowable;

	default int resume(Varargs varargs) throws CompileException, UnwindThrowable {
		throw new IllegalStateException("Cannot resume a non-yielding InputReader.");
	}
}
