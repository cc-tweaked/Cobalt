package org.squiddev.cobalt.lib.fmt;

/**
 * A wrapper to long to help safely keep an unsigned value
 */
public class UnsignedLong implements Comparable<UnsignedLong> {
	public static final UnsignedLong ZERO = new UnsignedLong(0);
	public static final UnsignedLong ONE = new UnsignedLong(1);

	private static final long UNSIGNED_MASK = 0x7fff_ffff_ffff_ffffL;
	private static final long MASK_32 = 0xffff_ffffL;
	private static final long UPPER_MASK = 0xffff_ffff_0000_0000L;

	private final long val;


	// requires overloading to prevent accidental sign extending

	/** New UnsignedLong from a naked unsigned integer */
	public static UnsignedLong uValueOf(short val) {
		return new UnsignedLong(Short.toUnsignedLong(val));
	}

	/** New UnsignedLong from a naked unsigned integer */
	public static UnsignedLong uValueOf(int val) {
		return new UnsignedLong(Integer.toUnsignedLong(val));
	}

	/** New UnsignedLong from a naked unsigned integer */
	public static UnsignedLong uValueOf(long val) {
		return new UnsignedLong(val);
	}

	/**
	 * Create a new <code>UnsignedLong</code> with a given value.  This value is treated as a signed value.
	 */
	public static UnsignedLong valueOf(long val) {
		return new UnsignedLong(val);
	}

	public static UnsignedLong valueOf(String str) {
		return new UnsignedLong(Long.parseUnsignedLong(str));
	}

	public static UnsignedLong valueOf(String str, int radix) {
		return new UnsignedLong(Long.parseUnsignedLong(str, radix));
	}

	private UnsignedLong(long val) {
		this.val = val;
	}

	public String toString() {
		return Long.toUnsignedString(val);
	}

	public String toString(int radix) {
		return Long.toUnsignedString(val, radix);
	}

	@Override
	public int compareTo(UnsignedLong o) {
		return compare(this, o);
	}

