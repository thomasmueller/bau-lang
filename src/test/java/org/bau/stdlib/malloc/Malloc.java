package org.bau.stdlib.malloc;

public class Malloc {

    // free:
    // (size << 1) + 1
    // next-free
    // prev-free
    // size (at the end of the block)

    // used:
    // (size << 1)
    // prevSize if prev is free, or 0 if not

    private final int[] data;

    private long levelBitmap;

    private static final int BLOCK_HEADER_OVERHEAD = 4;
    private static final int BLOCKS_START = 4;

    private static final boolean BRANCHLESS2 = false;

    // -1: prev_phys_block
    // +0: size
    // +1: next_free
    // +2: prev_free

    static int countCalls = 0;
    static int countBranches = 0;

    Malloc(int capacity) {
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

    int arenaRemaining = 0;
    int arenaStart = 0;

    public int malloc(int sizeBytes) {
        int size = (sizeBytes + 3) >> 2;
        size = (size + 1) & ~1;
        if (size < 4) {
            size = 4;
        }
        if (size < 8) {
            if (arenaRemaining < size + 2) {
                if (arenaRemaining > 0) {
                    free((arenaStart + 2) << 2);
                }
                int arenaSizeBytes = 1024;
                int x = mallocLarger(arenaSizeBytes);
                if (x != 0) {
                    arenaStart = (x >> 2) - 2;
                    arenaRemaining = data[arenaStart] >> 1;
                }
            }
            if (arenaRemaining >= size) {
                int result = arenaStart;
                if (arenaRemaining - size >= 4 + 2) {
                    // enough space for at least one more entry
                    data[arenaStart] = (size + 2) << 1;
                    arenaRemaining -= size + 2;
                    arenaStart += size + 2;
                    data[arenaStart] = arenaRemaining << 1;
                    data[arenaStart + 1] = 0;
                } else {
                    data[arenaStart] = arenaRemaining << 1;
                    arenaRemaining = 0;
                }
                return (result + 2) << 2;
            }
        }
        return mallocLarger(sizeBytes);
    }

    public int mallocLarger(int sizeBytes) {
        if (sizeBytes == 0) {
            // requested zero bytes
            return 0;
        }
        // round up to the next "words"
        int size = (sizeBytes + 3) >> 2;
        size = (size + 1) & ~1;
        if (size < 4) {
            size = 4;
        }
        int round = ((1 << (63 - Long.numberOfLeadingZeros(size - 2))) - 1) >>> 1;
        long s2 = size + round;
        int log2 = 63 - Long.numberOfLeadingZeros(s2 - 2);
        int result = 2 * log2 + (int) (((s2 - 2) << 1 >>> log2) ^ 2);
        int index0 = Math.min(63, result);
        if (index0 != sizeClass(s2)) {
            throw new AssertionError();
        }
        long mask = levelBitmap & (~0L << (index0));
        int index = Long.numberOfTrailingZeros(mask);
        if (index >= 64) {
            return 0;
        }
        int block = data[BLOCKS_START + 2 * index] - 1;
        removeFromFreeBlocksMap(block, index);
        assert (data[block] & 1) == 1;
        int currentSize = data[block] >> 1;
        if (block + currentSize < data.length / 2) {
            assert data[block + currentSize - 1] == currentSize;
        }
        data[block + 1] = 0;
        if (currentSize >= size + BLOCK_HEADER_OVERHEAD) {
            // start of the remaining free block
            int remaining = block + size;
            int remainingSize = currentSize - size;
            data[block + currentSize - 1] = remainingSize;
            insertIntoFreeBlocksMap(remaining, remainingSize);
            data[block] = size << 1;
        } else {
            // prev of the next one is no longer free
            data[block + currentSize + 1] &= ~1;
            data[block] = currentSize << 1;
        }
        return (block + 2) << 2;
    }

    private void insertIntoFreeBlocksMap(int block, int size) {
        int index = sizeClass(size);
        data[block] = (size << 1) | 1;
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
        int log2 = 63 - Long.numberOfLeadingZeros(size - 2);
        int result = 2 * log2 + (int) (((size - 2) << 1 >>> log2) ^ 2);
        result = Math.min(63, result);
        MallocTest.levelCount[result]++;
        return result;
    }

    public void free(int pos) {
        if (pos == 0) {
            return;
        }
        int block = (pos >> 2) - 2;
        assert (data[block] & 1) == 0;
        int size = data[block] >> 1;
        int next = block + size;
        int nextSize = data[next];
        if ((nextSize & 1) == 1) {
            nextSize >>= 1;
            int index = sizeClass(nextSize);
            removeFromFreeBlocksMap(next, index);
            size += nextSize;
        }
        if ((data[block + 1] & 1) == 1) {
            int prevSize = data[block - 1];
            int prev = block - prevSize;
            int index = sizeClass(prevSize);
            removeFromFreeBlocksMap(prev, index);
            size += prevSize;
            block = prev;
        }
        data[block + size - 1] = size;
        data[block + size + 1] |= 1;
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
                for (int loop = 0; f >= BLOCKS_START + 2 * 64 && loop < 20; loop++) {
                    int size = data[f] >> 1;
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
                int size = data[block] >> 1;
                int free = data[block] & 1;
                int prevIsFree = data[block + 1] & 1;
                if (free != 0) {
                    prevIsFree = 0;
                }
                buff.append("block " + block + ": size=" + size + " free=" + free + " prev-free=" + prevIsFree);
                if (free == 00 && prevIsFree != 0) {
                    if (data[block - 1] == 0) {
                        buff.append("ERROR1");
                        break;
                    }
                    buff.append(" prevSize=" + data[block - 1]);
                }
                if (free == 1) {
                    buff.append(" nextFree=" + (data[block + 1] - 1));
                    buff.append(" prevFree=" + (data[block + 2] - 1));
                }
                buff.append("\n");
                if (size <= 0) {
                    buff.append("ERROR2");
                    break;
                }
                block += size;
                if (block > data.length - 8) {
                    break;
                }
            }
        } catch (Exception e) {
            buff.append("ERROR3");
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
                int size = data[f] >> 1;
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
        int block = BLOCKS_START + 64 * 2;
        int last = 0;
        int lastIsFree = 0;
        while(true) {
            int isFree = data[block] & 1;
            int size = data[block] >> 1;
            int prevIsFree = data[block + 1] & 1;
            if (isFree != 0 && lastIsFree != 0) {
                throw new IllegalStateException();
            }
            if (isFree != 0) {
                prevIsFree = 0;
            }
            if (lastIsFree != 0 && prevIsFree == 0) {
                throw new IllegalStateException();
            }
            if (isFree == 0 && prevIsFree != 0) {
                int prevSize = data[block - 1];
                if (block - prevSize != last) {
                    throw new IllegalStateException();
                }
            }
            if (size <= 0) {
                throw new IllegalStateException();
            }
            last = block;
            block += size;
            if (block > data.length - 8) {
                break;
            }
            lastIsFree = isFree;
        }
    }

}
