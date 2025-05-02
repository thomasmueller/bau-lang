#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
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
typedef struct org_bau_compress_Lz4_XXHash org_bau_compress_Lz4_XXHash;
struct org_bau_compress_Lz4_XXHash;
typedef struct org_bau_compress_Lz4_LZ4Compress org_bau_compress_Lz4_LZ4Compress;
struct org_bau_compress_Lz4_LZ4Compress;
typedef struct org_bau_File_File org_bau_File_File;
struct org_bau_File_File;
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
struct org_bau_compress_Lz4_XXHash {
    int32_t v1;
    int32_t v2;
    int32_t v3;
    int32_t v4;
    int64_t total;
    int32_t _refCount;
};
org_bau_compress_Lz4_XXHash* org_bau_compress_Lz4_XXHash_new() {
    org_bau_compress_Lz4_XXHash* result = _malloc(sizeof(org_bau_compress_Lz4_XXHash));
    _traceMalloc(result);
    result->_refCount = 1;
    result->v1 = 0;
    result->v2 = 0;
    result->v3 = 0;
    result->v4 = 0;
    result->total = 0;
    return result;
}
struct org_bau_compress_Lz4_LZ4Compress {
    int_array* hashTable;
    int32_t _refCount;
};
org_bau_compress_Lz4_LZ4Compress* org_bau_compress_Lz4_LZ4Compress_new() {
    org_bau_compress_Lz4_LZ4Compress* result = _malloc(sizeof(org_bau_compress_Lz4_LZ4Compress));
    _traceMalloc(result);
    result->_refCount = 1;
    result->hashTable = 0;
    return result;
}
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
/* exception types */
/* global */
int __argc;
char **__argv;
/* functions */
int32_t i32_1(int64_t x);
int64_t idx_2(int64_t x, int64_t len);
i8_array* org_bau_Env_arg_1(int64_t index);
int64_t org_bau_Env_argCount_0();
org_bau_File_File* org_bau_File_openFile_2(i8_array* name, i8_array* mode);
void org_bau_File_File_close_1(org_bau_File_File* this);
int64_t org_bau_File_File_read_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len);
int64_t org_bau_File_File_readFully_4(org_bau_File_File* this, i8_array* buffer, int64_t pos, int64_t len);
int64_t org_bau_File_File_write_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len);
int64_t org_bau_Std_ord_1(i8_array* s);
org_bau_compress_Lz4_LZ4Compress* org_bau_compress_Lz4_LZ4Compress_1(int_array* hashTable);
org_bau_compress_Lz4_XXHash* org_bau_compress_Lz4_XXHash_0();
int64_t org_bau_compress_Lz4_countTrailingZeros_1(int64_t x);
int64_t org_bau_compress_Lz4_decompressBlock_4(i8_array* inData, int64_t inLen, i8_array* outData, int64_t outPos);
int32_t org_bau_compress_Lz4_hash5_2(i8_array* data, int64_t pos);
org_bau_compress_Lz4_LZ4Compress* org_bau_compress_Lz4_newLZ4Compress_0();
org_bau_compress_Lz4_XXHash* org_bau_compress_Lz4_newXXHash_1(int32_t seed);
int32_t org_bau_compress_Lz4_read4_2(i8_array* d, int64_t pos);
int64_t org_bau_compress_Lz4_read8_2(i8_array* d, int64_t pos);
int32_t org_bau_compress_Lz4_rotateLeft32_2(int32_t x, int64_t n);
int64_t org_bau_compress_Lz4_runLenCountFast_4(i8_array* data, int64_t aLen, int64_t ai, int64_t bi);
void org_bau_compress_Lz4_write4_3(i8_array* d, int64_t pos, int64_t x);
void org_bau_compress_Lz4_write8_3(i8_array* d, int64_t pos, int64_t x);
int64_t org_bau_compress_Lz4_LZ4Compress_compressBlock_5(org_bau_compress_Lz4_LZ4Compress* this, i8_array* inData, int64_t len, i8_array* outData, int64_t outPos);
int32_t org_bau_compress_Lz4_XXHash_update_4(org_bau_compress_Lz4_XXHash* this, i8_array* buf, int64_t start, int64_t len);
int64_t org_bau_compress_Lz4Tool_compressFile_3(i8_array* inputFileName, i8_array* outputFileName, int64_t level);
int64_t org_bau_compress_Lz4Tool_decompressFile_2(i8_array* inputFileName, i8_array* outputFileName);
i8_array* org_bau_compress_Lz4Tool_hex_2(int64_t x, int64_t len);
int64_t org_bau_compress_Lz4Tool_is_2(i8_array* a, i8_array* b);
int64_t org_bau_compress_Lz4Tool_main_0();
int64_t org_bau_compress_Lz4Tool_xxhashFile_1(i8_array* inputFileName);
int64_t shiftLeft_2(int64_t a, int64_t b);
int32_t shiftRight_i32_2(int32_t a, int64_t b);
int8_t shiftRight_i8_2(int8_t a, int64_t b);
int64_t shiftRight_int_2(int64_t a, int64_t b);
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_compress_Lz4_XXHash_free(org_bau_compress_Lz4_XXHash* x);
void org_bau_compress_Lz4_LZ4Compress_free(org_bau_compress_Lz4_LZ4Compress* x);
void org_bau_File_File_free(org_bau_File_File* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void org_bau_compress_Lz4_XXHash_free(org_bau_compress_Lz4_XXHash* x) {
    _free(x);
}
void org_bau_compress_Lz4_LZ4Compress_free(org_bau_compress_Lz4_LZ4Compress* x) {
    _decUse(x->hashTable, int_array);
    _free(x);
}
void org_bau_File_File_free(org_bau_File_File* x) {
    org_bau_File_File_close_1(x);
    if (x->_refCount) { fprintf(stdout, "Object re-referenced in the close method"); exit(1); }
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
i8_array* string_1004;
i8_array* string_1005;
i8_array* string_1006;
i8_array* string_1007;
i8_array* string_1008;
i8_array* string_1009;
i8_array* string_1010;
i8_array* string_1011;
i8_array* string_1012;
i8_array* string_1013;
int64_t BLOCK_SIZE;
int64_t PRIME1;
int64_t PRIME2;
int64_t PRIME3;
int64_t PRIME4;
int64_t PRIME5;
int32_t i32_1(int64_t x) {
    return x;
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    return arrayOutOfBounds(x, len);
}
i8_array* org_bau_Env_arg_1(int64_t index) {
    int64_t _t0 = index < 0;
    if (!(_t0)) {
        int64_t _t1 = index >= org_bau_Env_argCount_0();
        _t0 = _t1;
    }
    if (_t0) {
        i8_array* _r0 = i8_array_new(0);
        return _r0;
    }
    int64_t len = 0;
    len = strlen(__argv[index]);
    i8_array* result = i8_array_new(len);
    strncpy((char*) result->data, __argv[index], len);
    return result;
}
int64_t org_bau_Env_argCount_0() {
    return __argc;
    return 0;
}
org_bau_File_File* org_bau_File_openFile_2(i8_array* name, i8_array* mode) {
    // TODO verify strings
    FILE* fp = fopen((char*) name->data, (char*) mode->data);
    org_bau_File_File* f = org_bau_File_File_new();
    f->filePointer = (uint64_t) fp;
    return f;
}
void org_bau_File_File_close_1(org_bau_File_File* this) {
    FILE* fp = (FILE*) (this->filePointer);
    fclose(fp);
}
int64_t org_bau_File_File_read_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len) {
    FILE* fp = (FILE*) (this->filePointer);
    if (pos < 0 || len < 0 || pos + len > data->len) {
        return 0;
    } else {
        return fread(data->data + pos, 1, len, fp);
    }
}
int64_t org_bau_File_File_readFully_4(org_bau_File_File* this, i8_array* buffer, int64_t pos, int64_t len) {
    int64_t count = 0;
    while (1) {
        int64_t l = org_bau_File_File_read_4(this, buffer, count + pos, len - count);
        if (l == 0) {
            return count;
        } else {
            count += l;
        }
    }
}
int64_t org_bau_File_File_write_4(org_bau_File_File* this, i8_array* data, int64_t pos, int64_t len) {
    FILE* fp = (FILE*) (this->filePointer);
    if (pos < 0 || len < 0 || pos + len > data->len) {
        return -1;
    } else {
        return fwrite(data->data + pos, 1, len, fp);
    }
}
int64_t org_bau_Std_ord_1(i8_array* s) {
    if (s->len) {
        int8_t _r0 = s->data[idx_2(0, s->len)];
        return _r0;
    }
    return 0;
}
org_bau_compress_Lz4_LZ4Compress* org_bau_compress_Lz4_LZ4Compress_1(int_array* hashTable) {
    org_bau_compress_Lz4_LZ4Compress* _t2 = org_bau_compress_Lz4_LZ4Compress_new();
    _decUse(_t2->hashTable, int_array);
    _t2->hashTable = hashTable;
    _incUse(_t2->hashTable);
    return _t2;
}
org_bau_compress_Lz4_XXHash* org_bau_compress_Lz4_XXHash_0() {
    org_bau_compress_Lz4_XXHash* _t0 = org_bau_compress_Lz4_XXHash_new();
    _t0->v1 = 0;
    _t0->v2 = 0;
    _t0->v3 = 0;
    _t0->v4 = 0;
    _t0->total = 0;
    return _t0;
}
int64_t org_bau_compress_Lz4_countTrailingZeros_1(int64_t x) {
    return __builtin_ctz(x);
    return 0;
}
int64_t org_bau_compress_Lz4_decompressBlock_4(i8_array* inData, int64_t inLen, i8_array* outData, int64_t outPos) {
    int64_t p = 0;
    if (inData->len < 16) {
        return -1;
    }
    if (outData->len < 16) {
        return -1;
    }
    while (1) {
        int64_t tag = inData->data[idx_2(p, inData->len)] & 255;
        int64_t next = p + 1;
        if (next >= inData->len) {
            break;
        }
        p = next;
        int64_t literalLen = shiftRight_int_2(tag, 4);
        if (literalLen == 15) {
            while (1) {
                int64_t x = inData->data[idx_2(p, inData->len)] & 255;
                p += 1;
                literalLen += x;
                if (x != 255) {
                    break;
                }
            }
        }
        if (p < ( inData->len - 8 )) {
            int64_t s = 0;
            s = p;
            int64_t o = 0;
            if (outPos >= ( outData->len - 8 )) {
                return -1;
            }
            o = outPos;
            int64_t n = 0;
            while (1) {
                int64_t x = org_bau_compress_Lz4_read8_2(inData, s);
                org_bau_compress_Lz4_write8_3(outData, o, x);
                n += 8;
                if (n >= literalLen) {
                    break;
                }
                next = s + 8;
                if (next > ( inData->len - 8 )) {
                    break;
                }
                s = next;
                next = o + 8;
                if (next > ( outData->len - 8 )) {
                    break;
                }
                o = next;
            }
        } else {
            while (1 == 1) {
                int64_t i = 0;
                while (i < literalLen) {
                    outData->data[idx_2(outPos + i, outData->len)] = inData->data[idx_2(p + i, inData->len)];
                    continue4:;
                    i += 1;
                }
                break;
            }
        }
        outPos += literalLen;
        p += literalLen;
        if (( p + 1 ) >= inLen) {
            break;
        }
        int64_t offset = (inData->data[idx_2(p, inData->len)] & 255) | (shiftLeft_2((inData->data[idx_2(p + 1, inData->len)] & 255), 8));
        if (offset == 0) {
            return -1;
        }
        if (offset > outPos) {
            return -1;
        }
        p += 2;
        if (p >= ( inLen - 1 )) {
            break;
        }
        int64_t runLen = tag & 15;
        if (runLen == 15) {
            while (1) {
                int64_t x = inData->data[idx_2(p, inData->len)] & 255;
                p += 1;
                runLen += x;
                if (x != 255) {
                    break;
                }
            }
        }
        runLen += 4;
        if (offset >= 8) {
            next = outPos - offset;
            if (next >= ( outData->len - 8 )) {
                return -1;
            }
            int64_t s = 0;
            s = next;
            int64_t o = 0;
            if (outPos >= ( outData->len - 8 )) {
                return -1;
            }
            o = outPos;
            int64_t n = 0;
            while (1) {
                int64_t x = org_bau_compress_Lz4_read8_2(outData, s);
                org_bau_compress_Lz4_write8_3(outData, o, x);
                n += 8;
                if (n >= runLen) {
                    break;
                }
                next = s + 8;
                if (next >= ( outData->len - 8 )) {
                    break;
                }
                s = next;
                next = o + 8;
                if (next >= ( outData->len - 8 )) {
                    break;
                }
                o = next;
            }
        } else {
            while (1 == 1) {
                int64_t i = 0;
                while (i < runLen) {
                    outData->data[idx_2(outPos + i, outData->len)] = outData->data[idx_2(( outPos + i ) - offset, outData->len)];
                    continue8:;
                    i += 1;
                }
                break;
            }
        }
        outPos += runLen;
    }
    return outPos;
}
int32_t org_bau_compress_Lz4_hash5_2(i8_array* data, int64_t pos) {
    int64_t x = org_bau_compress_Lz4_read8_2(data, pos);
    int64_t prime5bytes = 889523592379;
    int32_t _t0 = i32_1(shiftRight_int_2(((shiftLeft_2(x, 24)) * 889523592379), 52));
    return _t0;
}
org_bau_compress_Lz4_LZ4Compress* org_bau_compress_Lz4_newLZ4Compress_0() {
    org_bau_compress_Lz4_LZ4Compress* x = org_bau_compress_Lz4_LZ4Compress_1(int_array_new(4096));
    return x;
}
org_bau_compress_Lz4_XXHash* org_bau_compress_Lz4_newXXHash_1(int32_t seed) {
    org_bau_compress_Lz4_XXHash* x = org_bau_compress_Lz4_XXHash_0();
    x->v1 = ( seed + 2654435761 ) + 2246822519;
    x->v2 = seed + 2246822519;
    x->v3 = seed;
    x->v4 = seed - 2654435761;
    x->total = 0;
    return x;
}
int32_t org_bau_compress_Lz4_read4_2(i8_array* d, int64_t pos) {
    int64_t _r0 = (d->data[pos] & 255) | (shiftLeft_2((d->data[pos + 1] & 255), 8)) | (shiftLeft_2((d->data[pos + 2] & 255), 16)) | (shiftLeft_2((d->data[pos + 3] & 255), 24));
    return _r0;
}
int64_t org_bau_compress_Lz4_read8_2(i8_array* d, int64_t pos) {
    int64_t _r0 = (d->data[pos] & 255) | (shiftLeft_2((d->data[pos + 1] & 255), 8)) | (shiftLeft_2((d->data[pos + 2] & 255), 16)) | (shiftLeft_2((d->data[pos + 3] & 255), 24)) | (shiftLeft_2((d->data[pos + 4] & 255), 32)) | (shiftLeft_2((d->data[pos + 5] & 255), 40)) | (shiftLeft_2((d->data[pos + 6] & 255), 48)) | (shiftLeft_2((d->data[pos + 7] & 255), 56));
    return _r0;
}
int32_t org_bau_compress_Lz4_rotateLeft32_2(int32_t x, int64_t n) {
    int64_t _r0 = (shiftLeft_2(x, n)) | (shiftRight_i32_2(x, (32 - n)));
    return _r0;
}
int64_t org_bau_compress_Lz4_runLenCountFast_4(i8_array* data, int64_t aLen, int64_t ai, int64_t bi) {
    int64_t runLen = 0;
    while (1) {
        int64_t ax = org_bau_compress_Lz4_read8_2(data, ai);
        int64_t bx = org_bau_compress_Lz4_read8_2(data, bi);
        int64_t diff = ax ^ bx;
        if (diff != 0) {
            runLen += shiftRight_int_2(org_bau_compress_Lz4_countTrailingZeros_1(diff), 3);
            return runLen;
        }
        int64_t next = ai + 8;
        if (next >= ( aLen - 8 )) {
            break;
        }
        if (next > ( data->len - 8 )) {
            break;
        }
        ai = next;
        next = bi + 8;
        if (next > ( data->len - 8 )) {
            break;
        }
        bi = next;
        runLen += 8;
    }
    return runLen;
}
void org_bau_compress_Lz4_write4_3(i8_array* d, int64_t pos, int64_t x) {
    d->data[pos] = x;
    d->data[pos + 1] = shiftRight_int_2(x, 8);
    d->data[pos + 2] = shiftRight_int_2(x, 16);
    d->data[pos + 3] = shiftRight_int_2(x, 24);
}
void org_bau_compress_Lz4_write8_3(i8_array* d, int64_t pos, int64_t x) {
    d->data[pos] = x;
    d->data[pos + 1] = shiftRight_int_2(x, 8);
    d->data[pos + 2] = shiftRight_int_2(x, 16);
    d->data[pos + 3] = shiftRight_int_2(x, 24);
    d->data[pos + 4] = shiftRight_int_2(x, 32);
    d->data[pos + 5] = shiftRight_int_2(x, 40);
    d->data[pos + 6] = shiftRight_int_2(x, 48);
    d->data[pos + 7] = shiftRight_int_2(x, 56);
}
int64_t org_bau_compress_Lz4_LZ4Compress_compressBlock_5(org_bau_compress_Lz4_LZ4Compress* this, i8_array* inData, int64_t len, i8_array* outData, int64_t outPos) {
    if (inData->len <= 16) {
        return -1;
    }
    if (len >= ( inData->len - 7 )) {
        return -1;
    }
    if (outData->len <= 16) {
        return -1;
    }
    int64_t inLen = 0;
    inLen = len;
    int64_t literalLen = 0;
    int64_t inPos = 0;
    int64_t skipTrigger = 6;
    int64_t searchMatch = shiftLeft_2(1, skipTrigger);
    while (1) {
        int64_t runLen = 0;
        int64_t candidatePos = 0;
        if (inPos < ( inLen - 16 )) {
            int32_t h = org_bau_compress_Lz4_hash5_2(inData, inPos);
            int64_t cp = this->hashTable->data[idx_2(h, this->hashTable->len)];
            this->hashTable->data[idx_2(h, this->hashTable->len)] = inPos;
            int64_t _t0 = cp >= inPos;
            if (!(_t0)) {
                int64_t _t1 = ( cp + 65535 ) < inPos;
                _t0 = _t1;
            }
            if (_t0) {
                int64_t step = shiftRight_int_2(searchMatch, skipTrigger);
                int64_t next = inPos + step;
                if (next < ( inData->len - 8 )) {
                    inPos = next;
                    literalLen += step;
                    searchMatch += 1;
                    continue;
                }
                literalLen += inLen - inPos;
                inPos = inLen;
                continue;
            } else {
                if (cp < ( inData->len - 8 )) {
                    candidatePos = cp;
                }
                runLen = org_bau_compress_Lz4_runLenCountFast_4(inData, inLen, inPos, candidatePos);
                if (runLen >= 4) {
                    int64_t p = ( inPos + runLen ) - 2;
                    if (p >= ( inData->len - 8 )) {
                        return -1;
                    }
                    h = org_bau_compress_Lz4_hash5_2(inData, p);
                    this->hashTable->data[idx_2(h, this->hashTable->len)] = p;
                } else {
                    int64_t step = shiftRight_int_2(searchMatch, skipTrigger);
                    int64_t next = inPos + step;
                    if (next < ( inData->len - 8 )) {
                        inPos = next;
                        literalLen += step;
                        searchMatch += 1;
                        continue;
                    }
                    literalLen += inLen - inPos;
                    inPos = inLen;
                    continue;
                }
            }
        } else {
            literalLen += inLen - inPos;
            inPos = inLen;
            runLen = 4;
            candidatePos = 0;
        }
        int64_t tagPos = outPos;
        outPos += 1;
        while (1 == 1) {
            int64_t _t2 = candidatePos > 0;
            if (_t2) {
                int64_t _t3 = literalLen > 0;
                _t2 = _t3;
            }
            int64_t _t4 = _t2;
            if (_t4) {
                int64_t _t5 = inPos > 0;
                _t4 = _t5;
            }
            int64_t _t6 = _t4;
            if (_t6) {
                int64_t _t7 = inData->data[idx_2(inPos - 1, inData->len)] == inData->data[candidatePos - 1];
                _t6 = _t7;
            }
            if (!(_t6)) {
                break;
            }
            runLen += 1;
            int64_t next = inPos - 1;
            if (next < ( inData->len - 7 )) {
                inPos = next;
            }
            literalLen -= 1;
            candidatePos -= 1;
        }
        int64_t copyLen = literalLen;
        if (literalLen >= 15) {
            while (( literalLen - 15 ) >= 255) {
                outData->data[idx_2(outPos, outData->len)] = 0xff;
                outPos += 1;
                literalLen -= 0xff;
            }
            outData->data[idx_2(outPos, outData->len)] = literalLen - 15;
            outPos += 1;
            literalLen = 0xf;
        }
        int64_t s = 0;
        int64_t next = inPos - copyLen;
        if (next >= ( inData->len - 8 )) {
            return -1;
        }
        s = next;
        int64_t o = 0;
        next = outPos;
        if (next >= ( outData->len - 8 )) {
            return -1;
        }
        o = next;
        int64_t n = 0;
        while (1) {
            int64_t x = org_bau_compress_Lz4_read8_2(inData, s);
            org_bau_compress_Lz4_write8_3(outData, o, x);
            n += 8;
            if (n >= copyLen) {
                break;
            }
            next = s + 8;
            if (next > ( inData->len - 8 )) {
                break;
            }
            s = next;
            next = o + 8;
            if (next > ( outData->len - 8 )) {
                break;
            }
            o = next;
        }
        outPos += copyLen;
        if (inPos < inLen) {
            int64_t offset = inPos - candidatePos;
            outData->data[idx_2(outPos, outData->len)] = offset;
            outData->data[idx_2(outPos + 1, outData->len)] = shiftRight_int_2(offset, 8);
            outPos += 2;
        } else {
            int64_t tag = shiftLeft_2(literalLen, 4);
            outData->data[idx_2(tagPos, outData->len)] = tag;
            break;
        }
        int64_t skipLen = runLen;
        runLen -= 4;
        if (runLen >= 15) {
            while (( runLen - 15 ) >= 255) {
                outData->data[idx_2(outPos, outData->len)] = 0xff;
                outPos += 1;
                runLen -= 0xff;
            }
            outData->data[idx_2(outPos, outData->len)] = runLen - 15;
            outPos += 1;
            runLen = 0xf;
        }
        int64_t tag = (shiftLeft_2(literalLen, 4)) | runLen;
        outData->data[idx_2(tagPos, outData->len)] = tag;
        next = inPos + skipLen;
        if (next >= ( inData->len - 7 )) {
            return -1;
        }
        inPos = next;
        literalLen = 0;
        searchMatch = shiftLeft_2(1, skipTrigger);
    }
    return outPos;
}
int32_t org_bau_compress_Lz4_XXHash_update_4(org_bau_compress_Lz4_XXHash* this, i8_array* buf, int64_t start, int64_t len) {
    int64_t end = start + len;
    int64_t pos = start;
    if (len >= 16) {
        if (buf->len < 16) {
            return -1;
        }
        if (pos >= ( buf->len - 16 )) {
            return -1;
        }
        int64_t p = 0;
        p = pos;
        while (1) {
            this->v1 = i32_1(org_bau_compress_Lz4_rotateLeft32_2(i32_1(( org_bau_compress_Lz4_read4_2(buf, p) * 2246822519 ) + this->v1), 13) * 2654435761);
            this->v2 = i32_1(org_bau_compress_Lz4_rotateLeft32_2(i32_1(( org_bau_compress_Lz4_read4_2(buf, p + 4) * 2246822519 ) + this->v2), 13) * 2654435761);
            this->v3 = i32_1(org_bau_compress_Lz4_rotateLeft32_2(i32_1(( org_bau_compress_Lz4_read4_2(buf, p + 8) * 2246822519 ) + this->v3), 13) * 2654435761);
            this->v4 = i32_1(org_bau_compress_Lz4_rotateLeft32_2(i32_1(( org_bau_compress_Lz4_read4_2(buf, p + 12) * 2246822519 ) + this->v4), 13) * 2654435761);
            int64_t next = p + 16;
            if (next >= ( buf->len - 16 )) {
                break;
            }
            p = next;
            if (next > ( end - 16 )) {
                break;
            }
        }
        pos = p;
    }
    this->total += len;
    int32_t h32 = i32_1(this->total);
    if (this->total >= 16) {
        h32 += ( org_bau_compress_Lz4_rotateLeft32_2(this->v1, 1) + org_bau_compress_Lz4_rotateLeft32_2(this->v2, 7) ) + org_bau_compress_Lz4_rotateLeft32_2(this->v3, 12) + org_bau_compress_Lz4_rotateLeft32_2(this->v4, 18);
    } else {
        h32 += this->v3 + 374761393;
    }
    if (( pos + 4 ) <= end) {
        if (buf->len < 4) {
            return -1;
        }
        int64_t p = 0;
        if (pos >= ( buf->len - 4 )) {
            return -1;
        }
        p = pos;
        while (( p + 4 ) <= end) {
            h32 = org_bau_compress_Lz4_rotateLeft32_2(i32_1(( org_bau_compress_Lz4_read4_2(buf, p) * 3266489917 ) + h32), 17) * 668265263;
            int64_t next = p + 4;
            if (next >= ( buf->len - 4 )) {
                break;
            }
            p = next;
        }
        pos = p;
    }
    while (pos < end) {
        h32 = org_bau_compress_Lz4_rotateLeft32_2(i32_1((buf->data[pos] & 255) * 374761393 + h32), 11) * 2654435761;
        int64_t next = pos + 1;
        if (next >= buf->len) {
            break;
        }
        pos = next;
    }
    h32 = (h32 ^ (shiftRight_i32_2(h32, 15))) * 2246822519;
    h32 = (h32 ^ (shiftRight_i32_2(h32, 13))) * 3266489917;
    int64_t _r0 = h32 ^ (shiftRight_i32_2(h32, 16));
    return _r0;
}
int64_t org_bau_compress_Lz4Tool_compressFile_3(i8_array* inputFileName, i8_array* outputFileName, int64_t level) {
    org_bau_File_File* in = org_bau_File_openFile_2(inputFileName, string_1002);
    if (!(in->filePointer)) {
        printf("File not found: %.*s\n", inputFileName->len, inputFileName->data);
        _decUseStack(in, org_bau_File_File);
        return 0;
    }
    org_bau_File_File* out = org_bau_File_openFile_2(outputFileName, string_1004);
    if (!(out->filePointer)) {
        printf("Could not open file: %.*s\n", outputFileName->len, outputFileName->data);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return 0;
    }
    i8_array* header = i8_array_new(7);
    org_bau_compress_Lz4_write4_3(header, 0, 0x184d2204);
    header->data[4] = 96;
    header->data[5] = 112;
    org_bau_compress_Lz4_XXHash* hash = org_bau_compress_Lz4_newXXHash_1(0);
    header->data[6] = (shiftRight_i32_2(org_bau_compress_Lz4_XXHash_update_4(hash, header, 4, 2), 8)) & 255;
    org_bau_File_File_write_4(out, header, 0, 7);
    int64_t blockSize = 4194304;
    i8_array* block = i8_array_new(4194314);
    i8_array* outBlock = i8_array_new(5242880);
    org_bau_compress_Lz4_LZ4Compress* lz4 = org_bau_compress_Lz4_newLZ4Compress_0();
    int64_t totalSize = 0;
    while (1) {
        int64_t read = org_bau_File_File_readFully_4(in, block, 0, 4194304);
        if (read == 0) {
            break;
        }
        totalSize += read;
        int64_t end = 0;
        if (read < 16) {
            end = read;
        } else {
            end = org_bau_compress_Lz4_LZ4Compress_compressBlock_5(lz4, block, read, outBlock, 4);
        }
        if (end >= read) {
            int64_t writeBlockSize = 2147483648 | read;
            while (1 == 1) {
                int64_t i = 0;
                while (1) {
                    outBlock->data[i + 4] = block->data[i];
                    continue2:;
                    int64_t _next = i + 1;
                    if (_next >= read) {
                        break;
                    }
                    i = _next;
                }
                break;
            }
            org_bau_compress_Lz4_write4_3(outBlock, 0, writeBlockSize);
            org_bau_File_File_write_4(out, outBlock, 0, read + 4);
        } else {
            org_bau_compress_Lz4_write4_3(outBlock, 0, end - 4);
            org_bau_File_File_write_4(out, outBlock, 0, end);
        }
    }
    org_bau_compress_Lz4_write4_3(outBlock, 0, 0);
    org_bau_File_File_write_4(out, outBlock, 0, 4);
    _decUseStack(lz4, org_bau_compress_Lz4_LZ4Compress);
    _decUseStack(outBlock, i8_array);
    _decUseStack(block, i8_array);
    _decUseStack(hash, org_bau_compress_Lz4_XXHash);
    _decUseStack(header, i8_array);
    _decUseStack(out, org_bau_File_File);
    _decUseStack(in, org_bau_File_File);
    return totalSize;
}
int64_t org_bau_compress_Lz4Tool_decompressFile_2(i8_array* inputFileName, i8_array* outputFileName) {
    org_bau_File_File* in = org_bau_File_openFile_2(inputFileName, string_1002);
    if (!(in->filePointer)) {
        printf("File not found: %.*s\n", inputFileName->len, inputFileName->data);
        _decUseStack(in, org_bau_File_File);
        return 0;
    }
    org_bau_File_File* out = org_bau_File_openFile_2(outputFileName, string_1004);
    if (!(out->filePointer)) {
        printf("Could not open file: %.*s\n", outputFileName->len, outputFileName->data);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return 0;
    }
    i8_array* header = i8_array_new(7);
    org_bau_File_File_readFully_4(in, header, 0, 7);
    int32_t magic = org_bau_compress_Lz4_read4_2(header, 0);
    if (magic != 407708164) {
        _decUseStack(header, i8_array);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return -1;
    }
    int8_t flags = header->data[4];
    int64_t version = shiftRight_i8_2(flags, 6);
    if (version != 1) {
        _decUseStack(header, i8_array);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return -1;
    }
    int64_t blockIndependanceFlag = (shiftRight_i8_2(flags, 5)) & 1;
    if (blockIndependanceFlag == 0) {
        _decUseStack(header, i8_array);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return -1;
    }
    int64_t blockChecksumFlag = (shiftRight_i8_2(flags, 4)) & 1;
    if (blockChecksumFlag) {
        _decUseStack(header, i8_array);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return -1;
    }
    int64_t contentSizeFlag = (shiftRight_i8_2(flags, 3)) & 1;
    if (contentSizeFlag) {
        _decUseStack(header, i8_array);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return -1;
    }
    int64_t contentChecksumFlag = (shiftRight_i8_2(flags, 2)) & 1;
    if ((shiftRight_i8_2(flags, 1)) & 1) {
        _decUseStack(header, i8_array);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return -1;
    }
    if (flags & 1) {
        _decUseStack(header, i8_array);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return -1;
    }
    int8_t bd = header->data[5];
    int64_t blockMaxSize = (shiftRight_i8_2(bd, 4)) & 7;
    int64_t _t0 = blockMaxSize < 4;
    if (!(_t0)) {
        int64_t _t1 = blockMaxSize > 7;
        _t0 = _t1;
    }
    if (_t0) {
        _decUseStack(header, i8_array);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return -1;
    }
    int64_t headerChecksum = header->data[6] & 255;
    org_bau_compress_Lz4_XXHash* hash = org_bau_compress_Lz4_newXXHash_1(0);
    int32_t _t2 = org_bau_compress_Lz4_XXHash_update_4(hash, header, 4, 2);
    int64_t xxhash = (shiftRight_i32_2(_t2, 8)) & 255;
    if ((xxhash & 255) != headerChecksum) {
        _decUseStack(hash, org_bau_compress_Lz4_XXHash);
        _decUseStack(header, i8_array);
        _decUseStack(out, org_bau_File_File);
        _decUseStack(in, org_bau_File_File);
        return -1;
    }
    i8_array* block = i8_array_new(4194314);
    i8_array* outBlock = i8_array_new(4194314);
    int64_t outputFileSize = 0;
    while (1) {
        org_bau_File_File_readFully_4(in, header, 0, 4);
        int32_t blockSize = org_bau_compress_Lz4_read4_2(header, 0);
        if (blockSize == 0) {
            break;
        }
        int64_t uncompressed = (shiftRight_i32_2(blockSize, 31)) & 1;
        blockSize &= 0x7fffffff;
        if (blockSize > 4194304) {
            _decUseStack(outBlock, i8_array);
            _decUseStack(block, i8_array);
            _decUseStack(hash, org_bau_compress_Lz4_XXHash);
            _decUseStack(header, i8_array);
            _decUseStack(out, org_bau_File_File);
            _decUseStack(in, org_bau_File_File);
            return -1;
        }
        org_bau_File_File_readFully_4(in, block, 0, blockSize);
        if (uncompressed) {
            org_bau_File_File_write_4(out, block, 0, blockSize);
            outputFileSize += blockSize;
        } else {
            int64_t size = org_bau_compress_Lz4_decompressBlock_4(block, blockSize, outBlock, 0);
            org_bau_File_File_write_4(out, outBlock, 0, size);
            outputFileSize += size;
        }
    }
    _decUseStack(outBlock, i8_array);
    _decUseStack(block, i8_array);
    _decUseStack(hash, org_bau_compress_Lz4_XXHash);
    _decUseStack(header, i8_array);
    _decUseStack(out, org_bau_File_File);
    _decUseStack(in, org_bau_File_File);
    return outputFileSize;
}
i8_array* org_bau_compress_Lz4Tool_hex_2(int64_t x, int64_t len) {
    int64_t l = len;
    if (l < 0) {
        l = 0;
    } else if (l > 8) {
        l = 8;
    }
    i8_array* data = i8_array_new(l);
    int64_t y = x;
    int64_t i = l - 1;
    while (i >= 0) {
        int64_t c = 48 + (y & 15);
        if ((y & 15) > 9) {
            c = ( 97 + (y & 15) ) - 10;
        }
        data->data[idx_2(i, data->len)] = c;
        y >>= 4;
        i -= 1;
    }
    return data;
}
int64_t org_bau_compress_Lz4Tool_is_2(i8_array* a, i8_array* b) {
    if (a->len != b->len) {
        return 0;
    }
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            if (a->data[i] != b->data[idx_2(i, b->len)]) {
                return 0;
            }
            continue1:;
            int64_t _next = i + 1;
            if (_next >= a->len) {
                break;
            }
            i = _next;
        }
        break;
    }
    return 1;
}
int64_t org_bau_compress_Lz4Tool_main_0() {
    int64_t args = org_bau_Env_argCount_0();
    if (args < 3) {
        return 0;
    }
    i8_array* a1 = org_bau_Env_arg_1(1);
    i8_array* a2 = org_bau_Env_arg_1(2);
    int64_t _t0 = args == 4;
    if (_t0) {
        int64_t _t1 = org_bau_compress_Lz4Tool_is_2(a1, string_1008);
        _t0 = _t1;
    }
    int64_t _t4 = args == 4;
    if (_t4) {
        int64_t _t5 = org_bau_compress_Lz4Tool_is_2(a1, string_1011);
        _t4 = _t5;
    }
    int64_t _t8 = args == 3;
    if (_t8) {
        int64_t _t9 = org_bau_compress_Lz4Tool_is_2(a1, string_1013);
        _t8 = _t9;
    }
    if (_t0) {
        i8_array* _t2 = org_bau_Env_arg_1(3);
        int64_t _t3 = org_bau_compress_Lz4Tool_decompressFile_2(a2, _t2);
        printf("Decompressed %lld bytes\n", (long long)_t3);
        _decUseStack(_t2, i8_array);
    } else if (_t4) {
        i8_array* _t6 = org_bau_Env_arg_1(3);
        int64_t _t7 = org_bau_compress_Lz4Tool_compressFile_3(a2, _t6, 1);
        printf("Compressed %lld bytes\n", (long long)_t7);
        _decUseStack(_t6, i8_array);
    } else if (_t8) {
        int64_t _t10 = org_bau_compress_Lz4Tool_xxhashFile_1(a2);
        i8_array* _t11 = org_bau_compress_Lz4Tool_hex_2(_t10, 8);
        printf("%.*s\n", _t11->len, _t11->data);
        _decUseStack(_t11, i8_array);
    }
    _decUseStack(a2, i8_array);
    _decUseStack(a1, i8_array);
    return 0;
}
int64_t org_bau_compress_Lz4Tool_xxhashFile_1(i8_array* inputFileName) {
    org_bau_File_File* in = org_bau_File_openFile_2(inputFileName, string_1002);
    if (!(in->filePointer)) {
        printf("File not found: %.*s\n", inputFileName->len, inputFileName->data);
        _decUseStack(in, org_bau_File_File);
        return 0;
    }
    org_bau_compress_Lz4_XXHash* hash = org_bau_compress_Lz4_newXXHash_1(0);
    i8_array* buffer = i8_array_new(4194314);
    int64_t result = 0;
    while (1) {
        int64_t read = org_bau_File_File_readFully_4(in, buffer, 0, 4194304);
        if (read <= 0) {
            _decUseStack(buffer, i8_array);
            _decUseStack(hash, org_bau_compress_Lz4_XXHash);
            _decUseStack(in, org_bau_File_File);
            return result;
        }
        result = org_bau_compress_Lz4_XXHash_update_4(hash, buffer, 0, read);
    }
    _decUseStack(buffer, i8_array);
    _decUseStack(hash, org_bau_compress_Lz4_XXHash);
    _decUseStack(in, org_bau_File_File);
}
int64_t shiftLeft_2(int64_t a, int64_t b) {
    return a << b;
}
int32_t shiftRight_i32_2(int32_t a, int64_t b) {
    return ((uint32_t) a) >> b;
}
int8_t shiftRight_i8_2(int8_t a, int64_t b) {
    return ((uint8_t) a) >> b;
}
int64_t shiftRight_int_2(int64_t a, int64_t b) {
    return ((uint64_t) a) >> b;
}
int main(int _argc, char *_argv[]) {
    __argc = _argc;
    __argv = _argv;
    string_1000 = str_const("Exit code ", 10);
    string_1001 = str_const("; will now throw an array out-of-bounds exception", 49);
    string_1002 = str_const("r", 1);
    string_1003 = str_const("File not found: ", 16);
    string_1004 = str_const("w", 1);
    string_1005 = str_const("Could not open file: ", 21);
    string_1006 = str_const("0", 1);
    string_1007 = str_const("a", 1);
    string_1008 = str_const("-d", 2);
    string_1009 = str_const("Decompressed ", 13);
    string_1010 = str_const(" bytes", 6);
    string_1011 = str_const("-1", 2);
    string_1012 = str_const("Compressed ", 11);
    string_1013 = str_const("-h", 2);
    BLOCK_SIZE = 4194304;
    PRIME1 = 2654435761;
    PRIME2 = 2246822519;
    PRIME3 = 3266489917;
    PRIME4 = 668265263;
    PRIME5 = 374761393;
    int64_t _t0 = org_bau_compress_Lz4Tool_main_0();
    ;
    _end();
    return 0;
}
/*

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

*/
