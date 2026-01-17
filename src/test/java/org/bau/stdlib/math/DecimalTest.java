package org.bau.stdlib.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.Test;

public class DecimalTest {

    @Test
    public void simple() {
        Decimal
        x = Decimal.parse("0");
        assertEquals(0, x.signum());
        assertEquals("0", x.toString());
        x = Decimal.parse("10.0");
        assertEquals("10", x.toString());
        x = Decimal.parse("-123");
        assertEquals("-123", x.toString());
        x = Decimal.parse("12345678901234560000");
        assertEquals("1.234567890123456E19", x.toString());
        x = Decimal.parse("12345678901234.56");
        assertEquals("12345678901234.56", x.toString());
        x = Decimal.parse("123456789012345.6");
        assertEquals("123456789012345.6", x.toString());
        x = Decimal.parse("1234567890123456");
        assertEquals("1234567890123456", x.toString());
        x = Decimal.parse("0");
        assertTrue(x.divide(x).equals(x));

        Decimal a = Decimal.parse("9.23456789012345678");
        Decimal b = a.add(a);
        assertEquals("18.4691357802469135", b.toString());
        b = b.add(a);
        assertEquals("27.7037036703703702", b.toString());
        b = b.add(a);
        assertEquals("36.9382715604938269", b.toString());
        assertEquals("100", "100");

        a = Decimal.parse("2.0e+100");
        assertEquals("1E50", a.toString());
        a = Decimal.parse("99999999999999999999999999999999999999999999999999");
        assertEquals("9.99999999999999999E49", a.toString());
        a = Decimal.parse("100000000000000000000000000000000000000000000000000");
        assertEquals("1E50", a.toString());
        a = Decimal.parse("100000000000000000000000000000000000000000000000001");
        assertEquals("1E50", a.toString());
        a = Decimal.parse("1000000000000000000000000000000000000000000000000000");
        assertEquals("1E50", a.toString());
        a = Decimal.parse("999999999999999999999999999999999999999999999999999999999");
        assertEquals("1E50", a.toString());

        a = Decimal.parse("-99999999999999999999999999999999999999999999999999");
        assertEquals("-9.99999999999999999E49", a.toString());
        a = Decimal.parse("-100000000000000000000000000000000000000000000000000");
        assertEquals("-1E50", a.toString());
        a = Decimal.parse("-100000000000000000000000000000000000000000000000001");
        assertEquals("-1E50", a.toString());
        a = Decimal.parse("-1000000000000000000000000000000000000000000000000000");
        assertEquals("-1E50", a.toString());

        a = Decimal.parse("0.00000000000000000000000000000000000000000000000001");
        assertEquals("1E-50", a.toString());
        a = Decimal.parse("0.000000000000000000000000000000000000000000000000009");
        assertEquals("0", a.toString());
        a = Decimal.parse("-0.00000000000000000000000000000000000000000000000001");
        assertEquals("-1E-50", a.toString());
        a = Decimal.parse("-0.000000000000000000000000000000000000000000000000009");
        assertEquals("0", a.toString());
    }

    @Test
    public void parseErrors() {
        for (String s : new String[] { "", "-", "--1", "1e3e", "1e", "1e+", "1e-", "9e9+", "9e000+", "0.a" }) {
            try {
                Decimal.parse(s);
                fail();
            } catch (NumberFormatException e) {
                // expected
            }
        }
    }

