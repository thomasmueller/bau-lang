package org.bau.traits.java;

import java.util.ArrayList;
import java.util.HashSet;

class TestType {

    // name of type
    String name;

    // the traits this type implements
    ArrayList<TestTrait> traits = new ArrayList<>();

    int totalTraitCount = 0;

    // functions (combinations of all functions of all traits)
    ArrayList<TestTraitFunction> traitFunctions = new ArrayList<>();

    public void collectAllNonMarkerTraits(HashSet<String> target) {
        for(TestTrait t : traits) {
            t.collectAllNonMarkerTraits(target);
        }
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("type " + name + " : ");

        int i=0;
        for(TestTrait t : traits) {
            if (i++ > 0) {
                buff.append(", ");
            }
            buff.append(t.name + "/" + t.functionList.size());
        }
//        buff.append(")");
        return buff.toString();
    }

}