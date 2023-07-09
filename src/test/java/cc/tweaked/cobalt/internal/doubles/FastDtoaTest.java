/*
 * Copyright 2006-2008 the V8 project authors. All rights reserved.
 * Java Port Copyright 2021 sir-maniac. All Rights reserved. *
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

import org.junit.jupiter.api.Test;

import static cc.tweaked.cobalt.internal.doubles.DoubleTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FastDtoaTest {
	private static final int BUFFER_SIZE = 100;

	@Test
	public void precisionVariousDoubles() {
		DecimalRepBuf buffer = new DecimalRepBuf(BUFFER_SIZE);
		boolean status;

		status = FastDtoa.fastDtoa(1.0, 3, buffer);
		CHECK(status);
		CHECK_GE(3, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		status = FastDtoa.fastDtoa(1.5, 10, buffer);
		if (status) {
			CHECK_GE(10, buffer.length());
			buffer.truncateAllZeros();
			CHECK_EQ("15", buffer);
			CHECK_EQ(1, buffer.getPointPosition());
		}

		double min_double = 5e-324;
		status = FastDtoa.fastDtoa(min_double, 5,
			buffer);
		CHECK(status);
		CHECK_EQ("49407", buffer);
		CHECK_EQ(-323, buffer.getPointPosition());

		double max_double = 1.7976931348623157e308;
		status = FastDtoa.fastDtoa(max_double, 7,
			buffer);
		CHECK(status);
		CHECK_EQ("1797693", buffer);
		CHECK_EQ(309, buffer.getPointPosition());

		status = FastDtoa.fastDtoa(4294967272.0, 14,
			buffer);
		if (status) {
			CHECK_GE(14, buffer.length());
			buffer.truncateAllZeros();
			CHECK_EQ("4294967272", buffer);
			CHECK_EQ(10, buffer.getPointPosition());
		}

		status = FastDtoa.fastDtoa(4.1855804968213567e298, 17,
			buffer);
		CHECK(status);
		CHECK_EQ("41855804968213567", buffer);
		CHECK_EQ(299, buffer.getPointPosition());

		status = FastDtoa.fastDtoa(5.5626846462680035e-309, 1,
			buffer);
		CHECK(status);
		CHECK_EQ("6", buffer);
		CHECK_EQ(-308, buffer.getPointPosition());

		status = FastDtoa.fastDtoa(2147483648.0, 5,
			buffer);
		CHECK(status);
		CHECK_EQ("21475", buffer);
		CHECK_EQ(10, buffer.getPointPosition());

		status = FastDtoa.fastDtoa(3.5844466002796428e+298, 10,
			buffer);
		CHECK(status);
		CHECK_GE(10, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("35844466", buffer);
		CHECK_EQ(299, buffer.getPointPosition());

		long smallest_normal64 = 0x00100000_00000000L;
		double v = Double.longBitsToDouble(smallest_normal64);
		status = FastDtoa.fastDtoa(v, 17, buffer);
		CHECK(status);
		CHECK_EQ("22250738585072014", buffer);
		CHECK_EQ(-307, buffer.getPointPosition());

		long largest_denormal64 = 0x000FFFFF_FFFFFFFFL;
		v = Double.longBitsToDouble(largest_denormal64);
		status = FastDtoa.fastDtoa(v, 17, buffer);
		CHECK(status);
		CHECK_GE(20, buffer.length());
		buffer.truncateAllZeros();
		CHECK_EQ("22250738585072009", buffer);
		CHECK_EQ(-307, buffer.getPointPosition());

		v = 3.3161339052167390562200598e-237;
		status = FastDtoa.fastDtoa(v, 18, buffer);
		CHECK(status);
		CHECK_EQ("331613390521673906", buffer);
		CHECK_EQ(-236, buffer.getPointPosition());

		v = 7.9885183916008099497815232e+191;
		status = FastDtoa.fastDtoa(v, 4, buffer);
		CHECK(status);
		CHECK_EQ("7989", buffer);
		CHECK_EQ(192, buffer.getPointPosition());
	}


	static class ShortestState {
		DecimalRepBuf buffer = new DecimalRepBuf(BUFFER_SIZE);
		;
		public int total = 0;
		public int succeeded = 0;
		public boolean needed_max_length = false;
		public String underTest = "";
	}

	static class PrecisionState {
		DecimalRepBuf buffer = new DecimalRepBuf(BUFFER_SIZE);
		public int total = 0;
		public int succeeded = 0;
		public int succeeded15 = 0;
		public int total15 = 0;

		public String underTest = "";
	}

	@Test
	public void gayPrecision() throws Exception {
		PrecisionState state = new PrecisionState();

		DoubleTestHelper.eachPrecision(state, (st, v, numberDigits, representation, decimalPoint) -> {
			boolean status;

			st.total++;
			st.underTest = String.format("Using {%g, %d, \"%s\", %d}", v, numberDigits, representation, decimalPoint);
			if (numberDigits <= 15) st.total15++;

			status = FastDtoa.fastDtoa(v, numberDigits, st.buffer);
			CHECK_GE(numberDigits, st.buffer.length());
			if (status) {
				st.succeeded++;
				if (numberDigits <= 15) st.succeeded15++;
				st.buffer.truncateAllZeros();
				assertThat(st.underTest, st.buffer.getPointPosition(), is(equalTo(decimalPoint)));
				assertThat(st.underTest, stringOf(st.buffer), is(equalTo(representation)));
			}
		});

		// The precomputed numbers contain many entries with many requested
		// digits. These have a high failure rate and we therefore expect a lower
		// success rate than for the shortest representation.
		assertThat("85% should succeed", state.succeeded * 1.0 / state.total, is(greaterThan(0.85)));
		// However with less than 15 digits almost the algorithm should almost always
		// succeed.
		assertThat(state.succeeded15 * 1.0 / state.total15, is(greaterThan(0.9999)));

		System.out.println("gay-precision tests run :" + Integer.toString(state.total));
	}
}
