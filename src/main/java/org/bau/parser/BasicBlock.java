package org.bau.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
        if (!localVersions.isEmpty()) {
            buff.append("; local " + localVersions);
        }
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

    public void collectTrivialPhis(HashMap<String, int[]> map) {
        for (String n : phiVersions.keySet()) {
            HashSet<Integer> set = phiSources.get(n);
            if (set.size() == 1) {
                map.put(n, new int[] { phiVersions.get(n), set.iterator().next() });
            }
        }
    }

    public void collectPhis(HashMap<Variable, Set<Integer>> map) {
        for (String n : phiVersions.keySet()) {
            Variable v = new Variable(n, null);
            v.setVersion(phiVersions.get(n));
            map.put(v, phiSources.get(n));
        }
    }

    public void removeTrivialPhis(HashMap<String, int[]> map) {
        for (String n : map.keySet()) {
            int[] fromTo = map.get(n);
            if (phiVersions.containsKey(n)) {
                int old = phiVersions.get(n);
                if (old == fromTo[0]) {
                    phiVersions.remove(n);
                    HashSet<Integer> set = phiSources.remove(n);
                    if (set.size() != 1 || set.iterator().next() != fromTo[1]) {
                        throw new IllegalStateException();
                    }
                }
            }
            if (phiSources.containsKey(n)) {
                HashSet<Integer> old = phiSources.get(n);
                if (old.contains(fromTo[0])) {
                    old.remove(fromTo[0]);
                    old.add(fromTo[1]);
                }
                phiSources.get(n).remove(phiVersions.get(n));
            }
            replaceVariableVersion(n, fromTo[0], fromTo[1]);
        }
    }

    void replaceVariableVersion(String n, int oldVersion, int newVersion) {
        if (localVersions.containsKey(n)) {
            int old = localVersions.get(n);
            if (old == oldVersion) {
                localVersions.put(n, newVersion);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            Statement s = list.get(i);
            s.setVariableVersions(n, oldVersion, newVersion);
        }
    }

    public String toC() {
        if (!Variable.DEBUG_VERSIONS) {
            return "";
        }
        StringBuilder buff = new StringBuilder();
        if (!phiVersions.isEmpty()) {
            buff.append("// basic block #" + id + "\n");
            for(String name : phiVersions.keySet()) {
                buff.append("// " + name + "_" + phiVersions.get(name) + " : phi(" + phiSources.get(name) + ")\n");
            }
        }
        return buff.toString();
    }

    Set<Integer> collectOuterVersionsRecursively(
            String varName, Set<BasicBlock> visiting,
            Map<Integer, BasicBlock> versionToPhiBlock) {
        // collect all reachable non-phi versions
        Set<Integer> result = new HashSet<>();
        Set<Integer> sources = phiSources.get(varName);
        if (sources == null) {
            return result;
        }
        for (int srcVersion : sources) {
            BasicBlock srcPhiBlock = versionToPhiBlock.get(srcVersion);
            if (srcPhiBlock == null) {
                // local assign
                result.add(srcVersion);
            } else if (!visiting.contains(srcPhiBlock)) {
                // not yet on the stack: follow
                visiting.add(srcPhiBlock);
                result.addAll(
                        srcPhiBlock.collectOuterVersionsRecursively(
                                varName, visiting, versionToPhiBlock));
                visiting.remove(srcPhiBlock);
            }
            // else srcPhiBlock is in visiting; skip
        }
        return result;
    }

}
