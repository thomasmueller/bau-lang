package org.bau.stdlib.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class HexEncodeTest {

    @Test
    public void performance() {
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            test();
            long time = System.currentTimeMillis() - start;
            System.out.println("time: " + time);
        }
    }

    @Test
    public void illegal() {
        assertNull(HexEncode.convertHexToBytes(".."));
        assertNull(HexEncode.convertHexToBytes("gg"));
        assertNull(HexEncode.convertHexToBytes("ag"));
        assertNull(HexEncode.convertHexToBytes("GA"));
        assertNull(HexEncode.convertHexToBytes("110"));
    }

    @Test
    public void nibble() {
        for (int i = 0; i < 256; i++) {
            int lower = i | 0x20;
            if (i >= 'A' && i <= 'F') {
                assertEquals(i - 'A' + 'a', lower);
            } else if (i >= '0' && i <= '9') {
                assertEquals(i, lower);
            }
            int x = HexEncode.nibble((char) i);
            if (i >= '0' && i <= '9') {
                assertEquals(i - '0', x);
            } else if (i >= 'a' && i <= 'f') {
                assertEquals(i - 'a' + 10, x);
            } else if (i >= 'A' && i <= 'F') {
                assertEquals(i - 'A' + 10, x);
            } else {
                if (x >= 0 && x <= 0xf) {
                    fail();
                    // System.out.println("? " + ((char) i) + " => " + x + " " + i);
                    // HexEncode.nibble((char) i);
                }
            }
        }
    }

    @Test
    public void test() {
        Random r = new Random(1);
        byte[] data = new byte[16];
        for (int i = 0; i < 100_000; i++) {
            r.nextBytes(data);
            String hex = HexEncode.convertBytesToHex(data);
            byte[] test = HexEncode.convertHexToBytes(hex);
            assertTrue(Arrays.equals(data, test));
        }
    }

}
