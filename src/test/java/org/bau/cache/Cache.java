package org.bau.cache;

public interface Cache<K, V> {
    V get(K key);
    void put(K key, V value);
}
