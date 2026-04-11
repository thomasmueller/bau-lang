package org.bau.parser.expr;

import java.util.ArrayList;

import org.bau.parser.BasicBlock;
import org.bau.parser.DataType;
import org.bau.parser.FunctionContext;
import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.bau.parser.Solver;
import org.bau.parser.stmt.Statement;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueNull;

public class NullValue implements Expression {

    private DataType type;

    public NullValue(DataType type) {
        this.type = type;
    }

    @Override
    public Value eval(Memory memory) {
        return ValueNull.INSTANCE;
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
        return this;
    }

    @Override
    public Expression simplify() {
        return this;
    }

    @Override
    public String format() {
        return "null";
    }

    @Override
    public String toC() {
        return "NULL";
    }

    @Override
    public void setOwnedBoundsToNull(Solver solver, int level, boolean loop) {
    }

    @Override
    public boolean isEasyToRead() {
        return true;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        return this;
    }

    @Override
    public void used(Program program) {
        if (type != null) {
            type.used(program);
        }
    }

    @Override
    public boolean containsModifiableVariables() {
        return false;
    }

    @Override
    public void setVariableVersions(FunctionContext functionContext, BasicBlock basicBlock) {
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
    }

    @Override
    public String toAST() {
        return "\"null\"";
    }

    @Override
    public Expression resolveTypes(Program program) {
        if (type != null) {
            type = type.resolve(program);
        }
        return this;
    }

    public String toString() {
        return format();
    }

}
