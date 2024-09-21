package org.bau.math.bigint;

import java.math.BigInteger;
import java.util.Arrays;

public class BigInt implements Comparable<BigInt> {
    private final static int KARATSUBA_LIMIT = 100;
    private final int[] data;
    private final boolean negative;

    private BigInt(long value) {
        if (value <= Integer.MAX_VALUE) {
            this.data = new int[] { (int) value };
        } else {
            this.data = new int[] { (int) value, (int) (value >>> 32) };
        }
        this.negative = false;
    }

    private BigInt(int[] data, boolean negative) {
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
        if (x < 0) {
            return valueOf(-x).negate();
        }
        return new BigInt(x);
    }

    public int len() {
        if (data.length == 0) {
            return 0;
        }
        int lastLen = 32 - Long.numberOfLeadingZeros(data[data.length - 1] & 0xffffffffL);
        return data.length * 32 + lastLen;
    }

    public BigInt shiftLeft(int n) {
        if (len() <= 0) {
            return BigInt.valueOf(0);
        }
        int len2 = len() + n;
        int[] data2 = new int[(len2 + 31) / 32];
        long carry = 0;
        for (int i = 0; i < len(); i += 32) {
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

        // int[] data2 = new int[(len2 + 31) / 32];
        // for (int i = 0; i < len2; i++) {
        //     data2[i >>> 5] |= (data[(i + n) >>> 5] >>> (i + n)) << i;
        // }

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
        return new BigInt(data, !negative);
    }

    private BigInt subtract(int[] a, int[] b) {
        if (a.length < b.length) {
            int[] temp = a;
            a = b;
            b = temp;
        }
        // now a.len >= b.len
        int[] result = Arrays.copyOf(a, a.length);
        int carry = 0;
        for (int i = 0; i < a.length; i++) {
            long x = result[i] & 0xffffffffL;
            long y = 0;
            if (i < b.length) {
                y = b[i] & 0xffffffffL;
            }
            long z = x - y - carry;
            if (z < 0) {
                carry = 1;
            } else {
                carry = 0;
            }
            result[i] = (int) z;
        }
        if (carry == 1) {
            result = Arrays.copyOf(result, result.length + 1);
            result[result.length - 1] = 1;
        }
        return new BigInt(result, false);
    }

    private BigInt multiply(int[] a, int[] b) {
        if (a.length < b.length) {
            int[] temp = a;
            a = b;
            b = temp;
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

    private BigInt multiplySmall(int[] a, int[] b) {
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
        if (this.len() == 0) {
            return this;
        } else if (other.len() == 0) {
            return other;
        }
        if (negative != other.negative) {
            return multiply(other.negate()).negate();
        }
        return multiply(data, other.data);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BigInt)) {
            return false;
        }
        BigInt o = (BigInt) other;
        if (len() != o.len()) {
            return false;
        }
        for (int i = 0; i < data.length && i < o.data.length; i++) {
            if (data[i] != o.data[i]) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        if (negative) {
            return "-" + negate().toString();
        }
        BigInteger temp = BigInteger.ZERO;
        for (int i = data.length - 1; i >= 0; i--) {
            temp = temp.shiftLeft(32);
            temp = temp.add(BigInteger.valueOf(data[i] & 0xffffffffL));
        }
        return temp.toString();
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

    public BigInt reminder(BigInt other) {
        BigInt div = this.divide(other);
        return this.subtract(div.multiply(other));
    }

    public BigInt divide(BigInt other) {
        if (other.len() == 0) {
            return null;
        }
        if (negative != other.negative) {
            return divide(other.negate()).negate();
        }
        if (negative) {
            return negate().divide(other).negate();
        } else if (other.negative) {
            return divide(other.negate()).negate();
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
        while (shifted.compareTo(remainder) < 0) {
            shifted = shifted.shiftLeft(1);
            shiftCount++;
        }
        shifted = shifted.shiftRight(1);
        shiftCount--;
        while (remainder.compareTo(other) > 0) {
            result = result.add(BigInt.valueOf(1).shiftLeft(shiftCount));
            remainder = remainder.subtract(shifted);
            while (shifted.compareTo(remainder) > 0) {
                shifted = shifted.shiftRight(1);
                shiftCount--;
            }
        }
        return result;
    }

}
