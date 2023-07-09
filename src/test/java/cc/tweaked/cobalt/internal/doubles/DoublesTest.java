/*
 * Copyright 2006-2008 the V8 project authors. All rights reserved.
 * Java Port Copyright 2021 sir-maniac. All Rights reserved. *
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
import org.junit.jupiter.api.Test;

import static cc.tweaked.cobalt.internal.doubles.DoubleTestHelper.CHECK;
import static cc.tweaked.cobalt.internal.doubles.DoubleTestHelper.CHECK_EQ;

public class DoublesTest {
	@Test
	public void uint64Conversions() {
		// Start by checking the byte-order.
		@Unsigned long ordered = 0x01234567_89ABCDEFL;
		CHECK_EQ(3512700564088504e-318, Double.longBitsToDouble(ordered));

		@Unsigned long min_double64 = 0x00000000_00000001L;
		CHECK_EQ(5e-324, Double.longBitsToDouble(min_double64));

		@Unsigned long max_double64 = 0x7fefffff_ffffffffL;
		CHECK_EQ(1.7976931348623157e308, Double.longBitsToDouble(max_double64));
	}

	@Test
	public void asNormalizedDiyFp() {
		@Unsigned long ordered = 0x01234567_89ABCDEFL;
		DiyFp diy_fp = Doubles.asNormalizedDiyFp(Double.longBitsToDouble(ordered));
		CHECK_EQ(0x12 - 0x3FF - 52 - 11, diy_fp.exponent());
		CHECK_EQ((0x00134567_89ABCDEFL << 11), diy_fp.significand());  // NOLINT

		@Unsigned long min_double64 = 0x00000000_00000001L;
		diy_fp = Doubles.asNormalizedDiyFp(Double.longBitsToDouble(min_double64));
		CHECK_EQ(-0x3FF - 52 + 1 - 63, diy_fp.exponent());
		// This is a denormal; so no hidden bit.
		CHECK_EQ(0x80000000_00000000L, diy_fp.significand());  // NOLINT

		@Unsigned long max_double64 = 0x7fefffff_ffffffffL;
		diy_fp = Doubles.asNormalizedDiyFp(Double.longBitsToDouble(max_double64));
		CHECK_EQ(0x7FE - 0x3FF - 52 - 11, diy_fp.exponent());
		CHECK_EQ((0x001fffff_ffffffffL << 11), diy_fp.significand());  // NOLINT
	}

	@Test
	public void double_IsDenormal() {
		@Unsigned long min_double64 = 0x00000000_00000001L;
		CHECK(Doubles.isDenormal(min_double64));
		@Unsigned long bits = 0x000FFFFF_FFFFFFFFL;
		CHECK(Doubles.isDenormal(bits));
		bits = 0x00100000_00000000L;
		CHECK(!Doubles.isDenormal(bits));
	}

	@Test
	public void double_IsSpecial() {
		CHECK(Doubles.isSpecial(Double.POSITIVE_INFINITY));
		CHECK(Doubles.isSpecial(-Double.POSITIVE_INFINITY));
		CHECK(Doubles.isSpecial(Double.NaN));
		@Unsigned long bits = 0xFFF12345_00000000L;
		CHECK(Doubles.isSpecial(bits));
		// Denormals are not special:
		CHECK(!Doubles.isSpecial(5e-324));
		CHECK(!Doubles.isSpecial(-5e-324));
		// And some random numbers:
		CHECK(!Doubles.isSpecial(0.0));
		CHECK(!Doubles.isSpecial(-0.0));
		CHECK(!Doubles.isSpecial(1.0));
		CHECK(!Doubles.isSpecial(-1.0));
		CHECK(!Doubles.isSpecial(1000000.0));
		CHECK(!Doubles.isSpecial(-1000000.0));
		CHECK(!Doubles.isSpecial(1e23));
		CHECK(!Doubles.isSpecial(-1e23));
		CHECK(!Doubles.isSpecial(1.7976931348623157e308));
		CHECK(!Doubles.isSpecial(-1.7976931348623157e308));
	}

	@Test
	public void double_Sign() {
		CHECK_EQ(1, Doubles.sign(1.0));
		CHECK_EQ(1, Doubles.sign(Double.POSITIVE_INFINITY));
		CHECK_EQ(-1, Doubles.sign(-Double.POSITIVE_INFINITY));
		CHECK_EQ(1, Doubles.sign(0.0));
		CHECK_EQ(-1, Doubles.sign(-0.0));
		@Unsigned long min_double64 = 0x00000000_00000001L;
		CHECK_EQ(1, Doubles.sign(min_double64));
	}
}
