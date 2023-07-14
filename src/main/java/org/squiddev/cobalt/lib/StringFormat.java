package org.squiddev.cobalt.lib;

import org.squiddev.cobalt.*;

import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.lib.StringLib.L_ESC;

class StringFormat {
	static class FormatState {
		final LuaString format;
		int i = 0;

		final Buffer buffer;

		int arg = 1;
		final Varargs args;
		FormatDesc current;

		FormatState(LuaString format, Buffer buffer, Varargs args) {
			this.args = args;
			this.format = format;
			this.buffer = buffer;
		}
	}

	/**
	 * string.format (formatstring, ...)
	 * <p>
	 * Returns a formatted version of its variable number of arguments following
	 * the description given in its first argument (which must be a string).
	 * The format string follows the same rules as the printf family of standard C functions.
	 * The only differences are that the options/modifiers *, l, L, n, p, and h are not supported
	 * and that there is an extra option, q. The q option formats a string in a form suitable
	 * to be safely read back by the Lua interpreter: the string is written between double quotes,
	 * and all double quotes, newlines, embedded zeros, and backslashes in the string are correctly
	 * escaped when written. For instance, the call
	 * string.format('%q', 'a string with "quotes" and \n new line')
	 * <p>
	 * will produce the string:
	 * "a string with \"quotes\" and \
	 * new line"
	 * <p>
	 * The options c, d, E, e, f, g, G, i, o, u, X, and x all expect a number as argument,
	 * whereas q and s expect a string.
	 * <p>
	 * This function does not accept string values containing embedded zeros,
	 * except as arguments to the q option.
	 *
	 * @throws LuaError On invalid arguments.
	 */
	static LuaString format(LuaState state, FormatState format) throws LuaError, UnwindThrowable {
		LuaString fmt = format.format;
		final int n = fmt.length();
		Buffer result = format.buffer;

		for (int i = format.i; i < n; ) {
			int c = fmt.charAt(i++);
			if (c != L_ESC) {
				result.append((byte) c);
				continue;
			}

			if (i >= n) throw new LuaError("invalid option '%' to 'format'");

			if (fmt.charAt(i) == L_ESC) {
				i++;
				result.append((byte) L_ESC);
				continue;
			}

			LuaValue value = format.args.arg(++format.arg);
			FormatDesc fdsc = new FormatDesc(fmt, i);
			i += fdsc.length;

			switch (fdsc.conversion) {
				case 'c' -> fdsc.format(result, (byte) value.checkLong());
				case 'i', 'd', 'o', 'u', 'x', 'X' -> fdsc.format(result, value.checkLong());
				case 'e', 'E', 'f', 'g', 'G' -> fdsc.format(result, value.checkDouble());
				case 'q' -> addQuoted(result, format.arg, value);
				case 's' -> {
					try {
						addString(result, fdsc, OperationHelper.checkToString(OperationHelper.toString(state, value)));
					} catch (UnwindThrowable e) {
						format.current = fdsc;
						format.i = i;
						throw e;
					}
				}
				default -> throw new LuaError("invalid option '%" + (char) fdsc.conversion + "' to 'format'");
			}
		}

		return result.toLuaString();
	}

	static void addString(Buffer result, FormatDesc fdsc, LuaString s) {
		if (fdsc.precision == -1 && s.length() >= 100) {
			result.append(s);
		} else {
			fdsc.format(result, s);
		}
	}

	private static void addQuoted(Buffer buf, int arg, LuaValue s) throws LuaError {
		switch (s.type()) {
			case TSTRING -> addQuoted(buf, s.checkLuaString());
			case TNUMBER -> {
				if (s instanceof LuaInteger) {
					buf.append(Integer.toString(s.checkInteger()));
				} else {
					double value = s.checkDouble();
					buf.append((long) value == value ? Long.toString((long) value) : Double.toHexString(value));
				}
			}
			case TBOOLEAN, TNIL -> buf.append(s.toString());
			default -> throw ErrorFactory.argError(arg, "value has no literal representation");
		}
	}

	private static void addQuoted(Buffer buf, LuaString s) {
		int c;
		buf.append((byte) '"');
		for (int i = 0, n = s.length(); i < n; i++) {
			switch (c = s.charAt(i)) {
				case '"', '\\', '\n' -> {
					buf.append((byte) '\\');
					buf.append((byte) c);
				}
				case '\r' -> buf.append("\\r");
				case '\0' -> buf.append("\\000");
				default -> buf.append((byte) c);
			}
		}
		buf.append((byte) '"');
	}
}
