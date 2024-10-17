package org.bau.string.regex;

import java.util.ArrayList;

/**
 * A simple regex implementation that doesn't support "(..)" and "or".
 */
public class RegexSimple {

    static enum TokenType {
        START,
        END,
        MATCH,
        LIST,
        NOT_LIST,
        ANY,
    }

    static class Token {
        TokenType type;
        boolean escaped;
        String data;
        int min;
        int max;

        boolean matchChar(String text, int pos) {
            if (pos >= text.length()) {
                return false;
            }
            char c = text.charAt(pos);
            switch (type) {
            case START:
            case END:
                throw new IllegalStateException();
            case MATCH:
                return c == data.charAt(0);
            case LIST:
                for (int i = 0; i < data.length(); i += 3) {
                    char min = data.charAt(i);
                    char max = data.charAt(i + 2);
                    if (c >= min && c <= max) {
                        return true;
                    }
                }
                return false;
            case NOT_LIST:
                for (int i = 0; i < data.length(); i += 3) {
                    char min = data.charAt(i);
                    char max = data.charAt(i + 2);
                    if (c >= min && c <= max) {
                        return false;
                    }
                }
                return true;
            case ANY:
                return true;
            default:
                throw new IllegalStateException();
            }
        }

        public String toString() {
            String result;
            switch (type) {
            case START:
                result = "^";
                break;
            case END:
                result = "$";
                break;
            case MATCH:
                if (escaped) {
                    result = "\\" + data;
                } else {
                    result = data;
                }
                break;
            case LIST:
                result = "[" + data + "]";
                break;
            case NOT_LIST:
                result = "[^" + data + "]";
                break;
            case ANY:
                result = ".";
                break;
            default:
                throw new IllegalStateException();
            }
            if (min == 1 && max == 1) {
                // default
            } else if (min == 0 && max == Integer.MAX_VALUE) {
                result += "*";
            } else if (min == 1 && max == Integer.MAX_VALUE) {
                result += "+";
            } else if (min == 0 && max == 1) {
                result += "?";
            } else {
                result += "{" + min + "," + max + "}";
            }
            return result;
        }
    }

    static char at(String s, int pos) {
        if (pos >= s.length()) {
            throw new IllegalArgumentException();
        }
        return s.charAt(pos);
    }

    static ArrayList<Token> parse(String regex) {
        ArrayList<Token> result = new ArrayList<>();
        for (int i = 0; i < regex.length(); i++) {
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
            } else {
                t = new Token();
                t.type = TokenType.MATCH;
                t.data = "" + c;
            }
            t.min = 1;
            t.max = 1;
            result.add(t);
            if (i + 1 >= regex.length()) {
                break;
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
        return result;
    }

    public static String replaceFirst(String text, String regex, String replacement) {
        int[] pair = find(text, regex);
        if (pair == null) {
            return text;
        }
        return text.substring(0, pair[0]) + replacement + text.substring(pair[1]);
    }

    public static String replaceAll(String text, String regex, String replacement) {
        StringBuilder buff = new StringBuilder();
        while (true) {
            int[] pair = find(text, regex);
            if (pair == null) {
                buff.append(text);
                return buff.toString();
            }
            buff.append(text.substring(0, pair[0]) + replacement);
            text = text.substring(pair[1]);
        }
    }

    public static String[] split(String text, String regex) {
        ArrayList<String> list = new ArrayList<>();
        while (true) {
            int[] pair = find(text, regex);
            if (pair == null) {
                list.add(text);
                return list.toArray(new String[list.size()]);
            }
            list.add(text.substring(0, pair[0]));
            text = text.substring(pair[1]);
        }
    }

    public static boolean matches(String text, String regex) {
        int[] result = find(text, regex);
        return result != null && result[0] == 0 && result[1] == text.length();
    }

    public static int[] find(String text, String regex) {
        ArrayList<Token> list = parse(regex);
        if (list.size() > 0 && list.get(0).type == TokenType.START) {
            int end = matchHere(list, 1, text, 0);
            if (end >= 0) {
                return new int[] {0, end};
            }
            return null;
        }
        int tp = 0;
        do {
            int end = matchHere(list, 0, text, tp);
            if (end >= 0) {
                return new int[] {tp, end};
            }
        } while (tp++ < text.length());
        return null;
    }

    private static int matchHere(ArrayList<Token> list, int rp, String text, int tp) {
        if (rp >= list.size()) {
            return tp;
        }
        Token t = list.get(rp);
        if (t.type == TokenType.END) {
            return tp == text.length() ? tp : -1;
        }
        for (int i = 0; i < t.min; i++) {
            if (!t.matchChar(text, tp)) {
                return -1;
            }
            tp++;
        }
        return matchStar(t, list, rp + 1, text, tp, t.max - t.min);
    }

    private static int matchStar(Token t, ArrayList<Token> list, int rp, String text, int tp, int max) {
        int result = -1;
        do {
            int end = matchHere(list, rp, text, tp);
            if (end >= 0) {
                result = end;
            }
        } while (t.matchChar(text, tp++) && max-- > 0);
        return result;
    }

}

