package org.bau.stdlib.collections;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bau.stdlib.math.IntMath;

public class SkipList2<K extends Comparable<K>, V> implements SortedMap<K, V> {

    private final Random random = new Random();
    private Node<K, V> head;
    private int size;
    private int capacity;
    private int maxLevel;

    public SkipList2() {
        clear();
    }

    @SuppressWarnings("unchecked")
    private SkipList2(SkipList2<K, V> old) {
        this.size = old.size;
        long p2 = IntMath.nextPowerOf2(2 * size + 1);
        this.maxLevel = 64 - Long.numberOfLeadingZeros(p2);
        head = new Node<K, V>(null, null, new Node[maxLevel]);
        Node<K, V> n = old.head.next[0];
        Node<K, V>[] last = new Node[maxLevel];
        for (int i = 0; i < maxLevel; i++) {
            last[i] = head;
        }
        for (int i = 0; i < size; i++) {
            int newLevel = 1 + Long.numberOfTrailingZeros(i - size / 2 + p2 / 2);
            Node<K, V> newNode = new Node<K, V>(n.key, n.value, new Node[newLevel]);
            for (int j = 0; j < newLevel; j++) {
                last[j].next[j] = newNode;
                last[j] = newNode;
            }
            n = n.next[0];
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void clear() {
        capacity = 16;
        maxLevel = 4;
        head = new Node<>(null, null, new Node[maxLevel]);
        size = 0;
    }

    private void rebuild() {
        SkipList2<K, V> optimal = new SkipList2<>(this);
        this.size = optimal.size;
        this.head = optimal.head;
        this.maxLevel = optimal.maxLevel;
        this.capacity *= 2;
    }

    public void put(K key, V value) {
        if (size > capacity) {
            rebuild();
        }
        int newLevel = Math.min(maxLevel, 1 + Long.numberOfLeadingZeros(random.nextLong()));
        @SuppressWarnings("unchecked")
        Node<K, V> newNode = new Node<K, V>(key, value, new Node[newLevel]);
        if (!replace(key, newNode)) {
            size++;
        }
    }

    public void remove(K key) {
        if (replace(key, null)) {
            size--;
        }
    }

    private boolean replace(K key, Node<K, V> newNode) {
        boolean found = false;
        Node<K, V> current = head;
        for (int i = maxLevel - 1; i >= 0; i--) {
            while (current.next[i] != null) {
                int comp = compare(current, i, key);
                if (comp > 0) {
                    break;
                } else if (comp == 0) {
                    found = true;
                    current.next[i] = current.next[i].next[i];
                    break;
                }
                current = current.next[i];
            }
            if (newNode != null && i < newNode.next.length) {
                newNode.next[i] = current.next[i];
                current.next[i] = newNode;
            }
        }
        return found;
    }

    private Node<K, V> findFirst(K key) {
        Node<K, V> current = head;
        if (key != null) {
            for (int i = head.next.length - 1; i >= 0; i--) {
                while (compare(current, i, key) < 0) {
                    current = current.next[i];
                }
            }
        }
        return current.next[0];
    }

    private int compare(Node<K, V> node, int i, K key) {
        if (node.next[i] == null) {
            return 1;
        }
        return node.next[i].key.compareTo(key);
    }

    public V get(K key) {
        Node<K, V> n = findFirst(key);
        if (n != null && n.key.compareTo(key) == 0) {
            return n.value;
        }
        return null;
    }

    public SkipListIterator iterate(K key) {
        return new SkipListIterator(findFirst(key));
    }

    private static class Node<K, V> {
        final K key;
        final V value;
        final Node<K, V>[] next;

        Node(K key, V value, Node<K, V>[] next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    public class SkipListIterator implements Iterator<Entry<K, V>> {
        private Node<K, V> current;

        public SkipListIterator(Node<K, V> current) {
            this.current = current;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Entry<K, V> next() {
            if (!hasNext()) {
                throw new IllegalStateException("No more elements.");
            }
            Node<K, V> n = current;
            Entry<K, V> e = new Map.Entry<>() {

                @Override
                public K getKey() {
                    return n.key;
                }

                @Override
                public V getValue() {
                    return n.value;
                }

                @Override
                public V setValue(V value) {
                    throw new UnsupportedOperationException();
                }

            };
            current = current.next[0];
            return e;
        }
    }

    @Override
    public int size() {
        return size;
    }

}
