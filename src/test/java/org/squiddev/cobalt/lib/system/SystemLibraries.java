package org.squiddev.cobalt.lib.system;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.lib.CoreLibraries;
import org.squiddev.cobalt.lib.DebugLib;

import java.io.InputStream;
import java.io.PrintStream;

public final class SystemLibraries {
	private SystemLibraries() {
	}

	/**
	 * Create a standard set of globals.
	 *
	 * @param state The current lua state
	 * @return Table of globals initialized with the standard JSE libraries
	 */
	public static LuaTable standardGlobals(LuaState state) throws LuaError {
		return standardGlobals(state, ResourceLoader.FILES, System.in, System.out);
	}

	/**
	 * Create a standard set of globals.
	 *
	 * @param state The current lua state
	 * @return Table of globals initialized with the standard JSE libraries
	 */
	public static LuaTable standardGlobals(LuaState state, ResourceLoader resources, InputStream stdin, PrintStream stdout) throws LuaError {
		LuaTable globals = CoreLibraries.standardGlobals(state);
		new SystemBaseLib(resources, stdin, stdout).add(globals);
		new PackageLib(resources).add(state, globals);
		new IoLib(stdin, stdout).add(state, globals);
		new OsLib().add(state, globals);
		return globals;
	}

	/**
	 * Create a standard set of globals.
	 *
	 * @param state The current lua state
	 * @return Table of globals initialized with the standard JSE libraries
	 */
	public static LuaTable debugGlobals(LuaState state) throws LuaError {
		return debugGlobals(state, ResourceLoader.FILES, System.in, System.out);
	}

	/**
	 * Create standard globals including the {@link DebugLib} library.
	 *
	 * @param state The current lua state
	 * @return Table of globals initialized with the standard JSE and debug libraries
	 * @see DebugLib
	 */
	public static LuaTable debugGlobals(LuaState state, ResourceLoader loader, InputStream stdin, PrintStream stdout) throws LuaError {
		LuaTable globals = standardGlobals(state, loader, stdin, stdout);
		DebugLib.add(state, globals);
		return globals;
	}
}
