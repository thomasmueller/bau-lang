package org.bau.perf.benchmarksgame;

import org.bau.stdlib.math.bigint.BigInt;

public class PiDigitsBigInt2 {

    public static void main(String... args) {
        for (int i = 0; i < 4; i++) {
            long start = System.currentTimeMillis();
            new PiDigitsBigInt2().run();
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

    BigInt acc = BigInt.valueOf(0);
    BigInt den = BigInt.valueOf(1);
    BigInt num = BigInt.valueOf(1);

    int extractDigit(int nth) {
        return num.multiply(BigInt.valueOf(nth)).add(acc).divide(den).intValue();
    }

    void eliminateDigit(int d) {
        acc = acc.subtract(den.multiply(BigInt.valueOf(d)));
        acc = acc.multiply(BigInt.valueOf(10));
        num = num.multiply(BigInt.valueOf(10));
    }

    void nextTerm(int k) {
        acc = acc.add(num.shiftLeft(1));
        acc = acc.multiply(BigInt.valueOf(k * 2 + 1));
        den = den.multiply(BigInt.valueOf(k * 2 + 1));
        num = num.multiply(BigInt.valueOf(k));
    }

}
