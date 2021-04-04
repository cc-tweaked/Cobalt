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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * A mutable proxy object to java BigDecimal.  Probably can be
 *   optimized away at some point, but for now it will get the things into a functional state.
 */
public class Bignum {
	private static final long LONG_SIGN_BIT = 0x8000_0000_0000_0000L;
	private static final long LONG_UNSIGNED_BITS = 0x7fff_ffff_ffff_ffffL;

	private BigDecimal val;

	public Bignum() {
		val = new BigDecimal(BigInteger.ZERO);
	}

	private Bignum(BigDecimal val) {
		this.val = val;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Bignum bignum = (Bignum) o;
		return val.equals(bignum.val);
	}

	@Override
	public int hashCode() {
		return Objects.hash(val);
	}

	/**
	 * Only used in tests, so doesn't have to be efficient
	 */
	boolean toHexString(char[] buffer, final int buffer_size) {
		String hex = val.toBigInteger().toString(16).toUpperCase();
		hex.getChars(0, hex.length(), buffer, 0);
		buffer[hex.length()] = '\0';
		return true;
	}

	public Bignum copy() {
		return new Bignum(val);
	}

	public void assignHexString(String value) {
		val = new BigDecimal(new BigInteger(value, 16));
	}

	public void assignDecimalString(String value) {
		val = new BigDecimal(value);
	}

	public void assignUInt(@Unsigned short unsignedValue) { val = fromUnsigned(unsignedValue); }
	public void assignUInt(@Unsigned int unsignedValue) { val = fromUnsigned(unsignedValue); }
	public void assignUInt(@Unsigned long unsignedValue) { val = fromUnsigned(unsignedValue); }

	// special care is needed to prevent sign-extending conversions
	public void assignUInt16(@Unsigned short unsignedValue) { assignUInt(unsignedValue); }
	public void assignUInt32(@Unsigned int unsignedValue) { assignUInt(unsignedValue); }
	public void assignUInt64(@Unsigned long unsignedValue) { assignUInt(unsignedValue); }

	public void assignBignum(Bignum other) {
		this.val = other.val;
	}

	public void assignDecimalString(char[] value) {
		this.val = new BigDecimal(value);
	}
	public void assignHexString(char[] value) {
		this.val = new BigDecimal(new BigInteger(String.valueOf(value), 16));
	}


	public void assignPowerUInt16(int base, int exponent) {
		assignPower(base, exponent);
	}

	public void assignPower(int base, int exponent) {
		//noinspection ImplicitNumericConversion
		val = BigDecimal.valueOf(base).pow(exponent);
	}

	public void addUInt64(@Unsigned long operand) { add(fromUnsigned(operand)); }

	private void add(BigDecimal operand) {
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

	public void shiftLeft(int shift_amount) {
		val = new BigDecimal(val.toBigIntegerExact().shiftLeft(shift_amount));
	}


	// special care is needed to prevent sign-extending conversions
	public void multiplyByUInt32(@Unsigned int unsignedFactor) { multiply(fromUnsigned(unsignedFactor)); }

	// special care is needed to prevent sign-extending conversions
	void multiplyByUInt64(@Unsigned long unsignedFactor) { multiply(fromUnsigned(unsignedFactor)); }

	public void multiplyByPowerOfTen(final int exponent) {
		val = val.scaleByPowerOfTen(exponent);
	}

	private void multiply(BigDecimal exponent) {
		val = val.multiply(exponent);
	}


//	void multiplyByPowerOfTen(int exponent);

	void times10() { multiplyByUInt32(10); }

	// Pseudocode:
	//  int result = this / other;
	//  this = this % other;
	// In the worst case this function is in O(this/other).
	int divideModuloIntBignum(Bignum other) {
		BigDecimal[] rets = val.divideAndRemainder(other.val);
		val = rets[1]; // remainder
		return rets[0].intValue(); // quotient
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
	private static BigDecimal fromUnsigned(@Unsigned short value) {
		return new BigDecimal(BigInteger.valueOf(Short.toUnsignedLong((@Signed short)value)));
	}

	@SuppressWarnings({"cast.unsafe", "cast"})
	private static BigDecimal fromUnsigned(@Unsigned int value) {
		return new BigDecimal(BigInteger.valueOf(Integer.toUnsignedLong((@Signed int)value)));
	}

	@SuppressWarnings("cast.unsafe")
	private static BigDecimal fromUnsigned(@Unsigned long value) {
		if ((value & LONG_SIGN_BIT) != 0L) {
			return new BigDecimal(BigInteger.valueOf(value & LONG_UNSIGNED_BITS)
					.add(BigInteger.valueOf(1L).shiftLeft(63)));
		} else {
			return BigDecimal.valueOf((@Signed long)value);
		}
	}
}
