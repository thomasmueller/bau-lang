package org.bau.stdlib.collections;

import java.util.ArrayList;
import java.util.List;

public class ScapegoatTree<K extends Comparable<K>, V> implements SortedMap<K, V> {

    private static final int ALPHA = 66;

    private Node<K, V> root;
    private int size;
    private int maxSize;

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> left, right, parent;

        Node(K key, V value, Node<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }
    }

    public ScapegoatTree() {
        clear();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
        maxSize = 0;
    }

    @Override
    public void put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        if (root == null) {
            root = new Node<>(key, value, null);
            size = 1;
            maxSize = 1;
            return;
        }
        Node<K, V> curr = root;
        Node<K, V> parent = null;
        int depth = 0;
        while (curr != null) {
            parent = curr;
            int cmp = key.compareTo(curr.key);
            if (cmp == 0) {
                curr.value = value; // replace
                return;
            } else if (cmp < 0) {
                curr = curr.left;
            } else {
                curr = curr.right;
            }
            depth++;
        }
        Node<K, V> newNode = new Node<>(key, value, parent);
        if (key.compareTo(parent.key) < 0) {
            parent.left = newNode;
        } else {
            parent.right = newNode;
        }

        size++;
        maxSize = Math.max(maxSize, size);
        if (depth > logAlpha(maxSize)) {
            Node<K, V> scapegoat = findScapegoat(newNode);
            rebuildSubtree(scapegoat);
        }
    }

    @Override
    public V get(K key) {
        Node<K, V> node = findNode(key);
        return node == null ? null : node.value;
    }

    @Override
    public void remove(K key) {
        Node<K, V> node = findNode(key);
        if (node == null)
            return;
        deleteNode(node);
        size--;
        if (size * 100 < ALPHA * maxSize) {
            root = rebuildTree(root);
            if (root != null)
                root.parent = null;
            maxSize = size;
        }
    }

    private Node<K, V> findNode(K key) {
        Node<K, V> curr = root;
        while (curr != null) {
            int cmp = key.compareTo(curr.key);
            if (cmp == 0)
                return curr;
            curr = (cmp < 0) ? curr.left : curr.right;
        }
        return null;
    }

    private void deleteNode(Node<K, V> node) {
        if (node.left != null && node.right != null) {
            // swap with successor
            Node<K, V> succ = minimum(node.right);
            node.key = succ.key;
            node.value = succ.value;
            node = succ;
        }
        Node<K, V> child = (node.left != null) ? node.left : node.right;
        if (child != null) {
            child.parent = node.parent;
        }
        if (node.parent == null) {
            root = child;
        } else if (node == node.parent.left) {
            node.parent.left = child;
        } else {
            node.parent.right = child;
        }
    }

    private Node<K, V> minimum(Node<K, V> node) {
        while (node.left != null)
            node = node.left;
        return node;
    }

    private Node<K, V> findScapegoat(Node<K, V> node) {
        Node<K, V> curr = node.parent;
        while (curr != null) {
            int leftSize = size(curr.left);
            int rightSize = size(curr.right);
            int total = leftSize + rightSize + 1;
            if (leftSize * 100 > ALPHA * total || rightSize * 100 > ALPHA * total) {
                return curr;
            }
            curr = curr.parent;
        }
        return null; // should not happen
    }

    private int size(Node<K, V> node) {
        if (node == null)
            return 0;
        return 1 + size(node.left) + size(node.right);
    }

    private void rebuildSubtree(Node<K, V> scapegoat) {
        Node<K, V> parent = scapegoat.parent;
        Node<K, V> rebuilt = rebuildTree(scapegoat);
        if (parent == null) {
            root = rebuilt;
            rebuilt.parent = null;
        } else if (parent.left == scapegoat) {
            parent.left = rebuilt;
            rebuilt.parent = parent;
        } else {
            parent.right = rebuilt;
            rebuilt.parent = parent;
        }
    }

    private Node<K, V> rebuildTree(Node<K, V> node) {
        List<Node<K, V>> nodes = new ArrayList<>();
        flatten(node, nodes);
        return buildBalanced(nodes, 0, nodes.size() - 1, null);
    }

    private void flatten(Node<K, V> node, List<Node<K, V>> out) {
        if (node == null)
            return;
        flatten(node.left, out);
        out.add(node);
        flatten(node.right, out);
    }

    private Node<K, V> buildBalanced(List<Node<K, V>> nodes, int lo, int hi, Node<K, V> parent) {
        if (lo > hi)
            return null;
        int mid = (lo + hi) >>> 1;
        Node<K, V> root = nodes.get(mid);
        root.parent = parent;
        root.left = buildBalanced(nodes, lo, mid - 1, root);
        root.right = buildBalanced(nodes, mid + 1, hi, root);
        return root;
    }

    private int logAlpha(int n) {
        return (int) Math.floor(Math.log(n) / Math.log(1.0 / (ALPHA / 100.)));
    }
}
