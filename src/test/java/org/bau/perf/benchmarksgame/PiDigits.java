package org.bau.perf.benchmarksgame;

import java.math.BigInteger;

public class PiDigits {

    public static void main(String... args) {
        new PiDigits().run();
    }

    void run() {
        int n = 10_000;
        int k = 0;
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
            System.out.print((char) ('0' + d));
            i++;
            if (i % 10 == 0) {
                System.out.println(" : " + i);
            }
        }
    }

    BigInteger ten = BigInteger.TEN;
    BigInteger acc = BigInteger.ZERO;
    BigInteger den = BigInteger.ONE;
    BigInteger num = BigInteger.ONE;

    int extractDigit(int nth) {
        return num.multiply(BigInteger.valueOf(nth)).add(acc).divide(den).intValue();
    }

    void eliminateDigit(int d) {
        acc = acc.subtract(den.multiply(BigInteger.valueOf(d)));
        acc = acc.multiply(ten);
        num = num.multiply(ten);
    }

    void nextTerm(int k) {
        acc = acc.add(num.shiftLeft(1));
        BigInteger k2p1 = BigInteger.valueOf(k * 2 + 1);
        acc = acc.multiply(k2p1);
        den = den.multiply(k2p1);
        num = num.multiply(BigInteger.valueOf(k));
    }

}
