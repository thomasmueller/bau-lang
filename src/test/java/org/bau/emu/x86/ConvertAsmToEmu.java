package org.bau.emu.x86;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ConvertAsmToEmu {

    public static void main(String... args) throws IOException {
        RandomAccessFile r = new RandomAccessFile("test.asm", "r");
        byte[] data = new byte[(int) r.length()];
        r.readFully(data);
        r.close();
        String s = new String(data, StandardCharsets.UTF_8);
        String[] lines = s.split("\n");
        HashMap<String, Integer> labelLineMap = new HashMap<>();
        HashMap<String, String> constantMap = new HashMap<>();

        int nextLine = 0;
        for (String l : lines) {
            l = l.split(";")[0].trim();
            if (l.isEmpty()) {
                continue;
            }
            int end = l.indexOf(":");
            String label = "";
            String command;
            if (end >= 0) {
                label = l.substring(0, end).trim();
                command = l.substring(end + 1).trim();
            } else {
                command = l.trim();
            }
            if (!label.isEmpty()) {
                labelLineMap.put(s, nextLine);
            }
            if (!command.isEmpty()) {
                if (command.startsWith("equ ")) {
                    if (label.isEmpty() || constantMap.containsKey(label)) {
                        throw new IllegalArgumentException();
                    }
                    constantMap.put(label, command.substring(4));
                    continue;
                }
                System.out.println("case " + nextLine + ":");
                nextLine++;
                if (command.startsWith("db ")
                        || command.startsWith("org") || command.startsWith("cpu ")) {
                    System.out.println("    // " + command);
                } else if (command.startsWith("%")) {
                    throw new IllegalArgumentException("unsupported command " + command);
                } else if (command.equals("cld")) {
                    System.out.println("    " + command + "();");
                } else if (command.startsWith("mov ")) {
                    String ar = command.substring("mov ".length());
                    String[] a = ar.split(",");
                    if (a[0].equals("di") || a[0].equals("ax") || a[0].equals("cx")|| a[0].equals("si") || a[0].equals("bx") || a[0].equals("sp")) {
                        System.out.println("    " + a[0] + " = mov16(" + a[0] + ", " + toString(a[1]) + ");");
                    } else if (a[0].equals("al")) {
                        System.out.println("    ax = movLow(ax, " + toString(a[1]) + ");");
                    } else if (a[0].equals("ah")) {
                        System.out.println("    ax = movHigh(ax, " + toString(a[1]) + ");");
                    } else if (a[0].equals("byte [di]")) {
                        System.out.println("    movByte(di, " + toString(a[1]) + ");");
                    } else if (a[0].equals("[stack-4]")) {
                        System.out.println("    movWord(stack - 4, " + toString(a[1]) + ");");
                    } else {
                        throw new IllegalArgumentException("unsupported command " + command);
                    }
                } else if (command.startsWith("inc ")) {
                    String ar = command.substring("mov ".length());
                    if (ar.equals("di")) {
                        System.out.println("    " + ar +" = inc(" + ar + ");");
                    }
                } else {
                    System.out.println("    // command " + command);
                }
                System.out.println("    break;");
            }
        }
    }

    private static String toString(String string) {
        return "<" + string + ">";
    }
}
