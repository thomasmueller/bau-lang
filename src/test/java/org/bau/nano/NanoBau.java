package org.bau.nano;

import java.util.ArrayList;
import java.util.HashMap;

public class NanoBau {
    // compile time
    private final HashMap<String, Variable> variables = new HashMap<>();
    private final HashMap<String, Integer> functionPos = new HashMap<>();
    private final ArrayList<Long> memoryInit = new ArrayList<>();
    private String text;
    private String token;
    private int indent;
    private int pos;
    private int memoryPos;
    private int lastPos;
    private Statement currentLoopStatement;
    private ArrayList<Statement> currentLoopBreakStatements = new ArrayList<>();

    // runtime
    private long[] memory;
    private ArrayList<Statement> statements = new ArrayList<>();
    private StringBuilder output;

    public void parse(String text) {
        this.text = text;
        read();
        Statement gotoInit = new Statement();
        gotoInit.type = StatementType.GOTO;
        statements.add(gotoInit);
        Statement gotoMain = new Statement();
        gotoMain.type = StatementType.GOTO;
        while (token != null) {
            while (match("\n"));
            if (match("fun")) {
                String name = token;
                int pos = statements.size();
                functionPos.put(name, pos);
                if (name.equals("main")) {
                    gotoMain.position = pos;
                }
                read();
                expect("(");
                expect(")");
                expect("\n");
                parseBlock(0);
                Statement ret = new Statement();
                ret.type = StatementType.RETURN;
                statements.add(ret);
            } else {
                String name = token;
                read();
                if (match(":=")) {
                    Expression expr = parseExpressionPrimary();
                    Variable variable = new Variable();
                    variable.pos = memoryPos++;
                    memoryInit.add((long) variable.pos);
                    memoryInit.add(expr.calculate(memory));
                    variables.put(name, variable);
                } else if (match(":")) {
                    if (match("array")) {
                        expect("(");
                        Expression expr = parseExpressionPrimary();
                        Variable variable = new Variable();
                        variable.pos = memoryPos++;
                        variable.array = true;
                        int len = (int) expr.calculate(memory);
                        memoryInit.add((long) variable.pos);
                        memoryInit.add((long) len);
                        memoryPos += len;
                        variables.put(name, variable);
                        expect(")");
                    } else {
                        Expression expr = parseExpressionPrimary();
                        Variable variable = new Variable();
                        variable.pos = memoryPos++;
                        long value = expr.calculate(memory);
                        memoryInit.add((long) variable.pos);
                        memoryInit.add(value);
                        variables.put(name, variable);
                    }
                }
                expect("\n");
            }
        }
        gotoInit.position = statements.size();
        for (int i = 0; i < memoryInit.size(); i += 2) {
            Statement s = new Statement();
            s.type = StatementType.ASSIGN;
            s.position = (int) (long) memoryInit.get(i);
            s.expr = new Expression();
            s.expr.type = ExpressionType.LITERAL_INT;
            s.expr.value = memoryInit.get(i + 1);
            statements.add(s);
        }
        statements.add(gotoMain);
    }

    private void parseBlock(int oldIndent) {
        while (true) {
            if (indent <= oldIndent) {
                break;
            }
            parseStatement();
        }
        for (Statement s : statements) {
            if (s.type == StatementType.CALL) {
                s.position = functionPos.get(s.name);
            }
        }
    }

    private void expect(String s) {
        if (!match(s)) {
            if (s.equals("\n")) {
                s = "\\n";
            }
            throw syntaxError("Expected " + s + " got " + token);
        }
    }

