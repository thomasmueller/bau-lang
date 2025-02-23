package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class ArenaValue implements Expression {
    Value value;
    DataType type;

    public ArenaValue(Value value, DataType type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public Value eval(Memory memory) {
        return value;
    }

    public DataType canThrowException() {
        return null;
    }

    public Expression replace(Variable old, Expression with) {
        return this;
    }

    @Override
    public DataType type() {
        return type;
    }

    public String toC() {
        return "newArena()";
    }

    public String toString() {
        return "newArena()";
    }

    @Override
    public boolean isEasyToRead() {
        return true;
    }

    @Override
    public Bounds getBounds() {
        return null;
    }

    @Override
    public Expression simplify() {
        return this;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        return this;
    }

}