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
import org.luaj.vm2.lib.platform.FileResourceManipulator;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;
import static org.luaj.vm2.Constants.*;
import static org.luaj.vm2.Factory.tableOf;
import static org.luaj.vm2.Factory.valueOf;

/**
 * Tests of basic unary and binary operators on main value types.
 */
public class UnaryBinaryOperatorsTest {
	private LuaState state = new LuaState(new FileResourceManipulator());

	@Test
	public void testEqualsBool() {
		Assert.assertEquals(FALSE, FALSE);
		assertEquals(TRUE, TRUE);
		assertTrue(FALSE.equals(FALSE));
		assertTrue(TRUE.equals(TRUE));
		assertTrue(!FALSE.equals(TRUE));
		assertTrue(!TRUE.equals(FALSE));
		assertTrue(FALSE.eq_b(state, FALSE));
		assertTrue(TRUE.eq_b(state, TRUE));
		assertFalse(FALSE.eq_b(state, TRUE));
		assertFalse(TRUE.eq_b(state, FALSE));
		assertEquals(TRUE, FALSE.eq(state, FALSE));
		assertEquals(TRUE, TRUE.eq(state, TRUE));
		assertEquals(FALSE, FALSE.eq(state, TRUE));
		assertEquals(FALSE, TRUE.eq(state, FALSE));
		assertFalse(FALSE.neq_b(state, FALSE));
		assertFalse(TRUE.neq_b(state, TRUE));
		assertTrue(FALSE.neq_b(state, TRUE));
		assertTrue(TRUE.neq_b(state, FALSE));
		assertEquals(FALSE, FALSE.neq(state, FALSE));
		assertEquals(FALSE, TRUE.neq(state, TRUE));
		assertEquals(TRUE, FALSE.neq(state, TRUE));
		assertEquals(TRUE, TRUE.neq(state, FALSE));
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
		assertDoubleEquals(-3., ia.neg(state).todouble());
		assertDoubleEquals(-.25, da.neg(state).todouble());
		assertDoubleEquals(-1.5, sa.neg(state).todouble());
		assertDoubleEquals(4., ib.neg(state).todouble());
		assertDoubleEquals(.5, db.neg(state).todouble());
		assertDoubleEquals(2.0, sb.neg(state).todouble());
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
		assertEquals(ia.eq(state, ib), TRUE);
		assertEquals(sa.eq(state, sb), TRUE);
		assertEquals(ia.eq(state, ic), FALSE);
		assertEquals(sa.eq(state, sc), FALSE);

		// check arithmetic equality among different types
		assertEquals(ia.eq(state, sa), FALSE);
		assertEquals(sa.eq(state, ia), FALSE);

		// equals with mismatched types
		LuaValue t = new LuaTable();
		assertEquals(ia.eq(state, t), FALSE);
		assertEquals(t.eq(state, ia), FALSE);
		assertEquals(ia.eq(state, FALSE), FALSE);
		assertEquals(FALSE.eq(state, ia), FALSE);
		assertEquals(ia.eq(state, NIL), FALSE);
		assertEquals(NIL.eq(state, ia), FALSE);
	}

	@Test
	public void testEqDouble() {
		LuaValue da = LuaDouble.valueOf(345.5), db = LuaDouble.valueOf(345.5), dc = LuaDouble.valueOf(-345.5);
		LuaValue sa = LuaString.valueOf("345.5"), sb = LuaString.valueOf("345.5"), sc = LuaString.valueOf("-345.5");

		// check arithmetic equality among same types
		assertEquals(da.eq(state, db), TRUE);
		assertEquals(sa.eq(state, sb), TRUE);
		assertEquals(da.eq(state, dc), FALSE);
		assertEquals(sa.eq(state, sc), FALSE);

		// check arithmetic equality among different types
		assertEquals(da.eq(state, sa), FALSE);
		assertEquals(sa.eq(state, da), FALSE);

		// equals with mismatched types
		LuaValue t = new LuaTable();
		assertEquals(da.eq(state, t), FALSE);
		assertEquals(t.eq(state, da), FALSE);
		assertEquals(da.eq(state, FALSE), FALSE);
		assertEquals(FALSE.eq(state, da), FALSE);
		assertEquals(da.eq(state, NIL), FALSE);
		assertEquals(NIL.eq(state, da), FALSE);
	}

