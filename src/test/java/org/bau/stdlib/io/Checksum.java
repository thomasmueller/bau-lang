package org.bau.stdlib.io;

public class Checksum {

    private static final int POLYNOMIAL = 0xedb88320;
    private static final int MOD_ADLER = 65521;

    public static long crc32(byte[] value) {
        int crc = 0xffffffff;
        for (byte b : value) {
            crc ^= b & 0xff;
            for (int i = 0; i < 8; i++) {
                int mask = -(crc & 1);
                crc = (crc >>> 1) ^ (POLYNOMIAL & mask);
            }
        }
        return (~crc) & 0xffffffffL;
    }


    public static int adler32(byte[] value) {
        int a = 1;
        int b = 0;
        for (byte x : value) {
            a = (a + (x & 0xff)) % MOD_ADLER;
            b = (b + a) % MOD_ADLER;
        }
        return (b << 16) | a;
    }

    public static int fletcher32(byte[] bytes) {
        int s1 = 0xffff, s2 = 0xffff;
        int i = 0, len = bytes.length & ~1;
        while (i < len) {
            // reduce after 360 words (each word is two bytes)
            for (int end = Math.min(i + 720, len); i < end;) {
                int x = ((bytes[i++] & 0xff) << 8) | (bytes[i++] & 0xff);
                s2 += s1 += x;
            }
            s1 = (s1 & 0xffff) + (s1 >>> 16);
            s2 = (s2 & 0xffff) + (s2 >>> 16);
        }
        if ((bytes.length & 1) != 0) {
            // odd length: append 0
            int x = (bytes[i] & 0xff) << 8;
            s2 += s1 += x;
        }
        s1 = (s1 & 0xffff) + (s1 >>> 16);
        s2 = (s2 & 0xffff) + (s2 >>> 16);
        return (s2 << 16) | s1;
    }

}
