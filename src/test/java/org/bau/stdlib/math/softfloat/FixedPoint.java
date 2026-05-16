package org.bau.stdlib.math.softfloat;

import java.nio.charset.StandardCharsets;

public class FixedPoint {

    public static final int ZERO = 0;
    public static final int ONE = 0x10000;
    public static final int MAX = 0x7fffffff;
    public static final int MIN = -0x7fffffff;
    public static final int NAN = 0x80000000;
    public static final int PI = 0x3243f;
    public static final int TAU = PI * 2;
    public static final int E = 0x2b7e1;
    public static final int LN2 = 0xb172;

    public static int parse(String s) {
        int sign = 1;
        long x = 0;
        int pos = 0;
        if (s.length() > 0) {
            if (s.charAt(0) == '-') {
                sign = -1;
                pos++;
            }
            int dot = -1;
            while (pos < s.length()) {
                int c = s.charAt(pos);
                if (c >= '0' && c <= '9') {
                    x = x * 10 + ((c - '0'));
                    if (dot != -1) {
                        dot++;
                    }
                    if (x > (1L << 32)) {
                        break;
                    }
                } else if (c == '.') {
                    if (dot >= 0) {
                        return NAN;
                    }
                    dot = 0;
                } else {
                    return NAN;
                }
                pos++;
            }
            x <<= 16;
            while (dot > 0) {
                x /= 10;
                dot--;
            }
            if (x > MAX) {
                x = MAX;
            }
        }
        return (int) (x * sign);
    }

    public static String toString(int x) {
        if (x == NAN) {
            return "NaN";
        }
        int len = 0;
        if (x < 0) {
            len = 1;
            x = -x;
        }
        int n = x >> 16;
        int rem = x & 0xffff;
        do {
            len += 1;
            n /= 10;
        } while (n != 0);
        int pos = len;
        if (rem != 0) {
            len += 7;
        }
        byte[] buff = new byte[len];
        buff[0] = '-';
        n = x >> 16;
        do {
            pos--;
            buff[pos] = (byte) ('0' + (n % 10));
            n /= 10;
        } while (n != 0);
        if (rem != 0) {
            pos = len - 7;
            buff[pos++] = '.';
            for (int i = 0; i < 6; i++) {
                rem = (rem & 0xffff) * 10;
                buff[pos++] = (byte) ('0' + (rem >> 16));
            }
        }
        return new String(buff, StandardCharsets.UTF_8);
    }

    public static int add(int a, int b) {
        return a + b;
    }

    public static int subtract(int a, int b) {
        return a - b;
    }

    public static int multiply(int a, int b) {
        return (int) (((long) a * (long) b) >> 16);
    }

    // divide by zero returns MAX, MIN, or zero
    public static int divide(int a, int b) {
        if (b == 0) {
            return a == 0 ? 0 : a > 0 ? MAX : MIN;
        }
        return (int) ((((long) a) << 16) / b);
    }

    public static int addSaturated(int a, int b) {
        int sum = a + b;
        int overflow = ((a ^ sum) & (b ^ sum)) >> 31;
        if (overflow != 0) {
            return (a >> 31) ^ MAX;
        }
        return sum;
    }

    public static int subtractSaturated(int a, int b) {
        int diff = a - b;
        int overflow = ((a ^ diff) & (b ^ diff)) >> 31;
        if (overflow != 0) {
            return (a >> 31) ^ MAX;
        }
        return diff;
    }

    public static int multiplySaturated(int a, int b) {
        long prod = ((long) a * (long) b) >> 16;
        if (prod > MAX) {
            return MAX;
        } else if (prod < -MAX) {
            return -MAX;
        }
        return (int) prod;
    }

    public static int sqrt(int x) {
        if (x <= 0) {
            return 0;
        }
        long n = ((long) x) << 16;
        long res = 0;
        long bit = 1L << 62;
        while (bit > n) {
            bit >>= 2;
        }
        while (bit != 0) {
            if (n >= res + bit) {
                n -= res + bit;
                res = (res >> 1) + bit;
            } else {
                res >>= 1;
            }

            bit >>= 2;
        }
        return (int) res;
    }

    public static int sin(int x) {
        x %= TAU;
        if (x > PI) {
            x -= TAU;
        } else if (x < -PI) {
            x += TAU;
        }
        if (x > PI / 2) {
            x = PI - x;
        } else if (x < -PI / 2) {
            x = -PI - x;
        }
        int x2 = multiply(x, x);
        return x
            - multiply(multiply(x2, x),  ONE / 6)
            + multiply(multiply(multiply(x2, x2), x), ONE / 120);
    }

    public static int cos(int x) {
        return sin(x + PI / 2);
    }

    public static int tan(int x) {
        int b = cos(x);
        if (b == 0) {
            return MAX;
        }
        // slightly compensated for missing division rounding
        return divide(sin(x) + 11, b);
    }

    public static int exp(int x) {
        int k = x / LN2;
        int r = x - k * LN2;
        int r2 = multiply(r, r);
        int r3 = multiply(r2, r);
        int r4 = multiply(r3, r);
        long y = ONE + r + r2 / 2 + r3 / 6 + r4 / 24;
        if (k >= 0) {
            y <<= k;
        } else {
            y >>= -k;
        }
        if (y > MAX) {
            return MAX;
        }
        return (int) y;
    }

    public static int log(int x) {
        if (x <= 0) {
            return -MAX;
        }
        int k = 0;
        while (x >= (2 << 16)) {
            x >>= 1;
            k++;
        }
        while (x < ONE) {
            x <<= 1;
            k--;
        }
        int y = divide(x - ONE, x + ONE);
        int y2 = multiply(y, y);
        int y3 = multiply(y2, y);
        int y5 = multiply(y3, y2);
        int y7 = multiply(y5, y2);
        int y9 = multiply(y7, y2);
        int ln = y + y3 / 3 + y5 / 5 + y7 / 7 + y9 / 9;
        return ln * 2 + k * LN2;
    }

    public static int pow(int a, int b) {
        return exp(multiply(log(a), b));
    }

}
