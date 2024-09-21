package org.bau.string;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class TestStringUtils {

    @Test
    public void split() {
        assertEquals("[]", Arrays.toString(StringUtils.split("", "")));
        assertEquals("[abc]", Arrays.toString(StringUtils.split("abc", "")));
        assertEquals("[abc]", Arrays.toString(StringUtils.split("abc", "x")));
        assertEquals("[a, b, c]", Arrays.toString(StringUtils.split("a|b|c", "|")));
        assertEquals("[, b, , ]", Arrays.toString(StringUtils.split("|b||", "|")));
        assertEquals("[, b, , ]", Arrays.toString(";b;;".split(";", -1)));
    }

    @Test
    public void replaceAll() {
        assertEquals("", StringUtils.replaceAll("", "", ""));
        assertEquals("abc", StringUtils.replaceAll("abc", "", ""));
        assertEquals("abc", StringUtils.replaceAll("abc", "", "x"));
        assertEquals("aabc", StringUtils.replaceAll("abc", "a", "aa"));
        assertEquals("ba", StringUtils.replaceAll("aaa", "aa", "b"));
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            StringBuilder original = new StringBuilder();
            StringBuilder replace = new StringBuilder();
            StringBuilder with = new StringBuilder();
            for (int j = r.nextInt(10); j >= 0; j--) {
                original.append("abc".charAt(r.nextInt(3)));
            }
            for (int j = r.nextInt(4); j >= 0; j--) {
                replace.append("abc".charAt(r.nextInt(3)));
            }
            for (int j = r.nextInt(4); j >= 0; j--) {
                replace.append("abc".charAt(r.nextInt(3)));
            }
            String expected = original.toString().replace(replace.toString(), with.toString());
            String got = StringUtils.replaceAll(original.toString(), replace.toString(), with.toString());
            assertEquals(expected, got);
        }
    }

}
