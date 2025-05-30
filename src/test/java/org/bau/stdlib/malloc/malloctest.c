#include <limits.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <stdint.h>
#include <time.h>

// malloc =============================
const size_t tmmalloc_memory = 256 * 1024 * 1024;
int tmmalloc_arenaRemaining = 0;
int tmmalloc_arenaStart = 0;
uint64_t tmmalloc_levelBitmap = 0;
uint32_t tmmalloc_data[tmmalloc_memory];
uint32_t* tmmalloc_init(int capacity);
void* tmmalloc(size_t size);
void* tmmalloc_larger(int size);
void tmfree(void* ptr);
void tmmalloc_insertIntoFreeBlocksMap(int block, int size);
void tmmalloc_removeFromFreeBlocksMap(int block, int index);
int tmmalloc_sizeClass(uint64_t size) {
    size -= 2;
    int log2 = 63 - __builtin_clzll(size);
    int result = 2 * log2 + (int) (((size) << 1 >> log2) ^ 2);
    return result > 63 ? 63 : result;
}
int tmmalloc_sizeClassRoundUp(uint64_t size) {
    size -= 2;
    int log2 = 63 - __builtin_clzll(size);
    int64_t twoBits = (size >> (log2 - 1)) << (log2 - 1);
    int result = 2 * log2 + (int) ((size << 1 >> log2) ^ 2);
    int64_t mask = (twoBits - (int64_t) size) >> 63;
    return result + (mask & 1);
}
void tmmalloc_insertIntoFreeBlocksMap(int block, int size) {
    int index = tmmalloc_sizeClass(size);
    tmmalloc_data[block] = (size << 1) | 1;
    tmmalloc_data[block + 1] = tmmalloc_data[2 * index];
    tmmalloc_data[block + 2] = 2 * index;
    tmmalloc_data[2 * index] = block + 1;
    int n = tmmalloc_data[block + 1];
    tmmalloc_data[n + 1] = block + 1;
    tmmalloc_levelBitmap |= 1ULL << index;
}
uint32_t* tmmalloc_init(int capacity) {
    int start = 64 * 2;
    tmmalloc_levelBitmap = 0;
    tmmalloc_arenaStart = 0;
    tmmalloc_arenaRemaining = 0;
    for (int i = 0; i < 64; i++) {
        tmmalloc_data[2 * i] = 2 * i;
        tmmalloc_data[2 * i + 1] = 2 * i;
    }
    tmmalloc_insertIntoFreeBlocksMap(start, (capacity - start - 4) >> 2);
    return tmmalloc_data;
}
void* tmmalloc(size_t sizeBytes) {
    if (sizeBytes == 0) return 0;
    // 8 bytes more for metadata; round up, and convert to ints
    int size = (sizeBytes + 8 + 3) >> 2;  
    size = (size + 1) & ~1;
    if (size <= 16) {
        if (tmmalloc_arenaRemaining < size + 2) {
            if (tmmalloc_arenaRemaining > 0) {
                tmfree(tmmalloc_data + tmmalloc_arenaStart + 2);
            }
            void* xx = tmmalloc_larger(256);
            if (xx != 0) {
                int x = (((uint32_t*) xx) - tmmalloc_data);
                tmmalloc_arenaStart = x - 2;
                tmmalloc_arenaRemaining = tmmalloc_data[tmmalloc_arenaStart] >> 1;
            }
        }
        if (tmmalloc_arenaRemaining >= size + 2) {
            int result = tmmalloc_arenaStart + 2;
            if (tmmalloc_arenaRemaining - size >= 4) {
                tmmalloc_data[tmmalloc_arenaStart] = (size + 2) << 1;
                tmmalloc_arenaRemaining -= size + 2;
                tmmalloc_arenaStart += size + 2;
                tmmalloc_data[tmmalloc_arenaStart] = tmmalloc_arenaRemaining << 1;
                tmmalloc_data[tmmalloc_arenaStart + 1] = 0;
            } else {
                tmmalloc_data[tmmalloc_arenaStart] = tmmalloc_arenaRemaining << 1;
                tmmalloc_arenaRemaining = 0;
            }
            return tmmalloc_data + result;
        }
    }
    return tmmalloc_larger(size);
}
void* tmmalloc_larger(int size) {
    int index0;
    int result = tmmalloc_sizeClassRoundUp(size);
    index0 = result > 63 ? 63 : result;        
    uint64_t mask = tmmalloc_levelBitmap & (~0ULL << index0);
    int index = __builtin_ctzll(mask);
    if (index >= 64) { printf("Out of memory\n") ; return 0; }
    int block = tmmalloc_data[2 * index] - 1;
    tmmalloc_removeFromFreeBlocksMap(block, index);
    int currentSize = tmmalloc_data[block] >> 1;
    tmmalloc_data[block + 1] = 0;
    if (currentSize >= size + 4) {
        int remaining = block + size;
        int remainingSize = currentSize - size;
        tmmalloc_data[block + currentSize + 1] = remainingSize;
        tmmalloc_insertIntoFreeBlocksMap(remaining, remainingSize);
        tmmalloc_data[block] = size << 1;
    } else {
        tmmalloc_data[block + currentSize + 1] = 0;
        tmmalloc_data[block] = currentSize << 1;
    }
    return tmmalloc_data + block + 2;
}
void tmfree(void* ptr) {
    if (ptr == 0) return;
    int block = (((uint32_t*) ptr) - tmmalloc_data);
    block -= 2;
    int size = tmmalloc_data[block] >> 1;
    int next = block + size;
    int nextSize = tmmalloc_data[next];
    if ((nextSize & 1) == 1) {
        nextSize >>= 1;
        int index = tmmalloc_sizeClass(nextSize);
        tmmalloc_removeFromFreeBlocksMap(next, index);
        size += nextSize;
    }
    int prevSize = tmmalloc_data[block + 1];
    if (prevSize) {
        int prev = block - prevSize;
        int index = tmmalloc_sizeClass(prevSize);
        tmmalloc_removeFromFreeBlocksMap(prev, index);
        size += prevSize;
        block = prev;
    }
    tmmalloc_data[block + size + 1] = size;
    tmmalloc_insertIntoFreeBlocksMap(block, size);
}
void tmmalloc_removeFromFreeBlocksMap(int block, int index) {
    int prev = tmmalloc_data[block + 2];
    int next = tmmalloc_data[block + 1];
    tmmalloc_data[prev] = next;
    tmmalloc_data[next + 1] = prev;
    int head = 2 * index;
    long diff = tmmalloc_data[head] - head;
    long mask = ~((diff - 1) >> 63);
    tmmalloc_levelBitmap &= ~(1ULL << index) | mask;
}
// tmmalloc end =============================

