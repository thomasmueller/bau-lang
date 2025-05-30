package org.bau.stdlib.malloc;

public class EmMalloc {

    private final int[] data;

    private long levelBitmap;

    private static int ALIGN_SIZE = 4;
    private static final int SIZE_MASK = ~3;
    private static final int BLOCK_HEADER_OVERHEAD = 1;
    private static final int BLOCKS_START = 4;

    // -1: prev_phys_block
    // +0: size
    // +1: next_free
    // +2: prev_free

    static int countCalls = 0;
    static int countBranches = 0;

    EmMalloc(int capacity) {
        data = new int[capacity];
        int start = BLOCKS_START + 64 * 2;
        for (int i = 0; i < 64; i++) {
            data[BLOCKS_START + 2 * i] = BLOCKS_START + 2 * i;
            data[BLOCKS_START + 2 * i + 1] = BLOCKS_START + 2 * i;
        }
        insertIntoFreeBlocksMap(start, capacity - start - 4);
    }

    public int malloc(int size) {
        if (size == 0) {
            return 0;
        }
        size = (size + (ALIGN_SIZE - 1)) & ~(ALIGN_SIZE - 1);
        // round up to the next group
        int s2 = size;

        int log2 = 63 - Long.numberOfLeadingZeros(s2);
        int small = -((log2 - 7) >> 31);       // 1 for small, 0 otherwise
        int large = -((12 - log2) >> 31);      // 1 for large, 0 otherwise
        int shift = 2 - large;
        int round = ((1 << log2) - 1) >>> shift;
        round &= ~-small;
        s2 += round;

        // TODO some redundancy
        int index0 = getGroup(s2);

        long mask = levelBitmap & (~0L << (index0 + 1));
        int index = Long.numberOfTrailingZeros(mask);

        if (index >= 64) {
            return 0;
        }
        int block = data[BLOCKS_START + 2 * index] - 1;
        removeFromFreeBlocksMap(block, index);
        int currentSize = data[block] & SIZE_MASK;
        if (currentSize > size + BLOCK_HEADER_OVERHEAD) {
            // split
            int remaining = block + size;
            data[block + currentSize - 1] = remaining;
            int remainingSize = currentSize - size;
            data[remaining] = remainingSize | 1;
            insertIntoFreeBlocksMap(remaining, remainingSize);
        } else {
            // prev of the next one is no longer free
            data[block + size] &= ~2;
        }
        data[block] = size;
        return block + 1;
    }

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
        return Math.min(63, grp);
    }

    public void free(int block) {
        if (block == 0) {
            return;
        }
        block -= 1;
        int size = data[block] & ~3;
        int next = block + size;
        int nextSize = data[next];
        if ((nextSize & 1) == 1) {
            nextSize &= ~3;
            int index = getGroup(nextSize);
            removeFromFreeBlocksMap(next, index);
            size += nextSize;
        }
        if ((data[block] & 2) == 2) {
            int prev = data[block - 1];
            int prevSize = data[prev];
            prevSize &= ~3;
            int index = getGroup(prevSize);
            removeFromFreeBlocksMap(prev, index);
            size += prevSize;
            block = prev;
        }
        data[block + size - 1] = block;
        data[block + size] |= 2;
        insertIntoFreeBlocksMap(block, size);
    }

    private void removeFromFreeBlocksMap(int block, int index) {
        int prev = data[block + 2];
        int next = data[block + 1];
        data[prev] = next;
        data[next + 1] = prev;
        long diff = data[BLOCKS_START + 2 * index] - (BLOCKS_START + 2 * index);
        long mask = ~((diff - 1) >> 63);
        levelBitmap &= ~(1L << index) | mask;
    }

    private void insertIntoFreeBlocksMap(int block, int size) {
        int index = getGroup(size);
        data[block] = size | 1;
        // block.next = head.next

        data[block + 1] = data[BLOCKS_START + 2 * index];
        // block.prev = head
        data[block + 2] = BLOCKS_START + 2 * index;
        // head.next = block
        data[BLOCKS_START + 2 * index] = block + 1;
        // block.next.prev = block
        int n = data[block + 1];
        data[n + 1] = block + 1;
        levelBitmap |= 1L << index;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        try {
            buff.append(Long.toBinaryString(levelBitmap));
            buff.append("\n");
            for (int group = 0; group < 64; group++) {
                int index = BLOCKS_START + 2 * group;
                if (data[index] < BLOCKS_START + 2 * 64) {
                    continue;
                }
                int f = data[index] - 1;
                buff.append(group + ": ");
                for(int loop = 0; f >= BLOCKS_START + 2 * 64 && loop < 20; loop++) {
                    int size = data[f] & ~3;
                    buff.append(f + " (size " + size + ") ");
                    int free = data[f] & 1;
                    if (free == 0) {
                        buff.append("ERROR");
                        return buff.toString();
                    }
                    int n = data[f + 1] - 1;
                    f = n;
                }
                buff.append("\n");
            }
            int block = BLOCKS_START + 64 * 2;
            while(true) {
                int size = data[block] & ~3;
                int free = data[block] & 1;
                int prevIsFree = data[block] & 2;
                buff.append("block " + block + ": size=" + size + " free=" + free + " prev-free=" + prevIsFree);
                if (prevIsFree != 0) {
                    if (data[block - 1] > block || data[block - 1] == 0) {
                        buff.append("ERROR");
                        break;
                    }
                    buff.append(" prev=" + data[block - 1]);
                }
                if (free == 1) {
                    buff.append(" nextFree=" + (data[block + 1] - 1));
                    buff.append(" prevFree=" + (data[block + 2] - 1));
                }
                buff.append("\n");
                if (size <= 0) {
                    buff.append("ERROR");
                    break;
                }
                block += size;
                if (block > data.length - 8) {
                    break;
                }
            }
        } catch (Exception e) {
            buff.append("ERROR");
            buff.append(e.toString());
        }
        return buff.toString();
    }

    public void verify() {
        for (int group = 0; group < 64; group++) {
            int index = BLOCKS_START + 2 * group;
            if (data[index] < BLOCKS_START + 2 * 64) {
                continue;
            }
            int f = data[index] - 1;
            for(int loop = 0; f >= BLOCKS_START + 2 * 64 && loop < 20; loop++) {
                int free = data[f] & 1;
                if (free == 0) {
                    throw new AssertionError();
                }
                int n = data[f + 1] - 1;
                f = n;
            }
        }
    }

}
