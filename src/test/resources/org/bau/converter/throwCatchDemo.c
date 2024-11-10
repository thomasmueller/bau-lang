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
    int32_t _refCount;
};
org_bau_Exception_exception org_bau_Exception_exception_new() {
    org_bau_Exception_exception result;
    result.exceptionType = 0;
    result.message = 0;
    return result;
}
/* exception types */
typedef struct _int64_t_or_exception _int64_t_or_exception;
struct _int64_t_or_exception {
    org_bau_Exception_exception exception;
    int64_t result;
};
_int64_t_or_exception ok_int64_t_or_exception(int64_t result) {
    _int64_t_or_exception x;
    x.exception.exceptionType = -1;
    x.result = result;
    return x;
}
_int64_t_or_exception exception_int64_t_or_exception(org_bau_Exception_exception exception) {
    _int64_t_or_exception x;
    x.exception = exception;
    return x;
}
/* functions */
org_bau_Exception_exception org_bau_Exception_exception_1(i8_array* message);
_int64_t_or_exception square_1(int64_t x);
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
org_bau_Exception_exception org_bau_Exception_exception_1(i8_array* message) {
    org_bau_Exception_exception result = org_bau_Exception_exception_new();
    result.exceptionType = 0;
    _decUse(result.message, i8_array);
    result.message = message;
    _incUse(result.message);
    return result;
    org_bau_Exception_exception_free(&result);
}
_int64_t_or_exception square_1(int64_t x) {
    org_bau_Exception_exception _lastException;
    _int64_t_or_exception _x0;
    if (x > 3000000000) {
        org_bau_Exception_exception _t0 = org_bau_Exception_exception_1(string_1000);
        _x0 = exception_int64_t_or_exception(_t0); _lastException = _x0.exception; goto catch0;
        org_bau_Exception_exception_free(&_t0);
    }
    int64_t _r0 = x * x;
    return ok_int64_t_or_exception(_r0);
    catch0:
    return exception_int64_t_or_exception(_lastException);
}
int main() {
    string_1000 = str_const("Too large", 9);
    org_bau_Exception_exception _lastException;
    _int64_t_or_exception _x0;
    _x0 = square_1(3000000001);
    if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
    int64_t x = _x0.result;
    printf("%lld\n", (long long)x);
    goto skip0;
    catch0:;
    org_bau_Exception_exception e = _lastException;
        printf("%.*s\n", e.message->len, e.message->data);
        org_bau_Exception_exception_free(&e);
    skip0:;
    _end();
    return 0;
}
/*

type exception
An exception

fun exception(message i8[]) exception
 Create a new exception with a message

*/