    private Statement parseStatement() {
        int oldIndent = indent;
        if (match("if")) {
            Statement statement = new Statement();
            statement.type = StatementType.IF;
            statements.add(statement);
            Statement topIf = statement;
            statement.expr = parseExpression();
            expect("\n");
            parseBlock(oldIndent);
            ArrayList<Statement> endList = new ArrayList<>();
            Statement gotoStatement = new Statement();
            gotoStatement.type = StatementType.GOTO;
            statements.add(gotoStatement);
            statement.position = statements.size();
            endList.add(gotoStatement);
            oldIndent = indent;
            while (match("elif")) {
                Statement statement2 = new Statement();
                statement2.type = StatementType.IF;
                statements.add(statement2);
                statement2.expr = parseExpression();
                expect("\n");
                parseBlock(oldIndent);
                gotoStatement = new Statement();
                gotoStatement.type = StatementType.GOTO;
                statements.add(gotoStatement);
                statement2.position = statements.size();
                endList.add(gotoStatement);
                statement = statement2;
            }
            if (match("else")) {
                expect("\n");
                parseBlock(oldIndent);
            }
            for(Statement g : endList) {
                g.position = statements.size();
            }
            return topIf;
        } else if (match("loop")) {
            expect("\n");
            int loopStart = statements.size();
            Statement statement = new Statement();
            Statement oldLoop = currentLoopStatement;
            ArrayList<Statement> oldBreaks = currentLoopBreakStatements;
            ArrayList<Statement> breaks = new ArrayList<>();
            currentLoopBreakStatements = breaks;
            currentLoopStatement = statement;
            parseBlock(oldIndent);
            currentLoopBreakStatements = oldBreaks;
            currentLoopStatement = oldLoop;
            Statement go = new Statement();
            go.type = StatementType.GOTO;
            statements.add(go);
            go.position = loopStart;
            int loopEnd = statements.size();
            for (Statement g : breaks) {
                g.position = loopEnd;
            }
            return statement;
        } else if (match("break")) {
            Statement statement = new Statement();
            statement.type = StatementType.BREAK;
            statements.add(statement);
            statement.expr = parseExpression();
            if (currentLoopStatement == null) {
                throw syntaxError("Break without continue");
            }
            currentLoopBreakStatements.add(statement);
            expect("\n");
            return statement;
        } else if (match("print")) {
            expect("(");
            Statement statement = new Statement();
            statements.add(statement);
            statement.expr = parseExpression();
            statement.type = StatementType.PRINT;
            expect(")");
            expect("\n");
            return statement;
        } else if (match("printText")) {
            expect("(");
            Statement statement = new Statement();
            statements.add(statement);
            statement.expr = parseExpression();
            statement.type = StatementType.PRINT_TEXT;
            expect(")");
            expect("\n");
            return statement;
        } else if (match("printEnd")) {
            expect("(");
            Statement statement = new Statement();
            statements.add(statement);
            statement.type = StatementType.PRINT_END;
            expect(")");
            expect("\n");
            return statement;
        } else {
            String identifier = token;
            read();
            if (match("=")) {
                Statement s = new Statement();
                s.type = StatementType.ASSIGN;
                s.name = identifier;
                statements.add(s);
                Variable variable = variables.get(identifier);
                s.position = variable.pos;
                if (variable.array) {
                    throw syntaxError("Can not re-assign an array");
                }
                s.expr = parseExpression();
                expect("\n");
                return s;
            } else if (match("(")) {
                expect(")");
                Statement statement = new Statement();
                statement.type = StatementType.CALL;
                statements.add(statement);
                statement.name = identifier;
                expect("\n");
                return statement;
            } else if (match("[")) {
                Statement s = new Statement();
                s.type = StatementType.ASSIGN_ARRAY;
                s.name = identifier;
                statements.add(s);
                Variable variable = variables.get(identifier);
                if (!variable.array) {
                    throw syntaxError("Not an array");
                }
                s.index = parseExpression();
                expect("]");
                expect("=");
                s.expr = parseExpression();
                expect("\n");
                return s;
            }
        }
        throw syntaxError("Expected a statement, got '" + token + "'");
    }

    private Expression parseExpressionPrimary() {
        Expression expr = new Expression();
        if (token.charAt(0) >= '0' && token.charAt(0) <= '9') {
            expr.type = ExpressionType.LITERAL_INT;
            expr.value = Long.parseLong(token);
            read();
            return expr;
        } else if (token.startsWith("'")) {
            expr.type = ExpressionType.LITERAL_INT;
            token = token.substring(1);
            expr.value = memoryPos;
            int len = token.length();
            memoryInit.add((long) memoryPos++);
            memoryInit.add((long) len);
            for (int i = 0; i < len; i++) {
                memoryInit.add((long) memoryPos++);
                memoryInit.add((long) token.charAt(i));
            }
            read();
            return expr;
        } else {
            String identifier = token;
            read();
            Variable variable = variables.get(identifier);
            if (match("[")) {
                expr.type = ExpressionType.ARRAY_LOOKUP;
                expr.value = variable.pos;
                if (!variable.array) {
                    throw syntaxError("Not an array");
                }
                expr.variableName = identifier;
                expr.left = parseExpressionPrimary();
                expect("]");
                return expr;
            } else {
                expr.type = ExpressionType.VARIABLE;
                expr.value = variable.pos;
                if (variable.array) {
                    throw syntaxError("Can not assign an array");
                }
                expr.variableName = identifier;
                return expr;
            }
        }
    }

