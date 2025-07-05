// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks;

public class BinaryTrees {

    public static void main(String[] args) {
        int n = 10;
        if (args.length > 0)
            n = Integer.parseInt(args[0]);
        int minDepth = 4;
        int maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n;
        int stretchDepth = maxDepth + 1;
        stretch(stretchDepth);
        var longLived = Tree.buildTree(maxDepth);
        for (int depth = minDepth; depth <= maxDepth; depth += 2) {
            int iterations = 1 << (maxDepth - depth + minDepth);
            int sum = 0;
            for (int i = 0; i < iterations; i++)
                sum += count(depth);
            System.out.println(iterations + "\t trees of depth " + depth + "\t check: " + sum);
        }
        System.out.println("long lived tree of depth " + maxDepth + "\t check: " + longLived.nodeCount());
    }

    static void stretch(int depth) {
        System.out.println("stretch tree of depth " + depth + "\t check: " + count(depth));
    }

    static int count(int depth) {
        return Tree.buildTree(depth).nodeCount();
    }

    static class Tree {
        Tree left, right;

        Tree(Tree left, Tree right) {
            this.left = left;
            this.right = right;
        }

        static Tree buildTree(int depth) {
            if (depth == 0)
                return new Tree(null, null);
            return new Tree(buildTree(depth - 1), buildTree(depth - 1));
        }

        int nodeCount() {
            int result = 1;
            if (left != null)
                result += left.nodeCount();
            if (right != null)
                result += right.nodeCount();
            return result;
        }

    }

}
