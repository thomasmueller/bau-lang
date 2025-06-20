package org.bau.tools;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.bau.parser.Parser;
import org.bau.parser.Program;

public class Transpile {

    public static void main(String... args) throws Exception {
        ArrayList<String> sourceFileNames = new ArrayList<>();
        ArrayList<String> gccOptions = new ArrayList<>();
        boolean useTmMalloc = false;
        boolean traceRefCounts = false;
        for (String a : args) {
            if (a.equals("-useTmMalloc")) {
                useTmMalloc = true;
            } else if (a.equals("-traceRefCounts")) {
                traceRefCounts = true;
            } else if (a.startsWith("-")) {
                gccOptions.add(a);
            } else if (a.endsWith("*.bau")) {
                String path = a.substring(0, a.length() - "*.bau".length());
                File dir = new File(path);
                if (dir.isDirectory()) {
                    System.out.println("Not a directory: " + path);
                } else if (!dir.exists()) {
                    System.out.println("Directory does not exist: " + path);
                } else {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.getName().endsWith(".bau")) {
                                sourceFileNames.add(f.getAbsolutePath());
                            }
                        }
                    }
                }
            } else {
                sourceFileNames.add(a);
            }
        }
        if (sourceFileNames.isEmpty()) {
            System.out.println("Usage: jar -cp ... " + Transpile.class.getName() + " <options> <source.bau>");
            System.out.println("This will create a file named source.c");
            System.out.println("Afterwards, you need to compile it using eg. gcc -O3 source.c");
            System.out.println("and run it using ./a.out");
            System.out.println("Transpiler options:");
            System.out.println("    -useTmMalloc     (enable faster malloc)");
            System.out.println("    -traceRefCounts  (enable tracing for reference counts)");
            System.out.println("Other options are for gcc.");
            System.out.println("To compile directly to a binary, append gcc options such as '-O3'");
            System.out.println("Example: jar -cp ... " + Transpile.class.getName() + " -O3 demo.bau");
            return;
        }
        for (String sourceFileName : sourceFileNames) {
            if (!sourceFileName.endsWith(".bau")) {
                System.out.println("Expected a file name with suffix '.bau', got " + sourceFileName);
                return;
            }
            System.out.println("Transpiling " + sourceFileName);
            RandomAccessFile rf = new RandomAccessFile(sourceFileName, "r");
            byte[] data = new byte[(int) rf.length()];
            rf.readFully(data);
            rf.close();
            String s = new String(data, StandardCharsets.UTF_8);
            Parser p = new Parser(s);
            String c = "";
            try {
                Program prog = p.parse();
                if (useTmMalloc) {
                    prog.useTmMalloc();
                }
                if (traceRefCounts) {
                    prog.traceRefCounts();
                }
                c = prog.toC();
            } catch (Exception e) {
                System.out.println("Error");
                System.out.println(e.getMessage());
                e.printStackTrace(System.out);
                return;
            }
            String cFileName = sourceFileName.substring(0, sourceFileName.length() - 3) + "c";
            RandomAccessFile rf2 = new RandomAccessFile(cFileName, "rw");
            data = c.getBytes(StandardCharsets.UTF_8);
            rf2.write(data);
            rf2.setLength(data.length);
            rf2.close();
            if (!gccOptions.isEmpty()) {
                String outFileName = sourceFileName.substring(0, sourceFileName.length() - 4);
                ArrayList<String> gcc = new ArrayList<>();
                gcc.add("gcc");
                gcc.addAll(gccOptions);
                gcc.add(cFileName);
                gcc.add("-o");
                gcc.add(outFileName);
                System.out.println("Compiling " + cFileName);
                int exitCode = Service.runProcess(gcc.toArray(new String[0]));
                if (exitCode != 0) {
                    System.out.println("Error: " + exitCode);
                }
            }
        }
    }

}
