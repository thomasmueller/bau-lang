package org.bau.stdlib.math.softfloat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FixedPointTest {

    @Test
    public void parse() {
        assertEquals(0, FixedPoint.parse("0"));
        assertEquals(0x10000, FixedPoint.parse("1"));
        assertEquals(0x08000, FixedPoint.parse("0.5"));
        assertEquals(0x00001, FixedPoint.parse("0.00002"));
        assertEquals(0x10001, FixedPoint.parse("1.00002"));
        assertEquals(0x14001, FixedPoint.parse("1.25002"));
        assertEquals(0x14001, FixedPoint.parse("1.2500219999999999"));
        assertEquals(0x7fffffff, FixedPoint.parse("999999.2500219999999999"));
        assertEquals(-0x10000, FixedPoint.parse("-1"));
        assertEquals(-0x14001, FixedPoint.parse("-1.25002"));
        assertEquals(0x7fffffff, FixedPoint.parse("999999"));
        assertEquals(-0x7fffffff, FixedPoint.parse("-999999"));
        assertEquals(0x80000000, FixedPoint.parse("1.2.5"));
        assertEquals(0x80000000, FixedPoint.parse(" 1 "));
        assertEquals(0x3243f, FixedPoint.parse("" + Math.PI));
        assertEquals(0x3243f, FixedPoint.parse("3.1415926535"));
        assertEquals(0x2b7e1, FixedPoint.parse("" + Math.E));
        assertEquals(0x2b7e1, FixedPoint.parse("2.7182818284"));
    }

    @Test
    public void toStringTest() {
        assertEquals("0", FixedPoint.toString(FixedPoint.parse("0")));
        assertEquals("12345", FixedPoint.toString(FixedPoint.parse("12345")));
        assertEquals("-12345", FixedPoint.toString(FixedPoint.parse("-12345")));
        assertEquals("32767.999984", FixedPoint.toString(FixedPoint.parse("99999")));
        assertEquals("-32767.999984", FixedPoint.toString(FixedPoint.parse("-99999")));
        assertEquals("NaN", FixedPoint.toString(FixedPoint.parse("0.0.0")));
        assertEquals("3.141586", FixedPoint.toString(FixedPoint.PI));
        assertEquals("3.141586", FixedPoint.toString(FixedPoint.PI));
        assertEquals("-32767.999984", FixedPoint.toString(FixedPoint.MIN));
        assertEquals("32767.999984", FixedPoint.toString(FixedPoint.MAX));
        assertEquals("NaN", FixedPoint.toString(FixedPoint.NAN));
    }

    @Test
    public void simple() {
        int zero = FixedPoint.parse("0");
        int one = FixedPoint.parse("1");
        int ten = FixedPoint.parse("10");
        int two = FixedPoint.parse("2");
        int half = FixedPoint.parse("0.5");

        assertEquals("0", FixedPoint.toString(FixedPoint.add(zero, zero)));
        assertEquals("2", FixedPoint.toString(FixedPoint.add(one, one)));
        assertEquals("1.500000", FixedPoint.toString(FixedPoint.subtract(two, half)));
        assertEquals("5", FixedPoint.toString(FixedPoint.divide(ten, two)));
        assertEquals("1.414199", FixedPoint.toString(FixedPoint.sqrt(two)));
    }

    @Test
    public void simpleSaturated() {
        int one = FixedPoint.parse("1");
        int two = FixedPoint.parse("2");
        int thousand = FixedPoint.parse("1000");
        int max = FixedPoint.parse("99999");

        assertEquals(max, FixedPoint.addSaturated(max, one));
        assertEquals(max, FixedPoint.multiplySaturated(max, two));
        assertEquals(max, FixedPoint.multiplySaturated(thousand, thousand));
    }

    @Test
    public void sqrt() {
        int one = FixedPoint.parse("1");
        int two = FixedPoint.parse("2");
        int thousand = FixedPoint.parse("1000");
        int max = FixedPoint.parse("99999");
        assertEquals("1", FixedPoint.toString(FixedPoint.sqrt(one)));
        assertEquals("1.414199", FixedPoint.toString(FixedPoint.sqrt(two)));
        assertEquals("31.622772", FixedPoint.toString(FixedPoint.sqrt(thousand)));
        assertEquals("181.019332", FixedPoint.toString(FixedPoint.sqrt(max)));
    }

    @Test
    public void divide() {
        int zero = FixedPoint.parse("0");
        int one = FixedPoint.parse("1");
        int two = FixedPoint.parse("2");
        int thousand = FixedPoint.parse("1000");
        int max = FixedPoint.parse("99999");
        assertEquals("1", FixedPoint.toString(FixedPoint.divide(one, one)));
        assertEquals("0", FixedPoint.toString(FixedPoint.divide(zero, one)));
        assertEquals("0.500000", FixedPoint.toString(FixedPoint.divide(one, two)));
        assertEquals("2", FixedPoint.toString(FixedPoint.divide(two, one)));
        assertEquals("0.000991", FixedPoint.toString(FixedPoint.divide(one, thousand)));
        assertEquals("0.000030", FixedPoint.toString(FixedPoint.divide(one, max)));
        assertEquals("1", FixedPoint.toString(FixedPoint.divide(max, max)));
        assertEquals("32767.999984", FixedPoint.toString(FixedPoint.divide(one, zero)));
        assertEquals("-32767.999984", FixedPoint.toString(FixedPoint.divide(-one, zero)));
        assertEquals("0", FixedPoint.toString(FixedPoint.divide(zero, zero)));
    }

    @Test
    public void pow() {
        double min = (double) FixedPoint.MIN / (1 << 16);
        double max = (double) FixedPoint.MAX / (1 << 16);
        double sumSq = 0;
        for (double a = 0.1; a < 10; a += 0.1) {
            for (double b = 0.1; b < 10; b += 0.1) {
                double exp = Math.pow(a, b);
                if (exp > max) {
                    exp = max;
                } else if (exp < min) {
                    exp = min;
                }
                double got = (double) FixedPoint.pow(
                        (int) (a * (1 << 16)),
                        (int) (b * (1 << 16))) / (1 << 16);
                double diff;
                if (Math.abs(exp) > 0.1) {
                    // relative error
                    diff = Math.abs((got - exp) / exp) * 100;
                } else {
                    // absolute error
                    int expInt = (int) Math.round(exp * (1 << 16));
                    int gotInt = (int) Math.round(got * (1 << 16));
                    diff = Math.abs(expInt - gotInt);
                }
                sumSq += diff * diff;
                // System.out.println(a + "^" + b + " = " + exp + " got " + got + " diff " + diff);
            }
        }
        assertTrue(sumSq < 1000);
    }

    @Test
    public void mathFunctions() {
        for(int function = 0; function < 6; function++) {
            double sumSq = 0;
            for (int i = 1; i < FixedPoint.PI; i++) {
                double v = (double) i / (1 << 16);
                int got;
                double expected;
                switch (function) {
                case 0:
                    expected = Math.log(v);
                    got = FixedPoint.log(i);
                    break;
                case 1:
                    expected = Math.exp(v);
                    got = FixedPoint.exp(i);
                    break;
                case 2:
                    expected = Math.sqrt(v);
                    got = FixedPoint.sqrt(i);
                    break;
                case 3:
                    expected = Math.sin(v);
                    got = FixedPoint.sin(i);
                    break;
                case 4:
                    expected = Math.cos(v);
                    got = FixedPoint.cos(i);
                    break;
                case 5:
                    expected = Math.tan(v);
                    if (Math.abs(expected) > 10) {
                        continue;
                    }
                    got = FixedPoint.tan(i);
                    break;
                default:
                    throw new IllegalArgumentException();
                }
                double gotV  = (double) got / (1 << 16);
                double diff;
                if (Math.abs(expected) > 0.1) {
                    // relative error
                    diff = Math.abs((gotV - expected) / expected) * 100;
                } else {
                    // absolute error
                    int expInt = (int) Math.round(expected * (1 << 16));
                    diff = Math.abs(expInt - got);
                }
                if (i % 1000 == -1) {
                    System.out.println("function " + function + " i " + i +
                            " exp " + expected + " got " + gotV +
                            " diff " + diff +
                            " for " + v + " " + sumSq) ;
                }
                sumSq += diff * diff;
            }
            System.out.println("fun " + function + " sumSq " + sumSq);
            assertTrue("sumSq " + sumSq, sumSq < 400_000L);
        }
    }

}
