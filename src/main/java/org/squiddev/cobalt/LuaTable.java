/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
package org.squiddev.cobalt;

import org.squiddev.cobalt.table.*;

import java.util.ArrayList;
import java.util.List;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.*;

/**
 * Subclass of {@link LuaValue} for representing lua tables.
 *
 * Almost all API's implemented in {@link LuaTable} are defined and documented in {@link LuaValue}.
 *
 * If a table is needed, the one of the type-checking functions can be used such as
 * {@link #isTable()},
 * {@link #checkTable()}, or
 * {@link #optTable(LuaTable)}
 *
 * The main table operations are defined on {@link LuaValue}
 * for getting and setting values with and without metatag processing:
 * <ul>
 * <li>{@link LuaValue#get(LuaState, LuaValue)}</li>
 * <li>{@link LuaValue#set(LuaState, LuaValue, LuaValue)}</li>
 * <li>{@link LuaValue#rawget(LuaValue)}</li>
 * <li>{@link LuaValue#rawset(LuaValue, LuaValue)}</li>
 * <li>plus overloads such as {@link LuaValue#get(LuaState, String)}, {@link LuaValue#get(LuaState, int)}, and so on</li>
 * </ul>
 *
 * To iterate over key-value pairs from Java, use
 * <pre> {@code
 * LuaValue k = LuaValue.NIL;
 * while ( true ) {
 *    Varargs n = table.next(k);
 *    if ( (k = n.arg1()).isnil() )
 *       break;
 *    LuaValue v = n.arg(2)
 *    process( k, v )
 * }}</pre>
 *
 * As with other types, {@link LuaTable} instances should be constructed via one of the table constructor
 * methods on {@link LuaValue}:
 * <ul>
 * <li>{@link ValueFactory#tableOf()} empty table</li>
 * <li>{@link ValueFactory#tableOf(int, int)} table with capacity</li>
 * <li>{@link ValueFactory#listOf(LuaValue[])} initialize array part</li>
 * <li>{@link ValueFactory#listOf(LuaValue[], Varargs)} initialize array part</li>
 * <li>{@link ValueFactory#tableOf(LuaValue[])} initialize named hash part</li>
 * <li>{@link ValueFactory#tableOf(Varargs, int)} initialize named hash part</li>
 * <li>{@link ValueFactory#tableOf(LuaValue[], LuaValue[])} initialize array and named parts</li>
 * <li>{@link ValueFactory#tableOf(LuaValue[], LuaValue[], Varargs)} initialize array and named parts</li>
 * </ul>
 *
 * @see LuaValue
 */
public final class LuaTable extends LuaValue implements Metatable {
	private static final int MIN_HASH_CAPACITY = 2;
	private static final LuaString N = valueOf("n");

	/**
	 * the array values
	 */
	protected LuaValue[] array;

	/**
	 * the hash part
	 */
	protected Slot[] hash;

	/**
	 * the number of hash entries
	 */
	protected int hashEntries;

	/**
	 * metatable for this table, or null
	 */
	protected Metatable metatable;

	/**
	 * Construct empty table
	 */
	public LuaTable() {
		super(TTABLE);
		array = NOVALS;
		hash = Slot.NOBUCKETS;
	}

	/**
	 * Construct table with preset capacity.
	 *
	 * @param narray capacity of array part
	 * @param nhash  capacity of hash part
	 */
	public LuaTable(int narray, int nhash) {
		super(TTABLE);
		presize(narray, nhash);
	}

