package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaValue;

import static org.squiddev.cobalt.LuaTable.defaultEntry;

public class NonTableMetatable implements Metatable {

	private final LuaValue value;

	public NonTableMetatable(LuaValue value) {
		this.value = value;
	}

	@Override
	public boolean useWeakKeys() {
		return false;
	}

	@Override
	public boolean useWeakValues() {
		return false;
	}

	@Override
	public LuaValue toLuaValue() {
		return value;
	}

	@Override
	public Slot entry(LuaValue key, LuaValue value) {
		return defaultEntry(key, value);
	}

	@Override
	public LuaValue wrap(LuaValue value) {
		return value;
	}

	@Override
	public LuaValue arrayGet(LuaValue[] array, int index) {
		return array[index];
	}
}
