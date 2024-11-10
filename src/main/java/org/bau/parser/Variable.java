package org.bau.parser;

import java.util.ArrayList;

import org.bau.parser.Statement.StatementResult;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Variable implements Expression, LeftValue {
    String name;
    String module;
    DataType type;
    private Bounds bounds;
    private Bounds lenBounds;
    boolean isConstant;
    Value constantValue;
    private boolean needToDecrementRefCountOnFree = true;
    public boolean global;

    public Variable(String name, DataType type) {
        this(null, name, false, type);
    }

    public Variable(String module, String name, boolean global, DataType type) {
        this.module = module;
        this.name = name;
        this.global = global;
        this.type = type;
        Expression max = type.maxValue;
        if (max != null) {
            bounds = new Bounds();
            bounds.addCondition(null, "<", max);
        }
    }

    public static String getGlobalVariableId(String module, String name) {
        if (module != null) {
            return module + "." + name;
        }
        return name;
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
            StringBuilder buff = new StringBuilder();
            if (type.isFloatingPoint) {
                buff.append(NumberValue.toC(constantValue.doubleValue()));
            } else {
                buff.append(NumberValue.toC(constantValue.longValue()));
            }
            buff.append(" /* " + name + " */");
            return buff.toString();
        }
        return name;
    }

    @Override
    public String decrementRefCountC() {
        if (type().needIncDec()) {
            if (needToDecrementRefCountOnFree) {
                return Free.DEC_USE + "(" + name + ", " + type().nameC() + ");\n";
            }
        }
        return "";
    }

    @Override
    public String incrementRefCountC() {
        if (type().needIncDec()) {
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
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        return this;
    }

    @Override
    public Value setValue(Memory memory, Value val, boolean incRefCount) {
        if (global) {
            if (type.needIncDec()) {
                Value old = memory.getGlobal(name);
                if (old != null) {
                    StatementResult result = Free.decRefCount(old, type, memory);
                    if (result == StatementResult.PANIC) {
                        return memory.getGlobal(Memory.PANIC);
                    }
                }
                memory.setGlobal(name, val);
                if (incRefCount) {
                    memory.incHeap(val.longValue());
                }
            } else {
                memory.setGlobal(name, val);
            }
        } else {
            if (type.needIncDec()) {
                Value old = memory.getLocal(name);
                if (old != null) {
                    StatementResult result = Free.decRefCount(old, type, memory);
                    if (result == StatementResult.PANIC) {
                        return memory.getGlobal(Memory.PANIC);
                    }
                }
                memory.setLocal(name, val);
                if (incRefCount) {
                    memory.incHeap(val.longValue());
                }
            } else {
                memory.setLocal(name, val);
            }
        }
        return null;
    }

}
