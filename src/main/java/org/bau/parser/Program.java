package org.bau.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bau.parser.Statement.StatementResult;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.std.Std;

public class Program {

    private final static boolean TRACE_REF_COUNTS = false;

    ArrayList<Statement> list = new ArrayList<>();

    LinkedHashMap<String, DataType> dataTypeMap = new LinkedHashMap<String, DataType>();

    // string -> reference
    HashMap<String, Long> stringConstants = new HashMap<>();

    // reference -> string
    HashMap<Long, String> stringConstantsMap = new HashMap<>();

    HashMap<String, Variable> variables = new HashMap<>();

    // global variables (or constants) in modules
    LinkedHashMap<String, Variable> globalVariables = new LinkedHashMap<>();

    TreeMap<String, FunctionDefinition> functions = new TreeMap<String, FunctionDefinition>();

    TreeSet<String> includes = new TreeSet<>();

    HashMap<String, FunctionDefinition> functionTemplates = new HashMap<>();

    ArrayList<String> comments = new ArrayList<>();

    // map from module identifier to full module name
    HashMap<String, String> imports = new HashMap<>();

    // map from type / method / constant identifier to module identifier
    HashMap<String, String> importEntries = new HashMap<>();

    List<Statement> autoClose;

    HashMap<String, FunctionDefinition> uncompiledFunctions = new HashMap<>();

    {
        FunctionDefinition f = new FunctionDefinition();
        // TODO move println to std
        f.name = "println";
        f.builtIn = true;
        f.varArgs = true;
        addFunction(f);
        Std.register(this);
    }

    private Map<String, String> modules = new HashMap<>();

    private long ticksExecuted;

    public Program(Map<String, String> modules) {
        this.modules = modules;
    }

    public FunctionDefinition getFunctionTemplate(DataType type, String module, String name) {
        String id = FunctionDefinition.getFunctionId(type, module, name, 0);
        return functionTemplates.get(id);
    }

    public void addFunctionTemplate(DataType type, String module, String name, FunctionDefinition def) {
        String id = FunctionDefinition.getFunctionId(type, module, name, 0);
        functionTemplates.put(id, def);
    }

    /**
     * Add a global variable or constant.
     *
     * @param var the variable
     */
    public void addGlobalVariable(Variable var) {
        String id = Variable.getGlobalVariableId(var.module, var.name);
        globalVariables.put(id, var);
    }

    public Variable getGlobalVariable(String module, String name) {
        String id = Variable.getGlobalVariableId(module, name);
        return globalVariables.get(id);
    }

    public long addStringConstant(String n) {
        Long reference = stringConstants.get(n);
        if (reference == null) {
            reference = 1000L + stringConstants.size();
            stringConstants.put(n, reference);
            stringConstantsMap.put(reference, n);
        }
        return reference;
    }

    public void removeFunction(FunctionDefinition old) {
        String id = old.getFunctionId();
        functions.remove(id);
    }

    public void addFunction(FunctionDefinition def) {
        String id = def.getFunctionId();
        if (functions.containsKey(id)) {
            throw new IllegalStateException("Function already exists: " + id);
        }
        functions.put(id, def);
        if (def.name.equals("close") && def.callType != null && def.callType.isPointer()) {
            def.callType.autoClose = def;
            def.used = true;
        }
    }

    public FunctionDefinition getFunction(DataType type, String module, String name, int parameterCount) {
        FunctionDefinition def = getFunctionIfExists(type, module, name, parameterCount);
        if (def == null) {
            throw new IllegalArgumentException(name);
        }
        return def;
    }

    public FunctionDefinition getFunctionIfExists(DataType type, String module, String name, int parameterCount) {
        if ("println".equals(name)) {
            // TODO support varargs
            parameterCount = 0;
        }
        String id = FunctionDefinition.getFunctionId(type, module, name, parameterCount);
        FunctionDefinition fd = functions.get(id);
        if (fd != null) {
            return fd;
        }
        id = FunctionDefinition.getFunctionId(type, module, name, Integer.MAX_VALUE);
        FunctionDefinition result = functions.get(id);
        if (result == null && module != null) {
            // no method in this module - but the might be a global function
            result = getFunctionIfExists(type, null, name, parameterCount);
        }
        return result;
    }

