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

import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

import static org.squiddev.cobalt.lib.doubles.Assert.DOUBLE_CONVERSION_ASSERT;
import static org.squiddev.cobalt.lib.doubles.UnsignedValues.toUlong;
import static org.squiddev.cobalt.lib.doubles.UnsignedValues.ulongGT;

public final class Ieee {

	public static class Double {
		public static final @Unsigned long SIGN_MASK = 0x8000000000000000L;
		public static final @Unsigned long EXPONENT_MASK = 0x7FF0000000000000L;
		public static final @Unsigned long SIGNIFICAND_MASK = 0x000FFFFFFFFFFFFFL;
		public static final @Unsigned long HIDDEN_BIT = 0x0010000000000000L;
		public static final @Unsigned long QUIET_NAN_BIT = 0x0008000000000000L;

		public static final int PHYSICAL_SIGNIFICAND_SIZE = 52;  // Excludes the hidden bit.
		public static final int SIGNIFICAND_SIZE = 53;
		public static final int EXPONENT_BIAS = 0x3FF + PHYSICAL_SIGNIFICAND_SIZE;
		public static final int MAX_EXPONENT = 0x7FF - EXPONENT_BIAS;

		public Double() { bits = 0L;}
		@SuppressWarnings("cast.unsafe")
		public Double(double d) {
			this.bits = (@Unsigned long)java.lang.Double.doubleToRawLongBits(d);
		}
		public Double(@Unsigned long d64) { this.bits = d64; }
		public Double(DiyFp diyFp) {
			bits = diyFpToUint64(diyFp);
		}

		/**
		 *  The value encoded by this Double must be greater or equal to +0.0.
		 *  It must not be special (infinity, or NaN).
		 */
		public DiyFp asDiyFp() {
			DOUBLE_CONVERSION_ASSERT(sign() > 0);
			DOUBLE_CONVERSION_ASSERT(!isSpecial());
			return new DiyFp(significand(),
					exponent());
		}

		// The value encoded by this Double must be strictly greater than 0.
		public DiyFp asNormalizedDiyFp() {
			DOUBLE_CONVERSION_ASSERT(value() > 0.0);
			@Unsigned long f = significand();
			int e = exponent();

			// The current double could be a denormal.
			while ((f & HIDDEN_BIT) == 0L) {
				f <<= 1L;
				e--;
			}
			// Do the final shifts in one go.
			//noinspection ImplicitNumericConversion
			f <<= DiyFp.SIGNIFICAND_SIZE - SIGNIFICAND_SIZE;
			e -= DiyFp.SIGNIFICAND_SIZE - SIGNIFICAND_SIZE;
			return new DiyFp(f, e);
		}

		// Returns the double's bit as uint64.
		public @Unsigned long asUint64() {
			return bits;
		}


		/** Returns the next greater double. Returns +infinity on input +infinity. */
		public double nextDouble() {
			if (bits == INFINITY) return new Double(INFINITY).value();
			if (sign() < 0 && significand() == 0L) {
				// -0.0
				return 0.0;
			}
			if (sign() < 0) {
				return new Double(bits - 1L).value();
			} else {
				return new Double(bits + 1L).value();
			}
		}

		public double previousDouble() {
			if (bits == (INFINITY | SIGN_MASK)) return -infinity();
			if (sign() < 0) {
				return new Double(bits + 1L).value();
			} else {
				if (significand() == 0L) return -0.0;
				return new Double(bits - 1L).value();
			}
		}

		public int exponent() {
			if (isDenormal()) return DENORMAL_EXPONENT;

			long d64 = asUint64();
			// Type Safety - Okay to cast, because the Shift-right is 52 bits
			int biasedE =
					(int)((d64 & EXPONENT_MASK) >> PHYSICAL_SIGNIFICAND_SIZE);
			return biasedE - EXPONENT_BIAS;
		}

