package org.bau.traits.java;

class TestTraitFunction {
    final int id;
    TestTrait trait;
    final String name;

    TestTraitFunction(int id, String name, TestTrait trait) {
        this.id = id;
        this.name = name;
        this.trait = trait;
    }

    public String toString() {
        return name + ":" + trait.id + ":" + id;
    }

}