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

/**
 * This "Do It Yourself Floating Point" class implements a floating-point number
 * with a uint64 significand and an int exponent. Normalized DiyFp numbers will
 * have the most significant bit of the significand set.
 * Multiplication and Subtraction do not normalize their results.
 * DiyFp store only non-negative numbers and are not designed to contain special
 * doubles (NaN and Infinity).
 */
public class DiyFp {
	public static final int kSignificandSize = 64;

	DiyFp() { this.f_ = UnsignedLong.valueOf(0); this.e_ = 0; }
	DiyFp(final UnsignedLong significand, final int exponent) { this.f_ = significand; this.e_ = exponent; }

	/**
	 * this -= other.
	 * The exponents of both numbers must be the same and the significand of this
	 * must be greater or equal than the significand of other.
	 * The result will not be normalized.
	 */
	public void Subtract(DiyFp other) {
		DOUBLE_CONVERSION_ASSERT(e_ == other.e_);
		DOUBLE_CONVERSION_ASSERT(f_.ge(other.f_));
		f_ = f_.minus(other.f_);
	}

	/**
	 * Returns a - b.
	 * The exponents of both numbers must be the same and a must be greater
	 * or equal than b. The result will not be normalized.
	 */
	public static DiyFp Minus(DiyFp a, DiyFp b) {
		DiyFp result = a;
		result.Subtract(b);
		return result;
	}

	/**
	 * this *= other.
	 */
	public void Multiply(DiyFp other) {
		// Simply "emulates" a 128 bit multiplication.
		// However: the resulting number only contains 64 bits. The least
		// significant 64 bits are only used for rounding the most significant 64
		// bits.
		long f = f_.longValue();
		long otherF = other.f_.longValue();

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
		final long tmp = (bd >> 32) + (ad & kM32) + (bc & kM32) + (1L << 31);
		e_ += other.e_ + 64;
		f_ = UnsignedLong.valueOf(ac + (ad >>> 32) + (bc >>> 32) + (tmp >>> 32));
	}


	/**
	 * returns a * b;
	 */
	public static DiyFp Times(DiyFp a, DiyFp b) {
		DiyFp result = a;
		result.Multiply(b);
		return result;
	}

	public void Normalize() {
		DOUBLE_CONVERSION_ASSERT(!f_.eq(0));
		UnsignedLong significand = f_;
		int exponent = e_;

		// This method is mainly called for normalizing boundaries. In general,
		// boundaries need to be shifted by 10 bits, and we optimize for this case.
		final long k10MSBits = 0xFFC0000000000000L;
		while ((significand.bitAnd(k10MSBits).eq(0))) {
			significand = significand.shl(10);
			exponent -= 10;
		}
		while (significand.bitAnd(kUint64MSB).eq(0)) {
			significand = significand.shl(1);
			exponent--;
		}
		f_ = significand;
		e_ = exponent;
	}

	public static DiyFp Normalize(DiyFp a) {
		DiyFp result = a;
		result.Normalize();
		return result;
	}

	public UnsignedLong f() { return f_; }
	public int e()  { return e_; }

	public void set_f(UnsignedLong new_value) { f_ = new_value; }
	public void set_e(int new_value) { e_ = new_value; }

	private static final long kUint64MSB = 0x80000000_00000000L;

	private UnsignedLong f_;
	private int e_;
}
