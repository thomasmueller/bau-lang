#include <limits.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <stdint.h>
#include <time.h>

// malloc =============================
#define ASSERT(A)   
// #define ASSERT(A)   do{if(!(A)){printf("Assertion %s, line %d\n",#A,__LINE__);exit(1);}}while(0)
size_t tmmalloc_nextAllocate = 32 * 1024 * 1024;
int tmmalloc_arenaRemaining = 0;
uint64_t* tmmalloc_arenaStart = 0;
uint64_t tmmalloc_levelBitmap = 0;
int tmmalloc_poolId;
uint64_t tmmalloc_data[256];
uint64_t* tmmalloc_init();
void* tmmalloc(size_t size);
void* tmmalloc_larger(int size, int index0);
void tmfree(void* ptr);
void tmmalloc_insertIntoFreeBlocksMap(uint64_t* block, uint64_t size);
void tmmalloc_removeFromFreeBlocksMap(uint64_t* block, int index);
int tmmalloc_sizeClass(uint64_t size) {
    int log2 = 63 - __builtin_clzll(size);
    int result = 2 * log2 + (int) (((size) << 1 >> log2) ^ 2);
    return result > 63 ? 63 : result;
}
int tmmalloc_sizeClassRoundUp(uint64_t size) {
    int log2 = 63 - __builtin_clzll(size);
    int64_t twoBits = (size >> (log2 - 1)) << (log2 - 1);
    int result = 2 * log2 + (int) ((size << 1 >> log2) ^ 2);
    int64_t mask = (twoBits - (int64_t) size) >> 63;
    return result + (mask & 1);
}
void tmmalloc_insertIntoFreeBlocksMap(uint64_t* block, uint64_t size) {
    int index = tmmalloc_sizeClass(size);
    block[0] = (size << 1) | 1;
    ASSERT(block[0] << 1 >> 32 == 0);
    block[1] = (uint64_t) tmmalloc_data[2 * index];
    block[2] = (uint64_t) &tmmalloc_data[2 * index];
    tmmalloc_data[2 * index] = (uint64_t) (block + 1);
    uint64_t* n = (uint64_t*) block[1];
    n[1] = (uint64_t) (block + 1);
    tmmalloc_levelBitmap |= 1ULL << index;
}
uint64_t* tmmalloc_addMemory() {
    for (int i = 0; i < 10; i++) {
        uint64_t x = (uint64_t) (uintptr_t) malloc(tmmalloc_nextAllocate);
        if (x != 0) {
            tmmalloc_data[tmmalloc_poolId++] = x;
            tmmalloc_insertIntoFreeBlocksMap((uint64_t*) x, (tmmalloc_nextAllocate - 8) >> 3);
            tmmalloc_nextAllocate *= 2;
            return (uint64_t*) x;
        }
        tmmalloc_nextAllocate /= 2;
    }
    printf("Out of memory");
    exit(-1);
}
uint64_t* tmmalloc_init() {
    tmmalloc_levelBitmap = 0;
    tmmalloc_arenaStart = 0;
    tmmalloc_arenaRemaining = 0;
    for (int i = 0; i < 64; i++) {
        uintptr_t x = (uintptr_t) &tmmalloc_data[2 * i];
        tmmalloc_data[2 * i] = (uint64_t) x;
        tmmalloc_data[2 * i + 1] = (uint64_t) x;
    }
    tmmalloc_poolId = 128;
    return (uint64_t*) tmmalloc_addMemory();
}
void tmmalloc_freeAll() {
    while (tmmalloc_poolId > 128) {
        free((uint64_t*) tmmalloc_data[--tmmalloc_poolId]);
    }
}
void* tmmalloc(size_t sizeBytes) {
    if (sizeBytes == 0) return 0;
    // 8 bytes more for metadata; round up, and convert to i64
    uint64_t size = (sizeBytes + 8 + 7) >> 3;  
    if (size < 3) size = 3;
    int index0;
    int result = tmmalloc_sizeClassRoundUp(size);
    index0 = result > 63 ? 63 : result;
    // return tmmalloc_larger(size, index0); 
    if ((tmmalloc_levelBitmap & (1ULL << index0)) == 1) {
        return tmmalloc_larger(size, index0);        
    }
    if (size <= 16) {
        if (tmmalloc_arenaRemaining < size) {
            if (tmmalloc_arenaRemaining > 0) {
                ASSERT(tmmalloc_arenaRemaining >= 3);
                tmmalloc_arenaRemaining = 0;
                tmfree(tmmalloc_arenaStart + 1);
            }
            int s2 = size * 32;
            int index2 = tmmalloc_sizeClassRoundUp(s2);
            uint64_t* xx = (uint64_t*) tmmalloc_larger(s2, index2);
            if (xx != 0) {
                tmmalloc_arenaStart = xx - 1;
                tmmalloc_arenaRemaining = tmmalloc_arenaStart[0] >> 1;
                ASSERT((tmmalloc_arenaStart[0] & 1) == 0);
                ASSERT(tmmalloc_arenaStart[0] >> 32 == 0);
            }
        }
        if (tmmalloc_arenaRemaining >= size ) {
            uint64_t* result = tmmalloc_arenaStart;
            // prev may be free already        
            uint64_t old = tmmalloc_arenaStart[0] >> 32 << 32;
            if (tmmalloc_arenaRemaining - size >= 3) {
                tmmalloc_arenaStart[0] = old | (size << 1);
                tmmalloc_arenaRemaining -= size;
                tmmalloc_arenaStart += size;
                tmmalloc_arenaStart[0] = tmmalloc_arenaRemaining << 1;
            } else {
                tmmalloc_arenaStart[0] = old | (tmmalloc_arenaRemaining << 1);
                tmmalloc_arenaRemaining = 0;
            }
            return result + 1;
        }
    }
    return tmmalloc_larger(size, index0);
}
void* tmmalloc_larger(int size, int index0) {
    uint64_t mask = tmmalloc_levelBitmap & (~0ULL << index0);
    int index = __builtin_ctzll(mask);
    if (index >= 64) {
        tmmalloc_addMemory();
        mask = tmmalloc_levelBitmap & (~0ULL << index0);
        index = __builtin_ctzll(mask);
        if (index >= 64) {
            printf("Out of memory trying to allocate %d; levels %llx\n", size, tmmalloc_levelBitmap) ; 
            exit(0);
            return 0;
        }
    }
    uint64_t* block = ((uint64_t*) tmmalloc_data[2 * index]) - 1;
    uint64_t currentSize = block[0] >> 1;
    ASSERT((block[0] & 1) == 1);
    if(block[0] >> 32 != 0) {
        int prevSize = block[0] >> 32;
        printf("prev block of free block is free: %p; prev size %d -> %p\n", block, prevSize, block - prevSize);
    }
    tmmalloc_removeFromFreeBlocksMap(block, index);
    ASSERT(block[0] >> 32 == 0);
    if (currentSize >= size + 3) {
        uint64_t* remaining = block + size;
        uint64_t remainingSize = currentSize - size;
        block[currentSize] &= (1L << 32) - 1;
        block[currentSize] |= remainingSize << 32;
        ASSERT((block[currentSize] & 1) == 0);
        ASSERT(block[currentSize] >> 32 != 0);
        tmmalloc_insertIntoFreeBlocksMap(remaining, remainingSize);
        block[0] = size << 1;
        ASSERT(block[size] >> 32 == 0);
    } else {
        block[currentSize] &= (1L << 32) - 1;
        block[0] = currentSize << 1;
        ASSERT((block[currentSize] & 1) == 0);
        ASSERT(block[currentSize] >> 32 == 0);
    }
    return block + 1;
}
void tmfree(void* ptr) {
    if (ptr == 0) return;
    uint64_t* block = (uint64_t*) ptr;
    block -= 1;    
    uint64_t header = block[0];
    ASSERT((block[0] & 1) == 0);
    uint64_t size = (((1L << 32) - 1) & header) >> 1;
    int prevSize = header >> 32;
    uint64_t* next = block + size;
    int nextSize = next[0] & ((1L << 32) - 1);
    if ((nextSize & 1) == 1) {
        nextSize >>= 1;
        int index = tmmalloc_sizeClass(nextSize);
        tmmalloc_removeFromFreeBlocksMap(next, index);
        size += nextSize;
    }
    if (prevSize) {
        uint64_t* prev = block - prevSize;
        int index = tmmalloc_sizeClass(prevSize);
        ASSERT((prev[0] & 1) == 1);
        tmmalloc_removeFromFreeBlocksMap(prev, index);
        size += prevSize;
        block = prev;
    }
    block[size] &= (1L << 32) - 1;
    block[size] |= size << 32;
    ASSERT((block[size] & 1) == 0);
    ASSERT(block[size] >> 32 != 0);
    tmmalloc_insertIntoFreeBlocksMap(block, size);
}
void tmmalloc_removeFromFreeBlocksMap(uint64_t* block, int index) {
    uint64_t* prev = (uint64_t*) block[2];
    uint64_t* next = (uint64_t*) block[1];
    prev[0] = (uint64_t) next;
    next[1] = (uint64_t) prev;
    int head = 2 * index;
    uint64_t a = tmmalloc_data[head];
    uint64_t b = (uint64_t) &tmmalloc_data[head];
    long diff = a - b;
    long mask = ~((diff - 1) >> 63);
    tmmalloc_levelBitmap &= ~(1ULL << index) | mask;
}
// tmmalloc end =============================

