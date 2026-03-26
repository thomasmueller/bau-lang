package org.bau.parser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueArray;
import org.bau.runtime.Value.ValueException;
import org.bau.runtime.Value.ValueI8Array;
import org.bau.runtime.Value.ValuePanic;
import org.bau.runtime.Value.ValueRef;
import org.bau.std.Std;

public class Parser {

    enum TokenType {
        END,
        IDENTIFIER,
        FLOAT,
        INTEGER,
        HEX_INTEGER,
        STRING,
        OPERATOR;
    }

    String text;
    TokenType type;
    String token;
    private String lastComment;
    private int lastPos;
    int pos;
    private int indent;
    private DataType exceptionType;

    // solver for array bound checks etc
    private Solver solver;
    // depth of blocks (indentation depth)
    private int depth;

    // the block id is is incremented for each if / elif / else block
    private Program program;
    private FunctionContext functionContext;
    private boolean isGlobalScope;
    public boolean isImport;
    private int nextConstantId;
    private String module;

    // the stack starting position of the current loop (of the scope)
    private int stackPosLoop;
    // the stack starting position of the function
    // (after the parameters)
    private int stackPosFunction;
    private FunctionDefinition currentFunctionDefinition;
    private Loop currentLoop;
    private boolean scanPhase = true;
    private final int lineOffset;

    public Parser(String moduleRootPath, String text) {
        this(new Program(moduleRootPath), null, text, 0);
    }

    public Parser(Map<String, String> modules, String text) {
        this(new Program(modules), null, text, 0);
    }

    public Parser(String text) {
        this(new Program(Collections.emptyMap()), null, text, 0);
    }

    public Parser(Program program, String module, String text, int lineOffset) {
        this.program = program;
        this.functionContext = new FunctionContext(program, "main");
        this.module = module;
        // add a newline to simplify end detection
        this.text = text + "\n";
        this.lineOffset = lineOffset;
        this.solver = new Solver(program.getSolver());
    }

    public void setScanPhase(boolean scanPhase) {
        this.scanPhase = scanPhase;
    }

    public Program parse() {
        readSpaces();
        Program program = parseProgram();
        if (scanPhase) {
            functionContext.rewindStack(0);
            ArrayList<FunctionDefinition> functions = new ArrayList<>();
            functions.addAll(program.getFunctions());
            if (module == null) {
                FunctionDefinition main = program.getFunctionIfExists(null, null, "main", 0);
                if (main != null) {
                    // move "main" to the end, so a range function is parsed first
                    // (this is only needed if we support custom range functions)
                    functions.remove(main);
                    functions.add(main);
                    if (main.returnType != null) {
                        throw syntaxError("The 'main' method may not return a value; use org.bau.Env.exit instead");
                    }
                }
            }
            for (FunctionDefinition def : functions) {
                // the function could be compiled in the meantime (constants)
                def = program.getFunctionById(def.getFunctionId());
                if (def.code != null) {
                    String f = def.toString();
                    Parser p = new Parser(program, def.module, f, def.lineOffset);
                    functionContext.reset(def.name);
                    p.functionContext = functionContext;
                    p.scanPhase = false;
                    p.parse();
                }
            }
            // there might be more functions now: templates might be expanded
            functions.clear();
            functions.addAll(program.getFunctions());
            for (FunctionDefinition def : functions) {
                if (def.code != null) {
                    String f = def.toString();
                    Parser p = new Parser(program, def.module, f, def.lineOffset);
                    functionContext.reset(def.name);
                    p.functionContext = functionContext;
                    p.scanPhase = false;
                    p.parse();
                }
            }
            if (module == null) {
                FunctionDefinition main = program.getFunctionIfExists(null, null, "main", 0);
                if (main != null) {
                    program.removeFunction(main);
                    program.mainList.addAll(main.list);
                    program.autoClose = main.autoClose;
                }
            }
        }
        return program;
    }

