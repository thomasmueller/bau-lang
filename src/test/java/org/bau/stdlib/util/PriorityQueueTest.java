package org.bau.stdlib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.PriorityQueue;
import java.util.Random;

import org.junit.Test;

public class PriorityQueueTest {

    @Test
    public void simple() {
        PriorityHeap<Integer> priorityQueue = new PriorityHeap<>();
        priorityQueue.add(10);
        priorityQueue.add(20);
        priorityQueue.add(5);
        assertEquals(5, priorityQueue.peek().intValue());
    }

    @Test
    public void random() {
        Random random = new Random(1);
        PriorityHeap<Integer> priorityHeap = new PriorityHeap<>();
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>();
        for (int i = 0; i < 100_000; i++) {
            int value = random.nextInt(1000);
            priorityHeap.add(value);
            priorityQueue.add(value);
            assertEquals(priorityHeap.peek(), priorityQueue.peek());
            if (priorityQueue.size() > 1000) {
                assertEquals(priorityHeap.poll(), priorityQueue.poll());
            }
            if (random.nextInt(1000) == 0) {
                while (!priorityQueue.isEmpty()) {
                    assertFalse(priorityHeap.isEmpty());
                    assertEquals(priorityHeap.poll(), priorityQueue.poll());
                }
                assertTrue(priorityHeap.isEmpty());
                assertNull(priorityHeap.peek());
                assertNull(priorityHeap.poll());
            }
        }
    }

}