#define TOTAL_BLOCKS 512

#define TOTAL_SIZE (128 * 1024 * 1024)
#define BITSET_SIZE (TOTAL_SIZE / 8)

#define VERIFY 0

void* malloc_custom(size_t size);
void free_custom(void* ptr);

int randomAllocationSize(int maxSize);
int test(int seed, int maxLoop);

#define test_malloc(A) tmmalloc(A)
#define test_free(A)   tmfree(A)

//#define test_malloc(A) malloc(A)
//#define test_free(A)   free(A)

#define BLOCK_HEADER_OVERHEAD 4
#define BLOCKS_START 4
#define CAPACITY (16 * 1024 * 1024)

// default                        6.070

// Peak diff 2565516 without arena
// tmmalloc                       4.240

// Peak diff 2508889 arena
// tmmalloc                       4.531

uint64_t biggestDiff = 0;
void* base;

int levelCounts[64];

int main(int _argc, char *_argv[]) {
    /*
    printf("tmmalloc_init\n"); 
    base = tmmalloc_init(128 * 1024 * 1024);
    printf("tmmalloc_done\n"); 
    
    void* x = test_malloc(100);
    void* y = test_malloc(100);
    printf("diff %ld\n", y - x);
    for(int i=0; i<64;i++) levelCounts[i] = 0;
    
    test_free(x);
    test_free(y);
    tmmalloc_free_all();
    */
    base = tmmalloc_init();
    
    for(int i=0; i < 10000; i++) {
     //   printf("test %d\n", i);
        int x = test(i, 10000);
        if (x < 2147483647) {
            printf("Seed %d result %d\n", i, x); 
            return -1;
        }
        //tmmalloc_free_all();
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
        // tmmalloc_reinit(128 * 1024 * 1024);
    }
    // printf("seed %d maxLoop %d\n", seed, maxLoop);
    void* low = 0;
    void* high = 0;
    srand(seed);
    for (int i = 0; i < maxLoop; i++) {
//printf("%d:\n", i);     
//if (i >= 10) exit(1);   
        int pos = rand() % TOTAL_BLOCKS;
        if (blocks[pos] != 0) {
 //           printf("  free %d\n", (int) (blocks[pos] - base));
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
       //     printf("  malloc byts %d\n", size);
            void* b = test_malloc(size);
            if (b == 0) {
           //     printf("  malloc byts %d => OOME\n", size);
                exit(0);
                continue; // Simulate out-of-memory
            }
//printf("     set memory\n");
//for(int i=0; i<size;i++) ((int8_t*)b)[i]=1|i;
            //*(int8_t*) (b + size - 1) = 0xee;
            //*(int8_t*) (b + size - 1) = 0xee;
    //        printf("  malloc %d => %d\n", size, (int) (b - base));
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
