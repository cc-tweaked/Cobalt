package org.squiddev.cobalt.vm;

import org.junit.Before;
import org.junit.Test;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StringTest {
	private LuaState state;

	@Before
	public void setup() throws Exception {
		state = new LuaState(new FileResourceManipulator());
		JsePlatform.standardGlobals(state);
	}

	@Test
	public void testToInputStream() throws IOException {
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

		LuaString substr = str.substring(1, 4);
		assertEquals(3, substr.length(state));

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
	public void testEncoding() throws UnsupportedEncodingException {
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
