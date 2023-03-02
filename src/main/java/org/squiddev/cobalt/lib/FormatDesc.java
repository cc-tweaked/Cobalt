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

import org.squiddev.cobalt.Buffer;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.lib.doubles.DoubleToStringConverter;

public class FormatDesc {
	private boolean leftAdjust;
	private boolean zeroPad;
	private boolean explicitPlus;
	private boolean space;
	private boolean alternateForm;
	private static final int MAX_FLAGS = 5;

	private static final DoubleToStringConverter.Symbols LOWER_SYMBOLS =
		new DoubleToStringConverter.Symbols("inf", "nan", 'e');
	private static final DoubleToStringConverter.Symbols UPPER_SYMBOLS =
		new DoubleToStringConverter.Symbols("INF", "NAN", 'E');
	private static final DoubleToStringConverter DOUBLE_CONVERTER = new DoubleToStringConverter(
		DoubleToStringConverter.Flags.UNIQUE_ZERO |
			DoubleToStringConverter.Flags.NO_TRAILING_ZERO |
			DoubleToStringConverter.Flags.EMIT_POSITIVE_EXPONENT_SIGN,
		new DoubleToStringConverter.PrecisionPolicy(4, 0),
		2
	);

	private int width;
	int precision;

	final int conversion;
	final int length;

	FormatDesc(LuaString strfrmt, final int start) throws LuaError {
		int p = start, n = strfrmt.length();
		int c = 0;

		boolean moreFlags = true;
		while (moreFlags) {
			switch (c = ((p < n) ? strfrmt.charAt(p++) : 0)) {
				case '-':
					leftAdjust = true;
					break;
				case '+':
					explicitPlus = true;
					break;
				case ' ':
					space = true;
					break;
				case '#':
					alternateForm = true;
					break;
				case '0':
					zeroPad = true;
					break;
				default:
					moreFlags = false;
					break;
			}
		}

		if (p - start - 1 > MAX_FLAGS) {
			throw new LuaError("invalid format (repeated flags)");
		}

		width = -1;
		if (Character.isDigit((char) c)) {
			width = c - '0';
			c = ((p < n) ? strfrmt.charAt(p++) : 0);
			if (Character.isDigit((char) c)) {
				width = width * 10 + (c - '0');
				c = ((p < n) ? strfrmt.charAt(p++) : 0);
			}
		}

		precision = -1;
		if (c == '.') {
			c = ((p < n) ? strfrmt.charAt(p++) : 0);
			if (Character.isDigit((char) c)) {
				precision = c - '0';
				c = ((p < n) ? strfrmt.charAt(p++) : 0);
				if (Character.isDigit((char) c)) {
					precision = precision * 10 + (c - '0');
					c = ((p < n) ? strfrmt.charAt(p++) : 0);
				}
			} else {
				precision = 0;
			}
		}

		if (Character.isDigit((char) c)) {
			throw new LuaError("invalid format (width or precision too long)");
		}

		zeroPad &= !leftAdjust; // '-' overrides '0'
		space &= !explicitPlus;
		conversion = c;
		length = p - start;
	}

	public static FormatDesc ofUnsafe(String format) {
		try {
			return new FormatDesc(LuaString.valueOf(format), 0);
		} catch (LuaError e) {
			throw new IllegalStateException(e);
		}
	}

	public void format(Buffer buf, byte c) {
		buf.append(c);
	}

	public void format(Buffer buf, long number) {
		String digits;
		boolean hasSign = false;

		switch (conversion) {
			case 'x':
				digits = Long.toHexString(number);
				break;
			case 'X':
				digits = Long.toHexString(number).toUpperCase();
				break;
			case 'o':
				digits = Long.toOctalString(number);
				break;
			case 'u': {
				// In order to remain safe with Java 8 we inline Long.toUnsignedString
				if (number >= 0) {
					digits = Long.toString(number);
				} else {
					long quot = (number >>> 1) / 5;
					long rem = number - quot * 10;
					digits = Long.toString(quot) + rem;
				}
				break;
			}
			default:
				digits = Long.toString(number);
				hasSign = true;
				break;
		}

		if (number == 0) {
			// "%.0d" and "%.0o" will be "".
			// "%#.0d" will be "", but "%#.0o" will be "0".
			if (precision == 0 && (conversion != 'o' || !alternateForm)) digits = "";
		}

		int minWidth = digits.length();
		int nDigits = minWidth;
		int nZeros;

		if (hasSign) {
			if (number < 0) {
				nDigits--;
				digits = digits.substring(1);
			} else if (explicitPlus || space) {
				minWidth++;
			}
		}

		String prefix = "";
		if (number != 0 && alternateForm) {
			// If we're not 0 and we've some alternative form, then prefix with that.
			// Note that octal's "0" counts as a digit but hex's "0x" does not.
			switch (conversion) {
				case 'x':
					prefix = "0x";
					break;
				case 'X':
					prefix = "0X";
					break;
				case 'o':
					prefix = "0";
					nDigits++;
					break;
			}
			minWidth += prefix.length();
		}


		if (precision > nDigits) {
			nZeros = precision - nDigits;
		} else if (precision == -1 && zeroPad && width > minWidth) {
			nZeros = width - minWidth;
		} else {
			nZeros = 0;
		}

		minWidth += nZeros;
		int nSpaces = width > minWidth ? width - minWidth : 0;

		if (!leftAdjust) pad(buf, ' ', nSpaces);

		if (hasSign) {
			if (number < 0) {
				buf.append('-');
			} else if (explicitPlus) {
				buf.append('+');
			} else if (space) {
				buf.append(' ');
			}
		}

		buf.append(prefix);
		if (nZeros > 0) pad(buf, '0', nZeros);
		buf.append(digits);

		if (leftAdjust) pad(buf, ' ', nSpaces);
	}

	public void format(Buffer buf, double number) {
		int prec = this.precision;
		if (prec == -1) prec = 6;

		if (conversion == 'g' || conversion == 'G') {
			if (prec == 0) prec = 1;
			DOUBLE_CONVERTER.toPrecision(
				number, prec,
				doubleOpts(conversion == 'G'),
				buf
			);
		} else if (conversion == 'e' || conversion == 'E') {
			DOUBLE_CONVERTER.toExponential(
				number, prec,
				doubleOpts(conversion == 'E'),
				buf
			);
		} else if (conversion == 'f') {
			DOUBLE_CONVERTER.toFixed(
				number, prec,
				doubleOpts(false),
				buf
			);
		}
	}

	public void format(Buffer buf, LuaString s) {
		int nullindex = s.indexOf((byte) '\0', 0);
		if (nullindex != -1) {
			s = s.substringOfEnd(0, nullindex);
		}
		if (precision >= 0 && s.length() > precision) {
			s = s.substringOfEnd(0, precision);
		}

		int minwidth = s.length();
		int nspaces = width > minwidth ? width - minwidth : 0;
		if (!leftAdjust) pad(buf, ' ', nspaces);

		buf.append(s);

		if (leftAdjust) pad(buf, ' ', nspaces);
	}

	private static void pad(Buffer buf, char c, int n) {
		byte b = (byte) c;
		while (n-- > 0) buf.append(b);
	}

	private DoubleToStringConverter.FormatOptions doubleOpts(boolean caps) {
		return new DoubleToStringConverter.FormatOptions(
			caps ? UPPER_SYMBOLS : LOWER_SYMBOLS,
			explicitPlus,
			space,
			alternateForm,
			width,
			zeroPad,
			leftAdjust
		);
	}
}
