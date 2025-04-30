package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

import org.bau.parser.Statement.StatementResult;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Variable implements Expression, LeftValue {
    String name;
    String module;
    private DataType type;
    private Bounds bounds;
    private Bounds lenBounds;
    boolean isConstant;
    Value constantValue;
    public boolean global;

    public Variable(String name, DataType type) {
        this(null, name, false, type);
    }

    public Variable(String module, String name, boolean global, DataType type) {
        this.module = module;
        this.name = name;
        this.global = global;
        this.type = type;
        if (type.isRange()) {
            Expression max = type.maxValue;
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
        if (constantValue != null && type.isNumber() && !type.isArray()) {
            StringBuilder buff = new StringBuilder();
            if (type.isFloatingPoint()) {
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
    public List<Expression> getUsedOwned() {
        ArrayList<Expression> usedOwned = new ArrayList<Expression>();
        DataType type = type();
        if (type != null && type.memoryType() == MemoryType.OWNER) {
            usedOwned.add(this);
        }
        return usedOwned;
    }

    @Override
    public void setOwnedBoundsToNull(Expression scope) {
        if (type != null && type.memoryType() == MemoryType.OWNER) {
            setBoundValue(scope, "=", new NullValue(type));
        }
    }

    @Override
    public String decrementRefCountC() {
        if (type().needIncDec()) {
            if (type().memoryType() == MemoryType.REF_COUNT) {
                return Free.DEC_USE_STACK + "(" + name + ", " + type().nameC() + ");\n";
            } else {
                return "";
            }
        }
        return "";
    }

    @Override
    public String incrementRefCountC() {
        if (type().needIncDec()) {
            if (type().memoryType() == MemoryType.REF_COUNT) {
                return Free.INC_USE_STACK + "(" + name + ");\n";
            } else {
                return "";
            }
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
        if (!(value instanceof NullValue) && !value.type().isNumber()) {
            return;
        }
        if (value instanceof Call) {
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

    @Override
    public boolean isContant() {
        return isConstant;
    }

}
