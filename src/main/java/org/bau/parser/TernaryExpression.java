package org.bau.parser;

import java.util.ArrayList;
import java.util.Collections;

import org.bau.parser.Statement.StatementResult;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValuePanic;

public class TernaryExpression implements Expression {

    DataType type;
    Expression condition;
    ArrayList<Statement> ifTrueStatements = new ArrayList<>();
    Expression ifTrue;
    ArrayList<Statement> ifFalseStatements = new ArrayList<>();
    Expression ifFalse;

    @Override
    public Value eval(Memory memory) {
        Value v = condition.eval(memory);
        if (v == null || v instanceof ValuePanic) {
            return v;
        }
        ArrayList<Statement> list = null;
        Expression returnExpr = null;
        if (v.longValue() != 0) {
            list = ifTrueStatements;
            returnExpr = ifTrue;
        } else {
            list = ifFalseStatements;
            returnExpr = ifFalse;
        }
        if (list != null) {
            StatementResult result = Program.runSequence(memory, list);
            if (result == StatementResult.PANIC) {
                return new ValuePanic(memory.getGlobal(Memory.PANIC).toString());
            }
        }
        if (returnExpr != null) {
            return returnExpr.eval(memory);
        }
        return null;
    }

    @Override
    public DataType type() {
        return type;
    }

    @Override
    public DataType canThrowException() {
        return null;
    }

    @Override
    public Expression replace(Variable old, Expression with) {
     // TODO not implemented yet (may never be needed)
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public Expression simplify() {
        return this;
    }

    @Override
    public String toC() {
        // TODO not implemented yet (may never be needed)
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public void setOwnedBoundsToNull(Expression scope) {
    }

    @Override
    public boolean isEasyToRead() {
        return false;
    }

    @Override
    public Bounds getBounds() {
        // TODO not implemented yet
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        Variable result;
        if (type == null) {
            result = null;
        } else {
            Expression init = type.isNumber() ?
                    new NumberValue(Value.ValueInt.ZERO, type, false) :
                    new NullValue();
            result = parser.assignTempVariable(target, init, type);
        }
        If ifStatement = new If();
        ifStatement.conditions.add(condition);
        ifStatement.listList.add(ifTrueStatements);
        ifStatement.autoClose(Collections.emptyList());
        if (result != null && ifTrue != null) {
            Assignment assign = new Assignment();
            assign.initial = false;
            assign.isConstant = false;
            assign.leftValue = result;
            assign.type = type;
            assign.value = ifTrue;
            ifTrueStatements.add(assign);
        }
        ifStatement.listList.add(ifFalseStatements);
        ifStatement.autoClose(Collections.emptyList());
        if (result != null && ifFalse != null) {
            Assignment assign = new Assignment();
            assign.initial = false;
            assign.isConstant = false;
            assign.leftValue = result;
            assign.type = type;
            assign.value = ifFalse;
            ifFalseStatements.add(assign);
        }
        target.add(ifStatement);
        return result;
    }

}
