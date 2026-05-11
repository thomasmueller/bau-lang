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
            assertEquals("i: " + i, rawExpected, rawGot);
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
        int zero = SoftFloat.toRaw(0);
        assertEquals(0, SoftFloat.compare(zero, SoftFloat.add(zero, zero)));
        int one = SoftFloat.toRaw(1);
        assertEquals(0, SoftFloat.compare(zero, SoftFloat.subtract(one, one)));
        assertEquals(0, SoftFloat.compare(zero, SoftFloat.add(SoftFloat.negate(one), one)));
        Random r = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            float a = randomFloat(i, r);
            float b = randomFloat(i, r);
            float c = a + b;
            int rawA = Float.floatToIntBits(a);
            assertEquals(Float.isInfinite(a), SoftFloat.isInfinity(rawA));
            assertEquals(Float.isNaN(a), SoftFloat.isNaN(rawA));
            int rawB = Float.floatToIntBits(b);
            int rawC = Float.floatToIntBits(c);
            int expected = rawC;
            int got = SoftFloat.add(rawA, rawB);
            if (expected != got) {
                if (Math.abs(expected - got) < 17) {
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
            float a = randomFloat(i, r);
            float b = randomFloat(i, r);
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
            float a = randomFloat(i, r);
            float b = randomFloat(i, r);
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
    public void infinityAndNaNOperations() {
        float[] array = new float[] {
                0.0f, -0.0f, 1.0f, -1.0f, 0.5f, -0.5f,
                1.5f, -1.5f, 2.0f, -2.0f, 3f, -3f, 1.5f, -0.5f,
                100f, -100f, 1.0f / 0.0f, -1.0f / 0.0f,
                0.0f / 0.0f
        };
        for (float a : array) {
            for (float b : array) {
                int rawA = Float.floatToIntBits(a);
                int rawB = Float.floatToIntBits(b);
                int expected = Integer.signum(Float.compare(a, b));
                int got = Integer.signum(SoftFloat.compare(rawA, rawB));
                if (expected != got) {
                    got = SoftFloat.compare(rawA, rawB);
                    assertEquals("SoftFloat.compare(rawA, rawB) for a: " + a + " (raw bits: " + Long.toHexString(rawA) +
                            ") b: " + b + " (raw bits: " + Long.toHexString(rawB) +
                            ") expected: " + expected + " got: " + got, expected, got);
                }
                float c = a + b;
                expected = Float.floatToIntBits(c);
                got = SoftFloat.add(rawA, rawB);
                if (expected != got) {
                    if (Float.isNaN(Float.intBitsToFloat(got)) && Float.isNaN(c)) {
                        // ok
                    } else if (Math.abs(expected - got) < 3) {
                        // ok
                    } else {
                        System.out.println(a + " + " + b + " = " + c);
                        System.out.println("got " + Float.intBitsToFloat(got));
                        got = SoftFloat.divide(rawA, rawB);
                        assertEquals(expected, got);
                    }
                }
                c = a - b;
                expected = Float.floatToIntBits(c);
                got = SoftFloat.subtract(rawA, rawB);
                if (expected != got) {
                    if (Float.isNaN(Float.intBitsToFloat(got)) && Float.isNaN(c)) {
                        // ok
                    } else if (Math.abs(expected - got) < 3) {
                        // ok
                    } else {
                        System.out.println(a + " - " + b + " = " + c);
                        System.out.println("got " + Float.intBitsToFloat(got));
                        got = SoftFloat.divide(rawA, rawB);
                        assertEquals(expected, got);
                    }
                }
                c = a / b;
                expected = Float.floatToIntBits(c);
                got = SoftFloat.divide(rawA, rawB);
                if (expected != got) {
                    if (Float.isNaN(Float.intBitsToFloat(got)) && Float.isNaN(c)) {
                        // ok
                    } else if (Math.abs(expected - got) < 3) {
                        // ok
                    } else {
                        System.out.println(a + " / " + b + " = " + c);
                        System.out.println("got " + Float.intBitsToFloat(got));
                        got = SoftFloat.divide(rawA, rawB);
                        assertEquals(expected, got);
                    }
                }
                c = a * b;
                expected = Float.floatToIntBits(c);
                got = SoftFloat.multiply(rawA, rawB);
                if (expected != got) {
                    if (Float.isNaN(Float.intBitsToFloat(got)) && Float.isNaN(c)) {
                        // ok
                    } else if (Math.abs(expected - got) < 3) {
                        // ok
                    } else {
                        System.out.println(a + " * " + b + " = " + c);
                        System.out.println("got " + Float.intBitsToFloat(got));
                        got = SoftFloat.divide(rawA, rawB);
                        assertEquals(expected, got);
                    }
                }
                int comp = Float.compare(a, b);
                got = SoftFloat.compare(rawA, rawB);
                assertEquals(comp, got);
            }
        }
    }

    @Test
    public void compare() {
        Random r = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            float a = randomFloat(i, r);
            float b = randomFloat(i, r);
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

    private float randomFloat(int index, Random r) {
        if (index < 100) {
            return r.nextInt(100) - 50;
        } else if (index < 1000) {
            return (float) (1.0 / (r.nextInt(100) - 50));
        }
        return r.nextFloat();
    }

}
