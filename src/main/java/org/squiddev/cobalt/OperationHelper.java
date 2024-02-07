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

import cc.tweaked.cobalt.internal.unwind.AutoUnwind;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.Lua.*;
import static org.squiddev.cobalt.LuaDouble.valueOf;
import static org.squiddev.cobalt.LuaInteger.valueOf;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_LEQ;

/**
 * Handles arithmetic operations
 */
public final class OperationHelper {
	private OperationHelper() {
	}

	//region Binary
	public static LuaValue add(LuaState state, LuaValue left, LuaValue right) throws LuaError, UnwindThrowable {
		double dLeft, dRight;
		if (checkNumber(left, dLeft = left.toDouble()) && checkNumber(right, dRight = right.toDouble())) {
			return valueOf(dLeft + dRight);
		} else {
			return arithMetatable(state, ADD, left, right);
		}
	}

	public static LuaValue sub(LuaState state, LuaValue left, LuaValue right) throws LuaError, UnwindThrowable {
		double dLeft, dRight;
		if (checkNumber(left, dLeft = left.toDouble()) && checkNumber(right, dRight = right.toDouble())) {
			return valueOf(dLeft - dRight);
		} else {
			return arithMetatable(state, SUB, left, right);
		}
	}

	public static LuaValue mul(LuaState state, LuaValue left, LuaValue right) throws LuaError, UnwindThrowable {
		if (left instanceof LuaInteger l && right instanceof LuaInteger r) {
			return valueOf((long) l.intValue() * (long) r.intValue());
		}

		double dLeft, dRight;
		if (checkNumber(left, dLeft = left.toDouble()) && checkNumber(right, dRight = right.toDouble())) {
			return valueOf(dLeft * dRight);
		} else {
			return arithMetatable(state, MUL, left, right);
		}
	}

	public static LuaValue div(LuaState state, LuaValue left, LuaValue right) throws LuaError, UnwindThrowable {
		double dLeft, dRight;
		if (checkNumber(left, dLeft = left.toDouble()) && checkNumber(right, dRight = right.toDouble())) {
			return valueOf(div(dLeft, dRight));
		} else {
			return arithMetatable(state, DIV, left, right);
		}
	}

	public static LuaValue mod(LuaState state, LuaValue left, LuaValue right) throws LuaError, UnwindThrowable {
		double dLeft, dRight;
		if (checkNumber(left, dLeft = left.toDouble()) && checkNumber(right, dRight = right.toDouble())) {
			return valueOf(mod(dLeft, dRight));
		} else {
			return arithMetatable(state, MOD, left, right);
		}
	}

	public static LuaValue pow(LuaState state, LuaValue left, LuaValue right) throws LuaError, UnwindThrowable {
		double dLeft, dRight;
		if (checkNumber(left, dLeft = left.toDouble()) && checkNumber(right, dRight = right.toDouble())) {
			return valueOf(Math.pow(dLeft, dRight));
		} else {
			return arithMetatable(state, POW, left, right);
		}
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
		double mod = lhs % rhs;
		return mod * rhs < 0 ? mod + rhs : mod;
	}

	/**
	 * Perform metatag processing for arithmetic operations.
	 * <p>
	 * Finds the supplied metatag value for {@code this} or {@code op2} and invokes it,
	 * or throws {@link LuaError} if neither is defined.
	 *
	 * @param state The current lua state
	 * @param tag   The metatag to look up
	 * @param left  The left operand value to perform the operation with
	 * @param right The other operand value to perform the operation with
	 * @return {@link LuaValue} resulting from metatag processing
	 * @throws LuaError        if metatag was not defined for either operand or the underlying operator errored.
	 * @throws UnwindThrowable If calling the metatable function yielded.
	 */
	private static LuaValue arithMetatable(LuaState state, LuaValue tag, LuaValue left, LuaValue right) throws LuaError, UnwindThrowable {
		return Dispatch.call(state, getMetatable(state, tag, left, right), left, right);
	}

