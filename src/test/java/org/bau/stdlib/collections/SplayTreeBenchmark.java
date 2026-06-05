package org.bau.stdlib.collections;

public class SplayTreeBenchmark {

    private static final int SIZE = 500_000;
    private static final int ITERATIONS = 1000;
    private static final int COUNT = 2000;

    private static long randomState = 42;

    public static void main(String[] args) {
        // setup
        SplayTree tree = new SplayTree();
        for (int i = 0; i < SIZE; i++) {
            int key = generateKey();
            tree.put(key, (long) key);
        }
        // run
        for (int i = 0; i < ITERATIONS; i++) {
            for (int j = 0; j < COUNT; j++) {
                int key = generateKey();
                tree.put(key, (long) key);
                int higher = tree.higherKey(key);
                if (higher < 0) {
                    tree.remove(key);
                } else {
                    tree.remove(higher);
                }
            }

        }
        // verify
        int k = tree.higherKey(-1);
        long sum = 0;
        for (int i = 0; i < tree.size(); i++) {
            sum += k;
            k = tree.higherKey(k);
        }
        System.out.println("sum: " + sum);
    }

    private static int generateKey() {
        randomState ^= (randomState << 13) & 0xffffffffL;
        randomState ^= (randomState >>> 17);
        randomState ^= (randomState << 5) & 0xffffffffL;
        return (int) randomState;
    }

    private static int compare(int a, int b) {
        return a == b ? 0 : a < b ? -1 : 1;
    }

    static class SplayTree {

        private Node root;
        private int size;

        public long get(int key) {
            if (root == null) {
                return -1;
            }
            splay(key);
            if (compare(root.key, key) == 0) {
                return root.value;
            }
            return -1;
        }

        public int higherKey(int key) {
            if (root == null) {
                return -1;
            }
            if (key < 0) {
                Node n = root;
                while (n.left != null) {
                    n = n.left;
                }
                splay(n.key);
                return n.key;
            }
            Node candidate = null;
            Node n = root;
            while (n != null) {
                int cmp = compare(key, n.key);
                if (cmp < 0) {
                    candidate = n;
                    n = n.left;
                } else {
                    n = n.right;
                }
            }
            if (candidate != null) {
                splay(candidate.key);
                return candidate.key;
            }
            return -1;
        }

        public void put(int key, long value) {
            if (root == null) {
                root = new Node(key, value);
                size = 1;
                return;
            }
            splay(key);
            int cmp = compare(root.key, key);
            if (cmp == 0) {
                root.value = value;
                return;
            }
            size++;
            Node newNode = new Node(key, value);
            boolean l = cmp < 0;
            newNode.set(l, root);
            newNode.set(!l, root.get(!l));
            root.set(!l, null);
            root = newNode;
        }

        public void remove(int key) {
            if (root == null) {
                return;
            }
            splay(key);
            if (compare(root.key, key) == 0) {
                size--;
                if (root.left == null) {
                    root = root.right;
                } else {
                    Node rightSide = root.right;
                    root = root.left;
                    splay(key);
                    root.right = rightSide;
                }
            }
        }

        private void splay(int key) {
            root = splayRecursive(root, key);
        }

        private Node splayRecursive(Node root, int key) {
            if (root == null) {
                return null;
            }
            int cmp = compare(key, root.key);
            if (cmp == 0) {
                return root;
            }
            boolean l = cmp < 0;
            if (root.get(l) == null) {
                return root;
            }
            cmp = (l ? 1 : -1) * compare(key, root.get(l).key);
            if (cmp < 0) {
                root.get(l).set(l, splayRecursive(root.get(l).get(l), key));
                root = rotate(root, !l);
            } else if (cmp > 0) {
                root.get(l).set(!l, splayRecursive(root.get(l).get(!l), key));
                if (root.get(l).get(!l) != null) {
                    root.set(l, rotate(root.get(l), l));
                }
            }
            return (root.get(l) == null) ? root : rotate(root, !l);
        }

        private Node rotate(Node x, boolean l) {
            Node y = x.get(!l);
            x.set(!l, y.get(l));
            y.set(l, x);
            return y;
        }

        public int size() {
            return size;
        }

        public void clear() {
            root = null;
        }

        private static class Node {
            int key;
            long value;
            Node left;
            Node right;

            Node(int key, long value) {
                this.key = key;
                this.value = value;
            }

            Node get(boolean isLeft) {
                if (isLeft) {
                    return left;
                }
                return right;
            }

            void set(boolean isLeft, Node c) {
                if (isLeft) {
                    left = c;
                } else {
                    right = c;
                }
            }

        }

    }

}
