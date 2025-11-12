package org.bau.stdlib.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.bau.perf.Profiler;
import org.junit.Test;

public class DoubleUtilsTest {

    public static void main(String... args) {
        for(int test = 0; test < 5; test++) {
            testStringToDouble(true);
            testStringToDouble(false);
        }
    }

    static void testStringToDouble(boolean std) {
        Random random = new Random(1);
        double y = 1.0;
        double z = 1.0;
        double step = 1.0000001;
        int count = 1_000_000;
        String[] data = new String[count * 3];
        int sign = 1;
        for (int i = 0; i < count; i++) {
            sign = -sign;
            double x;
            do {
                x = Double.longBitsToDouble(random.nextLong());
            } while (Double.isNaN(x));
            data[i * 3] = DoubleUtils.convertDoubleToString(x * sign);
            data[i * 3 + 1] = DoubleUtils.convertDoubleToString(y * sign);
            data[i * 3 + 2] = DoubleUtils.convertDoubleToString(z * sign);
            y *= step;
            z /= step;
        }
        double sum = 0;
        Profiler prof = new Profiler().startCollecting();
        long start = System.nanoTime();
        for (int i = 0; i < data.length; i++) {
            if (std) {
                sum += Double.parseDouble(data[i]);
            } else {
                sum += DoubleUtils.parseDouble(data[i]);
            }
        }
        long time = System.nanoTime() - start;
        System.out.println(time / count + " ns/entry std=" + std + " dummy " + sum);
        System.out.println(prof.getTop(10));
    }

    static void testDoubleToString(boolean std) {
        long start = System.nanoTime();
        long sum = 0;
        Profiler prof = new Profiler().startCollecting();
        Random random = new Random(1);
        StringBuffer buff = new StringBuffer(1000);
        byte[] buffer = new byte[1000];

        double y = 1.0;
        double z = 1.0;
        double step = 1.000001;
        int count = 1_000_000;
        for (int i = 0; i < count; i++) {
            double x = Double.longBitsToDouble(random.nextLong());
            if (std) {
                buff.append(y).append(x).append(z);
                sum += buff.length();
                buff.setLength(0);
            } else {
                int pos = DoubleUtils.appendDouble(buffer, 0, x);
                pos = DoubleUtils.appendDouble(buffer, pos, y);
                pos = DoubleUtils.appendDouble(buffer, pos, z);
                sum += pos;
            }
            y *= step;
            z /= step;
        }
        long time = System.nanoTime() - start;
        System.out.println(time / count + " ns/entry std=" + std + " dummy " + sum + " byte/value: " + sum / count / 3);
        System.out.println(prof.getTop(10));
    }

    @Test
    public void largeValues() {
        double step = 1.05;
        // double step = 1.0001;
        // double step = 1.00001;

        for (double i = 1.0; Double.isFinite(i); i *= step) {
            String got = DoubleUtils.convertDoubleToString(i);
            String expected = "" + i;
            try {
                Double.parseDouble(got);
            } catch (NumberFormatException e) {
                got = DoubleUtils.convertDoubleToString(i);
                fail();
            }
            double back = Double.parseDouble(got);
            if (i != back) {
                System.out.println("got " + got + "\nexp " + i);
                got = DoubleUtils.convertDoubleToString(i);
                fail();
                break;
            }
            if (got.charAt(0) != expected.charAt(0)) {
                got = DoubleUtils.convertDoubleToString(i);
                fail();
            }
            if (got.indexOf('.') != expected.indexOf('.') && got.indexOf('E') >= 0) {
                got = DoubleUtils.convertDoubleToString(i);
                fail();
            }
            double parsed2 = DoubleUtils.parseDouble(got);
            assertTrue(parsed2 == i);
        }
    }

