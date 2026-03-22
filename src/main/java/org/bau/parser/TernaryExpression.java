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
        if (memory == null) {
            return null;
        }
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
            } else if (result == null) {
                return null;
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
    public void setOwnedBoundsToNull(Solver solver, int level, boolean loop) {
    }

    @Override
    public boolean isEasyToRead() {
        return false;
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
                    new NullValue(type);
            result = parser.assignTempVariable(target, init, type);
        }
        condition = condition.writeStatements(parser, assignment, target);
        If ifStatement = new If();
        ifStatement.condition = condition;
        ifStatement.thenList = ifTrueStatements;
        ifStatement.thenAutoClose = Collections.emptyList();
        if (result != null && ifTrue != null) {
            Assignment assign = new Assignment();
            assign.initial = false;
            assign.isConstant = false;
            assign.leftValue = result;
            assign.type = type;
            assign.value = ifTrue.writeStatements(parser, assignment, target);
            ifTrueStatements.add(assign);
        }
        ifStatement.elseList = ifFalseStatements;
        ifStatement.elseAutoClose = Collections.emptyList();
        if (result != null && ifFalse != null) {
            Assignment assign = new Assignment();
            assign.initial = false;
            assign.isConstant = false;
            assign.leftValue = result;
            assign.type = type;
            assign.value = ifFalse.writeStatements(parser, assignment, target);
            ifFalseStatements.add(assign);
        }
        target.add(ifStatement);
        target.add(new PhiBlock());
        return result;
    }

    @Override
    public void used(Program program) {
        type.used(program);
        condition.used(program);
        for (Statement s : ifTrueStatements) {
            s.used(program);
        }
        ifTrue.used(program);
        for (Statement s : ifFalseStatements) {
            s.used(program);
        }
        ifFalse.used(program);
    }

    @Override
    public boolean containsModifiableVariables() {
        return condition.containsModifiableVariables() || ifTrue.containsModifiableVariables() || ifFalse.containsModifiableVariables();
    }

    @Override
    public void setVariableVersions(FunctionContext functionContext, BasicBlock basicBlock) {
        condition.setVariableVersions(functionContext, basicBlock);
        ifTrue.setVariableVersions(functionContext, basicBlock);
        for (Statement s : ifTrueStatements) {
            s.setVariableVersions(functionContext, basicBlock);
        }
        ifFalse.setVariableVersions(functionContext, basicBlock);
        for (Statement s : ifFalseStatements) {
            s.setVariableVersions(functionContext, basicBlock);
        }
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
        condition.setVariableVersions(name, oldVersion, newVersion);
        ifTrue.setVariableVersions(name, oldVersion, newVersion);
        for (Statement s : ifTrueStatements) {
            s.setVariableVersions(name, oldVersion, newVersion);
        }
        ifFalse.setVariableVersions(name, oldVersion, newVersion);
        for (Statement s : ifFalseStatements) {
            s.setVariableVersions(name, oldVersion, newVersion);
        }
    }

    @Override
    public String toAST() {
        throw new IllegalStateException();
    }

}
