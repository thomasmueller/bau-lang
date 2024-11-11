package org.bau.parser;

import java.util.ArrayList;

import org.bau.parser.Statement.StatementResult;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValuePanic;

public class ArrayAccess implements Expression, LeftValue {

    final Expression base;
    Expression arrayIndex;
    final boolean checkBounds;

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
            Value array = memory.getHeap(val.longValue());
            if (array.isArray()) {
                int index = idx.intValue();
                long len = array.len().longValue();
                if (index < 0 || index >= len) {
                    String message = "Array index " + index
                            + " is out of bounds for the array length " + len;
                    Value v = new ValuePanic(message);
                    memory.print(v);
                    memory.println();
                    memory.setGlobal(Memory.PANIC, v);
                    return v;
                }
                return array.get(index);
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
        if (type().needIncDec()) {
            return Free.DEC_USE + "(" + toC() + ", " + type().nameC() + ", 1);\n";
        }
        return "";
    }

    @Override
    public String incrementRefCountC() {
        if (type().needIncDec()) {
            return Free.INC_USE + "(" + toC() + ", 1);\n";
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
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        arrayIndex = arrayIndex.writeStatements(parser, false, target);
        return this;
    }

    @Override
    public Value setValue(Memory memory, Value val, boolean incRefCount) {
        Value indexValue = arrayIndex.eval(memory);
        if (indexValue == null) {
            throw new IllegalStateException();
        }
        int index = indexValue.intValue();
        Value baseVal = base.eval(memory);
        if (baseVal == null) {
            throw new IllegalStateException();
        }
        Value array = memory.getHeap(baseVal.longValue());
        long len = array.len().longValue();
        if (index < 0 || index >= len) {
            String message = "Array index " + index +
                    " is out of bounds for the array length " + len;
            ValuePanic v = new ValuePanic(message);
            memory.print(v);
            memory.println();
            memory.setGlobal(Memory.PANIC, v);
            return v;
        }
        if (type().needIncDec()) {
            Value old = array.get(index);
            if (old != null) {
                StatementResult result = Free.decRefCount(old, type(), memory);
                if (result == StatementResult.PANIC) {
                    return memory.getGlobal(Memory.PANIC);
                }
            }
            array.set(index, val);
            if (incRefCount) {
                memory.incHeap(val.longValue());
            }
        } else {
            array.set(index, val);
        }
        return null;
    }

}
