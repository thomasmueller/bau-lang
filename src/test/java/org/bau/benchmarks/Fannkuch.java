// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks;

public class Fannkuch {

    public static void main(String[] args) {
        int n = (args.length > 0) ? Integer.parseInt(args[0]) : 4;
        System.out.println("Pfannkuchen(" + n + ") = " + fannkuch(n));
    }

    static int fannkuch(int n) {
        var perm1 = new int[n];
        for (int i = 0; i < n; i++)
            perm1[i] = i;
        var perm = new int[n];
        var count = new int[n];
        int f = 0, flips = 0, nperm = 0, checksum = 0;
        int i, k, r;
        r = n;
        while (r > 0) {
            i = 0;
            while (r != 1) {
                count[r - 1] = r;
                r--;
            }
            while (i < n) {
                perm[i] = perm1[i];
                i++;
            }
            // Count flips and update max and checksum
            f = 0;
            k = perm[0];
            while (k != 0) {
                i = 0;
                while (2 * i < k) {
                    int t = perm[i];
                    perm[i] = perm[k - i];
                    perm[k - i] = t;
                    i++;
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
            var more = true;
            while (more) {
                if (r == n) {
                    System.out.println(checksum);
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
                    more = false;
                else
                    r++;
            }
            nperm++;
        }
        return flips;
    }

}