// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks

import java.math.BigInteger

fun main(args: Array<String>) {
    var n = 10000
    if (args.isNotEmpty())
        n = args[0].toInt()
    var k = 0
    var i = 0
    while (i < n) {
        k++
        nextTerm(k)
        if (num > acc)
            continue
        val d = extractDigit(3)
        if (d != extractDigit(4))
            continue
        eliminateDigit(d)
        print(('0'.code + d).toChar())
        i++
        if (i % 10 == 0)
            println(" : $i")
    }
}

var ten: BigInteger = BigInteger.TEN
var acc: BigInteger = BigInteger.ZERO
var den: BigInteger = BigInteger.ONE
var num: BigInteger = BigInteger.ONE

fun extractDigit(nth: Int): Int {
    return num.multiply(BigInteger.valueOf(nth.toLong())).add(acc).divide(den).toInt()
}

fun eliminateDigit(d: Int) {
    acc = acc.subtract(den.multiply(BigInteger.valueOf(d.toLong())))
    acc = acc.multiply(ten)
    num = num.multiply(ten)
}

fun nextTerm(k: Int) {
    acc = acc.add(num.shiftLeft(1))
    val k2p1 = BigInteger.valueOf((k * 2 + 1).toLong())
    acc = acc.multiply(k2p1)
    den = den.multiply(k2p1)
    num = num.multiply(BigInteger.valueOf(k.toLong()))
}
