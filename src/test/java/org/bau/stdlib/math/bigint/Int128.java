package org.bau.stdlib.math.bigint;

import java.nio.charset.StandardCharsets;

import org.bau.stdlib.math.IntMath;

public class Int128 {

    private final long low, high;

    public static final Int128 MIN_VALUE = new Int128(0, Long.MIN_VALUE);
    public static final Int128 MAX_VALUE = new Int128(-1, Long.MAX_VALUE);
    public static final Int128 ZERO = new Int128(0, 0);
    public static final Int128 ONE = new Int128(1, 0);

    public Int128(long low, long high) {
        this.low = low;
        this.high = high;
    }

    public static Int128 valueOf(long x) {
        int high = -(int) (x >>> 63);
        return new Int128(x, high);
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
        long l = low + other.low;
        long c = ((low & other.low) | ((low | other.low) & ~l)) >>> 63;
        long h = high + other.high + c;
        return new Int128(l, h);
    }

    public Int128 subtract(Int128 other) {
        return add(other.negate());
    }

    private long sign() {
        return (high >>> 63) & 1;
    }

    public Int128 multiply(Int128 other) {
        long h = IntMath.multiplyHighUnsigned(low, other.low);
        h += high * other.low + low * other.high;
        return new Int128(low * other.low, h);
    }

    public Int128 remainder(Int128 other) {
        return subtract(divide(other).multiply(other));
    }

    public Int128 divide(Int128 other) {
        int signum = signum();
        if (other.signum() == 0) {
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
        if (other.high == 0) {
            long q = IntMath.divide128Unsigned(high, low, other.low);
            return new Int128(q, 0);
        }
        // fallback (slow)
        Int128 remainder = this;
        Int128 result = Int128.valueOf(0);
        Int128 shifted = other;
        int shiftCount = 0;
        Int128 old = shifted;
        while (shifted.compareUnsigned(remainder) >= 0) {
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
        return low == o.low && high == o.high;
    }

    private long intValue() {
        return sign() == 1 ? -low : low;
    }

    public long longValue() {
        return low;
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
        long l = ~low + 1;
        long h = ~high;
        if (l == 0) {
            h++;
        }
        return new Int128(l, h);
    }

    public Int128 shiftLeft(int n) {
        if (n == 0) {
            return this;
        } else if (n < 64) {
            long h = (high << n) | (low >>> (64 - n));
            long l  = low << n;
            return new Int128(l, h);
        } else if (n < 128) {
            return new Int128(0, low << (n - 64));
        }
        return new Int128(0, 0);
    }

    public Int128 shiftRight(int n) {
        if (n == 0) {
            return this;
        } else if (n < 64) {
            long l  = (low >>> n) | (high << (64 - n));
            long h = high >>> n;
            return new Int128(l, h);
        } else if (n < 128) {
            return new Int128(high >>> (n - 64), 0);
        }
        return new Int128(0, 0);
    }

    public Int128 shiftRightArithmetic(int n) {
        if (n == 0) {
            return this;
        } else if (n < 64) {
            long l  = (low >>> n) | (high << (64 - n));
            long h = high >> n;
            return new Int128(l, h);
        } else if (n < 128) {
            long l  = high >> (n - 64);
            long h = high >> 63;
            return new Int128(l, h);
        }
        long fill = high >> 63;
        return new Int128(fill, fill);
    }

    public int numberOfLeadingZeros() {
        if (high != 0) {
            return Long.numberOfLeadingZeros(high);
        }
        return 64 + Long.numberOfLeadingZeros(low);
    }

    public int compareUnsigned(Int128 other) {
        int comp = Long.compareUnsigned(high, other.high);
        if (comp != 0) {
            return comp;
        }
        return Long.compareUnsigned(low, other.low);
    }

    public int compareTo(Int128 other) {
        int comp = Long.compare(high, other.high);
        if (comp != 0) {
            return comp;
        }
        return Long.compareUnsigned(low, other.low);
    }

    public int signum() {
        if (low == 0 && high == 0) {
            return 0;
        } else if (high < 0) {
            return -1;
        }
        return 1;
    }

    public Int128 abs() {
        if (high < 0) {
            return negate();
        }
        return this;
    }

}
