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

import org.squiddev.cobalt.function.LuaFunction;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Handles arithmetic operations
 */
public final class OperationHelper {
	private OperationHelper() {
	}

	//region Binary
	public static LuaValue add(LuaState state, LuaValue left, LuaValue right) {
		return add(state, left, right, -1, -1);
	}

	public static LuaValue add(LuaState state, LuaValue left, LuaValue right, int leftIdx, int rightIdx) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return valueOf(left.checkArith() + right.checkArith());
		}

		return arithMetatable(state, Constants.ADD, left, right, leftIdx, rightIdx);
	}

	public static LuaValue sub(LuaState state, LuaValue left, LuaValue right) {
		return sub(state, left, right, -1, -1);
	}

	public static LuaValue sub(LuaState state, LuaValue left, LuaValue right, int leftIdx, int rightIdx) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return valueOf(left.checkArith() - right.checkArith());
		}

		return arithMetatable(state, Constants.SUB, left, right, leftIdx, rightIdx);
	}

	public static LuaValue mul(LuaState state, LuaValue left, LuaValue right) {
		return mul(state, left, right, -1, -1);
	}

	public static LuaValue mul(LuaState state, LuaValue left, LuaValue right, int leftIdx, int rightIdx) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return valueOf(left.checkArith() * right.checkArith());
		}

		return arithMetatable(state, Constants.MUL, left, right, leftIdx, rightIdx);
	}

	public static LuaValue div(LuaState state, LuaValue left, LuaValue right) {
		return div(state, left, right, -1, -1);
	}

	public static LuaValue div(LuaState state, LuaValue left, LuaValue right, int leftIdx, int rightIdx) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return ValueFactory.valueOf(div(left.checkArith(), right.checkArith()));
		}

		return arithMetatable(state, Constants.DIV, left, right, leftIdx, rightIdx);
	}

	public static LuaValue mod(LuaState state, LuaValue left, LuaValue right) {
		return mod(state, left, right, -1, -1);
	}

	public static LuaValue mod(LuaState state, LuaValue left, LuaValue right, int leftIdx, int rightIdx) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return ValueFactory.valueOf(mod(left.checkArith(), right.checkArith()));
		}

		return arithMetatable(state, Constants.MOD, left, right, leftIdx, rightIdx);
	}

	public static LuaValue pow(LuaState state, LuaValue left, LuaValue right) {
		return pow(state, left, right, -1, -1);
	}

	public static LuaValue pow(LuaState state, LuaValue left, LuaValue right, int leftIdx, int rightIdx) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return ValueFactory.valueOf(Math.pow(left.checkArith(), right.checkArith()));
		}

		return arithMetatable(state, Constants.POW, left, right, leftIdx, rightIdx);
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

	/**
	 * Perform metatag processing for arithmetic operations.
	 *
	 * Finds the supplied metatag value for {@code this} or {@code op2} and invokes it,
	 * or throws {@link LuaError} if neither is defined.
	 *
	 * @param state      The current lua state
	 * @param tag        The metatag to look up
	 * @param left       The left operand value to perform the operation with
	 * @param right      The other operand value to perform the operation with
	 * @param leftStack  Stack index of the LHS
	 * @param rightStack Stack index of the RHS
	 * @return {@link LuaValue} resulting from metatag processing
	 * @throws LuaError if metatag was not defined for either operand
	 */
	protected static LuaValue arithMetatable(LuaState state, LuaValue tag, LuaValue left, LuaValue right, int leftStack, int rightStack) {
		LuaValue h = left.metatag(state, tag);
		if (h.isNil()) {
			h = right.metatag(state, tag);
			if (h.isNil()) {
				if (left.isNumber()) {
					left = right;
					leftStack = rightStack;
				}
				throw ErrorFactory.operandError(state, left, "perform arithmetic on", leftStack);
			}
		}
		return OperationHelper.call(state, h, left, right);
	}

	/**
	 * Perform metatag processing for concatenation operations.
	 *
	 * Finds the {@link Constants#CONCAT} metatag value and invokes it,
	 * or throws {@link LuaError} if it doesn't exist.
	 *
	 * @param state The current lua state
	 * @param left  The right-hand-side value to perform the operation with
	 * @param right The right-hand-side value to perform the operation with
	 * @return {@link LuaValue} resulting from metatag processing for {@link Constants#CONCAT} metatag.
	 * @throws LuaError if metatag was not defined for either operand
	 */
	public static LuaValue concat(LuaState state, LuaValue left, LuaValue right) {
		return concat(state, left, right, -1, -1);
	}

	public static LuaValue concat(LuaState state, LuaValue left, LuaValue right, int leftStack, int rightStack) {
		if (left.isString() && right.isString()) {
			return concat(left.checkLuaString(), right.checkLuaString());
		} else {
			LuaValue h = left.metatag(state, Constants.CONCAT);
			if (h.isNil() && (h = right.metatag(state, Constants.CONCAT)).isNil()) {
				if (left.isString()) {
					left = right;
					leftStack = rightStack;
				}
				throw ErrorFactory.operandError(state, left, "concatenate", leftStack);
			}

			return OperationHelper.call(state, h, left, right);
		}
	}

	public static LuaString concat(LuaString left, LuaString right) {
		byte[] b = new byte[left.length + right.length];
		System.arraycopy(left.bytes, left.offset, b, 0, left.length);
		System.arraycopy(right.bytes, right.offset, b, left.length, right.length);
		return ValueFactory.valueOf(b);
	}
	//endregion

	//region Compare
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
				LuaValue h = left.metatag(state, Constants.LT);
				if (!h.isNil() && h == right.metatag(state, Constants.LT)) {
					return OperationHelper.call(state, h, left, right).toBoolean();
				} else {
					throw new LuaError("attempt to compare two " + left.typeName() + " values");
				}
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
				LuaValue h = left.metatag(state, Constants.LE);
				if (h.isNil()) {
					h = left.metatag(state, Constants.LT);
					if (!h.isNil() && h == right.metatag(state, Constants.LT)) {
						return !OperationHelper.call(state, h, right, left).toBoolean();
					}
				} else if (h == right.metatag(state, Constants.LE)) {
					return OperationHelper.call(state, h, left, right).toBoolean();
				}

				throw new LuaError("attempt to compare two " + left.typeName() + " values");
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

				LuaTable leftMeta = left.getMetatable(state);
				if (leftMeta == null) return false;

				LuaTable rightMeta = right.getMetatable(state);
				if (rightMeta == null) return false;

				LuaValue h = leftMeta.rawget(Constants.EQ);
				return !(h.isNil() || h != rightMeta.rawget(Constants.EQ)) && OperationHelper.call(state, h, left, right).toBoolean();
			}
			default:
				return left == right || left.raweq(right);
		}
	}
	//endregion

	//region Unary

	/**
	 * Length operator: return lua length of object including metatag processing
	 *
	 * @param state The current lua state
	 * @param value The value to ge the length of
	 * @return length as defined by the lua # operator or metatag processing result
	 * @throws LuaError if {@code value} is not a table or string, and has no {@link Constants#LEN} metatag
	 */
	public static LuaValue length(LuaState state, LuaValue value) {
		return length(state, value, -1);
	}

	public static LuaValue length(LuaState state, LuaValue value, int stack) {
		switch (value.type()) {
			case Constants.TTABLE:
				return valueOf(((LuaTable) value).length());
			case Constants.TSTRING:
				return valueOf(((LuaString) value).length());
			default: {
				LuaValue h = value.metatag(state, Constants.LEN);
				if (h.isNil()) {
					throw ErrorFactory.operandError(state, value, "get length of", stack);
				}
				return OperationHelper.call(state, h, value);
			}
		}
	}

	/**
	 * Unary minus: return negative value {@code (-this)} as defined by lua unary minus operator
	 *
	 * @param state The current lua state
	 * @param value Value to get the minus of
	 * @return numeric inverse as {@link LuaNumber} if numeric, or metatag processing result if {@link Constants#UNM} metatag is defined
	 * @throws LuaError if {@code value} is not a table or string, and has no {@link Constants#UNM} metatag
	 */
	public static LuaValue neg(LuaState state, LuaValue value) {
		return neg(state, value, -1);
	}

	public static LuaValue neg(LuaState state, LuaValue value, int stack) {
		int tValue = value.type();
		if (tValue == Constants.TNUMBER) {
			return valueOf(-value.checkArith());
		} else if (tValue == Constants.TSTRING) {
			double res = value.toDouble();
			if (!Double.isNaN(res)) return valueOf(-res);
		}

		LuaValue meta = value.metatag(state, Constants.UNM);
		if (meta.isNil()) {
			throw ErrorFactory.operandError(state, value, "perform arithmetic on", stack);
		}

		return OperationHelper.call(state, meta, value);
	}
	//endregion

	//region Calling
	public static LuaValue call(LuaState state, LuaValue function) {
		return call(state, function, -1);
	}

	public static LuaValue call(LuaState state, LuaValue function, int stack) {
		if (function.isFunction()) {
			return ((LuaFunction) function).call(state);
		} else {
			LuaValue meta = function.metatag(state, Constants.CALL);
			if (!meta.isFunction()) throw ErrorFactory.operandError(state, function, "call", stack);

			return ((LuaFunction) meta).call(state, function);
		}
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg) {
		return call(state, function, arg, -1);
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg, int stack) {
		if (function.isFunction()) {
			return ((LuaFunction) function).call(state, arg);
		} else {
			LuaValue meta = function.metatag(state, Constants.CALL);
			if (!meta.isFunction()) throw ErrorFactory.operandError(state, function, "call", stack);

			return ((LuaFunction) meta).call(state, function, arg);
		}
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg1, LuaValue arg2) {
		return call(state, function, arg1, arg2, -1);
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg1, LuaValue arg2, int stack) {
		if (function.isFunction()) {
			return ((LuaFunction) function).call(state, arg1, arg2);
		} else {
			LuaValue meta = function.metatag(state, Constants.CALL);
			if (!meta.isFunction()) throw ErrorFactory.operandError(state, function, "call", stack);

			return ((LuaFunction) meta).call(state, function, arg1, arg2);
		}
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return call(state, function, arg1, arg2, arg3, -1);
	}

	public static LuaValue call(LuaState state, LuaValue function, LuaValue arg1, LuaValue arg2, LuaValue arg3, int stack) {
		if (function.isFunction()) {
			return ((LuaFunction) function).call(state, arg1, arg2, arg3);
		} else {
			LuaValue meta = function.metatag(state, Constants.CALL);
			if (!meta.isFunction()) throw ErrorFactory.operandError(state, function, "call", stack);

			return ((LuaFunction) meta).invoke(state, ValueFactory.varargsOf(function, arg1, arg2, arg3)).first();
		}
	}

	public static Varargs invoke(LuaState state, LuaValue function, Varargs args) {
		return invoke(state, function, args, -1);
	}

	public static Varargs invoke(LuaState state, LuaValue function, Varargs args, int stack) {
		if (function.isFunction()) {
			return ((LuaFunction) function).invoke(state, args);
		} else {
			LuaValue meta = function.metatag(state, Constants.CALL);
			if (!meta.isFunction()) throw ErrorFactory.operandError(state, function, "call", stack);

			return ((LuaFunction) meta).invoke(state, ValueFactory.varargsOf(function, args));
		}
	}

	public static Varargs onInvoke(LuaState state, LuaValue function, Varargs args) {
		return onInvoke(state, function, args, -1);
	}

	public static Varargs onInvoke(LuaState state, LuaValue function, Varargs args, int stack) {
		if (function.isFunction()) {
			return ((LuaFunction) function).onInvoke(state, args);
		} else {
			LuaValue meta = function.metatag(state, Constants.CALL);
			if (!meta.isFunction()) throw ErrorFactory.operandError(state, function, "call", stack);

			return ((LuaFunction) meta).onInvoke(state, ValueFactory.varargsOf(function, args));
		}
	}

	//endregion

	//region Tables

	/**
	 * Return value for field reference including metatag processing, or {@link Constants#NIL} if it doesn't exist.
	 *
	 * @param state The current lua state
	 * @param t     {@link LuaValue} on which field is being referenced, typically a table or something with the metatag {@link Constants#INDEX} defined
	 * @param key   {@link LuaValue} naming the field to reference
	 * @return {@link LuaValue} for the {@code key} if it exists, or {@link Constants#NIL}
	 * @throws LuaError if there is a loop in metatag processing
	 */
	public static LuaValue getTable(LuaState state, LuaValue t, LuaValue key) {
		return getTable(state, t, key, -1);
	}

	public static LuaValue getTable(LuaState state, LuaValue t, LuaValue key, int stack) {
		LuaValue tm;
		int loop = 0;
		do {
			if (t.isTable()) {
				LuaValue res = ((LuaTable) t).rawget(key);
				if (!res.isNil() || (tm = t.metatag(state, Constants.INDEX)).isNil()) {
					return res;
				}
			} else if ((tm = t.metatag(state, Constants.INDEX)).isNil()) {
				throw ErrorFactory.operandError(state, t, "index", stack);
			}
			if (tm.isFunction()) {
				return ((LuaFunction) tm).call(state, t, key);
			}
			t = tm;
			stack = -1;
		}
		while (++loop < Constants.MAXTAGLOOP);
		throw new LuaError("loop in gettable");
	}

	/**
	 * Perform field assignment including metatag processing.
	 *
	 * @param state The current lua state
	 * @param t     {@link LuaValue} on which value is being set, typically a table or something with the metatag {@link Constants#NEWINDEX} defined
	 * @param key   {@link LuaValue} naming the field to assign
	 * @param value {@link LuaValue} the new value to assign to {@code key}
	 * @throws LuaError if there is a loop in metatag processing
	 */
	public static void setTable(LuaState state, LuaValue t, LuaValue key, LuaValue value) {
		setTable(state, t, key, value, -1);
	}

	public static void setTable(LuaState state, LuaValue t, LuaValue key, LuaValue value, int stack) {
		LuaValue tm;
		int loop = 0;
		do {
			if (t.isTable()) {
				LuaTable table = (LuaTable) t;
				key.checkValidKey();
				if (!table.rawget(key).isNil() || (tm = t.metatag(state, Constants.NEWINDEX)).isNil()) {
					table.rawset(key, value);
					return;
				}
			} else if ((tm = t.metatag(state, Constants.NEWINDEX)).isNil()) {
				throw ErrorFactory.operandError(state, t, "index", stack);
			}
			if (tm.isFunction()) {
				((LuaFunction) tm).call(state, t, key, value);
				return;
			}
			t = tm;
			stack = -1;
		}
		while (++loop < Constants.MAXTAGLOOP);
		throw new LuaError("loop in settable");
	}
	//endregion
}
