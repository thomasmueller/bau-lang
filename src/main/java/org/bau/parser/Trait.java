package org.bau.parser;

import java.util.ArrayList;

public class Trait {
    private final FullName fullName;
    final ArrayList<FunctionDefinition> functions = new ArrayList<>();
    public final ArrayList<FullName> requiredTraitNames = new ArrayList<>();
    private int slot;

    public Trait(FullName fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return fullName.name;
    }

    public String getModule() {
        return fullName.module;
    }

}
