package org.bau.stdlib.math;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;

import org.junit.Test;

public class PrimeSieveTest {

    @Test
    public void test() {
        int largeN = 1_000;
        long sum = 0;
        PrimeSieve sieve = PrimeSieve.generate((int) Math.sqrt(largeN));
        for (int i = 0; i < largeN; i++) {
            if (sieve.isPrime(i)) {
                sum += i;
            }
        }
        assertEquals(sumPrimesTo(largeN), sum);
    }

    private static long sumPrimesTo(int largeN) {
        long sum = 0;
        for (int i = 0; i < largeN; i++) {
            if (BigInteger.valueOf(i).isProbablePrime(15)) {
                sum += i;
            }
        }
        return sum;
    }

    // @Test
    public void testLarger() {
        int largeN = 1_000_000_000;
        PrimeSieve sieve = PrimeSieve.generate((int) Math.sqrt(largeN));
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 1; i < largeN; i++) {
            if (i % 100_000_000 == 0) {
                long time = (System.nanoTime() - start) / i;
                System.out.println(i + " " + time + " ns/op sum=" + sum);
            }
            // boolean expected = sieve.isPrime(i);
            boolean got = PrimeSieve.isPrime32(i);
//            assertEquals(expected, got);
            if (got) {
                sum += i;
            }
//            assertEquals(expected, got);

            // 100000000 84 ns/op sum=279209790387276
            // 200000000 113 ns/op sum=1075207199997334
//            if (expected) {
//                sum += i;
//            }
        }
    }
}
