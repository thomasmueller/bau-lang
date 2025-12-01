package org.bau.stdlib.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class SimpleMathTest {

    static final double[] TEST = new double[350];

    static final double[] SPECIAL_VALUES = new double[] {
            0, 0.1, 0.5, 0.7, 1.0, 1.5, Math.PI / 2,
            2, 3, Math.PI, Math.PI * 2, 1e20, Double.POSITIVE_INFINITY
    };

    static {
        int i = 0;
        for(double s : SPECIAL_VALUES) {
            TEST[i++] = s;
            TEST[i++] = -s;
        }
        TEST[i++] = Double.NaN;
        Random r = new Random(1);
        for (int j = 0; j < 5; j++) {
            TEST[i++] = r.nextGaussian();
        }
        for (int j = 0; j < 5; j++) {
            TEST[i++] = r.nextDouble(1) - 0.5;
            TEST[i++] = r.nextDouble(10) - 5;
            TEST[i++] = r.nextDouble(100) - 50;
        }
        for (double x = 1; x < Double.POSITIVE_INFINITY; x *= 32 * 1024) {
            TEST[i++] = x;
            TEST[i++] = -x;
        }
        for (double x = 1; x > 0; x /= 32 * 1024) {
            TEST[i++] = x;
            TEST[i++] = -x;
        }
        while (i < TEST.length) {
            TEST[i++] = r.nextDouble();
        }
    }

    @Test
    public void sqrtTest() {
        for (double x : TEST) {
            double expected = Math.sqrt(x);
            double got = SimpleMath.sqrt(x);
            assertTrue(same(expected, got, x));
        }
        for (long x = 0; x < Integer.MAX_VALUE; x += 101) {
            long x2 = x * x;
            if ((long) ((double) x2) != x2) {
                break;
            }
            long got = (long) SimpleMath.sqrt(x2);
            assertEquals(x, got);
        }
    }

    @Test
    public void sqrtIntTest() {
        for (long x = 0; x <= Integer.MAX_VALUE; x += 1000) {
            int expected = (int) Math.sqrt(x);
            int got = SimpleMath.sqrtInt((int) x);
            if (expected != got) {
                got = SimpleMath.sqrtInt((int) x);
                assertEquals("sqrt " + x, expected, got);
            }
        }
        Random r = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            int x = r.nextInt();
            int expected = (int) Math.sqrt(x);
            int got = SimpleMath.sqrtInt((int) x);
            if (expected != got) {
                got = SimpleMath.sqrtInt((int) x);
                assertEquals("sqrt " + x, expected, got);
            }
        }
    }

    @Test
    public void sqrtLongTest() {
        for (long x = 1;; x += 10_000) {
            long s = x * x;
            if (s < 0) {
                break;
            }
            long got = SimpleMath.sqrtLong(s);
            assertEquals(x, got);
            got = SimpleMath.sqrtLong(s - 1);
            assertEquals(x - 1, got);
            got = SimpleMath.sqrtLong(s + 1);
            assertEquals(x, got);
        }
        for (long x = 0; x >= 0; x += 10 * Integer.MAX_VALUE) {
            long got = SimpleMath.sqrtLong(x);
            if (got * got < 0 || got * got > x) {
                Assert.fail();
            }
            long t2 = got + 1;
            if (t2 * t2 < 0 || t2 * t2 <= x) {
                assertEquals("sqrt " + x, t2, got);
            }
        }
        Random r = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            long x = r.nextLong();
            long expected = (long) Math.sqrt(x);
            long got = SimpleMath.sqrtLong(x);
            if (expected != got) {
                got = SimpleMath.sqrtLong(x);
                assertEquals("sqrt " + x, expected, got);
            }
        }
    }

    @Test
    public void convertLongBitsTest() {
        for (double x : TEST) {
            long expected = Double.doubleToLongBits(x);
            long got = SimpleMath.convertDoubleToLongBits(x);
            assertEquals(expected, got);
            double y = SimpleMath.convertLongBitsToDouble(got);
            long got2 = Double.doubleToLongBits(y);
            assertEquals(x + " != " + y, expected, got2);
        }
    }

    @Test
    public void sinTest() {
        for (double x : TEST) {
            double expected = Math.sin(x);
            double got = SimpleMath.sin(x);
            if (x < 1e7 && x > -1e7) {
                assertTrue(sameTrig(expected, got, x));
            }
        }
    }

    @Test
    public void cosTest() {
        for (double x : TEST) {
            double expected = Math.cos(x);
            double got = SimpleMath.cos(x);
            if (x < 1e7 && x > -1e7) {
                assertTrue(sameTrig(expected, got, x));
            }
        }
    }

    @Test
    public void tanTest() {
        for (double x : TEST) {
            double expected = Math.tan(x);
            double got = SimpleMath.tan(x);
            if (x < 1e7 && x > -1e7) {
                assertTrue(sameTrig(expected, got, x));
            }
        }
    }

    @Test
    public void logTest() {
        for (double x : TEST) {
            double expected = Math.log(x);
            double got = SimpleMath.log(x);
            assertTrue(same(expected, got, x));
        }
    }

    @Test
    public void expTest() {
        for (double x : TEST) {
            double expected = Math.exp(x);
            double got = SimpleMath.exp(x);
            assertTrue(same(expected, got, x));
        }
    }

    @Test
    public void log10Test() {
        for (double x : TEST) {
            double expected = Math.log10(x);
            double got = SimpleMath.log10(x);
            assertTrue(same(expected, got, x));
        }
    }

    boolean sameTrig(double expected, double got, double forValue) {
        if (same(expected, got, forValue)) {
            return true;
        }
        if (Math.abs(expected) > 1e16 || Math.abs(got) > 1e16) {
            if (Math.abs(expected) > 1e16 && Math.abs(got) > 1e16) {
                return true;
            }
        }
        return false;
    }

    boolean same(double expected, double got, double forValue) {
        if (Double.isNaN(expected) || Double.isNaN(got)) {
            return Double.isNaN(expected) && Double.isNaN(got);
        }
        if (expected == got) {
            return true;
        }
        if (expected == 0 || got == 0) {
            double max = Math.max(Math.abs(expected), Math.abs(got));
            if (max < 4e-14) {
                return true;
            }
        }
        double diff = diff(expected, got);
        if (diff > 0) {
            if (diff < 4e-10) {
                return true;
            }
        }
        return false;
    }

    double diff(double expected, double got) {
        double diff = expected - got;
        if (expected == 0) {
            return 1;
        }
        return Math.abs(1.0 * diff / expected);
    }

    @Test
    public void powTest() {
        for (double x : TEST) {
            for (double y : TEST) {
                double expected = Math.pow(x, y);
                double got = SimpleMath.pow(x, y);
                assertTrue(same(expected, got, x));
            }
        }
    }

    @Test
    public void powIntTest() {
        Random r = new Random(1);
        for (int i = 0; i < 100000; i++) {
            int a = r.nextInt();
            int b = r.nextInt();
            double got = SimpleMath.powInt(a, b);
            if (got == -1) {
                continue;
            }
            double expected = Math.pow(a, b);
            assertTrue(same(expected, got, a));
        }
    }

    @Test
    public void asinTest() {
        for (double x : TEST) {
            double expected = Math.asin(x);
            double got = SimpleMath.asin(x);
            assertTrue(sameTrig(expected, got, x));
        }
    }

    @Test
    public void nanTest() {
        for (double x : TEST) {
            assertTrue(Double.isNaN(x) == SimpleMath.isNotANumber(x));
        }
    }

    @Test
    public void floorCeilSignumRoundTest() {
        for (double x : TEST) {
            assertTrue(exactSame(Math.floor(x), SimpleMath.floor(x)));
            assertTrue(exactSame(Math.ceil(x), SimpleMath.ceil(x)));
            assertTrue(exactSame(Math.signum(x), SimpleMath.signum(x)));
            assertTrue(exactSame(Math.round(x), SimpleMath.round(x)));
        }
    }

    @Test
    public void minMaxText() {
        for (double x : TEST) {
            for (double y : TEST) {
                double min = Math.min(x, y);
                double max = Math.max(x, y);
                assertTrue(exactSame(min, SimpleMath.min(x, y)));
                assertTrue(exactSame(max, SimpleMath.max(x, y)));
            }
        }
    }

    public static boolean exactSame(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.isNaN(x) && Double.isNaN(y);
        }
        return Double.compare(x, y) == 0;
    }

    @Test
    public void atanTest() {
        for (double x : TEST) {
            double expected = Math.atan(x);
            double got = SimpleMath.atan(x);
            assertTrue(sameTrig(expected, got, x));
        }
    }

    @Test
    public void acosTest() {
        for (double x : TEST) {
            double expected = Math.acos(x);
            double got = SimpleMath.acos(x);
            assertTrue(sameTrig(expected, got, x));
        }
    }

}
