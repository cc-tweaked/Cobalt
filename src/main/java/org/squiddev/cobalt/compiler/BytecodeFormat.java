package org.squiddev.cobalt.compiler;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.unwind.SuspendedFunction;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Handles writing (i.e. {@code string.dump}) and reading (i.e. {@code load}) the bytecode of a function.
 *
 * @see LuaState#getBytecodeFormat()
 * @see LuaBytecodeFormat
 */
public interface BytecodeFormat {
	/**
	 * Write a function's bytecode to the output stream.
	 * <p>
	 * The written stream should start with {@code (byte)27}, so that the string can be distinguished from a text chunk.
	 *
	 * @param output   The stream to write to.
	 * @param function The function to dump.
	 * @param strip    Whether to strip debug information from this function.
	 * @throws LuaError    If this operation is not supported,
	 * @throws IOException If the underlying stream could not be written to.
	 */
	void writeFunction(OutputStream output, Prototype function, boolean strip) throws LuaError, IOException;

	/**
	 * Read a function from its bytecode representation.
	 *
	 * @param name  The name of the file we're reading.
	 * @param input The input to read from. This will have already read the initial {@code (byte)27} byte.
	 * @return The newly created function.
	 * @throws CompileException If the function failed to compile.
	 * @throws LuaError         If the underlying reader failed.
	 */
	SuspendedFunction<Prototype> readFunction(LuaString name, InputReader input) throws CompileException, LuaError;
}
