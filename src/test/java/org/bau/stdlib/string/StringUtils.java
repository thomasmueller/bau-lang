package org.bau.stdlib.string;

import java.util.ArrayList;

public class StringUtils {

    public static String trim(String s) {
        return s.trim();
    }

    public static String trimLeading(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > ' ') {
                return s.substring(i);
            }
        }
        return "";
    }

    public static String trimTrailing(String s) {
        for (int i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) > ' ') {
                return s.substring(0, i + 1);
            }
        }
        return "";
    }

    /**
     * Replace "before" with "after". If "before" is empty, then the original string
     * is returned.
     *
     * @param s
     * @param before
     * @param after
     * @return
     */
    public static String replaceAll(String s, String before, String after) {
        int next = s.indexOf(before);
        if (before.isEmpty() || next < 0) {
            return s;
        }
        StringBuilder buff = new StringBuilder(s.length());
        int index = 0;
        while (true) {
            buff.append(s, index, next).append(after);
            index = next + before.length();
            next = s.indexOf(before, index);
            if (next < 0) {
                break;
            }
        }
        buff.append(s, index, s.length());
        return buff.toString();
    }

    /**
     * Split at a delimiter. If the delimiter is not found, one entry is returned.
     *
     * @param s the original string
     * @param delimiter the delimiter
     * @return an array of strings
     */
    public static String[] split(String s, char delimiter) {
        int next = s.indexOf(delimiter);
        if (next < 0) {
            return new String[] {s};
        }
        ArrayList<String> list = new ArrayList<>();
        int index = 0;
        while (true) {
            list.add(s.substring(index, next));
            index = next + 1;
            next = s.indexOf(delimiter, index);
            if (next < 0) {
                break;
            }
        }
        list.add(s.substring(index));
        return list.toArray(new String[list.size()]);
    }

    public static boolean isSpace(char x) {
        // tab, newline, vertical tab, formfeed, return, space
        long mask = (1L << 0x9) | (1L << 0xa) | (1L << 0xb) | (1L << 0xc) | (1L << 0xd) | (1L << 0x20);
        return x <= ' ' && ((1L << x) & mask) != 0;
    }

    public static boolean isDigit(char x) {
        return x >= '0' && x <= '9';
    }

    public static char charAt(String s, int index) {
        if (index < s.length()) {
            return s.charAt(index);
        }
        return 0;
    }

    public static String toUpper(String s) {
        int i = 0;
        for (; i < s.length(); i++) {
            char x = s.charAt(i);
            char y = toUpper(x);
            if (x != y) {
                char[] chars = s.toCharArray();
                while (true) {
                    chars[i] = y;
                    i++;
                    if (i >= chars.length) {
                        break;
                    }
                    y = toUpper(chars[i]);
                }
                return new String(chars);
            }
        }
        return s;
    }

    public static String toLower(String s) {
        int i = 0;
        for (; i < s.length(); i++) {
            char x = s.charAt(i);
            char y = toLower(x);
            if (x != y) {
                char[] chars = s.toCharArray();
                while (true) {
                    chars[i] = y;
                    i++;
                    if (i >= chars.length) {
                        break;
                    }
                    y = toLower(chars[i]);
                }
                return new String(chars);
            }
        }
        return s;
    }

    public static char toUpper(char x) {
        if (x >= 'a' && x <= 'z') {
            return (char) (x - 'a' + 'A');
        }
        return x;
    }

    public static char toLower(char x) {
        if (x >= 'A' && x <= 'Z') {
            return (char) (x - 'A' + 'a');
        }
        return x;
    }

    public static int naturalCompare(String x, String y) {
        return naturalCompare(x, y, false);
    }

    public static int compareCaseInsensitive(String x, String y) {
        int xi = 0, yi = 0;
        while (xi < x.length() && yi < y.length()) {
            char xc = toUpper(x.charAt(xi));
            char yc = toUpper(y.charAt(yi));
            if (xc > yc) {
                return 1;
            } else if (xc < yc) {
                return -1;
            }
            xi++;
            yi++;
        }
        return Integer.compare(x.length(), y.length());
    }

    public static int naturalCompare(String x, String y, boolean caseInsensitive) {
        int xi = 0, yi = 0;
        while (true) {
            char xc = charAt(x, xi);
            while (isSpace(xc)) {
                xi++;
                xc = charAt(x, xi);
            }
            char yc = charAt(y, yi);
            while (isSpace(yc)) {
                yi++;
                yc = charAt(y, yi);
            }
            if (isDigit(xc) && isDigit(yc)) {
                boolean left = xc == '0' || yc == '0';
                int result = 0;
                while(true) {
                    xc = charAt(x, xi);
                    yc = charAt(y, yi);
                    if (!isDigit(xc) && !isDigit(yc)) {
                        if (result != 0) {
                            return result;
                        }
                        break;
                    } else if (!isDigit(xc)) {
                        return -1;
                    } else if (!isDigit(yc)) {
                        return 1;
                    } else if (xc < yc && result == 0) {
                        result = -1;
                    } else if (xc > yc && result == 0) {
                        result = 1;
                    }
                    if (left && result != 0) {
                        return result;
                    }
                    xi++;
                    yi++;
                }
            }
            if (xc == 0 && yc == 0) {
                return 0;
            }
            if (caseInsensitive) {
                xc = toUpper(xc);
                yc = toUpper(yc);
            }
            if (xc > yc) {
                return 1;
            } else if (xc < yc) {
                return -1;
            }
            xi++;
            yi++;
        }
    }

    public static char toUpperChar(char c) {
        if ((c >= 'a' && c <= 'z') || (c >= 224 && c <= 254 && c != 247)) {
            return (char) (c ^ 32);
        }
        return c;
    }

    public static char toLowerChar(char c) {
        if ((c >= 'A' && c <= 'Z') || (c >= 192 && c <= 222 && c != 215)) {
            return (char) (c ^ 32);
        }
        return c;
    }

}
