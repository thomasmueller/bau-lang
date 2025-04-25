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
typedef struct org_bau_Exception_exception org_bau_Exception_exception;
struct org_bau_Exception_exception;
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
struct org_bau_Exception_exception {
    int64_t exceptionType;
    i8_array* message;
};
org_bau_Exception_exception org_bau_Exception_exception_new() {
    org_bau_Exception_exception result;
    result.exceptionType = 0;
    result.message = 0;
    return result;
}
/* exception types */
typedef struct _or_exception _or_exception;
struct _or_exception {
    org_bau_Exception_exception exception;
};
_or_exception ok_or_exception() {
    _or_exception x;
    x.exception.exceptionType = -1;
    return x;
}
_or_exception exception_or_exception(org_bau_Exception_exception exception) {
    _or_exception x;
    x.exception = exception;
    return x;
}
/* global */
int __argc;
char **__argv;
/* functions */
org_bau_Exception_exception org_bau_Exception_exception_1(i8_array* message);
_or_exception print_1(int64_t x);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_Exception_exception_free(org_bau_Exception_exception* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void org_bau_Exception_exception_free(org_bau_Exception_exception* x) {
    _decUse(x->message, i8_array);
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
org_bau_Exception_exception org_bau_Exception_exception_1(i8_array* message) {
    org_bau_Exception_exception result = org_bau_Exception_exception_new();
    result.exceptionType = 0;
    _decUse(result.message, i8_array);
    result.message = message;
    _incUse(result.message);
    return result;
}
_or_exception print_1(int64_t x) {
    org_bau_Exception_exception _lastException;
    _or_exception _x0;
    if (x > 5) {
        org_bau_Exception_exception _t0 = org_bau_Exception_exception_1(string_1000);
        _x0 = exception_or_exception(_t0); _lastException = _x0.exception; goto catch0;
        org_bau_Exception_exception_free(&_t0);
    }
    printf("x = %lld\n", (long long)x);
    return ok_or_exception();
    catch0:
    return exception_or_exception(_lastException);
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("Value too large", 15);
    string_1001 = str_const("x = ", 4);
    string_1002 = str_const("Error: ", 7);
    org_bau_Exception_exception _lastException;
    _or_exception _x0;
    int64_t i = 0;
    while (i <= 10) {
        _x0 = print_1(i);
        if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
        goto skip0;
        catch0:;
        org_bau_Exception_exception e = _lastException;
            printf("Error: %.*s\n", e.message->len, e.message->data);
            org_bau_Exception_exception_free(&e);
        skip0:;
        i += 1;
    }
    _end();
    return 0;
}
/*

type exception
An exception

fun exception(message i8[]) exception
Create a new exception with a message

*/
