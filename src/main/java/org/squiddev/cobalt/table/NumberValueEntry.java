package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Entry class used with numeric values, but only when the key is not an integer.
 */
public class NumberValueEntry extends Entry {
	private double value;
	private final LuaValue key;

	public NumberValueEntry(LuaValue key, double value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public LuaValue key() {
		return key;
	}

	@Override
	public LuaValue value() {
		return valueOf(value);
	}

	@Override
	public Entry set(LuaValue value) {
		LuaValue n = value.toNumber();
		if (!n.isNil()) {
			this.value = n.toDouble();
			return this;
		} else {
			return new NormalEntry(this.key, value);
		}
	}

	@Override
	public int keyindex(int mask) {
		return LuaTable.hashSlot(key, mask);
	}

	@Override
	public boolean keyeq(LuaValue key) {
		return key.raweq(this.key);
	}
}
