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
package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.squiddev.cobalt.ErrorFactory.argError;
import static org.squiddev.cobalt.ValueFactory.*;

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
public class OsLib extends VarArgFunction implements LuaLibrary {
	private static final int CLOCK = 0;
	private static final int DATE = 1;
	private static final int DIFFTIME = 2;
	private static final int EXECUTE = 3;
	private static final int EXIT = 4;
	private static final int GETENV = 5;
	private static final int REMOVE = 6;
	private static final int RENAME = 7;
	private static final int SETLOCALE = 8;
	private static final int TIME = 9;
	private static final int TMPNAME = 10;

	private static final LuaString DATE_FORMAT = valueOf("%c");
	private static final LuaString DATE_TABLE = valueOf("*t");

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

	@Override
	public LuaValue add(LuaState state, LuaTable env) {
		LuaTable t = new LuaTable();
		LibFunction.bind(t, OsLib::new, NAMES);
		env.rawset("os", t);
		state.loadedPackages.rawset("os", t);
		return t;
	}

	@Override
	public Varargs invoke(LuaState state, Varargs args) throws LuaError {
		try {
			switch (opcode) {
				case CLOCK:
					return valueOf((System.currentTimeMillis() - t0) / 1000);
				case DATE: {
					LuaString format = args.arg(1).optLuaString(DATE_FORMAT);
					long time = args.arg(2).optLong(time(state, null));

					Calendar d = Calendar.getInstance(state.timezone, Locale.ROOT);
					d.setTime(new Date(time * 1000));
					if (format.startsWith('!')) {
						time -= timeZoneOffset(d);
						d.setTime(new Date(time * 1000));
						format = format.substring(1);
					}

					if (format.equals(DATE_TABLE)) {
						LuaTable tbl = tableOf();
						tbl.rawset("year", valueOf(d.get(Calendar.YEAR)));
						tbl.rawset("month", valueOf(d.get(Calendar.MONTH) + 1));
						tbl.rawset("day", valueOf(d.get(Calendar.DAY_OF_MONTH)));
						tbl.rawset("hour", valueOf(d.get(Calendar.HOUR_OF_DAY)));
						tbl.rawset("min", valueOf(d.get(Calendar.MINUTE)));
						tbl.rawset("sec", valueOf(d.get(Calendar.SECOND)));
						tbl.rawset("wday", valueOf(d.get(Calendar.DAY_OF_WEEK)));
						tbl.rawset("yday", valueOf(d.get(Calendar.DAY_OF_YEAR)));
						tbl.rawset("isdst", valueOf(isDaylightSavingsTime(d)));
						return tbl;
					}

					Buffer buffer = new Buffer(format.length);
					date(state, buffer, d, format);
					return buffer.toLuaString();
				}
				case DIFFTIME:
					return valueOf(args.arg(1).checkLong() - args.arg(2).checkLong());
				case EXECUTE:
					return valueOf(state.resourceManipulator.execute(args.arg(1).optString(null)));
				case EXIT:
					System.exit(args.arg(1).optInteger(0));
					return Constants.NONE;
				case GETENV: {
					final String val = System.getenv(args.arg(1).checkString());
					return val != null ? valueOf(val) : Constants.NIL;
				}
				case REMOVE:
					state.resourceManipulator.remove(args.arg(1).checkString());
					return Constants.TRUE;
				case RENAME:
					state.resourceManipulator.rename(args.arg(1).checkString(), args.arg(2).checkString());
					return Constants.TRUE;
				case SETLOCALE: {
					String locale = args.arg(1).optString(null);
					args.arg(2).optString("all");
					return locale == null || locale.equals("C") ? valueOf("C") : Constants.NIL;
				}
				case TIME:
					return valueOf(time(state, args.first().isNil() ? null : args.arg(1).checkTable()));
				case TMPNAME:
					return valueOf(state.resourceManipulator.tmpName());
			}
			return Constants.NONE;
		} catch (IOException e) {
			return varargsOf(Constants.NIL, valueOf(e.getMessage()));
		}
	}

	private static final String[] WEEKDAY_NAME_ABBREV = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
	private static final String[] WEEKDAY_NAME = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
	private static final String[] MONTH_NAME_ABBREV = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	private static final String[] MONTH_NAME = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

