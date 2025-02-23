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
#define _incUse(a)            {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT("++  %p line %d, from %d\n", a, __LINE__, (a)?(a)->_refCount:0);__builtin_assume((a)->_refCount > 0); (a)->_refCount++;}}
#define _decUse(a, type)      {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT("--  %p line %d, from %d\n", a, __LINE__, (a)->_refCount);if(--((a)->_refCount) == 0)type##_free(a);}}
#define _incUseStack(a)       _incUse(a)
#define _decUseStack(a, type) _decUse(a, type)
/* types */
typedef struct int_array int_array;
struct int_array;
typedef struct float_array float_array;
struct float_array;
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
struct float_array {
    int32_t len;
    double* data;
    int32_t _refCount;
};
float_array* float_array_new(uint32_t len) {
    float_array* result = _malloc(sizeof(float_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(double) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* functions */
double float_1(double x);
int64_t int_1(int64_t x);
double sum_float_var(int _vaCount,...);
int64_t sum_int_var(int _vaCount,...);
void int_array_free(int_array* x);
int int_array_freeIfUnused(void* x);
void float_array_free(float_array* x);
int float_array_freeIfUnused(void* x);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void float_array_free(float_array* x) {
    _free(x->data);
    _free(x);
}
double float_1(double x) {
    return x;
}
int64_t int_1(int64_t x) {
    return x;
}
double sum_float_var(int _vaCount,...) {
    va_list _vaList;
    float_array* x = float_array_new(_vaCount);
    va_start(_vaList, _vaCount);
    for (int _vaI = 0; _vaI < _vaCount; _vaI++) {
        x->data[_vaI] = va_arg(_vaList, double);
    }
    va_end(_vaList);
    double result = 0.0;
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            result += x->data[i];
            continue1:;
            int64_t _next = i + 1;
            if (_next >= x->len) {
                break;
            }
            i = _next;
        }
        break;
    }
    _decUseStack(x, float_array);
    return result;
}
int64_t sum_int_var(int _vaCount,...) {
    va_list _vaList;
    int_array* x = int_array_new(_vaCount);
    va_start(_vaList, _vaCount);
    for (int _vaI = 0; _vaI < _vaCount; _vaI++) {
        x->data[_vaI] = va_arg(_vaList, int64_t);
    }
    va_end(_vaList);
    int64_t result = 0;
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            result += x->data[i];
            continue1:;
            int64_t _next = i + 1;
            if (_next >= x->len) {
                break;
            }
            i = _next;
        }
        break;
    }
    _decUseStack(x, int_array);
    return result;
}
int main() {
    printf("%lld\n", (long long)6);
    printf("%.9f\n", 6.6);
    _end();
    return 0;
}
