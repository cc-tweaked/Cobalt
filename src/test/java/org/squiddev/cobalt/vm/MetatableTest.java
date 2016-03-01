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
package org.squiddev.cobalt.vm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.squiddev.cobalt.lib.StringLib;
import org.squiddev.cobalt.lib.ThreeArgFunction;
import org.squiddev.cobalt.lib.TwoArgFunction;
import org.squiddev.cobalt.lib.ZeroArgFunction;
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;
import org.squiddev.cobalt.*;

import static org.junit.Assert.assertEquals;

public class MetatableTest {

	private final String samplestring = "abcdef";
	private final Object sampleobject = new Object();
	private final TypeTest.MyData sampledata = new TypeTest.MyData();

	private final LuaValue string = Factory.valueOf(samplestring);
	private final LuaTable table = Factory.tableOf();
	private final LuaFunction function = new ZeroArgFunction() {
		@Override
		public LuaValue call(LuaState state) {
			return Constants.NONE;
		}
	};
	private final LuaState state = new LuaState(new FileResourceManipulator());
	private final LuaThread thread = new LuaThread(state, function, table);
	private final LuaClosure closure = new LuaClosure();
	private final LuaUserdata userdata = Factory.userdataOf(sampleobject);
	private final LuaUserdata userdatamt = Factory.userdataOf(sampledata, table);

	@Before
	public void setup() throws Exception {
		// needed for metatable ops to work on strings
		new StringLib();
	}

	@After
	public void tearDown() throws Exception {
		state.booleanMetatable = null;
		state.functionMetatable = null;
		state.nilMetatable = null;
		state.numberMetatable = null;
		state.threadMetatable = null;
//		LuaString.s_metatable = null;
	}

	@Test
	public void testGetMetatable() {
		assertEquals(null, Constants.NIL.getMetatable(state));
		assertEquals(null, Constants.TRUE.getMetatable(state));
		assertEquals(null, Constants.ONE.getMetatable(state));
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
		LuaValue mt = Factory.tableOf();
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
		assertEquals(null, Constants.NIL.getMetatable(state));
		assertEquals(null, Constants.TRUE.getMetatable(state));
		assertEquals(null, Constants.ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.nilMetatable = mt;
		assertEquals(mt, Constants.NIL.getMetatable(state));
		assertEquals(null, Constants.TRUE.getMetatable(state));
		assertEquals(null, Constants.ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.booleanMetatable = mt;
		assertEquals(mt, Constants.TRUE.getMetatable(state));
		assertEquals(null, Constants.ONE.getMetatable(state));
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable(state));
		assertEquals(null, thread.getMetatable(state));
		assertEquals(null, closure.getMetatable(state));
		state.numberMetatable = mt;
		assertEquals(mt, Constants.ONE.getMetatable(state));
		assertEquals(mt, Factory.valueOf(1.25).getMetatable(state));
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
		assertEquals(Constants.NIL, table.get(state, 1));
		assertEquals(Constants.NIL, userdata.get(state, 1));
		assertEquals(Constants.NIL, userdatamt.get(state, 1));

		// empty metatable
		LuaValue mt = Factory.tableOf();
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
		assertEquals(mt, Constants.NIL.getMetatable(state));
		assertEquals(mt, Constants.TRUE.getMetatable(state));
		assertEquals(mt, Constants.ONE.getMetatable(state));
// 		assertEquals( StringLib.instance, string.getmetatable() );
		assertEquals(mt, function.getMetatable(state));
		assertEquals(mt, thread.getMetatable(state));

		// plain metatable
		LuaValue abc = Factory.valueOf("abc");
		mt.set(state, Constants.INDEX, Factory.listOf(new LuaValue[]{abc}));
		assertEquals(abc, table.get(state, 1));
		assertEquals(abc, userdata.get(state, 1));
		assertEquals(abc, Constants.NIL.get(state, 1));
		assertEquals(abc, Constants.TRUE.get(state, 1));
		assertEquals(abc, Constants.ONE.get(state, 1));
// 		assertEquals( abc, string.get(1) );
		assertEquals(abc, function.get(state, 1));
		assertEquals(abc, thread.get(state, 1));

		// plain metatable
		mt.set(state, Constants.INDEX, new TwoArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) {
				return Factory.valueOf(arg1.typeName() + "[" + arg2.tojstring() + "]=xyz");
			}

		});
		assertEquals("table[1]=xyz", table.get(state, 1).tojstring());
		assertEquals("userdata[1]=xyz", userdata.get(state, 1).tojstring());
		assertEquals("nil[1]=xyz", Constants.NIL.get(state, 1).tojstring());
		assertEquals("boolean[1]=xyz", Constants.TRUE.get(state, 1).tojstring());
		assertEquals("number[1]=xyz", Constants.ONE.get(state, 1).tojstring());
		//	assertEquals( "string[1]=xyz",   string.get(1).tojstring() );
		assertEquals("function[1]=xyz", function.get(state, 1).tojstring());
		assertEquals("thread[1]=xyz", thread.get(state, 1).tojstring());
	}


	@Test
	public void testMetatableNewIndex() {
		// empty metatable
		LuaValue mt = Factory.tableOf();
		assertEquals(table, table.setMetatable(state, mt));
		assertEquals(userdata, userdata.setMetatable(state, mt));
		state.booleanMetatable = mt;
		state.functionMetatable = mt;
		state.nilMetatable = mt;
		state.numberMetatable = mt;
//		LuaString.s_metatable = mt;
		state.threadMetatable = mt;

		// plain metatable
		final LuaValue fallback = Factory.tableOf();
		LuaValue abc = Factory.valueOf("abc");
		mt.set(state, Constants.NEWINDEX, fallback);
		table.set(state, 2, abc);
		userdata.set(state, 3, abc);
		Constants.NIL.set(state, 4, abc);
		Constants.TRUE.set(state, 5, abc);
		Constants.ONE.set(state, 6, abc);
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
		mt.set(state, Constants.NEWINDEX, new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
				fallback.rawset(arg2, Factory.valueOf("via-func-" + arg3));
				return Constants.NONE;
			}

		});
		table.set(state, 12, abc);
		userdata.set(state, 13, abc);
		Constants.NIL.set(state, 14, abc);
		Constants.TRUE.set(state, 15, abc);
		Constants.ONE.set(state, 16, abc);
