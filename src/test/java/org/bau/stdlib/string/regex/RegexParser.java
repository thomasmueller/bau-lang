package org.bau.stdlib.string.regex;

import java.util.ArrayList;

/**
 * A regex parser. "(..)" and "or" are supported.
 */
public class RegexParser {

    private final String regex;
    int pos;

    RegexParser(String regex) {
        this.regex = regex;
    }

    static ArrayList<Token> parse(String regex) {
        RegexParser p = new RegexParser(regex);
        return p.parseRegex();
    }

    private ArrayList<Token> parseRegex() {
        ArrayList<Token> list = parseOr();
        if (pos < regex.length()) {
            throw new IllegalStateException("Unexpected at " + regex.substring(pos));
        }
        Token eof = new Token();
        eof.type = TokenType.EOF;
        setNext(list, eof);
        return list;
    }

    private static void setNext(ArrayList<Token> list, Token next) {
        for (int i = 0; i < list.size(); i++) {
            Token t = list.get(i);
            Token n = i == list.size() - 1 ? next : list.get(i + 1);
            t.next = n;
            if (t.list != null) {
                setNext(t.list, next);
            }
            if (t.list2 != null) {
                setNext(t.list2, next);
            }
        }
    }

    private ArrayList<Token> parseOr() {
        ArrayList<Token> list = parseList();
        while (pos < regex.length()) {
            char c = regex.charAt(pos);
            if (c != '|') {
                break;
            }
            pos++;
            Token t = new Token();
            t.type = TokenType.OR;
            t.min = 1;
            t.max = 1;
            t.list = list;
            t.list2 = parseList();
            list = new ArrayList<>();
            list.add(t);
        }
        return list;
    }

    static char at(String s, int pos) {
        if (pos >= s.length()) {
            throw new IllegalArgumentException();
        }
        return s.charAt(pos);
    }

    ArrayList<Token> parseList() {
        ArrayList<Token> result = new ArrayList<>();
        int i = pos;
        for (; i < regex.length(); i++) {
            char c = regex.charAt(i);
            Token t;
            if (c == '^') {
                t = new Token();
                t.type = TokenType.START;
                t.min = 1;
                t.max = 1;
                result.add(t);
                continue;
            }
            if (c == '$') {
                t = new Token();
                t.type = TokenType.END;
                t.min = 1;
                t.max = 1;
                result.add(t);
                continue;
            }
            if (c == '.') {
                t = new Token();
                t.type = TokenType.ANY;
            } else if (c == '\\') {
                t = new Token();
                t.escaped = true;
                t.type = TokenType.MATCH;
                t.data = "" + at(regex, ++i);
            } else if (c == '(') {
                t = new Token();
                t.type = TokenType.GROUP;
                pos = i + 1;
                t.list = parseOr();
                i = pos;
                c = at(regex, i);
                if (c != ')') {
                    throw new IllegalStateException("Expected ) at " + regex.substring(i));
                }
            } else if (c == '[') {
                t = new Token();
                c = at(regex, ++i);
                if (c == '^') {
                    t.type = TokenType.NOT_LIST;
                    c = at(regex, ++i);
                } else {
                    t.type = TokenType.LIST;
                }
                t.data = "" + c + "-" + c;
                while (true) {
                    c = at(regex, ++i);
                    if (c == ']') {
                        break;
                    } else if (c == '-') {
                        c = at(regex, ++i);
                        t.data = t.data.substring(0, t.data.length() - 1) + c;
                    } else {
                        t.data += c + "-" + c;
                    }
                }
            } else if (c == ')' || c == '|') {
                break;
            } else {
                t = new Token();
                t.type = TokenType.MATCH;
                t.data = "" + c;
            }
            t.min = 1;
            t.max = 1;
            result.add(t);
            if (i + 1 >= regex.length()) {
                continue;
            }
            c = regex.charAt(++i);
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
                while (i < regex.length()) {
                    c = regex.charAt(i);
                    if (c < '0' || c > '9') {
                        break;
                    }
                    i++;
                }
                t.min = Integer.parseInt(regex.substring(start, i));
                t.max = t.min;
                if (c == ',') {
                    start = ++i;
                    while (i < regex.length()) {
                        c = regex.charAt(i);
                        if (c < '0' || c > '9') {
                            break;
                        }
                        i++;
                    }
                    t.max = Integer.parseInt(regex.substring(start, i));
                }
                if (c != '}') {
                    throw new IllegalStateException("Expected } at " + regex.substring(start));
                }
            } else {
                i--;
            }
        }
        pos = i;
        return result;
    }

}
