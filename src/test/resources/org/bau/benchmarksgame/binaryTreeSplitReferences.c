#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <limits.h>
#include <time.h>
#include <math.h>

/*
:1 means it's a uniue pointer
 ? means it may be null
type Tree
    left Tree:1?
    right Tree:1?
*/
typedef struct treeNode {
    struct treeNode* left;
    struct treeNode* right;
} treeNode;

/*
fun newTree(left Tree, right Tree) Tree
    t := new(Tree)
    t.left = left
    t.right = right
    return t

# :1 for parameters means the method will assign (use) them
# :1 for return means the method will return a unique pointer
fun newTree(left Tree:1, right Tree:1) Tree:1
    t := new(Tree:1)
    t.left = left
    t.right = right
    return t
*/
treeNode* newTree(treeNode* left, treeNode* right) {
    treeNode* new = (treeNode*)malloc(sizeof(treeNode));
    new->left = left;
    new->right = right;
    return new;
}

/*
fun Tree nodeCount() int
    result := 1
    l := left
    if l
        result += l.nodeCount()
    r := right
    if r
        result += r.nodeCount()
    return result

# noFree means the method doesn't free memory 
# and doesn't call a method that does
fun Tree:1 nodeCount() noFree int
    result := 1
    # temporary references are aliases; 
    # they are allowed if we only call noFree methods
    l := left
    if l
        result += l.nodeCount()
    r := right
    if r
        result += r.nodeCount()
    right = r
    return result
*/
long nodeCount(treeNode* tree) {
    long result = 1;
    if (tree->left) {
        result += nodeCount(tree->left);
    }
    if (tree->right) {
        result += nodeCount(tree->right);
    }
    return result;
}

/*
fun with(depth int) Tree
    if depth = 0
        return newTree(null, null)
    return newTree(with(depth - 1), with(depth - 1))
    
fun with(depth int) Tree:1
    if depth = 0
        return newTree(null, null)
    return newTree(with(depth - 1), with(depth - 1))
    
*/
treeNode* with(unsigned depth) {
    if (depth == 0) {
        return newTree(NULL, NULL);
    }
    treeNode* l = with(depth - 1);
    treeNode* r = with(depth - 1);
    return newTree(l, r);
}

/*
fun Tree clear()
    if left <> null
        left.clear()
        left = null
        right.clear()
        right = null
        
fun Tree:1 clear() free
    if left <> null
        left = left.clear()
        left = null
        right = right.clear();
        right = null
*/
void clear(treeNode* tree) {
    if (tree->left != NULL) {
        clear(tree->left);
        free(tree->left);
        tree->left = NULL;
        clear(tree->right);
        free(tree->right);
        tree->right = NULL;
    }
}

/*
fun count(depth int) int
    t := with(depth)
    c := t.nodeCount()
    t.clear()
    return c
*/
long count(unsigned depth) {
    treeNode* t = with(depth);
    long c = nodeCount(t);
    clear(t);
    return c;
}

/*
fun stretch(depth int)
    c := count(depth)
    println('stretch tree of depth ' depth '; check: ' c)
*/
void stretch(unsigned depth) {
    long c = count(depth);
    printf("depth tree of depth %u; check: %li\n", depth, c);
}

/*
c:
./a.out  0.40s user 0.04s system 98% cpu 0.448 total

bau/refCount:
./a.out  0.56s user 0.08s system 98% cpu 0.655 total
*/
int test(unsigned N) {
    for (int i = 0; i < 10; i++) {
        stretch(20);
    }
    /*
    unsigned depth, minDepth, maxDepth, stretchDepth;
    minDepth = 4;
    if ((minDepth + 2) > N) {
        maxDepth = minDepth + 2;
    } else {
        maxDepth = N;
    }
    stretchDepth = maxDepth + 1;
    treeNode *stretchTree = BottomUpTree(stretchDepth);
    printf(
        "stretch tree of depth %u\t check: %li\n",
        stretchDepth,
        ItemCheck(stretchTree)
    );
    DeleteTree(stretchTree);
    treeNode *longLivedTree = BottomUpTree(maxDepth);
    for (depth = minDepth; depth <= maxDepth; depth += 2) {
        long    i, iterations, check;
        iterations = pow(2, maxDepth - depth + minDepth);
        check = 0;
        for (i = 1; i <= iterations; i++) {
            treeNode *tempTree = BottomUpTree(depth);
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
    */
    return 0;
}

int main2(int argc, char* argv[]) {
    unsigned N = atol(argv[1]);
    return test(N);
}

int main() {
    test(20);
    return 0;
}
