/**
 * ****************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
package org.luaj.vm2;

import org.luaj.vm2.lib.jse.JsePlatform;

import static org.luaj.vm2.Constants.*;
import static org.luaj.vm2.Factory.valueOf;
import static org.luaj.vm2.Factory.varargsOf;

/**
 * Base class for all concrete lua type values.
 * <p>
 * Establishes base implementations for all the operations on lua types.
 * This allows Java clients to deal essentially with one type for all Java values, namely {@link LuaValue}.
 * <p>
 * Constructors are provided as static methods for common Java types, such as
 * {@link Factory#valueOf(int)} or {@link Factory#valueOf(String)}
 * to allow for instance pooling.
 * <p>
 * Constants are defined for the lua values
 * {@link Constants#NIL}, {@link Constants#TRUE}, and {@link Constants#FALSE}.
 * A constant {@link Constants#NONE} is defined which is a {@link Varargs} list having no values.
 * <p>
 * Operations are performed on values directly via their Java methods.
 * For example, the following code divides two numbers:
 * <pre> {@code
 * LuaValue a = LuaValue.valueOf( 5 );
 * LuaValue b = LuaValue.valueOf( 4 );
 * LuaValue c = a.div(b);
 * } </pre>
 * Note that in this example, c will be a {@link LuaDouble}, but would be a {@link LuaInteger}
 * if the value of a were changed to 8, say.
 * In general the value of c in practice will vary depending on both the types and values of a and b
 * as well as any metatable/metatag processing that occurs.
 * <p>
 * Field access and function calls are similar, with common overloads to simplify Java usage:
 * <pre> {@code
 * LuaValue globals = JsePlatform.standardGlobals();
 * LuaValue sqrt = globals.get("math").get("sqrt");
 * LuaValue print = globals.get("print");
 * LuaValue d = sqrt.call( a );
 * print.call( LuaValue.valueOf("sqrt(5):"), a );
 * } </pre>
 * <p>
 * To supply variable arguments or get multiple return values, use
 * {@link #invoke(Varargs)} or {@link #invokemethod(LuaValue, Varargs)} methods:
 * <pre> {@code
 * LuaValue modf = globals.get("math").get("modf");
 * Varargs r = modf.invoke( d );
 * print.call( r.arg(1), r.arg(2) );
 * } </pre>
 * <p>
 * To load and run a script, {@link LoadState} is used:
 * <pre> {@code
 * LoadState.load(new FileInputStream("main.lua"), "main.lua", globals ).call();
 * } </pre>
 * <p>
 * although {@code require} could also be used:
 * <pre> {@code
 * globals.get("require").call(LuaValue.valueOf("main"));
 * } </pre>
 * For this to work the file must be in the current directory, or in the class path,
 * depending on the platform.
 * See {@link JsePlatform} for details.
 * <p>
 * In general a {@link LuaError} may be thrown on any operation when the
 * types supplied to any operation are illegal from a lua perspective.
 * Examples could be attempting to concatenate a NIL value, or attempting arithmetic
 * on values that are not number.
 * <p>
 * There are several methods for pre-initializing tables, such as:
 * <ul>
 * <li>{@link Factory#listOf(LuaValue[])} for unnamed elements</li>
 * <li>{@link Factory#tableOf(LuaValue[])} for named elements</li>
 * <li>{@link Factory#tableOf(LuaValue[], LuaValue[], Varargs)} for mixtures</li>
 * </ul>
 * <p>
 * Predefined constants exist for the standard lua type constants
 * {@link Constants#TNIL}, {@link Constants#TBOOLEAN}, {@link Constants#TLIGHTUSERDATA}, {@link Constants#TNUMBER}, {@link Constants#TSTRING},
 * {@link Constants#TTABLE}, {@link Constants#TFUNCTION}, {@link Constants#TUSERDATA}, {@link Constants#TTHREAD},
 * and extended lua type constants
 * {@link Constants#TINT}, {@link Constants#TNONE}, {@link Constants#TVALUE}
 * <p>
 * Predefined constants exist for all strings used as metatags:
 * {@link Constants#INDEX}, {@link Constants#NEWINDEX}, {@link Constants#CALL}, {@link Constants#MODE}, {@link Constants#METATABLE},
 * {@link Constants#ADD}, {@link Constants#SUB}, {@link Constants#DIV}, {@link Constants#MUL}, {@link Constants#POW},
 * {@link Constants#MOD}, {@link Constants#UNM}, {@link Constants#LEN}, {@link Constants#EQ}, {@link Constants#LT},
 * {@link Constants#LE}, {@link Constants#TOSTRING}, and {@link Constants#CONCAT}.
 *
 * @see JsePlatform
 * @see LoadState
 * @see Varargs
 */
public abstract class LuaValue extends Varargs {
	// type

	/**
	 * Get the enumeration value for the type of this value.
	 *
	 * @return value for this type, one of
	 * {@link Constants#TNIL},
	 * {@link Constants#TBOOLEAN},
	 * {@link Constants#TNUMBER},
	 * {@link Constants#TSTRING},
	 * {@link Constants#TTABLE},
	 * {@link Constants#TFUNCTION},
	 * {@link Constants#TUSERDATA},
	 * {@link Constants#TTHREAD}
	 * @see #typename()
	 */
	public abstract int type();

	/**
	 * Get the String name of the type of this value.
	 * <p>
	 *
	 * @return name from type name list {@link Constants#TYPE_NAMES}
	 * corresponding to the type of this value:
	 * "nil", "boolean", "number", "string",
	 * "table", "function", "userdata", "thread"
	 * @see #type()
	 */
	public abstract String typename();

