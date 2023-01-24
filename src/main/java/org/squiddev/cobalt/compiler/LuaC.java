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


import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState.LuaCompiler;
import org.squiddev.cobalt.function.LocalVariable;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.LuaInterpretedFunction;
import org.squiddev.cobalt.lib.BaseLib;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.InputStream;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.compiler.LoadState.checkMode;

/**
 * Compiler for Lua.
 *
 * Compiles lua source files into lua bytecode within a {@link Prototype},
 * loads lua binary files directly into a{@link Prototype},
 * and optionaly instantiates a {@link LuaInterpretedFunction} around the result
 * using a user-supplied environment.
 *
 * Implements the {@link LuaCompiler} interface for loading
 * initialized chunks, which is an interface common to
 * lua bytecode compiling and java bytecode compiling.
 *
 * The {@link LuaC} compiler is installed by default by the
 * {@link JsePlatform} class
 * so in the following example, the default {@link LuaC} compiler
 * will be used:
 * <pre> {@code
 * LuaValue _G = JsePlatform.standardGlobals();
 * LoadState.load( new ByteArrayInputStream("print 'hello'".getBytes()), "main.lua", _G ).call();
 * } </pre>
 *
 * @see LuaCompiler
 * @see JsePlatform
 * @see BaseLib
 * @see LuaValue
 * @see LuaCompiler
 * @see Prototype
 */
public class LuaC implements LuaCompiler {
	public static final LuaC INSTANCE = new LuaC();

	protected static void _assert(boolean b) throws CompileException {
		if (!b) {
			// So technically this should fire a runtime exception but...
			throw new CompileException("compiler assert failed");
		}
	}

	public static final int MAXSTACK = 250;
	public static final int LUAI_MAXUPVALUES = 60;
	public static final int LUAI_MAXVARS = 200;


	public static void SET_OPCODE(InstructionPtr i, int o) {
		i.set((i.get() & (Lua.MASK_NOT_OP)) | ((o << Lua.POS_OP) & Lua.MASK_OP));
	}

	public static void SETARG_A(InstructionPtr i, int u) {
		i.set((i.get() & (Lua.MASK_NOT_A)) | ((u << Lua.POS_A) & Lua.MASK_A));
	}

	public static void SETARG_B(InstructionPtr i, int u) {
		i.set((i.get() & (Lua.MASK_NOT_B)) | ((u << Lua.POS_B) & Lua.MASK_B));
	}

	public static void SETARG_C(InstructionPtr i, int u) {
		i.set((i.get() & (Lua.MASK_NOT_C)) | ((u << Lua.POS_C) & Lua.MASK_C));
	}

	public static void SETARG_Bx(InstructionPtr i, int u) {
		i.set((i.get() & (Lua.MASK_NOT_Bx)) | ((u << Lua.POS_Bx) & Lua.MASK_Bx));
	}

	public static void SETARG_sBx(InstructionPtr i, int u) {
		SETARG_Bx(i, u + Lua.MAXARG_sBx);
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

	// vector reallocation

	public static LuaValue[] realloc(LuaValue[] v, int n) {
		LuaValue[] a = new LuaValue[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	public static Prototype[] realloc(Prototype[] v, int n) {
		Prototype[] a = new Prototype[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	public static LuaString[] realloc(LuaString[] v, int n) {
		LuaString[] a = new LuaString[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	public static LocalVariable[] realloc(LocalVariable[] v, int n) {
		LocalVariable[] a = new LocalVariable[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	public static int[] realloc(int[] v, int n) {
		int[] a = new int[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	public static byte[] realloc(byte[] v, int n) {
		byte[] a = new byte[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	private LuaC() {
	}

	/**
	 * Load into a Closure or LuaFunction, with the supplied initial environment
	 */
	@Override
	public LuaClosure load(InputStream stream, LuaString name, LuaString mode, LuaTable env) throws IOException, CompileException {
		Prototype p = compile(stream, name, mode);
		LuaInterpretedFunction closure = new LuaInterpretedFunction(p, env);
		closure.nilUpvalues();
		return closure;
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
			return LoadState.loadBinaryChunk(firstByte, stream, name);
		} else {
			checkMode(mode, "text");
			return luaY_parser(firstByte, stream, name);
		}
	}

	/**
	 * Parse the input
	 */
	private static Prototype luaY_parser(int firstByte, InputStream z, LuaString name) throws CompileException {
		LexState lexstate = new LexState(z);
		FuncState funcstate = new FuncState();
		// lexstate.buff = buff;
		lexstate.setinput(firstByte, z, name);
		lexstate.open_func(funcstate);
		/* main func. is always vararg */
		funcstate.f.is_vararg = Lua.VARARG_ISVARARG;
		funcstate.f.source = name;
		lexstate.nextToken(); /* read first token */
		lexstate.chunk();
		lexstate.check(LexState.TK_EOS);
		lexstate.close_func();
		LuaC._assert(funcstate.prev == null);
		LuaC._assert(funcstate.f.nups == 0);
		LuaC._assert(lexstate.fs == null);
		return funcstate.f;
	}

	public LuaFunction load(Prototype p, LuaTable env) {
		return new LuaInterpretedFunction(p, env);
	}
}