	/**
	 * Perform metatag processing for arithmetic operations.
	 * <p>
	 * Finds the supplied metatag value for {@code this} or {@code op2} and invokes it,
	 * or throws {@link LuaError} if neither is defined.
	 *
	 * @param state The current lua state
	 * @param tag   The metatag to look up
	 * @param left  The left operand value to perform the operation with
	 * @param right The other operand value to perform the operation with
	 * @return {@link LuaValue} resulting from metatag processing
	 * @throws LuaError if metatag was not defined for either operand
	 */
	private static LuaValue getMetatable(LuaState state, LuaValue tag, LuaValue left, LuaValue right) throws LuaError {
		LuaValue h = left.metatag(state, tag);
		if (!h.isNil()) return h;

		h = right.metatag(state, tag);
		if (!h.isNil()) return h;


		throw createArithmeticError(state, left, right);
	}

	private static LuaError createArithmeticError(LuaState state, LuaValue left, LuaValue right) {
		// Read the current instruction and try to determine the registers involved. This allows us to avoid passing the
		// registers from the interpreter to here.
		// PUC Lua just does this by searching the stack for the given value, but that's not possible for us :(
		int b = -1, c = -1;
		DebugFrame frame = DebugState.get(state).getStack();
		if (frame != null && frame.closure != null) {
			var prototype = frame.closure.getPrototype();
			if (frame.pc >= 0 && frame.pc <= prototype.code.length) {
				int i = prototype.code[frame.pc];
				assert (
					getOpMode(GET_OPCODE(i)) == iABC && getBMode(GET_OPCODE(i)) == OpArgK && getCMode(GET_OPCODE(i)) == OpArgK
				) : getOpName(GET_OPCODE(i)) + " is not an iABC/RK/RX instruction";

				b = GETARG_B(i);
				c = GETARG_C(i);
			}
		}

		LuaValue value;
		int stack;
		if (!left.isNumber()) {
			value = left;
			stack = b;
		} else {
			value = right;
			stack = c;
		}

		return ErrorFactory.operandError(state, value, "perform arithmetic on", stack);
	}

	public static LuaValue concatNonStrings(LuaState state, LuaValue left, LuaValue right, int leftStack, int rightStack) throws LuaError, UnwindThrowable {
		LuaValue h = left.metatag(state, Constants.CONCAT);
		if (h.isNil() && (h = right.metatag(state, Constants.CONCAT)).isNil()) {
			if (left.isString()) {
				throw ErrorFactory.operandError(state, right, "concatenate", rightStack);
			} else {
				throw ErrorFactory.operandError(state, left, "concatenate", leftStack);
			}
		}

		return Dispatch.call(state, h, left, right);
	}
	//endregion

