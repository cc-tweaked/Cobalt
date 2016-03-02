package org.squiddev.cobalt;

/**
 * Factory class for errors
 */
public class ErrorFactory {
	/**
	 * Throw a {@link LuaError} indicating an invalid argument was supplied to a function
	 *
	 * @param expected String naming the type that was expected
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	public static LuaError argError(LuaValue value, String expected) {
		return new LuaError("bad argument: " + expected + " expected, got " + value.typeName());
	}

	/**
	 * Throw a {@link LuaError} indicating an invalid argument was supplied to a function
	 *
	 * @param iarg index of the argument that was invalid, first index is 1
	 * @param msg  String providing information about the invalid argument
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	public static LuaError argError(int iarg, String msg) {
		return new LuaError("bad argument #" + iarg + ": " + msg);
	}

	/**
	 * Throw a {@link LuaError} indicating an invalid type was supplied to a function
	 *
	 * @param value    The current value
	 * @param expected String naming the type that was expected
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	public static LuaError typeError(LuaValue value, String expected) {
		return new LuaError(expected + " expected, got " + value.typeName());
	}

	/**
	 * Throw a {@link LuaError} indicating an operation is not implemented
	 *
	 * @param value The current value
	 * @param fun   Function that hasn't been implemented
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	public static LuaError unimplemented(LuaValue value, String fun) {
		return new LuaError("'" + fun + "' not implemented for " + value.typeName());
	}

	/**
	 * Throw a {@link LuaError} indicating an illegal operation occurred,
	 * typically involved in managing weak references
	 *
	 * @param op       Operation
	 * @param typename Current type
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	public static LuaError illegal(String op, String typename) {
		return new LuaError("illegal operation '" + op + "' for " + typename);
	}

	/**
	 * Throw a {@link LuaError} based on an arithmetic error such as add, or pow,
	 * typically due to an invalid operand type
	 *
	 * @param value The value to perform arithmetic on
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	public static LuaError arithError(LuaValue value) {
		return new LuaError("attempt to perform arithmetic on " + value.typeName());
	}

	/**
	 * Throw a {@link LuaError} based on a comparison error such as greater-than or less-than,
	 * typically due to an invalid operand type
	 *
	 * @param lhs Left-hand-side of the comparison that resulted in the error.
	 * @param rhs Right-hand-side of the comparison that resulted in the error.
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	public static LuaError compareError(LuaValue lhs, LuaValue rhs) {
		return new LuaError("attempt to compare " + lhs.typeName() + " with " + rhs.typeName());
	}

	/**
	 * Throw {@link LuaError} indicating index was attempted on illegal type
	 *
	 * @param value The value to index
	 * @throws LuaError when called.
	 */
	public static LuaError indexError(LuaValue value) {
		return new LuaError("attempt to index ? (a " + value.typeName() + " value)");
	}
}
