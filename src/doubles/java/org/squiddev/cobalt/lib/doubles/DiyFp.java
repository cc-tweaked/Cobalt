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

package org.squiddev.cobalt.lib.doubles;

import org.checkerframework.checker.signedness.qual.Unsigned;

import static org.squiddev.cobalt.lib.doubles.Assert.DOUBLE_CONVERSION_ASSERT;
import static org.squiddev.cobalt.lib.doubles.UnsignedValues.ulongGE;

/**
 * This "Do It Yourself Floating Point" class implements a floating-point number
 * with a uint64 significand and an int exponent. Normalized DiyFp numbers will
 * have the most significant bit of the significand set.
 * Multiplication and Subtraction do not normalize their results.
 * DiyFp store only non-negative numbers and are not designed to contain special
 * doubles (NaN and Infinity).
 */
public class DiyFp {
	public static final int SIGNIFICAND_SIZE = 64;

	private static final long UINT_64_MSB = 0x80000000_00000000L;

	private @Unsigned long f;
	private int e;

	DiyFp() { this.f = 0L; this.e = 0; }
	DiyFp(@Unsigned long significand, final int exponent) { this.f = significand; this.e = exponent; }

	public DiyFp copy() {
		return new DiyFp(this.f, this.e);
	}

	/**
	 * this -= other.
	 * The exponents of both numbers must be the same and the significand of this
	 * must be greater or equal than the significand of other.
	 * The result will not be normalized.
	 */
	public void subtract(DiyFp other) {
		DOUBLE_CONVERSION_ASSERT(e == other.e);
		DOUBLE_CONVERSION_ASSERT(ulongGE(f, other.f));
		f = f - other.f;
	}

	/**
	 * Returns a - b.
	 * The exponents of both numbers must be the same and a must be greater
	 * or equal than b. The result will not be normalized.
	 */
	public static DiyFp minus(DiyFp a, DiyFp b) {
		DiyFp result = a.copy();
		result.subtract(b);
		return result;
	}

	/**
	 * this *= other.
	 */
	public void multiply(DiyFp other) {
		// Simply "emulates" a 128 bit multiplication.
		// However: the resulting number only contains 64 bits. The least
		// significant 64 bits are only used for rounding the most significant 64
		// bits.
		@Unsigned long otherF = other.f;

		final long kM32 = 0xFFFFFFFFL;
		final long a = f >>> 32;
		final long b = f & kM32;
		final long c = otherF >>> 32;
		final long d = otherF & kM32;
		final long ac = a * c;
		final long bc = b * c;
		final long ad = a * d;
		final long bd = b * d;
		// By adding 1U << 31 to tmp we round the final result.
		// Halfway cases will be rounded up.
		final long tmp = (bd >>> 32) + (ad & kM32) + (bc & kM32) + (1L << 31);
		e += other.e + 64;
		this.f = ac + (ad >>> 32) + (bc >>> 32) + (tmp >>> 32);
	}


	/**
	 * returns a * b;
	 */
	public static DiyFp times(DiyFp a, DiyFp b) {
		DiyFp result = a.copy();
		result.multiply(b);
		return result;
	}

	public void normalize() {
		DOUBLE_CONVERSION_ASSERT(f != 0L);
		@Unsigned long significand = f;
		int exponent = e;

		// This method is mainly called for normalizing boundaries. In general,
		// boundaries need to be shifted by 10 bits, and we optimize for this case.
		final @Unsigned long k10MSBits = 0xFFC0_0000_0000_0000L;
		while ((significand & k10MSBits) == 0L) {
			significand <<= 10L;
			exponent -= 10;
		}
		while ((significand & UINT_64_MSB) == 0L) {
			significand <<= 1L;
			exponent--;
		}
		f = significand;
		e = exponent;
	}

	public static DiyFp normalize(DiyFp a) {
		DiyFp result = a.copy();
		result.normalize();
		return result;
	}

	public @Unsigned long f() { return f; }
	public int e()  { return e; }

	public void setF(@Unsigned long new_value) { f = new_value; }
	public void setE(int new_value) { e = new_value; }
}
