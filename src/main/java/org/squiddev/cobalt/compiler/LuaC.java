/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt.compiler;


import cc.tweaked.cobalt.internal.unwind.AutoUnwind;
import cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState.FunctionFactory;
import org.squiddev.cobalt.function.LuaInterpretedFunction;
import org.squiddev.cobalt.lib.BaseLib;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.IOException;
import java.io.InputStream;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.compiler.LoadState.checkMode;

/**
 * Compiler for Lua.
 * <p>
 * Compiles lua source files into lua bytecode within a {@link Prototype},
 * loads lua binary files directly into a{@link Prototype},
 * and optionaly instantiates a {@link LuaInterpretedFunction} around the result
 * using a user-supplied environment.
 * <p>
 * Implements the {@link FunctionFactory} interface for loading
 * initialized chunks, which is an interface common to
 * lua bytecode compiling and java bytecode compiling.
 * <p>
 * The {@link LuaC} compiler is installed by default by the
 * {@link CoreLibraries} class
 * so in the following example, the default {@link LuaC} compiler
 * will be used:
 * <pre> {@code
 * LuaValue _G = JsePlatform.standardGlobals();
 * LoadState.load( new ByteArrayInputStream("print 'hello'".getBytes()), "main.lua", _G ).call();
 * } </pre>
 *
 * @see FunctionFactory
 * @see CoreLibraries
 * @see BaseLib
 * @see LuaValue
 * @see FunctionFactory
 * @see Prototype
 */
public class LuaC {
	public static final int MAXSTACK = 250;
	public static final int LUAI_MAXUPVALUES = 60;
	public static final int LUAI_MAXVARS = 200;

	public static int SET_OPCODE(int i, int o) {
		return (i & ~Lua.MASK_OP) | ((o << Lua.POS_OP) & Lua.MASK_OP);
	}

	public static int SETARG_A(int i, int u) {
		return (i & ~Lua.MASK_A) | ((u << Lua.POS_A) & Lua.MASK_A);
	}

	public static int SETARG_B(int i, int u) {
		return (i & ~Lua.MASK_B) | ((u << Lua.POS_B) & Lua.MASK_B);
	}

	public static int SETARG_C(int i, int u) {
		return (i & ~Lua.MASK_C) | ((u << Lua.POS_C) & Lua.MASK_C);
	}

	public static int SETARG_Bx(int i, int u) {
		return (i & ~Lua.MASK_Bx) | ((u << Lua.POS_Bx) & Lua.MASK_Bx);
	}

	public static int SETARG_sBx(int i, int u) {
		return SETARG_Bx(i, u + Lua.MAXARG_sBx);
	}

	public static int CREATE_ABC(int o, int a, int b, int c) {
		return ((o << Lua.POS_OP) & Lua.MASK_OP) |
			((a << Lua.POS_A) & Lua.MASK_A) |
			((b << Lua.POS_B) & Lua.MASK_B) |
			((c << Lua.POS_C) & Lua.MASK_C);
	}

	public static int CREATE_ABx(int o, int a, int bc) {
		return ((o << Lua.POS_OP) & Lua.MASK_OP) |
			((a << Lua.POS_A) & Lua.MASK_A) |
			((bc << Lua.POS_Bx) & Lua.MASK_Bx);
	}

	public static int CREATE_Ax(int o, int a) {
		return ((o << Lua.POS_OP) & Lua.MASK_OP) |
			((a << Lua.POS_Ax) & Lua.MASK_Ax);
	}

	public static int[] realloc(int[] v, int n) {
		int[] a = new int[n];
		if (v != null) System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		return a;
	}

	public static byte[] realloc(byte[] v, int n) {
		byte[] a = new byte[n];
		if (v != null) System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		return a;
	}

	public static short[] realloc(short[] v, int n) {
		short[] a = new short[n];
		if (v != null) System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		return a;
	}

	private LuaC() {
	}

	/**
	 * Compile a prototype or load as a binary chunk
	 *
	 * @param state  The current Lua state.
	 * @param stream The stream to read
	 * @param name   Name of the chunk
	 * @return The compiled code
	 * @throws CompileException If there is a syntax error.
	 */
	public static Prototype compile(LuaState state, InputStream stream, String name) throws CompileException, LuaError {
		return compile(state, stream, valueOf(name));
	}

	public static Prototype compile(LuaState state, InputStream stream, LuaString name) throws CompileException, LuaError {
		Object result = SuspendedAction.noYield(() -> {
			try {
				return compile(state, new InputStreamReader(stream), name, null);
			} catch (CompileException e) {
				return e;
			}
		});

		if (result instanceof CompileException) throw (CompileException) result;
		return (Prototype) result;
	}

	@AutoUnwind
	public static Prototype compile(LuaState state, InputReader stream, LuaString name, LuaString mode) throws CompileException, LuaError, UnwindThrowable {
		int firstByte = stream.read();
		if (firstByte == '\033') {
			checkMode(mode, "binary");
			var bytecode = state.getBytecodeFormat();
			if (bytecode == null) throw new CompileException("attempt to load a binary chunk");
			var reader = bytecode.readFunction(name, stream);
			return reader.call(state);
		} else {
			checkMode(mode, "text");
			return loadTextChunk(firstByte, stream, name);
		}
	}

	/**
	 * Parse the input
	 */
	@AutoUnwind
	private static Prototype loadTextChunk(int firstByte, InputReader stream, LuaString name) throws CompileException, LuaError, UnwindThrowable {
		Parser parser = new Parser(stream, firstByte, name, LoadState.getShortName(name));
		parser.lexer.skipShebang();
		return parser.mainFunction();
	}

	public static final class InputStreamReader extends InputReader {
		private final @Nullable LuaState state;
		private final InputStream stream;

		public InputStreamReader(InputStream stream) {
			this(null, stream);
		}

		public InputStreamReader(@Nullable LuaState state, InputStream stream) {
			this.state = state;
			this.stream = stream;
		}

		@Override
		public int read() throws CompileException, UnwindThrowable, LuaError {
			if (state != null && state.isInterrupted()) state.handleInterrupt();

			try {
				return stream.read();
			} catch (IOException e) {
				String message = e.getMessage() == null ? e.toString() : e.getMessage();
				throw new CompileException("io error: " + message);
			}
		}

		@Override
		public int resume(Varargs varargs) throws CompileException, LuaError, UnwindThrowable {
			return read();
		}
	}
}
