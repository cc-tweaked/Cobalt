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
package org.squiddev.cobalt.vm;

import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.function.*;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.ValueFactory.valueOf;

public class LuaOperationsTest {
	private final int sampleint = 77;
	private final long samplelong = 123400000000L;
	private final double sampledouble = 55.25;
	private final String samplestringstring = "abcdef";
	private final String samplestringint = String.valueOf(sampleint);
	private final String samplestringlong = String.valueOf(samplelong);
	private final String samplestringdouble = String.valueOf(sampledouble);
	private final Object sampleobject = new Object();
	private final TypeTest.MyData sampledata = new TypeTest.MyData();

	private final LuaValue somenil = NIL;
	private final LuaValue sometrue = Constants.TRUE;
	private final LuaValue somefalse = Constants.FALSE;
	private final LuaValue zero = Constants.ZERO;
	private final LuaValue intint = valueOf(sampleint);
	private final LuaValue longdouble = valueOf(samplelong);
	private final LuaValue doubledouble = valueOf(sampledouble);
	private final LuaValue stringstring = valueOf(samplestringstring);
	private final LuaValue stringint = valueOf(samplestringint);
	private final LuaValue stringlong = valueOf(samplestringlong);
	private final LuaValue stringdouble = valueOf(samplestringdouble);
	private final LuaTable table = ValueFactory.listOf(new LuaValue[]{valueOf("aaa"), valueOf("bbb")});
	private final LuaFunction somefunc = LibFunction.create(s -> NIL);

	{
		somefunc.setfenv(table);
	}

	private final LuaState state = new LuaState();
	private final LuaThread thread = new LuaThread(state, somefunc, table);
	private final Prototype proto = DataFactory.prototype();
	private final LuaClosure someclosure = new LuaInterpretedFunction(proto, table);
	private final LuaUserdata userdataobj = ValueFactory.userdataOf(sampleobject);
	private final LuaUserdata userdatacls = ValueFactory.userdataOf(sampledata);

	@Test
	public void testLength() throws LuaError, UnwindThrowable {
		var length = LuaOperators.createUnOp(state, "#");
		assertThrows(LuaError.class, () -> length.apply(somenil));
		assertThrows(LuaError.class, () -> length.apply(sometrue));
		assertThrows(LuaError.class, () -> length.apply(somefalse));
		assertThrows(LuaError.class, () -> length.apply(zero));
		assertThrows(LuaError.class, () -> length.apply(intint));
		assertThrows(LuaError.class, () -> length.apply(longdouble));
		assertThrows(LuaError.class, () -> length.apply(doubledouble));
		assertEquals(samplestringstring.length(), length.apply(stringstring).toInteger());
		assertEquals(samplestringint.length(), length.apply(stringint).toInteger());
		assertEquals(samplestringlong.length(), length.apply(stringlong).toInteger());
		assertEquals(samplestringdouble.length(), length.apply(stringdouble).toInteger());
		assertEquals(2, table.length());
		assertThrows(LuaError.class, () -> length.apply(somefunc));
		assertThrows(LuaError.class, () -> length.apply(thread));
		assertThrows(LuaError.class, () -> length.apply(someclosure));
		assertThrows(LuaError.class, () -> length.apply(userdataobj));
		assertThrows(LuaError.class, () -> length.apply(userdatacls));
	}

	@Test
	public void testGetfenv() {
		assertSame(NIL, sometrue.getfenv());
		assertSame(NIL, somefalse.getfenv());
		assertSame(NIL, zero.getfenv());
		assertSame(NIL, intint.getfenv());
		assertSame(NIL, longdouble.getfenv());
		assertSame(NIL, doubledouble.getfenv());
		assertSame(NIL, stringstring.getfenv());
		assertSame(NIL, stringint.getfenv());
		assertSame(NIL, stringlong.getfenv());
		assertSame(NIL, stringdouble.getfenv());
		assertSame(NIL, table.getfenv());
		assertSame(table, thread.getfenv());
		assertSame(table, someclosure.getfenv());
		assertSame(table, somefunc.getfenv());
		assertSame(NIL, userdataobj.getfenv());
		assertSame(NIL, userdatacls.getfenv());
	}

