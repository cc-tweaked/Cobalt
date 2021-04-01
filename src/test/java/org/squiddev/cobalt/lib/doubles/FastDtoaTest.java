/**
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
package org.squiddev.cobalt.lib.doubles;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.squiddev.cobalt.lib.doubles.DoubleTestHelper.*;
import static org.squiddev.cobalt.lib.doubles.FastDtoa.FastDtoaMode;

public class FastDtoaTest {

	private static final int kBufferSize = 100;

	private static void trimRepresentation(char[] buf) {
		int len = strlen(buf);
		int i;
		for (i = len - 1; i >= 0; --i) {
			if (buf[i] != '0') break;
		}
		buf[i + 1] = '\0';
	}


	@Test
	public void shortedVariousDoubles() {
		char[] buffer = new char[kBufferSize];
		int[] length = new int[1];
		int[] point = new int[1];
		boolean status;

		double min_double = 5e-324;
		status = FastDtoa.FastDtoa(min_double, FastDtoaMode.FAST_DTOA_SHORTEST, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("5", buffer);
		CHECK_EQ(-323, point[0]);

		double max_double = 1.7976931348623157e308;
		status = FastDtoa.FastDtoa(max_double, FastDtoaMode.FAST_DTOA_SHORTEST, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("17976931348623157", buffer);
		CHECK_EQ(309, point[0]);

		status = FastDtoa.FastDtoa(4294967272.0, FastDtoaMode.FAST_DTOA_SHORTEST, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("4294967272", buffer);
		CHECK_EQ(10, point[0]);

		status = FastDtoa.FastDtoa(4.1855804968213567e298, FastDtoaMode.FAST_DTOA_SHORTEST, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("4185580496821357", buffer);
		CHECK_EQ(299, point[0]);

		status = FastDtoa.FastDtoa(5.5626846462680035e-309, FastDtoaMode.FAST_DTOA_SHORTEST, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("5562684646268003", buffer);
		CHECK_EQ(-308, point[0]);

		status = FastDtoa.FastDtoa(2147483648.0, FastDtoaMode.FAST_DTOA_SHORTEST, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("2147483648", buffer);
		CHECK_EQ(10, point[0]);

		status = FastDtoa.FastDtoa(3.5844466002796428e+298, FastDtoaMode.FAST_DTOA_SHORTEST, 0,
				buffer, length, point);
		if (status) {  // Not all FastDtoa variants manage to compute this number.
			CHECK_EQ("35844466002796428", buffer);
			CHECK_EQ(299, point[0]);
		}

		long smallest_normal64 = 0x00100000_00000000L;
		double v = new Ieee.Double(smallest_normal64).value();
		status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_SHORTEST, 0, buffer, length, point);
		if (status) {
			CHECK_EQ("22250738585072014", buffer);
			CHECK_EQ(-307, point[0]);
		}

		long largest_denormal64 = 0x000FFFFF_FFFFFFFFL;
		v = new Ieee.Double(largest_denormal64).value();
		status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_SHORTEST, 0, buffer, length, point);
		if (status) {
			CHECK_EQ("2225073858507201", buffer);
			CHECK_EQ(-307, point[0]);
		}
	}


	@Test
	public void shortestVariousFloats() {
		char[] buffer = new char[kBufferSize];
		int[] length = new int[1];
		int[] point = new int[1];
		boolean status;

		float min_float = 1e-45f;
		status = FastDtoa.FastDtoa(min_float, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("1", buffer);
		CHECK_EQ(-44, point[0]);


		float max_float = 3.4028234e38f;
		status = FastDtoa.FastDtoa(max_float, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("34028235", buffer);
		CHECK_EQ(39, point[0]);

		status = FastDtoa.FastDtoa(4294967272.0f, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("42949673", buffer);
		CHECK_EQ(10, point[0]);

		status = FastDtoa.FastDtoa(3.32306998946228968226e+35f, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("332307", buffer);
		CHECK_EQ(36, point[0]);

		status = FastDtoa.FastDtoa(1.2341e-41f, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("12341", buffer);
		CHECK_EQ(-40, point[0]);

		status = FastDtoa.FastDtoa(3.3554432e7, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("33554432", buffer);
		CHECK_EQ(8, point[0]);

		status = FastDtoa.FastDtoa(3.26494756798464e14f, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("32649476", buffer);
		CHECK_EQ(15, point[0]);

		status = FastDtoa.FastDtoa(3.91132223637771935344e37f, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0,
				buffer, length, point);
		if (status) {  // Not all FastDtoa variants manage to compute this number.
			CHECK_EQ("39113222", buffer);
			CHECK_EQ(38, point[0]);
		}

		int smallest_normal32 = 0x00800000;
		float v = new Ieee.Single(smallest_normal32).value();
		status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0, buffer, length, point);
		if (status) {
			CHECK_EQ("11754944", buffer);
			CHECK_EQ(-37, point[0]);
		}

		int largest_denormal32 = 0x007FFFFF;
		v = new Ieee.Single(largest_denormal32).value();
		status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0, buffer, length, point);
		CHECK(status);
		CHECK_EQ("11754942", buffer);
		CHECK_EQ(-37, point[0]);
	}


	@Test
	public void precisionVariousDoubles() {
		char[] buffer = new char[kBufferSize];
		int[] length = new int[1];
		int[] point = new int[1];
		boolean status;

		status = FastDtoa.FastDtoa(1.0, FastDtoaMode.FAST_DTOA_PRECISION, 3, buffer, length, point);
		CHECK(status);
		CHECK_GE(3, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, point[0]);

		status = FastDtoa.FastDtoa(1.5, FastDtoaMode.FAST_DTOA_PRECISION, 10, buffer, length, point);
		if (status) {
			CHECK_GE(10, length[0]);
			trimRepresentation(buffer);
			CHECK_EQ("15", buffer);
			CHECK_EQ(1, point[0]);
		}

		double min_double = 5e-324;
		status = FastDtoa.FastDtoa(min_double, FastDtoaMode.FAST_DTOA_PRECISION, 5,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("49407", buffer);
		CHECK_EQ(-323, point[0]);

		double max_double = 1.7976931348623157e308;
		status = FastDtoa.FastDtoa(max_double, FastDtoaMode.FAST_DTOA_PRECISION, 7,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("1797693", buffer);
		CHECK_EQ(309, point[0]);

		status = FastDtoa.FastDtoa(4294967272.0, FastDtoaMode.FAST_DTOA_PRECISION, 14,
				buffer, length, point);
		if (status) {
			CHECK_GE(14, length[0]);
			trimRepresentation(buffer);
			CHECK_EQ("4294967272", buffer);
			CHECK_EQ(10, point[0]);
		}

		status = FastDtoa.FastDtoa(4.1855804968213567e298, FastDtoaMode.FAST_DTOA_PRECISION, 17,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("41855804968213567", buffer);
		CHECK_EQ(299, point[0]);

		status = FastDtoa.FastDtoa(5.5626846462680035e-309, FastDtoaMode.FAST_DTOA_PRECISION, 1,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("6", buffer);
		CHECK_EQ(-308, point[0]);

		status = FastDtoa.FastDtoa(2147483648.0, FastDtoaMode.FAST_DTOA_PRECISION, 5,
				buffer, length, point);
		CHECK(status);
		CHECK_EQ("21475", buffer);
		CHECK_EQ(10, point[0]);

		status = FastDtoa.FastDtoa(3.5844466002796428e+298, FastDtoaMode.FAST_DTOA_PRECISION, 10,
				buffer, length, point);
		CHECK(status);
		CHECK_GE(10, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("35844466", buffer);
		CHECK_EQ(299, point[0]);

		long smallest_normal64 = 0x00100000_00000000L;
		double v = new Ieee.Double(smallest_normal64).value();
		status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_PRECISION, 17, buffer, length, point);
		CHECK(status);
		CHECK_EQ("22250738585072014", buffer);
		CHECK_EQ(-307, point[0]);

		long largest_denormal64 = 0x000FFFFF_FFFFFFFFL;
		v = new Ieee.Double(largest_denormal64).value();
		status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_PRECISION, 17, buffer, length, point);
		CHECK(status);
		CHECK_GE(20, length[0]);
		trimRepresentation(buffer);
		CHECK_EQ("22250738585072009", buffer);
		CHECK_EQ(-307, point[0]);

		v = 3.3161339052167390562200598e-237;
		status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_PRECISION, 18, buffer, length, point);
		CHECK(status);
		CHECK_EQ("331613390521673906", buffer);
		CHECK_EQ(-236, point[0]);

		v = 7.9885183916008099497815232e+191;
		status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_PRECISION, 4, buffer, length, point);
		CHECK(status);
		CHECK_EQ("7989", buffer);
		CHECK_EQ(192, point[0]);
	}


	static class ShortestState {
		char[] buffer = new char[kBufferSize];
		public int total = 0;
		public int succeeded = 0;
		public boolean needed_max_length = false;
		public String underTest = "";
	}

	// disabled because file removed from this branch history
	@SuppressWarnings("InfiniteLoopStatement")
	//@Test
	public void gayShortest() throws Exception {
		ShortestState state = new ShortestState();
		try {
			DoubleTestHelper.eachShortest(state, (st, v, representation, decimalPoint) -> {
				int[] length = new int[1];
				int[] point = new int[1];
				boolean status;

				st.total++;
				st.underTest = String.format("Using {%g, \"%s\", %d}", v, representation, decimalPoint);

				status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_SHORTEST, 0, st.buffer, length, point);
				CHECK_GE(FastDtoa.kFastDtoaMaximalLength, length[0]);
				if (status) {
					if (length[0] == FastDtoa.kFastDtoaMaximalLength) st.needed_max_length = true;
					st.succeeded++;
					assertThat(st.underTest, point[0], is(equalTo(decimalPoint)));
					assertThat(st.underTest, stringOf(st.buffer), is(equalTo(representation)));
				}
			});
		} catch (Assert.DoubleConversionAssertionError e) {
			fail("Assertion failed for test " + state.underTest, e);
		}
		assertThat("99% should succeed", state.succeeded*1.0/state.total, is(greaterThan(0.99)));
		assertThat(state.needed_max_length, is(true));
	}


	// disabled because file removed from this branch history
	//@Test
	public void gayShortestSingle() throws Exception {
		ShortestState state = new ShortestState();
		try {
			DoubleTestHelper.eachShortestSingle(state, (st, v, representation, decimalPoint) -> {
				int[] length = new int[1];
				int[] point = new int[1];
				boolean status;

				st.total++;
				st.underTest = String.format("Using {%g, \"%s\", %d}", v, representation, decimalPoint);
				status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0, st.buffer, length, point);
				CHECK_GE(FastDtoa.kFastDtoaMaximalSingleLength, length[0]);
				if (status) {
					if (length[0] == FastDtoa.kFastDtoaMaximalSingleLength) st.needed_max_length = true;
					st.succeeded++;

					assertThat(st.underTest, point[0], is(equalTo(decimalPoint)));
					assertThat(st.underTest, stringOf(st.buffer), is(equalTo(representation)));
				}
			});
		} catch (Assert.DoubleConversionAssertionError e) {
			fail("Assertion failed for test " + state.underTest, e);
		}
		assertThat("98% should succeed", state.succeeded*1.0/state.total, is(greaterThan(0.98)));
		assertThat(state.needed_max_length, is(true));
	}


	static class PrecisionState {
		char[] buffer = new char[kBufferSize];
		public int total = 0;
		public int succeeded = 0;
		public int succeeded15 = 0;
		public int total15 = 0;

		public String underTest = "";
	}

	@Test
	public void gayPrecision() throws Exception {
		PrecisionState state = new PrecisionState();
		try {
			DoubleTestHelper.eachPrecision(state, (st, v, numberDigits, representation, decimalPoint) -> {
				int[] length = new int[1];
				int[] point = new int[1];
				boolean status;

				st.total++;
				st.underTest = String.format("Using {%g, %d, \"%s\", %d}", v, numberDigits, representation, decimalPoint);
				if (numberDigits <= 15) st.total15++;

				status = FastDtoa.FastDtoa(v, FastDtoaMode.FAST_DTOA_PRECISION, numberDigits,
						st.buffer, length, point);
				CHECK_GE(numberDigits, length[0]);
				if (status) {
					st.succeeded++;
					if (numberDigits <= 15) st.succeeded15++;
					trimRepresentation(st.buffer);
					assertThat(st.underTest, point[0], is(equalTo(decimalPoint)));
					assertThat(st.underTest, stringOf(st.buffer), is(equalTo(representation)));
				}
			});
		} catch (Assert.DoubleConversionAssertionError e) {
			fail("Assertion failed for test " + state.underTest, e);
		}
		// The precomputed numbers contain many entries with many requested
		// digits. These have a high failure rate and we therefore expect a lower
		// success rate than for the shortest representation.
		assertThat("85% should succeed", state.succeeded*1.0/state.total, is(greaterThan(0.85)));
		// However with less than 15 digits almost the algorithm should almost always
		// succeed.
		assertThat(state.succeeded15*1.0/state.total15, is(greaterThan(0.9999)));
	}
}
