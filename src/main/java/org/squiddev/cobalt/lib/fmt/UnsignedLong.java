package org.squiddev.cobalt.lib.fmt;

/**
 * A wrapper to long to help safely keep an unsigned value
 */
public class UnsignedLong extends Number implements Comparable<UnsignedLong> {
	private static final long UNSIGNED_MASK = 0x7fff_ffff_ffff_ffffL;
	private static final long MASK_32 = 0xffff_ffffL;

	private final long val;

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
	 * Act like a c cast to a unsigned 32-bit number, which
	 * 	truncates the value to the lowest 32 bits.
	 */
	public long unsignedIntValue() {
		return val & MASK_32;
	}

	@Override
	public int intValue() {
		return (int) val;
	}

	@Override
	public long longValue() {
		return val;
	}

	@Override
	public float floatValue() {
		float f = (float) (val & UNSIGNED_MASK);
		if (val < 0) f += 0x1.0p63f;
		return f;
	}

	@Override
	public double doubleValue() {
		double f = (double) (val & UNSIGNED_MASK);
		if (val < 0) f += 0x1.0p63d;
		return f;
	}

	public UnsignedLong bitAnd(long rVal) {
		return new UnsignedLong(val & rVal);
	}

	public UnsignedLong bitAnd(UnsignedLong rVal) {
		return new UnsignedLong(val & rVal.val);
	}

	public UnsignedLong bitOr(long rVal) {
		return new UnsignedLong(val | rVal);
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

	public UnsignedLong plus(long rVal) {
		return new UnsignedLong(val + rVal);
	}
	public UnsignedLong plus(UnsignedLong rVal) {
		return new UnsignedLong(val + rVal.val);
	}

	public UnsignedLong minus(long rVal) {
		return new UnsignedLong(val - rVal);
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

	public UnsignedLong times(long rVal) {
		return new UnsignedLong(val * rVal);
	}

	public UnsignedLong times(UnsignedLong rVal) {
		return new UnsignedLong(
				Long.divideUnsigned(val, rVal.val));
	}

}
