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
package org.squiddev.cobalt;

import org.squiddev.cobalt.compiler.LuaC;

import java.util.Arrays;

import static org.squiddev.cobalt.ValueFactory.valueOf;

/**
 * List of constants
 */
public final class Constants {
	/**
	 * Type enumeration constant for lua numbers that are ints, for compatibility with lua 5.1 number patch only
	 */
	public static final int TINT = (-2);
	/**
	 * Type enumeration constant for lua nil
	 */
	public static final int TNIL = 0;
	/**
	 * Type enumeration constant for lua booleans
	 */
	public static final int TBOOLEAN = 1;
	/**
	 * Type enumeration constant for lua light userdata, for compatibility with C-based lua only
	 */
	public static final int TLIGHTUSERDATA = 2;
	/**
	 * Type enumeration constant for lua numbers
	 */
	public static final int TNUMBER = 3;
	/**
	 * Type enumeration constant for lua strings
	 */
	public static final int TSTRING = 4;
	/**
	 * Type enumeration constant for lua tables
	 */
	public static final int TTABLE = 5;
	/**
	 * Type enumeration constant for lua functions
	 */
	public static final int TFUNCTION = 6;
	/**
	 * Type enumeration constant for lua userdatas
	 */
	public static final int TUSERDATA = 7;
	/**
	 * Type enumeration constant for lua threads
	 */
	public static final int TTHREAD = 8;
	/**
	 * Type enumeration constant for unknown values, for compatibility with C-based lua only
	 */
	public static final int TVALUE = 9;
	/**
	 * String array constant containing names of each of the lua value types
	 *
	 * @see LuaValue#type()
	 * @see LuaValue#typeName()
	 */
	static final String[] TYPE_NAMES = {
		"nil",
		"boolean",
		"lightuserdata",
		"number",
		"string",
		"table",
		"function",
		"userdata",
		"thread",
		"value",
	};

	static final LuaString[] TYPE_NAMES_LUA = Arrays.stream(TYPE_NAMES).map(LuaString::valueOf).toArray(LuaString[]::new);

	/**
	 * LuaValue constant corresponding to lua {@code nil}
	 */
	public static final LuaValue NIL = LuaNil.INSTANCE;

	/**
	 * LuaBoolean constant corresponding to lua {@code true}
	 */
	public static final LuaBoolean TRUE = LuaBoolean._TRUE;

	/**
	 * LuaBoolean constant corresponding to lua {@code false}
	 */
	public static final LuaBoolean FALSE = LuaBoolean._FALSE;

	/**
	 * LuaValue constant corresponding to a {@link Varargs} list of no values
	 */
	public static final Varargs NONE = new None();

	/**
	 * LuaValue number constant equal to 0
	 */
	public static final LuaNumber ZERO = LuaInteger.valueOf(0);

	/**
	 * LuaValue number constant equal to 1
	 */
	public static final LuaNumber ONE = LuaInteger.valueOf(1);

	/**
	 * LuaValue number constant equal to -1
	 */
	public static final LuaNumber MINUSONE = LuaInteger.valueOf(-1);

	/**
	 * LuaString constant with value "__name" for use as metatag
	 */
	public static final LuaString NAME = valueOf("__name");

	/**
	 * LuaString constant with value "__index" for use as metatag
	 */
	public static final LuaString INDEX = valueOf("__index");

	/**
	 * LuaString constant with value "__newindex" for use as metatag
	 */
	public static final LuaString NEWINDEX = valueOf("__newindex");

	/**
	 * LuaString constant with value "__call" for use as metatag
	 */
	public static final LuaString CALL = valueOf("__call");

	/**
	 * LuaString constant with value "__mode" for use as metatag
	 */
	public static final LuaString MODE = valueOf("__mode");

	/**
	 * LuaString constant with value "__metatable" for use as metatag
	 */
	public static final LuaString METATABLE = valueOf("__metatable");

	/**
	 * LuaString constant with value "__add" for use as metatag
	 */
	public static final LuaString ADD = valueOf("__add");

	/**
	 * LuaString constant with value "__sub" for use as metatag
	 */
	public static final LuaString SUB = valueOf("__sub");

	/**
	 * LuaString constant with value "__div" for use as metatag
	 */
	public static final LuaString DIV = valueOf("__div");

	/**
	 * LuaString constant with value "__mul" for use as metatag
	 */
	public static final LuaString MUL = valueOf("__mul");

	/**
	 * LuaString constant with value "__pow" for use as metatag
	 */
	public static final LuaString POW = valueOf("__pow");

	/**
	 * LuaString constant with value "__mod" for use as metatag
	 */
	public static final LuaString MOD = valueOf("__mod");

	/**
	 * LuaString constant with value "__unm" for use as metatag
	 */
	public static final LuaString UNM = valueOf("__unm");

	/**
	 * LuaString constant with value "__len" for use as metatag
	 */
	public static final LuaString LEN = valueOf("__len");

	/**
	 * LuaString constant with value "__eq" for use as metatag
	 */
	public static final LuaString EQ = valueOf("__eq");

	/**
	 * LuaString constant with value "__lt" for use as metatag
	 */
	public static final LuaString LT = valueOf("__lt");

	/**
	 * LuaString constant with value "__le" for use as metatag
	 */
	public static final LuaString LE = valueOf("__le");

	/**
	 * LuaString constant with value "__tostring" for use as metatag
	 */
	public static final LuaString TOSTRING = valueOf("__tostring");

	/**
	 * LuaString constant with value "__concat" for use as metatag
	 */
	public static final LuaString CONCAT = valueOf("__concat");

	/**
	 * LuaString constant with value "__pairs" for use as metatag
	 */
	public static final LuaString PAIRS = valueOf("__pairs");

	/**
	 * LuaString constant with value ""
	 */
	public static final LuaString EMPTYSTRING = valueOf("");

	/**
	 * The global loaded package table.
	 */
	public static final LuaString LOADED = valueOf("_LOADED");

	/**
	 * LuaString constant with value "_ENV" for use as metatag
	 */
	public static final LuaString ENV = valueOf("_ENV");

	/**
	 * Constant limiting metatag loop processing
	 */
	public static final int MAXTAGLOOP = 100;

	/**
	 * Array of {@link #NIL} values to optimize filling stacks using System.arraycopy().
	 * Must not be modified.
	 */
	public static final LuaValue[] NILS = new LuaValue[LuaC.MAXSTACK];

	static {
		Arrays.fill(NILS, NIL);
	}

	private Constants() {
	}

	/**
	 * Varargs implemenation with no values.
	 * <p>
	 * This is an internal class not intended to be used directly.
	 * Instead use the predefined constant {@link Constants#NONE}
	 *
	 * @see Constants#NONE
	 */
	private static final class None extends Varargs {
		@Override
		public LuaValue arg(int i) {
			return NIL;
		}

		@Override
		public int count() {
			return 0;
		}

		@Override
		public LuaValue first() {
			return NIL;
		}

		@Override
		public String toString() {
			return "none";
		}

		@Override
		public void fill(LuaValue[] array, int offset) {
		}
	}
}
