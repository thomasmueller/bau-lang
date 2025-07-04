// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package main

import (
    "flag"
    "fmt"
    "runtime"
    "strconv"
)

func main() {
    runtime.GOMAXPROCS(1)
    flag.Parse()
    n := 4
    if flag.NArg() > 0 { n,_ = strconv.Atoi(flag.Arg(0)) }
    fmt.Printf("Pfannkuchen(%d) = %d\n", n, fannkuch(n))
}

func fannkuch(n int) int {
    perm1 := make([]int, n)
    for i := 0; i < n; i++ {
        perm1[i] = i
    }
    perm := make([]int, n)
    count := make([]int, n)
    f, i, k, r, flips, nperm, checksum := 0, 0, 0, 0, 0, 0, 0
    r = n
    for r > 0 {
        for r > 1 {
            count[r - 1] = r
            r--
        }
        i = 0
        for i < n {
            perm[i] = perm1[i]
            i++
        }
        // Count flips and update max and checksum
        f = 0
        k = perm[0]
        for k != 0 {
            i = 0
            for 2 * i < k {
                t := perm[i]
                perm[i] = perm[k - i]
                perm[k - i] = t
                i++
            }
            k = perm[0]
            f++
        }
        if f > flips {
            flips = f
        }
        if (nperm & 1) == 0 {
            checksum += f
        } else {
            checksum -= f
        }
        // Use incremental change to generate another permutation
        for {
             if r == n {
                fmt.Println(checksum)
                return flips
             }
             p0 := perm1[0]
             i = 0
             for i < r {
                j := i + 1
                perm1[i] = perm1[j]
                i = j
            }
            perm1[r] = p0
            count[r]--
            if count[r] > 0 {
                break;
            }
            r++
        }
        nperm++
    }
    return flips
}

