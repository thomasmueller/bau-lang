package org.bau.games;

import static org.junit.Assert.assertEquals;

import org.bau.perf.Profiler;
import org.junit.Test;

public class ChessTest {

    public static void main(String... args) throws InterruptedException {
        new ChessTest().play();
    }

    // @Test
    public void play() throws InterruptedException {
        Profiler prof = new Profiler();
        prof.startCollecting();
        long whiteTime = 0, blackTime = 0;
        int whiteScore = 0, blackScore = 0;
        for (int test = 0; test < 100; test++) {
            Chess c = new Chess();
            c.init();
            while (true) {
                long start;
                start = System.nanoTime();
                long move = c.negamax(true, 4, true, Long.MIN_VALUE, Long.MAX_VALUE);
                blackTime += System.nanoTime() - start;
                c.move(move);
                print(c);
                System.out.println("time: white=" + (whiteTime / 1000000) + " black=" + (blackTime / 1000000));
                // Thread.sleep(3000);
                start = System.nanoTime();
                move = c.negamax(true, 4, false, -Long.MAX_VALUE, Long.MAX_VALUE);
                whiteTime += System.nanoTime() - start;

                if (c.turn % 10 == 9) {
                    System.out.println(prof.getTop(10));
                    prof = new Profiler();
                    prof.startCollecting();
                }
                c.move(move);
                print(c);
                System.out.println("time: white=" + (whiteTime / 1000000) + " black=" + (blackTime / 1000000));
                // Thread.sleep(3000);
                boolean whiteOk = false, blackOk = false;
                for (int i = 0; i < 64; i++) {
                    if (c.board[i] == Chess.KING) {
                        whiteOk = true;
                    } else if (c.board[i] == Chess.KING + Chess.BLACK) {
                        blackOk = true;
                    }
                }
                if (whiteOk != blackOk) {
                    if (whiteOk) {
                        whiteScore++;
                    } else {
                        blackScore++;
                    }
                }
                if (!whiteOk || !blackOk) {
                    break;
                }
            }
            System.out.println("white score " + whiteScore + " black " + blackScore);
        }
    }

    @Test
    public void evaluate() {
        Chess c = new Chess();
        c.init();
        assertEquals(0, c.evaluateBoard(false));
        c.board[0] = 0;
        assertEquals(500, c.evaluateBoard(false));
        c.board[1] = 0;
        assertEquals(822, c.evaluateBoard(false));
        c.board[2] = 0;
        assertEquals(1152, c.evaluateBoard(false));
        c.board[3] = 0;
        assertEquals(2152, c.evaluateBoard(false));
        c.board[4] = 0;
        assertEquals(10002152, c.evaluateBoard(false));
        c.board[63] = 0;
        assertEquals(10001652, c.evaluateBoard(false));
    }

    @Test
    public void possibleMoves2() {
        Chess c = new Chess();
        c.board[5 * 8 + 4] = Chess.KING;
        c.board[8] = Chess.PAWN + Chess.BLACK;
        c.board[6 * 8 + 4] = Chess.ROOK + Chess.BLACK;
        long move = c.negamax(true, 2, false, Long.MIN_VALUE, Long.MAX_VALUE);
        c.move(move);
        assertEquals(" A:-------- B:P------- C:-------- D:-------- E:-------- F:-------- G:----k--- H:-------- castling:12 pawnMoved2:0", toString(c));
        c.undo(move);
        move = c.negamax(true, 3, false, Long.MIN_VALUE, Long.MAX_VALUE);
        c.move(move);
        assertEquals(" A:-------- B:P------- C:-------- D:-------- E:-------- F:-------- G:----k--- H:-------- castling:12 pawnMoved2:0", toString(c));
    }

    @Test
    public void castling() {
        Chess c = new Chess();
        c.init();
        c.board[1] = c.board[2] = c.board[3] = 0;
        c.board[5] = c.board[6] = 0;
        long king = c.getPossibleMoves(4, false);
        assertEquals("--xx-xx-/--------/--------/--------/--------/--------/--------/--------/", ChessTest.toStringPossibleMove(king));
    }

    @Test
    public void enPassant() {
        Chess c = new Chess();
        c.init();
        c.move(1 * 8 + 6, 4 * 8 + 6);
        c.move(6 * 8 + 7, 4 * 8 + 7);
        print(c);
        long pawn = c.getPossibleMoves(4 * 8 + 6, false);
        assertEquals("--------/--------/--------/--------/-------x/------x-/--------/--------/", ChessTest.toStringPossibleMove(pawn));
    }

