package org.bau.mini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MiniBauBytecode {
    /*
    long[] bytecodes = new long[1024];
    long[] memory = new long[1024];

    // during parsing only
    // positions for functions in the bytecode
    HashMap<String, Integer> functionPos = new HashMap<>();
    // memory location of variables
    HashMap<String, Integer> variablePos = new HashMap<>();
    // type of variables (0=int, 1=array)
    HashMap<String, Integer> variableType = new HashMap<>();
    // heap position
    int heapPos;

    StringBuilder output;
    String text;
    String token;
    int indent;
    int pos;
    int lastPos;

    void parse(String text) {
        this.text = text;
        read();
        parseProgram();
    }

    void parseProgram() {
        while (token != null) {
            while (match("\n"));
            if (match("fun")) {
                String name = token;
                read();
                expect("(");
                expect(")");
                expect("\n");
                functionPos.put(name, bytecodes.size());
                parseList(0);
            } else {
                String name = token;
                read();
                if (match(":=")) {
                    Expression expr = parseExpressionPrimary();
                    Value value = expr.get(this);
                    memory[heapPos] = value.value;
                    variablePos.put(name, heapPos);
                    variableType.put(name, 0);
                    heapPos++;
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

    private List<Statement> parseList(int oldIndent) {
        List<Statement> list = new ArrayList<>();
        while (true) {
            if (indent <= oldIndent) {
                break;
            }
            list.add(parseStatement());
        }
        return list;
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
            If statement = new If();
            If topIf = statement;
            statement.condition = parseExpression();
            expect("\n");
            statement.thenList = parseList(oldIndent);
            oldIndent = indent;
            while (match("elif")) {
                If statement2 = new If();
                statement.elseList = new ArrayList<>();
                statement.elseList.add(statement2);
                statement2.condition = parseExpression();
                expect("\n");
                statement2.thenList = parseList(oldIndent);
                statement = statement2;
            }
            if (match("else")) {
                expect("\n");
                statement.elseList = parseList(oldIndent);
            }
            return topIf;
        } else if (match("loop")) {
            expect("\n");
            Loop statement = new Loop();
            statement.list = parseList(oldIndent);
            return statement;
        } else if (match("break")) {
            Break statement = new Break();
            statement.condition = parseExpression();
            expect("\n");
            return statement;
        } else if (match("print")) {
            expect("(");
            Print statement = new Print();
            if (!match(")")) {
                statement.expr = parseExpression();
                expect(")");
            }
            expect("\n");
            return statement;
        } else {
            String identifier = token;
            read();
            if (match("=")) {
                Assignment s = new Assignment();
                s.variable = identifier;
                s.expr = parseExpression();
                expect("\n");
                return s;
            } else if (match("(")) {
                expect(")");
                Call statement = new Call();
                statement.name = identifier;
                expect("\n");
                return statement;
            } else if (match("[")) {
                Assignment s = new Assignment();
                s.variable = identifier;
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
        if (token.charAt(0) >= '0' && token.charAt(0) <= '9') {
            Literal expr = new Literal();
            expr.value = new Value(Long.parseLong(token));
            read();
            return expr;
        } else if (token.startsWith("'")) {
            Literal expr = new Literal();
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
                ArrayLookup expr = new ArrayLookup();
                expr.name = identifier;
                expr.index = parseExpressionPrimary();
                expect("]");
                return expr;
            } else {
                Variable expr = new Variable();
                expr.name = identifier;
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
            Operation operation = new Operation();
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
        return syntaxError(message, null);
    }

    private IllegalStateException syntaxError(String message, Exception e) {
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
        IllegalStateException ex = new IllegalStateException(message, e);
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

    void read() {
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

    static enum ByteCode {
        EQUAL,
        GREATER,
        GREATER_EQUAL,
        LESS,
        LESS_EQUAL,
        NOT_EQUAL,
        PLUS,
        MINUS,
        MULIPLY,
        DIVIDE,
        ASSIGN,
        ASSIGN_ARRAY,
        CALL,
        IF,
        ELSE,
        END_IF,
        LOOP,
        END_LOOP,
        PRINT_NUMBER,
        PRINT_TEXT,
        PRINT_NEWLINE,
        BREAK,
        LITERAL_INT,
        LITERAL_ARRAY,
        VARIABLE_INT,
        VARIABLE_ARRAY,
        ARRAY_LOOKUP,
    }

    static abstract class Statement {
        abstract void run(MiniBau memory);
    }

    static class Assignment extends Statement {
        String variable;
        Expression index;
        Expression expr;
        void run(MiniBau memory) {
            long value = expr.get(memory).value;
            Value v = memory.variables.get(variable);
            if (v == null) {
                throw new IllegalStateException("Variable '" + variable + "' not found");
            }
            if (index != null) {
                v.set(index.get(memory).value, value);
            } else {
                v.set(value);
            }
        }
        public String toString() {
            return variable + (index == null ? "" : "[" + index + "]") + " = " + expr;
        }
    }

    static class Call extends Statement {
        String name;
        void run(MiniBau memory) {
            List<Statement> list = memory.functions.get(name);
            for(Statement s : list) {
                s.run(memory);
            }
        }
        public String toString() {
            return name + "()";
        }
    }

    static class If extends Statement {
        Expression condition;
        List<Statement> thenList = new ArrayList<>();
        List<Statement> elseList = new ArrayList<>();
        void run(MiniBau memory) {
            Value value = condition.get(memory);
            if(value.get() != 0) {
                for(Statement s : thenList) {
                    s.run(memory);
                }
            } else {
                for(Statement s : elseList) {
                    s.run(memory);
                }
            }
        }
        public String toString() {
            return "if " + condition + "\n" + thenList.toString() +
                    (elseList == null ? "" : "\nelse " + elseList);
        }
    }

    static class Loop extends Statement {
        List<Statement> list = new ArrayList<>();
        void run(MiniBau memory) {
            for (int i = 0;; i++) {
                Statement s = list.get(i % list.size());
                if (s instanceof Break) {
                    if (((Break) s).condition.get(memory).value != 0) {
                        break;
                    }
                } else {
                    s.run(memory);
                }
            }
        }
        public String toString() {
            return "loop\n" + list.toString();
        }
    }

    static class Print extends Statement {
        Expression expr;
        void run(MiniBau memory) {
            if (expr == null) {
                memory.output.append('\n');
            } else {
                memory.output.append(expr.get(memory));
            }
        }
        public String toString() {
            return "print(" + expr + ")";
        }
    }

    static class Break extends Statement {
        Expression condition;
        void run(MiniBau memory) {
            throw new UnsupportedOperationException();
        }
        public String toString() {
            return "break " + condition;
        }
    }

    static abstract class Expression {
        abstract Value get(MiniBau memory);
    }

    static class Literal extends Expression {
        Value value;
        Value get(MiniBau memory) {
            return value;
        }
        public String toString() {
            return value.toString();
        }
    }

    static class Variable extends Expression {
        String name;
        Value get(MiniBau memory) {
            return memory.variables.get(name);
        }
        public String toString() {
            return name;
        }
    }

    static class ArrayLookup extends Expression {
        String name;
        Expression index;
        Value get(MiniBau memory) {
            long x = memory.variables.get(name).get((int) index.get(memory).value);
            return new Value(x);
        }
        public String toString() {
            return name + "[" + index + "]";
        }
    }

    static class Operation extends Expression {
        String op;
        Expression left;
        Expression right;
        Value get(MiniBau memory) {
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
            throw new UnsupportedOperationException("op: "+ op);
        }
        public String toString() {
            return left + " " + op + " " + right;
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
        List<Statement> list = functions.get("main");
        if (list == null) {
            throw new IllegalStateException("Function 'main' not found");
        }
        for(Statement s : list) {
            s.run(this);
        }
        return output.toString();
    }
*/
}
