/*
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
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
package org.squiddev.cobalt;

import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import static org.squiddev.cobalt.Constants.*;

/**
 * Base class for all concrete lua type values.
 *
 * Establishes base implementations for all the operations on lua types.
 * This allows Java clients to deal essentially with one type for all Java values, namely {@link LuaValue}.
 *
 * Constructors are provided as static methods for common Java types, such as
 * {@link ValueFactory#valueOf(int)} or {@link ValueFactory#valueOf(String)}
 * to allow for instance pooling.
 *
 * Constants are defined for the lua values
 * {@link Constants#NIL}, {@link Constants#TRUE}, and {@link Constants#FALSE}.
 * A constant {@link Constants#NONE} is defined which is a {@link Varargs} list having no values.
 *
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
 *
 * Field access and function calls are similar, with common overloads to simplify Java usage:
 * <pre> {@code
 * LuaValue globals = JsePlatform.standardGlobals();
 * LuaValue sqrt = globals.get("math").get("sqrt");
 * LuaValue print = globals.get("print");
 * LuaValue d = sqrt.call( a );
 * print.call( LuaValue.valueOf("sqrt(5):"), a );
 * } </pre>
 *
 * To supply variable arguments or get multiple return values, use
 * {@link #invoke(LuaState, Varargs)} or {@link #invokeMethod(LuaState, LuaValue, Varargs)} methods:
 * <pre> {@code
 * LuaValue modf = globals.get("math").get("modf");
 * Varargs r = modf.invoke( d );
 * print.call( r.arg(1), r.arg(2) );
 * } </pre>
 *
 * To load and run a script, {@link LoadState} is used:
 * <pre> {@code
 * LoadState.load(new FileInputStream("main.lua"), "main.lua", globals ).call();
 * } </pre>
 *
 * although {@code require} could also be used:
 * <pre> {@code
 * globals.get("require").call(LuaValue.valueOf("main"));
 * } </pre>
 * For this to work the file must be in the current directory, or in the class path,
 * depending on the platform.
 * See {@link JsePlatform} for details.
 *
 * In general a {@link LuaError} may be thrown on any operation when the
 * types supplied to any operation are illegal from a lua perspective.
 * Examples could be attempting to concatenate a NIL value, or attempting arithmetic
 * on values that are not number.
 *
 * @see JsePlatform
 * @see LoadState
 * @see Varargs
 */
public abstract class LuaValue extends Varargs {
	private final int type;

	protected LuaValue(int type) {
		this.type = type;
	}

	//region Type checking

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
	 * @see #typeName()
	 */
	public final int type() {
		return type;
	}

	/**
	 * Get the String name of the type of this value.
	 *
	 * @return name from type name list {@link Constants#TYPE_NAMES}
	 * corresponding to the type of this value:
	 * "nil", "boolean", "number", "string",
	 * "table", "function", "userdata", "thread"
	 * @see #type()
	 */
	public final String typeName() {
		if (type >= 0) {
			return Constants.TYPE_NAMES[type];
		} else {
			throw ErrorFactory.illegal("type", "cannot get type of " + this);
		}
	}

	/**
	 * Check if {@code this} is a {@code boolean}
	 *
	 * @return true if this is a {@code boolean}, otherwise false
	 * @see #isBoolean()
	 * @see #toBoolean()
	 * @see #checkBoolean()
	 * @see #optBoolean(boolean)
	 * @see Constants#TBOOLEAN
	 */
	public final boolean isBoolean() {
		return type == TBOOLEAN;
	}

