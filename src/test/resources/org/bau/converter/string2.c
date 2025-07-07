#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <stddef.h>
#include <stdint.h>
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
typedef struct org_bau_String_string org_bau_String_string;
struct org_bau_String_string;
typedef struct org_bau_String_string_array org_bau_String_string_array;
struct org_bau_String_string_array;
typedef struct org_bau_String_StringBuilder org_bau_String_StringBuilder;
struct org_bau_String_StringBuilder;
typedef struct org_bau_List_List_org_bau_String_string org_bau_List_List_org_bau_String_string;
struct org_bau_List_List_org_bau_String_string;
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
    org_bau_String_string* data;
    int32_t _refCount;
};
org_bau_String_string_array* org_bau_String_string_array_new(uint32_t len) {
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
    i8_array* data;
    int64_t len;
    int32_t _refCount;
};
org_bau_String_StringBuilder* org_bau_String_StringBuilder_new() {
    org_bau_String_StringBuilder* result = _malloc(sizeof(org_bau_String_StringBuilder));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_List_List_org_bau_String_string {
    org_bau_String_string_array* array;
    int64_t size;
    int32_t _refCount;
};
org_bau_List_List_org_bau_String_string* org_bau_List_List_org_bau_String_string_new() {
    org_bau_List_List_org_bau_String_string* result = _malloc(sizeof(org_bau_List_List_org_bau_String_string));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
int64_t idx_2(int64_t x, int64_t len);
org_bau_List_List_org_bau_String_string* org_bau_List_List_org_bau_String_string_1(org_bau_String_string_array* array);
org_bau_List_List_org_bau_String_string* org_bau_List_newList_org_bau_String_string_1(int64_t _T);
void org_bau_List_List_org_bau_String_string_add_2(org_bau_List_List_org_bau_String_string* this, org_bau_String_string x);
org_bau_String_string org_bau_List_List_org_bau_String_string_get_2(org_bau_List_List_org_bau_String_string* this, int64_t x);
org_bau_String_StringBuilder* org_bau_String_StringBuilder_1(i8_array* data);
int64_t org_bau_String_indexOf_2(i8_array* s, i8_array* find);
int64_t org_bau_String_indexOf_3(i8_array* s, i8_array* find, int64_t start);
i8_array* org_bau_String_replaceAll_3(i8_array* s, i8_array* before, i8_array* after);
org_bau_List_List_org_bau_String_string* org_bau_String_split_2(i8_array* s, i8_array* delimiter);
org_bau_String_string org_bau_String_str_1(i8_array* s);
org_bau_String_string org_bau_String_string_1(i8_array* data);
i8_array* org_bau_String_substring_2(i8_array* s, int64_t start);
i8_array* org_bau_String_substring_3(i8_array* s, int64_t start, int64_t end);
void org_bau_String_StringBuilder_append_2(org_bau_String_StringBuilder* this, i8_array* b);
void org_bau_String_StringBuilder_append_4(org_bau_String_StringBuilder* this, i8_array* b, int64_t start, int64_t end);
void test_0();
void i8_array_free(i8_array* x);
void org_bau_String_string_free(org_bau_String_string* x);
void org_bau_String_string_copy(org_bau_String_string* x);
void org_bau_String_string_array_free(org_bau_String_string_array* x);
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x);
void org_bau_List_List_org_bau_String_string_free(org_bau_List_List_org_bau_String_string* x);
void i8_array_free(i8_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void org_bau_String_string_free(org_bau_String_string* x) {
    _decUse(x->data, i8_array);
}
void org_bau_String_string_copy(org_bau_String_string* x) {
    _incUse(x->data);
}
void org_bau_String_string_array_free(org_bau_String_string_array* x) {
    for (int i = 0; i < x->len; i++) org_bau_String_string_free(&(x->data[i]));
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x) {
    _decUse(x->data, i8_array);
    _free(x); _traceFree(x);
}
void org_bau_List_List_org_bau_String_string_free(org_bau_List_List_org_bau_String_string* x) {
    _decUse(x->array, org_bau_String_string_array);
    _free(x); _traceFree(x);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = (int8_t*) data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1003;
i8_array* string_1004;
i8_array* string_1005;
i8_array* string_1006;
i8_array* string_1007;
i8_array* string_1008;
i8_array* string_1009;
i8_array* string_1010;
i8_array* string_1011;
i8_array* string_1012;
i8_array* string_1013;
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
org_bau_List_List_org_bau_String_string* org_bau_List_List_org_bau_String_string_1(org_bau_String_string_array* array) {
    org_bau_List_List_org_bau_String_string* _t3 = org_bau_List_List_org_bau_String_string_new();
    _incUseStack(array);
    _t3->array = array;
    _t3->size = 0;
    return _t3;
}
org_bau_List_List_org_bau_String_string* org_bau_List_newList_org_bau_String_string_1(int64_t _T) {
    org_bau_String_string_array* _t0 = org_bau_String_string_array_new(4);
    org_bau_List_List_org_bau_String_string* _t1 = org_bau_List_List_org_bau_String_string_1(_t0);
    _decUseStack(_t0, org_bau_String_string_array);
    return _t1;
}
void org_bau_List_List_org_bau_String_string_add_2(org_bau_List_List_org_bau_String_string* this, org_bau_String_string x) {
    if (this->size >= this->array->len) {
        org_bau_String_string_array* _t0 = org_bau_String_string_array_new(this->array->len * 2);
        _incUseStack(_t0);
        org_bau_String_string_array* n = _t0;
        if (this->array->len > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    org_bau_String_string_copy(&this->array->data[i]);
                    n->data[idx_2(i, n->len)] = this->array->data[i];
                    int64_t _next = i + 1;
                    if (_next >= this->array->len) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        _incUseStack(n);
        _decUse(this->array, org_bau_String_string_array);
        this->array = n;
        _decUseStack(n, org_bau_String_string_array);
        _decUseStack(_t0, org_bau_String_string_array);
    }
    org_bau_String_string_copy(&x);
    this->array->data[idx_2(this->size, this->array->len)] = x;
    this->size += 1;
}
org_bau_String_string org_bau_List_List_org_bau_String_string_get_2(org_bau_List_List_org_bau_String_string* this, int64_t x) {
    org_bau_String_string_copy(&this->array->data[idx_2(x, this->array->len)]);
    org_bau_String_string _r0 = this->array->data[idx_2(x, this->array->len)];
    return _r0;
}
org_bau_String_StringBuilder* org_bau_String_StringBuilder_1(i8_array* data) {
    org_bau_String_StringBuilder* _t1 = org_bau_String_StringBuilder_new();
    _incUseStack(data);
    _t1->data = data;
    _t1->len = 0;
    return _t1;
}
int64_t org_bau_String_indexOf_2(i8_array* s, i8_array* find) {
    int64_t _t0 = org_bau_String_indexOf_3(s, find, 0);
    return _t0;
}
int64_t org_bau_String_indexOf_3(i8_array* s, i8_array* find, int64_t start) {
    if (find->len <= 0) {
        return -1;
    }
    if (s->len <= 0) {
        return -1;
    }
    int64_t i = 0;
    if (start < 0) {
        return -1;
    }
    if (start >= s->len) {
        return -1;
    }
    i = start;
    while (1) {
        int64_t j = 0;
        int64_t x = i;
        while (1) {
            if (s->data[x] != find->data[j]) {
                break;
            }
            int64_t nj = j + 1;
            if (nj >= find->len) {
                return i;
            }
            j = nj;
            int64_t nx = x + 1;
            if (nx >= s->len) {
                break;
            }
            x = nx;
        }
        int64_t ni = i + 1;
        if (ni >= s->len) {
            break;
        }
        i = ni;
    }
    return -1;
}
i8_array* org_bau_String_replaceAll_3(i8_array* s, i8_array* before, i8_array* after) {
    _incUseStack(s);
    _incUseStack(before);
    _incUseStack(after);
    int64_t next = org_bau_String_indexOf_2(s, before);
    int64_t _t0 = before->len <= 0;
    if (!(_t0)) {
        int64_t _t1 = next < 0;
        _t0 = _t1;
    }
    if (_t0) {
        _decUseStack(after, i8_array);
        _decUseStack(before, i8_array);
        return s;
    }
    i8_array* _t2 = i8_array_new(s->len);
    org_bau_String_StringBuilder* buff = org_bau_String_StringBuilder_1(_t2);
    int64_t index = 0;
    while (1) {
        org_bau_String_StringBuilder_append_4(buff, s, index, next);
        org_bau_String_StringBuilder_append_2(buff, after);
        index = next + before->len;
        int64_t _t3 = org_bau_String_indexOf_3(s, before, index);
        next = _t3;
        if (next < 0) {
            break;
        }
    }
    org_bau_String_StringBuilder_append_4(buff, s, index, s->len);
    _incUseStack(buff->data);
    i8_array* _r0 = buff->data;
    _decUseStack(buff, org_bau_String_StringBuilder);
    _decUseStack(_t2, i8_array);
    _decUseStack(after, i8_array);
    _decUseStack(before, i8_array);
    _decUseStack(s, i8_array);
    return _r0;
}
org_bau_List_List_org_bau_String_string* org_bau_String_split_2(i8_array* s, i8_array* delimiter) {
    org_bau_List_List_org_bau_String_string* list = org_bau_List_newList_org_bau_String_string_1(0);
    int64_t next = org_bau_String_indexOf_2(s, delimiter);
    int64_t _t0 = delimiter->len == 0;
    if (!(_t0)) {
        int64_t _t1 = next < 0;
        _t0 = _t1;
    }
    if (_t0) {
        org_bau_String_string _t2 = org_bau_String_str_1(s);
        org_bau_List_List_org_bau_String_string_add_2(list, _t2);
        org_bau_String_string_free(&_t2);
        return list;
    }
    int64_t index = 0;
    while (1) {
        i8_array* p = org_bau_String_substring_3(s, index, next);
        org_bau_String_string _t3 = org_bau_String_str_1(p);
        org_bau_List_List_org_bau_String_string_add_2(list, _t3);
        index = next + delimiter->len;
        int64_t _t4 = org_bau_String_indexOf_3(s, delimiter, index);
        next = _t4;
        if (next < 0) {
            org_bau_String_string_free(&_t3);
            _decUseStack(p, i8_array);
            break;
        }
        org_bau_String_string_free(&_t3);
        _decUseStack(p, i8_array);
    }
    i8_array* p = org_bau_String_substring_2(s, index);
    org_bau_String_string _t5 = org_bau_String_str_1(p);
    org_bau_List_List_org_bau_String_string_add_2(list, _t5);
    org_bau_String_string_free(&_t5);
    _decUseStack(p, i8_array);
    return list;
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
i8_array* org_bau_String_substring_2(i8_array* s, int64_t start) {
    _incUseStack(s);
    i8_array* _t0 = org_bau_String_substring_3(s, start, s->len);
    _decUseStack(s, i8_array);
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
        int64_t _t3 = end > s->len;
        _t2 = _t3;
    }
    if (_t2) {
        _decUseStack(s, i8_array);
        return string_1000;
    }
    if (s->len <= 0) {
        _decUseStack(s, i8_array);
        return string_1000;
    }
    int64_t i = 0;
    if (start >= s->len) {
        _decUseStack(s, i8_array);
        return string_1000;
    }
    i = start;
    i8_array* _t4 = i8_array_new(len);
    _incUseStack(_t4);
    i8_array* result = _t4;
    if (len > 0) {
        while (1 == 1) {
            int64_t j = 0;
            while (1) {
                result->data[idx_2(j, result->len)] = s->data[i];
                int64_t next = i + 1;
                if (next >= s->len) {
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
    org_bau_String_StringBuilder_append_4(this, b, 0, b->len);
}
void org_bau_String_StringBuilder_append_4(org_bau_String_StringBuilder* this, i8_array* b, int64_t start, int64_t end) {
    int64_t add = end - start;
    int64_t _t0 = start >= b->len;
    if (!(_t0)) {
        int64_t _t1 = end < start;
        _t0 = _t1;
    }
    int64_t _t2 = _t0;
    if (!(_t2)) {
        int64_t _t3 = end > b->len;
        _t2 = _t3;
    }
    if (_t2) {
        return;
    }
    while (( this->len + add ) > this->data->len) {
        i8_array* _t4 = i8_array_new(this->data->len * 2);
        _incUseStack(_t4);
        i8_array* n = _t4;
        if (this->data->len > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    n->data[idx_2(i, n->len)] = this->data->data[i];
                    int64_t _next = i + 1;
                    if (_next >= this->data->len) {
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
                this->data->data[idx_2(this->len + i, this->data->len)] = b->data[idx_2(start + i, b->len)];
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
void test_0() {
    _incUseStack(string_1001);
    i8_array* x = string_1001;
    int64_t _t0 = org_bau_String_indexOf_2(x, string_1003);
    printf("indexOf ll: %lld\n", (long long)_t0);
    i8_array* _t1 = org_bau_String_replaceAll_3(string_1001, string_1005, string_1006);
    printf("l ->L : %.*s\n", _t1->len, _t1->data);
    i8_array* _t2 = org_bau_String_replaceAll_3(string_1001, string_1005, string_1008);
    printf("l ->LL: %.*s\n", _t2->len, _t2->data);
    i8_array* _t3 = org_bau_String_replaceAll_3(string_1001, string_1003, string_1006);
    printf("ll->L : %.*s\n", _t3->len, _t3->data);
    org_bau_List_List_org_bau_String_string* list = org_bau_String_split_2(string_1010, string_1011);
    if (list->size > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                org_bau_String_string s = org_bau_List_List_org_bau_String_string_get_2(list, i);
                printf("#%lld: %.*s\n", (long long)i, s.data->len, s.data->data);
                int64_t _next = i + 1;
                if (_next >= list->size) {
                    org_bau_String_string_free(&s);
                    break;
                }
                i = _next;
                org_bau_String_string_free(&s);
            }
            break;
        }
    }
    _decUseStack(list, org_bau_List_List_org_bau_String_string);
    _decUseStack(_t3, i8_array);
    _decUseStack(_t2, i8_array);
    _decUseStack(_t1, i8_array);
    _decUseStack(x, i8_array);
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("", 0);
    string_1001 = str_const("hello world", 11);
    string_1002 = str_const("indexOf ll: ", 12);
    string_1003 = str_const("ll", 2);
    string_1004 = str_const("l ->L : ", 8);
    string_1005 = str_const("l", 1);
    string_1006 = str_const("L", 1);
    string_1007 = str_const("l ->LL: ", 8);
    string_1008 = str_const("LL", 2);
    string_1009 = str_const("ll->L : ", 8);
    string_1010 = str_const("hello,world,!", 13);
    string_1011 = str_const(",", 1);
    string_1012 = str_const("#", 1);
    string_1013 = str_const(": ", 2);
    _main();
    return 0;
}
void _main() {
    test_0();
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

*/
