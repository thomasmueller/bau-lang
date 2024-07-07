#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <limits.h>
#define _incUse(a) if(a){(a)->_refCount++;}
#define _decUse(a, type) if(a){if(--((a)->_refCount) == 0) type##_free(a);}
#define _malloc(a) malloc(a)
#define _traceMalloc(a) ;
#define _free(a) free(a)
#define _end() ;
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
float f32_1(float x);
double f64_1(double x);
int16_t i16_1(int16_t x);
int32_t i32_1(int32_t x);
char i8_1(char x);
int64_t idiv_2(int64_t a, int64_t b);
int64_t imod_2(int64_t a, int64_t b);
int64_t int_1(int64_t x);
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
float f32_1(float x) {
    return x;
}
double f64_1(double x) {
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
int main() {
    int64_t _t0 = int_1(1);
    int64_t i64v = _t0;
    int32_t _t1 = i32_1(1);
    int32_t i32v = _t1;
    int16_t _t2 = i16_1(1);
    int16_t i16v = _t2;
    char _t3 = i8_1(1);
    char i8v = _t3;
    double _t4 = f64_1(1);
    double f64v = _t4;
    float _t5 = f32_1(1);
    float f32v = _t5;
    int64_t a = shiftRight_int_2(12345, 1);
    int64_t b = shiftLeft_2(12345, 1);
    int64_t c = idiv_2(a, b);
    int64_t d = imod_2(a, b);
    _end();
    return 0;
}
