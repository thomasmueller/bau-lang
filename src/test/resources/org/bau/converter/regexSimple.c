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
typedef struct org_bau_List_List org_bau_List_List;
struct org_bau_List_List;
typedef struct org_bau_Exception_exception org_bau_Exception_exception;
struct org_bau_Exception_exception;
typedef struct Token Token;
struct Token;
typedef struct Token_array Token_array;
struct Token_array;
typedef struct org_bau_List_List_i8 org_bau_List_List_i8;
struct org_bau_List_List_i8;
typedef struct match match;
struct match;
typedef struct org_bau_List_List_Token org_bau_List_List_Token;
struct org_bau_List_List_Token;
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
struct org_bau_List_List {
    int32_t _refCount;
};
org_bau_List_List* org_bau_List_List_new() {
    org_bau_List_List* result = _malloc(sizeof(org_bau_List_List));
    _traceMalloc(result);
    result->_refCount = 1;
    return result;
}
struct org_bau_Exception_exception {
    int64_t exceptionType;
    i8_array* message;
    int32_t _refCount;
};
org_bau_Exception_exception org_bau_Exception_exception_new() {
    org_bau_Exception_exception result;
    result.exceptionType = 0;
    result.message = 0;
    return result;
}
struct Token {
    int64_t type;
    org_bau_List_List_i8* data;
    int64_t min;
    int64_t max;
    int32_t _refCount;
};
Token* Token_new() {
    Token* result = _malloc(sizeof(Token));
    _traceMalloc(result);
    result->_refCount = 1;
    result->type = 0;
    result->data = 0;
    result->min = 0;
    result->max = 0;
    return result;
}
struct Token_array {
    int32_t len;
    Token** data;
    int32_t _refCount;
};
Token_array* Token_array_new(uint32_t len) {
    Token_array* result = _malloc(sizeof(Token_array));
    _traceMalloc(result);
    result->len = len;
    result->data = _malloc(sizeof(Token*) * len);
    _traceMalloc(result->data);
    result->_refCount = 1;
    return result;
}
struct org_bau_List_List_i8 {
    i8_array* array;
    int64_t size;
    int32_t _refCount;
};
org_bau_List_List_i8* org_bau_List_List_i8_new() {
    org_bau_List_List_i8* result = _malloc(sizeof(org_bau_List_List_i8));
    _traceMalloc(result);
    result->_refCount = 1;
    result->array = 0;
    result->size = 0;
    return result;
}
struct match {
    int64_t start;
    int64_t end;
    int32_t _refCount;
};
match match_new() {
    match result;
    result.start = 0;
    result.end = 0;
    return result;
}
struct org_bau_List_List_Token {
    Token_array* array;
    int64_t size;
    int32_t _refCount;
};
org_bau_List_List_Token* org_bau_List_List_Token_new() {
    org_bau_List_List_Token* result = _malloc(sizeof(org_bau_List_List_Token));
    _traceMalloc(result);
    result->_refCount = 1;
    result->array = 0;
    result->size = 0;
    return result;
}
/* exception types */
typedef struct _int64_t_or_exception _int64_t_or_exception;
struct _int64_t_or_exception {
    org_bau_Exception_exception exception;
    int64_t result;
};
_int64_t_or_exception ok_int64_t_or_exception(int64_t result) {
    _int64_t_or_exception x;
    x.exception.exceptionType = -1;
    x.result = result;
    return x;
}
_int64_t_or_exception exception_int64_t_or_exception(org_bau_Exception_exception exception) {
    _int64_t_or_exception x;
    x.exception = exception;
    return x;
}
typedef struct _match_or_exception _match_or_exception;
struct _match_or_exception {
    org_bau_Exception_exception exception;
    match result;
};
_match_or_exception ok_match_or_exception(match result) {
    _match_or_exception x;
    x.exception.exceptionType = -1;
    x.result = result;
    return x;
}
_match_or_exception exception_match_or_exception(org_bau_Exception_exception exception) {
    _match_or_exception x;
    x.exception = exception;
    return x;
}
typedef struct _org_bau_List_List_Token_or_exception _org_bau_List_List_Token_or_exception;
struct _org_bau_List_List_Token_or_exception {
    org_bau_Exception_exception exception;
    org_bau_List_List_Token* result;
};
_org_bau_List_List_Token_or_exception ok_org_bau_List_List_Token_or_exception(org_bau_List_List_Token* result) {
    _org_bau_List_List_Token_or_exception x;
    x.exception.exceptionType = -1;
    x.result = result;
    return x;
}
_org_bau_List_List_Token_or_exception exception_org_bau_List_List_Token_or_exception(org_bau_Exception_exception exception) {
    _org_bau_List_List_Token_or_exception x;
    x.exception = exception;
    return x;
}
/* functions */
_int64_t_or_exception Token_matchChar_3(Token* this, i8_array* text, int64_t pos);
_match_or_exception find_2(i8_array* text, i8_array* regex);
int64_t idx_2(int64_t x, int64_t len);
match match_2(int64_t start, int64_t end);
_int64_t_or_exception matchHere_4(org_bau_List_List_Token* list, int64_t rp, i8_array* text, int64_t tp);
_int64_t_or_exception matchStar_6(Token* t, org_bau_List_List_Token* list, int64_t rp, i8_array* text, int64_t tp, int64_t max);
_int64_t_or_exception matches_2(i8_array* text, i8_array* regex);
org_bau_Exception_exception org_bau_Exception_exception_1(i8_array* message);
org_bau_List_List_Token* org_bau_List_newList_Token_1(int64_t _T);
org_bau_List_List_i8* org_bau_List_newList_i8_1(int64_t _T);
void org_bau_List_List_Token_add_2(org_bau_List_List_Token* this, Token* x);
void org_bau_List_List_i8_add_2(org_bau_List_List_i8* this, char x);
int64_t org_bau_Std_ord_1(i8_array* s);
_org_bau_List_List_Token_or_exception parse_1(i8_array* regex);
void test_0();
void i8_array_free(i8_array* x);
void int_array_free(int_array* x);
void org_bau_List_List_free(org_bau_List_List* x);
void org_bau_Exception_exception_free(org_bau_Exception_exception* x);
void Token_free(Token* x);
void Token_array_free(Token_array* x);
void org_bau_List_List_i8_free(org_bau_List_List_i8* x);
void org_bau_List_List_Token_free(org_bau_List_List_Token* x);
void i8_array_free(i8_array* x) {
    _free(x->data);
    _free(x);
}
void int_array_free(int_array* x) {
    _free(x->data);
    _free(x);
}
void org_bau_List_List_free(org_bau_List_List* x) {
    _free(x);
}
void org_bau_Exception_exception_free(org_bau_Exception_exception* x) {
    _decUse(x->message, i8_array, 0);
}
void Token_free(Token* x) {
    _decUse(x->data, org_bau_List_List_i8, 0);
    _free(x);
}
void Token_array_free(Token_array* x) {
    for (int i = 0; i < x->len; i++) _decUse(x->data[i], Token, 1);
    _free(x->data);
    _free(x);
}
void org_bau_List_List_i8_free(org_bau_List_List_i8* x) {
    _decUse(x->array, i8_array, 0);
    _free(x);
}
void org_bau_List_List_Token_free(org_bau_List_List_Token* x) {
    _decUse(x->array, Token_array, 0);
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
i8_array* string_1005;
i8_array* string_1006;
i8_array* string_1007;
i8_array* string_1008;
i8_array* string_1009;
i8_array* string_1010;
i8_array* string_1011;
i8_array* string_1012;
i8_array* string_1013;
i8_array* string_1014;
i8_array* string_1015;
i8_array* string_1016;
i8_array* string_1017;
i8_array* string_1018;
i8_array* string_1019;
i8_array* string_1020;
i8_array* string_1021;
i8_array* string_1022;
_int64_t_or_exception Token_matchChar_3(Token* this, i8_array* text, int64_t pos) {
    org_bau_Exception_exception _lastException;
    _int64_t_or_exception _x0;
    _int64_t_or_exception _x1;
    if (pos >= text->len) {
        return ok_int64_t_or_exception(0);
    }
    char c = text->data[pos];
    int64_t _t0 = this->type;
    if ((_t0 == 0) || (_t0 == 1)) {
        org_bau_Exception_exception _t1 = org_bau_Exception_exception_1(string_1000);
        _x0 = exception_int64_t_or_exception(_t1); _lastException = _x0.exception; goto catch0;
        org_bau_Exception_exception_free(&_t1);
    } else if (_t0 == 2) {
        int64_t _r0 = c == this->data->array->data[idx_2(0, this->data->array->len)];
        return ok_int64_t_or_exception(_r0);
    } else if (_t0 == 3) {
        while (1 == 1) {
            int64_t i = 0;
            while (i < this->data->size) {
                char min = this->data->array->data[idx_2(i, this->data->array->len)];
                char max = this->data->array->data[idx_2(i + 1, this->data->array->len)];
                int64_t _t2 = c >= min;
                if (_t2) {
                    int64_t _t3 = c <= max;
                    _t2 = _t3;
                }
                if (_t2) {
                    return ok_int64_t_or_exception(1);
                }
                i += 1;
                continue1:;
                i += 1;
            }
            break;
        }
        return ok_int64_t_or_exception(0);
    } else if (_t0 == 4) {
        while (1 == 1) {
            int64_t i = 0;
            while (i < this->data->size) {
                char min = this->data->array->data[idx_2(i, this->data->array->len)];
                char max = this->data->array->data[idx_2(i + 1, this->data->array->len)];
                int64_t _t4 = c >= min;
                if (_t4) {
                    int64_t _t5 = c <= max;
                    _t4 = _t5;
                }
                if (_t4) {
                    return ok_int64_t_or_exception(0);
                }
                i += 1;
                continue3:;
                i += 1;
            }
            break;
        }
        return ok_int64_t_or_exception(1);
    } else if (_t0 == 5) {
        return ok_int64_t_or_exception(1);
    } else {
        org_bau_Exception_exception _t6 = org_bau_Exception_exception_1(string_1001);
        _x1 = exception_int64_t_or_exception(_t6); _lastException = _x1.exception; goto catch0;
        org_bau_Exception_exception_free(&_t6);
    }
    catch0:
    return exception_int64_t_or_exception(_lastException);
}
_match_or_exception find_2(i8_array* text, i8_array* regex) {
    org_bau_Exception_exception _lastException;
    _org_bau_List_List_Token_or_exception _x0;
    _int64_t_or_exception _x1;
    _int64_t_or_exception _x2;
    _x0 = parse_1(regex);
    if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
    org_bau_List_List_Token* list = _x0.result;
    int64_t _t0 = list->size > 0;
    if (_t0) {
        int64_t _t1 = list->array->data[idx_2(0, list->array->len)]->type == 0;
        _t0 = _t1;
    }
    if (_t0) {
        _x1 = matchHere_4(list, 1, text, 0);
        if (_x1.exception.exceptionType != -1) { _lastException = _x1.exception; goto catch0; };
        int64_t end = _x1.result;
        if (end >= 0) {
            match _t2 = match_2(0, end);
            _decUse(list, org_bau_List_List_Token, 0);
            return ok_match_or_exception(_t2);
        }
        match _t3 = match_2(-1, -1);
        _decUse(list, org_bau_List_List_Token, 0);
        return ok_match_or_exception(_t3);
    }
    int64_t tp = 0;
    while (1) {
        _x2 = matchHere_4(list, 0, text, tp);
        if (_x2.exception.exceptionType != -1) { _lastException = _x2.exception; goto catch0; };
        int64_t end = _x2.result;
        if (end >= 0) {
            match _t4 = match_2(tp, end);
            _decUse(list, org_bau_List_List_Token, 0);
            return ok_match_or_exception(_t4);
        }
        if (tp < text->len) {
            break;
        }
        tp += 1;
    }
    match _t5 = match_2(-1, -1);
    _decUse(list, org_bau_List_List_Token, 0);
    return ok_match_or_exception(_t5);
    catch0:
    return exception_match_or_exception(_lastException);
}
int64_t idx_2(int64_t x, int64_t len) {
    if (x >= 0 && x < len) return x;
    fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\n", x, len);
    exit(1);
}
match match_2(int64_t start, int64_t end) {
    match m = match_new();
    m.start = start;
    m.end = end;
    return m;
}
_int64_t_or_exception matchHere_4(org_bau_List_List_Token* list, int64_t rp, i8_array* text, int64_t tp) {
    org_bau_Exception_exception _lastException;
    _int64_t_or_exception _x0;
    _int64_t_or_exception _x1;
    if (rp >= list->size) {
        return ok_int64_t_or_exception(tp);
    }
    Token* t = list->array->data[idx_2(rp, list->array->len)];
    _incUse(t, 0);
    if (t->type == 1) {
        if (tp == text->len) {
            _decUse(t, Token, 0);
            return ok_int64_t_or_exception(tp);
        }
        _decUse(t, Token, 0);
        return ok_int64_t_or_exception(-1);
    }
    while (1 == 1) {
        int64_t i = 0;
        while (1) {
            _x0 = Token_matchChar_3(t, text, tp);
            if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
            int64_t _t0 = _x0.result;
            if (!(_t0)) {
                _decUse(t, Token, 0);
                return ok_int64_t_or_exception(-1);
            }
            tp += 1;
            continue1:;
            int64_t _next = i + 1;
            if (_next >= t->min) {
                break;
            }
            i = _next;
        }
        break;
    }
    _x1 = matchStar_6(t, list, rp + 1, text, tp, t->max - t->min);
    if (_x1.exception.exceptionType != -1) { _lastException = _x1.exception; goto catch0; };
    int64_t _t1 = _x1.result;
    _decUse(t, Token, 0);
    return ok_int64_t_or_exception(_t1);
    catch0:
    return exception_int64_t_or_exception(_lastException);
}
_int64_t_or_exception matchStar_6(Token* t, org_bau_List_List_Token* list, int64_t rp, i8_array* text, int64_t tp, int64_t max) {
    org_bau_Exception_exception _lastException;
    _int64_t_or_exception _x0;
    _int64_t_or_exception _x1;
    int64_t result = -1;
    while (1) {
        _x0 = matchHere_4(list, rp, text, tp);
        if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
        int64_t end = _x0.result;
        if (end >= 0) {
            result = end;
        }
        _x1 = Token_matchChar_3(t, text, tp);
        if (_x1.exception.exceptionType != -1) { _lastException = _x1.exception; goto catch0; };
        int64_t r = _x1.result;
        tp += 1;
        if (r == 0) {
            break;
        }
        if (max <= 0) {
            break;
        }
        max -= 1;
    }
    return ok_int64_t_or_exception(result);
    catch0:
    return exception_int64_t_or_exception(_lastException);
}
_int64_t_or_exception matches_2(i8_array* text, i8_array* regex) {
    org_bau_Exception_exception _lastException;
    _match_or_exception _x0;
    _x0 = find_2(text, regex);
    if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
    match result = _x0.result;
    int64_t _t0 = result.start == 0;
    if (_t0) {
        int64_t _t1 = result.end == text->len;
        _t0 = _t1;
    }
    return ok_int64_t_or_exception(_t0);
    catch0:
    return exception_int64_t_or_exception(_lastException);
}
org_bau_Exception_exception org_bau_Exception_exception_1(i8_array* message) {
    org_bau_Exception_exception result = org_bau_Exception_exception_new();
    result.exceptionType = 0;
    _decUse(result.message, i8_array, 1);
    result.message = message;
    _incUse(result.message, 1);
    return result;
}
org_bau_List_List_Token* org_bau_List_newList_Token_1(int64_t _T) {
    org_bau_List_List_Token* result = org_bau_List_List_Token_new();
    _decUse(result->array, Token_array, 1);
    result->array = Token_array_new(4);
    result->size = 0;
    return result;
}
org_bau_List_List_i8* org_bau_List_newList_i8_1(int64_t _T) {
    org_bau_List_List_i8* result = org_bau_List_List_i8_new();
    _decUse(result->array, i8_array, 1);
    result->array = i8_array_new(4);
    result->size = 0;
    return result;
}
void org_bau_List_List_Token_add_2(org_bau_List_List_Token* this, Token* x) {
    if (this->size >= this->array->len) {
        Token_array* n = Token_array_new(this->array->len * 2);
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                _decUse(n->data[idx_2(i, n->len)], Token, 1);
                n->data[idx_2(i, n->len)] = this->array->data[i];
                _incUse(n->data[idx_2(i, n->len)], 1);
                continue1:;
                int64_t _next = i + 1;
                if (_next >= this->array->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
        _decUse(this->array, Token_array, 1);
        this->array = n;
        _incUse(this->array, 1);
        _decUse(n, Token_array, 0);
    }
    _decUse(this->array->data[idx_2(this->size, this->array->len)], Token, 1);
    this->array->data[idx_2(this->size, this->array->len)] = x;
    _incUse(this->array->data[idx_2(this->size, this->array->len)], 1);
    this->size += 1;
}
void org_bau_List_List_i8_add_2(org_bau_List_List_i8* this, char x) {
    if (this->size >= this->array->len) {
        i8_array* n = i8_array_new(this->array->len * 2);
        while (1 == 1) {
            int64_t i = 0;
            while (1) {
                n->data[idx_2(i, n->len)] = this->array->data[i];
                continue1:;
                int64_t _next = i + 1;
                if (_next >= this->array->len) {
                    break;
                }
                i = _next;
            }
            break;
        }
        _decUse(this->array, i8_array, 1);
        this->array = n;
        _incUse(this->array, 1);
        _decUse(n, i8_array, 0);
    }
    this->array->data[idx_2(this->size, this->array->len)] = x;
    this->size += 1;
}
int64_t org_bau_Std_ord_1(i8_array* s) {
    if (s->len) {
        char _r0 = s->data[idx_2(0, s->len)];
        return _r0;
    }
    return 0;
}
_org_bau_List_List_Token_or_exception parse_1(i8_array* regex) {
    org_bau_Exception_exception _lastException;
    _org_bau_List_List_Token_or_exception _x0;
    org_bau_List_List_Token* result = org_bau_List_newList_Token_1(0);
    int64_t i = 0;
    while (i < regex->len) {
        char c = regex->data[i];
        Token* t = Token_new();
        _decUse(t->data, org_bau_List_List_i8, 1);
        t->data = org_bau_List_newList_i8_1(0);
        t->min = 1;
        t->max = 1;
        if (c == 94) {
            t->type = 0;
            org_bau_List_List_Token_add_2(result, t);
            i += 1;
            continue;
        }
        if (c == 36) {
            t->type = 1;
            org_bau_List_List_Token_add_2(result, t);
            i += 1;
            continue;
        }
        if (c == 46) {
            t->type = 5;
        } else if (c == 92) {
            t->type = 2;
            i += 1;
            org_bau_List_List_i8_add_2(t->data, regex->data[i]);
        } else if (c == 91) {
            i += 1;
            c = regex->data[i];
            if (c == 94) {
                t->type = 4;
                i += 1;
                c = regex->data[i];
            } else {
                t->type = 3;
            }
            org_bau_List_List_i8_add_2(t->data, c);
            org_bau_List_List_i8_add_2(t->data, c);
            while (1) {
                i += 1;
                c = regex->data[i];
                if (c == 93) {
                    break;
                }
                if (c == 45) {
                    i += 1;
                    c = regex->data[i];
                    t->data->array->data[idx_2(t->data->size - 1, t->data->array->len)] = c;
                } else {
                    org_bau_List_List_i8_add_2(t->data, c);
                    org_bau_List_List_i8_add_2(t->data, c);
                }
            }
        } else {
            t->type = 2;
            org_bau_List_List_i8_add_2(t->data, c);
        }
        org_bau_List_List_Token_add_2(result, t);
        if (( i + 1 ) >= regex->len) {
            _decUse(t, Token, 0);
            break;
        }
        i += 1;
        c = regex->data[i];
        if (c == 43) {
            t->max = 9999;
        } else if (c == 42) {
            t->min = 0;
            t->max = 9999;
        } else if (c == 63) {
            t->min = 0;
            t->max = 1;
        } else if (c == 123) {
            i += 1;
            int64_t start = i;
            int64_t value = 0;
            while (i < regex->len) {
                c = regex->data[i];
                int64_t _t0 = c < 48;
                if (!(_t0)) {
                    int64_t _t1 = c > 57;
                    _t0 = _t1;
                }
                if (_t0) {
                    break;
                }
                value = ( value * 10 ) + c - 48;
                if (value > 9999) {
                    break;
                }
                i += 1;
            }
            t->min = value;
            t->max = t->min;
            if (c == 44) {
                i += 1;
                start = i;
                value = 0;
                while (i < regex->len) {
                    c = regex->data[i];
                    int64_t _t2 = c < 48;
                    if (!(_t2)) {
                        int64_t _t3 = c > 57;
                        _t2 = _t3;
                    }
                    if (_t2) {
                        break;
                    }
                    value = ( value * 10 ) + c - 48;
                    if (value > 9999) {
                        break;
                    }
                    i += 1;
                }
                t->max = value;
            }
            if (c != 125) {
                org_bau_Exception_exception _t4 = org_bau_Exception_exception_1(string_1017);
                _x0 = exception_org_bau_List_List_Token_or_exception(_t4); _lastException = _x0.exception; goto catch0;
                org_bau_Exception_exception_free(&_t4);
            }
        } else {
            i -= 1;
        }
        i += 1;
        continue0:;
        _decUse(t, Token, 0);
    }
    return ok_org_bau_List_List_Token_or_exception(result);
    catch0:
    return exception_org_bau_List_List_Token_or_exception(_lastException);
}
void test_0() {
    org_bau_Exception_exception _lastException;
    _int64_t_or_exception _x0;
    _int64_t_or_exception _x1;
    _x0 = matches_2(string_1018, string_1019);
    if (_x0.exception.exceptionType != -1) { _lastException = _x0.exception; goto catch0; };
    int64_t _t0 = _x0.result;
    printf("%lld\n", (long long)_t0);
    _x1 = matches_2(string_1020, string_1021);
    if (_x1.exception.exceptionType != -1) { _lastException = _x1.exception; goto catch0; };
    int64_t _t1 = _x1.result;
    printf("%lld\n", (long long)_t1);
    goto skip0;
    catch0:;
    org_bau_Exception_exception e = _lastException;
        printf("exception %.*s\n", e.message->len, e.message->data);
        org_bau_Exception_exception_free(&e);
    skip0:;
}
int main() {
    string_1000 = str_const("Start or end in wrong location", 30);
    string_1001 = str_const("Unknown token type", 18);
    string_1002 = str_const("^", 1);
    string_1003 = str_const("$", 1);
    string_1004 = str_const(".", 1);
    string_1005 = str_const("\\", 1);
    string_1006 = str_const("[", 1);
    string_1007 = str_const("]", 1);
    string_1008 = str_const("-", 1);
    string_1009 = str_const("+", 1);
    string_1010 = str_const("*", 1);
    string_1011 = str_const("?", 1);
    string_1012 = str_const("{", 1);
    string_1013 = str_const("0", 1);
    string_1014 = str_const("9", 1);
    string_1015 = str_const(",", 1);
    string_1016 = str_const("}", 1);
    string_1017 = str_const("Expected }", 10);
    string_1018 = str_const("hello", 5);
    string_1019 = str_const("hell.*", 6);
    string_1020 = str_const("hello world", 11);
    string_1021 = str_const("hell[o-z] world", 15);
    string_1022 = str_const("exception ", 10);
    test_0();
    _end();
    return 0;
}
/*

type List(T)
List

fun List(T) add(x T)
Add an entry to the list

fun List(T) get(x int) T
Add an entry to the list

type exception
An exception

fun exception(message i8[]) exception
 Create a new exception with a message

fun ord(s i8[]) const int
The value of the first byte in the string. 0 if the string is empty.

*/
