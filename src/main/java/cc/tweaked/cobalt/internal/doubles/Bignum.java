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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

import java.math.BigInteger;
import java.util.Objects;

import static cc.tweaked.cobalt.internal.doubles.Assert.requireState;
import static cc.tweaked.cobalt.internal.doubles.UnsignedValues.toUint;

/**
 * A mutable proxy object to java BigDecimal.  Probably can be
 * optimized away at some point, but for now it will get the things into a functional state.
 */
final class Bignum {
	private static final long LONG_SIGN_BIT = 0x8000_0000_0000_0000L;
	private static final long LONG_UNSIGNED_BITS = 0x7fff_ffff_ffff_ffffL;
	private static final BigInteger INT_MASK = BigInteger.valueOf(0xffff_ffffL);
	private static final BigInteger NOT_INT_MASK = INT_MASK.not();

	private BigInteger val;

	public Bignum() {
		val = BigInteger.ZERO;
	}

	private Bignum(BigInteger val) {
		this.val = val;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Bignum bignum = (Bignum) o;
		return val.equals(bignum.val);
	}

	@Override
	@SuppressWarnings("override.receiver")
	public int hashCode() {
		return Objects.hash(val);
	}

	/**
	 * Only used in tests, so doesn't have to be efficient
	 */
	String toHexString() {
		return val.toString(16).toUpperCase();
	}

	public Bignum copy() {
		return new Bignum(val);
	}

	public void assignHexString(String value) {
		val = new BigInteger(value, 16);
	}

	public void assignDecimalString(String value) {
		val = new BigInteger(value);
	}

	public void assignUInt(@Unsigned short unsignedValue) {
		val = fromUnsigned(unsignedValue);
	}

	public void assignUInt(@Unsigned int unsignedValue) {
		val = fromUnsigned(unsignedValue);
	}

	public void assignUInt(@Unsigned long unsignedValue) {
		val = fromUnsigned(unsignedValue);
	}

	// special care is needed to prevent sign-extending conversions
	public void assignUInt16(@Unsigned short unsignedValue) {
		assignUInt(unsignedValue);
	}

	public void assignUInt32(@Unsigned int unsignedValue) {
		assignUInt(unsignedValue);
	}

	public void assignUInt64(@Unsigned long unsignedValue) {
		assignUInt(unsignedValue);
	}

	public void assignBignum(Bignum other) {
		this.val = other.val;
	}

	public void assignPowerUInt16(int base, int exponent) {
		assignPower(base, exponent);
	}

	public void assignPower(int base, int exponent) {
		//noinspection ImplicitNumericConversion
		val = BigInteger.valueOf(base).pow(exponent);
	}

	public void addUInt64(@Unsigned long operand) {
		add(fromUnsigned(operand));
	}

	private void add(BigInteger operand) {
		val = val.add(operand);
	}

	public void addBignum(Bignum other) {
		val = val.add(other.val);
	}

	// Precondition: this >= other.
	public void subtractBignum(Bignum other) {
		val = val.subtract(other.val);
	}

	public void square() {
		val = val.multiply(val);
	}

	public void shiftLeft(int shiftAmount) {
		val = val.shiftLeft(shiftAmount);
	}


	// special care is needed to prevent sign-extending conversions
	public void multiplyByUInt32(@Unsigned int unsignedFactor) {
		multiply(fromUnsigned(unsignedFactor));
	}

	// special care is needed to prevent sign-extending conversions
	void multiplyByUInt64(@Unsigned long unsignedFactor) {
		multiply(fromUnsigned(unsignedFactor));
	}

	// only used in tests
	void multiplyByPowerOfTen(final int exponent) {
		val = val.multiply(BigInteger.valueOf(10L).pow(exponent));
	}

	private void multiply(BigInteger exponent) {
		val = val.multiply(exponent);
	}


//	void multiplyByPowerOfTen(int exponent);

	void times10() {
		multiplyByUInt32(10);
	}

	// Pseudocode:
	//  int result = this / other;
	//  this = this % other;
	// In the worst case this function is in O(this/other).
	@SuppressWarnings("return.type.incompatible") // values verified positive at beginning of method
	@Unsigned int divideModuloIntBignum(Bignum other) {
		requireState(val.signum() >= 0 && other.val.signum() >= 0, "values must be positive");
		BigInteger[] rets = val.divideAndRemainder(other.val);
		val = rets[1]; // remainder

		return toUint(rets[0]); // quotient
	}

//	bool toHexString(char* buffer, const int buffer_size) const;

	// Returns
	//  -1 if a < b,
	//   0 if a == b, and
	//  +1 if a > b.
	static int compare(Bignum a, Bignum b) {
		return a.val.compareTo(b.val);
	}

	static boolean equal(Bignum a, Bignum b) {
		return compare(a, b) == 0;
	}

	static boolean lessEqual(Bignum a, Bignum b) {
		return compare(a, b) <= 0;
	}

	static boolean less(Bignum a, Bignum b) {
		return compare(a, b) < 0;
	}

	// Returns compare(a + b, c);
	static int plusCompare(Bignum a, Bignum b, Bignum c) {
		return a.val.add(b.val).compareTo(c.val);
	}

	// Returns a + b == c
	static boolean plusEqual(Bignum a, Bignum b, Bignum c) {
		return plusCompare(a, b, c) == 0;
	}

	// Returns a + b <= c
	static boolean plusLessEqual(Bignum a, Bignum b, Bignum c) {
		return plusCompare(a, b, c) <= 0;
	}

	// Returns a + b < c
	static boolean plusLess(Bignum a, Bignum b, Bignum c) {
		return plusCompare(a, b, c) < 0;
	}

	@SuppressWarnings({"cast.unsafe", "cast"})
	private static BigInteger fromUnsigned(@Unsigned short value) {
		return BigInteger.valueOf(Short.toUnsignedLong((@Signed short) value));
	}

	@SuppressWarnings({"cast.unsafe", "cast"})
	private static BigInteger fromUnsigned(@Unsigned int value) {
		return BigInteger.valueOf(Integer.toUnsignedLong((@Signed int) value));
	}

	@SuppressWarnings("cast.unsafe")
	private static BigInteger fromUnsigned(@Unsigned long value) {
		if ((value & LONG_SIGN_BIT) != 0L) {
			return BigInteger.valueOf(value & LONG_UNSIGNED_BITS)
				.add(BigInteger.valueOf(1L).shiftLeft(63));
		} else {
			return BigInteger.valueOf((@Signed long) value);
		}
	}
}
