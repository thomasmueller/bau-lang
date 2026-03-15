package org.bau.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// basic block, with one entry, and one exit
// (but possibly multiple predecessors and successors)
public class BasicBlock {
    int id;
    boolean processed;
    ArrayList<BasicBlock> predecessors = new ArrayList<>();
    ArrayList<BasicBlock> successors = new ArrayList<>();
    ArrayList<Statement> list = new ArrayList<>();
    String recursing;
    HashMap<String, Integer> localVersions = new HashMap<>();
    HashMap<String, Integer> phiVersions = new HashMap<>();
    HashMap<String, HashSet<Integer>> phiSources = new HashMap<>();

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("Basic block #" + id);
        if (successors.size() > 0) {
            buff.append("; successors: ");
            for (int i = 0; i < successors.size(); i++) {
                if (i > 0) {
                    buff.append(", ");
                }
                buff.append(successors.get(i).id);
            }
        }
        if (predecessors.size() > 0) {
            buff.append("; predecessors: ");
            for (int i = 0; i < predecessors.size(); i++) {
                if (i > 0) {
                    buff.append(", ");
                }
                buff.append(predecessors.get(i).id);
            }
        }
        buff.append(":\n");
        if (!phiVersions.isEmpty()) {
            for(String name : phiVersions.keySet()) {
                buff.append(name + "_" + phiVersions.get(name) + " : " + phiSources.get(name) + "\n");
            }
        }
        for (Statement s : list) {
            String st = s.toString();
            if (st.indexOf('\n') >= 0) {
                st = st.substring(0, st.indexOf('\n'));
            }
            buff.append("  " + st).append('\n');
        }
        return buff.toString();
    }

    public void addSuccessor(BasicBlock s) {
        successors.add(s);
        s.predecessors.add(this);
    }

    public void setVariableVersions(FunctionContext functionContext) {
        if (processed) {
            return;
        }
        for (Statement s : list) {
            s.setVariableVersions(functionContext, this);
        }
        processed = true;
    }

    public void setVariableVersion(String name, int v) {
        localVersions.put(name, v);
    }

    public int getVersion(FunctionContext functionContext, String name) {
        Integer v = localVersions.get(name);
        if (v != null) {
            return v;
        }
        if (predecessors.size() == 1) {
            return predecessors.get(0).getVersion(functionContext, name);
        }
        v = functionContext.nextVariableVersion(name);
        localVersions.put(name, v);
        phiVersions.put(name, v);
        phiSources.put(name, new HashSet<>());
        return v;
    }

    public void fillPhis() {
        if (predecessors.isEmpty()) {
            // orphaned - we can ignore
            return;
        }
        for (String n : phiVersions.keySet()) {
            HashSet<Integer> set = phiSources.get(n);
            if (set.size() > 0) {
                continue;
            }
            for (BasicBlock b : predecessors) {
                set.addAll(b.fillPhiVersionsRecursive(n, 0));
            }
            set.remove(phiVersions.get(n));
            if (set.size() == 0) {
                throw new IllegalStateException();
            }
        }
    }

    private Set<Integer> fillPhiVersionsRecursive(String n, int depth) {
        if (depth >= 10000) {
            throw new IllegalStateException();
        }
        Integer v = localVersions.get(n);
        if (v != null) {
            return Set.of(v);
        } else {
            if (n.equals(recursing)) {
                return Set.of();
            }
            recursing = n;
            HashSet<Integer> s = new HashSet<>();
            for (BasicBlock b : predecessors) {
                s.addAll(b.fillPhiVersionsRecursive(n, depth + 1));
            }
            recursing = null;
            return s;
        }
    }

    public String toC() {
        StringBuilder buff = new StringBuilder();
        if (!phiVersions.isEmpty()) {
            buff.append("// basic block #" + id + "\n");
            for(String name : phiVersions.keySet()) {
                buff.append("// " + name + "_" + phiVersions.get(name) + " : phi(" + phiSources.get(name) + ")\n");
            }
        }
        return buff.toString();
    }
}
