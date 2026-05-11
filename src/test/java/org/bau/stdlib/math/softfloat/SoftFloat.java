package org.bau.stdlib.math.softfloat;

public class SoftFloat {

    private static final int INFINITY = 0x7f800000;
    private static final int NAN = 0x7fc00000;

    public static int toRaw(int value) {
        if (value == 0) {
            return 0;
        }
        int sign = value & (1 << 31);
        int exponent = 150;
        if (sign != 0) {
            if (value == Integer.MIN_VALUE) {
                value = 1;
                exponent += 31;
            } else {
                value = -value;
            }
        } else if (value == Integer.MAX_VALUE) {
            value = 1;
            exponent += 31;
        }
        return normalize(sign, exponent, value);
    }

    public static int intValue(int raw) {
        int exp = exponent(raw) - 127;
        int sign = sign(raw);
        int mantissa = mantissa(raw);
        if (exp >= 23) {
            if (exp > 30) {
                if (isNaN(raw)) {
                    return 0;
                } else if (sign == 0) {
                    return Integer.MAX_VALUE;
                }
                return Integer.MIN_VALUE;
            }
            mantissa <<= (exp - 23);
        } else if (exp < 0) {
            return 0;
        } else {
            mantissa >>>= (23 - exp);
        }
        return (sign == 0) ? mantissa : -mantissa;
    }

    public static int subtract(int a, int b) {
        return add(a, negate(b));
    }

    public static int add(int a, int b) {
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
        int shift = exponent(a) - exponent(b);
        int mantissaA = mantissa(a);
        int mantissaB = mantissa(b);
        int exponent;
        if (shift >= 0) {
            if (shift > 31) {
                return a;
            }
            exponent = exponent(a);
            mantissaB >>>= shift;
        } else {
            if (-shift > 31) {
                return b;
            }
            exponent = exponent(b);
            mantissaA >>>= -shift;
        }
        int mantissa;
        int sign = sign(a);
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

    public static int multiply(int a, int b) {
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
        int exponent = exponent(a) + exponent(b) - 150;
        int mantissaA = mantissa(a);
        int mantissaB = mantissa(b);
        long mantissa = (long) mantissaA * (long) mantissaB;
        return normalize(sign(a) ^ sign(b), exponent, mantissa);
    }

    public static int divide(int a, int b) {
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
        int mantissaA = mantissa(a);
        int mantissaB = mantissa(b);
        int exponent = exponent(a) - exponent(b) + 118;
        long mantissa = ((long) mantissaA << 32) / (long) mantissaB;
        return normalize(sign(a) ^ sign(b), exponent, mantissa);
    }

    private static int normalize(int sign, int exponent, long mantissa) {
        if (mantissa == 0) {
            return 0;
        }
        int shift = 40 - Long.numberOfLeadingZeros(mantissa);
        if (shift > 0) {
            mantissa >>>= shift;
        } else if (shift < 0) {
            mantissa <<= -shift;
        }
        exponent += shift;
        if (exponent >= 0xff) {
            exponent = 0xff;
            mantissa = 0;
        } else if (exponent <= 0) {
            exponent = 0;
            mantissa = 0;
        }
        return sign | (exponent << 23) | (int) (mantissa & 0x7fffff);
    }

    public static int negate(int raw) {
        return raw ^ (1 << 31);
    }

    public static boolean isNaN(int raw) {
        return exponent(raw) == 0xff && mantissa(raw) != 1 << 23;
    }

    public static boolean isInfinity(int raw) {
        return exponent(raw) == 0xff && mantissa(raw) == 1 << 23;
    }

    public static boolean isZero(int raw) {
        return (raw & 0x7fffffff) == 0;
    }

    public static int compare(int a, int b) {
        int r = (b >>> 31) - (a >>> 31);
        if (r != 0) {
            return r;
        }
        int ax = (a < 0) ? ~a : a ^ 0x80000000;
        int bx = (b < 0) ? ~b : b ^ 0x80000000;
        return Integer.compare(ax, bx);
    }

    private static int sign(int raw) {
        return raw & (1 << 31);
    }

    private static int exponent(int raw) {
        return (raw >>> 23) & 0xff;
    }

    private static int mantissa(int raw) {
        return (1 << 23) | (raw & 0x7fffff);
    }

}
