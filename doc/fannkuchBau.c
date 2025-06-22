#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <math.h>
#include <string.h>
#define _malloc(a)      malloc(a)
#define _free(a)        free(a)
#define REF_COUNT_INC
#define REF_COUNT_STACK_INC
#define PRINT(...)
#define _end()
#define _traceMalloc(a)
#define _traceFree(a)
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
    memset(result->data, 0, sizeof(int8_t) * len);
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
    memset(result->data, 0, sizeof(int64_t) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
int64_t fannkuch_1(int64_t n);
int64_t idx_2(int64_t x, int64_t len);
i8_array* org_bau_Env_arg_1(int64_t index);
int64_t org_bau_Env_argCount_0();
int64_t org_bau_Math_parseInt_1(i8_array* value);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void i8_array_free(i8_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
void int_array_free(int_array* x) {
    _free(x->data); _traceFree(x->data);
    _free(x); _traceFree(x);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = (int8_t*) data;
    return result;
}
i8_array* string_1010;
i8_array* string_1011;
double POS_INFINITY;
double NEG_INFINITY;
double NOT_A_NUMBER;
double PI;
double E;
double LOG10;
double LOG2;
int64_t MIN_INT;
int64_t MAX_INT;
int64_t fannkuch_1(int64_t n) {
    int_array* _t0 = int_array_new(n);
    _incUseStack(_t0);
    int_array* perm1 = _t0;
    if (n > 0) {
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                perm1->data[idx_2(i, perm1->len)] = i;
                int64_t _next = i + 1;
                if (_next >= n) {
                    break;
                }
                i = _next;
            }
            break;
        }
    }
    int_array* _t1 = int_array_new(n);
    _incUseStack(_t1);
    int_array* perm = _t1;
    int_array* _t2 = int_array_new(n);
    _incUseStack(_t2);
    int_array* count = _t2;
    int64_t f = 0;
    int64_t flips = 0;
    int64_t nperm = 0;
    int64_t checksum = 0;
    int64_t k = 0;
    int64_t r = n;
    while (r > 0) {
        while (r > 1) {
            count->data[idx_2(r - 1, count->len)] = r;
            r -= 1;
        }
        if (n > 0) {
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    perm->data[idx_2(i, perm->len)] = perm1->data[idx_2(i, perm1->len)];
                    int64_t _next = i + 1;
                    if (_next >= n) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
        }
        f = 0;
        k = perm->data[idx_2(0, perm->len)];
        while (k != 0) {
            if (perm->len > 0) {
                while (1 == 1) {
                    int64_t i = 0;
                    while (1) {
                        if (( 2 * i ) >= k) {
                            break;
                        }
                        int64_t t = perm->data[i];
                        perm->data[i] = perm->data[k - i];
                        perm->data[k - i] = t;
                        int64_t _next = i + 1;
                        if (_next >= perm->len) {
                            break;
                        }
                        i = _next;
                    }
                    break;
                }
            }
            k = perm->data[idx_2(0, perm->len)];
            f += 1;
        }
        if (f > flips) {
            flips = f;
        }
        if ((nperm & 1) == 0) {
            checksum += f;
        } else {
            checksum -= f;
        }
        int64_t more = 1;
        while (more) {
            if (r == n) {
                printf("%lld\n", (long long)checksum);
                _decUseStack(count, int_array);
                _decUseStack(_t2, int_array);
                _decUseStack(perm, int_array);
                _decUseStack(_t1, int_array);
                _decUseStack(perm1, int_array);
                _decUseStack(_t0, int_array);
                return flips;
            }
            int64_t p0 = perm1->data[idx_2(0, perm1->len)];
            int64_t i = 0;
            while (i < r) {
                int64_t j = i + 1;
                perm1->data[idx_2(i, perm1->len)] = perm1->data[idx_2(j, perm1->len)];
                i = j;
            }
            perm1->data[idx_2(r, perm1->len)] = p0;
            count->data[idx_2(r, count->len)] -= 1;
            if (count->data[idx_2(r, count->len)] > 0) {
                more = 0;
            } else {
                r += 1;
            }
        }
        nperm += 1;
    }
    _decUseStack(count, int_array);
    _decUseStack(_t2, int_array);
    _decUseStack(perm, int_array);
    _decUseStack(_t1, int_array);
    _decUseStack(perm1, int_array);
    _decUseStack(_t0, int_array);
    return flips;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
i8_array* org_bau_Env_arg_1(int64_t index) {
    int64_t _t0 = index < 0;
    if (!(_t0)) {
        int64_t _t1 = index >= org_bau_Env_argCount_0();
        _t0 = _t1;
    }
    if (_t0) {
        i8_array* _t2 = i8_array_new(0);
        return _t2;
    }
    int64_t len = 0;
    len = strlen(__argv[index]);
    i8_array* _t3 = i8_array_new(len);
    _incUseStack(_t3);
    i8_array* result = _t3;
    strncpy((char*) result->data, __argv[index], len);
    _decUseStack(_t3, i8_array);
    return result;
}
int64_t org_bau_Env_argCount_0() {
    return __argc;
    return 0;
}
int64_t org_bau_Math_parseInt_1(i8_array* value) {
    int64_t x = 0;
    int64_t sign = 1;
    if (value->len) {
        int64_t i = 0;
        if (value->data[idx_2(0, value->len)] == 45) {
            sign = -1;
            i += 1;
        }
        while (i < value->len) {
            int8_t n = value->data[i];
            int64_t _t0 = n >= 48;
            if (_t0) {
                int64_t _t1 = n <= 57;
                _t0 = _t1;
            }
            if (_t0) {
                x *= 10;
                x += n - 48;
            }
            i += 1;
        }
        int64_t _r0 = sign * x;
        return _r0;
    }
    return 0;
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    string_1010 = str_const("Pfannkuchen(", 12);
    string_1011 = str_const(") is ", 5);
    POS_INFINITY = (1.0 / 0.0);
    NEG_INFINITY = (-1.0 / 0.0);
    NOT_A_NUMBER = (0.0 / 0.0);
    PI = 3.141592653589793;
    E = 2.718281828459045;
    LOG10 = 2.302585092994046;
    LOG2 = 0.6931471805599453;
    MIN_INT = 0x8000000000000000;
    MAX_INT = 0x7fffffffffffffff;
    int64_t n = 4;
    int64_t _t0 = org_bau_Env_argCount_0();
    if (_t0 > 1) {
        i8_array* _t1 = org_bau_Env_arg_1(1);
        int64_t _t2 = org_bau_Math_parseInt_1(_t1);
        n = _t2;
        _decUseStack(_t1, i8_array);
    }
    int64_t _t3 = fannkuch_1(n);
    printf("Pfannkuchen(%lld) is %lld\n", (long long)n, (long long)_t3);
    _end();
    return 0;
}
/*

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

*/