    private int getLine(int pos) {
        int line = 1;
        for(int i=0; i<pos; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return (lineOffset == 0 ? 0 : (lineOffset - 1)) + line;
    }

    private IllegalStateException syntaxError(String message) {
        return syntaxError(message, null);
    }

    private IllegalStateException syntaxError(String message, Exception e) {
        int lineStart = lastPos;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        message += " at line " + getLine(lineStart) + ":\n";
        int lineEnd = text.indexOf('\n', lineStart);
        if (lineEnd < 0) {
            lineEnd = text.length();
        }
        message += text.substring(lineStart, lineEnd) + "\n";
        message += " ".repeat(lastPos - lineStart);
        message += "^".repeat(pos - lastPos);
        IllegalStateException ex = new IllegalStateException(message, e);
        return ex;
    }

    private Program parseProgram() {
        boolean mainStatements = false;
        while (true) {
            while (matchOp(";") || matchOp("\n")) {
                // ok
            }
            if (type == TokenType.END) {
                break;
            }
            if (parseFunctionDefinition(module)) {
                mainStatements = true;
                // ok
            } else if (parseTypeDefinition(module)) {
                mainStatements = true;
                // ok
            } else if (parseTraitDefinition(module)) {
                mainStatements = true;
                // ok
            } else if (parseImport()) {
                mainStatements = true;
                // ok
            } else if (parseModule()) {
                mainStatements = true;
                // ok
            } else if (parseEnumDefinition()) {
                mainStatements = true;
                // ok
            } else {
                if (mainStatements && module == null && program.getFunctionIfExists(null, null, "main", 0) == null) {
                    // there is no main yet: we thread the statements as a main function
                    pos = lastPos;
                    String mainCode = parseBlock(-1);
                    FunctionDefinition def = new FunctionDefinition(getLine(pos));
                    def.name = "main";
                    def.code = Statement.indent(mainCode);
                    program.addFunction(def);
                } else {
                    isGlobalScope = true;
                    parseStatements(program.initList);
                }
            }
        }
        program.autoClose = autoClose(0, null);
        return program;
    }

    private boolean parseModule() {
        if (!match("module")) {
            return false;
        }
        String id = readIdentifier();
        String name = id;
        while (matchOp(".")) {
            id = readIdentifier();
            name += "." + id;
        }
        if (!name.equals(module)) {
            throw syntaxError("The module name '" + name + "' doesn't match the expected '" + module + "'");
        }
        return true;
    }

    private boolean parseImport() {
        if (!match("import")) {
            return false;
        }
        String id = readIdentifier();
        String name = id;
        while (matchOp(".")) {
            id = readIdentifier();
            name += "." + id;
        }
        String old = program.getImport(id);
        boolean alreadyImported = false;
        if (old != null) {
            if (old.equals(name)) {
                alreadyImported = true;
            }
        }
        int oldIndent = indent;
        readEndOfStatement();
        ArrayList<String> entries = new ArrayList<>();
        while (indent > oldIndent) {
            if (!matchOp("\n")) {
                String entry = readIdentifier();
                readEndOfStatement();
                entries.add(entry);
            }
        }
        program.addImport(name, id, entries);
        if (!alreadyImported) {
            String moduleSource = program.readModule(name);
            if (moduleSource == null) {
                throw syntaxError("Resource not found: '" + name + ".bau'");
            }
            try {
                Parser parser = new Parser(program, name, moduleSource, 0);
                parser.isImport = true;
                parser.parse();
            } catch (IllegalStateException e) {
                throw syntaxError("Error parsing module " + name + ": " + e.getMessage(), e);
            }
        }
        for (String e : entries) {
            Variable v = program.getGlobalVariable(name, e);
            if (v != null && !v.isConstant()) {
                throw syntaxError("May not import global constants; use " + name + "." + e + " instead");
            }
        }
        return true;
    }

    private boolean parseTraitDefinition(String targetModule) {
        if (!match("trait")) {
            return false;
        }
        int stackPos = functionContext.getStackPos();
        int defIndent = indent;
        String comment = lastComment;
        String name = readIdentifier();
        if (functionContext.getType(targetModule, name) != null) {
            throw syntaxError("Type '" + name + "' was already defined");
        }
        boolean owned = match("owned");
        MemoryType memoryType = owned ? MemoryType.OWNER : MemoryType.REF_COUNT;
        DataType type = DataType.newTraitType(targetModule, name, memoryType);
        FullName traitName = new FullName(targetModule, name);
        type.setTraitDefinition(new Trait(traitName));
        int functionId = 0;
        if (matchOp(":")) {
            while (true) {
                FullName n = readIdentifierWithPossibleModule();
                type.getTraitDefinition().requiredTraitNames.add(n);
                DataType required = program.getType(n.module, n.name);
                if (required != null) {
                    Trait tr = required.getTraitDefinition();
                    if (tr == null) {
                        throw syntaxError("Type '" + name + "' is not a trait");
                    }
                    for (FunctionDefinition def : tr.functions) {
                        FunctionDefinition d2 = new FunctionDefinition(def.lineOffset);
                        d2.name = def.name;
                        d2.callType = type;
                        d2.varArgs = def.varArgs;
                        for (Variable v : def.parameters) {
                            // can we share the variables?
                            d2.parameters.add(v);
                        }
                        d2.traitFunctionId = def.traitFunctionId;
                        if (functionId <= d2.traitFunctionId) {
                            functionId = d2.traitFunctionId + 1;
                        }
                        d2.returnType = def.returnType;
                        type.getTraitDefinition().functions.add(d2);
                        program.addFunction(d2);
                    }
                }
                if (!matchOp(",")) {
                    break;
                }
            }
        }
        readEndOfStatement();
        functionContext.rewindStack(stackPos);
        program.addComment("trait " + type.toString(), comment);
        lastComment = null;
        while (indent > defIndent) {
            if (!matchOp("\n")) {
                FunctionDefinition def = new FunctionDefinition(getLine(lastPos));
                def.name = readIdentifier();
                def.callType = type;
                matchOp("(");
                Variable var = new Variable("this", type);
                var.setConstantValue(null);
                def.parameters.add(var);
                boolean template = parseFunctionDeclaration(false, targetModule, def);
                if (template) {
                    throw syntaxError("Template are not supported in traits");
                }
                def.traitFunctionId = functionId++;
                type.getTraitDefinition().functions.add(def);
                program.addFunction(def);
            }
        }
        program.addType(type);
        functionContext.rewindStack(stackPos);
        return true;
    }

    /**
     *
     * @param targetModule the module of the function
     * (eg when parsing a List<String>, the target module is the module of List, not String)
     * @return
     */
    private boolean parseTypeDefinition(String targetModule) {
        if (!match("type")) {
            return false;
        }
        int stackPos = functionContext.getStackPos();
        int defIndent = indent;
        String comment = lastComment;
        String name = readIdentifier();
        if (functionContext.getType(targetModule, name) != null) {
            throw syntaxError("Type '" + name + "' was already defined");
        }
        boolean template = false;
        ArrayList<String> parameters = new ArrayList<>();
        if (matchOp("(")) {
            matchOp("\n");
            while (true) {
                String t = readIdentifier();
                parameters.add(t);
                DataType type = DataType.newEmptyType(targetModule, t);
                functionContext.addTemporaryType(type);
                template = true;
                if (matchOp(")")) {
                    break;
                }
                if (!matchOp(",")) {
                    break;
                }
            }
        }
        boolean owned = match("owned");
        int sizeOf = 0;
        ArrayList<FullName> traitNames = new ArrayList<>();
        if (matchOp(":")) {
            while (true) {
                FullName n = readIdentifierWithPossibleModule();
                traitNames.add(n);
                if (!matchOp(",")) {
                    break;
                }
            }
        }
        readEndOfStatement();
        functionContext.rewindStack(stackPos);
        if (template) {
            parseTypeTemplate(defIndent, name, parameters, comment);
            return true;
        }
        MemoryType memoryType = name.charAt(0) > 'Z' ? MemoryType.COPY : MemoryType.REF_COUNT;
        if (owned) {
            if (memoryType == MemoryType.COPY) {
                throw syntaxError("Value types can not be owned");
            }
            memoryType = MemoryType.OWNER;
        }
        DataType type = DataType.newRegularType(targetModule, name, sizeOf, memoryType);
        // need to add it first, because one of the fields could be of this type
        program.addType(type);
        String title = "type " + type.toString();
        if (memoryType == MemoryType.OWNER) {
            title += " owned";
        }
        program.addComment(title, comment);
        lastComment = null;
        ArrayList<Variable> fields = new ArrayList<>();
        while (indent > defIndent) {
            if (!matchOp("\n")) {
                String fieldName = readIdentifier();
                DataType fieldType = readType(false);
                readEndOfStatement();
                sizeOf += fieldType.sizeOf();
                Variable var = new Variable(fieldName, fieldType);
                fields.add(var);
            }
        }
        type.addFields(fields);
        if (!parameters.isEmpty()) {
            type.parameters = parameters;
        }
        functionContext.rewindStack(stackPos);
        type.traitNames.addAll(traitNames);
        defineConstructor(type);
        return true;
    }

    private void defineConstructor(DataType type) {
        FunctionDefinition def = new FunctionDefinition(0);
        def.isConstructor = true;
        def.module = type.module();
        def.name = type.name();
        def.returnType = type;
        New n = new New(type, null);
        Variable result = assignTempVariable(def.list, n);
        for (Variable var : type.fields) {
            Assignment assign = new Assignment();
            assign.type = var.type();
            assign.initial = true;
            assign.leftValue = new FieldAccess(result, var.name(), var.type());
            if (var.type().isCopyType() && var.type().isNumber()) {
                assign.value = var.type().nullExpression();
            } else {
                Variable arg = new Variable(var.name(), var.type());
                def.parameters.add(arg);
                assign.value = arg;
            }
            def.list.add(assign);
        }
        Return ret = new Return(result);
        def.list.add(ret);
        program.addFunction(def);
    }

    private void parseTypeTemplate(int defIndent, String name, ArrayList<String> parameters, String comment) {
        int lastPos = pos;
        String code = parseBlock(defIndent);
        DataType type = DataType.newEmptyType(module, name);
        type.parameters = parameters;
        type.lineOffset = getLine(lastPos);
        type.template = code;
        lastComment = null;
        program.addComment("type " + type.toString(), comment);
        lastComment = null;
        program.addType(type);
    }

    private String parseBlock(int defIndent) {
        int pos = lastPos;
        while (text.charAt(pos) != '\n') {
            pos--;
        }
        pos++;
        if (pos >= text.length()) {
            return "";
        }
        while (true) {
            if (type == TokenType.OPERATOR && "\n".equals(token)) {
                readSpaces();
            }
            if (type == TokenType.END || indent <= defIndent) {
                break;
            }
            read();
        }
        return text.substring(pos, lastPos);
    }

    private boolean parseEnumDefinition() {
        if (!match("enum")) {
            return false;
        }
        String comment = lastComment;
        int defIndent = indent;
        String id = readIdentifier();
        readEndOfStatement();
        LinkedHashMap<String, Long> entries = new LinkedHashMap<>();
        HashMap<Long, String> map = new HashMap<>();
        long nextValue = 0;
        while (indent > defIndent) {
            if (!matchOp("\n")) {
                String name = readIdentifier();
                if (matchOp(":")) {
                    Expression expr = parseExpression();
                    if (expr.type().isFloatingPoint() || expr.type().isNullable() || !expr.type().isNumber()) {
                        throw syntaxError("Only integer types are supported");
                    }
                    Value v = eval(expr, false);
                    long x = v.longValue();
                    if (map.containsKey(x)) {
                        throw syntaxError("This value is already used by '" + map.get(x) + "'");
                    }
                    if (entries.containsKey(name)) {
                        throw syntaxError("Duplicate name '" + name + "'");
                    }
                    nextValue = x;
                } else {
                    while (map.containsKey(nextValue)) {
                        nextValue++;
                    }
                }
                map.put(nextValue, name);
                entries.put(name, nextValue);
                nextValue++;
                readEndOfStatement();
            }
        }
        DataType type = DataType.newEnumType(module, id);
        type.enumValues = entries;
        program.addType(type);
        program.addComment("enum " + type.toString(), comment);
        lastComment = null;
        return true;
    }

    private boolean parseFunctionDefinition(String targetModule) {
        int startParse = lastPos;
        if (!match("fun")) {
            return false;
        }
        if (functionContext.getStackPos() != 0) {
            // TODO currently we do parse functions while parsing functions...
            // throw new IllegalStateException();
        }
        String comment = lastComment;
        currentLoop = null;
        int defIndent = indent;
        isGlobalScope = false;
        DataType callType;
        String id = null;
        boolean template = DataType.isGenericTypeName(token);
        if (template) {
            DataType type = DataType.newEmptyType(targetModule, token);
            functionContext.addTemporaryType(type);
            callType = readType(true);
        } else {
            id = readIdentifier();
            callType = functionContext.getType(targetModule, id);
        }
        String typeName = id;
        if (callType != null) {
            if (matchOp("[")) {
                if (!matchOp("]")) {
                    throw syntaxError("Expected ']', got '" + token + "' when reading type");
                }
                callType = callType.arrayType();
            }
        }
        if (callType != null && callType.template != null) {
            if (!matchOp("(")) {
                throw syntaxError("Expected '(', got '" + token + "' when reading a function definition template");
            }
            matchOp("\n");
            for (int i = 0; i < callType.parameters.size(); i++) {
                String p = readIdentifier();
                String expected = callType.parameters.get(i);
                if (!p.equals(expected)) {
                    throw syntaxError("Expected '" + expected + "' , got '" + p
                            + "' when reading a function definition template");
                }
                matchOp(",");
            }
            if (!matchOp(")")) {
                throw syntaxError("Expected ')', got '" + token + "' when reading a function definition template");
            }
            parseTypeFunctionTemplate(defIndent, callType);
            return true;
        }
        int stackPos = functionContext.getStackPos();
        FunctionDefinition def = new FunctionDefinition(getLine(lastPos));
        if (currentFunctionDefinition != null) {
            throw new IllegalStateException();
        }
        currentFunctionDefinition = def;
        def.module = targetModule;
        // stack position is before the "this" parameter
        // (which also may need to be incremented, if the function returns "this")
        stackPosFunction = functionContext.getStackPos();
        if (matchOp("(")) {
            matchOp("\n");
            def.name = id;
        } else {
            if (callType == null && !template) {
                throw syntaxError("Type not found: " + typeName);
            }
            def.callType = callType;
            def.name = readIdentifier();
            if (!matchOp("(")) {
                throw syntaxError("Expected '(', got '" + token + "' when reading a function definition");
            }
            matchOp("\n");
            if (type == null) {
                throw syntaxError("Type '" + id + "' not found when reading a function definition");
            }
            Variable var = new Variable("this", callType);
            // "this" can not be re-assigned
            var.setConstantValue(null);

            def.parameters.add(var);
            functionContext.addVariable(var);
        }
        template = parseFunctionDeclaration(template, targetModule, def);
        FunctionDefinition old = program.getFunctionIfExists(def.callType, def.module, def.name, def.parameters.size());
        if (old != null) {
            if (old.list.isEmpty()) {
                program.removeFunction(old);
                // this ensures it is not called
                old.list = null;
            } else {
                throw syntaxError("Function '" + def.name + "' already has an implementation");
            }
        }
        if (scanPhase && template) {
            parseFunctionTemplate(defIndent, def);
            functionContext.rewindStack(stackPos);
            currentFunctionDefinition = null;
            return true;
        }
        if (scanPhase || template) {
            int startCode = lastPos;
            String s = parseBlock(defIndent);
            String header = text.substring(startParse, startCode).trim() + "\n";
            if (comment != null) {
                header = "##\n" + comment + "\n##\n" + header;
            }
            def.header = header;
            def.code = s;
            def.comment = comment;
            program.addFunction(def);
            functionContext.rewindStack(stackPos);
            currentFunctionDefinition = null;
            return true;
        }
        for (Variable var : def.parameters) {
            if (var.name().equals("this") && var.isConstant()) {
                // "this" is not null (if it is constant, which allows "this" to be a parameter for other functions)
                setBlockCondition(var, false, false);
            } else if (var.type().memoryType() == MemoryType.OWNER) {
                // not null, until used
                setBlockCondition(var, false, false);
            }
        }

        program.addComment(def.toString(), comment);
        program.addFunction(def);
        startBlock(false, null);
        while (true) {
            if (indent <= defIndent) {
                break;
            }
            parseStatements(def.list);
        }
        if (def.exceptionType != null && def.returnType == null) {
            def.list.add(new Return(null));
        }
        List<Statement> autoClose = autoClose(stackPosFunction, null);
        List<Statement> ownedParameters = new ArrayList<>();
        autoClose.addAll(ownedParameters);
        // we don't need to increment + decrement the function arguments,
        // if the function doesn't return this type
        for (int i = 0; i < autoClose.size(); i++) {
            Statement s = autoClose.get(i);
            if (s instanceof Free) {
                Free free = (Free) s;
                if (free.var.type() == def.returnType) {
                    continue;
                }
                for (int j = 0; j < def.parameters.size(); j++) {
                    if (def.varArgs && j == def.parameters.size() - 1) {
                        // var arg array needs to be freed
                        continue;
                    }
                    if (def.parameters.get(j) == free.var) {
                        if (free.var.reassignCount == 0) {
                            free.var.skipIncrementDecrementRefCount = true;
                        }
                        break;
                    }
                }
            }
        }
        def.autoClose(autoClose);
        functionContext.rewindStack(stackPos);
        currentLoop = null;
        endBlock();
        if (depth != 0) {
            throw new IllegalStateException();
        }
        solver.clear();
        if (currentFunctionDefinition.returnType != null) {
            if (!Program.doesReturn(currentFunctionDefinition.list)) {
                throw syntaxError("Function does not return or throw");
            }
        }
        if (Variable.SSA_FORM && functionContext.blockList.isEmpty()) {
            BasicBlock header = functionContext.newBasicBlock();
            for (Variable v : currentFunctionDefinition.parameters) {
                header.setVariableVersion(v.name(), 0);
            }
            functionContext.linkBasicBlocks(currentFunctionDefinition.list, header, null, null);
            functionContext.setBasicBlocksVariableVersions();
            functionContext.optimizeSkipIncrementDecrementRefCounts(currentFunctionDefinition);
            if (Variable.DEBUG_VERSIONS) {
                functionContext.printBasicBlocks();
            }
        }

        currentFunctionDefinition = null;
        if (def.macro) {
            Templates.checkMacroFunction(def);
            program.addFunctionTemplate(callType, module, def.name, def);
        }
        return true;
    }

    private boolean parseFunctionDeclaration(boolean template, String targetModule, FunctionDefinition def) {
        boolean varArgs = false;
        DataType itType = null;
        if (!matchOp(")")) {
            HashSet<String> templateTypes = new HashSet<>();
            while (true) {
                String name = readIdentifier();
                if (matchOp("(")) {
                    itType = readType(true);
                    def.itType = itType;
                    if (!matchOp(")")) {
                        throw syntaxError("Expected ')'");
                    }
                }
                DataType type;
                if (DataType.isGenericTypeName(token) && !templateTypes.contains(token)) {
                    templateTypes.add(token);
                    template = true;
                    type = DataType.newEmptyType(targetModule, token);
                    functionContext.addTemporaryType(type);
                    type = readType(template);
                    if (matchOp("..")) {
                        varArgs = true;
                        type = type.arrayType();
                    }
                    Variable var = new Variable(name, type);
                    def.parameters.add(var);
                    functionContext.addVariable(var);
                } else if (match("type")) {
                    template = true;
                    type = program.getType(null, DataType.TYPE);
                    templateTypes.add(name);
                    DataType t = DataType.newEmptyType(targetModule, name);
                    functionContext.addTemporaryType(t);
                    // we change the variable name, because the name is already a type
                    Variable var = new Variable("_" + name, type);
                    def.parameters.add(var);
                    functionContext.addVariable(var);
                } else {
                    type = readType(template);
                    if (matchOp("..")) {
                        varArgs = true;
                        type = type.arrayType();
                    }
                    Variable var = new Variable(name, type);
                    if (type.isRange()) {
                        setRangeBounds(var);
                    }
                    if (type.memoryType() == MemoryType.OWNER) {
                        if (varArgs) {
                            throw syntaxError("Owned var-args are not supported");
                        }
                    }
                    def.parameters.add(var);
                    functionContext.addVariable(var);
                }
                if (varArgs) {
                    if (!matchOp(")")) {
                        throw syntaxError("Expected ')', got " + token);
                    }
                    break;
                } else {
                }
                if (matchOp(")")) {
                    break;
                }
                if (!matchOp(",")) {
                    break;
                }
                // new line after operation
                matchOp("\n");
            }
        }
        def.varArgs = varArgs;
        if (match("const")) {
            def.constExpr = true;
        }
        if (match("macro")) {
            def.macro = true;
        }
        if (itType != null && !def.macro) {
            throw syntaxError("Hacks are only allowed in macros");
        }
        if (matchOp("\n")) {
            // void
        } else {
            if (match("throws")) {
                def.exceptionType = readType(false);
            } else {
                def.returnType = readType(template);
                if (match("throws")) {
                    def.exceptionType = readType(false);
                }
            }
            if (def.exceptionType != null) {
                if (def.exceptionType.needIncDec()) {
                    throw syntaxError("May only throw value types");
                }
                boolean found = false;
                for (Variable f : def.exceptionType.fields) {
                    if (f.name().equals("exceptionType")) {
                        if (f.type() != program.getType(null, DataType.INT)) {
                            throw syntaxError("The field 'exceptionType' must be of type 'int'");
                        }
                        found = true;
                    }
                }
                if (!found) {
                    throw syntaxError("The exception type needs to have an integer field 'exceptionType'");
                }
            }
            readEndOfStatement();
        }
        return template;
    }

    private void parseTypeFunctionTemplate(int defIndent, DataType t) {
        String comment = lastComment;
        int start = lastPos;
        // read to end of the line
        while (true) {
            if (type == TokenType.OPERATOR && "\n".equals(token)) {
                readSpaces();
                break;
            }
            read();
        }
        String functionName = text.substring(start, lastPos).trim();
        String code = parseBlock(defIndent);
        StringBuilder buff = new StringBuilder();
        buff.append("fun ").append(t.name());
        for (String p : t.parameters) {
            buff.append("_@@").append(p + "_").append("@@");
        }
        for (String p : t.parameters) {
            String find = t.name() + "(" + p + ")";
            String replace = t.name() + "_@@" + p + "_@@";
            if (functionName.indexOf(find) >= 0) {
                functionName = functionName.replace(find, replace);
            }
        }
        buff.append(" " + functionName + "\n");
        buff.append(code);
        t.lineOffset = getLine(lastPos);
        t.template += "\n" + buff.toString();
        if (comment != null) {
            program.addComment("fun " + t.toString() + " " + functionName.trim(), comment);
        }
    }

    private void parseFunctionTemplate(int defIndent, FunctionDefinition def) {
        String s = parseBlock(defIndent);
        if (program.getFunctionTemplate(def.callType, def.module, def.name) != null) {
            throw syntaxError("Function template '" + def.name + "' was already defined");
        }
        def.template = s;
        program.addFunctionTemplate(def.callType, def.module, def.name, def);
    }

    /**
     * Read a type definition. Either a template type can be returned (eg. List(T)),
     * or a concrete type is needed (eg. List(int)).
     *
     * @param templatesOk whether type templates are OK (e.g. List(T))
     * @return the type
     */
    private DataType readType(boolean templatesOk) {
        return readType(templatesOk, true);
    }

    private DataType readType(boolean templatesOk, boolean arraysOk) {
        if (DataType.TYPE.equals(token)) {
            throw syntaxError("Type '" + token + "' may not be used here");
        }
        if ("fun".equals(token)) {
            read();
            if (!matchOp("(")) {
                throw syntaxError("Expected '('");
            }
            ArrayList<DataType> params = new ArrayList<>();
            if (!matchOp(")")) {
                while (true) {
                    DataType t = readType(false, true);
                    params.add(t);
                    if (matchOp(",")) {
                        // next
                    } else if (matchOp(")")) {
                        break;
                    } else {
                        throw syntaxError("Expected ')'");
                    }
                }
            }
            DataType returnType = null;
            if (type == TokenType.IDENTIFIER) {
                returnType = readType(false, true);
            }
            DataType fp = DataType.newFunctionPointer(module, params, returnType);
            return fp;
        }
        if ("0".equals(token)) {
            read();
            if (matchOp("..")) {
                Expression upperBound = parseExpression();
                if (upperBound.canThrowException() != null) {
                    throw syntaxError("May not throw an exception here");
                }
                String rangeTypeName = "0.." + upperBound.toString();
                DataType t = functionContext.getType(null, rangeTypeName);
                if (t != null) {
                    return t;
                }
                DataType newType = DataType.newNumberType(rangeTypeName, 8);
                newType.maxValue = upperBound;
                functionContext.addTemporaryType(newType);
                return newType;
            }
        }
        boolean borrow = false;
        if (matchOp("&")) {
            borrow = true;
        }
        String name = readIdentifier();
        while (matchOp(".")) {
            name += "." + readIdentifier();
        }
        String m;
        m = program.getImportEntry(name);
        if (m == null) {
            m = module;
        }
        DataType t = functionContext.getType(m, name);
        if (t == null) {
            throw syntaxError("Type '" + name + "' not found when reading a type");
        }
        if (t.template != null) {
            ArrayList<DataType> params = parseTypeParameters(t, templatesOk);
            // if templates can be returned, then
            // we must not replace the types
            if (!templatesOk) {
                t = parseTemplatedType(t, params);
            }
        }
        if (arraysOk && matchOp("[")) {
            if (!matchOp("]")) {
                throw syntaxError("Expected ']', got '" + token + "' when reading a type");
            }
            t = t.arrayType();
        }
        if (borrow) {
            if (t.memoryType() != MemoryType.REF_COUNT) {
                throw syntaxError("Not a pointer type");
            }
            t = t.borrowType();
        }
        if (matchOp("?")) {
            if (t.isArray()) {
                throw syntaxError("Arrays can't be null (but they can be empty)");
            } else if (t.isCopyType()) {
                // ignore, to support templates
                // throw syntaxError("Numbers and value types can't be be null (but the value can be zero)");
            } else {
                t = t.orNull();
            }
        }
        return t;
    }

    private ArrayList<DataType> parseTypeParameters(DataType t, boolean templatesOk) {
        String name = t.name();
        if (!matchOp("(")) {
            throw syntaxError("Type '" + name + "' is a template; need to specify the parameters");
        }
        matchOp("\n");
        ArrayList<DataType> params = new ArrayList<>();
        for (int i = 0; i < t.parameters.size(); i++) {
            DataType t2 = readType(templatesOk);
            params.add(t2);
            matchOp(",");
        }
        if (!matchOp(")")) {
            throw syntaxError("Type '" + name + "' is a template; need to specify " + t.parameters.size() + " parameters");
        }
        return params;
    }

    private DataType parseTemplatedType(DataType t, ArrayList<DataType> params) {
        String name = t.name();
        String typeId = DataType.getId(name, params);
        DataType t2 = functionContext.getType(t.module(), typeId);
        if (t2 != null) {
            t = t2;
        } else {
            String code = t.template;
            ArrayList<String> with = new ArrayList<>();
            for (int i = 0; i < t.parameters.size(); i++) {
                with.add(params.get(i).fullName());
            }
            code = Templates.convertTemplate(code, t.parameters, with, program);
            code = "type " + typeId + "\n" + code;
            try {
                Parser p = new Parser(program, module, code, t.lineOffset);
                p.scanPhase = false;
                p.read();
                p.parseTypeDefinition(t.module());
                while (p.type != TokenType.END) {
                    p.parseFunctionDefinition(t.module());
                }
                t = functionContext.getType(t.module(), typeId);
            } catch (IllegalStateException e) {
                throw syntaxError("Error parsing template: " + e.getMessage(), e);
            }
        }
        return t;
    }

    private void parseStatements(ArrayList<Statement> target) {
        if (matchOp("\n")) {
            return;
        }
        if (type == TokenType.IDENTIFIER) {
            // if we parse inside an 'if' statement etc,
            // then we are no longer on the global scope
            // (variables are no longer global)
            boolean previousGlobal = isGlobalScope;
            isGlobalScope = false;
            try {
                if (match("if")) {
                    parseIf(target);
                    return;
                } else if (match("loop")) {
                    parseLoop(target);
                    return;
                } else if (match("while")) {
                    parseLoop(target);
                    return;
                } else if (match("for")) {
                    parseFor(target);
                    return;
                } else if (match("switch")) {
                    parseSwitch(target);
                    return;
                } else if (match("break")) {
                    parseBreak(target);
                    return;
                } else if (match("continue")) {
                    parseContinue(target);
                    return;
                } else if (match("return")) {
                    parseReturn(target);
                    return;
                } else if (match("throw")) {
                    parseThrow(target);
                    return;
                } else if (match("catch")) {
                    parseCatch(target);
                    return;
                }
            } finally {
                isGlobalScope = previousGlobal;
            }
            String m = module;
            ArrayList<String> identifierList = new ArrayList<>();
            while (true) {
                String identifier1 = readIdentifier();
                if (identifier1.equals("fun")) {
                    if ("main".equals(currentFunctionDefinition.name)) {
                        throw syntaxError("Trying to define a function inside the main function. Note that any statement starts the main function implicitly.");
                    }
                    throw syntaxError("Trying to define a function inside a function");
                }
                // TODO this is duplicate source code
                // if there is no variable with this name, and
                // no function with this name, and
                // no this.field with this name,
                // then it could be a fully qualified module name
                Variable thisVar = functionContext.getVariable(null, "this");
                if (functionContext.getVariable(null, identifier1) == null &&
                        functionContext.getType(m, identifier1) == null &&
                        (thisVar == null || thisVar.type().getFieldDataType(identifier1) == null)) {
                    while (matchOp(".")) {
                        m = m == module ? identifier1 : m + "." + identifier1;
                        identifier1 = readIdentifier();
                    }
                    String m2 = program.getImport(m);
                    if (m2 != null) {
                        m = m2;
                    }
                }
                identifierList.add(identifier1);
                if (!matchOp(",")) {
                    break;
                }
            }
            DataType targetType = null;
            if (type == TokenType.IDENTIFIER) {
                targetType = readType(true);
            }
            if (matchOp(":=")) {
                if (m != module && !m.equals(module)) {
                    throw syntaxError("Can not create a new variable in a different module");
                }
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, true, target);
                if (expr instanceof NullValue) {
                    if (targetType == null) {
                        throw syntaxError("Can only assign null if the type is known");
                    }
                    expr = new NullValue(targetType);
                }
                DataType type = expr.type();
                if (type.isArray()) {
                    throw syntaxError("Arrays need to be declared as constants to simplify array-bound verification");
                }
                boolean global = isGlobalScope;
                if (matchOp("..")) {
                    if (global) {
                        throw syntaxError("Global ranges are not allowed; they need to be in a function");
                    }
                    if (!"0".equals(expr.toString())) {
                        throw syntaxError("Range needs to start from 0: '" + expr + "'");
                    }
                    Expression upperBound = parseExpression();
                    if (upperBound.canThrowException() != null) {
                        throw syntaxError("May not throw an exception here");
                    }
                    String rangeTypeName = "0.." + upperBound.toString();
                    DataType rangeType = functionContext.getType(null, rangeTypeName);
                    if (rangeType == null) {
                        rangeType = DataType.newNumberType(rangeTypeName, 8);
                        rangeType.maxValue = upperBound;
                        functionContext.addTemporaryType(rangeType);
                    }
                    type = rangeType;
                }
                if (targetType != null) {
                    Expression cast = program.cast(expr, false, targetType);
                    if (cast == null) {
                        throw syntaxError("Need explicit cast for " + expr.type() + " to " + targetType);
                    }
                    expr = cast;
                    type = cast.type();
                }
                if (targetType != null && !targetType.equals(expr.type())) {
                    if (targetType.isNullable() && targetType.equals(type.orNull())) {
                        type = targetType;
                    } else {
                        throw syntaxError("The type of the variable is different than the type of the expression");
                    }
                }
                for (String identifier : identifierList) {
                    Assignment s = new Assignment();
                    s.initial = true;
                    s.isGlobalScope = global;
                    // no need for temp variables as it's just an assignment
                    s.value = expr;
                    s.type = type;
                    Variable v = new Variable(module, identifier, global, s.type);
                    s.leftValue = v;
                    if (global) {
                        program.addGlobalVariable(v);
                    } else {
                        if (functionContext.getVariable(module, v.name()) != null) {
                            throw syntaxError("Variable '" + v.name() + "' already exists");
                        }
                        functionContext.addVariable(v);
                    }
                    if (type.isRange()) {
                        setRangeBounds(v);
                    }
                    verifyBounds(s);
                    s.setBounds(solver, depth, false);
                    target.add(s);
                }
                readEndOfStatement();
                return;
            }
            if (matchOp(":")) {
                if (m != module && !m.equals(module)) {
                    throw syntaxError("Can not define a constant in a different module");
                }
                Expression expr = parseExpression();
                if (expr instanceof NullValue) {
                    if (targetType == null) {
                        throw syntaxError("Can only assign null if the type is known");
                    }
                    expr = new NullValue(targetType);
                }
                expr = expr.writeStatements(this, true, target);
                if (expr == null) {
                    throw syntaxError("Expression required");
                }
                Value constValue = eval(expr, true);
                if (constValue != null) {
                    if (constValue.isArray() || constValue instanceof Value.ValueRef) {
                        // the value would be in the heap, but we don't retain the heap
                        // here, so the data would be lost
                        constValue = null;
                    }
                }
                if (identifierList.size() != 1) {
                    throw syntaxError("Constant lists are not supported");
                }
                String identifier = identifierList.get(0);
                Assignment s = new Assignment();
                s.isConstant = true;
                s.isGlobalScope = isGlobalScope;
                s.initial = true;
                if (targetType != null && !targetType.equals(expr.type())) {
                    expr = program.cast(expr, false, targetType);
                    if (expr == null) {
                        throw syntaxError("The type of the variable is different than the type of the expression");
                    }
                }
                s.value = expr;
                boolean global = isGlobalScope;
                Variable v = new Variable(module, identifier, global, s.value.type());
                v.setConstantValue(constValue);
                s.leftValue = v;
                s.setConstantBounds(solver, v, expr);
                s.type = s.value.type();
                if (global && !identifier.toUpperCase(Locale.ROOT).equals(identifier)) {
                    if (!s.type.isArray()) {
                        throw syntaxError("Global constants need to be all caps: " + identifier);
                    }
                }
                if (functionContext.getVariable(null, v.name()) != null) {
                    throw syntaxError("Variable already defined: " + v.name());
                }
                functionContext.addVariable(v);
                if (global) {
                    program.addGlobalVariable(v);
                }
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("(")) {
                matchOp("\n");
                if (identifierList.size() != 1) {
                    throw syntaxError("Function lists are not supported");
                }
                String identifier = identifierList.get(0);
                if ("native".equals(identifier)) {
                    String s = token.trim();
                    read();
                    if (!matchOp(")")) {
                        throw syntaxError("Expected ')'");
                    }
                    while (s.startsWith("#include ")) {
                        int index = s.indexOf('\n');
                        if (index < 0) {
                            break;
                        }
                        String include = s.substring(0, index);
                        include = include.substring("#include ".length());
                        program.addIncludeC(include);
                        s = s.substring(index + 1).trim();
                    }
                    readEndOfStatement();
                    target.add(new NativeCode(s + "\n"));
                    return;
                }
                if (m == null) {
                    m = program.getImportEntry(identifier);
                }
                Call call = new Call();
                call.statement = true;
                Expression expr;
                DataType t = null;
                while (true) {
                    expr = parseCall(t, m, identifier, call, true);
                    expr = expr.writeStatements(this, false, target);
                    if (expr == null) {
                        // eg. ternary expression
                        break;
                    }
                    t = expr.type();
                    if (t == null || !matchOp(".")) {
                        break;
                    }
                    // chained call
                    call = new Call();
                    call.statement = true;
                    call.args.add(expr);
                    matchOp("\n");
                    identifier = readIdentifier();
                    if (!matchOp("(")) {
                        throw syntaxError("Only method calls are supported here");
                    }
                }
                readEndOfStatement();
                if (expr instanceof Call) {
                    target.add((Call) expr);
                }
                return;
            } else if (matchOp("\n") && targetType != null) {
                if (identifierList.size() != 1) {
                    throw syntaxError("Declaration lists are not supported");
                }
                String identifier = identifierList.get(0);
                Assignment s = new Assignment();
                s.initial = true;
                Expression expr;
                if (targetType.isNumber()) {
                    expr = new NumberValue(Value.ValueInt.ZERO, targetType, false);
                } else {
                    targetType = targetType.orNull();
                    expr = new NullValue(targetType);
                }
                if (targetType != null && !targetType.equals(expr.type())) {
                    expr = program.cast(expr, false, targetType);
                    if (expr == null) {
                        throw syntaxError("The type of the variable is different than the type of the expression");
                    }
                }
                s.value = expr;
                boolean global = isGlobalScope;
                Variable v = new Variable(module, identifier, global, targetType);
                s.leftValue = v;
                s.type = targetType;
                if (functionContext.getVariable(module, v.name()) != null) {
                    throw syntaxError("Variable '" + v.name() + "' already exists");
                }
                functionContext.addVariable(v);
                if (global) {
                    program.addGlobalVariable(v);
                }
                verifyBounds(s);
                // already read
                // readEndOfStatement();
                target.add(s);
                return;
            }
            if (identifierList.size() != 1) {
                throw syntaxError("Lists are currently not supported");
            }
            String identifier = identifierList.get(0);
            LeftValue left = functionContext.getVariable(module, identifier);
            if (left == null) {
                left = functionContext.getVariable(null, "this");
                if (left == null) {
                    throw syntaxError("Variable not found: '" + identifier
                            + "' \n"
                            + "(constants are declared with ':', "
                            + "new variable are declared with ':=')");
                }
                verifyNullAccess(left);
                DataType vt = left.type();
                DataType type = vt.getFieldDataType(identifier);
                if (type == null) {
                    throw syntaxError("Variable not found: '" + identifier
                            + "' \n"
                            + "(constants are declared with ':', "
                            + "new variable are declared with ':=')");
                }
                left = new FieldAccess(left, identifier, type);
            }
            while (true) {
                if (matchOp(".")) {
                    // TODO duplicate code, in parsePossibleDot
                    if (left.type().isPointer()) {
                        verifyNullAccess(left);
                    }
                    String f = readIdentifier();
                    if (matchOp("(")) {
                        matchOp("\n");
                        Call call = new Call();
                        call.args.add(left);
                        Expression expr = parseCall(left.type(), m, f, call, true);
                        if (expr instanceof Call) {
                            call = (Call) expr;
                            if (".".equals(token)) {
                                // chained call
                                left = call;
                            } else {
                                readEndOfStatement();
                                call.statement = true;
                                expr = call.writeStatements(this, false, target);
                                if (expr instanceof Call) {
                                    // could return a variable
                                    target.add((Call) call);
                                }
                                return;
                            }
                        } else {
                            // eg calling a macro that doesn't return anything
                            expr.writeStatements(this, false, target);
                            readEndOfStatement();
                            return;
                        }
                    } else {
                        DataType type;
                        if ("len".equals(f) && left.type().isArray()) {
                            type = program.getType(null, DataType.I32);
                        } else {
                            type = left.type().getFieldDataType(f);
                        }
                        if (type == null) {
                            throw syntaxError("Field '" + f + "' not found in type '" + left.type() + "'");
                        }
                        left = new FieldAccess(left, f, type);
                    }
                } else if (matchOp("[")) {
                    Expression arrayIndex = parseExpression();
                    boolean checkBounds = needBoundsCheck(left, arrayIndex);
                    if (matchOp("]!")) {
                        if (checkBounds) {
                            throw syntaxError("Expected ']', got '" + token + "' when reading from an array that needs a bounds check (use '[index]' instead of '[index]!').");
                        }
                    } else if (!matchOp("]")) {
                        throw syntaxError("Expected ']', got '" + token + "' in array access");
                    }
                    left = new ArrayAccess(left, arrayIndex, checkBounds);
                } else {
                    break;
                }
            }
            Assignment s = new Assignment();
            s.leftValue = left;
            if (left.isConstant()) {
                throw syntaxError("Can not modify constant '" + left + "'");
            }
            if (matchOp("=")) {
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                if (targetType != null && !targetType.equals(expr.type())) {
                    expr = program.cast(expr, false, targetType);
                    if (expr == null) {
                        throw syntaxError("The type of the variable is different than the type of the expression");
                    }
                }
                s.value = expr;
                // this possibly updates the reference,
                // and so it's important we have a temp variable
                s.type = s.value.type();
                if (s.leftValue instanceof Variable && s.type != null && s.type.isArray()) {
                    throw syntaxError("Arrays can not be re-assigned to simplify array-bound verification");
                }
                if (s.value instanceof NullValue) {
                    // for templates, we want to support assignment to null
                    s.value = left.type().nullExpression();
                }
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            }
            if (matchOp("*=")) {
                s.modify = "*";
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                s.value = expr;
                s.type = s.value.type();
                if (targetType != null && !targetType.equals(s.value.type())) {
                    throw syntaxError("The type of the variable is different than the type of the expression");
                }
                convertToExpandedForm(s);
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            } else if (matchOp("/=")) {
                s.modify = "/";
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                s.value = expr;
                s.type = s.value.type();
                if (targetType != null) {
                    if (!targetType.equals(s.value.type())) {
                        throw syntaxError("The type of the variable is different than the type of the expression");
                    }
                    if (!targetType.isFloatingPoint()) {
                        verifyNotZero(expr);
                    }
                }
                convertToExpandedForm(s);
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            } else if (matchOp("%=")) {
                s.modify = "%";
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                s.value = expr;
                s.type = s.value.type();
                if (targetType != null) {
                    if (!targetType.equals(s.value.type())) {
                        throw syntaxError("The type of the variable is different than the type of the expression");
                    }
                    if (!targetType.isFloatingPoint()) {
                        verifyNotZero(expr);
                    }
                }
                convertToExpandedForm(s);
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            } else if (matchOp("+=")) {
                s.modify = "+";
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                s.value = expr;
                s.type = s.value.type();
                if (targetType != null && !targetType.equals(s.value.type())) {
                    throw syntaxError("The type of the variable is different than the type of the expression");
                }
                convertToExpandedForm(s);
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            } else if (matchOp("-=")) {
                s.modify = "-";
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                s.value = expr;
                s.type = s.value.type();
                if (targetType != null && !targetType.equals(s.value.type())) {
                    throw syntaxError("The type of the variable is different than the type of the expression");
                }
                convertToExpandedForm(s);
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            } else if (matchOp("&=")) {
                s.modify = "&";
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                s.value = expr;
                s.type = s.value.type();
                if (targetType != null && !targetType.equals(s.value.type())) {
                    throw syntaxError("The type of the variable is different than the type of the expression");
                }
                convertToExpandedForm(s);
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            } else if (matchOp("|=")) {
                s.modify = "|";
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                s.value = expr;
                s.type = s.value.type();
                if (targetType != null && !targetType.equals(s.value.type())) {
                    throw syntaxError("The type of the variable is different than the type of the expression");
                }
                convertToExpandedForm(s);
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            } else if (matchOp("^=")) {
                s.modify = "^";
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                s.value = expr;
                s.type = s.value.type();
                if (targetType != null && !targetType.equals(s.value.type())) {
                    throw syntaxError("The type of the variable is different than the type of the expression");
                }
                convertToExpandedForm(s);
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            } else if (matchOp(">>=")) {
                s.modify = ">>";
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                s.value = expr;
                s.type = s.value.type();
                if (targetType != null && !targetType.equals(s.value.type())) {
                    throw syntaxError("The type of the variable is different than the type of the expression");
                }
                convertToExpandedForm(s);
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            } else if (matchOp("<<=")) {
                s.modify = "<<";
                Expression expr = parseExpression();
                expr = expr.writeStatements(this, false, target);
                s.value = expr;
                s.type = s.value.type();
                if (targetType != null && !targetType.equals(s.value.type())) {
                    throw syntaxError("The type of the variable is different than the type of the expression");
                }
                convertToExpandedForm(s);
                verifyBounds(s);
                s.setBounds(solver, depth, false);
                readEndOfStatement();
                target.add(s);
                s.cloneVariable(this);
                return;
            }
        }
        throw syntaxError("Expected a statement, got '" + token + "'");
    }

