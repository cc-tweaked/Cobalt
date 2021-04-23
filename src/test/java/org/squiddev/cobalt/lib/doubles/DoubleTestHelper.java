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

import java.io.BufferedReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DoubleTestHelper {
	public static void CHECK(boolean v) {
		assertTrue(v);
	}

	public static <T extends Comparable<T>> void CHECK_GE(T left, T right) {
		assertThat(left,
				greaterThanOrEqualTo(right));
	}

	public static <T extends Comparable<T>> void CHECK_GT(T left, T right) {
		assertThat(left, greaterThan(right));
	}

	/** special case when comparing buffers */
	public static void CHECK_EQ(String expected, DecimalRepBuf actual) {
		assertEquals(expected, stringOf(actual));
	}

	public static void CHECK_EQ(String expected, char[] actual) {
		assertEquals(expected, String.valueOf(actual));
	}

	/** comparing booleans with numeric values, that's so 1990s */
	public static void CHECK_EQ(int expected, boolean actual) {
		boolean ex = expected != 0;
		assertEquals(ex, actual);
	}

	public static void CHECK_EQ(String expected, String actual) {
		assertEquals(expected, actual);
	}

	public static void CHECK_EQ(int expected, int actual) {
		assertEquals(expected, actual);
	}

	public static void CHECK_EQ(long expected, long actual) {
		assertEquals(expected, actual);
	}

	public static void CHECK_EQ(double expected, double actual) {
		assertEquals(expected, actual);
	}

//	public static <T, U> void CHECK_EQ(T expected, U actual) {
//		assertEquals(expected, actual);
//	}

	public static String stringOf(DecimalRepBuf digits) {
		return digits.toString();
	}

	@SuppressWarnings("InfiniteLoopStatement")
	public static <T>void eachFixed(T initialState, FixedCallback<T> cb) throws Exception {
		URL rsrc = DoubleTestHelper.class.getResource("/double-convert/gay-fixed.txt");
		assertThat("File 'gay-fixed.txt' not found", rsrc, is(notNullValue()));

		int total = 0;

		try (BufferedReader in = Files.newBufferedReader(Paths.get(rsrc.toURI()))) {
			double v;
			int numberDigits;
			String representation;
			int decimalPoint;

			TokenStream ts = new TokenStream(in);
			while (!ts.isEof()) {
				try {
					v = ts.t('{').nextDouble();
					numberDigits = ts.t(',').nextInt();
					representation = ts.t(',').nextString();
					decimalPoint = ts.t(',').nextInt();
				} catch (TokenStream.EofException e) {
					throw new IllegalStateException("Unepected end of file 'gay-fixed.txt'", e);
				}

				cb.accept(initialState, v, numberDigits, representation, decimalPoint);

				total++;
				if (ts.isEof()) break;
				ts.t('}');
				if (ts.isEof()) break;
				ts.t(',');
			}
		}
	}

	@SuppressWarnings("InfiniteLoopStatement")
	public static <T>void eachPrecision(T initialState, FixedCallback<T> cb) throws Exception {
		URL rsrc = DoubleTestHelper.class.getResource("/double-convert/gay-precision.txt");
		assertThat("File 'gay-precision.txt' not found", rsrc, is(notNullValue()));

		int total = 0;

		try (BufferedReader in = Files.newBufferedReader(Paths.get(rsrc.toURI()))) {
			double v;
			int numberDigits;
			String representation;
			int decimalPoint;

			TokenStream ts = new TokenStream(in);
			while (!ts.isEof()) {
				try {
					v = ts.t('{').nextDouble();
					numberDigits = ts.t(',').nextInt();
					representation = ts.t(',').nextString();
					decimalPoint = ts.t(',').nextInt();
				} catch (TokenStream.EofException e) {
					throw new IllegalStateException("Unepected end of file 'gay-precision.txt'", e);
				}

				cb.accept(initialState, v, numberDigits, representation, decimalPoint);

				total++;
				if (ts.isEof()) break;
				ts.t('}');
				if (ts.isEof()) break;
				ts.t(',');
			}
		}
	}

	@SuppressWarnings("InfiniteLoopStatement")
	public static <T>void eachShortest(T initialState, ShortestCallback<T> cb) throws Exception {
		URL rsrc = DoubleTestHelper.class.getResource("/double-convert/gay-shortest.txt");
		assertThat("File 'gay-shortest.txt' not found", rsrc, is(notNullValue()));

		double v;
		String representation;
		int decimalPoint;

		int total = 0;

		try (BufferedReader in = Files.newBufferedReader(Paths.get(rsrc.toURI()))) {
			TokenStream ts = new TokenStream(in);
			while (!ts.isEof()) {
				try {
					v = ts.t('{').nextDouble();
					representation = ts.t(',').nextString();
					decimalPoint = ts.t(',').nextInt();
				} catch (TokenStream.EofException e) {
					throw new IllegalStateException("Unepected end of file 'gay-shortest.txt'", e);
				}

				cb.accept(initialState, v, representation, decimalPoint);

				total++;
				ts.t('}').t(',');
			}
		} catch (TokenStream.EofException ignore) {}  // ignore the trailing tokens
	}

	public static interface ShortestCallback<T> {
		void accept(T state, double v, String representation, int decimalPoint) throws Exception;
	}

	public static interface FixedCallback<T> {
		void accept(T state, double v, int numberDigits, String representation, int decimalPoint) throws Exception;
	}


	private DoubleTestHelper() {}
}
