package org.bau.stdlib.xml;

import java.util.ArrayList;

public class XMLBuilder {

    private final StringBuilder buff = new StringBuilder();
    private boolean startedElement;
    private boolean hasChild;
    private ArrayList<String> openElements = new ArrayList<>();

    public XMLBuilder declaration() {
        buff.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        return this;
    }

    public XMLBuilder element(String name) {
        hasChild = false;
        if (startedElement) {
            buff.append(">");
        }
        openElements.add(name);
        buff.append("<" + name);
        startedElement = true;
        return this;
    }

    public XMLBuilder attribute(String name, String value) {
        buff.append(" " + name + "=\"" + xmlText(value, false) + "\"");
        return this;
    }

    public XMLBuilder text(String text) {
        if (startedElement) {
            buff.append(">");
            startedElement = false;
        }
        buff.append(xmlText(text, false));
        hasChild = true;
        return this;
    }

    public XMLBuilder cdata(String text) {
        if (startedElement) {
            buff.append(">");
            startedElement = false;
        }
        buff.append(xmlCData(text));
        hasChild = true;
        return this;
    }

    public XMLBuilder endElement() {
        if (!hasChild) {
            openElements.removeLast();
            buff.append("/>\n");
        } else {
            buff.append("</" + openElements.removeLast() + ">\n");
        }
        hasChild = true;
        return this;
    }

    public static String xmlText(String text) {
        return xmlText(text, false);
    }
    public static String xmlCData(String data) {
        // CDATA cannot contain ]]>
        // alternatively, we could replace ]]> with  ]]]]><![CDATA[>
        if (data.contains("]]>")) {
            return xmlText(data);
        }
        boolean newline = data.endsWith("\n");
        data = "<![CDATA[" + data + "]]>";
        return newline ? data + "\n" : data;
    }

    public static String xmlComment(String data) {
        int idx = 0;
        while (true) {
            // Comments cannot contain --
            idx = data.indexOf("--", idx);
            if (idx < 0) {
                break;
            }
            data = data.substring(0, idx + 1) + " " + data.substring(idx + 1);
        }
        // must have a space at the beginning and at the end,
        // otherwise the data must not contain '-' as the first/last character
        if (data.indexOf('\n') >= 0) {
            StringBuilder builder = new StringBuilder(data.length() + 18).append("<!--\n");
            return indent(builder, data, 4, true).append("-->\n").toString();
        }
        return "<!-- " + data + " -->\n";
    }

    public static StringBuilder indent(StringBuilder builder, String s, int spaces, boolean newline) {
        for (int i = 0, length = s.length(); i < length;) {
            for (int j = 0; j < spaces; j++) {
                builder.append(' ');
            }
            int n = s.indexOf('\n', i);
            n = n < 0 ? length : n + 1;
            builder.append(s, i, n);
            i = n;
        }
        if (newline && !s.endsWith("\n")) {
            builder.append('\n');
        }
        return builder;
    }

    private static String xmlText(String text, boolean escapeNewline) {
        int length = text.length();
        StringBuilder buff = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char ch = text.charAt(i);
            switch (ch) {
            case '<':
                buff.append("&lt;");
                break;
            case '>':
                buff.append("&gt;");
                break;
            case '&':
                buff.append("&amp;");
                break;
            case '\'':
                // &apos; is not valid in HTML
                buff.append("&#39;");
                break;
            case '\"':
                buff.append("&quot;");
                break;
            case '\r':
            case '\n':
                if (escapeNewline) {
                    buff.append("&#x").
                        append(Integer.toHexString(ch)).
                        append(';');
                } else {
                    buff.append(ch);
                }
                break;
            case '\t':
                buff.append(ch);
                break;
            default:
                if (ch < ' ' || ch > 127) {
                    buff.append("&#x").
                        append(Integer.toHexString(ch)).
                        append(';');
                } else {
                    buff.append(ch);
                }
            }
        }
        return buff.toString();
    }

    public String toString() {
        return buff.toString();
    }

}
