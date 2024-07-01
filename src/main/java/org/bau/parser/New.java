package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class New implements Expression {
    
	final DataType type;
	Expression arrayLength;
    
    New(DataType type, Expression arrayLength) {
    	this.type = type;
    	this.arrayLength = arrayLength;
    }

    @Override
    public Value eval(Memory memory) {
        return null;
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
        if (type.valueType) {
            if (arrayLength == null) {
            	return type.nameC() + "_new()";
            }
        }
        String t = type.baseType().toC();
        if (t.endsWith("*")) {
            // TODO hack to get rid of pointers
            t = t.substring(0, t.length() - 1);
        }
        return type.nameC() + "_new()";
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
    public Expression writeStatements(Parser parser, ArrayList<Statement> target) {
        if (arrayLength != null) {
            arrayLength = arrayLength.writeStatements(parser, target);
        }
        return this;
    }

}
