package org.bau.stdlib.locale;

import java.nio.charset.StandardCharsets;

public class NumberFormatter {

    private static void reverse(byte[] data, int len) {
        for(int a = 0, b = len - 1; a < b; a++, b--) {
            byte temp = data[a];
            data[a] = data[b];
            data[b] = temp;
        }
    }

    public static String format(long value, String groupSeparator, int groupSizeSmall, int groupSizeLarge) {
        byte[] sep = groupSeparator.getBytes(StandardCharsets.UTF_8);
        byte sepByte = '_';
        if (sep.length == 1) {
            sepByte = sep[0];
        }
        String s = format(value, sepByte, groupSizeSmall, groupSizeLarge);
        if (sep.length == 1) {
            return s;
        }
        return s.replace("_", groupSeparator);
    }

    public static String format(long value, byte groupSeparatorChar, int groupSizeSmall, int groupSizeLarge) {
        byte[] buff = new byte[20];
        int group = groupSizeSmall;
        int pos = 0;
        while (true) {
            buff[pos] = (byte) ('0' + (value % 10));
            pos++;
            value /= 10;
            if (value == 0) {
                break;
            }
            group--;
            if (group == 0) {
                buff[pos] = groupSeparatorChar;
                pos++;
                group = groupSizeLarge;
            }
        }
        reverse(buff, pos);
        return new String(buff, 0, pos);
    }
}
