package org.bau.stdlib.json;

import org.bau.stdlib.json.Json.TokenType;

public class JsonUtils {

    public static String minify(String json) {
        return format(json, "", "", "");
    }

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
            case TokenType.END:
                return buff.toString();
            case TokenType.STRING:
                buff.append('\"').append(t.getEscapedValue()).append('\"');
                break;
            case TokenType.NUMBER:
            case TokenType.TRUE:
            case TokenType.FALSE:
            case TokenType.NULL:
                buff.append(t.getEscapedValue());
                break;
            case TokenType.OBJECT:
                if (t.matches(TokenType.OBJECT_END)) {
                    buff.append("{}");
                } else {
                    linePrefix += indent;
                    buff.append('{').append(newline).append(linePrefix);
                }
                break;
            case TokenType.OBJECT_END:
                if (linePrefix.length() >= indent.length()) {
                    linePrefix = linePrefix.substring(0, linePrefix.length() - indent.length());
                }
                buff.append(newline).append(linePrefix).append('}');
                break;
            case TokenType.ARRAY:
                inArray = true;
                buff.append('[');
                break;
            case TokenType.ARRAY_END:
                inArray = false;
                buff.append(']');
                break;
            case TokenType.COMMA:
                buff.append(',');
                if (!inArray) {
                    buff.append(newline).append(linePrefix);
                } else {
                    buff.append(space);
                }
                break;
            case TokenType.COLON:
                buff.append(':').append(space);
                break;
            default:
                return null;
            }
        }
    }


}
