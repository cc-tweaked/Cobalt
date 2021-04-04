/*
 *  Copyright 2010 the V8 project authors. All rights reserved.
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
import org.squiddev.cobalt.lib.doubles.DoubleToStringConverter.DtoaMode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.squiddev.cobalt.lib.doubles.DoubleTestHelper.*;

public class DtoaTest {


	static void doubleToAscii(double v, DtoaMode test_mode, int requested_digits,
							  char[] buffer, boolean[] sign, int[] length,
							  int[] point) {
		DoubleToStringConverter.DtoaMode mode = DtoaMode.SHORTEST;
		switch (test_mode) {
			case SHORTEST:
				mode = DtoaMode.SHORTEST;
				break;
			case SHORTEST_SINGLE:
				mode = DtoaMode.SHORTEST_SINGLE;
				break;
			case FIXED:
				mode = DtoaMode.FIXED;
				break;
			case PRECISION:
				mode = DtoaMode.PRECISION;
				break;
		}
		DoubleToStringConverter.doubleToAscii(v, mode, requested_digits,
				buffer, buffer.length,
				sign, length, point);
	}

	static final int BUFFER_SIZE = 100;

	private static int trimRepresentation(char[] buf) {
		return trimRepresentation(buf, strlen(buf));
	}

	private static int trimRepresentation(char[] buf, int len) {
		int i;
		for (i = len - 1; i >= 0; --i) {
			if (buf[i] != '0') break;
		}
		buf[i + 1] = '\0';
		return i+1;
	}

	@Test
	public void dtoaVariousDoubles() {
		char[] buffer = new char[BUFFER_SIZE];
		int[] length = new int[1];
		int[] point = new int[1];
		boolean[] sign = new boolean[1];

		doubleToAscii(0.0, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK_EQ("0", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(0.0f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK_EQ("0", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(0.0, DtoaMode.FIXED, 2, buffer, sign, length, point);
		CHECK_EQ(1, length[0]);
		CHECK_EQ("0", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(0.0, DtoaMode.PRECISION, 3, buffer, sign, length, point);
		CHECK_EQ(1, length[0]);
		CHECK_EQ("0", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(1.0, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK_EQ("1", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(1.0f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK_EQ("1", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(1.0, DtoaMode.FIXED, 3, buffer, sign, length, point);
		CHECK_GE(3, length[0] - point[0]);
		trimRepresentation(buffer);
		CHECK_EQ("1", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(1.0, DtoaMode.PRECISION, 3, buffer, sign, length, point);
		CHECK_GE(3, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("1", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(1.5, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK_EQ("15", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(1.5f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK_EQ("15", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(1.5, DtoaMode.FIXED, 10, buffer, sign, length, point);
		CHECK_GE(10, length[0] - point[0]);
		trimRepresentation(buffer);
		CHECK_EQ("15", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		doubleToAscii(1.5, DtoaMode.PRECISION, 10, buffer, sign, length, point);
		CHECK_GE(10, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("15", stringOf(buffer));
		CHECK_EQ(1, point[0]);

		double min_double = 5e-324;
		doubleToAscii(min_double, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK_EQ("5", stringOf(buffer));
		CHECK_EQ(-323, point[0]);

		float min_float = 1e-45f;
		doubleToAscii(min_float, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK_EQ("1", stringOf(buffer));
		CHECK_EQ(-44, point[0]);

		doubleToAscii(min_double, DtoaMode.FIXED, 5, buffer, sign, length, point);
		CHECK_GE(5, length[0] - point[0]);
		trimRepresentation(buffer);
		CHECK_EQ("", stringOf(buffer));
		CHECK_GE(-5, point[0]);

		doubleToAscii(min_double, DtoaMode.PRECISION, 5, buffer, sign, length, point);
		CHECK_GE(5, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("49407", stringOf(buffer));
		CHECK_EQ(-323, point[0]);

		double max_double = 1.7976931348623157e308;
		doubleToAscii(max_double, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK_EQ("17976931348623157", stringOf(buffer));
		CHECK_EQ(309, point[0]);

		float max_float = 3.4028234e38f;
		doubleToAscii(max_float, DtoaMode.SHORTEST_SINGLE, 0,
				buffer, sign, length, point);
		CHECK_EQ("34028235", stringOf(buffer));
		CHECK_EQ(39, point[0]);

		doubleToAscii(max_double, DtoaMode.PRECISION, 7, buffer, sign, length, point);
		CHECK_GE(7, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("1797693", stringOf(buffer));
		CHECK_EQ(309, point[0]);

		doubleToAscii(4294967272.0, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK_EQ("4294967272", stringOf(buffer));
		CHECK_EQ(10, point[0]);

		doubleToAscii(4294967272.0f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK_EQ("42949673", stringOf(buffer));
		CHECK_EQ(10, point[0]);

		doubleToAscii(4294967272.0, DtoaMode.FIXED, 5, buffer, sign, length, point);
		CHECK_GE(5, length[0] - point[0]);
		trimRepresentation(buffer);
		CHECK_EQ("4294967272", stringOf(buffer));
		CHECK_EQ(10, point[0]);

		doubleToAscii(4294967272.0, DtoaMode.PRECISION, 14,
				buffer, sign, length, point);
		CHECK_GE(14, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("4294967272", stringOf(buffer));
		CHECK_EQ(10, point[0]);

		doubleToAscii(4.1855804968213567e298, DtoaMode.SHORTEST, 0,
				buffer, sign, length, point);
		CHECK_EQ("4185580496821357", stringOf(buffer));
		CHECK_EQ(299, point[0]);

		doubleToAscii(4.1855804968213567e298, DtoaMode.PRECISION, 20,
				buffer, sign, length, point);
		CHECK_GE(20, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("41855804968213567225", stringOf(buffer));
		CHECK_EQ(299, point[0]);

		doubleToAscii(5.5626846462680035e-309, DtoaMode.SHORTEST, 0,
				buffer, sign, length, point);
		CHECK_EQ("5562684646268003", stringOf(buffer));
		CHECK_EQ(-308, point[0]);

		doubleToAscii(5.5626846462680035e-309, DtoaMode.PRECISION, 1,
				buffer, sign, length, point);
		CHECK_GE(1, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("6", stringOf(buffer));
		CHECK_EQ(-308, point[0]);

		doubleToAscii(-2147483648.0, DtoaMode.SHORTEST, 0,
				buffer, sign, length, point);
		CHECK_EQ(1, sign[0]);
		CHECK_EQ("2147483648", stringOf(buffer));
		CHECK_EQ(10, point[0]);

		doubleToAscii(-2147483648.0, DtoaMode.SHORTEST_SINGLE, 0,
				buffer, sign, length, point);
		CHECK_EQ(1, sign[0]);
		CHECK_EQ("21474836", stringOf(buffer));
		CHECK_EQ(10, point[0]);


		doubleToAscii(-2147483648.0, DtoaMode.FIXED, 2, buffer, sign, length, point);
		CHECK_GE(2, length[0] - point[0]);
		trimRepresentation(buffer);
		CHECK_EQ(1, sign[0]);
		CHECK_EQ("2147483648", stringOf(buffer));
		CHECK_EQ(10, point[0]);

		doubleToAscii(-2147483648.0, DtoaMode.PRECISION, 5,
				buffer, sign, length, point);
		CHECK_GE(5, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ(1, sign[0]);
		CHECK_EQ("21475", stringOf(buffer));
		CHECK_EQ(10, point[0]);

		doubleToAscii(-3.5844466002796428e+298, DtoaMode.SHORTEST, 0,
				buffer, sign, length, point);
		CHECK_EQ(1, sign[0]);
		CHECK_EQ("35844466002796428", stringOf(buffer));
		CHECK_EQ(299, point[0]);

		doubleToAscii(-3.5844466002796428e+298, DtoaMode.PRECISION, 10,
				buffer, sign, length, point);
		CHECK_EQ(1, sign[0]);
		CHECK_GE(10, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("35844466", stringOf(buffer));
		CHECK_EQ(299, point[0]);

		long smallest_normal64 = 0x00100000_00000000L;
		double v = new Ieee.Double(smallest_normal64).value();
		doubleToAscii(v, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK_EQ("22250738585072014", stringOf(buffer));
		CHECK_EQ(-307, point[0]);

		int smallest_normal32 = 0x00800000;
		float f = new Ieee.Single(smallest_normal32).value();
		doubleToAscii(f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK_EQ("11754944", stringOf(buffer));
		CHECK_EQ(-37, point[0]);

		doubleToAscii(v, DtoaMode.PRECISION, 20, buffer, sign, length, point);
		CHECK_GE(20, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("22250738585072013831", stringOf(buffer));
		CHECK_EQ(-307, point[0]);

		long largest_denormal64 = 0x000FFFFF_FFFFFFFFL;
		v = new Ieee.Double(largest_denormal64).value();
		doubleToAscii(v, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK_EQ("2225073858507201", stringOf(buffer));
		CHECK_EQ(-307, point[0]);

		int largest_denormal32 = 0x007FFFFF;
		f = new Ieee.Single(Float.intBitsToFloat(largest_denormal32)).value();
		doubleToAscii(f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK_EQ("11754942", stringOf(buffer));
		CHECK_EQ(-37, point[0]);

		doubleToAscii(v, DtoaMode.PRECISION, 20, buffer, sign, length, point);
		CHECK_GE(20, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("2225073858507200889", stringOf(buffer));
		CHECK_EQ(-307, point[0]);

		doubleToAscii(4128420500802942e-24, DtoaMode.SHORTEST, 0,
				buffer, sign, length, point);
		CHECK_EQ(0, sign[0]);
		CHECK_EQ("4128420500802942", stringOf(buffer));
		CHECK_EQ(-8, point[0]);

		v = -3.9292015898194142585311918e-10;
		doubleToAscii(v, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK_EQ("39292015898194143", stringOf(buffer));

		f = -3.9292015898194142585311918e-10f;
		doubleToAscii(f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK_EQ("39292017", stringOf(buffer));

		v = 4194304.0;
		doubleToAscii(v, DtoaMode.FIXED, 5, buffer, sign, length, point);
		CHECK_GE(5, length[0] - point[0]);
		trimRepresentation(buffer);
		CHECK_EQ("4194304", stringOf(buffer));

		v = 3.3161339052167390562200598e-237;
		doubleToAscii(v, DtoaMode.PRECISION, 19, buffer, sign, length, point);
		CHECK_GE(19, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("3316133905216739056", stringOf(buffer));
		CHECK_EQ(-236, point[0]);
	}


	@Test
	public void dtoaSign() {
		char[] buffer = new char[BUFFER_SIZE];
		int[] length = new int[1];
		int[] point = new int[1];
		boolean[] sign = new boolean[1];

		doubleToAscii(0.0, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK(!sign[0]);

		doubleToAscii(-0.0, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK(sign[0]);

		doubleToAscii(1.0, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK(!sign[0]);

		doubleToAscii(-1.0, DtoaMode.SHORTEST, 0, buffer, sign, length, point);
		CHECK(sign[0]);

		doubleToAscii(0.0f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK(!sign[0]);

		doubleToAscii(-0.0f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK(sign[0]);

		doubleToAscii(1.0f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK(!sign[0]);

		doubleToAscii(-1.0f, DtoaMode.SHORTEST_SINGLE, 0, buffer, sign, length, point);
		CHECK(sign[0]);

		doubleToAscii(0.0, DtoaMode.PRECISION, 1, buffer, sign, length, point);
		CHECK(!sign[0]);

		doubleToAscii(-0.0, DtoaMode.PRECISION, 1, buffer, sign, length, point);
		CHECK(sign[0]);

		doubleToAscii(1.0, DtoaMode.PRECISION, 1, buffer, sign, length, point);
		CHECK(!sign[0]);

		doubleToAscii(-1.0, DtoaMode.PRECISION, 1, buffer, sign, length, point);
		CHECK(sign[0]);

		doubleToAscii(0.0, DtoaMode.FIXED, 1, buffer, sign, length, point);
		CHECK(!sign[0]);

		doubleToAscii(-0.0, DtoaMode.FIXED, 1, buffer, sign, length, point);
		CHECK(sign[0]);

		doubleToAscii(1.0, DtoaMode.FIXED, 1, buffer, sign, length, point);
		CHECK(!sign[0]);

		doubleToAscii(-1.0, DtoaMode.FIXED, 1, buffer, sign, length, point);
		CHECK(sign[0]);
	}


	@Test
	public void dtoaCorners() {
		char[] buffer = new char[BUFFER_SIZE];
		int[] length = new int[1];
		int[] point = new int[1];
		boolean[] sign = new boolean[1];

		doubleToAscii(0.0, DtoaMode.PRECISION, 0, buffer, sign, length, point);
		CHECK_EQ(0, length[0]);
		CHECK_EQ("", stringOf(buffer));
		CHECK(!sign[0]);

		doubleToAscii(1.0, DtoaMode.PRECISION, 0, buffer, sign, length, point);
		CHECK_EQ(0, length[0]);
		CHECK_EQ("", stringOf(buffer));
		CHECK(!sign[0]);

		doubleToAscii(0.0, DtoaMode.FIXED, 0, buffer, sign, length, point);
		CHECK_EQ(1, length[0]);
		CHECK_EQ("0", stringOf(buffer));
		CHECK(!sign[0]);

		doubleToAscii(1.0, DtoaMode.FIXED, 0, buffer, sign, length, point);
		CHECK_EQ(1, length[0]);
		CHECK_EQ("1", stringOf(buffer));
		CHECK(!sign[0]);
	}


	static class DataTestState {
		char[] buffer = new char[BUFFER_SIZE];
		public String underTest = "";
		int total = 0;
	}

	// disabled because file removed from this branch history
	//@Test
	public void dtoaGayShortest() throws Exception {
		DataTestState state = new DataTestState();
		try {
			DoubleTestHelper.eachShortest(state, (st, v, representation, decimalPoint) -> {
				int[] length = new int[1];
				int[] point = new int[1];
				boolean[] sign = new boolean[1];

				st.underTest = String.format("Using {%g, \"%s\", %d}", v, representation, decimalPoint);

				doubleToAscii(v, DtoaMode.SHORTEST, 0, st.buffer, sign, length, point);
				assertThat(st.underTest, sign[0], is(false)); // All precomputed numbers are positive.
				assertThat(st.underTest, point[0], is(equalTo(decimalPoint)));
				assertThat(st.underTest, stringOf(st.buffer, length[0]), is(equalTo(representation)));
			});
		} catch (Assert.DoubleConversionAssertionError e) {
			fail("Assertion failed for test " + state.underTest, e);
		}
	}

	// disabled because file removed from this branch history
	//@Test
	public void dtoaGayShortestSingle() throws Exception {
		DataTestState state = new DataTestState();
		try {
			DoubleTestHelper.eachShortestSingle(state, (st, v, representation, decimalPoint) -> {
				int[] length = new int[1];
				int[] point = new int[1];
				boolean[] sign = new boolean[1];

				st.underTest = String.format("Using {%g, \"%s\", %d}", v, representation, decimalPoint);
				doubleToAscii(v, DtoaMode.SHORTEST_SINGLE, 0, st.buffer, sign, length, point);
				assertThat(st.underTest, sign[0], is(false)); // All precomputed numbers are positive.
				assertThat(st.underTest, point[0], is(equalTo(decimalPoint)));
				assertThat(st.underTest, stringOf(st.buffer, length[0]), is(equalTo(representation)));
			});
		} catch (Assert.DoubleConversionAssertionError e) {
			fail("Assertion failed for test " + state.underTest, e);
		}
	}

	@Test
	public void dtoaGayFixed() throws Exception {
		DataTestState state = new DataTestState();
		try {
			DoubleTestHelper.eachFixed(state, (st, v, numberDigits, representation, decimalPoint) -> {
				int[] length = new int[1];
				int[] point = new int[1];
				boolean[] sign = new boolean[1];

				st.underTest = String.format("Using {%g, \"%s\", %d}", v, representation, decimalPoint);
				doubleToAscii(v, DtoaMode.FIXED, numberDigits, st.buffer, sign, length, point);
				assertThat(st.underTest, sign[0], is(false)); // All precomputed numbers are positive.
				assertThat(st.underTest, point[0], is(equalTo(decimalPoint)));
				assertThat(st.underTest, (length[0] - point[0]), is(lessThanOrEqualTo(numberDigits)));
				int len = trimRepresentation(st.buffer);
				assertThat(st.underTest, stringOf(st.buffer, len), is(equalTo(representation)));
			});
		} catch (Assert.DoubleConversionAssertionError e) {
			fail("Assertion failed for test " + state.underTest, e);
		}
	}

	// FIXME: very slow
	//@Test
	public void dtoaGayPrecision() throws Exception {
		DataTestState state = new DataTestState();
		try {
			DoubleTestHelper.eachPrecision(state, (st, v, numberDigits, representation, decimalPoint) -> {
				int[] length = new int[1];
				int[] point = new int[1];
				boolean[] sign = new boolean[1];

				st.underTest = String.format("Using {%g, \"%s\", %d}", v, representation, decimalPoint);
				long t = System.currentTimeMillis();
				doubleToAscii(v, DtoaMode.PRECISION, numberDigits,
						st.buffer, sign, length, point);
				t = System.currentTimeMillis() - t;
				//assertThat(st.underTest, t, is(lessThanOrEqualTo(50L)));
				assertThat(st.underTest, sign[0], is(false)); // All precomputed numbers are positive.
				assertThat(st.underTest, point[0], is(equalTo(decimalPoint)));
				assertThat(st.underTest, length[0], is(greaterThanOrEqualTo(numberDigits)));
				int len = trimRepresentation(st.buffer);
				assertThat(st.underTest, stringOf(st.buffer, len), is(equalTo(representation)));
			});
		} catch (Assert.DoubleConversionAssertionError e) {
			fail("Assertion failed for test " + state.underTest, e);
		}
	}
}
