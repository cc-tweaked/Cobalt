package org.squiddev.cobalt.lib.fmt;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DoubleConversionCompatibility {
	public static void CHECK(boolean v) {
		assertTrue(v);
	}

	public static void CHECK_EQ(String left, char[] right) {
		int len = strlen(right);
		assertEquals(left, String.copyValueOf(right, 0, len));
	}

	private static int strlen(char[] chars) {
		int i = 0;
		while (chars[i] != '\0' && i < chars.length) {
			i++;
		}
		return i;
	}

	public static <T, U> void CHECK_EQ(T left, U right) {
		assertEquals(left, right);
	}

	public static <T extends Comparable<T>> void CHECK_GE(T left, T right) {
		assertThat(left,
				greaterThanOrEqualTo(right));
	}


	private DoubleConversionCompatibility() {}
}
