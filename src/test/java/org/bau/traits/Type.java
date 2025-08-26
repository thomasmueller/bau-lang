package org.bau.traits;

import java.util.ArrayList;
import java.util.HashSet;

class Type {

    // name of type
    String name;

    // the traits this type implements
    ArrayList<Trait> traits = new ArrayList<>();

    int totalTraitCount = 0;

    // functions (combinations of all functions of all traits)
    ArrayList<TraitFunction> traitFunctions = new ArrayList<>();

    public void collectAllNonMarkerTraits(HashSet<String> target) {
        for(Trait t : traits) {
            t.collectAllNonMarkerTraits(target);
        }
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("type " + name + " : ");

        int i=0;
        for(Trait t : traits) {
            if (i++ > 0) {
                buff.append(", ");
            }
            buff.append(t.name + "/" + t.functionList.size());
        }
//        buff.append(")");
        return buff.toString();
    }

}