		public @Unsigned long significand() {
			@Unsigned long d64 = asUint64();
			@Unsigned long significand = d64 & SIGNIFICAND_MASK;
			if (!isDenormal()) {
				return significand + HIDDEN_BIT;
			} else {
				return significand;
			}
		}

		/** Returns true if the double is a denormal. */
		public boolean isDenormal() {
			long d64 = asUint64();
			return (d64 & EXPONENT_MASK) == 0L;
		}

		/**
		 *  We consider denormals not to be special.
		 *  Hence only Infinity and NaN are special.
		 */
		public boolean isSpecial() {
			long d64 = asUint64();
			return (d64 & EXPONENT_MASK) == EXPONENT_MASK;
		}

		public boolean isNan() {
			long d64 = asUint64();
			return ((d64 & EXPONENT_MASK) == EXPONENT_MASK) &&
					((d64 & SIGNIFICAND_MASK) != 0L);
		}

		public boolean isQuietNan() {
			return isNan() && ((asUint64() & QUIET_NAN_BIT) != 0L);
		}

		public boolean isSignalingNan() {
			return isNan() && ((asUint64() & QUIET_NAN_BIT) == 0L);
		}


		public boolean isInfinite() {
			long d64 = asUint64();
			return ((d64 & EXPONENT_MASK) == EXPONENT_MASK) &&
					((d64 & SIGNIFICAND_MASK) == 0L);
		}

		public int sign() {
			long d64 = asUint64();
			return (d64 & SIGN_MASK) == 0L ? 1: -1;
		}

		/**
		 * Precondition: the value encoded by this Double must be greater or equal
		 * than +0.0.
		 */
		public DiyFp upperBoundary() {
			DOUBLE_CONVERSION_ASSERT(sign() > 0);
			return new DiyFp((significand() * 2L) + 1L,
					exponent() - 1);
		}

		/**
		 * Computes the two boundaries of this.
		 * The bigger boundary (m_plus) is normalized. The lower boundary has the same
		 * exponent as m_plus.
		 * Precondition: the value encoded by this Double must be greater than 0.
		 */
		public void normalizedBoundaries(DiyFp[] outMMinus, DiyFp[] outMPlus) {
			DOUBLE_CONVERSION_ASSERT(value() > 0.0);
			DiyFp v = this.asDiyFp();
			DiyFp mPlus = DiyFp.normalize(new DiyFp((v.f() << 1) + 1L, v.e() - 1));
			DiyFp mMinus;
			if (lowerBoundaryIsCloser()) {
				mMinus = new DiyFp((v.f() << 2) - 1L, v.e() - 2);
			} else {
				mMinus = new DiyFp((v.f() << 1) - 1L, v.e() - 1);
			}
			mMinus.setF(mMinus.f() << (mMinus.e() - mPlus.e()));
			mMinus.setE(mPlus.e());
			outMPlus[0] = mPlus;
			outMMinus[0] = mMinus;
		}

		public boolean lowerBoundaryIsCloser() {
			// The boundary is closer if the significand is of the form f == 2^p-1 then
			// the lower boundary is closer.
			// Think of v = 1000e10 and v- = 9999e9.
			// Then the boundary (== (v - v-)/2) is not just at a distance of 1e9 but
			// at a distance of 1e8.
			// The only exception is for the smallest normal: the largest denormal is
			// at the same distance as its successor.
			// Note: denormals have the same exponent as the smallest normals.
			boolean physicalSignificandIsZero = ((asUint64() & SIGNIFICAND_MASK) == 0L);
			return physicalSignificandIsZero && (exponent() != DENORMAL_EXPONENT);
		}

		@SuppressWarnings("cast.unsafe")
		public double value() { return java.lang.Double.longBitsToDouble((@Signed long) bits); }

