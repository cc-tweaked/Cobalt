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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.*;

/**
 * Subclass of {@link LuaValue} for representing lua tables.
 * <p>
 * Almost all API's implemented in {@link LuaTable} are defined and documented in {@link LuaValue}.
 * <p>
 * If a table is needed, the one of the type-checking functions can be used such as {@link #checkTable()} or
 * {@link #optTable(LuaTable)}.
 * <p>
 * The main table operations are defined on {@link OperationHelper} and {@link LuaTable}
 * for getting and setting values with and without metatag processing:
 * <ul>
 * <li>{@link OperationHelper#getTable(LuaState, LuaValue, LuaValue)}</li>
 * <li>{@link OperationHelper#setTable(LuaState, LuaValue, LuaValue, LuaValue)}</li>
 * <li>{@link LuaTable#rawget(LuaValue)}</li>
 * <li>{@link LuaTable#rawset(LuaValue, LuaValue)}</li>
 * </ul>
 * <p>
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
 * <p>
 * As with other types, {@link LuaTable} instances should be constructed via one of the table constructor
 * methods on {@link LuaValue}:
 * <ul>
 * <li>{@link ValueFactory#tableOf()} empty table</li>
 * <li>{@link ValueFactory#listOf(LuaValue[])} initialize array part</li>
 * <li>{@link ValueFactory#tableOf(LuaValue[])} initialize named hash part</li>
 * </ul>
 *
 * @see LuaValue
 */
public final class LuaTable extends LuaValue {
	private static final Object[] EMPTY_ARRAY = new Object[0];
	private static final int[] EMPTY_NEXT = new int[0];

	private Object[] array = EMPTY_ARRAY;