#define TOTAL_BLOCKS 512

#define TOTAL_SIZE (16 * 1024 * 1024)
#define BITSET_SIZE (TOTAL_SIZE / 8)

#define VERIFY 0

void* malloc_custom(size_t size);
void free_custom(void* ptr);

int randomAllocationSize(int maxSize);
int test(int seed, int maxLoop);

#define test_malloc(A) tmmalloc(A)
#define test_free(A)   tmfree(A)

// #define test_malloc(A) malloc(A)
// #define test_free(A)   free(A)

#define BLOCK_HEADER_OVERHEAD 4
#define BLOCKS_START 4
#define CAPACITY (16 * 1024 * 1024)

// default                        6.512

// Peak diff 2462471
// tmmalloc                       4.725

// Peak diff 2272617
// tlsf                          ~5.000

// Peak diff 3293039
// o1heap                        ~4.500

uint64_t biggestDiff = 0;
void* base;

int levelCounts[64];

int main(int _argc, char *_argv[]) {
    
    printf("tmmalloc_init\n"); 
    base = tmmalloc_init(16 * 1024 * 1024);
    printf("tmmalloc_done\n"); 
    
    void* x = test_malloc(100);
    void* y = test_malloc(100);
    printf("diff %ld\n", y - x);
    for(int i=0; i<64;i++) levelCounts[i] = 0;
    
    test_free(x);
    test_free(y);

    for(int i=0; i < 10000; i++) {
        // printf("%d\n", i);
        int x = test(i, 10000);
        if (x < 2147483647) {
            printf("Seed %d result %d\n", i, x); 
            return -1;
        }
    }
    printf("Peak diff %lld\n", biggestDiff);

    for(int i=0; i<64;i++) {
        // printf("levelCounts[%d] = %d\n", i, levelCounts[i]);
    }

}

// Bitset implementation (1 bit per byte of total memory)
uint8_t used[BITSET_SIZE];

// Sets bits in [start, end)
void set_bits(int start, int end) {
    // printf("    set_bits %d .. %d\n", start, end);
    if (start < 0 || start >= TOTAL_SIZE) {
        printf("out of bounds %d\n", start);
    }
    if (end < 0 || end >= TOTAL_SIZE) {
        printf("out of bounds %d\n", end);
    }
    for (int i = start; i < end; i++) {
        used[i / 8] |= (1 << (i % 8));
    }
}

