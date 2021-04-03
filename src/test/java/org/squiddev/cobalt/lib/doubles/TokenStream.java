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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StreamTokenizer;

public class TokenStream {
	private final StreamTokenizer st;

	/**
	 * @param r
	 * @throws IOException
	 */
	public TokenStream(BufferedReader r) throws IOException {
		st = new StreamTokenizer(r);
		st.resetSyntax();
		st.wordChars('a', 'z');
		st.wordChars('A', 'Z');
		st.wordChars(128 + 32, 255);
		st.whitespaceChars(0, ' ');
		st.commentChar('/');
		st.quoteChar('"');
		st.quoteChar('\'');
		// no way to customize numbers, so treat them as words and parse internally
		st.wordChars('0', '9');
		st.wordChars('-', '.');
		st.wordChars('+', '+');

		st.nextToken();
	}

	public boolean isEof() {
		return st.ttype == StreamTokenizer.TT_EOF;
	}

	public String nextString() throws IOException, TokenException {
		checkCurrent('"', "Expected '\"'");
		String sval = st.sval;
		st.nextToken();
		return sval;
	}

	public int nextInt() throws IOException, TokenException {
		checkCurrent(StreamTokenizer.TT_WORD, "Expected int");
		int val;
		try {
			val = Integer.parseInt(st.sval);
		} catch (NumberFormatException e) {
			throw new TokenException("Expected int, got: " + st.sval + " line " + st.lineno(), e);
		}
		st.nextToken();
		return val;
	}

	public double nextDouble() throws IOException, TokenException {
		checkCurrent(StreamTokenizer.TT_WORD, "Expected double");
		double val;
		try {
			val = Double.parseDouble(st.sval);
		} catch (NumberFormatException e) {
			throw new TokenException("Expected double, got: " + st.sval + " line " + st.lineno(), e);
		}
		st.nextToken();
		return val;
	}

	public float nextFloat() throws IOException, TokenException {
		checkCurrent(StreamTokenizer.TT_WORD, "Expected double");
		float val;
		try {
			val = Float.parseFloat(st.sval);
		} catch (NumberFormatException e) {
			throw new TokenException("Expected float, got: " + st.sval + " line " + st.lineno(), e);
		}
		st.nextToken();
		return val;
	}

	public TokenStream t(int expectedType) throws IOException, TokenException {
		return nextToken(expectedType);
	}

	public TokenStream nextToken(int expectedType) throws IOException, TokenException {
		checkCurrent(expectedType, String.format("Expected '%c'", expectedType));
		st.nextToken();
		return this;
	}

	private void checkCurrent(int expectedType, String expectedMessage) throws IOException, TokenException {
		int tt = st.ttype;
		if (tt == StreamTokenizer.TT_EOF) {
			throw new EofException("line " + st.lineno());
		} else if (tt != expectedType) {
			throw new TokenException(expectedMessage + " got " + ttToSring(tt) +
					 " line " + st.lineno());
		}
	}

	private String ttToSring(int tt) {
		if (tt > 0) return String.format("'%c'", tt);
		switch (tt) {
			case StreamTokenizer.TT_EOF: return "'TT_EOF'";
			case StreamTokenizer.TT_NUMBER: return "'TT_NUMBER'" + Double.toString(st.nval);
			case StreamTokenizer.TT_WORD: return "'TT_WORD':" + st.sval;
			case StreamTokenizer.TT_EOL: return "'TT_EOL'";
			default: return "unknown("+Integer.valueOf(tt)+")";
		}
	}

	public static class TokenException extends Exception {
		private static final long serialVersionUID = 1L;

		public TokenException() {
		}

		public TokenException(String message) {
			super(message);
		}

		public TokenException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static class EofException extends TokenException {
		private static final long serialVersionUID = 1L;

		public EofException() {
		}

		public EofException(String message) {
			super(message);
		}

		public EofException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
