package org.bau.stdlib.malloc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

public class ThreeLevelMallocTest {

    public static int[] levelCount = new int[64];
    public static int maxMemoryUsage;
    public static int worstMemoryUsage;

    @Test
    public void sizeClasses() {
        // 0..32: linear 8..128 (>90%)
        // 33..50: log+1 bit 192..65536 (>99%)
        // 51..63: log 131072..536870912
        for (int i = 0; i < 64; i++) {
            long size;
            if (i < 32) {
                size = (i + 2) * 2;
            } else if (i < 49) {
                int j = i - 32;
                size = ((64 + (32L * (j % 2))) << (j / 2)) + 8;
            } else {
                size = (1L << (i - 34)) + 8;
            }
            int sc = sizeClass(size);
            int scm1 = sizeClass(size - 1);
            long rounded = roundUp(size);
            assertEquals(sc, sizeClass(rounded));
            rounded = roundUp(size + 1);
            assertTrue(rounded > size);
            int scp1 = sizeClass(rounded);
            assertEquals(i, sc);
            assertEquals(sc, scm1 + 1);
            assertEquals(Math.min(63, sc + 1), scp1);
            // System.out.println(i + ": usableBytes:" + ((size - 2) * 4) + " words:" + size + " c(n):" + sc + " c(n-1):" + scm1 + " c(round(n+1)):" + scp1);
        }
        int last = 0;
        for (int i = 4; i < 1_000_000; i += 4) {
            int sc = sizeClass(i);
            if (sc < last) {
                // System.out.println();
            }
            assertTrue(sc >= last);
            last = sc;
        }
    }

    static long roundUp(long size) {
        if (size <= 66) {
            size = ((size - 1) & ~1) + 2;
            return size;
        } else if (size <= 16392) {
            int round = ((1 << (63 - Long.numberOfLeadingZeros(size - 8))) - 1) >>> 1;
            size += round;
            return size;
        }
        long round = (1L << (63 - Long.numberOfLeadingZeros(size - 8))) - 1;
        size += round;
        return size;
    }

    static int sizeClass(long size) {
        if (size <= 66) {
            return (int) (size >>> 1) - 2;
        }
        int log2 = 63 - Long.numberOfLeadingZeros(size - 8);
        if (size <= 16392) {
            return 20 + 2 * log2 + (int) (((size - 8) << 1 >>> log2) ^ 2);
        }
        return Math.min(63, log2 + 34);
    }

    @Test
    public void testCase() {
        int totalSize = 1 * 1024 * 1024;
        ThreeLevelMalloc m = new ThreeLevelMalloc(totalSize);
        for (int i = 0; i < 1000; i++) {
            int b = m.malloc(12);
            int b2 = m.malloc(16);
            m.free(b);
            b = m.malloc(16);
            m.free(b);
            m.free(b2);
        }
    }

    @Test
    public void test() {
        long sumMemoryUsage = 0;
        int best = Integer.MAX_VALUE;
        int maxLoop = 10_000;
        int count = 10;
        for(int seed = 0; seed < count; seed++) {
            int fail = test(seed, maxLoop);
            if (fail < best) {
                // System.out.println("best: " + seed + " @" + fail);
                best = fail;
                maxLoop = best;
            }
            worstMemoryUsage = Math.max(worstMemoryUsage, maxMemoryUsage);
            sumMemoryUsage += maxMemoryUsage;
        }
        assertTrue(sumMemoryUsage >= 0);
        // System.out.println("calls " + Malloc.countCalls + " branch " + Malloc.countBranches);
        for (int i = 0; i < 64; i++) {
            // System.out.println(i + " " + levelCount[i]);
        }
        // Average memory usage 1446439 worst 2394128
        // System.out.println("Average memory usage " + sumMemoryUsage / count + " worst " + worstMemoryUsage);
    }

    public int test(int seed, int maxLoop) {
        int totalSize = 16 * 1024 * 1024;
        ThreeLevelMalloc m = new ThreeLevelMalloc(totalSize);
        int available = totalSize / 2;
        Random r = new Random(seed);
        int totalBlocks = 512;
        int[] blocks = new int[totalBlocks];
        int[] lengths = new int[totalBlocks];
        BitSet used = new BitSet(totalSize);
        maxMemoryUsage = 0;
        for (int i = 0; i < maxLoop; i++) {
            try {
                int pos = r.nextInt(totalBlocks);
                // if (i >= 7) {
                //     System.out.println("test");
                // }
                if (blocks[pos] != 0) {
                    int p = blocks[pos];
                    if (m.getByte(p) != (byte) p) {
                        return i;
                    }
                    if (m.getByte(p + (lengths[pos]/4) - 1) != (byte) lengths[pos]) {
                        return i;
                    }
                    m.free(p);
                    m.verify();
                    used.clear(blocks[pos], blocks[pos] + lengths[pos]);
                    available += lengths[pos];
                    blocks[pos] = 0;
                    lengths[pos] = 0;
                }
                int size = 0;
                int retries = 0;
                do {
                    size = randomAllocationSize(r, 128 * 1024);
                    if (retries++ > 100) {
                        size = 0;
                        break;
                    }
                } while (size > available);
                if (size > 0) {
                    int b = m.malloc(size);
                    int usage = b + size;
                    if (usage > maxMemoryUsage) {
                        maxMemoryUsage = usage;
                    }
                    if (b == 0) {
                        // OOME
                        continue;
                    }
                    m.putByte(b, (byte) b);
                    m.putByte(b + (size/4) - 1, (byte) size);
                    if (used.get(b, b + size).cardinality() != 0) {
                        return i;
                    }
                    used.set(b, b + size);
                    blocks[pos] = b;
                    lengths[pos] = size;
                    available -= size;
                }
                m.verify();
            } catch(Exception e) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static int randomAllocationSize(Random random, int maxSize) {
        int r = random.nextInt(1000);
        if (r < 350) {
            // 8–32 bytes
            return 8 + random.nextInt(25);
        } else if (r < 600) {
            // 33–64 bytes
            return 33 + random.nextInt(32);
        } else if (r < 750) {
            // 65–128 bytes
            return 65 + random.nextInt(64);
        } else if (r < 850) {
            // 129–256 bytes
            return 129 + random.nextInt(128);
        } else if (r < 930) {
            // 257–512 bytes
            return 257 + random.nextInt(256);
        } else if (r < 980) {
            // 513–1024 bytes
            return 513 + random.nextInt(512);
        } else if (r < 990) {
            // up to 4KB
            return 1025 + random.nextInt(3072);
        } else if (r < 995) {
            // up to 64KB
            return 4096 + random.nextInt(64 * 1024);
        }
        // up to maxSize
        return 64 * 1024 + random.nextInt(maxSize);
    }

}
