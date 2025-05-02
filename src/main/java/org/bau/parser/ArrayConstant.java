package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueArray;

public class ArrayConstant implements Expression {

    private ValueArray data;
    private DataType type;
    private long reference;

    public ArrayConstant(ValueArray data, DataType type, long reference) {
        this.data = data;
        this.type = type;
        this.reference = reference;
    }

    @Override
    public Value eval(Memory memory) {
        return data;
    }

    @Override
    public DataType type() {
        return type;
    }

    public DataType canThrowException() {
        return null;
    }

    public Value len() {
        return data.len();
    }

    public String toC() {
        return "array_" + reference;
    }

    @Override
    public void setOwnedBoundsToNull(Expression scope) {
    }

    public Expression replace(Variable old, Expression with) {
        return this;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("arrayOf(");
        buff.append(type.toString());
        for (int i = 0; i < data.len().intValue(); i++) {
            buff.append(", ");
            Value v = data.get(i);
            NumberValue nv = new NumberValue(v, type, false);
            buff.append(nv.toString());
        }
        buff.append(")");
        return buff.toString();
    }

    @Override
    public boolean isEasyToRead() {
        return false;
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