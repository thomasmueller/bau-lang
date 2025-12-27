package org.bau.games;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SudokuTest {

    @Test
    public void satSolver() {
        assertEquals(
                "265137849\n" +
                "397824156\n" +
                "814965723\n" +
                "521496387\n" +
                "789312564\n" +
                "436578291\n" +
                "673289415\n" +
                "952741638\n" +
                "148653972\n",
                SudokuSolverSat.solve(
                "....37..9",
                "...82.1..",
                "8.4...7..",
                ".214...8.",
                ".8..1..6.",
                ".3...829.",
                "..3...4.5",
                "..2.41...",
                "1..65...."));
        assertEquals(
                "812753649\n"
                + "943682175\n"
                + "675491283\n"
                + "154237896\n"
                + "369845721\n"
                + "287169534\n"
                + "521974368\n"
                + "438526917\n"
                + "796318452\n",
        SudokuSolverSat.solve(
                "8........",
                "..36.....",
                ".7..9.2..",
                ".5...7...",
                "....457..",
                "...1...3.",
                "..1....68",
                "..85...10",
                ".9....4.."));
    }

    @Test
    public void solve() {
        char[] result = SudokuSolver.solve("8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4..".toCharArray());
        assertEquals("812753649943682175675491283154237896369845721287169534521974368438526917796318452", String.valueOf(result));

    }
}
