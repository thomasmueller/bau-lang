package org.bau.games;

/**
 * A very simple Sudoku solver.
 */
public class SudokuSolver {

    public static void main(String... args) {
        if (args.length != 1 || args[0].length() != 89) {
            System.out.println("Expected one argument: \"<line1> <line2> ... <line9>\"");
            System.out.println("where each line is 9 characters: '0'-'9', or '.' for unknown.");
            return;
        }
        char[] c = args[0].toCharArray();
        if (solve(c)) {
            for (int i = 0; i < 8; i++) {
                c[9 + 10 * i] = '\n';
            }
            System.out.println(new String(c));
        } else {
            System.out.println("No solution found");
        }
    }

    public static boolean solve(char[] c) {
        int i = 0;
        while (i < c.length) {
            if (c[i] == '.') {
                break;
            }
            i++;
        }
        if (i >= c.length) {
            return true;
        }
        for (char k = '1'; k <= '9'; k++) {
            int j = 0;
            while (j < 9) {
                int m = i / 10 * 9 + i % 10;
                if (c[((m / 9) * 10) + j] == k) {
                    // a digit in the same row matches
                    break;
                }
                if (c[(m % 9) + 10 * j] == k) {
                    // a digit in the same column matches
                    break;
                }
                // house
                int h = (((m / 27) % 3) * 3) + ((m % 9) / 3);
                int n3 = h * 3 + (h / 3) * 18 + (j % 3) + (j / 3) * 9;
                if (c[(n3 / 9 * 10) + (n3 % 9)] == k) {
                    // a digit in the same house matches
                    break;
                }
                j++;
            }
            if (j >= 9) {
                c[i] = k;
                if (solve(c)) {
                    return true;
                }
                c[i] = '.';
            }
        }
        return false;
    }

}
