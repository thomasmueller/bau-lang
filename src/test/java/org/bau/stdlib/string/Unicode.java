package org.bau.stdlib.string;

import java.util.Arrays;

public class Unicode {

    // Full support of Unicode collations etc
    // would require a lot of code and large tables.
    // See also icupkg, https://sqlite.org/src/file?name=ext/icu/icu.c

    // PRIMARY: a = A = ä < b
    // SECONDARY: a = A < ä = Ä < b
    // TERTIARY: a < A < ä < Ä < b
    // IDENTICAL: each characters is distinct
    public static final int STRENGTH_PRIMARY = 0, STRENGTH_SECONDARY = 1, STRENGTH_TERTIARY = 2, STRENGTH_IDENTICAL = 3;

    private final static int[][] POS_TABLE = new int[2][];

    static {
        POS_TABLE[0] = new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6,20,42,36,43,41,19,23,24,39,44,3,0,11,10,56,57,58,59,60,61,62,63,64,65,5,4,48,49,50,8,33,69,71,72,73,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,91,93,94,95,96,97,98,25,40,26,14,1,13,69,71,72,73,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,91,93,94,95,96,97,98,27,52,28,16,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,35,37,34,38,53,29,15,31,99,21,51,0,32,2,54,45,100,101,12,55,30,17,18,102,103,22,66,67,68,9,69,69,69,69,69,69,70,72,75,75,75,75,79,79,79,79,74,84,85,85,85,85,85,47,104,93,93,93,93,97,92,90,69,69,69,69,69,69,70,72,75,75,75,75,79,79,79,79,74,84,85,85,85,85,85,46,105,93,93,93,93,97,92,97};
        POS_TABLE[1] = new int[]{0,0,0,0,0,0,0,0,0,4,5,7,6,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,15,29,51,45,52,50,28,32,33,48,53,12,8,20,19,65,66,67,68,69,70,71,72,73,74,14,13,57,58,59,17,42,79,95,97,101,105,115,117,119,121,131,133,135,137,139,143,155,157,159,161,164,168,178,180,182,184,189,34,49,35,23,10,22,78,94,96,100,104,114,116,118,120,130,132,134,136,138,142,154,156,158,160,163,167,177,179,181,183,188,36,61,37,25,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,16,44,46,43,47,62,38,24,40,190,30,60,9,41,11,63,54,191,192,21,64,39,26,27,193,194,31,75,76,77,18,83,81,85,91,89,87,93,99,109,107,111,113,125,123,127,129,103,141,147,145,149,153,151,56,195,172,170,174,176,186,166,162,82,80,84,90,88,86,92,98,108,106,110,112,124,122,126,128,102,140,146,144,148,152,150,55,196,171,169,173,175,185,165,187};
    }

    public static int compare(String a, String b, int strength) {
        for (int i = 0; i < a.length() && i < b.length(); i++) {
            char ca = a.charAt(i);
            char cb = b.charAt(i);
            int comp = compareChar(ca, cb, strength);
            if (comp != 0) {
                return comp;
            }
        }
        return Integer.compare(a.length(), b.length());
    }

    public static int compareChar(char a, char b, int strength) {
        if (a > 0x100 || b > 0x100) {
            return Integer.compare(a, b);
        }
        int str = strength;
        if (strength > 0) {
            if (strength == 1) {
                a = StringUtils.toLowerChar((char) a);
                b = StringUtils.toLowerChar((char) b);
            }
            str = 1;
        }
        int pa = POS_TABLE[str][a];
        int pb = POS_TABLE[str][b];
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

    public static int codePointCount(byte[] utf8) {
        int pos = 0;
        int count = 0;
        while (pos < utf8.length) {
            UnicodeChar u = next(utf8, pos);
            count++;
            pos += u.byteCount;
        }
        return count;
    }

    public static byte[] normalize(byte[] utf8) {
        byte[] result = new byte[utf8.length * 3];
        int pos = 0;
        int targetPos = 0;
        while (pos < utf8.length) {
            UnicodeChar u = next(utf8, pos);
            pos += u.byteCount;
            if (u.codePoint < 0x80) {
                result[targetPos++] = (byte) u.codePoint;
            } else if (u.codePoint < 0x800) {
                result[targetPos++] = (byte) (0xc0 | (u.codePoint >>> 6));
                result[targetPos++] = (byte) (0x80 | (u.codePoint & 0x3f));
            } else if (u.codePoint < 0x10000) {
                result[targetPos++] = (byte) (0xe0 | (u.codePoint >>> 12));
                result[targetPos++] = (byte) (0x80 | ((u.codePoint >>> 6) & 0x3f));
                result[targetPos++] = (byte) (0x80 | (u.codePoint & 0x3f));
            } else {
                result[targetPos++] = (byte) (0xf0 | (u.codePoint >>> 18));
                result[targetPos++] = (byte) (0x80 | ((u.codePoint >>> 12) & 0x3f));
                result[targetPos++] = (byte) (0x80 | ((u.codePoint >>> 6) & 0x3f));
                result[targetPos++] = (byte) (0x80 | (u.codePoint & 0x3f));
            }
        }
        result = Arrays.copyOf(result, targetPos);
        return result;
    }

    public static UnicodeChar next(byte[] utf8, int start) {
        boolean error = true;
        int pos = start;
        int x = utf8[pos] & 0xff;
        int available = utf8.length - start;
        int len = 1;
        if (x < 0x80) {
            error = false;
        } else if (x < 0xc0 || available < 2 || ((utf8[pos + 1] & 0xff) >>> 6) != 2) {
            // error
        } else if (x < 0xe0) {
            x = ((x & 0x1f) << 6) | ((utf8[pos + 1] & 0x3f));
            if (x >= 0x80) {
                len = 2;
                error = false;
            }
        } else if (x < 0xf0) {
            x = ((x & 0x0f) << 12) | ((utf8[pos + 1] & 0x3f) << 6);
            if (x < 0x800) {
                // error
            } else if (available < 3 || ((utf8[pos + 2] & 0xff) >>> 6) != 2) {
                len = 2;
            } else {
                len = 3;
                x |= utf8[pos + 2] & 0x3f;
                if (x < 0xd800 || x > 0xdfff) {
                    error = false;
                }
            }
        } else if (x < 0xf8) {
            x = ((x & 0x7) << 18) | ((utf8[pos + 1] & 0x3f) << 12);
            if (x < 0x10000 || x > 0x10ffff) {
                // error
            } else if (available < 3 || ((utf8[pos + 2] & 0xff) >>> 6) != 2) {
                len = 2;
            } else if (available < 4 || ((utf8[pos + 3] & 0xff) >>> 6) != 2) {
                len = 3;
            } else {
                len = 4;
                x |= ((utf8[pos + 2] & 0x3f) << 6) | (utf8[pos + 3] & 0x3f);
                error = false;
            }
        }
        if (error) {
            x = 0xfffd;
        }
        UnicodeChar result = new UnicodeChar();
        result.error = error;
        result.byteCount = len;
        result.codePoint = x;
        return result;
    }

    static class UnicodeChar {

        // the number of UTF-8 bytes
        int byteCount;

        // the Unicode code point
        int codePoint;

        // whether the entry is illegal
        boolean error;

    }

}
