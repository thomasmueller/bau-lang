package org.bau.perf.benchmarksgame;

import java.math.BigInteger;

public class PiDigits2 {

    public static void main(String... args) {
        for(int i=0; i<4; i++) {
            long start = System.currentTimeMillis();
            new PiDigits2().run();
            System.out.println(System.currentTimeMillis() - start);
        }
    }

    void run() {
        int n = 10_000;
        int k = 0;
        int dummy = 0;
        int i = 0;
        while (i < n) {
            k++;
            nextTerm(k);
            if (num.compareTo(acc) > 0) {
                continue;
            }
            int d = extractDigit(3);
            if (d != extractDigit(4)) {
                continue;
            }
            eliminateDigit(d);
            dummy += d;
//            System.out.print((char) ('0' + d));
            i++;
            if (i % 10 == 0) {
//                System.out.println(" : " + i);
            }
        }
        System.out.println("dummy: " + dummy);
    }

    BigInteger acc = BigInteger.ZERO;
    BigInteger den = BigInteger.ONE;
    BigInteger num = BigInteger.ONE;

    int extractDigit(int nth) {
        return num.multiply(BigInteger.valueOf(nth)).add(acc).divide(den).intValue();
    }

    void eliminateDigit(int d) {
        acc = acc.subtract(den.multiply(BigInteger.valueOf(d)));
        acc = acc.multiply(BigInteger.TEN);
        num = num.multiply(BigInteger.TEN);
    }

    void nextTerm(int k) {
        acc = acc.add(num.shiftLeft(1));
        BigInteger k2p1 = BigInteger.valueOf(k * 2 + 1);
        acc = acc.multiply(k2p1);
        den = den.multiply(k2p1);
        num = num.multiply(BigInteger.valueOf(k));
    }

}
