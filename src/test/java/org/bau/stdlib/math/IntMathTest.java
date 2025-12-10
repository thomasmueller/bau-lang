package org.bau.stdlib.math;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
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
    public void factorial() {
        long f = 1;
        for (int i = 0; i < 70;) {
            assertEquals(i + "!", f, IntMath.factorial(i));
            i++;
            long next = f * i;
            if (next < f) {
                f = -1;
            } else {
                f = next;
            }
        }
    }

    @Test
    public void sqrtIntTest() {
        for (long x = 0; x <= Integer.MAX_VALUE; x += 1000) {
            int expected = (int) Math.sqrt(x);
            int got = IntMath.sqrtInt((int) x);
            if (expected != got) {
                got = IntMath.sqrtInt((int) x);
                assertEquals("sqrt " + x, expected, got);
            }
        }
        Random r = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            int x = r.nextInt();
            int expected = (int) Math.sqrt(x);
            int got = IntMath.sqrtInt((int) x);
            if (expected != got) {
                got = IntMath.sqrtInt((int) x);
                assertEquals("sqrt " + x, expected, got);
            }
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

    @Test
    public void greatestCommonDivisor() {
        Random r = new Random(1);
        for (int i = 0; i < 1_000; i++) {
            long x = r.nextLong();
            long y = r.nextLong();
            BigInteger x1 = BigInteger.valueOf(x);
            BigInteger y1 = BigInteger.valueOf(y);
            BigInteger exp = x1.gcd(y1);
            long got = IntMath.greatestCommonDivisor(x, y);
            assertEquals(exp.longValue(), got);
        }
    }

    @Test
    public void leastCommonMultiple() {
        assertEquals(42, IntMath.leastCommonMultiple(6, 21));
        assertEquals(42, IntMath.leastCommonMultiple(21, 6));
        assertEquals(42, IntMath.leastCommonMultiple(6, 21));
        assertEquals(0, IntMath.leastCommonMultiple(0, 0));
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

    private static short divide32by16(short x1, short x0, short y) {
        if (y == 0) {
            // division by 0
            return ~0;
        }
        if ((y & 0xffff) <= (x1 & 0xffff)) {
            // overflow
            return ~0;
        }
        if (x1 == 0) {
            return (short) ((x0 & 0xffff) / y);
        }
        int shift = (byte) (IntMath.countLeadingZeros(y & 0xffff) - 48);
        y <<= shift;
        short mask = (short) 0xff;
        short u = (short) (((x1 & 0xffff) << shift) | (x0 >>> (16 - shift)));
        short un = (short) ((x0 & 0xffff) << shift);
        short un1 = (short) ((un & 0xffff) >>> 8);
        short un0 = (short) (un & mask);
        short y1 = (short) ((y >>> 8) & 0xff);
        short y0 = (short) (y & mask);
        short q1 = (short) ((u & 0xffff) / (y1 & 0xff));
        short rhat = (short) ((u & 0xffff) - ((q1 & 0xffff) * (y1 & 0xff)));
        while (q1 >>> 8 > 0
                || (((q1 & 0xffff) * y0) & 0xffff) > (((rhat & 0xffff) << 8) + un1)) {
            q1--;
            rhat += y1;
            if (rhat >>> 8 > 0) {
                break;
            }
        }
        short v = (short) (((u & 0xffff) << 8) + un1 - (q1 & 0xffff) * (y & 0xffff));
        short q0 = (short) ((v & 0xffff) / (y1 & 0xff));
        rhat = (short) ((v & 0xffff) - ((q0 & 0xffff) * (y1 & 0xff)));
        while (q0 >>> 8 > 0
                || (((q0 & 0xffff) * y0) & 0xffff) > (((rhat & 0xffff) << 8) + un0)) {
            q0--;
            rhat += y1;
            if (rhat >>> 8 > 0) {
                break;
            }
        }
        return (short) ((((q1 & 0xffff) << 8) & 0xffff) + (q0 & 0xffff));
    }

    @Test
    public void divideHighLowShortTest() {
        for (int a = 1; a <= Short.MAX_VALUE; a += 311) {
            // System.out.println("a=" + a);
            for (int b = 1; b <= Short.MAX_VALUE; b += 311) {
                for (int c = 1; c <= Short.MAX_VALUE; c += 311) {
                    int x = (a << 16) | b;
                    int y = c;
                    int expected = x / y;
                    if (expected >= 0x10000) {
                        expected = -1;
                    }
                    short z = divide32by16((short) a, (short) b, (short) c);
                    assertEquals((short) expected, z);
                }
            }
        }
    }

    @Test
    public void divideHighLowTest() {
        Random r = new Random(1);
        for (int ai = 0; ai < 50; ai++) {
            long a = randomLong(r, ai);
             for (int bi = 0; bi < 50; bi++) {
                long b = randomLong(r, bi);
                for (int ci = 0; ci < 50; ci++) {
                    long c = randomLong(r, ci);
                    BigInteger xa = BigInteger.valueOf(a >>> 32).shiftLeft(32).add(BigInteger.valueOf(a & 0xffffffffL));
                    BigInteger xb = BigInteger.valueOf(b >>> 32).shiftLeft(32).add(BigInteger.valueOf(b & 0xffffffffL));
                    BigInteger x = xa.shiftLeft(64).or(xb);
                    BigInteger y = BigInteger.valueOf(c >>> 32).shiftLeft(32).add(BigInteger.valueOf(c & 0xffffffffL));
                    long exp = 0;
                    if (c == 0) {
                        exp = ~0;
                    } else {
                        BigInteger expected = x.divide(y);
                        if (expected.bitLength() > 64) {
                            expected = BigInteger.ONE.negate();
                        }
                        exp = expected.longValue();
                    }
                    long z = IntMath.divide128Unsigned(a, b, c);
                    if (exp != z) {
                        z = IntMath.divide128Unsigned(a, b, c);
                    }
                    assertEquals(exp, z);
                }
            }
        }
    }

    private static long randomLong(Random r, int idx) {
        switch (idx) {
        case 0:
            return 0;
        case 1:
            return 1;
        case 2:
            return -1;
        case 3:
            return Long.MAX_VALUE;
        case 4:
            return Long.MIN_VALUE;
        case 5:
            return 1L << r.nextInt(63);
        case 6:
            return ~(1L << r.nextInt(63));
        }
        return r.nextLong();
    }

}
