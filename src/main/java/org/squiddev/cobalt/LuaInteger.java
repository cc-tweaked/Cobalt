/**
 * ****************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
package org.squiddev.cobalt;

/**
 * Extension of {@link LuaNumber} which can hold a Java int as its value.
 * <p>
 * These instance are not instantiated directly by clients, but indirectly
 * via the static functions {@link Factory#valueOf(int)} or {@link Factory#valueOf(double)}
 * functions.  This ensures that policies regarding pooling of instances are
 * encapsulated.
 * <p>
 * There are no API's specific to LuaInteger that are useful beyond what is already
 * exposed in {@link LuaValue}.
 *
 * @see LuaValue
 * @see LuaNumber
 * @see LuaDouble
 * @see Factory#valueOf(int)
 * @see Factory#valueOf(double)
 */
public class LuaInteger extends LuaNumber {

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
	 * @see Factory#valueOf(int)
	 * @see Factory#valueOf(double)
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
	public final int v;

	/**
	 * Package protected constructor.
	 *
	 * @see Factory#valueOf(int)
	 */
	LuaInteger(int i) {
		this.v = i;
	}

	@Override
	public boolean isint() {
		return true;
	}

	@Override
	public boolean isinttype() {
		return true;
	}

	@Override
	public boolean islong() {
		return true;
	}

	@Override
	public byte tobyte() {
		return (byte) v;
	}

	@Override
	public char tochar() {
		return (char) v;
	}

	@Override
	public double todouble() {
		return v;
	}

	@Override
	public float tofloat() {
		return v;
	}

	@Override
	public int toint() {
		return v;
	}

	@Override
	public long tolong() {
		return v;
	}

	@Override
	public short toshort() {
		return (short) v;
	}

	@Override
	public double optdouble(double defval) {
		return v;
	}

	@Override
	public int optint(int defval) {
		return v;
	}

	@Override
	public LuaInteger optinteger(LuaInteger defval) {
		return this;
	}

	@Override
	public long optlong(long defval) {
		return v;
	}

	@Override
	public String tojstring() {
		return Integer.toString(v);
	}

	@Override
	public LuaString strvalue() {
		return LuaString.valueOf(Integer.toString(v));
	}

	@Override
	public LuaString optstring(LuaString defval) {
		return LuaString.valueOf(Integer.toString(v));
	}

	@Override
	public LuaValue tostring() {
		return LuaString.valueOf(Integer.toString(v));
	}

	@Override
	public String optjstring(String defval) {
		return Integer.toString(v);
	}

	@Override
	public LuaInteger checkinteger() {
		return this;
	}

	public int hashCode() {
		return v;
	}

	// unary operators
	@Override
	public LuaValue neg(LuaState state) {
		return valueOf(-(long) v);
	}

	// object equality, used for key comparison
	public boolean equals(Object o) {
		return o instanceof LuaInteger && ((LuaInteger) o).v == v;
	}

	@Override
	public boolean raweq(LuaValue val) {
		return val.raweq(v);
	}

	@Override
	public boolean raweq(double val) {
		return v == val;
	}

	@Override
	public boolean raweq(int val) {
		return v == val;
	}

	// string comparison
	@Override
	public int strcmp(LuaString rhs) {
		typeError("attempt to compare number with string");
		return 0;
	}

	@Override
	public int checkint() {
		return v;
	}

	@Override
	public long checklong() {
		return v;
	}

	@Override
	public double checkdouble() {
		return v;
	}

	@Override
	public String checkjstring() {
		return String.valueOf(v);
	}

	@Override
	public LuaString checkstring() {
		return Factory.valueOf(String.valueOf(v));
	}

	@Override
	public double checkarith() {
		return v;
	}
}
