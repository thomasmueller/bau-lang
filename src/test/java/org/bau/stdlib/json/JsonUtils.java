package org.bau.stdlib.json;

import org.bau.stdlib.json.Json.TokenType;

public class JsonUtils {

    /**
     * Try to minify.
     *
     * @param json the JSON string
     * @return the pretty JSON string, or null if parsing fails
     */
    public static String minify(String json) {
        return format(json, "", "", "");
    }

    /**
     * Try to pretty-print.
     *
     * No validation is made. The indentation is 2 spaces per level; the maximum
     * indentation is 64 characters.
     *
     * @param json the JSON string
     * @return the pretty JSON string, or null if parsing fails
     */
    public static String prettyPrint(String json) {
        return format(json, "  ", " ", "\n");
    }

    private static String format(String json, String indent, String space, String newline) {
        StringBuilder buff = new StringBuilder();
        Json t = new Json(json);
        String linePrefix = "";
        boolean inArray = false;
        while (true) {
            TokenType token = t.nextToken();
            switch (token) {
            case END:
                return buff.toString();
            case STRING:
                buff.append('\"').append(t.getEscapedValue()).append('\"');
                break;
            case NUMBER:
            case TRUE:
            case FALSE:
            case NULL:
                buff.append(t.getEscapedValue());
                break;
            case OBJECT:
                if (t.matches(TokenType.OBJECT_END)) {
                    buff.append("{}");
                } else {
                    if (linePrefix.length() < 64) {
                        // prevent out of memory
                        linePrefix += indent;
                    }
                    buff.append('{').append(newline).append(linePrefix);
                }
                break;
            case OBJECT_END:
                if (linePrefix.length() >= indent.length()) {
                    linePrefix = linePrefix.substring(0, linePrefix.length() - indent.length());
                }
                buff.append(newline).append(linePrefix).append('}');
                break;
            case ARRAY:
                inArray = true;
                buff.append('[');
                break;
            case ARRAY_END:
                inArray = false;
                buff.append(']');
                break;
            case COMMA:
                buff.append(',');
                if (!inArray) {
                    buff.append(newline).append(linePrefix);
                } else {
                    buff.append(space);
                }
                break;
            case COLON:
                buff.append(':').append(space);
                break;
            default:
                return null;
            }
        }
    }

    /**
     * Validate a JSON string.
     *
     * Numbers, even if very small or very large, are always considered valid.
     * Inside of string values, unicode sequences are not verified and always
     * considered valid (including overlong sequences, errors in surrogate pairs,
     * truncated sequences etc).
     *
     * The maximum nesting level is 2000; deeper nesting is considered invalid.
     *
     * @param json the JSON string
     * @return if the JSON is considered valid.
     */
    public static boolean validate(String json) {
        Json t = new Json(json);
        return validateJson(t, 0) && t.matches(TokenType.END);
    }

    private static boolean validateJson(Json t, int level) {
        if (level > 2000) {
            return false;
        }
        switch (t.getTokenType()) {
        case STRING:
            return t.getString() != null;
        case NUMBER:
            t.getNumber();
            return true;
        case TRUE:
        case FALSE:
        case NULL:
            t.nextToken();
            return true;
        case OBJECT:
            t.nextToken();
            if (t.matches(TokenType.OBJECT_END)) {
                return true;
            }
            while (true) {
                if (!t.matches(TokenType.STRING)) {
                    return false;
                }
                if (!t.matches(TokenType.COLON)) {
                    return false;
                }
                if (!validateJson(t, level + 1)) {
                    return false;
                }
                if (t.matches(TokenType.OBJECT_END)) {
                    return true;
                }
                if (!t.matches(TokenType.COMMA)) {
                    return false;
                }
            }
        case ARRAY:
            t.nextToken();
            if (t.matches(TokenType.ARRAY_END)) {
                return true;
            }
            while (true) {
                if (!validateJson(t, level + 1)) {
                    return false;
                }
                if (t.matches(TokenType.ARRAY_END)) {
                    return true;
                }
                if (!t.matches(TokenType.COMMA)) {
                    return false;
                }
            }
        default:
            return false;
        }
    }

}
