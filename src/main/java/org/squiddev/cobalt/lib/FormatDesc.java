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
package org.squiddev.cobalt.lib;

import cc.tweaked.cobalt.internal.doubles.DoubleToStringConverter;
import cc.tweaked.cobalt.internal.string.CharProperties;
import org.squiddev.cobalt.Buffer;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaString;

public class FormatDesc {
	private static final DoubleToStringConverter.Symbols LOWER_SYMBOLS =
		new DoubleToStringConverter.Symbols("inf", "nan", 'e', 'x', 'p', 'a');
	private static final DoubleToStringConverter.Symbols UPPER_SYMBOLS =
		new DoubleToStringConverter.Symbols("INF", "NAN", 'E', 'X', 'P', 'A');

	static final DoubleToStringConverter.FormatOptions DEFAULT_LOWER_OPTIONS = new DoubleToStringConverter.FormatOptions(
		LOWER_SYMBOLS, false, false, false, -1, false, false
	);

	private static final int MAX_FLAGS = 5;
	static final int LEFT_ADJUST = 1 << 0;
	static final int EXPLICIT_PLUS = 1 << 1;
	static final int SPACE = 1 << 2;
	static final int ALTERNATE_FORM = 1 << 3;
	static final int ZERO_PAD = 1 << 4;
	static final int PRECISION = 1 << 5;

	private final int flags;
	private final int width;
	private final int precision;

	final int conversion;

	final LuaString format;
	final int start;
	final int length;

	FormatDesc(LuaString format, final int start) throws LuaError {
		this.format = format;
		this.start = start;

		int p = start, n = format.length();
		int c = 0;

		int flags = 0;
		boolean moreFlags = true;
		while (moreFlags) {
			switch (c = p < n ? format.charAt(p++) : 0) {
				case '-' -> flags |= LEFT_ADJUST;
				case '+' -> flags |= EXPLICIT_PLUS;
				case ' ' -> flags |= SPACE;
				case '#' -> flags |= ALTERNATE_FORM;
				case '0' -> flags |= ZERO_PAD;
				default -> moreFlags = false;
			}
		}

		if (p - start - 1 > MAX_FLAGS) {
			throw new LuaError("invalid format (repeated flags)");
		}

		int width = -1;
		if (CharProperties.isDigit(c)) {
			width = c - '0';
			c = p < n ? format.charAt(p++) : 0;
			if (CharProperties.isDigit(c)) {
				width = width * 10 + c - '0';
				c = p < n ? format.charAt(p++) : 0;
			}
		}
		this.width = width;

		int precision = -1;
		if (c == '.') {
			c = p < n ? format.charAt(p++) : 0;
			if (CharProperties.isDigit(c)) {
				precision = c - '0';
				c = p < n ? format.charAt(p++) : 0;
				if (CharProperties.isDigit(c)) {
					precision = precision * 10 + c - '0';
					c = p < n ? format.charAt(p++) : 0;
				}
			} else {
				precision = 0;
			}
			flags |= PRECISION;
		}
		this.precision = precision;
		this.flags = flags;

		if (CharProperties.isDigit(c)) throw new LuaError("invalid format (width or precision too long)");

		conversion = c;
		length = p - start;
	}

	private boolean leftAdjust() {
		return (flags & LEFT_ADJUST) != 0;
	}

	private boolean alternateForm() {
		return (flags & ALTERNATE_FORM) != 0;
	}

	private boolean zeroPad() {
		// Only set '0' if '0' is present and '-' is not.
		return (flags & (ZERO_PAD | LEFT_ADJUST)) == ZERO_PAD;
	}

	private boolean explicitPlus() {
		// Only set ' ' if ' ' is present and '+' is not.
		return (flags & EXPLICIT_PLUS) != 0;
	}

	private boolean space() {
		// Only set ' ' if ' ' is present and '+' is not.
		return (flags & (SPACE | EXPLICIT_PLUS)) == SPACE;
	}

	public static FormatDesc ofUnsafe(String format) {
		try {
			return new FormatDesc(LuaString.valueOf(format), 0);
		} catch (LuaError e) {
			throw new IllegalStateException(e);
		}
	}

	void checkFlags(int flags) throws LuaError {
		if ((this.flags & ~flags) == 0) return;

		var buffer = new Buffer();
		buffer.append("invalid conversion specification: '%");
		buffer.append(format, start, length);
		buffer.append("'");
		throw new LuaError(buffer.toLuaString());
	}

