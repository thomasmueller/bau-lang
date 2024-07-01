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
int64_t even_1(int64_t x);
int64_t odd_1(int64_t x);
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
i8_array* string_1003;
int64_t even_1(int64_t x) {
    if (x == 0) {
        return 1;
    }
    int64_t _t1 = odd_1(x - 1);
    return _t1;
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
    int64_t _t2 = odd_1(10);
    printf("odd(10)=%lld\n", _t2);
    int64_t _t3 = even_1(10);
    printf("even(10)=%lld\n", _t3);
    int64_t _t4 = odd_1(11);
    printf("odd(11)=%lld\n", _t4);
    int64_t _t5 = even_1(11);
    printf("even(11)=%lld\n", _t5);
    _end();
    return 0;
}
/*

fun even(x int) int
declare function, but do not implement

*/
