/**
 * ****************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
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
 * Class to encapsulate varargs values, either as part of a variable argument list, or multiple return values.
 * <p>
 * To construct varargs, use one of the static methods such as
 * {@code LuaValue.varargsOf(LuaValue, LuaValue)}
 * <p>
 * Any LuaValue can be used as a stand-in for Varargs, for both calls and return values.
 * When doing so, nargs() will return 1 and arg1() or arg(1) will return this.
 * This simplifies the case when calling or implementing varargs functions with only
 * 1 argument or 1 return value.
 * <p>
 * Varargs can also be derived from other varargs by appending to the front with a call
 * such as  {@code LuaValue.varargsOf(LuaValue, Varargs)}
 * or by taking a portion of the args using {@code Varargs.subargs(int start)}
 *
 * @see ValueFactory#varargsOf(LuaValue[])
 * @see ValueFactory#varargsOf(LuaValue, Varargs)
 * @see ValueFactory#varargsOf(LuaValue[], Varargs)
 * @see ValueFactory#varargsOf(LuaValue, LuaValue, Varargs)
 * @see ValueFactory#varargsOf(LuaValue[], int, int)
 * @see ValueFactory#varargsOf(LuaValue[], int, int, Varargs)
 * @see LuaValue#subargs(int)
 */
public abstract class Varargs {

	/**
	 * Get the n-th argument value (1-based).
	 *
	 * @param i the index of the argument to get, 1 is the first argument
	 * @return Value at position i, or LuaValue.NIL if there is none.
	 * @see Varargs#first()
	 * @see Constants#NIL
	 */
	public abstract LuaValue arg(int i);

	/**
	 * Get the number of arguments, or 0 if there are none.
	 *
	 * @return number of arguments.
	 */
	public abstract int count();

	/**
	 * Get the first argument in the list.
	 *
	 * @return LuaValue which is first in the list, or LuaValue.NIL if there are no values.
	 * @see Varargs#arg(int)
	 * @see Constants#NIL
	 */
	public abstract LuaValue first();

	/**
	 * Evaluate any pending tail call and return result.
	 *
	 * @param state The current lua state
	 * @return the evaluated tail call result
	 */
	public Varargs eval(LuaState state) {
		return this;
	}

	/**
	 * Return true if this is a TailcallVarargs
	 *
	 * @return true if a tail call, false otherwise
	 */
	public boolean isTailcall() {
		return false;
	}

	// -----------------------------------------------------------------------
	// utilities to get specific arguments and type-check them.
	// -----------------------------------------------------------------------

	/**
	 * Gets the type of argument {@code i}
	 *
	 * @param i the index of the argument to convert, 1 is the first argument
	 * @return int value corresponding to one of the LuaValue integer type values
	 * @see LuaValue#type()
	 */
	public int type(int i) {
		return arg(i).type();
	}

	/**
	 * Tests if argument i is nil.
	 *
	 * @param i the index of the argument to test, 1 is the first argument
	 * @return true if the argument is nil or does not exist, false otherwise
	 * @see Constants#TNIL
	 */
	public boolean isNil(int i) {
		return arg(i).isNil();
	}

	/**
	 * Tests if a value exists at argument i.
	 *
	 * @param i the index of the argument to test, 1 is the first argument
	 * @return true if the argument exists, false otherwise
	 */
	public boolean exists(int i) {
		return i > 0 && i <= count();
	}

	/**
	 * Return argument i as a LuaValue if it exists, or throw an error.
	 *
	 * @param i the index of the argument to test, 1 is the first argument
	 * @return LuaValue value if the argument exists
	 * @throws LuaError if the argument does not exist.
	 */
	public LuaValue checkValue(int i) {
		if (i <= count()) {
			return arg(i);
		} else {
			throw ErrorFactory.argError(i, "value expected");
		}
	}

	/**
	 * Return argument i as a LuaValue if it is not nil, or throw an error if it is nil.
	 *
	 * @param i the index of the argument to test, 1 is the first argument
	 * @return LuaValue value if the argument is not nil
	 * @throws LuaError if the argument doesn't exist or evaluates to nil.
	 */
	public LuaValue checkNotNil(int i) {
		return arg(i).checkNotNil();
	}

	/**
	 * Throw an error if {@code test} fails
	 *
	 * @param test user supplied assertion to test against
	 * @param i    the index to report in any error message
	 * @param msg  the error message to use when the test fails
	 * @throws LuaError if the the value of {@code test} is {@code false}
	 */
	public static void argCheck(boolean test, int i, String msg) {
		if (!test) {
			throw ErrorFactory.argError(i, msg);
		}
	}

	/**
	 * Return true if there is no argument or nil at argument i.
	 *
	 * @param i the index of the argument to test, 1 is the first argument
	 * @return true if argument i contains either no argument or nil
	 */
	public boolean isNoneOrNil(int i) {
		return i > count() || arg(i).isNil();
	}

	/**
	 * Convert the list of varargs values to a human readable java String.
	 *
	 * @return String value in human readable form such as {1,2}.
	 */
	public String toString() {
		Buffer sb = new Buffer();
		sb.append("(");
		for (int i = 1, n = count(); i <= n; i++) {
			if (i > 1) sb.append(",");
			sb.append(arg(i).toString());
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Create a {@code Varargs} instance containing arguments starting at index {@code start}
	 *
	 * @param start the index from which to include arguments, where 1 is the first argument.
	 * @return Varargs containing argument { start, start+1,  ... , narg-start-1 }
	 */
	public Varargs subargs(final int start) {
		int end = count();
		switch (end - start) {
			case 0:
				return arg(start);
			case 1:
				return new LuaValue.PairVarargs(arg(start), arg(end));
		}
		return end < start ? (Varargs) Constants.NONE : new SubVarargs(this, start, end);
	}

	/**
	 * Implementation of Varargs for use in the Varargs.subargs() function.
	 *
	 * @see Varargs#subargs(int)
	 */
	private static class SubVarargs extends Varargs {
		private final Varargs v;
		private final int start;
		private final int end;

		public SubVarargs(Varargs varargs, int start, int end) {
			this.v = varargs;
			this.start = start;
			this.end = end;
		}

		@Override
		public LuaValue arg(int i) {
			i += start - 1;
			return i >= start && i <= end ? v.arg(i) : Constants.NIL;
		}

		@Override
		public LuaValue first() {
			return v.arg(start);
		}

		@Override
		public int count() {
			return end + 1 - start;
		}
	}
}
