#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
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
void printLen_1(i8_array* data);
void test_0();
void printLen_1(i8_array* data) {
    printf("%d\n", data->len);
}
void test_0() {
    i8_array* data = i8_array_new(10);
    printf("%d\n", data->len);
    printLen_1(data);
    _decUse(data, i8_array);
}
int main() {
    test_0();
    _end();
    return 0;
}
