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
int64_t if_int_3(int64_t condition, int64_t a, int64_t b);
int64_t int_then_int_3(int64_t this, int64_t a, int64_t b);
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
int64_t if_int_3(int64_t condition, int64_t a, int64_t b) {
    if (condition) {
        return a;
    }
    return b;
}
int64_t int_then_int_3(int64_t this, int64_t a, int64_t b) {
    if (this) {
        return a;
    }
    return b;
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("abs(", 4);
    string_1001 = str_const(")= ", 3);
    while (1 == 1) {
        int64_t i = -1;
        while (i < 2) {
            int64_t _t0 = if_int_3(i < 0, - i, i);
            printf("abs(%lld)= %lld\n", (long long)i, (long long)_t0);
            int64_t _t1 = int_then_int_3((i < 0), - i, i);
            printf("abs(%lld)= %lld\n", (long long)i, (long long)_t1);
            i += 1;
        }
        break;
    }
    _end();
    return 0;
}
