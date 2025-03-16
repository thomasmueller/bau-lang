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
/* types */
typedef struct i8_array i8_array;
struct i8_array;
typedef struct int_array int_array;
struct int_array;
typedef struct Entry Entry;
struct Entry;
typedef struct Entry_owned Entry_owned;
struct Entry_owned;
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
struct Entry {
    int64_t key;
    int64_t value;
    int32_t _refCount;
};
Entry* Entry_new() {
    Entry* result = _malloc(sizeof(Entry));
    _traceMalloc(result);
    result->_refCount = 1;
    result->key = 0;
    result->value = 0;
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
/* functions */
void Entry_owned_print_1(Entry_owned* this);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void Entry_free(Entry* x);
void Entry_owned_free(Entry_owned* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void Entry_free(Entry* x) {
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
    result->data = data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1003;
void Entry_owned_print_1(Entry_owned* this) {
    printf("key: %lld value: %lld\n", (long long)this->key, (long long)this->value);
}
int main() {
    string_1000 = str_const("key: ", 5);
    string_1001 = str_const(" value: ", 8);
    string_1002 = str_const("clear ", 6);
    string_1003 = str_const("end", 3);
    Entry_owned* x = Entry_owned_new();
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
