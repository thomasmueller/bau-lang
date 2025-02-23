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
typedef struct string string;
struct string;
typedef struct string_array string_array;
struct string_array;
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
struct string {
    i8_array* data;
    int32_t _refCount;
};
string string_new() {
    string result;
    result.data = 0;
    return result;
}
struct string_array {
    int32_t len;
    string* data;
    int32_t _refCount;
};
string_array* string_array_new(uint32_t len) {
    string_array* result = _malloc(sizeof(string_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(string) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* functions */
string str_1(i8_array* s);
void i8_array_free(i8_array* x);
int i8_array_freeIfUnused(void* x);
void int_array_free(int_array* x);
int int_array_freeIfUnused(void* x);
void string_free(string* x);
int string_freeIfUnused(void* x);
void string_array_free(string_array* x);
int string_array_freeIfUnused(void* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void string_free(string* x) {
    _decUse(x->data, i8_array);
}
int string_freeIfUnused(void* x) {
    PRINT("== freeIfUnused %p count=%d\n", x, ((string*)x)->_refCount);
    if (((string*)x)->_refCount == 0) { _free(x); return 1; } return 0;
}
void string_array_free(string_array* x) {
    for (int i = 0; i < x->len; i++) string_free(&(x->data[i]));
    _free(x->data);
    _free(x);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1003;
string str_1(i8_array* s) {
    string x = string_new();
    _decUse(x.data, i8_array);
    x.data = s;
    _incUse(x.data);
    return x;
}
int main() {
    string_1000 = str_const("hello", 5);
    string_1001 = str_const("world", 5);
    string_1002 = str_const("!", 1);
    string_1003 = str_const(" ", 1);
    string_array* x = string_array_new(3);
    x->data[0] = str_1(string_1000);
    x->data[1] = str_1(string_1001);
    x->data[2] = str_1(string_1002);
    printf("%.*s %.*s %.*s\n", x->data[0].data->len, x->data[0].data->data, x->data[1].data->len, x->data[1].data->data, x->data[2].data->len, x->data[2].data->data);
    _decUseStack(x, string_array);
    _end();
    return 0;
}
