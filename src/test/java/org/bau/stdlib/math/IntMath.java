package org.bau.stdlib.math;

public class IntMath {

    public static long hash(long x) {
        x = (x ^ (x >>> 33)) * 0xff51afd7ed558ccdL;
        x = (x ^ (x >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return x ^ (x >>> 33);
    }

    public static int reduce(long hash, int n) {
        return (int) (((hash & 0xffffffffL) * (n & 0xffffffffL)) >>> 32);
    }

    public static long multiplyHighSigned(long x, long y) {
        long xh = x >> 32;
        long yh = y >> 32;
        long xl = x & 0xffffffffL;
        long yl = y & 0xffffffffL;
        long a = ((xl * yl) >>> 32) + (xh * yl);
        long b = (a & 0xffffffffL) + (xl * yh);
        return (a >> 32) + (b >> 32) + (xh * yh);
    }

    public static long multiplyHighUnsigned(long x, long y) {
        long xh = ((x >>> 32) & 0xffffffffL);
        long yh = ((y >>> 32) & 0xffffffffL);
        long xl = x & 0xffffffffL;
        long yl = y & 0xffffffffL;
        long a = ((xl * yl) >>> 32) + (xh * yl);
        long b = (a & 0xffffffffL) + (xl * yh);
        return ((a >>> 32) & 0xffffffffL) + ((b >>> 32) & 0xffffffffL) + (xh * yh);
    }

    public static long signum(long x) {
        return (x >> 63) | (-x >>> 63);
    }

    public static int bitCount(long x) {
        return bitCountInt((int) x) + bitCountInt((int) (x >>> 32));
    }

    public static int bitCountInt(int x) {
        x -= (x >>> 1) & 0x55555555;
        x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
        x = ((x + (x >>> 4)) & 0x0F0F0F0F) * 0x01010101;
        return x >>> 24;
    }

    public static long reverseBytes(long x) {
        x = ((x >>> 8) & 0x00ff00ff00ff00ffL) | ((x & 0x00ff00ff00ff00ffL) << 8);
        return (x >>> 48) | ((x >>> 16) & 0xffff0000L) | ((x & 0xffff0000L) << 16) | (x << 48);
    }

    public static long reverseBits(long x) {
        x = (x >>> 1) & 0x5555555555555555L | (x & 0x5555555555555555L) << 1;
        x = (x >>> 2) & 0x3333333333333333L | (x & 0x3333333333333333L) << 2;
        x = (x >>> 4) & 0x0f0f0f0f0f0f0f0fL | (x & 0x0f0f0f0f0f0f0f0fL) << 4;
        return reverseBytes(x);
    }

    public static long rotateRight(long x, int count) {
        return (x << -count) | (x >>> count);
    }

    public static long highestBitSet(long x) {
        return ((1L << 63) >>> countLeadingZeros(x)) & x;
    }

    public static long lowestBitSet(long x) {
        return -x & x;
    }

    public static long rotateLeft(long x, int count) {
        return (x << count) | (x >>> -count);
    }

    public static long shiftRightArithmetic(long i, int count) {
        return ((-1L * (i >>> 63)) << (63 - count)) | (i >>> count);
    }

    public static long countLeadingZeros(long x) {
        return countTrailingZeros(reverseBits(x));
    }

    public static long countTrailingZeros(long x) {
        if (x == 0) {
            return 64;
        }
        return bitCount(x ^ (x - 1)) - 1;
    }

    public static long min(long x, long y) {
        if (x < y) {
            return x;
        }
        return y;
    }

    public static long max(long x, long y) {
        if (x > y) {
            return x;
        }
        return y;
    }

}
