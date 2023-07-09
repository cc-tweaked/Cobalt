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

import org.checkerframework.checker.signedness.qual.Unsigned;

import static cc.tweaked.cobalt.internal.doubles.Assert.requireArg;
import static cc.tweaked.cobalt.internal.doubles.UnsignedValues.ulongGE;

/**
 * This "Do It Yourself Floating Point" class implements a floating-point number
 * with a uint64 significand and an int exponent. Normalized DiyFp numbers will
 * have the most significant bit of the significand set.
 * Multiplication and Subtraction do not normalize their results.
 * DiyFp store only non-negative numbers and are not designed to contain special
 * doubles (NaN and Infinity).
 */
record DiyFp(@Unsigned long significand, int exponent) {
	public static final int SIGNIFICAND_SIZE = 64;

	/**
	 * Returns a - b.
	 * The exponents of both numbers must be the same and a must be greater
	 * or equal than b. The result will not be normalized.
	 */
	public static DiyFp minus(DiyFp a, DiyFp b) {
		requireArg(a.exponent == b.exponent, "exponents must match");
		requireArg(ulongGE(a.significand, b.significand), "other.f must be greater than this.f");
		return new DiyFp(a.significand - b.significand, a.exponent);
	}

	/**
	 * returns a * b;
	 */
	public static DiyFp times(DiyFp left, DiyFp right) {
		// Simply "emulates" a 128 bit multiplication.
		// However: the resulting number only contains 64 bits. The least
		// significant 64 bits are only used for rounding the most significant 64
		// bits.
		@Unsigned long otherF = right.significand;

		final long kM32 = 0xFFFFFFFFL;
		final long a = left.significand >>> 32;
		final long b = left.significand & kM32;
		final long c = otherF >>> 32;
		final long d = otherF & kM32;
		final long ac = a * c;
		final long bc = b * c;
		final long ad = a * d;
		final long bd = b * d;
		// By adding 1U << 31 to tmp we round the final result.
		// Halfway cases will be rounded up.
		final long tmp = (bd >>> 32) + (ad & kM32) + (bc & kM32) + (1L << 31);
		return new DiyFp(
			ac + (ad >>> 32) + (bc >>> 32) + (tmp >>> 32),
			left.exponent + right.exponent + 64
		);
	}
}
