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

    public final static boolean TM_MALLOC = true;
    private final static boolean TRACE_REF_COUNTS = false;

    public final static boolean SIMPLE_REF_COUNTING = true;
    public boolean simpleRefCount = SIMPLE_REF_COUNTING;

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

    public Expression cast(Expression expr, DataType target) {
        if (target.isNullable()) {
            if (expr.type() == null) {
                if (expr instanceof NullValue) {
                    return new NullValue(target);
                }
            } else if (expr.type().orNull() == target) {
                return expr;
            }
        }
        DataType source = expr.type();
        if (source == null) {
            return expr;
        } else if (source == target) {
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
        for(Statement s : initList) {
            buff.append(s);
        }
        for(Statement s : mainList) {
            buff.append(s);
        }
        return buff.toString();
    }

    public String toC() {
        for(Statement s : initList) {
            s.used(this);
        }
        for(Statement s : mainList) {
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
        if (TM_MALLOC) {
            includes.addAll(List.of(StandardLib.TM_MALLOC_INCLUDE.split("\n")));
        }
        for(String i : includes) {
            buff.append("#include " + i + "\n");
        }
        if (TM_MALLOC) {
            buff.append(StandardLib.TM_MALLOC);
            buff.append("#define _malloc(a)      tmmalloc(a)\n");
            buff.append("#define _free(a)        tmfree(a)\n");
        } else {
            buff.append("#define _malloc(a)      malloc(a)\n");
            buff.append("#define _free(a)        free(a)\n");
        }
        if (TRACE_REF_COUNTS) {
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
        if (SIMPLE_REF_COUNTING) {
            // note: __builtin_assume((a)->_refCount > 0) doesn't seem to have an effect
            buff.append("#define _incUse(a)            {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT(\"++  %p line %d, from %d\\n\", a, __LINE__, (a)?(a)->_refCount:0); (a)->_refCount++;}}\n");
            buff.append("#define _decUse(a, type)      {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT(\"--  %p line %d, from %d\\n\", a, __LINE__, (a)->_refCount);if(--((a)->_refCount) == 0)type##_free(a);}}\n");
            // buff.append("#define _decUse(a, type)      {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT(\"--  %p line %d, from %d\\n\", a, __LINE__, (a)->_refCount);if((a)->_refCount < 0) PRINT(\"################ DOUBLE FREE ################\\n\"); if(--((a)->_refCount) == 0)type##_free(a);}}\n");
            buff.append("#define _incUseStack(a)       _incUse(a)\n");
            buff.append("#define _decUseStack(a, type) _decUse(a, type)\n");
        } else {
            // note: __builtin_assume((a)->_refCount > 0) doesn't seem to have an effect
            buff.append("#define _incUse(a)            {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT(\"++  %p line %d, from %d\\n\", a, __LINE__, (a)?(a)->_refCount:0); (a)->_refCount++;}}\n");
            buff.append("#define _decUse(a, type)      {REF_COUNT_INC; if(a && (a)->_refCount < INT32_MAX){PRINT(\"--  %p line %d, from %d\\n\", a, __LINE__, (a)->_refCount);if(--((a)->_refCount) == 0){_addPossiblyFree((void*)a,type##_freeIfUnused);_zeroCountTableGC();}}}\n");
            buff.append("#define _incUseStack(a)       {REF_COUNT_STACK_INC; if(a && (a)->_refCount < INT32_MAX){PRINT(\"+s  %p line %d, from %d\\n\", a, __LINE__, (a)?(a)->_refCount:0);_pushStack(&(a)->_refCount);}}\n");
            buff.append("#define _decUseStack(a, type) {REF_COUNT_STACK_INC; if(a && (a)->_refCount < INT32_MAX){PRINT(\"-s  %p line %d, from %d\\n\", a, __LINE__, (a)->_refCount);if(_popStack()==0)_addPossiblyFree((void*)a,type##_freeIfUnused);}}\n");
        }
        if (!SIMPLE_REF_COUNTING) {
            buff.append("const int _stackReferenceTableSize = 1000;\n"
                    + "const int _zeroCountTableSize = 100;\n"
                    + "const int _zeroCountTableLimit = _zeroCountTableSize - 20;\n"
                    + "int _stackReferenceTableIndex = 0;\n"
                    + "int _zeroCountTableIndex = 0;\n"
                    + "typedef struct _stackReference _stackReference;\n"
                    + "struct _stackReference {\n"
                    + "    int32_t applied;\n"
                    + "    int32_t* refCount;\n"
                    + "};\n"
                    + "_stackReference* _stackReferenceTable;\n"
                    + "typedef struct _zeroCountTableEntry _zeroCountTableEntry;\n"
                    + "struct _zeroCountTableEntry {\n"
                    + "    void* object;\n"
                    + "    int(*freeIfUnused)(void*);\n"
                    + "};\n"
                    + "_zeroCountTableEntry* _zeroCountTable;\n"
                    + "void _initRefCount() {\n"
                    + "    _zeroCountTable = malloc(_zeroCountTableSize * sizeof(_zeroCountTableEntry));\n"
                    + "    _stackReferenceTable = malloc(_stackReferenceTableSize * sizeof(_stackReference));\n"
                    + "    PRINT(\"== initRefCount\\n\");\n"
                    + "}\n"
                    + "void _applyStackIncrements() {\n"
                    + "    PRINT(\"== applyStackIncrements\\n\");\n"
                    + "    for (int i = 0; i < _stackReferenceTableIndex; i++) {\n"
                    + "        _stackReference* ref = &_stackReferenceTable[i];\n"
                    + "        if (ref->applied == 0) {\n"
                    + "            PRINT(\"== ++ %p\\n\", ref->refCount);\n"
                    + "            ref->applied = 1;\n"
                    + "            (*ref->refCount)++;\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n"
                    + "int _compareZeroCountTableEntries(const void* a, const void* b) {\n"
                    + "    _zeroCountTableEntry* entryA = (struct _zeroCountTableEntry*)a;\n"
                    + "    _zeroCountTableEntry* entryB = (struct _zeroCountTableEntry*)b;\n"
                    + "    if (entryA->object < entryB->object) return -1;\n"
                    + "    if (entryA->object > entryB->object) return 1;\n"
                    + "    return 0;\n"
                    + "}\n"
                    + "void _systemGC() {\n"
                    + "    PRINT(\"== systemGC\\n\");\n"
                    + "    _applyStackIncrements();\n"
                    + "    if (_zeroCountTableIndex == 0) {\n"
                    + "        return;\n"
                    + "    }\n"
                    + "    qsort(_zeroCountTable, _zeroCountTableIndex, sizeof(_zeroCountTableEntry), _compareZeroCountTableEntries);\n"
                    + "    int target = 0;\n"
                    + "    PRINT(\"== retain %i %p\\n\", 0, _zeroCountTable[0].object);\n"
                    + "    for (int i = 1; i < _zeroCountTableIndex; i++) {\n"
                    + "        if (_zeroCountTable[i].object != _zeroCountTable[target].object) {\n"
                    + "            target++;\n"
                    + "            _zeroCountTable[target] = _zeroCountTable[i];\n"
                    + "            PRINT(\"== retain %i %p\\n\", i, _zeroCountTable[i].object);\n"
                    + "        } else {\n"
                    + "            PRINT(\"== ignore duplicate %i %p\\n\", i, _zeroCountTable[i].object);\n"
                    + "        }\n"
                    + "    }\n"
                    + "    _zeroCountTableIndex = target + 1;\n"
                    + "    for (int i = 0; i < _zeroCountTableIndex; i++) {\n"
                    + "        PRINT(\"== freeIfUnused %p [%d]\\n\", _zeroCountTable[i].object, i);\n"
                    + "        if (!_zeroCountTable[i].freeIfUnused(_zeroCountTable[i].object)) {\n"
                    + "            PRINT(\"== retain %p\\n\", _zeroCountTable[i].object);\n"
                    + "        } else {\n"
                    + "            PRINT(\"== freed %p\\n\", _zeroCountTable[i].object);\n"
                    + "        }\n"
                    + "    }\n"
                    + "    _zeroCountTableIndex = 0;\n"
                    + "}\n"
                    + "void _zeroCountTableGC() {\n"
                    + "    if (_zeroCountTableIndex >= _zeroCountTableLimit) {\n"
                    + "        if (_zeroCountTableIndex >= _zeroCountTableSize) {\n"
                    + "            fprintf(stdout, \"Zero count table overflow\");\n"
                    + "            exit(1);\n"
                    + "        }\n"
                    + "        _systemGC();\n"
                    + "    }\n"
                    + "}\n"
                    + "void _addPossiblyFree(void* object, int(*freeIfUnused)(void*)) {\n"
                    + "    PRINT(\"== addPossiblyFree %p\\n\", object);\n"
                    + "    _zeroCountTableEntry* e = &_zeroCountTable[_zeroCountTableIndex++];\n"
                    + "    e->object = object;\n"
                    + "    e->freeIfUnused = freeIfUnused;\n"
                    + "}\n"
                    + "void _pushStack(int* refCount) {\n"
                    + "    _stackReference* ref = &_stackReferenceTable[_stackReferenceTableIndex];\n"
                    + "    ref->applied = 0;\n"
                    + "    ref->refCount = refCount;\n"
                    + "    PRINT(\"== pushStack[%d] count=%d\\n\", _stackReferenceTableIndex, *ref->refCount);\n"
                    + "    _stackReferenceTableIndex++;\n"
                    + "}\n"
                    + "int _popStack() {\n"
                    + "    _stackReferenceTableIndex--;\n"
                    + "    PRINT(\"== popStack[%d]\\n\", _stackReferenceTableIndex);\n"
                    + "    _stackReference* ref = &_stackReferenceTable[_stackReferenceTableIndex];\n"
                    + "    PRINT(\"== popStack[%d] applied=%d count=%d\\n\", _stackReferenceTableIndex, ref->applied, *ref->refCount);\n"
                    + "    if (ref->applied == 1) {\n"
                    + "        ref->applied = 0;\n"
                    + "        (*(ref->refCount))--;\n"
                    + "        PRINT(\"== popStack[%d] applied=%d count=%d (dec)\\n\", _stackReferenceTableIndex, ref->applied, *ref->refCount);\n"
                    + "    }\n"
                    + "    return *(ref->refCount);\n"
                    + "}");
        }
        buff.append("int64_t arrayOutOfBounds(int64_t x, int64_t len) {\n"
                + "    fprintf(stdout, \"Array index %lld is out of bounds for the array length %lld\\n\", x, len);\n"
                + "    exit(1);\n"
                + "}\n");
        buff.append("/* types */\n");
        for (DataType t : dataTypeMap.values()) {
            if (t.enumValues != null) {
                continue;
            }
            if (!t.isNumber() && t.isUsed()) {
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
                    buff.append(Statement.indent(t.baseType().toC() + "* data;\n"));
                } else {
                    for (Variable f : t.fields) {
                        buff.append(Statement.indent(f.type().toC() + " " + f.nameC() + ";\n"));
                    }
                }
                if (t.memoryType() == MemoryType.REF_COUNT) {
                    buff.append(Statement.indent("int32_t _refCount;\n"));
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
                    buff.append(Statement.indent("result->_refCount = " + (SIMPLE_REF_COUNTING ? "1" : "0") + ";\n"));
                    buff.append(Statement.indent("return result;\n"));
                    buff.append("}\n");
                } else if (t.isPointer()) {
                    buff.append(t.nameC() + "* " + t.nameC() + "_new() {\n");
                    buff.append(Statement.indent(t.nameC() + "* result = _malloc(sizeof(" + t.nameC() + "));\n"));
                    buff.append(Statement.indent("_traceMalloc(result);\n"));
                    if (t.memoryType() == MemoryType.REF_COUNT) {
                        buff.append(Statement.indent("result->_refCount = " + (SIMPLE_REF_COUNTING ? "1" : "0") + ";\n"));
                    }
                    for (Variable f : t.fields) {
                        if (f.type().isNumber() || !f.type().isCopyType()) {
                            buff.append(Statement.indent("result->" + f.assignmentC() + " = 0;\n"));
                        }
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
                    if (!SIMPLE_REF_COUNTING) {
                        buff.append("int " + t.nameC() + "_freeIfUnused(void* x);\n");
                    }
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
                    if (!SIMPLE_REF_COUNTING) {
                        buff.append("int " + t.nameC() + "_freeIfUnused(void* x) {\n");
                        buff.append(Statement.indent("PRINT(\"== freeIfUnused %p count=%d\\n\", x, ((" + t.nameC() + "*)x)->_refCount);\n"));
                        buff.append(Statement.indent("if (((" + t.nameC() + "*)x)->_refCount == 0) { _free(x); _traceFree(x); return 1; } return 0;\n"));
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
            buff.append(esc("i8_array") + "* str_const(char* data, uint32_t len) {\n");
            buff.append(Statement.indent(esc("i8_array") +"* result = _malloc(sizeof(" + esc("i8_array") + "));\n"));
            buff.append(Statement.indent("result->len = len;\n"));
            // 0 means do not free the memory (it looks like it's already free)
            buff.append(Statement.indent("result->_refCount = INT32_MAX;\n"));
            buff.append(Statement.indent("result->data = (int8_t*) data;\n"));
            buff.append(Statement.indent("return result;\n"));
            buff.append("}\n");
            for (long id: stringConstantsMap.keySet()) {
                if (stringConstantsMap.get(id).isUsed()) {
                    buff.append(esc("i8_array") + "* string_" + id + ";\n");
                }
            }
        }
        if (!arrayConstantsMap.isEmpty()) {
            buff.append(esc("int_array") + "* int_array_const(int64_t* data, uint32_t len) {\n");
            buff.append(Statement.indent(esc("int_array") + "* result = _malloc(sizeof(" + esc("int_array") + "));\n"));
            buff.append(Statement.indent("result->len = len;\n"));
            // 0 means do not free the memory (it looks like it's already free)
            buff.append(Statement.indent("result->_refCount = INT32_MAX;\n"));
            buff.append(Statement.indent("result->data = (int64_t*) data;\n"));
            buff.append(Statement.indent("return result;\n"));
            buff.append("}\n");
            for (long id: arrayConstantsMap.keySet()) {
                buff.append(esc("int_array") + "* array_" + id + ";\n");
            }
        }
        for (Variable var : globalVariables.values()) {
            if (var.isUsed()) {
                buff.append(var.type().toC() + " " + var.nameC() + ";\n");
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
        buff.append("int main(int _argc, char *_argv[]) {\n");
        if (!SIMPLE_REF_COUNTING) {
            buff.append(Statement.indent("_initRefCount();\n"));
        }
        if (TM_MALLOC) {
            buff.append(Statement.indent("tmmalloc_init();\n"));
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
            // buff3.append("{\n");
            for (Statement s : initList) {
                buff3.append(s.toC());
            }
            // buff3.append("}\n");
            buff2.append(Statement.indent(buff3.toString()));
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
        if (!SIMPLE_REF_COUNTING) {
            buff.append(Statement.indent("_systemGC();\n"));
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
