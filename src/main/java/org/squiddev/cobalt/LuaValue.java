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

import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.CoreLibraries;

import static org.squiddev.cobalt.Constants.*;

/**
 * Base class for all concrete lua type values.
 * <p>
 * Establishes base implementations for all the operations on lua types.
 * This allows Java clients to deal essentially with one type for all Java values, namely {@link LuaValue}.
 * <p>
 * Constructors are provided as static methods for common Java types, such as
 * {@link ValueFactory#valueOf(int)} or {@link ValueFactory#valueOf(String)}
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
 * {@link LuaFunction#invoke(LuaState, Varargs)}
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
 * See {@link CoreLibraries} for details.
 * <p>
 * In general a {@link LuaError} may be thrown on any operation when the
 * types supplied to any operation are illegal from a lua perspective.
 * Examples could be attempting to concatenate a NIL value, or attempting arithmetic
 * on values that are not number.
 *
 * @see CoreLibraries
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
		return Constants.TYPE_NAMES[type];
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
	public final LuaString luaTypeName() {
		return Constants.TYPE_NAMES_LUA[type];
	}

	/**
	 * Check if {@code this} is {@code nil}
	 *
	 * @return true if this is {@code nil}, otherwise false
	 * @see Constants#NIL
	 * @see Constants#NONE
	 * @see #optValue(LuaValue)
	 * @see Constants#TNIL
	 */
	public final boolean isNil() {
		return this == NIL;
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
	 * Convert to boolean false if {@link Constants#NIL} or {@link Constants#FALSE}, true if anything else
	 *
	 * @return Value cast to byte if number or string convertible to number, otherwise 0
	 * @see #optBoolean(boolean)
	 * @see #checkBoolean()
	 * @see Constants#TBOOLEAN
	 */
	public final boolean toBoolean() {
		return this != NIL && this != FALSE;
	}

	/**
	 * Convert to double if numeric, or 0 if not.
	 *
	 * @return Value cast to double if number or string convertible to number, otherwise 0
	 * @see #toInteger()
	 * @see #optDouble(double)
	 * @see #checkNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public double toDouble() {
		return Double.NaN;
	}

	/**
	 * Convert to int if numeric, or 0 if not.
	 *
	 * @return Value cast to int if number or string convertible to number, otherwise 0
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
		return ErrorFactory.typeName(this) + ": " + Integer.toHexString(hashCode());
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
	 * @see Constants#TBOOLEAN
	 */
	public final boolean optBoolean(boolean defval) throws LuaError {
		return this == NIL ? defval : checkBoolean();
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
	 * @see #checkDouble()
	 * @see #toDouble()
	 * @see #toNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public final double optDouble(double defval) throws LuaError {
		return this == NIL ? defval : checkDouble();
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
	 * @see #checkFunction()
	 * @see Constants#TFUNCTION
	 */
	public final LuaFunction optFunction(LuaFunction defval) throws LuaError {
		return this == NIL ? defval : checkFunction();
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
	 * @see #checkInteger()
	 * @see #toInteger()
	 * @see #toNumber()
	 * @see #isNumber()
	 * @see Constants#TNUMBER
	 */
	public final int optInteger(int defval) throws LuaError {
		return this == NIL ? defval : checkInteger();
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
	public final long optLong(long defval) throws LuaError {
		return this == NIL ? defval : checkLong();
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
	public final LuaNumber optNumber(LuaNumber defval) throws LuaError {
		return this == NIL ? defval : checkNumber();
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
	public final String optString(String defval) throws LuaError {
		return this == NIL ? defval : checkString();
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
	public final LuaString optLuaString(LuaString defval) throws LuaError {
		return this == NIL ? defval : checkLuaString();
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
	 * @see Constants#TTABLE
	 */
	public final LuaTable optTable(LuaTable defval) throws LuaError {
		return this == NIL ? defval : checkTable();
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
	public final LuaThread optThread(LuaThread defval) throws LuaError {
		return this == NIL ? defval : checkThread();
	}

	/**
	 * Perform argument check that this is not nil or none.
	 *
	 * @param defval {@link LuaValue} to return if {@code this} is nil or none
	 * @return {@code this} if not nil or none, else {@code defval}
	 * @see Constants#NIL
	 * @see Constants#NONE
	 * @see #isNil()
	 * @see Constants#TNIL
	 */
	public final LuaValue optValue(LuaValue defval) {
		return this == NIL ? defval : this;
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
	public boolean checkBoolean() throws LuaError {
		throw ErrorFactory.argError(this, "boolean");
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
	 * @see #checkInteger()
	 * @see #checkLong()
	 * @see #optDouble(double)
	 * @see Constants#TNUMBER
	 */
	public double checkDouble() throws LuaError {
		throw ErrorFactory.argError(this, "number");
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
	 */
	public LuaFunction checkFunction() throws LuaError {
		throw ErrorFactory.argError(this, "function");
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
	 * @see #checkLong()
	 * @see #checkDouble()
	 * @see #optInteger(int)
	 * @see Constants#TNUMBER
	 */
	public int checkInteger() throws LuaError {
		throw ErrorFactory.argError(this, "number");
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
	 * @see #checkInteger()
	 * @see #checkDouble()
	 * @see #optLong(long)
	 * @see Constants#TNUMBER
	 */
	public long checkLong() throws LuaError {
		throw ErrorFactory.argError(this, "number");
	}

	/**
	 * Check that the value is numeric, and return as a LuaNumber if so, or throw {@link LuaError}
	 * <p>
	 * Values that are {@link LuaString} that can be converted to a number will be converted and returned.
	 *
	 * @return value as a {@link LuaNumber} if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkInteger()
	 * @see #checkDouble()
	 * @see #checkLong()
	 * @see #optNumber(LuaNumber)
	 * @see Constants#TNUMBER
	 */
	public LuaNumber checkNumber() throws LuaError {
		throw ErrorFactory.argError(this, "number");
	}

	/**
	 * Check that the value is numeric, and return as a LuaNumber if so, or throw {@link LuaError}
	 * <p>
	 * Values that are {@link LuaString} that can be converted to a number will be converted and returned.
	 *
	 * @param msg String message to supply if conversion fails
	 * @return value as a {@link LuaNumber} if numeric
	 * @throws LuaError if not a {@link LuaNumber} or is a {@link LuaString} that can't be converted to number
	 * @see #checkInteger()
	 * @see #checkDouble()
	 * @see #checkLong()
	 * @see #optNumber(LuaNumber)
	 * @see Constants#TNUMBER
	 */
	public LuaNumber checkNumber(String msg) throws LuaError {
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
	 * @throws LuaError If this is not a string
	 * @see #checkLuaString()
	 * @see #optString(String)
	 * @see #toString()
	 * @see #isString
	 * @see Constants#TSTRING
	 */
	public String checkString() throws LuaError {
		throw ErrorFactory.argError(this, "string");
	}

	/**
	 * Check that this is a lua string, or throw {@link LuaError} if it is not.
	 * <p>
	 * In lua all numbers are strings, so this will succeed for
	 * anything that derives from {@link LuaString} or {@link LuaNumber}.
	 * Numbers will be converted to {@link LuaString}.
	 *
	 * @return {@link LuaString} representation of the value if it is a {@link LuaString} or {@link LuaNumber}
	 * @throws LuaError if {@code this} is not a {@link LuaString}
	 * @see #checkString()
	 * @see #optLuaString(LuaString)
	 * @see #toLuaString()
	 * @see #isString()
	 * @see Constants#TSTRING
	 */
	public LuaString checkLuaString() throws LuaError {
		throw ErrorFactory.argError(this, "string");
	}

	/**
	 * Check that this is a {@link LuaTable}, or throw {@link LuaError} if it is not
	 *
	 * @return {@code this} if it is a {@link LuaTable}
	 * @throws LuaError if {@code this} is not a {@link LuaTable}
	 * @see #optTable(LuaTable)
	 * @see Constants#TTABLE
	 */
	public LuaTable checkTable() throws LuaError {
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
	public LuaThread checkThread() throws LuaError {
		throw ErrorFactory.argError(this, "thread");
	}
	//endregion

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
	@Deprecated
	public LuaValue first() {
		return this;
	}

	@Override
	public void fill(LuaValue[] array, int offset) {
		array[offset] = this;
	}

	/**
	 * Get the metatable for this {@link LuaValue}
	 * <p>
	 * For {@link LuaTable} and {@link LuaUserdata} instances,
	 * the metatable returned is this instance metatable.
	 * For all other types, the class metatable value will be returned.
	 *
	 * @param state The current lua state
	 * @return metatable, or null if it there is none
	 */
	public LuaTable getMetatable(LuaState state) {
		return null;
	}

	/**
	 * Set the metatable for this {@link LuaValue}
	 * <p>
	 * For {@link LuaTable} and {@link LuaUserdata} instances, the metatable is per instance.
	 * For all other types, there is one metatable per type that can be set directly from java
	 *
	 * @param state     The current lua state
	 * @param metatable {@link LuaValue} instance to serve as the metatable, or null to reset it.
	 * @throws LuaError If the metatable cannot be set.
	 */
	public void setMetatable(LuaState state, LuaTable metatable) throws LuaError {
		throw ErrorFactory.argError(this, "table");
	}

	/**
	 * Get particular metatag, or return {@link Constants#NIL} if it doesn't exist
	 *
	 * @param state The current lua state
	 * @param tag   Metatag name to look up, typically a string such as
	 *              {@link Constants#INDEX} or {@link Constants#NEWINDEX}
	 * @return {@link LuaValue} for tag {@code reason}, or  {@link Constants#NIL}
	 */
	public final LuaValue metatag(LuaState state, LuaValue tag) {
		LuaTable mt = getMetatable(state);
		return mt == null ? Constants.NIL : mt.rawget(tag);
	}

	/**
	 * Get particular metatag, or return {@link Constants#NIL} if it doesn't exist
	 *
	 * @param state The current lua state
	 * @param tag   Metatag name to look up, typically a string such as
	 *              {@link Constants#INDEX} or {@link Constants#NEWINDEX}
	 * @return {@link LuaValue} for tag {@code reason}, or  {@link Constants#NIL}
	 */
	public final LuaValue metatag(LuaState state, CachedMetamethod tag) {
		LuaTable mt = getMetatable(state);
		return mt == null ? Constants.NIL : mt.rawget(tag);
	}

	/**
	 * Varargs implementation backed by an array of LuaValues
	 * <p>
	 * This is an internal class not intended to be used directly.
	 * Instead use the corresponding static methods on LuaValue.
	 *
	 * @see ValueFactory#varargsOf(LuaValue[])
	 */
	protected static final class ArrayVarargs extends DepthVarargs {
		private final LuaValue[] v;
		private final Varargs r;

		/**
		 * Construct a Varargs from an array of LuaValue.
		 * <p>
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static methods on LuaValue.
		 *
		 * @param v The initial values
		 * @param r Remaining arguments
		 * @see ValueFactory#varargsOf(LuaValue[])
		 */
		public ArrayVarargs(LuaValue[] v, Varargs r) {
			super(depth(r) + 1);
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

		@Override
		public void fill(LuaValue[] array, int offset) {
			System.arraycopy(v, 0, array, offset, v.length);
			r.fill(array, offset + v.length);
		}
	}

	/**
	 * Varargs implementation backed by two values.
	 * <p>
	 * This is an internal class not intended to be used directly.
	 * Instead use the corresponding static method on LuaValue.
	 *
	 * @see ValueFactory#varargsOf(LuaValue, Varargs)
	 */
	protected static final class PairVarargs extends DepthVarargs {
		private final LuaValue v1;
		private final Varargs v2;

		/**
		 * Construct a Varargs from an two LuaValues.
		 * <p>
		 * This is an internal class not intended to be used directly.
		 * Instead use the corresponding static method on LuaValue.
		 *
		 * @param v1 First argument
		 * @param v2 Remaining arguments
		 * @see ValueFactory#varargsOf(LuaValue, Varargs)
		 */
		public PairVarargs(LuaValue v1, Varargs v2) {
			super(depth(v2) + 1);
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

		@Override
		public void fill(LuaValue[] array, int offset) {
			array[offset] = v1;
			v2.fill(array, offset + 1);
		}
	}
}
