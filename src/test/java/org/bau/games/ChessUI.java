package org.bau.games;

import java.io.IOException;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class ChessUI {

    public static void main(String... args) throws IOException {
        System.out.println("Chess");
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        terminal.enterRawMode();
        // clear screen
        System.out.print("\033[H\033[2J");
        Chess c = new Chess();
        c.init();
        int x = 3, y = 6, state = 0, from = 0;
        redraw(c, terminal, x, y);
        boolean black = false;
        long lastBlack = 0, lastWhite = 0;
        while (true) {
            int ch = terminal.reader().read();
            if (ch == 'q') {
                break;
            } else if (ch == ' ') {
                if (state == 0) {
                    int p = x + 8 * y;
                    if (c.board[p] == 0 || Chess.isBlack(c.board[p]) != black) {
                        // ignore
                    } else {
                        from = p;
                        state = 1;
                        long moves = c.getPossibleMoves(p, false);
                        while (moves != 0) {
                            int target = Long.numberOfTrailingZeros(moves);
                            c.board[target] |= 16;
                            moves ^= 1L << target;
                        }
                        redraw(c, terminal, x, y);
                    }
                } else if (state == 1) {
                    int p = x + 8 * y;
                    boolean allowed = (c.board[p] & 16) != 0;
                    for (int i = 0; i < 64; i++) {
                        c.board[i] &= ~16;
                    }
                    if (allowed) {
                        lastWhite = c.move(from, p);
                        redraw(c, terminal, x, y);
                        System.out.print("\033[?25l");
                        long move = c.negamax(true, 5, !black, -Long.MAX_VALUE, Long.MAX_VALUE);
                        if (move != 0) {
                            lastBlack = move;
                            c.move(move);
                        }
                    }
                    redraw(c, terminal, x, y);
                    System.out.print("\033[?25h");
                    state = 0;
                }
            } else if (ch == 'c') {
                int p = x + 8 * y;
                if (state == 0) {
                    from = p;
                    state = 1;
                } else {
                    lastWhite = c.move(from, p);
                    redraw(c, terminal, x, y);
                    state = 0;
                }
            } else if (ch == 'u') {
                if (lastBlack != 0 && lastWhite != 0) {
                    c.undo(lastBlack);
                    c.undo(lastWhite);
                    redraw(c, terminal, x, y);
                }
                lastBlack = 0;
                lastWhite = 0;
            } else if (ch == 's') {
                black = !black;
                System.out.print("\033[?25l");
                long move = c.negamax(true, 5, !black, -Long.MAX_VALUE, Long.MAX_VALUE);
                if (move != 0) {
                    c.move(move);
                }
                redraw(c, terminal, x, y);
                System.out.print("\033[?25h");
                state = 0;
            } else if (ch == 27) {
                int next1 = terminal.reader().read();
                int next2 = terminal.reader().read();
                if (next1 == '[') {
                    switch (next2) {
                    case 'A':
                        y = Math.max(0, y - 1);
                        break;
                    case 'B':
                        y = Math.min(7, y + 1);
                        break;
                    case 'C':
                        x = Math.min(7, x + 1);
                        break;
                    case 'D':
                        x = Math.max(0, x - 1);
                        break;
                    }
                }
                redraw(c, terminal, x, y);
            }
        }
        terminal.close();
    }

    static void redraw(Chess chess, Terminal terminal, int xx, int yy) {
        // disable cursor
        System.out.print("\033[?25l");
        // home
        System.out.print("\033[H");
        System.out.println("   a  b  c  d  e  f  g  h");
        for (int y = 0; y < 8; y++) {
            System.out.print((8 - y) + " ");
            for (int x = 0; x < 8; x++) {
                int b = chess.board[x + 8 * y];
                boolean allowed = (b & 16) != 0;
                b &= ~16;
                char ch = ' ';
                if (b != 0) {
                    ch = (char) (0x2654 + b - 1);
                }
                if (allowed) {
                    System.out.print("\033[30;100m");
                } else if ((y + x) % 2 == 1) {
                    System.out.print("\033[30;107m");
                }
                if (Chess.isBlack(b)) {
                    // red
                    System.out.print("\033[31m");
                } else {
                    // blue
                    System.out.print("\033[94m");
                }
                // reset all attributes
                System.out.print(" " + ch + " \033[0m");
            }
            System.out.println();
        }
        System.out.println("  arrows:select space:move");
        System.out.println("  u:undo  s:switch  q:quit");
        System.out.print("\033[" + (yy + 2) + ";" + (1 + 3 * xx + 3) + "H");
        // show cursor
        System.out.print("\033[?25h");
        System.out.flush();
    }

}
