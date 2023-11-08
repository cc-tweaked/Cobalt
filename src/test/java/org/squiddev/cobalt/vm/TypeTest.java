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
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;
import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.*;

public class TypeTest {

	private final int sampleint = 77;
	private final long samplelong = 123400000000L;
	private final double sampledouble = 55.25;
	private final String samplestringstring = "abcdef";
	private final String samplestringint = String.valueOf(sampleint);
	private final String samplestringlong = String.valueOf(samplelong);
	private final String samplestringdouble = String.valueOf(sampledouble);
	private final Object sampleobject = new Object();
	private final MyData sampledata = new MyData();

	private final LuaValue somenil = NIL;
	private final LuaValue sometrue = TRUE;
	private final LuaValue somefalse = FALSE;
	private final LuaValue zero = ZERO;
	private final LuaValue intint = valueOf(sampleint);
	private final LuaValue longdouble = valueOf(samplelong);
	private final LuaValue doubledouble = valueOf(sampledouble);
	private final LuaValue stringstring = valueOf(samplestringstring);
	private final LuaValue stringint = valueOf(samplestringint);
	private final LuaValue stringlong = valueOf(samplestringlong);
	private final LuaValue stringdouble = valueOf(samplestringdouble);
	private final LuaTable table = tableOf();
	private final LuaFunction somefunc = LibFunction.create(s -> NIL);
	private final LuaState state = new LuaState();
	private final LuaThread thread = new LuaThread(state, somefunc);
	private final LuaClosure someclosure = DataFactory.closure(state);
	private final LuaUserdata userdataobj = userdataOf(sampleobject);
	private final LuaUserdata userdatacls = userdataOf(sampledata);

	public static final class MyData {
		public MyData() {
		}
	}

	// ===================== type checks =======================

	@Test
	public void testIsNil() {
		assertTrue(somenil.isNil());
		assertFalse(sometrue.isNil());
		assertFalse(somefalse.isNil());
		assertFalse(zero.isNil());
		assertFalse(intint.isNil());
		assertFalse(longdouble.isNil());
		assertFalse(doubledouble.isNil());
		assertFalse(stringstring.isNil());
		assertFalse(stringint.isNil());
		assertFalse(stringlong.isNil());
		assertFalse(stringdouble.isNil());
		assertFalse(thread.isNil());
		assertFalse(table.isNil());
		assertFalse(userdataobj.isNil());
		assertFalse(userdatacls.isNil());
		assertFalse(somefunc.isNil());
		assertFalse(someclosure.isNil());
	}

	@Test
	public void testIsNumber() {
		assertFalse(somenil.isNumber());
		assertFalse(sometrue.isNumber());
		assertFalse(somefalse.isNumber());
		assertTrue(zero.isNumber());
		assertTrue(intint.isNumber());
		assertTrue(longdouble.isNumber());
		assertTrue(doubledouble.isNumber());
		assertFalse(stringstring.isNumber());
		assertTrue(stringint.isNumber());
		assertTrue(stringlong.isNumber());
		assertTrue(stringdouble.isNumber());
		assertFalse(thread.isNumber());
		assertFalse(table.isNumber());
		assertFalse(userdataobj.isNumber());
		assertFalse(userdatacls.isNumber());
		assertFalse(somefunc.isNumber());
		assertFalse(someclosure.isNumber());
	}

	@Test
	public void testIsString() {
		assertFalse(somenil.isString());
		assertFalse(sometrue.isString());
		assertFalse(somefalse.isString());
		assertTrue(zero.isString());
		assertTrue(longdouble.isString());
		assertTrue(doubledouble.isString());
		assertTrue(stringstring.isString());
		assertTrue(stringint.isString());
		assertTrue(stringlong.isString());
		assertTrue(stringdouble.isString());
		assertFalse(thread.isString());
		assertFalse(table.isString());
		assertFalse(userdataobj.isString());
		assertFalse(userdatacls.isString());
		assertFalse(somefunc.isString());
		assertFalse(someclosure.isString());
	}

