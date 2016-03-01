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
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.lib.TwoArgFunction;
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;
import org.squiddev.cobalt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.squiddev.cobalt.Factory.valueOf;

/**
 * Tests for tables used as lists.
 */
public class TableHashTest {
	private LuaState state;

	@Before
	public void setup() throws Exception {
		state = new LuaState(new FileResourceManipulator());
	}

	@Test
	public void testSetRemove() {
		LuaTable t = new LuaTable();

		assertEquals(0, t.getHashLength());
		assertEquals(0, t.length(state));
		assertEquals(0, t.keyCount());

		String[] keys = {"abc", "def", "ghi", "jkl", "mno", "pqr", "stu", "wxy", "z01",
			"cd", "ef", "g", "hi", "jk", "lm", "no", "pq", "rs",};
		int[] capacities = {0, 2, 4, 4, 7, 7, 7, 10, 10, 14, 14, 14, 14, 19, 19, 19, 19, 25, 25, 25};
		for (int i = 0; i < keys.length; ++i) {
			assertEquals(capacities[i], t.getHashLength());
			String si = "Test Value! " + i;
			t.set(state, keys[i], Factory.valueOf(si));
			assertEquals(0, t.length(state));
			assertEquals(i + 1, t.keyCount());
		}
		assertEquals(capacities[keys.length], t.getHashLength());
		for (int i = 0; i < keys.length; ++i) {
			LuaValue vi = LuaString.valueOf("Test Value! " + i);
			assertEquals(vi, t.get(state, keys[i]));
			assertEquals(vi, t.get(state, LuaString.valueOf(keys[i])));
			assertEquals(vi, t.rawget(keys[i]));
			assertEquals(vi, t.rawget(keys[i]));
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
			t.set(state, keys[i], Constants.NIL);
			assertEquals(0, t.length(state));
			assertEquals(keys.length - i - 1, t.keyCount());
			if (i < keys.length - 1) {
				assertEquals(capacities[keys.length], t.getHashLength());
			} else {
				assertTrue(0 <= t.getHashLength());
			}
		}
		for (String key : keys) {
			assertEquals(Constants.NIL, t.get(state, key));
		}
	}

	@Test
	public void testIndexMetatag() {
		LuaTable t = new LuaTable();
		LuaTable mt = new LuaTable();
		LuaTable fb = new LuaTable();

		// set basic values
		t.set(state, "ppp", Factory.valueOf("abc"));
		t.set(state, 123, Factory.valueOf("def"));
		mt.set(state, Constants.INDEX, fb);
		fb.set(state, "qqq", Factory.valueOf("ghi"));
		fb.set(state, 456, Factory.valueOf("jkl"));

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
		final LuaTable t = new LuaTable();
		final LuaTable mt = new LuaTable();

		final TwoArgFunction fb = new TwoArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue tbl, LuaValue key) {
				assertEquals(tbl, t);
				return Factory.valueOf("from mt: " + key);
			}
		};

		// set basic values
		t.set(state, "ppp", Factory.valueOf("abc"));
		t.set(state, 123, Factory.valueOf("def"));
		mt.set(state, Constants.INDEX, fb);

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
		t.rawset("qqq", Factory.valueOf("alt-qqq"));
		t.rawset(456, Factory.valueOf("alt-456"));
		assertEquals("abc", t.get(state, "ppp").tojstring());
		assertEquals("def", t.get(state, 123).tojstring());
		assertEquals("alt-qqq", t.get(state, "qqq").tojstring());
		assertEquals("alt-456", t.get(state, 456).tojstring());

		// remove using raw set
		t.rawset("qqq", Constants.NIL);
		t.rawset(456, Constants.NIL);
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
		final LuaTable t = new LuaTable();
		assertEquals(Constants.NIL, t.next(Constants.NIL));

		// insert array elements
		t.set(state, 1, Factory.valueOf("one"));
		assertEquals(Factory.valueOf(1), t.next(Constants.NIL).arg(1));
		assertEquals(Factory.valueOf("one"), t.next(Constants.NIL).arg(2));
		assertEquals(Constants.NIL, t.next(Constants.ONE));
		t.set(state, 2, Factory.valueOf("two"));
		assertEquals(Factory.valueOf(1), t.next(Constants.NIL).arg(1));
		assertEquals(Factory.valueOf("one"), t.next(Constants.NIL).arg(2));
		assertEquals(Factory.valueOf(2), t.next(Constants.ONE).arg(1));
		assertEquals(Factory.valueOf("two"), t.next(Constants.ONE).arg(2));
		assertEquals(Constants.NIL, t.next(Factory.valueOf(2)));

		// insert hash elements
		t.set(state, "aa", Factory.valueOf("aaa"));
		assertEquals(Factory.valueOf(1), t.next(Constants.NIL).arg(1));
		assertEquals(Factory.valueOf("one"), t.next(Constants.NIL).arg(2));
		assertEquals(Factory.valueOf(2), t.next(Constants.ONE).arg(1));
		assertEquals(Factory.valueOf("two"), t.next(Constants.ONE).arg(2));
		assertEquals(Factory.valueOf("aa"), t.next(Factory.valueOf(2)).arg(1));
		assertEquals(Factory.valueOf("aaa"), t.next(Factory.valueOf(2)).arg(2));
		assertEquals(Constants.NIL, t.next(Factory.valueOf("aa")));
		t.set(state, "bb", Factory.valueOf("bbb"));
		assertEquals(Factory.valueOf(1), t.next(Constants.NIL).arg(1));
		assertEquals(Factory.valueOf("one"), t.next(Constants.NIL).arg(2));
		assertEquals(Factory.valueOf(2), t.next(Constants.ONE).arg(1));
		assertEquals(Factory.valueOf("two"), t.next(Constants.ONE).arg(2));
		assertEquals(Factory.valueOf("aa"), t.next(Factory.valueOf(2)).arg(1));
		assertEquals(Factory.valueOf("aaa"), t.next(Factory.valueOf(2)).arg(2));
		assertEquals(Factory.valueOf("bb"), t.next(Factory.valueOf("aa")).arg(1));
		assertEquals(Factory.valueOf("bbb"), t.next(Factory.valueOf("aa")).arg(2));
		assertEquals(Constants.NIL, t.next(Factory.valueOf("bb")));
	}
}
