package org.bau.ena;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnaLexer {
    private final String src;
    private int index = 0;
    private int line = 1;
    private int column = 1;

    private final Deque<Integer> indentStack = new ArrayDeque<>();
    private boolean atLineStart = true;
    private final Deque<EnaToken> pending = new ArrayDeque<>();

    private static final Map<String, EnaTokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("if", EnaTokenType.IF);
        KEYWORDS.put("elif", EnaTokenType.ELIF);
        KEYWORDS.put("else", EnaTokenType.ELSE);
        KEYWORDS.put("loop", EnaTokenType.LOOP);
        KEYWORDS.put("exit", EnaTokenType.EXIT);
        KEYWORDS.put("ret", EnaTokenType.RET);
        KEYWORDS.put("fun", EnaTokenType.FUN);
        KEYWORDS.put("type", EnaTokenType.TYPE);
        KEYWORDS.put("and", EnaTokenType.AND);
        KEYWORDS.put("or", EnaTokenType.OR);
    }

    public EnaLexer(String src) {
        this.src = src.replace("\r\n", "\n").replace('\r', '\n');
        indentStack.push(0);
    }

    public List<EnaToken> tokenize() {
        List<EnaToken> out = new ArrayList<>();
        EnaToken t;
        do {
            t = next();
            out.add(t);
        } while (t.type != EnaTokenType.EOF);
        return out;
    }

    EnaToken next() {
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
                    return new EnaToken(EnaTokenType.DEDENT, "", line, startCol);
                }
                return emit(EnaTokenType.EOF, "");
            }
            if (peek() == '\n') {
                advance();
                atLineStart = true;
                return new EnaToken(EnaTokenType.NEWLINE, "\n", line - 1, 1);
            }
            int currentIndent = indentStack.peek();
            if (spaces > currentIndent) {
                indentStack.push(spaces);
                return new EnaToken(EnaTokenType.INDENT, "", line, startCol);
            } else if (spaces < currentIndent) {
                // may need multiple DEDENTs; queue them
                while (spaces < indentStack.peek()) {
                    indentStack.pop();
                    pending.addLast(new EnaToken(EnaTokenType.DEDENT, "", line, startCol));
                }
                return pending.removeFirst();
            }
        }

        skipWhitespace();
        if (eof()) {
            if (indentStack.peek() > 0) {
                indentStack.pop();
                return new EnaToken(EnaTokenType.DEDENT, "", line, column);
            }
            return emit(EnaTokenType.EOF, "");
        }

        char c = peek();
        if (c == '\n') {
            advance();
            atLineStart = true;
            return new EnaToken(EnaTokenType.NEWLINE, "\n", line - 1, 1);
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
            return emit(EnaTokenType.LPAREN, "(");
        case ')':
            advance();
            return emit(EnaTokenType.RPAREN, ")");
        case '[':
            advance();
            return emit(EnaTokenType.LBRACK, "[");
        case ']':
            advance();
            return emit(EnaTokenType.RBRACK, "]");
        case ',':
            advance();
            return emit(EnaTokenType.COMMA, ",");
        case '.':
            advance();
            return emit(EnaTokenType.DOT, ".");
        case '+':
            advance();
            return emit(EnaTokenType.PLUS, "+");
        case '-':
            advance();
            return emit(EnaTokenType.MINUS, "-");
        case '*':
            advance();
            return emit(EnaTokenType.STAR, "*");
        case '/':
            advance();
            return emit(EnaTokenType.SLASH, "/");
        case '%':
            advance();
            return emit(EnaTokenType.PERCENT, "%");
        case '<':
            advance();
            if (!eof() && peek() == '=') {
                advance();
                return emit(EnaTokenType.LE, "<=");
            }
            if (!eof() && peek() == '<') {
                advance();
                return emit(EnaTokenType.SHL, "<<");
            }
            if (!eof() && peek() == '>') {
                advance();
                return emit(EnaTokenType.NE, "<>");
            }
            return emit(EnaTokenType.LT, "<");
        case '>':
            advance();
            if (!eof() && peek() == '=') {
                advance();
                return emit(EnaTokenType.GE, ">=");
            }
            if (!eof() && peek() == '>') {
                advance();
                return emit(EnaTokenType.SHR, ">>");
            }
            return emit(EnaTokenType.GT, ">");
        case '&':
            advance();
            return emit(EnaTokenType.AMP, "&");
        case '|':
            advance();
            return emit(EnaTokenType.BAR, "|");
        case '^':
            advance();
            return emit(EnaTokenType.CARET, "^");
        case ':':
            advance();
            return emit(EnaTokenType.ASSIGN, ":");
        case '=':
            advance();
            return emit(EnaTokenType.EQ, "=");
        case '\'':
            return text();
        }

        throw error("Unexpected character '" + c + "'");
    }

    private EnaToken ident() {
        int start = index;
        int startCol = column;
        advance();
        while (!eof() && (isLetterOrDigit(peek()) || peek() == '_'))
            advance();
        String lex = src.substring(start, index);
        EnaTokenType kw = KEYWORDS.get(lex);
        if (kw != null)
            return new EnaToken(kw, lex, line, startCol);
        return new EnaToken(EnaTokenType.IDENT, lex, line, startCol);
    }

    private EnaToken number() {
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
        return new EnaToken(isReal ? EnaTokenType.REAL : EnaTokenType.INT, lex, line, startCol);
    }

    private EnaToken text() {
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
                return new EnaToken(EnaTokenType.TEXT, sb.toString(), line, startCol);
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

    private EnaToken emit(EnaTokenType type, String lexeme) {
        return new EnaToken(type, lexeme, line, column);
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
