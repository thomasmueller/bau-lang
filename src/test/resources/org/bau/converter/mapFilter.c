#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <limits.h>
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
typedef struct i8_array i8_array;
struct i8_array;
typedef struct int_array int_array;
struct int_array;
typedef struct List_int List_int;
struct List_int;
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
struct List_int {
    int32_t _refCount;
    int_array* array;
    int64_t size;
};
List_int* List_int_new() {
    List_int* result = _malloc(sizeof(List_int));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
List_int* List_int_0();
void List_int_add_2(List_int* this, int64_t x);
int64_t List_int_get_2(List_int* this, int64_t x);
int64_t idx_2(int64_t x, int64_t len);
int64_t imod_2(int64_t a, int64_t b);
int_array* intRange_2(int64_t start, int64_t end);
List_int* newList_int_1(int64_t _T);
List_int* rangeList_2(int64_t start, int64_t end);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void List_int_free(List_int* x);
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
void List_int_free_0(List_int* x) {
    _decUse(x->array, int_array);
    _free(x); _traceFree(x);
}
void List_int_free(List_int* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))List_int_free_0);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = _malloc(sizeof(char) * len);
    memcpy(result->data, data, sizeof(char) * len);
    return result;
}
i8_array* string_1014;
List_int* List_int_0() {
    List_int* _t0 = List_int_new();
    _t0->array = int_array_new(0);
    _t0->size = 0;
    return _t0;
}
void List_int_add_2(List_int* this, int64_t x) {
    if (this->size >= _arrayLen(this->array)) {
        int_array* _t2 = int_array_new(_arrayLen(this->array) * 2);
        _incUseStack(_t2);
        int_array* n = _t2;
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
        _decUseStack(_t2, int_array);
    }
    this->array->data[idx_2(this->size, _arrayLen(this->array))] = x;
    this->size += 1;
}
int64_t List_int_get_2(List_int* this, int64_t x) {
    int64_t _r0 = this->array->data[idx_2(x, _arrayLen(this->array))];
    return _r0;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
int64_t imod_2(int64_t a, int64_t b) {
    if (b != 0) return a % b;
    return 0;
}
int_array* intRange_2(int64_t start, int64_t end) {
    int_array* _t0 = int_array_new(end - start);
    _incUseStack(_t0);
    int_array* list = _t0;
    if (( end - start ) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                list->data[i] = i + start;
                if (( i + 1 ) >= ( end - start )) {
                    break;
                }
                i = i + 1;
            }
            break;
        }
    }
    _decUseStack(_t0, int_array);
    return list;
}
List_int* newList_int_1(int64_t _T) {
    List_int* result = List_int_0();
    int_array* _t0 = int_array_new(4);
    _incUseStack(_t0);
    _decUse(result->array, int_array);
    result->array = _t0;
    _decUseStack(_t0, int_array);
    return result;
}
List_int* rangeList_2(int64_t start, int64_t end) {
    List_int* list = newList_int_1(0);
    if (( end - start ) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                List_int_add_2(list, i + start);
                if (( i + 1 ) >= ( end - start )) {
                    break;
                }
                i = i + 1;
            }
            break;
        }
    }
    return list;
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1014 = str_const(": ", 2);
    _main();
    return 0;
}
void _main() {
    List_int* _t1 = NULL;
    if (1) {
        List_int* _t0 = NULL;
        if (1) {
            List_int* this = rangeList_2(0, 10);
            List_int* _u_result = newList_int_1(0);
            int64_t _u_i = 0;
            while (_u_i < this->size) {
                int64_t it = List_int_get_2(this, _u_i);
                if (( imod_2(it, 2) ) == 0) {
                    List_int_add_2(_u_result, it);
                }
                _u_i = _u_i + 1;
            }
            _incUseStack(_u_result);
            _decUseStack(_t0, List_int);
            _t0 = _u_result;
        } else {
        }
        _incUseStack(_t0);
        List_int* this = _t0;
        List_int* _u_result = newList_int_1(0);
        int64_t _u_i = 0;
        while (_u_i < this->size) {
            int64_t it = List_int_get_2(this, _u_i);
            List_int_add_2(_u_result, it * it);
            _u_i = _u_i + 1;
        }
        _incUseStack(_u_result);
        _decUseStack(_t1, List_int);
        _t1 = _u_result;
    } else {
    }
    _incUseStack(_t1);
    List_int* list = _t1;
    if (list->size > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                int64_t _t2 = List_int_get_2(list, i);
                printf("%lld: %lld\n", (long long)i, (long long)_t2);
                if (( i + 1 ) >= list->size) {
                    break;
                }
                i = i + 1;
            }
            break;
        }
    }
    int_array* _t4 = NULL;
    if (1) {
        int_array* _t3 = NULL;
        if (1) {
            int_array* this = intRange_2(0, 10);
            int_array* _0t0 = int_array_new(_arrayLen(this));
            _incUseStack(_0t0);
            int_array* _u_temp = _0t0;
            int64_t _u_j = 0;
            int64_t _u_i = 0;
            while (_u_i < _arrayLen(this)) {
                int64_t it = this->data[idx_2(_u_i, _arrayLen(this))];
                if (( imod_2(it, 2) ) == 0) {
                    _u_temp->data[idx_2(_u_j, _arrayLen(_u_temp))] = it;
                    _u_j = _u_j + 1;
                }
                _u_i = _u_i + 1;
            }
            int_array* _0t1 = int_array_new(_u_j);
            _incUseStack(_0t1);
            int_array* _u_result = _0t1;
            _u_i = 0;
            while (_u_i < _u_j) {
                _u_result->data[idx_2(_u_i, _arrayLen(_u_result))] = _u_temp->data[idx_2(_u_i, _arrayLen(_u_temp))];
                _u_i = _u_i + 1;
            }
            _incUseStack(_u_result);
            _decUseStack(_t3, int_array);
            _t3 = _u_result;
        } else {
        }
        _incUseStack(_t3);
        int_array* this = _t3;
        int_array* _0t0 = int_array_new(_arrayLen(this));
        _incUseStack(_0t0);
        int_array* _u_result = _0t0;
        int64_t _u_i = 0;
        while (_u_i < _arrayLen(this)) {
            int64_t it = this->data[idx_2(_u_i, _arrayLen(this))];
            _u_result->data[idx_2(_u_i, _arrayLen(_u_result))] = it * it;
            _u_i = _u_i + 1;
        }
        _incUseStack(_u_result);
        _decUseStack(_t4, int_array);
        _t4 = _u_result;
    } else {
    }
    _incUseStack(_t4);
    int_array* array = _t4;
    if (_arrayLen(array) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                printf("%lld: %lld\n", (long long)i, (long long)array->data[i]);
                if (( i + 1 ) >= _arrayLen(array)) {
                    break;
                }
                i = i + 1;
            }
            break;
        }
    }
    _decUseStack(array, int_array);
    _decUseStack(_t4, int_array);
    _decUseStack(list, List_int);
    _decUseStack(_t1, List_int);
    _end();
}
/*

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

*/
