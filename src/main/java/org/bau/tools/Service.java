package org.bau.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import org.bau.parser.Parser;
import org.bau.parser.Program;

public class Service {
    public static void main(String... args) throws Exception {
        System.out.println("Waiting for changes to the file 'demo.bau'");
        File f = new File("demo.bau");
        File fc = new File("demo.c");
        File md = new File("demo.md");
        f.createNewFile();
        System.out.println("Waiting =======================================");
        long lastModified = f.lastModified();
        while (true) {
            while (true) {
                Thread.sleep(100);
                long lastModified2 = f.lastModified();
                if (lastModified2 != lastModified) {
                    lastModified = lastModified2;
                    break;
                }
            }
            RandomAccessFile rf = new RandomAccessFile(f, "r");
            byte[] data = new byte[(int) f.length()];
            rf.readFully(data);
            rf.close();
            String s = new String(data, StandardCharsets.UTF_8);
            System.out.println("Parsing =======================================");
            Parser p = new Parser(s);
            String c;
            String markdown;
            try {
                Program prog = p.parse();
                c = prog.toC();
                markdown = prog.toMarkdown();
            } catch (Exception e) {
                System.out.println("Error =======================================");
                System.out.println(e.getMessage());
                e.printStackTrace(System.out);
                System.out.println("Waiting =====================================");
                continue;
            }

            RandomAccessFile rf2 = new RandomAccessFile(fc, "rw");
            data = c.getBytes(StandardCharsets.UTF_8);
            rf2.write(data);
            rf2.setLength(data.length);
            rf2.close();
            if(markdown != null) {
                RandomAccessFile fmd = new RandomAccessFile(md, "rw");
                data = markdown.getBytes(StandardCharsets.UTF_8);
                fmd.write(data);
                fmd.setLength(data.length);
                fmd.close();
            }
            System.out.println("Interpreting ====================================");
            try {
                long start = System.currentTimeMillis();
                Program prog = new Parser(s).parse();
                String result = prog.run();
                System.out.println(result);
                long ticksExecuted = prog.getTicksExecuted();
                long time = System.currentTimeMillis() - start;
                System.out.println("Ticks: " + ticksExecuted + " (" + time + " ms)");
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            System.out.println("Compiling =======================================");
            int exitCode = runProcess("gcc", "-O3", fc.toString());
            // int exitCode = runProcess("gcc", fc.toString());
            File f2 = new File("a.out");
            if (exitCode == 0) {
                System.out.println("Running =========================================");
                long start = System.currentTimeMillis();
                runProcess(f2.getAbsolutePath());
                long time = System.currentTimeMillis() - start;
                System.out.println("(" + time + " ms)");
            }
            System.out.println("Waiting =========================================");
        }
    }


    public static int runProcess(String... command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        Thread outThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        outThread.start();
        int exitCode = process.waitFor();
        outThread.join();
        return exitCode;
    }

}
