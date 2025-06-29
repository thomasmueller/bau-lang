// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks

fun main(args: Array<String>) {
    val n = if (args.isNotEmpty())
        args[0].toInt()
    else
        4
    println("Pfannkuchen(" + n + ") = " + fannkuch(n))
}

fun fannkuch(n: Int): Int {
    val perm1 = IntArray(n)
    for (i in 0 until n) perm1[i] = i
    val perm = IntArray(n)
    val count = IntArray(n)
    var f = 0
    var flips = 0
    var nperm = 0
    var checksum = 0
    var i: Int
    var k: Int
    var r = n
    while (r > 0) {
        i = 0
        while (r != 1) {
            count[r - 1] = r
            r--
        }
        while (i < n) {
            perm[i] = perm1[i]
            i++
        }
        // Count flips and update max and checksum
        f = 0
        k = perm[0]
        while (k != 0) {
            i = 0
            while (2 * i < k) {
                val t = perm[i]
                perm[i] = perm[k - i]
                perm[k - i] = t
                i++
            }
            k = perm[0]
            f++
        }
        if (f > flips)
            flips = f
        if ((nperm and 1) == 0)
            checksum += f
        else
            checksum -= f
        // Use incremental change to generate another permutation
        var more = true
        while (more) {
            if (r == n) {
                println(checksum)
                return flips
            }
            val p0 = perm1[0]
            i = 0
            while (i < r) {
                val j = i + 1
                perm1[i] = perm1[j]
                i = j
            }
            perm1[r] = p0
            count[r]--
            if (count[r] > 0)
                more = false
            else
                r++
        }
        nperm++
    }
    return flips
}
