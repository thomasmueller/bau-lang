package org.bau.ena;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Lexer {
    private final String src;
    private int index = 0;
    private int line = 1;
    private int column = 1;

    private final Deque<Integer> indentStack = new ArrayDeque<>();
    private boolean atLineStart = true;
    private final Deque<Token> pending = new ArrayDeque<>();

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("elif", TokenType.ELIF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("loop", TokenType.LOOP);
        KEYWORDS.put("exit", TokenType.EXIT);
        KEYWORDS.put("ret", TokenType.RET);
        KEYWORDS.put("fun", TokenType.FUN);
        KEYWORDS.put("type", TokenType.TYPE);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
    }

    public Lexer(String src) {
        this.src = src.replace("\r\n", "\n").replace('\r', '\n');
        indentStack.push(0);
    }

    public List<Token> tokenize() {
        List<Token> out = new ArrayList<>();
        Token t;
        do {
            t = next();
            out.add(t);
        } while (t.type != TokenType.EOF);
        return out;
    }

    Token next() {
        if (!pending.isEmpty())
            return pending.removeFirst();

        if (atLineStart) {
            atLineStart = false;
            int startCol = column;
            int spaces = 0;
            while (!eof() && peek() == ' ') {
                advance();
                spaces++;
            }
            if (!eof() && peek() == '#') {
                // full-line comment → treat as empty line
                while (!eof() && peek() != '\n')
                    advance();
            }
            if (eof()) {
                if (indentStack.peek() > 0) {
                    indentStack.pop();
                    return new Token(TokenType.DEDENT, "", line, startCol);
                }
                return emit(TokenType.EOF, "");
            }
            if (peek() == '\n') {
                advance();
                atLineStart = true;
                return new Token(TokenType.NEWLINE, "\n", line - 1, 1);
            }
            int currentIndent = indentStack.peek();
            if (spaces > currentIndent) {
                indentStack.push(spaces);
                return new Token(TokenType.INDENT, "", line, startCol);
            } else if (spaces < currentIndent) {
                // may need multiple DEDENTs; queue them
                while (spaces < indentStack.peek()) {
                    indentStack.pop();
                    pending.addLast(new Token(TokenType.DEDENT, "", line, startCol));
                }
                return pending.removeFirst();
            }
        }

        skipWhitespace();
        if (eof()) {
            if (indentStack.peek() > 0) {
                indentStack.pop();
                return new Token(TokenType.DEDENT, "", line, column);
            }
            return emit(TokenType.EOF, "");
        }

        char c = peek();
        if (c == '\n') {
            advance();
            atLineStart = true;
            return new Token(TokenType.NEWLINE, "\n", line - 1, 1);
        }
        if (c == '#') {
            // comment must be full line per spec; if encountered mid-line, skip to end
            while (!eof() && peek() != '\n')
                advance();
            return next();
        }

        if (isLetter(c) || c == '_')
            return ident();
        if (isDigit(c))
            return number();

        switch (c) {
        case '(':
            advance();
            return emit(TokenType.LPAREN, "(");
        case ')':
            advance();
            return emit(TokenType.RPAREN, ")");
        case '[':
            advance();
            return emit(TokenType.LBRACK, "[");
        case ']':
            advance();
            return emit(TokenType.RBRACK, "]");
        case ',':
            advance();
            return emit(TokenType.COMMA, ",");
        case '.':
            advance();
            return emit(TokenType.DOT, ".");
        case '+':
            advance();
            return emit(TokenType.PLUS, "+");
        case '-':
            advance();
            return emit(TokenType.MINUS, "-");
        case '*':
            advance();
            return emit(TokenType.STAR, "*");
        case '/':
            advance();
            return emit(TokenType.SLASH, "/");
        case '%':
            advance();
            return emit(TokenType.PERCENT, "%");
        case '<':
            advance();
            if (!eof() && peek() == '=') {
                advance();
                return emit(TokenType.LE, "<=");
            }
            if (!eof() && peek() == '<') {
                advance();
                return emit(TokenType.SHL, "<<");
            }
            if (!eof() && peek() == '>') {
                advance();
                return emit(TokenType.NE, "<>");
            }
            return emit(TokenType.LT, "<");
        case '>':
            advance();
            if (!eof() && peek() == '=') {
                advance();
                return emit(TokenType.GE, ">=");
            }
            if (!eof() && peek() == '>') {
                advance();
                return emit(TokenType.SHR, ">>");
            }
            return emit(TokenType.GT, ">");
        case '&':
            advance();
            return emit(TokenType.AMP, "&");
        case '|':
            advance();
            return emit(TokenType.BAR, "|");
        case '^':
            advance();
            return emit(TokenType.CARET, "^");
        case ':':
            advance();
            return emit(TokenType.ASSIGN, ":");
        case '=':
            advance();
            return emit(TokenType.EQ, "=");
        case '\'':
            return text();
        }

        throw error("Unexpected character '" + c + "'");
    }

    private Token ident() {
        int start = index;
        int startCol = column;
        advance();
        while (!eof() && (isLetterOrDigit(peek()) || peek() == '_'))
            advance();
        String lex = src.substring(start, index);
        TokenType kw = KEYWORDS.get(lex);
        if (kw != null)
            return new Token(kw, lex, line, startCol);
        return new Token(TokenType.IDENT, lex, line, startCol);
    }

    private Token number() {
        int start = index;
        int startCol = column;
        while (!eof() && isDigit(peek()))
            advance();
        boolean isReal = false;
        if (!eof() && peek() == '.') {
            isReal = true;
            advance();
            if (!eof() && isDigit(peek())) {
                while (!eof() && isDigit(peek()))
                    advance();
            }
            if (!eof() && (peek() == 'e')) {
                advance();
                if (!eof() && (peek() == '-'))
                    advance();
                while (!eof() && isDigit(peek()))
                    advance();
            }
        }
        String lex = src.substring(start, index);
        return new Token(isReal ? TokenType.REAL : TokenType.INT, lex, line, startCol);
    }

    private Token text() {
        int startCol = column;
        advance(); // opening '
        StringBuilder sb = new StringBuilder();
        while (!eof()) {
            char c = peek();
            if (c == '\'') {
                advance();
                if (!eof() && peek() == '\'') {
                    sb.append('\'');
                    advance();
                    continue;
                }
                return new Token(TokenType.TEXT, sb.toString(), line, startCol);
            }
            if (c == '\n')
                throw error("Unterminated string literal");
            sb.append(c);
            advance();
        }
        throw error("Unterminated string literal");
    }

    private void skipWhitespace() {
        while (!eof()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\f') {
                advance();
                continue;
            }
            break;
        }
    }

    private boolean eof() {
        return index >= src.length();
    }

    private char peek() {
        return src.charAt(index);
    }

    private void advance() {
        if (peek() == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        index++;
    }

    private RuntimeException error(String msg) {
        return new RuntimeException("Lex error @" + line + ":" + column + ": " + msg);
    }

    private Token emit(TokenType type, String lexeme) {
        return new Token(type, lexeme, line, column);
    }

    private boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    private boolean isLetterOrDigit(char c) {
        return isLetter(c) || isDigit(c);
    }
}