    @Test
    public void smallValues() {
        double mul = 0.99;
        // double mul = 0.9999;
        // double mul = 0.99999;
        for (double i = 1.0; i > 0; ) {
            String got = DoubleUtils.convertDoubleToString(i);
            double parsed2 = DoubleUtils.parseDouble(got);
            if (parsed2 != i) {
                parsed2 = DoubleUtils.parseDouble(got);
            }
            assertTrue(parsed2 == i);
            try {
                double back = Double.parseDouble(got);
                if (i != back) {
                    System.out.println("got " + got + "\n    exp " + i + "\n    bak " + back);
                    got = DoubleUtils.convertDoubleToString(i);
                    fail();
                    break;
                }
                double j = i * mul;
                while (j == i) {
                    // subnormal
                    mul *= 0.9;
                    j = i * mul;
                }
                i = j;
            } catch (Exception e) {
                got = DoubleUtils.convertDoubleToString(i);
                fail();

            }
        }
    }

    @Test
    public void parseVeryLarge() {
        assertEquals("Infinity", "" + DoubleUtils.parseDouble("1e1000000"));
        assertEquals("-Infinity", "" + DoubleUtils.parseDouble("-1e1000000"));
        assertEquals("0.0", "" + DoubleUtils.parseDouble("0"));
        assertEquals("-0.0", "" + DoubleUtils.parseDouble("-0"));
        assertEquals("0.0", "" + DoubleUtils.parseDouble("1e-1000000"));
        assertEquals("-0.0", "" + DoubleUtils.parseDouble("-1e-1000000"));
    }

    @Test
    public void parseTest() {
        Random r = new Random(1);
        for (int op = 0; op < 10_000; op++) {
            StringBuilder buff = new StringBuilder();
            int len = 1 + r.nextInt(20);
            for (int i = 0; i < len; i++) {
                switch (r.nextInt(10)) {
                case 0:
                    buff.append('-');
                    break;
                case 1:
                    buff.append('e');
                    break;
                case 2:
                    buff.append('.');
                    break;
                case 3:
                    buff.append('#');
                    break;
                default:
                    buff.append((byte) ('0' + r.nextInt(10)));
                    break;
                }
            }
            String s = buff.toString();
            double exp;
            try {
                exp = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                exp = Double.NaN;
            }
            double got;
            try {
                got = DoubleUtils.parseDouble(s);
            } catch (NumberFormatException e) {
                got = Double.NaN;
            }
            if (Double.isNaN(exp) || Double.isNaN(got)) {
                assertTrue(Double.isNaN(exp) && Double.isNaN(got));
            } else if (exp != got) {
                double diff = Math.abs(1 - 1 / exp * got);
                if (diff > 1e-15) {
                    System.out.println(diff);
                    got = DoubleUtils.parseDouble(s);
                    assertTrue(exp == got);
                }
            }
        }
    }

    @Test
    public void doubleTest() {
        assertEquals(Double.toString(Double.NEGATIVE_INFINITY), DoubleUtils.convertDoubleToString(Double.NEGATIVE_INFINITY));
        assertEquals(Double.toString(Double.POSITIVE_INFINITY), DoubleUtils.convertDoubleToString(Double.POSITIVE_INFINITY));
        assertEquals(Double.toString(0.0 / 0.0), DoubleUtils.convertDoubleToString(0.0 / 0.0));
        assertEquals("-0.0", DoubleUtils.convertDoubleToString(-0.0));
        for (double n = 1.0; n < Double.POSITIVE_INFINITY; n *= 1000.0) {
            String expected = Double.toString(n);
            double n3 = Double.parseDouble(expected);
            if (n3 != n) {
                fail("fail " + n + " got " + n3 + " diff " + (n3 - n));
            }
            String got = DoubleUtils.convertDoubleToString(n);
            double n2 = Double.parseDouble(got);
            if (n2 != n) {
                DoubleUtils.convertDoubleToString(n);
                fail("expected " + expected + " got " + got + " diff " + (n2 - n));
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
                String got = DoubleUtils.convertDoubleToString(n);
                double n2 = Double.parseDouble(got);
                if (n2 != n) {
                    fail("expected " + expected + " got " + got + " diff " + (n2 - n));
                }
                double parsed2 = DoubleUtils.parseDouble(got);
                assertTrue(parsed2 == n);
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
            String got = DoubleUtils.convertDoubleToString(n);
            double n2 = Double.parseDouble(got);
            if (n2 != n) {
                fail("expected " + expected + " got " + got + " diff " + (n2 - n));
            }
            double parsed2 = DoubleUtils.parseDouble(got);
            assertTrue(parsed2 == n);
        }
    }

}
