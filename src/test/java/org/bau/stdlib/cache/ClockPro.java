package org.bau.stdlib.cache;

import java.util.HashMap;

public class ClockPro<K, V> implements Cache<K, V> {

    private final HashMap<K, Value<K, V>> map = new HashMap<>();
    private Value<K, V> handCold;
    private Value<K, V> handHot;
    private Value<K, V> handTest;

    private int maxSize;
    private int maxSizeCold;
    private int maxSizeTest;

    private int countTest;
    private int countCold;
    private int countHot;

    ClockPro(int maxSize) {
        this.maxSize = maxSize;
        this.maxSizeCold = maxSize;
        this.maxSizeTest = maxSize * 2;
        handHot = new Value<>(null, null);
        handHot.next = handHot;
        handHot.prev = handHot;
        handCold = handHot;
        handTest = handHot;
    }

    @Override
    public V get(K key) {
        Value<K, V> val = map.get(key);
        if (val == null) {
            return null;
        }
        val.hit = true;
        return val.value;
    }

    private void metaDelete(Value<K, V> val) {
        map.remove(val.key);
        if (val == handHot) {
            handHot = handHot.prev;
        }
        if (val == handCold) {
            handCold = handCold.prev;
        }
        if (val == handTest) {
            handTest = handTest.prev;
        }
        removeFromQueue(val);
    }

    private void metaAdd(Value<K, V> val) {
        evict();
        map.put(val.key, val);
        addToQueue(handHot, val);
        if (handCold == handHot) {
            handCold = handCold.prev;
        }
    }

    private void evict() {
        toString();
        while (countHot + countCold > maxSize) {
            runHandCold();
        }
        toString();
    }

    private void runHandCold() {
        Value<K, V> m = handCold;
        toString();
        if (!m.hot && m.value != null) {
            if (m.hit) {
                m.hot = true;
                m.hit = false;
                countCold--;
                countHot++;
                toString();
            } else {
                m.value = null;
                countCold--;
                countTest++;
                toString();
                while (countTest > maxSizeTest) {
                    runHandTest();
                }
            }
        }
        handCold = handCold.next;
        while (countHot > maxSize - maxSizeCold) {
            runHandHot();
        }
    }

    void runHandHot() {
        toString();
        if (handHot == handTest) {
            runHandTest();
        }
        toString();
        Value<K, V> m = handHot;
        if (m.hot) {
            if (m.hit) {
                m.hit = false;
            } else {
                m.hot = false;
                countHot--;
                countCold++;
            }
        }
        handHot = handHot.next;
        toString();
    }

    void runHandTest() {
        toString();
        if (handTest == handCold) {
            runHandCold();
        }
        toString();
        if (handTest.key == null) {
            handTest = handTest.next;
        }
        Value<K, V> m = handTest;
        if (m.value == null) {
            Value<K, V> prev = handTest.prev;
            metaDelete(handTest);
            handTest = prev;
            countTest--;
            toString();
            if (maxSizeCold > 1) {
                maxSizeCold--;
            }
        }
        handTest = handTest.next;
        toString();
    }

    @Override
    public void put(K key, V value) {
        Value<K, V> val = map.get(key);
        if (val != null) {
            if (val.value != null) {
                // TODO adjust memory usage
                val.value = value;
                val.hit = true;
                return;
            }
            // test page
            if (maxSizeCold < maxSize) {
                maxSizeCold++;
            }
            val.hot = true;
            val.hit = false;
            val.value = value;
            countTest--;
            metaDelete(val);
            metaAdd(val);
            countHot++;
        } else {
            val = new Value<>(key, value);
            val.hot = false;
            metaAdd(val);
            countCold++;
        }
        toString();
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
        return "size=" + map.size() +
                " hot=" + countHot +
                " cold=" + countCold +
                " test=" + countTest +
                " max=" + maxSize +
                " maxCold=" + maxSizeCold;
    }

    private static class Value<K, V> {
        K key;
        V value;
        boolean hot;
        boolean hit;
        Value<K, V> next;
        Value<K, V> prev;
        Value(K key, V value) {
            this.key = key;
            this.value = value;
        }
        @Override
        public String toString() {
            return "(" + key + ":" + value + ")";
        }
    }

}
