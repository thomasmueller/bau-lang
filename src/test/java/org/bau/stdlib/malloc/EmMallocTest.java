package org.bau.stdlib.malloc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

/**
 * Malloc implementation similar to Emscripten
 *
 * https://github.com/emscripten-core/emscripten/blob/main/system/lib/emmalloc.c#L350
 * https://github.com/pavel-kirienko/o1heap/blob/master/o1heap/o1heap.c
 *
 */
public class EmMallocTest {
    /*
    #include <stddef.h>
    #include <stdio.h>
    #define NUM_FREE_BUCKETS 64
    #define MIN(x, y) ((x) < (y) ? (x) : (y))
    static int compute_free_list_bucket(size_t allocSize) {
      if (allocSize < 128) return (allocSize >> 3) - 1;
      int clz = __builtin_clz(allocSize);
      int bucketIndex =
        (clz > 19)
          ?     110 - (clz<<2) + ((allocSize >> (29-clz)) ^ 4)
          : MIN( 71 - (clz<<1) + ((allocSize >> (30-clz)) ^ 2), NUM_FREE_BUCKETS-1);
      return bucketIndex;
    }
    void main() {
        for(size_t i = 0; i < 1000000; i++) {
            int x = compute_free_list_bucket(i);
            printf("%d %d\n", i, x);
        }
    }

      Bucket 0: [8, 15], range size=8
      Bucket 1: [16, 23], range size=8
      Bucket 2: [24, 31], range size=8
      Bucket 3: [32, 39], range size=8
      Bucket 4: [40, 47], range size=8
      Bucket 5: [48, 55], range size=8
      Bucket 6: [56, 63], range size=8
      Bucket 7: [64, 71], range size=8
      Bucket 8: [72, 79], range size=8
      Bucket 9: [80, 87], range size=8
      Bucket 10: [88, 95], range size=8
      Bucket 11: [96, 103], range size=8
      Bucket 12: [104, 111], range size=8
      Bucket 13: [112, 119], range size=8
      Bucket 14: [120, 159], range size=40
      Bucket 15: [160, 191], range size=32
      Bucket 16: [192, 223], range size=32
      Bucket 17: [224, 255], range size=32
      Bucket 18: [256, 319], range size=64
      Bucket 19: [320, 383], range size=64
      Bucket 20: [384, 447], range size=64
      Bucket 21: [448, 511], range size=64
      Bucket 22: [512, 639], range size=128
      Bucket 23: [640, 767], range size=128
      Bucket 24: [768, 895], range size=128
      Bucket 25: [896, 1023], range size=128
      Bucket 26: [1024, 1279], range size=256
      Bucket 27: [1280, 1535], range size=256
      Bucket 28: [1536, 1791], range size=256
      Bucket 29: [1792, 2047], range size=256
      Bucket 30: [2048, 2559], range size=512
      Bucket 31: [2560, 3071], range size=512
      Bucket 32: [3072, 3583], range size=512
      Bucket 33: [3584, 6143], range size=2560
      Bucket 34: [6144, 8191], range size=2048
      Bucket 35: [8192, 12287], range size=4096
      Bucket 36: [12288, 16383], range size=4096
      Bucket 37: [16384, 24575], range size=8192
      Bucket 38: [24576, 32767], range size=8192
      Bucket 39: [32768, 49151], range size=16384
      Bucket 40: [49152, 65535], range size=16384
      Bucket 41: [65536, 98303], range size=32768
      Bucket 42: [98304, 131071], range size=32768
      Bucket 43: [131072, 196607], range size=65536
      Bucket 44: [196608, 262143], range size=65536
      Bucket 45: [262144, 393215], range size=131072
      Bucket 46: [393216, 524287], range size=131072
      Bucket 47: [524288, 786431], range size=262144
      Bucket 48: [786432, 1048575], range size=262144
      Bucket 49: [1048576, 1572863], range size=524288
      Bucket 50: [1572864, 2097151], range size=524288
      Bucket 51: [2097152, 3145727], range size=1048576
      Bucket 52: [3145728, 4194303], range size=1048576
      Bucket 53: [4194304, 6291455], range size=2097152
      Bucket 54: [6291456, 8388607], range size=2097152
      Bucket 55: [8388608, 12582911], range size=4194304
      Bucket 56: [12582912, 16777215], range size=4194304
      Bucket 57: [16777216, 25165823], range size=8388608
      Bucket 58: [25165824, 33554431], range size=8388608
      Bucket 59: [33554432, 50331647], range size=16777216
      Bucket 60: [50331648, 67108863], range size=16777216
      Bucket 61: [67108864, 100663295], range size=33554432
      Bucket 62: [100663296, 134217727], range size=33554432
      Bucket 63: 134217728 bytes and larger.

     */

