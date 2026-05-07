package org.bau.nanobasic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

public class BootBasic {

    // Variables a-z
    private int[] vars = new int[26];

    // Program storage: lineNumber -> code
    private TreeMap<Integer, String> program = new TreeMap<>();

    private Scanner scanner = new Scanner(System.in);
    private Random random = new Random();

    public static void main(String[] args) {
        new BootBasic().run();
    }

    private void run() {
        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) continue;

            // Try to parse line number
            String[] parts = line.split(" ", 2);
            if (isNumber(parts[0])) {
                int lineNum = Integer.parseInt(parts[0]);

                if (parts.length == 1) {
                    program.remove(lineNum); // delete line
                } else {
                    program.put(lineNum, parts[1]);
                }
            } else {
                execute(line); // immediate mode
            }
        }
    }

    private void execute(String code) {
        String[] tokens = code.trim().split(" ", 2);
        String cmd = tokens[0];

        switch (cmd) {
            case "print":
                handlePrint(tokens.length > 1 ? tokens[1] : "");
                break;

            case "input":
                handleInput(tokens[1]);
                break;

            case "run":
                runProgram();
                break;

            case "list":
                listProgram();
                break;

            case "new":
                program.clear();
                break;

            case "goto":
                // only valid in program execution
                break;

            case "if":
                handleIf(tokens[1]);
                break;

            case "system":
                System.exit(0);
                break;

            default:
                if (code.contains("=")) {
                    handleAssignment(code);
                } else {
                    error();
                }
        }
    }

    private void runProgram() {
        List<Integer> lines = new ArrayList<>(program.keySet());
        int index = 0;

        while (index < lines.size()) {
            int lineNum = lines.get(index);
            String code = program.get(lineNum);

            Integer jump = executeLine(code);
            if (jump != null) {
                index = lines.indexOf(jump);
                if (index == -1) break;
            } else {
                index++;
            }
        }
    }

    private Integer executeLine(String code) {
        String[] tokens = code.trim().split(" ", 2);
        String cmd = tokens[0];

        switch (cmd) {
            case "print":
                handlePrint(tokens.length > 1 ? tokens[1] : "");
                return null;

            case "input":
                handleInput(tokens[1]);
                return null;

            case "goto":
                return eval(tokens[1]);

            case "if":
                String[] parts = tokens[1].split("goto");
                int cond = eval(parts[0].trim());
                if (cond != 0) {
                    return eval(parts[1].trim());
                }
                return null;

            default:
                if (code.contains("=")) {
                    handleAssignment(code);
                    return null;
                }
        }
        return null;
    }

    private void handlePrint(String expr) {
        expr = expr.trim();

        if (expr.startsWith("\"")) {
            String text = expr.substring(1, expr.lastIndexOf("\""));
            if (expr.endsWith(";")) {
                System.out.print(text);
            } else {
                System.out.println(text);
            }
        } else {
            int value = eval(expr.replace(";", ""));
            if (expr.endsWith(";")) {
                System.out.print(value);
            } else {
                System.out.println(value);
            }
        }
    }

    private void handleInput(String var) {
        var = var.trim();
        System.out.print("? ");
        int value = Integer.parseInt(scanner.nextLine());
        vars[var.charAt(0) - 'a'] = value;
    }

    private void handleAssignment(String code) {
        String[] parts = code.split("=");
        char var = parts[0].trim().charAt(0);
        int value = eval(parts[1].trim());
        vars[var - 'a'] = value;
    }

    private void handleIf(String code) {
        // only meaningful inside program
    }

    private void listProgram() {
        for (Map.Entry<Integer, String> entry : program.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }

    // ---------------- Expression Evaluator ----------------

    private int eval(String expr) {
        return parseAddSub(new Tokenizer(expr));
    }

    private int parseAddSub(Tokenizer t) {
        int value = parseMulDiv(t);
        while (true) {
            if (t.peek('+')) {
                t.next();
                value += parseMulDiv(t);
            } else if (t.peek('-')) {
                t.next();
                value -= parseMulDiv(t);
            } else {
                break;
            }
        }
        return value;
    }

    private int parseMulDiv(Tokenizer t) {
        int value = parseFactor(t);
        while (true) {
            if (t.peek('*')) {
                t.next();
                value *= parseFactor(t);
            } else if (t.peek('/')) {
                t.next();
                value /= parseFactor(t);
            } else {
                break;
            }
        }
        return value;
    }

    private int parseFactor(Tokenizer t) {
        t.skipSpaces();

        if (t.peek('(')) {
            t.next();
            int val = parseAddSub(t);
            t.next(); // ')'
            return val;
        }

        if (Character.isLetter(t.peekChar())) {
            char var = t.next();
            return vars[var - 'a'];
        }

        if (t.startsWith("rnd")) {
            t.consume("rnd");
            return random.nextInt(256);
        }

        return t.readNumber();
    }

    // ---------------- Utilities ----------------

    private boolean isNumber(String s) {
        return s.matches("\\d+");
    }

    private void error() {
        System.out.println("@#!");
    }

    // Simple tokenizer
    static class Tokenizer {
        String s;
        int pos = 0;

        Tokenizer(String s) {
            this.s = s;
        }

        void skipSpaces() {
            while (pos < s.length() && s.charAt(pos) == ' ') pos++;
        }

        boolean peek(char c) {
            skipSpaces();
            return pos < s.length() && s.charAt(pos) == c;
        }

        char peekChar() {
            skipSpaces();
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        char next() {
            return s.charAt(pos++);
        }

        boolean startsWith(String str) {
            skipSpaces();
            return s.startsWith(str, pos);
        }

        void consume(String str) {
            pos += str.length();
        }

        int readNumber() {
            skipSpaces();
            int start = pos;
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            return Integer.parseInt(s.substring(start, pos));
        }
    }
}