	/**
	 * Construct table with named and unnamed parts.
	 *
	 * @param named   Named elements in order {@code key-a, value-a, key-b, value-b, ... }
	 * @param unnamed Unnamed elements in order {@code value-1, value-2, ... }
	 * @param lastarg Additional unnamed values beyond {@code unnamed.length}
	 */
	public LuaTable(LuaValue[] named, LuaValue[] unnamed, Varargs lastarg) {
		super(TTABLE);
		int nn = (named != null ? named.length : 0);
		int nu = (unnamed != null ? unnamed.length : 0);
		int nl = (lastarg != null ? lastarg.count() : 0);
		presize(nu + nl, nn >> 1);
		for (int i = 0; i < nu; i++) {
			rawset(i + 1, unnamed[i]);
		}
		if (lastarg != null) {
			for (int i = 1, n = lastarg.count(); i <= n; ++i) {
				rawset(nu + i, lastarg.arg(i));
			}
		}
		for (int i = 0; i < nn; i += 2) {
			if (!named[i + 1].isNil()) {
				rawset(named[i], named[i + 1]);
			}
		}
	}

	/**
	 * Construct table of unnamed elements.
	 *
	 * @param varargs Unnamed elements in order {@code value-1, value-2, ... }
	 */
	public LuaTable(Varargs varargs) {
		this(varargs, 1);
	}

	/**
	 * Construct table of unnamed elements.
	 *
	 * @param varargs  Unnamed elements in order {@code value-1, value-2, ... }
	 * @param firstarg the index in varargs of the first argument to include in the table
	 */
	public LuaTable(Varargs varargs, int firstarg) {
		super(TTABLE);
		int nskip = firstarg - 1;
		int n = Math.max(varargs.count() - nskip, 0);
		presize(n, 1);
		rawset(N, valueOf(n));
		for (int i = 1; i <= n; i++) {
			rawset(i, varargs.arg(i + nskip));
		}
	}

	@Override
	public LuaTable checkTable() {
		return this;
	}

	@Override
	public LuaTable optTable(LuaTable defval) {
		return this;
	}

	/**
	 * Preallocate the array part of a table to be a certain size,
	 *
	 * Primarily used internally in response to a SETLIST bytecode.
	 *
	 * @param narray the number of array slots to preallocate in the table.
	 * @throws LuaError if this is not a table.
	 */
	public void presize(int narray) {
		if (narray > array.length) {
			array = resize(array, 1 << log2(narray));
		}
	}

	public void presize(int narray, int nhash) {
		if (nhash > 0 && nhash < MIN_HASH_CAPACITY) {
			nhash = MIN_HASH_CAPACITY;
		}
		// Size of both parts must be a power of two.
		array = (narray > 0 ? new LuaValue[1 << log2(narray)] : NOVALS);
		hash = (nhash > 0 ? new Slot[1 << log2(nhash)] : Slot.NOBUCKETS);
		hashEntries = 0;
	}

	/**
	 * Resize the table
	 */
	private static LuaValue[] resize(LuaValue[] old, int n) {
		LuaValue[] v = new LuaValue[n];
		System.arraycopy(old, 0, v, 0, old.length);
		return v;
	}

	/**
	 * Get the length of the array part of the table.
	 *
	 * @return length of the array part, does not relate to count of objects in the table.
	 */
	public int getArrayLength() {
		return array.length;
	}

	/**
	 * Get the length of the hash part of the table.
	 *
	 * @return length of the hash part, does not relate to count of objects in the table.
	 */
	public int getHashLength() {
		return hash.length;
	}

	@Override
	public LuaValue getMetatable(LuaState state) {
		return metatable != null ? metatable.toLuaValue() : null;
	}

	public LuaValue setMetatable(LuaValue metatable) {
		useMetatable(metatableOf(metatable));
		return this;
	}

	public void useMetatable(Metatable metatable) {
		boolean hadWeakKeys = this.metatable != null && this.metatable.useWeakKeys();
		boolean hadWeakValues = this.metatable != null && this.metatable.useWeakValues();
		this.metatable = metatable;
		if ((hadWeakKeys != (metatable != null && metatable.useWeakKeys())) ||
			(hadWeakValues != (metatable != null && metatable.useWeakValues()))) {
			// force a rehash
			rehash(0);
		}
	}

	@Override
	public LuaValue setMetatable(LuaState state, LuaValue metatable) {
		return setMetatable(metatable);
	}

