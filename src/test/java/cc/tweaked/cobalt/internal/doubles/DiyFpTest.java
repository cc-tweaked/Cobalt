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

import org.junit.jupiter.api.Test;

import static cc.tweaked.cobalt.internal.doubles.DoubleTestHelper.CHECK_EQ;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiyFpTest {
	@Test
	public void subtract() {
		DiyFp diy_fp1 = new DiyFp(3L, 0);
		DiyFp diy_fp2 = new DiyFp(1L, 0);
		DiyFp diff = DiyFp.minus(diy_fp1, diy_fp2);

		CHECK_EQ(2, diff.significand());  // NOLINT
		CHECK_EQ(0, diff.exponent());
		diy_fp1 = DiyFp.minus(diy_fp1, diy_fp2);
		CHECK_EQ(2, diy_fp1.significand());  // NOLINT
		CHECK_EQ(0, diy_fp1.exponent());
	}

	@Test
	public void multiply() {
		DiyFp diy_fp1 = new DiyFp(3L, 0);
		DiyFp diy_fp2 = new DiyFp(2L, 0);
		DiyFp product = DiyFp.times(diy_fp1, diy_fp2);

		CHECK_EQ(0, product.significand());  // NOLINT
		assertEquals(64, product.exponent());
		diy_fp1 = DiyFp.times(diy_fp1, diy_fp2);
		CHECK_EQ(0, diy_fp1.significand());  // NOLINT
		CHECK_EQ(64, diy_fp1.exponent());

		diy_fp1 = new DiyFp(0x80000000_00000000L, 11);
		diy_fp2 = new DiyFp(2L, 13);
		product = DiyFp.times(diy_fp1, diy_fp2);
		CHECK_EQ(1, product.significand());  // NOLINT
		CHECK_EQ(11 + 13 + 64, product.exponent());

		// Test rounding.
		diy_fp1 = new DiyFp(0x80000000_00000001L, 11);
		diy_fp2 = new DiyFp(1L, 13);
		product = DiyFp.times(diy_fp1, diy_fp2);
		CHECK_EQ(1, product.significand());  // NOLINT
		CHECK_EQ(11 + 13 + 64, product.exponent());

		diy_fp1 = new DiyFp(0x7fffffff_ffffffffL, 11);
		diy_fp2 = new DiyFp(1L, 13);
		product = DiyFp.times(diy_fp1, diy_fp2);
		CHECK_EQ(0, product.significand());  // NOLINT
		CHECK_EQ(11 + 13 + 64, product.exponent());

		// Halfway cases are allowed to round either way. So don't check for it.

		// Big numbers.
		diy_fp1 = new DiyFp(0xFFFFFFFF_FFFFFFFFL, 11);
		diy_fp2 = new DiyFp(0xFFFFFFFF_FFFFFFFFL, 13);
		// 128bit result: 0xfffffffffffffffe0000000000000001
		product = DiyFp.times(diy_fp1, diy_fp2);
		CHECK_EQ(0xFFFFFFFF_FFFFFFFeL, product.significand());
		CHECK_EQ(11 + 13 + 64, product.exponent());
	}


}
