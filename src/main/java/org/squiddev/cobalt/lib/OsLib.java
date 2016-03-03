/**
 * ****************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
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
package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.IOException;

import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Subclass of {@link LibFunction} which implements the standard lua {@code os} library.
 *
 * This can be installed as-is on either platform, or extended
 * and refined to be used in a complete Jse implementation.
 *
 * Because the nature of the {@code os} library is to encapsulate
 * os-specific features, the behavior of these functions varies considerably
 * from their counterparts in the C platform.
 *
 * @see LibFunction
 * @see JsePlatform
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.8">http://www.lua.org/manual/5.1/manual.html#5.8</a>
 */
public class OsLib extends VarArgFunction {
	private static final int INIT = 0;
	private static final int CLOCK = 1;
	private static final int DATE = 2;
	private static final int DIFFTIME = 3;
	private static final int EXECUTE = 4;
	private static final int EXIT = 5;
	private static final int GETENV = 6;
	private static final int REMOVE = 7;
	private static final int RENAME = 8;
	private static final int SETLOCALE = 9;
	private static final int TIME = 10;
	private static final int TMPNAME = 11;

	private static final String[] NAMES = {
		"clock",
		"date",
		"difftime",
		"execute",
		"exit",
		"getenv",
		"remove",
		"rename",
		"setlocale",
		"time",
		"tmpname",
	};

	private static final long t0 = System.currentTimeMillis();

	public LuaValue init(LuaState state) {
		LuaTable t = new LuaTable();
		bind(state, t, this.getClass(), NAMES, CLOCK);
		env.set(state, "os", t);
		state.loadedPackages.set(state, "os", t);
		return t;
	}

	@Override
	public Varargs invoke(LuaState state, Varargs args) {
		try {
			switch (opcode) {
				case INIT:
					return init(state);
				case CLOCK:
					return ValueFactory.valueOf(clock());
				case DATE: {
					String s = args.arg(1).optjstring(null);
					double defval = -1;
					double t = args.arg(2).optDouble(defval);
					return ValueFactory.valueOf(date(s, t == -1 ? System.currentTimeMillis() / 1000. : t));
				}
				case DIFFTIME:
					return ValueFactory.valueOf(difftime(args.arg(1).checkDouble(), args.arg(2).checkDouble()));
				case EXECUTE:
					return valueOf(state.resourceManipulator.execute(args.arg(1).optjstring(null)));
				case EXIT:
					exit(args.arg(1).optInteger(0));
					return Constants.NONE;
				case GETENV: {
					final String val = getenv(args.arg(1).checkjstring());
					return val != null ? ValueFactory.valueOf(val) : Constants.NIL;
				}
				case REMOVE:
					state.resourceManipulator.remove(args.arg(1).checkjstring());
					return Constants.TRUE;
				case RENAME:
					state.resourceManipulator.rename(args.arg(1).checkjstring(), args.arg(2).checkjstring());
					return Constants.TRUE;
				case SETLOCALE: {
					String s = setlocale(args.arg(1).optjstring(null), args.arg(2).optjstring("all"));
					return s != null ? ValueFactory.valueOf(s) : Constants.NIL;
				}
				case TIME:
					return ValueFactory.valueOf(time(args.first().isNil() ? null : args.arg(1).checkTable()));
				case TMPNAME:
					return valueOf(state.resourceManipulator.tmpName());
			}
			return Constants.NONE;
		} catch (IOException e) {
			return varargsOf(Constants.NIL, ValueFactory.valueOf(e.getMessage()));
		}
	}

	/**
	 * @return an approximation of the amount in seconds of CPU time used by
	 * the program.
	 */
	protected double clock() {
		return (System.currentTimeMillis() - t0) / 1000.;
	}

	/**
	 * Returns the number of seconds from time t1 to time t2.
	 * In POSIX, Windows, and some other systems, this value is exactly t2-t1.
	 *
	 * @param t2 Last time
	 * @param t1 First time
	 * @return diffeence in time values, in seconds
	 */
	protected double difftime(double t2, double t1) {
		return t2 - t1;
	}

	/**
	 * If the time argument is present, this is the time to be formatted
	 * (see the os.time function for a description of this value).
	 * Otherwise, date formats the current time.
	 * <p>
	 * If format starts with '!', then the date is formatted in Coordinated
	 * Universal Time. After this optional character, if format is the string
	 * "*t", then date returns a table with the following fields: year
	 * (four digits), month (1--12), day (1--31), hour (0--23), min (0--59),
	 * sec (0--61), wday (weekday, Sunday is 1), yday (day of the year),
	 * and isdst (daylight saving flag, a boolean).
	 * <p>
	 * If format is not "*t", then date returns the date as a string,
	 * formatted according to the same rules as the C function strftime.
	 * <p>
	 * When called without arguments, date returns a reasonable date and
	 * time representation that depends on the host system and on the
	 * current locale (that is, os.date() is equivalent to os.date("%c")).
	 *
	 * @param format Format string to use
	 * @param time   time since epoch, or -1 if not supplied
	 * @return a LString or a LTable containing date and time,
	 * formatted according to the given string format.
	 */
	protected String date(String format, double time) {
		return new java.util.Date((long) (time * 1000)).toString();
	}

	/**
	 * Calls the C function exit, with an optional code, to terminate the host program.
	 *
	 * @param code The exit code
	 */
	protected void exit(int code) {
		System.exit(code);
	}

	/**
	 * Returns the value of the process environment variable varname,
	 * or null if the variable is not defined.
	 *
	 * @param varname The environment variable name
	 * @return String value, or null if not defined
	 */
	protected String getenv(String varname) {
		return System.getProperty(varname);
	}

	/**
	 * Sets the current locale of the program. locale is a string specifying
	 * a locale; category is an optional string describing which category to change:
	 * "all", "collate", "ctype", "monetary", "numeric", or "time"; the default category
	 * is "all".
	 * <p>
	 * If locale is the empty string, the current locale is set to an implementation-
	 * defined native locale. If locale is the string "C", the current locale is set
	 * to the standard C locale.
	 * <p>
	 * When called with null as the first argument, this function only returns the
	 * name of the current locale for the given category.
	 *
	 * @param locale   New locale
	 * @param category Category's locale to change
	 * @return the name of the new locale, or null if the request
	 * cannot be honored.
	 */
	protected String setlocale(String locale, String category) {
		return "C";
	}

	/**
	 * Returns the current time when called without arguments,
	 * or a time representing the date and time specified by the given table.
	 * This table must have fields year, month, and day,
	 * and may have fields hour, min, sec, and isdst
	 * (for a description of these fields, see the os.date function).
	 *
	 * @param table Table to use
	 * @return long value for the time
	 */
	protected long time(LuaTable table) {
		return System.currentTimeMillis();
	}
}
