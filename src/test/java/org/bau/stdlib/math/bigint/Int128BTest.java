package org.bau.stdlib.math.bigint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Test;

public class Int128BTest {

    private final static BigInteger MASK = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    private final static BigInteger MIN_VALUE = BigInteger.ONE.shiftLeft(127).negate();
    private final static BigInteger MAX_VALUE = BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE);

    private static long randomLong(Random r, int i) {
        if (i == 0) {
            return 0;
        } else if (i < 20) {
            return r.nextInt(10) - 5;
        }
        if (r.nextBoolean()) {
            return r.nextLong(1L << 62);
        }
        return r.nextInt();
    }

    private static BigInteger randomBigInteger128(Random r) {
        switch (r.nextInt(20)) {
        case 0:
            return BigInteger.ZERO;
        case 1:
            return BigInteger.ONE;
        case 2:
            return BigInteger.TWO;
        case 3:
            return BigInteger.TEN;
        case 4:
            return BigInteger.ONE.negate();
        case 5:
            return BigInteger.TWO.negate();
        case 6:
            return BigInteger.ONE.shiftLeft(127).negate();
        case 7:
            return BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE).negate();
        }
        long a = r.nextLong() >>> 1;
        long b = r.nextLong() >>> 32;
        long c = r.nextLong() >>> 32;
        BigInteger aa = BigInteger.valueOf(a);
        aa = aa.shiftLeft(32);
        aa = aa.add(BigInteger.valueOf(b));
        aa = aa.shiftLeft(32);
        aa = aa.add(BigInteger.valueOf(c));
        if (r.nextBoolean()) {
            aa = aa.negate();
        }
        return aa;
    }

    @Test
    public void toStringAndParse() {
        Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            long a = randomLong(r, i);
            Int128B aa = Int128B.valueOf(a);
            assertEquals("" + a, aa.toString());
            Int128B bb = Int128B.valueOf("" + a);
            assertEquals("" + a, bb.toString());
        }
        for (int i = 0; i < 1000; i++) {
            BigInteger a = randomBigInteger128(r);
            Int128B aa = Int128B.valueOf(a.toString());
            assertEquals(a.toString(), aa.toString());
        }
    }

    @Test
    public void shift() {
        Random r = new Random(1);
        for (int j = -100; j < 100; j++) {
            Int128B a = Int128B.valueOf(j);
            Int128B b = a.shiftLeft(100);
            Int128B c = b.shiftRightArithmetic(100);
            assertTrue(a.equals(c));
        }
        for (int i = 0; i < 1_000; i++) {
            BigInteger ai = randomBigInteger128(r);
            Int128B aa = Int128B.valueOf(ai.toString());
            int shift = r.nextInt(100);
            BigInteger li = shiftRightLogical(ai, shift);
            Int128B la = aa.shiftRight(shift);
            assertEquals(li.toString(), la.toString());
            BigInteger ri = shiftRightLogical(ai, shift);
            Int128B ra = aa.shiftRight(shift);
            assertEquals(ri.toString(), ra.toString());
        }
    }

    private BigInteger shiftRightLogical(BigInteger ai, int shift) {
        if (shift == 0) {
            return ai;
        }
        BigInteger li;
        if (ai.signum() < 0) {
            BigInteger n = ai.add(BigInteger.ONE).negate();
            n = n.xor(MASK);
            li = n.shiftRight(shift);
        } else {
            li = ai.shiftRight(shift);
        }
        return li;
    }

    @Test
    public void add() {
        assertEquals("170141183460469231731687303715884105727", Int128B.MAX_VALUE.toString());
        assertEquals("-170141183460469231731687303715884105728", Int128B.MIN_VALUE.toString());
        assertEquals(Int128B.MIN_VALUE, Int128B.MAX_VALUE.add(Int128B.valueOf(1)));
        assertEquals(Int128B.MAX_VALUE, Int128B.MIN_VALUE.subtract(Int128B.valueOf(1)));
        Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            long a = randomLong(r, i);
            long b = randomLong(r, i);
            Int128B aa = Int128B.valueOf(a);
            Int128B bb = Int128B.valueOf(b);
            if (r.nextBoolean()) {
                aa = aa.negate();
                a = -a;

            }
            if (r.nextBoolean()) {
                bb = bb.negate();
                b = -b;
            }
            Int128B cc = aa.add(bb);
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
            Int128B aa = Int128B.valueOf(a);
            Int128B bb = Int128B.valueOf(b);
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
    public void subtract() {
        Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            long a = randomLong(r, i);
            long b = randomLong(r, i);
            Int128B aa = Int128B.valueOf(a);
            Int128B bb = Int128B.valueOf(b);
            if (r.nextBoolean()) {
                aa = aa.negate();
                a = -a;
            }
            if (r.nextBoolean()) {
                bb = bb.negate();
                b = -b;
            }
            Int128B cc = aa.subtract(bb);
            if (!("" + (a - b)).equals(cc.toString())) {
                cc = aa.subtract(bb);
            }
            assertEquals(a + "-" + b, "" + (a - b), cc.toString());
        }
    }

    @Test
    public void multiply() {
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
            BigInt cc = aa.multiply(bb);
            BigInteger test = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b));
            assertEquals(a + "*" + b, test.toString(), cc.toString());
        }
    }

    @Test
    public void divideRemainder() {
        Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            long a = randomLong(r, i);
            long b = randomLong(r, i);
            Int128B aa = Int128B.valueOf(a);
            Int128B bb = Int128B.valueOf(b);
            if (r.nextBoolean()) {
                aa = aa.negate();
                a = -a;
            }
            if (r.nextBoolean()) {
                bb = bb.negate();
                b = -b;
            }
            Int128B cc = aa.divide(bb);
            BigInteger test;
            if (b == 0) {
                assertEquals(Int128B.ZERO, cc);
            } else {
                test = BigInteger.valueOf(a).divide(BigInteger.valueOf(b));
                if (!test.toString().equals(cc.toString())) {
                    cc = aa.divide(bb);
                    assertEquals(a + "/" + b, test.toString(), cc.toString());
                }
                Int128B ccMod = aa.remainder(bb);
                BigInteger testMod = BigInteger.valueOf(a).remainder(BigInteger.valueOf(b));
                assertEquals(a + "%" + b, testMod.toString(), ccMod.toString());
            }
        }
    }

    @Test
    public void largeOps() {
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            BigInteger ai = randomBigInteger128(r);
            BigInteger bi = randomBigInteger128(r);
            Int128B aa = Int128B.valueOf(ai.toString());
            Int128B bb = Int128B.valueOf(bi.toString());
            assertEquals(ai.toString(), aa.toString());
            if (!(bi.toString().equals(bb.toString()))) {
                bb = Int128B.valueOf(bi.toString());
            }
            assertEquals(Integer.signum(ai.compareTo(bi)),
                    Integer.signum(aa.compareTo(bb)));
            assertEquals(bi.toString(), bb.toString());
            BigInteger ci;
            Int128B cc;
            ci = overflow(ai.add(bi));
            cc = aa.add(bb);
            assertEquals(ci.toString(), cc.toString());
            ci = overflow(ai.subtract(bi));
            cc = aa.subtract(bb);
            assertEquals(ci.toString(), cc.toString());
            ci = ai.multiply(bi);
            ci = overflow(ci);
            cc = aa.multiply(bb);
            assertEquals(ci.toString(), cc.toString());
            if (!"0".equals(bi.toString())) {
                ci = ai.divide(bi);
                ci = overflow(ci);
                cc = aa.divide(bb);
                assertEquals(ci.toString(), cc.toString());
            }
        }
    }

    private BigInteger overflow(BigInteger a) {
        if (a.compareTo(MAX_VALUE) > 0) {
            a = a.and(MASK);
        } else if (a.compareTo(MIN_VALUE) < 0) {
            a = a.negate().add(BigInteger.ONE);
            a = a.and(MASK);
            a = a.negate().add(BigInteger.ONE);
        }
        if (a.compareTo(MIN_VALUE) < 0) {
            a = a.add(BigInteger.ONE.shiftLeft(128));
        }
        if (a.compareTo(MAX_VALUE) > 0) {
            a = a.subtract(BigInteger.ONE.shiftLeft(128));
        }
        return a;
    }

}