// 		string.set(17,abc);
		function.set(state, 18, abc);
		thread.set(state, 19, abc);
		LuaValue via = Factory.valueOf("via-func-abc");
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
		assertEquals(ra, t.rawget("aa"));
		assertEquals(rb, t.rawget("bb"));
		assertEquals(rc, t.rawget("cc"));
		assertEquals(rd, t.rawget("dd"));
		assertEquals(re, t.rawget("ee"));
		assertEquals(rf, t.rawget("ff"));
		assertEquals(rg, t.rawget("gg"));
	}

	private LuaValue makeTable(String key1, String val1, String key2, String val2) {
		return Factory.tableOf(new LuaValue[]{
			Factory.valueOf(key1), Factory.valueOf(val1),
			Factory.valueOf(key2), Factory.valueOf(val2),
		});
	}

	@Test
	public void testRawsetMetatableSet() {
		// set up tables
		LuaValue m = makeTable("aa", "aaa", "bb", "bbb");
		m.set(state, Constants.INDEX, m);
		m.set(state, Constants.NEWINDEX, m);
		LuaValue s = makeTable("cc", "ccc", "dd", "ddd");
		LuaValue t = makeTable("cc", "ccc", "dd", "ddd");
		t.setMetatable(state, m);
		LuaValue aaa = Factory.valueOf("aaa");
		LuaValue bbb = Factory.valueOf("bbb");
		LuaValue ccc = Factory.valueOf("ccc");
		LuaValue ddd = Factory.valueOf("ddd");
		LuaValue ppp = Factory.valueOf("ppp");
		LuaValue qqq = Factory.valueOf("qqq");
		LuaValue rrr = Factory.valueOf("rrr");
		LuaValue sss = Factory.valueOf("sss");
		LuaValue ttt = Factory.valueOf("ttt");
		LuaValue www = Factory.valueOf("www");
		LuaValue xxx = Factory.valueOf("xxx");
		LuaValue yyy = Factory.valueOf("yyy");
		LuaValue zzz = Factory.valueOf("zzz");
		LuaValue nil = Constants.NIL;

		// check initial values
		//             values via "bet()"           values via "rawget()"
		checkTable(s, nil, nil, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(t, aaa, bbb, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);

		// rawset()
		s.rawset("aa", www);
		checkTable(s, www, nil, ccc, ddd, nil, nil, nil, www, nil, ccc, ddd, nil, nil, nil);
		checkTable(t, aaa, bbb, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		s.rawset("cc", xxx);
		checkTable(s, www, nil, xxx, ddd, nil, nil, nil, www, nil, xxx, ddd, nil, nil, nil);
		checkTable(t, aaa, bbb, ccc, ddd, nil, nil, nil, nil, nil, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		t.rawset("bb", yyy);
		checkTable(s, www, nil, xxx, ddd, nil, nil, nil, www, nil, xxx, ddd, nil, nil, nil);
		checkTable(t, aaa, yyy, ccc, ddd, nil, nil, nil, nil, yyy, ccc, ddd, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		t.rawset("dd", zzz);
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
		s.setMetatable(state, Factory.tableOf(new LuaValue[]{Constants.INDEX, t, Constants.NEWINDEX, t}));
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
