/**
 *  Copyright 2006-2008 the V8 project authors. All rights reserved.
 *  Copyright 2021 sir-maniac. All Rights reserved.
 *
 *  Ported to java by sir-maniac
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of Google Inc. nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.squiddev.cobalt.lib.doubles;

import org.checkerframework.checker.signedness.qual.Unsigned;
import org.junit.jupiter.api.Test;

import static org.squiddev.cobalt.lib.doubles.DoubleTestHelper.*;

public class IeeeTest {

	@Test
	public void uint64Conversions() {
		// Start by checking the byte-order.
		@Unsigned long ordered = 0x01234567_89ABCDEFL;
		CHECK_EQ(3512700564088504e-318, new Ieee.Double(ordered).value());

		@Unsigned long min_double64 = 0x00000000_00000001L;
		CHECK_EQ(5e-324, new Ieee.Double(min_double64).value());

		@Unsigned long max_double64 = 0x7fefffff_ffffffffL;
		CHECK_EQ(1.7976931348623157e308, new Ieee.Double(max_double64).value());
	}


	@Test
	public void uint32Conversions() {
		// Start by checking the byte-order.
		@Unsigned int ordered = 0x01234567;
		CHECK_EQ(2.9988165487136453e-38f, new Ieee.Single(ordered).value());

		@Unsigned int min_float32 = 0x00000001;
		CHECK_EQ(1.4e-45f, new Ieee.Single(min_float32).value());

		@Unsigned int max_float32 = 0x7f7fffff;
		CHECK_EQ(3.4028234e38f, new Ieee.Single(max_float32).value());
	}


	@Test
	public void double_AsDiyFp() {
		@Unsigned long ordered = 0x01234567_89ABCDEFL;
		DiyFp diy_fp = new Ieee.Double(ordered).asDiyFp();
		CHECK_EQ(0x12 - 0x3FF - 52, diy_fp.e());
		// The 52 mantissa bits, plus the implicit 1 in bit 52 as a UINT64.
		CHECK_EQ(0x00134567_89ABCDEFL, diy_fp.f());  // NOLINT

		@Unsigned long min_double64 = 0x00000000_00000001L;
		diy_fp = new Ieee.Double(min_double64).asDiyFp();
		CHECK_EQ(-0x3FF - 52 + 1, diy_fp.e());
		// This is a denormal; so no hidden bit.
		CHECK_EQ(1L, diy_fp.f());  // NOLINT

		@Unsigned long max_double64 = 0x7fefffff_ffffffffL;
		diy_fp = new Ieee.Double(max_double64).asDiyFp();
		CHECK_EQ(0x7FE - 0x3FF - 52, diy_fp.e());
		CHECK_EQ(0x001fffff_ffffffffL, diy_fp.f());  // NOLINT
	}


	@Test
	public void single_AsDiyFp() {
		@Unsigned int ordered = 0x01234567;
		DiyFp diy_fp = new Ieee.Single(ordered).asDiyFp();
		CHECK_EQ(0x2 - 0x7F - 23, diy_fp.e());
		// The 23 mantissa bits, plus the implicit 1 in bit 24 as a @Unsigned int.
		CHECK_EQ(0xA34567L, diy_fp.f());

		@Unsigned int min_float32 = 0x00000001;
		diy_fp = new Ieee.Single(min_float32).asDiyFp();
		CHECK_EQ(-0x7F - 23 + 1, diy_fp.e());
		// This is a denormal; so no hidden bit.
		CHECK_EQ(1L, diy_fp.f());

		@Unsigned int max_float32 = 0x7f7fffff;
		diy_fp = new Ieee.Single(max_float32).asDiyFp();
		CHECK_EQ(0xFE - 0x7F - 23, diy_fp.e());
		CHECK_EQ(0x00ffffffL, diy_fp.f());
	}


	@Test
	public void asNormalizedDiyFp() {
		@Unsigned long ordered = 0x01234567_89ABCDEFL;
		DiyFp diy_fp = new Ieee.Double(ordered).asNormalizedDiyFp();
		CHECK_EQ(0x12 - 0x3FF - 52 - 11, diy_fp.e());
		CHECK_EQ((0x00134567_89ABCDEFL << 11), diy_fp.f());  // NOLINT

		@Unsigned long min_double64 = 0x00000000_00000001L;
		diy_fp = new Ieee.Double(min_double64).asNormalizedDiyFp();
		CHECK_EQ(-0x3FF - 52 + 1 - 63, diy_fp.e());
		// This is a denormal; so no hidden bit.
		CHECK_EQ(0x80000000_00000000L, diy_fp.f());  // NOLINT

		@Unsigned long max_double64 = 0x7fefffff_ffffffffL;
		diy_fp = new Ieee.Double(max_double64).asNormalizedDiyFp();
		CHECK_EQ(0x7FE - 0x3FF - 52 - 11, diy_fp.e());
		CHECK_EQ((0x001fffff_ffffffffL << 11) ,
				diy_fp.f());  // NOLINT
	}


	@Test
	public void double_IsDenormal() {
		@Unsigned long min_double64 = 0x00000000_00000001L;
		CHECK(new Ieee.Double(min_double64).isDenormal());
		@Unsigned long bits = 0x000FFFFF_FFFFFFFFL;
		CHECK(new Ieee.Double(bits).isDenormal());
		bits = 0x00100000_00000000L;
		CHECK(!new Ieee.Double(bits).isDenormal());
	}


	@Test
	public void single_IsDenormal() {
		@Unsigned int min_float32 = 0x00000001;
		CHECK(new Ieee.Single(min_float32).isDenormal());
		@Unsigned int bits = 0x007FFFFF;
		CHECK(new Ieee.Single(bits).isDenormal());
		bits = 0x00800000;
		CHECK(!new Ieee.Single(bits).isDenormal());
	}


	@Test
	public void double_IsSpecial() {
		CHECK(new Ieee.Double(Ieee.Double.infinity()).isSpecial());
		CHECK(new Ieee.Double(-Ieee.Double.infinity()).isSpecial());
		CHECK(new Ieee.Double(Ieee.Double.nan()).isSpecial());
		@Unsigned long bits = 0xFFF12345_00000000L;
		CHECK(new Ieee.Double(bits).isSpecial());
		// Denormals are not special:
		CHECK(!new Ieee.Double(5e-324).isSpecial());
		CHECK(!new Ieee.Double(-5e-324).isSpecial());
		// And some random numbers:
		CHECK(!new Ieee.Double(0.0).isSpecial());
		CHECK(!new Ieee.Double(-0.0).isSpecial());
		CHECK(!new Ieee.Double(1.0).isSpecial());
		CHECK(!new Ieee.Double(-1.0).isSpecial());
		CHECK(!new Ieee.Double(1000000.0).isSpecial());
		CHECK(!new Ieee.Double(-1000000.0).isSpecial());
		CHECK(!new Ieee.Double(1e23).isSpecial());
		CHECK(!new Ieee.Double(-1e23).isSpecial());
		CHECK(!new Ieee.Double(1.7976931348623157e308).isSpecial());
		CHECK(!new Ieee.Double(-1.7976931348623157e308).isSpecial());
	}


	@Test
	public void single_IsSpecial() {
		CHECK(new Ieee.Single(Ieee.Single.infinity()).isSpecial());
		CHECK(new Ieee.Single(-Ieee.Single.infinity()).isSpecial());
		CHECK(new Ieee.Single(Ieee.Single.nan()).isSpecial());
		@Unsigned int bits = 0xFFF12345;
		CHECK(new Ieee.Single(bits).isSpecial());
		// Denormals are not special:
		CHECK(!new Ieee.Single(1.4e-45f).isSpecial());
		CHECK(!new Ieee.Single(-1.4e-45f).isSpecial());
		// And some random numbers:
		CHECK(!new Ieee.Single(0.0f).isSpecial());
		CHECK(!new Ieee.Single(-0.0f).isSpecial());
		CHECK(!new Ieee.Single(1.0f).isSpecial());
		CHECK(!new Ieee.Single(-1.0f).isSpecial());
		CHECK(!new Ieee.Single(1000000.0f).isSpecial());
		CHECK(!new Ieee.Single(-1000000.0f).isSpecial());
		CHECK(!new Ieee.Single(1e23f).isSpecial());
		CHECK(!new Ieee.Single(-1e23f).isSpecial());
		CHECK(!new Ieee.Single(1.18e-38f).isSpecial());
		CHECK(!new Ieee.Single(-1.18e-38f).isSpecial());
	}


	@Test
	public void double_IsInfinite() {
		CHECK(new Ieee.Double(Ieee.Double.infinity()).isInfinite());
		CHECK(new Ieee.Double(-Ieee.Double.infinity()).isInfinite());
		CHECK(!new Ieee.Double(Ieee.Double.nan()).isInfinite());
		CHECK(!new Ieee.Double(0.0).isInfinite());
		CHECK(!new Ieee.Double(-0.0).isInfinite());
		CHECK(!new Ieee.Double(1.0).isInfinite());
		CHECK(!new Ieee.Double(-1.0).isInfinite());
		@Unsigned long min_double64 = 0x00000000_00000001L;
		CHECK(!new Ieee.Double(min_double64).isInfinite());
	}


	@Test
	public void single_IsInfinite() {
		CHECK(new Ieee.Single(Ieee.Single.infinity()).isInfinite());
		CHECK(new Ieee.Single(-Ieee.Single.infinity()).isInfinite());
		CHECK(!new Ieee.Single(Ieee.Single.nan()).isInfinite());
		CHECK(!new Ieee.Single(0.0f).isInfinite());
		CHECK(!new Ieee.Single(-0.0f).isInfinite());
		CHECK(!new Ieee.Single(1.0f).isInfinite());
		CHECK(!new Ieee.Single(-1.0f).isInfinite());
		@Unsigned int min_float32 = 0x00000001;
		CHECK(!new Ieee.Single(min_float32).isInfinite());
	}


	@Test
	public void double_IsNan() {
		CHECK(new Ieee.Double(Ieee.Double.nan()).isNan());
		@Unsigned long other_nan = 0xFFFFFFFF_00000001L;
		CHECK(new Ieee.Double(other_nan).isNan());
		CHECK(!new Ieee.Double(Ieee.Double.infinity()).isNan());
		CHECK(!new Ieee.Double(-Ieee.Double.infinity()).isNan());
		CHECK(!new Ieee.Double(0.0).isNan());
		CHECK(!new Ieee.Double(-0.0).isNan());
		CHECK(!new Ieee.Double(1.0).isNan());
		CHECK(!new Ieee.Double(-1.0).isNan());
		@Unsigned long min_double64 = 0x00000000_00000001L;
		CHECK(!new Ieee.Double(min_double64).isNan());
	}


	@Test
	public void single_IsNan() {
		CHECK(new Ieee.Single(Ieee.Single.nan()).isNan());
		@Unsigned int other_nan = 0xFFFFF001;
		CHECK(new Ieee.Single(other_nan).isNan());
		CHECK(!new Ieee.Single(Ieee.Single.infinity()).isNan());
		CHECK(!new Ieee.Single(-Ieee.Single.infinity()).isNan());
		CHECK(!new Ieee.Single(0.0f).isNan());
		CHECK(!new Ieee.Single(-0.0f).isNan());
		CHECK(!new Ieee.Single(1.0f).isNan());
		CHECK(!new Ieee.Single(-1.0f).isNan());
		@Unsigned int min_float32 = 0x00000001;
		CHECK(!new Ieee.Single(min_float32).isNan());
	}


	@Test
	public void double_Sign() {
		CHECK_EQ(1, new Ieee.Double(1.0).sign());
		CHECK_EQ(1, new Ieee.Double(Ieee.Double.infinity()).sign());
		CHECK_EQ(-1, new Ieee.Double(-Ieee.Double.infinity()).sign());
		CHECK_EQ(1, new Ieee.Double(0.0).sign());
		CHECK_EQ(-1, new Ieee.Double(-0.0).sign());
		@Unsigned long min_double64 = 0x00000000_00000001L;
		CHECK_EQ(1, new Ieee.Double(min_double64).sign());
	}


	@Test
	public void single_Sign() {
		CHECK_EQ(1, new Ieee.Single(1.0f).sign());
		CHECK_EQ(1, new Ieee.Single(Ieee.Single.infinity()).sign());
		CHECK_EQ(-1, new Ieee.Single(-Ieee.Single.infinity()).sign());
		CHECK_EQ(1, new Ieee.Single(0.0f).sign());
		CHECK_EQ(-1, new Ieee.Single(-0.0f).sign());
		@Unsigned int min_float32 = 0x00000001;
		CHECK_EQ(1, new Ieee.Single(min_float32).sign());
	}


	@Test
	public void double_NormalizedBoundaries() {
		DiyFp[] boundary_plus = new DiyFp[1];
		DiyFp[] boundary_minus = new DiyFp[1];
		DiyFp diy_fp = new Ieee.Double(1.5).asNormalizedDiyFp();
		new Ieee.Double(1.5).normalizedBoundaries(boundary_minus, boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		// 1.5 does not have a significand of the form 2^p (for some p).
		// Therefore its boundaries are at the same distance.
		CHECK_EQ(diy_fp.f() - boundary_minus[0].f() , boundary_plus[0].f() - diy_fp.f());
		CHECK_EQ((1L << 10) , diy_fp.f() - boundary_minus[0].f());  // NOLINT

		diy_fp = new Ieee.Double(1.0).asNormalizedDiyFp();
		new Ieee.Double(1.0).normalizedBoundaries(boundary_minus, boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		// 1.0 does have a significand of the form 2^p (for some p).
		// Therefore its lower boundary is twice as close as the upper boundary.
		CHECK_GT(boundary_plus[0].f() - diy_fp.f() , diy_fp.f() - boundary_minus[0].f());
		CHECK_EQ((1L << 9) , diy_fp.f() - boundary_minus[0].f());  // NOLINT
		CHECK_EQ((1L << 10) , boundary_plus[0].f() - diy_fp.f());  // NOLINT

		@Unsigned long min_double64 = 0x00000000_00000001L;
		diy_fp = new Ieee.Double(min_double64).asNormalizedDiyFp();
		new Ieee.Double(min_double64).normalizedBoundaries(boundary_minus, boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		// min-value does not have a significand of the form 2^p (for some p).
		// Therefore its boundaries are at the same distance.
		CHECK_EQ(diy_fp.f() - boundary_minus[0].f() , boundary_plus[0].f() - diy_fp.f());
		// Denormals have their boundaries much closer.
		CHECK_EQ((1L << 62) ,
				diy_fp.f() - boundary_minus[0].f());  // NOLINT

		@Unsigned long smallest_normal64 = 0x00100000_00000000L;
		diy_fp = new Ieee.Double(smallest_normal64).asNormalizedDiyFp();
		new Ieee.Double(smallest_normal64).normalizedBoundaries(boundary_minus,
                                                 boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		// Even though the significand is of the form 2^p (for some p), its boundaries
		// are at the same distance. (This is the only exception).
		CHECK_EQ(diy_fp.f() - boundary_minus[0].f() , boundary_plus[0].f() - diy_fp.f());
		CHECK_EQ((1L << 10) , diy_fp.f() - boundary_minus[0].f());  // NOLINT

		@Unsigned long largest_denormal64 = 0x000FFFFF_FFFFFFFFL;
		diy_fp = new Ieee.Double(largest_denormal64).asNormalizedDiyFp();
		new Ieee.Double(largest_denormal64).normalizedBoundaries(boundary_minus,
                                                  boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		CHECK_EQ(diy_fp.f() - boundary_minus[0].f() , boundary_plus[0].f() - diy_fp.f());
		CHECK_EQ((1L << 11) , diy_fp.f() - boundary_minus[0].f());  // NOLINT

		@Unsigned long max_double64 = 0x7fefffff_ffffffffL;
		diy_fp = new Ieee.Double(max_double64).asNormalizedDiyFp();
		new Ieee.Double(max_double64).normalizedBoundaries(boundary_minus, boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		// max-value does not have a significand of the form 2^p (for some p).
		// Therefore its boundaries are at the same distance.
		CHECK_EQ(diy_fp.f() - boundary_minus[0].f() , boundary_plus[0].f() - diy_fp.f());
		CHECK_EQ((1L << 10) , diy_fp.f() - boundary_minus[0].f());  // NOLINT
	}


	@Test
	public void single_NormalizedBoundaries() {
		@Unsigned long kOne64 = 1L;
		DiyFp[] boundary_plus = new DiyFp[1];
		DiyFp[] boundary_minus = new DiyFp[1];
		DiyFp diy_fp = new Ieee.Single(1.5f).asDiyFp();
		diy_fp.normalize();
		new Ieee.Single(1.5f).normalizedBoundaries(boundary_minus, boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		// 1.5 does not have a significand of the form 2^p (for some p).
		// Therefore its boundaries are at the same distance.
		CHECK_EQ(diy_fp.f() - boundary_minus[0].f() , boundary_plus[0].f() - diy_fp.f());
		// Normalization shifts the significand by 8 bits. Add 32 bits for the bigger
		// data-type, and remove 1 because boundaries are at half a ULP.
		CHECK_EQ((kOne64 << 39) , diy_fp.f() - boundary_minus[0].f());

		diy_fp = new Ieee.Single(1.0f).asDiyFp();
		diy_fp.normalize();
		new Ieee.Single(1.0f).normalizedBoundaries(boundary_minus, boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		// 1.0 does have a significand of the form 2^p (for some p).
		// Therefore its lower boundary is twice as close as the upper boundary.
		CHECK_GT(boundary_plus[0].f() - diy_fp.f() , diy_fp.f() - boundary_minus[0].f());
		CHECK_EQ((kOne64 << 38) , diy_fp.f() - boundary_minus[0].f());  // NOLINT
		CHECK_EQ((kOne64 << 39) , boundary_plus[0].f() - diy_fp.f());  // NOLINT

		@Unsigned int min_float32 = 0x00000001;
		diy_fp = new Ieee.Single(min_float32).asDiyFp();
		diy_fp.normalize();
		new Ieee.Single(min_float32).normalizedBoundaries(boundary_minus, boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		// min-value does not have a significand of the form 2^p (for some p).
		// Therefore its boundaries are at the same distance.
		CHECK_EQ(diy_fp.f() - boundary_minus[0].f() , boundary_plus[0].f() - diy_fp.f());
		// Denormals have their boundaries much closer.
		CHECK_EQ((kOne64 << 62) , diy_fp.f() - boundary_minus[0].f());  // NOLINT

		@Unsigned int smallest_normal32 = 0x00800000;
		diy_fp = new Ieee.Single(smallest_normal32).asDiyFp();
		diy_fp.normalize();
		new Ieee.Single(smallest_normal32).normalizedBoundaries(boundary_minus,
                                                 boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		// Even though the significand is of the form 2^p (for some p), its boundaries
		// are at the same distance. (This is the only exception).
		CHECK_EQ(diy_fp.f() - boundary_minus[0].f() , boundary_plus[0].f() - diy_fp.f());
		CHECK_EQ((kOne64 << 39) , diy_fp.f() - boundary_minus[0].f());  // NOLINT

		@Unsigned int largest_denormal32 = 0x007FFFFF;
		diy_fp = new Ieee.Single(largest_denormal32).asDiyFp();
		diy_fp.normalize();
		new Ieee.Single(largest_denormal32).normalizedBoundaries(boundary_minus,
                                                  boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		CHECK_EQ(diy_fp.f() - boundary_minus[0].f() , boundary_plus[0].f() - diy_fp.f());
		CHECK_EQ((kOne64 << 40) , diy_fp.f() - boundary_minus[0].f());  // NOLINT

		@Unsigned int max_float32 = 0x7f7fffff;
		diy_fp = new Ieee.Single(max_float32).asDiyFp();
		diy_fp.normalize();
		new Ieee.Single(max_float32).normalizedBoundaries(boundary_minus, boundary_plus);
		CHECK_EQ(diy_fp.e(), boundary_minus[0].e());
		CHECK_EQ(diy_fp.e(), boundary_plus[0].e());
		// max-value does not have a significand of the form 2^p (for some p).
		// Therefore its boundaries are at the same distance.
		CHECK_EQ(diy_fp.f() - boundary_minus[0].f() , boundary_plus[0].f() - diy_fp.f());
		CHECK_EQ((kOne64 << 39) , diy_fp.f() - boundary_minus[0].f());  // NOLINT
	}


	@Test
	public void nextDouble() {
		CHECK_EQ(4e-324, new Ieee.Double(0.0).nextDouble());
		CHECK_EQ(0.0, new Ieee.Double(-0.0).nextDouble());
		CHECK_EQ(-0.0, new Ieee.Double(-4e-324).nextDouble());
		CHECK_GT(new Ieee.Double(new Ieee.Double(-0.0).nextDouble()).sign() , 0);
		CHECK(new Ieee.Double(new Ieee.Double(-4e-324).nextDouble()).sign() < 0);
		Ieee.Double d0 = new Ieee.Double(-4e-324);
		Ieee.Double d1 = new Ieee.Double(d0.nextDouble());
		Ieee.Double d2 = new Ieee.Double(d1.nextDouble());
		CHECK_EQ(-0.0, d1.value());
		CHECK(d1.sign() < 0);
		CHECK_EQ(0.0, d2.value());
		CHECK(d2.sign() > 0);
		CHECK_EQ(4e-324, d2.nextDouble());
		CHECK_EQ(-1.7976931348623157e308, new Ieee.Double(-Ieee.Double.infinity()).nextDouble());
		CHECK_EQ(Ieee.Double.infinity(),
				new Ieee.Double(0x7fefffff_ffffffffL).nextDouble());
	}


	@Test
	public void previousDouble() {
		CHECK_EQ(0.0, new Ieee.Double(4e-324).previousDouble());
		CHECK_EQ(-0.0, new Ieee.Double(0.0).previousDouble());
		CHECK(new Ieee.Double(new Ieee.Double(0.0).previousDouble()).sign() < 0);
		CHECK_EQ(-4e-324, new Ieee.Double(-0.0).previousDouble());
		Ieee.Double d0 = new Ieee.Double(4e-324);
		Ieee.Double d1 = new Ieee.Double(d0.previousDouble());
		Ieee.Double d2 = new Ieee.Double(d1.previousDouble());
		CHECK_EQ(0.0, d1.value());
		CHECK(d1.sign() > 0);
		CHECK_EQ(-0.0, d2.value());
		CHECK(d2.sign() < 0);
		CHECK_EQ(-4e-324, d2.previousDouble());
		CHECK_EQ(1.7976931348623157e308, new Ieee.Double(Ieee.Double.infinity()).previousDouble());
		CHECK_EQ(-Ieee.Double.infinity(),
				new Ieee.Double(0xffefffff_ffffffffL).previousDouble());
	}

	@Test
	public void signalingNan() {
		Ieee.Double nan = new Ieee.Double(Ieee.Double.nan());
		CHECK(nan.isNan());
		CHECK(nan.isQuietNan());

		nan = new Ieee.Double(Double.NaN);
		CHECK(nan.isNan());
		CHECK(nan.isQuietNan());


		// TODO does java standard API have this?
//		CHECK(new Ieee.Double(std::numeric_limits<double>::quiet_NaN()).isQuietNan());
//		CHECK(new Ieee.Double(std::numeric_limits<double>::signaling_NaN()).isSignalingNan());
	}

	@Test
	public void signalingNanSingle() {
		Ieee.Single nan = new Ieee.Single(Ieee.Single.nan());
		CHECK(nan.isNan());
		CHECK(nan.isQuietNan());

		//CHECK(new Ieee.Single(std::numeric_limits<float>::quiet_NaN()).isQuietNan());
		//CHECK(new Ieee.Single(std::numeric_limits<float>::signaling_NaN()).isSignalingNan());
	}

}
