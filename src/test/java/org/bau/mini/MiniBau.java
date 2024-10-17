package org.bau.mini;

import java.util.ArrayList;
import java.util.HashMap;

public class MiniBau {
    private final HashMap<String, Value> variables = new HashMap<>();
    private final HashMap<String, Integer> functionPos = new HashMap<>();
    private StringBuilder output;
    private String text;
    private String token;
    private int indent;
    private int pos;
    private int lastPos;
    private Statement currentLoopStatement;
    private ArrayList<Statement> currentLoopBreakStatements = new ArrayList<>();

    private ArrayList<Statement> statements = new ArrayList<>();

    public void parse(String text) {
        this.text = text;
        read();
        while (token != null) {
            while (match("\n"));
            if (match("fun")) {
                String name = token;
                functionPos.put(name, statements.size());
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
                    Value value = expr.get(this);
                    variables.put(name, value);
                } else if (match(":")) {
                    if (match("array")) {
                        expect("(");
                        Expression expr = parseExpressionPrimary();
                        long len = expr.get(this).value;
                        Value value = new Value(0);
                        value.array = new long[(int) len];
                        variables.put(name, value);
                        expect(")");
                    } else {
                        Expression expr = parseExpressionPrimary();
                        variables.put(name, expr.get(this));
                    }
                }
                expect("\n");
            }
        }
    }

    private void parseBlock(int oldIndent) {
        while (true) {
            if (indent <= oldIndent) {
                break;
            }
            parseStatement();
        }
        for(Statement s : statements) {
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
            for(Statement g : breaks) {
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
            if (!match(")")) {
                statement.expr = parseExpression();
                statement.type = StatementType.PRINT;
                expect(")");
            } else {
                statement.type = StatementType.PRINT_NEWLINE;
            }
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
                s.variable = variables.get(identifier);
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
                s.variable = variables.get(identifier);
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
            expr.value = new Value(Long.parseLong(token));
            read();
            return expr;
        } else if (token.startsWith("'")) {
            expr.type = ExpressionType.LITERAL_ARRAY;
            token = token.substring(1);
            expr.value = new Value(0);
            expr.value.array = new long[token.length() + 1];
            expr.value.array[0] = token.length();
            for (int i = 0; i < token.length(); i++) {
                expr.value.array[i + 1] = token.charAt(i);
            }
            read();
            return expr;
        } else {
            String identifier = token;
            read();
            if (match("[")) {
                expr.type = ExpressionType.ARRAY_LOOKUP;
                expr.value = variables.get(identifier);
                expr.variableName = identifier;
                expr.left = parseExpressionPrimary();
                expect("]");
                return expr;
            } else {
                expr.type = ExpressionType.VARIABLE;
                expr.value = variables.get(identifier);
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
        case "!=":
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
        ELSE,
        LOOP,
        GOTO,
        BREAK,
        CALL,
        PRINT,
        PRINT_NEWLINE,
        RETURN;
    }

    static class Statement {
        StatementType type;
        Value variable;
        Expression expr;
        Expression index;
        int position;
        // only for toString
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
                return "print()";
            case PRINT_NEWLINE:
                return "print()";
            case RETURN:
                return "return";
            default:
                throw new IllegalStateException();
            }
        }
    }

    static enum ExpressionType {
        LITERAL_INT,
        LITERAL_ARRAY,
        VARIABLE,
        ARRAY_LOOKUP,
        OPERATION
    }

    static class Expression {
        ExpressionType type;
        // operation
        String op;
        // literal_int, literal_array, variable, array_lookup
        Value value;
        // operation (index is left for array)
        Expression left, right;
        String variableName;

        Value get(MiniBau memory) {
            switch (type) {
            case LITERAL_INT:
            case LITERAL_ARRAY:
            case VARIABLE:
                return value;
            case ARRAY_LOOKUP:
                long x = value.get((int) left.get(memory).value);
                return new Value(x);
            case OPERATION:
                long lv = left.get(memory).value;
                long rv = right.get(memory).value;
                switch(op) {
                case "=":
                    return new Value(lv == rv ? 1 : 0);
                case ">":
                    return new Value(lv > rv ? 1 : 0);
                case ">=":
                    return new Value(lv >= rv ? 1 : 0);
                case "<":
                    return new Value(lv < rv ? 1 : 0);
                case "<=":
                    return new Value(lv <= rv ? 1 : 0);
                case "!=":
                    return new Value(lv != rv ? 1 : 0);
                case "+":
                    return new Value(lv + rv);
                case "-":
                    return new Value(lv - rv);
                case "*":
                    return new Value(lv * rv);
                case "/":
                    return new Value(lv / rv);
                }
            }
            throw new UnsupportedOperationException("type: "+ type);
        }

        public String toString() {
            switch (type) {
            case LITERAL_INT:
            case LITERAL_ARRAY:
                return value.toString();
            case VARIABLE:
                return variableName;
            case ARRAY_LOOKUP:
                return variableName + "[" + left + "]";
            case OPERATION:
                return left + " " + op + " " + right;
            }
            return "?";
        }
    }

    static class Value {
        long value;
        long[] array;

        Value(long value) {
            this.value = value;
        }
        long get() {
            return value;
        }
        void set(long value) {
            this.value = value;
        }
        long get(long index) {
            return array[(int) index];
        }
        void set(long index, long value) {
            array[(int) index] = value;
        }
        public String toString() {
            if (array != null) {
                StringBuilder buff = new StringBuilder();
                int len = (int) array[0];
                for (int i = 0; i < len; i++) {
                    buff.append((char) array[i + 1]);
                }
                return buff.toString();
            }
            return "" + value;
        }
    }

    public String run() {
        output = new StringBuilder();
        ArrayList<Integer> stack = new ArrayList<>();
        int pc = functionPos.get("main");
        while(pc >= 0) {
            Statement s = statements.get(pc);
            switch (s.type) {
            case ASSIGN: {
                long value = s.expr.get(this).value;
                Value v = s.variable;
                v.set(value);
                pc++;
                break;
            }
            case ASSIGN_ARRAY: {
                long value = s.expr.get(this).value;
                Value v = s.variable;
                v.set(s.index.get(this).value, value);
                pc++;
                break;
            }
            case IF: {
                Value value = s.expr.get(this);
                if(value.get() == 0) {
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
                Value value = s.expr.get(this);
                if(value.get() != 0) {
                    pc = s.position;
                } else {
                    pc++;
                }
                break;
            }
            case CALL: {
                stack.add(pc + 1);
                pc = s.position;
                break;
            }
            case PRINT: {
                output.append(s.expr.get(this));
                pc++;
                break;
            }
            case PRINT_NEWLINE: {
                output.append("\n");
                pc++;
                break;
            }
            case RETURN: {
                if (stack.size() == 0) {
                    pc = -1;
                } else {
                    pc = stack.remove(stack.size() - 1);
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
