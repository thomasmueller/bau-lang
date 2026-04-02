package org.bau.parser.expr;

import java.util.ArrayList;
import java.util.List;

import org.bau.parser.BasicBlock;
import org.bau.parser.DataType;
import org.bau.parser.FunctionContext;
import org.bau.parser.MemoryType;
import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.bau.parser.Solver;
import org.bau.parser.stmt.Statement;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Borrow implements Expression {

    private Expression base;

    public Borrow(Expression base) {
        this.base = base;
    }

    @Override
    public Value eval(Memory memory) {
        return base.eval(memory);
    }

    @Override
    public DataType type() {
        DataType type = base.type();
        if (type.memoryType() != MemoryType.OWNER) {
            throw new IllegalArgumentException();
        }
        type = type.borrowType();
        return type;
    }

    @Override
    public DataType canThrowException() {
        return base.canThrowException();
    }

    @Override
    public Expression replace(Variable old, Expression with) {
        return new Borrow(base.replace(old, with));
    }

    @Override
    public Expression simplify() {
        return new Borrow(base.simplify());
    }

    @Override
    public String toC() {
        return base.toC();
    }

    @Override
    public void setOwnedBoundsToNull(Solver solver, int level, boolean loop) {
        // ignore
    }

    @Override
    public boolean isEasyToRead() {
        return base.isEasyToRead();
    }

    @Override
    public boolean isSimple() {
        return base.isSimple();
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        base = base.writeStatements(parser, false, target);
        return this;
    }

    public static String resetUsedOwned(List<Expression> usedOwnedList) {
        if (usedOwnedList.isEmpty()) {
            return "";
        }
        StringBuilder buff = new StringBuilder();
        for (Expression e : usedOwnedList) {
            if (e instanceof NullValue) {
                continue;
            }
            buff.append(e.toC() + " = NULL;\n");
        }
        return buff.toString();
    }

    @Override
    public String format() {
        return "&" + base.format();
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
    public String toAST() {
        throw new IllegalStateException();
    }

    @Override
    public void resolveTypes(Program program) {
        base.resolveTypes(program);
    }

}
