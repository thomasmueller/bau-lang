package org.bau.stdlib.malloc;

public class ThreeLevelMalloc {

    private final int[] data;

    private long levelBitmap;

    private static int ALIGN_SIZE = 4;
    private static final int SIZE_MASK = ~3;
    private static final int BLOCK_HEADER_OVERHEAD = 4;
    private static final int BLOCKS_START = 4;

    private static final boolean BRANCHLESS = true;
    private static final boolean BRANCHLESS2 = true;

    // -1: prev_phys_block
    // +0: size
    // +1: next_free
    // +2: prev_free

    static int countCalls = 0;
    static int countBranches = 0;

    ThreeLevelMalloc(int capacity) {
        data = new int[capacity];
        int start = BLOCKS_START + 64 * 2;
        for (int i = 0; i < 64; i++) {
            data[BLOCKS_START + 2 * i] = BLOCKS_START + 2 * i;
            data[BLOCKS_START + 2 * i + 1] = BLOCKS_START + 2 * i;
        }
        insertIntoFreeBlocksMap(start, capacity - start - 4);
    }

    public void putByte(int pos, byte x) {
        int intIndex = pos >> 2;
        int byteOffset = pos & 3;
        int shift = (3 - byteOffset) * 8;
        data[intIndex] = (data[intIndex] & ~(0xff << shift)) | ((x & 0xff) << shift);
    }

    public byte getByte(int pos) {
        int intIndex = pos >> 2;
        int byteOffset = pos & 3;
        int shift = (3 - byteOffset) * 8;
        return (byte) ((data[intIndex] >> shift) & 0xff);
    }

    public int malloc(int sizeBytes) {
        if (sizeBytes == 0) {
            // requested zero bytes
            return 0;
        }
        int index0;
        // 4 bytes more for metadata; and then convert to "words"
        int size = (sizeBytes + 7) >> 2;
        size = (size + (ALIGN_SIZE - 1)) & ~(ALIGN_SIZE - 1);
        if (size <= 128) {
            index0 = (int) (size >>> 2);
        } else if (size <= 65536) {
            int round = ((1 << (63 - Long.numberOfLeadingZeros(size))) - 1) >>> 1;
            long s2 = size + round;
            int log2 = 63 - Long.numberOfLeadingZeros(s2);
            index0 = 18 + 2 * log2 + (int) ((s2 << 1 >>> log2) ^ 2);
        } else {
            long round = (1L << (63 - Long.numberOfLeadingZeros(size))) - 1;
            long s2 = size + round;
            int log2 = 63 - Long.numberOfLeadingZeros(s2);
            index0 = Math.min(63, log2 + 34);
        }
        // one more for the metadata
        long mask = levelBitmap & (~0L << (index0));
        int index = Long.numberOfTrailingZeros(mask);
        if (index >= 64) {
            return 0;
        }
        int block = data[BLOCKS_START + 2 * index] - 1;
        removeFromFreeBlocksMap(block, index);
        int currentSize = data[block] & SIZE_MASK;
        if (BRANCHLESS) {
            int splitMask = (currentSize - size - BLOCK_HEADER_OVERHEAD) >> 31;
            int notSplitMask = ~splitMask;
            // start of the remaining free block
            int remaining = block + size;
            // point the "prev" to the new free block
            int prev = block + currentSize - 1;
            data[prev] = remaining;
            int remainingSize = currentSize - size;
            remaining -= 4 & splitMask;
            remainingSize &= notSplitMask;
            data[remaining] = remainingSize | 1;
            index = sizeClass(remainingSize);
            index &= notSplitMask;
            data[remaining + 1] = data[BLOCKS_START + 2 * index];
            // block.prev = head
            data[remaining + 2] = BLOCKS_START + 2 * index;
            // head.next = block
            data[BLOCKS_START + 2 * index] = remaining + 1;
            // block.next.prev = block
            int n = data[remaining + 1];
            n &= notSplitMask;
            data[n + 1] = remaining + 1;
            levelBitmap |= 1L << index;
            data[prev] = remaining;
            int and = ~2;
            and |= notSplitMask;
            data[block + size] &= and;
        } else {
            if (currentSize >= size + BLOCK_HEADER_OVERHEAD) {
                // start of the remaining free block
                int remaining = block + size;
                data[block + currentSize - 1] = remaining;
                int remainingSize = currentSize - size;
                insertIntoFreeBlocksMap(remaining, remainingSize);
            } else {
                // prev of the next one is no longer free
                data[block + size] &= ~2;
            }
        }
        data[block] = size;
        return (block + 1) << 2;
    }

    private void insertIntoFreeBlocksMap(int block, int size) {
        int index = sizeClass(size);
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

    static int sizeClass(long size) {
        if (size <= 128) {
            return (int) (size >>> 2);
        }
        int log2 = 63 - Long.numberOfLeadingZeros(size);
        if (log2 <= 15) {
            return 18 + 2 * log2 + (int) ((size << 1 >>> log2) ^ 2);
        }
        return Math.min(63, log2 + 34);
    }

    public void free(int pos) {
        if (pos == 0) {
            return;
        }
        int block = pos >> 2;
        block -= 1;
        int size = data[block] & ~3;
        int next = block + size;
        int nextSize = data[next];
        if ((nextSize & 1) == 1) {
            nextSize &= ~3;
            int index = sizeClass(nextSize);
            removeFromFreeBlocksMap(next, index);
            size += nextSize;
        }
        if ((data[block] & 2) == 2) {
            int prev = data[block - 1];
            int prevSize = data[prev];
            prevSize &= ~3;
            int index = sizeClass(prevSize);
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
        if (BRANCHLESS2) {
            long diff = data[BLOCKS_START + 2 * index] - (BLOCKS_START + 2 * index);
            long mask = ~((diff - 1) >> 63);
            levelBitmap &= ~(1L << index) | mask;
        } else {
            if(data[BLOCKS_START + 2 * index] == (BLOCKS_START + 2 * index)) {
                levelBitmap &= ~(1L << index);
            }
        }
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        try {
            buff.append(Long.toBinaryString(levelBitmap));
            buff.append("\n");
            for (int group = 1; group < 64; group++) {
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
        //if(true)return;
        for (int group = 1; group < 64; group++) {
            int index = BLOCKS_START + 2 * group;
            if (data[index] < BLOCKS_START + 2 * 64) {
                continue;
            }
            int pointer = index - 1;
            int f = data[index] - 1;
            for(int loop = 0; f >= BLOCKS_START + 2 * 64 && loop < 20; loop++) {
                int size = data[f] & ~3;
                if (sizeClass(size) != group) {
                    throw new IllegalStateException();
                }
                int free = data[f] & 1;
                if (free == 0) {
                    throw new IllegalStateException();
                }
                if (data[f + 2] != pointer + 1) {
                    throw new IllegalStateException();
                }
                int n = data[f + 1] - 1;
                pointer = f;
                f = n;
            }
        }
    }

}
