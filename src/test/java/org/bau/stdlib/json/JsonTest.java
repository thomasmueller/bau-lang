package org.bau.stdlib.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.bau.stdlib.json.JsonReader.TokenType;
import org.junit.Test;

public class JsonTest {

//    @Test
//    public void testSpec() throws IOException {
//        File dir = new File(System.getProperty("user.home") + "/Downloads/JSONTestSuite/test_parsing");
//        for(File f : dir.listFiles()) {
//            RandomAccessFile r = new RandomAccessFile(f, "r");
//            byte[] data = new byte[(int) r.length()];
//            r.readFully(data);
//            String json = new String(data, StandardCharsets.UTF_8);
//            r.close();
//            // System.out.println("testing " + f.getName());
//            String n = JsonUtils.prettyPrint(json);
//            boolean valid = JsonUtils.validate(json);
//            if (f.getName().startsWith("i")) {
//                System.out.println("impl defined: " + valid + " " + f.getName() + " " + json + " => " + n);
//            }
//            if (valid) {
//                if (n == null) {
//                    JsonUtils.validate(json);
//                    n = JsonUtils.prettyPrint(json);
//
//                    fail(f.getName() +  " valid but null " + json);
//                }
//                if (f.getName().startsWith("n")) {
//                    System.out.println("should fail: " + f.getName() + " " + json + " => " + n);
//                    JsonUtils.validate(json);
//                }
//                String n2 = JsonUtils.prettyPrint(n);
//                assertEquals(n, n2);
//            } else {
//                if (f.getName().startsWith("y")) {
//                    JsonUtils.validate(json);
//                    n = JsonUtils.prettyPrint(json);
//                    fail(f.getName() + " " + json);
//                }
//            }
//        }
//    }

