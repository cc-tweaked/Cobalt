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
package org.squiddev.cobalt;

import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.lang.ref.WeakReference;
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
 * The main table operations are defined on {@link OperationHelper} and {@link LuaTable}
 * for getting and setting values with and without metatag processing:
 * <ul>
 * <li>{@link OperationHelper#getTable(LuaState, LuaValue, LuaValue)}</li>
 * <li>{@link OperationHelper#setTable(LuaState, LuaValue, LuaValue, LuaValue)}</li>
 * <li>{@link LuaTable#rawget(LuaValue)}</li>
 * <li>{@link LuaTable#rawset(LuaValue, LuaValue)}</li>
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
public final class LuaTable extends LuaValue {
	private static final Object[] EMPTY_ARRAY = new Object[0];
	private static final Node[] EMPTY_NODES = new Node[0];
	private static final LuaString N = valueOf("n");

	private Object[] array = EMPTY_ARRAY;
	private Node[] nodes = EMPTY_NODES;
	private int lastFree = 0;

	private boolean weakKeys;
	private boolean weakValues;

	private int metatableFlags;
	private LuaTable metatable;

	/**
	 * Construct empty table
	 */
	public LuaTable() {
		super(TTABLE);
		array = NOVALS;
	}

	/**
	 * Construct table with preset capacity.
	 *
	 * @param narray capacity of array part
	 * @param nhash  capacity of hash part
	 */
	public LuaTable(int narray, int nhash) {
		super(TTABLE);
		resize(narray, nhash, false);
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
		resize(nu + nl, nn >> 1, false);
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
		resize(n, 1, false);
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
	 * @param nArray the number of array slots to preallocate in the table.
	 */
	public void presize(int nArray) {
		if (nArray > array.length) {
			array = setArrayVector(array, 1 << log2(nArray), false, weakValues);
		}
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
		return nodes.length;
	}

	@Override
	public LuaTable getMetatable(LuaState state) {
		return metatable;
	}

	public void setMetatable(LuaTable mt) {
		metatable = mt;

		boolean newWeakKeys = false, newWeakValues = false;

		if (mt != null) {
			LuaValue mode = mt.rawget(Constants.MODE);
			if (mode.isString()) {
				String m = mode.toString();
				if (m.indexOf('k') >= 0) newWeakKeys = true;
				if (m.indexOf('v') >= 0) newWeakValues = true;
			}
		}

		if (newWeakKeys != weakKeys || newWeakValues != weakValues) {
			weakKeys = newWeakKeys;
			weakValues = newWeakValues;
			rehash(null, true);
		}
	}

	public void useWeak(boolean newWeakKeys, boolean newWeakValues) {
		if (newWeakKeys != weakKeys || newWeakValues != weakValues) {
			weakKeys = newWeakKeys;
			weakValues = newWeakValues;
			rehash(null, true);
		}
	}

	@Override
	public void setMetatable(LuaState state, LuaTable metatable) {
		setMetatable(metatable);
	}

	/**
	 * Get a value in a table without metatag processing.
	 *
	 * @param key the key to look up, must not be null
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 */
	public LuaValue rawget(String key) {
		return rawget(ValueFactory.valueOf(key));
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use, must not be null
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 */
	public void rawset(String key, LuaValue value) {
		rawset(ValueFactory.valueOf(key), value);
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
	 * @throws LuaError When a value is not a string.
	 */
	public LuaValue concat(LuaString sep, int i, int j) throws LuaError {
		Buffer sb = new Buffer();
		if (i <= j) {
			sb.append(rawget(i).checkLuaString());
			while (++i <= j) {
				sb.append(sep);
				sb.append(rawget(i).checkLuaString());
			}
		}
		return sb.toLuaString();
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
			if (!strengthen(array[i]).isNil()) {
				n = i + 1;
			}
		}
		for (Node node : nodes) {
			LuaValue value = node.key();
			if (value.type() == Constants.TNUMBER) {
				double key = value.toDouble();
				if (key > n) n = key;
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
	public Varargs next(LuaValue key) throws LuaError {
		int i = findIndex(key);
		if (i < 0) throw new LuaError("invalid key to 'next'");

		for (; i < array.length; i++) {
			LuaValue value = strengthen(array[i]);
			if (!value.isNil()) return varargsOf(valueOf(i + 1), value);
		}

		i -= array.length;
		for (; i < nodes.length; i++) {
			Node node = nodes[i];
			LuaValue value = node.value();
			if (!node.key().isNil() && !value.isNil()) return varargsOf(node.key(), value);
		}

		return NIL;
	}

	/**
	 * Returns the index of this key for table traversals + 1.
	 *
	 * @param key The key to lookup
	 * @return The next index for traversals.
	 */
	private int findIndex(LuaValue key) {
		// Return the first entry
		if (key.isNil()) return 0;

		// Its in the array part so just return that
		int arrayIndex = arraySlot(key);
		if (arrayIndex > 0 && arrayIndex <= array.length) return arrayIndex;
		if (nodes.length == 0) return -1;

		// Must be in the main part so try to find it in the chain.
		int idx = hashSlot(key);
		Node node = nodes[idx];
		while (true) {
			if (node.key().equals(key)) {
				return idx + array.length + 1;
			}

			if (node.next >= 0) {
				idx = node.next;
				node = nodes[node.next];
			} else {
				return -1;
			}
		}
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
	public Varargs inext(LuaValue key) throws LuaError {
		int k = key.checkInteger() + 1;
		LuaValue v = rawget(k);
		return v.isNil() ? NONE : varargsOf(LuaInteger.valueOf(k), v);
	}


	private static int hashpow2(int hashCode, int mask) {
		return hashCode & mask;
	}

	private static int hashmod(int hashCode, int mask) {
		return (hashCode & 0x7FFFFFFF) % (mask | 1);
	}

	/**
	 * Find the hashtable slot index to use.
	 *
	 * @param key      the key to look for
	 * @param hashMask N-1 where N is the number of hash slots (must be power of 2)
	 * @return the slot index
	 */
	private static int hashSlot(LuaValue key, int hashMask) {
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
	 * Get the array slot from a set value.
	 *
	 * @param value The value to use
	 * @return The array slot or 0 if not usable in an array
	 */
	private static int arraySlot(LuaValue value) {
		if (value instanceof LuaInteger) {
			int val = ((LuaInteger) value).v;

			if (val > 0) return val;
		}

		return 0;
	}

	/**
	 * Find the hashtable slot to use
	 *
	 * @param key key to look for
	 * @return slot to use
	 */
	private int hashSlot(LuaValue key) {
		return hashSlot(key, nodes.length - 1);
	}

	private void dropWeakArrayValues() {
		for (int i = 0; i < array.length; ++i) {
			Object x = array[i];
			if (x != NIL && strengthen(x).isNil()) array[i] = NIL;
		}
	}

	// Compute ceil(log2(x))
	private static int log2(int x) {
		// TODO: Use 31 - Integer.numberOfLeadingZeros(bits);
		// See: http://stackoverflow.com/a/3305710/1447657

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

	// ----------------- sort support -----------------------------
	//
	// implemented heap sort from wikipedia
	//
	// Only sorts the contiguous array part.
	//

	/**
	 * Prepare the table for being sorted. Trimming unused values
	 *
	 * @return The length of this array.
	 * @throws LuaError On a runtime error.
	 */
	public int prepSort() throws LuaError {
		if (weakValues) dropWeakArrayValues();
		int n = array.length;
		while (n > 0 && array[n - 1] == NIL) {
			--n;
		}

		return n;
	}

	public boolean compare(LuaState state, int i, int j, LuaValue cmpfunc) throws LuaError, UnwindThrowable {
		LuaValue a, b;

		a = strengthen(array[i]);
		b = strengthen(array[j]);

		if (a.isNil() || b.isNil()) {
			return false;
		}
		if (!cmpfunc.isNil()) {
			return OperationHelper.call(state, cmpfunc, a, b).toBoolean();
		} else {
			return OperationHelper.lt(state, a, b);
		}
	}

	public void swap(int i, int j) {
		Object a = array[i];
		array[i] = array[j];
		array[j] = a;
	}

	/**
	 * This may be deprecated in a future release.
	 * It is recommended to count via iteration over next() instead
	 *
	 * @return count of keys in the table
	 * @throws LuaError If iterating the table fails.
	 */
	public int keyCount() throws LuaError {
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
	 * @throws LuaError If iterating the table fails.
	 */
	public LuaValue[] keys() throws LuaError {
		List<LuaValue> l = new ArrayList<>();
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

	/**
	 * Load a library instance by setting its environment to {@code this}
	 * and calling it, which should initialize the library instance and
	 * install itself into this instance.
	 *
	 * @param state   The current lua state
	 * @param library The callable {@link LuaFunction} to load into {@code this}
	 * @return {@link LuaValue} containing the result of the initialization call.
	 */
	public LuaValue load(LuaState state, LuaLibrary library) {
		return library.add(state, this);
	}

	//region Resizing

	/**
	 * Resize the table
	 */
	private static Object[] setArrayVector(Object[] oldArray, int n, boolean metaChange, boolean weakValues) {
		Object[] newArray = new Object[n];
		int len = Math.min(n, oldArray.length);
		if (metaChange) {
			for (int i = 0; i < len; i++) {
				LuaValue value = strengthen(oldArray[i]);
				newArray[i] = weakValues ? weaken(value) : value;
			}
		} else {
			System.arraycopy(oldArray, 0, newArray, 0, Math.min(n, oldArray.length));
		}

		// Fill remaining entries with nil
		for (int i = oldArray.length; i < newArray.length; i++) newArray[i] = NIL;

		return newArray;
	}

	private static int countInt(LuaValue key, int[] nums) {
		int idx = arraySlot(key);
		if (idx != 0) {
			nums[log2(idx)]++;
			return 1;
		}
		return 0;
	}

	/**
	 * Count the number of keys in the array part of table 't'.
	 *
	 * @param nums Filled with the count of values between 2^(i-1)+1 to 2^i;
	 * @return Total number of non-nil keys.
	 */
	private int numUseArray(int[] nums) {
		int lg;
		int ttlg; // 2 ^ lg;
		int ause = 0; // Summation of nums
		int i = 1; // Count to traverse all array keys

		for (lg = 0, ttlg = 1; lg <= 31; lg++, ttlg *= 2) {
			int lc = 0;
			int lim = ttlg;
			if (lim > array.length) {
				lim = array.length; // Adjust upper limit
				if (i > lim) break;
			}

			for (; i <= lim; i++) {
				LuaValue value = strengthen(array[i - 1]);
				if (!value.isNil()) lc++;
			}
			nums[lg] += lc;
			ause += lc;
		}

		return ause;
	}

	private void setNodeVector(int size) {
		if (size == 0) {
			nodes = EMPTY_NODES;
			lastFree = 0;
		} else {
			int lsize = log2(size);
			size = 1 << lsize;
			Node[] nodes = this.nodes = new Node[size];
			for (int i = 0; i < size; i++) nodes[i] = new Node(weakKeys, weakValues);

			// All positions are free
			lastFree = size - 1;
		}
	}

	private void resize(int newArraySize, int newHashSize, boolean modeChange) {
		int oldArraySize = array.length;
		int oldHashSize = nodes.length;

		if (newArraySize != 0 && newHashSize != 0 && newArraySize == oldArraySize && newHashSize == oldHashSize && !modeChange) {
			throw new IllegalStateException("Attempting to resize with no change");
		}

		// Array part must grow
		if (newArraySize > oldArraySize) {
			array = setArrayVector(array, newArraySize, modeChange, weakValues);
		}

		Node[] oldNode = nodes;
		setNodeVector(newHashSize);

		if (newArraySize < oldArraySize) {
			Object[] oldArray = array;
			array = setArrayVector(oldArray, newArraySize, modeChange, weakValues);

			// Copy values out of array part into the hash
			for (int i = newArraySize; i < oldArraySize; i++) {
				LuaValue value = strengthen(oldArray[i]);
				if (!value.isNil()) rawset(i + 1, value);
			}

		} else if (newArraySize == oldArraySize && modeChange) {
			Object[] values = array;
			for (int i = 0; i < oldArraySize; i++) {
				LuaValue value = strengthen(values[i]);
				values[i] = weakValues ? weaken(value) : value;
			}
		}

		// Re-insert elements from hash part
		for (int i = oldHashSize - 1; i >= 0; i--) {
			Node old = oldNode[i];
			LuaValue key = old.key();
			LuaValue value = old.value();
			if (!key.isNil() && !value.isNil()) rawset(key, value);
		}
	}

	private void rehash(LuaValue extraKey, boolean mode) {
		if (weakValues) dropWeakArrayValues();

		int[] nums = new int[32]; // Counts for various functions
		int arraySize = 0; // Optimal size for array part
		int arrayCount = numUseArray(nums); // Number of keys in the array part
		int totalCount = arrayCount; // Number of values in total

		// Count the number of hash values that can be moved to the array, as well as the total count.
		// See numusehash in ltable.c
		{
			int i = nodes.length;
			while (--i >= 0) {
				Node node = nodes[i];
				LuaValue key = node.key();
				if (!key.isNil()) {
					arrayCount += countInt(key, nums);
					totalCount++;
				}
			}
		}

		if (extraKey != null) {
			// Count the extra key we're using
			arrayCount += countInt(extraKey, nums);
			totalCount++;
		}

		// Derive optimal size for new array part
		// See computesizes in ltable.c
		{
			int sum = 0; // Number of elements smaller than 2 ^ i
			int numArray = 0; // Number of elements to go to array part

			// Loop while keys can fill more than half of total size
			for (int i1 = 0, twoPow = 1; arrayCount > twoPow / 2; i1++, twoPow *= 2) {
				if (nums[i1] > 0) {
					sum += nums[i1];

					// If more than half the elements are present
					if (sum > twoPow / 2) {
						arraySize = twoPow;
						numArray = sum;
					}
				}
			}

			assert (arraySize == 0 || arraySize / 2 < numArray) && numArray <= arraySize;
			arrayCount = numArray;
		}

		resize(arraySize, totalCount - arrayCount, mode);
	}
	//endregion

	//region Getting/setting

	/**
	 * Get the first free slot in the map
	 *
	 * @return The first slot in the map
	 */
	private int getFreePos() {
		if (nodes.length == 0) return -1;
		while (lastFree >= 0) {
			Node last = nodes[lastFree--];
			if (last.key == NIL) {
				return lastFree + 1;
			}
		}

		return -1;
	}

	/**
	 * Insert a new key into a hash table.
	 *
	 * First check whether key's main position is free. If not, check whether the colliding node is in its main position
	 * or not. If it is not, move colliding node to an empty place and put new key in its main position, otherwise the
	 * colliding node is in its main position and the new key goes to an empty position.
	 *
	 * @param key The key to set
	 * @throws IllegalArgumentException If this key cannot be used.
	 */
	private Node newKey(LuaValue key) {
		if (key.isNil()) throw new IllegalArgumentException("table index is nil");

		// Rehash and let the rawgetter handle it
		if (nodes.length == 0) {
			rehash(key, false);
			return null;
		}

		Node mainNode = nodes[hashSlot(key)];
		LuaValue mainKey = mainNode.key();
		if (!mainKey.isNil() && !mainNode.value().isNil()) {
			// If we've got a collision then
			final int freePos = getFreePos();

			if (freePos < 0) {
				rehash(key, false);
				return null;
			}

			final Node freeNode = nodes[freePos];

			int otherPos = hashSlot(mainKey);
			Node otherNode = nodes[otherPos];

			if (otherNode != mainNode) {
				// If the colliding position isn't at its main position then we move it to a free position

				// Walk the chain to find the node just before the desired one
				while (nodes[otherNode.next] != mainNode) {
					otherNode = nodes[otherNode.next];
				}

				// Rechain other to point to the free position
				otherNode.next = freePos;

				// Copy colliding node into free position
				freeNode.key = mainNode.key;
				freeNode.value = mainNode.value;
				freeNode.next = mainNode.next;

				// Clear main node
				mainNode.next = -1;
				mainNode.key = NIL;
				mainNode.value = NIL;
			} else {
				// Colliding node is in the main position so we will assign to a free position.

				if (mainNode.next != -1) {
					// We're inserting "after" the first node in the linked list so change the
					// next node.
					freeNode.next = mainNode.next;
				} else {
					assert freeNode.next == -1;
				}

				// Insert after the main node
				mainNode.next = freePos;

				mainNode = freeNode;
			}
		}

		mainNode.key = weakKeys ? weaken(key) : key;

		return mainNode;
	}

	private Node rawgetNode(int search) {
		if (nodes.length == 0) return null;

		Node node = nodes[hashmod(search, nodes.length - 1)];
		while (true) {
			LuaValue key = node.key();
			if (key instanceof LuaInteger && ((LuaInteger) key).v == search) {
				return node;
			} else {
				int next = node.next;
				if (next == -1) return null;
				node = nodes[next];
			}
		}
	}

	private Node rawgetNode(LuaValue search) {
		if (nodes.length == 0) return null;

		int slot = hashSlot(search);
		Node node = nodes[slot];
		while (true) {
			LuaValue key = node.key();
			if (key.equals(search)) {
				return node;
			} else {
				int next = node.next;
				if (next == -1) return null;
				node = nodes[next];
			}
		}
	}

	public LuaValue rawget(int search) {
		if (search > 0 && search <= array.length) {
			return strengthen(array[search - 1]);
		} else if (nodes.length == 0) {
			return NIL;
		} else {
			Node node = rawgetNode(search);
			return node == null ? NIL : node.value();
		}
	}

	public LuaValue rawget(LuaValue search) {
		if (search instanceof LuaInteger) return rawget(((LuaInteger) search).v);

		Node node = rawgetNode(search);
		return node == null ? NIL : node.value();
	}

	public LuaValue rawget(CachedMetamethod search) {
		int flag = 1 << search.ordinal();
		if ((metatableFlags & flag) != 0) return NIL;

		Node node = rawgetNode(search.getKey());
		if (node != null) {
			LuaValue value = node.value();
			if (!value.isNil()) return value;
		}

		metatableFlags |= flag;
		return NIL;
	}

	public void rawset(int key, LuaValue value) {
		LuaValue valueOf = null;
		do {
			if (key > 0 && key <= array.length) {
				array[key - 1] = weakValues ? weaken(value) : value;
				return;
			}

			if (valueOf == null) valueOf = valueOf(key);

			Node node = rawgetNode(valueOf);
			if (node == null) node = newKey(valueOf);

			// newKey will have handled this otherwise
			if (node != null) {
				// if (value.isNil() && !weakKeys) node.key = weaken((LuaValue) node.key);
				node.value = weakValues ? weaken(value) : value;
				return;
			}
		} while (true);
	}

	public void rawset(LuaValue key, LuaValue value) {
		if (key instanceof LuaInteger) {
			rawset(((LuaInteger) key).v, value);
			return;
		}

		do {
			Node node = rawgetNode(key);
			if (node == null) node = newKey(key);

			// newKey will have handled this otherwise
			if (node != null) {
				// if (value.isNil() && !weakKeys) node.key = weaken((LuaValue) node.key);
				node.value = weakValues ? weaken(value) : value;
				metatableFlags = 0;
				return;
			}
		} while (true);
	}
	//endregion

	//region Weak references

	/**
	 * Self-sent message to convert a value to its weak counterpart
	 *
	 * @param value value to convert
	 * @return {@link LuaValue} that is a strong or weak reference, depending on type of {@code value}
	 */
	private static Object weaken(LuaValue value) {
		switch (value.type()) {
			case TFUNCTION:
			case TTHREAD:
			case TTABLE:
				return new WeakReference<>(value);
			case TUSERDATA:
				return new WeakUserdata((LuaUserdata) value);
			default:
				return value;
		}
	}

	/**
	 * Unwrap a LuaValue from a WeakReference and/or WeakUserdata.
	 *
	 * @param ref reference to convert
	 * @return LuaValue or null
	 * @see #weaken(LuaValue)
	 */
	@SuppressWarnings("unchecked")
	static LuaValue strengthen(Object ref) {
		if (ref instanceof WeakReference) {
			LuaValue value = ((WeakReference<LuaValue>) ref).get();
			return value == null ? NIL : value;
		} else if (ref instanceof WeakUserdata) {
			return ((WeakUserdata) ref).strongValue();
		} else {
			return (LuaValue) ref;
		}
	}

	/**
	 * Internal class to implement weak userdata values.
	 */
	private static final class WeakUserdata {
		private WeakReference<LuaValue> ref;
		private final WeakReference<Object> ob;
		private final LuaTable mt;

		private WeakUserdata(LuaUserdata value) {
			ref = new WeakReference<>(value);
			ob = new WeakReference<>(value.instance);
			mt = value.metatable;
		}

		public LuaValue strongValue() {
			LuaValue u = ref.get();
			if (u != null) return u;

			Object o = ob.get();
			if (o != null) {
				LuaValue ud = userdataOf(o, mt);
				ref = new WeakReference<>(ud);
				return ud;
			} else {
				return NIL;
			}
		}
	}
	//endregion

	/**
	 * Represents a node in the hash element of the table.
	 */
	private static final class Node {
		private final boolean weakKey;
		private final boolean weakValue;
		Object value = NIL;
		Object key = NIL;
		int next = -1;

		Node(boolean weakKey, boolean weakValue) {
			this.weakKey = weakKey;
			this.weakValue = weakValue;
		}

		@Override
		public String toString() {
			String main = key + "=" + value;
			if (next >= 0) main += "->" + next;
			return main;
		}

		/**
		 * Get the current key, converting it to a strong reference if
		 * required. If it is nil then it clears the key and value (marking it
		 * as "dead").
		 *
		 * @return The entry's key.
		 */
		LuaValue key() {
			Object key = this.key;
			if (key == NIL || !weakKey) return (LuaValue) key;

			LuaValue strengthened = strengthen(key);
			if (strengthened.isNil()) this.value = NIL; // We preserve the key so we can check it is nil

			return strengthened;
		}

		/**
		 * Get the current value, converting it to a strong reference if required.
		 *
		 * @return The entry's value.
		 */
		LuaValue value() {
			Object value = this.value;
			if (value == NIL || !weakValue) return (LuaValue) value;

			LuaValue strengthened = strengthen(value);
			if (strengthened.isNil()) this.value = NIL;
			return strengthened;
		}
	}
}
