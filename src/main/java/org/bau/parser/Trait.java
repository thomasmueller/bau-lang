package org.bau.parser;

import java.util.ArrayList;

public class Trait {
    private final String module;
    private final String name;
    final ArrayList<FunctionDefinition> functions = new ArrayList<>();
    public final ArrayList<String> required = new ArrayList<>();
    private int slot;

    public Trait(String module, String name) {
        this.module = module;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getModule() {
        return module;
    }

}
