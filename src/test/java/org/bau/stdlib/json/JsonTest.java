package org.bau.stdlib.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.bau.stdlib.json.Json.TokenType;
import org.junit.Test;

public class JsonTest {

    @Test
    public void random() {
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            int len = r.nextInt(10);
            StringBuilder buff = new StringBuilder(len);
            for (int j = 0; j < len; j++) {
                String chars = "012.utrnfb/:,(){}[]-+\"\\\u1234\n\r\u0001\ud820\udc00";
                buff.append(chars.charAt(r.nextInt(chars.length())));
            }
            JsonUtils.prettyPrint(buff.toString());
            String s2 = Json.decodeString(buff.toString());
            if (s2 != null) {
                String s3 = JsonBuilder.encode(s2);
                Json json = new Json(s3);
                String s4 = json.getString();
                assertEquals(s2, s4);
            }
        }
    }

    @Test
    public void iterateKeys() {
        String json = "{\"a\":10,\"b\":\"hello\",\"c\":null,\"d1\":true,\"d2\":false," +
                "\"e\":[],\"f\":[1],\"g\":[1,2],\"h\":{},\"i\":{\"i1\":1}}";
        Json obj = new Json(json);

        assertEquals("a", obj.nextKey());
        Json val = obj.value();
        assertEquals(TokenType.NUMBER, val.getTokenType());
        assertEquals("10", val.getNumber());

        assertEquals("b", obj.nextKey());
        val = obj.value();
        assertEquals(TokenType.STRING, val.getTokenType());
        assertEquals("hello", obj.value().getString());

        assertEquals("c", obj.nextKey());
        val = obj.value();
        assertEquals(TokenType.NULL, val.getTokenType());
        assertEquals(null, obj.value().getString());

        assertEquals("d1", obj.nextKey());
        val = obj.value();
        assertEquals(TokenType.TRUE, val.getTokenType());
        assertTrue(val.getBoolean());

        assertEquals("d2", obj.nextKey());
        val = obj.value();
        assertEquals(TokenType.FALSE, val.getTokenType());
        assertTrue(!val.getBoolean());

        assertEquals("e", obj.nextKey());
        val = obj.value();
        assertEquals(TokenType.ARRAY, val.getTokenType());
        assertNull(val.nextValue());

        assertEquals("f", obj.nextKey());
        val = obj.value();
        assertEquals(TokenType.ARRAY, val.getTokenType());
        assertEquals("1", val.nextValue().getNumber());
        assertNull(val.nextValue());

        assertEquals("g", obj.nextKey());
        val = obj.value();
        assertEquals(TokenType.ARRAY, val.getTokenType());
        assertEquals("1", val.nextValue().getNumber());
        assertEquals("2", val.nextValue().getNumber());
        assertNull(val.nextValue());

        assertEquals("h", obj.nextKey());
        val = obj.value();
        assertEquals(TokenType.OBJECT, val.getTokenType());
        assertNull(val.nextKey());

        assertEquals("i", obj.nextKey());
        val = obj.value();
        assertEquals(TokenType.OBJECT, val.getTokenType());
        assertEquals("i1", val.nextKey());
        assertEquals(null, val.nextKey());
        assertEquals(null, obj.nextKey());
    }

    @Test
    public void numbers() {
        String json = "{\"a\":[0,-9,-0,12.45,0.1,1e1,-0.3e-1,1e+2,1E-30]}";
        Json obj = new Json(json);
        Json array = obj.get("a");
        assertEquals("0", array.nextValue().getNumber());
        assertEquals("-9", array.nextValue().getNumber());
        assertEquals("-0", array.nextValue().getNumber());
        assertEquals("12.45", array.nextValue().getNumber());
        assertEquals("0.1", array.nextValue().getNumber());
        assertEquals("1e1", array.nextValue().getNumber());
        assertEquals("-0.3e-1", array.nextValue().getNumber());
        assertEquals("1e+2", array.nextValue().getNumber());
        assertEquals("1E-30", array.nextValue().getNumber());
        assertEquals(null, array.nextValue());
    }

    @Test
    public void escapeStrings() {
        String json = "{\"a\":[\"\\n\",\"\",\"\\\"\",\"\\\\\",\"\\u1234\",\"\\/\\b\\f\\n\\r\\t\"]}";
        Json obj = new Json(json);
        Json array = obj.get("a");
        assertEquals("\n", array.nextValue().getString());
        assertEquals("", array.nextValue().getString());
        assertEquals("\"", array.nextValue().getString());
        assertEquals("\\", array.nextValue().getString());
        assertEquals("\u1234", array.nextValue().getString());
        assertEquals("/\b\f\n\r\t", array.nextValue().getString());
        assertEquals(null, array.nextValue());

        json = "{\"a\":[\"\\uD83D\\uDE03\",\"Line1\nLine2\"]}";
        obj = new Json(json);
        array = obj.get("a");
        assertEquals("\ud83d\ude03", array.nextValue().getString());
        assertEquals("Line1\nLine2", array.nextValue().getString());
        assertEquals(null, array.nextValue());
    }

    @Test
    public void directAccess() {
        String json = "{\"a\":10,\"b\":\"hello\",\"c\":null,\"d\":true,\"e\":[1]}";
        Json obj = new Json(json);
        assertEquals("hello", obj.get("b").value().getString());
        assertTrue(obj.get("d").value().getBoolean());
        assertNull(obj.get("f"));
    }

    @Test
    public void iterateOverTokens() {
        String json = "{\"a\":10,\"b\":\"hello\",\"c\":null,\"d\":true,\"e\":[1]}";
        Json reader = new Json(json);
        assertEquals(0, reader.getPos());
        assertEquals(TokenType.OBJECT, reader.nextToken());
        assertEquals(null, reader.getEscapedValue());
        assertEquals(TokenType.STRING, reader.nextToken());
        assertEquals("a", reader.getEscapedValue());
        assertEquals(TokenType.COLON, reader.nextToken());
        assertEquals(null, reader.getEscapedValue());
        assertEquals(TokenType.NUMBER, reader.nextToken());
        assertEquals("10", reader.getEscapedValue());
        assertEquals(TokenType.COMMA, reader.nextToken());
        int pos = reader.getPos();
        assertEquals(TokenType.STRING, reader.nextToken());
        assertEquals("b", reader.getEscapedValue());
        assertEquals(TokenType.COLON, reader.nextToken());
        assertEquals(TokenType.STRING, reader.nextToken());
        assertEquals("hello", reader.getEscapedValue());
        reader.setPos(pos);
        assertEquals(TokenType.STRING, reader.nextToken());
        assertEquals("b", reader.getEscapedValue());
        assertEquals(TokenType.COLON, reader.nextToken());
        assertEquals(TokenType.STRING, reader.nextToken());
        assertEquals("hello", reader.getEscapedValue());
    }

    @Test
    public void build() {
        JsonBuilder build = new JsonBuilder();
        build.object();
        build.key("a").value(10);
        build.key("b").value("hello");
        build.key("c").value(null);
        build.key("d").value(true);
        build.key("e").array().value(1).endArray();
        build.endObject();
        String json = build.toString();
        assertEquals("{\"a\":10,\"b\":\"hello\",\"c\":null,\"d\":true,\"e\":[1]}", json);
    }

    @Test
    public void prettyPrint() {
        String json = "{\"a\":10,\"b\":20}";
        assertEquals("{\n"
                + "  \"a\": 10,\n"
                + "  \"b\": 20\n"
                + "}", JsonUtils.prettyPrint(json));
        assertEquals(json, JsonUtils.minify(JsonUtils.prettyPrint(json)));

        json = "{\"a\":{\"b\":[1,null,3.14,{\"c\":true},{}]}}";
        assertEquals("{\n"
                + "  \"a\": {\n"
                + "    \"b\": [1, null, 3.14, {\n"
                + "      \"c\": true\n"
                + "    }, {}]\n"
                + "  }\n"
                + "}", JsonUtils.prettyPrint(json));
        assertEquals(json, JsonUtils.minify(JsonUtils.prettyPrint(json)));

        json = "{\n"
                + "  \"name\": \"Alice\",\n"
                + "  \"age\": 25,\n"
                + "}";
        assertEquals("{\n"
                + "  \"name\": \"Alice\",\n"
                + "  \"age\": 25,\n"
                + "  \n"
                + "}", JsonUtils.prettyPrint(json));

        json = "{\n"
                + "  \"mixed\": [1, \"two\", null, true, {\"key\": \"value\"}]\n"
                + "}";
        assertEquals("{\n"
                + "  \"mixed\": [1, \"two\", null, true, {\n"
                + "    \"key\": \"value\"\n"
                + "  }]\n"
                + "}", JsonUtils.prettyPrint(json));

        // error
        json = "1fg";
        assertEquals(null, JsonUtils.prettyPrint(json));
    }

}