    public DataType addType(DataType type) {
        if (dataTypeMap.containsKey(type.fullName())) {
            throw new IllegalStateException("Type already exists: " + type.fullName());
        }
        dataTypeMap.put(type.fullName(), type);
        if (!type.isArray()) {
        	dataTypeMap.put(type.arrayType().fullName(), type.arrayType());
        }
        return type;
    }

    public DataType getType2(String module, String name) {
        String fullName = DataType.fullName(module, name);
        DataType t = dataTypeMap.get(fullName);
        if (t == null && module != null) {
            t = dataTypeMap.get(name);
        }
        return t;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        for(Statement s : list) {
            buff.append(s);
        }
        return buff.toString();
    }

    public String toC() {
        ProgramContext context = new ProgramContext();
        StringBuilder buff = new StringBuilder();
        buff.append("#include <stdio.h>\n");
        buff.append("#include <stdlib.h>\n");
        buff.append("#include <stdarg.h>\n");
        buff.append("#include <stdint.h>\n");

        for(FunctionDefinition def : functions.values()) {
            if (def.used && def.includes != null) {
                includes.addAll(def.includes);
            }
        }
        for(String i : includes) {
            buff.append("#include " + i + "\n");
        }
        if (TRACE_REF_COUNTS) {
            buff.append("int __globalObjects = 0;\n");
            buff.append("int __refCountUpdates = 0;\n");
            buff.append("#define _incUse(a) {__refCountUpdates++; printf(\"++  %p line %d, from %d\\n\", a, __LINE__, (a)->_refCount);if(a && (a)->_refCount < INT32_MAX){(a)->_refCount++;}}\n");
            buff.append("#define _decUse(a, type) {__refCountUpdates++; if(a && (a)->_refCount < INT32_MAX){printf(\"--  %p line %d, from %d\\n\", a, __LINE__, (a)->_refCount);if(--((a)->_refCount) == 0) type##_free(a);}}\n");
            buff.append("#define _malloc(a) malloc(a)\n");
            buff.append("#define _traceMalloc(a) printf(\"new %p line %d (%d)\\n\", a, __LINE__, ++__globalObjects);\n");
            buff.append("#define _free(a) {printf(\"del %p line %d (%d)\\n\", a, __LINE__, --__globalObjects);free(a);}\n");
            buff.append("#define _end() {printf(\"refCountUpdates: %d\\n\", __refCountUpdates); if(__globalObjects!=0)printf(\"################ MEMORY LEAK: %d ################\\n\", __globalObjects);}\n");
        } else {
            buff.append("#define _incUse(a) if(a){(a)->_refCount++;}\n");
            buff.append("#define _decUse(a, type) if(a){if(--((a)->_refCount) == 0) type##_free(a);}\n");
            buff.append("#define _malloc(a) malloc(a)\n");
            buff.append("#define _traceMalloc(a) ;\n");
            buff.append("#define _free(a) free(a)\n");
            buff.append("#define _end() ;\n");
        }
        buff.append("/* types */\n");
        for(DataType t : dataTypeMap.values()) {
            if (t.enumValues != null) {
                continue;
            }
            if (!t.isSystem() && t.isUsed()) {
                buff.append("typedef struct " + t.nameC() + " " + t.nameC() + ";\n");
                buff.append("struct ").append(t.nameC()).append(";\n");
            }
        }
        for(DataType t : dataTypeMap.values()) {
            if (t.enumValues != null) {
                continue;
            }
            if (!t.isSystem() && t.isUsed()) {
                buff.append("struct ").append(t.nameC()).append(" {\n");
                if (t.isArray()) {
                    buff.append(Statement.indent("int32_t len;\n"));
                    buff.append(Statement.indent(t.baseType().toC() + "* data;\n"));
                } else {
                    for(Variable f : t.fields) {
                        buff.append(Statement.indent(f.type.toC() + " " + f.name + ";\n"));
                    }
                }
                buff.append(Statement.indent("int32_t _refCount;\n"));
                buff.append("};\n");
                if (t.isArray()) {
                    buff.append(t.nameC() + "* " + t.nameC() + "_new(uint32_t len) {\n");
                    buff.append(Statement.indent(t.nameC() + "* result = _malloc(sizeof(" + t.nameC() + "));\n"));
                    buff.append(Statement.indent("_traceMalloc(result);\n"));
                    buff.append(Statement.indent("result->len = len;\n"));
                    buff.append(Statement.indent("result->data = _malloc(sizeof(" + t.baseType().toC() + ") * len);\n"));
                    buff.append(Statement.indent("_traceMalloc(result->data);\n"));
                    buff.append(Statement.indent("result->_refCount = 1;\n"));
                    buff.append(Statement.indent("return result;\n"));
                    buff.append("}\n");
                } else if (t.isPointer()) {
                    buff.append(t.nameC() + "* " + t.nameC() + "_new() {\n");
                    buff.append(Statement.indent(t.nameC() + "* result = _malloc(sizeof(" + t.nameC() + "));\n"));
                    buff.append(Statement.indent("_traceMalloc(result);\n"));
                    buff.append(Statement.indent("result->_refCount = 1;\n"));
                    for(Variable f : t.fields) {
                        buff.append(Statement.indent("result->" + f.name + " = 0;\n"));
                    }
                    buff.append(Statement.indent("return result;\n"));
                    buff.append("}\n");
                } else if (!t.isArray()) {
                    buff.append(t.nameC() + " " + t.nameC() + "_new() {\n");
                    buff.append(Statement.indent(t.nameC() + " result;\n"));
                    for(Variable f : t.fields) {
                        buff.append(Statement.indent("result." + f.name + " = 0;\n"));
                    }
                    buff.append(Statement.indent("return result;\n"));
                    buff.append("}\n");
                }
            }
        }
        buff.append("/* exception types */\n");
        HashSet<String> exceptionStructs = new HashSet<>();
        for (FunctionDefinition def : functions.values()) {
            String s = def.getExceptionStruct();
            if (def.used && s != null && !exceptionStructs.contains(s)) {
                exceptionStructs.add(s);
                buff.append("typedef struct " + s + " " + s + ";\n");
                buff.append("struct ").append(s).append(" {\n");
                buff.append(Statement.indent(def.exceptionType.toC() + " exception;\n"));
                if (def.returnType != null) {
                    buff.append(Statement.indent(def.returnType.toC() + " result;\n"));
                }
                buff.append("};\n");
                buff.append(s + " ok" + s + "(");
                if (def.returnType != null) {
                    buff.append(def.returnType.toC() + " result");
                }
                buff.append(") {\n");
                buff.append(Statement.indent(s + " x;\n"));
                buff.append(Statement.indent("x.exception.exceptionType = -1;\n"));
                if (def.returnType != null) {
                    buff.append(Statement.indent("x.result = result;\n"));
                }
                buff.append(Statement.indent("return x;\n"));
                buff.append("}\n");
                buff.append(s + " exception" + s + "(");
                buff.append(def.exceptionType.toC() + " exception");
                buff.append(") {\n");
                buff.append(Statement.indent(s + " x;\n"));
                buff.append(Statement.indent("x.exception = exception;\n"));
                if (def.returnType != null) {
                    // no need
                    // buff.append(Statement.indent("x.result = 0;\n"));
                }
                buff.append(Statement.indent("return x;\n"));
                buff.append("}\n");
            }
        }
        buff.append("/* functions */\n");
        for (FunctionDefinition def : functions.values()) {
            if (def.used) {
                context.function = def;
                if (def.comment != null) {
                    buff.append("/*\n");
                    buff.append(Statement.indent(def.comment));
                    buff.append("*/\n");
                }
                buff.append(def.declarationToC());
            }
        }
        // _free needs be after close
        for (DataType t : dataTypeMap.values()) {
            if (!t.isSystem() && t.isUsed()) {
                if (t.needFree()) {
                    buff.append("void " + t.nameC() + "_free(" + t.nameC() + "* x);\n");
                }
            }
        }
        for (DataType t : dataTypeMap.values()) {
            if (!t.isSystem() && t.isUsed()) {
                if (t.isArray()) {
                    buff.append("void " + t.nameC() + "_free(" + t.nameC() + "* x) {\n");
                    if (t.baseType().needIncDec()) {
                        buff.append(Statement.indent("for (int i = 0; i < x->len; i++) _decUse(x->data[i], " + t.baseType().nameC() + ");\n"));
                    } else if (t.baseType().needFree()) {
                        buff.append(Statement.indent("for (int i = 0; i < x->len; i++) " + t.baseType().nameC() + "_free(&(x->data[i]));\n"));
                    }
                    buff.append(Statement.indent("_free(x->data);\n"));
                    buff.append(Statement.indent("_free(x);\n"));
                    buff.append("}\n");
                } else if (t.needFree()) {
                    buff.append("void " + t.nameC() + "_free(" + t.nameC() + "* x) {\n");
                    for (Variable f : t.fields) {
                        if (f.type.needIncDec()) {
                            buff.append(Statement.indent(Free.DEC_USE + "(x->" + f.name + ", " + f.type().nameC() + ");\n"));
                        } else if (f.type.needFree()) {
                            buff.append(Statement.indent(f.type.nameC() + "_free(x->" + f.name + ");\n"));
                        }
                    }
                    if (t.autoClose != null) {
                        buff.append(Statement.indent(t.nameC() + "_close_1(x);\n"));
                        buff.append(Statement.indent("if (x->_refCount) { fprintf(stdout, \"Object re-referenced in the close method\"); exit(1); }\n"));
                    }
                    if (t.needIncDec()) {
                        // structs don't need free
                        buff.append(Statement.indent("_free(x);\n"));
                    }
                    buff.append("}\n");
                }
            }
        }
        if (!stringConstantsMap.isEmpty()) {
            buff.append("i8_array* str_const(char* data, uint32_t len) {\n");
            buff.append(Statement.indent("i8_array* result = _malloc(sizeof(i8_array));\n"));
            buff.append(Statement.indent("result->len = len;\n"));
            // 0 means do not free the memory (it looks like it's already free)
            buff.append(Statement.indent("result->_refCount = INT32_MAX;\n"));
            buff.append(Statement.indent("result->data = data;\n"));
            buff.append(Statement.indent("return result;\n"));
            buff.append("}\n");
        }
        for (long id: stringConstantsMap.keySet()) {
            buff.append("i8_array* string_" + id + ";\n");
        }
        for (Variable var : globalVariables.values()) {
            buff.append(var.type.toC() + " " + var.name + ";\n");
        }
        for (FunctionDefinition def : functions.values()) {
            if (def.used) {
                context.nextFunction();
                context.function = def;
                def.optimize(context);
                buff.append(def.toC(context));
            }
        }
        buff.append("int main() {\n");
        for (long id: stringConstantsMap.keySet()) {
            String s = stringConstantsMap.get(id);
            byte[] data = s.getBytes(StandardCharsets.UTF_8);
            buff.append(Statement.indent("string_" + id + " = str_const(\"" + StringLiteral.escape(s) + "\", " + data.length + ");\n"));
        }
        context.nextFunction();
        StringBuilder buff2 = new StringBuilder();
        for(Statement s : list) {
            s.optimize(context);
        }
        for(Statement s : list) {
            buff2.append(Statement.indent(s.toC()));
        }
        if (!context.delareList.isEmpty()) {
            for (String s : context.delareList) {
                buff.append(Statement.indent(s + "\n"));
            }
        }
        buff.append(buff2.toString());
        if (autoClose != null) {
            for (Statement s : autoClose) {
                s.optimize(context);
            }
            for (Statement s : autoClose) {
                buff.append(Statement.indent(s.toC()));
            }
        }
        buff.append(Statement.indent("_end();\n"));
        buff.append(Statement.indent("return 0;\n"));
        if (context.needToCatch != null) {
            throw new IllegalStateException("Possible exception is not caught at " + buff.toString());
        }
        buff.append("}\n");
        if (!comments.isEmpty()) {
            buff.append("/*\n");
            StringBuffer commentBuff = new StringBuffer();
            for (int i = 0; i < comments.size(); i += 2) {
                String object = comments.get(i);
                String comment = comments.get(i + 1);
                commentBuff.append("\n");
                commentBuff.append(object);
                commentBuff.append("\n");
                commentBuff.append(comment);
                commentBuff.append("\n");
            }
            buff.append(commentBuff.toString().replace("*/", "* /"));
            buff.append("\n*/\n");
        }
        return buff.toString();
    }

