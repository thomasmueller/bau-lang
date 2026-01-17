package org.bau.stdlib.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.Random;

import org.junit.Test;

public class RingBufferTest {

    @Test
    public void random() {
        ArrayDeque<Integer> gold = new ArrayDeque<>(10);
        RingBuffer<Integer> test = RingBuffer.build(10);
        assertEquals(16, test.capacity());
        Random random = new Random(1);
        for (int i = 0; i < 1_000; i++) {
            int value = random.nextInt(100);
            switch (random.nextInt(2)) {
            case 0:
                if (gold.size() < 20) {
                    if (gold.size() < 16) {
                        gold.add(value);
                        assertTrue(test.add(value));
                    } else {
                        assertFalse(test.add(value));
                    }
                }
                break;
            case 1:
                if (gold.size() > 0) {
                    int exp = gold.remove().intValue();
                    int got = test.remove().intValue();
                    assertEquals(exp, got);
                } else {
                    assertTrue(test.remove() == null);
                }
                break;
            }
            assertEquals(gold.size(), test.size());
            assertEquals(gold.isEmpty(), test.isEmpty());
            assertEquals(gold.size() == 16, test.isFull());
        }
    }

    @Test
    public void fail() {
        assertTrue(RingBuffer.build(-1).isFull());
        assertTrue(RingBuffer.build(0).isFull());
    }

    @Test
    public void simple() {
        RingBuffer<Integer> test = RingBuffer.build(1);
        assertTrue(test.isEmpty());
        assertFalse(test.isFull());
        assertEquals(0, test.size());
        assertTrue(test.add(10));
        assertFalse(test.isEmpty());
        assertTrue(test.isFull());
        assertEquals(1, test.size());
        assertFalse(test.add(20));
        assertEquals(10, test.remove().intValue());
        assertTrue(test.isEmpty());
        assertFalse(test.isFull());
        assertEquals(0, test.size());
    }
}
