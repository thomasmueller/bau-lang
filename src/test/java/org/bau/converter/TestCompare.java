package org.bau.converter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.junit.Test;

/*

cd got
find . -name "*.c" -exec gcc {} -o {}.out \;
find . -name "*.c.out" -exec echo "############################# " {} \; -exec {} \;
cd ..

cp got/*.c src/test/resources/org/bau/converter/

find src/test -name "*.c" -delete
find src/test -name "*.c.out" -delete 


*/
public class TestCompare {

    @Test
    public void arrayBounds() throws IOException {
        test("arrayBounds");
    }

    @Test
    public void arrayFunction() throws IOException {
        test("arrayFunction");
    }

    @Test
    public void arrayLen() throws IOException {
        test("arrayLen");
    }
    
    @Test
    public void arrayMurmur() throws IOException {
        test("arrayMurmur");
    }

    @Test
    public void arrayRange() throws IOException {
        test("arrayRange");
    }

    @Test
    public void arrayReadInt() throws IOException {
        test("arrayReadInt");
    }

    @Test
    public void arrayShortFunction() throws IOException {
        test("arrayShortFunction");
    }

    @Test
    public void byteArray() throws IOException {
        test("byteArray");
    }

    @Test
    public void comment() throws IOException {
        test("comment");
    }

    @Test
    public void factorial() throws IOException {
        test("factorial");
    }

    @Test
    public void fileRead() throws IOException {
        test("fileRead");
    }

    @Test
    public void forLoopBreakContinue() throws IOException {
        test("forLoopBreakContinue");
    }

    @Test
    public void functionDeclareAndImplementLater() throws IOException {
        test("functionDeclareAndImplementLater");
    }
    
    @Test
    public void functions() throws IOException {
        test("functions");
    }
    
    @Test
    public void functionsBubbleSort() throws IOException {
        test("functionsBubbleSort");
    }

    @Test
    public void helloWorld() throws IOException {
        test("helloWorld");
    }
    
    @Test
    public void hexOrd() throws IOException {
        test("hexOrd");
    }

    @Test
    public void literals() throws IOException {
        test("literals");
    }
    
    @Test
    public void map() throws IOException {
        test("map");
    }    
    
    @Test
    public void modulesList() throws IOException {
        test("modulesList");
    }

    @Test
    public void modulesExample() throws IOException {
        test("modulesExample");
    }

    @Test
    public void modulesExample2() throws IOException {
        test("modulesExample2");
    }

    @Test
    public void nullMap() throws IOException {
        test("nullMap");
    }

    @Test
    public void nullPreventNullAccess() throws IOException {
        test("nullPreventNullAccess");
    }

    @Test
    public void operatorOrShortcut() throws IOException {
        test("operatorOrShortcut");
    }

    @Test
    public void operators() throws IOException {
        test("operators");
    }

    @Test
    public void throwCatch() throws IOException {
        test("throwCatch");
    }
    
    @Test
    public void throwCatchDemo() throws IOException {
        test("throwCatchDemo");
    }

    @Test
    public void throwCatchVoidFunction() throws IOException {
        test("throwCatchVoidFunction");
    }

    @Test
    public void typeOpenClose() throws IOException {
        test("typeOpenClose");
    }

    @Test
    public void typeWithParameter() throws IOException {
        test("typeWithParameter");
    }

    @Test
    public void utils() throws IOException {
        test("utils");
    }

    private String readResource(String fileName) throws IOException {
        InputStream in = getClass().getResourceAsStream(fileName);
        if (in == null) {
            return "";
        }
        LineNumberReader r = new LineNumberReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder buff = new StringBuilder();
        while (true) {
            String line = r.readLine();
            if (line == null) {
                break;
            }
            buff.append(line).append("\n");
        }
        return buff.toString();
    }
    
    private void test(String file) throws IOException {
        String source = readResource(file + ".bau");
        String expected = readResource(file + ".c");
        String expectedMarkdown = readResource(file + ".md");
        Program p = new Parser(source).parse();
        String c = p.toC();
        String md = p.toMarkdown();
        new File("got").mkdir();
        writeFile("got/" + file + ".c", c);
        if (md != null) {
            writeFile("got/" + file + ".md", md);
        }
        assertEquals(file, expected, c);
        if (!expectedMarkdown.isEmpty()) {
            assertEquals(file, expectedMarkdown, md);
        }
    }
    
    private void writeFile(String fileName, String s) throws IOException {
        RandomAccessFile f = new RandomAccessFile(fileName, "rw");
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        f.write(data);
        f.setLength(data.length);
        f.close();
    }

}
