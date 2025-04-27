package org.bau;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class TestOperators {

    @Test
    public void shiftRight() {
        Random r = new Random(1);
        for (int test = 0; test < 1000000; test++) {
            int i = r.nextInt();
            for (int n = 0; n < 64; n++) {
                int c1 = i >> n;
                // this would be wrong: int c2 = i / (1 << n);
                // see also https://en.wikipedia.org/wiki/Arithmetic_shift
                // from https://stackoverflow.com/questions/31879878/how-can-i-perform-arithmetic-right-shift-in-c-in-a-portable-way
                int s = -(i >>> 31);
                int c2 = ((s ^ i) >> n) ^ s;
                assertEquals(c1, c2);
            }
        }
    }

    @Test
    public void shift() {
        assertEquals(49152, 3L << -50);
        assertEquals(0, 3L >>> -50);
    }

}
