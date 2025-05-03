#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
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
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
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
    result->data = (int8_t*) data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1003;
int64_t ticks;
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
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
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
