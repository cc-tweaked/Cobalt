package org.squiddev.cobalt;

import cc.tweaked.cobalt.memory.MarkedObject;

public abstract non-sealed class MarkedLuaValue extends LuaValue implements MarkedObject {
	private byte markMask = -1;

	protected MarkedLuaValue(int type) {
		super(type);
	}

	@Override
	public final boolean markObject(byte mask) {
		if (markMask == IGNORE || markMask == mask) return false;
		markMask = mask;
		return true;
	}
}
