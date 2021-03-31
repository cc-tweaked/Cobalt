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

package org.squiddev.cobalt.lib.fmt;

import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

import static org.squiddev.cobalt.lib.fmt.Assert.DOUBLE_CONVERSION_ASSERT;
import static org.squiddev.cobalt.lib.fmt.UnsignedLong.ONE;

public class Ieee {

	public static class Double {
		public static final @Unsigned long kSignMask = 0x8000000000000000L;
		public static final @Unsigned long kExponentMask = 0x7FF0000000000000L;
		public static final @Unsigned long kSignificandMask = 0x000FFFFFFFFFFFFFL;
		public static final @Unsigned long kHiddenBit = 0x0010000000000000L;
		public static final @Unsigned long kQuietNanBit = 0x0008000000000000L;

		public static final int kPhysicalSignificandSize = 52;  // Excludes the hidden bit.
		public static final int kSignificandSize = 53;
		public static final int kExponentBias = 0x3FF + kPhysicalSignificandSize;
		public static final int kMaxExponent = 0x7FF - kExponentBias;

		public Double() { d64_ = 0L;}
		@SuppressWarnings("cast.unsafe")
		public Double(double d) {
			this.d64_ = (@Unsigned long)java.lang.Double.doubleToRawLongBits(d);
		}
		public Double(@Unsigned long d64) { this.d64_ = d64; }
		public Double(DiyFp diy_fp) {
			d64_ = DiyFpToUint64(diy_fp);
		}

		/**
		 *  The value encoded by this Double must be greater or equal to +0.0.
		 *  It must not be special (infinity, or NaN).
		 */
		public DiyFp AsDiyFp() {
			DOUBLE_CONVERSION_ASSERT(Sign() > 0);
			DOUBLE_CONVERSION_ASSERT(!IsSpecial());
			return new DiyFp(UnsignedLong.uValueOf(Significand()),
					Exponent());
		}

		// The value encoded by this Double must be strictly greater than 0.
		public DiyFp AsNormalizedDiyFp() {
			DOUBLE_CONVERSION_ASSERT(value() > 0.0);
			@Unsigned long f = Significand();
			int e = Exponent();

			// The current double could be a denormal.
			while ((f & kHiddenBit) == 0L) {
				f <<= 1L;
				e--;
			}
			// Do the final shifts in one go.
			//noinspection ImplicitNumericConversion
			f <<= DiyFp.kSignificandSize - kSignificandSize;
			e -= DiyFp.kSignificandSize - kSignificandSize;
			return new DiyFp(UnsignedLong.uValueOf(f), e);
		}

		// Returns the double's bit as uint64.
		public @Unsigned long AsUint64() {
			return d64_;
		}


		/** Returns the next greater double. Returns +infinity on input +infinity. */
		public double NextDouble() {
			if (d64_ == kInfinity) return new Double(kInfinity).value();
			if (Sign() < 0 && Significand() == 0L) {
				// -0.0
				return 0.0;
			}
			if (Sign() < 0) {
				return new Double(d64_ - 1L).value();
			} else {
				return new Double(d64_ + 1L).value();
			}
		}

		public double PreviousDouble() {
			if (d64_ == (kInfinity | kSignMask)) return -Infinity();
			if (Sign() < 0) {
				return new Double(d64_ + 1L).value();
			} else {
				if (Significand() == 0L) return -0.0;
				return new Double(d64_ - 1L).value();
			}
		}

		public int Exponent() {
			if (IsDenormal()) return kDenormalExponent;

			long d64 = AsUint64();
			// Type Safety - Okay to cast, because the Shift-right is 52 bits
			int biased_e =
					(int)((d64 & kExponentMask) >> kPhysicalSignificandSize);
			return biased_e - kExponentBias;
		}

