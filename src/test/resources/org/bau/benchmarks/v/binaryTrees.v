// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import os

struct Node {
    left  ?&Node
    right ?&Node
}

fn main() {
    mut n := 10
    if os.args.len > 1 {
        n = os.args[1].int()
    }
    min_depth := 4
    mut max_depth := n
    if min_depth + 2 > n {
        max_depth = min_depth + 2
    }
    stretch_depth := max_depth + 1
    stretch(stretch_depth)
    long_lived := build_tree(max_depth)
    for depth := min_depth; depth <= max_depth; depth += 2 {
        iterations := 1 << (max_depth - depth + min_depth)
        mut sum := 0
        for _ in 0 .. iterations {
            sum += count(depth)
        }
        println('${iterations}\t trees of depth ${depth}\t check: ${sum}')
    }
    println('long lived tree of depth ${max_depth}\t check: ${long_lived.node_count()}')
}

fn stretch(depth int) {
    println('stretch tree of depth ${depth}\t check: ${count(depth)}')
}

fn count(depth int) int {
    return build_tree(depth).node_count()
}

fn build_tree(depth int) &Node {
    if depth == 0 {
        return &Node{}
    }
    return &Node{
        left: build_tree(depth - 1)
        right: build_tree(depth - 1)
    }
}

fn (n &Node) node_count() int {
    mut result := 1
    if n.left != none {
        result += n.left.node_count()
    }
    if n.right != none {
        result += n.right.node_count()
    }
    return result
}
