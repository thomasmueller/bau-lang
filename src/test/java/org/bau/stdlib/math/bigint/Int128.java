package org.bau.stdlib.math.bigint;

import java.nio.charset.StandardCharsets;

public class Int128 {

    // x0: lowest, x3: highest
    private final int x0, x1, x2, x3;

    public static final Int128 MIN_VALUE = new Int128(0, 0, 0, Integer.MIN_VALUE);
    public static final Int128 MAX_VALUE = new Int128(-1, -1, -1, Integer.MAX_VALUE);
    public static final Int128 ZERO = new Int128(0, 0, 0, 0);
    public static final Int128 ONE = new Int128(1, 0, 0, 0);

    public Int128(int x0, int x1, int x2, int x3) {
        this.x0 = x0;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
    }

    public static Int128 valueOf(long x) {
        int high = -(int) (x >>> 63);
        return new Int128((int) (x), (int) (x >>> 32), high, high);
    }

    public static Int128 valueOf(String string) {
        if (string.startsWith("-")) {
            return valueOf(string.substring(1)).negate();
        }
        Int128 x = Int128.valueOf(0);
        for(char c : string.toCharArray()) {
            x = x.multiply(Int128.valueOf(10));
            x = x.add(Int128.valueOf((int) (c - '0')));
        }
        return x;
    }

    public Int128 add(Int128 other) {
        long r0 = (x0 & 0xffffffffL) + (other.x0 & 0xffffffffL);
        long r1 = (x1 & 0xffffffffL) + (other.x1 & 0xffffffffL) + (r0 >>> 32);
        long r2 = (x2 & 0xffffffffL) + (other.x2 & 0xffffffffL) + (r1 >>> 32);
        long r3 = (x3 & 0xffffffffL) + (other.x3 & 0xffffffffL) + (r2 >>> 32);
        return new Int128((int) r0, (int) r1, (int) r2, (int) r3);
    }

    public Int128 subtract(Int128 other) {
        return add(other.negate());
    }

    private int sign() {
        return (x3 >>> 63) & 1;
    }

    public Int128 multiply(Int128 other) {
        long r0 = (x0 & 0xffffffffL) * (other.x0 & 0xffffffffL);
        long r1 = (x0 & 0xffffffffL) * (other.x1 & 0xffffffffL) + (r0 >>> 32);
        long r2 = (x0 & 0xffffffffL) * (other.x2 & 0xffffffffL) + (r1 >>> 32);
        long r3 = (x0 & 0xffffffffL) * (other.x3 & 0xffffffffL) + (r2 >>> 32);
        r0 &= 0xffffffffL;
        r1 &= 0xffffffffL;
        r2 &= 0xffffffffL;
        r1 += (x1 & 0xffffffffL) * (other.x0 & 0xffffffffL);
        r2 += (x1 & 0xffffffffL) * (other.x1 & 0xffffffffL) + (r1 >>> 32);
        r3 += (x1 & 0xffffffffL) * (other.x2 & 0xffffffffL) + (r2 >>> 32);
        r1 &= 0xffffffffL;
        r2 &= 0xffffffffL;
        r2 += (x2 & 0xffffffffL) * (other.x0 & 0xffffffffL);
        r3 += (x2 & 0xffffffffL) * (other.x1 & 0xffffffffL) + (r2 >>> 32);
        r2 &= 0xffffffffL;
        r3 += (x3 & 0xffffffffL) * (other.x0 & 0xffffffffL) + (r2 >>> 32);
        return new Int128((int) r0, (int) r1, (int) r2, (int) r3);
    }

    public Int128 remainder(Int128 other) {
        Int128 div = divide(other);
        return subtract(div.multiply(other));
    }

    public Int128 divide(Int128 other) {
        int signum = signum();
        if (other.signum() == 0) {
            if (signum == 0) {
                return this;
            } else if (signum == 1) {
                return MAX_VALUE;
            }
            return MIN_VALUE;
        } else if (signum == 0) {
            return this;
        } else if (signum == 1) {
            return negate().divide(other.negate());
        } if (sign() != other.sign()) {
            return divide(other.negate()).negate();
        }
        int cmp = compareTo(other);
        if (cmp > 0) {
            return ZERO;
        } else if (cmp == 0) {
            return ONE;
        }
        Int128 remainder = this;
        Int128 result = Int128.valueOf(0);
        Int128 shifted = other;
        int shiftCount = 0;
        Int128 old = shifted;
        while (shifted.compareTo(remainder) >= 0) {
            old = shifted;
            shifted = shifted.shiftLeft(1);
            shiftCount++;
        }
        shifted = old;
        shiftCount--;
        while (remainder.compareTo(other) <= 0) {
            result = result.add(Int128.valueOf(1).shiftLeft(shiftCount));
            remainder = remainder.subtract(shifted);
            while (shiftCount > 0 && shifted.compareTo(remainder) <= 0) {
                shifted = shifted.shiftRightArithmetic(1);
                shiftCount--;
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Int128)) {
            return false;
        }
        Int128 o = (Int128) other;
        return x0 == o.x0 &&
                x1 == o.x1 &&
                x2 == o.x2 &&
                x3 == o.x3;
    }

