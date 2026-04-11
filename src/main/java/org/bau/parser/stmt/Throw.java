package org.bau.parser.stmt;

import java.util.HashSet;

import org.bau.parser.BasicBlock;
import org.bau.parser.DataType;
import org.bau.parser.FunctionContext;
import org.bau.parser.MemoryType;
import org.bau.parser.Program;
import org.bau.parser.ProgramContext;
import org.bau.parser.expr.Expression;
import org.bau.parser.expr.Variable;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Throw implements Statement {

    private String exceptionVar;
    private String catchLabel;
    private String exceptionStruct;

    public Expression expr;

    @Override
    public Throw replace(Variable old, Expression with) {
        Throw c = new Throw();
        c.expr = expr.replace(old, with);
        return c;
    }

    @Override
    public void optimize(ProgramContext context) {
        exceptionVar = "_x" + context.nextExceptionVariableId++;
        context.delareList.add(context.function.exceptionType.nameC() + " _lastException;");
        context.delareList.add(context.function.getExceptionStruct() + " " + exceptionVar + ";");
        context.needToCatch = context.function.exceptionType;
        catchLabel = "catch" + context.nextCatchLabel;
        exceptionStruct = context.function.getExceptionStruct();
    }

    @Override
    public String toC() {
        StringBuilder buff = new StringBuilder();
        buff.append(exceptionVar + " = exception" + exceptionStruct + "(" + expr.toC() + "); _lastException = " + exceptionVar + ".exception; goto " + catchLabel + ";\n");
        return buff.toString();
    }

    public String format() {
        return "throw " + expr.format();
    }

    @Override
    public StatementResult run(Memory m) {
        Value v = expr.eval(m);
        m.setGlobal(Memory.EXCEPTION, v);
        return StatementResult.THROW;
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        // nothing
    }

    @Override
    public void setVariableVersions(FunctionContext functionContext, BasicBlock basicBlock) {
        if (expr != null) {
            expr.setVariableVersions(functionContext, basicBlock);
        }
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
        if (expr != null) {
            expr.setVariableVersions(name, oldVersion, newVersion);
        }
    }

    @Override
    public void used(Program program) {
        expr.used(program);
    }

    @Override
    public DataType canThrowException() {
        return expr.type();
    }

    @Override
    public void resolveTypesForStatement(Program program) {
        expr = expr.resolveTypes(program);
    }

}
