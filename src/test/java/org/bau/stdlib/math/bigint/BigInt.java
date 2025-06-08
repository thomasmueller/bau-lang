package org.bau.stdlib.math.bigint;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BigInt implements Comparable<BigInt> {
    private final static int KARATSUBA_LIMIT = 100;
    private final int[] data;
    private final boolean negative;

    private BigInt(long value) {
        if (value == 0) {
            this.data = new int[] { };
            this.negative = false;
        } else if (value < 0) {
            if (value >= Integer.MIN_VALUE) {
                this.data = new int[] { (int) -value };
            } else if (value > Long.MIN_VALUE) {
                this.data = new int[] { (int) -value, (int) (-value) >>> 32 };
            } else {
                this.data = new int[] { 0, 0x80000000 };
            }
            this.negative = true;
        } else {
            if (value <= Integer.MAX_VALUE) {
                this.data = new int[] { (int) value };
            } else {
                this.data = new int[] { (int) value, (int) (value >>> 32) };
            }
            this.negative = false;
        }
    }

    public BigInt(int[] data, boolean negative) {
        while (data.length > 0 && data[data.length - 1] == 0) {
            data = Arrays.copyOf(data, data.length - 1);
        }
        if (data.length == 0) {
            negative = false;
        }
        this.data = data;
        this.negative = negative;
    }

    public static BigInt valueOf(long x) {
        return new BigInt(x);
    }

    private int len() {
        if (data.length == 0) {
            return 0;
        }
        int lastLen = 64 - Long.numberOfLeadingZeros(data[data.length - 1] & 0xffffffffL);
        return (data.length - 1) * 32 + lastLen;
    }

    public BigInt shiftLeft(int n) {
        int len = len();
        if (len <= 0) {
            return BigInt.valueOf(0);
        }
        int len2 = len + n;
        int[] data2 = new int[(len2 + 31) / 32];
        long carry = 0;
        for (int i = 0; i < len; i += 32) {
            long old = data[i >>> 5] & 0xffffffffL;
            data2[(i + n) >>> 5] = (int) ((old << (n & 31)) | carry);
            carry = old >>> (32 - (n & 31));
        }
        data2[data2.length - 1] |= (long) carry;
        return new BigInt(data2, negative);
    }

    public BigInt shiftRight(int n) {
        int len2 = len() - n;
        if (len2 <= 0) {
            return BigInt.valueOf(0);
        }
        int[] data2 = Arrays.copyOfRange(data, n / 32, data.length);
        long carry = 0;
        for (int i = data2.length - 1; i >= 0; i--) {
            long x = data2[i] & 0xffffffffL;
            data2[i] = (int) ((carry | x) >>> (n & 31));
            carry = x << 32;
        }
        return new BigInt(data2, negative);
    }

    private BigInt add(int[] a, int[] b) {
        if (a.length < b.length) {
            int[] temp = a;
            a = b;
            b = temp;
        }
        // now a.len >= b.len
        int[] result = Arrays.copyOf(a, a.length);
        long carry = 0;
        for (int i = 0; i < a.length; i++) {
            long x = result[i] & 0xffffffffL;
            long y = i >= b.length ? 0 : b[i] & 0xffffffffL;
            long z = x + y + carry;
            result[i] = (int) z;
            carry = z >>> 32;
        }
        if (carry > 0) {
            result = Arrays.copyOf(result, result.length + 1);
            result[result.length - 1] = 1;
        }
        return new BigInt(result, false);
    }

    public BigInt negate() {
        if (data.length == 0) {
            return this;
        }
        return new BigInt(data, !negative);
    }

    public BigInt abs() {
        if (!negative) {
            return this;
        }
        return negate();
    }

    private static BigInt subtract(int[] a, int[] b) {
        int[] result = Arrays.copyOf(a, a.length);
        long carry = 0;
        for (int i = 0; i < a.length; i++) {
            long x = result[i] & 0xffffffffL;
            long y = 0;
            if (i < b.length) {
                y = b[i] & 0xffffffffL;
            }
            long z = x - y - carry;
            carry = (z >>> 63) & 1;
            result[i] = (int) z;
        }
        return new BigInt(result, false);
    }

    private static BigInt multiply(int[] a, int[] b) {
        if (a.length < b.length) {
            return multiply(b, a);
        }
        // now a.len >= b.len
        if (a.length < KARATSUBA_LIMIT || b.length < KARATSUBA_LIMIT) {
            return multiplySmall(a, b);
        }
        int half = a.length / 2;
        BigInt a0 = new BigInt(a, false).shiftRight(half * 32);
        BigInt a1 = new BigInt(Arrays.copyOf(a, half), false);
        BigInt b0 = new BigInt(b, false).shiftRight(half * 32);
        BigInt b1 = new BigInt(Arrays.copyOf(b, half), false);
        BigInt z0 = a0.multiply(b0);
        BigInt z1 = a0.add(a1).multiply(b0.add(b1));
        BigInt z2 = a1.multiply(b1);
        BigInt result = z0.shiftLeft(half * 64);
        result = result.add(z1.subtract(z0).subtract(z2).shiftLeft(half * 32));
        result = result.add(z2);
        return result;
    }

    private static BigInt multiplyVerySmall(int a, int[] b) {
        if (a == 1) {
            return new BigInt(b, false);
        }
        int[] result = new int[b.length + 1];
        long ax = a & 0xffffffffL;
        long carry = 0;
        int i = 0;
        for (int bi = 0; bi < b.length; bi++, i++) {
            long bx = b[bi] & 0xffffffffL;
            long z = ax * bx + (result[i] & 0xffffffffL) + carry;
            result[i] = (int) z;
            carry = z >>> 32;
        }
        for (;carry > 0; i++) {
            long z = result[i] + carry;
            result[i] = (int) z;
            carry = z >>> 32;
        }
        return new BigInt(result, false);
    }

    private static BigInt multiplySmall(int[] a, int[] b) {
        if (a.length == 1) {
            return multiplyVerySmall(a[0], b);
        } else if (b.length == 1) {
            return multiplyVerySmall(b[0], a);
        }
        int[] result = new int[a.length + b.length];
        for (int ai = 0; ai < a.length; ai++) {
            long ax = a[ai] & 0xffffffffL;
            long carry = 0;
            int i = ai;
            for (int bi = 0; bi < b.length; bi++, i++) {
                long bx = b[bi] & 0xffffffffL;
                long z = ax * bx + (result[i] & 0xffffffffL) + carry;
                result[i] = (int) z;
                carry = z >>> 32;
            }
            for (;carry > 0; i++) {
                long z = result[i] + carry;
                result[i] = (int) z;
                carry = z >>> 32;
            }
        }
        return new BigInt(result, false);
    }

    public BigInt subtract(BigInt other) {
        if (this.len() == 0) {
            return other.negate();
        } else if (other.len() == 0) {
            return this;
        }
        if (negative) {
            if (!other.negative) {
                return negate().add(other).negate();
            }
            return negate().subtract(other.negate()).negate();
        }
        if (other.negative) {
            return add(other.negate());
        }
        if (compareTo(other) < 0) {
            return other.subtract(this).negate();
        }
        return subtract(data, other.data);
    }

    public BigInt add(BigInt other) {
        if (this.len() == 0) {
            return other;
        } else if (other.len() == 0) {
            return this;
        }
        if (negative) {
            if (other.negative) {
                return add(data, other.data).negate();
            }
            return negate().subtract(other).negate();
        }
        if (other.negative) {
            return subtract(other.negate());
        }
        return add(data, other.data);
    }

    public BigInt multiply(BigInt other) {
        int thisLen = this.len();
        int otherLen = other.len();
        if (thisLen == 0) {
            return this;
        } else if (otherLen == 0) {
            return other;
        }
        if (negative != other.negative) {
            return multiply(other.negate()).negate();
        }
        if (thisLen > otherLen) {
            return other.multiply(this);
        } else if (thisLen == 1) {
            // multiply by 1 / -1
            if (negative) {
                return other.negate();
            }
            return other;
        }
        return multiply(data, other.data);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BigInt)) {
            return false;
        }
        BigInt o = (BigInt) other;
        if (len() != o.len() || negative != o.negative) {
            return false;
        }
        for (int i = 0; i < data.length && i < o.data.length; i++) {
            if (data[i] != o.data[i]) {
                return false;
            }
        }
        return true;
    }

    public int intValue() {
        return signum() * (data.length == 0 ? 0 : data[0] & 0xffffffff);
    }

    public String toString() {
        BigInt n = this;
        byte[] buff = new byte[50];
        int start = 0;
        if (negative) {
            buff[start++] = '-';
            n = n.negate();
        }
        int i = start;
        BigInt group = BigInt.valueOf(1_000_000_000L);
        do {
            BigInt next = n.divide(group);
            BigInt remainder = n.subtract(next.multiply(group));
            long val = remainder.intValue();
            if (i + 9 >= buff.length) {
                buff = Arrays.copyOf(buff, buff.length * 2);
            }
            i = longToString(val, buff, i, 9);
            n = next;
        } while (n.signum() != 0);
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

    public static int longToString(long n, byte[] buff, int start, int size) {
        int i = 0;
        for(; i < size; i++) {
            buff[start + i] = (byte) ('0' + (n % 10));
            n /= 10;
        }
        return i + start;
    }

    @Override
    public int compareTo(BigInt o) {
        if (negative != o.negative) {
            return negative ? -1 : 1;
        }
        if (negative) {
            return -negate().compareTo(o.negate());
        }
        if (len() != o.len()) {
            return len() > o.len() ? 1 : -1;
        }
        for (int i = data.length - 1; i >= 0; i--) {
            long x = data[i] & 0xffffffffL;
            long y = o.data[i] & 0xffffffffL;
            if (x != y) {
                return x > y ? 1 : -1;
            }
        }
        return 0;
    }

    public static BigInt valueOf(String string) {
        BigInt x = BigInt.valueOf(0);
        for(char c : string.toCharArray()) {
            x = x.multiply(BigInt.valueOf(10));
            x = x.add(BigInt.valueOf((int) (c - '0')));
        }
        return x;
    }

    public BigInt remainder(BigInt other) {
        BigInt div = divide(other);
        return subtract(div.multiply(other));
    }

    public BigInt divide(BigInt other) {
        if (other.len() == 0) {
            return null;
        } else if (negative != other.negative) {
            return divide(other.negate()).negate();
        } else if (negative) {
            return negate().divide(other.negate());
        }
        int cmp = compareTo(other);
        if (cmp < 0) {
            return new BigInt(new int[0], false);
        } else if (cmp == 0) {
            return new BigInt(new int[] { 1 }, false);
        }
        BigInt remainder = this;
        BigInt result = BigInt.valueOf(0);
        BigInt shifted = other;
        int shiftCount = 0;
        int shiftedLen = shifted.len();
        int len = remainder.len();
        if (len - shiftedLen > 1) {
            shifted = shifted.shiftLeft(len - shiftedLen - 1);
            shiftCount = len - shiftedLen - 1;
        }
        while (remainder.compareTo(other) >= 0) {
            result = result.add(BigInt.valueOf(1).shiftLeft(shiftCount));
            remainder = remainder.subtract(shifted);
            while (shiftCount > 0 && shifted.compareTo(remainder) >= 0) {
                shifted = shifted.shiftRight(1);
                shiftCount--;
            }
        }
        return result;
    }

    public int signum() {
        if (len() == 0) {
            return 0;
        } else if (negative) {
            return -1;
        }
        return 1;
    }

}