	/**
	 * Check if {@code this} is a {@code boolean}
	 *
	 * @return true if this is a {@code boolean}, otherwise false
	 * @see #isboolean()
	 * @see #toboolean()
	 * @see #checkboolean()
	 * @see #optboolean(boolean)
	 * @see Constants#TBOOLEAN
	 */
	public boolean isboolean() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@code function} that is a closure,
	 * meaning interprets lua bytecode for its execution
	 *
	 * @return true if this is a {@code closure}, otherwise false
	 * @see #isfunction()
	 * @see #checkclosure()
	 * @see #optclosure(LuaClosure)
	 * @see Constants#TFUNCTION
	 */
	public boolean isclosure() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@code function}
	 *
	 * @return true if this is a {@code function}, otherwise false
	 * @see #isclosure()
	 * @see #checkfunction()
	 * @see #optfunction(LuaFunction)
	 * @see Constants#TFUNCTION
	 */
	public boolean isfunction() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@code number} and is representable by java int
	 * without rounding or truncation
	 *
	 * @return true if this is a {@code number}
	 * meaning derives from {@link LuaNumber}
	 * or derives from {@link LuaString} and is convertible to a number,
	 * and can be represented by int,
	 * otherwise false
	 * @see #isinttype()
	 * @see #islong()
	 * @see #tonumber()
	 * @see #checkint()
	 * @see #optint(int)
	 * @see Constants#TNUMBER
	 */
	public boolean isint() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@link LuaInteger}
	 * <p>
	 * No attempt to convert from string will be made by this call.
	 *
	 * @return true if this is a {@code LuaInteger},
	 * otherwise false
	 * @see #isint()
	 * @see #isnumber()
	 * @see #tonumber()
	 * @see Constants#TNUMBER
	 */
	public boolean isinttype() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@code number} and is representable by java long
	 * without rounding or truncation
	 *
	 * @return true if this is a {@code number}
	 * meaning derives from {@link LuaNumber}
	 * or derives from {@link LuaString} and is convertible to a number,
	 * and can be represented by long,
	 * otherwise false
	 * @see #tonumber()
	 * @see #checklong()
	 * @see #optlong(long)
	 * @see Constants#TNUMBER
	 */
	public boolean islong() {
		return false;
	}

	/**
	 * Check if {@code this} is {@code nil}
	 *
	 * @return true if this is {@code nil}, otherwise false
	 * @see Constants#NIL
	 * @see Constants#NONE
	 * @see #checknotnil()
	 * @see #optvalue(LuaValue)
	 * @see Varargs#isnoneornil(int)
	 * @see Constants#TNIL
	 * @see Constants#TNONE
	 */
	public boolean isnil() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@code number}
	 *
	 * @return true if this is a {@code number},
	 * meaning derives from {@link LuaNumber}
	 * or derives from {@link LuaString} and is convertible to a number,
	 * otherwise false
	 * @see #tonumber()
	 * @see #checknumber()
	 * @see #optnumber(LuaNumber)
	 * @see Constants#TNUMBER
	 */
	public boolean isnumber() {
		return false;
	} // may convert from string

	/**
	 * Check if {@code this} is a {@code string}
	 *
	 * @return true if this is a {@code string},
	 * meaning derives from {@link LuaString} or {@link LuaNumber},
	 * otherwise false
	 * @see #tostring()
	 * @see #checkstring()
	 * @see #optstring(LuaString)
	 * @see Constants#TSTRING
	 */
	public boolean isstring() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@code thread}
	 *
	 * @return true if this is a {@code thread}, otherwise false
	 * @see #checkthread()
	 * @see #optthread(LuaThread)
	 * @see Constants#TTHREAD
	 */
	public boolean isthread() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@code table}
	 *
	 * @return true if this is a {@code table}, otherwise false
	 * @see #checktable()
	 * @see #opttable(LuaTable)
	 * @see Constants#TTABLE
	 */
	public boolean istable() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@code userdata}
	 *
	 * @return true if this is a {@code userdata}, otherwise false
	 * @see #isuserdata(Class)
	 * @see #touserdata()
	 * @see #checkuserdata()
	 * @see #optuserdata(Object)
	 * @see Constants#TUSERDATA
	 */
	public boolean isuserdata() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@code userdata} of type {@code c}
	 *
	 * @param c Class to test instance against
	 * @return true if this is a {@code userdata}
	 * and the instance is assignable to {@code c},
	 * otherwise false
	 * @see #isuserdata()
	 * @see #touserdata(Class)
	 * @see #checkuserdata(Class)
	 * @see #optuserdata(Class, Object)
	 * @see Constants#TUSERDATA
	 */
	public boolean isuserdata(Class<?> c) {
		return false;
	}

	/**
	 * Convert to boolean false if {@link Constants#NIL} or {@link Constants#FALSE}, true if anything else
	 *
	 * @return Value cast to byte if number or string convertible to number, otherwise 0
	 * @see #optboolean(boolean)
	 * @see #checkboolean()
	 * @see #isboolean()
	 * @see Constants#TBOOLEAN
	 */
	public boolean toboolean() {
		return true;
	}

	/**
	 * Convert to byte if numeric, or 0 if not.
	 *
	 * @return Value cast to byte if number or string convertible to number, otherwise 0
	 * @see #toint()
	 * @see #todouble()
	 * @see #checknumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public byte tobyte() {
		return 0;
	}

	/**
	 * Convert to char if numeric, or 0 if not.
	 *
	 * @return Value cast to char if number or string convertible to number, otherwise 0
	 * @see #toint()
	 * @see #todouble()
	 * @see #checknumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public char tochar() {
		return 0;
	}

	/**
	 * Convert to double if numeric, or 0 if not.
	 *
	 * @return Value cast to double if number or string convertible to number, otherwise 0
	 * @see #toint()
	 * @see #tobyte()
	 * @see #tochar()
	 * @see #toshort()
	 * @see #tolong()
	 * @see #tofloat()
	 * @see #optdouble(double)
	 * @see #checknumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public double todouble() {
		return 0;
	}

	/**
	 * Convert to float if numeric, or 0 if not.
	 *
	 * @return Value cast to float if number or string convertible to number, otherwise 0
	 * @see #toint()
	 * @see #todouble()
	 * @see #checknumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public float tofloat() {
		return 0;
	}

	/**
	 * Convert to int if numeric, or 0 if not.
	 *
	 * @return Value cast to int if number or string convertible to number, otherwise 0
	 * @see #tobyte()
	 * @see #tochar()
	 * @see #toshort()
	 * @see #tolong()
	 * @see #tofloat()
	 * @see #todouble()
	 * @see #optint(int)
	 * @see #checknumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public int toint() {
		return 0;
	}

	/**
	 * Convert to long if numeric, or 0 if not.
	 *
	 * @return Value cast to long if number or string convertible to number, otherwise 0
	 * @see #isint()
	 * @see #isinttype()
	 * @see #toint()
	 * @see #todouble()
	 * @see #optlong(long)
	 * @see #checknumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public long tolong() {
		return 0;
	}

	/**
	 * Convert to short if numeric, or 0 if not.
	 *
	 * @return Value cast to short if number or string convertible to number, otherwise 0
	 * @see #toint()
	 * @see #todouble()
	 * @see #checknumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public short toshort() {
		return 0;
	}

	/**
	 * Convert to human readable String for any type.
	 *
	 * @return String for use by human readers based on type.
	 * @see #tostring()
	 * @see #optjstring(String)
	 * @see #checkjstring()
	 * @see #isstring()
	 * @see Constants#TSTRING
	 */
	@Override
	public String tojstring() {
		return typename() + ": " + Integer.toHexString(hashCode());
	}

	/**
	 * Convert to userdata instance, or null.
	 *
	 * @return userdata instance if userdata, or null if not {@link LuaUserdata}
	 * @see #optuserdata(Object)
	 * @see #checkuserdata()
	 * @see #isuserdata()
	 * @see Constants#TUSERDATA
	 */
	public Object touserdata() {
		return null;
	}

	/**
	 * Convert to userdata instance if specific type, or null.
	 *
	 * @param c Use this class to create userdata
	 * @return userdata instance if is a userdata whose instance derives from {@code c},
	 * or null if not {@link LuaUserdata}
	 * @see #optuserdata(Class, Object)
	 * @see #checkuserdata(Class)
	 * @see #isuserdata(Class)
	 * @see Constants#TUSERDATA
	 */
	public Object touserdata(Class<?> c) {
		return null;
	}

	/**
	 * Convert the value to a human readable string using {@link #tojstring()}
	 *
	 * @return String value intended to be human readible.
	 * @see #tostring()
	 * @see #tojstring()
	 * @see #optstring(LuaString)
	 * @see #checkstring()
	 * @see #toString()
	 */
	public String toString() {
		return tojstring();
	}

	/**
	 * Conditionally convert to lua number without throwing errors.
	 * <p>
	 * In lua all numbers are strings, but not all strings are numbers.
	 * This function will return
	 * the {@link LuaValue} {@code this} if it is a number
	 * or a string convertible to a number,
	 * and {@link Constants#NIL} for all other cases.
	 * <p>
	 * This allows values to be tested for their "numeric-ness" without
	 * the penalty of throwing exceptions,
	 * nor the cost of converting the type and creating storage for it.
	 *
	 * @return {@code this} if it is a {@link LuaNumber}
	 * or {@link LuaString} that can be converted to a number,
	 * otherwise {@link Constants#NIL}
	 * @see #tostring()
	 * @see #optnumber(LuaNumber)
	 * @see #checknumber()
	 * @see #toint()
	 * @see #todouble()
	 */
	public LuaValue tonumber() {
		return NIL;
	}

	/**
	 * Conditionally convert to lua string without throwing errors.
	 * <p>
	 * In lua all numbers are strings, so this function will return
	 * the {@link LuaValue} {@code this} if it is a string or number,
	 * and {@link Constants#NIL} for all other cases.
	 * <p>
	 * This allows values to be tested for their "string-ness" without
	 * the penalty of throwing exceptions.
	 *
	 * @return {@code this} if it is a {@link LuaString} or {@link LuaNumber},
	 * otherwise {@link Constants#NIL}
	 * @see #tonumber()
	 * @see #tojstring()
	 * @see #optstring(LuaString)
	 * @see #checkstring()
	 * @see #toString()
	 */
	public LuaValue tostring() {
		return NIL;
	}

	/**
	 * Check that optional argument is a boolean and return its boolean value
	 *
	 * @param defval boolean value to return if {@code this} is nil or none
	 * @return {@code this} cast to boolean if a {@link LuaBoolean},
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not a boolean or nil or none.
	 * @see #checkboolean()
	 * @see #isboolean()
	 * @see Constants#TBOOLEAN
	 */
	public boolean optboolean(boolean defval) {
		argerror("boolean");
		return false;
	}

	/**
	 * Check that optional argument is a closure and return as {@link LuaClosure}
	 * <p>
	 * A {@link LuaClosure} is a {@link LuaFunction} that executes lua byteccode.
	 *
	 * @param defval {@link LuaClosure} to return if {@code this} is nil or none
	 * @return {@code this} cast to {@link LuaClosure} if a function,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not a closure or nil or none.
	 * @see #checkclosure()
	 * @see #isclosure()
	 * @see Constants#TFUNCTION
	 */
	public LuaClosure optclosure(LuaClosure defval) {
		argerror("closure");
		return null;
	}

	/**
	 * Check that optional argument is a number or string convertible to number and return as double
	 *
	 * @param defval double to return if {@code this} is nil or none
	 * @return {@code this} cast to double if numeric,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not numeric or nil or none.
	 * @see #optint(int)
	 * @see #optinteger(LuaInteger)
	 * @see #checkdouble()
	 * @see #todouble()
	 * @see #tonumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public double optdouble(double defval) {
		argerror("double");
		return 0;
	}

	/**
	 * Check that optional argument is a function and return as {@link LuaFunction}
	 * <p>
	 * A {@link LuaFunction} may either be a Java function that implements
	 * functionality directly in Java,
	 * or a {@link LuaClosure}
	 * which is a {@link LuaFunction} that executes lua bytecode.
	 *
	 * @param defval {@link LuaFunction} to return if {@code this} is nil or none
	 * @return {@code this} cast to {@link LuaFunction} if a function,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not a function or nil or none.
	 * @see #checkfunction()
	 * @see #isfunction()
	 * @see Constants#TFUNCTION
	 */
	public LuaFunction optfunction(LuaFunction defval) {
		argerror("function");
		return null;
	}

	/**
	 * Check that optional argument is a number or string convertible to number and return as int
	 *
	 * @param defval int to return if {@code this} is nil or none
	 * @return {@code this} cast to int if numeric,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not numeric or nil or none.
	 * @see #optdouble(double)
	 * @see #optlong(long)
	 * @see #optinteger(LuaInteger)
	 * @see #checkint()
	 * @see #toint()
	 * @see #tonumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public int optint(int defval) {
		argerror("int");
		return 0;
	}

	/**
	 * Check that optional argument is a number or string convertible to number and return as {@link LuaInteger}
	 *
	 * @param defval {@link LuaInteger} to return if {@code this} is nil or none
	 * @return {@code this} converted and wrapped in {@link LuaInteger} if numeric,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not numeric or nil or none.
	 * @see #optdouble(double)
	 * @see #optint(int)
	 * @see #checkint()
	 * @see #toint()
	 * @see #tonumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public LuaInteger optinteger(LuaInteger defval) {
		argerror("integer");
		return null;
	}

	/**
	 * Check that optional argument is a number or string convertible to number and return as long
	 *
	 * @param defval long to return if {@code this} is nil or none
	 * @return {@code this} cast to long if numeric,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not numeric or nil or none.
	 * @see #optdouble(double)
	 * @see #optint(int)
	 * @see #checkint()
	 * @see #toint()
	 * @see #tonumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public long optlong(long defval) {
		argerror("long");
		return 0;
	}

	/**
	 * Check that optional argument is a number or string convertible to number and return as {@link LuaNumber}
	 *
	 * @param defval {@link LuaNumber} to return if {@code this} is nil or none
	 * @return {@code this} cast to {@link LuaNumber} if numeric,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not numeric or nil or none.
	 * @see #optdouble(double)
	 * @see #optlong(long)
	 * @see #optint(int)
	 * @see #checkint()
	 * @see #toint()
	 * @see #tonumber()
	 * @see #isnumber()
	 * @see Constants#TNUMBER
	 */
	public LuaNumber optnumber(LuaNumber defval) {
		argerror("number");
		return null;
	}

	/**
	 * Check that optional argument is a string or number and return as Java String
	 *
	 * @param defval {@link LuaString} to return if {@code this} is nil or none
	 * @return {@code this} converted to String if a string or number,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a string or number or nil or none.
	 * @see #tojstring()
	 * @see #optstring(LuaString)
	 * @see #checkjstring()
	 * @see #toString()
	 * @see Constants#TSTRING
	 */
	public String optjstring(String defval) {
		argerror("String");
		return null;
	}

	/**
	 * Check that optional argument is a string or number and return as {@link LuaString}
	 *
	 * @param defval {@link LuaString} to return if {@code this} is nil or none
	 * @return {@code this} converted to {@link LuaString} if a string or number,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a string or number or nil or none.
	 * @see #tojstring()
	 * @see #optjstring(String)
	 * @see #checkstring()
	 * @see #toString()
	 * @see Constants#TSTRING
	 */
	public LuaString optstring(LuaString defval) {
		argerror("string");
		return null;
	}

	/**
	 * Check that optional argument is a table and return as {@link LuaTable}
	 *
	 * @param defval {@link LuaTable} to return if {@code this} is nil or none
	 * @return {@code this} cast to {@link LuaTable} if a table,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a table or nil or none.
	 * @see #checktable()
	 * @see #istable()
	 * @see Constants#TTABLE
	 */
	public LuaTable opttable(LuaTable defval) {
		argerror("table");
		return null;
	}

	/**
	 * Check that optional argument is a thread and return as {@link LuaThread}
	 *
	 * @param defval {@link LuaThread} to return if {@code this} is nil or none
	 * @return {@code this} cast to {@link LuaTable} if a thread,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a thread or nil or none.
	 * @see #checkthread()
	 * @see #isthread()
	 * @see Constants#TTHREAD
	 */
	public LuaThread optthread(LuaThread defval) {
		argerror("thread");
		return null;
	}

	/**
	 * Check that optional argument is a userdata and return the Object instance
	 *
	 * @param defval Object to return if {@code this} is nil or none
	 * @return Object instance of the userdata if a {@link LuaUserdata},
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a userdata or nil or none.
	 * @see #checkuserdata()
	 * @see #isuserdata()
	 * @see #optuserdata(Class, Object)
	 * @see Constants#TUSERDATA
	 */
	public Object optuserdata(Object defval) {
		argerror("object");
		return null;
	}

	/**
	 * Check that optional argument is a userdata whose instance is of a type
	 * and return the Object instance
	 *
	 * @param c      Class to test userdata instance against
	 * @param defval Object to return if {@code this} is nil or none
	 * @return Object instance of the userdata if a {@link LuaUserdata} and instance is assignable to {@code c},
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a userdata whose instance is assignable to {@code c} or nil or none.
	 * @see #checkuserdata(Class)
	 * @see #isuserdata(Class)
	 * @see #optuserdata(Object)
	 * @see Constants#TUSERDATA
	 */
	public Object optuserdata(Class<?> c, Object defval) {
		argerror(c.getName());
		return null;
	}

	/**
	 * Perform argument check that this is not nil or none.
	 *
	 * @param defval {@link LuaValue} to return if {@code this} is nil or none
	 * @return {@code this} if not nil or none, else {@code defval}
	 * @see Constants#NIL
	 * @see Constants#NONE
	 * @see #isnil()
	 * @see Varargs#isnoneornil(int)
	 * @see Constants#TNIL
	 * @see Constants#TNONE
	 */
	public LuaValue optvalue(LuaValue defval) {
		return this;
	}


	/**
	 * Check that the value is a {@link LuaBoolean},
	 * or throw {@link LuaError} if not
	 *
	 * @return boolean value for {@code this} if it is a {@link LuaBoolean}
	 * @throws LuaError if not a {@link LuaBoolean}
	 * @see #optboolean(boolean)
	 * @see Constants#TBOOLEAN
	 */
	public boolean checkboolean() {
		argerror("boolean");
		return false;
	}

	/**
	 * Check that the value is a {@link LuaClosure} ,
	 * or throw {@link LuaError} if not
	 * <p>
	 * {@link LuaClosure} is a subclass of {@link LuaFunction} that interprets lua bytecode.
	 *
	 * @return {@code this} cast as {@link LuaClosure}
	 * @throws LuaError if not a {@link LuaClosure}
	 * @see #checkfunction()
	 * @see #optclosure(LuaClosure)
	 * @see #isclosure()
	 * @see Constants#TFUNCTION
	 */
	public LuaClosure checkclosure() {
		argerror("closure");
		return null;
	}

	public double checkarith() {
		aritherror();
		return Double.NaN;
	}

	/**
	 * Check that the value is numeric and return the value as a double,
	 * or throw {@link LuaError} if not numeric
	 * <p>
	 * Values that are {@link LuaNumber} and values that are {@link LuaString}
	 * that can be converted to a number will be converted to double.
	 *
	 * @return value cast to a double if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkint()
	 * @see #checkinteger()
	 * @see #checklong()
	 * @see #optdouble(double)
	 * @see Constants#TNUMBER
	 */
	public double checkdouble() {
		argerror("double");
		return 0;
	}

	/**
	 * Check that the value is a function , or throw {@link LuaError} if not
	 * <p>
	 * A function is considered anything whose {@link #type()} returns {@link Constants#TFUNCTION}.
	 * In practice it will be either a built-in Java function, typically deriving from
	 * {@link LuaFunction} or a {@link LuaClosure} which represents lua source compiled
	 * into lua bytecode.
	 *
	 * @return {@code this} if if a lua function or closure
	 * @throws LuaError if not a function
	 * @see #checkclosure()
	 */
	public LuaValue checkfunction() {
		argerror("function");
		return null;
	}

	/**
	 * Check that the value is numeric, and convert and cast value to int, or throw {@link LuaError} if not numeric
	 * <p>
	 * Values that are {@link LuaNumber} will be cast to int and may lose precision.
	 * Values that are {@link LuaString} that can be converted to a number will be converted,
	 * then cast to int, so may also lose precision.
	 *
	 * @return value cast to a int if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkinteger()
	 * @see #checklong()
	 * @see #checkdouble()
	 * @see #optint(int)
	 * @see Constants#TNUMBER
	 */
	public int checkint() {
		argerror("int");
		return 0;
	}

	/**
	 * Check that the value is numeric, and convert and cast value to int, or throw {@link LuaError} if not numeric
	 * <p>
	 * Values that are {@link LuaNumber} will be cast to int and may lose precision.
	 * Values that are {@link LuaString} that can be converted to a number will be converted,
	 * then cast to int, so may also lose precision.
	 *
	 * @return value cast to a int and wrapped in {@link LuaInteger} if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkint()
	 * @see #checklong()
	 * @see #checkdouble()
	 * @see #optinteger(LuaInteger)
	 * @see Constants#TNUMBER
	 */
	public LuaInteger checkinteger() {
		argerror("integer");
		return null;
	}

	/**
	 * Check that the value is numeric, and convert and cast value to long, or throw {@link LuaError} if not numeric
	 * <p>
	 * Values that are {@link LuaNumber} will be cast to long and may lose precision.
	 * Values that are {@link LuaString} that can be converted to a number will be converted,
	 * then cast to long, so may also lose precision.
	 *
	 * @return value cast to a long if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkint()
	 * @see #checkinteger()
	 * @see #checkdouble()
	 * @see #optlong(long)
	 * @see Constants#TNUMBER
	 */
	public long checklong() {
		argerror("long");
		return 0;
	}

	/**
	 * Check that the value is numeric, and return as a LuaNumber if so, or throw {@link LuaError}
	 * <p>
	 * Values that are {@link LuaString} that can be converted to a number will be converted and returned.
	 *
	 * @return value as a {@link LuaNumber} if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkint()
	 * @see #checkinteger()
	 * @see #checkdouble()
	 * @see #checklong()
	 * @see #optnumber(LuaNumber)
	 * @see Constants#TNUMBER
	 */
	public LuaNumber checknumber() {
		argerror("number");
		return null;
	}

	/**
	 * Check that the value is numeric, and return as a LuaNumber if so, or throw {@link LuaError}
	 * <p>
	 * Values that are {@link LuaString} that can be converted to a number will be converted and returned.
	 *
	 * @param msg String message to supply if conversion fails
	 * @return value as a {@link LuaNumber} if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkint()
	 * @see #checkinteger()
	 * @see #checkdouble()
	 * @see #checklong()
	 * @see #optnumber(LuaNumber)
	 * @see Constants#TNUMBER
	 */
	public LuaNumber checknumber(String msg) {
		throw new LuaError(msg);
	}

	/**
	 * Convert this value to a Java String.
	 * <p>
	 * The string representations here will roughly match what is produced by the
	 * C lua distribution, however hash codes have no relationship,
	 * and there may be differences in number formatting.
	 *
	 * @return String representation of the value
	 * @see #checkstring()
	 * @see #optjstring(String)
	 * @see #tojstring()
	 * @see #isstring
	 * @see Constants#TSTRING
	 */
	public String checkjstring() {
		argerror("string");
		return null;
	}

	/**
	 * Check that this is a lua string, or throw {@link LuaError} if it is not.
	 * <p>
	 * In lua all numbers are strings, so this will succeed for
	 * anything that derives from {@link LuaString} or {@link LuaNumber}.
	 * Numbers will be converted to {@link LuaString}.
	 *
	 * @return {@link LuaString} representation of the value if it is a {@link LuaString} or {@link LuaNumber}
	 * @throws LuaError if {@code this} is not a {@link LuaTable}
	 * @see #checkjstring()
	 * @see #optstring(LuaString)
	 * @see #tostring()
	 * @see #isstring()
	 * @see Constants#TSTRING
	 */
	public LuaString checkstring() {
		argerror("string");
		return null;
	}

	/**
	 * Check that this is a {@link LuaTable}, or throw {@link LuaError} if it is not
	 *
	 * @return {@code this} if it is a {@link LuaTable}
	 * @throws LuaError if {@code this} is not a {@link LuaTable}
	 * @see #istable()
	 * @see #opttable(LuaTable)
	 * @see Constants#TTABLE
	 */
	public LuaTable checktable() {
		argerror("table");
		return null;
	}

	/**
	 * Check that this is a {@link LuaThread}, or throw {@link LuaError} if it is not
	 *
	 * @return {@code this} if it is a {@link LuaThread}
	 * @throws LuaError if {@code this} is not a {@link LuaThread}
	 * @see #isthread()
	 * @see #optthread(LuaThread)
	 * @see Constants#TTHREAD
	 */
	public LuaThread checkthread() {
		argerror("thread");
		return null;
	}

	/**
	 * Check that this is a {@link LuaUserdata}, or throw {@link LuaError} if it is not
	 *
	 * @return {@code this} if it is a {@link LuaUserdata}
	 * @throws LuaError if {@code this} is not a {@link LuaUserdata}
	 * @see #isuserdata()
	 * @see #optuserdata(Object)
	 * @see #checkuserdata(Class)
	 * @see Constants#TUSERDATA
	 */
	public Object checkuserdata() {
		argerror("userdata");
		return null;
	}

	/**
	 * Check that this is a {@link LuaUserdata}, or throw {@link LuaError} if it is not
	 *
	 * @param c The class of userdata to convert to
	 * @return {@code this} if it is a {@link LuaUserdata}
	 * @throws LuaError if {@code this} is not a {@link LuaUserdata}
	 * @see #isuserdata(Class)
	 * @see #optuserdata(Class, Object)
	 * @see #checkuserdata()
	 * @see Constants#TUSERDATA
	 */
	public Object checkuserdata(Class<?> c) {
		argerror("userdata");
		return null;
	}

	/**
	 * Check that this is not the value {@link Constants#NIL}, or throw {@link LuaError} if it is
	 *
	 * @return {@code this} if it is not {@link Constants#NIL}
	 * @throws LuaError if {@code this} is {@link Constants#NIL}
	 * @see #optvalue(LuaValue)
	 */
	public LuaValue checknotnil() {
		return this;
	}

	/**
	 * Check that this is a valid key in a table index operation, or throw {@link LuaError} if not
	 *
	 * @return {@code this} if valid as a table key
	 * @throws LuaError if not valid as a table key
	 * @see #isnil()
	 * @see #isinttype()
	 */
	public LuaValue checkvalidkey() {
		return this;
	}

	/**
	 * Throw a {@link LuaError} with a particular message
	 *
	 * @param message String providing message details
	 * @return Absolutley nothing
	 * @throws LuaError in all cases
	 */
	public static LuaValue error(String message) {
		throw new LuaError(message);
	}

	/**
	 * Assert a condition is true, or throw a {@link LuaError} if not
	 *
	 * @param b   condition to test
	 * @param msg Error message
	 * @throws LuaError if b is not true
	 */
	public static void assert_(boolean b, String msg) {
		if (!b) throw new LuaError(msg);
	}

	/**
	 * Throw a {@link LuaError} indicating an invalid argument was supplied to a function
	 *
	 * @param expected String naming the type that was expected
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	protected LuaValue argerror(String expected) {
		throw new LuaError("bad argument: " + expected + " expected, got " + typename());
	}

	/**
	 * Throw a {@link LuaError} indicating an invalid argument was supplied to a function
	 *
	 * @param iarg index of the argument that was invalid, first index is 1
	 * @param msg  String providing information about the invalid argument
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	public static LuaValue argerror(int iarg, String msg) {
		throw new LuaError("bad argument #" + iarg + ": " + msg);
	}

	/**
	 * Throw a {@link LuaError} indicating an invalid type was supplied to a function
	 *
	 * @param expected String naming the type that was expected
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	protected LuaValue typerror(String expected) {
		throw new LuaError(expected + " expected, got " + typename());
	}

	/**
	 * Throw a {@link LuaError} indicating an operation is not implemented
	 *
	 * @param fun Function that hasn't been implemented
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	protected LuaValue unimplemented(String fun) {
		throw new LuaError("'" + fun + "' not implemented for " + typename());
	}

	/**
	 * Throw a {@link LuaError} indicating an illegal operation occurred,
	 * typically involved in managing weak references
	 *
	 * @param op       Operation
	 * @param typename Current type
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	protected LuaValue illegal(String op, String typename) {
		throw new LuaError("illegal operation '" + op + "' for " + typename);
	}

	/**
	 * Throw a {@link LuaError} based on the len operator,
	 * typically due to an invalid operand type
	 *
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	protected LuaValue lenerror() {
		throw new LuaError("attempt to get length of " + typename());
	}

	/**
	 * Throw a {@link LuaError} based on an arithmetic error such as add, or pow,
	 * typically due to an invalid operand type
	 *
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	protected LuaValue aritherror() {
		throw new LuaError("attempt to perform arithmetic on " + typename());
	}

	/**
	 * Throw a {@link LuaError} based on an arithmetic error such as add, or pow,
	 * typically due to an invalid operand type
	 *
	 * @param fun String description of the function that was attempted
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	protected LuaValue aritherror(String fun) {
		throw new LuaError("attempt to perform arithmetic '" + fun + "' on " + typename());
	}

	/**
	 * Throw a {@link LuaError} based on a comparison error such as greater-than or less-than,
	 * typically due to an invalid operand type
	 *
	 * @param rhs String description of what was on the right-hand-side of the comparison that resulted in the error.
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	protected LuaValue compareerror(String rhs) {
		throw new LuaError("attempt to compare " + typename() + " with " + rhs);
	}

	/**
	 * Throw a {@link LuaError} based on a comparison error such as greater-than or less-than,
	 * typically due to an invalid operand type
	 *
	 * @param rhs Right-hand-side of the comparison that resulted in the error.
	 * @return Nothing
	 * @throws LuaError in all cases
	 */
	protected LuaValue compareerror(LuaValue rhs) {
		throw new LuaError("attempt to compare " + typename() + " with " + rhs.typename());
	}

	/**
	 * Get a value in a table including metatag processing using {@link Constants#INDEX}.
	 *
	 * @param key the key to look up, must not be {@link Constants#NIL} or null
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found and no metatag
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#INDEX} metatag,
	 *                  or key is {@link Constants#NIL}
	 * @see #get(int)
	 * @see #get(String)
	 * @see #rawget(LuaValue)
	 */
	public LuaValue get(LuaValue key) {
		return gettable(this, key);
	}

	/**
	 * Get a value in a table including metatag processing using {@link Constants#INDEX}.
	 *
	 * @param key the key to look up
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#INDEX} metatag
	 * @see #get(LuaValue)
	 * @see #rawget(int)
	 */
	public LuaValue get(int key) {
		return get(LuaInteger.valueOf(key));
	}

	/**
	 * Get a value in a table including metatag processing using {@link Constants#INDEX}.
	 *
	 * @param key the key to look up, must not be null
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#INDEX} metatag
	 * @see #get(LuaValue)
	 * @see #rawget(String)
	 */
	public LuaValue get(String key) {
		return get(valueOf(key));
	}

	/**
	 * Set a value in a table without metatag processing using {@link Constants#NEWINDEX}.
	 *
	 * @param key   the key to use, must not be {@link Constants#NIL} or null
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table,
	 *                  or key is {@link Constants#NIL},
	 *                  or there is no {@link Constants#NEWINDEX} metatag
	 */
	public void set(LuaValue key, LuaValue value) {
		settable(this, key, value);
	}

	/**
	 * Set a value in a table without metatag processing using {@link Constants#NEWINDEX}.
	 *
	 * @param key   the key to use
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#NEWINDEX} metatag
	 */
	public void set(int key, LuaValue value) {
		set(LuaInteger.valueOf(key), value);
	}

	/**
	 * Set a value in a table without metatag processing using {@link Constants#NEWINDEX}.
	 *
	 * @param key   the key to use
	 * @param value the value to use, must not be null
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#NEWINDEX} metatag
	 */
	public void set(int key, String value) {
		set(key, valueOf(value));
	}

	/**
	 * Set a value in a table without metatag processing using {@link Constants#NEWINDEX}.
	 *
	 * @param key   the key to use, must not be {@link Constants#NIL} or null
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#NEWINDEX} metatag
	 */
	public void set(String key, LuaValue value) {
		set(valueOf(key), value);
	}

	/**
	 * Set a value in a table without metatag processing using {@link Constants#NEWINDEX}.
	 *
	 * @param key   the key to use, must not be null
	 * @param value the value to use
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#NEWINDEX} metatag
	 */
	public void set(String key, double value) {
		set(valueOf(key), valueOf(value));
	}

	/**
	 * Set a value in a table without metatag processing using {@link Constants#NEWINDEX}.
	 *
	 * @param key   the key to use, must not be null
	 * @param value the value to use
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#NEWINDEX} metatag
	 */
	public void set(String key, int value) {
		set(valueOf(key), valueOf(value));
	}

	/**
	 * Set a value in a table without metatag processing using {@link Constants#NEWINDEX}.
	 *
	 * @param key   the key to use, must not be null
	 * @param value the value to use, must not be null
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#NEWINDEX} metatag
	 */
	public void set(String key, String value) {
		set(valueOf(key), valueOf(value));
	}

	/**
	 * Get a value in a table without metatag processing.
	 *
	 * @param key the key to look up, must not be {@link Constants#NIL} or null
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 * @throws LuaError if {@code this} is not a table, or key is {@link Constants#NIL}
	 */
	public LuaValue rawget(LuaValue key) {
		return unimplemented("rawget");
	}

	/**
	 * Get a value in a table without metatag processing.
	 *
	 * @param key the key to look up
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 * @throws LuaError if {@code this} is not a table
	 */
	public LuaValue rawget(int key) {
		return rawget(valueOf(key));
	}

	/**
	 * Get a value in a table without metatag processing.
	 *
	 * @param key the key to look up, must not be null
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 * @throws LuaError if {@code this} is not a table
	 */
	public LuaValue rawget(String key) {
		return rawget(valueOf(key));
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use, must not be {@link Constants#NIL} or null
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table, or key is {@link Constants#NIL}
	 */
	public void rawset(LuaValue key, LuaValue value) {
		unimplemented("rawset");
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table
	 */
	public void rawset(int key, LuaValue value) {
		rawset(valueOf(key), value);
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table
	 */
	public void rawset(int key, String value) {
		rawset(key, valueOf(value));
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use, must not be null
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table
	 */
	public void rawset(String key, LuaValue value) {
		rawset(valueOf(key), value);
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use, must not be null
	 * @param value the value to use
	 * @throws LuaError if {@code this} is not a table
	 */
	public void rawset(String key, double value) {
		rawset(valueOf(key), valueOf(value));
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use, must not be null
	 * @param value the value to use
	 * @throws LuaError if {@code this} is not a table
	 */
	public void rawset(String key, int value) {
		rawset(valueOf(key), valueOf(value));
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use, must not be null
	 * @param value the value to use, must not be null
	 * @throws LuaError if {@code this} is not a table
	 */
	public void rawset(String key, String value) {
		rawset(valueOf(key), valueOf(value));
	}

	/**
	 * Set list values in a table without invoking metatag processing
	 * <p>
	 * Primarily used internally in response to a SETLIST bytecode.
	 *
	 * @param key0   the first key to set in the table
	 * @param values the list of values to set
	 * @throws LuaError if this is not a table.
	 */
	public void rawsetlist(int key0, Varargs values) {
		for (int i = 0, n = values.narg(); i < n; i++) {
			rawset(key0 + i, values.arg(i + 1));
		}
	}

	/**
	 * Preallocate the array part of a table to be a certain size,
	 * <p>
	 * Primarily used internally in response to a SETLIST bytecode.
	 *
	 * @param i the number of array slots to preallocate in the table.
	 * @throws LuaError if this is not a table.
	 */
	public void presize(int i) {
		typerror("table");
	}

	/**
	 * Find the next key,value pair if {@code this} is a table,
	 * return {@link Constants#NIL} if there are no more, or throw a {@link LuaError} if not a table.
	 * <p>
	 * To iterate over all key-value pairs in a table you can use
	 * <pre> {@code
	 * LuaValue k = LuaValue.NIL;
	 * while ( true ) {
	 *    Varargs n = table.next(k);
	 *    if ( (k = n.arg1()).isnil() )
	 *       break;
	 *    LuaValue v = n.arg(2)
	 *    process( k, v )
	 * }}</pre>
	 *
	 * @param index {@link LuaInteger} value identifying a key to start from,
	 *              or {@link Constants#NIL} to start at the beginning
	 * @return {@link Varargs} containing {key,value} for the next entry,
	 * or {@link Constants#NIL} if there are no more.
	 * @throws LuaError if {@code this} is not a table, or the supplied key is invalid.
	 * @see LuaTable
	 * @see #inext(LuaValue)
	 * @see Factory#valueOf(int)
	 * @see Varargs#arg1()
	 * @see Varargs#arg(int)
	 * @see #isnil()
	 */
	public Varargs next(LuaValue index) {
		return typerror("table");
	}

	/**
	 * Find the next integer-key,value pair if {@code this} is a table,
	 * return {@link Constants#NIL} if there are no more, or throw a {@link LuaError} if not a table.
	 * <p>
	 * To iterate over integer keys in a table you can use
	 * <pre> {@code
	 *   LuaValue k = LuaValue.NIL;
	 *   while ( true ) {
	 *      Varargs n = table.inext(k);
	 *      if ( (k = n.arg1()).isnil() )
	 *         break;
	 *      LuaValue v = n.arg(2)
	 *      process( k, v )
	 *   }
	 * } </pre>
	 *
	 * @param index {@link LuaInteger} value identifying a key to start from,
	 *              or {@link Constants#NIL} to start at the beginning
	 * @return {@link Varargs} containing {@code (key, value)} for the next entry,
	 * or {@link Constants#NONE} if there are no more.
	 * @throws LuaError if {@code this} is not a table, or the supplied key is invalid.
	 * @see LuaTable
	 * @see #next(LuaValue)
	 * @see Factory#valueOf(int)
	 * @see Varargs#arg1()
	 * @see Varargs#arg(int)
	 * @see #isnil()
	 */
	public Varargs inext(LuaValue index) {
		return typerror("table");
	}

	/**
	 * Load a library instance by setting its environment to {@code this}
	 * and calling it, which should iniitalize the library instance and
	 * install itself into this instance.
	 *
	 * @param library The callable {@link LuaValue} to load into {@code this}
	 * @return {@link LuaValue} containing the result of the initialization call.
	 */
	public LuaValue load(LuaValue library) {
		library.setfenv(this);
		return library.call();
	}

	// varargs references
	@Override
	public LuaValue arg(int index) {
		return index == 1 ? this : NIL;
	}

	@Override
	public int narg() {
		return 1;
	}

	@Override
	public LuaValue arg1() {
		return this;
	}

	/**
	 * Get the metatable for this {@link LuaValue}
	 * <p>
	 * For {@link LuaTable} and {@link LuaUserdata} instances,
	 * the metatable returned is this instance metatable.
	 * For all other types, the class metatable value will be returned.
	 *
	 * @return metatable, or null if it there is none
	 * @see LuaBoolean#s_metatable
	 * @see LuaNumber#s_metatable
	 * @see LuaNil#s_metatable
	 * @see LuaFunction#s_metatable
	 * @see LuaThread#s_metatable
	 */
	public LuaValue getmetatable() {
		return null;
	}

	/**
	 * Set the metatable for this {@link LuaValue}
	 * <p>
	 * For {@link LuaTable} and {@link LuaUserdata} instances, the metatable is per instance.
	 * For all other types, there is one metatable per type that can be set directly from java
	 *
	 * @param metatable {@link LuaValue} instance to serve as the metatable, or null to reset it.
	 * @return {@code this} to allow chaining of Java function calls
	 * @see LuaBoolean#s_metatable
	 * @see LuaNumber#s_metatable
	 * @see LuaNil#s_metatable
	 * @see LuaFunction#s_metatable
	 * @see LuaThread#s_metatable
	 */
	public LuaValue setmetatable(LuaValue metatable) {
		return argerror("table");
	}

	/**
	 * Get the environemnt for an instance.
	 *
	 * @return {@link LuaValue} currently set as the instances environent.
	 */
	public LuaValue getfenv() {
		typerror("function or thread");
		return null;
	}

	/**
	 * Set the environment on an object.
	 * <p>
	 * Typically the environment is created once per application via a platform
	 * helper method such as {@link org.luaj.vm2.lib.jse.JsePlatform#standardGlobals()}
	 * However, any object can serve as an environment if it contains suitable metatag
	 * values to implement {@link #get(LuaValue)} to provide the environment values.
	 *
	 * @param env {@link LuaValue} (typically a {@link LuaTable}) containing the environment.
	 * @see org.luaj.vm2.lib.jse.JsePlatform
	 */
	public void setfenv(LuaValue env) {
		typerror("function or thread");
	}

	/**
	 * Call {@code this} with 0 arguments, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke()} instead.
	 * <p>
	 * To call {@code this} as a method call, use {@link #method(LuaValue)} instead.
	 *
	 * @return First return value {@code (this())}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaValue)
	 * @see #call(LuaValue, LuaValue)
	 * @see #call(LuaValue, LuaValue, LuaValue)
	 * @see #invoke()
	 * @see #method(String)
	 * @see #method(LuaValue)
	 */
	public LuaValue call() {
		return callmt().call(this);
	}

	/**
	 * Call {@code this} with 1 argument, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke()} instead.
	 * <p>
	 * To call {@code this} as a method call, use {@link #method(LuaValue)} instead.
	 *
	 * @param arg First argument to supply to the called function
	 * @return First return value {@code (this(arg))}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #call(LuaValue, LuaValue)
	 * @see #call(LuaValue, LuaValue, LuaValue)
	 * @see #invoke(Varargs)
	 * @see #method(String, LuaValue)
	 * @see #method(LuaValue, LuaValue)
	 */
	public LuaValue call(LuaValue arg) {
		return callmt().call(this, arg);
	}

	/**
	 * Call {@code this} with 2 arguments, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke()} instead.
	 * <p>
	 * To call {@code this} as a method call, use {@link #method(LuaValue)} instead.
	 *
	 * @param arg1 First argument to supply to the called function
	 * @param arg2 Second argument to supply to the called function
	 * @return First return value {@code (this(arg1, arg2))}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #call(LuaValue)
	 * @see #call(LuaValue, LuaValue, LuaValue)
	 * @see #invoke(LuaValue, Varargs)
	 * @see #method(String, LuaValue, LuaValue)
	 * @see #method(LuaValue, LuaValue, LuaValue)
	 */
	public LuaValue call(LuaValue arg1, LuaValue arg2) {
		return callmt().call(this, arg1, arg2);
	}

	/**
	 * Call {@code this} with 3 arguments, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke()} instead.
	 * <p>
	 * To call {@code this} as a method call, use {@link #method(LuaValue)} instead.
	 *
	 * @param arg1 First argument to supply to the called function
	 * @param arg2 Second argument to supply to the called function
	 * @param arg3 Second argument to supply to the called function
	 * @return First return value {@code (this(arg1, arg2, arg3))}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #call(LuaValue)
	 * @see #call(LuaValue, LuaValue)
	 * @see #invoke(LuaValue, LuaValue, Varargs)
	 * @see #invokemethod(String, Varargs)
	 * @see #invokemethod(LuaValue, Varargs)
	 */
	public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return callmt().invoke(new LuaValue[]{this, arg1, arg2, arg3}).arg1();
	}

	/**
	 * Call named method on {@code this} with 0 arguments, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument.
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke()} instead.
	 * <p>
	 * To call {@code this} as a plain call, use {@link #call()} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @return All values returned from {@code this:name()} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #invoke()
	 * @see #method(LuaValue)
	 * @see #method(String, LuaValue)
	 * @see #method(String, LuaValue, LuaValue)
	 */
	public LuaValue method(String name) {
		return this.get(name).call(this);
	}

	/**
	 * Call named method on {@code this} with 0 arguments, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke()} instead.
	 * <p>
	 * To call {@code this} as a plain call, use {@link #call()} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @return All values returned from {@code this:name()} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #invoke()
	 * @see #method(String)
	 * @see #method(LuaValue, LuaValue)
	 * @see #method(LuaValue, LuaValue, LuaValue)
	 */
	public LuaValue method(LuaValue name) {
		return this.get(name).call(this);
	}

	/**
	 * Call named method on {@code this} with 1 argument, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke()} instead.
	 * <p>
	 * To call {@code this} as a plain call, use {@link #call(LuaValue)} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @param arg  Argument to supply to the method
	 * @return All values returned from {@code this:name(arg)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaValue)
	 * @see #invoke(Varargs)
	 * @see #method(LuaValue, LuaValue)
	 * @see #method(String)
	 * @see #method(String, LuaValue, LuaValue)
	 */
	public LuaValue method(String name, LuaValue arg) {
		return this.get(name).call(this, arg);
	}

	/**
	 * Call named method on {@code this} with 1 argument, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke()} instead.
	 * <p>
	 * To call {@code this} as a plain call, use {@link #call(LuaValue)} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @param arg  Argument to supply to the method
	 * @return All values returned from {@code this:name(arg)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaValue)
	 * @see #invoke(Varargs)
	 * @see #method(String, LuaValue)
	 * @see #method(LuaValue)
	 * @see #method(LuaValue, LuaValue, LuaValue)
	 */
	public LuaValue method(LuaValue name, LuaValue arg) {
		return this.get(name).call(this, arg);
	}

	/**
	 * Call named method on {@code this} with 2 arguments, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke()} instead.
	 * <p>
	 * To call {@code this} as a plain call, use {@link #call(LuaValue, LuaValue)} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @param arg1 First argument to supply to the method
	 * @param arg2 Second argument to supply to the method
	 * @return All values returned from {@code this:name(arg1,arg2)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaValue, LuaValue)
	 * @see #invoke(LuaValue, Varargs)
	 * @see #method(String, LuaValue)
	 * @see #method(LuaValue, LuaValue, LuaValue)
	 */
	public LuaValue method(String name, LuaValue arg1, LuaValue arg2) {
		return this.get(name).call(this, arg1, arg2);
	}

	/**
	 * Call named method on {@code this} with 2 arguments, including metatag processing,
	 * and return only the first return value.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke()} instead.
	 * <p>
	 * To call {@code this} as a plain call, use {@link #call(LuaValue, LuaValue)} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @param arg1 First argument to supply to the method
	 * @param arg2 Second argument to supply to the method
	 * @return All values returned from {@code this:name(arg1,arg2)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaValue, LuaValue)
	 * @see #invoke(LuaValue, Varargs)
	 * @see #method(LuaValue, LuaValue)
	 * @see #method(String, LuaValue, LuaValue)
	 */
	public LuaValue method(LuaValue name, LuaValue arg1, LuaValue arg2) {
		return this.get(name).call(this, arg1, arg2);
	}

	/**
	 * Call {@code this} with 0 arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it, and return all values.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a method call, use {@link #invokemethod(LuaValue)} instead.
	 *
	 * @return All return values as a {@link Varargs} instance.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #invoke(Varargs)
	 * @see #invokemethod(String)
	 * @see #invokemethod(LuaValue)
	 */
	public Varargs invoke() {
		return invoke(NONE);
	}

	/**
	 * Call {@code this} with variable arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it, and return all values.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a method call, use {@link #invokemethod(LuaValue)} instead.
	 *
	 * @param args Varargs containing the arguments to supply to the called function
	 * @return All return values as a {@link Varargs} instance.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see Factory#varargsOf(LuaValue[])
	 * @see #call(LuaValue)
	 * @see #invoke()
	 * @see #invoke(LuaValue, Varargs)
	 * @see #invokemethod(String, Varargs)
	 * @see #invokemethod(LuaValue, Varargs)
	 */
	public Varargs invoke(Varargs args) {
		return callmt().invoke(this, args);
	}

	/**
	 * Call {@code this} with variable arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it, and return all values.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a method call, use {@link #invokemethod(LuaValue, Varargs)} instead.
	 *
	 * @param arg     The first argument to supply to the called function
	 * @param varargs Varargs containing the remaining arguments to supply to the called function
	 * @return All return values as a {@link Varargs} instance.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see Factory#varargsOf(LuaValue[])
	 * @see #call(LuaValue, LuaValue)
	 * @see #invoke(LuaValue, Varargs)
	 * @see #invokemethod(String, Varargs)
	 * @see #invokemethod(LuaValue, Varargs)
	 */
	public Varargs invoke(LuaValue arg, Varargs varargs) {
		return invoke(varargsOf(arg, varargs));
	}

	/**
	 * Call {@code this} with variable arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it, and return all values.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a method call, use {@link #invokemethod(LuaValue, Varargs)} instead.
	 *
	 * @param arg1    The first argument to supply to the called function
	 * @param arg2    The second argument to supply to the called function
	 * @param varargs Varargs containing the remaining arguments to supply to the called function
	 * @return All return values as a {@link Varargs} instance.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see Factory#varargsOf(LuaValue[])
	 * @see #call(LuaValue, LuaValue, LuaValue)
	 * @see #invoke(LuaValue, LuaValue, Varargs)
	 * @see #invokemethod(String, Varargs)
	 * @see #invokemethod(LuaValue, Varargs)
	 */
	public Varargs invoke(LuaValue arg1, LuaValue arg2, Varargs varargs) {
		return invoke(varargsOf(arg1, arg2, varargs));
	}

	/**
	 * Call {@code this} with variable arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it, and return all values.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a method call, use {@link #invokemethod(LuaValue, Varargs)} instead.
	 *
	 * @param args Array of arguments to supply to the called function
	 * @return All return values as a {@link Varargs} instance.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see Factory#varargsOf(LuaValue[])
	 * @see #call(LuaValue, LuaValue, LuaValue)
	 * @see #invoke(LuaValue, LuaValue, Varargs)
	 * @see #invokemethod(String, LuaValue[])
	 * @see #invokemethod(LuaValue, LuaValue[])
	 */
	public Varargs invoke(LuaValue[] args) {
		return invoke(varargsOf(args));
	}

	/**
	 * Call {@code this} with variable arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * If {@code this} is a {@link LuaFunction}, call it, and return all values.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a method call, use {@link #invokemethod(LuaValue, Varargs)} instead.
	 *
	 * @param args    Array of arguments to supply to the called function
	 * @param varargs Varargs containing additional arguments to supply to the called function
	 * @return All return values as a {@link Varargs} instance.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see Factory#varargsOf(LuaValue[])
	 * @see #call(LuaValue, LuaValue, LuaValue)
	 * @see #invoke(LuaValue, LuaValue, Varargs)
	 * @see #invokemethod(String, LuaValue[])
	 * @see #invokemethod(LuaValue, LuaValue[])
	 * @see #invokemethod(String, Varargs)
	 * @see #invokemethod(LuaValue, Varargs)
	 */
	public Varargs invoke(LuaValue[] args, Varargs varargs) {
		return invoke(varargsOf(args, varargs));
	}

	/**
	 * Call named method on {@code this} with 0 arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return all return values as a {@link Varargs} instance.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a plain call, use {@link #invoke()} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @return All values returned from {@code this:name()} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #invoke()
	 * @see #method(String)
	 * @see #invokemethod(LuaValue)
	 * @see #invokemethod(String, Varargs)
	 */
	public Varargs invokemethod(String name) {
		return get(name).invoke(this);
	}

	/**
	 * Call named method on {@code this} with 0 arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return all return values as a {@link Varargs} instance.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a plain call, use {@link #invoke()} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @return All values returned from {@code this:name()} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #invoke()
	 * @see #method(LuaValue)
	 * @see #invokemethod(String)
	 * @see #invokemethod(LuaValue, Varargs)
	 */
	public Varargs invokemethod(LuaValue name) {
		return get(name).invoke(this);
	}

	/**
	 * Call named method on {@code this} with 1 argument, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return all return values as a {@link Varargs} instance.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a plain call, use {@link #invoke(Varargs)} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @param args {@link Varargs} containing arguments to supply to the called function after {@code this}
	 * @return All values returned from {@code this:name(args)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #invoke(Varargs)
	 * @see #method(String)
	 * @see #invokemethod(LuaValue, Varargs)
	 * @see #invokemethod(String, LuaValue[])
	 */
	public Varargs invokemethod(String name, Varargs args) {
		return get(name).invoke(varargsOf(this, args));
	}

	/**
	 * Call named method on {@code this} with variable arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return all return values as a {@link Varargs} instance.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a plain call, use {@link #invoke(Varargs)} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @param args {@link Varargs} containing arguments to supply to the called function after {@code this}
	 * @return All values returned from {@code this:name(args)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #invoke(Varargs)
	 * @see #method(String)
	 * @see #invokemethod(String, Varargs)
	 * @see #invokemethod(LuaValue, LuaValue[])
	 */
	public Varargs invokemethod(LuaValue name, Varargs args) {
		return get(name).invoke(varargsOf(this, args));
	}

	/**
	 * Call named method on {@code this} with 1 argument, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return all return values as a {@link Varargs} instance.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a plain call, use {@link #invoke(Varargs)} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @param args Array of {@link LuaValue} containing arguments to supply to the called function after {@code this}
	 * @return All values returned from {@code this:name(args)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #invoke(Varargs)
	 * @see #method(String)
	 * @see #invokemethod(LuaValue, LuaValue[])
	 * @see #invokemethod(String, Varargs)
	 * @see Factory#varargsOf(LuaValue[])
	 */
	public Varargs invokemethod(String name, LuaValue[] args) {
		return get(name).invoke(varargsOf(this, varargsOf(args)));
	}

	/**
	 * Call named method on {@code this} with variable arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 * <p>
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return all return values as a {@link Varargs} instance.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 * <p>
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 * <p>
	 * To call {@code this} as a plain call, use {@link #invoke(Varargs)} instead.
	 *
	 * @param name Name of the method to look up for invocation
	 * @param args Array of {@link LuaValue} containing arguments to supply to the called function after {@code this}
	 * @return All values returned from {@code this:name(args)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call()
	 * @see #invoke(Varargs)
	 * @see #method(String)
	 * @see #invokemethod(String, LuaValue[])
	 * @see #invokemethod(LuaValue, Varargs)
	 * @see Factory#varargsOf(LuaValue[])
	 */
	public Varargs invokemethod(LuaValue name, LuaValue[] args) {
		return get(name).invoke(varargsOf(this, varargsOf(args)));
	}

	/**
	 * Get the metatag value for the {@link Constants#CALL} metatag, if it exists.
	 *
	 * @return {@link LuaValue} value if metatag is defined
	 * @throws LuaError if {@link Constants#CALL} metatag is not defined.
	 */
	protected LuaValue callmt() {
		return checkmetatag(CALL, "attempt to call ");
	}

	/**
	 * Unary not: return inverse boolean value {@code (~this)} as defined by lua not operator
	 *
	 * @return {@link Constants#TRUE} if {@link Constants#NIL} or {@link Constants#FALSE}, otherwise {@link Constants#FALSE}
	 */
	public LuaValue not() {
		return FALSE;
	}

	/**
	 * Unary minus: return negative value {@code (-this)} as defined by lua unary minus operator
	 *
	 * @return boolean inverse as {@link LuaBoolean} if boolean or nil,
	 * numeric inverse as {@link LuaNumber} if numeric,
	 * or metatag processing result if {@link Constants#UNM} metatag is defined
	 * @throws LuaError if  {@code this} is not a table or string, and has no {@link Constants#UNM} metatag
	 */
	public LuaValue neg() {
		return checkmetatag(UNM, "attempt to perform arithmetic on ").call(this);
	}

	/**
	 * Length operator: return lua length of object {@code (#this)} including metatag processing as java int
	 *
	 * @return length as defined by the lua # operator
	 * or metatag processing result
	 * @throws LuaError if  {@code this} is not a table or string, and has no {@link Constants#LEN} metatag
	 */
	public LuaValue len() {
		return checkmetatag(LEN, "attempt to get length of ").call(this);
	}

	/**
	 * Length operator: return lua length of object {@code (#this)} including metatag processing as java int
	 *
	 * @return length as defined by the lua # operator
	 * or metatag processing result converted to java int using {@link #toint()}
	 * @throws LuaError if  {@code this} is not a table or string, and has no {@link Constants#LEN} metatag
	 */
	public int length() {
		return len().toint();
	}

	/**
	 * Implementation of lua 5.0 getn() function.
	 *
	 * @return value of getn() as defined in lua 5.0 spec if {@code this} is a {@link LuaTable}
	 * @throws LuaError if  {@code this} is not a {@link LuaTable}
	 */
	public LuaValue getn() {
		return typerror("getn");
	}

	// object equality, used for key comparison
	public boolean equals(Object obj) {
		return this == obj;
	}

	/**
	 * Equals: Perform direct equality comparison with another value
	 * without metatag processing.
	 *
	 * @param val The value to compare with.
	 * @return true if {@code (this == rhs)}, false otherwise
	 * @see #raweq(LuaUserdata)
	 * @see #raweq(LuaString)
	 * @see #raweq(double)
	 * @see #raweq(int)
	 * @see Constants#EQ
	 */
	public boolean raweq(LuaValue val) {
		return this == val;
	}

	/**
	 * Equals: Perform direct equality comparison with a {@link LuaUserdata} value
	 * without metatag processing.
	 *
	 * @param val The {@link LuaUserdata} to compare with.
	 * @return true if {@code this} is userdata
	 * and their metatables are the same using ==
	 * and their instances are equal using {@link #equals(Object)},
	 * otherwise false
	 * @see #raweq(LuaValue)
	 */
	public boolean raweq(LuaUserdata val) {
		return false;
	}

	/**
	 * Equals: Perform direct equality comparison with a {@link LuaString} value
	 * without metatag processing.
	 *
	 * @param val The {@link LuaString} to compare with.
	 * @return true if {@code this} is a {@link LuaString}
	 * and their byte sequences match,
	 * otherwise false
	 */
	public boolean raweq(LuaString val) {
		return false;
	}

	/**
	 * Equals: Perform direct equality comparison with a double value
	 * without metatag processing.
	 *
	 * @param val The double value to compare with.
	 * @return true if {@code this} is a {@link LuaNumber}
	 * whose value equals val,
	 * otherwise false
	 */
	public boolean raweq(double val) {
		return false;
	}

	/**
	 * Equals: Perform direct equality comparison with a int value
	 * without metatag processing.
	 *
	 * @param val The double value to compare with.
	 * @return true if {@code this} is a {@link LuaNumber}
	 * whose value equals val,
	 * otherwise false
	 */
	public boolean raweq(int val) {
		return false;
	}

	/**
	 * Perform equality testing metatag processing
	 *
	 * @param lhs   left-hand-side of equality expression
	 * @param lhsmt metatag value for left-hand-side
	 * @param rhs   right-hand-side of equality expression
	 * @param rhsmt metatag value for right-hand-side
	 * @return true if metatag processing result is not {@link Constants#NIL} or {@link Constants#FALSE}
	 * @throws LuaError if metatag was not defined for either operand
	 * @see #equals(Object)
	 * @see #raweq(LuaValue)
	 * @see Constants#EQ
	 */
	public static boolean eqmtcall(LuaValue lhs, LuaValue lhsmt, LuaValue rhs, LuaValue rhsmt) {
		LuaValue h = lhsmt.rawget(EQ);
		return !(h.isnil() || h != rhsmt.rawget(EQ)) && h.call(lhs, rhs).toboolean();
	}

	/**
	 * Perform string comparison with another value
	 * of any type
	 * using string comparison based on byte values.
	 * <p>
	 * Only strings can be compared, meaning
	 * each operand must derive from {@link LuaString}.
	 *
	 * @param rhs The right-hand-side value to perform the comparison with
	 * @return int &lt; 0 for {@code (this &lt; rhs)}, int &gt; 0 for {@code (this &gt; rhs)}, or 0 when same string.
	 * @throws LuaError if either operand is not a string
	 */
	public int strcmp(LuaValue rhs) {
		error("attempt to compare " + typename());
		return 0;
	}

	/**
	 * Perform string comparison with another value
	 * known to be a {@link LuaString}
	 * using string comparison based on byte values.
	 * <p>
	 * Only strings can be compared, meaning
	 * each operand must derive from {@link LuaString}.
	 *
	 * @param rhs The right-hand-side value to perform the comparison with
	 * @return int &lt; 0 for {@code (this &lt; rhs)}, int &gt; 0 for {@code (this &gt; rhs)}, or 0 when same string.
	 * @throws LuaError if this is not a string
	 */
	public int strcmp(LuaString rhs) {
		error("attempt to compare " + typename());
		return 0;
	}

	/**
	 * Concatenate another value onto this value and return the result
	 * using rules of lua string concatenation including metatag processing.
	 * <p>
	 * Only strings and numbers as represented can be concatenated, meaning
	 * each operand must derive from {@link LuaString} or {@link LuaNumber}.
	 *
	 * @param rhs The right-hand-side value to perform the operation with
	 * @return {@link LuaValue} resulting from concatenation of {@code (this .. rhs)}
	 * @throws LuaError if either operand is not of an appropriate type,
	 *                  such as nil or a table
	 */
	public LuaValue concat(LuaValue rhs) {
		return this.concatmt(rhs);
	}

	/**
	 * Reverse-concatenation: concatenate this value onto another value
	 * whose type is unknwon
	 * and return the result using rules of lua string concatenation including
	 * metatag processing.
	 * <p>
	 * Only strings and numbers as represented can be concatenated, meaning
	 * each operand must derive from {@link LuaString} or {@link LuaNumber}.
	 *
	 * @param lhs The left-hand-side value onto which this will be concatenated
	 * @return {@link LuaValue} resulting from concatenation of {@code (lhs .. this)}
	 * @throws LuaError if either operand is not of an appropriate type,
	 *                  such as nil or a table
	 * @see #concat(LuaValue)
	 */
	public LuaValue concatTo(LuaValue lhs) {
		return lhs.concatmt(this);
	}

	/**
	 * Reverse-concatenation: concatenate this value onto another value
	 * known to be a {@link  LuaNumber}
	 * and return the result using rules of lua string concatenation including
	 * metatag processing.
	 * <p>
	 * Only strings and numbers as represented can be concatenated, meaning
	 * each operand must derive from {@link LuaString} or {@link LuaNumber}.
	 *
	 * @param lhs The left-hand-side value onto which this will be concatenated
	 * @return {@link LuaValue} resulting from concatenation of {@code (lhs .. this)}
	 * @throws LuaError if either operand is not of an appropriate type,
	 *                  such as nil or a table
	 * @see #concat(LuaValue)
	 */
	public LuaValue concatTo(LuaNumber lhs) {
		return lhs.concatmt(this);
	}

	/**
	 * Reverse-concatenation: concatenate this value onto another value
	 * known to be a {@link  LuaString}
	 * and return the result using rules of lua string concatenation including
	 * metatag processing.
	 * <p>
	 * Only strings and numbers as represented can be concatenated, meaning
	 * each operand must derive from {@link LuaString} or {@link LuaNumber}.
	 *
	 * @param lhs The left-hand-side value onto which this will be concatenated
	 * @return {@link LuaValue} resulting from concatenation of {@code (lhs .. this)}
	 * @throws LuaError if either operand is not of an appropriate type,
	 *                  such as nil or a table
	 * @see #concat(LuaValue)
	 */
	public LuaValue concatTo(LuaString lhs) {
		return lhs.concatmt(this);
	}

	/**
	 * Convert the value to a {@link Buffer} for more efficient concatenation of
	 * multiple strings.
	 *
	 * @return Buffer instance containing the string or number
	 */
	public Buffer buffer() {
		return new Buffer(this);
	}

	/**
	 * Concatenate a {@link Buffer} onto this value and return the result
	 * using rules of lua string concatenation including metatag processing.
	 * <p>
	 * Only strings and numbers as represented can be concatenated, meaning
	 * each operand must derive from {@link LuaString} or {@link LuaNumber}.
	 *
	 * @param rhs The right-hand-side {@link Buffer} to perform the operation with
	 * @return LuaString resulting from concatenation of {@code (this .. rhs)}
	 * @throws LuaError if either operand is not of an appropriate type,
	 *                  such as nil or a table
	 */
	public Buffer concat(Buffer rhs) {
		return rhs.concatTo(this);
	}

	/**
	 * Perform metatag processing for concatenation operations.
	 * <p>
	 * Finds the {@link Constants#CONCAT} metatag value and invokes it,
	 * or throws {@link LuaError} if it doesn't exist.
	 *
	 * @param rhs The right-hand-side value to perform the operation with
	 * @return {@link LuaValue} resulting from metatag processing for {@link Constants#CONCAT} metatag.
	 * @throws LuaError if metatag was not defined for either operand
	 */
	public LuaValue concatmt(LuaValue rhs) {
		LuaValue h = metatag(CONCAT);
		if (h.isnil() && (h = rhs.metatag(CONCAT)).isnil()) {
			error("attempt to concatenate " + typename() + " and " + rhs.typename());
		}
		return h.call(this, rhs);
	}

	/**
	 * Perform boolean {@code and} with another operand, based on lua rules for boolean evaluation.
	 * This returns either {@code this} or {@code rhs} depending on the boolean value for {@code this}.
	 *
	 * @param rhs The right-hand-side value to perform the operation with
	 * @return {@code this} if {@code this.toboolean()} is false, {@code rhs} otherwise.
	 */
	public LuaValue and(LuaValue rhs) {
		return this.toboolean() ? rhs : this;
	}

	/**
	 * Perform boolean {@code or} with another operand, based on lua rules for boolean evaluation.
	 * This returns either {@code this} or {@code rhs} depending on the boolean value for {@code this}.
	 *
	 * @param rhs The right-hand-side value to perform the operation with
	 * @return {@code this} if {@code this.toboolean()} is true, {@code rhs} otherwise.
	 */
	public LuaValue or(LuaValue rhs) {
		return this.toboolean() ? this : rhs;
	}

	/**
	 * Convert this value to a string if it is a {@link LuaString} or {@link LuaNumber},
	 * or throw a {@link LuaError} if it is not
	 *
	 * @return {@link LuaString} corresponding to the value if a string or number
	 * @throws LuaError if not a string or number
	 */
	public LuaString strvalue() {
		typerror("strValue");
		return null;
	}

	/**
	 * Return the key part of this value if it is a weak table entry, or {@link Constants#NIL} if it was weak and is no longer referenced.
	 *
	 * @return {@link LuaValue} key, or {@link Constants#NIL} if it was weak and is no longer referenced.
	 * @see WeakTable
	 */
	public LuaValue strongkey() {
		return strongvalue();
	}

	/**
	 * Return this value as a strong reference, or {@link Constants#NIL} if it was weak and is no longer referenced.
	 *
	 * @return {@link LuaValue} referred to, or {@link Constants#NIL} if it was weak and is no longer referenced.
	 * @see WeakTable
	 */
	public LuaValue strongvalue() {
		return this;
	}

	/**
	 * Test if this is a weak reference and its value no longer is referenced.
	 *
	 * @return true if this is a weak reference whose value no longer is referenced
	 * @see WeakTable
	 */
	public boolean isweaknil() {
		return false;
	}

	/**
	 * Return value for field reference including metatag processing, or {@link Constants#NIL} if it doesn't exist.
	 *
	 * @param t   {@link LuaValue} on which field is being referenced, typically a table or something with the metatag {@link Constants#INDEX} defined
	 * @param key {@link LuaValue} naming the field to reference
	 * @return {@link LuaValue} for the {@code key} if it exists, or {@link Constants#NIL}
	 * @throws LuaError if there is a loop in metatag processing
	 */
	protected static LuaValue gettable(LuaValue t, LuaValue key) {
		LuaValue tm;
		int loop = 0;
		do {
			if (t.istable()) {
				LuaValue res = t.rawget(key);
				if ((!res.isnil()) || (tm = t.metatag(INDEX)).isnil()) {
					return res;
				}
			} else if ((tm = t.metatag(INDEX)).isnil()) {
				t.indexerror();
			}
			if (tm.isfunction()) {
				return tm.call(t, key);
			}
			t = tm;
		}
		while (++loop < MAXTAGLOOP);
		error("loop in gettable");
		return NIL;
	}

	/**
	 * Perform field assignment including metatag processing.
	 *
	 * @param t     {@link LuaValue} on which value is being set, typically a table or something with the metatag {@link Constants#NEWINDEX} defined
	 * @param key   {@link LuaValue} naming the field to assign
	 * @param value {@link LuaValue} the new value to assign to {@code key}
	 * @return true if assignment or metatag processing succeeded, false otherwise
	 * @throws LuaError if there is a loop in metatag processing
	 */
	protected static boolean settable(LuaValue t, LuaValue key, LuaValue value) {
		LuaValue tm;
		int loop = 0;
		do {
			if (t.istable()) {
				if ((!t.rawget(key).isnil()) || (tm = t.metatag(NEWINDEX)).isnil()) {
					t.rawset(key, value);
					return true;
				}
			} else if ((tm = t.metatag(NEWINDEX)).isnil()) {
				t.typerror("index");
			}
			if (tm.isfunction()) {
				tm.call(t, key, value);
				return true;
			}
			t = tm;
		}
		while (++loop < MAXTAGLOOP);
		error("loop in settable");
		return false;
	}

	/**
	 * Perform metatag processing for comparison operations.
	 * <p>
	 * Finds the supplied metatag value and invokes it,
	 * or throws {@link LuaError} if none applies.
	 *
	 * @param tag The metatag to look up
	 * @param op1 The right-hand-side value to perform the operation with
	 * @return {@link LuaValue} resulting from metatag processing
	 * @throws LuaError if metatag was not defined for either operand,
	 *                  or if the operands are not the same type,
	 *                  or the metatag values for the two operands are different.
	 */
	public LuaValue comparemt(LuaValue tag, LuaValue op1) {
		if (type() == op1.type()) {
			LuaValue h = metatag(tag);
			if (!h.isnil() && h == op1.metatag(tag)) {
				return h.call(this, op1);
			}
		}
		return error("attempt to compare " + tag + " on " + typename() + " and " + op1.typename());
	}

	/**
	 * Perform metatag processing for arithmetic operations.
	 * <p>
	 * Finds the supplied metatag value for {@code this} or {@code op2} and invokes it,
	 * or throws {@link LuaError} if neither is defined.
	 *
	 * @param tag The metatag to look up
	 * @param op2 The other operand value to perform the operation with
	 * @return {@link LuaValue} resulting from metatag processing
	 * @throws LuaError if metatag was not defined for either operand
	 * @see Constants#ADD
	 * @see Constants#SUB
	 * @see Constants#MUL
	 * @see Constants#POW
	 * @see Constants#DIV
	 * @see Constants#MOD
	 */
	protected LuaValue arithmt(LuaValue tag, LuaValue op2) {
		LuaValue h = this.metatag(tag);
		if (h.isnil()) {
			h = op2.metatag(tag);
			if (h.isnil()) {
				error("attempt to perform arithmetic " + tag + " on " + typename() + " and " + op2.typename());
			}
		}
		return h.call(this, op2);
	}

	/**
	 * Get particular metatag, or return {@link Constants#NIL} if it doesn't exist
	 *
	 * @param tag Metatag name to look up, typically a string such as
	 *            {@link Constants#INDEX} or {@link Constants#NEWINDEX}
	 * @return {@link LuaValue} for tag {@code reason}, or  {@link Constants#NIL}
	 */
	public LuaValue metatag(LuaValue tag) {
		LuaValue mt = getmetatable();
		if (mt == null) {
			return NIL;
		}
		return mt.rawget(tag);
	}

	/**
	 * Get particular metatag, or throw {@link LuaError} if it doesn't exist
	 *
	 * @param tag    Metatag name to look up, typically a string such as
	 *               {@link Constants#INDEX} or {@link Constants#NEWINDEX}
	 * @param reason Description of error when tag lookup fails.
	 * @return {@link LuaValue} that can be called
	 * @throws LuaError when the lookup fails.
	 */
	protected LuaValue checkmetatag(LuaValue tag, String reason) {
		LuaValue h = this.metatag(tag);
		if (h.isnil()) {
			throw new LuaError(reason + typename());
		}
		return h;
	}

	/**
	 * Throw {@link LuaError} indicating index was attempted on illegal type
	 *
	 * @throws LuaError when called.
	 */
	private void indexerror() {
		error("attempt to index ? (a " + typename() + " value)");
	}

	/**
	 * Callback used during tail call processing to invoke the function once.
	 * <p>
	 * This may return a {@link TailcallVarargs} to be evaluated by the client.
	 * <p>
	 * This should not be called directly, instead use on of the call invocation functions.
	 *
	 * @param args the arguments to the call invocation.
	 * @return Varargs the return values, possible a TailcallVarargs.
	 * @see LuaValue#call()
	 * @see LuaValue#invoke()
	 * @see LuaValue#method(LuaValue)
	 * @see LuaValue#invokemethod(LuaValue)
	 */
	public Varargs onInvoke(Varargs args) {
		return invoke(args);
	}

	/**
	 * Varargs implemenation backed by an array of LuaValues
	 * <p>
	 * This is an internal class not intended to be used directly.
	 * Instead use the corresponding static methods on LuaValue.
	 *
	 * @see Factory#varargsOf(LuaValue[])
	 * @see Factory#varargsOf(LuaValue[], Varargs)
	 */
	static final class ArrayVarargs extends Varargs {
		private final LuaValue[] v;
		private final Varargs r;

		/**
		 * Construct a Varargs from an array of LuaValue.
		 * <p>
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static methods on LuaValue.
		 *
		 * @see Factory#varargsOf(LuaValue[])
		 * @see Factory#varargsOf(LuaValue[], Varargs)
		 */
		ArrayVarargs(LuaValue[] v, Varargs r) {
			this.v = v;
			this.r = r;
		}

		@Override
		public LuaValue arg(int i) {
			return i >= 1 && i <= v.length ? v[i - 1] : r.arg(i - v.length);
		}

		@Override
		public int narg() {
			return v.length + r.narg();
		}

		@Override
		public LuaValue arg1() {
			return v.length > 0 ? v[0] : r.arg1();
		}
	}

	/**
	 * Varargs implemenation backed by an array of LuaValues
	 * <p>
	 * This is an internal class not intended to be used directly.
	 * Instead use the corresponding static methods on LuaValue.
	 *
	 * @see Factory#varargsOf(LuaValue[], int, int)
	 * @see Factory#varargsOf(LuaValue[], int, int, Varargs)
	 */
	static final class ArrayPartVarargs extends Varargs {
		private final int offset;
		private final LuaValue[] v;
		private final int length;
		private final Varargs more;

		/**
		 * Construct a Varargs from an array of LuaValue.
		 * <p>
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static methods on LuaValue.
		 *
		 * @see Factory#varargsOf(LuaValue[], int, int)
		 */
		ArrayPartVarargs(LuaValue[] v, int offset, int length) {
			this.v = v;
			this.offset = offset;
			this.length = length;
			this.more = NONE;
		}

		/**
		 * Construct a Varargs from an array of LuaValue and additional arguments.
		 * <p>
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static method on LuaValue.
		 *
		 * @see Factory#varargsOf(LuaValue[], int, int, Varargs)
		 */
		public ArrayPartVarargs(LuaValue[] v, int offset, int length, Varargs more) {
			this.v = v;
			this.offset = offset;
			this.length = length;
			this.more = more;
		}

		@Override
		public LuaValue arg(int i) {
			return i >= 1 && i <= length ? v[i + offset - 1] : more.arg(i - length);
		}

		@Override
		public int narg() {
			return length + more.narg();
		}

		@Override
		public LuaValue arg1() {
			return length > 0 ? v[offset] : more.arg1();
		}
	}

	/**
	 * Varargs implemenation backed by two values.
	 * <p>
	 * This is an internal class not intended to be used directly.
	 * Instead use the corresponding static method on LuaValue.
	 *
	 * @see Factory#varargsOf(LuaValue, Varargs)
	 */
	static final class PairVarargs extends Varargs {
		private final LuaValue v1;
		private final Varargs v2;

		/**
		 * Construct a Varargs from an two LuaValue.
		 * <p>
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static method on LuaValue.
		 *
		 * @param v1 First argument
		 * @param v2 Remaining arguments
		 * @see Factory#varargsOf(LuaValue, Varargs)
		 */
		PairVarargs(LuaValue v1, Varargs v2) {
			this.v1 = v1;
			this.v2 = v2;
		}

		@Override
		public LuaValue arg(int i) {
			return i == 1 ? v1 : v2.arg(i - 1);
		}

		@Override
		public int narg() {
			return 1 + v2.narg();
		}

		@Override
		public LuaValue arg1() {
			return v1;
		}
	}

	//region Legacy comparison
	public final LuaValue eq(LuaValue other) {
		return OperationHelper.eq(this, other) ? TRUE : FALSE;
	}

	public final boolean eq_b(LuaValue other) {
		return OperationHelper.eq(this, other);
	}

	public final LuaValue lt(LuaValue other) {
		return OperationHelper.ltValue(this, other);
	}

	public final boolean lt_b(LuaValue other) {
		return OperationHelper.lt(this, other);
	}

	public final LuaValue lteq(LuaValue other) {
		return OperationHelper.leValue(this, other);
	}

	public final boolean lteq_b(LuaValue other) {
		return OperationHelper.le(this, other);
	}

	public final LuaValue neq(LuaValue other) {
		return OperationHelper.eq(this, other) ? FALSE : TRUE;
	}

	public final boolean neq_b(LuaValue other) {
		return !OperationHelper.eq(this, other);
	}

	public final LuaValue gt(LuaValue other) {
		return OperationHelper.ltValue(other, this);
	}

	public final boolean gt_b(LuaValue other) {
		return OperationHelper.lt(other, this);
	}

	public final LuaValue gteq(LuaValue other) {
		return OperationHelper.leValue(other, this);
	}

	public final boolean gteq_b(LuaValue other) {
		return OperationHelper.le(other, this);
	}

	public final LuaValue add(LuaValue other) {
		return OperationHelper.add(this, other);
	}

	public final LuaValue sub(LuaValue other) {
		return OperationHelper.sub(this, other);
	}

	public final LuaValue mul(LuaValue other) {
		return OperationHelper.mul(this, other);
	}

	public final LuaValue div(LuaValue other) {
		return OperationHelper.div(this, other);
	}

	public final LuaValue mod(LuaValue other) {
		return OperationHelper.mod(this, other);
	}

	public final LuaValue pow(LuaValue other) {
		return OperationHelper.pow(this, other);
	}
	//endregion
}