	public static int compare(UnsignedLong x, UnsignedLong y) {
		return Long.compareUnsigned(x.val, y.val);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(val);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof UnsignedLong) {
			return val == ((UnsignedLong)o).val;
		}
		return false;
	}

	public boolean isZero() {
		return val == 0;
	}

	public boolean isEven() {
		return (val & 1) == 0;
	}


	// requires overloading to prevent accidental sign extending

	public boolean eq(short shortValue) {
		if (shortValue < 0) return false;
		return val == shortValue;
	}
	public boolean eq(int intValue) {
		if (intValue < 0) return false;
		return val == intValue;
	}
	public boolean eq(long longValue) {
		if (longValue < 0) return false;
		return val == longValue;
	}

	public boolean eq(UnsignedLong other) {
		return val == other.val;
	}

	/**
	 * {@code this} >= {@code ul}.
	 * convenience method equivalent to {@code this.compareTo(ul) >= 0}
	 */
	public boolean ge(UnsignedLong ul) {
		return this.compareTo(ul) >= 0;
	}

	/**
	 * {@code this} > {@code ul}.
	 * convenience method equivalent to {@code this.compareTo(ul) > 0}
	 */
	public boolean gt(UnsignedLong ul) {
		return this.compareTo(ul) > 0;
	}

	/**
	 * {@code this} <= {@code ul}.
	 * convenience method equivalent to {@code this.compareTo(ul) <= 0}
	 */
	public boolean le(UnsignedLong ul) {
		return this.compareTo(ul) <= 0;
	}

	/**
	 * {@code this} < {@code ul}.
	 * convenience method equivalent to {@code this.compareTo(ul) < 0}
	 */
	public boolean lt(UnsignedLong ul) {
		return this.compareTo(ul) < 0;
	}

	/**
	 * @return true if value can fit in an {@link UnsignedInt}
	 */
	public boolean isUIntValue() {
		return (val & UPPER_MASK) == 0;
	}

	/**
	 * Use this if your code can guarantee the value won't be truncated
	 */
	public UnsignedInt unsafeUIntValue() {
		return UnsignedInt.uValueOf((int)val);
	}

	/**
	 * Get the unsigned value, with a check to prevent truncation
	 * @throws ArithmeticException if value could not fit in an {@link UnsignedInt} value
	 */
	public UnsignedInt uIntValueExact() {
		if ((val & UPPER_MASK) != 0) throw new ArithmeticException("Value too large");
		return UnsignedInt.valueOf((int)val);
	}

	/**
	 * Get the unsigned value, with a check to prevent truncation
	 * @throws ArithmeticException if value could not fit in an {@link UnsignedInt} value
	 */
	public short shortValueExact() {
		if (val > Short.MAX_VALUE) throw new ArithmeticException("Value too large");
		return (short) val;
	}

	public int intValueExact() {
		if (val > Integer.MAX_VALUE) throw new ArithmeticException("Value too large");
		return (int) val;
	}

	public int unsafeIntValue() {
		return (int)val;
	}

	public long unsafeLongValue() {
		return val;
	}

	public long longValueExact() {
		if (val < 0) throw new ArithmeticException("Refusing to return with sign bit set");
		return val;
	}

	public float floatValue() {
		float f = (float) (val & UNSIGNED_MASK);
		if (val < 0) f += 0x1.0p63f;
		return f;
	}

	public double doubleValue() {
		double f = (double) (val & UNSIGNED_MASK);
		if (val < 0) f += 0x1.0p63d;
		return f;
	}

	// requires overloading to prevent accidental sign extending
	/** returns this & (unsigned)rVal */
	public UnsignedLong bitAndU(short rVal) { return new UnsignedLong(val & Short.toUnsignedLong(rVal)); }
	/** returns this & (unsigned)rVal */
	public UnsignedLong bitAndU(int rVal) { return new UnsignedLong(val & Integer.toUnsignedLong(rVal)); }
	/** returns this & (unsigned)rVal */
	public UnsignedLong bitAndU(long rVal) { return new UnsignedLong(val & rVal); }

	public UnsignedLong bitAnd(UnsignedLong rVal) {
		return new UnsignedLong(val & rVal.val);
	}

	public UnsignedLong bitOr(UnsignedLong rVal) {
		return new UnsignedLong(val | rVal.val);
	}

	public UnsignedLong shl(int rVal) {
		return new UnsignedLong(val << rVal);
	}

	/**
	 * {@code this} >> {@code rval}.
	 *  Bitwise shift right for this unsigned value.
	 *  Equivalent to {@code long >>> rval} for a raw {@code long}
	 */
	public UnsignedLong shr(int rVal) {
		return new UnsignedLong(val >>> rVal);
	}


	public UnsignedLong plus(UnsignedLong rVal) {
		return new UnsignedLong(val + rVal.val);
	}

	public UnsignedLong minus(UnsignedLong rVal) {
		return new UnsignedLong(val - rVal.val);
	}

	public UnsignedLong divideBy(long divisor) {
		if (divisor < 0) throw new IllegalArgumentException("divisor must be positive");
		return new UnsignedLong(
				Long.divideUnsigned(val, divisor));
	}

	public UnsignedLong divideBy(UnsignedLong divisor) {
		return new UnsignedLong(
				Long.divideUnsigned(val, divisor.val));
	}

	public UnsignedLong mod(long divisor) {
		if (divisor < 0) throw new IllegalArgumentException("divisor must be positive");
		return new UnsignedLong(
				Long.remainderUnsigned(val, divisor));
	}

	public UnsignedLong mod(UnsignedLong rVal) {
		return new UnsignedLong(
				Long.remainderUnsigned(val, rVal.val));
	}

	public int mod10() {
		return (int)Long.remainderUnsigned(val, 10);
	}

	/**
	 * return this % 10^7 (10,000,000)
	 */
	public UnsignedInt modTen7() {
		return UnsignedInt.uValueOf((int)Long.remainderUnsigned(val, 10_000_000));
	}


	public UnsignedLong times(long rVal) {
		return new UnsignedLong(val * rVal);
	}

	public UnsignedLong times(UnsignedLong multiplier) {
		return new UnsignedLong(val * multiplier.val);
	}

}
