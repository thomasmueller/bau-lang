package org.bau.stdlib.collections;

import java.util.Random;

public class Treap<K extends Comparable<K>, V> implements SortedMap<K, V> {

    private Node<K, V> root;
    private int size;
    private final Random random = new Random();

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    @Override
    public void put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        root = insert(root, key, value);
    }

    @Override
    public V get(K key) {
        Node<K, V> curr = root;
        while (curr != null) {
            int comp = key.compareTo(curr.key);
            if (comp == 0) {
                return curr.value;
            }
            curr = (comp < 0) ? curr.left : curr.right;
        }
        return null;
    }

    @Override
    public void remove(K key) {
        if (key == null) {
            return;
        }
        root = delete(root, key);
    }

    private Node<K, V> insert(Node<K, V> node, K key, V value) {
        if (node == null) {
            size++;
            return new Node<>(key, value, random.nextInt());
        }
        int comp = key.compareTo(node.key);
        if (comp == 0) {
            node.value = value;
            return node;
        } else if (comp < 0) {
            node.left = insert(node.left, key, value);
            if (node.left.priority > node.priority) {
                node = rotateRight(node);
            }
        } else {
            node.right = insert(node.right, key, value);
            if (node.right.priority > node.priority) {
                node = rotateLeft(node);
            }
        }
        return node;
    }

    private Node<K, V> delete(Node<K, V> node, K key) {
        if (node == null) {
            return null;
        }
        int comp = key.compareTo(node.key);
        if (comp < 0) {
            node.left = delete(node.left, key);
        } else if (comp > 0) {
            node.right = delete(node.right, key);
        } else {
            if (node.left == null) {
                size--;
                return node.right;
            }
            if (node.right == null) {
                size--;
                return node.left;
            }
            if (node.left.priority > node.right.priority) {
                node = rotateRight(node);
                node.right = delete(node.right, key);
            } else {
                node = rotateLeft(node);
                node.left = delete(node.left, key);
            }
        }
        return node;
    }

    private Node<K, V> rotateRight(Node<K, V> y) {
        Node<K, V> x = y.left;
        y.left = x.right;
        x.right = y;
        return x;
    }

    private Node<K, V> rotateLeft(Node<K, V> x) {
        Node<K, V> y = x.right;
        x.right = y.left;
        y.left = x;
        return y;
    }

    private static final class Node<K, V> {
        K key;
        V value;
        int priority;
        Node<K, V> left, right;

        Node(K key, V value, int priority) {
            this.key = key;
            this.value = value;
            this.priority = priority;
        }
    }

}
