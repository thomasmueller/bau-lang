// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks

fun main(args: Array<String>) {
    var n = 10
    if (args.isNotEmpty())
        n = args[0].toInt()
    val minDepth = 4
    val maxDepth = if (minDepth + 2 > n) minDepth + 2 else n
    val stretchDepth = maxDepth + 1
    stretch(stretchDepth)
    val longLivedTree = BinaryTrees.Tree.buildTree(maxDepth)
    var depth = minDepth
    while (depth <= maxDepth) {
        val iterations = 1 shl (maxDepth - depth + minDepth)
        var sum = 0
        for (i in 1..iterations) sum += count(depth)
        println("$iterations\t trees of depth $depth\t check: $sum")
        depth += 2
    }
    val count = longLivedTree.nodeCount()
    println("long lived tree of depth $maxDepth\t check: $count")
}

fun stretch(depth: Int) {
    println("stretch tree of depth " + depth + "\t check: " + count(depth))
}

fun count(depth: Int): Int {
    return BinaryTrees.Tree.buildTree(depth).nodeCount()
}

object BinaryTrees {
    internal class Tree(var left: Tree?, var right: Tree?) {
        fun nodeCount(): Int {
            var result = 1
            if (left != null) result += left!!.nodeCount()
            if (right != null) result += right!!.nodeCount()
            return result
        }

        companion object {
            fun buildTree(depth: Int): Tree {
                if (depth == 0) return Tree(null, null)
                return Tree(buildTree(depth - 1), buildTree(depth - 1))
            }
        }
    }
}