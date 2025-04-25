package org.bau.stdlib.string;

import java.util.Arrays;

public class Base64 {

    static int decode(int i) {
        if (i >= 'A' && i <= 'Z') {
            return i - 'A';
        } else if (i >= 'a' && i <= 'z') {
            return i - 'a' + 26;
        } else if (i >= '0' && i <= '9') {
            return i - '0' + 52;
        } else if (i == '+') {
            return 62;
        } else if (i == '/') {
            return 63;
        }
        return -1;
    }

    static byte code(int i) {
        if (i < 26) {
            return (byte) (i + 'A');
        } else if (i < 52) {
            return (byte) (i - 26 + 'a');
        } else if (i < 62) {
            return (byte) (i - 52 + '0');
        } else if (i == 62) {
            return (byte) ('+');
        } else if (i == 63) {
            return (byte) ('/');
        }
        return (byte) (-1);
    }

    public static byte[] encode(byte[] bin) {
        int size = bin.length;
        int len = ((size + 2) / 3) * 4;
        byte[] enc = new byte[len];
        int fast = size / 3 * 3, i = 0, j = 0;
        for (; i < fast; i += 3, j += 4) {
            int a = ((bin[i] & 255) << 16) + ((bin[i + 1] & 255) << 8) + (bin[i + 2] & 255);
            enc[j] = code(a >> 18);
            enc[j + 1] = code((a >> 12) & 63);
            enc[j + 2] = code((a >> 6) & 63);
            enc[j + 3] = code(a & 63);
        }
        if (i < size) {
            int a = (bin[i++] & 255) << 16;
            enc[j] = code(a >> 18);
            if (i < size) {
                a += (bin[i] & 255) << 8;
                enc[j + 2] = code((a >> 6) & 63);
            } else {
                enc[j + 2] = (byte) '=';
            }
            enc[j + 1] = code((a >> 12) & 63);
            enc[j + 3] = (byte) '=';
        }
        return enc;
    }

    public static byte[] decode(byte[] enc) {
        int len = enc.length, size = (len * 3) / 4;
        byte[] bin = new byte[size];
        int i = 0, j = 0;
        while (j < len) {
            int a = 0;
            int k = 0;
            for (; k < 4 && j < len;) {
                int x = decode(enc[j++] & 255);
                if (x >= 0) {
                    a = (a << 6) + x;
                    k++;
                }
            }
            if (k == 4) {
                bin[i] = (byte) (a >> 16);
                bin[i + 1] = (byte) (a >> 8);
                bin[i + 2] = (byte) a;
                i += 3;
            } else if (k == 3) {
                bin[i] = (byte) (a >> 10);
                bin[i + 1] = (byte) (a >> 2);
                i += 2;
            } else if (k == 2) {
                bin[i] = (byte) (a >> 4);
                i++;
            }
        }
        if (size != i) {
            bin = Arrays.copyOf(bin, i);
        }
        return bin;
    }

}
