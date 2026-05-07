package org.bau.nanobasic;

import java.util.Scanner;

public class NanoBasic {

    Scanner scanner = new Scanner(System.in);
    String line = null;

    private byte getchar() {
        if (line == null || line.isEmpty()) {
            line = scanner.hasNextLine() ? scanner.nextLine() : "exit";
            line += "\n";
        }
        long x = line.charAt(0);
        line = line.substring(1);
        return (byte) x;
    }

    private void putchar(long c) {
        System.out.print((char) (c & 0xff));
    }

    public static void main(String... args) {
        new NanoBasic().repl();
    }

    byte[] memory = new byte[32 * 1024];

    private static final int LINES = 256;
    private static final int LINE_LEN = 32;

    public void repl() {
        while (true) {
            short i = 0;
            byte mask = (byte) 0xff;
            byte c = 0;
            while(i < LINE_LEN) {
                if (mask != 0) {
                    c = getchar();
                }
                memory[i++] = (byte) (c & mask);
                if (c == '\n') {
                    mask = 0;
                }
            }
            short line = 0;
            i = 0;
            while (true) {
                c = memory[i++];
                if (c < '0' || c > '9') {
                    break;
                }
                line = (short) (line * 10 + c - '0');
            }
            if (line > 0 && line <= LINES) {
                short p = (short) (line * LINE_LEN);
                for (i = 0; i < LINE_LEN; i++) {
                    memory[p++] = memory[i];
                }
            } else {
                if (memory[0] == 'r') {
                    run();
                } else if (memory[0] == 'l') {
                    for (i = LINE_LEN; i < LINE_LEN * LINES; i++) {
                        c = memory[i];
                        if (c != 0) {
                            putchar(c);
                        }
                    }
                } else if (memory[0] == 'n') {
                    for (i = 0; i < LINES * LINE_LEN; i++) {
                        memory[i] = 0;
                    }
                } else if (memory[0] == 'e') {
                    return;
                }
            }
        }
    }

    void run() {
        System.out.println("run...");
    }


//        int pc = 1;
//        while (true) {
//            int p = pc * 64;
//            if (memory[p] == 0) {
//                pc++;
//            } else {
//                long c;
//                while (true) {
//                    c = memory[p];
//                    if (c <= '0' || c >= '9') {
//                        break;
//                    }
//                }
//                if (c == '[') {
//
//                }
//            }
//        }
//    }

}
