#include <stdio.h>

#define LINES 256
#define LINE_LEN 32

/* 32 KB memory buffer */
unsigned char memory[16 * 1024];

int main(void) {
    unsigned short i;
    unsigned char mask, c;
    unsigned short line, p;

    while (1) {
        i = 0;
        mask = 0xFF;
        c = 0;

        /* Read one input line into memory[0..31] */
        while (i < LINE_LEN) {
            if (mask != 0) {
                c = (unsigned char)getchar();
            }
            memory[i++] = (unsigned char)(c & mask);

            if (c == '\n') {
                mask = 0;
            }
        }

        /* Parse leading number */
        line = 0;
        i = 0;

        while (1) {
            c = memory[i++];
            if (c < '0' || c > '9') {
                break;
            }
            line = (unsigned short)(line * 10 + (c - '0'));
        }

        /* Store line */
        if (line > 0 && line <= LINES) {
            p = (unsigned short)(line * LINE_LEN);
            for (i = 0; i < LINE_LEN; i++) {
                memory[p++] = memory[i];
            }
        } else {
            /* Commands */
            if (memory[0] == 'r') {
                /* run(); intentionally omitted */
                puts("run...");
            } 
            else if (memory[0] == 'l') {
                for (i = LINE_LEN; i < LINE_LEN * LINES; i++) {
                    c = memory[i];
                    if (c != 0) {
                        putchar(c);
                    }
                }
            } 
            else if (memory[0] == 'n') {
                for (i = 0; i < LINE_LEN * LINES; i++) {
                    memory[i] = 0;
                }
            } 
            else if (memory[0] == 'e') {
                break;
            }
        }
    }
    return 0;
}