/*
 * Copyright 2010 the V8 project authors. All rights reserved.
 * Copyright 2021 sir-maniac. All Rights reserved.
 *
 * Ported to Java by sir-maniac
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Google Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.squiddev.cobalt.lib.doubles;

import org.checkerframework.checker.signedness.qual.Unsigned;

import static java.util.Objects.requireNonNull;
import static org.squiddev.cobalt.lib.doubles.Assert.DOUBLE_CONVERSION_ASSERT;

public class DoubleToStringConverter {
	/**
	 *  When calling toFixed with a double > 10^MAX_FIXED_DIGITS_BEFORE_POINT
	 *  or a requested_digits parameter > MAX_FIXED_DIGITS_AFTER_POINT then the
	 *  function returns false.
	 */
	public static final int MAX_FIXED_DIGITS_BEFORE_POINT = 60;
	private static final double FIRST_NON_FIXED = 1e60; // exponent must be the same as MAX_FIXED_DIGITS_BEFORE_POINT
	public static final int MAX_FIXED_DIGITS_AFTER_POINT = 100;



	/**
	 *  When calling toExponential with a requested_digits
	 *  parameter > MAX_EXPONENTIAL_DIGITS then the function returns false.
	 */
	public static final int MAX_EXPONENTIAL_DIGITS = 120;

	/**
	 *  When calling toPrecision with a requested_digits
	 *  parameter < MIN_PRECISION_DIGITS or requested_digits > MAX_PRECISION_DIGITS
	 *  then the function returns false.
	 */
	public static final int MIN_PRECISION_DIGITS = 1;
	/**
	 *  When calling toPrecision with a requested_digits
	 *  parameter < MIN_PRECISION_DIGITS or requested_digits > MAX_PRECISION_DIGITS
	 *  then the function returns false.
	 */
	public static final int MAX_PRECISION_DIGITS = 120;

	/**
	 * 	 The maximal number of digits that are needed to emit a double in base 10.
	 * 	 A higher precision can be achieved by using more digits, but the shortest
	 * 	 accurate representation of any double will never use more digits than
	 * 	 BASE_10_MAXIMAL_LENGTH.
	 * 	 Note that doubleToAscii null-terminates its input. So the given buffer
	 * 	 should be at least BASE_10_MAXIMAL_LENGTH + 1 characters long.
	 */
	public static final int BASE_10_MAXIMAL_LENGTH = 17;

	/**
	 *  The maximal number of digits that are needed to emit a single in base 10.
	 *  A higher precision can be achieved by using more digits, but the shortest
	 *  accurate representation of any single will never use more digits than
	 *  BASE_10_MAXIMAL_LENGTH_SINGLE.
	 */
	public static final int BASE_10_MAXIMAL_LENGTH_SINGLE = 9;

	/**
	 *  The length of the longest string that 'ToShortest' can produce when the
	 *  converter is instantiated with EcmaScript defaults (see
	 *  'ecmaScriptConverter')
	 *  This amount of characters is needed for negative values that hit the
	 *  'decimal_in_shortest_low' limit. For example: "-0.0000033333333333333333"
	 */
	public static final int MAX_CHARS_ECMA_SCRIPT_SHORTEST = 25;

	public static final int EXPONENTIAL_REP_CAPACITY = MAX_EXPONENTIAL_DIGITS + 2;
	public static final int FIXED_REP_CAPACITY = MAX_FIXED_DIGITS_BEFORE_POINT + MAX_FIXED_DIGITS_AFTER_POINT + 1;
	public static final int PRECISION_REP_CAPACITY = MAX_PRECISION_DIGITS + 1;
	public static final int SHORTEST_REP_CAPACITY = BASE_10_MAXIMAL_LENGTH + 1;

	public static final int MAX_EXPONENT_LENGTH = 5;

	@SuppressWarnings("ImplicitNumericConversion")
	private static final int ASCII_ZERO = '0';

	public static class Flags {
		/**
		 * No special flags (0)
		 */
		public static final int NO_FLAGS = 0;
		/**
		 * When the number is converted into exponent
		 *  form, emits a '+' for positive exponents. Example: <code>1.2e+2</code>
		 */
		public static final int EMIT_POSITIVE_EXPONENT_SIGN = 1;
		/**
		 * When the input number is an integer and is
		 *  converted into decimal format then a trailing decimal point is appended.
		 *  <p/>
		 *  Example: <code>2345.0</code> is converted to <code>"2345.".</code>
		 */
		public static final int EMIT_TRAILING_DECIMAL_POINT = 2;
		/**
		 * In addition to a trailing decimal point emits a trailing '0'-character.
		 *   This flag requires the <code>EMIT_TRAILING_DECIMAL_POINT</code> flag.
		 *  <p/>
		 *  Example: <code>2345.0</code> is converted to <code>"2345.0".</code>
		 */
		public static final int EMIT_TRAILING_ZERO_AFTER_POINT = 4;
		/**
		 * <code>"-0.0"</code> is converted to <code>"0.0"</code>.
		 */
		public static final int UNIQUE_ZERO = 8;
		/**
		 * Trailing zeros are removed from the fractional portion
		 *  of the result in precision mode. Matches C++ <code>printf</code>'s %g.
		 *  <p/>
		 *  When EMIT_TRAILING_ZERO_AFTER_POINT is also given, one trailing zero is
		 *  preserved.
		 */
		public static final int NO_TRAILING_ZERO = 16;
	}


	private final int flags;
	private final Symbols symbols;
	private final ShortestPolicy shortestPolicy;
	private final PrecisionPolicy precisionPolicy;
	private final int minExponentWidth;

	/**
	 * Construct a <code>DoubleToStringConvertor</code>.
	 * <p/>
	 * Flags should be a bit-or combination of the possible Flags members.
	 *   <ul>
	 *     <li><code>NO_FLAGS</code>: no special flags</li>
	 *     <li><code>EMIT_POSITIVE_EXPONENT_SIGN</code>: when the number is converted into exponent
	 * 	       form, emits a '+' for positive exponents. Example: 1.2e+2</li>
	 * 	   <li><code>EMIT_TRAILING_DECIMAL_POINT</code>: when the input number is an integer and is
	 * 	       converted into decimal format then a trailing decimal point is appended.
	 * 	       Example: 2345.0 is converted to "2345.".</li>
	 * 	   <li><code>EMIT_TRAILING_ZERO_AFTER_POINT</code>: in addition to a trailing decimal point
	 * 	       emits a trailing '0'-character. This flag requires the
	 * 	            EMIT_TRAILING_DECIMAL_POINT flag.
	 * 	       Example: 2345.0 is converted to "2345.0".</li>
	 * 	   <li><code>UNIQUE_ZERO</code>: "-0.0" is converted to "0.0".</li>
	 * 	   <li><code>NO_TRAILING_ZERO</code>: Trailing zeros are removed from the fractional portion
	 * 	       of the result in precision mode. Matches C++ <code>printf</code>'s %g.
	 * 	       When EMIT_TRAILING_ZERO_AFTER_POINT is also given, one trailing zero is
	 * 	       preserved.</li>
	 *   </ul>
	 *  <p/>
	 *
	 * @param flags the bit-or combination of {@link Flags}
	 * @param symbols the symbols output for special values, and the exponent character,
	 *                see {@link Symbols#Symbols(String, String, int)}
	 * @param shortestPolicy the parameters that configures when {@link #toShortest} output
	 *                       switches to an exponential representation, see
	 *                       {@link ShortestPolicy#ShortestPolicy(int, int)}
	 * @param precisionPolicy the parameters that configure when {@link #toPrecision}
	 *                        output switches to exponential representation, see
	 *                        {@link PrecisionPolicy#PrecisionPolicy(int, int)}
     *
	 * @see Symbols#Symbols(String, String, int)
	 * @see ShortestPolicy#ShortestPolicy(int, int)
	 * @see PrecisionPolicy#PrecisionPolicy(int, int)
	 */
	public DoubleToStringConverter(int flags,
								   Symbols symbols,
								   ShortestPolicy shortestPolicy,
								   PrecisionPolicy precisionPolicy) {
		this(flags,
			 symbols,
			 shortestPolicy,
			 precisionPolicy,
			 0);
	}

	/**
	 * Construct a <code>DoubleToStringConvertor</code>.
	 * <p/>
	 * Flags should be a bit-or combination of the possible Flags-enum.
	 *   <ul>
	 *     <li><code>NO_FLAGS</code>: no special flags</li>
	 *     <li><code>EMIT_POSITIVE_EXPONENT_SIGN</code>: when the number is converted into exponent
	 * 	       form, emits a '+' for positive exponents. Example: 1.2e+2</li>
	 * 	   <li><code>EMIT_TRAILING_DECIMAL_POINT</code>: when the input number is an integer and is
	 * 	       converted into decimal format then a trailing decimal point is appended.
	 * 	       Example: 2345.0 is converted to "2345.".</li>
	 * 	   <li><code>EMIT_TRAILING_ZERO_AFTER_POINT</code>: in addition to a trailing decimal point
	 * 	       emits a trailing '0'-character. This flag requires the
	 * 	            EMIT_TRAILING_DECIMAL_POINT flag.
	 * 	       Example: 2345.0 is converted to "2345.0".</li>
	 * 	   <li><code>UNIQUE_ZERO</code>: "-0.0" is converted to "0.0".</li>
	 * 	   <li><code>NO_TRAILING_ZERO</code>: Trailing zeros are removed from the fractional portion
	 * 	       of the result in precision mode. Matches C++ <code>printf</code>'s %g.
	 * 	       When EMIT_TRAILING_ZERO_AFTER_POINT is also given, one trailing zero is
	 * 	       preserved.</li>
	 *   </ul>
	 *  <p/>
	 *  The <code>minExponentWidth</code> is used for exponential representations.
	 *  The converter adds leading '0's to the exponent until the exponent
	 *    is at least minExponentWidth digits long.
	 *  <p/>
	 *  The <code>minExponentWidth</code> is clamped to 5.
	 *  As such, the exponent may never have more than 5 digits in total.<br/>
	 *
	 * @param flags the bit-or combination of {@link Flags}
	 * @param symbols the symbols output for special values, and the exponent character,
	 *                see {@link Symbols#Symbols(String, String, int)}
     * @param shortestPolicy the parameters that configures when {@link #toShortest} output
	 *                       switches to an exponential representation, see
	 *                       {@link ShortestPolicy#ShortestPolicy(int, int)}
	 * @param precisionPolicy the parameters that configure when {@link #toPrecision}
	 *                        output switches to exponential representation, see
	 *                        {@link PrecisionPolicy#PrecisionPolicy(int, int)}
	 * @param minExponentWidth The converter adds leading '0's to the exponent until the exponent
	 *                         is at least <code>minExponentWidth</code> digits long, clamped to 5
	 *
	 * @see Symbols#Symbols(String, String, int)
	 * @see ShortestPolicy#ShortestPolicy(int, int)
	 * @see PrecisionPolicy#PrecisionPolicy(int, int)
	 */
	public DoubleToStringConverter(int flags,
								   Symbols symbols,
								   ShortestPolicy shortestPolicy,
								   PrecisionPolicy precisionPolicy,
								   int minExponentWidth) {
        this.flags = flags;
        this.symbols = requireNonNull( symbols );
        this.shortestPolicy = requireNonNull( shortestPolicy );
        this.precisionPolicy = requireNonNull( precisionPolicy );
		this.minExponentWidth = minExponentWidth;
		// When 'trailing zero after the point' is set, then 'trailing point'
		// must be set too.
		DOUBLE_CONVERSION_ASSERT(((flags & Flags.EMIT_TRAILING_DECIMAL_POINT) != 0) ||
				!((flags & Flags.EMIT_TRAILING_ZERO_AFTER_POINT) != 0));
	}

	/**
	 *  Returns a converter following the EcmaScript specification.
	 *  <p/>
	 *  <b>Flags:</b> UNIQUE_ZERO and EMIT_POSITIVE_EXPONENT_SIGN.<br/>
	 *  <b>Special values:</b> "Infinity" and "NaN".
	 *  Lower case 'e' for exponential values.<br/>
	 *  <b>ShortestPolicy.decimalLow</b>: -6<br/>
	 *  <b>ShortestPolicy.decimalHigh</b>: 21<br/>
	 *  <b>PrecisionPolicy.maxLeadingZeros</b>: 6<br/>
	 *  <b>PrecisionPolicy.maxTrailingZeroes</b>: 0<br/>
	 */
	@SuppressWarnings("ImplicitNumericConversion")
	public static DoubleToStringConverter ecmaScriptConverter() {
		int flags = Flags.UNIQUE_ZERO | Flags.EMIT_POSITIVE_EXPONENT_SIGN;
		return new DoubleToStringConverter(flags,
										   new Symbols("Infinity", "NaN", 'e'),
										   new ShortestPolicy(-6, 21),
										   new PrecisionPolicy(6, 0));
	}


	/**
	 *  Computes the shortest string of digits that correctly represent the input
	 *  number. Depending on decimal_in_shortest_low and ShortestPolicy.decimalHigh
	 *  (see constructor) it then either returns a decimal representation, or an
	 *  exponential representation.
	 *  <p/>
	 *  Example with ShortestPolicy.decimalLow = -6,
	 * 			  ShortestPolicy.decimalHigh = 21,
	 * 			  <code>EMIT_POSITIVE_EXPONENT_SIGN</code> activated, and
	 * 			  <code>EMIT_TRAILING_DECIMAL_POINT</code> deactivated:<br/>
	 * <p/>
	 * <code>
	 *    toShortest(0.000001)  -> "0.000001"<br/>
	 *    toShortest(0.0000001) -> "1e-7"<br/>
	 *    toShortest(111111111111111111111.0)  -> "111111111111111110000"<br/>
	 *    toShortest(100000000000000000000.0)  -> "100000000000000000000"<br/>
	 *    toShortest(1111111111111111111111.0) -> "1.1111111111111111e+21"<br/>
	 *  </code>
	 *  <p/>
	 *  <b>Note:</b> the conversion may round the output if the returned string
	 *  is accurate enough to uniquely identify the input-number.
	 *  For example the most precise representation of the double 9e59 equals
	 *  "899999999999999918767229449717619953810131273674690656206848", but
	 *  the converter will return the shorter (but still correct) "9e59".
	 *  <p/>
	 *  The length of the longest result is the maximum of the length of the
	 *  following string representations (each with possible examples):
	 *  <ul>
	 *    <li>NaN and negative infinity: "NaN", "-Infinity", "-inf".</li>
	 *    <li>-10^(ShortestPolicy.decimalHigh - 1):
	 *        "-100000000000000000000", "-1000000000000000.0"</li>
	 *    <li>the longest string in range [0; -10^ShortestPolicy.decimalLow]. Generally,
	 *        this string is 3 + BASE_10_MAXIMAL_LENGTH - ShortestPolicy.decimalLow.
	 *        (sign, '0', decimal point, padding zeroes for ShortestPolicy.decimalLow,
	 *         and the significant digits).
	 *        "-0.0000033333333333333333", "-0.0012345678901234567"</li>
	 *    <li>the longest exponential representation. (A negative number with
	 *        BASE_10_MAXIMAL_LENGTH significant digits).
	 *        "-1.7976931348623157e+308", "-1.7976931348623157E308"</li>
	 *  </ul>
	 */
	public void toShortest(double value, Appendable resultBuilder) {
		toShortestIeeeNumber(value, resultBuilder, DtoaMode.SHORTEST);
	}

	/**
	 * Same as toShortest, but for single-precision floats.
	 *
	 * @see #toShortest
	 * */
	public void toShortestSingle(float value, Appendable resultBuilder) {
		//noinspection ImplicitNumericConversion
		toShortestIeeeNumber(value, resultBuilder, DtoaMode.SHORTEST_SINGLE);
	}

	/**
	 * 	 If the value is a special value (NaN or Infinity) constructs the
	 * 	 corresponding string using the configured infinity/nan-symbol.
	 * 	 If either of them is NULL or the value is not special then the
	 * 	 function returns false.
	 */
	private void handleSpecialValues(double value, Appendable resultBuilder) {
		Ieee.Double doubleInspect = new Ieee.Double(value);
		if (doubleInspect.isInfinite()) {
			if (value < 0.0) {
				resultBuilder.append('-');
			}
			resultBuilder.append(symbols.getInfinitySymbol());
		}
		if (doubleInspect.isNan()) {
			resultBuilder.append(symbols.getNanSymbol());
		}
	}

	/**
	 * 	 Constructs an exponential representation (i.e. 1.234e56).
	 * 	 The given exponent assumes a decimal point after the first decimal digit.
	 */
	private void createExponentialRepresentation(final DecimalRepBuf decimalDigits,
												 int length,
												 int exponent,
												 Appendable resultBuilder) {
		if (decimalDigits.length() == 0) throw new IllegalArgumentException("decimalDigits is empty");
		if (length > decimalDigits.length()) throw new IllegalArgumentException("length larger then decimalDigits");
		resultBuilder.append(decimalDigits.charAt(0));
		if (length != 1) {
			resultBuilder.append('.');
			resultBuilder.append(decimalDigits.getBuffer(), 1, length-1);
		}
		resultBuilder.append((char)symbols.getExponentCharacter());
		if (exponent < 0) {
			resultBuilder.append('-');
			exponent = -exponent;
		} else {
			if ((flags & Flags.EMIT_POSITIVE_EXPONENT_SIGN) != 0) {
				resultBuilder.append('+');
			}
		}
		DOUBLE_CONVERSION_ASSERT((double)exponent < 1e4);
		// Changing this constant requires updating the comment of DoubleToStringConverter constructor
		char[] buffer = new char[MAX_EXPONENT_LENGTH];
		int firstCharPos = MAX_EXPONENT_LENGTH;
		if (exponent == 0) {
			buffer[--firstCharPos] = '0';
		} else {
			while (exponent > 0) {
				buffer[--firstCharPos] = (char) (ASCII_ZERO + (exponent % 10));
				exponent /= 10;
			}
		}
		// Add prefix '0' to make exponent width >= min(min_exponent_with_, MAX_EXPONENT_LENGTH)
		// For example: convert 1e+9 -> 1e+09, if min_exponent_with_ is set to 2
		while(MAX_EXPONENT_LENGTH - firstCharPos < Math.min(minExponentWidth, MAX_EXPONENT_LENGTH)) {
			buffer[--firstCharPos] = '0';
		}
		resultBuilder.append(buffer, firstCharPos, MAX_EXPONENT_LENGTH - firstCharPos);
	}

	/** Creates a decimal representation (i.e 1234.5678). */
	private void createDecimalRepresentation(DecimalRepBuf decimalDigits,
											 int digitsAfterPoint,
											 Appendable resultBuilder) {
		int decimalPoint = decimalDigits.getPointPosition();
		int length = decimalDigits.length();
		// Create a representation that is padded with zeros if needed.
		if (decimalPoint <= 0) {
			// "0.00000decimal_rep" or "0.000decimal_rep00".
			resultBuilder.append('0');
			if (digitsAfterPoint > 0) {
				resultBuilder.append('.');

				addZeros(resultBuilder, -decimalPoint);
				DOUBLE_CONVERSION_ASSERT(length <= digitsAfterPoint - (-decimalPoint));
				resultBuilder.append(decimalDigits.getBuffer(), 0, decimalDigits.length());
				int remainingDigits = digitsAfterPoint - (-decimalPoint) - length;
				addZeros(resultBuilder, remainingDigits);
			}
		} else if (decimalPoint >= length) {
			// "decimal_rep0000.00000" or "decimalRep.0000".
			resultBuilder.append(decimalDigits.getBuffer(), 0, decimalDigits.length());
			addZeros(resultBuilder, decimalPoint - length);
			if (digitsAfterPoint > 0) {
				resultBuilder.append('.');
				addZeros(resultBuilder, digitsAfterPoint);
			}
		} else {
			// "decima.l_rep000".
			DOUBLE_CONVERSION_ASSERT(digitsAfterPoint > 0);
			resultBuilder.append(decimalDigits.getBuffer(), 0, decimalPoint);
			resultBuilder.append('.');
			DOUBLE_CONVERSION_ASSERT(length - decimalPoint <= digitsAfterPoint);
			resultBuilder.append(decimalDigits.getBuffer(), decimalPoint, length - decimalPoint);
			int remainingDigits = digitsAfterPoint - (length - decimalPoint);
			addZeros(resultBuilder, remainingDigits);
		}
		if (digitsAfterPoint == 0) {
			if ((flags & Flags.EMIT_TRAILING_DECIMAL_POINT) != 0) {
				resultBuilder.append('.');
			}
			if ((flags & Flags.EMIT_TRAILING_ZERO_AFTER_POINT) != 0) {
				resultBuilder.append('0');
			}
		}
	}

	/** Implementation for toShortest and toShortestSingle. */
	private void toShortestIeeeNumber(double value,
										 Appendable resultBuilder,
										 DtoaMode mode) {
		DOUBLE_CONVERSION_ASSERT(mode == DtoaMode.SHORTEST || mode == DtoaMode.SHORTEST_SINGLE);
		if (new Ieee.Double(value).isSpecial()) {
			handleSpecialValues(value, resultBuilder);
			return;
		}

		DecimalRepBuf decimalRep = new DecimalRepBuf(SHORTEST_REP_CAPACITY);

		doubleToAscii(value, mode, 0, decimalRep);

		boolean unique_zero = (flags & Flags.UNIQUE_ZERO) != 0;
		if (decimalRep.getSign() && (value != 0.0 || !unique_zero)) {
			resultBuilder.append('-');
		}

		int length = decimalRep.length();
		int decimalPoint = decimalRep.getPointPosition();
		int exponent = decimalPoint - 1;
		if ((shortestPolicy.getDecimalLow() <= exponent) &&
				(exponent < shortestPolicy.getDecimalHigh())) {
			createDecimalRepresentation(decimalRep,
					Math.max(0, length - decimalPoint),
					resultBuilder);
		} else {
			createExponentialRepresentation(decimalRep, decimalRep.length(), exponent,
					resultBuilder);
		}
	}


	/**
	 *  Computes a decimal representation with a fixed number of digits after the
	 *  decimal point. The last emitted digit is rounded.
	 *  <p/>
	 *  <b>Examples:</b><br/>
	 *  <code>
	 *    toFixed(3.12, 1) -> "3.1"<br/>
	 *    toFixed(3.1415, 3) -> "3.142"<br/>
	 *    toFixed(1234.56789, 4) -> "1234.5679"<br/>
	 *    toFixed(1.23, 5) -> "1.23000"<br/>
	 *    toFixed(0.1, 4) -> "0.1000"<br/>
	 *    toFixed(1e30, 2) -> "1000000000000000019884624838656.00"<br/>
	 *    toFixed(0.1, 30) -> "0.100000000000000005551115123126"<br/>
	 *    toFixed(0.1, 17) -> "0.10000000000000001"<br/>
	 *  </code>
	 *  <p/>
	 *  If <code>requestedDigits</code> equals 0, then the tail of the result depends on
	 *  the <code>EMIT_TRAILING_DECIMAL_POINT</code> and <code>EMIT_TRAILING_ZERO_AFTER_POINT</code>.
	 *  <p/>
	 *  Examples, for requestedDigits == 0,
	 *    let <code>EMIT_TRAILING_DECIMAL_POINT</code> and <code>EMIT_TRAILING_ZERO_AFTER_POINT</code> be<br/>
	 *  <table>
	 *    <tr><td>false and false: then</td> <td> 123.45 -> 123   </td></tr>
	 *    <tr><td/>                          <td> 0.678 -> 1      </td></tr>
	 *    <tr><td>true and false: then</td>  <td> 123.45 -> 123.  </td></tr>
	 *    <tr><td/>                          <td> 0.678 -> 1.     </td></tr>
	 *    <tr><td>true and true: then</td>   <td> 123.45 -> 123.0 </td></tr>
	 *    <tr><td/>                          <td> 0.678 -> 1.0    </td></tr>
	 *  </table>
	 *  <p/>
	 *
	 * @param requestedDigits the number of digits to the right of the decimal point, the last emitted digit is rounded
	 *
	 * @throws IllegalArgumentException if <code>requestedDigits > MAX_FIXED_DIGITS_BEFORE_POINT</code> or
	 *                                  if <code>value > 10^MAX_FIXED_DIGITS_BEFORE_POINT</code>
	 *                                  <p/>
	 *                                  These two conditions imply that the result for non-special values
	 *                                  never contains more than<br/>
	 *                                  <code>1 + MAX_FIXED_DIGITS_BEFORE_POINT + 1 +
	 *                                        MAX_FIXED_DIGITS_AFTER_POINT</code><br/>
	 *                                  characters (one additional character for the sign, and one for the decimal point).
	 *
	 */
	public void toFixed(double value,
					int requestedDigits,
					Appendable resultBuilder) {
		// DOUBLE_CONVERSION_ASSERT(MAX_FIXED_DIGITS_BEFORE_POINT == 60);

		if (new Ieee.Double(value).isSpecial()) {
			handleSpecialValues(value, resultBuilder);
			return;
		}

		if (requestedDigits > MAX_FIXED_DIGITS_AFTER_POINT) {
			throw new IllegalArgumentException("requestedDigits too large. max: " + MAX_FIXED_DIGITS_BEFORE_POINT +
					"(MAX_FIXED_DIGITS_BEFORE_POINT) got: " + requestedDigits);
		}
		if (value >= FIRST_NON_FIXED || value <= -FIRST_NON_FIXED) {
			throw new IllegalArgumentException("value greater than 10^"+MAX_FIXED_DIGITS_BEFORE_POINT +
													   "(MAX_FIXED_DIGITS_BEFORE_POINT)");
		}

		// Find a sufficiently precise decimal representation of n.
		DecimalRepBuf decimalRep = new DecimalRepBuf(FIXED_REP_CAPACITY);
		doubleToAscii(value, DtoaMode.FIXED, requestedDigits, decimalRep);

		boolean uniqueZero = ((flags & Flags.UNIQUE_ZERO) != 0);
		if (decimalRep.getSign() && (value != 0.0 || !uniqueZero)) {
			resultBuilder.append('-');
		}

		createDecimalRepresentation(decimalRep, requestedDigits, resultBuilder);
	}

	/**
	 *  Computes a representation in exponential format with <code>requestedDigits</code>
	 *  after the decimal point. The last emitted digit is rounded.
	 *  <p/>
	 *  If <code>requestedDigits</code> equals -1, then the shortest exponential representation
	 *  is computed.
	 *  <p/>
	 *  Examples with <b>EMIT_POSITIVE_EXPONENT_SIGN</b> deactivated, and
	 *    exponent_character set to <code>'e'</code>.
	 *  <p/>
	 *  <code>
	 *   toExponential(3.12, 1) -> "3.1e0"<br/>
	 *   toExponential(5.0, 3) -> "5.000e0"<br/>
	 *   toExponential(0.001, 2) -> "1.00e-3"<br/>
	 *   toExponential(3.1415, -1) -> "3.1415e0"<br/>
	 *   toExponential(3.1415, 4) -> "3.1415e0"<br/>
	 *   toExponential(3.1415, 3) -> "3.142e0"<br/>
	 *   toExponential(123456789000000, 3) -> "1.235e14"<br/>
	 *   toExponential(1000000000000000019884624838656.0, -1) -> "1e30"<br/>
	 *   toExponential(1000000000000000019884624838656.0, 32) ->
	 *        "1.00000000000000001988462483865600e30"<br/>
	 *   toExponential(1234, 0) -> "1e3"<br/>
	 *  </code>
	 *
	 * @param requestedDigits number of digits after the decimal point(last digit rounded), or
	 *                        <code>-1</code> for the shortest representation
	 *
	 * @throws IllegalArgumentException if <code>requestedDigits > MAX_EXPONENTIAL_DIGITS</code>
	 */
	public void toExponential(double value,
						  int requestedDigits,
						  Appendable resultBuilder) {
		if (new Ieee.Double(value).isSpecial()) {
			handleSpecialValues(value, resultBuilder);
			return;
		}

		if (requestedDigits > MAX_EXPONENTIAL_DIGITS) {
			throw new IllegalArgumentException(
					String.format("requestedDigits must be less than %d. got: %d",
								  MAX_EXPONENTIAL_DIGITS, requestedDigits));
		}

		// DOUBLE_CONVERSION_ASSERT(EXPONENTIAL_REP_CAPACITY > BASE_10_MAXIMAL_LENGTH);
		DecimalRepBuf decimalRep = new DecimalRepBuf(EXPONENTIAL_REP_CAPACITY);

		if (requestedDigits == -1) {
			doubleToAscii(value, DtoaMode.SHORTEST, 0, decimalRep);
		} else {
			doubleToAscii(value, DtoaMode.PRECISION, requestedDigits + 1,
					decimalRep);
			DOUBLE_CONVERSION_ASSERT(decimalRep.length() <= requestedDigits + 1);

			decimalRep.zeroExtend(requestedDigits);
		}

		boolean uniqueZero = ((flags & Flags.UNIQUE_ZERO) != 0);
		if (decimalRep.getSign() && (value != 0.0 || !uniqueZero)) {
			resultBuilder.append('-');
		}

		int exponent = decimalRep.getPointPosition() - 1;
		createExponentialRepresentation(decimalRep,
				decimalRep.length(),
				exponent,
				resultBuilder);
	}

	/**
	 *  Computes 'precision' leading digits of the given 'value' and returns them
	 *  either in exponential or decimal format, depending on
	 *  PrecisionPolicy.max{Leading|Trailing}Zeros (given to the
	 *  constructor).
	 *  <p/>
	 *  The last computed digit is rounded.
	 *  </p>
	 *  Example with PrecisionPolicy.maxLeadingZeros = 6.
	 *  <p/>
	 *  <code>
	 *    toPrecision(0.0000012345, 2) -> "0.0000012"<br/>
	 *    toPrecision(0.00000012345, 2) -> "1.2e-7"<br/>
	 *  </code>
	 *  <p/>
	 *  Similarily the converter may add up to
	 *  PrecisionPolicy.maxTrailingZeros in precision mode to avoid
	 *  returning an exponential representation. A zero added by the
	 *  <b>EMIT_TRAILING_ZERO_AFTER_POINT</b> flag is counted for this limit.
	 *  <p/>
	 *  Examples for PrecisionPolicy.maxTrailingZeros = 1:
	 *  </p>
	 *  <code>
	 *    toPrecision(230.0, 2) -> "230"<br/>
	 *    toPrecision(230.0, 2) -> "230."  with EMIT_TRAILING_DECIMAL_POINT.<br/>
	 *    toPrecision(230.0, 2) -> "2.3e2" with EMIT_TRAILING_ZERO_AFTER_POINT.<br/>
	 *  </code>
	 *  <p/>
	 *  Examples for PrecisionPolicy.maxTrailingZeros = 3, and no
	 * 	EMIT_TRAILING_ZERO_AFTER_POINT:
	 * 	</p>
	 * 	<code>
	 *    toPrecision(123450.0, 6) -> "123450"<br/>
	 *    toPrecision(123450.0, 5) -> "123450"<br/>
	 *    toPrecision(123450.0, 4) -> "123500"<br/>
	 *    toPrecision(123450.0, 3) -> "123000"<br/>
	 *    toPrecision(123450.0, 2) -> "1.2e5"<br/>
	 *  </code>
	 *  <p/>
	 * @throws IllegalArgumentException when <code>precision < MIN_PRECISION_DIGITS</code> or
	 *                                  <code>precision > MAX_PRECISION_DIGITS</code>
	 */
	public void toPrecision(double value,
						int precision,
						Appendable resultBuilder) {
		if (new Ieee.Double(value).isSpecial()) {
			handleSpecialValues(value, resultBuilder);
			return;
		}

		if (precision < MIN_PRECISION_DIGITS || precision > MAX_PRECISION_DIGITS) {
			throw new IllegalArgumentException(String.format(
					"argument precision must be in range (%d,%d)", MIN_PRECISION_DIGITS, MAX_PRECISION_DIGITS ));
		}

		// Find a sufficiently precise decimal representation of n.
		// Add one for the terminating null character.
		DecimalRepBuf decimalRep = new DecimalRepBuf(PRECISION_REP_CAPACITY);
		doubleToAscii(value, DtoaMode.PRECISION, precision, decimalRep);
		DOUBLE_CONVERSION_ASSERT(decimalRep.length() <= precision);

		boolean uniqueZero = ((flags & Flags.UNIQUE_ZERO) != 0);
		if (decimalRep.getSign() && (value != 0.0 || !uniqueZero)) {
			resultBuilder.append('-');
		}

		// The exponent if we print the number as x.xxeyyy. That is with the
		// decimal point after the first digit.
		int decimalPoint = decimalRep.getPointPosition();
		int exponent = decimalPoint - 1;

		int extraZero = ((flags & Flags.EMIT_TRAILING_ZERO_AFTER_POINT) != 0) ? 1 : 0;
		boolean asExponential =
				(-decimalPoint + 1 > precisionPolicy.getMaxLeadingZeroes()) ||
						(decimalPoint - precision + extraZero >
								precisionPolicy.getMaxTrailingZeroes());
		if ((flags & Flags.NO_TRAILING_ZERO) != 0) {
			// Truncate trailing zeros that occur after the decimal point (if exponential,
			// that is everything after the first digit).
			decimalRep.truncateZeros(asExponential);
			// Clamp precision to avoid the code below re-adding the zeros.
			precision = Math.min(precision, decimalRep.length());
		}
		if (asExponential) {
			// Fill buffer to contain 'precision' digits.
			// Usually the buffer is already at the correct length, but 'doubleToAscii'
			// is allowed to return less characters.
			decimalRep.zeroExtend(precision);

			createExponentialRepresentation(decimalRep,
					precision,
					exponent,
					resultBuilder);
		} else {
			createDecimalRepresentation(decimalRep,
					Math.max(0, precision - decimalRep.getPointPosition()),
					resultBuilder);
		}
	}

	public enum DtoaMode {
		// Produce the shortest correct representation.
		// For example the output of 0.299999999999999988897 is (the less accurate
		// but correct) 0.3.
		SHORTEST,
		// Same as SHORTEST, but for single-precision floats.
		SHORTEST_SINGLE,
		// Produce a fixed number of digits after the decimal point.
		// For instance fixed(0.1, 4) becomes 0.1000
		// If the input number is big, the output will be big.
		FIXED,
		// Fixed number of digits (independent of the decimal point).
		PRECISION
	}

	private static BigNumDtoa.BignumDtoaMode dtoaToBignumDtoaMode(
			DoubleToStringConverter.DtoaMode dtoaMode) {
		switch (dtoaMode) {
			case SHORTEST:  return BigNumDtoa.BignumDtoaMode.SHORTEST;
			case SHORTEST_SINGLE:
				return BigNumDtoa.BignumDtoaMode.SHORTEST_SINGLE;
			case FIXED:     return BigNumDtoa.BignumDtoaMode.FIXED;
			case PRECISION: return BigNumDtoa.BignumDtoaMode.PRECISION;
			default:
				throw new IllegalStateException("Unreachable");
		}
	}

	/**
	 * Converts the given double <code>v</code> to digit characters. <code>v</code> must
	 * not be <code>NaN</code>, <code>+Infinity</code>, or <code>-Infinity</code>. In
	 * SHORTEST_SINGLE-mode this restriction also applies to <code>v</code> after it has
	 * been casted to a single-precision float. That is, in this mode <code>(float)v</code> must
	 * not be <code>NaN</code>, <code>+Infinity</code> or <code>-Infinity</code>.
	 *   <p/>
	 * The result should be interpreted as <code>buffer * 10^(outPoint-outLength)</code>.
	 *   <p/>
	 * The digits are written to the  <code>buffer</code> in the platform's charset, which is
	 * often UTF-8 (with ASCII-range digits) but may be another charset, such
	 * as EBCDIC.
	 *   <p/>
	 * The output depends on the given mode:<br/>
	 * <ul>
	 *  <li>
	 *    {@link DtoaMode#SHORTEST SHORTEST}: produce the least amount of digits for which the internal
	 *    identity requirement is still satisfied. If the digits are printed
	 *    (together with the correct exponent) then reading this number will give
	 *    'v' again. The  <code>buffer</code> will choose the representation that is closest to
	 *    'v'. If there are two at the same distance, than the one farther away
	 *    from 0 is chosen (halfway cases - ending with 5 - are rounded up).
	 *    In this mode the 'requestedDigits' parameter is ignored.
	 *    <p/>
	 *  </li>
	 *  <li>
	 *      {@link DtoaMode#SHORTEST_SINGLE SHORTEST_SINGLE}: same as <code>SHORTEST</code> but with single-precision.
	 *    <p/>
	 *  </li>
	 *  <li>
	 *     {@link DtoaMode#FIXED FIXED}: produces digits necessary to print a given number with
	 *    'requestedDigits' digits after the decimal outPoint. The produced digits
	 *    might be too short in which case the caller has to fill the remainder
	 *    with '0's.
	 *    <p/>
	 *    <b>Example:</b> toFixed(0.001, 5) is allowed to return  <code>buffer="1", outPoint=-2</code>.
	 *    <p/>
	 *    Halfway cases are rounded towards +/-Infinity (away from 0). The call
	 *    toFixed(0.15, 2) thus returns  buffer="2", outPoint=0.
	 *    <p/>
	 *    The returned buffer may contain digits that would be truncated from the
	 *    shortest representation of the input.
	 *    <p/>
	 *  </li>
	 *  <li>
	 *    {@link DtoaMode#PRECISION PRECISION}: produces 'requestedDigits' where the first digit is not '0'.
	 *    Even though the outLength of produced digits usually equals
	 *    'requestedDigits', the function is allowed to return fewer digits, in
	 *    which case the caller has to fill the missing digits with '0's.
	 *    <p/>
	 *    Halfway cases are again rounded away from 0.
	 *  </li>
	 * </ul>
	 * <p/>
	 * <code>doubleToAscii</code> expects the given buffer to be big enough to hold all
	 *   digits. In SHORTEST-mode it expects a buffer of at least BASE_10_MAXIMAL_LENGTH. In
	 *   all other modes the requestedDigits parameter and the padding-zeroes limit the size of the
	 *   output. Don't forget the decimal point and the exponent character when
	 *   computing the maximal output size.
	 *
	 * @param v the value to be converted to digits
	 * <p/>
	 * @param mode the {@link DtoaMode DtoaMode} used for the conversion
	 * <p/>
	 * @param requestedDigits for <b>FIXED</b> the number of digits after teh decimal point,
	 *                        for <b>PRECISION</b> the number of digits where the first digit is not '0',
	 *                        for <b>SHORTEST</b> and <b>SHORTEST_SINGLE</b> this value is ignored
	 * <p/>
	 * @param buffer the {@link DecimalRepBuf} initialized with enough space for the conversion(explained above). On
	 *               successful completion this buffer contains the digits,
	 *               the {@link DecimalRepBuf#getPointPosition() pointPosition},
	 *               and the {@link DecimalRepBuf#getSign() sign} of the number.
	 */
	public static void doubleToAscii(double v,
										DtoaMode mode,
										int requestedDigits,
										DecimalRepBuf buffer) {
		DOUBLE_CONVERSION_ASSERT(!new Ieee.Double(v).isSpecial());
		DOUBLE_CONVERSION_ASSERT(mode == DtoaMode.SHORTEST || mode == DtoaMode.SHORTEST_SINGLE || requestedDigits >= 0);

		// begin with an empty buffer
		buffer.reset();

		if (new Ieee.Double(v).sign() < 0) {
			buffer.setSign(true);
			v = -v;
		} else {
			buffer.setSign(false);
		}

		if (mode == DtoaMode.PRECISION && requestedDigits == 0) {
			return;
		}

		if (v == 0.0) {
			buffer.append(0);
			buffer.setPointPosition(1);
			return;
		}

		boolean fastWorked;
		switch (mode) {
			case SHORTEST:
				fastWorked = FastDtoa.fastDtoa(v, FastDtoa.FastDtoaMode.SHORTEST, 0, buffer);
				break;
			case SHORTEST_SINGLE:
				fastWorked = FastDtoa.fastDtoa(v, FastDtoa.FastDtoaMode.SHORTEST_SINGLE, 0, buffer);
				break;
			case FIXED:
				fastWorked = FixedDtoa.fastFixedDtoa(v, requestedDigits, buffer);
				break;
			case PRECISION:
				fastWorked = FastDtoa.fastDtoa(v, FastDtoa.FastDtoaMode.PRECISION, requestedDigits, buffer);
				break;
			default:
				throw new IllegalStateException("Unreachable");
		}
		if (fastWorked) return;

		buffer.reset();
		// If the fast dtoa didn't succeed use the slower bignum version.
		BigNumDtoa.BignumDtoaMode dtoaMode = dtoaToBignumDtoaMode(mode);
		BigNumDtoa.bignumDtoa(v, dtoaMode, requestedDigits, buffer);
	}

	/**
	 * 	Add character padding to the builder. If count is non-positive,
	 * 	nothing is added to the builder.
	 */
	private static void addZeros(Appendable sb, int count) {
		for (int i=count; i > 0; i--) {
			sb.append('0');
		}
	}

	/**
	 * Parameter object for the symbols used during conversion.
	 *
	 * @see #Symbols(String, String, int)
	 */
	public static class Symbols {
		private final String infinitySymbol;
		private final String nanSymbol;
		private final @Unsigned int exponentCharacter;

		/**
		 * Construct a symbols parameter object
		 * <p/>
		 * <code>infinitySymbol</code> and <code>nanSymbol</code> provide the string representation for these
		 *   special values. If the string is NULL and the special value is encountered
		 *   then the conversion functions return false.
		 * <p/>
		 *  The <code>exponentCharacter</code> is used in exponential representations. It is
		 *     usually 'e' or 'E'.<br/>
		 *
		 * @param infinitySymbol string representation of 'infinity' special value
		 * @param nanSymbol string representation of 'NaN' special value
		 * @param exponentCharacter used in exponential representations, it is usually 'e' or 'E'
		 */
		public Symbols(String infinitySymbol, String nanSymbol, @Unsigned int exponentCharacter) {
			this.infinitySymbol = requireNonNull( infinitySymbol );
			this.nanSymbol = requireNonNull( nanSymbol );
			this.exponentCharacter = exponentCharacter;
		}

		public String getInfinitySymbol() {
			return infinitySymbol;
		}

		public String getNanSymbol() {
			return nanSymbol;
		}

		public @Unsigned int getExponentCharacter() {
			return exponentCharacter;
		}
	}

	/**
	 * Parameter object configuring usage of {@link DoubleToStringConverter#toShortest}
	 *
	 * @see #ShortestPolicy(int, int)
	 */
	public static class ShortestPolicy {
		private final int decimalLow;
		private final int decimalHigh;

		/**
		 * When converting to the shortest representation the converter will
		 * 	 represent input numbers in decimal format if they are in the interval
		 * 	 <p/>
		 * 	 <code>[10^decimalLow; 10^decimalHigh[</code><br/>
		 * 	 (lower boundary included, greater boundary excluded).
		 * 	 <p/>
		 * 	 Example: with decimalLow = -6 and
		 * 	 		   decimalHigh = 21:<br/>
		 *  <code>
		 * 	   toShortest(0.000001)  -> "0.000001"<br/>
		 * 	   toShortest(0.0000001) -> "1e-7"<br/>
		 * 	   toShortest(111111111111111111111.0)  -> "111111111111111110000"<br/>
		 * 	   toShortest(100000000000000000000.0)  -> "100000000000000000000"<br/>
		 * 	   toShortest(1111111111111111111111.0) -> "1.1111111111111111e+21"<br/>
		 * 	</code>
		 *
		 * @param decimalLow lower boundary to represent number in decimal format, inclusive
		 * @param decimalHigh greater boundary to represent number in decimal format, exclusive
		 */
		public ShortestPolicy(int decimalLow, int decimalHigh) {
			this.decimalLow = decimalLow;
			this.decimalHigh = decimalHigh;
		}

		public int getDecimalLow() {
			return decimalLow;
		}

		public int getDecimalHigh() {
			return decimalHigh;
		}
	}

	/**
	 * Parameter object configuring usage of {@link DoubleToStringConverter#toPrecision}
	 *
	 * @see #PrecisionPolicy(int, int)
	 */
	public static class PrecisionPolicy {
		private final int maxLeadingZeroes;
		private final int maxTrailingZeroes;

		/**
		 *  When converting to precision mode the converter may add
		 *  max_leading_padding_zeroes before returning the number in exponential
		 *  format.
		 *  <p/>
		 *  Example with maxLeadingZeroes = 6.<br/>
		 *  <code>
		 *    toPrecision(0.0000012345, 2) -> "0.0000012"<br/>
		 *    toPrecision(0.00000012345, 2) -> "1.2e-7"<br/>
		 *  </code>
		 *  <p/>
		 *  Similarily the converter may add up to
		 *  maxTrailingZeroes in precision mode to avoid
		 *  returning an exponential representation. A zero added by the
		 *  <code>EMIT_TRAILING_ZERO_AFTER_POINT</code> flag is counted for this limit.
		 *  <p/>
		 *  Examples for maxTrailingZeroes = 1:<br/>
		 *  <code>
		 *    toPrecision(230.0, 2) -> "230"<br/>
		 *    toPrecision(230.0, 2) -> "230."  with EMIT_TRAILING_DECIMAL_POINT.<br/>
		 *    toPrecision(230.0, 2) -> "2.3e2" with EMIT_TRAILING_ZERO_AFTER_POINT.<br/>
	     * </code>
		 * @param maxLeadingZeroes Maximum allowed leading zeros before switching to exponential representation
		 * @param maxTrailingZeroes Maximum allowed trailing zeros before switching to exponential representation
		 */
		public PrecisionPolicy(int maxLeadingZeroes, int maxTrailingZeroes) {
			this.maxLeadingZeroes = maxLeadingZeroes;
			this.maxTrailingZeroes = maxTrailingZeroes;
		}

		public int getMaxLeadingZeroes() {
			return maxLeadingZeroes;
		}

		public int getMaxTrailingZeroes() {
			return maxTrailingZeroes;
		}
	}
}
