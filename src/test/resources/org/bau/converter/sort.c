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
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
void insertionSort_int_array_int_1(int_array* a);
int64_t org_bau_Utils_random_0();
void shellSort_int_array_int_1(int_array* a);
int64_t shiftRight_int_2(int64_t a, int64_t b);
void test_0();
void int_array_free(int_array* x);
void org_bau_Utils_dateTime_free(org_bau_Utils_dateTime* x);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void org_bau_Utils_dateTime_free(org_bau_Utils_dateTime* x) {
}
int64_t randomSeed;
int64_t idiv_2(int64_t a, int64_t b) {
    if (b != 0) return a / b;
    if (a == 0) return 0;
    return a > 0 ? LLONG_MAX : LLONG_MIN;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
void insertionSort_int_array_int_1(int_array* a) {
    while (1 == 1) {
        int64_t i = 1;
        while (i < a->len) {
            int64_t t = a->data[i];
            int64_t j = i - 1;
            while (1 == 1) {
                int64_t _t0 = j >= 0;
                if (_t0) {
                    int64_t _t1 = a->data[idx_2(j, a->len)] > t;
                    _t0 = _t1;
                }
                if (!(_t0)) {
                    break;
                }
                a->data[idx_2(j + 1, a->len)] = a->data[idx_2(j, a->len)];
                j -= 1;
            }
            a->data[idx_2(j + 1, a->len)] = t;
            continue1:;
            i += 1;
        }
        break;
    }
}
int64_t org_bau_Utils_random_0() {
    randomSeed += 0x9e3779b97f4a7c15;
    int64_t z = randomSeed;
    z = (z ^ (shiftRight_int_2(z, 30))) * -4658895280553007687;
    z = (z ^ (shiftRight_int_2(z, 27))) * -7723592293110705685;
    int64_t _r0 = z ^ (shiftRight_int_2(z, 31));
    return _r0;
}
void shellSort_int_array_int_1(int_array* a) {
    int64_t h = 16;
    while (a->len > ( idiv_2(h, 16) )) {
        h = ( h + h ) + ( idiv_2(h, 4) ) + 16;
    }
    while (h > 15) {
        h = ( idiv_2((h - 16), 9) ) * 4;
        int64_t g = idiv_2((h + 15), 16);
        while (1 == 1) {
            int64_t i = g;
            while (i < a->len) {
                int64_t t = a->data[i];
                int64_t j = i - g;
                while (1 == 1) {
                    int64_t _t0 = j >= 0;
                    if (_t0) {
                        int64_t _t1 = a->data[idx_2(j, a->len)] > t;
                        _t0 = _t1;
                    }
                    if (!(_t0)) {
                        break;
                    }
                    a->data[idx_2(j + g, a->len)] = a->data[idx_2(j, a->len)];
                    j -= g;
                }
                a->data[idx_2(j + g, a->len)] = t;
                continue3:;
                i += 1;
            }
            break;
        }
    }
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
void test_0() {
    int_array* x = int_array_new(5);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            x->data[i] = org_bau_Utils_random_0();
            continue1:;
            int64_t _next = i + 1;
            if (_next >= 5) {
                break;
            }
            i = _next;
        }
        break;
    }
    insertionSort_int_array_int_1(x);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            printf("%lld\n", (long long)x->data[i]);
            continue3:;
            int64_t _next = i + 1;
            if (_next >= 5) {
                break;
            }
            i = _next;
        }
        break;
    }
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            x->data[i] = org_bau_Utils_random_0();
            continue5:;
            int64_t _next = i + 1;
            if (_next >= 5) {
                break;
            }
            i = _next;
        }
        break;
    }
    shellSort_int_array_int_1(x);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            printf("%lld\n", (long long)x->data[i]);
            continue7:;
            int64_t _next = i + 1;
            if (_next >= 5) {
                break;
            }
            i = _next;
        }
        break;
    }
    _decUseStack(x, int_array);
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    int64_t randomSeed = 0;
    test_0();
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
