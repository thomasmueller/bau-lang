package org.bau.stdlib.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class CsvTest {

    @Test
    public void random() throws IOException {
        Random r = new Random(1);
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 100; j++) {
                String chars = "'1,x\\\'\n\r\t ";
                buff.append(chars.charAt(r.nextInt(chars.length())));
            }
            buff.append("\r\n");
        }
        Csv csv = Csv.newReader(new StringReader(buff.toString()));
        csv.setDelimiter('\'');
        while (true) {
            String[] row = csv.readRow(2);
            if (row == null) {
                break;
            }
        }
        csv.close();
    }

    @Test
    public void test() throws IOException {
        StringWriter writer = new StringWriter();
        Csv csv = Csv.newWriter(writer);
        csv.setDelimiter('\'');
        csv.setSeparator(';');
        csv.writeRow(new String[] {"id", "text"});
        csv.writeRow(new String[] {"1", "hello"});
        csv.writeRow(new String[] {"2", "world"});
        csv.close();
        String out = writer.toString();
        assertEquals("'id';'text'\n"
                + "'1';'hello'\n"
                + "'2';'world'\n"
                + "", out);
        out = "'id'; 'text'\n"
                + "'1' ; 'hello'\n"
                + "'2' ;'world'\n"
                + ";'end'\n"
                + "";
        csv = Csv.newReader(new StringReader(out));
        csv.setDelimiter('\'');
        csv.setSeparator(';');
        assertEquals("[id, text]", Arrays.toString(csv.readRow(2)));
        assertEquals("[1, hello]", Arrays.toString(csv.readRow(2)));
        assertEquals("[2, world]", Arrays.toString(csv.readRow(2)));
        assertEquals("[null, end]", Arrays.toString(csv.readRow(2)));
        assertNull(csv.readRow(2));
        csv.close();
    }

    @Test
    public void escape() throws IOException {
        StringWriter writer = new StringWriter();
        Csv csv = Csv.newWriter(writer);
        csv.setEscape('\'');
        csv.setDelimiter('\'');
        csv.setNewline("\r\n");
        csv.writeRow(new String[] {"id", "text"});
        csv.writeRow(new String[] {"'1", "hello 'you'"});
        csv.writeRow(new String[] {"'2", "'world'"});
        csv.close();
        String out = writer.toString();
        assertEquals("'id','text'\r\n"
                + "'''1','hello ''you'''\r\n"
                + "'''2','''world'''\r\n"
                + "", out);
        csv = Csv.newReader(new StringReader(out));
        csv.setEscape('\'');
        csv.setDelimiter('\'');
        assertEquals("[id, text]", Arrays.toString(csv.readRow(2)));
        assertEquals("['1, hello 'you']", Arrays.toString(csv.readRow(2)));
        assertEquals("['2, 'world']", Arrays.toString(csv.readRow(2)));
        csv.close();
    }

    @Test
    public void witoutDelimiter() throws IOException {
        StringWriter writer = new StringWriter();
        Csv csv = Csv.newWriter(writer);
        csv.setEscape((char) 0);
        csv.setDelimiter((char) 0);
        csv.writeRow(new String[] {"id", "text"});
        csv.writeRow(new String[] {"'1", "hello 'you'"});
        csv.writeRow(new String[] {"'2", "'world'"});
        csv.close();
        String out = writer.toString();
        assertEquals("id,text\n"
                + "'1,hello 'you'\n"
                + "'2,'world'\n"
                + "", out);
        csv = Csv.newReader(new StringReader(out));
        csv.setEscape((char) 0);
        csv.setDelimiter((char) 0);
        assertEquals("[id, text]", Arrays.toString(csv.readRow(2)));
        assertEquals("['1, hello 'you']", Arrays.toString(csv.readRow(2)));
        assertEquals("['2, 'world']", Arrays.toString(csv.readRow(2)));
        csv.close();
    }

    @Test
    public void backslashEscape() throws IOException {
        StringWriter writer = new StringWriter();
        Csv csv = Csv.newWriter(writer);
        csv.setEscape('\\');
        csv.writeRow(new String[] {"id", "name"});
        csv.writeRow(new String[] {"'1", "hello \"you\""});
        csv.writeRow(new String[] {"'2", "c:\\data\\"});
        csv.close();
        String out = writer.toString();
        assertEquals("\"id\",\"name\"\n"
                + "\"'1\",\"hello \\\"you\\\"\"\n"
                + "\"'2\",\"c:\\\\data\\\\\"\n"
                + "", out);
        csv = Csv.newReader(new StringReader(out));
        csv.setEscape('\\');
        assertEquals("[id, name]", Arrays.toString(csv.readRow(2)));
        assertEquals("['1, hello \"you\"]", Arrays.toString(csv.readRow(2)));
        assertEquals("['2, c:\\data\\]", Arrays.toString(csv.readRow(2)));
        csv.close();
    }

}
