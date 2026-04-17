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

    @Test
    public void testLarger() {
        long largeN = Integer.MAX_VALUE;
        PrimeSieve sieve = PrimeSieve.generate((int) Math.sqrt(largeN));
        // long start = System.nanoTime();
        // long sum = 0;
        for (long i = 1; i <= largeN; i += 1_000_000) {
            if (i % 100_000_000 == 1) {
                // long time = (System.nanoTime() - start) / i;
                // System.out.println(i + " " + time + " ns/op sum=" + sum);
            }
            boolean expected = sieve.isPrime(i);
            boolean got = PrimeSieve.isPrime32(i);
            assertEquals(expected, got);
            assertEquals(expected, got);
            // if (expected) {
            //     sum += i;
            // }
        }
        System.out.println("done");
    }
}
