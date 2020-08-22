package org.squiddev.cobalt;

public final class LuaRope extends LuaBaseString {
	private LuaString string;
	private LuaBaseString[] contents;
	private final int length;

	public LuaRope(LuaBaseString[] contents) {
		this.contents = contents;
		int length = 0;
		for (LuaBaseString str : contents) length += str.length();
		this.length = length;
	}

	@Override
	public LuaString strvalue() {
		if (string != null) return string;

		// Unlike strings, ropes are not shared across Lua instances, and so we don't need to worry about race
		// conditions.
		byte[] out = new byte[length];
		append(out, 0, contents);
		contents = null;
		return this.string = LuaString.valueOf(out);
	}

	private static int append(byte[] out, int start, LuaBaseString[] contents) {
		for (LuaBaseString str : contents) {
			LuaString string;
			if (str instanceof LuaRope) {
				LuaRope rope = (LuaRope) str;
				if (rope.contents != null) {
					start = append(out, start, ((LuaRope) str).contents);
					continue;
				}

				string = rope.string;
			} else {
				string = (LuaString) str;
			}

			System.arraycopy(string.bytes, string.offset, out, start, string.length);
			start += string.length;
		}

		return start;
	}

	@Override
	public int length() {
		return length;
	}

	@Override
	public double scanNumber(int base) {
		return string.scanNumber(base);
	}

	@Override
	public String toString() {
		return strvalue().toString();
	}

	@Override
	public int hashCode() {
		return strvalue().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof LuaValue && ((LuaValue) o).raweq(strvalue()));
	}

	@Override
	public boolean raweq(LuaValue val) {
		return val.raweq(strvalue());
	}

	@Override
	public boolean raweq(LuaString val) {
		return strvalue().raweq(val);
	}
}
