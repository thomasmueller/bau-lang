package org.bau.stdlib.xml;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.bau.stdlib.xml.XMLReader.TokenType;
import org.junit.Test;

public class XMLTest {

    @Test
    public void randomData() {
        Random r = new Random(1);
        String[] list = new String[] { " ", "\r", "\n", "\t", ">", "<", "=", "?", "!", ";",
                "DATA", "[", "]", "<![CDATA[", "/", "&", "gt", "lt", "amp",
                "quot", "apos", "&#x", "&#1", "&gt", "&amp;", "0", "a", "d", "f", "ab" };
        for (int i = 0; i < 10_000; i++) {
            int len = r.nextInt(30);
            StringBuilder buff = new StringBuilder();
            for (int j = 0; j < len; j++) {
                switch (r.nextInt(10)) {
                case 0:
                    buff.append('<');
                    break;
                case 1:
                    buff.append('>');
                    break;
                case 2:
                    buff.append('/');
                    break;
                case 3:
                    buff.append('x');
                    break;
                case 4:
                case 5:
                case 6:
                    buff.append(list[r.nextInt(list.length)]);
                    break;
                }
            }
            String s = buff.toString();
            XMLReader.decodeString(s);
            XMLReader reader = new XMLReader(s);
            while(true) {
                TokenType t = reader.nextToken();
                if (t == TokenType.END) {
                    break;
                }
            }
        }
    }

    @Test
    public void decodeString() {
        assertEquals("hello", XMLReader.decodeString("hello"));
        assertEquals("test <>&\"'A ", XMLReader.decodeString("test &lt;&gt;&amp;&quot;&apos;&#65;&#x20;"));
        assertEquals(null, XMLReader.decodeString("invalid&test"));
        assertEquals(null, XMLReader.decodeString("invalid&#X;"));
        assertEquals(null, XMLReader.decodeString("invalid&#xg;"));
        assertEquals(null, XMLReader.decodeString("invalid&hm;"));
    }

