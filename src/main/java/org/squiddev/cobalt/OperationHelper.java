/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
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

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Handles arithmetic operations
 */
public final class OperationHelper {
	private OperationHelper() {
	}

	public static LuaValue add(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return valueOf(left.checkArith() + right.checkArith());
		}

		return left.arithmt(state, Constants.ADD, right);
	}

	public static LuaValue sub(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return valueOf(left.checkArith() - right.checkArith());
		}

		return left.arithmt(state, Constants.SUB, right);
	}

	public static LuaValue mul(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return valueOf(left.checkArith() * right.checkArith());
		}

		return left.arithmt(state, Constants.MUL, right);
	}

	public static LuaValue div(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return ValueFactory.valueOf(div(left.checkArith(), right.checkArith()));
		}

		return left.arithmt(state, Constants.DIV, right);
	}

	public static LuaValue mod(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return ValueFactory.valueOf(mod(left.checkArith(), right.checkArith()));
		}

		return left.arithmt(state, Constants.MOD, right);
	}

	public static LuaValue pow(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return ValueFactory.valueOf(Math.pow(left.checkArith(), right.checkArith()));
		}

		return left.arithmt(state, Constants.POW, right);
	}

	/**
	 * Divide two double numbers according to lua math, and return a double result.
	 *
	 * @param lhs Left-hand-side of the division.
	 * @param rhs Right-hand-side of the division.
	 * @return Value of the division, taking into account positive and negative infinity, and Nan
	 */
	public static double div(double lhs, double rhs) {
		return rhs != 0 ? lhs / rhs : lhs > 0 ? Double.POSITIVE_INFINITY : lhs == 0 ? Double.NaN : Double.NEGATIVE_INFINITY;
	}

	/**
	 * Take modulo for double numbers according to lua math, and return a double result.
	 *
	 * @param lhs Left-hand-side of the modulo.
	 * @param rhs Right-hand-side of the modulo.
	 * @return double value for the result of the modulo,
	 * using lua's rules for modulo
	 */
	public static double mod(double lhs, double rhs) {
		return rhs != 0 ? lhs - rhs * Math.floor(lhs / rhs) : Double.NaN;
	}

	public static boolean lt(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			throw ErrorFactory.compareError(left, right);
		}
		switch (tLeft) {
			case Constants.TNUMBER:
				return left.toDouble() < right.toDouble();
			case Constants.TSTRING:
				return left.strcmp(right) < 0;
			default:
				return left.comparemt(state, Constants.LT, right).toBoolean();
		}
	}

	public static boolean le(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			throw ErrorFactory.compareError(left, right);
		}
		switch (tLeft) {
			case Constants.TNUMBER:
				return left.toDouble() <= right.toDouble();
			case Constants.TSTRING:
				return left.strcmp(right) <= 0;
			default:
				if (left.type() == right.type()) {
					LuaValue h = left.metatag(state, Constants.LE);
					if (h.isNil()) {
						h = left.metatag(state, Constants.LT);
						if (!h.isNil() && h == right.metatag(state, Constants.LT)) {
							return !h.call(state, right, left).toBoolean();
						}
					} else if (h == right.metatag(state, Constants.LE)) {
						return h.call(state, left, right).toBoolean();
					}
				}

				throw new LuaError("attempt to compare " + Constants.LE + " on " + left.typeName() + " and " + right.typeName());
		}
	}

	public static boolean eq(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type();
		if (tLeft != right.type()) return false;

		switch (tLeft) {
			case Constants.TNIL:
				return true;
			case Constants.TNUMBER:
				return left.toDouble() == right.toDouble();
			case Constants.TBOOLEAN:
				return left.toBoolean() == right.toBoolean();
			case Constants.TSTRING:
				return left == right || left.raweq(right);
			case Constants.TUSERDATA:
			case Constants.TTABLE: {
				if (left == right || left.raweq(right)) return true;

				LuaValue leftMeta = left.getMetatable(state);
				if (leftMeta == null) return false;

				LuaValue rightMeta = right.getMetatable(state);
				if (rightMeta == null) return false;

				LuaValue h = leftMeta.rawget(Constants.EQ);
				return !(h.isNil() || h != rightMeta.rawget(Constants.EQ)) && h.call(state, left, right).toBoolean();
			}
			default:
				return left == right || left.raweq(right);
		}
	}

	public static LuaValue concat(LuaState state, LuaValue left, LuaValue right) {
		if (left.isString() && right.isString()) {
			return concat(left.checkLuaString(), right.checkLuaString());
		} else {
			return left.concatmt(state, right);
		}
	}

	public static LuaString concat(LuaString left, LuaString right) {
		byte[] b = new byte[left.length + right.length];
		System.arraycopy(left.bytes, left.offset, b, 0, left.length);
		System.arraycopy(right.bytes, right.offset, b, left.length, right.length);
		return ValueFactory.valueOf(b);
	}
}
