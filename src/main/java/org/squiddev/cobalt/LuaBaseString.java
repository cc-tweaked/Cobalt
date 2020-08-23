package org.squiddev.cobalt;

import static org.squiddev.cobalt.Constants.NIL;

public abstract class LuaBaseString extends LuaValue {
	LuaBaseString() {
		super(Constants.TSTRING);
	}

	@Override
	public final LuaTable getMetatable(LuaState state) {
		return state.stringMetatable;
	}

	@Override
	public final double checkArith() throws LuaError {
		double d = scanNumber(10);
		if (Double.isNaN(d)) {
			throw ErrorFactory.arithError(this);
		}
		return d;
	}

	@Override
	public final int checkInteger() throws LuaError {
		return (int) (long) checkDouble();
	}

	@Override
	public final LuaInteger checkLuaInteger() throws LuaError {
		return ValueFactory.valueOf(checkInteger());
	}

	@Override
	public final long checkLong() throws LuaError {
		return (long) checkDouble();
	}

	@Override
	public final double checkDouble() throws LuaError {
		double d = scanNumber(10);
		if (Double.isNaN(d)) {
			throw ErrorFactory.argError(this, "number");
		}
		return d;
	}

	@Override
	public final LuaNumber checkNumber() throws LuaError {
		return ValueFactory.valueOf(checkDouble());
	}

	@Override
	public final LuaNumber checkNumber(String msg) throws LuaError {
		double d = scanNumber(10);
		if (Double.isNaN(d)) {
			throw new LuaError(msg);
		}
		return ValueFactory.valueOf(d);
	}

	@Override
	public final LuaValue toNumber() {
		return tonumber(10);
	}

	@Override
	public final boolean isNumber() {
		double d = scanNumber(10);
		return !Double.isNaN(d);
	}

	@Override
	public final boolean isInteger() {
		double d = scanNumber(10);
		return !Double.isNaN(d) && (int) d == d;
	}

	@Override
	public final boolean isLong() {
		double d = scanNumber(10);
		return !Double.isNaN(d) && (long) d == d;
	}

	@Override
	public final double toDouble() {
		return scanNumber(10);
	}

	@Override
	public final int toInteger() {
		return (int) toLong();
	}

	@Override
	public final long toLong() {
		return (long) toDouble();
	}

	@Override
	public final double optDouble(double defval) throws LuaError {
		return checkNumber().checkDouble();
	}

	@Override
	public final int optInteger(int defval) throws LuaError {
		return checkNumber().checkInteger();
	}

	@Override
	public final LuaInteger optLuaInteger(LuaInteger defval) throws LuaError {
		return checkNumber().checkLuaInteger();
	}

	@Override
	public final long optLong(long defval) throws LuaError {
		return checkNumber().checkLong();
	}

	@Override
	public final LuaNumber optNumber(LuaNumber defval) throws LuaError {
		return checkNumber().checkNumber();
	}

	@Override
	public final LuaString optLuaString(LuaString defval) {
		return strvalue();
	}

	@Override
	public final LuaValue toLuaString() {
		return strvalue();
	}

	@Override
	public final String optString(String defval) {
		return toString();
	}

	@Override
	public abstract LuaString strvalue();

	@Override
	public final String checkString() {
		return toString();
	}

	@Override
	public final LuaString checkLuaString() {
		return strvalue();
	}

	@Override
	public final LuaBaseString checkLuaBaseString() {
		return this;
	}

	/**
	 * convert to a number using a supplied base, or NIL if it can't be converted
	 *
	 * @param base the base to use, such as 10
	 * @return IntValue, DoubleValue, or NIL depending on the content of the string.
	 * @see LuaValue#toNumber()
	 */
	public final LuaValue tonumber(int base) {
		double d = scanNumber(base);
		return Double.isNaN(d) ? NIL : ValueFactory.valueOf(d);
	}

	public abstract int length();

	/**
	 * Convert to a number in a base, or return Double.NaN if not a number.
	 *
	 * @param base the base to use, such as 10
	 * @return double value if conversion is valid, or Double.NaN if not
	 */
	public abstract double scanNumber(int base);
}
