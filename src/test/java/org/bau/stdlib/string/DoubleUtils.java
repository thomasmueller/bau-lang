package org.bau.stdlib.string;

import java.nio.charset.StandardCharsets;

import org.bau.stdlib.math.IntMath;

public class DoubleUtils {

    final static long[] FACT = new long[342];

    static {
        long a = 0, b = 1L << 62;
        int shifted = -62;
        for (int e10 = 0; e10 < FACT.length; e10++) {
            int e2 = exp2(e10);
            if (e10 > 0) {
                a = a * 10 + IntMath.multiplyHighUnsigned(b, 10);
                b *= 10;
            }
            int s = e2 - 62 - shifted;
            FACT[e10] = (a << (64 - s)) | (b >>> s);
            if (Long.numberOfLeadingZeros(a) < 32) {
                shifted += 32;
                b = (a << 32) | (b >>> 32);
                a = a >>> 32;
            }
        }
    }

    private static int exp2(int e10) {
        int r = 1 + (int)((e10 * 3483314 - 1385418) >> 20);
        return r & ~(r >> 31);
    }

    private static int exp10(int e2) {
        return (((e2 - 1) * 315651) + 732700) >> 20;
    }

    public static double parseDouble(String s) {
        if ("NaN".equals(s)) {
            return Double.NaN;
        } else if ("Infinity".equals(s)) {
            return Double.POSITIVE_INFINITY;
        } else if ("-Infinity".equals(s)) {
            return Double.NEGATIVE_INFINITY;
        }
        long signBit = 0;
        int i = 0;
        char[] buff = s.toCharArray();
        if (buff.length > 0 && buff[0] == '-') {
            signBit = 1L << 63;
            i++;
        }
        long x = 0;
        int digit = 0;
        int e10 = 0;
        for (; i < buff.length; i++) {
            char c = buff[i];
            if (c == '.') {
                i++;
                break;
            } else if (c >= '0' && c <= '9') {
                digit = 1;
                if (x >= 100_000_000_000_000_000L) {
                    e10++;
                    if (e10 > 1000) {
                        e10 = 1000;
                    }
                } else {
                    x = x * 10 + (c - '0');
                }
            } else {
                break;
            }
        }
        int fractionDigits = 0;
        for (; i < buff.length; i++) {
            char c = buff[i];
            if (c == 'E' || c == 'e') {
                i++;
                if (i >= buff.length) {
                    throw new NumberFormatException(s);
                }
                int expSign = 1;
                if (buff[i] == '-') {
                    expSign = -1;
                    i++;
                }
                if (i >= buff.length) {
                    throw new NumberFormatException(s);
                }
                int e = 0;
                for (; i < buff.length; i++) {
                    c = buff[i];
                    if (c >= '0' && c <= '9') {
                        e = e * 10 + (c - '0');
                        if (e > 1000) {
                            e = 1000;
                        }
                    } else {
                        throw new NumberFormatException(s);
                    }
                }
                e10 += expSign * e;
                break;
            } else if (c >= '0' && c <= '9') {
                digit = 1;
                if (x < 100_000_000_000_000_000L) {
                    fractionDigits++;
                    x = x * 10 + (c - '0');
                }
            } else {
                throw new NumberFormatException(s);
            }
        }
        if (digit == 0) {
            throw new NumberFormatException(s);
        }
        e10 -= fractionDigits;
        while (x != 0 && Long.numberOfLeadingZeros(x) > 5) {
            x *= 10;
            e10--;
        }
        long e2;
        long mantissa;
        if (x == 0) {
            mantissa = 0;
            e2 = 0;
        } else if (e10 > FACT.length) {
            if (signBit != 0) {
                return Double.NEGATIVE_INFINITY;
            }
            return Double.POSITIVE_INFINITY;
        } else if (-e10 > FACT.length) {
            if (signBit != 0) {
                return -0.0;
            }
            return 0.0;
        } else if (e10 >= 0) {
            e2 = exp2(e10);
            long fact = FACT[e10];
            mantissa = IntMath.multiplyHighUnsigned(fact, x << 2);
            e2 += 1024 + 51;
        } else {
            e2 = -exp2(-e10);
            long fact = FACT[-e10];
            mantissa = IntMath.divide128Unsigned(x, 0, fact);
            e2 += 1024 + 49;
        }
        int z = Long.numberOfLeadingZeros(mantissa);
        if (z < 9) {
            if (z < 10) {
                mantissa >>>= 10 - z;
            }
            if ((mantissa & 1) == 1) {
                mantissa++;
            }
            e2 += 11 - z;
            mantissa >>>= 1;
        }
        if (mantissa != 0 && z > 12) {
            e2 += 12 - z;
            mantissa >>>= 12 - z;
        }
        if (e2 >= 0x7ffL) {
            e2 = 0x7ffL;
            mantissa = 0;
        } else if (e2 <= 0) {
            mantissa >>>= 1;
            if (e2 < 0) {
                mantissa >>>=  -e2 - 1;
                if ((mantissa & 1) == 1) {
                    mantissa++;
                }
                mantissa >>>= 1;
                e2 = 0;
            }
        }
        long y = signBit | (e2 << 52) | (mantissa & 0xfffffffffffffL);
        return Double.longBitsToDouble(y);
    }

