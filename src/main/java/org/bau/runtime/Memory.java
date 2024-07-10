package org.bau.runtime;

import java.util.ArrayList;
import java.util.HashMap;

import org.bau.parser.FunctionDefinition;

public class Memory {

    public static final String RESULT = "_result";
    public static final String EXCEPTION = "_exception";
    public static final String PANIC = "_panic";

    private StringBuilder output = new StringBuilder();
    private HashMap<Long, Value> constants = new HashMap<>();
    private HashMap<String, Value> globalVariables = new HashMap<>();
    private ArrayList<HashMap<String, Value>> stack = new ArrayList<>();
    private HashMap<String, Value> localVariables = new HashMap<>();
    private HashMap<String, FunctionDefinition> functions = new HashMap<>();

    private boolean evaluateOnlyConstExpr;
    private long ticksRemaining;

    public void addFunction(String name, FunctionDefinition function) {
        functions.put(name, function);
    }

    public FunctionDefinition getFunction(String name) {
        return functions.get(name);
    }

    public Value getGlobal(String variable) {
        Value v = globalVariables.get(variable);
        if (v == null) {
            return null;
        }
        return v;
    }

    public void setGlobal(String variable, Value value) {
        globalVariables.put(variable, value);
    }

    public void saveLocal() {
        stack.add(localVariables);
        localVariables = new HashMap<>();
    }

    public void restoreLocal() {
        localVariables = stack.remove(stack.size() - 1);
    }

    public Value getLocal(String variable) {
        Value v = localVariables.get(variable);
        if (v == null) {
            return null;
        }
        return v;
    }

    public void setLocal(String variable, Value value) {
        localVariables.put(variable, value);
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
        if (v == null) {
            throw new IllegalStateException();
        }
        output.append(v.print());
    }

    public void println() {
        output.append('\n');
    }

    public boolean evaluateOnlyConstExpr() {
        return evaluateOnlyConstExpr;
    }

    public void evaluateOnlyConstExpr(boolean value, long ticksRemaining) {
        this.evaluateOnlyConstExpr = value;
        this.ticksRemaining = ticksRemaining;
    }

    public boolean tick() {
        if (ticksRemaining == 0) {
            return false;
        }
        if (ticksRemaining == 1) {
            return true;
        }
        ticksRemaining--;
        return false;
    }

}
