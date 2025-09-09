// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

#include <math.h>
#include <stdio.h>
#include <stdlib.h>

typedef struct tn treeNode;
void stretch(int depth);
int count(int depth);
treeNode* buildTree(int depth);
int nodeCount(treeNode* tree);
void clear(treeNode* tree);

int main(int argc, char* argv[]) {
    int n = 10;
    if (argc > 1) {
        n = atol(argv[1]);
    }
    int minDepth = 4;
    int maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n;
    int stretchDepth = maxDepth + 1;
    stretch(stretchDepth);
    treeNode *longLived = buildTree(maxDepth);
    for (int depth = minDepth; depth <= maxDepth; depth += 2) {
        int iterations = 1 << (maxDepth - depth + minDepth);
        int sum = 0;
        for (int i = 0; i < iterations; i++)
            sum += count(depth);
        printf("%d\t trees of depth %d\t check: %d\n", iterations, depth, sum);
    }
    printf("long lived tree of depth %d\t check: %d\n", maxDepth, nodeCount(longLived));
    clear(longLived);
    return 0;
}

void stretch(int depth) {
    printf("stretch tree of depth %d\t check: %d\n", depth, count(depth));
}

int count(int depth) {
    treeNode *t = buildTree(depth);
    int c = nodeCount(t);
    clear(t);
    return c;
}

typedef struct tn {
    struct tn* left;
    struct tn* right;
} treeNode;

treeNode* buildTree(int depth) {
    treeNode* new = (treeNode*) malloc(sizeof(treeNode));
    if (depth == 0) {
        new->left = NULL;
        new->right = NULL;
        return new;
    }
    new->left = buildTree(depth - 1);
    new->right = buildTree(depth - 1);
    return new;
}

int nodeCount(treeNode* tree) {
    int result = 1;
    if (tree->left != NULL)
        result += nodeCount(tree->left);
    if (tree->right != NULL)
        result += nodeCount(tree->right);
    return result;
}

void clear(treeNode* tree) {
    if (tree->left != NULL)
        clear(tree->left);
    if (tree->right != NULL)
        clear(tree->right);
    free(tree);
}

