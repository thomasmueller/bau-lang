#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <limits.h>
#include <sqlite3.h>
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
typedef struct org_bau_String_string org_bau_String_string;
struct org_bau_String_string;
typedef struct org_bau_String_string_array org_bau_String_string_array;
struct org_bau_String_string_array;
typedef struct org_bau_String_StringBuilder org_bau_String_StringBuilder;
struct org_bau_String_StringBuilder;
typedef struct org_bau_List_List_org_bau_String_string org_bau_List_List_org_bau_String_string;
struct org_bau_List_List_org_bau_String_string;
typedef struct org_bau_db_Sqlite3_Sqlite org_bau_db_Sqlite3_Sqlite;
struct org_bau_db_Sqlite3_Sqlite;
typedef struct org_bau_db_Sqlite3_text org_bau_db_Sqlite3_text;
struct org_bau_db_Sqlite3_text;
typedef struct org_bau_db_Sqlite3_Record org_bau_db_Sqlite3_Record;
struct org_bau_db_Sqlite3_Record;
typedef struct Customer Customer;
struct Customer;
typedef struct Customer_array Customer_array;
struct Customer_array;
typedef struct org_bau_db_Sqlite3_Query_Customer org_bau_db_Sqlite3_Query_Customer;
struct org_bau_db_Sqlite3_Query_Customer;
typedef struct org_bau_List_List_Customer org_bau_List_List_Customer;
struct org_bau_List_List_Customer;
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
struct org_bau_List_List_org_bau_String_string {
    int32_t _refCount;
    org_bau_String_string_array* array;
    int64_t size;
};
org_bau_List_List_org_bau_String_string* org_bau_List_List_org_bau_String_string_new() {
    org_bau_List_List_org_bau_String_string* result = _malloc(sizeof(org_bau_List_List_org_bau_String_string));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_db_Sqlite3_Sqlite {
    int32_t _refCount;
    i8_array* name;
    i8_array* error;
    int64_t db;
    int64_t stmt;
};
org_bau_db_Sqlite3_Sqlite* org_bau_db_Sqlite3_Sqlite_new() {
    org_bau_db_Sqlite3_Sqlite* result = _malloc(sizeof(org_bau_db_Sqlite3_Sqlite));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_db_Sqlite3_text {
    i8_array* data;
};
org_bau_db_Sqlite3_text org_bau_db_Sqlite3_text_new() {
    org_bau_db_Sqlite3_text result;
    result.data = 0;
    return result;
}
struct org_bau_db_Sqlite3_Record {
    int32_t _refCount;
    i8_array* data;
};
org_bau_db_Sqlite3_Record* org_bau_db_Sqlite3_Record_new() {
    org_bau_db_Sqlite3_Record* result = _malloc(sizeof(org_bau_db_Sqlite3_Record));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct Customer {
    int32_t _refCount;
    int64_t id;
    org_bau_db_Sqlite3_text name;
};
Customer* Customer_new() {
    Customer* result = _malloc(sizeof(Customer));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct Customer_array {
    int32_t len;
    int32_t _refCount;
    Customer** data;
};
Customer_array* Customer_array_new(uint64_t len) {
    if (len < 0 || len >= (1L << 31)) arrayOutOfBounds(len, 1L << 31);
    Customer_array* result = _malloc(sizeof(Customer_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(Customer*) * len);
    memset(result->data, 0, sizeof(Customer*) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_db_Sqlite3_Query_Customer {
    int32_t _refCount;
    org_bau_db_Sqlite3_Sqlite* db;
    org_bau_db_Sqlite3_text tableName;
    org_bau_db_Sqlite3_text condition;
    org_bau_db_Sqlite3_text orderBy;
};
org_bau_db_Sqlite3_Query_Customer* org_bau_db_Sqlite3_Query_Customer_new() {
    org_bau_db_Sqlite3_Query_Customer* result = _malloc(sizeof(org_bau_db_Sqlite3_Query_Customer));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_List_List_Customer {
    int32_t _refCount;
    Customer_array* array;
    int64_t size;
};
org_bau_List_List_Customer* org_bau_List_List_Customer_new() {
    org_bau_List_List_Customer* result = _malloc(sizeof(org_bau_List_List_Customer));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
Customer* Customer_1(org_bau_db_Sqlite3_text name);
Customer* convertRecordToCustomer_1(org_bau_db_Sqlite3_Record* r);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
int64_t imod_2(int64_t a, int64_t b);
Customer* newCustomer_2(int64_t id, org_bau_db_Sqlite3_text name);
int64_t org_bau_Int_parseInt_1(i8_array* value);
org_bau_List_List_Customer* org_bau_List_List_Customer_0();
org_bau_List_List_org_bau_String_string* org_bau_List_List_org_bau_String_string_0();
org_bau_List_List_Customer* org_bau_List_newList_Customer_1(int64_t _T);
org_bau_List_List_org_bau_String_string* org_bau_List_newList_org_bau_String_string_1(int64_t _T);
void org_bau_List_List_Customer_add_2(org_bau_List_List_Customer* this, Customer* x);
Customer* org_bau_List_List_Customer_get_2(org_bau_List_List_Customer* this, int64_t x);
void org_bau_List_List_org_bau_String_string_add_2(org_bau_List_List_org_bau_String_string* this, org_bau_String_string x);
org_bau_String_string org_bau_List_List_org_bau_String_string_get_2(org_bau_List_List_org_bau_String_string* this, int64_t x);
int64_t org_bau_List_List_org_bau_String_string_len_1(org_bau_List_List_org_bau_String_string* this);
i8_array* org_bau_Std_convertIntToI8Array_1(int64_t x);
org_bau_String_StringBuilder* org_bau_String_StringBuilder_0();
int64_t org_bau_String_indexOf_2(i8_array* s, i8_array* find);
int64_t org_bau_String_indexOf_3(i8_array* s, i8_array* find, int64_t start);
org_bau_String_StringBuilder* org_bau_String_newStringBuilder_1(int64_t len);
org_bau_List_List_org_bau_String_string* org_bau_String_split_2(i8_array* s, i8_array* delimiter);
org_bau_String_string org_bau_String_str_1(i8_array* s);
org_bau_String_string org_bau_String_string_0();
i8_array* org_bau_String_substring_2(i8_array* s, int64_t start);
i8_array* org_bau_String_substring_3(i8_array* s, int64_t start, int64_t end);
void org_bau_String_StringBuilder_append_2(org_bau_String_StringBuilder* this, i8_array* b);
void org_bau_String_StringBuilder_append_4(org_bau_String_StringBuilder* this, i8_array* b, int64_t start, int64_t end);
org_bau_String_string org_bau_String_StringBuilder_toString_1(org_bau_String_StringBuilder* this);
org_bau_db_Sqlite3_Query_Customer* org_bau_db_Sqlite3_Query_Customer_4(org_bau_db_Sqlite3_Sqlite* db, org_bau_db_Sqlite3_text tableName, org_bau_db_Sqlite3_text condition, org_bau_db_Sqlite3_text orderBy);
org_bau_db_Sqlite3_Record* org_bau_db_Sqlite3_Record_0();
org_bau_db_Sqlite3_Sqlite* org_bau_db_Sqlite3_Sqlite_0();
void org_bau_db_Sqlite3_cleanIt_1(i8_array* expr);
org_bau_db_Sqlite3_text org_bau_db_Sqlite3_convertI8ArrayToText_1(i8_array* t);
i8_array* org_bau_db_Sqlite3_convertTextToI8Array_1(org_bau_db_Sqlite3_text t);
int64_t org_bau_db_Sqlite3_convertTextToInt_1(org_bau_db_Sqlite3_text t);
i8_array* org_bau_db_Sqlite3_getValue_Customer_2(Customer* row, int64_t c);
org_bau_db_Sqlite3_Sqlite* org_bau_db_Sqlite3_open_1(i8_array* name);
i8_array* org_bau_db_Sqlite3_quote_1(i8_array* s);
void org_bau_db_Sqlite3_setField_Customer_3(Customer* row, int64_t c, org_bau_db_Sqlite3_text n);
org_bau_db_Sqlite3_text org_bau_db_Sqlite3_text_0();
org_bau_List_List_Customer* org_bau_db_Sqlite3_Query_Customer_select_1(org_bau_db_Sqlite3_Query_Customer* this);
void org_bau_db_Sqlite3_Sqlite_close_1(org_bau_db_Sqlite3_Sqlite* this);
void org_bau_db_Sqlite3_Sqlite_createTable_Customer_2(org_bau_db_Sqlite3_Sqlite* this, int64_t _T);
void org_bau_db_Sqlite3_Sqlite_dropTable_Customer_2(org_bau_db_Sqlite3_Sqlite* this, int64_t _T);
void org_bau_db_Sqlite3_Sqlite_execute_2(org_bau_db_Sqlite3_Sqlite* this, i8_array* sql);
org_bau_db_Sqlite3_Query_Customer* org_bau_db_Sqlite3_Sqlite_from_Customer_2(org_bau_db_Sqlite3_Sqlite* this, int64_t _T);
i8_array* org_bau_db_Sqlite3_Sqlite_getString_2(org_bau_db_Sqlite3_Sqlite* this, int64_t column);
void org_bau_db_Sqlite3_Sqlite_insert_Customer_2(org_bau_db_Sqlite3_Sqlite* this, Customer* x);
int64_t org_bau_db_Sqlite3_Sqlite_nextRow_1(org_bau_db_Sqlite3_Sqlite* this);
int64_t org_bau_db_Sqlite3_Sqlite_prepare_2(org_bau_db_Sqlite3_Sqlite* this, i8_array* sql);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_String_string_free(org_bau_String_string* x);
void org_bau_String_string_copy(org_bau_String_string* x);
void org_bau_String_string_array_free(org_bau_String_string_array* x);
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x);
void org_bau_List_List_org_bau_String_string_free(org_bau_List_List_org_bau_String_string* x);
void org_bau_db_Sqlite3_Sqlite_free(org_bau_db_Sqlite3_Sqlite* x);
void org_bau_db_Sqlite3_text_free(org_bau_db_Sqlite3_text* x);
void org_bau_db_Sqlite3_text_copy(org_bau_db_Sqlite3_text* x);
void org_bau_db_Sqlite3_Record_free(org_bau_db_Sqlite3_Record* x);
void Customer_free(Customer* x);
void Customer_array_free(Customer_array* x);
void org_bau_db_Sqlite3_Query_Customer_free(org_bau_db_Sqlite3_Query_Customer* x);
void org_bau_List_List_Customer_free(org_bau_List_List_Customer* x);
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
void org_bau_List_List_org_bau_String_string_free_0(org_bau_List_List_org_bau_String_string* x) {
    _decUse(x->array, org_bau_String_string_array);
    _free(x); _traceFree(x);
}
void org_bau_List_List_org_bau_String_string_free(org_bau_List_List_org_bau_String_string* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_List_List_org_bau_String_string_free_0);
}
void org_bau_db_Sqlite3_Sqlite_free_0(org_bau_db_Sqlite3_Sqlite* x) {
    _decUse(x->name, i8_array);
    _decUse(x->error, i8_array);
    org_bau_db_Sqlite3_Sqlite_close_1(x);
    if (x->_refCount) { fprintf(stdout, "Object re-referenced in the close method"); exit(1); }
    _free(x); _traceFree(x);
}
void org_bau_db_Sqlite3_Sqlite_free(org_bau_db_Sqlite3_Sqlite* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_db_Sqlite3_Sqlite_free_0);
}
void org_bau_db_Sqlite3_text_free_0(org_bau_db_Sqlite3_text* x) {
    _decUse(x->data, i8_array);
}
void org_bau_db_Sqlite3_text_free(org_bau_db_Sqlite3_text* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_db_Sqlite3_text_free_0);
}
void org_bau_db_Sqlite3_text_copy(org_bau_db_Sqlite3_text* x) {
    _incUse(x->data);
}
void org_bau_db_Sqlite3_Record_free_0(org_bau_db_Sqlite3_Record* x) {
    _decUse(x->data, i8_array);
    _free(x); _traceFree(x);
}
void org_bau_db_Sqlite3_Record_free(org_bau_db_Sqlite3_Record* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_db_Sqlite3_Record_free_0);
}
void Customer_free_0(Customer* x) {
    org_bau_db_Sqlite3_text_free(&x->name);
    _free(x); _traceFree(x);
}
void Customer_free(Customer* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))Customer_free_0);
}
void Customer_array_free_0(Customer_array* x) {
    for (int i = 0; i < _arrayLen(x); i++) _decUse(x->data[i], Customer);
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void Customer_array_free(Customer_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))Customer_array_free_0);
}
void org_bau_db_Sqlite3_Query_Customer_free_0(org_bau_db_Sqlite3_Query_Customer* x) {
    _decUse(x->db, org_bau_db_Sqlite3_Sqlite);
    org_bau_db_Sqlite3_text_free(&x->tableName);
    org_bau_db_Sqlite3_text_free(&x->condition);
    org_bau_db_Sqlite3_text_free(&x->orderBy);
    _free(x); _traceFree(x);
}
void org_bau_db_Sqlite3_Query_Customer_free(org_bau_db_Sqlite3_Query_Customer* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_db_Sqlite3_Query_Customer_free_0);
}
void org_bau_List_List_Customer_free_0(org_bau_List_List_Customer* x) {
    _decUse(x->array, Customer_array);
    _free(x); _traceFree(x);
}
void org_bau_List_List_Customer_free(org_bau_List_List_Customer* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))org_bau_List_List_Customer_free_0);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = _malloc(sizeof(char) * len);
    memcpy(result->data, data, sizeof(char) * len);
    return result;
}
i8_array* string_1005;
i8_array* string_1008;
i8_array* string_1014;
i8_array* string_1018;
i8_array* string_1019;
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
i8_array* string_1041;
i8_array* string_1043;
i8_array* string_1045;
i8_array* string_1046;
int64_t randomSeed;
int64_t MIN_INT;
int64_t MAX_INT;
int64_t MIN_I32;
int64_t MAX_I32;
Customer* Customer_1(org_bau_db_Sqlite3_text name) {
    Customer* _t19 = Customer_new();
    _t19->id = 0;
    org_bau_db_Sqlite3_text_copy(&name);
    _t19->name = name;
    return _t19;
}
Customer* convertRecordToCustomer_1(org_bau_db_Sqlite3_Record* r) {
    org_bau_db_Sqlite3_text _t0 = org_bau_db_Sqlite3_text_0();
    Customer* _t1 = Customer_1(_t0);
    org_bau_db_Sqlite3_text_free(&_t0);
    return _t1;
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
Customer* newCustomer_2(int64_t id, org_bau_db_Sqlite3_text name) {
    Customer* u = Customer_1(name);
    u->id = id;
    return u;
}
int64_t org_bau_Int_parseInt_1(i8_array* value) {
    int64_t x = 0;
    int64_t sign = 1;
    if (_arrayLen(value)) {
        int64_t i = 0;
        if (value->data[0] == 45) {
            sign = -1;
            i = i + 1;
        }
        while (i < _arrayLen(value)) {
            int8_t n = value->data[idx_2(i, _arrayLen(value))];
            int64_t _t0 = n >= 48;
            if (_t0) {
                int64_t _t1 = n <= 57;
                _t0 = _t1;
            }
            if (_t0) {
                x = x * 10;
                x = x + ( n - 48 );
            }
            i = i + 1;
        }
        int64_t _r0 = sign * x;
        return _r0;
    }
    return 0;
}
org_bau_List_List_Customer* org_bau_List_List_Customer_0() {
    org_bau_List_List_Customer* _t39 = org_bau_List_List_Customer_new();
    _t39->array = Customer_array_new(0);
    _t39->size = 0;
    return _t39;
}
org_bau_List_List_org_bau_String_string* org_bau_List_List_org_bau_String_string_0() {
    org_bau_List_List_org_bau_String_string* _t12 = org_bau_List_List_org_bau_String_string_new();
    _t12->array = org_bau_String_string_array_new(0);
    _t12->size = 0;
    return _t12;
}
org_bau_List_List_Customer* org_bau_List_newList_Customer_1(int64_t _T) {
    org_bau_List_List_Customer* result = org_bau_List_List_Customer_0();
    Customer_array* _t44 = Customer_array_new(4);
    _incUseStack(_t44);
    _decUse(result->array, Customer_array);
    result->array = _t44;
    _decUseStack(_t44, Customer_array);
    return result;
}
org_bau_List_List_org_bau_String_string* org_bau_List_newList_org_bau_String_string_1(int64_t _T) {
    org_bau_List_List_org_bau_String_string* result = org_bau_List_List_org_bau_String_string_0();
    org_bau_String_string_array* _t15 = org_bau_String_string_array_new(4);
    _incUseStack(_t15);
    _decUse(result->array, org_bau_String_string_array);
    result->array = _t15;
    _decUseStack(_t15, org_bau_String_string_array);
    return result;
}
void org_bau_List_List_Customer_add_2(org_bau_List_List_Customer* this, Customer* x) {
    _incUseStack(x);
    if (this->size >= _arrayLen(this->array)) {
        Customer_array* _t40 = Customer_array_new(_arrayLen(this->array) * 2);
        _incUseStack(_t40);
        Customer_array* n = _t40;
        if (_arrayLen(this->array) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    _incUseStack(this->array->data[i]);
                    _decUse(n->data[idx_2(i, _arrayLen(n))], Customer);
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
        _decUse(this->array, Customer_array);
        this->array = n;
        _decUseStack(n, Customer_array);
        _decUseStack(_t40, Customer_array);
    }
    _incUseStack(x);
    _decUse(this->array->data[idx_2(this->size, _arrayLen(this->array))], Customer);
    this->array->data[idx_2(this->size, _arrayLen(this->array))] = x;
    this->size += 1;
    _decUseStack(x, Customer);
}
Customer* org_bau_List_List_Customer_get_2(org_bau_List_List_Customer* this, int64_t x) {
    _incUseStack(this->array->data[idx_2(x, _arrayLen(this->array))]);
    Customer* _r1 = this->array->data[idx_2(x, _arrayLen(this->array))];
    return _r1;
}
void org_bau_List_List_org_bau_String_string_add_2(org_bau_List_List_org_bau_String_string* this, org_bau_String_string x) {
    org_bau_String_string_copy(&x);
    if (this->size >= _arrayLen(this->array)) {
        org_bau_String_string_array* _t13 = org_bau_String_string_array_new(_arrayLen(this->array) * 2);
        _incUseStack(_t13);
        org_bau_String_string_array* n = _t13;
        if (_arrayLen(this->array) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    org_bau_String_string_copy(&this->array->data[i]);
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
        _decUse(this->array, org_bau_String_string_array);
        this->array = n;
        _decUseStack(n, org_bau_String_string_array);
        _decUseStack(_t13, org_bau_String_string_array);
    }
    org_bau_String_string_copy(&x);
    this->array->data[idx_2(this->size, _arrayLen(this->array))] = x;
    this->size += 1;
    org_bau_String_string_free(&x);
}
org_bau_String_string org_bau_List_List_org_bau_String_string_get_2(org_bau_List_List_org_bau_String_string* this, int64_t x) {
    org_bau_String_string_copy(&this->array->data[idx_2(x, _arrayLen(this->array))]);
    org_bau_String_string _r1 = this->array->data[idx_2(x, _arrayLen(this->array))];
    return _r1;
}
int64_t org_bau_List_List_org_bau_String_string_len_1(org_bau_List_List_org_bau_String_string* this) {
    int64_t _r0 = this->size;
    return _r0;
}
i8_array* org_bau_Std_convertIntToI8Array_1(int64_t x) {
    int64_t len = 1;
    if (x >= 0) {
        x = - x;
        len = 0;
    }
    int64_t n = x;
    while (1) {
        len = len + 1;
        n = idiv_2(n, 10);
        if (n >= 0) {
            break;
        }
    }
    i8_array* _t1 = i8_array_new(len);
    _incUseStack(_t1);
    i8_array* buff = _t1;
    buff->data[idx_2(0, _arrayLen(buff))] = 45;
    while (1) {
        len = len - 1;
        buff->data[idx_2(len, _arrayLen(buff))] = 48 - (imod_2(x, 10));
        x = idiv_2(x, 10);
        if (x >= 0) {
            break;
        }
    }
    _decUseStack(_t1, i8_array);
    return buff;
}
org_bau_String_StringBuilder* org_bau_String_StringBuilder_0() {
    org_bau_String_StringBuilder* _t11 = org_bau_String_StringBuilder_new();
    _t11->data = i8_array_new(0);
    _t11->len = 0;
    return _t11;
}
int64_t org_bau_String_indexOf_2(i8_array* s, i8_array* find) {
    int64_t _t0 = org_bau_String_indexOf_3(s, find, 0);
    return _t0;
}
int64_t org_bau_String_indexOf_3(i8_array* s, i8_array* find, int64_t start) {
    if (_arrayLen(find) <= 0) {
        return -1;
    }
    if (_arrayLen(s) <= 0) {
        return -1;
    }
    int64_t i = 0;
    if (start < 0) {
        return -1;
    }
    if (start >= _arrayLen(s)) {
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
            if (nj >= _arrayLen(find)) {
                return i;
            }
            j = nj;
            int64_t nx = x + 1;
            if (nx >= _arrayLen(s)) {
                break;
            }
            x = nx;
        }
        int64_t ni = i + 1;
        if (ni >= _arrayLen(s)) {
            break;
        }
        i = ni;
    }
    return -1;
}
org_bau_String_StringBuilder* org_bau_String_newStringBuilder_1(int64_t len) {
    org_bau_String_StringBuilder* result = org_bau_String_StringBuilder_0();
    i8_array* _t0 = i8_array_new(len);
    _incUseStack(_t0);
    _decUse(result->data, i8_array);
    result->data = _t0;
    _decUseStack(_t0, i8_array);
    return result;
}
org_bau_List_List_org_bau_String_string* org_bau_String_split_2(i8_array* s, i8_array* delimiter) {
    org_bau_List_List_org_bau_String_string* list = org_bau_List_newList_org_bau_String_string_1(0);
    int64_t next = org_bau_String_indexOf_2(s, delimiter);
    int64_t _t0 = _arrayLen(delimiter) == 0;
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
        index = next + _arrayLen(delimiter);
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
    org_bau_String_string result = org_bau_String_string_0();
    _incUseStack(s);
    _decUse(result.data, i8_array);
    result.data = s;
    return result;
}
org_bau_String_string org_bau_String_string_0() {
    org_bau_String_string _t10 = org_bau_String_string_new();
    _t10.data = i8_array_new(0);
    return _t10;
}
i8_array* org_bau_String_substring_2(i8_array* s, int64_t start) {
    _incUseStack(s);
    i8_array* _t0 = org_bau_String_substring_3(s, start, _arrayLen(s));
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
        int64_t _t3 = end > _arrayLen(s);
        _t2 = _t3;
    }
    if (_t2) {
        _decUseStack(s, i8_array);
        return string_1014;
    }
    if (_arrayLen(s) <= 0) {
        _decUseStack(s, i8_array);
        return string_1014;
    }
    int64_t i = 0;
    if (start >= _arrayLen(s)) {
        _decUseStack(s, i8_array);
        return string_1014;
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
org_bau_db_Sqlite3_Query_Customer* org_bau_db_Sqlite3_Query_Customer_4(org_bau_db_Sqlite3_Sqlite* db, org_bau_db_Sqlite3_text tableName, org_bau_db_Sqlite3_text condition, org_bau_db_Sqlite3_text orderBy) {
    org_bau_db_Sqlite3_Query_Customer* _t37 = org_bau_db_Sqlite3_Query_Customer_new();
    _incUseStack(db);
    _t37->db = db;
    org_bau_db_Sqlite3_text_copy(&tableName);
    _t37->tableName = tableName;
    org_bau_db_Sqlite3_text_copy(&condition);
    _t37->condition = condition;
    org_bau_db_Sqlite3_text_copy(&orderBy);
    _t37->orderBy = orderBy;
    return _t37;
}
org_bau_db_Sqlite3_Record* org_bau_db_Sqlite3_Record_0() {
    org_bau_db_Sqlite3_Record* _t18 = org_bau_db_Sqlite3_Record_new();
    _t18->data = i8_array_new(0);
    return _t18;
}
org_bau_db_Sqlite3_Sqlite* org_bau_db_Sqlite3_Sqlite_0() {
    org_bau_db_Sqlite3_Sqlite* _t16 = org_bau_db_Sqlite3_Sqlite_new();
    _t16->name = i8_array_new(0);
    _t16->error = i8_array_new(0);
    _t16->db = 0;
    _t16->stmt = 0;
    return _t16;
}
void org_bau_db_Sqlite3_cleanIt_1(i8_array* expr) {
    while (1) {
        int64_t p = org_bau_String_indexOf_2(expr, string_1018);
        if (p < 0) {
            break;
        }
        expr->data[idx_2(p, _arrayLen(expr))] = 32;
        expr->data[idx_2(p + 1, _arrayLen(expr))] = 32;
        expr->data[idx_2(p + 2, _arrayLen(expr))] = 32;
    }
}
org_bau_db_Sqlite3_text org_bau_db_Sqlite3_convertI8ArrayToText_1(i8_array* t) {
    org_bau_db_Sqlite3_text result = org_bau_db_Sqlite3_text_0();
    _incUseStack(t);
    _decUse(result.data, i8_array);
    result.data = t;
    return result;
}
i8_array* org_bau_db_Sqlite3_convertTextToI8Array_1(org_bau_db_Sqlite3_text t) {
    _incUseStack(t.data);
    i8_array* _r0 = t.data;
    return _r0;
}
int64_t org_bau_db_Sqlite3_convertTextToInt_1(org_bau_db_Sqlite3_text t) {
    int64_t _t0 = org_bau_Int_parseInt_1(t.data);
    return _t0;
}
i8_array* org_bau_db_Sqlite3_getValue_Customer_2(Customer* row, int64_t c) {
    if (c == 0) {
        int64_t _r0 = row->id;
        return org_bau_Std_convertIntToI8Array_1(_r0);
    } else {
        if (c == 1) {
            org_bau_db_Sqlite3_text_copy(&row->name);
            org_bau_db_Sqlite3_text _r1 = row->name;
            return org_bau_db_Sqlite3_convertTextToI8Array_1(_r1);
        } else {
            if (c == 2) {
                int64_t _r2 = row->id;
                return org_bau_Std_convertIntToI8Array_1(_r2);
            }
        }
    }
    i8_array* _t27 = i8_array_new(0);
    return _t27;
}
org_bau_db_Sqlite3_Sqlite* org_bau_db_Sqlite3_open_1(i8_array* name) {
    org_bau_db_Sqlite3_Sqlite* result = org_bau_db_Sqlite3_Sqlite_0();
    _incUseStack(name);
    _decUse(result->name, i8_array);
    result->name = name;
    char n[256];
            for (int i = 0; i < 256; i++) {
                n[i] = 0;
                if (i < name->len) {
                    n[i] = name->data[i];
                }
            }
            sqlite3 *db;
            char *errmsg = NULL;
            if (sqlite3_open(n, &db) != SQLITE_OK) {
                // TODO sqlite3_errmsg(db);
            } else {
                result->db = (uint64_t) db;
            }
    return result;
}
i8_array* org_bau_db_Sqlite3_quote_1(i8_array* s) {
    _incUseStack(s);
    i8_array* _t0 = i8_array_new(2 + ( _arrayLen(s) * 2 ));
    _incUseStack(_t0);
    i8_array* result = _t0;
    result->data[idx_2(0, _arrayLen(result))] = 39;
    int64_t pos = 1;
    if (_arrayLen(s) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                result->data[idx_2(pos, _arrayLen(result))] = s->data[i];
                pos = pos + 1;
                if (s->data[i] == 39) {
                    result->data[idx_2(pos, _arrayLen(result))] = s->data[i];
                    pos = pos + 1;
                }
                int64_t _next = i + 1;
                if (_next >= _arrayLen(s)) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    result->data[idx_2(pos, _arrayLen(result))] = 39;
    pos = pos + 1;
    i8_array* _t1 = i8_array_new(pos);
    _incUseStack(_t1);
    i8_array* copy = _t1;
    if (pos > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                copy->data[i] = result->data[idx_2(i, _arrayLen(result))];
                int64_t _next = i + 1;
                if (_next >= pos) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    _decUseStack(_t1, i8_array);
    _decUseStack(result, i8_array);
    _decUseStack(_t0, i8_array);
    _decUseStack(s, i8_array);
    return copy;
}
void org_bau_db_Sqlite3_setField_Customer_3(Customer* row, int64_t c, org_bau_db_Sqlite3_text n) {
    if (c == 0) {
        row->id = org_bau_db_Sqlite3_convertTextToInt_1(n);
    } else {
        if (c == 1) {
            org_bau_db_Sqlite3_text_copy(&n);
            row->name = n;
        } else {
            if (c == 2) {
                row->id = org_bau_db_Sqlite3_convertTextToInt_1(n);
            }
        }
    }
}
org_bau_db_Sqlite3_text org_bau_db_Sqlite3_text_0() {
    org_bau_db_Sqlite3_text _t17 = org_bau_db_Sqlite3_text_new();
    _t17.data = i8_array_new(0);
    return _t17;
}
org_bau_List_List_Customer* org_bau_db_Sqlite3_Query_Customer_select_1(org_bau_db_Sqlite3_Query_Customer* this) {
    org_bau_String_StringBuilder* buff = org_bau_String_newStringBuilder_1(16);
    org_bau_String_StringBuilder_append_2(buff, string_1035);
    i8_array* _t42 = org_bau_db_Sqlite3_convertTextToI8Array_1(this->tableName);
    org_bau_String_StringBuilder_append_2(buff, _t42);
    if (_arrayLen(this->condition.data)) {
        org_bau_db_Sqlite3_cleanIt_1(this->condition.data);
        org_bau_String_StringBuilder_append_2(buff, string_1036);
        org_bau_String_StringBuilder_append_2(buff, this->condition.data);
    }
    if (_arrayLen(this->orderBy.data)) {
        org_bau_db_Sqlite3_cleanIt_1(this->orderBy.data);
        org_bau_String_StringBuilder_append_2(buff, string_1037);
        org_bau_String_StringBuilder_append_2(buff, this->orderBy.data);
    }
    org_bau_String_string _t43 = org_bau_String_StringBuilder_toString_1(buff);
    _incUseStack(_t43.data);
    i8_array* sql = _t43.data;
    org_bau_List_List_Customer* result = org_bau_List_newList_Customer_1(0);
    if (this->db != NULL) {
        _incUseStack(string_1026);
        i8_array* fieldNames = string_1026;
        int64_t _t45 = org_bau_db_Sqlite3_Sqlite_prepare_2(this->db, sql);
        ;
        int64_t cols = 2;
        while (1 == 1) {
            int64_t _t46 = org_bau_db_Sqlite3_Sqlite_nextRow_1(this->db);
            if (!(_t46)) {
                break;
            }
            Customer* _t47 = NULL;
            if (1) {
                Customer* _u_r = convertRecordToCustomer_1(org_bau_db_Sqlite3_Record_0());
                _incUseStack(_u_r);
                _decUseStack(_t47, Customer);
                _t47 = _u_r;
            } else {
            }
            _incUseStack(_t47);
            Customer* row = _t47;
            int64_t c = 0;
            while (c < 2) {
                if (this->db) {
                    org_bau_db_Sqlite3_text n = org_bau_db_Sqlite3_text_0();
                    i8_array* _t48 = org_bau_db_Sqlite3_Sqlite_getString_2(this->db, c);
                    _incUseStack(_t48);
                    _decUse(n.data, i8_array);
                    n.data = _t48;
                    org_bau_db_Sqlite3_setField_Customer_3(row, c, n);
                    _decUseStack(_t48, i8_array);
                    org_bau_db_Sqlite3_text_free(&n);
                }
                c = c + 1;
            }
            org_bau_List_List_Customer_add_2(result, row);
            _decUseStack(row, Customer);
            _decUseStack(_t47, Customer);
        }
        _decUseStack(fieldNames, i8_array);
    }
    _decUseStack(sql, i8_array);
    org_bau_String_string_free(&_t43);
    _decUseStack(_t42, i8_array);
    _decUseStack(buff, org_bau_String_StringBuilder);
    return result;
}
void org_bau_db_Sqlite3_Sqlite_close_1(org_bau_db_Sqlite3_Sqlite* this) {
    if (this->db == 0) {
        return;
    }
    sqlite3 *db = (sqlite3*) (this->db);
        sqlite3_close(db);
    this->db = 0;
}
void org_bau_db_Sqlite3_Sqlite_createTable_Customer_2(org_bau_db_Sqlite3_Sqlite* this, int64_t _T) {
    org_bau_String_StringBuilder* buff = org_bau_String_newStringBuilder_1(16);
    org_bau_String_StringBuilder_append_2(buff, string_1024);
    org_bau_String_StringBuilder_append_2(buff, string_1023);
    org_bau_String_StringBuilder_append_2(buff, string_1025);
    int64_t len = 2;
    _incUseStack(string_1026);
    i8_array* names = string_1026;
    org_bau_List_List_org_bau_String_string* nameList = org_bau_String_split_2(names, string_1008);
    _incUseStack(string_1027);
    i8_array* types = string_1027;
    org_bau_List_List_org_bau_String_string* typeList = org_bau_String_split_2(types, string_1008);
    if (2 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                if (i > 0) {
                    org_bau_String_StringBuilder_append_2(buff, string_1028);
                }
                org_bau_String_string _t21 = org_bau_List_List_org_bau_String_string_get_2(nameList, i);
                org_bau_String_StringBuilder_append_2(buff, _t21.data);
                org_bau_String_StringBuilder_append_2(buff, string_1019);
                org_bau_String_string _t22 = org_bau_List_List_org_bau_String_string_get_2(typeList, i);
                _incUseStack(_t22.data);
                i8_array* t = _t22.data;
                org_bau_List_List_org_bau_String_string* tn = org_bau_String_split_2(t, string_1005);
                int64_t _t23 = org_bau_List_List_org_bau_String_string_len_1(tn);
                org_bau_String_string _t24 = org_bau_List_List_org_bau_String_string_get_2(tn, _t23 - 1);
                org_bau_String_StringBuilder_append_2(buff, _t24.data);
                int64_t _next = i + 1;
                if (_next >= 2) {
                    org_bau_String_string_free(&_t24);
                    _decUseStack(tn, org_bau_List_List_org_bau_String_string);
                    _decUseStack(t, i8_array);
                    org_bau_String_string_free(&_t22);
                    org_bau_String_string_free(&_t21);
                    break;
                }
                i = _next;
                org_bau_String_string_free(&_t24);
                _decUseStack(tn, org_bau_List_List_org_bau_String_string);
                _decUseStack(t, i8_array);
                org_bau_String_string_free(&_t22);
                org_bau_String_string_free(&_t21);
            }
            break;
        }
    }
    org_bau_String_StringBuilder_append_2(buff, string_1029);
    org_bau_String_string _t25 = org_bau_String_StringBuilder_toString_1(buff);
    _incUseStack(_t25.data);
    i8_array* sql = _t25.data;
    org_bau_db_Sqlite3_Sqlite_execute_2(this, sql);
    _decUseStack(sql, i8_array);
    org_bau_String_string_free(&_t25);
    _decUseStack(typeList, org_bau_List_List_org_bau_String_string);
    _decUseStack(types, i8_array);
    _decUseStack(nameList, org_bau_List_List_org_bau_String_string);
    _decUseStack(names, i8_array);
    _decUseStack(buff, org_bau_String_StringBuilder);
}
void org_bau_db_Sqlite3_Sqlite_dropTable_Customer_2(org_bau_db_Sqlite3_Sqlite* this, int64_t _T) {
    org_bau_String_StringBuilder* buff = org_bau_String_newStringBuilder_1(16);
    org_bau_String_StringBuilder_append_2(buff, string_1022);
    org_bau_String_StringBuilder_append_2(buff, string_1023);
    org_bau_String_string _t20 = org_bau_String_StringBuilder_toString_1(buff);
    _incUseStack(_t20.data);
    i8_array* sql = _t20.data;
    org_bau_db_Sqlite3_Sqlite_execute_2(this, sql);
    _decUseStack(sql, i8_array);
    org_bau_String_string_free(&_t20);
    _decUseStack(buff, org_bau_String_StringBuilder);
}
void org_bau_db_Sqlite3_Sqlite_execute_2(org_bau_db_Sqlite3_Sqlite* this, i8_array* sql) {
    if (this->db == 0) {
        return;
    }
    char n[256];
        for (int i = 0; i < 256; i++) {
            n[i] = 0;
            if (i < sql->len) {
                n[i] = sql->data[i];
            }
        }    
        sqlite3 *db = (sqlite3*) (this->db);
        char *errmsg = NULL;
        // ignore errors 
        sqlite3_exec(db, n, NULL, NULL, &errmsg);
}
org_bau_db_Sqlite3_Query_Customer* org_bau_db_Sqlite3_Sqlite_from_Customer_2(org_bau_db_Sqlite3_Sqlite* this, int64_t _T) {
    org_bau_db_Sqlite3_text _t49 = org_bau_db_Sqlite3_text_0();
    org_bau_db_Sqlite3_text _t50 = org_bau_db_Sqlite3_text_0();
    org_bau_db_Sqlite3_text _t51 = org_bau_db_Sqlite3_text_0();
    org_bau_db_Sqlite3_Query_Customer* result = org_bau_db_Sqlite3_Query_Customer_4(this, _t49, _t50, _t51);
    result->tableName = org_bau_db_Sqlite3_convertI8ArrayToText_1(string_1023);
    org_bau_db_Sqlite3_text_free(&_t51);
    org_bau_db_Sqlite3_text_free(&_t50);
    org_bau_db_Sqlite3_text_free(&_t49);
    return result;
}
i8_array* org_bau_db_Sqlite3_Sqlite_getString_2(org_bau_db_Sqlite3_Sqlite* this, int64_t column) {
    int64_t _t0 = this->db == 0;
    if (!(_t0)) {
        int64_t _t1 = this->stmt == 0;
        _t0 = _t1;
    }
    if (_t0) {
        i8_array* _t2 = i8_array_new(0);
        return _t2;
    }
    i8_array* _t3 = i8_array_new(256);
    _incUseStack(_t3);
    i8_array* result = _t3;
    int64_t len = 0;
    sqlite3_stmt *stmt = (sqlite3_stmt*) (this->stmt);
        const unsigned char *text = sqlite3_column_text(stmt, column);
        if (text) {
            for (int i = 0; i < 255; i++) {
                if (text[i] == 0) {
                    break;
                }
                result->data[i] = text[i];
                len = i + 1;
            }
        }
    i8_array* _t4 = i8_array_new(len);
    _incUseStack(_t4);
    i8_array* result2 = _t4;
    if (len > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                result2->data[i] = result->data[idx_2(i, _arrayLen(result))];
                int64_t _next = i + 1;
                if (_next >= len) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    _decUseStack(_t4, i8_array);
    _decUseStack(result, i8_array);
    _decUseStack(_t3, i8_array);
    return result2;
}
void org_bau_db_Sqlite3_Sqlite_insert_Customer_2(org_bau_db_Sqlite3_Sqlite* this, Customer* x) {
    org_bau_String_StringBuilder* buff = org_bau_String_newStringBuilder_1(16);
    org_bau_String_StringBuilder_append_2(buff, string_1031);
    org_bau_String_StringBuilder_append_2(buff, string_1023);
    org_bau_String_StringBuilder_append_2(buff, string_1025);
    int64_t len = 2;
    _incUseStack(string_1026);
    i8_array* names = string_1026;
    org_bau_List_List_org_bau_String_string* nameList = org_bau_String_split_2(names, string_1008);
    if (2 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                if (i > 0) {
                    org_bau_String_StringBuilder_append_2(buff, string_1028);
                }
                org_bau_String_string _t26 = org_bau_List_List_org_bau_String_string_get_2(nameList, i);
                org_bau_String_StringBuilder_append_2(buff, _t26.data);
                int64_t _next = i + 1;
                if (_next >= 2) {
                    org_bau_String_string_free(&_t26);
                    break;
                }
                i = _next;
                org_bau_String_string_free(&_t26);
            }
            break;
        }
    }
    org_bau_String_StringBuilder_append_2(buff, string_1032);
    if (2 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                if (i > 0) {
                    org_bau_String_StringBuilder_append_2(buff, string_1028);
                }
                i8_array* _t28 = org_bau_db_Sqlite3_getValue_Customer_2(x, i);
                i8_array* _t29 = org_bau_db_Sqlite3_quote_1(_t28);
                org_bau_String_StringBuilder_append_2(buff, _t29);
                int64_t _next = i + 1;
                if (_next >= 2) {
                    _decUseStack(_t29, i8_array);
                    _decUseStack(_t28, i8_array);
                    break;
                }
                i = _next;
                _decUseStack(_t29, i8_array);
                _decUseStack(_t28, i8_array);
            }
            break;
        }
    }
    org_bau_String_StringBuilder_append_2(buff, string_1029);
    org_bau_String_string _t30 = org_bau_String_StringBuilder_toString_1(buff);
    _incUseStack(_t30.data);
    i8_array* sql = _t30.data;
    org_bau_db_Sqlite3_Sqlite_execute_2(this, sql);
    _decUseStack(sql, i8_array);
    org_bau_String_string_free(&_t30);
    _decUseStack(nameList, org_bau_List_List_org_bau_String_string);
    _decUseStack(names, i8_array);
    _decUseStack(buff, org_bau_String_StringBuilder);
}
int64_t org_bau_db_Sqlite3_Sqlite_nextRow_1(org_bau_db_Sqlite3_Sqlite* this) {
    int64_t _t0 = this->db == 0;
    if (!(_t0)) {
        int64_t _t1 = this->stmt == 0;
        _t0 = _t1;
    }
    if (_t0) {
        return 0;
    }
    int64_t result = 0;
    sqlite3_stmt *stmt = (sqlite3_stmt*) (this->stmt);
        if (sqlite3_step(stmt) == SQLITE_ROW) {
            result = 1;
        }
    return result;
}
int64_t org_bau_db_Sqlite3_Sqlite_prepare_2(org_bau_db_Sqlite3_Sqlite* this, i8_array* sql) {
    if (this->db == 0) {
        return 0;
    }
    int64_t result = 0;
    sqlite3 *db = (sqlite3*) (this->db);
        char *errmsg = NULL;
        sqlite3_stmt *stmt;
        // ignore errors
        char n[256];
        for (int i = 0; i < 256; i++) {
            n[i] = 0;
            if (i < sql->len) {
                n[i] = sql->data[i];
            }
        }
        result = sqlite3_prepare_v2(db, n, -1, &stmt, NULL);
        if (result == SQLITE_OK) {
            this->stmt = (uint64_t) stmt;
        }
    return result;
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1005 = str_const(".", 1);
    string_1008 = str_const(",", 1);
    string_1014 = str_const("", 0);
    string_1018 = str_const("it.", 3);
    string_1019 = str_const(" ", 1);
    string_1021 = str_const("demo.db", 7);
    string_1022 = str_const("drop table if exists ", 21);
    string_1023 = str_const("Customer", 8);
    string_1024 = str_const("create table if not exists ", 27);
    string_1025 = str_const("(", 1);
    string_1026 = str_const("id,name", 7);
    string_1027 = str_const("int,org.bau.db.Sqlite3.text", 27);
    string_1028 = str_const(", ", 2);
    string_1029 = str_const(")", 1);
    string_1030 = str_const("James", 5);
    string_1031 = str_const("insert into ", 12);
    string_1032 = str_const(") values(", 9);
    string_1033 = str_const("Sarah", 5);
    string_1034 = str_const("David", 5);
    string_1035 = str_const("select * from ", 14);
    string_1036 = str_const(" where ", 7);
    string_1037 = str_const(" order by ", 10);
    string_1041 = str_const("it.id > 0", 9);
    string_1043 = str_const("it.name", 7);
    string_1045 = str_const("id: ", 4);
    string_1046 = str_const("; name: ", 8);
    _main();
    return 0;
}
void _main() {
    randomSeed = 0;
    MIN_INT = 0x8000000000000000;
    MAX_INT = 0x7fffffffffffffff;
    MIN_I32 = -2147483648;
    MAX_I32 = 4294967295;
    org_bau_db_Sqlite3_Sqlite* db = org_bau_db_Sqlite3_open_1(string_1021);
    if (db != NULL) {
        org_bau_db_Sqlite3_Sqlite_dropTable_Customer_2(db, 0);
        org_bau_db_Sqlite3_Sqlite_createTable_Customer_2(db, 0);
        org_bau_db_Sqlite3_text _t31 = org_bau_db_Sqlite3_convertI8ArrayToText_1(string_1030);
        Customer* _t32 = newCustomer_2(0, _t31);
        org_bau_db_Sqlite3_Sqlite_insert_Customer_2(db, _t32);
        org_bau_db_Sqlite3_text _t33 = org_bau_db_Sqlite3_convertI8ArrayToText_1(string_1033);
        Customer* _t34 = newCustomer_2(1, _t33);
        org_bau_db_Sqlite3_Sqlite_insert_Customer_2(db, _t34);
        org_bau_db_Sqlite3_text _t35 = org_bau_db_Sqlite3_convertI8ArrayToText_1(string_1034);
        Customer* _t36 = newCustomer_2(2, _t35);
        org_bau_db_Sqlite3_Sqlite_insert_Customer_2(db, _t36);
        org_bau_db_Sqlite3_Query_Customer* _t54 = NULL;
        if (1) {
            org_bau_db_Sqlite3_Query_Customer* _t53 = NULL;
            if (1) {
                org_bau_db_Sqlite3_Query_Customer* this = org_bau_db_Sqlite3_Sqlite_from_Customer_2(db, 0);
                org_bau_db_Sqlite3_text _0t38 = org_bau_db_Sqlite3_text_0();
                org_bau_db_Sqlite3_text_copy(&_0t38);
                this->condition = _0t38;
                _incUseStack(string_1041);
                _decUse(this->condition.data, i8_array);
                this->condition.data = string_1041;
                _incUseStack(this);
                _decUseStack(_t53, org_bau_db_Sqlite3_Query_Customer);
                _t53 = this;
            } else {
            }
            _incUseStack(_t53);
            org_bau_db_Sqlite3_Query_Customer* this = _t53;
            org_bau_db_Sqlite3_text _0t52 = org_bau_db_Sqlite3_text_0();
            org_bau_db_Sqlite3_text_copy(&_0t52);
            this->orderBy = _0t52;
            _incUseStack(string_1043);
            _decUse(this->orderBy.data, i8_array);
            this->orderBy.data = string_1043;
            _incUseStack(this);
            _decUseStack(_t54, org_bau_db_Sqlite3_Query_Customer);
            _t54 = this;
        } else {
        }
        org_bau_List_List_Customer* list = org_bau_db_Sqlite3_Query_Customer_select_1(_t54);
        if (list->size > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    Customer* u = org_bau_List_List_Customer_get_2(list, i);
                    if (u == NULL) {
                        _decUseStack(u, Customer);
                        break;
                    }
                    printf("id: %lld; name: %.*s\n", (long long)u->id, _arrayLen(u->name.data), u->name.data->data);
                    int64_t _next = i + 1;
                    if (_next >= list->size) {
                        _decUseStack(u, Customer);
                        break;
                    }
                    i = _next;
                    _decUseStack(u, Customer);
                }
                break;
            }
        }
        _decUseStack(list, org_bau_List_List_Customer);
        _decUseStack(_t54, org_bau_db_Sqlite3_Query_Customer);
        _decUseStack(_t36, Customer);
        org_bau_db_Sqlite3_text_free(&_t35);
        _decUseStack(_t34, Customer);
        org_bau_db_Sqlite3_text_free(&_t33);
        _decUseStack(_t32, Customer);
        org_bau_db_Sqlite3_text_free(&_t31);
    }
    _decUseStack(db, org_bau_db_Sqlite3_Sqlite);
    _end();
}
/*

fun getRandomSeed() int
Get the random seed.

fun random() int
Pseudo-random number generated using the Splitmix64 algorithm.

fun random(smallerThan int) int
Pseudo-random number between 0 and smallerThan (excluding).

fun setRandomSeed(seed int)
Set the random seed.

type exception
An exception

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

fun parsePositiveInt(s i8[]) int throws exception
throws an exception if the string does not match [0-9]+

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

fun convertRecordToCustomer(r Record) Customer
helper function for de-serialization

*/
