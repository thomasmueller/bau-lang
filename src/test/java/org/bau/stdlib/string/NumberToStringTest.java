package org.bau.stdlib.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

public class NumberToStringTest {

    @Test
    public void doubleTest() {
        assertEquals(Double.toString(Double.NEGATIVE_INFINITY), NumberToString.convertDoubleToString(Double.NEGATIVE_INFINITY));
        assertEquals(Double.toString(Double.POSITIVE_INFINITY), NumberToString.convertDoubleToString(Double.POSITIVE_INFINITY));
        assertEquals(Double.toString(0.0 / 0.0), NumberToString.convertDoubleToString(0.0 / 0.0));
        for (double n = 1.0; n < Double.POSITIVE_INFINITY; n *= 1000.0) {
            String expected = Double.toString(n);
            double n3 = Double.parseDouble(expected);
            if (n3 != n) {
                fail("fail " + n + " got " + n3 + " diff " + (n3 - n));
            }
            String got = NumberToString.convertDoubleToString(n);
            double n2 = Double.parseDouble(got);
            double diff = diff(n, n2);
            if (diff > 1e-15 && n2 != n) {
                fail("expected " + expected + " got " + got + " diff " + diff + " diff " + (n2 - n));
            }
        }
        Random r = new Random(1);
        for (int i = 0; i < 1_000; i++) {
            for (long max = 1; max > 0; max *= 10) {
                double n = r.nextGaussian() * max;
                String expected = Double.toString(n);
                double n3 = Double.parseDouble(expected);
                if (n3 != n) {
                    fail();
                }
                String got = NumberToString.convertDoubleToString(n);
                double n2 = Double.parseDouble(got);
                double diff = diff(n, n2);
                if (diff > 2e-15 && n2 != n) {
                    fail("expected " + expected + " got " + got + " diff " + diff + " diff " + (n2 - n));
                }
            }
        }
        for (int i = 0; i < 1_000; i++) {
            double n = Double.longBitsToDouble(r.nextLong());
            if (Double.isNaN(n)) {
                continue;
            }
            String expected = Double.toString(n);
            double n3 = Double.parseDouble(expected);
            if (n3 != n) {
                fail();
            }
            String got = NumberToString.convertDoubleToString(n);
            double n2 = Double.parseDouble(got);
            double diff = diff(n, n2);
            if (diff > 2e-15 && n2 != n) {
                fail("expected " + expected + " got " + got + " diff " + diff + " diff " + (n2 - n));
            }
        }
    }

    private static double diff(double expected, double got) {
        double diff = expected - got;
        if (expected == 0) {
            return 1;
        }
        return Math.abs(1.0 * diff / expected);
    }

    @Test
    public void longTest() {
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            for (long max = 10; max > 0; max *= 10) {
                long x = r.nextLong(max);
                String expected = Long.toString(x);
                String got = NumberToString.convertLongToString(x);
                assertEquals(expected, got);
            }
        }
        String expected = Long.toString(Long.MIN_VALUE);
        String got = NumberToString.convertLongToString(Long.MIN_VALUE);
        assertEquals(expected, got);
        expected = Long.toString(Long.MAX_VALUE);
        got = NumberToString.convertLongToString(Long.MAX_VALUE);
        assertEquals(expected, got);
    }

    @Test
    public void performanceLong() {
        // 76 ns/key dummy=1411828872 modulo & reverse
        // 49 ns/key dummy=1411828872 Long.toString()
        Random r = new Random(1);
        int dummy = 0;
        int count = 0;
        long time = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            for (long max = 10; max > 0; max *= 10) {
                long x = r.nextLong(max);
                // String s = Long.toString(x);
                String s = NumberToString.convertLongToString(x);
                dummy += s.hashCode();
                count++;
            }
        }
        time = System.nanoTime() - time;
        System.out.println((time / count) + " ns/key dummy=" + dummy);
    }

    @Test
    public void performanceDouble() {
        // 243 ns/key dummy=-1587182100 doubleToString
        // 191 ns/key dummy=1800059164 Double.toString
        Random r = new Random(1);
        int dummy = 0;
        int count = 0;
        long time = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            for (long max = 10; max > 0; max *= 10) {
                double x = r.nextGaussian() * max;
                // String s = Double.toString(x);
                String s = NumberToString.convertDoubleToString(x);
                dummy += s.hashCode();
                count++;
            }
        }
        time = System.nanoTime() - time;
        System.out.println((time / count) + " ns/key dummy=" + dummy);
    }

}
