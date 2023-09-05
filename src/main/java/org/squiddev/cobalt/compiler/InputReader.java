package org.squiddev.cobalt.compiler;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.Varargs;

/**
 * A basic byte-by-byte input stream, which can yield when reading.
 */
public interface InputReader {
	int read() throws CompileException, LuaError, UnwindThrowable;

	default int resume(Varargs varargs) throws CompileException, LuaError, UnwindThrowable {
		throw new IllegalStateException("Cannot resume a non-yielding InputReader.");
	}
}