	/**
	 * Check if {@code this} is a {@code function} that is a closure,
	 * meaning interprets lua bytecode for its execution
	 *
	 * @return true if this is a {@code closure}, otherwise false
	 * @see #isFunction()
	 * @see #checkClosure()
	 * @see #optClosure(LuaClosure)
	 * @see Constants#TFUNCTION
	 */
	public boolean isClosure() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@code function}
	 *
	 * @return true if this is a {@code function}, otherwise false
	 * @see #isClosure()
	 * @see #checkFunction()
	 * @see #optFunction(LuaFunction)
	 * @see Constants#TFUNCTION
	 */
	public final boolean isFunction() {
		return type == TFUNCTION;
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
	 * @see #isIntExact()
	 * @see #isLong()
	 * @see #toNumber()
	 * @see #checkInteger()
	 * @see #optInteger(int)
	 * @see Constants#TNUMBER
	 */
	public boolean isInteger() {
		return false;
	}

	/**
	 * Check if {@code this} is a {@link LuaInteger}
	 *
	 * No attempt to convert from string will be made by this call.
	 *
	 * @return true if this is a {@code LuaInteger},
	 * otherwise false
	 * @see #isInteger()
	 * @see #isNumber()
	 * @see #toNumber()
	 * @see Constants#TNUMBER
	 */
	public boolean isIntExact() {
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
	 * @see #toNumber()
	 * @see #checkLong()
	 * @see #optLong(long)
	 * @see Constants#TNUMBER
	 */
	public boolean isLong() {
		return false;
	}

	/**
	 * Check if {@code this} is {@code nil}
	 *
	 * @return true if this is {@code nil}, otherwise false
	 * @see Constants#NIL
	 * @see Constants#NONE
	 * @see #checkNotNil()
	 * @see #optValue(LuaValue)
	 * @see Varargs#isNoneOrNil(int)
	 * @see Constants#TNIL
	 * @see Constants#TNONE
	 */
	public final boolean isNil() {
		return type == TNIL;
	}

	/**
	 * Check if {@code this} is a {@code number}
	 *
	 * @return true if this is a {@code number},
	 * meaning derives from {@link LuaNumber}
	 * or derives from {@link LuaString} and is convertible to a number,
	 * otherwise false
	 * @see #toNumber()
	 * @see #checkNumber()
	 * @see #optNumber(LuaNumber)
	 * @see Constants#TNUMBER
	 */
	public boolean isNumber() {
		return false;
	} // may convert from string

	/**
	 * Check if {@code this} is a {@code string}
	 *
	 * @return true if this is a {@code string},
	 * meaning derives from {@link LuaString} or {@link LuaNumber},
	 * otherwise false
	 * @see #toLuaString()
	 * @see #checkLuaString()
	 * @see #optLuaString(LuaString)
	 * @see Constants#TSTRING
	 */
	public final boolean isString() {
		return type == TSTRING || type == TNUMBER;
	}

	/**
	 * Check if {@code this} is a {@code thread}
	 *
	 * @return true if this is a {@code thread}, otherwise false
	 * @see #checkThread()
	 * @see #optThread(LuaThread)
	 * @see Constants#TTHREAD
	 */
	public final boolean isThread() {
		return type == TTHREAD;
	}

	/**
	 * Check if {@code this} is a {@code table}
	 *
	 * @return true if this is a {@code table}, otherwise false
	 * @see #checkTable()
	 * @see #optTable(LuaTable)
	 * @see Constants#TTABLE
	 */
	public final boolean isTable() {
		return type == TTABLE;
	}

	/**
	 * Check if {@code this} is a {@code userdata}
	 *
	 * @return true if this is a {@code userdata}, otherwise false
	 * @see #isUserdata(Class)
	 * @see #toUserdata()
	 * @see #checkUserdata()
	 * @see #optUserdata(Object)
	 * @see Constants#TUSERDATA
	 */
	public final boolean isUserdata() {
		return type == TUSERDATA;
	}

	/**
	 * Check if {@code this} is a {@code userdata} of type {@code c}
	 *
	 * @param c Class to test instance against
	 * @return true if this is a {@code userdata}
	 * and the instance is assignable to {@code c},
	 * otherwise false
	 * @see #isUserdata()
	 * @see #toUserdata(Class)
	 * @see #checkUserdata(Class)
	 * @see #optUserdata(Class, Object)
	 * @see Constants#TUSERDATA
	 */
	public boolean isUserdata(Class<?> c) {
		return false;
	}

	/**
	 * Convert to boolean false if {@link Constants#NIL} or {@link Constants#FALSE}, true if anything else
	 *
	 * @return Value cast to byte if number or string convertible to number, otherwise 0
	 * @see #optBoolean(boolean)
	 * @see #checkBoolean()
	 * @see #isBoolean()
	 * @see Constants#TBOOLEAN
	 */
	public boolean toBoolean() {
		return true;
	}

	/**
	 * Convert to double if numeric, or 0 if not.
	 *
	 * @return Value cast to double if number or string convertible to number, otherwise 0
	 * @see #toInteger()
	 * @see #toLong()
	 * @see #optDouble(double)
	 * @see #checkNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public double toDouble() {
		return 0;
	}

	/**
	 * Convert to int if numeric, or 0 if not.
	 *
	 * @return Value cast to int if number or string convertible to number, otherwise 0
	 * @see #toLong()
	 * @see #toDouble()
	 * @see #optInteger(int)
	 * @see #checkNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public int toInteger() {
		return 0;
	}

	/**
	 * Convert to long if numeric, or 0 if not.
	 *
	 * @return Value cast to long if number or string convertible to number, otherwise 0
	 * @see #isInteger()
	 * @see #isIntExact()
	 * @see #toInteger()
	 * @see #toDouble()
	 * @see #optLong(long)
	 * @see #checkNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public long toLong() {
		return 0;
	}

	/**
	 * Convert to human readable String for any type.
	 *
	 * @return String for use by human readers based on type.
	 * @see #toLuaString()
	 * @see #optString(String)
	 * @see #checkString()
	 * @see #isString()
	 * @see Constants#TSTRING
	 */
	@Override
	public String toString() {
		return typeName() + ": " + Integer.toHexString(hashCode());
	}

	/**
	 * Convert to userdata instance, or null.
	 *
	 * @return userdata instance if userdata, or null if not {@link LuaUserdata}
	 * @see #optUserdata(Object)
	 * @see #checkUserdata()
	 * @see #isUserdata()
	 * @see Constants#TUSERDATA
	 */
	public Object toUserdata() {
		return null;
	}

	/**
	 * Convert to userdata instance if specific type, or null.
	 *
	 * @param c Use this class to create userdata
	 * @return userdata instance if is a userdata whose instance derives from {@code c},
	 * or null if not {@link LuaUserdata}
	 * @see #optUserdata(Class, Object)
	 * @see #checkUserdata(Class)
	 * @see #isUserdata(Class)
	 * @see Constants#TUSERDATA
	 */
	public Object toUserdata(Class<?> c) {
		return null;
	}

	/**
	 * Conditionally convert to lua number without throwing errors.
	 *
	 * In lua all numbers are strings, but not all strings are numbers.
	 * This function will return
	 * the {@link LuaValue} {@code this} if it is a number
	 * or a string convertible to a number,
	 * and {@link Constants#NIL} for all other cases.
	 *
	 * This allows values to be tested for their "numeric-ness" without
	 * the penalty of throwing exceptions,
	 * nor the cost of converting the type and creating storage for it.
	 *
	 * @return {@code this} if it is a {@link LuaNumber}
	 * or {@link LuaString} that can be converted to a number,
	 * otherwise {@link Constants#NIL}
	 * @see #toLuaString()
	 * @see #optNumber(LuaNumber)
	 * @see #checkNumber()
	 * @see #toInteger()
	 * @see #toDouble()
	 */
	public LuaValue toNumber() {
		return Constants.NIL;
	}

	/**
	 * Conditionally convert to lua string without throwing errors.
	 *
	 * In lua all numbers are strings, so this function will return
	 * the {@link LuaValue} {@code this} if it is a string or number,
	 * and {@link Constants#NIL} for all other cases.
	 *
	 * This allows values to be tested for their "string-ness" without
	 * the penalty of throwing exceptions.
	 *
	 * @return {@code this} if it is a {@link LuaString} or {@link LuaNumber},
	 * otherwise {@link Constants#NIL}
	 * @see #toNumber()
	 * @see #toString()
	 * @see #optLuaString(LuaString)
	 * @see #checkLuaString()
	 * @see #toString()
	 */
	public LuaValue toLuaString() {
		return Constants.NIL;
	}

	/**
	 * Check that optional argument is a boolean and return its boolean value
	 *
	 * @param defval boolean value to return if {@code this} is nil or none
	 * @return {@code this} cast to boolean if a {@link LuaBoolean},
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not a boolean or nil or none.
	 * @see #checkBoolean()
	 * @see #isBoolean()
	 * @see Constants#TBOOLEAN
	 */
	public boolean optBoolean(boolean defval) {
		throw ErrorFactory.argError(this, "boolean");
	}

	/**
	 * Check that optional argument is a closure and return as {@link LuaClosure}
	 *
	 * A {@link LuaClosure} is a {@link LuaFunction} that executes lua byteccode.
	 *
	 * @param defval {@link LuaClosure} to return if {@code this} is nil or none
	 * @return {@code this} cast to {@link LuaClosure} if a function,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not a closure or nil or none.
	 * @see #checkClosure()
	 * @see #isClosure()
	 * @see Constants#TFUNCTION
	 */
	public LuaClosure optClosure(LuaClosure defval) {
		throw ErrorFactory.argError(this, "closure");
	}

	/**
	 * Check that optional argument is a number or string convertible to number and return as double
	 *
	 * @param defval double to return if {@code this} is nil or none
	 * @return {@code this} cast to double if numeric,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not numeric or nil or none.
	 * @see #optInteger(int)
	 * @see #optLuaInteger(LuaInteger)
	 * @see #checkDouble()
	 * @see #toDouble()
	 * @see #toNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public double optDouble(double defval) {
		throw ErrorFactory.argError(this, "number");
	}

	/**
	 * Check that optional argument is a function and return as {@link LuaFunction}
	 *
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
	 * @see #checkFunction()
	 * @see #isFunction()
	 * @see Constants#TFUNCTION
	 */
	public LuaFunction optFunction(LuaFunction defval) {
		throw ErrorFactory.argError(this, "function");
	}

	/**
	 * Check that optional argument is a number or string convertible to number and return as int
	 *
	 * @param defval int to return if {@code this} is nil or none
	 * @return {@code this} cast to int if numeric,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not numeric or nil or none.
	 * @see #optDouble(double)
	 * @see #optLong(long)
	 * @see #optLuaInteger(LuaInteger)
	 * @see #checkInteger()
	 * @see #toInteger()
	 * @see #toNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public int optInteger(int defval) {
		throw ErrorFactory.argError(this, "integer");
	}

	/**
	 * Check that optional argument is a number or string convertible to number and return as {@link LuaInteger}
	 *
	 * @param defval {@link LuaInteger} to return if {@code this} is nil or none
	 * @return {@code this} converted and wrapped in {@link LuaInteger} if numeric,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not numeric or nil or none.
	 * @see #optDouble(double)
	 * @see #optInteger(int)
	 * @see #checkInteger()
	 * @see #toInteger()
	 * @see #toNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public LuaInteger optLuaInteger(LuaInteger defval) {
		throw ErrorFactory.argError(this, "integer");
	}

	/**
	 * Check that optional argument is a number or string convertible to number and return as long
	 *
	 * @param defval long to return if {@code this} is nil or none
	 * @return {@code this} cast to long if numeric,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not numeric or nil or none.
	 * @see #optDouble(double)
	 * @see #optInteger(int)
	 * @see #checkInteger()
	 * @see #toInteger()
	 * @see #toNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public long optLong(long defval) {
		throw ErrorFactory.argError(this, "integer");
	}

	/**
	 * Check that optional argument is a number or string convertible to number and return as {@link LuaNumber}
	 *
	 * @param defval {@link LuaNumber} to return if {@code this} is nil or none
	 * @return {@code this} cast to {@link LuaNumber} if numeric,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} otherwise
	 * @throws LuaError if was not numeric or nil or none.
	 * @see #optDouble(double)
	 * @see #optLong(long)
	 * @see #optInteger(int)
	 * @see #checkInteger()
	 * @see #toInteger()
	 * @see #toNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public LuaNumber optNumber(LuaNumber defval) {
		throw ErrorFactory.argError(this, "number");
	}

	/**
	 * Check that optional argument is a string or number and return as Java String
	 *
	 * @param defval {@link LuaString} to return if {@code this} is nil or none
	 * @return {@code this} converted to String if a string or number,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a string or number or nil or none.
	 * @see #toString()
	 * @see #optLuaString(LuaString)
	 * @see #checkString()
	 * @see #toString()
	 * @see Constants#TSTRING
	 */
	public String optString(String defval) {
		throw ErrorFactory.argError(this, "string");
	}

	/**
	 * Check that optional argument is a string or number and return as {@link LuaString}
	 *
	 * @param defval {@link LuaString} to return if {@code this} is nil or none
	 * @return {@code this} converted to {@link LuaString} if a string or number,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a string or number or nil or none.
	 * @see #toString()
	 * @see #optString(String)
	 * @see #checkLuaString()
	 * @see #toString()
	 * @see Constants#TSTRING
	 */
	public LuaString optLuaString(LuaString defval) {
		throw ErrorFactory.argError(this, "string");
	}

	/**
	 * Check that optional argument is a table and return as {@link LuaTable}
	 *
	 * @param defval {@link LuaTable} to return if {@code this} is nil or none
	 * @return {@code this} cast to {@link LuaTable} if a table,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a table or nil or none.
	 * @see #checkTable()
	 * @see #isTable()
	 * @see Constants#TTABLE
	 */
	public LuaTable optTable(LuaTable defval) {
		throw ErrorFactory.argError(this, "table");
	}

	/**
	 * Check that optional argument is a thread and return as {@link LuaThread}
	 *
	 * @param defval {@link LuaThread} to return if {@code this} is nil or none
	 * @return {@code this} cast to {@link LuaTable} if a thread,
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a thread or nil or none.
	 * @see #checkThread()
	 * @see #isThread()
	 * @see Constants#TTHREAD
	 */
	public LuaThread optThread(LuaThread defval) {
		throw ErrorFactory.argError(this, "thread");
	}

	/**
	 * Check that optional argument is a userdata and return the Object instance
	 *
	 * @param defval Object to return if {@code this} is nil or none
	 * @return Object instance of the userdata if a {@link LuaUserdata},
	 * {@code defval} if nil or none,
	 * throws {@link LuaError} if some other type
	 * @throws LuaError if was not a userdata or nil or none.
	 * @see #checkUserdata()
	 * @see #isUserdata()
	 * @see #optUserdata(Class, Object)
	 * @see Constants#TUSERDATA
	 */
	public Object optUserdata(Object defval) {
		throw ErrorFactory.argError(this, "object");
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
	 * @see #checkUserdata(Class)
	 * @see #isUserdata(Class)
	 * @see #optUserdata(Object)
	 * @see Constants#TUSERDATA
	 */
	public Object optUserdata(Class<?> c, Object defval) {
		throw ErrorFactory.argError(this, c.getName());
	}

	/**
	 * Perform argument check that this is not nil or none.
	 *
	 * @param defval {@link LuaValue} to return if {@code this} is nil or none
	 * @return {@code this} if not nil or none, else {@code defval}
	 * @see Constants#NIL
	 * @see Constants#NONE
	 * @see #isNil()
	 * @see Varargs#isNoneOrNil(int)
	 * @see Constants#TNIL
	 * @see Constants#TNONE
	 */
	public LuaValue optValue(LuaValue defval) {
		return this;
	}


	/**
	 * Check that the value is a {@link LuaBoolean},
	 * or throw {@link LuaError} if not
	 *
	 * @return boolean value for {@code this} if it is a {@link LuaBoolean}
	 * @throws LuaError if not a {@link LuaBoolean}
	 * @see #optBoolean(boolean)
	 * @see Constants#TBOOLEAN
	 */
	public boolean checkBoolean() {
		throw ErrorFactory.argError(this, "boolean");
	}

	/**
	 * Check that the value is a {@link LuaClosure} ,
	 * or throw {@link LuaError} if not
	 *
	 * {@link LuaClosure} is a subclass of {@link LuaFunction} that interprets lua bytecode.
	 *
	 * @return {@code this} cast as {@link LuaClosure}
	 * @throws LuaError if not a {@link LuaClosure}
	 * @see #checkFunction()
	 * @see #optClosure(LuaClosure)
	 * @see #isClosure()
	 * @see Constants#TFUNCTION
	 */
	public LuaClosure checkClosure() {
		throw ErrorFactory.argError(this, "closure");
	}

	/**
	 * Check this is a number or raise an arithmetic error
	 *
	 * @return The number
	 * @throws LuaError if not a number
	 * @see #checkDouble()
	 */
	public double checkArith() {
		throw ErrorFactory.arithError(this);
	}

	/**
	 * Check that the value is numeric and return the value as a double,
	 * or throw {@link LuaError} if not numeric
	 *
	 * Values that are {@link LuaNumber} and values that are {@link LuaString}
	 * that can be converted to a number will be converted to double.
	 *
	 * @return value cast to a double if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkInteger()
	 * @see #checkLuaInteger()
	 * @see #checkLong()
	 * @see #optDouble(double)
	 * @see Constants#TNUMBER
	 */
	public double checkDouble() {
		throw ErrorFactory.argError(this, "number");
	}

	/**
	 * Check that the value is a function , or throw {@link LuaError} if not
	 *
	 * A function is considered anything whose {@link #type()} returns {@link Constants#TFUNCTION}.
	 * In practice it will be either a built-in Java function, typically deriving from
	 * {@link LuaFunction} or a {@link LuaClosure} which represents lua source compiled
	 * into lua bytecode.
	 *
	 * @return {@code this} if if a lua function or closure
	 * @throws LuaError if not a function
	 * @see #checkClosure()
	 */
	public LuaFunction checkFunction() {
		throw ErrorFactory.argError(this, "function");
	}

	/**
	 * Check that the value is numeric, and convert and cast value to int, or throw {@link LuaError} if not numeric
	 *
	 * Values that are {@link LuaNumber} will be cast to int and may lose precision.
	 * Values that are {@link LuaString} that can be converted to a number will be converted,
	 * then cast to int, so may also lose precision.
	 *
	 * @return value cast to a int if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkLuaInteger()
	 * @see #checkLong()
	 * @see #checkDouble()
	 * @see #optInteger(int)
	 * @see Constants#TNUMBER
	 */
	public int checkInteger() {
		throw ErrorFactory.argError(this, "integer");
	}

	/**
	 * Check that the value is numeric, and convert and cast value to int, or throw {@link LuaError} if not numeric
	 *
	 * Values that are {@link LuaNumber} will be cast to int and may lose precision.
	 * Values that are {@link LuaString} that can be converted to a number will be converted,
	 * then cast to int, so may also lose precision.
	 *
	 * @return value cast to a int and wrapped in {@link LuaInteger} if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkInteger()
	 * @see #checkLong()
	 * @see #checkDouble()
	 * @see #optLuaInteger(LuaInteger)
	 * @see Constants#TNUMBER
	 */
	public LuaInteger checkLuaInteger() {
		throw ErrorFactory.argError(this, "integer");
	}

	/**
	 * Check that the value is numeric, and convert and cast value to long, or throw {@link LuaError} if not numeric
	 *
	 * Values that are {@link LuaNumber} will be cast to long and may lose precision.
	 * Values that are {@link LuaString} that can be converted to a number will be converted,
	 * then cast to long, so may also lose precision.
	 *
	 * @return value cast to a long if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkInteger()
	 * @see #checkLuaInteger()
	 * @see #checkDouble()
	 * @see #optLong(long)
	 * @see Constants#TNUMBER
	 */
	public long checkLong() {
		throw ErrorFactory.argError(this, "integer");
	}

	/**
	 * Check that the value is numeric, and return as a LuaNumber if so, or throw {@link LuaError}
	 *
	 * Values that are {@link LuaString} that can be converted to a number will be converted and returned.
	 *
	 * @return value as a {@link LuaNumber} if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkInteger()
	 * @see #checkLuaInteger()
	 * @see #checkDouble()
	 * @see #checkLong()
	 * @see #optNumber(LuaNumber)
	 * @see Constants#TNUMBER
	 */
	public LuaNumber checkNumber() {
		throw ErrorFactory.argError(this, "number");
	}

	/**
	 * Check that the value is numeric, and return as a LuaNumber if so, or throw {@link LuaError}
	 *
	 * Values that are {@link LuaString} that can be converted to a number will be converted and returned.
	 *
	 * @param msg String message to supply if conversion fails
	 * @return value as a {@link LuaNumber} if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkInteger()
	 * @see #checkLuaInteger()
	 * @see #checkDouble()
	 * @see #checkLong()
	 * @see #optNumber(LuaNumber)
	 * @see Constants#TNUMBER
	 */
	public LuaNumber checkNumber(String msg) {
		throw new LuaError(msg);
	}

	/**
	 * Convert this value to a Java String.
	 *
	 * The string representations here will roughly match what is produced by the
	 * C lua distribution, however hash codes have no relationship,
	 * and there may be differences in number formatting.
	 *
	 * @return String representation of the value
	 * @see #checkLuaString()
	 * @see #optString(String)
	 * @see #toString()
	 * @see #isString
	 * @see Constants#TSTRING
	 */
	public String checkString() {
		throw ErrorFactory.argError(this, "string");
	}

	/**
	 * Check that this is a lua string, or throw {@link LuaError} if it is not.
	 *
	 * In lua all numbers are strings, so this will succeed for
	 * anything that derives from {@link LuaString} or {@link LuaNumber}.
	 * Numbers will be converted to {@link LuaString}.
	 *
	 * @return {@link LuaString} representation of the value if it is a {@link LuaString} or {@link LuaNumber}
	 * @throws LuaError if {@code this} is not a {@link LuaTable}
	 * @see #checkString()
	 * @see #optLuaString(LuaString)
	 * @see #toLuaString()
	 * @see #isString()
	 * @see Constants#TSTRING
	 */
	public LuaString checkLuaString() {
		throw ErrorFactory.argError(this, "string");
	}

	/**
	 * Check that this is a {@link LuaTable}, or throw {@link LuaError} if it is not
	 *
	 * @return {@code this} if it is a {@link LuaTable}
	 * @throws LuaError if {@code this} is not a {@link LuaTable}
	 * @see #isTable()
	 * @see #optTable(LuaTable)
	 * @see Constants#TTABLE
	 */
	public LuaTable checkTable() {
		throw ErrorFactory.argError(this, "table");
	}

	/**
	 * Check that this is a {@link LuaThread}, or throw {@link LuaError} if it is not
	 *
	 * @return {@code this} if it is a {@link LuaThread}
	 * @throws LuaError if {@code this} is not a {@link LuaThread}
	 * @see #isThread()
	 * @see #optThread(LuaThread)
	 * @see Constants#TTHREAD
	 */
	public LuaThread checkThread() {
		throw ErrorFactory.argError(this, "thread");
	}

	/**
	 * Check that this is a {@link LuaUserdata}, or throw {@link LuaError} if it is not
	 *
	 * @return {@code this} if it is a {@link LuaUserdata}
	 * @throws LuaError if {@code this} is not a {@link LuaUserdata}
	 * @see #isUserdata()
	 * @see #optUserdata(Object)
	 * @see #checkUserdata(Class)
	 * @see Constants#TUSERDATA
	 */
	public Object checkUserdata() {
		throw ErrorFactory.argError(this, "userdata");
	}

	/**
	 * Check that this is a {@link LuaUserdata}, or throw {@link LuaError} if it is not
	 *
	 * @param c The class of userdata to convert to
	 * @return {@code this} if it is a {@link LuaUserdata}
	 * @throws LuaError if {@code this} is not a {@link LuaUserdata}
	 * @see #isUserdata(Class)
	 * @see #optUserdata(Class, Object)
	 * @see #checkUserdata()
	 * @see Constants#TUSERDATA
	 */
	public Object checkUserdata(Class<?> c) {
		throw ErrorFactory.argError(this, "userdata");
	}

	/**
	 * Check that this is not the value {@link Constants#NIL}, or throw {@link LuaError} if it is
	 *
	 * @return {@code this} if it is not {@link Constants#NIL}
	 * @throws LuaError if {@code this} is {@link Constants#NIL}
	 * @see #optValue(LuaValue)
	 */
	public LuaValue checkNotNil() {
		return this;
	}

	/**
	 * Check that this is a valid key in a table index operation, or throw {@link LuaError} if not
	 *
	 * @return {@code this} if valid as a table key
	 * @throws LuaError if not valid as a table key
	 * @see #isNil()
	 * @see #isIntExact()
	 */
	public LuaValue checkValidKey() {
		return this;
	}
	//endregion

	/**
	 * Get a value in a table including metatag processing using {@link Constants#INDEX}.
	 *
	 * @param state The current lua state
	 * @param key   the key to look up, must not be {@link Constants#NIL} or null
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found and no metatag
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#INDEX} metatag,
	 *                  or key is {@link Constants#NIL}
	 * @see #get(LuaState, int)
	 * @see #get(LuaState, String)
	 * @see #rawget(LuaValue)
	 */
	public LuaValue get(LuaState state, LuaValue key) {
		return getTable(state, this, key);
	}

	/**
	 * Get a value in a table including metatag processing using {@link Constants#INDEX}.
	 *
	 * @param state The current lua state
	 * @param key   the key to look up
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#INDEX} metatag
	 * @see #get(LuaState, LuaValue)
	 * @see #rawget(int)
	 */
	public LuaValue get(LuaState state, int key) {
		return get(state, LuaInteger.valueOf(key));
	}

	/**
	 * Get a value in a table including metatag processing using {@link Constants#INDEX}.
	 *
	 * @param state The current lua state
	 * @param key   the key to look up, must not be null
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#INDEX} metatag
	 * @see #get(LuaState, LuaValue)
	 * @see #rawget(String)
	 */
	public LuaValue get(LuaState state, String key) {
		return get(state, ValueFactory.valueOf(key));
	}

	/**
	 * Set a value in a table without metatag processing using {@link Constants#NEWINDEX}.
	 *
	 * @param state The current lua state
	 * @param key   the key to use, must not be {@link Constants#NIL} or null
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table,
	 *                  or key is {@link Constants#NIL},
	 *                  or there is no {@link Constants#NEWINDEX} metatag
	 */
	public void set(LuaState state, LuaValue key, LuaValue value) {
		setTable(state, this, key, value);
	}

	/**
	 * Set a value in a table without metatag processing using {@link Constants#NEWINDEX}.
	 *
	 * @param state The current lua state
	 * @param key   the key to use
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#NEWINDEX} metatag
	 */
	public void set(LuaState state, int key, LuaValue value) {
		set(state, LuaInteger.valueOf(key), value);
	}

	/**
	 * Set a value in a table without metatag processing using {@link Constants#NEWINDEX}.
	 *
	 * @param state The current lua state
	 * @param key   the key to use, must not be {@link Constants#NIL} or null
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table,
	 *                  or there is no {@link Constants#NEWINDEX} metatag
	 */
	public void set(LuaState state, String key, LuaValue value) {
		set(state, ValueFactory.valueOf(key), value);
	}

	/**
	 * Get a value in a table without metatag processing.
	 *
	 * @param key the key to look up, must not be {@link Constants#NIL} or null
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 * @throws LuaError if {@code this} is not a table, or key is {@link Constants#NIL}
	 */
	public LuaValue rawget(LuaValue key) {
		throw ErrorFactory.unimplemented(this, "rawget");
	}

	/**
	 * Get a value in a table without metatag processing.
	 *
	 * @param key the key to look up
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 * @throws LuaError if {@code this} is not a table
	 */
	public LuaValue rawget(int key) {
		return rawget(ValueFactory.valueOf(key));
	}

	/**
	 * Get a value in a table without metatag processing.
	 *
	 * @param key the key to look up, must not be null
	 * @return {@link LuaValue} for that key, or {@link Constants#NIL} if not found
	 * @throws LuaError if {@code this} is not a table
	 */
	public LuaValue rawget(String key) {
		return rawget(ValueFactory.valueOf(key));
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use, must not be {@link Constants#NIL} or null
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table, or key is {@link Constants#NIL}
	 */
	public void rawset(LuaValue key, LuaValue value) {
		throw ErrorFactory.unimplemented(this, "rawset");
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table
	 */
	public void rawset(int key, LuaValue value) {
		rawset(ValueFactory.valueOf(key), value);
	}

	/**
	 * Set a value in a table without metatag processing.
	 *
	 * @param key   the key to use, must not be null
	 * @param value the value to use, can be {@link Constants#NIL}, must not be null
	 * @throws LuaError if {@code this} is not a table
	 */
	public void rawset(String key, LuaValue value) {
		rawset(ValueFactory.valueOf(key), value);
	}

	/**
	 * Preallocate the array part of a table to be a certain size,
	 *
	 * Primarily used internally in response to a SETLIST bytecode.
	 *
	 * @param i the number of array slots to preallocate in the table.
	 * @throws LuaError if this is not a table.
	 */
	public void presize(int i) {
		throw ErrorFactory.typeError(this, "table");
	}

	/**
	 * Find the next key,value pair if {@code this} is a table,
	 * return {@link Constants#NIL} if there are no more, or throw a {@link LuaError} if not a table.
	 *
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
	 * @see ValueFactory#valueOf(int)
	 * @see Varargs#first()
	 * @see Varargs#arg(int)
	 * @see #isNil()
	 */
	public Varargs next(LuaValue index) {
		throw ErrorFactory.typeError(this, "table");
	}

	/**
	 * Find the next integer-key,value pair if {@code this} is a table,
	 * return {@link Constants#NIL} if there are no more, or throw a {@link LuaError} if not a table.
	 *
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
	 * @see ValueFactory#valueOf(int)
	 * @see Varargs#first()
	 * @see Varargs#arg(int)
	 * @see #isNil()
	 */
	public Varargs inext(LuaValue index) {
		throw ErrorFactory.typeError(this, "table");
	}

	/**
	 * Load a library instance by setting its environment to {@code this}
	 * and calling it, which should iniitalize the library instance and
	 * install itself into this instance.
	 *
	 * @param state   The current lua state
	 * @param library The callable {@link LuaValue} to load into {@code this}
	 * @return {@link LuaValue} containing the result of the initialization call.
	 */
	public LuaValue load(LuaState state, LuaValue library) {
		library.setfenv(this);
		return library.call(state);
	}

	// varargs references
	@Override
	public LuaValue arg(int index) {
		return index == 1 ? this : Constants.NIL;
	}

	@Override
	public int count() {
		return 1;
	}

	@Override
	public LuaValue first() {
		return this;
	}

	/**
	 * Get the metatable for this {@link LuaValue}
	 *
	 * For {@link LuaTable} and {@link LuaUserdata} instances,
	 * the metatable returned is this instance metatable.
	 * For all other types, the class metatable value will be returned.
	 *
	 * @param state The current lua state
	 * @return metatable, or null if it there is none
	 */
	public LuaValue getMetatable(LuaState state) {
		return null;
	}

	/**
	 * Set the metatable for this {@link LuaValue}
	 *
	 * For {@link LuaTable} and {@link LuaUserdata} instances, the metatable is per instance.
	 * For all other types, there is one metatable per type that can be set directly from java
	 *
	 * @param state     The current lua state
	 * @param metatable {@link LuaValue} instance to serve as the metatable, or null to reset it.
	 * @return {@code this} to allow chaining of Java function calls
	 */
	public LuaValue setMetatable(LuaState state, LuaValue metatable) {
		throw ErrorFactory.argError(this, "table");
	}

	/**
	 * Get the environemnt for an instance.
	 *
	 * @return {@link LuaValue} currently set as the instances environent.
	 */
	public LuaValue getfenv() {
		throw ErrorFactory.typeError(this, "function or thread");
	}

	/**
	 * Set the environment on an object.
	 *
	 * Typically the environment is created once per application via a platform
	 * helper method such as {@link JsePlatform#standardGlobals(LuaState)}
	 * However, any object can serve as an environment if it contains suitable metatag
	 * values to implement {@link #get(LuaState, LuaValue)} to provide the environment values.
	 *
	 * @param env {@link LuaValue} (typically a {@link LuaTable}) containing the environment.
	 * @see JsePlatform
	 */
	public void setfenv(LuaValue env) {
		throw ErrorFactory.typeError(this, "function or thread");
	}

	/**
	 * Call {@code this} with 0 arguments, including metatag processing,
	 * and return only the first return value.
	 *
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 *
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * To call {@code this} as a method call, use {@link #method(LuaState, LuaValue)} instead.
	 *
	 * @param state The current lua state
	 * @return First return value {@code (this())}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaState, LuaValue)
	 * @see #call(LuaState, LuaValue, LuaValue)
	 * @see #call(LuaState, LuaValue, LuaValue, LuaValue)
	 * @see #invoke(LuaState, Varargs)
	 * @see #method(LuaState, LuaValue)
	 */
	public LuaValue call(LuaState state) {
		return callmt(state).call(state, this);
	}

	/**
	 * Call {@code this} with 1 argument, including metatag processing,
	 * and return only the first return value.
	 *
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 *
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * To call {@code this} as a method call, use {@link #method(LuaState, LuaValue)} instead.
	 *
	 * @param state The current lua state
	 * @param arg   First argument to supply to the called function
	 * @return First return value {@code (this(arg))}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaState)
	 * @see #call(LuaState, LuaValue, LuaValue)
	 * @see #call(LuaState, LuaValue, LuaValue, LuaValue)
	 * @see #invoke(LuaState, Varargs)
	 * @see #method(LuaState, LuaValue, LuaValue)
	 */
	public LuaValue call(LuaState state, LuaValue arg) {
		return callmt(state).call(state, this, arg);
	}

	/**
	 * Call {@code this} with 2 arguments, including metatag processing,
	 * and return only the first return value.
	 *
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 *
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * To call {@code this} as a method call, use {@link #method(LuaState, LuaValue)} instead.
	 *
	 * @param state The current lua state
	 * @param arg1  First argument to supply to the called function
	 * @param arg2  Second argument to supply to the called function
	 * @return First return value {@code (this(arg1, arg2))}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaState)
	 * @see #call(LuaState, LuaValue)
	 * @see #call(LuaState, LuaValue, LuaValue, LuaValue)
	 * @see #method(LuaState, LuaValue, LuaValue, LuaValue)
	 */
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) {
		return callmt(state).call(state, this, arg1, arg2);
	}

	/**
	 * Call {@code this} with 3 arguments, including metatag processing,
	 * and return only the first return value.
	 *
	 * If {@code this} is a {@link LuaFunction}, call it,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 *
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * To call {@code this} as a method call, use {@link #method(LuaState, LuaValue)} instead.
	 *
	 * @param state The current lua state
	 * @param arg1  First argument to supply to the called function
	 * @param arg2  Second argument to supply to the called function
	 * @param arg3  Second argument to supply to the called function
	 * @return First return value {@code (this(arg1, arg2, arg3))}, or {@link Constants#NIL} if there were none.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaState)
	 * @see #call(LuaState, LuaValue)
	 * @see #call(LuaState, LuaValue, LuaValue)
	 * @see #invokeMethod(LuaState, LuaValue, Varargs)
	 */
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return callmt(state).invoke(state, ValueFactory.varargsOf(this, arg1, arg2, arg3)).first();
	}

	/**
	 * Call named method on {@code this} with 0 arguments, including metatag processing,
	 * and return only the first return value.
	 *
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 *
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * To call {@code this} as a plain call, use {@link #call(LuaState)} instead.
	 *
	 * @param state The current lua state
	 * @param name  Name of the method to look up for invocation
	 * @return All values returned from {@code this:name()} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaState)
	 * @see #invoke(LuaState, Varargs)
	 * @see #method(LuaState, LuaValue, LuaValue)
	 * @see #method(LuaState, LuaValue, LuaValue, LuaValue)
	 */
	public LuaValue method(LuaState state, LuaValue name) {
		return this.get(state, name).call(state, this);
	}

	/**
	 * Call named method on {@code this} with 1 argument, including metatag processing,
	 * and return only the first return value.
	 *
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 *
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * To call {@code this} as a plain call, use {@link #call(LuaState, LuaValue)} instead.
	 *
	 * @param state The current lua state
	 * @param name  Name of the method to look up for invocation
	 * @param arg   Argument to supply to the method
	 * @return All values returned from {@code this:name(arg)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaState, LuaValue)
	 * @see #invoke(LuaState, Varargs)
	 * @see #method(LuaState, LuaValue)
	 * @see #method(LuaState, LuaValue, LuaValue, LuaValue)
	 */
	public LuaValue method(LuaState state, LuaValue name, LuaValue arg) {
		return this.get(state, name).call(state, this, arg);
	}

	/**
	 * Call named method on {@code this} with 2 arguments, including metatag processing,
	 * and return only the first return value.
	 *
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return only its first return value, dropping any others.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 *
	 * If the return value is a {@link Varargs}, only the 1st value will be returned.
	 * To get multiple values, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * To call {@code this} as a plain call, use {@link #call(LuaState, LuaValue, LuaValue)} instead.
	 *
	 * @param state The current lua state
	 * @param name  Name of the method to look up for invocation
	 * @param arg1  First argument to supply to the method
	 * @param arg2  Second argument to supply to the method
	 * @return All values returned from {@code this:name(arg1,arg2)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaState, LuaValue, LuaValue)
	 * @see #method(LuaState, LuaValue, LuaValue)
	 */
	public LuaValue method(LuaState state, LuaValue name, LuaValue arg1, LuaValue arg2) {
		return this.get(state, name).call(state, this, arg1, arg2);
	}

	/**
	 * Call {@code this} with variable arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 *
	 * If {@code this} is a {@link LuaFunction}, call it, and return all values.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 *
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 *
	 * To call {@code this} as a method call, use {@link #invokeMethod(LuaState, LuaValue, Varargs)} instead.
	 *
	 * @param state The current lua state
	 * @param args  Varargs containing the arguments to supply to the called function
	 * @return All return values as a {@link Varargs} instance.
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see ValueFactory#varargsOf(LuaValue[])
	 * @see #call(LuaState, LuaValue)
	 * @see #invokeMethod(LuaState, LuaValue, Varargs)
	 */
	public Varargs invoke(LuaState state, Varargs args) {
		return callmt(state).invoke(state, ValueFactory.varargsOf(this, args));
	}

	/**
	 * Call named method on {@code this} with variable arguments, including metatag processing,
	 * and retain all return values in a {@link Varargs}.
	 *
	 * Look up {@code this[name]} and if it is a {@link LuaFunction},
	 * call it inserting {@code this} as an additional first argument,
	 * and return all return values as a {@link Varargs} instance.
	 * Otherwise, look for the {@link Constants#CALL} metatag and call that.
	 *
	 * To get a particular return value, us {@link Varargs#arg(int)}
	 *
	 * To call {@code this} as a plain call, use {@link #invoke(LuaState, Varargs)} instead.
	 *
	 * @param state The current lua state
	 * @param name  Name of the method to look up for invocation
	 * @param args  {@link Varargs} containing arguments to supply to the called function after {@code this}
	 * @return All values returned from {@code this:name(args)} as a {@link Varargs} instance
	 * @throws LuaError if not a function and {@link Constants#CALL} is not defined,
	 *                  or the invoked function throws a {@link LuaError}
	 *                  or the invoked closure throw a lua {@code error}
	 * @see #call(LuaState)
	 * @see #invoke(LuaState, Varargs)
	 */
	public Varargs invokeMethod(LuaState state, LuaValue name, Varargs args) {
		return get(state, name).invoke(state, ValueFactory.varargsOf(this, args));
	}

	/**
	 * Get the metatag value for the {@link Constants#CALL} metatag, if it exists.
	 *
	 * @param state The current lua state
	 * @return {@link LuaValue} value if metatag is defined
	 * @throws LuaError if {@link Constants#CALL} metatag is not defined.
	 */
	protected LuaValue callmt(LuaState state) {
		return checkmetatag(state, Constants.CALL, "attempt to call ");
	}

	/**
	 * Unary minus: return negative value {@code (-this)} as defined by lua unary minus operator
	 *
	 * @param state The current lua state
	 * @return boolean inverse as {@link LuaBoolean} if boolean or nil,
	 * numeric inverse as {@link LuaNumber} if numeric,
	 * or metatag processing result if {@link Constants#UNM} metatag is defined
	 * @throws LuaError if  {@code this} is not a table or string, and has no {@link Constants#UNM} metatag
	 */
	public LuaValue neg(LuaState state) {
		return checkmetatag(state, Constants.UNM, "attempt to perform arithmetic on ").call(state, this);
	}

	/**
	 * Length operator: return lua length of object {@code (#this)} including metatag processing as java int
	 *
	 * @param state The current lua state
	 * @return length as defined by the lua # operator
	 * or metatag processing result
	 * @throws LuaError if  {@code this} is not a table or string, and has no {@link Constants#LEN} metatag
	 */
	public LuaValue len(LuaState state) {
		return checkmetatag(state, Constants.LEN, "attempt to get length of ").call(state, this);
	}

	/**
	 * Length operator: return lua length of object {@code (#this)} including metatag processing as java int
	 *
	 * @param state The current lua state
	 * @return length as defined by the lua # operator
	 * or metatag processing result converted to java int using {@link #toInteger()}
	 * @throws LuaError if  {@code this} is not a table or string, and has no {@link Constants#LEN} metatag
	 */
	public int length(LuaState state) {
		return len(state).toInteger();
	}

	/**
	 * Implementation of lua 5.0 getn() function.
	 *
	 * @return value of getn() as defined in lua 5.0 spec if {@code this} is a {@link LuaTable}
	 * @throws LuaError if  {@code this} is not a {@link LuaTable}
	 */
	public LuaValue getn() {
		throw ErrorFactory.typeError(this, "getn");
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
	 * Perform string comparison with another value
	 * of any type
	 * using string comparison based on byte values.
	 *
	 * Only strings can be compared, meaning
	 * each operand must derive from {@link LuaString}.
	 *
	 * @param rhs The right-hand-side value to perform the comparison with
	 * @return int &lt; 0 for {@code (this &lt; rhs)}, int &gt; 0 for {@code (this &gt; rhs)}, or 0 when same string.
	 * @throws LuaError if either operand is not a string
	 */
	public int strcmp(LuaValue rhs) {
		throw new LuaError("attempt to compare " + typeName());
	}

	/**
	 * Perform string comparison with another value
	 * known to be a {@link LuaString}
	 * using string comparison based on byte values.
	 *
	 * Only strings can be compared, meaning
	 * each operand must derive from {@link LuaString}.
	 *
	 * @param rhs The right-hand-side value to perform the comparison with
	 * @return int &lt; 0 for {@code (this &lt; rhs)}, int &gt; 0 for {@code (this &gt; rhs)}, or 0 when same string.
	 * @throws LuaError if this is not a string
	 */
	public int strcmp(LuaString rhs) {
		throw new LuaError("attempt to compare " + typeName());
	}

	/**
	 * Perform metatag processing for concatenation operations.
	 *
	 * Finds the {@link Constants#CONCAT} metatag value and invokes it,
	 * or throws {@link LuaError} if it doesn't exist.
	 *
	 * @param state The current lua state
	 * @param rhs   The right-hand-side value to perform the operation with
	 * @return {@link LuaValue} resulting from metatag processing for {@link Constants#CONCAT} metatag.
	 * @throws LuaError if metatag was not defined for either operand
	 */
	public LuaValue concatmt(LuaState state, LuaValue rhs) {
		LuaValue h = metatag(state, Constants.CONCAT);
		if (h.isNil() && (h = rhs.metatag(state, Constants.CONCAT)).isNil()) {
			throw new LuaError("attempt to concatenate " + typeName() + " and " + rhs.typeName());
		}
		return h.call(state, this, rhs);
	}

	/**
	 * Convert this value to a string if it is a {@link LuaString} or {@link LuaNumber},
	 * or throw a {@link LuaError} if it is not
	 *
	 * @return {@link LuaString} corresponding to the value if a string or number
	 * @throws LuaError if not a string or number
	 */
	public LuaString strvalue() {
		throw ErrorFactory.typeError(this, "strValue");
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
	 * @param state The current lua state
	 * @param t     {@link LuaValue} on which field is being referenced, typically a table or something with the metatag {@link Constants#INDEX} defined
	 * @param key   {@link LuaValue} naming the field to reference
	 * @return {@link LuaValue} for the {@code key} if it exists, or {@link Constants#NIL}
	 * @throws LuaError if there is a loop in metatag processing
	 */
	protected static LuaValue getTable(LuaState state, LuaValue t, LuaValue key) {
		LuaValue tm;
		int loop = 0;
		do {
			if (t.isTable()) {
				LuaValue res = t.rawget(key);
				if ((!res.isNil()) || (tm = t.metatag(state, Constants.INDEX)).isNil()) {
					return res;
				}
			} else if ((tm = t.metatag(state, Constants.INDEX)).isNil()) {
				throw ErrorFactory.indexError(t);
			}
			if (tm.isFunction()) {
				return tm.call(state, t, key);
			}
			t = tm;
		}
		while (++loop < Constants.MAXTAGLOOP);
		throw new LuaError("loop in gettable");
	}

	/**
	 * Perform field assignment including metatag processing.
	 *
	 * @param state The current lua state
	 * @param t     {@link LuaValue} on which value is being set, typically a table or something with the metatag {@link Constants#NEWINDEX} defined
	 * @param key   {@link LuaValue} naming the field to assign
	 * @param value {@link LuaValue} the new value to assign to {@code key}
	 * @return true if assignment or metatag processing succeeded, false otherwise
	 * @throws LuaError if there is a loop in metatag processing
	 */
	protected static boolean setTable(LuaState state, LuaValue t, LuaValue key, LuaValue value) {
		LuaValue tm;
		int loop = 0;
		do {
			if (t.isTable()) {
				if ((!t.rawget(key).isNil()) || (tm = t.metatag(state, Constants.NEWINDEX)).isNil()) {
					t.rawset(key, value);
					return true;
				}
			} else if ((tm = t.metatag(state, Constants.NEWINDEX)).isNil()) {
				throw ErrorFactory.typeError(t, "index");
			}
			if (tm.isFunction()) {
				tm.call(state, t, key, value);
				return true;
			}
			t = tm;
		}
		while (++loop < Constants.MAXTAGLOOP);
		throw new LuaError("loop in settable");
	}

	/**
	 * Perform metatag processing for comparison operations.
	 *
	 * Finds the supplied metatag value and invokes it,
	 * or throws {@link LuaError} if none applies.
	 *
	 * @param state The current lua state
	 * @param tag   The metatag to look up
	 * @param op1   The right-hand-side value to perform the operation with
	 * @return {@link LuaValue} resulting from metatag processing
	 * @throws LuaError if metatag was not defined for either operand,
	 *                  or if the operands are not the same type,
	 *                  or the metatag values for the two operands are different.
	 */
	public LuaValue comparemt(LuaState state, LuaValue tag, LuaValue op1) {
		if (type() == op1.type()) {
			LuaValue h = metatag(state, tag);
			if (!h.isNil() && h == op1.metatag(state, tag)) {
				return h.call(state, this, op1);
			}
		}
		throw new LuaError("attempt to compare " + tag + " on " + typeName() + " and " + op1.typeName());
	}

	/**
	 * Perform metatag processing for arithmetic operations.
	 *
	 * Finds the supplied metatag value for {@code this} or {@code op2} and invokes it,
	 * or throws {@link LuaError} if neither is defined.
	 *
	 * @param state The current lua state
	 * @param tag   The metatag to look up
	 * @param op2   The other operand value to perform the operation with
	 * @return {@link LuaValue} resulting from metatag processing
	 * @throws LuaError if metatag was not defined for either operand
	 * @see Constants#ADD
	 * @see Constants#SUB
	 * @see Constants#MUL
	 * @see Constants#POW
	 * @see Constants#DIV
	 * @see Constants#MOD
	 */
	protected LuaValue arithmt(LuaState state, LuaValue tag, LuaValue op2) {
		LuaValue h = this.metatag(state, tag);
		if (h.isNil()) {
			h = op2.metatag(state, tag);
			if (h.isNil()) {
				throw new LuaError("attempt to perform arithmetic " + tag + " on " + typeName() + " and " + op2.typeName());
			}
		}
		return h.call(state, this, op2);
	}

	/**
	 * Get particular metatag, or return {@link Constants#NIL} if it doesn't exist
	 *
	 * @param state The current lua state
	 * @param tag   Metatag name to look up, typically a string such as
	 *              {@link Constants#INDEX} or {@link Constants#NEWINDEX}
	 * @return {@link LuaValue} for tag {@code reason}, or  {@link Constants#NIL}
	 */
	public LuaValue metatag(LuaState state, LuaValue tag) {
		LuaValue mt = getMetatable(state);
		if (mt == null) {
			return Constants.NIL;
		}
		return mt.rawget(tag);
	}

	/**
	 * Get particular metatag, or throw {@link LuaError} if it doesn't exist
	 *
	 * @param state  The current lua state
	 * @param tag    Metatag name to look up, typically a string such as
	 *               {@link Constants#INDEX} or {@link Constants#NEWINDEX}
	 * @param reason Description of error when tag lookup fails.
	 * @return {@link LuaValue} that can be called
	 * @throws LuaError when the lookup fails.
	 */
	protected LuaValue checkmetatag(LuaState state, LuaValue tag, String reason) {
		LuaValue h = this.metatag(state, tag);
		if (h.isNil()) {
			throw new LuaError(reason + typeName());
		}
		return h;
	}

	/**
	 * Callback used during tail call processing to invoke the function once.
	 *
	 * This may return a {@link TailcallVarargs} to be evaluated by the client.
	 *
	 * This should not be called directly, instead use on of the call invocation functions.
	 *
	 * @param state The current lua state
	 * @param args  the arguments to the call invocation.
	 * @return Varargs the return values, possible a TailcallVarargs.
	 * @see LuaValue#call(LuaState)
	 * @see LuaValue#invoke(LuaState, Varargs)
	 * @see LuaValue#method(LuaState, LuaValue)
	 * @see LuaValue#invokeMethod(LuaState, LuaValue, Varargs)
	 */
	public Varargs onInvoke(LuaState state, Varargs args) {
		return invoke(state, args);
	}

	/**
	 * Varargs implementation backed by an array of LuaValues
	 *
	 * This is an internal class not intended to be used directly.
	 * Instead use the corresponding static methods on LuaValue.
	 *
	 * @see ValueFactory#varargsOf(LuaValue[])
	 * @see ValueFactory#varargsOf(LuaValue[], Varargs)
	 */
	protected static final class ArrayVarargs extends Varargs {
		private final LuaValue[] v;
		private final Varargs r;

		/**
		 * Construct a Varargs from an array of LuaValue.
		 *
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static methods on LuaValue.
		 *
		 * @param v The initial values
		 * @param r Remaining arguments
		 * @see ValueFactory#varargsOf(LuaValue[])
		 * @see ValueFactory#varargsOf(LuaValue[], Varargs)
		 */
		public ArrayVarargs(LuaValue[] v, Varargs r) {
			this.v = v;
			this.r = r;
		}

		@Override
		public LuaValue arg(int i) {
			return i >= 1 && i <= v.length ? v[i - 1] : r.arg(i - v.length);
		}

		@Override
		public int count() {
			return v.length + r.count();
		}

		@Override
		public LuaValue first() {
			return v.length > 0 ? v[0] : r.first();
		}
	}

	/**
	 * Varargs implementation backed by an array of LuaValues
	 *
	 * This is an internal class not intended to be used directly.
	 * Instead use the corresponding static methods on LuaValue.
	 *
	 * @see ValueFactory#varargsOf(LuaValue[], int, int)
	 * @see ValueFactory#varargsOf(LuaValue[], int, int, Varargs)
	 */
	protected static final class ArrayPartVarargs extends Varargs {
		private final int offset;
		private final LuaValue[] v;
		private final int length;
		private final Varargs more;

		/**
		 * Construct a Varargs from an array of LuaValue.
		 *
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static methods on LuaValue.
		 *
		 * @param v      The values to use
		 * @param offset Offset in the array
		 * @param length Length of the varargs
		 * @see ValueFactory#varargsOf(LuaValue[], int, int)
		 */
		public ArrayPartVarargs(LuaValue[] v, int offset, int length) {
			this.v = v;
			this.offset = offset;
			this.length = length;
			this.more = Constants.NONE;
		}

		/**
		 * Construct a Varargs from an array of LuaValue and additional arguments.
		 *
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static method on LuaValue.
		 *
		 * @param v      The values to use
		 * @param offset Offset in the array
		 * @param length Length of the varargs
		 * @param more   Remaining arguments
		 * @see ValueFactory#varargsOf(LuaValue[], int, int, Varargs)
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
		public int count() {
			return length + more.count();
		}

		@Override
		public LuaValue first() {
			return length > 0 ? v[offset] : more.first();
		}
	}

	/**
	 * Varargs implementation backed by two values.
	 *
	 * This is an internal class not intended to be used directly.
	 * Instead use the corresponding static method on LuaValue.
	 *
	 * @see ValueFactory#varargsOf(LuaValue, Varargs)
	 */
	protected static final class PairVarargs extends Varargs {
		private final LuaValue v1;
		private final Varargs v2;

		/**
		 * Construct a Varargs from an two LuaValues.
		 *
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static method on LuaValue.
		 *
		 * @param v1 First argument
		 * @param v2 Remaining arguments
		 * @see ValueFactory#varargsOf(LuaValue, Varargs)
		 */
		public PairVarargs(LuaValue v1, Varargs v2) {
			this.v1 = v1;
			this.v2 = v2;
		}

		@Override
		public LuaValue arg(int i) {
			return i == 1 ? v1 : v2.arg(i - 1);
		}

		@Override
		public int count() {
			return 1 + v2.count();
		}

		@Override
		public LuaValue first() {
			return v1;
		}
	}
}
