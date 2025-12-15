package org.bau.games;

public class Chess {

    final static int KING = 1, QUEEN = 2, ROOK = 3, BISHOP = 4, KNIGHT = 5, PAWN = 6, BLACK = 7;

//    white chess king    ♔   U+2654  &#9812; &#x2654;
//    white chess queen   ♕   U+2655  &#9813; &#x2655;
//    white chess rook    ♖   U+2656  &#9814; &#x2656;
//    white chess bishop  ♗   U+2657  &#9815; &#x2657;
//    white chess knight  ♘   U+2658  &#9816; &#x2658;
//    white chess pawn    ♙   U+2659  &#9817; &#x2659;
//    black chess king    ♚   U+265A  &#9818; &#x265A;
//    black chess queen   ♛   U+265B  &#9819; &#x265B;
//    black chess rook    ♜   U+265C  &#9820; &#x265C;
//    black chess bishop  ♝   U+265D  &#9821; &#x265D;
//    black chess knight  ♞   U+265E  &#9822; &#x265E;
//    black chess pawn    ♟   U+265F  &#9823; &#x265F;


    int[] board = new int[64];

    void init() {
        board = new int[64];
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
}
