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
typedef struct Entry_owned Entry_owned;
struct Entry_owned;
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
struct Entry_owned {
    int64_t key;
    int64_t value;
};
Entry_owned* Entry_owned_new() {
    Entry_owned* result = _malloc(sizeof(Entry_owned));
    _traceMalloc(result);
    result->key = 0;
    result->value = 0;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
void Entry_owned_print_1(Entry_owned* this);
Entry_owned* Entry_owned_0();
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void Entry_owned_free(Entry_owned* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void Entry_owned_free(Entry_owned* x) {
    if (x == NULL) return;
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
i8_array* string_1003;
void Entry_owned_print_1(Entry_owned* this) {
    printf("key: %lld value: %lld\n", (long long)this->key, (long long)this->value);
}
Entry_owned* Entry_owned_0() {
    Entry_owned* _t1 = Entry_owned_new();
    _t1->key = 0;
    _t1->value = 0;
    return _t1;
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("key: ", 5);
    string_1001 = str_const(" value: ", 8);
    string_1003 = str_const("end", 3);
    Entry_owned* x = Entry_owned_0();
    x->key = 1;
    x->value = 100;
    Entry_owned_print_1(x);
    Entry_owned* y = x;
    x = NULL;
    Entry_owned_print_1(y);
    printf("end\n");
    Entry_owned_free(y);
    Entry_owned_free(x);
    _end();
    return 0;
}
