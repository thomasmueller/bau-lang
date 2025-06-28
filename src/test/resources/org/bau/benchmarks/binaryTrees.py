# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

import sys

class Node:
    def __init__(self, left=None, right=None):
        self.left = left
        self.right = right

    def node_count(self):
        result = 1
        if self.left is not None:
            result += self.left.node_count()
        if self.right is not None:
            result += self.right.node_count()
        return result

def build_tree(depth):
    if depth <= 0:
        return Node()
    return Node(build_tree(depth - 1), build_tree(depth - 1))

def count(depth):
    return build_tree(depth).node_count()

def stretch(depth):
    c = count(depth)
    print(f"stretch tree of depth {depth}\t check: {c}")

def main():
    n = 10
    if len(sys.argv) > 1:
        n = int(sys.argv[1])
    min_depth = 4
    max_depth = n
    if min_depth + 2 > n:
        max_depth = min_depth + 2
    stretch_depth = max_depth + 1
    stretch(stretch_depth)
    long_lived = build_tree(max_depth)
    for depth in range(min_depth, max_depth + 1, 2):
        iterations = 1 << (max_depth - depth + min_depth)
        total = 0
        for _ in range(iterations):
            total += count(depth)
        print(f"{iterations}\t trees of depth {depth}\t check: {total}")
    count2 = long_lived.node_count()
    print(f"long lived tree of depth {max_depth}\t check: {count2}")

main()