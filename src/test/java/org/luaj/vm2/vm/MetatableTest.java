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
		public LuaValue call() {
			return NONE;
		}
	};
	private final LuaThread thread = new LuaThread(function, table);
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
		LuaBoolean.s_metatable = null;
		LuaFunction.s_metatable = null;
		LuaNil.s_metatable = null;
		LuaNumber.s_metatable = null;
//		LuaString.s_metatable = null;
		LuaThread.s_metatable = null;
	}

	@Test
	public void testGetMetatable() {
		assertEquals(null, NIL.getMetatable());
		assertEquals(null, TRUE.getMetatable());
		assertEquals(null, ONE.getMetatable());
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, table.getMetatable());
		assertEquals(null, function.getMetatable());
		assertEquals(null, thread.getMetatable());
		assertEquals(null, closure.getMetatable());
		assertEquals(null, userdata.getMetatable());
		assertEquals(table, userdatamt.getMetatable());
	}

	@Test
	public void testSetMetatable() {
		LuaValue mt = tableOf();
		assertEquals(null, table.getMetatable());
		assertEquals(null, userdata.getMetatable());
		assertEquals(table, userdatamt.getMetatable());
		assertEquals(table, table.setMetatable(mt));
		assertEquals(userdata, userdata.setMetatable(mt));
		assertEquals(userdatamt, userdatamt.setMetatable(mt));
		assertEquals(mt, table.getMetatable());
		assertEquals(mt, userdata.getMetatable());
		assertEquals(mt, userdatamt.getMetatable());

		// these all get metatable behind-the-scenes
		assertEquals(null, NIL.getMetatable());
		assertEquals(null, TRUE.getMetatable());
		assertEquals(null, ONE.getMetatable());
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable());
		assertEquals(null, thread.getMetatable());
		assertEquals(null, closure.getMetatable());
		LuaNil.s_metatable = mt;
		assertEquals(mt, NIL.getMetatable());
		assertEquals(null, TRUE.getMetatable());
		assertEquals(null, ONE.getMetatable());
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable());
		assertEquals(null, thread.getMetatable());
		assertEquals(null, closure.getMetatable());
		LuaBoolean.s_metatable = mt;
		assertEquals(mt, TRUE.getMetatable());
		assertEquals(null, ONE.getMetatable());
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable());
		assertEquals(null, thread.getMetatable());
		assertEquals(null, closure.getMetatable());
		LuaNumber.s_metatable = mt;
		assertEquals(mt, ONE.getMetatable());
		assertEquals(mt, valueOf(1.25).getMetatable());
//		assertEquals( null, string.getmetatable() );
		assertEquals(null, function.getMetatable());
		assertEquals(null, thread.getMetatable());
		assertEquals(null, closure.getMetatable());
//		LuaString.s_metatable = mt;
//		assertEquals( mt, string.getmetatable() );
		assertEquals(null, function.getMetatable());
		assertEquals(null, thread.getMetatable());
		assertEquals(null, closure.getMetatable());
		LuaFunction.s_metatable = mt;
		assertEquals(mt, function.getMetatable());
		assertEquals(null, thread.getMetatable());
		LuaThread.s_metatable = mt;
		assertEquals(mt, thread.getMetatable());
	}

	@Test
	public void testMetatableIndex() {
		assertEquals(table, table.setMetatable(null));
		assertEquals(userdata, userdata.setMetatable(null));
		assertEquals(userdatamt, userdatamt.setMetatable(null));
		assertEquals(NIL, table.get(1));
		assertEquals(NIL, userdata.get(1));
		assertEquals(NIL, userdatamt.get(1));

		// empty metatable
		LuaValue mt = tableOf();
		assertEquals(table, table.setMetatable(mt));
		assertEquals(userdata, userdata.setMetatable(mt));
		LuaBoolean.s_metatable = mt;
		LuaFunction.s_metatable = mt;
		LuaNil.s_metatable = mt;
		LuaNumber.s_metatable = mt;
//		LuaString.s_metatable = mt;
		LuaThread.s_metatable = mt;
		assertEquals(mt, table.getMetatable());
		assertEquals(mt, userdata.getMetatable());
		assertEquals(mt, NIL.getMetatable());
		assertEquals(mt, TRUE.getMetatable());
		assertEquals(mt, ONE.getMetatable());
// 		assertEquals( StringLib.instance, string.getmetatable() );
		assertEquals(mt, function.getMetatable());
		assertEquals(mt, thread.getMetatable());

		// plain metatable
		LuaValue abc = valueOf("abc");
		mt.set(INDEX, listOf(new LuaValue[]{abc}));
		assertEquals(abc, table.get(1));
		assertEquals(abc, userdata.get(1));
		assertEquals(abc, NIL.get(1));
		assertEquals(abc, TRUE.get(1));
		assertEquals(abc, ONE.get(1));
// 		assertEquals( abc, string.get(1) );
		assertEquals(abc, function.get(1));
		assertEquals(abc, thread.get(1));

		// plain metatable
		mt.set(INDEX, new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2) {
				return valueOf(arg1.typeName() + "[" + arg2.tojstring() + "]=xyz");
			}

		});
		assertEquals("table[1]=xyz", table.get(1).tojstring());
		assertEquals("userdata[1]=xyz", userdata.get(1).tojstring());
		assertEquals("nil[1]=xyz", NIL.get(1).tojstring());
		assertEquals("boolean[1]=xyz", TRUE.get(1).tojstring());
		assertEquals("number[1]=xyz", ONE.get(1).tojstring());
		//	assertEquals( "string[1]=xyz",   string.get(1).tojstring() );
		assertEquals("function[1]=xyz", function.get(1).tojstring());
		assertEquals("thread[1]=xyz", thread.get(1).tojstring());
	}


	@Test
	public void testMetatableNewIndex() {
		// empty metatable
		LuaValue mt = tableOf();
		assertEquals(table, table.setMetatable(mt));
		assertEquals(userdata, userdata.setMetatable(mt));
		LuaBoolean.s_metatable = mt;
		LuaFunction.s_metatable = mt;
		LuaNil.s_metatable = mt;
		LuaNumber.s_metatable = mt;
//		LuaString.s_metatable = mt;
		LuaThread.s_metatable = mt;

		// plain metatable
		final LuaValue fallback = tableOf();
		LuaValue abc = valueOf("abc");
		mt.set(NEWINDEX, fallback);
		table.set(2, abc);
		userdata.set(3, abc);
		NIL.set(4, abc);
		TRUE.set(5, abc);
		ONE.set(6, abc);
// 		string.set(7,abc);
		function.set(8, abc);
		thread.set(9, abc);
		assertEquals(abc, fallback.get(2));
		assertEquals(abc, fallback.get(3));
		assertEquals(abc, fallback.get(4));
		assertEquals(abc, fallback.get(5));
		assertEquals(abc, fallback.get(6));
// 		assertEquals( abc, StringLib.instance.get(7) );
		assertEquals(abc, fallback.get(8));
		assertEquals(abc, fallback.get(9));

		// metatable with function call
		mt.set(NEWINDEX, new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
				fallback.rawset(arg2, valueOf("via-func-" + arg3));
				return NONE;
			}

		});
		table.set(12, abc);
		userdata.set(13, abc);
		NIL.set(14, abc);
		TRUE.set(15, abc);
		ONE.set(16, abc);
