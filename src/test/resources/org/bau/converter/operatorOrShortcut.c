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
int64_t call_2(i8_array* name, int64_t x);
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
i8_array* string_1004;
i8_array* string_1005;
i8_array* string_1006;
i8_array* string_1007;
i8_array* string_1008;
i8_array* string_1009;
i8_array* string_1010;
i8_array* string_1011;
i8_array* string_1012;
i8_array* string_1013;
int64_t call_2(i8_array* name, int64_t x) {
    printf("  called %.*s=%lld\n", name->len, name->data, (long long)x);
    return x;
}
int main() {
    string_1000 = str_const("  called ", 9);
    string_1001 = str_const("=", 1);
    string_1002 = str_const("testing or: call first", 22);
    string_1003 = str_const("a", 1);
    string_1004 = str_const("b", 1);
    string_1005 = str_const("  true", 6);
    string_1006 = str_const("  false", 7);
    string_1007 = str_const("testing or: call both", 21);
    string_1008 = str_const("testing and: call first", 23);
    string_1009 = str_const("testing and: call both", 22);
    string_1010 = str_const("testing and loop", 16);
    string_1011 = str_const("  dec a", 7);
    string_1012 = str_const("  dec b", 7);
    string_1013 = str_const("testing or loop", 15);
    int64_t a = 1;
    int64_t b = 2;
    printf("testing or: call first\n");
    int64_t _t0 = call_2(string_1003, a);
    int64_t _t1 = _t0 > 0;
    if (!(_t1)) {
        int64_t _t2 = call_2(string_1004, b) < 1;
        _t1 = _t2;
    }
    if (_t1) {
        printf("  true\n");
    } else {
        printf("  false\n");
    }
    printf("testing or: call both\n");
    int64_t _t3 = call_2(string_1003, a);
    int64_t _t4 = _t3 > 1;
    if (!(_t4)) {
        int64_t _t5 = call_2(string_1004, b) < 1;
        _t4 = _t5;
    }
    if (_t4) {
        printf("  true\n");
    } else {
        printf("  false\n");
    }
    printf("testing and: call first\n");
    int64_t _t6 = call_2(string_1003, a);
    int64_t _t7 = _t6 > 1;
    if (_t7) {
        int64_t _t8 = call_2(string_1004, b) < 1;
        _t7 = _t8;
    }
    if (_t7) {
        printf("  true\n");
    } else {
        printf("  false\n");
    }
    printf("testing and: call both\n");
    int64_t _t9 = call_2(string_1003, a);
    int64_t _t10 = _t9 > 0;
    if (_t10) {
        int64_t _t11 = call_2(string_1004, b) < 1;
        _t10 = _t11;
    }
    if (_t10) {
        printf("  true\n");
    } else {
        printf("  false\n");
    }
    a = 1;
    b = 1;
    printf("testing and loop\n");
    while (1 == 1) {
        int64_t _t12 = call_2(string_1003, a);
        int64_t _t13 = _t12 > 0;
        if (_t13) {
            int64_t _t14 = call_2(string_1004, b) > 0;
            _t13 = _t14;
        }
        if (!(_t13)) {
            break;
        }
        printf("  dec a\n");
        a -= 1;
    }
    a = 1;
    b = 1;
    printf("testing and loop\n");
    while (1 == 1) {
        int64_t _t15 = call_2(string_1003, a);
        int64_t _t16 = _t15 > 0;
        if (_t16) {
            int64_t _t17 = call_2(string_1004, b) > 0;
            _t16 = _t17;
        }
        if (!(_t16)) {
            break;
        }
        printf("  dec b\n");
        b -= 1;
    }
    a = 1;
    b = 1;
    printf("testing or loop\n");
    while (1 == 1) {
        int64_t _t18 = call_2(string_1003, a);
        int64_t _t19 = _t18 > 0;
        if (!(_t19)) {
            int64_t _t20 = call_2(string_1004, b) > 0;
            _t19 = _t20;
        }
        if (!(_t19)) {
            break;
        }
        if (a > 0) {
            printf("  dec a\n");
            a -= 1;
        } else {
            printf("  dec b\n");
            b -= 1;
        }
    }
    _end();
    return 0;
}
