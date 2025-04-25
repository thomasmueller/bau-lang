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
typedef struct org_bau_String_string org_bau_String_string;
struct org_bau_String_string;
typedef struct org_bau_String_string_array org_bau_String_string_array;
struct org_bau_String_string_array;
typedef struct org_bau_String_StringBuilder org_bau_String_StringBuilder;
struct org_bau_String_StringBuilder;
typedef struct org_bau_List_List_org_bau_String_string org_bau_List_List_org_bau_String_string;
struct org_bau_List_List_org_bau_String_string;
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
struct org_bau_String_string {
    i8_array* data;
};
org_bau_String_string org_bau_String_string_new() {
    org_bau_String_string result;
    result.data = 0;
    return result;
}
struct org_bau_String_string_array {
    int32_t len;
    org_bau_String_string* data;
    int32_t _refCount;
};
org_bau_String_string_array* org_bau_String_string_array_new(uint32_t len) {
    org_bau_String_string_array* result = _malloc(sizeof(org_bau_String_string_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(org_bau_String_string) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_String_StringBuilder {
    i8_array* data;
    int64_t len;
    int32_t _refCount;
};
org_bau_String_StringBuilder* org_bau_String_StringBuilder_new() {
    org_bau_String_StringBuilder* result = _malloc(sizeof(org_bau_String_StringBuilder));
    _traceMalloc(result);
    result->_refCount = 1;
    result->data = 0;
    result->len = 0;
    return result;
}
struct org_bau_List_List_org_bau_String_string {
    org_bau_String_string_array* array;
    int64_t size;
    int32_t _refCount;
};
org_bau_List_List_org_bau_String_string* org_bau_List_List_org_bau_String_string_new() {
    org_bau_List_List_org_bau_String_string* result = _malloc(sizeof(org_bau_List_List_org_bau_String_string));
    _traceMalloc(result);
    result->_refCount = 1;
    result->array = 0;
    result->size = 0;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
int64_t idx_2(int64_t x, int64_t len);
org_bau_List_List_org_bau_String_string* org_bau_List_newList_org_bau_String_string_1(int64_t _T);
void org_bau_List_List_org_bau_String_string_add_2(org_bau_List_List_org_bau_String_string* this, org_bau_String_string x);
org_bau_String_string org_bau_List_List_org_bau_String_string_get_2(org_bau_List_List_org_bau_String_string* this, int64_t x);
int64_t org_bau_String_indexOf_2(i8_array* s, i8_array* find);
int64_t org_bau_String_indexOf_3(i8_array* s, i8_array* find, int64_t start);
i8_array* org_bau_String_replaceAll_3(i8_array* s, i8_array* before, i8_array* after);
org_bau_List_List_org_bau_String_string* org_bau_String_split_2(i8_array* s, i8_array* delimiter);
org_bau_String_string org_bau_String_str_1(i8_array* s);
i8_array* org_bau_String_substring_2(i8_array* s, int64_t start);
i8_array* org_bau_String_substring_3(i8_array* s, int64_t start, int64_t end);
void org_bau_String_StringBuilder_append_4(org_bau_String_StringBuilder* this, i8_array* b, int64_t start, int64_t end);
void test_0();
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_String_string_free(org_bau_String_string* x);
void org_bau_String_string_array_free(org_bau_String_string_array* x);
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x);
void org_bau_List_List_org_bau_String_string_free(org_bau_List_List_org_bau_String_string* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void org_bau_String_string_free(org_bau_String_string* x) {
    _decUse(x->data, i8_array);
}
void org_bau_String_string_array_free(org_bau_String_string_array* x) {
    for (int i = 0; i < x->len; i++) org_bau_String_string_free(&(x->data[i]));
    _free(x->data);
    _free(x);
}
void org_bau_String_StringBuilder_free(org_bau_String_StringBuilder* x) {
    _decUse(x->data, i8_array);
    _free(x);
}
void org_bau_List_List_org_bau_String_string_free(org_bau_List_List_org_bau_String_string* x) {
    _decUse(x->array, org_bau_String_string_array);
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
i8_array* string_1006;
i8_array* string_1007;
i8_array* string_1008;
i8_array* string_1009;
i8_array* string_1010;
i8_array* string_1011;
i8_array* string_1012;
i8_array* string_1013;
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
org_bau_List_List_org_bau_String_string* org_bau_List_newList_org_bau_String_string_1(int64_t _T) {
    org_bau_List_List_org_bau_String_string* result = org_bau_List_List_org_bau_String_string_new();
    _decUse(result->array, org_bau_String_string_array);
    result->array = org_bau_String_string_array_new(4);
    result->size = 0;
    return result;
}
void org_bau_List_List_org_bau_String_string_add_2(org_bau_List_List_org_bau_String_string* this, org_bau_String_string x) {
    if (this->size >= this->array->len) {
        org_bau_String_string_array* n = org_bau_String_string_array_new(this->array->len * 2);
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                n->data[idx_2(i, n->len)] = this->array->data[i];
                continue1:;
                int64_t _next = i + 1;
                if (_next >= this->array->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
        _decUse(this->array, org_bau_String_string_array);
        this->array = n;
        _incUse(this->array);
        _decUseStack(n, org_bau_String_string_array);
    }
    this->array->data[idx_2(this->size, this->array->len)] = x;
    this->size += 1;
}
org_bau_String_string org_bau_List_List_org_bau_String_string_get_2(org_bau_List_List_org_bau_String_string* this, int64_t x) {
    org_bau_String_string _r0 = this->array->data[idx_2(x, this->array->len)];
    return _r0;
}
int64_t org_bau_String_indexOf_2(i8_array* s, i8_array* find) {
    int64_t _t0 = org_bau_String_indexOf_3(s, find, 0);
    return _t0;
}
int64_t org_bau_String_indexOf_3(i8_array* s, i8_array* find, int64_t start) {
    if (find->len <= 0) {
        return -1;
    }
    if (s->len <= 0) {
        return -1;
    }
    int64_t i = 0;
    if (start < 0) {
        return -1;
    }
    if (start >= s->len) {
        return -1;
    }
    i = start;
    while (1) {
        int64_t j = 0;
        int64_t x = i;
        while (1) {
            if (s->data[x] != find->data[j]) {
                break;
            }
            int64_t nj = j + 1;
            if (nj >= find->len) {
                return i;
            }
            j = nj;
            int64_t nx = x + 1;
            if (nx >= s->len) {
                break;
            }
            x = nx;
        }
        int64_t ni = i + 1;
        if (ni >= s->len) {
            break;
        }
        i = ni;
    }
    return -1;
}
i8_array* org_bau_String_replaceAll_3(i8_array* s, i8_array* before, i8_array* after) {
    int64_t next = org_bau_String_indexOf_2(s, before);
    int64_t _t0 = before->len <= 0;
    if (!(_t0)) {
        int64_t _t1 = next < 0;
        _t0 = _t1;
    }
    if (_t0) {
        return s;
    }
    org_bau_String_StringBuilder* buff = org_bau_String_StringBuilder_new();
    _decUse(buff->data, i8_array);
    buff->data = i8_array_new(s->len);
    int64_t index = 0;
    while (1) {
        org_bau_String_StringBuilder_append_4(buff, s, index, next);
        org_bau_String_StringBuilder_append_4(buff, after, 0, after->len);
        index = next + before->len;
        next = org_bau_String_indexOf_3(s, before, index);
        if (next < 0) {
            break;
        }
    }
    org_bau_String_StringBuilder_append_4(buff, s, index, s->len);
    i8_array* _r0 = buff->data;
    _incUseStack(_r0);
    _decUseStack(buff, org_bau_String_StringBuilder);
    return _r0;
}
org_bau_List_List_org_bau_String_string* org_bau_String_split_2(i8_array* s, i8_array* delimiter) {
    org_bau_List_List_org_bau_String_string* list = org_bau_List_newList_org_bau_String_string_1(0);
    int64_t next = org_bau_String_indexOf_2(s, delimiter);
    int64_t _t0 = delimiter->len == 0;
    if (!(_t0)) {
        int64_t _t1 = next < 0;
        _t0 = _t1;
    }
    if (_t0) {
        org_bau_List_List_org_bau_String_string_add_2(list, org_bau_String_str_1(s));
        return list;
    }
    int64_t index = 0;
    while (1) {
        i8_array* p = org_bau_String_substring_3(s, index, next);
        org_bau_List_List_org_bau_String_string_add_2(list, org_bau_String_str_1(p));
        index = next + delimiter->len;
        next = org_bau_String_indexOf_3(s, delimiter, index);
        if (next < 0) {
            _decUseStack(p, i8_array);
            break;
        }
        continue0:;
        _decUseStack(p, i8_array);
    }
    i8_array* p = org_bau_String_substring_2(s, index);
    org_bau_List_List_org_bau_String_string_add_2(list, org_bau_String_str_1(p));
    _decUseStack(p, i8_array);
    return list;
}
org_bau_String_string org_bau_String_str_1(i8_array* s) {
    org_bau_String_string x = org_bau_String_string_new();
    _decUse(x.data, i8_array);
    x.data = s;
    _incUse(x.data);
    return x;
}
i8_array* org_bau_String_substring_2(i8_array* s, int64_t start) {
    i8_array* _t0 = org_bau_String_substring_3(s, start, s->len);
    return _t0;
}
i8_array* org_bau_String_substring_3(i8_array* s, int64_t start, int64_t end) {
    int64_t len = end - start;
    int64_t _t0 = len < 0;
    if (!(_t0)) {
        int64_t _t1 = start < 0;
        _t0 = _t1;
    }
    int64_t _t2 = _t0;
    if (!(_t2)) {
        int64_t _t3 = end > s->len;
        _t2 = _t3;
    }
    if (_t2) {
        return string_1000;
    }
    if (s->len <= 0) {
        return string_1000;
    }
    int64_t i = 0;
    if (start >= s->len) {
        return string_1000;
    }
    i = start;
    i8_array* result = i8_array_new(len);
    while (1 == 1) {
        int64_t j = 0;
        while (1) {
            result->data[idx_2(j, result->len)] = s->data[i];
            int64_t next = i + 1;
            if (next >= s->len) {
                break;
            }
            i = next;
            continue1:;
            int64_t _next = j + 1;
            if (_next >= len) {
                break;
            }
            j = _next;
        }
        break;
    }
    return result;
}
void org_bau_String_StringBuilder_append_4(org_bau_String_StringBuilder* this, i8_array* b, int64_t start, int64_t end) {
    int64_t add = end - start;
    int64_t _t0 = start >= b->len;
    if (!(_t0)) {
        int64_t _t1 = end < start;
        _t0 = _t1;
    }
    int64_t _t2 = _t0;
    if (!(_t2)) {
        int64_t _t3 = end > b->len;
        _t2 = _t3;
    }
    if (_t2) {
        return;
    }
    while (( this->len + add ) > this->data->len) {
        i8_array* n = i8_array_new(this->data->len * 2);
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                n->data[idx_2(i, n->len)] = this->data->data[i];
                continue2:;
                int64_t _next = i + 1;
                if (_next >= this->data->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
        _decUse(this->data, i8_array);
        this->data = n;
        _incUse(this->data);
        continue0:;
        _decUseStack(n, i8_array);
    }
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            this->data->data[idx_2(this->len + i, this->data->len)] = b->data[idx_2(start + i, b->len)];
            continue4:;
            int64_t _next = i + 1;
            if (_next >= add) {
                break;
            }
            i = _next;
        }
        break;
    }
    this->len += add;
}
void test_0() {
    i8_array* x = string_1001;
    _incUseStack(x);
    int64_t _t0 = org_bau_String_indexOf_2(x, string_1003);
    printf("indexOf ll: %lld\n", (long long)_t0);
    i8_array* _t1 = org_bau_String_replaceAll_3(string_1001, string_1005, string_1006);
    printf("l ->L : %.*s\n", _t1->len, _t1->data);
    i8_array* _t2 = org_bau_String_replaceAll_3(string_1001, string_1005, string_1008);
    printf("l ->LL: %.*s\n", _t2->len, _t2->data);
    i8_array* _t3 = org_bau_String_replaceAll_3(string_1001, string_1003, string_1006);
    printf("ll->L : %.*s\n", _t3->len, _t3->data);
    org_bau_List_List_org_bau_String_string* list = org_bau_String_split_2(string_1010, string_1011);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            org_bau_String_string s = org_bau_List_List_org_bau_String_string_get_2(list, i);
            printf("#%lld: %.*s\n", i, s.data->len, s.data->data);
            continue1:;
            int64_t _next = i + 1;
            if (_next >= list->size) {
                org_bau_String_string_free(&s);
                break;
            }
            i = _next;
            org_bau_String_string_free(&s);
        }
        break;
    }
    _decUseStack(list, org_bau_List_List_org_bau_String_string);
    _decUseStack(_t3, i8_array);
    _decUseStack(_t2, i8_array);
    _decUseStack(_t1, i8_array);
    _decUseStack(x, i8_array);
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("", 0);
    string_1001 = str_const("hello world", 11);
    string_1002 = str_const("indexOf ll: ", 12);
    string_1003 = str_const("ll", 2);
    string_1004 = str_const("l ->L : ", 8);
    string_1005 = str_const("l", 1);
    string_1006 = str_const("L", 1);
    string_1007 = str_const("l ->LL: ", 8);
    string_1008 = str_const("LL", 2);
    string_1009 = str_const("ll->L : ", 8);
    string_1010 = str_const("hello,world,!", 13);
    string_1011 = str_const(",", 1);
    string_1012 = str_const("#", 1);
    string_1013 = str_const(": ", 2);
    test_0();
    _end();
    return 0;
}
/*

type List(T)
List

fun List(T) add(x T)
Add an entry to the list

fun List(T) get(x int) T
Add an entry to the list

*/
