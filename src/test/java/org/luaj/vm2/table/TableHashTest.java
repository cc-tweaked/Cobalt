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
package org.luaj.vm2.table;

import org.junit.Test;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.luaj.vm2.Constants.*;
import static org.luaj.vm2.Factory.valueOf;

/**
 * Tests for tables used as lists.
 */
public class TableHashTest {
	private LuaState state = LuaThread.getRunning().luaState;

	protected LuaTable new_Table() {
		return new LuaTable();
	}

	@Test
	public void testSetRemove() {
		LuaTable t = new_Table();

		assertEquals(0, t.getHashLength());
		assertEquals(0, t.length(state));
		assertEquals(0, t.keyCount());

		String[] keys = {"abc", "def", "ghi", "jkl", "mno", "pqr", "stu", "wxy", "z01",
			"cd", "ef", "g", "hi", "jk", "lm", "no", "pq", "rs",};
		int[] capacities = {0, 2, 4, 4, 7, 7, 7, 10, 10, 14, 14, 14, 14, 19, 19, 19, 19, 25, 25, 25};
		for (int i = 0; i < keys.length; ++i) {
			assertEquals(capacities[i], t.getHashLength());
			String si = "Test Value! " + i;
			t.set(state, keys[i], valueOf(si));
			assertEquals(0, t.length(state));
			assertEquals(i + 1, t.keyCount());
		}
		assertEquals(capacities[keys.length], t.getHashLength());
		for (int i = 0; i < keys.length; ++i) {
			LuaValue vi = LuaString.valueOf("Test Value! " + i);
			assertEquals(vi, t.get(state, keys[i]));
			assertEquals(vi, t.get(state, LuaString.valueOf(keys[i])));
			assertEquals(vi, t.rawget(state, keys[i]));
			assertEquals(vi, t.rawget(state, keys[i]));
		}

		// replace with new values
		for (int i = 0; i < keys.length; ++i) {
			t.set(state, keys[i], LuaString.valueOf("Replacement Value! " + i));
			assertEquals(0, t.length(state));
			assertEquals(keys.length, t.keyCount());
			assertEquals(capacities[keys.length], t.getHashLength());
		}
		for (int i = 0; i < keys.length; ++i) {
			LuaValue vi = LuaString.valueOf("Replacement Value! " + i);
			assertEquals(vi, t.get(state, keys[i]));
		}

		// remove
		for (int i = 0; i < keys.length; ++i) {
			t.set(state, keys[i], NIL);
			assertEquals(0, t.length(state));
			assertEquals(keys.length - i - 1, t.keyCount());
			if (i < keys.length - 1) {
				assertEquals(capacities[keys.length], t.getHashLength());
			} else {
				assertTrue(0 <= t.getHashLength());
			}
		}
		for (String key : keys) {
			assertEquals(NIL, t.get(state, key));
		}
	}

	@Test
	public void testIndexMetatag() {
		LuaTable t = new_Table();
		LuaTable mt = new_Table();
		LuaTable fb = new_Table();

		// set basic values
		t.set(state, "ppp", valueOf("abc"));
		t.set(state, 123, valueOf("def"));
		mt.set(state, INDEX, fb);
		fb.set(state, "qqq", valueOf("ghi"));
		fb.set(state, 456, valueOf("jkl"));

		// check before setting metatable
		assertEquals("abc", t.get(state, "ppp").tojstring());
		assertEquals("def", t.get(state, 123).tojstring());
		assertEquals("nil", t.get(state, "qqq").tojstring());
		assertEquals("nil", t.get(state, 456).tojstring());
		assertEquals("nil", fb.get(state, "ppp").tojstring());
		assertEquals("nil", fb.get(state, 123).tojstring());
		assertEquals("ghi", fb.get(state, "qqq").tojstring());
		assertEquals("jkl", fb.get(state, 456).tojstring());
		assertEquals("nil", mt.get(state, "ppp").tojstring());
		assertEquals("nil", mt.get(state, 123).tojstring());
		assertEquals("nil", mt.get(state, "qqq").tojstring());
		assertEquals("nil", mt.get(state, 456).tojstring());

		// check before setting metatable
		t.setMetatable(state, mt);
		assertEquals(mt, t.getMetatable(state));
		assertEquals("abc", t.get(state, "ppp").tojstring());
		assertEquals("def", t.get(state, 123).tojstring());
		assertEquals("ghi", t.get(state, "qqq").tojstring());
		assertEquals("jkl", t.get(state, 456).tojstring());
		assertEquals("nil", fb.get(state, "ppp").tojstring());
		assertEquals("nil", fb.get(state, 123).tojstring());
		assertEquals("ghi", fb.get(state, "qqq").tojstring());
		assertEquals("jkl", fb.get(state, 456).tojstring());
		assertEquals("nil", mt.get(state, "ppp").tojstring());
		assertEquals("nil", mt.get(state, 123).tojstring());
		assertEquals("nil", mt.get(state, "qqq").tojstring());
		assertEquals("nil", mt.get(state, 456).tojstring());

		// set metatable to metatable without values
		t.setMetatable(state, fb);
		assertEquals("abc", t.get(state, "ppp").tojstring());
		assertEquals("def", t.get(state, 123).tojstring());
		assertEquals("nil", t.get(state, "qqq").tojstring());
		assertEquals("nil", t.get(state, 456).tojstring());

		// set metatable to null
		t.setMetatable(state, null);
		assertEquals("abc", t.get(state, "ppp").tojstring());
		assertEquals("def", t.get(state, 123).tojstring());
		assertEquals("nil", t.get(state, "qqq").tojstring());
		assertEquals("nil", t.get(state, 456).tojstring());
	}

