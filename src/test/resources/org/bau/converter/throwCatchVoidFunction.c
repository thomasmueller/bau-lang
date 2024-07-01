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
typedef struct org_bau_Exception_exception org_bau_Exception_exception;
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
typedef struct _null_or_exception _null_or_exception;
struct _null_or_exception {
    org_bau_Exception_exception exception;
};
_null_or_exception ok_null_or_exception() {
    _null_or_exception x;
    x.exception.exceptionType = -1;
    return x;
}
_null_or_exception exception_null_or_exception(org_bau_Exception_exception exception) {
    _null_or_exception x;
    x.exception = exception;
    return x;
}
org_bau_Exception_exception org_bau_Exception_exception_1(i8_array* message);
_null_or_exception print_1(int64_t x);
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
org_bau_Exception_exception org_bau_Exception_exception_1(i8_array* message) {
    org_bau_Exception_exception result = org_bau_Exception_exception_new();
    result.exceptionType = 0;
    _decUse(result.message, i8_array);
    result.message = message;
    _incUse(result.message);
    return result;
}
_null_or_exception print_1(int64_t x) {
    _null_or_exception _x;
    if (x > 5) {
        org_bau_Exception_exception _t0 = org_bau_Exception_exception_1(string_1000);
        _x = exception_null_or_exception(_t0); goto catch0;
    }
    printf("x = %lld\n", x);
    return ok_null_or_exception();
    catch0:
    return exception_null_or_exception(_x.exception);
}
int main() {
    string_1000 = str_const("Value too large", 15);
    string_1001 = str_const("x = ", 4);
    string_1002 = str_const("Error: ", 7);
    _null_or_exception _x;
    int64_t i = 0;
    while (i <= 10) {
        _x = print_1(i);
        if (_x.exception.exceptionType != -1) goto catch0;
        goto skip0;
        catch0:;
        org_bau_Exception_exception e = _x.exception;
            printf("Error: %.*s\n", e.message->len, e.message->data);
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
