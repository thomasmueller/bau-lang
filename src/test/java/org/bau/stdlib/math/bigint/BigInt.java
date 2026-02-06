package org.bau.stdlib.math.bigint;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.bau.stdlib.math.IntMath;

public class BigInt implements Comparable<BigInt> {
    private final static int KARATSUBA_LIMIT = 100;
    private final int[] data;
    private final boolean negative;

    private BigInt(long value) {
        if (value == 0) {
            this.data = new int[] { };
            this.negative = false;
        } else if (value < 0) {
            if (value >= (long) Integer.MIN_VALUE << 1) {
                this.data = new int[] { (int) -value };
            } else if (value > Long.MIN_VALUE) {
                this.data = new int[] { (int) -value, (int) ((-value) >>> 32) };
                assert data[1] != 0;
            } else {
                this.data = new int[] { 0, 0x80000000 };
            }
            this.negative = true;
        } else {
            if (value < 1L << 32) {
                this.data = new int[] { (int) value };
            } else {
                this.data = new int[] { (int) value, (int) (value >>> 32) };
                assert data[1] != 0;
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

    public int len() {
        if (data.length == 0) {
            return 0;
        }
        int lastLen = 64 - Long.numberOfLeadingZeros(data[data.length - 1] & 0xffffffffL);
        return (data.length - 1) * 32 + lastLen;
    }

    public BigInt shiftLeft(int n) {
        if (n < 0) {
            return shiftRight(-n);
        }
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
        if (n < 0) {
            return shiftLeft(-n);
        }
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
        int newSize = b.length;
        long last = b[b.length - 1] & 0xffffffff;
        if (((last | a) >>> 16) != 0) {
            newSize += 1;
        }
        int[] result = new int[newSize];
        long ax = a & 0xffffffffL;
        long carry = 0;
        int i = 0;
        for (int bi = 0; bi < b.length; bi++, i++) {
            long bx = b[bi] & 0xffffffffL;
            long z = ax * bx + (result[i] & 0xffffffffL) + carry;
            result[i] = (int) z;
            carry = z >>> 32;
        }
        if (carry != 0) {
            result[result.length - 1] = (int) carry;
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

	public long longValue() {
		if (data.length == 0) {
			return 0;
		}
		long result = data[0] & 0xffffffffL;
		if (data.length > 1) {
			result |= (data[1] & 0xffffffffL) << 32;
		}
		return signum() * result;
	}

    public int intValue() {
        return signum() * (data.length == 0 ? 0 : data[0]);
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
        for (char c : string.toCharArray()) {
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
        if (other.data.length == 0) {
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
        if (data.length > 2 && len() - other.len() < 16) {
            int x = shiftRight(other.len() - 16).intValue();
            int y = other.shiftRight(other.len() - 16).intValue();
            if (x / y == x / (y + 1) && x / y == x / (y - 1)) {
                return BigInt.valueOf(x / y);
            }
        }
        int otherLen = other.len();
        int shift = 31 - (otherLen & 31);
        otherLen += shift;
        if (otherLen < 3 * 32) {
            shift += 3 * 32;
        }
        if (shift > 0) {
            // ensure the first bit is set
            return shiftLeft(shift).divide(other.shiftLeft(shift));
        }
        int[] u = data;
        // vn.length is at least 4
        int[] vn = other.data;
        long b = 1L << 32;
        int n = vn.length + 1;
        int m = u.length + 1;
        int[] un = new int[m];
        System.arraycopy(u, 0, un, 0, m - 1);
        int[] q = new int[m - n + 1];
        long vn1 = vn[n - 2] & 0xffffffffL;
        long vn2 = vn[n - 3] & 0xffffffffL;
        for (int j = m - n; j >= 0; j--) {
            long aa = ((un[j + n - 1] & 0xffffffffL) * b) + (un[j + n - 2] & 0xffffffffL);
            long qhat = Long.divideUnsigned(aa, vn1);
            long rhat = aa - qhat * vn1;
            while (true) {
                if (qhat < b) {
                    long unnn = un[j + n - 3] & 0xffffffffL;
                    if (IntMath.compareUnsigned(qhat * vn2, rhat * b + unnn) <= 0) {
                        break;
                    }
                }
                qhat -= 1;
                rhat += vn1;
                if (rhat >= b) {
                    break;
                }
            }
            long carry = 0;
            for (int i = 0; i < n - 1; i++) {
                long p = qhat * (vn[i] & 0xffffffffL);
                long t = (un[i + j] & 0xffffffffL) - carry - (p & 0xffffffffL);
                un[i + j] = (int) t;
                carry = (p >>> 32) - (t >> 32);
            }
            long t = (un[j + n - 1] & 0xffffffffL) - carry;
            un[j + n - 1] = (int) t;
            q[j] = (int) qhat;
        }
        return new BigInt(q, false);
    }

    BigInt divideOld(BigInt other) {
      BigInt rem = this;
      long x = other.data[other.data.length - 1] & 0xffffffffL;
      int shiftBoth = 31 + Long.numberOfLeadingZeros(x);
      rem = rem.shiftLeft(shiftBoth);
      other = other.shiftLeft(shiftBoth);
      BigInt result = BigInt.valueOf(0);
      int shiftCount = (rem.data.length - other.data.length) * 32;
      long b = other.data[other.data.length - 1] & 0xffffffffL;
      BigInt shifted = other.shiftLeft(shiftCount).shiftRight(32);
      while (rem.compareTo(other) >= 0) {
          long a = ((long) rem.data[rem.data.length - 1] << 32) | (rem.data[rem.data.length - 2] & 0xffffffffL);
          long q = Long.divideUnsigned(a, b);
          BigInt minus = BigInt.valueOf(q).multiply(other).shiftLeft(shiftCount).shiftRight(32);
          rem = rem.subtract(minus);
          while (rem.negative) {
              q--;
              rem = rem.add(shifted);
          }
          result = result.add(BigInt.valueOf(q).shiftLeft(shiftCount).shiftRight(32));
          shiftCount -= 32;
          shifted = shifted.shiftRight(32);
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

    public static BigInt factorial(long n) {
        if (n < 0) {
            return BigInt.valueOf(-1);
        }
        BigInt result = BigInt.valueOf(1);
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInt.valueOf(i));
        }
        return result;
    }

    // ways to arrange 'r' items from 'n'
    // where order does matter (eg. arranging letters).
    public static BigInt combinations(int n, int r) {
        // n! / (r!(n-r)!)
        if (r < 0 || r > n) {
            return BigInt.valueOf(0);
        }
        r = Math.min(r, n - r);
        BigInt result = BigInt.valueOf(1);
        for (int i = 1; i <= r; i++) {
            result = result.multiply(BigInt.valueOf(n - i + 1))
                           .divide(BigInt.valueOf(i));
        }
        return result;
    }

    // ways to choose 'r' items from 'n'
    // where order doesn't matter (eg. picking lottery numbers).
    public static BigInt permutations(int n, int r) {
        // n! / (n-r)!
        if (r < 0 || r > n) {
            return BigInt.valueOf(0);
        }
        BigInt result = BigInt.valueOf(1);
        for (int i = 0; i < r; i++) {
            result = result.multiply(BigInt.valueOf(n - i));
        }
        return result;
    }

}
