// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package main

import (
    "flag"
    "fmt"
    "strconv"
)

func main() {
    n := 10
    flag.Parse()
    if flag.NArg() > 0 {
        n, _ = strconv.Atoi(flag.Arg(0))
    }
    minDepth := 4
    maxDepth := n
    if minDepth + 2 > n {
        maxDepth = minDepth + 2
    }
    stretchDepth := maxDepth + 1
    stretch(stretchDepth)
    longLived := buildTree(maxDepth)
    for depth := minDepth; depth <= maxDepth; depth += 2 {
        iterations := 1 << uint(maxDepth - depth + minDepth)
        sum := 0
        for i := 0; i < iterations; i++ {
            sum += count(depth)
        }
        fmt.Printf("%d\t trees of depth %d\t check: %d\n", iterations, depth, sum)
    }
    fmt.Printf("long lived tree of depth %d\t check: %d\n", maxDepth, longLived.nodeCount())
}

func stretch(depth int) {
    fmt.Printf("stretch tree of depth %d\t check: %d\n", depth, count(depth))
}

func count(depth int) int {
    return buildTree(depth).nodeCount()
}

type Node struct {
    left, right *Node
}

func buildTree(depth int) *Node {
    if depth <= 0 {
        return &Node{}
    }
    return &Node{ buildTree(depth-1), buildTree(depth-1) }
}

func (n *Node) nodeCount() int {
    result := 1
    if n.left != nil {
        result += n.left.nodeCount()
    }
    if n.right != nil {
        result += n.right.nodeCount()
    }
    return result
}