	@Override
	public LuaValue rawget(int key) {
		if (key > 0 && key <= array.length) {
			LuaValue v = metatable == null ? array[key - 1] : metatable.arrayGet(array, key - 1);
			return v != null ? v : NIL;
		}
		return hashget(LuaInteger.valueOf(key));
	}

	@Override
	public LuaValue rawget(LuaValue key) {
		if (key.isIntExact()) {
			int ikey = key.toInteger();
			if (ikey > 0 && ikey <= array.length) {
				LuaValue v = metatable == null
					? array[ikey - 1] : metatable.arrayGet(array, ikey - 1);
				return v != null ? v : NIL;
			}
		}
		return hashget(key);
	}

	protected LuaValue hashget(LuaValue key) {
		if (hashEntries > 0) {
			for (Slot slot = hash[hashSlot(key)]; slot != null; slot = slot.rest()) {
				StrongSlot foundSlot;
				if ((foundSlot = slot.find(key)) != null) {
					return foundSlot.value();
				}
			}
		}
		return NIL;
	}

	@Override
	public void rawset(int key, LuaValue value) {
		if (!arrayset(key, value)) {
			hashset(LuaInteger.valueOf(key), value);
		}
	}

	/**
	 * caller must ensure key is not nil
	 */
	@Override
	public void rawset(LuaValue key, LuaValue value) {
		if (!key.isIntExact() || !arrayset(key.toInteger(), value)) {
			hashset(key, value);
		}
	}

	/**
	 * Set an array element
	 */
	private boolean arrayset(int key, LuaValue value) {
		if (key > 0 && key <= array.length) {
			array[key - 1] = value.isNil() ? null : (metatable != null ? metatable.wrap(value) : value);
			return true;
		}
		return false;
	}

	/**
	 * Remove the element at a position in a list-table
	 *
	 * @param pos the position to remove
	 * @return The removed item, or {@link Constants#NONE} if not removed
	 */
	public LuaValue remove(int pos) {
		int n = length();
		if (pos == 0) {
			pos = n;
		} else if (pos > n) {
			return NONE;
		}
		LuaValue v = rawget(pos);
		for (LuaValue r = v; !r.isNil(); ) {
			r = rawget(pos + 1);
			rawset(pos++, r);
		}
		return v.isNil() ? NONE : v;
	}

	/**
	 * Insert an element at a position in a list-table
	 *
	 * @param pos   the position to remove
	 * @param value The value to insert
	 */
	public void insert(int pos, LuaValue value) {
		if (pos == 0) {
			pos = length() + 1;
		}
		while (!value.isNil()) {
			LuaValue v = rawget(pos);
			rawset(pos++, value);
			value = v;
		}
	}

	/**
	 * Concatenate the contents of a table efficiently, using {@link Buffer}
	 *
	 * @param sep {@link LuaString} separater to apply between elements
	 * @param i   the first element index
	 * @param j   the last element index, inclusive
	 * @return {@link LuaString} value of the concatenation
	 */
	public LuaValue concat(LuaString sep, int i, int j) {
		Buffer sb = new Buffer();
		if (i <= j) {
			sb.append(rawget(i).checkLuaString());
			while (++i <= j) {
				sb.append(sep);
				sb.append(rawget(i).checkLuaString());
			}
		}
		return sb.tostring();
	}

	@Override
	public LuaValue getn() {
		for (int n = getArrayLength(); n > 0; --n) {
			if (!rawget(n).isNil()) {
				return LuaInteger.valueOf(n);
			}
		}
		return ZERO;
	}


	public int length() {
		int a = getArrayLength();
		int n = a + 1, m = 0;
		while (!rawget(n).isNil()) {
			m = n;
			n += a + getHashLength() + 1;
		}
		while (n > m + 1) {
			int k = (n + m) / 2;
			if (!rawget(k).isNil()) {
				m = k;
			} else {
				n = k;
			}
		}
		return m;
	}

