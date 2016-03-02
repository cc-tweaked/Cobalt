/**
 * ****************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
package org.squiddev.cobalt.compiler;


import org.squiddev.cobalt.*;
import org.squiddev.cobalt.LoadState.LuaCompiler;
import org.squiddev.cobalt.lib.BaseLib;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import static org.squiddev.cobalt.Factory.valueOf;

/**
 * Compiler for Lua.
 * <p>
 * Compiles lua source files into lua bytecode within a {@link Prototype},
 * loads lua binary files directly into a{@link Prototype},
 * and optionaly instantiates a {@link LuaClosure} around the result
 * using a user-supplied environment.
 * <p>
 * Implements the {@link LuaCompiler} interface for loading
 * initialized chunks, which is an interface common to
 * lua bytecode compiling and java bytecode compiling.
 * <p>
 * Teh {@link LuaC} compiler is installed by default by the
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

	public static final LuaC instance = new LuaC();

	/**
	 * Install the compiler so that LoadState will first
	 * try to use it when handed bytes that are
	 * not already a compiled lua chunk.
	 *
	 * @param state The lua state to install into
	 */
	public static void install(LuaState state) {
		state.compiler = instance;
	}

	protected static void _assert(boolean b) {
		if (!b) {
			throw new LuaError("compiler assert failed");
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

	static Prototype[] realloc(Prototype[] v, int n) {
		Prototype[] a = new Prototype[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	static LuaString[] realloc(LuaString[] v, int n) {
		LuaString[] a = new LuaString[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	static LocVars[] realloc(LocVars[] v, int n) {
		LocVars[] a = new LocVars[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	static int[] realloc(int[] v, int n) {
		int[] a = new int[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	static byte[] realloc(byte[] v, int n) {
		byte[] a = new byte[n];
		if (v != null) {
			System.arraycopy(v, 0, a, 0, Math.min(v.length, n));
		}
		return a;
	}

	public int nCcalls;
	Hashtable<LuaString, LuaString> strings;

	protected LuaC() {
	}

	private LuaC(Hashtable<LuaString, LuaString> strings) {
		this.strings = strings;
	}

	/**
	 * Load into a Closure or LuaFunction, with the supplied initial environment
	 */
	@Override
	public LuaFunction load(InputStream stream, String name, LuaValue env) throws IOException {
		Prototype p = compile(stream, name);
		return new LuaClosure(p, env);
	}

	/**
	 * Compile a prototype or load as a binary chunk
	 *
	 * @param stream The stream to read
	 * @param name   Name of the chunk
	 * @return The compiled code
	 * @throws IOException On stream read errors
	 */
	public static Prototype compile(InputStream stream, String name) throws IOException {
		int firstByte = stream.read();
		return (firstByte == '\033') ?
			LoadState.loadBinaryChunk(firstByte, stream, name) :
			(new LuaC(new Hashtable<LuaString, LuaString>())).luaY_parser(firstByte, stream, name);
	}

	/**
	 * Parse the input
	 */
	private Prototype luaY_parser(int firstByte, InputStream z, String name) {
		LexState lexstate = new LexState(this, z);
		FuncState funcstate = new FuncState();
		// lexstate.buff = buff;
		lexstate.setinput(this, firstByte, z, valueOf(name));
		lexstate.open_func(funcstate);
		/* main func. is always vararg */
		funcstate.f.is_vararg = Lua.VARARG_ISVARARG;
		funcstate.f.source = valueOf(name);
		lexstate.next(); /* read first token */
		lexstate.chunk();
		lexstate.check(LexState.TK_EOS);
		lexstate.close_func();
		LuaC._assert(funcstate.prev == null);
		LuaC._assert(funcstate.f.nups == 0);
		LuaC._assert(lexstate.fs == null);
		return funcstate.f;
	}

	// look up and keep at most one copy of each string
	public LuaString newTString(byte[] bytes, int offset, int len) {
		LuaString tmp = LuaString.valueOf(bytes, offset, len);
		LuaString v = strings.get(tmp);
		if (v == null) {
			// must copy bytes, since bytes could be from reusable buffer
			byte[] copy = new byte[len];
			System.arraycopy(bytes, offset, copy, 0, len);
			v = LuaString.valueOf(copy);
			strings.put(v, v);
		}
		return v;
	}

	public String pushfstring(String string) {
		return string;
	}

	public LuaFunction load(Prototype p, String filename, LuaValue env) {
		return new LuaClosure(p, env);
	}

}
