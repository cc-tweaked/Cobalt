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
package org.squiddev.cobalt.lib.system;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.lib.CoreLibraries;
import org.squiddev.cobalt.lib.FormatDesc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.squiddev.cobalt.Constants.NIL;
import static org.squiddev.cobalt.Constants.ZERO;
import static org.squiddev.cobalt.ErrorFactory.argError;
import static org.squiddev.cobalt.ValueFactory.*;

/**
 * A Lua library which implements the standard lua {@code os} library.
 * <p>
 * Because the nature of the {@code os} library is to encapsulate os-specific features, the behavior of these functions
 * may vary from their counterparts in the C platform.
 *
 * @see LibFunction
 * @see CoreLibraries
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.8">http://www.lua.org/manual/5.1/manual.html#5.8</a>
 */
public class OsLib {
	private static final LuaString DATE_FORMAT = valueOf("%c");
	private static final LuaString DATE_TABLE = valueOf("*t");

	private final long startTime = System.nanoTime();

	public void add(LuaState state, LuaTable env) throws LuaError {
		LuaTable t = RegisteredFunction.bind(new RegisteredFunction[]{
			RegisteredFunction.of("clock", this::clock),
			RegisteredFunction.of("date", OsLib::date),
			RegisteredFunction.of("difftime", OsLib::difftime),
			RegisteredFunction.ofV("execute", OsLib::execute),
			RegisteredFunction.of("exit", OsLib::exit),
			RegisteredFunction.of("getenv", OsLib::getenv),
			RegisteredFunction.ofV("remove", OsLib::remove),
			RegisteredFunction.ofV("rename", OsLib::rename),
			RegisteredFunction.of("setlocale", OsLib::setlocale),
			RegisteredFunction.ofV("time", OsLib::time),
			RegisteredFunction.ofV("tmpname", OsLib::tmpname),
		});

		LibFunction.setGlobalLibrary(state, env, "os", t);
	}

	private LuaValue clock(LuaState state) {
		return valueOf((long) ((System.nanoTime() - startTime) * 1e-9));
	}

