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

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.squiddev.cobalt.Matchers.between;
import static org.squiddev.cobalt.table.TableOperations.*;

/**
 * Tests for tables used as lists.
 */
public class TableArrayTest {
	private final LuaState state = new LuaState();

	@Test
	public void testInOrderIntegerKeyInsertion() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.rawset(ValueFactory.valueOf(i), LuaString.valueOf("Test Value! " + i));
		}

		// Ensure all keys are still there.
		for (int i = 1; i <= 32; ++i) {
			assertEquals("Test Value! " + i, t.rawget(ValueFactory.valueOf(i)).toString());
		}

		// Ensure capacities make sense
		assertEquals(0, getHashLength(t));

		assertThat(getArrayLength(t), between(32, 64));
	}

	@Test
	public void testResize() throws LuaError {
		LuaTable t = new LuaTable();

		// NOTE: This order of insertion is important.
		t.rawset(ValueFactory.valueOf(3), LuaInteger.valueOf(3));
		t.rawset(ValueFactory.valueOf(1), LuaInteger.valueOf(1));
		t.rawset(ValueFactory.valueOf(5), LuaInteger.valueOf(5));
		t.rawset(ValueFactory.valueOf(4), LuaInteger.valueOf(4));
		t.rawset(ValueFactory.valueOf(6), LuaInteger.valueOf(6));
		t.rawset(ValueFactory.valueOf(2), LuaInteger.valueOf(2));

		for (int i = 1; i < 6; ++i) {
			assertEquals(LuaInteger.valueOf(i), t.rawget(ValueFactory.valueOf(i)));
		}

		assertThat(getArrayLength(t), between(3, 12));
		assertThat(getHashLength(t), lessThanOrEqualTo(3));
	}

	@Test
	public void testOutOfOrderIntegerKeyInsertion() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 32; i > 0; --i) {
			t.rawset(ValueFactory.valueOf(i), LuaString.valueOf("Test Value! " + i));
		}

		// Ensure all keys are still there.
		for (int i = 1; i <= 32; ++i) {
			assertEquals("Test Value! " + i, t.rawget(ValueFactory.valueOf(i)).toString());
		}

		// Ensure capacities make sense
		assertEquals(32, getArrayLength(t));
		assertEquals(0, getHashLength(t));

	}

	@Test
	public void testStringAndIntegerKeys() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 0; i < 10; ++i) {
			LuaString str = LuaString.valueOf(String.valueOf(i));
			t.rawset(ValueFactory.valueOf(i), str);
			t.rawset(str, LuaInteger.valueOf(i));
		}

		assertThat(getArrayLength(t), between(8, 18)); // 1, 2, ..., 9
		assertThat(getHashLength(t), between(11, 33)); // 0, "0", "1", ..., "9"

		Collection<LuaValue> keys = keys(t);

		int intKeys = 0;
		int stringKeys = 0;

		assertEquals(20, keys.size());
		for (LuaValue k : keys) {
			if (k instanceof LuaInteger) {
				final int ik = k.toInteger();
				assertTrue(ik >= 0 && ik < 10);
				final int mask = 1 << ik;
				assertEquals(0, (intKeys & mask));
				intKeys |= mask;
			} else if (k instanceof LuaString) {
				final int ik = Integer.parseInt(k.toString());
				assertEquals(String.valueOf(ik), k.toString());
				assertTrue(ik >= 0 && ik < 10);
				final int mask = 1 << ik;
				assertEquals(0, (stringKeys & mask), "Key \"" + ik + "\" found more than once");
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

		t.rawset(ValueFactory.valueOf("test"), LuaString.valueOf("foo"));
		t.rawset(ValueFactory.valueOf("explode"), LuaString.valueOf("explode"));
		assertEquals(2, t.size());
	}

	@Test
	public void testRemove0() throws LuaError {
		LuaTable t = new LuaTable(2, 0);

		t.rawset(ValueFactory.valueOf(1), LuaString.valueOf("foo"));
		t.rawset(ValueFactory.valueOf(2), LuaString.valueOf("bah"));
		assertNotSame(Constants.NIL, t.rawget(ValueFactory.valueOf(1)));
		assertNotSame(Constants.NIL, t.rawget(ValueFactory.valueOf(2)));
		assertEquals(Constants.NIL, t.rawget(ValueFactory.valueOf(3)));

		t.rawset(ValueFactory.valueOf(1), Constants.NIL);
		t.rawset(ValueFactory.valueOf(2), Constants.NIL);
		t.rawset(ValueFactory.valueOf(3), Constants.NIL);
		assertEquals(Constants.NIL, t.rawget(ValueFactory.valueOf(1)));
		assertEquals(Constants.NIL, t.rawget(ValueFactory.valueOf(2)));
		assertEquals(Constants.NIL, t.rawget(ValueFactory.valueOf(3)));
	}

	@Test
	public void testRemove1() throws LuaError {
		LuaTable t = new LuaTable(0, 1);

		assertEquals(0, t.size());

		t.rawset(ValueFactory.valueOf("test"), LuaString.valueOf("foo"));
		assertEquals(1, t.size());
		t.rawset(ValueFactory.valueOf("explode"), Constants.NIL);
		assertEquals(1, t.size());
		t.rawset(ValueFactory.valueOf(42), Constants.NIL);
		assertEquals(1, t.size());
		t.rawset(new LuaTable(), Constants.NIL);
		assertEquals(1, t.size());
		t.rawset(ValueFactory.valueOf("test"), Constants.NIL);
		assertEquals(0, t.size());

		t.rawset(ValueFactory.valueOf(10), LuaInteger.valueOf(5));
		t.rawset(ValueFactory.valueOf(10), Constants.NIL);
		assertEquals(0, t.size());
	}

	@Test
	public void testRemove2() throws LuaError {
		LuaTable t = new LuaTable(0, 1);

		t.rawset(ValueFactory.valueOf("test"), LuaString.valueOf("foo"));
		t.rawset(ValueFactory.valueOf("string"), LuaInteger.valueOf(10));
		assertEquals(2, t.size());

		t.rawset(ValueFactory.valueOf("string"), Constants.NIL);
		t.rawset(ValueFactory.valueOf("three"), LuaDouble.valueOf(3.14));
		assertEquals(2, t.size());

		t.rawset(ValueFactory.valueOf("test"), Constants.NIL);
		assertEquals(1, t.size());

		t.rawset(ValueFactory.valueOf(10), LuaInteger.valueOf(5));
		assertEquals(2, t.size());

		t.rawset(ValueFactory.valueOf(10), Constants.NIL);
		assertEquals(1, t.size());

		t.rawset(ValueFactory.valueOf("three"), Constants.NIL);
		assertEquals(0, t.size());
	}

	@Test
	public void testInOrderlen() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			LuaValue v = LuaString.valueOf("Test Value! " + i);
			t.rawset(ValueFactory.valueOf(i), v);
			assertEquals(i, t.length());
		}
	}

	@Test
	public void testOutOfOrderlen() throws LuaError {
		LuaTable t = new LuaTable();

		for (int j = 8; j < 32; j += 8) {
			for (int i = j; i > 0; --i) {
				t.rawset(ValueFactory.valueOf(i), LuaString.valueOf("Test Value! " + i));
			}
			assertEquals(j, t.length());
		}
	}

	@Test
	public void testStringKeyslen() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.rawset(ValueFactory.valueOf("str-" + i), LuaString.valueOf("String Key Test Value! " + i));
			assertEquals(0, t.length());
		}
	}

	@Test
	public void testMixedKeyslen() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.rawset(ValueFactory.valueOf("str-" + i), LuaString.valueOf("String Key Test Value! " + i));
			t.rawset(ValueFactory.valueOf(i), LuaString.valueOf("Int Key Test Value! " + i));
			assertEquals(i, t.length());
		}
	}
}