    public static String convertDoubleToString(double n) {
        byte[] buff = new byte[25];
        int pos = appendDouble(buff, 0, n);
        return new String(buff, 0 , pos, StandardCharsets.UTF_8);
    }

    public static int copyString(byte[] buff, String s) {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(data, 0, buff, 0, data.length);
        return data.length;
    }

    public static int appendDouble(byte[] buff, int pos, double n) {
        if (Double.isNaN(n)) {
            return copyString(buff, "NaN");
        } else if (n == Double.POSITIVE_INFINITY) {
            return copyString(buff, "Infinity");
        } else if (n == Double.NEGATIVE_INFINITY) {
            return copyString(buff, "-Infinity");
        }
        long raw = Double.doubleToLongBits(n);
        long sign = raw >>> 63;
        long mantissa = (1L << 52) | (raw & 0xfffffffffffffL);
        int e2 = (int) ((raw >>> 52) & 0x7ffL) - 1023;
        if (e2 == -1023) {
            // subnormal
            mantissa &= 0xfffffffffffffL;
            e2++;
            if (mantissa == 0) {
                e2 = 0;
            }
        }
        if (sign != 0) {
            buff[pos] = '-';
            pos++;
        }
        mantissa <<= 6;
        int shift = 58;
        int e10 = 0;
        if (e2 > 0) {
            e10 = exp10(e2 - 1);
            long fact = FACT[e10];
            if (e2 != exp2(e10)) {
                fact >>>= e2 - 1 - exp2(e10);
            }
            mantissa = IntMath.divide128Unsigned(mantissa, 0, fact);
            shift += 1;
        } else if (e2 < 0) {
            e10 = -exp10(1 - e2);
            long fact = FACT[-e10];
            if (e2 != exp2(-e10)) {
                fact >>>= 1 - e2 - exp2(-e10);
            }
            mantissa = IntMath.multiplyHighUnsigned(mantissa << 3, fact);
        }
        long x = mantissa >>> shift;
        if (x > 9) {
            mantissa /= 10;
            e10++;
        } else if (x == 0) {
            mantissa *= 10;
            e10--;
        }
        int dot = 0;
        if (e10 < 0 && e10 > -4) {
            buff[pos++] = (byte) '0';
            buff[pos++] = (byte) '.';
            while (++e10 < 0) {
                buff[pos++] = (byte) '0';
            }
            dot = -1;
        } else if (e10 >= 0 && e10 < 7) {
            dot = e10;
            e10 = 0;
        }
        for (int i = 0; i < 18; i++) {
            x = mantissa >>> shift;
            mantissa -= x << shift;
            buff[pos++] = (byte) ('0' + x);
            if (i == dot) {
                buff[pos++] = '.';
            }
            if (mantissa == 0 && i > dot + 1) {
                break;
            }
            mantissa *= 5;
            shift -= 1;
        }
        while (buff[pos - 1] == '0' && buff[pos - 2] != '.') {
            pos--;
        }
        if (e10 != 0) {
            buff[pos++] = (byte) 'E';
            if (e10 < 0) {
                buff[pos++] = '-';
                e10 = -e10;
            }
            if (e10 >= 10) {
                if (e10 >= 100) {
                    buff[pos++] = (byte) ('0' + (e10 / 100));
                }
                buff[pos++] = (byte) ('0' + ((e10 / 10) % 10));
            }
            buff[pos++] = (byte) ('0' + (e10 % 10));
        }
        return pos;
    }

}