	private static LuaValue date(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		LuaString format = arg1.optLuaString(DATE_FORMAT);
		long time = arg2.optLong(formatTime(null));

		Calendar d = Calendar.getInstance(Locale.ROOT);
		d.setTime(new Date(time * 1000));
		if (format.startsWith((byte) '!')) {
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

		Buffer buffer = new Buffer(format.length());
		formatDate(buffer, d, format);
		return buffer.toLuaString();
	}

	private static LuaValue difftime(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		return valueOf(arg1.checkLong() - arg2.checkLong());
	}

	private static Varargs execute(LuaState state, Varargs args) throws LuaError {
		return valueOf(execute(args.arg(1).optString(null)));
	}

	private static int execute(String command) {
		Runtime r = Runtime.getRuntime();
		try {
			final Process p = r.exec(command);
			try {
				p.waitFor();
				return p.exitValue();
			} finally {
				p.destroy();
			}
		} catch (IOException ioe) {
			return -1;
		} catch (InterruptedException e) {
			return -2;
		} catch (Throwable t) {
			return -3;
		}
	}

	private static LuaValue exit(LuaState state, LuaValue arg) throws LuaError {
		System.exit(arg.optInteger(0));
		return Constants.NIL;
	}

	private static LuaValue getenv(LuaState state, LuaValue arg) throws LuaError {
		final String val = System.getenv(arg.checkString());
		return val != null ? valueOf(val) : Constants.NIL;
	}

	private static Varargs remove(LuaState state, Varargs args) throws LuaError {
		Path file = Paths.get(args.first().checkString());

		try {
			Files.delete(file);
			return Constants.TRUE;
		} catch (FileNotFoundException e) {
			return errorResult("file not found");
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	private static Varargs rename(LuaState state, Varargs args) throws LuaError {
		String from = args.arg(1).checkString();
		String to = args.arg(2).checkString();

		try {
			Files.move(Paths.get(from), Paths.get(to), StandardCopyOption.REPLACE_EXISTING);
			return Constants.TRUE;
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	private static LuaValue setlocale(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		String locale = arg1.optString(null);
		arg2.optString("all");
		return locale == null || locale.equals("C") ? valueOf("C") : Constants.NIL;
	}

	private static Varargs time(LuaState state, Varargs args) throws LuaError {
		return valueOf(formatTime(args.first().isNil() ? null : args.arg(1).checkTable()));
	}

	private static Varargs tmpname(LuaState state, Varargs args) {
		try {
			Path path = Files.createTempFile(null, "cobalt");
			path.toFile().deleteOnExit();
			return valueOf(path.toString());
		} catch (IOException e) {
			return errorResult(e);
		}
	}

	private static Varargs errorResult(Exception ioe) {
		String s = ioe.getMessage();
		return errorResult("io error: " + (s != null ? s : ioe.toString()));
	}

	private static Varargs errorResult(String message) {
		return varargsOf(NIL, valueOf(message), ZERO);
	}

	private static final String[] WEEKDAY_NAME_ABBREV = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
	private static final String[] WEEKDAY_NAME = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
	private static final String[] MONTH_NAME_ABBREV = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	private static final String[] MONTH_NAME = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

	private static Calendar beginningOfYear(Calendar d) {
		Calendar y0 = Calendar.getInstance(Locale.ROOT);
		y0.setTime(d.getTime());
		y0.set(Calendar.MONTH, 0);
		y0.set(Calendar.DAY_OF_MONTH, 1);
		y0.set(Calendar.HOUR_OF_DAY, 0);
		y0.set(Calendar.MINUTE, 0);
		y0.set(Calendar.SECOND, 0);
		y0.set(Calendar.MILLISECOND, 0);
		return y0;
	}

	private static int weekNumber(Calendar d, int startDay) {
		Calendar y0 = beginningOfYear(d);
		y0.set(Calendar.DAY_OF_MONTH, 1 + (startDay + 8 - y0.get(Calendar.DAY_OF_WEEK)) % 7);
		if (y0.after(d)) {
			y0.set(Calendar.YEAR, y0.get(Calendar.YEAR) - 1);
			y0.set(Calendar.DAY_OF_MONTH, 1 + (startDay + 8 - y0.get(Calendar.DAY_OF_WEEK)) % 7);
		}
		long dt = d.getTime().getTime() - y0.getTime().getTime();
		return 1 + (int) (dt / (7L * 24L * 3600L * 1000L));
	}

	private static int timeZoneOffset(Calendar d) {
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

	private static boolean isDaylightSavingsTime(Calendar d) {
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

	private static void formatDate(Buffer result, Calendar date, LuaString format) throws LuaError {
		int n = format.length();
		for (int i = 0; i < format.length(); ) {
			byte c;
			switch (c = format.byteAt(i++)) {
				case '\n' -> result.append('\n');
				default -> result.append(c);
				case '%' -> {
					if (i >= n) break;
					switch (c = format.byteAt(i++)) {
						default -> throw argError(1, "invalid conversion specifier '%" + (char) c + "'");
						case '%' -> result.append('%');
						case 'a' -> result.append(WEEKDAY_NAME_ABBREV[date.get(Calendar.DAY_OF_WEEK) - 1]);
						case 'A' -> result.append(WEEKDAY_NAME[date.get(Calendar.DAY_OF_WEEK) - 1]);
						case 'b', 'h' -> result.append(MONTH_NAME_ABBREV[date.get(Calendar.MONTH)]);
						case 'B' -> result.append(MONTH_NAME[date.get(Calendar.MONTH)]);
						case 'c' -> formatDate(result, date, FORMAT_C);
						case 'C' -> ZERO_TWO.format(result, (date.get(Calendar.YEAR) / 100) % 100);
						case 'd' -> ZERO_TWO.format(result, date.get(Calendar.DAY_OF_MONTH));
						case 'D', 'x' -> formatDate(result, date, FORMAT_DATE_D);
						case 'e' -> SPACE_TWO.format(result, date.get(Calendar.DAY_OF_MONTH));
						case 'F' -> formatDate(result, date, FORMAT_DATE_F);
						case 'g' -> ZERO_TWO.format(result, date.isWeekDateSupported() ? date.getWeekYear() % 100 : 0);
						case 'G' ->
							result.append(Integer.toString(date.isWeekDateSupported() ? date.getWeekYear() : 0));
						case 'H' -> ZERO_TWO.format(result, date.get(Calendar.HOUR_OF_DAY));
						case 'I' -> ZERO_TWO.format(result, date.get(Calendar.HOUR_OF_DAY) % 12);
						case 'j' -> ZERO_THREE.format(result, date.get(Calendar.DAY_OF_YEAR));
						case 'm' -> ZERO_TWO.format(result, date.get(Calendar.MONTH) + 1);
						case 'M' -> ZERO_TWO.format(result, date.get(Calendar.MINUTE));
						case 'n' -> result.append('\n');
						case 'p' -> result.append(date.get(Calendar.HOUR_OF_DAY) < 12 ? "AM" : "PM");
						case 'r' -> formatDate(result, date, FORMAT_TIME_UPPER_R);
						case 'R' -> formatDate(result, date, FORMAT_TIME_LOWER_R);
						case 'S' -> ZERO_TWO.format(result, date.get(Calendar.SECOND));
						case 't' -> result.append('\t');
						case 'T', 'X' -> formatDate(result, date, FORMAT_TIME_T);
						case 'u' -> {
							int day = date.get(Calendar.DAY_OF_WEEK);
							result.append(day == Calendar.SUNDAY ? "7" : Integer.toString(day - 1));
						}
						case 'U' -> ZERO_TWO.format(result, weekNumber(date, 0));
						case 'V' -> ZERO_TWO.format(result, weekNumber(date, 1));
						case 'w' -> result.append(Integer.toString((date.get(Calendar.DAY_OF_WEEK) + 6) % 7));
						case 'W' -> ZERO_TWO.format(result, weekNumber(date, 1));
						case 'y' -> ZERO_TWO.format(result, date.get(Calendar.YEAR) % 100);
						case 'Y' -> result.append(Integer.toString(date.get(Calendar.YEAR)));
						case 'z' -> {
							final int tzo = timeZoneOffset(date) / 60;
							final int a = Math.abs(tzo);
							result.append(tzo >= 0 ? '+' : '-');
							ZERO_TWO.format(result, a / 60);
							ZERO_TWO.format(result, a % 60);
						}
						case 'Z' -> result.append(date.getTimeZone().getID());
					}
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
	private static long formatTime(LuaTable table) throws LuaError {
		if (table == null) return System.currentTimeMillis() / 1000;

		Calendar c = Calendar.getInstance(Locale.ROOT);
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
