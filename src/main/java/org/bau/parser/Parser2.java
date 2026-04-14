package org.bau.parser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bau.parser.expr.ArrayAccess;
import org.bau.parser.expr.Borrow;
import org.bau.parser.expr.Call;
import org.bau.parser.expr.Expression;
import org.bau.parser.expr.FieldAccess;
import org.bau.parser.expr.LeftValue;
import org.bau.parser.expr.NullValue;
import org.bau.parser.expr.NumberValue;
import org.bau.parser.expr.Operation;
import org.bau.parser.expr.Parentheses;
import org.bau.parser.expr.StringLiteral;
import org.bau.parser.expr.TernaryExpression;
import org.bau.parser.expr.Variable;
import org.bau.parser.stmt.Assignment;
import org.bau.parser.stmt.Break;
import org.bau.parser.stmt.Catch;
import org.bau.parser.stmt.Continue;
import org.bau.parser.stmt.For;
import org.bau.parser.stmt.If;
import org.bau.parser.stmt.Loop;
import org.bau.parser.stmt.NativeCode;
import org.bau.parser.stmt.PhiBlock;
import org.bau.parser.stmt.Return;
import org.bau.parser.stmt.Statement;
import org.bau.parser.stmt.Throw;
import org.bau.runtime.Value;
public class Parser2 {
    enum TokenType {
        END,
        IDENTIFIER,
        FLOAT,
        INTEGER,
        HEX_INTEGER,
        STRING,
        OPERATOR;
    }
    private String lastComment;
    private int lastPos;
    private int indent;
    private DataType exceptionType;
    // solver for array bound checks etc
    private Solver solver;
    // depth of blocks (indentation depth)
    private int depth;
    // the block id is is incremented for each if / elif / else block
    private Program program;
    private boolean isGlobalScope;
    private int nextConstantId;
    private String module;
    // the stack starting position of the current loop (of the scope)
    private int stackPosLoop;
    // the stack starting position of the function
    // (after the parameters)
    private int stackPosFunction;
    private FunctionDefinition currentFunctionDefinition;
    private final int posOffset;
    private int fileId;
    String text;
    TokenType type;
    String token;
    int pos;
    public boolean isImport;
    public Parser2(Map<String, String> modules, String text) {
        this(new Program(modules), "", text, 0);
    }
    public Parser2(String text) {
        this(new Program(Map.of()), "", text, 0);
    }
    public Parser2(Program program, String module, String text, int posOffset) {
        Utils.assertTrue(module != null);
        program.addSourceFile(module, text);
        this.fileId = program.getSourceFile(module).getFileId();
        this.program = program;
        this.module = module;
        // add a newline to simplify end detection
        this.text = text + "\n";
        this.posOffset = posOffset;
        this.solver = new Solver(program.getSolver());
    }
    public Program parse() {
        readSpaces();
        Program program = parseProgram();
        return program.checkErrors();
    }
    private void syntaxError(String message, Exception e) {
        program.syntaxError(fileId, lastPos + posOffset, message);
    }
    private void syntaxError(String message) {
        syntaxError(message, lastPos);
    }
    private void syntaxError(String message, int at) {
        program.syntaxError(fileId, at + posOffset, message);
        pos = lastPos;
        while (pos < text.length() && text.charAt(pos) != '\n') {
            pos++;
        }
        read();
        throw new IllegalStateException();
    }
    private Program parseProgram() {
        boolean mainStatements = false;
        while (true) {
            try {
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
                    if (mainStatements && (module == null || module.isEmpty()) && program.getFunctionIfExists(null, "", "main", 0) == null) {
                        // there is no main yet: we thread the statements as a main function
                        pos = lastPos;
                        String mainCode = parseBlock(-1);
                        FunctionDefinition def = new FunctionDefinition(new FullName("", "main"), pos);
                        def.code = Statement.indent(mainCode);
                        program.addFunction(def);
                    } else {
                        isGlobalScope = true;
                        parseStatement(program.initList);
                    }
                }
            } catch (IllegalStateException e) {
                if (e.getMessage() == null) {
                    // exception already processed - ignore and continue
                } else {
                    throw e;
                }
            }
        }
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
        boolean alreadyImported = false;
        SourceFile f = program.getSourceFile(name);
        if (f != null && f.isImported()) {
            alreadyImported = true;
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
        program.addImport(module, name, id, entries);
        for (String e : entries) {
            Variable v = program.getGlobalVariable(name, e);
        }
        return true;
    }
    private boolean parseTraitDefinition(String targetModule) {
        if (!match("trait")) {
            return false;
        }
        int defIndent = indent;
        String comment = lastComment;
        String name = readIdentifier();
        int location = lastPos - name.length();
        boolean owned = match("owned");
        MemoryType memoryType = owned ? MemoryType.OWNER : MemoryType.REF_COUNT;
        DataType type = DataType.newTraitType(new FullName(targetModule, name), memoryType);
        type.setLocation(program, module, fileId, location);
        FullName traitName = new FullName(targetModule, name);
        type.setTraitDefinition(new Trait(traitName));
        if (matchOp(":")) {
            while (true) {
                FullName n = readIdentifierWithPossibleModule();
                type.getTraitDefinition().requiredTraitNames.add(n);
                if (!matchOp(",")) {
                    break;
                }
            }
        }
        readEndOfStatement();
        program.addComment("trait " + type.format(), comment);
        lastComment = null;
        while (indent > defIndent) {
            if (!matchOp("\n")) {
                FullName fn = new FullName(module, readIdentifier());
                FunctionDefinition def = new FunctionDefinition(fn, lastPos);
                def.callType = type;
                matchOp("(");
                Variable var = new Variable("this", type);
                var.setConstantValue(null);
                def.parameters.add(var);
                boolean template = parseFunctionDeclaration(false, targetModule, def);
                if (template) {
                    syntaxError("Template are not supported in traits");
                }
                type.getTraitDefinition().functions.add(def);
                program.addFunction(def);
            }
        }
        program.addType(type);
        return true;
    }
    /**
     * @param targetModule the module of the function
     * (eg when parsing a List<String>, the target module is the module of List, not String)
     * @return
     */
    private boolean parseTypeDefinition(String targetModule) {
        if (!match("type")) {
            return false;
        }
        int defIndent = indent;
        String comment = lastComment;
        String name = readIdentifier();
        if (name.length() < 2) {
            syntaxError("Type name '" + name + "' is too short, needs to be at least 2 characters");
        }
        int location = lastPos - name.length();
        boolean template = false;
        ArrayList<String> parameters = new ArrayList<>();
        if (matchOp("(")) {
            matchOp("\n");
            while (true) {
                String t = readIdentifier();
                parameters.add(t);
                String m = targetModule;
                if (DataType.isGenericTypeName(t)) {
                    m = "";
                }
                DataType type = DataType.newUndefined(new FullName(m, t));
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
        if (template) {
            parseTypeTemplate(defIndent, name, parameters, comment, location);
            return true;
        }
        MemoryType memoryType = name.charAt(0) > 'Z' ? MemoryType.COPY : MemoryType.REF_COUNT;
        if (owned) {
            if (memoryType == MemoryType.COPY) {
                syntaxError("Value types can not be owned");
            }
            memoryType = MemoryType.OWNER;
        }
        DataType type = DataType.newRegularType(new FullName(targetModule, name), sizeOf, memoryType);
        type.setLocation(program, module, fileId, location);
        // need to add it first, because one of the fields could be of this type
        program.addType(type);
        String title = "type " + type.format();
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
        type.traitNames.addAll(traitNames);
        return true;
    }
    private void parseTypeTemplate(int defIndent, String name, ArrayList<String> parameters, String comment, int location) {
        int lastPos = pos;
        String code = parseBlock(defIndent);
        DataType type = DataType.newUndefined(new FullName(module, name));
        type.setLocation(program, module, fileId, location);
        type.parameters = parameters;
        type.posOffset = lastPos;
        type.template = code;
        lastComment = null;
        program.addComment("type " + type.format(), comment);
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
        if (id.length() < 2) {
            syntaxError("Enum name '" + id + "' is too short, needs to be at least 2 characters");
        }
        int location = lastPos - id.length();
        readEndOfStatement();
        LinkedHashMap<String, Expression> entries = new LinkedHashMap<>();
        HashMap<Long, String> map = new HashMap<>();
        long nextValue = 0;
        while (indent > defIndent) {
            if (!matchOp("\n")) {
                String name = readIdentifier();
                Expression expr = null;
                if (matchOp(":")) {
                    expr = parseExpression();
                }
                map.put(nextValue, name);
                entries.put(name, expr);
                nextValue++;
                readEndOfStatement();
            }
        }
        DataType type = DataType.newEnumType(new FullName(module, id));
        type.setLocation(program, module, fileId, location);
        type.enumExpressions = entries;
        program.addType(type);
        program.addComment("enum " + type.format(), comment);
        lastComment = null;
        return true;
    }
    private DataType readTypeInThisModule() {
        String typeName = readIdentifier();
        if (matchOp("[")) {
            if (!matchOp("]")) {
                syntaxError("Expected ']', got '" + token + "' when reading type");
            }
        }
        if (matchOp("(")) {
            typeName += "(";
            while(true) {
                String param = readIdentifier();
                typeName += param;
                if (matchOp(")")) {
                    typeName += ")";
                    break;
                } else if (matchOp(",")) {
                    typeName += ",";
                }
            }
        }
        return DataType.UNKNOWN;
    }
    private boolean parseFunctionDefinition(String targetModule) {
        int startParse = lastPos;
        if (!match("fun")) {
            return false;
        }
        String comment = lastComment;
        int defIndent = indent;
        isGlobalScope = false;
        int open = 0;
        boolean functionOnType = false;
        String methodName = "?";
        String lastIdentifier = null;
        while(true) {
            if (matchOp("(")) {
                if (open == 0) {
                    methodName = lastIdentifier;
                }
                open++;
            } else if (matchOp(",")) {
            } else if (matchOp(")")) {
                open--;
            } else if (matchOp("[")) {
                if (!matchOp("]")) {
                    syntaxError("Expected ']', got '" + token + "' when reading type");
                }
            } else if (matchOp(".")) {
                if (open == 0) {
                    functionOnType = true;
                    // TODO in theory, it could be a module name,
                    // but this is not currently supported
                    break;
                }
            } else if (matchOp("\n") && open == 0) {
                break;
            } else if (this.type == TokenType.IDENTIFIER) {
                lastIdentifier = readIdentifier();
            } else {
                break;
            }
        }
        DataType callType = DataType.UNKNOWN;
        if (functionOnType) {
            pos = startParse;
            read();
            match("fun");
            callType = readTypeInThisModule();
            if (!matchOp(".")) {
                syntaxError("Expected '.', got '" + token + "' ");
            }
        } else {
            pos = startParse;
            read();
            match("fun");
        }
        methodName = readIdentifier();
        int location = -1;
        boolean template = false; // DataType.isGenericTypeName(token);
        String module = targetModule;
        String name;
        DataType ct = null;
        // stack position is before the "this" parameter
        // (which also may need to be incremented, if the function returns "this")
        Variable thisVar = null;
        if (matchOp("(")) {
            matchOp("\n");
            name = methodName;
        } else {
            syntaxError("Expected '(', got '" + token + "' when reading a function definition");
            name = "x";
        }
        FunctionDefinition def = new FunctionDefinition(new FullName(module, name), startParse);
        def.setLocation(program, module, fileId, location);
        def.callType = ct;
        if (thisVar != null ) {
            def.parameters.add(thisVar);
        }
        currentFunctionDefinition = def;
        template = parseFunctionDeclaration(template, targetModule, def);
        for (Variable var : def.parameters) {
            if (var.name().equals("this") && var.isConstant()) {
                // "this" is not null (if it is constant, which allows "this" to be a parameter for other functions)
            } else if (var.type().memoryType() == MemoryType.OWNER) {
                // not null, until used
            }
        }
        startBlock(false, null);
        while (true) {
            if (indent <= defIndent || type == TokenType.END) {
                break;
            }
            parseStatement(def.list);
        }
        if (def.exceptionType != null && def.returnType == null) {
            def.list.add(new Return(null));
        }
        endBlock();
        if (depth != 0) {
            throw new IllegalStateException();
        }
        solver.clear();
        currentFunctionDefinition = null;
        if (def.macro) {
            Templates.checkMacroFunction(def);
            program.addFunctionTemplate(callType, module, def.getFullName().name, def);
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
                        syntaxError("Expected ')'");
                    }
                }
                DataType type;
                if (DataType.isGenericTypeName(token) && !templateTypes.contains(token)) {
                    templateTypes.add(token);
                    template = true;
                    type = DataType.newUndefined(new FullName("", token));
                    type = readType(template);
                    if (matchOp("..")) {
                        varArgs = true;
                        type = type.arrayType();
                    }
                    Variable var = new Variable(name, type);
                    def.parameters.add(var);
                } else if (match("type")) {
                    template = true;
                    type = DataType.TYPE_TYPE;
                    templateTypes.add(name);
                    String m = targetModule;
                    if (DataType.isGenericTypeName(name)) {
                        m = "";
                    }
                    DataType t = DataType.newUndefined(new FullName(m, name));
                    // we change the variable name, because the name is already a type
                    Variable var = new Variable("_" + name, type);
                    def.parameters.add(var);
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
                            syntaxError("Owned var-args are not supported");
                        }
                    }
                    def.parameters.add(var);
                }
                if (varArgs) {
                    if (!matchOp(")")) {
                        syntaxError("Expected ')', got " + token);
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
            syntaxError("Types on 'it' parameters are only allowed in macros");
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
            }
            readEndOfStatement();
        }
        return template;
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
            syntaxError("Type '" + token + "' may not be used here");
        }
        if ("fun".equals(token)) {
            read();
            if (!matchOp("(")) {
                syntaxError("Expected '('");
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
                        syntaxError("Expected ')'");
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
                String rangeTypeName = "0.." + upperBound.format();
                DataType newType = DataType.newNumberType(rangeTypeName, 8);
                newType.maxValue = upperBound;
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
        m = program.getImportModule(module, name);
        if (m.isEmpty() && !DataType.isGenericTypeName(name)) {
            m = module;
        }
        DataType t = DataType.newUndefined(new FullName(m, name));
        if (matchOp("(")) {
            while(true) {
                String param = readIdentifier();
                if (matchOp(")")) {
                    break;
                } else if (matchOp(",")) {
                }
            }
        }
        if (arraysOk && matchOp("[")) {
            if (!matchOp("]")) {
                syntaxError("Expected ']', got '" + token + "' when reading a type");
            }
            t = t.arrayType();
        }
        if (matchOp("?")) {
            if (t.isArray()) {
                syntaxError("Arrays can't be null (but they can be empty)");
            } else if (t.isCopyType()) {
                // ignore, to support templates
                // syntaxError("Numbers and value types can't be be null (but the value can be zero)");
            } else {
                t = t.orNull();
            }
        }
        return t;
    }
    private ArrayList<DataType> parseTypeParameters(DataType t, boolean templatesOk) {
        String name = t.name();
        if (!matchOp("(")) {
            syntaxError("Type '" + name + "' is a template; need to specify the parameters");
        }
        matchOp("\n");
        ArrayList<DataType> params = new ArrayList<>();
        for (int i = 0; i < t.parameters.size(); i++) {
            DataType t2 = readType(templatesOk);
            params.add(t2);
            matchOp(",");
        }
        if (!matchOp(")")) {
            syntaxError("Type '" + name + "' is a template; need to specify " + t.parameters.size() + " parameters");
        }
        return params;
    }
    private void parseStatement(ArrayList<Statement> target) {
        try {
            tryParseStatement(target);
        } catch (IllegalStateException e) {
            if (e.getMessage() == null) {
                // syntax error - already added to source file
                // ignore
            } else {
                throw e;
            }
        }
    }
    private void tryParseStatement(ArrayList<Statement> target) {
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
                    if ("main".equals(currentFunctionDefinition.getFullName().name)) {
                        syntaxError("Trying to define a function inside the main function. Note that any statement starts the main function implicitly.");
                    }
                    syntaxError("Trying to define a function inside a function");
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
                    syntaxError("Can not create a new variable in a different module");
                }
                Expression expr = parseExpression();
                if (expr instanceof NullValue) {
                    if (targetType == null) {
                        syntaxError("Can only assign null if the type is known");
                    }
                    expr = new NullValue(targetType);
                }
                DataType type = DataType.UNKNOWN; // expr.type();
                if (type.isArray()) {
                    syntaxError("Arrays need to be declared as constants to simplify array-bound verification");
                }
                boolean global = isGlobalScope;
                if (matchOp("..")) {
                    if (global) {
                        syntaxError("Global ranges are not allowed; they need to be in a function");
                    }
                    if (!"0".equals(expr.format())) {
                        syntaxError("Range needs to start from 0: '" + expr.format() + "'");
                    }
                    Expression upperBound = parseExpression();
                    if (upperBound.canThrowException() != null) {
                        syntaxError("May not throw an exception here");
                    }
                    String rangeTypeName = "0.." + upperBound.format();
                }
                for (String identifier : identifierList) {
                    Assignment s = new Assignment();
                    s.initial = true;
                    s.isGlobalScope = global;
                    // no need for temp variables as it's just an assignment
                    s.value = expr;
                    Variable v = new Variable(module, identifier, global, s.type);
                    s.leftValue = v;
                    target.add(s);
                }
                readEndOfStatement();
                return;
            }
            if (matchOp(":")) {
                if (m != module && !m.equals(module)) {
                    syntaxError("Can not define a constant in a different module");
                }
                Expression expr = parseExpression();
                if (expr instanceof NullValue) {
                    if (targetType == null) {
                        syntaxError("Can only assign null if the type is known");
                    }
                    expr = new NullValue(targetType);
                }
                if (identifierList.size() != 1) {
                    syntaxError("Constant lists are not supported");
                }
                String identifier = identifierList.get(0);
                Assignment s = new Assignment();
                s.isConstant = true;
                s.isGlobalScope = isGlobalScope;
                s.initial = true;
                s.value = expr;
                boolean global = isGlobalScope;
                Variable v = new Variable(module, identifier, global, DataType.UNKNOWN);
                s.leftValue = v;
                s.type = DataType.UNKNOWN; // s.value.type();
                if (global && !identifier.toUpperCase(Locale.ROOT).equals(identifier)) {
                }
                if (global) {
                    program.addGlobalVariable(v);
                }
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("(")) {
                matchOp("\n");
                if (identifierList.size() != 1) {
                    syntaxError("Function lists are not supported");
                }
                String identifier = identifierList.get(0);
                if ("native".equals(identifier)) {
                    String s = token.trim();
                    read();
                    if (!matchOp(")")) {
                        syntaxError("Expected ')'");
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
                if (m == null || m.isEmpty()) {
                    m = program.getImportModule(module, identifier);
                }
                Call call = new Call();
                call.statement = true;
                Expression expr;
                DataType t = null;
                while (true) {
                    expr = parseCall(t, m, identifier, call, true);
                    if (expr == null) {
                        // eg. ternary expression
                        break;
                    }
                    t = DataType.UNKNOWN; //  expr.type();
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
                        syntaxError("Only method calls are supported here");
                    }
                }
                readEndOfStatement();
                if (expr instanceof Call) {
                    target.add((Call) expr);
                }
                return;
            } else if (matchOp("\n") && targetType != null) {
                if (identifierList.size() != 1) {
                    syntaxError("Declaration lists are not supported");
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
                        syntaxError("The type of the variable is different than the type of the expression");
                    }
                }
                s.value = expr;
                boolean global = isGlobalScope;
                Variable v = new Variable(module, identifier, global, targetType);
                s.leftValue = v;
                if (global) {
                    program.addGlobalVariable(v);
                }
                // already read
                // readEndOfStatement();
                target.add(s);
                return;
            }
            if (identifierList.size() != 1) {
                syntaxError("Lists are currently not supported");
            }
            String identifier = identifierList.get(0);
            LeftValue left = new Variable(identifier, DataType.UNKNOWN);
            while (true) {
                if (matchOp(".")) {
                    // TODO duplicate code, in parsePossibleDot
                    String f;
                    if (left instanceof Variable && type == TokenType.INTEGER) {
                        int index = Integer.parseInt(token);
                        read();
                        f = "" + index;
                    } else {
                        f = readIdentifier();
                    }
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
                                return;
                            }
                        } else {
                            // eg calling a macro that doesn't return anything
                            readEndOfStatement();
                            return;
                        }
                    } else {
                        DataType type;
                        if ("len".equals(f) && left.type().isArray()) {
                            type = DataType.I32_TYPE;
                        } else {
                            type = left.type().getFieldDataType(f);
                        }
                        if (type == null) {
                            type = DataType.UNKNOWN;
                        }
                        left = new FieldAccess(left, f, false, type);
                    }
                } else if (matchOp("[")) {
                    Expression arrayIndex = parseExpression();
                    boolean checkBounds = true; //  = needBoundsCheck(left, arrayIndex);
                    if (matchOp("]!")) {
                        checkBounds = false;
                    } else if (!matchOp("]")) {
                        checkBounds = true;
                    }
                    left = new ArrayAccess(left, arrayIndex, checkBounds);
                } else {
                    break;
                }
            }
            Assignment s = new Assignment();
            s.leftValue = left;
            if (left.isConstant()) {
                syntaxError("Can not modify constant '" + left.format() + "'");
            }
            if (matchOp("=")) {
                Expression expr = parseExpression();
                if (targetType != null && !targetType.equals(expr.type())) {
                    expr = program.cast(expr, false, targetType);
                    if (expr == null) {
                        syntaxError("The type of the variable is different than the type of the expression");
                    }
                }
                s.value = expr;
                // this possibly updates the reference,
                // and so it's important we have a temp variable
                if (s.leftValue instanceof Variable && s.type != null && s.type.isArray()) {
                    syntaxError("Arrays can not be re-assigned to simplify array-bound verification");
                }
                if (s.value instanceof NullValue) {
                    // for templates, we want to support assignment to null
                }
                readEndOfStatement();
                target.add(s);
                return;
            }
            if (matchOp("*=")) {
                s.modify = "*";
                Expression expr = parseExpression();
                s.value = expr;
                if (targetType != null && !targetType.equals(s.value.type())) {
                    syntaxError("The type of the variable is different than the type of the expression");
                }
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("/=")) {
                s.modify = "/";
                Expression expr = parseExpression();
                s.value = expr;
                if (targetType != null) {
                    if (!targetType.equals(s.value.type())) {
                        syntaxError("The type of the variable is different than the type of the expression");
                    }
                }
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("%=")) {
                s.modify = "%";
                Expression expr = parseExpression();
                s.value = expr;
                if (targetType != null) {
                    if (!targetType.equals(s.value.type())) {
                        syntaxError("The type of the variable is different than the type of the expression");
                    }
                }
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("+=")) {
                s.modify = "+";
                Expression expr = parseExpression();
                s.value = expr;
                if (targetType != null && !targetType.equals(s.value.type())) {
                    syntaxError("The type of the variable is different than the type of the expression");
                }
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("-=")) {
                s.modify = "-";
                Expression expr = parseExpression();
                s.value = expr;
                if (targetType != null && !targetType.equals(s.value.type())) {
                    syntaxError("The type of the variable is different than the type of the expression");
                }
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("&=")) {
                s.modify = "&";
                Expression expr = parseExpression();
                s.value = expr;
                if (targetType != null && !targetType.equals(s.value.type())) {
                    syntaxError("The type of the variable is different than the type of the expression");
                }
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("|=")) {
                s.modify = "|";
                Expression expr = parseExpression();
                s.value = expr;
                if (targetType != null && !targetType.equals(s.value.type())) {
                    syntaxError("The type of the variable is different than the type of the expression");
                }
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("^=")) {
                s.modify = "^";
                Expression expr = parseExpression();
                s.value = expr;
                if (targetType != null && !targetType.equals(s.value.type())) {
                    syntaxError("The type of the variable is different than the type of the expression");
                }
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp(">>=")) {
                s.modify = ">>";
                Expression expr = parseExpression();
                s.value = expr;
                if (targetType != null && !targetType.equals(s.value.type())) {
                    syntaxError("The type of the variable is different than the type of the expression");
                }
                readEndOfStatement();
                target.add(s);
                return;
            } else if (matchOp("<<=")) {
                s.modify = "<<";
                Expression expr = parseExpression();
                s.value = expr;
                if (targetType != null && !targetType.equals(s.value.type())) {
                    syntaxError("The type of the variable is different than the type of the expression");
                }
                readEndOfStatement();
                target.add(s);
                return;
            }
        }
        syntaxError("Expected a statement, got '" + token + "'");
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
        FieldAccess f = new FieldAccess(base, "len", false, DataType.INT_TYPE);
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
    private void readEndOfStatement() {
        lastComment = null;
        if (token != null && !matchOp(";") && !matchOp("\n")) {
            syntaxError("Expected end of statement, got '" + token + "'");
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
            setThis.initial = true;
            setThis.isConstant = true;
            setThis.value = call.args.get(0);
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
        int callStart = lastPos - identifier.length();
        int posOffset = lastPos;
        if (type != null && (type.module() != null && !type.module().isEmpty())) {
            module = type.module();
        }
        FunctionDefinition template = program.getFunctionTemplate(type, module, identifier);
        if (template == null) {
            template = program.getFunctionTemplate(type, "", identifier);
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
        boolean lastWasComma = false;
        int pi = call.args.size(); // including 'this'
        int paramIndex = 0; // excluding 'this'
        if (template != null && template.macro) {
            DataType itType = DataType.INT_TYPE;
            if (template.itType != null) {
                itType = template.itType;
            }
            Variable it = new Variable("it", itType);
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
                    syntaxError("Expected ',' after '" + last.format() + "' or parentheses around the expression, to make it easier to read");
                }
            }
            if (template != null && pi < template.parameters.size() && DataType.TYPE.equals(template.parameters.get(pi).type().name())) {
                if (DataType.TYPE.equals(token)) {
                    syntaxError("Type '" + token + "' may not be used here");
                }
                DataType t = readType(false, true);
                String pName = template.parameters.get(pi).name();
                if (pName.startsWith("_")) {
                    pName = pName.substring(1);
                }
                templateNames.add(pName);
                templateParams.add(t.getFullName().getFullName());
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
                            templateParams.add(pt.getFullName().getFullName());
                            if (t.isArray()) {
                                // also replace the non-array version
                                t = t.baseType();
                                typeName = t.name();
                                templateNames.add(typeName);
                                pt = p.type();
                                if (!pt.isArray()) {
                                    syntaxError("Expected array, got " + pt.format());
                                }
                                pt = pt.baseType();
                                templateParams.add(pt.getFullName().getFullName());
                            }
                        }
                    }
                }
                if (checkComma) {
                    if (!p.isEasyToRead()) {
                        syntaxError("Expected ',' before '" + p.format() + "' or parentheses around the expression, to make it easier to read");
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
        } else {
        }
        // verify parameters with range restrictions
        // cast
        // owned variables are now null
        return call;
    }
    private void parseReturn(ArrayList<Statement> target) {
        if (currentFunctionDefinition == null) {
            syntaxError("Return needs to be inside of a function");
        }
        Return b = new Return(null);
        if (matchOp("\n") || matchOp(";")) {
            if (currentFunctionDefinition.returnType != null) {
                syntaxError("The function does not return an expression of type " + currentFunctionDefinition.returnType.format());
            }
            target.add(b);
            negateLastBlockCondition();
            return;
        }
        // if it is not a simple value  (e.g. string1 + string2)
        // assign it first to a variable so that the components can be freed up
        b.expr = parseExpression(target);
        if (currentFunctionDefinition.returnType == null) {
            syntaxError("The function declared to not return a value");
        }
        negateLastBlockCondition();
        if (matchOp("\n") || matchOp(";")) {
            target.add(b);
            return;
        }
        syntaxError("Expected end of statement, got '" + token + "' in 'return' statement");
    }
    private void parseCatch(ArrayList<Statement> target) {
        int catchIndent = indent;
        Catch catchStat = new Catch();
        String id = readIdentifier();
        Variable var = new Variable(id, DataType.UNKNOWN);
        catchStat.var = var;
        boolean sameLine;
        if (matchOp("\n")) {
            sameLine = false;
        } else if (matchOp("{")) {
            sameLine = true;
        } else {
            syntaxError("Expected end of statement, got '" + token + "' in 'catch' statement");
            sameLine = false;
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
            parseStatement(catchStat.list);
        }
        target.add(catchStat);
    }
    private void parseThrow(ArrayList<Statement> target) {
        if (currentFunctionDefinition.exceptionType == null) {
            syntaxError("This method does not throw an exception (local exceptions are not supported)");
        }
        Throw t = new Throw();
        if (matchOp("\n") || matchOp(";")) {
            target.add(t);
            negateLastBlockCondition();
            return;
        }
        t.expr = parseExpression(target);
        // exceptionType = t.expr.type();
        // theoretically, the exception could be caught in this method
        // so we can not verify that the exception type matches the one
        // declared in the function
        if (matchOp("\n") || matchOp(";")) {
            target.add(t);
            negateLastBlockCondition();
            return;
        }
        syntaxError("Expected end of statement, got '" + token + "' in 'throw' statement");
    }
    private void parseBreak(ArrayList<Statement> target) {
        Break b = new Break();
        if (matchOp("\n") || matchOp(";")) {
            target.add(b);
            negateLastBlockCondition();
            return;
        }
        b.condition = parseCondition(target);
        if (matchOp("\n") || matchOp(";")) {
            target.add(b);
            return;
        }
        syntaxError("Expected end of statement, got '" + token + "' in 'break' statement");
    }
    private void parseContinue(ArrayList<Statement> target) {
        Continue c = new Continue();
        if (matchOp("\n") || matchOp(";")) {
            target.add(c);
            negateLastBlockCondition();
            return;
        }
        c.condition = parseCondition(target);
        if (matchOp("\n") || matchOp(";")) {
            target.add(c);
            return;
        }
        syntaxError("Expected end of statement, got '" + token + "' in 'continue' statement");
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
        return expr;
    }
    private void parseSwitch(ArrayList<Statement> target) {
        int switchIndent = indent;
        If ifStatement = new If();
        If topIfStatement = ifStatement;
        Expression switchExpr = parseExpression();
        boolean elsePart = false;
        boolean first = true;
        if (!matchOp("\n")) {
            syntaxError("Expected end of statement, got '" + token + "' in 'switch' statement");
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
                    syntaxError("Expected end of statement, got '" + token + "' in 'switch' statement");
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
                    syntaxError("Expected end of statement, got '" + token + "' in 'switch' statement");
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
                parseStatement(list);
            }
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
        while (true) {
            if (matchOp("\n")) {
                sameLine = false;
            } else if (matchOp("{")) {
                sameLine = true;
            } else {
                syntaxError("Expected end of statement, got '" + token + "' in 'if' statement");
                sameLine = false;
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
                parseStatement(list);
            }
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
            syntaxError("For loops in macros are currently not supported");
        }
        For forStatement = new For();
        int loopIndent = indent;
        String variableName = readIdentifier();
        forStatement.variable = new Variable(variableName, DataType.UNKNOWN);
        if (!matchOp(":=")) {
            syntaxError("Expected ':=', got '" + token + "' in 'for' statement");
        }
        Call call = new Call();
        Expression expr;
            expr = parseExpressionPrimary();
        forStatement.generator = expr;
        boolean sameLine;
        if (matchOp("\n")) {
            sameLine = false;
        } else if (matchOp("{")) {
            sameLine = true;
        } else {
            syntaxError("Expected end of statement, got '"+token+"' in 'for' statement");
            sameLine = false;
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
            parseStatement(forStatement.list);
        }
        endBlock();
        endBlock();
    }
    private void startBlock(boolean loop, Expression condition) {
        depth++;
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
        Loop loop = new Loop();
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
                    NumberValue.valueOf(1),
                    "==",
                    NumberValue.valueOf(1)
                    );
        }
        boolean sameLine;
        if (matchOp("\n")) {
            sameLine = false;
        } else if (matchOp("{")) {
            sameLine = true;
        } else {
            syntaxError("Expected end of statement, got '"+token+"' in 'while' statement");
            sameLine = false;
        }
        int oldStackPosLoop = stackPosLoop;
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
            parseStatement(loop.list);
        }
        stackPosLoop = oldStackPosLoop;
        endBlock();
        // before the loop (we jump here from the end of the loop)
        target.add(new PhiBlock());
        target.add(loop);
        // loop condition doesn't match, and break statements
        target.add(new PhiBlock());
    }
    private Expression parseExpression(ArrayList<Statement> target) {
        try {
            return parseExpression(); // .writeStatements(this, false, target);
        } catch (IllegalStateException e) {
            syntaxError(e.getMessage());
            return NumberValue.ZERO;
        }
    }
    private Expression parseExpression() {
        Expression expr = parseExpression(parseExpressionPrimary(), 1);
            // null
        return expr;
    }
    private Expression parseFunctionOnLiteral(Expression expr) {
        String f = readIdentifier();
        matchOp("(");
        matchOp("\n");
        Call call = new Call();
        call.args.add(expr);
        String m = "";
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
            Expression expr = NumberValue.valueOf(v);
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (type == TokenType.HEX_INTEGER) {
            String n = token;
            read();
            long v = NumberValue.parseUnsignedHexLong(n.substring(2));
            Expression expr = new NumberValue(new Value.ValueInt(v), DataType.INT_TYPE, true);
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (type == TokenType.FLOAT) {
            String n = token;
            read();
            double v = Double.parseDouble(n);
            Expression expr = new NumberValue(new Value.ValueFloat(v), DataType.FLOAT_TYPE, false);
            if (matchOp(".")) {
                expr = parseFunctionOnLiteral(expr);
            }
            return expr;
        } else if (type == TokenType.STRING) {
            String n = token;
            read();
            DataType type = DataType.I8_TYPE.arrayType();
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
            String m = null;
            read();
            m = program.getImportModule(module, n);
            if (m == null || m.isEmpty()) {
                m = module;
                // if there is no variable with this name, and
                // no function with this name, and
                // no this.field with this name,
                // then it could be a fully qualified module name
            }
            if (matchOp("(")) {
                matchOp("\n");
                Call call = new Call();
                Expression expr = parseCall(null, m, n, call, true);
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
                    }
                    // replace the definition
                    call.def = program.functions.get(call.def.getFunctionId());
                    // try again (but only once)
                }
                return parsePossibleDot(expr);
            }
                    Variable var = new Variable(n, DataType.UNKNOWN);
            return parsePossibleDot(var);
        } else if (matchOp("(")) {
            matchOp("\n");
            Expression expr = parseExpression();
            if (!matchOp(")")) {
                syntaxError("Expected ')', got '" + token + "' in nested expression");
            }
            return parsePossibleDot(new Parentheses(expr));
        } else {
            syntaxError("Expected an expression, got '" + token + "'");
            return NumberValue.ZERO;
        }
    }
    private Expression parsePossibleDot(Expression v) {
        DataType vt = null; // v.type();
        while (true) {
            if (matchOp(".")) {
                matchOp("\n");
                String f;
                if (v instanceof Variable && type == TokenType.INTEGER) {
                    int index = Integer.parseInt(token);
                    read();
                    f = "" + index;
                } else {
                    f = readIdentifier();
                }
                if (matchOp("(")) {
                    matchOp("\n");
                    Call call = new Call();
                    call.args.add(v);
                    v = parseCall(vt, module, f, call, true);
                } else {
                    DataType type = DataType.UNKNOWN;
                    v = new FieldAccess(v, f, false, type);
                    vt = v.type();
                }
            } else if (matchOp("[")) {
                Expression arrayIndex = parseExpression();
                if (matchOp("]")) {
                    v = new ArrayAccess(v, arrayIndex, true);
                } else if (matchOp("]!")) {
                    v = new ArrayAccess(v, arrayIndex, false);
                } else {
                    syntaxError("Expected ']', got '" + token + "' in array access");
                }
            } else {
                break;
            }
        }
        return v;
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
                syntaxError("Comparing a result of a comparison requires parenthesis");
            }
            Operation operation = new Operation(expr, op, right);
            if ("/".equals(op) || "%".equals(op)) {
            }
            expr = operation;
        }
        return expr;
    }
    private FullName readIdentifierWithPossibleModule() {
        String name = readIdentifier();
        String module = "";
        while (matchOp(".")) {
            if (module.isEmpty()) {
                module = name;
            } else {
                module = module + "." + name;
            }
            name = readIdentifier();
        }
        String m = module;
        String m2 = program.getImport(module, name);
        if (m2 != null && !m2.isEmpty()) {
            m = m2;
        }
        return new FullName(m, name);
    }
    private String readIdentifier() {
        if (type != TokenType.IDENTIFIER) {
            syntaxError("Expected an identifier, got '" + token + "'");
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
                    while (pos < text.length() && text.charAt(pos) == '#') {
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
                            syntaxError("Expected '\\x00'");
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
                        syntaxError("Expected '\\n', '\\r', '\\t', '\\'', '\\\\', or '\\x'; got '" + c + "'");
                    }
                } else {
                    if (c > 127) {
                        isAscii = false;
                    }
                    buff.append((char) c);
                }
                pos++;
                if (pos >= text.length()) {
                    syntaxError("Unclosed string");
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
                    syntaxError("String literal is not normalized UTF-8");
                }
                for (int i = 0; i < buff.length(); i++) {
                    if (u8b[i] != utf8[i]) {
                        // the language doesn't need string literals to be valid UTF-8 sequences -
                        // this is a limitation of the current parser
                        syntaxError("String literal is not normalized UTF-8");
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
                while (pos < text.length() && text.charAt(pos) == '`') {
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
            syntaxError("Tab characters are not supported sorry");
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
}
