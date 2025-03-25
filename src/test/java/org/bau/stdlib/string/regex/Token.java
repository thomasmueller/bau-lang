package org.bau.stdlib.string.regex;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

class Token {
    TokenType type;
    boolean escaped;
    String data;
    Token next;
    ArrayList<Token> list;
    ArrayList<Token> list2;
    int min;
    int max;

    int matchOne(AtomicLong counter, String text, int pos) {
        if (counter.decrementAndGet() <= 0) {
            throw new IllegalStateException("Timeout");
        }
        if (pos >= text.length()) {
            return -1;
        }
        char c = text.charAt(pos);
        switch (type) {
        case MATCH:
            return c == data.charAt(0) ? pos + 1 : -1;
        case LIST:
            for (int i = 0; i < data.length(); i += 3) {
                char min = data.charAt(i);
                char max = data.charAt(i + 2);
                if (c >= min && c <= max) {
                    return pos + 1;
                }
            }
            return -1;
        case NOT_LIST:
            for (int i = 0; i < data.length(); i += 3) {
                char min = data.charAt(i);
                char max = data.charAt(i + 2);
                if (c >= min && c <= max) {
                    return -1;
                }
            }
            return pos + 1;
        case ANY:
            return pos + 1;
        case OR:
            int a = list.get(0).matchHere(counter, text, pos, null);
            if (a >= 0) {
                return a;
            }
            return list2.get(0).matchHere(counter, text, pos, null);
        default:
            throw new IllegalStateException();
        }
    }

    int matchHere(AtomicLong counter, String text, int tp, Token condition) {
        switch (type) {
        case END:
            return tp == text.length() ? tp : -1;
        case EOF:
            if (condition != null) {
                if (condition.matchHere(counter, text, tp, null) < 0) {
                    return -1;
                }
            }
            return tp;
        case GROUP:
            return matchGroup(counter, text, tp);
        default:
        }
        for (int i = 0; i < min; i++) {
            tp = matchOne(counter, text, tp);
            if (tp < 0) {
                return -1;
            }
        }
        int remain = max - min;
        int result = -1;
        while(true) {
            int end = next.matchHere(counter, text, tp, condition);
            if (end >= 0) {
                result = end;
            }
            tp = matchOne(counter, text, tp);
            if (tp < 0) {
                break;
            }
            if (remain-- <= 0) {
                break;
            }
        }
        return result;
    }

    int matchGroup(AtomicLong counter, String text, int pos) {
        Token first = list.get(0);
        for (int i = 0; i < min; i++) {
            int p = first.matchHere(counter, text, pos, next);
            if (p < 0) {
                p = first.matchHere(counter, text, pos, null);
            }
            if (p < 0) {
                return -1;
            }
            pos = p;
        }
        int remain = max - min;
        int result = -1;
        while(true) {
            int end = next.matchHere(counter, text, pos, null);
            if (end > result) {
                result = end;
            }
            int p = first.matchHere(counter, text, pos, next);
            if (p < 0) {
                p = first.matchHere(counter, text, pos, null);
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
        case GROUP: {
            StringBuilder buff = new StringBuilder();
            buff.append('(');
            for (Token t : list) {
                buff.append(t);
            }
            buff.append(')');
            result = buff.toString();
            break;
        }
        case EOF:
            return "";
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