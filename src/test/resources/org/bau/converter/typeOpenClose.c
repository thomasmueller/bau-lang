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
typedef struct i8_array i8_array;
struct i8_array;
typedef struct int_array int_array;
struct int_array;
typedef struct File File;
struct File;
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
struct File {
    int64_t fp;
    int32_t _refCount;
};
File* File_new() {
    File* result = _malloc(sizeof(File));
    _traceMalloc(result);
    result->_refCount = 1;
    result->fp = 0;
    return result;
}
/* exception types */
/* functions */
void File_close_1(File* this);
void File_use_1(File* this);
File* openFile_1(int64_t fp);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void File_free(File* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void File_free(File* x) {
    File_close_1(x);
    if (x->_refCount) { fprintf(stdout, "Object re-referenced in the close method"); exit(1); }
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
void File_close_1(File* this) {
    printf("closing %lld\n", (long long)this->fp);
}
void File_use_1(File* this) {
    printf("use %lld\n", (long long)this->fp);
}
File* openFile_1(int64_t fp) {
    File* f = File_new();
    f->fp = fp;
    printf("opening %lld\n", (long long)f->fp);
    return f;
}
int main() {
    string_1000 = str_const("closing ", 8);
    string_1001 = str_const("use ", 4);
    string_1002 = str_const("opening ", 8);
    string_1003 = str_const("opened ", 7);
    int64_t i = 0;
    while (i < 10) {
        File* f = openFile_1(i);
        printf("opened %lld\n", (long long)i);
        if (i == 5) {
            _decUse(f, File, 0);
            break;
        }
        File_use_1(f);
        i += 1;
        continue0:;
        _decUse(f, File, 0);
    }
    _end();
    return 0;
}
