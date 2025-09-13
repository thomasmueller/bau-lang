package org.bau.ena;

enum TokenType {
    // Special
    EOF, NEWLINE, INDENT, DEDENT,

    // Identifiers and literals
    IDENT, INT, REAL, TEXT,

    // Keywords
    IF, ELIF, ELSE, LOOP, EXIT, RET, FUN, TYPE, AND, OR,

    // Punctuation
    LPAREN, RPAREN, LBRACK, RBRACK, COMMA, DOT,

    // Operators
    ASSIGN, // : (assignment)
    EQ, // = (equality)
    PLUS, MINUS, STAR, SLASH, PERCENT, LT, GT, LE, GE, NE, // <> is NE
    SHL, SHR, AMP, BAR, CARET,
}
