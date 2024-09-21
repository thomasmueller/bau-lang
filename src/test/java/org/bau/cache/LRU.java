package org.bau.cache;

import java.util.HashMap;

public class LRU<K, V> implements Cache<K, V> {

    private final HashMap<K, Value<K, V>> map = new HashMap<>();
    private Value<K, V> head;
    private int maxSize;

    LRU(int maxSize) {
        this.maxSize = maxSize;
        head = new Value<>();
        head.next = head;
        head.prev = head;
    }

    void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public synchronized V get(K key) {
        Value<K, V> v = map.get(key);
        if (v == null) {
            return null;
        }
        removeFromQueue(v);
        addToQueue(v);
        return v.value;
    }

    public void remove(K key) {
        Value<K, V> v = map.remove(key);
        if (v == null) {
            return;
        }
        removeFromQueue(v);
    }

    @Override
    public synchronized void put(K key, V value) {
        Value<K, V> v = new Value<>();
        v.key = key;
        v.value = value;
        addToQueue(v);
        map.put(key, v);
        if (map.size() > maxSize) {
            Value<K, V> old = head.prev;
            removeFromQueue(old);
            map.remove(old.key);
        }
    }

    private void removeFromQueue(Value<K, V> v) {
        v.prev.next = v.next;
        v.next.prev = v.prev;
        v.prev = null;
        v.next = null;
    }

    @Override
    public String toString() {
        return "size: " + map.size();
    }

    private void addToQueue(Value<K, V> v) {
        v.prev = head;
        v.next = head.next;
        v.next.prev = v;
        head.next = v;
    }

    private static class Value<K, V> {
        K key;
        V value;
        Value<K, V> next;
        Value<K, V> prev;
    }

}
