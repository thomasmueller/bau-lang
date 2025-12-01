package org.bau.stdlib.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Random;

import org.junit.Test;

public class UnicodeTest {

    public static void main(String... args) {
        // 9079523
        Collator collator = Collator.getInstance(Locale.ROOT);
        collator.setStrength(Collator.PRIMARY);
        System.out.println("With Collator.PRIMARY, Locale.ROOT");
        System.out.println("'O' and 'o' are considered " +
                (collator.compare("O", "o") == 0 ? "the same" : "different"));
        System.out.println("'Ö' and 'ö' are considered " +
                (collator.compare("Ö", "ö") == 0 ? "the same" : "different"));
        System.out.println("'\u00D8' and '\u00F8' are considered " +
                (collator.compare("\u00D8", "\u00F8") == 0 ? "the same" : "different"));
    }

    @Test
    public void compare() {
        int[] strengths = new int[] {
                Collator.PRIMARY,
                Collator.SECONDARY,
                Collator.TERTIARY,
                Collator.IDENTICAL
        };
        String[] texts = new String[256];
        Locale locale = Locale.ROOT;
        for (int strength : strengths) {
            for (int i = 0; i < texts.length; i++) {
                String t = "" + (char) i;
                texts[i] = t;
            }
            Collator coll = Collator.getInstance(locale);
            coll.setStrength(strength);
            Arrays.sort(texts, coll);
            String last = "";
            for (int i = 0; i < texts.length; i++) {
                String t = texts[i];

                int cmp = last.length() == 0 ? 0 : coll.compare(t, last);
                if (strength == 1 && t.charAt(0) == 248) {
                    // 9079523
                    cmp = 0;
                }
                cmp = (int) Math.signum(cmp);
                int myCmp = last.length() == 0 ? 0 : Unicode.compare(t, last, strength);
                myCmp = (int) Math.signum(myCmp);
                assertEquals(cmp, myCmp);

                // reverse
                cmp = last.length() == 0 ? 0 : coll.compare(last, t);
                if (strength == 1 && t.charAt(0) == 248) {
                    // 9079523
                    cmp = 0;
                }
                cmp = (int) Math.signum(cmp);
                myCmp = last.length() == 0 ? 0 : Unicode.compare(last, t, strength);
                myCmp = (int) Math.signum(myCmp);
                assertEquals(cmp, myCmp);

                // equality
                assertEquals(0, Unicode.compare(t, t, strength));

                last = t;
            }
        }
    }

