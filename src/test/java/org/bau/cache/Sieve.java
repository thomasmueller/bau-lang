package org.bau.cache;

import java.util.HashMap;

/**
 * Sieve cache.
 * See
 * https://news.ycombinator.com/item?id=38850202
 * https://github.com/cacheMon/NSDI24-SIEVE
 * https://github.com/ben-manes/caffeine/wiki/Simulator
 */
public class Sieve<K, V> implements Cache<K, V> {

    private final HashMap<K, Value<K, V>> map = new HashMap<>();
    private Value<K, V> head;
    private Value<K, V> hand;
    private int maxSize;

    Sieve(int maxSize) {
        this.maxSize = maxSize;
        head = new Value<>();
        hand = head;
        head.next = head;
        head.prev = head;
    }

    @Override
    public V get(K key) {
        Value<K, V> v = map.get(key);
        if (v == null) {
            return null;
        }
        v.visited = true;
        return v.value;
    }

    @Override
    public synchronized void put(K key, V value) {
        if (map.size() >= maxSize) {
            while (true) {
                if (hand == head) {
                    hand = hand.prev;
                }
                if (!hand.visited) {
                    break;
                }
                hand.visited = false;
                hand = hand.prev;
            }
            Value<K, V> old = hand;
            hand = hand.prev;
            removeFromQueue(old);
            map.remove(old.key);
        }
        Value<K, V> v = new Value<>();
        v.key = key;
        v.value = value;
        addToQueue(head, v);
        map.put(key, v);
    }

    public synchronized void remove(K key) {
        Value<K, V> v = map.remove(key);
        if (v != null) {
            if (hand == v) {
                hand = hand.prev;
            }
            removeFromQueue(v);
        }
    }

    private void removeFromQueue(Value<K, V> v) {
        v.prev.next = v.next;
        v.next.prev = v.prev;
        v.prev = null;
        v.next = null;
    }

    private void addToQueue(Value<K, V> head, Value<K, V> v) {
        v.prev = head;
        v.next = head.next;
        v.next.prev = v;
        head.next = v;
    }

    @Override
    public String toString() {
        return "size: " + map.size();
    }

    private static class Value<K, V> {
        K key;
        V value;
        boolean visited;
        Value<K, V> next;
        Value<K, V> prev;
        @Override
        public String toString() {
            return "(" + key + ":" + value + ")";
        }
    }

}
