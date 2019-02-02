/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2017 SquidDev
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

public class ValueFactory {
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
	 * @param varargs  {@link Varargs} containing the values to use in initialization
	 * @param firstarg the index of the first argument to use from the varargs, 1 being the first.
	 * @return new {@link LuaTable} instance with sequential elements coming from the varargs.
	 */
	public static LuaTable tableOf(Varargs varargs, int firstarg) {
		return new LuaTable(varargs, firstarg);
	}

	/**
	 * Construct an empty {@link LuaTable} preallocated to hold array and hashed elements
	 *
	 * @param narray Number of array elements to preallocate
	 * @param nhash  Number of hash elements to preallocate
	 * @return new {@link LuaTable} instance with no values and no metatable, but preallocated for array and hashed elements.
	 */
	public static LuaTable tableOf(int narray, int nhash) {
		return new LuaTable(narray, nhash);
	}

	/**
	 * Construct a {@link LuaTable} initialized with supplied array values.
	 *
	 * @param unnamedValues array of {@link LuaValue} containing the values to use in initialization
	 * @return new {@link LuaTable} instance with sequential elements coming from the array.
	 */
	public static LuaTable listOf(LuaValue... unnamedValues) {
		return new LuaTable(null, unnamedValues, null);
	}

	/**
	 * Construct a {@link LuaTable} initialized with supplied array values.
	 *
	 * @param unnamedValues array of {@link LuaValue} containing the first values to use in initialization
	 * @param lastarg       {@link Varargs} containing additional values to use in initialization
	 *                      to be put after the last unnamedValues element
	 * @return new {@link LuaTable} instance with sequential elements coming from the array and varargs.
	 */
	public static LuaTable listOf(LuaValue[] unnamedValues, Varargs lastarg) {
		return new LuaTable(null, unnamedValues, lastarg);
	}

	/**
	 * Construct a {@link LuaTable} initialized with supplied named values.
	 *
	 * @param namedValues array of {@link LuaValue} containing the keys and values to use in initialization
	 *                    in order {@code {key-a, value-a, key-b, value-b, ...} }
	 * @return new {@link LuaTable} instance with non-sequential keys coming from the supplied array.
	 */
	public static LuaTable tableOf(LuaValue... namedValues) {
		return new LuaTable(namedValues, null, null);
	}

	/**
	 * Construct a {@link LuaTable} initialized with supplied named values and sequential elements.
	 * The named values will be assigned first, and the sequential elements will be assigned later,
	 * possibly overwriting named values at the same slot if there are conflicts.
	 *
	 * @param namedValues   array of {@link LuaValue} containing the keys and values to use in initialization
	 *                      in order {@code {key-a, value-a, key-b, value-b, ...} }
	 * @param unnamedValues array of {@link LuaValue} containing the sequenctial elements to use in initialization
	 *                      in order {@code {value-1, value-2, ...} }, or null if there are none
	 * @return new {@link LuaTable} instance with named and sequential values supplied.
	 */
	public static LuaTable tableOf(LuaValue[] namedValues, LuaValue[] unnamedValues) {
		return new LuaTable(namedValues, unnamedValues, null);
	}

