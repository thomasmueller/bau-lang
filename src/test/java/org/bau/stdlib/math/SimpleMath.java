package org.bau.stdlib.math;

public class SimpleMath {

    public static double E =            2.71828182845904523536;
    public static double PI =           3.14159265358979323846;
    public static double LOG10 =        2.30258509299404568402;
    public static double LOG2 =         0.69314718055994530942;
    public static double NOT_A_NUMBER = 0.0 / 0.0;
    public static double POS_INFINITY = 1.0 / 0.0;
    public static double NEG_INFINITY = -1.0 / 0.0;
    public static long MIN_LONG =       0x8000000000000000L;
    public static long MAX_LONG =       0x7fffffffffffffffL;

    public static double abs(double x) {
        return (x >= 0) ? x : -x;
    }

    public static boolean isNotANumber(double x) {
        return x != x;
    }

    public static boolean isNegativeZero(double x) {
        return (x == 0) && (1 / x == NEG_INFINITY);
    }

    public static double min(double a, double b) {
        if (isNotANumber(a)) {
            return a;
        } else if (a == b && a == 0 && isNegativeZero(b)) {
            return b;
        }
        return a <= b ? a : b;
    }

    public static double max(double a, double b) {
        if (isNotANumber(a)) {
            return a;
        } else if (a == b && a == 0 && isNegativeZero(a)) {
            return b;
        }
        return a >= b ? a : b;
    }

    public static double floor(double x) {
        if (x <= Long.MIN_VALUE || x >= Long.MAX_VALUE || isNotANumber(x) || x == 0) {
            return x;
        }
        long i = (long) x;
        double d = (double) i;
        if (d == x || x >= 0) {
            return d;
        }
        return d - 1;
    }

    public static double ceil(double x) {
        return -floor(-x);
    }

    public static long round(double x) {
        if (isNotANumber(x)) {
            return 0;
        } else if (x <= Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        } else if (x >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) floor(x + 0.5);
    }

    public static double signum(double x) {
        if (x == 0.0 || isNotANumber(x)) {
            return x;
        }
        return x >= 0 ? 1 : -1;
    }

    public static double exp(double x) {
        if (isNotANumber(x) || x == POS_INFINITY) {
            return x;
        } else if (x < 0) {
            return 1 / exp(-x);
        } else if (x > 2) {
            double r = exp(x / 2);
            return r * r;
        }
        double approx = 1.0;
        double term = 1.0;
        for (int i = 1; i < 22; i++) {
            term *= x / i;
            approx += term;
        }
        return approx;
    }

    public static double log(double x) {
        if (x == 0) {
            return NEG_INFINITY;
        } else if (x == POS_INFINITY) {
            return x;
        } else if (x <= 0 || isNotANumber(x)) {
            return NOT_A_NUMBER;
        } else if (x < 0.7) {
            return log(2 * x) - LOG2;
        } else if (x >= 1.5) {
            return log(x / 2) + LOG2;
        }
        double base = x - 1;
        int sign = 1;
        double term = base;
        double approx = term;
        for (int i = 2; i < 30; i++) {
            sign = -sign;
            term *= base;
            approx += sign * term / i;
        }
        return approx;
    }

    public static double sqrt(double x) {
        long x2 = (long) x;
        if (x2 > 0 && x2 == x) {
            long exact = sqrtLong(x2);
            if (exact * exact == x) {
                return exact;
            }
        }
        return exp(log(x) / 2);
    }

    public static long sqrtLong(long x) {
        if (x < 0) {
            return 0;
        }
        long g = 1L << 31;
        long c = g;
        while (true) {
            long t = g * g;
            if (t > x || t < 0) {
                g ^= c;
            }
            c >>= 1;
            if (c == 0) {
                break;
            }
            g |= c;
        }
        return g;
    }

    public static int sqrtInt(int x) {
        if (x < 0) {
            return 0;
        }
        int g = 1 << 15;
        int c = g;
        while (true) {
            int t = g * g;
            if (t > x || t < 0) {
                g ^= c;
            }
            c >>= 1;
            if (c == 0) {
                break;
            }
            g |= c;
        }
        return g;
    }

    public static double log10(double x) {
        return log(x) / LOG10;
    }

