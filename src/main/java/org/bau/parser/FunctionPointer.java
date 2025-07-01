package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class FunctionPointer implements Expression {

    DataType functionPointerType;
    FunctionDefinition function;

    @Override
    public Value eval(Memory memory) {
        return new Value.ValueFunctionPointer(function.getFunctionId());
    }

    @Override
    public DataType type() {
        return functionPointerType;
    }

    @Override
    public DataType canThrowException() {
        return function.exceptionType;
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
    public String toC() {
        return function.nameC() + "_" + function.parameters.size();
    }

    @Override
    public boolean isEasyToRead() {
        return false;
    }

    @Override
    public Bounds getBounds() {
        return null;
    }

    @Override
    public void setOwnedBoundsToNull(Expression scope) {
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        return this;
    }

    @Override
    public void used(Program program) {
        function.used(program);
    }

}
