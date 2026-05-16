package org.bau.stdlib.math.softfloat;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class FullSoftFloatTest {

    @Test
    public void convert() {
        assertEquals(Integer.MAX_VALUE, (int) Float.intBitsToFloat(FullSoftFloat.toRaw(Integer.MAX_VALUE)));
        assertEquals(Integer.MIN_VALUE, (int) Float.intBitsToFloat(FullSoftFloat.toRaw(Integer.MIN_VALUE)));
        for (long i = Integer.MIN_VALUE; i <= Integer.MAX_VALUE; i += 1000) {
            int raw = (int) i;
            float d = Float.intBitsToFloat(raw);
            int expected = (int) d;
            int got = FullSoftFloat.intValue(raw);
            if (expected != got) {
                System.out.println(Integer.toHexString(raw) + " " + d + " -> " + expected);
                System.out.println(FullSoftFloat.intValue(raw));
                assertEquals("i:" + i, expected, got);
            }
        }
        for (long i = Integer.MIN_VALUE; i <= Integer.MAX_VALUE; i += 10_000_000) {
            int raw = (int) i;
            float d = Float.intBitsToFloat(raw);
            int expected = (int) d;
            int got = FullSoftFloat.intValue(raw);
            assertEquals(expected, got);
            float f = Float.valueOf("" + got);
            int rawExpected = Float.floatToIntBits(f);
            int rawGot = FullSoftFloat.toRaw((int) f);
            assertEquals("i: " + i, rawExpected, rawGot);
        }
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            float d = r.nextFloat();
            int raw = Float.floatToIntBits(d);
            int expected = (int) d;
            int got = FullSoftFloat.intValue(raw);
            if (expected != got) {
                got = FullSoftFloat.intValue(raw);
                assertEquals(expected, got);
            }
        }
        for (int i = 0; i < 100_000; i++) {
            int raw = r.nextInt();
            float d = Float.intBitsToFloat(raw);
            int expected = (int) d;
            int got = FullSoftFloat.intValue(raw);
            if (expected != got) {
                System.out.println(d + " -> " + expected);
                System.out.println(FullSoftFloat.intValue(raw));
                assertEquals(expected, got);
            }
        }
    }

    @Test
    public void add() {
        int zero = FullSoftFloat.toRaw(0);
        assertEquals(0, FullSoftFloat.compare(zero, FullSoftFloat.add(zero, zero)));
        int one = FullSoftFloat.toRaw(1);
        assertEquals(0, FullSoftFloat.compare(zero, FullSoftFloat.subtract(one, one)));
        assertEquals(0, FullSoftFloat.compare(zero, FullSoftFloat.add(FullSoftFloat.negate(one), one)));
        Random r = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            float a = randomFloat(i, r);
            float b = randomFloat(i, r);
            float c = a + b;
            int rawA = Float.floatToIntBits(a);
            assertEquals(Float.isInfinite(a), FullSoftFloat.isInfinity(rawA));
            assertEquals(Float.isNaN(a), FullSoftFloat.isNaN(rawA));
            int rawB = Float.floatToIntBits(b);
            int rawC = Float.floatToIntBits(c);
            int expected = rawC;
            int got = FullSoftFloat.add(rawA, rawB);
            if (expected != got) {
                System.out.println("#" + i + ": " + a + " + " + b + " = " + c);
                System.out.println("got " + Float.intBitsToFloat(got));
                got = FullSoftFloat.add(rawA, rawB);
                assertEquals(expected, got);
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
            int got = FullSoftFloat.add(rawA, rawB);
            if (expected != got) {
                float e = Float.intBitsToFloat(expected);
                float g = Float.intBitsToFloat(got);
                if (Float.isNaN(e) && Float.isNaN(g)) {
                    // ok
                } else if (Math.abs(a) < 1e-35 || Math.abs(b) < 1e-35) {
                    // subnormals ar not fully correct yet
                } else {
                    System.out.println("#" + i + ": " + a + " + " + b + " = " + c);
                    System.out.println("got " + Float.intBitsToFloat(got));
                    System.out.println("expected " + Long.toBinaryString(expected));
                    System.out.println("got      " + Long.toBinaryString(got));
                    got = FullSoftFloat.add(rawA, rawB);
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
            int got = FullSoftFloat.multiply(rawA, rawB);
            if (expected != got) {
                if (Math.abs(expected - got) < 3) {
                    // ok
                } else {
                    System.out.println("#" + i + ": " + a + " * " + b + " = " + c);
                    System.out.println("got " + Float.intBitsToFloat(got));
                    got = FullSoftFloat.multiply(rawA, rawB);
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
            int got = FullSoftFloat.multiply(rawA, rawB);
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
                    got = FullSoftFloat.multiply(rawA, rawB);
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
            int got = FullSoftFloat.divide(rawA, rawB);
            if (expected != got) {
                if (Math.abs(expected - got) < 3) {
                    // ok
                } else {
                    System.out.println("#" + i + ": " + a + " / " + b + " = " + c);
                    System.out.println("got " + Float.intBitsToFloat(got));
                    got = FullSoftFloat.divide(rawA, rawB);
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
            int got = FullSoftFloat.divide(rawA, rawB);
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
                    got = FullSoftFloat.divide(rawA, rawB);
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
                int got = Integer.signum(FullSoftFloat.compare(rawA, rawB));
                if (expected != got) {
                    got = FullSoftFloat.compare(rawA, rawB);
                    assertEquals("FullSoftFloat.compare(rawA, rawB) for a: " + a + " (raw bits: " + Long.toHexString(rawA) +
                            ") b: " + b + " (raw bits: " + Long.toHexString(rawB) +
                            ") expected: " + expected + " got: " + got, expected, got);
                }
                float c = a + b;
                expected = Float.floatToIntBits(c);
                got = FullSoftFloat.add(rawA, rawB);
                if (expected != got) {
                    if (Float.isNaN(Float.intBitsToFloat(got)) && Float.isNaN(c)) {
                        // ok
                    } else if (Math.abs(expected - got) < 3) {
                        // ok
                    } else {
                        System.out.println(a + " + " + b + " = " + c);
                        System.out.println("got " + Float.intBitsToFloat(got));
                        got = FullSoftFloat.divide(rawA, rawB);
                        assertEquals(expected, got);
                    }
                }
                c = a - b;
                expected = Float.floatToIntBits(c);
                got = FullSoftFloat.subtract(rawA, rawB);
                if (expected != got) {
                    if (Float.isNaN(Float.intBitsToFloat(got)) && Float.isNaN(c)) {
                        // ok
                    } else if (Math.abs(expected - got) < 3) {
                        // ok
                    } else {
                        System.out.println(a + " - " + b + " = " + c);
                        System.out.println("got " + Float.intBitsToFloat(got));
                        got = FullSoftFloat.divide(rawA, rawB);
                        assertEquals(expected, got);
                    }
                }
                c = a / b;
                expected = Float.floatToIntBits(c);
                got = FullSoftFloat.divide(rawA, rawB);
                if (expected != got) {
                    if (Float.isNaN(Float.intBitsToFloat(got)) && Float.isNaN(c)) {
                        // ok
                    } else if (Math.abs(expected - got) < 3) {
                        // ok
                    } else {
                        System.out.println(a + " / " + b + " = " + c);
                        System.out.println("got " + Float.intBitsToFloat(got));
                        got = FullSoftFloat.divide(rawA, rawB);
                        assertEquals(expected, got);
                    }
                }
                c = a * b;
                expected = Float.floatToIntBits(c);
                got = FullSoftFloat.multiply(rawA, rawB);
                if (expected != got) {
                    if (Float.isNaN(Float.intBitsToFloat(got)) && Float.isNaN(c)) {
                        // ok
                    } else if (Math.abs(expected - got) < 3) {
                        // ok
                    } else {
                        System.out.println(a + " * " + b + " = " + c);
                        System.out.println("got " + Float.intBitsToFloat(got));
                        got = FullSoftFloat.divide(rawA, rawB);
                        assertEquals(expected, got);
                    }
                }
                int comp = Float.compare(a, b);
                got = FullSoftFloat.compare(rawA, rawB);
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
            int got = Integer.signum(FullSoftFloat.compare(rawA, rawB));
            if (expected != got) {
                System.out.println("#" + i + ": " + a + " comp " + b + ": " + expected);
                got = FullSoftFloat.compare(rawA, rawB);
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
            int got = Integer.signum(FullSoftFloat.compare(rawA, rawB));
            int expected = Integer.signum(Float.compare(a, b));
            if (expected != got) {
                System.out.println("#" + i + ": " + a + " comp " + b + ": " + expected);
                got = FullSoftFloat.compare(rawA, rawB);
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
