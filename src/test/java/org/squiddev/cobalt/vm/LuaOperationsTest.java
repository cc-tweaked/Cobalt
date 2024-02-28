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
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.LuaInterpretedFunction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.ValueFactory.valueOf;

public class LuaOperationsTest {
	private interface Constructor<T> {
		T construct() throws Exception;
	}

	private static <T> T safeConstruct(Constructor<T> c) {
		try {
			return c.construct();
		} catch (Exception e) {
			throw e instanceof RuntimeException re ? re : new RuntimeException(e);
		}
	}

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
	private final LuaTable table = safeConstruct(() -> ValueFactory.listOf(valueOf("aaa"), valueOf("bbb")));
	private final LuaFunction somefunc = LibFunction.create(s -> NIL);

	private final LuaState state = new LuaState();
	private final LuaThread thread = new LuaThread(state, somefunc);
	private final Prototype proto = DataFactory.prototype(state);
	private final LuaClosure someclosure = new LuaInterpretedFunction(proto);
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

	public Prototype createPrototype(String script, String name) {
		try {
			InputStream is = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
			return LuaC.compile(state, is, name);
		} catch (CompileException | LuaError e) {
			throw new IllegalStateException("Failed to compile " + name, e);
		}
	}
}
