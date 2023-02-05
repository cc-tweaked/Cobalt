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
        switch (op) {
            case TK_NOT:
                return NOT;
            case '-':
                return MINUS;
            case '#':
                return LEN;
            default:
                return null;
        }
    }
}
