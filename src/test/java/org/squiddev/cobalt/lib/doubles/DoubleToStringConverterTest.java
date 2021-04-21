/*
 *  Copyright 2021 sir-maniac. All Rights reserved.
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

import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.lib.doubles.DoubleToStringConverter.Flags;
import org.squiddev.cobalt.lib.doubles.DoubleToStringConverter.PrecisionPolicy;
import org.squiddev.cobalt.lib.doubles.DoubleToStringConverter.ShortestPolicy;
import org.squiddev.cobalt.lib.doubles.DoubleToStringConverter.Symbols;

import static org.junit.jupiter.api.Assertions.*;

class DoubleToStringConverterTest {
	private DoubleToStringConverter conv;
	private StringBuilder sb = new StringBuilder();

	@Test
	void toShortest() {
		conv = DoubleToStringConverter.ecmaScriptConverter();

		assertShortest("0.000001", 0.000001);
		assertShortest("1e-7", 0.0000001);
		assertShortest("111111111111111110000", 111111111111111111111.0);
		assertShortest("100000000000000000000", 100000000000000000000.0);
		assertShortest("1.1111111111111111e+21", 1111111111111111111111.0);
	}

	@Test
	void toFixed() {
		conv = DoubleToStringConverter.ecmaScriptConverter();

		assertFixed("3.1", 3.12, 1);
		assertFixed("3.142", 3.1415, 3);
		assertFixed("1234.5679", 1234.56789, 4);
		assertFixed("1.23000", 1.23, 5);
		assertFixed("0.1000", 0.1, 4);
		assertFixed("1000000000000000019884624838656.00", 1e30, 2);
		assertFixed("0.100000000000000005551115123126", 0.1, 30);
		assertFixed("0.10000000000000001", 0.1, 17);

		conv = newConv(Flags.NO_FLAGS);
		assertFixed("123", 123.45, 0);
		assertFixed("1", 0.678, 0);

		conv = newConv(Flags.EMIT_TRAILING_DECIMAL_POINT);
		assertFixed("123.", 123.45, 0);
		assertFixed("1.", 0.678, 0);

		conv = newConv(Flags.EMIT_TRAILING_DECIMAL_POINT | Flags.EMIT_TRAILING_ZERO_AFTER_POINT);
		assertFixed("123.0", 123.45, 0);
		assertFixed("1.0", 0.678, 0);
	}

	@Test
	void toExponential() {
		conv = newConv(0);

		assertExp("3.1e0",    3.12, 1);
		assertExp("5.000e0",  5.0, 3);
		assertExp("1.00e-3",  0.001, 2);
		assertExp("3.1415e0", 3.1415, -1);
		assertExp("3.1415e0", 3.1415, 4);
		assertExp("3.142e0",  3.1415, 3);
		assertExp("1.235e14", 123456789000000.0, 3);
		assertExp("1e30", 1000000000000000019884624838656.0, -1);
		assertExp("1.00000000000000001988462483865600e30",
				1000000000000000019884624838656.0, 32);
		assertExp("1e3", 1234, 0);
	}

	@Test
	void toPrecision() {
		int maxLeadingZeros = 6;
		int maxTrailingZeros = 0;
		conv = newConvPrec(0, maxLeadingZeros, maxTrailingZeros);

		assertPrec("0.0000012", 0.0000012345,  2);
		assertPrec("1.2e-7",    0.00000012345, 2);

		/// EMIT_TRAILING_ZERO_AFTER_POINT is counted toward the maxTrailingZeros limit
		maxTrailingZeros = 1;
		conv = newConvPrec(0, maxLeadingZeros, maxTrailingZeros);
		assertPrec("230", 230.0, 2);
		conv = newConvPrec(Flags.EMIT_TRAILING_DECIMAL_POINT, maxLeadingZeros, maxTrailingZeros);
		assertPrec("230.", 230.0, 2);
		conv = newConvPrec(Flags.EMIT_TRAILING_DECIMAL_POINT | Flags.EMIT_TRAILING_ZERO_AFTER_POINT,
				maxLeadingZeros, maxTrailingZeros);
		assertPrec("2.3e2", 230.0, 2);

		maxTrailingZeros = 3;
		conv = newConvPrec(0, maxLeadingZeros, maxTrailingZeros);
		assertPrec("123450", 123450.0, 6);
		assertPrec("123450", 123450.0, 5);
		assertPrec("123500", 123450.0, 4);
		assertPrec("123000", 123450.0, 3);
		assertPrec("1.2e5", 123450.0, 2);
	}

	private void assertPrec(String expected, double val, int requestedDigits) {
		sb.setLength(0);
		conv.toPrecision(val, requestedDigits, sb);
		assertEquals(expected, sb.toString());
	}

	private void assertExp(String expected, double val, int requestedDigits) {
		sb.setLength(0);
		conv.toExponential(val, requestedDigits, sb);
		assertEquals(expected, sb.toString());
	}

	private void assertFixed(String expected, double val, int requestedDigits) {
		sb.setLength(0);
		conv.toFixed(val, requestedDigits, sb);
		assertEquals(expected, sb.toString());
	}

	private void assertShortest(String expected, double val) {
		sb.setLength(0);
		conv.toShortest(val, sb);
		assertEquals(expected, sb.toString());
	}

	private DoubleToStringConverter newConvPrec(int flags, int maxLeadingZeros, int maxTrailingZeros) {
		return new DoubleToStringConverter(flags,
				new Symbols("Infinity", "NaN", 'e'),
				new ShortestPolicy(-6, 21),
				new PrecisionPolicy(maxLeadingZeros, maxTrailingZeros));
	}

	private DoubleToStringConverter newConv(int flags) {
		return new DoubleToStringConverter(flags,
			   new Symbols("Infinity", "NaN", 'e'),
			   new ShortestPolicy(-6, 21),
			   new PrecisionPolicy(6, 0));
	}
}
