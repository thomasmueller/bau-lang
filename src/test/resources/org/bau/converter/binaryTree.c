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
typedef struct Tree Tree;
struct Tree;
struct i8_array {
    int32_t len;
    int8_t* data;
    int32_t _refCount;
};
i8_array* i8_array_new(uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(int8_t) * len);
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
struct Tree {
    Tree* left;
    Tree* right;
    int32_t _refCount;
};
Tree* Tree_new() {
    Tree* result = _malloc(sizeof(Tree));
    _traceMalloc(result);
    result->_refCount = 1;
    result->left = 0;
    result->right = 0;
    return result;
}
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
Tree* Tree_0();
int64_t Tree_nodeCount_1(Tree* this);
int64_t count_1(int64_t depth);
Tree* newTree_2(Tree* left, Tree* right);
int64_t shiftLeft_2(int64_t a, int64_t b);
void stretch_1(int64_t depth);
Tree* with_1(int64_t depth);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void Tree_free(Tree* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void Tree_free(Tree* x) {
    _decUse(x->left, Tree);
    _decUse(x->right, Tree);
    _free(x);
}
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = INT32_MAX;
    result->data = (int8_t*) data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1003;
Tree* Tree_0() {
    Tree* _t0 = Tree_new();
    _decUse(_t0->left, Tree);
    _t0->left = NULL;
    _decUse(_t0->right, Tree);
    _t0->right = NULL;
    return _t0;
}
int64_t Tree_nodeCount_1(Tree* this) {
    int64_t result = 1;
    Tree* l = this->left;
    _incUseStack(l);
    if (l != NULL) {
        result += Tree_nodeCount_1(l);
    }
    Tree* r = this->right;
    _incUseStack(r);
    if (r != NULL) {
        result += Tree_nodeCount_1(r);
    }
    _decUseStack(r, Tree);
    _decUseStack(l, Tree);
    return result;
}
int64_t count_1(int64_t depth) {
    Tree* t = with_1(depth);
    int64_t c = Tree_nodeCount_1(t);
    _decUseStack(t, Tree);
    return c;
}
Tree* newTree_2(Tree* left, Tree* right) {
    Tree* t = Tree_0();
    _decUse(t->left, Tree);
    t->left = left;
    _incUse(t->left);
    _decUse(t->right, Tree);
    t->right = right;
    _incUse(t->right);
    return t;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
void stretch_1(int64_t depth) {
    int64_t c = count_1(depth);
    printf("stretch tree of depth %lld; check: %lld\n", (long long)depth, (long long)c);
}
Tree* with_1(int64_t depth) {
    if (depth == 0) {
        Tree* _t0 = newTree_2(NULL, NULL);
        return _t0;
    }
    Tree* _t1 = with_1(depth - 1);
    Tree* _t2 = with_1(depth - 1);
    Tree* _t3 = newTree_2(_t1, _t2);
    _decUseStack(_t2, Tree);
    _decUseStack(_t1, Tree);
    return _t3;
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("stretch tree of depth ", 22);
    string_1001 = str_const("; check: ", 9);
    string_1002 = str_const(" trees of depth ", 16);
    string_1003 = str_const("long lived tree of depth ", 25);
    int64_t n = 10;
    int64_t minDepth = 4;
    int64_t maxDepth = 10;
    int64_t stretchDepth = 11;
    stretch_1(11);
    Tree* longLivedTree = with_1(10);
    int64_t depth = 4;
    while (depth <= 10) {
        int64_t iterations = shiftLeft_2(1, (( 10 - depth ) + 4));
        int64_t sum = 0;
        int64_t i = 1;
        while (i <= iterations) {
            sum += count_1(depth);
            i += 1;
        }
        printf("%lld trees of depth %lld; check: %lld\n", (long long)iterations, (long long)depth, (long long)sum);
        depth += 2;
    }
    int64_t count = Tree_nodeCount_1(longLivedTree);
    printf("long lived tree of depth %lld; check: %lld\n", (long long)10, (long long)count);
    _decUseStack(longLivedTree, Tree);
    _end();
    return 0;
}
