/*
 * Copyright 2021 sir-maniac. All Rights reserved.
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

import cc.tweaked.cobalt.internal.doubles.FixedDtoa.UInt128;
import org.junit.jupiter.api.Test;

import static cc.tweaked.cobalt.internal.doubles.DoubleTestHelper.CHECK_EQ;

class UInt128Test {
	@Test
	public void shiftOps() {
		UInt128 val = new UInt128(0L, 0xffffL);
		CHECK_EQ(0, val.shift(0).rawHigh());
		CHECK_EQ(0xffffL, val.shift(0).rawLow());
		CHECK_EQ(0xffffL, val.shift(-64).rawHigh());
		CHECK_EQ(0, val.shift(-64).rawLow());
		val = new UInt128(0xffffL, 0L);
		CHECK_EQ(0, val.shift(64).rawHigh());
		CHECK_EQ(0xffffL, val.shift(64).rawLow());
		val = new UInt128(0L, 0xffff_0000_0000_0000L);
		CHECK_EQ(0x00ffL, val.shift(-8).rawHigh());
		CHECK_EQ(0xff00_0000_0000_0000L, val.shift(-8).rawLow());
		val = new UInt128(0xffffL, 0L);
		CHECK_EQ(0x00ffL, val.shift(8).rawHigh());
		CHECK_EQ(0xff00_0000_0000_0000L, val.shift(8).rawLow());
	}

	@Test
	public void divMod() {
		UInt128 val = new UInt128(0L, 17L);
		UInt128.QuotientRemainder qr = val.divModPowerOf2(3);
		CHECK_EQ(0, qr.remainder.rawHigh());
		CHECK_EQ(17 % (1 << 3), qr.remainder.rawLow());
		CHECK_EQ(17 / (1 << 3), qr.quotient);

		val = new UInt128(0xffL, 0xffL);
		qr = val.divModPowerOf2(68);
		CHECK_EQ(0xf, qr.quotient);
		CHECK_EQ(0xff, qr.remainder.rawLow());
		CHECK_EQ(0xf, qr.remainder.rawHigh());
	}

	@Test
	public void times() {
		UInt128 val = new UInt128(0L, 5L);
		CHECK_EQ(50, val.times(10).rawLow());
		val = new UInt128(0L, 0xffff_ffff_ffff_ffffL);
		UInt128 product = val.times(16);
		CHECK_EQ(0xffff_ffff_ffff_fff0L, product.rawLow());
		CHECK_EQ(0xf, product.rawHigh());
	}

}
