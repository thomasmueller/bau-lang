#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <time.h>
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
typedef struct i8_array i8_array;
struct i8_array;
typedef struct int_array int_array;
struct int_array;
typedef struct org_bau_Utils_dateTime org_bau_Utils_dateTime;
struct org_bau_Utils_dateTime;
typedef struct Tree Tree;
struct Tree;
typedef struct Tree_owned Tree_owned;
struct Tree_owned;
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
struct org_bau_Utils_dateTime {
    int32_t year;
    int64_t month;
    int64_t day;
    int64_t hour;
    int64_t minute;
    int64_t second;
    int64_t millis;
};
org_bau_Utils_dateTime org_bau_Utils_dateTime_new() {
    org_bau_Utils_dateTime result;
    result.year = 0;
    result.month = 0;
    result.day = 0;
    result.hour = 0;
    result.minute = 0;
    result.second = 0;
    result.millis = 0;
    return result;
}
struct Tree {
    Tree_owned* left;
    Tree_owned* right;
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
struct Tree_owned {
    Tree_owned* left;
    Tree_owned* right;
};
Tree_owned* Tree_owned_new() {
    Tree_owned* result = _malloc(sizeof(Tree_owned));
    _traceMalloc(result);
    result->left = 0;
    result->right = 0;
    return result;
}
/* exception types */
/* functions */
int64_t Tree_owned_nodeCount_1(Tree_owned* this);
Tree_owned* newTree_2(Tree_owned* left, Tree_owned* right);
int64_t shiftLeft_2(int64_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
Tree_owned* with_1(int64_t depth);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_Utils_dateTime_free(org_bau_Utils_dateTime* x);
void Tree_free(Tree* x);
void Tree_owned_free(Tree_owned* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void org_bau_Utils_dateTime_free(org_bau_Utils_dateTime* x) {
}
void Tree_free(Tree* x) {
    if (x->left) Tree_owned_free(x->left);
    if (x->right) Tree_owned_free(x->right);
    _free(x);
}
void Tree_owned_free(Tree_owned* x) {
    if (x == NULL) return;
    if (x->left) Tree_owned_free(x->left);
    if (x->right) Tree_owned_free(x->right);
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
int64_t randomSeed;
int64_t Tree_owned_nodeCount_1(Tree_owned* this) {
    int64_t result = 1;
    Tree_owned* l = this->left;
    if (l) {
        result += Tree_owned_nodeCount_1(l);
    }
    Tree_owned* r = this->right;
    if (r) {
        result += Tree_owned_nodeCount_1(r);
    }
    return result;
}
Tree_owned* newTree_2(Tree_owned* left, Tree_owned* right) {
    Tree_owned* t = Tree_owned_new();
    Tree_owned_free(t->left);
    t->left = left;
    left = NULL;
    Tree_owned_free(t->right);
    t->right = right;
    right = NULL;
    return t;
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
Tree_owned* with_1(int64_t depth) {
    if (depth == 0) {
        Tree_owned* _t0 = newTree_2(NULL, NULL);
        return _t0;
    }
    Tree_owned* _t1 = with_1(depth - 1);
    Tree_owned* _t2 = with_1(depth - 1);
    Tree_owned* _t3 = newTree_2(_t1, _t2);
    _t1 = NULL;
    _t2 = NULL;
    Tree_owned_free(_t2);
    Tree_owned_free(_t1);
    return _t3;
}
int main() {
    string_1000 = str_const("ownership / borrowing", 21);
    string_1001 = str_const("stretch tree of depth ", 22);
    string_1002 = str_const(" check: ", 8);
    string_1003 = str_const(" trees of depth ", 16);
    string_1004 = str_const("long lived tree of depth ", 25);
    int64_t randomSeed = 0;
    int64_t minDepth = 1;
    int64_t maxDepth = 3;
    int64_t stretchDepth = 4;
    Tree_owned* stretch = with_1(4);
    printf("ownership / borrowing\n");
    int64_t _t0 = Tree_owned_nodeCount_1(stretch);
    printf("stretch tree of depth %lld check: %lld\n", (long long)4, (long long)_t0);
    stretch = with_1(0);
    Tree_owned* longLived = with_1(3);
    int64_t depth = 1;
    while (depth <= 3) {
        int64_t iterations = shiftLeft_2(1, (( 3 - depth ) + 1));
        int64_t check = 0;
        int64_t i = 1;
        while (i <= iterations) {
            Tree_owned* t = with_1(depth);
            check += Tree_owned_nodeCount_1(t);
            i += 1;
            continue1:;
            Tree_owned_free(t);
        }
        printf("%lld trees of depth %lld check: %lld\n", (long long)iterations, (long long)depth, (long long)check);
        depth += 2;
    }
    int64_t _t1 = Tree_owned_nodeCount_1(longLived);
    printf("long lived tree of depth %lld check: %lld\n", (long long)3, (long long)_t1);
    Tree_owned_free(longLived);
    Tree_owned_free(stretch);
    _end();
    return 0;
}
/*

type dateTime
Date and time.

fun getDateTime() dateTime
Get the local time in millisecond precision.

fun getNanoTime() int
Nanosecons since some undefined point in the past. Never jumps backwards.

fun getNanoTimeUTC() int
Nanoseconds since 1970 (epoch). May jump backwards when the system clock is adjusted.

fun getRandomSeed() int
Get the random seed.

fun random() int
Pseudo-random number generated using the Splitmix64 algorithm.

fun setRandomSeed(seed int)
Set the random seed.

*/
