package org.bau.string.regex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class RegexTest {

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
        boolean expected = text.matches(regex);
        boolean got = Regex.matches(text, regex);
        if (expected != got) {
            fail("Regex.matches(\"" + text + "\", \"" + regex + "\"); // expected: " + expected + " got: " + got);
        }
    }

    private String parseAndConvert(String regex) {
        ArrayList<Token> list = RegexParser.parse(regex);
        StringBuilder buff = new StringBuilder();
        for (Token t : list) {
            buff.append(t.toString());
        }
        return buff.toString();
    }

    @Test
    public void or() {
        assertMatches("foobarfoobar", "(foo|bar)+");
        assertMatches("barfoo", "(foo|bar)+");
    }

    @Test
    public void group() {
        assertEquals("(a)b", parseAndConvert("(a)b"));
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
                        // ArrayList<Token> list = RegexParser.parse(regex);
                        matchCount += Regex.matches(s, regex) ? 1 : 0;
                    } else {
                        // Pattern p = Pattern.compile(regex);
                        matchCount += s.matches(regex) ? 1 : 0;
                    }
                }
                long time = (System.nanoTime() - start) / count;
                System.out.println("time: " + time + " m=" + (m == 0 ? "my " : "std") + " matchCount " + matchCount);
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

            // tokenization
            String r2 = parseAndConvert(regex);
            assertEquals(r2, regex);

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
                    System.out.println("Regex.replaceFirst(\"" + text + "\", \"" + regex + "\", \"X\");");
                    System.out.println("expected: " + exp);
                    System.out.println("got:      " + g);
                }
                // assertEquals("Regex.replaceFirst(\"" + text + "\", \"" + regex + "\", \"X\");", exp, g);
            }
            String expa = text.replaceAll(regex, "X");
            String ga = Regex.replaceAll(text, regex, "X");
            if (!expa.equals(ga)) {
                if (regex.length() < smallestReplaceFirst) {
                    smallestReplaceFirst = regex.length();
                    System.out.println("Regex.replaceAll(\"" + text + "\", \"" + regex + "\", \"X\");");
                    System.out.println("expected: " + expa);
                    System.out.println("got:      " + ga);
                }
                assertEquals("Regex.replaceAll(\"" + text + "\", \"" + regex + "\", \"X\");", exp, g);
            }
        }
        System.out.println("count: " + count + " timeout: " + timeout);
        if (timeout > count * 0.01) {
            fail();
        }
    }

}
