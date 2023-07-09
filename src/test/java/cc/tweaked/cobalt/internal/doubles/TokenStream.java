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
		return switch (tt) {
			case StreamTokenizer.TT_EOF -> "'TT_EOF'";
			case StreamTokenizer.TT_NUMBER -> "'TT_NUMBER'" + Double.toString(st.nval);
			case StreamTokenizer.TT_WORD -> "'TT_WORD':" + st.sval;
			case StreamTokenizer.TT_EOL -> "'TT_EOL'";
			default -> "unknown(" + Integer.valueOf(tt) + ")";
		};
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
