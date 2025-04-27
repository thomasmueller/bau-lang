package org.bau.stdlib.collections;

import static org.junit.Assert.assertEquals;

import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

public class BitFieldTest {

    public void benchmark() {
        // time 368 sum 279209790387276 Java / primeSumBitSet
        // time 368 sum 279209790387276 Java / primeSumBitField
        // time 365 sum 279209790387276 Java / primeSum2
        // time 323 sum 279209790387276 Bau / primeSum
        // time 324 sum 279209790387276 Bau / primeSum2
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            long sum = primeSum2(100_000_000);
            long time = System.currentTimeMillis() - start;
            System.out.println("time " + time + " sum " + sum);
        }
    }

    @Test
    public void random() {
        BitField test = new BitField(100);
        BitSet good = new BitSet(100);
        Random r = new Random(1);
        for (int i = 0; i < 1_000_000; i++) {
            int index = r.nextInt(100);
            switch (r.nextInt(10)) {
            case 0:
                test.set(index);
                good.set(index);
                break;
            case 1:
                test.clear(index);
                good.clear(index);
                break;
            case 2:
                assertEquals(good.get(index), test.get(index));
                break;
            case 3:
                test.flip(index);
                good.flip(index);
                break;
            }
        }
    }

    @Test
    public void primeSieve() {
        assertEquals(37_550_402_023L, primeSumBitSet(1_000_000));
        assertEquals(37_550_402_023L, primeSumBitField(1_000_000));
    }

    public long primeSumBitSet(int limit) {
        BitField sieve = new BitField(limit + 1);
        for (int i = 3; i * i <= limit; i += 2) {
            if (!sieve.get(i)) {
                for (int j = i * i; j <= limit; j += i) {
                    sieve.set(j);
                }
            }
        }
        long sum = 2;
        for (int i = 3; i <= limit; i += 2) {
            if (!sieve.get(i)) {
                sum += i;
            }
        }
        return sum;
    }

    public long primeSumBitField(int limit) {
        BitField sieve = new BitField(limit + 1);
        for (int i = 3; i * i <= limit; i += 2) {
            if (!sieve.get(i)) {
                for (int j = i * i; j <= limit; j += i) {
                    sieve.set(j);
                }
            }
        }
        long sum = 2;
        for (int i = 3; i <= limit; i += 2) {
            if (!sieve.get(i)) {
                sum += i;
            }
        }
        return sum;
    }

    public long primeSum2(int limit) {
        int len = (limit + 1 + 63) / 64;
        long[] sieve = new long[len];
        for (int i = 3; i * i <= limit; i += 2) {
            if ((sieve[i >>> 6] >> (i & 0x3f) & 1) == 0) {
                for (int j = i * i; j <= limit; j += i) {
                    sieve[j >>> 6] |= 1L << (j & 0x3f);
                }
            }
        }
        long sum = 2;
        for (int i = 3; i <= limit; i += 2) {
            if ((sieve[i >>> 6] >> (i & 0x3f) & 1) == 0) {
                sum += i;
            }
        }
        return sum;
    }

}