    private void convertToExpandedForm(Assignment s) {
        if (s.modify != null && s.leftValue instanceof Variable) {
            s.convertToExpandedForm();
//            Variable v = (Variable) s.leftValue;
//            s.value = new Operation(v.cloneVariable(), s.modify)
        }
        // TODO Auto-generated method stub

    }

    void updateVariable(String name, Variable newVersion) {
        functionContext.updateVariable(name, newVersion);
    }

    private void verifyNotZero(Expression expr) {
        Value v = expr.eval(null);
        if (v != null) {
            if (v.longValue() == 0) {
                throw syntaxError("Division by zero is not allowed");
            }
            return;
        }
        boolean maybeZero = false;
        Solver.Rule r = new Solver.Rule();
        r.left = Operation.toSolverExpr(expr);
        r.type = "<>";
        r.right = Solver.number(0);
        if (r.isComplete() && solver.isTrue(r)) {
            // eg. via "if x = null; return"
            return;
        }
        r = new Solver.Rule();
        r.left = Operation.toSolverExpr(expr);
        r.type = ">=";
        r.right = Solver.number(1);
        if (!r.isComplete() || !solver.isTrue(r)) {
            maybeZero = true;
        }
        r = new Solver.Rule();
        r.left = Operation.toSolverExpr(expr);
        r.type = "<=";
        r.right = Solver.number(-1);
        if (!r.isComplete() || !solver.isTrue(r)) {
            maybeZero = true;
        }
        if (maybeZero) {
            throw syntaxError("Can not verify if value might be zero; division by zero is not allowed: " + expr);
        }
    }

