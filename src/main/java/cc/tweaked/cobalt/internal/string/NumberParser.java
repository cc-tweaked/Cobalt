package cc.tweaked.cobalt.internal.string;

import org.squiddev.cobalt.lib.StringLib;

/**
 * Parsers numbers, including hexadecimal integers and doubles.
 * <p>
 * This is used by the lexer/parser and {@code tonumber}.
 */
public final class NumberParser {
	private NumberParser() {
	}

	public static double parse(byte[] bytes, int start, int length, int base) {
		int end = start + length;
		while (start < end && StringLib.isWhitespace(bytes[start])) start++;
		while (start < end && StringLib.isWhitespace(bytes[end - 1])) end--;

		var originalStart = start;
		boolean isNeg = false;
		if (start < end) {
			switch (bytes[start]) {
				case '+' -> start++;
				case '-' -> {
					start++;
					isNeg = true;
				}
			}
		}

		if (start >= end) return Double.NaN;

		if ((base == 10 || base == 16) && (bytes[start] == '0' && start + 1 < end && (bytes[start + 1] == 'x' || bytes[start + 1] == 'X'))) {
			base = 16;
			start += 2;

			if (start >= end) return Double.NaN;
		}

		return switch (base) {
			case 10 -> scanDouble(bytes, originalStart, end);
			case 16 -> scanHexDouble(bytes, start, end, isNeg);
			default -> scanLong(base, bytes, start, end, isNeg);
		};
	}

	/**
	 * Scan and convert a long value, or return Double.NaN if not found.
	 *
	 * @param base  the base to use, such as 10
	 * @param start the index to start searching from
	 * @param end   the first index beyond the search range
	 * @param isNeg Whether this is a negative value.
	 * @return double value if conversion is valid,
	 * or Double.NaN if not
	 */
	private static double scanLong(int base, byte[] bytes, int start, int end, boolean isNeg) {
		double x = 0;
		for (int i = start; i < end; i++) {
			var chr = bytes[i];
			int digit;
			if (CharProperties.isDigit(chr)) {
				digit = chr - '0';
			} else if (CharProperties.isAlpha(chr)) {
				digit = (chr | 0x20) - 'a' + 10;
			} else {
				return Double.NaN;
			}

			if (digit >= base) return Double.NaN;
			x = x * base + digit;
		}
		return isNeg ? -x : x;
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
		char[] c = new char[end - start];
		for (int i = start; i < end; i++) {
			var b = bytes[i];
			if (!isValidDoubleCharacter(b)) return Double.NaN;
			c[i - start] = (char) b;
		}
		try {
			return Double.parseDouble(String.valueOf(c));
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	private static boolean isValidDoubleCharacter(byte b) {
		return (b >= '0' && b <= '9') || b == '+' || b == '-' || b == '.' || b == 'E' || b == 'e';
	}

	private static double scanHexDouble(byte[] bytes, int index, int end, boolean isNeg) {
		double result = 0; // The mantissa
		int exponent = 0;

		int sigDigits = 0, nonSigDigits = 0; // Number of significant digits and non-significant digits (leading 0s).
		boolean hasDot = false;
		for (; index < end; index++) {
			var s = bytes[index];
			if (s == '.') {
				if (hasDot) return Double.NaN;
				hasDot = true;
			} else if (CharProperties.isHex(s)) {
				if (sigDigits == 0 && s == '0') {
					// Skip leading 0s
					nonSigDigits++;
				} else if (++sigDigits <= 30) {
					// Only allow up-to 30 significant digits.
					result = result * 16 + CharProperties.hexValue(s);
				} else {
					return Double.NaN;
				}

				// If we're after the decimal, reduce the exponent.
				if (hasDot) exponent--;
			} else {
				// Non dot-or digit. Assume this is an exponent separator (Pp), and we'll deal with this later.
				break;
			}
		}

		// If we've parsed no numbers, bail. "0x." isn't a valid number for instance!
		if (sigDigits + nonSigDigits == 0) return Double.NaN;

		exponent *= 4; // Compute the "actual" exponent, not just the number of hex digits after the dot.

		// Parse the exponent part.
		if (index < end) {
			var expSeparator = bytes[index];
			if (expSeparator != 'P' && expSeparator != 'p') return Double.NaN;

			index++;

			int givenExponent = 0;
			boolean expNegative = false;

			// Parse the exponent sign.
			if (index < end) {
				switch (bytes[index]) {
					case '+' -> index++;
					case '-' -> {
						index++;
						expNegative = true;
					}
				}
			}

			if (index >= end) return Double.NaN; // Exponent is empty!

			// Parse the actual exponent.
			for (; index < end; index++) {
				var expChar = bytes[index];
				if (!CharProperties.isDigit(expChar)) return Double.NaN;
				givenExponent = givenExponent * 10 + (expChar - '0');
			}

			if (expNegative) givenExponent = -givenExponent;
			exponent += givenExponent;
		}

		if (isNeg) result = -result;
		return Math.scalb(result, exponent);
	}
}
