package org.bau.stdlib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class UnicodeTest {

    @Test
    public void random() {
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            byte[] original = new byte[r.nextInt(10)];
            r.nextBytes(original);
            byte[] normalized = Unicode.normalize(original);
            String s = new String(original, StandardCharsets.UTF_8);
            byte[] data = s.getBytes(StandardCharsets.UTF_8);
            if (data.length != normalized.length || !Arrays.equals(data, normalized)) {
                System.out.println("original");
                for (byte x : original) {
                    System.out.print(Long.toBinaryString(0x100 | (x & 0xff)).substring(1) + ":");
                    System.out.printf("%2x ",   (int) (x & 0xff));
                }
                System.out.println();
                System.out.println("got");
                for (byte x : normalized) {
                    System.out.print(Long.toBinaryString(0x100 | (x & 0xff)).substring(1) + ":");
                    System.out.printf("%2x ",   (int) (x & 0xff));
                }
                System.out.println();
                System.out.println("expected");
                for (byte x : data) {
                    System.out.print(Long.toBinaryString(0x100 | (x & 0xff)).substring(1) + ":");
                    System.out.printf("%2x ",   (int) (x & 0xff));
                }
                System.out.println();
                normalized = Unicode.normalize(original);
            }
            assertEquals(data.length, normalized.length);
            assertTrue(Arrays.equals(data, normalized));
            int expected = s.codePointCount(0, s.length());
            int got = Unicode.codePointCount(data);
            if (expected != got) {
                for (byte x : data) {
                    System.out.printf("%2x ", (int) (x & 0xff));
                }
                System.out.println();
                got = Unicode.codePointCount(data);
            }
            assertEquals(expected, got);
        }
    }
}
