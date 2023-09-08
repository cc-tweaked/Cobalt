/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
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
package org.squiddev.cobalt;

import cc.tweaked.cobalt.internal.doubles.CharBuffer;

/**
 * String buffer for use in string library methods, optimized for producing {@link LuaString} instances.
 *
 * @see LuaValue
 * @see LuaString
 */
public final class Buffer implements CharBuffer {
	/**
	 * Default capacity for a buffer: 64
	 */
	private static final int DEFAULT_CAPACITY = 64;

	/**
	 * Bytes in this buffer
	 */
	private byte[] bytes;

	/**
	 * Length of this buffer
	 */
	private int length;

	/**
	 * Create buffer with default capacity
	 *
	 * @see #DEFAULT_CAPACITY
	 */
	public Buffer() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * Create buffer with specified initial capacity
	 *
	 * @param initialCapacity the initial capacity
	 */
	public Buffer(int initialCapacity) {
		bytes = new byte[initialCapacity];
		length = 0;
	}

	/**
	 * Convert the buffer to a {@link LuaString}
	 *
	 * @return the value as a {@link LuaString}
	 */
	public LuaString toLuaString() {
		realloc(length);
		return LuaString.valueOf(bytes, 0, length);
	}

	/**
	 * Convert the buffer to a Java String
	 *
	 * @return the value as a Java String
	 */
	@Override
	public String toString() {
		return toLuaString().toString();
	}

	/**
	 * Append a single byte to the buffer.
	 *
	 * @param b The byte to append
	 */
	public void append(byte b) {
		ensure(1);
		bytes[length++] = b;
	}

	/**
	 * Append an array of bytes to the buffer.
	 *
	 * @param b The bytes to append
	 */
	public void append(byte[] b) {
		ensure(b.length);
		System.arraycopy(b, 0, bytes, length, b.length);
		length += b.length;
	}

	/**
	 * Append a region of bytes to the buffer.
	 *
	 * @param b      The bytes to append
	 * @param start  The start index
	 * @param length The number of values to append
	 */
	public Buffer append(byte[] b, int start, int length) {
		ensure(length);
		System.arraycopy(b, start, bytes, this.length, length);
		this.length += length;
		return this;
	}

	/**
	 * Append a single character to the buffer.
	 *
	 * @param c The byte to append
	 */
	public void append(char c) {
		ensure(1);
		bytes[length++] = c < 256 ? (byte) c : 63;
	}

	/**
	 * Append a region of characters to the buffer.
	 *
	 * @param chars  The characters to append
	 * @param start  The start index
	 * @param length The number of values to append
	 */
	public void append(char[] chars, int start, int length) {
		ensure(length);
		int j = this.length;
		for (int i = start; i < start + length; i++, j++) {
			char c = chars[i];
			bytes[j] = c < 256 ? (byte) c : 63;
		}
		this.length += length;
	}

	/**
	 * Append a {@link LuaString} to the buffer.
	 *
	 * @param str The string to append
	 * @return {@code this}, for chaining.
	 */
	public Buffer append(LuaString str) {
		ensure(str.length());
		length = str.copyTo(bytes, length);
		return this;
	}

	/**
	 * Append a {@link LuaString} to the buffer.
	 *
	 * @param str The string to append
	 * @return {@code this}, for chaining.
	 */
	public Buffer append(LuaString str, int start, int srcLength) {
		ensure(length);
		length = str.copyTo(start, bytes, length, srcLength);
		return this;
	}

	/**
	 * Append a Java String to the buffer.
	 * The Java string will be converted to bytes by limiting between 0 and 255
	 *
	 * @param str The string to append
	 * @return {@code this}, for chaining.
	 * @see LuaString#encode(String, byte[], int)
	 */
	@Override
	public Buffer append(String str) {
		final int n = str.length();
		ensure(n);
		LuaString.encode(str, bytes, length);
		length += n;
		return this;
	}

	/**
	 * Ensure there is enough room before and after the bytes.
	 *
	 * @param space number of unused bytes which must follow the data after this completes
	 */
	public void ensure(int space) {
		int newLength = length + space;
		if (bytes.length >= newLength) return;

		int m = newLength < 32 ? 32 : Math.max(newLength, length * 2);
		realloc(m);
	}

	/**
	 * Reallocate the internal storage for the buffer
	 *
	 * @param newSize the size of the buffer to use
	 */
	private void realloc(int newSize) {
		if (newSize == bytes.length) return;

		byte[] newBytes = new byte[newSize];
		System.arraycopy(bytes, 0, newBytes, 0, length);
		bytes = newBytes;
	}
}
