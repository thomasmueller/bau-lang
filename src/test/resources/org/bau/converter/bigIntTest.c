#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <limits.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
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
typedef struct org_bau_BigInt_str org_bau_BigInt_str;
struct org_bau_BigInt_str;
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
struct org_bau_BigInt_str {
    i8_array* data;
};
org_bau_BigInt_str org_bau_BigInt_str_new() {
    org_bau_BigInt_str result;
    result.data = 0;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
int32_t i32_1(int64_t x);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
int64_t imod_2(int64_t a, int64_t b);
int64_t int_1(int64_t x);
org_bau_BigInt_bigInt org_bau_BigInt_add_2(i32_array* a, i32_array* b);
int64_t org_bau_BigInt_arithmeticRightShift_2(int64_t x, int64_t n);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_1(i32_array* data);
int64_t org_bau_BigInt_compareUnsigned_2(int64_t a, int64_t b);
i32_array* org_bau_BigInt_copyOf_2(i32_array* a, int64_t newLen);
i32_array* org_bau_BigInt_copyOfRange_i32_array_i32_3(i32_array* a, int64_t from, int64_t to);
i8_array* org_bau_BigInt_copyOfRange_i8_array_i8_3(i8_array* a, int64_t from, int64_t to);
int64_t org_bau_BigInt_divUnsigned_2(int64_t divnd, int64_t divisor);
int64_t org_bau_BigInt_intToStr_4(int64_t n, i8_array* buff, int64_t start, int64_t size);
org_bau_BigInt_bigInt org_bau_BigInt_mul_2(i32_array* a, i32_array* b);
org_bau_BigInt_bigInt org_bau_BigInt_mulBig_2(i32_array* a, i32_array* b);
org_bau_BigInt_bigInt org_bau_BigInt_mulSmall_2(int32_t a, i32_array* b);
org_bau_BigInt_bigInt org_bau_BigInt_newBigInt_1(int64_t value);
org_bau_BigInt_bigInt org_bau_BigInt_newBigIntShorten_2(i32_array* data, int64_t negative);
int64_t org_bau_BigInt_numberOfLeadingZeros_1(int64_t x);
org_bau_BigInt_str org_bau_BigInt_str_1(i8_array* data);
org_bau_BigInt_bigInt org_bau_BigInt_sub_2(i32_array* a, i32_array* b);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_add_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other);
int64_t org_bau_BigInt_bigInt_compare_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt o);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_div_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other);
int64_t org_bau_BigInt_bigInt_len_1(org_bau_BigInt_bigInt this);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_mul_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_neg_1(org_bau_BigInt_bigInt this);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_shiftLeft_2(org_bau_BigInt_bigInt this, int64_t n);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_shiftRight_2(org_bau_BigInt_bigInt this, int64_t n);
int64_t org_bau_BigInt_bigInt_signum_1(org_bau_BigInt_bigInt this);
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_sub_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other);
int64_t org_bau_BigInt_bigInt_toInt_1(org_bau_BigInt_bigInt this);
i8_array* org_bau_BigInt_bigInt_toStr_1(org_bau_BigInt_bigInt this);
int64_t shiftLeft_2(int64_t a, int64_t b);
int32_t shiftRight_i32_2(int32_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
void i8_array_free(i8_array* x);
void i32_array_free(i32_array* x);
void int_array_free(int_array* x);
void org_bau_BigInt_bigInt_free(org_bau_BigInt_bigInt* x);
void org_bau_BigInt_bigInt_copy(org_bau_BigInt_bigInt* x);
void org_bau_BigInt_str_free(org_bau_BigInt_str* x);
void org_bau_BigInt_str_copy(org_bau_BigInt_str* x);
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
void org_bau_BigInt_str_free(org_bau_BigInt_str* x) {
    _decUse(x->data, i8_array);
}
void org_bau_BigInt_str_copy(org_bau_BigInt_str* x) {
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
i8_array* string_1018;
i8_array* string_1019;
i8_array* string_1020;
i8_array* string_1021;
i8_array* string_1022;
i8_array* string_1023;
i8_array* string_1024;
i8_array* string_1025;
i8_array* string_1026;
i8_array* string_1027;
i8_array* string_1028;
i8_array* string_1029;
i8_array* string_1030;
i8_array* string_1031;
i8_array* string_1032;
i8_array* string_1033;
i8_array* string_1034;
i8_array* string_1035;
i8_array* string_1036;
i8_array* string_1037;
i8_array* string_1038;
i8_array* string_1039;
int64_t I32_MIN_VALUE;
int64_t I32_MAX_VALUE;
int64_t INT_MIN_VALUE;
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
int64_t imod_2(int64_t a, int64_t b) {
    if (b != 0) return a % b;
    return 0;
}
int64_t int_1(int64_t x) {
    return x;
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
i8_array* org_bau_BigInt_copyOfRange_i8_array_i8_3(i8_array* a, int64_t from, int64_t to) {
    _incUseStack(a);
    int64_t len = to - from;
    i8_array* _t0 = i8_array_new(len);
    _incUseStack(_t0);
    i8_array* x = _t0;
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
    _decUseStack(_t0, i8_array);
    _decUseStack(a, i8_array);
    return x;
}
int64_t org_bau_BigInt_divUnsigned_2(int64_t divnd, int64_t divisor) {
    if (divisor < 0) {
        int64_t _r0 = shiftRight_int_2((divnd & ( ~ (divnd - divisor) )), 63);
        return _r0;
    }
    int64_t q = shiftLeft_2((idiv_2((shiftRight_int_2(divnd, 1)), divisor)), 1);
    int64_t r = divnd - (q * divisor);
    int64_t _r1 = q + (shiftRight_int_2((r | ( ~ (r - divisor) )), 63));
    return _r1;
}
int64_t org_bau_BigInt_intToStr_4(int64_t n, i8_array* buff, int64_t start, int64_t size) {
    int64_t i = 0;
    while (i < size) {
        buff->data[idx_2(start + i, buff->len)] = 48 + (imod_2(n, 10));
        n = idiv_2(n, 10);
        i += 1;
    }
    int64_t _r0 = i + start;
    return _r0;
}
org_bau_BigInt_bigInt org_bau_BigInt_mul_2(i32_array* a, i32_array* b) {
    if (a->len < b->len) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_mul_2(b, a);
        return _t0;
    }
    org_bau_BigInt_bigInt _t1 = org_bau_BigInt_mulBig_2(a, b);
    return _t1;
}
org_bau_BigInt_bigInt org_bau_BigInt_mulBig_2(i32_array* a, i32_array* b) {
    if (a->len == 1) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_mulSmall_2(a->data[idx_2(0, a->len)], b);
        return _t0;
    } else if (b->len == 1) {
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_mulSmall_2(b->data[idx_2(0, b->len)], a);
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
org_bau_BigInt_bigInt org_bau_BigInt_mulSmall_2(int32_t a, i32_array* b) {
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
    return x == 0 ? 64 : _clzll(x);
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
org_bau_BigInt_str org_bau_BigInt_str_1(i8_array* data) {
    org_bau_BigInt_str _t1 = org_bau_BigInt_str_new();
    _incUseStack(data);
    _t1.data = data;
    return _t1;
}
org_bau_BigInt_bigInt org_bau_BigInt_sub_2(i32_array* a, i32_array* b) {
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
            org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_neg_1(_t0);
            org_bau_BigInt_bigInt_free(&_t0);
            org_bau_BigInt_bigInt_free(&other);
            org_bau_BigInt_bigInt_free(&this);
            return _t1;
        }
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_neg_1(this);
        org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_sub_2(_t2, other);
        org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_neg_1(_t3);
        org_bau_BigInt_bigInt_free(&_t3);
        org_bau_BigInt_bigInt_free(&_t2);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t4;
    }
    if (other.negative) {
        org_bau_BigInt_bigInt _t5 = org_bau_BigInt_bigInt_neg_1(other);
        org_bau_BigInt_bigInt _t6 = org_bau_BigInt_bigInt_sub_2(this, _t5);
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
int64_t org_bau_BigInt_bigInt_compare_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt o) {
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
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_neg_1(this);
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_neg_1(o);
        int64_t _t3 = org_bau_BigInt_bigInt_compare_2(_t1, _t2);
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
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_div_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other) {
    org_bau_BigInt_bigInt_copy(&this);
    org_bau_BigInt_bigInt_copy(&other);
    if (other.data->len == 0) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_newBigInt_1(0);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t0;
    } else if (this.negative != other.negative) {
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_neg_1(other);
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_div_2(this, _t1);
        org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_neg_1(_t2);
        org_bau_BigInt_bigInt_free(&_t2);
        org_bau_BigInt_bigInt_free(&_t1);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t3;
    } else if (this.negative) {
        org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_neg_1(this);
        org_bau_BigInt_bigInt _t5 = org_bau_BigInt_bigInt_neg_1(other);
        org_bau_BigInt_bigInt _t6 = org_bau_BigInt_bigInt_div_2(_t4, _t5);
        org_bau_BigInt_bigInt_free(&_t5);
        org_bau_BigInt_bigInt_free(&_t4);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t6;
    }
    int64_t cmp = org_bau_BigInt_bigInt_compare_2(this, other);
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
        int64_t x = org_bau_BigInt_bigInt_toInt_1(_t12);
        int64_t _t13 = org_bau_BigInt_bigInt_len_1(other);
        org_bau_BigInt_bigInt _t14 = org_bau_BigInt_bigInt_shiftRight_2(other, _t13 - 16);
        int64_t y = org_bau_BigInt_bigInt_toInt_1(_t14);
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
        int64_t qhat = org_bau_BigInt_divUnsigned_2(aa, bb);
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
int64_t org_bau_BigInt_bigInt_len_1(org_bau_BigInt_bigInt this) {
    if (this.data->len == 0) {
        return 0;
    }
    int64_t _t0 = org_bau_BigInt_numberOfLeadingZeros_1(this.data->data[idx_2(this.data->len - 1, this.data->len)] & 4294967295);
    int64_t lastLen = 64 - _t0;
    int64_t _r0 = (this.data->len - 1) * 32 + lastLen;
    return _r0;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_mul_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other) {
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
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_neg_1(other);
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_mul_2(this, _t0);
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_neg_1(_t1);
        org_bau_BigInt_bigInt_free(&_t1);
        org_bau_BigInt_bigInt_free(&_t0);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t2;
    }
    if (thisLen > otherLen) {
        org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_mul_2(other, this);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t3;
    } else if (thisLen == 1) {
        if (this.negative) {
            org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_neg_1(other);
            org_bau_BigInt_bigInt_free(&other);
            org_bau_BigInt_bigInt_free(&this);
            return _t4;
        }
        org_bau_BigInt_bigInt_free(&this);
        return other;
    }
    org_bau_BigInt_bigInt _t5 = org_bau_BigInt_mul_2(this.data, other.data);
    org_bau_BigInt_bigInt_free(&other);
    org_bau_BigInt_bigInt_free(&this);
    return _t5;
}
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_neg_1(org_bau_BigInt_bigInt this) {
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
org_bau_BigInt_bigInt org_bau_BigInt_bigInt_sub_2(org_bau_BigInt_bigInt this, org_bau_BigInt_bigInt other) {
    org_bau_BigInt_bigInt_copy(&this);
    org_bau_BigInt_bigInt_copy(&other);
    if (this.data->len == 0) {
        org_bau_BigInt_bigInt _t0 = org_bau_BigInt_bigInt_neg_1(other);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t0;
    } else if (other.data->len == 0) {
        org_bau_BigInt_bigInt_free(&other);
        return this;
    }
    if (this.negative) {
        if (!(other.negative)) {
            org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_neg_1(this);
            org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_add_2(_t1, other);
            org_bau_BigInt_bigInt _t3 = org_bau_BigInt_bigInt_neg_1(_t2);
            org_bau_BigInt_bigInt_free(&_t2);
            org_bau_BigInt_bigInt_free(&_t1);
            org_bau_BigInt_bigInt_free(&other);
            org_bau_BigInt_bigInt_free(&this);
            return _t3;
        }
        org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_neg_1(this);
        org_bau_BigInt_bigInt _t5 = org_bau_BigInt_bigInt_neg_1(other);
        org_bau_BigInt_bigInt _t6 = org_bau_BigInt_bigInt_sub_2(_t4, _t5);
        org_bau_BigInt_bigInt _t7 = org_bau_BigInt_bigInt_neg_1(_t6);
        org_bau_BigInt_bigInt_free(&_t6);
        org_bau_BigInt_bigInt_free(&_t5);
        org_bau_BigInt_bigInt_free(&_t4);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t7;
    }
    if (other.negative) {
        org_bau_BigInt_bigInt _t8 = org_bau_BigInt_bigInt_neg_1(other);
        org_bau_BigInt_bigInt _t9 = org_bau_BigInt_bigInt_add_2(this, _t8);
        org_bau_BigInt_bigInt_free(&_t8);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t9;
    }
    int64_t _t10 = org_bau_BigInt_bigInt_compare_2(this, other);
    if (_t10 < 0) {
        org_bau_BigInt_bigInt _t11 = org_bau_BigInt_bigInt_sub_2(other, this);
        org_bau_BigInt_bigInt _t12 = org_bau_BigInt_bigInt_neg_1(_t11);
        org_bau_BigInt_bigInt_free(&_t11);
        org_bau_BigInt_bigInt_free(&other);
        org_bau_BigInt_bigInt_free(&this);
        return _t12;
    }
    org_bau_BigInt_bigInt _t13 = org_bau_BigInt_sub_2(this.data, other.data);
    org_bau_BigInt_bigInt_free(&other);
    org_bau_BigInt_bigInt_free(&this);
    return _t13;
}
int64_t org_bau_BigInt_bigInt_toInt_1(org_bau_BigInt_bigInt this) {
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
i8_array* org_bau_BigInt_bigInt_toStr_1(org_bau_BigInt_bigInt this) {
    org_bau_BigInt_bigInt_copy(&this);
    org_bau_BigInt_bigInt n = this;
    i8_array* _t0 = i8_array_new(50);
    org_bau_BigInt_str buff = org_bau_BigInt_str_1(_t0);
    int64_t start = 0;
    if (this.negative) {
        buff.data->data[idx_2(0, buff.data->len)] = 45;
        start = 1;
        org_bau_BigInt_bigInt _t1 = org_bau_BigInt_bigInt_neg_1(n);
        org_bau_BigInt_bigInt_copy(&_t1);
        org_bau_BigInt_bigInt_free(&n);
        n = _t1;
        org_bau_BigInt_bigInt_free(&_t1);
    }
    int64_t i = start;
    org_bau_BigInt_bigInt group = org_bau_BigInt_newBigInt_1(1000000000);
    while (1) {
        org_bau_BigInt_bigInt next = org_bau_BigInt_bigInt_div_2(n, group);
        org_bau_BigInt_bigInt _t2 = org_bau_BigInt_bigInt_mul_2(next, group);
        org_bau_BigInt_bigInt remainder = org_bau_BigInt_bigInt_sub_2(n, _t2);
        int64_t val = org_bau_BigInt_bigInt_toInt_1(remainder);
        if (( i + 9 ) >= buff.data->len) {
            i8_array* _t3 = org_bau_BigInt_copyOfRange_i8_array_i8_3(buff.data, 0, buff.data->len * 2);
            org_bau_BigInt_str _t4 = org_bau_BigInt_str_1(_t3);
            org_bau_BigInt_str_copy(&_t4);
            org_bau_BigInt_str_free(&buff);
            buff = _t4;
            org_bau_BigInt_str_free(&_t4);
            _decUseStack(_t3, i8_array);
        }
        int64_t _t5 = org_bau_BigInt_intToStr_4(val, buff.data, i, 9);
        i = _t5;
        org_bau_BigInt_bigInt_copy(&next);
        org_bau_BigInt_bigInt_free(&n);
        n = next;
        int64_t _t6 = org_bau_BigInt_bigInt_signum_1(n);
        if (_t6 == 0) {
            org_bau_BigInt_bigInt_free(&remainder);
            org_bau_BigInt_bigInt_free(&_t2);
            org_bau_BigInt_bigInt_free(&next);
            break;
        }
        org_bau_BigInt_bigInt_free(&remainder);
        org_bau_BigInt_bigInt_free(&_t2);
        org_bau_BigInt_bigInt_free(&next);
    }
    while (1 == 1) {
        int64_t _t7 = i > ( start + 1 );
        if (_t7) {
            int64_t _t8 = buff.data->data[idx_2(i - 1, buff.data->len)] == 48;
            _t7 = _t8;
        }
        if (!(_t7)) {
            break;
        }
        i -= 1;
    }
    int64_t end = i;
    while (i > start) {
        i -= 1;
        int8_t temp = buff.data->data[idx_2(i, buff.data->len)];
        buff.data->data[idx_2(i, buff.data->len)] = buff.data->data[idx_2(start, buff.data->len)];
        buff.data->data[idx_2(start, buff.data->len)] = temp;
        start += 1;
    }
    i8_array* _t9 = org_bau_BigInt_copyOfRange_i8_array_i8_3(buff.data, 0, end);
    org_bau_BigInt_bigInt_free(&group);
    org_bau_BigInt_str_free(&buff);
    _decUseStack(_t0, i8_array);
    org_bau_BigInt_bigInt_free(&n);
    return _t9;
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
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1016 = str_const("assertion failed: ", 18);
    string_1017 = str_const("toInt(add(newBigInt(123), newBigInt(456))) = 579", 48);
    string_1018 = str_const("toInt(shiftRight(add(shiftLeft(newBigInt(123), 100), shiftLeft(newBigInt(456), 100)), 100)) = 579", 97);
    string_1019 = str_const("toInt(sub(newBigInt(1000), newBigInt(250))) = 750", 49);
    string_1020 = str_const("toInt(sub(newBigInt(250), newBigInt(1000))) = -750", 50);
    string_1021 = str_const("toInt(mul(newBigInt(20), newBigInt(5))) = 100", 45);
    string_1022 = str_const("toInt(shiftRight(mul(shiftLeft(newBigInt(20), 100), shiftLeft(newBigInt(5), 100)), 200)) = 100", 94);
    string_1023 = str_const("toInt(div(newBigInt(100), newBigInt(5))) = 20", 45);
    string_1024 = str_const("toInt(div(shiftLeft(newBigInt(100), 100), shiftLeft(newBigInt(5), 100))) = 20", 77);
    string_1025 = str_const("toInt(shiftLeft(newBigInt(1), 3)) = 8", 37);
    string_1026 = str_const("toInt(shiftRight(newBigInt(16), 2)) = 4", 39);
    string_1027 = str_const("toInt(neg(newBigInt(50))) = -50", 31);
    string_1028 = str_const("toInt(neg(neg(newBigInt(50)))) = 50", 35);
    string_1029 = str_const("compare(newBigInt(10), newBigInt(20)) = -1", 42);
    string_1030 = str_const("compare(newBigInt(10), newBigInt(-20)) = 1", 42);
    string_1031 = str_const("compare(newBigInt(10), newBigInt(10)) = 0", 41);
    string_1032 = str_const("signum(newBigInt(123)) = 1", 26);
    string_1033 = str_const("signum(newBigInt(0)) = 0", 24);
    string_1034 = str_const("signum(newBigInt(-123)) = -1", 28);
    string_1035 = str_const("len(newBigInt(1)) = 1", 21);
    string_1036 = str_const("len(newBigInt(2)) = 2", 21);
    string_1037 = str_const("len(newBigInt(255)) = 8", 23);
    string_1038 = str_const("len(newBigInt(256)) = 9", 23);
    string_1039 = str_const("len(shiftLeft(newBigInt(256), 100)) = 109", 41);
    _main();
    return 0;
}
void _main() {
    I32_MIN_VALUE = -2147483648;
    I32_MAX_VALUE = 4294967295;
    INT_MIN_VALUE = (-9223372036854775807LL-1LL);
    org_bau_BigInt_bigInt _t2 = org_bau_BigInt_newBigInt_1(123);
    org_bau_BigInt_bigInt _t3 = org_bau_BigInt_newBigInt_1(456);
    org_bau_BigInt_bigInt _t4 = org_bau_BigInt_bigInt_add_2(_t2, _t3);
    int64_t _t5 = org_bau_BigInt_bigInt_toInt_1(_t4);
    if (!(( _t5 == 579 ))) {
        printf("assertion failed: toInt(add(newBigInt(123), newBigInt(456))) = 579\n");
    } else {
    }
    org_bau_BigInt_bigInt _t6 = org_bau_BigInt_newBigInt_1(123);
    org_bau_BigInt_bigInt _t7 = org_bau_BigInt_bigInt_shiftLeft_2(_t6, 100);
    org_bau_BigInt_bigInt _t8 = org_bau_BigInt_newBigInt_1(456);
    org_bau_BigInt_bigInt _t9 = org_bau_BigInt_bigInt_shiftLeft_2(_t8, 100);
    org_bau_BigInt_bigInt _t10 = org_bau_BigInt_bigInt_add_2(_t7, _t9);
    org_bau_BigInt_bigInt _t11 = org_bau_BigInt_bigInt_shiftRight_2(_t10, 100);
    int64_t _t12 = org_bau_BigInt_bigInt_toInt_1(_t11);
    if (!(( _t12 == 579 ))) {
        printf("assertion failed: toInt(shiftRight(add(shiftLeft(newBigInt(123), 100), shiftLeft(newBigInt(456), 100)), 100)) = 579\n");
    } else {
    }
    org_bau_BigInt_bigInt _t13 = org_bau_BigInt_newBigInt_1(1000);
    org_bau_BigInt_bigInt _t14 = org_bau_BigInt_newBigInt_1(250);
    org_bau_BigInt_bigInt _t15 = org_bau_BigInt_bigInt_sub_2(_t13, _t14);
    int64_t _t16 = org_bau_BigInt_bigInt_toInt_1(_t15);
    if (!(( _t16 == 750 ))) {
        printf("assertion failed: toInt(sub(newBigInt(1000), newBigInt(250))) = 750\n");
    } else {
    }
    org_bau_BigInt_bigInt _t17 = org_bau_BigInt_newBigInt_1(250);
    org_bau_BigInt_bigInt _t18 = org_bau_BigInt_newBigInt_1(1000);
    org_bau_BigInt_bigInt _t19 = org_bau_BigInt_bigInt_sub_2(_t17, _t18);
    int64_t _t20 = org_bau_BigInt_bigInt_toInt_1(_t19);
    if (!(( _t20 == -750 ))) {
        printf("assertion failed: toInt(sub(newBigInt(250), newBigInt(1000))) = -750\n");
    } else {
    }
    org_bau_BigInt_bigInt _t21 = org_bau_BigInt_newBigInt_1(20);
    org_bau_BigInt_bigInt _t22 = org_bau_BigInt_newBigInt_1(5);
    org_bau_BigInt_bigInt _t23 = org_bau_BigInt_bigInt_mul_2(_t21, _t22);
    int64_t _t24 = org_bau_BigInt_bigInt_toInt_1(_t23);
    if (!(( _t24 == 100 ))) {
        printf("assertion failed: toInt(mul(newBigInt(20), newBigInt(5))) = 100\n");
    } else {
    }
    org_bau_BigInt_bigInt _t25 = org_bau_BigInt_newBigInt_1(20);
    org_bau_BigInt_bigInt _t26 = org_bau_BigInt_bigInt_shiftLeft_2(_t25, 100);
    org_bau_BigInt_bigInt _t27 = org_bau_BigInt_newBigInt_1(5);
    org_bau_BigInt_bigInt _t28 = org_bau_BigInt_bigInt_shiftLeft_2(_t27, 100);
    org_bau_BigInt_bigInt _t29 = org_bau_BigInt_bigInt_mul_2(_t26, _t28);
    org_bau_BigInt_bigInt _t30 = org_bau_BigInt_bigInt_shiftRight_2(_t29, 200);
    int64_t _t31 = org_bau_BigInt_bigInt_toInt_1(_t30);
    if (!(( _t31 == 100 ))) {
        printf("assertion failed: toInt(shiftRight(mul(shiftLeft(newBigInt(20), 100), shiftLeft(newBigInt(5), 100)), 200)) = 100\n");
    } else {
    }
    org_bau_BigInt_bigInt _t32 = org_bau_BigInt_newBigInt_1(100);
    org_bau_BigInt_bigInt _t33 = org_bau_BigInt_newBigInt_1(5);
    org_bau_BigInt_bigInt _t34 = org_bau_BigInt_bigInt_div_2(_t32, _t33);
    int64_t _t35 = org_bau_BigInt_bigInt_toInt_1(_t34);
    if (!(( _t35 == 20 ))) {
        printf("assertion failed: toInt(div(newBigInt(100), newBigInt(5))) = 20\n");
    } else {
    }
    org_bau_BigInt_bigInt _t36 = org_bau_BigInt_newBigInt_1(100);
    org_bau_BigInt_bigInt _t37 = org_bau_BigInt_bigInt_shiftLeft_2(_t36, 100);
    org_bau_BigInt_bigInt _t38 = org_bau_BigInt_newBigInt_1(5);
    org_bau_BigInt_bigInt _t39 = org_bau_BigInt_bigInt_shiftLeft_2(_t38, 100);
    org_bau_BigInt_bigInt _t40 = org_bau_BigInt_bigInt_div_2(_t37, _t39);
    int64_t _t41 = org_bau_BigInt_bigInt_toInt_1(_t40);
    if (!(( _t41 == 20 ))) {
        printf("assertion failed: toInt(div(shiftLeft(newBigInt(100), 100), shiftLeft(newBigInt(5), 100))) = 20\n");
    } else {
    }
    org_bau_BigInt_bigInt _t42 = org_bau_BigInt_newBigInt_1(1);
    org_bau_BigInt_bigInt _t43 = org_bau_BigInt_bigInt_shiftLeft_2(_t42, 3);
    int64_t _t44 = org_bau_BigInt_bigInt_toInt_1(_t43);
    if (!(( _t44 == 8 ))) {
        printf("assertion failed: toInt(shiftLeft(newBigInt(1), 3)) = 8\n");
    } else {
    }
    org_bau_BigInt_bigInt _t45 = org_bau_BigInt_newBigInt_1(16);
    org_bau_BigInt_bigInt _t46 = org_bau_BigInt_bigInt_shiftRight_2(_t45, 2);
    int64_t _t47 = org_bau_BigInt_bigInt_toInt_1(_t46);
    if (!(( _t47 == 4 ))) {
        printf("assertion failed: toInt(shiftRight(newBigInt(16), 2)) = 4\n");
    } else {
    }
    org_bau_BigInt_bigInt _t48 = org_bau_BigInt_newBigInt_1(50);
    org_bau_BigInt_bigInt _t49 = org_bau_BigInt_bigInt_neg_1(_t48);
    int64_t _t50 = org_bau_BigInt_bigInt_toInt_1(_t49);
    if (!(( _t50 == -50 ))) {
        printf("assertion failed: toInt(neg(newBigInt(50))) = -50\n");
    } else {
    }
    org_bau_BigInt_bigInt _t51 = org_bau_BigInt_newBigInt_1(50);
    org_bau_BigInt_bigInt _t52 = org_bau_BigInt_bigInt_neg_1(_t51);
    org_bau_BigInt_bigInt _t53 = org_bau_BigInt_bigInt_neg_1(_t52);
    int64_t _t54 = org_bau_BigInt_bigInt_toInt_1(_t53);
    if (!(( _t54 == 50 ))) {
        printf("assertion failed: toInt(neg(neg(newBigInt(50)))) = 50\n");
    } else {
    }
    org_bau_BigInt_bigInt _t55 = org_bau_BigInt_newBigInt_1(10);
    org_bau_BigInt_bigInt _t56 = org_bau_BigInt_newBigInt_1(20);
    int64_t _t57 = org_bau_BigInt_bigInt_compare_2(_t55, _t56);
    if (!(( _t57 == -1 ))) {
        printf("assertion failed: compare(newBigInt(10), newBigInt(20)) = -1\n");
    } else {
    }
    org_bau_BigInt_bigInt _t58 = org_bau_BigInt_newBigInt_1(10);
    org_bau_BigInt_bigInt _t59 = org_bau_BigInt_newBigInt_1(-20);
    int64_t _t60 = org_bau_BigInt_bigInt_compare_2(_t58, _t59);
    if (!(( _t60 == 1 ))) {
        printf("assertion failed: compare(newBigInt(10), newBigInt(-20)) = 1\n");
    } else {
    }
    org_bau_BigInt_bigInt _t61 = org_bau_BigInt_newBigInt_1(10);
    org_bau_BigInt_bigInt _t62 = org_bau_BigInt_newBigInt_1(10);
    int64_t _t63 = org_bau_BigInt_bigInt_compare_2(_t61, _t62);
    if (!(( _t63 == 0 ))) {
        printf("assertion failed: compare(newBigInt(10), newBigInt(10)) = 0\n");
    } else {
    }
    org_bau_BigInt_bigInt _t64 = org_bau_BigInt_newBigInt_1(123);
    int64_t _t65 = org_bau_BigInt_bigInt_signum_1(_t64);
    if (!(( _t65 == 1 ))) {
        printf("assertion failed: signum(newBigInt(123)) = 1\n");
    } else {
    }
    org_bau_BigInt_bigInt _t66 = org_bau_BigInt_newBigInt_1(0);
    int64_t _t67 = org_bau_BigInt_bigInt_signum_1(_t66);
    if (!(( _t67 == 0 ))) {
        printf("assertion failed: signum(newBigInt(0)) = 0\n");
    } else {
    }
    org_bau_BigInt_bigInt _t68 = org_bau_BigInt_newBigInt_1(-123);
    int64_t _t69 = org_bau_BigInt_bigInt_signum_1(_t68);
    if (!(( _t69 == -1 ))) {
        printf("assertion failed: signum(newBigInt(-123)) = -1\n");
    } else {
    }
    org_bau_BigInt_bigInt _t70 = org_bau_BigInt_newBigInt_1(1);
    int64_t _t71 = org_bau_BigInt_bigInt_len_1(_t70);
    if (!(( _t71 == 1 ))) {
        printf("assertion failed: len(newBigInt(1)) = 1\n");
    } else {
    }
    org_bau_BigInt_bigInt _t72 = org_bau_BigInt_newBigInt_1(2);
    int64_t _t73 = org_bau_BigInt_bigInt_len_1(_t72);
    if (!(( _t73 == 2 ))) {
        printf("assertion failed: len(newBigInt(2)) = 2\n");
    } else {
    }
    org_bau_BigInt_bigInt _t74 = org_bau_BigInt_newBigInt_1(255);
    int64_t _t75 = org_bau_BigInt_bigInt_len_1(_t74);
    if (!(( _t75 == 8 ))) {
        printf("assertion failed: len(newBigInt(255)) = 8\n");
    } else {
    }
    org_bau_BigInt_bigInt _t76 = org_bau_BigInt_newBigInt_1(256);
    int64_t _t77 = org_bau_BigInt_bigInt_len_1(_t76);
    if (!(( _t77 == 9 ))) {
        printf("assertion failed: len(newBigInt(256)) = 9\n");
    } else {
    }
    org_bau_BigInt_bigInt _t78 = org_bau_BigInt_newBigInt_1(256);
    org_bau_BigInt_bigInt _t79 = org_bau_BigInt_bigInt_shiftLeft_2(_t78, 100);
    int64_t _t80 = org_bau_BigInt_bigInt_len_1(_t79);
    if (!(( _t80 == 109 ))) {
        printf("assertion failed: len(shiftLeft(newBigInt(256), 100)) = 109\n");
    } else {
    }
    org_bau_BigInt_bigInt _t81 = org_bau_BigInt_newBigInt_1(1234);
    org_bau_BigInt_bigInt _t82 = org_bau_BigInt_bigInt_shiftLeft_2(_t81, 100);
    org_bau_BigInt_bigInt _t83 = org_bau_BigInt_newBigInt_1(5678);
    org_bau_BigInt_bigInt _t84 = org_bau_BigInt_bigInt_shiftLeft_2(_t83, 100);
    org_bau_BigInt_bigInt _t85 = org_bau_BigInt_bigInt_mul_2(_t82, _t84);
    i8_array* _t86 = org_bau_BigInt_bigInt_toStr_1(_t85);
    printf("%.*s\n", _t86->len, _t86->data);
    _decUseStack(_t86, i8_array);
    org_bau_BigInt_bigInt_free(&_t85);
    org_bau_BigInt_bigInt_free(&_t84);
    org_bau_BigInt_bigInt_free(&_t83);
    org_bau_BigInt_bigInt_free(&_t82);
    org_bau_BigInt_bigInt_free(&_t81);
    org_bau_BigInt_bigInt_free(&_t79);
    org_bau_BigInt_bigInt_free(&_t78);
    org_bau_BigInt_bigInt_free(&_t76);
    org_bau_BigInt_bigInt_free(&_t74);
    org_bau_BigInt_bigInt_free(&_t72);
    org_bau_BigInt_bigInt_free(&_t70);
    org_bau_BigInt_bigInt_free(&_t68);
    org_bau_BigInt_bigInt_free(&_t66);
    org_bau_BigInt_bigInt_free(&_t64);
    org_bau_BigInt_bigInt_free(&_t62);
    org_bau_BigInt_bigInt_free(&_t61);
    org_bau_BigInt_bigInt_free(&_t59);
    org_bau_BigInt_bigInt_free(&_t58);
    org_bau_BigInt_bigInt_free(&_t56);
    org_bau_BigInt_bigInt_free(&_t55);
    org_bau_BigInt_bigInt_free(&_t53);
    org_bau_BigInt_bigInt_free(&_t52);
    org_bau_BigInt_bigInt_free(&_t51);
    org_bau_BigInt_bigInt_free(&_t49);
    org_bau_BigInt_bigInt_free(&_t48);
    org_bau_BigInt_bigInt_free(&_t46);
    org_bau_BigInt_bigInt_free(&_t45);
    org_bau_BigInt_bigInt_free(&_t43);
    org_bau_BigInt_bigInt_free(&_t42);
    org_bau_BigInt_bigInt_free(&_t40);
    org_bau_BigInt_bigInt_free(&_t39);
    org_bau_BigInt_bigInt_free(&_t38);
    org_bau_BigInt_bigInt_free(&_t37);
    org_bau_BigInt_bigInt_free(&_t36);
    org_bau_BigInt_bigInt_free(&_t34);
    org_bau_BigInt_bigInt_free(&_t33);
    org_bau_BigInt_bigInt_free(&_t32);
    org_bau_BigInt_bigInt_free(&_t30);
    org_bau_BigInt_bigInt_free(&_t29);
    org_bau_BigInt_bigInt_free(&_t28);
    org_bau_BigInt_bigInt_free(&_t27);
    org_bau_BigInt_bigInt_free(&_t26);
    org_bau_BigInt_bigInt_free(&_t25);
    org_bau_BigInt_bigInt_free(&_t23);
    org_bau_BigInt_bigInt_free(&_t22);
    org_bau_BigInt_bigInt_free(&_t21);
    org_bau_BigInt_bigInt_free(&_t19);
    org_bau_BigInt_bigInt_free(&_t18);
    org_bau_BigInt_bigInt_free(&_t17);
    org_bau_BigInt_bigInt_free(&_t15);
    org_bau_BigInt_bigInt_free(&_t14);
    org_bau_BigInt_bigInt_free(&_t13);
    org_bau_BigInt_bigInt_free(&_t11);
    org_bau_BigInt_bigInt_free(&_t10);
    org_bau_BigInt_bigInt_free(&_t9);
    org_bau_BigInt_bigInt_free(&_t8);
    org_bau_BigInt_bigInt_free(&_t7);
    org_bau_BigInt_bigInt_free(&_t6);
    org_bau_BigInt_bigInt_free(&_t4);
    org_bau_BigInt_bigInt_free(&_t3);
    org_bau_BigInt_bigInt_free(&_t2);
    _end();
}
/*

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

fun newBigInt(value int) bigInt
create a new bigInt

fun bigInt add(other bigInt) bigInt
addition

fun bigInt compare(o bigInt) int
compare to another value

fun bigInt div(other bigInt) bigInt
division

fun bigInt len() int
length in bits

fun bigInt mul(other bigInt) bigInt
multiplication

fun bigInt mulInt(value int) bigInt
mul by an integer

fun bigInt neg() bigInt
negate

fun bigInt shiftLeft(n int) bigInt
left shift

fun bigInt shiftRight(n int) bigInt
right shift

fun bigInt signum() int
get the signum

fun bigInt sub(other bigInt) bigInt
subion

fun bigInt toInt() int
get the int value (at most 32 bits)

fun bigInt toStr() i8[]
convert to string

*/
