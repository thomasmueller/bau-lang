#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <stdint.h>
#include <limits.h>
#include <time.h>
#include <math.h>

typedef struct PointerNode {
    void **ptr;
    struct PointerNode *prev;  // Reference to the previous pointer
} PointerNode;

PointerNode *top_ptr = NULL;

// 5.688 empty
// 6.683 with pointers
#define PUSH(X)  PointerNode X##_p = { .ptr = (void**)&X, .prev = top_ptr }; top_ptr = &X##_p;
#define POP()    top_ptr = top_ptr->prev;

void print_stack() {
    PointerNode *current = top_ptr;
    printf("Current stack of pointers:\n");
    while (current != NULL) {
        printf("  Pointer at stack %p is address %p\n", current, current->ptr);
        // printf("Pointer at stack %p is address %p and points to %s\n", current, current->ptr, *(char**)(current->ptr));
        current = current->prev;
    }
}


#define BUFFER_SIZE 1024

void* freeBuffer[BUFFER_SIZE];
int bufferIndex = 0;

void freeAllBuffers() {
    // printf("free %d\n", bufferIndex);
    while (bufferIndex > 0) {
        free(freeBuffer[--bufferIndex]);
    }
}

void addFreeBuffer(void* ptr) {
    freeBuffer[bufferIndex++] = ptr; 
    if (bufferIndex >= BUFFER_SIZE) {
        freeAllBuffers();
    }
}

typedef struct tn {
    struct tn* left;
    struct tn* right;
} treeNode;

treeNode* NewTreeNode(treeNode* left, treeNode* right) {
    treeNode* new = (treeNode*)malloc(sizeof(treeNode));
    new->left = left;
    new->right = right;
    return new;
}

long ItemCheck(treeNode* tree) {
    PUSH(tree);
    if (tree->left == NULL) {
        POP();
        return 1;
    } else {
        long result = 1 + ItemCheck(tree->left) + ItemCheck(tree->right);
        POP();
        return result;
    }
}

treeNode* BottomUpTree(unsigned depth) {
    if (depth > 0) {
        return NewTreeNode(
            BottomUpTree(depth - 1),
            BottomUpTree(depth - 1)
        );
    } else {
        return NewTreeNode(NULL, NULL);
    }
}

void DeleteTree(treeNode* tree) {
    PUSH(tree);
    if (tree->left != NULL) {
        DeleteTree(tree->left);
        DeleteTree(tree->right);
    }
    // 6.683 with addFreeBuffer
    // 6.188 directly free
    // 4.977 directly free, without pointer chain
    // free(tree);
     
    addFreeBuffer(tree);
    POP();
}

int test(unsigned N) {
    for (int i=0; i<10; i++) {
        int stretchDepth = 20;
        treeNode *stretchTree = BottomUpTree(stretchDepth);
        printf(
            "stretch tree of depth %u\t check: %li\n",
            stretchDepth,
            ItemCheck(stretchTree)
        );
        DeleteTree(stretchTree);            
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
    PUSH(stretchTree);
    printf(
        "stretch tree of depth %u\t check: %li\n",
        stretchDepth,
        ItemCheck(stretchTree)
    );
    DeleteTree(stretchTree);
    treeNode *longLivedTree = BottomUpTree(maxDepth);
    PUSH(longLivedTree);
    for (depth = minDepth; depth <= maxDepth; depth += 2) {
        long    i, iterations, check;
        iterations = pow(2, maxDepth - depth + minDepth);
        check = 0;
        for (i = 1; i <= iterations; i++) {
            treeNode *tempTree = BottomUpTree(depth);
            PUSH(tempTree);
            check += ItemCheck(tempTree);
            DeleteTree(tempTree);
            POP();
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
    POP();
    POP();
    */
    return 0;
}

int main2(int argc, char* argv[]) {
    unsigned N = atol(argv[1]);
    return test(N);
}


int test1() {
    char* world = "world";
    PUSH(world);
    print_stack();
    POP();
    return 0;
}

int main() {
    char* hello = "hello";
    PUSH(hello);
    print_stack();
    test1();
    print_stack();
    POP();
    test(20);
    freeAllBuffers();
    return 0;
}
