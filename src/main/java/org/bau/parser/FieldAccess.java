package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueStruct;

public class FieldAccess implements Expression, LeftValue {
    
    final Expression base;
    final String fieldName;
    private Bounds lenBounds;
    final DataType type;
    private boolean needToDecrementRefCountOnFree = false;
    
    FieldAccess(Expression base, String fieldName, DataType type) {
        this.base = base;
        this.fieldName = fieldName;
        this.type = type;
    }

    @Override
    public Value eval(Memory memory) {
        if (base.type().isArray()) {
            if ("len".equals(fieldName)) {
                Value v = base.eval(memory);
                if (v == null) {
                    return null;
                }
                return v.len();
            }
        }
        Value v = base.eval(memory);
        if (v == null) {
            return null;
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
    
    public void addLenBoundCondition(Expression scope, String operation, Expression expr) {
        if (lenBounds == null) {
            lenBounds = new Bounds();
        }
        lenBounds.addCondition(scope, operation, expr);
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
        Value baseVal = base.eval(memory);
        if (baseVal == null) {
            throw new IllegalStateException();
        }
        if (!(baseVal instanceof ValueStruct)) {
            throw new IllegalStateException();
        }
        ValueStruct v = (ValueStruct) baseVal;
        v.set(fieldName, val);
        return null;
    }
}
