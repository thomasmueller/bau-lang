package org.bau.stdlib.string.regex;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Regex {

    public static String replaceFirst(String text, String regex, String replacement) {
        int[] pair = find(text, regex);
        if (pair == null) {
            return text;
        }
        return text.substring(0, pair[0]) + replacement + text.substring(pair[1]);
    }

    public static String replaceAll(String text, String regex, String replacement) {
        StringBuilder buff = new StringBuilder(text.length());
        while (!text.isEmpty()) {
            int[] pair = find(text, regex);
            if (pair == null) {
                buff.append(text);
                break;
            } else if (pair[0] == 0 && pair[1] == 0) {
                // at least one character
                buff.append(text.charAt(0));
                text = text.substring(1);
            } else {
                buff.append(text.substring(0, pair[0]) + replacement);
                text = text.substring(pair[1]);
            }
        }
        return buff.toString();
    }

    public static String[] split(String text, String regex) {
        ArrayList<String> list = new ArrayList<>();
        while (true) {
            int[] pair = find(text, regex);
            if (pair == null) {
                list.add(text);
                return list.toArray(new String[list.size()]);
            }
            list.add(text.substring(0, pair[0]));
            text = text.substring(pair[1]);
        }
    }

    public static boolean matches(String text, String regex) {
        int[] result = find(text, regex);
        return result != null && result[0] == 0 && result[1] == text.length();
    }

    public static int[] find(String text, String regex) {
        ArrayList<Token> list = RegexParser.parse(regex);
        AtomicLong counter = new AtomicLong(100 + text.length() * 100);
        if (list.size() > 0 && list.get(0).type == TokenType.START) {
            int end = list.get(1).matchHere(counter, text, 0, null);
            if (end >= 0) {
                return new int[] {0, end};
            }
            return null;
        }
        int tp = 0;
        do {
            int end = list.get(0).matchHere(counter, text, tp, null);
            if (end >= 0) {
                return new int[] {tp, end};
            }
        } while (tp++ < text.length());
        return null;
    }

}

