package org.bau.stdlib.util;

public class Hash {

    public static long hash(long x) {
        x = (x ^ (x >>> 33)) * 0xff51afd7ed558ccdL;
        x = (x ^ (x >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return x ^ (x >>> 33);
    }

    public static int reduce(long hash, int n) {
        return (int) (((hash & 0xffffffffL) * (n & 0xffffffffL)) >>> 32);
    }

    public static long hashCode(byte[] data) {
        if (data.length <= 0) {
            return 0;
        }
        long h = data.length << 32;
        int i = 0;
        if (i < data.length - 3) {
            while (true) {
                int x = (data[i] & 0xff) |
                        ((data[i + 1] & 0xff) << 8) |
                        ((data[i + 2] & 0xff) << 16) |
                        ((data[i + 3] & 0xff) << 24);
                h = (h + x) * 0x9e3779b185ebca87L;
                int n = i + 4;
                if (n >= data.length) {
                    break;
                }
                i = n;
            }
        }
        if (i < data.length && i != data.length - 4) {
            int x = 0;
            int s = 0;
            while (true) {
                x ^= (data[i] & 0xff) << s;
                s += 8;
                int n = i + 1;
                if (n >= data.length) {
                    break;
                }
                i = n;
            }
            h = (h + x) * 0x9e3779b185ebca87L;
        }
        return h ^ (h >>> 32);
    }

}