	private Object[] keys = EMPTY_ARRAY;
	private Object[] values = EMPTY_ARRAY;
	private int[] next = EMPTY_NEXT;

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
	}

	/**
	 * Construct table with preset capacity.
	 *
	 * @param arraySize capacity of array part
	 * @param hashSize  capacity of hash part
	 */
	public LuaTable(int arraySize, int hashSize) {
		super(TTABLE);
		resize(arraySize, hashSize, false);
	}

	@Override
	public LuaTable checkTable() {
		return this;
	}

	/**
	 * Preallocate the array part of a table to be a certain size,
	 * <p>
	 * Primarily used internally in response to a SETLIST bytecode.
	 *
	 * @param nArray the number of array slots to preallocate in the table.
	 */
	public void presize(int nArray) {
		if (nArray > array.length) {
			resize(nArray, keys.length, false);
		}
	}

	@Override
	public LuaTable getMetatable(@Nullable LuaState state) {
		return metatable;
	}

	@Override
	public void setMetatable(@Nullable LuaState state, LuaTable mt) {
		metatable = mt;

		boolean newWeakKeys = false, newWeakValues = false;

		if (mt != null) {
			LuaValue mode = mt.rawget(Constants.MODE);
			if (mode.isString()) {
				LuaString m = (LuaString) mode.toLuaString();
				if (m.indexOf((byte) 'k') >= 0) newWeakKeys = true;
				if (m.indexOf((byte) 'v') >= 0) newWeakValues = true;
			}
		}

		if (newWeakKeys != weakKeys || newWeakValues != weakValues) {
			weakKeys = newWeakKeys;
			weakValues = newWeakValues;
			rehash(null, true);
		}
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
		rawsetImpl(ValueFactory.valueOf(key), value);
	}

	/**
	 * Move items inside this table.
	 *
	 * @param from  The start position.
	 * @param to    The destination position.
	 * @param count The number of values to move.
	 */
	public void move(int from, int to, int count) {
		// TODO: Use System.arraycopy here where possible.
		if (to >= from + count || to <= from) {
			for (int i = 0; i < count; i++) rawset(to + i, rawget(from + i));
		} else {
			for (int i = count - 1; i >= 0; i--) rawset(to + i, rawget(from + i));
		}
	}

	public int length() {
		int a = array.length;
		/*
		 * Array cannot contain nil value, except if that array is statically allocated
		 * So if the last element is nil it means we need to binary search the array to find
		 * the first element non nil followed with a nil value
		 */
		if (a > 0 && rawget(a).isNil()) {
			int n = a + 1, m = 0;
			while (n - m > 1) {
				int k = (m + n) / 2;

				if (rawget(k).isNil()) {
					n = k;
				} else {
					m = k;
				}
			}
			return m;
		} else if (keys.length == 0) {
			// When no nodes are present and the last item is not nil,
			// the size of the table is the exact same size its capacity,
			// so we can directly return the array.length
			return a;
		} else {
			long i = a;
			long j = i + 1;
			while (!rawget((int) j).isNil()) {
				i = j;
				j *= 2;
				// Fallback to linear search, something is wrong
				if (j > ((long) Integer.MAX_VALUE * 2) - 2) {
					i = 1;
					while (!rawget((int) i).isNil()) i++;
					return (int) i - 1;
				}
			}

			// Binary search
			while (j - i > 1) {
				int k = ((int) i + (int) j) / 2;
				if (!rawget(k).isNil()) {
					i = k;
				} else {
					j = k;
				}
			}
			return (int) i;
		}
	}

	/**
	 * Get the number of entries in this table.
	 *
	 * @return The number of items in this table.
	 * @see Map#size()
	 * @see #length()
	 */
	public int size() {
		int n = 0;
		for (var k : array) if (!strengthen(k).isNil()) n++;
		for (int i = 0; i < keys.length; i++) {
			if (!key(i).isNil() && !value(i).isNil()) n++;
		}
		return n;
	}

	/**
	 * Find the next key,value pair if {@code this} is a table,
	 * return {@link Constants#NIL} if there are no more, or throw a {@link LuaError} if not a table.
	 * <p>
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
		for (; i < keys.length; i++) {
			LuaValue thisKey = key(i);
			LuaValue thisValue = value(i);
			if (!thisKey.isNil() && !thisValue.isNil()) return varargsOf(thisKey, thisValue);
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
		if (keys.length == 0) return -1;

		// Must be in the main part so try to find it in the chain.
		int idx = hashSlot(key);
		while (true) {
			if (key(idx).equals(key)) {
				return idx + array.length + 1;
			}

			idx = next[idx];
			if (idx < 0) return -1;
		}
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
		return switch (key.type()) {
			case TNUMBER, TTABLE, TTHREAD, TLIGHTUSERDATA, TUSERDATA -> hashmod(key.hashCode(), hashMask);
			default -> hashpow2(key.hashCode(), hashMask);
		};
	}

	/**
	 * Get the array slot from a set value.
	 *
	 * @param value The value to use
	 * @return The array slot or 0 if not usable in an array
	 */
	private static int arraySlot(LuaValue value) {
		if (value instanceof LuaInteger i) {
			int val = i.intValue();
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
		return hashSlot(key, keys.length - 1);
	}

	private void dropWeakArrayValues() {
		for (int i = 0; i < array.length; ++i) {
			Object x = array[i];
			if (x != NIL && strengthen(x).isNil()) array[i] = NIL;
		}
	}

	// Compute ceil(log2(x))
	private static int log2(int x) {
		return 32 - Integer.numberOfLeadingZeros(x - 1);
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
			keys = values = EMPTY_ARRAY;
			next = EMPTY_NEXT;
			lastFree = 0;
		} else {
			int lsize = log2(size);
			size = 1 << lsize;

			keys = new Object[size];
			values = new Object[size];
			next = new int[size];

			// TODO: It would be nice if we didn't need to fill here, as this can be quite slow.
			Arrays.fill(keys, NIL);
			Arrays.fill(values, NIL);
			Arrays.fill(next, -1);

			// All positions are free
			lastFree = size - 1;
		}
	}

	private void resize(int newArraySize, int newHashSize, boolean modeChange) {
		int oldArraySize = array.length;
		int oldHashSize = keys.length;

		// Array part must grow
		if (newArraySize > oldArraySize) {
			array = setArrayVector(array, newArraySize, modeChange, weakValues);
		}

		Object[] oldKeys = keys;
		Object[] oldValues = values;
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
			LuaValue key = key(oldKeys, oldValues, i, true);
			LuaValue value = value(oldValues, i, true);
			if (!key.isNil() && !value.isNil()) rawsetImpl(key, value);
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
			int i = keys.length;
			while (--i >= 0) {
				LuaValue key = key(keys, values, i, true);
				LuaValue value = value(values, i, true);
				if (!value.isNil()) {
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
		if (keys.length == 0) return -1;
		while (lastFree >= 0) {
			if (keys[lastFree--] == NIL) {
				return lastFree + 1;
			}
		}

		return -1;
	}

	/**
	 * Get the current key, converting it to a strong reference if required. If it is nil then it clears the key and
	 * value (marking it as "dead").
	 *
	 * @return The entry's key.
	 */
	private LuaValue key(int slot) {
		return key(keys, values, slot, weakKeys);
	}

	private static LuaValue key(Object[] keys, Object[] values, int slot, boolean weak) {
		assert keys.length == values.length;
		Object key = keys[slot];
		if (key == NIL || !weak) return (LuaValue) key;

		LuaValue strengthened = strengthen(key);
		if (strengthened.isNil()) values[slot] = NIL; // We preserve the key so we can check it is nil

		return strengthened;
	}

	private LuaValue value(int slot) {
		return value(values, slot, weakValues);
	}

	/**
	 * Get the current value, converting it to a strong reference if required.
	 *
	 * @return The entry's value.
	 */
	private static LuaValue value(Object[] values, int slot, boolean weak) {
		Object value = values[slot];
		if (value == NIL || !weak) return (LuaValue) value;

		LuaValue strengthened = strengthen(value);
		if (strengthened.isNil()) values[slot] = NIL;
		return strengthened;
	}

	/**
	 * Set the value of a node. This is the inverse of {@link #value(int)}.
	 *
	 * @param slot  The node slot.
	 * @param value The new value.
	 */
	private void setNodeValue(int slot, LuaValue value) {
		values[slot] = weakValues ? weaken(value) : value;
		metatableFlags = 0;
	}

	/**
	 * Insert a new key into a hash table.
	 * <p>
	 * First check whether key's main position is free. If not, check whether the colliding node is in its main position
	 * or not. If it is not, move colliding node to an empty place and put new key in its main position, otherwise the
	 * colliding node is in its main position and the new key goes to an empty position.
	 *
	 * @param key The key to set
	 * @throws IllegalArgumentException If this key cannot be used.
	 */
	private int newKey(LuaValue key) {
		if (key.isNil()) throw new IllegalArgumentException("table index is nil");

		// Rehash and let the rawgetter handle it
		if (keys.length == 0) {
			rehash(key, false);
			return -1;
		}

		int mainNode = hashSlot(key);
		LuaValue mainKey = key(mainNode);
		if (!mainKey.isNil() && !value(mainNode).isNil()) {
			// If we've got a collision then
			final int freeNode = getFreePos();

			if (freeNode < 0) {
				rehash(key, false);
				return -1;
			}

			int otherNode = hashSlot(mainKey);

			if (otherNode != mainNode) {
				// If the colliding position isn't at its main position then we move it to a free position

				// Walk the chain to find the node just before the desired one
				while (next[otherNode] != mainNode) otherNode = next[otherNode];

				// Rechain other to point to the free position
				next[otherNode] = freeNode;

				// Copy colliding node into free position
				keys[freeNode] = keys[mainNode];
				values[freeNode] = values[mainNode];
				next[freeNode] = next[mainNode];

				// Clear main node
				next[mainNode] = -1;
				keys[mainNode] = NIL;
				values[mainNode] = NIL;
			} else {
				// Colliding node is in the main position so we will assign to a free position.

				if (next[mainNode] != -1) {
					// We're inserting "after" the first node in the linked list so change the
					// next node.
					next[freeNode] = next[mainNode];
				} else {
					assert next[freeNode] == -1;
				}

				// Insert after the main node
				next[mainNode] = freeNode;

				mainNode = freeNode;
			}
		}

		keys[mainNode] = weakKeys ? weaken(key) : key;

		return mainNode;
	}

	private int getNode(int search) {
		if (keys.length == 0) return -1;

		int node = hashmod(search, keys.length - 1);
		while (true) {
			LuaValue key = key(node);
			if (key instanceof LuaInteger keyI && keyI.intValue() == search) {
				return node;
			} else {
				node = next[node];
				if (node == -1) return -1;
			}
		}
	}

	private int getNode(LuaValue search) {
		if (keys.length == 0 || search == NIL) return -1;

		int node = hashSlot(search);
		while (true) {
			LuaValue key = key(node);
			if (key.equals(search)) {
				return node;
			} else {
				node = next[node];
				if (node == -1) return -1;
			}
		}
	}

	public LuaValue rawget(int search) {
		if (search > 0 && search <= array.length) {
			return strengthen(array[search - 1]);
		} else if (keys.length == 0) {
			return NIL;
		} else {
			int node = getNode(search);
			return node == -1 ? NIL : value(node);
		}
	}

	public LuaValue rawget(LuaValue search) {
		if (search instanceof LuaInteger i) return rawget(i.intValue());

		int node = getNode(search);
		return node == -1 ? NIL : value(node);
	}

	public LuaValue rawget(CachedMetamethod search) {
		int flag = 1 << search.ordinal();
		if ((metatableFlags & flag) != 0) return NIL;

		int node = getNode(search.getKey());
		if (node != -1) {
			LuaValue value = value(node);
			if (!value.isNil()) return value;
		}

		metatableFlags |= flag;
		return NIL;
	}

	/**
	 * Check if this table has a {@code __newindex} metamethod.
	 *
	 * @return Whether this method has a {@code __newindex} metamethod.
	 */
	private boolean hasNewIndex() {
		LuaTable metatable = this.metatable;
		return metatable != null && metatable.rawget(CachedMetamethod.NEWINDEX) != NIL;
	}

	/**
	 * Set a key in this table if the key is already present or if there is no metamethod.
	 *
	 * @param key   The key to set.
	 * @param value The value to set.
	 * @return {@code true} if the table was updated. If {@code false}, the table's metamethod should be invoked.
	 * @see OperationHelper#setTable(LuaState, LuaValue, int, LuaValue)
	 */
	boolean trySet(int key, LuaValue value) {
		return trySet(key, value, null);
	}

	private boolean trySet(int key, LuaValue value, LuaValue keyValue) {
		if (key > 0 && key <= array.length) {
			// If value is absent and we've got a __newindex method, don't insert.
			if (strengthen(array[key - 1]) == NIL && hasNewIndex()) return false;
			array[key - 1] = weakValues ? weaken(value) : value;
			return true;
		}

		int node = getNode(key);
		if (node == -1) {
			if (hasNewIndex()) return false;
		} else {
			if (value(node) == NIL && hasNewIndex()) return false;
			setNodeValue(node, value);
			return true;
		}

		assert !hasNewIndex();
		rawset(key, value, keyValue);
		return true;
	}

	/**
	 * Set a key in this table if the key is already present or if there is no metamethod.
	 *
	 * @param key   The key to set.
	 * @param value The value to set.
	 * @return {@code true} if the table was updated. If {@code false}, the table's metamethod should be invoked.
	 * @see OperationHelper#setTable(LuaState, LuaValue, LuaValue, LuaValue)
	 */
	boolean trySet(LuaValue key, LuaValue value) throws LuaError {
		if (key instanceof LuaInteger keyI) return trySet(keyI.intValue(), value, key);

		int node = getNode(key);
		if (node == -1) {
			if (hasNewIndex()) return false;
		} else {
			if (value(node) == NIL && hasNewIndex()) return false;
			setNodeValue(node, value);
			return true;
		}

		assert !hasNewIndex();
		rawset(key, value);
		return true;
	}

	public void rawset(int key, LuaValue value) {
		rawset(key, value, null);
	}

	private void rawset(int key, LuaValue value, LuaValue valueOf) {
		do {
			if (key > 0 && key <= array.length) {
				array[key - 1] = weakValues ? weaken(value) : value;
				return;
			}

			int node = getNode(key);
			if (node == -1) {
				if (valueOf == null) valueOf = valueOf(key);
				node = newKey(valueOf);
			}

			// newKey will have handled this otherwise
			if (node != -1) {
				setNodeValue(node, value);
				return;
			}
		} while (true);
	}

	public void rawset(LuaValue key, LuaValue value) throws LuaError {
		if (key.isNil()) throw new LuaError("table index is nil");
		if (key instanceof LuaDouble d && Double.isNaN(d.doubleValue())) throw new LuaError("table index is NaN");
		rawsetImpl(key, value);
	}

	public void rawsetImpl(LuaValue key, LuaValue value) {
		if (key instanceof LuaInteger keyI) {
			rawset(keyI.intValue(), value, key);
			return;
		}

		do {
			int node = getNode(key);
			if (node == -1) node = newKey(key);

			// newKey will have handled this otherwise
			if (node != -1) {
				setNodeValue(node, value);
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
		return switch (value.type()) {
			case TFUNCTION, TTHREAD, TTABLE -> new WeakReference<>(value);
			case TUSERDATA -> new WeakUserdata((LuaUserdata) value);
			default -> value;
		};
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

		LuaValue strongValue() {
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
}