    @Test
    public void formatting() {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            buff.append("{\"\":");
        }
        buff.append("\"\"");
        for (int i = 0; i < 1000; i++) {
            buff.append("}");
        }
        assertEquals(72002, JsonUtils.prettyPrint(buff.toString()).length());
    }

    @Test
    public void validate() {
        assertTrue(JsonUtils.validate("null"));
        assertTrue(JsonUtils.validate("true"));
        assertTrue(JsonUtils.validate("false"));
        assertTrue(JsonUtils.validate("\"\""));
        assertTrue(JsonUtils.validate("[]"));
        assertTrue(JsonUtils.validate("{}"));
        assertTrue(JsonUtils.validate("{\"\":1}"));
        assertTrue(JsonUtils.validate("{\"\":1,\"\":2}"));
        assertTrue(JsonUtils.validate("[1,2]"));
        assertTrue(JsonUtils.validate(" \r\n\t1"));
        assertFalse(JsonUtils.validate("{1:1}"));
        assertFalse(JsonUtils.validate("{\"\":1,}"));
        assertFalse(JsonUtils.validate("{\"\" 1}"));
        assertFalse(JsonUtils.validate("{\"\":1,\"\":}"));
        assertFalse(JsonUtils.validate("{\"\":1 \"\":2}"));
        assertFalse(JsonUtils.validate("[1 2]"));
        assertFalse(JsonUtils.validate("[1,]"));
        assertFalse(JsonUtils.validate("[1,[]"));
        assertFalse(JsonUtils.validate("nul"));
        assertFalse(JsonUtils.validate("]"));
        assertFalse(JsonUtils.validate("["));
        assertFalse(JsonUtils.validate("-true"));
        assertFalse(JsonUtils.validate("{"));
        assertFalse(JsonUtils.validate("{}end"));
        assertFalse(JsonUtils.validate("{\"a\":\"b\"}/**/"));
        assertFalse(JsonUtils.validate("{\"\":"));
        assertFalse(JsonUtils.validate("{}}"));
        assertFalse(JsonUtils.validate(" \f1"));
    }

    @Test
    public void validateNesting() {
        StringBuilder buff = new StringBuilder();
        buff.repeat('[', 2000);
        buff.append("0");
        buff.repeat("]", 2000);
        assertTrue(JsonUtils.validate(buff.toString()));
        buff = new StringBuilder();
        buff.repeat("[", 2001);
        buff.append("0");
        buff.repeat("]", 2001);
        assertFalse(JsonUtils.validate(buff.toString()));
    }

    @Test
    public void validateStrings() {
        assertTrue(JsonUtils.validate("\"\""));
        assertTrue(JsonUtils.validate("\"a\""));
        assertTrue(JsonUtils.validate("\"\\u0000\""));
        assertFalse(JsonUtils.validate("\"\\a\""));
    }

    @Test
    public void validateNumbers() {
        assertTrue(JsonUtils.validate("1"));
        assertTrue(JsonUtils.validate("0"));
        assertTrue(JsonUtils.validate("-1"));
        assertTrue(JsonUtils.validate("-0"));
        assertTrue(JsonUtils.validate("-0.0"));
        assertTrue(JsonUtils.validate("0.0"));
        assertTrue(JsonUtils.validate("1e0"));
        assertTrue(JsonUtils.validate("1e00"));
        assertTrue(JsonUtils.validate("1e-0"));
        assertTrue(JsonUtils.validate("1e-00"));
        assertTrue(JsonUtils.validate("1e1"));
        assertTrue(JsonUtils.validate("1e-1"));
        assertTrue(JsonUtils.validate("1e10"));
        assertTrue(JsonUtils.validate("1e-10"));
        assertTrue(JsonUtils.validate("0e100"));
        assertTrue(JsonUtils.validate("0e-100"));
        assertTrue(JsonUtils.validate("3e1000"));
        assertTrue(JsonUtils.validate("3e-1000"));
        assertTrue(JsonUtils.validate("3e999999999999999999999"));
        assertTrue(JsonUtils.validate("3e-999999999999999999999"));
        assertTrue(JsonUtils.validate("999999999999999999999"));
        assertTrue(JsonUtils.validate("-999999999999999999999"));

        assertFalse(JsonUtils.validate(".0"));
        assertFalse(JsonUtils.validate("0."));
        assertFalse(JsonUtils.validate("-.0"));
        assertFalse(JsonUtils.validate("-0."));
        assertFalse(JsonUtils.validate("--1"));
        assertFalse(JsonUtils.validate("--0"));
        assertFalse(JsonUtils.validate("01"));
        assertFalse(JsonUtils.validate("-00"));
        assertFalse(JsonUtils.validate("NaN"));
        assertFalse(JsonUtils.validate("Inf"));
        assertFalse(JsonUtils.validate("-Inf"));
        assertFalse(JsonUtils.validate("-Infinity"));
        assertFalse(JsonUtils.validate("-Infinity"));
    }

    @Test
    public void random() {
        Random r = new Random(1);
        for (int i = 0; i < 100_000; i++) {
            int len = r.nextInt(10);
            StringBuilder buff = new StringBuilder(len);
            for (int j = 0; j < len; j++) {
                String chars = "012.eutrnfb/:,(){}[]-+\"\\\u1234\n\r\u0001\ud820\udc00";
                buff.append(chars.charAt(r.nextInt(chars.length())));
            }
            JsonUtils.prettyPrint(buff.toString());
            String s2 = JsonReader.decodeString(buff.toString());
            if (s2 != null) {
                String s3 = JsonBuilder.encode(s2);
                JsonReader json = new JsonReader(s3);
                String s4 = json.getString();
                assertEquals(s2, s4);
            }
        }
    }

    @Test
    public void iterateKeys() {
        String json = "{\"a\":10,\"b\":\"hello\",\"c\":null,\"d1\":true,\"d2\":false," +
                "\"e\":[],\"f\":[1],\"g\":[1,2],\"h\":{},\"i\":{\"i1\":1},\"j\":[{}],\"k\":1}";
        JsonReader obj = new JsonReader(json);

        assertEquals("a", obj.nextKey());
        JsonReader val = obj.value();
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

        assertEquals("j", obj.nextKey());
        assertEquals("k", obj.nextKey());

        assertEquals(null, val.nextKey());
        assertEquals(null, obj.nextKey());
    }

    @Test
    public void numbers() {
        String json = "{\"a\":[0,-9,-0,12.45,0.1,1e1,-0.3e-1,1e+2,1E-30]}";
        JsonReader obj = new JsonReader(json);
        JsonReader array = obj.get("a");
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
        JsonReader obj = new JsonReader(json);
        JsonReader array = obj.get("a");
        assertEquals("\n", array.nextValue().getString());
        assertEquals("", array.nextValue().getString());
        assertEquals("\"", array.nextValue().getString());
        assertEquals("\\", array.nextValue().getString());
        assertEquals("\u1234", array.nextValue().getString());
        assertEquals("/\b\f\n\r\t", array.nextValue().getString());
        assertEquals(null, array.nextValue());

        json = "{\"a\":[\"\\uD83D\\uDE03\",\"Line1\\nLine2\"]}";
        obj = new JsonReader(json);
        array = obj.get("a");
        assertEquals("\ud83d\ude03", array.nextValue().getString());
        assertEquals("Line1\nLine2", array.nextValue().getString());
        assertEquals(null, array.nextValue());
    }

    @Test
    public void directAccess() {
        String json = "{\"a\":10,\"b\":\"hello\",\"c\":null,\"d\":true,\"e\":[1]}";
        JsonReader obj = new JsonReader(json);
        assertEquals("hello", obj.get("b").value().getString());
        assertTrue(obj.get("d").value().getBoolean());
        assertNull(obj.get("f"));
    }

    @Test
    public void iterateOverTokens() {
        String json = "{\"a\":10,\"b\":\"hello\",\"c\":null,\"d\":true,\"e\":[1]}";
        JsonReader reader = new JsonReader(json);
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
