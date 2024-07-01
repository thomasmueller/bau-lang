package org.bau.parser;

import java.util.ArrayList;
import java.util.Collections;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueInt;

public class Operation implements Expression {
    
    Expression left;
    String operation;
    Expression right;
    
    Operation(Expression left, String operation, Expression right) {
        if (left != null) {
            Value l = left.eval(null);
            if (l != null) {
                left = new NumberValue(l, left.type(), false);
            }
        }
        Value r = right.eval(null);
        if (r != null) {
            right = new NumberValue(r, right.type(), false);
        }
        this.left = left;
        this.operation = operation;
        this.right = right;
    }
    
    @Override
    public Expression simplify() {
        return this;
    }
    
    private static String addBracketsIfNeeded(Expression expr) {
        String s = expr.toString();
        // TODO this is a hack
        if (expr instanceof Operation) {
            if (!s.startsWith("(")) {
                return "( " + s + " )";
            }
        }
        return s;
    }
    
    public DataType canThrowException() {
        DataType fails = null;
        if (left != null && left.canThrowException() != null) {
            fails = left.canThrowException();
        }
        if (right != null && right.canThrowException() != null) {
            fails = right.canThrowException();
        }
        if (fails != null) {
            throw new IllegalStateException("Method calls that can throw an exception must be in a separate line");  
        }
        return null;
    }
    
    private static String addBracketsIfNeededToC(Expression expr) {
        String s = expr.toC();
        // TODO this is a hack
        if (expr instanceof Operation) {
            if (!s.startsWith("(")) {
                return "( " + s + " )";
            }
        }
        return s;
    }

    @Override
    public Value eval(Memory memory) {
        Value r = right.eval(memory);
        if (left == null) {
            if ("-".equals(operation)) {
                if (r != null) {
                    return new Value.ValueInt(- r.longValue());
                }
            } else {
                throw new IllegalStateException("operation " + operation);                
            }
        }
        Value l = left.eval(memory);
        if (l == null || r == null) {
            return null;
        }
        switch (operation) {
        case "+":
            return new Value.ValueInt(l.longValue() + r.longValue());
        case "*":
            return new Value.ValueInt(l.longValue() * r.longValue());
        case "-":
            return new Value.ValueInt(l.longValue() - r.longValue());
        case ">":
            return l.longValue() > r.longValue() ? ValueInt.ONE : ValueInt.ZERO;
        case "<":
            return l.longValue() < r.longValue() ? ValueInt.ONE : ValueInt.ZERO;
        case "=":
            return l.longValue() == r.longValue() ? ValueInt.ONE : ValueInt.ZERO;
        case "!=":
            return l.longValue() != r.longValue() ? ValueInt.ONE : ValueInt.ZERO;
        case "&":
            return new Value.ValueInt(l.longValue() & r.longValue());
        case "|":
            return new Value.ValueInt(l.longValue() | r.longValue());
        case "^":
            return new Value.ValueInt(l.longValue() ^ r.longValue());
        default:
            throw new IllegalStateException("operation " + operation);
        }
    }
    
    @Override
    public DataType type() {
        if (left == null) {
            return right.type();
        }
        DataType l = left.type();
        if (!l.isSystem()) {
            throw new IllegalStateException("Not a built-in type: " + l + " for operation " + operation);
        }
        DataType r = right.type();
        if (!r.isSystem()) {
            throw new IllegalStateException("Not a built-in type: " + r + " for operation " + operation);
        }
        if (l.equals(r)) {
            return l;
        }
        if (l.isSystem() && r.isSystem()) {
            DataType higher = null;
            if (l.isFloatingPoint != r.isFloatingPoint) {
                // only on is floating point: take that
                if (l.isFloatingPoint) {
                    higher = l;
                } else {
                    higher = r;
                }
            }
            if (higher == null) {
                // take the one with more bits
                if (l.sizeOf() > r.sizeOf()) {
                    higher = l;
                } else {
                    higher = r;
                }
            }
            if (higher != null) {
                return higher;
            }
        }            
        throw new IllegalStateException("Operands needs to be of the same type: " + l + " <-> " + r);
    }
    
    public Expression replace(Variable old, Expression with) {
        Operation c = new Operation(left.replace(old, with), operation, right.replace(old, with));
        return c;
    }
    
