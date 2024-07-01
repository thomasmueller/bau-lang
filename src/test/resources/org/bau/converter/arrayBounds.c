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
int64_t idx_2(int64_t x, int64_t len);
void test_0();
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
void test_0() {
    i8_array* data = i8_array_new(10);
    while (1 == 1) {
        int64_t i = 0;
        while (1 == 1) {
            data->data[i] = i;
            continue1:;
            int64_t next = i + 1;
            if (next >= data->len) {
                break;
            }
            i = next;
        }
        break;
    }
    int64_t sum = 0;
    while (1 == 1) {
        int64_t i = 0;
        while (i < 20) {
            sum += data->data[idx_2(i, data->len)];
            continue3:;
            i += 1;
        }
        break;
    }
    printf("%lld\n", sum);
    _decUse(data, i8_array);
}
int main() {
    test_0();
    _end();
    return 0;
}
