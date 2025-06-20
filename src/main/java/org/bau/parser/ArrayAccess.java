package org.bau.parser;

import java.util.ArrayList;

import org.bau.parser.Statement.StatementResult;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValuePanic;
import org.bau.runtime.Value.ValueRef;

public class ArrayAccess implements Expression, LeftValue {

    Expression base;
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
            Value array = null;
            if (val instanceof ValueRef) {
                array = memory.getHeap(val.longValue());
            } else if (val.isArray()) {
                array = val;
            }
            if (array != null && array.isArray()) {
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
                buff.append("[" + Program.esc("idx") + "_2(");
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
            if (type().memoryType() == MemoryType.REF_COUNT) {
                return Free.DEC_USE + "(" + toC() + ", " + type().nameC() + ");\n";
            } else {
                return type().idC() + "_free(" + toC() + ");\n";
            }
        }
        return "";
    }

    @Override
    public String incrementRefCountC() {
        if (type().needIncDec()) {
            if (type().memoryType() == MemoryType.REF_COUNT) {
                return Free.INC_USE + "(" + toC() + ");\n";
            } else if (type().needFree()) {
                return "";
            }
        } else if (type().needFree()) {
            return type().toC() + "_copy(&" + toC() + ");\n";
        }
        return "";
    }

    @Override
    public DataType type() {
        // array elements is always nullable
        DataType type = base.type().baseType();
        if (type.orNull() != null) {
            return type.orNull();
        }
        // numbers can not be null
        return type;
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
                buff.append("[" + Program.esc("idx") + "_2(");
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
    public void setOwnedBoundsToNull(Expression scope) {
        // ignore
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
        base = base.writeStatements(parser, false, target);
        arrayIndex = arrayIndex.writeStatements(parser, false, target);
        return this;
    }

    @Override
    public Value setValue(Memory memory, Value val, boolean incRefCount, boolean initial) {
        Value indexValue = arrayIndex.eval(memory);
        if (indexValue == null) {
            throw new IllegalStateException();
        }
        int index = indexValue.intValue();
        Value baseVal = base.eval(memory);
        if (baseVal == null) {
            throw new IllegalStateException();
        }
        Value array;
        if (baseVal instanceof ValueRef) {
            array = memory.getHeap(baseVal.longValue());
        } else {
            array = baseVal;
        }
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
            array.set(index, val);
            if (incRefCount) {
                memory.incHeap(val.longValue());
            }
            if (old != null) {
                StatementResult result = Free.decRefCount(old, type(), memory);
                if (result == StatementResult.PANIC) {
                    return memory.getGlobal(Memory.PANIC);
                }
            }
        } else {
            array.set(index, val);
        }
        return null;
    }

    @Override
    public boolean isContant() {
        return false;
    }

    @Override
    public void used(Program program) {
        base.used(program);
        if (arrayIndex != null) {
            if (checkBounds) {
                program.getFunction(null, null, "idx", 2).used(program);
            }
            arrayIndex.used(program);
        }
    }

    @Override
    public void incrementReassignCount() {
        // ignore
    }

}
