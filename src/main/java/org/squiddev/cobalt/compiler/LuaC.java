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


import org.squiddev.cobalt.Lua;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.compiler.LoadState.FunctionFactory;
import org.squiddev.cobalt.function.LuaInterpretedFunction;
import org.squiddev.cobalt.lib.BaseLib;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

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
	protected static void _assert(boolean b) throws CompileException {
		if (!b) {
			// So technically this should fire a runtime exception but...
			throw new CompileException("compiler assert failed");
		}
	}

	public static final int MAXSTACK = 250;
	public static final int LUAI_MAXUPVALUES = 60;
	public static final int LUAI_MAXVARS = 200;

	public static int SET_OPCODE(int i, int o) {
		return (i & (Lua.MASK_NOT_OP)) | ((o << Lua.POS_OP) & Lua.MASK_OP);
	}

	public static int SETARG_A(int i, int u) {
		return (i & (Lua.MASK_NOT_A)) | ((u << Lua.POS_A) & Lua.MASK_A);
	}

	public static int SETARG_B(int i, int u) {
		return (i & (Lua.MASK_NOT_B)) | ((u << Lua.POS_B) & Lua.MASK_B);
	}

	public static int SETARG_C(int i, int u) {
		return (i & (Lua.MASK_NOT_C)) | ((u << Lua.POS_C) & Lua.MASK_C);
	}

	public static int SETARG_Bx(int i, int u) {
		return (i & (Lua.MASK_NOT_Bx)) | ((u << Lua.POS_Bx) & Lua.MASK_Bx);
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

	private LuaC() {
	}

	/**
	 * Load lua thought to be a binary chunk from its first byte from an input stream.
	 *
	 * @param firstByte the first byte of the input stream
	 * @param stream    InputStream to read, after having read the first byte already
	 * @param name      Name to apply to the loaded chunk
	 * @return {@link Prototype} that was loaded
	 * @throws IllegalArgumentException If the signature is bac
	 * @throws IOException              If an IOException occurs
	 * @throws CompileException         If the stream cannot be loaded.
	 */
	public static Prototype loadBinaryChunk(int firstByte, InputStream stream, LuaString name) throws IOException, CompileException {
		name = LoadState.getSourceName(name);
		// check rest of signature
		if (firstByte != LoadState.LUA_SIGNATURE[0]
			|| stream.read() != LoadState.LUA_SIGNATURE[1]
			|| stream.read() != LoadState.LUA_SIGNATURE[2]
			|| stream.read() != LoadState.LUA_SIGNATURE[3]) {
			throw new IllegalArgumentException("bad signature");
		}

		// load file as a compiled chunk
		BytecodeLoader s = new BytecodeLoader(stream);
		s.loadHeader();
		return s.loadFunction(name);
	}

	public static Prototype compile(InputStream stream, String name) throws IOException, CompileException {
		return compile(stream, valueOf(name));
	}

	/**
	 * Compile a prototype or load as a binary chunk
	 *
	 * @param stream The stream to read
	 * @param name   Name of the chunk
	 * @return The compiled code
	 * @throws IOException      On stream read errors
	 * @throws CompileException If there is a syntax error.
	 */
	public static Prototype compile(InputStream stream, LuaString name) throws IOException, CompileException {
		return compile(stream, name, null);
	}

	public static Prototype compile(InputStream stream, LuaString name, LuaString mode) throws IOException, CompileException {
		int firstByte = stream.read();
		if (firstByte == '\033') {
			checkMode(mode, "binary");
			return loadBinaryChunk(firstByte, stream, name);
		} else {
			checkMode(mode, "text");
			try {
				return loadTextChunk(firstByte, stream, name);
			} catch (UncheckedIOException e) {
				throw e.getCause();
			}
		}
	}

	/**
	 * Parse the input
	 */
	private static Prototype loadTextChunk(int firstByte, InputStream stream, LuaString name) throws CompileException {
		Parser lexstate = new Parser(stream, firstByte, name);
		FuncState funcstate = lexstate.openFunc();
		funcstate.varargFlags = Lua.VARARG_ISVARARG; /* main func. is always vararg */

		lexstate.lexer.nextToken(); // read first token
		lexstate.chunk();
		lexstate.check(Lex.TK_EOS);
		Prototype prototype = lexstate.closeFunc();
		LuaC._assert(funcstate.upvalues.size() == 0);
		LuaC._assert(lexstate.fs == null);
		return prototype;
	}
}
