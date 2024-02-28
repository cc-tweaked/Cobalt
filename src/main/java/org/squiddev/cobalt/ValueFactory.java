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

import java.util.Arrays;
import java.util.List;

public final class ValueFactory {
	private static final int MAX_DEPTH = 5;

	private ValueFactory() {
	}

	/**
	 * Convert java boolean to a {@link LuaValue}.
	 *
	 * @param b boolean value to convert
	 * @return {@link Constants#TRUE} if not  or {@link Constants#FALSE} if false
	 */
	public static LuaBoolean valueOf(boolean b) {
		return b ? Constants.TRUE : Constants.FALSE;
	}

	/**
	 * Convert java int to a {@link LuaValue}.
	 *
	 * @param i int value to convert
	 * @return {@link LuaInteger} instance, possibly pooled, whose value is i
	 */
	public static LuaInteger valueOf(int i) {
		return LuaInteger.valueOf(i);
	}

	/**
	 * Convert java double to a {@link LuaValue}.
	 * This may return a {@link LuaInteger} or {@link LuaDouble} depending
	 * on the value supplied.
	 *
	 * @param d double value to convert
	 * @return {@link LuaNumber} instance, possibly pooled, whose value is d
	 */
	public static LuaNumber valueOf(double d) {
		return LuaDouble.valueOf(d);
	}

	/**
	 * Convert java string to a {@link LuaValue}.
	 *
	 * @param s String value to convert
	 * @return {@link LuaString} instance, possibly pooled, whose value is s
	 */
	public static LuaString valueOf(String s) {
		return LuaString.valueOf(s);
	}

	/**
	 * Convert bytes in an array to a {@link LuaValue}.
	 *
	 * @param bytes byte array to convert
	 * @return {@link LuaString} instance, possibly pooled, whose bytes are those in the supplied array
	 */
	public static LuaString valueOf(byte[] bytes) {
		return LuaString.valueOf(bytes);
	}

	/**
	 * Convert bytes in an array to a {@link LuaValue}.
	 *
	 * @param bytes byte array to convert
	 * @param off   offset into the byte array, starting at 0
	 * @param len   number of bytes to include in the {@link LuaString}
	 * @return {@link LuaString} instance, possibly pooled, whose bytes are those in the supplied array
	 */
	public static LuaString valueOf(byte[] bytes, int off, int len) {
		return LuaString.valueOf(bytes, off, len);
	}

	/**
	 * Construct an empty {@link LuaTable}.
	 *
	 * @return new {@link LuaTable} instance with no values and no metatable.
	 */
	public static LuaTable tableOf() {
		return new LuaTable();
	}

	/**
	 * Construct a {@link LuaTable} initialized with supplied array values.
	 *
	 * @param values array of {@link LuaValue} containing the values to use in initialization
	 * @return new {@link LuaTable} instance with sequential elements coming from the array.
	 */
	public static LuaTable listOf(LuaValue... values) {
		LuaTable table = new LuaTable(values.length, 0);
		for (int i = 0; i < values.length; i++) table.rawset(i + 1, values[i]);
		return table;
	}

	/**
	 * Construct a {@link LuaTable} initialized with supplied named values.
	 *
	 * @param items array of {@link LuaValue} containing the keys and values to use in initialization
	 *              in order {@code {key-a, value-a, key-b, value-b, ...} }
	 * @return new {@link LuaTable} instance with non-sequential keys coming from the supplied array.
	 */
	public static LuaTable tableOf(LuaValue... items) throws LuaError {
		LuaTable table = new LuaTable(0, items.length >> 1);
		for (int i = 0; i < items.length; i += 2) {
			if (!items[i + 1].isNil()) table.rawset(items[i], items[i + 1]);
		}
		return table;
	}

	/**
	 * Construct a LuaUserdata for an object.
	 *
	 * @param o The java instance to be wrapped as userdata
	 * @return {@link LuaUserdata} value wrapping the java instance.
	 */
	public static LuaUserdata userdataOf(Object o) {
		return new LuaUserdata(o);
	}

	/**
	 * Construct a LuaUserdata for an object with a user supplied metatable.
	 *
	 * @param o         The java instance to be wrapped as userdata
	 * @param metatable The metatble to associate with the userdata instance.
	 * @return {@link LuaUserdata} value wrapping the java instance.
	 */
	public static LuaUserdata userdataOf(Object o, LuaTable metatable) {
		return new LuaUserdata(o, metatable);
	}

	/**
	 * Construct a {@link Varargs} from a single value. This is a helper function to ensure you don't call this
	 * redundant pattern.
	 *
	 * @param v The value to pack up.
	 * @return The exact same value as before.
	 */
	@Deprecated
	public static Varargs varargsOf(final LuaValue v) {
		return v;
	}

	/**
	 * Construct a {@link Varargs} around an array of {@link LuaValue}s.
	 *
	 * @param v The array of {@link LuaValue}s
	 * @return {@link Varargs} wrapping the supplied values.
	 * @see ValueFactory#varargsOf(LuaValue, Varargs)
	 * @see ValueFactory#varargsOfCopy(LuaValue[], int, int)
	 */
	public static Varargs varargsOf(final LuaValue... v) {
		return switch (v.length) {
			case 0 -> Constants.NONE;
			case 1 -> v[0];
			case 2 -> new LuaValue.PairVarargs(v[0], v[1]);
			default -> new LuaValue.ArrayVarargs(v, Constants.NONE);
		};
	}

