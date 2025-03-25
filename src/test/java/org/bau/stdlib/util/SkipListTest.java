package org.bau.stdlib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.junit.Test;

public class SkipListTest {

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

    private static void log(String message) {
        // System.out.println(message);
    }

    @Test
    public void random() {
        Random random = new Random(1);
        SkipList<Integer, Long> list = new SkipList<>();
        TreeMap<Integer, Long> map = new TreeMap<>();
        for (int i = 0; i < 10_000; i++) {
            int key = random.nextInt(1000);
            long value = random.nextInt(10_000);
            switch (random.nextInt(10)) {
            case 0:
            case 1:
                log("PUT " + key + "=" + value);
                list.put(key, value);
                map.put(key, value);
                Long exp = map.get(key);
                Long got = list.get(key);
                log("get " + key + "=" + exp + " got " + got);
                if (exp == null) {
                    assertTrue(got == null);
                } else {
                    assertEquals("i:" + i, exp, got);
                }

                break;
            case 2:
                log("REMOVE " + key);
                list.remove(key);
                map.remove(key);
                break;
            default:
                exp = map.get(key);
                got = list.get(key);
                log("get " + key + "=" + exp + " got " + got);
                if (exp == null) {
                    assertTrue(got == null);
                } else {
                    assertEquals("i:" + i, exp, got);
                }
            }
        }
    }

    @Test
    public void performanceLoop() {
        for(int i = 0; i<10; i++) {
            performance();
        }
    }

    public void performance() {
        long start = System.currentTimeMillis();
        int dummy = 0;
        Random random = new Random(1);
        // time: 344 ms 31755339
        SkipList<Integer, Long> map = new SkipList<>();
        // time: 210 ms 31755339
        // TreeMap<Integer, Long> map = new TreeMap<>();
        // time: 358 ms 31755339
        // ConcurrentSkipListMap<Integer, Long> map = new ConcurrentSkipListMap<>();
        for (int i = 0; i < 1_000_000; i++) {
            int key = random.nextInt(1_000_000);
            long value = random.nextInt(1000);
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
