#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <limits.h>
#include <math.h>
#include <string.h>
#include <time.h>
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
typedef struct int_array int_array;
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
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
typedef struct org_bau_Utils_dateTime org_bau_Utils_dateTime;
struct org_bau_Utils_dateTime {
    int32_t year;
    int64_t month;
    int64_t day;
    int64_t hour;
    int64_t minute;
    int64_t second;
    int64_t millis;
    int32_t _refCount;
};
org_bau_Utils_dateTime org_bau_Utils_dateTime_new() {
    org_bau_Utils_dateTime result;
    result.year = 0;
    result.month = 0;
    result.day = 0;
    result.hour = 0;
    result.minute = 0;
    result.second = 0;
    result.millis = 0;
    return result;
}
double f64_1(double x);
int64_t idiv_2(int64_t a, int64_t b);
int64_t idx_2(int64_t x, int64_t len);
int64_t imod_2(int64_t a, int64_t b);
int64_t int_1(int64_t x);
double org_bau_Math_abs_1(double x);
int64_t org_bau_Math_appendF64_3(double n, i8_array* buff, int64_t pos);
int64_t org_bau_Math_appendInt_3(int64_t n, i8_array* buff, int64_t pos);
double org_bau_Math_asin_1(double x);
double org_bau_Math_atan_1(double x);
int64_t org_bau_Math_convertDoubleToLongBits_1(double x);
double org_bau_Math_cos_1(double x);
double org_bau_Math_exp_1(double x);
double org_bau_Math_floor_1(double x);
int64_t org_bau_Math_isNegativeZero_1(double x);
int64_t org_bau_Math_isNotANumber_1(double x);
double org_bau_Math_log_1(double x);
double org_bau_Math_max_2(double a, double b);
double org_bau_Math_min_2(double a, double b);
double org_bau_Math_pow_2(double x, double y);
double org_bau_Math_sin_1(double x);
double org_bau_Math_sqrt_1(double x);
int64_t org_bau_Std_ord_1(i8_array* s);
int64_t org_bau_Utils_random_0();
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
const double POS_INFINITY = (1.0 / 0.0);
const double NEG_INFINITY = (-1.0 / 0.0);
const double NOT_A_NUMBER = (0.0 / 0.0);
const double PI = 3.141592653589793;
const double E = 2.718281828459045;
const double LOG10 = 2.302585092994046;
const double LOG2 = 0.6931471805599453;
const int64_t MIN_INT = 0x8000000000000000;
const int64_t MAX_INT = 0x7fffffffffffffff;
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
i8_array* string_1003;
i8_array* string_1004;
i8_array* string_1005;
i8_array* string_1006;
int64_t randomSeed;
double f64_1(double x) {
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
int64_t org_bau_Math_appendF64_3(double n, i8_array* buff, int64_t pos) {
    int64_t e = 0;
    int64_t _t74 = n >= 1.0E8;
    if (!(_t74)) {
        int64_t _t75 = n <= 0.001;
        _t74 = _t75;
    }
    if (_t74) {
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
        pos = org_bau_Math_appendF64_3(n, buff, pos);
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
            continue27:;
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
double org_bau_Math_asin_1(double x) {
    return asin(x);
    double _t64 = org_bau_Math_sqrt_1(1 - ( x * x ));
    double _t65 = org_bau_Math_atan_1(x / _t64);
    return _t65;
}
double org_bau_Math_atan_1(double x) {
    return atan(x);
    x = org_bau_Math_min_2(org_bau_Math_max_2(-1.0E16, x), 1.0E16);
    double _t58 = org_bau_Math_abs_1(x);
    if (_t58 >= 0.4) {
        double _t59 = org_bau_Math_sqrt_1(1 + ( x * x ));
        double _t60 = org_bau_Math_atan_1(x / (1 + _t59));
        double _r13 = 2 * _t60;
        return _r13;
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
            continue7:;
            n += 1;
        }
        break;
    }
    return approx;
}
int64_t org_bau_Math_convertDoubleToLongBits_1(double x) {
    uint64_t y;
    memcpy(&y, &x, sizeof y);
    return y;
    if (x == 0) {
        int64_t _t67 = org_bau_Math_isNegativeZero_1(x);
        if (_t67) {
            return (-9223372036854775807LL-1LL);
        }
        return 0;
    }
    if (x < 0) {
        int64_t _t68 = org_bau_Math_convertDoubleToLongBits_1(- x);
        int64_t _r17 = _t68 | (-9223372036854775807LL-1LL);
        return _r17;
    }
    if (x == (1.0 / 0.0)) {
        return 9218868437227405312;
    }
    int64_t _t69 = org_bau_Math_isNotANumber_1(x);
    if (_t69) {
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
        int64_t _t70 = x < 2.3283064365386963E-10;
        if (_t70) {
            int64_t _t71 = exp > -990;
            _t70 = _t71;
        }
        if (!(_t70)) {
            break;
        }
        exp -= 32;
        x *= 4294967296;
    }
    while (1 == 1) {
        int64_t _t72 = x < 1;
        if (_t72) {
            int64_t _t73 = exp > -1022;
            _t72 = _t73;
        }
        if (!(_t72)) {
            break;
        }
        exp -= 1;
        x += x;
    }
    int64_t fraction = shiftRight_int_2(shiftLeft_2(int_1(x * 4503599627370496), 11), 11);
    int64_t _r18 = (shiftLeft_2((exp + 1022), 52)) + fraction;
    return _r18;
}
double org_bau_Math_cos_1(double x) {
    return cos(x);
    double _t61 = org_bau_Math_sin_1(x + 1.5707963267948966);
    return _t61;
}
double org_bau_Math_exp_1(double x) {
    return exp(x);
    int64_t _t26 = org_bau_Math_isNotANumber_1(x);
    int64_t _t27 = _t26;
    if (!(_t27)) {
        int64_t _t28 = x == (1.0 / 0.0);
        _t27 = _t28;
    }
    if (_t27) {
        return x;
    } else if (x < 0) {
        double _t29 = org_bau_Math_exp_1(- x);
        double _r4 = 1 / _t29;
        return _r4;
    }
    if (x > 2) {
        double r = org_bau_Math_exp_1(x / 2);
        double _r5 = r * r;
        return _r5;
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
    int64_t _t12 = x <= (-9223372036854775807LL-1LL);
    if (!(_t12)) {
        int64_t _t13 = x >= 9223372036854775807;
        _t12 = _t13;
    }
    int64_t _t14 = _t12;
    if (!(_t14)) {
        int64_t _t15 = org_bau_Math_isNotANumber_1(x);
        _t14 = _t15;
    }
    int64_t _t16 = _t14;
    if (!(_t16)) {
        int64_t _t17 = x == 0;
        _t16 = _t17;
    }
    if (_t16) {
        return x;
    }
    int64_t i = int_1(x);
    double d = f64_1(i);
    int64_t _t18 = d == x;
    if (!(_t18)) {
        int64_t _t19 = x >= 0;
        _t18 = _t19;
    }
    if (_t18) {
        return d;
    }
    double _r2 = d - 1;
    return _r2;
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
    int64_t _r1 = x != x;
    return _r1;
}
double org_bau_Math_log_1(double x) {
    return log(x);
    int64_t _t30 = x <= 0;
    if (!(_t30)) {
        int64_t _t31 = org_bau_Math_isNotANumber_1(x);
        _t30 = _t31;
    }
    if (x == 0) {
        return (-1.0 / 0.0);
    } else if (x == (1.0 / 0.0)) {
        return x;
    } else if (_t30) {
        return (0.0 / 0.0);
    }
    if (x < 0.7) {
        double _t32 = org_bau_Math_log_1(2 * x);
        double _r6 = _t32 - 0.6931471805599453;
        return _r6;
    }
    if (x >= 1.5) {
        double _t33 = org_bau_Math_log_1(x / 2);
        double _r7 = _t33 + 0.6931471805599453;
        return _r7;
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
            continue3:;
            i += 1;
        }
        break;
    }
    return result;
}
double org_bau_Math_max_2(double a, double b) {
    return fmax(a, b);
    int64_t _t7 = org_bau_Math_isNotANumber_1(a);
    if (_t7) {
        return a;
    }
    int64_t _t8 = a == b;
    if (_t8) {
        int64_t _t9 = a == 0;
        _t8 = _t9;
    }
    int64_t _t10 = _t8;
    if (_t10) {
        int64_t _t11 = org_bau_Math_isNegativeZero_1(a);
        _t10 = _t11;
    }
    if (_t10) {
        return b;
    }
    if (a >= b) {
        return a;
    }
    return b;
}
double org_bau_Math_min_2(double a, double b) {
    return fmin(a, b);
    int64_t _t2 = org_bau_Math_isNotANumber_1(a);
    if (_t2) {
        return a;
    }
    int64_t _t3 = a == b;
    if (_t3) {
        int64_t _t4 = a == 0;
        _t3 = _t4;
    }
    int64_t _t5 = _t3;
    if (_t5) {
        int64_t _t6 = org_bau_Math_isNegativeZero_1(b);
        _t5 = _t6;
    }
    if (_t5) {
        return b;
    }
    if (a <= b) {
        return a;
    }
    return b;
}
double org_bau_Math_pow_2(double x, double y) {
    return pow(x, y);
    int64_t _t37 = x > 0;
    if (_t37) {
        int64_t _t38 = y > 0;
        _t37 = _t38;
    }
    if (_t37) {
        double _t39 = org_bau_Math_log_1(x);
        double _t40 = org_bau_Math_exp_1(y * _t39);
        return _t40;
    }
    if (y == 0.0) {
        return 1.0;
    }
    int64_t _t41 = org_bau_Math_isNotANumber_1(x);
    int64_t _t42 = _t41;
    if (!(_t42)) {
        int64_t _t43 = org_bau_Math_isNotANumber_1(y);
        _t42 = _t43;
    }
    if (_t42) {
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
    int64_t odd = ((yy == y)) && (((yy & 1) == 1));
    int64_t _t44 = org_bau_Math_isNegativeZero_1(x);
    int64_t _t45 = _t44;
    if (_t45) {
        int64_t _t46 = yy == y;
        _t45 = _t46;
    }
    if (_t45) {
        if (odd) {
            double _t47 = org_bau_Math_pow_2(- x, y);
            double _r9 = - _t47;
            return _r9;
        }
        double _t48 = org_bau_Math_pow_2(- x, y);
        return _t48;
    }
    if (x >= 0) {
        double _t49 = org_bau_Math_log_1(x);
        double _t50 = org_bau_Math_exp_1(y * _t49);
        return _t50;
    }
    if (odd) {
        double _t51 = org_bau_Math_pow_2(- x, y);
        double _r10 = - _t51;
        return _r10;
    }
    if (x == (-1.0 / 0.0)) {
        if (y < 0) {
            return 0.0;
        }
        return (1.0 / 0.0);
    }
    if (yy == y) {
        double _t52 = org_bau_Math_pow_2(- x, y);
        return _t52;
    }
    double _t53 = org_bau_Math_abs_1(y);
    if (_t53 > 1.0E19) {
        if (y > 0) {
            double _t54 = org_bau_Math_pow_2(- x, y);
            return _t54;
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
double org_bau_Math_sin_1(double x) {
    return sin(x);
    if (x == (1.0 / 0.0)) {
        return (0.0 / 0.0);
    }
    double _t55 = org_bau_Math_abs_1(x);
    if (_t55 < 1.0E-8) {
        return x;
    }
    if (x < 0) {
        double _t56 = org_bau_Math_sin_1(- x);
        double _r11 = - _t56;
        return _r11;
    }
    if (x > 6.283185307179586) {
        x -= org_bau_Math_floor_1(x / 6.283185307179586) * 6.283185307179586;
    }
    if (x > 3.141592653589793) {
        double _t57 = org_bau_Math_sin_1(x - 3.141592653589793);
        double _r12 = - _t57;
        return _r12;
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
            continue5:;
            n += 1;
        }
        break;
    }
    return approx;
}
double org_bau_Math_sqrt_1(double x) {
    return sqrt(x);
    double _t34 = org_bau_Math_log_1(x);
    double _t35 = org_bau_Math_exp_1(_t34 / 2);
    return _t35;
}
int64_t org_bau_Std_ord_1(i8_array* s) {
    if (s->len) {
        char _r0 = s->data[idx_2(0, s->len)];
        return _r0;
    }
    return 0;
}
int64_t org_bau_Utils_random_0() {
    randomSeed += 0x9e3779b97f4a7c15;
    int64_t z = randomSeed;
    z = (z ^ (shiftRight_int_2(z, 30))) * -4658895280553007687;
    z = (z ^ (shiftRight_int_2(z, 27))) * -7723592293110705685;
    int64_t _r0 = z ^ (shiftRight_int_2(z, 31));
    return _r0;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
int main() {
    string_1000 = str_const("-", 1);
    string_1001 = str_const("0", 1);
    string_1002 = str_const("E", 1);
    string_1003 = str_const(".", 1);
    string_1004 = str_const("NaN", 3);
    string_1005 = str_const("Infinity", 8);
    string_1006 = str_const("-Infinity", 9);
    int64_t randomSeed = 0;
    double POS_INFINITY = (1.0 / 0.0);
    double NEG_INFINITY = (-1.0 / 0.0);
    double NOT_A_NUMBER = (0.0 / 0.0);
    double PI = 3.141592653589793;
    double E = 2.718281828459045;
    double LOG10 = 2.302585092994046;
    double LOG2 = 0.6931471805599453;
    int64_t MIN_INT = 0x8000000000000000;
    int64_t MAX_INT = 0x7fffffffffffffff;
    int64_t _t77 = org_bau_Utils_random_0();
    printf("%lld\n", _t77);
    printf("%.9f\n", 3.141592653589793);
    _end();
    return 0;
}
/*

type dateTime
Date and time.

fun getDateTime() dateTime
Get the local time in millisecond precision.

fun getNanoTime() int
Nanosecons since some undefined point in the past. Never jumps backwards.

fun getNanoTimeUTC() int
Nanoseconds since 1970 (epoch). May jump backwards when the system clock is adjusted.

fun setRandomSeed(seed int)
Set the random seed.

fun getRandomSeed() int
Get the random seed.

fun random() int
Pseudo-random number generated using the Splitmix64 algorithm.

fun ord(s i8[]) int const
The value of the first byte in the string. 0 if the string is empty.

*/
