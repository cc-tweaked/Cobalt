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
package org.luaj.vm2.vm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.vm.TypeTest.MyData;

import static org.junit.Assert.assertEquals;
import static org.luaj.vm2.Constants.*;
import static org.luaj.vm2.Factory.*;

public class MetatableTest {

	private final String samplestring = "abcdef";
	private final Object sampleobject = new Object();
	private final MyData sampledata = new MyData();

	private final LuaValue string = valueOf(samplestring);
	private final LuaTable table = tableOf();
	private final LuaFunction function = new ZeroArgFunction() {
		@Override
		public LuaValue call(LuaState state) {
			return NONE;
		}
	};
	private final LuaState state = LuaThread.getRunning().luaState;
	private final LuaThread thread = new LuaThread(LuaThread.getRunning().luaState, function, table);
	private final LuaClosure closure = new LuaClosure();
	private final LuaUserdata userdata = userdataOf(sampleobject);
	private final LuaUserdata userdatamt = userdataOf(sampledata, table);

	@Before
	public void setup() throws Exception {
		// needed for metatable ops to work on strings
		new StringLib();
	}

	@After
	public void tearDown() throws Exception {
		LuaState state = LuaThread.getRunning().luaState;
		state.booleanMetatable = null;
		state.functionMetatable = null;
		state.nilMetatable = null;
		state.numberMetatable = null;
		state.threadMetatable = null;
//		LuaString.s_metatable = null;
	}

	@Test
	public void testGetMetatable() {
		assertEquals(null, NIL.getMetatable(state));
		assertEquals(null, TRUE.getMetatable(state));
		assertEquals(null, ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, table.getMetatable(state));
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		assertEquals(null, userdata.getMetatable(state));
		assertEquals(table, userdatamt.getMetatable(state));
	}

