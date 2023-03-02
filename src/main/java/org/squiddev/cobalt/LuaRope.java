package org.squiddev.cobalt;

public final class LuaRope extends LuaBaseString {
	private static final int SMALL_STRING = 32;

	private LuaString string;
	private LuaBaseString[] contents;
	private final int length;

	private LuaRope parent;
	private int index;

	private LuaRope(LuaBaseString[] contents, int length) {
		this.contents = contents;
		this.length = length;
	}

	public static LuaBaseString valueOf(LuaValue[] contents, int start, int length, int strLength) {
		if (length == 0 || strLength == 0) return Constants.EMPTYSTRING;
		if (length == 1) return (LuaBaseString) contents[0];

		if (strLength > SMALL_STRING) {
			LuaBaseString[] slice = new LuaBaseString[length];
			System.arraycopy(contents, start, slice, 0, length);
			return new LuaRope(slice, strLength);
		}

		byte[] out = new byte[strLength];
		int position = 0;
		for (int i = 0; i < length; i++) {
			LuaString string = (LuaString) contents[start + i];
			position = string.copyTo(out, position);
		}

		return LuaString.valueOf(out);
	}

	@Override
	public LuaString strvalue() {
		return string != null ? string : actualise(this);
	}

	/**
	 * Convert a rope into a string, updating it in place.
	 * <p>
	 * This is effectively a recursive algorithm, but implemented as a basic loop. The ropes themselves act as the
	 * stack, holding onto the current iteration state of their parent. Unlike strings, ropes are not shared across Lua
	 * instances, and so we don't need to worry about race conditions.
	 *
	 * @param current The rope to convert to a string.
	 * @return The actualised string.
	 */
	private static LuaString actualise(LuaRope current) {
		byte[] out = new byte[current.length];
		int position = 0;

		top:
		while (true) {
			LuaBaseString[] contents = current.contents;
			int index = current.index;

			for (; index < contents.length; index++) {
				LuaBaseString str = contents[index];
				LuaString string;
				if (str instanceof LuaRope rope) {
					if (rope.contents != null) {
						current.index = index + 1;
						rope.parent = current;
						current = rope;
						continue top;
					}

					string = rope.string;
				} else {
					string = (LuaString) str;
				}

				position = string.copyTo(out, position);
			}

			current.index = 0;
			LuaRope parent = current.parent;
			if (parent == null) break;
			current.parent = null;
			current = parent;
		}

		current.contents = null;
		return current.string = LuaString.valueOf(out);
	}

	@Override
	public int length() {
		return length;
	}

	@Override
	public double scanNumber(int base) {
		return strvalue().scanNumber(base);
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
