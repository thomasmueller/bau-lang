package org.bau.stdlib.string;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.bau.stdlib.math.IntMath;
import org.bau.stdlib.math.bigint.BigInt;
import org.bau.stdlib.math.bigint.Int128;

public class DoubleToString {

    public static String convertLongToString(long n) {
        int len = getLongStringLen(n);
        byte[] buff = new byte[len];
        if (n < 0) {
            buff[0] = '-';
        } else {
            n = -n;
        }
        int i = len;
        do {
            buff[--i] = (byte) ('0' - (n % 10));
            n /= 10;
        } while (n < 0);
        return new String(buff, StandardCharsets.UTF_8);
    }

    private static int getLongStringLen(long n) {
        int len = 0;
        if (n < 0) {
            len++;
        } else {
            n = -n;
        }
        do {
            len++;
            n /= 10;
        } while (n < 0);
        return len;
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

    final static int[] expLen = new int[2048];
    final static BigInt[] FACT = new BigInt[2060];
    final static int[] FACT_DIV = new int[2060];

    final static long[] FACT_LONG = new long[2051];
    final static long[] FACT_LONG2 = new long[2060];

//    static {
//        BigInt ten = BigInt.valueOf(10L);
//        BigInt d2 = BigInt.valueOf(1L);
//        for (int i = 0; i < 1024; i++) {
//            d2 = d2.multiply(ten);
//            int len = d2.len();
//            System.out.println("len=" + len + " d2=" + d2);
//            FACT[len] = d2;
//        }
//
//    }

    public static String exactString(double n) {
        long raw = Double.doubleToLongBits(n);
        long mantissa = mantissa(raw);
        int exp = (int) exponent(raw) - 1023;
        if (exp == -1023) {
            mantissa &= 0xfffffffffffffL;
            exp++;
        }
        return "2^" + (exp - 52) + "*" + mantissa;
    }

    public static String convertDoubleToString(double n) {
        if (n < 0) {
            return "-" + convertDoubleToString(-n);
        } else if (Double.isNaN(n)) {
            return "NaN";
        } else if (n == Double.POSITIVE_INFINITY) {
            return "Infinity";
        }
        long raw = Double.doubleToLongBits(n);
        long mantissa = mantissa(raw);
        int exp = (int) exponent(raw) - 1023;

        if (exp == -1023) {
            mantissa &= 0xfffffffffffffL;
            exp++;
        }

        StringBuilder buff = new StringBuilder();
        int e = 0;
        int OFFSET = 2;

        // a bit of rounding up
        mantissa = (mantissa << OFFSET) + (1L << OFFSET - 2);
        // mantissa <<= OFFSET;

        if (exp >= 50) {
//        	BigInt l2 = BigInt.valueOf(mantissa);
//        	l2 = l2.shiftLeft(exp);
//            BigInt ten = BigInt.valueOf(10L);
//            BigInt d2 = BigInt.valueOf(1L);
//            int divCount = 0;
//            while (d2.len() < exp - OFFSET) {
//            	d2 = d2.multiply(ten);
//            	divCount++;
//            }
//            if (FACT[exp + 1024] == null) {
//                FACT[exp + 1024] = d2;
//                FACT_DIV[exp + 1024] = divCount;
//            }
//            if (!d2.equals(FACT[exp - OFFSET])) {
//                System.out.println(d2);
//                System.out.println(FACT[exp - OFFSET]);
//            }
//        	BigInt r2 = l2.divide(d2);
//            long result2 = r2.longValue();
//            long d2h = d2.shiftRight(exp - 63).longValue();

            long d2h = FACT_LONG2[exp + 1024];

            long result3 = IntMath.divide128Unsigned(mantissa, 0, d2h) >>> 1;
//            FACT_LONG[exp + 1024] = d2h;
//            if (Math.abs(result2 - result3) > 1) {
//                System.out.println("d2.len=" + d2.shiftRight(exp - 63).len());
//                System.out.println("nlz=" + Long.numberOfLeadingZeros(mantissa << 1));
//                System.out.println("result2= " + result2);
//                System.out.println("result3= " + result3);
//
//            }
          long  result2 = result3;
          int divCount = FACT_DIV[exp + 1024];


            e += divCount;
            mantissa = result2;
            exp = 0;
        } else if (exp <= -10) {
            BigInt l2 = BigInt.valueOf(mantissa);
            BigInt ten = BigInt.valueOf(10L);
            BigInt d2 = BigInt.valueOf(1L);
            int mulCount = 0;
            while (d2.len() < -exp) {
                d2 = d2.multiply(ten);
                mulCount++;
            }
            if (FACT[exp + 1024] == null) {
                FACT[exp + 1024] = d2;
                FACT_DIV[exp + 1024] = mulCount;
            }
            BigInt r2 = l2.multiply(d2);
            r2 = r2.shiftRight(-exp);

            long result2 = r2.longValue();

//            BigInt r2test = l2.multiply(d2.shiftRight(-exp - 64 - OFFSET)).shiftRight(64);

            int p = 4;
            long d2h = d2.shiftRight(-exp - 57 - p).longValue();
            FACT_LONG[exp + 1024] = d2h;
            long result3 = IntMath.multiplyHighUnsigned(mantissa << (7 - p), d2h);
//            int n0 = Long.numberOfLeadingZeros(d2h);
//            if (n0 > 0) {
//                result3 = IntMath.multiplyHighUnsigned(mantissa << (7 - p), d2h << n0) >> n0;
//            }
            // result3 = Math.unsignedMultiplyHigh(mantissa, d2h);

            // result3 = r2test.longValue();

            if (Math.abs(result2 - result3) > 1) {
                System.out.println("result2= " + result2);
                System.out.println("result3= " + result3);

                System.out.println("mantissa " + Long.toHexString(mantissa));
                System.out.println("d2h      " + Long.toHexString(d2h));

                System.out.println("d2.len origina=" + d2.len());
                System.out.println("d2.len shifted="+ d2.shiftRight(-exp - 60).len());
                System.out.println("-exp " + -exp);

//                System.out.println("d2.s="+ d2.shiftRight(-exp - 64));
//                System.out.println("len " + d2.shiftRight(-exp - 64).len());
//                System.out.println("d2h ="+ d2h);
//                System.out.println("r2test= " + r2test);
//                System.out.println("mantissa= " + mantissa + " * " + d2h);
                throw new AssertionError();

            }
            result2 = result3;
           // result2 = r2test.longValue();




            e -= mulCount;
            mantissa = result2;
            exp = 0;
        }
        int shift;
        if (exp >= 0) {
            shift = (52 + OFFSET) - exp;
        } else {
            shift = (52 + OFFSET);
            while(exp < 0) {
                shift += 1;
                exp += 1;
            }
        }
        long x = mantissa >>> shift;
        if (e != 0) {
            if (x > 9) {
                mantissa /= 10;
                x /= 10;
                e++;
            } else if (x == 0) {
                mantissa *= 10;
                x = mantissa >>> shift;
                e--;
            }
        }
        mantissa -= x << shift;
        buff.append(convertLongToString(x));
        buff.append('.');
        mantissa *= 5;
        shift -= 1;
        if (mantissa <= 0) {
            buff.append('0');
        }
        for (int i = 0; i <= 18 && mantissa > 0; i++) {
            x = mantissa >>> shift;
            mantissa -= x << shift;
            buff.append((char) ('0' + x));
            mantissa *= 5;
            shift -= 1;
        }
        if (e != 0) {
            buff.append('E');
            if (e < 0) {
                buff.append('-');
                e = -e;
            }
            buff.append(convertLongToString(e));
        }
        return buff.toString();
    }

    static {

        int OFFSET = 2;
        for(int exp=0; exp<=1024; exp++) {
            BigInt ten = BigInt.valueOf(10L);
            BigInt d2 = BigInt.valueOf(1L);
            int divCount = 0;
            while (d2.len() < exp - OFFSET) {
                d2 = d2.multiply(ten);
                divCount++;
            }
            long d2h = d2.shiftRight(exp - 63).longValue();
            FACT_LONG2[2050 - (exp + 1024)] = d2h;
            FACT_LONG2[exp + 1024] = d2h;
            FACT_DIV[exp + 1024] = divCount;

        }
    }

    public static void printFact() {
        for (int i = 0; i < FACT.length; i++) {
            // (1014)   10:   3
            // (2)    1022: 308  3.31818182
            // (1074)   50 : 15
            // (2047)  1023: 308
            int test = 0;
            // ln(10)/ln(2)
            double f = 3.32192809;
            if (i < 1024) {
                int x = 1026 - i;
                test = (int) (x / f);
            } else {
                int x = i - 1024;
                test = (int) (x / f);
            }
            int diff = FACT_DIV[i] - test;
            if (FACT[i] != null && (diff < 0 || diff > 1)) {
                System.out.println(i + " : " + FACT_DIV[i] + " " + diff + " " + FACT[i]);
            }
//            System.out.println(i + " " + FACT_LONG[i]);
            if (i < 1015) {

                if (FACT_LONG[i] != 0) {
                    if (FACT_LONG[i] != FACT_LONG2[i]) {
                        System.out.println("error " + i + " " + FACT_LONG[i] + " " + FACT_LONG2[i]);
                    }
                }


                if (FACT_LONG[i] ==  FACT_LONG[2050 - i]) {

                } else {
                    System.out.println(i + " " + FACT_LONG[i] + " " + FACT_LONG[2050 - i]);
                }
            }
//
//
//            } else if (FACT_LONG[i] != 0) {
//                throw new AssertionError();
//            }
        }

    }

}