	/**
	 * Construct a {@link LuaTable} initialized with supplied named values and sequential elements in an array part and as varargs.
	 * The named values will be assigned first, and the sequential elements will be assigned later,
	 * possibly overwriting named values at the same slot if there are conflicts.
	 *
	 * @param namedValues   array of {@link LuaValue} containing the keys and values to use in initialization
	 *                      in order {@code {key-a, value-a, key-b, value-b, ...} }
	 * @param unnamedValues array of {@link LuaValue} containing the first sequenctial elements to use in initialization
	 *                      in order {@code {value-1, value-2, ...} }, or null if there are none
	 * @param lastarg       {@link Varargs} containing additional values to use in the sequential part of the initialization,
	 *                      to be put after the last unnamedValues element
	 * @return new {@link LuaTable} instance with named and sequential values supplied.
	 */
	public static LuaTable tableOf(LuaValue[] namedValues, LuaValue[] unnamedValues, Varargs lastarg) {
		return new LuaTable(namedValues, unnamedValues, lastarg);
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
	 * Construct a {@link Varargs} around an array of {@link LuaValue}s.
	 *
	 * @param v The array of {@link LuaValue}s
	 * @return {@link Varargs} wrapping the supplied values.
	 * @see ValueFactory#varargsOf(LuaValue, Varargs)
	 * @see ValueFactory#varargsOf(LuaValue[], int, int)
	 */
	public static Varargs varargsOf(final LuaValue... v) {
		switch (v.length) {
			case 0:
				return Constants.NONE;
			case 1:
				return v[0];
			case 2:
				return new LuaValue.PairVarargs(v[0], v[1]);
			default:
				return new LuaValue.ArrayVarargs(v, Constants.NONE);
		}
	}

	/**
	 * Construct a {@link Varargs} around an array of {@link LuaValue}s.
	 *
	 * @param v The array of {@link LuaValue}s
	 * @param r {@link Varargs} contain values to include at the end
	 * @return {@link Varargs} wrapping the supplied values.
	 * @see ValueFactory#varargsOf(LuaValue[])
	 * @see ValueFactory#varargsOf(LuaValue[], int, int, Varargs)
	 */
	public static Varargs varargsOf(final LuaValue[] v, Varargs r) {
		switch (v.length) {
			case 0:
				return r;
			case 1:
				return new LuaValue.PairVarargs(v[0], r);
			default:
				return new LuaValue.ArrayVarargs(v, r);
		}
	}

	/**
	 * Construct a {@link Varargs} around an array of {@link LuaValue}s.
	 *
	 * @param v      The array of {@link LuaValue}s
	 * @param offset number of initial values to skip in the array
	 * @param length number of values to include from the array
	 * @return {@link Varargs} wrapping the supplied values.
	 * @see ValueFactory#varargsOf(LuaValue[])
	 * @see ValueFactory#varargsOf(LuaValue[], int, int, Varargs)
	 */
	public static Varargs varargsOf(final LuaValue[] v, final int offset, final int length) {
		switch (length) {
			case 0:
				return Constants.NONE;
			case 1:
				return v[offset];
			case 2:
				return new LuaValue.PairVarargs(v[offset + 0], v[offset + 1]);
			default:
				return new LuaValue.ArrayPartVarargs(v, offset, length);
		}
	}

	/**
	 * Construct a {@link Varargs} around an array of {@link LuaValue}s.
	 *
	 * @param v      The array of {@link LuaValue}s
	 * @param offset number of initial values to skip in the array
	 * @param length number of values to include from the array
	 * @param more   {@link Varargs} contain values to include at the end
	 * @return {@link Varargs} wrapping the supplied values.
	 * @see ValueFactory#varargsOf(LuaValue[], Varargs)
	 * @see ValueFactory#varargsOf(LuaValue[], int, int)
	 */
	public static Varargs varargsOf(final LuaValue[] v, final int offset, final int length, Varargs more) {
		switch (length) {
			case 0:
				return more;
			case 1:
				return new LuaValue.PairVarargs(v[offset], more);
			default:
				return new LuaValue.ArrayPartVarargs(v, offset, length, more);
		}
	}

	/**
	 * Construct a {@link Varargs} around a set of 2 or more {@link LuaValue}s.
	 *
	 * This can be used to wrap exactly 2 values, or a list consisting of 1 initial value
	 * followed by another variable list of remaining values.
	 *
	 * @param v First {@link LuaValue} in the {@link Varargs}
	 * @param r {@link LuaValue} supplying the 2rd value,
	 *          or {@link Varargs}s supplying all values beyond the first
	 * @return {@link Varargs} wrapping the supplied values.
	 */
	public static Varargs varargsOf(LuaValue v, Varargs r) {
		switch (r.count()) {
			case 0:
				return v;
			default:
				return new LuaValue.PairVarargs(v, r);
		}
	}

	/**
	 * Construct a {@link Varargs} around a set of 3 or more {@link LuaValue}s.
	 *
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
		switch (v3.count()) {
			case 0:
				return new LuaValue.PairVarargs(v1, v2);
			default:
				return new LuaValue.ArrayVarargs(new LuaValue[]{v1, v2}, v3);
		}
	}

	public static LuaTable weakTable(boolean weakKeys, boolean weakValues) {
		LuaTable table = new LuaTable();
		table.useWeak(weakKeys, weakValues);
		return table;
	}
}
