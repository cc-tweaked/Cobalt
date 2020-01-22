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
import sun.misc.FormattedFloatingDecimal;

public class FormatDesc {
	private boolean leftAdjust;
	private boolean zeroPad;
	private boolean explicitPlus;
	private boolean space;
	private boolean alternateForm;
	private static final int MAX_FLAGS = 5;

	private int width;
	int precision;

	final int conversion;
	final int length;

	private static boolean useOracleFormatting = true;

	FormatDesc(LuaString strfrmt, final int start) throws LuaError {
		int p = start, n = strfrmt.length();
		int c = 0;

		boolean moreFlags = true;
		while (moreFlags) {
			switch (c = ((p < n) ? strfrmt.luaByte(p++) : 0)) {
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
			c = ((p < n) ? strfrmt.luaByte(p++) : 0);
			if (Character.isDigit((char) c)) {
				width = width * 10 + (c - '0');
				c = ((p < n) ? strfrmt.luaByte(p++) : 0);
			}
		}

		precision = -1;
		if (c == '.') {
			c = ((p < n) ? strfrmt.luaByte(p++) : 0);
			if (Character.isDigit((char) c)) {
				precision = c - '0';
				c = ((p < n) ? strfrmt.luaByte(p++) : 0);
				if (Character.isDigit((char) c)) {
					precision = precision * 10 + (c - '0');
					c = ((p < n) ? strfrmt.luaByte(p++) : 0);
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
		int effectiveWidth = width;
		if (number < 0 || explicitPlus || space) effectiveWidth--;

		if (Double.isNaN(number)) {
			if (!leftAdjust) pad(buf, ' ', effectiveWidth - 3);
			appendSign(buf, number);
			buf.append(Character.isUpperCase(conversion) ? "NAN" : "nan");
			if (leftAdjust) pad(buf, ' ', effectiveWidth - 3);
		} else if (Double.isInfinite(number)) {
			if (!leftAdjust) pad(buf, ' ', effectiveWidth - 3);
			appendSign(buf, number);
			buf.append(Character.isUpperCase(conversion) ? "INF" : "inf");
			if (leftAdjust) pad(buf, ' ', effectiveWidth - 3);
		} else {
			if (useOracleFormatting) {
				try {
					formatWithOracle(buf, number, effectiveWidth);
					return;
				} catch (LinkageError ignored) {
					useOracleFormatting = false;
				}
			}

			StringBuilder format = new StringBuilder("%");
			if (alternateForm) format.append('#');
			if (explicitPlus) format.append('+');
			if (space) format.append(' ');
			if (width >= 0) {
				if (leftAdjust) format.append('-');
				if (zeroPad) format.append('0');
				format.append(width);
			}
			format.append('.').append(precision >= 0 ? precision : 6);
			format.append((char) conversion);

			buf.append(String.format(format.toString(), number));
		}
	}

	/**
	 * Java's handling of format strings isn't entirely correct, so we attempt to roll our own.
	 *
	 * It's a little ugly, and depends on Oracle internals, so we have a fallback should the propritary APIs not be
	 * available.
	 *
	 * @param buf            The buffer to write to
	 * @param number         The number to write
	 * @param effectiveWidth The width remaining after emitting the sign
	 */
	private void formatWithOracle(Buffer buf, double number, int effectiveWidth) {
		char[] mantissa, exp;
		int expRounded;
		StringBuilder mantissaBuilder;

		int precision = this.precision;
		if (this.precision == -1) precision = 6;

		if (conversion == 'g' || conversion == 'G') {
			if (precision == 0) precision = 1;

			if (number == 0) {
				mantissa = new char[]{'0'};
				exp = null;
				expRounded = 0;
			} else {
				FormattedFloatingDecimal fd = FormattedFloatingDecimal.valueOf(Math.abs(number), precision, FormattedFloatingDecimal.Form.GENERAL);
				mantissa = fd.getMantissa();
				exp = fd.getExponent();
				expRounded = fd.getExponentRounded();
			}

			mantissaBuilder = new StringBuilder(mantissa.length);
			mantissaBuilder.append(mantissa);
			stripZeros(mantissaBuilder);
			if (alternateForm) {
				precision -= exp != null ? 1 : expRounded + 1;
				addZeros(mantissaBuilder, precision);
			}
		} else if (conversion == 'e' || conversion == 'E') {
			FormattedFloatingDecimal fd = FormattedFloatingDecimal.valueOf(Math.abs(number), precision, FormattedFloatingDecimal.Form.SCIENTIFIC);
			mantissa = fd.getMantissa();
			exp = number == 0 ? new char[]{'+', '0', '0'} : fd.getExponent();

			mantissaBuilder = new StringBuilder(mantissa.length);
			mantissaBuilder.append(mantissa);
			addZeros(mantissaBuilder, precision);
		} else if (conversion == 'f') {
			FormattedFloatingDecimal fd = FormattedFloatingDecimal.valueOf(Math.abs(number), precision, FormattedFloatingDecimal.Form.DECIMAL_FLOAT);
			mantissa = fd.getMantissa();
			exp = null;

			mantissaBuilder = new StringBuilder(mantissa.length);
			mantissaBuilder.append(mantissa);
			addZeros(mantissaBuilder, precision);
		} else {
			throw new IllegalStateException("Unknown converter " + conversion);
		}

		// Calculate the effective width
		effectiveWidth -= mantissaBuilder.length();
		if (exp != null) effectiveWidth -= 1 + exp.length;
		if (alternateForm && precision == 0) effectiveWidth--;

		// Spaces must occur before the sign but 0s afterwards
		if (!zeroPad && !leftAdjust) pad(buf, ' ', effectiveWidth);
		appendSign(buf, number);
		if (zeroPad && !leftAdjust) pad(buf, '0', effectiveWidth);

		// Append required parts of the mantissa.
		buf.append(mantissaBuilder.toString());

		// If the precision is zero and the '#' flag is set, add the requested decimal point.
		if (alternateForm && precision == 0) buf.append('.');

		if (exp != null) {
			buf.append(conversion <= 'Z' ? 'E' : 'e');
			buf.append(exp);
		}

		if (leftAdjust) pad(buf, ' ', effectiveWidth);
	}

	public void format(Buffer buf, LuaString s) {
		int nullindex = s.indexOf((byte) '\0', 0);
		if (nullindex != -1) {
			s = s.substring(0, nullindex);
		}
		if (precision >= 0 && s.length() > precision) {
			s = s.substring(0, precision);
		}

		int minwidth = s.length();
		int nspaces = width > minwidth ? width - minwidth : 0;
		if (!leftAdjust) pad(buf, ' ', nspaces);

		buf.append(s);

		if (leftAdjust) pad(buf, ' ', nspaces);
	}

	private void appendSign(Buffer buf, double number) {
		if (number < 0) {
			buf.append('-');
		} else if (explicitPlus) {
			buf.append('+');
		} else if (space) {
			buf.append(' ');
		}
	}

	private static void pad(Buffer buf, char c, int n) {
		byte b = (byte) c;
		while (n-- > 0) buf.append(b);
	}

	private static void addZeros(StringBuilder v, int prec) {
		// Look for the dot.  If we don't find one, the we'll need to add
		// it before we add the zeros.
		int i;
		for (i = 0; i < v.length(); i++) {
			if (v.charAt(i) == '.') break;
		}
		boolean needDot = i == v.length();

		// Determine existing precision.
		int outPrec = v.length() - i - (needDot ? 0 : 1);
		assert (outPrec <= prec);
		if (outPrec == prec) return;

		// Add dot if previously determined to be necessary.
		if (needDot) v.append('.');

		// Add zeros.
		for (int j = 0; j < prec - outPrec; j++) v.append('0');
	}

	private static void stripZeros(StringBuilder v) {
		// If we don't end with a zero then skip.
		if (v.charAt(v.length() - 1) != '0') return;

		// Ensure we've a dot
		if (v.lastIndexOf(".") == -1) return;

		int length = v.length();
		while (v.charAt(length - 1) == '0' || v.charAt(length - 1) == '.') length--;
		v.setLength(length);
	}
}
