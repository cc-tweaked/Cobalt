package org.squiddev.cobalt.table;

import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;

/**
 * Created by 09CoatJo on 18/03/2016.
 */
public class NormalEntry extends Entry {
	private final LuaValue key;
	private LuaValue value;

	public NormalEntry(LuaValue key, LuaValue value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public LuaValue key() {
		return key;
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
	public Varargs toVarargs() {
		return this;
	}

	@Override
	public int keyindex(int hashMask) {
		return LuaTable.hashSlot(key, hashMask);
	}

	@Override
	public boolean keyeq(LuaValue key) {
		return key.raweq(this.key);
	}
}
