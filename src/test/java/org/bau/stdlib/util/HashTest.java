package org.bau.stdlib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

public class HashTest {

    @Test
    public void hash() {
        assertEquals(0, Hash.hash(0));
        assertTrue(1 != Hash.hash(1));
    }

    @Test
    public void hashBytes() {
        assertEquals(0, Hash.hashCode(new byte[0]));
        assertEquals(-5509026399873832431L, Hash.hashCode(new byte[100]));
        assertEquals(-4519909738722218942L, Hash.hashCode(new byte[200]));
    }

    @Test
    public void reduce() {
        Random r = new Random(0);
        for (int i = 0; i < 1_000_000; i++) {
            long x = r.nextLong();
            int max = r.nextInt(10_000);
            int got = Hash.reduce(x, max);
            if (max == 0) {
                assertEquals(0, got);
            } else {
                assertTrue(got >= 0 && got < max);
                assertEquals(0, Hash.reduce(0, max));
                assertEquals(max - 1, Hash.reduce(-1, max));
            }
        }
    }

}
