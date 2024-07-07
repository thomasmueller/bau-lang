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
int64_t readInt_2(i8_array* d, int64_t pos);
int64_t shiftLeft_2(int64_t a, int64_t b);
void test_0();
int64_t readInt_2(i8_array* d, int64_t pos) {
    int64_t _r0 = (d->data[pos] & 255) | (shiftLeft_2((d->data[pos + 1] & 255), 8)) | (shiftLeft_2((d->data[pos + 2] & 255), 16)) | (shiftLeft_2((d->data[pos + 3] & 255), 24));
    return _r0;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
void test_0() {
    i8_array* data = i8_array_new(4);
    int64_t _t0 = readInt_2(data, 0);
    printf("%lld\n", _t0);
    _decUse(data, i8_array);
}
int main() {
    test_0();
    _end();
    return 0;
}
