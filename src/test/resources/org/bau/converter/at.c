#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <limits.h>
#include <math.h>
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
typedef struct i8_array i8_array;
struct i8_array;
typedef struct int_array int_array;
struct int_array;
typedef struct float_array float_array;
struct float_array;
typedef struct org_bau_String_string org_bau_String_string;
struct org_bau_String_string;
typedef struct org_bau_String_string_array org_bau_String_string_array;
struct org_bau_String_string_array;
typedef struct org_bau_String_StringBuilder org_bau_String_StringBuilder;
struct org_bau_String_StringBuilder;
typedef struct Value Value;
struct Value;
typedef struct Value_array Value_array;
struct Value_array;
typedef struct org_bau_List_List_float org_bau_List_List_float;
struct org_bau_List_List_float;
typedef struct Expr Expr;
struct Expr;
typedef struct Expr_array Expr_array;
struct Expr_array;
typedef struct org_bau_List_List_Expr org_bau_List_List_Expr;
struct org_bau_List_List_Expr;
typedef struct At At;
struct At;
typedef struct org_bau_HashMap_HashMap_org_bau_String_string_Value org_bau_HashMap_HashMap_org_bau_String_string_Value;
struct org_bau_HashMap_HashMap_org_bau_String_string_Value;
typedef struct org_bau_HashMap_HashMap_org_bau_String_string_Value_array org_bau_HashMap_HashMap_org_bau_String_string_Value_array;
struct org_bau_HashMap_HashMap_org_bau_String_string_Value_array;
typedef struct org_bau_HashMap_HashMap_org_bau_String_string_Expr org_bau_HashMap_HashMap_org_bau_String_string_Expr;
struct org_bau_HashMap_HashMap_org_bau_String_string_Expr;
typedef struct org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value;
struct org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value;
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
struct float_array {
    int32_t len;
    int32_t _refCount;
    double* data;
};
float_array* float_array_new(uint64_t len) {
    if (len < 0 || len >= (1L << 31)) arrayOutOfBounds(len, 1L << 31);
    float_array* result = _malloc(sizeof(float_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(double) * len);
    memset(result->data, 0, sizeof(double) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_String_string {
    i8_array* data;
};
org_bau_String_string org_bau_String_string_new() {
    org_bau_String_string result;
    result.data = 0;
    return result;
}
struct org_bau_String_string_array {
    int32_t len;
    int32_t _refCount;
    org_bau_String_string* data;
};
org_bau_String_string_array* org_bau_String_string_array_new(uint64_t len) {
    if (len < 0 || len >= (1L << 31)) arrayOutOfBounds(len, 1L << 31);
    org_bau_String_string_array* result = _malloc(sizeof(org_bau_String_string_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(org_bau_String_string) * len);
    memset(result->data, 0, sizeof(org_bau_String_string) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_String_StringBuilder {
    int32_t _refCount;
    i8_array* data;
    int64_t len;
};
org_bau_String_StringBuilder* org_bau_String_StringBuilder_new() {
    org_bau_String_StringBuilder* result = _malloc(sizeof(org_bau_String_StringBuilder));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct Value {
    int32_t _refCount;
    org_bau_List_List_float* list;
};
Value* Value_new() {
    Value* result = _malloc(sizeof(Value));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct Value_array {
    int32_t len;
    int32_t _refCount;
    Value** data;
};
Value_array* Value_array_new(uint64_t len) {
    if (len < 0 || len >= (1L << 31)) arrayOutOfBounds(len, 1L << 31);
    Value_array* result = _malloc(sizeof(Value_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(Value*) * len);
    memset(result->data, 0, sizeof(Value*) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_List_List_float {
    int32_t _refCount;
    float_array* array;
    int64_t size;
};
org_bau_List_List_float* org_bau_List_List_float_new() {
    org_bau_List_List_float* result = _malloc(sizeof(org_bau_List_List_float));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct Expr {
    int32_t _refCount;
    int64_t exprType;
    org_bau_String_string name;
    Value* value;
    org_bau_List_List_Expr* list;
};
Expr* Expr_new() {
    Expr* result = _malloc(sizeof(Expr));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct Expr_array {
    int32_t len;
    int32_t _refCount;
    Expr** data;
};
Expr_array* Expr_array_new(uint64_t len) {
    if (len < 0 || len >= (1L << 31)) arrayOutOfBounds(len, 1L << 31);
    Expr_array* result = _malloc(sizeof(Expr_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(Expr*) * len);
    memset(result->data, 0, sizeof(Expr*) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_List_List_Expr {
    int32_t _refCount;
    Expr_array* array;
    int64_t size;
};
org_bau_List_List_Expr* org_bau_List_List_Expr_new() {
    org_bau_List_List_Expr* result = _malloc(sizeof(org_bau_List_List_Expr));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct At {
    int32_t _refCount;
    org_bau_HashMap_HashMap_org_bau_String_string_Value* global;
    org_bau_HashMap_HashMap_org_bau_String_string_Value* local;
    org_bau_HashMap_HashMap_org_bau_String_string_Expr* functions;
    org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* stack;
    org_bau_String_StringBuilder* out;
    org_bau_String_string code;
    org_bau_String_string token;
    Value* value;
    int64_t pos;
    Value* returnValue;
    int64_t hasReturnValue;
    int64_t counter;
    int64_t inOperator;
};
At* At_new() {
    At* result = _malloc(sizeof(At));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_HashMap_HashMap_org_bau_String_string_Value {
    int32_t _refCount;
    int64_t size;
    org_bau_String_string_array* keys;
    Value_array* values;
    int_array* hashes;
};
org_bau_HashMap_HashMap_org_bau_String_string_Value* org_bau_HashMap_HashMap_org_bau_String_string_Value_new() {
    org_bau_HashMap_HashMap_org_bau_String_string_Value* result = _malloc(sizeof(org_bau_HashMap_HashMap_org_bau_String_string_Value));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_HashMap_HashMap_org_bau_String_string_Value_array {
    int32_t len;
    int32_t _refCount;
    org_bau_HashMap_HashMap_org_bau_String_string_Value** data;
};
org_bau_HashMap_HashMap_org_bau_String_string_Value_array* org_bau_HashMap_HashMap_org_bau_String_string_Value_array_new(uint64_t len) {
    if (len < 0 || len >= (1L << 31)) arrayOutOfBounds(len, 1L << 31);
    org_bau_HashMap_HashMap_org_bau_String_string_Value_array* result = _malloc(sizeof(org_bau_HashMap_HashMap_org_bau_String_string_Value_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(org_bau_HashMap_HashMap_org_bau_String_string_Value*) * len);
    memset(result->data, 0, sizeof(org_bau_HashMap_HashMap_org_bau_String_string_Value*) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_HashMap_HashMap_org_bau_String_string_Expr {
    int32_t _refCount;
    int64_t size;
    org_bau_String_string_array* keys;
    Expr_array* values;
    int_array* hashes;
};
org_bau_HashMap_HashMap_org_bau_String_string_Expr* org_bau_HashMap_HashMap_org_bau_String_string_Expr_new() {
    org_bau_HashMap_HashMap_org_bau_String_string_Expr* result = _malloc(sizeof(org_bau_HashMap_HashMap_org_bau_String_string_Expr));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value {
    int32_t _refCount;
    org_bau_HashMap_HashMap_org_bau_String_string_Value_array* array;
    int64_t size;
};
org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_new() {
    org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* result = _malloc(sizeof(org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
At* At_9(org_bau_HashMap_HashMap_org_bau_String_string_Value* global, org_bau_HashMap_HashMap_org_bau_String_string_Value* local, org_bau_HashMap_HashMap_org_bau_String_string_Expr* functions, org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* stack, org_bau_String_StringBuilder* out, org_bau_String_string code, org_bau_String_string token, Value* value, Value* returnValue);
Value* At_call_3(At* this, org_bau_String_string name, Expr_array* argList);
Value* At_get_3(At* this, org_bau_HashMap_HashMap_org_bau_String_string_Value* map, org_bau_String_string name);
org_bau_List_List_Expr* At_getFunction_2(At* this, org_bau_String_string name);
int64_t At_hasFunction_2(At* this, org_bau_String_string name);
int64_t At_match_3(At* this, i8_array* a, i8_array* b);
double At_operator_5(At* this, org_bau_String_string op, int64_t precedence, double l, double r);
Expr* At_parseExpr_2(At* this, int64_t min);
Expr* At_parsePrimary_1(At* this);
void At_put_4(At* this, org_bau_HashMap_HashMap_org_bau_String_string_Value* map, org_bau_String_string name, Value* value);
void At_read_1(At* this);
org_bau_String_string At_run_2(At* this, i8_array* programCode);
Value* At_runExpr_2(At* this, Expr* expr);
void At_setFunction_3(At* this, org_bau_String_string name, Expr* expr);
Expr* Expr_3(org_bau_String_string name, Value* value, org_bau_List_List_Expr* list);
Value* Value_1(org_bau_List_List_float* list);
void Value_add_2(Value* this, double x);
double Value_get_2(Value* this, int64_t i);
int64_t Value_len_1(Value* this);
void Value_set_3(Value* this, int64_t i, double x);
org_bau_String_string convertToString_1(Value* array);
double float_1(double x);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
int64_t imod_2(int64_t a, int64_t b);
int64_t int_1(int64_t x);
At* newAt_0();
Expr* newExpr_3(int64_t exprType, i8_array* name, Value* value);
int64_t org_bau_Arrays_equals_i8_array_i8_2(i8_array* a, i8_array* b);
void org_bau_Arrays_reverse_i8_array_i8_3(i8_array* buff, int64_t first, int64_t last);
org_bau_HashMap_HashMap_org_bau_String_string_Expr* org_bau_HashMap_HashMap_org_bau_String_string_Expr_3(org_bau_String_string_array* keys, Expr_array* values, int_array* hashes);
org_bau_HashMap_HashMap_org_bau_String_string_Value* org_bau_HashMap_HashMap_org_bau_String_string_Value_3(org_bau_String_string_array* keys, Value_array* values, int_array* hashes);
int64_t org_bau_HashMap_hashCode_1(i8_array* data);
org_bau_HashMap_HashMap_org_bau_String_string_Expr* org_bau_HashMap_newHashMap_org_bau_String_string_Expr_2(int64_t _K, int64_t _V);
org_bau_HashMap_HashMap_org_bau_String_string_Value* org_bau_HashMap_newHashMap_org_bau_String_string_Value_2(int64_t _K, int64_t _V);
Expr* org_bau_HashMap_HashMap_org_bau_String_string_Expr_get_3(org_bau_HashMap_HashMap_org_bau_String_string_Expr* this, int64_t hash, org_bau_String_string key);
void org_bau_HashMap_HashMap_org_bau_String_string_Expr_put_4(org_bau_HashMap_HashMap_org_bau_String_string_Expr* this, int64_t hash, org_bau_String_string key, Expr* value);
Value* org_bau_HashMap_HashMap_org_bau_String_string_Value_get_3(org_bau_HashMap_HashMap_org_bau_String_string_Value* this, int64_t hash, org_bau_String_string key);
void org_bau_HashMap_HashMap_org_bau_String_string_Value_put_4(org_bau_HashMap_HashMap_org_bau_String_string_Value* this, int64_t hash, org_bau_String_string key, Value* value);
int64_t org_bau_Int_appendInt_3(int64_t n, i8_array* buff, int64_t pos);
i8_array* org_bau_Int_intToString_1(int64_t n);
org_bau_List_List_Expr* org_bau_List_List_Expr_1(Expr_array* array);
org_bau_List_List_float* org_bau_List_List_float_1(float_array* array);
org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_1(org_bau_HashMap_HashMap_org_bau_String_string_Value_array* array);
org_bau_List_List_Expr* org_bau_List_newList_Expr_1(int64_t _T);
org_bau_List_List_float* org_bau_List_newList_float_1(int64_t _T);
org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* org_bau_List_newList_org_bau_HashMap_HashMap_org_bau_String_string_Value_1(int64_t _T);
void org_bau_List_List_Expr_add_2(org_bau_List_List_Expr* this, Expr* x);
Expr* org_bau_List_List_Expr_get_2(org_bau_List_List_Expr* this, int64_t x);
int64_t org_bau_List_List_Expr_len_1(org_bau_List_List_Expr* this);
void org_bau_List_List_float_add_2(org_bau_List_List_float* this, double x);
double org_bau_List_List_float_get_2(org_bau_List_List_float* this, int64_t x);
int64_t org_bau_List_List_float_len_1(org_bau_List_List_float* this);
void org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_add_2(org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* this, org_bau_HashMap_HashMap_org_bau_String_string_Value* x);
void org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_remove_2(org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* this, int64_t pos);
int64_t org_bau_Math_appendFloat_3(double n, i8_array* buff, int64_t pos);
i8_array* org_bau_Math_floatToString_1(double n);
int64_t org_bau_Math_isNotANumber_1(double x);
org_bau_String_StringBuilder* org_bau_String_StringBuilder_1(i8_array* data);
org_bau_String_string org_bau_String_str_1(i8_array* s);
org_bau_String_string org_bau_String_string_1(i8_array* data);
i8_array* org_bau_String_substring_3(i8_array* s, int64_t start, int64_t end);
void org_bau_String_StringBuilder_append_2(org_bau_String_StringBuilder* this, i8_array* b);
void org_bau_String_StringBuilder_append_4(org_bau_String_StringBuilder* this, i8_array* b, int64_t start, int64_t end);
org_bau_String_string org_bau_String_StringBuilder_toString_1(org_bau_String_StringBuilder* this);
int64_t org_bau_String_string_charAt_2(org_bau_String_string this, int64_t i);
int64_t org_bau_String_string_equals_2(org_bau_String_string this, org_bau_String_string other);
int64_t org_bau_String_string_len_1(org_bau_String_string this);
org_bau_String_string org_bau_String_string_substring_3(org_bau_String_string this, int64_t start, int64_t end);
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
Value* valueOf_1(double v);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void float_array_free(float_array* x);
void org_bau_String_string_free(org_bau_String_string* x);
void org_bau_String_string_copy(org_bau_String_string* x);
void org_bau_String_string_array_free(org_bau_String_string_array* x);
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x);
void Value_free(Value* x);
void Value_array_free(Value_array* x);
void org_bau_List_List_float_free(org_bau_List_List_float* x);
void Expr_free(Expr* x);
void Expr_array_free(Expr_array* x);
void org_bau_List_List_Expr_free(org_bau_List_List_Expr* x);
void At_free(At* x);
void org_bau_HashMap_HashMap_org_bau_String_string_Value_free(org_bau_HashMap_HashMap_org_bau_String_string_Value* x);
void org_bau_HashMap_HashMap_org_bau_String_string_Value_array_free(org_bau_HashMap_HashMap_org_bau_String_string_Value_array* x);
void org_bau_HashMap_HashMap_org_bau_String_string_Expr_free(org_bau_HashMap_HashMap_org_bau_String_string_Expr* x);
void org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_free(org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* x);
void i8_array_free_0(i8_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void i8_array_free(i8_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))i8_array_free_0);
}
void int_array_free_0(int_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void int_array_free(int_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))int_array_free_0);
}
void float_array_free_0(float_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void float_array_free(float_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))float_array_free_0);
}
void org_bau_String_string_free_0(org_bau_String_string* x) {
    _decUse(x->data, i8_array);
}
void org_bau_String_string_free(org_bau_String_string* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_String_string_free_0);
}
void org_bau_String_string_copy(org_bau_String_string* x) {
    _incUse(x->data);
}
void org_bau_String_string_array_free_0(org_bau_String_string_array* x) {
    for (int i = 0; i < _arrayLen(x); i++) org_bau_String_string_free(&(x->data[i]));
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void org_bau_String_string_array_free(org_bau_String_string_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_String_string_array_free_0);
}
void org_bau_String_StringBuilder_free_0(org_bau_String_StringBuilder* x) {
    _decUse(x->data, i8_array);
    _free(x); _traceFree(x);
}
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_String_StringBuilder_free_0);
}
void Value_free_0(Value* x) {
    _decUse(x->list, org_bau_List_List_float);
    _free(x); _traceFree(x);
}
void Value_free(Value* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))Value_free_0);
}
void Value_array_free_0(Value_array* x) {
    for (int i = 0; i < _arrayLen(x); i++) _decUse(x->data[i], Value);
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void Value_array_free(Value_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))Value_array_free_0);
}
void org_bau_List_List_float_free_0(org_bau_List_List_float* x) {
    _decUse(x->array, float_array);
    _free(x); _traceFree(x);
}
void org_bau_List_List_float_free(org_bau_List_List_float* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_List_List_float_free_0);
}
void Expr_free_0(Expr* x) {
    org_bau_String_string_free(&x->name);
    _decUse(x->value, Value);
    _decUse(x->list, org_bau_List_List_Expr);
    _free(x); _traceFree(x);
}
void Expr_free(Expr* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))Expr_free_0);
}
void Expr_array_free_0(Expr_array* x) {
    for (int i = 0; i < _arrayLen(x); i++) _decUse(x->data[i], Expr);
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void Expr_array_free(Expr_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))Expr_array_free_0);
}
void org_bau_List_List_Expr_free_0(org_bau_List_List_Expr* x) {
    _decUse(x->array, Expr_array);
    _free(x); _traceFree(x);
}
void org_bau_List_List_Expr_free(org_bau_List_List_Expr* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_List_List_Expr_free_0);
}
void At_free_0(At* x) {
    _decUse(x->global, org_bau_HashMap_HashMap_org_bau_String_string_Value);
    _decUse(x->local, org_bau_HashMap_HashMap_org_bau_String_string_Value);
    _decUse(x->functions, org_bau_HashMap_HashMap_org_bau_String_string_Expr);
    _decUse(x->stack, org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value);
    _decUse(x->out, org_bau_String_StringBuilder);
    org_bau_String_string_free(&x->code);
    org_bau_String_string_free(&x->token);
    _decUse(x->value, Value);
    _decUse(x->returnValue, Value);
    _free(x); _traceFree(x);
}
void At_free(At* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))At_free_0);
}
void org_bau_HashMap_HashMap_org_bau_String_string_Value_free_0(org_bau_HashMap_HashMap_org_bau_String_string_Value* x) {
    _decUse(x->keys, org_bau_String_string_array);
    _decUse(x->values, Value_array);
    _decUse(x->hashes, int_array);
    _free(x); _traceFree(x);
}
void org_bau_HashMap_HashMap_org_bau_String_string_Value_free(org_bau_HashMap_HashMap_org_bau_String_string_Value* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_HashMap_HashMap_org_bau_String_string_Value_free_0);
}
void org_bau_HashMap_HashMap_org_bau_String_string_Value_array_free_0(org_bau_HashMap_HashMap_org_bau_String_string_Value_array* x) {
    for (int i = 0; i < _arrayLen(x); i++) _decUse(x->data[i], org_bau_HashMap_HashMap_org_bau_String_string_Value);
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void org_bau_HashMap_HashMap_org_bau_String_string_Value_array_free(org_bau_HashMap_HashMap_org_bau_String_string_Value_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_HashMap_HashMap_org_bau_String_string_Value_array_free_0);
}
void org_bau_HashMap_HashMap_org_bau_String_string_Expr_free_0(org_bau_HashMap_HashMap_org_bau_String_string_Expr* x) {
    _decUse(x->keys, org_bau_String_string_array);
    _decUse(x->values, Expr_array);
    _decUse(x->hashes, int_array);
    _free(x); _traceFree(x);
}
void org_bau_HashMap_HashMap_org_bau_String_string_Expr_free(org_bau_HashMap_HashMap_org_bau_String_string_Expr* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_HashMap_HashMap_org_bau_String_string_Expr_free_0);
}
void org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_free_0(org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* x) {
    _decUse(x->array, org_bau_HashMap_HashMap_org_bau_String_string_Value_array);
    _free(x); _traceFree(x);
}
void org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_free(org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_free_0);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = _malloc(sizeof(char) * len);
    memcpy(result->data, data, sizeof(char) * len);
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1011;
i8_array* string_1012;
i8_array* string_1013;
i8_array* string_1014;
i8_array* string_1015;
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
i8_array* string_1040;
i8_array* string_1041;
i8_array* string_1042;
i8_array* string_1043;
i8_array* string_1045;
i8_array* string_1048;
i8_array* string_1049;
i8_array* string_1050;
int64_t randomSeed;
int64_t MIN_INT;
int64_t MAX_INT;
int64_t MIN_I32;
int64_t MAX_I32;
double POS_INFINITY;
double NEG_INFINITY;
double NOT_A_NUMBER;
double PI;
double E;
double LOG10;
double LOG2;
int64_t BLOCK;
int64_t CALL;
int64_t IF;
int64_t LITERAL;
int64_t REPEAT;
int64_t LOOP;
int64_t OPERATION;
int64_t VARIABLE;
int64_t RETURN;
int64_t LIST;
At* At_9(org_bau_HashMap_HashMap_org_bau_String_string_Value* global, org_bau_HashMap_HashMap_org_bau_String_string_Value* local, org_bau_HashMap_HashMap_org_bau_String_string_Expr* functions, org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* stack, org_bau_String_StringBuilder* out, org_bau_String_string code, org_bau_String_string token, Value* value, Value* returnValue) {
    At* _t45 = At_new();
    _incUseStack(global);
    _t45->global = global;
    _incUseStack(local);
    _t45->local = local;
    _incUseStack(functions);
    _t45->functions = functions;
    _incUseStack(stack);
    _t45->stack = stack;
    _incUseStack(out);
    _t45->out = out;
    org_bau_String_string_copy(&code);
    _t45->code = code;
    org_bau_String_string_copy(&token);
    _t45->token = token;
    _incUseStack(value);
    _t45->value = value;
    _t45->pos = 0;
    _incUseStack(returnValue);
    _t45->returnValue = returnValue;
    _t45->hasReturnValue = 0;
    _t45->counter = 0;
    _t45->inOperator = 0;
    return _t45;
}
Value* At_call_3(At* this, org_bau_String_string name, Expr_array* argList) {
    org_bau_List_List_Expr* m = At_getFunction_2(this, name);
    if (m == NULL) {
        Value* _t0 = valueOf_1(0);
        _decUseStack(m, org_bau_List_List_Expr);
        return _t0;
    }
    org_bau_HashMap_HashMap_org_bau_String_string_Value* newLocal = org_bau_HashMap_newHashMap_org_bau_String_string_Value_2(0, 0);
    if (( m->size - 1 ) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                if (i < _arrayLen(argList)) {
                    Expr* _t1 = org_bau_List_List_Expr_get_2(m, i);
                    Value* _t2 = At_runExpr_2(this, argList->data[i]);
                    At_put_4(this, newLocal, _t1->name, _t2);
                    _decUseStack(_t2, Value);
                    _decUseStack(_t1, Expr);
                }
                int64_t _next = i + 1;
                if (_next >= ( m->size - 1 )) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_add_2(this->stack, this->local);
    _incUseStack(newLocal);
    _decUse(this->local, org_bau_HashMap_HashMap_org_bau_String_string_Value);
    this->local = newLocal;
    Value* result = At_runExpr_2(this, m->array->data[idx_2(m->size - 1, _arrayLen(m->array))]);
    if (this->hasReturnValue) {
        _incUseStack(this->returnValue);
        _decUseStack(result, Value);
        result = this->returnValue;
        this->hasReturnValue = 0;
    }
    if (this->stack->size > 0) {
        _incUseStack(this->stack->array->data[idx_2(this->stack->size - 1, _arrayLen(this->stack->array))]);
        org_bau_HashMap_HashMap_org_bau_String_string_Value* l = this->stack->array->data[idx_2(this->stack->size - 1, _arrayLen(this->stack->array))];
        if (l != NULL) {
            _incUseStack(l);
            _decUse(this->local, org_bau_HashMap_HashMap_org_bau_String_string_Value);
            this->local = l;
        }
        org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_remove_2(this->stack, this->stack->size - 1);
        _decUseStack(l, org_bau_HashMap_HashMap_org_bau_String_string_Value);
    }
    _decUseStack(newLocal, org_bau_HashMap_HashMap_org_bau_String_string_Value);
    _decUseStack(m, org_bau_List_List_Expr);
    return result;
}
Value* At_get_3(At* this, org_bau_HashMap_HashMap_org_bau_String_string_Value* map, org_bau_String_string name) {
    int64_t _t0 = org_bau_HashMap_hashCode_1(name.data);
    Value* _t1 = org_bau_HashMap_HashMap_org_bau_String_string_Value_get_3(map, _t0, name);
    return _t1;
}
org_bau_List_List_Expr* At_getFunction_2(At* this, org_bau_String_string name) {
    int64_t _t0 = org_bau_HashMap_hashCode_1(name.data);
    Expr* _t1 = org_bau_HashMap_HashMap_org_bau_String_string_Expr_get_3(this->functions, _t0, name);
    _incUseStack(_t1->list);
    org_bau_List_List_Expr* _r0 = _t1->list;
    _decUseStack(_t1, Expr);
    return _r0;
}
int64_t At_hasFunction_2(At* this, org_bau_String_string name) {
    int64_t _t0 = org_bau_HashMap_hashCode_1(name.data);
    Expr* _t1 = org_bau_HashMap_HashMap_org_bau_String_string_Expr_get_3(this->functions, _t0, name);
    int64_t _r0 = _t1 != NULL;
    _decUseStack(_t1, Expr);
    return _r0;
}
int64_t At_match_3(At* this, i8_array* a, i8_array* b) {
    int64_t _t0 = org_bau_Arrays_equals_i8_array_i8_2(this->token.data, a);
    int64_t _t1 = _t0;
    if (!(_t1)) {
        int64_t _t2 = org_bau_Arrays_equals_i8_array_i8_2(this->token.data, b);
        _t1 = _t2;
    }
    if (_t1) {
        At_read_1(this);
        return 1;
    }
    return 0;
}
double At_operator_5(At* this, org_bau_String_string op, int64_t precedence, double l, double r) {
    int64_t _t0 = At_hasFunction_2(this, op);
    int64_t _t1 = _t0;
    if (_t1) {
        int64_t _t2 = !(this->inOperator);
        _t1 = _t2;
    }
    if (_t1) {
        if (precedence) {
            return 40;
        }
        this->inOperator = 1;
        Expr_array* _t3 = Expr_array_new(2);
        _incUseStack(_t3);
        Expr_array* list = _t3;
        Value* _t4 = valueOf_1(l);
        Expr* _t5 = newExpr_3(4, string_1000, _t4);
        _incUseStack(_t5);
        _decUse(list->data[0], Expr);
        list->data[0] = _t5;
        Value* _t6 = valueOf_1(r);
        Expr* _t7 = newExpr_3(4, string_1000, _t6);
        _incUseStack(_t7);
        _decUse(list->data[1], Expr);
        list->data[1] = _t7;
        Value* result = At_call_3(this, op, list);
        this->inOperator = 0;
        double _t8 = Value_get_2(result, 0);
        _decUseStack(result, Value);
        _decUseStack(_t7, Expr);
        _decUseStack(_t6, Value);
        _decUseStack(_t5, Expr);
        _decUseStack(_t4, Value);
        _decUseStack(list, Expr_array);
        _decUseStack(_t3, Expr_array);
        return _t8;
    }
    double res = 0.0;
    int64_t power = -1;
    org_bau_String_string _t9 = org_bau_String_str_1(string_1014);
    int64_t _t10 = org_bau_String_string_equals_2(op, _t9);
    if (_t10) {
        int64_t _t11 = int_1(l);
        int64_t _t12 = int_1(r);
        double _t13 = float_1(_t11 & _t12);
        res = _t13;
        power = 20;
    } else {
        org_bau_String_string _t14 = org_bau_String_str_1(string_1015);
        int64_t _t15 = org_bau_String_string_equals_2(op, _t14);
        if (_t15) {
            int64_t _t16 = int_1(l);
            int64_t _t17 = int_1(r);
            double _t18 = float_1(_t16 | _t17);
            res = _t18;
            power = 20;
            org_bau_String_string_free(&_t14);
        } else {
            org_bau_String_string _t19 = org_bau_String_str_1(string_1016);
            int64_t _t20 = org_bau_String_string_equals_2(op, _t19);
            if (_t20) {
                if (l == r) {
                    res = 1;
                }
                power = 30;
                org_bau_String_string_free(&_t19);
            } else {
                org_bau_String_string _t21 = org_bau_String_str_1(string_1017);
                int64_t _t22 = org_bau_String_string_equals_2(op, _t21);
                if (_t22) {
                    if (l != r) {
                        res = 1;
                    }
                    power = 30;
                    org_bau_String_string_free(&_t21);
                } else {
                    org_bau_String_string _t23 = org_bau_String_str_1(string_1018);
                    int64_t _t24 = org_bau_String_string_equals_2(op, _t23);
                    if (_t24) {
                        if (l > r) {
                            res = 1;
                        }
                        power = 30;
                        org_bau_String_string_free(&_t23);
                    } else {
                        org_bau_String_string _t25 = org_bau_String_str_1(string_1019);
                        int64_t _t26 = org_bau_String_string_equals_2(op, _t25);
                        if (_t26) {
                            if (l < r) {
                                res = 1;
                            }
                            power = 30;
                            org_bau_String_string_free(&_t25);
                        } else {
                            org_bau_String_string _t27 = org_bau_String_str_1(string_1020);
                            int64_t _t28 = org_bau_String_string_equals_2(op, _t27);
                            if (_t28) {
                                if (l >= r) {
                                    res = 1;
                                }
                                power = 30;
                                org_bau_String_string_free(&_t27);
                            } else {
                                org_bau_String_string _t29 = org_bau_String_str_1(string_1021);
                                int64_t _t30 = org_bau_String_string_equals_2(op, _t29);
                                if (_t30) {
                                    if (l <= r) {
                                        res = 1;
                                    }
                                    power = 30;
                                    org_bau_String_string_free(&_t29);
                                } else {
                                    org_bau_String_string _t31 = org_bau_String_str_1(string_1022);
                                    int64_t _t32 = org_bau_String_string_equals_2(op, _t31);
                                    if (_t32) {
                                        res = l + r;
                                        power = 40;
                                        org_bau_String_string_free(&_t31);
                                    } else {
                                        org_bau_String_string _t33 = org_bau_String_str_1(string_1001);
                                        int64_t _t34 = org_bau_String_string_equals_2(op, _t33);
                                        if (_t34) {
                                            res = l - r;
                                            power = 40;
                                            org_bau_String_string_free(&_t33);
                                        } else {
                                            org_bau_String_string _t35 = org_bau_String_str_1(string_1023);
                                            int64_t _t36 = org_bau_String_string_equals_2(op, _t35);
                                            if (_t36) {
                                                res = l * r;
                                                power = 50;
                                                org_bau_String_string_free(&_t35);
                                            } else {
                                                org_bau_String_string _t37 = org_bau_String_str_1(string_1024);
                                                int64_t _t38 = org_bau_String_string_equals_2(op, _t37);
                                                if (_t38) {
                                                    res = l / r;
                                                    power = 50;
                                                    org_bau_String_string_free(&_t37);
                                                } else {
                                                    org_bau_String_string _t39 = org_bau_String_str_1(string_1025);
                                                    int64_t _t40 = org_bau_String_string_equals_2(op, _t39);
                                                    if (_t40) {
                                                        res = imod_2(l, r);
                                                        power = 50;
                                                        org_bau_String_string_free(&_t39);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (precedence) {
        double _t41 = float_1(power);
        org_bau_String_string_free(&_t9);
        return _t41;
    }
    org_bau_String_string_free(&_t9);
    return res;
}
Expr* At_parseExpr_2(At* this, int64_t min) {
    Expr* prim = At_parsePrimary_1(this);
    while (1) {
        int64_t power = -1;
        if (_arrayLen(this->token.data) == 0) {
            return prim;
        }
        Value* _t0 = valueOf_1(0);
        Expr* op = newExpr_3(7, this->token.data, _t0);
        org_bau_List_List_Expr_add_2(op->list, prim);
        int64_t right = 1;
        int64_t _t1 = org_bau_String_string_charAt_2(this->token, 0);
        if (_t1 == 58) {
            int64_t _t2 = op->list->array->data[idx_2(0, _arrayLen(op->list->array))]->exprType != 8;
            if (_t2) {
                int64_t _t3 = org_bau_String_string_charAt_2(op->list->array->data[idx_2(0, _arrayLen(op->list->array))]->name, 0) != 46;
                _t2 = _t3;
            }
            if (_t2) {
                _decUseStack(op, Expr);
                _decUseStack(_t0, Value);
                return prim;
            }
            power = 10;
            right = -1;
        } else {
            int64_t _t4 = org_bau_String_string_charAt_2(this->token, 0);
            if (_t4 == 46) {
                power = 60;
            } else {
                int64_t _t5 = org_bau_String_string_charAt_2(this->token, 0);
                int64_t _t6 = _t5 < 97;
                if (!(_t6)) {
                    int64_t _t7 = org_bau_String_string_charAt_2(this->token, 0) > 122;
                    _t6 = _t7;
                }
                if (_t6) {
                    double _t8 = At_operator_5(this, this->token, 1, 0, 0);
                    int64_t _t9 = int_1(_t8);
                    power = _t9;
                }
            }
        }
        if (power < min) {
            _decUseStack(op, Expr);
            _decUseStack(_t0, Value);
            return prim;
        }
        At_read_1(this);
        Expr* _t10 = At_parseExpr_2(this, power + right);
        org_bau_List_List_Expr_add_2(op->list, _t10);
        _incUseStack(op);
        _decUseStack(prim, Expr);
        prim = op;
        _decUseStack(_t10, Expr);
        _decUseStack(op, Expr);
        _decUseStack(_t0, Value);
    }
    return prim;
}
Expr* At_parsePrimary_1(At* this) {
    _incUseStack(this->value);
    Value* v = this->value;
    if (_arrayLen(this->token.data) == 0) {
        Value* _t0 = valueOf_1(0);
        Expr* _t1 = newExpr_3(4, string_1000, _t0);
        _decUseStack(_t0, Value);
        _decUseStack(v, Value);
        return _t1;
    } else {
        int64_t _t2 = At_match_3(this, string_1029, string_1030);
        if (_t2) {
            Value* _t3 = valueOf_1(0);
            Expr* e = newExpr_3(6, string_1000, _t3);
            Expr* _t4 = At_parseExpr_2(this, 0);
            org_bau_List_List_Expr_add_2(e->list, _t4);
            Expr* _t5 = At_parseExpr_2(this, 0);
            org_bau_List_List_Expr_add_2(e->list, _t5);
            _decUseStack(_t5, Expr);
            _decUseStack(_t4, Expr);
            _decUseStack(_t3, Value);
            _decUseStack(v, Value);
            return e;
        } else {
            int64_t _t6 = At_match_3(this, string_1023, string_1031);
            if (_t6) {
                Value* _t7 = valueOf_1(0);
                Expr* e = newExpr_3(5, string_1000, _t7);
                Expr* _t8 = At_parseExpr_2(this, 0);
                org_bau_List_List_Expr_add_2(e->list, _t8);
                Expr* _t9 = At_parseExpr_2(this, 0);
                org_bau_List_List_Expr_add_2(e->list, _t9);
                _decUseStack(_t9, Expr);
                _decUseStack(_t8, Expr);
                _decUseStack(_t7, Value);
                _decUseStack(v, Value);
                return e;
            } else {
                int64_t _t10 = At_match_3(this, string_1032, string_1033);
                if (_t10) {
                    Value* _t11 = valueOf_1(0);
                    Expr* e = newExpr_3(3, string_1000, _t11);
                    Expr* _t12 = At_parseExpr_2(this, 0);
                    org_bau_List_List_Expr_add_2(e->list, _t12);
                    Expr* _t13 = At_parseExpr_2(this, 0);
                    org_bau_List_List_Expr_add_2(e->list, _t13);
                    int64_t _t14 = At_match_3(this, string_1026, string_1034);
                    if (_t14) {
                        Expr* _t15 = At_parseExpr_2(this, 0);
                        org_bau_List_List_Expr_add_2(e->list, _t15);
                        _decUseStack(_t15, Expr);
                    }
                    _decUseStack(_t13, Expr);
                    _decUseStack(_t12, Expr);
                    _decUseStack(_t11, Value);
                    _decUseStack(v, Value);
                    return e;
                } else {
                    int64_t _t16 = At_match_3(this, string_1016, string_1035);
                    if (_t16) {
                        Value* _t17 = valueOf_1(0);
                        Expr* e = newExpr_3(9, string_1000, _t17);
                        Expr* _t18 = At_parseExpr_2(this, 0);
                        org_bau_List_List_Expr_add_2(e->list, _t18);
                        _decUseStack(_t18, Expr);
                        _decUseStack(_t17, Value);
                        _decUseStack(v, Value);
                        return e;
                    } else {
                        int64_t _t19 = At_match_3(this, string_1001, string_1001);
                        if (_t19) {
                            Value* _t20 = valueOf_1(0);
                            Expr* e = newExpr_3(7, string_1001, _t20);
                            Value* _t21 = valueOf_1(0);
                            Expr* _t22 = newExpr_3(4, string_1000, _t21);
                            org_bau_List_List_Expr_add_2(e->list, _t22);
                            Expr* _t23 = At_parsePrimary_1(this);
                            org_bau_List_List_Expr_add_2(e->list, _t23);
                            _decUseStack(_t23, Expr);
                            _decUseStack(_t22, Expr);
                            _decUseStack(_t21, Value);
                            _decUseStack(_t20, Value);
                            _decUseStack(v, Value);
                            return e;
                        } else {
                            int64_t _t24 = At_match_3(this, string_1036, string_1036);
                            if (_t24) {
                                Value* _t25 = valueOf_1(0);
                                Expr* e = newExpr_3(10, string_1000, _t25);
                                while (1 == 1) {
                                    int64_t _t26 = At_match_3(this, string_1037, string_1037);
                                    int64_t _t27 = !(_t26);
                                    if (_t27) {
                                        int64_t _t28 = this->pos < _arrayLen(this->code.data);
                                        _t27 = _t28;
                                    }
                                    if (!(_t27)) {
                                        break;
                                    }
                                    Expr* _t29 = At_parseExpr_2(this, 0);
                                    org_bau_List_List_Expr_add_2(e->list, _t29);
                                    int64_t _t30 = At_match_3(this, string_1038, string_1038);
                                    ;
                                    _decUseStack(_t29, Expr);
                                }
                                _decUseStack(_t25, Value);
                                _decUseStack(v, Value);
                                return e;
                            } else {
                                int64_t _t31 = At_match_3(this, string_1039, string_1039);
                                if (_t31) {
                                    Value* _t32 = valueOf_1(0);
                                    Expr* e = newExpr_3(1, string_1000, _t32);
                                    while (1 == 1) {
                                        int64_t _t33 = At_match_3(this, string_1040, string_1040);
                                        int64_t _t34 = _t33 == 0;
                                        if (_t34) {
                                            int64_t _t35 = this->pos < _arrayLen(this->code.data);
                                            _t34 = _t35;
                                        }
                                        if (!(_t34)) {
                                            break;
                                        }
                                        Expr* _t36 = At_parseExpr_2(this, 0);
                                        org_bau_List_List_Expr_add_2(e->list, _t36);
                                        int64_t _t37 = At_match_3(this, string_1041, string_1041);
                                        ;
                                        _decUseStack(_t36, Expr);
                                    }
                                    _decUseStack(_t32, Value);
                                    _decUseStack(v, Value);
                                    return e;
                                } else {
                                    int64_t _t38 = At_match_3(this, string_1002, string_1042);
                                    if (_t38) {
                                        Expr* _t39 = newExpr_3(4, string_1000, v);
                                        _decUseStack(v, Value);
                                        return _t39;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    org_bau_String_string_copy(&this->token);
    org_bau_String_string id = this->token;
    int64_t _t40 = At_match_3(this, string_1018, string_1043);
    if (_t40) {
        org_bau_String_string _t41 = org_bau_String_str_1(string_1018);
        org_bau_String_string_copy(&_t41);
        org_bau_String_string_free(&id);
        id = _t41;
        org_bau_String_string_free(&_t41);
    } else {
        At_read_1(this);
    }
    int64_t _t42 = At_hasFunction_2(this, id);
    int64_t _t43 = _t42;
    if (!(_t43)) {
        int64_t _t44 = org_bau_String_string_charAt_2(id, 0) == 62;
        _t43 = _t44;
    }
    int64_t _t45 = (_t43);
    if (_t45) {
        int64_t _t46 = At_match_3(this, string_1036, string_1036);
        _t45 = _t46;
    }
    if (_t45) {
        Value* _t47 = valueOf_1(0);
        Expr* e = newExpr_3(2, id.data, _t47);
        while (1 == 1) {
            int64_t _t48 = At_match_3(this, string_1037, string_1037);
            int64_t _t49 = (!(_t48));
            if (_t49) {
                int64_t _t50 = this->pos < org_bau_String_string_len_1(this->code);
                _t49 = _t50;
            }
            if (!(_t49)) {
                break;
            }
            Expr* _t51 = At_parseExpr_2(this, 0);
            org_bau_List_List_Expr_add_2(e->list, _t51);
            int64_t _t52 = At_match_3(this, string_1038, string_1038);
            ;
            _decUseStack(_t51, Expr);
        }
        _decUseStack(_t47, Value);
        org_bau_String_string_free(&id);
        _decUseStack(v, Value);
        return e;
    }
    Value* _t53 = valueOf_1(0);
    Expr* _t54 = newExpr_3(8, id.data, _t53);
    _decUseStack(_t53, Value);
    org_bau_String_string_free(&id);
    _decUseStack(v, Value);
    return _t54;
}
void At_put_4(At* this, org_bau_HashMap_HashMap_org_bau_String_string_Value* map, org_bau_String_string name, Value* value) {
    int64_t _t0 = org_bau_HashMap_hashCode_1(name.data);
    org_bau_HashMap_HashMap_org_bau_String_string_Value_put_4(map, _t0, name, value);
}
void At_read_1(At* this) {
    org_bau_String_string _t0 = org_bau_String_str_1(string_1000);
    org_bau_String_string_copy(&_t0);
    this->token = _t0;
    while (this->pos < _arrayLen(this->code.data)) {
        int64_t c = org_bau_String_string_charAt_2(this->code, this->pos);
        int64_t _t1 = c == 32;
        if (!(_t1)) {
            int64_t _t2 = c == 10;
            _t1 = _t2;
        }
        if (_t1) {
            this->pos += 1;
        } else {
            break;
        }
    }
    if (this->pos >= _arrayLen(this->code.data)) {
        return;
    }
    int64_t start = this->pos;
    int64_t c = org_bau_String_string_charAt_2(this->code, this->pos);
    int64_t _t3 = c >= 97;
    if (_t3) {
        int64_t _t4 = c <= 122;
        _t3 = _t4;
    }
    int64_t _t5 = (_t3);
    if (!(_t5)) {
        int64_t _t6 = ((c >= 65) && (c <= 90));
        _t5 = _t6;
    }
    int64_t _t7 = _t5;
    if (!(_t7)) {
        int64_t _t8 = c == 95;
        _t7 = _t8;
    }
    if (_t7) {
        this->pos += 1;
        while (this->pos < _arrayLen(this->code.data)) {
            int64_t _t9 = org_bau_String_string_charAt_2(this->code, this->pos);
            c = _t9;
            int64_t _t10 = c >= 97;
            if (_t10) {
                int64_t _t11 = c <= 122;
                _t10 = _t11;
            }
            int64_t _t12 = (_t10);
            if (!(_t12)) {
                int64_t _t13 = ((c >= 65) && (c <= 90));
                _t12 = _t13;
            }
            int64_t _t14 = _t12;
            if (!(_t14)) {
                int64_t _t15 = c == 95;
                _t14 = _t15;
            }
            int64_t _t16 = _t14;
            if (!(_t16)) {
                int64_t _t17 = ((c >= 48) && (c <= 57));
                _t16 = _t17;
            }
            if (_t16) {
                this->pos += 1;
            } else {
                break;
            }
        }
        org_bau_String_string _t18 = org_bau_String_string_substring_3(this->code, start, this->pos);
        org_bau_String_string_copy(&_t18);
        this->token = _t18;
        org_bau_String_string_free(&_t18);
    } else {
        int64_t _t19 = c >= 48;
        if (_t19) {
            int64_t _t20 = c <= 57;
            _t19 = _t20;
        }
        if (_t19) {
            this->pos += 1;
            double v = float_1(c - 48);
            double div = 0.0;
            while (this->pos < _arrayLen(this->code.data)) {
                int64_t _t21 = org_bau_String_string_charAt_2(this->code, this->pos);
                c = _t21;
                int64_t _t22 = c >= 48;
                if (_t22) {
                    int64_t _t23 = c <= 57;
                    _t22 = _t23;
                }
                if (_t22) {
                    double _t24 = float_1(c - 48);
                    v = ( v * 10 ) + _t24;
                    div = div * 10;
                } else {
                    int64_t _t25 = c == 46;
                    if (_t25) {
                        int64_t _t26 = div == 0;
                        _t25 = _t26;
                    }
                    if (_t25) {
                        div = 1;
                    } else {
                        break;
                    }
                }
                this->pos += 1;
            }
            if (div > 0) {
                v = v / div;
            }
            Value* _t27 = valueOf_1(v);
            _incUseStack(_t27);
            _decUse(this->value, Value);
            this->value = _t27;
            org_bau_String_string _t28 = org_bau_String_str_1(string_1002);
            org_bau_String_string_copy(&_t28);
            this->token = _t28;
            org_bau_String_string_free(&_t28);
            _decUseStack(_t27, Value);
        } else {
            if (c == 39) {
                this->pos += 1;
                org_bau_List_List_float* _t29 = org_bau_List_newList_float_1(0);
                Value* _t30 = Value_1(_t29);
                _incUseStack(_t30);
                _decUse(this->value, Value);
                this->value = _t30;
                while (1) {
                    int64_t _t31 = org_bau_String_string_len_1(this->code);
                    int64_t _t32 = this->pos >= _t31;
                    if (!(_t32)) {
                        int64_t _t33 = org_bau_String_string_charAt_2(this->code, this->pos) == 39;
                        _t32 = _t33;
                    }
                    if (_t32) {
                        this->pos += 1;
                        int64_t _t34 = org_bau_String_string_len_1(this->code);
                        int64_t _t35 = this->pos >= _t34;
                        if (!(_t35)) {
                            int64_t _t36 = org_bau_String_string_charAt_2(this->code, this->pos) != 39;
                            _t35 = _t36;
                        }
                        if (_t35) {
                            break;
                        }
                    }
                    int64_t _t37 = org_bau_String_string_charAt_2(this->code, this->pos);
                    double _t38 = float_1(_t37);
                    org_bau_List_List_float_add_2(this->value->list, _t38);
                    this->pos += 1;
                }
                org_bau_List_List_float_add_2(this->value->list, 0.0);
                org_bau_String_string _t39 = org_bau_String_str_1(string_1042);
                org_bau_String_string_copy(&_t39);
                this->token = _t39;
                org_bau_String_string_free(&_t39);
                _decUseStack(_t30, Value);
                _decUseStack(_t29, org_bau_List_List_float);
            } else {
                int64_t first = c;
                this->pos += 1;
                if (this->pos < _arrayLen(this->code.data)) {
                    int64_t _t40 = org_bau_String_string_charAt_2(this->code, this->pos);
                    c = _t40;
                    int64_t _t41 = first == 60;
                    if (_t41) {
                        int64_t _t42 = (((c == 60) || (c == 61)) || (c == 62));
                        _t41 = _t42;
                    }
                    int64_t _t43 = (_t41);
                    if (!(_t43)) {
                        int64_t _t44 = ((first == 42) && (c == 42));
                        _t43 = _t44;
                    }
                    int64_t _t45 = _t43;
                    if (!(_t45)) {
                        int64_t _t46 = ((first == 62) && (((c == 61) || (c == 62))));
                        _t45 = _t46;
                    }
                    if (_t45) {
                        this->pos += 1;
                    }
                }
                org_bau_String_string _t47 = org_bau_String_string_substring_3(this->code, start, this->pos);
                org_bau_String_string_copy(&_t47);
                this->token = _t47;
                org_bau_String_string_free(&_t47);
            }
        }
    }
    org_bau_String_string_free(&_t0);
}
org_bau_String_string At_run_2(At* this, i8_array* programCode) {
    org_bau_String_string _t0 = org_bau_String_str_1(programCode);
    org_bau_String_string_copy(&_t0);
    this->code = _t0;
    this->pos = 0;
    this->counter = 100000;
    At_read_1(this);
    org_bau_List_List_Expr* program = org_bau_List_newList_Expr_1(0);
    while (1 == 1) {
        int64_t _t1 = org_bau_String_string_len_1(this->token);
        if (!(( _t1 > 0 ))) {
            break;
        }
        int64_t _t2 = At_match_3(this, string_1022, string_1049);
        if (_t2) {
            org_bau_String_string_copy(&this->token);
            org_bau_String_string n = this->token;
            this->inOperator = 1;
            Value* _t3 = valueOf_1(0);
            Expr* _t4 = newExpr_3(1, string_1000, _t3);
            At_setFunction_3(this, n, _t4);
            Expr* c = At_parseExpr_2(this, 0);
            Expr* _t5 = At_parseExpr_2(this, 0);
            org_bau_List_List_Expr_add_2(c->list, _t5);
            this->inOperator = 0;
            At_setFunction_3(this, n, c);
            _decUseStack(_t5, Expr);
            _decUseStack(c, Expr);
            _decUseStack(_t4, Expr);
            _decUseStack(_t3, Value);
            org_bau_String_string_free(&n);
        } else {
            Expr* _t6 = At_parseExpr_2(this, 0);
            org_bau_List_List_Expr_add_2(program, _t6);
            _decUseStack(_t6, Expr);
        }
        int64_t _t7 = At_match_3(this, string_1038, string_1041);
        ;
    }
    i8_array* _t8 = i8_array_new(16);
    org_bau_String_StringBuilder* _t9 = org_bau_String_StringBuilder_1(_t8);
    _incUseStack(_t9);
    _decUse(this->out, org_bau_String_StringBuilder);
    this->out = _t9;
    Value* result = valueOf_1(0);
    if (program->size > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                Expr* e = org_bau_List_List_Expr_get_2(program, i);
                if (e != NULL) {
                    Value* _t10 = At_runExpr_2(this, e);
                    _incUseStack(_t10);
                    _decUseStack(result, Value);
                    result = _t10;
                    _decUseStack(_t10, Value);
                }
                int64_t _next = i + 1;
                if (_next >= program->size) {
                    _decUseStack(e, Expr);
                    break;
                }
                i = _next;
                _decUseStack(e, Expr);
            }
            break;
        }
    }
    int64_t _t11 = _arrayLen(this->out->data) == 0;
    if (_t11) {
        int64_t _t12 = result->list->size > 0;
        _t11 = _t12;
    }
    if (_t11) {
        org_bau_String_string _t13 = convertToString_1(result);
        org_bau_String_StringBuilder_append_2(this->out, _t13.data);
        org_bau_String_string_free(&_t13);
    }
    org_bau_String_string _t14 = org_bau_String_str_1(this->out->data);
    _decUseStack(result, Value);
    _decUseStack(_t9, org_bau_String_StringBuilder);
    _decUseStack(_t8, i8_array);
    _decUseStack(program, org_bau_List_List_Expr);
    org_bau_String_string_free(&_t0);
    return _t14;
}
Value* At_runExpr_2(At* this, Expr* expr) {
    Value* result = valueOf_1(0);
    if (expr == NULL) {
        return result;
    }
    if (expr->exprType == 4) {
        _incUseStack(expr->value);
        _decUseStack(result, Value);
        result = expr->value;
    } else {
        if (expr->exprType == 8) {
            _incUseStack(this->global);
            org_bau_HashMap_HashMap_org_bau_String_string_Value* map = this->global;
            int64_t _t0 = org_bau_String_string_charAt_2(expr->name, 0);
            if (_t0 > 90) {
                _incUseStack(this->local);
                _decUseStack(map, org_bau_HashMap_HashMap_org_bau_String_string_Value);
                map = this->local;
            }
            Value* r = At_get_3(this, map, expr->name);
            if (r != NULL) {
                _incUseStack(r);
                _decUseStack(result, Value);
                result = r;
            } else {
                Value* _t1 = valueOf_1(0);
                _incUseStack(_t1);
                _decUseStack(result, Value);
                result = _t1;
                _decUseStack(_t1, Value);
            }
            _decUseStack(r, Value);
            _decUseStack(map, org_bau_HashMap_HashMap_org_bau_String_string_Value);
        } else {
            if (expr->exprType == 3) {
                Expr* _t2 = org_bau_List_List_Expr_get_2(expr->list, 0);
                Value* cond = At_runExpr_2(this, _t2);
                double _t3 = Value_get_2(cond, 0);
                int64_t _t4 = int_1(_t3);
                if (_t4 != 0) {
                    Expr* _t5 = org_bau_List_List_Expr_get_2(expr->list, 1);
                    Value* _t6 = At_runExpr_2(this, _t5);
                    _incUseStack(_t6);
                    _decUseStack(result, Value);
                    result = _t6;
                    _decUseStack(_t6, Value);
                    _decUseStack(_t5, Expr);
                } else {
                    int64_t _t7 = org_bau_List_List_Expr_len_1(expr->list);
                    if (_t7 > 2) {
                        Expr* _t8 = org_bau_List_List_Expr_get_2(expr->list, 2);
                        Value* _t9 = At_runExpr_2(this, _t8);
                        _incUseStack(_t9);
                        _decUseStack(result, Value);
                        result = _t9;
                        _decUseStack(_t9, Value);
                        _decUseStack(_t8, Expr);
                    }
                }
                _decUseStack(cond, Value);
                _decUseStack(_t2, Expr);
            } else {
                if (expr->exprType == 5) {
                    Expr* _t10 = org_bau_List_List_Expr_get_2(expr->list, 0);
                    Value* _t11 = At_runExpr_2(this, _t10);
                    double _t12 = org_bau_List_List_float_get_2(_t11->list, 0);
                    int64_t c = int_1(_t12);
                    if (c > 0) {
                        while (1 == 1) {
                            int64_t i = 0;
                            while (1) {
                                int64_t _t13 = this->hasReturnValue;
                                if (!(_t13)) {
                                    int64_t _t14 = this->counter <= 0;
                                    _t13 = _t14;
                                }
                                if (_t13) {
                                    break;
                                }
                                this->counter -= 1;
                                org_bau_String_string _t15 = org_bau_String_str_1(string_1048);
                                double _t16 = float_1(i);
                                Value* _t17 = valueOf_1(_t16);
                                At_put_4(this, this->local, _t15, _t17);
                                Expr* _t18 = org_bau_List_List_Expr_get_2(expr->list, 1);
                                Value* _t19 = At_runExpr_2(this, _t18);
                                _incUseStack(_t19);
                                _decUseStack(result, Value);
                                result = _t19;
                                int64_t _next = i + 1;
                                if (_next >= c) {
                                    _decUseStack(_t19, Value);
                                    _decUseStack(_t18, Expr);
                                    _decUseStack(_t17, Value);
                                    org_bau_String_string_free(&_t15);
                                    break;
                                }
                                i = _next;
                                _decUseStack(_t19, Value);
                                _decUseStack(_t18, Expr);
                                _decUseStack(_t17, Value);
                                org_bau_String_string_free(&_t15);
                            }
                            break;
                        }
                    }
                    _decUseStack(_t11, Value);
                    _decUseStack(_t10, Expr);
                } else {
                    if (expr->exprType == 6) {
                        while (1) {
                            Expr* _t20 = org_bau_List_List_Expr_get_2(expr->list, 0);
                            Value* _t21 = At_runExpr_2(this, _t20);
                            double _t22 = Value_get_2(_t21, 0);
                            int64_t _t23 = int_1(_t22);
                            if (_t23 == 0) {
                                _decUseStack(_t21, Value);
                                _decUseStack(_t20, Expr);
                                break;
                            }
                            int64_t _t24 = this->hasReturnValue;
                            if (!(_t24)) {
                                int64_t _t25 = this->counter <= 0;
                                _t24 = _t25;
                            }
                            if (_t24) {
                                _decUseStack(_t21, Value);
                                _decUseStack(_t20, Expr);
                                break;
                            }
                            this->counter -= 1;
                            Expr* _t26 = org_bau_List_List_Expr_get_2(expr->list, 1);
                            Value* _t27 = At_runExpr_2(this, _t26);
                            _incUseStack(_t27);
                            _decUseStack(result, Value);
                            result = _t27;
                            _decUseStack(_t27, Value);
                            _decUseStack(_t26, Expr);
                            _decUseStack(_t21, Value);
                            _decUseStack(_t20, Expr);
                        }
                    } else {
                        if (expr->exprType == 9) {
                            Expr* _t28 = org_bau_List_List_Expr_get_2(expr->list, 0);
                            Value* _t29 = At_runExpr_2(this, _t28);
                            _incUseStack(_t29);
                            _decUseStack(result, Value);
                            result = _t29;
                            _incUseStack(result);
                            _decUse(this->returnValue, Value);
                            this->returnValue = result;
                            this->hasReturnValue = 1;
                            _decUseStack(_t29, Value);
                            _decUseStack(_t28, Expr);
                        } else {
                            if (expr->exprType == 2) {
                                int64_t _t30 = org_bau_String_string_charAt_2(expr->name, 0);
                                if (_t30 == 62) {
                                    if (org_bau_List_List_Expr_len_1(expr->list) > 0) {
                                        while (1 == 1) {
                                            int64_t i = 0;
                                            while (1) {
                                                Expr* _t31 = org_bau_List_List_Expr_get_2(expr->list, i);
                                                Value* _t32 = At_runExpr_2(this, _t31);
                                                org_bau_String_string _t33 = convertToString_1(_t32);
                                                org_bau_String_StringBuilder_append_2(this->out, _t33.data);
                                                int64_t _next = i + 1;
                                                if (_next >= org_bau_List_List_Expr_len_1(expr->list)) {
                                                    org_bau_String_string_free(&_t33);
                                                    _decUseStack(_t32, Value);
                                                    _decUseStack(_t31, Expr);
                                                    break;
                                                }
                                                i = _next;
                                                org_bau_String_string_free(&_t33);
                                                _decUseStack(_t32, Value);
                                                _decUseStack(_t31, Expr);
                                            }
                                            break;
                                        }
                                    }
                                    org_bau_String_StringBuilder_append_2(this->out, string_1045);
                                } else {
                                    Value* _t34 = At_call_3(this, expr->name, expr->list->array);
                                    _incUseStack(_t34);
                                    _decUseStack(result, Value);
                                    result = _t34;
                                    _decUseStack(_t34, Value);
                                }
                            } else {
                                if (expr->exprType == 1) {
                                    if (org_bau_List_List_Expr_len_1(expr->list) > 0) {
                                        while (1 == 1) {
                                            int64_t i = 0;
                                            while (1) {
                                                Expr* _t35 = org_bau_List_List_Expr_get_2(expr->list, i);
                                                Value* _t36 = At_runExpr_2(this, _t35);
                                                _incUseStack(_t36);
                                                _decUseStack(result, Value);
                                                result = _t36;
                                                if (this->hasReturnValue) {
                                                    _decUseStack(_t36, Value);
                                                    _decUseStack(_t35, Expr);
                                                    break;
                                                }
                                                int64_t _next = i + 1;
                                                if (_next >= org_bau_List_List_Expr_len_1(expr->list)) {
                                                    _decUseStack(_t36, Value);
                                                    _decUseStack(_t35, Expr);
                                                    break;
                                                }
                                                i = _next;
                                                _decUseStack(_t36, Value);
                                                _decUseStack(_t35, Expr);
                                            }
                                            break;
                                        }
                                    }
                                } else {
                                    if (expr->exprType == 10) {
                                        int64_t totalLen = 0;
                                        if (org_bau_List_List_Expr_len_1(expr->list) > 0) {
                                            while (1 == 1) {
                                                int64_t i = 0;
                                                while (1) {
                                                    if (this->hasReturnValue) {
                                                        break;
                                                    }
                                                    Expr* _t37 = org_bau_List_List_Expr_get_2(expr->list, i);
                                                    Value* r = At_runExpr_2(this, _t37);
                                                    totalLen = totalLen + r->list->size;
                                                    int64_t _next = i + 1;
                                                    if (_next >= org_bau_List_List_Expr_len_1(expr->list)) {
                                                        _decUseStack(r, Value);
                                                        _decUseStack(_t37, Expr);
                                                        break;
                                                    }
                                                    i = _next;
                                                    _decUseStack(r, Value);
                                                    _decUseStack(_t37, Expr);
                                                }
                                                break;
                                            }
                                        }
                                        org_bau_List_List_float* _t38 = org_bau_List_newList_float_1(0);
                                        Value* _t39 = Value_1(_t38);
                                        _incUseStack(_t39);
                                        _decUseStack(result, Value);
                                        result = _t39;
                                        if (org_bau_List_List_Expr_len_1(expr->list) > 0) {
                                            while (1 == 1) {
                                                int64_t i = 0;
                                                while (1) {
                                                    if (this->hasReturnValue) {
                                                        break;
                                                    }
                                                    Expr* _t40 = org_bau_List_List_Expr_get_2(expr->list, i);
                                                    Value* r = At_runExpr_2(this, _t40);
                                                    if (org_bau_List_List_float_len_1(r->list) > 0) {
                                                        while (1 == 1) {
                                                            int64_t j = 0;
                                                            while (1) {
                                                                double _t41 = org_bau_List_List_float_get_2(r->list, j);
                                                                org_bau_List_List_float_add_2(result->list, _t41);
                                                                int64_t _next = j + 1;
                                                                if (_next >= org_bau_List_List_float_len_1(r->list)) {
                                                                    break;
                                                                }
                                                                j = _next;
                                                            }
                                                            break;
                                                        }
                                                    }
                                                    int64_t _next = i + 1;
                                                    if (_next >= org_bau_List_List_Expr_len_1(expr->list)) {
                                                        _decUseStack(r, Value);
                                                        _decUseStack(_t40, Expr);
                                                        break;
                                                    }
                                                    i = _next;
                                                    _decUseStack(r, Value);
                                                    _decUseStack(_t40, Expr);
                                                }
                                                break;
                                            }
                                        }
                                        if (result->list->size == 0) {
                                            Value* _t42 = valueOf_1(0);
                                            _incUseStack(_t42);
                                            _decUseStack(result, Value);
                                            result = _t42;
                                            _decUseStack(_t42, Value);
                                        }
                                        _decUseStack(_t39, Value);
                                        _decUseStack(_t38, org_bau_List_List_float);
                                    } else {
                                        if (expr->exprType == 7) {
                                            Expr* left = org_bau_List_List_Expr_get_2(expr->list, 0);
                                            Expr* right = org_bau_List_List_Expr_get_2(expr->list, 1);
                                            if (!(left)) {
                                                Value* _t43 = valueOf_1(0);
                                                _decUseStack(right, Expr);
                                                _decUseStack(left, Expr);
                                                _decUseStack(result, Value);
                                                return _t43;
                                            }
                                            if (!(right)) {
                                                Value* _t44 = valueOf_1(0);
                                                _decUseStack(right, Expr);
                                                _decUseStack(left, Expr);
                                                _decUseStack(result, Value);
                                                return _t44;
                                            }
                                            int64_t _t45 = org_bau_String_string_charAt_2(expr->name, 0);
                                            if (_t45 == 58) {
                                                org_bau_String_string name = org_bau_String_str_1(string_1000);
                                                Expr* arrayIndex = NULL;
                                                if (left->exprType == 8) {
                                                    org_bau_String_string_copy(&left->name);
                                                    org_bau_String_string_free(&name);
                                                    name = left->name;
                                                } else {
                                                    int64_t _t46 = left->exprType == 7;
                                                    if (_t46) {
                                                        int64_t _t47 = org_bau_String_string_charAt_2(left->name, 0) == 46;
                                                        _t46 = _t47;
                                                    }
                                                    if (_t46) {
                                                        Expr* _t48 = org_bau_List_List_Expr_get_2(left->list, 0);
                                                        org_bau_String_string_copy(&_t48->name);
                                                        org_bau_String_string_free(&name);
                                                        name = _t48->name;
                                                        Expr* _t49 = org_bau_List_List_Expr_get_2(left->list, 1);
                                                        _incUseStack(_t49);
                                                        _decUseStack(arrayIndex, Expr);
                                                        arrayIndex = _t49;
                                                        _decUseStack(_t49, Expr);
                                                        _decUseStack(_t48, Expr);
                                                    }
                                                }
                                                _incUseStack(this->global);
                                                org_bau_HashMap_HashMap_org_bau_String_string_Value* map = this->global;
                                                int64_t _t50 = org_bau_String_string_len_1(name);
                                                int64_t _t51 = _t50 > 0;
                                                if (_t51) {
                                                    int64_t _t52 = org_bau_String_string_charAt_2(name, 0) > 90;
                                                    _t51 = _t52;
                                                }
                                                if (_t51) {
                                                    _incUseStack(this->local);
                                                    _decUseStack(map, org_bau_HashMap_HashMap_org_bau_String_string_Value);
                                                    map = this->local;
                                                }
                                                Value* v = At_runExpr_2(this, right);
                                                if (arrayIndex != NULL) {
                                                    Value* o = valueOf_1(0);
                                                    Value* n = At_get_3(this, map, name);
                                                    if (n != NULL) {
                                                        _incUseStack(n);
                                                        _decUseStack(o, Value);
                                                        o = n;
                                                    }
                                                    At_put_4(this, map, name, o);
                                                    Value* _t53 = At_runExpr_2(this, arrayIndex);
                                                    double _t54 = Value_get_2(_t53, 0);
                                                    int64_t index = int_1(_t54);
                                                    if (index >= 0) {
                                                        while (1 == 1) {
                                                            int64_t _t55 = Value_len_1(o);
                                                            if (!(( _t55 <= index ))) {
                                                                break;
                                                            }
                                                            Value_add_2(o, 0.0);
                                                        }
                                                        double _t56 = Value_get_2(v, 0);
                                                        Value_set_3(o, index, _t56);
                                                    }
                                                    _decUseStack(_t53, Value);
                                                    _decUseStack(n, Value);
                                                    _decUseStack(o, Value);
                                                } else {
                                                    At_put_4(this, map, name, v);
                                                }
                                                _decUseStack(map, org_bau_HashMap_HashMap_org_bau_String_string_Value);
                                                _decUseStack(arrayIndex, Expr);
                                                org_bau_String_string_free(&name);
                                                _decUseStack(right, Expr);
                                                _decUseStack(left, Expr);
                                                _decUseStack(result, Value);
                                                return v;
                                            }
                                            Value* lo = At_runExpr_2(this, left);
                                            Value* _t57 = At_runExpr_2(this, right);
                                            double r = Value_get_2(_t57, 0);
                                            int64_t _t58 = org_bau_String_string_charAt_2(expr->name, 0);
                                            if (_t58 == 46) {
                                                int64_t _t59 = r < 0;
                                                if (!(_t59)) {
                                                    int64_t _t60 = r >= float_1(Value_len_1(lo));
                                                    _t59 = _t60;
                                                }
                                                if (_t59) {
                                                    if (r == -1) {
                                                        int64_t _t61 = Value_len_1(lo);
                                                        double _t62 = float_1(_t61);
                                                        Value* _t63 = valueOf_1(_t62);
                                                        _decUseStack(_t57, Value);
                                                        _decUseStack(lo, Value);
                                                        _decUseStack(right, Expr);
                                                        _decUseStack(left, Expr);
                                                        _decUseStack(result, Value);
                                                        return _t63;
                                                    }
                                                    Value* _t64 = valueOf_1(0);
                                                    _decUseStack(_t57, Value);
                                                    _decUseStack(lo, Value);
                                                    _decUseStack(right, Expr);
                                                    _decUseStack(left, Expr);
                                                    _decUseStack(result, Value);
                                                    return _t64;
                                                }
                                                int64_t _t65 = int_1(r);
                                                double _t66 = Value_get_2(lo, _t65);
                                                Value* _t67 = valueOf_1(_t66);
                                                _decUseStack(_t57, Value);
                                                _decUseStack(lo, Value);
                                                _decUseStack(right, Expr);
                                                _decUseStack(left, Expr);
                                                _decUseStack(result, Value);
                                                return _t67;
                                            }
                                            double _t68 = Value_get_2(lo, 0);
                                            double _t69 = At_operator_5(this, expr->name, 0, _t68, r);
                                            Value* _t70 = valueOf_1(_t69);
                                            _incUseStack(_t70);
                                            _decUseStack(result, Value);
                                            result = _t70;
                                            _decUseStack(_t70, Value);
                                            _decUseStack(_t57, Value);
                                            _decUseStack(lo, Value);
                                            _decUseStack(right, Expr);
                                            _decUseStack(left, Expr);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return result;
}
void At_setFunction_3(At* this, org_bau_String_string name, Expr* expr) {
    int64_t _t0 = org_bau_HashMap_hashCode_1(name.data);
    org_bau_HashMap_HashMap_org_bau_String_string_Expr_put_4(this->functions, _t0, name, expr);
}
Expr* Expr_3(org_bau_String_string name, Value* value, org_bau_List_List_Expr* list) {
    Expr* _t41 = Expr_new();
    _t41->exprType = 0;
    org_bau_String_string_copy(&name);
    _t41->name = name;
    _incUseStack(value);
    _t41->value = value;
    _incUseStack(list);
    _t41->list = list;
    return _t41;
}
Value* Value_1(org_bau_List_List_float* list) {
    Value* _t39 = Value_new();
    _incUseStack(list);
    _t39->list = list;
    return _t39;
}
void Value_add_2(Value* this, double x) {
    org_bau_List_List_float_add_2(this->list, x);
}
double Value_get_2(Value* this, int64_t i) {
    double _r0 = this->list->array->data[idx_2(i, _arrayLen(this->list->array))];
    return _r0;
}
int64_t Value_len_1(Value* this) {
    int64_t _t0 = org_bau_List_List_float_len_1(this->list);
    return _t0;
}
void Value_set_3(Value* this, int64_t i, double x) {
    this->list->array->data[idx_2(i, _arrayLen(this->list->array))] = x;
}
org_bau_String_string convertToString_1(Value* array) {
    int64_t _t0 = Value_len_1(array);
    if (_t0 == 1) {
        double x = Value_get_2(array, 0);
        int64_t _t1 = int_1(x);
        if (x == _t1) {
            int64_t _t2 = int_1(x);
            i8_array* _t3 = org_bau_Int_intToString_1(_t2);
            org_bau_String_string _t4 = org_bau_String_str_1(_t3);
            _decUseStack(_t3, i8_array);
            return _t4;
        }
        i8_array* _t5 = org_bau_Math_floatToString_1(x);
        org_bau_String_string _t6 = org_bau_String_str_1(_t5);
        _decUseStack(_t5, i8_array);
        return _t6;
    }
    i8_array* _t7 = i8_array_new(16);
    org_bau_String_StringBuilder* buff = org_bau_String_StringBuilder_1(_t7);
    if (Value_len_1(array) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                double d = Value_get_2(array, i);
                if (d > 0) {
                    i8_array* _t8 = i8_array_new(1);
                    _incUseStack(_t8);
                    i8_array* c = _t8;
                    int64_t _t9 = int_1(d);
                    c->data[0] = _t9;
                    org_bau_String_StringBuilder_append_2(buff, c);
                    _decUseStack(c, i8_array);
                    _decUseStack(_t8, i8_array);
                }
                int64_t _next = i + 1;
                if (_next >= Value_len_1(array)) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    org_bau_String_string _t10 = org_bau_String_StringBuilder_toString_1(buff);
    _decUseStack(buff, org_bau_String_StringBuilder);
    _decUseStack(_t7, i8_array);
    return _t10;
}
double float_1(double x) {
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
At* newAt_0() {
    org_bau_HashMap_HashMap_org_bau_String_string_Value* global = org_bau_HashMap_newHashMap_org_bau_String_string_Value_2(0, 0);
    org_bau_HashMap_HashMap_org_bau_String_string_Value* local = org_bau_HashMap_newHashMap_org_bau_String_string_Value_2(0, 0);
    org_bau_HashMap_HashMap_org_bau_String_string_Expr* functions = org_bau_HashMap_newHashMap_org_bau_String_string_Expr_2(0, 0);
    org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* stack = org_bau_List_newList_org_bau_HashMap_HashMap_org_bau_String_string_Value_1(0);
    i8_array* _t0 = i8_array_new(0);
    org_bau_String_StringBuilder* out = org_bau_String_StringBuilder_1(_t0);
    i8_array* _t1 = i8_array_new(0);
    org_bau_String_string code = org_bau_String_string_1(_t1);
    i8_array* _t2 = i8_array_new(0);
    org_bau_String_string token = org_bau_String_string_1(_t2);
    Value* value = valueOf_1(0);
    Value* returnValue = valueOf_1(0);
    At* _t3 = At_9(global, local, functions, stack, out, code, token, value, returnValue);
    _decUseStack(returnValue, Value);
    _decUseStack(value, Value);
    org_bau_String_string_free(&token);
    _decUseStack(_t2, i8_array);
    org_bau_String_string_free(&code);
    _decUseStack(_t1, i8_array);
    _decUseStack(out, org_bau_String_StringBuilder);
    _decUseStack(_t0, i8_array);
    _decUseStack(stack, org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value);
    _decUseStack(functions, org_bau_HashMap_HashMap_org_bau_String_string_Expr);
    _decUseStack(local, org_bau_HashMap_HashMap_org_bau_String_string_Value);
    _decUseStack(global, org_bau_HashMap_HashMap_org_bau_String_string_Value);
    return _t3;
}
Expr* newExpr_3(int64_t exprType, i8_array* name, Value* value) {
    org_bau_String_string _t0 = org_bau_String_str_1(name);
    org_bau_List_List_Expr* _t1 = org_bau_List_newList_Expr_1(0);
    Expr* e = Expr_3(_t0, value, _t1);
    e->exprType = exprType;
    _decUseStack(_t1, org_bau_List_List_Expr);
    org_bau_String_string_free(&_t0);
    return e;
}
int64_t org_bau_Arrays_equals_i8_array_i8_2(i8_array* a, i8_array* b) {
    if (_arrayLen(a) != _arrayLen(b)) {
        return 0;
    }
    if (_arrayLen(a) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                if (a->data[i] != b->data[i]) {
                    return 0;
                }
                int64_t _next = i + 1;
                if (_next >= _arrayLen(a)) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    return 1;
}
void org_bau_Arrays_reverse_i8_array_i8_3(i8_array* buff, int64_t first, int64_t last) {
    while (first < last) {
        int8_t temp = buff->data[first];
        buff->data[first] = buff->data[last];
        buff->data[last] = temp;
        if (first >= ( _arrayLen(buff) - 1 )) {
            break;
        }
        first = first + 1;
        if (last < 1) {
            break;
        }
        last = last - 1;
    }
}
org_bau_HashMap_HashMap_org_bau_String_string_Expr* org_bau_HashMap_HashMap_org_bau_String_string_Expr_3(org_bau_String_string_array* keys, Expr_array* values, int_array* hashes) {
    org_bau_HashMap_HashMap_org_bau_String_string_Expr* _t43 = org_bau_HashMap_HashMap_org_bau_String_string_Expr_new();
    _t43->size = 0;
    _incUseStack(keys);
    _t43->keys = keys;
    _incUseStack(values);
    _t43->values = values;
    _incUseStack(hashes);
    _t43->hashes = hashes;
    return _t43;
}
org_bau_HashMap_HashMap_org_bau_String_string_Value* org_bau_HashMap_HashMap_org_bau_String_string_Value_3(org_bau_String_string_array* keys, Value_array* values, int_array* hashes) {
    org_bau_HashMap_HashMap_org_bau_String_string_Value* _t42 = org_bau_HashMap_HashMap_org_bau_String_string_Value_new();
    _t42->size = 0;
    _incUseStack(keys);
    _t42->keys = keys;
    _incUseStack(values);
    _t42->values = values;
    _incUseStack(hashes);
    _t42->hashes = hashes;
    return _t42;
}
int64_t org_bau_HashMap_hashCode_1(i8_array* data) {
    if (_arrayLen(data) <= 0) {
        return 0;
    }
    int64_t h = shiftLeft_2(_arrayLen(data), 32);
    int64_t i = 0;
    if (_arrayLen(data) >= 4) {
        int64_t j = 0;
        while (1) {
            int64_t x = (data->data[j] & 255) | (shiftLeft_2((data->data[j + 1] & 255), 8)) | (shiftLeft_2((data->data[j + 2] & 255), 16)) | (shiftLeft_2((data->data[j + 3] & 255), 24));
            h = (h + x) * -7046029288634856825;
            int64_t n = j + 4;
            if (( n + 4 ) >= _arrayLen(data)) {
                break;
            }
            j = n;
        }
    }
    int64_t _t0 = i < _arrayLen(data);
    if (_t0) {
        int64_t _t1 = i != ( _arrayLen(data) - 4 );
        _t0 = _t1;
    }
    if (_t0) {
        int64_t x = 0;
        int64_t s = 0;
        while (1) {
            x = x ^ ( shiftLeft_2((data->data[i] & 255), s) );
            s = s + 8;
            int64_t n = i + 1;
            if (n >= _arrayLen(data)) {
                break;
            }
            i = n;
        }
        h = (h + x) * -7046029288634856825;
    }
    int64_t _r0 = h ^ (shiftRight_int_2(h, 32));
    return _r0;
}
org_bau_HashMap_HashMap_org_bau_String_string_Expr* org_bau_HashMap_newHashMap_org_bau_String_string_Expr_2(int64_t _K, int64_t _V) {
    org_bau_String_string_array* _t54 = org_bau_String_string_array_new(4);
    Expr_array* _t55 = Expr_array_new(4);
    int_array* _t56 = int_array_new(4);
    org_bau_HashMap_HashMap_org_bau_String_string_Expr* _t57 = org_bau_HashMap_HashMap_org_bau_String_string_Expr_3(_t54, _t55, _t56);
    _decUseStack(_t56, int_array);
    _decUseStack(_t55, Expr_array);
    _decUseStack(_t54, org_bau_String_string_array);
    return _t57;
}
org_bau_HashMap_HashMap_org_bau_String_string_Value* org_bau_HashMap_newHashMap_org_bau_String_string_Value_2(int64_t _K, int64_t _V) {
    org_bau_String_string_array* _t46 = org_bau_String_string_array_new(4);
    Value_array* _t47 = Value_array_new(4);
    int_array* _t48 = int_array_new(4);
    org_bau_HashMap_HashMap_org_bau_String_string_Value* _t49 = org_bau_HashMap_HashMap_org_bau_String_string_Value_3(_t46, _t47, _t48);
    _decUseStack(_t48, int_array);
    _decUseStack(_t47, Value_array);
    _decUseStack(_t46, org_bau_String_string_array);
    return _t49;
}
Expr* org_bau_HashMap_HashMap_org_bau_String_string_Expr_get_3(org_bau_HashMap_HashMap_org_bau_String_string_Expr* this, int64_t hash, org_bau_String_string key) {
    int64_t p = hash & (_arrayLen(this->keys) - 1);
    while (1) {
        if (this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == 0) {
            _incUseStack(this->values->data[idx_2(p, _arrayLen(this->values))]);
            Expr* _r0 = this->values->data[idx_2(p, _arrayLen(this->values))];
            return _r0;
        } else {
            int64_t _t0 = this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == hash;
            if (_t0) {
                int64_t _t1 = org_bau_String_string_equals_2(key, this->keys->data[idx_2(p, _arrayLen(this->keys))]);
                _t0 = _t1;
            }
            if (_t0) {
                _incUseStack(this->values->data[idx_2(p, _arrayLen(this->values))]);
                Expr* _r1 = this->values->data[idx_2(p, _arrayLen(this->values))];
                return _r1;
            }
        }
        p = (p + 1) & (_arrayLen(this->keys) - 1);
    }
}
void org_bau_HashMap_HashMap_org_bau_String_string_Expr_put_4(org_bau_HashMap_HashMap_org_bau_String_string_Expr* this, int64_t hash, org_bau_String_string key, Expr* value) {
    if (( this->size * 2 ) >= _arrayLen(this->keys)) {
        org_bau_String_string_array* _t0 = org_bau_String_string_array_new(_arrayLen(this->keys) * 2);
        _incUseStack(_t0);
        org_bau_String_string_array* kn = _t0;
        Expr_array* _t1 = Expr_array_new(_arrayLen(this->keys) * 2);
        _incUseStack(_t1);
        Expr_array* vn = _t1;
        int_array* _t2 = int_array_new(_arrayLen(this->keys) * 2);
        _incUseStack(_t2);
        int_array* hn = _t2;
        _incUseStack(this->keys);
        org_bau_String_string_array* ok = this->keys;
        _incUseStack(this->hashes);
        int_array* oh = this->hashes;
        _incUseStack(this->values);
        Expr_array* ov = this->values;
        _incUseStack(kn);
        _decUse(this->keys, org_bau_String_string_array);
        this->keys = kn;
        _incUseStack(vn);
        _decUse(this->values, Expr_array);
        this->values = vn;
        _incUseStack(hn);
        _decUse(this->hashes, int_array);
        this->hashes = hn;
        if (_arrayLen(ok) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    if (oh->data[idx_2(i, _arrayLen(oh))] != 0) {
                        org_bau_HashMap_HashMap_org_bau_String_string_Expr_put_4(this, oh->data[idx_2(i, _arrayLen(oh))], ok->data[i], ov->data[idx_2(i, _arrayLen(ov))]);
                    }
                    int64_t _next = i + 1;
                    if (_next >= _arrayLen(ok)) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        _decUseStack(ov, Expr_array);
        _decUseStack(oh, int_array);
        _decUseStack(ok, org_bau_String_string_array);
        _decUseStack(hn, int_array);
        _decUseStack(_t2, int_array);
        _decUseStack(vn, Expr_array);
        _decUseStack(_t1, Expr_array);
        _decUseStack(kn, org_bau_String_string_array);
        _decUseStack(_t0, org_bau_String_string_array);
    }
    int64_t p = hash & (_arrayLen(this->keys) - 1);
    while (1) {
        int64_t _t3 = this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == hash;
        if (_t3) {
            int64_t _t4 = org_bau_String_string_equals_2(key, this->keys->data[idx_2(p, _arrayLen(this->keys))]);
            _t3 = _t4;
        }
        if (_t3) {
            _incUseStack(value);
            _decUse(this->values->data[idx_2(p, _arrayLen(this->values))], Expr);
            this->values->data[idx_2(p, _arrayLen(this->values))] = value;
            return;
        } else {
            if (this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == 0) {
                this->size += 1;
                org_bau_String_string_copy(&key);
                this->keys->data[idx_2(p, _arrayLen(this->keys))] = key;
                this->hashes->data[idx_2(p, _arrayLen(this->hashes))] = hash;
                _incUseStack(value);
                _decUse(this->values->data[idx_2(p, _arrayLen(this->values))], Expr);
                this->values->data[idx_2(p, _arrayLen(this->values))] = value;
                return;
            }
        }
        p = (p + 1) & (_arrayLen(this->keys) - 1);
    }
}
Value* org_bau_HashMap_HashMap_org_bau_String_string_Value_get_3(org_bau_HashMap_HashMap_org_bau_String_string_Value* this, int64_t hash, org_bau_String_string key) {
    int64_t p = hash & (_arrayLen(this->keys) - 1);
    while (1) {
        if (this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == 0) {
            _incUseStack(this->values->data[idx_2(p, _arrayLen(this->values))]);
            Value* _r0 = this->values->data[idx_2(p, _arrayLen(this->values))];
            return _r0;
        } else {
            int64_t _t0 = this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == hash;
            if (_t0) {
                int64_t _t1 = org_bau_String_string_equals_2(key, this->keys->data[idx_2(p, _arrayLen(this->keys))]);
                _t0 = _t1;
            }
            if (_t0) {
                _incUseStack(this->values->data[idx_2(p, _arrayLen(this->values))]);
                Value* _r1 = this->values->data[idx_2(p, _arrayLen(this->values))];
                return _r1;
            }
        }
        p = (p + 1) & (_arrayLen(this->keys) - 1);
    }
}
void org_bau_HashMap_HashMap_org_bau_String_string_Value_put_4(org_bau_HashMap_HashMap_org_bau_String_string_Value* this, int64_t hash, org_bau_String_string key, Value* value) {
    if (( this->size * 2 ) >= _arrayLen(this->keys)) {
        org_bau_String_string_array* _t0 = org_bau_String_string_array_new(_arrayLen(this->keys) * 2);
        _incUseStack(_t0);
        org_bau_String_string_array* kn = _t0;
        Value_array* _t1 = Value_array_new(_arrayLen(this->keys) * 2);
        _incUseStack(_t1);
        Value_array* vn = _t1;
        int_array* _t2 = int_array_new(_arrayLen(this->keys) * 2);
        _incUseStack(_t2);
        int_array* hn = _t2;
        _incUseStack(this->keys);
        org_bau_String_string_array* ok = this->keys;
        _incUseStack(this->hashes);
        int_array* oh = this->hashes;
        _incUseStack(this->values);
        Value_array* ov = this->values;
        _incUseStack(kn);
        _decUse(this->keys, org_bau_String_string_array);
        this->keys = kn;
        _incUseStack(vn);
        _decUse(this->values, Value_array);
        this->values = vn;
        _incUseStack(hn);
        _decUse(this->hashes, int_array);
        this->hashes = hn;
        if (_arrayLen(ok) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    if (oh->data[idx_2(i, _arrayLen(oh))] != 0) {
                        org_bau_HashMap_HashMap_org_bau_String_string_Value_put_4(this, oh->data[idx_2(i, _arrayLen(oh))], ok->data[i], ov->data[idx_2(i, _arrayLen(ov))]);
                    }
                    int64_t _next = i + 1;
                    if (_next >= _arrayLen(ok)) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        _decUseStack(ov, Value_array);
        _decUseStack(oh, int_array);
        _decUseStack(ok, org_bau_String_string_array);
        _decUseStack(hn, int_array);
        _decUseStack(_t2, int_array);
        _decUseStack(vn, Value_array);
        _decUseStack(_t1, Value_array);
        _decUseStack(kn, org_bau_String_string_array);
        _decUseStack(_t0, org_bau_String_string_array);
    }
    int64_t p = hash & (_arrayLen(this->keys) - 1);
    while (1) {
        int64_t _t3 = this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == hash;
        if (_t3) {
            int64_t _t4 = org_bau_String_string_equals_2(key, this->keys->data[idx_2(p, _arrayLen(this->keys))]);
            _t3 = _t4;
        }
        if (_t3) {
            _incUseStack(value);
            _decUse(this->values->data[idx_2(p, _arrayLen(this->values))], Value);
            this->values->data[idx_2(p, _arrayLen(this->values))] = value;
            return;
        } else {
            if (this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == 0) {
                this->size += 1;
                org_bau_String_string_copy(&key);
                this->keys->data[idx_2(p, _arrayLen(this->keys))] = key;
                this->hashes->data[idx_2(p, _arrayLen(this->hashes))] = hash;
                _incUseStack(value);
                _decUse(this->values->data[idx_2(p, _arrayLen(this->values))], Value);
                this->values->data[idx_2(p, _arrayLen(this->values))] = value;
                return;
            }
        }
        p = (p + 1) & (_arrayLen(this->keys) - 1);
    }
}
int64_t org_bau_Int_appendInt_3(int64_t n, i8_array* buff, int64_t pos) {
    if (_arrayLen(buff) < 1) {
        return pos;
    }
    int64_t p = 0;
    if (pos >= _arrayLen(buff)) {
        return pos;
    }
    if (pos < 0) {
        return pos;
    }
    p = pos;
    if (n < 0) {
        buff->data[p] = 45;
        pos = pos + 1;
        if (pos >= _arrayLen(buff)) {
            return pos;
        }
        if (pos < 0) {
            return 0;
        }
        p = pos;
    } else {
        n = - n;
    }
    int64_t start = p;
    while (1) {
        buff->data[p] = 48 - (imod_2(n, 10));
        n = idiv_2(n, 10);
        pos = pos + 1;
        if (pos >= _arrayLen(buff)) {
            return pos;
        }
        if (pos < 0) {
            return 0;
        }
        if (n == 0) {
            break;
        }
        p = pos;
    }
    int64_t end = pos;
    org_bau_Arrays_reverse_i8_array_i8_3(buff, start, p);
    return end;
}
i8_array* org_bau_Int_intToString_1(int64_t n) {
    i8_array* _t0 = i8_array_new(20);
    _incUseStack(_t0);
    i8_array* buff = _t0;
    int64_t pos = org_bau_Int_appendInt_3(n, buff, 0);
    i8_array* _t1 = i8_array_new(pos);
    _incUseStack(_t1);
    i8_array* result = _t1;
    if (pos > 0) {
        while (1 == 1) {
            int64_t j = 0;
            while (1) {
                result->data[j] = buff->data[idx_2(j, _arrayLen(buff))];
                int64_t _next = j + 1;
                if (_next >= pos) {
                    break;
                }
                j = _next;
            }
            break;
        }
    }
    _decUseStack(_t1, i8_array);
    _decUseStack(buff, i8_array);
    _decUseStack(_t0, i8_array);
    return result;
}
org_bau_List_List_Expr* org_bau_List_List_Expr_1(Expr_array* array) {
    org_bau_List_List_Expr* _t40 = org_bau_List_List_Expr_new();
    _incUseStack(array);
    _t40->array = array;
    _t40->size = 0;
    return _t40;
}
org_bau_List_List_float* org_bau_List_List_float_1(float_array* array) {
    org_bau_List_List_float* _t38 = org_bau_List_List_float_new();
    _incUseStack(array);
    _t38->array = array;
    _t38->size = 0;
    return _t38;
}
org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_1(org_bau_HashMap_HashMap_org_bau_String_string_Value_array* array) {
    org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* _t44 = org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_new();
    _incUseStack(array);
    _t44->array = array;
    _t44->size = 0;
    return _t44;
}
org_bau_List_List_Expr* org_bau_List_newList_Expr_1(int64_t _T) {
    Expr_array* _t52 = Expr_array_new(4);
    org_bau_List_List_Expr* _t53 = org_bau_List_List_Expr_1(_t52);
    _decUseStack(_t52, Expr_array);
    return _t53;
}
org_bau_List_List_float* org_bau_List_newList_float_1(int64_t _T) {
    float_array* _t50 = float_array_new(4);
    org_bau_List_List_float* _t51 = org_bau_List_List_float_1(_t50);
    _decUseStack(_t50, float_array);
    return _t51;
}
org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* org_bau_List_newList_org_bau_HashMap_HashMap_org_bau_String_string_Value_1(int64_t _T) {
    org_bau_HashMap_HashMap_org_bau_String_string_Value_array* _t58 = org_bau_HashMap_HashMap_org_bau_String_string_Value_array_new(4);
    org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* _t59 = org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_1(_t58);
    _decUseStack(_t58, org_bau_HashMap_HashMap_org_bau_String_string_Value_array);
    return _t59;
}
void org_bau_List_List_Expr_add_2(org_bau_List_List_Expr* this, Expr* x) {
    if (this->size >= _arrayLen(this->array)) {
        Expr_array* _t0 = Expr_array_new(_arrayLen(this->array) * 2);
        _incUseStack(_t0);
        Expr_array* n = _t0;
        if (_arrayLen(this->array) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    _incUseStack(this->array->data[i]);
                    _decUse(n->data[idx_2(i, _arrayLen(n))], Expr);
                    n->data[idx_2(i, _arrayLen(n))] = this->array->data[i];
                    int64_t _next = i + 1;
                    if (_next >= _arrayLen(this->array)) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        _incUseStack(n);
        _decUse(this->array, Expr_array);
        this->array = n;
        _decUseStack(n, Expr_array);
        _decUseStack(_t0, Expr_array);
    }
    _incUseStack(x);
    _decUse(this->array->data[idx_2(this->size, _arrayLen(this->array))], Expr);
    this->array->data[idx_2(this->size, _arrayLen(this->array))] = x;
    this->size += 1;
}
Expr* org_bau_List_List_Expr_get_2(org_bau_List_List_Expr* this, int64_t x) {
    _incUseStack(this->array->data[idx_2(x, _arrayLen(this->array))]);
    Expr* _r0 = this->array->data[idx_2(x, _arrayLen(this->array))];
    return _r0;
}
int64_t org_bau_List_List_Expr_len_1(org_bau_List_List_Expr* this) {
    int64_t _r0 = this->size;
    return _r0;
}
void org_bau_List_List_float_add_2(org_bau_List_List_float* this, double x) {
    if (this->size >= _arrayLen(this->array)) {
        float_array* _t0 = float_array_new(_arrayLen(this->array) * 2);
        _incUseStack(_t0);
        float_array* n = _t0;
        if (_arrayLen(this->array) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    n->data[idx_2(i, _arrayLen(n))] = this->array->data[i];
                    int64_t _next = i + 1;
                    if (_next >= _arrayLen(this->array)) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        _incUseStack(n);
        _decUse(this->array, float_array);
        this->array = n;
        _decUseStack(n, float_array);
        _decUseStack(_t0, float_array);
    }
    this->array->data[idx_2(this->size, _arrayLen(this->array))] = x;
    this->size += 1;
}
double org_bau_List_List_float_get_2(org_bau_List_List_float* this, int64_t x) {
    double _r0 = this->array->data[idx_2(x, _arrayLen(this->array))];
    return _r0;
}
int64_t org_bau_List_List_float_len_1(org_bau_List_List_float* this) {
    int64_t _r0 = this->size;
    return _r0;
}
void org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_add_2(org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* this, org_bau_HashMap_HashMap_org_bau_String_string_Value* x) {
    if (this->size >= _arrayLen(this->array)) {
        org_bau_HashMap_HashMap_org_bau_String_string_Value_array* _t0 = org_bau_HashMap_HashMap_org_bau_String_string_Value_array_new(_arrayLen(this->array) * 2);
        _incUseStack(_t0);
        org_bau_HashMap_HashMap_org_bau_String_string_Value_array* n = _t0;
        if (_arrayLen(this->array) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    _incUseStack(this->array->data[i]);
                    _decUse(n->data[idx_2(i, _arrayLen(n))], org_bau_HashMap_HashMap_org_bau_String_string_Value);
                    n->data[idx_2(i, _arrayLen(n))] = this->array->data[i];
                    int64_t _next = i + 1;
                    if (_next >= _arrayLen(this->array)) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        _incUseStack(n);
        _decUse(this->array, org_bau_HashMap_HashMap_org_bau_String_string_Value_array);
        this->array = n;
        _decUseStack(n, org_bau_HashMap_HashMap_org_bau_String_string_Value_array);
        _decUseStack(_t0, org_bau_HashMap_HashMap_org_bau_String_string_Value_array);
    }
    _incUseStack(x);
    _decUse(this->array->data[idx_2(this->size, _arrayLen(this->array))], org_bau_HashMap_HashMap_org_bau_String_string_Value);
    this->array->data[idx_2(this->size, _arrayLen(this->array))] = x;
    this->size += 1;
}
void org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value_remove_2(org_bau_List_List_org_bau_HashMap_HashMap_org_bau_String_string_Value* this, int64_t pos) {
    while (pos < ( this->size - 1 )) {
        _incUseStack(this->array->data[idx_2(pos + 1, _arrayLen(this->array))]);
        _decUse(this->array->data[idx_2(pos, _arrayLen(this->array))], org_bau_HashMap_HashMap_org_bau_String_string_Value);
        this->array->data[idx_2(pos, _arrayLen(this->array))] = this->array->data[idx_2(pos + 1, _arrayLen(this->array))];
        pos = pos + 1;
    }
    this->size -= 1;
    _decUse(this->array->data[idx_2(pos, _arrayLen(this->array))], org_bau_HashMap_HashMap_org_bau_String_string_Value);
    this->array->data[idx_2(pos, _arrayLen(this->array))] = NULL;
}
int64_t org_bau_Math_appendFloat_3(double n, i8_array* buff, int64_t pos) {
    int64_t e = 0;
    int64_t _t0 = n >= 1.0E8;
    if (!(_t0)) {
        int64_t _t1 = n <= 0.001;
        _t0 = _t1;
    }
    if (_t0) {
        while (n > 1.0E20) {
            n = n / 1.0E20;
            e = e + 20;
        }
        while (n < 1.0E-20) {
            n = n * 1.0E20;
            e = e - 20;
        }
        while (n >= 10) {
            n = n / 10;
            e = e + 1;
        }
        while (n < 1) {
            n = n * 10;
            e = e - 1;
        }
        int64_t _t2 = org_bau_Math_appendFloat_3(n, buff, pos);
        pos = _t2;
        buff->data[idx_2(pos, _arrayLen(buff))] = 69;
        pos = pos + 1;
        int64_t _t3 = org_bau_Int_appendInt_3(e, buff, pos);
        pos = _t3;
        return pos;
    }
    int64_t _t4 = int_1(n);
    int64_t _t5 = org_bau_Int_appendInt_3(_t4, buff, pos);
    pos = _t5;
    buff->data[idx_2(pos, _arrayLen(buff))] = 46;
    pos = pos + 1;
    int64_t _t6 = int_1(n);
    n = (n - _t6) * 10;
    int64_t y = int_1(n * 100000000000000000);
    if (19 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                if (y <= 0) {
                    break;
                }
                int64_t x = idiv_2(y, 100000000000000000);
                buff->data[idx_2(pos, _arrayLen(buff))] = 48 + (imod_2(x, 10));
                pos = pos + 1;
                y = y - ( x * 100000000000000000 );
                y = y * 10;
                int64_t _next = i + 1;
                if (_next >= 19) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    return pos;
}
i8_array* org_bau_Math_floatToString_1(double n) {
    int64_t _t0 = org_bau_Math_isNotANumber_1(n);
    if (_t0) {
        return string_1011;
    }
    if (n == (1.0 / 0.0)) {
        return string_1012;
    }
    if (n == (-1.0 / 0.0)) {
        return string_1013;
    }
    i8_array* _t1 = i8_array_new(30);
    _incUseStack(_t1);
    i8_array* buff = _t1;
    int64_t pos = 0;
    if (n < 0) {
        buff->data[0] = 45;
        pos = 1;
        n = - n;
    } else {
    }
    int64_t end = org_bau_Math_appendFloat_3(n, buff, pos);
    i8_array* _t2 = i8_array_new(end);
    _incUseStack(_t2);
    i8_array* result = _t2;
    if (end > 0) {
        while (1 == 1) {
            int64_t j = 0;
            while (1) {
                result->data[j] = buff->data[idx_2(j, _arrayLen(buff))];
                int64_t _next = j + 1;
                if (_next >= end) {
                    break;
                }
                j = _next;
            }
            break;
        }
    }
    _decUseStack(_t2, i8_array);
    _decUseStack(buff, i8_array);
    _decUseStack(_t1, i8_array);
    return result;
}
int64_t org_bau_Math_isNotANumber_1(double x) {
    return isnan(x);
    int64_t _r0 = x != x;
    return _r0;
}
org_bau_String_StringBuilder* org_bau_String_StringBuilder_1(i8_array* data) {
    org_bau_String_StringBuilder* _t1 = org_bau_String_StringBuilder_new();
    _incUseStack(data);
    _t1->data = data;
    _t1->len = 0;
    return _t1;
}
org_bau_String_string org_bau_String_str_1(i8_array* s) {
    org_bau_String_string _t0 = org_bau_String_string_1(s);
    return _t0;
}
org_bau_String_string org_bau_String_string_1(i8_array* data) {
    org_bau_String_string _t0 = org_bau_String_string_new();
    _incUseStack(data);
    _t0.data = data;
    return _t0;
}
i8_array* org_bau_String_substring_3(i8_array* s, int64_t start, int64_t end) {
    _incUseStack(s);
    int64_t len = end - start;
    int64_t _t0 = len < 0;
    if (!(_t0)) {
        int64_t _t1 = start < 0;
        _t0 = _t1;
    }
    int64_t _t2 = _t0;
    if (!(_t2)) {
        int64_t _t3 = end > _arrayLen(s);
        _t2 = _t3;
    }
    if (_t2) {
        _decUseStack(s, i8_array);
        return string_1000;
    }
    if (_arrayLen(s) <= 0) {
        _decUseStack(s, i8_array);
        return string_1000;
    }
    int64_t i = 0;
    if (start >= _arrayLen(s)) {
        _decUseStack(s, i8_array);
        return string_1000;
    }
    if (start < 0) {
        start = 0;
    }
    i = start;
    i8_array* _t4 = i8_array_new(len);
    _incUseStack(_t4);
    i8_array* result = _t4;
    if (len > 0) {
        while (1 == 1) {
            int64_t j = 0;
            while (1) {
                result->data[j] = s->data[i];
                int64_t next = i + 1;
                if (next >= _arrayLen(s)) {
                    break;
                }
                i = next;
                int64_t _next = j + 1;
                if (_next >= len) {
                    break;
                }
                j = _next;
            }
            break;
        }
    }
    _decUseStack(_t4, i8_array);
    _decUseStack(s, i8_array);
    return result;
}
void org_bau_String_StringBuilder_append_2(org_bau_String_StringBuilder* this, i8_array* b) {
    org_bau_String_StringBuilder_append_4(this, b, 0, _arrayLen(b));
}
void org_bau_String_StringBuilder_append_4(org_bau_String_StringBuilder* this, i8_array* b, int64_t start, int64_t end) {
    int64_t add = end - start;
    int64_t _t0 = start >= _arrayLen(b);
    if (!(_t0)) {
        int64_t _t1 = end < start;
        _t0 = _t1;
    }
    int64_t _t2 = _t0;
    if (!(_t2)) {
        int64_t _t3 = end > _arrayLen(b);
        _t2 = _t3;
    }
    if (_t2) {
        return;
    }
    while (( this->len + add ) > _arrayLen(this->data)) {
        i8_array* _t4 = i8_array_new(_arrayLen(this->data) * 2);
        _incUseStack(_t4);
        i8_array* n = _t4;
        if (_arrayLen(this->data) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    n->data[idx_2(i, _arrayLen(n))] = this->data->data[i];
                    int64_t _next = i + 1;
                    if (_next >= _arrayLen(this->data)) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        _incUseStack(n);
        _decUse(this->data, i8_array);
        this->data = n;
        _decUseStack(n, i8_array);
        _decUseStack(_t4, i8_array);
    }
    if (add > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                this->data->data[idx_2(this->len + i, _arrayLen(this->data))] = b->data[idx_2(start + i, _arrayLen(b))];
                int64_t _next = i + 1;
                if (_next >= add) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    this->len += add;
}
org_bau_String_string org_bau_String_StringBuilder_toString_1(org_bau_String_StringBuilder* this) {
    i8_array* _t0 = org_bau_String_substring_3(this->data, 0, this->len);
    org_bau_String_string _t1 = org_bau_String_str_1(_t0);
    _decUseStack(_t0, i8_array);
    return _t1;
}
int64_t org_bau_String_string_charAt_2(org_bau_String_string this, int64_t i) {
    int64_t _t0 = i < 0;
    if (!(_t0)) {
        int64_t _t1 = i >= _arrayLen(this.data);
        _t0 = _t1;
    }
    if (_t0) {
        return 0;
    }
    int8_t _r0 = this.data->data[idx_2(i, _arrayLen(this.data))];
    return _r0;
}
int64_t org_bau_String_string_equals_2(org_bau_String_string this, org_bau_String_string other) {
    int64_t _t0 = org_bau_Arrays_equals_i8_array_i8_2(this.data, other.data);
    return _t0;
}
int64_t org_bau_String_string_len_1(org_bau_String_string this) {
    int32_t _r0 = _arrayLen(this.data);
    return _r0;
}
org_bau_String_string org_bau_String_string_substring_3(org_bau_String_string this, int64_t start, int64_t end) {
    org_bau_String_string_copy(&this);
    i8_array* _t0 = org_bau_String_substring_3(this.data, start, end);
    org_bau_String_string _t1 = org_bau_String_str_1(_t0);
    _decUseStack(_t0, i8_array);
    org_bau_String_string_free(&this);
    return _t1;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
Value* valueOf_1(double v) {
    float_array* _t0 = float_array_new(1);
    org_bau_List_List_float* list = org_bau_List_List_float_1(_t0);
    list->size = 1;
    list->array->data[idx_2(0, _arrayLen(list->array))] = v;
    Value* _t1 = Value_1(list);
    _decUseStack(list, org_bau_List_List_float);
    _decUseStack(_t0, float_array);
    return _t1;
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("", 0);
    string_1001 = str_const("-", 1);
    string_1002 = str_const("0", 1);
    string_1011 = str_const("NaN", 3);
    string_1012 = str_const("Infinity", 8);
    string_1013 = str_const("-Infinity", 9);
    string_1014 = str_const("&", 1);
    string_1015 = str_const("|", 1);
    string_1016 = str_const("=", 1);
    string_1017 = str_const("<>", 2);
    string_1018 = str_const(">", 1);
    string_1019 = str_const("<", 1);
    string_1020 = str_const(">=", 2);
    string_1021 = str_const("<=", 2);
    string_1022 = str_const("+", 1);
    string_1023 = str_const("*", 1);
    string_1024 = str_const("/", 1);
    string_1025 = str_const("%", 1);
    string_1026 = str_const(":", 1);
    string_1029 = str_const("@", 1);
    string_1030 = str_const("while", 5);
    string_1031 = str_const("repeat", 6);
    string_1032 = str_const("?", 1);
    string_1033 = str_const("if", 2);
    string_1034 = str_const("else", 4);
    string_1035 = str_const("return", 6);
    string_1036 = str_const("(", 1);
    string_1037 = str_const(")", 1);
    string_1038 = str_const(",", 1);
    string_1039 = str_const("{", 1);
    string_1040 = str_const("}", 1);
    string_1041 = str_const(";", 1);
    string_1042 = str_const("\'", 1);
    string_1043 = str_const("print", 5);
    string_1045 = str_const("\n", 1);
    string_1048 = str_const("_", 1);
    string_1049 = str_const("fun", 3);
    string_1050 = str_const("\'Math Library, partially minified\';\nPOS_INFINITY:1/0;NEG_INFINITY:-1/0;NOT_A_NUMBER:0/0;\nPI:3.14159265358979323846;E:2.71828182845904523536;\nLOG10:2.30258509299404568402;LOG2:0.69314718055994530942;\nfun abs(x)       {?x>=0{=x};=-x}\nfun isNaN(x)     {=x<>x}\nfun isNegZero(x) {=(x=0)&(1/x=NEG_INFINITY)}\nfun min(a,b)     { if isNaN(a) { return a } \n           if a = b & a = 0 & isNegZero(b) { return b }\n           if a <= b {return a }\n           return b }\nfun max(a,b)     {\xc2\xa0if isNaN(a) {\xc2\xa0return a } \n           if a = b & a = 0 & isNegZero(a) { return b }\n           if a >= b {\xc2\xa0return a }\n           return b }\nfun floor(x)     {\xc2\xa0if isNaN(x) | x = 0 { return x }\n           i : x | 0; d : i;\n           if d = x | x >= 0 { return d } \n           return d - 1 }\nfun ceil(x)      {\xc2\xa0return -floor(-x) }\nfun round(x)     {\xc2\xa0if isNaN(x) { return 0 }\n           return (x + 0.5) | 0 }\nfun signum(x)    {\xc2\xa0if x = 0.0 | isNaN(x) { return x }\n           if x >= 0 {\xc2\xa0return 1 }\n           return -1 }\nfun exp(x)       {\xc2\xa0if isNaN(x) | x = POS_INFINITY { return x }\n           if x < 0 { return 1 / exp(-x) }\n           if x > 2 { r : exp(x / 2);  return r * r }\n           approx : 1.0; term : 1.0; \n           i : 1; while i < 22 { term:term*x/i ; approx:approx+term; i:i+1 }\n           return approx }\nfun log(x)       { if x = 0 {\xc2\xa0return NEG_INFINITY }\n           if x = POS_INFINITY { return x }\n           if x <= 0 | isNaN(x) { return NOT_A_NUMBER }\n           if x < 0.7 { return log(2 * x) - LOG2 }\n           if x >= 1.5 { return log(x / 2) + LOG2 }\n           base:x-1; sign:1; term:base; result:term;\n           i : 2; while i < 30 {sign:-sign; term : term * base; \n           result:result+sign*term/i; i:i+1}\n           return result }\nfun sqrt(x)      {=exp(log(x)/2)}\nfun log10(x)     {=log(x)/LOG10}\nfun e(x,n)       {@n>0{x:x*10;n:n-1};@n<0{x:x/10;n:n+1};=x} \nfun pow(x,y)     { if x > 0 & y > 0 { return exp(y * log(x)) }\n           if y = 0.0 { return 1.0 }\n           if isNaN(x) | isNaN(y) { return NOT_A_NUMBER }\n           absX : abs(x); if y = NEG_INFINITY {\n             if absX > 1.0 { return 0.0 }\n             if absX = 1.0 { return NOT_A_NUMBER }\n             return POS_INFINITY }\n           yy : y | 0; odd : (yy = y) & ((yy & 1) = 1);\n           if isNegZero(x) & yy = y { if odd { return -pow(-x, y) } return pow(-x, y) }\n           if x >= 0 { return exp(y * log(x)) }\n           if odd { return -pow(-x, y) }\n           if x = NEG_INFINITY { if y < 0 { return 0.0 } return POS_INFINITY }\n           if yy = y { return pow(-x, y) }\n           if abs(y) > e(1,19) { if y > 0 { return pow(-x, y) } \n           if x = -1 { return 1 } if absX > 1 { return 0 } return POS_INFINITY }\n           return NOT_A_NUMBER }\nfun sin(x)       { if x = POS_INFINITY { return NOT_A_NUMBER }\n           if abs(x) < e(1,-8) { return x }\n           if x < 0 {return -sin(-x) }\n           if x > PI * 2 { x : x - floor(x / (PI * 2)) * (PI * 2) }\n           if x > PI { return -sin(x - PI) }\n           if x > PI / 2 { x : PI - x }\n           approx : 0.0; sign : 1; p : x; n : 2;\n           while n < 24 {\n             approx : approx + sign * p;\n             p : p * x * x / (n * (n + 1)); sign : -sign; n:n+2;\n           } return approx; }\nfun atan(x)      { x : min(max(e(-1,16), x), e(1,16));\n           if abs(x) >= 0.4 { return 2 * atan(x / (1 + sqrt(1 + x * x))) }\n           approx : x; sign : -1; p : x;\n           n:3; while n < 32 {\n             p : p * x * x; approx : approx + sign * p / n;\n             sign : -sign; n:n+1\n           } return approx; }\nfun cos(x) {=sin(x+PI/2)}\nfun tan(x) {=sin(x)/cos(x)}\nfun asin(x) {=atan(x/sqrt(1-x*x))}\nfun acos(x) {=PI/2-asin(x)}\n\nprint(\'pow(2, 10) \' pow(2, 10));\nprint(\'sqrt(2) \' sqrt(2));\nprint(\'sin(0.5) \' sin(0.5));\nprint(\'cos(0.5) \' cos(0.5));\nprint(\'tan(0.5) \' tan(0.5));\nprint(\'asin(0.5) \' asin(0.5));\nprint(\'acos(0.5) \' acos(0.5));\nprint(\'atan(0.5) \' atan(0.5));", 3938);
    _main();
    return 0;
}
void _main() {
    randomSeed = 0;
    MIN_INT = 0x8000000000000000;
    MAX_INT = 0x7fffffffffffffff;
    MIN_I32 = -2147483648;
    MAX_I32 = 4294967295;
    POS_INFINITY = (1.0 / 0.0);
    NEG_INFINITY = (-1.0 / 0.0);
    NOT_A_NUMBER = (0.0 / 0.0);
    PI = 3.141592653589793;
    E = 2.718281828459045;
    LOG10 = 2.302585092994046;
    LOG2 = 0.6931471805599453;
    BLOCK = 1;
    CALL = 2;
    IF = 3;
    LITERAL = 4;
    REPEAT = 5;
    LOOP = 6;
    OPERATION = 7;
    VARIABLE = 8;
    RETURN = 9;
    LIST = 10;
    At* at = newAt_0();
    org_bau_String_string result = At_run_2(at, string_1050);
    printf("%.*s\n", _arrayLen(result.data), result.data->data);
    org_bau_String_string_free(&result);
    _decUseStack(at, At);
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

fun hashCode(data i8[]) int
4 bytes at a time if possible

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
