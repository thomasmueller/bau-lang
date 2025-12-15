package org.bau.stdlib.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void isX() {
        for (int i = 0; i < 256; i++) {
            if ((" " + (char) 9 + (char) 10 + (char) 11 + (char) 12 + (char) 13).indexOf(i) >= 0) {
                assertTrue(StringUtils.isSpace((char) i));
            } else {
                assertFalse("" + i, StringUtils.isSpace((char) i));
            }
            if (i >= '0' && i <= '9') {
                assertTrue(StringUtils.isDigit((char) i));
            } else {
                assertFalse(StringUtils.isDigit((char) i));
            }
        }
    }

    @Test
    public void upper() {
        for (int i = 0; i < 256; i++) {
            String t = "" + (char) i;
            char[] upperChars = t.toUpperCase(Locale.ROOT).toCharArray();
            int upper;
            if (upperChars.length > 1 || upperChars[0] > 0xff) {
                upper = i;
            } else {
                upper = upperChars[0];
            }
            assertEquals("char " + i, upper, (int) StringUtils.toUpperChar((char) i));
        }
    }

    @Test
    public void lower() {
        for (int i = 0; i < 256; i++) {
            String t = "" + (char) i;
            char[] lowerChars = t.toLowerCase(Locale.ROOT).toCharArray();
            int lower;
            if (lowerChars.length > 1 || lowerChars[0] > 0xff) {
                lower = i;
            } else {
                lower = lowerChars[0];
            }
            assertEquals("char " + i, lower, (int) StringUtils.toLowerChar((char) i));
        }
    }

    @Test
    public void trim() {
        assertEquals("test", StringUtils.trim(" test "));
        assertEquals("test ", StringUtils.trimLeading(" test "));
        assertEquals(" test", StringUtils.trimTrailing(" test "));
        assertEquals("", StringUtils.trim(" "));
        assertEquals("", StringUtils.trimLeading(" "));
        assertEquals("", StringUtils.trimTrailing(" "));
    }

    @Test
    public void toUpperLower() {
        assertEquals("HELLO", StringUtils.toUpper("Hello"));
        assertEquals("HELLO", StringUtils.toUpper("HELLO"));
        assertEquals("HELLO WORLD", StringUtils.toUpper("hello World"));
        assertEquals("hello", StringUtils.toLower("Hello"));
        assertEquals("hello", StringUtils.toLower("hello"));
        assertEquals("hello world", StringUtils.toLower("hello World"));
    }

    @Test
    public void compareCaseInsensitive() {
        assertEquals(0, StringUtils.compareCaseInsensitive("hello", "HELLO"));
        assertEquals(-1, StringUtils.compareCaseInsensitive("hello", "hello world"));
        assertEquals(1, StringUtils.compareCaseInsensitive("hello world", "hello"));
        assertEquals(-1, StringUtils.compareCaseInsensitive("hello", "world"));
        assertEquals(-1, StringUtils.compareCaseInsensitive("hello 1", "hello 2"));
        assertEquals(1, StringUtils.compareCaseInsensitive("hello 2", "hello 1"));
    }

    @Test
    public void naturalSort() {
        // https://github.com/sourcefrog/natsort.git
        String[] list = new String[] { "1.001", "1.002", "1.010", "1.02", "1.1", "1.3",
                "a", "a0", "a1", "a1a", "a1b", "a2", "a10", "a20",
                "a space 1", "a    space  2", "a space      3",
                "x2-g8", "x2-y08", "x2-y7", "x8-y8",
                "za-0!", "za-0a", "za-01", "za-01z", "zb-20",
                "zb-101", "zb-102", "zb-201", "zc 10", "zc11", "zc 12" };
        for (int i = 0; i < list.length; i++) {
            if (i > 0) {
                String prev = list[i - 1];
                String current = list[i];
                assertEquals(prev + "<" + current, -1, StringUtils.naturalCompare(prev, current));
                assertEquals(current + ">" + prev, 1, StringUtils.naturalCompare(current, prev));
                assertEquals(0, StringUtils.naturalCompare(current, current));
            }
        }
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            String x = randomString(r, "  0123ab!AB.~");
            String y = randomString(r, "  0123ab!AB.~");
            StringUtils.naturalCompare(x, y);
            StringUtils.naturalCompare(y, x);
            StringUtils.naturalCompare(x, x);
            StringUtils.naturalCompare(x, y, true);
            StringUtils.naturalCompare(y, x, true);
            StringUtils.naturalCompare(x, x, true);
        }
    }

    private String randomString(Random r, String string) {
        int len = r.nextInt(10);
        StringBuilder buff = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            buff.append(string.charAt(r.nextInt(string.length())));
        }
        return buff.toString();
    }

    @Test
    public void split() {
        assertEquals("[abc]", Arrays.toString(StringUtils.split("abc", 'x')));
        assertEquals("[a, b, c]", Arrays.toString(StringUtils.split("a|b|c", '|')));
        assertEquals("[, b, , ]", Arrays.toString(StringUtils.split("|b||", '|')));
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

    @Test
    public void repeat() {
        assertEquals("", StringUtils.repeat((byte) 'b', 0));
        assertEquals("xxxxxxxxxx", StringUtils.repeat((byte) 'x', 10));
    }

    @Test
    public void reverse() {
        assertEquals("olleh", StringUtils.reverse("hello"));
        assertEquals("", StringUtils.reverse(""));
    }

}
