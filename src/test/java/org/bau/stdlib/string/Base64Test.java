package org.bau.stdlib.string;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class Base64Test {

    @Test
    public void test() {
        assertEquals("", new String(Base64.encode(new byte[] {})));
        assertEquals("QQ==", new String(Base64.encode("A".getBytes())));
        assertEquals("QUI=", new String(Base64.encode("AB".getBytes())));
        assertEquals("QUJD", new String(Base64.encode("ABC".getBytes())));
        assertEquals("QUJDRA==", new String(Base64.encode("ABCD".getBytes())));
        assertEquals("", new String(Base64.decode(new byte[] {})));
        assertEquals("A", new String(Base64.decode("QQ==".getBytes())));
        assertEquals("AB", new String(Base64.decode("QUI=".getBytes())));
        assertEquals("ABC", new String(Base64.decode("QUJD".getBytes())));
        assertEquals("ABCD", new String(Base64.decode("QUJDRA==".getBytes())));
        int len = 100;
        test(len);
    }

    private static void test(int len) {
        Random random = new Random(10);
        long time = System.nanoTime();
        byte[] bin = new byte[len];
        random.nextBytes(bin);
        for (int i = 0; i < len; i++) {
            byte[] dec;
            byte[] enc = Base64.encode(bin);
            dec = Base64.decode(enc);
            test(bin, dec);
        }
        time = System.nanoTime() - time;
        System.out.println("time=" + TimeUnit.NANOSECONDS.toMillis(time));
    }

    private static void test(byte[] in, byte[] out) {
        if (in.length != out.length) {
            throw new RuntimeException("Length error");
        }
        for (int i = 0; i < in.length; i++) {
            if (in[i] != out[i]) {
                throw new RuntimeException("Error at " + i);
            }
        }
    }

    @Test
    public void benchmark() {
        for(int i=0; i<10; i++) {
            code();
        }
    }

    @Test
    public void code() {
        for(int i=0; i<256; i++) {
            assertEquals(codeWithoutBranch(i), Base64.code(i));
        }
    }

    @Test
    public void testInRange() {
        assertEquals(0, isInRange(1, 2, 20));
        assertEquals(-1, isInRange(2, 2, 20));
        assertEquals(-1, isInRange(10, 2, 20));
        assertEquals(-1, isInRange(20, 2, 20));
        assertEquals(0, isInRange(21, 2, 20));
        // swapped range
        assertEquals(0, isInRange(1, 20, 2));
        assertEquals(0, isInRange(2, 20, 2));
        assertEquals(0, isInRange(10, 20, 2));
        assertEquals(0, isInRange(20, 20, 2));
        assertEquals(0, isInRange(21, 20, 2));

        // negative
        assertEquals(0, isInRange(-1, 2, 2));
        assertEquals(0, isInRange(-1, -2, -2));
        assertEquals(-1, isInRange(-2, -2, -2));
        assertEquals(-1, isInRange(-2, -3, -2));
        // negative & swapped range
        assertEquals(0, isInRange(-2, -2, -3));
    }

    static int isInRange(int positiveX, int minValue, int maxValue) {
        return ~(((positiveX - minValue) | (maxValue - positiveX)) >> 31);
    }

    static int isEqual(int positiveX, int exactValue) {
        return (((positiveX ^ exactValue) - 1) >> 31);
    }

    @Test
    public void testEqual() {
        assertEquals(0, isEqual(1, 2));
        assertEquals(-1, isEqual(2, 2));
        assertEquals(0, isEqual(3, 2));
        // negative
        assertEquals(-1, isEqual(-1, 0));
    }

    static byte codeWithoutBranch(int i) {
        return (byte) ((isInRange(i, 0, 25) & ('A' + 1 + i))
                + (isInRange(i, 26, 51) & ('a' + 1 + i - 26))
                + (isInRange(i, 52, 61) & ('0' + 1 + i - 52))
                + (isEqual(i, 62) & ('+' + 1))
                + (isEqual(i, 63) & ('/' + 1)) - 1);
    }
}
