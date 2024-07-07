package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class NumberValue implements Expression {
    boolean hex;
    Value value;
    DataType type;
    
    public NumberValue(Value value, DataType type, boolean hex) {
        this.value = value;
        this.type = type;
        this.hex = hex;
    }

    @Override
    public Value eval(Memory memory) {
        return value;
    }
    
    public DataType canThrowException() {
        return null;
    }
    
    public Expression replace(Variable old, Expression with) {
        return this;
    }
    
    @Override
    public DataType type() {
        return type;
    }

    public String toC() {
        return toString();
    }
    
    public String toString() {
        if (hex) {
            return "0x" + Long.toHexString(value.longValue());
        }
        return value.toString();
    }
    
    @Override
    public boolean isEasyToRead() {
        return true;
    }
    
    @Override
    public Bounds getBounds() {
        return new Bounds(this);
    }
    
    @Override
    public Expression simplify() {
        return this;
    }
    
    @Override
    public boolean isSimple() {
        return true;
    }
    
    @Override
    public Expression writeStatements(Parser parser, ArrayList<Statement> target) {
        return this;
    }

    public static long parseUnsignedHexLong(String hex) {
        if (hex.length() < 16) {
            return Long.parseLong(hex, 16);
        } else if (hex.length() > 16) {
            throw new IllegalArgumentException(hex);
        }
        return Long.parseLong(hex.substring(0, 8), 16) << 32 |
                Long.parseLong(hex.substring(8), 16);
    }

}
