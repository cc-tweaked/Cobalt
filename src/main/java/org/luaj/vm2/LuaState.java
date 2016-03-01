package org.luaj.vm2;

import org.luaj.vm2.lib.platform.ResourceManipulator;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Global lua state
 */
public class LuaState {
	public InputStream STDIN = System.in;
	public PrintStream STDOUT = System.out;
	public PrintStream STDERR = System.err;

	public LuaValue stringMetatable;

	public LuaValue booleanMetatable;

	public LuaValue numberMetatable;

	public LuaValue nilMetatable;

	public LuaValue functionMetatable;

	public LuaValue threadMetatable;

	public LuaTable loadedPackages;

	public ResourceManipulator resourceManipulator;

	public LuaThread currentThread;

	public LuaState(ResourceManipulator resourceManipulator) {
		this.resourceManipulator = resourceManipulator;
	}

	public LuaThread getCurrentThread() {
		return LuaThread.getRunning();
	}
}
