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
typedef struct File File;
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
void File_free(File* x) {
    _free(x);
}
void File_close_1(File* this);
void File_use_1(File* this);
File* openFile_1(int64_t fp);
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = 1;
    result->data = data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
i8_array* string_1002;
i8_array* string_1003;
void File_close_1(File* this) {
    printf("closing %lld\n", this->fp);
}
void File_use_1(File* this) {
    printf("use %lld\n", this->fp);
}
File* openFile_1(int64_t fp) {
    File* f = File_new();
    f->fp = fp;
    printf("opening %lld\n", f->fp);
    return f;
    _decUse(f, File);
}
int main() {
    string_1000 = str_const("opening ", 8);
    string_1001 = str_const("closing ", 8);
    string_1002 = str_const("use ", 4);
    string_1003 = str_const("opened ", 7);
    int64_t i = 0;
    while (i < 10) {
        File* _t0 = openFile_1(i);
        File* f = _t0;
        printf("opened %lld\n", i);
        if (i == 5) {
            File_close_1(_t0);
            File_close_1(f);
            _decUse(f, File);
            break;
        }
        File_use_1(f);
        i += 1;
        continue0:;
        File_close_1(_t0);
        File_close_1(f);
        _decUse(f, File);
    }
    _end();
    return 0;
}
