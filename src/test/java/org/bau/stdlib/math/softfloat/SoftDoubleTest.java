package org.bau.stdlib.math.softfloat;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class SoftDoubleTest {

    @Test
    public void convert() {
        for (long i = Long.MIN_VALUE; i <= 0; i += 1000L << 32) {
            long raw = (long) i;
            double d = Double.longBitsToDouble(raw);
            long expected = (long) d;
            long got = SoftDouble.longValue(raw);
            if (expected != got) {
                System.out.println(Long.toHexString(raw) + " " + d + " -> " + expected);
                System.out.println(SoftDouble.longValue(raw));
                assertEquals("i:" + i, expected, got);
            }
        }
        for (long i = 0; i >= 0; i += 1000L << 32) {
            long raw = (long) i;
            double d = Double.longBitsToDouble(raw);
            long expected = (long) d;
            long got = SoftDouble.longValue(raw);
            if (expected != got) {
                System.out.println(Long.toHexString(raw) + " " + d + " -> " + expected);
                System.out.println(SoftDouble.longValue(raw));
                assertEquals("i:" + i, expected, got);
            }
        }
        for (long i = Long.MIN_VALUE; i < 0; i += 10_000_000L << 32) {
            long raw = Double.doubleToLongBits((double) i);
            double d = Double.longBitsToDouble(raw);
            long expected = (long) d;
            long got = SoftDouble.longValue(raw);
            assertEquals(expected, got);
            double f = Double.valueOf("" + got);
            long rawExpected = Double.doubleToLongBits(f);
            long rawGot = SoftDouble.toRaw((long) f);
            assertEquals(rawExpected, rawGot);
        }
        for (long i = 0; i >= 0; i += 10_000_000L << 32) {
            long raw = Double.doubleToLongBits((double) i);
            double d = Double.longBitsToDouble(raw);
            long expected = (long) d;
            long got = SoftDouble.longValue(raw);
            assertEquals(expected, got);
            double f = Double.valueOf("" + got);
            long rawExpected = Double.doubleToLongBits(f);
            long rawGot = SoftDouble.toRaw((long) f);
            assertEquals(rawExpected, rawGot);
        }
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            double d = r.nextDouble();
            long raw = Double.doubleToLongBits(d);
            long expected = (long) d;
            long got = SoftDouble.longValue(raw);
            if (expected != got) {
                got = SoftDouble.longValue(raw);
                assertEquals(expected, got);
            }
        }
        for (int i = 0; i < 100_000; i++) {
            long raw = r.nextLong();
            double d = Double.longBitsToDouble(raw);
            long expected = (long) d;
            long got = SoftDouble.longValue(raw);
            if (expected != got) {
                System.out.println(d + " -> " + expected);
                System.out.println(SoftDouble.longValue(raw));
                assertEquals(expected, got);
            }
        }
    }

    @Test
    public void add() {
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            double a = r.nextDouble();
            double b = r.nextDouble();
            double c = a + b;
            long rawA = Double.doubleToLongBits(a);
            long rawB = Double.doubleToLongBits(b);
            long rawC = Double.doubleToLongBits(c);
            long expected = rawC;
            long got = SoftDouble.add(rawA, rawB);
            if (expected != got) {
                if (Math.abs(expected - got) < 3) {
                    // ok
                } else {
                    System.out.println("#" + i + ": " + a + " + " + b + " = " + c);
                    System.out.println("got " + Double.longBitsToDouble(got));
                    got = SoftDouble.add(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
        for (int i = 0; i < 100_000; i++) {
            long rawA = r.nextLong();
            long rawB = r.nextLong();
            double a = Double.longBitsToDouble(rawA);
            double b = Double.longBitsToDouble(rawB);
            double c = a + b;
            long rawC = Double.doubleToLongBits(c);
            long expected = rawC;
            long got = SoftDouble.add(rawA, rawB);
            if (expected != got) {
                double e = Double.longBitsToDouble(expected);
                double g = Double.longBitsToDouble(got);
                if (Double.isNaN(e) && Double.isNaN(g)) {
                    // ok
                } else if (Math.abs(expected - got) < 1000) {
                    // ok
                } else if (Math.abs(a) < 1e-32 || Math.abs(b) < 1e-32) {
                    // subnormals
                } else {
                    System.out.println("#" + i + ": " + a + " + " + b + " = " + c);
                    System.out.println("got " + Double.longBitsToDouble(got));
                    System.out.println("expected " + Long.toBinaryString(expected));
                    System.out.println("got      " + Long.toBinaryString(got));
                    got = SoftDouble.add(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
    }

    @Test
    public void multiply() {
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            double a = r.nextDouble();
            double b = r.nextDouble();
            double c = a * b;
            long rawA = Double.doubleToLongBits(a);
            long rawB = Double.doubleToLongBits(b);
            long rawC = Double.doubleToLongBits(c);
            long expected = rawC;
            long got = SoftDouble.multiply(rawA, rawB);
            if (expected != got) {
                if (Math.abs(expected - got) < 3) {
                    // ok
                } else {
                    System.out.println("#" + i + ": " + a + " * " + b + " = " + c);
                    System.out.println("got " + Double.longBitsToDouble(got));
                    got = SoftDouble.multiply(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
        for (int i = 0; i < 1_000_000; i++) {
            long rawA = r.nextLong();
            long rawB = r.nextLong();
            double a = Double.longBitsToDouble(rawA);
            double b = Double.longBitsToDouble(rawB);
            double c = a * b;
            long rawC = Double.doubleToLongBits(c);
            long expected = rawC;
            long got = SoftDouble.multiply(rawA, rawB);
            if (expected != got) {
                double e = Double.longBitsToDouble(expected);
                double g = Double.longBitsToDouble(got);
                if (Double.isNaN(e) && Double.isNaN(g)) {
                    // ok
                } else if (Math.abs(expected - got) < 1000) {
                    // ok
                } else if (Math.abs(c) < 2e-31) {
                    // subnormals
                } else if (Math.abs(a) > 1e7 && Math.abs(b) < 1e-30) {
                    // large multiply by small
                } else if (Math.abs(b) > 1e7 && Math.abs(a) < 1e-30) {
                    // small multiply by large
                } else {
                    System.out.println("#" + i + ": " + a + " * " + b + " = " + c);
                    System.out.println("got " + Double.longBitsToDouble(got));
                    System.out.println("expected " + Long.toBinaryString(expected));
                    System.out.println("got      " + Long.toBinaryString(got));
                    got = SoftDouble.multiply(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
    }

    @Test
    public void divide() {
        Random r = new Random(1);
        for (int i = 0; i < 1_000; i++) {
            double a = r.nextDouble();
            double b = r.nextDouble();
            double c = a / b;
            long rawA = Double.doubleToLongBits(a);
            long rawB = Double.doubleToLongBits(b);
            long rawC = Double.doubleToLongBits(c);
            long expected = rawC;
            long got = SoftDouble.divide(rawA, rawB);
            if (expected != got) {
                if (Math.abs(expected - got) < 3) {
                    // ok
                } else {
                    System.out.println("#" + i + ": " + a + " / " + b + " = " + c);
                    System.out.println("got " + Double.longBitsToDouble(got));
                    got = SoftDouble.divide(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
        for (int i = 0; i < 10_000; i++) {
            long rawA = r.nextLong();
            long rawB = r.nextLong();
            double a = Double.longBitsToDouble(rawA);
            double b = Double.longBitsToDouble(rawB);
            double c = a / b;
            long rawC = Double.doubleToLongBits(c);
            long expected = rawC;
            long got = SoftDouble.divide(rawA, rawB);
            if (expected != got) {
                double e = Double.longBitsToDouble(expected);
                double g = Double.longBitsToDouble(got);
                if (Double.isNaN(e) && Double.isNaN(g)) {
                    // ok
                } else if (Math.abs(expected - got) < 1000) {
                    // ok
                } else if (Math.abs(b) < 2e-30) {
                    // subnormals
                } else if (Math.abs(c) < 2e-30) {
                    // subnormals
                } else if (Math.abs(a) < 1e-8 && Math.abs(b) < 1e-30) {
                    // divide small by small
                } else if (Math.abs(b) < 1e-8 && Math.abs(a) < 1e-30) {
                    // divide small by small
                } else {
                    System.out.println("#" + i + ": " + a + " / " + b + " = " + c);
                    System.out.println("got " + Double.longBitsToDouble(got));
                    System.out.println("expected " + Long.toBinaryString(expected));
                    System.out.println("got      " + Long.toBinaryString(got));
                    got = SoftDouble.divide(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
    }

    @Test
    public void compare() {
        Random r = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            double a = r.nextDouble();
            double b = r.nextDouble();
            long rawA = Double.doubleToLongBits(a);
            long rawB = Double.doubleToLongBits(b);
            int expected = Integer.signum(Double.compare(a, b));
            long got = Long.signum(SoftDouble.compare(rawA, rawB));
            if (expected != got) {
                System.out.println("#" + i + ": " + a + " comp " + b + ": " + expected);
                got = SoftDouble.compare(rawA, rawB);
                assertEquals(expected, got);
            }
        }
        for (int i = 0; i < 1_000_000; i++) {
            long rawA = r.nextLong();
            long rawB = r.nextLong();
            double a = Double.longBitsToDouble(rawA);
            rawA = Double.doubleToLongBits(a);
            double b = Double.longBitsToDouble(rawB);
            rawB = Double.doubleToLongBits(b);
            long got = Long.signum(SoftDouble.compare(rawA, rawB));
            int expected = Integer.signum(Double.compare(a, b));
            if (expected != got) {
                System.out.println("#" + i + ": " + a + " comp " + b + ": " + expected);
                got = SoftDouble.compare(rawA, rawB);
                expected = Double.compare(a, b);
                assertEquals(expected, got);
            }
        }
    }

}
