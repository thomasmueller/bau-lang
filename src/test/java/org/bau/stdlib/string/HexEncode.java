package org.bau.stdlib.string;

import java.nio.charset.StandardCharsets;

public class HexEncode {

    public static byte[] convertHexToBytes(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            return null;
        }
        len /= 2;
        byte[] buff = new byte[len];
        int mask = 0;
        try {
            for (int i = 0; i < len; i++) {
                int a = nibble(s.charAt(i + i));
                int b = nibble(s.charAt(i + i + 1));
                mask |= a | b;
                buff[i] = (byte) ((a << 4) | b);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
        if ((mask & ~0xf) != 0) {
            return null;
        }
        return buff;
    }

    static int nibble(char hexChar) {
        // if (hexChar >= '0' && hexChar <= '9') {
        //     return hexChar - '0';
        // }
        // int lower = (hexChar | 0x20);
        // if (lower >= 'a' && lower <= 'f') {
        //     return lower - 'a' + 10;
        // }
        // return -1;

        // convert 'A'... to 'a'...
        int lowerCase = hexChar | ((hexChar >> 1) & 0x20);
        // 0xf if <= '9'
        int maskDigit = ((lowerCase - '9' - 1) >> 8);
        // 0xf if >= 'a'
        int maskLetter = (('a' - lowerCase - 1) >> 8);
        return lowerCase - (maskDigit & '0') - (maskLetter & ('a' - 10));
    }

    public static String convertBytesToHex(byte[] value) {
        int len = value.length;
        byte[] bytes = new byte[len * 2];
        for (int i = 0, j = 0; i < len; i++) {
            int c = value[i] & 0xff;
            bytes[j++] = hexDigit((c >>> 4) & 0xf);
            bytes[j++] = hexDigit(c & 0xf);
        }
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    private static byte hexDigit(int i) {
        return (byte) ('0' + i + (('a' - '0' - 10) & ((9 - i) >>> 8)));
    }

}
