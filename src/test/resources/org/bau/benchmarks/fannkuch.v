// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import os

fn main() {
    mut n := 4
    if os.args.len > 1 {
        n = os.args[1].int()
    }
    println('Pfannkuchen(${n}) = ${fannkuch(n)}')
}

fn fannkuch(n int) int {
    mut perm1 := []int{len: n}
    for i in 0 .. n {
        perm1[i] = i
    }
    mut perm := []int{len: n}
    mut count := []int{len: n}
    mut flips := 0
    mut nperm := 0
    mut checksum := 0
    mut r := n
    for r > 0 {
        for r > 1 {
            count[r - 1] = r
            r--
        }
        for i in 0 .. n {
            perm[i] = perm1[i]
        }
        // Count flips and update max and checksum
        mut f := 0
        mut k := perm[0]
        for k != 0 {
            for i in 0 .. (k + 1) / 2 {
                t := perm[i]
                perm[i] = perm[k - i]
                perm[k - i] = t
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
                println(checksum)
                return flips
            }
            p0 := perm1[0]
            for i := 0; i < r; i++ {
                perm1[i] = perm1[i + 1]
            }
            perm1[r] = p0
            count[r]--
            if count[r] > 0 {
                break
            }
            r++
        }
        nperm++
    }
    return flips
}
