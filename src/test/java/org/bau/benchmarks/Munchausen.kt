// https://github.com/jabbalaci/SpeedTests

package org.bau.benchmarks

import kotlin.math.pow

var LIMIT: Int = 440000000
var cache: IntArray = getCache2()

fun main(args: Array<String>) {
    for (i in 0 until LIMIT)
        if (isMunchausen(i))
            println(i)
}

fun isMunchausen(number: Int): Boolean {
    var n = number
    var total = 0
    while (n > 0) {
        val digit = n % 10
        total += cache[digit]
        if (total > number)
            return false
        n /= 10
    }
    return total == number
}

fun getCache2(): IntArray {
    val cache = IntArray(10)
    for (i in 1..9)
        cache[i] = i.toDouble().pow(i.toDouble()).toInt()
    return cache
}