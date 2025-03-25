package org.bau.stdlib.string;

import java.util.ArrayList;

public class StringUtils {

    /**
     * Replace "before" with "after". If "before" is empty, then the original string
     * is returned.
     *
     * @param s
     * @param before
     * @param after
     * @return
     */
    public static String replaceAll(String s, String before, String after) {
        int next = s.indexOf(before);
        if (before.isEmpty() || next < 0) {
            return s;
        }
        StringBuilder buff = new StringBuilder(s.length());
        int index = 0;
        while (true) {
            buff.append(s, index, next).append(after);
            index = next + before.length();
            next = s.indexOf(before, index);
            if (next < 0) {
                break;
            }
        }
        buff.append(s, index, s.length());
        return buff.toString();
    }

    /**
     * Split at a delimiter. If the delimiter is empty or not found, one entry is returned.
     *
     * @param s the original string
     * @param delimiter the delimiter
     * @return an array of strings
     */
    public static String[] split(String s, String delimiter) {
        int next = s.indexOf(delimiter);
        if (delimiter.isEmpty() || next < 0) {
            return new String[] {s};
        }
        ArrayList<String> list = new ArrayList<>();
        int index = 0;
        while (true) {
            list.add(s.substring(index, next));
            index = next + delimiter.length();
            next = s.indexOf(delimiter, index);
            if (next < 0) {
                break;
            }
        }
        list.add(s.substring(index));
        return list.toArray(new String[list.size()]);
    }

}
