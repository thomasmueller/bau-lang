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

public class Parentheses implements Expression {

    private Expression base;

    public Parentheses(Expression base) {
        this.base = base;
    }

    @Override
    public Value eval(Memory memory) {
        return base.eval(memory);
    }

    @Override
    public DataType type() {
        return base.type();
    }

    @Override
    public DataType canThrowException() {
        return base.canThrowException();
    }

    @Override
    public Expression replace(Variable old, Expression with) {
        return new Parentheses(base.replace(old, with));
    }

    @Override
    public String toC() {
        return "(" + base.toC() + ")";
    }

    @Override
    public String format() {
        return "(" + base.format() + ")";
    }

    @Override
    public boolean isEasyToRead() {
        return true;
    }

    @Override
    public void setOwnedBoundsToNull(Solver solver, int level, boolean loop) {
        base.setOwnedBoundsToNull(solver, level, loop);
    }

    @Override
    public Expression simplify() {
        return this;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        base = base.writeStatements(parser, assignment, target);
        return this;
    }

    @Override
    public void used(Program program) {
        base.used(program);
    }

    @Override
    public boolean containsModifiableVariables() {
        return base.containsModifiableVariables();
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
    public String toAST() {
        return base.toAST();
    }

    @Override
    public void resolveTypes(Program program) {
        base.resolveTypes(program);
    }

}