	/**
	 * Construct a {@link Varargs} around a list of {@link LuaValue}s.
	 *
	 * @param v The array of {@link LuaValue}s
	 * @return {@link Varargs} wrapping the supplied values.
	 * @see ValueFactory#varargsOf(LuaValue, Varargs)
	 * @see ValueFactory#varargsOfCopy(LuaValue[], int, int)
	 */
	public static Varargs varargsOf(final List<LuaValue> v) {
		return switch (v.size()) {
			case 0 -> Constants.NONE;
			case 1 -> v.get(0);
			case 2 -> new LuaValue.PairVarargs(v.get(0), v.get(1));
			default -> new LuaValue.ArrayVarargs(v.toArray(new LuaValue[0]), Constants.NONE);
		};
	}

	/**
	 * Construct a {@link Varargs} around an array of {@link LuaValue}s.
	 *
	 * @param v      The array of {@link LuaValue}s
	 * @param offset number of initial values to skip in the array
	 * @param length number of values to include from the array
	 * @return {@link Varargs} wrapping the supplied values.
	 * @see ValueFactory#varargsOf(LuaValue[])
	 * @see ValueFactory#varargsOfCopy(LuaValue[], int, int, Varargs)
	 */
	public static Varargs varargsOfCopy(final LuaValue[] v, final int offset, final int length) {
		return switch (length) {
			case 0 -> Constants.NONE;
			case 1 -> v[offset];
			case 2 -> new LuaValue.PairVarargs(v[offset + 0], v[offset + 1]);
			default -> new LuaValue.ArrayVarargs(Arrays.copyOfRange(v, offset, offset + length), Constants.NONE);
		};
	}

	/**
	 * Construct a {@link Varargs} around an array of {@link LuaValue}s.
	 *
	 * @param v      The array of {@link LuaValue}s
	 * @param offset number of initial values to skip in the array
	 * @param length number of values to include from the array
	 * @param more   {@link Varargs} contain values to include at the end
	 * @return {@link Varargs} wrapping the supplied values.
	 * @see ValueFactory#varargsOfCopy(LuaValue[], int, int)
	 */
	public static Varargs varargsOfCopy(final LuaValue[] v, final int offset, final int length, Varargs more) {
		if (length == 0) return more;

		if (Varargs.DepthVarargs.depth(more) > MAX_DEPTH) {
			LuaValue[] values = new LuaValue[length + more.count()];
			System.arraycopy(v, offset, values, 0, length);
			more.fill(values, length);
			return new LuaValue.ArrayVarargs(values, Constants.NONE);
		}

		return length == 1 ? new LuaValue.PairVarargs(v[offset], more) : new LuaValue.ArrayVarargs(Arrays.copyOfRange(v, offset, offset + length), more);
	}

	/**
	 * Construct a {@link Varargs} around a set of 2 or more {@link LuaValue}s.
	 * <p>
	 * This can be used to wrap exactly 2 values, or a list consisting of 1 initial value
	 * followed by another variable list of remaining values.
	 *
	 * @param v First {@link LuaValue} in the {@link Varargs}
	 * @param r {@link LuaValue} supplying the 2rd value,
	 *          or {@link Varargs}s supplying all values beyond the first
	 * @return {@link Varargs} wrapping the supplied values.
	 */
	public static Varargs varargsOf(LuaValue v, Varargs r) {
		if (Varargs.DepthVarargs.depth(r) > MAX_DEPTH) {
			LuaValue[] values = new LuaValue[1 + r.count()];
			values[0] = v;
			r.fill(values, 1);
			return new LuaValue.ArrayVarargs(values, Constants.NONE);
		}

		return r.count() == 0 ? v : new LuaValue.PairVarargs(v, r);
	}

	/**
	 * Construct a {@link Varargs} around a set of 3 or more {@link LuaValue}s.
	 * <p>
	 * This can be used to wrap exactly 3 values, or a list consisting of 2 initial values
	 * followed by another variable list of remaining values.
	 *
	 * @param v1 First {@link LuaValue} in the {@link Varargs}
	 * @param v2 Second {@link LuaValue} in the {@link Varargs}
	 * @param v3 {@link LuaValue} supplying the 3rd value,
	 *           or {@link Varargs}s supplying all values beyond the second
	 * @return {@link Varargs} wrapping the supplied values.
	 */
	public static Varargs varargsOf(LuaValue v1, LuaValue v2, Varargs v3) {
		if (Varargs.DepthVarargs.depth(v3) > MAX_DEPTH) {
			LuaValue[] values = new LuaValue[2 + v3.count()];
			values[0] = v1;
			values[1] = v2;
			v3.fill(values, 2);
			return new LuaValue.ArrayVarargs(values, Constants.NONE);
		}

		return v3.count() == 0 ? new LuaValue.PairVarargs(v1, v2) : new LuaValue.ArrayVarargs(new LuaValue[]{v1, v2}, v3);
	}
}
