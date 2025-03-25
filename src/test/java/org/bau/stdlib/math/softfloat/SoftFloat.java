package org.bau.stdlib.math.softfloat;

public class SoftFloat {

    public static int toRaw(int value) {
        if (value == 0) {
            return 0;
        }
        int sign = (value < 0) ? 1 : 0;
        int exponent = 150;
        if (sign == 1) {
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

    public static int add(int a, int b) {
        if (isNaN(a) || isNaN(b)) {
            return a | b;
        }
        int shift = exponent(a) - exponent(b);
        int mantissaA = mantissa(a);
        int mantissaB = mantissa(b);
        int exponent = 0;
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
        int mantissa = 0;
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
            return a | b;
        }
        if (a == 0 || b == 0) {
            return 0;
        }
        int exponent = exponent(a) + exponent(b) - 150;
        int mantissaA = mantissa(a) | (1 << 23);
        int mantissaB = mantissa(b) | (1 << 23);
        long mantissa = (long) mantissaA * (long) mantissaB;
        return normalize(sign(a) ^ sign(b), exponent, mantissa);
    }

    public static int divide(int a, int b) {
        if (isNaN(a) || isNaN(b)) {
            return a | b;
        }
        int exponent = exponent(a) - exponent(b) + 118;
        int mantissaA = mantissa(a);
        int mantissaB = mantissa(b);
        long mantissa = ((long) mantissaA << 32) / (long) mantissaB;
        return normalize(sign(a) ^ sign(b), exponent, mantissa);
    }

    private static int normalize(int sign, int exponent, long mantissa) {
        while ((mantissa >= (0x1 << 24))) {
            exponent++;
            mantissa >>>= 1;
        }
        while (mantissa != 0 && (mantissa & (0x1 << 23)) == 0) {
            exponent--;
            mantissa <<= 1;
        }
        if (exponent >= 0xff) {
            exponent = 0xff;
            mantissa = 0;
        } else if (exponent <= 0) {
            exponent = 0;
            mantissa = 0;
        }
        return (sign << 31) | (exponent << 23) | (int) (mantissa & 0x7fffff);
    }

    public static int negate(int raw) {
        return raw ^ (1 << 31);
    }

    public static boolean isNaN(int raw) {
        return exponent(raw) == 0xff && mantissa(raw) != 0;
    }

    public static boolean isInfinity(int raw) {
        return exponent(raw) == 0xff && mantissa(raw) == 0;
    }

    public static int compare(int a, int b) {
        int r = sign(b) - sign(a);
        if (r != 0) {
            return r;
        }
        r = (a >>> 1) - (b >>> 1);
        if (sign(a) == 1) {
            r = -r;
        }
        return r;
    }

    public static int sign(int raw) {
        return raw >>> 31;
    }

    private static int exponent(int raw) {
        return (raw >>> 23) & 0xff;
    }

    private static int mantissa(int raw) {
        return (1 << 23) | (raw & 0x7fffff);
    }

}
