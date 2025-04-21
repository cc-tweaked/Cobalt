package org.squiddev.cobalt.lib.system;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
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
	 */
	public static void standardGlobals(LuaState state) throws LuaError {
		standardGlobals(state, ResourceLoader.FILES, System.in, System.out);
	}

	/**
	 * Create a standard set of globals.
	 *
	 * @param state The current lua state
	 */
	public static void standardGlobals(LuaState state, ResourceLoader resources, InputStream stdin, PrintStream stdout) throws LuaError {
		CoreLibraries.standardGlobals(state);
		new SystemBaseLib(resources, stdin, stdout).add(state);
		new PackageLib(resources).add(state);
		new IoLib(stdin, stdout).add(state);
		new OsLib().add(state);
	}

	/**
	 * Create a standard set of globals.
	 *
	 * @param state The current lua state
	 */
	public static void debugGlobals(LuaState state) throws LuaError {
		debugGlobals(state, ResourceLoader.FILES, System.in, System.out);
	}

	/**
	 * Create standard globals including the {@link DebugLib} library.
	 *
	 * @param state The current lua state
	 * @see DebugLib
	 */
	public static void debugGlobals(LuaState state, ResourceLoader loader, InputStream stdin, PrintStream stdout) throws LuaError {
		standardGlobals(state, loader, stdin, stdout);
		DebugLib.add(state);
	}
}
