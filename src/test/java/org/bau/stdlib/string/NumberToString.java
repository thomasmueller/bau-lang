package org.bau.stdlib.string;

import java.nio.charset.StandardCharsets;

public class NumberToString {

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
            return convertDoubleToString(n) + "E" + e;
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

}
