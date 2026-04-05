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
    if ((tmmalloc_levelBitmap & (1ULL << index0)) != 0) {
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
typedef struct _ToBeFreed _ToBeFreed;
struct _ToBeFreed { void* obj; void (*free)(void*); };
#define FREE_STACK_MAX_RECURSION 2048
#define FREE_STACK_ARRAY_SIZE 1024
_ToBeFreed _toBeFreedStack[FREE_STACK_ARRAY_SIZE];
int _freeStackDraining = 0, _freeStackArrayPos = 0;
void _registerAndMaybeDrain(void* x, void (*free)(void*)) {
    if (_freeStackDraining < FREE_STACK_MAX_RECURSION || _freeStackArrayPos >= FREE_STACK_ARRAY_SIZE) {
        _freeStackDraining++; free(x); _freeStackDraining--; return; }
    _toBeFreedStack[_freeStackArrayPos].obj = x;
    _toBeFreedStack[_freeStackArrayPos].free = free;
    if (_freeStackDraining == FREE_STACK_MAX_RECURSION) {
        _freeStackDraining = FREE_STACK_MAX_RECURSION + 1;
        while(_freeStackArrayPos > 0) {
            _freeStackArrayPos--; void* n = _toBeFreedStack[_freeStackArrayPos].obj;
            void (*free)(void*) = _toBeFreedStack[_freeStackArrayPos].free;
            free(n);
        } _freeStackDraining = FREE_STACK_MAX_RECURSION; } }
/* types */
typedef struct int_array int_array;
struct int_array;
typedef struct Point Point;
struct Point;
typedef struct Point_array Point_array;
struct Point_array;
typedef struct org_bau_List_List_Point org_bau_List_List_Point;
struct org_bau_List_List_Point;
typedef struct org_bau_FilterMap_FilterMap_Point org_bau_FilterMap_FilterMap_Point;
struct org_bau_FilterMap_FilterMap_Point;
typedef struct org_bau_FilterMap_FilterMap_int org_bau_FilterMap_FilterMap_int;
struct org_bau_FilterMap_FilterMap_int;
typedef struct org_bau_List_List_int org_bau_List_List_int;
struct org_bau_List_List_int;
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
struct Point {
    int32_t _refCount;
    int64_t x;
    int64_t y;
};
Point* Point_new() {
    Point* result = _malloc(sizeof(Point));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct Point_array {
    int32_t len;
    int32_t _refCount;
    Point** data;
};
Point_array* Point_array_new(uint64_t len) {
    if (len < 0 || len >= (1L << 31)) arrayOutOfBounds(len, 1L << 31);
    Point_array* result = _malloc(sizeof(Point_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(Point*) * len);
    memset(result->data, 0, sizeof(Point*) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_List_List_Point {
    int32_t _refCount;
    Point_array* array;
    int64_t size;
};
org_bau_List_List_Point* org_bau_List_List_Point_new() {
    org_bau_List_List_Point* result = _malloc(sizeof(org_bau_List_List_Point));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_FilterMap_FilterMap_Point {
    int32_t _refCount;
    org_bau_List_List_Point* list;
};
org_bau_FilterMap_FilterMap_Point* org_bau_FilterMap_FilterMap_Point_new() {
    org_bau_FilterMap_FilterMap_Point* result = _malloc(sizeof(org_bau_FilterMap_FilterMap_Point));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_FilterMap_FilterMap_int {
    int32_t _refCount;
    org_bau_List_List_int* list;
};
org_bau_FilterMap_FilterMap_int* org_bau_FilterMap_FilterMap_int_new() {
    org_bau_FilterMap_FilterMap_int* result = _malloc(sizeof(org_bau_FilterMap_FilterMap_int));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_List_List_int {
    int32_t _refCount;
    int_array* array;
    int64_t size;
};
org_bau_List_List_int* org_bau_List_List_int_new() {
    org_bau_List_List_int* result = _malloc(sizeof(org_bau_List_List_int));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
Point* Point_0();
int64_t idx_2(int64_t x, int64_t len);
Point* newPoint_2(int64_t x, int64_t y);
org_bau_FilterMap_FilterMap_Point* org_bau_FilterMap_FilterMap_Point_1(org_bau_List_List_Point* list);
org_bau_FilterMap_FilterMap_int* org_bau_FilterMap_FilterMap_int_1(org_bau_List_List_int* list);
org_bau_FilterMap_FilterMap_Point* org_bau_FilterMap_from_Point_1(int64_t _T);
org_bau_FilterMap_FilterMap_int* org_bau_FilterMap_from_int_1(int64_t _T);
org_bau_FilterMap_FilterMap_Point* org_bau_FilterMap_FilterMap_Point_in_2(org_bau_FilterMap_FilterMap_Point* this, org_bau_List_List_Point* list);
org_bau_FilterMap_FilterMap_int* org_bau_FilterMap_FilterMap_int_in_2(org_bau_FilterMap_FilterMap_int* this, org_bau_List_List_int* list);
org_bau_List_List_int* org_bau_FilterMap_FilterMap_int_select_1(org_bau_FilterMap_FilterMap_int* this);
org_bau_List_List_Point* org_bau_List_List_Point_0();
org_bau_List_List_int* org_bau_List_List_int_0();
org_bau_List_List_Point* org_bau_List_newList_Point_1(int64_t _T);
void org_bau_List_List_Point_add_2(org_bau_List_List_Point* this, Point* x);
void org_bau_List_List_int_add_2(org_bau_List_List_int* this, int64_t x);
int64_t org_bau_List_List_int_get_2(org_bau_List_List_int* this, int64_t x);
int64_t org_bau_List_List_int_len_1(org_bau_List_List_int* this);
void int_array_free(int_array* x);
void Point_free(Point* x);
void Point_array_free(Point_array* x);
void org_bau_List_List_Point_free(org_bau_List_List_Point* x);
void org_bau_FilterMap_FilterMap_Point_free(org_bau_FilterMap_FilterMap_Point* x);
void org_bau_FilterMap_FilterMap_int_free(org_bau_FilterMap_FilterMap_int* x);
void org_bau_List_List_int_free(org_bau_List_List_int* x);
void int_array_free_0(int_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void int_array_free(int_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))int_array_free_0);
}
void Point_free_0(Point* x) {
    _free(x); _traceFree(x);
}
void Point_free(Point* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))Point_free_0);
}
void Point_array_free_0(Point_array* x) {
    for (int i = 0; i < _arrayLen(x); i++) _decUse(x->data[i], Point);
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void Point_array_free(Point_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))Point_array_free_0);
}
void org_bau_List_List_Point_free_0(org_bau_List_List_Point* x) {
    _decUse(x->array, Point_array);
    _free(x); _traceFree(x);
}
void org_bau_List_List_Point_free(org_bau_List_List_Point* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_List_List_Point_free_0);
}
void org_bau_FilterMap_FilterMap_Point_free_0(org_bau_FilterMap_FilterMap_Point* x) {
    _decUse(x->list, org_bau_List_List_Point);
    _free(x); _traceFree(x);
}
void org_bau_FilterMap_FilterMap_Point_free(org_bau_FilterMap_FilterMap_Point* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_FilterMap_FilterMap_Point_free_0);
}
void org_bau_FilterMap_FilterMap_int_free_0(org_bau_FilterMap_FilterMap_int* x) {
    _decUse(x->list, org_bau_List_List_int);
    _free(x); _traceFree(x);
}
void org_bau_FilterMap_FilterMap_int_free(org_bau_FilterMap_FilterMap_int* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_FilterMap_FilterMap_int_free_0);
}
void org_bau_List_List_int_free_0(org_bau_List_List_int* x) {
    _decUse(x->array, int_array);
    _free(x); _traceFree(x);
}
void org_bau_List_List_int_free(org_bau_List_List_int* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_List_List_int_free_0);
}
Point* Point_0() {
    Point* _t0 = Point_new();
    _t0->x = 0;
    _t0->y = 0;
    return _t0;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
Point* newPoint_2(int64_t x, int64_t y) {
    Point* p = Point_0();
    p->x = x;
    p->y = y;
    return p;
}
org_bau_FilterMap_FilterMap_Point* org_bau_FilterMap_FilterMap_Point_1(org_bau_List_List_Point* list) {
    org_bau_FilterMap_FilterMap_Point* _t0 = org_bau_FilterMap_FilterMap_Point_new();
    _incUseStack(list);
    _t0->list = list;
    return _t0;
}
org_bau_FilterMap_FilterMap_int* org_bau_FilterMap_FilterMap_int_1(org_bau_List_List_int* list) {
    org_bau_FilterMap_FilterMap_int* _t0 = org_bau_FilterMap_FilterMap_int_new();
    _incUseStack(list);
    _t0->list = list;
    return _t0;
}
org_bau_FilterMap_FilterMap_Point* org_bau_FilterMap_from_Point_1(int64_t _T) {
    org_bau_List_List_Point* _t0 = org_bau_List_List_Point_0();
    org_bau_FilterMap_FilterMap_Point* _t1 = org_bau_FilterMap_FilterMap_Point_1(_t0);
    _decUseStack(_t0, org_bau_List_List_Point);
    return _t1;
}
org_bau_FilterMap_FilterMap_int* org_bau_FilterMap_from_int_1(int64_t _T) {
    org_bau_List_List_int* _t0 = org_bau_List_List_int_0();
    org_bau_FilterMap_FilterMap_int* _t1 = org_bau_FilterMap_FilterMap_int_1(_t0);
    _decUseStack(_t0, org_bau_List_List_int);
    return _t1;
}
org_bau_FilterMap_FilterMap_Point* org_bau_FilterMap_FilterMap_Point_in_2(org_bau_FilterMap_FilterMap_Point* this, org_bau_List_List_Point* list) {
    _incUseStack(this);
    _incUseStack(list);
    _decUse(this->list, org_bau_List_List_Point);
    this->list = list;
    return this;
}
org_bau_FilterMap_FilterMap_int* org_bau_FilterMap_FilterMap_int_in_2(org_bau_FilterMap_FilterMap_int* this, org_bau_List_List_int* list) {
    _incUseStack(this);
    _incUseStack(list);
    _decUse(this->list, org_bau_List_List_int);
    this->list = list;
    return this;
}
org_bau_List_List_int* org_bau_FilterMap_FilterMap_int_select_1(org_bau_FilterMap_FilterMap_int* this) {
    _incUseStack(this->list);
    org_bau_List_List_int* _r0 = this->list;
    return _r0;
}
org_bau_List_List_Point* org_bau_List_List_Point_0() {
    org_bau_List_List_Point* _t0 = org_bau_List_List_Point_new();
    _t0->array = Point_array_new(0);
    _t0->size = 0;
    return _t0;
}
org_bau_List_List_int* org_bau_List_List_int_0() {
    org_bau_List_List_int* _t0 = org_bau_List_List_int_new();
    _t0->array = int_array_new(0);
    _t0->size = 0;
    return _t0;
}
org_bau_List_List_Point* org_bau_List_newList_Point_1(int64_t _T) {
    org_bau_List_List_Point* result = org_bau_List_List_Point_0();
    Point_array* _t0 = Point_array_new(4);
    _incUseStack(_t0);
    _decUse(result->array, Point_array);
    result->array = _t0;
    _decUseStack(_t0, Point_array);
    return result;
}
void org_bau_List_List_Point_add_2(org_bau_List_List_Point* this, Point* x) {
    _incUseStack(x);
    if (this->size >= _arrayLen(this->array)) {
        Point_array* _t1 = Point_array_new(_arrayLen(this->array) * 2);
        _incUseStack(_t1);
        Point_array* n = _t1;
        if (_arrayLen(this->array) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    _incUseStack(this->array->data[i]);
                    _decUse(n->data[idx_2(i, _arrayLen(n))], Point);
                    n->data[idx_2(i, _arrayLen(n))] = this->array->data[i];
                    if (( i + 1 ) >= _arrayLen(this->array)) {
                        break;
                    }
                    i = i + 1;
                }
                break;
            }
        }
        _incUseStack(n);
        _decUse(this->array, Point_array);
        this->array = n;
        _decUseStack(n, Point_array);
        _decUseStack(_t1, Point_array);
    }
    _incUseStack(x);
    _decUse(this->array->data[idx_2(this->size, _arrayLen(this->array))], Point);
    this->array->data[idx_2(this->size, _arrayLen(this->array))] = x;
    this->size += 1;
    _decUseStack(x, Point);
}
void org_bau_List_List_int_add_2(org_bau_List_List_int* this, int64_t x) {
    if (this->size >= _arrayLen(this->array)) {
        int_array* _t1 = int_array_new(_arrayLen(this->array) * 2);
        _incUseStack(_t1);
        int_array* n = _t1;
        if (_arrayLen(this->array) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    n->data[idx_2(i, _arrayLen(n))] = this->array->data[i];
                    if (( i + 1 ) >= _arrayLen(this->array)) {
                        break;
                    }
                    i = i + 1;
                }
                break;
            }
        }
        _incUseStack(n);
        _decUse(this->array, int_array);
        this->array = n;
        _decUseStack(n, int_array);
        _decUseStack(_t1, int_array);
    }
    this->array->data[idx_2(this->size, _arrayLen(this->array))] = x;
    this->size += 1;
}
int64_t org_bau_List_List_int_get_2(org_bau_List_List_int* this, int64_t x) {
    int64_t _r1 = this->array->data[idx_2(x, _arrayLen(this->array))];
    return _r1;
}
int64_t org_bau_List_List_int_len_1(org_bau_List_List_int* this) {
    int64_t _r0 = this->size;
    return _r0;
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    _main();
    return 0;
}
void _main() {
    org_bau_List_List_Point* list = org_bau_List_newList_Point_1(0);
    Point* _t0 = newPoint_2(1, 10);
    org_bau_List_List_Point_add_2(list, _t0);
    Point* _t1 = newPoint_2(3, 300);
    org_bau_List_List_Point_add_2(list, _t1);
    org_bau_FilterMap_FilterMap_int* _t4 = NULL;
    if (1) {
        org_bau_FilterMap_FilterMap_Point* _t3 = NULL;
        if (1) {
            org_bau_FilterMap_FilterMap_Point* _t2 = org_bau_FilterMap_from_Point_1(0);
            org_bau_FilterMap_FilterMap_Point* this = org_bau_FilterMap_FilterMap_Point_in_2(_t2, list);
            org_bau_List_List_Point* _u_result = org_bau_List_List_Point_0();
            Point_array* _0t1 = Point_array_new(this->list->size);
            _incUseStack(_0t1);
            _decUse(_u_result->array, Point_array);
            _u_result->array = _0t1;
            int64_t _u_i = 0;
            while (_u_i < this->list->size) {
                _incUseStack(this->list->array->data[idx_2(_u_i, _arrayLen(this->list->array))]);
                Point* it = this->list->array->data[idx_2(_u_i, _arrayLen(this->list->array))];
                if (it->y > 100) {
                    org_bau_List_List_Point_add_2(_u_result, it);
                }
                _u_i = _u_i + 1;
            }
            _incUseStack(_u_result);
            _decUse(this->list, org_bau_List_List_Point);
            this->list = _u_result;
            _incUseStack(this);
            _decUseStack(_t3, org_bau_FilterMap_FilterMap_Point);
            _t3 = this;
        } else {
        }
        _incUseStack(_t3);
        org_bau_FilterMap_FilterMap_Point* this = _t3;
        org_bau_List_List_int* _u_result = org_bau_List_List_int_0();
        int_array* _0t0 = int_array_new(this->list->size);
        _incUseStack(_0t0);
        _decUse(_u_result->array, int_array);
        _u_result->array = _0t0;
        int64_t _u_i = 0;
        while (_u_i < this->list->size) {
            _incUseStack(this->list->array->data[idx_2(_u_i, _arrayLen(this->list->array))]);
            Point* it = this->list->array->data[idx_2(_u_i, _arrayLen(this->list->array))];
            int64_t _u_x = it->x * it->y;
            org_bau_List_List_int_add_2(_u_result, _u_x);
            _u_i = _u_i + 1;
        }
        org_bau_FilterMap_FilterMap_int* _0t1 = org_bau_FilterMap_from_int_1(0);
        org_bau_FilterMap_FilterMap_int* _0t2 = org_bau_FilterMap_FilterMap_int_in_2(_0t1, _u_result);
        _incUseStack(_0t2);
        _decUseStack(_t4, org_bau_FilterMap_FilterMap_int);
        _t4 = _0t2;
    } else {
    }
    org_bau_List_List_int* l2 = org_bau_FilterMap_FilterMap_int_select_1(_t4);
    if (org_bau_List_List_int_len_1(l2) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                int64_t _t5 = org_bau_List_List_int_get_2(l2, i);
                printf("%lld\n", (long long)_t5);
                if (( i + 1 ) >= org_bau_List_List_int_len_1(l2)) {
                    break;
                }
                i = i + 1;
            }
            break;
        }
    }
    _decUseStack(l2, org_bau_List_List_int);
    _decUseStack(_t4, org_bau_FilterMap_FilterMap_int);
    _decUseStack(_t1, Point);
    _decUseStack(_t0, Point);
    _decUseStack(list, org_bau_List_List_Point);
    _end();
}
/*

type List(T)
List

fun List(T) add(x T)
Append an entry to the list

fun List(T) add(index int, x T)
Insert an entry to the list at the given index

fun List(T) remove(pos int)
Insert an entry to the list at the given index

fun List(T) get(x int) T?
Insert an entry to the list at the given index

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

*/
