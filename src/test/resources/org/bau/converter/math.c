#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <limits.h>
#include <math.h>
#include <string.h>
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
double float_1(double x);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
int64_t imod_2(int64_t a, int64_t b);
int64_t int_1(int64_t x);
double org_bau_Math_abs_1(double x);
double org_bau_Math_acos_1(double x);
int64_t org_bau_Math_appendInt_3(int64_t n, i8_array* buff, int64_t pos);
int64_t org_bau_Math_appendfloat_3(double n, i8_array* buff, int64_t pos);
double org_bau_Math_asin_1(double x);
double org_bau_Math_atan_1(double x);
double org_bau_Math_ceil_1(double x);
int64_t org_bau_Math_convertDoubleToLongBits_1(double x);
double org_bau_Math_convertLongBitsToDouble_1(int64_t x);
double org_bau_Math_cos_1(double x);
double org_bau_Math_exp_1(double x);
double org_bau_Math_floor_1(double x);
int64_t org_bau_Math_isNegativeZero_1(double x);
int64_t org_bau_Math_isNotANumber_1(double x);
double org_bau_Math_log_1(double x);
double org_bau_Math_log10_1(double x);
double org_bau_Math_max_2(double a, double b);
double org_bau_Math_min_2(double a, double b);
double org_bau_Math_pow_2(double x, double y);
double org_bau_Math_round_1(double x);
double org_bau_Math_signum_1(double x);
double org_bau_Math_sin_1(double x);
double org_bau_Math_sqrt_1(double x);
double org_bau_Math_tan_1(double x);
int64_t org_bau_Std_ord_1(i8_array* s);
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
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
    result->_refCount = -1;
    result->data = data;
    return result;
}
i8_array* string_1024;
i8_array* string_1025;
i8_array* string_1026;
i8_array* string_1027;
i8_array* string_1028;
i8_array* string_1029;
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
i8_array* string_1014;
i8_array* string_1015;
i8_array* string_1016;
i8_array* string_1017;
i8_array* string_1018;
i8_array* string_1019;
i8_array* string_1020;
i8_array* string_1021;
i8_array* string_1022;
i8_array* string_1023;
double POS_INFINITY;
double NEG_INFINITY;
double NOT_A_NUMBER;
double PI;
double E;
double LOG10;
double LOG2;
int64_t MIN_INT;
int64_t MAX_INT;
double float_1(double x) {
    return x;
}
int64_t idiv_2(int64_t a, int64_t b) {
    if (b != 0) return a / b;
    if (a == 0) return 0;
    return a > 0 ? LLONG_MAX : LLONG_MIN;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
int64_t imod_2(int64_t a, int64_t b) {
    if (b != 0) return a % b;
    if (a == 0) return 0;
    return a > 0 ? LLONG_MAX : LLONG_MIN;
}
int64_t int_1(int64_t x) {
    return x;
}
double org_bau_Math_abs_1(double x) {
    return fabs(x);
    if (x >= 0) {
        return x;
    }
    double _r0 = - x;
    return _r0;
}
double org_bau_Math_acos_1(double x) {
    return acos(x);
    double _t0 = org_bau_Math_asin_1(x);
    double _r0 = 1.5707963267948966 - _t0;
    return _r0;
}
int64_t org_bau_Math_appendInt_3(int64_t n, i8_array* buff, int64_t pos) {
    if (n < 0) {
        buff->data[idx_2(pos, buff->len)] = 45;
        pos += 1;
    } else {
        n = - n;
    }
    int64_t start = pos;
    while (1) {
        buff->data[idx_2(pos, buff->len)] = 48 - (imod_2(n, 10));
        pos += 1;
        n /= 10;
        if (n == 0) {
            break;
        }
    }
    int64_t end = pos;
    while (pos > start) {
        pos -= 1;
        char temp = buff->data[idx_2(pos, buff->len)];
        buff->data[idx_2(pos, buff->len)] = buff->data[idx_2(start, buff->len)];
        buff->data[idx_2(start, buff->len)] = temp;
        start += 1;
    }
    return end;
}
int64_t org_bau_Math_appendfloat_3(double n, i8_array* buff, int64_t pos) {
    int64_t e = 0;
    int64_t _t0 = n >= 1.0E8;
    if (!(_t0)) {
        int64_t _t1 = n <= 0.001;
        _t0 = _t1;
    }
    if (_t0) {
        while (n > 1.0E20) {
            n /= 1.0E20;
            e += 20;
        }
        while (n < 1.0E-20) {
            n *= 1.0E20;
            e -= 20;
        }
        while (n >= 10) {
            n /= 10;
            e += 1;
        }
        while (n < 1) {
            n *= 10;
            e -= 1;
        }
        pos = org_bau_Math_appendfloat_3(n, buff, pos);
        buff->data[idx_2(pos, buff->len)] = 69;
        pos += 1;
        pos = org_bau_Math_appendInt_3(e, buff, pos);
        return pos;
    }
    pos = org_bau_Math_appendInt_3(int_1(n), buff, pos);
    buff->data[idx_2(pos, buff->len)] = 46;
    pos += 1;
    n = (n - int_1(n)) * 10;
    int64_t y = int_1(n * 100000000000000000);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            if (y <= 0) {
                break;
            }
            int64_t x = idiv_2(y, 100000000000000000);
            buff->data[idx_2(pos, buff->len)] = 48 + (imod_2(x, 10));
            pos += 1;
            y -= x * 100000000000000000;
            y *= 10;
            continue5:;
            int64_t _next = i + 1;
            if (_next >= 19) {
                break;
            }
            i = _next;
        }
        break;
    }
    return pos;
}
double org_bau_Math_asin_1(double x) {
    return asin(x);
    double _t0 = org_bau_Math_sqrt_1(1 - ( x * x ));
    double _t1 = org_bau_Math_atan_1(x / _t0);
    return _t1;
}
double org_bau_Math_atan_1(double x) {
    return atan(x);
    x = org_bau_Math_min_2(org_bau_Math_max_2(-1.0E16, x), 1.0E16);
    double _t0 = org_bau_Math_abs_1(x);
    if (_t0 >= 0.4) {
        double _t1 = org_bau_Math_sqrt_1(1 + ( x * x ));
        double _t2 = org_bau_Math_atan_1(x / (1 + _t1));
        double _r0 = 2 * _t2;
        return _r0;
    }
    double approx = x;
    int64_t sign = -1;
    double p = x;
    while (1 == 1) {
        int64_t n = 3;
        while (n < 32) {
            p *= x * x;
            approx += sign * p / n;
            sign = - sign;
            n += 1;
            continue1:;
            n += 1;
        }
        break;
    }
    return approx;
}
double org_bau_Math_ceil_1(double x) {
    return ceil(x);
    double _t0 = org_bau_Math_floor_1(- x);
    double _r0 = - _t0;
    return _r0;
}
int64_t org_bau_Math_convertDoubleToLongBits_1(double x) {
    uint64_t y;
    memcpy(&y, &x, sizeof y);
    return y;
    if (x == 0) {
        int64_t _t0 = org_bau_Math_isNegativeZero_1(x);
        if (_t0) {
            return (-9223372036854775807LL-1LL);
        }
        return 0;
    }
    if (x < 0) {
        int64_t _t1 = org_bau_Math_convertDoubleToLongBits_1(- x);
        int64_t _r0 = _t1 | (-9223372036854775807LL-1LL);
        return _r0;
    }
    if (x == (1.0 / 0.0)) {
        return 9218868437227405312;
    }
    int64_t _t2 = org_bau_Math_isNotANumber_1(x);
    if (_t2) {
        return 9221120237041090560;
    }
    int64_t exp = 0;
    while (x >= 4294967296) {
        exp += 32;
        x /= 4294967296;
    }
    while (x >= 2) {
        exp += 1;
        x /= 2;
    }
    while (1 == 1) {
        int64_t _t3 = x < 2.3283064365386963E-10;
        if (_t3) {
            int64_t _t4 = exp > -990;
            _t3 = _t4;
        }
        if (!(_t3)) {
            break;
        }
        exp -= 32;
        x *= 4294967296;
    }
    while (1 == 1) {
        int64_t _t5 = x < 1;
        if (_t5) {
            int64_t _t6 = exp > -1022;
            _t5 = _t6;
        }
        if (!(_t5)) {
            break;
        }
        exp -= 1;
        x += x;
    }
    int64_t _t7 = int_1(x * 4503599627370496);
    int64_t fraction = shiftRight_int_2(shiftLeft_2(_t7, 11), 11);
    int64_t _r1 = (shiftLeft_2((exp + 1022), 52)) + fraction;
    return _r1;
}
double org_bau_Math_convertLongBitsToDouble_1(int64_t x) {
    double y;
    memcpy(&y, &x, sizeof y);
    return y;
    int64_t sign = shiftRight_int_2(x, 63);
    int64_t exp = (shiftRight_int_2(x, 52)) & 2047;
    if (x == 0) {
        return 0.0;
    } else if (x == (-9223372036854775807LL-1LL)) {
        return -0.0;
    }
    int64_t fraction = shiftRight_int_2(shiftLeft_2(x, 12), 12);
    if (exp == 2047) {
        if (fraction != 0) {
            return (0.0 / 0.0);
        } else if (sign == 0) {
            return (1.0 / 0.0);
        }
        return (-1.0 / 0.0);
    }
    double result = 0.0;
    while (1 == 1) {
        int64_t i = 0;
        while (i < 52) {
            if ((fraction & 1) == 1) {
                result += 1;
            }
            result /= 2;
            fraction >>= 1;
            continue1:;
            i += 1;
        }
        break;
    }
    if (exp == 0) {
        result *= 2;
    } else {
        result += 1;
    }
    exp -= 1023;
    while (exp <= -32) {
        result /= 4294967296;
        exp += 32;
    }
    while (exp < 0) {
        result /= 2;
        exp += 1;
    }
    while (exp >= 32) {
        result = result * 4294967296;
        exp -= 32;
    }
    while (exp > 0) {
        result += result;
        exp = -1;
    }
    if (sign == 0) {
        return result;
    }
    double _r0 = - result;
    return _r0;
}
double org_bau_Math_cos_1(double x) {
    return cos(x);
    double _t0 = org_bau_Math_sin_1(x + 1.5707963267948966);
    return _t0;
}
double org_bau_Math_exp_1(double x) {
    return exp(x);
    int64_t _t0 = org_bau_Math_isNotANumber_1(x);
    int64_t _t1 = _t0;
    if (!(_t1)) {
        int64_t _t2 = x == (1.0 / 0.0);
        _t1 = _t2;
    }
    if (_t1) {
        return x;
    } else if (x < 0) {
        double _t3 = org_bau_Math_exp_1(- x);
        double _r0 = 1 / _t3;
        return _r0;
    }
    if (x > 2) {
        double r = org_bau_Math_exp_1(x / 2);
        double _r1 = r * r;
        return _r1;
    }
    double approx = 1.0;
    double term = 1.0;
    while (1 == 1) {
        int64_t i = 1;
        while (i < 22) {
            term *= x / i;
            approx += term;
            continue1:;
            i += 1;
        }
        break;
    }
    return approx;
}
double org_bau_Math_floor_1(double x) {
    return floor(x);
    int64_t _t0 = x <= (-9223372036854775807LL-1LL);
    if (!(_t0)) {
        int64_t _t1 = x >= 9223372036854775807;
        _t0 = _t1;
    }
    int64_t _t2 = _t0;
    if (!(_t2)) {
        int64_t _t3 = org_bau_Math_isNotANumber_1(x);
        _t2 = _t3;
    }
    int64_t _t4 = _t2;
    if (!(_t4)) {
        int64_t _t5 = x == 0;
        _t4 = _t5;
    }
    if (_t4) {
        return x;
    }
    int64_t i = int_1(x);
    double d = float_1(i);
    int64_t _t6 = d == x;
    if (!(_t6)) {
        int64_t _t7 = x >= 0;
        _t6 = _t7;
    }
    if (_t6) {
        return d;
    }
    double _r0 = d - 1;
    return _r0;
}
int64_t org_bau_Math_isNegativeZero_1(double x) {
    int64_t _t0 = (x == 0.0);
    if (_t0) {
        int64_t _t1 = (( 1.0 / x ) == (-1.0 / 0.0));
        _t0 = _t1;
    }
    return _t0;
}
int64_t org_bau_Math_isNotANumber_1(double x) {
    return isnan(x);
    int64_t _r0 = x != x;
    return _r0;
}
double org_bau_Math_log_1(double x) {
    return log(x);
    int64_t _t0 = x <= 0;
    if (!(_t0)) {
        int64_t _t1 = org_bau_Math_isNotANumber_1(x);
        _t0 = _t1;
    }
    if (x == 0) {
        return (-1.0 / 0.0);
    } else if (x == (1.0 / 0.0)) {
        return x;
    } else if (_t0) {
        return (0.0 / 0.0);
    }
    if (x < 0.7) {
        double _t2 = org_bau_Math_log_1(2 * x);
        double _r0 = _t2 - 0.6931471805599453;
        return _r0;
    }
    if (x >= 1.5) {
        double _t3 = org_bau_Math_log_1(x / 2);
        double _r1 = _t3 + 0.6931471805599453;
        return _r1;
    }
    double base = x - 1;
    int64_t sign = 1;
    double term = base;
    double result = term;
    while (1 == 1) {
        int64_t i = 2;
        while (i < 30) {
            sign = - sign;
            term *= base;
            result += sign * term / i;
            continue1:;
            i += 1;
        }
        break;
    }
    return result;
}
double org_bau_Math_log10_1(double x) {
    return log10(x);
    double _t0 = org_bau_Math_log_1(x);
    double _r0 = _t0 / 2.302585092994046;
    return _r0;
}
double org_bau_Math_max_2(double a, double b) {
    return fmax(a, b);
    int64_t _t0 = org_bau_Math_isNotANumber_1(a);
    if (_t0) {
        return a;
    }
    int64_t _t1 = a == b;
    if (_t1) {
        int64_t _t2 = a == 0;
        _t1 = _t2;
    }
    int64_t _t3 = _t1;
    if (_t3) {
        int64_t _t4 = org_bau_Math_isNegativeZero_1(a);
        _t3 = _t4;
    }
    if (_t3) {
        return b;
    }
    if (a >= b) {
        return a;
    }
    return b;
}
double org_bau_Math_min_2(double a, double b) {
    return fmin(a, b);
    int64_t _t0 = org_bau_Math_isNotANumber_1(a);
    if (_t0) {
        return a;
    }
    int64_t _t1 = a == b;
    if (_t1) {
        int64_t _t2 = a == 0;
        _t1 = _t2;
    }
    int64_t _t3 = _t1;
    if (_t3) {
        int64_t _t4 = org_bau_Math_isNegativeZero_1(b);
        _t3 = _t4;
    }
    if (_t3) {
        return b;
    }
    if (a <= b) {
        return a;
    }
    return b;
}
double org_bau_Math_pow_2(double x, double y) {
    return pow(x, y);
    int64_t _t0 = x > 0;
    if (_t0) {
        int64_t _t1 = y > 0;
        _t0 = _t1;
    }
    if (_t0) {
        double _t2 = org_bau_Math_log_1(x);
        double _t3 = org_bau_Math_exp_1(y * _t2);
        return _t3;
    }
    if (y == 0.0) {
        return 1.0;
    }
    int64_t _t4 = org_bau_Math_isNotANumber_1(x);
    int64_t _t5 = _t4;
    if (!(_t5)) {
        int64_t _t6 = org_bau_Math_isNotANumber_1(y);
        _t5 = _t6;
    }
    if (_t5) {
        return (0.0 / 0.0);
    }
    double absX = org_bau_Math_abs_1(x);
    if (y == (-1.0 / 0.0)) {
        if (absX > 1.0) {
            return 0.0;
        }
        if (absX == 1.0) {
            return (0.0 / 0.0);
        }
        return (1.0 / 0.0);
    }
    int64_t yy = int_1(y);
    int64_t _t7 = (yy == y);
    if (_t7) {
        int64_t _t8 = ((yy & 1) == 1);
        _t7 = _t8;
    }
    int64_t odd = _t7;
    int64_t _t9 = org_bau_Math_isNegativeZero_1(x);
    int64_t _t10 = _t9;
    if (_t10) {
        int64_t _t11 = yy == y;
        _t10 = _t11;
    }
    if (_t10) {
        if (odd) {
            double _t12 = org_bau_Math_pow_2(- x, y);
            double _r0 = - _t12;
            return _r0;
        }
        double _t13 = org_bau_Math_pow_2(- x, y);
        return _t13;
    }
    if (x >= 0) {
        double _t14 = org_bau_Math_log_1(x);
        double _t15 = org_bau_Math_exp_1(y * _t14);
        return _t15;
    }
    if (odd) {
        double _t16 = org_bau_Math_pow_2(- x, y);
        double _r1 = - _t16;
        return _r1;
    }
    if (x == (-1.0 / 0.0)) {
        if (y < 0) {
            return 0.0;
        }
        return (1.0 / 0.0);
    }
    if (yy == y) {
        double _t17 = org_bau_Math_pow_2(- x, y);
        return _t17;
    }
    double _t18 = org_bau_Math_abs_1(y);
    if (_t18 > 1.0E19) {
        if (y > 0) {
            double _t19 = org_bau_Math_pow_2(- x, y);
            return _t19;
        }
        if (x == -1) {
            return 1;
        }
        if (absX > 1) {
            return 0;
        }
        return (1.0 / 0.0);
    }
    return (0.0 / 0.0);
}
double org_bau_Math_round_1(double x) {
    return floor(x + 0.5);
    int64_t _t0 = org_bau_Math_isNotANumber_1(x);
    if (_t0) {
        return 0;
    }
    if (x <= (-9223372036854775807LL-1LL)) {
        return (-9223372036854775807LL-1LL);
    }
    if (x >= 9223372036854775807) {
        return 9223372036854775807;
    }
    double _t1 = org_bau_Math_floor_1(x + 0.5);
    int64_t _t2 = int_1(_t1);
    return _t2;
}
double org_bau_Math_signum_1(double x) {
    int64_t _t0 = x == 0.0;
    if (!(_t0)) {
        int64_t _t1 = org_bau_Math_isNotANumber_1(x);
        _t0 = _t1;
    }
    if (_t0) {
        return x;
    }
    if (x >= 0) {
        return 1;
    }
    return -1;
}
double org_bau_Math_sin_1(double x) {
    return sin(x);
    if (x == (1.0 / 0.0)) {
        return (0.0 / 0.0);
    }
    double _t0 = org_bau_Math_abs_1(x);
    if (_t0 < 1.0E-8) {
        return x;
    }
    if (x < 0) {
        double _t1 = org_bau_Math_sin_1(- x);
        double _r0 = - _t1;
        return _r0;
    }
    if (x > 6.283185307179586) {
        x -= org_bau_Math_floor_1(x / 6.283185307179586) * 6.283185307179586;
    }
    if (x > 3.141592653589793) {
        double _t2 = org_bau_Math_sin_1(x - 3.141592653589793);
        double _r1 = - _t2;
        return _r1;
    }
    if (x > 1.5707963267948966) {
        x = 3.141592653589793 - x;
    }
    double approx = 0.0;
    int64_t sign = 1;
    double p = x;
    while (1 == 1) {
        int64_t n = 2;
        while (n < 24) {
            approx += sign * p;
            p *= x * x / (n * (n + 1));
            sign = - sign;
            n += 1;
            continue1:;
            n += 1;
        }
        break;
    }
    return approx;
}
double org_bau_Math_sqrt_1(double x) {
    return sqrt(x);
    double _t0 = org_bau_Math_log_1(x);
    double _t1 = org_bau_Math_exp_1(_t0 / 2);
    return _t1;
}
double org_bau_Math_tan_1(double x) {
    return tan(x);
    double _t0 = org_bau_Math_sin_1(x);
    double _t1 = org_bau_Math_cos_1(x);
    double _r0 = _t0 / _t1;
    return _r0;
}
int64_t org_bau_Std_ord_1(i8_array* s) {
    if (s->len) {
        char _r0 = s->data[idx_2(0, s->len)];
        return _r0;
    }
    return 0;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
int main() {
    string_1024 = str_const("isNegativeZero ", 15);
    string_1025 = str_const("convertDoubleToLongBits ", 24);
    string_1026 = str_const("convertLongBitsToDouble ", 24);
    string_1027 = str_const("pow ", 4);
    string_1028 = str_const("min ", 4);
    string_1029 = str_const("max ", 4);
    string_1000 = str_const("-", 1);
    string_1001 = str_const("0", 1);
    string_1002 = str_const("E", 1);
    string_1003 = str_const(".", 1);
    string_1004 = str_const("NaN", 3);
    string_1005 = str_const("Infinity", 8);
    string_1006 = str_const("-Infinity", 9);
    string_1007 = str_const("signum ", 7);
    string_1008 = str_const(" = ", 3);
    string_1009 = str_const("abs    ", 7);
    string_1010 = str_const("floor  ", 7);
    string_1011 = str_const("ceil   ", 7);
    string_1012 = str_const("round  ", 7);
    string_1013 = str_const("exp    ", 7);
    string_1014 = str_const("log    ", 7);
    string_1015 = str_const("log10  ", 7);
    string_1016 = str_const("sqrt   ", 7);
    string_1017 = str_const("sin    ", 7);
    string_1018 = str_const("cos    ", 7);
    string_1019 = str_const("tan    ", 7);
    string_1020 = str_const("atan   ", 7);
    string_1021 = str_const("asin   ", 7);
    string_1022 = str_const("acos   ", 7);
    string_1023 = str_const("isNotANumber   ", 15);
    double POS_INFINITY = (1.0 / 0.0);
    double NEG_INFINITY = (-1.0 / 0.0);
    double NOT_A_NUMBER = (0.0 / 0.0);
    double PI = 3.141592653589793;
    double E = 2.718281828459045;
    double LOG10 = 2.302585092994046;
    double LOG2 = 0.6931471805599453;
    int64_t MIN_INT = 0x8000000000000000;
    int64_t MAX_INT = 0x7fffffffffffffff;
    double x = -1.0;
    x = -1;
    while (x <= 1) {
        double _t0 = org_bau_Math_signum_1(x);
        printf("signum %.9f = %.9f\n", x, _t0);
        double _t1 = org_bau_Math_abs_1(x);
        printf("abs    %.9f = %.9f\n", x, _t1);
        double _t2 = org_bau_Math_floor_1(x);
        printf("floor  %.9f = %.9f\n", x, _t2);
        double _t3 = org_bau_Math_ceil_1(x);
        printf("ceil   %.9f = %.9f\n", x, _t3);
        double _t4 = org_bau_Math_round_1(x);
        printf("round  %.9f = %.9f\n", x, _t4);
        double _t5 = org_bau_Math_exp_1(x);
        printf("exp    %.9f = %.9f\n", x, _t5);
        double _t6 = org_bau_Math_log_1(x);
        printf("log    %.9f = %.9f\n", x, _t6);
        double _t7 = org_bau_Math_log10_1(x);
        printf("log10  %.9f = %.9f\n", x, _t7);
        double _t8 = org_bau_Math_sqrt_1(x);
        printf("sqrt   %.9f = %.9f\n", x, _t8);
        double _t9 = org_bau_Math_sin_1(x);
        printf("sin    %.9f = %.9f\n", x, _t9);
        double _t10 = org_bau_Math_cos_1(x);
        printf("cos    %.9f = %.9f\n", x, _t10);
        double _t11 = org_bau_Math_tan_1(x);
        printf("tan    %.9f = %.9f\n", x, _t11);
        double _t12 = org_bau_Math_atan_1(x);
        printf("atan   %.9f = %.9f\n", x, _t12);
        double _t13 = org_bau_Math_asin_1(x);
        printf("asin   %.9f = %.9f\n", x, _t13);
        double _t14 = org_bau_Math_acos_1(x);
        printf("acos   %.9f = %.9f\n", x, _t14);
        x += 0.5;
    }
    x = -1;
    while (x <= 1) {
        int64_t _t15 = org_bau_Math_isNotANumber_1(x);
        printf("isNotANumber   %.9f = %lld\n", x, (long long)_t15);
        int64_t _t16 = org_bau_Math_isNegativeZero_1(x);
        printf("isNegativeZero %.9f = %lld\n", x, (long long)_t16);
        int64_t _t17 = org_bau_Math_convertDoubleToLongBits_1(x);
        printf("convertDoubleToLongBits %.9f = %lld\n", x, (long long)_t17);
        double _t18 = org_bau_Math_convertLongBitsToDouble_1(4602678819172646912);
        printf("convertLongBitsToDouble %.9f = %.9f\n", x, _t18);
        x += 0.5;
    }
    x = -1;
    while (x <= 1) {
        double y = -1.0;
        while (y <= 1) {
            double _t19 = org_bau_Math_pow_2(x, y);
            printf("pow %.9f\n", _t19);
            double _t20 = org_bau_Math_min_2(x, y);
            printf("min %.9f\n", _t20);
            double _t21 = org_bau_Math_max_2(x, y);
            printf("max %.9f\n", _t21);
            y += 0.5;
        }
        x += 0.5;
    }
    _end();
    return 0;
}
/*

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

*/
