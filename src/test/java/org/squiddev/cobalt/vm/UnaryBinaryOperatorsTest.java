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

import org.junit.Assert;
import org.junit.Test;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.lib.TwoArgFunction;
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;
import static org.squiddev.cobalt.ValueFactory.tableOf;
import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Tests of basic unary and binary operators on main value types.
 */
public class UnaryBinaryOperatorsTest {
	private LuaState state = new LuaState(new FileResourceManipulator());

	@Test
	public void testEqualsBool() {
		Assert.assertEquals(Constants.FALSE, Constants.FALSE);
		Assert.assertEquals(Constants.TRUE, Constants.TRUE);
		assertTrue(Constants.FALSE.equals(Constants.FALSE));
		assertTrue(Constants.TRUE.equals(Constants.TRUE));
		assertTrue(!Constants.FALSE.equals(Constants.TRUE));
		assertTrue(!Constants.TRUE.equals(Constants.FALSE));
		assertTrue(OperationHelper.eq(state, Constants.FALSE, Constants.FALSE));
		assertTrue(OperationHelper.eq(state, Constants.TRUE, Constants.TRUE));
		assertFalse(OperationHelper.eq(state, Constants.FALSE, Constants.TRUE));
		assertFalse(OperationHelper.eq(state, Constants.TRUE, Constants.FALSE));
		assertEquals(Constants.TRUE, OperationHelper.eq(state, Constants.FALSE, Constants.FALSE) ? Constants.TRUE : Constants.FALSE);
		assertEquals(Constants.TRUE, OperationHelper.eq(state, Constants.TRUE, Constants.TRUE) ? Constants.TRUE : Constants.FALSE);
		assertEquals(Constants.FALSE, OperationHelper.eq(state, Constants.FALSE, Constants.TRUE) ? Constants.TRUE : Constants.FALSE);
		assertEquals(Constants.FALSE, OperationHelper.eq(state, Constants.TRUE, Constants.FALSE) ? Constants.TRUE : Constants.FALSE);
		assertFalse(!OperationHelper.eq(state, Constants.FALSE, Constants.FALSE));
		assertFalse(!OperationHelper.eq(state, Constants.TRUE, Constants.TRUE));
		assertTrue(!OperationHelper.eq(state, Constants.FALSE, Constants.TRUE));
		assertTrue(!OperationHelper.eq(state, Constants.TRUE, Constants.FALSE));
		assertEquals(Constants.FALSE, OperationHelper.eq(state, Constants.FALSE, Constants.FALSE) ? Constants.FALSE : Constants.TRUE);
		assertEquals(Constants.FALSE, OperationHelper.eq(state, Constants.TRUE, Constants.TRUE) ? Constants.FALSE : Constants.TRUE);
		assertEquals(Constants.TRUE, OperationHelper.eq(state, Constants.FALSE, Constants.TRUE) ? Constants.FALSE : Constants.TRUE);
		assertEquals(Constants.TRUE, OperationHelper.eq(state, Constants.TRUE, Constants.FALSE) ? Constants.FALSE : Constants.TRUE);
		assertTrue(Constants.TRUE.toBoolean());
		assertFalse(Constants.FALSE.toBoolean());
	}

	@Test
	public void testNeg() {
		LuaValue ia = valueOf(3), ib = valueOf(-4);
		LuaValue da = valueOf(.25), db = valueOf(-.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("-2.0");

		// like kinds
		assertDoubleEquals(-3., ia.neg(state).toDouble());
		assertDoubleEquals(-.25, da.neg(state).toDouble());
		assertDoubleEquals(-1.5, sa.neg(state).toDouble());
		assertDoubleEquals(4., ib.neg(state).toDouble());
		assertDoubleEquals(.5, db.neg(state).toDouble());
		assertDoubleEquals(2.0, sb.neg(state).toDouble());
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
		assertEquals(ia.toInteger(), 345);
		assertEquals(da.toInteger(), 345);
		assertDoubleEquals(da.toDouble(), 345.0);
		assertDoubleEquals(db.toDouble(), 345.5);

		assertTrue(sa instanceof LuaString);
		assertTrue(sb instanceof LuaString);
		assertTrue(sc instanceof LuaString);
		assertTrue(sd instanceof LuaString);
		assertDoubleEquals(3., sa.toDouble());
		assertDoubleEquals(3., sb.toDouble());
		assertDoubleEquals(-2., sc.toDouble());
		assertDoubleEquals(-2., sd.toDouble());

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
		assertEquals(OperationHelper.eq(state, ia, ib) ? Constants.TRUE : Constants.FALSE, Constants.TRUE);
		assertEquals(OperationHelper.eq(state, sa, sb) ? Constants.TRUE : Constants.FALSE, Constants.TRUE);
		assertEquals(OperationHelper.eq(state, ia, ic) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, sa, sc) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);

		// check arithmetic equality among different types
		assertEquals(OperationHelper.eq(state, ia, sa) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, sa, ia) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);

