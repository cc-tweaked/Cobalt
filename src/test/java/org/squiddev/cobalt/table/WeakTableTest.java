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

import java.lang.ref.WeakReference;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;
import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.ValueFactory.userdataOf;

public abstract class WeakTableTest {
	protected LuaState state;

	@Before
	public void setup() throws Exception {
		state = new LuaState();
	}

	public static class MyData {
		public final int value;

		public MyData(int value) {
			this.value = value;
		}

		public int hashCode() {
			return value;
		}

		public boolean equals(Object o) {
			return (o instanceof MyData) && ((MyData) o).value == value;
		}

		public String toString() {
			return "mydata-" + value;
		}
	}

	private static void collectGarbage() {
		Runtime rt = Runtime.getRuntime();
		rt.gc();
		try {
			Thread.sleep(20);
			rt.gc();
			Thread.sleep(20);
		} catch (Exception e) {
			e.printStackTrace();
		}
		rt.gc();
	}

	public static class WeakValueTableTest extends WeakTableTest {
		protected LuaTable new_Table() {
			return ValueFactory.weakTable(false, true);
		}

		@Test
		public void testWeakValuesTable() throws LuaError {
			LuaTable t = new_Table();

			Object obj = new Object();
			LuaTable tableValue = new LuaTable();
			LuaTable tableValue2 = new LuaTable();
			LuaString stringValue = LuaString.valueOf("this is a test");

			t.set(state, "table", tableValue);
			t.set(state, "userdata", userdataOf(obj, null));
			t.set(state, "string", stringValue);
			t.set(state, "string2", LuaString.valueOf("another string"));
			t.set(state, 1, tableValue2);
			assertThat("table must have at least 4 elements", t.getHashLength(), greaterThanOrEqualTo(4));
			assertThat("array part must have 1 element", t.getArrayLength(), greaterThanOrEqualTo(1));

			// check that table can be used to get elements
			assertEquals(tableValue, t.get(state, "table"));
			assertEquals(stringValue, t.get(state, "string"));
			assertEquals(obj, t.get(state, "userdata").checkUserdata());

			// nothing should be collected, since we have strong references here
			collectGarbage();

			// check that elements are still there
			assertEquals(tableValue, t.get(state, "table"));
			assertEquals(stringValue, t.get(state, "string"));
			assertEquals(obj, t.get(state, "userdata").checkUserdata());

			// drop our strong references
			obj = null;
			tableValue = null;
			stringValue = null;

			// Garbage collection should cause weak entries to be dropped.
			collectGarbage();

			// check that they are dropped
			assertEquals(NIL, t.get(state, "table"));
			assertEquals(NIL, t.get(state, "userdata"));
			assertFalse("strings should not be in weak references", t.get(state, "string").isNil());
		}

		@Test
		public void testChangeMode() throws LuaError {
			LuaTable table = new LuaTable();

			table.set(state, 1, new LuaTable());
			assertThat(table.get(state, 1), instanceOf(LuaTable.class));

			table.useWeak(false, true);
			collectGarbage();
			assertThat(table.get(state, 1), instanceOf(LuaNil.class));
		}
	}

	public static class WeakKeyTableTest extends WeakTableTest {
		@Test
		public void testWeakKeysTable() throws LuaError {
			LuaTable t = ValueFactory.weakTable(true, false);

			LuaValue key = userdataOf(new MyData(111));
			LuaValue val = userdataOf(new MyData(222));

			// set up the table
			t.set(state, key, val);
			assertEquals(val, t.get(state, key));
			System.gc();
			assertEquals(val, t.get(state, key));

			// drop key and value references, replace them with new ones
			WeakReference<LuaValue> origkey = new WeakReference<>(key);
			WeakReference<LuaValue> origval = new WeakReference<>(val);
			key = userdataOf(new MyData(111));
			val = userdataOf(new MyData(222));

			// new key and value should be interchangeable (feature of this test class)
			assertEquals(key, origkey.get());
			assertEquals(val, origval.get());
			assertEquals(val, t.get(state, key));
			assertEquals(val, t.get(state, origkey.get()));
			assertEquals(origval.get(), t.get(state, key));

			// value should not be reachable after gc
			collectGarbage();
			assertEquals(null, origkey.get());
			assertEquals(NIL, t.get(state, key));
			collectGarbage();
			assertEquals(null, origval.get());
		}

