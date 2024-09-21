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
void fill_1(int_array* data);
void test_0();
void fill_1(int_array* data) {
    if (data->len <= 0) {
        return;
    }
    int64_t i = 0;
    while (1) {
        data->data[i] = i;
        int64_t next = i + 1;
        if (next >= data->len) {
            break;
        }
        i = next;
    }
}
void test_0() {
    int_array* data = int_array_new(16);
    fill_1(data);
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            printf("%lld\n", data->data[i]);
            continue2:;
            int64_t _next = i + 1;
            if (_next >= 16) {
                break;
            }
            i = _next;
        }
        break;
    }
    _decUse(data, int_array);
}
int main() {
    test_0();
    _end();
    return 0;
}