		// equals with mismatched types
		LuaValue t = new LuaTable();
		assertEquals(OperationHelper.eq(state, ia, t) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, t, ia) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, ia, Constants.FALSE) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, Constants.FALSE, ia) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, ia, Constants.NIL) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, Constants.NIL, ia) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
	}

	@Test
	public void testEqDouble() {
		LuaValue da = LuaDouble.valueOf(345.5), db = LuaDouble.valueOf(345.5), dc = LuaDouble.valueOf(-345.5);
		LuaValue sa = LuaString.valueOf("345.5"), sb = LuaString.valueOf("345.5"), sc = LuaString.valueOf("-345.5");

		// check arithmetic equality among same types
		assertEquals(OperationHelper.eq(state, da, db) ? Constants.TRUE : Constants.FALSE, Constants.TRUE);
		assertEquals(OperationHelper.eq(state, sa, sb) ? Constants.TRUE : Constants.FALSE, Constants.TRUE);
		assertEquals(OperationHelper.eq(state, da, dc) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, sa, sc) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);

		// check arithmetic equality among different types
		assertEquals(OperationHelper.eq(state, da, sa) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, sa, da) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);

		// equals with mismatched types
		LuaValue t = new LuaTable();
		assertEquals(OperationHelper.eq(state, da, t) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, t, da) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, da, Constants.FALSE) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, Constants.FALSE, da) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, da, Constants.NIL) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(OperationHelper.eq(state, Constants.NIL, da) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
	}

	private static final TwoArgFunction RETURN_NIL = new TwoArgFunction() {
		@Override
		public LuaValue call(LuaState state, LuaValue lhs, LuaValue rhs) {
			return Constants.NIL;
		}
	};

	private static final TwoArgFunction RETURN_ONE = new TwoArgFunction() {
		@Override
		public LuaValue call(LuaState state, LuaValue lhs, LuaValue rhs) {
			return Constants.ONE;
		}
	};


	@Test
	public void testEqualsMetatag() {
		LuaValue tru = Constants.TRUE;
		LuaValue fal = Constants.FALSE;
		LuaValue zer = Constants.ZERO;
		LuaValue one = Constants.ONE;
		LuaValue abc = valueOf("abcdef").substring(0, 3);
		LuaValue def = valueOf("abcdef").substring(3, 6);
		LuaValue pi = valueOf(Math.PI);
		LuaValue ee = valueOf(Math.E);
		LuaValue tbl = new LuaTable();
		LuaValue tbl2 = new LuaTable();
		LuaValue tbl3 = new LuaTable();
		LuaValue uda = new LuaUserdata(new Object());
		LuaValue udb = new LuaUserdata(uda.toUserdata());
		LuaValue uda2 = new LuaUserdata(new Object());
		LuaValue uda3 = new LuaUserdata(uda.toUserdata());
		LuaValue nilb = valueOf(Constants.NIL.toBoolean());
		LuaValue oneb = valueOf(Constants.ONE.toBoolean());
		assertEquals(Constants.FALSE, nilb);
		assertEquals(Constants.TRUE, oneb);
		LuaValue smt = state.stringMetatable;
		try {
			// always return nil0
			state.booleanMetatable = tableOf(new LuaValue[]{Constants.EQ, RETURN_NIL,});
			state.numberMetatable = tableOf(new LuaValue[]{Constants.EQ, RETURN_NIL,});
			state.stringMetatable = tableOf(new LuaValue[]{Constants.EQ, RETURN_NIL,});
			tbl.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_NIL,}));
			tbl2.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_NIL,}));
			uda.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_NIL,}));
			udb.setMetatable(state, uda.getMetatable(state));
			uda2.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_NIL,}));
			// diff metatag function
			tbl3.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_ONE,}));
			uda3.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_ONE,}));

			// primitive types or same valu do not invoke metatag as per C implementation
			assertEquals(tru, OperationHelper.eq(state, tru, tru) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, OperationHelper.eq(state, one, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, OperationHelper.eq(state, abc, abc) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, OperationHelper.eq(state, tbl, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, OperationHelper.eq(state, uda, uda) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, OperationHelper.eq(state, uda, udb) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, tru, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, fal, tru) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, zer, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, one, zer) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, pi, ee) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, ee, pi) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, pi, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, one, pi) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, abc, def) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, def, abc) ? Constants.TRUE : Constants.FALSE);
			// different types.  not comparable
			assertEquals(fal, OperationHelper.eq(state, fal, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, tbl, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, tbl, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, one, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, fal, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, one, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, abc, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, one, abc) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, tbl, uda) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, uda, tbl) ? Constants.TRUE : Constants.FALSE);
			// same type, same value, does not invoke metatag op
			assertEquals(tru, OperationHelper.eq(state, tbl, tbl) ? Constants.TRUE : Constants.FALSE);
			// same type, different value, same metatag op.  comparabile via metatag op
			assertEquals(nilb, OperationHelper.eq(state, tbl, tbl2) ? Constants.TRUE : Constants.FALSE);
			assertEquals(nilb, OperationHelper.eq(state, tbl2, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(nilb, OperationHelper.eq(state, uda, uda2) ? Constants.TRUE : Constants.FALSE);
			assertEquals(nilb, OperationHelper.eq(state, uda2, uda) ? Constants.TRUE : Constants.FALSE);
			// same type, different metatag ops.  not comparable
			assertEquals(fal, OperationHelper.eq(state, tbl, tbl3) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, tbl3, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, uda, uda3) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, uda3, uda) ? Constants.TRUE : Constants.FALSE);

			// always use right argument
			state.booleanMetatable = tableOf(new LuaValue[]{Constants.EQ, RETURN_ONE,});
			state.numberMetatable = tableOf(new LuaValue[]{Constants.EQ, RETURN_ONE,});
			state.stringMetatable = tableOf(new LuaValue[]{Constants.EQ, RETURN_ONE,});
			tbl.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_ONE,}));
			tbl2.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_ONE,}));
			uda.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_ONE,}));
			udb.setMetatable(state, uda.getMetatable(state));
			uda2.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_ONE,}));
			// diff metatag function
			tbl3.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_NIL,}));
			uda3.setMetatable(state, tableOf(new LuaValue[]{Constants.EQ, RETURN_NIL,}));

			// primitive types or same value do not invoke metatag as per C implementation
			assertEquals(tru, OperationHelper.eq(state, tru, tru) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, OperationHelper.eq(state, one, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, OperationHelper.eq(state, abc, abc) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, OperationHelper.eq(state, tbl, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, OperationHelper.eq(state, uda, uda) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, OperationHelper.eq(state, uda, udb) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, tru, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, fal, tru) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, zer, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, one, zer) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, pi, ee) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, ee, pi) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, pi, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, one, pi) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, abc, def) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, def, abc) ? Constants.TRUE : Constants.FALSE);
			// different types.  not comparable
			assertEquals(fal, OperationHelper.eq(state, fal, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, tbl, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, tbl, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, one, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, fal, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, one, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, abc, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, one, abc) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, tbl, uda) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, uda, tbl) ? Constants.TRUE : Constants.FALSE);
			// same type, same value, does not invoke metatag op
			assertEquals(tru, OperationHelper.eq(state, tbl, tbl) ? Constants.TRUE : Constants.FALSE);
			// same type, different value, same metatag op.  comparabile via metatag op
			assertEquals(oneb, OperationHelper.eq(state, tbl, tbl2) ? Constants.TRUE : Constants.FALSE);
			assertEquals(oneb, OperationHelper.eq(state, tbl2, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(oneb, OperationHelper.eq(state, uda, uda2) ? Constants.TRUE : Constants.FALSE);
			assertEquals(oneb, OperationHelper.eq(state, uda2, uda) ? Constants.TRUE : Constants.FALSE);
			// same type, different metatag ops.  not comparable
			assertEquals(fal, OperationHelper.eq(state, tbl, tbl3) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, tbl3, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, uda, uda3) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, OperationHelper.eq(state, uda3, uda) ? Constants.TRUE : Constants.FALSE);

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
		assertDoubleEquals(155.0, OperationHelper.add(state, ia, ib).toDouble());
		assertDoubleEquals(58.75, OperationHelper.add(state, da, db).toDouble());
		assertDoubleEquals(29.375, OperationHelper.add(state, sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(166.25, OperationHelper.add(state, ia, da).toDouble());
		assertDoubleEquals(166.25, OperationHelper.add(state, da, ia).toDouble());
		assertDoubleEquals(133.125, OperationHelper.add(state, ia, sa).toDouble());
		assertDoubleEquals(133.125, OperationHelper.add(state, sa, ia).toDouble());
		assertDoubleEquals(77.375, OperationHelper.add(state, da, sa).toDouble());
		assertDoubleEquals(77.375, OperationHelper.add(state, sa, da).toDouble());
	}

	@Test
	public void testSub() {
		LuaValue ia = valueOf(111), ib = valueOf(44);
		LuaValue da = valueOf(55.25), db = valueOf(3.5);
		LuaValue sa = valueOf("22.125"), sb = valueOf("7.25");

		// like kinds
		assertDoubleEquals(67.0, OperationHelper.sub(state, ia, ib).toDouble());
		assertDoubleEquals(51.75, OperationHelper.sub(state, da, db).toDouble());
		assertDoubleEquals(14.875, OperationHelper.sub(state, sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(55.75, OperationHelper.sub(state, ia, da).toDouble());
		assertDoubleEquals(-55.75, OperationHelper.sub(state, da, ia).toDouble());
		assertDoubleEquals(88.875, OperationHelper.sub(state, ia, sa).toDouble());
		assertDoubleEquals(-88.875, OperationHelper.sub(state, sa, ia).toDouble());
		assertDoubleEquals(33.125, OperationHelper.sub(state, da, sa).toDouble());
		assertDoubleEquals(-33.125, OperationHelper.sub(state, sa, da).toDouble());
	}

	@Test
	public void testMul() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(12.0, OperationHelper.mul(state, ia, ib).toDouble());
		assertDoubleEquals(.125, OperationHelper.mul(state, da, db).toDouble());
		assertDoubleEquals(3.0, OperationHelper.mul(state, sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(.75, OperationHelper.mul(state, ia, da).toDouble());
		assertDoubleEquals(.75, OperationHelper.mul(state, da, ia).toDouble());
		assertDoubleEquals(4.5, OperationHelper.mul(state, ia, sa).toDouble());
		assertDoubleEquals(4.5, OperationHelper.mul(state, sa, ia).toDouble());
		assertDoubleEquals(.375, OperationHelper.mul(state, da, sa).toDouble());
		assertDoubleEquals(.375, OperationHelper.mul(state, sa, da).toDouble());
	}

	@Test
	public void testDiv() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(3. / 4., OperationHelper.div(state, ia, ib).toDouble());
		assertDoubleEquals(.25 / .5, OperationHelper.div(state, da, db).toDouble());
		assertDoubleEquals(1.5 / 2., OperationHelper.div(state, sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(3. / .25, OperationHelper.div(state, ia, da).toDouble());
		assertDoubleEquals(.25 / 3., OperationHelper.div(state, da, ia).toDouble());
		assertDoubleEquals(3. / 1.5, OperationHelper.div(state, ia, sa).toDouble());
		assertDoubleEquals(1.5 / 3., OperationHelper.div(state, sa, ia).toDouble());
		assertDoubleEquals(.25 / 1.5, OperationHelper.div(state, da, sa).toDouble());
		assertDoubleEquals(1.5 / .25, OperationHelper.div(state, sa, da).toDouble());
	}

	@Test
	public void testPow() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(4.), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(Math.pow(3., 4.), OperationHelper.pow(state, ia, ib).toDouble());
		assertDoubleEquals(Math.pow(4., .5), OperationHelper.pow(state, da, db).toDouble());
		assertDoubleEquals(Math.pow(1.5, 2.), OperationHelper.pow(state, sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(Math.pow(3., 4.), OperationHelper.pow(state, ia, da).toDouble());
		assertDoubleEquals(Math.pow(4., 3.), OperationHelper.pow(state, da, ia).toDouble());
		assertDoubleEquals(Math.pow(3., 1.5), OperationHelper.pow(state, ia, sa).toDouble());
		assertDoubleEquals(Math.pow(1.5, 3.), OperationHelper.pow(state, sa, ia).toDouble());
		assertDoubleEquals(Math.pow(4., 1.5), OperationHelper.pow(state, da, sa).toDouble());
		assertDoubleEquals(Math.pow(1.5, 4.), OperationHelper.pow(state, sa, da).toDouble());
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
		assertDoubleEquals(luaMod(3., -4.), OperationHelper.mod(state, ia, ib).toDouble());
		assertDoubleEquals(luaMod(.25, -.5), OperationHelper.mod(state, da, db).toDouble());
		assertDoubleEquals(luaMod(1.5, -2.), OperationHelper.mod(state, sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(luaMod(3., .25), OperationHelper.mod(state, ia, da).toDouble());
		assertDoubleEquals(luaMod(.25, 3.), OperationHelper.mod(state, da, ia).toDouble());
		assertDoubleEquals(luaMod(3., 1.5), OperationHelper.mod(state, ia, sa).toDouble());
		assertDoubleEquals(luaMod(1.5, 3.), OperationHelper.mod(state, sa, ia).toDouble());
		assertDoubleEquals(luaMod(.25, 1.5), OperationHelper.mod(state, da, sa).toDouble());
		assertDoubleEquals(luaMod(1.5, .25), OperationHelper.mod(state, sa, da).toDouble());
	}

	@Test
	public void testArithErrors() {
		LuaValue ia = valueOf(111), ib = valueOf(44);
		LuaValue da = valueOf(55.25), db = valueOf(3.5);
		LuaValue sa = valueOf("22.125"), sb = valueOf("7.25");

		String[] ops = {"add", "sub", "mul", "div", "mod", "pow"};
		LuaValue[] vals = {Constants.NIL, Constants.TRUE, tableOf()};
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
			OperationHelper.class.getMethod(op, new Class[]{LuaState.class, LuaValue.class, LuaValue.class}).invoke(null, state, a, b);
		} catch (InvocationTargetException ite) {
			String actual = ite.getTargetException().getMessage();
			if ((!actual.startsWith("attempt to perform arithmetic")) || !actual.contains(type)) {
				fail(op + "op(" + a.typeName() + "," + b.typeName() + ") reported '" + actual + "'");
			}
		} catch (Exception e) {
			fail(op + "(" + a.typeName() + "," + b.typeName() + ") threw " + e);
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
		LuaValue tru = Constants.TRUE;
		LuaValue fal = Constants.FALSE;
		LuaValue tbl = new LuaTable();
		LuaValue tbl2 = new LuaTable();
		try {
			try {
				OperationHelper.add(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.mul(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.div(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.pow(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.mod(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use left argument
			state.booleanMetatable = tableOf(new LuaValue[]{Constants.ADD, RETURN_LHS,});
			assertEquals(tru, OperationHelper.add(state, tru, fal));
			assertEquals(tru, OperationHelper.add(state, tru, tbl));
			assertEquals(tbl, OperationHelper.add(state, tbl, tru));
			try {
				OperationHelper.add(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{Constants.SUB, RETURN_LHS,});
			assertEquals(tru, OperationHelper.sub(state, tru, fal));
			assertEquals(tru, OperationHelper.sub(state, tru, tbl));
			assertEquals(tbl, OperationHelper.sub(state, tbl, tru));
			try {
				OperationHelper.sub(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.add(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{Constants.MUL, RETURN_LHS,});
			assertEquals(tru, OperationHelper.mul(state, tru, fal));
			assertEquals(tru, OperationHelper.mul(state, tru, tbl));
			assertEquals(tbl, OperationHelper.mul(state, tbl, tru));
			try {
				OperationHelper.mul(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{Constants.DIV, RETURN_LHS,});
			assertEquals(tru, OperationHelper.div(state, tru, fal));
			assertEquals(tru, OperationHelper.div(state, tru, tbl));
			assertEquals(tbl, OperationHelper.div(state, tbl, tru));
			try {
				OperationHelper.div(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{Constants.POW, RETURN_LHS,});
			assertEquals(tru, OperationHelper.pow(state, tru, fal));
			assertEquals(tru, OperationHelper.pow(state, tru, tbl));
			assertEquals(tbl, OperationHelper.pow(state, tbl, tru));
			try {
				OperationHelper.pow(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{Constants.MOD, RETURN_LHS,});
			assertEquals(tru, OperationHelper.mod(state, tru, fal));
			assertEquals(tru, OperationHelper.mod(state, tru, tbl));
			assertEquals(tbl, OperationHelper.mod(state, tbl, tru));
			try {
				OperationHelper.mod(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use right argument
			state.booleanMetatable = tableOf(new LuaValue[]{Constants.ADD, RETURN_RHS,});
			assertEquals(fal, OperationHelper.add(state, tru, fal));
			assertEquals(tbl, OperationHelper.add(state, tru, tbl));
			assertEquals(tru, OperationHelper.add(state, tbl, tru));
			try {
				OperationHelper.add(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{Constants.SUB, RETURN_RHS,});
			assertEquals(fal, OperationHelper.sub(state, tru, fal));
			assertEquals(tbl, OperationHelper.sub(state, tru, tbl));
			assertEquals(tru, OperationHelper.sub(state, tbl, tru));
			try {
				OperationHelper.sub(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.add(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{Constants.MUL, RETURN_RHS,});
			assertEquals(fal, OperationHelper.mul(state, tru, fal));
			assertEquals(tbl, OperationHelper.mul(state, tru, tbl));
			assertEquals(tru, OperationHelper.mul(state, tbl, tru));
			try {
				OperationHelper.mul(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{Constants.DIV, RETURN_RHS,});
			assertEquals(fal, OperationHelper.div(state, tru, fal));
			assertEquals(tbl, OperationHelper.div(state, tru, tbl));
			assertEquals(tru, OperationHelper.div(state, tbl, tru));
			try {
				OperationHelper.div(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{Constants.POW, RETURN_RHS,});
			assertEquals(fal, OperationHelper.pow(state, tru, fal));
			assertEquals(tbl, OperationHelper.pow(state, tru, tbl));
			assertEquals(tru, OperationHelper.pow(state, tbl, tru));
			try {
				OperationHelper.pow(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			state.booleanMetatable = tableOf(new LuaValue[]{Constants.MOD, RETURN_RHS,});
			assertEquals(fal, OperationHelper.mod(state, tru, fal));
			assertEquals(tbl, OperationHelper.mod(state, tru, tbl));
			assertEquals(tru, OperationHelper.mod(state, tbl, tru));
			try {
				OperationHelper.mod(state, tbl, tbl2);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.sub(state, tru, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


		} finally {
			state.booleanMetatable = null;
		}
	}

	@Test
	public void testArithMetatagNumberTable() {
		LuaValue zero = Constants.ZERO;
		LuaValue one = Constants.ONE;
		LuaValue tbl = new LuaTable();

		try {
			OperationHelper.add(state, tbl, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			OperationHelper.add(state, zero, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{Constants.ADD, RETURN_ONE,}));
		assertEquals(one, OperationHelper.add(state, tbl, zero));
		assertEquals(one, OperationHelper.add(state, zero, tbl));

		try {
			OperationHelper.sub(state, tbl, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			OperationHelper.sub(state, zero, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{Constants.SUB, RETURN_ONE,}));
		assertEquals(one, OperationHelper.sub(state, tbl, zero));
		assertEquals(one, OperationHelper.sub(state, zero, tbl));

		try {
			OperationHelper.mul(state, tbl, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			OperationHelper.mul(state, zero, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{Constants.MUL, RETURN_ONE,}));
		assertEquals(one, OperationHelper.mul(state, tbl, zero));
		assertEquals(one, OperationHelper.mul(state, zero, tbl));

		try {
			OperationHelper.div(state, tbl, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			OperationHelper.div(state, zero, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{Constants.DIV, RETURN_ONE,}));
		assertEquals(one, OperationHelper.div(state, tbl, zero));
		assertEquals(one, OperationHelper.div(state, zero, tbl));

		try {
			OperationHelper.pow(state, tbl, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			OperationHelper.pow(state, zero, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{Constants.POW, RETURN_ONE,}));
		assertEquals(one, OperationHelper.pow(state, tbl, zero));
		assertEquals(one, OperationHelper.pow(state, zero, tbl));

		try {
			OperationHelper.mod(state, tbl, zero);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		try {
			OperationHelper.mod(state, zero, tbl);
			fail("did not throw error");
		} catch (LuaError ignored) {
		}

		tbl.setMetatable(state, tableOf(new LuaValue[]{Constants.MOD, RETURN_ONE,}));
		assertEquals(one, OperationHelper.mod(state, tbl, zero));
		assertEquals(one, OperationHelper.mod(state, zero, tbl));
	}

	@Test
	public void testCompareStrings() {
		// these are lexical compare!
		LuaValue sa = valueOf("-1.5");
		LuaValue sb = valueOf("-2.0");
		LuaValue sc = valueOf("1.5");
		LuaValue sd = valueOf("2.0");

		assertEquals(Constants.FALSE, OperationHelper.ltValue(state, sa, sa));
		assertEquals(Constants.TRUE, OperationHelper.ltValue(state, sa, sb));
		assertEquals(Constants.TRUE, OperationHelper.ltValue(state, sa, sc));
		assertEquals(Constants.TRUE, OperationHelper.ltValue(state, sa, sd));
		assertEquals(Constants.FALSE, OperationHelper.ltValue(state, sb, sa));
		assertEquals(Constants.FALSE, OperationHelper.ltValue(state, sb, sb));
		assertEquals(Constants.TRUE, OperationHelper.ltValue(state, sb, sc));
		assertEquals(Constants.TRUE, OperationHelper.ltValue(state, sb, sd));
		assertEquals(Constants.FALSE, OperationHelper.ltValue(state, sc, sa));
		assertEquals(Constants.FALSE, OperationHelper.ltValue(state, sc, sb));
		assertEquals(Constants.FALSE, OperationHelper.ltValue(state, sc, sc));
		assertEquals(Constants.TRUE, OperationHelper.ltValue(state, sc, sd));
		assertEquals(Constants.FALSE, OperationHelper.ltValue(state, sd, sa));
		assertEquals(Constants.FALSE, OperationHelper.ltValue(state, sd, sb));
		assertEquals(Constants.FALSE, OperationHelper.ltValue(state, sd, sc));
		assertEquals(Constants.FALSE, OperationHelper.ltValue(state, sd, sd));
	}

	@Test
	public void testLt() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. < 4., OperationHelper.ltValue(state, ia, ib).toBoolean());
		assertEquals(.25 < .5, OperationHelper.ltValue(state, da, db).toBoolean());
		assertEquals(3. < 4., OperationHelper.lt(state, ia, ib));
		assertEquals(.25 < .5, OperationHelper.lt(state, da, db));

		// unlike kinds
		assertEquals(3. < .25, OperationHelper.ltValue(state, ia, da).toBoolean());
		assertEquals(.25 < 3., OperationHelper.ltValue(state, da, ia).toBoolean());
		assertEquals(3. < .25, OperationHelper.lt(state, ia, da));
		assertEquals(.25 < 3., OperationHelper.lt(state, da, ia));
	}

	@Test
	public void testLtEq() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. <= 4., OperationHelper.leValue(state, ia, ib).toBoolean());
		assertEquals(.25 <= .5, OperationHelper.leValue(state, da, db).toBoolean());
		assertEquals(3. <= 4., OperationHelper.le(state, ia, ib));
		assertEquals(.25 <= .5, OperationHelper.le(state, da, db));

		// unlike kinds
		assertEquals(3. <= .25, OperationHelper.leValue(state, ia, da).toBoolean());
		assertEquals(.25 <= 3., OperationHelper.leValue(state, da, ia).toBoolean());
		assertEquals(3. <= .25, OperationHelper.le(state, ia, da));
		assertEquals(.25 <= 3., OperationHelper.le(state, da, ia));
	}

	@Test
	public void testGt() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. > 4., OperationHelper.ltValue(state, ib, ia).toBoolean());
		assertEquals(.25 > .5, OperationHelper.ltValue(state, db, da).toBoolean());
		assertEquals(3. > 4., OperationHelper.lt(state, ib, ia));
		assertEquals(.25 > .5, OperationHelper.lt(state, db, da));

		// unlike kinds
		assertEquals(3. > .25, OperationHelper.ltValue(state, da, ia).toBoolean());
		assertEquals(.25 > 3., OperationHelper.ltValue(state, ia, da).toBoolean());
		assertEquals(3. > .25, OperationHelper.lt(state, da, ia));
		assertEquals(.25 > 3., OperationHelper.lt(state, ia, da));
	}

	@Test
	public void testGtEq() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. >= 4., OperationHelper.leValue(state, ib, ia).toBoolean());
		assertEquals(.25 >= .5, OperationHelper.leValue(state, db, da).toBoolean());
		assertEquals(3. >= 4., OperationHelper.le(state, ib, ia));
		assertEquals(.25 >= .5, OperationHelper.le(state, db, da));

		// unlike kinds
		assertEquals(3. >= .25, OperationHelper.leValue(state, da, ia).toBoolean());
		assertEquals(.25 >= 3., OperationHelper.leValue(state, ia, da).toBoolean());
		assertEquals(3. >= .25, OperationHelper.le(state, da, ia));
		assertEquals(.25 >= 3., OperationHelper.le(state, ia, da));
	}

	@Test
	public void testNotEq() {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertEquals(3. != 4., (OperationHelper.eq(state, ia, ib) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(.25 != .5, (OperationHelper.eq(state, da, db) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(1.5 != 2., (OperationHelper.eq(state, sa, sb) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(3. != 4., !OperationHelper.eq(state, ia, ib));
		assertEquals(.25 != .5, !OperationHelper.eq(state, da, db));
		assertEquals(1.5 != 2., !OperationHelper.eq(state, sa, sb));

		// unlike kinds
		assertEquals(3. != .25, (OperationHelper.eq(state, ia, da) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(.25 != 3., (OperationHelper.eq(state, da, ia) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(3. != 1.5, (OperationHelper.eq(state, ia, sa) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(1.5 != 3., (OperationHelper.eq(state, sa, ia) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(.25 != 1.5, (OperationHelper.eq(state, da, sa) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(1.5 != .25, (OperationHelper.eq(state, sa, da) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(3. != .25, !OperationHelper.eq(state, ia, da));
		assertEquals(.25 != 3., !OperationHelper.eq(state, da, ia));
		assertEquals(3. != 1.5, !OperationHelper.eq(state, ia, sa));
		assertEquals(1.5 != 3., !OperationHelper.eq(state, sa, ia));
		assertEquals(.25 != 1.5, !OperationHelper.eq(state, da, sa));
		assertEquals(1.5 != .25, !OperationHelper.eq(state, sa, da));
	}

	@Test
	public void testCompareErrors() {
		LuaValue ia = valueOf(111), ib = valueOf(44);
		LuaValue da = valueOf(55.25), db = valueOf(3.5);
		LuaValue sa = valueOf("22.125"), sb = valueOf("7.25");

		String[] ops = {"lt", "le",};
		LuaValue[] vals = {Constants.NIL, Constants.TRUE, tableOf()};
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
			OperationHelper.class.getMethod(op, new Class[]{LuaState.class, LuaValue.class, LuaValue.class}).invoke(null, state, a, b);
		} catch (InvocationTargetException ite) {
			String actual = ite.getTargetException().getMessage();
			if ((!actual.startsWith("attempt to compare")) || !actual.contains(type)) {
				fail(op + "(" + a.typeName() + "," + b.typeName() + ") reported '" + actual + "'");
			}
		} catch (Exception e) {
			fail(op + "(" + a.typeName() + "," + b.typeName() + ") threw " + e);
		}
	}

	@Test
	public void testCompareMetatag() {
		LuaValue tru = Constants.TRUE;
		LuaValue fal = Constants.FALSE;
		LuaValue tbl = new LuaTable();
		LuaValue tbl2 = new LuaTable();
		LuaValue tbl3 = new LuaTable();
		try {
			// always use left argument
			LuaValue mt = tableOf(new LuaValue[]{
				Constants.LT, RETURN_LHS,
				Constants.LE, RETURN_RHS,
			});
			state.booleanMetatable = mt;
			tbl.setMetatable(state, mt);
			tbl2.setMetatable(state, mt);
			assertEquals(tru, OperationHelper.ltValue(state, tru, fal));
			assertEquals(fal, OperationHelper.ltValue(state, fal, tru));
			assertEquals(tbl, OperationHelper.ltValue(state, tbl, tbl2));
			assertEquals(tbl2, OperationHelper.ltValue(state, tbl2, tbl));
			try {
				OperationHelper.ltValue(state, tbl, tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.ltValue(state, tbl3, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			assertEquals(fal, OperationHelper.leValue(state, tru, fal));
			assertEquals(tru, OperationHelper.leValue(state, fal, tru));
			assertEquals(tbl2, OperationHelper.leValue(state, tbl, tbl2));
			assertEquals(tbl, OperationHelper.leValue(state, tbl2, tbl));
			try {
				OperationHelper.leValue(state, tbl, tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.leValue(state, tbl3, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use right argument
			mt = tableOf(new LuaValue[]{
				Constants.LT, RETURN_RHS,
				Constants.LE, RETURN_LHS});
			state.booleanMetatable = mt;
			tbl.setMetatable(state, mt);
			tbl2.setMetatable(state, mt);
			assertEquals(fal, OperationHelper.ltValue(state, tru, fal));
			assertEquals(tru, OperationHelper.ltValue(state, fal, tru));
			assertEquals(tbl2, OperationHelper.ltValue(state, tbl, tbl2));
			assertEquals(tbl, OperationHelper.ltValue(state, tbl2, tbl));
			try {
				OperationHelper.ltValue(state, tbl, tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.ltValue(state, tbl3, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			assertEquals(tru, OperationHelper.leValue(state, tru, fal));
			assertEquals(fal, OperationHelper.leValue(state, fal, tru));
			assertEquals(tbl, OperationHelper.leValue(state, tbl, tbl2));
			assertEquals(tbl2, OperationHelper.leValue(state, tbl2, tbl));
			try {
				OperationHelper.leValue(state, tbl, tbl3);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.leValue(state, tbl3, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


		} finally {
			state.booleanMetatable = null;
		}
	}

	@Test
	public void testLexicalComparison() {
		LuaValue aaa = valueOf("aaa");
		LuaValue baa = valueOf("baa");
		LuaValue Aaa = valueOf("Aaa");
		LuaValue aba = valueOf("aba");
		LuaValue aaaa = valueOf("aaaa");
		LuaValue t = Constants.TRUE;
		LuaValue f = Constants.FALSE;

		// basics
		assertEquals(t, OperationHelper.eq(state, aaa, aaa) ? Constants.TRUE : Constants.FALSE);
		assertEquals(t, OperationHelper.ltValue(state, aaa, baa));
		assertEquals(t, OperationHelper.leValue(state, aaa, baa));
		assertEquals(f, OperationHelper.ltValue(state, baa, aaa));
		assertEquals(f, OperationHelper.leValue(state, baa, aaa));
		assertEquals(f, OperationHelper.ltValue(state, baa, aaa));
		assertEquals(f, OperationHelper.leValue(state, baa, aaa));
		assertEquals(t, OperationHelper.ltValue(state, aaa, baa));
		assertEquals(t, OperationHelper.leValue(state, aaa, baa));
		assertEquals(t, OperationHelper.leValue(state, aaa, aaa));
		assertEquals(t, OperationHelper.leValue(state, aaa, aaa));

		// different case
		assertEquals(t, OperationHelper.eq(state, Aaa, Aaa) ? Constants.TRUE : Constants.FALSE);
		assertEquals(t, OperationHelper.ltValue(state, Aaa, aaa));
		assertEquals(t, OperationHelper.leValue(state, Aaa, aaa));
		assertEquals(f, OperationHelper.ltValue(state, aaa, Aaa));
		assertEquals(f, OperationHelper.leValue(state, aaa, Aaa));
		assertEquals(f, OperationHelper.ltValue(state, aaa, Aaa));
		assertEquals(f, OperationHelper.leValue(state, aaa, Aaa));
		assertEquals(t, OperationHelper.ltValue(state, Aaa, aaa));
		assertEquals(t, OperationHelper.leValue(state, Aaa, aaa));
		assertEquals(t, OperationHelper.leValue(state, Aaa, Aaa));
		assertEquals(t, OperationHelper.leValue(state, Aaa, Aaa));

		// second letter differs
		assertEquals(t, OperationHelper.eq(state, aaa, aaa) ? Constants.TRUE : Constants.FALSE);
		assertEquals(t, OperationHelper.ltValue(state, aaa, aba));
		assertEquals(t, OperationHelper.leValue(state, aaa, aba));
		assertEquals(f, OperationHelper.ltValue(state, aba, aaa));
		assertEquals(f, OperationHelper.leValue(state, aba, aaa));
		assertEquals(f, OperationHelper.ltValue(state, aba, aaa));
		assertEquals(f, OperationHelper.leValue(state, aba, aaa));
		assertEquals(t, OperationHelper.ltValue(state, aaa, aba));
		assertEquals(t, OperationHelper.leValue(state, aaa, aba));
		assertEquals(t, OperationHelper.leValue(state, aaa, aaa));
		assertEquals(t, OperationHelper.leValue(state, aaa, aaa));

		// longer
		assertEquals(t, OperationHelper.eq(state, aaa, aaa) ? Constants.TRUE : Constants.FALSE);
		assertEquals(t, OperationHelper.ltValue(state, aaa, aaaa));
		assertEquals(t, OperationHelper.leValue(state, aaa, aaaa));
		assertEquals(f, OperationHelper.ltValue(state, aaaa, aaa));
		assertEquals(f, OperationHelper.leValue(state, aaaa, aaa));
		assertEquals(f, OperationHelper.ltValue(state, aaaa, aaa));
		assertEquals(f, OperationHelper.leValue(state, aaaa, aaa));
		assertEquals(t, OperationHelper.ltValue(state, aaa, aaaa));
		assertEquals(t, OperationHelper.leValue(state, aaa, aaaa));
		assertEquals(t, OperationHelper.leValue(state, aaa, aaa));
		assertEquals(t, OperationHelper.leValue(state, aaa, aaa));
	}

	@Test
	public void testBuffer() {
		LuaValue abc = valueOf("abcdefghi").substring(0, 3);
		LuaValue def = valueOf("abcdefghi").substring(3, 6);
		LuaValue ghi = valueOf("abcdefghi").substring(6, 9);
		LuaValue n123 = valueOf(123);

		// basic append
		Buffer b = new Buffer();
		assertEquals("", b.value().toString());
		b.append(def);
		assertEquals("def", b.value().toString());
		b.append(abc);
		assertEquals("defabc", b.value().toString());
		b.append(ghi);
		assertEquals("defabcghi", b.value().toString());
		b.append(n123);
		assertEquals("defabcghi123", b.value().toString());

		// basic prepend
		b = new Buffer();
		assertEquals("", b.value().toString());
		b.prepend(def.strvalue());
		assertEquals("def", b.value().toString());
		b.prepend(ghi.strvalue());
		assertEquals("ghidef", b.value().toString());
		b.prepend(abc.strvalue());
		assertEquals("abcghidef", b.value().toString());
		b.prepend(n123.strvalue());
		assertEquals("123abcghidef", b.value().toString());

		// mixed append, prepend
		b = new Buffer();
		assertEquals("", b.value().toString());
		b.append(def);
		assertEquals("def", b.value().toString());
		b.append(abc);
		assertEquals("defabc", b.value().toString());
		b.prepend(ghi.strvalue());
		assertEquals("ghidefabc", b.value().toString());
		b.prepend(n123.strvalue());
		assertEquals("123ghidefabc", b.value().toString());
		b.append(def);
		assertEquals("123ghidefabcdef", b.value().toString());
		b.append(abc);
		assertEquals("123ghidefabcdefabc", b.value().toString());
		b.prepend(ghi.strvalue());
		assertEquals("ghi123ghidefabcdefabc", b.value().toString());
		b.prepend(n123.strvalue());
		assertEquals("123ghi123ghidefabcdefabc", b.value().toString());

		// value
		b = new Buffer(def);
		assertEquals("def", b.value().toString());
		b.append(abc);
		assertEquals("defabc", b.value().toString());
		b.prepend(ghi.strvalue());
		assertEquals("ghidefabc", b.value().toString());
		b.setvalue(def);
		assertEquals("def", b.value().toString());
		b.prepend(ghi.strvalue());
		assertEquals("ghidef", b.value().toString());
		b.append(abc);
		assertEquals("ghidefabc", b.value().toString());
	}

	@Test
	public void testConcat() {
		LuaValue abc = valueOf("abcdefghi").substring(0, 3);
		LuaValue def = valueOf("abcdefghi").substring(3, 6);
		LuaValue ghi = valueOf("abcdefghi").substring(6, 9);
		LuaValue n123 = valueOf(123);

		assertEquals("abc", abc.toString());
		assertEquals("def", def.toString());
		assertEquals("ghi", ghi.toString());
		assertEquals("123", n123.toString());
		assertEquals("abcabc", OperationHelper.concat(state, abc, abc).toString());
		assertEquals("defghi", OperationHelper.concat(state, def, ghi).toString());
		assertEquals("ghidef", OperationHelper.concat(state, ghi, def).toString());
		assertEquals("ghidefabcghi", OperationHelper.concat(state, OperationHelper.concat(state, OperationHelper.concat(state, ghi, def), abc), ghi).toString());
		assertEquals("123def", OperationHelper.concat(state, n123, def).toString());
		assertEquals("def123", OperationHelper.concat(state, def, n123).toString());
	}

	@Test
	public void testConcatMetatag() {
		LuaValue def = valueOf("abcdefghi").substring(3, 6);
		LuaValue ghi = valueOf("abcdefghi").substring(6, 9);
		LuaValue tru = Constants.TRUE;
		LuaValue fal = Constants.FALSE;
		LuaValue tbl = new LuaTable();
		LuaValue uda = new LuaUserdata(new Object());
		try {
			// always use left argument
			state.booleanMetatable = tableOf(new LuaValue[]{Constants.CONCAT, RETURN_LHS});
			assertEquals(tru, OperationHelper.concat(state, tru, tbl));
			assertEquals(tbl, OperationHelper.concat(state, tbl, tru));
			assertEquals(tru, OperationHelper.concat(state, tru, tbl));
			assertEquals(tbl, OperationHelper.concat(state, tbl, tru));
			try {
				OperationHelper.concat(state, tbl, def);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.concat(state, def, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.concat(state, uda, OperationHelper.concat(state, def, tbl));
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.concat(state, ghi, OperationHelper.concat(state, tbl, def));
				fail("did not throw error");
			} catch (LuaError ignored) {
			}


			// always use right argument
			state.booleanMetatable = tableOf(new LuaValue[]{Constants.CONCAT, RETURN_RHS});
			assertEquals(tbl, OperationHelper.concat(state, tru, tbl));
			assertEquals(tru, OperationHelper.concat(state, tbl, tru));
			assertEquals(tbl, OperationHelper.concat(state, tru, tbl));
			assertEquals(tru, OperationHelper.concat(state, tbl, tru));
			assertEquals(tru, OperationHelper.concat(state, uda, OperationHelper.concat(state, tbl, tru)));
			assertEquals(tbl, OperationHelper.concat(state, fal, OperationHelper.concat(state, tru, tbl)));
			try {
				OperationHelper.concat(state, tbl, def);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.concat(state, def, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.concat(state, tbl, def);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.concat(state, def, tbl);
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.concat(state, uda, OperationHelper.concat(state, def, tbl));
				fail("did not throw error");
			} catch (LuaError ignored) {
			}

			try {
				OperationHelper.concat(state, uda, OperationHelper.concat(state, tbl, def));
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
		LuaValue[] vals = {Constants.NIL, Constants.TRUE, tableOf()};
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
			OperationHelper.class.getMethod(op, new Class[]{LuaState.class, LuaValue.class, LuaValue.class}).invoke(null, state, a, b);
		} catch (InvocationTargetException ite) {
			String actual = ite.getTargetException().getMessage();
			if ((!actual.startsWith("attempt to concatenate")) || !actual.contains(type)) {
				fail(op + "(" + a.typeName() + "," + b.typeName() + ") reported '" + actual + "'");
			}
		} catch (Exception e) {
			fail(op + "(" + a.typeName() + "," + b.typeName() + ") threw " + e);
		}
	}


	public static void assertDoubleEquals(double a, double b) {
		assertEquals(a, b, 1e-10);
	}
}
