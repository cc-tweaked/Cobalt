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

import cc.tweaked.cobalt.internal.string.NumberParser;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import static org.squiddev.cobalt.Constants.NIL;

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
@DefaultQualifier(NonNull.class)
public final class LuaString extends LuaValue implements Comparable<LuaString> {
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
	 * The contents of this string. Either a {@code byte[]} or a {@code LuaString[]}.
	 *
	 * @see #bytes()
	 * @see #flatten()
	 */
	private Object contents;

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
			if (cached != null && s.equals(cached)) {
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
	 * Create a string from a concatenation of other strings. This may be more efficient than building a string
	 * with {@link Buffer} as it defers allocating the underlying byte array.
	 *
	 * @param contents  The array of strings to build this from. All values in the range {@code [offset, offset+length)}
	 *                  must be {@link LuaString}s.
	 * @param offset    The offset into this array.
	 * @param length    The number of values in this array.
	 * @param strLength The length of the resulting string. This must be equal
	 * @return The resulting Lua string.
	 */
	public static LuaString valueOfStrings(LuaValue[] contents, int offset, int length, int strLength) {
		if (length == 0 || strLength == 0) return Constants.EMPTYSTRING;
		if (length == 1) return (LuaString) contents[0];

		if (strLength > RECENT_STRINGS_MAX_LENGTH) {
			LuaString[] slice = new LuaString[length];
			System.arraycopy(contents, offset, slice, 0, length);
			return new LuaString(slice, strLength);
		}

		byte[] out = new byte[strLength];
		int position = 0;
		for (int i = 0; i < length; i++) position = ((LuaString) contents[offset + i]).copyTo(out, position);
		return valueOf(out);
	}

	/**
	 * Construct a {@link LuaString} around a byte array without copying the contents.
	 * <p>
	 * The array is used directly after this is called, so clients must not change contents.
	 *
	 * @param contents byte buffer
	 * @param offset   offset into the byte buffer
	 * @param length   length of the byte buffer
	 */
	private LuaString(byte[] contents, int offset, int length) {
		super(Constants.TSTRING);
		this.contents = contents;
		this.offset = offset;
		this.length = length;
	}

	private LuaString(LuaValue[] contents, int length) {
		super(Constants.TSTRING);
		this.contents = contents;
		offset = 0;
		this.length = length;
	}

	@Override
	public String toString() {
		return decode(bytes(), offset, length);
	}

	@Override
	public LuaTable getMetatable(LuaState state) {
		return state.stringMetatable;
	}

	public int length() {
		return length;
	}

	private byte[] bytes() {
		Object contents = this.contents;
		if (contents instanceof byte[] bytes) return bytes;
		return flatten();
	}

	/**
	 * Flatten a nested list of {@link LuaString}s into a single {@link byte[]}.
	 *
	 * @return The flattened array.
	 */
	private byte[] flatten() {
		byte[] out = new byte[length];
		int position = 0;

		// We maintain a stack of values to avoid blowing the actual stack. Previous versions used to maintain this
		// stack by maintaining a linked list on the rope (avoiding allocations), but it's not clear if that's worth it
		// in practice.
		Deque<LuaString> queue;
		LuaString string;

		// Try to handle the easy case of having a list of byte[]s. In this case, we don't have to recurse, so can avoid
		// allocating the stack.
		rope:
		{
			LuaString[] strings = (LuaString[]) contents;
			for (int i = 0; i < strings.length; i++) {
				string = strings[i];
				Object contents = string.contents;
				if (contents instanceof byte[] bytes) {
					System.arraycopy(bytes, string.offset, out, position, string.length);
					position += string.length;
				} else {
					// We've got a more complex value, so add the remaining values to the queue and then begin to work.
					queue = new ArrayDeque<>(Math.max(4, strings.length - i));
					i++;
					for (; i < strings.length; i++) queue.addLast(strings[i]);
					break rope;
				}
			}

			contents = out;
			return out;
		}

		// If we were unable to unpack the string in the initial pass, loop through expanding the rope.
		while (true) {
			Object contents = string.contents;
			if (contents instanceof byte[] bytes) {
				System.arraycopy(bytes, string.offset, out, position, string.length);
				position += string.length;

				string = queue.pollFirst();
				if (string == null) break;
			} else {
				LuaString[] newStrings = (LuaString[]) contents;
				for (int i = newStrings.length - 1; i > 0; i--) queue.addFirst(newStrings[i]);
				string = newStrings[0];
			}
		}

		contents = out;
		return out;
	}

	//region Equality and comparison
	@Override
	public int compareTo(LuaString rhs) {
		byte[] bytes = bytes(), rhsBytes = rhs.bytes();
		// Find the first mismatched character in 0..n
		int len = Math.min(length, rhs.length);
		int mismatch = Arrays.mismatch(bytes, offset, offset + len, rhsBytes, rhs.offset, rhs.offset + len);
		if (mismatch >= 0) return Byte.compareUnsigned(bytes[offset + mismatch], rhsBytes[rhs.offset + mismatch]);

		// If one is a prefix of the other, sort by length.
		return length - rhs.length;
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof LuaString str && equals(str));
	}

