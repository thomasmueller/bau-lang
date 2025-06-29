// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks

import java.io.BufferedOutputStream
import java.io.OutputStream

fun main(args: Array<String>) {
    val w: Int
    val h: Int
    var bitNum = 0
    val iter = 50
    val limit = 2.0
    var byteAcc: Byte = 0
    var n = 200
    if (args.isNotEmpty())
        n = args[0].toInt()
    h = n
    w = h
    val out: OutputStream = BufferedOutputStream(System.out)
    out.write("P4\n$w $h\n".toByteArray())
    for (y in 0 until h) {
        for (x in 0 until w) {
            var Zr = 0.0
            var Zi = 0.0
            var Tr = 0.0
            var Ti = 0.0
            val Cr = (2.0 * x / w - 1.5)
            val Ci = (2.0 * y / h - 1.0)
            var i = 0
            while (i < iter && Tr + Ti <= limit * limit) {
                Zi = 2.0 * Zr * Zi + Ci
                Zr = Tr - Ti + Cr
                Tr = Zr * Zr
                Ti = Zi * Zi
                i++
            }
            byteAcc = (byteAcc.toInt() shl 1).toByte()
            if (Tr + Ti <= limit * limit)
                byteAcc = (byteAcc.toInt() or 0x01).toByte()
            bitNum++
            if (bitNum == 8) {
                out.write(byteAcc.toInt())
                byteAcc = 0
                bitNum = 0
            } else if (x == w - 1) {
                byteAcc = (byteAcc.toInt() shl (8 - (w % 8))).toByte()
                out.write(byteAcc.toInt())
                byteAcc = 0
                bitNum = 0
            }
        }
    }
    out.flush()
}