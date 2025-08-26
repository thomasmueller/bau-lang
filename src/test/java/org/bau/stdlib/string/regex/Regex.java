package org.bau.stdlib.string.regex;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A small regular expression engine.
 * <p>
 * Supported features:
 * <ul>
 *   <li>Literals and the dot wildcard '.' (any character, including newline)</li>
 *   <li>Grouping with parentheses '(...)' (no capture results are exposed)</li>
 *   <li>Alternation with '|'</li>
 *   <li>Anchors '^' (start of input) and '$' (end of input)</li>
 *   <li>Greedy quantifiers: '*', '+', '?', and '{m,n}' (bounded)</li>
 *   <li>Bracket expressions: '[...]' and '[^...]' with ranges like 'a-z'.
 *       The first ']' is treated as a literal when it appears first, and a trailing
 *       '-' is treated as a literal '-'.</li>
 *   <li>Backslash escape for a literal next character outside brackets (e.g., '\.', '\[')</li>
 * </ul>
 * Limitations:
 * <ul>
 *   <li>No character classes like '\d', '\w', '\s' (see alternatives below)</li>
 *   <li>No flags (e.g., case-insensitive, dot-all, multiline)</li>
 *   <li>No non-greedy quantifiers ('*?', '+?', '??')</li>
 *   <li>No backreferences or exposed captures</li>
 *   <li>No lookarounds: '(?=...)', '(?!...)', '(?<=...)', '(?<!...)'</li>
 *   <li>No Unicode or POSIX classes</li>
 * </ul>
 * Alternatives for common shorthand classes:
 * <ul>
 *   <li>'\d': use '[0-9]'</li>
 *   <li>'\w': use '[A-Za-z0-9_]'</li>
 *   <li>'\s': use '[ \t\r\n\f]'</li>
 * </ul>
 * If trying to find a match takes more than 100 times the input length,
 * an IllegalStateException is thrown.
 */
public class Regex {

    private final String pattern;
    private Token[] tokens;
    private int headIndex;
    private int inPos;
    private int outPos;

    private Regex(String pattern) {
        this.pattern = pattern;
        this.tokens = new Token[pattern.length() + 1];
    }

    /**
     * Replace the first match of the given regular expression.
     *
     * @param text the input
     * @param regex the search pattern
     * @param replacement the replacement
     * @return the input with the first match replaced
     */
    public static String replaceFirst(String text, String regex, String replacement) {
        int[] pair = find(text, regex);
        if (pair == null) {
            return text;
        }
        return text.substring(0, pair[0]) + replacement + text.substring(pair[1]);
    }

    /**
     * Replace all matches of the regular expression.
     * Zero-length matches are ignored.
     *
     * @param text the input
     * @param regex the search pattern
     * @param replacement the replacement
     * @return the input with all matches replaced
     */
    public static String replaceAll(String text, String regex, String replacement) {
        StringBuilder buff = new StringBuilder(text.length());
        Regex pattern = new Regex(regex);
        pattern.parse();
        while (!text.isEmpty()) {
            int[] pair = pattern.find(text);
            if (pair == null) {
                buff.append(text);
                break;
            } else if (pair[0] == 0 && pair[1] == 0) {
                buff.append(text.charAt(0));
                text = text.substring(1);
            } else {
                buff.append(text.substring(0, pair[0]) + replacement);
                text = text.substring(pair[1]);
            }
        }
        return buff.toString();
    }

    /**
     * Split the input text around matches.
     *
     * @param text the input
     * @param regex the delimiter pattern
     * @return an array of substrings
     */
    public static String[] split(String text, String regex) {
        ArrayList<String> list = new ArrayList<>();
        Regex pattern = new Regex(regex);
        pattern.parse();
        while (true) {
            int[] pair = pattern.find(text);
            if (pair == null) {
                list.add(text);
                return list.toArray(new String[list.size()]);
            }
            list.add(text.substring(0, pair[0]));
            text = text.substring(pair[1]);
        }
    }

    /**
     * Test whether the entire input matches.
     *
     * @param text the input
     * @param regex the pattern
     * @return whether the entire input matches
     */
    public static boolean matches(String text, String regex) {
        int[] result = find(text, regex);
        return result != null && result[0] == 0 && result[1] == text.length();
    }

    /**
     * Find the first match.
     * <p>
     * Returns the start (inclusive) and end (exclusive) indices,
     * or {@code null} if no match is found.
     *
     * @param text the input
     * @param regex the search pattern
     * @return an int array {start, end} for the first match, or null if none
     */
    public static int[] find(String text, String regex) {
        Regex pattern = new Regex(regex);
        pattern.parse();
        return pattern.find(text);
    }

