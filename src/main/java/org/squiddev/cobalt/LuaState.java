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

package org.squiddev.cobalt;

import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.lib.platform.ResourceManipulator;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Global lua state
 */
public final class LuaState {
	/**
	 * The active input stream
	 */
	public InputStream stdin = System.in;

	/**
	 * The active output stream
	 */
	public PrintStream stdout = System.out;

	/**
	 * The metatable for all strings
	 */
	public LuaTable stringMetatable;

	/**
	 * The metatable for all booleans
	 */
	public LuaTable booleanMetatable;

	/**
	 * The metatable for all numbers
	 */
	public LuaTable numberMetatable;

	/**
	 * The metatable for all nil values
	 */
	public LuaTable nilMetatable;

	/**
	 * The metatable for all functions
	 */
	public LuaTable functionMetatable;

	/**
	 * The metatable for all threads
	 */
	public LuaTable threadMetatable;

	/**
	 * Lookup of loaded packages
	 */
	public final LuaTable loadedPackages = new LuaTable();

	/**
	 * The active resource manipulator
	 */
	public final ResourceManipulator resourceManipulator;

	/**
	 * The compiler for this threstate
	 */
	public LoadState.LuaCompiler compiler = LuaC.instance;

	/**
	 * The handler for the debugger. Override this for custom debug actions.
	 */
	public DebugHandler debug;

	/**
	 * The random instance for this state.
	 */
	public Random random;

	/**
	 * The currently executing state
	 */
	protected LuaThread currentThread;

	/**
	 * The root thread
	 */
	protected LuaThread mainThread;

	protected Set<LuaThread> threads = Collections.newSetFromMap(new WeakHashMap<LuaThread, Boolean>());

	public LuaState(ResourceManipulator resourceManipulator) {
		this.resourceManipulator = resourceManipulator;
		debug = new DebugHandler(this);
	}

	/**
	 * Get the main thread
	 *
	 * @return The main thread
	 * @see #getCurrentThread()
	 * @see #setupThread(LuaTable)
	 */
	public LuaThread getMainThread() {
		return mainThread;
	}

	/**
	 * The active thread
	 *
	 * @return The active thread
	 * @see #getMainThread()
	 * @see #setupThread(LuaTable)
	 */
	public LuaThread getCurrentThread() {
		return currentThread;
	}

	/**
	 * Setup the main thread
	 *
	 * @param environment The main thread to use
	 * @see #getMainThread()
	 * @see #getCurrentThread()
	 */
	public void setupThread(LuaTable environment) {
		if (mainThread != null && mainThread.isAlive()) {
			throw new IllegalStateException("State already has main thread");
		}

		LuaThread thread = new LuaThread(this, environment);
		mainThread = thread;
		currentThread = thread;
	}

	/**
	 * Abandon all threads but the main one
	 */
	public void abandon() {
		next:
		while (true) {
			for (LuaThread thread : threads) {
				if (thread != mainThread) {
					thread.abandon();
					continue next;
				}
			}

			break;
		}
	}
}
