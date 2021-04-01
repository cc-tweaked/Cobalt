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
import java.math.RoundingMode;
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
	boolean ToHexString(char[] buffer, final int buffer_size) {
		String hex = val.toBigInteger().toString(16).toUpperCase();
		hex.getChars(0, hex.length(), buffer, 0);
		buffer[hex.length()] = '\0';
		return true;
	}

	public Bignum copy() {
		return new Bignum(val);
	}

	public void AssignHexString(String value) {
		val = new BigDecimal(new BigInteger(value, 16));
	}

	public void AssignDecimalString(String value) {
		val = new BigDecimal(value);
	}

	public void AssignUInt(@Unsigned short unsignedValue) { val = fromUnsigned(unsignedValue); }
	public void AssignUInt(@Unsigned int unsignedValue) { val = fromUnsigned(unsignedValue); }
	public void AssignUInt(@Unsigned long unsignedValue) { val = fromUnsigned(unsignedValue); }

	// special care is needed to prevent sign-extending conversions
	public void AssignUInt16(@Unsigned short unsignedValue) { AssignUInt(unsignedValue); }
	public void AssignUInt32(@Unsigned int unsignedValue) { AssignUInt(unsignedValue); }
	public void AssignUInt64(@Unsigned long unsignedValue) { AssignUInt(unsignedValue); }

	public void AssignBignum(Bignum other) {
		this.val = other.val;
	}

	public void AssignDecimalString(char[] value) {
		this.val = new BigDecimal(value);
	}
	public void AssignHexString(char[] value) {
		this.val = new BigDecimal(new BigInteger(String.valueOf(value), 16));
	}


	public void AssignPowerUInt16(int base, int exponent) {
		AssignPower(base, exponent);
	}

	public void AssignPower(int base, int exponent) {
		//noinspection ImplicitNumericConversion
		val = BigDecimal.valueOf(base).pow(exponent);
	}

	public void AddUInt64(@Unsigned long operand) { add(fromUnsigned(operand)); }

	private void add(BigDecimal operand) {
		val = val.add(operand);
	}

	public void AddBignum(Bignum other) {
		val = val.add(other.val);
	}

	// Precondition: this >= other.
	public void SubtractBignum(Bignum other) {
		val = val.subtract(other.val);
	}

	public void Square() {
		val = val.multiply(val);
	}

	public void ShiftLeft(int shift_amount) {
		val = new BigDecimal(val.toBigIntegerExact().shiftLeft(shift_amount));
	}


	// special care is needed to prevent sign-extending conversions
	public void MultiplyByUInt32(@Unsigned int unsignedFactor) { multiply(fromUnsigned(unsignedFactor)); }

	// special care is needed to prevent sign-extending conversions
	void MultiplyByUInt64(@Unsigned long unsignedFactor) { multiply(fromUnsigned(unsignedFactor)); }

	public void MultiplyByPowerOfTen(final int exponent) {
		val = val.scaleByPowerOfTen(exponent);
	}

	private void multiply(BigDecimal exponent) {
		val = val.multiply(exponent);
	}


//	void MultiplyByPowerOfTen(int exponent);

	void Times10() { MultiplyByUInt32(10); }

	// Pseudocode:
	//  int result = this / other;
	//  this = this % other;
	// In the worst case this function is in O(this/other).
	int DivideModuloIntBignum(Bignum other) {
		int quotient = val.divide(other.val, RoundingMode.DOWN).intValue();
		val = val.remainder(other.val);
		return quotient;
	}

//	bool ToHexString(char* buffer, const int buffer_size) const;

	// Returns
	//  -1 if a < b,
	//   0 if a == b, and
	//  +1 if a > b.
	static int Compare(Bignum a, Bignum b) {
		return a.val.compareTo(b.val);
	}

	static boolean Equal(Bignum a, Bignum b) {
		return Compare(a, b) == 0;
	}
	static boolean LessEqual(Bignum a, Bignum b) {
		return Compare(a, b) <= 0;
	}
	static boolean Less(Bignum a, Bignum b) {
		return Compare(a, b) < 0;
	}
	// Returns Compare(a + b, c);
	static int PlusCompare(Bignum a, Bignum b, Bignum c) {
		return a.val.add(b.val).compareTo(c.val);
	}
	// Returns a + b == c
	static boolean PlusEqual(Bignum a, Bignum b, Bignum c) {
		return PlusCompare(a, b, c) == 0;
	}
	// Returns a + b <= c
	static boolean PlusLessEqual(Bignum a, Bignum b, Bignum c) {
		return PlusCompare(a, b, c) <= 0;
	}
	// Returns a + b < c
	static boolean PlusLess(Bignum a, Bignum b, Bignum c) {
		return PlusCompare(a, b, c) < 0;
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