    public String toC() {
        String op = operation;
        if (left == null) {
            if ("not".equals(op)) {
                return "!(" + addBracketsIfNeededToC(right) + ")";
            }
            return op + " " + addBracketsIfNeededToC(right);
        }
        if (">>".equals(op)) {
            return "shiftRight_" + left.type().name() + "_2(" + left.toC() + ", " + right.toC() + ")";
        } else if ("<<".equals(op)) {
            return "shiftLeft_2(" + left.toC() + ", " + right.toC() + ")";
        } else if ("/".equals(op)) {
            return "idiv_2(" + left.toC() + ", " + right.toC() + ")";
        } else if ("%".equals(op)) {
            return "imod_2(" + left.toC() + ", " + right.toC() + ")";
        } else if ("and".equals(op)) {
            return "(" + left.toC() + ") && (" + right.toC() + ")";
        } else if ("or".equals(op)) {
            return "(" + left.toC() + ") || (" + right.toC() + ")";
        }
        if ("=".equals(op)) {
            op = "==";
        }
        return addBracketsIfNeededToC(left) + " " + op + " " + addBracketsIfNeededToC(right);
    }
    
    public String toString() {
        if (left == null) {
            return operation + " " + addBracketsIfNeeded(right);
        }
        return addBracketsIfNeeded(left) + " " + operation + " " + addBracketsIfNeeded(right);
    }
    
    @Override
    public boolean isEasyToRead() {
        return false;
    }

    public void applyBoundCondition(Expression scope, boolean reversed) {
        if ("and".equals(operation) && !reversed) {
            left.applyBoundCondition(scope, false);
            right.applyBoundCondition(scope, false);
            return;
        } else if ("or".equals(operation) && reversed) {
            left.applyBoundCondition(scope, true);
            right.applyBoundCondition(scope, true);
        }
        if (!(left instanceof LeftValue)) {
            return;
        }
        LeftValue var = (LeftValue) left;
        String op = operation;
        if (reversed) {
            switch(operation) {
            case ">":
                op = "<=";
                break;
            case ">=":
                op = "<";
                break;
            case "=":
                op = "!=";
                break;
            case "!=":
                op = "=";
                break;
            case "<":
                op = ">=";
                break;
            case "<=":
                op = ">";
                break;
            default: 
                op = null;
            }
        }
        switch (op) {
        case ">":
        case ">=":
        case "=":
        case "<":
        case "<=":
        case "!=":
            var.addBoundCondition(scope, op, right);
        }
    }

    @Override
    public Bounds getBounds() {
        if ("+".equals(operation)) {
            Value v = right.eval(null);
            if (v != null) {
                Bounds b = left.getBounds();
                if (b != null) {
                    b = b.plus(v.longValue());
                    return b;
                }
            }
        } else if ("-".equals(operation)) {
            Value v = right.eval(null);
            if (v != null) {
                Bounds b = left.getBounds();
                if (b != null) {
                    b = b.plus(-v.longValue());
                    return b;
                }
            }
        }
        return null;
    }
    
    @Override
    public boolean isSimple() {
        return false;
    }
    
    public Expression writeStatements(Parser parser, ArrayList<Statement> target) {
        if (left != null) {
            left = left.writeStatements(parser, target);
        }
        if ("or".equals(operation) || "and".equals(operation)) {
            Variable var = parser.assignTempVariable(target, left);
            If ifStatement = new If();
            if ("or".equals(operation)) {
                Expression not = new Operation(null, "not", var);
                ifStatement.conditions.add(not);
            } else {
                ifStatement.conditions.add(var);
            }
            ArrayList<Statement> list = new ArrayList<Statement>();
            ifStatement.listList.add(list);
            ifStatement.autoClose(Collections.emptyList());
            Variable v2 = parser.assignTempVariable(list, right);
            Assignment assign = new Assignment();
            assign.initial = false;
            assign.isConstant = false;
            assign.leftValue = var;
            assign.type = v2.type;
            assign.value = v2;
            list.add(assign);
            
            target.add(ifStatement);
            return var;
        } else {
            right = right.writeStatements(parser, target);
        }
        if (canThrowException() == null) {
            return this;
        }
        if (left != null) {
            Variable varLeft = parser.assignTempVariable(target, left);
            Variable varRight = parser.assignTempVariable(target, right);
            return new Operation(varLeft, operation, varRight);
        }
        Variable varRight = parser.assignTempVariable(target, right);
        return new Operation(null, operation, varRight);
    }

    static int getPrecedence(String operator) {
        if (operator == null) {
            return 0;
        }
        switch(operator) {
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

}
