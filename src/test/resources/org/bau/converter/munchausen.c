#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <limits.h>
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
int_array* getCache_0();
int64_t idiv_2(int64_t a, int64_t b);
int64_t imod_2(int64_t a, int64_t b);
int64_t isMunchausen_1(int64_t number);
int64_t pow_2(int64_t a, int64_t b);
void int_array_free(int_array* x);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
int_array* int_array_const(int64_t* data, uint32_t len) {
    int_array* result = _malloc(sizeof(int_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = (int64_t*) data;
    return result;
}
int_array* array_1000;
int64_t LIMIT;
int_array* cache;
int_array* getCache_0() {
    int_array* result = int_array_new(10);
    result->data[0] = 0;
    while (1 == 1) {
        int64_t i = 1;
        while (i < 10) {
            result->data[i] = pow_2(i, i);
            continue1:;
            i += 1;
        }
        break;
    }
    return result;
}
int64_t idiv_2(int64_t a, int64_t b) {
    if (b != 0) return a / b;
    if (a == 0) return 0;
    return a > 0 ? LLONG_MAX : LLONG_MIN;
}
int64_t imod_2(int64_t a, int64_t b) {
    if (b != 0) return a % b;
    return 0;
}
int64_t isMunchausen_1(int64_t number) {
    int64_t n = number;
    int64_t total = 0;
    while (n > 0) {
        int64_t digit = imod_2(n, 10);
        total += cache->data[digit];
        if (total > number) {
            return 0;
        }
        n = idiv_2(n, 10);
    }
    int64_t _r0 = total == number;
    return _r0;
}
int64_t pow_2(int64_t a, int64_t b) {
    int64_t result = 1;
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            result *= a;
            continue1:;
            int64_t _next = i + 1;
            if (_next >= b) {
                break;
            }
            i = _next;
        }
        break;
    }
    return result;
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    int64_t array_const_1000[] = {0, 1, 4, 27, 256, 3125, 46656, 823543, 16777216, 387420489};
    array_1000 = int_array_const(array_const_1000, 10);
    LIMIT = 4400;
    cache = array_1000;
    _incUseStack(cache);
    while (1 == 1) {
        int64_t i = 0;
        while (i < 4400) {
            int64_t _t0 = isMunchausen_1(i);
            if (_t0) {
                printf("%lld\n", (long long)i);
            }
            continue1:;
            i += 1;
        }
        break;
    }
    _end();
    return 0;
}
