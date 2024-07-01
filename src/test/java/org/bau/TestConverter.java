package org.bau;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.junit.Test;

public class TestConverter {
    
    @Test
    public void demo() throws IOException {
        InputStream in = getClass().getResourceAsStream("demo.bau");
        LineNumberReader r = new LineNumberReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder buff = new StringBuilder();
        while (true) {
            String line = r.readLine();
            if (line == null) {
                break;
            }
            buff.append(line).append("\n");
        }
        String source = buff.toString();
        Program p = new Parser(source).parse();
        RandomAccessFile f = new RandomAccessFile("demo.c", "rw");
        String c = p.toC();
        byte[] data = c.getBytes(StandardCharsets.UTF_8);
        f.write(data);
        f.setLength(data.length);
        f.close();
        // gcc -O3 demo.c; ./a.out
        
    }

}
