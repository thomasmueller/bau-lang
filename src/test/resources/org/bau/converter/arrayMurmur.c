#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <stddef.h>
#include <stdint.h>
/* builtin */
static inline int _ctzll(uint64_t x) {
#if defined(__GNUC__) || defined(__clang__)
    return __builtin_ctzll(x);
#else
    if (!x) return 64; int c = 0; while (!(x & 1)) { x >>= 1; c++; } return c;
#endif
}
static inline int _clzll(uint64_t x) {
#if defined(__GNUC__) || defined(__clang__)
    return __builtin_clzll(x);
#else
    if (!x) return 64; int c = 0; uint64_t m = (uint64_t)1 << 63; while (!(x & m)) { m >>= 1; c++; } return c;
#endif
}
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
    int log2 = 63 - _clzll(size);
    int result = 2 * log2 + (int) (((size) << 1 >> log2) ^ 2);
    return result > 63 ? 63 : result;
}
int tmmalloc_sizeClassRoundUp(uint64_t size) {
    int log2 = 63 - _clzll(size);
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
    int index = _ctzll(mask);
    if (index >= 64) {
        tmmalloc_addMemory();
        mask = tmmalloc_levelBitmap & (~0ULL << index0);
        index = _ctzll(mask);
        if (index >= 64) {
            printf("Out of memory trying to allocate %d; levels %llx\n", size, tmmalloc_levelBitmap) ; 
            exit(0);
            return 0;
        }
    }
    uint64_t* block = ((uint64_t*) tmmalloc_data[2 * index]) - 1;
    uint64_t currentSize = block[0] >> 1;
    ASSERT((block[0] & 1) == 1);
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
#define _malloc(a)      tmmalloc(a)
#define _free(a)        tmfree(a)
#define REF_COUNT_INC
#define REF_COUNT_STACK_INC
#define PRINT(...)
#define _end()
#define _traceMalloc(a)
#define _traceFree(a)
#define _incUse(a)            {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT("++  %p line %d, from %d\n", a, __LINE__, (a)?(a)->_refCount:0);if(a)(a)->_refCount++;}}
#define _decUse(a, type)      {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT("--  %p line %d, from %d\n", a, __LINE__, (a)->_refCount);if((a)&&--((a)->_refCount) == 0)type##_free(a);}}
#define _incUseStack(a)       _incUse(a)
#define _decUseStack(a, type) _decUse(a, type)
#define _arrayLen(a) (a==0?0:*((int32_t*)a))
int64_t arrayOutOfBounds(int64_t x, int64_t len) {
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
/* types */
typedef struct i8_array i8_array;
struct i8_array;
typedef struct int_array int_array;
struct int_array;
struct i8_array {
    int32_t len;
    int32_t _refCount;
    int8_t* data;
};
i8_array* i8_array_new(uint64_t len) {
    if (len < 0 || len >= (1L << 31)) arrayOutOfBounds(len, 1L << 31);
    i8_array* result = _malloc(sizeof(i8_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(int8_t) * len);
    memset(result->data, 0, sizeof(int8_t) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct int_array {
    int32_t len;
    int32_t _refCount;
    int64_t* data;
};
int_array* int_array_new(uint64_t len) {
    if (len < 0 || len >= (1L << 31)) arrayOutOfBounds(len, 1L << 31);
    int_array* result = _malloc(sizeof(int_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(int64_t) * len);
    memset(result->data, 0, sizeof(int64_t) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
i8_array* hex_2(int64_t x, int64_t len);
int32_t i32_1(int64_t x);
int64_t idx_2(int64_t x, int64_t len);
int32_t murmur3_32_1(i8_array* data);
int32_t rotLefti32_2(int32_t x, int64_t n);
int64_t shiftLeft_2(int64_t a, int64_t b);
int32_t shiftRight_i32_2(int32_t a, int64_t b);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void i8_array_free(i8_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void int_array_free(int_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = _malloc(sizeof(char) * len);
    memcpy(result->data, data, sizeof(char) * len);
    return result;
}
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1003;
i8_array* string_1004;
i8_array* string_1005;
i8_array* hex_2(int64_t x, int64_t len) {
    int64_t l = len;
    if (l < 0) {
        l = 0;
    } else {
        if (l > 16) {
            l = 16;
        }
    }
    i8_array* _t0 = i8_array_new(l);
    _incUseStack(_t0);
    i8_array* data = _t0;
    int64_t y = x;
    int64_t i = l - 1;
    while (i >= 0) {
        int64_t c = 48 + (y & 15);
        if ((y & 15) > 9) {
            c = ( 97 + (y & 15) ) - 10;
        }
        data->data[idx_2(i, _arrayLen(data))] = c;
        y >>= 4;
        i -= 1;
    }
    _decUseStack(_t0, i8_array);
    return data;
}
int32_t i32_1(int64_t x) {
    return x;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
int32_t murmur3_32_1(i8_array* data) {
    if (_arrayLen(data) <= 0) {
        return 0;
    }
    int32_t c1 = -862048943;
    int32_t c2 = 461845907;
    int32_t h = 0;
    int64_t i = 0;
    if (_arrayLen(data) > 3) {
        int64_t j = 0;
        while (1) {
            int32_t x = i32_1(((data->data[j] & 255)) | (shiftLeft_2((data->data[j + 1] & 255), 8)) | (shiftLeft_2((data->data[j + 2] & 255), 16)) | (shiftLeft_2((data->data[j + 3] & 255), 24)));
            x *= -862048943;
            int32_t _t0 = rotLefti32_2(x, 15);
            x = _t0;
            x *= 461845907;
            h ^= x;
            int32_t _t1 = rotLefti32_2(h, 13);
            h = _t1;
            h = ( h * 5 ) + 3864292196;
            int64_t n = j + 4;
            if (n > ( _arrayLen(data) - 4 )) {
                break;
            }
            j = n;
        }
        i = j;
    }
    int32_t x = 0;
    if (i < ( _arrayLen(data) - 4 )) {
        i += 4;
    }
    int64_t _t2 = i < _arrayLen(data);
    if (_t2) {
        int64_t _t3 = i != ( _arrayLen(data) - 4 );
        _t2 = _t3;
    }
    if (_t2) {
        int64_t s = 0;
        while (1) {
            x ^= shiftLeft_2((data->data[i] & 255), s);
            int64_t n = i + 1;
            if (n >= _arrayLen(data)) {
                break;
            }
            i = n;
            s += 8;
        }
        x *= -862048943;
        int32_t _t4 = rotLefti32_2(x, 15);
        x = _t4;
        x *= 461845907;
        h ^= x;
    }
    h ^= _arrayLen(data);
    h ^= shiftRight_i32_2(h, 16);
    h *= 0x85ebca6b;
    h ^= shiftRight_i32_2(h, 13);
    h *= 0xc2b2ae35;
    h ^= shiftRight_i32_2(h, 16);
    return h;
}
int32_t rotLefti32_2(int32_t x, int64_t n) {
    int64_t _r0 = (shiftLeft_2(x, n)) | (shiftRight_i32_2(x, (32 - n)));
    return _r0;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int32_t shiftRight_i32_2(int32_t a, int64_t b) {
    return ((uint32_t) a) >> b;
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1001 = str_const("a", 1);
    string_1002 = str_const("", 0);
    string_1003 = str_const("test", 4);
    string_1004 = str_const("Hello, world!", 13);
    string_1005 = str_const("The quick brown fox jumps over the lazy dog", 43);
    _main();
    return 0;
}
void _main() {
    int32_t _t0 = murmur3_32_1(string_1002);
    i8_array* _t1 = hex_2(_t0, 8);
    printf("%.*s\n", _arrayLen(_t1), _t1->data);
    int32_t _t2 = murmur3_32_1(string_1001);
    i8_array* _t3 = hex_2(_t2, 8);
    printf("%.*s\n", _arrayLen(_t3), _t3->data);
    int32_t _t4 = murmur3_32_1(string_1003);
    i8_array* _t5 = hex_2(_t4, 8);
    printf("%.*s\n", _arrayLen(_t5), _t5->data);
    int32_t _t6 = murmur3_32_1(string_1004);
    i8_array* _t7 = hex_2(_t6, 8);
    printf("%.*s\n", _arrayLen(_t7), _t7->data);
    int32_t _t8 = murmur3_32_1(string_1005);
    i8_array* _t9 = hex_2(_t8, 8);
    printf("%.*s\n", _arrayLen(_t9), _t9->data);
    _decUseStack(_t9, i8_array);
    _decUseStack(_t7, i8_array);
    _decUseStack(_t5, i8_array);
    _decUseStack(_t3, i8_array);
    _decUseStack(_t1, i8_array);
    _end();
}
/*

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

*/
