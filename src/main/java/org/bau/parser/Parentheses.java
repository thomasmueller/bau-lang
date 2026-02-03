package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Parentheses implements Expression {

    Expression base;

    public Parentheses(Expression base) {
        this.base = base;
    }

    @Override
    public Value eval(Memory memory) {
        return base.eval(memory);
    }

    @Override
    public DataType type() {
        return base.type();
    }

    @Override
    public DataType canThrowException() {
        return base.canThrowException();
    }

    @Override
    public Expression replace(Variable old, Expression with) {
        return new Parentheses(base.replace(old, with));
    }

    @Override
    public String toC() {
        return "(" + base.toC() + ")";
    }

    @Override
    public String toString() {
        return "(" + base + ")";
    }

    @Override
    public boolean isEasyToRead() {
        return true;
    }

    @Override
    public void setOwnedBoundsToNull(Solver solver, int level, boolean loop) {
        base.setOwnedBoundsToNull(solver, level, loop);
    }

    @Override
    public Expression simplify() {
        return this;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        base = base.writeStatements(parser, assignment, target);
        return this;
    }

    @Override
    public void used(Program program) {
        base.used(program);
    }

    @Override
    public boolean containsModifiableVariables() {
        return base.containsModifiableVariables();
    }

}
