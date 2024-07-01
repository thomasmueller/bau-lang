package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class ArrayAccess implements Expression, LeftValue {

    final Expression base;
    Expression arrayIndex;
    final boolean checkBounds;
    private boolean needToDecrementRefCountOnFree;

    ArrayAccess(Expression base, Expression arrayIndex, boolean checkBounds) {
        this.base = base;
        this.arrayIndex = arrayIndex;
        this.checkBounds = checkBounds;
    }

    @Override
    public Value eval(Memory memory) {
        Value val = base.eval(memory);
        Value idx = arrayIndex.eval(memory);
        if (val != null && idx != null) {
            if (val.isArray()) {
                return val.get(idx.intValue());
            }
        }
        return null;
    }
    
    public String assignmentC() {
        StringBuilder buff = new StringBuilder();
        buff.append(base.toC());
        if (arrayIndex != null) {
            buff.append("->data");
            if (checkBounds) {
                buff.append("[idx_2(");
                buff.append(arrayIndex.toC());
                buff.append(", ");
                buff.append(base.toC() + "->len");
                buff.append(")]");
            } else {
                buff.append("[");
                buff.append(arrayIndex.toC());
                buff.append("]");
            }
        }
        return buff.toString();
    }
    
    @Override
    public String decrementRefCountC() {
        if (base.type().isPointer()) {
            throw new IllegalStateException("Refcounts not yet supported on arrays of pointers");
        }
        return "";
    }

    @Override
    public String incrementRefCountC() {
        if (base.type().isPointer()) {
            throw new IllegalStateException("Refcounts not yet supported on arrays of pointers");
        }
        return "";
    }

    @Override
    public DataType type() {
        return base.type().baseType();
    }

    @Override
    public DataType canThrowException() {
        return null;
    }

    @Override
    public ArrayAccess replace(Variable old, Expression with) {
        Expression b2 = base.replace(old, with);
        Expression i2 = arrayIndex.replace(old, with);
        if (b2 == base && arrayIndex == i2) {
            return this;
        }
        return new ArrayAccess(b2, i2, checkBounds);
    }
    
    public String toString() {
        return base + "[" + arrayIndex + "]";
    }

    @Override
    public String toC() {
        StringBuilder buff = new StringBuilder();
        buff.append(base.toC());
        if (arrayIndex != null) {
            buff.append("->data");
            if (checkBounds) {
                buff.append("[idx_2(");
                buff.append(arrayIndex.toC());
                buff.append(", ");
                buff.append(base.toC() + "->len");
                buff.append(")]");
            } else {
                buff.append("[");
                buff.append(arrayIndex.toC());
                buff.append("]");
            }
        }
        return buff.toString();
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
    public void setBoundValue(Expression scope, String modify, Expression value) {
    }

    @Override
    public void addBoundCondition(Expression scope, String operation, Expression right) {        
    }
    
    @Override
    public boolean isSimple() {
        return false;
    }
    
    @Override
    public Expression writeStatements(Parser parser, ArrayList<Statement> target) {
        arrayIndex = arrayIndex.writeStatements(parser, target);
        return this;
    }
    
    @Override
    public boolean needToDecrementRefCountOnFree() {
        return needToDecrementRefCountOnFree;
    }
    
    @Override
    public void needToDecrementRefCountOnFree(boolean value) {
        needToDecrementRefCountOnFree = value;
    }

}