		public @Unsigned long Significand() {
			@Unsigned long d64 = AsUint64();
			@Unsigned long significand = d64 & kSignificandMask;
			if (!IsDenormal()) {
				return significand + kHiddenBit;
			} else {
				return significand;
			}
		}

		/** Returns true if the double is a denormal. */
		public boolean IsDenormal() {
			long d64 = AsUint64();
			return (d64 & kExponentMask) == 0L;
		}

		/**
		 *  We consider denormals not to be special.
		 *  Hence only Infinity and NaN are special.
		 */
		public boolean IsSpecial() {
			long d64 = AsUint64();
			return (d64 & kExponentMask) == kExponentMask;
		}

		public boolean IsNan() {
			long d64 = AsUint64();
			return ((d64 & kExponentMask) == kExponentMask) &&
					((d64 & kSignificandMask) != 0L);
		}

		public boolean IsQuietNan() {
			return IsNan() && ((AsUint64() & kQuietNanBit) != 0L);
		}

		public boolean IsSignalingNan() {
			return IsNan() && ((AsUint64() & kQuietNanBit) == 0L);
		}


		public boolean IsInfinite() {
			long d64 = AsUint64();
			return ((d64 & kExponentMask) == kExponentMask) &&
					((d64 & kSignificandMask) == 0L);
		}

		public int Sign() {
			long d64 = AsUint64();
			return (d64 & kSignMask) == 0L ? 1: -1;
		}

		/**
		 * Precondition: the value encoded by this Double must be greater or equal
		 * than +0.0.
		 */
		public DiyFp UpperBoundary() {
			DOUBLE_CONVERSION_ASSERT(Sign() > 0);
			return new DiyFp(UnsignedLong.uValueOf((Significand() * 2L) + 1L),
					Exponent() - 1);
		}

		/**
		 * Computes the two boundaries of this.
		 * The bigger boundary (m_plus) is normalized. The lower boundary has the same
		 * exponent as m_plus.
		 * Precondition: the value encoded by this Double must be greater than 0.
		 */
		public void NormalizedBoundaries(DiyFp[] out_m_minus, DiyFp[] out_m_plus) {
			DOUBLE_CONVERSION_ASSERT(value() > 0.0);
			DiyFp v = this.AsDiyFp();
			DiyFp mPlus = DiyFp.Normalize(new DiyFp(v.f().shl(1).plus(ONE), v.e() - 1));
			DiyFp mMinus;
			if (LowerBoundaryIsCloser()) {
				mMinus = new DiyFp(v.f().shl(2).minus(ONE), v.e() - 2);
			} else {
				mMinus = new DiyFp(v.f().shl(1).minus(ONE), v.e() - 1);
			}
			mMinus.set_f(mMinus.f().shl(mMinus.e() - mPlus.e()));
			mMinus.set_e(mPlus.e());
			out_m_plus[0] = mPlus;
			out_m_minus[0] = mMinus;
		}

		public boolean LowerBoundaryIsCloser() {
			// The boundary is closer if the significand is of the form f == 2^p-1 then
			// the lower boundary is closer.
			// Think of v = 1000e10 and v- = 9999e9.
			// Then the boundary (== (v - v-)/2) is not just at a distance of 1e9 but
			// at a distance of 1e8.
			// The only exception is for the smallest normal: the largest denormal is
			// at the same distance as its successor.
			// Note: denormals have the same exponent as the smallest normals.
			boolean physical_significand_is_zero = ((AsUint64() & kSignificandMask) == 0L);
			return physical_significand_is_zero && (Exponent() != kDenormalExponent);
		}

		@SuppressWarnings("cast.unsafe")
		public double value() { return java.lang.Double.longBitsToDouble((@Signed long)d64_); }

