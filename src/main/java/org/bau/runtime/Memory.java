package org.bau.runtime;

import java.util.HashMap;

import org.bau.parser.Expression;

public class Memory {
    
    public static final String RESULT = "_result";
    
    private StringBuilder output = new StringBuilder();
    private HashMap<Long, Value> constants = new HashMap<>();
    private HashMap<String, Value> variables = new HashMap<>();

    public Value get(String variable) {
        Value v = variables.get(variable);
        if (v == null) {
            throw new IllegalStateException("Not initialized");
        }
        return v;
    }

    public void set(String variable, Expression arrayIndex, String modify, Value value) {
        if (arrayIndex != null) {
            int index = arrayIndex.eval(this).intValue();
            if (modify != null) {
                Value v = variables.get(variable);
                if (v == null) {
                    throw new IllegalStateException("Not initialized");
                }
                Value old = v.get(index);
                if (old == null) {
                    throw new IllegalStateException("Not initialized");
                }
                switch (modify) {
                case "+":
                    v.set(index, old.longValue() + value.longValue());
                    break;
                case "-":
                    v.set(index, old.longValue() - value.longValue());
                    break;
                default:
                    throw new IllegalStateException();
                }
            } else {
                Value v = variables.get(variable);
                if (v == null) {
                    throw new IllegalStateException("Not initialized");
                }
                v.set(index, value.longValue());
            }
        } else {
            if (modify != null) {
                Value v = variables.get(variable);
                if (v == null) {
                    throw new IllegalStateException("Not initialized");
                }
                switch (modify) {
                case "+":
                    v.set(v.get().longValue() + value.longValue());
                    break;
                case "-":
                    v.set(v.get().longValue() - value.longValue());
                    break;
                default:
                    throw new IllegalStateException();
                }
            } else {
                variables.put(variable, value);
            }
        }
    }
    
    public Value getConstant(Long constantId) {
        return constants.get(constantId);
    }

    public String getOutput() {
        return output.toString();
    }

    public void setConstant(long key, Value value) {
        constants.put(key, value);
    }

    public void print(Value v) {
        output.append(v);
    }

}
