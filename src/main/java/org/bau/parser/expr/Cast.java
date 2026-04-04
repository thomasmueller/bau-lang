package org.bau.parser.expr;

import java.util.ArrayList;
import java.util.List;

import org.bau.parser.BasicBlock;
import org.bau.parser.DataType;
import org.bau.parser.FunctionContext;
import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.bau.parser.Solver;
import org.bau.parser.stmt.Statement;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Cast implements Expression {

    private final Expression base;
    private DataType targetType;

    public Cast(Expression base, DataType targetType) {
        this.base = base;
        this.targetType = targetType;
    }

    @Override
    public Value eval(Memory memory) {
        return base.eval(memory);
    }

    @Override
    public DataType type() {
        return targetType;
    }

    @Override
    public DataType canThrowException() {
        return base.canThrowException();
    }

    @Override
    public Expression replace(Variable old, Expression with) {
        return new Cast(base.replace(old, with), targetType);
    }

    @Override
    public Expression simplify() {
        return new Cast(base.simplify(), targetType);
    }

    @Override
    public String toC() {
        return "((" + targetType.toC() + ") " + base.toC() + ")";
    }

    @Override
    public boolean isEasyToRead() {
        return base.isEasyToRead();
    }

    @Override
    public void setOwnedBoundsToNull(Solver solver, int level, boolean loop) {
        base.setOwnedBoundsToNull(solver, level, loop);
    }

    @Override
    public boolean isSimple() {
        return base.isSimple();
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        return new Cast(base.writeStatements(parser, assignment, target), targetType);
    }

    @Override
    public void used(Program program) {
        base.used(program);
        targetType.used(program);
    }

    @Override
    public boolean containsModifiableVariables() {
        return base.containsModifiableVariables();
    }

    @Override
    public List<Expression> getUsedOwned() {
        return base.getUsedOwned();
    }

    @Override
    public void setVariableVersions(FunctionContext functionContext, BasicBlock basicBlock) {
        base.setVariableVersions(functionContext, basicBlock);
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
        base.setVariableVersions(name, oldVersion, newVersion);
    }

    @Override
    public List<Variable> getVariables() {
        return base.getVariables();
    }

    @Override
    public String format() {
        return base.format();
    }

    @Override
    public String toAST() {
        return "\"cast\"," + base.toAST() + ",\"" + targetType + "\"";
    }

    @Override
    public void resolveTypes(Program program) {
        base.resolveTypes(program);
        targetType = targetType.resolve(program);
    }

    public String toString() {
        return format();
    }

}
