package org.bau.stdlib.math.softfloat;

public class SimpleSoftFloat {

    public static int toRaw(int value) {
        return normalize(150, value);
    }

    public static int intValue(int raw) {
        int exp = exponent(raw) - 127;
        int sign = raw >> 31;
        int mantissa = (1 << 23) | (raw & 0x7fffff);
        if (exp >= 23) {
            if (exp > 30) {
                return sign == 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }
            mantissa <<= exp - 23;
        } else if (exp < 0) {
            return 0;
        } else {
            mantissa >>= 23 - exp;
        }
        return sign == 0 ? mantissa : -mantissa;
    }

    public static int subtract(int a, int b) {
        return add(a, negate(b));
    }

    public static int add(int a, int b) {
        int exponent = exponent(a);
        int shift = exponent - exponent(b);
        if (shift < 0) {
            return add(b, a);
        }
        return normalize(exponent, signedMantissa(a) +
                ((signedMantissa(b) >> (shift & 31)) & ((shift - 32) >> 31)));
    }

    public static int multiply(int a, int b) {
        return normalize(
                exponent(a) + exponent(b) - 150,
                (long) signedMantissa(a) * (long) signedMantissa(b));
    }

    public static int divide(int a, int b) {
        long ma = signedMantissa(a);
        long mb = signedMantissa(b);
        if (mb == 0) {
            return ma == 0 ? 0 : (int) (ma & 0x80000000) | 0x7f7fffff;
        }
        return normalize(exponent(a) - exponent(b) + 118,
                (ma << 32) / mb);
    }

    public static int negate(int raw) {
        return raw == 0 ? 0 : raw ^ (1 << 31);
    }

    public static int compare(int a, int b) {
        int ax = a ^ ((a >> 31) >>> 1);
        int bx = b ^ ((b >> 31) >>> 1);
        return Integer.compare(ax, bx);
    }

    private static int normalize(int exponent, long signedMantissa) {
        if (signedMantissa == 0) {
            return 0;
        }
        int sign = 0;
        long mantissa = signedMantissa;
        if (signedMantissa < 0) {
            sign = 1 << 31;
            mantissa = -signedMantissa;
        }
        int shift = 40 - Long.numberOfLeadingZeros(mantissa);
        if (shift > 0) {
            mantissa >>>= shift;
        } else if (shift < 0) {
            mantissa <<= -shift;
        }
        exponent += shift;
        if (exponent > 0xfe) {
            exponent = 0xfe;
            mantissa = -1;
        }
        return sign | (exponent << 23) | (int) (mantissa & 0x7fffff);
    }

    private static int exponent(int raw) {
        return (raw >>> 23) & 0xff;
    }

    private static int signedMantissa(int raw) {
        if (raw << 1 == 0) {
            return 0;
        }
        int mantissa = (1 << 23) | raw & 0x7fffff;
        return raw >> 31 == 0 ? mantissa : -mantissa;
    }

}
