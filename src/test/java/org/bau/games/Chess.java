package org.bau.games;

public class Chess {

    final static int KING = 1, QUEEN = 2, ROOK = 3, BISHOP = 4, KNIGHT = 5, PAWN = 6, BLACK = 6;

    int[] board = new int[64];
    int castlingFlags;
    int pawnMoved2;
    int turn;

    void init() {
        board[0] = board[7] = ROOK;
        board[1] = board[6] = KNIGHT;
        board[2] = board[5] = BISHOP;
        board[3] = QUEEN;
        board[4] = KING;
        for (int i = 0; i < 8; i++) {
            board[i + 56] = board[i];
            board[i] += BLACK;
            board[i + 8] = PAWN + BLACK;
            board[i + 48] = PAWN;
        }
    }

    long negamax(boolean top, int depth, boolean black, long alpha, long beta) {
        long best = -Long.MAX_VALUE;
        long bestMove = 0;
        if (depth <= 0) {
            best = evaluateBoard(black);
            if (best >= beta || depth < -1) {
                return best;
            }
        }
        for (int phase = 0; phase <= 1; phase++) {
            if (depth <= 0 && phase == 1) {
                return best;
            }
            for (int i = 0; i < 64; i++) {
                if (board[i] == 0 || isBlack(board[i]) != black) {
                    continue;
                }
                long moves = getPossibleMoves(i, phase == 0);
                while (moves != 0) {
                    int target = Long.numberOfTrailingZeros(moves);
                    moves ^= 1L << target;
                    boolean capture = board[target] != 0;
                    if (capture != (phase == 0)) {
                        continue;
                    }
                    long move = move(i, target);
                    long score = -negamax(false, depth - 1, !black, -beta, -alpha);
                    if (score > best) {
                        bestMove = move;
                        best = score;
                        alpha = Math.max(alpha, score);
                    }
                    undo(move);
                    if (!top && best >= beta) {
                        break;
                    }
                }
            }
        }
        if (top) {
            return bestMove;
        }
        return best;
    }

    boolean isFieldAttacked(boolean black, int pos) {
        for (int i = 0; i < 64; i++) {
            int b = board[i];
            if (b == 0 || isBlack(b) == black) {
                continue;
            }
            long moves = getPossibleMoves(i, true);
            if (((moves >>> pos) & 1) == 1) {
                return true;
            }
        }
        return false;
    }

    long evaluateBoard(boolean black) {
        int sum = 0;
        for (int i = 0; i < 64; i++) {
            int b = board[i];
            int sc = 0;
            switch(getPiece(b)) {
            case KING:
                sc = 10000000;
                break;
            case QUEEN:
                sc = 1000;
                break;
            case ROOK:
                sc = 500;
                break;
            case KNIGHT:
                sc = Long.bitCount(getPossibleMoves(i, false));
                sc += 320;
                break;
            case BISHOP:
                sc = Long.bitCount(getPossibleMoves(i, false));
                sc += 330;
                break;
            case PAWN:
                sc = 100;
                if (turn > 40) {
                    sc += 1 << (isBlack(b) ? (i / 8) - 1 : 6 - (i / 8));
                }
            }
            if (isBlack(b) != black) {
                sc = -sc;
            }
            sum += sc;
        }
        return sum;
    }

    long getPossibleMoves(int from, boolean attacksOnly) {
        int b = board[from];
        int p = getPiece(b);
        boolean black = isBlack(b);
        int maxDist = 1;
        long result = 0;
        if (p == QUEEN || p == ROOK || p == BISHOP) {
            maxDist = 7;
        }
        if (p == KING || p == QUEEN || p == BISHOP) {
            result |= slide(from, maxDist, 1, 1);
            result |= slide(from, maxDist, -1, -1);
            result |= slide(from, maxDist, 1, -1);
            result |= slide(from, maxDist, -1, 1);
        }
        if (p == KING || p == QUEEN || p == ROOK) {
            result |= slide(from, maxDist, 1, 0);
            result |= slide(from, maxDist, -1, 0);
            result |= slide(from, maxDist, 0, 1);
            result |= slide(from, maxDist, 0, -1);
        }
        if (p == KNIGHT) {
            result |= slide(from, maxDist, 1, 2);
            result |= slide(from, maxDist, 2, 1);
            result |= slide(from, maxDist, -1, -2);
            result |= slide(from, maxDist, -2, -1);
            result |= slide(from, maxDist, 1, -2);
            result |= slide(from, maxDist, 2, -1);
            result |= slide(from, maxDist, -1, 2);
            result |= slide(from, maxDist, -2, 1);
        } else if (p == PAWN) {
            int dir = isBlack(b) ? 1 : -1;
            // straight
            int dist = 1;
            if (!attacksOnly) {
                if (dir == 1 && from / 8 == 1 && board[from + 16] == 0) {
                    dist = 2;
                }
                if (dir == -1 && from / 8 == 6 && board[from - 16] == 0) {
                    dist = 2;
                }
                result |= slide(from, dist, 0, dir);
                if (result != 0 && board[from + dir * 8] != 0) {
                    result = 0;
                }
            }
            if (pawnMoved2 / 8 == from / 8) {
                // en passant
                if (pawnMoved2 == from - 1) {
                    result |= 1L << (from - 1);
                } else if (pawnMoved2 == from + 1) {
                    result |= 1L << (from + 1);
                }
            }
            long capture = slide(from, maxDist, 1, dir);
            if (capture != 0 && board[from + 1 + dir * 8] != 0) {
                result |= capture;
            }
            capture = slide(from, maxDist, -1, dir);
            if (capture != 0 && board[from - 1 + dir * 8] != 0) {
                result |= capture;
            }
        } else if (p == KING && !attacksOnly) {
            // castling
            int r = castlingFlags >>> (black ? 0 : 2);
            if ((r & 3) != 3 && !isFieldAttacked(black, from)) {
                if ((r & 1) == 0 && !isFieldAttacked(black, from - 1)) {
                    long rook = slide(from - 4, 8, 1, 0);
                    rook >>>= from - 3;
                    if ((rook & 7) == 7) {
                        result |= 1L << (from - 2);
                    }
                }
                if ((r & 2) == 0 && !isFieldAttacked(black, from + 1)) {
                    long rook = slide(from + 3, 8, -1, 0);
                    rook >>>= from + 1;
                    if ((rook & 3) == 3) {
                        result |= 1L << (from + 2);
                    }
                }
            }
        }
        return result;
    }

