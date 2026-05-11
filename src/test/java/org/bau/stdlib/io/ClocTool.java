package org.bau.stdlib.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bau.stdlib.util.Hash;

public class ClocTool {

    private static final boolean PER_FILE = false;
    private static final String[] IGNORE = { "asm", "at", "class", "com", "css", "effekt", "ena", "html", "idx", "img",
            "jar", "json", "kk", "lst", "lua", "lz4", "md", "MF", "out", "pack", "png", "prefs", "properties", "rev",
            "sample", "sh", "tiff", "toml", "txt", "wasm", "wat", "xlsx", "xml" };
    private static final String[] TYPES = {
            "c     | C          | // | /* | - | */",
            "cpp   | C++        | // | /* | - | */",
            "java  | Java       | // | /* | - | */",
            "go    | Go         | // | /* | - | */",
            "js    | JavaScript | // | /* | - | */",
            "zig   | Zig        | // | /* | - | */",
            "rs    | Rust       | // | /* | - | */",
            "kt    | Kotlin     | // | /* | + | */",
            "swift | Swift      | // | /* | + | */",
            "v     | VLang      | // | /* | + | */",
            "py    | Python     | #  | \"\"\" | - | \"\"\" ",
            "nim   | Nim        | #  | #[ | + |  ]#",
            "bau   | Bau        | #  | ## | + | ##" };
    private static HashSet<String> IGNORE_SET = new HashSet<>();

    static {
        for (String s : IGNORE) {
            IGNORE_SET.add(s);
        }
    }

    static class Stats {
        String type;
        int files;
        int blank;
        int comment;
        int code;
    }

    public static void main(String... args) throws IOException {
        List<File> list = listFiles("./src/test");
        HashMap<String, Stats> stats = new HashMap<>();
        HashSet<String> unique = new HashSet<>();
        for (File f : list) {
            printLinesOfCode(f, stats, unique);
        }
        System.out.println("Language                         files          blank        comment           code");
        System.out.println("--------                         -----          -----        -------           ----");
        for (Stats s : stats.values()) {
            System.out.printf("%-28s %9d      %9d      %9d      %9d\n", s.type, s.files, s.blank,
                    s.comment, s.code);
        }
    }

    private static void printLinesOfCode(File f, HashMap<String, Stats> stats, HashSet<String> unique) throws IOException {
        if (!f.isFile()) {
            return;
        }
        String name = f.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot <= 0) {
            return;
        }
        String suffix = name.substring(lastDot + 1);
        if (IGNORE_SET.contains(suffix)) {
            return;
        }
        // if (!"bau".equals(suffix)) {
        //     return;
        // }
        String type = null;
        String line = null;
        String start = null;
        boolean nested = false;
        String end = null;
        for (String t : TYPES) {
            if (t.startsWith(suffix + " ")) {
                String[] mode = t.split("\\|");
                type = mode[1].trim();
                line = mode[2].trim();
                start = mode[3].trim();
                nested = mode[4].trim().equals("+");
                end = mode[5].trim();
                break;
            }
        }
        Stats stat = stats.get(suffix);
        if (stat == null) {
            stat = new Stats();
            stat.type = type;
            stats.put(suffix, stat);
        }
        RandomAccessFile file = new RandomAccessFile(f, "r");
        byte[] data = new byte[(int) file.length()];
        file.readFully(data);
        file.close();
        long hash = Hash.hashCode(data);
        if (!unique.add("" + hash)) {
            return;
        }
        String s = new String(data, StandardCharsets.UTF_8);
        StringBuilder buff = new StringBuilder(s.length());
        int commentLevel = 0;
        boolean lineComment = false;
        boolean lineWithString = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                buff.append(c);
            } else if (c == '\n') {
                buff.append(c);
                lineWithString = false;
                lineComment = false;
            } else if (lineComment) {
                buff.append('#');
            } else if (commentLevel == 0) {
                if (!start.isEmpty() && c == start.charAt(0)
                        && start.equals(s.substring(i, Math.min(s.length(), i + start.length())))) {
                    // block comment
                    buff.append('#');
                    i += start.length() - 1;
                    commentLevel = 1;
                } else if (c == '\"' || c == '\'') {
                    // we assume the string is single line and doesn't have a comment
                    // (this is a simplification, yes, and won't match everything correctly)
                    lineWithString = true;
                    buff.append('c');
                } else if (lineWithString) {
                    buff.append('c');
                } else if (!line.isEmpty() && c == line.charAt(0)
                        && line.equals(s.substring(i, Math.min(s.length(), i + line.length())))) {
                    // line comment
                    buff.append('#');
                    lineComment = true;
                } else {
                    buff.append('c');
                }
            } else {
                // commentLevel > 0
                buff.append('#');
                if (nested && c == start.charAt(0)
                        && start.equals(s.substring(i, Math.min(s.length(), i + start.length())))) {
                    commentLevel++;
                } else if (c == end.charAt(0)
                        && end.equals(s.substring(i, Math.min(s.length(), i + end.length())))) {
                    commentLevel--;
                    i += end.length() - 1;
                }
            }
        }
        String[] lines = buff.toString().split("\n");
        int code = 0;
        int comments = 0;
        int lineCount = lines.length;
        if (s.endsWith("\n\n")) {
            // trailing newline is counted as empty line
            lineCount++;
        }
        for (String l : lines) {
            l = l.trim();
            if (l.isEmpty()) {
                continue;
            }
            if (l.indexOf('c') >= 0) {
                code++;
            } else if (l.indexOf('#') >= 0) {
                comments++;
            }
        }
        int blank = lineCount - code - comments;
        if (PER_FILE) {
            System.out.printf("%-28s %9d      %9d      %9d      %9d\n", f.getName(), 1, blank,
                    comments, code);
        }
        stat.code += code;
        stat.comment += comments;
        stat.files++;
        stat.blank += blank;
    }

    private static List<File> listFiles(String path) {
        ArrayList<File> result = new ArrayList<>();
        ArrayList<File> dirs = new ArrayList<>();
        File f = new File(path);
        if (f.isDirectory()) {
            dirs.add(f);
        } else {
            result.add(f);
        }
        while (!dirs.isEmpty()) {
            File dir = dirs.removeLast();
            File[] list = dir.listFiles();
            if (list == null) {
                continue;
            }
            for (File file : list) {
                if (file.isDirectory()) {
                    dirs.add(file);
                } else {
                    result.add(file);
                }
            }
        }
        return result;
    }
}
