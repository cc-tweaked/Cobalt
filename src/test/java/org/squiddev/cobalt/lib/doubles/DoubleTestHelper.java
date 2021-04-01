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

import org.checkerframework.checker.signedness.qual.Unsigned;

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
	public static void CHECK_EQ(String expected, char[] actual) {
		assertEquals(expected, stringOf(actual));
	}

	public static void CHECK_EQ(UnsignedLong expected, @Unsigned long actual) {
		assertEquals(expected.unsafeLongValue(), actual);
	}


	public static void CHECK_EQ(@Unsigned long expected, UnsignedLong actual) {
		assertEquals(expected, actual.unsafeLongValue());
	}

	/** comparing booleans with numeric values, that's so 1990s */
	public static void CHECK_EQ(int expected, boolean actual) {
		boolean ex = expected != 0;
		assertEquals(ex, actual);
	}

	public static <T, U> void CHECK_EQ(T expected, U actual) {
		assertEquals(expected, actual);
	}


	public static String stringOf(char[] chars) {
		return String.copyValueOf(chars, 0, strlen(chars));
	}

	public static int strlen(char[] chars) {
		int len = chars.length;
		int i = 0;
		while (i < len) {
			if (chars[i] == '\0') return i;
			i++;
		}
		return i;
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

	@SuppressWarnings("InfiniteLoopStatement")
	public static <T>void eachShortestSingle(T initialState, ShortestSingleCallback<T> cb) throws Exception {
		URL rsrc = DoubleTestHelper.class.getResource("/double-convert/gay-shortest-single.txt");
		assertThat("File 'gay-shortest-single.txt' not found", rsrc, is(notNullValue()));

		float v;
		String representation;
		int decimalPoint;

		int total = 0;

		try (BufferedReader in = Files.newBufferedReader(Paths.get(rsrc.toURI()))) {
			TokenStream ts = new TokenStream(in);
			while (!ts.isEof()) {
				try {
					v = ts.t('{').nextFloat();
					representation = ts.t(',').nextString();
					decimalPoint = ts.t(',').nextInt();
				} catch (TokenStream.EofException e) {
					throw new IllegalStateException("Unepected end of file 'gay-shortest-single.txt'", e);
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

	public static interface ShortestSingleCallback<T> {
		void accept(T state, float v, String representation, int decimalPoint) throws Exception;
	}

	public static interface FixedCallback<T> {
		void accept(T state, double v, int numberDigits, String representation, int decimalPoint) throws Exception;
	}


	private DoubleTestHelper() {}
}
