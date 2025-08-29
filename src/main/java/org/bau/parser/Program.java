package org.bau.parser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bau.parser.Statement.StatementResult;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueRef;
import org.bau.std.Std;

public class Program {

    private boolean useTmMalloc = false;
    private boolean traceRefCounts = false;

    ArrayList<Statement> initList = new ArrayList<>();
    ArrayList<Statement> mainList = new ArrayList<>();

    LinkedHashMap<String, DataType> dataTypeMap = new LinkedHashMap<String, DataType>();

    // string -> reference
    HashMap<String, Long> stringConstants = new HashMap<>();

    // reference -> string
    TreeMap<Long, StringLiteral> stringConstantsMap = new TreeMap<>();

    TreeMap<Long, Variable> arrayConstantsMap = new TreeMap<>();

    HashMap<String, Variable> variables = new HashMap<>();

    // global variables (or constants) in modules
    LinkedHashMap<String, Variable> globalVariables = new LinkedHashMap<>();

    TreeMap<String, Trait> traits = new TreeMap<>();

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

    private int nextTempVariableIdGlobalScopeId;

    {
        FunctionDefinition f = new FunctionDefinition(0);
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

    public int nextTempVariableIdGlobalScope() {
        return nextTempVariableIdGlobalScopeId++;
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

    public long addArrayConstant(Variable var) {
        long reference = 1000L + arrayConstantsMap.size();
        arrayConstantsMap.put(reference, var);
        return reference;
    }

    public StringLiteral getStringLiteral(String n) {
        Long reference = stringConstants.get(n);
        if (reference == null) {
            return null;
        }
        return stringConstantsMap.get(reference);
    }

    public long addStringConstant(String n, StringLiteral literal) {
        Long reference = stringConstants.get(n);
        if (reference == null) {
            reference = 1000L + stringConstants.size();
            stringConstants.put(n, reference);
            stringConstantsMap.put(reference, literal);
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
        }
    }

    public FunctionDefinition getFunction(DataType type, String module, String name, int parameterCount) {
        FunctionDefinition def = getFunctionIfExists(type, module, name, parameterCount);
        if (def == null) {
            throw new IllegalArgumentException(name);
        }
        return def;
    }

    public void traceRefCounts() {
        this.traceRefCounts = true;
    }

    public void useTmMalloc() {
        this.useTmMalloc = true;
    }

    public Expression cast(Expression expr, DataType target) {
        if (target.isNullable()) {
            if (expr.type() == null) {
                if (expr instanceof NullValue) {
                    return new NullValue(target);
                }
            } else if (expr.type().orNull().equals(target)) {
                return expr;
            }
        }
        if (target.traitDefinition != null) {
            for (String t : expr.type().traits) {
                if (t.equals(target.name())) {
                    // type has this trait
                    return new Cast(expr, target);
                }
            }
        }
        DataType source = expr.type();
        if (source == null) {
            return expr;
        } else if (source.equals(target)) {
            return null;
        } else if (source.isRange()) {
            return expr;
        }
        // 'convertIntToString(int x) org.bau.String'
        String convertFunctionName = "convert" + source.getCamelCaseName() + "To" + target.getCamelCaseName();
        FunctionDefinition fromFunction = getFunctionIfExists(null, source.module, convertFunctionName, 1);
        if (fromFunction != null) {
            Call call = new Call();
            call.args.add(expr);
            call.def = fromFunction;
            return call;
        }
        fromFunction = getFunctionIfExists(null, target.module, convertFunctionName, 1);
        if (fromFunction != null) {
            Call call = new Call();
            call.args.add(expr);
            call.def = fromFunction;
            return call;
        }
        // 'int toString() org.bau.String'
        String toFunctionName = "to" + target.getCamelCaseName();
        FunctionDefinition toFunction = getFunctionIfExists(source, target.module, toFunctionName, 1);
        if (toFunction != null) {
            Call call = new Call();
            call.args.add(expr);
            call.def = toFunction;
            return call;
        }

        if (source.isFloatingPoint()) {
            if (!target.isFloatingPoint()) {
                return null;
            }
            if (target.sizeOf() >= source.sizeOf()) {
                return expr;
            }
            return null;
        } else if (source.isNumber()) {
            if (!target.isNumber()) {
                return null;
            }
            if (target.sizeOf() >= source.sizeOf() || target.isFloatingPoint()) {
                return expr;
            } else if (expr instanceof NumberValue) {
                NumberValue nv = (NumberValue) expr;
                Value v = expr.eval(null);
                if (v != null) {
                    long x = v.longValue();
                    long minValue = -(1L << (target.sizeOf() * 8 - 1));
                    long maxValue = (1L << (target.sizeOf() * 8 - 1)) - 1;
                    if (x >= minValue && x <= maxValue) {
                        // small value
                        return new NumberValue(v, target, nv.hex);
                    }
                }
            }
            return null;
        } else if (source.toString().equals(target.toString())) {
            // memory type is different
            return expr;
        } else if (source.isNullable() && target.orNull() == source) {
            return expr;
        }
        return null;
    }

    public FunctionDefinition getFunctionFuzzyMatch(DataType type, String module, String name,
            int expectedParameterCount) {
        for (int offset = 0; offset < 20; offset++) {
            // start with the expected count (eg. 5), and then:
            // 4, 6, 3, 7, 2, 8, 1, 9, 0, 10, 11, 12,...
            int parameterCount;
            if (offset == 0) {
                parameterCount = expectedParameterCount;
            } else {
                int o = (offset + 1) / 2;
                if (offset % 2 == 1) {
                    parameterCount = expectedParameterCount - o;
                } else {
                    parameterCount = expectedParameterCount + o;
                }
            }
            if (parameterCount < 0) {
                continue;
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
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public FunctionDefinition getFunctionIfExists(DataType type, FunctionDefinition calledFrom, String module, String name, int parameterCount) {
        FunctionDefinition def = getFunctionIfExists(type, module, name, parameterCount);
        if (def != null) {
            return def;
        }

        // try to call a "this." function (e.g. from within a function BigInt.add(), call BigInt.negate())
        if (type == null && calledFrom != null && calledFrom.callType != null) {
            // add the "this." parameter
            def = getFunctionIfExists(calledFrom.callType, module, name, 1 + parameterCount);
        }
        return def;
    }

    public FunctionDefinition getFunctionIfExists(DataType type, String module, String name, int parameterCount) {
        if ("println".equals(name)) {
            // TODO support varargs
            parameterCount = 0;
        }
        String id = FunctionDefinition.getFunctionId(type, module, name, parameterCount);
        if (id == null) {
            return null;
        }
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
        if (type.memoryType() == MemoryType.REF_COUNT) {
            dataTypeMap.put(type.ownerType().fullName(), type.ownerType());
        }
        return type;
    }

    public DataType getType(String module, String name) {
        String fullName = DataType.fullName(module, name);
        DataType t = dataTypeMap.get(fullName);
        if (t == null && module != null) {
            t = dataTypeMap.get(name);
        }
        return t;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        for (Statement s : initList) {
            buff.append(s);
        }
        for (Statement s : mainList) {
            buff.append(s);
        }
        return buff.toString();
    }

    public String toC() {
        for (Statement s : initList) {
            s.used(this);
        }
        for (Statement s : mainList) {
            s.used(this);
        }
        ProgramContext context = new ProgramContext();
        StringBuilder buff = new StringBuilder();
        buff.append("#include <stdio.h>\n");
        buff.append("#include <stdlib.h>\n");
        buff.append("#include <stdarg.h>\n");
        buff.append("#include <stdint.h>\n");
        buff.append("#include <string.h>\n");
        for (FunctionDefinition def : functions.values()) {
            if (def.isUsed() && def.includes != null) {
                includes.addAll(def.includes);
            }
        }
        if (useTmMalloc) {
            includes.addAll(List.of(StandardLib.TM_MALLOC_INCLUDE.split("\n")));
        }
        for(String i : includes) {
            buff.append("#include " + i + "\n");
        }
        buff.append("/* builtin */\n");
        buff.append("static inline int _ctzll(uint64_t x) {\n");
        buff.append("#if defined(__GNUC__) || defined(__clang__)\n");
        buff.append("    return __builtin_ctzll(x);\n");
        buff.append("#else\n");
        buff.append("    if (!x) return 64; int c = 0; while (!(x & 1)) { x >>= 1; c++; } return c;\n");
        buff.append("#endif\n");
        buff.append("}\n");
        buff.append("static inline int _clzll(uint64_t x) {\n");
        buff.append("#if defined(__GNUC__) || defined(__clang__)\n");
        buff.append("    return __builtin_clzll(x);\n");
        buff.append("#else\n");
        buff.append("    if (!x) return 64; int c = 0; uint64_t m = (uint64_t)1 << 63; while (!(x & m)) { m >>= 1; c++; } return c;\n");
        buff.append("#endif\n");
        buff.append("}\n");
        if (useTmMalloc) {
            buff.append(StandardLib.TM_MALLOC);
            buff.append("#define _malloc(a)      tmmalloc(a)\n");
            buff.append("#define _free(a)        tmfree(a)\n");
        } else {
            buff.append("#define _malloc(a)      malloc(a)\n");
            buff.append("#define _free(a)        free(a)\n");
        }
        if (traceRefCounts) {
            buff.append("int __globalObjects = 0;\n");
            buff.append("int __refCountUpdates = 0;\n");
            buff.append("int __refCountStackUpdates = 0;\n");
            buff.append("#define REF_COUNT_INC       __refCountUpdates++\n");
            buff.append("#define REF_COUNT_STACK_INC __refCountStackUpdates++\n");
            buff.append("#define PRINT(...)          printf(__VA_ARGS__);\n");
            buff.append("#define _end()              {PRINT(\"refCountUpdates: %d, stack: %d\\n\", __refCountUpdates, __refCountStackUpdates); if(__globalObjects!=0)PRINT(\"################ MEMORY LEAK: %d ################\\n\", __globalObjects);}\n");
            buff.append("#define _traceMalloc(a)     PRINT(\"new %p line %d (%d)\\n\", a, __LINE__, ++__globalObjects);\n");
            buff.append("#define _traceFree(a)       PRINT(\"del %p line %d (%d)\\n\", a, __LINE__, --__globalObjects);\n");
        } else {
            buff.append("#define REF_COUNT_INC\n");
            buff.append("#define REF_COUNT_STACK_INC\n");
            buff.append("#define PRINT(...)\n");
            buff.append("#define _end()\n");
            buff.append("#define _traceMalloc(a)\n");
            buff.append("#define _traceFree(a)\n");
        }
        // note: __builtin_assume((a)->_refCount > 0) doesn't seem to have an effect
        buff.append("#define _incUse(a)            {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT(\"++  %p line %d, from %d\\n\", a, __LINE__, (a)?(a)->_refCount:0); (a)->_refCount++;}}\n");
        buff.append("#define _decUse(a, type)      {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT(\"--  %p line %d, from %d\\n\", a, __LINE__, (a)->_refCount);if(--((a)->_refCount) == 0)type##_free(a);}}\n");
        // buff.append("#define _decUse(a, type)      {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT(\"--  %p line %d, from %d\\n\", a, __LINE__, (a)->_refCount);if((a)->_refCount < 0) PRINT(\"################ DOUBLE FREE ################\\n\"); if(--((a)->_refCount) == 0)type##_free(a);}}\n");
        buff.append("#define _incUseStack(a)       _incUse(a)\n");
        buff.append("#define _decUseStack(a, type) _decUse(a, type)\n");
        buff.append("int64_t arrayOutOfBounds(int64_t x, int64_t len) {\n"
                + "    fprintf(stdout, \"Array index %lld is out of bounds for the array length %lld\\n\", x, len);\n"
                + "    exit(1);\n"
                + "}\n");
        boolean hasTraits = false;
        for (DataType t : dataTypeMap.values()) {
            if (t.isUsed() && !t.traits.isEmpty()) {
                hasTraits = true;
            }
        }
        if (hasTraits) {
            buff.append("/* traits */\n");
            buff.append("typedef struct _typeMetaData _typeMetaData;\n");
            buff.append("typedef void (*_func)(void);\n");
            buff.append("struct _typeMetaData {\n");
            buff.append(Statement.indent("const char* typeName;\n"));
            buff.append(Statement.indent("void (*vtable[])();\n"));
            buff.append("};\n");
            for (DataType t : dataTypeMap.values()) {
                if (t.isUsed() && !t.traits.isEmpty()) {
                    buff.append("static _typeMetaData *_typeMeta" + t.nameC() + ";\n");
                }
            }
        }
        buff.append("/* types */\n");
        for (DataType t : dataTypeMap.values()) {
            if (t.enumValues != null) {
                continue;
            }
            if (t.isUsed() && !t.isNumber()) {
                buff.append("typedef struct " + t.nameC() + " " + t.nameC() + ";\n");
                buff.append("struct ").append(t.nameC()).append(";\n");
            }
        }
        for (DataType t : dataTypeMap.values()) {
            if (t.enumValues != null) {
                continue;
            }
            if (!t.isNumber() && t.isUsed()) {
                buff.append("struct ").append(t.nameC()).append(" {\n");
                if (t.isArray()) {
                    buff.append(Statement.indent("int32_t len;\n"));
                    if (t.memoryType() == MemoryType.REF_COUNT) {
                        buff.append(Statement.indent("int32_t _refCount;\n"));
                    }
                    buff.append(Statement.indent(t.baseType().toC() + "* data;\n"));
                } else {
                    if (!t.traits.isEmpty() || t.traitDefinition != null) {
                        buff.append(Statement.indent("_typeMetaData* _type;\n"));
                    }
                    if (t.memoryType() == MemoryType.REF_COUNT) {
                        buff.append(Statement.indent("int32_t _refCount;\n"));
                    }
                    for (Variable f : t.fields) {
                        buff.append(Statement.indent(f.type().toC() + " " + f.nameC() + ";\n"));
                    }
                }
                buff.append("};\n");
                if (t.isArray()) {
                    buff.append(t.nameC() + "* " + t.nameC() + "_new(uint32_t len) {\n");
                    buff.append(Statement.indent(t.nameC() + "* result = _malloc(sizeof(" + t.nameC() + "));\n"));
                    buff.append(Statement.indent("_traceMalloc(result);\n"));
                    buff.append(Statement.indent("result->len = len;\n"));
                    buff.append(Statement.indent("result->data = _malloc(sizeof(" + t.baseType().toC() + ") * len);\n"));
                    buff.append(Statement.indent("memset(result->data, 0, sizeof(" + t.baseType().toC() + ") * len);\n"));
                    buff.append(Statement.indent("_traceMalloc(result->data);\n"));
                    buff.append(Statement.indent("result->_refCount = 1;\n"));
                    buff.append(Statement.indent("return result;\n"));
                    buff.append("}\n");
                } else if (t.isPointer()) {
                    buff.append(t.nameC() + "* " + t.nameC() + "_new() {\n");
                    buff.append(Statement.indent(t.nameC() + "* result = _malloc(sizeof(" + t.nameC() + "));\n"));
                    buff.append(Statement.indent("_traceMalloc(result);\n"));
                    if (t.memoryType() == MemoryType.REF_COUNT) {
                        if (!t.traits.isEmpty()) {
                            buff.append(Statement.indent("result->_type = _typeMeta" + t.nameC() + ";\n"));
                        }
                        buff.append(Statement.indent("result->_refCount = 1;\n"));
                    }
                    buff.append(Statement.indent("return result;\n"));
                    buff.append("}\n");
                } else if (!t.isArray()) {
                    buff.append(t.nameC() + " " + t.nameC() + "_new() {\n");
                    buff.append(Statement.indent(t.nameC() + " result;\n"));
                    for (Variable f : t.fields) {
                        buff.append(Statement.indent("result." + f.assignmentC() + " = 0;\n"));
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
            if (def.isUsed() && s != null && !exceptionStructs.contains(s)) {
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
        buff.append("/* global */\n");
        buff.append("int __argc;\n");
        buff.append("char **__argv;\n");
        buff.append("/* functions */\n");
        for (FunctionDefinition def : functions.values()) {
            if (def.isUsed()) {
                def.borrowCheck();
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
            if (t.isUsed()) {
                if (t.isArray() || t.needFree()) {
                    buff.append("void " + t.nameC() + "_free(" + t.nameC() + "* x);\n");
                    if (t.isCopyType() && !t.isArray()) {
                        buff.append("void " + t.nameC() + "_copy(" + t.nameC() + "* x);\n");
                    }
                }
            }
        }
        for (DataType t : dataTypeMap.values()) {
            if (t.isUsed()) {
                if (t.isArray() || t.needFree()) {
                    buff.append("void " + t.nameC() + "_free(" + t.nameC() + "* x) {\n");
                    if (t.memoryType() == MemoryType.OWNER) {
                        buff.append(Statement.indent("if (x == NULL) return;\n"));
                    }
                    if (t.isArray()) {
                        if (t.baseType().needIncDec()) {
                            buff.append(Statement.indent("for (int i = 0; i < x->len; i++) " + Free.DEC_USE + "(x->data[i], " + t.baseType().nameC() + ");\n"));
                        } else if (t.baseType().needFree()) {
                            buff.append(Statement.indent("for (int i = 0; i < x->len; i++) " + t.baseType().nameC() + "_free(&(x->data[i]));\n"));
                        }
                        buff.append(Statement.indent("_free(x->data); _traceFree(x->data);\n"));
                        buff.append(Statement.indent("_free(x); _traceFree(x);\n"));
                        buff.append("}\n");
                    } else {
                        for (Variable f : t.fields) {
                            if (f.type().needIncDec()) {
                                if (f.type().memoryType() == MemoryType.REF_COUNT) {
                                    buff.append(Statement.indent(Free.DEC_USE + "(x->" + f.nameC() + ", " + f.type().nameC() + ");\n"));
                                } else {
                                    buff.append(Statement.indent("if (x->" + f.nameC() + ") " + f.type().nameC() + "_free(x->" + f.nameC() + ");\n"));
                                }
                            } else if (f.type().needFree()) {
                                if (f.type().isCopyType()) {
                                    buff.append(Statement.indent(f.type().nameC() + "_free(&x->" + f.nameC() + ");\n"));
                                } else {
                                    buff.append(Statement.indent("if (x->" + f.nameC() + ") " + f.type().nameC() + "_free(x->" + f.nameC() + ");\n"));
                                }
                            }
                        }
                        if (t.autoClose != null) {
                            buff.append(Statement.indent(t.nameC() + "_" + esc("close") + "_1(x);\n"));
                            buff.append(Statement.indent("if (x->_refCount) { fprintf(stdout, \"Object re-referenced in the close method\"); exit(1); }\n"));
                        }
                        if (t.needIncDec()) {
                            // structs don't need free
                            buff.append(Statement.indent("_free(x); _traceFree(x);\n"));
                        }
                        buff.append("}\n");
                    }
                    if (t.isCopyType() && !t.isArray()) {
                        buff.append("void " + t.nameC() + "_copy(" + t.nameC() + "* x) {\n");
                        for (Variable f : t.fields) {
                            if (f.type().needIncDec()) {
                                if (f.type().memoryType() == MemoryType.REF_COUNT) {
                                    buff.append(Statement.indent(Free.INC_USE + "(x->" + f.nameC() + ");\n"));
                                } else {
                                    buff.append(Statement.indent("if (x->" + f.nameC() + ") " + f.type().nameC() + "_copy(x->" + f.nameC() + ");\n"));
                                }
                            } else if (f.type().needFree()) {
                                buff.append(Statement.indent("if (x->" + f.nameC() + ") " + f.type().nameC() + "_copy(x->" + f.nameC() + ");\n"));
                            }
                        }
                        buff.append("}\n");
                    }
                }
            }
        }
        boolean hasStringConstants = false;
        for (long id: stringConstantsMap.keySet()) {
            if (stringConstantsMap.get(id).isUsed()) {
                hasStringConstants = true;
                break;
            }
        }
        if (hasStringConstants) {
            buff.append(esc("i8") + "_array* str_const(char* data, uint32_t len) {\n");
            buff.append(Statement.indent(esc("i8") + "_array* result = _malloc(sizeof(" + esc("i8") + "_array));\n"));
            buff.append(Statement.indent("result->len = len;\n"));
            // 0 means do not free the memory (it looks like it's already free)
            buff.append(Statement.indent("result->_refCount = INT32_MAX;\n"));
            buff.append(Statement.indent("result->data = (int8_t*) data;\n"));
            buff.append(Statement.indent("return result;\n"));
            buff.append("}\n");
            for (long id: stringConstantsMap.keySet()) {
                if (stringConstantsMap.get(id).isUsed()) {
                    buff.append(esc("i8") + "_array* string_" + id + ";\n");
                }
            }
        }
        if (!arrayConstantsMap.isEmpty()) {
            buff.append(esc("int") + "_array* int_array_const(int64_t* data, uint32_t len) {\n");
            buff.append(Statement.indent(esc("int") + "_array* result = _malloc(sizeof(" + esc("int") + "_array));\n"));
            buff.append(Statement.indent("result->len = len;\n"));
            // 0 means do not free the memory (it looks like it's already free)
            buff.append(Statement.indent("result->_refCount = INT32_MAX;\n"));
            buff.append(Statement.indent("result->data = (int64_t*) data;\n"));
            buff.append(Statement.indent("return result;\n"));
            buff.append("}\n");
            for (long id: arrayConstantsMap.keySet()) {
                buff.append(esc("int") + "_array* array_" + id + ";\n");
            }
        }
        for (Variable var : globalVariables.values()) {
            if (var.isUsed()) {
                buff.append(var.declarationToC() + ";\n");
            }
        }
        for (FunctionDefinition def : functions.values()) {
            if (def.isUsed()) {
                context.nextFunction();
                context.function = def;
                def.optimize(context);
                buff.append(def.toC(context));
            }
        }
        if (hasTraits) {
            buff.append("/* traits */\n");
            buff.append("void _traitInit() {\n");
            for (DataType t : dataTypeMap.values()) {
                if (t.isUsed() && !t.traits.isEmpty()) {
                    int n = 3;
                    String name = "_typeMeta" + t.nameC();
                    buff.append(Statement.indent(name + " = malloc(sizeof(_typeMetaData) + " + n + " * sizeof(int));\n"));
                    buff.append(Statement.indent(name + "->typeName = \"" + t.name() + "\";\n"));
                    for (int i = 0; i < n; i++) {
                        buff.append(Statement.indent(name + "->vtable[" + i + "] = _traitInit;\n"));
                    }
                }
            }
            buff.append("}\n");
        }
        buff.append("void _main();\n");
        buff.append("int main(int _argc, char *_argv[]) {\n");
        if (useTmMalloc) {
            buff.append(Statement.indent("tmmalloc_init();\n"));
        }
        if (hasTraits) {
            buff.append(Statement.indent("_traitInit();\n"));
        }
        buff.append(Statement.indent("__argc = _argc;\n"));
        buff.append(Statement.indent("__argv = _argv;\n"));
        for (long id: stringConstantsMap.keySet()) {
            StringLiteral literal = stringConstantsMap.get(id);
            if (literal.isUsed()) {
                String s = literal.value;
                byte[] data = s.getBytes(StandardCharsets.UTF_8);
                buff.append(Statement.indent("string_" + id + " = str_const(\"" + StringLiteral.escape(s) + "\", " + data.length + ");\n"));
            }
        }
        for (long id : arrayConstantsMap.keySet()) {
            Variable var = arrayConstantsMap.get(id);
            if (var.type().baseType() != DataType.INT_TYPE) {
                throw new IllegalStateException("Only integer array constants are supported currently");
            }
            Value data = var.constantValue;
            StringBuilder buff2 = new StringBuilder();
            for (int i = 0; i < data.len().intValue(); i++) {
                if (i > 0) {
                    buff2.append(", ");
                }
                buff2.append(data.get(i).toString());
            }
            buff.append(Statement.indent("int64_t array_const_" + id + "[] = {" + buff2.toString() + "};\n"));
            buff.append(Statement.indent(
                    "array_" + id + " = int_array_const(array_const_" + id + ", " + data.len().intValue() + ");\n"));
        }
        buff.append(Statement.indent("_main();\n"));
        buff.append(Statement.indent("return 0;\n"));
        buff.append("}\n");
        buff.append("void _main() {\n");
        context.nextFunction();
        FunctionDefinition main = new FunctionDefinition(0);
        main.list = mainList;
        main.name = "main";
        main.borrowCheck();
        StringBuilder buff2 = new StringBuilder();
        for (Statement s : initList) {
            s.optimize(context);
        }
        for (Statement s : mainList) {
            s.optimize(context);
        }
        if (!initList.isEmpty()) {
            StringBuilder buff3 = new StringBuilder();
            for (Statement s : initList) {
                buff3.append(s.toC());
            }
            buff2.append(Statement.indent(buff3.toString()));
        }
        boolean hasCatch = Program.hasCatch(mainList);
        if (hasCatch) {
            buff2.append(Statement.indent("do { do {\n"));
        }
        for (Statement s : mainList) {
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
        // free contants (to avoid memory leaks)
        for (Statement s : initList) {
            if (s instanceof Assignment) {
                LeftValue lv = ((Assignment) s).leftValue;
                if (lv instanceof Variable) {
                    if (lv.type().needIncDec() || lv.type().needFree()) {
                        Free free = new Free((Variable) lv);
                        buff.append(Statement.indent(free.toC()));
                    }
                }
            }
        }
        buff.append(Statement.indent("_end();\n"));
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
        return run(Long.MAX_VALUE);
    }

    public String run(long maxTicks) {
        Memory m = new Memory();
        if (maxTicks != Long.MAX_VALUE) {
            m.evaluateOnlyConstExpr(false, maxTicks);
        }
        for (Entry<String, FunctionDefinition> e : functions.entrySet()) {
            m.addFunction(e.getKey(), e.getValue());
        }
        for (Entry<Long, StringLiteral> e : stringConstantsMap.entrySet()) {
            byte[] bytes = e.getValue().value.getBytes(StandardCharsets.UTF_8);
            Value v = new Value.ValueI8Array(bytes);
            m.setConstant(e.getKey(), v);
        }
        for (Entry<String, Variable> e : globalVariables.entrySet()) {
            m.setGlobal(e.getValue().name, e.getValue().type().getZeroValue());
        }
        ArrayList<Statement> l2 = new ArrayList<>();
        l2.addAll(initList);
        l2.addAll(mainList);
        l2.addAll(autoClose);
        try {
            runSequence(m, l2);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        this.ticksExecuted = m.getTicksExecuted();
        String output = m.getOutput();
        if (ticksExecuted >= maxTicks) {
            output += "\n(Program stopped after " + ticksExecuted + " ticks)";
        }
        return output;
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
        String fileName = name.replace('.', '/') + ".bau";
        InputStream in = getClass().getResourceAsStream("/" + fileName);
        if (in != null) {
            return readFromInputStream(in);
        }
        if (new File(fileName).exists()) {
            try (FileInputStream f = new FileInputStream(fileName)) {
                return readFromInputStream(f);
            } catch (IOException e) {
                throw new RuntimeException("Failed reading from input stream: " + e);
            }
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

    public static boolean hasCatch(ArrayList<Statement> list) {
        for (Statement s : list) {
            if (s instanceof Catch) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasReturn(ArrayList<Statement> list) {
        for (Statement s : list) {
            if (s instanceof Return) {
                return true;
            }
        }
        return false;
    }

    public static StatementResult runSequence(Memory m, List<Statement> list) {
        return runSequence(m, list, -1);
    }

    public static boolean doesReturn(List<Statement> list) {
        boolean doesReturn = false;
        for (Statement s : list) {
            if (s instanceof Return) {
                doesReturn = true;
                break;
            } else if (s instanceof Throw) {
                doesReturn = true;
                break;
            } else if (s instanceof If) {
                If ifStatement = (If) s;
                for (List<Statement> l2 : ifStatement.listList) {
                    // TODO we need to check all branches
                    if (doesReturn(l2)) {
                        doesReturn = true;
                        break;
                    }
                }
            } else if (s instanceof While) {
                While whileStatement = (While) s;
                if (doesReturn(whileStatement.list)) {
                    doesReturn = true;
                    break;
                }
            }
        }
        return doesReturn;
    }

    public static StatementResult runSequence(Memory m, List<Statement> list, int continuePoint) {
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
                if (continuePoint == -1) {
                    return n;
                } else {
                    i = continuePoint - 1;
                }
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

    /**
     * Collect the set of types where owned variables or fields are freed. This is
     * to ensure borrowing is only allowed if a function doesn't free fields of this
     * type.
     *
     * @param list
     * @param set
     */
    public static void collectTypes(List<Statement> list, HashSet<DataType> set, MemoryType memoryType) {
        for (int i = 0; list != null && i < list.size(); i++) {
            Statement s = list.get(i);
            s.collectTypes(set, memoryType);
        }
    }

    Value evalConstants(Expression expr) {
        Memory memory = new Memory();
        memory.addFunction(null, null);
        memory.evaluateOnlyConstExpr(true, 1_000_000);
        Value result = expr.eval(memory);
        uncompiledFunctions.putAll(memory.getUncompiledFunctions());
        if (result instanceof ValueRef) {
            // e.g. arrays
            result = memory.getHeap(result.get().longValue());
        }
        return result;
    }

    public List<FunctionDefinition> getFunctions() {
        return new ArrayList<>(functions.values());
    }

    public FunctionDefinition getFunctionById(String functionId) {
        return functions.get(functionId);
    }

    public static String esc(String identifier) {
        if (identifier.length() == 1) {
            return identifier;
        }
        if (identifier.startsWith("0")) {
            return "_" + identifier.substring(1);
        }
        int underscoreIndex = identifier.indexOf('_', 1);
        if (underscoreIndex > 0) {
            if (identifier.toUpperCase(Locale.ENGLISH).equals(identifier)) {
                // constants
                return identifier;
            }
            identifier = identifier.replaceAll("_", "__");
        }
        if (identifier.startsWith("_")) {
            if (identifier.charAt(1) <= 'Z') {
                // uppercase
                return identifier;
            }
            if (identifier.equals("_next")) {
                return identifier;
            }
            // user defined
            return "_u" + identifier;
        }
        if (identifier.indexOf('_') > 0) {
            return identifier;
        }
        return identifier;
        // TODO this needs some more work
        /*
        switch (identifier) {
        case "code":
        case "copyLen":
        case "data":
        case "dateTime":
        case "day":
        case "exceptionType":
        case "File":
        case "filePointer":
        case "hour":
        case "idx":
        case "index":
        case "i32":
        case "i32_array":
        case "len":
        case "mode":
        case "month":
        case "minute":
        case "millis":
        case "newLen":
        case "name":
        case "pos":
        case "result":
        case "second":
        case "shiftLeft":
        case "shiftRight":
        case "this":
        case "year":
            return identifier;
        }
        return "b_" + identifier;
        */
    }

}
