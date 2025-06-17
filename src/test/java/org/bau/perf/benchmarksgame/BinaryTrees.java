package org.bau.perf.benchmarksgame;

// https://benchmarksgame-team.pages.debian.net/benchmarksgame/program/binarytrees-java-8.html

/* The Computer Language Benchmarks Game
https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

contributed by Isaac Gouy
*/
public class BinaryTrees {

    public static void main(String[] args) {
        int n = 10;
        if (args.length > 0)
            n = Integer.parseInt(args[0]);

        int minDepth = 4;
        int maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n;
        int stretchDepth = maxDepth + 1;

        stretch(stretchDepth);
        var longLivedTree = Tree.with(maxDepth);

        for (int depth = minDepth; depth <= maxDepth; depth += 2) {
            int iterations = 1 << (maxDepth - depth + minDepth);
            int sum = 0;
            for (int i = 1; i <= iterations; i++) {
                sum += count(depth);
            }
            System.out.println(iterations + "\t trees of depth " + depth + "\t check: " + sum);
        }
        int count = longLivedTree.nodeCount();
        longLivedTree.clear();
        System.out.println("long lived tree of depth " + maxDepth + "\t check: " + count);
    }

    static void stretch(int depth) {
        System.out.println("stretch tree of depth " + depth + "\t check: " + count(depth));
    }

    static int count(int depth) {
        var t = Tree.with(depth);
        int c = t.nodeCount();
        t.clear();
        return c;
    }

    final static class Tree {
        Tree left, right;

        Tree(Tree left, Tree right) {
            this.left = left;
            this.right = right;
        }

        static Tree with(int depth) {
            return (depth == 0) ?
                new Tree(null, null) :
                new Tree(with(depth - 1), with(depth - 1));
        }

        int nodeCount() {
            int result = 1;
            if (left != null) {
                result += left.nodeCount();
            }
            if (right != null) {
                result += right.nodeCount();
            }
            return result;
        }

        void clear() {
            if (left != null) {
                left.clear();
                left = null;
                right.clear();
                right = null;
            }
        }
    }

    /*

https://benchmarksgame-team.pages.debian.net/benchmarksgame/program/binarytrees-gcc-1.html

#include <malloc.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>

typedef struct tn {
    struct tn*    left;
    struct tn*    right;
} treeNode;

treeNode* NewTreeNode(treeNode* left, treeNode* right) {
    treeNode*    new;
    new = (treeNode*)malloc(sizeof(treeNode));
    new->left = left;
    new->right = right;
    return new;
}

long ItemCheck(treeNode* tree) {
    if (tree->left == NULL)
        return 1;
    else
        return 1 + ItemCheck(tree->left) + ItemCheck(tree->right);
}

treeNode* BottomUpTree(unsigned depth) {
    if (depth > 0)
        return NewTreeNode
        (
            BottomUpTree(depth - 1),
            BottomUpTree(depth - 1)
        );
    else
        return NewTreeNode(NULL, NULL);
}

void DeleteTree(treeNode* tree) {
    if (tree->left != NULL) {
        DeleteTree(tree->left);
        DeleteTree(tree->right);
    }
    free(tree);
}

int main(int argc, char* argv[]) {
    unsigned   N, depth, minDepth, maxDepth, stretchDepth;
    treeNode   *stretchTree, *longLivedTree, *tempTree;
    N = atol(argv[1]);
    minDepth = 4;
    if ((minDepth + 2) > N)
        maxDepth = minDepth + 2;
    else
        maxDepth = N;
    stretchDepth = maxDepth + 1;
    stretchTree = BottomUpTree(stretchDepth);
    printf
    (
        "stretch tree of depth %u\t check: %li\n",
        stretchDepth,
        ItemCheck(stretchTree)
    );
    DeleteTree(stretchTree);
    longLivedTree = BottomUpTree(maxDepth);
    for (depth = minDepth; depth <= maxDepth; depth += 2) {
        long    i, iterations, check;
        iterations = pow(2, maxDepth - depth + minDepth);
        check = 0;
        for (i = 1; i <= iterations; i++) {
            tempTree = BottomUpTree(depth);
            check += ItemCheck(tempTree);
            DeleteTree(tempTree);
        }
        printf
        (
            "%li\t trees of depth %u\t check: %li\n",
            iterations,
            depth,
            check
        );
    }
    printf
    (
        "long lived tree of depth %u\t check: %li\n",
        maxDepth,
        ItemCheck(longLivedTree)
    );
    return 0;
}

     */
}
