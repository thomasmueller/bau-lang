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
const int64_t a = 1000000;
const double b = 3.1415;
const int64_t c = 0xcafe;
i8_array* d;
i8_array* e;
i8_array* f;
const double g = - 1.23E-45;
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
int main() {
    string_1000 = str_const("String literal", 14);
    string_1001 = str_const("Raw string", 10);
    string_1002 = str_const("Two-line\nraw string with `", 26);
    d = string_1000;
    e = string_1001;
    f = string_1002;
    int64_t a = 1000000;
    double b = 3.1415;
    int64_t c = 0xcafe;
    i8_array* d = string_1000;
    _incUse(d);
    i8_array* e = string_1001;
    _incUse(e);
    i8_array* f = string_1002;
    _incUse(f);
    double g = - 1.23E-45;
    _decUse(d, i8_array);
    _decUse(e, i8_array);
    _decUse(f, i8_array);
    _end();
    return 0;
}
