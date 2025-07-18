package org.bau.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class FunctionContext {

    private final Program program;
    private final ArrayList<String> identifierStack = new ArrayList<>();
    private final ArrayList<Integer> identifierCloseCount = new ArrayList<>();
    private final HashMap<String, Variable> variables = new HashMap<>();
    private final LinkedHashMap<String, DataType> dataTypeMap = new LinkedHashMap<String, DataType>();
    private int nextTempVariableId;
    private String currentFunctionName;

    public FunctionContext(Program program, String functionName) {
        this.program = program;
        this.currentFunctionName = functionName;
    }

    public int nextTempVariableId() {
        if ("main".equals(currentFunctionName)) {
            return program.nextTempVariableIdGlobalScope();
        }
        return nextTempVariableId++;
    }

    public void reset(String functionName) {
        currentFunctionName = functionName;
        nextTempVariableId = 0;
    }

    /**
     * Get the stack position (for identifiers etc)
     */
    public int getStackPos() {
        return identifierStack.size();
    }

    /**
     * Get the number of auto-close identifiers between the current stack and the old stack position
     */
    public int getStackCloseDifference(int oldStack) {
        if (oldStack >= identifierCloseCount.size()) {
            return 0;
        }
        int oldCount = oldStack == 0 ? 0 : identifierCloseCount.get(oldStack - 1);
        int newCount = identifierCloseCount.get(identifierCloseCount.size() - 1);
        return newCount - oldCount;
    }

    public void addIdentifier(String name, DataType type) {
        // TODO this is an assertion
        if (identifierStack.contains(name)) {
            throw new IllegalStateException();
        }
        identifierStack.add(name);
        int offset = (type.needFree()) ? 1 : 0;
        int now = identifierCloseCount.isEmpty() ? 0 : identifierCloseCount.get(identifierCloseCount.size() - 1);
        identifierCloseCount.add(now + offset);
    }

    /**
     * Get the list of variables that are new since the given stack position
     */
    public ArrayList<String> newVariablesList(int stackPos) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = stackPos; i < identifierStack.size(); i++) {
            String id = identifierStack.get(i);
            if (variables.containsKey(id)) {
                list.add(id);
            } else if (dataTypeMap.containsKey(id)) {
                // types don't need to be freed
            } else {
                throw new IllegalStateException("Id not found: " + id);
            }
        }
        return list;
    }

    /**
     * Rewind the stack to the target position
     */
    public void rewindStack(int stackPos) {
        while (identifierStack.size() > stackPos) {
            String id = identifierStack.remove(identifierStack.size() - 1);
            identifierCloseCount.remove(identifierCloseCount.size() - 1);
            if (variables.containsKey(id)) {
                variables.remove(id);
            } else if (dataTypeMap.containsKey(id)) {
                dataTypeMap.remove(id);
            } else {
                throw new IllegalStateException("Id not found: " + id);
            }
        }
    }

    public void addVariable(Variable var) {
        String name = var.name;
        if (variables.containsKey(name)) {
            throw new IllegalStateException("Variable already exists: " + name);
        }
        variables.put(name, var);
        addIdentifier(name, var.type());
    }

    public void addTemporaryType(DataType type) {
        addType(type);
        addIdentifier(type.fullName(), type);
        if (!type.isRange()) {
            // no need for range types
            addIdentifier(type.arrayType().fullName(), type.arrayType());
        }
    }

    public FunctionDefinition getFunctionIfExists(String module, String name) {
        Variable var = variables.get(name);
        if (var == null) {
            var = program.getGlobalVariable(module, name);
        }
        if (var != null && "fun".equals(var.type().name())) {
            FunctionDefinition def = new FunctionDefinition(0);
            def.isFunctionPointer = true;
            def.name = name;
            def.returnType = var.type().functionPointerReturnType;
            int paramId = 0;
            for (DataType pt : var.type().functionPointerArgs) {
                Variable v = new Variable("p" + paramId++, pt);
                def.parameters.add(v);
            }
            return def;
        }
        for (int i = 0; i < 10; i++) {
            FunctionDefinition def = program.getFunctionIfExists(null, module, name, i);
            if (def != null) {
                return def;
            }
        }
        return null;
    }

    public FunctionDefinition getFunctionIfExists(DataType type, FunctionDefinition calledFrom, String module, String name, int parameterCount) {
        if (type == null && module == null) {
            Variable var = variables.get(name);
            if (var != null && "fun".equals(var.type().name())) {
                FunctionDefinition def = new FunctionDefinition(0);
                def.isFunctionPointer = true;
                def.name = name;
                def.returnType = var.type().functionPointerReturnType;
                int paramId = 0;
                for (DataType pt : var.type().functionPointerArgs) {
                    Variable v = new Variable("p" + paramId++, pt);
                    def.parameters.add(v);
                }
                return def;
            }
        }
        return program.getFunctionIfExists(type, calledFrom, module, name, parameterCount);
    }

    public Variable getVariable(String module, String name) {
        Variable var = null;
        var = variables.get(name);
        if (var == null) {
            var = program.getGlobalVariable(module, name);
        }
        return var;
    }

    private DataType addType(DataType type) {
        if (dataTypeMap.containsKey(type.fullName())) {
            throw new IllegalStateException("Type already exists: " + type.fullName());
        }
        dataTypeMap.put(type.fullName(), type);
        if (!type.isArray()) {
            dataTypeMap.put(type.arrayType().fullName(), type.arrayType());
        }
        return type;
    }

    public DataType getType(String module, String name) {
        String fullName = DataType.fullName(module, name);
        DataType t = dataTypeMap.get(fullName);
        if (t == null && module != null) {
            t = dataTypeMap.get(name);
        }
        if (t == null) {
            t = program.getType(module, name);
        }
        return t;
    }

}
