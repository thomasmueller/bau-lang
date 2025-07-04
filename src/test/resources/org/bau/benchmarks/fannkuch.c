// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

#include <stdio.h>
#include <stdlib.h>

int fannkuch(int n) {
    int perm1[n];
    for (int i = 0; i < n; i++)
        perm1[i] = i;
    int perm[n];
    int count[n];
    int flips = 0, nperm = 0;
    int checksum = 0, k = 0, i = 0;
    int r = n;
    while (r > 0) {
        while (r > 1) {
            count[r - 1] = r;
            r--;
        }
        for (i = 0; i < n; i++)
            perm[i] = perm1[i];
        // Count flips and update max and checksum
        int f = 0;
        int k = perm[0];
        while (k != 0) {
            for (i = 0; 2 * i < k; i++) {
                int t = perm[i];
                perm[i] = perm[k - i];
                perm[k - i] = t;
            }
            k = perm[0];
            f++;
        }
        if (f > flips)
            flips = f;
        if ((nperm & 1) == 0)
            checksum += f;
        else
            checksum -= f;
        // Use incremental change to generate another permutation
        while (1) {
            if (r == n) {
                printf("%d\n", checksum);
                return flips;
            }
            int p0 = perm1[0];
            i = 0;
            while (i < r) {
                int j = i + 1;
                perm1[i] = perm1[j];
                i = j;
            }
            perm1[r] = p0;
            count[r]--;
            if (count[r] > 0)
                break;
            r++;
        }
        nperm++;
    }
    return flips;
}

int main(int argc, char *argv[]) {
    int n = argc > 1 ? atoi(argv[1]) : 4;
    printf("Pfannkuchen(%d) = %d\n", n, fannkuch(n));
    return 0;
}
