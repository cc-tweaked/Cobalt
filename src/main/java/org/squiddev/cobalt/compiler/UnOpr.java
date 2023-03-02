package org.squiddev.cobalt.compiler;

import static org.squiddev.cobalt.compiler.Lex.TK_NOT;

enum UnOpr {
	NOT,
	MINUS,
	LEN,
	;

	/**
	 * Priority for unary operators
	 */
	static final int PRIORITY = 8;

	static UnOpr ofToken(int op) {
		return switch (op) {
			case TK_NOT -> NOT;
			case '-' -> MINUS;
			case '#' -> LEN;
			default -> null;
		};
	}
}
