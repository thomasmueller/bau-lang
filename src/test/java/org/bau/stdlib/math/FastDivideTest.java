package org.bau.stdlib.math;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class FastDivideTest {

    public static void main(String... args) {
        for(int test = 0; test < 4; test++) {
            testPerformance(true);
            testPerformance(false);
        }
    }

    static void testPerformance(boolean std) {
        long count = 100_000L;
        long start = System.nanoTime();
        long sum = 0;
        FastDivide[] dividers = new FastDivide[100];
        for (int i = 1; i < dividers.length; i++) {
            dividers[i] = FastDivide.build(i);
        }
        Random r = new Random(1);
        if (std) {
            for (long i = 0; i < count; i++) {
                int divisor = 1 + r.nextInt(dividers.length - 1);
                for (int j = 0; j < 1000; j++) {
                    long x = r.nextLong(Integer.MAX_VALUE / 2);
                    sum += x / divisor;
                }
            }
        } else {
            for (long i = 0; i < count; i++) {
                int divisor = 1 + r.nextInt(dividers.length - 1);
                FastDivide d = dividers[divisor];
                for (int j = 0; j < 1000; j++) {
                    long x = r.nextLong(Integer.MAX_VALUE / 2);
                    sum += d.divide(x);
                }
            }
        }
        long time = System.nanoTime() - start;
        System.out.println(time / count + " ns/entry dummy " + sum + " std=" + std);
    }


    @Test
    public void test() {
        long last = System.currentTimeMillis();
        for (int divisor = 1; divisor < Integer.MAX_VALUE / 2; divisor += 10301) {
            FastDivide div = FastDivide.build(divisor);
            long now = System.currentTimeMillis();
            if (now - last > 2000) {
                System.out.println("divisor " + divisor);
                last = now;
            }
            for (int x = 0, i = 0; x >= 0 && i < 1000; x += divisor, i++) {
                assertEquals(x / divisor, div.divide(x));
                if (x > 0) {
                    if ((x - 1) / divisor != div.divide(x - 1)) {
                        System.out.println((x - 1) + "/" + divisor + ": expected " + ((x - 1) / divisor) + " got "
                                + div.divide(x - 1));
                    }
                    assertEquals((x - 1) / divisor, div.divide(x - 1));
                }
                if ((x + 1) / divisor != div.divide(x + 1)) {
                    System.out.println((x + 1) + "/" + divisor + ": expected " + ((x + 1) / divisor) + " got "
                            + div.divide(x + 1));
                }
                assertEquals((x + 1) / divisor, div.divide(x + 1));
            }
        }
    }
}
