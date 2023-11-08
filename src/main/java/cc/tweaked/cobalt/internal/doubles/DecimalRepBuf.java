/*
 * Copyright 2021 sir-maniac
 * Portions copyright 2010-2012 the V8 project authors.
 *
 * All rights reserved.
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

import org.checkerframework.checker.signedness.qual.SignednessGlb;
import org.checkerframework.checker.signedness.qual.Unsigned;

import java.util.Arrays;

import static cc.tweaked.cobalt.internal.doubles.UnsignedValues.digitToChar;

/**
 * A decimal representation buffer, this contains the final digits ready for formatting, the position
 * of the decimal point, and the sign of the number.
 * Also includes point position {@code pointPosition}
 */
final class DecimalRepBuf {
	private static final char ASCII_ZERO = '0';
	@SuppressWarnings("ImplicitNumericConversion")
	private static final int DECIMAL_OVERFLOW = 10 + '0';

	private final char[] buffer;
	private boolean sign;
	private int length;
	private int pointPosition;

	/**
	 *
	 */
	public DecimalRepBuf(int size) {
		this.buffer = new char[size];
		length = 0;
		pointPosition = 0;
	}

	public void addLength(int size) {
		length += size;
	}

	// method overridden to avoid implicit casts
	public void append(@Unsigned long digit) {
		doAppend(digitToChar(digit));
	}

	public void append(@Unsigned int digit) {
		doAppend(digitToChar(digit));
	}

	public char charAt(int pos) {
		return buffer[pos];
	}

	public char lastChar() {
		return buffer[length - 1];
	}

	/**
	 * Increment the last value, carrying to more significant digit if last value is '9'.
	 * If all values are 9, the first digit is changed to '1', the remaining digits are set to '0',
	 * and 1 is returned as an overflow value;
	 * Used for rounding in {@link FastDtoa#roundWeedCounted}
	 *
	 * @return 1 if all digits were '9' (overflow) or 0 if no overflow
	 */
	public int incrementLast() {
		int overflow = 0;

		buffer[length - 1]++;

		// Increment the last digit recursively until we find a non '9' digit.
		for (int i = length - 1; i > 0; --i) {
			if ((int) buffer[i] != DECIMAL_OVERFLOW) break;
			buffer[i] = ASCII_ZERO;
			buffer[i - 1]++;
		}
		// If the first digit is now '0'+ 10 we had a buffer with all '9's. With the
		// exception of the first digit all digits are now '0'. Simply switch the
		// first digit to '1' and adjust the kappa. Example: "99" becomes "10" and
		// the power (the kappa) is increased.
		if ((int) buffer[0] == DECIMAL_OVERFLOW) {
			buffer[0] = '1';
			overflow = 1;
		}

		return overflow;
	}


	public void incrementLastNoOverflow() {
		if ((int) buffer[length - 1] == (int) '9') {
			throw new ArithmeticException("Last digit is '9' and so would overflow");
		}
		buffer[length - 1]++;
	}

	public int length() {
		return length;
	}

	/**
	 * Reverse the characters of the buffer, starting with start to the end of the buffer.
	 * Used by {@link FixedDtoa#fillDigits32(int, DecimalRepBuf)}
	 *
	 * @param start the start position to do the reverse
	 */
	public void reverseLast(int start) {
		char t;
		for (int i = start, j = length - 1; i < j; i++, j--) {
			t = buffer[i];
			buffer[i] = buffer[j];
			buffer[j] = t;
		}
	}

	/**
	 * reset length to zero, while keeping the pointPosition, eseectially clearing the buffer
	 */
	public void clearBuf() {
		length = 0;
	}

	/**
	 * reset the length and pointPosition to zero, preserving the sign.
	 */
	public void reset() {
		length = 0;
		pointPosition = 0;
	}

	/**
	 * Rounds the buffer values up.
	 * <p>
	 * If the entire buffer is '9', the buffer is set to '1' with trailing zeros,
	 * and the pointPosition is incremented by 1.
	 * <p>
	 * Used by {@link FixedDtoa#}
	 */
	public void roundUp() {
		if (length() == 0) {
			// An empty buffer represents 0.
			append(1);
			setPointPosition(1);
		} else {
			pointPosition += incrementLast();
		}
	}

	public void setCharAt(int index, @Unsigned int digit) {
		if (index >= length) throw new IndexOutOfBoundsException("index: " + index);
		buffer[index] = digitToChar(digit);
	}

	public CharSequence subSequence(int start, int end) {
		return new CharSequence() {
			@Override
			public int length() {
				return end - start;
			}

			@Override
			public char charAt(int index) {
				return buffer[start + index];
			}

			@Override
			public CharSequence subSequence(int st, int e) {
				return DecimalRepBuf.this.subSequence(start + st, start + e);
			}

			@Override
			public String toString() {
				return String.valueOf(buffer, start, end - start);
			}
		};
	}

	@Override
	public String toString() {
		return String.valueOf(buffer, 0, length);
	}

	/**
	 * Remove the leading and trailing zeros.
	 * If leading zeros are removed then the decimal point position is adjusted.
	 */
	public void trimZeros() {
		while (length > 0 && buffer[length - 1] == ASCII_ZERO) {
			length--;
		}

		int firstNonZero = 0;
		while (firstNonZero < length && buffer[firstNonZero] == ASCII_ZERO) {
			firstNonZero++;
		}
		if (firstNonZero != 0) {
			System.arraycopy(buffer, firstNonZero, buffer, 0, length - firstNonZero);
			length -= firstNonZero;
			pointPosition -= firstNonZero;
		}
	}

	/**
	 * remove trailing zeros to the right of the dsecimal point
	 *
	 * @param exponential if true, remove zeros up to the first character, if false,
	 *                    stop at {@code pointPosition}
	 */
	public void truncateZeros(boolean exponential) {
		int stop = exponential ? 1 : Math.max(1, pointPosition);
		while (length > stop && buffer[length - 1] == ASCII_ZERO) {
			length--;
		}
	}

	public void truncateAllZeros() {
		while (length > 0 && buffer[length - 1] == ASCII_ZERO) {
			length--;
		}
	}

	/**
	 * extend buffer to targetLength with zeros
	 *
	 * @param targetLength the length the buffer should be after extending
	 */
	public void zeroExtend(int targetLength) {
		if (length < targetLength) {
			Arrays.fill((@SignednessGlb char[]) buffer, length, targetLength, '0');
			length = targetLength;
		}
	}

	private void doAppend(char ch) {
		buffer[length++] = ch;
	}

	public char[] getBuffer() {
		return buffer;
	}

	public boolean getSign() {
		return sign;
	}

	public void setSign(boolean sign) {
		this.sign = sign;
	}

	public int getPointPosition() {
		return pointPosition;
	}

	void setPointPosition(int point) {
		this.pointPosition = point;
	}

}
