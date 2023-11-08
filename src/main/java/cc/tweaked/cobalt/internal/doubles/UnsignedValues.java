/*
 * Copyright 2021 sir-maniac. All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Google Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cc.tweaked.cobalt.internal.doubles;


import org.checkerframework.checker.signedness.qual.SignedPositive;
import org.checkerframework.checker.signedness.qual.Unsigned;

import java.math.BigInteger;

final class UnsignedValues {
	@SuppressWarnings("ImplicitNumericConversion")
	private static final int ASCII_ZERO = '0';

	private static final @Unsigned long INT_MASK = 0xffff_ffffL;
	private static final @Unsigned long NOT_INT_MASK = ~INT_MASK;

	/**
	 * return true the unsigned long value can fit into an unsigned int
	 */
	public static boolean isAssignableToUint(@Unsigned long value) {
		return (value & NOT_INT_MASK) == 0L;
	}

	public static @Unsigned long toUlong(@Unsigned int value) {
		return Integer.toUnsignedLong(value);
	}

	/**
	 * convert a signed value to an unsigned long
	 */
	public static @Unsigned long toUlongFromSigned(@SignedPositive int value) {
		if (value < 0) throw new IllegalArgumentException("value must be positive");
		return Integer.toUnsignedLong(value);
	}

	public static @Unsigned int toUint(@Unsigned long value) {
		// TODO casting to int might be enough
		return (int) (value & INT_MASK);
	}

	/**
	 * Convert a positive {@link BigInteger} to asn unsigned int.  Positivity is checked,
	 * but Overflow is not checked.
	 */
	@SuppressWarnings("return") // value arguments is check for positivity
	public static @Unsigned int toUint(BigInteger value) {
		if (value.signum() < 0) throw new IllegalArgumentException("value must be positive");
		return (int) value.and(BigInteger.valueOf(INT_MASK)).longValue();
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

	public static boolean uintLE(@Unsigned int lval, @Unsigned int rval) {
		return Integer.compareUnsigned(lval, rval) <= 0;
	}

	public static boolean uintLT(@Unsigned int lval, @Unsigned int rval) {
		return Integer.compareUnsigned(lval, rval) < 0;
	}

	public static boolean uintGE(@Unsigned int lval, @Unsigned int rval) {
		return Integer.compareUnsigned(lval, rval) >= 0;
	}

	public static boolean uintGT(@Unsigned int lval, @Unsigned int rval) {
		return Integer.compareUnsigned(lval, rval) > 0;
	}

	public static boolean ulongGE(@Unsigned long lval, @Unsigned long rval) {
		return Long.compareUnsigned(lval, rval) >= 0;
	}

	public static boolean ulongGT(@Unsigned long lval, @Unsigned long rval) {
		return Long.compareUnsigned(lval, rval) > 0;
	}

	public static boolean ulongLE(@Unsigned long lval, @Unsigned long rval) {
		return Long.compareUnsigned(lval, rval) <= 0;
	}

	public static boolean ulongLT(@Unsigned long lval, @Unsigned long rval) {
		return Long.compareUnsigned(lval, rval) < 0;
	}

	private UnsignedValues() {
	}

	// method overridden to avoid implicit casts
	@SuppressWarnings({"cast.unsafe"})
	public static char digitToChar(@Unsigned long digit) {
		if (ulongGT(digit, 9)) throw new IllegalArgumentException("digit must be 0-9");
		return (char) (digit + ASCII_ZERO);
	}

	@SuppressWarnings("cast.unsafe")
	public static char digitToChar(@Unsigned int digit) {
		if (uintGT(digit, 9)) throw new IllegalArgumentException("digit must be 0-9");
		return (char) (digit + ASCII_ZERO);
	}
}