// 		string.set(17,abc);
		function.set(18, abc);
		thread.set(19, abc);
		LuaValue via = valueOf("via-func-abc");
		assertEquals(via, fallback.get(12));
		assertEquals(via, fallback.get(13));
		assertEquals(via, fallback.get(14));
		assertEquals(via, fallback.get(15));
		assertEquals(via, fallback.get(16));
//		assertEquals( via, StringLib.instance.get(17) );
		assertEquals(via, fallback.get(18));
		assertEquals(via, fallback.get(19));
	}


	private void checkTable(LuaValue t,
	                        LuaValue aa, LuaValue bb, LuaValue cc, LuaValue dd, LuaValue ee, LuaValue ff, LuaValue gg,
	                        LuaValue ra, LuaValue rb, LuaValue rc, LuaValue rd, LuaValue re, LuaValue rf, LuaValue rg) {
		assertEquals(aa, t.get("aa"));
		assertEquals(bb, t.get("bb"));
		assertEquals(cc, t.get("cc"));
		assertEquals(dd, t.get("dd"));
		assertEquals(ee, t.get("ee"));
		assertEquals(ff, t.get("ff"));
		assertEquals(gg, t.get("gg"));
		assertEquals(ra, t.rawget("aa"));
		assertEquals(rb, t.rawget("bb"));
		assertEquals(rc, t.rawget("cc"));
		assertEquals(rd, t.rawget("dd"));
		assertEquals(re, t.rawget("ee"));
		assertEquals(rf, t.rawget("ff"));
		assertEquals(rg, t.rawget("gg"));
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
		m.set(INDEX, m);
		m.set(NEWINDEX, m);
		LuaValue s = makeTable("cc", "ccc", "dd", "ddd");
		LuaValue t = makeTable("cc", "ccc", "dd", "ddd");
		t.setMetatable(m);
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
		s.set("ee", ppp);
		checkTable(s, www, nil, xxx, ddd, ppp, nil, nil, www, nil, xxx, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, nil, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		s.set("cc", qqq);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, nil, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, nil, nil, aaa, bbb, nil, nil, nil, nil, nil);
		t.set("ff", rrr);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, zzz, nil, rrr, nil, nil, yyy, ccc, zzz, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, nil, aaa, bbb, nil, nil, nil, rrr, nil);
		t.set("dd", sss);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, nil, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, nil, aaa, bbb, nil, nil, nil, rrr, nil);
		m.set("gg", ttt);
		checkTable(s, www, nil, qqq, ddd, ppp, nil, nil, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, ttt, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);

		// make s fall back to t
		s.setMetatable(tableOf(new LuaValue[]{INDEX, t, NEWINDEX, t}));
		checkTable(s, www, yyy, qqq, ddd, ppp, rrr, ttt, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, ttt, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		s.set("aa", www);
		checkTable(s, www, yyy, qqq, ddd, ppp, rrr, ttt, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, yyy, ccc, sss, nil, rrr, ttt, nil, yyy, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		s.set("bb", zzz);
		checkTable(s, www, zzz, qqq, ddd, ppp, rrr, ttt, www, nil, qqq, ddd, ppp, nil, nil);
		checkTable(t, aaa, zzz, ccc, sss, nil, rrr, ttt, nil, zzz, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		s.set("ee", xxx);
		checkTable(s, www, zzz, qqq, ddd, xxx, rrr, ttt, www, nil, qqq, ddd, xxx, nil, nil);
		checkTable(t, aaa, zzz, ccc, sss, nil, rrr, ttt, nil, zzz, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, rrr, ttt, aaa, bbb, nil, nil, nil, rrr, ttt);
		s.set("ff", yyy);
		checkTable(s, www, zzz, qqq, ddd, xxx, yyy, ttt, www, nil, qqq, ddd, xxx, nil, nil);
		checkTable(t, aaa, zzz, ccc, sss, nil, yyy, ttt, nil, zzz, ccc, sss, nil, nil, nil);
		checkTable(m, aaa, bbb, nil, nil, nil, yyy, ttt, aaa, bbb, nil, nil, nil, yyy, ttt);


	}

}
