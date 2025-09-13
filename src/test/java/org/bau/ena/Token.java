package org.bau.ena;

public final class Token {
    final TokenType type;
    final String lexeme;
    final int line;
    final int column;

    Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return type + "(" + lexeme + ")@" + line + ":" + column;
    }
}
