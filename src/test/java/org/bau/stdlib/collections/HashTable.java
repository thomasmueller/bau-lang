package org.bau.stdlib.collections;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class HashTable<K, V> {

    private static final int USED = 1 << 31;

    private int size;
    private int[] hashes = new int[4];
    @SuppressWarnings("unchecked")
    private K[] keys = (K[]) new Object[4];
    @SuppressWarnings("unchecked")
    private V[] values = (V[]) new Object[4];

    public void put(K key, V value) {
        if (value == null) {
            remove(key);
            return;
        }
        if (size * 5 >= keys.length * 3) {
            rehash(keys.length * 2);
        }
        int hash = key.hashCode() | USED;
        int p = pos(hash);
        while (true) {
            if (hashes[p] == hash && keys[p].equals(key)) {
                values[p] = value;
                return;
            } else if (hashes[p] == 0) {
                size += 1;
                hashes[p] = hash;
                keys[p] = key;
                values[p] = value;
                return;
            }
            p = pos(p + 1);
        }
    }

    private void rehash(int newCapacity) {
        int[] newHashes = new int[newCapacity];
        @SuppressWarnings("unchecked")
        K[] newKeys = (K[]) new Object[newCapacity];
        @SuppressWarnings("unchecked")
        V[] newValues = (V[]) new Object[newCapacity];
        for (int i = 0; i < hashes.length; i++) {
            int h = hashes[i];
            if (h != 0) {
                int p = h & (newCapacity - 1);
                while (newHashes[p] != 0) {
                    p = (p + 1) & (newCapacity - 1);
                }
                newHashes[p] = h;
                newKeys[p] = keys[i];
                newValues[p] = values[i];
            }
        }
        hashes = newHashes;
        keys = newKeys;
        values = newValues;
    }

    public int size() {
        return size;
    }

    private int pos(int x) {
        return x & (keys.length - 1);
    }

    public V get(K key) {
        int hash = key.hashCode() | USED;
        int p = pos(hash);
        while (true) {
            if (hashes[p] == 0 || (hashes[p] == hash && keys[p].equals(key))) {
                return values[p];
            }
            p = pos(p + 1);
        }
    }

    public int remove(K key) {
        int hash = key.hashCode() | USED;
        int p = pos(hash);
        while (true) {
            if (hashes[p] == 0) {
                return 0;
            } else if (hashes[p] == hash && keys[p].equals(key)) {
                hashes[p] = 0;
                keys[p] = null;
                values[p] = null;
                size -= 1;
                break;
            }
            p = pos(p + 1);
        }
        int p2 = p;
        while (true) {
            p2 = pos(p2 + 1);
            int h2 = hashes[p2];
            if (h2 == 0) {
                return 1;
            }
            if ((pos(p2 - h2) >= pos(p - h2))) {
                hashes[p] = hashes[p2];
                keys[p] = keys[p2];
                values[p] = values[p2];
                hashes[p2] = 0;
                keys[p2] = null;
                values[p2] = null;
                p = p2;
            }
        }
    }

    public HashMapIterator<K, V> iterator() {
        return new HashMapIterator<>(keys, values);
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        hashes = new int[4];
        keys = (K[]) new Object[4];
        values = (V[]) new Object[4];
        size = 0;
    }

    public static class HashMapIterator<K, V> implements Iterator<Entry<K, V>> {
        private final K[] keys;
        private final V[] values;
        private int current;

        HashMapIterator(K[] keys, V[] values) {
            this.keys = keys;
            this.values = values;
            fetchNext();
        }

        private void fetchNext() {
            current++;
            while(current < keys.length && keys[current] == null) {
                current++;
            }
        }

        @Override
        public boolean hasNext() {
            return current < keys.length;
        }

        @Override
        public Entry<K, V> next() {
            if (!hasNext()) {
                throw new IllegalStateException("No more elements.");
            }
            Entry<K, V> e = new Map.Entry<>() {

                int pos = current;

                @Override
                public K getKey() {
                    return keys[pos];
                }

                @Override
                public V getValue() {
                    return values[pos];
                }

                @Override
                public V setValue(V value) {
                    throw new UnsupportedOperationException();
                }

            };
            fetchNext();
            return e;
        }
    }

}
