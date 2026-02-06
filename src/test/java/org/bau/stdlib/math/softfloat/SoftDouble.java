package org.bau.stdlib.math.softfloat;

import org.bau.stdlib.math.bigint.Int128;

public class SoftDouble {

    public static long toRaw(long value) {
        if (value == 0) {
            return 0;
        }
        int sign = (value < 0) ? 1 : 0;
        int exponent = 1075;
        if (sign == 1) {
            if (value == Long.MIN_VALUE) {
                value = 1;
                exponent += 63;
            } else {
                value = -value;
            }
        } else if (value == Long.MAX_VALUE) {
            value = 1;
            exponent += 63;
        }
        return normalize(sign, exponent, value);
    }

    public static long longValue(long raw) {
        long exp = exponent(raw) - 1023;
        long sign = sign(raw);
        long mantissa = mantissa(raw);
        if (exp >= 52) {
            if (exp > 62) {
                if (isNaN(raw)) {
                    return 0;
                } else if (sign == 0) {
                    return Long.MAX_VALUE;
                }
                return Long.MIN_VALUE;
            }
            mantissa <<= (exp - 52);
        } else if (exp < 0) {
            return 0;
        } else {
            mantissa >>>= (52 - exp);
        }
        return (sign == 0) ? mantissa : -mantissa;
    }

    public static long add(long a, long b) {
        if (isNaN(a) || isNaN(b)) {
            return a | b;
        }
        long shift = exponent(a) - exponent(b);
        long mantissaA = mantissa(a);
        long mantissaB = mantissa(b);
        long exponent;
        if (shift >= 0) {
            if (shift > 63) {
                return a;
            }
            exponent = exponent(a);
            mantissaB >>>= shift;
        } else {
            if (-shift > 63) {
                return b;
            }
            exponent = exponent(b);
            mantissaA >>>= -shift;
        }
        long mantissa;
        long sign = sign(a);
        if (sign == sign(b)) {
            mantissa = mantissaA + mantissaB;
        } else {
            mantissa = mantissaA - mantissaB;
            if (mantissa < 0) {
                mantissa = -mantissa;
                sign = sign(b);
            }
        }
        return normalize(sign, exponent, mantissa);
    }

    public static long multiply(long a, long b) {
        if (isNaN(a) || isNaN(b)) {
            return a | b;
        }
        if (a == 0 || b == 0) {
            return 0;
        }
        long exponent = exponent(a) + exponent(b) - 1023;
        long mantissaA = mantissa(a);
        long mantissaB = mantissa(b);
        Int128 ma = new Int128(mantissaA, 0);
        Int128 mb = new Int128(mantissaB, 0);
        Int128 m = ma.multiply(mb).shiftRight(42);
        long mm = m.longValue() >>> 10;
        return normalize(sign(a) ^ sign(b), exponent, mm);
    }

    public static long divide(long a, long b) {
        if (isNaN(a) || isNaN(b)) {
            return a | b;
        }
        long exponent = exponent(a) - exponent(b) + 1023;
        long mantissaA = mantissa(a) << 0;
        long mantissaB = mantissa(b) << 2;
        Int128 ma = new Int128(0, mantissaA);
        Int128 mb = new Int128(mantissaB, 0);
        Int128 m = ma.divide(mb);
        long mm = m.longValue() >>> 10;
        return normalize(sign(a) ^ sign(b), exponent, mm);
    }

    private static long normalize(long sign, long exponent, long mantissa) {
        while ((mantissa >= (1L << 53))) {
            exponent++;
            mantissa >>>= 1;
        }
        while (mantissa != 0 && (mantissa & (1L << 52)) == 0) {
            exponent--;
            mantissa <<= 1;
        }
        if (exponent >= 0x7ffL) {
            exponent = 0x7ffL;
            mantissa = 0;
        } else if (exponent <= 0) {
            exponent = 0;
            mantissa = 0;
        }
        return (sign << 63) | (exponent << 52) | (mantissa & 0xfffffffffffffL);
    }

    public static long negate(int raw) {
        return raw ^ (1L << 63);
    }

    public static boolean isNaN(long raw) {
        return exponent(raw) == 0x7ffL && mantissa(raw) != 0;
    }

    public static boolean isInfinity(long raw) {
        return exponent(raw) == 0x7ffL && mantissa(raw) == 0;
    }

    public static long compare(long a, long b) {
        long r = sign(b) - sign(a);
        if (r != 0) {
            return r;
        }
        r = (a >>> 1) - (b >>> 1);
        if (sign(a) == 1) {
            r = -r;
        }
        return r;
    }

    public static long sign(long raw) {
        return raw >>> 63;
    }

    private static long exponent(long raw) {
        return (raw >>> 52) & 0x7ffL;
    }

    private static long mantissa(long raw) {
        return (1L << 52) | (raw & 0xfffffffffffffL);
    }

}
