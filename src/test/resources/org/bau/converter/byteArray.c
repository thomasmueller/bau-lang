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
int64_t idx_2(int64_t x, int64_t len);
int32_t readI32Le_2(i8_array* d, int32_t pos);
int64_t shiftLeft_2(int64_t a, int64_t b);
void test_0();
void i8_array_free(i8_array* x);
int i8_array_freeIfUnused(void* x);
void int_array_free(int_array* x);
int int_array_freeIfUnused(void* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
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
    _decUseStack(x, i8_array);
}
int main() {
    test_0();
    _end();
    return 0;
}