    private boolean needBoundsCheck(Expression base, Expression arrayIndex) {
        Solver.Rule r = new Solver.Rule();
        r.left = Operation.toSolverExpr(arrayIndex);
        r.type = ">=";
        r.right = Solver.number(0);
        boolean largerEqualZero;
        if (!r.isComplete()) {
            largerEqualZero = false;
        } else {
            largerEqualZero = solver.isTrue(r);
        }
        r = new Solver.Rule();
        r.left = Operation.toSolverExpr(arrayIndex);
        r.type = "<";
        FieldAccess f = new FieldAccess(base, "len", DataType.INT_TYPE);
        r.right = Operation.toSolverExpr(f);
        boolean smallerLen;
        if (!r.isComplete()) {
            smallerLen = false;
        } else {
            smallerLen = solver.isTrue(r);
        }
        boolean newResult = !largerEqualZero || !smallerLen;
        return newResult;
    }

    private void verifyBounds(Assignment s) {
        if (s.leftValue instanceof ArrayAccess && !((ArrayAccess) s.leftValue).isBaseTypeArray()) {
            throw syntaxError("Not an array: " + s.leftValue);
        }
        if (!areTypesCompatible(s.value, s.leftValue.type())) {
            throw syntaxError("Incompatible types: " + s.value.type() + "; required: " + s.leftValue.type());
        }
        if (s.leftValue.type().isTrait() && s.value.type().implementsTrait(s.leftValue.type())) {
            s.value = new Cast(s.value, s.leftValue.type());
        }
        if (s.modify == null) {
            verifyBounds(s.leftValue.type(), s.value);
        } else {
            Expression newValue = new Operation(s.leftValue, s.modify, s.value);
            verifyBounds(s.leftValue.type(), newValue);
        }
    }

    private void verifyBounds(DataType targetType, Expression expr) {
        if (expr instanceof NullValue) {
            if (!targetType.isNullable()) {
                throw syntaxError("The expression may not be 'null' here.");
            }
        } else if (expr.type().isNullable() && !targetType.isNullable()) {
            verifyNullAccess(expr);
            // throw syntaxError("The expression may be 'null', but this is not allowed here.");
        }
        if (!targetType.isRange()) {
            return;
        }
        verifyBounds(targetType, expr, targetType.maxValue);
    }

    private void verifyBounds(DataType targetType, Expression expr, Expression max) {
        Solver.Rule r0 = new Solver.Rule();
        r0.left = Operation.toSolverExpr(expr);
        r0.type = ">=";
        r0.right = Solver.number(0);
        boolean largerEqualZero;
        if (!r0.isComplete()) {
            largerEqualZero = false;
        } else {
            largerEqualZero = solver.isTrue(r0);
        }
        Solver.Rule r = new Solver.Rule();
        r.left = Operation.toSolverExpr(expr);
        r.type = "<";
        r.right = Operation.toSolverExpr(max);
        boolean smallerLen;
        if (!r.isComplete()) {
            smallerLen = false;
        } else {
            smallerLen = solver.isTrue(r);
        }
        if (!largerEqualZero) {
            throw syntaxError("Can not verify if value is at least 0");
        }
        if (!smallerLen) {
            throw syntaxError("Can not verify if value is smaller than '" + max + "'");
        }
    }

    private void readEndOfStatement() {
        lastComment = null;
        if (token != null && !matchOp(";") && !matchOp("\n")) {
            throw syntaxError("Expected end of statement, got '" + token + "'");
        }
    }

    private TernaryExpression expandMacro(Call call, FunctionDefinition def,
            DataType type, ArrayList<Variable> params,
            ArrayList<Expression> args) {
        TernaryExpression ternary = new TernaryExpression();
        ternary.type = type;
        If ifStatement = null;

        // replace all local variables, except for "it"
        // (hygienic macros)
        List<Variable> localVars = call.def.getDeclaredVariables();
        Variable itVar = null;
        for(Variable v : localVars) {
            if (v.name().equals("it")) {
                itVar = v;
            }
        }
        if(itVar != null) {
            localVars.remove(itVar);
        }
        ArrayList<Statement> list = def.list;
        if (localVars.size() != 0) {
            ArrayList<Variable> v2 = new ArrayList<>();
            for (Variable old : localVars) {
                Variable var = new Variable("_" + old.name(), old.type());
                v2.add(var);
            }
            for (int j = 0; j < localVars.size(); j++) {
                for (int i = 0; i < list.size(); i++) {
                    Statement s = list.get(i);
                    s = s.replace(localVars.get(j), v2.get(j));
                    list.set(i, s);
                }
            }
        }

        if (list.size() == 2 && list.get(0) instanceof If) {
            ifStatement = (If) list.get(0);
            // the second block is the phi block
        } else {
            ifStatement = new If();
            ifStatement.condition = NumberValue.valueOf(1);
            ifStatement.thenList = list;
        }
        Expression condition = ifStatement.condition;
        ternary.condition = replaceAll(condition, params, args);
        if (def.callType != null) {
            Assignment setThis = new Assignment();
            // cannot use def.callType because it might be generic
            DataType ct = call.args.get(0).type();
            if (def.itType == null && ct.isArray()) {
                def.itType = ct.baseType();
            }
            setThis.leftValue = new Variable("this", ct);
            setThis.type = ct;
            setThis.initial = true;
            setThis.isConstant = true;
            setThis.value = call.args.get(0);
            setThis.value = setThis.value.writeStatements(this, true, ternary.ifTrueStatements);
            ternary.ifTrueStatements.add(setThis);
        }
        List<Statement> ifTrue = ifStatement.thenList;
        for (int i = 0; i < ifTrue.size(); i++) {
            Statement s2 = ifTrue.get(i);
            if (s2 instanceof Return) {
                Return r = (Return) s2;
                ternary.ifTrue = replaceAll(r.expr, params, args);
                break;
            }
            s2 = replaceAll(s2, params, args);
            ternary.ifTrueStatements.add(s2);
        }
        if (ifStatement.elseList != null) {
            List<Statement> ifFalse = ifStatement.elseList;
            for (int i = 0; i < ifFalse.size(); i++) {
                Statement s2 = ifFalse.get(i);
                if (s2 instanceof Return) {
                    Return r = (Return) s2;
                    ternary.ifFalse = replaceAll(r.expr, params, args);
                    break;
                }
                s2 = replaceAll(s2, params, args);
                ternary.ifFalseStatements.add(s2);
            }
        }
        return ternary;
    }

    private static Expression replaceAll(Expression expr, ArrayList<Variable> params, ArrayList<Expression> args) {
        ArrayList<Variable> newParams = new ArrayList<>();
        for (Variable p : params) {
            Variable v2 = new Variable("_" + p.name(), p.type());
            newParams.add(v2);
        }
        for (int i = 0; i < params.size(); i++) {
            expr = expr.replace(params.get(i), newParams.get(i));
        }
        for (int i = 0; i < newParams.size(); i++) {
            expr = expr.replace(newParams.get(i), args.get(i));
        }
        return expr;
    }

    private static Statement replaceAll(Statement stat, ArrayList<Variable> params, ArrayList<Expression> args) {
        ArrayList<Variable> newParams = new ArrayList<>();
        for (Variable p : params) {
            Variable v2 = new Variable("_" + p.name(), p.type());
            newParams.add(v2);
        }
        for (int i = 0; i < params.size(); i++) {
            stat = stat.replace(params.get(i), newParams.get(i));
        }
        for (int i = 0; i < newParams.size(); i++) {
            stat = stat.replace(newParams.get(i), args.get(i));
        }
        return stat;
    }