	@Test
	public void testIsThread() {
		assertFalse(somenil.isThread());
		assertFalse(sometrue.isThread());
		assertFalse(somefalse.isThread());
		assertFalse(intint.isThread());
		assertFalse(longdouble.isThread());
		assertFalse(doubledouble.isThread());
		assertFalse(stringstring.isThread());
		assertFalse(stringint.isThread());
		assertFalse(stringdouble.isThread());
		assertTrue(thread.isThread());
		assertFalse(table.isThread());
		assertFalse(userdataobj.isThread());
		assertFalse(userdatacls.isThread());
		assertFalse(somefunc.isThread());
		assertFalse(someclosure.isThread());
	}


	// ===================== Coerce to Java =======================
	@Test
	public void testToBoolean() {
		assertFalse(somenil.toBoolean());
		assertTrue(sometrue.toBoolean());
		assertFalse(somefalse.toBoolean());
		assertTrue(zero.toBoolean());
		assertTrue(intint.toBoolean());
		assertTrue(longdouble.toBoolean());
		assertTrue(doubledouble.toBoolean());
		assertTrue(stringstring.toBoolean());
		assertTrue(stringint.toBoolean());
		assertTrue(stringlong.toBoolean());
		assertTrue(stringdouble.toBoolean());
		assertTrue(thread.toBoolean());
		assertTrue(table.toBoolean());
		assertTrue(userdataobj.toBoolean());
		assertTrue(userdatacls.toBoolean());
		assertTrue(somefunc.toBoolean());
		assertTrue(someclosure.toBoolean());
	}

	@Test
	public void testToDouble() {
		assertDoubleEquals(Double.NaN, somenil.toDouble());
		assertDoubleEquals(Double.NaN, somefalse.toDouble());
		assertDoubleEquals(Double.NaN, sometrue.toDouble());
		assertDoubleEquals(0, zero.toDouble());
		assertDoubleEquals(sampleint, intint.toDouble());
		assertDoubleEquals((double) samplelong, longdouble.toDouble());
		assertDoubleEquals(sampledouble, doubledouble.toDouble());
		assertTrue(Double.isNaN(stringstring.toDouble()));
		assertDoubleEquals(sampleint, stringint.toDouble());
		assertDoubleEquals((double) samplelong, stringlong.toDouble());
		assertDoubleEquals(sampledouble, stringdouble.toDouble());
		assertDoubleEquals(Double.NaN, thread.toDouble());
		assertDoubleEquals(Double.NaN, table.toDouble());
		assertDoubleEquals(Double.NaN, userdataobj.toDouble());
		assertDoubleEquals(Double.NaN, userdatacls.toDouble());
		assertDoubleEquals(Double.NaN, somefunc.toDouble());
		assertDoubleEquals(Double.NaN, someclosure.toDouble());
	}

	@Test
	public void testToInt() {
		assertEquals(0, somenil.toInteger());
		assertEquals(0, somefalse.toInteger());
		assertEquals(0, sometrue.toInteger());
		assertEquals(0, zero.toInteger());
		assertEquals(sampleint, intint.toInteger());
		assertEquals((int) samplelong, longdouble.toInteger());
		assertEquals((int) sampledouble, doubledouble.toInteger());
		assertEquals(0, stringstring.toInteger());
		assertEquals(sampleint, stringint.toInteger());
		assertEquals((int) samplelong, stringlong.toInteger());
		assertEquals((int) sampledouble, stringdouble.toInteger());
		assertEquals(0, thread.toInteger());
		assertEquals(0, table.toInteger());
		assertEquals(0, userdataobj.toInteger());
		assertEquals(0, userdatacls.toInteger());
		assertEquals(0, somefunc.toInteger());
		assertEquals(0, someclosure.toInteger());
	}

	@Test
	public void testToString() {
		assertEquals("nil", somenil.toString());
		assertEquals("false", somefalse.toString());
		assertEquals("true", sometrue.toString());
		assertEquals("0", zero.toString());
		assertEquals(String.valueOf(sampleint), intint.toString());
		assertEquals(String.valueOf(samplelong), longdouble.toString());
		assertEquals(String.valueOf(sampledouble), doubledouble.toString());
		assertEquals(samplestringstring, stringstring.toString());
		assertEquals(String.valueOf(sampleint), stringint.toString());
		assertEquals(String.valueOf(samplelong), stringlong.toString());
		assertEquals(String.valueOf(sampledouble), stringdouble.toString());
		assertEquals("thread: ", thread.toString().substring(0, 8));
		assertEquals("table: ", table.toString().substring(0, 7));
		assertEquals("userdata: ", userdataobj.toString().substring(0, 10));
		assertEquals("function: ", somefunc.toString().substring(0, 10));
		assertEquals("function: ", someclosure.toString().substring(0, 10));
	}

