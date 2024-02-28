package org.squiddev.cobalt.compiler;

import cc.tweaked.cobalt.internal.string.CharProperties;
import cc.tweaked.cobalt.internal.string.NumberParser;
import cc.tweaked.cobalt.internal.unwind.AutoUnwind;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.lib.Utf8Lib;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A Lexer for Lua code.
 * <p>
 * This largely follows the structure and implementation of llex.c.
 */
@AutoUnwind
final class Lex {
	private static final int EOZ = -1;
	static final int MAX_INT = Integer.MAX_VALUE - 2;

	private static final int POSITION_SHIFT = 32;
	private static final long POSITION_MASK = 0xFFFFFFFFL;

	// Terminal symbols denoted by reserved words
	static final int
		TK_AND = 257, TK_BREAK = 258, TK_DO = 259, TK_ELSE = 260, TK_ELSEIF = 261,
		TK_END = 262, TK_FALSE = 263, TK_FOR = 264, TK_FUNCTION = 265, TK_GOTO = 266, TK_IF = 267,
		TK_IN = 268, TK_LOCAL = 269, TK_NIL = 270, TK_NOT = 271, TK_OR = 272, TK_REPEAT = 273,
		TK_RETURN = 274, TK_THEN = 275, TK_TRUE = 276, TK_UNTIL = 277, TK_WHILE = 278;
	// Other terminal symbols
	static final int
		TK_CONCAT = 279, TK_DOTS = 280, TK_EQ = 281, TK_GE = 282, TK_LE = 283, TK_NE = 284, TK_DBCOLON = 285,
		TK_EOS = 286,
		TK_NUMBER = 287, TK_NAME = 288, TK_STRING = 289;

	/* Token names: this must be consistent with the list above. */
	private final static String[] tokenNames = {
		"and", "break", "do", "else", "elseif",
		"end", "false", "for", "function", "goto", "if",
		"in", "local", "nil", "not", "or", "repeat",
		"return", "then", "true", "until", "while",
		"..", "...", "==", ">=", "<=", "~=", "::",
		"<eof>",
		"<number>", "<name>", "<string>",
	};

	private final static int FIRST_RESERVED = TK_AND;
	private final static int NUM_RESERVED = TK_WHILE + 1 - FIRST_RESERVED;

	private final static Map<ByteBuffer, Integer> RESERVED;

	static {
		Map<ByteBuffer, Integer> reserved = new HashMap<>();
		for (int i = 0; i < NUM_RESERVED; i++) {
			// We skip GOTO and inject it later on when parsing statements.
			if (FIRST_RESERVED + i == TK_GOTO) continue;

			reserved.put(ValueFactory.valueOf(tokenNames[i]).toBuffer(), FIRST_RESERVED + i);
		}
		RESERVED = Collections.unmodifiableMap(reserved);
	}

