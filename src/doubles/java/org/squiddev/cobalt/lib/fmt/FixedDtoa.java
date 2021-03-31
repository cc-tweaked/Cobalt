/*
 * Copyright 2010 the V8 project authors. All rights reserved.
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

package org.squiddev.cobalt.lib.fmt;

import static org.squiddev.cobalt.lib.fmt.Assert.DOUBLE_CONVERSION_ASSERT;

public class FixedDtoa {

	// Represents a 128bit type. This class should be replaced by a native type on
	// platforms that support 128bit integers.
	// Protected for testins
	static class UInt128 {
		private static final long kMask32 = 0xFFFFFFFFL;
		// Value == (high_bits_ << 64) + low_bits_
		private final long high;
		private final long low;

		public UInt128() {
			this.high = 0;
			this.low = 0;
		}

		public UInt128(UnsignedLong high, UnsignedLong low) {
			this(high.unsafeLongValue(), low.unsafeLongValue());
		}

		private UInt128(long high, long low) {
			this.high = high;
			this.low = low;
		}

		// package for testing
		long rawHigh() {
			return high;
		}

		// package for testing
		long rawLow() {
			return low;
		}

		public UInt128 times(long multiplicand) {
			long accumulator;

			accumulator = (low & kMask32) * multiplicand;
			long part = accumulator & kMask32;
			accumulator >>>= 32;
			accumulator = accumulator + (low >>> 32) * multiplicand;
			long newLowBits = (accumulator << 32) + part;
			accumulator >>>= 32;
			accumulator = accumulator + (high & kMask32) * multiplicand;
			part = accumulator & kMask32;
			accumulator >>>= 32;
			accumulator = accumulator + (high >>> 32) * multiplicand;
			long newHighBits = (accumulator << 32) + part;
			DOUBLE_CONVERSION_ASSERT((accumulator >>> 32) == 0);

			return new UInt128(newHighBits, newLowBits);
		}

		public UInt128 shift(int shift_amount) {
			DOUBLE_CONVERSION_ASSERT(-64 <= shift_amount && shift_amount <= 64);
			long nHigh, nLow;
			if (shift_amount == 0) {
				return this;
			} else if (shift_amount == -64) {
				nHigh = low;
				nLow = 0;
			} else if (shift_amount == 64) {
				nHigh = 0;
				nLow = high;
			} else if (shift_amount <= 0) {
				nHigh = high << -shift_amount;
				nHigh += low >>> (64 + shift_amount);
				nLow = low << -shift_amount;
			} else {
				nLow = low >>> shift_amount;
				nLow += high << (64 - shift_amount);
				nHigh = high >>> shift_amount;
			}
			return new UInt128(nHigh, nLow);
		}

		// Modifies *this to *this MOD (2^power).
		// Returns *this DIV (2^power).
		public QuotientRemainder divModPowerOf2(int power) {
			long remHigh, remLow;
			int quotient;
			if (power >= 64) {
				quotient = (int) (high >>> (power - 64));
				remHigh = high - (((long) quotient) << (power - 64));
				remLow = low;
			} else {
				long partLow = low >>> power;
				long partHigh = high << (64 - power);
				quotient = (int) (partLow + partHigh);
				remHigh = 0;
				remLow = low - (partLow << power);
			}
			return new QuotientRemainder(quotient, new UInt128(remHigh, remLow));
		}

		public boolean isZero() {
			return high == 0 && low == 0;
		}

		public int bitAt(int position) {
			if (position >= 64) {
				return (int) (high >>> (position - 64)) & 1;
			} else {
				return (int) (low >>> position) & 1;
			}
		}

		public static class QuotientRemainder {
			public final int quotient;
			public final UInt128 remainder;

			public  QuotientRemainder(int quotient, UInt128 remainder) {
				this.quotient = quotient;
				this.remainder = remainder;
			}
		}
	}

	private static final int kDoubleSignificandSize = 53;  // Includes the hidden bit.

	private static void FillDigits32FixedLength(UnsignedInt number, int requested_length,
												char[] buffer, int[] length) {
		int len = length[0];
		for (int i = requested_length - 1; i >= 0; --i) {
			buffer[len + i] = (char)('0' + number.mod10());
			number = number.divideBy(10);
		}
  		length[0] += requested_length;
	}


	private static void FillDigits32(UnsignedInt number, char[] buffer, int[] length) {
		int number_length = 0;
		// We fill the digits in reverse order and exchange them afterwards.
		while (!number.isZero()) {
			int digit = number.mod10();
			number  = number.divideBy(10);
			buffer[length[0] + number_length] = (char)('0' + digit);
			number_length++;
		}
		// Exchange the digits.
		int i = length[0];
		int j = length[0] + number_length - 1;
		while (i < j) {
			char tmp = buffer[i];
			buffer[i] = buffer[j];
			buffer[j] = tmp;
			i++;
			j--;
		}
  		length[0] += number_length;
	}


	private static void FillDigits64FixedLength(UnsignedLong number,
												char[] buffer, int[] length) {
  		final long kTen7 = 10000000L;
		// For efficiency cut the number into 3 uint32_t parts, and print those.
		UnsignedInt part2 = number.modTen7();
		number = number.divideBy(kTen7);
		UnsignedInt part1 = number.modTen7();
		UnsignedInt part0 = number.divideBy(kTen7).uIntValueExact();

		FillDigits32FixedLength(part0, 3, buffer, length);
		FillDigits32FixedLength(part1, 7, buffer, length);
		FillDigits32FixedLength(part2, 7, buffer, length);
	}


	private static void FillDigits64(UnsignedLong number, char[] buffer, int[] length) {
  		final long kTen7 = 10000000L;
		// For efficiency cut the number into 3 uint32_t parts, and print those.
		UnsignedInt part2 = number.modTen7();
		number = number.divideBy(kTen7);
		UnsignedInt part1 = number.modTen7();
		UnsignedInt part0 = number.divideBy(kTen7).uIntValueExact();

		if (!part0.isZero()) {
			FillDigits32(part0, buffer, length);
			FillDigits32FixedLength(part1, 7, buffer, length);
			FillDigits32FixedLength(part2, 7, buffer, length);
		} else if (!part1.isZero()) {
			FillDigits32(part1, buffer, length);
			FillDigits32FixedLength(part2, 7, buffer, length);
		} else {
			FillDigits32(part2, buffer, length);
		}
	}


	private static void RoundUp(char[] buffer, int[] length, int[] decimal_point) {
		// An empty buffer represents 0.
		if (length[0] == 0) {
			buffer[0] = '1';
    		decimal_point[0] = 1;
    		length[0] = 1;
			return;
		}
		// Round the last digit until we either have a digit that was not '9' or until
		// we reached the first digit.
		buffer[length[0] - 1]++;
		for (int i = length[0] - 1; i > 0; --i) {
			if (buffer[i] != '0' + 10) {
				return;
			}
			buffer[i] = '0';
			buffer[i - 1]++;
		}
		// If the first digit is now '0' + 10, we would need to set it to '0' and add
		// a '1' in front. However we reach the first digit only if all following
		// digits had been '9' before rounding up. Now all trailing digits are '0' and
		// we simply switch the first digit to '1' and update the decimal-point
		// (indicating that the point is now one digit to the right).
		if (buffer[0] == '0' + 10) {
			buffer[0] = '1';
			decimal_point[0]++;
		}
	}


	// The given fractionals number represents a fixed-point number with binary
	// point at bit (-exponent).
	// Preconditions:
	//   -128 <= exponent <= 0.
	//   0 <= fractionals * 2^exponent < 1
	//   The buffer holds the result.
	// The function will round its result. During the rounding-process digits not
	// generated by this function might be updated, and the decimal-point variable
	// might be updated. If this function generates the digits 99 and the buffer
	// already contained "199" (thus yielding a buffer of "19999") then a
	// rounding-up will change the contents of the buffer to "20000".
	private static void FillFractionals(UnsignedLong fractionals, int exponent,
										int fractional_count, char[] buffer,
										int[] length, int[] decimal_point) {
		DOUBLE_CONVERSION_ASSERT(-128 <= exponent && exponent <= 0);
		// 'fractionals' is a fixed-point number, with binary point at bit
		// (-exponent). Inside the function the non-converted remainder of fractionals
		// is a fixed-point number, with binary point at bit 'point'.
		if (-exponent <= 64) {
			// One 64 bit number is sufficient.
			DOUBLE_CONVERSION_ASSERT(fractionals.shr(56).isZero());
			int point = -exponent;
			for (int i = 0; i < fractional_count; ++i) {
				if (fractionals.isZero()) break;
				// Instead of multiplying by 10 we multiply by 5 and adjust the point
				// location. This way the fractionals variable will not overflow.
				// Invariant at the beginning of the loop: fractionals < 2^point.
				// Initially we have: point <= 64 and fractionals < 2^56
				// After each iteration the point is decremented by one.
				// Note that 5^3 = 125 < 128 = 2^7.
				// Therefore three iterations of this loop will not overflow fractionals
				// (even without the subtraction at the end of the loop body). At this
				// time point will satisfy point <= 61 and therefore fractionals < 2^point
				// and any further multiplication of fractionals by 5 will not overflow.
				fractionals = fractionals.times(5);
				point--;
				int digit = fractionals.shr(point).unsafeIntValue();
				DOUBLE_CONVERSION_ASSERT(digit <= 9);
				buffer[length[0]] = (char)('0' + digit);
				length[0]++;
				fractionals = fractionals.minus(UnsignedLong.valueOf(digit).shl(point));
			}
			// If the first bit after the point is set we have to round up.
			DOUBLE_CONVERSION_ASSERT(fractionals.isZero() || point - 1 >= 0);
			if (!fractionals.isZero() && !fractionals.shr(point - 1).isEven()) {
				RoundUp(buffer, length, decimal_point);
			}
		} else {  // We need 128 bits.
			DOUBLE_CONVERSION_ASSERT(64 < -exponent && -exponent <= 128);
			UInt128 fractionals128 = new UInt128(fractionals, UnsignedLong.ZERO);
			fractionals128 = fractionals128.shift(-exponent - 64);
			int point = 128;
			for (int i = 0; i < fractional_count; ++i) {
				if (fractionals128.isZero()) break;
				// As before: instead of multiplying by 10 we multiply by 5 and adjust the
				// point location.
				// This multiplication will not overflow for the same reasons as before.
				fractionals128 = fractionals128.times(5);
				point--;
				UInt128.QuotientRemainder qr = fractionals128.divModPowerOf2(point);
				int digit = qr.quotient;
				fractionals128 = qr.remainder;
				DOUBLE_CONVERSION_ASSERT(digit <= 9);
				buffer[length[0]] = (char)('0' + digit);
				length[0]++;
			}
			if (fractionals128.bitAt(point - 1) == 1) {
				RoundUp(buffer, length, decimal_point);
			}
		}
	}


	// Removes leading and trailing zeros.
	// If leading zeros are removed then the decimal point position is adjusted.
	private static void TrimZeros(char[] buffer, int[] length, int[] decimal_point) {
		while (length[0] > 0 && buffer[length[0] - 1] == '0') {
			length[0]--;
		}
		int first_non_zero = 0;
		while (first_non_zero < length[0] && buffer[first_non_zero] == '0') {
			first_non_zero++;
		}
		if (first_non_zero != 0) {
			for (int i = first_non_zero; i < length[0]; ++i) {
				buffer[i - first_non_zero] = buffer[i];
			}
    		length[0] -= first_non_zero;
    		decimal_point[0] -= first_non_zero;
		}
	}


	// Produces digits necessary to print a given number with
	// 'fractional_count' digits after the decimal point.
	// The buffer must be big enough to hold the result plus one terminating null
	// character.
	//
	// The produced digits might be too short in which case the caller has to fill
	// the gaps with '0's.
	// Example: FastFixedDtoa(0.001, 5, ...) is allowed to return buffer = "1", and
	// decimal_point = -2.
	// Halfway cases are rounded towards +/-Infinity (away from 0). The call
	// FastFixedDtoa(0.15, 2, ...) thus returns buffer = "2", decimal_point = 0.
	// The returned buffer may contain digits that would be truncated from the
	// shortest representation of the input.
	//
	// This method only works for some parameters. If it can't handle the input it
	// returns false. The output is null-terminated when the function succeeds.
	public static boolean FastFixedDtoa(double v, int fractional_count,
										char[] buffer, int[] length, int[] decimal_point) {
  		final UnsignedLong kMaxUInt32 = UnsignedLong.uValueOf(0xFFFFFFFFL);
		UnsignedLong significand = UnsignedLong.valueOf(new Ieee.Double(v).Significand());
		int exponent = new Ieee.Double(v).Exponent();
		// v = significand * 2^exponent (with significand a 53bit integer).
		// If the exponent is larger than 20 (i.e. we may have a 73bit number) then we
		// don't know how to compute the representation. 2^73 ~= 9.5*10^21.
		// If necessary this limit could probably be increased, but we don't need
		// more.
		if (exponent > 20) return false;
		if (fractional_count > 20) return false;
  		length[0] = 0;
		// At most kDoubleSignificandSize bits of the significand are non-zero.
		// Given a 64 bit integer we have 11 0s followed by 53 potentially non-zero
		// bits:  0..11*..0xxx..53*..xx
		if (exponent + kDoubleSignificandSize > 64) {
			// The exponent must be > 11.
			//
			// We know that v = significand * 2^exponent.
			// And the exponent > 11.
			// We simplify the task by dividing v by 10^17.
			// The quotient delivers the first digits, and the remainder fits into a 64
			// bit number.
			// Dividing by 10^17 is equivalent to dividing by 5^17*2^17.
    		final long kFive17 = 0xB1_A2BC2EC5L;  // 5^17
			UnsignedLong divisor = UnsignedLong.uValueOf(kFive17);
			int divisor_power = 17;
			UnsignedLong dividend = significand;
			UnsignedInt quotient;
			UnsignedLong remainder;
			// Let v = f * 2^e with f == significand and e == exponent.
			// Then need q (quotient) and r (remainder) as follows:
			//   v            = q * 10^17       + r
			//   f * 2^e      = q * 10^17       + r
			//   f * 2^e      = q * 5^17 * 2^17 + r
			// If e > 17 then
			//   f * 2^(e-17) = q * 5^17        + r/2^17
			// else
			//   f  = q * 5^17 * 2^(17-e) + r/2^e
			if (exponent > divisor_power) {
				// We only allow exponents of up to 20 and therefore (17 - e) <= 3
				dividend = dividend.shl(exponent - divisor_power);
				quotient = dividend.divideBy(divisor).uIntValueExact();
				remainder = dividend.mod(divisor).shl(divisor_power);
			} else {
				divisor = divisor.shl(divisor_power - exponent);
				quotient = dividend.divideBy(divisor).uIntValueExact();
				remainder = dividend.mod(divisor).shl(exponent);
			}
			FillDigits32(quotient, buffer, length);
			FillDigits64FixedLength(remainder, buffer, length);
    		decimal_point[0] = length[0];
		} else if (exponent >= 0) {
			// 0 <= exponent <= 11
			significand = significand.shl(exponent);
			FillDigits64(significand, buffer, length);
    		decimal_point[0] = length[0];
		} else if (exponent > -kDoubleSignificandSize) {
			// We have to cut the number.
			UnsignedLong integrals = significand.shr(-exponent);
			UnsignedLong fractionals = significand.minus(integrals.shl(-exponent));
			if (!integrals.isUIntValue()) {
				FillDigits64(integrals, buffer, length);
			} else {
				FillDigits32(integrals.unsafeUIntValue(), buffer, length);
			}
    		decimal_point[0] = length[0];
			FillFractionals(fractionals, exponent, fractional_count,
					buffer, length, decimal_point);
		} else if (exponent < -128) {
			// This configuration (with at most 20 digits) means that all digits must be
			// 0.
			DOUBLE_CONVERSION_ASSERT(fractional_count <= 20);
			buffer[0] = '\0';
    		length[0] = 0;
    		decimal_point[0] = -fractional_count;
		} else {
    		decimal_point[0] = 0;
			FillFractionals(significand, exponent, fractional_count,
					buffer, length, decimal_point);
		}
		TrimZeros(buffer, length, decimal_point);
		buffer[length[0]] = '\0';
		if (length[0] == 0) {
			// The string is empty and the decimal_point thus has no importance. Mimick
			// Gay's dtoa and and set it to -fractional_count.
    		decimal_point[0] = -fractional_count;
		}
		return true;
	}

}
