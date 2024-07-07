package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Variable implements Expression, LeftValue {
    String name;
    DataType type;
    private Bounds bounds;
    private Bounds lenBounds;
    boolean isConstant;
    Value constantValue;
    private boolean needToDecrementRefCountOnFree = true;
    public boolean global;
    
    public Variable(String id, DataType type) {
        this.name = id;
        this.type = type;
    }
    
    @Override
    public Value eval(Memory memory) {
        if (isConstant && constantValue != null) {
            return constantValue;
        }
        if (memory == null) {
            return null;
        }
        if (global) {
            return memory.getGlobal(name);
        }
        return memory.getLocal(name);
    }
    
    public DataType canThrowException() {
        return null;
    }
    
    @Override
    public DataType type() {
        return type;
    }
    
    public Expression replace(Variable old, Expression with) {
        if (name.equals(old.name)) {
            return with;
        }
        return this;
    }    
    
    public String toString() {
        return name;
    }

    public String assignmentC() {
        return name;
    }
    
    public String toC() {
        if (constantValue != null && type.isSystem() && !type.isArray()) {
            return constantValue.toString() + " /* " + name + " */";
        }
        return name;
    }
    
    @Override
    public String decrementRefCountC() {
        if (type().isPointer() || type().isArray()) {
            if (needToDecrementRefCountOnFree) {
                return Free.DEC_USE + "(" + name + ");\n";
            }
        }
        return "";
    }

    @Override
    public String incrementRefCountC() {
        if (type().isPointer() || type().isArray()) {
            return Free.INC_USE + "(" + name + ");\n";
        }
        return "";
    }
    
    @Override
    public boolean isEasyToRead() {
        return true;
    }
    
    @Override
    public Bounds getBounds() {
        if (bounds == null) {
            Expression max = type.maxValue;
            if (max != null) {
                bounds = new Bounds();
                bounds.addCondition(null, "<", max);
            }
        }
        return bounds;
    }
    
    public Bounds getLenBounds() {
        return lenBounds;
    }
    
    @Override
    public Expression simplify() {
        if (isConstant && constantValue != null) {
            return new NumberValue(constantValue, type(), false);
        }
        return this;
    }
    
    public void addLenBoundCondition(Expression scope, String operation, Expression expr) {
        if (lenBounds == null) {
            lenBounds = new Bounds();
        }
        lenBounds.addCondition(scope, operation, expr);
    }
    
    public void addBoundCondition(Expression scope, String operation, Expression expr) {
        if (bounds == null) {
            bounds = new Bounds();
        }
        bounds.addCondition(scope, operation, expr);
    }

    @Override
    public void setBoundValue(Expression scope, String modify, Expression value) {
        if (!value.type().isSystem()) {
            return;
        }
        if (bounds == null) {
            bounds = new Bounds();
        }
        bounds.setBoundValue(scope, modify, value);
    }
    
    @Override
    public boolean isSimple() {
        return true;
    }
    
    @Override
    public Expression writeStatements(Parser parser, ArrayList<Statement> target) {
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

    @Override
    public Value setValue(Memory memory, Value val) {
        if (global) {
            memory.setGlobal(name, val);
        }
        memory.setLocal(name, val);
        return null;
    }
    
}
