package org.squiddev.cobalt;

import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.lib.platform.ResourceManipulator;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Global lua state
 */
public class LuaState {
	public InputStream stdin = System.in;
	public PrintStream stdout = System.out;

	public LuaValue stringMetatable;

	public LuaValue booleanMetatable;

	public LuaValue numberMetatable;

	public LuaValue nilMetatable;

	public LuaValue functionMetatable;

	public LuaValue threadMetatable;

	public LuaTable loadedPackages;

	public final ResourceManipulator resourceManipulator;

	public LuaThread currentThread;

	public LuaThread mainThread;

	public LoadState.LuaCompiler compiler = LuaC.instance;

	public LuaState(ResourceManipulator resourceManipulator) {
		this.resourceManipulator = resourceManipulator;
	}
}
