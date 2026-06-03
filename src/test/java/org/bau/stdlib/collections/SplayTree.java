package org.bau.stdlib.collections;

public class SplayTree<K extends Comparable<K>, V> implements SortedMap<K, V> {

    private Node<K, V> root;
    private int size;

    @Override
    public V get(K key) {
        if (root == null) {
            return null;
        }
        splay(key);
        if (root.key.compareTo(key) == 0) {
            return root.value;
        }
        return null;
    }

    public K getNextKey(K key) {
        int test;
        if (root == null) {
            return null;
        }
        Node<K, V> n = root;
        if (key != null) {
            splay(key);
            if (n.key.compareTo(key) > 0) {
                return n.key;
            }
        }
        while (n.left != null) {
            n = n.left;
        }
        return n.key;
    }

    @Override
    public void put(K key, V value) {
        if (root == null) {
            root = new Node<>(key, value);
            size = 1;
            return;
        }
        splay(key);
        int cmp = root.key.compareTo(key);
        if (cmp == 0) {
            root.value = value;
            return;
        }
        size++;
        Node<K, V> newNode = new Node<>(key, value);
        boolean l = cmp < 0;
        newNode.set(l, root);
        newNode.set(!l, root.get(!l));
        root.set(!l, null);
        root = newNode;
    }

    @Override
    public void remove(K key) {
        if (root == null) {
            return;
        }
        splay(key);
        if (root.key.compareTo(key) == 0) {
            size--;
            if (root.left == null) {
                root = root.right;
            } else {
                Node<K, V> rightSide = root.right;
                root = root.left;
                splay(key);
                root.right = rightSide;
            }
        }
    }

    private void splay(K key) {
        root = splayRecursive(root, key);
    }

    private Node<K, V> splayRecursive(Node<K, V> root, K key) {
        if (root == null) {
            return null;
        }
        int cmp = key.compareTo(root.key);
        if (cmp == 0) {
            return root;
        }
        boolean l = cmp < 0;
        if (root.get(l) == null) {
            return root;
        }
        cmp = (l ? 1 : -1) * key.compareTo(root.get(l).key);
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

    private Node<K, V> rotate(Node<K, V> x, boolean l) {
        Node<K, V> y = x.get(!l);
        x.set(!l, y.get(l));
        y.set(l, x);
        return y;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        root = null;
    }

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> left;
        Node<K, V> right;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        Node<K, V> get(boolean isLeft) {
            if (isLeft) {
                return left;
            }
            return right;
        }

        void set(boolean isLeft, Node<K, V> c) {
            if (isLeft) {
                left = c;
            } else {
                right = c;
            }
        }

    }

}