    private Expression parseCall(DataType type, String module, String identifier, Call call, boolean nonMacroCall) {
        int lineNumber = getLine(lastPos);
        if (type != null && type.module() != null) {
            module = type.module();
        }
        FunctionDefinition template = program.getFunctionTemplate(type, module, identifier);
        if (template == null) {
            template = program.getFunctionTemplate(type, null, identifier);
        }
        ArrayList<String> templateNames = new ArrayList<>();
        ArrayList<String> templateParams = new ArrayList<>();
        if (template != null && template.callType != null) {
            // fill in template params from call type, if generic
            DataType t = template.callType;
            if (DataType.isGenericTypeName(t.name())) {
                templateNames.add(t.name());
                templateParams.add(type.name());
                if (t.isArray()) {
                    templateNames.add(t.baseType().name());
                    templateParams.add(type.baseType().name());
                }
            }
        }
        if (template != null && template.macro) {
            if (!templateNames.isEmpty()) {
                String code = template.getCode();
                code = Templates.convertTemplate(code, templateNames, templateParams, program);
                String header = template.toHeaderString();
                header = Templates.convertTemplate(header, "type", "int", program);
                header = Templates.convertTemplate(header, templateNames, templateParams, program);
                code = header.trim() + "\n" + code;
                try {
                    Parser p = new Parser(program, module, code, template.lineOffset);
                    p.scanPhase = false;
                    p.read();
                    p.parseFunctionDefinition(module);
                    call.def = functionContext.getFunctionIfExists(type, currentFunctionDefinition, module, template.name, template.parameters.size());
                } catch (IllegalStateException e) {
                    throw syntaxError("Error parsing template: " + e.getMessage(), e);
                }
                String templateCode = template.template;
                template = call.def;
                template.template = templateCode;
            }
        }
        boolean lastWasComma = false;
        int pi = call.args.size(); // including 'this'
        int paramIndex = 0; // excluding 'this'
        int stackPos = functionContext.getStackPos();
        if (template != null && template.macro) {
            DataType itType = DataType.INT_TYPE;
            if (template.itType != null) {
                itType = template.itType;
            }
            Variable it = new Variable("it", itType);
            functionContext.addVariable(it);
        }
        HashSet<String> templateTypes = new HashSet<>();
        while (true) {
            if (matchOp(")")) {
                break;
            }
            boolean checkComma = !lastWasComma && paramIndex > 0;
            if (checkComma) {
                Expression last = call.args.get(call.args.size() - 1);
                if (!last.isEasyToRead()) {
                    throw syntaxError("Expected ',' after '" + last + "' or parentheses around the expression, to make it easier to read");
                }
            }
            if (template != null && pi < template.parameters.size() && DataType.TYPE.equals(template.parameters.get(pi).type().name())) {
                if (DataType.TYPE.equals(token)) {
                    throw syntaxError("Type '" + token + "' may not be used here");
                }
                DataType t = readType(false, true);
                String pName = template.parameters.get(pi).name();
                if (pName.startsWith("_")) {
                    pName = pName.substring(1);
                }
                templateNames.add(pName);
                templateParams.add(t.fullName());
                // we add a dummy value for each type parameter,
                // to simplify the parser a bit
                Expression p = NumberValue.ZERO;
                call.args.add(p);
            } else {
                Expression p = parseExpression();
                if (template != null && pi < template.parameters.size()) {
                    // template without explicit type parameters
                    // TODO this is not quite correct:
                    // we should probably have have a flag "templateWithoutExplicityParams"
                    DataType t = template.parameters.get(pi).type();
                    if (template.varArgs && pi == template.parameters.size() - 1) {
                        // the last parameters of a varargs function is an array type
                        t = t.baseType();
                    }
                    String typeName = t.name();
                    if (DataType.isGenericTypeName(typeName)) {
                        if (!templateTypes.contains(typeName)) {
                            templateTypes.add(typeName);
                            templateNames.add(typeName);
                            DataType pt = p.type();
                            if (pt.isRange()) {
                                // generics on ranges are not supported
                                pt = DataType.INT_TYPE;
                            }
                            templateParams.add(pt.fullName());
                            if (t.isArray()) {
                                // also replace the non-array version
                                t = t.baseType();
                                typeName = t.name();
                                templateNames.add(typeName);
                                pt = p.type();
                                if (!pt.isArray()) {
                                    throw syntaxError("Expected array, got " + pt);
                                }
                                pt = pt.baseType();
                                templateParams.add(pt.fullName());
                            }
                        }
                    }
                }
                if (checkComma) {
                    if (!p.isEasyToRead()) {
                        throw syntaxError("Expected ',' before '" + p + "' or parentheses around the expression, to make it easier to read");
                    }
                }
                call.args.add(p);
            }
            // dangling ',' is supported
            lastWasComma = matchOp(",");
            // new line after operation
            matchOp("\n");
            pi++;
            paramIndex++;
        }
        if (template != null) {
            String fullName = identifier;
            for (String t : templateParams) {
                fullName += "_" + t.replace('.', '_').replace("[]", "_array");
            }
            call.def = functionContext.getFunctionIfExists(type, currentFunctionDefinition, module, fullName, call.args.size());
            if (call.def == null) {
                String code = template.getCode();
                code = Templates.convertTemplate(code, templateNames, templateParams, program);
                String header = template.toHeaderString();
                header = Templates.convertTemplate(header, template.name, fullName, program);
                header = Templates.convertTemplate(header, "type", "int", program);
                header = Templates.convertTemplate(header, templateNames, templateParams, program);
                code = header.trim() + "\n" + code;
                try {
                    Parser p = new Parser(program, module, code, template.lineOffset);
                    p.scanPhase = false;
                    p.read();
                    p.parseFunctionDefinition(module);
                    call.def = functionContext.getFunctionIfExists(type, currentFunctionDefinition, module, fullName, call.args.size());
                } catch (IllegalStateException e) {
                    throw syntaxError("Error parsing template: " + e.getMessage(), e);
                }
            }
        } else {
            call.def = functionContext.getFunctionIfExists(type, currentFunctionDefinition, module, identifier, call.args.size());
            if (call.def == null) {
                call.def = functionContext.getFunctionIfExists(module, identifier);
            }
            if (call.def == null) {
                call.def = functionContext.getFunctionIfExists(null, identifier);
            }
        }
        if (call.def == null) {
            FunctionDefinition didYouMean = program.getFunctionFuzzyMatch(type, module, identifier, call.args.size());
            String notFound = "Function '" + identifier + "' not found";
            if (type != null) {
                notFound += " on type " + type;
            }
            if (didYouMean != null) {
                notFound += "; did you mean " + didYouMean.toString() + " ?";
            }
            throw syntaxError(notFound);
        }
        if (type == null && currentFunctionDefinition != null && call.def.callType != null && call.def.callType == currentFunctionDefinition.callType) {
            // add the "this." parameter at the beginning
            Variable thisVar = functionContext.getVariable(null, "this");
            call.args.add(0, thisVar);
        }
        if (call.def.parameters.size() > call.args.size()) {
            int thisParam = call.def.callType == null ? 0 : 1;
            StringBuilder buff = new StringBuilder();
            buff.append("Expected " + (call.def.parameters.size() - thisParam) + " parameters, got "
                    + (call.args.size() - thisParam) + " in call to " + call.def.name + "(");
            for (int i = thisParam; i < call.def.parameters.size(); i++) {
                if (i > thisParam) {
                    buff.append(", ");
                }
                buff.append(call.def.parameters.get(i).name());
            }
            buff.append(")");
            throw syntaxError(buff.toString());
        }

        // verify parameters with range restrictions
        boolean hasRangeParameter = false;
        if (nonMacroCall && currentFunctionDefinition != null &&
                currentFunctionDefinition.constExpr &&
                !call.def.constExpr) {
            throw syntaxError("A method marked as const can only call methods marked as const, but " + call.def.name + " is not");
        }

        for (Variable var : call.def.parameters) {
            if (var.type().isRange()) {
                hasRangeParameter = true;
            }
        }
        if (hasRangeParameter) {
            // build a list of original -> replacement pairs
            ArrayList<Variable> original = new ArrayList<>();
            ArrayList<Expression> replacement = new ArrayList<>();
            for (int i = 0; i < call.def.parameters.size(); i++) {
                Variable var = call.def.parameters.get(i);
                Expression expr = call.args.get(i);
                DataType varType = var.type();
                if (varType.isRange()) {
                    Expression max = varType.maxValue;
                    // replace all
                    for(int j = 0; j < original.size(); j++) {
                        max = max.replace(original.get(j), replacement.get(j));
                    }
                    verifyBounds(varType, expr, max);
                } else {
                    if (!expr.isSimple()) {
                        continue;
                    }
                    original.add(var);
                    replacement.add(expr);
                }
            }
        }
        // cast
        if (call.def.name.equals("println")) {
            // special case for now
        } else {
            if (call.def.parameters.size() > call.args.size()) {
                throw syntaxError("Function '" + identifier + "' not found");
            }
            for (int i = 0; i < call.args.size(); i++) {
                DataType targetType;
                if (i >= call.def.parameters.size() - 1  && call.def.varArgs) {
                    // last
                    Variable var = call.def.parameters.get(call.def.parameters.size() - 1);
                    targetType = var.type().baseType();
                } else if (i >= call.def.parameters.size()) {
                    throw syntaxError("Function '" + identifier + "' not found");
                } else {
                    Variable var = call.def.parameters.get(i);
                    targetType = var.type();
                }
                Expression expr = call.args.get(i);
                if (expr.type() == targetType) {
                    continue;
                }
                if (expr.type() != null
                        && (expr.type().isNumber() || expr.type().isFloatingPoint())
                        && call.def.name.equals(targetType.toString())) {
                    // explicit cast
                    continue;
                }
                if (expr.type() != null && expr.type().equals(targetType)) {
                    continue;
                }
                boolean isNotNull = false;
                if (expr.type() != null && expr.type().isNullable()) {
                    Solver.Rule r = new Solver.Rule();
                    r.left = Operation.toSolverExpr(expr);
                    if (r.left != null) {
                        r.type = "<>";
                        r.right = Solver.number(0);
                        if (r != null && solver.isTrue(r)) {
                            isNotNull = true;
                        }
                    }
                }
                Expression cast = program.cast(expr, isNotNull, targetType);
                if (cast == null) {
                    throw syntaxError("Need explicit cast for " + expr.type() + " to " + targetType);
                }
                call.args.set(i, cast);
            }
        }
        if (call.exceptionType() != null) {
            exceptionType = call.exceptionType();
        }
        // owned variables are now null
        call.setBounds(solver, depth, false);

        if (call.def.macro) {

            ArrayList<Variable> params = new ArrayList<>();
            ArrayList<Expression> args = new ArrayList<>();

            int argCount = call.args.size();
            for (int i = 0; i < argCount; i++) {
                if (i == 0 && call.def.callType != null) {
                    // don't expand "this"
                    continue;
                }
                Expression p = call.args.get(i);
                Variable var = call.def.parameters.get(i);
                Variable lineVar = new Variable(var.name() + ".line", DataType.INT_TYPE);
                params.add(lineVar);
                args.add(NumberValue.valueOf(lineNumber));
                DataType strType = program.getType(null, DataType.I8).arrayType();
                Variable moduleVar = new Variable(var.name() + ".module", strType);
                String m = this.module == null ? "" : this.module;
                StringLiteral moduleValue = StringLiteral.buildStringLiteral(m, strType, program);
                params.add(moduleVar);
                args.add(moduleValue);
                Variable sourceVar = new Variable(var.name() + ".source", strType);
                StringLiteral sourceValue = StringLiteral.buildStringLiteral(p.toString(), strType, program);
                params.add(sourceVar);
                args.add(sourceValue);
                Variable astVar = new Variable(var.name() + ".ast", strType);
                StringLiteral astValue = StringLiteral.buildStringLiteral(p.toAST(), strType, program);
                params.add(astVar);
                args.add(astValue);
                List<Variable> vars = p.getVariables();
                // deduplicate
                vars = new ArrayList<>(new HashSet<Variable>(vars));
                vars.sort(new Comparator<Variable>() {
                    @Override
                    public int compare(Variable o1, Variable o2) {
                        return o1.name().compareTo(o2.name());
                    }
                });
                ArrayList<Expression> valueList = new ArrayList<>();
                for (Variable v : vars) {
                    if (v.name().equals("it")) {
                        continue;
                    }
                    Expression e = program.cast(v, false, strType);
                    valueList.add(StringLiteral.buildStringLiteral(v.name(), strType, program));
                    if (e == null) {
                        valueList.add(StringLiteral.buildStringLiteral("?", strType, program));
                    } else {
                        valueList.add(e);
                    }
                }
                Variable varsVar = new Variable(var.name() + ".values", strType);
                Expression varExpr;
                if (valueList.isEmpty()) {
                    varExpr = StringLiteral.buildStringLiteral("", strType, program);
                } else {
                    String join = "appendValue";
                    FunctionDefinition append = program.getFunctionIfExists(null, "org.bau.Std", join, 2);
                    // append(append(append(append(null, a), b), c))
                    // that way, the append function could double the array size when needed,
                    // store the length at the end, and in the last call truncate;
                    // and in this way, use O(n) instead of O(n^2) time
                    if (append != null) {
                        Expression last = StringLiteral.buildStringLiteral("", strType, program);
                        valueList.add(last);
                        while (valueList.size() > 0) {
                            Expression a = valueList.remove(0);
                            Call appendCall = new Call();
                            appendCall.def = append;
                            appendCall.args.add(last);
                            appendCall.args.add(a);
                            last = appendCall;
                        }
                        valueList.add(last);
                    }
                    varExpr = valueList.get(0);
                }
                params.add(varsVar);
                args.add(varExpr);
                // needs to be done at the very end at the end,
                // otherwise fields are not expanded
                params.add(var);
                args.add(p);
            }
            type = call.def.returnType;
            TernaryExpression result = expandMacro(call, call.def, type, params, args);
            functionContext.rewindStack(stackPos);
            return result;
        }

        functionContext.rewindStack(stackPos);

        return call;
    }

    private void parseReturn(ArrayList<Statement> target) {
        if (currentFunctionDefinition == null) {
            throw syntaxError("Return needs to be inside of a function");
        }
        Return b = new Return(null);
        if (matchOp("\n") || matchOp(";")) {
            if (currentFunctionDefinition.returnType != null) {
                throw syntaxError("The function does not return an expression of type " + currentFunctionDefinition.returnType);
            }
            target.add(b);
            negateLastBlockCondition();
            return;
        }
        // if it is not a simple value  (e.g. string1 + string2)
        // assign it first to a variable so that the components can be freed up
        b.expr = parseExpression(target);
        if (currentFunctionDefinition.returnType == null) {
            throw syntaxError("The function declared to not return a value");
        }
        if (!b.expr.isSimple()) {
            // this is converted to "_r", but we can not use "_r" internally
            // because this can conflict with user-defined variable names.
            // user defined names can not start with a number
            // (same as "0t")
            String constId = "0r" + nextConstantId++;
            Assignment ret = new Assignment();
            ret.initial = true;
            ret.isConstant = true;
            DataType type = b.expr.type();
            if (type == null) {
                throw syntaxError("No type");
            }
            Variable var = Variable.newInternal(constId, type);
            ret.leftValue = var;
            ret.type = b.expr.type();
            ret.value = b.expr;
            b.expr = ret.leftValue;
            target.add(ret);
        }
        DataType type = currentFunctionDefinition.returnType;
        if (!areTypesCompatible(b.expr, type)) {
            throw syntaxError("Incompatible types: " + b.expr.type() + "; required: " + currentFunctionDefinition.returnType);
        }
        if (type.isTrait() && b.expr.type().implementsTrait(type)) {
            b.expr = new Cast(b.expr, type);
        }
        verifyBounds(currentFunctionDefinition.returnType, b.expr);
        b.autoClose = autoClose(stackPosFunction, b.expr);
        negateLastBlockCondition();
        if (matchOp("\n") || matchOp(";")) {
            target.add(b);
            return;
        }
        throw syntaxError("Expected end of statement, got '" + token + "' in 'return' statement");
    }