	@Test
	public void testSetMetatable() {
		LuaValue mt = tableOf();
		assertEquals(null, table.getMetatable(state));
		assertEquals(null, userdata.getMetatable(state));
		assertEquals(table, userdatamt.getMetatable(state));
		assertEquals(table, table.setMetatable(state, mt));
		assertEquals(userdata, userdata.setMetatable(state, mt));
		assertEquals(userdatamt, userdatamt.setMetatable(state, mt));
		assertEquals(mt, table.getMetatable(state));
		assertEquals(mt, userdata.getMetatable(state));
		assertEquals(mt, userdatamt.getMetatable(state));

		// these all get metatable behind-the-scenes
		assertEquals(null, NIL.getMetatable(state));
		assertEquals(null, TRUE.getMetatable(state));
		assertEquals(null, ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.nilMetatable = mt;
		assertEquals(mt, NIL.getMetatable(state));
		assertEquals(null, TRUE.getMetatable(state));
		assertEquals(null, ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.booleanMetatable = mt;
		assertEquals(mt, TRUE.getMetatable(state));
		assertEquals(null, ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.numberMetatable = mt;
		assertEquals(mt, ONE.getMetatable(state));
		assertEquals(mt, valueOf(1.25).getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
//		LuaString.s_metatable = mt;
//		assertEquals( mt, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.functionMetatable = mt;
		assertEquals(mt, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		state.threadMetatable = mt;
		assertEquals(mt, thread.getMetatable(state));
	}

	@Test
	public void testMetatableIndex() {
		assertEquals(table, table.setMetatable(state, null));
		assertEquals(userdata, userdata.setMetatable(state, null));
		assertEquals(userdatamt, userdatamt.setMetatable(state, null));
		assertEquals(NIL, table.get(state, 1));
		assertEquals(NIL, userdata.get(state, 1));
		assertEquals(NIL, userdatamt.get(state, 1));

		// empty metatable
		LuaValue mt = tableOf();
		assertEquals(table, table.setMetatable(state, mt));
		assertEquals(userdata, userdata.setMetatable(state, mt));
		state.booleanMetatable = mt;
		state.functionMetatable = mt;
		state.nilMetatable = mt;
		state.numberMetatable = mt;
//		LuaString.s_metatable = mt;
		state.threadMetatable = mt;
		assertEquals(mt, table.getMetatable(state));
		assertEquals(mt, userdata.getMetatable(state));
		assertEquals(mt, NIL.getMetatable(state));
		assertEquals(mt, TRUE.getMetatable(state));
		assertEquals(mt, ONE.getMetatable(state));
// 		assertEquals( StringLib.instance, string.getmetatable() );
		assertEquals(mt, function.getMetatable(state));
		assertEquals(mt, thread.getMetatable(state));

		// plain metatable
		LuaValue abc = valueOf("abc");
		mt.set(state, INDEX, listOf(new LuaValue[]{abc}));
		assertEquals(abc, table.get(state, 1));
		assertEquals(abc, userdata.get(state, 1));
		assertEquals(abc, NIL.get(state, 1));
		assertEquals(abc, TRUE.get(state, 1));
		assertEquals(abc, ONE.get(state, 1));
// 		assertEquals( abc, string.get(1) );
		assertEquals(abc, function.get(state, 1));
		assertEquals(abc, thread.get(state, 1));

		// plain metatable
		mt.set(state, INDEX, new TwoArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) {
				return valueOf(arg1.typeName() + "[" + arg2.tojstring() + "]=xyz");
			}

		});
		assertEquals("table[1]=xyz", table.get(state, 1).tojstring());
		assertEquals("userdata[1]=xyz", userdata.get(state, 1).tojstring());
		assertEquals("nil[1]=xyz", NIL.get(state, 1).tojstring());
		assertEquals("boolean[1]=xyz", TRUE.get(state, 1).tojstring());
		assertEquals("number[1]=xyz", ONE.get(state, 1).tojstring());
		//	assertEquals( "string[1]=xyz",   string.get(1).tojstring() );
		assertEquals("function[1]=xyz", function.get(state, 1).tojstring());
		assertEquals("thread[1]=xyz", thread.get(state, 1).tojstring());
	}


	@Test
	public void testMetatableNewIndex() {
		// empty metatable
		LuaValue mt = tableOf();
		assertEquals(table, table.setMetatable(state, mt));
		assertEquals(userdata, userdata.setMetatable(state, mt));
		state.booleanMetatable = mt;
		state.functionMetatable = mt;
		state.nilMetatable = mt;
		state.numberMetatable = mt;
//		LuaString.s_metatable = mt;
		state.threadMetatable = mt;

		// plain metatable
		final LuaValue fallback = tableOf();
		LuaValue abc = valueOf("abc");
		mt.set(state, NEWINDEX, fallback);
		table.set(state, 2, abc);
		userdata.set(state, 3, abc);
		NIL.set(state, 4, abc);
		TRUE.set(state, 5, abc);
		ONE.set(state, 6, abc);
// 		string.set(7,abc);
		function.set(state, 8, abc);
		thread.set(state, 9, abc);
		assertEquals(abc, fallback.get(state, 2));
		assertEquals(abc, fallback.get(state, 3));
		assertEquals(abc, fallback.get(state, 4));
		assertEquals(abc, fallback.get(state, 5));
		assertEquals(abc, fallback.get(state, 6));
// 		assertEquals( abc, StringLib.instance.get(7) );
		assertEquals(abc, fallback.get(state, 8));
		assertEquals(abc, fallback.get(state, 9));

		// metatable with function call
		mt.set(state, NEWINDEX, new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
				fallback.rawset(state, arg2, valueOf("via-func-" + arg3));
				return NONE;
			}

		});
		table.set(state, 12, abc);
		userdata.set(state, 13, abc);
		NIL.set(state, 14, abc);
		TRUE.set(state, 15, abc);
		ONE.set(state, 16, abc);
// 		string.set(17,abc);
		function.set(state, 18, abc);
		thread.set(state, 19, abc);
		LuaValue via = valueOf("via-func-abc");
		assertEquals(via, fallback.get(state, 12));
		assertEquals(via, fallback.get(state, 13));
		assertEquals(via, fallback.get(state, 14));
		assertEquals(via, fallback.get(state, 15));
		assertEquals(via, fallback.get(state, 16));
//		assertEquals( via, StringLib.instance.get(17) );
		assertEquals(via, fallback.get(state, 18));
		assertEquals(via, fallback.get(state, 19));
	}


	private void checkTable(LuaValue t,
	                        LuaValue aa, LuaValue bb, LuaValue cc, LuaValue dd, LuaValue ee, LuaValue ff, LuaValue gg,
	                        LuaValue ra, LuaValue rb, LuaValue rc, LuaValue rd, LuaValue re, LuaValue rf, LuaValue rg) {
		assertEquals(aa, t.get(state, "aa"));
		assertEquals(bb, t.get(state, "bb"));
		assertEquals(cc, t.get(state, "cc"));
		assertEquals(dd, t.get(state, "dd"));
		assertEquals(ee, t.get(state, "ee"));
		assertEquals(ff, t.get(state, "ff"));
		assertEquals(gg, t.get(state, "gg"));
		assertEquals(ra, t.rawget(state, "aa"));
		assertEquals(rb, t.rawget(state, "bb"));
		assertEquals(rc, t.rawget(state, "cc"));
		assertEquals(rd, t.rawget(state, "dd"));
		assertEquals(re, t.rawget(state, "ee"));
		assertEquals(rf, t.rawget(state, "ff"));
		assertEquals(rg, t.rawget(state, "gg"));
	}

	private LuaValue makeTable(String key1, String val1, String key2, String val2) {
		return tableOf(new LuaValue[]{
			valueOf(key1), valueOf(val1),
			valueOf(key2), valueOf(val2),
		});
	}

	@Test
	public void testRawsetMetatableSet() {
		// set up tables
		LuaValue m = makeTable("aa", "aaa", "bb", "bbb");
		m.set(state, INDEX, m);
		m.set(state, NEWINDEX, m);
		LuaValue s = makeTable("cc", "ccc", "dd", "ddd");
		LuaValue t = makeTable("cc", "ccc", "dd", "ddd");
		t.setMetatable(state, m);
		LuaValue aaa = valueOf("aaa");
		LuaValue bbb = valueOf("bbb");
		LuaValue ccc = valueOf("ccc");
		LuaValue ddd = valueOf("ddd");
		LuaValue ppp = valueOf("ppp");
		LuaValue qqq = valueOf("qqq");
		LuaValue rrr = valueOf("rrr");
		LuaValue sss = valueOf("sss");
		LuaValue ttt = valueOf("ttt");
		LuaValue www = valueOf("www");
		LuaValue xxx = valueOf("xxx");
		LuaValue yyy = valueOf("yyy");
		LuaValue zzz = valueOf("zzz");
		LuaValue nil = NIL;

		// check initial values
		//             values via "bet()"           values via "rawget()"
		checkTable(s, nil, nil, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(t, aaa, bbb, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);

		// rawset()
		s.rawset(state, "aa", www);
		checkTable(s, www, nil, ccc, ddd, nil, nil, nil, www, nil, ccc, ddd, nil, nil, nil);
		checkTable(t, aaa, bbb, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		s.rawset(state, "cc", xxx);
		checkTable(s, www, nil, xxx, ddd, nil, nil, nil, www, nil, xxx, ddd, nil, nil, nil);
		checkTable(t, aaa, bbb, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		t.rawset(state, "bb", yyy);
		checkTable(s, www, nil, xxx, ddd, nil, nil, nil, www, nil, xxx, ddd, nil, nil, nil);
		checkTable(t, aaa, yyy, ccc, ddd, nil, nil, nil, nil, yyy, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		t.rawset(state, "dd", zzz);
		checkTable(s, www, nil, xxx, ddd, nil, nil, nil, www, nil, xxx, ddd, nil, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, nil, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);

		// set() invoking metatables
		s.set(state, "ee", ppp);
		checkTable(s, www, nil, xxx, ddd, ppp, nil, nil, www, nil, xxx, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, nil, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		s.set(state, "cc", qqq);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, nil, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		t.set(state, "ff", rrr);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, rrr, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, nil, aaa, bbb, nil, nil, nil, rrr, nil);
		t.set(state, "dd", sss);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, nil, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, nil, aaa, bbb, nil, nil, nil, rrr, nil);
		m.set(state, "gg", ttt);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, ttt, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);

		// make s fall back to t
		s.setMetatable(state, tableOf(new LuaValue[]{INDEX, t, NEWINDEX, t}));
		checkTable(s, www, yyy, qqq, ddd, ppp, rrr, ttt, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, ttt, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		s.set(state, "aa", www);
		checkTable(s, www, yyy, qqq, ddd, ppp, rrr, ttt, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, ttt, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		s.set(state, "bb", zzz);
		checkTable(s, www, zzz, qqq, ddd, ppp, rrr, ttt, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, zzz, ccc, sss, nil, rrr, ttt, nil, zzz, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		s.set(state, "ee", xxx);
		checkTable(s, www, zzz, qqq, ddd, xxx, rrr, ttt, www, nil, qqq, ddd, xxx, nil, nil);
		checkTable(t, aaa, zzz, ccc, sss, nil, rrr, ttt, nil, zzz, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		s.set(state, "ff", yyy);
		checkTable(s, www, zzz, qqq, ddd, xxx, yyy, ttt, www, nil, qqq, ddd, xxx, nil, nil);
		checkTable(t, aaa, zzz, ccc, sss, nil, yyy, ttt, nil, zzz, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, yyy, ttt, aaa, bbb, nil, nil, nil, yyy, ttt);


	}

}
