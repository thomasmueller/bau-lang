package org.bau.traits;

class TraitFunction {
    final int id;
    Trait trait;
    final String name;

    TraitFunction(int id, String name, Trait trait) {
        this.id = id;
        this.name = name;
        this.trait = trait;
    }

    public String toString() {
        return name + ":" + trait.id + ":" + id;
    }

}