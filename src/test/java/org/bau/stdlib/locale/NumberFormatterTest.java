package org.bau.stdlib.locale;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NumberFormatterTest {

    @Test
    public void format() {
        assertEquals("0", NumberFormatter.format(0, (byte) '_', 3, 3));
        assertEquals("1_234", NumberFormatter.format(1234, (byte) '_', 3, 3));
        assertEquals("1_234_567", NumberFormatter.format(1234567, (byte) '_', 3, 3));
        assertEquals("1_234_567_890", NumberFormatter.format(1234567890, (byte) '_', 3, 3));
        assertEquals("1_23_45_67_890", NumberFormatter.format(1234567890, (byte) '_', 3, 2));
        assertEquals("12_3456_7890", NumberFormatter.format(1234567890, (byte) '_', 4, 4));

        assertEquals("1 ' 234", NumberFormatter.format(1234, " ' ", 3, 3));
    }
}
