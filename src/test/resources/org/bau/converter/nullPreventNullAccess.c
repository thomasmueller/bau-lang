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
typedef struct Value Value;
struct Value;
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
void testBreak_0();
void testContinue_0();
void testIf_0();
void testReturn_0();
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void Value_free(Value* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void Value_free(Value* x) {
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
i8_array* string_1003;
i8_array* string_1004;
i8_array* string_1005;
i8_array* string_1006;
Value* get_1(int64_t key) {
    if (key <= 0) {
        return NULL;
    }
    Value* result = Value_new();
    result->data = key * 10;
    return result;
}
void testBreak_0() {
    printf("testBreak\n");
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            Value* a = get_1(i);
            if (!(a)) {
                _decUseStack(a, Value);
                break;
            }
            printf("  get(%lld) = %lld\n", i, (long long)a->data);
            continue1:;
            int64_t _next = i + 1;
            if (_next >= 3) {
                _decUseStack(a, Value);
                break;
            }
            i = _next;
            _decUseStack(a, Value);
        }
        break;
    }
}
void testContinue_0() {
    printf("testContinue\n");
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            Value* a = get_1(i);
            if (!(a)) {
                _decUseStack(a, Value);
                goto continue1;
            }
            printf("  get(%lld) = %lld\n", i, (long long)a->data);
            continue1:;
            int64_t _next = i + 1;
            if (_next >= 3) {
                _decUseStack(a, Value);
                break;
            }
            i = _next;
            _decUseStack(a, Value);
        }
        break;
    }
}
void testIf_0() {
    printf("testIf\n");
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            Value* a = get_1(i);
            if (a != NULL) {
                printf("  get(%lld) = %lld\n", i, (long long)a->data);
            } else {
                printf("  get(%lld) = null\n", i);
            }
            continue1:;
            int64_t _next = i + 1;
            if (_next >= 3) {
                _decUseStack(a, Value);
                break;
            }
            i = _next;
            _decUseStack(a, Value);
        }
        break;
    }
}
void testReturn_0() {
    printf("testReturn\n");
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            Value* a = get_1(i);
            if (!(a)) {
                return;
            }
            printf("  get(%lld) = %lld\n", i, (long long)a->data);
            continue1:;
            int64_t _next = i + 1;
            if (_next >= 3) {
                _decUseStack(a, Value);
                break;
            }
            i = _next;
            _decUseStack(a, Value);
        }
        break;
    }
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("testBreak", 9);
    string_1001 = str_const("  get(", 6);
    string_1002 = str_const(") = ", 4);
    string_1003 = str_const("testContinue", 12);
    string_1004 = str_const("testIf", 6);
    string_1005 = str_const(") = null", 8);
    string_1006 = str_const("testReturn", 10);
    testIf_0();
    testReturn_0();
    testContinue_0();
    testBreak_0();
    _end();
    return 0;
}
