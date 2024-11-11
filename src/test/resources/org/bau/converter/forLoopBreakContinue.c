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
/* exception types */
/* functions */
void int_array_free(int_array* x);
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
int main() {
    while (1 == 1) {
        int64_t a = 0;
        while (a < 10) {
            if (a == 5) {
                break;
            }
            if (a == 2) {
                goto continue1;
            }
            printf("%lld\n", (long long)a);
            continue1:;
            a += 1;
        }
        break;
    }
    _end();
    return 0;
}