    private void parse() {
        int head = parseOr();
        Token eof = new Token();
        eof.type = TokenType.EOF;
        eof.min = 1;
        eof.max = 1;
        int eofIndex = append(eof);
        for (int i = 0; i < outPos; i++) {
            Token tk = tokens[i];
            if (tk.type == TokenType.GROUP) {
                setNext(tk.mainStart, eofIndex);
            }
        }
        setNext(head, eofIndex);
        Token[] out = new Token[outPos];
        System.arraycopy(tokens, 0, out, 0, outPos);
        this.headIndex = head;
    }

    private int parseOr() {
        int leftHead = parseList();
        while (inPos < pattern.length()) {
            char c = pattern.charAt(inPos);
            if (c != '|') {
                break;
            }
            inPos++;
            int rightHead = parseList();
            Token t = new Token();
            t.type = TokenType.OR;
            t.min = 1;
            t.max = 1;
            int orIndex = append(t);
            t.mainStart = leftHead;
            t.otherStart = rightHead;
            leftHead = orIndex;
        }
        return leftHead;
    }

    private int parseList() {
        int head = -1;
        int tail = -1;
        int i = inPos;
        for (; i < pattern.length(); i++) {
            char c = charAt(i);
            Token t;
            if (c == '^') {
                if (head >= 0) {
                    throw new IllegalArgumentException("^ needs to be the first entry");
                }
                t = new Token();
                t.type = TokenType.START;
                t.min = 1;
                t.max = 1;
                int ti = append(t);
                head = ti;
                tail = ti;
                continue;
            }
            if (c == '$') {
                if (tail < 0) {
                    throw new IllegalArgumentException("$ needs to be the last entry");
                }
                t = new Token();
                t.type = TokenType.END;
                t.min = 1;
                t.max = 1;
                int ti = append(t);
                tokens[tail].nextIndex = ti;
                tail = ti;
                continue;
            }
            if (c == '.') {
                t = new Token();
                t.type = TokenType.ANY;
            } else if (c == '\\') {
                t = new Token();
                t.type = TokenType.MATCH;
                t.dataIndex = ++i;
            } else if (c == '(') {
                t = new Token();
                t.type = TokenType.GROUP;
                inPos = i + 1;
                int innerHead = parseOr();
                t.mainStart = innerHead;
                i = inPos;
                c = charAt(i);
            } else if (c == '[') {
                t = new Token();
                c = charAt(++i);
                if (c == '^') {
                    t.type = TokenType.NOT_LIST;
                    c = charAt(++i);
                } else {
                    t.type = TokenType.LIST;
                }
                int start = i;
                while (true) {
                    c = charAt(++i);
                    if (c == ']') {
                        break;
                    }
                }
                t.dataIndex = start;
            } else if (c == ')' || c == '|') {
                break;
            } else {
                t = new Token();
                t.type = TokenType.MATCH;
                t.dataIndex = i;
            }
            t.min = 1;
            t.max = 1;
            int ti = append(t);
            if (head == -1) {
                head = ti;
            } else {
                tokens[tail].nextIndex = ti;
            }
            tail = ti;
            if (i + 1 >= pattern.length()) {
                continue;
            }
            c = charAt(++i);
            if (c == '+') {
                t.max = Integer.MAX_VALUE;
            } else if (c == '*') {
                t.min = 0;
                t.max = Integer.MAX_VALUE;
            } else if (c == '?') {
                t.min = 0;
                t.max = 1;
            } else if (c == '{') {
                int start = ++i;
                while (true) {
                    c = charAt(i);
                    if (c < '0' || c > '9') {
                        break;
                    }
                    i++;
                }
                t.min = 0;
                if (i > start) {
                    t.min = Integer.parseInt(pattern.substring(start, i));
                }
                t.max = t.min;
                if (c == ',') {
                    start = ++i;
                    while (true) {
                        c = charAt(i);
                        if (c < '0' || c > '9') {
                            break;
                        }
                        i++;
                    }
                    t.max = Integer.MAX_VALUE;
                    if (i > start) {
                        t.max = Integer.parseInt(pattern.substring(start, i));
                    }
                }
                if (c != '}') {
                    throw new IllegalArgumentException("Expected } at " + pattern.substring(start));
                }
            } else {
                i--;
            }
        }
        inPos = i;
        return head;
    }

    private void setNext(int headIndex, int nextIndex) {
        int index = headIndex;
        while (true) {
            Token t = tokens[index];
            if (t.type == TokenType.OR) {
                setNext(t.mainStart, nextIndex);
                setNext(t.otherStart, nextIndex);
            }
            if (t.nextIndex == 0) {
                t.nextIndex = nextIndex;
                return;
            }
            index = t.nextIndex;
        }
    }