    private Expression parseExpression() {
        return parseExpression(parseExpressionPrimary(), 1);
    }

    private Expression parseExpression(Expression expr, int minPrecedence) {
        while (true) {
            String op = token;
            int prec = getPrecedence(op);
            if (prec < minPrecedence) {
                break;
            }
            read();
            Expression right = parseExpressionPrimary();
            while (true) {
                String op2 = token;
                int prec2 = getPrecedence(op2);
                if (prec2 <= prec) {
                    break;
                }
                right = parseExpression(right, prec + (prec2 > prec ? 1 : 0));
            }
            Expression operation = new Expression();
            operation.type = ExpressionType.OPERATION;
            operation.left = expr;
            operation.op = op;
            operation.right = right;
            expr = operation;
        }
        return expr;
    }

    private int getPrecedence(String token) {
        if (token == null) {
            return 0;
        }
        switch (token) {
        case "*":
        case "/":
        case "%":
            return 70;
        case "+":
        case "-":
            return 60;
        case "<<":
        case ">>":
            return 50;
        case "=":
        case "<>":
        case "<=":
        case ">=":
        case "<":
        case ">":
            return 40;
        case "^":
        case "&":
        case "|":
            // TODO this is weird, and should result in an error
            return 30;
        case "and":
            return 20;
        case "or":
            return 10;
        }
        return 0;
    }

    private IllegalStateException syntaxError(String message) {
        int lineStart = lastPos;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        int lineEnd = text.indexOf('\n', lineStart);
        if (lineEnd < 0) {
            lineEnd = text.length();
        }
        message += "\n" + text.substring(lineStart, lineEnd) + "\n";
        message += " ".repeat(lastPos - lineStart);
        message += "^".repeat(pos - lastPos);
        IllegalStateException ex = new IllegalStateException(message);
        return ex;
    }

    private boolean match(String s) {
        if (s.equals(token)) {
            if ("\n".equals(token)) {
                readSpaces();
            } else {
                read();
            }
            return true;
        }
        return false;
    }

    private void readSpaces() {
        token = null;
        lastPos = pos;
        indent = 0;
        while (true) {
            if (pos >= text.length()) {
                return;
            } else if (text.charAt(pos) == '\n') {
                indent = 0;
            } else if (text.charAt(pos) == ' ') {
                indent++;
            } else {
                break;
            }
            pos++;
        }
        read();
    }

