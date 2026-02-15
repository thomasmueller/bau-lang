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
typedef struct int_array int_array;
struct int_array;
typedef struct org_bau_HashMap_HashMap_int_int org_bau_HashMap_HashMap_int_int;
struct org_bau_HashMap_HashMap_int_int;
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
struct org_bau_HashMap_HashMap_int_int {
    int32_t _refCount;
    int64_t size;
    int_array* keys;
    int_array* values;
    int_array* hashes;
};
org_bau_HashMap_HashMap_int_int* org_bau_HashMap_HashMap_int_int_new() {
    org_bau_HashMap_HashMap_int_int* result = _malloc(sizeof(org_bau_HashMap_HashMap_int_int));
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
int64_t int_equals_2(int64_t this, int64_t o);
org_bau_HashMap_HashMap_int_int* org_bau_HashMap_HashMap_int_int_3(int_array* keys, int_array* values, int_array* hashes);
int64_t org_bau_HashMap_mix_1(int64_t z);
org_bau_HashMap_HashMap_int_int* org_bau_HashMap_newHashMap_int_int_2(int64_t _K, int64_t _V);
int64_t org_bau_HashMap_HashMap_int_int_get_3(org_bau_HashMap_HashMap_int_int* this, int64_t hash, int64_t key);
void org_bau_HashMap_HashMap_int_int_put_4(org_bau_HashMap_HashMap_int_int* this, int64_t hash, int64_t key, int64_t value);
int64_t shiftRight_int_2(int64_t a, int64_t b);
void int_array_free(int_array* x);
void org_bau_HashMap_HashMap_int_int_free(org_bau_HashMap_HashMap_int_int* x);
void int_array_free(int_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void org_bau_HashMap_HashMap_int_int_free(org_bau_HashMap_HashMap_int_int* x) {
    _decUse(x->keys, int_array);
    _decUse(x->values, int_array);
    _decUse(x->hashes, int_array);
    _free(x); _traceFree(x);
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
int64_t int_equals_2(int64_t this, int64_t o) {
    int64_t _r0 = this == o;
    return _r0;
}
org_bau_HashMap_HashMap_int_int* org_bau_HashMap_HashMap_int_int_3(int_array* keys, int_array* values, int_array* hashes) {
    org_bau_HashMap_HashMap_int_int* _t0 = org_bau_HashMap_HashMap_int_int_new();
    _t0->size = 0;
    _incUseStack(keys);
    _t0->keys = keys;
    _incUseStack(values);
    _t0->values = values;
    _incUseStack(hashes);
    _t0->hashes = hashes;
    return _t0;
}
int64_t org_bau_HashMap_mix_1(int64_t z) {
    z = (z ^ (shiftRight_int_2(z, 30))) * -4658895280553007687;
    z = (z ^ (shiftRight_int_2(z, 27))) * -7723592293110705685;
    int64_t _r0 = z ^ (shiftRight_int_2(z, 31));
    return _r0;
}
org_bau_HashMap_HashMap_int_int* org_bau_HashMap_newHashMap_int_int_2(int64_t _K, int64_t _V) {
    int_array* _t0 = int_array_new(4);
    int_array* _t1 = int_array_new(4);
    int_array* _t2 = int_array_new(4);
    org_bau_HashMap_HashMap_int_int* _t3 = org_bau_HashMap_HashMap_int_int_3(_t0, _t1, _t2);
    _decUseStack(_t2, int_array);
    _decUseStack(_t1, int_array);
    _decUseStack(_t0, int_array);
    return _t3;
}
int64_t org_bau_HashMap_HashMap_int_int_get_3(org_bau_HashMap_HashMap_int_int* this, int64_t hash, int64_t key) {
    int64_t p = hash & (_arrayLen(this->keys) - 1);
    while (1) {
        if (this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == 0) {
            int64_t _r0 = this->values->data[idx_2(p, _arrayLen(this->values))];
            return _r0;
        } else {
            int64_t _t0 = this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == hash;
            if (_t0) {
                int64_t _t1 = int_equals_2(key, this->keys->data[idx_2(p, _arrayLen(this->keys))]);
                _t0 = _t1;
            }
            if (_t0) {
                int64_t _r1 = this->values->data[idx_2(p, _arrayLen(this->values))];
                return _r1;
            }
        }
        p = (p + 1) & (_arrayLen(this->keys) - 1);
    }
}
void org_bau_HashMap_HashMap_int_int_put_4(org_bau_HashMap_HashMap_int_int* this, int64_t hash, int64_t key, int64_t value) {
    if (( this->size * 2 ) >= _arrayLen(this->keys)) {
        int_array* _t0 = int_array_new(_arrayLen(this->keys) * 2);
        _incUseStack(_t0);
        int_array* kn = _t0;
        int_array* _t1 = int_array_new(_arrayLen(this->keys) * 2);
        _incUseStack(_t1);
        int_array* vn = _t1;
        int_array* _t2 = int_array_new(_arrayLen(this->keys) * 2);
        _incUseStack(_t2);
        int_array* hn = _t2;
        _incUseStack(this->keys);
        int_array* ok = this->keys;
        _incUseStack(this->hashes);
        int_array* oh = this->hashes;
        _incUseStack(this->values);
        int_array* ov = this->values;
        _incUseStack(kn);
        _decUse(this->keys, int_array);
        this->keys = kn;
        _incUseStack(vn);
        _decUse(this->values, int_array);
        this->values = vn;
        _incUseStack(hn);
        _decUse(this->hashes, int_array);
        this->hashes = hn;
        if (_arrayLen(ok) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    if (oh->data[idx_2(i, _arrayLen(oh))] != 0) {
                        org_bau_HashMap_HashMap_int_int_put_4(this, oh->data[idx_2(i, _arrayLen(oh))], ok->data[i], ov->data[idx_2(i, _arrayLen(ov))]);
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
        _decUseStack(ov, int_array);
        _decUseStack(oh, int_array);
        _decUseStack(ok, int_array);
        _decUseStack(hn, int_array);
        _decUseStack(_t2, int_array);
        _decUseStack(vn, int_array);
        _decUseStack(_t1, int_array);
        _decUseStack(kn, int_array);
        _decUseStack(_t0, int_array);
    }
    int64_t p = hash & (_arrayLen(this->keys) - 1);
    while (1) {
        int64_t _t3 = this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == hash;
        if (_t3) {
            int64_t _t4 = int_equals_2(key, this->keys->data[idx_2(p, _arrayLen(this->keys))]);
            _t3 = _t4;
        }
        if (_t3) {
            this->values->data[idx_2(p, _arrayLen(this->values))] = value;
            return;
        } else {
            if (this->hashes->data[idx_2(p, _arrayLen(this->hashes))] == 0) {
                this->size += 1;
                this->keys->data[idx_2(p, _arrayLen(this->keys))] = key;
                this->hashes->data[idx_2(p, _arrayLen(this->hashes))] = hash;
                this->values->data[idx_2(p, _arrayLen(this->values))] = value;
                return;
            }
        }
        p = (p + 1) & (_arrayLen(this->keys) - 1);
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
    _main();
    return 0;
}
void _main() {
    org_bau_HashMap_HashMap_int_int* map = org_bau_HashMap_newHashMap_int_int_2(0, 0);
    if (5 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                int64_t _t2 = org_bau_HashMap_mix_1(i);
                org_bau_HashMap_HashMap_int_int_put_4(map, _t2, i, i * 10);
                int64_t _next = i + 1;
                if (_next >= 5) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    if (5 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                int64_t _t3 = org_bau_HashMap_mix_1(i);
                int64_t _t4 = org_bau_HashMap_HashMap_int_int_get_3(map, _t3, i);
                printf("%lld\n", (long long)_t4);
                int64_t _next = i + 1;
                if (_next >= 5) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    _decUseStack(map, org_bau_HashMap_HashMap_int_int);
    _end();
}
/*

fun hashCode(data i8[]) int
4 bytes at a time if possible

*/
