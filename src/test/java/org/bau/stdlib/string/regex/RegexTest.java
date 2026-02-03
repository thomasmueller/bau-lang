package org.bau.stdlib.string.regex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class RegexTest {

    @Test
    public void timeout() {
        assertMatches("1,2,3,4,5", "^.*,.*,.*,.*,.*$");
        assertMatches("1,2,3,4,5,6", "^.*,.*,.*,.*,.*,.*$");
        assertMatches("1,2,3,4,5,6,7", "^.*,.*,.*,.*,.*,.*,.*$");
        assertMatches("1,2,3,4,5,6,7,8", "^.*,.*,.*,.*,.*,.*,.*,.*$");
        try {
            assertMatches("1,2,3,4,5,6,7,8,9", "^.*,.*,.*,.*,.*,.*,.*,.*,.*$");
            fail();
        } catch (IllegalStateException e) {
            // expected: timeout
        }
    }

    @Test
    public void anchors() {
        assertMatches("ab", "^a|z$");
        assertMatches("az", "^a|b$");
        assertMatches("bz", "^a|b$");
        assertMatches("zb", "^a|b$");
        assertMatches("ba", "^a|b$");
    }

    @Test
    public void minMax() {
        assertMatches("1", "[0-9]{2}");
        assertMatches("12", "[0-9]{2}");
        assertMatches("123", "[0-9]{2}");
        assertMatches("1", "[0-9]{2,3}");
        assertMatches("12", "[0-9]{2,3}");
        assertMatches("123", "[0-9]{2,3}");
        assertMatches("1234", "[0-9]{2,3}");
        assertMatches("1", "[0-9]{2,}");
        assertMatches("12", "[0-9]{2,}");
        assertMatches("123", "[0-9]{2,}");
        assertMatches("1234", "[0-9]{2,}");
        assertTrue(Regex.matches("", "[0-9]{0,2}"));
        assertTrue(Regex.matches("1", "[0-9]{,2}"));
        assertTrue(Regex.matches("12", "[0-9]{,2}"));
        assertFalse(Regex.matches("123", "[0-9]{,2}"));
        assertTrue(Regex.matches("123", "[0-9]{,}"));
    }

    @Test
    public void replaceAll() {
        assertEquals("X", "".replaceAll("b?", "X"));
        assertEquals("", Regex.replaceAll("", "b?", "X"));
        assertEquals("XdXXX", "dbb".replaceAll("b?", "X"));
        assertEquals("dXX", Regex.replaceAll("dbb", "b?", "X"));
    }

    @Test
    public void escape() {
        // '-' can be included if it's the first or last
        assertMatches("a-a-", "[a-]+");
        assertMatches("a-a-", "[-a]+");
        // ']' can be included if it's the first in the list
        assertMatches("]]]", "[]a]+");
    }

    @Test
    public void matches() {
        assertMatches("adbaa", "(.)+a");
        assertMatches("dbaa", "(.)+a");
        assertMatches("acb", "(a.*)+b");
        assertMatches("baa", "(.)+a");
        assertMatches("dddd", ".+.");
        assertMatches("dddd", "(.+).");
        assertMatches("ba", "(.)+a");
        assertMatches("b", ".");
        assertMatches("acadb", ".");
        assertMatches("ad", "^.*.?b$");
        assertMatches("ac", "^.*ab?$");
        assertMatches("dda", "^.*b?$");
        assertMatches("aba", "^.*b?$");
        assertMatches("abcc", "^.*[^a-b]$");
        assertMatches("abc", "^abc$");
        assertMatches("abcd", "^abc$");
        assertMatches("hello abc world", "abc");
        assertMatches("hello abd world", "abc");
        assertMatches("aaabcc", "a+b+c+");
        assertMatches("aaabcc", "a+b+c+");
        assertMatches("123abc123", "abc");
        assertMatches("defabcdef", "abc");
    }

    private static void assertMatches(String text, String regex) {
        boolean got = Regex.matches(text, regex);
        boolean expected = text.matches(regex);
        if (expected != got) {
            fail("Regex.matches(\"" + text + "\", \"" + regex + "\"); // expected: " + expected + " got: " + got);
        }
    }

    @Test
    public void or() {
        assertMatches("foobarfoobar", "(foo|bar)+");
        assertMatches("barfoo", "(foo|bar)+");
    }

    @Test
    public void group() {
        assertMatches("lalala", "(la)+");
        assertMatches("lalalax", "(la)+");
    }

    @Test
    public void performance() {
        for (int test = 0; test < 5; test++) {
            String s = "hello world how are you?";
            for (int m = 0; m <= 1; m++) {
                Random r = new Random(1);
                int count = 10_000;
                int matchCount = 0;
                long start = System.nanoTime();
                for (int i = 0; i < count; i++) {
                    String regex = "^" + randomRegex(r, 10, false) + "$";
                    if (m == 0) {
                        matchCount += Regex.matches(s, regex) ? 1 : 0;
                    } else {
                        matchCount += s.matches(regex) ? 1 : 0;
                    }
                }
                long time = (System.nanoTime() - start) / count;
                // System.out.println("time: " + time + " m=" + (m == 0 ? "my " : "std") + " matchCount " + matchCount);
            }
        }
    }

    @Test
    public void replace() {
        assertEquals("bXbadddb", Regex.replaceFirst("bbabadddb", ".a", "X"));
        assertEquals("bXbadddb", Regex.replaceFirst("bbabadddb", "(.)a", "X"));
        assertEquals("c", Regex.replaceFirst("c", ".{2,2}", "X"));
        assertEquals("baNanna", "bannanna".replaceFirst("n+", "N"));
        assertEquals("baNanna", Regex.replaceFirst("bannanna", "n+", "N"));
        assertEquals("baNaNa", "bannanna".replaceAll("n+", "N"));
        assertEquals("baNaNa", Regex.replaceAll("bannanna", "n+", "N"));
    }

    @Test
    public void split() {
        assertEquals("[a, b, c]", Arrays.toString("a, b,   c".split(", *")));
        assertEquals("[a, b, c]", Arrays.toString(Regex.split("a, b,   c", ", *")));
    }

    @Test
    public void parseError() {
        try {
            Regex.find("a", "a{0,");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Regex.find("a", "a{0(");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Regex.find("a", "(abc](");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Regex.find("a", "(abc");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Regex.find("a", "[abc");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            assertMatches("$test", "test", false);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            assertMatches("test^", "test", false);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public static String randomRegex(Random r, int maxLen, boolean groups) {
        StringBuilder buff = new StringBuilder();
        int regexLen = 1 + r.nextInt(maxLen);
        for (int i = 0; i < regexLen; i++) {
            switch (r.nextInt(11)) {
            case 0:
            case 1:
            case 2:
                buff.append('a');
                break;
            case 3:
            case 4:
                buff.append('b');
                break;
            case 5:
                buff.append('.');
                break;
            case 6:
                buff.append(' ');
                break;
            case 7:
                buff.append('[');
                buff.append('a');
                buff.append('-');
                buff.append('b');
                buff.append(']');
                break;
            case 8:
                buff.append('[');
                buff.append('^');
                buff.append('a');
                buff.append('-');
                buff.append('b');
                buff.append(']');
                break;
            case 9:
                buff.append('\\');
                String any = "\\^$.|?*+()[]{}";
                buff.append(any.charAt(r.nextInt(any.length())));
                break;
            case 10:
                if (groups) {
                    buff.append('(');
                    buff.append(randomRegex(r, maxLen / 2 + 1, false));
                    buff.append(')');
                } else {
                    buff.append('a');
                }
                break;
            default:
                throw new IllegalStateException();
            }
            if (groups) {
                switch (r.nextInt(7)) {
                case 0:
                case 1:
                case 2:
                    break;
                case 3:
                    buff.append("+");
                    break;
                case 4:
                    buff.append("*");
                    break;
                case 5:
                    buff.append("?");
                    break;
                case 6:
                    int min, max;
                    while(true) {
                        min = r.nextInt(3);
                        max = min + r.nextInt(2);
                        if (min == 1 && max == 1) {
                            continue;
                        }
                        if (min == 0 && max == 1) {
                            continue;
                        }
                        break;
                    }
                    buff.append("{");
                    buff.append(min);
                    buff.append(",");
                    buff.append(max);
                    buff.append("}");
                    break;
                default:
                    throw new IllegalStateException();
                }
            }
        }
        return buff.toString();
    }

    @Test
    public void random() {
        Random r = new Random(1);
        int smallestFailure = Integer.MAX_VALUE;
        int smallestReplaceFirst = Integer.MAX_VALUE;
        int timeout = 0;
        int count = 10_000;
        for (int test = 0; test < count; test++) {
            StringBuilder buff = new StringBuilder();
            int len = 1 + r.nextInt(10);
            for (int i = 0; i < len; i++) {
                buff.append((char) ('a' + r.nextInt(4)));
            }
            String text = buff.toString();
            String regex = randomRegex(r, 9, true);
            if (r.nextInt(5) == 0) {
                if (r.nextBoolean()) {
                    regex = "^.*" + regex;
                } else {
                    regex = "^" + regex;
                }
            }
            if (r.nextInt(5) == 0) {
                regex += "$";
            }

            // matches
            boolean expected = text.matches(regex);
            boolean got;
            try {
                got = Regex.matches(text, regex);
            } catch (IllegalStateException e) {
                timeout++;
                continue;
            }
            if (expected != got) {
                if (regex.length() < smallestFailure) {
                    System.out.println("assertMatches(\"" + text + "\", \"" + regex + "\"); expected " + expected + " got " + got);
                    smallestFailure = regex.length();
                }
            }

            // replaceFirst
            String exp = text.replaceFirst(regex, "X");
            String g = Regex.replaceFirst(text, regex, "X");
            if (!exp.equals(g)) {
                if (regex.length() < smallestReplaceFirst) {
                    smallestReplaceFirst = regex.length();
                    /*
                    System.out.println("Regex.replaceFirst(\"" + text + "\", \"" + regex + "\", \"X\");");
                    System.out.println("expected: " + exp);
                    System.out.println("got:      " + g);
                    */
                }
                // assertEquals("Regex.replaceFirst(\"" + text + "\", \"" + regex + "\", \"X\");", exp, g);
            }
            String expa = text.replaceAll(regex, "X");
            String ga = Regex.replaceAll(text, regex, "X");
            if (!expa.equals(ga)) {
                if (regex.length() < smallestReplaceFirst) {
                    smallestReplaceFirst = regex.length();
                    /*
                    System.out.println("Regex.replaceAll(\"" + text + "\", \"" + regex + "\", \"X\");");
                    System.out.println("expected: " + expa);
                    System.out.println("got:      " + ga);
                    */
                }
                assertEquals("Regex.replaceAll(\"" + text + "\", \"" + regex + "\", \"X\");", exp, g);
            }
        }
        // System.out.println("count: " + count + " timeout: " + timeout);
        if (timeout > count * 0.01) {
            fail();
        }
    }

    @Test
    public void testSimpleLiteralMatching() {
        assertMatches("hello", "hello", true);
        assertMatches("hello", "world", false);
        assertFind("hello", "hello world", true);
        assertMatches("abc", "abc", true);
        assertMatches("abc", "def", false);
        assertFind("test", "this is a test", true);
    }

    @Test
    public void testDotWildcard() {
        assertMatches("h.llo", "hello", true);
        assertMatches("h.llo", "hallo", true);
        assertMatches("h.llo", "hillo", true);
        assertMatches("h.llo", "h", false);
        assertMatches(".", "a", true);
        assertMatches(".", "", false);
        assertMatches("a.c", "abc", true);
        assertMatches("a.c", "axc", true);
        assertMatches("a.c", "ac", false);
    }

    @Test
    public void testStartAnchor() {
        assertFind("^hello", "hello world", true);
        assertMatches("^hello", "say hello", false);
        assertMatches("^test", "test", true);
        assertMatches("^test", "pretest", false);
    }

    @Test
    public void testEndAnchor() {
        assertFind("world$", "hello world", true);
        assertFind("world$", "world peace", false);
        assertMatches("test$", "test", true);
        assertFind("test$", "testing", false);
    }

    @Test
    public void testBothAnchors() {
        assertMatches("^hello$", "hello", true);
        assertMatches("^hello$", "hello world", false);
        assertMatches("^hello$", "say hello", false);
        assertMatches("^test$", "test", true);
        assertMatches("^test$", "testing", false);
    }

    @Test
    public void testQuestionMarkQuantifier() {
        assertMatches("colou?r", "color", true);
        assertMatches("colou?r", "colour", true);
        assertMatches("colou?r", "colouur", false);
        assertMatches("ab?c", "ac", true);
        assertMatches("ab?c", "abc", true);
        assertMatches("ab?c", "abbc", false);
    }

    @Test
    public void testStarQuantifier() {
        assertMatches("ab*c", "ac", true);
        assertMatches("ab*c", "abc", true);
        assertMatches("ab*c", "abbc", true);
        assertMatches("ab*c", "abbbc", true);
         assertMatches("a*", "", true);
        assertMatches("a*", "aaa", true);
        assertMatches("a*b", "b", true);
        assertMatches("a*b", "aaab", true);
    }

    @Test
    public void testPlusQuantifier() {
        assertMatches("ab+c", "ac", false);
        assertMatches("ab+c", "abc", true);
        assertMatches("ab+c", "abbc", true);
        assertMatches("ab+c", "abbbc", true);
        assertMatches("a+", "a", true);
        assertMatches("a+", "aaa", true);
        assertMatches("a+", "", false);
    }

    @Test
    public void testCombinedPatterns() {
        assertMatches("^h.*o$", "hello", true);
        assertMatches("^h.*o$", "hero", true);
        assertMatches("^h.*o$", "hello world", false);
        assertMatches("a.+b", "axxxb", true);
        assertMatches("a.+b", "ab", false);
        assertMatches("^test.*$", "test123", true);
        assertMatches("^test.*$", "pretest", false);
    }

    @Test
    public void testEmptyAndEdgeCases() {
        assertMatches(".*", "anything", true);
        assertMatches("^$", "", true);
        assertMatches("^$", "a", false);
    }

    @Test
    public void testCharacterSets() {
        assertMatches("[abc]", "a", true);
        assertMatches("[abc]", "b", true);
        assertMatches("[abc]", "c", true);
        assertMatches("[abc]", "d", false);
        assertMatches("[abc]def", "adef", true);
        assertMatches("[abc]def", "bdef", true);
        assertMatches("[abc]def", "ddef", false);
        assertMatches("[^abc]", "d", true);
        assertMatches("[^abc]", "a", false);
        assertMatches("[^abc]", "b", false);
        assertMatches("[a-z]", "m", true);
        assertMatches("[a-z]", "A", false);
        assertMatches("[0-9]", "5", true);
        assertMatches("[0-9]", "a", false);
    }

    @Test
    public void testEscapedCharacters() {
        assertMatches("\\.", ".", true);
        assertMatches("\\.", "a", false);
        assertMatches("\\*", "*", true);
        assertMatches("\\+", "+", true);
        assertMatches("\\?", "?", true);
        assertMatches("\\(", "(", true);
        assertMatches("\\)", ")", true);
        assertMatches("\\[", "[", true);
        assertMatches("\\]", "]", true);
        assertMatches("\\{", "{", true);
        assertMatches("\\}", "}", true);
        assertFind("\\\n", "\n", true);
        assertFind("\\\t", "\t", true);
        assertFind("\\\r", "\r", true);
        assertMatches("line1\\\nline2", "line1\nline2", true);
    }

    @Test
    public void testConvenienceMethods() {
        assertTrue("Simple matches should work", Regex.find("hello world", "hello") != null);
        assertFalse("Non-matching should return false", Regex.matches("hello world", "xyz"));
    }


    private void assertMatches(String pattern, String input, boolean shouldMatch) {
        assertEquals(shouldMatch, Regex.matches(input, pattern));
    }

    private void assertFind(String pattern, String input, boolean shouldMatch) {
        assertEquals(shouldMatch, Regex.find(input, pattern) != null);
    }

}
