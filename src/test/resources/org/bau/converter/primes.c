#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <limits.h>
#include <time.h>
#define REF_COUNT_INC
#define REF_COUNT_STACK_INC
#define PRINT(...)
#define _end()
#define _malloc(a)      malloc(a)
#define _traceMalloc(a)
#define _free(a)        free(a)
#define _incUse(a)            {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT("++  %p line %d, from %d\n", a, __LINE__, (a)?(a)->_refCount:0); (a)->_refCount++;}}
#define _decUse(a, type)      {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT("--  %p line %d, from %d\n", a, __LINE__, (a)->_refCount);if(--((a)->_refCount) == 0)type##_free(a);}}
#define _incUseStack(a)       _incUse(a)
#define _decUseStack(a, type) _decUse(a, type)
int64_t arrayOutOfBounds(int64_t x, int64_t len) {
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
/* types */
typedef struct int_array int_array;
struct int_array;
typedef struct org_bau_Utils_dateTime org_bau_Utils_dateTime;
struct org_bau_Utils_dateTime;
typedef struct BitField BitField;
struct BitField;
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
struct org_bau_Utils_dateTime {
    int32_t year;
    int64_t month;
    int64_t day;
    int64_t hour;
    int64_t minute;
    int64_t second;
    int64_t millis;
};
org_bau_Utils_dateTime org_bau_Utils_dateTime_new() {
    org_bau_Utils_dateTime result;
    result.year = 0;
    result.month = 0;
    result.day = 0;
    result.hour = 0;
    result.minute = 0;
    result.second = 0;
    result.millis = 0;
    return result;
}
struct BitField {
    int_array* data;
    int32_t _refCount;
};
BitField* BitField_new() {
    BitField* result = _malloc(sizeof(BitField));
    _traceMalloc(result);
    result->_refCount = 1;
    result->data = 0;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
int64_t BitField_get_2(BitField* this, int64_t index);
void BitField_set_2(BitField* this, int64_t index);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
BitField* newBitField_1(int64_t len);
int64_t primeSum_1(int64_t limit);
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
void int_array_free(int_array* x);
void org_bau_Utils_dateTime_free(org_bau_Utils_dateTime* x);
void BitField_free(BitField* x);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void org_bau_Utils_dateTime_free(org_bau_Utils_dateTime* x) {
}
void BitField_free(BitField* x) {
    _decUse(x->data, int_array);
    _free(x);
}
int64_t randomSeed;
int64_t BitField_get_2(BitField* this, int64_t index) {
    int64_t _r0 = ( shiftRight_int_2(this->data->data[idx_2(shiftRight_int_2(index, 6), this->data->len)], (index & 63)) ) & 1;
    return _r0;
}
void BitField_set_2(BitField* this, int64_t index) {
    this->data->data[idx_2(shiftRight_int_2(index, 6), this->data->len)] |= shiftLeft_2(1, (index & 63));
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
BitField* newBitField_1(int64_t len) {
    BitField* x = BitField_new();
    _decUse(x->data, int_array);
    x->data = int_array_new(idiv_2((len + 63), 64));
    return x;
}
int64_t primeSum_1(int64_t limit) {
    BitField* sieve = newBitField_1(limit + 1);
    int64_t i = 3;
    while (( i * i ) <= limit) {
        int64_t _t0 = BitField_get_2(sieve, i);
        if (!(_t0)) {
            int64_t j = i * i;
            while (j <= limit) {
                BitField_set_2(sieve, j);
                j += i;
            }
        }
        i += 2;
    }
    int64_t sum = 2;
    i = 3;
    while (i <= limit) {
        int64_t _t1 = BitField_get_2(sieve, i);
        if (!(_t1)) {
            sum += i;
        }
        i += 2;
    }
    _decUseStack(sieve, BitField);
    return sum;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    int64_t randomSeed = 0;
    int64_t _t0 = primeSum_1(100);
    printf("%lld\n", (long long)_t0);
    _end();
    return 0;
}
/*

type dateTime
Date and time.

fun getDateTime() dateTime
Get the local time in millisecond precision.

fun getNanoTime() int
Nanosecons since some undefined point in the past. Never jumps backwards.

fun getNanoTimeUTC() int
Nanoseconds since 1970 (epoch). May jump backwards when the system clock is adjusted.

fun getRandomSeed() int
Get the random seed.

fun random() int
Pseudo-random number generated using the Splitmix64 algorithm.

fun setRandomSeed(seed int)
Set the random seed.

*/
