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

package org.squiddev.cobalt.lib.fmt;

import static org.squiddev.cobalt.lib.fmt.Assert.DOUBLE_CONVERSION_ASSERT;
import static org.squiddev.cobalt.lib.fmt.FixedDtoa.FastFixedDtoa;

public class DoubleToStringConverter {
	/**
	 *  When calling ToFixed with a double > 10^kMaxFixedDigitsBeforePoint
	 *  or a requested_digits parameter > kMaxFixedDigitsAfterPoint then the
	 *  function returns false.
	 */
	public static final int kMaxFixedDigitsBeforePoint = 60;
	public static final int kMaxFixedDigitsAfterPoint = 100;

	/**
	 *  When calling ToExponential with a requested_digits
	 *  parameter > kMaxExponentialDigits then the function returns false.
	 */
	public static final int kMaxExponentialDigits = 120;

	/**
	 *  When calling ToPrecision with a requested_digits
	 *  parameter < kMinPrecisionDigits or requested_digits > kMaxPrecisionDigits
	 *  then the function returns false.
	 */
	public static final int kMinPrecisionDigits = 1;
	/**
	 *  When calling ToPrecision with a requested_digits
	 *  parameter < kMinPrecisionDigits or requested_digits > kMaxPrecisionDigits
	 *  then the function returns false.
	 */
	public static final int kMaxPrecisionDigits = 120;

	/**
	 * 	 The maximal number of digits that are needed to emit a double in base 10.
	 * 	 A higher precision can be achieved by using more digits, but the shortest
	 * 	 accurate representation of any double will never use more digits than
	 * 	 kBase10MaximalLength.
	 * 	 Note that DoubleToAscii null-terminates its input. So the given buffer
	 * 	 should be at least kBase10MaximalLength + 1 characters long.
	 */
	public static final int kBase10MaximalLength = 17;

	/**
	 *  The maximal number of digits that are needed to emit a single in base 10.
	 *  A higher precision can be achieved by using more digits, but the shortest
	 *  accurate representation of any single will never use more digits than
	 *  kBase10MaximalLengthSingle.
	 */
	public static final int kBase10MaximalLengthSingle = 9;

	/**
	 *  The length of the longest string that 'ToShortest' can produce when the
	 *  converter is instantiated with EcmaScript defaults (see
	 *  'EcmaScriptConverter')
	 *  This value does not include the trailing '\0' character.
	 *  This amount of characters is needed for negative values that hit the
	 *  'decimal_in_shortest_low' limit. For example: "-0.0000033333333333333333"
	 */
	public static final int kMaxCharsEcmaScriptShortest = 25;

	public static class Flags {
		public static final int NO_FLAGS = 0;
		public static final int EMIT_POSITIVE_EXPONENT_SIGN = 1;
		public static final int EMIT_TRAILING_DECIMAL_POINT = 2;
		public static final int EMIT_TRAILING_ZERO_AFTER_POINT = 4;
		public static final int UNIQUE_ZERO = 8;
		public static final int NO_TRAILING_ZERO = 16;
	};


