package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Cast implements Expression {

    private final Expression base;
    private final DataType targetType;

    Cast(Expression base, DataType targetType) {
        this.base = base;
        this.targetType = targetType;
    }

    @Override
    public Value eval(Memory memory) {
        return base.eval(memory);
    }

    @Override
    public DataType type() {
        return targetType;
    }

    @Override
    public DataType canThrowException() {
        return base.canThrowException();
    }

    @Override
    public Expression replace(Variable old, Expression with) {
        return new Cast(base.replace(old, with), targetType);
    }

    @Override
    public Expression simplify() {
        return new Cast(base.simplify(), targetType);
    }

    @Override
    public String toC() {
        return "((" + targetType.toC() + ") " + base.toC() + ")";
    }

    @Override
    public boolean isEasyToRead() {
        return base.isEasyToRead();
    }

    @Override
    public Bounds getBounds() {
        return base.getBounds();
    }

    @Override
    public void setOwnedBoundsToNull(Expression scope) {
        base.setOwnedBoundsToNull(scope);
    }

    @Override
    public boolean isSimple() {
        return base.isSimple();
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        return new Cast(base.writeStatements(parser, assignment, target), targetType);
    }

    @Override
    public void used(Program program) {
        base.used(program);
        targetType.used(program);
    }

}
