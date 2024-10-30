package org.bau.parser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.bau.parser.Bounds.ApplyType;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueException;
import org.bau.runtime.Value.ValueI8Array;
import org.bau.runtime.Value.ValueInt;
import org.bau.runtime.Value.ValuePanic;
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

    // the block level is the level of indentation
    // (1 for a function, and incremented by one for each nesting level)
    private ArrayList<Expression> blockConditions = new ArrayList<>();
    // the block id is is incremented for each if / elif / else block
    private Program program;
    private FunctionContext functionContext;
    private boolean isGlobalScope;
    public boolean isImport;
    private int nextContinueId;
    private int nextConstantId;
    private String module;

    // the stack starting position of the current loop (of the scope)
    private int stackPosLoop;
    // the stack starting position of the function
    // (after the parameters)
    private int stackPosFunction;
    private FunctionDefinition currentFunctionDefinition;
    private While currentLoop;
    private boolean scanPhase = true;

    public Parser(String text) {
        this(new Program(Collections.emptyMap()), null, text);
    }

    public Parser(Program program, String module, String text) {
        this.program = program;
        this.functionContext = new FunctionContext(program);
        this.module = module;
        // add a newline to simplify end detection
        this.text = text + "\n";
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
                }
            }
            for (FunctionDefinition def : functions) {
                if (def.code != null) {
                    String f = def.toString();
                    Parser p = new Parser(program, def.module, f);
                    functionContext.reset();
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
                    Parser p = new Parser(program, def.module, f);
                    functionContext.reset();
                    p.functionContext = functionContext;
                    p.scanPhase = false;
                    p.parse();
                }
            }
            if (module == null) {
                FunctionDefinition main = program.getFunctionIfExists(null, null, "main", 0);
                if (main != null) {
                    program.removeFunction(main);
                    program.list.addAll(main.list);
                    program.autoClose = main.autoClose;
                }
            }
        }
        return program;
    }

    public ArrayList<Expression> getBlockConditions() {
        return blockConditions;
    }

    private int getLine(int pos) {
        int line = 1;
        for(int i=0; i<pos; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
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
                if (mainStatements && module == null) {
                    FunctionDefinition old = program.getFunctionIfExists(null, null, "main", 0);
                    if (old != null) {
                        throw syntaxError("Function 'main' already exists");
                    }
                    pos = lastPos;
                    String mainCode = parseBlock(-1);
                    FunctionDefinition def = new FunctionDefinition();
                    def.name = "main";
                    def.code = Statement.indent(mainCode);
                    program.addFunction(def);
                } else {
                    isGlobalScope = true;
                    parseStatements(program.list);
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
                Parser parser = new Parser(program, name, moduleSource);
                parser.isImport = true;
                parser.parse();
            } catch (IllegalStateException e) {
                throw syntaxError("Error parsing module: " + e.getMessage(), e);
            }
        }
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
            while (true) {
                String t = readIdentifier();
                parameters.add(t);
                DataType type = new DataType(targetModule, t, 0, false, Collections.emptyList());
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
        int sizeOf = 0;
        readEndOfStatement();
        functionContext.rewindStack(stackPos);
        if (template) {
            parseTypeTemplate(defIndent, name, parameters, comment);
            return true;
        }
        ArrayList<Variable> fields = new ArrayList<>();
        DataType type = new DataType(targetModule, name, sizeOf, false, fields);
        program.addType(type);
        while (indent > defIndent) {
            if (!matchOp("\n")) {
                String fieldName = readIdentifier();
                DataType fieldType = readType(false);
                readEndOfStatement();
                sizeOf += type.sizeOf();
                Variable var = new Variable(fieldName, fieldType);
                fields.add(var);
            }
        }
        program.addComment("type " + type.toString(), comment);
        lastComment = null;
        if (!parameters.isEmpty()) {
            type.parameters = parameters;
        }
        functionContext.rewindStack(stackPos);
        return true;
    }

    private void parseTypeTemplate(int defIndent, String name, ArrayList<String> parameters, String comment) {
        String code = parseBlock(defIndent);
        DataType type = new DataType(module, name, 0, false, Collections.emptyList());
        type.parameters = parameters;
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
        while (true) {
            if ("\n".equals(token)) {
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
                    if (expr.type().isFloatingPoint || expr.type().isNullable() || !expr.type().isSystem()) {
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

        DataType type = new DataType(module, id, 8, false, Collections.emptyList());
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
        String id = readIdentifier();
        DataType t1 = functionContext.getType(targetModule, id);
        if (t1 != null) {
        	if (matchOp("[")) {
                if (!matchOp("]")) {
                	throw syntaxError("Expected ']', got '" + token + "' when reading type");
                }
                t1 = t1.arrayType();
        	}
        }
        if (t1 != null && t1.template != null) {
            if (!matchOp("(")) {
                throw syntaxError("Expected '(', got '" + token + "' when reading a function definition template");
            }
            for (int i = 0; i < t1.parameters.size(); i++) {
                String p = readIdentifier();
                String expected = t1.parameters.get(i);
                if (!p.equals(expected)) {
                    throw syntaxError("Expected '" + expected + "' , got '" + p
                            + "' when reading a function definition template");
                }
                matchOp(",");
            }
            if (!matchOp(")")) {
                throw syntaxError("Expected ')', got '" + token + "' when reading a function definition template");
            }
            parseTypeFunctionTemplate(defIndent, t1);
            return true;
        }
        int stackPos = functionContext.getStackPos();
        FunctionDefinition def = new FunctionDefinition();
        if (currentFunctionDefinition != null) {
            throw new IllegalStateException();
        }
        currentFunctionDefinition = def;
        def.module = targetModule;
        if (matchOp("(")) {
            def.name = id;
        } else {
            def.callType = functionContext.getType(targetModule, id);
            def.name = readIdentifier();
            if (!matchOp("(")) {
                throw syntaxError("Expected '(', got '" + token + "' when reading a function definition");
            }
            DataType type = functionContext.getType(targetModule, id);
            if (type == null) {
                throw syntaxError("Type '" + id + "' not found when reading a function definition");
            }
            type.used();
            Variable var = new Variable("this", type);
            var.isConstant = true;
            def.parameters.add(var);
            functionContext.addVariable(var);
        }
        boolean varArgs = false;
        boolean template = false;
        if (!matchOp(")")) {
            while (true) {
                String name = readIdentifier();
                DataType type;
                if (DataType.isGenericTypeName(token) && !template) {
                    template = true;
                    type = new DataType(targetModule, token, 0, false, Collections.emptyList());
                    functionContext.addTemporaryType(type);
                    type = readType(template);
                    if (matchOp("..")) {
                        varArgs = true;
                        type = type.arrayType();
                        type.used();
                    }
                    Variable var = new Variable(name, type);
                    var.isConstant = true;
                    def.parameters.add(var);
                    functionContext.addVariable(var);
                } else if (match("type")) {
                    template = true;
                    type = program.getType2(null, DataType.TYPE);
                    DataType t = new DataType(targetModule, name, 0, false, Collections.emptyList());
                    functionContext.addTemporaryType(t);
                    // we change the variable name, because the name is already a type
                    Variable var = new Variable("_" + name, type);
                    var.isConstant = true;
                    def.parameters.add(var);
                    functionContext.addVariable(var);
                } else {
                    type = readType(template);
                    if (matchOp("..")) {
                        varArgs = true;
                        type = type.arrayType();
                        type.used();
                    }
                    Variable var = new Variable(name, type);
                    var.isConstant = true;
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
                if (def.exceptionType.isArray() || def.exceptionType.isPointer()) {
                    throw syntaxError("May only throw value types");
                }
                boolean found = false;
                for (Variable f : def.exceptionType.fields) {
                    if (f.name.equals("exceptionType")) {
                        if (f.type != program.getType2(null, DataType.INT)) {
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
        stackPosFunction = functionContext.getStackPos();
        if (def.varArgs) {
            // for varargs, we have an array that needs to be freed
            stackPosFunction--;
        }
        FunctionDefinition old = program.getFunctionIfExists(def.callType, def.module, def.name, def.parameters.size());
        if (scanPhase && !def.macro) {
            if (old != null) {
                throw syntaxError("Function '" + def.name + "' already exists");
            }
            if (template) {
                parseFunctionTemplate(defIndent, def);
                functionContext.rewindStack(stackPos);
                currentFunctionDefinition = null;
                return true;
            }
            int startCode = lastPos;
            String s = parseBlock(defIndent);
            String header = text.substring(startParse, startCode).trim() + "\n";
            def.header = header;
            def.code = s;
            def.comment = comment;
            program.addFunction(def);
            functionContext.rewindStack(stackPos);
            currentFunctionDefinition = null;
            return true;
        }
        if (old != null) {
            if (old.list.isEmpty()) {
                program.removeFunction(old);
                // this ensures it is not called
                old.list = null;
                if (old.used) {
                    def.used = true;
                }
            } else {
                throw syntaxError("Function '" + def.name + "' already has an implementation");
            }
        }
        program.addComment(def.toString(), comment);
        program.addFunction(def);
        addBlockCondition(null);
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
        def.autoClose(autoClose);
        functionContext.rewindStack(stackPos);
        currentLoop = null;
        undoLastBlockCondition();
        if (!blockConditions.isEmpty()) {
            throw new IllegalStateException();
        }
        currentFunctionDefinition = null;
        if (def.macro) {
            Templates.checkMacroFunction(def);
            program.addFunctionTemplate(null, module, def.name, def);
        }
        return true;
    }

    private void parseTypeFunctionTemplate(int defIndent, DataType t) {
        String comment = lastComment;
        int start = lastPos;
        // read to end of the line
        while (true) {
            if ("\n".equals(token)) {
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
        buff.append(" " + functionName + "\n");
        buff.append(code);
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
        if (DataType.TYPE.equals(token)) {
            throw syntaxError("Type '" + token + "' may not be used here");
        }
        if ("0".equals(token)) {
            read();
            if (matchOp("..")) {
                Expression upperBound = parseExpression();
                if (upperBound.canThrowException() != null) {
                    throw syntaxError("May not throw an exception here");
                }
                String rangeTypeName = "0.." + upperBound.toString();
                DataType newType = new DataType(null, rangeTypeName, 8, true, false, Collections.emptyList(), false);
                newType.maxValue = upperBound;
                functionContext.addTemporaryType(newType);
                return newType;
            }
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
            if (!matchOp("(")) {
                throw syntaxError("Type '" + name + "' is a template; need to specify the parameters");
            }
            ArrayList<DataType> params = new ArrayList<>();
            for (int i = 0; i < t.parameters.size(); i++) {
                DataType t2 = readType(templatesOk);
                params.add(t2);
                matchOp(",");
            }
            if (!matchOp(")")) {
                throw syntaxError("Type '" + name + "' is a template; need to specify " + t.parameters.size() + " parameters");
            }
            // if templates can be returned, then
            // we must not replace the types
            if (!templatesOk) {
                String typeId = DataType.getId(name, params);
                DataType t2 = functionContext.getType(t.module, typeId);
                if (t2 != null) {
                    t = t2;
                } else {
                    String code = t.template;
                    ArrayList<String> with = new ArrayList<>();
                    for (int i = 0; i < t.parameters.size(); i++) {
                        with.add(params.get(i).fullName());
                    }
                    code = Templates.convertTemplate(code, t.parameters, with);
                    code = "type " + typeId + "\n" + code;
                    try {
                        Parser p = new Parser(program, module, code);
                        p.read();
                        p.parseTypeDefinition(t.module);
                        while (p.type != TokenType.END) {
                            p.parseFunctionDefinition(t.module);
                        }
                        t = functionContext.getType(t.module, typeId);
                    } catch (IllegalStateException e) {
                        throw syntaxError("Error parsing template: " + e.getMessage(), e);
                    }
                }
            }
        }
        if (matchOp("[")) {
            if (!matchOp("]")) {
                throw syntaxError("Expected ']', got '\"+token+\"' when reading a type");
            }
            t = t.arrayType();
        }
        t.used();
        if (matchOp("?")) {
            if (t.isArray()) {
                throw syntaxError("Array can't be null (but they can be empty)");
            } else if (t.isSystem()) {
                throw syntaxError("Built-in types can't be be null (but the value can be zero)");
            } else if (!t.isPointer()) {
                throw syntaxError("Value types can't be be null (but the value can be zero)");
            }
            return t.orNull();
        }
        return t;
    }

    private void parseStatements(ArrayList<Statement> target) {
        if (matchOp("\n")) {
            return;
        }
        if (type == TokenType.IDENTIFIER) {
            if (match("if")) {
                parseIf(target);
                return;
            } else if (match("while")) {
                parseWhile(target);
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
            String m = module;
            String identifier = readIdentifier();
            // if there is no variable with this name,
            // TODO it could be a this.field name too?
            // then it could be a fully qualified module name
            if (functionContext.getVariable(null, identifier) == null) {
                while (matchOp(".")) {
                    m = m == module ? identifier : m + "." + identifier;
                    identifier = readIdentifier();
                }
                String m2 = program.getImport(m);
                if (m2 != null) {
                    m = m2;
                }
            }
            DataType targetType = null;
            if (type == TokenType.IDENTIFIER) {
                targetType = readType(true);
            }
            if (matchOp(":")) {
                if (m != module && !m.equals(module)) {
                    throw syntaxError("Can not define a constant in a different module");
                }
                Assignment s = new Assignment();
                s.isConstant = true;
                s.isGlobalScope = isGlobalScope;
                s.initial = true;
                // no need for temp variables as it's just an assignment
                s.value = parseExpression();
                s.value = s.value.writeStatements(this, true, target);
                boolean global = isGlobalScope;
                Variable v = new Variable(module, identifier, global, s.value.type());
                v.isConstant = true;
                v.constantValue = eval(s.value, true);
                if (v.constantValue instanceof Value.ValueRef) {
                    // the value would be in the heap, but we don't retain the heap
                    // here, so the data would be lost
                    v.constantValue = null;
                }
                if (s.value.type().isArray() && s.value instanceof New) {
                    New n = (New) s.value;
                    v.addLenBoundCondition(null, "=", n.arrayLength);
                }
                v.setBoundValue(null, "=", s.value);
                s.leftValue = v;
                s.type = s.value.type();
                functionContext.addVariable(v);
                if (global) {
                    program.addGlobalVariable(v);
                }
                verifyBounds(s);
                s.setBounds(getScope(0));
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp(":=")) {
                if (m != module && !m.equals(module)) {
                    throw syntaxError("Can not create a new variable in a different module");
                }
                Assignment s = new Assignment();
                s.initial = true;
                // no need for temp variables as it's just an assignment
                s.value = parseExpression();
                s.value = s.value.writeStatements(this, true, target);
                DataType type = s.value.type();
                if (type.isArray()) {
                    throw syntaxError("Arrays need to be declared as constants to simplify array-bound verification");
                }
                if (matchOp("..")) {
                    if (!"0".equals(s.value.toString())) {
                        throw syntaxError("Range needs to start from 0: '"+s+"'");
                    }
                    Expression upperBound = parseExpression();
                    if (upperBound.canThrowException() != null) {
                        throw syntaxError("May not throw an exception here");
                    }
                    String rangeTypeName = "0.." + upperBound.toString();
                    DataType rangeType = functionContext.getType(null, rangeTypeName);
                    if (rangeType == null) {
                        rangeType = new DataType(null, rangeTypeName, 8, true, false, Collections.emptyList(), false);
                        rangeType.maxValue = upperBound;
                        functionContext.addTemporaryType(rangeType);
                    }
                    type = rangeType;
                }
                boolean global = isGlobalScope;
                Variable v = new Variable(module, identifier, global, type);
                s.leftValue = v;
                s.type = s.value.type();
                if (functionContext.getVariable(module, v.name) != null) {
                    throw syntaxError("Variable '" + v.name + "' already exists");
                }
                functionContext.addVariable(v);
                if (global) {
                    program.addGlobalVariable(v);
                }
                verifyBounds(s);
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("(")) {
                if ("native".equals(identifier)) {
                    String s = token;
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
                        s = s.substring(index + 1);
                    }
                    readEndOfStatement();
                    target.add(new NativeCode(s + "\n"));
                    return;
                }
                Call call = new Call();
                call.statement = true;
                Expression expr = parseCall(null, m, identifier, call, true);
                readEndOfStatement();
                expr = expr.writeStatements(this, false, target);
                if (expr instanceof Call) {
                    target.add((Call) expr);
                }
                return;
            } else if (matchOp("\n") && targetType != null) {
                // TODO assign null or zero
                Assignment s = new Assignment();
                s.initial = true;
                Expression value;
                if (targetType.isSystem()) {
                    value = new NumberValue(Value.ValueInt.ZERO, targetType, false);
                } else {
                    value = new NullValue();
                }
                s.value = value;
                boolean global = isGlobalScope;
                Variable v = new Variable(module, identifier, global, targetType);
                s.leftValue = v;
                s.type = targetType;
                if (functionContext.getVariable(module, v.name) != null) {
                    throw syntaxError("Variable '" + v.name + "' already exists");
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
            LeftValue left = functionContext.getVariable(module, identifier);
            if (left == null) {
                left = functionContext.getVariable(null, "this");
                if (left == null) {
                    throw syntaxError("Variable not found: '"+identifier+"' \n"+
                            "(constants are declared with ':', " +
                            "new variable are declared with ':=')");
                }
                verifyNullAccess(left);
                DataType vt = left.type();
                DataType type = vt.getFieldDataType(identifier);
                if (type == null) {
                    throw syntaxError("Variable not found: '" + identifier + "' \n"
                            + "(constants are declared with ':', " +
                            "new variable are declared with ':=')");
                }
                left = new FieldAccess(left, identifier, type);
            }
            while (true) {
                if(matchOp(".")) {
                    String f = readIdentifier();
                    if (matchOp("(")) {
                        Call call = new Call();
                        call.statement = true;
                        call.args.add(left);
                        parseCall(left.type(), m, f, call, true);
                        readEndOfStatement();
                        target.add(call);
                        return;
                    } else {
                        DataType type;
                        if ("len".equals(f) && left.type().isArray()) {
                            type = program.getType2(null, DataType.I32);
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
                    if (checkBounds) {
                        program.getFunction(null, null, "idx", 2).used = true;
                    }
                    left = new ArrayAccess(left, arrayIndex, checkBounds);
                } else {
                    break;
                }
            }
            Assignment s = new Assignment();
            s.leftValue = left;
            if (matchOp("=")) {
                s.value = parseExpression();
                s.type = s.value.type();
                verifyBounds(s);
                s.setBounds(getScope(0));
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("*=")) {
                s.modify = "*";
                s.value = parseExpression();
                s.type = s.value.type();
                verifyBounds(s);
                s.setBounds(getScope(0));
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("/=")) {
                s.modify = "/";
                s.value = parseExpression();
                s.type = s.value.type();
                verifyBounds(s);
                s.setBounds(getScope(0));
                program.getFunction(null, null, "idiv", 2).used = true;
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("+=")) {
                s.modify = "+";
                s.value = parseExpression();
                s.type = s.value.type();
                verifyBounds(s);
                s.setBounds(getScope(0));
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("-=")) {
                s.modify = "-";
                s.value = parseExpression();
                s.type = s.value.type();
                verifyBounds(s);
                s.setBounds(getScope(0));
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("&=")) {
                s.modify = "&";
                s.value = parseExpression();
                s.type = s.value.type();
                verifyBounds(s);
                s.setBounds(getScope(0));
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("|=")) {
                s.modify = "|";
                s.value = parseExpression();
                s.type = s.value.type();
                verifyBounds(s);
                s.setBounds(getScope(0));
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("^=")) {
                s.modify = "^";
                s.value = parseExpression();
                s.type = s.value.type();
                verifyBounds(s);
                s.setBounds(getScope(0));
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp(">>=")) {
                s.modify = ">>";
                s.value = parseExpression();
                s.type = s.value.type();
                verifyBounds(s);
                s.setBounds(getScope(0));
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("<<=")) {
                s.modify = "<<";
                s.value = parseExpression();
                s.type = s.value.type();
                verifyBounds(s);
                s.setBounds(getScope(0));
                readEndOfStatement();
                target.add(s);
                return;
            }
        }
        throw syntaxError("Expected a statement, got '" + token + "'");
    }

    private boolean needBoundsCheck(Expression base, Expression arrayIndex) {
        Value v = arrayIndex.eval(null);
        if (v != null) {
            if (base instanceof Variable) {
                Variable var = (Variable) base;
                Bounds b = var.getLenBounds();
                if (b != null && b.largerThan(v.longValue())) {
                    return false;
                }
            }
            Value v2 = base.eval(null);
            if (v2 != null && v.longValue() < v2.longValue()) {
                return false;
            }
        }
        FieldAccess f = new FieldAccess(base, "len", program.getType2(null, DataType.INT));
        Bounds b = arrayIndex.getBounds();
        if (b != null && b.compareTo(this, f) < 0) {
            return false;
        }
        DataType indexType = arrayIndex.type();
        if (indexType.maxValue == null) {
            return true;
        }
        // TODO this is a bit fuzzy; need to compare identity
        // and verify bounds exactly
        if (indexType.maxValue.toString().startsWith(base.toString() + ".len")) {
            return false;
        }
        return true;
    }

    private void verifyBounds(Assignment s) {
        if (!s.leftValue.type().isFloatingPoint) {
            if (s.value.type().isFloatingPoint) {
                // TODO there are many more cases that are not allowed
                throw syntaxError("The expression is floating point, but the variable is not.");
            }
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
            throw syntaxError("The expression may be 'null', but this is not allowed here.");
        }
        Expression max = targetType.maxValue;
        if (max == null) {
            return;
        }

        Value v = expr.eval(null);
        Value m = max.eval(null);
        if (v != null && m != null) {
            if (v.longValue() > m.longValue()) {
                // TODO handle overflow etc
                throw syntaxError("Value is out-of-range");
            }
            return;
        }
        DataType exprType = expr.type();
        if (exprType == targetType) {
            return;
        }
        Bounds b = expr.getBounds();
        if (b == null) {
            throw syntaxError("Can not verify if value is smaller than '" + max + "'");
        }
        int result = b.compareTo(this, max);
        if (result < 0) {
            return;
        }
        Bounds mb = max.getBounds();
        if (mb != null) {
            result = mb.compareTo(this, expr);
            if (result > 0) {
                return;
            }
        }
        throw syntaxError("Can not verify if value is smaller than '" + max + "'");
    }

    private void readEndOfStatement() {
        lastComment = null;
        if (token != null && !matchOp(";") && !matchOp("\n")) {
            throw syntaxError("Expected end of statement, got '" + token + "'");
        }
    }

    private Expression parseMacro(FunctionDefinition def) {
        boolean lastWasComma = false;
        int pi = 0;
        ArrayList<Variable> params = new ArrayList<>();
        ArrayList<Expression> args = new ArrayList<>();
        DataType type = def.returnType;
        boolean isGeneric = type != null && DataType.isGenericTypeName(type.name());
        while (true) {
            if (matchOp(")")) {
                break;
            }
            boolean checkComma = !lastWasComma && pi > 0;
            if (checkComma) {
                Expression last = args.get(args.size() - 1);
                if (!last.isEasyToRead()) {
                    throw syntaxError("Expected ',' after '" + last
                            + "' or parentheses around the expression, to make it easier to read");
                }
            }
            Expression p = parseExpression();
            if (checkComma) {
                if (!p.isEasyToRead()) {
                    throw syntaxError("Expected ',' before '" + p
                            + "' or parentheses around the expression, to make it easier to read");
                }
            }
            Variable var = def.parameters.get(pi);
            // replace generic types
            if (isGeneric) {
                def.parameters.get(pi);
                if (var.type.name().equals(type.name())) {
                    type = p.type();
                }
            }
            params.add(var);
            args.add(p);
            // dangling ',' is supported
            lastWasComma = matchOp(",");
            // new line after operation
            matchOp("\n");
            pi++;
        }
        TernaryExpression ternary = new TernaryExpression();
        ternary.type = type;
        for (Statement s : def.list) {
            if (s instanceof If) {
                If ifStatement = (If) s;
                Expression condition = ifStatement.conditions.get(0);
                ternary.condition = replaceAll(condition, params, args);
                ArrayList<Statement> ifTrue = ifStatement.listList.get(0);
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
                if (ifStatement.listList.size() > 1) {
                    ArrayList<Statement> ifFalse = ifStatement.listList.get(1);
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
            }
        }
        return ternary;
    }

    private static Expression replaceAll(Expression expr, ArrayList<Variable> params, ArrayList<Expression> args) {
        ArrayList<Variable> newParams = new ArrayList<>();
        for (Variable p : params) {
            Variable v2 = new Variable("_" + p.name, p.type);
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
            Variable v2 = new Variable("_" + p.name, p.type);
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

    private Expression parseCall(DataType type, String module, String identifier, Call call, boolean use) {
        if (type != null && type.module != null) {
            module = type.module;
        }
        FunctionDefinition template = program.getFunctionTemplate(type, module, identifier);
        if (template == null) {
            template = program.getFunctionTemplate(type, null, identifier);
        }
        if (template != null && template.macro) {
            return parseMacro(template);
        }
        ArrayList<String> templateNames = new ArrayList<>();
        ArrayList<String> templateParams = new ArrayList<>();
        boolean lastWasComma = false;
        int pi = 0;
        while (true) {
            if (matchOp(")")) {
                break;
            }
            boolean checkComma = !lastWasComma && pi > 0;
            if (checkComma) {
                Expression last = call.args.get(call.args.size() - 1);
                if (!last.isEasyToRead()) {
                    throw syntaxError("Expected ',' after '" + last + "' or parentheses around the expression, to make it easier to read");
                }
            }
            if (template != null && pi < template.parameters.size() && DataType.TYPE.equals(template.parameters.get(pi).type.name())) {
                if (DataType.TYPE.equals(token)) {
                    throw syntaxError("Type '" + token + "' may not be used here");
                }
                String name = readIdentifier();
                DataType t = functionContext.getType(module, name);
                if (t == null) {
                    t = functionContext.getType(this.module, name);
                }
                if (t == null) {
                    throw syntaxError("Type '" + name + "' not found when reading a type");
                }
            	if (matchOp("[")) {
                    if (!matchOp("]")) {
                    	throw syntaxError("Expected ']', got '" + token + "' when reading type");
                    }
                    t = t.arrayType();
            	}
                String pName = template.parameters.get(pi).name;
                if (pName.startsWith("_")) {
                    pName = pName.substring(1);
                }
                templateNames.add(pName);
                templateParams.add(t.fullName());
                // we add a dummy value for each type parameter,
                // to simplify the parser a bit
                Expression p = new NumberValue(ValueInt.ZERO, program.getType2(null, DataType.INT), false);
                call.args.add(p);
            } else {
                Expression p = parseExpression();
                if (template != null && pi < template.parameters.size()) {
                    // template without explicit type parameters
                    // TODO this is not quite correct:
                    // we should probably have have a flag "templateWithoutExplicityParams"
                    if (templateNames.isEmpty()) {
                        DataType t = template.parameters.get(pi).type;
                        if (template.varArgs && pi == template.parameters.size() - 1) {
                            // the last parameters of a varargs function is an array type
                            t = t.baseType();
                        }
                        String typeName = t.name();
                        if (DataType.isGenericTypeName(typeName)) {
                            templateNames.add(typeName);
                            DataType pt = p.type();
                            templateParams.add(pt.fullName());
                            if (t.isArray()) {
                                // also replace the non-array version
                                t = t.baseType();
                                typeName = t.name();
                                templateNames.add(typeName);
                                pt = p.type();
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
        }
        if (template != null) {
            String fullName = identifier;
            for(String t : templateParams) {
                fullName += "_" + t.replace('.', '_').replace("[]", "_array");
            }
            call.def = program.getFunctionIfExists(type, module, fullName, call.args.size());
            if (call.def == null) {
                String code = template.template;
                code = Templates.convertTemplate(code, templateNames, templateParams);
                String header = template.toString();
                header = Templates.convertTemplate(header, template.name, fullName);
                header = Templates.convertTemplate(header, "type", "int");
                header = Templates.convertTemplate(header, templateNames, templateParams);
                code = header.trim() + "\n" + code;
                try {
                    Parser p = new Parser(program, module, code);
                    p.read();
                    p.parseFunctionDefinition(module);
                    call.def = program.getFunctionIfExists(type, module, fullName, call.args.size());
                } catch (IllegalStateException e) {
                    throw syntaxError("Error parsing template: " + e.getMessage(), e);
                }
            }
        } else {
            call.def = program.getFunctionIfExists(type, module, identifier, call.args.size());
        }
        if (call.def == null) {
            throw syntaxError("Function '" + identifier + "' not found");
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
                buff.append(call.def.parameters.get(i).name);
            }
            buff.append(")");
            throw syntaxError(buff.toString());
        }
        if (use) {
            call.def.used = true;
        }
        if (call.exceptionType() != null) {
            exceptionType = call.exceptionType();
        }
        return call;
    }

    private void parseReturn(ArrayList<Statement> target) {
        if (currentFunctionDefinition == null) {
            throw syntaxError("Return needs to be inside of a function");
        }
        Return b = new Return(null);
        if (blockConditions.size() > 0) {
            Expression expr = blockConditions.get(blockConditions.size() - 1);
            if (expr != null) {
                expr.applyBoundCondition(getScope(-1), ApplyType.NEGATIVE);
            }
        }
        if (matchOp("\n") || matchOp(";")) {
            if (currentFunctionDefinition.returnType != null) {
                throw syntaxError("The function does not return an expression of type " + currentFunctionDefinition.returnType);
            }
            target.add(b);
            return;
        }
        // if it is not a simple value  (e.g. string1 + string2)
        // assign it first to a variable so that the components can be freed up
        b.expr = parseExpression(target);
        if (currentFunctionDefinition.returnType == null) {
            throw syntaxError("The function declared to not return a value");
        }
        if (!b.expr.isSimple()) {
            String constId = "_r" + nextConstantId++;
            Assignment ret = new Assignment();
            ret.initial = true;
            ret.isConstant = true;
            DataType type = b.expr.type();
            if (type == null) {
                throw syntaxError("No type");
            }
            ret.leftValue = new Variable(constId, type);
            ret.type = b.expr.type();
            ret.value = b.expr;
            b.expr = ret.leftValue;
            target.add(ret);
        }
        verifyBounds(currentFunctionDefinition.returnType, b.expr);
        b.autoClose = autoClose(stackPosFunction, b.expr);
        if (matchOp("\n") || matchOp(";")) {
            target.add(b);
            return;
        }
        throw syntaxError("Expected end of statement, got '" + token + "' in 'return' statement");
    }

    private void parseCatch(ArrayList<Statement> target) {
        int catchIndent = indent;
        int stackPos = functionContext.getStackPos();
        Catch catchStat = new Catch();
        String id = readIdentifier();
        Variable var = new Variable(id, exceptionType);
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
        Throw t = new Throw();
        if (matchOp("\n") || matchOp(";")) {
            target.add(t);
            return;
        }
        t.expr = parseExpression(target);
        // theoretically, the exception could be caught in this method
        // so we can not verify that the exception type matches the one
        // declared in the function
        if (matchOp("\n") || matchOp(";")) {
            target.add(t);
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
            return;
        }
        b.condition = parseCondition(target);
        b.condition.applyBoundCondition(getScope(0), ApplyType.NEGATIVE);
        b.autoClose = autoClose(stackPosLoop, null);
        if (matchOp("\n") || matchOp(";")) {
            target.add(b);
            return;
        }
        throw syntaxError("Expected end of statement, got '" + token + "' in 'break' statement");
    }

    private Expression getScope(int level) {
        int l = getBlockConditions().size() + level - 1;
        if (l < 0) {
            return null;
        }
        return getBlockConditions().get(l);
    }

    private void parseContinue(ArrayList<Statement> target) {
        if (currentLoop == null) {
            throw syntaxError("'continue' statement outside of a loop");
        }
        Continue c = new Continue();
        if (matchOp("\n") || matchOp(";")) {
            target.add(c);
            return;
        }
        c.continuedId = currentLoop.continueId;
        c.condition = parseCondition(target);
        c.condition.applyBoundCondition(getScope(0), ApplyType.NEGATIVE);
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
            return new Operation(expr, "<>", new NullValue());
        }
        return expr;
    }

    private void parseSwitch(ArrayList<Statement> target) {
        int switchIndent = indent;
        If ifStatement = new If();
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
                    Expression cond = new Operation(switchExpr, "=", caseExpr);
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
                    undoLastBlockCondition();
                }
                addBlockCondition(condition);
                first = false;
                ifStatement.conditions.add(condition);
            } else if (match("else")) {
                if (!matchOp("\n")) {
                    throw syntaxError("Expected end of statement, got '" + token + "' in 'switch' statement");
                }
                undoLastBlockCondition();
                addBlockCondition(null);
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
            if (!list.isEmpty()) {
                ifStatement.listList.add(list);
                ifStatement.autoClose(autoClose(stackPos, null));
                functionContext.rewindStack(stackPos);
            }
            if (elsePart) {
                break;
            }
            switchIndent = indent;
        }
        undoLastBlockCondition();
        target.add(ifStatement);
    }

    private void parseIf(ArrayList<Statement> target) {
        int ifIndent = indent;
        boolean sameLine;
        If ifStatement = new If();
        Expression condition = parseCondition(target);
        addBlockCondition(condition);
        ifStatement.conditions.add(condition);
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
            ifStatement.listList.add(list);
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
            ifStatement.autoClose(autoClose(stackPos, null));
            functionContext.rewindStack(stackPos);
            if (elsePart) {
                break;
            }
            if (indent < ifIndent) {
                break;
            }
            ifIndent = indent;
            if (match("elif")) {
                undoLastBlockCondition();
                condition = parseCondition(target);
                addBlockCondition(condition);
                ifStatement.conditions.add(condition);
            } else if (match("else")) {
                undoLastBlockCondition();
                addBlockCondition(null);
                elsePart = true;
            } else {
                break;
            }
        }
        undoLastBlockCondition();
        target.add(ifStatement);
    }

    private void parseFor(ArrayList<Statement> target) {
        int loopIndent = indent;
        String variableName = readIdentifier();
        if (!matchOp(":=")) {
            throw syntaxError("Expected ':=', got '" + token + "' in 'for' statement");
        }
        String method = readIdentifier();
        if (!matchOp("(")) {
            throw syntaxError("Expected a function call, got '" + token + "' in 'for' statement");
        }
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
        stackPosLoop = stackPos;
        While outerLoop = new While();
        outerLoop.continueId = nextContinueId++;
        ArrayList<Variable> oldArgs = new ArrayList<>();
        ArrayList<Expression> newArgs = new ArrayList<>();
        for (int i = 0; i < functionDef.parameters.size(); i++) {
            Variable v = functionDef.parameters.get(i);
            Variable v2 = new Variable("_" + v.name, v.type);
            v2.isConstant = true;
            oldArgs.add(v);
            newArgs.add(call.args.get(i));
        }
        DataType type = functionDef.returnType;
        if (type.maxValue != null) {
            // TODO hack: replace maxValue for loops over arrays
            type.maxValue = call.args.get(0);
        }
        Operation comp = new Operation(
                new NumberValue(new Value.ValueInt(1), program.getType2(null, DataType.INT), false),
                "=",
                new NumberValue(new Value.ValueInt(1), program.getType2(null, DataType.INT), false)
                );
        comp.operator = "=";
        Variable var = new Variable(variableName, call.type());
        functionContext.addVariable(var);
        While loop = new While();
        loop.continueId = nextContinueId++;
        int i = 0;
        Variable old = new Variable("_", call.def.returnType);
        ArrayList<Statement> whileLoop = null;
        if (functionDef.list.size() == 1) {
            Statement stat = functionDef.list.get(0);
            if (stat instanceof If) {
                If w = (If) stat;
                Expression condition = w.conditions.get(0);
                condition = condition.replace(old, var);
                for(int k = 0; k < oldArgs.size(); k++) {
                    condition = condition.replace(oldArgs.get(k), newArgs.get(k));
                }
                outerLoop.condition = condition;
                functionDef.list = w.listList.get(0);
            }
        }
        addBlockCondition(comp);
        outerLoop.condition = comp;
        for (; i < functionDef.list.size(); i++) {
            Statement s = functionDef.list.get(i);
            s = s.replace(old, var);
            for(int k = 0; k < oldArgs.size(); k++) {
                s = s.replace(oldArgs.get(k), newArgs.get(k));
            }
            if (s instanceof While) {
                While w = (While) s;
                whileLoop = w.list;
                loop.condition = w.condition;
                loop.condition.applyBoundCondition(getScope(0), ApplyType.POSITIVE);
                i++;
                break;
            }
            s.setBounds(getScope(0));
            outerLoop.list.add(s);
        }
        While oldLoop = currentLoop;
        currentLoop = loop;
        addBlockCondition(loop.condition);
        int j = 0;
        for (; j < whileLoop.size(); j++) {
            Statement s = whileLoop.get(j);
            if (s instanceof Return) {
                j++;
                break;
            }
            s = s.replace(old, var);
            for(int k = 0; k < oldArgs.size(); k++) {
                s = s.replace(oldArgs.get(k), newArgs.get(k));
            }
            s.setBounds(getScope(0));
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
            s.setBounds(getScope(0));
            loop.listContinue.add(s);
        }
        undoLastBlockCondition();
        outerLoop.list.add(loop);
        for (; i < functionDef.list.size(); i++) {
            Statement s = functionDef.list.get(i);
            outerLoop.list.add(s);
        }
        outerLoop.list.add(new Break());
        loop.autoClose(autoClose(stackPos, null));
        functionContext.rewindStack(stackPos);
        undoLastBlockCondition();
        stackPosLoop = stackPos;
        currentLoop = oldLoop;
        target.add(outerLoop);
    }

    private void addBlockCondition(Expression condition) {
        blockConditions.add(condition);
        if (condition != null) {
            condition.applyBoundCondition(condition, ApplyType.POSITIVE);
        }
    }

    private void undoLastBlockCondition() {
        Expression last = blockConditions.remove(blockConditions.size() - 1);
        if (last != null) {
            last.applyBoundCondition(last, ApplyType.UNDO);
        }
    }

    private void parseWhile(ArrayList<Statement> target) {
        int loopIndent = indent;
        While oldLoop = currentLoop;
        While loop = new While();
        currentLoop = loop;
        loop.condition = parseCondition(loop.list);
        addBlockCondition(loop.condition);
        if (!loop.list.isEmpty()) {
            // we need to make it a "while true" loop with a break condition
            Break b = new Break();
            b.condition = new Operation(null, "not", loop.condition);
            loop.list.add(b);
            loop.condition = new Operation(
                    new NumberValue(new Value.ValueInt(1), program.getType2(null, DataType.INT), false),
                    "=",
                    new NumberValue(new Value.ValueInt(1), program.getType2(null, DataType.INT), false)
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
        stackPosLoop = stackPos;
        loop.continueId = nextContinueId++;
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
        stackPosLoop = stackPos;
        undoLastBlockCondition();
        currentLoop = oldLoop;
        target.add(loop);
    }

    private List<Statement> autoClose(int stackPos, Expression except) {
        ArrayList<Statement> autoClose = new ArrayList<Statement>();
        if (functionContext.getStackCloseDifference(stackPos) == 0) {
            return autoClose;
        }
        ArrayList<String> list = functionContext.newVariablesList(stackPos);
        String exceptString = except == null ? "" : except.toString();
        for(String name : list) {
            // do not close if we return it
            if (!name.equals(exceptString)) {
                Variable var = functionContext.getVariable(null, name);
                if (var == null) {
                    throw syntaxError("Variable not found: '" + name + "'");
                }
                if (var.type.isPointer() || var.type.isArray()) {
                    Free free = new Free(var);
                    autoClose.add(free);
                }
            }
        }
        return autoClose;
    }

    private Expression parseExpression(ArrayList<Statement> target) {
        return parseExpression().writeStatements(this, false, target);
    }

    private Expression parseExpression() {
        Expression expr = parseExpression(parseExpressionPrimary(), 1);
        if (expr.type() == null) {
            // null
            return expr;
        }
        if (expr.type().isSystem() && !(expr instanceof NumberValue)) {
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
        Call call = new Call();
        call.args.add(expr);
        String m = null;
        if (isImport) {
            m = module;
        }
        // TODO support chained calls?
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
            Expression expr = new NumberValue(new Value.ValueInt(v), program.getType2(null, DataType.INT), false);
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (type == TokenType.HEX_INTEGER) {
            String n = token;
            read();
            long v = NumberValue.parseUnsignedHexLong(n.substring(2));
            Expression expr = new NumberValue(new Value.ValueInt(v), program.getType2(null, DataType.INT), true);
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (type == TokenType.FLOAT) {
            String n = token;
            read();
            double v = Double.parseDouble(n);
            Expression expr = new NumberValue(new Value.ValueFloat(v), program.getType2(null, DataType.FLOAT), false);
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (type == TokenType.STRING) {
            String n = token;
            long reference = program.addStringConstant(n);
            read();
            DataType type = program.getType2(null, DataType.I8).arrayType();
            type.used();
            Expression expr = new StringLiteral(n, type, reference);
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (type == TokenType.IDENTIFIER) {
            String n = token;
            if ("null".equals(n)) {
                read();
                return new NullValue();
            }
            Variable thisVar = functionContext.getVariable(null, "this");
            String m = null;
            if ("ord".equals(n)) {
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
            if (matchOp("(")) {
                if ("new".equals(n) || "new".equals(n)) {
                    DataType type = readType(false);
                    type.used();
                    Expression arrayLength = null;
                    if (type.isArray()) {
                        matchOp(",");
                        arrayLength = parseExpression();
                        if (arrayLength.canThrowException() != null) {
                            throw syntaxError("May not throw an exception here");
                        }
                    }
                    if (!matchOp(")")) {
                        throw syntaxError("Expected ')', got '"+token+"' in constructor");
                    }
                    New newExpr = new New(type, arrayLength);
                    type.used();
                    return newExpr;
                }
                Call call = new Call();
                Expression expr = parseCall(null, m, n, call, true);
                Value val = eval(expr, true);
                ArrayList<FunctionDefinition> toCompileFirst = new ArrayList<>(program.uncompiledFunctions.values());
                if (!toCompileFirst.isEmpty()) {
                    // could not yet evaluate, because a function was not yet compiled
                    // we compile those functions first, but only once
                    program.uncompiledFunctions.clear();
                    for (FunctionDefinition def : toCompileFirst) {
                        if (def.code != null) {
                            try {
                                Parser p = new Parser(program, def.module, def.toString());
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
                        long reference = program.addStringConstant(s);
                        DataType type = program.getType2(null, DataType.I8).arrayType();
                        type.used();
                        expr = new StringLiteral(s, type, reference);
                        return expr;
                    }
                    return new NumberValue(val, expr.type(), false);
                }
                return expr;
            }
            DataType enumType = functionContext.getType(m, n);
            if (enumType != null && enumType.enumValues != null) {
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
            }
            Expression var = functionContext.getVariable(m, n);
            if (var == null) {
                if (thisVar != null) {
                    verifyNullAccess(thisVar);
                    DataType thisType = thisVar.type();
                    if (!thisType.isSystem()) {
                        DataType type = thisType.getFieldDataType(n);
                        if (type != null) {
                            var = new FieldAccess(thisVar, n, type);
                        }
                    }
                }
                if (var == null) {
                    throw syntaxError("Variable or constant not found: '" + n + "'");
                }
            }
            return parsePossibleDot(var);
        } else if (matchOp("(")) {
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
                String f = readIdentifier();
                if (matchOp("(")) {
                    Call call = new Call();
                    call.args.add(v);
                    // TODO support chained calls?
                    return parseCall(vt, module, f, call, true);
                } else {
                    DataType type;
                    if ("len".equals(f) && vt.isArray()) {
                        type = program.getType2(null, DataType.I32);
                    } else {
                        type = vt.getFieldDataType(f);
                    }
                    if (type == null) {
                        throw syntaxError("Field '" + f + "' not found in type '" + vt + "'");
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
                if (checkBounds) {
                    program.getFunction(null, null, "idx", 2).used = true;
                }
                v = new ArrayAccess(v, arrayIndex, checkBounds);
                vt = v.type();
            } else {
                break;
            }
        }
        return v;
    }

    private void verifyNullAccess(Expression e) {
        if (e.type().isNullable()) {
            Bounds b = e.getBounds();
            if (b == null || !b.isNotNull(this)) {
                throw syntaxError("The expression '" + e + "' could be null here. You need to verify using 'if " + e + "' before accessing it.");
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
            if ("/".equals(op)) {
                program.getFunction(null, null, "idiv", 2).used = true;
            } else if ("%".equals(op)) {
                program.getFunction(null, null, "imod", 2).used = true;
            } else if ("<<".equals(op)) {
                program.getFunction(null, null, "shiftLeft", 2).used = true;
            } else if (">>".equals(op)) {
                program.getFunction(null, null, "shiftRight_" + expr.type().name(), 2).used = true;
            }
            if (Operation.isComparison(op) && (expr.isComparison() || right.isComparison())) {
                throw syntaxError("Comparing a result of a comparison requires parenthesis");
            }
            Operation operation = new Operation(expr, op, right);
            expr = operation;
        }
        return expr;
    }

    private String readIdentifier() {
        if (type != TokenType.IDENTIFIER) {
            throw syntaxError("Expected an identifier, got '"+token+"'");
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
                    lastComment = text.substring(start, pos - 2).trim();
                } else {
                    int start = pos;
                    while (true) {
                        c = text.charAt(pos);
                        if (c == '\n') {
                            pos++;
                            break;
                        }
                        pos++;
                    }
                    lastComment = text.substring(start, pos).trim();
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
                        throw syntaxError("Expected '\\n', '\\t', '\\'', '\\\\', or '\\x'; got '" + c + "'");
                    }
                } else {
                    if (c > 127) {
                        isAscii = false;
                    }
                    buff.append((char) c);
                }
                pos++;
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
        assign.isConstant = false;
        Variable var = new Variable("_t" + functionContext.nextTempVariableId(), type);
        assign.type = type;
        assign.leftValue = var;
        assign.value = expr;
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
