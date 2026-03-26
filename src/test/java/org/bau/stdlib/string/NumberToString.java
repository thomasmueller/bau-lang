package org.bau.stdlib.string;

import java.nio.charset.StandardCharsets;

public class NumberToString {

    public static String convertIntToI8Array(long x) {
        int len = 1;
        if (x >= 0) {
            x = -x;
            len = 0;
        }
        long n = x;
        while (true) {
            len += 1;
            n /= 10;
            if (n >= 0) {
                break;
            }
        }
        byte[] buff = new byte[len];
        buff[0] = '-';
        while (true) {
            len -= 1;
            buff[len] = (byte) ('0' - (x % 10));
            x /= 10;
            if (x >= 0) {
                break;
            }
        }
        return new String(buff, StandardCharsets.UTF_8);
    }

    public static String convertLongToString(long n) {
        int len = getLongStringLen(n);
        byte[] buff = new byte[len];
        if (n < 0) {
            buff[0] = '-';
        } else {
            n = -n;
        }
        int i = len;
        do {
            buff[--i] = (byte) ('0' - (n % 10));
            n /= 10;
        } while (n < 0);
        return new String(buff, StandardCharsets.UTF_8);
    }

    private static int getLongStringLen(long n) {
        int len = 0;
        if (n < 0) {
            len++;
        } else {
            n = -n;
        }
        do {
            len++;
            n /= 10;
        } while (n < 0);
        return len;
    }

    public static String convertDoubleToString(double n) {
        if (n < 0) {
            return "-" + convertDoubleToString(-n);
        } else if (Double.isNaN(n)) {
            return "NaN";
        } else if (n == Double.POSITIVE_INFINITY) {
            return "Infinity";
        }
        int e = 0;
        if (n >= 1e8 || n <= 1e-3) {
            while (n > 1e20) {
                n /= 1e20;
                e += 20;
            }
            while (n < 1e-20) {
                n *= 1e20;
                e -= 20;
            }
            while (n >= 10) {
                n /= 10;
                e++;
            }
            while (n < 1) {
                n *= 10;
                e--;
            }
            return convertDoubleToString(n) + "e" + e;
        }
        StringBuilder buff = new StringBuilder();
        buff.append(convertLongToString((long) n));
        buff.append('.');
        n = (n - (long) n) * 10;
        long y = (long) (n * 100_000_000_000_000_000L);
        for (int i = 0; i <= 18 && y > 0; i++) {
            long x = y / 100_000_000_000_000_000L;
            buff.append((char) ('0' + (x % 10)));
            y -= x * 100_000_000_000_000_000L;
            y *= 10;
        }
        return buff.toString();
    }