		@Test
		public void testNext() throws LuaError {
			LuaTable t = ValueFactory.weakTable(true, true);

			LuaValue key = userdataOf(new MyData(111));
			LuaValue val = userdataOf(new MyData(222));
			LuaValue key2 = userdataOf(new MyData(333));
			LuaValue val2 = userdataOf(new MyData(444));
			LuaValue key3 = userdataOf(new MyData(555));
			LuaValue val3 = userdataOf(new MyData(666));

			// set up the table
			t.set(state, key, val);
			t.set(state, key2, val2);
			t.set(state, key3, val3);

			// forget one of the keys
			key2 = null;
			val2 = null;
			collectGarbage();

			// table should have 2 entries
			int size = 0;
			for (LuaValue k = t.next(NIL).first(); !k.isNil();
				 k = t.next(k).first()) {
				size++;
			}
			assertEquals(2, size);
		}
	}

	public static class WeakKeyValueTableTest extends WeakTableTest {
		@Test
		public void testWeakKeysValuesTable() throws LuaError {
			LuaTable t = ValueFactory.weakTable(true, true);

			LuaValue key = userdataOf(new MyData(111));
			LuaValue val = userdataOf(new MyData(222));
			LuaValue key2 = userdataOf(new MyData(333));
			LuaValue val2 = userdataOf(new MyData(444));
			LuaValue key3 = userdataOf(new MyData(555));
			LuaValue val3 = userdataOf(new MyData(666));

			// set up the table
			t.set(state, key, val);
			t.set(state, key2, val2);
			t.set(state, key3, val3);
			assertEquals(val, t.get(state, key));
			assertEquals(val2, t.get(state, key2));
			assertEquals(val3, t.get(state, key3));
			System.gc();
			assertEquals(val, t.get(state, key));
			assertEquals(val2, t.get(state, key2));
			assertEquals(val3, t.get(state, key3));

			// drop key and value references, replace them with new ones
			WeakReference<LuaValue> origkey = new WeakReference<>(key);
			WeakReference<LuaValue> origval = new WeakReference<>(val);
			WeakReference<LuaValue> origkey2 = new WeakReference<>(key2);
			WeakReference<LuaValue> origval2 = new WeakReference<>(val2);
			WeakReference<LuaValue> origkey3 = new WeakReference<>(key3);
			WeakReference<LuaValue> origval3 = new WeakReference<>(val3);
			key = userdataOf(new MyData(111));
			val = userdataOf(new MyData(222));
			key2 = userdataOf(new MyData(333));
			// don't drop val2, or key3
			val3 = userdataOf(new MyData(666));

			// no values should be reachable after gc
			collectGarbage();
			assertEquals(null, origkey.get());
			assertEquals(null, origval.get());
			assertEquals(null, origkey2.get());
			assertEquals(null, origval3.get());
			assertEquals(NIL, t.get(state, key));
			assertEquals(NIL, t.get(state, key2));
			assertEquals(NIL, t.get(state, key3));

			// all originals should be gone after gc, then access
			val2 = null;
			key3 = null;
			collectGarbage();
			assertEquals(null, origval2.get());
			assertEquals(null, origkey3.get());
		}

		@Test
		public void testReplace() throws LuaError {
			LuaTable t = ValueFactory.weakTable(true, true);

			LuaValue key = userdataOf(new MyData(111));
			LuaValue val = userdataOf(new MyData(222));
			LuaValue key2 = userdataOf(new MyData(333));
			LuaValue val2 = userdataOf(new MyData(444));
			LuaValue key3 = userdataOf(new MyData(555));
			LuaValue val3 = userdataOf(new MyData(666));

			// set up the table
			t.set(state, key, val);
			t.set(state, key2, val2);
			t.set(state, key3, val3);

			LuaValue val4 = userdataOf(new MyData(777));
			t.set(state, key2, val4);

			// table should have 3 entries
			int size = 0;
			for (LuaValue k = t.next(NIL).first();
				 !k.isNil() && size < 1000;
				 k = t.next(k).first()) {
				size++;
			}
			assertEquals(3, size);
		}

		@Test
		public void testChangeArrayMode() throws LuaError {
			LuaTable table = new LuaTable();

			table.set(state, 1, new LuaTable());
			assertThat(table.get(state, 1), instanceOf(LuaTable.class));

			table.useWeak(true, true);
			collectGarbage();
			assertThat(table.get(state, 1), instanceOf(LuaNil.class));
		}

		@Test
		public void testChangeHashMode() throws LuaError {
			LuaTable table = new LuaTable();

			table.set(state, "foo", new LuaTable());
			assertThat(table.get(state, "foo"), instanceOf(LuaTable.class));

			table.useWeak(true, true);
			collectGarbage();
			assertThat(table.get(state, "foo"), instanceOf(LuaNil.class));
		}
	}
}