	//region Compare
	public static boolean lt(LuaState state, LuaValue left, LuaValue right) throws LuaError, UnwindThrowable {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			throw ErrorFactory.compareError(left, right);
		}
		switch (tLeft) {
			case TNUMBER:
				return left.toDouble() < right.toDouble();
			case TSTRING:
				return left.checkLuaString().compareTo(right.checkLuaString()) < 0;
			default:
				LuaValue h = left.metatag(state, Constants.LT);
				if (!h.isNil() && h == right.metatag(state, Constants.LT)) {
					return Dispatch.call(state, h, left, right).toBoolean();
				} else {
					throw ErrorFactory.compareError(left, right);
				}
		}
	}

	public static boolean le(LuaState state, LuaValue left, LuaValue right) throws LuaError, UnwindThrowable {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			throw ErrorFactory.compareError(left, right);
		}
		switch (tLeft) {
			case TNUMBER:
				return left.toDouble() <= right.toDouble();
			case TSTRING:
				return left.checkLuaString().compareTo(right.checkLuaString()) <= 0;
			default:
				LuaValue h = left.metatag(state, Constants.LE);
				if (h.isNil()) {
					h = left.metatag(state, Constants.LT);
					if (!h.isNil() && h == right.metatag(state, Constants.LT)) {
						DebugFrame frame = DebugState.get(state).getStackUnsafe();

						frame.flags |= FLAG_LEQ;
						boolean result = !Dispatch.call(state, h, right, left).toBoolean();
						frame.flags ^= FLAG_LEQ;

						return result;
					}
				} else if (h == right.metatag(state, Constants.LE)) {
					return Dispatch.call(state, h, left, right).toBoolean();
				}

				throw ErrorFactory.compareError(left, right);
		}
	}

	public static boolean eq(LuaState state, LuaValue left, LuaValue right) throws LuaError, UnwindThrowable {
		int tLeft = left.type();
		if (tLeft != right.type()) return false;

		return switch (tLeft) {
			case TNIL -> true;
			case TNUMBER -> left.toDouble() == right.toDouble();
			case TBOOLEAN -> left.toBoolean() == right.toBoolean();
			case TSTRING -> left == right || left.equals(right);
			case TUSERDATA, TTABLE -> {
				if (left == right || left.equals(right)) yield true;

				LuaTable leftMeta = left.getMetatable(state);
				if (leftMeta == null) yield false;

				LuaTable rightMeta = right.getMetatable(state);
				if (rightMeta == null) yield false;

				LuaValue h = leftMeta.rawget(CachedMetamethod.EQ);
				yield !(h.isNil() || h != rightMeta.rawget(CachedMetamethod.EQ)) && Dispatch.call(state, h, left, right).toBoolean();
			}
			default -> left == right || left.equals(right);
		};
	}
	//endregion

	//region Unary

	/**
	 * Length operator: return lua length of object including metatag processing
	 *
	 * @param state The current lua state
	 * @param value The value to ge the length of
	 * @return length as defined by the lua # operator or metatag processing result
	 * @throws LuaError        If {@code value} is not a table or string, and has no {@link Constants#LEN} metatag
	 * @throws UnwindThrowable If the {@code __len} metamethod yielded.
	 */
	public static LuaValue length(LuaState state, LuaValue value) throws LuaError, UnwindThrowable {
		switch (value.type()) {
			case Constants.TTABLE: {
				LuaValue h = value.metatag(state, CachedMetamethod.LEN);
				if (h.isNil()) {
					return valueOf(((LuaTable) value).length());
				} else {
					return Dispatch.call(state, h, value);
				}
			}
			case TSTRING:
				return valueOf(((LuaString) value).length());
			default: {
				LuaValue h = value.metatag(state, CachedMetamethod.LEN);
				if (h.isNil()) throw createUnaryOpError(state, value, "get length of");
				return Dispatch.call(state, h, value);
			}
		}
	}

	@AutoUnwind
	public static int intLength(LuaState state, LuaValue table) throws LuaError, UnwindThrowable {
		LuaValue length = length(state, table);
		if (length instanceof LuaInteger i) return i.intValue();

		// TODO: Would be useful to have a checkInteger function which accepts an error message.
		double value = length.toDouble();
		int intValue = (int) value;
		if (value == intValue) return intValue;

		throw new LuaError("object length is not an integer");
	}

	/**
	 * Unary minus: return negative value {@code (-this)} as defined by lua unary minus operator
	 *
	 * @param state The current lua state
	 * @param value Value to get the minus of
	 * @return numeric inverse as {@link LuaNumber} if numeric, or metatag processing result if {@link Constants#UNM} metatag is defined
	 * @throws LuaError        If {@code value} is not a table or string, and has no {@link Constants#UNM} metatag
	 * @throws UnwindThrowable If the {@code __unm} metamethod yielded.
	 */
	public static LuaValue neg(LuaState state, LuaValue value) throws LuaError, UnwindThrowable {
		int type = value.type();
		if (type == TNUMBER) {
			if (value instanceof LuaInteger) {
				int x = ((LuaInteger) value).intValue();
				if (x != Integer.MIN_VALUE) return valueOf(-x);
			}

			return valueOf(-value.toDouble());
		} else if (type == TSTRING) {
			double res = value.toDouble();
			if (!Double.isNaN(res)) return valueOf(-res);
		}

		LuaValue meta = value.metatag(state, Constants.UNM);
		if (meta.isNil()) throw createUnaryOpError(state, value, "perform arithmetic on");

		return Dispatch.call(state, meta, value);
	}

	private static boolean checkNumber(LuaValue lua, double value) {
		return lua.type() == TNUMBER || !Double.isNaN(value);
	}

	private static LuaError createUnaryOpError(LuaState state, LuaValue value, String message) {
		// Read the current instruction and try to determine the register involved. This allows us to avoid passing the
		// registers from the interpreter to here.
		// PUC Lua just does this by searching the stack for the given value, but that's not possible for us :(
		int b = -1;
		DebugFrame frame = DebugState.get(state).getStack();
		if (frame != null && frame.closure != null) {
			var prototype = frame.closure.getPrototype();
			if (frame.pc >= 0 && frame.pc <= prototype.code.length) {
				int i = prototype.code[frame.pc];
				assert (
					getOpMode(GET_OPCODE(i)) == iABC && getBMode(GET_OPCODE(i)) == OpArgR
				) : getOpName(GET_OPCODE(i)) + " is not an iABC/RK instruction";

				b = GETARG_B(i);
			}
		}

		return ErrorFactory.operandError(state, value, message, b);
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
	 * @throws LuaError        If there is a loop in metatag processing
	 * @throws UnwindThrowable If the {@code __get} metamethod yielded.
	 */
	public static LuaValue getTable(LuaState state, LuaValue t, LuaValue key) throws LuaError, UnwindThrowable {
		return getTable(state, t, key, -1);
	}

	public static LuaValue getTable(LuaState state, LuaValue t, int key) throws LuaError, UnwindThrowable {
		// Optimised case for an integer key.
		if (t instanceof LuaTable table) {
			LuaValue value = table.rawget(key);
			if (!value.isNil()) return value;
		}

		// Fall back to the slow lookup.
		return getTable(state, t, valueOf(key));
	}

	public static LuaValue getTable(LuaState state, LuaValue t, LuaValue key, int stack) throws LuaError, UnwindThrowable {
		LuaValue tm;
		int loop = 0;
		do {
			if (t instanceof LuaTable table) {
				LuaValue res = table.rawget(key);
				if (!res.isNil() || (tm = t.metatag(state, CachedMetamethod.INDEX)).isNil()) {
					return res;
				}
			} else if ((tm = t.metatag(state, CachedMetamethod.INDEX)).isNil()) {
				throw ErrorFactory.operandError(state, t, "index", stack);
			}

			if (tm instanceof LuaFunction metaFunc) return Dispatch.call(state, metaFunc, t, key);

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
	 * @throws LuaError        If there is a loop in metatag processing
	 * @throws UnwindThrowable If the {@code __set} metamethod yielded.
	 */
	public static void setTable(LuaState state, LuaValue t, LuaValue key, LuaValue value) throws LuaError, UnwindThrowable {
		setTable(state, t, key, value, -1);
	}

	public static void setTable(LuaState state, LuaValue t, int key, LuaValue value) throws LuaError, UnwindThrowable {
		// Optimised case for an integer key.
		if (t instanceof LuaTable table && table.trySet(key, value)) return;

		// Fall back to the slow lookup.
		setTable(state, t, valueOf(key), value);
	}

	public static void setTable(LuaState state, LuaValue t, LuaValue key, LuaValue value, int stack) throws LuaError, UnwindThrowable {
		int loop = 0;
		do {
			LuaValue tm;
			if (t instanceof LuaTable table && table.trySet(key, value)) return;
			if ((tm = t.metatag(state, CachedMetamethod.NEWINDEX)).isNil()) {
				throw ErrorFactory.operandError(state, t, "index", stack);
			}
			if (tm instanceof LuaFunction metaFunc) {
				Dispatch.call(state, metaFunc, t, key, value);
				return;
			}
			t = tm;
			stack = -1;
		}
		while (++loop < Constants.MAXTAGLOOP);
		throw new LuaError("loop in settable");
	}
	//endregion

	public static LuaValue toString(LuaState state, LuaValue value) throws LuaError, UnwindThrowable {
		LuaValue h = value.metatag(state, Constants.TOSTRING);
		return h.isNil() ? toStringDirect(value) : Dispatch.call(state, h, value);
	}

	public static LuaString checkToString(LuaValue value) throws LuaError {
		LuaValue asStr = value.toLuaString();
		if (asStr.isNil()) throw new LuaError("'__tostring' must return a string");
		return (LuaString) asStr;
	}

	/**
	 * A version of {@link #toString(LuaState, LuaValue)} which doesn't obey metamethods.
	 * <p>
	 * Technically this is wrong, as <code>luaL_tolstring</code> should use metamethods, but sometimes it's easier
	 * to not worry about them yielding.
	 *
	 * @param value The value to convert to a string.
	 * @return This value as a string.
	 * @see <a href="https://www.lua.org/source/5.3/lauxlib.c.html#luaL_tolstring">luaL_tolstring</a>
	 */
	public static LuaString toStringDirect(LuaValue value) {
		LuaValue v = value.toLuaString();
		return v.isNil() ? LuaString.valueOf(value.toString()) : (LuaString) v;
	}
}
