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

import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

import static cc.tweaked.cobalt.internal.doubles.Assert.requireState;

/**
 * Additional utility methods for working with doubles.
 */
class Doubles {
	public static final @Unsigned long SIGN_MASK = 0x8000000000000000L;
	private static final @Unsigned long EXPONENT_MASK = 0x7FF0000000000000L;
	private static final @Unsigned long SIGNIFICAND_MASK = 0x000FFFFFFFFFFFFFL;
	public static final @Unsigned long HIDDEN_BIT = 0x0010000000000000L;

	public static final int PHYSICAL_SIGNIFICAND_SIZE = 52;  // Excludes the hidden bit.
	public static final int SIGNIFICAND_SIZE = 53;
	private static final int EXPONENT_BIAS = 0x3FF + PHYSICAL_SIGNIFICAND_SIZE;
	private static final int DENORMAL_EXPONENT = -EXPONENT_BIAS + 1;

	private Doubles() {
	}

	@Deprecated
	public static DiyFp asNormalizedDiyFp(long value) {
		throw new IllegalStateException("Call the double version");
	}

	// The value encoded by this Double must be strictly greater than 0.
	public static DiyFp asNormalizedDiyFp(double value) {
		requireState(value > 0.0, "instance must be positive");
		@Unsigned long f = significand(value);
		int exponent = exponent(value);

		// The current double could be a denormal.
		while ((f & HIDDEN_BIT) == 0L) {
			f <<= 1L;
			exponent--;
		}
		// Do the final shifts in one go.
		//noinspection ImplicitNumericConversion
		f <<= DiyFp.SIGNIFICAND_SIZE - SIGNIFICAND_SIZE;
		exponent -= DiyFp.SIGNIFICAND_SIZE - SIGNIFICAND_SIZE;
		return new DiyFp(f, exponent);
	}

	public static int exponent(double v) {
		if (isDenormal(v)) return DENORMAL_EXPONENT;

		// Type Safety - Okay to cast, because the Shift-right is 52 bits
		@SuppressWarnings({"shift.signed", "cast.unsafe"})
		int biasedE = (@Signed int) ((getBits(v) & EXPONENT_MASK) >> PHYSICAL_SIGNIFICAND_SIZE);
		return biasedE - EXPONENT_BIAS;
	}

	public static @Unsigned long significand(double value) {
		@Unsigned long significand = getBits(value) & SIGNIFICAND_MASK;
		if (!isDenormal(value)) {
			return significand + HIDDEN_BIT;
		} else {
			return significand;
		}
	}

	/**
	 * Returns true if the double is a denormal.
	 */
	public static boolean isDenormal(@Unsigned long v) {
		return (v & EXPONENT_MASK) == 0L;
	}

	/**
	 * Returns true if the double is a denormal.
	 */
	public static boolean isDenormal(double v) {
		return isDenormal(getBits(v));
	}

	/**
	 * We consider denormals not to be special.
	 * Hence only Infinity and NaN are special.
	 */
	public static boolean isSpecial(@Unsigned long value) {
		return (value & EXPONENT_MASK) == EXPONENT_MASK;
	}

	/**
	 * We consider denormals not to be special.
	 * Hence only Infinity and NaN are special.
	 */
	public static boolean isSpecial(double value) {
		return isSpecial(getBits(value));
	}

	public static int sign(@Unsigned long value) {
		return (value & SIGN_MASK) == 0L ? 1 : -1;
	}

	public static int sign(double value) {
		return sign(getBits(value));
	}

	@SuppressWarnings("cast.unsafe")
	public static @Unsigned long getBits(double value) {
		return (@Unsigned long) Double.doubleToRawLongBits(value);
	}
}
