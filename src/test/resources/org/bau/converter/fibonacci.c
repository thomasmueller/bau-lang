#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#define _incUse(a) if(a){(a)->_refCount++;}
#define _decUse(a, type) if(a){if(--((a)->_refCount) == 0) type##_free(a);}
#define _malloc(a) malloc(a)
#define _traceMalloc(a) ;
#define _free(a) free(a)
#define _end() ;
/* types */
typedef struct i8_array i8_array;
struct i8_array;
typedef struct int_array int_array;
struct int_array;
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
/* exception types */
/* functions */
int64_t fibonacciConst_1(int64_t n);
int64_t fibonacciIterative_1(int64_t n);
int64_t fibonacciRecursive_1(int64_t n);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1003;
int64_t ticks;
int64_t fibonacciConst_1(int64_t n) {
    int64_t r0 = 0;
    int64_t r1 = 0;
    int64_t r = 1;
    while (1 == 1) {
        int64_t i = 1;
        while (i < n) {
            r1 = r0;
            r0 = r;
            r = r1 + r0;
            continue1:;
            i += 1;
        }
        break;
    }
    return r;
}
int64_t fibonacciIterative_1(int64_t n) {
    int64_t r0 = 0;
    int64_t r1 = 0;
    int64_t r = 1;
    while (1 == 1) {
        int64_t i = 1;
        while (i < n) {
            r1 = r0;
            r0 = r;
            r = r1 + r0;
            ticks += 1;
            continue1:;
            i += 1;
        }
        break;
    }
    return r;
}
int64_t fibonacciRecursive_1(int64_t n) {
    ticks += 1;
    if (n < 2) {
        return n;
    }
    int64_t _t0 = fibonacciRecursive_1(n - 1);
    int64_t _t1 = fibonacciRecursive_1(n - 2);
    int64_t _r0 = _t0 + _t1;
    return _r0;
}
int main() {
    string_1000 = str_const("fibonacci(20) recursive: ", 25);
    string_1001 = str_const(", ticks: ", 9);
    string_1002 = str_const("fibonacci(20) iterative: ", 25);
    string_1003 = str_const("fibonacci(20) const:     ", 25);
    int64_t ticks = 0;
    ticks = 0;
    int64_t _t0 = fibonacciRecursive_1(20);
    printf("fibonacci(20) recursive: %lld, ticks: %lld\n", (long long)_t0, (long long)ticks);
    ticks = 0;
    int64_t _t1 = fibonacciIterative_1(20);
    printf("fibonacci(20) iterative: %lld, ticks: %lld\n", (long long)_t1, (long long)ticks);
    ticks = 0;
    printf("fibonacci(20) const:     %lld, ticks: %lld\n", (long long)6765, (long long)ticks);
    _end();
    return 0;
}
