package org.bau.stdlib.util;

import java.util.Arrays;

public class Unicode {

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
        boolean error;
        int byteCount;
        int codePoint;
    }

}
