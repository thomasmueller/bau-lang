#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <fcntl.h>
#include <limits.h>
#include <signal.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <termios.h>
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
typedef struct org_bau_os_Terminal_termIos org_bau_os_Terminal_termIos;
struct org_bau_os_Terminal_termIos;
typedef struct org_bau_String_StringBuilder org_bau_String_StringBuilder;
struct org_bau_String_StringBuilder;
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
struct org_bau_os_Terminal_termIos {
    i8_array* data;
};
org_bau_os_Terminal_termIos org_bau_os_Terminal_termIos_new() {
    org_bau_os_Terminal_termIos result;
    result.data = 0;
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
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
int64_t evaluateBoard_1(int64_t black);
int64_t getPiece_1(int64_t p);
int64_t getPossibleMoves_2(int64_t from, int64_t attacksOnly);
int32_t i32_1(int64_t x);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
int64_t imod_2(int64_t a, int64_t b);
void init_0();
int64_t isBlack_1(int64_t p);
int64_t isFieldAttacked_2(int64_t black, int64_t pos);
int64_t move_1(int64_t move);
int64_t move_2(int64_t source, int64_t target);
int64_t negamax_5(int64_t top, int64_t depth, int64_t black, int64_t alpha, int64_t beta);
void org_bau_Arrays_reverse_i8_array_i8_3(i8_array* buff, int64_t first, int64_t last);
void org_bau_Env_atExit_1(void  (*callback_0)());
void org_bau_Env_exit_1(int64_t code);
int64_t org_bau_Int_abs_1(int64_t x);
int64_t org_bau_Int_appendInt_3(int64_t n, i8_array* buff, int64_t pos);
int64_t org_bau_Int_bitCount_1(int64_t x);
int64_t org_bau_Int_bitCountI32_1(int32_t x);
i8_array* org_bau_Int_intToString_1(int64_t n);
int64_t org_bau_Int_max_2(int64_t a, int64_t b);
int64_t org_bau_Int_min_2(int64_t a, int64_t b);
int64_t org_bau_Int_numberOfTrailingZeros_1(int64_t x);
org_bau_String_StringBuilder* org_bau_String_StringBuilder_1(i8_array* data);
void org_bau_String_StringBuilder_append_2(org_bau_String_StringBuilder* this, i8_array* b);
void org_bau_String_StringBuilder_append_4(org_bau_String_StringBuilder* this, i8_array* b, int64_t start, int64_t end);
void org_bau_os_Signal_signal_2(int64_t signalId, void  (*callback_1)(int32_t));
void org_bau_os_Terminal_disableRawMode_0();
void org_bau_os_Terminal_doNothing_0();
void org_bau_os_Terminal_enableRawMode_1(void  (*refreshScreen_0)());
int64_t org_bau_os_Terminal_isTerminal_0();
int64_t org_bau_os_Terminal_readByte_0();
int64_t org_bau_os_Terminal_readEditorKey_0();
org_bau_os_Terminal_termIos org_bau_os_Terminal_termIos_1(i8_array* data);
void org_bau_os_Terminal_windowSizeChanged_1(int32_t x);
void org_bau_os_Terminal_writeToTerminal_2(i8_array* data, int64_t len);
void refreshScreen_0();
int64_t shiftLeft_2(int64_t a, int64_t b);
int32_t shiftRight_i32_2(int32_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
int64_t slide_4(int64_t from, int64_t maxDist, int64_t xo, int64_t yo);
void undo_1(int64_t move);
void updateCastlingRights_1(int64_t pos);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_os_Terminal_termIos_free(org_bau_os_Terminal_termIos* x);
void org_bau_os_Terminal_termIos_copy(org_bau_os_Terminal_termIos* x);
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x);
void i8_array_free(i8_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void int_array_free(int_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void org_bau_os_Terminal_termIos_free(org_bau_os_Terminal_termIos* x) {
    _decUse(x->data, i8_array);
}
void org_bau_os_Terminal_termIos_copy(org_bau_os_Terminal_termIos* x) {
    _incUse(x->data);
}
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x) {
    _decUse(x->data, i8_array);
    _free(x); _traceFree(x);
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
i8_array* string_1006;
i8_array* string_1008;
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
i8_array* string_1044;
i8_array* string_1048;
int64_t randomSeed;
int64_t MIN_INT;
int64_t MAX_INT;
int64_t MIN_I32;
int64_t MAX_I32;
int8_t queueByte;
org_bau_os_Terminal_termIos oldTermIos;
void  (*refreshScreenCallback_0)();
int64_t xx;
int64_t yy;
int64_t state;
int64_t showCursor;
int64_t from;
int64_t blackTurn;
int64_t lastBlack;
int64_t lastWhite;
int64_t KING;
int64_t QUEEN;
int64_t ROOK;
int64_t BISHOP;
int64_t KNIGHT;
int64_t PAWN;
int64_t BLACK;
int_array* board;
int64_t castlingFlags;
int64_t pawnMoved2;
int64_t turn;
int64_t evaluateBoard_1(int64_t black) {
    int64_t sum = 0;
    if (64 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                int64_t b = board->data[idx_2(i, _arrayLen(board))];
                int64_t sc = 0;
                int64_t _t0 = getPiece_1(b);
                if (_t0 == 1) {
                    sc = 10000000;
                } else if (_t0 == 2) {
                    sc = 1000;
                } else if (_t0 == 3) {
                    sc = 500;
                } else if (_t0 == 5) {
                    int64_t _t1 = getPossibleMoves_2(i, 0);
                    int64_t _t2 = org_bau_Int_bitCount_1(_t1);
                    sc = _t2;
                    sc += 320;
                } else if (_t0 == 4) {
                    int64_t _t3 = getPossibleMoves_2(i, 0);
                    int64_t _t4 = org_bau_Int_bitCount_1(_t3);
                    sc = _t4;
                    sc += 330;
                } else if (_t0 == 6) {
                    sc = 100;
                    if (turn > 40) {
                        int64_t _t5 = 0;
                        int64_t _t6 = isBlack_1(b);
                        if (_t6) {
                            _t5 = (idiv_2(i, 8)) - 1;
                        } else {
                            _t5 = 6 - (idiv_2(i, 8));
                        }
                        sc += shiftLeft_2(1, _t5);
                    }
                }
                int64_t _t7 = isBlack_1(b);
                if (_t7 != black) {
                    sc = - sc;
                }
                sum += sc;
                int64_t _next = i + 1;
                if (_next >= 64) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    return sum;
}
int64_t getPiece_1(int64_t p) {
    int64_t _t0 = 0;
    if (p > 6) {
        _t0 = p - 6;
    } else {
        _t0 = p;
    }
    return _t0;
}
int64_t getPossibleMoves_2(int64_t from, int64_t attacksOnly) {
    int64_t b = board->data[idx_2(from, _arrayLen(board))];
    int64_t p = getPiece_1(b);
    int64_t black = isBlack_1(b);
    int64_t maxDist = 1;
    int64_t result = 0;
    int64_t _t0 = p == 2;
    if (!(_t0)) {
        int64_t _t1 = p == 3;
        _t0 = _t1;
    }
    int64_t _t2 = _t0;
    if (!(_t2)) {
        int64_t _t3 = p == 4;
        _t2 = _t3;
    }
    if (_t2) {
        maxDist = 7;
    }
    int64_t _t4 = p == 1;
    if (!(_t4)) {
        int64_t _t5 = p == 2;
        _t4 = _t5;
    }
    int64_t _t6 = _t4;
    if (!(_t6)) {
        int64_t _t7 = p == 4;
        _t6 = _t7;
    }
    if (_t6) {
        int64_t _t8 = slide_4(from, maxDist, 1, 1);
        result |= _t8;
        int64_t _t9 = slide_4(from, maxDist, -1, -1);
        result |= _t9;
        int64_t _t10 = slide_4(from, maxDist, 1, -1);
        result |= _t10;
        int64_t _t11 = slide_4(from, maxDist, -1, 1);
        result |= _t11;
    }
    int64_t _t12 = p == 1;
    if (!(_t12)) {
        int64_t _t13 = p == 2;
        _t12 = _t13;
    }
    int64_t _t14 = _t12;
    if (!(_t14)) {
        int64_t _t15 = p == 3;
        _t14 = _t15;
    }
    if (_t14) {
        int64_t _t16 = slide_4(from, maxDist, 1, 0);
        result |= _t16;
        int64_t _t17 = slide_4(from, maxDist, -1, 0);
        result |= _t17;
        int64_t _t18 = slide_4(from, maxDist, 0, 1);
        result |= _t18;
        int64_t _t19 = slide_4(from, maxDist, 0, -1);
        result |= _t19;
    }
    if (p == 5) {
        int64_t _t20 = slide_4(from, maxDist, 1, 2);
        result |= _t20;
        int64_t _t21 = slide_4(from, maxDist, 2, 1);
        result |= _t21;
        int64_t _t22 = slide_4(from, maxDist, -1, -2);
        result |= _t22;
        int64_t _t23 = slide_4(from, maxDist, -2, -1);
        result |= _t23;
        int64_t _t24 = slide_4(from, maxDist, 1, -2);
        result |= _t24;
        int64_t _t25 = slide_4(from, maxDist, 2, -1);
        result |= _t25;
        int64_t _t26 = slide_4(from, maxDist, -1, 2);
        result |= _t26;
        int64_t _t27 = slide_4(from, maxDist, -2, 1);
        result |= _t27;
    } else if (p == 6) {
        int64_t _t28 = 0;
        if (isBlack_1(b)) {
            _t28 = 1;
        } else {
            _t28 = -1;
        }
        int64_t dir = _t28;
        int64_t dist = 1;
        if (!(attacksOnly)) {
            int64_t _t29 = dir == 1;
            if (_t29) {
                int64_t _t30 = ( idiv_2(from, 8) ) == 1;
                _t29 = _t30;
            }
            int64_t _t31 = _t29;
            if (_t31) {
                int64_t _t32 = board->data[idx_2(from + 16, _arrayLen(board))] == 0;
                _t31 = _t32;
            }
            if (_t31) {
                dist = 2;
            }
            int64_t _t33 = dir == -1;
            if (_t33) {
                int64_t _t34 = ( idiv_2(from, 8) ) == 6;
                _t33 = _t34;
            }
            int64_t _t35 = _t33;
            if (_t35) {
                int64_t _t36 = board->data[idx_2(from - 16, _arrayLen(board))] == 0;
                _t35 = _t36;
            }
            if (_t35) {
                dist = 2;
            }
            int64_t _t37 = slide_4(from, dist, 0, dir);
            result |= _t37;
            int64_t _t38 = result != 0;
            if (_t38) {
                int64_t _t39 = board->data[idx_2(from + ( dir * 8 ), _arrayLen(board))] != 0;
                _t38 = _t39;
            }
            if (_t38) {
                result = 0;
            }
        }
        if (( idiv_2(pawnMoved2, 8) ) == ( idiv_2(from, 8) )) {
            if (pawnMoved2 == ( from - 1 )) {
                result |= shiftLeft_2(1, (from - 1));
            } else if (pawnMoved2 == ( from + 1 )) {
                result |= shiftLeft_2(1, (from + 1));
            }
        }
        int64_t capture = slide_4(from, maxDist, 1, dir);
        int64_t _t40 = capture != 0;
        if (_t40) {
            int64_t _t41 = board->data[idx_2(( from + 1 ) + ( dir * 8 ), _arrayLen(board))] != 0;
            _t40 = _t41;
        }
        if (_t40) {
            result |= capture;
        }
        int64_t _t42 = slide_4(from, maxDist, -1, dir);
        capture = _t42;
        int64_t _t43 = capture != 0;
        if (_t43) {
            int64_t _t44 = board->data[idx_2(( from - 1 ) + ( dir * 8 ), _arrayLen(board))] != 0;
            _t43 = _t44;
        }
        if (_t43) {
            result |= capture;
        }
    }
    int64_t _t45 = p == 1;
    if (_t45) {
        int64_t _t46 = attacksOnly == 0;
        _t45 = _t46;
    }
    if (_t45) {
        int64_t _t47 = 0;
        if (black) {
            _t47 = 0;
        } else {
            _t47 = 2;
        }
        int64_t r = shiftRight_int_2(castlingFlags, _t47);
        int64_t _t48 = (r & 3) != 3;
        if (_t48) {
            int64_t _t49 = !(isFieldAttacked_2(black, from));
            _t48 = _t49;
        }
        if (_t48) {
            int64_t _t50 = (r & 1) == 0;
            if (_t50) {
                int64_t _t51 = !(isFieldAttacked_2(black, from - 1));
                _t50 = _t51;
            }
            if (_t50) {
                int64_t rook = slide_4(from - 4, 8, 1, 0);
                rook >>= from - 3;
                if ((rook & 7) == 7) {
                    result |= shiftLeft_2(1, (from - 2));
                }
            }
            int64_t _t52 = (r & 2) == 0;
            if (_t52) {
                int64_t _t53 = !(isFieldAttacked_2(black, from + 1));
                _t52 = _t53;
            }
            if (_t52) {
                int64_t rook = slide_4(from + 3, 8, -1, 0);
                rook >>= from + 1;
                if ((rook & 3) == 3) {
                    result |= shiftLeft_2(1, (from + 2));
                }
            }
        }
    }
    return result;
}
int32_t i32_1(int64_t x) {
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
void init_0() {
    board->data[idx_2(0, _arrayLen(board))] = 3;
    board->data[idx_2(1, _arrayLen(board))] = 5;
    board->data[idx_2(2, _arrayLen(board))] = 4;
    board->data[idx_2(3, _arrayLen(board))] = 2;
    board->data[idx_2(4, _arrayLen(board))] = 1;
    board->data[idx_2(5, _arrayLen(board))] = 4;
    board->data[idx_2(6, _arrayLen(board))] = 5;
    board->data[idx_2(7, _arrayLen(board))] = 3;
    if (8 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                board->data[idx_2(i + 56, _arrayLen(board))] = board->data[idx_2(i, _arrayLen(board))];
                board->data[idx_2(i, _arrayLen(board))] += 6;
                board->data[idx_2(i + 8, _arrayLen(board))] = 12;
                board->data[idx_2(i + 48, _arrayLen(board))] = 6;
                int64_t _next = i + 1;
                if (_next >= 8) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
}
int64_t isBlack_1(int64_t p) {
    int64_t _r0 = p > 6;
    return _r0;
}
int64_t isFieldAttacked_2(int64_t black, int64_t pos) {
    if (64 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                int64_t b = board->data[idx_2(i, _arrayLen(board))];
                int64_t _t0 = b == 0;
                if (!(_t0)) {
                    int64_t _t1 = isBlack_1(b) == black;
                    _t0 = _t1;
                }
                if (_t0) {
                    int64_t _next = i + 1;
                    if (_next >= 64) {
                        break;
                    }
                    i = _next;
                    continue;
                }
                int64_t moves = getPossibleMoves_2(i, 1);
                if (((shiftRight_int_2(moves, pos)) & 1) == 1) {
                    return 1;
                }
                int64_t _next = i + 1;
                if (_next >= 64) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    return 0;
}
int64_t move_1(int64_t move) {
    int64_t source = (shiftRight_int_2(move, 16)) & 255;
    int64_t target = (shiftRight_int_2(move, 8)) & 255;
    int64_t _t0 = move_2(source, target);
    return _t0;
}
int64_t move_2(int64_t source, int64_t target) {
    turn += 1;
    int64_t captured = board->data[idx_2(target, _arrayLen(board))];
    int64_t oldCastlingFlags = castlingFlags;
    updateCastlingRights_1(source);
    updateCastlingRights_1(target);
    int64_t old = board->data[idx_2(source, _arrayLen(board))];
    board->data[idx_2(target, _arrayLen(board))] = old;
    int64_t p = getPiece_1(old);
    int64_t isBlack = isBlack_1(old);
    board->data[idx_2(source, _arrayLen(board))] = 0;
    int64_t oldPawnMoved = pawnMoved2;
    pawnMoved2 = 0;
    if (p == 6) {
        int64_t shift = (target & 7) - (source & 7);
        int64_t _t0 = shift != 0;
        if (_t0) {
            int64_t _t1 = captured == 0;
            _t0 = _t1;
        }
        if (_t0) {
            board->data[idx_2(source + shift, _arrayLen(board))] = 0;
        }
        int64_t _t2 = target <= 7;
        if (!(_t2)) {
            int64_t _t3 = target >= 56;
            _t2 = _t3;
        }
        if (_t2) {
            int64_t _t4 = 0;
            if (isBlack) {
                _t4 = 6;
            } else {
                _t4 = 0;
            }
            board->data[idx_2(target, _arrayLen(board))] = 2 + _t4;
        }
        int64_t _t5 = org_bau_Int_abs_1(source - target);
        if (_t5 == 16) {
            pawnMoved2 = target;
        }
    }
    if (p == 1) {
        int64_t _t6 = 0;
        if (isBlack) {
            _t6 = 0;
        } else {
            _t6 = 2;
        }
        castlingFlags |= shiftLeft_2(3, _t6);
        int64_t _t7 = org_bau_Int_abs_1((source & 7) - (target & 7));
        if (_t7 > 1) {
            if (target > source) {
                board->data[idx_2(target - 1, _arrayLen(board))] = board->data[idx_2(target + 1, _arrayLen(board))];
                board->data[idx_2(target + 1, _arrayLen(board))] = 0;
            } else {
                board->data[idx_2(target + 1, _arrayLen(board))] = board->data[idx_2(target - 2, _arrayLen(board))];
                board->data[idx_2(target - 2, _arrayLen(board))] = 0;
            }
        }
    }
    int64_t _r0 = (shiftLeft_2(old, 40)) | (shiftLeft_2(oldPawnMoved, 32)) | (shiftLeft_2(captured, 24)) | (shiftLeft_2(source, 16)) | (shiftLeft_2(target, 8)) | oldCastlingFlags;
    return _r0;
}
int64_t negamax_5(int64_t top, int64_t depth, int64_t black, int64_t alpha, int64_t beta) {
    int64_t best = -9223372036854775807;
    int64_t bestMove = 0;
    if (depth <= 0) {
        int64_t _t0 = evaluateBoard_1(black);
        best = _t0;
        int64_t _t1 = best >= beta;
        if (!(_t1)) {
            int64_t _t2 = depth < -1;
            _t1 = _t2;
        }
        if (_t1) {
            return best;
        }
    }
    if (2 > 0) {
        while (1 == 1) {
            int64_t phase = 0;
            while (1) {
                int64_t _t3 = depth <= 0;
                if (_t3) {
                    int64_t _t4 = phase == 1;
                    _t3 = _t4;
                }
                if (_t3) {
                    return best;
                }
                if (64 > 0) {
                    while (1 == 1) {
                        int64_t i = 0;
                        while (1) {
                            int64_t _t5 = board->data[idx_2(i, _arrayLen(board))] == 0;
                            if (!(_t5)) {
                                int64_t _t6 = isBlack_1(board->data[idx_2(i, _arrayLen(board))]) != black;
                                _t5 = _t6;
                            }
                            if (_t5) {
                                int64_t _next = i + 1;
                                if (_next >= 64) {
                                    break;
                                }
                                i = _next;
                                continue;
                            }
                            int64_t attackOnly = 0;
                            if (phase == 0) {
                                attackOnly = 1;
                            }
                            int64_t moves = getPossibleMoves_2(i, attackOnly);
                            while (moves != 0) {
                                int64_t target = org_bau_Int_numberOfTrailingZeros_1(moves);
                                moves ^= shiftLeft_2(1, target);
                                int64_t capture = 0;
                                if (board->data[idx_2(target, _arrayLen(board))] != 0) {
                                    capture = 1;
                                }
                                if (capture != attackOnly) {
                                    continue;
                                }
                                int64_t move = move_2(i, target);
                                int64_t _t7 = negamax_5(0, depth - 1, 1 - black, - beta, - alpha);
                                int64_t score = - _t7;
                                if (score > best) {
                                    bestMove = move;
                                    best = score;
                                    int64_t _t8 = org_bau_Int_max_2(alpha, score);
                                    alpha = _t8;
                                }
                                undo_1(move);
                                int64_t _t9 = !(top);
                                if (_t9) {
                                    int64_t _t10 = best >= beta;
                                    _t9 = _t10;
                                }
                                if (_t9) {
                                    break;
                                }
                            }
                            int64_t _next = i + 1;
                            if (_next >= 64) {
                                break;
                            }
                            i = _next;
                        }
                        break;
                    }
                }
                int64_t _next = phase + 1;
                if (_next >= 2) {
                    break;
                }
                phase = _next;
            }
            break;
        }
    }
    if (top) {
        return bestMove;
    }
    return best;
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
void org_bau_Env_atExit_1(void  (*callback_0)()) {
    atexit(callback_0);
}
void org_bau_Env_exit_1(int64_t code) {
    exit(code);
    printf("Exit code %lld; will now cause a stack overflow\n", (long long)code);
    org_bau_Env_exit_1(code);
}
int64_t org_bau_Int_abs_1(int64_t x) {
    if (x > 0) {
        return x;
    }
    int64_t _r0 = - x;
    return _r0;
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
int64_t org_bau_Int_bitCount_1(int64_t x) {
    int32_t _t0 = i32_1(x);
    int64_t _t1 = org_bau_Int_bitCountI32_1(_t0);
    int32_t _t2 = i32_1(shiftRight_int_2(x, 32));
    int64_t _t3 = org_bau_Int_bitCountI32_1(_t2);
    int64_t _r0 = _t1 + _t3;
    return _r0;
}
int64_t org_bau_Int_bitCountI32_1(int32_t x) {
    x -= (shiftRight_i32_2(x, 1)) & 1431655765;
    x = (x & 858993459) + ((shiftRight_i32_2(x, 2)) & 858993459);
    x = ((x + (shiftRight_i32_2(x, 4))) & 252645135) * 16843009;
    int64_t _r0 = shiftRight_i32_2(x, 24);
    return _r0;
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
int64_t org_bau_Int_max_2(int64_t a, int64_t b) {
    if (a > b) {
        return a;
    }
    return b;
}
int64_t org_bau_Int_min_2(int64_t a, int64_t b) {
    if (a < b) {
        return a;
    }
    return b;
}
int64_t org_bau_Int_numberOfTrailingZeros_1(int64_t x) {
    return x == 0 ? 64 : _ctzll(x);
    if (x == 0) {
        return 64;
    }
    int64_t _t0 = org_bau_Int_bitCount_1(x ^ (x - 1));
    int64_t _r0 = _t0 - 1;
    return _r0;
}
org_bau_String_StringBuilder* org_bau_String_StringBuilder_1(i8_array* data) {
    org_bau_String_StringBuilder* _t5 = org_bau_String_StringBuilder_new();
    _incUseStack(data);
    _t5->data = data;
    _t5->len = 0;
    return _t5;
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
int64_t org_bau_os_Terminal_isTerminal_0() {
    return isatty(0);
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
    org_bau_os_Terminal_termIos _t1 = org_bau_os_Terminal_termIos_new();
    _incUseStack(data);
    _t1.data = data;
    return _t1;
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
    org_bau_String_StringBuilder_append_2(buff, string_1031);
    org_bau_String_StringBuilder_append_2(buff, string_1032);
    if (8 > 0) {
        while (1 == 1) {
            int64_t y = 0;
            while (1) {
                i8_array* _t1 = org_bau_Int_intToString_1(8 - y);
                org_bau_String_StringBuilder_append_2(buff, _t1);
                org_bau_String_StringBuilder_append_2(buff, string_1033);
                if (8 > 0) {
                    while (1 == 1) {
                        int64_t x = 0;
                        while (1) {
                            int64_t b = board->data[idx_2(x + ( 8 * y ), _arrayLen(board))];
                            int64_t allowed = (b & 16) != 0;
                            b &= -17;
                            if (allowed) {
                                org_bau_String_StringBuilder_append_2(buff, string_1034);
                            } else if (( imod_2((x + y), 2) ) == 1) {
                                org_bau_String_StringBuilder_append_2(buff, string_1035);
                            }
                            int64_t _t2 = isBlack_1(b);
                            if (_t2) {
                                org_bau_String_StringBuilder_append_2(buff, string_1036);
                            } else {
                                org_bau_String_StringBuilder_append_2(buff, string_1037);
                            }
                            org_bau_String_StringBuilder_append_2(buff, string_1033);
                            if (b == 0) {
                                org_bau_String_StringBuilder_append_2(buff, string_1033);
                            } else {
                                i8_array* _t3 = i8_array_new(3);
                                _incUseStack(_t3);
                                i8_array* b2 = _t3;
                                b2->data[0] = 0xe2;
                                b2->data[1] = 0x99;
                                b2->data[2] = 148 + (b - 1);
                                org_bau_String_StringBuilder_append_2(buff, b2);
                                _decUseStack(b2, i8_array);
                                _decUseStack(_t3, i8_array);
                            }
                            org_bau_String_StringBuilder_append_2(buff, string_1033);
                            org_bau_String_StringBuilder_append_2(buff, string_1038);
                            int64_t _next = x + 1;
                            if (_next >= 8) {
                                break;
                            }
                            x = _next;
                        }
                        break;
                    }
                }
                org_bau_String_StringBuilder_append_2(buff, string_1039);
                int64_t _next = y + 1;
                if (_next >= 8) {
                    _decUseStack(_t1, i8_array);
                    break;
                }
                y = _next;
                _decUseStack(_t1, i8_array);
            }
            break;
        }
    }
    org_bau_String_StringBuilder_append_2(buff, string_1040);
    org_bau_String_StringBuilder_append_2(buff, string_1041);
    org_bau_String_StringBuilder_append_2(buff, string_1042);
    i8_array* _t4 = org_bau_Int_intToString_1(yy + 2);
    org_bau_String_StringBuilder_append_2(buff, _t4);
    org_bau_String_StringBuilder_append_2(buff, string_1043);
    i8_array* _t5 = org_bau_Int_intToString_1(( 1 + ( 3 * xx ) ) + 3);
    org_bau_String_StringBuilder_append_2(buff, _t5);
    org_bau_String_StringBuilder_append_2(buff, string_1008);
    if (showCursor) {
        org_bau_String_StringBuilder_append_2(buff, string_1044);
    }
    org_bau_os_Terminal_writeToTerminal_2(buff->data, buff->len);
    _decUseStack(_t5, i8_array);
    _decUseStack(_t4, i8_array);
    _decUseStack(buff, org_bau_String_StringBuilder);
    _decUseStack(_t0, i8_array);
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int32_t shiftRight_i32_2(int32_t a, int64_t b) {
    return ((uint32_t) a) >> b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
int64_t slide_4(int64_t from, int64_t maxDist, int64_t xo, int64_t yo) {
    int64_t x = from & 7;
    int64_t y = idiv_2(from, 8);
    int64_t isBlack = isBlack_1(board->data[idx_2(from, _arrayLen(board))]);
    int64_t result = 0;
    int64_t i = 1;
    while (1) {
        x += xo;
        y += yo;
        int64_t _t0 = i > maxDist;
        if (!(_t0)) {
            int64_t _t1 = x < 0;
            _t0 = _t1;
        }
        int64_t _t2 = _t0;
        if (!(_t2)) {
            int64_t _t3 = x > 7;
            _t2 = _t3;
        }
        int64_t _t4 = _t2;
        if (!(_t4)) {
            int64_t _t5 = y < 0;
            _t4 = _t5;
        }
        int64_t _t6 = _t4;
        if (!(_t6)) {
            int64_t _t7 = y > 7;
            _t6 = _t7;
        }
        if (_t6) {
            break;
        }
        int64_t p = x + ( y * 8 );
        int64_t b = board->data[idx_2(p, _arrayLen(board))];
        if (b != 0) {
            int64_t _t8 = isBlack_1(b);
            if (_t8 != isBlack) {
                result |= shiftLeft_2(1, p);
            }
            break;
        }
        result |= shiftLeft_2(1, p);
        i += 1;
    }
    return result;
}
void undo_1(int64_t move) {
    turn -= 1;
    int64_t old = (shiftRight_int_2(move, 40)) & 255;
    pawnMoved2 = (shiftRight_int_2(move, 32)) & 255;
    int64_t captured = (shiftRight_int_2(move, 24)) & 255;
    int64_t source = (shiftRight_int_2(move, 16)) & 255;
    int64_t target = (shiftRight_int_2(move, 8)) & 255;
    castlingFlags = move & 255;
    board->data[idx_2(target, _arrayLen(board))] = captured;
    board->data[idx_2(source, _arrayLen(board))] = old;
    int64_t _t0 = getPiece_1(old);
    int64_t _t2 = getPiece_1(old);
    if (_t0 == 1) {
        int64_t _t1 = org_bau_Int_abs_1((source & 7) - (target & 7));
        if (_t1 > 1) {
            if (target > source) {
                board->data[idx_2(target + 1, _arrayLen(board))] = board->data[idx_2(target - 1, _arrayLen(board))];
                board->data[idx_2(target - 1, _arrayLen(board))] = 0;
            } else {
                board->data[idx_2(target - 2, _arrayLen(board))] = board->data[idx_2(target + 1, _arrayLen(board))];
                board->data[idx_2(target + 1, _arrayLen(board))] = 0;
            }
        }
    } else if (_t2 == 6) {
        int64_t shift = (target & 7) - (source & 7);
        int64_t _t3 = shift != 0;
        if (_t3) {
            int64_t _t4 = captured == 0;
            _t3 = _t4;
        }
        if (_t3) {
            int64_t _t5 = 0;
            int64_t _t6 = isBlack_1(old);
            if (_t6) {
                _t5 = 0;
            } else {
                _t5 = 6;
            }
            board->data[idx_2(source + shift, _arrayLen(board))] = 6 + _t5;
        }
    }
}
void updateCastlingRights_1(int64_t pos) {
    int64_t p = board->data[idx_2(pos, _arrayLen(board))];
    int64_t _t0 = getPiece_1(p);
    if (_t0 == 3) {
        int64_t _t1 = (pos & 7) == 0;
        if (!(_t1)) {
            int64_t _t2 = (pos & 7) == 7;
            _t1 = _t2;
        }
        if (_t1) {
            int64_t _t3 = 0;
            if ((pos & 7) == 0) {
                _t3 = 1;
            } else {
                _t3 = 2;
            }
            int64_t which = _t3;
            int64_t _t4 = 0;
            int64_t _t5 = isBlack_1(p);
            if (_t5) {
                _t4 = 0;
            } else {
                _t4 = 2;
            }
            castlingFlags |= shiftLeft_2(which, _t4);
        }
    }
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1005 = str_const("Exit code ", 10);
    string_1006 = str_const("; will now cause a stack overflow", 33);
    string_1008 = str_const("H", 1);
    string_1031 = str_const("\x1b[?25l\x1b[H\x1b[0m", 13);
    string_1032 = str_const("   a  b  c  d  e  f  g  h  \x0d\n", 29);
    string_1033 = str_const(" ", 1);
    string_1034 = str_const("\x1b[30;100m", 9);
    string_1035 = str_const("\x1b[30;107m", 9);
    string_1036 = str_const("\x1b[31m", 5);
    string_1037 = str_const("\x1b[94m", 5);
    string_1038 = str_const("\x1b[0m", 4);
    string_1039 = str_const(" \x0d\n", 3);
    string_1040 = str_const("  arrows:select space:move\x0d\n", 28);
    string_1041 = str_const("  u:undo  s:switch  q:quit", 26);
    string_1042 = str_const("\x1b[", 2);
    string_1043 = str_const(";", 1);
    string_1044 = str_const("\x1b[?25h", 6);
    string_1048 = str_const("Not a terminal", 14);
    _main();
    return 0;
}
void _main() {
    randomSeed = 0;
    MIN_INT = 0x8000000000000000;
    MAX_INT = 0x7fffffffffffffff;
    MIN_I32 = -2147483648;
    MAX_I32 = 4294967295;
    queueByte = 0;
    i8_array* _t3 = i8_array_new(0);
    oldTermIos = org_bau_os_Terminal_termIos_1(_t3);
    refreshScreenCallback_0 = org_bau_os_Terminal_doNothing_0;
    xx = 3;
    yy = 6;
    state = 0;
    showCursor = 1;
    from = 3;
    blackTurn = 0;
    lastBlack = 0;
    lastWhite = 0;
    KING = 1;
    QUEEN = 2;
    ROOK = 3;
    BISHOP = 4;
    KNIGHT = 5;
    PAWN = 6;
    BLACK = 6;
    int_array* _t9 = int_array_new(64);
    _incUseStack(_t9);
    board = _t9;
    castlingFlags = 0;
    pawnMoved2 = 0;
    turn = 0;
    int64_t _t10 = org_bau_os_Terminal_isTerminal_0();
    if (!(_t10)) {
        printf("Not a terminal\n");
        return;
    }
    org_bau_os_Terminal_enableRawMode_1(refreshScreen_0);
    init_0();
    while (1) {
        refreshScreen_0();
        int64_t key = org_bau_os_Terminal_readEditorKey_0();
        if (key < 0) {
            break;
        }
        if (key == 0) {
            continue;
        }
        int64_t _t11 = key;
        if (_t11 == 113) {
            break;
        } else if (_t11 == 32) {
            if (state == 0) {
                int64_t p = xx + ( 8 * yy );
                int64_t _t12 = board->data[idx_2(p, _arrayLen(board))] == 0;
                if (!(_t12)) {
                    int64_t _t13 = isBlack_1(board->data[idx_2(p, _arrayLen(board))]) != blackTurn;
                    _t12 = _t13;
                }
                if (_t12) {
                    continue;
                }
                from = p;
                state = 1;
                int64_t moves = getPossibleMoves_2(p, 0);
                while (moves != 0) {
                    int64_t target = org_bau_Int_numberOfTrailingZeros_1(moves);
                    board->data[idx_2(target, _arrayLen(board))] |= 16;
                    moves ^= shiftLeft_2(1, target);
                }
                refreshScreen_0();
            } else if (state == 1) {
                int64_t p = xx + ( 8 * yy );
                int64_t allowed = (board->data[idx_2(p, _arrayLen(board))] & 16) != 0;
                if (64 > 0) {
                    while (1 == 1) {
                        int64_t i = 0;
                        while (1) {
                            board->data[idx_2(i, _arrayLen(board))] &= -17;
                            int64_t _next = i + 1;
                            if (_next >= 64) {
                                break;
                            }
                            i = _next;
                        }
                        break;
                    }
                }
                if (allowed) {
                    int64_t _t14 = move_2(from, p);
                    lastWhite = _t14;
                    showCursor = 0;
                    refreshScreen_0();
                    int64_t move = negamax_5(1, 5, !(blackTurn), -9223372036854775807, 9223372036854775807);
                    if (move != 0) {
                        lastBlack = move;
                        int64_t _t15 = move_1(move);
                        ;
                    }
                }
                showCursor = 1;
                refreshScreen_0();
                state = 0;
            }
        } else if (_t11 == 99) {
            int64_t p = xx + ( 8 * yy );
            if (state == 0) {
                from = p;
                state = 1;
            } else {
                int64_t _t16 = move_2(from, p);
                lastWhite = _t16;
                refreshScreen_0();
                state = 0;
            }
        } else if (_t11 == 117) {
            int64_t _t17 = lastBlack != 0;
            if (_t17) {
                int64_t _t18 = lastWhite != 0;
                _t17 = _t18;
            }
            if (_t17) {
                undo_1(lastBlack);
                undo_1(lastWhite);
                refreshScreen_0();
            }
            lastBlack = 0;
            lastWhite = 0;
        } else if (_t11 == 115) {
            blackTurn = !(blackTurn);
            showCursor = 0;
            refreshScreen_0();
            int64_t move = negamax_5(1, 5, !(blackTurn), -9223372036854775807, 9223372036854775807);
            if (move != 0) {
                int64_t _t19 = move_1(move);
                ;
            }
            showCursor = 1;
            refreshScreen_0();
            state = 0;
        } else if (_t11 == 1001) {
            int64_t _t20 = org_bau_Int_min_2(7, xx + 1);
            xx = _t20;
        } else if (_t11 == 1000) {
            int64_t _t21 = org_bau_Int_max_2(0, xx - 1);
            xx = _t21;
        } else if (_t11 == 1002) {
            int64_t _t22 = org_bau_Int_max_2(0, yy - 1);
            yy = _t22;
        } else if (_t11 == 1003) {
            int64_t _t23 = org_bau_Int_min_2(7, yy + 1);
            yy = _t23;
        }
    }
    _decUseStack(_t3, i8_array);
    org_bau_os_Terminal_termIos_free(&oldTermIos);
    _decUseStack(_t9, int_array);
    _decUseStack(board, int_array);
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

*/
