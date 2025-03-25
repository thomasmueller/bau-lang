package org.bau.stdlib.string.regex.v2;

public class Regex2 {

    static String REGEX2_PREFIX = "(?2)";

    static String toRegex(String regex2) {
        StringBuilder buff = new StringBuilder();
        for(int i=0; i<regex2.length(); i++) {
            char c = regex2.charAt(i);
            if (c == '\'') {
                while (true) {
                    c = regex2.charAt(++i);
                    if (c == '\'') {
                        if (i >= regex2.length() - 1) {
                            break;
                        }
                        c = regex2.charAt(++i);
                        if (c == '\'') {
                            buff.append(c);
                        } else {
                            i--;
                            break;
                        }
                    } else {
                        if ("^$.()+|*?/\\".indexOf(c) >= 0) {
                            buff.append('\\');
                        }
                        buff.append(c);
                    }
                }
            } else if (c == 'x') {
                buff.append("\\");
                buff.append(regex2.substring(i, i + 3));
                i += 2;
            } else if (c == 'u') {
                buff.append("\\");
                buff.append(regex2.substring(i, i + 5));
                i += 4;
            } else if (c >= 'a' && c <= 'z') {
                buff.append('\\');
                buff.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                buff.append('\\');
                buff.append(c);
            } else if (c == '{') {
                while (true) {
                    buff.append(c);
                    c = regex2.charAt(++i);
                    if (c == '}') {
                        buff.append(c);
                        break;
                    }
                }
            } else {
                buff.append(c);
            }
        }
        return buff.toString();
    }

    static String toRegex2(String regex) {
        StringBuilder buff = new StringBuilder();
        for(int i=0; i<regex.length(); i++) {
            char c = regex.charAt(i);
            if ("^$.()+|*?".indexOf(c) >= 0) {
                buff.append(c);
            } else if (c == '{') {
                buff.append(c);
                while (true) {
                    c = regex.charAt(++i);
                    buff.append(c);
                    if (c == '}') {
                        break;
                    }
                }
            } else if (c == '[') {
                buff.append(c);
                c = regex.charAt(++i);
                if (c == '^') {
                    buff.append(c);
                    c = regex.charAt(++i);
                }
                while (true) {
                    if (c == ']') {
                        break;
                    } else if (c == '\\') {
                        i = appendEscaped(regex, i, buff);
                    } else if (c == '-') {
                        char last = buff.charAt(buff.length() - 1);
                        if (last == '\'' || last == '[' || last == '^') {
                            appendLiteral(buff, c);
                        } else {
                            buff.append(c);
                        }
                    } else {
                        appendLiteral(buff, c);
                    }
                    c = regex.charAt(++i);
                }
                buff.append("]");
            } else if (c == '\\') {
                i = appendEscaped(regex, i, buff);
            } else {
                appendLiteral(buff, c);
            }
        }
        return buff.toString();
    }

    private static void appendLiteral(StringBuilder buff, char c) {
        if (buff.length() > 0 && buff.charAt(buff.length() - 1) == '\'') {
            buff.setCharAt(buff.length() - 1, c);
            if (c == '\'') {
                buff.append('\'');
            }
            buff.append('\'');
        } else {
            buff.append('\'');
            buff.append(c);
            if (c == '\'') {
                buff.append('\'');
            }
            buff.append('\'');
        }
    }

    private static int appendEscaped(String regex, int i, StringBuilder buff) {
        char c = regex.charAt(++i);
        if ('x' == c) {
            buff.append(regex.substring(i, i + 3));
            i += 2;
        } else if ('u' == c) {
            buff.append(regex.substring(i, i + 5));
            i += 4;
        } else if (c >= 'a' && c <= 'z') {
            buff.append(c);
        } else if (c >= 'A' && c <= 'Z') {
            buff.append(c);
        } else {
            appendLiteral(buff, c);
        }
        return i;
    }

}
