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
package org.squiddev.cobalt.vm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringTest {
	private final LuaState state = new LuaState();

	@BeforeEach
	public void setup() throws LuaError {
		CoreLibraries.standardGlobals(state);
	}

	@Test
	public void testToInputStream() throws IOException, LuaError, UnwindThrowable {
		LuaString str = LuaString.valueOf("Hello");

		InputStream is = str.toInputStream();

		assertEquals('H', is.read());
		assertEquals('e', is.read());
		assertEquals(2, is.skip(2));
		assertEquals('o', is.read());
		assertEquals(-1, is.read());

		assertTrue(is.markSupported());

		is.reset();

		assertEquals('H', is.read());
		is.mark(4);

		assertEquals('e', is.read());
		is.reset();
		assertEquals('e', is.read());

		var length = LuaOperators.createUnOp(state, "#");
		LuaString substr = str.substringOfEnd(1, 4);
		assertEquals(3, length.apply(substr).toInteger());

		is.close();
		is = substr.toInputStream();

		assertEquals('e', is.read());
		assertEquals('l', is.read());
		assertEquals('l', is.read());
		assertEquals(-1, is.read());

		is = substr.toInputStream();
		is.reset();

		assertEquals('e', is.read());
	}


	private static String userFriendly(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0, n = s.length(); i < n; i++) {
			int c = s.charAt(i);
			if (c < ' ' || c >= 0x80) {
				sb.append("\\u").append(Integer.toHexString(0x10000 + c).substring(1));
			} else {
				sb.append((char) c);
			}
		}
		return sb.toString();
	}

	@Test
	public void testEncoding() {
		int i = 240;
		char[] c = {(char) (i), (char) (i + 1), (char) (i + 2), (char) (i + 3)};
		String before = new String(c) + " " + i + "-" + (i + 4);
		LuaString ls = LuaString.valueOf(before);
		String after = ls.toString();
		assertEquals(userFriendly(before), userFriendly(after));
	}

	@Test
	public void testNullTerminated() {
		char[] c = {'a', 'b', 'c', '\0', 'd', 'e', 'f'};
		String before = new String(c);
		LuaString ls = LuaString.valueOf(before);
		String after = ls.toString();
		assertEquals(userFriendly("abc\0def"), userFriendly(after));

	}
}
