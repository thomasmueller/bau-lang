package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueInt;
import org.bau.runtime.Value.ValueRef;
import org.bau.runtime.Value.ValueStruct;

public class New implements Expression {

	final DataType type;
	Expression arrayLength;

    New(DataType type, Expression arrayLength) {
    	this.type = type;
    	this.arrayLength = arrayLength;
    }

    @Override
    public Value eval(Memory memory) {
        Value result;
        if (type.isArray()) {
            Value len = arrayLength.eval(memory);
            if (len == null) {
                return null;
            }
            int l = len.intValue();
            if (type.baseType().isCopyType()) {
                switch (type.baseType().name()) {
                case DataType.I32:
                    result = new Value.ValueI32Array(l);
                    break;
                case DataType.I8:
                    result = new Value.ValueI8Array(new byte[l]);
                    break;
                default:
                    result = new Value.ValueArray(l, ValueInt.ZERO);
                    break;
                }
            } else if (type.baseType().isArray() || type.baseType().isPointer()) {
                result = new Value.ValueArray(l, new Value.ValueRef(0));
            } else {
                result = new Value.ValueArray(l, new Value.ValueStruct());
            }
            long heapId = memory.putHeap(result);
            return new ValueRef(heapId);
        } else {
            ValueStruct struct = new Value.ValueStruct();
            for (Variable f : type.fields) {
                struct.set(f.name, f.type.getZeroValue());
            }
            result = struct;
        }
        if (type.isArray() || type.isPointer()) {
            long heapId = memory.putHeap(result);
            return new ValueRef(heapId);
        } else {
            return result;
        }
    }

    @Override
    public DataType type() {
        return type;
    }

    public Expression replace(Variable old, Expression with) {
        New c = new New(type, arrayLength.replace(old, with));
        return c;
    }

    public DataType canThrowException() {
        return null;
    }

    @Override
    public String toC() {
        if (type.isArray()) {
            StringBuilder buff = new StringBuilder();
            buff.append(type.nameC() + "_new(" + arrayLength.toC() + ")");
            return buff.toString();
        }
        if (type.isPointer()) {
            if (arrayLength == null) {
            	return type.nameC() + "_new()";
            }
        }
        String t = type.toC();
        if (t.endsWith("*")) {
            // TODO hack to get rid of pointers
            t = t.substring(0, t.length() - 1);
        }
        return type.nameC() + "_new()";
    }

    @Override
    public void setOwnedBoundsToNull(Expression scope) {
    }

    public String toString() {
        if (arrayLength != null) {
            return "new " + type.baseType().name() + "[" + arrayLength + "]";
        }
        return "new " + type.name();
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
        return false;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        if (arrayLength != null) {
            arrayLength = arrayLength.writeStatements(parser, false, target);
        }
        return this;
    }

}