	private boolean equals(LuaString s) {
		if (this == s) return true;
		if (s.length != length) return false;
		if (contents == s.contents && s.offset == offset) return true;
		if (s.hashCode() != hashCode()) return false;

		return equals(bytes(), offset, s.bytes(), s.offset, length);
	}

	public static boolean equals(LuaString a, int aOffset, LuaString b, int bOffset, int length) {
		return equals(a.bytes(), a.offset + aOffset, b.bytes(), b.offset + bOffset, length);
	}

	private static boolean equals(byte[] a, int aOffset, byte[] b, int bOffset, int length) {
		return Arrays.equals(a, aOffset, aOffset + length, b, bOffset, bOffset + length);
	}

	@Override
	public int hashCode() {
		int h = hashCode;
		if (h != 0) return h;

		byte[] bytes = bytes();

		h = length;  /* seed */
		int step = (length >> 5) + 1;  /* if string is too long, don't hash all its chars */
		for (int l1 = length; l1 >= step; l1 -= step)  /* compute hash */ {
			h = h ^ ((h << 5) + (h >> 2) + (((int) bytes[offset + l1 - 1]) & 0x0FF));
		}
		return hashCode = h;
	}
	// endregion

	// region String operations
	public LuaString substringOfLen(int beginIndex, int length) {
		return valueOf(bytes(), offset + beginIndex, length);
	}

	public LuaString substringOfEnd(int beginIndex, int endIndex) {
		return valueOf(bytes(), offset + beginIndex, endIndex - beginIndex);
	}

	public LuaString substring(int beginIndex) {
		return valueOf(bytes(), offset + beginIndex, length - 1);
	}

	public byte byteAt(int index) {
		if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
		return bytes()[offset + index];
	}

	public int charAt(int index) {
		if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
		return Byte.toUnsignedInt(bytes()[offset + index]);
	}

	public boolean startsWith(byte character) {
		return length != 0 && byteAt(0) == character;
	}

	/**
	 * Java version of strpbrk - find index of any byte that in an accept string.
	 *
	 * @param accept {@link LuaString} containing characters to look for.
	 * @return index of first match in the {@code accept} string, or -1 if not found.
	 */
	public int indexOfAny(LuaString accept) {
		byte[] bytes = bytes(), acceptBytes = accept.bytes();
		final int limit = offset + length;
		final int searchLimit = accept.offset + accept.length;
		for (int i = offset; i < limit; ++i) {
			for (int j = accept.offset; j < searchLimit; ++j) {
				if (bytes[i] == acceptBytes[j]) return i - offset;
			}
		}
		return -1;
	}

