package org.bau.perf.benchmarksgame;

import org.bau.stdlib.math.bigint.BigInt;

// https://benchmarksgame-team.pages.debian.net/benchmarksgame/program/pidigits-java-1.html
public class PiDigitsBigInt {
    static final int L = 10;

    public static void main(String args[]) {
        // int n = Integer.parseInt(args[0]);
        int n = 1_000;
        int j = 0;
        long start = System.currentTimeMillis();

        PiDigitSpigot digits = new PiDigitSpigot();
        int dummy = 0;

        while (n > 0) {
            if (n >= L) {
                for (int i = 0; i < L; i++) {
                    int digit = digits.next();
                    dummy += digit;
                    print(digit);
                }
                j += L;
            } else {
                for (int i = 0; i < n; i++) {
                    int digit = digits.next();
                    dummy += digit;
                    print(digits.next());
                }
                for (int i = n; i < L; i++) {
                     System.out.print(" ");
                }
                j += n;
            }
            println("\t:" + j);
            n -= L;
        }
        long time = System.currentTimeMillis() - start;
        System.out.println("time: " + time + " ms dummy:" + dummy);
    }

    private static void print(int digit) {
        // System.out.print(digit);
    }
    private static void println(String s) {
        // System.out.println(s);
    }

    static class PiDigitSpigot {
        Transformation z, x, inverse;

        public PiDigitSpigot() {
            z = new Transformation(1, 0, 0, 1);
            x = new Transformation(0, 0, 0, 0);
            inverse = new Transformation(0, 0, 0, 0);
        }

        public int next() {
            int y = digit();
            if (isSafe(y)) {
                z = produce(y);
                return y;
            } else {
                z = consume(x.next());
                return next();
            }
        }

        public int digit() {
            return z.extract(3);
        }

        public boolean isSafe(int digit) {
            return digit == z.extract(4);
        }

        public Transformation produce(int i) {
            return (inverse.qrst(10, -10 * i, 0, 1)).compose(z);
        }

        public Transformation consume(Transformation a) {
            return z.compose(a);
        }
    }

    static class Transformation {
        BigInt q, r, s, t;
        int k;

        public Transformation(int q, int r, int s, int t) {
            this.q = BigInt.valueOf(q);
            this.r = BigInt.valueOf(r);
            this.s = BigInt.valueOf(s);
            this.t = BigInt.valueOf(t);
            k = 0;
        }

        public Transformation(BigInt q, BigInt r, BigInt s, BigInt t) {
            this.q = q;
            this.r = r;
            this.s = s;
            this.t = t;
            k = 0;
        }

        public Transformation next() {
            k++;
            q = BigInt.valueOf(k);
            r = BigInt.valueOf(4 * k + 2);
            s = BigInt.valueOf(0);
            t = BigInt.valueOf(2 * k + 1);
            return this;
        }

        public int extract(int j) {
            BigInt bigj = BigInt.valueOf(j);
            BigInt numerator = (q.multiply(bigj)).add(r);
            BigInt denominator = (s.multiply(bigj)).add(t);
            return (numerator.divide(denominator)).intValue();
        }

        public Transformation qrst(int q, int r, int s, int t) {
            this.q = BigInt.valueOf(q);
            this.r = BigInt.valueOf(r);
            this.s = BigInt.valueOf(s);
            this.t = BigInt.valueOf(t);
            k = 0;
            return this;
        }

        public Transformation compose(Transformation a) {
            return new Transformation(q.multiply(a.q), (q.multiply(a.r)).add((r.multiply(a.t))),
                    (s.multiply(a.q)).add((t.multiply(a.s))), (s.multiply(a.r)).add((t.multiply(a.t))));
        }
    }

}
