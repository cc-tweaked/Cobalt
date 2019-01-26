/**
 * ****************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
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
package org.squiddev.cobalt.table;

import org.junit.Before;
import org.junit.Test;
import org.squiddev.cobalt.*;

import java.util.ArrayList;
import java.util.Vector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.*;
import static org.squiddev.cobalt.Matchers.between;
import static org.squiddev.cobalt.ValueFactory.valueOf;

public class TableTest {
	private LuaState state;

	@Before
	public void setup() {
		state = new LuaState();
	}

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
	public void testInOrderIntegerKeyInsertion() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.set(state, i, valueOf("Test Value! " + i));
		}

		// Ensure all keys are still there.
		for (int i = 1; i <= 32; ++i) {
			try {
				assertEquals("Test Value! " + i, t.get(state, i).toString());
			} catch (LuaError luaError) {
				luaError.printStackTrace();
			}
		}

		// Ensure capacities make sense
		assertEquals(0, t.getHashLength());
		assertThat(t.getArrayLength(), between(32, 64));
	}

	@Test
	public void testRekeyCount() throws LuaError {
		LuaTable t = new LuaTable();

		// NOTE: This order of insertion is important.
		t.set(state, 3, LuaInteger.valueOf(3));
		t.set(state, 1, LuaInteger.valueOf(1));
		t.set(state, 5, LuaInteger.valueOf(5));
		t.set(state, 4, LuaInteger.valueOf(4));
		t.set(state, 6, LuaInteger.valueOf(6));
		t.set(state, 2, LuaInteger.valueOf(2));

		for (int i = 1; i < 6; ++i) {
			assertEquals(LuaInteger.valueOf(i), t.get(state, i));
		}

		assertThat(t.getArrayLength(), between(3, 12));
		assertThat(t.getHashLength(), lessThanOrEqualTo(3));
	}

	@Test
	public void testOutOfOrderIntegerKeyInsertion() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 32; i > 0; --i) {
			t.set(state, i, valueOf("Test Value! " + i));
		}

		// Ensure all keys are still there.
		for (int i = 1; i <= 32; ++i) {
			assertEquals("Test Value! " + i, t.get(state, i).toString());
		}

		// Ensure capacities make sense
		assertEquals(32, t.getArrayLength());
		assertEquals(0, t.getHashLength());

	}

	@Test
	public void testStringAndIntegerKeys() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 0; i < 10; ++i) {
			LuaString str = valueOf(String.valueOf(i));
			t.set(state, i, str);
			t.set(state, str, LuaInteger.valueOf(i));
		}

		assertThat(t.getArrayLength(), between(8, 18)); // 1, 2, ..., 9
		assertThat(t.getHashLength(), between(11, 33)); // 0, "0", "1", ..., "9"

		LuaValue[] keys = keys(t);

		int intKeys = 0;
		int stringKeys = 0;

		assertEquals(20, keys.length);
		for (LuaValue k : keys) {
			if (k instanceof LuaInteger) {
				final int ik = k.toInteger();
				assertTrue(ik >= 0 && ik < 10);
				final int mask = 1 << ik;
				assertTrue((intKeys & mask) == 0);
				intKeys |= mask;
			} else if (k instanceof LuaString) {
				final int ik = Integer.parseInt(k.strvalue().toString());
				assertEquals(String.valueOf(ik), k.strvalue().toString());
				assertTrue(ik >= 0 && ik < 10);
				final int mask = 1 << ik;
				assertTrue("Key \"" + ik + "\" found more than once", (stringKeys & mask) == 0);
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

		t.set(state, "test", valueOf("foo"));
		t.set(state, "explode", valueOf("explode"));
		assertEquals(2, keyCount(t));
	}

	@Test
	public void testRemove0() throws LuaError {
		LuaTable t = new LuaTable(2, 0);

		t.set(state, 1, valueOf("foo"));
		t.set(state, 2, valueOf("bah"));
		assertNotSame(Constants.NIL, t.get(state, 1));
		assertNotSame(Constants.NIL, t.get(state, 2));
		assertEquals(Constants.NIL, t.get(state, 3));

		t.set(state, 1, Constants.NIL);
		t.set(state, 2, Constants.NIL);
		t.set(state, 3, Constants.NIL);
		assertEquals(Constants.NIL, t.get(state, 1));
		assertEquals(Constants.NIL, t.get(state, 2));
		assertEquals(Constants.NIL, t.get(state, 3));
	}

	@Test
	public void testRemove1() throws LuaError {
		LuaTable t = new LuaTable(0, 1);

		t.set(state, "test", valueOf("foo"));
		t.set(state, "explode", Constants.NIL);
		t.set(state, 42, Constants.NIL);
		t.set(state, new LuaTable(), Constants.NIL);
		t.set(state, "test", Constants.NIL);
		assertEquals(0, keyCount(t));

		t.set(state, 10, LuaInteger.valueOf(5));
		t.set(state, 10, Constants.NIL);
		assertEquals(0, keyCount(t));
	}

	@Test
	public void testRemove2() throws LuaError {
		LuaTable t = new LuaTable(0, 1);

		t.set(state, "test", valueOf("foo"));
		t.set(state, "string", LuaInteger.valueOf(10));
		assertEquals(2, keyCount(t));

		t.set(state, "string", Constants.NIL);
		t.set(state, "three", valueOf(3.14));
		assertEquals(2, keyCount(t));

		t.set(state, "test", Constants.NIL);
		assertEquals(1, keyCount(t));

		t.set(state, 10, LuaInteger.valueOf(5));
		assertEquals(2, keyCount(t));

		t.set(state, 10, Constants.NIL);
		assertEquals(1, keyCount(t));

		t.set(state, "three", Constants.NIL);
		assertEquals(0, keyCount(t));
	}

	@Test
	public void testInOrderLuaLength() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.set(state, i, valueOf("Test Value! " + i));
			assertEquals(i, OperationHelper.length(state, t).toInteger());
			assertEquals(i, t.maxn(), 1e-10);
		}
	}

	@Test
	public void testOutOfOrderLuaLength() throws LuaError {
		LuaTable t = new LuaTable();

		for (int j = 8; j < 32; j += 8) {
			for (int i = j; i > 0; --i) {
				t.set(state, i, valueOf("Test Value! " + i));
			}
			assertEquals(j, OperationHelper.length(state, t).toInteger());
			assertEquals(j, t.maxn(), 1e-10);
		}
	}

	@Test
	public void testStringKeysLuaLength() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.set(state, "str-" + i, valueOf("String Key Test Value! " + i));
			assertEquals(0, OperationHelper.length(state, t).toInteger());
			assertEquals(0, t.maxn(), 1e-10);
		}
	}

	@Test
	public void testMixedKeysLuaLength() throws LuaError {
		LuaTable t = new LuaTable();

		for (int i = 1; i <= 32; ++i) {
			t.set(state, "str-" + i, valueOf("String Key Test Value! " + i));
			t.set(state, i, valueOf("Int Key Test Value! " + i));
			assertEquals(i, OperationHelper.length(state, t).toInteger());
			assertEquals(i, t.maxn(), 1e-10);
		}
	}

	private void compareLists(LuaTable t, Vector<LuaString> v) throws LuaError {
		int n = v.size();
		assertEquals(v.size(), OperationHelper.length(state, t).toInteger());
		for (int j = 0; j < n; j++) {
			Object vj = v.elementAt(j);
			Object tj = t.get(state, j + 1).toString();
			vj = vj.toString();
			assertEquals(vj, tj);
		}
	}

	@Test
	public void testInsertBeginningOfList() throws LuaError {
		LuaTable t = new LuaTable();
		Vector<LuaString> v = new Vector<>();

		for (int i = 1; i <= 32; ++i) {
			LuaString test = valueOf("Test Value! " + i);
			t.insert(1, test);
			v.insertElementAt(test, 0);
			compareLists(t, v);
		}
	}

	@Test
	public void testInsertEndOfList() throws LuaError {
		LuaTable t = new LuaTable();
		Vector<LuaString> v = new Vector<>();

		for (int i = 1; i <= 32; ++i) {
			LuaString test = valueOf("Test Value! " + i);
			t.insert(0, test);
			v.insertElementAt(test, v.size());
			compareLists(t, v);
		}
	}

	@Test
	public void testInsertMiddleOfList() throws LuaError {
		LuaTable t = new LuaTable();
		Vector<LuaString> v = new Vector<>();

		for (int i = 1; i <= 32; ++i) {
			LuaString test = valueOf("Test Value! " + i);
			int m = i / 2;
			t.insert(m + 1, test);
			v.insertElementAt(test, m);
			compareLists(t, v);
		}
	}

	private static void prefillLists(LuaTable t, Vector<LuaString> v) {
		for (int i = 1; i <= 32; ++i) {
			LuaString test = valueOf("Test Value! " + i);
			t.insert(0, test);
			v.insertElementAt(test, v.size());
		}
	}

	@Test
	public void testRemoveBeginningOfList() throws LuaError {
		LuaTable t = new LuaTable();
		Vector<LuaString> v = new Vector<>();
		prefillLists(t, v);
		for (int i = 1; i <= 32; ++i) {
			t.remove(1);
			v.removeElementAt(0);
			compareLists(t, v);
		}
	}

	@Test
	public void testRemoveEndOfList() throws LuaError {
		LuaTable t = new LuaTable();
		Vector<LuaString> v = new Vector<>();
		prefillLists(t, v);
		for (int i = 1; i <= 32; ++i) {
			t.remove(0);
			v.removeElementAt(v.size() - 1);
			compareLists(t, v);
		}
	}

	@Test
	public void testRemoveMiddleOfList() throws LuaError {
		LuaTable t = new LuaTable();
		Vector<LuaString> v = new Vector<>();
		prefillLists(t, v);
		for (int i = 1; i <= 32; ++i) {
			int m = v.size() / 2;
			t.remove(m + 1);
			v.removeElementAt(m);
			compareLists(t, v);
		}
	}

}
