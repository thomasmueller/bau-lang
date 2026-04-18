package org.bau.parser;

import java.util.ArrayList;

import org.bau.parser.expr.Variable;

public class Import {

    private final String moduleName;
    private final String moduleIdentifier;
    private int fileId;
    private ArrayList<Variable> symbols = new ArrayList<>();

    public Import(String name, String id) {
        this.moduleName = name;
        this.moduleIdentifier = id;
    }

    public void setLocation(Program program, String module, int fileId, int location) {
        this.fileId = fileId;
        program.setLocation(module, location, this);
    }

    public void addSymbol(String entry, int location) {
        Variable var = new Variable(entry, DataType.UNKNOWN);
        var.setLocation(fileId, location);
        symbols.add(var);
    }

    public String format() {
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
