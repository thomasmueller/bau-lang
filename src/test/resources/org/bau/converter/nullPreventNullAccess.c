#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#define _incUse(a) if(a){(a)->_refCount++;}
#define _decUse(a, type) if(a){if(--((a)->_refCount) == 0) type##_free(a);}
#define _malloc(a) malloc(a)
#define _traceMalloc(a) ;
#define _free(a) free(a)
#define _end() ;
typedef struct Value Value;
struct Value {
    int64_t data;
    int32_t _refCount;
};
Value* Value_new() {
    Value* result = _malloc(sizeof(Value));
    _traceMalloc(result);
    result->_refCount = 1;
    result->data = 0;
    return result;
}
void Value_free(Value* x) {
    _free(x);
}
Value* get_1(int64_t key);
void test_0();
Value* get_1(int64_t key) {
    if (key <= 0) {
        return NULL;
    }
    Value* result = Value_new();
    result->data = key * 10;
    return result;
    _decUse(result, Value);
}
void test_0() {
    Value* _t0 = get_1(0);
    Value* a = _t0;
    if (a != NULL) {
        printf("%lld\n", a->data);
    }
    _decUse(a, Value);
}
int main() {
    test_0();
    _end();
    return 0;
}
