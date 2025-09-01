# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import os, strutils, std/strformat

type
    Node = ref object
        left, right: Node

proc buildTree(depth: int): Node =
    if depth == 0:
        new result
    else:
        result = Node(left: buildTree(depth - 1), right: buildTree(depth - 1))

proc nodeCount(n: Node): int =
    if n == nil:
        return 0
    else:
        return 1 + nodeCount(n.left) + nodeCount(n.right)

proc count(depth: int): int =
    result = nodeCount(buildTree(depth))

proc stretch(depth: int) =
    let c = count(depth)
    echo &"stretch tree of depth {depth}\t check: {c}"

proc main() =
    let args = commandLineParams()
    var n = 10
    if args.len > 0:
        n = parseInt(args[0])
    const minDepth = 4
    var maxDepth = n
    if minDepth + 2 > n:
        maxDepth = minDepth + 2
    let stretchDepth = maxDepth + 1
    stretch(stretchDepth)
    let longLived = buildTree(maxDepth)
    for depth in countup(minDepth, maxDepth, 2):
        let iterations = 1 shl (maxDepth - depth + minDepth)
        var sum = 0
        for i in 0 ..< iterations:
            sum += count(depth)
        echo &"{iterations}\t trees of depth {depth}\t check: {sum}"
    echo &"long lived tree of depth {maxDepth}\t check: {nodeCount(longLived)}"

when isMainModule:
    main()