	/**
	 * Return table.maxn() as defined by lua 5.0.
	 *
	 * Provided for compatibility, not a scalable operation.
	 *
	 * @return value for maxn
	 */
	public double maxn() {
		double n = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i] != null) {
				n = i + 1;
			}
		}
		for (Slot slot : hash) {
			while (slot != null) {
				StrongSlot first = slot.firstSlot();
				if (first != null) {
					LuaValue v = first.key();
					if (v != null && v.type() == Constants.TNUMBER) {
						double key = v.toDouble();
						if (key > n) {
							n = key;
						}
					}
				}
				slot = slot.rest();
			}
		}
		return n;
	}

	/**
	 * Find the next key,value pair if {@code this} is a table,
	 * return {@link Constants#NIL} if there are no more, or throw a {@link LuaError} if not a table.
	 *
	 * To iterate over all key-value pairs in a table you can use
	 * <pre> {@code
	 * LuaValue k = LuaValue.NIL;
	 * while ( true ) {
	 *    Varargs n = table.next(k);
	 *    if ( (k = n.arg1()).isnil() )
	 *       break;
	 *    LuaValue v = n.arg(2)
	 *    process( k, v )
	 * }}</pre>
	 *
	 * @param key {@link LuaInteger} value identifying a key to start from,
	 *            or {@link Constants#NIL} to start at the beginning
	 * @return {@link Varargs} containing {key,value} for the next entry,
	 * or {@link Constants#NIL} if there are no more.
	 * @throws LuaError if {@code this} is not a table, or the supplied key is invalid.
	 * @see LuaTable
	 * @see #inext(LuaValue)
	 * @see ValueFactory#valueOf(int)
	 * @see Varargs#first()
	 * @see Varargs#arg(int)
	 * @see #isNil()
	 */
	public Varargs next(LuaValue key) {
		int i = 0;
		do {
			// find current key index
			if (!key.isNil()) {
				if (key.isIntExact()) {
					i = key.toInteger();
					if (i > 0 && i <= array.length) {
						break;
					}
				}
				if (hash.length == 0) {
					throw new LuaError("invalid key to 'next'");
				}
				i = hashSlot(key);
				boolean found = false;
				for (Slot slot = hash[i]; slot != null; slot = slot.rest()) {
					if (found) {
						StrongSlot nextEntry = slot.firstSlot();
						if (nextEntry != null) {
							return nextEntry.toVarargs();
						}
					} else if (slot.keyeq(key)) {
						found = true;
					}
				}
				if (!found) {
					throw new LuaError("invalid key to 'next'");
				}
				i += 1 + array.length;
			}
		} while (false);

		// check array part
		for (; i < array.length; ++i) {
			if (array[i] != null) {
				LuaValue value = metatable == null ? array[i] : metatable.arrayGet(array, i);
				if (value != null) {
					return varargsOf(LuaInteger.valueOf(i + 1), value);
				}
			}
		}

		// check hash part
		for (i -= array.length; i < hash.length; ++i) {
			Slot slot = hash[i];
			while (slot != null) {
				StrongSlot first = slot.firstSlot();
				if (first != null) {
					return first.toVarargs();
				}
				slot = slot.rest();
			}
		}

		// nothing found, push nil, return nil.
		return NIL;
	}

	/**
	 * Find the next integer-key,value pair if {@code this} is a table,
	 * return {@link Constants#NIL} if there are no more, or throw a {@link LuaError} if not a table.
	 *
	 * To iterate over integer keys in a table you can use
	 * <pre> {@code
	 *   LuaValue k = LuaValue.NIL;
	 *   while ( true ) {
	 *      Varargs n = table.inext(k);
	 *      if ( (k = n.arg1()).isnil() )
	 *         break;
	 *      LuaValue v = n.arg(2)
	 *      process( k, v )
	 *   }
	 * } </pre>
	 *
	 * @param key {@link LuaInteger} value identifying a key to start from,
	 *            or {@link Constants#NIL} to start at the beginning
	 * @return {@link Varargs} containing {@code (key, value)} for the next entry,
	 * or {@link Constants#NONE} if there are no more.
	 * @throws LuaError if {@code this} is not a table, or the supplied key is invalid.
	 * @see LuaTable
	 * @see #next(LuaValue)
	 * @see ValueFactory#valueOf(int)
	 * @see Varargs#first()
	 * @see Varargs#arg(int)
	 * @see #isNil()
	 */
	public Varargs inext(LuaValue key) {
		int k = key.checkInteger() + 1;
		LuaValue v = rawget(k);
		return v.isNil() ? NONE : varargsOf(LuaInteger.valueOf(k), v);
	}

	/**
	 * Call the supplied function once for each key-value pair
	 *
	 * @param state The current lua state
	 * @param func  The function to call
	 * @return {@link Constants#NIL}
	 */
	public LuaValue foreach(LuaState state, LuaValue func) {
		Varargs n;
		LuaValue k = NIL;
		LuaValue v;
		while (!(k = ((n = next(k)).first())).isNil()) {
			if (!(v = OperationHelper.call(state, func, k, n.arg(2))).isNil()) {
				return v;
			}
		}
		return NIL;
	}

	/**
	 * Call the supplied function once for each key-value pair
	 * in the contiguous array part
	 *
	 * @param state The current lua state
	 * @param func  The function to call
	 * @return {@link Constants#NIL}
	 */
	public LuaValue foreachi(LuaState state, LuaValue func) {
		LuaValue v, r;
		for (int k = 0; !(v = rawget(++k)).isNil(); ) {
			if (!(r = OperationHelper.call(state, func, valueOf(k), v)).isNil()) {
				return r;
			}
		}
		return NIL;
	}

	/**
	 * Set a hashtable value
	 *
	 * @param key   key to set
	 * @param value value to set
	 */
	public void hashset(LuaValue key, LuaValue value) {
		if (value.isNil()) {
			hashRemove(key);
		} else {
			int index = 0;
			if (hash.length > 0) {
				index = hashSlot(key);
				for (Slot slot = hash[index]; slot != null; slot = slot.rest()) {
					StrongSlot foundSlot;
					if ((foundSlot = slot.find(key)) != null) {
						hash[index] = hash[index].set(foundSlot, value);
						return;
					}
				}
			}
			if (checkLoadFactor()) {
				if (key.isIntExact() && key.toInteger() > 0) {
					// a rehash might make room in the array portion for this key.
					rehash(key.toInteger());
					if (arrayset(key.toInteger(), value)) {
						return;
					}
				} else {
					rehash(-1);
				}
				index = hashSlot(key);
			}
			Slot entry = (metatable != null)
				? metatable.entry(key, value)
				: defaultEntry(key, value);
			hash[index] = (hash[index] != null) ? hash[index].add(entry) : entry;
			++hashEntries;
		}
	}

	public static int hashpow2(int hashCode, int mask) {
		return hashCode & mask;
	}

	public static int hashmod(int hashCode, int mask) {
		return (hashCode & 0x7FFFFFFF) % mask;
	}

	/**
	 * Find the hashtable slot index to use.
	 *
	 * @param key      the key to look for
	 * @param hashMask N-1 where N is the number of hash slots (must be power of 2)
	 * @return the slot index
	 */
	public static int hashSlot(LuaValue key, int hashMask) {
		switch (key.type()) {
			case TNUMBER:
			case TTABLE:
			case TTHREAD:
			case TLIGHTUSERDATA:
			case TUSERDATA:
				return hashmod(key.hashCode(), hashMask);
			default:
				return hashpow2(key.hashCode(), hashMask);
		}
	}

	/**
	 * Find the hashtable slot to use
	 *
	 * @param key key to look for
	 * @return slot to use
	 */
	private int hashSlot(LuaValue key) {
		return hashSlot(key, hash.length - 1);
	}

	private void hashRemove(LuaValue key) {
		if (hash.length > 0) {
			int index = hashSlot(key);
			for (Slot slot = hash[index]; slot != null; slot = slot.rest()) {
				StrongSlot foundSlot;
				if ((foundSlot = slot.find(key)) != null) {
					hash[index] = hash[index].remove(foundSlot);
					--hashEntries;
					return;
				}
			}
		}
	}

	private boolean checkLoadFactor() {
		return hashEntries >= hash.length;
	}

	private int countHashKeys() {
		int keys = 0;
		for (Slot aHash : hash) {
			for (Slot slot = aHash; slot != null; slot = slot.rest()) {
				if (slot.firstSlot() != null) {
					keys++;
				}
			}
		}
		return keys;
	}

	private void dropWeakArrayValues() {
		for (int i = 0; i < array.length; ++i) {
			metatable.arrayGet(array, i);
		}
	}

	private int countIntKeys(int[] nums) {
		int total = 0;
		int i = 1;

		// Count integer keys in array part
		for (int bit = 0; bit < 31; ++bit) {
			if (i > array.length) {
				break;
			}
			int j = Math.min(array.length, 1 << bit);
			int c = 0;
			while (i <= j) {
				if (array[i++ - 1] != null) {
					c++;
				}
			}
			nums[bit] = c;
			total += c;
		}

		// Count integer keys in hash part
		for (i = 0; i < hash.length; ++i) {
			for (Slot s = hash[i]; s != null; s = s.rest()) {
				int k;
				if ((k = s.arraykey(Integer.MAX_VALUE)) > 0) {
					nums[log2(k)]++;
					total++;
				}
			}
		}

		return total;
	}

	// Compute ceil(log2(x))
	static int log2(int x) {
		int lg = 0;
		x -= 1;
		if (x < 0)
		// 2^(-(2^31)) is approximately 0
		{
			return Integer.MIN_VALUE;
		}
		if ((x & 0xFFFF0000) != 0) {
			lg = 16;
			x >>>= 16;
		}
		if ((x & 0xFF00) != 0) {
			lg += 8;
			x >>>= 8;
		}
		if ((x & 0xF0) != 0) {
			lg += 4;
			x >>>= 4;
		}
		switch (x) {
			case 0x0:
				return 0;
			case 0x1:
				lg += 1;
				break;
			case 0x2:
				lg += 2;
				break;
			case 0x3:
				lg += 2;
				break;
			case 0x4:
				lg += 3;
				break;
			case 0x5:
				lg += 3;
				break;
			case 0x6:
				lg += 3;
				break;
			case 0x7:
				lg += 3;
				break;
			case 0x8:
				lg += 4;
				break;
			case 0x9:
				lg += 4;
				break;
			case 0xA:
				lg += 4;
				break;
			case 0xB:
				lg += 4;
				break;
			case 0xC:
				lg += 4;
				break;
			case 0xD:
				lg += 4;
				break;
			case 0xE:
				lg += 4;
				break;
			case 0xF:
				lg += 4;
				break;
		}
		return lg;
	}

	private static void changeMode(Metatable metatable, LuaValue[] from, LuaValue[] to, int limit) {
		for (int i = 0; i < limit; i++) {
			LuaValue value = metatable.arrayGet(from, i);
			if (value == null) {
				to[i] = null;
			} else {
				to[i] = metatable.wrap(value);
			}
		}
	}

	/*
	 * newKey > 0 is next key to insert
	 * newKey == 0 means number of keys not changing (__mode changed)
	 * newKey < 0 next key will go in hash part
	 */
	private void rehash(int newKey) {
		Metatable metatable = this.metatable;
		if (metatable != null && (metatable.useWeakKeys() || metatable.useWeakValues())) {
			// If this table has weak entries, hashEntries is just an upper bound.
			hashEntries = countHashKeys();
			if (metatable.useWeakValues()) {
				dropWeakArrayValues();
			}
		}
		int[] nums = new int[32];
		int total = countIntKeys(nums);
		if (newKey > 0) {
			total++;
			nums[log2(newKey)]++;
		}

		// Choose N such that N <= sum(nums[0..log(N)]) < 2N
		int keys = nums[0];
		int newArraySize = 0;
		for (int log = 1; log < 32; ++log) {
			keys += nums[log];
			if (total * 2 < 1 << log) {
				// Not enough integer keys.
				break;
			} else if (keys >= (1 << (log - 1))) {
				newArraySize = 1 << log;
			}
		}

		final LuaValue[] oldArray = array;
		final Slot[] oldHash = hash;
		final LuaValue[] newArray;
		final Slot[] newHash;

		// Copy existing array entries and compute number of moving entries.
		int movingToArray = 0;
		if (newKey > 0 && newKey <= newArraySize) {
			movingToArray--;
		}
		if (newArraySize != oldArray.length) {
			newArray = new LuaValue[newArraySize];
			if (newArraySize > oldArray.length) {
				for (int i = log2(oldArray.length + 1), j = log2(newArraySize) + 1; i < j; ++i) {
					movingToArray += nums[i];
				}
			} else if (oldArray.length > newArraySize) {
				for (int i = log2(newArraySize + 1), j = log2(oldArray.length) + 1; i < j; ++i) {
					movingToArray -= nums[i];
				}
			}

			if (newKey == 0 && metatable != null) {
				changeMode(metatable, oldArray, newArray, Math.min(oldArray.length, newArraySize));
			} else {
				System.arraycopy(oldArray, 0, newArray, 0, Math.min(oldArray.length, newArraySize));
			}
		} else if (newKey == 0 && metatable != null) {
			newArray = oldArray;
			changeMode(metatable, oldArray, newArray, oldArray.length);
		} else {
			newArray = oldArray;
		}

		final int newHashSize = hashEntries - movingToArray + ((newKey < 0 || newKey > newArraySize) ? 1 : 0); // Make room for the new entry
		final int oldCapacity = oldHash.length;
		final int newCapacity;
		final int newHashMask;

		if (newHashSize > 0) {
			// round up to next power of 2.
			newCapacity = (newHashSize < MIN_HASH_CAPACITY)
				? MIN_HASH_CAPACITY
				: 1 << log2(newHashSize);
			newHashMask = newCapacity - 1;
			newHash = new Slot[newCapacity];
		} else {
			newCapacity = 0;
			newHashMask = 0;
			newHash = Slot.NOBUCKETS;
		}

		// Move hash buckets
		for (Slot anOldHash : oldHash) {
			for (Slot slot = anOldHash; slot != null; slot = slot.rest()) {
				int k;
				if ((k = slot.arraykey(newArraySize)) > 0) {
					StrongSlot entry = slot.firstSlot();
					if (entry != null) {
						newArray[k - 1] = metatable == null ? entry.value() : metatable.wrap(entry.value());
					}
				} else {
					int j = slot.keyindex(newHashMask);
					if (newKey == 0) {
						Slot current = newHash[j];
						StrongSlot currentEntry = slot.firstSlot();
						if (currentEntry != null) {
							Slot entry = metatable != null
								? metatable.entry(currentEntry.key(), currentEntry.value())
								: defaultEntry(currentEntry.key(), currentEntry.value());
							newHash[j] = current != null ? current.add(entry) : entry;
						}
					} else {
						newHash[j] = slot.relink(newHash[j]);
					}
				}
			}
		}

		// Move array values into hash portion
		for (int i = newArraySize; i < oldArray.length; ) {
			LuaValue v;
			if ((v = oldArray[i++]) != null) {
				int slot = hashmod(i, newHashMask);
				Slot newEntry;
				if (metatable != null) {
					newEntry = metatable.entry(valueOf(i), v);
					if (newEntry == null) {
						continue;
					}
				} else {
					newEntry = defaultEntry(valueOf(i), v);
				}
				newHash[slot] = (newHash[slot] != null)
					? newHash[slot].add(newEntry) : newEntry;
			}
		}

		hash = newHash;
		array = newArray;
		hashEntries -= movingToArray;
	}

	@Override
	public Slot entry(LuaValue key, LuaValue value) {
		return defaultEntry(key, value);
	}

	public static boolean isLargeKey(LuaValue key) {
		switch (key.type()) {
			case TSTRING:
				return ((LuaString) key).length() > LuaString.RECENT_STRINGS_MAX_LENGTH;
			case TNUMBER:
			case TBOOLEAN:
				return false;
			default:
				return true;
		}
	}

	public static Entry defaultEntry(LuaValue key, LuaValue value) {
		if (key.isIntExact()) {
			return new IntKeyEntry(key.toInteger(), value);
		} else if (value.type() == TNUMBER) {
			return new NumberValueEntry(key, value.toDouble());
		} else {
			return new NormalEntry(key, value);
		}
	}

	// ----------------- sort support -----------------------------
	//
	// implemented heap sort from wikipedia
	//
	// Only sorts the contiguous array part.
	//

	/**
	 * Sort the table using a comparator.
	 *
	 * @param comparator {@link LuaValue} to be called to compare elements.
	 */
	public void sort(LuaState state, LuaValue comparator) {
		if (metatable != null && metatable.useWeakValues()) {
			dropWeakArrayValues();
		}
		int n = array.length;
		while (n > 0 && array[n - 1] == null) {
			--n;
		}
		if (n > 1) {
			heapSort(state, n, comparator);
		}
	}

	private void heapSort(LuaState state, int count, LuaValue cmpfunc) {
		heapify(state, count, cmpfunc);
		for (int end = count - 1; end > 0; ) {
			swap(end, 0);
			siftDown(state, 0, --end, cmpfunc);
		}
	}

	private void heapify(LuaState state, int count, LuaValue cmpfunc) {
		for (int start = count / 2 - 1; start >= 0; --start) {
			siftDown(state, start, count - 1, cmpfunc);
		}
	}

	private void siftDown(LuaState state, int start, int end, LuaValue cmpfunc) {
		for (int root = start; root * 2 + 1 <= end; ) {
			int child = root * 2 + 1;
			if (child < end && compare(state, child, child + 1, cmpfunc)) {
				++child;
			}
			if (compare(state, root, child, cmpfunc)) {
				swap(root, child);
				root = child;
			} else {
				return;
			}
		}
	}

	private boolean compare(LuaState state, int i, int j, LuaValue cmpfunc) {
		LuaValue a, b;
		if (metatable == null) {
			a = array[i];
			b = array[j];
		} else {
			a = metatable.arrayGet(array, i);
			b = metatable.arrayGet(array, j);
		}
		if (a == null || b == null) {
			return false;
		}
		if (!cmpfunc.isNil()) {
			return OperationHelper.call(state, cmpfunc, a, b).toBoolean();
		} else {
			return OperationHelper.lt(state, a, b);
		}
	}

	private void swap(int i, int j) {
		LuaValue a = array[i];
		array[i] = array[j];
		array[j] = a;
	}

	/**
	 * This may be deprecated in a future release.
	 * It is recommended to count via iteration over next() instead
	 *
	 * @return count of keys in the table
	 */
	public int keyCount() {
		LuaValue k = NIL;
		for (int i = 0; true; i++) {
			Varargs n = next(k);
			if ((k = n.first()).isNil()) {
				return i;
			}
		}
	}

	/**
	 * This may be deprecated in a future release.
	 * It is recommended to use next() instead
	 *
	 * @return array of keys in the table
	 */
	public LuaValue[] keys() {
		List<LuaValue> l = new ArrayList<LuaValue>();
		LuaValue k = NIL;
		while (true) {
			Varargs n = next(k);
			if ((k = n.first()).isNil()) {
				break;
			}
			l.add(k);
		}

		return l.toArray(new LuaValue[l.size()]);
	}

	// Metatable operations

	@Override
	public boolean useWeakKeys() {
		return false;
	}

	@Override
	public boolean useWeakValues() {
		return false;
	}

	@Override
	public LuaValue toLuaValue() {
		return this;
	}

	@Override
	public LuaValue wrap(LuaValue value) {
		return value;
	}

	@Override
	public LuaValue arrayGet(LuaValue[] array, int index) {
		return array[index];
	}
}