	private Calendar beginningOfYear(LuaState state, Calendar d) {
		Calendar y0 = Calendar.getInstance(state.timezone, Locale.ROOT);
		y0.setTime(d.getTime());
		y0.set(Calendar.MONTH, 0);
		y0.set(Calendar.DAY_OF_MONTH, 1);
		y0.set(Calendar.HOUR_OF_DAY, 0);
		y0.set(Calendar.MINUTE, 0);
		y0.set(Calendar.SECOND, 0);
		y0.set(Calendar.MILLISECOND, 0);
		return y0;
	}

	private int weekNumber(LuaState state, Calendar d, int startDay) {
		Calendar y0 = beginningOfYear(state, d);
		y0.set(Calendar.DAY_OF_MONTH, 1 + (startDay + 8 - y0.get(Calendar.DAY_OF_WEEK)) % 7);
		if (y0.after(d)) {
			y0.set(Calendar.YEAR, y0.get(Calendar.YEAR) - 1);
			y0.set(Calendar.DAY_OF_MONTH, 1 + (startDay + 8 - y0.get(Calendar.DAY_OF_WEEK)) % 7);
		}
		long dt = d.getTime().getTime() - y0.getTime().getTime();
		return 1 + (int) (dt / (7L * 24L * 3600L * 1000L));
	}

	private int timeZoneOffset(Calendar d) {
		int localStandardTimeMillis = 1000 *
			(d.get(Calendar.HOUR_OF_DAY) * 3600 + d.get(Calendar.MINUTE) * 60 + d.get(Calendar.SECOND));

		return d.getTimeZone().getOffset(
			1,
			d.get(Calendar.YEAR),
			d.get(Calendar.MONTH),
			d.get(Calendar.DAY_OF_MONTH),
			d.get(Calendar.DAY_OF_WEEK),
			localStandardTimeMillis
		) / 1000;
	}

	private boolean isDaylightSavingsTime(Calendar d) {
		return timeZoneOffset(d) != d.getTimeZone().getRawOffset() / 1000;
	}

	private static final LuaString FORMAT_C = valueOf("%a %b %e %H:%M:%S %Y");
	private static final LuaString FORMAT_DATE_D = valueOf("%m/%d/%y");
	private static final LuaString FORMAT_DATE_F = valueOf("%Y-%m-%d");
	private static final LuaString FORMAT_TIME_UPPER_R = valueOf("%I:%M:%S %p");
	private static final LuaString FORMAT_TIME_LOWER_R = valueOf("%H:%M");
	private static final LuaString FORMAT_TIME_T = valueOf("%H:%M:%S");

	private static final FormatDesc ZERO_TWO = FormatDesc.ofUnsafe("02d");
	private static final FormatDesc ZERO_THREE = FormatDesc.ofUnsafe("03d");
	private static final FormatDesc SPACE_TWO = FormatDesc.ofUnsafe("2d");

