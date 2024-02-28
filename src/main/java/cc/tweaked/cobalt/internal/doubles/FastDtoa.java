/*
 * Copyright 2012 the V8 project authors. All rights reserved.
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

import org.checkerframework.checker.signedness.qual.Unsigned;

import static cc.tweaked.cobalt.internal.doubles.Assert.requireArg;
import static cc.tweaked.cobalt.internal.doubles.UnsignedValues.*;

final class FastDtoa {
	@SuppressWarnings("ImplicitNumericConversion")
	private static final int ASCII_ZERO = '0';

	/**
	 * fastDtoa will produce at most FAST_DTOA_MAXIMAL_LENGTH digits.
	 */
	public static final int FAST_DTOA_MAXIMAL_LENGTH = 17;


	/**
	 * The minimal and maximal target exponent define the range of w's binary
	 * exponent, where 'w' is the result of multiplying the input by a cached power
	 * of ten.
	 * <p>
	 * A different range might be chosen on a different platform, to optimize digit
	 * generation, but a smaller range requires more powers of ten to be cached.
	 */
	private static final int MINIMAL_TARGET_EXPONENT = -60;
	/**
	 * The minimal and maximal target exponent define the range of w's binary
	 * exponent, where 'w' is the result of multiplying the input by a cached power
	 * of ten.
	 * <p>
	 * A different range might be chosen on a different platform, to optimize digit
	 * generation, but a smaller range requires more powers of ten to be cached.
	 */
	private static final int MAXIMAL_TARGET_EXPONENT = -32;

	/**
	 * Rounds the buffer upwards if the result is closer to v by possibly adding
	 * 1 to the buffer. If the precision of the calculation is not sufficient to
	 * round correctly, return false.
	 * The rounding might shift the whole buffer in which case the kappa is
	 * adjusted. For example "99", kappa = 3 might become "10", kappa = 4.
	 * <p>
	 * If 2*rest > tenKappa then the buffer needs to be round up.
	 * rest can have an error of +/- 1 unit. This function accounts for the
	 * imprecision and returns false, if the rounding direction cannot be
	 * unambiguously determined.
	 * <p>
	 * Precondition: rest < tenKappa.
	 */
	private static boolean roundWeedCounted(
		DecimalRepBuf buf,
		@Unsigned long rest,
		@Unsigned long tenKappa,
		@Unsigned long unit,
		int[] kappa
	) {
		assert ulongLT(rest, tenKappa);
		// The following tests are done in a specific order to avoid overflows. They
		// will work correctly with any uint64 values of rest < tenKappa and unit.
		//
		// If the unit is too big, then we don't know which way to round. For example
		// a unit of 50 means that the real number lies within rest +/- 50. If
		// 10^kappa == 40 then there is no way to tell which way to round.
		if (ulongGE(unit, tenKappa)) return false;
		// Even if unit is just half the size of 10^kappa we are already completely
		// lost. (And after the previous test we know that the expression will not
		// over/underflow.)
		if (ulongLE(tenKappa - unit, unit)) return false;
		// If 2 * (rest + unit) <= 10^kappa we can safely round down.
		if (ulongGT(tenKappa - rest, rest) && ulongGE(tenKappa - 2L * rest, 2L * unit)) {
			return true;
		}
		// If 2 * (rest - unit) >= 10^kappa, then we can safely round up.
		if (ulongGT(rest, unit) && ulongLE((tenKappa - (rest - unit)), (rest - unit))) {
			kappa[0] += buf.incrementLast();
			return true;
		}
		return false;
	}

	/**
	 * Inspired by the method for finding an integer log base 10 from here:
	 * http://graphics.stanford.edu/~seander/bithacks.html#IntegerLog10
	 */
	private static final @Unsigned int[] SMALL_POWERS_OF_TEN =
		{0, 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000,
			1000000000};

	/**
	 * Returns the biggest power of ten that is less than or equal to the given
	 * number. We furthermore receive the maximum number of bits 'number' has.
	 * <p>
	 * Returns power == 10^(exponentPlusOne-1) such that
	 * power <= number < power * 10.
	 * If numberBits == 0 then 0^(0-1) is returned.
	 * The number of bits must be <= 32.
	 * Precondition: number < (1 << (numberBits + 1)).
	 */
	static void biggestPowerTen(@Unsigned int number, int numberBits, @Unsigned int[] power, int[] exponentPlusOne) {
		requireArg(uintLT(number, 1 << (numberBits + 1)), "number must fit in numberBits");
		// 1233/4096 is approximately 1/lg(10).
		int exponentPlusOneGuess = ((numberBits + 1) * 1233 >> 12);
		// We increment to skip over the first entry in the SMALL_POWERS_OF_TEN table.
		// Note: SMALL_POWERS_OF_TEN[i] == 10^(i-1).
		exponentPlusOneGuess++;
		@Unsigned int pow = SMALL_POWERS_OF_TEN[exponentPlusOneGuess];
		// We don't have any guarantees that 2^numberBits <= number.
		if (uintLT(number, pow)) {
			exponentPlusOneGuess--;
			pow = SMALL_POWERS_OF_TEN[exponentPlusOneGuess];
		}
		power[0] = pow;
		exponentPlusOne[0] = exponentPlusOneGuess;
	}

	/**
	 * Generates (at most) requestedDigits digits of input number w.
	 * w is a floating-point number (DiyFp), consisting of a significand and an
	 * exponent. Its exponent is bounded by MAXIMAL_TARGET_EXPONENT and
	 * MAXIMAL_TARGET_EXPONENT.
	 * Hence -60 <= w.e() <= -32.
	 * <p>
	 * Returns false if it fails, in which case the generated digits in the buffer
	 * should not be used.
	 * Preconditions:
	 * * w is correct up to 1 ulp (unit in the last place). That
	 * is, its error must be strictly less than a unit of its last digit.
	 * * MINIMAL_TARGET_EXPONENT <= w.e() <= MAXIMAL_TARGET_EXPONENT
	 * <p>
	 * Postconditions: returns false if procedure fails.
	 * otherwise:
	 * * length contains the number of digits.
	 * * the representation in buffer is the most precise representation of
	 * requestedDigits digits.
	 * * buffer contains at most requestedDigits digits of w. If there are less
	 * than requestedDigits digits then some trailing '0's have been removed.
	 * * kappa is such that
	 * w = buffer * 10^kappa + eps with |eps| < 10^kappa / 2.
	 * <p>
	 * Remark: This procedure takes into account the imprecision of its input
	 * numbers. If the precision is not enough to guarantee all the postconditions
	 * then false is returned. This usually happens rarely, but the failure-rate
	 * increases with higher requestedDigits.
	 */
	private static boolean digitGenCounted(DiyFp w, int requestedDigits, DecimalRepBuf buf, int[] kappa) {
		assert MINIMAL_TARGET_EXPONENT <= w.exponent() && w.exponent() <= MAXIMAL_TARGET_EXPONENT;
		// w is assumed to have an error less than 1 unit. Whenever w is scaled we
		// also scale its error.
		@Unsigned long wError = 1L;
		// We cut the input number into two parts: the integral digits and the
		// fractional digits. We don't emit any decimal separator, but adapt kappa
		// instead. Example: instead of writing "1.2" we put "12" into the buffer and
		// increase kappa by 1.
		DiyFp one = new DiyFp(1L << -w.exponent(), w.exponent());
		// Division by one is a shift.
		@Unsigned int integrals = toUint(w.significand() >>> -one.exponent());
		// Modulo by one is an and.
		@Unsigned long fractionals = w.significand() & (one.significand() - 1L);
		@Unsigned int divisor;
		int divisorExponentPlusOne;
		{
			int[] inDivisorExponentPlusOne = new int[1];
			@Unsigned int[] inDivisor = new int[1];
			biggestPowerTen(integrals, DiyFp.SIGNIFICAND_SIZE - (-one.exponent()),
				inDivisor, inDivisorExponentPlusOne);
			divisor = inDivisor[0];
			divisorExponentPlusOne = inDivisorExponentPlusOne[0];
		}

		kappa[0] = divisorExponentPlusOne;
		buf.clearBuf();

		// Loop invariant: buffer = w / 10^kappa  (integer division)
		// The invariant holds for the first iteration: kappa has been initialized
		// with the divisor exponent + 1. And the divisor is the biggest power of ten
		// that is smaller than 'integrals'.
		while (kappa[0] > 0) {
			buf.append(uDivide(integrals, divisor));
			requestedDigits--;
			integrals = uRemainder(integrals, divisor);
			kappa[0]--;
			// Note that kappa now equals the exponent of the divisor and that the
			// invariant thus holds again.
			if (requestedDigits == 0) break;
			divisor = uDivide(divisor, 10);
		}

		if (requestedDigits == 0) {
			@Unsigned long rest = (toUlong(integrals) << -one.exponent()) + fractionals;
			return roundWeedCounted(buf, rest,
				toUlong(divisor) << -one.exponent(), wError,
				kappa);
		}

		// The integrals have been generated. We are at the point of the decimal
		// separator. In the following loop we simply multiply the remaining digits by
		// 10 and divide by one. We just need to pay attention to multiply associated
		// data (the 'unit'), too.
		// Note that the multiplication by 10 does not overflow, because w.e >= -60
		// and thus one.e >= -60.
		assert one.exponent() >= -60;
		assert ulongLT(fractionals, one.significand());
		assert ulongGE(uDivide(0xFFFF_FFFF_FFFF_FFFFL, 10L), one.significand());
		while (requestedDigits > 0 && ulongGT(fractionals, wError)) {
			fractionals *= 10L;
			wError *= 10L;
			// Integer division by one.
			buf.append(fractionals >>> -one.exponent());
			requestedDigits--;
			fractionals &= one.significand() - 1L;  // Modulo by one.
			kappa[0]--;
		}
		if (requestedDigits != 0) return false;
		return roundWeedCounted(buf, fractionals, one.significand(), wError,
			kappa);
	}


	/**
	 * The "counted" version of grisu3 (see above) only generates requestedDigits
	 * number of digits. This version does not generate the shortest representation,
	 * and with enough requested digits 0.1 will at some point print as 0.9999999...
	 * grisu3 is too imprecise for real halfway cases (1.5 will not work) and
	 * therefore the rounding strategy for halfway cases is irrelevant.
	 */
	private static boolean grisu3Counted(double v, int requestedDigits, DecimalRepBuf buf, int[] decimalExponent) {
		DiyFp w = Doubles.asNormalizedDiyFp(v);
		DiyFp ten_mk;  // Cached power of ten: 10^-k
		int mk;        // -k
		{
			DiyFp[] inTenMk = new DiyFp[1];
			int[] inMk = new int[1];
			int tenMkMinimalBinaryExponent =
				MINIMAL_TARGET_EXPONENT - (w.exponent() + DiyFp.SIGNIFICAND_SIZE);
			int tenMkMaximalBinaryExponent =
				MAXIMAL_TARGET_EXPONENT - (w.exponent() + DiyFp.SIGNIFICAND_SIZE);
			PowersOfTenCache.getCachedPowerForBinaryExponentRange(
				tenMkMinimalBinaryExponent,
				tenMkMaximalBinaryExponent,
				inTenMk, inMk);
			ten_mk = inTenMk[0];
			mk = inMk[0];
		}
		assert MINIMAL_TARGET_EXPONENT <= w.exponent() + ten_mk.exponent() + DiyFp.SIGNIFICAND_SIZE;
		assert MAXIMAL_TARGET_EXPONENT >= w.exponent() + ten_mk.exponent() + DiyFp.SIGNIFICAND_SIZE;
		// Note that ten_mk is only an approximation of 10^-k. A DiyFp only contains a
		// 64 bit significand and ten_mk is thus only precise up to 64 bits.

		// The DiyFp.times procedure rounds its result, and ten_mk is approximated
		// too. The variable scaledW (as well as scaled_boundary_minus/plus) are now
		// off by a small amount.
		// In fact: scaledW - w*10^k < 1ulp (unit in the last place) of scaledW.
		// In other words: let f = scaledW.f() and e = scaledW.e(), then
		//           (f-1) * 2^e < w*10^k < (f+1) * 2^e
		DiyFp scaledW = DiyFp.times(w, ten_mk);

		// We now have (double) (scaledW * 10^-mk).
		// digitGen will generate the first requestedDigits digits of scaledW and
		// return together with a kappa such that scaledW ~= buffer * 10^kappa. (It
		// will not always be exactly the same since digitGenCounted only produces a
		// limited number of digits.)
		int[] inKappa = new int[1];
		boolean result = digitGenCounted(scaledW, requestedDigits,
			buf, inKappa);
		decimalExponent[0] = -mk + inKappa[0];
		return result;
	}

	/**
	 * Provides a decimal representation of v.
	 * The result should be interpreted as buffer * 10^(point - outLength).
	 * <p>
	 * Precondition:
	 * * v must be a strictly positive finite double.
	 * <p>
	 * Returns true if it succeeds, otherwise the result can not be trusted.
	 * If the function returns true and mode equals
	 * - FAST_DTOA_PRECISION, then
	 * the buffer contains requestedDigits digits.
	 * the difference v - (buffer * 10^(point-outLength)) is closest to zero for
	 * all possible representations of requestedDigits digits.
	 * If there are two values that are equally close, then fastDtoa returns
	 * false.
	 * For both modes the buffer must be large enough to hold the result.
	 */
	public static boolean fastDtoa(double v, int requestedDigits, DecimalRepBuf buf) {
		assert v > 0.0;
		assert !Doubles.isSpecial(v);

		boolean result;
		int[] decimalExponent = new int[1]; // initialized to 0
		result = grisu3Counted(v, requestedDigits, buf, decimalExponent);
		if (result) {
			buf.setPointPosition(buf.length() + decimalExponent[0]);
		} else {
			buf.reset();
		}
		return result;
	}

}
