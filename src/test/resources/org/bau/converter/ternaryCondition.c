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
int64_t if_int_3(int64_t condition, int64_t a, int64_t b);
int64_t int_then_int_3(int64_t this, int64_t a, int64_t b);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = -1;
    result->data = data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
int64_t if_int_3(int64_t condition, int64_t a, int64_t b) {
    if (condition) {
        return a;
    }
    return b;
}
int64_t int_then_int_3(int64_t this, int64_t a, int64_t b) {
    if (this) {
        return a;
    }
    return b;
}
int main() {
    string_1000 = str_const("abs(", 4);
    string_1001 = str_const(")= ", 3);
    while (1 == 1) {
        int64_t i = -1;
        while (i < 2) {
            int64_t _t0 = if_int_3(i < 0, - i, i);
            printf("abs(%lld)= %lld\n", (long long)i, (long long)_t0);
            int64_t _t1 = int_then_int_3((i < 0), - i, i);
            printf("abs(%lld)= %lld\n", (long long)i, (long long)_t1);
            continue1:;
            i += 1;
        }
        break;
    }
    _end();
    return 0;
}
