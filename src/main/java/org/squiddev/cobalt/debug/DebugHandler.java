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

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;

import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * The main handler for debugging
 */
public class DebugHandler {
	private static final LuaString CALL = valueOf("call");
	private static final LuaString LINE = valueOf("line");
	private static final LuaString COUNT = valueOf("count");
	private static final LuaString RETURN = valueOf("return");
	private static final LuaString TAILRETURN = valueOf("tail return");

	private final LuaState state;

	public DebugHandler(LuaState state) {
		this.state = state;
	}

	public static DebugState getDebugState(LuaThread thread) {
		if (thread.debugState == null) thread.debugState = new DebugState(thread);
		return thread.debugState;
	}

	public final DebugState getDebugState() {
		return getDebugState(state.getCurrentThread());
	}

	/**
	 * Called by recursing java functions on entry
	 *
	 * @param ds   The current debug state
	 * @param func the function called
	 */
	public void onCall(DebugState ds, LuaFunction func) {
		DebugInfo di = ds.pushInfo();
		di.setFunction(func);

		if (!ds.inhook && ds.hookcall) ds.callHookFunc(CALL, NIL);
	}

	/**
	 * Called by closures
	 *
	 * @param ds    The current debug state
	 * @param func  the function called
	 * @param args  The arguments to this function
	 * @param stack The current lua stack
	 * @return The pushed info
	 */
	public DebugInfo onCall(DebugState ds, LuaClosure func, Varargs args, LuaValue[] stack) {
		DebugInfo di = ds.pushInfo();
		di.setFunction(func, args, stack);

		if (!ds.inhook && ds.hookcall) ds.callHookFunc(CALL, NIL);
		return di;
	}

	/**
	 * Called by Closures and recursing java functions on return
	 *
	 * @param ds Debug state
	 */
	public void onReturn(DebugState ds) {
		try {
			if (!ds.inhook && ds.hookrtrn) ds.callHookFunc(RETURN, NIL);
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
	 */
	public void onInstruction(DebugState ds, DebugInfo di, int pc, Varargs extras, int top) {
		di.bytecode(pc, extras, top);

		if (ds.inhook) {
			if (ds.hookcount > 0) {
				if (++ds.hookcodes >= ds.hookcount) {
					ds.hookcodes = 0;
					ds.callHookFunc(COUNT, NIL);
				}
			}
			if (ds.hookline) {
				int newline = di.currentline();
				if (newline != ds.line) {
					int c = di.closure.getPrototype().code[pc];
					if ((c & 0x3f) != Lua.OP_JMP || ((c >>> 14) - 0x1ffff) >= 0) {
						ds.line = newline;
						ds.callHookFunc(LINE, valueOf(newline));
					}
				}
			}
		}
	}
}