    @Test
    public void simple() {
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<hello>world</hello>\n"
                + "", new XMLBuilder().
                    declaration().
                    element("hello").text("world").endElement().
                    toString());
        assertEquals("<hello><world/>\n"
                + "</hello>\n"
                + "", new XMLBuilder().
                    element("hello").element("world").endElement().endElement().
                    toString());
        assertEquals("<hello who=\"world\"/>\n", new XMLBuilder().
                    element("hello").attribute("who", "world").endElement().
                    toString());
        assertEquals("<hello><![CDATA[test]]></hello>\n", new XMLBuilder().
                    element("hello").cdata("test").endElement().
                    toString());
    }

    @Test
    public void escape() {
        assertEquals("<hello x=\"value &#39;&quot;&lt;&amp;&gt;&#xf6;\">text &#39;&quot;&lt;&amp;&gt;&#xf6;</hello>\n"
                + "", new XMLBuilder().
                    element("hello").attribute("x", "value '\"<&>ö").text("text '\"<&>ö").endElement().
                    toString());
    }

    @Test
    public void iterateOverTokens() {
        String xml = "<hello>world</hello>";
        XMLReader reader;
        reader = new XMLReader(xml);
        assertEquals(0, reader.getPos());
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("hello", reader.getEscapedValue());
        assertEquals(TokenType.TEXT, reader.nextToken());
        assertEquals("world", reader.getEscapedValue());
        assertEquals(TokenType.TAG_END, reader.nextToken());
        assertEquals("hello", reader.getEscapedValue());
        assertEquals(TokenType.END, reader.nextToken());

        xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><hello /><world/>";
        reader = new XMLReader(xml);
        assertEquals(TokenType.PROCESSING_INSTRUCTION, reader.nextToken());
        assertEquals("?xml version=\"1.0\" encoding=\"UTF-8\"?", reader.getEscapedValue());
        assertEquals(TokenType.TAG_EMPTY, reader.nextToken());
        assertEquals("hello", reader.getEscapedValue());
        assertEquals(TokenType.TAG_EMPTY, reader.nextToken());
        assertEquals("world", reader.getEscapedValue());
        assertEquals(TokenType.END, reader.nextToken());

        xml = "<!-- comment --><hello/>";
        reader = new XMLReader(xml);
        assertEquals(TokenType.COMMENT, reader.nextToken());
        assertEquals("!-- comment --", reader.getEscapedValue());
        assertEquals(TokenType.TAG_EMPTY, reader.nextToken());
        assertEquals("hello", reader.getEscapedValue());
        assertEquals(TokenType.END, reader.nextToken());

        xml = "<hello who=\"world\"/>";
        reader = new XMLReader(xml);
        assertEquals(TokenType.TAG_EMPTY, reader.nextToken());
        assertEquals("hello", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("who", reader.getEscapedValue());
        assertEquals(TokenType.VALUE, reader.nextToken());
        assertEquals("world", reader.getEscapedValue());
        assertEquals(TokenType.END, reader.nextToken());

        xml = "<hello who = \"world\" empty=\"\" />";
        reader = new XMLReader(xml);
        assertEquals(TokenType.TAG_EMPTY, reader.nextToken());
        assertEquals("hello", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("who", reader.getEscapedValue());
        assertEquals(TokenType.VALUE, reader.nextToken());
        assertEquals("world", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("empty", reader.getEscapedValue());
        assertEquals(TokenType.VALUE, reader.nextToken());
        assertEquals("", reader.getEscapedValue());
        assertEquals(TokenType.END, reader.nextToken());

        xml = "<test><![CDATA[<greeting>Hello, world!</greeting>]]></test>";
        reader = new XMLReader(xml);
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("test", reader.getEscapedValue());
        assertEquals(TokenType.CDATA, reader.nextToken());
        assertEquals("<greeting>Hello, world!</greeting>", reader.getEscapedValue());
        assertEquals(TokenType.TAG_END, reader.nextToken());
        assertEquals("test", reader.getEscapedValue());
        assertEquals(TokenType.END, reader.nextToken());
    }

    @Test
    public void iterateOverRandom() {
        String xml = "<?/</<";
        XMLReader reader;
        reader = new XMLReader(xml);
        assertEquals(TokenType.ERROR, reader.nextToken());
    }

    @Test
    public void iterateOverHtmlTokens() {
        String html = "<button disabled>";
        XMLReader reader;
        reader = new XMLReader(html);
        assertEquals(0, reader.getPos());
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("button", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("disabled", reader.getEscapedValue());
        assertEquals(TokenType.END, reader.nextToken());

        html = "<div class=main>";
        reader = new XMLReader(html);
        assertEquals(0, reader.getPos());
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("div", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("class", reader.getEscapedValue());
        assertEquals(TokenType.VALUE, reader.nextToken());
        assertEquals("main", reader.getEscapedValue());
        assertEquals(TokenType.END, reader.nextToken());

        html = "<!DOCTYPE html>\n"
                + "        <BODY>\n"
                + "            <div class=main>\n"
                + "                <input type=\"checkbox\" disabled>\n"
                + "                <p>This paragraph is open\n"
                + "            </div>\n"
                + "        </BODY>";
        reader = new XMLReader(html);
        assertEquals(0, reader.getPos());
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("!DOCTYPE", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("html", reader.getEscapedValue());
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("BODY", reader.getEscapedValue());
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("div", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("class", reader.getEscapedValue());
        assertEquals(TokenType.VALUE, reader.nextToken());
        assertEquals("main", reader.getEscapedValue());
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("input", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("type", reader.getEscapedValue());
        assertEquals(TokenType.VALUE, reader.nextToken());
        assertEquals("checkbox", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("disabled", reader.getEscapedValue());
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("p", reader.getEscapedValue());
        assertEquals(TokenType.TEXT, reader.nextToken());
        assertEquals("This paragraph is open\n"
                + "            ", reader.getEscapedValue());
        assertEquals(TokenType.TAG_END, reader.nextToken());
        assertEquals("div", reader.getEscapedValue());
        assertEquals(TokenType.TAG_END, reader.nextToken());
        assertEquals("BODY", reader.getEscapedValue());
        assertEquals(TokenType.END, reader.nextToken());

        html = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "        <root attr1='value with &gt; char'>\n"
                + "            <data>\n"
                + "                <div id='header' />";
        reader = new XMLReader(html);
        assertEquals(0, reader.getPos());
        assertEquals(TokenType.PROCESSING_INSTRUCTION, reader.nextToken());
        assertEquals("?xml version=\"1.0\" encoding=\"UTF-8\"?", reader.getEscapedValue());
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("root", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("attr1", reader.getEscapedValue());
        assertEquals(TokenType.VALUE, reader.nextToken());
        assertEquals("value with &gt; char", reader.getEscapedValue());
        assertEquals(TokenType.TAG_START, reader.nextToken());
        assertEquals("data", reader.getEscapedValue());
        assertEquals(TokenType.TAG_EMPTY, reader.nextToken());
        assertEquals("div", reader.getEscapedValue());
        assertEquals(TokenType.ATTRIBUTE, reader.nextToken());
        assertEquals("id", reader.getEscapedValue());
        assertEquals(TokenType.VALUE, reader.nextToken());
        assertEquals("header", reader.getEscapedValue());
    }


}
