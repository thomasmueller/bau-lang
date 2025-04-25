#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <limits.h>
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
float f32_1(float x);
double float_1(double x);
int16_t i16_1(int16_t x);
int32_t i32_1(int32_t x);
char i8_1(char x);
int64_t idiv_2(int64_t a, int64_t b);
int64_t imod_2(int64_t a, int64_t b);
int64_t int_1(int64_t x);
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
void int_array_free(int_array* x);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
int64_t i64v;
int32_t i32v;
int16_t i16v;
char i8v;
double f64v;
float f32v;
int64_t a;
int64_t b;
int64_t c;
int64_t d;
float f32_1(float x) {
    return x;
}
double float_1(double x) {
    return x;
}
int16_t i16_1(int16_t x) {
    return x;
}
int32_t i32_1(int32_t x) {
    return x;
}
char i8_1(char x) {
    return x;
}
int64_t idiv_2(int64_t a, int64_t b) {
    if (b != 0) return a / b;
    if (a == 0) return 0;
    return a > 0 ? LLONG_MAX : LLONG_MIN;
}
int64_t imod_2(int64_t a, int64_t b) {
    if (b != 0) return a % b;
    if (a == 0) return 0;
    return a > 0 ? LLONG_MAX : LLONG_MIN;
}
int64_t int_1(int64_t x) {
    return x;
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
    int64_t i64v = 1;
    int32_t i32v = 1;
    int16_t i16v = 1;
    char i8v = 1;
    double f64v = 1.0;
    float f32v = 1.0;
    int64_t a = 6172;
    int64_t b = 24690;
    int64_t c = idiv_2(a, b);
    int64_t d = imod_2(a, b);
    _end();
    return 0;
}