	void format(Buffer buf, byte c) {
		int nSpaces = width > 1 ? width - 1 : 0;
		if (!leftAdjust()) pad(buf, ' ', nSpaces);
		buf.append(c);
		if (leftAdjust()) pad(buf, ' ', nSpaces);
	}

	public void format(Buffer buf, long number) {
		String digits;
		boolean hasSign = false;

		switch (conversion) {
			case 'x' -> digits = Long.toHexString(number);
			case 'X' -> digits = Long.toHexString(number).toUpperCase();
			case 'o' -> digits = Long.toOctalString(number);
			case 'u' -> digits = Long.toUnsignedString(number);
			default -> {
				digits = Long.toString(number);
				hasSign = true;
			}
		}

		if (number == 0) {
			// "%.0d" and "%.0o" will be "".
			// "%#.0d" will be "", but "%#.0o" will be "0".
			if (precision == 0 && (conversion != 'o' || !alternateForm())) digits = "";
		}

		int minWidth = digits.length();
		int nDigits = minWidth;
		int nZeros;

		if (hasSign) {
			if (number < 0) {
				nDigits--;
				digits = digits.substring(1);
			} else if (explicitPlus() || space()) {
				minWidth++;
			}
		}

		String prefix = "";
		if (number != 0 && alternateForm()) {
			// If we're not 0 and we've some alternative form, then prefix with that.
			// Note that octal's "0" counts as a digit but hex's "0x" does not.
			switch (conversion) {
				case 'x' -> prefix = "0x";
				case 'X' -> prefix = "0X";
				case 'o' -> {
					prefix = "0";
					nDigits++;
				}
			}
			minWidth += prefix.length();
		}


		if (precision > nDigits) {
			nZeros = precision - nDigits;
		} else if (precision == -1 && zeroPad() && width > minWidth) {
			nZeros = width - minWidth;
		} else {
			nZeros = 0;
		}

		minWidth += nZeros;
		int nSpaces = width > minWidth ? width - minWidth : 0;

		if (!leftAdjust()) pad(buf, ' ', nSpaces);

		if (hasSign) {
			if (number < 0) {
				buf.append('-');
			} else if (explicitPlus()) {
				buf.append('+');
			} else if (space()) {
				buf.append(' ');
			}
		}

		buf.append(prefix);
		if (nZeros > 0) pad(buf, '0', nZeros);
		buf.append(digits);

		if (leftAdjust()) pad(buf, ' ', nSpaces);
	}

	public void format(Buffer buf, double number) {
		int prec = precision;
		switch (conversion) {
			case 'g', 'G' -> {
				int computedPrecision = switch (prec) {
					case -1 -> 6;
					case 0 -> 1;
					default -> prec;
				};
				DoubleToStringConverter.toPrecision(number, computedPrecision, doubleOpts(conversion == 'G'), buf);
			}
			case 'e', 'E' ->
				DoubleToStringConverter.toExponential(number, prec == -1 ? 6 : prec, doubleOpts(conversion == 'E'), buf);
			case 'a', 'A' -> DoubleToStringConverter.toHex(number, prec, doubleOpts(conversion == 'A'), buf);
			case 'f' -> DoubleToStringConverter.toFixed(number, prec == -1 ? 6 : prec, doubleOpts(false), buf);
		}
	}

	void format(Buffer buf, LuaString s) {
		if (precision == -1 && s.length() >= 100) {
			buf.append(s);
			return;
		}

		int nullIndex = s.indexOf((byte) '\0');
		if (nullIndex != -1) {
			s = s.substringOfEnd(0, nullIndex);
		}
		if (precision >= 0 && s.length() > precision) {
			s = s.substringOfEnd(0, precision);
		}

		int minWidth = s.length();
		int nSpaces = width > minWidth ? width - minWidth : 0;
		if (!leftAdjust()) pad(buf, ' ', nSpaces);

		buf.append(s);

		if (leftAdjust()) pad(buf, ' ', nSpaces);
	}

	private static void pad(Buffer buf, char c, int n) {
		byte b = (byte) c;
		while (n-- > 0) buf.append(b);
	}

	private DoubleToStringConverter.FormatOptions doubleOpts(boolean caps) {
		return new DoubleToStringConverter.FormatOptions(
			caps ? UPPER_SYMBOLS : LOWER_SYMBOLS,
			explicitPlus(), space(), alternateForm(), width, zeroPad(), leftAdjust()
		);
	}
}
