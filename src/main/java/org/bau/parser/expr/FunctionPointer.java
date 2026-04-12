package org.bau.parser.expr;

import java.util.ArrayList;

import org.bau.parser.BasicBlock;
import org.bau.parser.DataType;
import org.bau.parser.FunctionContext;
import org.bau.parser.FunctionDefinition;
import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.bau.parser.Solver;
import org.bau.parser.stmt.Statement;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class FunctionPointer implements Expression {

    private final FunctionDefinition function;
    private DataType functionPointerType;

    public FunctionPointer(FunctionDefinition function) {
        this.function = function;
        ArrayList<DataType> argTypes = new ArrayList<>();
        for (int i = 0; i < function.parameters.size(); i++) {
            Variable v = function.parameters.get(i);
            argTypes.add(v.type());
        }
        this.functionPointerType = DataType.newFunctionPointer(function.getFullName().module, argTypes, function.returnType);
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
        if (!function.getFullName().module.isEmpty()) {
            buff.append(function.getFullName().getEscapedModuleId());
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
    public void setOwnedBoundsToNull(Solver solver, int level, boolean loop) {
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        return this;
    }

    public String format() {
        return function.toString();
    }

    @Override
    public void used(Program program) {
        program.getFunctionById(function.getFunctionId()).used(program);
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
        throw new IllegalStateException();
    }

    @Override
    public Expression resolveTypes(FunctionContext context) {
        function.resolveTypes(context.getProgram());
        functionPointerType = functionPointerType.resolve(context.getProgram());
        return this;
    }

    public String toString() {
        return format();
    }

}