    private boolean areTypesCompatible(Expression expr, DataType assignToType) {
        DataType exprType = expr.type();
        if (exprType == null) {
            // unknown (eg. "null")
            expr = assignToType.nullExpression();
            exprType = assignToType.orNull();
        }
        // TODO use this function everywhere where it makes sense
        if (exprType.equals(assignToType)) {
            return true;
        }
        if (exprType.isNumber() || assignToType.isNumber()) {
            if (expr.toString().equals("0") && (assignToType.isCopyType() || assignToType.isNullable())) {
                // setting 0 to a nullable variable or value type is ok
                return true;
            }
            if (!exprType.isNumber() || !assignToType.isNumber()) {
                return false;
            }
            if (!exprType.isRange() && assignToType.isRange()) {
                // verified via bounds check
                return true;
            }
            if (exprType.isRange() && !assignToType.isRange()) {
                return true;
            }
            if (!exprType.isRange() && assignToType.isRange()) {
                return false;
            }
            if (exprType.isFloatingPoint() && !assignToType.isFloatingPoint()) {
                // throw syntaxError("The expression is floating point, but the variable is not.");
                // TODO there are many more cases that are not allowed
                return false;
            }
            return true;
        }
        if (exprType == null || assignToType == null) {
            return exprType == assignToType;
        }
        if (exprType == assignToType || exprType.equals(assignToType)) {
            return true;
        }
        if (exprType.isArray() != assignToType.isArray()) {
            return false;
        }
        if (exprType.isArray()) {
            return exprType.equals(assignToType);
        }
        if (exprType.isNullable() == assignToType.isNullable()) {
            return exprType.equals(assignToType);
        } else if (exprType.isNullable() && !assignToType.isNullable()) {
            verifyNullAccess(expr);
            return exprType.equals(assignToType.orNull());
        } else if (!exprType.isNullable() && assignToType.isNullable()) {
            assignToType = assignToType.notNullType();
        }
        if (exprType.equals(assignToType)) {
            return true;
        }
        if (exprType.implementsTrait(assignToType)) {
            return true;
        }
        return false;
    }

    private void parseCatch(ArrayList<Statement> target) {
        int catchIndent = indent;
        int stackPos = functionContext.getStackPos();
        Catch catchStat = new Catch();
        String id = readIdentifier();
        Variable var = new Variable(id, exceptionType);
        if (exceptionType == null) {
            throw syntaxError("Exception type is not known");
        }
        functionContext.addVariable(var);
        catchStat.var = var;
        boolean sameLine;
        if (matchOp("\n")) {
            sameLine = false;
        } else if (matchOp("{")) {
            sameLine = true;
        } else {
            throw syntaxError("Expected end of statement, got '" + token + "' in 'catch' statement");
        }
        while (true) {
            if (sameLine) {
                if (matchOp("}")) {
                    break;
                }
            } else {
                if (indent <= catchIndent) {
                    break;
                }
            }
            parseStatements(catchStat.list);
        }
        catchStat.autoClose(autoClose(stackPos, null));
        functionContext.rewindStack(stackPos);
        target.add(catchStat);
    }

    private void parseThrow(ArrayList<Statement> target) {
        if (currentFunctionDefinition.exceptionType == null) {
            throw syntaxError("This method does not throw an exception (local exceptions are not supported)");
        }
        Throw t = new Throw();
        if (matchOp("\n") || matchOp(";")) {
            target.add(t);
            negateLastBlockCondition();
            return;
        }
        t.expr = parseExpression(target);
        exceptionType = t.expr.type();
        // theoretically, the exception could be caught in this method
        // so we can not verify that the exception type matches the one
        // declared in the function
        if (matchOp("\n") || matchOp(";")) {
            target.add(t);
            negateLastBlockCondition();
            return;
        }
        throw syntaxError("Expected end of statement, got '" + token + "' in 'throw' statement");
    }

    private void parseBreak(ArrayList<Statement> target) {
        if (currentLoop == null) {
            throw syntaxError("'break' statement outside of a loop");
        }
        Break b = new Break();
        if (matchOp("\n") || matchOp(";")) {
            target.add(b);
            negateLastBlockCondition();
            return;
        }
        b.condition = parseCondition(target);
        setBlockCondition(b.condition, false, true);
        b.autoClose = autoClose(stackPosLoop, null);
        if (matchOp("\n") || matchOp(";")) {
            target.add(b);
            return;
        }
        throw syntaxError("Expected end of statement, got '" + token + "' in 'break' statement");
    }

    private void parseContinue(ArrayList<Statement> target) {
        if (currentLoop == null) {
            throw syntaxError("'continue' statement outside of a loop");
        }
        Continue c = new Continue();
        if (matchOp("\n") || matchOp(";")) {
            target.add(c);
            negateLastBlockCondition();
            return;
        }
        c.condition = parseCondition(target);
        c.loop = currentLoop;
        setBlockCondition(c.condition, false, true);

        c.autoClose = autoClose(stackPosLoop, null);
        if (matchOp("\n") || matchOp(";")) {
            target.add(c);
            return;
        }
        throw syntaxError("Expected end of statement, got '" + token + "' in 'continue' statement");
    }

    private boolean match(String s) {
        if (type == TokenType.IDENTIFIER && s.equals(token)) {
            read();
            return true;
        }
        return false;
    }

    private boolean matchOp(String s) {
        if (type == TokenType.OPERATOR && s.equals(token)) {
            if ("\n".equals(token)) {
                readSpaces();
            } else {
                read();
            }
            return true;
        }
        return false;
    }

    private Expression parseCondition(ArrayList<Statement> target) {
        Expression expr = parseExpression(target);
        if (expr.type().isNullable()) {
            return new Operation(expr, "<>", new NullValue(expr.type()));
        }
        return expr;
    }

    private void parseSwitch(ArrayList<Statement> target) {
        int switchIndent = indent;
        If ifStatement = new If();
        If topIfStatement = ifStatement;
        Expression switchExpr = assignTempVariable(target, parseExpression());

        boolean elsePart = false;
        int stackPos = functionContext.getStackPos();
        boolean first = true;
        if (!matchOp("\n")) {
            throw syntaxError("Expected end of statement, got '" + token + "' in 'switch' statement");
        }
        while (true) {
            if (match("case")) {
                Expression condition = null;
                while (true) {
                    Expression caseExpr = parseExpression();
                    Expression cond = new Operation(switchExpr, "==", caseExpr);
                    if (condition == null) {
                        condition = cond;
                    } else {
                        condition = new Operation(condition, "or", cond);
                    }
                    if (!matchOp(",")) {
                        break;
                    }
                    // optional new line
                    matchOp("\n");
                }
                if (!matchOp("\n")) {
                    throw syntaxError("Expected end of statement, got '" + token + "' in 'switch' statement");
                }
                if (!first) {
                    endBlock();
                }
                startBlock(false, condition);
                if (!first) {
                    If elseIf = new If();
                    ArrayList<Statement> list = new ArrayList<>();
                    list.add(elseIf);
                    list.add(new PhiBlock());
                    ifStatement.elseList = list;
                    ifStatement.elseAutoClose = List.of();
                    ifStatement = elseIf;
                }
                first = false;
                ifStatement.condition = condition;
            } else if (match("else")) {
                if (!matchOp("\n")) {
                    throw syntaxError("Expected end of statement, got '" + token + "' in 'switch' statement");
                }
                endBlock();
                startBlock(false, null);
                first = false;
                elsePart = true;
            } else {
                break;
            }
            ArrayList<Statement> list = new ArrayList<>();
            while (true) {
                if (indent <= switchIndent) {
                    break;
                }
                parseStatements(list);
            }
            if (!elsePart) {
                ifStatement.thenList = list;
                ifStatement.thenAutoClose = autoClose(stackPos, null);
            } else {
                ifStatement.elseList = list;
                ifStatement.elseAutoClose = autoClose(stackPos, null);
            }
            functionContext.rewindStack(stackPos);
            if (elsePart) {
                break;
            }
            switchIndent = indent;
        }
        endBlock();
        target.add(topIfStatement);
        target.add(new PhiBlock());
    }

    private void parseIf(ArrayList<Statement> target) {
        int ifIndent = indent;
        boolean sameLine;
        If ifStatement = new If();
        If topIfStatement = ifStatement;
        Expression condition = parseCondition(target);
        startBlock(false, condition);
        ifStatement.condition = condition;
        boolean elsePart = false;
        int stackPos = functionContext.getStackPos();
        while (true) {
            if (matchOp("\n")) {
                sameLine = false;
            } else if (matchOp("{")) {
                sameLine = true;
            } else {
                throw syntaxError("Expected end of statement, got '" + token + "' in 'if' statement");
            }
            ArrayList<Statement> list = new ArrayList<>();
            if (ifStatement.thenList == null) {
                ifStatement.thenList = list;
            } else {
                ifStatement.elseList = list;
            }
            while (true) {
                if (sameLine) {
                    if (matchOp("}")) {
                        break;
                    }
                } else {
                    if (indent <= ifIndent) {
                        break;
                    }
                }
                parseStatements(list);
            }
            if (ifStatement.thenAutoClose == null) {
                ifStatement.thenAutoClose = autoClose(stackPos, null);
            } else {
                ifStatement.elseAutoClose = autoClose(stackPos, null);
            }
            functionContext.rewindStack(stackPos);
            if (elsePart) {
                break;
            }
            if (indent < ifIndent) {
                break;
            }
            ifIndent = indent;
            if (match("elif")) {
                endBlock();
                If elseIf = new If();
                list = new ArrayList<>();
                condition = parseCondition(list);
                elseIf.condition = condition;
                list.add(elseIf);
                list.add(new PhiBlock());
                ifStatement.elseList = list;
                ifStatement.elseAutoClose = List.of();
                ifStatement = elseIf;
                startBlock(false, condition);
            } else if (match("else")) {
                endBlock();
                startBlock(false, null);
                elsePart = true;
            } else {
                break;
            }
        }
        endBlock();
        target.add(topIfStatement);
        target.add(new PhiBlock());
    }

    private void setRangeBounds(Variable var) {
        DataType type = var.type();
        if (type.isRange()) {
            Solver.Rule r = Solver.rule(Solver.variable(var.name()), ">=", Solver.number(0));
            if (!var.global()) {
                r.depth = depth;
            }
            r.always = true;
            solver.addRule(r);
            r = Solver.rule(Solver.variable(var.name()), "<", Operation.toSolverExpr(type.maxValue));
            if (!var.global()) {
                r.depth = depth;
            }
            r.always = true;
            solver.addRule(r);
        }
    }

    private void parseFor(ArrayList<Statement> target) {
        if (currentFunctionDefinition != null && currentFunctionDefinition.macro) {
            throw syntaxError("For loops in macros are currently not supported");
        }
        int loopIndent = indent;
        String variableName = readIdentifier();
        if (!matchOp(":=")) {
            throw syntaxError("Expected ':=', got '" + token + "' in 'for' statement");
        }
        String method = readIdentifier();
        if (!matchOp("(")) {
            throw syntaxError("Expected a function call, got '" + token + "' in 'for' statement");
        }
        matchOp("\n");
        if ("range".equals(method)) {
            Std.registerRange(program);
        } else if ("until".equals(method)) {
            Std.registerUntil(program);
        }
        Call call = new Call();
        Expression expr = parseCall(null, null, method, call, false);
        if (!(expr instanceof Call)) {
            throw syntaxError("Only range functions are supported");
        }
        call = (Call) expr;
        FunctionDefinition functionDef = call.def;
        if (functionDef.exceptionType != null) {
            throw syntaxError("The function in the 'for' statement may not throw an exception sorry");
        }
        int stackPos = functionContext.getStackPos();
        int oldStackPosLoop = stackPosLoop;
        stackPosLoop = stackPos;
        Loop outerLoop = new Loop();

        ArrayList<Variable> oldArgs = new ArrayList<>();
        ArrayList<Expression> newArgs = new ArrayList<>();
        for (int i = 0; i < functionDef.parameters.size(); i++) {
            Variable v = functionDef.parameters.get(i);
            Variable v2 = new Variable("_" + v.name(), v.type());
            v2.setConstantValue(null);
            oldArgs.add(v);
            newArgs.add(call.args.get(i));
        }
        DataType type = functionDef.returnType;
        if (type.isRange()) {
            // TODO hack: replace maxValue for loops over arrays
            type.maxValue = call.args.get(0);
        }
        Operation comp = new Operation(
                new NumberValue(new Value.ValueInt(1), program.getType(null, DataType.INT), false),
                "==",
                new NumberValue(new Value.ValueInt(1), program.getType(null, DataType.INT), false)
                );
        comp.operator = "==";
        Variable var = new Variable(variableName, call.type());
        if (var.type().isRange()) {
            setRangeBounds(var);
        }
        functionContext.addVariable(var);
        Loop loop = new Loop();
        int i = 0;
        Variable old = new Variable("_", call.def.returnType);
        ArrayList<Statement> whileLoop = null;
        If wrappingIf = null;
        List<Statement> loopFunctionList = new ArrayList<>();
        loopFunctionList.addAll(functionDef.list);
        if (!loopFunctionList.isEmpty()) {
            while (loopFunctionList.get(0) instanceof PhiBlock) {
                loopFunctionList.remove(0);
            }
            while (loopFunctionList.get(loopFunctionList.size() - 1) instanceof PhiBlock) {
                loopFunctionList.remove(loopFunctionList.size() - 1);
            }
        }
        if (loopFunctionList.size() == 1) {
            Statement stat = loopFunctionList.get(0);
            if (stat instanceof If) {
                wrappingIf = (If) stat;
                Expression condition = wrappingIf.condition;
                condition = condition.replace(old, var);
                for(int k = 0; k < oldArgs.size(); k++) {
                    condition = condition.replace(oldArgs.get(k), newArgs.get(k));
                }
                loopFunctionList = wrappingIf.thenList;
                // clone
                wrappingIf = new If();
                wrappingIf.condition = condition;
            }
            if (stat instanceof Assignment) {
                // need to make sure we have our own copy of all variables,
                ((Assignment) stat).cloneVariable(this);
            }
        }
        startBlock(true, comp);
        outerLoop.condition = comp;
        for (; i < loopFunctionList.size(); i++) {
            Statement s = loopFunctionList.get(i);
            s = s.replace(old, var);
            for (int k = 0; k < oldArgs.size(); k++) {
                s = s.replace(oldArgs.get(k), newArgs.get(k));
            }
            if (s instanceof Assignment) {
                // need to make sure we have our own copy of all variables,
                ((Assignment) s).cloneVariable(this);
            }
            if (s instanceof Loop) {
                Loop w = (Loop) s;
                whileLoop = w.list;
                loop.condition = w.condition;
                setBlockCondition(loop.condition, true, false);
                i++;
                break;
            }
            s.setBounds(solver, depth, true);
            outerLoop.list.add(s);
        }
        Loop oldLoop = currentLoop;
        currentLoop = loop;
        startBlock(true, loop.condition);
        int j = 0;
        for (; j < whileLoop.size(); j++) {
            Statement s = whileLoop.get(j);
            if (s instanceof Return) {
                j++;
                break;
            }
            s = s.replace(old, var);
            for (int k = 0; k < oldArgs.size(); k++) {
                s = s.replace(oldArgs.get(k), newArgs.get(k));
            }
            if (s instanceof Assignment) {
                // need to make sure we have our own copy of all variables,
                ((Assignment) s).cloneVariable(this);
            }
            s.setBounds(solver, depth, true);
            loop.list.add(s);
        }
        boolean sameLine;
        if (matchOp("\n")) {
            sameLine = false;
        } else if (matchOp("{")) {
            sameLine = true;
        } else {
            throw syntaxError("Expected end of statement, got '"+token+"' in 'for' statement");
        }
        while (true) {
            if (sameLine) {
                if (matchOp("}")) {
                    break;
                }
            } else {
                if (indent <= loopIndent) {
                    break;
                }
            }
            parseStatements(loop.list);
        }
        for (; j < whileLoop.size(); j++) {
            Statement s = whileLoop.get(j);
            s = s.replace(old, var);
            for(int k = 0; k < oldArgs.size(); k++) {
                s = s.replace(oldArgs.get(k), newArgs.get(k));
            }
            if (s instanceof Break) {
                ((Break) s).autoClose = autoClose(stackPosLoop, null);
            } else if (s instanceof Continue) {
                ((Continue) s).autoClose = autoClose(stackPosLoop, null);
            }
            s.setBounds(solver, depth, true);
            if (loop.listContinue.isEmpty()) {
                loop.listContinue.add(new PhiBlock());
            }
            if (s instanceof Assignment) {
                // need to make sure we have our own copy of all variables,
                ((Assignment) s).cloneVariable(this);
            }
            loop.listContinue.add(s);
        }
        endBlock();
        outerLoop.list.add(new PhiBlock());
        outerLoop.list.add(loop);
        outerLoop.list.add(new PhiBlock());
        for (; i < loopFunctionList.size(); i++) {
            Statement s = loopFunctionList.get(i);
            outerLoop.list.add(s);
        }
        outerLoop.list.add(new Break());
        loop.autoClose(autoClose(stackPos, null));
        functionContext.rewindStack(stackPos);
        endBlock();
        stackPosLoop = oldStackPosLoop;
        currentLoop = oldLoop;
        if (wrappingIf == null) {
            target.add(new PhiBlock());
            target.add(outerLoop);
            target.add(new PhiBlock());
        } else {
            ArrayList<Statement> list = new ArrayList<>();
            list.add(new PhiBlock());
            list.add(outerLoop);
            list.add(new PhiBlock());
            if (wrappingIf.thenList == null) {
                wrappingIf.thenList = list;
                wrappingIf.thenAutoClose = new ArrayList<>();
            } else {
                wrappingIf.elseList = list;
                wrappingIf.elseAutoClose = new ArrayList<>();
            }
            target.add(wrappingIf);
            target.add(new PhiBlock());
        }
    }

