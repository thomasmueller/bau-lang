package org.bau.stdlib.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.Test;

public class HashTableTest {

    public static void main(String... args) {
        memoryUsage();
    }

    public static void memoryUsage() {
        // -mx1g: i=12000000
        // HashMap<Integer, Long> map = new HashMap<>();
        // -mx1g: i=16000000 (newCapacity=33554432)
        // -mx2g: i=33000000
        HashTable<Integer, Long> map = new HashTable<>();
        Random r = new Random();
        for (int i = 0; i < 1_000_000_000; i++) {
            int key = r.nextInt();
            map.put(key, key * 10L);
            if (i % 1_000_000 == 0) {
                System.out.println("i=" + i);
            }
        }
    }

    @Test
    public void performanceLoop() {
        for (int i = 0; i < 10; i++) {
            // performance();
        }
    }

    public void performance() {
        long start = System.currentTimeMillis();
        int dummy = 0;
        Random random = new Random(1);

        // time: 300 ms -1253345464
        // HashMap<Integer, Long> map = new HashMap<>();

        // time: 299 ms -1253345464
        HashTable<Integer, Long> map = new HashTable<>();

        for (int i = 0; i < 10_000_000; i++) {
            int key = random.nextInt(1_000_000);
            long value = random.nextInt(10_000);
            switch (random.nextInt(10)) {
            case 0:
            case 1:
                map.put(key, value);
                break;
            case 2:
                map.remove(key);
                break;
            default:
                Long got = map.get(key);
                if (got != null) {
                    dummy += got;
                }
            }
        }
        long time = System.currentTimeMillis() - start;
        // System.out.println("time: " + time + " ms " + dummy);
    }

    @Test
    public void simple() {
        HashTable<Integer, String> list = new HashTable<>();
        list.put(1, "Hello");
        list.put(2, "World");
        assertEquals("Hello", list.get(1));
        assertEquals("World", list.get(2));
        assertEquals(null, list.get(3));
        list.remove(1);
        assertEquals("World", list.get(2));
        list.remove(3);
        list.remove(1);
        Iterator<Entry<Integer, String>> it = list.iterator();
        assertTrue(it.hasNext());
        Entry<Integer, String> e = it.next();
        assertEquals((Integer) 2, e.getKey());
        assertEquals("World", e.getValue());
        assertFalse(it.hasNext());
        list.remove(2);
        assertEquals(null, list.get(2));
    }

    private static void log(Object... args) {
        // System.out.println("    " + Arrays.toString(args));
    }

    @Test
    public void random() {
        HashTable<Integer, Long> myMap = new HashTable<>();
        randomOps(myMap);
    }

    public void randomOps(HashTable<Integer, Long> myMap) {
        boolean findError = false;
        int smallestIndex = Integer.MAX_VALUE;
        nextSeed: for (int seed = 1; seed <= 1; seed++) {
            // System.out.println("seed: " + seed);
            Random random = new Random(seed);
            myMap.clear();
            HashMap<Integer, Long> map = new HashMap<>();
            for (int i = 0; i < 200_000; i++) {
                int key = random.nextInt(1000);
                long value = random.nextInt(10_000);
                switch (random.nextInt(10)) {
                case 0:
                case 1:
                    log("PUT ", key, "=", value);
                    myMap.put(key, value);
                    map.put(key, value);
                    Long exp = map.get(key);
                    Long got = myMap.get(key);
                    log("get ", key, "=", exp, " got ", got);
                    if (exp == null) {
                        assertTrue(got == null);
                    } else {
                        if (!exp.equals(got)) {
                            if (findError) {
                                if (i < smallestIndex) {
                                    smallestIndex = i;
                                    System.out.println("seed: " + seed + " i: " + i);
                                }
                                continue nextSeed;
                            }
                            assertEquals("i:" + i, exp, got);
                        }
                    }
                    break;
                case 2:
                    log("REMOVE " + key);
                    myMap.remove(key);
                    if (myMap.get(key) != null) {
                        if (findError) {
                            if (i < smallestIndex) {
                                smallestIndex = i;
                                System.out.println("seed: " + seed + " i: " + i);
                            }
                            continue nextSeed;
                        }
                        assertNull(myMap.get(key));
                    }
                    map.remove(key);
                    break;
                default:
                    exp = map.get(key);
                    got = myMap.get(key);
                    log("get ", key, "=", exp, " got ", got);
                    if (exp == null) {
                        assertTrue(got == null);
                    } else {
                        if (!exp.equals(got)) {
                            if (findError) {
                                if (i < smallestIndex) {
                                    smallestIndex = i;
                                    System.out.println("seed: " + seed + " i: " + i);
                                }
                                continue nextSeed;
                            }
                            assertEquals(exp, got);
                        }
                    }
                }
                assertEquals(map.size(), myMap.size());
            }
        }
    }

}