    @Test
    public void possibleMoves() {
        Chess c = new Chess();
        c.init();
        StringBuilder buff = new StringBuilder();
        long bits = c.getPossibleMoves(0, false);
        buff.append(ChessTest.toStringPossibleMove(bits)).append('\n');
        bits = c.getPossibleMoves(8, false);
        buff.append(ChessTest.toStringPossibleMove(bits)).append('\n');
        c.move(8, 9);
        bits = c.getPossibleMoves(0, false);
        buff.append(ChessTest.toStringPossibleMove(bits)).append('\n');
        c.move(0, 28);
        bits = c.getPossibleMoves(28, false);
        buff.append(ChessTest.toStringPossibleMove(bits)).append('\n');
        c.move(1, 36);
        bits = c.getPossibleMoves(36, false);
        buff.append(ChessTest.toStringPossibleMove(bits)).append('\n');
        c.move(2, 37);
        bits = c.getPossibleMoves(37, false);
        buff.append(ChessTest.toStringPossibleMove(bits)).append('\n');
        c.move(3, 38);
        bits = c.getPossibleMoves(38, false);
        buff.append(ChessTest.toStringPossibleMove(bits)).append('\n');
        bits = c.getPossibleMoves(4, false);
        buff.append(ChessTest.toStringPossibleMove(bits)).append('\n');
        c.move(5 + 6 * 8, 5 + 5 * 8);
        bits = c.getPossibleMoves(5 + 5 * 8, false);
        buff.append(ChessTest.toStringPossibleMove(bits)).append('\n');
        assertEquals("--------/--------/--------/--------/--------/--------/--------/--------/\n"
                + "--------/--------/x-------/x-------/--------/--------/--------/--------/\n"
                + "--------/x-------/x-------/x-------/x-------/x-------/x-------/--------/\n"
                + "--------/--------/----x---/xxxx-xxx/----x---/----x---/----x---/--------/\n"
                + "--------/--------/---x-x--/--x---x-/--------/--x---x-/---x-x--/--------/\n"
                + "--------/--------/-------x/------x-/--------/----x-x-/---x---x/--------/\n"
                + "--------/--------/----x-x-/-----xxx/-------x/-----xxx/----x-x-/--------/\n"
                + "---x----/--------/--------/--------/--------/--------/--------/--------/\n"
                + "--------/--------/--------/--------/----x-x-/--------/--------/--------/\n"
                + "", buff.toString());
    }

