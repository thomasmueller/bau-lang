package org.bau.stdlib.math.softfloat;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class SoftFloatTest {

    @Test
    public void convert() {
        for (long i = Integer.MIN_VALUE; i <= Integer.MAX_VALUE; i += 1000) {
            int raw = (int) i;
            float d = Float.intBitsToFloat(raw);
            int expected = (int) d;
            int got = SoftFloat.intValue(raw);
            if (expected != got) {
                System.out.println(Integer.toHexString(raw) + " " + d + " -> " + expected);
                System.out.println(SoftFloat.intValue(raw));
                assertEquals("i:" + i, expected, got);
            }
        }
        for (long i = Integer.MIN_VALUE; i <= Integer.MAX_VALUE; i += 10_000_000) {
            int raw = (int) i;
            float d = Float.intBitsToFloat(raw);
            int expected = (int) d;
            int got = SoftFloat.intValue(raw);
            assertEquals(expected, got);
            float f = Float.valueOf("" + got);
            int rawExpected = Float.floatToIntBits(f);
            int rawGot = SoftFloat.toRaw((int) f);
            assertEquals(rawExpected, rawGot);
        }
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            float d = r.nextFloat();
            int raw = Float.floatToIntBits(d);
            int expected = (int) d;
            int got = SoftFloat.intValue(raw);
            if (expected != got) {
                got = SoftFloat.intValue(raw);
                assertEquals(expected, got);
            }
        }
        for (int i = 0; i < 100_000; i++) {
            int raw = r.nextInt();
            float d = Float.intBitsToFloat(raw);
            int expected = (int) d;
            int got = SoftFloat.intValue(raw);
            if (expected != got) {
                System.out.println(d + " -> " + expected);
                System.out.println(SoftFloat.intValue(raw));
                assertEquals(expected, got);
            }
        }
    }

    @Test
    public void add() {
        Random r = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            float a = r.nextFloat();
            float b = r.nextFloat();
            float c = a + b;
            int rawA = Float.floatToIntBits(a);
            int rawB = Float.floatToIntBits(b);
            int rawC = Float.floatToIntBits(c);
            int expected = rawC;
            int got = SoftFloat.add(rawA, rawB);
            if (expected != got) {
                if (Math.abs(expected - got) < 3) {
                    // ok
                } else {
                    System.out.println("#" + i + ": " + a + " + " + b + " = " + c);
                    System.out.println("got " + Float.intBitsToFloat(got));
                    got = SoftFloat.add(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
        for (int i = 0; i < 1_000_000; i++) {
            int rawA = r.nextInt();
            int rawB = r.nextInt();
            float a = Float.intBitsToFloat(rawA);
            float b = Float.intBitsToFloat(rawB);
            float c = a + b;
            int rawC = Float.floatToIntBits(c);
            int expected = rawC;
            int got = SoftFloat.add(rawA, rawB);
            if (expected != got) {
                float e = Float.intBitsToFloat(expected);
                float g = Float.intBitsToFloat(got);
                if (Float.isNaN(e) && Float.isNaN(g)) {
                    // ok
                } else if (Math.abs(expected - got) < 1000) {
                    // ok
                } else if (Math.abs(a) < 1e-32 || Math.abs(b) < 1e-32) {
                    // subnormals
                } else {
                    System.out.println("#" + i + ": " + a + " + " + b + " = " + c);
                    System.out.println("got " + Float.intBitsToFloat(got));
                    System.out.println("expected " + Long.toBinaryString(expected));
                    System.out.println("got      " + Long.toBinaryString(got));
                    got = SoftFloat.add(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
    }

    @Test
    public void multiply() {
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            float a = r.nextFloat();
            float b = r.nextFloat();
            float c = a * b;
            int rawA = Float.floatToIntBits(a);
            int rawB = Float.floatToIntBits(b);
            int rawC = Float.floatToIntBits(c);
            int expected = rawC;
            int got = SoftFloat.multiply(rawA, rawB);
            if (expected != got) {
                if (Math.abs(expected - got) < 3) {
                    // ok
                } else {
                    System.out.println("#" + i + ": " + a + " * " + b + " = " + c);
                    System.out.println("got " + Float.intBitsToFloat(got));
                    got = SoftFloat.multiply(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
        for (int i = 0; i < 1_000_000; i++) {
            int rawA = r.nextInt();
            int rawB = r.nextInt();
            float a = Float.intBitsToFloat(rawA);
            float b = Float.intBitsToFloat(rawB);
            float c = a * b;
            int rawC = Float.floatToIntBits(c);
            int expected = rawC;
            int got = SoftFloat.multiply(rawA, rawB);
            if (expected != got) {
                float e = Float.intBitsToFloat(expected);
                float g = Float.intBitsToFloat(got);
                if (Float.isNaN(e) && Float.isNaN(g)) {
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
                    System.out.println("got " + Float.intBitsToFloat(got));
                    System.out.println("expected " + Long.toBinaryString(expected));
                    System.out.println("got      " + Long.toBinaryString(got));
                    got = SoftFloat.multiply(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
    }

    @Test
    public void divide() {
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            float a = r.nextFloat();
            float b = r.nextFloat();
            float c = a / b;
            int rawA = Float.floatToIntBits(a);
            int rawB = Float.floatToIntBits(b);
            int rawC = Float.floatToIntBits(c);
            int expected = rawC;
            int got = SoftFloat.divide(rawA, rawB);
            if (expected != got) {
                if (Math.abs(expected - got) < 3) {
                    // ok
                } else {
                    System.out.println("#" + i + ": " + a + " / " + b + " = " + c);
                    System.out.println("got " + Float.intBitsToFloat(got));
                    got = SoftFloat.divide(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
        for (int i = 0; i < 1_000_000; i++) {
            int rawA = r.nextInt();
            int rawB = r.nextInt();
            float a = Float.intBitsToFloat(rawA);
            float b = Float.intBitsToFloat(rawB);
            float c = a / b;
            int rawC = Float.floatToIntBits(c);
            int expected = rawC;
            int got = SoftFloat.divide(rawA, rawB);
            if (expected != got) {
                float e = Float.intBitsToFloat(expected);
                float g = Float.intBitsToFloat(got);
                if (Float.isNaN(e) && Float.isNaN(g)) {
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
                    System.out.println("got " + Float.intBitsToFloat(got));
                    System.out.println("expected " + Long.toBinaryString(expected));
                    System.out.println("got      " + Long.toBinaryString(got));
                    got = SoftFloat.divide(rawA, rawB);
                    assertEquals(expected, got);
                }
            }
        }
    }

    @Test
    public void compare() {
        Random r = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            float a = r.nextFloat();
            float b = r.nextFloat();
            int rawA = Float.floatToIntBits(a);
            int rawB = Float.floatToIntBits(b);
            int expected = Integer.signum(Float.compare(a, b));
            int got = Integer.signum(SoftFloat.compare(rawA, rawB));
            if (expected != got) {
                System.out.println("#" + i + ": " + a + " comp " + b + ": " + expected);
                got = SoftFloat.compare(rawA, rawB);
                assertEquals(expected, got);
            }
        }
        for (int i = 0; i < 1_000_000; i++) {
            int rawA = r.nextInt();
            int rawB = r.nextInt();
            float a = Float.intBitsToFloat(rawA);
            rawA = Float.floatToIntBits(a);
            float b = Float.intBitsToFloat(rawB);
            rawB = Float.floatToIntBits(b);
            int got = Integer.signum(SoftFloat.compare(rawA, rawB));
            int expected = Integer.signum(Float.compare(a, b));
            if (expected != got) {
                System.out.println("#" + i + ": " + a + " comp " + b + ": " + expected);
                got = SoftFloat.compare(rawA, rawB);
                expected = Float.compare(a, b);
                assertEquals(expected, got);
            }
        }
    }

}
