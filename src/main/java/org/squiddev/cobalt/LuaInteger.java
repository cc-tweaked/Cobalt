/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt;

/**
 * Extension of {@link LuaNumber} which can hold a Java int as its value.
 * <p>
 * These instance are not instantiated directly by clients, but indirectly
 * via the static functions {@link ValueFactory#valueOf(int)} or {@link ValueFactory#valueOf(double)}
 * functions.  This ensures that policies regarding pooling of instances are
 * encapsulated.
 * <p>
 * There are no API's specific to LuaInteger that are useful beyond what is already
 * exposed in {@link LuaValue}.
 *
 * @see LuaValue
 * @see LuaNumber
 * @see LuaDouble
 * @see ValueFactory#valueOf(int)
 * @see ValueFactory#valueOf(double)
 */
public final class LuaInteger extends LuaNumber {

	private static final LuaInteger[] intValues = new LuaInteger[512];

	static {
		for (int i = 0; i < 512; i++) {
			intValues[i] = new LuaInteger(i - 256);
		}
	}

	public static LuaInteger valueOf(int i) {
		return i <= 255 && i >= -256 ? intValues[i + 256] : new LuaInteger(i);
	}

	// TODO consider moving this to LuaValue

	/**
	 * Return a LuaNumber that represents the value provided
	 *
	 * @param l long value to represent.
	 * @return LuaNumber that is eithe LuaInteger or LuaDouble representing l
	 * @see ValueFactory#valueOf(int)
	 * @see ValueFactory#valueOf(double)
	 */
	public static LuaNumber valueOf(long l) {
		int i = (int) l;
		return l == i ? (i <= 255 && i >= -256 ? intValues[i + 256] :
			new LuaInteger(i)) :
			LuaDouble.valueOf(l);
	}

	/**
	 * The value being held by this instance.
	 */
	private final int v;

	/**
	 * Package protected constructor.
	 *
	 * @see ValueFactory#valueOf(int)
	 */
	LuaInteger(int i) {
		this.v = i;
	}

	public int intValue() {
		return v;
	}

	@Override
	public double toDouble() {
		return v;
	}

	@Override
	public int toInteger() {
		return v;
	}

	@Override
	public String toString() {
		return Integer.toString(v);
	}

	@Override
	public LuaString checkLuaString() {
		return LuaString.valueOf(Integer.toString(v));
	}

	@Override
	public LuaValue toLuaString() {
		return LuaString.valueOf(Integer.toString(v));
	}

	public int hashCode() {
		return v;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof LuaInteger && ((LuaInteger) o).v == v;
	}

	@Override
	public int checkInteger() {
		return v;
	}

	@Override
	public long checkLong() {
		return v;
	}

	@Override
	public double checkDouble() {
		return v;
	}

	@Override
	public String checkString() {
		return String.valueOf(v);
	}
}
