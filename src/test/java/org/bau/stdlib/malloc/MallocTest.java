package org.bau.stdlib.malloc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

public class MallocTest {

    public static int[] levelCount = new int[64];
    public static int maxMemoryUsage;
    public static int worstMemoryUsage;

    @Test
    public void sizeClasses() {
        for (int i = 0; i < 58; i++) {
            long size;
            size = ((2L + 1L * (i % 2)) << (i / 2)) + 2;
            if ((size & 1) == 1) continue;
            int sc = sizeClass(size);
            long rounded = roundUp(size);
            assertEquals(sc, sizeClass(rounded));
            rounded = roundUp(size + 1);
            assertTrue(rounded > size);
            int scp1 = sizeClass(rounded);
            assertEquals(i, sc - 2);
            int scm1 = sizeClass(size - 1);
            if (i == 0) {
                assertEquals(sc, scm1 + 2);
            } else {
                assertEquals(sc, scm1 + 1);
            }

            assertEquals(Math.min(63, sc + 1), scp1);
            // System.out.println(i + ": usableBytes:" + ((size - 2) * 4) + " words:" + size + " c(n):" + sc + " c(n-1):" + scm1 + " c(round(n+1)):" + scp1);

            if(sizeClass(roundUp(size)) != sizeClassRoundUp(size)) {
                sizeClassRoundUp(size);
                assertEquals(sizeClass(roundUp(size)), sizeClassRoundUp(size));
            }

            if(sizeClass(roundUp(size + 1)) != sizeClassRoundUp(size + 1)) {
                sizeClassRoundUp(size);
                assertEquals(sizeClass(roundUp(size)), sizeClassRoundUp(size));
            }

            if(sizeClass(roundUp(size - 1)) != sizeClassRoundUp(size - 1)) {
                sizeClassRoundUp(size);
                assertEquals(sizeClass(roundUp(size)), sizeClassRoundUp(size));
            }


        }
    }

    static long roundUp(long size) {
        int round = ((1 << (63 - Long.numberOfLeadingZeros(size - 2))) - 1) >>> 1;
        size += round;
        return size;
    }

    static int sizeClass(long size) {
        int log2 = 63 - Long.numberOfLeadingZeros(size - 2);
        int result = 2 * log2 + (int) (((size - 2) << 1 >>> log2) ^ 2);
        return Math.min(63, result);
    }

    static int sizeClassRoundUp(long size) {
        size -= 2;
        int log2 = 63 - Long.numberOfLeadingZeros(size);
        log2++;
        long twoBits = (size >>> (log2 - 2)) << log2;
        log2--;
        int result = 2 * log2 + (int) ((size << 1 >>> log2) ^ 2);
        long mask = (twoBits - size) >> 63;
        result += mask & 1;
        return result;
    }

    @Test
    public void testCase() {
        int totalSize = 1 * 1024 * 1024;
        Malloc m = new Malloc(totalSize);
        for (int i = 0; i < 1000; i++) {
            int min = m.malloc(1);
            int b = m.malloc(12);
            int b2 = m.malloc(13);
            int b3 = m.malloc(16);
            m.free(b);
            b = m.malloc(16);
            m.free(min);
            m.free(b3);
            m.free(b2);
        }
    }

    @Test
    public void test() {
        long sumMemoryUsage = 0;
        int best = Integer.MAX_VALUE;
        int maxLoop = 1_000;
        int count = 10;
        int totalSize = 16 * 1024 * 1024;
        Malloc m = new Malloc(totalSize);
        for(int seed = 0; seed < count; seed++) {
            m = new Malloc(totalSize);
            int fail = test(m, totalSize, seed, maxLoop);
            if (fail < best) {
                System.out.println("best: " + seed + " @" + fail);
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
        // 3 level:
        // Average memory usage 1446439 worst 2394128

        // factor 1.5:
        // Average memory usage 1449140 worst 2418258

        // factor 2:
        // Average memory usage 1548130 worst 2562924

        // TLSF:
        // Average memory usage 1369360 worst 2107308

        // System.out.println("Average memory usage " + sumMemoryUsage / count + " worst " + worstMemoryUsage);
    }

    public int test(Malloc m, int totalSize, int seed, int maxLoop) {
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
                if (i >= 97) {
                    // System.out.println("test");
                }
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
            } catch(Exception | AssertionError e) {
                return i;
            }
        }
        for (int i = 0; i < totalBlocks; i++) {
            if (blocks[i] != 0) {
                m.free(blocks[i]);
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
