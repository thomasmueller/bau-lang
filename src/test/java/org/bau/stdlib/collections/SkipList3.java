package org.bau.stdlib.collections;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bau.stdlib.math.IntMath;

public class SkipList3<K extends Comparable<K>, V> implements SortedMap<K, V> {

    private final static boolean REUSE_SPACE = true;

    private final Random random = new Random();
    private int size;
    private int capacity;
    private int maxLevel;
    private int[] indexArray;
    private int indexPos;
    private K[] keys;
    private V[] values;
    private int[] nextFree;
    private int nextFreeKey;
    private int[] nextFreeKeyChain;
    private int pos;

    public SkipList3() {
        clear();
    }

    @Override
    public void clear() {
        clear(4, 4);
    }

    @SuppressWarnings("unchecked")
    private void clear(int capacity, int maxLevel) {
        this.capacity = capacity;
        this.keys = (K[]) new Comparable[capacity];
        this.values = (V[]) new Object[capacity];
        this.indexArray = new int[(int) (3.2 * capacity)];
        this.nextFree = new int[maxLevel + 1];
        this.nextFreeKeyChain = new int[capacity];
        this.nextFreeKey = 0;
        this.size = 0;
        this.pos = 0;
        this.indexPos = 0;
        this.maxLevel = maxLevel;
        newNode(maxLevel, -1);
    }

    private SkipList3(SkipList3<K, V> old) {
        long p2 = IntMath.nextPowerOf2(2 * old.size + 1);
        int maxLevel = 64 - Long.numberOfLeadingZeros(p2);
        clear(old.capacity * 2, maxLevel);
        this.size = old.size;
        int n = old.indexArray[1];
        int[] last = new int[maxLevel];
        for (int i = 0; i < size; i++) {
            int newLevel = 1 + Long.numberOfTrailingZeros(i - size / 2 + p2 / 2);
            int index = putKeyValue(old.keys[old.indexArray[n]], old.values[old.indexArray[n]]);
            int newNode = newNode(newLevel, index);
            for (int j = 0; j < newLevel; j++) {
                indexArray[last[j] + 1 + j] = newNode;
                last[j] = newNode;
            }
            n = old.indexArray[n + 1];
        }
    }

    int newNode(int level, int pos) {
        if (REUSE_SPACE && nextFree[level] > 0) {
            int result = nextFree[level];
            nextFree[level] = indexArray[result];
            indexArray[result] = pos;
            return result;
        }
        int result = indexPos;
        indexPos += level + 1;
        indexArray[result] = pos;
        return result;
    }

    int putKeyValue(K key, V value) {
        if (REUSE_SPACE && nextFreeKey > 0) {
            int result = nextFreeKey;
            keys[result] = key;
            values[result] = value;
            nextFreeKey = nextFreeKeyChain[result];
            return result;
        }
        keys[pos] = key;
        values[pos] = value;
        return pos++;
    }

    private void rebuild() {
        SkipList3<K, V> optimal = new SkipList3<>(this);
        this.size = optimal.size;
        this.indexArray = optimal.indexArray;
        this.indexPos = optimal.indexPos;
        this.keys = optimal.keys;
        this.values = optimal.values;
        this.pos = optimal.pos;
        this.capacity = optimal.capacity;
        this.maxLevel = optimal.maxLevel;
        this.nextFree = optimal.nextFree;
        this.nextFreeKeyChain = optimal.nextFreeKeyChain;
        this.nextFreeKey = optimal.nextFreeKey;
    }

    public void put(K key, V value) {
        if (pos >= capacity || indexPos + 2 * maxLevel >= indexArray.length) {
            rebuild();
        }
        int newLevel = Math.min(maxLevel, 1 + Long.numberOfLeadingZeros(random.nextLong()));
        int index = putKeyValue(key, value);
        int newNode = newNode(newLevel, index);
        if (!replace(key, newNode, newLevel, value)) {
            size++;
        }
    }

    public void remove(K key) {
        if (replace(key, -1, 0, null)) {
            size--;
        }
    }

    private boolean replace(K key, int newNode, int newLevel, V newValue) {
        int foundLevel = -1;
        int foundNode = -1;
        int current = 0;
        for (int i = maxLevel - 1; i >= 0; i--) {
            while (indexArray[current + 1 + i] != 0) {
                int comp = compare(current, i, key);
                if (comp > 0) {
                    break;
                } else if (comp == 0) {
                    if (foundLevel < 0) {
                        foundLevel = i;
                    }
                    foundNode = indexArray[current + 1 + i];
                    indexArray[current + 1 + i] = indexArray[indexArray[current + 1 + i] + 1 + i];
                    break;
                }
                current = indexArray[current + 1 + i];
            }
            if (newNode >= 0 && i < newLevel) {
                indexArray[newNode + 1 + i] = indexArray[current + 1 + i];
                indexArray[current + 1 + i] = newNode;
            }
        }
        if (foundNode >= 0) {
            int freeKey = indexArray[foundNode];
            keys[freeKey] = null;
            values[freeKey] = null;
            if (REUSE_SPACE) {
                nextFreeKeyChain[freeKey] = nextFreeKey;
                nextFreeKey = freeKey;
                indexArray[foundNode] = nextFree[foundLevel];
                nextFree[foundLevel] = foundNode;
            }
            return true;
        }
        return false;
    }

    private int findFirst(K key) {
        int current = 0;
        if (key != null) {
            for (int i = maxLevel - 1; i >= 0; i--) {
                while (compare(current, i, key) < 0) {
                    current = indexArray[current + 1 + i];
                }
            }
        }
        return indexArray[current + 1];
    }

    private int compare(int node, int i, K key) {
        int n = indexArray[node + 1 + i];
        if (n == 0) {
            return 1;
        }
        return keys[indexArray[n]].compareTo(key);
    }

    public V get(K key) {
       int n = findFirst(key);
       int index = indexArray[n];
        if (n >= 0 && index >= 0 && keys[index].compareTo(key) == 0) {
            return values[index];
        }
        return null;
    }

    public SkipListIterator iterate(K key) {
        return new SkipListIterator(findFirst(key));
    }

    public class SkipListIterator implements Iterator<Entry<K, V>> {
        private int current;

        public SkipListIterator(int current) {
            this.current = current;
        }

        @Override
        public boolean hasNext() {
            return current > 0;
        }

        @Override
        public Entry<K, V> next() {
            if (!hasNext()) {
                throw new IllegalStateException("No more elements.");
            }
            int n = current;
            Entry<K, V> e = new Map.Entry<>() {

                @Override
                public K getKey() {
                    return keys[indexArray[n]];
                }

                @Override
                public V getValue() {
                    return values[indexArray[n]];
                }

                @Override
                public V setValue(V value) {
                    throw new UnsupportedOperationException();
                }

            };
            current = indexArray[current + 1];
            return e;
        }
    }

    @Override
    public int size() {
        return size;
    }

}
