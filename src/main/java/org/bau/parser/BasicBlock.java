package org.bau.parser;

import java.util.ArrayList;
import java.util.HashMap;

// basic block, with one entry, and one exit
// (but possibly multiple predecessors and successors)
public class BasicBlock {
    int id;
    boolean processed;
    ArrayList<BasicBlock> predecessors = new ArrayList<>();
    ArrayList<BasicBlock> successors = new ArrayList<>();
    ArrayList<Statement> list = new ArrayList<>();
    HashMap<String, Integer> localVersions = new HashMap<>();
    HashMap<String, Integer> phiVersions = new HashMap<>();
    HashMap<String, ArrayList<Integer>> phiSources = new HashMap<>();

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
        for(String n : localVersions.keySet()) {
            buff.append("local " + n + "_" + localVersions.get(n) + "\n");
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
        System.out.println("process variables in block #" + id);
        for (Statement s : list) {
            s.setVariableVersions(functionContext, this);
        }
        System.out.println("process variables in block #" + id + " done: " + localVersions.toString());
        processed = true;
    }

    public void setVariableVersion(String name, int v) {
        System.out.println("      block #" + id + " " + name + " =" + v);
        localVersions.put(name, v);
    }

    public int getVersion(FunctionContext functionContext, String name) {
        Integer v = localVersions.get(name);
        if (v != null) {
            System.out.println("    getV of #" + id + " for " + name + " = " + v);
            return v;
        }
        if (predecessors.size() == 1) {
            return predecessors.get(0).getVersion(functionContext, name);
        }
        v = functionContext.nextVariableVersion(name);
        localVersions.put(name, v);
        phiVersions.put(name, v);
        phiSources.put(name, new ArrayList<>());
        return v;
    }

    public void fillPhis() {
        for (String n : phiVersions.keySet()) {
            ArrayList<Integer> list = phiSources.get(n);
            if (list.size() > 0) {
                continue;
            }
            for (BasicBlock b : predecessors) {
                b.fillPhiVersionsRecursive(n, list, 0);
            }
        }
    }

    private void fillPhiVersionsRecursive(String n, ArrayList<Integer> list, int depth) {
        if (depth > 20) {
            System.out.println("?");
        }
        Integer v = localVersions.get(n);
        if (v != null) {
            if (v >= 0 && !list.contains(v)) {
                list.add(v);
            }
        } else {
            // prevent endless recursion
            localVersions.put(n, -1);
            for (BasicBlock b : predecessors) {
                b.fillPhiVersionsRecursive(n, list, depth + 1);
            }
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
