#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <fcntl.h>
#include <limits.h>
#include <math.h>
#include <signal.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <time.h>
#include <unistd.h>
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
typedef struct org_bau_String_StringBuilder org_bau_String_StringBuilder;
struct org_bau_String_StringBuilder;
typedef struct org_bau_os_Terminal_termIos org_bau_os_Terminal_termIos;
struct org_bau_os_Terminal_termIos;
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
struct org_bau_os_Terminal_termIos {
    i8_array* data;
};
org_bau_os_Terminal_termIos org_bau_os_Terminal_termIos_new() {
    org_bau_os_Terminal_termIos result;
    result.data = 0;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
int64_t canPlace_2(int64_t pos, int64_t rot);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
int64_t imod_2(int64_t a, int64_t b);
void org_bau_Arrays_reverse_i8_array_i8_3(i8_array* buff, int64_t first, int64_t last);
int64_t org_bau_DateTime_getNanoTime_0();
void org_bau_Env_atExit_1(void  (*callback_0)());
void org_bau_Env_exit_1(int64_t code);
int64_t org_bau_Int_appendInt_3(int64_t n, i8_array* buff, int64_t pos);
i8_array* org_bau_Int_intToString_1(int64_t n);
org_bau_String_StringBuilder* org_bau_String_StringBuilder_1(i8_array* data);
void org_bau_String_StringBuilder_append_2(org_bau_String_StringBuilder* this, i8_array* b);
void org_bau_String_StringBuilder_append_4(org_bau_String_StringBuilder* this, i8_array* b, int64_t start, int64_t end);
int64_t org_bau_Utils_random_0();
int64_t org_bau_Utils_random_1(int64_t smallerThan);
void org_bau_Utils_setRandomSeed_1(int64_t seed);
void org_bau_os_Signal_signal_2(int64_t signalId, void  (*callback_1)(int32_t));
void org_bau_os_Sleep_sleep_1(int64_t millis);
void org_bau_os_Terminal_disableRawMode_0();
void org_bau_os_Terminal_doNothing_0();
void org_bau_os_Terminal_enableRawMode_1(void  (*refreshScreen_0)());
int64_t org_bau_os_Terminal_isTerminal_0();
int64_t org_bau_os_Terminal_keyboardHit_0();
int64_t org_bau_os_Terminal_readByte_0();
int64_t org_bau_os_Terminal_readEditorKey_0();
org_bau_os_Terminal_termIos org_bau_os_Terminal_termIos_1(i8_array* data);
void org_bau_os_Terminal_windowSizeChanged_1(int32_t x);
void org_bau_os_Terminal_writeToTerminal_2(i8_array* data, int64_t len);
void refreshScreen_0();
int64_t shiftRight_int_2(int64_t a, int64_t b);
void updateBlock_1(int64_t draw);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x);
void org_bau_os_Terminal_termIos_free(org_bau_os_Terminal_termIos* x);
void org_bau_os_Terminal_termIos_copy(org_bau_os_Terminal_termIos* x);
void i8_array_free(i8_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void int_array_free(int_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x) {
    _decUse(x->data, i8_array);
    _free(x); _traceFree(x);
}
void org_bau_os_Terminal_termIos_free(org_bau_os_Terminal_termIos* x) {
    _decUse(x->data, i8_array);
}
void org_bau_os_Terminal_termIos_copy(org_bau_os_Terminal_termIos* x) {
    _incUse(x->data);
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
i8_array* string_1025;
i8_array* string_1026;
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
int8_t queueByte;
org_bau_os_Terminal_termIos oldTermIos;
void  (*refreshScreenCallback_0)();
int64_t width;
int64_t height;
int64_t score;
int64_t highScore;
int64_t position;
int64_t rotation;
int64_t running;
int64_t blockType;
i8_array* field;
i8_array* shapes;
i8_array* rotated;
int64_t canPlace_2(int64_t pos, int64_t rot) {
    if (4 > 0) {
        while (1 == 1) {
            int64_t y = 0;
            while (1) {
                if (4 > 0) {
                    while (1 == 1) {
                        int64_t x = 0;
                        while (1) {
                            int8_t c = shapes->data[idx_2(( y * 21 ) * 6 + ( rot * 6 ) + x, _arrayLen(shapes))];
                            if (c != 88) {
                                int64_t _next = x + 1;
                                if (_next >= 4) {
                                    break;
                                }
                                x = _next;
                                continue;
                            }
                            int64_t offset = ( y * 14 ) + x;
                            if (field->data[idx_2(pos + offset, _arrayLen(field))]) {
                                return 0;
                            }
                            int64_t _next = x + 1;
                            if (_next >= 4) {
                                break;
                            }
                            x = _next;
                        }
                        break;
                    }
                }
                int64_t _next = y + 1;
                if (_next >= 4) {
                    break;
                }
                y = _next;
            }
            break;
        }
    }
    return 1;
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
void org_bau_Arrays_reverse_i8_array_i8_3(i8_array* buff, int64_t first, int64_t last) {
    while (first < last) {
        int8_t temp = buff->data[first];
        buff->data[first] = buff->data[last];
        buff->data[last] = temp;
        if (first >= ( _arrayLen(buff) - 1 )) {
            break;
        }
        first += 1;
        if (last < 1) {
            break;
        }
        last -= 1;
    }
}
int64_t org_bau_DateTime_getNanoTime_0() {
    struct timespec time={0,0};
    clock_gettime(CLOCK_MONOTONIC, &time);
    return time.tv_sec * 1000000000ULL + time.tv_nsec;
    return 0;
}
void org_bau_Env_atExit_1(void  (*callback_0)()) {
    atexit(callback_0);
}
void org_bau_Env_exit_1(int64_t code) {
    exit(code);
    printf("Exit code %lld; will now cause a stack overflow\n", (long long)code);
    org_bau_Env_exit_1(code);
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
        pos += 1;
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
        pos += 1;
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
org_bau_String_StringBuilder* org_bau_String_StringBuilder_1(i8_array* data) {
    org_bau_String_StringBuilder* _t35 = org_bau_String_StringBuilder_new();
    _incUseStack(data);
    _t35->data = data;
    _t35->len = 0;
    return _t35;
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
int64_t org_bau_Utils_random_0() {
    randomSeed += 0x9e3779b97f4a7c15;
    int64_t z = randomSeed;
    z = (z ^ (shiftRight_int_2(z, 30))) * -4658895280553007687;
    z = (z ^ (shiftRight_int_2(z, 27))) * -7723592293110705685;
    int64_t _r0 = z ^ (shiftRight_int_2(z, 31));
    return _r0;
}
int64_t org_bau_Utils_random_1(int64_t smallerThan) {
    int64_t limit = smallerThan;
    if (limit == 0) {
        return 0;
    }
    int64_t max = limit - 1;
    if ((limit & (max - 1)) == 0) {
        int64_t _t0 = org_bau_Utils_random_0();
        int64_t _r0 = _t0 & max;
        return _r0;
    }
    while (1) {
        int64_t _t1 = org_bau_Utils_random_0();
        int64_t x = shiftRight_int_2(_t1, 1);
        int64_t result = imod_2(x, limit);
        return result;
    }
}
void org_bau_Utils_setRandomSeed_1(int64_t seed) {
    randomSeed = seed;
}
void org_bau_os_Signal_signal_2(int64_t signalId, void  (*callback_1)(int32_t)) {
    signal(signalId, callback_1);
}
void org_bau_os_Sleep_sleep_1(int64_t millis) {
    if (millis < 0) {
        return;
    }
    #include <errno.h> 
          struct timespec ts;
          ts.tv_sec = millis / 1000;
          ts.tv_nsec = (millis % 1000) * 1000000;
          int res;
          do {
              res = nanosleep(&ts, &ts);
          } while (res && errno == EINTR);
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
int64_t org_bau_os_Terminal_isTerminal_0() {
    return isatty(0);
    return 0;
}
int64_t org_bau_os_Terminal_keyboardHit_0() {
    int oldf = fcntl(STDIN_FILENO, F_GETFL, 0);
            fcntl(STDIN_FILENO, F_SETFL, oldf | O_NONBLOCK);
    int64_t x = org_bau_os_Terminal_readByte_0();
    fcntl(STDIN_FILENO, F_SETFL, oldf);
    if (x) {
        queueByte = x;
        return 1;
    }
    return 0;
}
int64_t org_bau_os_Terminal_readByte_0() {
    if (queueByte) {
        int8_t result = queueByte;
        queueByte = 0;
        return result;
    }
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
            } else {
                if (e1 == 70) {
                    return 1006;
                }
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
    org_bau_os_Terminal_termIos _t39 = org_bau_os_Terminal_termIos_new();
    _incUseStack(data);
    _t39.data = data;
    return _t39;
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
    org_bau_String_StringBuilder_append_2(buff, string_1028);
    org_bau_String_StringBuilder_append_2(buff, string_1029);
    org_bau_String_StringBuilder_append_2(buff, string_1030);
    i8_array* _t1 = org_bau_Int_intToString_1(score);
    org_bau_String_StringBuilder_append_2(buff, _t1);
    org_bau_String_StringBuilder_append_2(buff, string_1031);
    i8_array* _t2 = org_bau_Int_intToString_1(highScore);
    org_bau_String_StringBuilder_append_2(buff, _t2);
    org_bau_String_StringBuilder_append_2(buff, string_1032);
    if (24 > 0) {
        while (1 == 1) {
            int64_t y = 0;
            while (1) {
                if (14 > 0) {
                    while (1 == 1) {
                        int64_t x = 0;
                        while (1) {
                            int8_t c = field->data[idx_2(x + ( 14 * y ), _arrayLen(field))];
                            if (c) {
                                i8_array* _t3 = i8_array_new(1);
                                _incUseStack(_t3);
                                i8_array* c2 = _t3;
                                c2->data[0] = ( 48 + c ) - 1;
                                org_bau_String_StringBuilder_append_2(buff, string_1033);
                                org_bau_String_StringBuilder_append_2(buff, c2);
                                org_bau_String_StringBuilder_append_2(buff, string_1034);
                                _decUseStack(c2, i8_array);
                                _decUseStack(_t3, i8_array);
                            }
                            org_bau_String_StringBuilder_append_2(buff, string_1035);
                            int64_t _next = x + 1;
                            if (_next >= 14) {
                                break;
                            }
                            x = _next;
                        }
                        break;
                    }
                }
                org_bau_String_StringBuilder_append_2(buff, string_1036);
                int64_t _next = y + 1;
                if (_next >= 24) {
                    break;
                }
                y = _next;
            }
            break;
        }
    }
    org_bau_os_Terminal_writeToTerminal_2(buff->data, buff->len);
    _decUseStack(_t2, i8_array);
    _decUseStack(_t1, i8_array);
    _decUseStack(buff, org_bau_String_StringBuilder);
    _decUseStack(_t0, i8_array);
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
void updateBlock_1(int64_t draw) {
    if (4 > 0) {
        while (1 == 1) {
            int64_t y = 0;
            while (1) {
                if (4 > 0) {
                    while (1 == 1) {
                        int64_t x = 0;
                        while (1) {
                            int8_t c = shapes->data[idx_2(( y * 21 ) * 6 + ( rotation * 6 ) + x, _arrayLen(shapes))];
                            if (c != 88) {
                                int64_t _next = x + 1;
                                if (_next >= 4) {
                                    break;
                                }
                                x = _next;
                                continue;
                            }
                            int64_t offset = ( y * 14 ) + x;
                            field->data[idx_2(position + offset, _arrayLen(field))] = (1 + blockType) * draw;
                            int64_t _next = x + 1;
                            if (_next >= 4) {
                                break;
                            }
                            x = _next;
                        }
                        break;
                    }
                }
                int64_t _next = y + 1;
                if (_next >= 4) {
                    break;
                }
                y = _next;
            }
            break;
        }
    }
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("Exit code ", 10);
    string_1001 = str_const("; will now cause a stack overflow", 33);
    string_1025 = str_const("......       X     X      X    X           X     X     X           X                      XX      X   X      XX         .....\n.XX...XXXX  XXX    XX    XX    X    X X    X     X     XX   XXX   XX     XX   XX    XXX    X    XXX   XXX    X    XXX   XXX..\n.XX...              X    X     XX   XXX   XX     X     X     X     X    XX     XX   X      X                 X      X   X.X..\n......                                           X                                                                      ......", 504);
    string_1026 = str_const("AIJMNOURBKLCDEPQFSTFG", 21);
    string_1028 = str_const("\x1b[?25l\x1b[H\x1b[0m", 13);
    string_1029 = str_const("\x1b[0K\x0d\n", 6);
    string_1030 = str_const("  Score: ", 9);
    string_1031 = str_const(" High: ", 7);
    string_1032 = str_const("\x1b[0K\x0d\n\x1b[0K\x0d\n", 12);
    string_1033 = str_const("\x1b[4", 3);
    string_1034 = str_const("m", 1);
    string_1035 = str_const("  \x1b[0m", 6);
    string_1036 = str_const("\x1b[0m\x1b[0K\x0d\n", 10);
    string_1037 = str_const("Not a terminal", 14);
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
    queueByte = 0;
    i8_array* _t41 = i8_array_new(0);
    oldTermIos = org_bau_os_Terminal_termIos_1(_t41);
    refreshScreenCallback_0 = org_bau_os_Terminal_doNothing_0;
    width = 14;
    height = 24;
    score = 0;
    highScore = 0;
    position = 0;
    rotation = 0;
    running = 0;
    blockType = 0;
    i8_array* _t42 = i8_array_new(336);
    _incUseStack(_t42);
    field = _t42;
    _incUseStack(string_1025);
    shapes = string_1025;
    _incUseStack(string_1026);
    rotated = string_1026;
    int64_t _t43 = org_bau_os_Terminal_isTerminal_0();
    if (!(_t43)) {
        printf("Not a terminal\n");
        return;
    }
    int64_t _t44 = org_bau_DateTime_getNanoTime_0();
    org_bau_Utils_setRandomSeed_1(_t44);
    org_bau_os_Terminal_enableRawMode_1(refreshScreen_0);
    while (1) {
        refreshScreen_0();
        score = 0;
        rotation = 0;
        position = 20;
        running = 1;
        blockType = 1;
        if (336 > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    field->data[idx_2(i, _arrayLen(field))] = 0;
                    int64_t _next = i + 1;
                    if (_next >= 336) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        if (24 > 0) {
            while (1 == 1) {
                int64_t y = 0;
                while (1) {
                    field->data[idx_2(14 * y, _arrayLen(field))] = 1;
                    field->data[idx_2(13 + ( 14 * y ), _arrayLen(field))] = 1;
                    int64_t _next = y + 1;
                    if (_next >= 24) {
                        break;
                    }
                    y = _next;
                }
                break;
            }
        }
        if (14 > 0) {
            while (1 == 1) {
                int64_t x = 0;
                while (1) {
                    field->data[idx_2(x + 322, _arrayLen(field))] = 1;
                    int64_t _next = x + 1;
                    if (_next >= 14) {
                        break;
                    }
                    x = _next;
                }
                break;
            }
        }
        refreshScreen_0();
        while (1) {
            updateBlock_1(0);
            int64_t tick = 16;
            while (1 == 1) {
                int64_t _t45 = tick > 0;
                if (_t45) {
                    int64_t _t46 = running;
                    _t45 = _t46;
                }
                if (!(_t45)) {
                    break;
                }
                tick -= 1;
                org_bau_os_Sleep_sleep_1(14);
                int64_t _t47 = org_bau_os_Terminal_keyboardHit_0();
                if (!(_t47)) {
                    continue;
                }
                int64_t tempRot = rotation;
                int64_t tempPos = position;
                int64_t key = org_bau_os_Terminal_readEditorKey_0();
                int64_t _t48 = key;
                if (_t48 == 1000) {
                    tempPos -= 1;
                } else if (_t48 == 1001) {
                    tempPos += 1;
                } else if ((_t48 == 32) || (_t48 == 1002)) {
                    tempRot = rotated->data[idx_2(rotation, _arrayLen(rotated))] - 65;
                } else if (_t48 == 27) {
                    org_bau_Env_exit_1(0);
                } else if (_t48 == 1003) {
                    running = 0;
                }
                int64_t _t49 = canPlace_2(tempPos, tempRot);
                if (_t49) {
                    position = tempPos;
                    rotation = tempRot;
                    updateBlock_1(1);
                    refreshScreen_0();
                    updateBlock_1(0);
                }
            }
            int64_t _t50 = canPlace_2(position + 14, rotation);
            if (_t50) {
                position += 14;
                updateBlock_1(1);
                refreshScreen_0();
                continue;
            }
            updateBlock_1(1);
            int64_t i = 22;
            while (i > 0) {
                int64_t found = 1;
                if (14 > 0) {
                    while (1 == 1) {
                        int64_t j = 0;
                        while (1) {
                            if (field->data[idx_2(( i * 14 ) + j, _arrayLen(field))] == 0) {
                                found = 0;
                                break;
                            }
                            int64_t _next = j + 1;
                            if (_next >= 14) {
                                break;
                            }
                            j = _next;
                        }
                        break;
                    }
                }
                if (found) {
                    i += 1;
                    score += 1;
                    if (score > highScore) {
                        highScore = score;
                    }
                    int64_t k = ( i * 14 ) - 14;
                    while (k >= 0) {
                        field->data[idx_2(k + 14, _arrayLen(field))] = field->data[idx_2(k, _arrayLen(field))];
                        k -= 1;
                    }
                }
                i -= 1;
            }
            refreshScreen_0();
            int64_t _t51 = org_bau_Utils_random_1(7);
            rotation = _t51;
            blockType = rotation + 1;
            if (position < 28) {
                break;
            }
            position = 20;
            running = 1;
        }
    }
    _decUseStack(_t41, i8_array);
    org_bau_os_Terminal_termIos_free(&oldTermIos);
    _decUseStack(_t42, i8_array);
    _decUseStack(field, i8_array);
    _decUseStack(shapes, i8_array);
    _decUseStack(rotated, i8_array);
    _end();
}
/*

type dateTime
Date and time.

fun daylightSaving() int
Is daylight saving time active currently.

fun daylightSaving(year int, month int, day int, hour int, min int, sec int) int
Is daylight saving time active currently.

fun getDateTime() dateTime
Get the local time in millisecond precision.

fun getNanoTime() int
Nanosecons since some undefined point in the past. Never jumps backwards.

fun getNanoTimeUTC() int
Nanoseconds since 1970 (epoch). May jump backwards when the system clock is adjusted.

fun timeMillis() int
Get the seconds since the epoch

fun timeOffset() int
Get the current offset between the local time and UTC in seconds.

fun timeOffset(year int, month int, day int, hour int, min int, sec int) int
Get the current offset between the local time and UTC in seconds.

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

*/
