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


/**
 * String buffer for use in string library methods, optimized for production
 * of StrValue instances.
 *
 * The buffer can begin initially as a wrapped {@link LuaValue}
 * and only when concatenation actually occurs are the bytes first copied.
 *
 * To convert back to a {@link LuaValue} again,
 * the function {@link Buffer#value()} is used.
 *
 * @see LuaValue
 * @see LuaString
 */
public final class Buffer {

	/**
	 * Default capacity for a buffer: 64
	 */
	private static final int DEFAULT_CAPACITY = 64;

	/**
	 * Shared static array with no bytes
	 */
	private static final byte[] NOBYTES = {};

	/**
	 * Bytes in this buffer
	 */
	private byte[] bytes;

	/**
	 * Length of this buffer
	 */
	private int length;

	/**
	 * Offset into the byte array
	 */
	private int offset;

	/**
	 * Value of this buffer, when not represented in bytes
	 */
	private LuaString value;

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
		offset = 0;
		value = null;
	}

	/**
	 * Create buffer with specified initial value
	 *
	 * @param value the initial value
	 */
	public Buffer(LuaString value) {
		bytes = NOBYTES;
		length = offset = 0;
		this.value = value;
	}

	/**
	 * Get buffer contents as a {@link LuaValue}
	 *
	 * @return value as a {@link LuaValue}, converting as necessary
	 */
	public LuaValue value() {
		return value != null ? value : this.toLuaString();
	}

	/**
	 * Set buffer contents as a {@link LuaValue}
	 *
	 * @param value value to set
	 * @return {@code this}
	 */
	public Buffer setvalue(LuaString value) {
		bytes = NOBYTES;
		offset = length = 0;
		this.value = value;
		return this;
	}

	/**
	 * Convert the buffer to a {@link LuaString}
	 *
	 * @return the value as a {@link LuaString}
	 */
	public final LuaString toLuaString() {
		realloc(length, 0);
		return LuaString.valueOf(bytes, offset, length);
	}

	/**
	 * Convert the buffer to a Java String
	 *
	 * @return the value as a Java String
	 */
	@Override
	public String toString() {
		return value().toString();
	}

	/**
	 * Append a single byte to the buffer.
	 *
	 * @param b The byte to append
	 * @return {@code this} to allow call chaining
	 */
	public final Buffer append(byte b) {
		makeRoom(0, 1);
		bytes[offset + length++] = b;
		return this;
	}

	/**
	 * Append an array of bytes to the buffer.
	 *
	 * @param b The bytes to append
	 * @return {@code this} to allow call chaining
	 */
	public final Buffer append(byte[] b) {
		makeRoom(0, b.length);
		System.arraycopy(b, 0, bytes, offset + length, b.length);
		length += b.length;
		return this;
	}

	/**
	 * Append a region of bytes to the buffer.
	 *
	 * @param b      The bytes to append
	 * @param start  The start index
	 * @param length The number of values to append
	 * @return {@code this} to allow call chaining
	 */
	public final Buffer append(byte[] b, int start, int length) {
		makeRoom(0, length);
		System.arraycopy(b, start, bytes, offset + this.length, length);
		this.length += length;
		return this;
	}

	/**
	 * Append a single character to the buffer.
	 *
	 * @param c The byte to append
	 * @return {@code this} to allow call chaining
	 */
	public final Buffer append(char c) {
		makeRoom(0, 1);
		bytes[offset + length++] = c < 256 ? (byte) c : 63;
		return this;
	}

	/**
	 * Append a character array to the buffer.
	 *
	 * @param chars The byte to append
	 * @return {@code this} to allow call chaining
	 */
	public final Buffer append(char[] chars) {
		final int n = chars.length;
		makeRoom(0, n);
		LuaString.encode(chars, bytes, offset + length);
		length += n;
		return this;
	}

	/**
	 * Append a region of characters to the buffer.
	 *
	 * @param chars  The characters to append
	 * @param start  The start index
	 * @param length The number of values to append
	 * @return {@code this} to allow call chaining
	 */
	public Buffer append(char[] chars, int start, int length) {
		makeRoom(0, length);
		int j = this.offset + this.length;
		for (int i = start; i < start+length; i++, j++) {
			char c = chars[i];
			bytes[j] = c < 256 ? (byte) c : 63;
		}
		this.length += length;
		return this;
	}

	/**
	 * Append a {@link LuaString} to the buffer.
	 *
	 * @param str The string to append
	 * @return {@code this} to allow call chaining
	 */
	public final Buffer append(LuaString str) {
		final int n = str.length;
		makeRoom(0, n);
		str.copyTo(0, bytes, offset + length, n);
		length += n;
		return this;
	}

	/**
	 * Append a Java String to the buffer.
	 * The Java string will be converted to bytes by limiting between 0 and 255
	 *
	 * @param str The string to append
	 * @return {@code this} to allow call chaining
	 * @see LuaString#encode(String, byte[], int)
	 */
	public final Buffer append(String str) {
		final int n = str.length();
		makeRoom(0, n);
		LuaString.encode(str, bytes, offset + length);
		length += n;
		return this;
	}

	/**
	 * Concatenate bytes from a {@link LuaString} onto the front of this buffer
	 *
	 * @param s the left-hand-side value which we will concatenate onto the front of {@code this}
	 * @return {@link Buffer} for use in call chaining.
	 */
	public Buffer prepend(LuaString s) {
		int n = s.length;
		makeRoom(n, 0);
		System.arraycopy(s.bytes, s.offset, bytes, offset - n, n);
		offset -= n;
		length += n;
		value = null;
		return this;
	}

	/**
	 * Ensure there is enough room before and after the bytes.
	 *
	 * @param nbefore number of unused bytes which must precede the data after this completes
	 * @param nafter  number of unused bytes which must follow the data after this completes
	 */
	public final void makeRoom(int nbefore, int nafter) {
		if (value != null) {
			LuaString s = value;
			value = null;
			length = s.length;
			offset = nbefore;
			bytes = new byte[nbefore + length + nafter];
			System.arraycopy(s.bytes, s.offset, bytes, offset, length);
		} else if (offset + length + nafter > bytes.length || offset < nbefore) {
			int n = nbefore + length + nafter;
			int m = n < 32 ? 32 : Math.max(n, length * 2);
			realloc(m, nbefore == 0 ? 0 : m - length - nafter);
		}
	}

	/**
	 * Reallocate the internal storage for the buffer
	 *
	 * @param newSize   the size of the buffer to use
	 * @param newOffset the offset to use
	 */
	private void realloc(int newSize, int newOffset) {
		if (newSize != bytes.length) {
			byte[] newBytes = new byte[newSize];
			System.arraycopy(bytes, offset, newBytes, newOffset, length);
			bytes = newBytes;
			offset = newOffset;
		}
	}

}
