package org.bau.stdlib.math.bigint;

import java.nio.charset.StandardCharsets;

public class Int128B {

    // x0: lowest, x3: highest
    private final int x0, x1, x2, x3;

    public static final Int128B MIN_VALUE = new Int128B(0, 0, 0, Integer.MIN_VALUE);
    public static final Int128B MAX_VALUE = new Int128B(-1, -1, -1, Integer.MAX_VALUE);
    public static final Int128B ZERO = new Int128B(0, 0, 0, 0);
    public static final Int128B ONE = new Int128B(1, 0, 0, 0);

    public Int128B(int x0, int x1, int x2, int x3) {
        this.x0 = x0;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
    }

    public static Int128B valueOf(long x) {
        int high = -(int) (x >>> 63);
        return new Int128B((int) (x), (int) (x >>> 32), high, high);
    }

    public static Int128B valueOf(String string) {
        if (string.startsWith("-")) {
            return valueOf(string.substring(1)).negate();
        }
        Int128B x = Int128B.valueOf(0);
        for(char c : string.toCharArray()) {
            x = x.multiply(Int128B.valueOf(10));
            x = x.add(Int128B.valueOf((int) (c - '0')));
        }
        return x;
    }

    public Int128B add(Int128B other) {
        long r0 = (x0 & 0xffffffffL) + (other.x0 & 0xffffffffL);
        long r1 = (x1 & 0xffffffffL) + (other.x1 & 0xffffffffL) + (r0 >>> 32);
        long r2 = (x2 & 0xffffffffL) + (other.x2 & 0xffffffffL) + (r1 >>> 32);
        long r3 = (x3 & 0xffffffffL) + (other.x3 & 0xffffffffL) + (r2 >>> 32);
        return new Int128B((int) r0, (int) r1, (int) r2, (int) r3);
    }

    public Int128B subtract(Int128B other) {
        return add(other.negate());
    }

    public Int128B multiply(Int128B other) {
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
        return new Int128B((int) r0, (int) r1, (int) r2, (int) r3);
    }

    public Int128B remainder(Int128B other) {
        Int128B div = divide(other);
        return subtract(div.multiply(other));
    }

    public Int128B divide(Int128B other) {
        int signum = signum();
        if (other.signum() == 0) {
            // division by zero
            return other;
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
        Int128B remainder = this;
        Int128B result = Int128B.valueOf(0);
        Int128B shifted = other;
        int shiftCount = 0;
        Int128B old = shifted;
        while (shifted.compareUnsigned(remainder) >= 0) {
            old = shifted;
            shifted = shifted.shiftLeft(1);
            shiftCount++;
        }
        shifted = old;
        shiftCount--;
        while (remainder.compareTo(other) <= 0) {
            result = result.add(Int128B.valueOf(1).shiftLeft(shiftCount));
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
        if (!(other instanceof Int128B)) {
            return false;
        }
        Int128B o = (Int128B) other;
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
        Int128B n = this;
        byte[] buff = new byte[50];
        int start = 0;
        if (n.sign() == 1) {
            buff[start++] = '-';
        } else {
            n = n.negate();
        }
        int i = start;
        Int128B group = Int128B.valueOf(1_000_000_000);
        do {
            Int128B next = n.divide(group);
            Int128B remainder = n.subtract(next.multiply(group));
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

    public Int128B negate() {
        long r0 = (~x0 & 0xffffffffL) + 1;
        long r1 = (~x1 & 0xffffffffL) + (r0 >>> 32);
        long r2 = (~x2 & 0xffffffffL) + (r1 >>> 32);
        long r3 = (~x3 & 0xffffffffL) + (r2 >>> 32);
        return new Int128B((int) r0, (int) r1, (int) r2, (int) r3);
    }

    public Int128B shiftLeft(int digits) {
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
            return new Int128B(t0,  t1,  t2,  t3);
        } else if (digits < 64) {
            return new Int128B(0,  t0,  t1,  t2);
        } else if (digits < 96) {
            return new Int128B(0, 0,  t0,  t1);
        } else {
            return new Int128B(0, 0, 0,  t0);
        }
    }

    public Int128B shiftRight(int digits) {
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
            return new Int128B(t0, t1, t2, t3);
        } else if (digits < 64) {
            return new Int128B(t1, t2, t3, 0);
        } else if (digits < 96) {
            return new Int128B(t2, t3, 0, 0);
        } else {
            return new Int128B(t3, 0, 0, 0);
        }
    }

    public Int128B shiftRightArithmetic(int digits) {
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
            return new Int128B(t0, t1, t2, t3);
        } else if (digits < 64) {
            return new Int128B(t1, t2, t3, fill);
        } else if (digits < 96) {
            return new Int128B(t2, t3, fill, fill);
        } else {
            return new Int128B(t3, fill, fill, fill);
        }
    }

    public int numberOfLeadingZeros() {
        if (x3 != 0) {
            return Integer.numberOfLeadingZeros(x3);
        } else if (x2 != 0) {
            return 32 + Integer.numberOfLeadingZeros(x2);
        } else if (x1 != 0) {
            return 64 + Integer.numberOfLeadingZeros(x1);
        }
        return 96 + Integer.numberOfLeadingZeros(x0);
    }

    public int compareUnsigned(Int128B other) {
        return subtract(other).signum();
    }

    public int compareTo(Int128B other) {
        int signCompare = Integer.compare(signum(), other.signum());
        if (signCompare != 0) {
            return signCompare;
        }
        return subtract(other).signum();
    }

    private int sign() {
        return (x3 >>> 63) & 1;
    }

    public int signum() {
        if (x0 == 0 && x1 == 0 && x2 == 0 && x3 == 0) {
            return 0;
        } else if (x3 < 0) {
            return -1;
        }
        return 1;
    }

    public Int128B abs() {
        if (x3 < 0) {
            return negate();
        }
        return this;
    }

}