    private void startBlock(boolean loop, Expression condition) {
        depth++;
        setBlockCondition(condition, loop, false);
    }

    private void setBlockCondition(Expression condition, boolean loop, boolean negated) {
        if (loop) {
            solver.removeNotAlwaysRules();
        }
        if (condition != null) {
            List<Solver.Rule> list = condition.getRules();
            if (!list.isEmpty()) {
                for (Solver.Rule r : list) {
                    if (negated) {
                        r = r.reverse();
                    }
                    r.depth = depth;
                    if (!condition.containsModifiableVariables()) {
                        r.always = true;
                    }
                    solver.addRule(r);
                }
            }
        }
    }

    private void negateLastBlockCondition() {
        // for example, after: "if x = 0 { throw }" we know that x will not be 0
        solver.reversRulesOfDepth(depth);
    }

    private void endBlock() {
        depth--;
        solver.removeDeeperRules(depth);
    }

    private void parseLoop(ArrayList<Statement> target) {
        int loopIndent = indent;
        Loop oldLoop = currentLoop;
        Loop loop = new Loop();
        currentLoop = loop;
        if (type == TokenType.OPERATOR && ("\n".equals(token) || "{".equals(token))) {
            loop.condition = new NumberValue(new Value.ValueInt(1), DataType.INT_TYPE, false);
        } else {
            loop.condition = parseCondition(loop.list);
        }
        startBlock(true, loop.condition);
        if (!loop.list.isEmpty()) {
            // we need to make it an endless loop with a break condition
            Break b = new Break();
            b.condition = new Operation(null, "not", loop.condition);
            loop.list.add(b);
            loop.condition = new Operation(
                    new NumberValue(new Value.ValueInt(1), program.getType(null, DataType.INT), false),
                    "==",
                    new NumberValue(new Value.ValueInt(1), program.getType(null, DataType.INT), false)
                    );
        }
        boolean sameLine;
        if (matchOp("\n")) {
            sameLine = false;
        } else if (matchOp("{")) {
            sameLine = true;
        } else {
            throw syntaxError("Expected end of statement, got '"+token+"' in 'while' statement");
        }
        int stackPos = functionContext.getStackPos();
        int oldStackPosLoop = stackPosLoop;
        stackPosLoop = stackPos;
        while (true) {
            if (sameLine) {
                if (matchOp("}")) {
                    break;
                }
            } else {
                if (indent <= loopIndent) {
                    break;
                }
            }
            parseStatements(loop.list);
        }
        loop.autoClose(autoClose(stackPos, null));
        functionContext.rewindStack(stackPos);
        stackPosLoop = oldStackPosLoop;
        endBlock();
        currentLoop = oldLoop;
        // before the loop (we jump here from the end of the loop)
        target.add(new PhiBlock());
        target.add(loop);
        // loop condition doesn't match, and break statements
        target.add(new PhiBlock());
    }

    private List<Statement> autoClose(int stackPos, Expression except) {
        ArrayList<Statement> autoClose = new ArrayList<Statement>();
        if (functionContext.getStackCloseDifference(stackPos) == 0) {
            return autoClose;
        }
        ArrayList<String> list = functionContext.newVariablesList(stackPos);
        String exceptString = except == null ? "" : except.toString();
        for (String name : list) {
            // do not close if we return it
            if (name.equals(exceptString)) {
                continue;
            }
            Variable var = functionContext.getVariable(null, name);
            if (var == null) {
                throw syntaxError("Variable not found: '" + name + "'");
            }
            if (var.type().needFree()) {
                Free free = new Free(var);
                autoClose.add(free);
            }
        }
        Collections.reverse(autoClose);
        return autoClose;
    }

    private Expression parseExpression(ArrayList<Statement> target) {
        try {
            return parseExpression().writeStatements(this, false, target);
        } catch (IllegalStateException e) {
            throw syntaxError(e.getMessage());
        }
    }

    private Expression parseExpression() {
        Expression expr = parseExpression(parseExpressionPrimary(), 1);
        if (expr.type() == null) {
            // null
            return expr;
        }
        if (expr.type().isNumber() && !(expr instanceof NumberValue)) {
            Value v = eval(expr, true);
            if (v != null) {
                return new NumberValue(v, expr.type(), false);
            }
        }
        return expr;
    }

    private Expression parseFunctionOnLiteral(Expression expr) {
        String f = readIdentifier();
        matchOp("(");
        matchOp("\n");
        Call call = new Call();
        call.args.add(expr);
        String m = null;
        if (isImport) {
            m = module;
        }
        return parseCall(expr.type(), m, f, call, true);
    }

