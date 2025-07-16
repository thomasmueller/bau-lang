// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/


#include <stdio.h>
#include <stdlib.h>
#include <gmp.h>

mpz_t ten, acc, den, num;

void extractDigit(int nth, int *result) {
    mpz_t t1, t2;
    mpz_inits(t1, t2, NULL);
    mpz_mul_si(t1, num, nth);
    mpz_add(t2, t1, acc);
    mpz_fdiv_q(t2, t2, den);
    *result = mpz_get_si(t2);
    mpz_clears(t1, t2, NULL);
}

void eliminateDigit(int d) {
    mpz_t t1, t2;
    mpz_inits(t1, t2, NULL);
    mpz_mul_si(t1, den, d);
    mpz_sub(acc, acc, t1);
    mpz_mul(acc, acc, ten);
    mpz_mul(num, num, ten);
    mpz_clears(t1, t2, NULL);
}

void nextTerm(int k) {
    mpz_t k2p1, t;
    mpz_inits(k2p1, t, NULL);
    mpz_mul_ui(t, num, 2);
    mpz_add(acc, acc, t);
    mpz_set_ui(k2p1, 2 * k + 1);
    mpz_mul(acc, acc, k2p1);
    mpz_mul(den, den, k2p1);
    mpz_mul_ui(num, num, k);
    mpz_clears(k2p1, t, NULL);
}

int main(int argc, char *argv[]) {
    int n = 10000;
    if (argc > 1)
        n = atoi(argv[1]);
    int i = 0;
    int k = 0;
    mpz_inits(ten, acc, den, num, NULL);
    mpz_set_ui(ten, 10);
    mpz_set_ui(acc, 0);
    mpz_set_ui(den, 1);
    mpz_set_ui(num, 1);
    while (i < n) {
        k++;
        nextTerm(k);
        if (mpz_cmp(num, acc) > 0)
            continue;
        int d3, d4;
        extractDigit(3, &d3);
        extractDigit(4, &d4);
        if (d3 != d4)
            continue;
        eliminateDigit(d3);
        putchar('0' + d3);
        i++;
        if (i % 10 == 0)
            printf(" : %d\n", i);
    }
    return 0;
}
