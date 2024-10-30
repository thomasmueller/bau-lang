package org.bau.perf;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PrimeNumbers {

    // private final static int FIRST = 1_000_000;
    // private final static int FIRST = 150_000;
    private final static int FIRST = 1_000;

    public boolean isPrime(long n) {
        if ((n & 1) == 0) {
            return false;
        }
        // slightly faster but more complex
        // for (long p = 16, sq = 9, i = 3; sq <= n; i += 2, sq += p, p += 8) {
        for (long i = 3; i * i <= n; i += 2) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void calcPrimesDoubleSize() {
        long sum = 0;
        for (int n = 3; n > 0; n = 2 * n - 1) {
            for (;; n -= 2) {
                if (isPrime(n)) {
                    System.out.println("n=" + n);
                    sum += n;
                    break;
                }
            }
        }
        assertEquals(2611138177L, sum);
    }

    @Test
    public void calcPrimesWithSieve() {
        long start = System.currentTimeMillis();
        int max = 16_000_000;
        int count = 1;
        int last = 0;
        long[] field = new long[(max + 63) / 64];
        for (int i = 3; i < max; i += 2) {
            if ((field[i >>> 6] & (1L << i)) != 0) {
                continue;
            }
            count++;
            last = i;
            if (count >= FIRST) {
                break;
            }
            if ((long) i * i > max) {
                continue;
            }
            for (int j = i * i; j < max; j += i) {
                field[j >>> 6] |= 1L << j;
            }
        }
        long time = System.currentTimeMillis() - start;
        System.out.println("calcPrimesWithSieve last: " + last + " count: " + count + " time: " + time + " ms");
    }

    @Test
    public void calcPrimesWithTest() {
        long start = System.currentTimeMillis();
        long last = 0;
        int count = 1;
        for (int n = 3; count < FIRST; n += 2) {
            if (isPrime(n)) {
                last = n;
                count++;
            }
        }
        long time = System.currentTimeMillis() - start;
        System.out.println("calcPrimesWithTest last: " + last + " count: " + count + " time: " + time + " ms");
    }
}
