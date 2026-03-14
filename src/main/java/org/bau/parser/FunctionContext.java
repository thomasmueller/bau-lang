package org.bau.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

public class FunctionContext {

    private final Program program;
    private final ArrayList<String> identifierStack = new ArrayList<>();
    private final ArrayList<Integer> identifierCloseCount = new ArrayList<>();
    private final HashMap<String, Variable> variables = new HashMap<>();
    private final LinkedHashMap<String, DataType> dataTypeMap = new LinkedHashMap<String, DataType>();
    private int nextTempVariableId;
    private String currentFunctionName;

    private HashMap<Statement, ArrayList<Statement>> predecessors = new HashMap<>();
    HashMap<Statement, ArrayList<Statement>> successors = new HashMap<>();
    HashMap<Integer, Integer> statementIdMap = new HashMap<>();

    ArrayList<BasicBlock> blockList = new ArrayList<>();
    HashMap<Statement, BasicBlock> blockMap = new HashMap<>();

    HashMap<String, Integer> variableVersions = new HashMap<>();

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
        String name = var.name();
        if (variables.containsKey(name)) {
            throw new IllegalStateException("Variable already exists: " + name);
        }
        variables.put(name, var);
        addIdentifier(name, var.type());
    }

    public void updateVariable(String name, Variable newVersion) {
        if (variables.remove(name) == null) {
            throw new IllegalStateException("Variable not found: " + name);
        }
        variables.put(name, newVersion);
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
        if (var != null) {
            var = var.cloneVariable();
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
        String fullName = new FullName(module, name).toString();
        DataType t = dataTypeMap.get(fullName);
        if (t == null && module != null) {
            t = dataTypeMap.get(name);
        }
        if (t == null) {
            t = program.getType(module, name);
        }
        return t;
    }

    public void linkStatements(Statement a, Statement b) {
        if (a == null || b == null) {
            return;
        }
        ArrayList<Statement> list = predecessors.get(b);
        if (list == null) {
            list = new ArrayList<Statement>();
            predecessors.put(b, list);
        }
if (list.contains(a)) {
    int test;
    throw new IllegalStateException();
}
        list.add(a);
        list = successors.get(a);
        if (list == null) {
            list = new ArrayList<Statement>();
            successors.put(a, list);
        }
if (list.contains(b)) {
    int test;
    throw new IllegalStateException();
}
        list.add(b);
    }

    public void linkList(List<Statement> list, Statement prev, Statement next, Statement breakTarget, Statement continueTarget) {
        // a:1 <=> b:1
        // a:1 <=> if a>0 => then 1 => / => else 2 => / endif => phi
        // a:1 <=> if a>0 => then 1 => / endif => phi
        // a:1 <=> loop => phiAtStart => then 1 => first / endLoop != phi
        // a:1 <=> loop a>0 => phiAtEnd / => phiAtStart => then 1 => / endLoop => phiAtEnd

        Statement p = prev;
        for (int i = 0; i < list.size(); i++) {
            Statement a = list.get(i);
            Statement n = next;
            if (i > 0) {
                p = list.get(i - 1);
            }
            if (i < list.size() - 1) {
                n = list.get(i + 1);
            }
            a.link(this, p, n, breakTarget, continueTarget);
        }
    }

    public BasicBlock linkBasicBlocks(List<Statement> list, BasicBlock current, BasicBlock breakTarget, BasicBlock continueTarget) {
        if (blockList.isEmpty()) {
            current = newBasicBlock();
        }
        for (Statement s : list) {
            current = s.linkBasicBlocks(this, current, breakTarget, continueTarget);
        }
        return current;
    }

    public BasicBlock linkBasicBlocks(Statement statement, BasicBlock current) {
        if (statement != null) {
            if (statement instanceof PhiBlock) {
                ((PhiBlock) statement).setBasicBlock(current);
            }
            if (statement instanceof Loop) {
                ((Loop) statement).setBasicBlock(current);
            }
            current.list.add(statement);
        }
        return current;
    }

    public void printBasicBlocks() {
        for (BasicBlock b : blockList) {
            System.out.println(b);
        }
    }

    public BasicBlock newBasicBlock() {
        BasicBlock b = new BasicBlock();
        b.id = blockList.size();
        blockList.add(b);
        return b;
    }


    public void setBasicBlocksVariableVersions() {
        for (BasicBlock b : blockList) {
            b.setVariableVersions(this);
        }
        for (BasicBlock b : blockList) {
            b.fillPhis();
        }
    }

    public List<Statement> getPrecedessors(Statement s) {
        ArrayList<Statement> list = predecessors.get(s);
        if (list == null) {
            return List.of();
        }
        return list;
    }

    public void printLinks(String indentation, List<Statement> list) {
        for (int i = 0; i < list.size(); i++) {
            Statement a = list.get(i);
            a.printLinks(indentation, this);
        }
        System.out.println();
    }

    public void printLinks(String indentation, Statement a) {
        List<Statement> sl = successors.get(a);
        List<Statement> pl = predecessors.get(a);
        String txt = a.toString().trim();
        if (txt.indexOf('\n') >= 0) {
            txt = txt.substring(0, txt.indexOf('\n'));
        }
        int count = statementIdMap.computeIfAbsent(System.identityHashCode(a), key -> statementIdMap.size());
        System.out.println("    " + count + ": " + indentation + txt);
        StringBuilder buff = new StringBuilder();
        buff.append("    ");
        if (pl != null) {
            buff.append("prev: ");
            for (int i = 0; i < pl.size(); i++) {
                Statement s = pl.get(i);
                count = statementIdMap.computeIfAbsent(System.identityHashCode(s), key -> statementIdMap.size());
                buff.append(count + " ");
            }
            if (pl != null && pl.size() > 1) {
                if (!(a instanceof PhiBlock)) {
                    buff.append(" NOT A PHI TARGET ");
                    throw new IllegalStateException();
                }
                HashSet<Statement> set = new HashSet<>();
                set.addAll(pl);
                if (pl.size() != set.size()) {
                    buff.append(" duplicate entries ");
                    throw new IllegalStateException();
                }
            }
        }
        if (sl != null) {
            buff.append("next: ");
            for (int i = 0; i < sl.size(); i++) {
                Statement s = sl.get(i);
                count = statementIdMap.computeIfAbsent(System.identityHashCode(s), key -> statementIdMap.size());
                buff.append(count + " ");
            }
            HashSet<Statement> set = new HashSet<>();
            set.addAll(sl);
            if (sl.size() != set.size()) {
                buff.append(" duplicate entries ");
                throw new IllegalStateException();
            }
        }
        System.out.println(buff.toString());
    }

    public void addPhiVersion(Statement start, String name, int version) {
        List<Statement> sl = successors.get(start);
        if (sl == null) {
            return;
        }
        while (sl.size() == 1) {
            start = sl.get(0);
            if (start instanceof PhiBlock) {
                break;
            }
            sl = successors.get(start);
            if (sl == null) {
                return;
            }
        }
        for (Statement s : sl) {
            if (s instanceof PhiBlock) {
                PhiBlock phi = (PhiBlock) s;
                if (phi.getCurrentVersion(name) != null) {
                    phi.getVersionList(name).add(version);
                } else {
                    addPhiVersion(s, name, version);
                }
            } else {
                addPhiVersion(s, name, version);
            }
        }
    }

    public int nextVariableVersion(String name) {
        Integer v = variableVersions.get(name);
        if (v == null) {
            v = 1;
        }
        variableVersions.put(name, v + 1);
        return v;
    }

}
