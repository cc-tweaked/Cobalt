package org.squiddev.cobalt;

/**
 * A metamethod whose absence will be cached.
 */
public enum CachedMetamethod {
	INDEX(Constants.INDEX),
	NEWINDEX(Constants.NEWINDEX),
	LEN(Constants.LEN),
	EQ(Constants.EQ);

	private final LuaString key;

	CachedMetamethod(LuaString key) {
		this.key = key;
	}

	public LuaString getKey() {
		return key;
	}
}
