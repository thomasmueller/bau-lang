package org.bau.traits;

import java.util.ArrayList;
import java.util.List;

class Trait {

    // unique id (not really needed)
    final int id;

    // trait name
    final String name;

    // list of functions
    final ArrayList<TraitFunction> functionList = new ArrayList<>();

    // which types use it
    final ArrayList<Type> usedIn = new ArrayList<>();

    // two traits in the same type may not occupy the same slot
    int slot = -1;

    public Trait(int id, String name, List<TraitFunction> l2) {
        this.id = id;
        this.name = name;
        this.functionList.addAll(l2);
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("trait slot=" + slot + " name=" + name + " methods=" + functionList.size() + " (");
        int i=0;
        for(TraitFunction f : functionList) {
            if (i++ > 0) {
                buff.append(", ");
            }
            buff.append(f.name);
        }
        buff.append(") id=" + id);
        return buff.toString();
    }

}