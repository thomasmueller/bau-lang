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
typedef struct float_array float_array;
struct float_array;
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
struct float_array {
    int32_t len;
    double* data;
    int32_t _refCount;
};
float_array* float_array_new(uint32_t len) {
    float_array* result = _malloc(sizeof(float_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(double) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
float_array* arrayOf_float_var(int64_t _T, int _vaCount,...);
i8_array* arrayOf_i8_var(int64_t _T, int _vaCount,...);
int_array* arrayOf_int_var(int64_t _T, int _vaCount,...);
float_array* array_float_var(int _vaCount,...);
int_array* array_int_var(int _vaCount,...);
void fill_1(int_array* data);
int64_t i8_array_len_1(i8_array* this);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void float_array_free(float_array* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void float_array_free(float_array* x) {
    _free(x->data);
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
i8_array* string_1002;
float_array* arrayOf_float_var(int64_t _T, int _vaCount,...) {
    va_list _vaList;
    float_array* entries = float_array_new(_vaCount);
    va_start(_vaList, _vaCount);
    for (int _vaI = 0; _vaI < _vaCount; _vaI++) {
        entries->data[_vaI] = va_arg(_vaList, double);
    }
    va_end(_vaList);
    return entries;
}
i8_array* arrayOf_i8_var(int64_t _T, int _vaCount,...) {
    va_list _vaList;
    i8_array* entries = i8_array_new(_vaCount);
    va_start(_vaList, _vaCount);
    for (int _vaI = 0; _vaI < _vaCount; _vaI++) {
        entries->data[_vaI] = (int8_t) va_arg(_vaList, int);
    }
    va_end(_vaList);
    return entries;
}
int_array* arrayOf_int_var(int64_t _T, int _vaCount,...) {
    va_list _vaList;
    int_array* entries = int_array_new(_vaCount);
    va_start(_vaList, _vaCount);
    for (int _vaI = 0; _vaI < _vaCount; _vaI++) {
        entries->data[_vaI] = va_arg(_vaList, int64_t);
    }
    va_end(_vaList);
    return entries;
}
float_array* array_float_var(int _vaCount,...) {
    va_list _vaList;
    float_array* entries = float_array_new(_vaCount);
    va_start(_vaList, _vaCount);
    for (int _vaI = 0; _vaI < _vaCount; _vaI++) {
        entries->data[_vaI] = va_arg(_vaList, double);
    }
    va_end(_vaList);
    return entries;
}
int_array* array_int_var(int _vaCount,...) {
    va_list _vaList;
    int_array* entries = int_array_new(_vaCount);
    va_start(_vaList, _vaCount);
    for (int _vaI = 0; _vaI < _vaCount; _vaI++) {
        entries->data[_vaI] = va_arg(_vaList, int64_t);
    }
    va_end(_vaList);
    return entries;
}
void fill_1(int_array* data) {
    if (data->len <= 0) {
        return;
    }
    int64_t i = 0;
    while (1) {
        data->data[i] = i;
        int64_t next = i + 1;
        if (next >= data->len) {
            break;
        }
        i = next;
    }
}
int64_t i8_array_len_1(i8_array* this) {
    int32_t _r0 = this->len;
    return _r0;
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("len ", 4);
    string_1001 = str_const("len of hello is ", 16);
    string_1002 = str_const("hello", 5);
    int_array* _t0 = int_array_new(16);
    int_array* data = _t0;
    _incUseStack(data);
    fill_1(data);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            printf("%lld\n", (long long)data->data[i]);
            int64_t _next = i + 1;
            if (_next >= 16) {
                break;
            }
            i = _next;
        }
        break;
    }
    i8_array* a = arrayOf_i8_var(0, /* argCount */ 5, 0, 1, 2, 3, 44);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            printf("%d\n", a->data[i]);
            int64_t _next = i + 1;
            if (_next >= a->len) {
                break;
            }
            i = _next;
        }
        break;
    }
    int_array* b = array_int_var(/* argCount */ 4, 0, 1, 2, 3);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            printf("%lld\n", (long long)b->data[i]);
            int64_t _next = i + 1;
            if (_next >= b->len) {
                break;
            }
            i = _next;
        }
        break;
    }
    float_array* c = array_float_var(/* argCount */ 3, 1.0, 2.0, 3.0);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            printf("%.9f\n", c->data[i]);
            int64_t _next = i + 1;
            if (_next >= c->len) {
                break;
            }
            i = _next;
        }
        break;
    }
    int_array* x = arrayOf_int_var(0, /* argCount */ 4, 0, 1, 2, 3);
    float_array* y = arrayOf_float_var(0, /* argCount */ 3, 1.0, 2.0, 3.0);
    i8_array* z = arrayOf_i8_var(0, /* argCount */ 3, 1, 0, 44);
    printf("len %d\n", z->len);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            printf("%d\n", z->data[i]);
            int64_t _next = i + 1;
            if (_next >= z->len) {
                break;
            }
            i = _next;
        }
        break;
    }
    int64_t _t1 = i8_array_len_1(string_1002);
    printf("len of hello is %lld\n", (long long)_t1);
    _decUseStack(z, i8_array);
    _decUseStack(y, float_array);
    _decUseStack(x, int_array);
    _decUseStack(c, float_array);
    _decUseStack(b, int_array);
    _decUseStack(a, i8_array);
    _decUseStack(data, int_array);
    _decUseStack(_t0, int_array);
    _end();
    return 0;
}
