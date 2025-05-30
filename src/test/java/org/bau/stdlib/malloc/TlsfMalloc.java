package org.bau.stdlib.malloc;

public class TlsfMalloc {

    private final int[] data;

    private long firstLevelBitmap;
    private int[] secondLevelBitmaps = new int[65];

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

    TlsfMalloc(int capacity) {
        data = new int[capacity];
        int start = BLOCKS_START + 64 * 32 * 2;
        for (int i = 0; i < 64 * 32; i++) {
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
        int round = ((1 << (31 - Integer.numberOfLeadingZeros(s2))) - 1) >>> 5;
        s2 += round;
        int fl = 31 - Integer.numberOfLeadingZeros(s2);
        int sl = ((s2 << 5 >>> fl) ^ (1 << 5)) & 31;
        int index;
        int slMap1 = secondLevelBitmaps[fl] & (~0 << sl);
        long flMap2 = firstLevelBitmap & (~0L << (fl + 1));
        int fl2 = Long.numberOfTrailingZeros(flMap2);
        int slMap2 = secondLevelBitmaps[fl2];
        int index1 = Integer.numberOfTrailingZeros(slMap1);
        int index2 = Integer.numberOfTrailingZeros(slMap2);
        int mask = -((slMap1 - 1) >>> 31);
        index = (((fl2 << 5) + index2) & mask) | (((fl << 5) + index1) & ~mask);
        if (index >= 64 * 32) {
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
            int fl = 31 - Integer.numberOfLeadingZeros(nextSize);
            int sl = ((nextSize << 5 >>> fl) ^ (1 << 5)) & 31;
            removeFromFreeBlocksMap(next, (fl << 5) + sl);
            size += nextSize;
        }
        if ((data[block] & 2) == 2) {
            int prev = data[block - 1];
            int prevSize = data[prev];
            prevSize &= ~3;
            int fl = 31 - Integer.numberOfLeadingZeros(prevSize);
            int sl = ((prevSize << 5 >>> fl) ^ (1 << 5)) & 31;
            removeFromFreeBlocksMap(prev, (fl << 5) + sl);
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
        int fl = index >>> 5;
        int sl = index & 63;
        long mask = ~((diff - 1) >> 63);
        secondLevelBitmaps[fl] &= (~(1 << sl)) | mask;
        mask = ~((secondLevelBitmaps[fl] - 1) >> 63);
        firstLevelBitmap &= ~(1L << fl) | mask;
    }

    private void insertIntoFreeBlocksMap(int block, int size) {
        int fl = 31 - Integer.numberOfLeadingZeros(size);
        int sl = ((size << 5 >>> fl) ^ (1 << 5)) & 31;
        int index = (fl << 5) + sl;
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
        firstLevelBitmap |= 1L << fl;
        secondLevelBitmaps[fl] |= 1 << sl;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        try {
            buff.append(Long.toBinaryString(firstLevelBitmap));
            buff.append("\n");
            for (int i = 0; i < 30; i++) {
                if (secondLevelBitmaps[i] != 0) {
                    buff.append(i + ":" + Integer.toBinaryString(secondLevelBitmaps[i]));
                    buff.append("\n");
                }
            }
            for (int fl = 0; fl < 64; fl++) {
                for (int sl = 0; sl < 32; sl++) {
                    int index = BLOCKS_START + 2 * ((fl << 5) + sl);
                    if (data[index] < BLOCKS_START + 2 * 64 * 32) {
                        continue;
                    }
                    int f = data[index] - 1;
                    buff.append(fl + "/" + sl + ": ");
                    for(int loop = 0; f >= BLOCKS_START + 2 * 64 * 32 && loop < 20; loop++) {
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
            }
            int block = BLOCKS_START + 64 * 32 * 2;
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
        // TODO
    }

}