    long slide(int from, int maxDist, int xo, int yo) {
        int x = from & 7;
        int y = from / 8;
        boolean isBlack = isBlack(board[from]);
        long result = 0;
        for (int i = 1;; i++) {
            x += xo;
            y += yo;
            if (i > maxDist || x < 0 || x > 7 || y < 0 || y > 7) {
                break;
            }
            int p = x + y * 8;
            int b = board[p];
            if (b != 0) {
                if (isBlack(b) != isBlack) {
                    result |= 1L << p;
                }
                break;
            }
            result |= 1L << p;
        }
        return result;
    }

    void updateCastlingRights(int pos) {
        int p = board[pos];
        if (getPiece(p) == ROOK) {
            if ((pos & 7) == 0 || (pos & 7) == 7) {
                int which = (pos & 7) == 0 ? 1 : 2;
                castlingFlags |= which << (isBlack(p) ? 0 : 2);
            }
        }
    }

    long move(long move) {
        int source = (int) ((move >>> 16) & 0xff);
        int target = (int) ((move >>> 8) & 0xff);
        return move(source, target);
    }

    long move(int source, int target) {
        turn++;
        long captured = board[target];
        long oldCastlingFlags = castlingFlags;
        updateCastlingRights(source);
        updateCastlingRights(target);
        long piece = board[source];
        board[target] = board[source];
        int p = getPiece((int) piece);
        boolean isBlack = isBlack(board[source]);
        board[source] = 0;
        long oldPawnMoved = pawnMoved2;
        pawnMoved2 = 0;
        if (p == PAWN) {
            int shift = (target & 7) - (source & 7);
            if (shift != 0 && captured == 0) {
                // en passant capture
                board[source + shift] = 0;
            }
            if (target <= 7 || target >= 56) {
                // promotion
                board[target] = QUEEN + (isBlack ? BLACK : 0);
            }
            if (Math.abs(source - target) == 16) {
                pawnMoved2 = target;
            }
        } else if (p == KING) {
            castlingFlags |= 3 << (isBlack ? 0 : 2);
            if (Math.abs((source & 7) - (target & 7)) > 1) {
                // castling
                if (target > source) {
                    board[target - 1] = board[target + 1];
                    board[target + 1] = 0;
                } else {
                    board[target + 1] = board[target - 2];
                    board[target - 2] = 0;
                }
            }
        }
        return (piece << 40) | (oldPawnMoved << 32) | (captured << 24) | (source << 16) | (target << 8) | oldCastlingFlags;
    }

    void undo(long move) {
        turn--;
        int piece = (int) ((move >>> 40) & 0xff);
        pawnMoved2 = (int) ((move >>> 32) & 0xff);
        int captured = (int) ((move >>> 24) & 0xff);
        int source = (int) ((move >>> 16) & 0xff);
        int target = (int) ((move >>> 8) & 0xff);
        castlingFlags = (int) (move & 0xff);
        board[target] = captured;
        board[source] = piece;
        if (getPiece(piece) == KING) {
            if (Math.abs((source & 7) - (target & 7)) > 1) {
                // undo castling
                if (target > source) {
                    board[target + 1] = board[target - 1];
                    board[target - 1] = 0;
                } else {
                    board[target - 2] = board[target + 1];
                    board[target + 1] = 0;
                }
            }
        } else if (getPiece(piece) == PAWN) {
            int shift = (target & 7) - (source & 7);
            if (shift != 0 && captured == 0) {
                // en passant capture
                board[source + shift] = PAWN + (isBlack(piece) ? 0 : BLACK);
            }
        }
    }

    static boolean isBlack(int p) {
        return p > 6;
    }

    static int getPiece(int p) {
        return p > 6 ? p - 6 : p;
    }

}
