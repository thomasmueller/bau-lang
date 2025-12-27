package org.bau.games;

import java.util.ArrayList;
import java.util.List;

/**
 * A Sudoku solver that is using a (ver simple) SAT solver internally.
 * The problem is converted to CNF form (conjunctive normal form),
 * and then solved using the DPLL algorithm.
 */
public class SudokuSolverSat {

    public static void main(String... args) {
        System.out.println(solve(args));
    }

    public static String solve(String... args) {
        int[] puzzle = new int[9*9];
        String usage = "Expected 9 command line arguments:\n" +
                "<line1> <line2> ... <line9>\n" +
                "where each line is 9 characters: '0'-'9', or '.' for unknown";
        if(args.length != 9) {
            return "Got " + args.length + " arguments\n" + usage;
        }
        int pos = 0;
        for(int i=0; i<args.length; i++) {
            String a = args[i];
            for(int j=0; j<a.length(); j++, pos++) {
                char c = a.charAt(j);
                if (c == '.') {
                    c = '0';
                }
                if (c >= '0' && c <= '9') {
                    if (pos >= 9*9) {
                        return "Too many arguments\n" + usage;
                    }
                    puzzle[pos] = c - '0';
                } else {
                    return "Got " + a + "\n" + usage;
                }
            }
        }
        List<int[]> cnf = buildSudokuCNF(puzzle);
        int[] assignment = new int[9*9*9];
        SatSolver solver = new SatSolver(cnf, assignment);
        boolean sat = solver.solve();
        if (!sat) {
            return "Can not solve";
        }
        char[] result = new char[9*10];
        for (int r = 0; r < 9; r++) {
            result[r * 10 + 9] = '\n';
        }
        for (int v = 0; v < 9*9*9; v++) {
            if (assignment[v] == 1) {
                int d = v % 9;
                int c = (v / 9) % 9;
                int r = (v / 9) / 9;
                result[10 * r + c] = (char) ('1' + d);
            }
        }
        return new String(result);
    }

    private static int var(int r, int c, int d) {
        return r * 81 + c * 9 + d + 1;
    }

    private static List<int[]> buildSudokuCNF(int[] puzzle) {
        List<int[]> cnf = new ArrayList<>();

        // there is at least one number in each entry
        for (int x=0; x<9; x++) {
            for (int y=0; y<9; y++) {
                int[] clause = new int[9];
                for (int z=0; z<9; z++) {
                    clause[z] = var(x,y,z);
                }
                cnf.add(clause);
            }
        }

        // each number appears at most once in each row
        for (int y=0; y<9; y++) {
            for (int z=0; z<9; z++) {
                for (int x=0; x<8; x++) {
                    for (int i=x+1; i<9; i++) {
                        cnf.add(new int[]{-var(x,y,z), -var(i,y,z)});
                    }
                }
            }
        }

        // each number appears at most once in each column
        for (int x=0; x<9; x++) {
            for (int z=0; z<9; z++) {
                for (int y=0; y<8; y++) {
                    for (int i=y+1; i<9; i++) {
                        cnf.add(new int[]{-var(x,y,z), -var(x,i,z)});
                    }
                }
            }
        }

        // each number appears at most once in each 3x3 sub-grid
        for (int z=0; z<9; z++) {
            for (int i=0; i<3; i++) {
                for (int j=0; j<3; j++) {
                    for (int x=0; x<3; x++) {
                        for (int y=0; y<2; y++) {
                            for (int k=y+1; k<3; k++) {
                                cnf.add(new int[]{-var(3*i+x,3*j+y,z), -var(3*i+x,3*j+k,z)});
                            }
                        }
                    }
                }
            }
        }

        // each number appears at most once in each 3x3 subgrid (house)
        for (int z=0; z<9; z++) {
            for (int i=0; i<3; i++) {
                for (int j=0; j<3; j++) {
                    for (int x=0; x<2; x++) {
                        for (int y=0; y<3; y++) {
                            for (int k=x+1; k<3; k++) {
                                for (int l=0; l<3; l++) {
                                    cnf.add(new int[]{-var(3*i+x,3*j+y,z), -var(3*i+k,3*j+l,z)});
                                }
                            }
                        }
                    }
                }
            }
        }
        // known entries
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int val = puzzle[r * 9 + c];
                if (val != 0) {
                    cnf.add(new int[] { var(r, c, val - 1) });
                }
            }
        }
        return cnf;
    }

    static class SatSolver {
        // n is true, negative: n is false
        final List<int[]> clauses;

        // 0 unassigned, 1 true, -1 false
        final int[] assignment;

        final int[] stack;
        int stackPos;

        SatSolver(List<int[]> clauses, int[] assignment) {
            this.clauses = clauses;
            this.assignment = assignment;
            this.stack = new int[assignment.length * 10];
        }
        boolean solve() {
            while (true) {
                boolean unitFound = false;
                for (int i = 0; i < clauses.size(); i++) {
                    int[] clause = clauses.get(i);
                    int unassignedCount = 0;
                    int lastLit = 0;
                    boolean clauseSatisfied = false;
                    for (int lit : clause) {
                        int v = Math.abs(lit) - 1;
                        int sign = lit > 0 ? 1 : -1;
                        if (assignment[v] == 0) {
                            unassignedCount++;
                            lastLit = lit;
                        } else if (assignment[v] == sign) {
                            // clause already satisfied
                            clauseSatisfied = true;
                            break;
                        }
                    }
                    if (clauseSatisfied) {
                        continue;
                    }
                    if (unassignedCount == 0) {
                        // conflict
                        return false;
                    }
                    if (unassignedCount == 1) {
                        unitFound = true;
                        int v = Math.abs(lastLit) - 1;
                        int sign = lastLit > 0 ? 1 : -1;
                        if (assignment[v] == 0) {
                            stack[stackPos++] = assignment[v];
                            stack[stackPos++] = v;
                            assignment[v] = sign;
                        } else if (assignment[v] != sign) {
                            // conflict
                            return false;
                        }
                    }
                }
                if (!unitFound) {
                    break;
                }
            }
            boolean done = true;
            for (int a : assignment) {
                if (a == 0) {
                    done = false;
                    break;
                }
            }
            if (done) {
                return true;
            }
            int nextVar = -1;
            for (int i = 0; i < assignment.length; i++) {
                if (assignment[i] == 0) {
                    nextVar = i;
                    break;
                }
            }
            assignment[nextVar] = 1;
            int sp = stackPos;
            if (solve()) {
                return true;
            }
            while (stackPos > sp) {
                assignment[stack[--stackPos]] = stack[--stackPos];
            }
            assignment[nextVar] = -1;
            if (solve()) {
                return true;
            }
            while (stackPos > sp) {
                assignment[stack[--stackPos]] = stack[--stackPos];
            }
            assignment[nextVar] = 0;
            return false;
        }

    }

}
