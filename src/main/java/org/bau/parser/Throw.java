package org.bau.parser;

import java.util.HashSet;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Throw implements Statement {

    Expression expr;

    private String exceptionVar;
    private String catchLabel;
    private String exceptionStruct;

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

    public String toString() {
        return "throw " + expr;
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

}
