#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <math.h>
#include <time.h>
#define _incUse(a) if(a){(a)->_refCount++;}
#define _decUse(a, type) if(a){if(--((a)->_refCount) == 0) type##_free(a);}
#define _malloc(a) malloc(a)
#define _traceMalloc(a) ;
#define _free(a) free(a)
#define _end() ;
typedef struct i8_array i8_array;
struct i8_array {
    int32_t len;
    char* data;
    int32_t _refCount;
};
i8_array* i8_array_new(uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(char) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
typedef struct org_bau_Utils_dateTime org_bau_Utils_dateTime;
struct org_bau_Utils_dateTime {
    int32_t year;
    int64_t month;
    int64_t day;
    int64_t hour;
    int64_t minute;
    int64_t second;
    int64_t millis;
    int32_t _refCount;
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
double org_bau_Math_sqrt_1(double x);
org_bau_Utils_dateTime org_bau_Utils_getDateTime_0();
int64_t org_bau_Utils_getNanoTime_0();
int64_t org_bau_Utils_random_0();
void org_bau_Utils_setRandomSeed_1(int64_t seed);
int64_t shiftRight_int_2(int64_t a, int64_t b);
const double PI = 3.141592653589793;
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = 1;
    result->data = data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1003;
int64_t randomSeed;
double org_bau_Math_sqrt_1(double x) {
    return sqrt(x);
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
}
int64_t org_bau_Utils_getNanoTime_0() {
    struct timespec time={0,0};
    clock_gettime(CLOCK_MONOTONIC, &time);
    return time.tv_sec * 1000000000ULL + time.tv_nsec;
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
int main() {
    string_1000 = str_const("-", 1);
    string_1001 = str_const(" ", 1);
    string_1002 = str_const(":", 1);
    string_1003 = str_const(".", 1);
    int64_t randomSeed = 0;
    double PI = 3.141592653589793;
    int64_t _t0 = org_bau_Utils_getNanoTime_0();
    int64_t a = _t0;
    printf("%lld\n", a);
    int64_t _t1 = org_bau_Utils_getNanoTime_0();
    int64_t b = _t1;
    printf("%lld\n", a);
    org_bau_Utils_dateTime _t2 = org_bau_Utils_getDateTime_0();
    org_bau_Utils_dateTime c = _t2;
    printf("%d-%lld-%lld %lld:%lld:%lld.%lld\n", c.year, c.month, c.day, c.hour, c.minute, c.second, c.millis);
    int64_t _t3 = org_bau_Utils_getNanoTime_0();
    org_bau_Utils_setRandomSeed_1(_t3);
    int64_t _t4 = org_bau_Utils_random_0();
    printf("%lld\n", _t4);
    printf("%.9f\n", 3.141592653589793 /* PI */);
    double _t5 = org_bau_Math_sqrt_1(2);
    printf("%.9f\n", _t5);
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

fun setRandomSeed(seed int)
Set the random seed.

fun getRandomSeed() int
Get the random seed.

fun random() int
Pseudo-random number generated using the Splitmix64 algorithm.

*/
