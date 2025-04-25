package org.bau.stdlib.sort.number;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Tests the sorting implementations.
 */
public class NumberTestSort {

    private static boolean LOG = false;
    private static int SIZE = 100_000;

    private final long[] array = new long[SIZE];
    private Class<?> clazz;

    private static void log(String message) {
        if (LOG) {
            System.out.println(message);
        }
    }

    @Test
    public void test() throws Exception {
        test(Arrays.class);
        test(CountingSort.class);
        test(NumberIntroSort.class);
        test(NumberShellSort.class);
    }

    private void test(Class<?> c) throws Exception {
        this.clazz = c;
        random(array);
        test("random");
        ordered(array);
        shuffle(array);
        test("random small");
        ordered(array);
        test("ordered small");
        ordered(array);
        reverse(array);
        test("reverse small");
        ordered(array);
        stretch(array);
        shuffle(array);
        test("few random");
        ordered(array);
        stretch(array);
        test("few ordered");
        ordered(array);
        reverse(array);
        stretch(array);
        test("few reverse");
    }

    /**
     * Sort the array and verify the result.
     *
     * @param type the type of data
     */
    private void  test(String type) throws Exception {
        long t = System.nanoTime();
        clazz.getMethod("sort", long[].class).invoke(null,
                array);

        if (LOG) {
            System.out.printf("%4d ms %s data: %s\n",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t), clazz, type);
        }

        verify(array);

    }

    private static void verify(long[] array) {
        long last = Long.MIN_VALUE;
        int len = array.length;
        for (int i = 0; i < len; i++) {
            long x = array[i];
            if (x < last) {
                if (array.length < 1000) {
                    log(Arrays.toString(array));
                }
                throw new RuntimeException("" + x);
            }
            last = x;
        }
    }

    private static void random(long[] array) {
        Random r = new Random(1);
        for (int i = 0; i < array.length; i++) {
            array[i] = r.nextLong();
        }
        array[r.nextInt(array.length)] = Long.MIN_VALUE;
        array[r.nextInt(array.length)] = Long.MAX_VALUE;
    }

    private static void ordered(long[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = (long) i;
        }
    }

    private static void stretch(long[] array) {
        for (int i = array.length - 1; i >= 0; i--) {
            array[i] = array[i / 4];
        }
    }

    private static void reverse(long[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            long temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }

    private static void shuffle(long[] array) {
        Random r = new Random(1);
        for (int i = 0; i < array.length; i++) {
            long temp = array[i];
            int j = r.nextInt(array.length);
            array[j] = array[i];
            array[i] = temp;
        }
    }

}
