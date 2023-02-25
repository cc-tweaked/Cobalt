package org.squiddev.cobalt.compiler;

import org.squiddev.cobalt.LuaNumber;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.lib.Utf8Lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A Lexer for Lua code.
 * <p>
 * This largely follows the structure and implementation of llex.c.
 */
final class Lex {
	private static final int EOZ = -1;
	static final int MAX_INT = Integer.MAX_VALUE - 2;

	private static final int POSITION_SHIFT = 32;
	private static final long POSITION_MASK = 0xFFFFFFFFL;

	// Terminal symbols denoted by reserved words
	static final int
		TK_AND = 257, TK_BREAK = 258, TK_DO = 259, TK_ELSE = 260, TK_ELSEIF = 261,
		TK_END = 262, TK_FALSE = 263, TK_FOR = 264, TK_FUNCTION = 265, TK_IF = 266,
		TK_IN = 267, TK_LOCAL = 268, TK_NIL = 269, TK_NOT = 270, TK_OR = 271, TK_REPEAT = 272,
		TK_RETURN = 273, TK_THEN = 274, TK_TRUE = 275, TK_UNTIL = 276, TK_WHILE = 277;
	// Other terminal symbols
	static final int
		TK_CONCAT = 278, TK_DOTS = 279, TK_EQ = 280, TK_GE = 281, TK_LE = 282, TK_NE = 283,
		TK_EOS = 284,
		TK_NUMBER = 285, TK_NAME = 286, TK_STRING = 287;

	/* Token names: this must be consistent with the list above. */
	private final static String[] tokenNames = {
		"and", "break", "do", "else", "elseif",
		"end", "false", "for", "function", "if",
		"in", "local", "nil", "not", "or", "repeat",
		"return", "then", "true", "until", "while",
		"..", "...", "==", ">=", "<=", "~=",
		"<eof>",
		"<number>", "<name>", "<string>",
	};

	private final static int FIRST_RESERVED = TK_AND;
	private final static int NUM_RESERVED = TK_WHILE + 1 - FIRST_RESERVED;

	private final static Map<LuaString, Integer> RESERVED;

	static {
		Map<LuaString, Integer> reserved = new HashMap<>();
		for (int i = 0; i < NUM_RESERVED; i++) {
			reserved.put(ValueFactory.valueOf(tokenNames[i]), FIRST_RESERVED + i);
		}
		RESERVED = Collections.unmodifiableMap(reserved);
	}

	static class Token {
		private int token;
		private LuaValue value;
		private long position;

		private void set(Token other) {
			token = other.token;
			value = other.value;
			position = other.position;
		}

		int token() {
			return token;
		}

		int line() {
			return (int) (position & POSITION_MASK);
		}

		long position() {
			return position;
		}

		LuaString stringContents() {
			return (LuaString) value;
		}

		LuaNumber numberContents() {
			return (LuaNumber) value;
		}
	}

	/**
	 * The current lookahead character.
	 */
	private int current;

	/**
	 * Current source name
	 */
	final LuaString source;

	/**
	 * The buffer we're reading from
	 */
	private final InputStream z;

	/**
	 * Input line counter
	 */
	private int lineNumber = 1;

	/**
	 * Input column counter.
	 */
	private int columnNumber = 1;

	private long lastPosition = 1 | 1L << POSITION_SHIFT;

	private final HashMap<LuaString, LuaString> strings = new HashMap<>();

	final Token token = new Token();
	final Token lookahead = new Token();

	private byte[] buff = new byte[32];  /* buffer for tokens */
	private int bufferSize; /* length of buffer */

	Lex(LuaString source, InputStream z, int current) {
		this.source = source;
		this.z = z;
		this.current = current;

		token.token = 0;
		lookahead.token = TK_EOS;
	}

