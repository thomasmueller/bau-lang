package org.bau.parser;

import java.util.ArrayList;
import java.util.HashSet;
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
    private boolean used;
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
    private HashSet<DataType> freedOwnedTypes = null;
    private HashSet<DataType> borrowedTypes = null;
    final int lineOffset;
    public boolean isConstructor;
    public boolean isFunctionPointer;

    public FunctionDefinition(int lineOffset) {
        this.lineOffset = lineOffset;
    }

    public String getFunctionId() {
        int parameterCount = varArgs ? Integer.MAX_VALUE : parameters.size();
        return getFunctionId(callType, module, name, parameterCount);
    }

    public static String getFunctionId(DataType type, String module, String name, int parameterCount) {
        if (module != null && type != null && type.getModule() != null && !type.getModule().equals(module)) {
            // throw new IllegalStateException("Module does not match the module of the type");
            return null;
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
            buff.append(Program.esc(module).replace(".", "_") + "_");
        }
        if (callType != null) {
            buff.append(callType.idC()).append('_');
        }
        buff.append(nameC() + "_");
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
                buff.append(v.declarationToC());
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
            for (Statement s : autoClose) {
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
            buff.append(Statement.indent(v.type().nameC() + "* " + v.nameC() + " = " + v.type().nameC() + "_new(_vaCount);\n"));
            buff.append(Statement.indent("va_start(_vaList, _vaCount);\n"));
            buff.append(Statement.indent("for (int _vaI = 0; _vaI < _vaCount; _vaI++) {\n"));
            if (v.type().baseType().isNumber() && v.type().baseType().sizeOf() <= 1) {
                buff.append("    " + Statement.indent(v.nameC() + "->data[_vaI] = (" + v.type().baseType().toC() + ") va_arg(_vaList, int);\n"));
            } else {
                buff.append("    " + Statement.indent(v.nameC() + "->data[_vaI] = va_arg(_vaList, " + v.type().baseType().toC() + ");\n"));
            }
            buff.append(Statement.indent("}\n"));
            buff.append(Statement.indent("va_end(_vaList);\n"));
        }
        if (!isConstructor) {
            for (int i = 0; i < parameters.size(); i++) {
                if (varArgs && i == parameters.size() - 1) {
                    // no need to increment varargs array
                    continue;
                }
                Variable v = parameters.get(i);
                buff.append(Statement.indent(v.incrementRefCountC()));
            }
        }
        StringBuilder buff2 = new StringBuilder();
        boolean hasReturn = Program.hasReturn(list);
        boolean hasCatch = Program.hasCatch(list);
        if (hasCatch) {
            buff2.append(Statement.indent("do { do {\n"));
        }
        for (Statement s : list) {
            buff2.append(Statement.indent(s.toC()));
        }
        if (!context.delareList.isEmpty()) {
            for (String s : context.delareList) {
                buff.append(Statement.indent(s + "\n"));
            }
        }
        if (context.needToCatch != null) {
            buff.append(Statement.indent("do {\n"));
            buff2.append(Statement.indent("} while(0);\n"));
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
                buff.append(v.type().baseType());
                buff.append("..");
            } else {
                buff.append(v.type());
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

    boolean isOwnedTypeFreed(DataType type) {
        if (freedOwnedTypes == null) {
            collectTypes(new HashSet<>(), MemoryType.OWNER);
        }
        return freedOwnedTypes.contains(type);
    }

    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        if (memoryType == MemoryType.OWNER) {
            if (freedOwnedTypes == null) {
                freedOwnedTypes = new HashSet<>();
                Program.collectTypes(list, freedOwnedTypes, memoryType);
                Program.collectTypes(autoClose, freedOwnedTypes, memoryType);
            }
            set.addAll(freedOwnedTypes);
        } else if (memoryType == MemoryType.BORROW) {
            if (borrowedTypes == null) {
                borrowedTypes = new HashSet<>();
                Program.collectTypes(list, borrowedTypes, memoryType);
                Program.collectTypes(autoClose, borrowedTypes, memoryType);
            }
            set.addAll(borrowedTypes);
        }
    }

    void borrowCheck() {
        collectTypes(new HashSet<>(), MemoryType.OWNER);
        collectTypes(new HashSet<>(), MemoryType.BORROW);
        for (DataType type : borrowedTypes) {
            DataType ownerType = type.refCountBaseType().ownerType();
            if (freedOwnedTypes.contains(ownerType)) {
                throw new IllegalStateException(
                        "Function " + getFunctionId() + ": borrowing " + type + " which is freed");
            }
        }
    }

    boolean isUsed() {
        return used;
    }

    void used(Program program) {
        if (used) {
            return;
        }
        used = true;
        if (macro) {
            throw new IllegalStateException();
        }
        if (template != null) {
            throw new IllegalStateException();
        }
        this.used = true;
        if (list != null) {
            for (Statement s : list) {
                s.used(program);
            }
        }
        if (autoClose != null) {
            for (Statement s : autoClose) {
                s.used(program);
            }
        }
        for (Variable var : parameters) {
            var.type().used(program);
        }
        if (callType != null) {
            callType.used(program);
        }
        if (returnType != null) {
            returnType.used(program);
        }
        if (exceptionType != null) {
            exceptionType.used(program);
        }
    }

    public String nameC() {
        return name;
    }

}
