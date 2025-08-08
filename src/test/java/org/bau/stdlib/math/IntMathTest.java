package org.bau.stdlib.math;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class IntMathTest {

    @Test
    public void signum() {
        Random r = new Random(0);
        for (int i = 0; i < 1_000_000; i++) {
            long x = r.nextLong();
            assertEquals(Long.signum(x), IntMath.signum(x));
        }
    }

    @Test
    public void bitOperations() {
        bitOperations(0);
        bitOperations(Long.MAX_VALUE);
        bitOperations(Long.MIN_VALUE);
        for (int i = 0; i < 63; i++) {
            bitOperations(1L << i);
            bitOperations((1L << i) - 1);
        }
        Random r = new Random(0);
        for (int i = 0; i < 1_000_000; i++) {
            long x = r.nextLong();
            bitOperations(x);
        }
    }

    private static void bitOperations(long x) {
        assertEquals(Long.bitCount(x), IntMath.bitCount(x));
        assertEquals(Long.numberOfLeadingZeros(x), IntMath.countLeadingZeros(x));
        assertEquals(Long.numberOfTrailingZeros(x), IntMath.countTrailingZeros(x));
        assertEquals(Long.reverse(x), IntMath.reverseBits(x));
        assertEquals(Long.reverseBytes(x), IntMath.reverseBytes(x));
        assertEquals(Long.lowestOneBit(x), IntMath.lowestBitSet(x));
        assertEquals(Long.highestOneBit(x), IntMath.highestBitSet(x));
        for (int i = 0; i < 64; i++) {
            assertEquals(Long.rotateLeft(x, i), IntMath.rotateLeft(x, i));
            assertEquals(Long.rotateRight(x, i), IntMath.rotateRight(x, i));
            assertEquals(x >> i, IntMath.shiftRightArithmetic(x, i));
        }
    }

    @Test
    public void multiplyHigh() {
        Random r = new Random(0);
        for (int i = 0; i < 1_000_000; i++) {
            long x = r.nextLong();
            long y = r.nextLong();
            long expectedUnsigned = Math.unsignedMultiplyHigh(x, y);
            long gotUnsigned = IntMath.multiplyHighUnsigned(x, y);
            assertEquals(expectedUnsigned, gotUnsigned);
            long expectedSigned = Math.multiplyHigh(x, y);
            long gotSigned = IntMath.multiplyHighSigned(x, y);
            assertEquals(expectedSigned, gotSigned);
        }
    }

    @Test
    public void twoParameterOperations() {
        long[] special = { Long.MIN_VALUE, Long.MIN_VALUE + 1, -2, -1, 0, 1, 2, Long.MAX_VALUE - 1, Long.MAX_VALUE };
        for (long x : special) {
            for (long y : special) {
                twoParameterOperations(x, y);
            }
        }
        Random r = new Random(0);
        for (int i = 0; i < 1_000_000; i++) {
            long x = r.nextLong();
            long y = r.nextLong();
            twoParameterOperations(x, y);
        }
    }

    private static void twoParameterOperations(long x, long y) {
        long expectedMin = Math.min(x, y);
        long gotMin = IntMath.min(x, y);
        assertEquals(expectedMin, gotMin);
        long expectedMax = Math.max(x, y);
        long gotMax = IntMath.max(x, y);
        assertEquals(expectedMax, gotMax);
    }

    @Test
    public void multiplyHighShort() {
        for (int x = Short.MIN_VALUE; x <= Short.MAX_VALUE; x += 31) {
            for (int y = Short.MIN_VALUE; y <= Short.MAX_VALUE; y += 11) {
                short expectedUnsigned = (short) ((((x & 0xffff) * (y & 0xffff)) >>> 16) & 0xffff);
                short gotUnsigned = multiplyHighShortUnsigned((short) x, (short) y);
                if (expectedUnsigned != gotUnsigned) {
                    System.out.println("x: " + Integer.toHexString(x));
                    System.out.println("y: " + Integer.toHexString(y));
                    System.out.println("z: " + Integer.toHexString(expectedUnsigned));
                    System.out.println(x + " * " + y + " = " + expectedUnsigned);
                    System.out.println("got " + gotUnsigned);
                    assertEquals(expectedUnsigned, gotUnsigned);
                }
                short expectedSigned = (short) (((x * y) >> 16) & 0xffff);
                short gotSigned = multiplyHighShortSigned((short) x, (short) y);
                if (expectedSigned != gotSigned) {
                    System.out.println("x: " + Integer.toHexString(x));
                    System.out.println("y: " + Integer.toHexString(y));
                    System.out.println("z: " + Integer.toHexString(expectedSigned));
                    System.out.println(x + " * " + y + " = " + expectedSigned);
                    System.out.println("got " + gotSigned);
                    assertEquals(expectedSigned, gotSigned);
                }
            }
        }
    }

    @Test
    public void nextPowerOf2Test() {
        for (int i = -10; i < 10; i++) {
            long x = i;
            assertEquals(nextPowerOf2Loop(x), IntMath.nextPowerOf2(x));
            x = Long.MAX_VALUE + x;
            assertEquals(nextPowerOf2Loop(x), IntMath.nextPowerOf2(x));
        }
        Random r = new Random(0);
        for (int i = 0; i < 10_000; i++) {
            long x = r.nextLong();
            assertEquals(nextPowerOf2Loop(x), IntMath.nextPowerOf2(x));
        }
    }

    private static long nextPowerOf2Loop(long x) {
        long result = 1;
        while (result > 0 && result < x) {
            result += result;
        }
        return result;
    }

    public static short multiplyHighShortSigned(short x, short y) {
        short xh = (short) (x >> 8);
        short xl = (short) (x & 0xff);
        short yh = (short) (y >> 8);
        short yl = (short) (y & 0xff);
        short a = (short) (((xl * yl) >>> 8) + (xh * yl));
        short b = (short) ((a & 0xff) + (xl * yh));
        return (short) ((a >> 8) + (b >> 8) + (xh * yh));
    }

    public static short multiplyHighShortUnsigned(short x, short y) {
        short xh = (short) ((x >>> 8) & 0xff);
        short xl = (short) (x & 0xff);
        short yh = (short) ((y >>> 8) & 0xff);
        short yl = (short) (y & 0xff);
        short a = (short) (((xl * yl) >>> 8) + (xh * yl));
        short b = (short) ((a & 0xff) + (xl * yh));
        return (short) (((a >>> 8) & 0xff) + ((b >>> 8) & 0xff) + (xh * yh));
    }

}
