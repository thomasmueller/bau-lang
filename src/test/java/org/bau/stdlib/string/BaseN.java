package org.bau.stdlib.string;

public class BaseN {

    static String toBinary(long x) {
        return toString(x, 2);
    }

    static String toString(long x, int base) {
        if (base < 2 || base > 10) {
            return null;
        }
        byte[] buff = new byte[65];
        int pos = 64;
        while (pos == 64 || x != 0) {
            long d = x % base;
            buff[pos--] = (byte) ('0' + d);
            x = x / base;
        }
        return new String(buff, pos + 1, 64 - pos);
    }

    static long parseBinary(String s) {
        return parseInt(s, 2);
    }

    static long parseInt(String s, int base) {
        if (base < 2 || base > 10) {
            return -1;
        }
        long x = 0;
        for (int i = 0; i < s.length(); i++) {
            x = x * base + ((s.charAt(i) - '0'));
        }
        return x;
    }

}
