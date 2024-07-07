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
i8_array* hex_2(int64_t x, int64_t len);
int32_t i32_1(int32_t x);
int64_t idx_2(int64_t x, int64_t len);
int32_t murmur3_32_1(i8_array* data);
int64_t org_bau_Std_ord_1(i8_array* s);
int32_t rotLefti32_2(int32_t x, int32_t n);
int64_t shiftLeft_2(int64_t a, int64_t b);
int32_t shiftRight_i32_2(int32_t a, int64_t b);
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
i8_array* hex_2(int64_t x, int64_t len) {
    int64_t l = len;
    if (l < 0) {
        l = 0;
    } else if (l > 16) {
        l = 16;
    }
    i8_array* data = i8_array_new(l);
    int64_t y = x;
    int64_t i = l - 1;
    while (i >= 0) {
        int64_t c = 48 + (y & 15);
        if ((y & 15) > 9) {
            c = ( 97 + (y & 15) ) - 10;
        }
        data->data[idx_2(i, data->len)] = c;
        y >>= 4;
        i -= 1;
    }
    return data;
    _decUse(data, i8_array);
}
int32_t i32_1(int32_t x) {
    return x;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
int32_t murmur3_32_1(i8_array* data) {
    if (data->len <= 0) {
        return 0;
    }
    int32_t _t0 = i32_1(0xcc9e2d51);
    int32_t c1 = _t0;
    int32_t _t1 = i32_1(0x1b873593);
    int32_t c2 = _t1;
    int32_t _t2 = i32_1(0);
    int32_t h = _t2;
    int64_t i = 0;
    if (i < ( data->len - 3 )) {
        while (1) {
            int32_t _t3 = i32_1(((data->data[i] & 255)) | (shiftLeft_2((data->data[i + 1] & 255), 8)) | (shiftLeft_2((data->data[i + 2] & 255), 16)) | (shiftLeft_2((data->data[i + 3] & 255), 24)));
            int32_t x = _t3;
            x *= c1;
            x = rotLefti32_2(x, 15);
            x *= c2;
            h ^= x;
            h = rotLefti32_2(h, 13);
            h = ( h * 5 ) + 3864292196L;
            int64_t n = i + 4;
            if (n > ( data->len - 4 )) {
                break;
            }
            i = n;
        }
    }
    int32_t _t4 = i32_1(0);
    int32_t x = _t4;
    if (i < ( data->len - 4 )) {
        i += 4;
    }
    int64_t _t5 = i < data->len;
    if (_t5) {
        int64_t _t6 = i != ( data->len - 4 );
        _t5 = _t6;
    }
    if (_t5) {
        int64_t s = 0;
        while (1) {
            x ^= shiftLeft_2((data->data[i] & 255), s);
            int64_t n = i + 1;
            if (n >= data->len) {
                break;
            }
            i = n;
            s += 8;
        }
        x *= c1;
        x = rotLefti32_2(x, 15);
        x *= c2;
        h ^= x;
    }
    h ^= data->len;
    h ^= shiftRight_i32_2(h, 16);
    h *= 0x85ebca6b;
    h ^= shiftRight_i32_2(h, 13);
    h *= 0xc2b2ae35;
    h ^= shiftRight_i32_2(h, 16);
    return h;
}
int64_t org_bau_Std_ord_1(i8_array* s) {
    if (s->len) {
        char _r0 = s->data[idx_2(0, s->len)];
        return _r0;
    }
    return 0;
}
int32_t rotLefti32_2(int32_t x, int32_t n) {
    int64_t _r0 = (shiftLeft_2(x, n)) | (shiftRight_i32_2(x, (32 - n)));
    return _r0;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int32_t shiftRight_i32_2(int32_t a, int64_t b) {
    return ((uint32_t) a) >> b;
}
int main() {
    string_1000 = str_const("0", 1);
    string_1001 = str_const("a", 1);
    string_1002 = str_const("test", 4);
    int32_t _t7 = murmur3_32_1(string_1002);
    i8_array* _t8 = hex_2(_t7, 8);
    printf("%.*s\n", _t8->len, _t8->data);
    _decUse(_t8, i8_array);
    _end();
    return 0;
}
/*

fun ord(s i8[]) int const
The value of the first byte in the string. 0 if the string is empty.

*/
