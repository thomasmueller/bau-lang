#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdlib.h>
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
void exit_1(int64_t code);
i8_array* expensiveCalc_1(i8_array* a);
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
i8_array* string_1004;
i8_array* string_1005;
i8_array* string_1006;
void exit_1(int64_t code) {
    exit(code);
    exit_1(code);
}
i8_array* expensiveCalc_1(i8_array* a) {
    printf("expensive calculation with param: %.*s\n", a->len, a->data);
    return a;
}
int main() {
    string_1000 = str_const("expensive calculation with param: ", 34);
    string_1001 = str_const("not zero", 8);
    string_1002 = str_const("zero", 4);
    string_1003 = str_const(": ", 2);
    string_1004 = str_const("assertion failed", 16);
    string_1005 = str_const("next", 4);
    string_1006 = str_const("end", 3);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            i8_array* _t0 = NULL;
            if (i) {
                _decUseStack(_t0, i8_array);
                _t0 = expensiveCalc_1(string_1001);
            } else {
                _decUseStack(_t0, i8_array);
                _t0 = expensiveCalc_1(string_1002);
            }
            i8_array* x = _t0;
            _incUseStack(x);
            printf("%lld: %.*s\n", i, x->len, x->data);
            if (!(( i < 1 ))) {
                printf("assertion failed\n");
                exit_1(1);
            } else {
            }
            printf("next\n");
            continue1:;
            int64_t _next = i + 1;
            if (_next >= 2) {
                _decUseStack(x, i8_array);
                _decUseStack(_t0, i8_array);
                break;
            }
            i = _next;
            _decUseStack(x, i8_array);
            _decUseStack(_t0, i8_array);
        }
        break;
    }
    printf("end\n");
    _end();
    return 0;
}
/*

fun assert(condition int, message i8[])
recursive call causes stack overflow

*/
