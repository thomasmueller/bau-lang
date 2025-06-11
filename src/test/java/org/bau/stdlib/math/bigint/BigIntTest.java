package org.bau.stdlib.math.bigint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Test;

public class BigIntTest {

    private static long randomLong(Random r, int i) {
        if (i == 0) {
            return 0;
        } else if (i < 20) {
            return r.nextInt(10) - 5;
        }
        if (r.nextBoolean()) {
            return r.nextLong(1L << 60);
        }
        return r.nextInt();
    }

    private static BigInteger randomBigInteger(Random r) {
        return randomBigInteger(r, 0, r.nextInt(50));
    }

    private static BigInteger randomBigInteger(Random r, int minSize, int maxSize) {
        int len = minSize + r.nextInt(1 + maxSize - minSize);
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < len; i++) {
            buff.append(r.nextInt(10));
        }
        if (buff.isEmpty()) {
            buff.append('0');
        }
        return new BigInteger(buff.toString());
    }

    @Test
    public void shift() {
        BigInt a = BigInt.valueOf(1);
        for (int j = 0; j < 1000; j++) {
            BigInt b = a.shiftLeft(1);
            BigInt c = b.shiftRight(1);
            assertEquals(a, c);
            b = c;
        }
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            BigInteger ai = randomBigInteger(r);
            BigInt aa = BigInt.valueOf(ai.toString());
            int shift = r.nextInt(100);

            BigInteger li = ai.shiftRight(shift);
            BigInt la = aa.shiftRight(shift);
            assertEquals(li.toString(), la.toString());
            BigInteger ri = ai.shiftRight(shift);
            BigInt ra = aa.shiftRight(shift);
            assertEquals(ri.toString(), ra.toString());
        }
    }

    @Test
    public void add() {
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            long a = randomLong(r, i);
            long b = randomLong(r, i);
            BigInt aa = BigInt.valueOf(a);
            BigInt bb = BigInt.valueOf(b);
            if (r.nextBoolean()) {
                aa = aa.negate();
                a = -a;
            }
            if (r.nextBoolean()) {
                bb = bb.negate();
                b = -b;
            }
            BigInt cc = aa.add(bb);
            assertEquals(a + "+" + b, "" + (a + b), cc.toString());
            BigInteger ai = BigInteger.valueOf(a);
            BigInteger bi = BigInteger.valueOf(b);
            int sa = r.nextInt(10);
            int sb = r.nextInt(10);
            ai = ai.shiftLeft(sa);
            aa = aa.shiftLeft(sa);
            bi = bi.shiftLeft(sb);
            bb = bb.shiftLeft(sb);
            BigInteger ci = ai.add(bi);
            cc = aa.add(bb);
            if (!ci.toString().equals(cc.toString())) {
                cc = aa.add(bb);
                assertEquals(ai + "+" + bi, ci.toString(), cc.toString());
            }
        }
    }

    @Test
    public void compare() {
        Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            long a = randomLong(r, i);
            long b = randomLong(r, i);
            BigInt aa = BigInt.valueOf(a);
            BigInt bb = BigInt.valueOf(b);
            if (r.nextBoolean()) {
                aa = aa.negate();
                a = -a;
            }
            if (r.nextBoolean()) {
                bb = bb.negate();
                b = -b;
            }
            assertEquals(Long.compare(Math.abs(a), b), aa.abs().compareTo(bb));
            assertEquals(0, aa.compareTo(aa));
            assertEquals(0, bb.compareTo(bb));
            assertEquals(Long.compare(a, b), aa.compareTo(bb));
            assertEquals(Long.compare(b, a), bb.compareTo(aa));
            if (a == b) {
                assertTrue(aa.equals(bb));
                assertTrue(bb.equals(aa));
            } else {
                assertFalse(aa.equals(bb));
                assertFalse(bb.equals(aa));
            }
        }
    }

    @Test
    public void intValue() {
        assertEquals(0, BigInt.valueOf(0).intValue());
        assertEquals(100, BigInt.valueOf(100).intValue());
        assertEquals(-100, BigInt.valueOf(-100).intValue());
    }

    @Test
    public void subtract() {
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            long a = randomLong(r, i);
            long b = randomLong(r, i);
            BigInt aa = BigInt.valueOf(a);
            BigInt bb = BigInt.valueOf(b);
            if (r.nextBoolean()) {
                aa = aa.negate();
                a = -a;
            }
            if (r.nextBoolean()) {
                bb = bb.negate();
                b = -b;
            }
            BigInt cc = aa.subtract(bb);
            assertEquals(a + "-" + b, "" + (a - b), cc.toString());
        }
    }

    @Test
    public void multiply() {
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            long a = randomLong(r, i);
            long b = randomLong(r, i);
            BigInt aa = BigInt.valueOf(a);
            BigInt bb = BigInt.valueOf(b);
            if (r.nextBoolean()) {
                aa = aa.negate();
                a = -a;
            }
            if (r.nextBoolean()) {
                bb = bb.negate();
                b = -b;
            }
            BigInt cc = aa.multiply(bb);
            BigInteger test = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b));
            assertEquals(a + "*" + b, test.toString(), cc.toString());
        }
    }

    @Test
    public void divideReminder() {
        BigInt allOnes = BigInt.valueOf(1).shiftLeft(128).subtract(BigInt.valueOf(1));
        BigInt oneOne = BigInt.valueOf(1).shiftLeft(190);
        BigInt div = oneOne.divide(allOnes);
        BigInteger all1 = BigInteger.valueOf(1).shiftLeft(128).subtract(BigInteger.valueOf(1));
        BigInteger one1 = BigInteger.valueOf(1).shiftLeft(190);
        BigInteger divExp = one1.divide(all1);
        assertEquals(divExp.toString(), div.toString());

        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            long a = randomLong(r, i);
            long b = randomLong(r, i);
            BigInt aa = BigInt.valueOf(a);
            BigInt bb = BigInt.valueOf(b);
            if (r.nextBoolean()) {
                aa = aa.negate();
                a = -a;
            }
            if (r.nextBoolean()) {
                bb = bb.negate();
                b = -b;
            }
            BigInt cc = aa.divide(bb);
            BigInteger test;
            try {
                test = BigInteger.valueOf(a).divide(BigInteger.valueOf(b));
            } catch (ArithmeticException e) {
                test = null;
            }
            if (test == null) {
                assertTrue(cc == null);
            } else {
                if (!test.toString().equals(cc.toString())) {
                    cc = aa.divide(bb);
                    assertEquals(a + "/" + b, test.toString(), cc.toString());
                }
                BigInt ccMod = aa.remainder(bb);
                BigInteger testMod = BigInteger.valueOf(a).remainder(BigInteger.valueOf(b));
                assertEquals(a + "%" + b, testMod.toString(), ccMod.toString());
            }
        }
    }

    @Test
    public void largeOps() {
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            BigInteger ai = randomBigInteger(r);
            BigInteger bi = randomBigInteger(r);
            BigInt aa = BigInt.valueOf(ai.toString());
            BigInt bb = BigInt.valueOf(bi.toString());
            assertEquals(ai.toString(), aa.toString());
            assertEquals(bi.toString(), bb.toString());
            BigInteger ci;
            BigInt cc;
            BigInteger di;
            BigInt dc;
            ci = ai.add(bi);
            cc = aa.add(bb);
            assertEquals(ci.toString(), cc.toString());
            ci = ai.subtract(bi);
            cc = aa.subtract(bb);
            assertEquals(ci.toString(), cc.toString());
            ci = ai.multiply(bi);
            cc = aa.multiply(bb);
            assertEquals(ci.toString(), cc.toString());
            if (bi.signum() != 0) {
                di = ci.divide(bi);
                dc = cc.divide(bb);
                assertEquals(di.toString(), dc.toString());
            }
        }
    }

    @Test
    public void convert() {
        assertEquals("" + Integer.MIN_VALUE, BigInt.valueOf(Integer.MIN_VALUE).toString());
        assertEquals("" + Integer.MAX_VALUE, BigInt.valueOf(Integer.MAX_VALUE).toString());
        assertEquals("" + Long.MIN_VALUE, BigInt.valueOf(Long.MIN_VALUE).toString());
        assertEquals("" + Long.MAX_VALUE, BigInt.valueOf(Long.MAX_VALUE).toString());
    }

    public void performanceMultiply() {
        long baseTime = 0;
        long myTime = 0;
        Random r = new Random(1);
        for(int test = 0; test < 5; test++) {
            for(int size = 1; size <= 100; size = (int) (1 + size * 1.5)) {
                for (int i = 0; i < 100 / size; i++) {
                    BigInteger ai = randomBigInteger(r, size, size * 2);
                    BigInteger bi = randomBigInteger(r, size, size * 2);
                    BigInt aa = BigInt.valueOf(ai.toString());
                    BigInt bb = BigInt.valueOf(bi.toString());
                    assertEquals(ai.toString(), aa.toString());
                    assertEquals(bi.toString(), bb.toString());
                    BigInteger ci = ai;
                    BigInt cc = aa;
                    long start;
                    start = System.nanoTime();
                    for (int j = 0; j < 10; j++) {
                        cc = aa.multiply(bb);
                    }
                    myTime += System.nanoTime() - start;
                    if (i == 0) {
                        log("  eg. " + ai + " * " + bi + " = " + ci);
                    }
                    start = System.nanoTime();
                    for (int j = 0; j < 10; j++) {
                        ci = ai.multiply(bi);
                    }
                    baseTime += System.nanoTime() - start;
                    assertEquals(ci.toString(), cc.toString());
                }
                log("size " + size + " factor " + ((double) myTime / baseTime) + " myTime " + myTime + " base " + baseTime);
            }
        }
    }

    private void log(String msg) {
        System.out.println(msg);
    }

}
