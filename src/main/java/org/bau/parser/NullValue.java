package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class NullValue implements Expression {

    @Override
    public Value eval(Memory memory) {
        return null;
    }

    @Override
    public DataType type() {
        return null;
    }

    @Override
    public DataType canThrowException() {
        return null;
    }

    @Override
    public Expression replace(Variable old, Expression with) {
        return this;
    }

    @Override
    public Expression simplify() {
        return this;
    }
    
    @Override
    public String toString() {
        return "null";
    }

    @Override
    public String toC() {
        return "NULL";
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
    public boolean isSimple() {
        return true;
    }

    @Override
    public Expression writeStatements(Parser parser, ArrayList<Statement> target) {
        return this;
    }

}
