#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
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
    if(block[0] >> 32 != 0) {
        int prevSize = block[0] >> 32;
        printf("prev block of free block is free: %p; prev size %d -> %p\n", block, prevSize, block - prevSize);
    }
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
#define REF_COUNT_INC
#define REF_COUNT_STACK_INC
#define PRINT(...)
#define _end()
#define _malloc(a)      tmmalloc(a)
#define _traceMalloc(a)
#define _free(a)        tmfree(a)
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
typedef struct int_array int_array;
struct int_array;
typedef struct org_bau_Exception_exception org_bau_Exception_exception;
struct org_bau_Exception_exception;
typedef struct Token Token;
struct Token;
typedef struct Token_array Token_array;
struct Token_array;
typedef struct org_bau_List_List_i8 org_bau_List_List_i8;
struct org_bau_List_List_i8;
typedef struct match match;
struct match;
typedef struct org_bau_List_List_Token org_bau_List_List_Token;
struct org_bau_List_List_Token;
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
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_Exception_exception {
    int64_t exceptionType;
    i8_array* message;
};
org_bau_Exception_exception org_bau_Exception_exception_new() {
    org_bau_Exception_exception result;
    result.exceptionType = 0;
    result.message = 0;
    return result;
}
struct Token {
    int64_t ttype;
    org_bau_List_List_i8* data;
    int64_t min;
    int64_t max;
    int32_t _refCount;
};
Token* Token_new() {
    Token* result = _malloc(sizeof(Token));
    _traceMalloc(result);
    result->_refCount = 1;
    result->ttype = 0;
    result->data = 0;
    result->min = 0;
    result->max = 0;
    return result;
}
struct Token_array {
    int32_t len;
    Token** data;
    int32_t _refCount;
};
Token_array* Token_array_new(uint32_t len) {
    Token_array* result = _malloc(sizeof(Token_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(Token*) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_List_List_i8 {
    i8_array* array;
    int64_t size;
    int32_t _refCount;
};
org_bau_List_List_i8* org_bau_List_List_i8_new() {
    org_bau_List_List_i8* result = _malloc(sizeof(org_bau_List_List_i8));
    _traceMalloc(result);
    result->_refCount = 1;
    result->array = 0;
    result->size = 0;
    return result;
}
struct match {
    int64_t start;
    int64_t end;
};
match match_new() {
    match result;
    result.start = 0;
    result.end = 0;
    return result;
}
struct org_bau_List_List_Token {
    Token_array* array;
    int64_t size;
    int32_t _refCount;
};
org_bau_List_List_Token* org_bau_List_List_Token_new() {
    org_bau_List_List_Token* result = _malloc(sizeof(org_bau_List_List_Token));
    _traceMalloc(result);
    result->_refCount = 1;
    result->array = 0;
    result->size = 0;
    return result;
}
/* exception types */
typedef struct _int64_t_or_exception _int64_t_or_exception;
struct _int64_t_or_exception {
    org_bau_Exception_exception exception;
    int64_t result;
};
_int64_t_or_exception ok_int64_t_or_exception(int64_t result) {
    _int64_t_or_exception x;
    x.exception.exceptionType = -1;
    x.result = result;
    return x;
}
_int64_t_or_exception exception_int64_t_or_exception(org_bau_Exception_exception exception) {
    _int64_t_or_exception x;
    x.exception = exception;
    return x;
}
typedef struct _match_or_exception _match_or_exception;
struct _match_or_exception {
    org_bau_Exception_exception exception;
    match result;
};
_match_or_exception ok_match_or_exception(match result) {
    _match_or_exception x;
    x.exception.exceptionType = -1;
    x.result = result;
    return x;
}
_match_or_exception exception_match_or_exception(org_bau_Exception_exception exception) {
    _match_or_exception x;
    x.exception = exception;
    return x;
}
typedef struct _org_bau_List_List_Token_or_exception _org_bau_List_List_Token_or_exception;
struct _org_bau_List_List_Token_or_exception {
    org_bau_Exception_exception exception;
    org_bau_List_List_Token* result;
};
_org_bau_List_List_Token_or_exception ok_org_bau_List_List_Token_or_exception(org_bau_List_List_Token* result) {
    _org_bau_List_List_Token_or_exception x;
    x.exception.exceptionType = -1;
    x.result = result;
    return x;
}
_org_bau_List_List_Token_or_exception exception_org_bau_List_List_Token_or_exception(org_bau_Exception_exception exception) {
    _org_bau_List_List_Token_or_exception x;
    x.exception = exception;
    return x;
}
/* global */
int __argc;
char **__argv;
/* functions */
Token* Token_1(org_bau_List_List_i8* data);
_int64_t_or_exception Token_matchChar_3(Token* this, i8_array* text, int64_t pos);
_match_or_exception find_2(i8_array* text, i8_array* regex);
int64_t idx_2(int64_t x, int64_t len);
match match_0();
match match_2(int64_t start, int64_t end);
_int64_t_or_exception matchHere_4(org_bau_List_List_Token* list, int64_t rp, i8_array* text, int64_t tp);
_int64_t_or_exception matchStar_6(Token* t, org_bau_List_List_Token* list, int64_t rp, i8_array* text, int64_t tp, int64_t max);
_int64_t_or_exception matches_2(i8_array* text, i8_array* regex);
org_bau_Exception_exception org_bau_Exception_exception_1(i8_array* message);
org_bau_List_List_Token* org_bau_List_List_Token_1(Token_array* array);
org_bau_List_List_i8* org_bau_List_List_i8_1(i8_array* array);
org_bau_List_List_Token* org_bau_List_newList_Token_1(int64_t _T);
org_bau_List_List_i8* org_bau_List_newList_i8_1(int64_t _T);
void org_bau_List_List_Token_add_2(org_bau_List_List_Token* this, Token* x);
void org_bau_List_List_i8_add_2(org_bau_List_List_i8* this, int8_t x);
_org_bau_List_List_Token_or_exception parse_1(i8_array* regex);
void test_0();
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_Exception_exception_free(org_bau_Exception_exception* x);
void org_bau_Exception_exception_copy(org_bau_Exception_exception* x);
void Token_free(Token* x);
void Token_array_free(Token_array* x);
void org_bau_List_List_i8_free(org_bau_List_List_i8* x);
void match_free(match* x);
void match_copy(match* x);
void org_bau_List_List_Token_free(org_bau_List_List_Token* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void org_bau_Exception_exception_free(org_bau_Exception_exception* x) {
    _decUse(x->message, i8_array);
}
void org_bau_Exception_exception_copy(org_bau_Exception_exception* x) {
    _incUse(x->message);
}
void Token_free(Token* x) {
    _decUse(x->data, org_bau_List_List_i8);
    _free(x);
}
void Token_array_free(Token_array* x) {
    for (int i = 0; i < x->len; i++) _decUse(x->data[i], Token);
    _free(x->data);
    _free(x);
}
void org_bau_List_List_i8_free(org_bau_List_List_i8* x) {
    _decUse(x->array, i8_array);
    _free(x);
}
void match_free(match* x) {
}
void match_copy(match* x) {
}
void org_bau_List_List_Token_free(org_bau_List_List_Token* x) {
    _decUse(x->array, Token_array);
    _free(x);
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
i8_array* string_1017;
i8_array* string_1018;
i8_array* string_1019;
i8_array* string_1020;
i8_array* string_1021;
i8_array* string_1022;
Token* Token_1(org_bau_List_List_i8* data) {
    Token* _t0 = Token_new();
    _t0->ttype = 0;
    _decUse(_t0->data, org_bau_List_List_i8);
    _t0->data = data;
    _incUse(_t0->data);
    _t0->min = 0;
    _t0->max = 0;
    return _t0;
}
_int64_t_or_exception Token_matchChar_3(Token* this, i8_array* text, int64_t pos) {
    org_bau_Exception_exception _lastException;
    _int64_t_or_exception _x0;
    _int64_t_or_exception _x1;
    if (pos >= text->len) {
        return ok_int64_t_or_exception(0);
    }
    int8_t c = text->data[pos];
    int64_t _t0 = this->ttype;
    if ((_t0 == 0) || (_t0 == 1)) {
        org_bau_Exception_exception _t1 = org_bau_Exception_exception_1(string_1000);
        _x0 = exception_int64_t_or_exception(_t1); _lastException = _x0.exception; goto catch0;
        org_bau_Exception_exception_free(&_t1);
    } else if (_t0 == 2) {
        int64_t _r0 = c == this->data->array->data[idx_2(0, this->data->array->len)];
        return ok_int64_t_or_exception(_r0);
    } else if (_t0 == 3) {
        while (1 == 1) {
            int64_t i = 0;
            while (i < this->data->size) {
                int8_t min = this->data->array->data[idx_2(i, this->data->array->len)];
                int8_t max = this->data->array->data[idx_2(i + 1, this->data->array->len)];
                int64_t _t2 = c >= min;
                if (_t2) {
                    int64_t _t3 = c <= max;
                    _t2 = _t3;
                }
                if (_t2) {
                    return ok_int64_t_or_exception(1);
                }
                i += 1;
                i += 1;
            }
            break;
        }
        return ok_int64_t_or_exception(0);
    } else if (_t0 == 4) {
        while (1 == 1) {
            int64_t i = 0;
            while (i < this->data->size) {
                int8_t min = this->data->array->data[idx_2(i, this->data->array->len)];
                int8_t max = this->data->array->data[idx_2(i + 1, this->data->array->len)];
                int64_t _t4 = c >= min;
                if (_t4) {
                    int64_t _t5 = c <= max;
                    _t4 = _t5;
                }
                if (_t4) {
                    return ok_int64_t_or_exception(0);
                }
                i += 1;
                i += 1;
            }
            break;
        }
        return ok_int64_t_or_exception(1);
    } else if (_t0 == 5) {
        return ok_int64_t_or_exception(1);
    } else {
        org_bau_Exception_exception _t6 = org_bau_Exception_exception_1(string_1001);
        _x1 = exception_int64_t_or_exception(_t6); _lastException = _x1.exception; goto catch0;
        org_bau_Exception_exception_free(&_t6);
    }
    catch0:
    return exception_int64_t_or_exception(_lastException);
}
_match_or_exception find_2(i8_array* text, i8_array* regex) {
    org_bau_Exception_exception _lastException;
    _org_bau_List_List_Token_or_exception _x0;
    _int64_t_or_exception _x1;
    _int64_t_or_exception _x2;
    _x0 = parse_1(regex);
    if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
    org_bau_List_List_Token* list = _x0.result;
    if (list->size > 0) {
        Token* e = list->array->data[idx_2(0, list->array->len)];
        _incUseStack(e);
        if (e != NULL) {
            if (e->ttype == 0) {
                _x1 = matchHere_4(list, 1, text, 0);
                if (_x1.exception.exceptionType != -1) { _lastException = _x1.exception; goto catch0; };
                int64_t end = _x1.result;
                if (end >= 0) {
                    match _t0 = match_2(0, end);
                    _decUseStack(e, Token);
                    _decUseStack(list, org_bau_List_List_Token);
                    return ok_match_or_exception(_t0);
                }
                match _t1 = match_2(-1, -1);
                _decUseStack(e, Token);
                _decUseStack(list, org_bau_List_List_Token);
                return ok_match_or_exception(_t1);
            }
        }
        _decUseStack(e, Token);
    }
    int64_t tp = 0;
    while (1) {
        _x2 = matchHere_4(list, 0, text, tp);
        if (_x2.exception.exceptionType != -1) { _lastException = _x2.exception; goto catch0; };
        int64_t end = _x2.result;
        if (end >= 0) {
            match _t2 = match_2(tp, end);
            _decUseStack(list, org_bau_List_List_Token);
            return ok_match_or_exception(_t2);
        }
        if (tp < text->len) {
            break;
        }
        tp += 1;
    }
    match _t3 = match_2(-1, -1);
    _decUseStack(list, org_bau_List_List_Token);
    return ok_match_or_exception(_t3);
    catch0:
    return exception_match_or_exception(_lastException);
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
match match_0() {
    match _t2 = match_new();
    _t2.start = 0;
    _t2.end = 0;
    return _t2;
}
match match_2(int64_t start, int64_t end) {
    match m = match_0();
    m.start = start;
    m.end = end;
    return m;
}
_int64_t_or_exception matchHere_4(org_bau_List_List_Token* list, int64_t rp, i8_array* text, int64_t tp) {
    org_bau_Exception_exception _lastException;
    _int64_t_or_exception _x0;
    _int64_t_or_exception _x1;
    if (rp >= list->size) {
        return ok_int64_t_or_exception(tp);
    }
    Token* t = list->array->data[idx_2(rp, list->array->len)];
    _incUseStack(t);
    if (!(t)) {
        _decUseStack(t, Token);
        return ok_int64_t_or_exception(tp);
    }
    if (t->ttype == 1) {
        if (tp == text->len) {
            _decUseStack(t, Token);
            return ok_int64_t_or_exception(tp);
        }
        _decUseStack(t, Token);
        return ok_int64_t_or_exception(-1);
    }
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            _x0 = Token_matchChar_3(t, text, tp);
            if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
            int64_t _t0 = _x0.result;
            if (!(_t0)) {
                _decUseStack(t, Token);
                return ok_int64_t_or_exception(-1);
            }
            tp += 1;
            int64_t _next = i + 1;
            if (_next >= t->min) {
                break;
            }
            i = _next;
        }
        break;
    }
    _x1 = matchStar_6(t, list, rp + 1, text, tp, t->max - t->min);
    if (_x1.exception.exceptionType != -1) { _lastException = _x1.exception; goto catch0; };
    int64_t _t1 = _x1.result;
    _decUseStack(t, Token);
    return ok_int64_t_or_exception(_t1);
    catch0:
    return exception_int64_t_or_exception(_lastException);
}
_int64_t_or_exception matchStar_6(Token* t, org_bau_List_List_Token* list, int64_t rp, i8_array* text, int64_t tp, int64_t max) {
    org_bau_Exception_exception _lastException;
    _int64_t_or_exception _x0;
    _int64_t_or_exception _x1;
    int64_t result = -1;
    while (1) {
        _x0 = matchHere_4(list, rp, text, tp);
        if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
        int64_t end = _x0.result;
        if (end >= 0) {
            result = end;
        }
        _x1 = Token_matchChar_3(t, text, tp);
        if (_x1.exception.exceptionType != -1) { _lastException = _x1.exception; goto catch0; };
        int64_t r = _x1.result;
        tp += 1;
        if (r == 0) {
            break;
        }
        if (max <= 0) {
            break;
        }
        max -= 1;
    }
    return ok_int64_t_or_exception(result);
    catch0:
    return exception_int64_t_or_exception(_lastException);
}
_int64_t_or_exception matches_2(i8_array* text, i8_array* regex) {
    org_bau_Exception_exception _lastException;
    _match_or_exception _x0;
    _x0 = find_2(text, regex);
    if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
    match result = _x0.result;
    int64_t _t0 = result.start == 0;
    if (_t0) {
        int64_t _t1 = result.end == text->len;
        _t0 = _t1;
    }
    match_free(&result);
    return ok_int64_t_or_exception(_t0);
    catch0:
    return exception_int64_t_or_exception(_lastException);
}
org_bau_Exception_exception org_bau_Exception_exception_1(i8_array* message) {
    org_bau_Exception_exception _t0 = org_bau_Exception_exception_new();
    _t0.exceptionType = 0;
    _decUse(_t0.message, i8_array);
    _t0.message = message;
    _incUse(_t0.message);
    return _t0;
}
org_bau_List_List_Token* org_bau_List_List_Token_1(Token_array* array) {
    org_bau_List_List_Token* _t0 = org_bau_List_List_Token_new();
    _decUse(_t0->array, Token_array);
    _t0->array = array;
    _incUse(_t0->array);
    _t0->size = 0;
    return _t0;
}
org_bau_List_List_i8* org_bau_List_List_i8_1(i8_array* array) {
    org_bau_List_List_i8* _t0 = org_bau_List_List_i8_new();
    _decUse(_t0->array, i8_array);
    _t0->array = array;
    _incUse(_t0->array);
    _t0->size = 0;
    return _t0;
}
org_bau_List_List_Token* org_bau_List_newList_Token_1(int64_t _T) {
    Token_array* _t0 = Token_array_new(4);
    org_bau_List_List_Token* _t1 = org_bau_List_List_Token_1(_t0);
    _decUseStack(_t0, Token_array);
    return _t1;
}
org_bau_List_List_i8* org_bau_List_newList_i8_1(int64_t _T) {
    i8_array* _t0 = i8_array_new(4);
    org_bau_List_List_i8* _t1 = org_bau_List_List_i8_1(_t0);
    _decUseStack(_t0, i8_array);
    return _t1;
}
void org_bau_List_List_Token_add_2(org_bau_List_List_Token* this, Token* x) {
    if (this->size >= this->array->len) {
        Token_array* _t0 = Token_array_new(this->array->len * 2);
        Token_array* n = _t0;
        _incUseStack(n);
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                _decUse(n->data[idx_2(i, n->len)], Token);
                n->data[idx_2(i, n->len)] = this->array->data[i];
                _incUse(n->data[idx_2(i, n->len)]);
                int64_t _next = i + 1;
                if (_next >= this->array->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
        _decUse(this->array, Token_array);
        this->array = n;
        _incUse(this->array);
        _decUseStack(n, Token_array);
        _decUseStack(_t0, Token_array);
    }
    _decUse(this->array->data[idx_2(this->size, this->array->len)], Token);
    this->array->data[idx_2(this->size, this->array->len)] = x;
    _incUse(this->array->data[idx_2(this->size, this->array->len)]);
    this->size += 1;
}
void org_bau_List_List_i8_add_2(org_bau_List_List_i8* this, int8_t x) {
    if (this->size >= this->array->len) {
        i8_array* _t0 = i8_array_new(this->array->len * 2);
        i8_array* n = _t0;
        _incUseStack(n);
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                n->data[idx_2(i, n->len)] = this->array->data[i];
                int64_t _next = i + 1;
                if (_next >= this->array->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
        _decUse(this->array, i8_array);
        this->array = n;
        _incUse(this->array);
        _decUseStack(n, i8_array);
        _decUseStack(_t0, i8_array);
    }
    this->array->data[idx_2(this->size, this->array->len)] = x;
    this->size += 1;
}
_org_bau_List_List_Token_or_exception parse_1(i8_array* regex) {
    org_bau_Exception_exception _lastException;
    _org_bau_List_List_Token_or_exception _x0;
    org_bau_List_List_Token* result = org_bau_List_newList_Token_1(0);
    int64_t i = 0;
    while (i < regex->len) {
        int8_t c = regex->data[i];
        org_bau_List_List_i8* _t0 = org_bau_List_newList_i8_1(0);
        Token* t = Token_1(_t0);
        t->min = 1;
        t->max = 1;
        if (c == 94) {
            t->ttype = 0;
            org_bau_List_List_Token_add_2(result, t);
            i += 1;
            continue;
        }
        if (c == 36) {
            t->ttype = 1;
            org_bau_List_List_Token_add_2(result, t);
            i += 1;
            continue;
        }
        if (c == 46) {
            t->ttype = 5;
        } else if (c == 92) {
            t->ttype = 2;
            i += 1;
            org_bau_List_List_i8_add_2(t->data, regex->data[i]);
        } else if (c == 91) {
            i += 1;
            c = regex->data[i];
            if (c == 94) {
                t->ttype = 4;
                i += 1;
                c = regex->data[i];
            } else {
                t->ttype = 3;
            }
            org_bau_List_List_i8_add_2(t->data, c);
            org_bau_List_List_i8_add_2(t->data, c);
            while (1) {
                i += 1;
                c = regex->data[i];
                if (c == 93) {
                    break;
                }
                if (c == 45) {
                    i += 1;
                    c = regex->data[i];
                    t->data->array->data[idx_2(t->data->size - 1, t->data->array->len)] = c;
                } else {
                    org_bau_List_List_i8_add_2(t->data, c);
                    org_bau_List_List_i8_add_2(t->data, c);
                }
            }
        } else {
            t->ttype = 2;
            org_bau_List_List_i8_add_2(t->data, c);
        }
        org_bau_List_List_Token_add_2(result, t);
        if (( i + 1 ) >= regex->len) {
            _decUseStack(t, Token);
            _decUseStack(_t0, org_bau_List_List_i8);
            break;
        }
        i += 1;
        c = regex->data[i];
        if (c == 43) {
            t->max = 9999;
        } else if (c == 42) {
            t->min = 0;
            t->max = 9999;
        } else if (c == 63) {
            t->min = 0;
            t->max = 1;
        } else if (c == 123) {
            i += 1;
            int64_t start = i;
            int64_t value = 0;
            while (i < regex->len) {
                c = regex->data[i];
                int64_t _t1 = c < 48;
                if (!(_t1)) {
                    int64_t _t2 = c > 57;
                    _t1 = _t2;
                }
                if (_t1) {
                    break;
                }
                value = ( value * 10 ) + c - 48;
                if (value > 9999) {
                    break;
                }
                i += 1;
            }
            t->min = value;
            t->max = t->min;
            if (c == 44) {
                i += 1;
                start = i;
                value = 0;
                while (i < regex->len) {
                    c = regex->data[i];
                    int64_t _t3 = c < 48;
                    if (!(_t3)) {
                        int64_t _t4 = c > 57;
                        _t3 = _t4;
                    }
                    if (_t3) {
                        break;
                    }
                    value = ( value * 10 ) + c - 48;
                    if (value > 9999) {
                        break;
                    }
                    i += 1;
                }
                t->max = value;
            }
            if (c != 125) {
                org_bau_Exception_exception _t5 = org_bau_Exception_exception_1(string_1017);
                _x0 = exception_org_bau_List_List_Token_or_exception(_t5); _lastException = _x0.exception; goto catch0;
                org_bau_Exception_exception_free(&_t5);
            }
        } else {
            i -= 1;
        }
        i += 1;
        _decUseStack(t, Token);
        _decUseStack(_t0, org_bau_List_List_i8);
    }
    return ok_org_bau_List_List_Token_or_exception(result);
    catch0:
    return exception_org_bau_List_List_Token_or_exception(_lastException);
}
void test_0() {
    org_bau_Exception_exception _lastException;
    _int64_t_or_exception _x0;
    _int64_t_or_exception _x1;
    _x0 = matches_2(string_1018, string_1019);
    if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
    int64_t _t0 = _x0.result;
    printf("%lld\n", (long long)_t0);
    _x1 = matches_2(string_1020, string_1021);
    if (_x1.exception.exceptionType != -1) { _lastException = _x1.exception; goto catch0; };
    int64_t _t1 = _x1.result;
    printf("%lld\n", (long long)_t1);
    goto skip0;
    catch0:;
    org_bau_Exception_exception e = _lastException;
        printf("exception %.*s\n", e.message->len, e.message->data);
        org_bau_Exception_exception_free(&e);
    skip0:;
}
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("Start or end in wrong location", 30);
    string_1001 = str_const("Unknown token type", 18);
    string_1017 = str_const("Expected }", 10);
    string_1018 = str_const("hello", 5);
    string_1019 = str_const("hell.*", 6);
    string_1020 = str_const("hello world", 11);
    string_1021 = str_const("hell[o-z] world", 15);
    string_1022 = str_const("exception ", 10);
    test_0();
    _end();
    return 0;
}
/*

type List(T)
List

fun List(T) add(x T)
Add an entry to the list

fun List(T) get(x int) T?
Add an entry to the list

type exception
An exception

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

*/