	/**
	 * Find the index of a byte in this string.
	 *
	 * @param b the byte to look for
	 * @return index of first match found, or -1 if not found.
	 */
	public int indexOf(byte b) {
		byte[] bytes = bytes();
		for (int i = 0, j = offset; i < length; ++i) {
			if (bytes[j++] == b) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Find the index of a string starting at a point in this string
	 *
	 * @param search the string to search for
	 * @param start  the first index in the string
	 * @return index of first match found, or -1 if not found.
	 */
	public int indexOf(LuaString search, int start) {
		byte[] bytes = bytes(), searchBytes = search.bytes();
		final int searchLen = search.length();
		final int limit = offset + length - searchLen;
		for (int i = offset + start; i <= limit; ++i) {
			if (equals(bytes, i, searchBytes, search.offset, searchLen)) {
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
		byte[] bytes = bytes();
		for (int i = offset + length - 1; i >= offset; i--) {
			if (bytes[i] == c) return i;
		}
		return -1;
	}
	// endregion

	// region Byte export

	/**
	 * Write this string to a {@link DataOutput}.
	 *
	 * @param output The output to write this to.
	 * @throws IOException If the underlying writer fails.
	 */
	public void write(DataOutput output) throws IOException {
		output.write(bytes(), offset, length);
	}

	/**
	 * Write this string to a {@link OutputStream}.
	 *
	 * @param output The output to write this to.
	 * @throws IOException If the underlying writer fails.
	 */
	public void write(OutputStream output) throws IOException {
		output.write(bytes(), offset, length);
	}

	/**
	 * Convert value to an input stream.
	 *
	 * @return {@link InputStream} whose data matches the bytes in this {@link LuaString}
	 */
	public InputStream toInputStream() {
		return new ByteArrayInputStream(bytes(), offset, length);
	}

	/**
	 * Convert this string to a {@link ByteBuffer}.
	 *
	 * @return A view over the underlying string.
	 */
	public ByteBuffer toBuffer() {
		return ByteBuffer.wrap(bytes(), offset, length).asReadOnlyBuffer();
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
		System.arraycopy(bytes(), offset + strOffset, bytes, arrayOffset, len);
		return arrayOffset + len;
	}

	/**
	 * Copy the bytes of the string into the given byte array.
	 *
	 * @param dest       destination byte array
	 * @param destOffset offset in destination
	 * @return The next byte free
	 */
	public int copyTo(byte[] dest, int destOffset) {
		// We could avoid unpacking the bytes here, but it's not clear it's worth it.
		System.arraycopy(bytes(), offset, dest, destOffset, length);
		return destOffset + length;
	}
	// endregion

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

	// region Number conversion
	@Override
	public int checkInteger() throws LuaError {
		return (int) (long) checkDouble();
	}

	@Override
	public long checkLong() throws LuaError {
		return (long) checkDouble();
	}

	@Override
	public double checkDouble() throws LuaError {
		double d = scanNumber(10);
		if (Double.isNaN(d)) {
			throw ErrorFactory.argError(this, "number");
		}
		return d;
	}

	@Override
	public LuaNumber checkNumber() throws LuaError {
		return ValueFactory.valueOf(checkDouble());
	}

	@Override
	public LuaNumber checkNumber(String msg) throws LuaError {
		double d = scanNumber(10);
		if (Double.isNaN(d)) {
			throw new LuaError(msg);
		}
		return ValueFactory.valueOf(d);
	}

	@Override
	public LuaValue toNumber() {
		return toNumber(10);
	}

	@Override
	public boolean isNumber() {
		double d = scanNumber(10);
		return !Double.isNaN(d);
	}

	@Override
	public double toDouble() {
		return scanNumber(10);
	}

	@Override
	public int toInteger() {
		return (int) (long) toDouble();
	}

	@Override
	public LuaValue toLuaString() {
		return this;
	}

	@Override
	public String checkString() {
		return toString();
	}

	@Override
	public LuaString checkLuaString() {
		return this;
	}

	/**
	 * convert to a number using a supplied base, or NIL if it can't be converted
	 *
	 * @param base the base to use, such as 10
	 * @return {@link LuaNumber} or {@link Constants#NIL} depending on the content of the string.
	 * @see LuaValue#toNumber()
	 */
	public LuaValue toNumber(int base) {
		double d = scanNumber(base);
		return Double.isNaN(d) ? NIL : ValueFactory.valueOf(d);
	}

	private double scanNumber(int base) {
		if (base < 2 || base > 36) return Double.NaN;
		return NumberParser.parse(bytes(), offset, length, base);
	}

	// endregion
}
