package org.bau.stdlib.malloc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

public class TlsfMallocTest {

    public static int[] levelCount = new int[64];
    public static int maxMemoryUsage;
    public static int worstMemoryUsage;

    @Test
    public void doublyLinked() {
        Node zero = new Node();
        zero.name = "head";
        zero.next = zero;
        zero.prev = zero;
        Node head = zero;

        // insert a
        Node a = new Node();
        a.name = "a";
        a.next = head.next;
        a.prev = head.prev;
        a.next.prev = a;
        a.prev.next = a;

        // insert b
        Node b = new Node();
        b.name = "b";
        b.next = head.next;
        b.prev = head.prev;
        b.next.prev = b;
        b.prev.next = b;

        // remove a
        Node prev = a.prev;
        Node next = a.next;
        next.prev = prev;
        prev.next = next;
        if (head.next == head) {
            System.err.println("remove last");
        }




        // remove a
//        Node prev = a.prev;
//        Node next = a.next;
//        next.prev = prev;
//        prev.next = next;
//        if (head == a) {
//            head = next;
//            if (head == null) {
//
//            }
//        }
//


    }

    static class Node {
        String name;
        Node prev, next;
    }


    @Test
    public void minMaxSizeSimpler() {
        int ALIGN_SIZE = 4;
        for (int size = 1; size < 1_000_000; size++) {

            int s1 = size;
            s1 = (s1 + (ALIGN_SIZE - 1)) & ~(ALIGN_SIZE - 1);

            int round = ((1 << (31 - Integer.numberOfLeadingZeros(s1))) - 1) >>> 5;
//            int round = (1 << (31 - Integer.numberOfLeadingZeros(s1) - 5)) - 1;
            s1 += round;
            assertTrue(round >= 0);
            assertTrue(round < s1);
            assertTrue(s1 >= size);

            int fl1 = 31 - Integer.numberOfLeadingZeros(s1);
            int sl1 = ((s1 << 5 >>> fl1) ^ (1 << 5)) & 31;

            int s2 = size - 4;
            int fl2 = 31 - Integer.numberOfLeadingZeros(s2);
            int sl2 = ((s2 << 5 >>> fl2) ^ (1 << 5)) & 31;

//            System.out.println("s1: " + Integer.toBinaryString(s1));
//            System.out.println("s2: " + Integer.toBinaryString(s2));
//            System.out.println("ro: " + Integer.toBinaryString(round));
//
//            System.out.println("size " + size + " s1:" + s1 + ":" + fl1 + ":" + sl1 + " s2:" + s2 + ":" + fl2 + ":" + sl2);

            if (fl1 == fl2 && sl1 == sl2) {
                fail("size " + s1);
            }

        }
    }

    @Test
    public void minMaxSize() {
        int ALIGN_SIZE = 4;
        for (int size = 1; size < 2_000_000; size++) {

            int s1 = size;
            s1 = (s1 + (ALIGN_SIZE - 1)) & ~(ALIGN_SIZE - 1);
            if (s1 >= 32) {
                int round = (1 << (31 - Integer.numberOfLeadingZeros(s1) - 5)) - 1;
                s1 += round;
            }

            int fl1, sl1;
            if (s1 < 32) {
                fl1 = 0;
                sl1 = s1;
            } else {
                fl1 = 31 - Integer.numberOfLeadingZeros(s1);
                sl1 = ((s1 >>> (fl1 - 5))) ^ (1 << 5);
                fl1 -= 4;
            }

            int s2 = size - 1;
            int fl2, sl2;
            if (s2 < 32) {
                fl2 = 0;
                sl2 = s2;
            } else {
                fl2 = 31 - Integer.numberOfLeadingZeros(s2);
                sl2 = ((s2 >>> (fl2 - 5))) ^ (1 << 5);
                fl2 -= 4;
            }

            if (fl1 == fl2 && sl1 == sl2) {
                fail("size " + s1);
            }
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
                System.out.println("best: " + seed + " @" + fail);
                best = fail;
                maxLoop = best;
            }
            worstMemoryUsage = Math.max(worstMemoryUsage, maxMemoryUsage);
            sumMemoryUsage += maxMemoryUsage;
        }
        // System.out.println("calls " + Malloc.countCalls + " branch " + Malloc.countBranches);
        for (int i = 0; i < 64; i++) {
            // System.out.println(i + " " + levelCount[i]);
        }
        // Average memory usage 1369360 worst 2107308
        // System.out.println("Average memory usage " + sumMemoryUsage / count + " worst " + worstMemoryUsage);
    }

    public int test(int seed, int maxLoop) {
        int totalSize = 16 * 1024 * 1024;
        TlsfMalloc m = new TlsfMalloc(totalSize);
        // System.out.println(m.toString());
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
                // if (i >= 34) {
                //     System.out.println("test");
                // }
                if (blocks[pos] != 0) {
                    // System.out.printf("%4d: pos %4d size %4d free      %4d\n",
                    //     i, pos, lengths[pos], blocks[pos]);
                    m.free(blocks[pos]);
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
