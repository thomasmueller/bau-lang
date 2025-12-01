package org.bau.stdlib.json;

/**
 * A simple JSON parser.
 *
 * In case of errors, the methods return null or return TokenType.ERROR, but never throw an exception.
 */
public class JsonReader {

    public enum TokenType {
        STRING, NUMBER, TRUE, FALSE, NULL, COMMA, COLON, ARRAY, ARRAY_END, OBJECT, OBJECT_END, ERROR, END;
    }

    private final String json;
    private int lastPos, pos;
    private TokenType last, current;
    private int lastTokenStart, lastTokenEnd;
    private int currentTokenStart, currentTokenEnd;

    public JsonReader(String json) {
        this(json, 0);
    }

    private JsonReader(String json, int pos) {
        this.json = json;
        this.pos = pos;
        nextToken();
    }

    public TokenType getTokenType() {
        return current;
    }

    public JsonReader get(String key) {
        while(true) {
            String k = nextKey();
            if (k == null) {
                return null;
            } else if (key.equals(k)) {
                return new JsonReader(json, pos);
            }
        }
    }

    public String nextKey() {
        if (matches(TokenType.COLON)) {
            skipValue();
        }
        if (current == TokenType.COMMA || current == TokenType.OBJECT) {
            nextToken();
        }
        if (matches(TokenType.STRING)) {
            String key = lastToken();
            if (current == TokenType.COLON) {
                return decodeString(key);
            }
        }
        return null;
    }

    public JsonReader value() {
        JsonReader result = new JsonReader(json, lastPos);
        if (result.current == TokenType.COLON) {
            result.nextToken();
        }
        return result;
    }

    public JsonReader nextValue() {
        if (current == TokenType.COMMA || current == TokenType.ARRAY) {
            nextToken();
        }
        if (current == TokenType.ARRAY_END) {
            return null;
        }
        JsonReader result = new JsonReader(json, lastPos);
        skipValue();
        return result;
    }

    public String getString() {
        if (matches(TokenType.NULL)) {
            return null;
        } else if (matches(TokenType.STRING)) {
            return decodeString(lastToken());
        }
        return null;
    }

    public String getNumber() {
        if (matches(TokenType.NUMBER)) {
            return lastToken();
        }
        return null;
    }

    public Boolean getBoolean() {
        if (matches(TokenType.TRUE)) {
            return Boolean.TRUE;
        } else if (matches(TokenType.FALSE)) {
            return Boolean.FALSE;
        }
        return null;
    }

    public int getPos() {
        return lastPos;
    }