	static boolean isReserved(LuaString name) {
		return RESERVED.containsKey(name.toBuffer());
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

	final LuaString shortSource;

	/**
	 * The buffer we're reading from
	 */
	private final InputReader z;

	/**
	 * Input line counter
	 */
	private int lineNumber = 1;

	/**
	 * Input column counter.
	 */
	private int columnNumber = 1;

	private long lastPosition = 1 | 1L << POSITION_SHIFT;

	private final HashMap<ByteBuffer, LuaString> strings = new HashMap<>();

	final Token token = new Token();
	private final Token lookahead = new Token();

	private byte[] buff = new byte[32];  /* buffer for tokens */
	private int bufferSize; /* length of buffer */

	Lex(LuaString source, LuaString shortSource, InputReader z, int current) {
		this.source = source;
		this.shortSource = shortSource;
		this.z = z;
		this.current = current;

		token.token = 0;
		lookahead.token = TK_EOS;
	}

	private void next() throws CompileException, LuaError, UnwindThrowable {
		current = z.read();
		columnNumber++;
	}

	private void save(int toSave) {
		if (buff == null || bufferSize + 1 > buff.length) buff = LuaC.realloc(buff, bufferSize * 2 + 1);
		buff[bufferSize++] = (byte) toSave;
	}

	private boolean currIsNewline() {
		return current == '\n' || current == '\r';
	}

	private void saveAndNext() throws CompileException, LuaError, UnwindThrowable {
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

	Buffer createErrorMessage(int line) {
		return new Buffer().append(shortSource).append(":").append(Integer.toString(line)).append(": ");
	}

	CompileException lexError(String msg, int token) {
		var buffer = createErrorMessage(lineNumber).append(msg);
		if (token != 0) {
			buffer.append(" near ");
			switch (token) {
				case TK_NAME, TK_STRING, TK_NUMBER -> buffer.append("'").append(buff, 0, bufferSize).append("'");
				default -> buffer.append(token2str(token));
			}
		}
		return new CompileException(buffer.toString());
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
		LuaString interned = strings.get(ByteBuffer.wrap(bytes, offset, len));
		if (interned == null) {
			// must copy bytes, since bytes could be from reusable buffer
			byte[] slice = new byte[len];
			System.arraycopy(bytes, offset, slice, 0, len);
			strings.put(ByteBuffer.wrap(slice), interned = LuaString.valueOf(slice));
		}
		return interned;
	}

	LuaString newString(String value) {
		byte[] contents = new byte[value.length()];
		LuaString.encode(value, contents, 0);
		var buffer = ByteBuffer.wrap(contents);

		LuaString interned = strings.get(buffer);
		if (interned == null) strings.put(buffer, interned = LuaString.valueOf(contents));
		return interned;
	}

	/**
	 * Increment line number and skips newline sequence (any of \n, \r, \n\r, or \r\n)
	 */
	private void inclineNumber() throws CompileException, LuaError, UnwindThrowable {
		int old = current;
		assert currIsNewline();
		next(); /* skip '\n' or '\r' */
		if (currIsNewline() && current != old) next(); // skip '\n\r' or '\r\n'
		if (++lineNumber >= MAX_INT) throw lexError("chunk has too many lines", 0);
		columnNumber = 1;
	}

	private boolean checkNext(char character) throws CompileException, LuaError, UnwindThrowable {
		if (current == character) {
			next();
			return true;
		}

		return false;
	}

	private boolean checkNext(char c1, char c2) throws CompileException, LuaError, UnwindThrowable {
		if (current == c1 || current == c2) {
			saveAndNext();
			return true;
		}

		return false;
	}

	private LuaNumber readNumeral() throws CompileException, LuaError, UnwindThrowable {
		assert CharProperties.isDigit(current);

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

			if (CharProperties.isHex(current) || current == '.') {
				saveAndNext();
			} else {
				break;
			}
		}

		var value = NumberParser.parse(buff, 0, bufferSize, 10);
		if (Double.isNaN(value)) throw lexError("malformed number", TK_NUMBER);
		return ValueFactory.valueOf(value);
	}

	/**
	 * Read a sequence '[=*[' or ']=*]', leaving the last bracket.
	 *
	 * @return If the sequence is well formed, the number of '='s + 2. If not, then 1 if it is a single bracket (no '='s
	 * and no 2nd bracket); otherwise (an unfinished '[==...') 0.
	 */
	private int skipSep() throws CompileException, LuaError, UnwindThrowable {
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

	private void readLongString(Token token, int sep) throws CompileException, LuaError, UnwindThrowable {
		int line = lineNumber;
		saveAndNext(); // skip 2nd `['
		if (currIsNewline()) inclineNumber(); // Skip leading string if needed

		while (true) {
			switch (current) {
				case EOZ ->
					throw lexError("unfinished long " + (token != null ? "string" : "comment") + " (started at line " + line + ")", TK_EOS);
				case '[' -> {
					if (skipSep() == sep) {
						saveAndNext(); // skip 2nd `['
						if (sep == 2) throw lexError("nesting of [[...]] is deprecated", '[');
					}
				}
				case ']' -> {
					if (skipSep() == sep) {
						saveAndNext(); // skip 2nd `]'
						if (token != null) token.value = newString(buff, sep, bufferSize - 2 * sep);
						return;
					}
				}
				case '\n', '\r' -> {
					save('\n');
					inclineNumber();
					if (token == null) bufferSize = 0; // avoid wasting space
				}
				default -> {
					if (token != null) {
						saveAndNext();
					} else {
						next();
					}
				}
			}
		}
	}

	private CompileException escapeError(String message) throws CompileException, LuaError, UnwindThrowable {
		if (current != EOZ) saveAndNext();
		return lexError(message, TK_STRING);
	}

	private int readHex() throws CompileException, LuaError, UnwindThrowable {
		saveAndNext();
		if (!CharProperties.isHex(current)) throw escapeError("hexadecimal digit expected");
		return CharProperties.hexValue(current);
	}

	private int readHexEsc() throws CompileException, LuaError, UnwindThrowable {
		int left = readHex();
		int right = readHex();
		bufferSize -= 2;
		return (left << 4) | right;
	}

	private void readUtf8Esc() throws CompileException, LuaError, UnwindThrowable {
		saveAndNext();
		if (current != '{') throw escapeError("missing '{'");

		int i = 4;
		long codepoint = readHex();
		while (true) {
			saveAndNext();
			if (!CharProperties.isHex(current)) break;

			i++;
			codepoint = (codepoint << 4) | CharProperties.hexValue(current);
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

	private int readDecEsc() throws CompileException, LuaError, UnwindThrowable {
		int i = 0;
		int result = 0;
		for (; i < 3 && CharProperties.isDigit(current); i++) {
			result = 10 * result + current - '0';
			saveAndNext();
		}

		if (result > 255) throw escapeError("escape sequence too large");
		bufferSize -= i;

		return result;
	}

	private LuaString readString(int del) throws CompileException, LuaError, UnwindThrowable {
		saveAndNext();
		while (current != del) {
			switch (current) {
				case EOZ -> throw lexError("unfinished string", TK_EOS);
				case '\n', '\r' -> throw lexError("unfinished string", TK_STRING);
				case '\\' -> {
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
							while (current != EOZ && CharProperties.isSpace(current)) {
								if (currIsNewline()) inclineNumber();
								next();
							}
							break;
						}
						default: {
							if (!CharProperties.isDigit(current)) {
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
				}
				default -> saveAndNext();
			}
		}

		saveAndNext(); /* skip delimiter */
		return newString(buff, 1, bufferSize - 2);
	}

	private void saveEscape(int character) throws CompileException, LuaError, UnwindThrowable {
		next();
		bufferSize--;
		save(character);
	}

	private int lexToken(Token token) throws CompileException, LuaError, UnwindThrowable {
		bufferSize = 0;
		while (true) {
			token.position = packPosition(lineNumber, columnNumber);

			switch (current) {
				case '\n', '\r' -> {
					inclineNumber();
				}
				case '-' -> {
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
				case '[' -> {
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
				case '=' -> {
					next();
					return checkNext('=') ? TK_EQ : '=';
				}
				case '<' -> {
					next();
					return checkNext('=') ? TK_LE : '<';
				}
				case '>' -> {
					next();
					return checkNext('=') ? TK_GE : '>';
				}
				case '~' -> {
					next();
					return checkNext('=') ? TK_NE : '~';
				}
				case ':' -> {
					next();
					return checkNext(':') ? TK_DBCOLON : ':';
				}
				case '"', '\'' -> {
					token.value = readString(current);
					return TK_STRING;
				}
				case '.' -> {
					saveAndNext();
					if (checkNext('.')) {
						if (checkNext('.')) {
							return TK_DOTS; /* ... */
						} else {
							return TK_CONCAT; /* .. */
						}
					} else if (!CharProperties.isDigit(current)) {
						return '.';
					} else {
						token.value = readNumeral();
						return TK_NUMBER;
					}
				}
				case EOZ -> {
					return TK_EOS;
				}
				default -> {
					if (CharProperties.isSpace(current)) {
						next();
						continue;
					}

					if (CharProperties.isDigit(current)) {
						token.value = readNumeral();
						return TK_NUMBER;
					}

					if (CharProperties.isAlpha(current) || current == '_') {
						/* identifier or reserved word */
						do {
							saveAndNext();
						} while (CharProperties.isAlphaNum(current) || current == '_');
						Integer reservedIdx = RESERVED.get(ByteBuffer.wrap(buff, 0, bufferSize));
						if (reservedIdx != null) {
							return reservedIdx;
						} else {
							token.value = newString(buff, 0, bufferSize);
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

	long lastPosition() {
		return lastPosition;
	}

	void skipShebang() throws CompileException, LuaError, UnwindThrowable {
		if (current == '#') {
			while (!currIsNewline() && current != EOZ) next();
		}
	}

	void nextToken() throws CompileException, LuaError, UnwindThrowable {
		lastPosition = packPosition(lineNumber, columnNumber);
		if (lookahead.token != TK_EOS) { // is there a look-ahead token?
			token.set(lookahead);
			lookahead.token = TK_EOS; // and discharge it
		} else {
			token.token = lexToken(token); // read next token
		}
	}

	Token lookahead() throws CompileException, LuaError, UnwindThrowable {
		if (lookahead.token == TK_EOS) lookahead.token = lexToken(lookahead);
		return lookahead;
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
