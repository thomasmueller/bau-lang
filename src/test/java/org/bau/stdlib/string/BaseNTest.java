package org.bau.stdlib.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BaseNTest {

    @Test
    public void simple() {
        assertEquals(01234567, BaseN.parseInt("1234567", 8));
        assertEquals("1234567", BaseN.toString(01234567, 8));
        assertEquals(0b11110000, BaseN.parseInt("11110000", 2));
        assertEquals("11110000", BaseN.toString(0b11110000, 2));

        assertEquals(-1, BaseN.parseInt("1234567", 1));
        assertEquals(-1, BaseN.parseInt("1234567", 11));
        assertTrue(BaseN.toString(1234567, 1) == null);
        assertTrue(BaseN.toString(1234567, 11) == null);
    }

    @Test
    public void loop() {
        for(int base = 2; base <= 10; base++) {
            for(long x = 0; x >= 0; x = (long) ((x + 3) * 1.5)) {
                String s = Long.toString(x, base);
                assertEquals(s, BaseN.toString(x, base));
                assertEquals(x, BaseN.parseInt(s, base));
            }
        }
    }
}