	private static final TwoArgFunction RETURN_NIL = new TwoArgFunction() {
		@Override
		public LuaValue call(LuaState state, LuaValue lhs, LuaValue rhs) {
			return NIL;
		}
	};

	private static final TwoArgFunction RETURN_ONE = new TwoArgFunction() {
		@Override
		public LuaValue call(LuaState state, LuaValue lhs, LuaValue rhs) {
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
		LuaValue smt = state.stringMetatable;
		try {
			// always return nil0
			state.booleanMetatable = tableOf(new LuaValue[]{EQ, RETURN_NIL,});
			state.numberMetatable = tableOf(new LuaValue[]{EQ, RETURN_NIL,});
			state.stringMetatable = tableOf(new LuaValue[]{EQ, RETURN_NIL,});
			tbl.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_NIL,}));
			tbl2.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_NIL,}));
			uda.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_NIL,}));
			udb.setMetatable(state, uda.getMetatable(state));
			uda2.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_NIL,}));
			// diff metatag function
			tbl3.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_ONE,}));
			uda3.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_ONE,}));

			// primitive types or same valu do not invoke metatag as per C implementation
			assertEquals(tru, tru.eq(state, tru));
			assertEquals(tru, one.eq(state, one));
			assertEquals(tru, abc.eq(state, abc));
			assertEquals(tru, tbl.eq(state, tbl));
			assertEquals(tru, uda.eq(state, uda));
			assertEquals(tru, uda.eq(state, udb));
			assertEquals(fal, tru.eq(state, fal));
			assertEquals(fal, fal.eq(state, tru));
			assertEquals(fal, zer.eq(state, one));
			assertEquals(fal, one.eq(state, zer));
			assertEquals(fal, pi.eq(state, ee));
			assertEquals(fal, ee.eq(state, pi));
			assertEquals(fal, pi.eq(state, one));
			assertEquals(fal, one.eq(state, pi));
			assertEquals(fal, abc.eq(state, def));
			assertEquals(fal, def.eq(state, abc));
			// different types.  not comparable
			assertEquals(fal, fal.eq(state, tbl));
			assertEquals(fal, tbl.eq(state, fal));
			assertEquals(fal, tbl.eq(state, one));
			assertEquals(fal, one.eq(state, tbl));
			assertEquals(fal, fal.eq(state, one));
			assertEquals(fal, one.eq(state, fal));
			assertEquals(fal, abc.eq(state, one));
			assertEquals(fal, one.eq(state, abc));
			assertEquals(fal, tbl.eq(state, uda));
			assertEquals(fal, uda.eq(state, tbl));
			// same type, same value, does not invoke metatag op
			assertEquals(tru, tbl.eq(state, tbl));
			// same type, different value, same metatag op.  comparabile via metatag op
			assertEquals(nilb, tbl.eq(state, tbl2));
			assertEquals(nilb, tbl2.eq(state, tbl));
			assertEquals(nilb, uda.eq(state, uda2));
			assertEquals(nilb, uda2.eq(state, uda));
			// same type, different metatag ops.  not comparable
			assertEquals(fal, tbl.eq(state, tbl3));
			assertEquals(fal, tbl3.eq(state, tbl));
			assertEquals(fal, uda.eq(state, uda3));
			assertEquals(fal, uda3.eq(state, uda));

			// always use right argument
			state.booleanMetatable = tableOf(new LuaValue[]{EQ, RETURN_ONE,});
			state.numberMetatable = tableOf(new LuaValue[]{EQ, RETURN_ONE,});
			state.stringMetatable = tableOf(new LuaValue[]{EQ, RETURN_ONE,});
			tbl.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_ONE,}));
			tbl2.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_ONE,}));
			uda.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_ONE,}));
			udb.setMetatable(state, uda.getMetatable(state));
			uda2.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_ONE,}));
			// diff metatag function
			tbl3.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_NIL,}));
			uda3.setMetatable(state, tableOf(new LuaValue[]{EQ, RETURN_NIL,}));

			// primitive types or same value do not invoke metatag as per C implementation
			assertEquals(tru, tru.eq(state, tru));
			assertEquals(tru, one.eq(state, one));
			assertEquals(tru, abc.eq(state, abc));
			assertEquals(tru, tbl.eq(state, tbl));
			assertEquals(tru, uda.eq(state, uda));
			assertEquals(tru, uda.eq(state, udb));
			assertEquals(fal, tru.eq(state, fal));
			assertEquals(fal, fal.eq(state, tru));
			assertEquals(fal, zer.eq(state, one));
			assertEquals(fal, one.eq(state, zer));
			assertEquals(fal, pi.eq(state, ee));
			assertEquals(fal, ee.eq(state, pi));
			assertEquals(fal, pi.eq(state, one));
			assertEquals(fal, one.eq(state, pi));
			assertEquals(fal, abc.eq(state, def));
			assertEquals(fal, def.eq(state, abc));
			// different types.  not comparable
			assertEquals(fal, fal.eq(state, tbl));
			assertEquals(fal, tbl.eq(state, fal));
			assertEquals(fal, tbl.eq(state, one));
			assertEquals(fal, one.eq(state, tbl));
			assertEquals(fal, fal.eq(state, one));
			assertEquals(fal, one.eq(state, fal));
			assertEquals(fal, abc.eq(state, one));
			assertEquals(fal, one.eq(state, abc));
			assertEquals(fal, tbl.eq(state, uda));
			assertEquals(fal, uda.eq(state, tbl));
			// same type, same value, does not invoke metatag op
			assertEquals(tru, tbl.eq(state, tbl));
			// same type, different value, same metatag op.  comparabile via metatag op
			assertEquals(oneb, tbl.eq(state, tbl2));
			assertEquals(oneb, tbl2.eq(state, tbl));
			assertEquals(oneb, uda.eq(state, uda2));
			assertEquals(oneb, uda2.eq(state, uda));
			// same type, different metatag ops.  not comparable
			assertEquals(fal, tbl.eq(state, tbl3));
			assertEquals(fal, tbl3.eq(state, tbl));
			assertEquals(fal, uda.eq(state, uda3));
			assertEquals(fal, uda3.eq(state, uda));

		} finally {
			state.booleanMetatable = null;
			state.numberMetatable = null;
			state.stringMetatable = smt;
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
		assertDoubleEquals(155.0, ia.add(state, ib).todouble());
		assertDoubleEquals(58.75, da.add(state, db).todouble());
		assertDoubleEquals(29.375, sa.add(state, sb).todouble());

		// unlike kinds
		assertDoubleEquals(166.25, ia.add(state, da).todouble());
		assertDoubleEquals(166.25, da.add(state, ia).todouble());
		assertDoubleEquals(133.125, ia.add(state, sa).todouble());
		assertDoubleEquals(133.125, sa.add(state, ia).todouble());
		assertDoubleEquals(77.375, da.add(state, sa).todouble());
		assertDoubleEquals(77.375, sa.add(state, da).todouble());
	}

	@Test
	public void testSub() {
		LuaValue ia = valueOf(111), ib = valueOf(44);
		LuaValue da = valueOf(55.25), db = valueOf(3.5);
		LuaValue sa = valueOf("22.125"), sb = valueOf("7.25");

		// like kinds
		assertDoubleEquals(67.0, ia.sub(state, ib).todouble());
		assertDoubleEquals(51.75, da.sub(state, db).todouble());
		assertDoubleEquals(14.875, sa.sub(state, sb).todouble());

		// unlike kinds
		assertDoubleEquals(55.75, ia.sub(state, da).todouble());
		assertDoubleEquals(-55.75, da.sub(state, ia).todouble());
		assertDoubleEquals(88.875, ia.sub(state, sa).todouble());
		assertDoubleEquals(-88.875, sa.sub(state, ia).todouble());
		assertDoubleEquals(33.125, da.sub(state, sa).todouble());
		assertDoubleEquals(-33.125, sa.sub(state, da).todouble());
	}

	@Test
	public void testMul() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(12.0, ia.mul(state, ib).todouble());
		assertDoubleEquals(.125, da.mul(state, db).todouble());
		assertDoubleEquals(3.0, sa.mul(state, sb).todouble());

		// unlike kinds
		assertDoubleEquals(.75, ia.mul(state, da).todouble());
		assertDoubleEquals(.75, da.mul(state, ia).todouble());
		assertDoubleEquals(4.5, ia.mul(state, sa).todouble());
		assertDoubleEquals(4.5, sa.mul(state, ia).todouble());
		assertDoubleEquals(.375, da.mul(state, sa).todouble());
		assertDoubleEquals(.375, sa.mul(state, da).todouble());
	}

	@Test
	public void testDiv() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(3. / 4., ia.div(state, ib).todouble());
		assertDoubleEquals(.25 / .5, da.div(state, db).todouble());
		assertDoubleEquals(1.5 / 2., sa.div(state, sb).todouble());

		// unlike kinds
		assertDoubleEquals(3. / .25, ia.div(state, da).todouble());
		assertDoubleEquals(.25 / 3., da.div(state, ia).todouble());
		assertDoubleEquals(3. / 1.5, ia.div(state, sa).todouble());
		assertDoubleEquals(1.5 / 3., sa.div(state, ia).todouble());
		assertDoubleEquals(.25 / 1.5, da.div(state, sa).todouble());
		assertDoubleEquals(1.5 / .25, sa.div(state, da).todouble());
	}

	@Test
	public void testPow() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(4.), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(Math.pow(3., 4.), ia.pow(state, ib).todouble());
		assertDoubleEquals(Math.pow(4., .5), da.pow(state, db).todouble());
		assertDoubleEquals(Math.pow(1.5, 2.), sa.pow(state, sb).todouble());

		// unlike kinds
		assertDoubleEquals(Math.pow(3., 4.), ia.pow(state, da).todouble());
		assertDoubleEquals(Math.pow(4., 3.), da.pow(state, ia).todouble());
		assertDoubleEquals(Math.pow(3., 1.5), ia.pow(state, sa).todouble());
		assertDoubleEquals(Math.pow(1.5, 3.), sa.pow(state, ia).todouble());
		assertDoubleEquals(Math.pow(4., 1.5), da.pow(state, sa).todouble());
		assertDoubleEquals(Math.pow(1.5, 4.), sa.pow(state, da).todouble());
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
		assertDoubleEquals(luaMod(3., -4.), ia.mod(state, ib).todouble());
		assertDoubleEquals(luaMod(.25, -.5), da.mod(state, db).todouble());
		assertDoubleEquals(luaMod(1.5, -2.), sa.mod(state, sb).todouble());

		// unlike kinds
		assertDoubleEquals(luaMod(3., .25), ia.mod(state, da).todouble());
		assertDoubleEquals(luaMod(.25, 3.), da.mod(state, ia).todouble());
		assertDoubleEquals(luaMod(3., 1.5), ia.mod(state, sa).todouble());
		assertDoubleEquals(luaMod(1.5, 3.), sa.mod(state, ia).todouble());
		assertDoubleEquals(luaMod(.25, 1.5), da.mod(state, sa).todouble());
		assertDoubleEquals(luaMod(1.5, .25), sa.mod(state, da).todouble());
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
			LuaValue.class.getMethod(op, new Class[]{LuaState.class, LuaValue.class}).invoke(a, state, b);
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
		public LuaValue call(LuaState state, LuaValue lhs, LuaValue rhs) {
			return lhs;
		}
	};

	private static final TwoArgFunction RETURN_RHS = new TwoArgFunction() {
		@Override
		public LuaValue call(LuaState state, LuaValue lhs, LuaValue rhs) {
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
				tru.add(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.mul(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.div(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.pow(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.mod(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use left argument
			state.booleanMetatable = tableOf(new LuaValue[]{ADD, RETURN_LHS,});
			assertEquals(tru, tru.add(state, fal));
			assertEquals(tru, tru.add(state, tbl));
			assertEquals(tbl, tbl.add(state, tru));
			try {
				tbl.add(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{SUB, RETURN_LHS,});
			assertEquals(tru, tru.sub(state, fal));
			assertEquals(tru, tru.sub(state, tbl));
			assertEquals(tbl, tbl.sub(state, tru));
			try {
				tbl.sub(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.add(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{MUL, RETURN_LHS,});
			assertEquals(tru, tru.mul(state, fal));
			assertEquals(tru, tru.mul(state, tbl));
			assertEquals(tbl, tbl.mul(state, tru));
			try {
				tbl.mul(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{DIV, RETURN_LHS,});
			assertEquals(tru, tru.div(state, fal));
			assertEquals(tru, tru.div(state, tbl));
			assertEquals(tbl, tbl.div(state, tru));
			try {
				tbl.div(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{POW, RETURN_LHS,});
			assertEquals(tru, tru.pow(state, fal));
			assertEquals(tru, tru.pow(state, tbl));
			assertEquals(tbl, tbl.pow(state, tru));
			try {
				tbl.pow(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{MOD, RETURN_LHS,});
			assertEquals(tru, tru.mod(state, fal));
			assertEquals(tru, tru.mod(state, tbl));
			assertEquals(tbl, tbl.mod(state, tru));
			try {
				tbl.mod(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use right argument
			state.booleanMetatable = tableOf(new LuaValue[]{ADD, RETURN_RHS,});
			assertEquals(fal, tru.add(state, fal));
			assertEquals(tbl, tru.add(state, tbl));
			assertEquals(tru, tbl.add(state, tru));
			try {
				tbl.add(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{SUB, RETURN_RHS,});
			assertEquals(fal, tru.sub(state, fal));
			assertEquals(tbl, tru.sub(state, tbl));
			assertEquals(tru, tbl.sub(state, tru));
			try {
				tbl.sub(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.add(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{MUL, RETURN_RHS,});
			assertEquals(fal, tru.mul(state, fal));
			assertEquals(tbl, tru.mul(state, tbl));
			assertEquals(tru, tbl.mul(state, tru));
			try {
				tbl.mul(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{DIV, RETURN_RHS,});
			assertEquals(fal, tru.div(state, fal));
			assertEquals(tbl, tru.div(state, tbl));
			assertEquals(tru, tbl.div(state, tru));
			try {
				tbl.div(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{POW, RETURN_RHS,});
			assertEquals(fal, tru.pow(state, fal));
			assertEquals(tbl, tru.pow(state, tbl));
			assertEquals(tru, tbl.pow(state, tru));
			try {
				tbl.pow(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{MOD, RETURN_RHS,});
			assertEquals(fal, tru.mod(state, fal));
			assertEquals(tbl, tru.mod(state, tbl));
			assertEquals(tru, tbl.mod(state, tru));
			try {
				tbl.mod(state, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tru.sub(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


		} finally {
			state.booleanMetatable = null;
		}
	}

	@Test
	public void testArithMetatagNumberTable() {
		LuaValue zero = ZERO;
		LuaValue one = ONE;
		LuaValue tbl = new LuaTable();

		try {
			tbl.add(state, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.add(state, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{ADD, RETURN_ONE,}));
		assertEquals(one, tbl.add(state, zero));
		assertEquals(one, zero.add(state, tbl));

		try {
			tbl.sub(state, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.sub(state, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{SUB, RETURN_ONE,}));
		assertEquals(one, tbl.sub(state, zero));
		assertEquals(one, zero.sub(state, tbl));

		try {
			tbl.mul(state, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.mul(state, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{MUL, RETURN_ONE,}));
		assertEquals(one, tbl.mul(state, zero));
		assertEquals(one, zero.mul(state, tbl));

		try {
			tbl.div(state, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.div(state, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{DIV, RETURN_ONE,}));
		assertEquals(one, tbl.div(state, zero));
		assertEquals(one, zero.div(state, tbl));

		try {
			tbl.pow(state, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.pow(state, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{POW, RETURN_ONE,}));
		assertEquals(one, tbl.pow(state, zero));
		assertEquals(one, zero.pow(state, tbl));

		try {
			tbl.mod(state, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			zero.mod(state, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{MOD, RETURN_ONE,}));
		assertEquals(one, tbl.mod(state, zero));
		assertEquals(one, zero.mod(state, tbl));
	}

	@Test
	public void testCompareStrings() {
		// these are lexical compare!
		LuaValue sa = valueOf("-1.5");
		LuaValue sb = valueOf("-2.0");
		LuaValue sc = valueOf("1.5");
		LuaValue sd = valueOf("2.0");

		assertEquals(FALSE, sa.lt(state, sa));
		assertEquals(TRUE, sa.lt(state, sb));
		assertEquals(TRUE, sa.lt(state, sc));
		assertEquals(TRUE, sa.lt(state, sd));
		assertEquals(FALSE, sb.lt(state, sa));
		assertEquals(FALSE, sb.lt(state, sb));
		assertEquals(TRUE, sb.lt(state, sc));
		assertEquals(TRUE, sb.lt(state, sd));
		assertEquals(FALSE, sc.lt(state, sa));
		assertEquals(FALSE, sc.lt(state, sb));
		assertEquals(FALSE, sc.lt(state, sc));
		assertEquals(TRUE, sc.lt(state, sd));
		assertEquals(FALSE, sd.lt(state, sa));
		assertEquals(FALSE, sd.lt(state, sb));
		assertEquals(FALSE, sd.lt(state, sc));
		assertEquals(FALSE, sd.lt(state, sd));
	}

	@Test
	public void testLt() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. < 4., ia.lt(state, ib).toboolean());
		assertEquals(.25 < .5, da.lt(state, db).toboolean());
		assertEquals(3. < 4., ia.lt_b(state, ib));
		assertEquals(.25 < .5, da.lt_b(state, db));

		// unlike kinds
		assertEquals(3. < .25, ia.lt(state, da).toboolean());
		assertEquals(.25 < 3., da.lt(state, ia).toboolean());
		assertEquals(3. < .25, ia.lt_b(state, da));
		assertEquals(.25 < 3., da.lt_b(state, ia));
	}

	@Test
	public void testLtEq() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. <= 4., ia.lteq(state, ib).toboolean());
		assertEquals(.25 <= .5, da.lteq(state, db).toboolean());
		assertEquals(3. <= 4., ia.lteq_b(state, ib));
		assertEquals(.25 <= .5, da.lteq_b(state, db));

		// unlike kinds
		assertEquals(3. <= .25, ia.lteq(state, da).toboolean());
		assertEquals(.25 <= 3., da.lteq(state, ia).toboolean());
		assertEquals(3. <= .25, ia.lteq_b(state, da));
		assertEquals(.25 <= 3., da.lteq_b(state, ia));
	}

	@Test
	public void testGt() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. > 4., ia.gt(state, ib).toboolean());
		assertEquals(.25 > .5, da.gt(state, db).toboolean());
		assertEquals(3. > 4., ia.gt_b(state, ib));
		assertEquals(.25 > .5, da.gt_b(state, db));

		// unlike kinds
		assertEquals(3. > .25, ia.gt(state, da).toboolean());
		assertEquals(.25 > 3., da.gt(state, ia).toboolean());
		assertEquals(3. > .25, ia.gt_b(state, da));
		assertEquals(.25 > 3., da.gt_b(state, ia));
	}

	@Test
	public void testGtEq() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. >= 4., ia.gteq(state, ib).toboolean());
		assertEquals(.25 >= .5, da.gteq(state, db).toboolean());
		assertEquals(3. >= 4., ia.gteq_b(state, ib));
		assertEquals(.25 >= .5, da.gteq_b(state, db));

		// unlike kinds
		assertEquals(3. >= .25, ia.gteq(state, da).toboolean());
		assertEquals(.25 >= 3., da.gteq(state, ia).toboolean());
		assertEquals(3. >= .25, ia.gteq_b(state, da));
		assertEquals(.25 >= 3., da.gteq_b(state, ia));
	}

	@Test
	public void testNotEq() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertEquals(3. != 4., ia.neq(state, ib).toboolean());
		assertEquals(.25 != .5, da.neq(state, db).toboolean());
		assertEquals(1.5 != 2., sa.neq(state, sb).toboolean());
		assertEquals(3. != 4., ia.neq_b(state, ib));
		assertEquals(.25 != .5, da.neq_b(state, db));
		assertEquals(1.5 != 2., sa.neq_b(state, sb));

		// unlike kinds
		assertEquals(3. != .25, ia.neq(state, da).toboolean());
		assertEquals(.25 != 3., da.neq(state, ia).toboolean());
		assertEquals(3. != 1.5, ia.neq(state, sa).toboolean());
		assertEquals(1.5 != 3., sa.neq(state, ia).toboolean());
		assertEquals(.25 != 1.5, da.neq(state, sa).toboolean());
		assertEquals(1.5 != .25, sa.neq(state, da).toboolean());
		assertEquals(3. != .25, ia.neq_b(state, da));
		assertEquals(.25 != 3., da.neq_b(state, ia));
		assertEquals(3. != 1.5, ia.neq_b(state, sa));
		assertEquals(1.5 != 3., sa.neq_b(state, ia));
		assertEquals(.25 != 1.5, da.neq_b(state, sa));
		assertEquals(1.5 != .25, sa.neq_b(state, da));
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
			LuaValue.class.getMethod(op, new Class[]{LuaState.class, LuaValue.class}).invoke(a, state, b);
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
			state.booleanMetatable = mt;
			tbl.setMetatable(state, mt);
			tbl2.setMetatable(state, mt);
			assertEquals(tru, tru.lt(state, fal));
			assertEquals(fal, fal.lt(state, tru));
			assertEquals(tbl, tbl.lt(state, tbl2));
			assertEquals(tbl2, tbl2.lt(state, tbl));
			try {
				tbl.lt(state, tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl3.lt(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			assertEquals(fal, tru.lteq(state, fal));
			assertEquals(tru, fal.lteq(state, tru));
			assertEquals(tbl2, tbl.lteq(state, tbl2));
			assertEquals(tbl, tbl2.lteq(state, tbl));
			try {
				tbl.lteq(state, tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl3.lteq(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use right argument
			mt = tableOf(new LuaValue[]{
				LT, RETURN_RHS,
				LE, RETURN_LHS});
			state.booleanMetatable = mt;
			tbl.setMetatable(state, mt);
			tbl2.setMetatable(state, mt);
			assertEquals(fal, tru.lt(state, fal));
			assertEquals(tru, fal.lt(state, tru));
			assertEquals(tbl2, tbl.lt(state, tbl2));
			assertEquals(tbl, tbl2.lt(state, tbl));
			try {
				tbl.lt(state, tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl3.lt(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			assertEquals(tru, tru.lteq(state, fal));
			assertEquals(fal, fal.lteq(state, tru));
			assertEquals(tbl, tbl.lteq(state, tbl2));
			assertEquals(tbl2, tbl2.lteq(state, tbl));
			try {
				tbl.lteq(state, tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl3.lteq(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


		} finally {
			state.booleanMetatable = null;
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
		assertEquals(t, aaa.eq(state, aaa));
		assertEquals(t, aaa.lt(state, baa));
		assertEquals(t, aaa.lteq(state, baa));
		assertEquals(f, aaa.gt(state, baa));
		assertEquals(f, aaa.gteq(state, baa));
		assertEquals(f, baa.lt(state, aaa));
		assertEquals(f, baa.lteq(state, aaa));
		assertEquals(t, baa.gt(state, aaa));
		assertEquals(t, baa.gteq(state, aaa));
		assertEquals(t, aaa.lteq(state, aaa));
		assertEquals(t, aaa.gteq(state, aaa));

		// different case
		assertEquals(t, Aaa.eq(state, Aaa));
		assertEquals(t, Aaa.lt(state, aaa));
		assertEquals(t, Aaa.lteq(state, aaa));
		assertEquals(f, Aaa.gt(state, aaa));
		assertEquals(f, Aaa.gteq(state, aaa));
		assertEquals(f, aaa.lt(state, Aaa));
		assertEquals(f, aaa.lteq(state, Aaa));
		assertEquals(t, aaa.gt(state, Aaa));
		assertEquals(t, aaa.gteq(state, Aaa));
		assertEquals(t, Aaa.lteq(state, Aaa));
		assertEquals(t, Aaa.gteq(state, Aaa));

		// second letter differs
		assertEquals(t, aaa.eq(state, aaa));
		assertEquals(t, aaa.lt(state, aba));
		assertEquals(t, aaa.lteq(state, aba));
		assertEquals(f, aaa.gt(state, aba));
		assertEquals(f, aaa.gteq(state, aba));
		assertEquals(f, aba.lt(state, aaa));
		assertEquals(f, aba.lteq(state, aaa));
		assertEquals(t, aba.gt(state, aaa));
		assertEquals(t, aba.gteq(state, aaa));
		assertEquals(t, aaa.lteq(state, aaa));
		assertEquals(t, aaa.gteq(state, aaa));

		// longer
		assertEquals(t, aaa.eq(state, aaa));
		assertEquals(t, aaa.lt(state, aaaa));
		assertEquals(t, aaa.lteq(state, aaaa));
		assertEquals(f, aaa.gt(state, aaaa));
		assertEquals(f, aaa.gteq(state, aaaa));
		assertEquals(f, aaaa.lt(state, aaa));
		assertEquals(f, aaaa.lteq(state, aaa));
		assertEquals(t, aaaa.gt(state, aaa));
		assertEquals(t, aaaa.gteq(state, aaa));
		assertEquals(t, aaa.lteq(state, aaa));
		assertEquals(t, aaa.gteq(state, aaa));
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
		assertEquals("abcabc", abc.concat(state, abc).tojstring());
		assertEquals("defghi", def.concat(state, ghi).tojstring());
		assertEquals("ghidef", ghi.concat(state, def).tojstring());
		assertEquals("ghidefabcghi", ghi.concat(state, def).concat(state, abc).concat(state, ghi).tojstring());
		assertEquals("123def", n123.concat(state, def).tojstring());
		assertEquals("def123", def.concat(state, n123).tojstring());
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
		b = ghi.concat(state, b);
		assertEquals("ghidef", b.value().tojstring());
		b = abc.concat(state, b);
		assertEquals("abcghidef", b.value().tojstring());
		b = n123.concat(state, b);
		assertEquals("123abcghidef", b.value().tojstring());
		b.setvalue(n123);
		b = def.concat(state, b);
		assertEquals("def123", b.value().tojstring());
		b = abc.concat(state, b);
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
			state.booleanMetatable = tableOf(new LuaValue[]{CONCAT, RETURN_LHS});
			assertEquals(tru, tru.concat(state, tbl));
			assertEquals(tbl, tbl.concat(state, tru));
			assertEquals(tru, tru.concat(state, tbl));
			assertEquals(tbl, tbl.concat(state, tru));
			assertEquals(tru, tru.concat(state, tbl.buffer()).value());
			assertEquals(tbl, tbl.concat(state, tru.buffer()).value());
			assertEquals(fal, fal.concat(state, tbl.concat(state, tru.buffer())).value());
			assertEquals(uda, uda.concat(state, tru.concat(state, tbl.buffer())).value());
			try {
				tbl.concat(state, def);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				def.concat(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl.concat(state, def.buffer()).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				def.concat(state, tbl.buffer()).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				uda.concat(state, def.concat(state, tbl.buffer())).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				ghi.concat(state, tbl.concat(state, def.buffer())).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use right argument
			state.booleanMetatable = tableOf(new LuaValue[]{CONCAT, RETURN_RHS});
			assertEquals(tbl, tru.concat(state, tbl));
			assertEquals(tru, tbl.concat(state, tru));
			assertEquals(tbl, tru.concat(state, tbl.buffer()).value());
			assertEquals(tru, tbl.concat(state, tru.buffer()).value());
			assertEquals(tru, uda.concat(state, tbl.concat(state, tru.buffer())).value());
			assertEquals(tbl, fal.concat(state, tru.concat(state, tbl.buffer())).value());
			try {
				tbl.concat(state, def);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				def.concat(state, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				tbl.concat(state, def.buffer()).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				def.concat(state, tbl.buffer()).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				uda.concat(state, def.concat(state, tbl.buffer())).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				uda.concat(state, tbl.concat(state, def.buffer())).value();
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


		} finally {
			state.booleanMetatable = null;
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
			LuaValue.class.getMethod(op, new Class[]{LuaState.class, LuaValue.class}).invoke(a, state, b);
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
