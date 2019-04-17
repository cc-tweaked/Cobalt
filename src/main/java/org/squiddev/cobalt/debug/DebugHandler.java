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

import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKYIELD;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKYIELD_LINE;

/**
 * The main handler for debugging
 */
public class DebugHandler {
	public static final DebugHandler INSTANCE = new DebugHandler();

	public static DebugState getDebugState(LuaState state) {
		return state.getCurrentThread().getDebugState();
	}

	protected DebugHandler() {
	}

	/**
	 * Called by recurring java functions on entry
	 *
	 * @param ds    The current debug state
	 * @param func  the function called
	 * @param state The state which will be used when resuming the function.
	 * @param <S>   The type of the state used when resuming the function.
	 * @param <T>   The type of the function
	 * @return The pushed debug frame for this function.
	 * @throws LuaError On a stack overflow
	 */
	public <S, T extends LuaFunction & Resumable<S>> DebugFrame setupCall(DebugState ds, T func, S state) throws LuaError {
		DebugFrame di = ds.pushJavaInfo();
		di.setFunction(func, state);
		return di;
	}

	/**
	 * Called by closures
	 *
	 * @param ds            The current debug state
	 * @param func          the function called
	 * @param args          The arguments to this function
	 * @param stack         The current lua stack
	 * @param stackUpvalues The upvalue equivalent of this stack
	 * @return The pushed debug frame for this function.
	 * @throws LuaError On a stack overflow
	 */
	public DebugFrame setupCall(DebugState ds, LuaClosure func, Varargs args, LuaValue[] stack, Upvalue[] stackUpvalues) throws LuaError {
		DebugFrame di = ds.pushInfo();
		di.setFunction(func, args, stack, stackUpvalues);
		return di;
	}

	/**
	 * Called by Closures and recurring Java functions on return
	 *
	 * @param ds Debug state
	 * @param di The head debug frame
	 * @throws LuaError        On a runtime error within the hook.
	 * @throws UnwindThrowable If the hook transfers control to another coroutine.
	 */
	public void onReturn(DebugState ds, DebugFrame di) throws LuaError, UnwindThrowable {
		try {
			if (!ds.inhook && ds.hookrtrn) ds.hookReturn(di);
		} catch (LuaError | RuntimeException e) {
			ds.popInfo();
			throw e;
		}

		ds.popInfo();
	}

	/**
	 * Called by Closures and recurring Java functions on return when a runtime error
	 * occurred (and thus the hook should not be called).
	 *
	 * @param ds Debug state
	 */
	public void onReturnError(DebugState ds) {
		ds.popInfo();
	}

	/**
	 * Called by Closures on bytecode execution
	 *
	 * @param ds Debug state
	 * @param di Debug info
	 * @param pc The current program counter
	 * @throws LuaError        On a runtime error.
	 * @throws UnwindThrowable If the hook transfers control to another coroutine.
	 */
	public void onInstruction(DebugState ds, DebugFrame di, int pc) throws LuaError, UnwindThrowable {
		di.pc = pc;

		if (!ds.inhook) {
			// See traceexec for some of the ideas behind this.

			// If the HOOKYIELD flag is set, we've definitely executed the instruction
			// hook, and so should skip it.
			if ((di.flags & FLAG_HOOKYIELD) == 0) {
				if (ds.hookcount > 0 && ++ds.hookcodes >= ds.hookcount) {
					ds.hookcodes = 0;
					ds.hookInstruction(di);
				}
			} else {
				di.flags &= ~FLAG_HOOKYIELD;
			}

			// If the HOOKYIELD_LINE flag is set, we've executed the line hook too, and so
			// should skip it.
			if ((di.flags & FLAG_HOOKYIELD_LINE) == 0) {
				if (ds.hookline && di.closure != null) {
					Prototype prototype = di.closure.getPrototype();
					int[] lineInfo = prototype.lineinfo;
					int newLine = lineInfo != null && pc >= 0 && pc < lineInfo.length ? lineInfo[pc] : -1;
					int oldPc = di.oldPc;

					// call linehook when enter a new function, when jump back (loop), or when enter a new line
					if (oldPc == -1 || pc <= oldPc
						|| newLine != (lineInfo != null && oldPc >= 0 && oldPc < lineInfo.length ? lineInfo[oldPc] : -1)) {
						ds.hookLine(di, newLine);
					}
				}
			} else {
				di.flags &= ~FLAG_HOOKYIELD_LINE;
			}

			di.oldPc = pc;
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
