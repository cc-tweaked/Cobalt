package org.squiddev.cobalt;

import static org.squiddev.cobalt.Factory.valueOf;

/**
 * Handles arithmetic operations
 */
public final class OperationHelper {
	private OperationHelper() {
	}

	public static LuaValue add(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return valueOf(left.checkarith() + right.checkarith());
		}

		return left.arithmt(state, Constants.ADD, right);
	}

	public static LuaValue sub(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return valueOf(left.checkarith() - right.checkarith());
		}

		return left.arithmt(state, Constants.SUB, right);
	}

	public static LuaValue mul(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return valueOf(left.checkarith() * right.checkarith());
		}

		return left.arithmt(state, Constants.MUL, right);
	}

	public static LuaValue div(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return Factory.valueOf(div(left.checkarith(), right.checkarith()));
		}

		return left.arithmt(state, Constants.DIV, right);
	}

	public static LuaValue mod(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return Factory.valueOf(mod(left.checkarith(), right.checkarith()));
		}

		return left.arithmt(state, Constants.MOD, right);
	}

	public static LuaValue pow(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type(), tRight = right.type();
		if ((tLeft == Constants.TNUMBER || tLeft == Constants.TSTRING) && (tRight == Constants.TNUMBER || tRight == Constants.TSTRING)) {
			return Factory.valueOf(Math.pow(left.checkarith(), right.checkarith()));
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
			left.compareError(right);
			return false;
		}
		switch (tLeft) {
			case Constants.TNUMBER:
				return left.todouble() < right.todouble();
			case Constants.TSTRING:
				return left.strcmp(right) < 0;
			default:
				return left.comparemt(state, Constants.LT, right).toboolean();
		}
	}

	public static LuaValue ltValue(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			left.compareError(right);
			return Constants.FALSE;
		}
		switch (tLeft) {
			case Constants.TNUMBER:
				return left.todouble() < right.todouble() ? Constants.TRUE : Constants.FALSE;
			case Constants.TSTRING:
				return left.strcmp(right) < 0 ? Constants.TRUE : Constants.FALSE;
			default:
				return left.comparemt(state, Constants.LT, right);
		}
	}

	public static boolean le(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			left.compareError(right);
			return false;
		}
		switch (tLeft) {
			case Constants.TNUMBER:
				return left.todouble() <= right.todouble();
			case Constants.TSTRING:
				return left.strcmp(right) <= 0;
			default:
				return left.comparemt(state, Constants.LE, right).toboolean();
		}
	}

	public static LuaValue leValue(LuaState state, LuaValue left, LuaValue right) {
		int tLeft = left.type();
		if (tLeft != right.type()) {
			left.compareError(right);
			return Constants.FALSE;
		}
		switch (tLeft) {
			case Constants.TNUMBER:
				return left.todouble() <= right.todouble() ? Constants.TRUE : Constants.FALSE;
			case Constants.TSTRING:
				return left.strcmp(right) <= 0 ? Constants.TRUE : Constants.FALSE;
			default:
				return left.comparemt(state, Constants.LE, right);
		}
	}

	public static boolean eq(LuaState state, LuaValue left, LuaValue right) {
		if (left == right) return true;

		int tLeft = left.type();
		if (tLeft != right.type()) return false;

		switch (tLeft) {
			case Constants.TNIL:
				return true;
			case Constants.TNUMBER:
				return left.todouble() == right.todouble();
			case Constants.TBOOLEAN:
				return left.toboolean() == right.toboolean();
			case Constants.TSTRING:
				return left.raweq(right);
			case Constants.TUSERDATA:
			case Constants.TTABLE: {
				if (left.raweq(right)) return true;

				LuaValue leftMeta = left.getMetatable(state);
				if (leftMeta == null) return false;

				LuaValue rightMeta = right.getMetatable(state);
				return rightMeta != null && LuaValue.eqmtcall(state, left, leftMeta, right, rightMeta);
			}
			default:
				return left.raweq(right);
		}
	}
}