    private Expression parseExpressionPrimary() {
        if (matchOp("-")) {
            Operation op = new Operation(null, "-", parseExpressionPrimary());
            return op;
        } else if (matchOp("+")) {
            return parseExpressionPrimary();
        } else if (matchOp("~")) {
            Operation op = new Operation(null, "~", parseExpressionPrimary());
            return op;
        } else if (match("not")) {
            Operation op = new Operation(null, "not", parseExpressionPrimary());
            return op;
        } else if (type == TokenType.INTEGER) {
            String n = token;
            read();
            long v = Long.parseLong(n);
            Expression expr = new NumberValue(new Value.ValueInt(v), program.getType(null, DataType.INT), false);
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (type == TokenType.HEX_INTEGER) {
            String n = token;
            read();
            long v = NumberValue.parseUnsignedHexLong(n.substring(2));
            Expression expr = new NumberValue(new Value.ValueInt(v), program.getType(null, DataType.INT), true);
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (type == TokenType.FLOAT) {
            String n = token;
            read();
            double v = Double.parseDouble(n);
            Expression expr = new NumberValue(new Value.ValueFloat(v), program.getType(null, DataType.FLOAT), false);
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (type == TokenType.STRING) {
            String n = token;
            read();
            DataType type = program.getType(null, DataType.I8).arrayType();
            Expression expr = program.getStringLiteral(n);
            if (expr == null) {
                expr = StringLiteral.buildStringLiteral(n, type, program);
            }
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (matchOp("&")) {
            Expression target = parseExpressionPrimary();
            target = new Borrow(target);
            return target;
        } else if (type == TokenType.IDENTIFIER) {
            String n = token;
            if ("null".equals(n)) {
                read();
                return new NullValue(null);
            }
            Variable thisVar = functionContext.getVariable(null, "this");
            String m = null;
            if ("ord".equals(n)
                    || "appendValue".equals(n)
                    || "convertIntToI8Array".equals(n)
                    || "convertFloatToI8Array".equals(n)) {
                Std.registerStd(program);
                m = "org.bau.Std";
                read();
            } else {
                read();
                m = program.getImportEntry(n);
                if (m == null) {
                    m = module;
                    // if there is no variable with this name, and
                    // no function with this name, and
                    // no this.field with this name,
                    // then it could be a fully qualified module name
                    if (functionContext.getVariable(null, n) == null &&
                            functionContext.getType(m, n) == null &&
                            (thisVar == null || thisVar.type().getFieldDataType(n) == null)) {
                        while (matchOp(".")) {
                            m = m == module ? n : m + "." + n;
                            n = readIdentifier();
                        }
                        String m2 = program.getImport(m);
                        if (m2 != null) {
                            m = m2;
                        }
                    }
                }
            }
            DataType dataType = functionContext.getType(m, n);
            if (dataType != null && dataType.enumValues != null) {
                DataType enumType = functionContext.getType(m, n);
                read();
                if (matchOp(".")) {
                    throw syntaxError("Expected '.' after reading enum type '" + enumType.name() + "'");
                }
                String val = readIdentifier();
                Long value = enumType.enumValues.get(val);
                if (value == null) {
                    throw syntaxError("Value '" + val + "' not found for enum type '" + enumType.name() + "'");
                }
                Expression expr = new NumberValue(new Value.ValueInt(value), enumType, false);
                return expr;
            } else if (dataType != null) {
                if (dataType.template != null) {
                    ArrayList<DataType> params = parseTypeParameters(dataType, false);
                    // if templates can be returned, then
                    // we must not replace the types
                    dataType = parseTemplatedType(dataType, params);
                    // the name of the constructor includes the type names
                    n = dataType.name();
                }
                if (matchOp("[")) {
                    Expression arrayLength = parseExpression();
                    if (arrayLength.canThrowException() != null) {
                        throw syntaxError("May not throw an exception here");
                    }
                    if (!matchOp("]")) {
                        throw syntaxError("Expected ')', got '" + token + "' in constructor");
                    }
                    New newExpr = new New(dataType.arrayType(), arrayLength);
                    return newExpr;
                }
                if (matchOp("(")) {
                    matchOp("\n");
                    Call call = new Call();
                    Expression expr = parseCall(null, m, n, call, true);
                    return expr;
                } else {
                    throw syntaxError("Expected '(' to call the constructor");
                }
            }
            if (matchOp("(")) {
                matchOp("\n");
                Call call = new Call();
                Expression expr = parseCall(null, m, n, call, true);
                Value val = eval(expr, true);
                ArrayList<FunctionDefinition> toCompileFirst = new ArrayList<>(program.uncompiledFunctions.values());
                if (!toCompileFirst.isEmpty()) {
                    // could not yet evaluate, because a function was not yet compiled
                    // we compile those functions first, but only once
                    program.uncompiledFunctions.clear();
                    for (FunctionDefinition def : toCompileFirst) {
                        FunctionDefinition latest = program.functions.get(def.getFunctionId());
                        if (def != latest) {
                            continue;
                        }
                        if (def.code != null) {
                            try {
                                Parser p = new Parser(program, def.module, def.toString(), def.lineOffset);
                                p.scanPhase = false;
                                p.read();
                                p.parseFunctionDefinition(def.module);
                            } catch (IllegalStateException e) {
                                throw syntaxError("Error parsing function: " + e.getMessage(), e);
                            }
                        }
                    }
                    // replace the definition
                    call.def = program.functions.get(call.def.getFunctionId());
                    // try again (but only once)
                    val = eval(call, true);
                }
                if (val != null) {
                    if (val instanceof ValueI8Array) {
                        ValueI8Array str = (ValueI8Array) val;
                        String s = str.toString();
                        DataType type = program.getType(null, DataType.I8).arrayType();
                        expr = StringLiteral.buildStringLiteral(s, type, program);
                        return expr;
                    } else if (val instanceof ValueArray) {
                        if (call.type().baseType().isNumber()) {
                            Variable var = new Variable("x", call.type());
                            var.setConstantValue(val);
                            long reference = program.addArrayConstant(var);
                            return new ArrayConstant((ValueArray) val, expr.type(), reference);
                        }
                    } else if (val instanceof ValueRef) {
                        // the memory is no longer available
                    } else {
                        return new NumberValue(val, expr.type(), false);
                    }
                }
                return parsePossibleDot(expr);
            }
            Expression var = functionContext.getVariable(m, n);
            if (var == null) {
                if (thisVar != null) {
                    verifyNullAccess(thisVar);
                    DataType thisType = thisVar.type();
                    DataType type = thisType.getFieldDataType(n);
                    if (type != null) {
                        var = new FieldAccess(thisVar, n, type);
                    }
                }
                FunctionDefinition def = functionContext.getFunctionIfExists(null, n);
                if (def == null) {
                    def = functionContext.getFunctionIfExists(m, n);
                }
                if (def != null) {
                    if (def.exceptionType != null) {
                        throw syntaxError("Function throws an exception; this is not supported");
                    }
                    if (def.varArgs) {
                        throw syntaxError("Function has a variable number of arguments; this is not supported");
                    }
                    FunctionPointer fp = new FunctionPointer(def);
                    return fp;
                }
                if (var == null) {
                    throw syntaxError("Variable or constant not found: '" + n + "'");
                }
            }
            return parsePossibleDot(var);
        } else if (matchOp("(")) {
            matchOp("\n");
            Expression expr = parseExpression();
            if (!matchOp(")")) {
                throw syntaxError("Expected ')', got '" + token + "' in nested expression");
            }
            return parsePossibleDot(new Parentheses(expr));
        } else {
            throw syntaxError("Expected an expression, got '"+token+"'");
        }
    }

    private Expression parsePossibleDot(Expression v) {
        DataType vt = v.type();
        while (true) {
            if (matchOp(".")) {
                if (vt.isPointer()) {
                    verifyNullAccess(v);
                }
                matchOp("\n");
                String f;
                if (v instanceof Variable && type == TokenType.INTEGER) {
                    int index = Integer.parseInt(token);
                    read();
                    String[] fieldNames = vt.getFieldNames();
                    if (fieldNames.length == 0) {
                        return v;
                    } else if (index >= 0 && index < fieldNames.length) {
                        f = fieldNames[index];
                    } else {
                        f = fieldNames[0];
                    }
                } else {
                    f = readIdentifier();
                }
                if (matchOp("(")) {
                    matchOp("\n");
                    Call call = new Call();
                    call.args.add(v);
                    v = parseCall(vt, module, f, call, true);
                    vt = v.type();
                } else {
                    DataType type;
                    if ("len".equals(f) && vt.isArray()) {
                        type = program.getType(null, DataType.I32);
                    } else {
                        type = vt.getFieldDataType(f);
                    }
                    if (type == null) {
                        if (currentFunctionDefinition != null
                                && currentFunctionDefinition.macro) {
                            if ("source".equals(f)) {
                                type = program.getType(null, DataType.I8).arrayType();
                            } else if ("ast".equals(f)) {
                                type = program.getType(null, DataType.I8).arrayType();
                            } else if ("values".equals(f)) {
                                type = program.getType(null, DataType.I8).arrayType();
                            } else if ("module".equals(f)) {
                                type = program.getType(null, DataType.I8).arrayType();
                            } else if ("line".equals(f)) {
                                type = DataType.INT_TYPE;
                            } else {
                                throw syntaxError("Field '" + f + "' not found with type '" + vt + "'");
                            }
                        } else {
                            throw syntaxError("Field '" + f + "' not found with type '" + vt + "'");
                        }
                    }
                    v = new FieldAccess(v, f, type);
                    vt = v.type();
                }
            } else if (matchOp("[")) {
                Expression arrayIndex = parseExpression();
                boolean checkBounds = needBoundsCheck(v, arrayIndex);
                if (matchOp("]!")) {
                    if (checkBounds) {
                        throw syntaxError("Expected ']', got '" + token + "' when reading from an array that needs a bounds check (use '[index]' instead of '[index]!').");
                    }
                } else if (!matchOp("]")) {
                    throw syntaxError("Expected ']', got '" + token + "' in array access");
                }
                if (!v.type().isArray()) {
                    throw syntaxError("Not an array type: " + v.type());
                }
                v = new ArrayAccess(v, arrayIndex, checkBounds);
                vt = v.type();
            } else {
                break;
            }
        }
        return v;
    }

    private void verifyNullAccess(Expression expr) {
        boolean needTest = true;
        if (expr.type().isNullable()) {
            Solver.Rule r = new Solver.Rule();
            r.left = Operation.toSolverExpr(expr);
            r.type = "<>";
            r.right = Solver.number(0);
            if (!r.isComplete() || solver.isTrue(r)) {
                needTest = false;
            }
            if (needTest) {
                throw syntaxError("The expression '" + expr + "' could be null here. You need to verify using 'if " + expr + "' before accessing it.");
            }
        } else if (expr.type().memoryType() == MemoryType.OWNER) {
            // owners are known to _not_ be null, until they are cleared
            needTest = false;
            Solver.Rule r = new Solver.Rule();
            r.left = Operation.toSolverExpr(expr);
            r.type = "<>";
            r.right = Solver.number(0);
            if (!r.isComplete() || solver.isTrue(r)) {
                needTest = true;
            }
            if (!needTest) {
                throw syntaxError("The expression '" + expr + "' could be null here. You need to verify using 'if " + expr
                        + "' before accessing it.");
            }
        }
    }

    private String operatorToken() {
        if (type == TokenType.OPERATOR) {
            return token;
        } else if ("and".equals(token)) {
            return token;
        } else if ("or".equals(token)) {
            return token;
        } else if ("not".equals(token)) {
            return token;
        }
        return null;
    }

    private Expression parseExpression(Expression expr, int minPrecedence) {
        while (true) {
            String op = operatorToken();
            int prec = Operation.getPrecedence(op);
            if (token == null || prec < minPrecedence) {
                break;
            }
            read();
            // new line after operation
            matchOp("\n");
            Expression right = parseExpressionPrimary();
            while (true) {
                String o2 = operatorToken();
                int p2 = Operation.getPrecedence(o2);
                if (o2 == null || p2 <= prec) {
                    break;
                }
                right = parseExpression(right, prec + (p2 > prec ? 1 : 0));
            }
            if (Operation.isComparison(op) && (expr.isComparison() || right.isComparison())) {
                throw syntaxError("Comparing a result of a comparison requires parenthesis");
            }
            Operation operation = new Operation(expr, op, right);
            if ("/".equals(op) || "%".equals(op)) {
                if (!operation.type().isFloatingPoint()) {
                    verifyNotZero(right);
                }
            }
            expr = operation;
        }
        return expr;
    }

    private FullName readIdentifierWithPossibleModule() {
        String name = readIdentifier();
        String module = null;
        while (matchOp(".")) {
            if (module == null) {
                module = name;
            } else {
                module = module + "." + name;
            }
            name = readIdentifier();
        }
        return new FullName(module, name);
    }

    private String readIdentifier() {
        if (type != TokenType.IDENTIFIER) {
            throw syntaxError("Expected an identifier, got '" + token + "'");
        }
        String name = token;
        read();
        return name;
    }

    private void readSpaces() {
        token = null;
        lastPos = pos;
        indent = 0;
        while (true) {
            if (pos >= text.length()) {
                type = TokenType.END;
                return;
            }
            char c = text.charAt(pos);
            if (c == ' ') {
                pos++;
                indent++;
            } else if (c == '\n') {
                indent = 0;
                pos++;
            } else {
                break;
            }
        }
        read();
    }

    void read() {
        token = null;
        lastPos = pos;
        while (true) {
            if (pos >= text.length()) {
                type = TokenType.END;
                return;
            }
            char c = text.charAt(pos);
            if (c == ' ') {
                pos++;
            } else if (c == '#') {
                // comment
                pos++;
                c = text.charAt(pos);
                if (c == '#') {
                    // block comment
                    pos++;
                    int len = 2;
                    while (text.charAt(pos) == '#') {
                        pos++;
                        len++;
                    }
                    int start = pos;
                    while (pos < text.length()) {
                        while (pos < text.length() && text.charAt(pos) != '#') {
                            pos++;
                        }
                        int l2 = 0;
                        while (pos < text.length() && text.charAt(pos) == '#') {
                            pos++;
                            l2++;
                        }
                        if (l2 == len) {
                            break;
                        }
                    }
                    int end = Math.max(start, pos - 2);
                    lastComment = text.substring(start, end).trim();
                } else {
                    int start = pos;
                    while (true) {
                        c = text.charAt(pos);
                        if (c == '\n') {
                            break;
                        }
                        pos++;
                    }
                    lastComment = text.substring(start, pos).trim();
                }
                if (pos + 1 < text.length() && text.charAt(pos + 1) == '\n') {
                    // comments with newline afterwards are ignored
                    lastComment = null;
                }
                if (indent != 0) {
                    // indented comments are ignored
                    lastComment = null;
                }
            } else {
                break;
            }
        }
        int start = pos;
        char c = text.charAt(pos);
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') {
            pos++;
            c = text.charAt(pos);
            while ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                pos++;
                c = text.charAt(pos);
            }
            type = TokenType.IDENTIFIER;
            token = text.substring(start, pos);
            return;
        } else if (c >= '0' && c <= '9') {
            boolean floatingPoint = false;
            StringBuilder buff = new StringBuilder();
            buff.append((char) c);
            pos++;
            if (pos < text.length()) {
                c = text.charAt(pos);
                if (c == 'x') {
                    buff.append(c);
                    pos++;
                    c = text.charAt(pos);
                    while (true) {
                        if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {
                            buff.append((char) c);
                        } else {
                            break;
                        }
                        pos++;
                        c = text.charAt(pos);
                    }
                    type = TokenType.HEX_INTEGER;
                    token = buff.toString();
                } else {
                    while (true) {
                        if (c >= '0' && c <= '9') {
                            buff.append((char) c);
                        } else if (c == '.' && text.charAt(pos + 1) >= '0' && text.charAt(pos + 1) <= '9') {
                            floatingPoint = true;
                            buff.append(c);
                        } else if (c == 'e') {
                            floatingPoint = true;
                            buff.append(c);
                            if (text.charAt(pos + 1) == '-') {
                                buff.append('-');
                                pos++;
                            }
                        } else if (c == '_') {
                            // ignore
                        } else {
                            break;
                        }
                        pos++;
                        c = text.charAt(pos);
                    }
                    type = floatingPoint ? TokenType.FLOAT : TokenType.INTEGER;
                    token = buff.toString();
                }
            } else {
                type = floatingPoint ? TokenType.FLOAT : TokenType.INTEGER;
                token = buff.toString();
            }
        } else if (c == '\'') {
            pos++;
            boolean isAscii = true;
            StringBuilder buff = new StringBuilder();
            c = text.charAt(pos);
            while (true) {
                if (c == '\'') {
                    pos++;
                    break;
                } else if (c == '\\') {
                    pos++;
                    c = text.charAt(pos);
                    switch (c) {
                    case '\\':
                        buff.append(c);
                        break;
                    case 'n':
                        buff.append('\n');
                        break;
                    case 'r':
                        buff.append('\r');
                        break;
                    case 't':
                        buff.append('\t');
                        break;
                    case '\'':
                        buff.append('\'');
                        break;
                    case 'x': {
                        pos++;
                        if (pos + 2 >= text.length()) {
                            throw syntaxError("Expected '\\x00'");
                        }
                        String u = text.substring(pos, pos + 2);
                        pos += 1;
                        int x = Integer.parseInt(u, 16);
                        if (x > 127) {
                            isAscii = false;
                        }
                        buff.append((char) x);
                        break;
                    }
                    default:
                        throw syntaxError("Expected '\\n', '\\r', '\\t', '\\'', '\\\\', or '\\x'; got '" + c + "'");
                    }
                } else {
                    if (c > 127) {
                        isAscii = false;
                    }
                    buff.append((char) c);
                }
                pos++;
                if (pos >= text.length()) {
                    throw syntaxError("Unclosed string");
                }
                c = text.charAt(pos);
            }
            type = TokenType.STRING;
            if (isAscii) {
                token = buff.toString();
            } else {
                byte[] utf8 = new byte[buff.length()];
                for (int i = 0; i < buff.length(); i++) {
                    utf8[i] = (byte) (buff.charAt(i) & 0xff);
                }
                token = new String(utf8, StandardCharsets.UTF_8);
                byte[] u8b = token.getBytes(StandardCharsets.UTF_8);
                if (u8b.length != utf8.length) {
                    throw syntaxError("String literal is not normalized UTF-8");
                }
                for (int i = 0; i < buff.length(); i++) {
                    if (u8b[i] != utf8[i]) {
                        // the language doesn't need string literals to be valid UTF-8 sequences -
                        // this is a limitation of the current parser
                        throw syntaxError("String literal is not normalized UTF-8");
                    }
                }
            }
        } else if (c == '`') {
            pos++;
            int len = 1;
            while (text.charAt(pos) == '`') {
                pos++;
                len++;
            }
            int begin = pos;
            while (pos < text.length()) {
                while (pos < text.length() && text.charAt(pos) != '`') {
                    pos++;
                }
                int l2 = 0;
                while (text.charAt(pos) == '`') {
                    pos++;
                    l2++;
                }
                if (l2 == len) {
                    break;
                }
            }
            token = text.substring(begin, pos - len);
            type = TokenType.STRING;
            token = StringLiteral.unindentRawMultiLineString(token);
        } else if (c == '\t') {
            throw syntaxError("Tab characters are not supported sorry");
        } else if (c > ' ') {
            char first = c;
            pos++;
            type = TokenType.OPERATOR;
            c = text.charAt(pos);
            if (c == '=') {
                pos++;
            } else if (first == '@' && c == '@') {
                pos++;
            } else if (first == ']' && c == '!') {
                pos++;
            } else if (first == ':' && c == ':') {
                pos++;
            } else if (first == '.' && c == '.') {
                pos++;
            } else if (first == '<' && c == '>') {
                pos++;
            } else if (first == '>' && c == '>') {
                pos++;
                c = text.charAt(pos);
                if (c == '=') {
                    pos++;
                }
            } else if (first == '<' && c == '<') {
                pos++;
                c = text.charAt(pos);
                if (c == '=') {
                    pos++;
                }
            }
            token = text.substring(start, pos);
        } else {
            pos++;
            type = TokenType.OPERATOR;
            token = text.substring(start, pos);
        }
    }

    public String toString() {
        return text.substring(0, pos) + "[*]" + text.substring(pos);
    }

    public Variable assignTempVariable(ArrayList<Statement> target, Expression expr) {
        return assignTempVariable(target, expr, expr.type());
    }

    public Variable assignTempVariable(ArrayList<Statement> target, Expression expr, DataType type) {
        Assignment assign = new Assignment();
        assign.initial = true;
        assign.isConstant = true;
        // this is converted to "_t", but we can not use "_t" internally
        // because this can conflict with user-defined variable names.
        // user defined names can not start with a number
        // (same as "0r")
        Variable var = Variable.newInternal("0t" + functionContext.nextTempVariableId(), type);
        assign.type = type;
        assign.leftValue = var;
        assign.value = expr;
        Solver.Rule r = Operation.toRule(var, "==", expr);
        if (r != null) {
            r.depth = depth;
            r.always = true;
            solver.addRule(r);
        }
        assign.setConstantBounds(solver, var, expr);
        target.add(assign);
        functionContext.addVariable(var);
        return var;
    }

    private Value eval(Expression expr, boolean mayFail) {
        Value v = program.evalConstants(expr);
        if (v == null) {
            if (mayFail) {
                return null;
            }
            throw syntaxError("Can not evaluate the value");
        } else if (v instanceof ValueException) {
            ValueException ex = (ValueException) v;
            throw syntaxError("Evaluating the value threw an exception: " + ex.message);
        } else if (v instanceof ValuePanic) {
            ValuePanic ex = (ValuePanic) v;
            throw syntaxError("Evaluating the value failed: " + ex.message);
        }
        return v;
    }

}
