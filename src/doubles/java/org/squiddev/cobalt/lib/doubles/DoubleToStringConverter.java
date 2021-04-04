/*
 * Copyright 2010 the V8 project authors. All rights reserved.
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

import static org.squiddev.cobalt.lib.doubles.Assert.DOUBLE_CONVERSION_ASSERT;
import static org.squiddev.cobalt.lib.doubles.FixedDtoa.fastFixedDtoa;

public class DoubleToStringConverter {
	/**
	 *  When calling toFixed with a double > 10^kMaxFixedDigitsBeforePoint
	 *  or a requested_digits parameter > kMaxFixedDigitsAfterPoint then the
	 *  function returns false.
	 */
	public static final int MAX_FIXED_DIGITS_BEFORE_POINT = 60;
	public static final int MAX_FIXED_DIGITS_AFTER_POINT = 100;

	/**
	 *  When calling toExponential with a requested_digits
	 *  parameter > kMaxExponentialDigits then the function returns false.
	 */
	public static final int MAX_EXPONENTIAL_DIGITS = 120;

	/**
	 *  When calling toPrecision with a requested_digits
	 *  parameter < kMinPrecisionDigits or requested_digits > kMaxPrecisionDigits
	 *  then the function returns false.
	 */
	public static final int MIN_PRECISION_DIGITS = 1;
	/**
	 *  When calling toPrecision with a requested_digits
	 *  parameter < kMinPrecisionDigits or requested_digits > kMaxPrecisionDigits
	 *  then the function returns false.
	 */
	public static final int MAX_PRECISION_DIGITS = 120;

	/**
	 * 	 The maximal number of digits that are needed to emit a double in base 10.
	 * 	 A higher precision can be achieved by using more digits, but the shortest
	 * 	 accurate representation of any double will never use more digits than
	 * 	 kBase10MaximalLength.
	 * 	 Note that doubleToAscii null-terminates its input. So the given buffer
	 * 	 should be at least kBase10MaximalLength + 1 characters long.
	 */
	public static final int BASE_10_MAXIMAL_LENGTH = 17;

	/**
	 *  The maximal number of digits that are needed to emit a single in base 10.
	 *  A higher precision can be achieved by using more digits, but the shortest
	 *  accurate representation of any single will never use more digits than
	 *  kBase10MaximalLengthSingle.
	 */
	public static final int BASE_10_MAXIMAL_LENGTH_SINGLE = 9;

	/**
	 *  The length of the longest string that 'ToShortest' can produce when the
	 *  converter is instantiated with EcmaScript defaults (see
	 *  'ecmaScriptConverter')
	 *  This value does not include the trailing '\0' character.
	 *  This amount of characters is needed for negative values that hit the
	 *  'decimal_in_shortest_low' limit. For example: "-0.0000033333333333333333"
	 */
	public static final int MAX_CHARS_ECMA_SCRIPT_SHORTEST = 25;
	@SuppressWarnings("ImplicitNumericConversion")
	private static final int ASCII_ZERO = '0';

	public static class Flags {
		public static final int NO_FLAGS = 0;
		public static final int EMIT_POSITIVE_EXPONENT_SIGN = 1;
		public static final int EMIT_TRAILING_DECIMAL_POINT = 2;
		public static final int EMIT_TRAILING_ZERO_AFTER_POINT = 4;
		public static final int UNIQUE_ZERO = 8;
		public static final int NO_TRAILING_ZERO = 16;
	}


	private final int flags;
	private final String infinitySymbol;
	private final String nanSymbol;
	private final int exponentCharacter;
	private final int decimalInShortestLow;
	private final int decimalInShortestHigh;
	private final int maxLeadingPaddingZeroesInPrecisionMode;
	private final int maxTrailingPaddingZeroesInPrecisionMode;
	private final int minExponentWidth;

	public DoubleToStringConverter(int flags,
								   String infinitySymbol,
								   String nanSymbol,
								   int exponentCharacter,
								   int decimalInShortestLow,
								   int decimalInShortestHigh,
								   int maxLeadingPaddingZeroesInPrecisionMode,
								   int maxTrailingPaddingZeroesInPrecisionMode) {
		this(flags,
			infinitySymbol,
			nanSymbol,
			exponentCharacter,
			decimalInShortestLow,
			decimalInShortestHigh,
			maxLeadingPaddingZeroesInPrecisionMode,
			maxTrailingPaddingZeroesInPrecisionMode, 0);
	}

	/**
	 *  Flags should be a bit-or combination of the possible Flags-enum.
	 *   - NO_FLAGS: no special flags.
	 *   - EMIT_POSITIVE_EXPONENT_SIGN: when the number is converted into exponent
	 * 	form, emits a '+' for positive exponents. Example: 1.2e+2.
	 *   - EMIT_TRAILING_DECIMAL_POINT: when the input number is an integer and is
	 * 	converted into decimal format then a trailing decimal point is appended.
	 * 	Example: 2345.0 is converted to "2345.".
	 *   - EMIT_TRAILING_ZERO_AFTER_POINT: in addition to a trailing decimal point
	 * 	emits a trailing '0'-character. This flag requires the
	 * 	EMIT_TRAILING_DECIMAL_POINT flag.
	 * 	Example: 2345.0 is converted to "2345.0".
	 *   - UNIQUE_ZERO: "-0.0" is converted to "0.0".
	 *   - NO_TRAILING_ZERO: Trailing zeros are removed from the fractional portion
	 * 	of the result in precision mode. Matches printf's %g.
	 * 	When EMIT_TRAILING_ZERO_AFTER_POINT is also given, one trailing zero is
	 * 	preserved.
	 *
	 *  Infinity symbol and nanSymbol provide the string representation for these
	 *  special values. If the string is NULL and the special value is encountered
	 *  then the conversion functions return false.
	 *
	 *  The exponentCharacter is used in exponential representations. It is
	 *  usually 'e' or 'E'.
	 *
	 *  When converting to the shortest representation the converter will
	 *  represent input numbers in decimal format if they are in the interval
	 *  [10^decimalInShortestLow; 10^decimalInShortestHigh[
	 * 	(lower boundary included, greater boundary excluded).
	 *  Example: with decimalInShortestLow = -6 and
	 * 			   decimalInShortestHigh = 21:
	 *    toShortest(0.000001)  -> "0.000001"
	 *    toShortest(0.0000001) -> "1e-7"
	 *    toShortest(111111111111111111111.0)  -> "111111111111111110000"
	 *    toShortest(100000000000000000000.0)  -> "100000000000000000000"
	 *    toShortest(1111111111111111111111.0) -> "1.1111111111111111e+21"
	 *
	 *  When converting to precision mode the converter may add
	 *  max_leading_padding_zeroes before returning the number in exponential
	 *  format.
	 *  Example with maxLeadingPaddingZeroesInPrecisionMode = 6.
	 *    toPrecision(0.0000012345, 2) -> "0.0000012"
	 *    toPrecision(0.00000012345, 2) -> "1.2e-7"
	 *  Similarily the converter may add up to
	 *  maxTrailingPaddingZeroesInPrecisionMode in precision mode to avoid
	 *  returning an exponential representation. A zero added by the
	 *  EMIT_TRAILING_ZERO_AFTER_POINT flag is counted for this limit.
	 *  Examples for maxTrailingPaddingZeroesInPrecisionMode = 1:
	 *    toPrecision(230.0, 2) -> "230"
	 *    toPrecision(230.0, 2) -> "230."  with EMIT_TRAILING_DECIMAL_POINT.
	 *    toPrecision(230.0, 2) -> "2.3e2" with EMIT_TRAILING_ZERO_AFTER_POINT.
	 *
	 *  The minExponentWidth is used for exponential representations.
	 *  The converter adds leading '0's to the exponent until the exponent
	 *  is at least minExponentWidth digits long.
	 *  The minExponentWidth is clamped to 5.
	 *  As such, the exponent may never have more than 5 digits in total.
	 */
	public DoubleToStringConverter(int flags,
								   String infinitySymbol,
								   String nanSymbol,
								   int exponentCharacter,
								   int decimalInShortestLow,
								   int decimalInShortestHigh,
								   int maxLeadingPaddingZeroesInPrecisionMode,
								   int maxTrailingPaddingZeroesInPrecisionMode,
								   int minExponentWidth) {
        this.flags = flags;
		this.infinitySymbol = infinitySymbol;
		this.nanSymbol = nanSymbol;
		this.exponentCharacter = exponentCharacter;
		this.decimalInShortestLow = decimalInShortestLow;
		this.decimalInShortestHigh = decimalInShortestHigh;
		this.maxLeadingPaddingZeroesInPrecisionMode =
			maxLeadingPaddingZeroesInPrecisionMode;
		this.maxTrailingPaddingZeroesInPrecisionMode =
			maxTrailingPaddingZeroesInPrecisionMode;
		this.minExponentWidth = minExponentWidth;
		// When 'trailing zero after the point' is set, then 'trailing point'
		// must be set too.
		DOUBLE_CONVERSION_ASSERT(((flags & Flags.EMIT_TRAILING_DECIMAL_POINT) != 0) ||
				!((flags & Flags.EMIT_TRAILING_ZERO_AFTER_POINT) != 0));
	}

	/**
	 *  Returns a converter following the EcmaScript specification.
	 *
	 *  Flags: UNIQUE_ZERO and EMIT_POSITIVE_EXPONENT_SIGN.
	 *  Special values: "Infinity" and "NaN".
	 *  Lower case 'e' for exponential values.
	 *  decimal_in_shortest_low: -6
	 *  decimal_in_shortest_high: 21
	 *  max_leading_padding_zeroes_in_precision_mode: 6
	 *  max_trailing_padding_zeroes_in_precision_mode: 0
	 */
	@SuppressWarnings("ImplicitNumericConversion")
	public static DoubleToStringConverter ecmaScriptConverter() {
		int flags = Flags.UNIQUE_ZERO | Flags.EMIT_POSITIVE_EXPONENT_SIGN;
		return new DoubleToStringConverter(flags,
				"Infinity",
				"NaN",
				'e',
				-6, 21,
				6, 0);
	}


	/**
	 *  Computes the shortest string of digits that correctly represent the input
	 *  number. Depending on decimal_in_shortest_low and decimal_in_shortest_high
	 *  (see constructor) it then either returns a decimal representation, or an
	 *  exponential representation.
	 *  Example with decimal_in_shortest_low = -6,
	 * 			  decimal_in_shortest_high = 21,
	 * 			  EMIT_POSITIVE_EXPONENT_SIGN activated, and
	 * 			  EMIT_TRAILING_DECIMAL_POINT deactived:
	 *    toShortest(0.000001)  -> "0.000001"
	 *    toShortest(0.0000001) -> "1e-7"
	 *    toShortest(111111111111111111111.0)  -> "111111111111111110000"
	 *    toShortest(100000000000000000000.0)  -> "100000000000000000000"
	 *    toShortest(1111111111111111111111.0) -> "1.1111111111111111e+21"
	 *
	 *  Note: the conversion may round the output if the returned string
	 *  is accurate enough to uniquely identify the input-number.
	 *  For example the most precise representation of the double 9e59 equals
	 *  "899999999999999918767229449717619953810131273674690656206848", but
	 *  the converter will return the shorter (but still correct) "9e59".
	 *
	 *  Returns true if the conversion succeeds. The conversion always succeeds
	 *  except when the input value is special and no infinity_symbol or
	 *  nan_symbol has been given to the constructor.
	 *
	 *  The length of the longest result is the maximum of the length of the
	 *  following string representations (each with possible examples):
	 *  - NaN and negative infinity: "NaN", "-Infinity", "-inf".
	 *  - -10^(decimal_in_shortest_high - 1):
	 * 	  "-100000000000000000000", "-1000000000000000.0"
	 *  - the longest string in range [0; -10^decimal_in_shortest_low]. Generally,
	 *    this string is 3 + kBase10MaximalLength - decimal_in_shortest_low.
	 *    (sign, '0', decimal point, padding zeroes for decimal_in_shortest_low,
	 *    and the significant digits).
	 * 	  "-0.0000033333333333333333", "-0.0012345678901234567"
	 *  - the longest exponential representation. (A negative number with
	 *    kBase10MaximalLength significant digits).
	 * 	  "-1.7976931348623157e+308", "-1.7976931348623157E308"
	 *  In addition, the buffer must be able to hold the trailing '\0' character.
	 */
	public boolean toShortest(double value, StringBuilder resultBuilder) {
		return toShortestIeeeNumber(value, resultBuilder, DtoaMode.SHORTEST);
	}

	/** Same as toShortest, but for single-precision floats. */
	public boolean toShortestSingle(float value, StringBuilder resultBuilder) {
		//noinspection ImplicitNumericConversion
		return toShortestIeeeNumber(value, resultBuilder, DtoaMode.SHORTEST_SINGLE);
	}

	/**
	 * 	 If the value is a special value (NaN or Infinity) constructs the
	 * 	 corresponding string using the configured infinity/nan-symbol.
	 * 	 If either of them is NULL or the value is not special then the
	 * 	 function returns false.
	 */
	private boolean handleSpecialValues(double value, StringBuilder resultBuilder) {
		Ieee.Double doubleInspect = new Ieee.Double(value);
		if (doubleInspect.isInfinite()) {
			if (infinitySymbol == null) return false;
			if (value < 0.0) {
				resultBuilder.append('-');
			}
			resultBuilder.append(infinitySymbol);
			return true;
		}
		if (doubleInspect.isNan()) {
			if (nanSymbol == null) return false;
			resultBuilder.append(nanSymbol);
			return true;
		}
		return false;
	}

	/**
	 * 	 Constructs an exponential representation (i.e. 1.234e56).
	 * 	 The given exponent assumes a decimal point after the first decimal digit.
	 */
	private void createExponentialRepresentation(final char[] decimalDigits,
												 int length,
												 int exponent,
												 StringBuilder resultBuilder) {
		DOUBLE_CONVERSION_ASSERT(length != 0);
		resultBuilder.append(decimalDigits[0]);
		if (length != 1) {
			resultBuilder.append('.');
			resultBuilder.append(decimalDigits, 1, length-1);
		}
		resultBuilder.append((char)exponentCharacter);
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
		final int kMaxExponentLength = 5;
		char[] buffer = new char[kMaxExponentLength + 1];
		buffer[kMaxExponentLength] = '\0';
		int firstCharPos = kMaxExponentLength;
		if (exponent == 0) {
			buffer[--firstCharPos] = (char) ASCII_ZERO;
		} else {
			while (exponent > 0) {
				buffer[--firstCharPos] = (char) (ASCII_ZERO + (exponent % 10));
				exponent /= 10;
			}
		}
		// Add prefix '0' to make exponent width >= min(min_exponent_with_, kMaxExponentLength)
		// For example: convert 1e+9 -> 1e+09, if min_exponent_with_ is set to 2
		while(kMaxExponentLength - firstCharPos < Math.min(minExponentWidth, kMaxExponentLength)) {
			buffer[--firstCharPos] = (char) ASCII_ZERO;
		}
		// TODO verify this is equivalent
		resultBuilder.append(buffer, firstCharPos, kMaxExponentLength -firstCharPos);
//		resultBuilder->AddSubstring(&buffer[firstCharPos],
//				kMaxExponentLength - firstCharPos);
	}

	/** Creates a decimal representation (i.e 1234.5678). */
	private void createDecimalRepresentation(final char[] decimalDigits,
											 int length,
											 int decimalPoint,
											 int digitsAfterPoint,
											 StringBuilder resultBuilder) {
		// Create a representation that is padded with zeros if needed.
		if (decimalPoint <= 0) {
			// "0.00000decimal_rep" or "0.000decimal_rep00".
			resultBuilder.append(ASCII_ZERO);
			if (digitsAfterPoint > 0) {
				resultBuilder.append('.');

				addPadding(resultBuilder, ASCII_ZERO, -decimalPoint);
				DOUBLE_CONVERSION_ASSERT(length <= digitsAfterPoint - (-decimalPoint));
				resultBuilder.append(decimalDigits);
				int remainingDigits = digitsAfterPoint - (-decimalPoint) - length;
				addPadding(resultBuilder, ASCII_ZERO, remainingDigits);
			}
		} else if (decimalPoint >= length) {
			// "decimal_rep0000.00000" or "decimalRep.0000".
			resultBuilder.append(decimalDigits);
			addPadding(resultBuilder, ASCII_ZERO, decimalPoint - length);
			if (digitsAfterPoint > 0) {
				resultBuilder.append('.');
				addPadding(resultBuilder, ASCII_ZERO, digitsAfterPoint);
			}
		} else {
			// "decima.l_rep000".
			DOUBLE_CONVERSION_ASSERT(digitsAfterPoint > 0);
			resultBuilder.append(decimalDigits, 0, decimalPoint);
			resultBuilder.append('.');
			DOUBLE_CONVERSION_ASSERT(length - decimalPoint <= digitsAfterPoint);
			resultBuilder.append(decimalDigits, decimalPoint, length - decimalPoint);
			int remainingDigits = digitsAfterPoint - (length - decimalPoint);
			addPadding(resultBuilder, ASCII_ZERO, remainingDigits);
		}
		if (digitsAfterPoint == 0) {
			if ((flags & Flags.EMIT_TRAILING_DECIMAL_POINT) != 0) {
				resultBuilder.append('.');
			}
			if ((flags & Flags.EMIT_TRAILING_ZERO_AFTER_POINT) != 0) {
				resultBuilder.append(ASCII_ZERO);
			}
		}
	}

	/** Implementation for toShortest and toShortestSingle. */
	private boolean toShortestIeeeNumber(double value,
										 StringBuilder resultBuilder,
										 DtoaMode mode) {
		DOUBLE_CONVERSION_ASSERT(mode == DtoaMode.SHORTEST || mode == DtoaMode.SHORTEST_SINGLE);
		if (new Ieee.Double(value).isSpecial()) {
			return handleSpecialValues(value, resultBuilder);
		}

		int[] decimalPoint = new int[1];
		boolean[] sign = new boolean[1];
		final int kDecimalRepCapacity = BASE_10_MAXIMAL_LENGTH + 1;
		char[] decimalRep = new char[kDecimalRepCapacity];
		int[] decimalRepLength = new int[1];

		doubleToAscii(value, mode, 0, decimalRep, kDecimalRepCapacity,
				sign, decimalRepLength, decimalPoint);

		boolean unique_zero = (flags & Flags.UNIQUE_ZERO) != 0;
		if (sign[0] && (value != 0.0 || !unique_zero)) {
			resultBuilder.append('-');
		}

		int exponent = decimalPoint[0] - 1;
		if ((decimalInShortestLow <= exponent) &&
				(exponent < decimalInShortestHigh)) {
			createDecimalRepresentation(decimalRep, decimalRepLength[0],
					decimalPoint[0],
					Math.max(0, decimalRepLength[0] - decimalPoint[0]),
					resultBuilder);
		} else {
			createExponentialRepresentation(decimalRep, decimalRepLength[0], exponent,
					resultBuilder);
		}
		return true;
	}


	/**
	 *  Computes a decimal representation with a fixed number of digits after the
	 *  decimal point. The last emitted digit is rounded.
	 *
	 *  Examples:
	 *    toFixed(3.12, 1) -> "3.1"
	 *    toFixed(3.1415, 3) -> "3.142"
	 *    toFixed(1234.56789, 4) -> "1234.5679"
	 *    toFixed(1.23, 5) -> "1.23000"
	 *    toFixed(0.1, 4) -> "0.1000"
	 *    toFixed(1e30, 2) -> "1000000000000000019884624838656.00"
	 *    toFixed(0.1, 30) -> "0.100000000000000005551115123126"
	 *    toFixed(0.1, 17) -> "0.10000000000000001"
	 *
	 *  If requestedDigits equals 0, then the tail of the result depends on
	 *  the EMIT_TRAILING_DECIMAL_POINT and EMIT_TRAILING_ZERO_AFTER_POINT.
	 *  Examples, for requestedDigits == 0,
	 *    let EMIT_TRAILING_DECIMAL_POINT and EMIT_TRAILING_ZERO_AFTER_POINT be
	 * 	- false and false: then 123.45 -> 123
	 * 							 0.678 -> 1
	 * 	- true and false: then 123.45 -> 123.
	 * 							0.678 -> 1.
	 * 	- true and true: then 123.45 -> 123.0
	 * 						   0.678 -> 1.0
	 *
	 *  Returns true if the conversion succeeds. The conversion always succeeds
	 *  except for the following cases:
	 *    - the input value is special and no infinity_symbol or nan_symbol has
	 * 	 been provided to the constructor,
	 *    - 'value' > 10^kMaxFixedDigitsBeforePoint, or
	 *    - 'requestedDigits' > kMaxFixedDigitsAfterPoint.
	 *  The last two conditions imply that the result for non-special values never
	 *  contains more than
	 *   1 + kMaxFixedDigitsBeforePoint + 1 + kMaxFixedDigitsAfterPoint characters
	 *  (one additional character for the sign, and one for the decimal point).
	 *  In addition, the buffer must be able to hold the trailing '\0' character.
	 */
	public boolean toFixed(double value,
					int requestedDigits,
					StringBuilder resultBuilder) {
		DOUBLE_CONVERSION_ASSERT(MAX_FIXED_DIGITS_BEFORE_POINT == 60);
  		final double kFirstNonFixed = 1e60;

		if (new Ieee.Double(value).isSpecial()) {
			return handleSpecialValues(value, resultBuilder);
		}

		if (requestedDigits > MAX_FIXED_DIGITS_AFTER_POINT) return false;
		if (value >= kFirstNonFixed || value <= -kFirstNonFixed) return false;

		// Find a sufficiently precise decimal representation of n.
		int[] decimalPoint = new int[1];
		boolean[] sign = new boolean[1];
		// Add space for the '\0' byte.
  		final int kDecimalRepCapacity =
				MAX_FIXED_DIGITS_BEFORE_POINT + MAX_FIXED_DIGITS_AFTER_POINT + 1;
		char[] decimalRep = new char[kDecimalRepCapacity];
		int[] decimalRepLength = new int[1];
		doubleToAscii(value, DtoaMode.FIXED, requestedDigits,
				decimalRep, kDecimalRepCapacity,
				sign, decimalRepLength, decimalPoint);

		boolean uniqueZero = ((flags & Flags.UNIQUE_ZERO) != 0);
		if (sign[0] && (value != 0.0 || !uniqueZero)) {
			resultBuilder.append('-');
		}

		createDecimalRepresentation(decimalRep, decimalRepLength[0], decimalPoint[0],
				requestedDigits, resultBuilder);
		return true;
	}

	/**
	 *  Computes a representation in exponential format with requestedDigits
	 *  after the decimal point. The last emitted digit is rounded.
	 *  If requestedDigits equals -1, then the shortest exponential representation
	 *  is computed.
	 *
	 *  Examples with EMIT_POSITIVE_EXPONENT_SIGN deactivated, and
	 *  exponent_character set to 'e'.
	 *  toExponential(3.12, 1) -> "3.1e0"
	 *  toExponential(5.0, 3) -> "5.000e0"
	 *  toExponential(0.001, 2) -> "1.00e-3"
	 *  toExponential(3.1415, -1) -> "3.1415e0"
	 *  toExponential(3.1415, 4) -> "3.1415e0"
	 *  toExponential(3.1415, 3) -> "3.142e0"
	 *  toExponential(123456789000000, 3) -> "1.235e14"
	 *  toExponential(1000000000000000019884624838656.0, -1) -> "1e30"
	 *  toExponential(1000000000000000019884624838656.0, 32) ->
	 *  "1.00000000000000001988462483865600e30"
	 *  toExponential(1234, 0) -> "1e3"
	 *
	 *  Returns true if the conversion succeeds. The conversion always succeeds
	 *  except for the following cases:
	 *  - the input value is special and no infinity_symbol or nan_symbol has
	 *  been provided to the constructor,
	 *  - 'requestedDigits' > kMaxExponentialDigits.
	 *  The last condition implies that the result will never contains more than
	 *  kMaxExponentialDigits + 8 characters (the sign, the digit before the
	 *  decimal point, the decimal point, the exponent character, the
	 *  exponent's sign, and at most 3 exponent digits).
	 */
	public boolean toExponential(double value,
						  int requestedDigits,
						  StringBuilder resultBuilder) {
		if (new Ieee.Double(value).isSpecial()) {
			return handleSpecialValues(value, resultBuilder);
		}

		if (requestedDigits < -1) return false;
		if (requestedDigits > MAX_EXPONENTIAL_DIGITS) return false;

		int[] decimalPoint = new int[1];
		boolean[] sign = new boolean[1];
		// Add space for digit before the decimal point and the '\0' character.
  		final int kDecimalRepCapacity = MAX_EXPONENTIAL_DIGITS + 2;
		DOUBLE_CONVERSION_ASSERT(kDecimalRepCapacity > BASE_10_MAXIMAL_LENGTH);
		char[] decimalRep = new char[kDecimalRepCapacity];
		// TODO make sure this isn't a problem in java
//#ifndef NDEBUG
//		// Problem: there is an assert in StringBuilder::AddSubstring() that
//		// will pass this buffer to strlen(), and this buffer is not generally
//		// null-terminated.
//		memset(decimalRep, 0, sizeof(decimalRep));
//#endif
		int[] decimalRepLength = new int[1];

		if (requestedDigits == -1) {
			doubleToAscii(value, DtoaMode.SHORTEST, 0,
					decimalRep, kDecimalRepCapacity,
					sign, decimalRepLength, decimalPoint);
		} else {
			doubleToAscii(value, DtoaMode.PRECISION, requestedDigits + 1,
					decimalRep, kDecimalRepCapacity,
					sign, decimalRepLength, decimalPoint);
			DOUBLE_CONVERSION_ASSERT(decimalRepLength[0] <= requestedDigits + 1);

			for (int i = decimalRepLength[0]; i < requestedDigits + 1; ++i) {
				decimalRep[i] = (char) ASCII_ZERO;
			}
			decimalRepLength[0] = requestedDigits + 1;
		}

		boolean uniqueZero = ((flags & Flags.UNIQUE_ZERO) != 0);
		if (sign[0] && (value != 0.0 || !uniqueZero)) {
			resultBuilder.append('-');
		}

		int exponent = decimalPoint[0] - 1;
		createExponentialRepresentation(decimalRep,
				decimalRepLength[0],
				exponent,
				resultBuilder);
		return true;
	}

	/**
	 *  Computes 'precision' leading digits of the given 'value' and returns them
	 *  either in exponential or decimal format, depending on
	 *  max_{leading|trailing}_padding_zeroes_in_precision_mode (given to the
	 *  constructor).
	 *  The last computed digit is rounded.
	 *
	 *  Example with max_leading_padding_zeroes_in_precision_mode = 6.
	 *    toPrecision(0.0000012345, 2) -> "0.0000012"
	 *    toPrecision(0.00000012345, 2) -> "1.2e-7"
	 *  Similarily the converter may add up to
	 *  max_trailing_padding_zeroes_in_precision_mode in precision mode to avoid
	 *  returning an exponential representation. A zero added by the
	 *  EMIT_TRAILING_ZERO_AFTER_POINT flag is counted for this limit.
	 *  Examples for max_trailing_padding_zeroes_in_precision_mode = 1:
	 *    toPrecision(230.0, 2) -> "230"
	 *    toPrecision(230.0, 2) -> "230."  with EMIT_TRAILING_DECIMAL_POINT.
	 *    toPrecision(230.0, 2) -> "2.3e2" with EMIT_TRAILING_ZERO_AFTER_POINT.
	 *  Examples for max_trailing_padding_zeroes_in_precision_mode = 3, and no
	 * 	EMIT_TRAILING_ZERO_AFTER_POINT:
	 *    toPrecision(123450.0, 6) -> "123450"
	 *    toPrecision(123450.0, 5) -> "123450"
	 *    toPrecision(123450.0, 4) -> "123500"
	 *    toPrecision(123450.0, 3) -> "123000"
	 *    toPrecision(123450.0, 2) -> "1.2e5"
	 *
	 *  Returns true if the conversion succeeds. The conversion always succeeds
	 *  except for the following cases:
	 *    - the input value is special and no infinity_symbol or nan_symbol has
	 * 	 been provided to the constructor,
	 *    - precision < kMinPericisionDigits
	 *    - precision > kMaxPrecisionDigits
	 *
	 *  The last condition implies that the result never contains more than
	 *  kMaxPrecisionDigits + 7 characters (the sign, the decimal point, the
	 *  exponent character, the exponent's sign, and at most 3 exponent digits).
	 *  In addition, the buffer must be able to hold the trailing '\0' character.
	 */
	public boolean toPrecision(double value,
						int precision,
						StringBuilder resultBuilder) {
		if (new Ieee.Double(value).isSpecial()) {
			return handleSpecialValues(value, resultBuilder);
		}

		if (precision < MIN_PRECISION_DIGITS || precision > MAX_PRECISION_DIGITS) {
			return false;
		}

		// Find a sufficiently precise decimal representation of n.
		int decimalPoint;
		boolean sign;
		// Add one for the terminating null character.
  		final int kDecimalRepCapacity = MAX_PRECISION_DIGITS + 1;
		char[] decimalRep = new char[kDecimalRepCapacity];
		int decimalRepLength;

		{
			int[] inDecimalPoint = new int[1];
			boolean[] inSign = new boolean[1];
			int[] inDecimalRepLength = new int[1];
			doubleToAscii(value, DtoaMode.PRECISION, precision,
					decimalRep, kDecimalRepCapacity,
					inSign, inDecimalRepLength, inDecimalPoint);
			DOUBLE_CONVERSION_ASSERT(inDecimalRepLength[0] <= precision);
			decimalPoint = inDecimalPoint[0];
			sign = inSign[0];
			decimalRepLength = inDecimalRepLength[0];
		}

		boolean uniqueZero = ((flags & Flags.UNIQUE_ZERO) != 0);
		if (sign && (value != 0.0 || !uniqueZero)) {
			resultBuilder.append('-');
		}

		// The exponent if we print the number as x.xxeyyy. That is with the
		// decimal point after the first digit.
		int exponent = decimalPoint - 1;

		int extraZero = ((flags & Flags.EMIT_TRAILING_ZERO_AFTER_POINT) != 0) ? 1 : 0;
		boolean asExponential =
				(-decimalPoint + 1 > maxLeadingPaddingZeroesInPrecisionMode) ||
						(decimalPoint - precision + extraZero >
								maxTrailingPaddingZeroesInPrecisionMode);
		if ((flags & Flags.NO_TRAILING_ZERO) != 0) {
			// Truncate trailing zeros that occur after the decimal point (if exponential,
			// that is everything after the first digit).
			int stop = asExponential ? 1 : Math.max(1, decimalPoint);
			while (decimalRepLength > stop && (int) decimalRep[decimalRepLength - 1] == ASCII_ZERO) {
				--decimalRepLength;
			}
			// Clamp precision to avoid the code below re-adding the zeros.
			precision = Math.min(precision, decimalRepLength);
		}
		if (asExponential) {
			// Fill buffer to contain 'precision' digits.
			// Usually the buffer is already at the correct length, but 'doubleToAscii'
			// is allowed to return less characters.
			for (int i = decimalRepLength; i < precision; ++i) {
				decimalRep[i] = (char) ASCII_ZERO;
			}

			createExponentialRepresentation(decimalRep,
					precision,
					exponent,
					resultBuilder);
		} else {
			createDecimalRepresentation(decimalRep, decimalRepLength, decimalPoint,
					Math.max(0, precision - decimalPoint),
					resultBuilder);
		}
		return true;
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
	 * 	 Converts the given double 'v' to digit characters. 'v' must not be NaN,
	 * 	 +Infinity, or -Infinity. In SHORTEST_SINGLE-mode this restriction also
	 * 	 applies to 'v' after it has been casted to a single-precision float. That
	 * 	 is, in this mode static_cast<float>(v) must not be NaN, +Infinity or
	 * 	 -Infinity.
	 *
	 * 	 The result should be interpreted as outBuffer * 10^(outPoint-outLength).
	 *
	 * 	 The digits are written to the outBuffer in the platform's charset, which is
	 * 	 often UTF-8 (with ASCII-range digits) but may be another charset, such
	 * 	 as EBCDIC.
	 *
	 * 	 The output depends on the given mode:
	 * 	  - SHORTEST: produce the least amount of digits for which the internal
	 * 	   identity requirement is still satisfied. If the digits are printed
	 * 	   (together with the correct exponent) then reading this number will give
	 * 	   'v' again. The outBuffer will choose the representation that is closest to
	 * 	   'v'. If there are two at the same distance, than the one farther away
	 * 	   from 0 is chosen (halfway cases - ending with 5 - are rounded up).
	 * 	   In this mode the 'requestedDigits' parameter is ignored.
	 * 	  - SHORTEST_SINGLE: same as SHORTEST but with single-precision.
	 * 	  - FIXED: produces digits necessary to print a given number with
	 * 	   'requestedDigits' digits after the decimal outPoint. The produced digits
	 * 	   might be too short in which case the caller has to fill the remainder
	 * 	   with '0's.
	 * 	   Example: toFixed(0.001, 5) is allowed to return outBuffer="1", outPoint=-2.
	 * 	   Halfway cases are rounded towards +/-Infinity (away from 0). The call
	 * 	   toFixed(0.15, 2) thus returns outBuffer="2", outPoint=0.
	 * 	   The returned outBuffer may contain digits that would be truncated from the
	 * 	   shortest representation of the input.
	 * 	  - PRECISION: produces 'requestedDigits' where the first digit is not '0'.
	 * 	   Even though the outLength of produced digits usually equals
	 * 	   'requestedDigits', the function is allowed to return fewer digits, in
	 * 	   which case the caller has to fill the missing digits with '0's.
	 * 	   Halfway cases are again rounded away from 0.
	 * 	 doubleToAscii expects the given outBuffer to be big enough to hold all
	 * 	 digits and a terminating null-character. In SHORTEST-mode it expects a
	 * 	 outBuffer of at least kBase10MaximalLength + 1. In all other modes the
	 * 	 requestedDigits parameter and the padding-zeroes limit the size of the
	 * 	 output. Don't forget the decimal outPoint, the exponent character and the
	 * 	 terminating null-character when computing the maximal output size.
	 * 	 The given outLength is only used in debug mode to ensure the outBuffer is big
	 * 	 enough.
	 *
	 */
	public static void doubleToAscii(double v,
										DtoaMode mode,
										int requestedDigits,
										char[] vector,
										int bufferLength,
										boolean[] sign,
										int[] length,
										int[] point) {
		DOUBLE_CONVERSION_ASSERT(!new Ieee.Double(v).isSpecial());
		DOUBLE_CONVERSION_ASSERT(mode == DtoaMode.SHORTEST || mode == DtoaMode.SHORTEST_SINGLE || requestedDigits >= 0);

		if (new Ieee.Double(v).sign() < 0) {
			sign[0] = true;
			v = -v;
		} else {
    		sign[0] = false;
		}

		if (mode == DtoaMode.PRECISION && requestedDigits == 0) {
			vector[0] = '\0';
			length[0] = 0;
			return;
		}

		if (v == 0.0) {
			vector[0] = (char) ASCII_ZERO;
			vector[1] = '\0';
			length[0] = 1;
			point[0] = 1;
			return;
		}

		boolean fastWorked;
		switch (mode) {
			case SHORTEST:
				fastWorked = FastDtoa.fastDtoa(v, FastDtoa.FastDtoaMode.SHORTEST, 0, vector, length, point);
				break;
			case SHORTEST_SINGLE:
				fastWorked = FastDtoa.fastDtoa(v, FastDtoa.FastDtoaMode.SHORTEST_SINGLE, 0,
						vector, length, point);
				break;
			case FIXED:
				fastWorked = fastFixedDtoa(v, requestedDigits, vector, length, point);
				break;
			case PRECISION:
				fastWorked = FastDtoa.fastDtoa(v, FastDtoa.FastDtoaMode.PRECISION, requestedDigits,
						vector, length, point);
				break;
			default:
				fastWorked = false;
				throw new IllegalStateException("Unreachable");
		}
		if (fastWorked) return;

		// If the fast dtoa didn't succeed use the slower bignum version.
		BigNumDtoa.BignumDtoaMode dtoaMode = dtoaToBignumDtoaMode(mode);
		BigNumDtoa.bignumDtoa(v, dtoaMode, requestedDigits, vector, length, point);
		vector[length[0]] = '\0';
	}

	/**
	 * 	Add character padding to the builder. If count is non-positive,
	 * 	nothing is added to the builder.
	 */
	private static void addPadding(StringBuilder sb, int c, int count) {
		for (int i=count; i > 0; i--) {
			sb.append(c);
		}
	}
}
