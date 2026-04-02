package org.bau.parser;

import java.util.ArrayList;

public class Trait {

    private final FullName fullName;

    // same as DataType.traitSlot (one is redundant)
    // slot of the trait. most commonly used traits have slot 0, then 1 etc
    // if a data type implements multiple traits, then each of those
    // needs to have a unique slot
    // multiple traits may share the same slot,
    // if they are not implemented by the same type
    private int slot = -1;

    // function id offset for the given trait
    // slot 0 has offset 0 by definition,
    // slot 1 has offset the maximum of all traits with slot 0
    private int functionOffset = -1;

    final ArrayList<FunctionDefinition> functions = new ArrayList<>();
    public final ArrayList<FullName> requiredTraitNames = new ArrayList<>();

    public Trait(FullName fullName) {
        this.fullName = fullName;
    }

    public FullName getFullName() {
        return fullName;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

    public void setFunctionOffset(int functionOffset) {
        this.functionOffset = functionOffset;
    }

    public int getFunctionOffset() {
        return functionOffset;
    }

}
