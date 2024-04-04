package org.squiddev.cobalt.compiler;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.Varargs;

/**
 * A basic byte-by-byte input stream, which can yield when reading.
 */
public abstract class InputReader {
	protected InputReader() {
	}

	/**
	 * Read a single byte from this input.
	 *
	 * @return The read byte.
	 * @throws LuaError         If the underlying reader threw a Lua error.
	 * @throws CompileException If reading failed for some other reason. Unlike a {@link LuaError}, this will not be
	 *                          passed to the {@code xpcall} error handler.
	 * @throws UnwindThrowable  If the reader yielded. {@link #resume(Varargs)} will be called when the coroutine is
	 *                          resumed.
	 */
	public abstract int read() throws CompileException, LuaError, UnwindThrowable;

	/**
	 * Resume this reader after yielding.
	 *
	 * @param varargs The value returned from the function above this in the stack
	 * @return The read byte.
	 * @throws LuaError         If the underlying reader threw a Lua error.
	 * @throws CompileException If reading failed for some other reason. Unlike a {@link LuaError}, this will not be
	 *                          passed to the {@code xpcall} error handler.
	 * @throws UnwindThrowable  If the reader yielded.
	 */
	public abstract int resume(Varargs varargs) throws CompileException, LuaError, UnwindThrowable;
}
