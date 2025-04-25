package org.bau.stdlib.collections;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class HashTable<K, V> {

    private static final int USED = 1 << 31;

    private int size;
    @SuppressWarnings("unchecked")
    private K[] keys = (K[]) new Object[4];
    @SuppressWarnings("unchecked")
    private V[] values = (V[]) new Object[4];
    private int[] hashes = new int[4];

    public void put(K key, V value) {
        int hash = key.hashCode() | USED;
        if (size * 2 >= keys.length) {
            @SuppressWarnings("unchecked")
            K[] kn = (K[]) new Object[keys.length * 2];
            @SuppressWarnings("unchecked")
            V[] vn = (V[]) new Object[keys.length * 2];
            int[] hn = new int[keys.length * 2];
            K[] ok = keys;
            int[] oh = hashes;
            V[] ov = values;
            keys = kn;
            values = vn;
            hashes = hn;
            for (int i = 0; i < ok.length; i++) {
                if (oh[i] != 0) {
                    put(ok[i], ov[i]);
                }
            }
        }
        int p = hash & (keys.length - 1);
        while (true) {
            if (hashes[p] == hash && keys[p].equals(key)) {
                values[p] = value;
                return;
            } else if (hashes[p] == 0) {
                size += 1;
                keys[p] = key;
                hashes[p] = hash;
                values[p] = value;
                return;
            }
            p = (p + 1) & (keys.length - 1);
        }
    }

    public V get(K key) {
        int hash = key.hashCode() | USED;
        int p = hash & (keys.length - 1);
        while (true) {
            if (hashes[p] == 0) {
                return values[p];
            } else if (hashes[p] == hash && keys[p].equals(key)) {
                return values[p];
            }
            p = (p + 1) & (keys.length - 1);
        }
    }

    public int remove(K key) {
        int hash = key.hashCode() | USED;
        int p = hash & (keys.length - 1);
        while (true) {
            if (hashes[p] == 0) {
                // not found
                return 0;
            } else if (hashes[p] == hash && keys[p].equals(key)) {
                keys[p] = null;
                hashes[p] = 0;
                values[p] = null;
                size -= 1;
                break;
            }
            p = (p + 1) & (keys.length - 1);
        }
        int p2 = (p + 1) & (keys.length - 1);
        while (true) {
            int h2 = hashes[p2];
            if (h2 == 0) {
                break;
            }
            int pb = h2 & (keys.length - 1);
            boolean canMove = false;
            if (pb <= p2) {
                //  normal case (no wrap)
                //  pb.....p2
                canMove = (pb <= p) && (p2 >= p);
            } else {
                // wrapped around
                //  ....p2 pb.....
                canMove = pb <= p || p2 > p;
            }
            if (canMove) {
                keys[p] = keys[p2];
                hashes[p] = hashes[p2];
                values[p] = values[p2];
                keys[p2] = null;
                hashes[p2] = 0;
                values[p2] = null;
                p = p2;
            }
            p2 = (p2 + 1) & (keys.length - 1);
        }
        return 1;
    }

    public HashMapIterator<K, V> iterator() {
        return new HashMapIterator<>(keys, values);
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        keys = (K[]) new Object[4];
        values = (V[]) new Object[4];
        hashes = new int[4];
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
