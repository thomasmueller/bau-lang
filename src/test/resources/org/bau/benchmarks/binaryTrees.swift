// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import Foundation

class Tree {
    let left: Tree?
    let right: Tree?

    init(left: Tree?, right: Tree?) {
        self.left = left
        self.right = right
    }

    static func buildTree(_ depth: Int) -> Tree {
        if depth == 0 {
            return Tree(left: nil, right: nil)
        }
        return Tree(left: buildTree(depth - 1), right: buildTree(depth - 1))
    }

    func nodeCount() -> Int {
        var result = 1
        if let left = left {
            result += left.nodeCount()
        }
        if let right = right {
            result += right.nodeCount()
        }
        return result
    }
}

func stretch(_ depth: Int) {
    print("stretch tree of depth \(depth)\t check: \(count(depth))")
}

func count(_ depth: Int) -> Int {
    return Tree.buildTree(depth).nodeCount()
}

func main() {
    var n = 10
    if CommandLine.arguments.count > 1, let arg = Int(CommandLine.arguments[1]) {
        n = arg
    }
    let minDepth = 4
    let maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n
    let stretchDepth = maxDepth + 1
    stretch(stretchDepth)
    let longLivedTree = Tree.buildTree(maxDepth)
    for depth in stride(from: minDepth, through: maxDepth, by: 2) {
        let iterations = 1 << (maxDepth - depth + minDepth)
        var sum = 0
        for _ in 1...iterations {
            sum += count(depth)
        }
        print("\(iterations)\t trees of depth \(depth)\t check: \(sum)")
    }
    let count = longLivedTree.nodeCount()
    print("long lived tree of depth \(maxDepth)\t check: \(count)")
}

main()