		/**
		 *  Returns the significand size for a given order of magnitude.
		 *  If v = f*2^e with 2^p-1 <= f <= 2^p then p+e is v's order of magnitude.
		 *  This function returns the number of significant binary digits v will have
		 *  once it's encoded into a double. In almost all cases this is equal to
		 *  kSignificandSize. The only exceptions are denormals. They start with
		 *  leading zeroes and their effective significand-size is hence smaller.
		 */
		public static int significandSizeForOrderOfMagnitude(int order) {
			if (order >= (DENORMAL_EXPONENT + SIGNIFICAND_SIZE)) {
				return SIGNIFICAND_SIZE;
			}
			if (order <= DENORMAL_EXPONENT) return 0;
			return order - DENORMAL_EXPONENT;
		}

		public static double infinity() {
			return new Double(INFINITY).value();
		}

		public static double nan() {
			return new Double(NAN).value();
		}

		private static final int DENORMAL_EXPONENT = -EXPONENT_BIAS + 1;
		private static final @Unsigned long INFINITY = 0x7FF0000000000000L;
		private static final @Unsigned long NAN = 0x7FF8000000000000L;

		private final @Unsigned long bits;

		private static @Unsigned long diyFpToUint64(DiyFp diyFp) {
			@Unsigned long significand = diyFp.f();
			int exponent = diyFp.e();
			while (ulongGT(significand, HIDDEN_BIT + SIGNIFICAND_MASK)) {
				significand >>>= 1L;
				exponent++;
			}
			if (exponent >= MAX_EXPONENT) {
				return INFINITY;
			}
			if (exponent < DENORMAL_EXPONENT) {
				return 0L;
			}
			while (exponent > DENORMAL_EXPONENT && (significand & HIDDEN_BIT) == 0L) {
				significand <<= 1L;
				exponent--;
			}
			long biasedExponent;
			if (exponent == DENORMAL_EXPONENT && (significand & HIDDEN_BIT) == 0L) {
				biasedExponent = 0L;
			} else {
				biasedExponent = toUlong(exponent + EXPONENT_BIAS);
			}
			return (significand & SIGNIFICAND_MASK) |
					(biasedExponent << PHYSICAL_SIGNIFICAND_SIZE);
		}

	}

	public static class Single {
		public static final @Unsigned int SIGN_MASK = 0x80000000;
		public static final @Unsigned int EXPONENT_MASK = 0x7F800000;
		public static final @Unsigned int SIGNIFICAND_MASK = 0x007FFFFF;
		public static final @Unsigned int HIDDEN_BIT = 0x00800000;
		public static final @Unsigned int QUIET_NAN_BIT = 0x00400000;
		public static final int PHYSICAL_SIGNIFICAND_SIZE = 23;  // Excludes the hidden bit.
		public static final int SIGNIFICAND_SIZE = 24;

		public Single() { this.bits = 0; }
		@SuppressWarnings("cast.unsafe")
		public Single(float f) { bits = (@Unsigned int)Float.floatToIntBits(f); }
		public Single(@Unsigned int d32) { this.bits = d32; }

		/**
		 *  The value encoded by this Single must be greater or equal to +0.0.
		 *  It must not be special (infinity, or NaN).
		 */
		public DiyFp asDiyFp() {
			DOUBLE_CONVERSION_ASSERT(sign() > 0);
			DOUBLE_CONVERSION_ASSERT(!isSpecial());
			return new DiyFp(toUlong(significand()), exponent());
		}

		/** Returns the single's bit as uint64. */
		public @Unsigned int asUint32() {
			return bits;
		}

		int exponent() {
			if (isDenormal()) return DENORMAL_EXPONENT;

			int d32 = asUint32();
			int biasedE =
					((d32 & EXPONENT_MASK) >>> PHYSICAL_SIGNIFICAND_SIZE);
			return biasedE - EXPONENT_BIAS;
		}

		public @Unsigned int significand() {
			int d32 = asUint32();
			int significand = d32 & SIGNIFICAND_MASK;
			if (!isDenormal()) {
				return significand + HIDDEN_BIT;
			} else {
				return significand;
			}
		}

		/** Returns true if the single is a denormal. */
		public boolean isDenormal() {
			int d32 = asUint32();
			return (d32 & EXPONENT_MASK) == 0;
		}

