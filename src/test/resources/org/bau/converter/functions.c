#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
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
typedef struct int_array int_array;
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
void int_array_free(int_array* x) {
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
double f64_double_1(double this);
int64_t idx_2(int64_t x, int64_t len);
int64_t int_rotateRight_2(int64_t this, int64_t n);
int64_t org_bau_Std_ord_1(i8_array* s);
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
int64_t square_1(int64_t x);
int64_t square_2(int64_t a, int64_t b);
int64_t sum_var(int64_t a, int _vaCount,...);
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
double f64_double_1(double this) {
    double _r3 = this + this;
    return _r3;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
int64_t int_rotateRight_2(int64_t this, int64_t n) {
    int64_t _r2 = (shiftRight_int_2(this, n)) | (shiftLeft_2(this, (64 - n)));
    return _r2;
}
int64_t org_bau_Std_ord_1(i8_array* s) {
    if (s->len) {
        char _r0 = s->data[idx_2(0, s->len)];
        return _r0;
    }
    return 0;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
int64_t square_1(int64_t x) {
    int64_t _r0 = x * x;
    return _r0;
}
int64_t square_2(int64_t a, int64_t b) {
    int64_t _r1 = ( a * a ) + ( b * b );
    return _r1;
}
int64_t sum_var(int64_t a, int _vaCount,...) {
    va_list _vaList;
    int_array* b = int_array_new(_vaCount);
    va_start(_vaList, _vaCount);
    for (int _vaI = 0; _vaI < _vaCount; _vaI++) {
        b->data[_vaI] = va_arg(_vaList, int64_t);
    }
    va_end(_vaList);
    int64_t sum = a;
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            sum += b->data[i];
            continue1:;
            int64_t _next = i + 1;
            if (_next >= b->len) {
                break;
            }
            i = _next;
        }
        break;
    }
    _decUse(b, int_array);
    return sum;
    _decUse(b, int_array);
}
int main() {
    string_1000 = str_const(" ", 1);
    string_1001 = str_const("ascii of \"0\" is ", 16);
    string_1002 = str_const("0", 1);
    string_1003 = str_const(" = ", 3);
    int64_t _t0 = square_1(2);
    int64_t _t1 = square_2(2, 3);
    printf("%lld %lld\n", _t0, _t1);
    int64_t _t2 = sum_var(1, /* argCount */ 1, 2);
    printf("%lld\n", _t2);
    int64_t _t3 = sum_var(1, /* argCount */ 2, 2, 3);
    printf("%lld\n", _t3);
    int64_t _t4 = sum_var(1, /* argCount */ 3, 2, 3, 4);
    printf("%lld\n", _t4);
    int64_t randomSeed = 0;
    printf("ascii of \"0\" is %lld\n", 48);
    int64_t _t5 = int_rotateRight_2(1, 1);
    int64_t _t6 = int_rotateRight_2(0x1, 1);
    printf("%lld = %lld\n", _t5, _t6);
    double _t7 = f64_double_1(1.0);
    double _t8 = f64_double_1(1.0);
    printf("%.9f = %.9f\n", _t7, _t8);
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

fun ord(s i8[]) int const
The value of the first byte in the string. 0 if the string is empty.

*/
