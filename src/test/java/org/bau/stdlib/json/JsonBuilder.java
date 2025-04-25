package org.bau.stdlib.json;

public class JsonBuilder {

    private final StringBuilder buff = new StringBuilder();
    private boolean needComma;

    public JsonBuilder object() {
        optionalComma();
        buff.append('{');
        needComma = false;
        return this;
    }

    public JsonBuilder endObject() {
        buff.append('}');
        needComma = true;
        return this;
    }

    public JsonBuilder array() {
        optionalComma();
        buff.append('[');
        needComma = false;
        return this;
    }

    public JsonBuilder endArray() {
        buff.append(']');
        needComma = true;
        return this;
    }

    public JsonBuilder key(String name) {
        optionalComma();
        buff.append(encode(name)).append(':');
        needComma = false;
        return this;
    }

    public JsonBuilder value(long value) {
        return encodedValue(Long.toString(value));
    }

    public JsonBuilder value(boolean value) {
        return encodedValue(Boolean.toString(value));
    }

    public JsonBuilder value(String value) {
        return encodedValue(encode(value));
    }

    public JsonBuilder encodedValue(String value) {
        optionalComma();
        buff.append(value);
        needComma = true;
        return this;
    }

    private void optionalComma() {
        if (needComma) {
            buff.append(',');
        }
    }

    @Override
    public String toString() {
        return buff.toString();
    }

    public static String encode(String s) {
        if (s == null) {
            return "null";
        }
        int length = s.length();
        if (length == 0) {
            return "\"\"";
        }
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == '\"' || c == '\\' || c < ' ' || (c >= 0xd800 && c <= 0xdbff)) {
                StringBuilder buff = new StringBuilder(length + 2 + length / 8);
                buff.append('\"');
                escape(s, length, buff);
                return buff.append('\"').toString();
            }
        }
        StringBuilder buff = new StringBuilder(length + 2);
        return buff.append('\"').append(s).append('\"').toString();
    }

    private static void escape(String s, int length, StringBuilder buff) {
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            switch (c) {
            case '"':
                buff.append("\\\"");
                break;
            case '\\':
                buff.append("\\\\");
                break;
            case '\b':
                buff.append("\\b");
                break;
            case '\f':
                buff.append("\\f");
                break;
            case '\n':
                buff.append("\\n");
                break;
            case '\r':
                buff.append("\\r");
                break;
            case '\t':
                buff.append("\\t");
                break;
            default:
                if (c < ' ') {
                    buff.append(String.format("\\u%04x", (int) c));
                } else if (c >= 0xd800 && c <= 0xdbff) {
                    // isSurrogate(), only available in Java 7
                    if (i < length - 1 && Character.isSurrogatePair(c, s.charAt(i + 1))) {
                        // ok surrogate
                        buff.append(c);
                        buff.append(s.charAt(i + 1));
                        i += 1;
                    } else {
                        // broken surrogate -> escape
                        buff.append(String.format("\\u%04x", (int) c));
                    }
                } else {
                    buff.append(c);
                }
            }
        }
    }

}