    static int getGroup(int size) {
        int ALIGN_SIZE = 8;
        size = (size + (ALIGN_SIZE - 1)) & ~(ALIGN_SIZE - 1);
        int log2 = 63 - Long.numberOfLeadingZeros(size);
        int small = -((log2 - 7) >> 31);       // 1 for small, 0 otherwise
        int large = -((12 - log2) >> 31);      // 1 for large, 0 otherwise
        int offset = 10 + 14 * large - 11 * small;
        int x = -2 + log2
                - small * (log2 - 5)
                + large;
        int y = ((log2 - 7) << (2 - large)) & ~-small;
        int grp = offset + (int) ((size >>> x) & 31) + y;
        // System.out.printf("                             s:%3d sm:%d la:%d l2:%2d of:%2d x:%2d y:%2d g:%2d\n",
        return Math.min(63, grp);
    }

    @Test
    public void minMaxSizeSimpler() {
        int ALIGN_SIZE = 8;
        int lastGroup = 0;
        for (int size = 16; size < 1_000_000_000;) {

            int s1 = size;
            s1 = (s1 + (ALIGN_SIZE - 1)) & ~(ALIGN_SIZE - 1);

            int log2 = 63 - Long.numberOfLeadingZeros(s1);
            int small = -((log2 - 7) >> 31);       // 1 for small, 0 otherwise
            int large = -((12 - log2) >> 31);      // 1 for large, 0 otherwise
            // shift 3 bits (medium) or 2 bit (large)
            int shift = 2 - large;

            int round = ((1 << log2) - 1) >>> shift;
            round &= ~-small;

            s1 += round;

            assertTrue(round >= 0);
            assertTrue(round < s1);
            assertTrue(s1 >= size);

            int group = getGroup(s1);
            if (group < lastGroup) {
                fail();
            } else if (group > lastGroup) {
                // System.out.println("size => " + size + " is " + group);
                lastGroup = group;
            }
            if (group <= 0 || group >= 64) {
                fail();
            }

            int s2 = size - 8;
            int group2 = getGroup(s2);

            if (group == group2) {
                group = getGroup(s1);
                group2 = getGroup(s2);
                fail("size " + size + " " + s1 + "=>" + group + " " + s2 + "=>" + group2);
            }

            if (group >= 63) {
                break;
            }

            size += 8;

        }
    }

    @Test
    public void test() {
        int best = Integer.MAX_VALUE;
        int maxLoop = 10_000;
        for(int seed = 0; seed < 10; seed++) {
            int fail = test(seed, maxLoop);
            if (fail < best) {
                // System.out.println("best: " + seed + " @" + fail);
                best = fail;
                maxLoop = best;
            }
        }
    }

    public int test(int seed, int maxLoop) {
        int totalSize = 1 * 1024 * 1024;
        EmMalloc m = new EmMalloc(totalSize);
        int available = totalSize / 2;
        Random r = new Random(seed);
        int totalBlocks = 512;
        int[] blocks = new int[totalBlocks];
        int[] lengths = new int[totalBlocks];
        BitSet used = new BitSet(totalSize);
        for (int i = 0; i < maxLoop; i++) {

            try {
                int pos = r.nextInt(totalBlocks);
                //    if (i >= 38) {
                //        System.out.println("test");
                //    }

                if (blocks[pos] != 0) {
                    // System.out.printf("%4d: pos %4d size %4d free      %4d (%4d..%4d)\n",
                    //     i, pos, lengths[pos], blocks[pos], blocks[pos], blocks[pos] + lengths[pos]);
                    m.free(blocks[pos]);
                    m.verify();
                    used.clear(blocks[pos], blocks[pos] + lengths[pos]);
                    // if (m.toString().endsWith("ERROR")) {
                    //     return i;
                    // }
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
                    if (b == 0) {
                        // OOME
                        continue;
                    }
                    if (used.get(b, b + size).cardinality() != 0) {
                        return i;
                    }
                    used.set(b, b + size);
                    // System.out.printf("%4d: pos %4d size %4d malloc -> %4d (%4d..%4d)\n",
                    //     i, pos, size, b, b, b + size);
                    blocks[pos] = b;
                    lengths[pos] = size;
                    available -= size;
                }
                // if (m.toString().endsWith("ERROR")) {
                //     return i;
                // }
                m.verify();
            } catch(Exception e) {
                e.printStackTrace();
                return i;
            }
        }
        // System.out.println("calls " + m.countCalls + " branch " + m.countBranches);
        // System.out.printf("totalSize %d available %d\n", totalSize, available );
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
