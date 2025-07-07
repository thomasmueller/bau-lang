#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <limits.h>
#include <math.h>
#include <signal.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <termios.h>
#include <unistd.h>
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
typedef struct int_array int_array;
struct int_array;
typedef struct org_bau_File_File org_bau_File_File;
struct org_bau_File_File;
typedef struct org_bau_String_string org_bau_String_string;
struct org_bau_String_string;
typedef struct org_bau_String_string_array org_bau_String_string_array;
struct org_bau_String_string_array;
typedef struct org_bau_String_StringBuilder org_bau_String_StringBuilder;
struct org_bau_String_StringBuilder;
typedef struct org_bau_List_List_org_bau_String_string org_bau_List_List_org_bau_String_string;
struct org_bau_List_List_org_bau_String_string;
typedef struct org_bau_os_Terminal_termIos org_bau_os_Terminal_termIos;
struct org_bau_os_Terminal_termIos;
typedef struct org_bau_os_Terminal_windowSize org_bau_os_Terminal_windowSize;
struct org_bau_os_Terminal_windowSize;
typedef struct fileContent fileContent;
struct fileContent;
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
struct org_bau_File_File {
    int64_t filePointer;
    int32_t _refCount;
};
org_bau_File_File* org_bau_File_File_new() {
    org_bau_File_File* result = _malloc(sizeof(org_bau_File_File));
    _traceMalloc(result);
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
struct org_bau_os_Terminal_termIos {
    i8_array* data;
};
org_bau_os_Terminal_termIos org_bau_os_Terminal_termIos_new() {
    org_bau_os_Terminal_termIos result;
    result.data = 0;
    return result;
}
struct org_bau_os_Terminal_windowSize {
    int64_t rows;
    int64_t columns;
};
org_bau_os_Terminal_windowSize org_bau_os_Terminal_windowSize_new() {
    org_bau_os_Terminal_windowSize result;
    result.rows = 0;
    result.columns = 0;
    return result;
}
struct fileContent {
    i8_array* fileName;
    i8_array* data;
    org_bau_List_List_org_bau_String_string* lines;
    int64_t modified;
};
fileContent fileContent_new() {
    fileContent result;
    result.fileName = 0;
    result.data = 0;
    result.lines = 0;
    result.modified = 0;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
void down_0();
fileContent fileContent_3(i8_array* fileName, i8_array* data, org_bau_List_List_org_bau_String_string* lines);
void find_0();
int8_t i8_1(int64_t x);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
int64_t imod_2(int64_t a, int64_t b);
void insertByte_1(int8_t add);
void insertNewline_0();
i8_array* org_bau_Env_arg_1(int64_t index);
int64_t org_bau_Env_argCount_0();
void org_bau_Env_atExit_1(void  (*callback_0)());
void org_bau_Env_exit_1(int64_t code);
org_bau_File_File* org_bau_File_File_0();
org_bau_File_File* org_bau_File_openFile_2(i8_array* name, i8_array* mode);
void org_bau_File_File_close_1(org_bau_File_File* this);
int64_t org_bau_File_File_len_1(org_bau_File_File* this);
int64_t org_bau_File_File_read_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len);
int64_t org_bau_File_File_readFully_4(org_bau_File_File* this, i8_array* buffer, int64_t pos, int64_t len);
int64_t org_bau_File_File_write_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len);
org_bau_List_List_org_bau_String_string* org_bau_List_List_org_bau_String_string_1(org_bau_String_string_array* array);
org_bau_List_List_org_bau_String_string* org_bau_List_newList_org_bau_String_string_1(int64_t _T);
void org_bau_List_List_org_bau_String_string_add_2(org_bau_List_List_org_bau_String_string* this, org_bau_String_string x);
void org_bau_List_List_org_bau_String_string_add_3(org_bau_List_List_org_bau_String_string* this, int64_t index, org_bau_String_string x);
void org_bau_List_List_org_bau_String_string_remove_2(org_bau_List_List_org_bau_String_string* this, int64_t pos);
int64_t org_bau_Math_appendInt_3(int64_t n, i8_array* buff, int64_t pos);
i8_array* org_bau_Math_intToString_1(int64_t n);
org_bau_String_StringBuilder* org_bau_String_StringBuilder_1(i8_array* data);
int64_t org_bau_String_indexOf_2(i8_array* s, i8_array* find);
int64_t org_bau_String_indexOf_3(i8_array* s, i8_array* find, int64_t start);
org_bau_List_List_org_bau_String_string* org_bau_String_split_2(i8_array* s, i8_array* delimiter);
org_bau_String_string org_bau_String_str_1(i8_array* s);
org_bau_String_string org_bau_String_string_1(i8_array* data);
i8_array* org_bau_String_substring_2(i8_array* s, int64_t start);
i8_array* org_bau_String_substring_3(i8_array* s, int64_t start, int64_t end);
void org_bau_String_StringBuilder_append_2(org_bau_String_StringBuilder* this, i8_array* b);
void org_bau_String_StringBuilder_append_4(org_bau_String_StringBuilder* this, i8_array* b, int64_t start, int64_t end);
void org_bau_os_Signal_signal_2(int64_t signalId, void  (*callback_1)(int32_t));
void org_bau_os_Terminal_disableRawMode_0();
void org_bau_os_Terminal_doNothing_0();
void org_bau_os_Terminal_enableRawMode_1(void  (*refreshScreen_0)());
org_bau_os_Terminal_windowSize org_bau_os_Terminal_getWindowSize_0();
int64_t org_bau_os_Terminal_isTerminal_0();
int64_t org_bau_os_Terminal_readByte_0();
int64_t org_bau_os_Terminal_readEditorKey_0();
org_bau_os_Terminal_termIos org_bau_os_Terminal_termIos_1(i8_array* data);
org_bau_os_Terminal_windowSize org_bau_os_Terminal_windowSize_0();
void org_bau_os_Terminal_windowSizeChanged_1(int32_t x);
void org_bau_os_Terminal_writeToTerminal_2(i8_array* data, int64_t len);
void refreshScreen_0();
void removeByte_0();
void save_0();
void up_0();
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_File_File_free(org_bau_File_File* x);
void org_bau_String_string_free(org_bau_String_string* x);
void org_bau_String_string_copy(org_bau_String_string* x);
void org_bau_String_string_array_free(org_bau_String_string_array* x);
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x);
void org_bau_List_List_org_bau_String_string_free(org_bau_List_List_org_bau_String_string* x);
void org_bau_os_Terminal_termIos_free(org_bau_os_Terminal_termIos* x);
void org_bau_os_Terminal_termIos_copy(org_bau_os_Terminal_termIos* x);
void org_bau_os_Terminal_windowSize_free(org_bau_os_Terminal_windowSize* x);
void org_bau_os_Terminal_windowSize_copy(org_bau_os_Terminal_windowSize* x);
void fileContent_free(fileContent* x);
void fileContent_copy(fileContent* x);
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
void org_bau_os_Terminal_termIos_free(org_bau_os_Terminal_termIos* x) {
    _decUse(x->data, i8_array);
}
void org_bau_os_Terminal_termIos_copy(org_bau_os_Terminal_termIos* x) {
    _incUse(x->data);
}
void org_bau_os_Terminal_windowSize_free(org_bau_os_Terminal_windowSize* x) {
}
void org_bau_os_Terminal_windowSize_copy(org_bau_os_Terminal_windowSize* x) {
}
void fileContent_free(fileContent* x) {
    _decUse(x->fileName, i8_array);
    _decUse(x->data, i8_array);
    _decUse(x->lines, org_bau_List_List_org_bau_String_string);
}
void fileContent_copy(fileContent* x) {
    _incUse(x->fileName);
    _incUse(x->data);
    _incUse(x->lines);
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
i8_array* string_1010;
i8_array* string_1012;
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
i8_array* string_1035;
i8_array* string_1036;
i8_array* string_1037;
i8_array* string_1038;
i8_array* string_1039;
i8_array* string_1040;
i8_array* string_1041;
i8_array* string_1042;
double POS_INFINITY;
double NEG_INFINITY;
double NOT_A_NUMBER;
double PI;
double E;
double LOG10;
double LOG2;
int64_t MIN_INT;
int64_t MAX_INT;
org_bau_os_Terminal_termIos oldTermIos;
void  (*refreshScreenCallback_0)();
int64_t cursorY;
int64_t cursorX;
int64_t offsetY;
int64_t offsetX;
int64_t foundX;
int64_t foundY;
org_bau_String_string findText;
int64_t mode;
org_bau_os_Terminal_windowSize currentWindowSize;
fileContent currentFile;
void down_0() {
    if (cursorY < ( currentWindowSize.rows - 1 )) {
        cursorY += 1;
    } else if (( cursorY + offsetY ) < currentFile.lines->size) {
        offsetY += 1;
    }
}
fileContent fileContent_3(i8_array* fileName, i8_array* data, org_bau_List_List_org_bau_String_string* lines) {
    fileContent _t48 = fileContent_new();
    _incUseStack(fileName);
    _t48.fileName = fileName;
    _incUseStack(data);
    _t48.data = data;
    _incUseStack(lines);
    _t48.lines = lines;
    _t48.modified = 0;
    return _t48;
}
void find_0() {
    mode = 2;
    while (1) {
        refreshScreen_0();
        int64_t key = org_bau_os_Terminal_readEditorKey_0();
        if (key < 0) {
            break;
        }
        int64_t _t0 = key;
        if ((((_t0 == 3) || (_t0 == 17)) || (_t0 == 27)) || (_t0 == 13)) {
            break;
        } else if (((_t0 == 127) || (_t0 == 8)) || (_t0 == 1004)) {
            if (findText.data->len > 0) {
                i8_array* _t1 = i8_array_new(findText.data->len - 1);
                _incUseStack(_t1);
                i8_array* new = _t1;
                if (new->len > 0) {
                    while (1 == 1) {
                        int64_t i = 0;
                        while (1) {
                            new->data[i] = findText.data->data[idx_2(i, findText.data->len)];
                            int64_t _next = i + 1;
                            if (_next >= new->len) {
                                break;
                            }
                            i = _next;
                        }
                        break;
                    }
                }
                org_bau_String_string _t2 = org_bau_String_string_1(new);
                org_bau_String_string_copy(&_t2);
                org_bau_String_string_free(&findText);
                findText = _t2;
                org_bau_String_string_free(&_t2);
                _decUseStack(new, i8_array);
                _decUseStack(_t1, i8_array);
            }
        } else if (_t0 == 1003) {
            foundX = 10;
            foundY = 10;
        } else {
            int64_t _t3 = key >= 32;
            if (_t3) {
                int64_t _t4 = key <= 255;
                _t3 = _t4;
            }
            int64_t _t5 = _t3;
            if (_t5) {
                int64_t _t6 = findText.data->len < 100;
                _t5 = _t6;
            }
            if (_t5) {
                i8_array* _t7 = i8_array_new(findText.data->len + 1);
                _incUseStack(_t7);
                i8_array* new = _t7;
                if (findText.data->len > 0) {
                    while (1 == 1) {
                        int64_t i = 0;
                        while (1) {
                            new->data[idx_2(i, new->len)] = findText.data->data[i];
                            int64_t _next = i + 1;
                            if (_next >= findText.data->len) {
                                break;
                            }
                            i = _next;
                        }
                        break;
                    }
                }
                new->data[idx_2(new->len - 1, new->len)] = key;
                org_bau_String_string _t8 = org_bau_String_string_1(new);
                org_bau_String_string_copy(&_t8);
                org_bau_String_string_free(&findText);
                findText = _t8;
                org_bau_String_string_free(&_t8);
                _decUseStack(new, i8_array);
                _decUseStack(_t7, i8_array);
            }
        }
    }
    mode = 0;
}
int8_t i8_1(int64_t x) {
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
void insertByte_1(int8_t add) {
    currentFile.modified = 1;
    int64_t y = ( cursorY + offsetY ) - 2;
    _incUseStack(currentFile.lines);
    org_bau_List_List_org_bau_String_string* lines = currentFile.lines;
    org_bau_String_string_copy(&lines->array->data[idx_2(y, lines->array->len)]);
    org_bau_String_string line = lines->array->data[idx_2(y, lines->array->len)];
    int64_t x = ( cursorX + offsetX ) - 1;
    if (line.data->len <= x) {
        i8_array* _t0 = i8_array_new(x + 1);
        _incUseStack(_t0);
        i8_array* new = _t0;
        if (x > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    new->data[idx_2(i, new->len)] = 32;
                    int64_t _next = i + 1;
                    if (_next >= x) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        if (line.data->len > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    new->data[idx_2(i, new->len)] = line.data->data[i];
                    int64_t _next = i + 1;
                    if (_next >= line.data->len) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        new->data[idx_2(x, new->len)] = add;
        org_bau_String_string _t1 = org_bau_String_string_1(new);
        org_bau_String_string_copy(&_t1);
        lines->array->data[idx_2(y, lines->array->len)] = _t1;
        org_bau_String_string_free(&_t1);
        _decUseStack(new, i8_array);
        _decUseStack(_t0, i8_array);
    } else {
        i8_array* _t2 = i8_array_new(line.data->len + 1);
        _incUseStack(_t2);
        i8_array* new = _t2;
        if (line.data->len > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    new->data[idx_2(i + 1, new->len)] = line.data->data[i];
                    int64_t _next = i + 1;
                    if (_next >= line.data->len) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        if (x > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    new->data[idx_2(i, new->len)] = line.data->data[idx_2(i, line.data->len)];
                    int64_t _next = i + 1;
                    if (_next >= x) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        new->data[idx_2(x, new->len)] = add;
        org_bau_String_string _t3 = org_bau_String_string_1(new);
        org_bau_String_string_copy(&_t3);
        lines->array->data[idx_2(y, lines->array->len)] = _t3;
        org_bau_String_string_free(&_t3);
        _decUseStack(new, i8_array);
        _decUseStack(_t2, i8_array);
    }
    org_bau_String_string_free(&line);
    _decUseStack(lines, org_bau_List_List_org_bau_String_string);
}
void insertNewline_0() {
    currentFile.modified = 1;
    int64_t y = ( cursorY + offsetY ) - 2;
    _incUseStack(currentFile.lines);
    org_bau_List_List_org_bau_String_string* lines = currentFile.lines;
    org_bau_String_string_copy(&lines->array->data[idx_2(y, lines->array->len)]);
    org_bau_String_string line = lines->array->data[idx_2(y, lines->array->len)];
    int64_t x = ( cursorX + offsetX ) - 1;
    if (x >= line.data->len) {
        x = line.data->len;
    }
    i8_array* _t0 = org_bau_String_substring_3(line.data, 0, x);
    org_bau_String_string line1 = org_bau_String_string_1(_t0);
    i8_array* _t1 = org_bau_String_substring_3(line.data, x, line.data->len);
    org_bau_String_string line2 = org_bau_String_string_1(_t1);
    org_bau_List_List_org_bau_String_string_add_3(lines, y, line1);
    org_bau_String_string_copy(&line2);
    lines->array->data[idx_2(y + 1, lines->array->len)] = line2;
    int64_t indent = 0;
    while (1 == 1) {
        int64_t _t2 = line1.data->len > indent;
        if (_t2) {
            int64_t _t3 = line1.data->data[idx_2(indent, line1.data->len)] == 32;
            _t2 = _t3;
        }
        if (!(_t2)) {
            break;
        }
        indent += 1;
    }
    cursorX = 1 + indent;
    if (cursorY < ( currentWindowSize.rows - 1 )) {
        cursorY += 1;
    } else if (( cursorY + offsetY ) < currentFile.lines->size) {
        offsetY += 1;
    }
    org_bau_String_string_free(&line2);
    _decUseStack(_t1, i8_array);
    org_bau_String_string_free(&line1);
    _decUseStack(_t0, i8_array);
    org_bau_String_string_free(&line);
    _decUseStack(lines, org_bau_List_List_org_bau_String_string);
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
void org_bau_Env_atExit_1(void  (*callback_0)()) {
    atexit(callback_0);
}
void org_bau_Env_exit_1(int64_t code) {
    exit(code);
    printf("Exit code %lld; will now throw an array out-of-bounds exception\n", (long long)code);
    int_array* _t0 = int_array_new(0);
    _incUseStack(_t0);
    int_array* x = _t0;
    x->data[idx_2(0, x->len)] = 1;
    _decUseStack(x, int_array);
    _decUseStack(_t0, int_array);
}
org_bau_File_File* org_bau_File_File_0() {
    org_bau_File_File* _t0 = org_bau_File_File_new();
    _t0->filePointer = 0;
    return _t0;
}
org_bau_File_File* org_bau_File_openFile_2(i8_array* name, i8_array* mode) {
    // TODO verify strings
    FILE* fp = fopen((char*) name->data, (char*) mode->data);
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
int64_t org_bau_File_File_readFully_4(org_bau_File_File* this, i8_array* buffer, int64_t pos, int64_t len) {
    int64_t count = 0;
    while (1) {
        int64_t l = org_bau_File_File_read_4(this, buffer, count + pos, len - count);
        if (l == 0) {
            return count;
        } else {
            count += l;
        }
    }
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
org_bau_List_List_org_bau_String_string* org_bau_List_List_org_bau_String_string_1(org_bau_String_string_array* array) {
    org_bau_List_List_org_bau_String_string* _t43 = org_bau_List_List_org_bau_String_string_new();
    _incUseStack(array);
    _t43->array = array;
    _t43->size = 0;
    return _t43;
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
void org_bau_List_List_org_bau_String_string_add_3(org_bau_List_List_org_bau_String_string* this, int64_t index, org_bau_String_string x) {
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
    int64_t p = this->size;
    while (p > index) {
        org_bau_String_string_copy(&this->array->data[idx_2(p - 1, this->array->len)]);
        this->array->data[idx_2(p, this->array->len)] = this->array->data[idx_2(p - 1, this->array->len)];
        p -= 1;
    }
    org_bau_String_string_copy(&x);
    this->array->data[idx_2(index, this->array->len)] = x;
    this->size += 1;
}
void org_bau_List_List_org_bau_String_string_remove_2(org_bau_List_List_org_bau_String_string* this, int64_t pos) {
    while (pos < ( this->size - 1 )) {
        org_bau_String_string_copy(&this->array->data[idx_2(pos + 1, this->array->len)]);
        this->array->data[idx_2(pos, this->array->len)] = this->array->data[idx_2(pos + 1, this->array->len)];
        pos += 1;
    }
    this->array->data[idx_2(pos, this->array->len)] = org_bau_String_string_new();
}
int64_t org_bau_Math_appendInt_3(int64_t n, i8_array* buff, int64_t pos) {
    if (n < 0) {
        buff->data[idx_2(pos, buff->len)] = 45;
        pos += 1;
    } else {
        n = - n;
    }
    int64_t start = pos;
    while (1) {
        buff->data[idx_2(pos, buff->len)] = 48 - (imod_2(n, 10));
        pos += 1;
        n = idiv_2(n, 10);
        if (n == 0) {
            break;
        }
    }
    int64_t end = pos;
    while (pos > start) {
        pos -= 1;
        int8_t temp = buff->data[idx_2(pos, buff->len)];
        buff->data[idx_2(pos, buff->len)] = buff->data[idx_2(start, buff->len)];
        buff->data[idx_2(start, buff->len)] = temp;
        start += 1;
    }
    return end;
}
i8_array* org_bau_Math_intToString_1(int64_t n) {
    i8_array* _t0 = i8_array_new(20);
    _incUseStack(_t0);
    i8_array* buff = _t0;
    int64_t pos = org_bau_Math_appendInt_3(n, buff, 0);
    i8_array* _t1 = i8_array_new(pos);
    _incUseStack(_t1);
    i8_array* result = _t1;
    if (pos > 0) {
        while (1 == 1) {
            int64_t j = 0;
            while (1) {
                result->data[idx_2(j, result->len)] = buff->data[j];
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
org_bau_String_StringBuilder* org_bau_String_StringBuilder_1(i8_array* data) {
    org_bau_String_StringBuilder* _t41 = org_bau_String_StringBuilder_new();
    _incUseStack(data);
    _t41->data = data;
    _t41->len = 0;
    return _t41;
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
    org_bau_String_string _t40 = org_bau_String_string_new();
    _incUseStack(data);
    _t40.data = data;
    return _t40;
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
        return string_1010;
    }
    if (s->len <= 0) {
        _decUseStack(s, i8_array);
        return string_1010;
    }
    int64_t i = 0;
    if (start >= s->len) {
        _decUseStack(s, i8_array);
        return string_1010;
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
void org_bau_os_Signal_signal_2(int64_t signalId, void  (*callback_1)(int32_t)) {
    signal(signalId, callback_1);
}
void org_bau_os_Terminal_disableRawMode_0() {
    struct termios old;
            memcpy(&old, oldTermIos.data, sizeof(struct termios));
            tcsetattr(STDIN_FILENO, TCSAFLUSH, &old);
}
void org_bau_os_Terminal_doNothing_0() {
}
void org_bau_os_Terminal_enableRawMode_1(void  (*refreshScreen_0)()) {
    refreshScreenCallback_0 = refreshScreen_0;
    org_bau_Env_atExit_1(org_bau_os_Terminal_disableRawMode_0);
    int64_t size = 0;
    size = sizeof(struct termios);
    i8_array* _t0 = i8_array_new(size);
    org_bau_os_Terminal_termIos _t1 = org_bau_os_Terminal_termIos_1(_t0);
    org_bau_os_Terminal_termIos_copy(&_t1);
    org_bau_os_Terminal_termIos_free(&oldTermIos);
    oldTermIos = _t1;
    struct termios old;
            if (tcgetattr(0, &old) == -1) return;
            memcpy(oldTermIos.data, &old, size);
            struct termios raw;
            raw = old;
            raw.c_iflag &= ~(BRKINT | ICRNL | INPCK | ISTRIP | IXON);
            raw.c_oflag &= ~(OPOST);
            raw.c_cflag |= (CS8);
            raw.c_lflag &= ~(ECHO | ICANON | IEXTEN | ISIG);
            raw.c_cc[VMIN] = 0;
            raw.c_cc[VTIME] = 1;
            if (tcsetattr(0, TCSAFLUSH, &raw) < 0) return;
    org_bau_os_Signal_signal_2(28, org_bau_os_Terminal_windowSizeChanged_1);
    org_bau_os_Terminal_windowSizeChanged_1(0);
    org_bau_os_Terminal_termIos_free(&_t1);
    _decUseStack(_t0, i8_array);
}
org_bau_os_Terminal_windowSize org_bau_os_Terminal_getWindowSize_0() {
    org_bau_os_Terminal_windowSize size = org_bau_os_Terminal_windowSize_0();
    struct winsize ws;
            ioctl(1, TIOCGWINSZ, &ws);
            size.rows = ws.ws_row;
            size.columns = ws.ws_col;
    return size;
}
int64_t org_bau_os_Terminal_isTerminal_0() {
    return isatty(0);
    return 0;
}
int64_t org_bau_os_Terminal_readByte_0() {
    int8_t x = 0;
    int size = read(0, &x, 1);
            if (!size)
                return 0;
            return x;
    return 0;
}
int64_t org_bau_os_Terminal_readEditorKey_0() {
    while (1) {
        int64_t key = org_bau_os_Terminal_readByte_0();
        if (key == 0) {
            continue;
        }
        if (key < 0) {
            org_bau_Env_exit_1(1);
        }
        if (key != 27) {
            return key;
        }
        int64_t e0 = org_bau_os_Terminal_readByte_0();
        if (e0 == 0) {
            return key;
        }
        int64_t e1 = org_bau_os_Terminal_readByte_0();
        if (e1 == 0) {
            return key;
        }
        if (e0 == 79) {
            if (e1 == 72) {
                return 1005;
            } else if (e1 == 70) {
                return 1006;
            }
            return key;
        }
        if (e0 != 91) {
            return key;
        }
        int64_t _t0 = e1 >= 48;
        if (_t0) {
            int64_t _t1 = e1 <= 57;
            _t0 = _t1;
        }
        if (_t0) {
            int64_t e2 = org_bau_os_Terminal_readByte_0();
            if (e2 != 126) {
                return key;
            }
            int64_t _t2 = e1;
            if (_t2 == 51) {
                return 1004;
            } else if (_t2 == 53) {
                return 1007;
            } else if (_t2 == 54) {
                return 1008;
            }
            return key;
        }
        int64_t _t3 = e1;
        if (_t3 == 65) {
            return 1002;
        } else if (_t3 == 66) {
            return 1003;
        } else if (_t3 == 67) {
            return 1001;
        } else if (_t3 == 68) {
            return 1000;
        } else if (_t3 == 70) {
            return 1006;
        } else if (_t3 == 72) {
            return 1005;
        }
        return key;
    }
}
org_bau_os_Terminal_termIos org_bau_os_Terminal_termIos_1(i8_array* data) {
    org_bau_os_Terminal_termIos _t45 = org_bau_os_Terminal_termIos_new();
    _incUseStack(data);
    _t45.data = data;
    return _t45;
}
org_bau_os_Terminal_windowSize org_bau_os_Terminal_windowSize_0() {
    org_bau_os_Terminal_windowSize _t46 = org_bau_os_Terminal_windowSize_new();
    _t46.rows = 0;
    _t46.columns = 0;
    return _t46;
}
void org_bau_os_Terminal_windowSizeChanged_1(int32_t x) {
    refreshScreenCallback_0();
}
void org_bau_os_Terminal_writeToTerminal_2(i8_array* data, int64_t len) {
    write(1, data->data, len);
}
void refreshScreen_0() {
    i8_array* _t0 = i8_array_new(32);
    org_bau_String_StringBuilder* buff = org_bau_String_StringBuilder_1(_t0);
    org_bau_os_Terminal_windowSize _t1 = org_bau_os_Terminal_getWindowSize_0();
    org_bau_os_Terminal_windowSize_copy(&_t1);
    org_bau_os_Terminal_windowSize_free(&currentWindowSize);
    currentWindowSize = _t1;
    org_bau_String_StringBuilder_append_2(buff, string_1024);
    int64_t curX = cursorX;
    int64_t curY = cursorY;
    org_bau_String_StringBuilder_append_2(buff, string_1025);
    if (mode == 1) {
        org_bau_String_StringBuilder_append_2(buff, string_1026);
    } else if (mode == 2) {
        org_bau_String_StringBuilder_append_2(buff, string_1027);
        org_bau_String_StringBuilder_append_2(buff, findText.data);
        curX = 27 + findText.data->len;
        curY = 1;
        org_bau_String_StringBuilder_append_2(buff, string_1023);
    } else {
        org_bau_String_StringBuilder_append_2(buff, string_1028);
        org_bau_String_StringBuilder_append_2(buff, currentFile.fileName);
        org_bau_String_StringBuilder_append_2(buff, string_1029);
        i8_array* _t2 = org_bau_Math_intToString_1(( cursorY + offsetY ) - 1);
        org_bau_String_StringBuilder_append_2(buff, _t2);
        _decUseStack(_t2, i8_array);
    }
    org_bau_String_StringBuilder_append_2(buff, string_1030);
    org_bau_String_StringBuilder_append_2(buff, string_1031);
    int64_t pos = 0;
    int64_t remainingHighlight = 0;
    _incUseStack(currentFile.lines);
    org_bau_List_List_org_bau_String_string* lines = currentFile.lines;
    if (( currentWindowSize.rows - 2 ) > 0) {
        while (1 == 1) {
            int64_t y = 0;
            while (1) {
                if (( y + offsetY ) < lines->size) {
                    org_bau_String_string_copy(&lines->array->data[idx_2(y + offsetY, lines->array->len)]);
                    org_bau_String_string line = lines->array->data[idx_2(y + offsetY, lines->array->len)];
                    if (currentWindowSize.columns > 0) {
                        while (1 == 1) {
                            int64_t x = 0;
                            while (1) {
                                if (( x + offsetX ) >= line.data->len) {
                                    break;
                                }
                                int8_t c = line.data->data[idx_2(x + offsetX, line.data->len)];
                                if (c == 10) {
                                    break;
                                }
                                int64_t _t3 = findText.data->len > 0;
                                if (_t3) {
                                    int64_t _t4 = findText.data->data[idx_2(0, findText.data->len)] == c;
                                    _t3 = _t4;
                                }
                                int64_t _t5 = _t3;
                                if (_t5) {
                                    int64_t _t6 = remainingHighlight == 0;
                                    _t5 = _t6;
                                }
                                if (_t5) {
                                    if (( line.data->len - x ) - offsetX >= findText.data->len) {
                                        int64_t found = 1;
                                        if (findText.data->len > 0) {
                                            while (1 == 1) {
                                                int64_t i = 0;
                                                while (1) {
                                                    if (findText.data->data[i] != line.data->data[idx_2(( x + offsetX ) + i, line.data->len)]) {
                                                        found = 0;
                                                        break;
                                                    }
                                                    int64_t _next = i + 1;
                                                    if (_next >= findText.data->len) {
                                                        break;
                                                    }
                                                    i = _next;
                                                }
                                                break;
                                            }
                                        }
                                        if (found) {
                                            org_bau_String_StringBuilder_append_2(buff, string_1033);
                                            remainingHighlight = findText.data->len;
                                        }
                                    }
                                }
                                if (c < 32) {
                                    c = 63;
                                }
                                i8_array* _t7 = i8_array_new(1);
                                _incUseStack(_t7);
                                i8_array* ch = _t7;
                                ch->data[0] = c;
                                org_bau_String_StringBuilder_append_2(buff, ch);
                                if (remainingHighlight > 0) {
                                    remainingHighlight -= 1;
                                    if (remainingHighlight == 0) {
                                        org_bau_String_StringBuilder_append_2(buff, string_1030);
                                    }
                                }
                                int64_t _next = x + 1;
                                if (_next >= currentWindowSize.columns) {
                                    _decUseStack(ch, i8_array);
                                    _decUseStack(_t7, i8_array);
                                    break;
                                }
                                x = _next;
                                _decUseStack(ch, i8_array);
                                _decUseStack(_t7, i8_array);
                            }
                            break;
                        }
                    }
                    org_bau_String_string_free(&line);
                }
                org_bau_String_StringBuilder_append_2(buff, string_1030);
                org_bau_String_StringBuilder_append_2(buff, string_1031);
                int64_t _next = y + 1;
                if (_next >= ( currentWindowSize.rows - 2 )) {
                    break;
                }
                y = _next;
            }
            break;
        }
    }
    org_bau_String_StringBuilder_append_2(buff, string_1035);
    i8_array* _t8 = org_bau_Math_intToString_1(curY);
    org_bau_String_StringBuilder_append_2(buff, _t8);
    org_bau_String_StringBuilder_append_2(buff, string_1036);
    i8_array* _t9 = org_bau_Math_intToString_1(curX);
    org_bau_String_StringBuilder_append_2(buff, _t9);
    org_bau_String_StringBuilder_append_2(buff, string_1012);
    org_bau_String_StringBuilder_append_2(buff, string_1037);
    org_bau_os_Terminal_writeToTerminal_2(buff->data, buff->len);
    _decUseStack(_t9, i8_array);
    _decUseStack(_t8, i8_array);
    _decUseStack(lines, org_bau_List_List_org_bau_String_string);
    org_bau_os_Terminal_windowSize_free(&_t1);
    _decUseStack(buff, org_bau_String_StringBuilder);
    _decUseStack(_t0, i8_array);
}
void removeByte_0() {
    currentFile.modified = 1;
    int64_t y = ( cursorY + offsetY ) - 2;
    _incUseStack(currentFile.lines);
    org_bau_List_List_org_bau_String_string* lines = currentFile.lines;
    org_bau_String_string_copy(&lines->array->data[idx_2(y, lines->array->len)]);
    org_bau_String_string line = lines->array->data[idx_2(y, lines->array->len)];
    int64_t x = ( cursorX + offsetX ) - 1;
    if (line.data->len < x) {
        return;
    }
    if (x == 0) {
        if (y == 0) {
            return;
        }
        org_bau_String_string_copy(&lines->array->data[idx_2(y - 1, lines->array->len)]);
        org_bau_String_string last = lines->array->data[idx_2(y - 1, lines->array->len)];
        i8_array* _t0 = i8_array_new(line.data->len + last.data->len);
        _incUseStack(_t0);
        i8_array* new = _t0;
        if (last.data->len > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    new->data[idx_2(i, new->len)] = last.data->data[i];
                    int64_t _next = i + 1;
                    if (_next >= last.data->len) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        if (line.data->len > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    new->data[idx_2(i + last.data->len, new->len)] = line.data->data[i];
                    int64_t _next = i + 1;
                    if (_next >= line.data->len) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        if (cursorY > 2) {
            cursorY -= 1;
        } else if (offsetY > 0) {
            offsetY -= 1;
        }
        cursorX += last.data->len + 1;
        while (cursorX >= currentWindowSize.columns) {
            cursorX -= 1;
            offsetX += 1;
        }
        org_bau_String_string _t1 = org_bau_String_string_1(new);
        org_bau_String_string_copy(&_t1);
        lines->array->data[idx_2(y - 1, lines->array->len)] = _t1;
        org_bau_List_List_org_bau_String_string_remove_2(lines, y);
        return;
    }
    i8_array* _t2 = i8_array_new(line.data->len - 1);
    _incUseStack(_t2);
    i8_array* new = _t2;
    if (line.data->len > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                if (i > 0) {
                    new->data[idx_2(i - 1, new->len)] = line.data->data[i];
                }
                int64_t _next = i + 1;
                if (_next >= line.data->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    if (( x - 1 ) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                new->data[idx_2(i, new->len)] = line.data->data[idx_2(i, line.data->len)];
                int64_t _next = i + 1;
                if (_next >= ( x - 1 )) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    org_bau_String_string _t3 = org_bau_String_string_1(new);
    org_bau_String_string_copy(&_t3);
    lines->array->data[idx_2(y, lines->array->len)] = _t3;
    org_bau_String_string_free(&_t3);
    _decUseStack(new, i8_array);
    _decUseStack(_t2, i8_array);
    org_bau_String_string_free(&line);
    _decUseStack(lines, org_bau_List_List_org_bau_String_string);
}
void save_0() {
    _incUseStack(currentFile.lines);
    org_bau_List_List_org_bau_String_string* lines = currentFile.lines;
    int64_t size = lines->size;
    if (lines->size > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                size += lines->array->data[idx_2(i, lines->array->len)].data->len;
                int64_t _next = i + 1;
                if (_next >= lines->size) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    i8_array* _t0 = i8_array_new(size);
    org_bau_String_StringBuilder* data = org_bau_String_StringBuilder_1(_t0);
    if (lines->size > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                org_bau_String_StringBuilder_append_2(data, lines->array->data[idx_2(i, lines->array->len)].data);
                org_bau_String_StringBuilder_append_2(data, string_1032);
                int64_t _next = i + 1;
                if (_next >= lines->size) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    org_bau_File_File* f = org_bau_File_openFile_2(currentFile.fileName, string_1038);
    int64_t _t1 = org_bau_File_File_write_4(f, data->data, 0, size);
    ;
    org_bau_File_File_close_1(f);
    currentFile.modified = 0;
    _decUseStack(f, org_bau_File_File);
    _decUseStack(data, org_bau_String_StringBuilder);
    _decUseStack(_t0, i8_array);
    _decUseStack(lines, org_bau_List_List_org_bau_String_string);
}
void up_0() {
    if (cursorY > 2) {
        cursorY -= 1;
    } else if (offsetY > 0) {
        offsetY -= 1;
    }
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("Exit code ", 10);
    string_1001 = str_const("; will now throw an array out-of-bounds exception", 49);
    string_1010 = str_const("", 0);
    string_1012 = str_const("H", 1);
    string_1023 = str_const(" ", 1);
    string_1024 = str_const("\x1b[?25l\x1b[H", 9);
    string_1025 = str_const("\x1b[7m", 4);
    string_1026 = str_const("File is modified. Quit without saving with Ctrl-Q", 49);
    string_1027 = str_const("Find (Esc/Up/Down/Enter): ", 26);
    string_1028 = str_const("Find Ctrl-F | Save Ctrl-S | Quit Ctrl-Q | File: ", 48);
    string_1029 = str_const(" | Line ", 8);
    string_1030 = str_const("\x1b[0m", 4);
    string_1031 = str_const("\x1b[0K\x0d\n", 6);
    string_1032 = str_const("\n", 1);
    string_1033 = str_const("\x1b[47m", 5);
    string_1035 = str_const("\x1b[", 2);
    string_1036 = str_const(";", 1);
    string_1037 = str_const("\x1b[?25h", 6);
    string_1038 = str_const("w", 1);
    string_1039 = str_const("Not a terminal", 14);
    string_1040 = str_const("Usage: ", 7);
    string_1041 = str_const(" <fileName>", 11);
    string_1042 = str_const("r", 1);
    _main();
    return 0;
}
void _main() {
    POS_INFINITY = (1.0 / 0.0);
    NEG_INFINITY = (-1.0 / 0.0);
    NOT_A_NUMBER = (0.0 / 0.0);
    PI = 3.141592653589793;
    E = 2.718281828459045;
    LOG10 = 2.302585092994046;
    LOG2 = 0.6931471805599453;
    MIN_INT = 0x8000000000000000;
    MAX_INT = 0x7fffffffffffffff;
    i8_array* _t47 = i8_array_new(0);
    oldTermIos = org_bau_os_Terminal_termIos_1(_t47);
    refreshScreenCallback_0 = org_bau_os_Terminal_doNothing_0;
    cursorY = 2;
    cursorX = 1;
    offsetY = 0;
    offsetX = 0;
    foundX = 0;
    foundY = 0;
    i8_array* _t49 = i8_array_new(0);
    findText = org_bau_String_string_1(_t49);
    mode = 0;
    currentWindowSize = org_bau_os_Terminal_getWindowSize_0();
    i8_array* _t50 = i8_array_new(0);
    i8_array* _t51 = i8_array_new(0);
    org_bau_List_List_org_bau_String_string* _t52 = org_bau_List_newList_org_bau_String_string_1(0);
    currentFile = fileContent_3(_t50, _t51, _t52);
    int64_t _t53 = org_bau_os_Terminal_isTerminal_0();
    if (!(_t53)) {
        printf("Not a terminal\n");
        return;
    }
    int64_t _t54 = org_bau_Env_argCount_0();
    if (_t54 != 2) {
        i8_array* _t55 = org_bau_Env_arg_1(0);
        printf("Usage: %.*s <fileName>\n", _t55->len, _t55->data);
        return;
    }
    i8_array* fileName = org_bau_Env_arg_1(1);
    org_bau_File_File* f = org_bau_File_openFile_2(fileName, string_1042);
    int64_t _t56 = org_bau_File_File_len_1(f);
    i8_array* _t57 = i8_array_new(_t56);
    _incUseStack(_t57);
    i8_array* fileData = _t57;
    int64_t _t58 = org_bau_File_File_len_1(f);
    int64_t _t59 = org_bau_File_File_readFully_4(f, fileData, 0, _t58);
    ;
    org_bau_File_File_close_1(f);
    org_bau_List_List_org_bau_String_string* lines = org_bau_String_split_2(fileData, string_1032);
    fileContent _t60 = fileContent_3(fileName, fileData, lines);
    fileContent_copy(&_t60);
    fileContent_free(&currentFile);
    currentFile = _t60;
    org_bau_os_Terminal_enableRawMode_1(refreshScreen_0);
    while (1) {
        refreshScreen_0();
        int64_t key = org_bau_os_Terminal_readEditorKey_0();
        if (key < 0) {
            break;
        }
        int64_t _t61 = key;
        if ((_t61 == 3) || (_t61 == 17)) {
            if (!(currentFile.modified)) {
                break;
            }
            mode = 1;
            refreshScreen_0();
            int64_t _t62 = org_bau_os_Terminal_readEditorKey_0();
            key = _t62;
            if (key == 17) {
                break;
            }
            mode = 0;
        } else if (_t61 == 13) {
            insertNewline_0();
        } else if (_t61 == 19) {
            save_0();
        } else if (_t61 == 6) {
            find_0();
        } else if (((_t61 == 127) || (_t61 == 8)) || (_t61 == 1004)) {
            removeByte_0();
            if (cursorX > 1) {
                cursorX -= 1;
            } else if (offsetX > 0) {
                offsetX -= 1;
            }
        } else if (_t61 == 1007) {
            if (currentWindowSize.rows > 0) {
                while (1 == 1) {
                    int64_t i = 0;
                    while (1) {
                        up_0();
                        int64_t _next = i + 1;
                        if (_next >= currentWindowSize.rows) {
                            break;
                        }
                        i = _next;
                    }
                    break;
                }
            }
        } else if (_t61 == 1008) {
            if (currentWindowSize.rows > 0) {
                while (1 == 1) {
                    int64_t i = 0;
                    while (1) {
                        down_0();
                        int64_t _next = i + 1;
                        if (_next >= currentWindowSize.rows) {
                            break;
                        }
                        i = _next;
                    }
                    break;
                }
            }
        } else if (_t61 == 1002) {
            up_0();
        } else if (_t61 == 1003) {
            down_0();
        } else if (_t61 == 1000) {
            int64_t _t63 = cursorY > 2;
            if (!(_t63)) {
                int64_t _t64 = offsetY > 0;
                _t63 = _t64;
            }
            if (cursorX > 1) {
                cursorX -= 1;
            } else if (offsetX > 0) {
                offsetX -= 1;
            } else if (_t63) {
                if (cursorY > 2) {
                    cursorY -= 1;
                } else if (offsetY > 0) {
                    offsetY -= 1;
                }
                int64_t y = ( cursorY + offsetY ) - 2;
                org_bau_String_string_copy(&lines->array->data[idx_2(y, lines->array->len)]);
                org_bau_String_string line = lines->array->data[idx_2(y, lines->array->len)];
                cursorX = line.data->len + 1;
                while (cursorX >= currentWindowSize.columns) {
                    cursorX -= 1;
                    offsetX += 1;
                }
                org_bau_String_string_free(&line);
            }
        } else if (_t61 == 1001) {
            int64_t x = ( cursorX + offsetX ) - 1;
            int64_t y = ( cursorY + offsetY ) - 2;
            org_bau_String_string_copy(&lines->array->data[idx_2(y, lines->array->len)]);
            org_bau_String_string line = lines->array->data[idx_2(y, lines->array->len)];
            if (x < line.data->len) {
                if (cursorX < currentWindowSize.columns) {
                    cursorX += 1;
                } else {
                    offsetX += 1;
                }
            } else if (y < lines->size) {
                cursorX = 1;
                offsetX = 0;
                if (cursorY < ( currentWindowSize.rows - 1 )) {
                    cursorY += 1;
                } else if (( cursorY + offsetY ) < currentFile.lines->size) {
                    offsetY += 1;
                }
            }
            org_bau_String_string_free(&line);
        } else {
            int8_t _t65 = i8_1(key);
            insertByte_1(_t65);
            if (cursorX < currentWindowSize.columns) {
                cursorX += 1;
            } else {
                offsetX += 1;
            }
        }
    }
    fileContent_free(&_t60);
    _decUseStack(lines, org_bau_List_List_org_bau_String_string);
    _decUseStack(fileData, i8_array);
    _decUseStack(_t57, i8_array);
    _decUseStack(f, org_bau_File_File);
    _decUseStack(fileName, i8_array);
    _decUseStack(_t47, i8_array);
    org_bau_os_Terminal_termIos_free(&oldTermIos);
    _decUseStack(_t49, i8_array);
    org_bau_String_string_free(&findText);
    org_bau_os_Terminal_windowSize_free(&currentWindowSize);
    _decUseStack(_t50, i8_array);
    _decUseStack(_t51, i8_array);
    _decUseStack(_t52, org_bau_List_List_org_bau_String_string);
    fileContent_free(&currentFile);
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
