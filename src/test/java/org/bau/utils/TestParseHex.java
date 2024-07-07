package org.bau.utils;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.bau.parser.NumberValue;
import org.junit.Test;

public class TestParseHex {
    @Test
    public void test() {
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            long x = r.nextLong();
            String hex = Long.toHexString(x);
            long y = NumberValue.parseUnsignedHexLong(hex);
            assertEquals(x, y);
        }
    }
}