    private char charAt(int pos) {
        if (pos >= pattern.length()) {
            throw new IllegalArgumentException();
        }
        return pattern.charAt(pos);
    }

    private int append(Token t) {
        tokens[outPos] = t;
        return outPos++;
    }

    private int[] find(String text) {
        AtomicLong counter = new AtomicLong((text.length() + 1) * 100);
        int head = headIndex;
        int tp = 0;
        if (tokens[head].type == TokenType.START) {
            int end = matchHere(counter, text, 0, -1, head);
            if (end >= 0) {
                return new int[] {0, end};
            }
            return null;
        }
        do {
            int end = matchHere(counter, text, tp, -1, head);
            if (end >= 0) {
                return new int[] {tp, end};
            }
        } while (tp++ < text.length());
        return null;
    }

    private int matchHere(AtomicLong counter, String text, int tp, int conditionIndex, int tokenIndex) {
        Token t = tokens[tokenIndex];
        switch (t.type) {
        case START: {
            if (tp != 0) {
                return -1;
            }
            int next = t.nextIndex;
            return matchHere(counter, text, tp, conditionIndex, next);
        }
        case END:
            return tp == text.length() ? tp : -1;
        case EOF:
            if (conditionIndex >= 0 && matchHere(counter, text, tp, -1, conditionIndex) < 0) {
                return -1;
            }
            return tp;
        case GROUP:
            return matchGroup(counter, text, tp, tokenIndex);
        case OR:
            int a = matchHere(counter, text, tp, -1, t.mainStart);
            if (a >= 0) {
                return a;
            }
            return matchHere(counter, text, tp, -1, t.otherStart);
        default:
        }
        for (int i = 0; i < t.min; i++) {
            tp = matchOne(counter, text, tp, t);
            if (tp < 0) {
                return -1;
            }
        }
        int remain = t.max - t.min;
        int result = -1;
        while (true) {
            int next = t.nextIndex;
            int end = matchHere(counter, text, tp, conditionIndex, next);
            if (end >= 0) {
                result = end;
            }
            tp = matchOne(counter, text, tp, t);
            if (tp < 0) {
                break;
            }
            if (remain-- <= 0) {
                break;
            }
        }
        return result;
    }

    private int matchGroup(AtomicLong counter, String text, int pos, int tokenIndex) {
        Token t = tokens[tokenIndex];
        int firstIndex = t.mainStart;
        for (int i = 0; i < t.min; i++) {
            int next = t.nextIndex;
            int p = matchHere(counter, text, pos, next, firstIndex);
            if (p < 0) {
                p = matchHere(counter, text, pos, -1, firstIndex);
            }
            if (p < 0) {
                return -1;
            }
            pos = p;
        }
        int remain = t.max - t.min;
        int result = -1;
        while (true) {
            int next = t.nextIndex;
            int end = matchHere(counter, text, pos, -1, next);
            if (end > result) {
                result = end;
            }
            int p = matchHere(counter, text, pos, next, firstIndex);
            if (p < 0) {
                p = matchHere(counter, text, pos, -1, firstIndex);
            }
            if (p < 0) {
                break;
            }
            pos = p;
            if (remain-- <= 0) {
                break;
            }
        }
        return result;
    }

    private int matchOne(AtomicLong counter, String text, int pos, Token t) {
        if (counter.decrementAndGet() <= 0) {
            throw new IllegalStateException("Timeout");
        }
        if (pos >= text.length()) {
            return -1;
        }
        char c = text.charAt(pos);
        switch (t.type) {
        case MATCH:
            return c == pattern.charAt(t.dataIndex) ? pos + 1 : -1;
        case LIST:
            return matchList(t.dataIndex, c) ? pos + 1 : -1;
        case NOT_LIST:
            return matchList(t.dataIndex, c) ? -1 : pos + 1;
        case ANY:
            return pos + 1;
        default:
            throw new IllegalStateException();
        }
    }

    private boolean matchList(int start, char ch) {
        int i = start;
        boolean first = true;
        while (true) {
            char a = pattern.charAt(i++);
            if (!first && a == ']') {
                return false;
            }
            if (i + 1 < pattern.length() && pattern.charAt(i) == '-' && pattern.charAt(i + 1) != ']') {
                char b = pattern.charAt(i + 1);
                if (ch >= a && ch <= b) {
                    return true;
                }
                i += 2;
                first = false;
                continue;
            }
            if (ch == a) {
                return true;
            }
            first = false;
        }
    }

    private static class Token {
        TokenType type;
        int dataIndex;
        int nextIndex;
        int mainStart;
        int otherStart;
        int min;
        int max;
    }

    private enum TokenType {
        START,
        END,
        MATCH,
        LIST,
        NOT_LIST,
        ANY,
        GROUP,
        OR,
        EOF,
    }

}

