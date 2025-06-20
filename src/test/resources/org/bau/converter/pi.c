#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <limits.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
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
#define _incUse(a)            {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT("++  %p line %d, from %d\n", a, __LINE__, (a)?(a)->_refCount:0); (a)->_refCount++;}}
#define _decUse(a, type)      {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT("--  %p line %d, from %d\n", a, __LINE__, (a)->_refCount);if(--((a)->_refCount) == 0)type##_free(a);}}
#define _incUseStack(a)       _incUse(a)
#define _decUseStack(a, type) _decUse(a, type)
int64_t arrayOutOfBounds(int64_t x, int64_t len) {
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
/* types */
typedef struct i8_array i8_array;
struct i8_array;
typedef struct i32_array i32_array;
struct i32_array;
typedef struct int_array int_array;
struct int_array;
typedef struct org_bau_BigInt_bigInt org_bau_BigInt_bigInt;
struct org_bau_BigInt_bigInt;
struct i8_array {
    int32_t len;
    int8_t* data;
    int32_t _refCount;
};
i8_array* i8_array_new(uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(int8_t) * len);
    memset(result->data, 0, sizeof(int8_t) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct i32_array {
    int32_t len;
    int32_t* data;
    int32_t _refCount;
};
i32_array* i32_array_new(uint32_t len) {
    i32_array* result = _malloc(sizeof(i32_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(int32_t) * len);
    memset(result->data, 0, sizeof(int32_t) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct int_array {
    int32_t len;
    int64_t* data;
    int32_t _refCount;
};
int_array* int_array_new(uint32_t len) {
    int_array* result = _malloc(sizeof(int_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(int64_t) * len);
    memset(result->data, 0, sizeof(int64_t) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_BigInt_bigInt {
    int64_t negative;
    i32_array* data;
};
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_new() {
    org_bau_BigInt_bigInt result;
    result.negative = 0;
    result.data = 0;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
void eliminateDigit_1(int64_t d);
int64_t extractDigit_1(int64_t nth);
int32_t i32_1(int64_t x);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
int64_t int_1(int64_t x);
void nextTerm_1(int64_t k);
org_bau_BigInt_bigInt org_bau_BigInt_add_2(i32_array* a, i32_array* b);
int64_t org_bau_BigInt_arithmeticRightShift_2(int64_t x, int64_t n);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_1(i32_array* data);
int64_t org_bau_BigInt_compareUnsigned_2(int64_t a, int64_t b);
i32_array* org_bau_BigInt_copyOf_2(i32_array* a, int64_t newLen);
i32_array* org_bau_BigInt_copyOfRange_i32_array_i32_3(i32_array* a, int64_t from, int64_t to);
int64_t org_bau_BigInt_divideUnsigned_2(int64_t dividend, int64_t divisor);
org_bau_BigInt_bigInt org_bau_BigInt_multiply_2(i32_array* a, i32_array* b);
org_bau_BigInt_bigInt org_bau_BigInt_multiplyBig_2(i32_array* a, i32_array* b);
org_bau_BigInt_bigInt org_bau_BigInt_multiplySmall_2(int32_t a, i32_array* b);
org_bau_BigInt_bigInt org_bau_BigInt_newBigInt_1(int64_t value);
org_bau_BigInt_bigInt org_bau_BigInt_newBigIntShorten_2(i32_array* data, int64_t negative);
int64_t org_bau_BigInt_numberOfLeadingZeros_1(int64_t x);
org_bau_BigInt_bigInt org_bau_BigInt_subtract_2(i32_array* a, i32_array* b);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_add_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other);
int64_t org_bau_BigInt_bigInt_compareTo_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt o);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_divide_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other);
int64_t org_bau_BigInt_bigInt_intValue_1(org_bau_BigInt_bigInt this);
int64_t org_bau_BigInt_bigInt_len_1(org_bau_BigInt_bigInt this);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_multiply_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_multiplyInt_2(org_bau_BigInt_bigInt this, int64_t value);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_negate_1(org_bau_BigInt_bigInt this);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_shiftLeft_2(org_bau_BigInt_bigInt this, int64_t n);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_shiftRight_2(org_bau_BigInt_bigInt this, int64_t n);
int64_t org_bau_BigInt_bigInt_signum_1(org_bau_BigInt_bigInt this);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_subtract_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other);
int64_t shiftLeft_2(int64_t a, int64_t b);
int32_t shiftRight_i32_2(int32_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
void i8_array_free(i8_array* x);
void i32_array_free(i32_array* x);
void int_array_free(int_array* x);
void org_bau_BigInt_bigInt_free(org_bau_BigInt_bigInt* x);
void org_bau_BigInt_bigInt_copy(org_bau_BigInt_bigInt* x);
void i8_array_free(i8_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void i32_array_free(i32_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void int_array_free(int_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void org_bau_BigInt_bigInt_free(org_bau_BigInt_bigInt* x) {
    _decUse(x->data, i32_array);
}
void org_bau_BigInt_bigInt_copy(org_bau_BigInt_bigInt* x) {
    _incUse(x->data);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = (int8_t*) data;
    return result;
}
i8_array* string_1016;
i8_array* string_1017;
int64_t I32_MIN_VALUE;
int64_t I32_MAX_VALUE;
int64_t INT_MIN_VALUE;
org_bau_BigInt_bigInt tmp1;
org_bau_BigInt_bigInt tmp2;
org_bau_BigInt_bigInt acc;
org_bau_BigInt_bigInt den;
org_bau_BigInt_bigInt num;
void eliminateDigit_1(int64_t d) {
    org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_multiplyInt_2(den, d);
    org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_subtract_2(acc, _t0);
    org_bau_BigInt_bigInt_copy(&_t1);
    org_bau_BigInt_bigInt_free(&acc);
    acc = _t1;
    org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_multiplyInt_2(acc, 10);
    org_bau_BigInt_bigInt_copy(&_t2);
    org_bau_BigInt_bigInt_free(&acc);
    acc = _t2;
    org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_multiplyInt_2(num, 10);
    org_bau_BigInt_bigInt_copy(&_t3);
    org_bau_BigInt_bigInt_free(&num);
    num = _t3;
    org_bau_BigInt_bigInt_free(&_t3);
    org_bau_BigInt_bigInt_free(&_t2);
    org_bau_BigInt_bigInt_free(&_t1);
    org_bau_BigInt_bigInt_free(&_t0);
}
int64_t extractDigit_1(int64_t nth) {
    org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_multiplyInt_2(num, nth);
    org_bau_BigInt_bigInt_copy(&_t0);
    org_bau_BigInt_bigInt_free(&tmp1);
    tmp1 = _t0;
    org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_add_2(tmp1, acc);
    org_bau_BigInt_bigInt_copy(&_t1);
    org_bau_BigInt_bigInt_free(&tmp2);
    tmp2 = _t1;
    org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_divide_2(tmp2, den);
    org_bau_BigInt_bigInt_copy(&_t2);
    org_bau_BigInt_bigInt_free(&tmp1);
    tmp1 = _t2;
    int64_t _t3 = org_bau_BigInt_bigInt_intValue_1(tmp1);
    org_bau_BigInt_bigInt_free(&_t2);
    org_bau_BigInt_bigInt_free(&_t1);
    org_bau_BigInt_bigInt_free(&_t0);
    return _t3;
}
int32_t i32_1(int64_t x) {
    return x;
}
int64_t idiv_2(int64_t a, int64_t b) {
    if (b != 0) return a / b;
    if (a == 0) return 0;
    return a > 0 ? LLONG_MAX : LLONG_MIN;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
int64_t int_1(int64_t x) {
    return x;
}
void nextTerm_1(int64_t k) {
    org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_shiftLeft_2(num, 1);
    org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_add_2(acc, _t0);
    org_bau_BigInt_bigInt_copy(&_t1);
    org_bau_BigInt_bigInt_free(&acc);
    acc = _t1;
    org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_multiplyInt_2(acc, ( k * 2 ) + 1);
    org_bau_BigInt_bigInt_copy(&_t2);
    org_bau_BigInt_bigInt_free(&acc);
    acc = _t2;
    org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_multiplyInt_2(den, ( k * 2 ) + 1);
    org_bau_BigInt_bigInt_copy(&_t3);
    org_bau_BigInt_bigInt_free(&den);
    den = _t3;
    org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_multiplyInt_2(num, k);
    org_bau_BigInt_bigInt_copy(&_t4);
    org_bau_BigInt_bigInt_free(&num);
    num = _t4;
    org_bau_BigInt_bigInt_free(&_t4);
    org_bau_BigInt_bigInt_free(&_t3);
    org_bau_BigInt_bigInt_free(&_t2);
    org_bau_BigInt_bigInt_free(&_t1);
    org_bau_BigInt_bigInt_free(&_t0);
}
org_bau_BigInt_bigInt org_bau_BigInt_add_2(i32_array* a, i32_array* b) {
    if (a->len < b->len) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_add_2(b, a);
        return _t0;
    }
    i32_array* result = org_bau_BigInt_copyOf_2(a, a->len);
    int64_t carry = 0;
    if (a->len > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                int64_t x = result->data[idx_2(i, result->len)] & 4294967295;
                int64_t _t1 = 0;
                if (i >= b->len) {
                    _t1 = 0;
                } else {
                    _t1 = b->data[idx_2(i, b->len)] & 4294967295;
                }
                int64_t y = _t1;
                int64_t z = ( x + y ) + carry;
                int32_t _t2 = i32_1(z);
                result->data[idx_2(i, result->len)] = _t2;
                carry = shiftRight_int_2(z, 32);
                int64_t _next = i + 1;
                if (_next >= a->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    if (carry == 0) {
        org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_1(result);
        _decUseStack(result, i32_array);
        return _t3;
    }
    i32_array* r2 = org_bau_BigInt_copyOf_2(result, result->len + 1);
    r2->data[idx_2(r2->len - 1, r2->len)] = 1;
    org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_1(r2);
    _decUseStack(r2, i32_array);
    _decUseStack(result, i32_array);
    return _t4;
}
int64_t org_bau_BigInt_arithmeticRightShift_2(int64_t x, int64_t n) {
    return (int64_t) (((uint64_t) x) >> n);
    int64_t _r0 = (shiftRight_int_2(x, n)) | (shiftLeft_2((0 - (x < 0)), (64 - n)));
    return _r0;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_1(i32_array* data) {
    org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_new();
    _t0.negative = 0;
    _incUseStack(data);
    _t0.data = data;
    return _t0;
}
int64_t org_bau_BigInt_compareUnsigned_2(int64_t a, int64_t b) {
    a += 0x8000000000000000;
    b += 0x8000000000000000;
    if (a == b) {
        return 0;
    }
    if (a < b) {
        return -1;
    }
    return 1;
}
i32_array* org_bau_BigInt_copyOf_2(i32_array* a, int64_t newLen) {
    _incUseStack(a);
    int64_t copyLen = newLen;
    if (a->len < newLen) {
        copyLen = a->len;
    }
    i32_array* result1 = _malloc(sizeof(i32_array));
    _traceMalloc(result1);
    result1->len = newLen;
    result1->data = _malloc(sizeof(int32_t) * newLen);
    if (newLen > copyLen) {
        memset(result1->data + copyLen, 0, sizeof(int32_t) * (newLen - copyLen));
    }
    _traceMalloc(result1->data);
    result1->_refCount = 1;
    memcpy(result1->data, a->data, copyLen * sizeof(int32_t));
    _decUseStack(a, i32_array);
    return result1;
    i32_array* _t0 = i32_array_new(newLen);
    _incUseStack(_t0);
    i32_array* result = _t0;
    int64_t i = 0;
    while (1 == 1) {
        int64_t _t1 = i < newLen;
        if (_t1) {
            int64_t _t2 = i < a->len;
            _t1 = _t2;
        }
        if (!(_t1)) {
            break;
        }
        result->data[idx_2(i, result->len)] = a->data[idx_2(i, a->len)];
        i += 1;
    }
    _decUseStack(_t0, i32_array);
    _decUseStack(a, i32_array);
    return result;
}
i32_array* org_bau_BigInt_copyOfRange_i32_array_i32_3(i32_array* a, int64_t from, int64_t to) {
    _incUseStack(a);
    int64_t len = to - from;
    i32_array* _t0 = i32_array_new(len);
    _incUseStack(_t0);
    i32_array* x = _t0;
    int64_t i = 0;
    while (1 == 1) {
        int64_t _t1 = ( i + from ) < to;
        if (_t1) {
            int64_t _t2 = i < a->len;
            _t1 = _t2;
        }
        if (!(_t1)) {
            break;
        }
        x->data[idx_2(i, x->len)] = a->data[idx_2(i + from, a->len)];
        i += 1;
    }
    _decUseStack(_t0, i32_array);
    _decUseStack(a, i32_array);
    return x;
}
int64_t org_bau_BigInt_divideUnsigned_2(int64_t dividend, int64_t divisor) {
    if (divisor < 0) {
        int64_t _r0 = shiftRight_int_2((dividend & ( ~ (dividend - divisor) )), 63);
        return _r0;
    }
    int64_t q = shiftLeft_2((idiv_2((shiftRight_int_2(dividend, 1)), divisor)), 1);
    int64_t r = dividend - (q * divisor);
    int64_t _r1 = q + (shiftRight_int_2((r | ( ~ (r - divisor) )), 63));
    return _r1;
}
org_bau_BigInt_bigInt org_bau_BigInt_multiply_2(i32_array* a, i32_array* b) {
    if (a->len < b->len) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_multiply_2(b, a);
        return _t0;
    }
    org_bau_BigInt_bigInt _t1 = org_bau_BigInt_multiplyBig_2(a, b);
    return _t1;
}
org_bau_BigInt_bigInt org_bau_BigInt_multiplyBig_2(i32_array* a, i32_array* b) {
    if (a->len == 1) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_multiplySmall_2(a->data[idx_2(0, a->len)], b);
        return _t0;
    } else if (b->len == 1) {
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_multiplySmall_2(b->data[idx_2(0, b->len)], a);
        return _t1;
    }
    i32_array* _t2 = i32_array_new(a->len + b->len);
    _incUseStack(_t2);
    i32_array* result = _t2;
    if (a->len > 0) {
        while (1 == 1) {
            int64_t ai = 0;
            while (1) {
                int64_t ax = a->data[ai] & 4294967295;
                int64_t carry = 0;
                int32_t i = i32_1(ai);
                if (b->len > 0) {
                    while (1 == 1) {
                        int64_t bi = 0;
                        while (1) {
                            int64_t bx = b->data[bi] & 4294967295;
                            int64_t z = ( ax * bx ) + (result->data[idx_2(i, result->len)] & 4294967295) + carry;
                            int32_t _t3 = i32_1(z);
                            result->data[idx_2(i, result->len)] = _t3;
                            carry = shiftRight_int_2(z, 32);
                            i += 1;
                            int64_t _next = bi + 1;
                            if (_next >= b->len) {
                                break;
                            }
                            bi = _next;
                        }
                        break;
                    }
                }
                while (carry > 0) {
                    int64_t z = result->data[idx_2(i, result->len)] + carry;
                    int32_t _t4 = i32_1(z);
                    result->data[idx_2(i, result->len)] = _t4;
                    carry = shiftRight_int_2(z, 32);
                    i += 1;
                }
                int64_t _next = ai + 1;
                if (_next >= a->len) {
                    break;
                }
                ai = _next;
            }
            break;
        }
    }
    org_bau_BigInt_bigInt _t5 = org_bau_BigInt_newBigIntShorten_2(result, 0);
    _decUseStack(result, i32_array);
    _decUseStack(_t2, i32_array);
    return _t5;
}
org_bau_BigInt_bigInt org_bau_BigInt_multiplySmall_2(int32_t a, i32_array* b) {
    if (a == 1) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_1(b);
        return _t0;
    }
    int32_t newSize = b->len;
    if (( shiftRight_i32_2((b->data[idx_2(b->len - 1, b->len)] | a), 16) ) != 0) {
        newSize += 1;
    }
    i32_array* result = org_bau_BigInt_copyOf_2(b, newSize);
    int64_t ax = a & 4294967295;
    int64_t carry = 0;
    if (result->len > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                int64_t bx = result->data[i] & 4294967295;
                int64_t z = ( ax * bx ) + carry;
                int32_t _t1 = i32_1(z);
                result->data[i] = _t1;
                carry = shiftRight_int_2(z, 32);
                int64_t _next = i + 1;
                if (_next >= result->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    if (carry) {
        result->data[idx_2(result->len - 1, result->len)] = carry;
    }
    org_bau_BigInt_bigInt _t2 = org_bau_BigInt_newBigIntShorten_2(result, 0);
    _decUseStack(result, i32_array);
    return _t2;
}
org_bau_BigInt_bigInt org_bau_BigInt_newBigInt_1(int64_t value) {
    if (value == 0) {
        i32_array* _t0 = i32_array_new(0);
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_1(_t0);
        _decUseStack(_t0, i32_array);
        return _t1;
    } else if (value < 0) {
        if (value >= -2147483648) {
            i32_array* _t2 = i32_array_new(1);
            org_bau_BigInt_bigInt x = org_bau_BigInt_bigInt_1(_t2);
            x.data->data[idx_2(0, x.data->len)] = - value;
            x.negative = 1;
            _decUseStack(_t2, i32_array);
            return x;
        } else if (value > (-9223372036854775807LL-1LL)) {
            i32_array* _t3 = i32_array_new(2);
            org_bau_BigInt_bigInt x = org_bau_BigInt_bigInt_1(_t3);
            x.data->data[idx_2(0, x.data->len)] = - value;
            x.data->data[idx_2(1, x.data->len)] = shiftRight_int_2(- value, 32);
            x.negative = 1;
            _decUseStack(_t3, i32_array);
            return x;
        } else {
            i32_array* _t4 = i32_array_new(2);
            org_bau_BigInt_bigInt x = org_bau_BigInt_bigInt_1(_t4);
            x.data->data[idx_2(0, x.data->len)] = 0;
            x.data->data[idx_2(1, x.data->len)] = 0x80000000;
            x.negative = 1;
            _decUseStack(_t4, i32_array);
            return x;
        }
    } else {
        if (value <= 4294967295) {
            i32_array* _t5 = i32_array_new(1);
            org_bau_BigInt_bigInt x = org_bau_BigInt_bigInt_1(_t5);
            x.data->data[idx_2(0, x.data->len)] = value;
            _decUseStack(_t5, i32_array);
            return x;
        } else {
            i32_array* _t6 = i32_array_new(2);
            org_bau_BigInt_bigInt x = org_bau_BigInt_bigInt_1(_t6);
            x.data->data[idx_2(0, x.data->len)] = value;
            x.data->data[idx_2(1, x.data->len)] = shiftRight_int_2(value, 32);
            _decUseStack(_t6, i32_array);
            return x;
        }
    }
}
org_bau_BigInt_bigInt org_bau_BigInt_newBigIntShorten_2(i32_array* data, int64_t negative) {
    int32_t newLen = data->len;
    while (1 == 1) {
        int64_t _t0 = newLen > 0;
        if (_t0) {
            int64_t _t1 = data->data[newLen - 1] == 0;
            _t0 = _t1;
        }
        if (!(_t0)) {
            break;
        }
        newLen -= 1;
    }
    if (newLen == 0) {
        negative = 0;
    }
    i32_array* _t2 = NULL;
    if (newLen == data->len) {
        _incUseStack(data);
        _decUseStack(_t2, i32_array);
        _t2 = data;
    } else {
        _decUseStack(_t2, i32_array);
        _t2 = org_bau_BigInt_copyOf_2(data, newLen);
    }
    _incUseStack(_t2);
    i32_array* d2 = _t2;
    org_bau_BigInt_bigInt result = org_bau_BigInt_bigInt_1(d2);
    result.negative = negative;
    _decUseStack(d2, i32_array);
    _decUseStack(_t2, i32_array);
    return result;
}
int64_t org_bau_BigInt_numberOfLeadingZeros_1(int64_t x) {
    return x == 0 ? 64 : __builtin_clzll(x);
    if (x <= 0) {
        int64_t _t0 = 0;
        if (x == 0) {
            _t0 = 64;
        } else {
            _t0 = 0;
        }
        return _t0;
    }
    int64_t n = 63;
    int64_t shift = 32;
    while (shift > 0) {
        if (x >= ( shiftLeft_2(1, shift) )) {
            n -= shift;
            x >>= shift;
        }
        shift >>= 1;
    }
    return n;
}
org_bau_BigInt_bigInt org_bau_BigInt_subtract_2(i32_array* a, i32_array* b) {
    i32_array* result = org_bau_BigInt_copyOf_2(a, a->len);
    int64_t carry = 0;
    if (a->len > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                int64_t x = result->data[idx_2(i, result->len)] & 4294967295;
                int64_t y = 0;
                if (i < b->len) {
                    y = b->data[i] & 4294967295;
                }
                int64_t z = ( x - y ) - carry;
                carry = (shiftRight_int_2(z, 63)) & 1;
                int32_t _t0 = i32_1(z);
                result->data[idx_2(i, result->len)] = _t0;
                int64_t _next = i + 1;
                if (_next >= a->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    org_bau_BigInt_bigInt _t1 = org_bau_BigInt_newBigIntShorten_2(result, 0);
    _decUseStack(result, i32_array);
    return _t1;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_add_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other) {
    org_bau_BigInt_bigInt_copy(&this);
    org_bau_BigInt_bigInt_copy(&other);
    if (this.data->len == 0) {
        org_bau_BigInt_bigInt_free(&this);
        return other;
    } else if (other.data->len == 0) {
        org_bau_BigInt_bigInt_free(&other);
        return this;
    }
    if (this.negative) {
        if (other.negative) {
            org_bau_BigInt_bigInt _t0 = org_bau_BigInt_add_2(this.data, other.data);
            org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_negate_1(_t0);
            org_bau_BigInt_bigInt_free(&_t0);
            org_bau_BigInt_bigInt_free(&other);
            org_bau_BigInt_bigInt_free(&this);
            return _t1;
        }
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_negate_1(this);
        org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_subtract_2(_t2, other);
        org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_negate_1(_t3);
        org_bau_BigInt_bigInt_free(&_t3);
        org_bau_BigInt_bigInt_free(&_t2);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t4;
    }
    if (other.negative) {
        org_bau_BigInt_bigInt _t5 = org_bau_BigInt_bigInt_negate_1(other);
        org_bau_BigInt_bigInt _t6 = org_bau_BigInt_bigInt_subtract_2(this, _t5);
        org_bau_BigInt_bigInt_free(&_t5);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t6;
    }
    org_bau_BigInt_bigInt _t7 = org_bau_BigInt_add_2(this.data, other.data);
    org_bau_BigInt_bigInt_free(&other);
    org_bau_BigInt_bigInt_free(&this);
    return _t7;
}
int64_t org_bau_BigInt_bigInt_compareTo_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt o) {
    if (this.negative != o.negative) {
        int64_t _t0 = 0;
        if (this.negative) {
            _t0 = -1;
        } else {
            _t0 = 1;
        }
        return _t0;
    }
    if (this.negative) {
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_negate_1(this);
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_negate_1(o);
        int64_t _t3 = org_bau_BigInt_bigInt_compareTo_2(_t1, _t2);
        int64_t _r0 = - _t3;
        org_bau_BigInt_bigInt_free(&_t2);
        org_bau_BigInt_bigInt_free(&_t1);
        return _r0;
    }
    int64_t len = org_bau_BigInt_bigInt_len_1(this);
    int64_t oLen = org_bau_BigInt_bigInt_len_1(o);
    if (len != oLen) {
        int64_t _t4 = 0;
        if (len > oLen) {
            _t4 = 1;
        } else {
            _t4 = -1;
        }
        return _t4;
    }
    int64_t i = this.data->len - 1;
    while (i >= 0) {
        int64_t x = this.data->data[i] & 4294967295;
        int64_t y = o.data->data[idx_2(i, o.data->len)] & 4294967295;
        if (x != y) {
            int64_t _t5 = 0;
            if (x > y) {
                _t5 = 1;
            } else {
                _t5 = -1;
            }
            return _t5;
        }
        i -= 1;
    }
    return 0;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_divide_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other) {
    org_bau_BigInt_bigInt_copy(&this);
    org_bau_BigInt_bigInt_copy(&other);
    if (other.data->len == 0) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_newBigInt_1(0);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t0;
    } else if (this.negative != other.negative) {
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_negate_1(other);
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_divide_2(this, _t1);
        org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_negate_1(_t2);
        org_bau_BigInt_bigInt_free(&_t2);
        org_bau_BigInt_bigInt_free(&_t1);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t3;
    } else if (this.negative) {
        org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_negate_1(this);
        org_bau_BigInt_bigInt _t5 = org_bau_BigInt_bigInt_negate_1(other);
        org_bau_BigInt_bigInt _t6 = org_bau_BigInt_bigInt_divide_2(_t4, _t5);
        org_bau_BigInt_bigInt_free(&_t5);
        org_bau_BigInt_bigInt_free(&_t4);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t6;
    }
    int64_t cmp = org_bau_BigInt_bigInt_compareTo_2(this, other);
    if (cmp < 0) {
        org_bau_BigInt_bigInt _t7 = org_bau_BigInt_newBigInt_1(0);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t7;
    } else if (cmp == 0) {
        org_bau_BigInt_bigInt _t8 = org_bau_BigInt_newBigInt_1(1);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t8;
    }
    int64_t _t9 = this.data->len > 2;
    if (_t9) {
        int64_t _t10 = ( org_bau_BigInt_bigInt_len_1(this) - org_bau_BigInt_bigInt_len_1(other) ) < 16;
        _t9 = _t10;
    }
    if (_t9) {
        int64_t _t11 = org_bau_BigInt_bigInt_len_1(other);
        org_bau_BigInt_bigInt _t12 = org_bau_BigInt_bigInt_shiftRight_2(this, _t11 - 16);
        int64_t x = org_bau_BigInt_bigInt_intValue_1(_t12);
        int64_t _t13 = org_bau_BigInt_bigInt_len_1(other);
        org_bau_BigInt_bigInt _t14 = org_bau_BigInt_bigInt_shiftRight_2(other, _t13 - 16);
        int64_t y = org_bau_BigInt_bigInt_intValue_1(_t14);
        int64_t z = idiv_2(x, y);
        int64_t _t15 = z == ( idiv_2(x, (y + 1)) );
        if (_t15) {
            int64_t _t16 = z == ( idiv_2(x, (y - 1)) );
            _t15 = _t16;
        }
        if (_t15) {
            org_bau_BigInt_bigInt _t17 = org_bau_BigInt_newBigInt_1(z);
            org_bau_BigInt_bigInt_free(&_t14);
            org_bau_BigInt_bigInt_free(&_t12);
            org_bau_BigInt_bigInt_free(&other);
            org_bau_BigInt_bigInt_free(&this);
            return _t17;
        }
        org_bau_BigInt_bigInt_free(&_t14);
        org_bau_BigInt_bigInt_free(&_t12);
    }
    int64_t otherLen = org_bau_BigInt_bigInt_len_1(other);
    int64_t shift = (32 - otherLen) & 31;
    if (otherLen >= 128) {
        shift = 32;
    } else if (otherLen <= 32) {
        shift += 32;
    }
    org_bau_BigInt_bigInt _t18 = org_bau_BigInt_bigInt_shiftLeft_2(this, shift);
    _incUseStack(_t18.data);
    i32_array* u = _t18.data;
    org_bau_BigInt_bigInt _t19 = org_bau_BigInt_bigInt_shiftLeft_2(other, shift);
    _incUseStack(_t19.data);
    i32_array* vn = _t19.data;
    int32_t m = u->len;
    int32_t n = vn->len;
    i32_array* _t20 = i32_array_new(( m - n ) + 1);
    _incUseStack(_t20);
    i32_array* q = _t20;
    int64_t b = 4294967296;
    i32_array* un = org_bau_BigInt_copyOf_2(u, u->len + 1);
    int32_t j = m - n;
    while (j >= 0) {
        int64_t aa = ((un->data[idx_2(j + n, un->len)] & 4294967295) * 4294967296) + (un->data[idx_2(( j + n ) - 1, un->len)] & 4294967295);
        int64_t bb = vn->data[n - 1] & 4294967295;
        int64_t qhat = org_bau_BigInt_divideUnsigned_2(aa, bb);
        int64_t rhat = aa - ( qhat * bb );
        while (1 == 1) {
            int64_t _t21 = qhat >= 4294967296;
            if (!(_t21)) {
                int64_t _t22 = org_bau_BigInt_compareUnsigned_2(qhat * (vn->data[n - 2] & 4294967295), (rhat * 4294967296) + (un->data[idx_2(( j + n ) - 2, un->len)] & 4294967295)) > 0;
                _t21 = _t22;
            }
            if (!(_t21)) {
                break;
            }
            qhat -= 1;
            rhat = rhat + (vn->data[n - 1] & 4294967295);
            if (rhat >= 4294967296) {
                break;
            }
        }
        int64_t carry = 0;
        if (n > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    int64_t p = qhat * (vn->data[idx_2(i, vn->len)] & 4294967295);
                    int64_t t = (un->data[idx_2(i + j, un->len)] & 4294967295) - carry - (p & 4294967295);
                    int64_t _t23 = int_1(t);
                    un->data[idx_2(i + j, un->len)] = _t23;
                    int64_t _t24 = org_bau_BigInt_arithmeticRightShift_2(t, 32);
                    carry = (shiftRight_int_2(p, 32)) - _t24;
                    int64_t _next = i + 1;
                    if (_next >= n) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        int64_t t = (un->data[idx_2(j + n, un->len)] & 4294967295) - carry;
        int64_t _t25 = int_1(t);
        un->data[idx_2(j + n, un->len)] = _t25;
        int64_t _t26 = int_1(qhat);
        q->data[idx_2(j, q->len)] = _t26;
        if (t < 0) {
            q->data[idx_2(j, q->len)] -= 1;
            if (j == 0) {
                break;
            }
            carry = 0;
            if (n > 0) {
                while (1 == 1) {
                    int64_t i = 0;
                    while (1) {
                        t = (un->data[idx_2(i + j, un->len)] & 4294967295) + (vn->data[idx_2(i, vn->len)] & 4294967295) + carry;
                        int64_t _t27 = int_1(t);
                        un->data[idx_2(i + j, un->len)] = _t27;
                        carry = shiftRight_int_2(t, 32);
                        int64_t _next = i + 1;
                        if (_next >= n) {
                            break;
                        }
                        i = _next;
                    }
                    break;
                }
            }
            int64_t _t28 = int_1((un->data[idx_2(j + n, un->len)] & 4294967295) + carry);
            un->data[idx_2(j + n, un->len)] = _t28;
        }
        j -= 1;
    }
    org_bau_BigInt_bigInt _t29 = org_bau_BigInt_bigInt_1(q);
    _decUseStack(un, i32_array);
    _decUseStack(q, i32_array);
    _decUseStack(_t20, i32_array);
    _decUseStack(vn, i32_array);
    org_bau_BigInt_bigInt_free(&_t19);
    _decUseStack(u, i32_array);
    org_bau_BigInt_bigInt_free(&_t18);
    org_bau_BigInt_bigInt_free(&other);
    org_bau_BigInt_bigInt_free(&this);
    return _t29;
}
int64_t org_bau_BigInt_bigInt_intValue_1(org_bau_BigInt_bigInt this) {
    int64_t _t0 = org_bau_BigInt_bigInt_signum_1(this);
    int64_t _t1 = 0;
    if (this.data->len == 0) {
        _t1 = 0;
    } else {
        _t1 = this.data->data[idx_2(0, this.data->len)] & 4294967295;
    }
    int64_t _r0 = _t0 * _t1;
    return _r0;
}
int64_t org_bau_BigInt_bigInt_len_1(org_bau_BigInt_bigInt this) {
    if (this.data->len == 0) {
        return 0;
    }
    int64_t _t0 = org_bau_BigInt_numberOfLeadingZeros_1(this.data->data[idx_2(this.data->len - 1, this.data->len)] & 4294967295);
    int64_t lastLen = 64 - _t0;
    int64_t _r0 = (this.data->len - 1) * 32 + lastLen;
    return _r0;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_multiply_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other) {
    org_bau_BigInt_bigInt_copy(&this);
    org_bau_BigInt_bigInt_copy(&other);
    int64_t thisLen = org_bau_BigInt_bigInt_len_1(this);
    int64_t otherLen = org_bau_BigInt_bigInt_len_1(other);
    if (thisLen == 0) {
        org_bau_BigInt_bigInt_free(&other);
        return this;
    } else if (otherLen == 0) {
        org_bau_BigInt_bigInt_free(&this);
        return other;
    }
    if (this.negative != other.negative) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_negate_1(other);
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_multiply_2(this, _t0);
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_negate_1(_t1);
        org_bau_BigInt_bigInt_free(&_t1);
        org_bau_BigInt_bigInt_free(&_t0);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t2;
    }
    if (thisLen > otherLen) {
        org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_multiply_2(other, this);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t3;
    } else if (thisLen == 1) {
        if (this.negative) {
            org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_negate_1(other);
            org_bau_BigInt_bigInt_free(&other);
            org_bau_BigInt_bigInt_free(&this);
            return _t4;
        }
        org_bau_BigInt_bigInt_free(&this);
        return other;
    }
    org_bau_BigInt_bigInt _t5 = org_bau_BigInt_multiply_2(this.data, other.data);
    org_bau_BigInt_bigInt_free(&other);
    org_bau_BigInt_bigInt_free(&this);
    return _t5;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_multiplyInt_2(org_bau_BigInt_bigInt this, int64_t value) {
    org_bau_BigInt_bigInt_copy(&this);
    int64_t _t0 = value >= 2147483648;
    if (!(_t0)) {
        int64_t _t1 = value <= -2147483648;
        _t0 = _t1;
    }
    if ((_t0)) {
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_newBigInt_1(value);
        org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_multiply_2(this, _t2);
        org_bau_BigInt_bigInt_free(&_t2);
        org_bau_BigInt_bigInt_free(&this);
        return _t3;
    }
    int32_t other = i32_1(value);
    int64_t thisLen = org_bau_BigInt_bigInt_len_1(this);
    if (thisLen == 0) {
        return this;
    } else if (other == 0) {
        org_bau_BigInt_bigInt _t4 = org_bau_BigInt_newBigInt_1(other);
        org_bau_BigInt_bigInt_free(&this);
        return _t4;
    }
    int64_t otherNegative = other < 0;
    if (this.negative != otherNegative) {
        org_bau_BigInt_bigInt _t5 = org_bau_BigInt_bigInt_multiplyInt_2(this, - other);
        org_bau_BigInt_bigInt _t6 = org_bau_BigInt_bigInt_negate_1(_t5);
        org_bau_BigInt_bigInt_free(&_t5);
        org_bau_BigInt_bigInt_free(&this);
        return _t6;
    } else if (thisLen == 1) {
        if (this.negative) {
            org_bau_BigInt_bigInt _t7 = org_bau_BigInt_newBigInt_1(- other);
            org_bau_BigInt_bigInt_free(&this);
            return _t7;
        }
        org_bau_BigInt_bigInt _t8 = org_bau_BigInt_newBigInt_1(other);
        org_bau_BigInt_bigInt_free(&this);
        return _t8;
    }
    if (otherNegative) {
        other = - other;
    }
    org_bau_BigInt_bigInt _t9 = org_bau_BigInt_multiplySmall_2(other, this.data);
    org_bau_BigInt_bigInt_free(&this);
    return _t9;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_negate_1(org_bau_BigInt_bigInt this) {
    org_bau_BigInt_bigInt_copy(&this);
    if (this.data->len == 0) {
        return this;
    }
    org_bau_BigInt_bigInt x = org_bau_BigInt_bigInt_1(this.data);
    x.negative = !(this.negative);
    org_bau_BigInt_bigInt_free(&this);
    return x;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_shiftLeft_2(org_bau_BigInt_bigInt this, int64_t n) {
    org_bau_BigInt_bigInt_copy(&this);
    if (n < 0) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_shiftRight_2(this, - n);
        org_bau_BigInt_bigInt_free(&this);
        return _t0;
    }
    if (this.data->len == 0) {
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_newBigInt_1(0);
        org_bau_BigInt_bigInt_free(&this);
        return _t1;
    }
    int64_t len = org_bau_BigInt_bigInt_len_1(this);
    int64_t len2 = len + n;
    i32_array* _t2 = i32_array_new(idiv_2((len2 + 31), 32));
    _incUseStack(_t2);
    i32_array* data2 = _t2;
    int64_t carry = 0;
    int64_t i = 0;
    while (i < len) {
        int64_t old = this.data->data[idx_2(shiftRight_int_2(i, 5), this.data->len)] & 4294967295;
        int32_t _t3 = i32_1((shiftLeft_2(old, (n & 31))) | carry);
        data2->data[idx_2(shiftRight_int_2((i + n), 5), data2->len)] = _t3;
        carry = shiftRight_int_2(old, (32 - (n & 31)));
        i += 32;
    }
    data2->data[idx_2(data2->len - 1, data2->len)] |= carry;
    org_bau_BigInt_bigInt _t4 = org_bau_BigInt_newBigIntShorten_2(data2, this.negative);
    _decUseStack(data2, i32_array);
    _decUseStack(_t2, i32_array);
    org_bau_BigInt_bigInt_free(&this);
    return _t4;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_shiftRight_2(org_bau_BigInt_bigInt this, int64_t n) {
    org_bau_BigInt_bigInt_copy(&this);
    if (n < 0) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_shiftLeft_2(this, - n);
        org_bau_BigInt_bigInt_free(&this);
        return _t0;
    }
    int64_t _t1 = org_bau_BigInt_bigInt_len_1(this);
    int64_t len2 = _t1 - n;
    if (len2 <= 0) {
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_newBigInt_1(0);
        org_bau_BigInt_bigInt_free(&this);
        return _t2;
    }
    i32_array* data2 = org_bau_BigInt_copyOfRange_i32_array_i32_3(this.data, idiv_2(n, 32), this.data->len);
    int64_t carry = 0;
    int64_t i = data2->len - 1;
    while (i >= 0) {
        int64_t x = data2->data[i] & 4294967295;
        int32_t _t3 = i32_1(shiftRight_int_2((carry | x), (n & 31)));
        data2->data[i] = _t3;
        carry = shiftLeft_2(x, 32);
        i -= 1;
    }
    org_bau_BigInt_bigInt _t4 = org_bau_BigInt_newBigIntShorten_2(data2, this.negative);
    _decUseStack(data2, i32_array);
    org_bau_BigInt_bigInt_free(&this);
    return _t4;
}
int64_t org_bau_BigInt_bigInt_signum_1(org_bau_BigInt_bigInt this) {
    if (this.data->len == 0) {
        return 0;
    } else if (this.negative) {
        return -1;
    }
    return 1;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_subtract_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other) {
    org_bau_BigInt_bigInt_copy(&this);
    org_bau_BigInt_bigInt_copy(&other);
    if (this.data->len == 0) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_negate_1(other);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t0;
    } else if (other.data->len == 0) {
        org_bau_BigInt_bigInt_free(&other);
        return this;
    }
    if (this.negative) {
        if (!(other.negative)) {
            org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_negate_1(this);
            org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_add_2(_t1, other);
            org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_negate_1(_t2);
            org_bau_BigInt_bigInt_free(&_t2);
            org_bau_BigInt_bigInt_free(&_t1);
            org_bau_BigInt_bigInt_free(&other);
            org_bau_BigInt_bigInt_free(&this);
            return _t3;
        }
        org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_negate_1(this);
        org_bau_BigInt_bigInt _t5 = org_bau_BigInt_bigInt_negate_1(other);
        org_bau_BigInt_bigInt _t6 = org_bau_BigInt_bigInt_subtract_2(_t4, _t5);
        org_bau_BigInt_bigInt _t7 = org_bau_BigInt_bigInt_negate_1(_t6);
        org_bau_BigInt_bigInt_free(&_t6);
        org_bau_BigInt_bigInt_free(&_t5);
        org_bau_BigInt_bigInt_free(&_t4);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t7;
    }
    if (other.negative) {
        org_bau_BigInt_bigInt _t8 = org_bau_BigInt_bigInt_negate_1(other);
        org_bau_BigInt_bigInt _t9 = org_bau_BigInt_bigInt_add_2(this, _t8);
        org_bau_BigInt_bigInt_free(&_t8);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t9;
    }
    int64_t _t10 = org_bau_BigInt_bigInt_compareTo_2(this, other);
    if (_t10 < 0) {
        org_bau_BigInt_bigInt _t11 = org_bau_BigInt_bigInt_subtract_2(other, this);
        org_bau_BigInt_bigInt _t12 = org_bau_BigInt_bigInt_negate_1(_t11);
        org_bau_BigInt_bigInt_free(&_t11);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t12;
    }
    org_bau_BigInt_bigInt _t13 = org_bau_BigInt_subtract_2(this.data, other.data);
    org_bau_BigInt_bigInt_free(&other);
    org_bau_BigInt_bigInt_free(&this);
    return _t13;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int32_t shiftRight_i32_2(int32_t a, int64_t b) {
    return ((uint32_t) a) >> b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1016 = str_const(" : ", 3);
    string_1017 = str_const("dummy: ", 7);
    I32_MIN_VALUE = -2147483648;
    I32_MAX_VALUE = 4294967295;
    INT_MIN_VALUE = (-9223372036854775807LL-1LL);
    tmp1 = org_bau_BigInt_newBigInt_1(0);
    tmp2 = org_bau_BigInt_newBigInt_1(0);
    acc = org_bau_BigInt_newBigInt_1(0);
    den = org_bau_BigInt_newBigInt_1(1);
    num = org_bau_BigInt_newBigInt_1(1);
    int64_t n = 100;
    int64_t i = 0;
    int64_t k = 0;
    int64_t L = 10;
    i8_array* _t0 = i8_array_new(10);
    _incUseStack(_t0);
    i8_array* buff = _t0;
    int64_t j = 0;
    int64_t dummy = 0;
    while (i < n) {
        k += 1;
        nextTerm_1(k);
        int64_t _t1 = org_bau_BigInt_bigInt_compareTo_2(num, acc);
        if (_t1 > 0) {
            continue;
        }
        int64_t d = extractDigit_1(3);
        int64_t _t2 = extractDigit_1(4);
        if (d != _t2) {
            continue;
        }
        eliminateDigit_1(d);
        dummy += d;
        buff->data[j] = 48 + d;
        j += 1;
        i += 1;
        if (j == 10) {
            printf("%.*s : %lld\n", buff->len, buff->data, (long long)i);
            j = 0;
        }
    }
    printf("dummy: %lld\n", (long long)dummy);
    _decUseStack(buff, i8_array);
    _decUseStack(_t0, i8_array);
    org_bau_BigInt_bigInt_free(&tmp1);
    org_bau_BigInt_bigInt_free(&tmp2);
    org_bau_BigInt_bigInt_free(&acc);
    org_bau_BigInt_bigInt_free(&den);
    org_bau_BigInt_bigInt_free(&num);
    _end();
    return 0;
}
/*

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

fun newBigInt(value int) bigInt
create a new bigInt

fun bigInt add(other bigInt) bigInt
addition

fun bigInt compareTo(o bigInt) int
compare to another value

fun bigInt divide(other bigInt) bigInt
division

fun bigInt intValue() int
get the int value (at most 32 bits)

fun bigInt len() int
length in bits

fun bigInt multiply(other bigInt) bigInt
multiplication

fun bigInt multiplyInt(value int) bigInt
multiply by an integer

fun bigInt negate() bigInt
negate

fun bigInt shiftLeft(n int) bigInt
left shift

fun bigInt shiftRight(n int) bigInt
right shift

fun bigInt signum() int
get the signum

fun bigInt subtract(other bigInt) bigInt
subtraction

fun bigInt toStr() i8[]
convert to string

*/