		/**
		 *  We consider denormals not to be special.
		 *  Hence only Infinity and NaN are special.
		 */
		public boolean isSpecial() {
			int d32 = asUint32();
			return (d32 & EXPONENT_MASK) == EXPONENT_MASK;
		}

		public boolean isNan() {
			int d32 = asUint32();
			return ((d32 & EXPONENT_MASK) == EXPONENT_MASK) &&
					((d32 & SIGNIFICAND_MASK) != 0);
		}

		public boolean isQuietNan() {
			return isNan() && ((asUint32() & QUIET_NAN_BIT) != 0);
		}

		public boolean isSignalingNan() {
			return isNan() && ((asUint32() & QUIET_NAN_BIT) == 0);
		}


		public boolean isInfinite() {
			int d32 = asUint32();
			return ((d32 & EXPONENT_MASK) == EXPONENT_MASK) &&
					((d32 & SIGNIFICAND_MASK) == 0);
		}

		public int sign() {
			int d32 = asUint32();
			return (d32 & SIGN_MASK) == 0? 1: -1;
		}

		/**
		 *  Computes the two boundaries of this.
		 *  The bigger boundary (m_plus) is normalized. The lower boundary has the same
		 *  exponent as m_plus.
		 *  Precondition: the value encoded by this Single must be greater than 0.
		 */
		void normalizedBoundaries(DiyFp[] outMMinus, DiyFp[] outMPlus) {
			DOUBLE_CONVERSION_ASSERT(value() > 0.0F);
			DiyFp v = this.asDiyFp();
			DiyFp mPlus = DiyFp.normalize(new DiyFp((v.f() << 1) + 1L, v.e() - 1));
			DiyFp mMinus;
			if (lowerBoundaryIsCloser()) {
				mMinus = new DiyFp((v.f() << 2) - 1L, v.e() - 2);
			} else {
				mMinus = new DiyFp((v.f() << 1) - 1L, v.e() - 1);
			}
			mMinus.setF(mMinus.f() << (mMinus.e() - mPlus.e()));
			mMinus.setE(mPlus.e());
    		outMPlus[0] = mPlus;
    		outMMinus[0] = mMinus;
		}

		/**
		 *  Precondition: the value encoded by this Single must be greater or equal
		 *  than +0.0.
		 */
		public DiyFp upperBoundary() {
			DOUBLE_CONVERSION_ASSERT(sign() > 0);
			return new DiyFp((toUlong(significand()) * 2L) + 1L, exponent() - 1);
		}

		public boolean lowerBoundaryIsCloser() {
			// The boundary is closer if the significand is of the form f == 2^p-1 then
			// the lower boundary is closer.
			// Think of v = 1000e10 and v- = 9999e9.
			// Then the boundary (== (v - v-)/2) is not just at a distance of 1e9 but
			// at a distance of 1e8.
			// The only exception is for the smallest normal: the largest denormal is
			// at the same distance as its successor.
			// Note: denormals have the same exponent as the smallest normals.
			boolean physicalSignificandIsZero = ((asUint32() & SIGNIFICAND_MASK) == 0);
			return physicalSignificandIsZero && (exponent() != DENORMAL_EXPONENT);
		}

		@SuppressWarnings("cast.unsafe")
		public float value() { return Float.intBitsToFloat((@Signed int) bits); }

		public static float infinity() {
			return new Single(INFINITY).value();
		}

		public static float nan() {
			return new Single(NAN).value();
		}

		private static final @Unsigned int EXPONENT_BIAS = 0x7F + PHYSICAL_SIGNIFICAND_SIZE;
		private static final @Unsigned int DENORMAL_EXPONENT = -EXPONENT_BIAS + 1;
		private static final @Unsigned int MAX_EXPONENT = 0xFF - EXPONENT_BIAS;
		private static final @Unsigned int INFINITY = 0x7F800000;
		private static final @Unsigned int NAN = 0x7FC00000;

  		private final @Unsigned int bits;
	}

	private Ieee() {}
}