    @Test
    public void moveAndUndo() {
        Chess c = new Chess();
        c.init();
        StringBuilder buff = new StringBuilder();
        buff.append(toString(c) + "\n");

        // capture and undo
        long move = c.move(0, 6 * 8);
        buff.append(toString(c) + "\n");
        c.undo(move);
        buff.append(toString(c) + "\n");

        // pawn moves 2, and undo
        move = c.move(1 * 8, 3 * 8);
        buff.append(toString(c) + "\n");
        c.undo(move);
        buff.append(toString(c) + "\n");

        // promotion to queen (updates castling rights)
        move = c.move(1 * 8, 7 * 8);
        buff.append(toString(c) + "\n");
        c.undo(move);
        buff.append(toString(c) + "\n");

        // castling black right
        move = c.move(4, 6);
        buff.append(toString(c) + "\n");
        c.undo(move);
        buff.append(toString(c) + "\n");

        // castling black left
        move = c.move(4, 2);
        buff.append(toString(c) + "\n");
        c.undo(move);
        buff.append(toString(c) + "\n");

        // en passant capture and undo
        move = c.move(1 * 8, 4 * 8);
        buff.append(toString(c) + "\n");
        move = c.move(6 * 8 + 1, 4 * 8 + 1);
        buff.append(toString(c) + "\n");
        move = c.move(4 * 8, 5 * 8 + 1);
        buff.append(toString(c) + "\n");
        c.undo(move);
        buff.append(toString(c) + "\n");

        assertEquals(
                  " A:RNBQKBNR B:PPPPPPPP C:-------- D:-------- E:-------- F:-------- G:pppppppp H:rnbqkbnr castling:0 pawnMoved2:0\n"
                  + " A:-NBQKBNR B:PPPPPPPP C:-------- D:-------- E:-------- F:-------- G:Rppppppp H:rnbqkbnr castling:1 pawnMoved2:0\n"
                  + " A:RNBQKBNR B:PPPPPPPP C:-------- D:-------- E:-------- F:-------- G:pppppppp H:rnbqkbnr castling:0 pawnMoved2:0\n"
                  + " A:RNBQKBNR B:-PPPPPPP C:-------- D:P------- E:-------- F:-------- G:pppppppp H:rnbqkbnr castling:0 pawnMoved2:24\n"
                  + " A:RNBQKBNR B:PPPPPPPP C:-------- D:-------- E:-------- F:-------- G:pppppppp H:rnbqkbnr castling:0 pawnMoved2:0\n"
                  + " A:RNBQKBNR B:-PPPPPPP C:-------- D:-------- E:-------- F:-------- G:pppppppp H:Qnbqkbnr castling:4 pawnMoved2:0\n"
                  + " A:RNBQKBNR B:PPPPPPPP C:-------- D:-------- E:-------- F:-------- G:pppppppp H:rnbqkbnr castling:0 pawnMoved2:0\n"
                  + " A:RNBQ-RK- B:PPPPPPPP C:-------- D:-------- E:-------- F:-------- G:pppppppp H:rnbqkbnr castling:3 pawnMoved2:0\n"
                  + " A:RNBQK-NR B:PPPPPPPP C:-------- D:-------- E:-------- F:-------- G:pppppppp H:rnbqkbnr castling:0 pawnMoved2:0\n"
                  + " A:-NKR--NR B:PPPPPPPP C:-------- D:-------- E:-------- F:-------- G:pppppppp H:rnbqkbnr castling:3 pawnMoved2:0\n"
                  + " A:RNB-K-NR B:PPPPPPPP C:-------- D:-------- E:-------- F:-------- G:pppppppp H:rnbqkbnr castling:0 pawnMoved2:0\n"
                  + " A:RNB-K-NR B:-PPPPPPP C:-------- D:-------- E:P------- F:-------- G:pppppppp H:rnbqkbnr castling:0 pawnMoved2:0\n"
                  + " A:RNB-K-NR B:-PPPPPPP C:-------- D:-------- E:Pp------ F:-------- G:p-pppppp H:rnbqkbnr castling:0 pawnMoved2:33\n"
                  + " A:RNB-K-NR B:-PPPPPPP C:-------- D:-------- E:-------- F:-P------ G:p-pppppp H:rnbqkbnr castling:0 pawnMoved2:0\n"
                  + " A:RNB-K-NR B:-PPPPPPP C:-------- D:-------- E:Pp------ F:-------- G:p-pppppp H:rnbqkbnr castling:0 pawnMoved2:33\n"
                  + "", buff.toString());
    }

    public static void printPossibleMove(long bits) {
        System.out.println("legal " + Long.toBinaryString(bits));
        for (int i = 0; i < 64; i++) {
            char c = ((bits >>> i) & 1) == 1 ? 'x' : '-';
            System.out.print(c);
            if (i % 8 == 7) {
                System.out.println();
            }
        }
    }

    public static void print(Chess c) {
        for (int i = 0; i < 64; i++) {
            int b = c.board[i];
            char ch = b == 0 ? '=' : (char) (0x2654 + b - 1);
            System.out.print(ch);
            if (i % 8 == 7) {
                System.out.print((char) ('8' - (i / 8)));
                System.out.println();
            }
        }
        for (int i = 0; i < 8; i++) {
            System.out.print((char) ('a' + i));
        }
        System.out.println();
    }

    public static String toStringPossibleMove(long bits) {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            char c = ((bits >>> i) & 1) == 1 ? 'x' : '-';
            buff.append(c);
            if (i % 8 == 7) {
                buff.append('/');
            }
        }
        return buff.toString();
    }


    public static String toString(Chess c) {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            int p = c.board[i];
            if ((i & 7) == 0) {
                buff.append(" " + (char) ('A' + i / 8) + ":");
            }
            buff.append("-kqrbnpKQRBNP".toCharArray()[p]);
        }
        buff.append(" castling:" + c.castlingFlags);
        buff.append(" pawnMoved2:" + c.pawnMoved2);
        return buff.toString();
    }
}
