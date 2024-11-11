#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#define _incUse(a, g) if(a){(a)->_refCount++;}
#define _decUse(a, type, g) if(a){if(--((a)->_refCount) == 0) type##_free(a);}
#define _malloc(a) malloc(a)
#define _traceMalloc(a) ;
#define _free(a) free(a)
#define _end() ;
/* types */
typedef struct i8_array i8_array;
struct i8_array;
typedef struct int_array int_array;
struct int_array;
typedef struct HashMap HashMap;
struct HashMap;
typedef struct str str;
struct str;
typedef struct str_array str_array;
struct str_array;
typedef struct HashMap_int_int HashMap_int_int;
struct HashMap_int_int;
typedef struct HashMap_str_str HashMap_str_str;
struct HashMap_str_str;
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
struct HashMap {
    int32_t _refCount;
};
HashMap* HashMap_new() {
    HashMap* result = _malloc(sizeof(HashMap));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct str {
    i8_array* value;
    int32_t _refCount;
};
str str_new() {
    str result;
    result.value = 0;
    return result;
}
struct str_array {
    int32_t len;
    str* data;
    int32_t _refCount;
};
str_array* str_array_new(uint32_t len) {
    str_array* result = _malloc(sizeof(str_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(str) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct HashMap_int_int {
    int64_t size;
    int_array* keys;
    int_array* values;
    int_array* hashes;
    int32_t _refCount;
};
HashMap_int_int* HashMap_int_int_new() {
    HashMap_int_int* result = _malloc(sizeof(HashMap_int_int));
    _traceMalloc(result);
    result->_refCount = 1;
    result->size = 0;
    result->keys = 0;
    result->values = 0;
    result->hashes = 0;
    return result;
}
struct HashMap_str_str {
    int64_t size;
    str_array* keys;
    str_array* values;
    int_array* hashes;
    int32_t _refCount;
};
HashMap_str_str* HashMap_str_str_new() {
    HashMap_str_str* result = _malloc(sizeof(HashMap_str_str));
    _traceMalloc(result);
    result->_refCount = 1;
    result->size = 0;
    result->keys = 0;
    result->values = 0;
    result->hashes = 0;
    return result;
}
/* exception types */
/* functions */
int64_t HashMap_int_int_get_2(HashMap_int_int* this, int64_t key);
void HashMap_int_int_put_3(HashMap_int_int* this, int64_t key, int64_t value);
int64_t HashMap_int_int_remove_2(HashMap_int_int* this, int64_t key);
str HashMap_str_str_get_2(HashMap_str_str* this, str key);
void HashMap_str_str_put_3(HashMap_str_str* this, str key, str value);
int64_t hashCode_1(i8_array* data);
int64_t idx_2(int64_t x, int64_t len);
int64_t int_equals_2(int64_t this, int64_t other);
int64_t int_hashCode_1(int64_t this);
int64_t mix_1(int64_t z);
HashMap_int_int* newHashMap_int_int_2(int64_t _K, int64_t _V);
HashMap_str_str* newHashMap_str_str_2(int64_t _K, int64_t _V);
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
str str_1(i8_array* x);
int64_t str_equals_2(str this, str other);
int64_t str_hashCode_1(str this);
void test_0();
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void HashMap_free(HashMap* x);
void str_free(str* x);
void str_array_free(str_array* x);
void HashMap_int_int_free(HashMap_int_int* x);
void HashMap_str_str_free(HashMap_str_str* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void HashMap_free(HashMap* x) {
    _free(x);
}
void str_free(str* x) {
    _decUse(x->value, i8_array, 0);
}
void str_array_free(str_array* x) {
    for (int i = 0; i < x->len; i++) str_free(&(x->data[i]));
    _free(x->data);
    _free(x);
}
void HashMap_int_int_free(HashMap_int_int* x) {
    _decUse(x->keys, int_array, 0);
    _decUse(x->values, int_array, 0);
    _decUse(x->hashes, int_array, 0);
    _free(x);
}
void HashMap_str_str_free(HashMap_str_str* x) {
    _decUse(x->keys, str_array, 0);
    _decUse(x->values, str_array, 0);
    _decUse(x->hashes, int_array, 0);
    _free(x);
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
i8_array* string_1003;
i8_array* string_1004;
i8_array* string_1005;
int64_t HashMap_int_int_get_2(HashMap_int_int* this, int64_t key) {
    int64_t hash = int_hashCode_1(key);
    int64_t p = hash & (this->keys->len - 1);
    while (1) {
        int64_t _t0 = this->hashes->data[idx_2(p, this->hashes->len)] == hash;
        if (_t0) {
            int64_t _t1 = int_equals_2(key, this->keys->data[idx_2(p, this->keys->len)]);
            _t0 = _t1;
        }
        if (this->hashes->data[idx_2(p, this->hashes->len)] == 0) {
            int64_t _r0 = this->values->data[idx_2(p, this->values->len)];
            return _r0;
        } else if (_t0) {
            int64_t _r1 = this->values->data[idx_2(p, this->values->len)];
            return _r1;
        }
        p = (p + 1) & (this->keys->len - 1);
    }
}
void HashMap_int_int_put_3(HashMap_int_int* this, int64_t key, int64_t value) {
    if (( this->size * 2 ) >= this->keys->len) {
        int_array* kn = int_array_new(this->keys->len * 2);
        int_array* vn = int_array_new(this->keys->len * 2);
        int_array* hn = int_array_new(this->keys->len * 2);
        int_array* ok = this->keys;
        _incUse(ok, 0);
        int_array* oh = this->hashes;
        _incUse(oh, 0);
        int_array* ov = this->values;
        _incUse(ov, 0);
        _decUse(this->keys, int_array, 1);
        this->keys = kn;
        _incUse(this->keys, 1);
        _decUse(this->values, int_array, 1);
        this->values = vn;
        _incUse(this->values, 1);
        _decUse(this->hashes, int_array, 1);
        this->hashes = hn;
        _incUse(this->hashes, 1);
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                if (oh->data[idx_2(i, oh->len)] != 0) {
                    HashMap_int_int_put_3(this, ok->data[i], ov->data[idx_2(i, ov->len)]);
                }
                continue1:;
                int64_t _next = i + 1;
                if (_next >= ok->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
        _decUse(kn, int_array, 0);
        _decUse(vn, int_array, 0);
        _decUse(hn, int_array, 0);
        _decUse(ok, int_array, 0);
        _decUse(oh, int_array, 0);
        _decUse(ov, int_array, 0);
    }
    int64_t hash = int_hashCode_1(key);
    int64_t p = hash & (this->keys->len - 1);
    while (1) {
        int64_t _t0 = this->hashes->data[idx_2(p, this->hashes->len)] == hash;
        if (_t0) {
            int64_t _t1 = int_equals_2(key, this->keys->data[idx_2(p, this->keys->len)]);
            _t0 = _t1;
        }
        if (_t0) {
            this->values->data[idx_2(p, this->values->len)] = value;
            return;
        } else if (this->hashes->data[idx_2(p, this->hashes->len)] == 0) {
            this->size += 1;
            this->keys->data[idx_2(p, this->keys->len)] = key;
            this->hashes->data[idx_2(p, this->hashes->len)] = hash;
            this->values->data[idx_2(p, this->values->len)] = value;
            return;
        }
        p = (p + 1) & (this->keys->len - 1);
    }
}
int64_t HashMap_int_int_remove_2(HashMap_int_int* this, int64_t key) {
    int64_t hash = int_hashCode_1(key);
    int64_t p = hash & (this->keys->len - 1);
    while (1) {
        int64_t _t0 = this->hashes->data[idx_2(p, this->hashes->len)] == hash;
        if (_t0) {
            int64_t _t1 = int_equals_2(key, this->keys->data[idx_2(p, this->keys->len)]);
            _t0 = _t1;
        }
        if (this->hashes->data[idx_2(p, this->hashes->len)] == 0) {
            return 0;
        } else if (_t0) {
            this->keys->data[idx_2(p, this->keys->len)] = 0;
            this->hashes->data[idx_2(p, this->hashes->len)] = 0;
            this->values->data[idx_2(p, this->values->len)] = 0;
            this->size -= 1;
            break;
        }
        p = (p + 1) & (this->keys->len - 1);
    }
    int64_t p2 = (p + 1) & (this->keys->len - 1);
    while (1) {
        int64_t h2 = this->hashes->data[idx_2(p2, this->hashes->len)];
        if (h2 == 0) {
            break;
        }
        int64_t pb = h2 & (this->keys->len - 1);
        int64_t canMove = 0;
        if (pb <= p2) {
            canMove = ((pb <= p)) && ((p2 >= p));
        } else {
            canMove = (pb <= p) || (p2 > p);
        }
        if (canMove) {
            this->keys->data[idx_2(p, this->keys->len)] = this->keys->data[idx_2(p2, this->keys->len)];
            this->hashes->data[idx_2(p, this->hashes->len)] = this->hashes->data[idx_2(p2, this->hashes->len)];
            this->values->data[idx_2(p, this->values->len)] = this->values->data[idx_2(p2, this->values->len)];
            this->keys->data[idx_2(p2, this->keys->len)] = 0;
            this->hashes->data[idx_2(p2, this->hashes->len)] = 0;
            this->values->data[idx_2(p2, this->values->len)] = 0;
            p = p2;
        }
        p2 = (p2 + 1) & (this->keys->len - 1);
    }
    return 1;
}
str HashMap_str_str_get_2(HashMap_str_str* this, str key) {
    int64_t hash = str_hashCode_1(key);
    int64_t p = hash & (this->keys->len - 1);
    while (1) {
        int64_t _t0 = this->hashes->data[idx_2(p, this->hashes->len)] == hash;
        if (_t0) {
            int64_t _t1 = str_equals_2(key, this->keys->data[idx_2(p, this->keys->len)]);
            _t0 = _t1;
        }
        if (this->hashes->data[idx_2(p, this->hashes->len)] == 0) {
            str _r0 = this->values->data[idx_2(p, this->values->len)];
            return _r0;
        } else if (_t0) {
            str _r1 = this->values->data[idx_2(p, this->values->len)];
            return _r1;
        }
        p = (p + 1) & (this->keys->len - 1);
    }
}
void HashMap_str_str_put_3(HashMap_str_str* this, str key, str value) {
    if (( this->size * 2 ) >= this->keys->len) {
        str_array* kn = str_array_new(this->keys->len * 2);
        str_array* vn = str_array_new(this->keys->len * 2);
        int_array* hn = int_array_new(this->keys->len * 2);
        str_array* ok = this->keys;
        _incUse(ok, 0);
        int_array* oh = this->hashes;
        _incUse(oh, 0);
        str_array* ov = this->values;
        _incUse(ov, 0);
        _decUse(this->keys, str_array, 1);
        this->keys = kn;
        _incUse(this->keys, 1);
        _decUse(this->values, str_array, 1);
        this->values = vn;
        _incUse(this->values, 1);
        _decUse(this->hashes, int_array, 1);
        this->hashes = hn;
        _incUse(this->hashes, 1);
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                if (oh->data[idx_2(i, oh->len)] != 0) {
                    HashMap_str_str_put_3(this, ok->data[i], ov->data[idx_2(i, ov->len)]);
                }
                continue1:;
                int64_t _next = i + 1;
                if (_next >= ok->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
        _decUse(kn, str_array, 0);
        _decUse(vn, str_array, 0);
        _decUse(hn, int_array, 0);
        _decUse(ok, str_array, 0);
        _decUse(oh, int_array, 0);
        _decUse(ov, str_array, 0);
    }
    int64_t hash = str_hashCode_1(key);
    int64_t p = hash & (this->keys->len - 1);
    while (1) {
        int64_t _t0 = this->hashes->data[idx_2(p, this->hashes->len)] == hash;
        if (_t0) {
            int64_t _t1 = str_equals_2(key, this->keys->data[idx_2(p, this->keys->len)]);
            _t0 = _t1;
        }
        if (_t0) {
            this->values->data[idx_2(p, this->values->len)] = value;
            return;
        } else if (this->hashes->data[idx_2(p, this->hashes->len)] == 0) {
            this->size += 1;
            this->keys->data[idx_2(p, this->keys->len)] = key;
            this->hashes->data[idx_2(p, this->hashes->len)] = hash;
            this->values->data[idx_2(p, this->values->len)] = value;
            return;
        }
        p = (p + 1) & (this->keys->len - 1);
    }
}
int64_t hashCode_1(i8_array* data) {
    if (data->len <= 0) {
        return 0;
    }
    int64_t h = shiftLeft_2(data->len, 32);
    int64_t i = 0;
    if (i < ( data->len - 3 )) {
        while (1) {
            int64_t x = (data->data[i] & 255) | (shiftLeft_2((data->data[i + 1] & 255), 8)) | (shiftLeft_2((data->data[i + 2] & 255), 16)) | (shiftLeft_2((data->data[i + 3] & 255), 24));
            h = (h + x) * -7046029288634856825;
            int64_t n = i + 4;
            if (n >= ( data->len - 3 )) {
                break;
            }
            i = n;
        }
    }
    int64_t _t0 = i < data->len;
    if (_t0) {
        int64_t _t1 = i != ( data->len - 4 );
        _t0 = _t1;
    }
    if (_t0) {
        int64_t x = 0;
        int64_t s = 0;
        while (1) {
            x ^= shiftLeft_2((data->data[i] & 255), s);
            s += 8;
            int64_t n = i + 1;
            if (n >= data->len) {
                break;
            }
            i = n;
        }
        h = (h + x) * -7046029288634856825;
    }
    int64_t _r0 = h ^ (shiftRight_int_2(h, 32));
    return _r0;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
int64_t int_equals_2(int64_t this, int64_t other) {
    int64_t _r0 = this == other;
    return _r0;
}
int64_t int_hashCode_1(int64_t this) {
    int64_t _t0 = mix_1(this);
    return _t0;
}
int64_t mix_1(int64_t z) {
    z = (z ^ (shiftRight_int_2(z, 30))) * -4658895280553007687;
    z = (z ^ (shiftRight_int_2(z, 27))) * -7723592293110705685;
    int64_t _r0 = z ^ (shiftRight_int_2(z, 31));
    return _r0;
}
HashMap_int_int* newHashMap_int_int_2(int64_t _K, int64_t _V) {
    HashMap_int_int* result = HashMap_int_int_new();
    result->size = 0;
    _decUse(result->keys, int_array, 1);
    result->keys = int_array_new(4);
    _decUse(result->values, int_array, 1);
    result->values = int_array_new(4);
    _decUse(result->hashes, int_array, 1);
    result->hashes = int_array_new(4);
    return result;
}
HashMap_str_str* newHashMap_str_str_2(int64_t _K, int64_t _V) {
    HashMap_str_str* result = HashMap_str_str_new();
    result->size = 0;
    _decUse(result->keys, str_array, 1);
    result->keys = str_array_new(4);
    _decUse(result->values, str_array, 1);
    result->values = str_array_new(4);
    _decUse(result->hashes, int_array, 1);
    result->hashes = int_array_new(4);
    return result;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
str str_1(i8_array* x) {
    str result = str_new();
    _decUse(result.value, i8_array, 1);
    result.value = x;
    _incUse(result.value, 1);
    return result;
}
int64_t str_equals_2(str this, str other) {
    int64_t _r0 = this.value->len == other.value->len;
    return _r0;
}
int64_t str_hashCode_1(str this) {
    int64_t _t0 = hashCode_1(this.value);
    return _t0;
}
void test_0() {
    int64_t _t0 = hashCode_1(string_1001);
    printf("hash of hello: %lld\n", (long long)_t0);
    HashMap_int_int* map = newHashMap_int_int_2(0, 0);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            HashMap_int_int_put_3(map, i, 10 * i);
            if (i > 10) {
                HashMap_int_int_remove_2(map, i - 10);
            }
            int64_t _t1 = HashMap_int_int_get_2(map, i);
            printf("int map[%lld]=%lld\n", i, (long long)_t1);
            continue1:;
            int64_t _next = i + 1;
            if (_next >= 20) {
                break;
            }
            i = _next;
        }
        break;
    }
    HashMap_str_str* map2 = newHashMap_str_str_2(0, 0);
    str a = str_1(string_1004);
    str b = str_1(string_1001);
    HashMap_str_str_put_3(map2, a, b);
    str c = HashMap_str_str_get_2(map2, a);
    printf("str map[%.*s]=%.*s\n", a.value->len, a.value->data, c.value->len, c.value->data);
    _decUse(map, HashMap_int_int, 0);
    _decUse(map2, HashMap_str_str, 0);
    str_free(&a);
    str_free(&b);
    str_free(&c);
}
int main() {
    string_1000 = str_const("hash of hello: ", 15);
    string_1001 = str_const("hello", 5);
    string_1002 = str_const("int map[", 8);
    string_1003 = str_const("]=", 2);
    string_1004 = str_const("h", 1);
    string_1005 = str_const("str map[", 8);
    test_0();
    _end();
    return 0;
}
/*

type HashMap(K, V)
 module org.bau.HashMap

fun hashCode(data i8[]) int
4 bytes at a time if possible

*/
