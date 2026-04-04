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
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
double float_1(double x);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
int64_t imod_2(int64_t a, int64_t b);
int64_t int_1(int64_t x);
void operations_2(int64_t a, int64_t b);
int64_t org_bau_Int_powInt_2(int64_t base, int64_t exponent);
int64_t org_bau_Int_sqrtInt_1(int64_t x);
double org_bau_Math_abs_1(double x);
double org_bau_Math_acos_1(double x);
double org_bau_Math_asin_1(double x);
double org_bau_Math_atan_1(double x);
double org_bau_Math_ceil_1(double x);
int64_t org_bau_Math_convertDoubleToLongBits_1(double x);
double org_bau_Math_convertLongBitsToDouble_1(int64_t x);
double org_bau_Math_cos_1(double x);
double org_bau_Math_exp_1(double x);
double org_bau_Math_floor_1(double x);
int64_t org_bau_Math_isNegativeZero_1(double x);
int64_t org_bau_Math_isNotANumber_1(double x);
double org_bau_Math_log_1(double x);
double org_bau_Math_log10_1(double x);
double org_bau_Math_maxFloat_2(double a, double b);
double org_bau_Math_minFloat_2(double a, double b);
double org_bau_Math_pow_2(double x, double y);
double org_bau_Math_round_1(double x);
double org_bau_Math_signum_1(double x);
double org_bau_Math_sin_1(double x);
double org_bau_Math_sqrt_1(double x);
double org_bau_Math_tan_1(double x);
i8_array* org_bau_Std_convertFloatToI8Array_1(double n);
i8_array* org_bau_Std_convertIntToI8Array_1(int64_t x);
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
void i8_array_free(i8_array* x);
void i8_array_free_0(i8_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void i8_array_free(i8_array* x) {
    _registerAndMaybeDrain(x, (void(*)(void*))i8_array_free_0);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = _malloc(sizeof(char) * len);
    memcpy(result->data, data, sizeof(char) * len);
    return result;
}
i8_array* string_1002;
i8_array* string_1003;
i8_array* string_1004;
i8_array* string_1019;
i8_array* string_1020;
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
i8_array* string_1038;
i8_array* string_1039;
i8_array* string_1040;
i8_array* string_1041;
i8_array* string_1042;
i8_array* string_1043;
i8_array* string_1044;
i8_array* string_1045;
i8_array* string_1046;
i8_array* string_1047;
i8_array* string_1048;
i8_array* string_1049;
i8_array* string_1050;
i8_array* string_1051;
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
void operations_2(int64_t a, int64_t b) {
    if (b != 0) {
        printf("%lld  / %lld = %lld\n", (long long)a, (long long)b, (long long)(idiv_2(a, b)));
        printf("%lld  %% %lld = %lld\n", (long long)a, (long long)b, (long long)(imod_2(a, b)));
    }
    printf("%lld >> %lld = %lld\n", (long long)a, (long long)b, (long long)(shiftRight_int_2(a, b)));
    printf("%lld << %lld = %lld\n", (long long)a, (long long)b, (long long)(shiftLeft_2(a, b)));
}
int64_t org_bau_Int_powInt_2(int64_t base, int64_t exponent) {
    if (exponent < 0) {
        return -1;
    }
    int64_t result = 1;
    int64_t b = base;
    while (exponent > 0) {
        if ((exponent & 1) == 1) {
            result = result * b;
            int64_t _t0 = result > 2147483648;
            if (!(_t0)) {
                int64_t _t1 = result < -2147483648;
                _t0 = _t1;
            }
            if (_t0) {
                return -1;
            }
        }
        exponent = shiftRight_int_2(exponent, 1);
        if (exponent > 0) {
            b = b * b;
            int64_t _t2 = b > 2147483648;
            if (!(_t2)) {
                int64_t _t3 = b < -2147483648;
                _t2 = _t3;
            }
            if (_t2) {
                return -1;
            }
        }
    }
    return result;
}
int64_t org_bau_Int_sqrtInt_1(int64_t x) {
    if (x < 0) {
        return 0;
    }
    int64_t g = 2147483648;
    int64_t c = g;
    while (1) {
        int64_t t = g * g;
        int64_t _t0 = t > x;
        if (!(_t0)) {
            int64_t _t1 = t < 0;
            _t0 = _t1;
        }
        if (_t0) {
            g = g ^ c;
        }
        c = shiftRight_int_2(c, 1);
        if (c == 0) {
            break;
        }
        g = g | c;
    }
    return g;
}
double org_bau_Math_abs_1(double x) {
    return fabs(x);
    if (x >= 0) {
        return x;
    }
    double _r0 = - x;
    return _r0;
}
double org_bau_Math_acos_1(double x) {
    return acos(x);
    double _t0 = org_bau_Math_asin_1(x);
    double _r0 = 1.5707963267948966 - _t0;
    return _r0;
}
double org_bau_Math_asin_1(double x) {
    return asin(x);
    double _t0 = org_bau_Math_sqrt_1(1 - ( x * x ));
    double _t1 = org_bau_Math_atan_1(x / _t0);
    return _t1;
}
double org_bau_Math_atan_1(double x) {
    return atan(x);
    double _t0 = org_bau_Math_maxFloat_2(-1.0E16, x);
    double _t1 = org_bau_Math_minFloat_2(_t0, 1.0E16);
    x = _t1;
    double _t2 = org_bau_Math_abs_1(x);
    if (_t2 >= 0.4) {
        double _t3 = org_bau_Math_sqrt_1(1 + ( x * x ));
        double _t4 = org_bau_Math_atan_1(x / (1 + _t3));
        double _r0 = 2 * _t4;
        return _r0;
    }
    double approx = x;
    int64_t sign = -1;
    double p = x;
    while (1 == 1) {
        int64_t n = 3;
        while (n < 32) {
            p = p * ( x * x );
            approx = approx + ( sign * p / n );
            sign = - sign;
            n = n + 1;
            n = n + 1;
        }
        break;
    }
    return approx;
}
double org_bau_Math_ceil_1(double x) {
    return ceil(x);
    double _t0 = org_bau_Math_floor_1(- x);
    double _r0 = - _t0;
    return _r0;
}
int64_t org_bau_Math_convertDoubleToLongBits_1(double x) {
    uint64_t y;
    memcpy(&y, &x, sizeof y);
    return y;
    if (x == 0) {
        int64_t _t0 = org_bau_Math_isNegativeZero_1(x);
        if (_t0) {
            return (-9223372036854775807LL-1LL);
        }
        return 0;
    }
    if (x < 0) {
        int64_t _t1 = org_bau_Math_convertDoubleToLongBits_1(- x);
        int64_t _r0 = _t1 | (-9223372036854775807LL-1LL);
        return _r0;
    }
    if (x == (1.0 / 0.0)) {
        return 9218868437227405312;
    }
    int64_t _t2 = org_bau_Math_isNotANumber_1(x);
    if (_t2) {
        return 9221120237041090560;
    }
    int64_t exp = 0;
    while (x >= 4294967296) {
        exp = exp + 32;
        x = x / 4294967296;
    }
    while (x >= 2) {
        exp = exp + 1;
        x = x / 2;
    }
    while (1 == 1) {
        int64_t _t3 = x < 2.3283064365386963E-10;
        if (_t3) {
            int64_t _t4 = exp > -990;
            _t3 = _t4;
        }
        if (!(_t3)) {
            break;
        }
        exp = exp - 32;
        x = x * 4294967296;
    }
    while (1 == 1) {
        int64_t _t5 = x < 1;
        if (_t5) {
            int64_t _t6 = exp > -1022;
            _t5 = _t6;
        }
        if (!(_t5)) {
            break;
        }
        exp = exp - 1;
        x = x + x;
    }
    int64_t _t7 = int_1(x * 4503599627370496);
    int64_t fraction = shiftRight_int_2(shiftLeft_2(_t7, 11), 11);
    int64_t _r1 = (shiftLeft_2((exp + 1022), 52)) + fraction;
    return _r1;
}
double org_bau_Math_convertLongBitsToDouble_1(int64_t x) {
    double y;
    memcpy(&y, &x, sizeof y);
    return y;
    int64_t sign = shiftRight_int_2(x, 63);
    int64_t exp = (shiftRight_int_2(x, 52)) & 2047;
    if (x == 0) {
        return 0.0;
    } else {
        if (x == (-9223372036854775807LL-1LL)) {
            return -0.0;
        }
    }
    int64_t fraction = shiftRight_int_2(shiftLeft_2(x, 12), 12);
    if (exp == 2047) {
        if (fraction != 0) {
            return (0.0 / 0.0);
        } else {
            if (sign == 0) {
                return (1.0 / 0.0);
            }
        }
        return (-1.0 / 0.0);
    }
    double result = 0.0;
    while (1 == 1) {
        int64_t i = 0;
        while (i < 52) {
            if ((fraction & 1) == 1) {
                result = result + 1;
            }
            result = result / 2;
            fraction = shiftRight_int_2(fraction, 1);
            i = i + 1;
        }
        break;
    }
    if (exp == 0) {
        result = result * 2;
    } else {
        result = result + 1;
    }
    exp = exp - 1023;
    while (exp <= -32) {
        result = result / 4294967296;
        exp = exp + 32;
    }
    while (exp < 0) {
        result = result / 2;
        exp = exp + 1;
    }
    while (exp >= 32) {
        result = result * 4294967296;
        exp = exp - 32;
    }
    while (exp > 0) {
        result = result + result;
        exp = -1;
    }
    if (sign == 0) {
        return result;
    }
    double _r0 = - result;
    return _r0;
}
double org_bau_Math_cos_1(double x) {
    return cos(x);
    double _t0 = org_bau_Math_sin_1(x + 1.5707963267948966);
    return _t0;
}
double org_bau_Math_exp_1(double x) {
    return exp(x);
    int64_t _t0 = org_bau_Math_isNotANumber_1(x);
    int64_t _t1 = _t0;
    if (!(_t1)) {
        int64_t _t2 = x == (1.0 / 0.0);
        _t1 = _t2;
    }
    if (_t1) {
        return x;
    } else {
        if (x < 0) {
            double _t3 = org_bau_Math_exp_1(- x);
            double _r0 = 1 / _t3;
            return _r0;
        }
    }
    if (x > 2) {
        double r = org_bau_Math_exp_1(x / 2);
        double _r1 = r * r;
        return _r1;
    }
    double approx = 1.0;
    double term = 1.0;
    while (1 == 1) {
        int64_t i = 1;
        while (i < 22) {
            term = term * ( x / i );
            approx = approx + term;
            i = i + 1;
        }
        break;
    }
    return approx;
}
double org_bau_Math_floor_1(double x) {
    return floor(x);
    int64_t _t0 = x <= (-9223372036854775807LL-1LL);
    if (!(_t0)) {
        int64_t _t1 = x >= 9223372036854775807;
        _t0 = _t1;
    }
    int64_t _t2 = _t0;
    if (!(_t2)) {
        int64_t _t3 = org_bau_Math_isNotANumber_1(x);
        _t2 = _t3;
    }
    int64_t _t4 = _t2;
    if (!(_t4)) {
        int64_t _t5 = x == 0;
        _t4 = _t5;
    }
    if (_t4) {
        return x;
    }
    int64_t i = int_1(x);
    double d = float_1(i);
    int64_t _t6 = d == x;
    if (!(_t6)) {
        int64_t _t7 = x >= 0;
        _t6 = _t7;
    }
    if (_t6) {
        return d;
    }
    double _r0 = d - 1;
    return _r0;
}
int64_t org_bau_Math_isNegativeZero_1(double x) {
    int64_t _t0 = (x == 0.0);
    if (_t0) {
        int64_t _t1 = (( 1.0 / x ) == (-1.0 / 0.0));
        _t0 = _t1;
    }
    return _t0;
}
int64_t org_bau_Math_isNotANumber_1(double x) {
    return isnan(x);
    int64_t _r0 = x != x;
    return _r0;
}
double org_bau_Math_log_1(double x) {
    return log(x);
    if (x == 0) {
        return (-1.0 / 0.0);
    } else {
        if (x == (1.0 / 0.0)) {
            return x;
        } else {
            int64_t _t0 = x <= 0;
            if (!(_t0)) {
                int64_t _t1 = org_bau_Math_isNotANumber_1(x);
                _t0 = _t1;
            }
            if (_t0) {
                return (0.0 / 0.0);
            }
        }
    }
    if (x < 0.7) {
        double _t2 = org_bau_Math_log_1(2 * x);
        double _r0 = _t2 - 0.6931471805599453;
        return _r0;
    }
    if (x >= 1.5) {
        double _t3 = org_bau_Math_log_1(x / 2);
        double _r1 = _t3 + 0.6931471805599453;
        return _r1;
    }
    double base = x - 1;
    int64_t sign = 1;
    double term = base;
    double result = term;
    while (1 == 1) {
        int64_t i = 2;
        while (i < 30) {
            sign = - sign;
            term = term * base;
            result = result + ( sign * term / i );
            i = i + 1;
        }
        break;
    }
    return result;
}
double org_bau_Math_log10_1(double x) {
    return log10(x);
    double _t0 = org_bau_Math_log_1(x);
    double _r0 = _t0 / 2.302585092994046;
    return _r0;
}
double org_bau_Math_maxFloat_2(double a, double b) {
    return fmax(a, b);
    int64_t _t0 = org_bau_Math_isNotANumber_1(a);
    if (_t0) {
        return a;
    }
    int64_t _t1 = a == b;
    if (_t1) {
        int64_t _t2 = a == 0;
        _t1 = _t2;
    }
    int64_t _t3 = _t1;
    if (_t3) {
        int64_t _t4 = org_bau_Math_isNegativeZero_1(a);
        _t3 = _t4;
    }
    if (_t3) {
        return b;
    }
    if (a >= b) {
        return a;
    }
    return b;
}
double org_bau_Math_minFloat_2(double a, double b) {
    return fmin(a, b);
    int64_t _t0 = org_bau_Math_isNotANumber_1(a);
    if (_t0) {
        return a;
    }
    int64_t _t1 = a == b;
    if (_t1) {
        int64_t _t2 = a == 0;
        _t1 = _t2;
    }
    int64_t _t3 = _t1;
    if (_t3) {
        int64_t _t4 = org_bau_Math_isNegativeZero_1(b);
        _t3 = _t4;
    }
    if (_t3) {
        return b;
    }
    if (a <= b) {
        return a;
    }
    return b;
}
double org_bau_Math_pow_2(double x, double y) {
    return pow(x, y);
    int64_t x2 = int_1(x);
    int64_t y2 = int_1(y);
    double _t0 = float_1(x2);
    int64_t _t1 = _t0 == x;
    if (_t1) {
        int64_t _t2 = float_1(y2) == y;
        _t1 = _t2;
    }
    if (_t1) {
        int64_t r = org_bau_Int_powInt_2(x2, y2);
        if (r != -1) {
            return r;
        }
    }
    int64_t _t3 = x > 0;
    if (_t3) {
        int64_t _t4 = y > 0;
        _t3 = _t4;
    }
    if (_t3) {
        double _t5 = org_bau_Math_log_1(x);
        double _t6 = org_bau_Math_exp_1(y * _t5);
        return _t6;
    }
    if (y == 0.0) {
        return 1.0;
    }
    int64_t _t7 = org_bau_Math_isNotANumber_1(x);
    int64_t _t8 = _t7;
    if (!(_t8)) {
        int64_t _t9 = org_bau_Math_isNotANumber_1(y);
        _t8 = _t9;
    }
    if (_t8) {
        return (0.0 / 0.0);
    }
    double absX = org_bau_Math_abs_1(x);
    if (y == (-1.0 / 0.0)) {
        if (absX > 1.0) {
            return 0.0;
        }
        if (absX == 1.0) {
            return (0.0 / 0.0);
        }
        return (1.0 / 0.0);
    }
    int64_t yy = int_1(y);
    int64_t _t10 = (yy == y);
    if (_t10) {
        int64_t _t11 = ((yy & 1) == 1);
        _t10 = _t11;
    }
    int64_t odd = _t10;
    int64_t _t12 = org_bau_Math_isNegativeZero_1(x);
    int64_t _t13 = _t12;
    if (_t13) {
        int64_t _t14 = yy == y;
        _t13 = _t14;
    }
    if (_t13) {
        if (odd) {
            double _t15 = org_bau_Math_pow_2(- x, y);
            double _r0 = - _t15;
            return _r0;
        }
        double _t16 = org_bau_Math_pow_2(- x, y);
        return _t16;
    }
    if (x >= 0) {
        double _t17 = org_bau_Math_log_1(x);
        double _t18 = org_bau_Math_exp_1(y * _t17);
        return _t18;
    }
    if (odd) {
        double _t19 = org_bau_Math_pow_2(- x, y);
        double _r1 = - _t19;
        return _r1;
    }
    if (x == (-1.0 / 0.0)) {
        if (y < 0) {
            return 0.0;
        }
        return (1.0 / 0.0);
    }
    if (yy == y) {
        double _t20 = org_bau_Math_pow_2(- x, y);
        return _t20;
    }
    double _t21 = org_bau_Math_abs_1(y);
    if (_t21 > 1.0E19) {
        if (y > 0) {
            double _t22 = org_bau_Math_pow_2(- x, y);
            return _t22;
        }
        if (x == -1) {
            return 1;
        }
        if (absX > 1) {
            return 0;
        }
        return (1.0 / 0.0);
    }
    return (0.0 / 0.0);
}
double org_bau_Math_round_1(double x) {
    return floor(x + 0.5);
    int64_t _t0 = org_bau_Math_isNotANumber_1(x);
    if (_t0) {
        return 0;
    }
    if (x <= (-9223372036854775807LL-1LL)) {
        return (-9223372036854775807LL-1LL);
    }
    if (x >= 9223372036854775807) {
        return 9223372036854775807;
    }
    double _t1 = org_bau_Math_floor_1(x + 0.5);
    int64_t _t2 = int_1(_t1);
    return _t2;
}
double org_bau_Math_signum_1(double x) {
    int64_t _t0 = x == 0.0;
    if (!(_t0)) {
        int64_t _t1 = org_bau_Math_isNotANumber_1(x);
        _t0 = _t1;
    }
    if (_t0) {
        return x;
    }
    if (x >= 0) {
        return 1;
    }
    return -1;
}
double org_bau_Math_sin_1(double x) {
    return sin(x);
    if (x == (1.0 / 0.0)) {
        return (0.0 / 0.0);
    }
    double _t0 = org_bau_Math_abs_1(x);
    if (_t0 < 1.0E-8) {
        return x;
    }
    if (x < 0) {
        double _t1 = org_bau_Math_sin_1(- x);
        double _r0 = - _t1;
        return _r0;
    }
    if (x > 6.283185307179586) {
        double _t2 = org_bau_Math_floor_1(x / 6.283185307179586);
        x = x - ( _t2 * 6.283185307179586 );
    }
    if (x > 3.141592653589793) {
        double _t3 = org_bau_Math_sin_1(x - 3.141592653589793);
        double _r1 = - _t3;
        return _r1;
    }
    if (x > 1.5707963267948966) {
        x = 3.141592653589793 - x;
    }
    double approx = 0.0;
    int64_t sign = 1;
    double p = x;
    while (1 == 1) {
        int64_t n = 2;
        while (n < 24) {
            approx = approx + ( sign * p );
            p = p * ( x * x / (n * (n + 1)) );
            sign = - sign;
            n = n + 1;
            n = n + 1;
        }
        break;
    }
    return approx;
}
double org_bau_Math_sqrt_1(double x) {
    return sqrt(x);
    int64_t x2 = int_1(x);
    int64_t _t0 = x2 > 0;
    if (_t0) {
        int64_t _t1 = x2 == x;
        _t0 = _t1;
    }
    if (_t0) {
        int64_t exact = org_bau_Int_sqrtInt_1(x2);
        if (( exact * exact ) == x) {
            return exact;
        }
    }
    double _t2 = org_bau_Math_log_1(x);
    double _t3 = org_bau_Math_exp_1(_t2 / 2);
    return _t3;
}
double org_bau_Math_tan_1(double x) {
    return tan(x);
    double _t0 = org_bau_Math_sin_1(x);
    double _t1 = org_bau_Math_cos_1(x);
    double _r0 = _t0 / _t1;
    return _r0;
}
i8_array* org_bau_Std_convertFloatToI8Array_1(double n) {
    if (n != n) {
        return string_1002;
    } else {
        if (n == (1.0 / 0.0)) {
            return string_1003;
        } else {
            if (n == (-1.0 / 0.0)) {
                return string_1004;
            }
        }
    }
    int64_t pos = 0;
    int64_t e = 0;
    if (n < 0) {
        pos = 1;
        n = - n;
    }
    int64_t _t1 = n != 0;
    if (_t1) {
        int64_t _t2 = ((n >= 1.0E8) || (n <= 0.001));
        _t1 = _t2;
    }
    if (_t1) {
        while (n >= 10) {
            n = n / 10;
            e = e + 1;
        }
        while (n < 1) {
            n = n * 10;
            e = e - 1;
        }
    }
    int64_t _t3 = int_1(n);
    i8_array* aa = org_bau_Std_convertIntToI8Array_1(_t3);
    int64_t _t4 = int_1(n);
    int64_t b = int_1(((n - _t4) * 1000000000000000000) + 1000000000000000000);
    while (1 == 1) {
        int64_t _t5 = b >= 100;
        if (_t5) {
            int64_t _t6 = ( imod_2(b, 10) ) == 0;
            _t5 = _t6;
        }
        if (!(_t5)) {
            break;
        }
        b = idiv_2(b, 10);
    }
    i8_array* bb = org_bau_Std_convertIntToI8Array_1(b);
    i8_array* ee = org_bau_Std_convertIntToI8Array_1(e);
    int64_t len = ( pos + _arrayLen(aa) ) + _arrayLen(bb);
    if (e != 0) {
        len = len + ( 1 + _arrayLen(ee) );
    }
    i8_array* _t7 = i8_array_new(len);
    _incUseStack(_t7);
    i8_array* buff = _t7;
    buff->data[idx_2(0, _arrayLen(buff))] = 45;
    if (_arrayLen(aa) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                buff->data[idx_2(pos + i, _arrayLen(buff))] = aa->data[i];
                if (( i + 1 ) >= _arrayLen(aa)) {
                    break;
                }
                i = i + 1;
            }
            break;
        }
    }
    if (_arrayLen(bb) > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                buff->data[idx_2(( pos + _arrayLen(aa) ) + i, _arrayLen(buff))] = bb->data[i];
                if (( i + 1 ) >= _arrayLen(bb)) {
                    break;
                }
                i = i + 1;
            }
            break;
        }
    }
    buff->data[idx_2(pos + _arrayLen(aa), _arrayLen(buff))] = 46;
    if (e != 0) {
        buff->data[idx_2(( pos + _arrayLen(aa) ) + _arrayLen(bb), _arrayLen(buff))] = 101;
        if (_arrayLen(ee) > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    buff->data[idx_2(( pos + _arrayLen(aa) ) + _arrayLen(bb) + 1 + i, _arrayLen(buff))] = ee->data[i];
                    if (( i + 1 ) >= _arrayLen(ee)) {
                        break;
                    }
                    i = i + 1;
                }
                break;
            }
        }
    }
    _decUseStack(_t7, i8_array);
    _decUseStack(ee, i8_array);
    _decUseStack(bb, i8_array);
    _decUseStack(aa, i8_array);
    return buff;
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
    i8_array* _t0 = i8_array_new(len);
    _incUseStack(_t0);
    i8_array* buff = _t0;
    buff->data[idx_2(0, _arrayLen(buff))] = 45;
    while (1) {
        len = len - 1;
        buff->data[idx_2(len, _arrayLen(buff))] = 48 - (imod_2(x, 10));
        x = idiv_2(x, 10);
        if (x >= 0) {
            break;
        }
    }
    _decUseStack(_t0, i8_array);
    return buff;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
