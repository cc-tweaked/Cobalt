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
import org.squiddev.cobalt.lib.doubles.DoubleToStringConverter.*;

import static org.junit.jupiter.api.Assertions.*;

class DoubleToStringConverterTest {
	private static final Symbols SYMBOLS = new Symbols("Infinity", "NaN", 'e');
	private static final FormatOptions FORMAT_OPTIONS =
			new FormatOptions(SYMBOLS, false, false, false, -1, false, false);
	private DoubleToStringConverter conv;
	private final StringAppendable appendable = new StringAppendable();

	@Test
	void toShortest() {
		conv = DoubleToStringConverter.ecmaScriptConverter();

		testShortest("0.000001", 0.000001);
		testShortest("1e-7", 0.0000001);
		testShortest("111111111111111110000", 111111111111111111111.0);
		testShortest("100000000000000000000", 100000000000000000000.0);
		testShortest("1.1111111111111111e+21", 1111111111111111111111.0);
	}

	@Test
	void toFixed() {
		conv = DoubleToStringConverter.ecmaScriptConverter();

		testFixed("3.1", 3.12, 1);
		testFixed("3.142", 3.1415, 3);
		testFixed("1234.5679", 1234.56789, 4);
		testFixed("1.23000", 1.23, 5);
		testFixed("0.1000", 0.1, 4);
		testFixed("1000000000000000019884624838656.00", 1e30, 2);
		testFixed("0.100000000000000005551115123126", 0.1, 30);
		testFixed("0.10000000000000001", 0.1, 17);

		conv = newConv(Flags.NO_FLAGS);
		testFixed("123", 123.45, 0);
		testFixed("1", 0.678, 0);

		conv = newConv(Flags.EMIT_TRAILING_DECIMAL_POINT);
		testFixed("123.", 123.45, 0);
		testFixed("1.", 0.678, 0);

		conv = newConv(Flags.EMIT_TRAILING_DECIMAL_POINT | Flags.EMIT_TRAILING_ZERO_AFTER_POINT);
		testFixed("123.0", 123.45, 0);
		testFixed("1.0", 0.678, 0);

		// from string-issues.lua:3
		conv = newConv(0);
		testFixed("20.00", 20.0, 2, 5); // %5.2f
		testFixed(" 0.05", 5.2e-2, 2, 5); // %5.2f
		testFixed("52.30", 52.3, 2, 5); // %5.2f

	}

	@Test
	void toExponential() {
		conv = newConv(0);

		testExp("3.1e0",    3.12, 1);
		testExp("5.000e0",  5.0, 3);
		testExp("1.00e-3",  0.001, 2);
		testExp("3.1415e0", 3.1415, -1);
		testExp("3.1415e0", 3.1415, 4);
		testExp("3.142e0",  3.1415, 3);
		testExp("1.235e14", 123456789000000.0, 3);
		testExp("1e30", 1000000000000000019884624838656.0, -1);
		testExp("1.00000000000000001988462483865600e30",
				1000000000000000019884624838656.0, 32);
		testExp("1e3", 1234, 0);

		conv = newConv(Flags.EMIT_POSITIVE_EXPONENT_SIGN);
		testExp("0.000000e+0", 0.0, 6);
		testExp("1.000000e+0", 1.0, 6);

	}

	@Test
	void toPrecision() {
		int maxLeadingZeros = 6;
		int maxTrailingZeros = 0;
		conv = newConvPrec(0, maxLeadingZeros, maxTrailingZeros);

		testPrec("0.0000012", 0.0000012345,  2);
		testPrec("1.2e-7",    0.00000012345, 2);

		/// EMIT_TRAILING_ZERO_AFTER_POINT is counted toward the maxTrailingZeros limit
		maxTrailingZeros = 1;
		conv = newConvPrec(0, maxLeadingZeros, maxTrailingZeros);
		testPrec("230", 230.0, 2);
		conv = newConvPrec(Flags.EMIT_TRAILING_DECIMAL_POINT, maxLeadingZeros, maxTrailingZeros);
		testPrec("230.", 230.0, 2);
		conv = newConvPrec(Flags.EMIT_TRAILING_DECIMAL_POINT | Flags.EMIT_TRAILING_ZERO_AFTER_POINT,
				maxLeadingZeros, maxTrailingZeros);
		testPrec("2.3e2", 230.0, 2);

		maxTrailingZeros = 3;
		conv = newConvPrec(0, maxLeadingZeros, maxTrailingZeros);
		testPrec("123450", 123450.0, 6);
		testPrec("123450", 123450.0, 5);
		testPrec("123500", 123450.0, 4);
		testPrec("123000", 123450.0, 3);
		testPrec("1.2e5", 123450.0, 2);

		conv = newConvPrec(Flags.NO_TRAILING_ZERO, maxLeadingZeros, maxTrailingZeros);
		testPrec("32300", 32.3 * 1000.0, 14);
	}

	private void testPrec(String expected, double val, int requestedDigits) {
		appendable.setLength(0);
		conv.toPrecision(val, requestedDigits, FORMAT_OPTIONS, appendable);
		assertEquals(expected, appendable.toString());
	}

	private void testExp(String expected, double val, int requestedDigits) {
		appendable.setLength(0);
		conv.toExponential(val, requestedDigits, FORMAT_OPTIONS, appendable);
		assertEquals(expected, appendable.toString());
	}

	private void testFixed(String expected, double val, int requestedDigits, int padWidth) {
		appendable.setLength(0);
		conv.toFixed(val, requestedDigits, formatOptPadding(padWidth) , appendable);
		assertEquals(expected, appendable.toString());
	}

	private void testFixed(String expected, double val, int requestedDigits) {
		appendable.setLength(0);
		conv.toFixed(val, requestedDigits, FORMAT_OPTIONS , appendable);
		assertEquals(expected, appendable.toString());
	}

	private void testShortest(String expected, double val) {
		appendable.setLength(0);
		conv.toShortest(val, FORMAT_OPTIONS, appendable);
		assertEquals(expected, appendable.toString());
	}

	private FormatOptions formatOptPadding(int padWidth) {
		return new FormatOptions(SYMBOLS,
				false,
				false,
				false,
				padWidth,
				false,
				false);
	}

	private DoubleToStringConverter newConvPrec(int flags, int maxLeadingZeros, int maxTrailingZeros) {
		return new DoubleToStringConverter(flags,
				new ShortestPolicy(-6, 21),
				new PrecisionPolicy(maxLeadingZeros, maxTrailingZeros));
	}

	private DoubleToStringConverter newConv(int flags) {
		return new DoubleToStringConverter(flags,
			   new ShortestPolicy(-6, 21),
			   new PrecisionPolicy(6, 0));
	}

	private static class StringAppendable implements Appendable {
		final StringBuilder sb = new StringBuilder();

		@Override
		public String toString() {
			return sb.toString();
		}

		public void setLength(int newLength) {
			sb.setLength(newLength);
		}

		@Override
		public StringAppendable append(String string) {
			sb.append(sb);
			return this;
		}

		@Override
		public StringAppendable append(char character) {
			sb.append(character);
			return this;
		}

		@Override
		public StringAppendable append(char[] chars, int offset, int len) {
			sb.append(chars, offset, len);
			return this;
		}
	}
}
