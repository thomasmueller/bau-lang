#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
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
int64_t idx_2(int64_t x, int64_t len);
int32_t readI32Le_2(i8_array* d, int32_t pos);
int64_t shiftLeft_2(int64_t a, int64_t b);
void test_0();
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
int32_t readI32Le_2(i8_array* d, int32_t pos) {
    int64_t _r0 = ( d->data[idx_2(pos, d->len)] | (shiftLeft_2(d->data[idx_2(pos + 1, d->len)], 8)) ) | (shiftLeft_2(d->data[idx_2(pos + 2, d->len)], 16)) | (shiftLeft_2(d->data[idx_2(pos + 3, d->len)], 24));
    return _r0;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
void test_0() {
    i8_array* x = i8_array_new(4);
    x->data[0] = 0x12;
    x->data[1] = 0x34;
    x->data[2] = 0x56;
    x->data[3] = 0x78;
    int32_t _t0 = readI32Le_2(x, 0);
    printf("%d\n", _t0);
    _decUse(x, i8_array);
}
int main() {
    test_0();
    _end();
    return 0;
}