    public String toMarkdown() {
        // TODO json output, and then convert that to markdown
        if (comments.isEmpty()) {
            return null;
        }
        /*

### Type `List(T)`
A list of entries.

#### `List(T) add(x T)`
Add an entry to the list.

#### `test()`
Testing.

         */

        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < comments.size(); i += 2) {
            String object = comments.get(i);
            String comment = comments.get(i + 1);
            buff.append("\n");
            if (object.startsWith("type")) {
                buff.append("### " + object);
            } else {
                buff.append("#### " + object);
            }
            buff.append("\n");
            buff.append(comment);
            buff.append("\n");
        }
        return buff.toString();
    }

    public String run() {
        Memory m = new Memory();
        for (Entry<String, FunctionDefinition> e : functions.entrySet()) {
            m.addFunction(e.getKey(), e.getValue());
        }
        for (Entry<Long, String> e : stringConstantsMap.entrySet()) {
            byte[] bytes = e.getValue().getBytes(StandardCharsets.UTF_8);
            Value v = new Value.ValueI8Array(bytes);
            m.setConstant(e.getKey(), v);
        }
        for (Entry<String, Variable> e : globalVariables.entrySet()) {
            m.setGlobal(e.getValue().name, e.getValue().type().getZeroValue());
        }
        ArrayList<Statement> l2 = new ArrayList<>();
        l2.addAll(list);
        l2.addAll(autoClose);
        runSequence(m, l2);
        this.ticksExecuted = m.getTicksExecuted();
        return m.getOutput();
    }

    public long getTicksExecuted() {
        return ticksExecuted;
    }

    public String getModuleId(String module) {
        if (module == null) {
            return null;
        }
        // TODO use a reverse map
        for (Entry<String, String> e : imports.entrySet()) {
            if (module.equals(e.getValue())) {
                return e.getKey();
            }
        }
        return null;
    }

    public void addImport(String name, String as, ArrayList<String> entries) {
        imports.put(as, name);
        for(String e : entries) {
            importEntries.put(e, name);
        }
    }

    public String getImportEntry(String identifier) {
        return importEntries.get(identifier);
    }

    public String getImport(String as) {
        return imports.get(as);
    }

    public void addIncludeC(String file) {
        includes.add(file);
    }

    public void addComment(String object, String comment) {
        if (comment != null) {
            comments.add(object);
            comments.add(comment);
        }
    }

    public String readModule(String name) {
        String m = modules.get(name);
        if (m != null) {
            return m;
        }
        String fileName = name.replace('.', '/');
        InputStream in = getClass().getResourceAsStream("/" + fileName + ".bau");
        if (in != null) {
            return readFromInputStream(in);
        }
        return null;
    }

    public static String readFromInputStream(InputStream in) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        try {
            while (true) {
                int n = in.read(data);
                if (n < 0) {
                    break;
                }
                buffer.write(data, 0, n);
            }
            in.close();
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed reading from input stream: " + e);
        }
    }

    public static StatementResult runSequence(Memory m, List<Statement> list) {
        for (int i = 0; i < list.size(); i++) {
            Statement s = list.get(i);
            StatementResult n = s.run(m);
            if (m.tick()) {
                return StatementResult.TIMEOUT;
            }
            if (n == StatementResult.OK) {
                // ok
            } else if (n == StatementResult.BREAK) {
                return n;
            } else if (n == StatementResult.CONTINUE) {
                return n;
            } else if (n == StatementResult.RETURN) {
                return n;
            } else if (n == StatementResult.THROW) {
                i++;
                for (; i < list.size(); i++) {
                    s = list.get(i);
                    if (s instanceof Catch) {
                        i--;
                        break;
                    }
                }
                if (i == list.size()) {
                    return StatementResult.THROW;
                }
            } else if (n == StatementResult.PANIC) {
                return n;
            }
        }
        return StatementResult.OK;
    }

    Value evalConstants(Expression expr) {
        Memory memory = new Memory();
        memory.addFunction(null, null);
        memory.evaluateOnlyConstExpr(true, 1_000_000);
        Value result = expr.eval(memory);
        uncompiledFunctions.putAll(memory.getUncompiledFunctions());
        return result;
    }

    public List<FunctionDefinition> getFunctions() {
        return new ArrayList<>(functions.values());
    }

}
