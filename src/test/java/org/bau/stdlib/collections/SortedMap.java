package org.bau.stdlib.collections;

public interface SortedMap<K, V> {

    int size();

    void clear();

    void put(K key, V value);

    V get(K key);

    void remove(K key);

}
