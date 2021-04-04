/*
 * Copyright 2012 the V8 project authors. All rights reserved.
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
package org.squiddev.cobalt.lib.doubles;

import org.checkerframework.checker.signedness.qual.Unsigned;

import static org.squiddev.cobalt.lib.doubles.Assert.DOUBLE_CONVERSION_ASSERT;
import static org.squiddev.cobalt.lib.doubles.UnsignedValues.*;

public class FastDtoa {
	@SuppressWarnings("ImplicitNumericConversion")
	private static final int ASCII_ZERO = '0';

	public enum  FastDtoaMode {
		/**
		 * 	 Computes the shortest representation of the given input. The returned
		 * 	 result will be the most accurate number of this length. Longer
		 * 	 representations might be more accurate.
		 */
		SHORTEST,
		/** Same as FAST_DTOA_SHORTEST but for single-precision floats. */
		SHORTEST_SINGLE,
		/**
		 * 	 Computes a representation where the precision (number of digits) is
		 * 	 given as input. The precision is independent of the decimal point.
		 */
		PRECISION
	}

	/**
	 *   fastDtoa will produce at most kFastDtoaMaximalLength digits. This does not
	 * 	 include the terminating '\0' character.
	 */
	public static final int FAST_DTOA_MAXIMAL_LENGTH = 17;
	/** Same for single-precision numbers. */
	public static final int FAST_DTOA_MAXIMAL_SINGLE_LENGTH = 9;



	/**
	 *  The minimal and maximal target exponent define the range of w's binary
	 *  exponent, where 'w' is the result of multiplying the input by a cached power
	 *  of ten.
	 *
	 *  A different range might be chosen on a different platform, to optimize digit
	 *  generation, but a smaller range requires more powers of ten to be cached.
	 */
	private static final int MINIMAL_TARGET_EXPONENT = -60;
	/**
	 *  The minimal and maximal target exponent define the range of w's binary
	 *  exponent, where 'w' is the result of multiplying the input by a cached power
	 *  of ten.
	 *
	 *  A different range might be chosen on a different platform, to optimize digit
	 *  generation, but a smaller range requires more powers of ten to be cached.
	 */
	private static final int MAXIMAL_TARGET_EXPONENT = -32;

	/**
	 *  Adjusts the last digit of the generated number, and screens out generated
	 *  solutions that may be inaccurate. A solution may be inaccurate if it is
	 *  outside the safe interval, or if we cannot prove that it is closer to the
	 *  input than a neighboring representation of the same length.
	 *
	 *  Input: * buffer containing the digits of too_high / 10^kappa
	 * 		* the buffer's length
	 * 		* distanceTooHighW == (too_high - w).f() * unit
	 * 		* unsafeInterval == (too_high - too_low).f() * unit
	 * 		* rest = (too_high - buffer * 10^kappa).f() * unit
	 * 		* tenKappa = 10^kappa * unit
	 * 		* unit = the common multiplier
	 *  Output: returns true if the buffer is guaranteed to contain the closest
	 * 	representable number to the input.
	 *   Modifies the generated digits in the buffer to approach (round towards) w.
	 */
	private static boolean roundWeed(char[] buffer,
						  int length,
						 @Unsigned long distanceTooHighW,
						 @Unsigned long unsafeInterval,
						 @Unsigned long rest,
						 @Unsigned long tenKappa,
						 @Unsigned long unit) {
		@Unsigned long smallDistance = distanceTooHighW - unit;
		@Unsigned long bigDistance = distanceTooHighW + unit;
		// Let w_low  = too_high - bigDistance, and
		//     w_high = too_high - smallDistance.
		// Note: w_low < w < w_high
		//
		// The real w (* unit) must lie somewhere inside the interval
		// ]w_low; w_high[ (often written as "(w_low; w_high)")

		// Basically the buffer currently contains a number in the unsafe interval
		// ]too_low; too_high[ with too_low < w < too_high
		//
		//  too_high - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
		//                     ^v 1 unit            ^      ^                 ^      ^
		//  boundary_high ---------------------     .      .                 .      .
		//                     ^v 1 unit            .      .                 .      .
		//   - - - - - - - - - - - - - - - - - - -  +  - - + - - - - - -     .      .
		//                                          .      .         ^       .      .
		//                                          .  bigDistance  .       .      .
		//                                          .      .         .       .    rest
		//                              smallDistance     .         .       .      .
		//                                          v      .         .       .      .
		//  w_high - - - - - - - - - - - - - - - - - -     .         .       .      .
		//                     ^v 1 unit                   .         .       .      .
		//  w ----------------------------------------     .         .       .      .
		//                     ^v 1 unit                   v         .       .      .
		//  w_low  - - - - - - - - - - - - - - - - - - - - -         .       .      .
		//                                                           .       .      v
		//  buffer --------------------------------------------------+-------+--------
		//                                                           .       .
		//                                                  safe_interval    .
		//                                                           v       .
		//   - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -     .
		//                     ^v 1 unit                                     .
		//  boundary_low -------------------------                     unsafeInterval
		//                     ^v 1 unit                                     v
		//  too_low  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
		//
		//
		// Note that the value of buffer could lie anywhere inside the range too_low
		// to too_high.
		//
		// boundary_low, boundary_high and w are approximations of the real boundaries
		// and v (the input number). They are guaranteed to be precise up to one unit.
		// In fact the error is guaranteed to be strictly less than one unit.
		//
		// Anything that lies outside the unsafe interval is guaranteed not to round
		// to v when read again.
		// Anything that lies inside the safe interval is guaranteed to round to v
		// when read again.
		// If the number inside the buffer lies inside the unsafe interval but not
		// inside the safe interval then we simply do not know and bail out (returning
		// false).
		//
		// Similarly we have to take into account the imprecision of 'w' when finding
		// the closest representation of 'w'. If we have two potential
		// representations, and one is closer to both w_low and w_high, then we know
		// it is closer to the actual value v.
		//
		// By generating the digits of too_high we got the largest (closest to
		// too_high) buffer that is still in the unsafe interval. In the case where
		// w_high < buffer < too_high we try to decrement the buffer.
		// This way the buffer approaches (rounds towards) w.
		// There are 3 conditions that stop the decrementation process:
		//   1) the buffer is already below w_high
		//   2) decrementing the buffer would make it leave the unsafe interval
		//   3) decrementing the buffer would yield a number below w_high and farther
		//      away than the current number. In other words:
		//              (buffer{-1} < w_high) && w_high - buffer{-1} > buffer - w_high
		// Instead of using the buffer directly we use its distance to too_high.
		// Conceptually rest ~= too_high - buffer
		// We need to do the following tests in this order to avoid over- and
		// underflows.
		DOUBLE_CONVERSION_ASSERT(ulongLE(rest, unsafeInterval));
		while (ulongLT(rest, smallDistance) &&  // Negated condition 1
				ulongGE(unsafeInterval - rest, tenKappa) &&  // Negated condition 2
				( ulongLT( rest + tenKappa, smallDistance) ||  // buffer{-1} > w_high
						ulongGE(smallDistance - rest, rest + tenKappa - smallDistance))) {
			buffer[length - 1]--;
			rest += tenKappa;
		}

		// We have approached w+ as much as possible. We now test if approaching w-
		// would require changing the buffer. If yes, then we have two possible
		// representations close to w, but we cannot decide which one is closer.
		if (ulongLT(rest, bigDistance) &&
				ulongGE(unsafeInterval - rest, tenKappa) &&
				( ulongLT(rest + tenKappa, bigDistance) ||
						ulongGT(bigDistance - rest, rest + tenKappa - bigDistance))) {
			return false;
		}

		// Weeding test.
		//   The safe interval is [too_low + 2 ulp; too_high - 2 ulp]
		//   Since too_low = too_high - unsafeInterval this is equivalent to
		//      [too_high - unsafeInterval + 4 ulp; too_high - 2 ulp]
		//   Conceptually we have: rest ~= too_high - buffer
		return ulongLE(2L * unit, rest) && ulongLE(rest, unsafeInterval - 4L * unit);
	}


	/**
	 *  Rounds the buffer upwards if the result is closer to v by possibly adding
	 *  1 to the buffer. If the precision of the calculation is not sufficient to
	 *  round correctly, return false.
	 *  The rounding might shift the whole buffer in which case the kappa is
	 *  adjusted. For example "99", kappa = 3 might become "10", kappa = 4.
	 *
	 *  If 2*rest > tenKappa then the buffer needs to be round up.
	 *  rest can have an error of +/- 1 unit. This function accounts for the
	 *  imprecision and returns false, if the rounding direction cannot be
	 *  unambiguously determined.
	 *
	 *  Precondition: rest < tenKappa.
	 */
	private static boolean roundWeedCounted(char[] buffer,
								 int length,
								 @Unsigned long rest,
								 @Unsigned long tenKappa,
								 @Unsigned long unit,
								 int[] kappa) {
		DOUBLE_CONVERSION_ASSERT(ulongLT(rest, tenKappa));
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
		if ( ulongGT(rest, unit) && ulongLE((tenKappa - (rest - unit)), (rest - unit)) ) {
			// Increment the last digit recursively until we find a non '9' digit.
			buffer[length - 1]++;
			for (int i = length - 1; i > 0; --i) {
				if ((int) buffer[i] != ASCII_ZERO + 10) break;
				buffer[i] = (char) ASCII_ZERO;
				buffer[i - 1]++;
			}
			// If the first digit is now '0'+ 10 we had a buffer with all '9's. With the
			// If the first digit is now '0'+ 10 we had a buffer with all '9's. With the
			// exception of the first digit all digits are now '0'. Simply switch the
			// first digit to '1' and adjust the kappa. Example: "99" becomes "10" and
			// the power (the kappa) is increased.
			if ((int) buffer[0] == ASCII_ZERO + 10) {
				buffer[0] = '1';
				kappa[0] += 1;
			}
			return true;
		}
		return false;
	}

	/**
	 *  Inspired by the method for finding an integer log base 10 from here:
	 *  http://graphics.stanford.edu/~seander/bithacks.html#IntegerLog10
	 */
	private static final @Unsigned int[] SMALL_POWERS_OF_TEN =
		{0, 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000,
			1000000000};

	/**
	 *  Returns the biggest power of ten that is less than or equal to the given
	 *  number. We furthermore receive the maximum number of bits 'number' has.
	 *
	 *  Returns power == 10^(exponentPlusOne-1) such that
	 * 	power <= number < power * 10.
	 *  If numberBits == 0 then 0^(0-1) is returned.
	 *  The number of bits must be <= 32.
	 *  Precondition: number < (1 << (numberBits + 1)).
	 */
	static void biggestPowerTen(@Unsigned int number,
								int numberBits,
								@Unsigned int[] power,
								int[] exponentPlusOne) {
		if (uintGE(number, 1 << (numberBits + 1)))
			throw new IllegalArgumentException("number must fit in numberBits");
		// 1233/4096 is approximately 1/lg(10).
		int exponentPlusOneGuess = ((numberBits + 1) * 1233 >> 12);
		// We increment to skip over the first entry in the kPowersOf10 table.
		// Note: kPowersOf10[i] == 10^(i-1).
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
	 *  Generates the digits of input number w.
	 *  w is a floating-point number (DiyFp), consisting of a significand and an
	 *  exponent. Its exponent is bounded by kMinimalTargetExponent and
	 *  kMaximalTargetExponent.
	 *        Hence -60 <= w.e() <= -32.
	 *
	 *  Returns false if it fails, in which case the generated digits in the buffer
	 *  should not be used.
	 *  Preconditions:
	 *   * low, w and high are correct up to 1 ulp (unit in the last place). That
	 *     is, their error must be less than a unit of their last digits.
	 *   * low.e() == w.e() == high.e()
	 *   * low < w < high, and taking into account their error: low~ <= high~
	 *   * kMinimalTargetExponent <= w.e() <= kMaximalTargetExponent
	 *  Postconditions: returns false if procedure fails.
	 *    otherwise:
	 *      * buffer is not null-terminated, but len contains the number of digits.
	 *      * buffer contains the shortest possible decimal digit-sequence
	 *        such that LOW < buffer * 10^kappa < HIGH, where LOW and HIGH are the
	 *        correct values of low and high (without their error).
	 *      * if more than one decimal representation gives the minimal number of
	 *        decimal digits then the one closest to W (where W is the correct value
	 *        of w) is chosen.
	 *  Remark: this procedure takes into account the imprecision of its input
	 *    numbers. If the precision is not enough to guarantee all the postconditions
	 *    then false is returned. This usually happens rarely (~0.5%).
	 *
	 *  Say, for the sake of example, that
	 *    w.e() == -48, and w.f() == 0x1234567890abcdef
	 *  w's value can be computed by w.f() * 2^w.e()
	 *  We can obtain w's integral digits by simply shifting w.f() by -w.e().
	 *   -> w's integral part is 0x1234
	 *   w's fractional part is therefore 0x567890abcdef.
	 *  Printing w's integral part is easy (simply print 0x1234 in decimal).
	 *  In order to print its fraction we repeatedly multiply the fraction by 10 and
	 *  get each digit. Example the first digit after the point would be computed by
	 *    (0x567890abcdef * 10) >> 48. -> 3
	 *  The whole thing becomes slightly more complicated because we want to stop
	 *  once we have enough digits. That is, once the digits inside the buffer
	 *  represent 'w' we can stop. Everything inside the interval low - high
	 *  represents w. However we have to pay attention to low, high and w's
	 *  imprecision.
	 */
	private static boolean digitGen(DiyFp low,
						 DiyFp w,
						 DiyFp high,
						 char[] buffer,
						 int[] length,
						 int[] kappa) {
		DOUBLE_CONVERSION_ASSERT(low.e() == w.e() && w.e() == high.e() );
		DOUBLE_CONVERSION_ASSERT( ulongLE(low.f() + 1L, high.f() - 1L) );
		DOUBLE_CONVERSION_ASSERT(MINIMAL_TARGET_EXPONENT <= w.e() && w.e() <= MAXIMAL_TARGET_EXPONENT);
		// low, w and high are imprecise, but by less than one ulp (unit in the last
		// place).
		// If we remove (resp. add) 1 ulp from low (resp. high) we are certain that
		// the new numbers are outside of the interval we want the final
		// representation to lie in.
		// Inversely adding (resp. removing) 1 ulp from low (resp. high) would yield
		// numbers that are certain to lie in the interval. We will use this fact
		// later on.
		// We will now start by generating the digits within the uncertain
		// interval. Later we will weed out representations that lie outside the safe
		// interval and thus _might_ lie outside the correct interval.
		@Unsigned long unit = 1L;
		DiyFp tooLow = new DiyFp(low.f() - unit, low.e());
		DiyFp tooHigh = new DiyFp(high.f() + unit, high.e());
		// tooLow and tooHigh are guaranteed to lie outside the interval we want the
		// generated number in.
		DiyFp unsafeInterval = DiyFp.minus(tooHigh, tooLow);
		// We now cut the input number into two parts: the integral digits and the
		// fractionals. We will not write any decimal separator though, but adapt
		// kappa instead.
		// Reminder: we are currently computing the digits (stored inside the buffer)
		// such that:   tooLow < buffer * 10^kappa < tooHigh
		// We use tooHigh for the digit_generation and stop as soon as possible.
		// If we stop early we effectively round down.
		DiyFp one = new DiyFp(1L << -w.e(), w.e());
		// Division by one is a shift.
		@Unsigned int integrals = toUint(tooHigh.f() >>> -one.e());
		// Modulo by one is an and.
		@Unsigned long fractionals = tooHigh.f() & (one.f() - 1L);
		@Unsigned int divisor;
		int divisorExponentPlusOne;
		{
			int[] inDivisorExponentPlusOne = new int[1];
			@Unsigned int[] uiInDivisor = new int[1];
			biggestPowerTen(integrals, DiyFp.SIGNIFICAND_SIZE - (-one.e()),
					uiInDivisor, inDivisorExponentPlusOne);
			divisor = uiInDivisor[0];
			divisorExponentPlusOne = inDivisorExponentPlusOne[0];
		}
		kappa[0] = divisorExponentPlusOne;
		length[0] = 0;
		// Loop invariant: buffer = tooHigh / 10^kappa  (integer division)
		// The invariant holds for the first iteration: kappa has been initialized
		// with the divisor exponent + 1. And the divisor is the biggest power of ten
		// that is smaller than integrals.
		while (kappa[0] > 0) {
			buffer[length[0]] = digitToChar(uDivide(integrals, divisor));
			length[0]++;
			integrals = uRemainder(integrals, divisor);
			kappa[0]--;
			// Note that kappa now equals the exponent of the divisor and that the
			// invariant thus holds again.
			@Unsigned long rest = (toUlong(integrals) << -one.e()) + fractionals;
			// Invariant: tooHigh = buffer * 10^kappa + DiyFp(rest, one.e())
			// Reminder: unsafeInterval.e() == one.e()
			if (ulongLT(rest, unsafeInterval.f())) {
				// Rounding down (by not emitting the remaining digits) yields a number
				// that lies within the unsafe interval.
				return roundWeed(buffer, length[0], DiyFp.minus(tooHigh, w).f(),
						unsafeInterval.f(), rest,
						toUlong(divisor) << -one.e(), unit);
			}
			divisor = uDivide(divisor, 10);
		}

		// The integrals have been generated. We are at the point of the decimal
		// separator. In the following loop we simply multiply the remaining digits by
		// 10 and divide by one. We just need to pay attention to multiply associated
		// data (like the interval or 'unit'), too.
		// Note that the multiplication by 10 does not overflow, because w.e >= -60
		// and thus one.e >= -60.
		DOUBLE_CONVERSION_ASSERT(one.e() >= -60);
		DOUBLE_CONVERSION_ASSERT(ulongLT(fractionals, one.f()));
		DOUBLE_CONVERSION_ASSERT( ulongGT( uDivide(0xFFFFFFFFFFFFFFFFL, 10L), one.f())  );
		for (;;) {
			fractionals = fractionals * 10L;
			unit *= 10L;
			unsafeInterval.setF(unsafeInterval.f() * 10L);
			// Integer division by one.
			buffer[length[0]] = digitToChar(fractionals >>> -one.e());
			length[0]++;
			fractionals &= one.f() - 1L;  // Modulo by one.
			kappa[0]--;
			if (ulongLT(fractionals, unsafeInterval.f())) {
				return roundWeed(buffer, length[0], DiyFp.minus(tooHigh, w).f() * unit,
						unsafeInterval.f(), fractionals, one.f(), unit);
			}
		}
	}

	/**
	 *  Generates (at most) requestedDigits digits of input number w.
	 *  w is a floating-point number (DiyFp), consisting of a significand and an
	 *  exponent. Its exponent is bounded by kMinimalTargetExponent and
	 *  kMaximalTargetExponent.
	 *        Hence -60 <= w.e() <= -32.
	 *
	 *  Returns false if it fails, in which case the generated digits in the buffer
	 *  should not be used.
	 *  Preconditions:
	 *   * w is correct up to 1 ulp (unit in the last place). That
	 *     is, its error must be strictly less than a unit of its last digit.
	 *   * kMinimalTargetExponent <= w.e() <= kMaximalTargetExponent
	 *
	 *  Postconditions: returns false if procedure fails.
	 *    otherwise:
	 *      * buffer is not null-terminated, but length contains the number of
	 *        digits.
	 *      * the representation in buffer is the most precise representation of
	 *        requestedDigits digits.
	 *      * buffer contains at most requestedDigits digits of w. If there are less
	 *        than requestedDigits digits then some trailing '0's have been removed.
	 *      * kappa is such that
	 *             w = buffer * 10^kappa + eps with |eps| < 10^kappa / 2.
	 *
	 *  Remark: This procedure takes into account the imprecision of its input
	 *    numbers. If the precision is not enough to guarantee all the postconditions
	 *    then false is returned. This usually happens rarely, but the failure-rate
	 *    increases with higher requestedDigits.
	 */
	private static boolean digitGenCounted(DiyFp w,
								int requestedDigits,
								char[] buffer,
								int[] length,
								int[] kappa) {
		DOUBLE_CONVERSION_ASSERT(MINIMAL_TARGET_EXPONENT <= w.e() && w.e() <= MAXIMAL_TARGET_EXPONENT);
		DOUBLE_CONVERSION_ASSERT(MINIMAL_TARGET_EXPONENT >= -60);
		DOUBLE_CONVERSION_ASSERT(MAXIMAL_TARGET_EXPONENT <= -32);
		// w is assumed to have an error less than 1 unit. Whenever w is scaled we
		// also scale its error.
		@Unsigned long wError = 1L;
		// We cut the input number into two parts: the integral digits and the
		// fractional digits. We don't emit any decimal separator, but adapt kappa
		// instead. Example: instead of writing "1.2" we put "12" into the buffer and
		// increase kappa by 1.
		DiyFp one = new DiyFp(1L << -w.e(), w.e());
		// Division by one is a shift.
		@Unsigned int integrals = toUint(w.f() >>> -one.e());
		// Modulo by one is an and.
		@Unsigned long fractionals = w.f() & (one.f() - 1L);
		@Unsigned int divisor;
		int divisorExponentPlusOne;
		{
			int[] inDivisorExponentPlusOne = new int[1];
			@Unsigned int[] inDivisor = new int[1];
			biggestPowerTen(integrals, DiyFp.SIGNIFICAND_SIZE - (-one.e()),
					inDivisor, inDivisorExponentPlusOne);
			divisor = inDivisor[0];
			divisorExponentPlusOne = inDivisorExponentPlusOne[0];
		}

		kappa[0] = divisorExponentPlusOne;
		length[0] = 0;

		// Loop invariant: buffer = w / 10^kappa  (integer division)
		// The invariant holds for the first iteration: kappa has been initialized
		// with the divisor exponent + 1. And the divisor is the biggest power of ten
		// that is smaller than 'integrals'.
		while (kappa[0] > 0) {
			buffer[length[0]] = digitToChar(uDivide(integrals, divisor));
			length[0]++;
			requestedDigits--;
			integrals = uRemainder(integrals, divisor);
			kappa[0]--;
			// Note that kappa now equals the exponent of the divisor and that the
			// invariant thus holds again.
			if (requestedDigits == 0) break;
			divisor = uDivide(divisor, 10);
		}

		if (requestedDigits == 0) {
			@Unsigned long rest = (toUlong(integrals) << -one.e()) + fractionals;
			return roundWeedCounted(buffer, length[0], rest,
					toUlong(divisor) << -one.e(), wError,
					kappa);
		}

		// The integrals have been generated. We are at the point of the decimal
		// separator. In the following loop we simply multiply the remaining digits by
		// 10 and divide by one. We just need to pay attention to multiply associated
		// data (the 'unit'), too.
		// Note that the multiplication by 10 does not overflow, because w.e >= -60
		// and thus one.e >= -60.
		DOUBLE_CONVERSION_ASSERT(one.e() >= -60);
		DOUBLE_CONVERSION_ASSERT(ulongLT(fractionals, one.f()));
		DOUBLE_CONVERSION_ASSERT( ulongGE(uDivide(0xFFFF_FFFF_FFFF_FFFFL, 10L), one.f() ));
		while (requestedDigits > 0 && ulongGT(fractionals, wError)) {
			fractionals *= 10L;
			wError *= 10L;
			// Integer division by one.
			buffer[length[0]] = digitToChar(fractionals >>> -one.e());
			length[0]++;
			requestedDigits--;
			fractionals &= one.f() - 1L;  // Modulo by one.
			kappa[0]--;
		}
		if (requestedDigits != 0) return false;
		return roundWeedCounted(buffer, length[0], fractionals, one.f(), wError,
				kappa);
	}


	/**
	 * Provides a decimal representation of v.
	 * Returns true if it succeeds, otherwise the result cannot be trusted.
	 * There will be *outLength digits inside the buffer (not null-terminated).
	 * If the function returns true then
	 * 	v == (double) (buffer * 10^outDecimalExponent).
	 * The digits in the buffer are the shortest representation possible: no
	 * 0.09999999999999999 instead of 0.1. The shorter representation will even be
	 * chosen even if the longer one would be closer to v.
	 * The last digit will be closest to the actual v. That is, even if several
	 * digits might correctly yield 'v' when read again, the closest will be
	 * computed.
	 */
	private static boolean grisu3(double v,
					   FastDtoaMode mode,
					   char[] buffer,
					   int[] length,
					   int[] decimalExponent) {
		DiyFp w = new Ieee.Double(v).asNormalizedDiyFp();
		// boundaryMinus and boundaryPlus are the boundaries between v and its
		// closest floating-point neighbors. Any number strictly between
		// boundaryMinus and boundaryPlus will round to v when convert to a double.
		// grisu3 will never output representations that lie exactly on a boundary.
		DiyFp boundaryMinus, boundaryPlus;
		{
			DiyFp[] inBoundaryMinus = new DiyFp[1];
			DiyFp[] inBoundaryPlus = new DiyFp[1];
			if (mode == FastDtoaMode.SHORTEST) {
				new Ieee.Double(v).normalizedBoundaries(inBoundaryMinus, inBoundaryPlus);
			} else {
				DOUBLE_CONVERSION_ASSERT(mode == FastDtoaMode.SHORTEST_SINGLE);
				float singleV = (float)v;
				new Ieee.Single(singleV).normalizedBoundaries(inBoundaryMinus, inBoundaryPlus);
			}
			boundaryMinus = inBoundaryMinus[0];
			boundaryPlus = inBoundaryPlus[0];
		}
		DOUBLE_CONVERSION_ASSERT(boundaryPlus.e() == w.e());
		DiyFp tenMk; // Cached power of ten: 10^-k
		int mk;       // -k
		{
			DiyFp[] inTenMk = new DiyFp[1];
			int[] inMk = new int[1];
			int tenMkMinimalBinaryExponent =
					MINIMAL_TARGET_EXPONENT - (w.e() + DiyFp.SIGNIFICAND_SIZE);
			int tenMkMaximalBinaryExponent =
					MAXIMAL_TARGET_EXPONENT - (w.e() + DiyFp.SIGNIFICAND_SIZE);
			PowersOfTenCache.getCachedPowerForBinaryExponentRange(
					tenMkMinimalBinaryExponent,
					tenMkMaximalBinaryExponent,
					inTenMk, inMk);
			tenMk = inTenMk[0];
			mk = inMk[0];
		}
		DOUBLE_CONVERSION_ASSERT((MINIMAL_TARGET_EXPONENT <= w.e() + tenMk.e() +
				DiyFp.SIGNIFICAND_SIZE) &&
				(MAXIMAL_TARGET_EXPONENT >= w.e() + tenMk.e() +
						DiyFp.SIGNIFICAND_SIZE));
		// Note that tenMk is only an approximation of 10^-k. A DiyFp only contains a
		// 64 bit significand and tenMk is thus only precise up to 64 bits.

		// The DiyFp.times procedure rounds its result, and tenMk is approximated
		// too. The variable scaledW (as well as scaledBoundaryMinus/plus) are now
		// off by a small amount.
		// In fact: scaledW - w*10^k < 1ulp (unit in the last place) of scaledW.
		// In other words: let f = scaledW.f() and e = scaledW.e(), then
		//           (f-1) * 2^e < w*10^k < (f+1) * 2^e
		DiyFp scaledW = DiyFp.times(w, tenMk);
		DOUBLE_CONVERSION_ASSERT(scaledW.e() ==
				boundaryPlus.e() + tenMk.e() + DiyFp.SIGNIFICAND_SIZE);
		// In theory it would be possible to avoid some recomputations by computing
		// the difference between w and boundaryMinus/plus (a power of 2) and to
		// compute scaledBoundaryMinus/plus by subtracting/adding from
		// scaledW. However the code becomes much less readable and the speed
		// enhancements are not terriffic.
		DiyFp scaledBoundaryMinus = DiyFp.times(boundaryMinus, tenMk);
		DiyFp scaledBoundaryPlus  = DiyFp.times(boundaryPlus,  tenMk);

		// digitGen will generate the digits of scaledW. Therefore we have
		// v == (double) (scaledW * 10^-mk).
		// Set decimalExponent == -mk and pass it to digitGen. If scaledW is not an
		// integer than it will be updated. For instance if scaledW == 1.23 then
		// the buffer will be filled with "123" und the decimalExponent will be
		// decreased by 2.
		int[] kappa = new int[1];
		boolean result = digitGen(scaledBoundaryMinus, scaledW, scaledBoundaryPlus,
				buffer, length, kappa);
		decimalExponent[0] = -mk + kappa[0];
		return result;
	}


	/**
	 * The "counted" version of grisu3 (see above) only generates requestedDigits
	 * number of digits. This version does not generate the shortest representation,
	 * and with enough requested digits 0.1 will at some point print as 0.9999999...
	 * grisu3 is too imprecise for real halfway cases (1.5 will not work) and
	 * therefore the rounding strategy for halfway cases is irrelevant.
	 */
	private static boolean grisu3Counted(double v,
							  int requestedDigits,
							  char[] buffer,
							  int[] length,
							  int[] decimalExponent) {
		DiyFp w = new Ieee.Double(v).asNormalizedDiyFp();
		DiyFp ten_mk;  // Cached power of ten: 10^-k
		int mk;        // -k
		{
			DiyFp[] inTenMk = new DiyFp[1];
			int[] inMk = new int[1];
			int tenMkMinimalBinaryExponent =
					MINIMAL_TARGET_EXPONENT - (w.e() + DiyFp.SIGNIFICAND_SIZE);
			int tenMkMaximalBinaryExponent =
					MAXIMAL_TARGET_EXPONENT - (w.e() + DiyFp.SIGNIFICAND_SIZE);
			PowersOfTenCache.getCachedPowerForBinaryExponentRange(
					tenMkMinimalBinaryExponent,
					tenMkMaximalBinaryExponent,
					inTenMk, inMk);
			ten_mk = inTenMk[0];
			mk = inMk[0];
		}
		DOUBLE_CONVERSION_ASSERT((MINIMAL_TARGET_EXPONENT <= w.e() + ten_mk.e() +
				DiyFp.SIGNIFICAND_SIZE) &&
				(MAXIMAL_TARGET_EXPONENT >= w.e() + ten_mk.e() +
						DiyFp.SIGNIFICAND_SIZE));
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
				buffer, length, inKappa);
		decimalExponent[0] = -mk + inKappa[0];
		return result;
	}

	/**
	 *  Provides a decimal representation of v.
	 *  The result should be interpreted as buffer * 10^(point - outLength).
	 *
	 *  Precondition:
	 *    * v must be a strictly positive finite double.
	 *
	 *  Returns true if it succeeds, otherwise the result can not be trusted.
	 *  There will be *outLength digits inside the buffer followed by a null terminator.
	 *  If the function returns true and mode equals
	 *    - FAST_DTOA_SHORTEST, then
	 *      the parameter requestedDigits is ignored.
	 *      The result satisfies
	 *          v == (double) (buffer * 10^(point - outLength)).
	 *      The digits in the buffer are the shortest representation possible. E.g.
	 *      if 0.099999999999 and 0.1 represent the same double then "1" is returned
	 *      with point = 0.
	 *      The last digit will be closest to the actual v. That is, even if several
	 *      digits might correctly yield 'v' when read again, the buffer will contain
	 *      the one closest to v.
	 *    - FAST_DTOA_PRECISION, then
	 *      the buffer contains requestedDigits digits.
	 *      the difference v - (buffer * 10^(point-outLength)) is closest to zero for
	 *      all possible representations of requestedDigits digits.
	 *      If there are two values that are equally close, then fastDtoa returns
	 *      false.
	 *  For both modes the buffer must be large enough to hold the result.
	 */
	public static boolean fastDtoa(double v,
								   FastDtoaMode mode,
								   int requestedDigits,
								   char[] buffer,
								   int[] length,
								   int[] decimalPoint) {
		DOUBLE_CONVERSION_ASSERT(v > 0.0);
		DOUBLE_CONVERSION_ASSERT(!new Ieee.Double(v).isSpecial());

		boolean result = false;
		int[] decimalExponent = new int[1]; // initialized to 0
		switch (mode) {
			case SHORTEST:
			case SHORTEST_SINGLE:
				result = grisu3(v, mode, buffer, length, decimalExponent);
				break;
			case PRECISION:
				result = grisu3Counted(v, requestedDigits,
						buffer, length, decimalExponent);
				break;
			default:
				throw new IllegalStateException("Unreachable");
		}
		if (result) {
			decimalPoint[0] = length[0] + decimalExponent[0];
			buffer[length[0]] = '\0';
		}
		return result;
	}

}
