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

import org.junit.Assert;
import org.junit.Test;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;
import static org.luaj.vm2.Constants.*;
import static org.luaj.vm2.Factory.tableOf;
import static org.luaj.vm2.Factory.valueOf;

/**
 * Tests of basic unary and binary operators on main value types.
 */
public class UnaryBinaryOperatorsTest {
	@Test
	public void testEqualsBool() {
		Assert.assertEquals(FALSE, FALSE);
		assertEquals(TRUE, TRUE);
		assertTrue(FALSE.equals(FALSE));
		assertTrue(TRUE.equals(TRUE));
		assertTrue(!FALSE.equals(TRUE));
		assertTrue(!TRUE.equals(FALSE));
		assertTrue(FALSE.eq_b(FALSE));
		assertTrue(TRUE.eq_b(TRUE));
		assertFalse(FALSE.eq_b(TRUE));
		assertFalse(TRUE.eq_b(FALSE));
		assertEquals(TRUE, FALSE.eq(FALSE));
		assertEquals(TRUE, TRUE.eq(TRUE));
		assertEquals(FALSE, FALSE.eq(TRUE));
		assertEquals(FALSE, TRUE.eq(FALSE));
		assertFalse(FALSE.neq_b(FALSE));
		assertFalse(TRUE.neq_b(TRUE));
		assertTrue(FALSE.neq_b(TRUE));
		assertTrue(TRUE.neq_b(FALSE));
		assertEquals(FALSE, FALSE.neq(FALSE));
		assertEquals(FALSE, TRUE.neq(TRUE));
		assertEquals(TRUE, FALSE.neq(TRUE));
		assertEquals(TRUE, TRUE.neq(FALSE));
		assertTrue(TRUE.toboolean());
		assertFalse(FALSE.toboolean());
	}

	@Test
	public void testNot() {
		LuaValue ia = valueOf(3);
		LuaValue da = valueOf(.25);
		LuaValue sa = valueOf("1.5");
		LuaValue ba = TRUE, bb = FALSE;

		// like kinds
		assertEquals(FALSE, ia.not());
		assertEquals(FALSE, da.not());
		assertEquals(FALSE, sa.not());
		assertEquals(FALSE, ba.not());
		assertEquals(TRUE, bb.not());
	}