    private void read() {
        token = null;
        lastPos = pos;
        while (true) {
            if (pos >= text.length()) {
                token = null;
                return;
            }
            char c = text.charAt(pos);
            if (c == ' ') {
                pos++;
            } else if (c == '#') {
                // comment
                pos++;
                while (true) {
                    c = text.charAt(pos);
                    if (c == '\n') {
                        pos++;
                        break;
                    }
                    pos++;
                }
            } else {
                break;
            }
        }
        int start = pos;
        char c = text.charAt(pos);
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            pos++;
            while (pos < text.length()) {
                c = text.charAt(pos);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                    pos++;
                } else {
                    break;
                }
            }
            token = text.substring(start, pos);
        } else if (c >= '0' && c <= '9') {
            pos++;
            while (pos < text.length()) {
                c = text.charAt(pos);
                if (c >= '0' && c <= '9') {
                    pos++;
                } else {
                    break;
                }
            }
            token = text.substring(start, pos);
        } else if (c == '\'') {
            pos++;
            while (pos < text.length() && text.charAt(pos) != '\'') {
                pos++;
            }
            token = text.substring(start, pos);
            pos++;
        } else if (c > ' ') {
            pos++;
            c = text.charAt(pos);
            if (c == '=') {
                pos++;
            }
            token = text.substring(start, pos);
        } else {
            pos++;
            token = text.substring(start, pos);
        }
    }

    static enum StatementType {
        ASSIGN,
        ASSIGN_ARRAY,
        IF,
        LOOP,
        GOTO,
        BREAK,
        CALL,
        PRINT,
        PRINT_TEXT,
        PRINT_END,
        RETURN;
    }

    static class Statement {
        StatementType type;
        Expression expr;
        Expression index;
        int position;

        // method name (during compilation)
        String name;

        public String toString() {
            switch (type) {
            case ASSIGN:
                return name + " = " + expr;
            case ASSIGN_ARRAY:
                return name + "[" + index + "]" + " = " + expr;
            case IF:
                return "if " + expr + " else " + position;
            case GOTO:
                return "goto " + position;
            case BREAK:
                return "break " + expr;
            case CALL:
                return name + "()";
            case PRINT:
                return "print(" + expr + ")";
            case PRINT_TEXT:
                return "printText(" + expr + ")";
            case PRINT_END:
                return "printEnd()";
            case RETURN:
                return "return";
            default:
                throw new IllegalStateException();
            }
        }
    }

    static enum ExpressionType {
        LITERAL_INT,
        VARIABLE,
        ARRAY_LOOKUP,
        OPERATION
    }

    static class Expression {
        // compile time
        String variableName;

        ExpressionType type;
        // operation
        String op;
        // literal_int, literal_array
        long value;
        // operation (index is left for array)
        Expression left, right;

        long calculate(long[] memory) {
            switch (type) {
            case LITERAL_INT:
                return value;
            case VARIABLE:
                return memory[(int) value];
            case ARRAY_LOOKUP:
                int index = (int) left.calculate(memory);
                int len = (int) memory[(int) value];
                if (index < 0 || index > len) {
                    throw new ArrayIndexOutOfBoundsException("index " + index + " max " + len);
                }
                return memory[(int) value + 1 + index];
            case OPERATION:
                long lv = left.calculate(memory);
                long rv = right.calculate(memory);
                switch(op) {
                case "=":
                    return lv == rv ? 1 : 0;
                case ">":
                    return lv > rv ? 1 : 0;
                case ">=":
                    return lv >= rv ? 1 : 0;
                case "<":
                    return lv < rv ? 1 : 0;
                case "<=":
                    return lv <= rv ? 1 : 0;
                case "<>":
                    return lv != rv ? 1 : 0;
                case "+":
                    return lv + rv;
                case "-":
                    return lv - rv;
                case "*":
                    return lv * rv;
                case "/":
                    return lv / rv;
                }
            }
            throw new UnsupportedOperationException("type: "+ type);
        }

        public String toString() {
            switch (type) {
            case LITERAL_INT:
                return "" + value;
            case VARIABLE:
                return "[" + value + "] (" + variableName + ")";
            case ARRAY_LOOKUP:
                return "[" + value + "+" + left + "] (" + variableName + ")";
            case OPERATION:
                return left + " " + op + " " + right;
            }
            return "?";
        }
    }

    static class Variable {
        int pos;
        boolean array;
    }

    public String run() {
        output = new StringBuilder();
        memory = new long[16 * 1024];
        int[] stack = new int[1024];
        int stackPos = 0;
        int pc = 0;
        while(pc >= 0) {
            Statement s = statements.get(pc);
            switch (s.type) {
            case ASSIGN: {
                long value = s.expr.calculate(memory);
                memory[s.position] = value;
                pc++;
                break;
            }
            case ASSIGN_ARRAY: {
                long value = s.expr.calculate(memory);
                int index = (int) s.index.calculate(memory);
                int len = (int) memory[(int) s.position];
                if (index < 0 || index > len) {
                    throw new ArrayIndexOutOfBoundsException("index " + index + " max " + len);
                }
                memory[s.position + 1 + index] = value;
                pc++;
                break;
            }
            case IF: {
                long value = s.expr.calculate(memory);
                if(value == 0) {
                    pc = s.position;
                } else {
                    pc++;
                }
                break;
            }
            case GOTO:
                pc = s.position;
                break;
            case BREAK: {
                long value = s.expr.calculate(memory);
                if(value != 0) {
                    pc = s.position;
                } else {
                    pc++;
                }
                break;
            }
            case CALL: {
                stack[stackPos++] = pc + 1;
                pc = s.position;
                break;
            }
            case PRINT: {
                output.append(s.expr.calculate(memory));
                pc++;
                break;
            }
            case PRINT_TEXT: {
                int pos = (int) s.expr.calculate(memory);
                int len = (int) memory[pos];
                for (int i = 0; i < len; i++) {
                    int x = (int) memory[pos + 1 + i];
                    output.append((char) x);
                }
                pc++;
                break;
            }
            case PRINT_END: {
                output.append("\n");
                pc++;
                break;
            }
            case RETURN: {
                if (stackPos == 0) {
                    pc = -1;
                } else {
                    pc = stack[--stackPos];
                }
                break;
            }
            default:
                throw new IllegalStateException();
            }
        }
        return output.toString();
    }

}
