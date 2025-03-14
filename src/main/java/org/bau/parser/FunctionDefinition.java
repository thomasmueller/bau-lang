package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

public class FunctionDefinition {
    public ArrayList<Statement> list = new ArrayList<>();
    List<Statement> autoClose;
    public ArrayList<Variable> parameters = new ArrayList<>();
    boolean builtIn;

    // functions with a type have a "this" parameter
    public DataType callType;
    public String module;
    public String name;
    public DataType returnType;
    public DataType exceptionType;
    boolean used;
    public ArrayList<String> includes;
    public String cCode;
    public boolean varArgs;
    public boolean constExpr;
    public boolean macro;
    public String template;
    public String header;
    public String code;
    public String comment;

    private String catchLabel;

    public String getFunctionId() {
        int parameterCount = varArgs ? Integer.MAX_VALUE : parameters.size();
        return getFunctionId(callType, module, name, parameterCount);
    }

    public static String getFunctionId(DataType type, String module, String name, int parameterCount) {
        if (module != null && type != null && type.getModule() != null && !type.getModule().equals(module)) {
            throw new IllegalStateException("Module does not match type module");
        }
        StringBuilder buff = new StringBuilder();
        if (type != null) {
            buff.append(type.fullName()).append(' ');
        } else if (module != null) {
            buff.append(module).append(' ');
        }
        buff.append(name).append(' ').append(parameterCount);
        return buff.toString();
    }

    public String headerToC() {
        StringBuilder buff = new StringBuilder();
        if (builtIn) {
            return "";
        }
        if (exceptionType != null) {
            buff.append(getExceptionStruct());
        } else if (returnType == null) {
            buff.append("void");
        } else {
            buff.append(returnType.toC());
        }
        buff.append(' ');
        if (module != null) {
            buff.append(module.replace(".", "_") + "_");
        }
        if (callType != null) {
            buff.append(callType.idC()).append('_');
        }
        buff.append(name + "_");
        if (varArgs) {
            buff.append("var");
        } else {
            buff.append(parameters.size());
        }
        buff.append('(');
        int i = 0;
        for (Variable v : parameters) {
            if (i++ > 0) {
                buff.append(", ");
            }
            if (varArgs && i == parameters.size()) {
                buff.append("int _vaCount,...");
                break;
            } else {
                buff.append(v.type.toC());
                buff.append(' ');
                buff.append(v.name);
            }
        }
        buff.append(")");
        return buff.toString();
    }

    public String declarationToC() {
        String header = headerToC();
        if (header.isEmpty()) {
            return header;
        }
        return header + ";\n";
    }

    public void optimize(ProgramContext context) {
        if (builtIn) {
            return;
        }
        for (Statement s : list) {
            s.optimize(context);
        }
        if (context.needToCatch != null) {
            if (exceptionType != context.needToCatch) {
                throw new IllegalStateException("Possible exception is not caught in " + toString());
            }
            catchLabel = "catch" + context.nextCatchLabel;
        }
        if (autoClose != null) {
            for(Statement s : autoClose) {
                s.optimize(context);
            }
        }
    }

    public String toC(ProgramContext context) {
        if (builtIn) {
            return "";
        }
        StringBuilder buff = new StringBuilder();
        buff.append(headerToC());
        buff.append(" {\n");
        if (cCode != null) {
            buff.append(Statement.indent(cCode));
        }
        if (varArgs) {
            buff.append(Statement.indent("va_list _vaList;\n"));
            Variable v = parameters.get(parameters.size() - 1);
            buff.append(Statement.indent(v.type().nameC() + "* " + v.name + " = " + v.type().nameC() + "_new(_vaCount);\n"));
            buff.append(Statement.indent("va_start(_vaList, _vaCount);\n"));
            buff.append(Statement.indent("for (int _vaI = 0; _vaI < _vaCount; _vaI++) {\n"));
            buff.append("    " + Statement.indent(v.name + "->data[_vaI] = va_arg(_vaList, "+ v.type().baseType().toC() +");\n"));
            buff.append(Statement.indent("}\n"));
            buff.append(Statement.indent("va_end(_vaList);\n"));
        }
        StringBuilder buff2 = new StringBuilder();
        boolean hasReturn = false;
        for (Statement s : list) {
            if (s instanceof Return) {
                hasReturn = true;
            }
            buff2.append(Statement.indent(s.toC()));
        }
        if (!context.delareList.isEmpty()) {
            for (String s : context.delareList) {
                buff.append(Statement.indent(s + "\n"));
            }
        }
        if (context.needToCatch != null) {
            buff2.append(Statement.indent(catchLabel + ":\n"));
            buff2.append(Statement.indent("return exception" + context.function.getExceptionStruct() + "(_lastException);\n"));
        }
        buff.append(buff2.toString());
        if (!hasReturn && autoClose != null) {
            for(Statement s : autoClose) {
                buff.append(Statement.indent(s.toC()));
            }
        }
        buff.append("}\n");
        return buff.toString();
    }

    public String getExceptionStruct() {
        if (exceptionType == null) {
            return null;
        }
        StringBuilder buff = new StringBuilder();
        if (returnType != null) {
            buff.append("_" + returnType.nameC());
        }
        buff.append("_or_");
        buff.append(exceptionType);
        return buff.toString();
    }

    public void autoClose(List<Statement> autoClose) {
        this.autoClose = autoClose;
    }

    public String toString() {
        if (header != null && code != null) {
            return header + code;
        }
        StringBuilder buff = new StringBuilder();
        if (comment != null) {
            buff.append("##\n");
            buff.append(comment);
            buff.append("\n##\n");
        }
        buff.append("fun ");
        if (callType != null) {
            buff.append(callType).append(' ');
        }
        buff.append(name);
        buff.append('(');
        int first = callType == null ? 0 : 1;
        for (int i = first; i < parameters.size(); i++) {
            Variable v = parameters.get(i);
            if (i > first) {
                buff.append(", ");
            }
            buff.append(v.name);
            buff.append(' ');
            if (varArgs && i == parameters.size() - 1) {
                buff.append(v.type.baseType());
                buff.append("..");
            } else {
                buff.append(v.type);
            }
        }
        buff.append(")");
        if (constExpr) {
            buff.append(" const");
        }
        if (returnType != null) {
            buff.append(' ');
            buff.append(returnType);
        }
        if (exceptionType != null) {
            buff.append(" throws ");
            buff.append(exceptionType);
        }
        if (code != null) {
            buff.append("\n");
            buff.append(code);
        }
        return buff.toString();
    }

}