// Clears bits in [start, end)
void clear_bits(int start, int end) {
    // printf("clear_bits %d .. %d\n", start, end);
    if (start < 0 || start >= TOTAL_SIZE) {
        printf("out of bounds %d\n", start);
    }
    if (end < 0 || end >= TOTAL_SIZE) {
        printf("out of bounds %d\n", end);
    }
    for (int i = start; i < end; i++) {
        used[i / 8] &= ~(1 << (i % 8));
    }
}

// Returns 1 if any bit in [start, end) is set
int is_any_bit_set(int start, int end) {
    if (start < 0 || start >= TOTAL_SIZE) {
        printf("out of bounds %d\n", start);
    }
    if (end < 0 || end >= TOTAL_SIZE) {
        printf("out of bounds %d\n", end);
    }    
    for (int i = start; i < end; i++) {
        if (used[i / 8] & (1 << (i % 8))) {
            printf("    is_any_bit_set %d .. %d\n", start, end);
            printf("    yes: %d\n", i);
            return 1;
        }
    }
    return 0;
}
int countCalls = 0;
int countBranches = 0;

int test(int seed, int maxLoop) {
    int available = TOTAL_SIZE / 2;
    void* blocks[TOTAL_BLOCKS] = {0};
    int lengths[TOTAL_BLOCKS] = {0};
    if (VERIFY) {
        memset(used, 0, sizeof(used));
        tmmalloc_init(16 * 1024 * 1024);
    }
    // printf("seed %d maxLoop %d\n", seed, maxLoop);
    void* low = 0;
    void* high = 0;
    srand(seed);
    for (int i = 0; i < maxLoop; i++) {
        int pos = rand() % TOTAL_BLOCKS;
        if (blocks[pos] != 0) {
            // printf("free %d\n", (int) (blocks[pos] - base));
            test_free(blocks[pos]);
            if (VERIFY) {
                // printf("  free => %ld to %ld\n", (blocks[pos] - base) / 4, (blocks[pos] - base + lengths[pos]) / 4);
                clear_bits(blocks[pos] - base, blocks[pos] - base + lengths[pos]);
            }
            available += lengths[pos];
            blocks[pos] = 0;
            lengths[pos] = 0;
        }
        int size = 0;
        int retries = 0;
        do {
            size = randomAllocationSize(128 * 1024);
            if (++retries > 100) {
                size = 0;
                break;
            }
        } while (size > available);
        if (size > 0) {
            void* b = test_malloc(size);
            if (b == 0) {
                printf("  malloc %d => OOME\n", size);
                exit(0);
                continue; // Simulate out-of-memory
            }
            *(int8_t*) (b + size - 1) = 0;
            // printf("  malloc %d => %d\n", size, (int) (b - base));
            if (low == 0 || b < low) {
                low = b;
            }
            if (high == 0 || b + size > high) {
                high = b + size;
            }
            if (VERIFY) {
                //printf("  malloc size=%d => %d..%d\n", size, (int) (b - base) / 4 - 1, (int) (b - base + size) / 4 - 1);
                if (is_any_bit_set(b - base, b - base + size)) {
                    return i;
                }
                set_bits(b - base, b - base + size);
            }
            blocks[pos] = b;
            lengths[pos] = size;
            available -= size;
        }
    }
    for(int i=0; i<TOTAL_BLOCKS; i++) {
        if (blocks[i] != 0) {
            test_free(blocks[i]);
        }
    }
    uint64_t diff = high - low;
    if (diff > biggestDiff) {
        biggestDiff = diff;
    }
    return INT_MAX;
}

int randomAllocationSize(int maxSize) {
    int r = rand() % 1000;
    if (r < 350) {
        // 8–32 bytes
        return 8 + rand() % 25;
    } else if (r < 600) {
        // 33–64 bytes
        return 33 + rand() % 32;
    } else if (r < 750) {
        // 65–128 bytes
        return 65 + rand() % 64;
    } else if (r < 850) {
        // 129–256 bytes
        return 129 + rand() % 128;
    } else if (r < 930) {
        // 257–512 bytes
        return 257 + rand() % 256;
    } else if (r < 980) {
        // 513–1024 bytes
        return 513 + rand() % 512;
    } else if (r < 990) {
        // up to 4KB
        return 1025 + rand() % (3072);
    } else if (r < 995) {
        // up to 64KB
        return 4096 + rand() % (64 * 1024);
    }
    // up to maxSize
    return 64 * 1024 + rand() % (maxSize);
}