    public static String convertDoubleToString3(double n) {
        if (Double.isNaN(n)) {
            return "NaN";
        } else if (n == Double.POSITIVE_INFINITY) {
            return "Infinity";
        } else if (n == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        byte[] buff = new byte[30];
        int pos = 0;
        if (n < 0) {
            buff[0] = '-';
            pos++;
            n = -n;
        }
        int e = 0;
        if (n >= 1e8 || n <= 1e-3) {
            while (n >= 10) {
                n /= 10;
                e++;
            }
            while (n < 1) {
                n *= 10;
                e--;
            }
        }
        String s = convertLongToString((long) n);
        for (int i = 0; i < s.length(); i++) {
            buff[pos++] = (byte) s.charAt(i);
        }
        buff[pos++] = '.';
        n = (n - (long) n) * 10;
        long y = (long) (n * 100_000_000_000_000_000L);
        for (int i = 0; i <= 18 && y > 0; i++) {
            long x = y / 100_000_000_000_000_000L;
            buff[pos++] = (byte) ('0' + (x % 10));
            y -= x * 100_000_000_000_000_000L;
            y *= 10;
        }
        if (e != 0) {
            buff[pos++] = 'e';
            if (e < 0) {
                buff[pos++] = '-';
                e = -e;
            }
            buff[pos++] = (byte) ('0' + (e / 100));
            buff[pos++] = (byte) ('0' + ((e / 10) % 10));
            buff[pos++] = (byte) ('0' + (e % 10));
        }
        return new String(buff, 0, pos, StandardCharsets.UTF_8);
    }

    public static String convertDoubleToString4(double n) {
        if (Double.isNaN(n)) {
            return "NaN";
        } else if (n == Double.POSITIVE_INFINITY) {
            return "Infinity";
        } else if (n == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        byte[] buff = new byte[30];
        int pos = 0;
        if (n < 0) {
            buff[0] = '-';
            pos++;
            n = -n;
        }
        int e = 0;
        if (n >= 1e8 || n <= 1e-3) {
            while (n >= 10) {
                n /= 10;
                e++;
            }
            while (n < 1) {
                n *= 10;
                e--;
            }
        }
        int p = pos;
        long nn = (long) n;
        n -= nn;
        while (true) {
            buff[pos++] = (byte) ('0' + (nn % 10));
            nn /= 10;
            if (nn == 0) {
                break;
            }
        }
        int t = pos - 1;
        while(p < t) {
            byte tmp = buff[p];
            buff[p] = buff[t];
            buff[t] = tmp;
            p++;
            t--;
        }
        buff[pos++] = '.';
        n = (n - (long) n) * 10;
        long y = (long) (n * 100_000_000_000_000_000L);
        for (int i = 0; i <= 18 && y > 0; i++) {
            long x = y / 100_000_000_000_000_000L;
            buff[pos++] = (byte) ('0' + (x % 10));
            y -= x * 100_000_000_000_000_000L;
            y *= 10;
        }
        if (e != 0) {
            buff[pos++] = 'e';
            if (e < 0) {
                buff[pos++] = '-';
                e = -e;
            }
            buff[pos++] = (byte) ('0' + (e / 100));
            buff[pos++] = (byte) ('0' + ((e / 10) % 10));
            buff[pos++] = (byte) ('0' + (e % 10));
        }
        return new String(buff, 0, pos, StandardCharsets.UTF_8);
    }

    public static String convertDoubleToString2alt(double n) {
        if (Double.isNaN(n)) {
            return "NaN";
        } else if (n == Double.POSITIVE_INFINITY) {
            return "Infinity";
        } else if (n == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        int pos = 0, e = 0;
        if (n < 0) {
            pos = 1;
            n = -n;
        }
        if (n != 0 && (n >= 1e8 || n <= 1e-3)) {
            while (n >= 10) {
                n /= 10;
                e++;
            }
            while (n < 1) {
                n *= 10;
                e--;
            }
        }
        String aa = convertLongToString((long) n);
        long b = (long) ((n - (long) n) * 1_000_000_000_000_000_000L) + 1_000_000_000_000_000_000L;
        while (b >= 100 && b % 10 == 0) {
            b /= 10;
        }
        String bb = convertLongToString(b);
        String ee = convertLongToString(e);
        int len = pos + aa.length() + bb.length();
        if (e != 0) {
            len += 1 + ee.length();
        }
        byte[] buff = new byte[len];
        buff[0] = '-';
        for (int i = 0; i < aa.length(); i++) {
            buff[pos + i] = (byte) aa.charAt(i);
        }
        buff[pos + aa.length()] = '.';
        for (int i = 1; i < bb.length(); i++) {
            buff[pos + aa.length() + i] = (byte) bb.charAt(i);
        }
        if (e != 0) {
            buff[pos + aa.length() + bb.length()] = 'e';
            for (int i = 0; i < ee.length(); i++) {
                buff[pos + aa.length() + bb.length() + 1 + i] = (byte) ee.charAt(i);
            }
        }
        return new String(buff, StandardCharsets.UTF_8);
    }

    public static String convertDoubleToString2(double n) {
        if (Double.isNaN(n)) {
            return "NaN";
        }
        if (n == Double.POSITIVE_INFINITY) {
            return "Infinity";
        }
        if (n == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        int e = 0;
        byte[] buff = new byte[32];
        int p = 0;
        if (n < 0) {
            buff[p++] = '-';
            n = -n;
        }
        if (n != 0 && (n >= 1e8 || n <= 1e-3)) {
            while (n >= 10) {
                n /= 10;
                e++;
            }
            while (n < 1) {
                n *= 10;
                e--;
            }
        }
        String aa = convertLongToString((long) n);
        for (int i = 0; i < aa.length(); i++) {
            buff[p++] = (byte) aa.charAt(i);
        }
        buff[p++] = '.';
        long b = (long) ((n - (long) n) * 1_000_000_000_000_000_000L) + 1_000_000_000_000_000_000L;
        while (b >= 100 && b % 10 == 0) {
            b /= 10;
        }
        String bb = convertLongToString(b);
        for (int i = 1; i < bb.length(); i++) {
            buff[p++] = (byte) bb.charAt(i);
        }
        if (e != 0) {
            buff[p++] = 'e';
            String ee = convertLongToString(e);
            for (int i = 0; i < ee.length(); i++) {
                buff[p++] = (byte) ee.charAt(i);
            }
        }
        return new String(buff, 0, p, StandardCharsets.UTF_8);
    }

}
