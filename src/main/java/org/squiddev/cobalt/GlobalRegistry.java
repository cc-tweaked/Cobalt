package org.squiddev.cobalt;

/**
 * The global registry, a store of Lua values
 */
public final class GlobalRegistry {
	private final LuaTable table = new LuaTable();

	GlobalRegistry() {
	}

	/**
	 * Get the underlying registry table.
	 *
	 * @return The global debug registry.
	 */
	public LuaTable get() {
		return table;
	}

	/**
	 * Get a subtable in the global {@linkplain #get()} registry table}. If the key exists but is not a table, then
	 * it will be overridden.
	 *
	 * @param name The name of the registry table.
	 * @return The subentry.
	 */
	public LuaTable getSubTable(LuaString name) throws LuaError {
		LuaValue value = table.rawget(name);
		if (value instanceof LuaTable table) return table;

		LuaTable newValue = new LuaTable();
		table.rawset(name, newValue);
		return newValue;
	}
}
