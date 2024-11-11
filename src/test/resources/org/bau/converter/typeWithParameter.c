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
    _decUse(x->array, int_array, 0);
    _free(x);
}
void List_int_add_2(List_int* this, int64_t x) {
    if (this->size >= this->array->len) {
        int_array* n = int_array_new(this->array->len * 2);
        _decUse(this->array, int_array, 1);
        this->array = n;
        _incUse(this->array, 1);
        _decUse(n, int_array, 0);
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
    List_int* intList = List_int_new();
    _decUse(intList->array, int_array, 1);
    intList->array = int_array_new(4);
    List_int_add_2(intList, 10);
    List_int_add_2(intList, 20);
    printf("%lld\n", (long long)intList->array->data[idx_2(0, intList->array->len)]);
    printf("%lld\n", (long long)intList->array->data[idx_2(1, intList->array->len)]);
    _decUse(intList, List_int, 0);
}
int main() {
    test_0();
    _end();
    return 0;
}
