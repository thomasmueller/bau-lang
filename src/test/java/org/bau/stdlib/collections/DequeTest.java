package org.bau.stdlib.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.Random;

import org.junit.Test;

public class DequeTest {

    @Test
    public void random() {
        ArrayDeque<Integer> gold = new ArrayDeque<>(10);
        Deque<Integer> test = new Deque<>(10);
        Random random = new Random(1);
        for (int i = 0; i < 1_000; i++) {
            int value = random.nextInt(100);
            switch (random.nextInt(10)) {
            case 0:
                if (gold.size() < 100) {
                    gold.add(value);
                    assertTrue(test.add(value));
                }
                break;
            case 1:
                if (gold.size() < 100) {
                    gold.addFirst(value);
                    assertTrue(test.addFirst(value));
                }
                break;
            case 2:
                if (!gold.isEmpty()) {
                    int expected = gold.remove().intValue();
                    int got = test.remove().intValue();
                    assertEquals(expected, got);
                }
                break;
            case 3:
                if (!gold.isEmpty()) {
                    int expected = gold.removeLast().intValue();
                    int got = test.removeLast().intValue();
                    assertEquals(expected, got);
                }
                break;
            case 4:
                if (random.nextInt(10) == 0) {
                    while (!gold.isEmpty()) {
                        if (random.nextBoolean()) {
                            int expected = gold.remove().intValue();
                            int got = test.remove().intValue();
                            assertEquals(expected, got);
                        } else {
                            int expected = gold.removeLast().intValue();
                            int got = test.removeLast().intValue();
                            assertEquals(expected, got);
                        }
                    }
                }
            }
            assertEquals(gold.size(), test.size());
            assertEquals(gold.isEmpty(), test.isEmpty());
        }
    }

}
