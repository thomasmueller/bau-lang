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
int64_t even_1(int64_t x);
int64_t odd_1(int64_t x);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
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
    result->_refCount = INT32_MAX;
    result->data = data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1003;
int64_t even_1(int64_t x) {
    if (x == 0) {
        return 1;
    }
    int64_t _t0 = odd_1(x - 1);
    return _t0;
}
int64_t odd_1(int64_t x) {
    if (x == 0) {
        return 0;
    }
    int64_t _t0 = even_1(x - 1);
    return _t0;
}
int main() {
    string_1000 = str_const("odd(10)=", 8);
    string_1001 = str_const("even(10)=", 9);
    string_1002 = str_const("odd(11)=", 8);
    string_1003 = str_const("even(11)=", 9);
    int64_t _t0 = odd_1(10);
    printf("odd(10)=%lld\n", (long long)_t0);
    int64_t _t1 = even_1(10);
    printf("even(10)=%lld\n", (long long)_t1);
    int64_t _t2 = odd_1(11);
    printf("odd(11)=%lld\n", (long long)_t2);
    int64_t _t3 = even_1(11);
    printf("even(11)=%lld\n", (long long)_t3);
    _end();
    return 0;
}
