package org.bau.games;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ChessUCI {

    Chess chess = new Chess();

    public static void main(String... args) throws IOException {
        new ChessUCI().loop();
    }

    private void loop() throws IOException {
        BufferedInputStream in = new BufferedInputStream(System.in);
        chess.init();
        while (true) {
            byte[] data = new byte[4 * 1024];
            int i = 0;
            for (;; i++) {
                int x = in.read();
                if (x == -1) {
                    return;
                }
                data[i] = (byte) x;
                if (x == '\n') {
                    break;
                }
            }
            String line = new String(data, 0, i, StandardCharsets.UTF_8);
            line = line.trim();
            //System.err.println(line);
            if (line.equals("ucinewgame")) {
                chess = new Chess();
                chess.init();
            } else if (line.equals("uci")) {
                System.out.println("id name BauChess");
                System.out.println("id author ThomasMuellerGraf");
                System.out.println("uciok");
            } else if (line.equals("isready")) {
                System.out.println("readyok");
            } else if (line.equals("stop")) {
                // not searching: ignore
            } else if (line.startsWith("setoption ")) {
                // ignore
            } else if (line.startsWith("debug")) {
                // ignore
            } else if (line.startsWith("ponderhit")) {
                // ignore
            } else if (line.equals("quit")) {
                break;
            } else if (line.equals("position startpos")) {
                chess = new Chess();
                chess.init();
                chess.blackTurn = false;
            } else if (line.startsWith("position startpos moves ")) {
                chess = new Chess();
                chess.init();
                chess.blackTurn = false;
                String moves = line.substring("position startpos moves ".length());
                String[] movesArray = moves.split(" ");
                for (String m : movesArray) {
                    String from = m.substring(0, 2);
                    String to = m.substring(2, 4);
                    char promotion = 'q';
                    if (m.length() == 5) {
                        promotion = m.charAt(4);
                    }
                    chess.move(pos(from), pos(to), piece(promotion));
                    chess.blackTurn = !chess.blackTurn;
                }
            } else if (line.startsWith("position fen ")) {
                // not supported
                chess = new Chess();
                String fen = line.substring("position fen ".length());
                // e.g.
                // position fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 moves e2e4
            } else if (line.startsWith("print")) {
                for (int j = 0; j < 64; j++) {
                    if ((j & 7) == 0) {
                        System.out.println();
                    }
                    int p = chess.board[j];
                    int piece = Chess.getPiece(p);
                    String n = "" + (char) " kqrbnp".charAt(piece);
                    if (!Chess.isBlack(p)) {
                        n = n.toUpperCase(Locale.ROOT);
                    }
                    if (p == 0) {
                        n = ".";
                    }
                    System.out.print(n + " ");
                }
                System.out.println();
            } else if (line.startsWith("go movetime ")) {
                int timeMillis = Integer.parseInt(line.substring("go movetime ".length()));
                move(timeMillis);
            } else if (line.equals("go")) {
                move(100);
            }
            System.out.flush();
        }
    }

    void move(int timeMillis) {
        boolean black = chess.blackTurn;
        chess.blackTurn = !chess.blackTurn;
        long move = chess.negamax(true, 4, black, -Long.MAX_VALUE, Long.MAX_VALUE);
        // long move = chess.negamax(true, 5, black, -Long.MAX_VALUE, Long.MAX_VALUE);
        int source = (int) ((move >>> 16) & 0xff);
        int pieceMoved = chess.board[source];
        int target = (int) ((move >>> 8) & 0xff);
        String moveString = field(source) + field(target);
        if (Chess.getPiece(pieceMoved) == Chess.PAWN) {
            if ((black && target / 8 == 7)
                    || (!black && target / 8 == 0)) {
                moveString += "q";
            }
        }
        System.out.println("bestmove " + moveString);
        chess.move(move);
    }

    static String field(int pos) {
        return (char) ('a' + (pos & 7)) + "" + (char) ('1' + (7 - (pos / 8)));
    }

    static int pos(String field) {
        field = field.toLowerCase(Locale.ROOT);
        return (field.charAt(0) - 'a') + 8 * (7 - (field.charAt(1) - '1'));
    }

    static int piece(char name) {
        name = Character.toLowerCase(name);
        return " kqrbnp".indexOf(name);
    }

}
