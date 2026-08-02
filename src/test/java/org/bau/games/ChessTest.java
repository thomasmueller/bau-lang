package org.bau.games;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
                Thread.sleep(3000);
                start = System.nanoTime();
                move = c.negamax(true, 4, false, -Long.MAX_VALUE, Long.MAX_VALUE);
                whiteTime += System.nanoTime() - start;

                if (c.turn % 10 == 9) {
                    // System.out.println(prof.getTop(10));
                    prof = new Profiler();
                    prof.startCollecting();
                }
                c.move(move);
                print(c);
                System.out.println("time: white=" + (whiteTime / 1000000) + " black=" + (blackTime / 1000000));
                Thread.sleep(3000);
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
    public void noNullMove() {
        Chess c = new Chess();
        initBoard(c, ".......k ......pp ..p..... P....... P.....n. ...rqb.. ..P.N... ..R.K... ");
        long move = c.negamax(true, 3, true, -Long.MAX_VALUE, Long.MAX_VALUE);
        assertTrue(move != 0);
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
        assertEquals(1149, c.evaluateBoard(false));
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
        assertEquals(" A:-------- B:P------- C:-------- D:-------- E:-------- F:-------- G:----k--- H:-------- castling:12 pawnMoved2:0",
                toString(c));
        c.undo(move);
        move = c.negamax(true, 3, false, Long.MIN_VALUE, Long.MAX_VALUE);
        c.move(move);
        assertEquals(" A:-------- B:P------- C:-------- D:-------- E:-------- F:-------- G:----k--- H:-------- castling:12 pawnMoved2:0",
                toString(c));
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
        // print(c);
        long pawn = c.getPossibleMoves(4 * 8 + 6, false);
        assertEquals("--------/--------/--------/--------/--------/------xx/--------/--------/", ChessTest.toStringPossibleMove(pawn));
    }

    @Test
    public void enPassant2() {
        Chess c = new Chess();
        c.init();
        c.move(1 * 8 + 3, 4 * 8 + 3);
        c.move(6 * 8 + 4, 4 * 8 + 4);
        // print(c);
        long pawn = c.getPossibleMoves(4 * 8 + 3, false);
        assertEquals("--------/--------/--------/--------/--------/---xx---/--------/--------/", ChessTest.toStringPossibleMove(pawn));
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

    @Test
    public void testPerft() {
        // https://joeyrobert.org/chessprogramming-wiki/Perft%20Results.html
        // Position 2 (Kiwipete)
        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
        Chess chess = initBoardFromFen(fen);
        assertEquals(48, countNodes(chess));
        assertEquals(8, countCaptures(chess));
        // Position 3
        fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -";
        chess = initBoardFromFen(fen);
        assertEquals(14, countNodes(chess));
        assertEquals(1, countCaptures(chess));
        // Position 4
        fen = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
        chess = initBoardFromFen(fen);
        assertEquals(6, countNodes(chess));
        assertEquals(0, countCaptures(chess));
        // Position 5
        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        chess = initBoardFromFen(fen);
        // 3 less, because we always promote to queen
        assertEquals(41, countNodes(chess));
    }

    public static int countCaptures(Chess chess) {
        int count = 0;
        for (int i = 0; i < 64; i++) {
            if (chess.board[i] == 0) {
                continue;
            }
            if (Chess.isBlack(chess.board[i]) != chess.blackTurn) {
                continue;
            }
            long moves = chess.getPossibleMoves(i, true);
            // remove those where the king would be in check
            for (int j = 0; j < 64; j++) {
                if (((moves >>> j) & 1) == 0) {
                    continue;
                }
                long move = chess.move(i, j);
                int kingPos = chess.findKing(chess.blackTurn);
                if (chess.isFieldAttacked(chess.blackTurn, kingPos)) {
                    moves &= ~(1L << j);
                }
                chess.undo(move);
            }
            for (int j = 0; j < 64; j++) {
                if (((moves >> j) & 1) == 1) {
                    if (chess.board[j] != 0 && Chess.isBlack(chess.board[j]) != chess.blackTurn) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static int countNodes(Chess chess) {
        int count = 0;
        for (int i = 0; i < 64; i++) {
            if (chess.board[i] == 0) {
                continue;
            }
            if (Chess.isBlack(chess.board[i]) != chess.blackTurn) {
                continue;
            }
            long moves = chess.getPossibleMoves(i, false);
            if (moves == 0) {
                continue;
            }
            // remove those where the king would be in check
            for (int j = 0; j < 64; j++) {
                if (((moves >>> j) & 1) == 0) {
                    continue;
                }
                long move = chess.move(i, j);
                int kingPos = chess.findKing(chess.blackTurn);
                if (chess.isFieldAttacked(chess.blackTurn, kingPos)) {
                    moves &= ~(1L << j);
                }
                chess.undo(move);
            }
            if (moves != 0) {
                // System.out.println("from " + ChessUCI.field(i) + ": " + Long.bitCount(moves) + ": " + ChessTest.toStringPossibleMove(moves));
                count += Long.bitCount(moves);
            }
        }
        return count;
    }

    // set the board from a FEN notation
    // see https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation
    public static Chess initBoardFromFen(String fen) {
        Chess chess = new Chess();
        int i = 0;
        int x = 0, y = 0;
        for (; i < fen.length(); i++) {
            char c = fen.charAt(i);
            if (c == ' ') {
                break;
            } else if (c >= '1' && c <= '8') {
                x += c - '0';
            } else if (c == '/') {
                y++;
                x = 0;
            } else {
                int p = piece(c);
                chess.board[x + 8 * y] = p;
                x++;
            }
        }
        String[] parts = fen.split(" ");
        String turn = parts[1];
        if ("b".equals(turn)) {
            chess.blackTurn = true;
        } else if ("w".equals(turn)) {
            chess.blackTurn = false;
        } else {
            throw new IllegalArgumentException("Invalid turn: " + fen);
        }
        String castling = parts[2];
        chess.castlingFlags = 0xf;
        if (castling.equals("-")) {
            // none
        } else {
            for(char c : castling.toCharArray()) {
                if (c == 'k') {
                    chess.castlingFlags ^= 1 << 1;
                } else if (c == 'K') {
                    chess.castlingFlags ^= 1 << 3;
                } else if (c == 'q') {
                    chess.castlingFlags ^= 1 << 0;
                } else if (c == 'Q') {
                    chess.castlingFlags ^= 1 << 2;
                } else {
                    throw new IllegalArgumentException("Unsupported castling; only KQkq and -: " + fen);
                }
            }
        }
        String enPassant = parts[3];
        if (!enPassant.equals("-")) {
            throw new IllegalArgumentException("Unsupported en passant; only -: " + fen);
        }
        return chess;
    }

    static int piece(char ch) {
        int p = 0;
        if (ch < 'a') {
            ch += 'a' - 'A';
        } else {
            p += Chess.BLACK;
        }
        p += " kqrbnp".indexOf("" + ch);
        return p;
    }

    static void initBoard(Chess c, String b) {
        for (int i = 0, j = 0; i < b.length(); i++) {
            char ch = b.charAt(i);
            if (ch == ' ') {
                continue;
            }
            int p = piece(ch);
            c.board[j++] = p;
        }
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
