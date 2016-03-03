package org.squiddev.cobalt;

import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.lib.platform.ResourceManipulator;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
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
	public LuaValue stringMetatable;

	/**
	 * The metatable for all booleans
	 */
	public LuaValue booleanMetatable;

	/**
	 * The metatable for all numbers
	 */
	public LuaValue numberMetatable;

	/**
	 * The metatable for all nil values
	 */
	public LuaValue nilMetatable;

	/**
	 * The metatable for all functions
	 */
	public LuaValue functionMetatable;

	/**
	 * The metatable for all threads
	 */
	public LuaValue threadMetatable;

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

	protected LuaThread currentThread;

	/**
	 * The root thread
	 */
	protected LuaThread mainThread;

	public LuaState(ResourceManipulator resourceManipulator) {
		this.resourceManipulator = resourceManipulator;
	}

	/**
	 * Get the main thread
	 * @return The main thread
	 */
	public LuaThread getMainThread() {
		return mainThread;
	}

	/**
	 * The active thread
	 */
	public LuaThread getCurrentThread() {
		return currentThread;
	}

	public void setupThread(LuaTable environment) {
		if (mainThread != null && mainThread.isAlive()) {
			throw new IllegalStateException("State already has main thread");
		}

		LuaThread thread = new LuaThread(this, environment);
		mainThread = thread;
		currentThread = thread;
	}
}
