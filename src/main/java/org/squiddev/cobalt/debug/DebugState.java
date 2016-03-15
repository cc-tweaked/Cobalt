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
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.DebugLib;

import java.lang.ref.WeakReference;

/**
 * DebugState is associated with a Thread
 */
public final class DebugState {
	/**
	 * The thread that owns this object
	 */
	private final WeakReference<LuaThread> thread;

	/**
	 * The thread's lua state
	 */
	private final LuaState state;

	private final DebugHandler handler;

	/**
	 * The top function
	 */
	public int top = -1;

	/**
	 * The stack of debug info
	 */
	private final DebugInfo[] debugInfo = new DebugInfo[LuaThread.MAX_CALLSTACK];

	/**
	 * The hook function to call
	 */
	public LuaFunction hookfunc;

	/**
	 * Which item hooks should be called on
	 */
	public boolean hookcall, hookline, hookrtrn, inhook;

	/**
	 * Number of instructions to execute
	 */
	public int hookcount;

	/**
	 * Number of instructions executed
	 */
	public int hookcodes;

	public DebugState(LuaThread thread) {
		this.thread = new WeakReference<LuaThread>(thread);
		this.state = thread.luaState;
		this.handler = state.debug;
	}

	public DebugInfo getInfo(int call) {
		DebugInfo di = debugInfo[call];
		if (di == null) debugInfo[call] = di = new DebugInfo(call == 0 ? null : getInfo(call - 1));
		return di;
	}

	public void onCall(LuaFunction func) {
		handler.onCall(this, func);
	}

	public void onReturn() {
		handler.onReturn(this);
	}

	/**
	 * Push a new debug info onto the stack
	 *
	 * @return The created info
	 */
	protected DebugInfo pushInfo() {
		int top = this.top + 1;
		if (top >= LuaThread.MAX_CALLSTACK) throw new LuaError("stack overflow");

		this.top = top;
		return getInfo(top);
	}

	/**
	 * Pop a debug info off the stack
	 */
	protected void popInfo() {
		debugInfo[top--].clear();
	}

	protected void callHookFunc(LuaString type, LuaValue arg) {
		if (inhook || hookfunc == null) return;

		inhook = true;
		try {
			hookfunc.call(state, type, arg);
		} catch (LuaError e) {
			throw e;
		} catch (RuntimeException e) {
			throw new LuaError(e);
		} finally {
			inhook = false;
		}
	}

	/**
	 * Setup the hook
	 *
	 * @param func  The function hook
	 * @param call  Hook on calls
	 * @param line  Hook on lines
	 * @param rtrn  Hook on returns
	 * @param count Number of bytecode operators to use
	 */
	public void setHook(LuaFunction func, boolean call, boolean line, boolean rtrn, int count) {
		this.hookcount = count;
		this.hookcall = call;
		this.hookline = line;
		this.hookrtrn = rtrn;
		this.hookfunc = func;
	}

	/**
	 * Get the top debug info
	 *
	 * @return The top debug info or {@code null}
	 */
	public DebugInfo getDebugInfo() {
		return top >= 0 ? debugInfo[top] : null;
	}

	/**
	 * Get the debug info at a particular level
	 *
	 * @param level The level to get at
	 * @return The debug info or {@code null}
	 */
	public DebugInfo getDebugInfo(int level) {
		return level >= 0 && level <= top ? debugInfo[top - level] : null;
	}

	/**
	 * Find the debug info for a function
	 *
	 * @param func The function to find
	 * @return The debug info for this function
	 */
	public DebugInfo findDebugInfo(LuaFunction func) {
		for (int i = top - 1; --i >= 0; ) {
			if (debugInfo[i].func == func) {
				return debugInfo[i];
			}
		}
		return new DebugInfo(func);
	}

	@Override
	public String toString() {
		LuaThread thread = this.thread.get();
		return thread != null ? DebugLib.traceback(thread, 0) : "orphaned thread";
	}
}
