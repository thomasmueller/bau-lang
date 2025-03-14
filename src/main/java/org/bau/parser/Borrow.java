package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Borrow implements Expression {

    private Expression base;

    Borrow(Expression base) {
        this.base = base;
    }

    @Override
    public Value eval(Memory memory) {
        return base.eval(memory);
    }

    @Override
    public DataType type() {
        DataType type = base.type();
        if (type.memoryType() != MemoryType.OWNER) {
            throw new IllegalArgumentException();
        }
        type = type.borrowType();
        return type;
    }

    @Override
    public DataType canThrowException() {
        return base.canThrowException();
    }

    @Override
    public Expression replace(Variable old, Expression with) {
        return new Borrow(base.replace(old, with));
    }

    @Override
    public Expression simplify() {
        return new Borrow(base.simplify());
    }

    @Override
    public String toC() {
        return base.toC();
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
    public boolean isSimple() {
        return base.isSimple();
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        base = base.writeStatements(parser, false, target);
        return this;
    }

    public static String resetUsedOwned(List<Expression> usedOwnedList) {
        if (usedOwnedList.isEmpty()) {
            return "";
        }
        StringBuilder buff = new StringBuilder();
        for(Expression e : usedOwnedList) {
            buff.append(e.toC() + " = NULL;\n");
        }
        return buff.toString();
    }

}
