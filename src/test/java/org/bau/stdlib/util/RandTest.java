package org.bau.stdlib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;
import java.util.SplittableRandom;

import org.junit.Test;

public class RandTest {

    public static void main(String... args) {
        for (int test = 0; test < 10; test++) {
            SplittableRandom r = new SplittableRandom(test);
            long sum = 0;
            int count = 100_000_000;
            long start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                sum += r.nextLong();
            }
            long time = System.nanoTime() - start;
            System.out.println(((double) time / count) + " ns/entry sum=" + sum);
            // Rand
            // 0.793035 ns/entry sum=8360924715103292710
            // 1.36748125 ns/entry sum=4356514589549280216

            // Rand2
            // 1.09636209 ns/entry sum=-6322780007203548443
            // 3.4024175 ns/entry sum=-6824676324516211852

            // SplittableRandom
            // 0.81550625 ns/entry sum=8360924715103292710
            // 1.24596 ns/entry sum=4356514589549280216

            // Random
            // 16.40278375 ns/entry sum=8773587155106358783
        }
    }

    @Test
    public void minMax() {
        Rand r = new Rand(1);
        assertEquals(0, r.nextLong(1));
        assertEquals(0, r.nextLong(0));
        assertEquals(0, r.nextLong(-1));
        for (int smallerThan = 1; smallerThan < 16; smallerThan++) {
            for (int i = 0; i < 1000; i++) {
                long result = r.nextLong(smallerThan);
                assertTrue(result >= 0);
                assertTrue(result < smallerThan);
            }
        }
    }

    @Test
    public void uniform() {
        Rand r = new Rand(1);
        int[] groups = new int[16];
        for (int i = 0; i < 1_000_000; i++) {
            groups[(int) (r.nextLong() >>> (60))]++;
        }
        for (int n : groups) {
            assertTrue(n >= 61000);
        }
        groups = new int[16];
        long max = (long) (Long.MAX_VALUE * 0.75);
        for (int i = 0; i < 1_000_000; i++) {
            long result = r.nextLong(max);
            assertTrue(result >= 0);
            assertTrue(result < max);
            groups[(int) ((double) groups.length * result / max)]++;
        }
        for (int n : groups) {
            assertTrue(n >= 61000);
        }
        groups = new int[16];
        for (int i = 0; i < 1_000_000; i++) {
            double d = r.nextDouble();
            assertTrue(d >= 0 && d < 1.0);
            groups[(int) (d * groups.length)]++;
        }
        for (int n : groups) {
            assertTrue(n >= 61000);
        }
        long x = -1 & ((1L << 53) - 1);
        double d = (double) x / (1L << 53);
        assertTrue(d < 1.0);
    }

    @Test
    public void shuffle() {
        Random r = new Random(1);
        Integer[] array = new Integer[10];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        Rand.shuffle(array, r);
        assertEquals("[9, 5, 7, 2, 8, 4, 3, 1, 0, 6]", Arrays.toString(array));
    }
}
