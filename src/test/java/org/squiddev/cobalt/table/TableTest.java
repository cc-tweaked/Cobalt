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
package org.squiddev.cobalt.table;

import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.squiddev.cobalt.Matchers.between;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.table.TableOperations.getArrayLength;
import static org.squiddev.cobalt.table.TableOperations.getHashLength;

public class TableTest {
	private int keyCount(LuaTable t) throws LuaError {
		return keys(t).length;
	}

	private LuaValue[] keys(LuaTable t) throws LuaError {
		ArrayList<LuaValue> l = new ArrayList<>();
		LuaValue k = Constants.NIL;
		while (true) {
			Varargs n = t.next(k);
			if ((k = n.first()).isNil()) {
				break;
			}
			l.add(k);
		}
		return l.toArray(new LuaValue[t.length()]);
	}


	@Test
	public void testInOrderIntegerKeyInsertion() {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.rawset(i, valueOf("Test Value! " + i));
		}

		// Ensure all keys are still there.
		for (int i = 1; i <= 32; ++i) {
			assertEquals("Test Value! " + i, t.rawget(valueOf(i)).toString());
		}

		// Ensure capacities make sense
		assertEquals(0, getHashLength(t));
		assertThat(getArrayLength(t), between(32, 64));
	}

	@Test
	public void testRekeyCount() {
		LuaTable t = new LuaTable();

		// NOTE: This order of insertion is important.
		t.rawset(3, LuaInteger.valueOf(3));
		t.rawset(1, LuaInteger.valueOf(1));
		t.rawset(5, LuaInteger.valueOf(5));
		t.rawset(4, LuaInteger.valueOf(4));
		t.rawset(6, LuaInteger.valueOf(6));
		t.rawset(2, LuaInteger.valueOf(2));

		for (int i = 1; i < 6; ++i) {
			assertEquals(LuaInteger.valueOf(i), t.rawget(valueOf(i)));
		}

		assertThat(getArrayLength(t), between(3, 12));
		assertThat(getHashLength(t), lessThanOrEqualTo(3));
	}

	@Test
	public void testOutOfOrderIntegerKeyInsertion() {
		LuaTable t = new LuaTable();

		for (int i = 32; i > 0; --i) {
			t.rawset(i, valueOf("Test Value! " + i));
		}

		// Ensure all keys are still there.
		for (int i = 1; i <= 32; ++i) {
			assertEquals("Test Value! " + i, t.rawget(i).toString());
		}

		// Ensure capacities make sense
		assertEquals(32, getArrayLength(t));
		assertEquals(0, getHashLength(t));

	}

	@Test
	public void testStringAndIntegerKeys() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 0; i < 10; ++i) {
			LuaString str = valueOf(String.valueOf(i));
			t.rawset(i, str);
			t.rawset(str, LuaInteger.valueOf(i));
		}

		assertThat(getArrayLength(t), between(8, 18)); // 1, 2, ..., 9
		assertThat(getHashLength(t), between(11, 33)); // 0, "0", "1", ..., "9"

		LuaValue[] keys = keys(t);

		int intKeys = 0;
		int stringKeys = 0;

		assertEquals(20, keys.length);
		for (LuaValue k : keys) {
			if (k instanceof LuaInteger) {
				final int ik = k.toInteger();
				assertTrue(ik >= 0 && ik < 10);
				final int mask = 1 << ik;
				assertEquals(0, intKeys & mask);
				intKeys |= mask;
			} else if (k instanceof LuaString) {
				final int ik = Integer.parseInt(k.checkLuaString().toString());
				assertEquals(String.valueOf(ik), k.checkLuaString().toString());
				assertTrue(ik >= 0 && ik < 10);
				final int mask = 1 << ik;
				assertEquals(0, stringKeys & mask, "Key \"" + ik + "\" found more than once");
				stringKeys |= mask;
			} else {
				fail("Unexpected type of key found");
			}
		}

		assertEquals(0x03FF, intKeys);
		assertEquals(0x03FF, stringKeys);
	}

	@Test
	public void testBadInitialCapacity() throws LuaError {
		LuaTable t = new LuaTable(0, 1);

		t.rawset("test", valueOf("foo"));
		t.rawset("explode", valueOf("explode"));
		assertEquals(2, keyCount(t));
	}

	@Test
	public void testRemove0() {
		LuaTable t = new LuaTable(2, 0);

		t.rawset(1, valueOf("foo"));
		t.rawset(2, valueOf("bah"));
		assertNotSame(Constants.NIL, t.rawget(1));
		assertNotSame(Constants.NIL, t.rawget(2));
		assertEquals(Constants.NIL, t.rawget(3));

		t.rawset(1, Constants.NIL);
		t.rawset(2, Constants.NIL);
		t.rawset(3, Constants.NIL);
		assertEquals(Constants.NIL, t.rawget(1));
		assertEquals(Constants.NIL, t.rawget(2));
		assertEquals(Constants.NIL, t.rawget(3));
	}

	@Test
	public void testRemove1() throws LuaError {
		LuaTable t = new LuaTable(0, 1);

		t.rawset("test", valueOf("foo"));
		t.rawset("explode", Constants.NIL);
		t.rawset(42, Constants.NIL);
		t.rawset(new LuaTable(), Constants.NIL);
		t.rawset("test", Constants.NIL);
		assertEquals(0, keyCount(t));

		t.rawset(10, LuaInteger.valueOf(5));
		t.rawset(10, Constants.NIL);
		assertEquals(0, keyCount(t));
	}

	@Test
	public void testRemove2() throws LuaError {
		LuaTable t = new LuaTable(0, 1);

		t.rawset("test", valueOf("foo"));
		t.rawset("string", LuaInteger.valueOf(10));
		assertEquals(2, keyCount(t));

		t.rawset("string", Constants.NIL);
		t.rawset("three", valueOf(3.14));
		assertEquals(2, keyCount(t));

		t.rawset("test", Constants.NIL);
		assertEquals(1, keyCount(t));

		t.rawset(10, LuaInteger.valueOf(5));
		assertEquals(2, keyCount(t));

		t.rawset(10, Constants.NIL);
		assertEquals(1, keyCount(t));

		t.rawset("three", Constants.NIL);
		assertEquals(0, keyCount(t));
	}

	@Test
	public void testInOrderLuaLength() {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.rawset(i, valueOf("Test Value! " + i));
			assertEquals(i, t.length());
		}
	}

	@Test
	public void testOutOfOrderLuaLength() {
		LuaTable t = new LuaTable();

		for (int j = 8; j < 32; j += 8) {
			for (int i = j; i > 0; --i) {
				t.rawset(i, valueOf("Test Value! " + i));
			}
			assertEquals(j, t.length());
		}
	}

	@Test
	public void testStringKeysLuaLength() {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.rawset("str-" + i, valueOf("String Key Test Value! " + i));
			assertEquals(0, t.length());
		}
	}

	@Test
	public void testMixedKeysLuaLength() {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.rawset("str-" + i, valueOf("String Key Test Value! " + i));
			t.rawset(i, valueOf("Int Key Test Value! " + i));
			assertEquals(i, t.length());
		}
	}

	@Test
	public void testPresizeRehashes() throws LuaError {
		// Set presize correctly moves items from the hash part to the array part.
		// See https://github.com/SquidDev/Cobalt/pull/61.
		LuaTable t = new LuaTable();
		t.rawset(7, valueOf(123));
		t.presize(8);

		assertEquals(Constants.NIL, t.next(valueOf(7)));
	}

	@Test
	public void testCachedMetamethod() throws LuaError {
		var t = new LuaTable();
		var f = LibFunction.create(s -> Constants.NIL);

		// The field is absent initially.
		assertEquals(Constants.NIL, t.rawget(CachedMetamethod.INDEX));

		// After setting the field, it is now present.
		TableOperations.setValue(t, CachedMetamethod.INDEX.getKey(), f);
		assertEquals(f, t.rawget(CachedMetamethod.INDEX));

		// If we clear it, the field is no longer there.
		TableOperations.setValue(t, CachedMetamethod.INDEX.getKey(), Constants.NIL);
		assertEquals(Constants.NIL, t.rawget(CachedMetamethod.INDEX));

		// HOWEVER, the node *is* still present. So setting it again will not create a new node. Ensure that we still
		// reset the cache.
		TableOperations.setValue(t, CachedMetamethod.INDEX.getKey(), f);
		assertEquals(f, t.rawget(CachedMetamethod.INDEX));
	}
}
