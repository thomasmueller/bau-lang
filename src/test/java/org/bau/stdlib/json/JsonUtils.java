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
                    linePrefix += indent;
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


}