	@Test
	public void testNeg() {
		LuaValue ia = valueOf(3), ib = valueOf(-4);
		LuaValue da = valueOf(.25), db = valueOf(-.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("-2.0");

		// like kinds
		assertDoubleEquals(-3., ia.neg().todouble());
		assertDoubleEquals(-.25, da.neg().todouble());
		assertDoubleEquals(-1.5, sa.neg().todouble());
		assertDoubleEquals(4., ib.neg().todouble());
		assertDoubleEquals(.5, db.neg().todouble());
		assertDoubleEquals(2.0, sb.neg().todouble());
	}

	@Test
	public void testDoublesBecomeInts() {
		// DoubleValue.valueOf should return int
		LuaValue ia = LuaInteger.valueOf(345), da = LuaDouble.valueOf(345.0), db = LuaDouble.valueOf(345.5);
		LuaValue sa = valueOf("3.0"), sb = valueOf("3"), sc = valueOf("-2.0"), sd = valueOf("-2");

		assertEquals(ia, da);
		assertTrue(ia instanceof LuaInteger);
		assertTrue(da instanceof LuaInteger);
		assertTrue(db instanceof LuaDouble);
		assertEquals(ia.toint(), 345);
		assertEquals(da.toint(), 345);
		assertDoubleEquals(da.todouble(), 345.0);
		assertDoubleEquals(db.todouble(), 345.5);

		assertTrue(sa instanceof LuaString);
		assertTrue(sb instanceof LuaString);
		assertTrue(sc instanceof LuaString);
		assertTrue(sd instanceof LuaString);
		assertDoubleEquals(3., sa.todouble());
		assertDoubleEquals(3., sb.todouble());
		assertDoubleEquals(-2., sc.todouble());
		assertDoubleEquals(-2., sd.todouble());

	}


	@Test
	public void testEqualsInt() {
		LuaValue ia = LuaInteger.valueOf(345), ib = LuaInteger.valueOf(345), ic = LuaInteger.valueOf(-345);
		LuaString sa = LuaString.valueOf("345"), sb = LuaString.valueOf("345"), sc = LuaString.valueOf("-345");

		// objects should be different
		assertNotSame(ia, ib);
		assertSame(sa, sb);
		assertNotSame(ia, ic);
		assertNotSame(sa, sc);

		// assert equals for same type
		assertEquals(ia, ib);
		assertEquals(sa, sb);
		assertFalse(ia.equals(ic));
		assertFalse(sa.equals(sc));

		// check object equality for different types
		assertFalse(ia.equals(sa));
		assertFalse(sa.equals(ia));
	}

	@Test
	public void testEqualsDouble() {
		LuaValue da = LuaDouble.valueOf(345.5), db = LuaDouble.valueOf(345.5), dc = LuaDouble.valueOf(-345.5);
		LuaString sa = LuaString.valueOf("345.5"), sb = LuaString.valueOf("345.5"), sc = LuaString.valueOf("-345.5");

		// objects should be different
		assertNotSame(da, db);
		assertSame(sa, sb);
		assertNotSame(da, dc);
		assertNotSame(sa, sc);

		// assert equals for same type
		assertEquals(da, db);
		assertEquals(sa, sb);
		assertFalse(da.equals(dc));
		assertFalse(sa.equals(sc));

		// check object equality for different types
		assertFalse(da.equals(sa));
		assertFalse(sa.equals(da));
	}

	@Test
	public void testEqInt() {
		LuaValue ia = LuaInteger.valueOf(345), ib = LuaInteger.valueOf(345), ic = LuaInteger.valueOf(-123);
		LuaValue sa = LuaString.valueOf("345"), sb = LuaString.valueOf("345"), sc = LuaString.valueOf("-345");

		// check arithmetic equality among same types
		assertEquals(ia.eq(ib), TRUE);
		assertEquals(sa.eq(sb), TRUE);
		assertEquals(ia.eq(ic), FALSE);
		assertEquals(sa.eq(sc), FALSE);

		// check arithmetic equality among different types
		assertEquals(ia.eq(sa), FALSE);
		assertEquals(sa.eq(ia), FALSE);

		// equals with mismatched types
		LuaValue t = new LuaTable();
		assertEquals(ia.eq(t), FALSE);
		assertEquals(t.eq(ia), FALSE);
		assertEquals(ia.eq(FALSE), FALSE);
		assertEquals(FALSE.eq(ia), FALSE);
		assertEquals(ia.eq(NIL), FALSE);
		assertEquals(NIL.eq(ia), FALSE);
	}

	@Test
	public void testEqDouble() {
		LuaValue da = LuaDouble.valueOf(345.5), db = LuaDouble.valueOf(345.5), dc = LuaDouble.valueOf(-345.5);
		LuaValue sa = LuaString.valueOf("345.5"), sb = LuaString.valueOf("345.5"), sc = LuaString.valueOf("-345.5");

		// check arithmetic equality among same types
		assertEquals(da.eq(db), TRUE);
		assertEquals(sa.eq(sb), TRUE);
		assertEquals(da.eq(dc), FALSE);
		assertEquals(sa.eq(sc), FALSE);

		// check arithmetic equality among different types
		assertEquals(da.eq(sa), FALSE);
		assertEquals(sa.eq(da), FALSE);

		// equals with mismatched types
		LuaValue t = new LuaTable();
		assertEquals(da.eq(t), FALSE);
		assertEquals(t.eq(da), FALSE);
		assertEquals(da.eq(FALSE), FALSE);
		assertEquals(FALSE.eq(da), FALSE);
		assertEquals(da.eq(NIL), FALSE);
		assertEquals(NIL.eq(da), FALSE);
	}

	private static final TwoArgFunction RETURN_NIL = new TwoArgFunction() {
		@Override
		public LuaValue call(LuaValue lhs, LuaValue rhs) {
			return NIL;
		}
	};

	private static final TwoArgFunction RETURN_ONE = new TwoArgFunction() {
		@Override
		public LuaValue call(LuaValue lhs, LuaValue rhs) {
			return ONE;
		}
	};


	@Test
	public void testEqualsMetatag() {
		LuaValue tru = TRUE;
		LuaValue fal = FALSE;
		LuaValue zer = ZERO;
		LuaValue one = ONE;
		LuaValue abc = valueOf("abcdef").substring(0, 3);
		LuaValue def = valueOf("abcdef").substring(3, 6);
		LuaValue pi = valueOf(Math.PI);
		LuaValue ee = valueOf(Math.E);
		LuaValue tbl = new LuaTable();
		LuaValue tbl2 = new LuaTable();
		LuaValue tbl3 = new LuaTable();
		LuaValue uda = new LuaUserdata(new Object());
		LuaValue udb = new LuaUserdata(uda.touserdata());
		LuaValue uda2 = new LuaUserdata(new Object());
		LuaValue uda3 = new LuaUserdata(uda.touserdata());
		LuaValue nilb = valueOf(NIL.toboolean());
		LuaValue oneb = valueOf(ONE.toboolean());
		assertEquals(FALSE, nilb);
		assertEquals(TRUE, oneb);
		LuaValue smt = LuaString.s_metatable;
		try {
			// always return nil0
			LuaBoolean.s_metatable = tableOf(new LuaValue[]{EQ, RETURN_NIL,});
			LuaNumber.s_metatable = tableOf(new LuaValue[]{EQ, RETURN_NIL,});
			LuaString.s_metatable = tableOf(new LuaValue[]{EQ, RETURN_NIL,});
			tbl.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_NIL,}));
			tbl2.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_NIL,}));
			uda.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_NIL,}));
			udb.setMetatable(uda.getMetatable());
			uda2.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_NIL,}));
			// diff metatag function
			tbl3.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_ONE,}));
			uda3.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_ONE,}));

			// primitive types or same valu do not invoke metatag as per C implementation
			assertEquals(tru, tru.eq(tru));
			assertEquals(tru, one.eq(one));
			assertEquals(tru, abc.eq(abc));
			assertEquals(tru, tbl.eq(tbl));
			assertEquals(tru, uda.eq(uda));
			assertEquals(tru, uda.eq(udb));
			assertEquals(fal, tru.eq(fal));
			assertEquals(fal, fal.eq(tru));
			assertEquals(fal, zer.eq(one));
			assertEquals(fal, one.eq(zer));
			assertEquals(fal, pi.eq(ee));
			assertEquals(fal, ee.eq(pi));
			assertEquals(fal, pi.eq(one));
			assertEquals(fal, one.eq(pi));
			assertEquals(fal, abc.eq(def));
			assertEquals(fal, def.eq(abc));
			// different types.  not comparable
			assertEquals(fal, fal.eq(tbl));
			assertEquals(fal, tbl.eq(fal));
			assertEquals(fal, tbl.eq(one));
			assertEquals(fal, one.eq(tbl));
			assertEquals(fal, fal.eq(one));
			assertEquals(fal, one.eq(fal));
			assertEquals(fal, abc.eq(one));
			assertEquals(fal, one.eq(abc));
			assertEquals(fal, tbl.eq(uda));
			assertEquals(fal, uda.eq(tbl));
			// same type, same value, does not invoke metatag op
			assertEquals(tru, tbl.eq(tbl));
			// same type, different value, same metatag op.  comparabile via metatag op
			assertEquals(nilb, tbl.eq(tbl2));
			assertEquals(nilb, tbl2.eq(tbl));
			assertEquals(nilb, uda.eq(uda2));
			assertEquals(nilb, uda2.eq(uda));
			// same type, different metatag ops.  not comparable
			assertEquals(fal, tbl.eq(tbl3));
			assertEquals(fal, tbl3.eq(tbl));
			assertEquals(fal, uda.eq(uda3));
			assertEquals(fal, uda3.eq(uda));

			// always use right argument
			LuaBoolean.s_metatable = tableOf(new LuaValue[]{EQ, RETURN_ONE,});
			LuaNumber.s_metatable = tableOf(new LuaValue[]{EQ, RETURN_ONE,});
			LuaString.s_metatable = tableOf(new LuaValue[]{EQ, RETURN_ONE,});
			tbl.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_ONE,}));
			tbl2.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_ONE,}));
			uda.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_ONE,}));
			udb.setMetatable(uda.getMetatable());
			uda2.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_ONE,}));
			// diff metatag function
			tbl3.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_NIL,}));
			uda3.setMetatable(tableOf(new LuaValue[]{EQ, RETURN_NIL,}));

			// primitive types or same value do not invoke metatag as per C implementation
			assertEquals(tru, tru.eq(tru));
			assertEquals(tru, one.eq(one));
			assertEquals(tru, abc.eq(abc));
			assertEquals(tru, tbl.eq(tbl));
			assertEquals(tru, uda.eq(uda));
			assertEquals(tru, uda.eq(udb));
			assertEquals(fal, tru.eq(fal));
			assertEquals(fal, fal.eq(tru));
			assertEquals(fal, zer.eq(one));
			assertEquals(fal, one.eq(zer));
			assertEquals(fal, pi.eq(ee));
			assertEquals(fal, ee.eq(pi));
			assertEquals(fal, pi.eq(one));
			assertEquals(fal, one.eq(pi));
			assertEquals(fal, abc.eq(def));
			assertEquals(fal, def.eq(abc));
			// different types.  not comparable
			assertEquals(fal, fal.eq(tbl));
			assertEquals(fal, tbl.eq(fal));
			assertEquals(fal, tbl.eq(one));
			assertEquals(fal, one.eq(tbl));
			assertEquals(fal, fal.eq(one));
			assertEquals(fal, one.eq(fal));
			assertEquals(fal, abc.eq(one));
			assertEquals(fal, one.eq(abc));
			assertEquals(fal, tbl.eq(uda));
			assertEquals(fal, uda.eq(tbl));
			// same type, same value, does not invoke metatag op
			assertEquals(tru, tbl.eq(tbl));
			// same type, different value, same metatag op.  comparabile via metatag op
			assertEquals(oneb, tbl.eq(tbl2));
			assertEquals(oneb, tbl2.eq(tbl));
			assertEquals(oneb, uda.eq(uda2));
			assertEquals(oneb, uda2.eq(uda));
			// same type, different metatag ops.  not comparable
			assertEquals(fal, tbl.eq(tbl3));
			assertEquals(fal, tbl3.eq(tbl));
			assertEquals(fal, uda.eq(uda3));
			assertEquals(fal, uda3.eq(uda));

		} finally {
			LuaBoolean.s_metatable = null;
			LuaNumber.s_metatable = null;
			LuaString.s_metatable = smt;
		}
	}

	@Test
	public void testAdd() {
		LuaValue ia = valueOf(111), ib = valueOf(44);
		LuaValue da = valueOf(55.25), db = valueOf(3.5);
		LuaValue sa = valueOf("22.125"), sb = valueOf("7.25");

		// check types
		assertTrue(ia instanceof LuaInteger);
		assertTrue(ib instanceof LuaInteger);
		assertTrue(da instanceof LuaDouble);
		assertTrue(db instanceof LuaDouble);
		assertTrue(sa instanceof LuaString);
		assertTrue(sb instanceof LuaString);

		// like kinds
		assertDoubleEquals(155.0, ia.add(ib).todouble());
		assertDoubleEquals(58.75, da.add(db).todouble());
		assertDoubleEquals(29.375, sa.add(sb).todouble());

		// unlike kinds
		assertDoubleEquals(166.25, ia.add(da).todouble());
		assertDoubleEquals(166.25, da.add(ia).todouble());
		assertDoubleEquals(133.125, ia.add(sa).todouble());
		assertDoubleEquals(133.125, sa.add(ia).todouble());
		assertDoubleEquals(77.375, da.add(sa).todouble());
		assertDoubleEquals(77.375, sa.add(da).todouble());
	}

	@Test
	public void testSub() {
		LuaValue ia = valueOf(111), ib = valueOf(44);
		LuaValue da = valueOf(55.25), db = valueOf(3.5);
		LuaValue sa = valueOf("22.125"), sb = valueOf("7.25");

		// like kinds
		assertDoubleEquals(67.0, ia.sub(ib).todouble());
		assertDoubleEquals(51.75, da.sub(db).todouble());
		assertDoubleEquals(14.875, sa.sub(sb).todouble());

		// unlike kinds
		assertDoubleEquals(55.75, ia.sub(da).todouble());
		assertDoubleEquals(-55.75, da.sub(ia).todouble());
		assertDoubleEquals(88.875, ia.sub(sa).todouble());
		assertDoubleEquals(-88.875, sa.sub(ia).todouble());
		assertDoubleEquals(33.125, da.sub(sa).todouble());
		assertDoubleEquals(-33.125, sa.sub(da).todouble());
	}

	@Test
	public void testMul() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(12.0, ia.mul(ib).todouble());
		assertDoubleEquals(.125, da.mul(db).todouble());
		assertDoubleEquals(3.0, sa.mul(sb).todouble());

		// unlike kinds
		assertDoubleEquals(.75, ia.mul(da).todouble());
		assertDoubleEquals(.75, da.mul(ia).todouble());
		assertDoubleEquals(4.5, ia.mul(sa).todouble());
		assertDoubleEquals(4.5, sa.mul(ia).todouble());
		assertDoubleEquals(.375, da.mul(sa).todouble());
		assertDoubleEquals(.375, sa.mul(da).todouble());
	}

	@Test
	public void testDiv() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(3. / 4., ia.div(ib).todouble());
		assertDoubleEquals(.25 / .5, da.div(db).todouble());
		assertDoubleEquals(1.5 / 2., sa.div(sb).todouble());

		// unlike kinds
		assertDoubleEquals(3. / .25, ia.div(da).todouble());
		assertDoubleEquals(.25 / 3., da.div(ia).todouble());
		assertDoubleEquals(3. / 1.5, ia.div(sa).todouble());
		assertDoubleEquals(1.5 / 3., sa.div(ia).todouble());
		assertDoubleEquals(.25 / 1.5, da.div(sa).todouble());
		assertDoubleEquals(1.5 / .25, sa.div(da).todouble());
	}

	@Test
	public void testPow() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(4.), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(Math.pow(3., 4.), ia.pow(ib).todouble());
		assertDoubleEquals(Math.pow(4., .5), da.pow(db).todouble());
		assertDoubleEquals(Math.pow(1.5, 2.), sa.pow(sb).todouble());

		// unlike kinds
		assertDoubleEquals(Math.pow(3., 4.), ia.pow(da).todouble());
		assertDoubleEquals(Math.pow(4., 3.), da.pow(ia).todouble());
		assertDoubleEquals(Math.pow(3., 1.5), ia.pow(sa).todouble());
		assertDoubleEquals(Math.pow(1.5, 3.), sa.pow(ia).todouble());
		assertDoubleEquals(Math.pow(4., 1.5), da.pow(sa).todouble());
		assertDoubleEquals(Math.pow(1.5, 4.), sa.pow(da).todouble());
	}

	private static double luaMod(double x, double y) {
		return y != 0 ? x - y * Math.floor(x / y) : Double.NaN;
	}

	@Test
	public void testMod() {
		LuaValue ia = valueOf(3), ib = valueOf(-4);
		LuaValue da = valueOf(.25), db = valueOf(-.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("-2.0");

		// like kinds
		assertDoubleEquals(luaMod(3., -4.), ia.mod(ib).todouble());
		assertDoubleEquals(luaMod(.25, -.5), da.mod(db).todouble());
		assertDoubleEquals(luaMod(1.5, -2.), sa.mod(sb).todouble());

		// unlike kinds
		assertDoubleEquals(luaMod(3., .25), ia.mod(da).todouble());
		assertDoubleEquals(luaMod(.25, 3.), da.mod(ia).todouble());
		assertDoubleEquals(luaMod(3., 1.5), ia.mod(sa).todouble());
		assertDoubleEquals(luaMod(1.5, 3.), sa.mod(ia).todouble());
		assertDoubleEquals(luaMod(.25, 1.5), da.mod(sa).todouble());
		assertDoubleEquals(luaMod(1.5, .25), sa.mod(da).todouble());
	}

	@Test
	public void testArithErrors() {
		LuaValue ia = valueOf(111), ib = valueOf(44);
		LuaValue da = valueOf(55.25), db = valueOf(3.5);
		LuaValue sa = valueOf("22.125"), sb = valueOf("7.25");

		String[] ops = {"add", "sub", "mul", "div", "mod", "pow"};
		LuaValue[] vals = {NIL, TRUE, tableOf()};
		LuaValue[] numerics = {valueOf(111), valueOf(55.25), valueOf("22.125")};
		for (String op : ops) {
			for (LuaValue val : vals) {
				for (LuaValue numeric : numerics) {
					checkArithError(val, numeric, op, val.typeName());
					checkArithError(numeric, val, op, val.typeName());
				}
			}
		}
	}

	private void checkArithError(LuaValue a, LuaValue b, String op, String type) {
		try {
			LuaValue.class.getMethod(op, new Class[]{LuaValue.class}).invoke(a, b);
		} catch (InvocationTargetException ite) {
			String actual = ite.getTargetException().getMessage();
			if ((!actual.startsWith("attempt to perform arithmetic")) || !actual.contains(type)) {
				fail("(" + a.typeName() + "," + op + "," + b.typeName() + ") reported '" + actual + "'");
			}
		} catch (Exception e) {
			fail("(" + a.typeName() + "," + op + "," + b.typeName() + ") threw " + e);
		}
	}

	private static final TwoArgFunction RETURN_LHS = new TwoArgFunction() {
		@Override
		public LuaValue call(LuaValue lhs, LuaValue rhs) {
			return lhs;
		}
	};

	private static final TwoArgFunction RETURN_RHS = new TwoArgFunction() {
		@Override
		public LuaValue call(LuaValue lhs, LuaValue rhs) {
			return rhs;
		}
	};

	@Test
	public void testArithMetatag() {
		LuaValue tru = TRUE;
		LuaValue fal = FALSE;
		LuaValue tbl = new LuaTable();
		LuaValue tbl2 = new LuaTable();
		try {
			try {
				tru.add(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.mul(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.div(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.pow(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.mod(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use left argument
			LuaBoolean.s_metatable = tableOf(new LuaValue[]{ADD, RETURN_LHS,});
			assertEquals(tru, tru.add(fal));
			assertEquals(tru, tru.add(tbl));
			assertEquals(tbl, tbl.add(tru));
			try {
				tbl.add(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			LuaBoolean.s_metatable = tableOf(new LuaValue[]{SUB, RETURN_LHS,});
			assertEquals(tru, tru.sub(fal));
			assertEquals(tru, tru.sub(tbl));
			assertEquals(tbl, tbl.sub(tru));
			try {
				tbl.sub(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.add(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			LuaBoolean.s_metatable = tableOf(new LuaValue[]{MUL, RETURN_LHS,});
			assertEquals(tru, tru.mul(fal));
			assertEquals(tru, tru.mul(tbl));
			assertEquals(tbl, tbl.mul(tru));
			try {
				tbl.mul(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			LuaBoolean.s_metatable = tableOf(new LuaValue[]{DIV, RETURN_LHS,});
			assertEquals(tru, tru.div(fal));
			assertEquals(tru, tru.div(tbl));
			assertEquals(tbl, tbl.div(tru));
			try {
				tbl.div(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			LuaBoolean.s_metatable = tableOf(new LuaValue[]{POW, RETURN_LHS,});
			assertEquals(tru, tru.pow(fal));
			assertEquals(tru, tru.pow(tbl));
			assertEquals(tbl, tbl.pow(tru));
			try {
				tbl.pow(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			LuaBoolean.s_metatable = tableOf(new LuaValue[]{MOD, RETURN_LHS,});
			assertEquals(tru, tru.mod(fal));
			assertEquals(tru, tru.mod(tbl));
			assertEquals(tbl, tbl.mod(tru));
			try {
				tbl.mod(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use right argument
			LuaBoolean.s_metatable = tableOf(new LuaValue[]{ADD, RETURN_RHS,});
			assertEquals(fal, tru.add(fal));
			assertEquals(tbl, tru.add(tbl));
			assertEquals(tru, tbl.add(tru));
			try {
				tbl.add(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			LuaBoolean.s_metatable = tableOf(new LuaValue[]{SUB, RETURN_RHS,});
			assertEquals(fal, tru.sub(fal));
			assertEquals(tbl, tru.sub(tbl));
			assertEquals(tru, tbl.sub(tru));
			try {
				tbl.sub(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.add(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			LuaBoolean.s_metatable = tableOf(new LuaValue[]{MUL, RETURN_RHS,});
			assertEquals(fal, tru.mul(fal));
			assertEquals(tbl, tru.mul(tbl));
			assertEquals(tru, tbl.mul(tru));
			try {
				tbl.mul(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			LuaBoolean.s_metatable = tableOf(new LuaValue[]{DIV, RETURN_RHS,});
			assertEquals(fal, tru.div(fal));
			assertEquals(tbl, tru.div(tbl));
			assertEquals(tru, tbl.div(tru));
			try {
				tbl.div(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			LuaBoolean.s_metatable = tableOf(new LuaValue[]{POW, RETURN_RHS,});
			assertEquals(fal, tru.pow(fal));
			assertEquals(tbl, tru.pow(tbl));
			assertEquals(tru, tbl.pow(tru));
			try {
				tbl.pow(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			LuaBoolean.s_metatable = tableOf(new LuaValue[]{MOD, RETURN_RHS,});
			assertEquals(fal, tru.mod(fal));
			assertEquals(tbl, tru.mod(tbl));
			assertEquals(tru, tbl.mod(tru));
			try {
				tbl.mod(tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


		} finally {
			LuaBoolean.s_metatable = null;
		}
	}

	@Test
	public void testArithMetatagNumberTable() {
		LuaValue zero = ZERO;
		LuaValue one = ONE;
		LuaValue tbl = new LuaTable();

		try {
			tbl.add(zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.add(tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(tableOf(new LuaValue[]{ADD, RETURN_ONE,}));
		assertEquals(one, tbl.add(zero));
		assertEquals(one, zero.add(tbl));

		try {
			tbl.sub(zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.sub(tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(tableOf(new LuaValue[]{SUB, RETURN_ONE,}));
		assertEquals(one, tbl.sub(zero));
		assertEquals(one, zero.sub(tbl));

		try {
			tbl.mul(zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.mul(tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(tableOf(new LuaValue[]{MUL, RETURN_ONE,}));
		assertEquals(one, tbl.mul(zero));
		assertEquals(one, zero.mul(tbl));

		try {
			tbl.div(zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.div(tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(tableOf(new LuaValue[]{DIV, RETURN_ONE,}));
		assertEquals(one, tbl.div(zero));
		assertEquals(one, zero.div(tbl));

		try {
			tbl.pow(zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.pow(tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(tableOf(new LuaValue[]{POW, RETURN_ONE,}));
		assertEquals(one, tbl.pow(zero));
		assertEquals(one, zero.pow(tbl));

		try {
			tbl.mod(zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.mod(tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(tableOf(new LuaValue[]{MOD, RETURN_ONE,}));
		assertEquals(one, tbl.mod(zero));
		assertEquals(one, zero.mod(tbl));
	}

	@Test
	public void testCompareStrings() {
		// these are lexical compare!
		LuaValue sa = valueOf("-1.5");
		LuaValue sb = valueOf("-2.0");
		LuaValue sc = valueOf("1.5");
		LuaValue sd = valueOf("2.0");

		assertEquals(FALSE, sa.lt(sa));
		assertEquals(TRUE, sa.lt(sb));
		assertEquals(TRUE, sa.lt(sc));
		assertEquals(TRUE, sa.lt(sd));
		assertEquals(FALSE, sb.lt(sa));
		assertEquals(FALSE, sb.lt(sb));
		assertEquals(TRUE, sb.lt(sc));
		assertEquals(TRUE, sb.lt(sd));
		assertEquals(FALSE, sc.lt(sa));
		assertEquals(FALSE, sc.lt(sb));
		assertEquals(FALSE, sc.lt(sc));
		assertEquals(TRUE, sc.lt(sd));
		assertEquals(FALSE, sd.lt(sa));
		assertEquals(FALSE, sd.lt(sb));
		assertEquals(FALSE, sd.lt(sc));
		assertEquals(FALSE, sd.lt(sd));
	}

	@Test
	public void testLt() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. < 4., ia.lt(ib).toboolean());
		assertEquals(.25 < .5, da.lt(db).toboolean());
		assertEquals(3. < 4., ia.lt_b(ib));
		assertEquals(.25 < .5, da.lt_b(db));

		// unlike kinds
		assertEquals(3. < .25, ia.lt(da).toboolean());
		assertEquals(.25 < 3., da.lt(ia).toboolean());
		assertEquals(3. < .25, ia.lt_b(da));
		assertEquals(.25 < 3., da.lt_b(ia));
	}

	@Test
	public void testLtEq() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. <= 4., ia.lteq(ib).toboolean());
		assertEquals(.25 <= .5, da.lteq(db).toboolean());
		assertEquals(3. <= 4., ia.lteq_b(ib));
		assertEquals(.25 <= .5, da.lteq_b(db));

		// unlike kinds
		assertEquals(3. <= .25, ia.lteq(da).toboolean());
		assertEquals(.25 <= 3., da.lteq(ia).toboolean());
		assertEquals(3. <= .25, ia.lteq_b(da));
		assertEquals(.25 <= 3., da.lteq_b(ia));
	}

	@Test
	public void testGt() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. > 4., ia.gt(ib).toboolean());
		assertEquals(.25 > .5, da.gt(db).toboolean());
		assertEquals(3. > 4., ia.gt_b(ib));
		assertEquals(.25 > .5, da.gt_b(db));

		// unlike kinds
		assertEquals(3. > .25, ia.gt(da).toboolean());
		assertEquals(.25 > 3., da.gt(ia).toboolean());
		assertEquals(3. > .25, ia.gt_b(da));
		assertEquals(.25 > 3., da.gt_b(ia));
	}

	@Test
	public void testGtEq() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. >= 4., ia.gteq(ib).toboolean());
		assertEquals(.25 >= .5, da.gteq(db).toboolean());
		assertEquals(3. >= 4., ia.gteq_b(ib));
		assertEquals(.25 >= .5, da.gteq_b(db));

		// unlike kinds
		assertEquals(3. >= .25, ia.gteq(da).toboolean());
		assertEquals(.25 >= 3., da.gteq(ia).toboolean());
		assertEquals(3. >= .25, ia.gteq_b(da));
		assertEquals(.25 >= 3., da.gteq_b(ia));
	}

	@Test
	public void testNotEq() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertEquals(3. != 4., ia.neq(ib).toboolean());
		assertEquals(.25 != .5, da.neq(db).toboolean());
		assertEquals(1.5 != 2., sa.neq(sb).toboolean());
		assertEquals(3. != 4., ia.neq_b(ib));
		assertEquals(.25 != .5, da.neq_b(db));
		assertEquals(1.5 != 2., sa.neq_b(sb));

		// unlike kinds
		assertEquals(3. != .25, ia.neq(da).toboolean());
		assertEquals(.25 != 3., da.neq(ia).toboolean());
		assertEquals(3. != 1.5, ia.neq(sa).toboolean());
		assertEquals(1.5 != 3., sa.neq(ia).toboolean());
		assertEquals(.25 != 1.5, da.neq(sa).toboolean());
		assertEquals(1.5 != .25, sa.neq(da).toboolean());
		assertEquals(3. != .25, ia.neq_b(da));
		assertEquals(.25 != 3., da.neq_b(ia));
		assertEquals(3. != 1.5, ia.neq_b(sa));
		assertEquals(1.5 != 3., sa.neq_b(ia));
		assertEquals(.25 != 1.5, da.neq_b(sa));
		assertEquals(1.5 != .25, sa.neq_b(da));
	}

	@Test
	public void testCompareErrors() {
		LuaValue ia = valueOf(111), ib = valueOf(44);
		LuaValue da = valueOf(55.25), db = valueOf(3.5);
		LuaValue sa = valueOf("22.125"), sb = valueOf("7.25");

		String[] ops = {"lt", "lteq",};
		LuaValue[] vals = {NIL, TRUE, tableOf()};
		LuaValue[] numerics = {valueOf(111), valueOf(55.25), valueOf("22.125")};
		for (String op : ops) {
			for (LuaValue val : vals) {
				for (LuaValue numeric : numerics) {
					checkCompareError(val, numeric, op, val.typeName());
					checkCompareError(numeric, val, op, val.typeName());
				}
			}
		}
	}

	private void checkCompareError(LuaValue a, LuaValue b, String op, String type) {
		try {
			LuaValue.class.getMethod(op, new Class[]{LuaValue.class}).invoke(a, b);
		} catch (InvocationTargetException ite) {
			String actual = ite.getTargetException().getMessage();
			if ((!actual.startsWith("attempt to compare")) || !actual.contains(type)) {
				fail("(" + a.typeName() + "," + op + "," + b.typeName() + ") reported '" + actual + "'");
			}
		} catch (Exception e) {
			fail("(" + a.typeName() + "," + op + "," + b.typeName() + ") threw " + e);
		}
	}

	@Test
	public void testCompareMetatag() {
		LuaValue tru = TRUE;
		LuaValue fal = FALSE;
		LuaValue tbl = new LuaTable();
		LuaValue tbl2 = new LuaTable();
		LuaValue tbl3 = new LuaTable();
		try {
			// always use left argument
			LuaValue mt = tableOf(new LuaValue[]{
				LT, RETURN_LHS,
				LE, RETURN_RHS,
			});
			LuaBoolean.s_metatable = mt;
			tbl.setMetatable(mt);
			tbl2.setMetatable(mt);
			assertEquals(tru, tru.lt(fal));
			assertEquals(fal, fal.lt(tru));
			assertEquals(tbl, tbl.lt(tbl2));
			assertEquals(tbl2, tbl2.lt(tbl));
			try {
				tbl.lt(tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl3.lt(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			assertEquals(fal, tru.lteq(fal));
			assertEquals(tru, fal.lteq(tru));
			assertEquals(tbl2, tbl.lteq(tbl2));
			assertEquals(tbl, tbl2.lteq(tbl));
			try {
				tbl.lteq(tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl3.lteq(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use right argument
			mt = tableOf(new LuaValue[]{
				LT, RETURN_RHS,
				LE, RETURN_LHS});
			LuaBoolean.s_metatable = mt;
			tbl.setMetatable(mt);
			tbl2.setMetatable(mt);
			assertEquals(fal, tru.lt(fal));
			assertEquals(tru, fal.lt(tru));
			assertEquals(tbl2, tbl.lt(tbl2));
			assertEquals(tbl, tbl2.lt(tbl));
			try {
				tbl.lt(tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl3.lt(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			assertEquals(tru, tru.lteq(fal));
			assertEquals(fal, fal.lteq(tru));
			assertEquals(tbl, tbl.lteq(tbl2));
			assertEquals(tbl2, tbl2.lteq(tbl));
			try {
				tbl.lteq(tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl3.lteq(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


		} finally {
			LuaBoolean.s_metatable = null;
		}
	}

	@Test
	public void testAnd() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");
		LuaValue ba = TRUE, bb = FALSE;

		// like kinds
		assertSame(ib, ia.and(ib));
		assertSame(db, da.and(db));
		assertSame(sb, sa.and(sb));

		// unlike kinds
		assertSame(da, ia.and(da));
		assertSame(ia, da.and(ia));
		assertSame(sa, ia.and(sa));
		assertSame(ia, sa.and(ia));
		assertSame(sa, da.and(sa));
		assertSame(da, sa.and(da));

		// boolean values
		assertSame(bb, ba.and(bb));
		assertSame(bb, bb.and(ba));
		assertSame(ia, ba.and(ia));
		assertSame(bb, bb.and(ia));
	}

	@Test
	public void testOr() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");
		LuaValue ba = TRUE, bb = FALSE;

		// like kinds
		assertSame(ia, ia.or(ib));
		assertSame(da, da.or(db));
		assertSame(sa, sa.or(sb));

		// unlike kinds
		assertSame(ia, ia.or(da));
		assertSame(da, da.or(ia));
		assertSame(ia, ia.or(sa));
		assertSame(sa, sa.or(ia));
		assertSame(da, da.or(sa));
		assertSame(sa, sa.or(da));

		// boolean values
		assertSame(ba, ba.or(bb));
		assertSame(ba, bb.or(ba));
		assertSame(ba, ba.or(ia));
		assertSame(ia, bb.or(ia));
	}

	@Test
	public void testLexicalComparison() {
		LuaValue aaa = valueOf("aaa");
		LuaValue baa = valueOf("baa");
		LuaValue Aaa = valueOf("Aaa");
		LuaValue aba = valueOf("aba");
		LuaValue aaaa = valueOf("aaaa");
		LuaValue t = TRUE;
		LuaValue f = FALSE;

		// basics
		assertEquals(t, aaa.eq(aaa));
		assertEquals(t, aaa.lt(baa));
		assertEquals(t, aaa.lteq(baa));
		assertEquals(f, aaa.gt(baa));
		assertEquals(f, aaa.gteq(baa));
		assertEquals(f, baa.lt(aaa));
		assertEquals(f, baa.lteq(aaa));
		assertEquals(t, baa.gt(aaa));
		assertEquals(t, baa.gteq(aaa));
		assertEquals(t, aaa.lteq(aaa));
		assertEquals(t, aaa.gteq(aaa));

		// different case
		assertEquals(t, Aaa.eq(Aaa));
		assertEquals(t, Aaa.lt(aaa));
		assertEquals(t, Aaa.lteq(aaa));
		assertEquals(f, Aaa.gt(aaa));
		assertEquals(f, Aaa.gteq(aaa));
		assertEquals(f, aaa.lt(Aaa));
		assertEquals(f, aaa.lteq(Aaa));
		assertEquals(t, aaa.gt(Aaa));
		assertEquals(t, aaa.gteq(Aaa));
		assertEquals(t, Aaa.lteq(Aaa));
		assertEquals(t, Aaa.gteq(Aaa));

		// second letter differs
		assertEquals(t, aaa.eq(aaa));
		assertEquals(t, aaa.lt(aba));
		assertEquals(t, aaa.lteq(aba));
		assertEquals(f, aaa.gt(aba));
		assertEquals(f, aaa.gteq(aba));
		assertEquals(f, aba.lt(aaa));
		assertEquals(f, aba.lteq(aaa));
		assertEquals(t, aba.gt(aaa));
		assertEquals(t, aba.gteq(aaa));
		assertEquals(t, aaa.lteq(aaa));
		assertEquals(t, aaa.gteq(aaa));

		// longer
		assertEquals(t, aaa.eq(aaa));
		assertEquals(t, aaa.lt(aaaa));
		assertEquals(t, aaa.lteq(aaaa));
		assertEquals(f, aaa.gt(aaaa));
		assertEquals(f, aaa.gteq(aaaa));
		assertEquals(f, aaaa.lt(aaa));
		assertEquals(f, aaaa.lteq(aaa));
		assertEquals(t, aaaa.gt(aaa));
		assertEquals(t, aaaa.gteq(aaa));
		assertEquals(t, aaa.lteq(aaa));
		assertEquals(t, aaa.gteq(aaa));
	}

	@Test
	public void testBuffer() {
		LuaValue abc = valueOf("abcdefghi").substring(0, 3);
		LuaValue def = valueOf("abcdefghi").substring(3, 6);
		LuaValue ghi = valueOf("abcdefghi").substring(6, 9);
		LuaValue n123 = valueOf(123);

		// basic append
		Buffer b = new Buffer();
		assertEquals("", b.value().tojstring());
		b.append(def);
		assertEquals("def", b.value().tojstring());
		b.append(abc);
		assertEquals("defabc", b.value().tojstring());
		b.append(ghi);
		assertEquals("defabcghi", b.value().tojstring());
		b.append(n123);
		assertEquals("defabcghi123", b.value().tojstring());

		// basic prepend
		b = new Buffer();
		assertEquals("", b.value().tojstring());
		b.prepend(def.strvalue());
		assertEquals("def", b.value().tojstring());
		b.prepend(ghi.strvalue());
		assertEquals("ghidef", b.value().tojstring());
		b.prepend(abc.strvalue());
		assertEquals("abcghidef", b.value().tojstring());
		b.prepend(n123.strvalue());
		assertEquals("123abcghidef", b.value().tojstring());

		// mixed append, prepend
		b = new Buffer();
		assertEquals("", b.value().tojstring());
		b.append(def);
		assertEquals("def", b.value().tojstring());
		b.append(abc);
		assertEquals("defabc", b.value().tojstring());
		b.prepend(ghi.strvalue());
		assertEquals("ghidefabc", b.value().tojstring());
		b.prepend(n123.strvalue());
		assertEquals("123ghidefabc", b.value().tojstring());
		b.append(def);
		assertEquals("123ghidefabcdef", b.value().tojstring());
		b.append(abc);
		assertEquals("123ghidefabcdefabc", b.value().tojstring());
		b.prepend(ghi.strvalue());
		assertEquals("ghi123ghidefabcdefabc", b.value().tojstring());
		b.prepend(n123.strvalue());
		assertEquals("123ghi123ghidefabcdefabc", b.value().tojstring());

		// value
		b = new Buffer(def);
		assertEquals("def", b.value().tojstring());
		b.append(abc);
		assertEquals("defabc", b.value().tojstring());
		b.prepend(ghi.strvalue());
		assertEquals("ghidefabc", b.value().tojstring());
		b.setvalue(def);
		assertEquals("def", b.value().tojstring());
		b.prepend(ghi.strvalue());
		assertEquals("ghidef", b.value().tojstring());
		b.append(abc);
		assertEquals("ghidefabc", b.value().tojstring());
	}

	@Test
	public void testConcat() {
		LuaValue abc = valueOf("abcdefghi").substring(0, 3);
		LuaValue def = valueOf("abcdefghi").substring(3, 6);
		LuaValue ghi = valueOf("abcdefghi").substring(6, 9);
		LuaValue n123 = valueOf(123);

		assertEquals("abc", abc.tojstring());
		assertEquals("def", def.tojstring());
		assertEquals("ghi", ghi.tojstring());
		assertEquals("123", n123.tojstring());
		assertEquals("abcabc", abc.concat(abc).tojstring());
		assertEquals("defghi", def.concat(ghi).tojstring());
		assertEquals("ghidef", ghi.concat(def).tojstring());
		assertEquals("ghidefabcghi", ghi.concat(def).concat(abc).concat(ghi).tojstring());
		assertEquals("123def", n123.concat(def).tojstring());
		assertEquals("def123", def.concat(n123).tojstring());
	}

	@Test
	public void testConcatBuffer() {
		LuaValue abc = valueOf("abcdefghi").substring(0, 3);
		LuaValue def = valueOf("abcdefghi").substring(3, 6);
		LuaValue ghi = valueOf("abcdefghi").substring(6, 9);
		LuaValue n123 = valueOf(123);
		Buffer b;

		b = new Buffer(def);
		assertEquals("def", b.value().tojstring());
		b = ghi.concat(b);
		assertEquals("ghidef", b.value().tojstring());
		b = abc.concat(b);
		assertEquals("abcghidef", b.value().tojstring());
		b = n123.concat(b);
		assertEquals("123abcghidef", b.value().tojstring());
		b.setvalue(n123);
		b = def.concat(b);
		assertEquals("def123", b.value().tojstring());
		b = abc.concat(b);
		assertEquals("abcdef123", b.value().tojstring());
	}

	@Test
	public void testConcatMetatag() {
		LuaValue def = valueOf("abcdefghi").substring(3, 6);
		LuaValue ghi = valueOf("abcdefghi").substring(6, 9);
		LuaValue tru = TRUE;
		LuaValue fal = FALSE;
		LuaValue tbl = new LuaTable();
		LuaValue uda = new LuaUserdata(new Object());
		try {
			// always use left argument
			LuaBoolean.s_metatable = tableOf(new LuaValue[]{CONCAT, RETURN_LHS});
			assertEquals(tru, tru.concat(tbl));
			assertEquals(tbl, tbl.concat(tru));
			assertEquals(tru, tru.concat(tbl));
			assertEquals(tbl, tbl.concat(tru));
			assertEquals(tru, tru.concat(tbl.buffer()).value());
			assertEquals(tbl, tbl.concat(tru.buffer()).value());
			assertEquals(fal, fal.concat(tbl.concat(tru.buffer())).value());
			assertEquals(uda, uda.concat(tru.concat(tbl.buffer())).value());
			try {
				tbl.concat(def);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				def.concat(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl.concat(def.buffer()).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				def.concat(tbl.buffer()).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				uda.concat(def.concat(tbl.buffer())).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				ghi.concat(tbl.concat(def.buffer())).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use right argument
			LuaBoolean.s_metatable = tableOf(new LuaValue[]{CONCAT, RETURN_RHS});
			assertEquals(tbl, tru.concat(tbl));
			assertEquals(tru, tbl.concat(tru));
			assertEquals(tbl, tru.concat(tbl.buffer()).value());
			assertEquals(tru, tbl.concat(tru.buffer()).value());
			assertEquals(tru, uda.concat(tbl.concat(tru.buffer())).value());
			assertEquals(tbl, fal.concat(tru.concat(tbl.buffer())).value());
			try {
				tbl.concat(def);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				def.concat(tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl.concat(def.buffer()).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				def.concat(tbl.buffer()).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				uda.concat(def.concat(tbl.buffer())).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				uda.concat(tbl.concat(def.buffer())).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


		} finally {
			LuaBoolean.s_metatable = null;
		}
	}

	@Test
	public void testConcatErrors() {
		LuaValue ia = valueOf(111), ib = valueOf(44);
		LuaValue da = valueOf(55.25), db = valueOf(3.5);
		LuaValue sa = valueOf("22.125"), sb = valueOf("7.25");

		String[] ops = {"concat"};
		LuaValue[] vals = {NIL, TRUE, tableOf()};
		LuaValue[] numerics = {valueOf(111), valueOf(55.25), valueOf("22.125")};
		for (String op : ops) {
			for (LuaValue val : vals) {
				for (LuaValue numeric : numerics) {
					checkConcatError(val, numeric, op, val.typeName());
					checkConcatError(numeric, val, op, val.typeName());
				}
			}
		}
	}

	private void checkConcatError(LuaValue a, LuaValue b, String op, String type) {
		try {
			LuaValue.class.getMethod(op, new Class[]{LuaValue.class}).invoke(a, b);
		} catch (InvocationTargetException ite) {
			String actual = ite.getTargetException().getMessage();
			if ((!actual.startsWith("attempt to concatenate")) || !actual.contains(type)) {
				fail("(" + a.typeName() + "," + op + "," + b.typeName() + ") reported '" + actual + "'");
			}
		} catch (Exception e) {
			fail("(" + a.typeName() + "," + op + "," + b.typeName() + ") threw " + e);
		}
	}


	public static void assertDoubleEquals(double a, double b) {
		assertEquals(a, b, 1e-10);
	}
}
