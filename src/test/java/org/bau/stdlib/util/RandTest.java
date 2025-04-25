package org.bau.stdlib.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RandTest {

    @Test
    public void uniform() {
        Rand r = new Rand(1);
        int[] groups = new int[16];
        for (int i = 0; i < 1_000_000; i++) {
            groups[(int) (r.nextLong() >>> (60))]++;
        }
        for(int n : groups) {
            assertTrue(n >= 61000);
        }
        groups = new int[16];
        long max = (long) (Long.MAX_VALUE * 0.75);
        for (int i = 0; i < 1_000_000; i++) {
            long result = r.nextLong(max);
            groups[(int) ((double) groups.length * result / max)]++;
        }
        for(int n : groups) {
            assertTrue(n >= 61000);
        }
        groups = new int[16];
        for (int i = 0; i < 1_000_000; i++) {
            double d = r.nextDouble();
            assertTrue(d >= 0 && d < 1.0);
            groups[(int) (d * groups.length)]++;
        }
        for(int n : groups) {
            assertTrue(n >= 61000);
        }
        long x = -1 & ((1L << 53) - 1);
        double d = (double) x / (1L << 53);
        assertTrue(d < 1.0);
    }
}
