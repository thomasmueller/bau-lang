/* The Computer Language Benchmarks Game
 * https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
 */
#include <stdio.h>
#include <stdlib.h>

int fannkuch(int n);

int main(int argc, char *argv[]) {
    int n = argc > 1 ? atoi(argv[1]) : 12;
    printf("Pfannkuchen(%d) = %d\n", n, fannkuch(n));
    return 0;
}

int fannkuch(int n) {
    int perm[n];
    int perm1[n];
    int count[n];
    int flips = 0;
    int permCount = 0;
    int checksum = 0;
    int i;
    for (i = 0; i < n; i += 1) {
        perm1[i] = i;
    }
    int r = n;
    while (1) {
        while (r != 1) {
            count[r - 1] = r;
            r -= 1;
        }
        for (i = 0; i < n; i += 1) {
            perm[i] = perm1[i];
        }
        int f = 0;
        int k;
        while ( !((k = perm[0]) == 0) ) {
            int k2 = (k + 1) >> 1;
            for (i = 0; i< k2; i++) {
                int temp = perm[i]; 
                perm[i] = perm[k - i]; 
                perm[k - i] = temp;
            }
            f += 1;
        }
        if (f > flips) { 
            flips = f; 
        }
        if ((permCount & 1) == 0) { 
            checksum += f;
        } else { 
            checksum -= f;
        }
        /* Use incremental change to generate another permutation */
        while (1) {
            if (r == n) {
                printf("%d\n", checksum);
                return flips;
            }
            int perm0 = perm1[0];
            i = 0;
            while (i < r) {
                int j = i + 1;
                perm1[i] = perm1[j];
                i = j;
            }
            perm1[r] = perm0;
            count[r] = count[r] - 1;
            if (count[r] > 0) {
                break;
            }
            r++;
        }
        permCount++;
    }
}
