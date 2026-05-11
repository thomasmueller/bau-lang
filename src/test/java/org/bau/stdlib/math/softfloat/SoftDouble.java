package org.bau.stdlib.math.softfloat;

import org.bau.stdlib.math.bigint.Int128;

public class SoftDouble {

    private static final long INFINITY = 0x7ff0000000000000L;
    private static final long NAN = 0x7ff8000000000000L;

    public static long toRaw(long value) {
        if (value == 0) {
            return 0;
        }
        long sign = value & (1L << 63);
        int exponent = 1075;
        if (sign != 0) {
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

    public static long subtract(long a, long b) {
        return add(a, negate(b));
    }

    public static long add(long a, long b) {
        if (isNaN(a) || isNaN(b)) {
            return NAN;
        } else if (isInfinity(a) || isZero(b)) {
            if (isInfinity(b) && sign(a) != sign(b)) {
                return NAN;
            }
            return a;
        } else if (isInfinity(b) || isZero(a)) {
            return b;
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
            return NAN;
        } else if (isInfinity(a) || isInfinity(b)) {
            if (isZero(a) || isZero(b)) {
                return NAN;
            }
            return (sign(a) ^ sign(b)) | INFINITY;
        } else if (isZero(a) || isZero(b)) {
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
        if (isNaN(a) || isNaN(b) || (isZero(a) && isZero(b)) || (isInfinity(a) && isInfinity(b))) {
            return NAN;
        } else if (isInfinity(a)) {
            return (sign(a) ^ sign(b)) | INFINITY;
        } else if (isInfinity(b)) {
            return 0;
        } else if (isZero(b)) {
            return sign(a) | INFINITY;
        } else if (isZero(a)) {
            return a;
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
        if (mantissa == 0) {
            return 0;
        }
        int shift = 11 - Long.numberOfLeadingZeros(mantissa);
        if (shift > 0) {
            mantissa >>>= shift;
        } else if (shift < 0) {
            mantissa <<= -shift;
        }
        exponent += shift;
        if (exponent >= 0x7ffL) {
            exponent = 0x7ffL;
            mantissa = 0;
        } else if (exponent <= 0) {
            exponent = 0;
            mantissa = 0;
        }
        return sign | (exponent << 52) | (mantissa & 0xfffffffffffffL);
    }

    public static long negate(long raw) {
        return raw ^ (1L << 63);
    }

    public static boolean isNaN(long raw) {
        return exponent(raw) == 0x7ffL && mantissa(raw) != 1L << 52;
    }

    public static boolean isInfinity(long raw) {
        return exponent(raw) == 0x7ffL && mantissa(raw) == 1L << 52;
    }

    public static boolean isZero(long raw) {
        return (raw & 0x7fffffffffffffffL) == 0;
    }

    public static long compare(long a, long b) {
        long r = (b >>> 63) - (a >>> 63);
        if (r != 0) {
            return r;
        }
        long ax = (a < 0) ? ~a : a ^ 0x8000000000000000L;
        long bx = (b < 0) ? ~b : b ^ 0x8000000000000000L;
        return Long.compare(ax, bx);
    }

    public static long sign(long raw) {
        return raw & (1L << 63);
    }

    private static long exponent(long raw) {
        return (raw >>> 52) & 0x7ffL;
    }

    private static long mantissa(long raw) {
        return (1L << 52) | (raw & 0xfffffffffffffL);
    }

}
