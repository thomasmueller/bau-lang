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
typedef struct org_bau_File_File org_bau_File_File;
struct org_bau_File_File {
    int64_t filePointer;
    int32_t _refCount;
};
org_bau_File_File* org_bau_File_File_new() {
    org_bau_File_File* result = _malloc(sizeof(org_bau_File_File));
    _traceMalloc(result);
    result->_refCount = 1;
    result->filePointer = 0;
    return result;
}
void org_bau_File_File_free(org_bau_File_File* x) {
    _free(x);
}
void File_org_bau_File_close_1(org_bau_File_File* this);
int64_t File_org_bau_File_read_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len);
org_bau_File_File* org_bau_File_openFile_2(i8_array* name, i8_array* mode);
void test_0();
i8_array* str_const(char* data, uint32_t len) {
    i8_array* result = _malloc(sizeof(i8_array));
    result->len = len;
    result->_refCount = 1;
    result->data = data;
    return result;
}
i8_array* string_1000;
i8_array* string_1001;
void File_org_bau_File_close_1(org_bau_File_File* this) {
    FILE* fp = (FILE*) (this->filePointer);
    fclose(fp);
}
int64_t File_org_bau_File_read_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len) {
    FILE* fp = (FILE*) (this->filePointer);
    if (pos < 0 || len < 0 || pos + len > data->len) {
      return 0;
    } else {
      return fread(data->data + pos, 1, len, fp);
    }
}
org_bau_File_File* org_bau_File_openFile_2(i8_array* name, i8_array* mode) {
    // TODO verify strings
    FILE* fp = fopen(name->data, mode->data);
    org_bau_File_File* f = org_bau_File_File_new();
    f->filePointer = (uint64_t) fp;
    return f;
}
void test_0() {
    org_bau_File_File* _t0 = org_bau_File_openFile_2(string_1000, string_1001);
    org_bau_File_File* file = _t0;
    i8_array* data = i8_array_new(16);
    File_org_bau_File_read_4(file, data, 0, 15);
    data->data[5] = 0;
    printf("%.*s\n", data->len, data->data);
    File_org_bau_File_close_1(_t0);
    File_org_bau_File_close_1(file);
    _decUse(file, org_bau_File_File);
    _decUse(data, i8_array);
}
int main() {
    string_1000 = str_const("hello.txt", 9);
    string_1001 = str_const("r", 1);
    test_0();
    _end();
    return 0;
}