    public void setPos(int pos) {
        this.pos = pos;
        nextToken();
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

    public boolean matches(TokenType type) {
        if (current == type) {
            nextToken();
            return true;
        }
        return false;
    }

    private void skipValue() {
        int objectLevel = 0, arrayLevel = 0;
        do {
            if (current == TokenType.END || current == TokenType.ERROR) {
                return;
            } else if (current == TokenType.OBJECT_END) {
                if (objectLevel > 0 || arrayLevel > 0) {
                    nextToken();
                }
                objectLevel--;
            } else if (current == TokenType.ARRAY_END) {
                if (objectLevel > 0 || arrayLevel > 0) {
                    nextToken();
                }
                arrayLevel--;
            } else if (matches(TokenType.OBJECT)) {
                objectLevel++;
            } else if (matches(TokenType.ARRAY)) {
                arrayLevel++;
            } else {
                nextToken();
            }
        } while (objectLevel > 0 || arrayLevel > 0);
    }

    private String lastToken() {
        if (lastTokenStart == -1) {
            return null;
        }
        return json.substring(lastTokenStart, lastTokenEnd);
    }

    private TokenType read() {
        char ch;
        while (true) {
            if (pos >= json.length()) {
                return TokenType.END;
            }
            ch = json.charAt(pos);
            if (ch != ' ' && ch != '\r' && ch != '\n' && ch != '\t') {
                break;
            }
            pos++;
        }
        int start = pos++;
        currentTokenStart = -1;
        currentTokenEnd = -1;
        switch (ch) {
        case '\"': {
            while (true) {
                if (pos >= json.length()) {
                    return TokenType.ERROR;
                }
                ch = json.charAt(pos++);
                if (ch == '\"') {
                    break;
                } else if (ch == '\\') {
                    pos++;
                } else if (ch < ' ') {
                    return TokenType.ERROR;
                }
            }
            currentTokenStart = start + 1;
            currentTokenEnd = pos - 1;
            return TokenType.STRING;
        }
        case '{':
            return TokenType.OBJECT;
        case '}':
            return TokenType.OBJECT_END;
        case '[':
            return TokenType.ARRAY;
        case ']':
            return TokenType.ARRAY_END;
        case ',':
            return TokenType.COMMA;
        case ':':
            return TokenType.COLON;
        case '-':
            if (pos >= json.length()) {
                return TokenType.ERROR;
            }
            ch = json.charAt(pos);
            pos++;
            if (ch < '0' || ch > '9') {
                // lookahead
                return TokenType.ERROR;
            }
            // else fall though
        default:
            if (ch >= '0' && ch <= '9') {
                if (ch == '0' && pos < json.length()) {
                    ch = json.charAt(pos);
                    if (ch >= '0' && ch <= '9') {
                        return TokenType.ERROR;
                    }
                }
                while (pos < json.length()) {
                    ch = json.charAt(pos);
                    if (ch < '0' || ch > '9') {
                        break;
                    }
                    pos++;
                }
                if (ch == '.') {
                    if (++pos >= json.length()) {
                        return TokenType.ERROR;
                    }
                    ch = json.charAt(pos);
                    if (ch < '0' || ch > '9') {
                        return TokenType.ERROR;
                    }
                    pos++;
                    while (pos < json.length()) {
                        ch = json.charAt(pos);
                        if (ch < '0' || ch > '9') {
                            break;
                        }
                        pos++;
                    }
                }
                if (ch == 'e' || ch == 'E') {
                    if (++pos >= json.length()) {
                        return TokenType.ERROR;
                    }
                    ch = json.charAt(pos);
                    if (ch == '+' || ch == '-') {
                        if (++pos >= json.length()) {
                            return TokenType.ERROR;
                        }
                        ch = json.charAt(pos);
                    }
                    if (ch < '0' || ch > '9') {
                        return TokenType.ERROR;
                    }
                    pos++;
                    while (pos < json.length()) {
                        ch = json.charAt(pos);
                        if (ch < '0' || ch > '9') {
                            break;
                        }
                        pos++;
                    }
                }
                currentTokenStart = start;
                currentTokenEnd = pos;
                return TokenType.NUMBER;
            } else if (ch >= 'a' && ch <= 'z') {
                while (pos < json.length()) {
                    ch = json.charAt(pos);
                    if ((ch < 'a' || ch > 'z')) {
                        break;
                    }
                    pos++;
                }
                if (json.regionMatches(start, "null", 0, 4)) {
                    currentTokenStart = -1;
                    currentTokenEnd = -1;
                    return TokenType.NULL;
                } else if (json.regionMatches(start, "true", 0, 4)) {
                    currentTokenStart = start;
                    currentTokenEnd = pos;
                    return TokenType.TRUE;
                } else if (json.regionMatches(start, "false", 0, 5)) {
                    currentTokenStart = start;
                    currentTokenEnd = pos;
                    return TokenType.FALSE;
                }
            }
            return TokenType.ERROR;
        }
    }

    public static String decodeString(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        int length = s.length();
        StringBuilder buff = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c != '\\') {
                buff.append(c);
                continue;
            }
            if (i + 1 >= length) {
                return null;
            }
            c = s.charAt(++i);
            switch (c) {
            case '"':
                buff.append('"');
                break;
            case '\\':
                buff.append('\\');
                break;
            case '/':
                buff.append('/');
                break;
            case 'b':
                buff.append('\b');
                break;
            case 'f':
                buff.append('\f');
                break;
            case 'n':
                buff.append('\n');
                break;
            case 'r':
                buff.append('\r');
                break;
            case 't':
                buff.append('\t');
                break;
            case 'u': {
                if (s.length() < i + 5) {
                    return null;
                }
                int x = 0;
                for (int j = 0; j < 4; j++) {
                    int digit = Character.digit(s.charAt(++i), 16);
                    if (digit < 0) {
                        return null;
                    }
                    x = (x << 4) + digit;
                }
                c = (char) x;
                buff.append(c);
                break;
            }
            default:
                return null;
            }
        }
        return buff.toString();
    }

}
