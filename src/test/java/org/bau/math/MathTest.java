package org.bau.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

public class MathTest {

    static final double[] TEST = new double[500];
    
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
        for (int j = 0; j < 10; j++) {
            TEST[i++] = r.nextGaussian();
        }
        for (int j = 0; j < 10; j++) {
            TEST[i++] = r.nextDouble(1) - 0.5;
            TEST[i++] = r.nextDouble(10) - 5;
            TEST[i++] = r.nextDouble(100) - 50;
        }
        for (double x = 1; x < Double.POSITIVE_INFINITY; x *= 1024) {
            TEST[i++] = x;
            TEST[i++] = -x;
        }
        for (double x = 1; x > 0; x /= 1024) {
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
            double got = MathOps.sqrt(x);
            assertTrue(same(expected, got, x));
        }
    }
    
    @Test
    public void convertLongBitsTest() {
        for (double x : TEST) {
            long expected = Double.doubleToLongBits(x);
            long got = MathOps.convertDoubleToLongBits(x);
            if (expected != got) {
                System.out.println("x: " + x);
                System.out.println(Long.toBinaryString(expected) + " expected");
                System.out.println(Long.toBinaryString(got) + " got");
            }
            assertEquals(expected, got);
            double y = MathOps.convertLongBitsToDouble(got);
            long got2 = Double.doubleToLongBits(y);
            if (got2 != got) {
                System.out.println(Long.toBinaryString(expected) + " expected");
                y = MathOps.convertLongBitsToDouble(got);                
            }
            assertEquals(x + " != " + y, expected, got2);
        }
    }
    
    @Test
    public void sinTest() {
        for (double x : TEST) {
            double expected = Math.sin(x);
            double got = MathOps.sin(x);
            if (x < 1e9 && x > -1e9) {
                assertTrue(sameTrig(expected, got, x));
            }
        }
    }

    @Test
    public void cosTest() {
        for (double x : TEST) {
            double expected = Math.cos(x);
            double got = MathOps.cos(x);
            if (x < 1e8 && x > -1e8) {
                assertTrue(sameTrig(expected, got, x));
            }
        }
    }
    
    @Test
    public void tanTest() {
        for (double x : TEST) {
            double expected = Math.tan(x);
            double got = MathOps.tan(x);
            if (x < 1e8 && x > -1e8) {
                assertTrue(sameTrig(expected, got, x));
            }
        }
    }    

    @Test
    public void logTest() {
        for (double x : TEST) {
            double expected = Math.log(x);
            double got = MathOps.log(x);
            assertTrue(same(expected, got, x));
        }
    }

    @Test
    public void expTest() {
        for (double x : TEST) {
            double expected = Math.exp(x);
            double got = MathOps.exp(x);
            assertTrue(same(expected, got, x));
        }
    }

    @Test
    public void log10Test() {
        for (double x : TEST) {
            double expected = Math.log10(x);
            double got = MathOps.log10(x);
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
            // System.out.println("diff " + diff + " expected " + expected + " got " + got + " for " + forValue);
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
                double got = MathOps.pow(x, y);
                assertTrue(same(expected, got, x));
            }
        }
    }    

    @Test
    public void asinTest() {
        for (double x : TEST) {
            double expected = Math.asin(x);
            double got = MathOps.asin(x);
            assertTrue(sameTrig(expected, got, x));
        }
    }
    
    @Test
    public void nanTest() {
        for (double x : TEST) {
            assertTrue(Double.isNaN(x) == MathOps.isNotANumber(x));
        }
    }
    
    @Test
    public void floorCeilSignumRoundTest() {
        System.out.println(Math.round(-1));
        System.out.println(Math.round(-0.5));
        System.out.println(Math.round(0.0));
        System.out.println(Math.round(0.5));
        System.out.println(Math.round(1));
        
        for (double x : TEST) {
            assertTrue(exactSame(Math.floor(x), MathOps.floor(x)));
            assertTrue(exactSame(Math.ceil(x), MathOps.ceil(x)));
            assertTrue(exactSame(Math.signum(x), MathOps.signum(x)));
            assertTrue(exactSame(Math.round(x), MathOps.round(x)));
        }
    }
    
    @Test
    public void minMaxText() {
        for (double x : TEST) {
            for (double y : TEST) {
                double min = Math.min(x, y);
                double max = Math.max(x, y);
                assertTrue(exactSame(min, MathOps.min(x, y)));
                assertTrue(exactSame(max, MathOps.max(x, y)));
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
            double got = MathOps.atan(x);
            assertTrue(sameTrig(expected, got, x));
        }
    }
    
    @Test
    public void acosTest() {
        for (double x : TEST) {
            double expected = Math.acos(x);
            double got = MathOps.acos(x);
            assertTrue(sameTrig(expected, got, x));
        }
    }

}
