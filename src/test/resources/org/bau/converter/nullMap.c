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
typedef struct int_array int_array;
struct int_array;
typedef struct Value Value;
struct Value;
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
struct Value {
    int64_t data;
    int32_t _refCount;
};
Value* Value_new() {
    Value* result = _malloc(sizeof(Value));
    _traceMalloc(result);
    result->_refCount = 1;
    result->data = 0;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
Value* get_1(int64_t key);
void test_0();
void int_array_free(int_array* x);
void Value_free(Value* x);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void Value_free(Value* x) {
    _free(x);
}
Value* get_1(int64_t key) {
    if (key <= 0) {
        return NULL;
    }
    Value* result = Value_new();
    result->data = key * 10;
    return result;
}
void test_0() {
    Value* a = get_1(0);
    if (a != NULL) {
        printf("%lld\n", (long long)a->data);
    }
    Value* b = get_1(1);
    if (b != NULL) {
        printf("%lld\n", (long long)b->data);
    }
    _decUseStack(b, Value);
    _decUseStack(a, Value);
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    test_0();
    _end();
    return 0;
}
