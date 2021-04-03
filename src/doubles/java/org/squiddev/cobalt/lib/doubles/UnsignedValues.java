/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 sir-maniac
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
package org.squiddev.cobalt.lib.doubles;


import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public final class UnsignedValues {
	@SuppressWarnings("ImplicitNumericConversion")
	private static final int ASCII_ZERO = '0';

	private static final @Unsigned long INT_MASK = 0xffff_ffffL;
	private static final @Unsigned long NOT_INT_MASK = ~INT_MASK;
	private static final @Unsigned int INT_MIN_VALUE = (@Unsigned int)Integer.MIN_VALUE;
	private static final @Unsigned long LONG_MIN_VALUE = (@Unsigned long)Long.MIN_VALUE;

	/**
	 * return true the unsigned long value can fit into an unsigned int
	 */
	public static boolean isAssignableToUint(@Unsigned long value) {
		return (value & NOT_INT_MASK) == 0;
	}

	@SuppressWarnings("argument.type.incompatible") // because API method isn't annotated
	public static @Unsigned long toUlong(@Unsigned int value) {
		return Integer.toUnsignedLong(value);
	}

	/** convert a signed value to an unsigned long */
	@SuppressWarnings("argument.type.incompatible") // because API method isn't annotated
	public static @Unsigned long toUlongS(@Signed int value) {
		return Integer.toUnsignedLong(value);
	}

	public static @Unsigned int toUint(@Unsigned long value) {
		// TODO casting to int might be enough
		return (int)(value & INT_MASK);
	}

	public static @Unsigned int uDivide(@Unsigned int dividend, @Unsigned int divisor) {
		return Integer.divideUnsigned(dividend, divisor);
	}

	public static @Unsigned int uRemainder(@Unsigned int dividend, @Unsigned int divisor) {
		return Integer.remainderUnsigned(dividend, divisor);
	}

	public static @Unsigned long uDivide(@Unsigned long dividend, @Unsigned long divisor) {
		return Long.divideUnsigned(dividend, divisor);
	}

	public static @Unsigned long uRemainder(@Unsigned long dividend, @Unsigned long divisor) {
		return Long.remainderUnsigned(dividend, divisor);
	}


	@SuppressWarnings("comparison.unsignedlhs")
	public static boolean uintLE(@Unsigned int lval, @Unsigned int rval) {
		return lval + INT_MIN_VALUE <= rval + INT_MIN_VALUE;
	}
	@SuppressWarnings("comparison.unsignedlhs")
	public static boolean uintLT(@Unsigned int lval, @Unsigned int rval) {
		return lval + INT_MIN_VALUE < rval + INT_MIN_VALUE;
	}

	@SuppressWarnings("comparison.unsignedlhs")
	public static boolean uintGE(@Unsigned int lval, @Unsigned int rval) {
		return lval + INT_MIN_VALUE >= rval + INT_MIN_VALUE;
	}
	@SuppressWarnings("comparison.unsignedlhs")
	public static boolean uintGT(@Unsigned int lval, @Unsigned int rval) {
		return lval + INT_MIN_VALUE > rval + INT_MIN_VALUE;
	}

	@SuppressWarnings("comparison.unsignedlhs")
	public static boolean ulongGE(@Unsigned long lval, @Unsigned long rval) {
		return lval + LONG_MIN_VALUE >= rval + LONG_MIN_VALUE;
	}
	@SuppressWarnings("comparison.unsignedlhs")
	public static boolean ulongGT(@Unsigned long lval, @Unsigned long rval) {
		return lval + LONG_MIN_VALUE > rval + LONG_MIN_VALUE;
	}

	@SuppressWarnings("comparison.unsignedlhs")
	public static boolean ulongLE(@Unsigned long lval, @Unsigned long rval) {
		return lval + LONG_MIN_VALUE <= rval + LONG_MIN_VALUE;
	}
	@SuppressWarnings("comparison.unsignedlhs")
	public static boolean ulongLT(@Unsigned long lval, @Unsigned long rval) {
		return lval + LONG_MIN_VALUE < rval + LONG_MIN_VALUE;
	}

	private UnsignedValues() {}

	@SuppressWarnings({"cast.unsafe", "ImplicitNumericConversion"})
	static char digitToChar(@Unsigned long digit) {
		if (ulongGT(digit, 9))
			throw new IllegalArgumentException("digit must be 0-9");
		return (char)(digit + ASCII_ZERO);
	}

	@SuppressWarnings("cast.unsafe")
	static char digitToChar(@Unsigned int digit) {
		if (uintGT(digit, 9))
			throw new IllegalArgumentException("digit must be 0-9");
		return (char)(digit + ASCII_ZERO);
	}
}
