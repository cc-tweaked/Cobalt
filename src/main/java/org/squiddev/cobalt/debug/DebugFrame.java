/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
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

package org.squiddev.cobalt.debug;

import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;

/**
 * Each thread will get a DebugState attached to it by the debug library
 * which will track function calls, hook functions, etc.
 */
public final class DebugFrame {
	/**
	 * The debug info's function
	 */
	public LuaFunction func;

	/**
	 * The debug info's closure
	 */
	public LuaClosure closure;

	/**
	 * The stack for this info
	 */
	public LuaValue[] stack;

	public final DebugFrame previous;


	public Varargs varargs, extras;
	public int pc, top;

	public DebugFrame(DebugFrame previous) {
		this.previous = previous;
		func = null;
	}

	public DebugFrame(LuaFunction func) {
		pc = -1;
		previous = null;
		setFunction(func);
	}

	public void setFunction(LuaClosure closure, Varargs varargs, LuaValue[] stack) {
		this.func = closure;
		this.closure = closure;
		this.varargs = varargs;
		this.stack = stack;
	}

	public void setFunction(LuaFunction func) {
		this.func = func;
		this.closure = func instanceof LuaClosure ? (LuaClosure) func : null;
	}

	public void clear() {
		func = null;
		closure = null;
		stack = null;
		varargs = extras = null;
		pc = top = -1;
	}

	public void bytecode(int pc, Varargs extras, int top) {
		this.pc = pc;
		this.top = top;
		this.extras = extras;
	}

	/**
	 * Get the current line
	 *
	 * @return The current line the function is on
	 */
	public int currentLine() {
		if (closure == null) return -1;
		int[] li = closure.getPrototype().lineinfo;
		return li == null || pc < 0 || pc >= li.length ? -1 : li[pc];
	}

	/**
	 * Get the kind for this function
	 *
	 * @return This function's kind
	 */
	public LuaString[] getFuncKind() {
		DebugFrame previous = this.previous;
		if (previous == null || previous.closure == null || previous.pc < 0) return null;

		int stackpos = (previous.closure.getPrototype().code[previous.pc] >> 6) & 0xff;
		return DebugHelpers.getobjname(previous, stackpos);
	}

	public String sourceLine() {
		if (closure == null) return func == null ? "nil" : func.debugName();
		LuaString s = closure.getPrototype().sourceShort();
		int line = currentLine();
		return (s.startsWith('@') || s.startsWith('=') ? s.substring(1) : s) + ":" + line;
	}

	public LuaString getLocalName(int index) {
		if (closure == null) return null;
		return closure.getPrototype().getlocalname(index, pc);
	}
}