    public static double pow(double x, double y) {
        int x2 = (int) x;
        int y2 = (int) y;
        if (x2 == x && y2 == y) {
            long r = powInt(x2, y2);
            if (r != -1) {
                return r;
            }
        }
        if (x > 0 && y > 0) {
            return exp(y * log(x));
        } else if (y == 0.0) {
            return 1.0;
        } else if (isNotANumber(x) || isNotANumber(y)) {
            return NOT_A_NUMBER;
        }
        double absX = abs(x);
        if (y == NEG_INFINITY) {
            if (absX > 1.0) {
                return 0.0;
            } else if (absX == 1.0) {
                return NOT_A_NUMBER;
            }
            return POS_INFINITY;
        }
        long yy = (long) y;
        boolean odd = yy == y && (yy & 1) == 1;
        if (isNegativeZero(x) && yy == y) {
            if (odd) {
                return -pow(-x, y);
            }
            return pow(-x, y);
        } else if (x >= 0) {
            return exp(y * log(x));
        } else if (odd) {
            return -pow(-x, y);
        } else if (x == NEG_INFINITY) {
            if (y < 0) {
                return 0.0;
            }
            return POS_INFINITY;
        } else if (yy == y) {
            return pow(-x, y);
        } else if (abs(y) > 1e19) {
            if (y > 0) {
                return pow(-x, y);
            } else if (x == -1) {
                return 1;
            } else if (absX > 1) {
                return 0;
            }
            return POS_INFINITY;
        }
        return NOT_A_NUMBER;
    }

    public static long powInt(int base, int exponent) {
        if (exponent < 0) {
            return -1;
        }
        long result = 1;
        long b = base;
        while (exponent > 0) {
            if ((exponent & 1) == 1) {
                result *= b;
                if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
                    return -1;
                }
            }
            exponent >>= 1;
            if (exponent > 0) {
                b *= b;
                if (b > Integer.MAX_VALUE || b < Integer.MIN_VALUE) {
                    return -1;
                }
            }
        }
        return result;
    }

    public static double sin(double x) {
        if (x == POS_INFINITY) {
            return NOT_A_NUMBER;
        } else if (abs(x) < 1e-8) {
            return x;
        } else if (x < 0) {
            return -sin(-x);
        }
        if (x > PI * 2) {
            x -= floor(x / (PI * 2)) * (PI * 2);
        }
        if (x > PI) {
            return -sin(x - PI);
        } else if (x > PI / 2) {
            x = PI - x;
        }
        double approx = 0.0;
        int sign = 1;
        double p = x;
        for (int n = 2; n < 24; n += 2) {
            approx += sign * p;
            p *= x * x / (n * (n + 1));
            sign = -sign;
        }
        return approx;
    }

    public static double atan(double x) {
        x = min(max(-1e16, x), 1e16);
        if (abs(x) >= 0.4) {
            return 2 * atan(x / (1 + sqrt(1 + x * x)));
        }
        double approx = x;
        int sign = -1;
        double p = x;
        for (int n = 3; n < 32; n += 2) {
            p *= x * x;
            approx += sign * p / n;
            sign = -sign;
        }
        return approx;
    }

    public static double cos(double x) {
        return sin(x + PI / 2);
    }

    public static double tan(double x) {
        return sin(x) / cos(x);
    }

    public static double asin(double x) {
        return atan(x / sqrt(1 - x * x));
    }

    public static double acos(double x) {
        return PI / 2 - asin(x);
    }

    public static double convertLongBitsToDouble(long x) {
        long sign = x >>> 63;
        long exp = (x >>> 52) & 0x7ff;
        if (x == 0) {
            return 0.0;
        } else if (x == 1L << 63) {
            return -0.0;
        }
        long fraction = x << 12 >>> 12;
        if (exp == 0x7ff) {
            if (fraction != 0) {
                return NOT_A_NUMBER;
            } else if (sign == 0) {
                return POS_INFINITY;
            } else {
                return NEG_INFINITY;
            }
        }
        double result = 0;
        for (int i = 0; i < 52; i++) {
            if ((fraction & 1) == 1) {
                result += 1;
            }
            result /= 2;
            fraction >>>= 1;
        }
        if (exp == 0) {
            result *= 2;
        } else {
            result += 1;
        }
        exp -= 1023;
        while (exp <= -32) {
            result /= 1L << 32;
            exp += 32;
        }
        while(exp < 0) {
            result /= 2;
            exp++;
        }
        while (exp >= 32) {
            result = result * (1L << 32);
            exp -= 32;
        }
        while (exp > 0) {
            result += result;
            exp--;
        }
        if (sign == 0) {
            return result;
        }
        return -result;
    }

    public static long convertDoubleToLongBits(double x) {
        if (x == 0) {
            if (isNegativeZero(x)) {
                return 1L << 63;
            }
            return 0;
        } else if (x < 0) {
            return convertDoubleToLongBits(-x) | (1L << 63);
        } else if (x == POS_INFINITY) {
            return 0x7ffL << 52;
        } else if (isNotANumber(x)) {
            return (0x7ffL << 52) + (1L << 51);
        }
        long exp = 0;
        while (x >= (1L << 32)) {
            exp += 32;
            x /= 1L << 32;
        }
        while (x >= 2) {
            exp++;
            x /= 2;
        }
        while (x < (1.0 / (1L << 32)) && exp > -1022 + 32) {
            exp -= 32;
            x *= 1L << 32;
        }
        while (x < 1 && exp > -1022) {
            exp--;
            x += x;
        }
        long fraction = (long) (x * (1L << 52)) << 11 >>> 11;
        return ((exp + 1022) << 52) + fraction;
    }

}
