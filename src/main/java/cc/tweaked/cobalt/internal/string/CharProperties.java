package cc.tweaked.cobalt.internal.string;

/**
 * Various properties on characters, as used by the lexer, and other parsers within the codebase.
 */
public final class CharProperties {
	private CharProperties() {
	}

	public static boolean isAlphaNum(int c) {
		return c >= '0' && c <= '9'
			|| c >= 'a' && c <= 'z'
			|| c >= 'A' && c <= 'Z'
			|| c == '_';
	}

	public static boolean isAlpha(int c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
	}

	public static boolean isDigit(int c) {
		return c >= '0' && c <= '9';
	}

	public static boolean isSpace(int c) {
		return c <= ' ';
	}

	public static boolean isHex(int c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
	}

	public static int hexValue(int c) {
		// Terrible bit twiddling right here:
		// 'A'..'F' corresponds to 0x41..0x46, and 'a'..'f' to 0x61..0x66. So bitwise and with 0xf
		// gives us the last digit, +9 to map from 1..6 to 10..15.
		return c <= '9' ? c - '0' : (c & 0xf) + 9;
	}
}