void _main();
int main(int _argc, char *_argv[]) {
    tmmalloc_init();
    __argc = _argc;
    __argv = _argv;
    string_1002 = str_const("NaN", 3);
    string_1003 = str_const("Infinity", 8);
    string_1004 = str_const("-Infinity", 9);
    string_1019 = str_const("  / ", 4);
    string_1020 = str_const(" = ", 3);
    string_1021 = str_const("  % ", 4);
    string_1022 = str_const(" >> ", 4);
    string_1023 = str_const(" << ", 4);
    string_1024 = str_const("NaN  ", 5);
    string_1025 = str_const("Inf  ", 5);
    string_1026 = str_const("-Inf ", 5);
    string_1027 = str_const("0    ", 5);
    string_1028 = str_const(" ", 1);
    string_1029 = str_const(", ", 2);
    string_1030 = str_const("signum ", 7);
    string_1031 = str_const("abs    ", 7);
    string_1032 = str_const("floor  ", 7);
    string_1033 = str_const("ceil   ", 7);
    string_1034 = str_const("round  ", 7);
    string_1035 = str_const("exp    ", 7);
    string_1036 = str_const("log    ", 7);
    string_1037 = str_const("log10  ", 7);
    string_1038 = str_const("sqrt   ", 7);
    string_1039 = str_const("sin    ", 7);
    string_1040 = str_const("cos    ", 7);
    string_1041 = str_const("tan    ", 7);
    string_1042 = str_const("atan   ", 7);
    string_1043 = str_const("asin   ", 7);
    string_1044 = str_const("acos   ", 7);
    string_1045 = str_const("isNotANumber   ", 15);
    string_1046 = str_const("isNegativeZero ", 15);
    string_1047 = str_const("convertDoubleToLongBits ", 24);
    string_1048 = str_const("convertLongBitsToDouble ", 24);
    string_1049 = str_const("pow ", 4);
    string_1050 = str_const("min ", 4);
    string_1051 = str_const("max ", 4);
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
    i8_array* _t0 = org_bau_Std_convertFloatToI8Array_1((0.0 / 0.0));
    printf("NaN  %.*s\n", _arrayLen(_t0), _t0->data);
    i8_array* _t1 = org_bau_Std_convertFloatToI8Array_1((1.0 / 0.0));
    printf("Inf  %.*s\n", _arrayLen(_t1), _t1->data);
    i8_array* _t2 = org_bau_Std_convertFloatToI8Array_1((-1.0 / 0.0));
    printf("-Inf %.*s\n", _arrayLen(_t2), _t2->data);
    i8_array* _t3 = org_bau_Std_convertFloatToI8Array_1(0);
    printf("0    %.*s\n", _arrayLen(_t3), _t3->data);
    double x = 1.0;
    if (25 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                i8_array* _t4 = org_bau_Std_convertFloatToI8Array_1(x);
                i8_array* _t5 = org_bau_Std_convertFloatToI8Array_1(- x);
                printf("%lld %.*s, %.*s\n", (long long)i, _arrayLen(_t4), _t4->data, _arrayLen(_t5), _t5->data);
                x = x * 12345678901234;
                if (( i + 1 ) >= 25) {
                    _decUseStack(_t5, i8_array);
                    _decUseStack(_t4, i8_array);
                    break;
                }
                i = i + 1;
                _decUseStack(_t5, i8_array);
                _decUseStack(_t4, i8_array);
            }
            break;
        }
    }
    x = 10;
    if (25 > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                i8_array* _t6 = org_bau_Std_convertFloatToI8Array_1(x);
                i8_array* _t7 = org_bau_Std_convertFloatToI8Array_1(- x);
                printf("%lld %.*s, %.*s\n", (long long)i, _arrayLen(_t6), _t6->data, _arrayLen(_t7), _t7->data);
                x = x * 12;
                if (( i + 1 ) >= 25) {
                    _decUseStack(_t7, i8_array);
                    _decUseStack(_t6, i8_array);
                    break;
                }
                i = i + 1;
                _decUseStack(_t7, i8_array);
                _decUseStack(_t6, i8_array);
            }
            break;
        }
    }
    operations_2(0, 0);
    operations_2(1, 0);
    operations_2(-1, 0);
    operations_2(3, -50);
    x = -1.0;
    x = -1;
    while (x <= 1) {
        double _t8 = org_bau_Math_signum_1(x);
        printf("signum %.9f = %.9f\n", x, _t8);
        double _t9 = org_bau_Math_abs_1(x);
        printf("abs    %.9f = %.9f\n", x, _t9);
        double _t10 = org_bau_Math_floor_1(x);
        printf("floor  %.9f = %.9f\n", x, _t10);
        double _t11 = org_bau_Math_ceil_1(x);
        printf("ceil   %.9f = %.9f\n", x, _t11);
        double _t12 = org_bau_Math_round_1(x);
        printf("round  %.9f = %.9f\n", x, _t12);
        double _t13 = org_bau_Math_exp_1(x);
        printf("exp    %.9f = %.9f\n", x, _t13);
        double _t14 = org_bau_Math_log_1(x);
        printf("log    %.9f = %.9f\n", x, _t14);
        double _t15 = org_bau_Math_log10_1(x);
        printf("log10  %.9f = %.9f\n", x, _t15);
        double _t16 = org_bau_Math_sqrt_1(x);
        printf("sqrt   %.9f = %.9f\n", x, _t16);
        double _t17 = org_bau_Math_sin_1(x);
        printf("sin    %.9f = %.9f\n", x, _t17);
        double _t18 = org_bau_Math_cos_1(x);
        printf("cos    %.9f = %.9f\n", x, _t18);
        double _t19 = org_bau_Math_tan_1(x);
        printf("tan    %.9f = %.9f\n", x, _t19);
        double _t20 = org_bau_Math_atan_1(x);
        printf("atan   %.9f = %.9f\n", x, _t20);
        double _t21 = org_bau_Math_asin_1(x);
        printf("asin   %.9f = %.9f\n", x, _t21);
        double _t22 = org_bau_Math_acos_1(x);
        printf("acos   %.9f = %.9f\n", x, _t22);
        x = x + 0.5;
    }
    x = -1;
    while (x <= 1) {
        int64_t _t23 = org_bau_Math_isNotANumber_1(x);
        printf("isNotANumber   %.9f = %lld\n", x, (long long)_t23);
        int64_t _t24 = org_bau_Math_isNegativeZero_1(x);
        printf("isNegativeZero %.9f = %lld\n", x, (long long)_t24);
        int64_t _t25 = org_bau_Math_convertDoubleToLongBits_1(x);
        printf("convertDoubleToLongBits %.9f = %lld\n", x, (long long)_t25);
        double _t26 = org_bau_Math_convertLongBitsToDouble_1(4602678819172646912);
        printf("convertLongBitsToDouble %.9f = %.9f\n", x, _t26);
        x = x + 0.5;
    }
    x = -1;
    while (x <= 1) {
        double y = -1.0;
        while (y <= 1) {
            double _t27 = org_bau_Math_pow_2(x, y);
            printf("pow %.9f\n", _t27);
            double _t28 = org_bau_Math_minFloat_2(x, y);
            printf("min %.9f\n", _t28);
            double _t29 = org_bau_Math_maxFloat_2(x, y);
            printf("max %.9f\n", _t29);
            y = y + 0.5;
        }
        x = x + 0.5;
    }
    _decUseStack(_t3, i8_array);
    _decUseStack(_t2, i8_array);
    _decUseStack(_t1, i8_array);
    _decUseStack(_t0, i8_array);
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

*/