		/**
		 *  Returns the significand size for a given order of magnitude.
		 *  If v = f*2^e with 2^p-1 <= f <= 2^p then p+e is v's order of magnitude.
		 *  This function returns the number of significant binary digits v will have
		 *  once it's encoded into a double. In almost all cases this is equal to
		 *  kSignificandSize. The only exceptions are denormals. They start with
		 *  leading zeroes and their effective significand-size is hence smaller.
		 */
		public static int SignificandSizeForOrderOfMagnitude(int order) {
			if (order >= (kDenormalExponent + kSignificandSize)) {
				return kSignificandSize;
			}
			if (order <= kDenormalExponent) return 0;
			return order - kDenormalExponent;
		}

		public static double Infinity() {
			return new Double(kInfinity).value();
		}

		public static double NaN() {
			return new Double(kNaN).value();
		}

		private static final int kDenormalExponent = -kExponentBias + 1;
		private static final @Unsigned long kInfinity = 0x7FF0000000000000L;
		private static final @Unsigned long kNaN = 0x7FF8000000000000L;

		private final @Unsigned long d64_;

		private static @Unsigned long DiyFpToUint64(DiyFp diy_fp) {
			UnsignedLong significand = diy_fp.f();
			int exponent = diy_fp.e();
			while (significand.gt(
					UnsignedLong.uValueOf(kHiddenBit + kSignificandMask))) {
				significand = significand.shr(1);
				exponent++;
			}
			if (exponent >= kMaxExponent) {
				return kInfinity;
			}
			if (exponent < kDenormalExponent) {
				return 0L;
			}
			while (exponent > kDenormalExponent && significand.bitAndU(kHiddenBit).eq(0L)) {
				significand = significand.shl(1);
				exponent--;
			}
			long biased_exponent;
			if (exponent == kDenormalExponent && significand.bitAndU(kHiddenBit).eq(0L)) {
				biased_exponent = 0L;
			} else {
				//noinspection ImplicitNumericConversion
				biased_exponent = (exponent + kExponentBias);
			}
			return significand.bitAndU(kSignificandMask).unsafeLongValue() |
					(biased_exponent << kPhysicalSignificandSize);
		}

	}

	public static class Single {
		public static final @Unsigned int kSignMask = 0x80000000;
		public static final @Unsigned int kExponentMask = 0x7F800000;
		public static final @Unsigned int kSignificandMask = 0x007FFFFF;
		public static final @Unsigned int kHiddenBit = 0x00800000;
		public static final @Unsigned int kQuietNanBit = 0x00400000;
		public static final int kPhysicalSignificandSize = 23;  // Excludes the hidden bit.
		public static final int kSignificandSize = 24;

		public Single() { this.d32_ = 0; }
		@SuppressWarnings("cast.unsafe")
		public Single(float f) { d32_ = (@Unsigned int)Float.floatToIntBits(f); }
		public Single(@Unsigned int d32) { this.d32_ = d32; }

		/**
		 *  The value encoded by this Single must be greater or equal to +0.0.
		 *  It must not be special (infinity, or NaN).
		 */
		public DiyFp AsDiyFp() {
			DOUBLE_CONVERSION_ASSERT(Sign() > 0);
			DOUBLE_CONVERSION_ASSERT(!IsSpecial());
			return new DiyFp(UnsignedLong.uValueOf(Significand()), Exponent());
		}

		/** Returns the single's bit as uint64. */
		public @Unsigned int AsUint32() {
			return d32_;
		}

		int Exponent() {
			if (IsDenormal()) return kDenormalExponent;

			int d32 = AsUint32();
			int biased_e =
					((d32 & kExponentMask) >>> kPhysicalSignificandSize);
			return biased_e - kExponentBias;
		}

		public @Unsigned int Significand() {
			int d32 = AsUint32();
			int significand = d32 & kSignificandMask;
			if (!IsDenormal()) {
				return significand + kHiddenBit;
			} else {
				return significand;
			}
		}

		/** Returns true if the single is a denormal. */
		public boolean IsDenormal() {
			int d32 = AsUint32();
			return (d32 & kExponentMask) == 0;
		}

