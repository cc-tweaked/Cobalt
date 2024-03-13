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

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;
import static org.squiddev.cobalt.ValueFactory.tableOf;
import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * Tests of basic unary and binary operators on main value types.
 */
public class UnaryBinaryOperatorsTest {
	private final LuaState state = new LuaState();
	private final LuaOperators.Comparison eq = LuaOperators.createComparison(state, "==");
	private final LuaOperators.Comparison lt = LuaOperators.createComparison(state, "<");
	private final LuaOperators.Comparison le = LuaOperators.createComparison(state, "<=");

	private final LuaOperators.UnaryOperator neg = LuaOperators.createUnOp(state, "-");

	private final LuaOperators.BinaryOperator add = LuaOperators.createBinOp(state, "+");
	private final LuaOperators.BinaryOperator sub = LuaOperators.createBinOp(state, "-");
	private final LuaOperators.BinaryOperator mul = LuaOperators.createBinOp(state, "*");
	private final LuaOperators.BinaryOperator div = LuaOperators.createBinOp(state, "/");
	private final LuaOperators.BinaryOperator pow = LuaOperators.createBinOp(state, "^");
	private final LuaOperators.BinaryOperator mod = LuaOperators.createBinOp(state, "%");
	private final LuaOperators.BinaryOperator concat = LuaOperators.createBinOp(state, "..");

	@Test
	public void testEqualsBool() throws LuaError, UnwindThrowable {
		assertEquals(Constants.FALSE, Constants.FALSE);
		assertEquals(Constants.TRUE, Constants.TRUE);
		assertEquals(Constants.FALSE, Constants.FALSE);
		assertEquals(Constants.TRUE, Constants.TRUE);
		assertFalse(Constants.FALSE.equals(Constants.TRUE));
		assertFalse(Constants.TRUE.equals(Constants.FALSE));
		assertTrue(eq.apply(Constants.FALSE, Constants.FALSE));
		assertTrue(eq.apply(Constants.TRUE, Constants.TRUE));
		assertFalse(eq.apply(Constants.FALSE, Constants.TRUE));
		assertFalse(eq.apply(Constants.TRUE, Constants.FALSE));
		assertEquals(Constants.TRUE, eq.apply(Constants.FALSE, Constants.FALSE) ? Constants.TRUE : Constants.FALSE);
		assertEquals(Constants.TRUE, eq.apply(Constants.TRUE, Constants.TRUE) ? Constants.TRUE : Constants.FALSE);
		assertEquals(Constants.FALSE, eq.apply(Constants.FALSE, Constants.TRUE) ? Constants.TRUE : Constants.FALSE);
		assertEquals(Constants.FALSE, eq.apply(Constants.TRUE, Constants.FALSE) ? Constants.TRUE : Constants.FALSE);
		assertTrue(eq.apply(Constants.FALSE, Constants.FALSE));
		assertTrue(eq.apply(Constants.TRUE, Constants.TRUE));
		assertFalse(eq.apply(Constants.FALSE, Constants.TRUE));
		assertFalse(eq.apply(Constants.TRUE, Constants.FALSE));
		assertEquals(Constants.FALSE, eq.apply(Constants.FALSE, Constants.FALSE) ? Constants.FALSE : Constants.TRUE);
		assertEquals(Constants.FALSE, eq.apply(Constants.TRUE, Constants.TRUE) ? Constants.FALSE : Constants.TRUE);
		assertEquals(Constants.TRUE, eq.apply(Constants.FALSE, Constants.TRUE) ? Constants.FALSE : Constants.TRUE);
		assertEquals(Constants.TRUE, eq.apply(Constants.TRUE, Constants.FALSE) ? Constants.FALSE : Constants.TRUE);
		assertTrue(Constants.TRUE.toBoolean());
		assertFalse(Constants.FALSE.toBoolean());
	}

	@Test
	public void testNeg() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(3), ib = valueOf(-4);
		LuaValue da = valueOf(.25), db = valueOf(-.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("-2.0");

		// like kinds
		assertDoubleEquals(-3., neg.apply(ia).toDouble());
		assertDoubleEquals(-.25, neg.apply(da).toDouble());
		assertDoubleEquals(-1.5, neg.apply(sa).toDouble());
		assertDoubleEquals(4., neg.apply(ib).toDouble());
		assertDoubleEquals(.5, neg.apply(db).toDouble());
		assertDoubleEquals(2.0, neg.apply(sb).toDouble());
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
		assertNotEquals(ia, ic);
		assertNotEquals(sa, sc);

		// check object equality for different types
		assertNotEquals(ia, sa);
		assertNotEquals(sa, ia);
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
		assertNotEquals(da, dc);
		assertNotEquals(sa, sc);

		// check object equality for different types
		assertNotEquals(da, sa);
		assertNotEquals(sa, da);
	}