	// ===================== Optional argument conversion =======================


	private void throwsError(LuaValue obj, String method, Class<?> argtype, Object argument) {
		try {
			obj.getClass().getMethod(method, argtype).invoke(obj, argument);
		} catch (InvocationTargetException e) {
			if (!(e.getTargetException() instanceof LuaError)) {
				fail("not a LuaError: " + e.getTargetException());
			}
			return; // pass
		} catch (Exception e) {
			fail("bad exception: " + e);
		}
		fail("failed to throw LuaError as required");
	}

	@Test
	public void testOptBoolean() throws LuaError {
		assertTrue(somenil.optBoolean(true));
		assertFalse(somenil.optBoolean(false));
		assertTrue(sometrue.optBoolean(false));
		assertFalse(somefalse.optBoolean(true));
		throwsError(zero, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(intint, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(longdouble, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(doubledouble, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(somefunc, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(someclosure, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(stringstring, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(stringint, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(stringlong, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(stringdouble, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(thread, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(table, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(userdataobj, "optBoolean", boolean.class, Boolean.FALSE);
		throwsError(userdatacls, "optBoolean", boolean.class, Boolean.FALSE);
	}

	@Test
	public void testOptDouble() throws LuaError {
		assertDoubleEquals(33., somenil.optDouble(33.));
		throwsError(sometrue, "optDouble", double.class, 33.);
		throwsError(somefalse, "optDouble", double.class, 33.);
		assertDoubleEquals(0., zero.optDouble(33.));
		assertDoubleEquals(sampleint, intint.optDouble(33.));
		assertDoubleEquals((double) samplelong, longdouble.optDouble(33.));
		assertDoubleEquals(sampledouble, doubledouble.optDouble(33.));
		throwsError(somefunc, "optDouble", double.class, 33.);
		throwsError(someclosure, "optDouble", double.class, 33.);
		throwsError(stringstring, "optDouble", double.class, 33.);
		assertDoubleEquals(sampleint, stringint.optDouble(33.));
		assertDoubleEquals((double) samplelong, stringlong.optDouble(33.));
		assertDoubleEquals(sampledouble, stringdouble.optDouble(33.));
		throwsError(thread, "optDouble", double.class, 33.);
		throwsError(table, "optDouble", double.class, 33.);
		throwsError(userdataobj, "optDouble", double.class, 33.);
		throwsError(userdatacls, "optDouble", double.class, 33.);
	}

	@Test
	public void testOptFunction() throws LuaError {
		assertEquals(somefunc, somenil.optFunction(somefunc));
		assertNull(somenil.optFunction(null));
		throwsError(sometrue, "optFunction", LuaFunction.class, somefunc);
		throwsError(somefalse, "optFunction", LuaFunction.class, somefunc);
		throwsError(zero, "optFunction", LuaFunction.class, somefunc);
		throwsError(intint, "optFunction", LuaFunction.class, somefunc);
		throwsError(longdouble, "optFunction", LuaFunction.class, somefunc);
		throwsError(doubledouble, "optFunction", LuaFunction.class, somefunc);
		assertEquals(somefunc, somefunc.optFunction(null));
		assertEquals(someclosure, someclosure.optFunction(null));
		assertEquals(somefunc, somefunc.optFunction(somefunc));
		assertEquals(someclosure, someclosure.optFunction(somefunc));
		throwsError(stringstring, "optFunction", LuaFunction.class, somefunc);
		throwsError(stringint, "optFunction", LuaFunction.class, somefunc);
		throwsError(stringlong, "optFunction", LuaFunction.class, somefunc);
		throwsError(stringdouble, "optFunction", LuaFunction.class, somefunc);
		throwsError(thread, "optFunction", LuaFunction.class, somefunc);
		throwsError(table, "optFunction", LuaFunction.class, somefunc);
		throwsError(userdataobj, "optFunction", LuaFunction.class, somefunc);
		throwsError(userdatacls, "optFunction", LuaFunction.class, somefunc);
	}

	@Test
	public void testOptInt() throws LuaError {
		assertEquals(33, somenil.optInteger(33));
		throwsError(sometrue, "optInteger", int.class, 33);
		throwsError(somefalse, "optInteger", int.class, 33);
		assertEquals(0, zero.optInteger(33));
		assertEquals(sampleint, intint.optInteger(33));
		assertEquals((int) samplelong, longdouble.optInteger(33));
		assertEquals((int) sampledouble, doubledouble.optInteger(33));
		throwsError(somefunc, "optInteger", int.class, 33);
		throwsError(someclosure, "optInteger", int.class, 33);
		throwsError(stringstring, "optInteger", int.class, 33);
		assertEquals(sampleint, stringint.optInteger(33));
		assertEquals((int) samplelong, stringlong.optInteger(33));
		assertEquals((int) sampledouble, stringdouble.optInteger(33));
		throwsError(thread, "optInteger", int.class, 33);
		throwsError(table, "optInteger", int.class, 33);
		throwsError(userdataobj, "optInteger", int.class, 33);
		throwsError(userdatacls, "optInteger", int.class, 33);
	}

	@Test
	public void testOptLong() throws LuaError {
		assertEquals(33L, somenil.optLong(33));
		throwsError(sometrue, "optLong", long.class, (long) 33);
		throwsError(somefalse, "optLong", long.class, (long) 33);
		assertEquals(0L, zero.optLong(33));
		assertEquals(sampleint, intint.optLong(33));
		assertEquals(samplelong, longdouble.optLong(33));
		assertEquals((long) sampledouble, doubledouble.optLong(33));
		throwsError(somefunc, "optLong", long.class, (long) 33);
		throwsError(someclosure, "optLong", long.class, (long) 33);
		throwsError(stringstring, "optLong", long.class, (long) 33);
		assertEquals(sampleint, stringint.optLong(33));
		assertEquals(samplelong, stringlong.optLong(33));
		assertEquals((long) sampledouble, stringdouble.optLong(33));
		throwsError(thread, "optLong", long.class, (long) 33);
		throwsError(table, "optLong", long.class, (long) 33);
		throwsError(userdataobj, "optLong", long.class, (long) 33);
		throwsError(userdatacls, "optLong", long.class, (long) 33);
	}

	@Test
	public void testOptNumber() throws LuaError {
		assertEquals(valueOf(33), somenil.optNumber(valueOf(33)));
		throwsError(sometrue, "optNumber", LuaNumber.class, valueOf(33));
		throwsError(somefalse, "optNumber", LuaNumber.class, valueOf(33));
		assertEquals(zero, zero.optNumber(valueOf(33)));
		assertEquals(valueOf(sampleint), intint.optNumber(valueOf(33)));
		assertEquals(valueOf(samplelong), longdouble.optNumber(valueOf(33)));
		assertEquals(valueOf(sampledouble), doubledouble.optNumber(valueOf(33)));
		throwsError(somefunc, "optNumber", LuaNumber.class, valueOf(33));
		throwsError(someclosure, "optNumber", LuaNumber.class, valueOf(33));
		throwsError(stringstring, "optNumber", LuaNumber.class, valueOf(33));
		assertEquals(valueOf(sampleint), stringint.optNumber(valueOf(33)));
		assertEquals(valueOf(samplelong), stringlong.optNumber(valueOf(33)));
		assertEquals(valueOf(sampledouble), stringdouble.optNumber(valueOf(33)));
		throwsError(thread, "optNumber", LuaNumber.class, valueOf(33));
		throwsError(table, "optNumber", LuaNumber.class, valueOf(33));
		throwsError(userdataobj, "optNumber", LuaNumber.class, valueOf(33));
		throwsError(userdatacls, "optNumber", LuaNumber.class, valueOf(33));
	}

	@Test
	public void testOptTable() throws LuaError {
		assertEquals(table, somenil.optTable(table));
		assertNull(somenil.optTable(null));
		throwsError(sometrue, "optTable", LuaTable.class, table);
		throwsError(somefalse, "optTable", LuaTable.class, table);
		throwsError(zero, "optTable", LuaTable.class, table);
		throwsError(intint, "optTable", LuaTable.class, table);
		throwsError(longdouble, "optTable", LuaTable.class, table);
		throwsError(doubledouble, "optTable", LuaTable.class, table);
		throwsError(somefunc, "optTable", LuaTable.class, table);
		throwsError(someclosure, "optTable", LuaTable.class, table);
		throwsError(stringstring, "optTable", LuaTable.class, table);
		throwsError(stringint, "optTable", LuaTable.class, table);
		throwsError(stringlong, "optTable", LuaTable.class, table);
		throwsError(stringdouble, "optTable", LuaTable.class, table);
		throwsError(thread, "optTable", LuaTable.class, table);
		assertEquals(table, table.optTable(table));
		assertEquals(table, table.optTable(null));
		throwsError(userdataobj, "optTable", LuaTable.class, table);
		throwsError(userdatacls, "optTable", LuaTable.class, table);
	}

	@Test
	public void testOptThread() throws LuaError {
		assertEquals(thread, somenil.optThread(thread));
		assertNull(somenil.optThread(null));
		throwsError(sometrue, "optThread", LuaThread.class, thread);
		throwsError(somefalse, "optThread", LuaThread.class, thread);
		throwsError(zero, "optThread", LuaThread.class, thread);
		throwsError(intint, "optThread", LuaThread.class, thread);
		throwsError(longdouble, "optThread", LuaThread.class, thread);
		throwsError(doubledouble, "optThread", LuaThread.class, thread);
		throwsError(somefunc, "optThread", LuaThread.class, thread);
		throwsError(someclosure, "optThread", LuaThread.class, thread);
		throwsError(stringstring, "optThread", LuaThread.class, thread);
		throwsError(stringint, "optThread", LuaThread.class, thread);
		throwsError(stringlong, "optThread", LuaThread.class, thread);
		throwsError(stringdouble, "optThread", LuaThread.class, thread);
		throwsError(table, "optThread", LuaThread.class, thread);
		assertEquals(thread, thread.optThread(thread));
		assertEquals(thread, thread.optThread(null));
		throwsError(userdataobj, "optThread", LuaThread.class, thread);
		throwsError(userdatacls, "optThread", LuaThread.class, thread);
	}

	@Test
	public void testOptJavaString() throws LuaError {
		assertEquals("xyz", somenil.optString("xyz"));
		assertNull(somenil.optString(null));
		throwsError(sometrue, "optString", String.class, "xyz");
		throwsError(somefalse, "optString", String.class, "xyz");
		assertEquals(String.valueOf(zero), zero.optString("xyz"));
		assertEquals(String.valueOf(intint), intint.optString("xyz"));
		assertEquals(String.valueOf(longdouble), longdouble.optString("xyz"));
		assertEquals(String.valueOf(doubledouble), doubledouble.optString("xyz"));
		throwsError(somefunc, "optString", String.class, "xyz");
		throwsError(someclosure, "optString", String.class, "xyz");
		assertEquals(samplestringstring, stringstring.optString("xyz"));
		assertEquals(samplestringint, stringint.optString("xyz"));
		assertEquals(samplestringlong, stringlong.optString("xyz"));
		assertEquals(samplestringdouble, stringdouble.optString("xyz"));
		throwsError(thread, "optString", String.class, "xyz");
		throwsError(table, "optString", String.class, "xyz");
		throwsError(userdataobj, "optString", String.class, "xyz");
		throwsError(userdatacls, "optString", String.class, "xyz");
	}

	@Test
	public void testOptLuaString() throws LuaError {
		assertEquals(valueOf("xyz"), somenil.optLuaString(valueOf("xyz")));
		assertNull(somenil.optLuaString(null));
		throwsError(sometrue, "optLuaString", LuaString.class, valueOf("xyz"));
		throwsError(somefalse, "optLuaString", LuaString.class, valueOf("xyz"));
		assertEquals(valueOf("0"), zero.optLuaString(valueOf("xyz")));
		assertEquals(stringint, intint.optLuaString(valueOf("xyz")));
		assertEquals(stringlong, longdouble.optLuaString(valueOf("xyz")));
		assertEquals(stringdouble, doubledouble.optLuaString(valueOf("xyz")));
		throwsError(somefunc, "optLuaString", LuaString.class, valueOf("xyz"));
		throwsError(someclosure, "optLuaString", LuaString.class, valueOf("xyz"));
		assertEquals(stringstring, stringstring.optLuaString(valueOf("xyz")));
		assertEquals(stringint, stringint.optLuaString(valueOf("xyz")));
		assertEquals(stringlong, stringlong.optLuaString(valueOf("xyz")));
		assertEquals(stringdouble, stringdouble.optLuaString(valueOf("xyz")));
		throwsError(thread, "optLuaString", LuaString.class, valueOf("xyz"));
		throwsError(table, "optLuaString", LuaString.class, valueOf("xyz"));
		throwsError(userdataobj, "optLuaString", LuaString.class, valueOf("xyz"));
		throwsError(userdatacls, "optLuaString", LuaString.class, valueOf("xyz"));
	}

	@Test
	public void testOptValue() {
		assertEquals(zero, somenil.optValue(zero));
		assertEquals(stringstring, somenil.optValue(stringstring));
		assertEquals(sometrue, sometrue.optValue(TRUE));
		assertEquals(somefalse, somefalse.optValue(TRUE));
		assertEquals(zero, zero.optValue(TRUE));
		assertEquals(intint, intint.optValue(TRUE));
		assertEquals(longdouble, longdouble.optValue(TRUE));
		assertEquals(somefunc, somefunc.optValue(TRUE));
		assertEquals(someclosure, someclosure.optValue(TRUE));
		assertEquals(stringstring, stringstring.optValue(TRUE));
		assertEquals(stringint, stringint.optValue(TRUE));
		assertEquals(stringlong, stringlong.optValue(TRUE));
		assertEquals(stringdouble, stringdouble.optValue(TRUE));
		assertEquals(thread, thread.optValue(TRUE));
		assertEquals(table, table.optValue(TRUE));
		assertEquals(userdataobj, userdataobj.optValue(TRUE));
		assertEquals(userdatacls, userdatacls.optValue(TRUE));
	}


	// ===================== Required argument conversion =======================


	private void throwsErrorReq(LuaValue obj, String method) {
		try {
			obj.getClass().getMethod(method).invoke(obj);
		} catch (InvocationTargetException e) {
			if (!(e.getTargetException() instanceof LuaError)) {
				fail("not a LuaError: " + e.getTargetException());
			}
			return; // pass
		} catch (Exception e) {
			fail("bad exception: " + e);
		}
		fail("failed to throw LuaError as required");
	}

	@Test
	public void testCheckBoolean() throws LuaError {
		throwsErrorReq(somenil, "checkBoolean");
		assertTrue(sometrue.checkBoolean());
		assertFalse(somefalse.checkBoolean());
		throwsErrorReq(zero, "checkBoolean");
		throwsErrorReq(intint, "checkBoolean");
		throwsErrorReq(longdouble, "checkBoolean");
		throwsErrorReq(doubledouble, "checkBoolean");
		throwsErrorReq(somefunc, "checkBoolean");
		throwsErrorReq(someclosure, "checkBoolean");
		throwsErrorReq(stringstring, "checkBoolean");
		throwsErrorReq(stringint, "checkBoolean");
		throwsErrorReq(stringlong, "checkBoolean");
		throwsErrorReq(stringdouble, "checkBoolean");
		throwsErrorReq(thread, "checkBoolean");
		throwsErrorReq(table, "checkBoolean");
		throwsErrorReq(userdataobj, "checkBoolean");
		throwsErrorReq(userdatacls, "checkBoolean");
	}

	@Test
	public void testCheckDouble() throws LuaError {
		throwsErrorReq(somenil, "checkDouble");
		throwsErrorReq(sometrue, "checkDouble");
		throwsErrorReq(somefalse, "checkDouble");
		assertDoubleEquals(0., zero.checkDouble());
		assertDoubleEquals(sampleint, intint.checkDouble());
		assertDoubleEquals((double) samplelong, longdouble.checkDouble());
		assertDoubleEquals(sampledouble, doubledouble.checkDouble());
		throwsErrorReq(somefunc, "checkDouble");
		throwsErrorReq(someclosure, "checkDouble");
		throwsErrorReq(stringstring, "checkDouble");
		assertDoubleEquals(sampleint, stringint.checkDouble());
		assertDoubleEquals((double) samplelong, stringlong.checkDouble());
		assertDoubleEquals(sampledouble, stringdouble.checkDouble());
		throwsErrorReq(thread, "checkDouble");
		throwsErrorReq(table, "checkDouble");
		throwsErrorReq(userdataobj, "checkDouble");
		throwsErrorReq(userdatacls, "checkDouble");
	}

	@Test
	public void testCheckFunction() {
		throwsErrorReq(somenil, "checkFunction");
		throwsErrorReq(sometrue, "checkFunction");
		throwsErrorReq(somefalse, "checkFunction");
		throwsErrorReq(zero, "checkFunction");
		throwsErrorReq(intint, "checkFunction");
		throwsErrorReq(longdouble, "checkFunction");
		throwsErrorReq(doubledouble, "checkFunction");
		assertEquals(somefunc, somefunc.checkFunction());
		assertEquals(someclosure, someclosure.checkFunction());
		assertEquals(somefunc, somefunc.checkFunction());
		assertEquals(someclosure, someclosure.checkFunction());
		throwsErrorReq(stringstring, "checkFunction");
		throwsErrorReq(stringint, "checkFunction");
		throwsErrorReq(stringlong, "checkFunction");
		throwsErrorReq(stringdouble, "checkFunction");
		throwsErrorReq(thread, "checkFunction");
		throwsErrorReq(table, "checkFunction");
		throwsErrorReq(userdataobj, "checkFunction");
		throwsErrorReq(userdatacls, "checkFunction");
	}

	@Test
	public void testCheckInt() throws LuaError {
		throwsErrorReq(somenil, "checkInteger");
		throwsErrorReq(sometrue, "checkInteger");
		throwsErrorReq(somefalse, "checkInteger");
		assertEquals(0, zero.checkInteger());
		assertEquals(sampleint, intint.checkInteger());
		assertEquals((int) samplelong, longdouble.checkInteger());
		assertEquals((int) sampledouble, doubledouble.checkInteger());
		throwsErrorReq(somefunc, "checkInteger");
		throwsErrorReq(someclosure, "checkInteger");
		throwsErrorReq(stringstring, "checkInteger");
		assertEquals(sampleint, stringint.checkInteger());
		assertEquals((int) samplelong, stringlong.checkInteger());
		assertEquals((int) sampledouble, stringdouble.checkInteger());
		throwsErrorReq(thread, "checkInteger");
		throwsErrorReq(table, "checkInteger");
		throwsErrorReq(userdataobj, "checkInteger");
		throwsErrorReq(userdatacls, "checkInteger");
	}

	@Test
	public void testCheckLong() throws LuaError {
		throwsErrorReq(somenil, "checkLong");
		throwsErrorReq(sometrue, "checkLong");
		throwsErrorReq(somefalse, "checkLong");
		assertEquals(0L, zero.checkLong());
		assertEquals(sampleint, intint.checkLong());
		assertEquals(samplelong, longdouble.checkLong());
		assertEquals((long) sampledouble, doubledouble.checkLong());
		throwsErrorReq(somefunc, "checkLong");
		throwsErrorReq(someclosure, "checkLong");
		throwsErrorReq(stringstring, "checkLong");
		assertEquals(sampleint, stringint.checkLong());
		assertEquals(samplelong, stringlong.checkLong());
		assertEquals((long) sampledouble, stringdouble.checkLong());
		throwsErrorReq(thread, "checkLong");
		throwsErrorReq(table, "checkLong");
		throwsErrorReq(userdataobj, "checkLong");
		throwsErrorReq(userdatacls, "checkLong");
	}

	@Test
	public void testCheckNumber() throws LuaError {
		throwsErrorReq(somenil, "checkNumber");
		throwsErrorReq(sometrue, "checkNumber");
		throwsErrorReq(somefalse, "checkNumber");
		assertEquals(zero, zero.checkNumber());
		assertEquals(valueOf(sampleint), intint.checkNumber());
		assertEquals(valueOf(samplelong), longdouble.checkNumber());
		assertEquals(valueOf(sampledouble), doubledouble.checkNumber());
		throwsErrorReq(somefunc, "checkNumber");
		throwsErrorReq(someclosure, "checkNumber");
		throwsErrorReq(stringstring, "checkNumber");
		assertEquals(valueOf(sampleint), stringint.checkNumber());
		assertEquals(valueOf(samplelong), stringlong.checkNumber());
		assertEquals(valueOf(sampledouble), stringdouble.checkNumber());
		throwsErrorReq(thread, "checkNumber");
		throwsErrorReq(table, "checkNumber");
		throwsErrorReq(userdataobj, "checkNumber");
		throwsErrorReq(userdatacls, "checkNumber");
	}

	@Test
	public void testCheckTable() {
		throwsErrorReq(somenil, "checkTable");
		throwsErrorReq(sometrue, "checkTable");
		throwsErrorReq(somefalse, "checkTable");
		throwsErrorReq(zero, "checkTable");
		throwsErrorReq(intint, "checkTable");
		throwsErrorReq(longdouble, "checkTable");
		throwsErrorReq(doubledouble, "checkTable");
		throwsErrorReq(somefunc, "checkTable");
		throwsErrorReq(someclosure, "checkTable");
		throwsErrorReq(stringstring, "checkTable");
		throwsErrorReq(stringint, "checkTable");
		throwsErrorReq(stringlong, "checkTable");
		throwsErrorReq(stringdouble, "checkTable");
		throwsErrorReq(thread, "checkTable");
		assertEquals(table, table.checkTable());
		assertEquals(table, table.checkTable());
		throwsErrorReq(userdataobj, "checkTable");
		throwsErrorReq(userdatacls, "checkTable");
	}

	@Test
	public void testCheckThread() {
		throwsErrorReq(somenil, "checkThread");
		throwsErrorReq(sometrue, "checkThread");
		throwsErrorReq(somefalse, "checkThread");
		throwsErrorReq(zero, "checkThread");
		throwsErrorReq(intint, "checkThread");
		throwsErrorReq(longdouble, "checkThread");
		throwsErrorReq(doubledouble, "checkThread");
		throwsErrorReq(somefunc, "checkThread");
		throwsErrorReq(someclosure, "checkThread");
		throwsErrorReq(stringstring, "checkThread");
		throwsErrorReq(stringint, "checkThread");
		throwsErrorReq(stringlong, "checkThread");
		throwsErrorReq(stringdouble, "checkThread");
		throwsErrorReq(table, "checkThread");
		assertEquals(thread, thread.checkThread());
		assertEquals(thread, thread.checkThread());
		throwsErrorReq(userdataobj, "checkThread");
		throwsErrorReq(userdatacls, "checkThread");
	}

	@Test
	public void testCheckJavaString() throws LuaError {
		throwsErrorReq(somenil, "checkString");
		throwsErrorReq(sometrue, "checkString");
		throwsErrorReq(somefalse, "checkString");
		assertEquals(String.valueOf(zero), zero.checkString());
		assertEquals(String.valueOf(intint), intint.checkString());
		assertEquals(String.valueOf(longdouble), longdouble.checkString());
		assertEquals(String.valueOf(doubledouble), doubledouble.checkString());
		throwsErrorReq(somefunc, "checkString");
		throwsErrorReq(someclosure, "checkString");
		assertEquals(samplestringstring, stringstring.checkString());
		assertEquals(samplestringint, stringint.checkString());
		assertEquals(samplestringlong, stringlong.checkString());
		assertEquals(samplestringdouble, stringdouble.checkString());
		throwsErrorReq(thread, "checkString");
		throwsErrorReq(table, "checkString");
		throwsErrorReq(userdataobj, "checkString");
		throwsErrorReq(userdatacls, "checkString");
	}

	@Test
	public void testCheckLuaString() throws LuaError {
		throwsErrorReq(somenil, "checkLuaString");
		throwsErrorReq(sometrue, "checkLuaString");
		throwsErrorReq(somefalse, "checkLuaString");
		assertEquals(valueOf("0"), zero.checkLuaString());
		assertEquals(stringint, intint.checkLuaString());
		assertEquals(stringlong, longdouble.checkLuaString());
		assertEquals(stringdouble, doubledouble.checkLuaString());
		throwsErrorReq(somefunc, "checkLuaString");
		throwsErrorReq(someclosure, "checkLuaString");
		assertEquals(stringstring, stringstring.checkLuaString());
		assertEquals(stringint, stringint.checkLuaString());
		assertEquals(stringlong, stringlong.checkLuaString());
		assertEquals(stringdouble, stringdouble.checkLuaString());
		throwsErrorReq(thread, "checkLuaString");
		throwsErrorReq(table, "checkLuaString");
		throwsErrorReq(userdataobj, "checkLuaString");
		throwsErrorReq(userdatacls, "checkLuaString");
	}

	/**
	 * Really bad function to make it easier to compare doubles
	 *
	 * @param a First double
	 * @param b Second double
	 */
	public static void assertDoubleEquals(double a, double b) {
		assertEquals(a, b, 1e-10);
	}
}
