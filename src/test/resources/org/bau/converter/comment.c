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
/* types */
typedef struct int_array int_array;
struct int_array;
typedef struct List_int List_int;
struct List_int;
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
struct List_int {
    int_array* array;
    int64_t size;
    int32_t _refCount;
};
List_int* List_int_new() {
    List_int* result = _malloc(sizeof(List_int));
    _traceMalloc(result);
    result->_refCount = 1;
    result->array = 0;
    result->size = 0;
    return result;
}
/* exception types */
/* functions */
void List_int_add_2(List_int* this, int64_t x);
int64_t idx_2(int64_t x, int64_t len);
void test_0();
void int_array_free(int_array* x);
void List_int_free(List_int* x);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void List_int_free(List_int* x) {
    _decUse(x->array, int_array);
    _free(x);
}
void List_int_add_2(List_int* this, int64_t x) {
    if (this->size >= this->array->len) {
        int_array* n = int_array_new(this->array->len * 2);
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                n->data[idx_2(i, n->len)] = this->array->data[idx_2(i, this->array->len)];
                continue1:;
                int64_t _next = i + 1;
                if (_next >= this->size) {
                    break;
                }
                i = _next;
            }
            break;
        }
        _decUse(this->array, int_array);
        this->array = n;
        _incUse(this->array);
        _decUseStack(n, int_array);
    }
    this->array->data[idx_2(this->size, this->array->len)] = x;
    this->size += 1;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
void test_0() {
    List_int* list = List_int_new();
    _decUse(list->array, int_array);
    list->array = int_array_new(1);
    while (1 == 1) {
        int64_t i = 0;
        while (i < 10) {
            List_int_add_2(list, i);
            continue1:;
            i += 1;
        }
        break;
    }
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            printf("%lld\n", (long long)list->array->data[idx_2(i, list->array->len)]);
            continue3:;
            int64_t _next = i + 1;
            if (_next >= list->size) {
                break;
            }
            i = _next;
        }
        break;
    }
    _decUseStack(list, List_int);
}
int main() {
    test_0();
    _end();
    return 0;
}
/*

type List(T)
A list of entries

fun test()
Test function

*/