    private long intValue() {
        return sign() == 1 ? -x0 : x0;
    }

    public long longValue() {
        return (((long) x1) << 32) | (x0 & 0xffffffffL);
    }

    public String toString() {
        Int128 n = this;
        byte[] buff = new byte[50];
        int start = 0;
        if (n.sign() == 1) {
            buff[start++] = '-';
        } else {
            n = n.negate();
        }
        int i = start;
        Int128 group = Int128.valueOf(1_000_000_000);
        do {
            Int128 next = n.divide(group);
            Int128 remainder = n.subtract(next.multiply(group));
            long val = remainder.intValue();
            i = longToString(val, buff, i, 9);
            n = next;
        } while (n.sign() == 1);
        while (i > start + 1 && buff[i - 1] == '0') {
            i--;
        }
        int end = i;
        while (i > start) {
            i--;
            byte temp = buff[i];
            buff[i] = buff[start];
            buff[start] = temp;
            start++;
        }
        return new String(buff, 0, end, StandardCharsets.UTF_8);
    }

    private static int longToString(long n, byte[] buff, int start, int size) {
        int i = 0;
        for(; i < size; i++) {
            buff[start + i] = (byte) ('0' + (n % 10));
            n /= 10;
        }
        return i + start;
    }

    public Int128 negate() {
        long r0 = (~x0 & 0xffffffffL) + 1;
        long r1 = (~x1 & 0xffffffffL) + (r0 >>> 32);
        long r2 = (~x2 & 0xffffffffL) + (r1 >>> 32);
        long r3 = (~x3 & 0xffffffffL) + (r2 >>> 32);
        return new Int128((int) r0, (int) r1, (int) r2, (int) r3);
    }

    public Int128 shiftLeft(int digits) {
        int n = digits & 31;
        long m0 = x0 & 0xffffffffL;
        long m1 = x1 & 0xffffffffL;
        long m2 = x2 & 0xffffffffL;
        long m3 = x3 & 0xffffffffL;
        int t0 = (int) (m0 << n);
        int t1 = (int) ((m1 << n) | (m0 >>> (32 - n)));
        int t2 = (int) ((m2 << n) | (m1 >>> (32 - n)));
        int t3 = (int) ((m3 << n) | (m2 >>> (32 - n)));
        if (digits < 32) {
            return new Int128(t0,  t1,  t2,  t3);
        } else if (digits < 64) {
            return new Int128(0,  t0,  t1,  t2);
        } else if (digits < 96) {
            return new Int128(0, 0,  t0,  t1);
        } else {
            return new Int128(0, 0, 0,  t0);
        }
    }

    public Int128 shiftRight(int digits) {
        int n = digits & 31;
        long m0 = x0 & 0xffffffffL;
        long m1 = x1 & 0xffffffffL;
        long m2 = x2 & 0xffffffffL;
        long m3 = x3 & 0xffffffffL;
        int t3 = (int) (m3 >>> n);
        int t2 = (int) ((m2 >>> n) | (m3 << (32 - n)));
        int t1 = (int) ((m1 >>> n) | (m2 << (32 - n)));
        int t0 = (int) ((m0 >>> n) | (m1 << (32 - n)));
        if (digits < 32) {
            return new Int128(t0, t1, t2, t3);
        } else if (digits < 64) {
            return new Int128(t1, t2, t3, 0);
        } else if (digits < 96) {
            return new Int128(t2, t3, 0, 0);
        } else {
            return new Int128(t3, 0, 0, 0);
        }
    }

    public Int128 shiftRightArithmetic(int digits) {
        int n = digits & 31;
        int fill = -sign();
        long m0 = x0 & 0xffffffffL;
        long m1 = x1 & 0xffffffffL;
        long m2 = x2 & 0xffffffffL;
        long m3 = x3;
        int t3 = (int) (m3 >> n);
        int t2 = (int) ((m2 >>> n) | (m3 << (32 - n)));
        int t1 = (int) ((m1 >>> n) | (m2 << (32 - n)));
        int t0 = (int) ((m0 >>> n) | (m1 << (32 - n)));
        if (digits < 32) {
            return new Int128(t0, t1, t2, t3);
        } else if (digits < 64) {
            return new Int128(t1, t2, t3, fill);
        } else if (digits < 96) {
            return new Int128(t2, t3, fill, fill);
        } else {
            return new Int128(t3, fill, fill, fill);
        }
    }

    public int compareTo(Int128 other) {
        return subtract(other).signum();
    }

    public int signum() {
        if (x0 == 0 && x1 == 0 && x2 == 0 && x3 == 0) {
            return 0;
        } else if (x3 < 0) {
            return -1;
        }
        return 1;
    }

    public Int128 abs() {
        if (x3 < 0) {
            return negate();
        }
        return this;
    }

}