	private void next() {
		try {
			current = z.read();
			columnNumber++;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void save(int toSave) {
		if (buff == null || bufferSize + 1 > buff.length) buff = LuaC.realloc(buff, bufferSize * 2 + 1);
		buff[bufferSize++] = (byte) toSave;
	}

	private boolean currIsNewline() {
		return current == '\n' || current == '\r';
	}

	private void saveAndNext() {
		save(current);
		next();
	}

	static String token2str(int token) {
		if (token < FIRST_RESERVED) {
			if (token >= ' ' && token <= '~') {
				return "'" + (char) token + "'";
			} else {
				return "<\\" + token + ">'";
			}
		} else {
			String name = tokenNames[token - FIRST_RESERVED];
			return token < TK_EOS ? "'" + name + "'" : name;
		}
	}

	private String txtToken(int token) {
		switch (token) {
			case TK_NAME:
			case TK_STRING:
			case TK_NUMBER:
				return "'" + LuaString.valueOf(buff, 0, bufferSize) + "'";
			default:
				return token2str(token);
		}
	}

	CompileException lexError(String msg, int token) {
		LuaString cid = LoadState.getShortName(source);
		String message = cid + ":" + lineNumber + ": " + msg;
		if (token != 0) message += " near " + txtToken(token);
		return new CompileException(message);
	}

	CompileException syntaxError(String msg) {
		return lexError(msg, token.token());
	}

	/**
	 * Create and intern a string.
	 * <p>
	 * This both saves memory and allows us to use {@code ==} for string comparison in the rest of the
	 * parser.
	 *
	 * @param bytes  The buffer to create the string from. Unlike {@link LuaString#valueOf(byte[])}, this may be
	 *               modified afterwards.
	 * @param offset The offset into this array.
	 * @param len    The length of the string.
	 * @return The created or interned string.
	 */
	LuaString newString(byte[] bytes, int offset, int len) {
		LuaString string = LuaString.valueOf(bytes, offset, len);
		LuaString interned = strings.get(string);
		if (interned == null) {
			// must copy bytes, since bytes could be from reusable buffer
			byte[] slice = new byte[len];
			System.arraycopy(bytes, offset, slice, 0, len);
			interned = LuaString.valueOf(slice);
			strings.put(interned, interned);
		}
		return interned;
	}

	LuaString newString(String value) {
		LuaString string = LuaString.valueOf(value);
		return strings.computeIfAbsent(string, Function.identity());
	}

	/**
	 * Increment line number and skips newline sequence (any of \n, \r, \n\r, or \r\n)
	 */
	private void inclineNumber() throws CompileException {
		int old = current;
		assert currIsNewline();
		next(); /* skip '\n' or '\r' */
		if (currIsNewline() && current != old) next(); // skip '\n\r' or '\r\n'
		if (++lineNumber >= MAX_INT) throw lexError("chunk has too many lines", 0);
		columnNumber = 1;
	}

	private boolean checkNext(char character) {
		if (current == character) {
			next();
			return true;
		}

		return false;
	}

	private boolean checkNext(char c1, char c2) {
		if (current == c1 || current == c2) {
			saveAndNext();
			return true;
		}

		return false;
	}

	private LuaNumber parseNumber(String str) throws CompileException {
		// If we're a hex string, try to parse as a long. Java's handling of hex floats
		// is much more limited than C's version.
		if (str.startsWith("0x") || str.startsWith("0X")) {
			try {
				return ValueFactory.valueOf(Long.valueOf(str.substring(2), 16));
			} catch (NumberFormatException ignored) {
			}
		}

		try {
			return ValueFactory.valueOf(Double.parseDouble(str));
		} catch (NumberFormatException ignored) {
		}

		throw lexError("malformed number", TK_NUMBER);
	}

	private LuaNumber readNumeral() throws CompileException {
		assert isDigit(current);

		int first = current;
		saveAndNext();
		char exp1 = 'E', exp2 = 'e';
		if (first == '0' && checkNext('x', 'X')) {
			exp1 = 'P';
			exp2 = 'p';
		}

		while (true) {
			// Exponential and sign
			if (checkNext(exp1, exp2)) checkNext('+', '-');

			if (isHex(current) || current == '.') {
				saveAndNext();
			} else {
				break;
			}
		}

		return parseNumber(new String(buff, 0, bufferSize));
	}

	/**
	 * Read a sequence '[=*[' or ']=*]', leaving the last bracket.
	 *
	 * @return If the sequence is well formed, the number of '='s + 2. If not, then 1 if it is a single bracket (no '='s
	 * and no 2nd bracket); otherwise (an unfinished '[==...') 0.
	 */
	private int skipSep() {
		int count = 0;
		int s = current;
		assert s == '[' || s == ']';
		saveAndNext();
		while (current == '=') {
			saveAndNext();
			count++;
		}

		if (current == s) return count + 2;
		return count == 0 ? 1 : 0;
	}

	private void readLongString(Token token, int sep) throws CompileException {
		int line = lineNumber;
		saveAndNext(); // skip 2nd `['
		if (currIsNewline()) inclineNumber(); // Skip leading string if needed

		while (true) {
			switch (current) {
				case EOZ:
					throw lexError("unfinished long " + (token != null ? "string" : "comment") + " (started at line " + line + ")", TK_EOS);
				case '[': {
					if (skipSep() == sep) {
						saveAndNext(); // skip 2nd `['
						if (sep == 2) throw lexError("nesting of [[...]] is deprecated", '[');
					}
					break;
				}
				case ']': {
					if (skipSep() == sep) {
						saveAndNext(); // skip 2nd `]'
						if (token != null) token.value = newString(buff, sep, bufferSize - 2 * sep);
						return;
					}
					break;
				}
				case '\n':
				case '\r': {
					save('\n');
					inclineNumber();
					if (token == null) bufferSize = 0; // avoid wasting space
					break;
				}
				default: {
					if (token != null) {
						saveAndNext();
					} else {
						next();
					}
				}
			}
		}
	}

	private CompileException escapeError(String message) {
		if (current != EOZ) saveAndNext();
		return lexError(message, TK_STRING);
	}

	private int readHex() throws CompileException {
		saveAndNext();
		if (!isHex(current)) throw escapeError("hexadecimal digit expected");
		return hexValue(current);
	}

	private int readHexEsc() throws CompileException {
		int r = (readHex() << 4) | readHex();
		bufferSize -= 2;
		return r;
	}

	private void readUtf8Esc() throws CompileException {
		saveAndNext();
		if (current != '{') throw escapeError("mising '{'");

		int i = 4;
		long codepoint = readHex();
		while (true) {
			saveAndNext();
			if (!isHex(current)) break;

			i++;
			codepoint = (codepoint << 4) | hexValue(current);
			if (codepoint > Utf8Lib.MAX_UNICODE) throw escapeError("UTF-8 value too large");
		}
		if (current != '}') throw escapeError("missing '}'");
		next();

		bufferSize -= i;
		if (codepoint < 0x80) {
			save((int) codepoint);
		} else {
			byte[] buffer = new byte[8];
			int j = Utf8Lib.buildCharacter(buffer, codepoint);
			for (; j > 0; j--) save(buffer[8 - j]);
		}
	}

	private int readDecEsc() throws CompileException {
		int i = 0;
		int result = 0;
		for (; i < 3 && isDigit(current); i++) {
			result = 10 * result + current - '0';
			saveAndNext();
		}

		if (result > 255) throw escapeError("escape sequence too large");
		bufferSize -= i;

		return result;
	}

	private LuaString readString(int del) throws CompileException {
		saveAndNext();
		while (current != del) {
			switch (current) {
				case EOZ:
					throw lexError("unfinished string", TK_EOS);
				case '\n':
				case '\r':
					throw lexError("unfinished string", TK_STRING);
				case '\\': {
					saveAndNext();
					switch (current) {
						case 'a': /* bell */
							saveEscape('\u0007');
							break;
						case 'b': /* backspace */
							saveEscape('\b');
							break;
						case 'f': /* form feed */
							saveEscape('\f');
							break;
						case 'n': /* newline */
							saveEscape('\n');
							break;
						case 'r': /* carriage return */
							saveEscape('\r');
							break;
						case 't': /* tab */
							saveEscape('\t');
							break;
						case 'v': /* vertical tab */
							saveEscape('\u000B');
							break;
						case 'x':
							saveEscape(readHexEsc());
							break;
						case 'u':
							readUtf8Esc();
							break;
						case '\n': /* go through */
						case '\r':
							bufferSize--;
							save('\n');
							inclineNumber();
							break;
						case EOZ:
							break; /* will raise an error next loop */
						case 'z': { // "zap" following span of spaces
							bufferSize--;
							next(); // Skip z and remove '\\'
							while (current != EOZ && isSpace(current)) {
								if (currIsNewline()) inclineNumber();
								next();
							}
							break;
						}
						default: {
							if (!isDigit(current)) {
								bufferSize--;
								saveAndNext(); /* handles \\, \", \', and \? */
							} else { /* \xxx */
								int c = readDecEsc();
								bufferSize--;
								save(c);
							}
							break;
						}
					}
					break;
				}
				default:
					saveAndNext();
			}
		}

		saveAndNext(); /* skip delimiter */
		return newString(buff, 1, bufferSize - 2);
	}

	private void saveEscape(int character) {
		next();
		bufferSize--;
		save(character);
	}

	private int lexToken(Token token) throws CompileException {
		bufferSize = 0;
		while (true) {
			token.position = packPosition(lineNumber, columnNumber);

			switch (current) {
				case '\n':
				case '\r': {
					inclineNumber();
					continue;
				}
				case '-': {
					next();
					if (current != '-') return '-';

					// Two '-'s, so must be a comment
					next();
					if (current == '[') {
						int sep = skipSep();
						bufferSize = 0; // 'skipSep' may dirty the buffer

						if (sep >= 2) {
							// Consume a block comment
							readLongString(null, sep);
							bufferSize = 0;
							continue;
						}
					}

					// Consume a line comment
					while (!currIsNewline() && current != EOZ) next();
					continue;
				}
				case '[': {
					int sep = skipSep();
					if (sep >= 2) {
						readLongString(token, sep);
						return TK_STRING;
					} else if (sep == 0) {
						throw lexError("invalid long string delimiter", TK_STRING);
					} else {
						return '[';
					}
				}
				case '=': {
					next();
					return checkNext('=') ? TK_EQ : '=';
				}
				case '<': {
					next();
					return checkNext('=') ? TK_LE : '<';
				}
				case '>': {
					next();
					return checkNext('=') ? TK_GE : '>';
				}
				case '~': {
					next();
					return checkNext('=') ? TK_NE : '~';
				}
				case '"':
				case '\'': {
					token.value = readString(current);
					return TK_STRING;
				}
				case '.': {
					saveAndNext();
					if (checkNext('.')) {
						if (checkNext('.')) {
							return TK_DOTS; /* ... */
						} else {
							return TK_CONCAT; /* .. */
						}
					} else if (!isDigit(current)) {
						return '.';
					} else {
						token.value = readNumeral();
						return TK_NUMBER;
					}
				}
				case EOZ: {
					return TK_EOS;
				}
				default: {
					if (isSpace(current)) {
						next();
						continue;
					}

					if (isDigit(current)) {
						token.value = readNumeral();
						return TK_NUMBER;
					}

					if (isAlpha(current) || current == '_') {
						/* identifier or reserved word */
						do {
							saveAndNext();
						} while (isAlphaNum(current) || current == '_');
						LuaString ts = newString(buff, 0, bufferSize);
						if (RESERVED.containsKey(ts)) {
							return RESERVED.get(ts);
						} else {
							token.value = ts;
							return TK_NAME;
						}
					} else {
						int c = current;
						next();
						return c; /* single-char tokens (+ - / ...) */
					}
				}
			}
		}
	}

	int lastLine() {
		return unpackLine(lastPosition);
	}

	long lastPosition() {
		return lastPosition;
	}

	void skipShebang() {
		if (current == '#') {
			while (!currIsNewline() && current != EOZ) next();
		}
	}

	void nextToken() throws CompileException {
		lastPosition = packPosition(lineNumber, columnNumber);
		if (lookahead.token != TK_EOS) { // is there a look-ahead token?
			token.set(lookahead);
			lookahead.token = TK_EOS; // and discharge it
		} else {
			token.token = lexToken(token); // read next token
		}
	}

	void lookahead() throws CompileException {
		LuaC._assert(lookahead.token == TK_EOS);
		lookahead.token = lexToken(lookahead);
	}

	private static boolean isAlphaNum(int c) {
		return c >= '0' && c <= '9'
			|| c >= 'a' && c <= 'z'
			|| c >= 'A' && c <= 'Z'
			|| c == '_';
	}

	private static boolean isAlpha(int c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
	}

	private static boolean isDigit(int c) {
		return c >= '0' && c <= '9';
	}

	private static boolean isSpace(int c) {
		return c <= ' ';
	}

	private static boolean isHex(int c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
	}

	private static int hexValue(int c) {
		// Terrible bit twiddling right here:
		// 'A'..'F' corresponds to 0x41..0x46, and 'a'..'f' to 0x61..0x66. So bitwise and with 0xf
		// gives us the last digit, +9 to map from 1..6 to 10..15.
		return c <= '9' ? c - '0' : (c & 0xf) + 9;
	}

	static long packPosition(int line, int column) {
		return line | (long) column << POSITION_SHIFT;
	}

	static int unpackLine(long position) {
		return (int) (position & POSITION_MASK);
	}

	static int unpackColumn(long position) {
		return (int) ((position >> POSITION_SHIFT) & POSITION_MASK);
	}
}
