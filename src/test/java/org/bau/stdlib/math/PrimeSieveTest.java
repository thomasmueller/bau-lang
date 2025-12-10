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
        int count = 0;
        for (int i = 0; i < largeN; i++) {
            if (BigInteger.valueOf(i).isProbablePrime(15)) {
                sum += i;
                count++;
            }
        }
        System.out.println("count " + count + " sum " + sum + " to " + largeN);
        return sum;
    }
}
