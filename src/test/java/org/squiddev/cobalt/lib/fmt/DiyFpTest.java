/*
 *  Copyright 2006-2008 the V8 project authors. All rights reserved.
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
package org.squiddev.cobalt.lib.fmt;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class DiyFpTest {

	@Test
	public void subtract() {
		DiyFp diy_fp1 = new DiyFp(UnsignedLong.valueOf(3), 0);
		DiyFp diy_fp2 = new DiyFp(UnsignedLong.ONE, 0);
		DiyFp diff = DiyFp.Minus(diy_fp1, diy_fp2);

		assertEquals(2, diff.f().longValueExact());  // NOLINT
		assertEquals(0, diff.e());
		diy_fp1.Subtract(diy_fp2);
		assertEquals(2, diy_fp1.f().longValueExact());  // NOLINT
		assertEquals(0, diy_fp1.e());
	}

	@Test
	public void multiply() {
		DiyFp diy_fp1 = new DiyFp(UnsignedLong.valueOf(3), 0);
		DiyFp diy_fp2 = new DiyFp(UnsignedLong.valueOf(2), 0);
		DiyFp product = DiyFp.Times(diy_fp1, diy_fp2);

		assertEquals(0, product.f().longValueExact());  // NOLINT
		assertEquals(64, product.e());
		diy_fp1.Multiply(diy_fp2);
		assertEquals(0, diy_fp1.f().longValueExact());  // NOLINT
		assertEquals(64, diy_fp1.e());

		diy_fp1 = new DiyFp(UnsignedLong.uValueOf(0x80000000_00000000L), 11);
		diy_fp2 = new DiyFp(UnsignedLong.valueOf(2), 13);
		product = DiyFp.Times(diy_fp1, diy_fp2);
		assertEquals(1, product.f().longValueExact());  // NOLINT
		assertEquals(11 + 13 + 64, product.e());

		// Test rounding.
		diy_fp1 = new DiyFp(UnsignedLong.uValueOf(0x80000000_00000001L), 11);
		diy_fp2 = new DiyFp(UnsignedLong.valueOf(1), 13);
		product = DiyFp.Times(diy_fp1, diy_fp2);
		assertEquals(1, product.f().longValueExact());  // NOLINT
		assertEquals(11 + 13 + 64, product.e());

		diy_fp1 = new DiyFp(UnsignedLong.uValueOf(0x7fffffff_ffffffffL), 11);
		diy_fp2 = new DiyFp(UnsignedLong.ONE, 13);
		product = DiyFp.Times(diy_fp1, diy_fp2);
		assertEquals(0, product.f().longValueExact());  // NOLINT
		assertEquals(11 + 13 + 64, product.e());

		// Halfway cases are allowed to round either way. So don't check for it.

		// Big numbers.
		diy_fp1 = new DiyFp(UnsignedLong.uValueOf(0xFFFFFFFF_FFFFFFFFL), 11);
		diy_fp2 = new DiyFp(UnsignedLong.uValueOf(0xFFFFFFFF_FFFFFFFFL), 13);
		// 128bit result: 0xfffffffffffffffe0000000000000001
		product = DiyFp.Times(diy_fp1, diy_fp2);
		assertEquals(UnsignedLong.uValueOf(0xFFFFFFFF_FFFFFFFeL), product.f());
		assertEquals(11 + 13 + 64, product.e());
	}


}
