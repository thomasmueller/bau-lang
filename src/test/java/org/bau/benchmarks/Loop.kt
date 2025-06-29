package org.bau.benchmarks

import java.util.*

fun main(args: Array<String>) {
    require(args.size >= 1)
    val main = Class.forName(args[0]).getMethod("main", Array<String>::class.java)
    val a2 = Arrays.copyOfRange(args, 1, args.size)
    for (i in 0..2) {
        // Just an attempt to collect garbage
        System.gc()
        val start = System.currentTimeMillis()
        main.invoke(null, a2 as Any)
        val time = System.currentTimeMillis() - start
        System.out.printf("\nRun #%d: %3.3f s\n", i, time / 1000.0)
        System.gc()
    }
}