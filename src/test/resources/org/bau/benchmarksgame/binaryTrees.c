// https://benchmarksgame-team.pages.debian.net/benchmarksgame/program/binarytrees-gcc-1.html

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
        return NewTreeNode(
            BottomUpTree(depth - 1),
            BottomUpTree(depth - 1)
        );
    else
        return NewTreeNode(NULL, NULL);
}

void DeleteTree(treeNode* tree) {
    if (tree->left != NULL) {
        DeleteTree(tree->left);
    }
    if (tree->right != NULL) {
        DeleteTree(tree->right);
    }
    free(tree);
}

int main(int argc, char* argv[]) {
    unsigned   N, depth, minDepth, maxDepth, stretchDepth;
    treeNode   *stretchTree, *longLivedTree, *tempTree;
    N = argc > 1 ? atol(argv[1]) : 20;
    minDepth = 4;
    if ((minDepth + 2) > N)
        maxDepth = minDepth + 2;
    else
        maxDepth = N;
    stretchDepth = maxDepth + 1;
    stretchTree = BottomUpTree(stretchDepth);
    printf(
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
        printf(
            "%li\t trees of depth %u\t check: %li\n",
            iterations,
            depth,
            check
        );
    }
    printf(
        "long lived tree of depth %u\t check: %li\n",
        maxDepth,
        ItemCheck(longLivedTree)
    );
    return 0;
}
