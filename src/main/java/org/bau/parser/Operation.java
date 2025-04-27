package org.bau.parser;

import java.util.ArrayList;
import java.util.Collections;

import org.bau.parser.Bounds.ApplyType;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueFloat;
import org.bau.runtime.Value.ValueI16;
import org.bau.runtime.Value.ValueI32;
import org.bau.runtime.Value.ValueI8;
import org.bau.runtime.Value.ValueInt;
import org.bau.runtime.Value.ValueNull;
import org.bau.runtime.Value.ValuePanic;
import org.bau.runtime.Value.ValueRef;
import org.bau.runtime.Value.ValueStruct;

public class Operation implements Expression {

    Expression left;
    String operator;
    Expression right;

    Operation(Expression left, String operator, Expression right) {
        if (left != null) {
            Value l = left.eval(null);
            if (l != null) {
                if (l == ValueNull.INSTANCE) {
                    left = new NullValue();
                } else {
                    left = new NumberValue(l, left.type(), false);
                }
            }
        }
        Value r = right.eval(null);
        if (r != null) {
            if (r == ValueNull.INSTANCE) {
                right = new NullValue();
            } else {
                right = new NumberValue(r, right.type(), false);
            }
        }
        this.left = left;
        this.operator = operator;
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

    @Override
    public Value eval(Memory memory) {
        Value r = right.eval(memory);
        if (left == null) {
            if (r == null) {
                return null;
            }
            if ("-".equals(operator)) {
                if (right.type().isFloatingPoint()) {
                    return new Value.ValueFloat(- r.doubleValue());
                }
                return new Value.ValueInt(- r.longValue());
            } else if ("not".equals(operator)){
                return new Value.ValueInt(r.longValue() == 0 ? 1 : 0);
            } else {
                throw new IllegalStateException("operation " + operator);
            }
        }
        Value l = left.eval(memory);
        if (l == null || r == null) {
            return null;
        }
        if (l instanceof ValuePanic) {
            return l;
        } else if (r instanceof ValuePanic) {
            return r;
        }
        switch (operator) {
        case "<<":
        case ">>":
            // need to use the type of the left, so that right shift
            // is truncated correctly
            return eval(left.type(), l, operator, r);
        }
        // need to use the wider type, so that multiplication etc
        // by float work correctly
        return eval(widerType(), l, operator, r);
    }

    private DataType widerType() {
        switch (operator) {
        case "and":
        case "or":
        case "not":
            return DataType.INT_TYPE;
        case "=":
        case "<>":
            if (left instanceof NullValue || right instanceof NullValue) {
                return DataType.INT_TYPE;
            }
        }
        if (left == null) {
            return right.type().resolveEnumType();
        }
        DataType l = left.type().resolveEnumType();
        if (!l.isNumber()) {
            throw new IllegalStateException("Not a number type: " + l + " for operation " + operator);
        }
        DataType r = right.type().resolveEnumType();
        if (!r.isNumber()) {
            throw new IllegalStateException("Not a number type: " + r + " for operation " + operator);
        }
        if (l.equals(r)) {
            return l;
        }
        if (l.isNumber() && r.isNumber()) {
            DataType higher = null;
            if (l.isFloatingPoint() != r.isFloatingPoint()) {
                // only on is floating point: take that
                if (l.isFloatingPoint()) {
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

    public static Value eval(DataType type, Value l, String operator, Value r) {
        if (type.isFloatingPoint()) {
            return evalFloat(type, l, operator, r);
        }
        long result;
        switch (operator) {
        case "+":
            result = l.longValue() + r.longValue();
            break;
        case "*":
            result = l.longValue() * r.longValue();
            break;
        case "/": {
            if (r.longValue() == 0) {
                if (l.longValue() == 0) {
                    result = 0;
                } else if (l.longValue() > 0) {
                    result = Long.MAX_VALUE;
                } else {
                    result = Long.MIN_VALUE;
                }
            } else {
                result = l.longValue() / r.longValue();
            }
            break;
        }
        case "%":
            if (r.longValue() == 0) {
                result = 0;
            } else {
                result = l.longValue() % r.longValue();
            }
            break;
        case "-":
            result = l.longValue() - r.longValue();
            break;
        case ">":
            result = l.longValue() > r.longValue() ? 1 : 0;
            break;
        case ">=":
            result = l.longValue() >= r.longValue() ? 1 : 0;
            break;
        case "<":
            result = l.longValue() < r.longValue() ? 1 : 0;
            break;
        case "<=":
            result = l.longValue() <= r.longValue() ? 1 : 0;
            break;
        case "=":
            if (l == ValueNull.INSTANCE || r == ValueNull.INSTANCE) {
                result = l == r ? 1 : 0;
            } else {
                result = l.longValue() == r.longValue() ? 1 : 0;
            }
            break;
        case "<>":
            if (l == ValueNull.INSTANCE || r == ValueNull.INSTANCE) {
                result = l != r ? 1 : 0;
            } else {
                result = l.longValue() != r.longValue() ? 1 : 0;
            }
            break;
        case "&":
            result = l.longValue() & r.longValue();
            break;
        case "|":
            result = l.longValue() | r.longValue();
            break;
        case "^":
            result = l.longValue() ^ r.longValue();
            break;
        case ">>":
            // need to truncate negative number before shifting
            if (type.name().equals(DataType.I32)) {
                result = ((int) l.longValue()) >>> r.intValue();
            } else if (type.name().equals(DataType.I16)) {
                result = ((short) l.longValue()) >>> r.intValue();
            } else if (type.name().equals(DataType.I8)) {
                result = ((byte) l.longValue()) >>> r.intValue();
            } else {
                result = l.longValue() >>> r.intValue();
            }
            break;
        case "<<":
            result = l.longValue() << r.longValue();
            break;
        case "and":
            result = (l.longValue() != 0 && r.longValue() != 0) ? 1 : 0;
            break;
        case "or":
            result = (l.longValue() != 0 || r.longValue() != 0) ? 1 : 0;
            break;
        default:
            throw new IllegalStateException("operation " + operator);
        }
        return new ValueInt(result);
    }

    public static Value evalFloat(DataType type, Value l, String operator, Value r) {
        double result;
        switch (operator) {
        case "+":
            result = l.doubleValue() + r.doubleValue();
            break;
        case "*":
            result = l.doubleValue() * r.doubleValue();
            break;
        case "/":
            result = l.doubleValue() / r.doubleValue();
            break;
        case "%":
            result = l.doubleValue() % r.doubleValue();
            break;
        case "-":
            result = l.doubleValue() - r.doubleValue();
            break;
        case ">":
            result = l.doubleValue() > r.doubleValue() ? 1 : 0;
            break;
        case ">=":
            result = l.doubleValue() >= r.doubleValue() ? 1 : 0;
            break;
        case "<":
            result = l.doubleValue() < r.doubleValue() ? 1 : 0;
            break;
        case "<=":
            result = l.doubleValue() <= r.doubleValue() ? 1 : 0;
            break;
        case "=":
            if (l == ValueNull.INSTANCE || r == ValueNull.INSTANCE) {
                result = l == r ? 1 : 0;
            } else {
                result = l.doubleValue() == r.doubleValue() ? 1 : 0;
            }
            break;
        case "<>":
            if (l == ValueNull.INSTANCE || r == ValueNull.INSTANCE) {
                result = l != r ? 1 : 0;
            } else {
                result = l.doubleValue() != r.doubleValue() ? 1 : 0;
            }
            break;
        case "&":
            result = l.longValue() & r.longValue();
            break;
        case "|":
            result = l.longValue() | r.longValue();
            break;
        case "^":
            result = l.longValue() ^ r.longValue();
            break;
        case ">>":
            result = l.longValue() >>> r.longValue();
            break;
        case "<<":
            result = l.longValue() << r.longValue();
            break;
        case "and":
            result = (l.longValue() != 0 && r.longValue() != 0) ? 1 : 0;
            break;
        case "or":
            result = (l.longValue() != 0 || r.longValue() != 0) ? 1 : 0;
            break;
        default:
            throw new IllegalStateException("operation " + operator);
        }
        return new ValueFloat(result);
    }

    @Override
    public DataType type() {
        if (isComparison()) {
            return DataType.INT_TYPE;
        }
        return widerType();
    }

    public Expression replace(Variable old, Expression with) {
        Operation c = new Operation(left == null ? null : left.replace(old, with), operator, right.replace(old, with));
        return c;
    }

    public String toC() {
        String op = operator;
        if (left == null) {
            if ("not".equals(op)) {
                return "!(" + addBracketsIfNeededToC(right) + ")";
            }
            return op + " " + addBracketsIfNeededToC(right);
        }
        if (">>".equals(op)) {
            DataType t = left.type();
            if (t.isRange()) {
                t = DataType.INT_TYPE;
            }
            return "shiftRight_" + t.name() + "_2(" +
                    left.toC() + ", " + right.toC() + ")";
        } else if ("<<".equals(op)) {
            return "shiftLeft_2(" +
                    left.toC() + ", " + right.toC() + ")";
        } else if ("/".equals(op)) {
            if (widerType().isFloatingPoint()) {
                return left.toC() + " / " + right.toC();
            } else {
                return "idiv_2(" +
                        left.toC() + ", " + right.toC() + ")";
            }
        } else if ("%".equals(op)) {
            return "imod_2(" +
                    left.toC() + ", " + right.toC() + ")";
        } else if ("and".equals(op)) {
            return "(" + left.toC() + ") && (" + right.toC() + ")";
        } else if ("or".equals(op)) {
            return "(" + left.toC() + ") || (" + right.toC() + ")";
        }
        if ("=".equals(op)) {
            op = "==";
        } else if ("<>".equals(op)) {
            op = "!=";
        }
        return addBracketsIfNeededToC(left) + " " + op + " " + addBracketsIfNeededToC(right);
    }

    public String toString() {
        if (left == null) {
            return operator + " " + addBracketsIfNeeded(right);
        }
        return addBracketsIfNeeded(left) + " " + operator + " " + addBracketsIfNeeded(right);
    }

    @Override
    public boolean isEasyToRead() {
        return false;
    }

    public void applyBoundCondition(Expression scope, ApplyType type) {
        if (type == ApplyType.UNDO && ("and".equals(operator) || "or".equals(operator))) {
            left.applyBoundCondition(scope, type);
            right.applyBoundCondition(scope, type);
            return;
        }
        if ("and".equals(operator) && type == ApplyType.POSITIVE) {
            left.applyBoundCondition(scope, type);
            right.applyBoundCondition(scope, type);
            return;
        } else if ("or".equals(operator) && type == ApplyType.NEGATIVE) {
            left.applyBoundCondition(scope, type);
            right.applyBoundCondition(scope, type);
        }
        String op = operator;
        LeftValue var = null;
        if (left instanceof LeftValue) {
            var = (LeftValue) left;
        }
        Expression compare = right;
        if (type == ApplyType.NEGATIVE) {
            switch(operator) {
            case "not":
                if (right instanceof LeftValue) {
                    op = "<>";
                    var = (LeftValue) right;
                    compare = new NullValue();
                }
                break;
            case ">":
                op = "<=";
                break;
            case ">=":
                op = "<";
                break;
            case "=":
                op = "<>";
                break;
            case "<>":
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
        if (var == null) {
            return;
        }
        if (op == null) {
            // eg. "if i & 1"
            return;
        }
        switch (op) {
        case ">":
        case ">=":
        case "=":
        case "<":
        case "<=":
        case "<>":
            if (type == ApplyType.UNDO) {
                var.addBoundCondition(scope, null, null);
            } else {
                var.addBoundCondition(scope, op, compare);
            }
        }
    }

    @Override
    public Bounds getBounds() {
        if ("+".equals(operator)) {
            Value v = right.eval(null);
            if (v != null) {
                Bounds b = left.getBounds();
                if (b != null) {
                    b = b.plus(v.longValue());
                    return b;
                }
            }
        } else if ("-".equals(operator)) {
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

    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        if (left != null) {
            left = left.writeStatements(parser, false, target);
        }
        if ("or".equals(operator) || "and".equals(operator)) {
            Variable var = parser.assignTempVariable(target, left);
            If ifStatement = new If();
            if ("or".equals(operator)) {
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
            assign.type = v2.type();
            assign.value = v2;
            list.add(assign);
            target.add(ifStatement);
            return var;
        } else {
            right = right.writeStatements(parser, false, target);
        }
        if (canThrowException() == null) {
            return this;
        }
        if (left != null) {
            Variable varLeft = parser.assignTempVariable(target, left);
            Variable varRight = parser.assignTempVariable(target, right);
            return new Operation(varLeft, operator, varRight);
        }
        Variable varRight = parser.assignTempVariable(target, right);
        return new Operation(null, operator, varRight);
    }

    @Override
    public boolean isComparison() {
        return isComparison(operator);
    }

    public static boolean isComparison(String operator) {
        switch(operator) {
        case "=":
        case "<>":
        case "<=":
        case ">=":
        case "<":
        case ">":
            return true;
        }
        return false;
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

    @Override
    public void setOwnedBoundsToNull(Expression scope) {
        if (left != null) {
            left.setOwnedBoundsToNull(scope);
        }
        right.setOwnedBoundsToNull(scope);
    }

    public static Value convertToType(Value val, DataType targetType) {
        if (val == null) {
            throw new IllegalStateException("Cannot convert null to " + targetType);
        }
        if (val instanceof ValueStruct || val instanceof ValueRef) {
            return val;
        }
        switch (targetType.name()) {
        case DataType.F32:
            return new ValueFloat((float) val.doubleValue());
        case DataType.FLOAT:
            return new ValueFloat(val.doubleValue());
        case DataType.I8:
            return new ValueI8((byte) val.intValue());
        case DataType.I16:
            return new ValueI16((short) val.intValue());
        case DataType.I32:
            return new ValueI32(val.intValue());
        case DataType.INT:
            return new ValueInt(val.longValue());
        }
        if (targetType.isRange()) {
            return new ValueInt(val.longValue());
        }
        if (targetType.isArray() || targetType.isPointer()) {
            if (val instanceof ValueNull) {
                return val;
            }
        }
        throw new IllegalStateException("Unsupported target type " + targetType + " for " + val);
    }

}