	@Test
	public void testIndexFunction() {
		final LuaTable t = new_Table();
		final LuaTable mt = new_Table();

		final TwoArgFunction fb = new TwoArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue tbl, LuaValue key) {
				assertEquals(tbl, t);
				return valueOf("from mt: " + key);
			}
		};

		// set basic values
		t.set(state, "ppp", valueOf("abc"));
		t.set(state, 123, valueOf("def"));
		mt.set(state, INDEX, fb);

		// check before setting metatable
		assertEquals("abc", t.get(state, "ppp").tojstring());
		assertEquals("def", t.get(state, 123).tojstring());
		assertEquals("nil", t.get(state, "qqq").tojstring());
		assertEquals("nil", t.get(state, 456).tojstring());


		// check before setting metatable
		t.setMetatable(state, mt);
		assertEquals(mt, t.getMetatable(state));
		assertEquals("abc", t.get(state, "ppp").tojstring());
		assertEquals("def", t.get(state, 123).tojstring());
		assertEquals("from mt: qqq", t.get(state, "qqq").tojstring());
		assertEquals("from mt: 456", t.get(state, 456).tojstring());

		// use raw set
		t.rawset(state, "qqq", valueOf("alt-qqq"));
		t.rawset(state, 456, valueOf("alt-456"));
		assertEquals("abc", t.get(state, "ppp").tojstring());
		assertEquals("def", t.get(state, 123).tojstring());
		assertEquals("alt-qqq", t.get(state, "qqq").tojstring());
		assertEquals("alt-456", t.get(state, 456).tojstring());

		// remove using raw set
		t.rawset(state, "qqq", NIL);
		t.rawset(state, 456, NIL);
		assertEquals("abc", t.get(state, "ppp").tojstring());
		assertEquals("def", t.get(state, 123).tojstring());
		assertEquals("from mt: qqq", t.get(state, "qqq").tojstring());
		assertEquals("from mt: 456", t.get(state, 456).tojstring());

		// set metatable to null
		t.setMetatable(state, null);
		assertEquals("abc", t.get(state, "ppp").tojstring());
		assertEquals("def", t.get(state, 123).tojstring());
		assertEquals("nil", t.get(state, "qqq").tojstring());
		assertEquals("nil", t.get(state, 456).tojstring());
	}

	@Test
	public void testNext() {
		final LuaTable t = new_Table();
		assertEquals(NIL, t.next(NIL));

		// insert array elements
		t.set(state, 1, valueOf("one"));
		assertEquals(valueOf(1), t.next(NIL).arg(1));
		assertEquals(valueOf("one"), t.next(NIL).arg(2));
		assertEquals(NIL, t.next(ONE));
		t.set(state, 2, valueOf("two"));
		assertEquals(valueOf(1), t.next(NIL).arg(1));
		assertEquals(valueOf("one"), t.next(NIL).arg(2));
		assertEquals(valueOf(2), t.next(ONE).arg(1));
		assertEquals(valueOf("two"), t.next(ONE).arg(2));
		assertEquals(NIL, t.next(valueOf(2)));

		// insert hash elements
		t.set(state, "aa", valueOf("aaa"));
		assertEquals(valueOf(1), t.next(NIL).arg(1));
		assertEquals(valueOf("one"), t.next(NIL).arg(2));
		assertEquals(valueOf(2), t.next(ONE).arg(1));
		assertEquals(valueOf("two"), t.next(ONE).arg(2));
		assertEquals(valueOf("aa"), t.next(valueOf(2)).arg(1));
		assertEquals(valueOf("aaa"), t.next(valueOf(2)).arg(2));
		assertEquals(NIL, t.next(valueOf("aa")));
		t.set(state, "bb", valueOf("bbb"));
		assertEquals(valueOf(1), t.next(NIL).arg(1));
		assertEquals(valueOf("one"), t.next(NIL).arg(2));
		assertEquals(valueOf(2), t.next(ONE).arg(1));
		assertEquals(valueOf("two"), t.next(ONE).arg(2));
		assertEquals(valueOf("aa"), t.next(valueOf(2)).arg(1));
		assertEquals(valueOf("aaa"), t.next(valueOf(2)).arg(2));
		assertEquals(valueOf("bb"), t.next(valueOf("aa")).arg(1));
		assertEquals(valueOf("bbb"), t.next(valueOf("aa")).arg(2));
		assertEquals(NIL, t.next(valueOf("bb")));
	}
}