		/**
		 *  We consider denormals not to be special.
		 *  Hence only Infinity and NaN are special.
		 */
		public boolean IsSpecial() {
			int d32 = AsUint32();
			return (d32 & kExponentMask) == kExponentMask;
		}

		public boolean IsNan() {
			int d32 = AsUint32();
			return ((d32 & kExponentMask) == kExponentMask) &&
					((d32 & kSignificandMask) != 0);
		}

		public boolean IsQuietNan() {
			return IsNan() && ((AsUint32() & kQuietNanBit) != 0);
		}

		public boolean IsSignalingNan() {
			return IsNan() && ((AsUint32() & kQuietNanBit) == 0);
		}


		public boolean IsInfinite() {
			int d32 = AsUint32();
			return ((d32 & kExponentMask) == kExponentMask) &&
					((d32 & kSignificandMask) == 0);
		}

		public int Sign() {
			int d32 = AsUint32();
			return (d32 & kSignMask) == 0? 1: -1;
		}

		/**
		 *  Computes the two boundaries of this.
		 *  The bigger boundary (m_plus) is normalized. The lower boundary has the same
		 *  exponent as m_plus.
		 *  Precondition: the value encoded by this Single must be greater than 0.
		 */
		void NormalizedBoundaries(DiyFp[] out_m_minus, DiyFp[] out_m_plus) {
			DOUBLE_CONVERSION_ASSERT(value() > 0.0F);
			DiyFp v = this.AsDiyFp();
			DiyFp m_plus = DiyFp.Normalize(new DiyFp(v.f().shl(1).plus(ONE), v.e() - 1));
			DiyFp m_minus;
			if (LowerBoundaryIsCloser()) {
				m_minus = new DiyFp(v.f().shl(2).minus(ONE), v.e() - 2);
			} else {
				m_minus = new DiyFp(v.f().shl(1).minus(ONE), v.e() - 1);
			}
			m_minus.set_f(m_minus.f().shl(m_minus.e() - m_plus.e()));
			m_minus.set_e(m_plus.e());
    		out_m_plus[0] = m_plus;
    		out_m_minus[0] = m_minus;
		}

		/**
		 *  Precondition: the value encoded by this Single must be greater or equal
		 *  than +0.0.
		 */
		public DiyFp UpperBoundary() {
			DOUBLE_CONVERSION_ASSERT(Sign() > 0);
			return new DiyFp(UnsignedLong.uValueOf((Significand() * 2) + 1), Exponent() - 1);
		}

		public boolean LowerBoundaryIsCloser() {
			// The boundary is closer if the significand is of the form f == 2^p-1 then
			// the lower boundary is closer.
			// Think of v = 1000e10 and v- = 9999e9.
			// Then the boundary (== (v - v-)/2) is not just at a distance of 1e9 but
			// at a distance of 1e8.
			// The only exception is for the smallest normal: the largest denormal is
			// at the same distance as its successor.
			// Note: denormals have the same exponent as the smallest normals.
			boolean physical_significand_is_zero = ((AsUint32() & kSignificandMask) == 0);
			return physical_significand_is_zero && (Exponent() != kDenormalExponent);
		}

		@SuppressWarnings("cast.unsafe")
		public float value() { return Float.intBitsToFloat((@Signed int)d32_); }

		public static float Infinity() {
			return new Single(kInfinity).value();
		}

		public static float NaN() {
			return new Single(kNaN).value();
		}

		private static final @Unsigned int kExponentBias = 0x7F + kPhysicalSignificandSize;
		private static final @Unsigned int kDenormalExponent = -kExponentBias + 1;
		private static final @Unsigned int kMaxExponent = 0xFF - kExponentBias;
		private static final @Unsigned int kInfinity = 0x7F800000;
		private static final @Unsigned int kNaN = 0x7FC00000;

  		private final @Unsigned int d32_;
	}

}
