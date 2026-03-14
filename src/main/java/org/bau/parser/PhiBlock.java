package org.bau.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.bau.runtime.Memory;

/**

# if: original
a := 1; if condition { a += 1 } a += 2
# SSA form
a0: 1; if condition { a1: a0 + 1 } a2: phi(a0, a1); a3: a2 + 1
# C, almost SSO (without phi nodes)
a0: 1; a2 := a0; if condition { a1: a0 + 1; a2 = a1 } a3: a2 + 1

# loop: original
a := 1; loop condition { a += 1 } a += 2
# SSA form
a0: 1; loop condition { a1: phi(a0, a2); a2: a1 + 1 } a3: phi(a0, a3); a4: a3 + 2
# C, almost SSO (without phi nodes)
a0: 1; a3 := a0; a1 := a0; while condition { a2: a1 + 1; a1 = a3 = a2 } a4: a3 + 2

 */
public class PhiBlock implements Statement {

    HashMap<String, Integer> current = new HashMap<>();
    HashMap<String, Integer> latest = new HashMap<>();
    HashMap<String, List<Integer>> versions = new HashMap<>();

    private BasicBlock basicBlock;

    @Override
    public Statement replace(Variable old, Expression with) {
        return this;
    }

    @Override
    public StatementResult run(Memory m) {
        return StatementResult.OK;
    }

    @Override
    public void optimize(ProgramContext context) {
    }

    @Override
    public String toC() {
        if (basicBlock != null) {
            return basicBlock.toC();
        }
        if (Variable.DEBUG_VERSIONS) {
            // Original SSA1 debug output
            for (String n : versions.keySet()) {
                TreeSet<Integer> set = new TreeSet<>();
                set.addAll(versions.get(n));
                versions.put(n, new ArrayList<Integer>(set));
            }
            if (current.size() != 0 || versions.size() != 0 || latest.size() != 0) {
                StringBuilder buff = new StringBuilder();
                buff.append("/*\n");
                buff.append("current " + current).append("\n");
                buff.append("phis " + versions).append("\n");
                buff.append("latest " + latest).append("\n");
                buff.append("*/\n");
                return buff.toString();
            } else {
                return "/* empty phis */\n";
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
    }

    @Override
    public void used(Program program) {
    }

    public void setCurrentVersion(String name, int version) {
        current.put(name, version);
    }

    public Integer getCurrentVersion(String name) {
        return current.get(name);
    }

    public void add(String name) {
        versions.put(name, new ArrayList<Integer>());
        current.put(name, -1);
        latest.put(name, -1);
    }

    public List<Integer> getVersionList(String name) {
        return versions.get(name);
    }

    public void setLatestVersion(String name, int version) {
        latest.put(name, version);
    }

    public Integer getLatestVersion(String name) {
        return latest.get(name);
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

}
