#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <stddef.h>
#include <stdint.h>
#include <string.h>
#include <sys/stat.h>
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
typedef struct org_bau_File_File org_bau_File_File;
struct org_bau_File_File;
typedef struct Sr3 Sr3;
struct Sr3;
struct i8_array {
    int32_t len;
    int32_t _refCount;
    int8_t* data;
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
struct int_array {
    int32_t len;
    int32_t _refCount;
    int64_t* data;
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
struct org_bau_File_File {
    int32_t _refCount;
    int64_t filePointer;
};
org_bau_File_File* org_bau_File_File_new() {
    org_bau_File_File* result = _malloc(sizeof(org_bau_File_File));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct Sr3 {
    int32_t _refCount;
    i8_array* input;
    i8_array* output;
};
Sr3* Sr3_new() {
    Sr3* result = _malloc(sizeof(Sr3));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
Sr3* Sr3_2(i8_array* input, i8_array* output);
void compress_0();
void decompress_0();
void flush_0();
int64_t idx_2(int64_t x, int64_t len);
void initRead_0();
i8_array* org_bau_Env_arg_1(int64_t index);
int64_t org_bau_Env_argCount_0();
org_bau_File_File* org_bau_File_File_0();
org_bau_File_File* org_bau_File_openFile_2(i8_array* name, i8_array* mode);
void org_bau_File_File_close_1(org_bau_File_File* this);
int64_t org_bau_File_File_len_1(org_bau_File_File* this);
int64_t org_bau_File_File_read_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len);
int64_t org_bau_File_File_write_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len);
int64_t probability_1(int64_t cxt);
int64_t readBit_1(int64_t cxt);
int64_t readByte_1(int64_t cxt);
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
void update_2(int64_t cxt, int64_t bit);
void writeBit_2(int64_t cxt, int64_t bit);
void writeByte_2(int64_t cxt, int64_t x);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_File_File_free(org_bau_File_File* x);
void Sr3_free(Sr3* x);
void i8_array_free(i8_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void int_array_free(int_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void org_bau_File_File_free(org_bau_File_File* x) {
    org_bau_File_File_close_1(x);
    if (x->_refCount) { fprintf(stdout, "Object re-referenced in the close method"); exit(1); }
    _free(x); _traceFree(x);
}
void Sr3_free(Sr3* x) {
    _decUse(x->input, i8_array);
    _decUse(x->output, i8_array);
    _free(x); _traceFree(x);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = (int8_t*) data;
    return result;
}
i8_array* string_1002;
i8_array* string_1003;
i8_array* string_1004;
i8_array* string_1008;
i8_array* string_1009;
i8_array* string_1010;
int_array* int_array_const(int64_t* data, uint32_t len) {
    int_array* result = _malloc(sizeof(int_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = (int64_t*) data;
    return result;
}
int_array* array_1000;
int_array* DT;
int64_t di1;
int64_t h;
int_array* state;
int64_t x1;
int64_t x2;
int64_t x;
int64_t inPos;
int64_t outPos;
Sr3* global;
Sr3* Sr3_2(i8_array* input, i8_array* output) {
    Sr3* _t2 = Sr3_new();
    _incUseStack(input);
    _t2->input = input;
    _incUseStack(output);
    _t2->output = output;
    return _t2;
}
void compress_0() {
    int64_t c1 = 0;
    int_array* _t0 = int_array_new(0x1000000);
    _incUseStack(_t0);
    int_array* t4 = _t0;
    while (1) {
        int64_t r = t4->data[h];
        int64_t cxt = 0;
        if (( r & 4294967295 ) >= 67108864) {
            cxt = 1024 + (shiftRight_int_2(r, 20));
        } else {
            cxt = c1 | ((shiftRight_int_2(r, 16)) & 16128);
        }
        cxt *= 258;
        if (inPos >= _arrayLen(global->input)) {
            writeBit_2(cxt, 1);
            writeBit_2(cxt + 1, 0);
            writeByte_2(cxt + 2, r & 255);
            break;
        }
        int64_t c = global->input->data[idx_2(inPos, _arrayLen(global->input))] & 255;
        inPos += 1;
        int64_t comp3 = (c * 65793) ^ r;
        if (( comp3 & 255 ) == 0) {
            writeBit_2(cxt, 0);
            if ((r & 4294967295) < 1056964608) {
                t4->data[h] += 0x1000000;
            }
        } else if (( comp3 & 65280 ) == 0) {
            writeBit_2(cxt, 1);
            writeBit_2(cxt + 1, 1);
            writeBit_2(cxt + 2, 0);
            t4->data[h] = (r & 16711680) | ((shiftLeft_2(r, 8)) & 65280) | c | 16777216;
        } else if (( comp3 & 16711680 ) == 0) {
            writeBit_2(cxt, 1);
            writeBit_2(cxt + 1, 1);
            writeBit_2(cxt + 2, 1);
            t4->data[h] = ((shiftLeft_2(r, 8)) & 16776960) | c | 16777216;
        } else {
            writeBit_2(cxt, 1);
            writeBit_2(cxt + 1, 0);
            writeByte_2(cxt + 2, c);
            t4->data[h] = ((shiftLeft_2(r, 8)) & 16776960) | c;
        }
        c1 = c;
        h = (( h * (shiftLeft_2(5, di1)) ) + c + 1) & 16777215;
    }
    flush_0();
    _decUseStack(t4, int_array);
    _decUseStack(_t0, int_array);
}
void decompress_0() {
    initRead_0();
    int64_t c1 = 0;
    int_array* _t0 = int_array_new(0x1000000);
    _incUseStack(_t0);
    int_array* t4 = _t0;
    while (1) {
        int64_t r = t4->data[h];
        int64_t cxt = 0;
        if (( r & 4294967295 ) >= 67108864) {
            cxt = 1024 + (shiftRight_int_2(r, 20));
        } else {
            cxt = c1 | ((shiftRight_int_2(r, 16)) & 16128);
        }
        cxt *= 258;
        int64_t _t1 = readBit_1(cxt);
        if (_t1 == 1) {
            int64_t _t2 = readBit_1(cxt + 1);
            if (_t2 == 1) {
                int64_t _t3 = readBit_1(cxt + 2);
                if (_t3 == 1) {
                    c1 = (shiftRight_int_2(r, 16)) & 255;
                    t4->data[h] = ((shiftLeft_2(r, 8)) & 16776960) | c1 | 16777216;
                } else {
                    c1 = (shiftRight_int_2(r, 8)) & 255;
                    t4->data[h] = (r & 16711680) | ((shiftLeft_2(r, 8)) & 65280) | c1 | 16777216;
                }
            } else {
                int64_t _t4 = readByte_1(cxt + 2);
                c1 = _t4;
                if (c1 == (r & 255)) {
                    break;
                }
                t4->data[h] = ((shiftLeft_2(r, 8)) & 16776960) | c1;
            }
        } else {
            c1 = r & 255;
            if ((r & 4294967295) < 1056964608) {
                t4->data[h] = r + 16777216;
            }
        }
        global->output->data[idx_2(outPos, _arrayLen(global->output))] = c1;
        outPos += 1;
        h = (( h * (shiftLeft_2(5, di1)) ) + c1 + 1) & 16777215;
    }
    _decUseStack(t4, int_array);
    _decUseStack(_t0, int_array);
}
void flush_0() {
    global->output->data[idx_2(outPos, _arrayLen(global->output))] = shiftRight_int_2(x1, 24);
    global->output->data[idx_2(outPos + 1, _arrayLen(global->output))] = 0xff;
    global->output->data[idx_2(outPos + 2, _arrayLen(global->output))] = 0xff;
    global->output->data[idx_2(outPos + 3, _arrayLen(global->output))] = 0xff;
    global->output->data[idx_2(outPos + 4, _arrayLen(global->output))] = 0xff;
    outPos += 8;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
void initRead_0() {
    if (4 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                x = ((shiftLeft_2(x, 8)) + (global->input->data[idx_2(inPos, _arrayLen(global->input))] & 255)) & 4294967295;
                inPos += 1;
                int64_t _next = i + 1;
                if (_next >= 4) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
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
org_bau_File_File* org_bau_File_File_0() {
    org_bau_File_File* _t0 = org_bau_File_File_new();
    _t0->filePointer = 0;
    return _t0;
}
org_bau_File_File* org_bau_File_openFile_2(i8_array* name, i8_array* mode) {
    char n[256];
    for (int i = 0; i < 256; i++) {
        n[i] = 0;
        if (i < name->len) {
            n[i] = name->data[i];
        }
    }
    char m[256];
    for (int i = 0; i < 256; i++) {
        m[i] = 0;
        if (i < mode->len) {
            m[i] = mode->data[i];
        }
    }
    FILE* fp = fopen(n, m);
    if (!fp) {
        return 0;
    }
    org_bau_File_File* f = org_bau_File_File_new();
    f->filePointer = (uint64_t) fp;
    return f;
    org_bau_File_File* _t0 = org_bau_File_File_0();
    return _t0;
}
void org_bau_File_File_close_1(org_bau_File_File* this) {
    FILE* fp = (FILE*) (this->filePointer);
    fclose(fp);
}
int64_t org_bau_File_File_len_1(org_bau_File_File* this) {
    FILE* fp = (FILE*) (this->filePointer);
    struct stat st;
    if (fstat(fileno(fp), &st) == -1) {
        return -1;
    }
    return st.st_size;
    return -1;
}
int64_t org_bau_File_File_read_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len) {
    FILE* fp = (FILE*) (this->filePointer);
    if (pos < 0 || len < 0 || pos + len > data->len) {
        return 0;
    } else {
        return fread(data->data + pos, 1, len, fp);
    }
    return 0;
}
int64_t org_bau_File_File_write_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len) {
    FILE* fp = (FILE*) (this->filePointer);
    if (pos < 0 || len < 0 || pos + len > data->len) {
        return -1;
    } else {
        return fwrite(data->data + pos, 1, len, fp);
    }
    return 0;
}
int64_t probability_1(int64_t cxt) {
    int64_t _r0 = shiftRight_int_2(state->data[idx_2(cxt, _arrayLen(state))], 20);
    return _r0;
}
int64_t readBit_1(int64_t cxt) {
    int64_t p = probability_1(cxt);
    int64_t xmid = x1 + (shiftRight_int_2((x2 - x1), 12)) * p;
    int64_t bit = 0;
    if (x <= xmid) {
        bit = 1;
    }
    if (bit == 1) {
        x2 = xmid;
    } else {
        x1 = xmid + 1;
    }
    update_2(cxt, bit);
    while (((x1 ^ x2) & 4278190080) == 0) {
        x1 = (shiftLeft_2(x1, 8)) & 4294967295;
        x2 = ((shiftLeft_2(x2, 8)) + 255) & 4294967295;
        x = ((shiftLeft_2(x, 8)) + (global->input->data[idx_2(inPos, _arrayLen(global->input))] & 255)) & 4294967295;
        inPos += 1;
    }
    return bit;
}
int64_t readByte_1(int64_t cxt) {
    int64_t hi = 1;
    int64_t lo = 1;
    int64_t _t0 = readBit_1(cxt + hi);
    hi += hi + _t0;
    int64_t _t1 = readBit_1(cxt + hi);
    hi += hi + _t1;
    int64_t _t2 = readBit_1(cxt + hi);
    hi += hi + _t2;
    int64_t _t3 = readBit_1(cxt + hi);
    hi += hi + _t3;
    cxt += 15 * (hi - 15);
    int64_t _t4 = readBit_1(cxt + lo);
    lo += lo + _t4;
    int64_t _t5 = readBit_1(cxt + lo);
    lo += lo + _t5;
    int64_t _t6 = readBit_1(cxt + lo);
    lo += lo + _t6;
    int64_t _t7 = readBit_1(cxt + lo);
    lo += lo + _t7;
    int64_t _r0 = (shiftLeft_2((hi - 16), 4)) | (lo - 16);
    return _r0;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
void update_2(int64_t cxt, int64_t bit) {
    int64_t tc = state->data[idx_2(cxt, _arrayLen(state))];
    int64_t n = tc & 127;
    int64_t p = shiftRight_int_2(tc, 9);
    if (n < 127) {
        tc = (tc + 1) & 4294967295;
    }
    int64_t delta = ((shiftLeft_2(bit, 23)) - p) * DT->data[idx_2(n, _arrayLen(DT))];
    tc = (tc + (delta & 4294967168)) & 4294967295;
    state->data[idx_2(cxt, _arrayLen(state))] = tc;
}
void writeBit_2(int64_t cxt, int64_t bit) {
    int64_t p = probability_1(cxt);
    int64_t xmid = (x1 + (shiftRight_int_2(((x2 - x1) & 4294967295), 12)) * p) & 4294967295;
    if (bit == 1) {
        x2 = xmid;
    } else {
        x1 = (xmid + 1) & 4294967295;
    }
    update_2(cxt, bit);
    while (((x1 ^ x2) & 4278190080) == 0) {
        global->output->data[idx_2(outPos, _arrayLen(global->output))] = shiftRight_int_2(x2, 24);
        outPos += 1;
        x1 = (shiftLeft_2(x1, 8)) & 4294967295;
        x2 = ((shiftLeft_2(x2, 8)) + 255) & 4294967295;
    }
}
void writeByte_2(int64_t cxt, int64_t x) {
    int64_t b = (shiftRight_int_2(x, 4)) + 16;
    writeBit_2(cxt + 1, (shiftRight_int_2(b, 3)) & 1);
    writeBit_2(cxt + (shiftRight_int_2(b, 3)), (shiftRight_int_2(b, 2)) & 1);
    writeBit_2(cxt + (shiftRight_int_2(b, 2)), (shiftRight_int_2(b, 1)) & 1);
    writeBit_2(cxt + (shiftRight_int_2(b, 1)), b & 1);
    cxt += 15 * (b - 15);
    b = (x & 15) | 16;
    writeBit_2(cxt + 1, (shiftRight_int_2(b, 3)) & 1);
    writeBit_2(cxt + (shiftRight_int_2(b, 3)), (shiftRight_int_2(b, 2)) & 1);
    writeBit_2(cxt + (shiftRight_int_2(b, 2)), (shiftRight_int_2(b, 1)) & 1);
    writeBit_2(cxt + (shiftRight_int_2(b, 1)), b & 1);
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1002 = str_const("Parameters: c|d input output", 28);
    string_1003 = str_const("r", 1);
    string_1004 = str_const("Input does not exist", 20);
    string_1008 = str_const("Not an SR3 file", 15);
    string_1009 = str_const("w", 1);
    string_1010 = str_const("Could not open output", 21);
    int64_t array_const_1000[] = {256, 170, 128, 102, 85, 73, 64, 56, 51, 46, 42, 39, 36, 34, 32, 30, 28, 26, 25, 24, 23, 22, 21, 20, 19, 18, 18, 17, 17, 16, 16, 15, 15, 14, 14, 13, 13, 13, 12, 12, 12, 11, 11, 11, 11, 10, 10, 10, 10, 10, 9, 9, 9, 9, 9, 8, 8, 8, 8, 8, 8, 8, 8, 7, 7, 7, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3};
    array_1000 = int_array_const(array_const_1000, 128);
    _main();
    return 0;
}
void _main() {
    _incUseStack(array_1000);
    DT = array_1000;
    di1 = 5;
    h = 0;
    int_array* _t5 = int_array_new(528384);
    _incUseStack(_t5);
    state = _t5;
    x1 = 0;
    x2 = 0xffffffff;
    x = 0;
    inPos = 0;
    outPos = 0;
    i8_array* _t6 = i8_array_new(0);
    i8_array* _t7 = i8_array_new(0);
    global = Sr3_2(_t6, _t7);
    int64_t _t8 = org_bau_Env_argCount_0();
    if (_t8 != 4) {
        printf("Parameters: c|d input output\n");
        return;
    }
    i8_array* _t9 = org_bau_Env_arg_1(2);
    org_bau_File_File* in = org_bau_File_openFile_2(_t9, string_1003);
    if (!(in)) {
        printf("Input does not exist\n");
        return;
    }
    int64_t _t10 = org_bau_File_File_len_1(in);
    i8_array* _t11 = i8_array_new(_t10);
    _incUseStack(_t11);
    i8_array* input = _t11;
    int64_t _t12 = org_bau_File_File_read_4(in, input, 0, _arrayLen(input));
    ;
    if (528384 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                state->data[i] = 2147483648;
                int64_t _next = i + 1;
                if (_next >= 528384) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    org_bau_File_File_close_1(in);
    i8_array* _t13 = i8_array_new(_arrayLen(input) * 10);
    _incUseStack(_t13);
    i8_array* output = _t13;
    Sr3* _t14 = Sr3_2(input, output);
    _incUseStack(_t14);
    _decUseStack(global, Sr3);
    global = _t14;
    i8_array* _t15 = org_bau_Env_arg_1(1);
    if (_t15->data[idx_2(0, _arrayLen(_t15))] == 99) {
        output->data[idx_2(0, _arrayLen(output))] = 115;
        output->data[idx_2(1, _arrayLen(output))] = 82;
        output->data[idx_2(2, _arrayLen(output))] = 2;
        output->data[idx_2(3, _arrayLen(output))] = di1;
        outPos = 4;
        compress_0();
    } else {
        int64_t _t16 = input->data[idx_2(0, _arrayLen(input))] != 115;
        if (!(_t16)) {
            int64_t _t17 = input->data[idx_2(1, _arrayLen(input))] != 82;
            _t16 = _t17;
        }
        int64_t _t18 = _t16;
        if (!(_t18)) {
            int64_t _t19 = input->data[idx_2(2, _arrayLen(input))] != 2;
            _t18 = _t19;
        }
        if (_t18) {
            printf("Not an SR3 file\n");
            return;
        }
        di1 = input->data[idx_2(3, _arrayLen(input))];
        inPos = 4;
        decompress_0();
    }
    i8_array* _t20 = org_bau_Env_arg_1(3);
    org_bau_File_File* out = org_bau_File_openFile_2(_t20, string_1009);
    if (!(out)) {
        printf("Could not open output\n");
        return;
    }
    int64_t _t21 = org_bau_File_File_write_4(out, output, 0, outPos);
    ;
    org_bau_File_File_close_1(out);
    _decUseStack(out, org_bau_File_File);
    _decUseStack(_t20, i8_array);
    _decUseStack(_t15, i8_array);
    _decUseStack(_t14, Sr3);
    _decUseStack(output, i8_array);
    _decUseStack(_t13, i8_array);
    _decUseStack(input, i8_array);
    _decUseStack(_t11, i8_array);
    _decUseStack(in, org_bau_File_File);
    _decUseStack(_t9, i8_array);
    _decUseStack(DT, int_array);
    _decUseStack(_t5, int_array);
    _decUseStack(state, int_array);
    _decUseStack(_t6, i8_array);
    _decUseStack(_t7, i8_array);
    _decUseStack(global, Sr3);
    _end();
}
/*

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

*/
