package org.bau.parser;

import java.util.ArrayList;

import org.bau.parser.Statement.StatementResult;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueStruct;

public class FieldAccess implements Expression, LeftValue {

    final Expression base;
    final String fieldName;
    private Bounds lenBounds;
    final DataType type;

    FieldAccess(Expression base, String fieldName, DataType type) {
        this.base = base;
        this.fieldName = fieldName;
        this.type = type;
    }

    @Override
    public Value eval(Memory memory) {
        if (base.type().isArray()) {
            if ("len".equals(fieldName)) {
                if (base instanceof Variable) {
                    Variable var = (Variable) base;
                    Bounds lenBounds = var.getLenBounds();
                    if (lenBounds != null) {
                        Value v = lenBounds.eval();
                        if (v != null) {
                            return v;
                        }
                    }
                }
                Value v = base.eval(memory);
                if (v == null) {
                    return null;
                }
                if (memory == null) {
                    Bounds b = getBounds();
                    if (b != null) {
                        Value val = b.eval();
                        if (val != null) {
                            return val;
                        }
                    }
                }
                Value array = memory.getHeap(v.longValue());
                return array.len();
            }
        }
        Value v = base.eval(memory);
        if (v == null) {
            return null;
        }
        if (base.type().isPointer()) {
            v = memory.getHeap(v.longValue());
        }
        if (!(v instanceof ValueStruct)) {
            throw new IllegalStateException("Expected a struct, got " + v);
        }
        ValueStruct vs = (ValueStruct) v;
        return vs.get(fieldName);
    }

    @Override
    public DataType type() {
        return type;
    }

    @Override
    public DataType canThrowException() {
        return null;
    }

    @Override
    public FieldAccess replace(Variable old, Expression with) {
        Expression b2 = base.replace(old, with);
        if (b2 == base) {
            return this;
        }
        return new FieldAccess(b2, fieldName, type);
    }

    @Override
    public String toC() {
        if (base.type().isArray()) {
            // only "len" is supported
            if (!"len".equals(fieldName)) {
                throw new IllegalStateException("Only 'len' is supported");
            }
            return base.toC() + "->len";
        } else if (base.type().isPointer()) {
            return base.toC() + "->" + fieldName;
        } else {
            return base.toC() + "." + fieldName;
        }
    }

    public String assignmentC() {
        StringBuilder buff = new StringBuilder();
        buff.append(base.toC());
        if (base.type().isArray()) {
            if (!"len".equals(fieldName)) {
                throw new IllegalStateException("Can not change the length");
            } else {
                throw new IllegalStateException("Only 'len' is supported");
            }
        } else if (base.type().isPointer()) {
            return base.toC() + "->" + fieldName;
        } else {
            return base.toC() + "." + fieldName;
        }
    }

    @Override
    public String decrementRefCountC() {
        if (type().isPointer() || type().isArray()) {
            return Free.DEC_USE + "(" + toC() + ", " + type().nameC() + ");\n";
        }
        return "";
    }

    @Override
    public String incrementRefCountC() {
        if (type().isPointer() || type().isArray()) {
            return Free.INC_USE + "(" + toC() + ");\n";
        }
        return "";
    }

    @Override
    public boolean isEasyToRead() {
        return true;
    }

    public String toString() {
        return base + "." + fieldName;
    }

    @Override
    public Expression simplify() {
        return this;
    }

    @Override
    public void setBoundValue(Expression scope, String modify, Expression value) {

    }

    @Override
    public Bounds getBounds() {
        if (base.type().isArray() && fieldName.equals("len")) {
            if (base instanceof Variable) {
                return ((Variable) base).getLenBounds();
            } else if (base instanceof FieldAccess) {
                FieldAccess f = (FieldAccess) base;
                return f.getLenBounds();
            }
        }
        return null;
    }

    public Bounds getLenBounds() {
        return lenBounds;
    }

    @Override
    public void addBoundCondition(Expression scope, String operation, Expression right) {
        if (base.type().isArray() && fieldName.equals("len") && base instanceof Variable) {
            ((Variable) base).addLenBoundCondition(scope, operation, right);
        }
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        return this;
    }

    @Override
    public Value setValue(Memory memory, Value val, boolean incRefCount) {
        Value baseVal = base.eval(memory);
        if (baseVal == null) {
            throw new IllegalStateException();
        }
        if (base.type().isPointer()) {
            baseVal = memory.getHeap(baseVal.longValue());
        }
        if (!(baseVal instanceof ValueStruct)) {
            throw new IllegalStateException();
        }
        ValueStruct v = (ValueStruct) baseVal;
        if (type().isArray() || type.isPointer()) {
            Value old = v.get(fieldName);
            if (old != null) {
                StatementResult result = Free.decRefCount(old, type, memory);
                if (result == StatementResult.PANIC) {
                    return memory.getGlobal(Memory.PANIC);
                }
            }
            v.set(fieldName, val);
            if (incRefCount) {
                memory.incHeap(val.longValue());
            }
        } else {
            v.set(fieldName, val);
        }
        return null;
    }
}
