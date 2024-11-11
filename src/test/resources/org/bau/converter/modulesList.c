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
typedef struct int_array int_array;
struct int_array;
typedef struct org_bau_List_List org_bau_List_List;
struct org_bau_List_List;
typedef struct org_bau_List_List_int org_bau_List_List_int;
struct org_bau_List_List_int;
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
struct org_bau_List_List {
    int32_t _refCount;
};
org_bau_List_List* org_bau_List_List_new() {
    org_bau_List_List* result = _malloc(sizeof(org_bau_List_List));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_List_List_int {
    int_array* array;
    int64_t size;
    int32_t _refCount;
};
org_bau_List_List_int* org_bau_List_List_int_new() {
    org_bau_List_List_int* result = _malloc(sizeof(org_bau_List_List_int));
    _traceMalloc(result);
    result->_refCount = 1;
    result->array = 0;
    result->size = 0;
    return result;
}
/* exception types */
/* functions */
int64_t idx_2(int64_t x, int64_t len);
org_bau_List_List_int* org_bau_List_newList_int_1(int64_t _T);
void org_bau_List_List_int_add_2(org_bau_List_List_int* this, int64_t x);
void int_array_free(int_array* x);
void org_bau_List_List_free(org_bau_List_List* x);
void org_bau_List_List_int_free(org_bau_List_List_int* x);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void org_bau_List_List_free(org_bau_List_List* x) {
    _free(x);
}
void org_bau_List_List_int_free(org_bau_List_List_int* x) {
    _decUse(x->array, int_array, 0);
    _free(x);
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
org_bau_List_List_int* org_bau_List_newList_int_1(int64_t _T) {
    org_bau_List_List_int* result = org_bau_List_List_int_new();
    _decUse(result->array, int_array, 1);
    result->array = int_array_new(4);
    result->size = 0;
    return result;
}
void org_bau_List_List_int_add_2(org_bau_List_List_int* this, int64_t x) {
    if (this->size >= this->array->len) {
        int_array* n = int_array_new(this->array->len * 2);
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
        _decUse(this->array, int_array, 1);
        this->array = n;
        _incUse(this->array, 1);
        _decUse(n, int_array, 0);
    }
    this->array->data[idx_2(this->size, this->array->len)] = x;
    this->size += 1;
}
int main() {
    org_bau_List_List_int* list = org_bau_List_newList_int_1(0);
    org_bau_List_List_int_add_2(list, 100);
    org_bau_List_List_int_add_2(list, 80);
    printf("%lld\n", (long long)list->size);
    printf("%lld\n", (long long)list->array->data[idx_2(0, list->array->len)]);
    _decUse(list, org_bau_List_List_int, 0);
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
