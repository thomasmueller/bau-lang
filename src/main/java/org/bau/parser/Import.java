package org.bau.parser;

import java.util.ArrayList;

import org.bau.parser.expr.Variable;

public class Import implements Section {

    private final String moduleName;
    private final String moduleIdentifier;
    private ArrayList<Variable> symbols = new ArrayList<>();

    public Import(String name, String id) {
        this.moduleName = name;
        this.moduleIdentifier = id;
    }

    public void setLocation(SourceFile sourceFile, int location) {
        sourceFile.setLocation(location, this);
    }

    public void addSymbol(String entry, int location) {
        Variable var = new Variable(moduleName, entry, false, DataType.UNKNOWN);
        var.setLocation(moduleName, location);
        symbols.add(var);
    }

    public String getModuleName() {
        return moduleName;
    }

    public String formatSource() {
        StringBuilder buff = new StringBuilder();
        buff.append("import ");
        buff.append(moduleName);
        if (!moduleName.endsWith("." + moduleIdentifier)) {
            buff.append(": ");
            buff.append(moduleIdentifier);
        }
        if (!symbols.isEmpty()) {
            for (Variable var : symbols) {
                buff.append("\n    " + var.name());
            }
        }
        return buff.toString();
    }

}
