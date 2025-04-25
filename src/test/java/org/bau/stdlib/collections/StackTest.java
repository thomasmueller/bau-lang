package org.bau.stdlib.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StackTest {

    @Test
    public void test() {
        Stack<Integer> stack = new Stack<>(5);
        assertTrue(stack.push(10));
        assertTrue(stack.push(20));
        assertTrue(stack.push(30));
        assertTrue(stack.push(40));
        assertTrue(stack.push(50));
        assertFalse(stack.push(60));
        assertEquals(50, stack.peek().intValue());
        assertEquals(50, stack.pop().intValue());
        assertEquals(40, stack.pop().intValue());
        assertEquals(30, stack.pop().intValue());
        assertEquals(20, stack.pop().intValue());
        assertEquals(10, stack.pop().intValue());
        assertEquals(null, stack.pop());
        assertEquals(null, stack.peek());
    }
}
