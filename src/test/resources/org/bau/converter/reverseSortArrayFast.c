#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <limits.h>
#include <stddef.h>
#include <stdint.h>
#include <string.h>
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
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
i8_array* org_bau_Env_arg_1(int64_t index);
int64_t org_bau_Env_argCount_0();
int64_t org_bau_Int_parseInt_1(i8_array* value);
int64_t org_bau_Utils_random_0();
void reverse_int_array_int_3(int_array* buff, int64_t first, int64_t last);
void shellSort_int_array_int_1(int_array* a);
int64_t shiftRight_int_2(int64_t a, int64_t b);
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
i8_array* string_1007;
i8_array* string_1008;
i8_array* string_1009;
i8_array* string_1010;
i8_array* string_1011;
int64_t randomSeed;
int64_t MIN_INT;
int64_t MAX_INT;
int64_t MIN_I32;
int64_t MAX_I32;
int64_t idiv_2(int64_t a, int64_t b) {
    if (b != 0) return a / b;
    if (a == 0) return 0;
    return a > 0 ? LLONG_MAX : LLONG_MIN;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
i8_array* org_bau_Env_arg_1(int64_t index) {
    int64_t _t0 = index < 0;
    if (!(_t0)) {
        int64_t _t1 = index >= org_bau_Env_argCount_0();
        _t0 = _t1;
    }
    if (_t0) {
        i8_array* _t2 = i8_array_new(0);
        return _t2;
    }
    int64_t len = 0;
    len = strlen(__argv[index]);
    i8_array* _t3 = i8_array_new(len);
    _incUseStack(_t3);
    i8_array* result = _t3;
    strncpy((char*) result->data, __argv[index], len);
    _decUseStack(_t3, i8_array);
    return result;
}
int64_t org_bau_Env_argCount_0() {
    return __argc;
    return 0;
}
int64_t org_bau_Int_parseInt_1(i8_array* value) {
    int64_t x = 0;
    int64_t sign = 1;
    if (_arrayLen(value)) {
        int64_t i = 0;
        if (value->data[0] == 45) {
            sign = -1;
            i += 1;
        }
        while (i < _arrayLen(value)) {
            int8_t n = value->data[idx_2(i, _arrayLen(value))];
            int64_t _t0 = n >= 48;
            if (_t0) {
                int64_t _t1 = n <= 57;
                _t0 = _t1;
            }
            if (_t0) {
                x *= 10;
                x += n - 48;
            }
            i += 1;
        }
        int64_t _r0 = sign * x;
        return _r0;
    }
    return 0;
}
int64_t org_bau_Utils_random_0() {
    randomSeed += 0x9e3779b97f4a7c15;
    int64_t z = randomSeed;
    z = (z ^ (shiftRight_int_2(z, 30))) * -4658895280553007687;
    z = (z ^ (shiftRight_int_2(z, 27))) * -7723592293110705685;
    int64_t _r0 = z ^ (shiftRight_int_2(z, 31));
    return _r0;
}
void reverse_int_array_int_3(int_array* buff, int64_t first, int64_t last) {
    while (first < last) {
        int64_t temp = buff->data[first];
        buff->data[first] = buff->data[last];
        buff->data[last] = temp;
        if (first >= ( _arrayLen(buff) - 1 )) {
            break;
        }
        first += 1;
        if (last < 1) {
            break;
        }
        last -= 1;
    }
}
void shellSort_int_array_int_1(int_array* a) {
    if (_arrayLen(a) < 1) {
        return;
    }
    int64_t h = 16;
    while (_arrayLen(a) > ( idiv_2(h, 16) )) {
        h = ( h + h ) + ( idiv_2(h, 4) ) + 16;
    }
    while (h > 15) {
        h = ( idiv_2((h - 16), 9) ) * 4;
        int64_t g = idiv_2((h + 15), 16);
        if (g < 0) {
            break;
        }
        if (g >= _arrayLen(a)) {
            break;
        }
        int64_t i = 0;
        i = g;
        while (i < _arrayLen(a)) {
            int64_t t = a->data[i];
            if (( i - g ) < 0) {
                break;
            }
            if (( i - g ) >= _arrayLen(a)) {
                break;
            }
            int64_t j = 0;
            j = i - g;
            int64_t prev = 0;
            prev = i;
            while (a->data[j] > t) {
                a->data[prev] = a->data[j];
                int64_t next = j - g;
                prev = j;
                if (next < 0) {
                    break;
                }
                if (next >= _arrayLen(a)) {
                    break;
                }
                j = next;
            }
            a->data[prev] = t;
            if (( i + 1 ) >= _arrayLen(a)) {
                break;
            }
            i += 1;
        }
    }
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1007 = str_const("repeat ", 7);
    string_1008 = str_const(" count ", 7);
    string_1009 = str_const("first ", 6);
    string_1010 = str_const(" last ", 6);
    string_1011 = str_const("sum ", 4);
    _main();
    return 0;
}
void _main() {
    randomSeed = 0;
    MIN_INT = 0x8000000000000000;
    MAX_INT = 0x7fffffffffffffff;
    MIN_I32 = -2147483648;
    MAX_I32 = 4294967295;
    int64_t repeat = 10;
    int64_t count = 10;
    int64_t _t1 = org_bau_Env_argCount_0();
    if (_t1 > 1) {
        i8_array* _t2 = org_bau_Env_arg_1(1);
        int64_t _t3 = org_bau_Int_parseInt_1(_t2);
        repeat = _t3;
        _decUseStack(_t2, i8_array);
    }
    int64_t _t4 = org_bau_Env_argCount_0();
    if (_t4 > 2) {
        i8_array* _t5 = org_bau_Env_arg_1(2);
        int64_t _t6 = org_bau_Int_parseInt_1(_t5);
        count = _t6;
        _decUseStack(_t5, i8_array);
    }
    printf("repeat %lld count %lld\n", (long long)repeat, (long long)count);
    int_array* _t7 = int_array_new(count);
    _incUseStack(_t7);
    int_array* test = _t7;
    if (_arrayLen(test) <= 1) {
        return;
    }
    if (_arrayLen(test) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                int64_t _t8 = org_bau_Utils_random_0();
                test->data[i] = _t8;
                int64_t _next = i + 1;
                if (_next >= _arrayLen(test)) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    int64_t sum = 0;
    if (repeat > 0) {
        while (1 == 1) {
            int64_t loop = 0;
            while (1) {
                if (loop < 5) {
                    printf("first %lld last %lld\n", (long long)test->data[0], (long long)test->data[_arrayLen(test) - 1]);
                }
                reverse_int_array_int_3(test, 0, _arrayLen(test) - 1);
                shellSort_int_array_int_1(test);
                sum += test->data[0];
                int64_t _next = loop + 1;
                if (_next >= repeat) {
                    break;
                }
                loop = _next;
            }
            break;
        }
    }
    printf("sum %lld\n", (long long)sum);
    _decUseStack(test, int_array);
    _decUseStack(_t7, int_array);
    _end();
}
/*

type exception
An exception

fun getRandomSeed() int
Get the random seed.

fun random() int
Pseudo-random number generated using the Splitmix64 algorithm.

fun random(smallerThan int) int
Pseudo-random number between 0 and smallerThan (excluding).

fun setRandomSeed(seed int)
Set the random seed.

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

fun parsePositiveInt(s i8[]) int throws exception
throws an exception if the string does not match [0-9]+

*/
