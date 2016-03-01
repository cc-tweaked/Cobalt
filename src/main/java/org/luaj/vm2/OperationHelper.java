package org.luaj.vm2;

import static org.luaj.vm2.Constants.*;
import static org.luaj.vm2.Factory.valueOf;

/**
 * Handles arithmetic operations
 */
public final class OperationHelper {
	private OperationHelper() {
	}

	public static LuaValue add(LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == TNUMBER || tLeft == TSTRING) && (tRight == TNUMBER || tRight == TSTRING)) {
			return valueOf(left.checkarith() + right.checkarith());
		}

		return left.arithmt(ADD, right);
	}

	public static LuaValue sub(LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == TNUMBER || tLeft == TSTRING) && (tRight == TNUMBER || tRight == TSTRING)) {
			return valueOf(left.checkarith() - right.checkarith());
		}

		return left.arithmt(SUB, right);
	}

	public static LuaValue mul(LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == TNUMBER || tLeft == TSTRING) && (tRight == TNUMBER || tRight == TSTRING)) {
			return valueOf(left.checkarith() * right.checkarith());
		}

		return left.arithmt(MUL, right);
	}

	public static LuaValue div(LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == TNUMBER || tLeft == TSTRING) && (tRight == TNUMBER || tRight == TSTRING)) {
			return valueOf(div(left.checkarith(), right.checkarith()));
		}

		return left.arithmt(DIV, right);
	}

	public static LuaValue mod(LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == TNUMBER || tLeft == TSTRING) && (tRight == TNUMBER || tRight == TSTRING)) {
			return valueOf(mod(left.checkarith(), right.checkarith()));
		}

		return left.arithmt(MOD, right);
	}

	public static LuaValue pow(LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == TNUMBER || tLeft == TSTRING) && (tRight == TNUMBER || tRight == TSTRING)) {
			return valueOf(Math.pow(left.checkarith(), right.checkarith()));
		}

		return left.arithmt(POW, right);
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

	public static boolean lt(LuaValue left, LuaValue right) {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			left.compareError(right);
			return false;
		}
		switch (tLeft) {
			case TNUMBER:
				return left.todouble() < right.todouble();
			case TSTRING:
				return left.strcmp(right) < 0;
			default:
				return left.comparemt(LT, right).toboolean();
		}
	}

	public static LuaValue ltValue(LuaValue left, LuaValue right) {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			left.compareError(right);
			return FALSE;
		}
		switch (tLeft) {
			case TNUMBER:
				return left.todouble() < right.todouble() ? TRUE : FALSE;
			case TSTRING:
				return left.strcmp(right) < 0 ? TRUE : FALSE;
			default:
				return left.comparemt(LT, right);
		}
	}

	public static boolean le(LuaValue left, LuaValue right) {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			left.compareError(right);
			return false;
		}
		switch (tLeft) {
			case TNUMBER:
				return left.todouble() <= right.todouble();
			case TSTRING:
				return left.strcmp(right) <= 0;
			default:
				return left.comparemt(LE, right).toboolean();
		}
	}

	public static LuaValue leValue(LuaValue left, LuaValue right) {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			left.compareError(right);
			return FALSE;
		}
		switch (tLeft) {
			case TNUMBER:
				return left.todouble() <= right.todouble() ? TRUE : FALSE;
			case TSTRING:
				return left.strcmp(right) <= 0 ? TRUE : FALSE;
			default:
				return left.comparemt(LE, right);
		}
	}

	public static boolean eq(LuaValue left, LuaValue right) {
		if (left == right) return true;

		int tLeft = left.type();
		if (tLeft != right.type()) return false;

		switch (tLeft) {
			case TNIL:
				return true;
			case TNUMBER:
				return left.todouble() == right.todouble();
			case TBOOLEAN:
				return left.toboolean() == right.toboolean();
			case TSTRING:
				return left.raweq(right);
			case TUSERDATA:
			case TTABLE: {
				if (left.raweq(right)) return true;

				LuaValue leftMeta = left.getMetatable();
				if (leftMeta == null) return false;

				LuaValue rightMeta = right.getMetatable();
				return rightMeta != null && LuaValue.eqmtcall(left, leftMeta, right, rightMeta);
			}
			default:
				return left.raweq(right);
		}
	}
}