    @Test
    public void operations() {
        Random r = new Random(1);
        // Profiler prof = new Profiler().startCollecting();
        for (int i = 0; i < 100; i++) {
            BigDecimal a = randomDecimal(r, i);
            Decimal aa = Decimal.parse(a.toString());
            double exp = Double.parseDouble(a.abs().toString());
            double got = Double.parseDouble(aa.abs().toString());
            assertTrue(SimpleMathTest.almostSame(exp, got, exp));
            exp = Double.parseDouble(a.negate().toString());
            got = Double.parseDouble(aa.negate().toString());
            assertTrue(SimpleMathTest.almostSame(exp, got, exp));
            assertEquals(a.signum(), aa.signum());
            for (int j = 0; j < 100; j++) {
                BigDecimal b = randomDecimal(r, j);
                Decimal bb = Decimal.parse(b.toString());

                if (a.compareTo(b) != aa.compareTo(bb)) {
                    aa.compareTo(bb);
                }
                assertEquals(a + "<->" + b, a.compareTo(b), aa.compareTo(bb));

                BigDecimal c = a.add(b);
                exp = Double.parseDouble(c.toString());
                exp = limit(exp);
                got = Double.parseDouble(aa.add(bb).toString());
                if (!SimpleMathTest.almostSame(exp, got, exp)) {
                    aa.add(bb);
                    SimpleMathTest.almostSame(exp, got, exp);
                }
                assertTrue(a + " + " + b + " = " + c + " got " + got,
                        SimpleMathTest.almostSame(exp, got, exp));

                c = a.subtract(b);
                exp = Double.parseDouble(c.toString());
                exp = limit(exp);
                got = Double.parseDouble(aa.subtract(bb).toString());
                if (!SimpleMathTest.almostSame(exp, got, exp)) {
                    aa.subtract(bb);
                    SimpleMathTest.almostSame(exp, got, exp);
                }
                assertTrue(a + " - " + b + " = " + c + " got " + got,
                        SimpleMathTest.almostSame(exp, got, exp));

                c = a.multiply(b);
                exp = Double.parseDouble(c.toString());
                exp = limit(exp);
                got = Double.parseDouble(aa.multiply(bb).toString());
                if (!SimpleMathTest.almostSame(exp, got, exp)) {
                    aa.multiply(bb);
                    SimpleMathTest.almostSame(exp, got, exp);
                }
                assertTrue(a + " * " + b + " = " + c + " got " + got,
                        SimpleMathTest.almostSame(exp, got, exp));

                if (b.doubleValue() != 0) {
                    c = BigDecimal.valueOf(a.doubleValue() / b.doubleValue());
                    exp = Double.parseDouble(c.toString());
                    exp = limit(exp);
                    got = Double.parseDouble(aa.divide(bb).toString());
                    if (!SimpleMathTest.almostSame(exp, got, exp)) {
                        aa.divide(bb);
                        SimpleMathTest.almostSame(exp, got, exp);
                    }
                    assertTrue(a + " / " + b + " = " + c + " got " + got,
                            SimpleMathTest.almostSame(exp, got, exp));
                }
            }
        }
        // System.out.println(prof.getTop(10));
    }

    private static double limit(double x) {
        if (x >= 1e50) {
            return 1e50;
        }
        if (x <= -1e50) {
            return -1e50;
        }
        if (x > 0 && x < 1e-50) {
            return 1e-50;
        }
        if (x < 0 && x > -1e-50) {
            return -1e-50;
        }
        return x;
    }

    @Test
    public void multiply() {
        Decimal a = Decimal.parse("10.3");
        Decimal b = Decimal.parse("2.0");
        assertEquals("20.6", a.multiply(b).toString());
        b = Decimal.parse("0.5");
        assertEquals("5.15", a.multiply(b).toString());
    }

    @Test
    public void divide() {
        Decimal a = Decimal.parse("10.3");
        Decimal b = Decimal.parse("2.0");
        assertEquals("5.15", a.divide(b).toString());
        b = Decimal.parse("0.5");
        assertEquals("20.6", a.divide(b).toString());
    }

    @Test
    public void compare() {
        assertTrue(Decimal.parse("10.3").compareTo(Decimal.parse("-10.3")) > 0);
        assertTrue(Decimal.parse("-10.3").compareTo(Decimal.parse("10.3")) < 0);
        assertTrue(Decimal.parse("10.3").compareTo(Decimal.parse("10.3")) == 0);
        assertTrue(Decimal.parse("103").compareTo(Decimal.parse("99.3")) > 0);
        assertTrue(Decimal.parse("1.1").compareTo(Decimal.parse("10.0")) < 0);
    }

    private static BigDecimal randomDecimal(Random r, int i) {
        if (i == 0) {
            return BigDecimal.ZERO;
        } else if (i < 20) {
            return BigDecimal.valueOf(r.nextInt(10) - 5);
        }
        return BigDecimal.valueOf(r.nextLong(1_000_000_000_000_000_000L)).scaleByPowerOfTen(30 - r.nextInt(40));
    }
}
