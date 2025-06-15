package org.bau.perf.benchmarksgame;

import org.bau.stdlib.math.bigint.BigInt;

public class PiDigitsBigInt {

    public static void main(String... args) {
        for (int i = 0; i < 4; i++) {
            long start = System.currentTimeMillis();
            new PiDigitsBigInt().run();
            System.out.println(System.currentTimeMillis() - start);
        }
    }

    void run() {
        int n = 10_000;
        int k = 0;
        int sum = 0;
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
            sum += d;
//            System.out.print((char) ('0' + d));
            i++;
            if (i % 10 == 0) {
//                System.out.println(" : " + i);
            }
        }
        System.out.println("sum: " + sum);
    }

    BigInt ten = BigInt.valueOf(10);
    BigInt acc = BigInt.valueOf(0);
    BigInt den = BigInt.valueOf(1);
    BigInt num = BigInt.valueOf(1);

    int extractDigit(int nth) {
        return num.multiply(BigInt.valueOf(nth)).add(acc).divide(den).intValue();
    }

    void eliminateDigit(int d) {
        acc = acc.subtract(den.multiply(BigInt.valueOf(d)));
        acc = acc.multiply(ten);
        num = num.multiply(ten);
    }

    void nextTerm(int k) {
        acc = acc.add(num.shiftLeft(1));
        BigInt k2p1 = BigInt.valueOf(k * 2 + 1);
        acc = acc.multiply(k2p1);
        den = den.multiply(k2p1);
        num = num.multiply(BigInt.valueOf(k));
    }

}
