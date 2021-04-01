package org.squiddev.cobalt.lib.doubles;

import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.SignednessGlb;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.checkerframework.common.value.qual.IntRange;

public class UnsignedInt implements Comparable<UnsignedInt> {
	public static final UnsignedInt ZERO = new UnsignedInt(0);
	public static final UnsignedInt ONE = new UnsignedInt(1);

	private static final @Unsigned long INT_MASK = 0xffff_ffffL;
	private static final @Unsigned long NOT_INT_MASK = ~INT_MASK;
	private static final @Unsigned int INT_SIGN_BIT = 0x8000_0000;
	private static final @Unsigned int INT_UNSIGNED_MASK = ~INT_SIGN_BIT;
	private static final @Unsigned long LONG_SIGNED_MASK = 0x8000_0000_0000_0000L;
	private static final @Unsigned int SHORT_MASK = 0xffff;
	private static final @Unsigned int NOT_SHORT_MASK = ~SHORT_MASK;

	private final @Unsigned int val;

	// requires overloading to prevent accidental sign extending
	/** New UnsignedLong from a naked unsigned integer */
	@SuppressWarnings("argument.type.incompatible")
	public static UnsignedInt uValueOf(@Unsigned short value) {
		return new UnsignedInt(Short.toUnsignedInt(value));
	}
	/** New UnsignedLong from a naked unsigned integer */
	public static UnsignedInt uValueOf(@Unsigned int value) {
		return new UnsignedInt(value);
	}

	/**
	 * Create a new <code>UnsignedInt</code> with a given value.  This value is treated as a signed value.
	 */
	public static UnsignedInt valueOf(@SignednessGlb int value) {
		return new UnsignedInt(value);
	}

	public static UnsignedInt valueOf(String str) {
		return new UnsignedInt(Integer.parseUnsignedInt(str));
	}

	public static UnsignedInt valueOf(String str, @IntRange(from=2,to=36) int radix) {
		return new UnsignedInt(Integer.parseUnsignedInt(str, radix));
	}

	private UnsignedInt(@Unsigned int value) {
		this.val = value;
	}

	public String toString() {
		return Integer.toUnsignedString(val);
	}

	public String toString(@IntRange(from=2, to=36) int radix) {
		return Integer.toUnsignedString(val, radix);
	}

	@Override
	public int compareTo(UnsignedInt o) {
		return compare(this, o);
	}

	public static int compare(UnsignedInt x, UnsignedInt y) {
		return Integer.compareUnsigned(x.val, y.val);
	}

