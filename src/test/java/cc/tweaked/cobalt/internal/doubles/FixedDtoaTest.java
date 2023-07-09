/*
 * Copyright 2010 the V8 project authors. All rights reserved.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static cc.tweaked.cobalt.internal.doubles.DoubleTestHelper.*;

public class FixedDtoaTest {
	private static final int kBufferSize = 500;

	@Test
	public void variousDoubles() {
		DecimalRepBuf buffer = new DecimalRepBuf(kBufferSize);

		CHECK(FixedDtoa.fastFixedDtoa(1.0, 1, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1.0, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1.0, 0, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		// FIXME  Doesn't seem to convert in java
//		CHECK(FixedDtoa.fastFixedDtoa(0xFFFFFFFF, 5, buffer));
//		CHECK_EQ("4294967295", buffer);
//		CHECK_EQ(10, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(4294967296.0, 5, buffer));
		CHECK_EQ("4294967296", buffer);
		CHECK_EQ(10, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1e21, 5, buffer));
		CHECK_EQ("1", buffer);
		// CHECK_EQ(22, buffer.getPointPosition());
		CHECK_EQ(22, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(999999999999999868928.00, 2, buffer));
		CHECK_EQ("999999999999999868928", buffer);
		CHECK_EQ(21, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(6.9999999999999989514240000e+21, 5, buffer));
		CHECK_EQ("6999999999999998951424", buffer);
		CHECK_EQ(22, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1.5, 5, buffer));
		CHECK_EQ("15", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1.55, 5, buffer));
		CHECK_EQ("155", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1.55, 1, buffer));
		CHECK_EQ("16", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1.00000001, 15, buffer));
		CHECK_EQ("100000001", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.1, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(0, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.01, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.001, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-2, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0001, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-3, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00001, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-4, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000001, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-5, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000001, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-6, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000001, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-7, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000001, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-8, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000001, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-9, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000000001, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-10, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000001, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-11, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000001, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-12, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000000000001, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-13, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000001, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-14, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000000001, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-15, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000000000000001, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-16, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000001, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-17, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000000000001, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-18, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000000000000000001, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-19, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.10000000004, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(0, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.01000000004, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00100000004, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-2, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00010000004, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-3, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00001000004, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-4, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000100004, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-5, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000010004, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-6, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000001004, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-7, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000000104, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-8, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000001000004, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-9, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000100004, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-10, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000010004, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-11, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000001004, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-12, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000000104, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-13, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000001000004, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-14, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000100004, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-15, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000010004, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-16, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000001004, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-17, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000000104, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-18, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000000014, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-19, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.10000000006, 10, buffer));
		CHECK_EQ("1000000001", buffer);
		CHECK_EQ(0, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.01000000006, 10, buffer));
		CHECK_EQ("100000001", buffer);
		CHECK_EQ(-1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00100000006, 10, buffer));
		CHECK_EQ("10000001", buffer);
		CHECK_EQ(-2, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00010000006, 10, buffer));
		CHECK_EQ("1000001", buffer);
		CHECK_EQ(-3, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00001000006, 10, buffer));
		CHECK_EQ("100001", buffer);
		CHECK_EQ(-4, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000100006, 10, buffer));
		CHECK_EQ("10001", buffer);
		CHECK_EQ(-5, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000010006, 10, buffer));
		CHECK_EQ("1001", buffer);
		CHECK_EQ(-6, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000001006, 10, buffer));
		CHECK_EQ("101", buffer);
		CHECK_EQ(-7, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000000106, 10, buffer));
		CHECK_EQ("11", buffer);
		CHECK_EQ(-8, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000001000006, 15, buffer));
		CHECK_EQ("100001", buffer);
		CHECK_EQ(-9, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000100006, 15, buffer));
		CHECK_EQ("10001", buffer);
		CHECK_EQ(-10, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000010006, 15, buffer));
		CHECK_EQ("1001", buffer);
		CHECK_EQ(-11, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000001006, 15, buffer));
		CHECK_EQ("101", buffer);
		CHECK_EQ(-12, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000000000000106, 15, buffer));
		CHECK_EQ("11", buffer);
		CHECK_EQ(-13, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000001000006, 20, buffer));
		CHECK_EQ("100001", buffer);
		CHECK_EQ(-14, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000100006, 20, buffer));
		CHECK_EQ("10001", buffer);
		CHECK_EQ(-15, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000010006, 20, buffer));
		CHECK_EQ("1001", buffer);
		CHECK_EQ(-16, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000001006, 20, buffer));
		CHECK_EQ("101", buffer);
		CHECK_EQ(-17, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000000106, 20, buffer));
		CHECK_EQ("11", buffer);
		CHECK_EQ(-18, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000000000000000016, 20, buffer));
		CHECK_EQ("2", buffer);
		CHECK_EQ(-19, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.6, 0, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.96, 1, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.996, 2, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.9996, 3, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.99996, 4, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.999996, 5, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.9999996, 6, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.99999996, 7, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.999999996, 8, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.9999999996, 9, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.99999999996, 10, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.999999999996, 11, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.9999999999996, 12, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.99999999999996, 13, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.999999999999996, 14, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.9999999999999996, 15, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00999999999999996, 16, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000999999999999996, 17, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-2, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.0000999999999999996, 18, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-3, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.00000999999999999996, 19, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-4, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.000000999999999999996, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-5, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(323423.234234, 10, buffer));
		CHECK_EQ("323423234234", buffer);
		CHECK_EQ(6, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(12345678.901234, 4, buffer));
		CHECK_EQ("123456789012", buffer);
		CHECK_EQ(8, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(98765.432109, 5, buffer));
		CHECK_EQ("9876543211", buffer);
		CHECK_EQ(5, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(42, 20, buffer));
		CHECK_EQ("42", buffer);
		CHECK_EQ(2, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(0.5, 0, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(1, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1e-23, 10, buffer));
		CHECK_EQ("", buffer);
		CHECK_EQ(-10, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1e-123, 2, buffer));
		CHECK_EQ("", buffer);
		CHECK_EQ(-2, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1e-123, 0, buffer));
		CHECK_EQ("", buffer);
		CHECK_EQ(0, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1e-23, 20, buffer));
		CHECK_EQ("", buffer);
		CHECK_EQ(-20, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1e-21, 20, buffer));
		CHECK_EQ("", buffer);
		CHECK_EQ(-20, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1e-22, 20, buffer));
		CHECK_EQ("", buffer);
		CHECK_EQ(-20, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(6e-21, 20, buffer));
		CHECK_EQ("1", buffer);
		CHECK_EQ(-19, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(9.1193616301674545152000000e+19, 0,
			buffer));
		CHECK_EQ("91193616301674545152", buffer);
		CHECK_EQ(20, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(4.8184662102767651659096515e-04, 19,
			buffer));
		CHECK_EQ("4818466210276765", buffer);
		CHECK_EQ(-3, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1.9023164229540652612705182e-23, 8,
			buffer));
		CHECK_EQ("", buffer);
		CHECK_EQ(-8, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(1000000000000000128.0, 0,
			buffer));
		CHECK_EQ("1000000000000000128", buffer);
		CHECK_EQ(19, buffer.getPointPosition());

		CHECK(FixedDtoa.fastFixedDtoa(2.10861548515811875e+15, 17, buffer));
		CHECK_EQ("210861548515811875", buffer);
		CHECK_EQ(16, buffer.getPointPosition());
	}

	static class DataTestState {
		char[] buffer = new char[kBufferSize];
		public String underTest = "";
		int total = 0;
	}

	@Test
	public void gayFixed() throws Exception {
		DtoaTest.DataTestState state = new DtoaTest.DataTestState();
		DoubleTestHelper.eachFixed(state, (st, v, numberDigits, representation, decimalPoint) -> {
			st.total++;

			st.underTest = String.format("Using {%g, \"%s\", %d}", v, representation, decimalPoint);
			boolean status = FixedDtoa.fastFixedDtoa(v, numberDigits, st.buffer);

			assertThat(st.underTest, status, is(true));
			assertThat(st.underTest, st.buffer.getPointPosition(), is(equalTo(decimalPoint)));

			assertThat(st.underTest, st.buffer.getPointPosition(), is(equalTo(decimalPoint)));
			assertThat(st.underTest, (st.buffer.length() - st.buffer.getPointPosition()), is(lessThanOrEqualTo(numberDigits)));
			assertThat(st.underTest, stringOf(st.buffer), is(equalTo(representation)));
		});

		System.out.println("day-precision tests run :" + Integer.toString(state.total));
	}
}
