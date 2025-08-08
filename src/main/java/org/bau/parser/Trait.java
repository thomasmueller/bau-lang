package org.bau.parser;

import java.util.ArrayList;

public class Trait {
    private final String module;
    private final String name;
    private final ArrayList<FunctionDefinition> functions = new ArrayList<>();

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
