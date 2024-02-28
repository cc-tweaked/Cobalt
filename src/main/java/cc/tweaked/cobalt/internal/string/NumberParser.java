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

	public static double parse(byte[] bytes, int offset, int length, int base) {
		int index = offset, end = offset + length;
		while (index < end && StringLib.isWhitespace(bytes[index])) index++;
		while (index < end && StringLib.isWhitespace(bytes[end - 1])) end--;

		boolean isNeg = false;
		if (index < end) {
			switch (bytes[index]) {
				case '+' -> index++;
				case '-' -> {
					index++;
					isNeg = true;
				}
			}
		}

		if (index >= end) return Double.NaN;

		if ((base == 10 || base == 16) && (bytes[index] == '0' && index + 1 < end && (bytes[index + 1] == 'x' || bytes[index + 1] == 'X'))) {
			base = 16;
			index += 2;

			if (index >= end) return Double.NaN;
		}

		double value = scanLong(base, bytes, index, end);
		if (Double.isNaN(value)) {
			value = switch (base) {
				case 10 -> scanDouble(bytes, index, end);
				case 16 -> scanHexDouble(bytes, index, end);
				default -> Double.NaN;
			};
		}

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

	private static double scanHexDouble(byte[] bytes, int index, int end) {
		long result = 0; // The mantissa
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

		return Math.scalb((double) result, exponent);
	}
}
