// https://benchmarksgame-team.pages.debian.net/benchmarksgame/program/binarytrees-gcc-1.html

#include <math.h>
#include <stdio.h>
#include <stdlib.h>

typedef struct tn {
    struct tn* left;
    struct tn* right;
} treeNode;

treeNode* buildTree(unsigned depth) {
    treeNode* new = (treeNode*) malloc(sizeof(treeNode));
    if (depth <= 0) {
        new->left = NULL;
        new->right = NULL;
        return new;
    }
    new->left = buildTree(depth - 1);
    new->right = buildTree(depth - 1);
    return new;
}

long nodeCount(treeNode* tree) {
    long result = 1;
    if (tree->left != NULL) {
        result += nodeCount(tree->left);
    }
    if (tree->right != NULL) {
        result += nodeCount(tree->right);
    }
    return result;
}

void clear(treeNode* tree) {
    if (tree->left != NULL) {
        clear(tree->left);
    }
    if (tree->right != NULL) {
        clear(tree->right);
    }
    free(tree);
}

long count(int depth) {
    treeNode *t = buildTree(depth);
    int c = nodeCount(t);
    clear(t);
    return c;
}

void stretch(int depth) {
    printf(
        "stretch tree of depth %u\t check: %li\n",
        depth, count(depth)
    );
}

int main(int argc, char* argv[]) {
    int n = argc > 1 ? atol(argv[1]) : 20;
    int minDepth = 4;
    int maxDepth = n;
    if (minDepth + 2 > n)
        maxDepth = minDepth + 2;
    int stretchDepth = maxDepth + 1;
    stretch(stretchDepth);
    treeNode *longLivedTree = buildTree(maxDepth);
    for (int depth = minDepth; depth <= maxDepth; depth += 2) {
        long i, iterations, check;
        iterations = pow(2, maxDepth - depth + minDepth);
        check = 0;
        for (i = 1; i <= iterations; i++) {
            check += count(depth);
        }
        printf("%li\t trees of depth %u\t check: %li\n",
            iterations, depth, check
        );
    }
    printf("long lived tree of depth %u\t check: %li\n",
        maxDepth, nodeCount(longLivedTree)
    );
    clear(longLivedTree);
    return 0;
}
