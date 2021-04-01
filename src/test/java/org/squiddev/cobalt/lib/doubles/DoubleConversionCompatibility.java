package org.squiddev.cobalt.lib.doubles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DoubleConversionCompatibility {
	public static void CHECK(boolean v) {
		assertTrue(v);
	}

	public static <T extends Comparable<T>> void CHECK_GE(T left, T right) {
		assertThat(left,
				greaterThanOrEqualTo(right));
	}

	/** special case when comparing buffers */
	public static void CHECK_EQ(String expected, char[] actual) {
		assertEquals(expected, stringOf(actual));
	}

	/** comparing booleans with numeric values, that's so 1990s */
	public static void CHECK_EQ(int expected, boolean actual) {
		boolean ex = expected != 0;
		assertEquals(ex, actual);
	}

	public static <T, U> void CHECK_EQ(T expected, U actual) {
		assertEquals(expected, actual);
	}


	public static String stringOf(char[] chars) {
		return String.copyValueOf(chars, 0, strlen(chars));
	}

	public static int strlen(char[] chars) {
		int len = chars.length;
		int i = 0;
		while (i < len) {
			if (chars[i] == '\0') return i;
			i++;
		}
		return i;
	}

	private DoubleConversionCompatibility() {}
}
