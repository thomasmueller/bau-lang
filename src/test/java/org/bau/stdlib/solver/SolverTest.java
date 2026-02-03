package org.bau.stdlib.solver;

import static org.bau.parser.Solver.number;
import static org.bau.parser.Solver.operation;
import static org.bau.parser.Solver.rule;
import static org.bau.parser.Solver.variable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.bau.parser.Solver;
import org.bau.parser.Solver.Rule;
import org.junit.Test;

public class SolverTest {

    @Test
    public void addSubtract() {
        Solver s;

        s = new Solver();
        s.addRule(rule(variable("a"), "=", operation(variable("x"), "+", number(1))));
        assertTrue(s.isTrue(rule(variable("a"), ">", variable("x"))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", operation(variable("x"), "-", number(1))));
        assertTrue(s.isTrue(rule(variable("a"), "<", variable("x"))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", operation(variable("x"), "-", number(-1))));
        assertTrue(s.isTrue(rule(variable("a"), ">=", variable("x"))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", operation(variable("x"), "+", number(-1))));
        assertTrue(s.isTrue(rule(variable("a"), "<=", variable("x"))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", operation(variable("x"), "+", number(0))));
        assertTrue(s.isTrue(rule(variable("a"), "=", variable("x"))));

    }

    @Test
    public void solveFor() {
        Rule r = rule(operation(variable("i"), "+", number(1)), ">=", variable("len"));
        Rule r2 = Solver.trySolveFor(r, variable("i"));
        assertEquals("i >= (len + -1)", r2.toString());

        r = rule(operation(variable("a"), "-", variable("b")), ">=", variable("len"));
        r2 = Solver.trySolveFor(r, variable("b"));
        assertEquals("b <= (a - len)", r2.toString());
}

    @Test
    public void numbers() {
        Solver s;

        s = new Solver();
        assertTrue(s.isTrue(rule(number(0), "<", operation(number(4), "-", number(3)))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", number(1)));
        assertTrue(s.isTrue(rule(variable("a"), ">", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", number(1)));
        assertFalse(s.isTrue(rule(variable("a"), "<", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", number(0)));
        assertTrue(s.isTrue(rule(variable("a"), "<=", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", number(0)));
        assertTrue(s.isTrue(rule(variable("a"), "<=", number(1))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", number(1)));
        assertFalse(s.isTrue(rule(variable("a"), "<=", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", number(1)));
        assertTrue(s.isTrue(rule(variable("a"), ">=", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), ">", number(1)));
        assertTrue(s.isTrue(rule(variable("a"), ">", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), ">", number(0)));
        assertTrue(s.isTrue(rule(variable("a"), ">", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), ">", number(0)));
        assertFalse(s.isTrue(rule(variable("a"), ">", number(1))));

        s = new Solver();
        s.addRule(rule(variable("a"), ">=", number(1)));
        assertTrue(s.isTrue(rule(variable("a"), ">", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), ">=", number(0)));
        assertFalse(s.isTrue(rule(variable("a"), ">", number(0))));
    }

    @Test
    public void indirect() {
        Solver s;

        s = new Solver();
        s.addRule(rule(variable("x"), ">", number(0)));
        assertTrue(s.isTrue(rule(operation(variable("x"), "-", number(1)), ">=", number(0))));

        s = new Solver();
        s.addRule(rule(variable("x"), ">", number(0)));
        assertFalse(s.isTrue(rule(operation(variable("x"), "-", number(2)), ">=", number(0))));

        s = new Solver();
        s.addRule(rule(variable("len"), ">=", number(16)));
        assertTrue(s.isTrue(rule(number(0), "<", operation(variable("len"), "-", number(7)))));

        s = new Solver();
        s.addRule(rule(variable("p"), "<", operation(variable("len"), "-", number(3))));
        assertTrue(s.isTrue(rule(operation(variable("p"), "+", number(3)), "<", variable("len"))));

        s = new Solver();
        s.addRule(rule(variable("p"), ">=", number(0)));
        assertTrue(s.isTrue(rule(operation(variable("p"), "+", number(1)), ">=", number(0))));

        s = new Solver();
        s.addRule(rule(variable("p"), ">=", number(0)));
        s.addRule(rule(variable("p"), "<", operation(variable("l"), "-", number(4))));
        assertTrue(s.isTrue(rule(operation(variable("p"), "+", number(1)), "<", variable("l"))));

        s = new Solver();
        s.addRule(rule(variable("a"), ">=", number(0)));
        s.addRule(rule(variable("b"), "=", operation(variable("a"), "+", number(1))));
        assertTrue(s.isTrue(rule(variable("b"), ">=", number(0))));

        s = new Solver();
        s.addRule(rule(variable("0t0.len"), "=", number(10)));
        s.addRule(rule(variable("0t0.len"), "=", variable("result.len")));
        s.addRule(rule(variable("i"), "<", number(10)));
        assertTrue(s.isTrue(rule(variable("i"), "<", variable("result.len"))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", number(1)));
        s.addRule(rule(variable("b"), "=", variable("a")));
        assertTrue(s.isTrue(rule(variable("b"), "=", number(1))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", number(1)));
        s.addRule(rule(variable("a"), "=", variable("b")));
        assertTrue(s.isTrue(rule(variable("b"), "=", number(1))));

        s = new Solver();
        s.addRule(rule(variable("a"), ">", number(0)));
        s.addRule(rule(variable("b"), ">", variable("a")));
        assertTrue(s.isTrue(rule(variable("b"), ">", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), ">", number(0)));
        s.addRule(rule(variable("b"), ">=", variable("a")));
        assertTrue(s.isTrue(rule(variable("b"), ">", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), ">", number(0)));
        s.addRule(rule(variable("b"), "<", variable("a")));
        assertFalse(s.isTrue(rule(variable("b"), "=", number(0))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", operation(variable("b"), "+", number(1))));
        s.addRule(rule(variable("b"), "=", number(10)));
        assertTrue(s.isTrue(rule(variable("a"), "=", number(11))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", operation(variable("b"), "+", number(1))));
        s.addRule(rule(variable("b"), "=", number(11)));
        assertFalse(s.isTrue(rule(variable("a"), "=", number(11))));

    }

    @Test
    public void variables() {
        Solver s;

        s = new Solver();
        s.addRule(rule(variable("a"), "=", variable("b")));
        assertTrue(s.isTrue(rule(variable("a"), "=", variable("b"))));

        s = new Solver();
        s.addRule(rule(variable("a"), "=", variable("b")));
        assertFalse(s.isTrue(rule(variable("a"), "<", variable("b"))));

        s = new Solver();
        s.addRule(rule(variable("a"), "<", variable("b")));
        assertTrue(s.isTrue(rule(variable("a"), "<", variable("b"))));

        s = new Solver();
        s.addRule(rule(variable("a"), "<", variable("b")));
        assertTrue(s.isTrue(rule(variable("a"), "<=", variable("b"))));
    }

}
