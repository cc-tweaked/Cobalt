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

package org.squiddev.cobalt.lib.doubles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.lib.doubles.FixedDtoa.UInt128;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UInt128Test {

	@BeforeAll
	static void initAll() {
		Assert.setEnabled(true);
	}

	@Test
	public void shiftOps() {
		UInt128 val = new UInt128(0L, 0xffffL);
		assertEquals(0, val.shift(0).rawHigh());
		assertEquals(0xffffL, val.shift(0).rawLow());
		assertEquals( 0xffffL, val.shift(-64).rawHigh());
		assertEquals(0, val.shift(-64).rawLow());
		val = new UInt128(0xffffL, 0L);
		assertEquals(0,  val.shift(64).rawHigh());
		assertEquals(0xffffL, val.shift(64).rawLow());
		val = new UInt128(0L, 0xffff_0000_0000_0000L);
		assertEquals(0x00ffL, val.shift(-8).rawHigh());
		assertEquals(0xff00_0000_0000_0000L, val.shift(-8).rawLow());
		val = new UInt128(0xffffL, 0L);
		assertEquals(0x00ffL, val.shift(8).rawHigh());
		assertEquals(0xff00_0000_0000_0000L, val.shift(8).rawLow());
	}

	@Test
	public void divMod() {
		UInt128 val = new UInt128(0L, 17L);
		UInt128.QuotientRemainder qr = val.divModPowerOf2(3);
		assertEquals(0, qr.remainder.rawHigh());
		assertEquals(17 % (1<<3), qr.remainder.rawLow());
		assertEquals(17 / (1<<3), qr.quotient);

		val = new UInt128(0xffL, 0xffL);
		qr = val.divModPowerOf2(68);
		assertEquals(0xf, qr.quotient);
		assertEquals(0xff, qr.remainder.rawLow());
		assertEquals(0xf, qr.remainder.rawHigh());
	}

	@Test
	public void times() {
		UInt128 val = new UInt128(0L, 5L);
		assertEquals(50, val.times(10).rawLow());
		val = new UInt128(0L, 0xffff_ffff_ffff_ffffL);
		UInt128 product = val.times(16);
		assertEquals(0xffff_ffff_ffff_fff0L, product.rawLow());
		assertEquals(0xf, product.rawHigh());
	}

}