    // @Test
    public void buildCollationTable() throws NoSuchAlgorithmException {
        String[] texts = new String[256];
        int[][] posTable = new int[2][256];
        int[] strengths = new int[] {
                Collator.PRIMARY,
                Collator.TERTIARY,
        };
        Locale locale = Locale.ROOT;
        for (int strength : strengths) {
            for (int i = 0; i < texts.length; i++) {
                String t = "" + (char) i;
                texts[i] = t;
            }
            Collator coll = Collator.getInstance(locale);
            coll.setStrength(Collator.IDENTICAL);
            Arrays.sort(texts, coll);
            coll.setStrength(strength);
            Arrays.sort(texts, coll);
            String last = "";
            int pos = 0;
            for (int i = 0; i < texts.length; i++) {
                String t = texts[i];
                int cmp = last.length() == 0 ? 0 : coll.compare(t, last);
                char c = t.charAt(0);
                if (cmp < 0) {
                    fail();
                } else if (cmp > 0) {
                    pos++;
                }
                int str = strength == 0 ? 0 : 1;
                posTable[str][c] = pos;
                last = t;
            }
        }
        strengths = new int[] {
                Collator.PRIMARY,
                Collator.SECONDARY,
                Collator.TERTIARY,
                Collator.IDENTICAL
        };
        for (int strength : strengths) {
            for (int i = 0; i < texts.length; i++) {
                String t = "" + (char) i;
                texts[i] = t;
            }
            Comparator<String> myComp = new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    int str = strength;
                    int a = o1.charAt(0);
                    int b = o2.charAt(0);
                    if (strength == 1) {
                        a = StringUtils.toLowerChar((char) a);
                        b = StringUtils.toLowerChar((char) b);
                    }
                    if (strength > 0) {
                        str = 1;
                    }
                    int pa = posTable[str][a];
                    int pb = posTable[str][b];
                    if (pa == pb) {
                        if (strength < 3) {
                            return 0;
                        }
                        pa = a;
                        pb = b;
                    }
                    if (pa > pb) {
                        return 1;
                    } else if (pa < pb) {
                        return -1;
                    }
                    return 0;
                }
            };
            Arrays.sort(texts, myComp);
            Collator coll = Collator.getInstance(locale);
            coll.setStrength(strength);
            String last = "";
            for (int i = 0; i < texts.length; i++) {
                String t = texts[i];
                if (i > 0) {
                    int cmp = coll.compare(t, last);
                    if (cmp < 0) {
                        fail();
                    }
                    int myCmp = myComp.compare(t, last);
                    if (cmp == 0 || myCmp == 0) {
                        if (cmp != myCmp) {
                            myComp.compare(t, last);
                            if (strength < Collator.TERTIARY) {

                            }
                            String tu = t.toUpperCase(Locale.ROOT);
                            String lastu = last.toUpperCase(Locale.ROOT);
                            cmp = coll.compare(tu, lastu);
                        }
                        assertEquals("strength " + strength, cmp, myCmp);
                    } else {
                        cmp = (int) Math.signum(cmp);
                        myCmp = (int) Math.signum(myCmp);
                        assertEquals(cmp, myCmp);
                    }
                }
                last = t;
            }
        }
        for (int i = 0; i < 2; i++) {
            StringBuilder buff = new StringBuilder();
            for (int j = 0; j < 256; j++) {
                if (j > 0) {
                    buff.append(",");
                }
                buff.append(posTable[i][j]);
                // buff.append("\\x" + Integer.toHexString(0x100 + posTable[i][j]).substring(1));
            }
            // System.out.println("POS_TABLE[" + i + "] = '" + buff.toString() + "'");
            System.out.println("POS_TABLE[" + i + "] = new int[]{" + buff.toString() + "};");
        }
        int[][] POS_TABLE = new int[2][];
        POS_TABLE[0] = new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6,20,42,36,43,41,19,23,24,39,44,3,0,11,10,56,57,58,59,60,61,62,63,64,65,5,4,48,49,50,8,33,69,71,72,73,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,91,93,94,95,96,97,98,25,40,26,14,1,13,69,71,72,73,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,91,93,94,95,96,97,98,27,52,28,16,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,35,37,34,38,53,29,15,31,99,21,51,0,32,2,54,45,100,101,12,55,30,17,18,102,103,22,66,67,68,9,69,69,69,69,69,69,70,72,75,75,75,75,79,79,79,79,74,84,85,85,85,85,85,47,104,93,93,93,93,97,92,90,69,69,69,69,69,69,70,72,75,75,75,75,79,79,79,79,74,84,85,85,85,85,85,46,105,93,93,93,93,97,92,97};
        POS_TABLE[1] = new int[]{0,0,0,0,0,0,0,0,0,4,5,7,6,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,15,29,51,45,52,50,28,32,33,48,53,12,8,20,19,65,66,67,68,69,70,71,72,73,74,14,13,57,58,59,17,42,79,95,97,101,105,115,117,119,121,131,133,135,137,139,143,155,157,159,161,164,168,178,180,182,184,189,34,49,35,23,10,22,78,94,96,100,104,114,116,118,120,130,132,134,136,138,142,154,156,158,160,163,167,177,179,181,183,188,36,61,37,25,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,16,44,46,43,47,62,38,24,40,190,30,60,9,41,11,63,54,191,192,21,64,39,26,27,193,194,31,75,76,77,18,83,81,85,91,89,87,93,99,109,107,111,113,125,123,127,129,103,141,147,145,149,153,151,56,195,172,170,174,176,186,166,162,82,80,84,90,88,86,92,98,108,106,110,112,124,122,126,128,102,140,146,144,148,152,150,55,196,171,169,173,175,185,165,187};
    }

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
