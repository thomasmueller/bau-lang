package org.bau.stdlib.string;

public class StringFormat {

    public static String sql(String... args) {
        int len = 0;
        for (String a : args) {
            len += a.length();
        }
        StringBuilder buff = new StringBuilder(len);
        boolean escape = false;
        for (String a : args) {
            if (escape) {
                a = a.replaceAll("\'", "\'\'");
                escape = false;
            } else {
                for (char c : a.toCharArray()) {
                    if (c == '\'') {
                        escape = !escape;
                    }
                }
            }
            buff.append(a);
        }
        return buff.toString();
    }

    public static String format(String... args) {
        int len = 0;
        for (String a : args) {
            len += a.length();
        }
        StringBuilder buff = new StringBuilder(len);
        for (String a : args) {
            buff.append(a);
        }
        return buff.toString();
    }

    public static String pad(String s, int len) {
        if (s.length() >= len) {
            return s;
        }
        return s + new StringBuilder().repeat(' ', len - s.length());
    }

    public static String hex(long x) {
        return Long.toHexString(x);
    }

    public static String hex(long x, int len) {
        String s = Long.toHexString(x);
        if (s.length() >= len) {
            return s;
        }
        return new StringBuilder().repeat('0', len - s.length()) + s;
    }

    public static String pad(long x, int len) {
        String s = Long.toString(x);
        if (s.length() >= len) {
            return s;
        }
        return new StringBuilder().repeat(' ', len - s.length()) + s;
    }

    public static String pad(double x, int len, int precision) {
        StringBuilder buff = new StringBuilder();
        if (x < 0) {
            x = -x;
            buff.append('-');
        }
        long y = (long) x;
        buff.append(y);
        buff.append('.');
        x -= y;
        while (precision > 0) {
            x *= 10;
            precision--;
        }
        y = (long) x;
        x -= y;
        if (x >= 0.5) {
            // rounding
            y++;
        }
        buff.append(y);
        String s = buff.toString();
        if (s.length() >= len) {
            return s;
        }
        return new StringBuilder().repeat(' ', len - s.length()) + s;
    }

}
