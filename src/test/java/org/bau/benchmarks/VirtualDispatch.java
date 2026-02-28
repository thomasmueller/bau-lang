package org.bau.benchmarks;

public class VirtualDispatch {

    static final int SIZE = 1_000_000;
    static final int ITERATIONS = 20;

    public static void main(String[] args) {
        Expression[] expressions = new Expression[SIZE];
        Statement[] statements = new Statement[SIZE];
        for (int i = 0; i < SIZE; i++) {
            switch (random(i, 9)) {
                case 0: expressions[i] = new Variable(); break;
                case 1: expressions[i] = new NumberLiteral(); break;
                case 2: expressions[i] = new StringLiteral(); break;
                case 3: expressions[i] = new NewExpr(); break;
                case 4: expressions[i] = new NullValue(); break;
                case 5: expressions[i] = new ArrayAccess(); break;
                case 6: expressions[i] = new Call(); break;
                case 7: expressions[i] = new Operation(); break;
                case 8: expressions[i] = new Comparison(); break;
                default: throw new AssertionError();
            }
            switch (random(i + SIZE, 8)) {
                case 0: statements[i] = new IfStmt(); break;
                case 1: statements[i] = new WhileStmt(); break;
                case 2: statements[i] = new DoWhileStmt(); break;
                case 3: statements[i] = new Assignment(); break;
                case 4: statements[i] = new SwitchStmt(); break;
                case 5: statements[i] = new ReturnStmt(); break;
                case 6: statements[i] = new BreakStmt(); break;
                case 7: statements[i] = new ContinueStmt(); break;
                default: throw new AssertionError();
            }
        }
        for (int test = 0; test < 5; test++) {
            long start = System.nanoTime();
            long dummy = 0;
            for (int i = 0; i < ITERATIONS; i++) {
                dummy += run(expressions, statements);
            }
            long end = System.nanoTime();
            long timeMs = (end - start) / 1_000_000;
            System.out.println("Time " + timeMs + " ms; dummy: " + dummy);
        }
    }

    static long run(Expression[] expressions, Statement[] statements) {
        long sum = 0;
        for (int i = 0; i < expressions.length; i++) {
            sum += expressions[i].eval();
            sum += expressions[i].hash();
            sum += statements[i].execute();
            sum += statements[i].hash();
        }
        return sum;
    }

    private static int random(int x, int max) {
        // https://stackoverflow.com/questions/664014/what-integer-hash-function-are-good-that-accepts-an-integer-hash-key/12996028#12996028
        int ux = ((x >>> 16) ^ x) * 0x45d9f3b;
        ux = ((ux >>> 16) ^ ux) * 0x45d9f3b;
        ux = (ux >>> 16) ^ ux;
        return (int) (ux & 0xfffffffL) % max;
    }

    // ===== Interfaces =====

    interface Hashable {
        int hash();
    }

    interface Expression extends Hashable {
        int eval();
    }

    interface LeftValue extends Expression {
        int store(int v);
    }

    interface Statement extends Hashable {
        int execute();
    }

    // ===== Expression Implementations =====

    static class Variable implements LeftValue {
        int value = 1;
        public int eval() { return value; }
        public int store(int v) { value = v; return v; }
        public int hash() { return 11; }
    }

    static class NumberLiteral implements Expression {
        public int eval() { return 2; }
        public int hash() { return 12; }
    }

    static class StringLiteral implements Expression {
        public int eval() { return 3; }
        public int hash() { return 13; }
    }

    static class NewExpr implements Expression {
        public int eval() { return 4; }
        public int hash() { return 14; }
    }

    static class NullValue implements Expression {
        public int eval() { return 5; }
        public int hash() { return 15; }
    }

    static class ArrayAccess implements Expression {
        public int eval() { return 6; }
        public int hash() { return 16; }
    }

    static class Call implements Expression {
        public int eval() { return 7; }
        public int hash() { return 17; }
    }

    static class Operation implements Expression {
        public int eval() { return 8; }
        public int hash() { return 18; }
    }

    static class Comparison implements Expression {
        public int eval() { return 9; }
        public int hash() { return 19; }
    }

    // ===== Statement Implementations =====

    static class IfStmt implements Statement {
        public int execute() { return 21; }
        public int hash() { return 31; }
    }

    static class WhileStmt implements Statement {
        public int execute() { return 22; }
        public int hash() { return 32; }
    }

    static class DoWhileStmt implements Statement {
        public int execute() { return 23; }
        public int hash() { return 33; }
    }

    static class Assignment implements Statement, Expression {
        public int execute() { return 24; }
        public int eval() { return 25; }
        public int hash() { return 34; }
    }

    static class SwitchStmt implements Statement {
        public int execute() { return 26; }
        public int hash() { return 36; }
    }

    static class ReturnStmt implements Statement {
        public int execute() { return 27; }
        public int hash() { return 37; }
    }

    static class BreakStmt implements Statement {
        public int execute() { return 28; }
        public int hash() { return 38; }
    }

    static class ContinueStmt implements Statement {
        public int execute() { return 29; }
        public int hash() { return 39; }
    }
}
