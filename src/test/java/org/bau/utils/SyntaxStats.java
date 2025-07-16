package org.bau.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.TreeSet;

public class SyntaxStats {

    // these we do not list those
    final static String[] LANGUAGE_WORDS = {
            "Atoi", "Arg", "argc", "argv", "atol", "atoi", "arg", "argCount",
            "BigInt", "BigInteger", "BufferedOutputStream", "break", "bool", "boolean",
            "class", "continue", "compareTo", "const", "Cmp", "char", "chr",
            "Div", "define", "double", "def",
            "else", "env",
            "fun", "for", "fn", "func", "false", "Flush", "Fprintf", "f64", "float64", "False",
            "if", "int", "import", "Integer", "include", "i8", "i32", "IOException", "Int64",
            "let",
            "main", "mut", "Math", "math",
            "null", "NULL", "None",
            "println", "Println", "Printf", "print", "pow", "printf", "package", "parseInt",
            "return",
            "stdlib", "stdio", "struct", "String", "static", "strconv", "self",
            "true",
            "until", "uint", "unwrap", "use",
            "void", "var",
            "while", "write", "Write"};
    final static HashSet<String> LANGUAGE_WORDS_MAP = new HashSet<>();
    static {
        for(String k : LANGUAGE_WORDS) {
            LANGUAGE_WORDS_MAP.add(k);
        }
    }

    public static void main(String... args) throws IOException {
        String[] languages = { "Python:py", "Bau:bau", "Swift:swift", "Kotlin:kt", "C:c", "Go:go", "Java:java", "Rust:rs" };
        String[] files = { "BinaryTrees", "Fannkuch", "Mandelbrot", "Munchausen", "PiDigits" };

        LinkedHashMap<String, Stats> stats = new LinkedHashMap<>();
        for (String file : files) {
            String origFile = file;
            for (String l : languages) {
                file = origFile;
                String[] l2 = l.split(":");
                Stats s = stats.get(l);
                if (s == null) {
                    s = new Stats();
                    s.language = l2[0];
                    s.suffix = l2[1];
                    stats.put(l, s);
                }
                String dir = "src/test/resources/org/bau/benchmarks/";
                if (s.language.equals("Java") || s.language.equals("Kotlin")) {
                    dir = "src/test/java/org/bau/benchmarks/";
                } else {
                    file = Character.toLowerCase(file.charAt(0)) + file.substring(1);
                }
                if (file.equals("piDigits")) {
                    if (s.language.equals("Rust")) {
                        file = "rust/pi_digits/src/main";
                    } else if (s.language.equals("Swift")) {
                        file = "swift/piDigits/Sources/main";
                    }
                }
                File f = new File(dir + file + "." + s.suffix);
                if (!f.exists()) {
                    throw new AssertionError("File not found: " + f);
                }
                byte[] fileData = new byte[(int) f.length()];
                RandomAccessFile rf = new RandomAccessFile(f, "r");
                rf.readFully(fileData);
                rf.close();
                int lineCount = 1;
                int whitespace = 0;
                int underscore = 0;
                int underscoreInIdentifier = 0;
                s.rawBytes += fileData.length;
                int alphaNumeric = 0;
                int special = 0;
                boolean identifierStart = false;
                boolean identifier = false;
                String[] lines = new String(fileData).split("\n");
                for(String line : lines) {
                    String trimmed = line.trim();
                    if ("runtime.GOMAXPROCS(1)".equals(trimmed)) {
                        continue;
                    }
                    line += "\n";
                    byte[] lineData = line.getBytes(StandardCharsets.UTF_8);
                    for (int i = 0; i < lineData.length; i++) {
                        byte c = lineData[i];
                        switch (c) {
                        case '\n':
                            identifier = identifierStart = false;
                            if (i > 0 && lineData[i - 1] == ' ') {
                                System.out.println("Trailing whitespace: line " + lineCount + " of " + f.getName());
                            }
                            lineCount++;
                            // continue
                        case ' ':
                            identifier = identifierStart = false;
                            whitespace++;
                            break;
                        case '_':
                            underscore++;
                            if (i > 0 && i < lineData.length - 1) {
                                char last = (char) lineData[i - 1];
                                char next = (char) lineData[i + 1];
                                if (Character.isAlphabetic(last) && Character.isAlphabetic(next)) {
                                    underscoreInIdentifier++;
                                }
                            }
                            if (!identifier) {
                                identifierStart = true;
                            }
                            break;
                        default:
                            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                                if (!identifier) {
                                    identifierStart = true;
                                }
                                alphaNumeric++;
                            } else if ("+\\\"'-(){}[]|&.,#*/%=<>!?!:;,.$@".indexOf(c) >= 0) {
                                identifier = identifierStart = false;
                                special++;
                            } else {
                                throw new AssertionError("Unexpected char " + (char) c);
                            }
                        }
                        if (identifierStart && !identifier) {
                            addIdentifier(s, lineData, i);
                            identifier = true;
                        }
                    }
                }
                s.lines += lineCount;
                s.whitespace += whitespace;
                s.underscoreInIdentifier += underscoreInIdentifier;
                s.alphaNumeric += alphaNumeric;
                s.special += special;
                s.special += underscore - underscoreInIdentifier;
                System.out.printf("%-8s %-20s %s\n", s.language, origFile, s.identifiers);
                s.identifiers.clear();
            }
        }
        System.out.println("Language Lines Bytes Whitespace Alphanumeric Special Underscore");
        for (Stats s : stats.values()) {
            System.out.printf("%-8s %5d %5d %10d %12d %7d %10d\n", s.language, s.lines, s.rawBytes, s.whitespace,
                    s.alphaNumeric, s.special, s.underscoreInIdentifier);
        }
    }

    private static void addIdentifier(Stats s, byte[] data, int start) {
        int pos = start;
        StringBuilder buff = new StringBuilder();
        boolean wasUpper = true;
        boolean toUpper = false;
        while (pos < data.length) {
            char c = (char) data[pos];
            if (c >= 'a' && c <= 'z') {
                wasUpper = false;
                if (toUpper) {
                    buff.append((char) (c + 'A' - 'a'));
                    toUpper = false;
                } else {
                    buff.append(c);
                }
            } else if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                buff.append(c);
                wasUpper = true;
            } else if (c == '_') {
                if (wasUpper) {
                    buff.append(c);
                }
                wasUpper = false;
                toUpper = true;
            } else {
                break;
            }
            pos++;
        }
        String id = buff.toString();
        if (!LANGUAGE_WORDS_MAP.contains(id)) {
            s.identifiers.add(id);
        }
    }

    static class Stats {
        TreeSet<String> identifiers = new TreeSet<>();
        String language;
        String suffix;
        int lines;
        int rawBytes;
        int whitespace;
        int underscoreInIdentifier;

        int alphaNumeric;
        int special;
    }
}
