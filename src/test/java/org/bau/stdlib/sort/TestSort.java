package org.bau.stdlib.sort;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Tests the sorting implementations.
 */
public class TestSort {

    public static void main(String... args) {
        TestSort test = new TestSort();
        test.log = true;
        test.size = 1_000_000;
        test.test();
    }

    private boolean log = false;
    private int size = 10_000;

    private boolean stable;

    /**
     * The number of times the compare method was called.
     */
    private AtomicLong compareCount = new AtomicLong();

    /**
     * The comparison object used in this test.
     */
    Comparator<Long> comp;

    private Long[] array;
    private Class<?> clazz;

    private void log(String message) {
        if (log) {
            System.out.println(message);
        }
    }

    @Test
    public void test() {
        array = new Long[size];
        for (int i = 0; i < 2; i++) {
            log("");
            test(Arrays.class, true);
            test(MergeSort.class, true);
            test(IntroSort.class, false);
            test(ShellSort.class, false);
            test(HeapSort.class, false);
        }
    }

    private void test(Class<?> c, boolean stable) {
        this.clazz = c;
        this.stable = stable;
        ordered(array);
        shuffle(array);
        test("random");
        ordered(array);
        test("ordered");
        ordered(array);
        reverse(array);
        test("reverse");
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
    private void  test(String type) {
        if (stable) {
            stabilize(array);
        }
        comp = (o1, o2) -> {
            compareCount.incrementAndGet();
            if (stable) {
                return Long.compare(o1 >> 32, o2 >> 32);
            } else {
                return Long.compare(o1, o2);
            }
        };
        compareCount.set(0);

        long t = System.nanoTime();

        try {
            clazz.getMethod("sort", Object[].class, Comparator.class).invoke(null,
                    array, comp);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }

        if (log) {
            System.out.printf("%4d ms; %10d comparisons algorithm: %s stable: %s data: %s\n",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t), compareCount.get(), clazz, stable, type);
        }

        verify(array);

    }

    private void verify(Long[] array) {
        long last = Long.MIN_VALUE;
        int len = array.length;
        for (int i = 0; i < len; i++) {
            long x = array[i];
            long x1 = x >> 32, x2 = x - (x1 << 32);
            long last1 = last >> 32, last2 = last - (last1 << 32);
            if (x1 < last1) {
                if (array.length < 1000) {
                    log(Arrays.toString(array));
                }
                throw new RuntimeException("" + x);
            } else if (x1 == last1 && x2 < last2) {
                if (array.length < 1000) {
                    log(Arrays.toString(array));
                }
                throw new RuntimeException("" + x);
            }
            last = x;
        }
    }

    private static void ordered(Long[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = (long) i;
        }
    }

    private static void stretch(Long[] array) {
        for (int i = array.length - 1; i >= 0; i--) {
            array[i] = array[i / 4];
        }
    }

    private static void reverse(Long[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            long temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }

    private static void shuffle(Long[] array) {
        Random r = new Random(1);
        for (int i = 0; i < array.length; i++) {
            long temp = array[i];
            int j = r.nextInt(array.length);
            array[j] = array[i];
            array[i] = temp;
        }
    }

    private static void stabilize(Long[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = (array[i] << 32) + i;
        }
    }

}
