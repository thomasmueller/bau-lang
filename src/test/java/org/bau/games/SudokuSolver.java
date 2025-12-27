package org.bau.games;

/**
 * A very simple Sudoku solver.
 */
public class SudokuSolver {

    public static void main(String... args) {
        if (args.length != 1 || args[0].length() != 9 * 9) {
            System.out.println("Expected: <line1><line2>...<line9>");
            System.out.println("where each line is 9 characters: '0'-'9', or '.' for unknown.");
        } else {
            char[] result = solve(args[0].toCharArray());
            if (result != null) {
                for(int i=0; i<result.length; i++) {
                    System.out.print(result[i]);
                    if(i%9 == 8) {
                        System.out.println();
                    }
                }
                System.out.println();
            } else {
                System.out.println("No solution found");
            }
        }
    }
    public static char[] solve(char[] c) {
        int i = 0;
        while(i < c.length) {
            if (c[i] == '.') {
                break;
            }
            i++;
        }
        if (i >= c.length) {
            return c;
        }
        for (char k = '1'; k <= '9'; k++) {
            int j = 0;
            while (j < 9) {
                if (c[(i / 9 * 9) + j] == k) {
                    // a digit in the same row matches
                    break;
                }
                if (c[(i % 9) + 9 * j] == k) {
                    // a digit in the same column matches
                    break;
                }
                // house
                int h = (((i / 27) % 3) * 3) + ((i % 9) / 3);
                int n3 = h * 3 + (h / 3) * 18 + (j % 3) + (j / 3) * 9;
                if (c[n3] == k) {
                    // a digit in the same house matches
                    break;
                }
                j++;
            }
            if (j >= 9) {
                c[i] = k;
                char[] result = solve(c);
                if (result != null) {
                    return result;
                }
                c[i] = '.';
            }
        }
        return null;
    }

}