	private void date(LuaState state, Buffer result, Calendar date, LuaString format) throws LuaError {
		byte[] fmt = format.bytes;
		int n = format.length + format.offset;
		for (int i = format.offset; i < n; ) {
			byte c;
			switch (c = fmt[i++]) {
				case '\n':
					result.append("\n");
					break;
				default:
					result.append(c);
					break;
				case '%':
					if (i >= n) break;
					switch (c = fmt[i++]) {
						default:
							throw argError(1, "invalid conversion specifier '%" + (char) c + "'");

						case '%':
							result.append((byte) '%');
							break;
						case 'a':
							result.append(WEEKDAY_NAME_ABBREV[date.get(Calendar.DAY_OF_WEEK) - 1]);
							break;
						case 'A':
							result.append(WEEKDAY_NAME[date.get(Calendar.DAY_OF_WEEK) - 1]);
							break;
						case 'b':
						case 'h':
							result.append(MONTH_NAME_ABBREV[date.get(Calendar.MONTH)]);
							break;
						case 'B':
							result.append(MONTH_NAME[date.get(Calendar.MONTH)]);
							break;
						case 'c':
							date(state, result, date, FORMAT_C);
							break;
						case 'C':
							ZERO_TWO.format(result, (date.get(Calendar.YEAR) / 100) % 100);
							break;
						case 'd':
							ZERO_TWO.format(result, date.get(Calendar.DAY_OF_MONTH));
							break;
						case 'D':
						case 'x':
							date(state, result, date, FORMAT_DATE_D);
							break;
						case 'e':
							SPACE_TWO.format(result, date.get(Calendar.DAY_OF_MONTH));
							break;
						case 'F':
							date(state, result, date, FORMAT_DATE_F);
							break;
						case 'g':
							ZERO_TWO.format(result, date.isWeekDateSupported() ? date.getWeekYear() % 100 : 0);
							break;
						case 'G':
							result.append(Integer.toString(date.isWeekDateSupported() ? date.getWeekYear() : 0));
							break;
						case 'H':
							ZERO_TWO.format(result, date.get(Calendar.HOUR_OF_DAY));
							break;
						case 'I':
							ZERO_TWO.format(result, date.get(Calendar.HOUR_OF_DAY) % 12);
							break;
						case 'j':
							ZERO_THREE.format(result, date.get(Calendar.DAY_OF_YEAR));
							break;
						case 'm':
							ZERO_TWO.format(result, date.get(Calendar.MONTH) + 1);
							break;
						case 'M':
							ZERO_TWO.format(result, date.get(Calendar.MINUTE));
							break;
						case 'n':
							result.append('\n');
							break;
						case 'p':
							result.append(date.get(Calendar.HOUR_OF_DAY) < 12 ? "AM" : "PM");
							break;
						case 'r':
							date(state, result, date, FORMAT_TIME_UPPER_R);
							break;
						case 'R':
							date(state, result, date, FORMAT_TIME_LOWER_R);
							break;
						case 'S':
							ZERO_TWO.format(result, date.get(Calendar.SECOND));
							break;
						case 't':
							result.append('\t');
							break;
						case 'T':
						case 'X':
							date(state, result, date, FORMAT_TIME_T);
							break;
						case 'u': {
							int day = date.get(Calendar.DAY_OF_WEEK);
							result.append(day == Calendar.SUNDAY ? "7" : Integer.toString(day - 1));
							break;
						}
						case 'U':
							ZERO_TWO.format(result, weekNumber(state, date, 0));
							break;
						case 'V':
							ZERO_TWO.format(result, weekNumber(state, date, 1));
							break;
						case 'w':
							result.append(Integer.toString((date.get(Calendar.DAY_OF_WEEK) + 6) % 7));
							break;
						case 'W':
							ZERO_TWO.format(result, weekNumber(state, date, 1));
							break;
						case 'y':
							ZERO_TWO.format(result, date.get(Calendar.YEAR) % 100);
							break;
						case 'Y':
							result.append(Integer.toString(date.get(Calendar.YEAR)));
							break;
						case 'z': {
							final int tzo = timeZoneOffset(date) / 60;
							final int a = Math.abs(tzo);
							result.append(tzo >= 0 ? '+' : '-');
							ZERO_TWO.format(result, a / 60);
							ZERO_TWO.format(result, a % 60);
							break;
						}
						case 'Z':
							result.append(date.getTimeZone().getID());
							break;
					}
			}
		}
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
	private long time(LuaState state, LuaTable table) throws LuaError {
		if (table == null) return System.currentTimeMillis() / 1000;

		Calendar c = Calendar.getInstance(state.timezone, Locale.ROOT);
		c.set(Calendar.YEAR, getField(table, "year", -1));
		c.set(Calendar.MONTH, getField(table, "month", -1) - 1);
		c.set(Calendar.DAY_OF_MONTH, getField(table, "day", -1));
		c.set(Calendar.HOUR_OF_DAY, getField(table, "hour", 12));
		c.set(Calendar.MINUTE, getField(table, "min", 0));
		c.set(Calendar.SECOND, getField(table, "sec", 0));
		c.set(Calendar.MILLISECOND, 0);
		return c.getTimeInMillis() / 1000;
	}

	private static int getField(LuaTable table, String field, int def) throws LuaError {
		LuaValue value = table.rawget(field);
		if (value.isNumber()) {
			return value.toInteger();
		} else {
			if (def < 0) throw new LuaError("field \"" + field + "\" missing in date table");
			return def;
		}
	}
}
