package org.bau.stdlib.math;

public class IntMath {

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
        // notice >>> instead of >>
        // alternatively, use (x >> y) & 0xffffffffL
        long xh = x >>> 32;
        long yh = y >>> 32;
        long xl = x & 0xffffffffL;
        long yl = y & 0xffffffffL;
        long a = ((xl * yl) >>> 32) + (xh * yl);
        long b = (a & 0xffffffffL) + (xl * yh);
        return (a >>> 32) + (b >>> 32) + (xh * yh);
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

    public static long nextPowerOf2(long x) {
        if (x <= 1) {
            return 1;
        }
        return (-1L >>> Long.numberOfLeadingZeros(x - 1)) + 1;
    }

    public static long compareUnsigned(long a, long b) {
        a += 0x8000000000000000L;
        b += 0x8000000000000000L;
        if (a == b) {
            return 0;
        } else if (a < b) {
            return -1;
        }
        return 1;
    }

    public static long divide128Unsigned(long x1, long x0, long y) {
        if (y == 0) {
            // division by 0
            return ~0L;
        }
        if (Long.compareUnsigned(y, x1) <= 0) {
            return ~0;
        }
        if (x1 == 0) {
            return Long.divideUnsigned(x0, y);
        }
        int shift = Long.numberOfLeadingZeros(y);
        y <<= shift;
        long mask = (1L << 32) - 1;
        long u = (x1 << shift);
        if (shift != 0) {
            u |= x0 >>> (64 - shift);
        }
        long un = x0 << shift;
        long un1 = un >>> 32;
        long un0 = un & mask;
        long y1 = y >>> 32;
        long y0 = y & mask;
        long q1 = Long.divideUnsigned(u, y1);
        long rhat = u - q1 * y1;
        while (q1 >>> 32 > 0
                || Long.compareUnsigned(q1 * y0, (rhat << 32) + un1) > 0) {
            q1--;
            rhat += y1;
            if (rhat >>> 32 > 0) {
                break;
            }
        }
        long v = (u << 32) + un1 - q1 * y;
        long q0 = Long.divideUnsigned(v, y1);
        rhat = v - q0 * y1;
        while (q0 >>> 32 > 0
                || Long.compareUnsigned(q0 * y0, (rhat << 32) + un0) > 0) {
            q0--;
            rhat += y1;
            if (rhat >>> 32 > 0) {
                break;
            }
        }
        return (q1 << 32) + q0;
    }

}
