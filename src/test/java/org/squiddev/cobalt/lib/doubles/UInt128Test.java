/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 sir-maniac
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt.lib.doubles;

import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.lib.doubles.FixedDtoa.UInt128;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UInt128Test {

	@Test
	public void shiftOps() {
		UInt128 val = new UInt128(UnsignedLong.ZERO, UnsignedLong.uValueOf(0xffffL));
		assertEquals(0, val.shift(0).rawHigh());
		assertEquals(0xffffL, val.shift(0).rawLow());
		assertEquals( 0xffffL, val.shift(-64).rawHigh());
		assertEquals(0, val.shift(-64).rawLow());
		val = new UInt128(UnsignedLong.uValueOf(0xffffL), UnsignedLong.ZERO);
		assertEquals(0,  val.shift(64).rawHigh());
		assertEquals(0xffffL, val.shift(64).rawLow());
		val = new UInt128(UnsignedLong.ZERO, UnsignedLong.uValueOf(0xffff_0000_0000_0000L));
		assertEquals(0x00ffL, val.shift(-8).rawHigh());
		assertEquals(0xff00_0000_0000_0000L, val.shift(-8).rawLow());
		val = new UInt128(UnsignedLong.uValueOf(0xffffL), UnsignedLong.ZERO);
		assertEquals(0x00ffL, val.shift(8).rawHigh());
		assertEquals(0xff00_0000_0000_0000L, val.shift(8).rawLow());
	}

	@Test
	public void divMod() {
		UInt128 val = new UInt128(UnsignedLong.ZERO, UnsignedLong.valueOf(17));
		UInt128.QuotientRemainder qr = val.divModPowerOf2(3);
		assertEquals(0, qr.remainder.rawHigh());
		assertEquals(17 % (1<<3), qr.remainder.rawLow());
		assertEquals(17 / (1<<3), qr.quotient);

		val = new UInt128(UnsignedLong.uValueOf(0xff), UnsignedLong.valueOf(0xff));
		qr = val.divModPowerOf2(68);
		assertEquals(0xf, qr.quotient);
		assertEquals(0xff, qr.remainder.rawLow());
		assertEquals(0xf, qr.remainder.rawHigh());
	}

	@Test
	public void times() {
		UInt128 val = new UInt128(UnsignedLong.ZERO, UnsignedLong.valueOf(5));
		assertEquals(50, val.times(10).rawLow());
		val = new UInt128(UnsignedLong.ZERO, UnsignedLong.uValueOf(0xffff_ffff_ffff_ffffL));
		UInt128 product = val.times(16);
		assertEquals(0xffff_ffff_ffff_fff0L, product.rawLow());
		assertEquals(0xf, product.rawHigh());
	}

}