	private final int flags_;
	private final String infinity_symbol_;
	private final String nan_symbol_;
	private final char exponent_character_;
	private final int decimal_in_shortest_low_;
	private final int decimal_in_shortest_high_;
	private final int max_leading_padding_zeroes_in_precision_mode_;
	private final int max_trailing_padding_zeroes_in_precision_mode_;
	private final int min_exponent_width_;

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
	 *  Infinity symbol and nan_symbol provide the string representation for these
	 *  special values. If the string is NULL and the special value is encountered
	 *  then the conversion functions return false.
	 *
	 *  The exponent_character is used in exponential representations. It is
	 *  usually 'e' or 'E'.
	 *
	 *  When converting to the shortest representation the converter will
	 *  represent input numbers in decimal format if they are in the interval
	 *  [10^decimal_in_shortest_low; 10^decimal_in_shortest_high[
	 * 	(lower boundary included, greater boundary excluded).
	 *  Example: with decimal_in_shortest_low = -6 and
	 * 			   decimal_in_shortest_high = 21:
	 *    ToShortest(0.000001)  -> "0.000001"
	 *    ToShortest(0.0000001) -> "1e-7"
	 *    ToShortest(111111111111111111111.0)  -> "111111111111111110000"
	 *    ToShortest(100000000000000000000.0)  -> "100000000000000000000"
	 *    ToShortest(1111111111111111111111.0) -> "1.1111111111111111e+21"
	 *
	 *  When converting to precision mode the converter may add
	 *  max_leading_padding_zeroes before returning the number in exponential
	 *  format.
	 *  Example with max_leading_padding_zeroes_in_precision_mode = 6.
	 *    ToPrecision(0.0000012345, 2) -> "0.0000012"
	 *    ToPrecision(0.00000012345, 2) -> "1.2e-7"
	 *  Similarily the converter may add up to
	 *  max_trailing_padding_zeroes_in_precision_mode in precision mode to avoid
	 *  returning an exponential representation. A zero added by the
	 *  EMIT_TRAILING_ZERO_AFTER_POINT flag is counted for this limit.
	 *  Examples for max_trailing_padding_zeroes_in_precision_mode = 1:
	 *    ToPrecision(230.0, 2) -> "230"
	 *    ToPrecision(230.0, 2) -> "230."  with EMIT_TRAILING_DECIMAL_POINT.
	 *    ToPrecision(230.0, 2) -> "2.3e2" with EMIT_TRAILING_ZERO_AFTER_POINT.
	 *
	 *  The min_exponent_width is used for exponential representations.
	 *  The converter adds leading '0's to the exponent until the exponent
	 *  is at least min_exponent_width digits long.
	 *  The min_exponent_width is clamped to 5.
	 *  As such, the exponent may never have more than 5 digits in total.
	 */
	public DoubleToStringConverter(int flags,
							String infinity_symbol,
						    String nan_symbol,
							char exponent_character,
							int decimal_in_shortest_low,
							int decimal_in_shortest_high,
							int max_leading_padding_zeroes_in_precision_mode,
							int max_trailing_padding_zeroes_in_precision_mode,
							int min_exponent_width = 0) {
        this.flags_ = flags;
		this.infinity_symbol_ = infinity_symbol;
		this.nan_symbol_ = nan_symbol;
		this.exponent_character_ = exponent_character;
		this.decimal_in_shortest_low_ = decimal_in_shortest_low;
		this.decimal_in_shortest_high_ = decimal_in_shortest_high;
		this.max_leading_padding_zeroes_in_precision_mode_ =
			max_leading_padding_zeroes_in_precision_mode;
		this.max_trailing_padding_zeroes_in_precision_mode_ =
			max_trailing_padding_zeroes_in_precision_mode;
		this.min_exponent_width_ = min_exponent_width;
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
	public static final DoubleToStringConverter EcmaScriptConverter() {
		int flags = Flags.UNIQUE_ZERO | Flags.EMIT_POSITIVE_EXPONENT_SIGN;
		return new DoubleToStringConverter(flags,
				"Infinity",
				"NaN",
				'e',
				-6, 21,
				6, 0, 0);
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
	 *    ToShortest(0.000001)  -> "0.000001"
	 *    ToShortest(0.0000001) -> "1e-7"
	 *    ToShortest(111111111111111111111.0)  -> "111111111111111110000"
	 *    ToShortest(100000000000000000000.0)  -> "100000000000000000000"
	 *    ToShortest(1111111111111111111111.0) -> "1.1111111111111111e+21"
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
	 *    (Sign, '0', decimal point, padding zeroes for decimal_in_shortest_low,
	 *    and the significant digits).
	 * 	  "-0.0000033333333333333333", "-0.0012345678901234567"
	 *  - the longest exponential representation. (A negative number with
	 *    kBase10MaximalLength significant digits).
	 * 	  "-1.7976931348623157e+308", "-1.7976931348623157E308"
	 *  In addition, the buffer must be able to hold the trailing '\0' character.
	 */
	public boolean ToShortest(double value, StringBuilder result_builder) {
		return ToShortestIeeeNumber(value, result_builder, DtoaMode.SHORTEST);
	}

	/** Same as ToShortest, but for single-precision floats. */
	public boolean ToShortestSingle(float value, StringBuilder result_builder) {
		return ToShortestIeeeNumber(value, result_builder, DtoaMode.SHORTEST_SINGLE);
	}

	/**
	 * 	 If the value is a special value (NaN or Infinity) constructs the
	 * 	 corresponding string using the configured infinity/nan-symbol.
	 * 	 If either of them is NULL or the value is not special then the
	 * 	 function returns false.
	 */
	private boolean HandleSpecialValues(double value, StringBuilder result_builder) {
		Ieee.Double double_inspect = new Ieee.Double(value);
		if (double_inspect.IsInfinite()) {
			if (infinity_symbol_ == null) return false;
			if (value < 0) {
				result_builder.append('-');
			}
			result_builder.append(infinity_symbol_);
			return true;
		}
		if (double_inspect.IsNan()) {
			if (nan_symbol_ == null) return false;
			result_builder.append(nan_symbol_);
			return true;
		}
		return false;
	}

	/**
	 * 	 Constructs an exponential representation (i.e. 1.234e56).
	 * 	 The given exponent assumes a decimal point after the first decimal digit.
	 */
	private void CreateExponentialRepresentation(final char[] decimal_digits,
												 int length,
												 int exponent,
												 StringBuilder result_builder) {
		DOUBLE_CONVERSION_ASSERT(length != 0);
		result_builder.append(decimal_digits[0]);
		if (length != 1) {
			result_builder.append('.');
			result_builder.append(decimal_digits, 1, length-1);
		}
		result_builder.append(exponent_character_);
		if (exponent < 0) {
			result_builder.append('-');
			exponent = -exponent;
		} else {
			if ((flags_ & Flags.EMIT_POSITIVE_EXPONENT_SIGN) != 0) {
				result_builder.append('+');
			}
		}
		DOUBLE_CONVERSION_ASSERT(exponent < 1e4);
		// Changing this constant requires updating the comment of DoubleToStringConverter constructor
		final int kMaxExponentLength = 5;
		char[] buffer = new char[kMaxExponentLength + 1];
		buffer[kMaxExponentLength] = '\0';
		int first_char_pos = kMaxExponentLength;
		if (exponent == 0) {
			buffer[--first_char_pos] = '0';
		} else {
			while (exponent > 0) {
				buffer[--first_char_pos] = '0' + (exponent % 10);
				exponent /= 10;
			}
		}
		// Add prefix '0' to make exponent width >= min(min_exponent_with_, kMaxExponentLength)
		// For example: convert 1e+9 -> 1e+09, if min_exponent_with_ is set to 2
		while(kMaxExponentLength - first_char_pos < std::min(min_exponent_width_, kMaxExponentLength)) {
			buffer[--first_char_pos] = '0';
		}
		result_builder.AddSubstring(buffer[first_char_pos],
				kMaxExponentLength - first_char_pos);
	}

	/** Creates a decimal representation (i.e 1234.5678). */
	private void CreateDecimalRepresentation(final char[] decimal_digits,
											 int length,
											 int decimal_point,
											 int digits_after_point,
											 StringBuilder result_builder) {
		// Create a representation that is padded with zeros if needed.
		if (decimal_point <= 0) {
			// "0.00000decimal_rep" or "0.000decimal_rep00".
			result_builder.append('0');
			if (digits_after_point > 0) {
				result_builder.append('.');

				addPadding(result_builder, '0', -decimal_point);
				DOUBLE_CONVERSION_ASSERT(length <= digits_after_point - (-decimal_point));
				result_builder.append(decimal_digits);
				int remaining_digits = digits_after_point - (-decimal_point) - length;
				addPadding(result_builder, '0', remaining_digits);
			}
		} else if (decimal_point >= length) {
			// "decimal_rep0000.00000" or "decimal_rep.0000".
			result_builder.append(decimal_digits);
			addPadding(result_builder, '0', decimal_point - length);
			if (digits_after_point > 0) {
				result_builder.append('.');
				addPadding(result_builder, '0', digits_after_point);
			}
		} else {
			// "decima.l_rep000".
			DOUBLE_CONVERSION_ASSERT(digits_after_point > 0);
			result_builder.append(decimal_digits, 0, decimal_point+1);
			result_builder.append('.');
			DOUBLE_CONVERSION_ASSERT(length - decimal_point <= digits_after_point);
			result_builder.append(decimal_digits, decimal_point, length - decimal_point);
			int remaining_digits = digits_after_point - (length - decimal_point);
			addPadding(result_builder, '0', remaining_digits);
		}
		if (digits_after_point == 0) {
			if ((flags_ & Flags.EMIT_TRAILING_DECIMAL_POINT) != 0) {
				result_builder.append('.');
			}
			if ((flags_ & Flags.EMIT_TRAILING_ZERO_AFTER_POINT) != 0) {
				result_builder.append('0');
			}
		}
	}

	/** Implementation for ToShortest and ToShortestSingle. */
	private boolean ToShortestIeeeNumber(double value,
										 StringBuilder result_builder,
										 DtoaMode mode) {
		DOUBLE_CONVERSION_ASSERT(mode == DtoaMode.SHORTEST || mode == DtoaMode.SHORTEST_SINGLE);
		if (new Ieee.Double(value).IsSpecial()) {
			return HandleSpecialValues(value, result_builder);
		}

		int[] decimal_point = new int[1];
		boolean[] sign = new boolean[1];
		final int kDecimalRepCapacity = kBase10MaximalLength + 1;
		char[] decimal_rep = new char[kDecimalRepCapacity];
		int[] decimal_rep_length = new int[1];

		DoubleToAscii(value, mode, 0, decimal_rep, kDecimalRepCapacity,
				sign, decimal_rep_length, decimal_point);

		boolean unique_zero = (flags_ & Flags.UNIQUE_ZERO) != 0;
		if (sign[0] && (value != 0.0 || !unique_zero)) {
			result_builder.append('-');
		}

		int exponent = decimal_point[0] - 1;
		if ((decimal_in_shortest_low_ <= exponent) &&
				(exponent < decimal_in_shortest_high_)) {
			CreateDecimalRepresentation(decimal_rep, decimal_rep_length[0],
					decimal_point[0],
					(std::max)(0, decimal_rep_length[0] - decimal_point[0]),
			result_builder);
		} else {
			CreateExponentialRepresentation(decimal_rep, decimal_rep_length[0], exponent,
					result_builder);
		}
		return true;
	}


	/**
	 *  Computes a decimal representation with a fixed number of digits after the
	 *  decimal point. The last emitted digit is rounded.
	 *
	 *  Examples:
	 *    ToFixed(3.12, 1) -> "3.1"
	 *    ToFixed(3.1415, 3) -> "3.142"
	 *    ToFixed(1234.56789, 4) -> "1234.5679"
	 *    ToFixed(1.23, 5) -> "1.23000"
	 *    ToFixed(0.1, 4) -> "0.1000"
	 *    ToFixed(1e30, 2) -> "1000000000000000019884624838656.00"
	 *    ToFixed(0.1, 30) -> "0.100000000000000005551115123126"
	 *    ToFixed(0.1, 17) -> "0.10000000000000001"
	 *
	 *  If requested_digits equals 0, then the tail of the result depends on
	 *  the EMIT_TRAILING_DECIMAL_POINT and EMIT_TRAILING_ZERO_AFTER_POINT.
	 *  Examples, for requested_digits == 0,
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
	 *    - 'requested_digits' > kMaxFixedDigitsAfterPoint.
	 *  The last two conditions imply that the result for non-special values never
	 *  contains more than
	 *   1 + kMaxFixedDigitsBeforePoint + 1 + kMaxFixedDigitsAfterPoint characters
	 *  (one additional character for the sign, and one for the decimal point).
	 *  In addition, the buffer must be able to hold the trailing '\0' character.
	 */
	boolean ToFixed(double value,
				 int requested_digits,
				 StringBuilder result_builder) {
		DOUBLE_CONVERSION_ASSERT(kMaxFixedDigitsBeforePoint == 60);
  		final double kFirstNonFixed = 1e60;

		if (new Ieee.Double(value).IsSpecial()) {
			return HandleSpecialValues(value, result_builder);
		}

		if (requested_digits > kMaxFixedDigitsAfterPoint) return false;
		if (value >= kFirstNonFixed || value <= -kFirstNonFixed) return false;

		// Find a sufficiently precise decimal representation of n.
		int[] decimal_point = new int[1];
		boolean[] sign = new boolean[1];
		// Add space for the '\0' byte.
  		final int kDecimalRepCapacity =
				kMaxFixedDigitsBeforePoint + kMaxFixedDigitsAfterPoint + 1;
		char[] decimal_rep = new char[kDecimalRepCapacity];
		int[] decimal_rep_length = new int[1];
		DoubleToAscii(value, DtoaMode.FIXED, requested_digits,
				decimal_rep, kDecimalRepCapacity,
				sign, decimal_rep_length, decimal_point);

		boolean unique_zero = ((flags_ & Flags.UNIQUE_ZERO) != 0);
		if (sign[0] && (value != 0.0 || !unique_zero)) {
			result_builder.append('-');
		}

		CreateDecimalRepresentation(decimal_rep, decimal_rep_length[0], decimal_point[0],
				requested_digits, result_builder);
		return true;
	}

	/**
	 *  Computes a representation in exponential format with requested_digits
	 *  after the decimal point. The last emitted digit is rounded.
	 *  If requested_digits equals -1, then the shortest exponential representation
	 *  is computed.
	 *
	 *  Examples with EMIT_POSITIVE_EXPONENT_SIGN deactivated, and
	 *  exponent_character set to 'e'.
	 *  ToExponential(3.12, 1) -> "3.1e0"
	 *  ToExponential(5.0, 3) -> "5.000e0"
	 *  ToExponential(0.001, 2) -> "1.00e-3"
	 *  ToExponential(3.1415, -1) -> "3.1415e0"
	 *  ToExponential(3.1415, 4) -> "3.1415e0"
	 *  ToExponential(3.1415, 3) -> "3.142e0"
	 *  ToExponential(123456789000000, 3) -> "1.235e14"
	 *  ToExponential(1000000000000000019884624838656.0, -1) -> "1e30"
	 *  ToExponential(1000000000000000019884624838656.0, 32) ->
	 *  "1.00000000000000001988462483865600e30"
	 *  ToExponential(1234, 0) -> "1e3"
	 *
	 *  Returns true if the conversion succeeds. The conversion always succeeds
	 *  except for the following cases:
	 *  - the input value is special and no infinity_symbol or nan_symbol has
	 *  been provided to the constructor,
	 *  - 'requested_digits' > kMaxExponentialDigits.
	 *  The last condition implies that the result will never contains more than
	 *  kMaxExponentialDigits + 8 characters (the sign, the digit before the
	 *  decimal point, the decimal point, the exponent character, the
	 *  exponent's sign, and at most 3 exponent digits).
	 */
	boolean ToExponential(double value,
					   int requested_digits,
					   StringBuilder result_builder) {
		if (new Ieee.Double(value).IsSpecial()) {
			return HandleSpecialValues(value, result_builder);
		}

		if (requested_digits < -1) return false;
		if (requested_digits > kMaxExponentialDigits) return false;

		int[] decimal_point = new int[1];
		boolean[] sign = new boolean[1];
		// Add space for digit before the decimal point and the '\0' character.
  		final int kDecimalRepCapacity = kMaxExponentialDigits + 2;
		DOUBLE_CONVERSION_ASSERT(kDecimalRepCapacity > kBase10MaximalLength);
		char[] decimal_rep = new char[kDecimalRepCapacity];
//#ifndef NDEBUG
//		// Problem: there is an assert in StringBuilder::AddSubstring() that
//		// will pass this buffer to strlen(), and this buffer is not generally
//		// null-terminated.
//		memset(decimal_rep, 0, sizeof(decimal_rep));
//#endif
		int[] decimal_rep_length = new int[1];

		if (requested_digits == -1) {
			DoubleToAscii(value, DtoaMode.SHORTEST, 0,
					decimal_rep, kDecimalRepCapacity,
					sign, decimal_rep_length, decimal_point);
		} else {
			DoubleToAscii(value, DtoaMode.PRECISION, requested_digits + 1,
					decimal_rep, kDecimalRepCapacity,
					sign, decimal_rep_length, decimal_point);
			DOUBLE_CONVERSION_ASSERT(decimal_rep_length[0] <= requested_digits + 1);

			for (int i = decimal_rep_length[0]; i < requested_digits + 1; ++i) {
				decimal_rep[i] = '0';
			}
			decimal_rep_length[0] = requested_digits + 1;
		}

		boolean unique_zero = ((flags_ & Flags.UNIQUE_ZERO) != 0);
		if (sign[0] && (value != 0.0 || !unique_zero)) {
			result_builder.append('-');
		}

		int exponent = decimal_point[0] - 1;
		CreateExponentialRepresentation(decimal_rep,
				decimal_rep_length[0],
				exponent,
				result_builder);
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
	 *    ToPrecision(0.0000012345, 2) -> "0.0000012"
	 *    ToPrecision(0.00000012345, 2) -> "1.2e-7"
	 *  Similarily the converter may add up to
	 *  max_trailing_padding_zeroes_in_precision_mode in precision mode to avoid
	 *  returning an exponential representation. A zero added by the
	 *  EMIT_TRAILING_ZERO_AFTER_POINT flag is counted for this limit.
	 *  Examples for max_trailing_padding_zeroes_in_precision_mode = 1:
	 *    ToPrecision(230.0, 2) -> "230"
	 *    ToPrecision(230.0, 2) -> "230."  with EMIT_TRAILING_DECIMAL_POINT.
	 *    ToPrecision(230.0, 2) -> "2.3e2" with EMIT_TRAILING_ZERO_AFTER_POINT.
	 *  Examples for max_trailing_padding_zeroes_in_precision_mode = 3, and no
	 * 	EMIT_TRAILING_ZERO_AFTER_POINT:
	 *    ToPrecision(123450.0, 6) -> "123450"
	 *    ToPrecision(123450.0, 5) -> "123450"
	 *    ToPrecision(123450.0, 4) -> "123500"
	 *    ToPrecision(123450.0, 3) -> "123000"
	 *    ToPrecision(123450.0, 2) -> "1.2e5"
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
	boolean ToPrecision(double value,
					 int precision,
					 StringBuilder result_builder) {
		if (new Ieee.Double(value).IsSpecial()) {
			return HandleSpecialValues(value, result_builder);
		}

		if (precision < kMinPrecisionDigits || precision > kMaxPrecisionDigits) {
			return false;
		}

		// Find a sufficiently precise decimal representation of n.
		int decimal_point;
		boolean sign;
		// Add one for the terminating null character.
  		final int kDecimalRepCapacity = kMaxPrecisionDigits + 1;
		char[] decimal_rep = new char[kDecimalRepCapacity];
		int decimal_rep_length;

		{
			int[] inDecimalPoint = new int[1];
			boolean[] inSign = new boolean[1];
			int[] inDecimalRepLength = new int[1];
			DoubleToAscii(value, DtoaMode.PRECISION, precision,
					decimal_rep, kDecimalRepCapacity,
					inSign, inDecimalRepLength, inDecimalPoint);
			DOUBLE_CONVERSION_ASSERT(inDecimalRepLength[0] <= precision);
			decimal_point = inDecimalPoint[0];
			sign = inSign[0];
			decimal_rep_length = inDecimalRepLength[0];
		}

		boolean unique_zero = ((flags_ & Flags.UNIQUE_ZERO) != 0);
		if (sign && (value != 0.0 || !unique_zero)) {
			result_builder.append('-');
		}

		// The exponent if we print the number as x.xxeyyy. That is with the
		// decimal point after the first digit.
		int exponent = decimal_point - 1;

		int extra_zero = ((flags_ & Flags.EMIT_TRAILING_ZERO_AFTER_POINT) != 0) ? 1 : 0;
		boolean as_exponential =
				(-decimal_point + 1 > max_leading_padding_zeroes_in_precision_mode_) ||
						(decimal_point - precision + extra_zero >
								max_trailing_padding_zeroes_in_precision_mode_);
		if ((flags_ & Flags.NO_TRAILING_ZERO) != 0) {
			// Truncate trailing zeros that occur after the decimal point (if exponential,
			// that is everything after the first digit).
			int stop = as_exponential ? 1 : std::max(1, decimal_point);
			while (decimal_rep_length > stop && decimal_rep[decimal_rep_length - 1] == '0') {
				--decimal_rep_length;
			}
			// Clamp precision to avoid the code below re-adding the zeros.
			precision = std::min(precision, decimal_rep_length);
		}
		if (as_exponential) {
			// Fill buffer to contain 'precision' digits.
			// Usually the buffer is already at the correct length, but 'DoubleToAscii'
			// is allowed to return less characters.
			for (int i = decimal_rep_length; i < precision; ++i) {
				decimal_rep[i] = '0';
			}

			CreateExponentialRepresentation(decimal_rep,
					precision,
					exponent,
					result_builder);
		} else {
			CreateDecimalRepresentation(decimal_rep, decimal_rep_length, decimal_point,
					(std::max)(0, precision - decimal_point),
			result_builder);
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
	};

	private static BigNumDtoa.BignumDtoaMode DtoaToBignumDtoaMode(
			DoubleToStringConverter.DtoaMode dtoa_mode) {
		switch (dtoa_mode) {
			case SHORTEST:  return BigNumDtoa.BignumDtoaMode.BIGNUM_DTOA_SHORTEST;
			case SHORTEST_SINGLE:
				return BigNumDtoa.BignumDtoaMode.BIGNUM_DTOA_SHORTEST_SINGLE;
			case FIXED:     return BigNumDtoa.BignumDtoaMode.BIGNUM_DTOA_FIXED;
			case PRECISION: return BigNumDtoa.BignumDtoaMode.BIGNUM_DTOA_PRECISION;
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
	 * 	   In this mode the 'requested_digits' parameter is ignored.
	 * 	  - SHORTEST_SINGLE: same as SHORTEST but with single-precision.
	 * 	  - FIXED: produces digits necessary to print a given number with
	 * 	   'requested_digits' digits after the decimal outPoint. The produced digits
	 * 	   might be too short in which case the caller has to fill the remainder
	 * 	   with '0's.
	 * 	   Example: ToFixed(0.001, 5) is allowed to return outBuffer="1", outPoint=-2.
	 * 	   Halfway cases are rounded towards +/-Infinity (away from 0). The call
	 * 	   ToFixed(0.15, 2) thus returns outBuffer="2", outPoint=0.
	 * 	   The returned outBuffer may contain digits that would be truncated from the
	 * 	   shortest representation of the input.
	 * 	  - PRECISION: produces 'requested_digits' where the first digit is not '0'.
	 * 	   Even though the outLength of produced digits usually equals
	 * 	   'requested_digits', the function is allowed to return fewer digits, in
	 * 	   which case the caller has to fill the missing digits with '0's.
	 * 	   Halfway cases are again rounded away from 0.
	 * 	 DoubleToAscii expects the given outBuffer to be big enough to hold all
	 * 	 digits and a terminating null-character. In SHORTEST-mode it expects a
	 * 	 outBuffer of at least kBase10MaximalLength + 1. In all other modes the
	 * 	 requested_digits parameter and the padding-zeroes limit the size of the
	 * 	 output. Don't forget the decimal outPoint, the exponent character and the
	 * 	 terminating null-character when computing the maximal output size.
	 * 	 The given outLength is only used in debug mode to ensure the outBuffer is big
	 * 	 enough.
	 *
	 */
	public static void DoubleToAscii(double v,
							  DtoaMode mode,
							  int requested_digits,
							  char[] vector,
							  int buffer_length,
							  boolean[] sign,
							  int[] length,
							  int[] point) {
		DOUBLE_CONVERSION_ASSERT(!new Ieee.Double(v).IsSpecial());
		DOUBLE_CONVERSION_ASSERT(mode == DtoaMode.SHORTEST || mode == DtoaMode.SHORTEST_SINGLE || requested_digits >= 0);

		if (new Ieee.Double(v).Sign() < 0) {
			sign[0] = true;
			v = -v;
		} else {
    		sign[0] = false;
		}

		if (mode == DtoaMode.PRECISION && requested_digits == 0) {
			vector[0] = '\0';
			length[0] = 0;
			return;
		}

		if (v == 0) {
			vector[0] = '0';
			vector[1] = '\0';
			length[0] = 1;
			point[0] = 1;
			return;
		}

		boolean fast_worked;
		switch (mode) {
			case SHORTEST:
				fast_worked = FastDtoa.FastDtoa(v, FastDtoa.FastDtoaMode.FAST_DTOA_SHORTEST, 0, vector, length, point);
				break;
			case SHORTEST_SINGLE:
				fast_worked = FastDtoa.FastDtoa(v, FastDtoa.FastDtoaMode.FAST_DTOA_SHORTEST_SINGLE, 0,
						vector, length, point);
				break;
			case FIXED:
				fast_worked = FastFixedDtoa(v, requested_digits, vector, length, point);
				break;
			case PRECISION:
				fast_worked = FastDtoa.FastDtoa(v, FastDtoa.FastDtoaMode.FAST_DTOA_PRECISION, requested_digits,
						vector, length, point);
				break;
			default:
				fast_worked = false;
				throw new IllegalStateException("Unreachable");
		}
		if (fast_worked) return;

		// If the fast dtoa didn't succeed use the slower bignum version.
		BigNumDtoa.BignumDtoaMode bignum_mode = DtoaToBignumDtoaMode(mode);
		BigNumDtoa.BignumDtoa(v, bignum_mode, requested_digits, vector, length, point);
		vector[length[0]] = '\0';
	}

	/**
	 * 	Add character padding to the builder. If count is non-positive,
	 * 	nothing is added to the builder.
	 */
	private static void addPadding(StringBuilder sb, char c, int count) {
		for (int i=count; i > 0; i--) {
			sb.append(c);
		}
	}
}