	@Test
	public void testSetfenv() {
		LuaTable table2 = ValueFactory.listOf(valueOf("ccc"), valueOf("ddd"));
		assertFalse(somenil.setfenv(table2));
		assertFalse(sometrue.setfenv(table2));
		assertFalse(somefalse.setfenv(table2));
		assertFalse(zero.setfenv(table2));
		assertFalse(intint.setfenv(table2));
		assertFalse(longdouble.setfenv(table2));
		assertFalse(doubledouble.setfenv(table2));
		assertFalse(stringstring.setfenv(table2));
		assertFalse(stringint.setfenv(table2));
		assertFalse(stringlong.setfenv(table2));
		assertFalse(stringdouble.setfenv(table2));
		assertFalse(table.setfenv(table2));
		thread.setfenv(table2);
		assertSame(table2, thread.getfenv());
		assertSame(table, someclosure.getfenv());
		assertSame(table, somefunc.getfenv());
		someclosure.setfenv(table2);
		assertSame(table2, someclosure.getfenv());
		assertSame(table, somefunc.getfenv());
		somefunc.setfenv(table2);
		assertSame(table2, somefunc.getfenv());
		assertFalse(userdataobj.setfenv(table2));
		assertFalse(userdatacls.setfenv(table2));
	}

	public Prototype createPrototype(String script, String name) {
		try {
			InputStream is = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
			return LuaC.compile(is, name);
		} catch (CompileException | LuaError e) {
			throw new IllegalStateException("Failed to compile " + name, e);
		}
	}

	@Test
	public void testFunctionClosureThreadEnv() throws LuaError, UnwindThrowable, InterruptedException {
		// set up suitable environments for execution
		LuaValue aaa = valueOf("aaa");
		LuaValue eee = valueOf("eee");
		LuaTable _G = CoreLibraries.standardGlobals(state);
		LuaTable newenv = ValueFactory.tableOf(valueOf("a"), valueOf("aaa"),
			valueOf("b"), valueOf("bbb"));
		LuaTable mt = ValueFactory.tableOf(Constants.INDEX, _G);
		newenv.setMetatable(state, mt);
		OperationHelper.setTable(state, _G, valueOf("a"), aaa);
		OperationHelper.setTable(state, newenv, valueOf("a"), eee);

		// function tests
		{
			LuaFunction f = new VarArgFunction() {
				{
					setfenv(_G);
				}

				@Override
				public Varargs invoke(LuaState state, Varargs varargs) {
					return getfenv().rawget(valueOf("a"));
				}
			};
			assertEquals(aaa, f.call(state));
			f.setfenv(newenv);
			assertEquals(newenv, f.getfenv());
			assertEquals(eee, f.call(state));
		}

		// closure tests
		{
			Prototype p = createPrototype("return a\n", "closuretester");
			LuaClosure c = new LuaInterpretedFunction(p, _G);
			assertEquals(aaa, c.call(state));
			c.setfenv(newenv);
			assertEquals(newenv, c.getfenv());
			assertEquals(eee, c.call(state));
		}

		// thread tests, functions created in threads inherit the thread's environment initially
		// those closures created not in any other function get the thread's enviroment
		Prototype p2 = createPrototype("return loadstring('return a')", "threadtester");
		{
			LuaThread t = new LuaThread(state, new LuaInterpretedFunction(p2, _G), _G);
			Varargs v = LuaThread.run(t, Constants.NONE);
			LuaValue f = v.arg(1);
			assertEquals(Constants.TFUNCTION, f.type());
			assertEquals(aaa, OperationHelper.call(state, f));
			assertEquals(_G, f.getfenv());
		}
		{
			// change the thread environment after creation!
			LuaThread t = new LuaThread(state, new LuaInterpretedFunction(p2, _G), _G);
			t.setfenv(newenv);
			Varargs v = LuaThread.run(t, Constants.NONE);
			LuaValue f = v.arg(1);
			assertEquals(Constants.TFUNCTION, f.type());
			assertEquals(eee, OperationHelper.call(state, f));
			assertEquals(newenv, f.getfenv());
		}
		{
			// let the closure have a different environment from the thread
			Prototype p3 = createPrototype("return function() return a end", "envtester");
			LuaThread t = new LuaThread(state, new LuaInterpretedFunction(p3, newenv), _G);
			Varargs v = LuaThread.run(t, Constants.NONE);
			LuaValue f = v.arg(1);
			assertEquals(Constants.TFUNCTION, f.type());
			assertEquals(eee, OperationHelper.call(state, f));
			assertEquals(newenv, f.getfenv());
		}
	}
}