	@Test
	public void testEqInt() throws LuaError, UnwindThrowable {
		LuaValue ia = LuaInteger.valueOf(345), ib = LuaInteger.valueOf(345), ic = LuaInteger.valueOf(-123);
		LuaValue sa = LuaString.valueOf("345"), sb = LuaString.valueOf("345"), sc = LuaString.valueOf("-345");

		// check arithmetic equality among same types
		assertEquals(eq.apply(ia, ib) ? Constants.TRUE : Constants.FALSE, Constants.TRUE);
		assertEquals(eq.apply(sa, sb) ? Constants.TRUE : Constants.FALSE, Constants.TRUE);
		assertEquals(eq.apply(ia, ic) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(sa, sc) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);

		// check arithmetic equality among different types
		assertEquals(eq.apply(ia, sa) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(sa, ia) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);

		// equals with mismatched types
		LuaValue t = new LuaTable();
		assertEquals(eq.apply(ia, t) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(t, ia) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(ia, Constants.FALSE) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(Constants.FALSE, ia) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(ia, Constants.NIL) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(Constants.NIL, ia) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
	}

	@Test
	public void testEqDouble() throws LuaError, UnwindThrowable {
		LuaValue da = LuaDouble.valueOf(345.5), db = LuaDouble.valueOf(345.5), dc = LuaDouble.valueOf(-345.5);
		LuaValue sa = LuaString.valueOf("345.5"), sb = LuaString.valueOf("345.5"), sc = LuaString.valueOf("-345.5");

		// check arithmetic equality among same types
		assertEquals(eq.apply(da, db) ? Constants.TRUE : Constants.FALSE, Constants.TRUE);
		assertEquals(eq.apply(sa, sb) ? Constants.TRUE : Constants.FALSE, Constants.TRUE);
		assertEquals(eq.apply(da, dc) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(sa, sc) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);

		// check arithmetic equality among different types
		assertEquals(eq.apply(da, sa) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(sa, da) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);

		// equals with mismatched types
		LuaValue t = new LuaTable();
		assertEquals(eq.apply(da, t) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(t, da) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(da, Constants.FALSE) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(Constants.FALSE, da) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(da, Constants.NIL) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
		assertEquals(eq.apply(Constants.NIL, da) ? Constants.TRUE : Constants.FALSE, Constants.FALSE);
	}

	private static final LibFunction RETURN_NIL = LibFunction.create((state, lhs, rhs) -> Constants.NIL);

	private static final LibFunction RETURN_ONE = LibFunction.create((state, lhs, rhs) -> Constants.ONE);


	@Test
	public void testEqualsMetatag() throws LuaError, UnwindThrowable {
		LuaValue tru = Constants.TRUE;
		LuaValue fal = Constants.FALSE;
		LuaValue zer = Constants.ZERO;
		LuaValue one = Constants.ONE;
		LuaValue abc = valueOf("abcdef").substringOfEnd(0, 3);
		LuaValue def = valueOf("abcdef").substringOfEnd(3, 6);
		LuaValue pi = valueOf(Math.PI);
		LuaValue ee = valueOf(Math.E);
		LuaValue tbl = new LuaTable();
		LuaValue tbl2 = new LuaTable();
		LuaValue tbl3 = new LuaTable();
		LuaUserdata uda = new LuaUserdata(new Object());
		LuaValue udb = new LuaUserdata(uda.toUserdata());
		LuaValue uda2 = new LuaUserdata(new Object());
		LuaValue uda3 = new LuaUserdata(uda.toUserdata());
		LuaValue nilb = valueOf(Constants.NIL.toBoolean());
		LuaValue oneb = valueOf(Constants.ONE.toBoolean());
		assertEquals(Constants.FALSE, nilb);
		assertEquals(Constants.TRUE, oneb);
		LuaTable smt = state.stringMetatable;
		try {
			// always return nil0
			state.booleanMetatable = tableOf(Constants.EQ, RETURN_NIL);
			state.numberMetatable = tableOf(Constants.EQ, RETURN_NIL);
			state.stringMetatable = tableOf(Constants.EQ, RETURN_NIL);
			tbl.setMetatable(state, tableOf(Constants.EQ, RETURN_NIL));
			tbl2.setMetatable(state, tableOf(Constants.EQ, RETURN_NIL));
			uda.setMetatable(state, tableOf(Constants.EQ, RETURN_NIL));
			udb.setMetatable(state, uda.getMetatable(state));
			uda2.setMetatable(state, tableOf(Constants.EQ, RETURN_NIL));
			// diff metatag function
			tbl3.setMetatable(state, tableOf(Constants.EQ, RETURN_ONE));
			uda3.setMetatable(state, tableOf(Constants.EQ, RETURN_ONE));

			// primitive types or same valu do not invoke metatag as per C implementation
			assertEquals(tru, eq.apply(tru, tru) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, eq.apply(one, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, eq.apply(abc, abc) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, eq.apply(tbl, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, eq.apply(uda, uda) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, eq.apply(uda, udb) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(tru, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(fal, tru) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(zer, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(one, zer) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(pi, ee) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(ee, pi) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(pi, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(one, pi) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(abc, def) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(def, abc) ? Constants.TRUE : Constants.FALSE);
			// different types.  not comparable
			assertEquals(fal, eq.apply(fal, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(tbl, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(tbl, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(one, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(fal, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(one, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(abc, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(one, abc) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(tbl, uda) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(uda, tbl) ? Constants.TRUE : Constants.FALSE);
			// same type, same value, does not invoke metatag op
			assertEquals(tru, eq.apply(tbl, tbl) ? Constants.TRUE : Constants.FALSE);
			// same type, different value, same metatag op.  comparabile via metatag op
			assertEquals(nilb, eq.apply(tbl, tbl2) ? Constants.TRUE : Constants.FALSE);
			assertEquals(nilb, eq.apply(tbl2, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(nilb, eq.apply(uda, uda2) ? Constants.TRUE : Constants.FALSE);
			assertEquals(nilb, eq.apply(uda2, uda) ? Constants.TRUE : Constants.FALSE);
			// same type, different metatag ops.  not comparable
			assertEquals(fal, eq.apply(tbl, tbl3) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(tbl3, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(uda, uda3) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(uda3, uda) ? Constants.TRUE : Constants.FALSE);

			// always use right argument
			state.booleanMetatable = tableOf(Constants.EQ, RETURN_ONE);
			state.numberMetatable = tableOf(Constants.EQ, RETURN_ONE);
			state.stringMetatable = tableOf(Constants.EQ, RETURN_ONE);
			tbl.setMetatable(state, tableOf(Constants.EQ, RETURN_ONE));
			tbl2.setMetatable(state, tableOf(Constants.EQ, RETURN_ONE));
			uda.setMetatable(state, tableOf(Constants.EQ, RETURN_ONE));
			udb.setMetatable(state, uda.getMetatable(state));
			uda2.setMetatable(state, tableOf(Constants.EQ, RETURN_ONE));
			// diff metatag function
			tbl3.setMetatable(state, tableOf(Constants.EQ, RETURN_NIL));
			uda3.setMetatable(state, tableOf(Constants.EQ, RETURN_NIL));

			// primitive types or same value do not invoke metatag as per C implementation
			assertEquals(tru, eq.apply(tru, tru) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, eq.apply(one, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, eq.apply(abc, abc) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, eq.apply(tbl, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, eq.apply(uda, uda) ? Constants.TRUE : Constants.FALSE);
			assertEquals(tru, eq.apply(uda, udb) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(tru, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(fal, tru) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(zer, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(one, zer) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(pi, ee) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(ee, pi) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(pi, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(one, pi) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(abc, def) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(def, abc) ? Constants.TRUE : Constants.FALSE);
			// different types.  not comparable
			assertEquals(fal, eq.apply(fal, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(tbl, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(tbl, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(one, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(fal, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(one, fal) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(abc, one) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(one, abc) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(tbl, uda) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(uda, tbl) ? Constants.TRUE : Constants.FALSE);
			// same type, same value, does not invoke metatag op
			assertEquals(tru, eq.apply(tbl, tbl) ? Constants.TRUE : Constants.FALSE);
			// same type, different value, same metatag op.  comparabile via metatag op
			assertEquals(oneb, eq.apply(tbl, tbl2) ? Constants.TRUE : Constants.FALSE);
			assertEquals(oneb, eq.apply(tbl2, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(oneb, eq.apply(uda, uda2) ? Constants.TRUE : Constants.FALSE);
			assertEquals(oneb, eq.apply(uda2, uda) ? Constants.TRUE : Constants.FALSE);
			// same type, different metatag ops.  not comparable
			assertEquals(fal, eq.apply(tbl, tbl3) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(tbl3, tbl) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(uda, uda3) ? Constants.TRUE : Constants.FALSE);
			assertEquals(fal, eq.apply(uda3, uda) ? Constants.TRUE : Constants.FALSE);

		} finally {
			state.booleanMetatable = null;
			state.numberMetatable = null;
			state.stringMetatable = smt;
		}
	}

	@Test
	public void testAdd() throws LuaError, UnwindThrowable {
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
		assertDoubleEquals(155.0, add.apply(ia, ib).toDouble());
		assertDoubleEquals(58.75, add.apply(da, db).toDouble());
		assertDoubleEquals(29.375, add.apply(sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(166.25, add.apply(ia, da).toDouble());
		assertDoubleEquals(166.25, add.apply(da, ia).toDouble());
		assertDoubleEquals(133.125, add.apply(ia, sa).toDouble());
		assertDoubleEquals(133.125, add.apply(sa, ia).toDouble());
		assertDoubleEquals(77.375, add.apply(da, sa).toDouble());
		assertDoubleEquals(77.375, add.apply(sa, da).toDouble());
	}

	@Test
	public void testSub() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(111), ib = valueOf(44);
		LuaValue da = valueOf(55.25), db = valueOf(3.5);
		LuaValue sa = valueOf("22.125"), sb = valueOf("7.25");

		// like kinds
		assertDoubleEquals(67.0, sub.apply(ia, ib).toDouble());
		assertDoubleEquals(51.75, sub.apply(da, db).toDouble());
		assertDoubleEquals(14.875, sub.apply(sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(55.75, sub.apply(ia, da).toDouble());
		assertDoubleEquals(-55.75, sub.apply(da, ia).toDouble());
		assertDoubleEquals(88.875, sub.apply(ia, sa).toDouble());
		assertDoubleEquals(-88.875, sub.apply(sa, ia).toDouble());
		assertDoubleEquals(33.125, sub.apply(da, sa).toDouble());
		assertDoubleEquals(-33.125, sub.apply(sa, da).toDouble());
	}

	@Test
	public void testMul() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(12.0, mul.apply(ia, ib).toDouble());
		assertDoubleEquals(.125, mul.apply(da, db).toDouble());
		assertDoubleEquals(3.0, mul.apply(sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(.75, mul.apply(ia, da).toDouble());
		assertDoubleEquals(.75, mul.apply(da, ia).toDouble());
		assertDoubleEquals(4.5, mul.apply(ia, sa).toDouble());
		assertDoubleEquals(4.5, mul.apply(sa, ia).toDouble());
		assertDoubleEquals(.375, mul.apply(da, sa).toDouble());
		assertDoubleEquals(.375, mul.apply(sa, da).toDouble());
	}

	@Test
	public void testDiv() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(3. / 4., div.apply(ia, ib).toDouble());
		assertDoubleEquals(.25 / .5, div.apply(da, db).toDouble());
		assertDoubleEquals(1.5 / 2., div.apply(sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(3. / .25, div.apply(ia, da).toDouble());
		assertDoubleEquals(.25 / 3., div.apply(da, ia).toDouble());
		assertDoubleEquals(3. / 1.5, div.apply(ia, sa).toDouble());
		assertDoubleEquals(1.5 / 3., div.apply(sa, ia).toDouble());
		assertDoubleEquals(.25 / 1.5, div.apply(da, sa).toDouble());
		assertDoubleEquals(1.5 / .25, div.apply(sa, da).toDouble());
	}

	@Test
	public void testPow() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(4.), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertDoubleEquals(Math.pow(3., 4.), pow.apply(ia, ib).toDouble());
		assertDoubleEquals(Math.pow(4., .5), pow.apply(da, db).toDouble());
		assertDoubleEquals(Math.pow(1.5, 2.), pow.apply(sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(Math.pow(3., 4.), pow.apply(ia, da).toDouble());
		assertDoubleEquals(Math.pow(4., 3.), pow.apply(da, ia).toDouble());
		assertDoubleEquals(Math.pow(3., 1.5), pow.apply(ia, sa).toDouble());
		assertDoubleEquals(Math.pow(1.5, 3.), pow.apply(sa, ia).toDouble());
		assertDoubleEquals(Math.pow(4., 1.5), pow.apply(da, sa).toDouble());
		assertDoubleEquals(Math.pow(1.5, 4.), pow.apply(sa, da).toDouble());
	}

	private static double luaMod(double x, double y) {
		return y != 0 ? x - y * Math.floor(x / y) : Double.NaN;
	}

	@Test
	public void testMod() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(3), ib = valueOf(-4);
		LuaValue da = valueOf(.25), db = valueOf(-.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("-2.0");

		// like kinds
		assertDoubleEquals(luaMod(3., -4.), mod.apply(ia, ib).toDouble());
		assertDoubleEquals(luaMod(.25, -.5), mod.apply(da, db).toDouble());
		assertDoubleEquals(luaMod(1.5, -2.), mod.apply(sa, sb).toDouble());

		// unlike kinds
		assertDoubleEquals(luaMod(3., .25), mod.apply(ia, da).toDouble());
		assertDoubleEquals(luaMod(.25, 3.), mod.apply(da, ia).toDouble());
		assertDoubleEquals(luaMod(3., 1.5), mod.apply(ia, sa).toDouble());
		assertDoubleEquals(luaMod(1.5, 3.), mod.apply(sa, ia).toDouble());
		assertDoubleEquals(luaMod(.25, 1.5), mod.apply(da, sa).toDouble());
		assertDoubleEquals(luaMod(1.5, .25), mod.apply(sa, da).toDouble());
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
			OperationHelper.class.getMethod(op, new Class<?>[]{LuaState.class, LuaValue.class, LuaValue.class}).invoke(null, state, a, b);
		} catch (InvocationTargetException ite) {
			String actual = ite.getTargetException().getMessage();
			if ((!actual.startsWith("attempt to perform arithmetic")) || !actual.contains(type)) {
				fail(op + "op(" + a.typeName() + "," + b.typeName() + ") reported '" + actual + "'");
			}
		} catch (Exception e) {
			fail(op + "(" + a.typeName() + "," + b.typeName() + ") threw " + e);
		}
	}

	private static final LibFunction RETURN_LHS = LibFunction.create((state, lhs, rhs) -> lhs);

	private static final LibFunction RETURN_RHS = LibFunction.create((state, lhs, rhs) -> rhs);

	@Test
	public void testArithMetatag() throws LuaError, UnwindThrowable {
		LuaValue tru = Constants.TRUE;
		LuaValue fal = Constants.FALSE;
		LuaValue tbl = new LuaTable();
		LuaValue tbl2 = new LuaTable();
		LuaTable stringMt = state.stringMetatable;
		try {
			assertThrows(LuaError.class, () -> add.apply(tru, tbl));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));
			assertThrows(LuaError.class, () -> mul.apply(tru, tbl));
			assertThrows(LuaError.class, () -> div.apply(tru, tbl));
			assertThrows(LuaError.class, () -> pow.apply(tru, tbl));
			assertThrows(LuaError.class, () -> mod.apply(tru, tbl));

			// always use left argument
			state.booleanMetatable = tableOf(Constants.ADD, RETURN_LHS);
			assertEquals(tru, add.apply(tru, fal));
			assertEquals(tru, add.apply(tru, tbl));
			assertEquals(tbl, add.apply(tbl, tru));
			assertThrows(LuaError.class, () -> add.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));

			state.booleanMetatable = tableOf(Constants.SUB, RETURN_LHS);
			assertEquals(tru, sub.apply(tru, fal));
			assertEquals(tru, sub.apply(tru, tbl));
			assertEquals(tbl, sub.apply(tbl, tru));
			assertThrows(LuaError.class, () -> sub.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> add.apply(tru, tbl));

			state.booleanMetatable = tableOf(Constants.MUL, RETURN_LHS);
			assertEquals(tru, mul.apply(tru, fal));
			assertEquals(tru, mul.apply(tru, tbl));
			assertEquals(tbl, mul.apply(tbl, tru));
			assertThrows(LuaError.class, () -> mul.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));


			state.booleanMetatable = tableOf(Constants.DIV, RETURN_LHS);
			assertEquals(tru, div.apply(tru, fal));
			assertEquals(tru, div.apply(tru, tbl));
			assertEquals(tbl, div.apply(tbl, tru));
			assertThrows(LuaError.class, () -> div.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));

			state.booleanMetatable = tableOf(Constants.POW, RETURN_LHS);
			assertEquals(tru, pow.apply(tru, fal));
			assertEquals(tru, pow.apply(tru, tbl));
			assertEquals(tbl, pow.apply(tbl, tru));
			assertThrows(LuaError.class, () -> pow.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));

			state.booleanMetatable = tableOf(Constants.MOD, RETURN_LHS);
			assertEquals(tru, mod.apply(tru, fal));
			assertEquals(tru, mod.apply(tru, tbl));
			assertEquals(tbl, mod.apply(tbl, tru));
			assertThrows(LuaError.class, () -> mod.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));

			// always use right argument
			state.booleanMetatable = tableOf(Constants.ADD, RETURN_RHS);
			assertEquals(fal, add.apply(tru, fal));
			assertEquals(tbl, add.apply(tru, tbl));
			assertEquals(tru, add.apply(tbl, tru));
			assertThrows(LuaError.class, () -> add.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));

			state.booleanMetatable = tableOf(Constants.SUB, RETURN_RHS);
			assertEquals(fal, sub.apply(tru, fal));
			assertEquals(tbl, sub.apply(tru, tbl));
			assertEquals(tru, sub.apply(tbl, tru));
			assertThrows(LuaError.class, () -> sub.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> add.apply(tru, tbl));

			state.booleanMetatable = tableOf(Constants.MUL, RETURN_RHS);
			assertEquals(fal, mul.apply(tru, fal));
			assertEquals(tbl, mul.apply(tru, tbl));
			assertEquals(tru, mul.apply(tbl, tru));
			assertThrows(LuaError.class, () -> mul.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));

			state.booleanMetatable = tableOf(Constants.DIV, RETURN_RHS);
			assertEquals(fal, div.apply(tru, fal));
			assertEquals(tbl, div.apply(tru, tbl));
			assertEquals(tru, div.apply(tbl, tru));
			assertThrows(LuaError.class, () -> div.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));


			state.booleanMetatable = tableOf(Constants.POW, RETURN_RHS);
			assertEquals(fal, pow.apply(tru, fal));
			assertEquals(tbl, pow.apply(tru, tbl));
			assertEquals(tru, pow.apply(tbl, tru));
			assertThrows(LuaError.class, () -> pow.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));

			state.booleanMetatable = tableOf(Constants.MOD, RETURN_RHS);
			assertEquals(fal, mod.apply(tru, fal));
			assertEquals(tbl, mod.apply(tru, tbl));
			assertEquals(tru, mod.apply(tbl, tru));
			assertThrows(LuaError.class, () -> mod.apply(tbl, tbl2));
			assertThrows(LuaError.class, () -> sub.apply(tru, tbl));

			// Ensures string arithmetic work as expected
			var concat = LuaOperators.createBinOp(state, "..");
			state.stringMetatable = tableOf(Constants.ADD, LibFunction.createS((state, di, args) -> concat.apply(valueOf(args.arg(1).toString()), valueOf(args.arg(2).toString()))));

			assertEquals(valueOf("ab"), add.apply(valueOf("a"), valueOf("b")));
			assertEquals(valueOf("a2"), add.apply(valueOf("a"), valueOf("2")));
			assertEquals(valueOf("a2"), add.apply(valueOf("a"), valueOf(2)));
			assertEquals(valueOf("2b"), add.apply(valueOf("2"), valueOf("b")));
			assertEquals(valueOf("2b"), add.apply(valueOf(2), valueOf("b")));
			assertEquals(valueOf(4), add.apply(valueOf("2"), valueOf("2")));
			assertEquals(valueOf(4), add.apply(valueOf("2"), valueOf(2)));
			assertEquals(valueOf(4), add.apply(valueOf(2), valueOf("2")));
		} finally {
			state.booleanMetatable = null;
			state.stringMetatable = stringMt;
		}
	}

	@Test
	public void testArithMetatagNumberTable() throws LuaError, UnwindThrowable {
		LuaValue zero = Constants.ZERO;
		LuaValue one = Constants.ONE;
		LuaValue tbl = new LuaTable();

		assertThrows(LuaError.class, () -> add.apply(tbl, zero));
		assertThrows(LuaError.class, () -> add.apply(zero, tbl));

		tbl.setMetatable(state, tableOf(Constants.ADD, RETURN_ONE));
		assertEquals(one, add.apply(tbl, zero));
		assertEquals(one, add.apply(zero, tbl));

		assertThrows(LuaError.class, () -> sub.apply(tbl, zero));
		assertThrows(LuaError.class, () -> sub.apply(zero, tbl));

		tbl.setMetatable(state, tableOf(Constants.SUB, RETURN_ONE));
		assertEquals(one, sub.apply(tbl, zero));
		assertEquals(one, sub.apply(zero, tbl));

		assertThrows(LuaError.class, () -> mul.apply(tbl, zero));
		assertThrows(LuaError.class, () -> mul.apply(zero, tbl));

		tbl.setMetatable(state, tableOf(Constants.MUL, RETURN_ONE));
		assertEquals(one, mul.apply(tbl, zero));
		assertEquals(one, mul.apply(zero, tbl));

		assertThrows(LuaError.class, () -> div.apply(tbl, zero));
		assertThrows(LuaError.class, () -> div.apply(zero, tbl));

		tbl.setMetatable(state, tableOf(Constants.DIV, RETURN_ONE));
		assertEquals(one, div.apply(tbl, zero));
		assertEquals(one, div.apply(zero, tbl));

		assertThrows(LuaError.class, () -> pow.apply(tbl, zero));
		assertThrows(LuaError.class, () -> pow.apply(zero, tbl));

		tbl.setMetatable(state, tableOf(Constants.POW, RETURN_ONE));
		assertEquals(one, pow.apply(tbl, zero));
		assertEquals(one, pow.apply(zero, tbl));

		assertThrows(LuaError.class, () -> mod.apply(tbl, zero));
		assertThrows(LuaError.class, () -> mod.apply(zero, tbl));

		tbl.setMetatable(state, tableOf(Constants.MOD, RETURN_ONE));
		assertEquals(one, mod.apply(tbl, zero));
		assertEquals(one, mod.apply(zero, tbl));
	}

	@Test
	public void testCompareStrings() throws LuaError, UnwindThrowable {
		// these are lexical compare!
		LuaValue sa = valueOf("-1.5");
		LuaValue sb = valueOf("-2.0");
		LuaValue sc = valueOf("1.5");
		LuaValue sd = valueOf("2.0");

		assertFalse(lt.apply(sa, sa));
		assertTrue(lt.apply(sa, sb));
		assertTrue(lt.apply(sa, sc));
		assertTrue(lt.apply(sa, sd));
		assertFalse(lt.apply(sb, sa));
		assertFalse(lt.apply(sb, sb));
		assertTrue(lt.apply(sb, sc));
		assertTrue(lt.apply(sb, sd));
		assertFalse(lt.apply(sc, sa));
		assertFalse(lt.apply(sc, sb));
		assertFalse(lt.apply(sc, sc));
		assertTrue(lt.apply(sc, sd));
		assertFalse(lt.apply(sd, sa));
		assertFalse(lt.apply(sd, sb));
		assertFalse(lt.apply(sd, sc));
		assertFalse(lt.apply(sd, sd));
	}

	@Test
	public void testLt() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. < 4., lt.apply(ia, ib));
		assertEquals(.25 < .5, lt.apply(da, db));

		// unlike kinds
		assertEquals(3. < .25, lt.apply(ia, da));
		assertEquals(.25 < 3., lt.apply(da, ia));
	}

	@Test
	public void testLtEq() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. <= 4., le.apply(ia, ib));
		assertEquals(.25 <= .5, le.apply(da, db));

		// unlike kinds
		assertEquals(3. <= .25, le.apply(ia, da));
		assertEquals(.25 <= 3., le.apply(da, ia));
	}

	@Test
	public void testGt() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. > 4., lt.apply(ib, ia));
		assertEquals(.25 > .5, lt.apply(db, da));

		// unlike kinds
		assertEquals(3. > .25, lt.apply(da, ia));
		assertEquals(.25 > 3., lt.apply(ia, da));
	}

	@Test
	public void testGtEq() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);

		// like kinds
		assertEquals(3. >= 4., le.apply(ib, ia));
		assertEquals(.25 >= .5, le.apply(db, da));

		// unlike kinds
		assertEquals(3. >= .25, le.apply(da, ia));
		assertEquals(.25 >= 3., le.apply(ia, da));
	}

	@Test
	public void testNotEq() throws LuaError, UnwindThrowable {
		LuaValue ia = valueOf(3), ib = valueOf(4);
		LuaValue da = valueOf(.25), db = valueOf(.5);
		LuaValue sa = valueOf("1.5"), sb = valueOf("2.0");

		// like kinds
		assertEquals(3. != 4., (eq.apply(ia, ib) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(.25 != .5, (eq.apply(da, db) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(1.5 != 2., (eq.apply(sa, sb) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(3. != 4., !eq.apply(ia, ib));
		assertEquals(.25 != .5, !eq.apply(da, db));
		assertEquals(1.5 != 2., !eq.apply(sa, sb));

		// unlike kinds
		assertEquals(3. != .25, (eq.apply(ia, da) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(.25 != 3., (eq.apply(da, ia) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(3. != 1.5, (eq.apply(ia, sa) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(1.5 != 3., (eq.apply(sa, ia) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(.25 != 1.5, (eq.apply(da, sa) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(1.5 != .25, (eq.apply(sa, da) ? Constants.FALSE : Constants.TRUE).toBoolean());
		assertEquals(3. != .25, !eq.apply(ia, da));
		assertEquals(.25 != 3., !eq.apply(da, ia));
		assertEquals(3. != 1.5, !eq.apply(ia, sa));
		assertEquals(1.5 != 3., !eq.apply(sa, ia));
		assertEquals(.25 != 1.5, !eq.apply(da, sa));
		assertEquals(1.5 != .25, !eq.apply(sa, da));
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
			OperationHelper.class.getMethod(op, new Class<?>[]{LuaState.class, LuaValue.class, LuaValue.class}).invoke(null, state, a, b);
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
	public void testCompareMetatag() throws LuaError, UnwindThrowable {
		LuaValue tbl = new LuaTable();
		LuaValue tbl2 = new LuaTable();
		LuaValue tbl3 = new LuaTable();
		try {
			// always use left argument
			LuaTable mt = tableOf(
				Constants.LT, RETURN_LHS,
				Constants.LE, RETURN_RHS
			);
			state.booleanMetatable = mt;
			tbl.setMetatable(state, mt);
			tbl2.setMetatable(state, mt);
			assertTrue(lt.apply(Constants.TRUE, Constants.FALSE));
			assertFalse(lt.apply(Constants.FALSE, Constants.TRUE));
			assertThrows(LuaError.class, () -> lt.apply(tbl, tbl3));
			assertThrows(LuaError.class, () -> lt.apply(tbl3, tbl));

			assertFalse(le.apply(Constants.TRUE, Constants.FALSE));
			assertTrue(le.apply(Constants.FALSE, Constants.TRUE));
			assertThrows(LuaError.class, () -> le.apply(tbl, tbl3));
			assertThrows(LuaError.class, () -> le.apply(tbl3, tbl));


			// always use right argument
			mt = tableOf(
				Constants.LT, RETURN_RHS,
				Constants.LE, RETURN_LHS
			);
			state.booleanMetatable = mt;
			tbl.setMetatable(state, mt);
			tbl2.setMetatable(state, mt);
			assertFalse(lt.apply(Constants.TRUE, Constants.FALSE));
			assertTrue(lt.apply(Constants.FALSE, Constants.TRUE));
			assertThrows(LuaError.class, () -> lt.apply(tbl, tbl3));
			assertThrows(LuaError.class, () -> lt.apply(tbl3, tbl));

			assertTrue(le.apply(Constants.TRUE, Constants.FALSE));
			assertFalse(le.apply(Constants.FALSE, Constants.TRUE));
			assertThrows(LuaError.class, () -> le.apply(tbl, tbl3));
			assertThrows(LuaError.class, () -> le.apply(tbl3, tbl));


		} finally {
			state.booleanMetatable = null;
		}
	}

	@Test
	public void testLexicalComparison() throws LuaError, UnwindThrowable {
		LuaValue aaa = valueOf("aaa");
		LuaValue baa = valueOf("baa");
		LuaValue Aaa = valueOf("Aaa");
		LuaValue aba = valueOf("aba");
		LuaValue aaaa = valueOf("aaaa");

		// basics
		assertTrue(eq.apply(aaa, aaa));
		assertTrue(lt.apply(aaa, baa));
		assertTrue(le.apply(aaa, baa));
		assertFalse(lt.apply(baa, aaa));
		assertFalse(le.apply(baa, aaa));
		assertFalse(lt.apply(baa, aaa));
		assertFalse(le.apply(baa, aaa));
		assertTrue(lt.apply(aaa, baa));
		assertTrue(le.apply(aaa, baa));
		assertTrue(le.apply(aaa, aaa));
		assertTrue(le.apply(aaa, aaa));

		// different case
		assertTrue(eq.apply(Aaa, Aaa));
		assertTrue(lt.apply(Aaa, aaa));
		assertTrue(le.apply(Aaa, aaa));
		assertFalse(lt.apply(aaa, Aaa));
		assertFalse(le.apply(aaa, Aaa));
		assertFalse(lt.apply(aaa, Aaa));
		assertFalse(le.apply(aaa, Aaa));
		assertTrue(lt.apply(Aaa, aaa));
		assertTrue(le.apply(Aaa, aaa));
		assertTrue(le.apply(Aaa, Aaa));
		assertTrue(le.apply(Aaa, Aaa));

		// second letter differs
		assertTrue(eq.apply(aaa, aaa));
		assertTrue(lt.apply(aaa, aba));
		assertTrue(le.apply(aaa, aba));
		assertFalse(lt.apply(aba, aaa));
		assertFalse(le.apply(aba, aaa));
		assertFalse(lt.apply(aba, aaa));
		assertFalse(le.apply(aba, aaa));
		assertTrue(lt.apply(aaa, aba));
		assertTrue(le.apply(aaa, aba));
		assertTrue(le.apply(aaa, aaa));
		assertTrue(le.apply(aaa, aaa));

		// longer
		assertTrue(eq.apply(aaa, aaa));
		assertTrue(lt.apply(aaa, aaaa));
		assertTrue(le.apply(aaa, aaaa));
		assertFalse(lt.apply(aaaa, aaa));
		assertFalse(le.apply(aaaa, aaa));
		assertFalse(lt.apply(aaaa, aaa));
		assertFalse(le.apply(aaaa, aaa));
		assertTrue(lt.apply(aaa, aaaa));
		assertTrue(le.apply(aaa, aaaa));
		assertTrue(le.apply(aaa, aaa));
		assertTrue(le.apply(aaa, aaa));
	}

	@Test
	public void testBuffer() {
		LuaString abc = valueOf("abcdefghi").substringOfEnd(0, 3);
		LuaString def = valueOf("abcdefghi").substringOfEnd(3, 6);
		LuaString ghi = valueOf("abcdefghi").substringOfEnd(6, 9);
		LuaString n123 = valueOf(123).checkLuaString();

		// basic append
		Buffer b = new Buffer();
		assertEquals("", ((LuaValue) b.toLuaString()).toString());
		b.append(def);
		assertEquals("def", ((LuaValue) b.toLuaString()).toString());
		b.append(abc);
		assertEquals("defabc", ((LuaValue) b.toLuaString()).toString());
		b.append(ghi);
		assertEquals("defabcghi", ((LuaValue) b.toLuaString()).toString());
		b.append(n123);
		assertEquals("defabcghi123", ((LuaValue) b.toLuaString()).toString());
	}

	@Test
	public void testConcat() throws LuaError, UnwindThrowable {
		LuaValue abc = valueOf("abcdefghi").substringOfEnd(0, 3);
		LuaValue def = valueOf("abcdefghi").substringOfEnd(3, 6);
		LuaValue ghi = valueOf("abcdefghi").substringOfEnd(6, 9);
		LuaValue n123 = valueOf(123);

		assertEquals("abc", abc.toString());
		assertEquals("def", def.toString());
		assertEquals("ghi", ghi.toString());
		assertEquals("123", n123.toString());
		assertEquals("abcabc", concat.apply(abc, abc).toString());
		assertEquals("defghi", concat.apply(def, ghi).toString());
		assertEquals("ghidef", concat.apply(ghi, def).toString());
		assertEquals("ghidefabcghi", concat.apply(concat.apply(concat.apply(ghi, def), abc), ghi).toString());
		assertEquals("123def", concat.apply(n123, def).toString());
		assertEquals("def123", concat.apply(def, n123).toString());
	}

	@Test
	public void testConcatMetatag() throws LuaError, UnwindThrowable {
		LuaValue def = valueOf("abcdefghi").substringOfEnd(3, 6);
		LuaValue ghi = valueOf("abcdefghi").substringOfEnd(6, 9);
		LuaValue tru = Constants.TRUE;
		LuaValue fal = Constants.FALSE;
		LuaValue tbl = new LuaTable();
		LuaValue uda = new LuaUserdata(new Object());
		try {
			// always use left argument
			state.booleanMetatable = tableOf(Constants.CONCAT, RETURN_LHS);
			assertEquals(tru, concat.apply(tru, tbl));
			assertEquals(tbl, concat.apply(tbl, tru));
			assertEquals(tru, concat.apply(tru, tbl));
			assertEquals(tbl, concat.apply(tbl, tru));

			assertThrows(LuaError.class, () -> concat.apply(tbl, def));
			assertThrows(LuaError.class, () -> concat.apply(def, tbl));
			assertThrows(LuaError.class, () -> concat.apply(uda, concat.apply(def, tbl)));
			assertThrows(LuaError.class, () -> concat.apply(ghi, concat.apply(tbl, def)));

			// always use right argument
			state.booleanMetatable = tableOf(Constants.CONCAT, RETURN_RHS);
			assertEquals(tbl, concat.apply(tru, tbl));
			assertEquals(tru, concat.apply(tbl, tru));
			assertEquals(tbl, concat.apply(tru, tbl));
			assertEquals(tru, concat.apply(tbl, tru));
			assertEquals(tru, concat.apply(uda, concat.apply(tbl, tru)));
			assertEquals(tbl, concat.apply(fal, concat.apply(tru, tbl)));

			assertThrows(LuaError.class, () -> concat.apply(tbl, def));
			assertThrows(LuaError.class, () -> concat.apply(def, tbl));
			assertThrows(LuaError.class, () -> concat.apply(uda, concat.apply(def, tbl)));
		} finally {
			state.booleanMetatable = null;
		}
	}

	@Test
	public void testConcatErrors() {
		LuaValue[] vals = {Constants.NIL, Constants.TRUE, tableOf()};
		LuaValue[] numerics = {valueOf(111), valueOf(55.25), valueOf("22.125")};
		for (LuaValue val : vals) {
			for (LuaValue numeric : numerics) {
				checkConcatError(val, numeric, val.typeName());
				checkConcatError(numeric, val, val.typeName());
			}
		}
	}

	private void checkConcatError(LuaValue a, LuaValue b, String type) {
		var e = assertThrows(LuaError.class, () -> concat.apply(a, b));
		String actual = e.getMessage();
		if ((!actual.startsWith("attempt to concatenate")) || !actual.contains(type)) {
			fail("concat(" + a.typeName() + "," + b.typeName() + ") reported '" + actual + "'");
		}
	}


	public static void assertDoubleEquals(double a, double b) {
		assertEquals(a, b, 1e-10);
	}
}
