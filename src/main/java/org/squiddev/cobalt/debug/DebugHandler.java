/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2017 SquidDev
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

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.Upvalue;

/**
 * The main handler for debugging
 */
public class DebugHandler {
	public static final DebugHandler INSTANCE = new DebugHandler();

	public static DebugState getDebugState(LuaThread thread) {
		if (thread.debugState == null) thread.debugState = new DebugState(thread);
		return thread.debugState;
	}

	public static DebugState getDebugState(LuaState state) {
		return getDebugState(state.getCurrentThread());
	}

	protected DebugHandler() {
	}

	/**
	 * Called by recursing java functions on entry
	 *
	 * @param ds   The current debug state
	 * @param func the function called
	 * @throws LuaError On a runtime error.
	 */
	public void onCall(DebugState ds, LuaFunction func) throws LuaError {
		DebugFrame di = ds.pushInfo();
		di.setFunction(func);

		if (!ds.inhook && ds.hookcall) ds.hookCall(di);
	}

	/**
	 * Called by closures
	 *
	 * @param ds    The current debug state
	 * @param func  the function called
	 * @param args  The arguments to this function
	 * @param stack The current lua stack
	 * @return The pushed info
	 * @throws LuaError On a runtime error.
	 */
	public DebugFrame onCall(DebugState ds, LuaClosure func, Varargs args, LuaValue[] stack, Upvalue[] stackUpvalues) throws LuaError {
		DebugFrame di = ds.pushInfo();
		di.setFunction(func, args, stack, stackUpvalues);

		if (!ds.inhook && ds.hookcall) {
			// Pretend we are at the first instruction for the hook.
			try {
				di.pc++;
				ds.hookCall(di);
			} finally {
				di.pc--;
			}
		}
		return di;
	}

	/**
	 * Called by Closures and recursing java functions on return
	 *
	 * @param ds Debug state
	 * @throws LuaError On a runtime error.
	 */
	public void onReturn(DebugState ds) throws LuaError {
		try {
			if (!ds.inhook && ds.hookrtrn) ds.hookReturn();
		} finally {
			ds.popInfo();
		}
	}

	/**
	 * Called by Closures on bytecode execution
	 *
	 * @param ds     Debug state
	 * @param di     Debug info
	 * @param pc     Current program counter
	 * @param extras Extra arguments
	 * @param top    The top of the callstack
	 * @throws LuaError On a runtime error.
	 */
	public void onInstruction(DebugState ds, DebugFrame di, int pc, Varargs extras, int top) throws LuaError {
		Prototype prototype = ds.inhook || di.closure == null ? null : di.closure.getPrototype();
		int oldPc = di.pc;

		di.bytecode(pc, extras, top);

		if (!ds.inhook) {
			if (ds.hookcount > 0) {
				if (++ds.hookcodes >= ds.hookcount) {
					ds.hookcodes = 0;
					ds.hookInstruction(di);
				}
			}

			if (ds.hookline && prototype != null) {
				int[] lineInfo = prototype.lineinfo;
				if (lineInfo != null && pc >= 0 && pc < lineInfo.length) {
					int oldLine = oldPc >= 0 && oldPc < lineInfo.length ? lineInfo[oldPc] : -1;
					int newLine = lineInfo[pc];

					if (oldLine != newLine) {
						int c = prototype.code[pc];
						if ((c & 0x3f) != Lua.OP_JMP || ((c >>> 14) - 0x1ffff) >= 0) {
							ds.hookLine(di, oldLine, newLine);
						}
					}
				}
			}
		}
	}

	/**
	 * Called within long running processes (such as pattern matching)
	 * to allow terminating the process.
	 *
	 * @throws LuaError On a runtime error.
	 */
	public void poll() throws LuaError {
	}
}
