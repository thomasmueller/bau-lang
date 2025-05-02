package org.bau.tools;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import org.bau.parser.Parser;
import org.bau.parser.Program;

public class Transpile {

    public static void main(String... args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: jar -cp ... " + Transpile.class.getName() + " <source.bau>");
            System.out.println("This will create a file named source.c");
            System.out.println("Afterwards, you need to compile it using eg. gcc -O3 source.c");
            System.out.println("and run it using ./a.out");
            return;
        }
        String sourceFileName = args[0];
        if (!sourceFileName.endsWith(".bau")) {
            System.out.println("Expected a file name with suffix '.bau', got " + sourceFileName);
            return;
        }
        System.out.println("Reading file " + sourceFileName);
        RandomAccessFile rf = new RandomAccessFile(sourceFileName, "r");
        byte[] data = new byte[(int) rf.length()];
        rf.readFully(data);
        rf.close();
        String s = new String(data, StandardCharsets.UTF_8);
        System.out.println("Transpiling");
        Parser p = new Parser(s);
        String c = "";
        try {
            Program prog = p.parse();
            c = prog.toC();
        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);
            return;
        }
        String cFileName = sourceFileName.substring(0, sourceFileName.length() - 3) + "c";
        System.out.println("Writing " + cFileName);
        RandomAccessFile rf2 = new RandomAccessFile(cFileName, "rw");
        data = c.getBytes(StandardCharsets.UTF_8);
        rf2.write(data);
        rf2.setLength(data.length);
        rf2.close();
        System.out.println("Done");
    }

}
