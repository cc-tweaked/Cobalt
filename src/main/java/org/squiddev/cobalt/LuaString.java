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

import org.squiddev.cobalt.lib.StringLib;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Subclass of {@link LuaValue} for representing lua strings.
 * <p>
 * Because lua string values are more nearly sequences of bytes than
 * sequences of characters or unicode code points, the {@link LuaString}
 * implementation holds the string value in an internal byte array.
 * <p>
 * {@link LuaString} values are generally not mutable once constructed,
 * so multiple {@link LuaString} values can chare a single byte array.
 * <p>
 * Currently {@link LuaString}s are pooled via a centrally managed weak table.
 * To ensure that as many string values as possible take advantage of this,
 * Constructors are not exposed directly.  As with number, booleans, and nil,
 * instance construction should be via {@link ValueFactory#valueOf(byte[])} or similar API.
 *
 * @see LuaValue
 * @see ValueFactory#valueOf(String)
 * @see ValueFactory#valueOf(byte[])
 */
public final class LuaString extends LuaBaseString {
	/**
	 * Size of cache of recent short strings. This is the maximum number of LuaStrings that
	 * will be retained in the cache of recent short strings. Must be a power of 2.
	 */
	public static final int RECENT_STRINGS_CACHE_SIZE = 128;

	/**
	 * Maximum length of a string to be considered for recent short strings caching.
	 * This effectively limits the total memory that can be spent on the recent strings cache,
	 * because no LuaString whose backing exceeds this length will be put into the cache.
	 */
	public static final int RECENT_STRINGS_MAX_LENGTH = 32;

	/**
	 * The bytes for the string
	 */
	private final byte[] bytes;

	/**
	 * The offset into the byte array, 0 means start at the first byte
	 */
	private final int offset;

	/**
	 * The number of bytes that comprise this string
	 */
	private final int length;

	private int hashCode;

	private static class Cache {
		/**
		 * Simple cache of recently created strings that are short.
		 * This is simply a list of strings, indexed by their hash codes modulo the cache size
		 * that have been recently constructed.  If a string is being constructed frequently
		 * from different contexts, it will generally may show up as a cache hit and resolve
		 * to the same value.
		 */
		public final LuaString[] recentShortStrings = new LuaString[RECENT_STRINGS_CACHE_SIZE];

		public LuaString get(LuaString s) {
			final int index = s.hashCode() & (RECENT_STRINGS_CACHE_SIZE - 1);
			final LuaString cached = recentShortStrings[index];
			if (cached != null && s.raweq(cached)) {
				return cached;
			}
			recentShortStrings[index] = s;
			return s;
		}

		public static final Cache instance = new Cache();
	}

	/**
	 * Get a {@link LuaString} instance whose bytes match
	 * the supplied Java String which will be limited to the 0-255 range
	 *
	 * @param string Java String containing characters which will be limited to the 0-255 range
	 * @return {@link LuaString} with bytes corresponding to the supplied String
	 */
	public static LuaString valueOf(String string) {
		byte[] bytes = new byte[string.length()];
		encode(string, bytes, 0);
		return valueOf(bytes, 0, bytes.length);
	}

	/**
	 * Construct a {@link LuaString} around a byte array without copying the contents.
	 * <p>
	 * The array is used directly after this is called, so clients must not change contents.
	 *
	 * @param bytes byte buffer
	 * @param off   offset into the byte buffer
	 * @param len   length of the byte buffer
	 * @return {@link LuaString} wrapping the byte buffer
	 */
	public static LuaString valueOf(byte[] bytes, int off, int len) {
		if (bytes.length < RECENT_STRINGS_MAX_LENGTH) {
			// Short string.  Reuse the backing and check the cache of recent strings before returning.
			return Cache.instance.get(new LuaString(bytes, off, len));
		} else if (len >= bytes.length / 2) {
			// Reuse backing only when more than half the bytes are part of the result.
			return new LuaString(bytes, off, len);
		} else {
			// Short result relative to the source.  Copy only the bytes that are actually to be used.
			final byte[] b = new byte[len];
			System.arraycopy(bytes, off, b, 0, len);
			LuaString string = new LuaString(b, 0, len);
			return len < RECENT_STRINGS_MAX_LENGTH ? Cache.instance.get(string) : string;
		}
	}

	/**
	 * Construct a {@link LuaString} around a byte array without copying the contents.
	 * <p>
	 * The array is used directly after this is called, so clients must not change contents.
	 *
	 * @param bytes byte buffer
	 * @return {@link LuaString} wrapping the byte buffer
	 */
	public static LuaString valueOf(byte[] bytes) {
		return valueOf(bytes, 0, bytes.length);
	}

	/**
	 * Construct a {@link LuaString} around a byte array without copying the contents.
	 * <p>
	 * The array is used directly after this is called, so clients must not change contents.
	 *
	 * @param bytes  byte buffer
	 * @param offset offset into the byte buffer
	 * @param length length of the byte buffer
	 */
	private LuaString(byte[] bytes, int offset, int length) {
		super();
		this.bytes = bytes;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public String toString() {
		return decode(bytes, offset, length);
	}

	public int compare(LuaString rhs) {
		// Find the first mismatched character in 0..n
		int len = Math.min(length, rhs.length);
		int mismatch = Arrays.mismatch(bytes, offset, offset + len, rhs.bytes, rhs.offset, rhs.offset + len);
		if (mismatch >= 0) return Byte.compareUnsigned(bytes[offset + mismatch], rhs.bytes[rhs.offset + mismatch]);

		// If one is a prefix of the other, sort by length.
		return length - rhs.length;
	}

	@Override
	public LuaString strvalue() {
		return this;
	}

	public LuaString substringOfLen(int beginIndex, int length) {
		return valueOf(bytes, offset + beginIndex, length);
	}

	public LuaString substringOfEnd(int beginIndex, int endIndex) {
		return valueOf(bytes, offset + beginIndex, endIndex - beginIndex);
	}

	public LuaString substring(int beginIndex) {
		return valueOf(bytes, offset + beginIndex, length - 1);
	}

	@Override
	public int hashCode() {
		int h = hashCode;
		if (h != 0) return h;

		h = length;  /* seed */
		int step = (length >> 5) + 1;  /* if string is too long, don't hash all its chars */
		for (int l1 = length; l1 >= step; l1 -= step)  /* compute hash */ {
			h = h ^ ((h << 5) + (h >> 2) + (((int) bytes[offset + l1 - 1]) & 0x0FF));
		}
		return hashCode = h;
	}

	// object comparison, used in key comparison
	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof LuaValue && ((LuaValue) o).raweq(this));
	}

	// equality w/o metatable processing
	@Override
	public boolean raweq(LuaValue val) {
		return val.raweq(this);
	}

	@Override
	public boolean raweq(LuaString s) {
		if (this == s) return true;
		if (s.length != length) return false;
		if (s.bytes == bytes && s.offset == offset) return true;
		if (s.hashCode() != hashCode()) return false;

		return equals(bytes, offset, s.bytes, s.offset, length);
	}

	public static boolean equals(LuaString a, int aOffset, LuaString b, int bOffset, int length) {
		return equals(a.bytes, a.offset + aOffset, b.bytes, b.offset + bOffset, length);
	}

	private static boolean equals(byte[] a, int aOffset, byte[] b, int bOffset, int length) {
		return Arrays.equals(a, aOffset, aOffset + length, b, bOffset, bOffset + length);
	}

	public void write(DataOutput writer) throws IOException {
		writer.write(bytes, offset, length);
	}

	public void write(OutputStream writer) throws IOException {
		writer.write(bytes, offset, length);
	}

	@Override
	public int length() {
		return length;
	}

	public byte byteAt(int index) {
		if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
		return bytes[offset + index];
	}

	public int charAt(int index) {
		if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
		return Byte.toUnsignedInt(bytes[offset + index]);
	}

	public boolean startsWith(byte character) {
		return length != 0 && byteAt(0) == character;
	}

	/**
	 * Convert value to an input stream.
	 *
	 * @return {@link InputStream} whose data matches the bytes in this {@link LuaString}
	 */
	public InputStream toInputStream() {
		return new ByteArrayInputStream(bytes, offset, length);
	}

	/**
	 * Convert this string to a {@link ByteBuffer}.
	 *
	 * @return A view over the underlying string.
	 */
	public ByteBuffer toBuffer() {
		return ByteBuffer.wrap(bytes, offset, length).asReadOnlyBuffer();
	}

	/**
	 * Copy the bytes of the string into the given byte array.
	 *
	 * @param strOffset   offset from which to copy
	 * @param bytes       destination byte array
	 * @param arrayOffset offset in destination
	 * @param len         number of bytes to copy
	 * @return The next byte free
	 */
	public int copyTo(int strOffset, byte[] bytes, int arrayOffset, int len) {
		if (strOffset < 0 || len > length - strOffset) throw new IndexOutOfBoundsException();
		System.arraycopy(this.bytes, offset + strOffset, bytes, arrayOffset, len);
		return arrayOffset + len;
	}

	public int copyTo(byte[] dest, int destOffset) {
		System.arraycopy(bytes, offset, dest, destOffset, length);
		return destOffset + length;
	}

	/**
	 * Java version of strpbrk - find index of any byte that in an accept string.
	 *
	 * @param accept {@link LuaString} containing characters to look for.
	 * @return index of first match in the {@code accept} string, or -1 if not found.
	 */
	public int indexOfAny(LuaString accept) {
		final int limit = offset + length;
		final int searchLimit = accept.offset + accept.length;
		for (int i = offset; i < limit; ++i) {
			for (int j = accept.offset; j < searchLimit; ++j) {
				if (bytes[i] == accept.bytes[j]) {
					return i - offset;
				}
			}
		}
		return -1;
	}

	/**
	 * Find the index of a byte starting at a point in this string
	 *
	 * @param b     the byte to look for
	 * @param start the first index in the string
	 * @return index of first match found, or -1 if not found.
	 */
	public int indexOf(byte b, int start) {
		for (int i = 0, j = offset + start; i < length; ++i) {
			if (bytes[j++] == b) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Find the index of a string starting at a point in this string
	 *
	 * @param s     the string to search for
	 * @param start the first index in the string
	 * @return index of first match found, or -1 if not found.
	 */
	public int indexOf(LuaString s, int start) {
		final int slen = s.length();
		final int limit = offset + length - slen;
		for (int i = offset + start; i <= limit; ++i) {
			if (equals(bytes, i, s.bytes, s.offset, slen)) {
				return i - offset;
			}
		}
		return -1;
	}

	/**
	 * Find the last index of a character in this string
	 *
	 * @param c the character to search for
	 * @return index of last match found, or -1 if not found.
	 */
	public int lastIndexOf(byte c) {
		for (int i = offset + length - 1; i >= offset; i--) {
			if (bytes[i] == c) return i;
		}
		return -1;
	}


	/**
	 * Convert to Java String
	 *
	 * @param bytes  byte array to convert
	 * @param offset starting index in byte array
	 * @param length number of bytes to convert
	 * @return Java String corresponding to the value of bytes interpreted using UTF8
	 * @see #encode(String, byte[], int)
	 */
	private static String decode(byte[] bytes, int offset, int length) {
		char[] chars = new char[length];
		for (int i = 0; i < length; i++) {
			chars[i] = ((char) (bytes[offset + i] & 0xFF));
		}
		return String.valueOf(chars);
	}

	/**
	 * Encode the given Java string with characters limited to the 0-255 range,
	 * writing the result to bytes starting at offset.
	 * <p>
	 * The string should be measured first with lengthAsUtf8
	 * to make sure the given byte array is large enough.
	 *
	 * @param string Array of characters to be encoded
	 * @param bytes  byte array to hold the result
	 * @param off    offset into the byte array to start writing
	 * @see #decode(byte[], int, int)
	 */
	public static void encode(String string, byte[] bytes, int off) {
		int length = string.length();
		for (int i = 0; i < length; i++) {
			int c = string.charAt(i);
			bytes[i + off] = (c < 256 ? (byte) c : 63);
		}
	}

	// --------------------- number conversion -----------------------

	@Override
	public double scanNumber(int base) {
		if (base < 2 || base > 36) return Double.NaN;

		int i = offset, j = offset + length;
		while (i < j && StringLib.isWhitespace(bytes[i])) i++;
		while (i < j && StringLib.isWhitespace(bytes[j - 1])) j--;

		boolean isNeg = i < j && bytes[i] == '-';
		if (isNeg) i++;

		if (i >= j) return Double.NaN;

		if ((base == 10 || base == 16) && (bytes[i] == '0' && i + 1 < j && (bytes[i + 1] == 'x' || bytes[i + 1] == 'X'))) {
			base = 16;
			i += 2;

			if (i >= j) return Double.NaN;
		}

		double l = scanLong(base, bytes, i, j);
		double value = Double.isNaN(l) && base == 10 ? scanDouble(bytes, i, j) : l;
		return isNeg ? -value : value;
	}

	/**
	 * Scan and convert a long value, or return Double.NaN if not found.
	 *
	 * @param base  the base to use, such as 10
	 * @param start the index to start searching from
	 * @param end   the first index beyond the search range
	 * @return double value if conversion is valid,
	 * or Double.NaN if not
	 */
	private static double scanLong(int base, byte[] bytes, int start, int end) {
		long x = 0;
		for (int i = start; i < end; i++) {
			int digit = bytes[i] - (base <= 10 || (bytes[i] >= '0' && bytes[i] <= '9') ? '0' :
				bytes[i] >= 'A' && bytes[i] <= 'Z' ? ('A' - 10) : ('a' - 10));
			if (digit < 0 || digit >= base) {
				return Double.NaN;
			}
			x = x * base + digit;
		}
		return x;
	}

	/**
	 * Scan and convert a double value, or return Double.NaN if not a double.
	 *
	 * @param start the index to start searching from
	 * @param end   the first index beyond the search range
	 * @return double value if conversion is valid,
	 * or Double.NaN if not
	 */
	private static double scanDouble(byte[] bytes, int start, int end) {
		if (end > start + 64) end = start + 64;
		for (int i = start; i < end; i++) {
			switch (bytes[i]) {
				case '-':
				case '+':
				case '.':
				case 'e':
				case 'E':
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					break;
				default:
					return Double.NaN;
			}
		}
		char[] c = new char[end - start];
		for (int i = start; i < end; i++) {
			c[i - start] = (char) bytes[i];
		}
		try {
			return Double.parseDouble(String.valueOf(c));
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

}
