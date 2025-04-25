package org.bau.stdlib.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.junit.Test;

public class SortedMapTest {

    @Test
    public void simple() {
        SkipList<Integer, String> list = new SkipList<>();
        list.put(1, "Hello");
        list.put(2, "World");
        assertEquals("Hello", list.get(1));
        assertEquals("World", list.get(2));
        Iterator<Entry<Integer, String>> it = list.iterate(2);
        assertTrue(it.hasNext());
        Entry<Integer, String> e = it.next();
        assertEquals((Integer) 2, e.getKey());
        assertEquals("World", e.getValue());
        assertFalse(it.hasNext());
        assertEquals(null, list.get(3));
        list.remove(1);
        assertEquals("World", list.get(2));
        list.remove(2);
        assertEquals(null, list.get(2));
        list.remove(3);
    }

    private static void log(Object... args) {
         // System.out.println("    " + Arrays.toString(args));
    }

    @Test
    public void randomAvlTree() {
        AVLTree<Integer, Long> myMap = new AVLTree<>();
        randomOps(myMap);
    }

    @Test
    public void randomSkipList() {
        SkipList<Integer, Long> myMap = new SkipList<>();
        randomOps(myMap);
    }

    public void randomOps(SortedMap<Integer, Long> myMap) {
        boolean findError = false;
        int smallestIndex = Integer.MAX_VALUE;
        nextSeed: for (int seed = 1; seed <= 1; seed++) {
            // System.out.println("seed: " + seed);
            Random random = new Random(seed);
            myMap.clear();
            TreeMap<Integer, Long> map = new TreeMap<>();
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

        // time: 341 ms 317012287
        // ConcurrentSkipListMap<Integer, Long> map = new ConcurrentSkipListMap<>();

        // time: 347 ms 317012287
        // SkipList<Integer, Long> map = new SkipList<>();

        // time: 206 ms 317012287
        // TreeMap<Integer, Long> map = new TreeMap<>();

        // time: 194 ms 317012287
        AVLTree<Integer, Long> map = new AVLTree<>();

        for (int i = 0; i < 1_000_000; i++) {
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
        System.out.println("time: " + time + " ms " + dummy);
    }

}
