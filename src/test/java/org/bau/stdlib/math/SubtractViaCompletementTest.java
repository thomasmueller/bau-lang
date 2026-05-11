package org.bau.stdlib.math;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class SubtractViaCompletementTest {

    @Test
    public void test() {
        Random r = new Random(1);
        for (int i = 0; i < 1000000; i++) {
            int a = r.nextInt(100);
            int b = r.nextInt(100);
            if (a < b) {
                int t = a;
                a = b;
                b = t;
            }
            int exp = a - b;
            // System.out.println(a + " - " + b + " = " + exp);
            int comp = 0;
            for (int s = 1; s < 100; s *= 10) {
                int c = 9 - ((b / s) % 10);
                if (s == 1) {
                    c += 1;
                }
                comp += s * c;
            }
            int test = a + comp - 100;
            assertEquals(exp, test);
        }
    }
}
