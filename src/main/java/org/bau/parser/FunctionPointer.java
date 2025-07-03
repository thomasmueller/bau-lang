package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class FunctionPointer implements Expression {

    private final FunctionDefinition function;
    private final DataType functionPointerType;

    FunctionPointer(FunctionDefinition function) {
        this.function = function;
        ArrayList<DataType> argTypes = new ArrayList<>();
        for (int i = 0; i < function.parameters.size(); i++) {
            Variable v = function.parameters.get(i);
            argTypes.add(v.type());
        }
        this.functionPointerType = DataType.newFunctionPointer(function.module, argTypes, function.returnType);
    }

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
        StringBuilder buff = new StringBuilder();
        if (function.module != null) {
            buff.append(Program.esc(function.module).replace(".", "_"));
            buff.append("_");
        }
        buff.append(function.nameC());
        buff.append("_");
        buff.append(function.parameters.size());
        return buff.toString();
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

    public String toString() {
        return function.toString();
    }

    @Override
    public void used(Program program) {
        program.getFunctionById(function.getFunctionId()).used(program);
    }

}
