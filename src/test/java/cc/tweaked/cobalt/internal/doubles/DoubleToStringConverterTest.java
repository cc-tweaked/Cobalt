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

import cc.tweaked.cobalt.internal.doubles.DoubleToStringConverter.Flags;
import cc.tweaked.cobalt.internal.doubles.DoubleToStringConverter.FormatOptions;
import cc.tweaked.cobalt.internal.doubles.DoubleToStringConverter.PrecisionPolicy;
import cc.tweaked.cobalt.internal.doubles.DoubleToStringConverter.Symbols;
import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.Buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoubleToStringConverterTest {
	private static final Symbols SYMBOLS = new Symbols("Infinity", "NaN", 'e');
	private static final FormatOptions FORMAT_OPTIONS =
		new FormatOptions(SYMBOLS, false, false, false, -1, false, false);
	private DoubleToStringConverter conv;

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
		testFixed("20.00", 20.0, 2, padding(5, false, false)); // %5.2f
		testFixed(" 0.05", 5.2e-2, 2, padding(5, false, false)); // %5.2f
		testFixed("52.30", 52.3, 2, padding(5, false, false)); // %5.2f

		// padding
		testFixed("      1.0", 1.0, 1, padding(9, false, false));
		testFixed("1.0      ", 1.0, 1, padding(9, false, true));
		testFixed("0000001.0", 1.0, 1, padding(9, true, false));

		boolean zeroPad = true;
		testFixed("1", 1.0, 0, padding(1, zeroPad, false));
		testFixed("0", 0.1, 0, padding(1, zeroPad, false));
		testFixed("01", 1.0, 0, padding(2, zeroPad, false));
		testFixed("-1", -1.0, 0, padding(2, zeroPad, false));
		testFixed("001", 1.0, 0, padding(3, zeroPad, false));
		testFixed("-01", -1.0, 0, padding(3, zeroPad, false));

		testFixed("123", 123.456, 0, padding(1, zeroPad, false));
		testFixed("0", 1.23456e-05, 0, padding(1, zeroPad, false));

		testFixed("100", 100.0, 0, padding(2, zeroPad, false));
		testFixed("1000", 1000.0, 0, padding(2, zeroPad, false));

		testFixed("000100", 100.0, 0, padding(6, zeroPad, false));
		testFixed("001000", 1000.0, 0, padding(6, zeroPad, false));
		testFixed("010000", 10000.0, 0, padding(6, zeroPad, false));
		testFixed("100000", 100000.0, 0, padding(6, zeroPad, false));
		testFixed("1000000", 1000000.0, 0, padding(6, zeroPad, false));

		testFixed("00", 0.01, 0, padding(2, zeroPad, false));

		testFixed("0.0", 0.01, 1, padding(2, false, false));

		testFixed("            0.010000", 0.01, 6, padding(20, false, false));

	}

	@Test
	void toExponential() {
		conv = newConv(0);

		testExp("3.1e0", 3.12, 1);
		testExp("5.000e0", 5.0, 3);
		testExp("1.00e-3", 0.001, 2);
		testExp("3.1415e0", 3.1415, 4);
		testExp("3.142e0", 3.1415, 3);
		testExp("1.235e14", 123456789000000.0, 3);
		testExp("1.00000000000000001988462483865600e30",
			1000000000000000019884624838656.0, 32);
		testExp("1e3", 1234, 0);

		conv = newCobaltConv();
		testExp("0.000000e+00", 0.0, 6);
		testExp("1.000000e+00", 1.0, 6);

		testExp("0.e+00", 0.0, 0, formatOptTrailingPoint());
		testExp("1.e+00", 1.0, 0, formatOptTrailingPoint());

		// padding
		testExp("01e+02", 100, 0, padding(6, true, false));
		testExp("-1e+02", -100, 0, padding(6, true, false));
		testExp("01e+03", 1000, 0, padding(6, true, false));
		testExp("001e+02", 100, 0, padding(7, true, false));

	}

	@Test
	void toPrecision() {
		int maxLeadingZeros = 6;
		int maxTrailingZeros = 0;
		conv = newConvPrec(0, maxLeadingZeros, maxTrailingZeros);

		testPrec("0.0000012", 0.0000012345, 2);
		testPrec("1.2e-7", 0.00000012345, 2);

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

		int precision = 6;
		//conv = newConvPrec(Flags.NO_TRAILING_ZERO, -4, 0, 2);
		conv = newCobaltConv();
		testPrec("              100000", 100000.0, precision, padding(20, false, false));
		testPrec("               1e+06", 1000000.0, precision, padding(20, false, false));
		testPrec("               1e+07", 10000000.0, precision, padding(20, false, false));

		FormatOptions fo = new FormatOptions(SYMBOLS,
			true,
			false,
			true,
			20,
			false,
			false);

		testPrec("            +0.00000", 0.0, precision, fo);
		testPrec("            +1.00000", 1.0, precision, fo);
		testPrec("            -1.00000", -1.0, precision, fo);

	}

	private void testPrec(String expected, double val, int requestedDigits, FormatOptions fo) {
		Buffer appendable = new Buffer();
		conv.toPrecision(val, requestedDigits, fo, appendable);
		assertEquals(expected, appendable.toString());
	}

	private void testPrec(String expected, double val, int requestedDigits) {
		Buffer appendable = new Buffer();
		conv.toPrecision(val, requestedDigits, FORMAT_OPTIONS, appendable);
		assertEquals(expected, appendable.toString());
	}

	private void testExp(String expected, double val, int requestedDigits) {
		testExp(expected, val, requestedDigits, FORMAT_OPTIONS);
	}

	private void testExp(String expected, double val, int requestedDigits, FormatOptions fo) {
		Buffer appendable = new Buffer();
		conv.toExponential(val, requestedDigits, fo, appendable);
		assertEquals(expected, appendable.toString());
	}

	private void testFixed(String expected, double val, int precision, FormatOptions fo) {
		Buffer appendable = new Buffer();
		conv.toFixed(val, precision, fo, appendable);
		assertEquals(expected, appendable.toString());
	}

	private void testFixed(String expected, double val, int requestedDigits) {
		Buffer appendable = new Buffer();
		conv.toFixed(val, requestedDigits, FORMAT_OPTIONS, appendable);
		assertEquals(expected, appendable.toString());
	}

	private FormatOptions padding(int padWidth, boolean zeroPad, boolean leftAdjust) {
		return new FormatOptions(SYMBOLS,
			false,
			false,
			false,
			padWidth,
			zeroPad,
			leftAdjust);
	}

	private FormatOptions formatOptTrailingPoint() {
		return new FormatOptions(SYMBOLS,
			false,
			false,
			true,
			-1,
			false,
			false
		);
	}

	private DoubleToStringConverter newConvPrec(int flags, int maxLeadingZeros, int maxTrailingZeros) {
		return new DoubleToStringConverter(flags, new PrecisionPolicy(maxLeadingZeros, maxTrailingZeros), 0);
	}

	private DoubleToStringConverter newConv(int flags) {
		return new DoubleToStringConverter(flags, new PrecisionPolicy(6, 0), 0);
	}

	private DoubleToStringConverter newCobaltConv() {
		int flags = Flags.EMIT_POSITIVE_EXPONENT_SIGN | Flags.NO_TRAILING_ZERO;
		return new DoubleToStringConverter(flags, new PrecisionPolicy(6, 0), 2);
	}
}
