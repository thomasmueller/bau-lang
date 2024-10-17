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
typedef struct int_array int_array;
struct int_array;
typedef struct f64_array f64_array;
struct f64_array;
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
struct f64_array {
    int32_t len;
    double* data;
    int32_t _refCount;
};
f64_array* f64_array_new(uint32_t len) {
    f64_array* result = _malloc(sizeof(f64_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(double) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* functions */
double f64_1(double x);
int64_t int_1(int64_t x);
double sum_f64_var(int _vaCount,...);
int64_t sum_int_var(int _vaCount,...);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void f64_array_free(f64_array* x) {
    _free(x->data);
    _free(x);
}
double f64_1(double x) {
    return x;
}
int64_t int_1(int64_t x) {
    return x;
}
double sum_f64_var(int _vaCount,...) {
    va_list _vaList;
    f64_array* x = f64_array_new(_vaCount);
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
    _decUse(x, f64_array);
    return result;
    _decUse(x, f64_array);
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
    _decUse(x, int_array);
    return result;
    _decUse(x, int_array);
}
int main() {
    printf("%lld\n", (long long)6);
    printf("%.9f\n", 6.6);
    _end();
    return 0;
}
