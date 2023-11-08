/*
 * Copyright 2010 the V8 project authors. All rights reserved.
 * Java Port Copyright 2021 sir-maniac. All Rights reserved.
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

import static cc.tweaked.cobalt.internal.doubles.UnsignedValues.*;

final class FixedDtoa {
	private static final int DOUBLE_SIGNIFICAND_SIZE = Doubles.SIGNIFICAND_SIZE;  // Includes the hidden bit.
	private static final @Unsigned long TEN_POW_OF_7 = 10000000L;

	// Represents a 128bit type. This class should be replaced by a native type on
	// platforms that support 128bit integers.
	// Protected for testins
	static class UInt128 {
		private static final @Unsigned long MASK_32 = 0xFFFFFFFFL;
		// Value == (high_bits_ << 64) + low_bits_
		private final @Unsigned long high;
		private final @Unsigned long low;

		public UInt128() {
			this.high = 0L;
			this.low = 0L;
		}

		public UInt128(@Unsigned long high, @Unsigned long low) {
			this.high = high;
			this.low = low;
		}

		// package for testing
		@Unsigned long rawHigh() {
			return high;
		}

		// package for testing
		@Unsigned long rawLow() {
			return low;
		}

		public UInt128 times(@Unsigned long multiplicand) {
			long accumulator;

			accumulator = (low & MASK_32) * multiplicand;
			long part = accumulator & MASK_32;
			accumulator >>>= 32L;
			accumulator = accumulator + (low >>> 32) * multiplicand;
			long newLowBits = (accumulator << 32) + part;
			accumulator >>>= 32L;
			accumulator = accumulator + (high & MASK_32) * multiplicand;
			part = accumulator & MASK_32;
			accumulator >>>= 32L;
			accumulator = accumulator + (high >>> 32) * multiplicand;
			long newHighBits = (accumulator << 32) + part;
			assert (accumulator >>> 32) == 0L;

			return new UInt128(newHighBits, newLowBits);
		}

		public UInt128 shift(int shift_amount) {
			assert -64 <= shift_amount && shift_amount <= 64;
			long nHigh, nLow;
			if (shift_amount == 0) {
				return this;
			} else if (shift_amount == -64) {
				nHigh = low;
				nLow = 0L;
			} else if (shift_amount == 64) {
				nHigh = 0L;
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
			@Unsigned int quotient;
			if (power >= 64) {
				quotient = toUint(high >>> (power - 64));
				remHigh = high - (((long) quotient) << (power - 64));
				remLow = low;
			} else {
				long partLow = low >>> power;
				long partHigh = high << (64 - power);
				quotient = (int) (partLow + partHigh);
				remHigh = 0L;
				remLow = low - (partLow << power);
			}
			return new QuotientRemainder(quotient, new UInt128(remHigh, remLow));
		}

		public boolean isZero() {
			return high == 0L && low == 0L;
		}

		public int bitAt(int position) {
			if (position >= 64) {
				return (int) (high >>> (position - 64)) & 1;
			} else {
				return (int) (low >>> position) & 1;
			}
		}

		public static class QuotientRemainder {
			public final @Unsigned int quotient;
			public final UInt128 remainder;

			public QuotientRemainder(@Unsigned int quotient, UInt128 remainder) {
				this.quotient = quotient;
				this.remainder = remainder;
			}
		}
	}

	private static void fillDigits32FixedLength(@Unsigned int number, int requestedLength, DecimalRepBuf buf) {
		int start = buf.length();
		buf.addLength(requestedLength);
		for (int i = buf.length() - 1; i >= start; i--) {
			buf.setCharAt(i, uRemainder(number, 10));
			number = uDivide(number, 10);
		}
	}


	private static void fillDigits32(@Unsigned int number, DecimalRepBuf buf) {
		int start = buf.length();
		// We fill the digits in reverse order and exchange them afterwards.
		while (number != 0) {
			@Unsigned int digit = uRemainder(number, 10);
			number = uDivide(number, 10);
			buf.append(digit);
		}
		buf.reverseLast(start);
	}


	private static void fillDigits64FixedLength(@Unsigned long number, DecimalRepBuf buf) {
		// For efficiency cut the number into 3 uint32_t parts, and print those.
		@Unsigned int part2 = toUint(uRemainder(number, TEN_POW_OF_7));
		number = uDivide(number, TEN_POW_OF_7);
		@Unsigned int part1 = toUint(uRemainder(number, TEN_POW_OF_7));
		@Unsigned int part0 = toUint(uDivide(number, TEN_POW_OF_7));

		fillDigits32FixedLength(part0, 3, buf);
		fillDigits32FixedLength(part1, 7, buf);
		fillDigits32FixedLength(part2, 7, buf);
	}


	private static void fillDigits64(@Unsigned long number, DecimalRepBuf buf) {
		// For efficiency cut the number into 3 uint32_t parts, and print those.
		@Unsigned int part2 = toUint(uRemainder(number, TEN_POW_OF_7));
		number = uDivide(number, TEN_POW_OF_7);
		@Unsigned int part1 = toUint(uRemainder(number, TEN_POW_OF_7));
		@Unsigned int part0 = toUint(uDivide(number, TEN_POW_OF_7));

		if (part0 != 0) {
			fillDigits32(part0, buf);
			fillDigits32FixedLength(part1, 7, buf);
			fillDigits32FixedLength(part2, 7, buf);
		} else if (part1 != 0) {
			fillDigits32(part1, buf);
			fillDigits32FixedLength(part2, 7, buf);
		} else {
			fillDigits32(part2, buf);
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
	private static void fillFractionals(@Unsigned long fractionals, int exponent, int fractionalCount, DecimalRepBuf buf) {
		assert -128 <= exponent && exponent <= 0;
		// 'fractionals' is a fixed-point number, with binary point at bit
		// (-exponent). Inside the function the non-converted remainder of fractionals
		// is a fixed-point number, with binary point at bit 'point'.
		if (-exponent <= 64) {
			// One 64 bit number is sufficient.
			assert fractionals >>> 56 == 0L;
			int point = -exponent;
			for (int i = 0; i < fractionalCount; ++i) {
				if (fractionals == 0L) break;
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
				fractionals *= 5L;
				point--;
				@Unsigned int digit = toUint(fractionals >>> point);
				buf.append(digit);
				fractionals = fractionals - (toUlong(digit) << point);
			}
			// If the first bit after the point is set we have to round up.
			assert fractionals == 0L || point - 1 >= 0;
			if (fractionals != 0L && ((fractionals >>> (point - 1)) & 1L) == 1L) {
				buf.roundUp();
			}
		} else {  // We need 128 bits.
			assert 64 < -exponent && -exponent <= 128;
			UInt128 fractionals128 = new UInt128(fractionals, 0L);
			fractionals128 = fractionals128.shift(-exponent - 64);
			int point = 128;
			for (int i = 0; i < fractionalCount; ++i) {
				if (fractionals128.isZero()) break;
				// As before: instead of multiplying by 10 we multiply by 5 and adjust the
				// point location.
				// This multiplication will not overflow for the same reasons as before.
				fractionals128 = fractionals128.times(5L);
				point--;
				UInt128.QuotientRemainder qr = fractionals128.divModPowerOf2(point);
				@Unsigned int digit = qr.quotient;
				fractionals128 = qr.remainder;
				buf.append(digit);
			}
			if (fractionals128.bitAt(point - 1) == 1) {
				buf.roundUp();
			}
		}
	}


	// Produces digits necessary to print a given number with
	// 'fractionalCount' digits after the decimal point.
	// The buffer must be big enough to hold the result.
	//
	// The produced digits might be too short in which case the caller has to fill
	// the gaps with '0's.
	// Example: fastFixedDtoa(0.001, 5, ...) is allowed to return buffer = "1", and
	// decimalPoint = -2.
	// Halfway cases are rounded towards +/-Infinity (away from 0). The call
	// fastFixedDtoa(0.15, 2, ...) thus returns buffer = "2", decimalPoint = 0.
	// The returned buffer may contain digits that would be truncated from the
	// shortest representation of the input.
	//
	// This method only works for some parameters. If it can't handle the input it
	// returns false.
	public static boolean fastFixedDtoa(double v, int fractionalCount, DecimalRepBuf buf) {
		final @Unsigned long kMaxUInt32 = 0xFFFF_FFFFL;
		@Unsigned long significand = Doubles.significand(v);
		int exponent = Doubles.exponent(v);
		// v = significand * 2^exponent (with significand a 53bit integer).
		// If the exponent is larger than 20 (i.e. we may have a 73bit number) then we
		// don't know how to compute the representation. 2^73 ~= 9.5*10^21.
		// If necessary this limit could probably be increased, but we don't need
		// more.
		if (exponent > 20) return false;
		if (fractionalCount > 20) return false;

		buf.clearBuf();
		// At most DOUBLE_SIGNIFICAND_SIZE bits of the significand are non-zero.
		// Given a 64 bit integer we have 11 0s followed by 53 potentially non-zero
		// bits:  0..11*..0xxx..53*..xx
		if (exponent + DOUBLE_SIGNIFICAND_SIZE > 64) {
			// compile-time promise that exponent is positive
			@SuppressWarnings("cast.unsafe")
			@SignedPositive int positiveExponent = (@SignedPositive int) exponent;
			// The exponent must be > 11.
			//
			// We know that v = significand * 2^exponent.
			// And the exponent > 11.
			// We simplify the task by dividing v by 10^17.
			// The quotient delivers the first digits, and the remainder fits into a 64
			// bit number.
			// Dividing by 10^17 is equivalent to dividing by 5^17*2^17.
			final @Unsigned long kFive17 = 0xB1_A2BC2EC5L;  // 5^17
			@Unsigned long divisor = kFive17;
			@SignedPositive int divisorPower = 17;
			@Unsigned long dividend = significand;
			@Unsigned int quotient;
			@Unsigned long remainder;
			// Let v = f * 2^e with f == significand and e == exponent.
			// Then need q (quotient) and r (remainder) as follows:
			//   v            = q * 10^17       + r
			//   f * 2^e      = q * 10^17       + r
			//   f * 2^e      = q * 5^17 * 2^17 + r
			// If e > 17 then
			//   f * 2^(e-17) = q * 5^17        + r/2^17
			// else
			//   f  = q * 5^17 * 2^(17-e) + r/2^e
			if (exponent > divisorPower) {
				// We only allow exponents of up to 20 and therefore (17 - e) <= 3
				dividend <<= toUlongFromSigned(positiveExponent - divisorPower);
				quotient = toUint(uDivide(dividend, divisor));
				remainder = uRemainder(dividend, divisor) << divisorPower;
			} else {
				divisor <<= toUlongFromSigned(divisorPower - positiveExponent);
				quotient = toUint(uDivide(dividend, divisor));
				remainder = uRemainder(dividend, divisor) << exponent;
			}
			fillDigits32(quotient, buf);
			fillDigits64FixedLength(remainder, buf);
			buf.setPointPosition(buf.length());
		} else if (exponent >= 0) {
			// 0 <= exponent <= 11
			significand = significand << exponent;
			fillDigits64(significand, buf);
			buf.setPointPosition(buf.length());
		} else if (exponent > -DOUBLE_SIGNIFICAND_SIZE) {
			// We have to cut the number.
			@Unsigned long integrals = significand >>> -exponent;
			@Unsigned long fractionals = significand - (integrals << -exponent);
			if (!isAssignableToUint(integrals)) {
				fillDigits64(integrals, buf);
			} else {
				fillDigits32(toUint(integrals), buf);
			}
			buf.setPointPosition(buf.length());
			fillFractionals(fractionals, exponent, fractionalCount, buf);
		} else if (exponent < -128) {
			// This configuration (with at most 20 digits) means that all digits must be
			// 0.
			assert fractionalCount <= 20;
			buf.clearBuf();
			buf.setPointPosition(-fractionalCount);
		} else {
			buf.setPointPosition(0);
			fillFractionals(significand, exponent, fractionalCount, buf);
		}
		buf.trimZeros();
		if (buf.length() == 0) {
			// The string is empty and the decimalPoint thus has no importance. Mimick
			// Gay's dtoa and and set it to -fractionalCount.
			buf.setPointPosition(-fractionalCount);
		}
		return true;
	}

}
