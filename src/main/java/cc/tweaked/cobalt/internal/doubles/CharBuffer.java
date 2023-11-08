// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.cobalt.internal.doubles;

/**
 * A buffer to which characters can be appended.
 */
public interface CharBuffer {
	/**
	 * Append a string to this buffer.
	 *
	 * @param str The string to append.
	 * @return {@code this}, for chaining.
	 */
	CharBuffer append(String str);

	/**
	 * Append a single character to this buffer.
	 *
	 * @param chr The character to append.
	 */
	void append(char chr);

	/**
	 * Append a slice of characters to this buffer.
	 *
	 * @param chars  The characters to append.
	 * @param offset The offset into this array.
	 * @param length The length of the slice to append.
	 * @throws ArrayIndexOutOfBoundsException If the slice extends beyond the array's bounds.
	 */
	void append(char[] chars, int offset, int length);
}
