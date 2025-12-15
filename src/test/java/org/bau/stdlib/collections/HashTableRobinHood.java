package org.bau.stdlib.collections;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class HashTableRobinHood<K, V> {

    // see also https://www.corsix.org/content/my-favourite-small-hash-table

    private static final int USED = 1 << 31;

    private int size;
    private int maxSize;
    private int[] hashes;
    private Object[] keyValue;
    private int mask;

    HashTableRobinHood() {
        this(4);
    }

    private HashTableRobinHood(int capacityPowerOf2) {
        hashes = new int[capacityPowerOf2];
        keyValue = new Object[2 * capacityPowerOf2];
        mask = capacityPowerOf2 - 1;
        maxSize = capacityPowerOf2 * 3 / 4;
    }

    public void put(K key, V value) {
        if (value == null) {
            remove(key);
            return;
        }
        if (size > maxSize) {
            rehash(keyValue.length);
        }
        int hash = key.hashCode() | USED;
        for (int d = 0;; d++) {
            int p = (hash + d) & mask;
            int h = hashes[p];
            if (h == hash && keyValue[p + p].equals(key)) {
                keyValue[p + p + 1] = value;
                return;
            }
            if (h == 0) {
                size += 1;
                hashes[p] = hash;
                keyValue[p + p] = key;
                keyValue[p + p + 1] = value;
                return;
            }
            int d2 = (p - h) & mask;
            if (d2 < d) {
                size += 1;
                int oldHash = hashes[p];
                Object oldKey = keyValue[p + p];
                Object oldValue = keyValue[p + p + 1];
                hashes[p] = hash;
                keyValue[p + p] = key;
                keyValue[p + p + 1] = value;
                reinsert(oldHash, oldKey, oldValue, d2);
                return;
            }
        }
    }

    private void reinsert(int hash, Object key, Object value, int d) {
        for (;; ++d) {
            int p = (hash + d) & mask;
            int h = hashes[p];
            if (h == 0) {
                hashes[p] = hash;
                keyValue[p + p] = key;
                keyValue[p + p + 1] = value;
                return;
            }
            int d2 = (p - h) & mask;
            if (d2 < d) {
                int oldHash = hashes[p];
                Object oldKey = keyValue[p + p];
                Object oldValue = keyValue[p + p + 1];
                hashes[p] = hash;
                keyValue[p + p] = key;
                keyValue[p + p + 1] = value;
                hash = oldHash;
                key = oldKey;
                value = oldValue;
                d = d2;
            }
        }
    }

    private void rehash(int newCapacity) {
        HashTableRobinHood<K, V> newTable = new HashTableRobinHood<>(newCapacity);
        for (int i = 0; i < hashes.length; i++) {
            int h = hashes[i];
            if (h != 0) {
                newTable.reinsert(h, keyValue[i + i], keyValue[i + i + 1], 0);
            }
        }
        hashes = newTable.hashes;
        keyValue = newTable.keyValue;
        mask = newTable.mask;
        maxSize = newTable.maxSize;
    }

    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    public V get(K key) {
        int hash = key.hashCode() | USED;
        for (int d = 0;; d++) {
            int p = (hash + d) & mask;
            int h = hashes[p];
            if (h == hash && keyValue[p + p].equals(key)) {
                return (V) keyValue[p + p + 1];
            } else if (h == 0 || ((p - h) & mask) < d) {
                return null;
            }
        }
    }

    public int remove(K key) {
        int hash = key.hashCode() | USED;
        for (int d = 0;; ++d) {
            int p = (hash + d) & mask;
            int h = hashes[p];
            if (h == 0 || ((p - h) & mask) < d) {
                return 0;
            } else if (h == hash && keyValue[p + p].equals(key)) {
                int next = (p + 1) & mask;
                size -= 1;
                while (hashes[next] != 0 && ((hashes[next] ^ next) & mask) != 0) {
                    hashes[p] = hashes[next];
                    keyValue[p + p] = keyValue[next + next];
                    keyValue[p + p + 1] = keyValue[next + next + 1];
                    p = next;
                    next = (p + 1) & mask;
                }
                hashes[p] = 0;
                keyValue[p + p] = null;
                keyValue[p + p + 1] = null;
                return 1;
            }
        }
    }

    public HashMapIterator<K, V> iterator() {
        return new HashMapIterator<>(keyValue);
    }

    public void clear() {
        hashes = new int[4];
        keyValue = new Object[8];
        mask = 3;
        size = 0;
        maxSize = 4 * 3 / 4;
    }

    public static class HashMapIterator<K, V> implements Iterator<Entry<K, V>> {
        private final Object[] keyValue;
        private int current;

        HashMapIterator(Object[] keyValue) {
            this.keyValue = keyValue;
            fetchNext();
        }

        private void fetchNext() {
            current++;
            while(current * 2 < keyValue.length && keyValue[current * 2] == null) {
                current++;
            }
        }

        @Override
        public boolean hasNext() {
            return current * 2 < keyValue.length;
        }

        @Override
        public Entry<K, V> next() {
            if (!hasNext()) {
                throw new IllegalStateException("No more elements.");
            }
            Entry<K, V> e = new Map.Entry<>() {

                int pos = current;

                @SuppressWarnings("unchecked")
                @Override
                public K getKey() {
                    return (K) keyValue[pos * 2];
                }

                @SuppressWarnings("unchecked")
                @Override
                public V getValue() {
                    return (V) keyValue[pos * 2 + 1];
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
