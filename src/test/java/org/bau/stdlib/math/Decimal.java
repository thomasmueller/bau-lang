package org.bau.stdlib.math;

import java.nio.charset.StandardCharsets;

import org.bau.stdlib.math.bigint.Int128;

public class Decimal {

    private final int sign;
    private final int exponent;
    private final long mantissa;

    private Decimal(int sign, int exponent, long mantissa) {
        this.sign = sign;
        this.exponent = exponent;
        this.mantissa = mantissa;
    }

    public static Decimal parse(String s) {
        int sign = 1;
        int i = 0;
        char[] buff = s.toCharArray();
        if (buff.length > 0 && buff[0] == '-') {
            sign = -1;
            i++;
        }
        long x = 0;
        int digit = 0;
        long exp = 0;
        for (; i < buff.length; i++) {
            char c = buff[i];
            if (c == '.') {
                i++;
                break;
            } else if (c >= '0' && c <= '9') {
                digit = 1;
                if (x >= 100_000_000_000_000_000L) {
                    exp++;
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
                } else if (buff[i] == '+') {
                    i++;
                }
                if (i >= buff.length) {
                    throw new NumberFormatException(s);
                }
                long e = 0;
                for (; i < buff.length; i++) {
                    c = buff[i];
                    if (c >= '0' && c <= '9') {
                        e = e * 10 + (c - '0');
                    } else {
                        throw new NumberFormatException(s);
                    }
                }
                exp += expSign * e;
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
        exp -= fractionDigits;
        return normalize(sign, exp, x);
    }

    private static Decimal normalize(int s, long e, long m) {
        if (m == 0) {
            return new Decimal(0, 0, 0);
        }
        if (m < 0) {
            s = -s;
            m = -m;
        }
        while (m < 100_000_000_000_000_000L) {
            m *= 10;
            e--;
        }
        while (m >= 1_000_000_000_000_000_000L) {
            m /= 10;
            e++;
        }
        if (e <= -68) {
            return new Decimal(0, 0, 0);
        } else if(e >= 33) {
            return new Decimal(s, 33, 100_000_000_000_000_000L);
        }
        return new Decimal(s, (int) e, m);
    }

    public String toString() {
        byte[] buff = new byte[26];
        int pos = 0;
        if (sign == 0) {
            buff[pos++] = '0';
            return new String(buff, 0 , pos, StandardCharsets.UTF_8);
        }
        if (sign < 0) {
            buff[pos++] = '-';
        }
        int dot = 0;
        int exp = exponent + 17;
        if (exp < 0 && exp > -7) {
            buff[pos++] = (byte) '0';
            buff[pos++] = (byte) '.';
            while (++exp < 0) {
                buff[pos++] = (byte) '0';
            }
            dot = -1;
        } else if (exp >= 0 && exp < 18) {
            dot = exp;
            exp = 0;
        }
        long m = mantissa;
        for (int i = 0; i < 18; i++) {
            long x = m / 100_000_000_000_000_000L;
            m -= x * 100_000_000_000_000_000L;
            buff[pos++] = (byte) ('0' + x);
            if (m == 0 && i > dot + 1) {
                break;
            }
            if (i == dot) {
                buff[pos++] = '.';
            }
            m *= 10;
        }
        while (buff[pos - 1] == '0') {
            pos--;
        }
        if (buff[pos - 1] == '.') {
            pos--;
        }
        if (exp != 0) {
            buff[pos++] = 'E';
            if (exp < 0) {
                buff[pos++] = '-';
                exp = -exp;
            }
            if (exp >= 10) {
                buff[pos++] = (byte) ('0' + ((exp / 10) % 10));
            }
            buff[pos++] = (byte) ('0' + (exp % 10));
        }
        return new String(buff, 0 , pos, StandardCharsets.UTF_8);
    }

    public Decimal subtract(Decimal other) {
        return add(other.negate());
    }

    public Decimal negate() {
        return new Decimal(-sign, exponent, mantissa);
    }

    public Decimal abs() {
        if (sign >= 0) {
            return this;
        }
        return normalize(-sign, exponent, mantissa);
    }

    public int signum() {
        return sign;
    }

    public int compareTo(Decimal other) {
        int comp = Integer.compare(sign, other.sign);
        if (comp != 0) {
            return comp;
        }
        comp = Integer.compare(exponent, other.exponent);
        if (comp != 0) {
            return comp;
        }
        return sign * Long.compare(mantissa, other.mantissa);
    }

    public Decimal add(Decimal other) {
        if (sign == 0) {
            return other;
        }
        if (other.sign == 0) {
            return this;
        }
        long m1 = sign * mantissa;
        long m2 = other.sign * other.mantissa;
        int de = exponent - other.exponent;
        int exp = exponent;
        if (de > 0) {
            if (de > 16) {
                return this;
            }
            while(de > 0) {
                m2 /= 10;
                de--;
            }
        } else if (de < 0) {
            if (-de > 16) {
                return other;
            }
            while (de < 0) {
                m1 /= 10;
                de++;
            }
            exp = other.exponent;
        }
        return normalize(1, exp, m1 + m2);
    }

    public Decimal multiply(Decimal other) {
        if (other.mantissa == 0 || mantissa == 0) {
            return new Decimal(0, 0, 0);
        }
        int exp = exponent + other.exponent + 18;
        long mantissaA = mantissa;
        long mantissaB = other.mantissa;
        Int128 ma = new Int128((int) mantissaA, (int) (mantissaA >>> 32), 0, 0);
        Int128 mb = new Int128((int) mantissaB, (int) (mantissaB >>> 32), 0, 0);
        Int128 m = ma.multiply(mb).divide(Int128.valueOf(1_000_000_000_000_000_000L));
        return normalize(sign * other.sign, exp, m.longValue());
    }

    public Decimal divide(Decimal other) {
        if (other.mantissa == 0 || mantissa == 0) {
            return new Decimal(0, 0, 0);
        }
        int exp = exponent - other.exponent - 17;
        long mantissaA = mantissa;
        long mantissaB = other.mantissa;
        Int128 ma = new Int128((int) mantissaA, (int) (mantissaA >>> 32), 0, 0);
        ma = ma.multiply(Int128.valueOf(100_000_000_000_000_000L));
        Int128 mb = new Int128((int) mantissaB, (int) (mantissaB >>> 32), 0, 0);
        Int128 m = ma.divide(mb);
        return normalize(sign * other.sign, exp, m.longValue());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Decimal)) {
            return false;
        }
        Decimal o = (Decimal) other;
        return sign == o.sign && exponent == o.exponent && mantissa == o.mantissa;
    }

}
