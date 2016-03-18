package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Created by 09CoatJo on 18/03/2016.
 */
public class IntKeyEntry extends Entry {
	private final int key;
	private LuaValue value;

	public IntKeyEntry(int key, LuaValue value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public LuaValue key() {
		return valueOf(key);
	}

	@Override
	public int arraykey(int max) {
		return (key >= 1 && key <= max) ? key : 0;
	}

	@Override
	public LuaValue value() {
		return value;
	}

	@Override
	public Entry set(LuaValue value) {
		this.value = value;
		return this;
	}

	@Override
	public int keyindex(int mask) {
		return LuaTable.hashmod(key, mask);
	}

	@Override
	public boolean keyeq(LuaValue key) {
		return key.raweq(this.key);
	}
}
