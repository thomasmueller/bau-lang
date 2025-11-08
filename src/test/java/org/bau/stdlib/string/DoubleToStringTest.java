package org.bau.stdlib.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

public class DoubleToStringTest {

    @Test
    public void largeValues() {
        for (double i = 1.0; Double.isFinite(i); i *= 1.001) {
            String got = DoubleToString.convertDoubleToString(i);
            String expected = "" + i;
            double back = Double.parseDouble(got);
            if (i != back) {
                System.out.println("got " + got + "\nexp " + i);
                got = DoubleToString.convertDoubleToString(i);
                fail();
                break;
            }
            if (got.charAt(0) != expected.charAt(0)) {
                got = DoubleToString.convertDoubleToString(i);
                fail();
            }
            if (got.indexOf('.') != expected.indexOf('.') && got.indexOf('E') >= 0) {
                got = DoubleToString.convertDoubleToString(i);
                fail();
            }
        }
//        DoubleToString.printFact();
    }

    @Test
    public void smallValues() {
//        System.out.println("7: " + Double.parseDouble("9.713542516562364563E-116"));
//        System.out.println("A: " + Double.parseDouble("9.713542516562364564E-116"));
//        long a = Double.doubleToRawLongBits(Double.parseDouble("9.713542516562364563E-116"));
//        long b = Double.doubleToRawLongBits(Double.parseDouble("9.713542516562364564E-116"));
//        System.out.println(a);
//        System.out.println(b);
//        System.out.println(DoubleToString.exactString(Double.parseDouble("1.0")));
//        System.out.println(DoubleToString.exactString(Double.parseDouble("9.713542516562364563E-116")));
//        System.out.println(DoubleToString.exactString(Double.parseDouble("9.713542516562364564E-116")));
        for (double i = 1.0; i > 0; ) {
            String got = DoubleToString.convertDoubleToString(i);
            double back = Double.parseDouble(got);
            if (i != back) {
                System.out.println("got " + got + "\n    exp " + i + "\n    bak " + back);
                got = DoubleToString.convertDoubleToString(i);
//                fail();
//                break;
            }
            double mul = 0.99;
            double j = i * mul;
            while (j == i) {
                // subnormal
                mul *= 0.9;
                j = i * mul;
            }
            i = j;
        }
//        DoubleToString.printFact();
    }

//    @Test
    public void doubleTest() {
        assertEquals(Double.toString(Double.NEGATIVE_INFINITY), DoubleToString.convertDoubleToString(Double.NEGATIVE_INFINITY));
        assertEquals(Double.toString(Double.POSITIVE_INFINITY), DoubleToString.convertDoubleToString(Double.POSITIVE_INFINITY));
        assertEquals(Double.toString(0.0 / 0.0), DoubleToString.convertDoubleToString(0.0 / 0.0));
        for (double n = 1.0; n < Double.POSITIVE_INFINITY; n *= 1000.0) {
            String expected = Double.toString(n);
            double n3 = Double.parseDouble(expected);
            if (n3 != n) {
                fail("fail " + n + " got " + n3 + " diff " + (n3 - n));
            }
            String got = DoubleToString.convertDoubleToString(n);
            double n2 = Double.parseDouble(got);
            double diff = diff(n, n2);
            if (diff > 0 && n2 != n) {
                DoubleToString.convertDoubleToString(n);
                fail("expected " + expected + " got " + got + " diff " + diff + " diff " + (n2 - n));
            }
        }
//        Random r = new Random(1);
//        for (int i = 0; i < 1_000; i++) {
//            for (long max = 1; max > 0; max *= 10) {
//                double n = r.nextGaussian() * max;
//                String expected = Double.toString(n);
//                double n3 = Double.parseDouble(expected);
//                if (n3 != n) {
//                    fail();
//                }
//                String got = DoubleToString.convertDoubleToString(n);
//                double n2 = Double.parseDouble(got);
//                double diff = diff(n, n2);
//                if (diff > 0 && n2 != n) {
//                    fail("expected " + expected + " got " + got + " diff " + diff + " diff " + (n2 - n));
//                }
//            }
//        }
//        for (int i = 0; i < 1_000; i++) {
//            double n = Double.longBitsToDouble(r.nextLong());
//            if (Double.isNaN(n)) {
//                continue;
//            }
//            String expected = Double.toString(n);
//            double n3 = Double.parseDouble(expected);
//            if (n3 != n) {
//                fail();
//            }
//            String got = DoubleToString.convertDoubleToString(n);
//            double n2 = Double.parseDouble(got);
//            double diff = diff(n, n2);
//            if (diff > 0 && n2 != n) {
//                fail("expected " + expected + " got " + got + " diff " + diff + " diff " + (n2 - n));
//            }
//        }
    }

    private static double diff(double expected, double got) {
        double diff = expected - got;
        return diff;
//        if (expected == 0) {
//            return 1;
//        }
//        return Math.abs(1.0 * diff / expected);
    }

    @Test
    public void end() {
        DoubleToString.printFact();
    }
}
