/*
 * Copyright 2010 the V8 project authors. All rights reserved.
 * Java Port Copyright 2021 sir-maniac. All Rights reserved.
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

import cc.tweaked.cobalt.internal.doubles.DoubleToStringConverter.DtoaMode;
import org.junit.jupiter.api.Test;

import static cc.tweaked.cobalt.internal.doubles.DoubleTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SuppressWarnings("type.argument.type.incompatible")
public class DtoaTest {
	static void doubleToAscii(double v, DtoaMode test_mode, int requested_digits,
							  DecimalRepBuf buffer) {
		DoubleToStringConverter.DtoaMode mode = switch (test_mode) {
			case FIXED -> DtoaMode.FIXED;
			case PRECISION -> DtoaMode.PRECISION;
		};
		DoubleToStringConverter.doubleToAscii(v, mode, requested_digits, buffer);
	}

	static final int BUFFER_SIZE = 100;

	private static int trimRepresentation(DecimalRepBuf buf) {
		buf.truncateAllZeros();
		return buf.length();
	}

	@Test
	public void dtoaVariousDoubles() {
		DecimalRepBuf buffer = new DecimalRepBuf(BUFFER_SIZE);

		doubleToAscii(0.0, DtoaMode.FIXED, 2, buffer);
		CHECK_EQ(1, buffer.length());
		CHECK_EQ("0", stringOf(buffer));
		CHECK_EQ(1, buffer.getPointPosition());

		doubleToAscii(0.0, DtoaMode.PRECISION, 3, buffer);
		CHECK_EQ(1, buffer.length());
		CHECK_EQ("0", stringOf(buffer));
		CHECK_EQ(1, buffer.getPointPosition());

		doubleToAscii(1.0, DtoaMode.FIXED, 3, buffer);
		CHECK_GE(3, buffer.length() - buffer.getPointPosition());
		buffer.truncateAllZeros();
		CHECK_EQ("1", stringOf(buffer));
		CHECK_EQ(1, buffer.getPointPosition());

		doubleToAscii(1.0, DtoaMode.PRECISION, 3, buffer);
		CHECK_GE(3, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("1", stringOf(buffer));
		CHECK_EQ(1, buffer.getPointPosition());

		doubleToAscii(1.5, DtoaMode.FIXED, 10, buffer);
		CHECK_GE(10, buffer.length() - buffer.getPointPosition());
		buffer.truncateAllZeros();
		CHECK_EQ("15", stringOf(buffer));
		CHECK_EQ(1, buffer.getPointPosition());

		doubleToAscii(1.5, DtoaMode.PRECISION, 10, buffer);
		CHECK_GE(10, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("15", stringOf(buffer));
		CHECK_EQ(1, buffer.getPointPosition());

		double min_double = 5e-324;
		doubleToAscii(min_double, DtoaMode.FIXED, 5, buffer);
		CHECK_GE(5, buffer.length() - buffer.getPointPosition());
		buffer.truncateAllZeros();
		CHECK_EQ("", stringOf(buffer));
		CHECK_GE(-5, buffer.getPointPosition());

		doubleToAscii(min_double, DtoaMode.PRECISION, 5, buffer);
		CHECK_GE(5, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("49407", stringOf(buffer));
		CHECK_EQ(-323, buffer.getPointPosition());

		double max_double = 1.7976931348623157e308;
		doubleToAscii(max_double, DtoaMode.PRECISION, 7, buffer);
		CHECK_GE(7, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("1797693", stringOf(buffer));
		CHECK_EQ(309, buffer.getPointPosition());

		doubleToAscii(4294967272.0, DtoaMode.FIXED, 5, buffer);
		CHECK_GE(5, buffer.length() - buffer.getPointPosition());
		buffer.truncateAllZeros();
		CHECK_EQ("4294967272", stringOf(buffer));
		CHECK_EQ(10, buffer.getPointPosition());

		doubleToAscii(4294967272.0, DtoaMode.PRECISION, 14,
			buffer);
		CHECK_GE(14, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("4294967272", stringOf(buffer));
		CHECK_EQ(10, buffer.getPointPosition());

		doubleToAscii(4.1855804968213567e298, DtoaMode.PRECISION, 20,
			buffer);
		CHECK_GE(20, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("41855804968213567225", stringOf(buffer));
		CHECK_EQ(299, buffer.getPointPosition());

		doubleToAscii(5.5626846462680035e-309, DtoaMode.PRECISION, 1,
			buffer);
		CHECK_GE(1, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("6", stringOf(buffer));
		CHECK_EQ(-308, buffer.getPointPosition());

		doubleToAscii(-2147483648.0, DtoaMode.FIXED, 2, buffer);
		CHECK_GE(2, buffer.length() - buffer.getPointPosition());
		buffer.truncateAllZeros();
		CHECK_EQ(1, buffer.getSign());
		CHECK_EQ("2147483648", stringOf(buffer));
		CHECK_EQ(10, buffer.getPointPosition());

		doubleToAscii(-2147483648.0, DtoaMode.PRECISION, 5,
			buffer);
		CHECK_GE(5, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ(1, buffer.getSign());
		CHECK_EQ("21475", stringOf(buffer));
		CHECK_EQ(10, buffer.getPointPosition());

		doubleToAscii(-3.5844466002796428e+298, DtoaMode.PRECISION, 10,
			buffer);
		CHECK_EQ(1, buffer.getSign());
		CHECK_GE(10, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("35844466", stringOf(buffer));
		CHECK_EQ(299, buffer.getPointPosition());

		long smallest_normal64 = 0x00100000_00000000L;
		double v = Double.longBitsToDouble(smallest_normal64);
		doubleToAscii(v, DtoaMode.PRECISION, 20, buffer);
		CHECK_GE(20, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("22250738585072013831", stringOf(buffer));
		CHECK_EQ(-307, buffer.getPointPosition());

		long largest_denormal64 = 0x000FFFFF_FFFFFFFFL;
		v = Double.longBitsToDouble(largest_denormal64);
		doubleToAscii(v, DtoaMode.PRECISION, 20, buffer);
		CHECK_GE(20, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("2225073858507200889", stringOf(buffer));
		CHECK_EQ(-307, buffer.getPointPosition());

		v = 4194304.0;
		doubleToAscii(v, DtoaMode.FIXED, 5, buffer);
		CHECK_GE(5, buffer.length() - buffer.getPointPosition());
		buffer.truncateAllZeros();
		CHECK_EQ("4194304", stringOf(buffer));

		v = 3.3161339052167390562200598e-237;
		doubleToAscii(v, DtoaMode.PRECISION, 19, buffer);
		CHECK_GE(19, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("3316133905216739056", stringOf(buffer));
		CHECK_EQ(-236, buffer.getPointPosition());
	}


	@Test
	public void dtoaSign() {
		DecimalRepBuf buffer = new DecimalRepBuf(BUFFER_SIZE);

		doubleToAscii(0.0, DtoaMode.PRECISION, 1, buffer);
		CHECK(!buffer.getSign());

		doubleToAscii(-0.0, DtoaMode.PRECISION, 1, buffer);
		CHECK(buffer.getSign());

		doubleToAscii(1.0, DtoaMode.PRECISION, 1, buffer);
		CHECK(!buffer.getSign());

		doubleToAscii(-1.0, DtoaMode.PRECISION, 1, buffer);
		CHECK(buffer.getSign());

		doubleToAscii(0.0, DtoaMode.FIXED, 1, buffer);
		CHECK(!buffer.getSign());

		doubleToAscii(-0.0, DtoaMode.FIXED, 1, buffer);
		CHECK(buffer.getSign());

		doubleToAscii(1.0, DtoaMode.FIXED, 1, buffer);
		CHECK(!buffer.getSign());

		doubleToAscii(-1.0, DtoaMode.FIXED, 1, buffer);
		CHECK(buffer.getSign());
	}


	@Test
	public void dtoaCorners() {
		DecimalRepBuf buffer = new DecimalRepBuf(BUFFER_SIZE);

		doubleToAscii(0.0, DtoaMode.PRECISION, 0, buffer);
		CHECK_EQ(0, buffer.length());
		CHECK_EQ("", stringOf(buffer));
		CHECK(!buffer.getSign());

		doubleToAscii(1.0, DtoaMode.PRECISION, 0, buffer);
		CHECK_EQ(0, buffer.length());
		CHECK_EQ("", stringOf(buffer));
		CHECK(!buffer.getSign());

		doubleToAscii(0.0, DtoaMode.FIXED, 0, buffer);
		CHECK_EQ(1, buffer.length());
		CHECK_EQ("0", stringOf(buffer));
		CHECK(!buffer.getSign());

		doubleToAscii(1.0, DtoaMode.FIXED, 0, buffer);
		CHECK_EQ(1, buffer.length());
		CHECK_EQ("1", stringOf(buffer));
		CHECK(!buffer.getSign());
	}


	static class DataTestState {
		DecimalRepBuf buffer = new DecimalRepBuf(BUFFER_SIZE);
		public String underTest = "";
		int total = 0;
	}


	@Test
	public void dtoaGayFixed() throws Exception {
		DataTestState state = new DataTestState();
		DoubleTestHelper.eachFixed(state, (st, v, numberDigits, representation, decimalPoint) -> {
			st.underTest = String.format("Using {%g, \"%s\", %d}", v, representation, decimalPoint);
			doubleToAscii(v, DtoaMode.FIXED, numberDigits, st.buffer);
			assertThat(st.underTest, st.buffer.getSign(), is(false)); // All precomputed numbers are positive.
			assertThat(st.underTest, st.buffer.getPointPosition(), is(equalTo(decimalPoint)));
			assertThat(st.underTest, (st.buffer.length() - st.buffer.getPointPosition()), is(lessThanOrEqualTo(numberDigits)));
			st.buffer.truncateAllZeros();
			assertThat(st.underTest, stringOf(st.buffer), is(equalTo(representation)));
		});
	}

	@Test
	public void dtoaGayPrecision() throws Exception {
		DataTestState state = new DataTestState();
		DoubleTestHelper.eachPrecision(state, (st, v, numberDigits, representation, decimalPoint) -> {
			st.underTest = String.format("Using {%g, \"%s\", %d}", v, representation, decimalPoint);
			doubleToAscii(v, DtoaMode.PRECISION, numberDigits,
				st.buffer);
			assertThat(st.underTest, st.buffer.getSign(), is(false)); // All precomputed numbers are positive.
			assertThat(st.underTest, st.buffer.getPointPosition(), is(equalTo(decimalPoint)));
			assertThat(st.underTest, st.buffer.length(), is(greaterThanOrEqualTo(numberDigits)));
			st.buffer.truncateAllZeros();
			assertThat(st.underTest, stringOf(st.buffer), is(equalTo(representation)));
		});
	}
}
