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
package org.squiddev.cobalt;

import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LocalVariable;
import org.squiddev.cobalt.function.LuaInterpretedFunction;

/**
 * Prototype representing compiled lua code.
 * <p>
 * This is both a straight translation of the corresponding C type,
 * and the main data structure for execution of compiled lua bytecode.
 * <p>
 * See documentation on {@link LuaInterpretedFunction} for information on how to load
 * and execute a {@link Prototype}.
 *
 * @see LuaInterpretedFunction
 */
public final class Prototype {
	public final LuaString source;
	private LuaString shortSource;

	/**
	 * Constants used by the function
	 */
	public final LuaValue[] constants;

	public final int[] code;

	/**
	 * Functions defined inside the function
	 */
	public final Prototype[] children;

	public final int upvalues;
	public final int lineDefined;
	public final int lastLineDefined;
	public final int parameters;
	public final int isVarArg;
	public final int maxStackSize;

	/**
	 * Map from opcodes to source lines
	 */
	public final int[] lineInfo;

	/**
	 * Information about local variables
	 */
	public final LocalVariable[] locals;

	public final LuaString[] upvalueNames;

	public Prototype(
		LuaString source,
		LuaValue[] constants, int[] code, Prototype[] children, int parameters, int isVarArg, int maxStackSize, int upvalues,
		int lineDefined, int lastLineDefined, int[] lineInfo, LocalVariable[] localVars, LuaString[] upvalueNames
	) {
		this.source = source;

		this.constants = constants;
		this.code = code;
		this.children = children;
		this.parameters = parameters;
		this.isVarArg = isVarArg;
		this.maxStackSize = maxStackSize;
		this.upvalues = upvalues;

		this.lineDefined = lineDefined;
		this.lastLineDefined = lastLineDefined;
		this.lineInfo = lineInfo;
		this.locals = localVars;
		this.upvalueNames = upvalueNames;
	}

	public LuaString sourceShort() {
		LuaString shortSource = this.shortSource;
		return shortSource != null ? shortSource : (this.shortSource = LoadState.getShortName(source));
	}

	public String toString() {
		return source + ":" + lineDefined + "-" + lastLineDefined;
	}

	/**
	 * Get the name of a local variable.
	 *
	 * @param number the local variable number to look up
	 * @param pc     the program counter
	 * @return the name, or null if not found
	 */
	public LuaString getLocalName(int number, int pc) {
		int i;
		for (i = 0; i < locals.length && locals[i].startpc <= pc; i++) {
			if (pc < locals[i].endpc) {  /* is variable active? */
				number--;
				if (number == 0) return locals[i].name;
			}
		}
		return null;  // not found
	}
}
