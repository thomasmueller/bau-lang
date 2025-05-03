#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <math.h>
#include <string.h>
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
typedef struct i8_array i8_array;
struct i8_array;
typedef struct int_array int_array;
struct int_array;
typedef struct org_bau_Utils_dateTime org_bau_Utils_dateTime;
struct org_bau_Utils_dateTime;
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
double org_bau_Math_exp_1(double x);
int64_t org_bau_Math_isNotANumber_1(double x);
double org_bau_Math_log_1(double x);
double org_bau_Math_sqrt_1(double x);
org_bau_Utils_dateTime org_bau_Utils_dateTime_0();
org_bau_Utils_dateTime org_bau_Utils_getDateTime_0();
int64_t org_bau_Utils_getNanoTime_0();
int64_t org_bau_Utils_random_0();
void org_bau_Utils_setRandomSeed_1(int64_t seed);
int64_t shiftRight_int_2(int64_t a, int64_t b);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_Utils_dateTime_free(org_bau_Utils_dateTime* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void org_bau_Utils_dateTime_free(org_bau_Utils_dateTime* x) {
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = (int8_t*) data;
    return result;
}
i8_array* string_1000;
i8_array* string_1003;
i8_array* string_1007;
i8_array* string_1008;
int64_t randomSeed;
double POS_INFINITY;
double NEG_INFINITY;
double NOT_A_NUMBER;
double PI;
double E;
double LOG10;
double LOG2;
int64_t MIN_INT;
int64_t MAX_INT;
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
    } else if (x < 0) {
        double _t3 = org_bau_Math_exp_1(- x);
        double _r0 = 1 / _t3;
        return _r0;
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
            term *= x / i;
            approx += term;
            continue1:;
            i += 1;
        }
        break;
    }
    return approx;
}
int64_t org_bau_Math_isNotANumber_1(double x) {
    return isnan(x);
    int64_t _r0 = x != x;
    return _r0;
}
double org_bau_Math_log_1(double x) {
    return log(x);
    int64_t _t0 = x <= 0;
    if (!(_t0)) {
        int64_t _t1 = org_bau_Math_isNotANumber_1(x);
        _t0 = _t1;
    }
    if (x == 0) {
        return (-1.0 / 0.0);
    } else if (x == (1.0 / 0.0)) {
        return x;
    } else if (_t0) {
        return (0.0 / 0.0);
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
            term *= base;
            result += sign * term / i;
            continue1:;
            i += 1;
        }
        break;
    }
    return result;
}
double org_bau_Math_sqrt_1(double x) {
    return sqrt(x);
    double _t0 = org_bau_Math_log_1(x);
    double _t1 = org_bau_Math_exp_1(_t0 / 2);
    return _t1;
}
org_bau_Utils_dateTime org_bau_Utils_dateTime_0() {
    org_bau_Utils_dateTime _t0 = org_bau_Utils_dateTime_new();
    _t0.year = 0;
    _t0.month = 0;
    _t0.day = 0;
    _t0.hour = 0;
    _t0.minute = 0;
    _t0.second = 0;
    _t0.millis = 0;
    return _t0;
}
org_bau_Utils_dateTime org_bau_Utils_getDateTime_0() {
    time_t current;
    time(&current);
    struct tm* timeinfo;
    timeinfo = localtime(&current);
    org_bau_Utils_dateTime result;
    result.year = timeinfo->tm_year + 1900;
    result.month = timeinfo->tm_mon + 1;
    result.day = timeinfo->tm_mday;
    result.hour = timeinfo->tm_hour;
    result.minute = timeinfo->tm_min;
    result.second = timeinfo->tm_sec;
    struct timespec time={0,0};
    clock_gettime(CLOCK_REALTIME, &time);
    result.millis = time.tv_nsec / 1000000;
    return result;
    org_bau_Utils_dateTime alternative = org_bau_Utils_dateTime_0();
    alternative.year = 2000;
    alternative.month = 1;
    alternative.day = 1;
    alternative.hour = 0;
    alternative.minute = 0;
    alternative.second = 0;
    alternative.millis = 0;
    return alternative;
}
int64_t org_bau_Utils_getNanoTime_0() {
    struct timespec time={0,0};
    clock_gettime(CLOCK_MONOTONIC, &time);
    return time.tv_sec * 1000000000ULL + time.tv_nsec;
    return 0;
}
int64_t org_bau_Utils_random_0() {
    randomSeed += 0x9e3779b97f4a7c15;
    int64_t z = randomSeed;
    z = (z ^ (shiftRight_int_2(z, 30))) * -4658895280553007687;
    z = (z ^ (shiftRight_int_2(z, 27))) * -7723592293110705685;
    int64_t _r0 = z ^ (shiftRight_int_2(z, 31));
    return _r0;
}
void org_bau_Utils_setRandomSeed_1(int64_t seed) {
    randomSeed = seed;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("-", 1);
    string_1003 = str_const(".", 1);
    string_1007 = str_const(" ", 1);
    string_1008 = str_const(":", 1);
    int64_t randomSeed = 0;
    POS_INFINITY = (1.0 / 0.0);
    NEG_INFINITY = (-1.0 / 0.0);
    NOT_A_NUMBER = (0.0 / 0.0);
    PI = 3.141592653589793;
    E = 2.718281828459045;
    LOG10 = 2.302585092994046;
    LOG2 = 0.6931471805599453;
    MIN_INT = 0x8000000000000000;
    MAX_INT = 0x7fffffffffffffff;
    int64_t a = org_bau_Utils_getNanoTime_0();
    printf("%lld\n", (long long)a);
    int64_t b = org_bau_Utils_getNanoTime_0();
    printf("%lld\n", (long long)a);
    org_bau_Utils_dateTime c = org_bau_Utils_getDateTime_0();
    printf("%d-%lld-%lld %lld:%lld:%lld.%lld\n", c.year, c.month, c.day, c.hour, c.minute, c.second, c.millis);
    int64_t _t0 = org_bau_Utils_getNanoTime_0();
    org_bau_Utils_setRandomSeed_1(_t0);
    int64_t _t1 = org_bau_Utils_random_0();
    printf("%lld\n", (long long)_t1);
    printf("%.9f\n", 3.141592653589793);
    double _t2 = org_bau_Math_sqrt_1(2);
    printf("%.9f\n", _t2);
    org_bau_Utils_dateTime_free(&c);
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

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

*/
