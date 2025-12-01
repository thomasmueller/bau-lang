package org.bau.stdlib.xml;

public class XMLReader {

    public enum TokenType {
        PROCESSING_INSTRUCTION, COMMENT, TAG_EMPTY, TAG_START, ATTRIBUTE, VALUE, TEXT, CDATA, TAG_END, ERROR, END;
    }

    private final String xml;
    private int lastPos, pos;
    private TokenType last, current;
    private int lastTokenStart, lastTokenEnd;
    private int currentTokenStart, currentTokenEnd;
    private boolean attributes;

    public XMLReader(String xml) {
        this(xml, 0);
    }

    private XMLReader(String xml, int pos) {
        this.xml = xml;
        this.pos = pos;
        nextToken();
    }

    public TokenType getTokenType() {
        return current;
    }

    public int getPos() {
        return lastPos;
    }

    public TokenType nextToken() {
        lastPos = pos;
        last = current;
        lastTokenStart = currentTokenStart;
        lastTokenEnd = currentTokenEnd;
        current = read();
        return last;
    }

    public String getEscapedValue() {
        return lastToken();
    }

    private String lastToken() {
        return xml.substring(lastTokenStart, lastTokenEnd);
    }

    private char readSkipSpace() {
        char ch;
        while (true) {
            if (pos < 0 || pos >= xml.length()) {
                return 0;
            }
            ch = xml.charAt(pos);
            if (ch != ' ' && ch != '\r' && ch != '\n' && ch != '\t') {
                break;
            }
            pos++;
        }
        return ch;
    }

    private TokenType read() {
        char ch = readSkipSpace();
        if (ch == 0) {
            return TokenType.END;
        }
        if (attributes) {
            currentTokenStart = pos;
            currentTokenEnd = -1;
            if (ch == '/') {
                attributes = false;
                pos += 2;
                return read();
            } else if (ch == '>') {
                attributes = false;
                pos++;
                return read();
            }
            TokenType result = TokenType.ATTRIBUTE;
            if (ch == '=') {
                pos++;
                ch = readSkipSpace();
                if (ch == 0) {
                    return TokenType.ERROR;
                }
                currentTokenStart = pos;
                result = TokenType.VALUE;
            }
            if (ch == '\"' || ch == '\'') {
                pos++;
                currentTokenStart = pos;
                while (true) {
                    if (pos < 0 || pos >= xml.length()) {
                        return TokenType.ERROR;
                    }
                    ch = xml.charAt(pos++);
                    if (ch == '\"' || ch == '\'') {
                        break;
                    }
                }
                currentTokenEnd = pos - 1;
                return TokenType.VALUE;
            }
            while (true) {
                if (pos < 0 || pos >= xml.length()) {
                    return TokenType.ERROR;
                }
                ch = xml.charAt(pos++);
                if (ch == '>' || ch == '=' || ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                    break;
                }
            }
            currentTokenEnd = pos - 1;
            if (ch == '=') {
                pos--;
            } else if (ch == '>') {
                attributes = false;
            }
            return result;
        }
        int start = pos++;
        currentTokenStart = -1;
        currentTokenEnd = -1;
        switch (ch) {
        case '<': {
            if (pos >= xml.length()) {
                return TokenType.ERROR;
            }
            TokenType result = TokenType.TAG_START;
            ch = xml.charAt(pos);
            if (ch == '/') {
                start++;
                result = TokenType.TAG_END;
            } else if (ch == '?') {
                result = TokenType.PROCESSING_INSTRUCTION;
                pos = xml.indexOf("?>", start);
            } else if (ch == '!') {
                String prefix = xml.substring(pos + 1);
                if (prefix.startsWith("--")) {
                    result = TokenType.COMMENT;
                    pos = xml.indexOf("--", pos + 3);
                } else if (prefix.startsWith("[CDATA[")) {
                    result = TokenType.CDATA;
                    pos = xml.indexOf("]]>", start);
                }
            }
            while (true) {
                if (pos < 0 || pos >= xml.length()) {
                    return TokenType.ERROR;
                }
                ch = xml.charAt(pos++);
                if (ch == '>' || ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                    break;
                }
            }
            currentTokenEnd = pos - 1;
            if (result == TokenType.TAG_START) {
                // lookahead to detect empty tag
                if (ch != '>') {
                    int p = pos;
                    boolean quoted = false;
                    attributes = false;
                    while (true) {
                        if (p < 0 || p >= xml.length()) {
                            return TokenType.ERROR;
                        }
                        ch = xml.charAt(p++);
                        if (ch == '\"') {
                            quoted = !quoted;
                            attributes = true;
                        } else  if (!quoted && ch == '>') {
                            if (xml.charAt(p - 2) == '/') {
                                result = TokenType.TAG_EMPTY;
                            }
                            break;
                        } else if (ch == '/' || ch == '>' || ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                            // not an attribute char
                        } else {
                            attributes = true;
                        }
                    }
                    if (!attributes) {
                        pos = p;
                    }
                }
                if (ch == '>') {
                    if (xml.charAt(pos - 2) == '/') {
                        currentTokenEnd = Math.min(currentTokenEnd, pos - 2);
                        result = TokenType.TAG_EMPTY;
                    }
                }
            }
            // TODO there could be multiple CDATA sections
            currentTokenStart = start + 1;
            if (result == TokenType.CDATA) {
                currentTokenStart += 8;
                currentTokenEnd -= 2;
            }
            return result;
        }
        default:
            while (true) {
                if (pos >= xml.length()) {
                    return TokenType.ERROR;
                }
                ch = xml.charAt(pos++);
                if (ch == '<') {
                    break;
                }
            }
            currentTokenStart = start;
            currentTokenEnd = pos - 1;
            pos--;
            return TokenType.TEXT;
        }
    }

    public static String decodeString(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        int length = s.length();
        StringBuilder buff = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c != '&') {
                buff.append(c);
                continue;
            }
            int start = i + 1;
            while (true) {
                i++;
                if (i >= length) {
                    return null;
                }
                c = s.charAt(i);
                if (c == ';') {
                    break;
                }
            }
            String s2 = s.substring(start, i);
            if (s2.equals("lt")) {
                buff.append('<');
            } else if (s2.equals("gt")) {
                buff.append('>');
            } else if (s2.equals("amp")) {
                buff.append('&');
            } else if (s2.equals("quot")) {
                buff.append('\"');
            } else if (s2.equals("apos")) {
                buff.append('\'');
            } else if (s2.startsWith("#x")) {
                try {
                    int x = Integer.parseInt(s2.substring(2), 16);
                    buff.append((char) x);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else if (s2.startsWith("#")) {
                try {
                    int x = Integer.parseInt(s2.substring(1));
                    buff.append((char) x);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return buff.toString();
    }

}
