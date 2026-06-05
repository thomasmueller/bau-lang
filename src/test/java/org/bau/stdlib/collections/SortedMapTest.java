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
        SkipList3<Integer, String> list = new SkipList3<>();
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

    @Test
    public void higherKey() {
        SplayTree<Integer, String> list = new SplayTree<>();
        list.put(1, "Hello");
        list.put(2, "World");
        assertEquals(1, list.higherKey(null).intValue());
        assertEquals(2, list.higherKey(1).intValue());
        assertEquals(null, list.higherKey(2));
    }

    private static void log(Object... args) {
        if ("size".equals(args[0])) {
            return;
        }
        if ("get ".equals(args[0])) {
            return;
        }
        // System.out.println("    " + Arrays.toString(args));
    }

    @Test
    public void randomAvlTree() {
        AVLTree<Integer, Long> myMap = new AVLTree<>();
        randomOps(myMap);
    }

    @Test
    public void randomTreap() {
        Treap<Integer, Long> myMap = new Treap<>();
        randomOps(myMap);
    }

    @Test
    public void randomScapegoatTree() {
        ScapegoatTree<Integer, Long> myMap = new ScapegoatTree<>();
        randomOps(myMap);
    }

    @Test
    public void randomSkipList() {
        SkipList<Integer, Long> myMap = new SkipList<>();
        randomOps(myMap);
    }

    @Test
    public void randomSkipList2() {
        SkipList2<Integer, Long> myMap = new SkipList2<>();
        randomOps(myMap);
    }

    @Test
    public void randomSkipList3() {
        SkipList3<Integer, Long> myMap = new SkipList3<>();
        randomOps(myMap);
    }

    @Test
    public void randomSplayTree() {
        SplayTree<Integer, Long> myMap = new SplayTree<>();
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
            for (int i = 0; i < 50_000; i++) {
                log("size", map.size(), map.size() == myMap.size() ? "=" : "<>", myMap.size());
                assertEquals(map.size(), myMap.size());
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
                    Integer ke = map.higherKey(key);
                    Integer kg = myMap.higherKey(key);
                    log("higherKey ", key, "=", ke, " got ", kg);
                    if (ke == null || kg == null) {
                        assertTrue(ke == null && kg == null);
                    } else {
                        assertEquals("higherKey " + key, ke, kg);
                    }
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
                            assertEquals("get " + key, exp, got);
                        }
                    }
                }
            }
        }
    }

    // @Test
    public void performanceLoop() {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < 10; i++) {
            long time = performance();
            best = Math.min(best, time);
        }
        System.out.println("best " + best);
    }

    public long performance() {
        long start = System.currentTimeMillis();
        int dummy = 0;
        Random random = new Random(1);

        // Profiler prof = new Profiler().startCollecting();

        // time: 258 ms 317012287
        // ConcurrentSkipListMap<Integer, Long> map = new ConcurrentSkipListMap<>();

        // time: 245 ms 317012287
        // SkipList<Integer, Long> map = new SkipList<>();

        // time: 227 ms 317012287
        // SkipList2<Integer, Long> map = new SkipList2<>();

        // time: 240 ms 317012287
        // SkipList3<Integer, Long> map = new SkipList3<>();

        // time: 151 ms 317012287
        // TreeMap<Integer, Long> map = new TreeMap<>();

        // time: 157 ms 317012287
        // AVLTree<Integer, Long> map = new AVLTree<>();

        // time: 167 ms 317012287
        // ScapegoatTree<Integer, Long> map = new ScapegoatTree<>();

        // time: 195 ms 317012287
        Treap<Integer, Long> map = new Treap<>();

        // time: 335 ms 317012287
        // SplayTree<Integer, Long> map = new SplayTree<>();

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
        assertTrue(dummy >= 0);
        System.out.println("time: " + time + " ms " + dummy);

//        System.out.println(prof.getTop(10));

        return time;
    }

}
