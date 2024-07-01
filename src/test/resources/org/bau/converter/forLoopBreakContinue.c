#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#define _incUse(a) if(a){(a)->_refCount++;}
#define _decUse(a, type) if(a){if(--((a)->_refCount) == 0) type##_free(a);}
#define _malloc(a) malloc(a)
#define _traceMalloc(a) ;
#define _free(a) free(a)
#define _end() ;
int main() {
    while (1 == 1) {
        int64_t a = 0;
        while (a < 10) {
            if (a == 5) {
                break;
            }
            if (a == 2) {
                goto continue2;
            }
            printf("%lld\n", a);
            continue2:;
            a += 1;
        }
        break;
    }
    _end();
    return 0;
}