	@Override
	@SuppressWarnings("argument.type.incompatible")
	public int hashCode() {
		return Integer.hashCode(val);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof UnsignedInt) {
			return val == ((UnsignedInt)o).val;
		}
		return false;
	}

	public boolean isZero() {
		return val == 0;
	}

	public boolean eq(int intValue) {
		if (intValue < 0) return false;
		return val == intValue;
	}

	public boolean eq(UnsignedInt other) {
		return val == other.val;
	}

	/**
	 * {@code this} >= {@code ui}.
	 * convenience method equivalent to {@code this.compareTo(ui) >= 0}
	 */
	public boolean ge(UnsignedInt ui) {
		return this.compareTo(ui) >= 0;
	}

	/**
	 * {@code this} > {@code ui}.
	 * convenience method equivalent to {@code this.compareTo(ui) > 0}
	 */
	public boolean gt(UnsignedInt ui) {
		return this.compareTo(ui) > 0;
	}

	/**
	 * {@code this} <= {@code ui}.
	 * convenience method equivalent to {@code this.compareTo(ui) <= 0}
	 */
	public boolean le(UnsignedInt ui) {
		return this.compareTo(ui) <= 0;
	}

	/**
	 * {@code this} < {@code ui}.
	 * convenience method equivalent to {@code this.compareTo(ui) < 0}
	 */
	public boolean lt(UnsignedInt ui) {
		return this.compareTo(ui) < 0;
	}

	public @Unsigned short shortValueExact() {
		if ((val & NOT_SHORT_MASK) != 0) throw new ArithmeticException("Value too large");
		return (@Unsigned short) val;
	}

	public @Unsigned int intValueExact() {
		return val;
	}

	@SuppressWarnings("cast.unsafe")
	public @SignednessGlb int unsafeIntValue() {
		return (@SignednessGlb int) val;
	}

	public UnsignedLong uLongValue() {
		return UnsignedLong.uValueOf(val);
	}

	public @Signed long longValueExact() {
		if ((val & INT_SIGN_BIT) != 0) throw new ArithmeticException("Refusing to return with sign bit set");
		return (@Signed long) val;
	}

	public float floatValue() {
		float f = (float) (val & INT_UNSIGNED_MASK);
		if ((val & INT_SIGN_BIT) != 0) f += 0x1.0p31f;
		return f;
	}

	public double doubleValue() {
		double f = (double) (val & INT_UNSIGNED_MASK);
		if ((val & INT_SIGN_BIT) != 0) f += 0x1.0p31d;
		return f;
	}

	// requires overloading to prevent accidental sign extending
	/** returns this & (unsigned)rVal */
	public UnsignedInt bitAndU(short rVal) {
		return new UnsignedInt(val & Short.toUnsignedInt(rVal));
	}
	/** returns this & (unsigned)rVal */
	public UnsignedInt bitAndU(@Unsigned int rVal) {
		return new UnsignedInt(val & rVal);
	}

	public UnsignedInt bitAnd(UnsignedInt rVal) {
		return new UnsignedInt(val & rVal.val);
	}

	public UnsignedInt bitOr(UnsignedInt rVal) {
		return new UnsignedInt(val | rVal.val);
	}

	public UnsignedInt shl(int rVal) {
		return new UnsignedInt(val << rVal);
	}

	/**
	 * {@code this} >> {@code rval}.
	 *  Bitwise shift right for this unsigned value.
	 *  Equivalent to {@code long >>> rval} for a raw {@code long}
	 */
	public UnsignedInt shr(int rVal) {
		return new UnsignedInt(val >>> rVal);
	}

	public UnsignedInt plus(UnsignedInt rVal) {
		return new UnsignedInt(val + rVal.val);
	}

	// requires overloading to prevent accidental sign extending
	/** returns this + (unsigned)rVal */
	public UnsignedInt minusU(short rVal) {
		return new UnsignedInt(val - Short.toUnsignedInt(rVal));
	}
	/** returns this + (unsigned)rVal */
	public UnsignedInt minusU(@Unsigned int rVal) {
		return new UnsignedInt(val - rVal);
	}

	public UnsignedInt minus(UnsignedInt rVal) {
		return new UnsignedInt(val - rVal.val);
	}

	public UnsignedInt divideBy(int divisor) {
		if (divisor < 0) throw new IllegalArgumentException("divisor must be positive");
		return new UnsignedInt(
				Integer.divideUnsigned(val, divisor));
	}

	@SuppressWarnings("cast.unsafe")
	public UnsignedInt divideExact(long divisor) {
		if ((divisor & NOT_INT_MASK) != 0L) throw new ArithmeticException("divisor must fit in 32 bits");
		return new UnsignedInt(
				Integer.divideUnsigned(val, (@Unsigned int)divisor));
	}

	public UnsignedInt divideBy(UnsignedInt divisor) {
		return new UnsignedInt(
				Integer.divideUnsigned(val, divisor.val));
	}

	public UnsignedInt mod(int divisor) {
		if (divisor < 0) throw new IllegalArgumentException("divisor must be positive");
		return new UnsignedInt(
				Integer.remainderUnsigned(val, divisor));
	}

	public @Unsigned int mod10() {
		return Integer.remainderUnsigned(val, 10);
	}

	/**
	 * this % 10^7 (10,000,000)
	 *
	 */
	public UnsignedInt modTen7() {
		return UnsignedInt.uValueOf(Integer.remainderUnsigned(val, 10_000_000));
	}


	@SuppressWarnings("cast.unsafe")
	public UnsignedInt modExact(long divisor) {
		if ((divisor & NOT_INT_MASK) != 0L) throw new ArithmeticException("divisor must fit in 32 bits");
		return new UnsignedInt(
				Integer.remainderUnsigned(val, (@Unsigned int)divisor));
	}

	public UnsignedInt mod(UnsignedInt rVal) {
		return new UnsignedInt(
				Integer.remainderUnsigned(val, rVal.val));
	}

	@SuppressWarnings("cast.unsafe")
	public UnsignedInt timesExact(long multiplier) {
		if ((multiplier & NOT_INT_MASK) != 0L) throw new ArithmeticException("multiplier must fit in 32 bits");
		return new UnsignedInt(val * (@Unsigned int)multiplier);
	}

	public UnsignedInt times(@Unsigned int multiplier) {
		return new UnsignedInt(val * multiplier);
	}

	public UnsignedInt times(UnsignedInt multiplier) {
		return new UnsignedInt(val * multiplier.val);
	}

}
