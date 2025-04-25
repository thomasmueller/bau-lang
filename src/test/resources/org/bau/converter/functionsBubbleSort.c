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
typedef struct int_array int_array;
struct int_array;
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
/* global */
int __argc;
char **__argv;
/* functions */
void bubbleSort_int_var(int64_t _T, int _vaCount,...);
void int_array_free(int_array* x);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void bubbleSort_int_var(int64_t _T, int _vaCount,...) {
    va_list _vaList;
    int_array* array = int_array_new(_vaCount);
    va_start(_vaList, _vaCount);
    for (int _vaI = 0; _vaI < _vaCount; _vaI++) {
        array->data[_vaI] = va_arg(_vaList, int64_t);
    }
    va_end(_vaList);
    int32_t n = array->len;
    while (1) {
        int64_t swapped = 0;
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                if (array->data[i] > array->data[i + 1]) {
                    int64_t tmp = array->data[i];
                    array->data[i] = array->data[i + 1];
                    array->data[i + 1] = tmp;
                    swapped = 1;
                }
                continue2:;
                int64_t _next = i + 1;
                if (_next >= ( array->len - 1 )) {
                    break;
                }
                i = _next;
            }
            break;
        }
        n -= 1;
        if (swapped == 0) {
            break;
        }
    }
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            printf("%lld\n", (long long)array->data[i]);
            continue4:;
            int64_t _next = i + 1;
            if (_next >= array->len) {
                break;
            }
            i = _next;
        }
        break;
    }
    _decUseStack(array, int_array);
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    bubbleSort_int_var(0, /* argCount */ 3, 1, 3, 2);
    _end();
    return 0;
}
